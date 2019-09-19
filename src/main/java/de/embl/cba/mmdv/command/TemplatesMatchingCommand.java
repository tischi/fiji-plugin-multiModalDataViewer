package de.embl.cba.templatematching.command;

import de.embl.cba.templatematching.browse.MatchedTemplatesBrowser;
import de.embl.cba.templatematching.browse.TemplatesBrowsingSettings;
import de.embl.cba.templatematching.match.TemplatesMatcher;
import de.embl.cba.templatematching.match.TemplatesMatchingSettings;
import ij.IJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>Multi Template Matching" )
public class TemplatesMatchingCommand implements Command
{
	TemplatesMatchingSettings settings = new TemplatesMatchingSettings();

	@Parameter ( label = "Overview Image" )
	public File overviewImage = settings.overviewImageFile;

	@Parameter ( label = "Templates Directory", style = "directory" )
	public File inputDirectory = settings.templatesInputDirectory;

	@Parameter ( label = "Templates Regular Expression" )
	public String templatesRegExp = settings.templatesRegExp;

	@Parameter ( label = "Angle between Overview and Templates" )
	public double tomogramAngleDegrees = settings.overviewAngleDegrees;

	@Parameter ( label = "Output Directory", style = "directory" )
	public File outputDirectory = settings.outputDirectory;

	@Parameter ( label = "Pixel Spacing during Matching (Put 0 for Overview Image Pixel Spacing) [nm]" )
	public double pixelSpacingDuringMatching =
			settings.matchingPixelSpacingNanometer;

	@Parameter ( label = "Hierarchical matching" )
	public boolean isHierarchicalMatching = false;

//	@Parameter ( label = "All Images fit into RAM (faster writing)" )
	boolean allTemplatesFitInRAM = false;

	@Parameter ( label = "Run Silent" )
	public boolean runSilent = false;

	@Parameter ( label = "Save Results in BigDataViewer Format" )
	public boolean saveResultsAsBdv = settings.saveResultsAsBdv;

	public void run()
	{
		setSettings();

		final TemplatesMatcher matching = new TemplatesMatcher( settings );

		if ( matching.run() )
		{
			TemplatesBrowsingSettings browsingSettings
					= new TemplatesBrowsingSettings();
			browsingSettings.inputDirectory = settings.outputDirectory;

			if ( saveResultsAsBdv )
				new MatchedTemplatesBrowser( browsingSettings ).run();
		}
		else
		{
			IJ.showMessage( "Template matching finished!" );
		}

	}

	public void setSettings()
	{
		settings.outputDirectory = outputDirectory;
		settings.overviewImageFile = overviewImage;
		settings.templatesInputDirectory = inputDirectory;
		settings.overviewAngleDegrees = tomogramAngleDegrees;
		settings.showIntermediateResults = ! runSilent;
		settings.confirmScalingViaUI = false;
		settings.matchingPixelSpacingNanometer = pixelSpacingDuringMatching;
		settings.templatesRegExp = templatesRegExp;
		settings.isHierarchicalMatching = isHierarchicalMatching;
		settings.saveResultsAsBdv = saveResultsAsBdv;
	}


}
