package plugin.google.maps;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class MyFrameLayout extends FrameLayout {

  public MyFrameLayout(Context context) {
    super(context);
  }
  
  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    return true;
  }
}
