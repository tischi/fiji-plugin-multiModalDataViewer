package de.embl.cba.mmdv.viewer;

import bdv.tools.HelpDialog;
import bdv.tools.brightness.MinMaxGroup;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.transformation.TransformedSource;
import bdv.tools.transformation.XmlIoTransformedSources;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.state.ViewerState;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.bdv.utils.behaviour.BdvBehaviours;
import de.embl.cba.bdv.utils.io.SPIMDataReaders;
import de.embl.cba.bdv.utils.render.AccumulateEMAndFMProjectorARGB;
import de.embl.cba.mmdv.Utils;
import de.embl.cba.mmdv.bdv.ImageSource;
import de.embl.cba.mmdv.rendertest.AccumulateAverageProjectorARGB;
import de.embl.cba.morphometry.registration.platynereis.PlatynereisRegistration;
import de.embl.cba.morphometry.registration.platynereis.PlatynereisRegistrationSettings;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.registration.ViewTransformAffine;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.histogram.DiscreteFrequencyDistribution;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static de.embl.cba.morphometry.registration.platynereis.PlatynereisRegistrationSettings.ThresholdMethod.*;

public class MultiModalDataViewer< R extends RealType< R > & NativeType< R > >
{
	private List< String > inputFilePaths = new ArrayList<>();
	private BdvHandle bdv;
	private List< ImageSource > imageSources;
	private double contrastFactor = 0.1;
	private BlendingMode blendingMode;
	private boolean isFirstImage = true;
	private OpService opService = null;
	private BdvOptions options;
	private HelpDialog helpDialog;
	private WeakHashMap< Source< ? >, String > sourceToXmlPath;
	private WeakHashMap< Source< ? >, SpimData > sourceToSpimData;
	private WeakHashMap< Source< ? >, Source< ? extends Volatile< ? > > > sourceToVolatileSource;


	public enum BlendingMode
	{
		Avg,
		Sum,
		Auto
	}

	public MultiModalDataViewer( List< ? > inputFiles )
	{
		if ( inputFiles.get( 0 ) instanceof File )
		{
			setInputFilePaths( (List< File >) inputFiles );
		}
		else if ( inputFiles.get( 0 ) instanceof String  )
		{
			this.inputFilePaths = (List< String >) inputFiles;
		}
		else
		{
			throw new UnsupportedOperationException( "Input file list is neither of type String nor File." );
		}
	}

	public MultiModalDataViewer( String[] inputFilePaths )
	{
		this.inputFilePaths = Arrays.asList( inputFilePaths );
	}

	public MultiModalDataViewer( File[] inputFiles )
	{
		setInputFilePaths( inputFiles );
	}

	private void initHelpDialog()
	{
		final URL helpFile = Utils.class.getResource( "/MultiModalDataViewerHelp.html" );
		helpDialog = new HelpDialog( null, helpFile );
	}

	public void setOpService( OpService opService )
	{
		this.opService = opService;
	}

	private void installBdvBehaviours()
	{
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdv.getTriggerbindings(), "" );

		/**
		 * TODO:
		 * - Currently one cannot change the color, because the sources are
		 * of ARGBType. This would be solved by being able to show SourceAndConverter.
		 *
		 */
		BdvBehaviours.addDisplaySettingsBehaviour( bdv, behaviours, "D" );

		BdvBehaviours.addViewCaptureBehaviour( bdv, behaviours, "C" );

		BdvBehaviours.addPositionAndViewLoggingBehaviour( bdv, behaviours, "P" );

		installSourceBrowsingBehaviour( behaviours );

