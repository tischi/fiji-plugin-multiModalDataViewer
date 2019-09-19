package de.embl.cba.mmdv;

import bdv.util.BdvHandle;
import net.imglib2.realtransform.AffineTransform3D;

public class BdvUtils
{
	public static void moveBdvViewToAxialZeroPosition( BdvHandle bdvHandle )
	{
		final AffineTransform3D viewerTransform = new AffineTransform3D();
		bdvHandle.getViewerPanel()
				.getState().getViewerTransform( viewerTransform );
		final double[] translation = viewerTransform.getTranslation();
		translation[ 2 ] = 0;
		viewerTransform.setTranslation( translation );
		bdvHandle.getViewerPanel().setCurrentViewerTransform( viewerTransform );
	}
}
