package de.embl.cba.templatematching.image;

import de.embl.cba.templatematching.Utils;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class CalibratedRaiPlus< T extends RealType< T > & NativeType< T > >
		implements CalibratedRai< T >
{
	private RandomAccessibleInterval< T > rai;
	private double[] nanometerCalibration;
	public boolean is3D = false;
	public boolean isMultiChannel = false;
	public String name;

	public CalibratedRaiPlus( ImagePlus imp )
	{
		super();

		rai = ImageJFunctions.wrapReal( imp );

		final String unit = imp.getCalibration().getUnit();

		nanometerCalibration = new double[ 3 ];

		nanometerCalibration[ 0 ] = Utils.asNanometers(
				imp.getCalibration().pixelWidth, unit );
		nanometerCalibration[ 1 ] = Utils.asNanometers(
				imp.getCalibration().pixelHeight, unit );
		nanometerCalibration[ 2 ] = Utils.asNanometers(
				imp.getCalibration().pixelDepth, unit );

		if ( imp.getNChannels() > 1 ) isMultiChannel = true;
		if ( imp.getNSlices() > 1 ) is3D = true;

		name = imp.getTitle();

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
