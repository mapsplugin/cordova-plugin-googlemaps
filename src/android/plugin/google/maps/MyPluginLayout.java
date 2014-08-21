package plugin.google.maps;

import org.apache.cordova.CordovaWebView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;

public class MyPluginLayout extends FrameLayout  {
  private CordovaWebView webView;
  private ViewGroup root;
  private RectF drawRect = new RectF();
  private Context context;
  private FrontLayerLayout frontLayer;
  private ScrollView scrollView = null;
  private FrameLayout scrollFrameLayout = null;
  private View backgroundView = null;
  private TouchableWrapper touchableWrapper;
  private ViewGroup myView = null;
  
  public MyPluginLayout(CordovaWebView webView) {
    super(webView.getContext());
    this.webView = webView;
    this.root = (ViewGroup) webView.getParent();
    this.context = webView.getContext();
    webView.setBackgroundColor(Color.TRANSPARENT);
    frontLayer = new FrontLayerLayout(this.context);
    
    scrollView = new ScrollView(this.context);
    scrollView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

    backgroundView = new View(this.context);
    backgroundView.setBackgroundColor(Color.WHITE);
    backgroundView.setVerticalScrollBarEnabled(false);
    backgroundView.setHorizontalScrollBarEnabled(false);
    backgroundView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, (int) (webView.getContentHeight() * webView.getScale() + webView.getHeight())));
    
    scrollFrameLayout = new FrameLayout(this.context);
    scrollFrameLayout.addView(backgroundView);
    scrollFrameLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    
    this.touchableWrapper = new TouchableWrapper(this.context);
  }
  
  public void setDrawingRect(float left, float top, float right, float bottom) {
    this.drawRect.left = left;
    this.drawRect.top = top;
    this.drawRect.right = right;
    this.drawRect.bottom = bottom;
  }

  public void detachMyView() {
    root.removeView(this);
    this.removeView(frontLayer);
    frontLayer.removeView(webView);
    
    scrollFrameLayout.removeView(myView);
    myView.removeView(this.touchableWrapper);
    
    this.removeView(this.scrollView);
    this.scrollView.removeView(scrollFrameLayout);
    
    root.addView(webView);
  }
  
  public void attachMyView(ViewGroup pluginView) {
    backgroundView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, (int) (webView.getContentHeight() * webView.getScale() + webView.getHeight())));
    
    myView = pluginView;
    root.removeView(webView);
    scrollView.addView(scrollFrameLayout);
    this.addView(scrollView);
    
    pluginView.addView(this.touchableWrapper);
    scrollFrameLayout.addView(pluginView);
    
    frontLayer.addView(webView);
    this.addView(frontLayer);
    
    root.addView(this);
  }
  
  public void setPageSize(int width, int height) {
    android.view.ViewGroup.LayoutParams lParams = backgroundView.getLayoutParams();
    lParams.width = width;
    lParams.height = height;
    backgroundView.setLayoutParams(lParams);
  }
  
  public void scrollTo(int x, int y) {
    this.scrollView.scrollTo(x, y);
  }

  
  public void setBackgroundColor(int color) {
    this.backgroundView.setBackgroundColor(color);
  }
  

  private class FrontLayerLayout extends FrameLayout {
    
    public FrontLayerLayout(Context context) {
      super(context);
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
      int x = (int)event.getX();
      int y = (int)event.getY();
      return drawRect.contains(x, y);
    }
  }
  
  private class TouchableWrapper extends FrameLayout {
    
    public TouchableWrapper(Context context) {
      super(context);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
      int action = event.getAction();
      if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
        scrollView.requestDisallowInterceptTouchEvent(true);
      }
      return super.dispatchTouchEvent(event);
    }
  } 
}
