package plugin.google.maps;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.cordova.CordovaWebView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Build;
import android.os.Build.VERSION;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;

@SuppressWarnings("deprecation")
public class MyPluginLayout extends FrameLayout  {
  private CordovaWebView webView;
  private ViewGroup root;
  private RectF drawRect = new RectF();
  private Context context;
  private FrontLayerLayout frontLayer;
  private ScrollView scrollView = null;
  private TouchableWrapper scrollFrameLayout = null;
  private View backgroundView = null;
  private MapView myView = null;
  private boolean isScrolling = false;
  private ViewGroup.LayoutParams orgLayoutParams = null;
  private boolean isDebug = false;
  private boolean isClickable = true;
  private Map<String, RectF> HTMLNodes = new HashMap<String, RectF>();
  private Activity mActivity = null;
  
  @SuppressLint("NewApi")
  public MyPluginLayout(CordovaWebView webView, Activity activity) {
    super(webView.getContext());
    mActivity = activity;
    this.webView = webView;
    this.root = (ViewGroup) webView.getParent();
    this.context = webView.getContext();
    webView.setBackgroundColor(Color.TRANSPARENT);
    if (VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
    frontLayer = new FrontLayerLayout(this.context);
    
    scrollView = new ScrollView(this.context);
    scrollView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

    backgroundView = new View(this.context);
    backgroundView.setBackgroundColor(Color.WHITE);
    backgroundView.setVerticalScrollBarEnabled(false);
    backgroundView.setHorizontalScrollBarEnabled(false);
    backgroundView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 9999));
    
    scrollFrameLayout = new TouchableWrapper(this.context);
    scrollFrameLayout.addView(backgroundView);
    scrollFrameLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    
    scrollView.setHorizontalScrollBarEnabled(false);
    scrollView.setVerticalScrollBarEnabled(false);
    
