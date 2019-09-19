package de.embl.cba.templatematching.bdv;

import bdv.util.BdvSource;
import mpicbg.spim.data.SpimData;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;

import java.io.File;

import static de.embl.cba.transforms.utils.Transforms.createBoundingIntervalAfterTransformation;

public class ImageSource
{
	private final File file;
	private final BdvSource bdvSource;
	private final SpimData spimData;

	public ImageSource( File file, BdvSource bdvSource, SpimData spimData )
	{
		this.file = file;
		this.bdvSource = bdvSource;
		this.spimData = spimData;
	}

	public String getName()
	{
		return file.getName().split( "\\." )[ 0 ];
	}

	public File getFile()
	{
		return file;
	}

	public FinalInterval getInterval()
	{
		final AffineTransform3D affineTransform3D = spimData.getViewRegistrations().getViewRegistration( 0, 0 ).getModel();
		RandomAccessibleInterval< ? > image = spimData.getSequenceDescription().getImgLoader().getSetupImgLoader( 0 ).getImage( 0 );

		final FinalInterval boundingIntervalAfterTransformation = createBoundingIntervalAfterTransformation( image, affineTransform3D );

		return boundingIntervalAfterTransformation;
	}

	public SpimData getSpimData()
	{
		return spimData;
	}


}
