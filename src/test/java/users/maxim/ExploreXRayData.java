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

		final ArrayList< File > paths = getFileList( new File( "/Volumes/cba/exchange/maxim" ), ".*.xml" );

		new MultiModalDataViewer( paths );
	}
}
