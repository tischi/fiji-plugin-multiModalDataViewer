package de.embl.cba.mmdv.command;

import de.embl.cba.mmdv.viewer.MultiModalDataViewer;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Plugin(type = Command.class, menuPath = "Plugins>BigDataViewer>Open>Open Multiple XML/HDF5 Images" )
public class OpenMultipleFilesInBdvCommand implements Command
{
	@Parameter ( visibility = ItemVisibility.MESSAGE  )
	String message = "Select images";

	@Parameter ( label = "Choose input files" )
	public File[] inputFiles;

	@Parameter ( label = "Only consider files matching" )
	public String regExp = ".*.xml";

	@Parameter ( label = "Blending mode", choices = { "Avg", "Sum", "Auto" } )
	public String blendingMode = "Avg";

	public void run()
	{
		final ArrayList< String > validPaths = getValidPaths( regExp, inputFiles );
		final MultiModalDataViewer viewer = new MultiModalDataViewer( validPaths );
		viewer.showImages( MultiModalDataViewer.BlendingMode.valueOf( blendingMode ) );
	}

	private ArrayList< String > getValidPaths( String regExp, File[] paths )
	{
		final ArrayList< String > validPaths = new ArrayList<>();

		for( File file : paths )
		{
			final Matcher matcher = Pattern.compile( regExp ).matcher( file.getName() );
			if ( matcher.matches() ) validPaths.add( file.getAbsolutePath() );
		}
		return validPaths;
	}
}
