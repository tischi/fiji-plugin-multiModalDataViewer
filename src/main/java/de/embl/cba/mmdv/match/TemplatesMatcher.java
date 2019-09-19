package de.embl.cba.templatematching.match;

import de.embl.cba.bdv.utils.io.BdvRaiXYZCTExport;
import de.embl.cba.templatematching.FileUtils;
import de.embl.cba.templatematching.ImageIO;
import de.embl.cba.templatematching.Utils;
import de.embl.cba.templatematching.image.CalibratedRai;
import de.embl.cba.templatematching.image.CalibratedRaiPlus;
import de.embl.cba.templatematching.image.DefaultCalibratedRai;
import de.embl.cba.templatematching.process.Processor;
import ij.ImagePlus;
import ij.gui.*;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;


public class TemplatesMatcher< T extends RealType< T > & NativeType< T > >
{

	private final TemplatesMatchingSettings settings;
	private ArrayList< File > templateFiles;
	private int templateIndex;
	private ArrayList< MatchedTemplate > matchedTemplates;
	private CalibratedRai< T > subsampledOverviewForMatching;
	private String highMagId;
	private String lowMagId;
	private CalibratedRaiPlus< T > rawOverview;
	private CalibratedRai rotatedOverviewForExport;

	public TemplatesMatcher( TemplatesMatchingSettings settings )
	{
		this.settings = settings;
		templateIndex = 0;
		highMagId = "_hm.";
		lowMagId = "_lm.";

		Utils.showIntermediateResults = settings.showIntermediateResults;
	}

	public boolean run()
	{
		if ( ! createTemplateFileList() ) return false;

		openOverview();

		if ( settings.saveResultsAsBdv)
			exportOverview( rotatedOverviewForExport, rawOverview.is3D, rawOverview.isMultiChannel );

		matchTemplates( subsampledOverviewForMatching );

		return true;
	}

	// TODO: refactor into separate class!
	public boolean saveResults()
	{
		if ( settings.showIntermediateResults )
			if ( !confirmSaving() ) return false;

		return saveImagesAsBdvHdf5();
	}

	public boolean confirmSaving()
	{
		final GenericDialog gd =
				new NonBlockingGenericDialog( "Save results" );

		gd.addMessage( "Would you like to " +
				"export the matched templates as multi-resolution images;\n" +
				"for viewing with the " +
				"Template Browsing plugin?" );

		gd.showDialog();

		if ( gd.wasCanceled() )
			return false;

		return true;
	}

	private boolean createTemplateFileList()
	{
		templateFiles = FileUtils.getFileList(
				settings.templatesInputDirectory, settings.templatesRegExp );

		if ( templateFiles.size() == 0 )
		{
			Utils.error( "No tomograms found!" );
			return false;
		}
		else
			return true;

	}

	private void matchTemplates( CalibratedRai< T > overview )
	{
		matchedTemplates = new ArrayList<>();

		TemplateMatcherTranslation2D templateToOverviewMatcher
				= new TemplateMatcherTranslation2D( overview );

		for ( File templateFile : templateFiles )
		{
			if ( settings.isHierarchicalMatching
					&& templateFile.getName().contains( highMagId ) )
				continue; // as this will be later matched in the hierarchy

			final CalibratedRaiPlus< T > template = openImage( templateFile );

			final MatchedTemplate matchedTemplate = templateToOverviewMatcher.match( template );

			matchedTemplate.file = templateFile;

			if ( settings.showMatching )
				showBestMatchOnOverview( matchedTemplate,
						templateToOverviewMatcher.getOverviewImagePlus() );

			if ( settings.saveResultsAsBdv )
				exportTemplate( matchedTemplate );

			if ( settings.isHierarchicalMatching && templateFile.getName().contains( lowMagId ) )
			{
				File highResFile = getHighResFile( templateFile );

				final CalibratedRaiPlus< T > highResTemplate = openImage( highResFile );

				final TemplateMatcherTranslation2D highResToLowResMatcher
						= new TemplateMatcherTranslation2D( templateToOverviewMatcher.getProcessedTemplate() );

				final MatchedTemplate matchedHighResTemplate
						= highResToLowResMatcher.match( highResTemplate );

				matchedHighResTemplate.file = highResFile;

				// Show match on lower resolution template
				if ( settings.showIntermediateResults )
					showBestMatchOnOverview( matchedHighResTemplate,
							highResToLowResMatcher.getOverviewImagePlus() );

				for ( int d = 0; d < 2; d++ )
					matchedHighResTemplate.matchedPositionNanometer[ d ] += matchedTemplate.matchedPositionNanometer[ d ];

				if ( settings.showMatching )
					showBestMatchOnOverview( matchedHighResTemplate,
							templateToOverviewMatcher.getOverviewImagePlus() );

				if( settings.saveResultsAsBdv )
					exportTemplate( matchedHighResTemplate );

			}
		}
	}

