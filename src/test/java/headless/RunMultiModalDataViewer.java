package headless;

import de.embl.cba.mmdv.viewer.MultiModalDataViewer;
import de.embl.cba.mmdv.viewer.TemplatesBrowsingSettings;
import net.imagej.ImageJ;

import java.io.File;
import java.util.ArrayList;

public class RunMultiModalDataViewer
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final ArrayList< String > imagePaths = new ArrayList<>();
		imagePaths.add( "/Users/tischer/Documents/fiji-plugin-multiModalDataViewer/src/test/resources/bdv-images/fib-sem-volume.xml" );
		imagePaths.add( "/Users/tischer/Documents/fiji-plugin-multiModalDataViewer/src/test/resources/bdv-images/fib-sem-volume-crop.xml" );

		new MultiModalDataViewer( imagePaths );
	}
}
