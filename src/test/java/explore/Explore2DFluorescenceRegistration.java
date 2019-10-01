package explore;

import de.embl.cba.mmdv.viewer.MultiModalDataViewer;
import net.imagej.ImageJ;

import java.util.ArrayList;

public class Explore2DFluorescenceRegistration
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final ArrayList< String > imagePaths = new ArrayList<>();
		imagePaths.add( Explore2DFluorescenceRegistration.class.getResource( "../correlate-2d-fm/Hela_full.xml" ).getFile() );
		imagePaths.add( Explore2DFluorescenceRegistration.class.getResource( "../correlate-2d-fm/Hela_crop.xml" ).getFile() );
		new MultiModalDataViewer( imagePaths );
	}
}
