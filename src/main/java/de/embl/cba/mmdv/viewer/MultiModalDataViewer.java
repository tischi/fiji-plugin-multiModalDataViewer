package de.embl.cba.mmdv.viewer;

import bdv.util.*;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.mmdv.bdv.BehaviourTransformEventHandler3DWithoutRotation;
import de.embl.cba.mmdv.bdv.ImageSource;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.embl.cba.mmdv.BdvUtils.*;

public class MultiModalDataViewer< R extends RealType< R > & NativeType< R > >
{
	private List< String > inputFilePaths = new ArrayList<>();
	private BdvHandle bdv;
	private List< ImageSource > imageSources;
	private double contrastFactor = 0.1;

	public static final ARGBType OVERVIEW_EM_COLOR =
			new ARGBType( ARGBType.rgba( 125, 125, 125, 255 ) );


	public MultiModalDataViewer( List< String > inputFilePaths )
	{
		this.inputFilePaths = inputFilePaths;
		run();
	}

	public MultiModalDataViewer( String[] inputFilePaths )
	{
		this.inputFilePaths = Arrays.asList( inputFilePaths );
		run();
	}

	public MultiModalDataViewer( File[] inputFiles )
	{
		final List< File > files = Arrays.asList( inputFiles );

		this.inputFilePaths = new ArrayList< >();
		for (int i = 0; i < files.size(); i++)
			this.inputFilePaths.add( files.get( i ).getAbsolutePath() );

		run();
	}

	private void run( )
	{
		imageSources = new ArrayList<>(  );
		showImages();
		// showUI();
	}

	private void showUI()
	{
		final MatchedTemplatesBrowserUI ui = new MatchedTemplatesBrowserUI( bdv );
		ui.showUI();
	}

	private void showImages()
	{
		for ( String filePath : inputFilePaths )
			addToBdv( filePath );

		moveBdvViewToAxialZeroPosition( bdv.getBdvHandle() );
	}

	private void addToBdv( String filePath )
	{
		final SpimData spimData = openSpimData( filePath );

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

		setColor( filePath, bdvStackSource );

		bdv = bdvStackSource.getBdvHandle();

		imageSources.add( new ImageSource( filePath, bdvStackSource, spimData ) );

		//Utils.updateBdv( bdv,1000 );
	}

	private void setColor( String filePath, BdvStackSource< ? > bdvStackSource )
	{
		if ( filePath.contains( "overview" ) )
			bdvStackSource.setColor( OVERVIEW_EM_COLOR );

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