	private File getHighResFile( File lowResFile )
	{
		final String highResFileName = lowResFile.getName().replace( lowMagId, highMagId );

		for ( File file : templateFiles )
			if ( file.getName().equals( highResFileName ) )
				return file;

		return null;
	}

	private void openOverview()
	{
		rawOverview = openImage( settings.overviewImageFile );

		rotatedOverviewForExport = Processor.rotate2D( rawOverview, settings.overviewAngleDegrees );

		rotatedOverviewForExport = new DefaultCalibratedRai<>(
				Views.zeroMin( rotatedOverviewForExport.rai() ), rotatedOverviewForExport.nanometerCalibration() );

		if ( settings.matchingPixelSpacingNanometer != 0 )
		{
			final long[] overviewSubSampling = getOverviewSubSamplingXY( rawOverview, settings.matchingPixelSpacingNanometer );

			Utils.log( "Requested matching pixel size [nm]: " + settings.matchingPixelSpacingNanometer );
			Utils.log( "Sub-sampling overview image by " + overviewSubSampling[ 0 ] );

			subsampledOverviewForMatching = Processor.subSample( rotatedOverviewForExport, overviewSubSampling );
		}
		else
		{
			Utils.log( "Pixel size during matching (= pixel size overview image) [nm]: "
					+ rotatedOverviewForExport.nanometerCalibration()[ 0 ] );

			subsampledOverviewForMatching = rotatedOverviewForExport;
		}
	}


	private long[] getOverviewSubSamplingXY( CalibratedRai< T > overview, double matchingPixelSpacingNanometer )
	{

		final long[] subSampling = new long[ 2 ];

		for ( int d = 0; d < 2; d++ )
			subSampling[ d ] = (long) (
					matchingPixelSpacingNanometer
							/ overview.nanometerCalibration()[ d ] );

		if ( overview.rai().numDimensions() == 2 )
			return new long[]{ subSampling[ 0 ], subSampling[ 1 ] };
		else if ( overview.rai().numDimensions() == 3 )
			return new long[]{ subSampling[ 0 ], subSampling[ 1 ], 1 };
		else
			return null;
	}

	private double confirmImageScalingUI( double value, final String imageName )
	{
		final GenericDialog gd =
				new GenericDialog( imageName + " scaling" );
		gd.addNumericField(
				imageName + " pixel spacing",
				value, 20, 30, "nm" );
		gd.hideCancelButton();
		gd.showDialog();
		return gd.getNextNumber();
	}

	private boolean saveImagesAsBdvHdf5()
	{
		Utils.log( "# Saving results" );
		exportOverview( rawOverview, rawOverview.is3D, rawOverview.isMultiChannel );
		exportTemplates();
		return true;
	}

	private void exportTemplates()
	{
		for ( MatchedTemplate< T > template : matchedTemplates )
		{
			exportTemplate( template );
		}
	}

