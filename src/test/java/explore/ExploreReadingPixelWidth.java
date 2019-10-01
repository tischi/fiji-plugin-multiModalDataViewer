package explore;

import de.embl.cba.mmdv.ImageIO;

import java.io.File;

public class ExploreReadingPixelWidth
{
	public static void main( String[] args )
	{
		final double pixelWidth = ImageIO.getNanometerPixelWidthUsingBF( new File( "/Users/tischer/Desktop/20x_g5_a1.nd2" ) );

		System.out.println( pixelWidth );
	}
}
