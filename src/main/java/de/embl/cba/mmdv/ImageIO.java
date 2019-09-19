package de.embl.cba.templatematching;

import de.embl.cba.templatematching.image.CalibratedRaiPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ImageConverter;
import ij.process.StackConverter;
import loci.common.services.ServiceFactory;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.VirtualStackAdapter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.io.File;

public class ImageIO
{
	public static  < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > openImageAs8Bit( File file )
	{
		Utils.log( "Loading " + file.getName() + "...");

		ImagePlus imp = withBFopenImp( file );

		if ( imp == null )
		{
			Utils.error( "Could not open file: " + file.getAbsolutePath() );
			return null;
		}

		if ( imp.getNSlices() > 1 )
		{
			new StackConverter( imp ).convertToGray8();
		}
		else
		{
			new ImageConverter( imp ).convertToGray8();
		}

		return ImageJFunctions.wrapReal( imp );
	}

	public static double getNanometerVoxelSize( ImagePlus imagePlus )
	{
		final Calibration calibration = imagePlus.getCalibration();

		String unit = calibration.getUnit();

		double voxelSize = calibration.pixelWidth;

		if ( unit != null )
		{
			if ( unit.equals( "nm" ) || unit.equals( "nanometer" )
					|| unit.equals( "nanometers" ) )
			{
				voxelSize = voxelSize;
			}
			else if ( unit.equals( "\u00B5m" ) || unit.equals( "um" )
					|| unit.equals( "micrometer" ) || unit.equals( "micrometers" )
					|| unit.equals( "microns" ) || unit.equals( "micron" ) )
			{
				voxelSize = voxelSize * 1000D;
			}
		}

		return voxelSize;
	}

	public static ImagePlus withBFopenImp( File file )
	{
		try
		{
			ImporterOptions opts = new ImporterOptions();
			opts.setId( file.toString() );
			opts.setVirtual( false );

			ImportProcess process = new ImportProcess( opts );
			process.execute();

			ImagePlusReader impReader = new ImagePlusReader( process );
			ImagePlus[] imps = impReader.openImagePlus();
			return imps[ 0 ];
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			return null;
		}

	}

	public static double getNanometerPixelWidth( File file )
	{
		if ( file.getName().contains( ".tif" ) )
		{
			Utils.log( "Reading voxel size from " + file.getName() );

			final ImagePlus imagePlus = IJ.openVirtual( file.getAbsolutePath() );

			double voxelSize = Utils.asNanometers(
					imagePlus.getCalibration().pixelWidth,
					imagePlus.getCalibration().getUnit() );

			if ( voxelSize == -1 )
			{
				Utils.error( "Could not interpret nanometerCalibration unit of "
						+ file.getName() +
						"; unit found was: "
						+ imagePlus.getCalibration().getUnit() );
			}

			Utils.log("Voxel size [nm]: " + voxelSize );
			return voxelSize;
		}
		else
		{
			return getNanometerPixelWidthUsingBF( file );
		}

	}

	public static double getNanometerPixelWidthUsingBF( File file )
	{
		Utils.log( "Reading voxel size from " + file.getName() );

		// create OME-XML metadata store
		ServiceFactory factory = null;
		try
		{
			factory = new ServiceFactory();
			OMEXMLService service = factory.getInstance(OMEXMLService.class);
			IMetadata meta = service.createOMEXMLMetadata();

			// create format reader
			IFormatReader reader = new ImageReader();
			reader.setMetadataStore( meta );

			// initialize file
			reader.setId( file.getAbsolutePath() );
			reader.setSeries(0);

			String unit = meta.getPixelsPhysicalSizeX( 0 ).unit().getSymbol();
			final double value = meta.getPixelsPhysicalSizeX( 0 ).value().doubleValue();

			double voxelSize = Utils.asNanometers( value, unit );

			Utils.log("Voxel size [nm]: " + voxelSize );
			return voxelSize;

		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}

		return 0.0;
	}

	public static  < T extends RealType< T > & NativeType< T > >
	CalibratedRaiPlus< T > withBFopenRAI( File file )
	{
		ImagePlus imp = withBFopenImp( file );

		final CalibratedRaiPlus< T > calibratedRaiPlus = new CalibratedRaiPlus<>( imp );

		return calibratedRaiPlus;
	}
}
