package de.embl.cba.mmdv.command;

import de.embl.cba.mmdv.viewer.MultiModalDataViewer;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>Multi Modal Data Viewer>View Multiple XML/HDF5 Images" )
public class MultiModalDataViewerCommand implements Command
{
	@Parameter ( visibility = ItemVisibility.MESSAGE  )
	String message = "Select images";

	@Parameter ( label = "Choose input files", style = "files, extensions:xml" )
	public File[] inputFiles;

	public void run()
	{
		final MultiModalDataViewer viewer = new MultiModalDataViewer( inputFiles );
	}
}