    //this.touchableWrapper = new TouchableWrapper(this.context);
    
  }
  
  public void setDrawingRect(float left, float top, float right, float bottom) {
    this.drawRect.left = left;
    this.drawRect.top = top;
    this.drawRect.right = right;
    this.drawRect.bottom = bottom;
    if (this.isDebug == true) {
      this.inValidate();
    }
  }
  
  public void putHTMLElement(String domId, float left, float top, float right, float bottom) {
    RectF rect = null;
    if (this.HTMLNodes.containsKey(domId)) {
      rect = this.HTMLNodes.get(domId);
    } else {
      rect = new RectF();
    }
    rect.left = left;
    rect.top = top;
    rect.right = right;
    rect.bottom = bottom;
    this.HTMLNodes.put(domId, rect);
    if (this.isDebug == true) {
      this.inValidate();
    }
  }
  public void removeHTMLElement(String domId) {
    this.HTMLNodes.remove(domId);
    if (this.isDebug == true) {
      this.inValidate();
    }
  }
  public void clearHTMLElement() {
    this.HTMLNodes.clear();
    if (this.isDebug == true) {
      this.inValidate();
    }
  }

  public void setClickable(boolean clickable) {
    this.isClickable = clickable;
    if (this.isDebug == true) {
      this.inValidate();
    }
  }
  
  public void updateViewPosition() {
    if (myView == null) {
      return;
    }
    ViewGroup.LayoutParams lParams = this.myView.getLayoutParams();
    int scrollY = webView.getScrollY();

    if (lParams instanceof AbsoluteLayout.LayoutParams) {
      AbsoluteLayout.LayoutParams params = (AbsoluteLayout.LayoutParams) lParams;
      params.width = (int) this.drawRect.width();
      params.height = (int) this.drawRect.height();
      params.x = (int) this.drawRect.left;
      params.y = (int) this.drawRect.top + scrollY;
      myView.setLayoutParams(params);
    } else if (lParams instanceof LinearLayout.LayoutParams) {
      LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lParams;
      params.width = (int) this.drawRect.width();
      params.height = (int) this.drawRect.height();
      params.topMargin = (int) this.drawRect.top + scrollY;
      params.leftMargin = (int) this.drawRect.left;
      myView.setLayoutParams(params);
    } else if (lParams instanceof FrameLayout.LayoutParams) {
      FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lParams;
      params.width = (int) this.drawRect.width();
      params.height = (int) this.drawRect.height();
      params.topMargin = (int) this.drawRect.top + scrollY;
      params.leftMargin = (int) this.drawRect.left;
      params.gravity = Gravity.TOP;
      myView.setLayoutParams(params);
    } 
    if (android.os.Build.VERSION.SDK_INT < 11) {
      // Force redraw
      myView.requestLayout();
    }
    this.frontLayer.invalidate();
  }

  public View getMyView() {
    return myView;
  }
  public void setDebug(boolean debug) {
    this.isDebug = debug;
    if (this.isDebug == true) {
      this.inValidate();
    }
  }

  public void detachMyView() {
    if (myView == null) {
      return;
    }
    root.removeView(this);
    this.removeView(frontLayer);
    frontLayer.removeView(webView);
    
    scrollFrameLayout.removeView(myView);
    
    this.removeView(this.scrollView);
    this.scrollView.removeView(scrollFrameLayout);
    if (orgLayoutParams != null) {
      myView.setLayoutParams(orgLayoutParams);
    }
    
    root.addView(webView);
    myView = null;
    mActivity.getWindow().getDecorView().requestFocus();
  }
  
  public void attachMyView(MapView mapView) {
    scrollView.setHorizontalScrollBarEnabled(false);
    scrollView.setVerticalScrollBarEnabled(false);
    
    scrollView.scrollTo(webView.getScrollX(), webView.getScrollY());
    if (myView == mapView) {
      return;
    } else {
      this.detachMyView();
    }
    //backgroundView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, (int) (webView.getContentHeight() * webView.getScale() + webView.getHeight())));
    
    myView = mapView;
    ViewGroup.LayoutParams lParams = myView.getLayoutParams();
    orgLayoutParams = null;
    if (lParams != null) {
      orgLayoutParams = new ViewGroup.LayoutParams(lParams);
    }
    root.removeView(webView);
    scrollView.addView(scrollFrameLayout);
    this.addView(scrollView);
    
    scrollFrameLayout.addView(mapView);
    
    frontLayer.addView(webView);
    this.addView(frontLayer);
    root.addView(this);
    mActivity.getWindow().getDecorView().requestFocus();
    
    scrollView.setHorizontalScrollBarEnabled(true);
    scrollView.setVerticalScrollBarEnabled(true);
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
  
  public void inValidate() {
    this.frontLayer.invalidate();
  }
  
  
  private class FrontLayerLayout extends FrameLayout {
    
    public FrontLayerLayout(Context context) {
      super(context);
      this.setWillNotDraw(false);
    }
    
    /**
     * True if the touch event should be received by MapView.
     * False, the event is received by WebView.
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
      if (isClickable == false || myView == null || myView.getVisibility() != View.VISIBLE) {
        webView.requestFocus(View.FOCUS_DOWN);
        return false;
      }
      int x = (int)event.getX();
      int y = (int)event.getY();
      int scrollY = webView.getScrollY();
      boolean contains = drawRect.contains(x, y);
      int action = event.getAction();
      isScrolling = (contains == false && action == MotionEvent.ACTION_DOWN) ? true : isScrolling;
      isScrolling = (action == MotionEvent.ACTION_UP) ? false : isScrolling;
      contains = isScrolling == true ? false : contains;
      
      if (contains) {
        // Is the touch point on any HTML elements?
        Set<Entry<String, RectF>> elements = MyPluginLayout.this.HTMLNodes.entrySet();
        Iterator<Entry<String, RectF>> iterator = elements.iterator();
        Entry <String, RectF> entry;
        RectF rect;
        while(iterator.hasNext() && contains == true) {
          entry = iterator.next();
          rect = entry.getValue();
          rect.top -= scrollY;
          rect.bottom -= scrollY;
          if (entry.getValue().contains(x, y)) {
            contains = false;
          }
          rect.top += scrollY;
          rect.bottom += scrollY;
        }
      }
      if (!contains) {
        webView.requestFocus(View.FOCUS_DOWN);
      }
      return contains;
    }
    @Override
    protected void onDraw(Canvas canvas) {
      if (drawRect == null || isDebug == false) {
        return;
      }
      int width = canvas.getWidth();
      int height = canvas.getHeight();
      int scrollY = webView.getScrollY();
      
      Paint paint = new Paint();
      paint.setColor(Color.argb(100, 0, 255, 0));
      if (isClickable == false) {
        canvas.drawRect(0f, 0f, width, height, paint);
        return;
      }
      canvas.drawRect(0f, 0f, width, drawRect.top, paint);
      canvas.drawRect(0, drawRect.top, drawRect.left, drawRect.bottom, paint);
      canvas.drawRect(drawRect.right, drawRect.top, width, drawRect.bottom, paint);
      canvas.drawRect(0, drawRect.bottom, width, height, paint);
      
      
      paint.setColor(Color.argb(100, 255, 0, 0));
      
      Set<Entry<String, RectF>> elements = MyPluginLayout.this.HTMLNodes.entrySet();
      Iterator<Entry<String, RectF>> iterator = elements.iterator();
      Entry <String, RectF> entry;
      RectF rect;
      while(iterator.hasNext()) {
        entry = iterator.next();
        rect = entry.getValue();
        rect.top -= scrollY;
        rect.bottom -= scrollY;
        canvas.drawRect(rect, paint);
        rect.top += scrollY;
        rect.bottom += scrollY;
      }
    }
  }
  
  private class TouchableWrapper extends FrameLayout {
    
    public TouchableWrapper(Context context) {
      super(context);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
      if (isClickable == false || myView == null || myView.getVisibility() != View.VISIBLE) {
        webView.requestFocus(View.FOCUS_DOWN);
        return false;
      }
      String eventName = null;
      switch(event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        eventName = "touchstart";
        break;
      case MotionEvent.ACTION_MOVE:
        eventName = "touchmove";
        break;
      case MotionEvent.ACTION_UP:
        eventName = "touchend";
        break;
      case MotionEvent.ACTION_CANCEL:
        eventName = "touchcancel";
        break;
      case MotionEvent.ACTION_OUTSIDE:
        eventName = "touchleave";
        break;
      default:
        break;
      }
      int x = (int)event.getX() - myView.getLeft();
      int y = (int)event.getY() - myView.getTop();
      Projection projection = myView.getMap().getProjection();
      Point point = new Point(x, y);
      LatLng latLng = projection.fromScreenLocation(point);
      webView.loadUrl(
          String.format("javascript:plugin.google.maps.Map._onTouchEvent('%s', [%d, %d], new window.plugin.google.maps.LatLng(%f, %f));", 
              eventName, x, y, latLng.latitude, latLng.longitude));
      
      return false;
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
