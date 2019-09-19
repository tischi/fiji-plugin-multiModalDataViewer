package de.embl.cba.mmdv.image;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public interface CalibratedRai< T extends RealType< T > & NativeType< T > >
{
	RandomAccessibleInterval< T > rai();
	double[] nanometerCalibration();
}
