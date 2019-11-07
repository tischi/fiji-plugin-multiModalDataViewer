package de.embl.cba.mmdv.viewer;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.DisplayMode;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.bdv.utils.behaviour.BdvBehaviours;
import de.embl.cba.bdv.utils.io.SPIMDataReaders;
import de.embl.cba.bdv.utils.render.AccumulateEMAndFMProjectorARGB;
import de.embl.cba.mmdv.Utils;
import de.embl.cba.mmdv.bdv.ImageSource;
import de.embl.cba.mmdv.rendertest.AccumulateAverageProjectorARGB;
import de.embl.cba.morphometry.Algorithms;
import de.embl.cba.morphometry.registration.platynereis.PlatynereisRegistration;
import de.embl.cba.morphometry.registration.platynereis.PlatynereisRegistrationSettings;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> (new Thread( () -> {
			prealignCurrentPlatynereisXRaySource( );
		} )).start(), "Register Platy", "P" ) ;
	}

	public void prealignCurrentPlatynereisXRaySource( )
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
		settings.showIntermediateResults = false;
		settings.inputCalibration = BdvUtils.getCalibration( source, level );
		settings.thresholdMethod = Huang;
		final PlatynereisRegistration< R > registration = new PlatynereisRegistration<>( settings, opService );
		final RandomAccessibleInterval< R > rai = ( RandomAccessibleInterval< R >) source.getSource( 0, level );
		registration.run( rai );
		final AffineTransform3D registrationTransform = registration.getRegistrationTransform(
				new double[]{1,1,1}, 1);

		System.out.println( registrationTransform );
		final TransformedSource transformedSource = ( TransformedSource ) source;
		transformedSource.setFixedTransform( registrationTransform );

		BdvUtils.repaint( bdv );
		BdvUtils.moveToPosition( bdv, new double[]{0,0,0}, 0, 100 );

//		final RandomAccessibleInterval< R > fullRes = ( RandomAccessibleInterval< R >) source.getSource( 0, 0 );
//		final RandomAccessibleInterval< R > arrayImg = Utils.copyAsArrayImg( fullRes );
//		BdvFunctions.show( arrayImg, "fullRes", BdvOptions.options().sourceTransform( registrationTransform ) );
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
		this.blendingMode = blendingMode;

		for ( String filePath : inputFilePaths )
		{
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

	private void addToBdv( String filePath ) throws SpimDataException
	{
		Source< VolatileARGBType > source = openVolatileARGBTypeSource( filePath );

		BdvOptions options = createBdvOptions();

		final SpimData spimData = new XmlIoSpimData().load( filePath );

		final BdvStackSource< ? > bdvStackSource = BdvFunctions.show(
				spimData,
				options
				).get( 0 );

		// TODO: see how this is done in bdv-fiji
//		new Thread( () -> setAutoContrastDisplayRange( bdvStackSource ) ).start();

//		setColor( filePath, bdvStackSource );

		bdv = bdvStackSource.getBdvHandle();

		if ( isFirstImage )
		{
			bdv.getViewerPanel().setDisplayMode( DisplayMode.SINGLE ); // TODO: make this optional (or in fact control with own UI)
			isFirstImage = false;
		}

		// TODO:
		// autocontrast
		//imageSources.add( new ImageSource( filePath, bdvStackSource, spimData ) );

		//Utils.updateBdv( bdv,1000 );
	}

	private BdvOptions createBdvOptions()
	{
		BdvOptions options = BdvOptions.options()
				.addTo( bdv )
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
