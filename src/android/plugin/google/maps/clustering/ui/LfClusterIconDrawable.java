package plugin.google.maps.clustering.ui;

import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import de.lf.erdgas.R;

/**
 * Created by christian on 02.06.15.
 */
public class LfClusterIconDrawable extends Drawable {

	private final Drawable mMask;
	private int mColor = Color.WHITE;

	public LfClusterIconDrawable(Resources res) {
		mMask = res.getDrawable(R.drawable.cluster_icon);
	}

	public void setColor(int color) {
		mColor = color;
	}

	@Override
	public void draw(Canvas canvas) {
		mMask.draw(canvas);
		canvas.drawColor(mColor, PorterDuff.Mode.SRC_IN);
	}

	@Override
	public void setAlpha(int alpha) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public void setBounds(int left, int top, int right, int bottom) {
		mMask.setBounds(left, top, right, bottom);
	}

	@Override
	public void setBounds(Rect bounds) {
		mMask.setBounds(bounds);
	}

	@Override
	public boolean getPadding(Rect padding) {
		return mMask.getPadding(padding);
	}

}
