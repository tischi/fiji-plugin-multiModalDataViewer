package users.maxim;

import de.embl.cba.mmdv.command.OpenMultipleFilesInBdvCommand;
import net.imagej.ImageJ;

import java.io.File;

import static de.embl.cba.mmdv.Utils.getFileList;

public class ExploreXRayDataWithViewMultipleBdvImagesCommand
{
	public static void main(final String... args)
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final OpenMultipleFilesInBdvCommand command = new OpenMultipleFilesInBdvCommand();
		command.inputFiles = getFileList( new File( "/Volumes/cba/exchange/maxim/ver2" ), ".*.xml" ).toArray( new File[]{} );

		command.run();
	}
}
