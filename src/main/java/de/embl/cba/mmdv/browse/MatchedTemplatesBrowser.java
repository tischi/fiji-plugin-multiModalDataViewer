package de.embl.cba.templatematching.browse;

import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.util.*;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.templatematching.bdv.BehaviourTransformEventHandler3DWithoutRotation;
import de.embl.cba.templatematching.bdv.ImageSource;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MatchedTemplatesBrowser< T extends RealType< T > & NativeType< T > >
{
	public static final ARGBType OVERVIEW_EM_COLOR =
			new ARGBType( ARGBType.rgba( 125, 125, 125, 255 ) );
	private final TemplatesBrowsingSettings settings;
	private ArrayList< File > inputFiles = new ArrayList<>();
	private BdvHandle bdv;
	private ArrayList< ImageSource > imageSources;
	private double contrastFactor = 0.1;

	public MatchedTemplatesBrowser( TemplatesBrowsingSettings settings )
	{
		this.settings = settings;
		this.imageSources = new ArrayList<>( );
	}

	public void run()
	{
		fetchImageSources( settings.inputDirectory.getAbsolutePath(), inputFiles );
		showImageSources();
		showUI();
	}

	public void moveBdvViewToAxialZeroPosition()
	{
		final AffineTransform3D viewerTransform = new AffineTransform3D();
		bdv.getBdvHandle().getViewerPanel()
				.getState().getViewerTransform( viewerTransform );
		final double[] translation = viewerTransform.getTranslation();
		translation[ 2 ] = 0;
		viewerTransform.setTranslation( translation );
		bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( viewerTransform );
	}

	private void showUI()
	{
		final MatchedTemplatesBrowserUI ui = new MatchedTemplatesBrowserUI( bdv );
		ui.showUI();
	}

	private void showImageSources()
	{
		for ( File file : inputFiles )
			addToBdv( file );
		moveBdvViewToAxialZeroPosition();
	}

	private void addToBdv( File file )
	{
		final SpimData spimData = openSpimData( file );

		setNames( spimData, file.getName() );

		final BdvStackSource< ? > bdvStackSource = BdvFunctions.show(
				spimData,
				BdvOptions.options()
						.addTo( bdv )
						.preferredSize( 800, 800 )
						.transformEventHandlerFactory(
								new BehaviourTransformEventHandler3DWithoutRotation
										.BehaviourTransformEventHandler3DFactory() )
				).get( 0 );


//		new Thread( () -> setAutoContrastDisplayRange( bdvStackSource ) ).start();

		setColor( file, bdvStackSource );

		bdv = bdvStackSource.getBdvHandle();

		imageSources.add( new ImageSource( file, bdvStackSource, spimData ) );

		//Utils.updateBdv( bdv,1000 );
	}

	private void setNames( SpimData spimData, String name )
	{
		int n = spimData.getSequenceDescription().getViewSetupsOrdered().size();

		for ( int i = 0; i < n; ++i )
		{
			// TODO: does not work, maybe because SpimData is inherently disk resident?
			// spimData.getSequenceDescription().getViewSetupsOrdered().get( i ).getChannel().setName( name + "-channel" + i );
		}

	}

	private void setColor( File file, BdvStackSource< ? > bdvStackSource )
	{
		if ( file.getName().contains( "overview" ) )
		{
			bdvStackSource.setColor( OVERVIEW_EM_COLOR );
		}
	}

	private void setAutoContrastDisplayRange( BdvStackSource< ? > bdvStackSource )
	{
		final List< ? extends SourceAndConverter< ? > > sources = bdvStackSource.getSources();

		for ( SourceAndConverter< ? > sourceAndConverter : sources )
		{
			final int numMipmapLevels =
					sourceAndConverter.getSpimSource().getNumMipmapLevels();

			final RandomAccessibleInterval< T > rai =
					( RandomAccessibleInterval ) sourceAndConverter
							.getSpimSource().getSource( 0, numMipmapLevels - 1 );

			final long stackCenter =
					( rai.max( 2 ) - rai.min( 2 ) ) / 2 + rai.min( 2 );

			final IntervalView< T > slice = Views.hyperSlice( rai, 2, stackCenter );

			final Cursor< T > cursor = Views.iterable( slice ).cursor();

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

	private SpimData openSpimData( File file )
	{
		try
		{
			final XmlIoSpimDataMinimal xmlIoSpimDataMinimal = new XmlIoSpimDataMinimal();
			final SpimData spimData =
					new XmlIoSpimData().load( file.getAbsolutePath() );
			return spimData;
		}
		catch ( SpimDataException e )
		{
			e.printStackTrace();
			return null;
		}
	}

	public void fetchImageSources( String directoryName, List<File> files)
	{
		File directory = new File( directoryName );

		File[] fList = directory.listFiles();
		if ( fList != null )
			for ( File file : fList )
				if ( file.isFile() )
					if ( isValid( file ) )
						files.add( file );
					else if ( file.isDirectory() )
						fetchImageSources( file.getAbsolutePath(), files );
	}

	private boolean isValid( File file )
	{
		if ( file.getName().endsWith( ".xml" ) )
			return true;
		else
			return false;
	}
}
