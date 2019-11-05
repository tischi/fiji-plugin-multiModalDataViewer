package command;

import de.embl.cba.mmdv.command.OpenMultipleFilesInBdvCommand;
import net.imagej.ImageJ;

public class RunViewMultipleBdvImagesCommand
{
	public static void main(final String... args)
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// invoke the plugin
		ij.command().run( OpenMultipleFilesInBdvCommand.class, true );
	}
}
