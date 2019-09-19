package de.embl.cba.templatematching;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils
{
	public static ArrayList< File > getFileList( File directory, String fileNameRegExp )
	{
		final ArrayList< File > files = new ArrayList<>();
		populateFileList( directory, fileNameRegExp, files );
		return files;
	}

	private static void populateFileList( File directory,
										  String fileNameRegExp,
										  List< File > files)
	{
		File[] fList = directory.listFiles();

		if( fList != null )
		{
			for ( File file : fList )
			{
				if ( file.isFile() )
				{
					final Matcher matcher = Pattern.compile( fileNameRegExp ).matcher( file.getName() );

					if ( matcher.matches() )
					{
						files.add( file );
					}

				}
				else if ( file.isDirectory() )
				{
					populateFileList( file, fileNameRegExp, files );
				}
			}
		}
	}
}
