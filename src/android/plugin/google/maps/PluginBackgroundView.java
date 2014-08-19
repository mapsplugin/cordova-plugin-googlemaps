package plugin.google.maps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class PluginBackgroundView extends View {
  public Rect mapRect = new Rect();

  public PluginBackgroundView(Context context) {
    super(context);
    Log.d("GoogleMaps", "PluginBackgroundView is created");
  }

  @Override
  protected void onDraw(Canvas canvas) {
    //Log.d("GoogleMaps", "mapRect=" + mapRect);
    if (mapRect == null) {
      return;
    }
    int width = canvas.getWidth();
    int height = canvas.getHeight();
    Log.d("GoogleMaps", "width=" + width + ",height=" + height);
    
    Paint paint = new Paint();
    paint.setColor(Color.argb(177, 255, 255, 255));
    canvas.drawRect(0f, 0f, (float)canvas.getWidth(), (float)mapRect.top, paint);
    canvas.drawRect(0, (float)mapRect.top, (float)mapRect.left, (float)mapRect.bottom, paint);
    canvas.drawRect((float)mapRect.right, (float)mapRect.top, (float)width, (float)mapRect.bottom, paint);
    canvas.drawRect(0, (float)mapRect.bottom, (float)width, (float)height, paint);
  }
}