	private void exportTemplate( MatchedTemplate< T > template )
	{
		// TODO: maybe let the user confirm?!
		RandomAccessibleInterval< T > rai = template.calibratedRai.rai();

		String path = getOutputPath( template.file.getName() );
		Utils.log( "Exporting " + template.file.getName() );

		if ( rai.numDimensions() == 2 ) // add z-dimension
			rai = Views.addDimension( rai, 0, 0 );

		// add channel dimension
		rai = Views.addDimension( rai, 0, 0 );

		// add time dimension
		rai = Views.addDimension( rai, 0, 0 );

		new BdvRaiXYZCTExport< T >().export(
				rai,
				template.file.getName(),
				path,
				template.calibratedRai.nanometerCalibration(),
				"nanometer",
				template.matchedPositionNanometer );
	}

	private String getOutputPath( String name )
	{
		return settings.outputDirectory + File.separator + name;
	}

	private void exportOverview( CalibratedRai< T > overview, boolean is3D, boolean isMultiChannel )
	{
		Utils.log( "Exporting overview..." );

		double[] calibration = new double[ 3 ];
		calibration[ 0 ] = overview.nanometerCalibration()[ 0 ];
		calibration[ 1 ] = overview.nanometerCalibration()[ 1 ];
		calibration[ 2 ] = 2000;

		double[] translation = new double[ 3 ];

		String path = getOutputPath( "overview" );

		RandomAccessibleInterval< T > overviewRai = overview.rai();

		if ( !is3D ) // add z-dimension
			overviewRai = Views.addDimension( overviewRai, 0, 0 );

		if ( isMultiChannel ) // swap z and channel dimension
			overviewRai = Views.permute( overviewRai, 2, 3 );
		else
			overviewRai = Views.addDimension( overviewRai, 0, 0 );

		// add time dimension
		overviewRai = Views.addDimension( overviewRai, 0, 0 );

		new BdvRaiXYZCTExport< T >().export(
				Views.zeroMin( overviewRai ),
				"overview",
				path,
				calibration,
				"nanometer",
				translation );
	}


	private void showBestMatchOnOverview(
			MatchedTemplate matchedTemplate, ImagePlus overviewImagePlus )
	{
		final double[] position = matchedTemplate.matchedPositionNanometer;
		final double[] size = matchedTemplate.getImageSizeNanometer();

		final int[] pixelPosition = new int[ 2 ];
		pixelPosition[ 0 ] = ( int ) ( position[ 0 ] / overviewImagePlus.getCalibration().pixelWidth );
		pixelPosition[ 1 ] = ( int ) ( position[ 1 ] / overviewImagePlus.getCalibration().pixelHeight );

		final int[] templateSizePixel = new int[ 2 ];
		templateSizePixel[ 0 ] = ( int ) ( size[ 0 ] / overviewImagePlus.getCalibration().pixelWidth );
		templateSizePixel[ 1 ] = ( int ) ( size[ 1 ] / overviewImagePlus.getCalibration().pixelHeight );

		if ( overviewImagePlus.getOverlay() == null )
			overviewImagePlus.setOverlay( new Overlay() );

		overviewImagePlus.getOverlay().add( getRectangleRoi( pixelPosition, templateSizePixel ) );
		overviewImagePlus.getOverlay().add( getTextRoi( templateSizePixel ) );
		overviewImagePlus.show();
		overviewImagePlus.updateAndDraw();
	}

	private Roi getRectangleRoi( int[] position, int[] size )
	{
		Roi r = new Roi( position[ 0 ], position[ 1 ], size[ 0 ], size[ 1 ] );
		r.setStrokeColor( Color.green );
		return r;
	}

	private Roi getTextRoi( int[] bestMatch )
	{
		Roi r = new TextRoi(
				bestMatch[ 0 ] + 5, bestMatch[ 1 ] + 5,
				"" + templateIndex );
		r.setStrokeColor( Color.magenta );
		return r;
	}

	private CalibratedRaiPlus< T > openImage( File file )
	{
		Utils.log( "Opening: " + file );
		final CalibratedRaiPlus calibratedRaiPlus = ImageIO.withBFopenRAI( file );
		Utils.log( "Pixel size [nm]: " + calibratedRaiPlus.nanometerCalibration()[ 0 ] );
		return  calibratedRaiPlus;
	}


}
