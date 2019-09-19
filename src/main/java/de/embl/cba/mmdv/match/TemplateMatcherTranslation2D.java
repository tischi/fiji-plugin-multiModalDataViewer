package de.embl.cba.templatematching.match;

import de.embl.cba.templatematching.image.CalibratedRai;
import de.embl.cba.templatematching.Utils;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.FloatProcessor;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.algorithm.localextrema.SubpixelLocalization;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.ArrayList;

import static de.embl.cba.templatematching.Utils.*;
import static de.embl.cba.templatematching.process.Processor.*;
import static de.embl.cba.transforms.utils.Transforms.getCenter;

public class TemplateMatcherTranslation2D< T extends RealType< T > & NativeType< T > >
{
	public static final int CV_TM_SQDIFF = 0;
	public static final int CORRELATION = 4;
	public static final int NORMALIZED_CORRELATION = 5;

	private final CalibratedRai< T > overviewCalibratedRai;
	private ImagePlus overviewImagePlus;
	private CalibratedRai< T > processedTemplate;

	public TemplateMatcherTranslation2D( CalibratedRai< T > overviewCalibratedRai )
	{
		this.overviewCalibratedRai = overviewCalibratedRai;
		int addNoiseLevel = 5;
		setOverviewImagePlus( overviewCalibratedRai, addNoiseLevel );
	}

	public ImagePlus getOverviewImagePlus()
	{
		return overviewImagePlus;
	}

	public MatchedTemplate match( CalibratedRai< T > template )
	{
		// sub-sampling first makes projection faster
		final CalibratedRai< T > subSampled = subSample( template, getSubSamplingXY( template ) );

		CalibratedRai< T > projected = project( subSampled );

		processedTemplate = scale( projected, getScalingsXY( projected ) );

		showIntermediateResult( processedTemplate, "processed template" );

		final double[] calibratedPosition = findPositionWithinOverviewImage( processedTemplate.rai() );

		final MatchedTemplate matched = getMatchedTemplate( template, calibratedPosition );

		return matched;
	}

	public CalibratedRai< T > getProcessedTemplate()
	{
		return processedTemplate;
	}

	private void setOverviewImagePlus( CalibratedRai< T > calibratedRai, int addNoiseLevel )
	{
		asFloatProcessor( calibratedRai.rai(), addNoiseLevel );

		overviewImagePlus = new ImagePlus(
				"Overview",
				asFloatProcessor( calibratedRai.rai(), addNoiseLevel ) );

		final Calibration calibration = overviewImagePlus.getCalibration();
		calibration.pixelWidth = calibratedRai.nanometerCalibration()[ 0 ];
		calibration.pixelHeight = calibratedRai.nanometerCalibration()[ 1 ];
		calibration.setUnit( "nanometer" );
	}


	private MatchedTemplate getMatchedTemplate(
			CalibratedRai< T > template,
			double[] positionNanometer )
	{

		if ( template.rai().numDimensions() == 3)
		{
			// set z position to template center
			final double[] center = getCenter( template.rai() );
			positionNanometer[ 2 ] = ( -center[ 2 ] ) * template.nanometerCalibration()[ 2 ];
		}

		return new MatchedTemplate( template, positionNanometer);
	}

	private double[] getCalibratedPosition3D( double[] pixelPosition )
	{
		final double[] calibratedPosition = new double[ 3 ];

		for ( int d = 0; d < pixelPosition.length; d++ )
			calibratedPosition[ d ] = pixelPosition[ d ] * overviewCalibratedRai.nanometerCalibration()[ d ];

		return calibratedPosition;
	}


	private long[] getSubSamplingXY( CalibratedRai< T > template )
	{
		final int numDimensions = template.rai().numDimensions();

		final long[] subSampling = new long[ numDimensions ];

		for ( int d = 0; d < 2; d++ )
			subSampling[ d ] = (long) ( overviewCalibratedRai.nanometerCalibration()[ d ]
					/ template.nanometerCalibration()[ d ] );

		if ( numDimensions == 3 )
			subSampling[ 2 ] = 1;

		return subSampling;
	}

	private double[] getScalingsXY( CalibratedRai< T > calibratedRai )
	{
		final double[] scalings = new double[ 2 ];
		final double[] calibration = calibratedRai.nanometerCalibration();

		for ( int d = 0; d < 2; d++ )
			scalings[ d ] = calibration[ d ] / overviewCalibratedRai.nanometerCalibration()[ d ];

		return scalings;
	}

	private double[] findPositionWithinOverviewImage( RandomAccessibleInterval< T > template )
	{
		Utils.log( "Computing x-correlation..." );
		FloatProcessor correlation = TemplateMatchingPlugin.doMatch(
				overviewImagePlus.getProcessor(),
				asFloatProcessor( template ),
				NORMALIZED_CORRELATION );

		if ( showIntermediateResults )
			new ImagePlus( "correlation", correlation ).show();

		Utils.log( "Finding maximum in x-correlation..." );
		final int[] position = findMaximumPosition( correlation );

		Utils.log( "Refining maximum to sub-pixel resolution..." );
		final double[] refinedPosition = computeRefinedPosition( correlation, position );

		final double[] calibratedPosition = getCalibratedPosition3D( refinedPosition );

		return calibratedPosition;
	}

	private double[] computeRefinedPosition( FloatProcessor correlation, int[] position )
	{
		final RandomAccessibleInterval< T > correlationRai
				= ImageJFunctions.wrapReal( new ImagePlus( "", correlation ) );

		final int numDimensions = correlationRai.numDimensions();

		final SubpixelLocalization< Point, T > spl =
				new SubpixelLocalization< >( numDimensions );
		spl.setNumThreads( 1 );
		spl.setReturnInvalidPeaks( true );
		spl.setCanMoveOutside( true );
		spl.setAllowMaximaTolerance( true );
		spl.setMaxNumMoves( 10 );

		ArrayList peaks = new ArrayList< Point >(  );
		peaks.add( new Point( position[ 0 ], position[ 1 ] ) );

		final ArrayList< RefinedPeak< Point > > refined = spl.process(
				peaks,
				correlationRai,
				correlationRai );

		final RefinedPeak< Point > refinedPeak = refined.get( 0 );

		final double[] refinedPosition = new double[ numDimensions ];
		for ( int d = 0; d < numDimensions; d++ )
			refinedPosition[ d ] = refinedPeak.getDoublePosition( d );

		return refinedPosition;
	}

	private int[] findMaximumPosition( FloatProcessor processor )
	{
		final int[] maxPos = findMax( processor );
		return maxPos;
	}


}
