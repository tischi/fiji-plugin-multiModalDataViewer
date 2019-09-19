package de.embl.cba.templatematching.match;

import de.embl.cba.templatematching.image.CalibratedRai;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.io.File;

public class MatchedTemplate < T extends RealType< T > & NativeType< T > >
{
	public final CalibratedRai< T > calibratedRai;
	public final double[] matchedPositionNanometer; // upper left corner = offset
	public File file;

	public MatchedTemplate( CalibratedRai< T > calibratedRai,
							double[] matchedPositionNanometer )
	{
		this.calibratedRai = calibratedRai;
		this.matchedPositionNanometer = matchedPositionNanometer;
	}

	public double[] getImageSizeNanometer()
	{
		final int numDimensions = calibratedRai.rai().numDimensions();

		double[] imageSizeNanometer = new double[ numDimensions ];
		for ( int d = 0; d < numDimensions; d++ )
			imageSizeNanometer[ d ] = calibratedRai.rai().dimension( d )
					* calibratedRai.nanometerCalibration()[ d ];

		return imageSizeNanometer;
	}

}
