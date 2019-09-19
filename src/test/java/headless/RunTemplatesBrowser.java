package headless;

import de.embl.cba.templatematching.browse.MatchedTemplatesBrowser;
import de.embl.cba.templatematching.browse.TemplatesBrowsingSettings;
import net.imagej.ImageJ;

import java.io.File;

public class RunTemplatesBrowser
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		TemplatesBrowsingSettings browsingSettings = new TemplatesBrowsingSettings();

		browsingSettings.inputDirectory = new File(
		"/Users/tischer/Documents/fiji-plugin-em-tomogram-matching/src/test/resources/matched-data-00" );

		new MatchedTemplatesBrowser( browsingSettings ).run();
	}
}
