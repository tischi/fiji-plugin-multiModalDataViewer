package test;

import de.embl.cba.mmdv.viewer.MultiModalDataViewer;

import java.util.ArrayList;

public class TestRunMultiModalDataViewer2
{
	//@Test
	public static void main( String[] args )
	{
		final ArrayList< String > imagePaths = new ArrayList<>();

		imagePaths.add( "/Users/tischer/Desktop/bdv-images/fib-sem-volume.xml" );
		imagePaths.add( "/Users/tischer/Desktop/bdv-images/fib-sem-volume-crop.xml" );

		new MultiModalDataViewer( imagePaths ).showImages();
	}
}
