package command;

import de.embl.cba.templatematching.command.TemplatesMatchingCommand;
import net.imagej.ImageJ;

public class RunMultiTemplateMatchingCommand
{

	public static void main(final String... args)
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// invoke the plugin
		ij.command().run( TemplatesMatchingCommand.class, true );
	}

}
