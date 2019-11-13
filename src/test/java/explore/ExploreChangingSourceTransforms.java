package explore;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.mmdv.viewer.MultiModalDataViewer;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.realtransform.AffineTransform3D;

import java.io.File;
import java.util.ArrayList;

public class ExploreChangingSourceTransforms
{
	public static void main( String[] args ) throws SpimDataException
	{
		final ArrayList< String > imagePaths = new ArrayList<>();

		imagePaths.add( "/Users/tischer/Documents/fiji-plugin-multiModalDataViewer/src/test/resources/bdv-images/fib-sem-volume.xml" );

		final MultiModalDataViewer viewer = new MultiModalDataViewer( imagePaths );
		viewer.showImages();

		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		affineTransform3D.translate( 500, 0, 0 );
		affineTransform3D.rotate( 2, 1.0 );
		viewer.applyTransformToCurrentSource( affineTransform3D, "Registration transform" );
		final File file = viewer.saveSettingsXmlForCurrentSource();

		final SpimData load = new XmlIoSpimData().load( file.getAbsolutePath() );
		BdvFunctions.show( load ).get( 0 ).setDisplayRange( 0, 255 );
	}
}
