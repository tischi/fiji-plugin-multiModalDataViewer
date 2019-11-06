package users.maxim;

import de.embl.cba.mmdv.viewer.MultiModalDataViewer;
import net.imagej.ImageJ;

import java.io.File;
import java.util.ArrayList;

import static de.embl.cba.mmdv.Utils.getFileList;

public class ExploreXRayData
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final ArrayList< File > paths = getFileList( new File( "/Volumes/cba/exchange/maxim/ver2" ), ".*.xml" );

//		final ArrayList< File > subSet = new ArrayList<>();
//		subSet.add( paths.get( 0 ) );
//		subSet.add( paths.get( 1 ) );

		final MultiModalDataViewer viewer = new MultiModalDataViewer( paths );
		viewer.showImages( MultiModalDataViewer.BlendingMode.Avg );
	}
}
