package users.mizzon;

import de.embl.cba.templatematching.browse.MatchedTemplatesBrowser;
import de.embl.cba.templatematching.browse.TemplatesBrowsingSettings;
import de.embl.cba.templatematching.command.TemplatesBrowserCommand;
import net.imagej.ImageJ;

import java.io.File;

public class BrowseMatchedTemplatesCLEM01
{
	public static void main(final String... args)
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		TemplatesBrowsingSettings browsingSettings = new TemplatesBrowsingSettings();

		browsingSettings.inputDirectory = new File(
				"/Volumes/emcf/Mizzon/tischi/clem01/match" );

		new MatchedTemplatesBrowser( browsingSettings ).run();
	}
}
