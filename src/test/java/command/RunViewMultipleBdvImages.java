package command;

import de.embl.cba.mmdv.command.ViewMultipleBdvImages;
import net.imagej.ImageJ;

public class RunViewMultipleBdvImages
{
	public static void main(final String... args)
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// invoke the plugin
		ij.command().run( ViewMultipleBdvImages.class, true );
	}
}
