package plugin.google.maps;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.google.android.gms.maps.MapView;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@SuppressWarnings("deprecation")
public class MyPluginLayout extends FrameLayout  {
  private View browserView;
  private ViewGroup root;
  private HashMap<String, RectF> drawRects = new HashMap<String, RectF>();
  private Context context;
  private FrontLayerLayout frontLayer;
  private ScrollView scrollView = null;
  public FrameLayout scrollFrameLayout = null;
  private HashMap<String, PluginMap> pluginMaps = new HashMap<String, PluginMap>();
  private HashMap<String, TouchableWrapper> touchableWrappers = new HashMap<String, TouchableWrapper>();
  private boolean isScrolling = false;
  private boolean isDebug = false;
  private HashMap<String, HashMap<String, RectF>> MAP_HTMLNodes = new HashMap<String, HashMap<String, RectF>>();
  private Activity mActivity = null;
  private Paint debugPaint = new Paint();
  
  @SuppressLint("NewApi")
  public MyPluginLayout(View browserView, Activity activity) {
    super(browserView.getContext());
    mActivity = activity;
    this.browserView = browserView;
    this.root = (ViewGroup) browserView.getParent();
    this.context = browserView.getContext();
    //if (VERSION.SDK_INT >= 21 || "org.xwalk.core.XWalkView".equals(browserView.getClass().getName())) {
    //  browserView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    //}

    frontLayer = new FrontLayerLayout(this.context);

    scrollView = new ScrollView(this.context);
    scrollView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

    root.removeView(browserView);
    frontLayer.addView(browserView);

    scrollFrameLayout = new FrameLayout(this.context);
    scrollFrameLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));


    View dummyView = new View(this.context);
    dummyView.setLayoutParams(new LayoutParams(1, 99999));
    scrollFrameLayout.addView(dummyView);

    scrollView.setHorizontalScrollBarEnabled(true);
    scrollView.setVerticalScrollBarEnabled(true);
    scrollView.addView(scrollFrameLayout);



    this.addView(scrollView);
    this.addView(frontLayer);
    root.addView(this);



    browserView.setBackgroundColor(Color.TRANSPARENT);
    if("org.xwalk.core.XWalkView".equals(browserView.getClass().getName())
      || "org.crosswalk.engine.XWalkCordovaView".equals(browserView.getClass().getName())) {
      try {
    /* view.setZOrderOnTop(true)
     * Called just in time as with root.setBackground(...) the color
     * come in front and take the whole screen */
        browserView.getClass().getMethod("setZOrderOnTop", boolean.class)
          .invoke(browserView, true);
      }
      catch(Exception e) {
        e.printStackTrace();
      }
    }
    scrollView.setHorizontalScrollBarEnabled(false);
    scrollView.setVerticalScrollBarEnabled(false);

    //backgroundView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, (int) (view.getContentHeight() * view.getScale() + view.getHeight())));


    mActivity.getWindow().getDecorView().requestFocus();
  }
  
  public void setDrawingRect(String mapId, float left, float top, float right, float bottom) {
    RectF drawRect = drawRects.get(mapId);
    drawRect.left = left;
    drawRect.top = top;
    drawRect.right = right;
    drawRect.bottom = bottom;
    this.updateViewPosition(mapId);
  }
  
  public void putHTMLElement(String mapId, String domId, float left, float top, float right, float bottom) {
    //Log.d("MyPluginLayout", "--> putHTMLElement / mapId = " + mapId + ", domId = " + domId);
    //Log.d("MyPluginLayout", "--> putHTMLElement / " + left + ", " + top + " - " + right + ", " + bottom);
    HashMap<String, RectF> HTMLNodes;
    if (MAP_HTMLNodes.containsKey(mapId)) {
      HTMLNodes = MAP_HTMLNodes.get(mapId);
    } else {
      HTMLNodes = new HashMap<String, RectF>();
      MAP_HTMLNodes.put(mapId, HTMLNodes);
    }

    RectF rect;
    if (HTMLNodes.containsKey(domId)) {
      rect = HTMLNodes.get(domId);
    } else {
      rect = new RectF();
    }
    rect.left = left;
    rect.top = top;
    rect.right = right;
    rect.bottom = bottom;
    HTMLNodes.put(domId, rect);
    if (this.isDebug) {
      this.inValidate();
    }
  }
  public void removeHTMLElement(String mapId, String domId) {
    if (!MAP_HTMLNodes.containsKey(mapId)) {
      return;
    }
    HashMap<String, RectF> HTMLNodes = MAP_HTMLNodes.get(mapId);
    HTMLNodes.remove(domId);
    if (this.isDebug) {
      this.inValidate();
    }
  }
  public void clearHTMLElement(String mapId) {
    if (!MAP_HTMLNodes.containsKey(mapId)) {
      return;
    }
    HashMap<String, RectF> HTMLNodes = MAP_HTMLNodes.get(mapId);
    HTMLNodes.clear();
    MAP_HTMLNodes.remove(mapId);
    if (this.isDebug) {
      this.inValidate();
    }
  }

  
  public void updateViewPosition(final String mapId) {
    //Log.d("MyPluginLayout", "---> updateViewPosition / mapId = " + mapId);

    if (!pluginMaps.containsKey(mapId)) {
      return;
    }


    mActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        PluginMap pluginMap = pluginMaps.get(mapId);
        ViewGroup.LayoutParams lParams = pluginMap.mapView.getLayoutParams();
        //int scrollX = browserView.getScrollX();
        int scrollY = browserView.getScrollY();
        int webviewWidth = browserView.getWidth();
        int webviewHeight = browserView.getHeight();
        RectF drawRect = drawRects.get(mapId);

        if (lParams instanceof AbsoluteLayout.LayoutParams) {
          AbsoluteLayout.LayoutParams params = (AbsoluteLayout.LayoutParams) lParams;
          params.width = (int) drawRect.width();
          params.height = (int) drawRect.height();
          params.x = (int) drawRect.left;
          params.y = (int) drawRect.top + scrollY;
          pluginMap.mapView.setLayoutParams(params);
        } else if (lParams instanceof LinearLayout.LayoutParams) {
          LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lParams;
          params.width = (int) drawRect.width();
          params.height = (int) drawRect.height();
          params.topMargin = (int) drawRect.top + scrollY;
          params.leftMargin = (int) drawRect.left;
          pluginMap.mapView.setLayoutParams(params);
        } else if (lParams instanceof FrameLayout.LayoutParams) {
          FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lParams;
          params.width = (int) drawRect.width();
          params.height = (int) drawRect.height();
          params.topMargin = (int) drawRect.top + scrollY;
          params.leftMargin = (int) drawRect.left;
          params.gravity = Gravity.TOP;
          pluginMap.mapView.setLayoutParams(params);
        }

        if ((drawRect.top + drawRect.height() < 0) ||
          (drawRect.top >  webviewHeight) ||
          (drawRect.left + drawRect.width() < 0) ||
          (drawRect.left > webviewWidth))  {

          pluginMap.mapView.setVisibility(View.INVISIBLE);
        } else {
          pluginMap.mapView.setVisibility(View.VISIBLE);
        }
        frontLayer.invalidate();
      }
    });
  }


  public void setDebug(final boolean debug) {
    this.isDebug = debug;
    mActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (debug) {
          inValidate();
        }
      }
    });
  }

  public void removePluginMap(final String mapId) {
    if (!pluginMaps.containsKey(mapId)) {
      return;
    }

    mActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        PluginMap pluginMap = pluginMaps.remove(mapId);

        scrollFrameLayout.removeView(pluginMap.mapView);
        pluginMap.mapView.removeView(touchableWrappers.remove(mapId));
        drawRects.remove(mapId);

        mActivity.getWindow().getDecorView().requestFocus();
      }
    });
  }
  
  public void addPluginMap(final PluginMap pluginMap) {
    if (pluginMaps.containsKey(pluginMap.mapId)) {
      return;
    } else {
      removePluginMap(pluginMap.mapId);
    }
    MAP_HTMLNodes.put(pluginMap.mapId, new HashMap<String, RectF>());
    drawRects.put(pluginMap.mapId, new RectF());

    mActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {

        scrollView.scrollTo(browserView.getScrollX(), browserView.getScrollY());

        pluginMaps.put(pluginMap.mapId, pluginMap);

        TouchableWrapper wrapper = new TouchableWrapper(context);
        touchableWrappers.put(pluginMap.mapId, wrapper);
        pluginMap.mapView.addView(wrapper);
        scrollFrameLayout.addView(pluginMap.mapView);

        mActivity.getWindow().getDecorView().requestFocus();

      }
    });
  }

  public void scrollTo(int x, int y) {
    this.scrollView.scrollTo(x, y);
  }

  public void inValidate() {
    this.frontLayer.invalidate();
  }
  

  private class FrontLayerLayout extends FrameLayout {
    
    public FrontLayerLayout(Context context) {
      super(context);
      this.setWillNotDraw(false);
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {

      int action = event.getAction();
      //Log.d("FrontLayerLayout", "----> action = " + MotionEvent.actionToString(action) + ", isScrolling = " + isScrolling);

      // The scroll action that started in the browser region is end.
      isScrolling = action != MotionEvent.ACTION_UP && isScrolling;
      if (isScrolling) {
        return false;
      }

      PluginMap pluginMap;
      Iterator<Map.Entry<String, PluginMap>> iterator =  pluginMaps.entrySet().iterator();
      Entry<String, PluginMap> entry;
      HashMap<String, RectF> HTMLNodes;
      String mapId;
      int x = (int)event.getX();
      int y = (int)event.getY();
      int scrollY = browserView.getScrollY();
      RectF drawRect;
      boolean contains = false;


      while(iterator.hasNext()) {
        entry = iterator.next();
        mapId = entry.getKey();
        pluginMap = entry.getValue();
        if (!pluginMap.isVisible || !pluginMap.isClickable) {
          continue;
        }

        drawRect = drawRects.get(mapId);
        contains = drawRect.contains(x, y);


        if (contains && MAP_HTMLNodes.containsKey(mapId)) {
          // Is the touch point on any HTML elements?
          HTMLNodes = MAP_HTMLNodes.get(mapId);
          Set<Entry<String, RectF>> elements = HTMLNodes.entrySet();
          Iterator<Entry<String, RectF>> iterator2 = elements.iterator();
          Entry <String, RectF> entry2;
          RectF rect;
          while(iterator2.hasNext() && contains) {
            entry2 = iterator2.next();
            rect = entry2.getValue();
            rect.top -= scrollY;
            rect.bottom -= scrollY;


            if (entry2.getValue().contains(x, y)) {
              contains = false;
            }
            rect.top += scrollY;
            rect.bottom += scrollY;
          }
        }
        if (contains) {
          break;
        }
      }
      isScrolling = (!contains && action == MotionEvent.ACTION_DOWN) || isScrolling;
      contains = !isScrolling && contains;

      if (!contains) {
        browserView.requestFocus(View.FOCUS_DOWN);
      }


      return contains;
    }
    @Override
    protected void onDraw(Canvas canvas) {
      if (drawRects.isEmpty() || !isDebug) {
        return;
      }
      int width = canvas.getWidth();
      int height = canvas.getHeight();
      int scrollY = browserView.getScrollY();

      RectF drawRect;
      Iterator<Entry<String, HashMap<String, RectF>>> iterator = MAP_HTMLNodes.entrySet().iterator();
      Entry<String, HashMap<String, RectF>> entry;
      HashMap<String, RectF> HTMLNodes;
      String mapId;
      Set<Entry<String, RectF>> elements;
      Iterator<Entry<String, RectF>> iterator2;
      Entry <String, RectF> entry2;
      while(iterator.hasNext()) {
        entry = iterator.next();
        mapId = entry.getKey();
        drawRect = drawRects.get(mapId);
        //Log.d("MyPluginLayout", "---> mapId = " + mapId + ", drawRect = " + drawRect.left + "," + drawRect.top + " - " + drawRect.width() + ", " + drawRect.height());

        debugPaint.setColor(Color.argb(100, 0, 255, 0));
        canvas.drawRect(0f, 0f, width, drawRect.top, debugPaint);
        canvas.drawRect(0, drawRect.top, drawRect.left, drawRect.bottom, debugPaint);
        canvas.drawRect(drawRect.right, drawRect.top, width, drawRect.bottom, debugPaint);
        canvas.drawRect(0, drawRect.bottom, width, height, debugPaint);


        debugPaint.setColor(Color.argb(100, 255, 0, 0));
        HTMLNodes = MAP_HTMLNodes.get(mapId);

        elements = HTMLNodes.entrySet();
        iterator2 = elements.iterator();
        RectF rect;
        while(iterator2.hasNext()) {
          entry2 = iterator2.next();
          rect = entry2.getValue();
          rect.top -= scrollY;
          rect.bottom -= scrollY;
          canvas.drawRect(rect, debugPaint);
          rect.top += scrollY;
          rect.bottom += scrollY;
        }

      }


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
