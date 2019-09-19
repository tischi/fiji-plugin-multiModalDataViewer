package users.mizzon;

import de.embl.cba.templatematching.command.TemplatesMatchingCommand;
import net.imagej.ImageJ;

import java.io.File;

public class RunTemplatesMatchingGiulia00
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final TemplatesMatchingCommand command = new TemplatesMatchingCommand();

		command.inputDirectory =
				new File("/Volumes/emcf/Mizzon/projects/Vineet_CLEM/20190627-Vineet/a4-tomos");

		command.overviewImage =
				new File("/Volumes/emcf/Mizzon/projects/Vineet_CLEM/20190627-Vineet/a4-tomos/a4-Overlayed-21pts-reord.tif");

		command.outputDirectory =
				new File("/Volumes/emcf/Mizzon/projects/Vineet_CLEM/20190627-Vineet/a4-tomos/matched");

		command.templatesRegExp = ".*m.rec";

		command.pixelSpacingDuringMatching = 0;
		command.tomogramAngleDegrees = 0.0;
		command.saveResultsAsBdv = false;
		command.isHierarchicalMatching = false;
		command.runSilent = false;
		command.run();
	}

}
