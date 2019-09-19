package explore;

import bdv.util.AxisOrder;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.embl.cba.templatematching.image.CalibratedRaiPlus;
import de.embl.cba.templatematching.ImageIO;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class ApplyTransformationFromImageFile
{
	public static < T extends RealType< T > & NativeType< T > >
	void main( String[] args )  throws IOException
	{
		ObjectMapper objectMapper = new ObjectMapper();

		final Map map = objectMapper.readValue( new File( "/Volumes/cba/exchange/Giulia/TomoMatching3/A120g4-a1.tif (RGB).meta_src.json" ), Map.class );

		System.out.println( map );

		final ArrayList< ArrayList< Double >>  affineTransformation2D
				= ( ArrayList< ArrayList< Double >> ) map.get( "AffineTransformation2D" );

		final AffineTransform3D transform3D = new AffineTransform3D();

		for ( int d = 0; d < 2; d++ )
		{
			transform3D.set( affineTransformation2D.get(d).get(0), d, 0 );
			transform3D.set( affineTransformation2D.get(d).get(1), d, 1 );
			transform3D.set( affineTransformation2D.get(d).get(2), d, 3 );
		}

		transform3D.set( 1, 2, 2 );


		final CalibratedRaiPlus< T > fluorescence = ImageIO.withBFopenRAI(
				new File( "/Volumes/cba/exchange/Giulia/TomoMatching3/A120g4-a1.tif (RGB).tif" ) );

		final CalibratedRaiPlus< T > em = ImageIO.withBFopenRAI(
				new File( "/Volumes/cba/exchange/Giulia/TomoMatching3/polyA1_merged_s0.mrc" ) );

		final BdvStackSource< T > emSource = BdvFunctions.show( em.rai(), "em",
				BdvOptions.options().sourceTransform( em.nanometerCalibration() ).is2D() );

		final BdvStackSource< T > fluoSource = BdvFunctions.show( fluorescence.rai(), "fluo",
				BdvOptions.options()
						.sourceTransform( transform3D )
						.addTo( emSource.getBdvHandle() )
						.axisOrder( AxisOrder.XYC ) );

		fluoSource.setColor( new ARGBType( ARGBType.rgba( 0, 255, 0, 100 ) ) );


//		JSONObject jsonObject = ( JSONObject ) parser.parse(
//				new FileReader("/Volumes/cba/exchange/Giulia/TomoMatching3/A120g4-a1.tif (RGB).meta.json"));
//
//		final AffineTransform3D transform3D = new AffineTransform3D();
//		for ( Object key : jsonObject.keySet() )
//		{
//			System.out.println( key.toString() + ": " );
//			System.out.println( jsonObject.get( key ));
//			System.out.println( "\n");
//
//			if ( key.toString().equals( "AffineTransformation2D" ))
//			{
//				jsonObject.
//			}
//		}

	}
}
