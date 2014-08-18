package plugin.google.maps;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class MyFrameLayout extends FrameLayout {
  public Rect mapRect = new Rect();
  
  public MyFrameLayout(Context context) {
    super(context);
  }
  
  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    int x = (int)event.getX();
    int y = (int)event.getY();
    return mapRect.contains(x, y);
  }
}