		installPlatynereisRegistrationBehaviour( behaviours );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> (new Thread( () -> {
			printManualTransformOfCurrentSource();
		} )).start(), "Print manual transform", "shift T" ) ;

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> SwingUtilities.invokeLater( () -> {
			saveSettingsXmlForCurrentSource();
		} ), "Save new XML for current source (including additional transformations)", "ctrl S" ) ;

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> SwingUtilities.invokeLater( () -> {
			addSource();
		} ), "Open source from file", "O" ) ;

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> SwingUtilities.invokeLater( () -> {
			helpDialog.setVisible( ! helpDialog.isVisible() );
		} ), "Show additional help", "F2" ) ;

	}

	private void installPlatynereisRegistrationBehaviour( Behaviours behaviours )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> (new Thread( () -> {
			prealignCurrentPlatynereisXRaySource( false );
		} )).start(), "Register Platy Silent", "R" ) ;

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> (new Thread( () -> {
			prealignCurrentPlatynereisXRaySource( true );
		} )).start(), "Register Platy", "shift R" ) ;
	}

	private void installSourceBrowsingBehaviour( Behaviours behaviours )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {

			(new Thread( () -> {
				final int currentSource = bdv.getViewerPanel().getVisibilityAndGrouping().getCurrentSource();
				if ( currentSource == 0 ) return;
				bdv.getViewerPanel().getVisibilityAndGrouping().setCurrentSource( currentSource - 1 );
			} )).start();

		}, "Go to previous source", "J" ) ;

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {

			(new Thread( () -> {
				final int currentSource = bdv.getViewerPanel().getVisibilityAndGrouping().getCurrentSource();
				if ( currentSource == bdv.getViewerPanel().getVisibilityAndGrouping().numSources() - 1  ) return;
				bdv.getViewerPanel().getVisibilityAndGrouping().setCurrentSource( currentSource + 1 );
			} )).start();

		}, "Go to next source", "K" ) ;
	}

	public void prealignCurrentPlatynereisXRaySource( boolean showIntermediateResults )
	{
		// TODO: make all of this work for non-isotropic data
		Logger.log( "Registering..." );

		final PlatynereisRegistrationSettings settings = new PlatynereisRegistrationSettings();

		final int currentSource = bdv.getBdvHandle().getViewerPanel().getState().getCurrentSource();
		final VoxelDimensions voxelDimensions = BdvUtils.getVoxelDimensions( bdv, currentSource );
		final double[] calibration = new double[ 3 ];
		voxelDimensions.dimensions( calibration );

		final Source< ? > source = BdvUtils.getSource( bdv, currentSource );
		final int level = BdvUtils.getLevel( source, settings.registrationResolution );

		settings.showIntermediateResults = false;
		settings.outputResolution = voxelDimensions.dimension( 0 ); // assuming isotropic
		settings.invertImage = true;
		settings.showIntermediateResults = showIntermediateResults;
		settings.inputCalibration = BdvUtils.getCalibration( source, level );
		settings.thresholdMethod = Huang;
		final PlatynereisRegistration< R > registration = new PlatynereisRegistration<>( settings, opService );
		final RandomAccessibleInterval< R > rai = ( RandomAccessibleInterval< R >) source.getSource( 0, level );
		registration.run( rai );
		final AffineTransform3D registrationTransform = registration.getRegistrationTransform( new double[]{1,1,1}, 1);

		applyTransform( source, registrationTransform, "Platynereis registration transform" );

		final TransformedSource transformedSource = ( TransformedSource ) source;
		transformedSource.setFixedTransform( registrationTransform.copy() );
		BdvUtils.repaint( bdv );

		BdvUtils.moveToPosition( bdv, new double[]{ 0, 0, 0}, 0, 100 );
	}

	public void applyTransformToCurrentSource(
			AffineTransform3D transform,
			String transformName )
	{
		final int currentSource = bdv.getViewerPanel().getState().getCurrentSource();
		final Source< ? > source = BdvUtils.getSource( bdv, currentSource );
		applyTransform( source, transform, transformName );
	}

	private void applyTransform(
			Source< ? > source,
			AffineTransform3D transform,
			String transformName )
	{
//		final SpimData spimData = sourceToSpimData.get( source );

//		spimData.getViewRegistrations().getViewRegistration( 0, 0 ).preconcatenateTransform( new ViewTransformAffine( transformName, transform.copy() ) );
//
//		spimData.getViewRegistrations().getViewRegistration( 0, 0 ).updateModel();

		final TransformedSource transformedSource = ( TransformedSource ) source;
//
//		try
//		{
//			Method updateBdvSource = Class.forName("bdv.AbstractSpimSource").getDeclaredMethod("loadTimepoint", int.class);
//			updateBdvSource.setAccessible(true);
//			updateBdvSource.invoke( transformedSource.getWrappedSource(), 0 );
//			final Source< ? extends Volatile< ? > > volatileSource = sourceToVolatileSource.get( source );
//
//			final Source wrappedSource = ( ( TransformedSource ) volatileSource ).getWrappedSource();
//			updateBdvSource.invoke( wrappedSource, 0 );
//
//		}
//		catch ( Exception e )
//		{
//			e.printStackTrace();
//		}

		transformedSource.setFixedTransform( transform.copy() );
		BdvUtils.repaint( bdv );
	}

	public void printManualTransformOfCurrentSource( )
	{
		final int currentSource = bdv.getBdvHandle().getViewerPanel().getState().getCurrentSource();
		final Source< ? > source = BdvUtils.getSource( bdv, currentSource );
		final TransformedSource< ? > transformedSource = ( TransformedSource ) source;
		final AffineTransform3D manualTransform = new AffineTransform3D();
		transformedSource.getFixedTransform( manualTransform );
		final AffineTransform3D baseTransform = new AffineTransform3D();
		source.getSourceTransform( 0, 0, baseTransform );
		final AffineTransform3D concatenate = baseTransform.copy().concatenate( manualTransform );
		Logger.log( source.getName() );
		Logger.log( "Base transform:" + baseTransform.toString() );
		Logger.log( "Additional manual transform:" + manualTransform.toString() );
		Logger.log( "Full transform:" + concatenate.toString() );
	}

	public File saveSettingsXmlForCurrentSource()
	{
		final int currentSource = bdv.getBdvHandle().getViewerPanel().getState().getCurrentSource();
		final Source< ? > source = BdvUtils.getSource( bdv, currentSource );
		final TransformedSource< ? > transformedSource = ( TransformedSource ) source;
		final AffineTransform3D fixedTransform = new AffineTransform3D();
		transformedSource.getFixedTransform( fixedTransform );
		Logger.log( source.getName() );
		Logger.log( "Additional transform: " + fixedTransform.toString() );

		final SpimData spimData = sourceToSpimData.get( source );
		spimData.getViewRegistrations().getViewRegistration( 0, 0 ).preconcatenateTransform( new ViewTransformAffine( "Additional transform", fixedTransform ) );

		final String xmlPath = sourceToXmlPath.get( source );

		final JFileChooser fileChooser = new JFileChooser( new File( xmlPath ).getParent() );
		File proposedFile = new File( xmlPath.replace( ".xml", "-transformed.xml" ) );

		fileChooser.setSelectedFile( proposedFile );
		final int returnVal = fileChooser.showSaveDialog( null );
		if ( returnVal == JFileChooser.APPROVE_OPTION )
			proposedFile = fileChooser.getSelectedFile();
		else
			return proposedFile;

		try
		{
			System.out.println("save transformed image xml");
			new XmlIoSpimData().save( spimData, proposedFile.getAbsolutePath() );
		} catch ( SpimDataException e )
		{
			e.printStackTrace();
		}

		return proposedFile;


//		final XmlIoTransformedSources xmlIoTransformedSources = new XmlIoTransformedSources();
//		final ArrayList< AffineTransform3D > transforms = new ArrayList<>();
//		transforms.add( manualTransform );
//		final Element manualTransformXml = xmlIoTransformedSources.toXml( new ManualSourceTransforms( transforms ) );
//
//		final Element root = new Element( "Transformation" );
//		root.addContent( manualTransformXml );
//
//		final Document doc = new Document( root );
//		final XMLOutputter xout = new XMLOutputter( Format.getPrettyFormat() );
//
//		final String xmlPath = sourceToXmlPath.get( source );
//
//		final String settingsPath = xmlPath.replace( ".xml", ".settings.xml" );
//
//		Logger.log( "Saving settings: " + settingsPath );
//
//		try
//		{
//			xout.output( doc, new FileWriter( settingsPath ) );
//		} catch ( IOException e )
//		{
//			e.printStackTrace();
//		}
	}

	public void addSource( )
	{
		final JFileChooser jFileChooser = new JFileChooser( );
		if ( jFileChooser.showOpenDialog( bdv.getViewerPanel() ) == JFileChooser.APPROVE_OPTION )
		{
			final String absolutePath = jFileChooser.getSelectedFile().getAbsolutePath();
			try
			{
				final SpimData spimData = new XmlIoSpimData().load( absolutePath );
				BdvFunctions.show( spimData, BdvOptions.options().addTo( bdv ) );
			} catch ( SpimDataException e )
			{
				e.printStackTrace();
			}
		}
	}

	private void setInputFilePaths( File[] inputFiles )
	{
		final List< File > files = Arrays.asList( inputFiles );
		setInputFilePaths( files );
	}

	private void setInputFilePaths( List< File > files )
	{
		this.inputFilePaths = new ArrayList< >();
		for (int i = 0; i < files.size(); i++)
			this.inputFilePaths.add( files.get( i ).getAbsolutePath() );
	}

	private void showUI()
	{
		final MatchedTemplatesBrowserUI ui = new MatchedTemplatesBrowserUI( bdv );
		ui.showUI();
	}

	public void showImages( BlendingMode blendingMode )
	{
		initHelpDialog();

		this.blendingMode = blendingMode;

		sourceToXmlPath = new WeakHashMap<>();
		sourceToSpimData = new WeakHashMap<>();
		sourceToVolatileSource = new WeakHashMap<>();

		for ( String filePath : inputFilePaths )
		{
			if ( filePath.contains( ".settings.xml" ) ) continue;
			try
			{
				addToBdv( filePath );
			} catch ( SpimDataException e )
			{
				e.printStackTrace();
			}
		}

//		moveBdvViewToAxialZeroPosition( bdv.getBdvHandle() );

		installBdvBehaviours();
	}

	public void showImages()
	{
		showImages( BlendingMode.Sum );
	}

	public BdvHandle getBdv()
	{
		return bdv;
	}

	private void addToBdv( String xmlPath ) throws SpimDataException
	{
//		Source< VolatileARGBType > source = openVolatileARGBTypeSource( filePath );

		if ( isFirstImage )
			options = createBdvOptions();
		else
			options = options.addTo( bdv );

		final SpimData spimData = new XmlIoSpimData().load( xmlPath );

		// This seems slow and less volatile...
		final Source< R > inputSource = BdvUtils.openSource( xmlPath, 0 );

		final File settingsXml = new File( xmlPath.replace( ".xml", ".settings.xml" ) );

		Settings settings = null;

		if ( settingsXml.exists() )
			settings = tryLoadSettings( settingsXml.getAbsolutePath() );

		final BdvStackSource< ? > bdvStackSource = BdvFunctions.show(
				spimData,
				options
				).get( 0 );

		if ( isFirstImage ) bdv = bdvStackSource.getBdvHandle();

		if ( isFirstImage ) bdv.getViewerPanel().showMessage( "Press F1 or F2 for help." );

		final Source< ? > source = bdvStackSource.getSources().get( 0 ).getSpimSource();
		final Source< ? extends Volatile< ? > > volatileSource = bdvStackSource.getSources().get( 0 ).asVolatile().getSpimSource();

		sourceToXmlPath.put( source, xmlPath );
		sourceToSpimData.put( source, spimData );
		sourceToVolatileSource.put( source, volatileSource );

		if ( settings != null )
		{
			( ( TransformedSource ) source ).setFixedTransform( settings.transform );
			BdvUtils.repaint( bdv );
		}

//		setColor( filePath, bdvStackSource );

		initBrightness( 0.001, 0.999, bdv.getViewerPanel().getState(), bdv.getSetupAssignments(), sourceToXmlPath.size() - 1  );

		options = options.sourceTransform( new AffineTransform3D() );

		isFirstImage = false;
	}


	public static void initBrightness(
			final double cumulativeMinCutoff,
			final double cumulativeMaxCutoff,
			final ViewerState state,
			final SetupAssignments setupAssignments,
			int sourceIndex )
	{

		final Source< ? > source = state.getSources().get( sourceIndex ).getSpimSource();
		final int timepoint = state.getCurrentTimepoint();
		if ( !source.isPresent( timepoint ) )
			return;
		if ( !UnsignedShortType.class.isInstance( source.getType() ) )
			return;
		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< UnsignedShortType > img = ( RandomAccessibleInterval< UnsignedShortType > ) source.getSource( timepoint, source.getNumMipmapLevels() - 1 );
		final long z = ( img.min( 2 ) + img.max( 2 ) + 1 ) / 2;

		final int numBins = 6535;
		final Histogram1d< UnsignedShortType > histogram = new Histogram1d<>( Views.iterable( Views.hyperSlice( img, 2, z ) ), new Real1dBinMapper< UnsignedShortType >( 0, 65535, numBins, false ) );
		final DiscreteFrequencyDistribution dfd = histogram.dfd();
		final long[] bin = new long[] { 0 };
		double cumulative = 0;
		int i = 0;
		for ( ; i < numBins && cumulative < cumulativeMinCutoff; ++i )
		{
			bin[ 0 ] = i;
			cumulative += dfd.relativeFrequency( bin );
		}
		final int min = i * 65535 / numBins;
		for ( ; i < numBins && cumulative < cumulativeMaxCutoff; ++i )
		{
			bin[ 0 ] = i;
			cumulative += dfd.relativeFrequency( bin );
		}
		final int max = i * 65535 / numBins;
		final MinMaxGroup minmax = setupAssignments.getMinMaxGroups().get( sourceIndex );
		minmax.getMinBoundedValue().setCurrentValue( min );
		minmax.getMaxBoundedValue().setCurrentValue( max );
	}

	private Settings tryLoadSettings( String xmlPath )
	{
		Settings settings = null;
		try
		{

			settings = loadSettings( xmlPath );
		} catch ( IOException e )
		{
			e.printStackTrace();
		} catch ( JDOMException e )
		{
			e.printStackTrace();
		}
		return settings;
	}

	class Settings{
		AffineTransform3D transform;
	}

	private Settings loadSettings( final String xmlFilename ) throws IOException, JDOMException
	{
		final SAXBuilder sax = new SAXBuilder();
		final Document doc = sax.build( xmlFilename );
		final Element root = doc.getRootElement();

		final XmlIoTransformedSources io = new XmlIoTransformedSources();

		final Element elem = root.getChild( io.getTagName() );
		final List< AffineTransform3D > transforms = io.fromXml( elem ).getTransforms();

		final Settings settings = new Settings();
		settings.transform = transforms.get( 0 );
		return settings;
	}

	private BdvOptions createBdvOptions()
	{
		BdvOptions options = BdvOptions.options()
				.preferredSize( 600, 600 );

		options = addBlendingMode( options );

		return options;
	}

	private BdvOptions addBlendingMode( BdvOptions options )
	{
		if ( blendingMode.equals( BlendingMode.Auto ) )
			options = options.accumulateProjectorFactory( AccumulateEMAndFMProjectorARGB.factory );
		else if ( blendingMode.equals( BlendingMode.Avg ) )
			options = options.accumulateProjectorFactory( AccumulateAverageProjectorARGB.factory );
		else if ( blendingMode.equals( BlendingMode.Sum ) )
			options = options;

		return options;
	}

	private Source< VolatileARGBType > openVolatileARGBTypeSource( String filePath )
	{
		Source< VolatileARGBType > source;

		if ( filePath.endsWith( ".xml" ) )
			source = SPIMDataReaders.openAsVolatileARGBTypeSource( filePath, 0 );
		else
			throw new UnsupportedOperationException( "File type not supported: " + filePath );
		return source;
	}

	private void setAutoContrastDisplayRange( BdvStackSource< ? > bdvStackSource )
	{
		final List< ? extends SourceAndConverter< ? > > sources = bdvStackSource.getSources();

		for ( SourceAndConverter< ? > sourceAndConverter : sources )
		{
			final int numMipmapLevels =
					sourceAndConverter.getSpimSource().getNumMipmapLevels();

			final RandomAccessibleInterval< R > rai =
					( RandomAccessibleInterval ) sourceAndConverter
							.getSpimSource().getSource( 0, numMipmapLevels - 1 );

			final long stackCenter =
					( rai.max( 2 ) - rai.min( 2 ) ) / 2 + rai.min( 2 );

			final IntervalView< R > slice = Views.hyperSlice( rai, 2, stackCenter );

			final Cursor< R > cursor = Views.iterable( slice ).cursor();

			double min = Double.MAX_VALUE;
			double max = -Double.MAX_VALUE;
			double value;

			while ( cursor.hasNext() )
			{
				value = cursor.next().getRealDouble();
				if ( value < min ) min = value;
				if ( value > max ) max = value;
			}

			min = min - ( max - min ) * contrastFactor;
			max = max + ( max - min ) * contrastFactor;

			final int sourceIndex = BdvUtils.
					getSourceIndex( bdv, sourceAndConverter.getSpimSource() );

			bdv.getBdvHandle().getSetupAssignments().
					getConverterSetups().get( sourceIndex ).setDisplayRange( min, max  );
		}

	}

	private SpimData openSpimData( String filePath )
	{
		try
		{
			final SpimData spimData =
					new XmlIoSpimData().load( filePath );
			return spimData;
		}
		catch ( SpimDataException e )
		{
			e.printStackTrace();
			return null;
		}
	}

}
