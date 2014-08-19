package plugin.google.maps;

import android.content.Context;
import android.widget.ScrollView;

public class MyScrollView extends ScrollView {
  public interface ScrollViewListener {
    void onScrollChanged(MyScrollView scrollView, int x, int y, int oldx, int oldy);
  }
  
  private ScrollViewListener scrollViewListener = null;
  
  public MyScrollView(Context context) {
    super(context);
  }
  
  public void setOnScrollViewListener(ScrollViewListener scrollViewListener) {
    this.scrollViewListener = scrollViewListener;
  }
  
  @Override
  protected void onScrollChanged(int x, int y, int oldx, int oldy) {
    super.onScrollChanged(x, y, oldx, oldy);
    if (scrollViewListener != null) {
      scrollViewListener.onScrollChanged(this, x, y, oldx, oldy);
    }
  }
}