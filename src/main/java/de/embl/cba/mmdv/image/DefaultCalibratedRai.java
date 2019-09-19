package de.embl.cba.templatematching.image;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class DefaultCalibratedRai< T extends RealType< T > & NativeType< T > >
		implements CalibratedRai< T >
{
	public RandomAccessibleInterval< T > rai;
	public double[] nanometerCalibration;

	public DefaultCalibratedRai( RandomAccessibleInterval< T > rai,
								 double[] nanometerCalibration )
	{
		this.rai = rai;
		this.nanometerCalibration = nanometerCalibration;
	}

	@Override
	public RandomAccessibleInterval< T > rai()
	{
		return rai;
	}

	@Override
	public double[] nanometerCalibration()
	{
		return nanometerCalibration;
	}
}
