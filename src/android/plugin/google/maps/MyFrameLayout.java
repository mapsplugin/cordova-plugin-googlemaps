package plugin.google.maps;

import plugin.google.maps.MyScrollView.ScrollViewListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.webkit.WebView;
import android.widget.FrameLayout;

public class MyFrameLayout extends FrameLayout {
  public Rect mapRect = new Rect();
  private Context context;
  
  public MyScrollView backgroundScrollView = null;
  public FrameLayout dummyLayout;
  
  public MyFrameLayout(Context context) {
    super(context);
    this.context = context;
    init();
  }
  
  public void init() {
    backgroundScrollView = new MyScrollView(this.context);
    backgroundScrollView.setBackgroundColor(Color.argb(30, 0, 0, 255));
    backgroundScrollView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    
    dummyLayout = new FrameLayout(this.context);
    dummyLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    dummyLayout.setBackgroundColor(Color.TRANSPARENT);
    backgroundScrollView.addView(dummyLayout);
    
    this.setBackgroundColor(Color.TRANSPARENT);
    this.addView(backgroundScrollView);
  }
  
  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    int x = (int)event.getX();
    int y = (int)event.getY();
    return mapRect.contains(x, y);
  }
  

  @Override
  protected void onDraw(Canvas canvas) {
    //Log.d("GoogleMaps", "mapRect=" + mapRect);
    if (mapRect == null) {
      return;
    }
    int width = canvas.getWidth();
    int height = canvas.getHeight();
    
    Paint paint = new Paint();
    paint.setColor(Color.argb(177, 0, 255, 0));
    canvas.drawRect(0f, 0f, (float)canvas.getWidth(), (float)mapRect.top, paint);
    canvas.drawRect(0, (float)mapRect.top, (float)mapRect.left, (float)mapRect.bottom, paint);
    canvas.drawRect((float)mapRect.right, (float)mapRect.top, (float)width, (float)mapRect.bottom, paint);
    canvas.drawRect(0, (float)mapRect.bottom, (float)width, (float)height, paint);
  }
}
