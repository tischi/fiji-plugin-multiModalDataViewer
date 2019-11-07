package test;

import de.embl.cba.mmdv.viewer.MultiModalDataViewer;
import net.imagej.ImageJ;
import org.junit.Test;

import java.util.ArrayList;

public class TestRunMultiModalDataViewer
{
	//@Test
	public static void main( String[] args )
	{
		final ArrayList< String > imagePaths = new ArrayList<>();

		imagePaths.add( TestRunMultiModalDataViewer.class.getResource(
				"../test-data/bdv-images/fib-sem-volume.xml" ).getFile() );
		imagePaths.add( TestRunMultiModalDataViewer.class.getResource(
				"../test-data/bdv-images/fib-sem-volume-crop.xml" ).getFile() );

		new MultiModalDataViewer( imagePaths ).showImages();
	}
}
