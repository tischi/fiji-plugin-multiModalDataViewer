package de.embl.cba.templatematching.command;

import de.embl.cba.templatematching.browse.MatchedTemplatesBrowser;
import de.embl.cba.templatematching.browse.TemplatesBrowsingSettings;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>Matched Template Browsing" )
public class TemplatesBrowserCommand implements Command
{
	TemplatesBrowsingSettings settings = new TemplatesBrowsingSettings();

	@Parameter ( style = "directory" )
	public File inputDirectory = settings.inputDirectory;

	public void run()
	{
		setSettings();

		final MatchedTemplatesBrowser browser = new MatchedTemplatesBrowser( settings );

		browser.run();
	}

	public void setSettings()
	{
		settings.inputDirectory = inputDirectory;
	}


}
