package plugin.google.maps;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

public class MyScrollView extends ScrollView {
  
  public interface ScrollViewListener {
    void onScrollChanged(MyScrollView scrollView, int x, int y, int oldx, int oldy);
    void onEndScroll();
  }
  
  private Runnable scrollerTask;
  private int initialPosition;
  
  private ScrollViewListener scrollViewListener = null;
  
  public MyScrollView(Context context) {
    super(context);
    
    scrollerTask = new Runnable() {
      public void run() {
          if (initialPosition - getScrollY() == 0) {//has stopped
            if (scrollViewListener != null) {
              scrollViewListener.onEndScroll();
            }
          } else {
            initialPosition = getScrollY();
            MyScrollView.this.postDelayed(scrollerTask, 100);
          }
      }
    };
  }
  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    return true;
  }
  
  public void setOnScrollViewListener(ScrollViewListener scrollViewListener) {
    this.scrollViewListener = scrollViewListener;
  }
  
  @Override
  protected void onScrollChanged(int x, int y, int oldX, int oldY) {
    super.onScrollChanged(x, y, oldX, oldY);
    if (scrollViewListener != null) {
      scrollViewListener.onScrollChanged(this, x, y, oldX, oldY);
    }
  }

  public void startScrollerTask() {
    initialPosition = getScrollY();
    MyScrollView.this.postDelayed(scrollerTask, 100);
  }

}