package plugin.google.maps;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.TranslateAnimation;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.apache.cordova.CordovaWebView;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("deprecation")
public class MyPluginLayout extends FrameLayout implements ViewTreeObserver.OnScrollChangedListener, ViewTreeObserver.OnGlobalLayoutListener {
  private static final String TAG = "MyPluginLayout";
  private CordovaWebView webView;
  private View browserView;
  private ViewGroup root;
  private Context context;
  private FrontLayerLayout frontLayer;
  private ScrollView scrollView = null;
  public FrameLayout scrollFrameLayout = null;
  public HashMap<String, PluginMap> pluginMaps = new HashMap<String, PluginMap>();
  private HashMap<String, TouchableWrapper> touchableWrappers = new HashMap<String, TouchableWrapper>();
  private boolean isScrolling = false;
  public boolean isDebug = false;
  public HashMap<String, Bundle> HTMLNodes = new HashMap<String, Bundle>();
  public HashMap<String, RectF> HTMLNodeRectFs = new HashMap<String, RectF>();
  private Activity mActivity = null;
  private Paint debugPaint = new Paint();
  public boolean stopFlag = false;
  public boolean needUpdatePosition = false;
  public boolean isSuspended = false;
  private float zoomScale;
  public final Object timerLock = new Object();
  public boolean isWaiting = false;

  public Timer redrawTimer;

  @Override
  public void onGlobalLayout() {
    ViewTreeObserver observer = browserView.getViewTreeObserver();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      observer.removeOnGlobalLayoutListener(this);
    } else {
      observer.removeGlobalOnLayoutListener(this);
    }
    observer.addOnScrollChangedListener(this);
  }


  private class ResizeTask extends TimerTask {
    @Override
    public void run() {
      if (isSuspended) {
        //Log.d(TAG, "--->ResizeTask : isSuspended = " +isSuspended);
        synchronized (timerLock) {
          isWaiting = true;
          try {
            timerLock.wait();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        return;
      }
      isWaiting = false;
      //final PluginMap pluginMap = pluginMaps.get(mapId);
      //if (pluginMap.mapDivId == null) {
      //  return;
      //}
      //int scrollX = browserView.getScrollX();
      final int scrollY = browserView.getScrollY();
      //final int webviewWidth = browserView.getWidth();
      //final int webviewHeight = browserView.getHeight();

      mActivity.runOnUiThread(new Runnable() {
        @Override
        public void run() {

          Set<String> keySet = pluginMaps.keySet();
          String[] toArrayBuf = new String[pluginMaps.size()];
          String[] mapIds = keySet.toArray(toArrayBuf);
          toArrayBuf = null;
          keySet = null;
          String mapId;
          PluginMap pluginMap;
          RectF drawRect;
          for (int i = 0; i < mapIds.length; i++) {
            mapId = mapIds[i];
            pluginMap = pluginMaps.get(mapId);
            if (pluginMap == null || pluginMap.mapDivId == null) {
              continue;
            }
            drawRect = HTMLNodeRectFs.get(pluginMap.mapDivId);
            if (drawRect == null) {
              continue;
            }

            int width = (int)drawRect.width();
            int height = (int)drawRect.height();
            int x = (int) drawRect.left;
            int y = (int) drawRect.top + scrollY;
            ViewGroup.LayoutParams lParams = pluginMap.mapView.getLayoutParams();

            if (lParams instanceof FrameLayout.LayoutParams) {
              FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lParams;

              if (params.leftMargin == x && params.topMargin == y &&
                  params.width == width && params.height == height) {
                return;
              }
              params.width = width;
              params.height = height;
              params.leftMargin = x;
              params.topMargin = y;
              //Log.d("MyPluginLayout", "-->FrameLayout x = " + x + ", y = " + y + ", w = " + params.width + ", h = " + params.height);
              pluginMap.mapView.setLayoutParams(params);

            } else if (lParams instanceof AbsoluteLayout.LayoutParams) {
              AbsoluteLayout.LayoutParams params = (AbsoluteLayout.LayoutParams) lParams;
              if (params.x == x && params.y == y &&
                params.width == width && params.height == height) {
                return;
              }
              params.width = width;
              params.height = height;
              params.x = x;
              params.y = y;
              pluginMap.mapView.setLayoutParams(params);
            } else if (lParams instanceof LinearLayout.LayoutParams) {
              LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lParams;

              if (params.leftMargin == x && params.topMargin == y &&
                params.width == width && params.height == height) {
                return;
              }
              params.width = width;
              params.height = height;
              params.leftMargin = x;
              params.topMargin = y;
              pluginMap.mapView.setLayoutParams(params);
            }

          }
          mapIds = null;
          pluginMap = null;
          mapId = null;
          pluginMap = null;
          drawRect = null;
        }
      });

    }
  };

  @SuppressLint("NewApi")
  public MyPluginLayout(CordovaWebView webView, Activity activity) {
    super(webView.getView().getContext());
    this.browserView = webView.getView();
    browserView.getViewTreeObserver().addOnGlobalLayoutListener(this);
    mActivity = activity;
    this.webView = webView;
    this.root = (ViewGroup) browserView.getParent();
    this.context = browserView.getContext();
    //if (Build.VERSION.SDK_INT >= 21 || "org.xwalk.core.XWalkView".equals(browserView.getClass().getName())) {
    //  browserView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    //}

    zoomScale = Resources.getSystem().getDisplayMetrics().density;
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

    browserView.setDrawingCacheEnabled(false);


    this.addView(scrollView);
    this.addView(frontLayer);
    root.addView(this);
    browserView.setBackgroundColor(Color.TRANSPARENT);
    /*
    if("org.xwalk.core.XWalkView".equals(browserView.getClass().getName())
      || "org.crosswalk.engine.XWalkCordovaView".equals(browserView.getClass().getName())) {
      try {
    // view.setZOrderOnTop(true)
    // Called just in time as with root.setBackground(...) the color
    // come in front and take the whole screen
        browserView.getClass().getMethod("setZOrderOnTop", boolean.class)
          .invoke(browserView, true);
      }
      catch(Exception e) {
        e.printStackTrace();
      }
    }
    */
    scrollView.setHorizontalScrollBarEnabled(false);
    scrollView.setVerticalScrollBarEnabled(false);

    redrawTimer = new Timer();
    redrawTimer.scheduleAtFixedRate(new ResizeTask(), 100, 25);
    mActivity.getWindow().getDecorView().requestFocus();
  }



  public void clearHtmlElements()  {
    Bundle bundle;
    RectF rectF;
    synchronized (HTMLNodes) {
      String[] keys = HTMLNodes.keySet().toArray(new String[HTMLNodes.size()]);
      for (int i = 0; i < HTMLNodes.size(); i++) {
        bundle = HTMLNodes.remove(keys[i]);
        bundle = null;
        rectF = HTMLNodeRectFs.remove(keys[i]);
        rectF = null;
      }
      keys = null;
    }
  }

  public void putHTMLElements(JSONObject elements)  {


    HashMap<String, Bundle> newBuffer = new HashMap<String, Bundle>();
    HashMap<String, RectF> newBufferRectFs = new HashMap<String, RectF>();

    Bundle elementsBundle = PluginUtil.Json2Bundle(elements);

    Iterator<String> domIDs = elementsBundle.keySet().iterator();
    String domId;
    Bundle domInfo, size;
    while (domIDs.hasNext()) {
      domId = domIDs.next();
      domInfo = elementsBundle.getBundle(domId);

      size = domInfo.getBundle("size");
      RectF rectF = new RectF();
      rectF.left = (float)(Double.parseDouble(size.get("left") + "") * zoomScale);
      rectF.top = (float)(Double.parseDouble(size.get("top") + "") * zoomScale);
      rectF.right = rectF.left  + (float)(Double.parseDouble(size.get("width") + "") * zoomScale);
      rectF.bottom = rectF.top  + (float)(Double.parseDouble(size.get("height") + "") * zoomScale);
      newBufferRectFs.put(domId, rectF);

      domInfo.remove("size");
      newBuffer.put(domId, domInfo);
    }

    Bundle bundle;
    RectF rectF;
    HashMap<String, Bundle> oldBuffer = HTMLNodes;
    HashMap<String, RectF> oldBufferRectFs = HTMLNodeRectFs;

    synchronized (HTMLNodes) {
      HTMLNodes = newBuffer;
      HTMLNodeRectFs = newBufferRectFs;
    }

    String[] keys = oldBuffer.keySet().toArray(new String[oldBuffer.size()]);
    for (int i = 0; i < oldBuffer.size(); i++) {
      bundle = oldBuffer.remove(keys[i]);
      bundle = null;
      rectF = oldBufferRectFs.remove(keys[i]);
      rectF = null;
    }
    oldBuffer = null;
    oldBufferRectFs = null;
    keys = null;
    elementsBundle = null;
  }

  /*
  public void updateViewPosition(final String mapId) {
    //Log.d("MyPluginLayout", "---> updateViewPosition / mapId = " + mapId);

    if (!pluginMaps.containsKey(mapId)) {
      return;
    }

    final PluginMap pluginMap = pluginMaps.get(mapId);
        if (pluginMap.mapDivId == null) {
          return;
        }
        final ViewGroup.LayoutParams lParams = pluginMap.mapView.getLayoutParams();
        //int scrollX = browserView.getScrollX();
    final int scrollY = browserView.getScrollY();
    final int webviewWidth = browserView.getWidth();
    final int webviewHeight = browserView.getHeight();
    final RectF drawRect = HTMLNodeRectFs.get(pluginMap.mapDivId);

    final int width = (int)drawRect.width();
    final int height = (int)drawRect.height();
    final int x = (int) drawRect.left;
    final int y = (int) drawRect.top + scrollY;

    mActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (lParams instanceof AbsoluteLayout.LayoutParams) {
          AbsoluteLayout.LayoutParams params = (AbsoluteLayout.LayoutParams) lParams;
          if (params.x == x && params.y == y &&
              params.width == width && params.height == height) {
            return;
          }
          params.width = width;
          params.height = height;
          params.x = x;
          params.y = y;
          Log.d("MyPluginLayout", "-->absolute " + params.x + ", " + params.y + " - " + params.width + ", " + params.height);
          pluginMap.mapView.setLayoutParams(params);
        } else if (lParams instanceof LinearLayout.LayoutParams) {
          LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lParams;

          if (params.leftMargin == x && params.topMargin == y &&
            params.width == width && params.height == height) {
            return;
          }
          params.width = width;
          params.height = height;
          params.leftMargin = x;
          params.topMargin = y;
          Log.d("MyPluginLayout", "-->LinearLayout " + params.leftMargin + ", " + params.topMargin + " - " + params.width + ", " + params.height);
          pluginMap.mapView.setLayoutParams(params);

        } else if (lParams instanceof FrameLayout.LayoutParams) {
          FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lParams;

          if (params.leftMargin == x && params.topMargin == y &&
            params.width == width && params.height == height) {
            return;
          }
          params.width = width;
          params.height = height;
          params.leftMargin = x;
          params.topMargin = y;
          params.gravity = Gravity.TOP;
          Log.d("MyPluginLayout", "-->FrameLayout " + params.leftMargin + ", " + params.topMargin + " - " + params.width + ", " + params.height);
          pluginMap.mapView.setLayoutParams(params);
        }
        //Log.d("MyPluginLayout", "---> mapId : " + mapId + " drawRect = " + drawRect.left + ", " + drawRect.top + " - " + drawRect.width() + ", " + drawRect.height());
/ *
        if ((drawRect.top + drawRect.height() < 0) ||
          (drawRect.top >  webviewHeight) ||
          (drawRect.left + drawRect.width() < 0) ||
          (drawRect.left > webviewWidth))  {

          pluginMap.mapView.setVisibility(View.INVISIBLE);
        } else {
          pluginMap.mapView.setVisibility(View.VISIBLE);
        }
        frontLayer.invalidate();
        * /
      }
    });
  }
*/

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

  public PluginMap removePluginMap(final String mapId) {
    if (!pluginMaps.containsKey(mapId)) {
      return null;
    }
    final PluginMap pluginMap = pluginMaps.remove(mapId);

    mActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          scrollFrameLayout.removeView(pluginMap.mapView);
          pluginMap.mapView.removeView(touchableWrappers.remove(mapId));

          //Log.d("MyPluginLayout", "--> removePluginMap / mapId = " + mapId);


          mActivity.getWindow().getDecorView().requestFocus();
        } catch (Exception e) {
          // ignore
          //e.printStackTrace();
        }
      }
    });
    return pluginMap;
  }

  public void addPluginMap(final PluginMap pluginMap) {
    if (pluginMap.mapDivId == null) {
      return;
    }

    if (!HTMLNodes.containsKey(pluginMap.mapDivId)) {
      Bundle dummyInfo = new Bundle();
      dummyInfo.putDouble("offsetX", 0);
      dummyInfo.putDouble("offsetY", 3000);
      dummyInfo.putBoolean("isDummy", true);
      HTMLNodes.put(pluginMap.mapDivId, dummyInfo);
      HTMLNodeRectFs.put(pluginMap.mapDivId, new RectF(0, 3000, 100, 100));
    }
    pluginMaps.put(pluginMap.mapId, pluginMap);

    mActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {

        if (pluginMap.mapView.getParent() == null) {
          TouchableWrapper wrapper = new TouchableWrapper(context);
          touchableWrappers.put(pluginMap.mapId, wrapper);
          pluginMap.mapView.addView(wrapper);
          scrollFrameLayout.addView(pluginMap.mapView);
        }

        mActivity.getWindow().getDecorView().requestFocus();

        //updateViewPosition(pluginMap.mapId);

      }
    });
  }

  public void scrollTo(int x, int y) {
    this.scrollView.scrollTo(x, y);
  }

  public void inValidate() {
    this.frontLayer.invalidate();
  }

  @Override
  public void onScrollChanged() {
//Log.d("Layout", "---> onScrollChanged");
    scrollView.scrollTo(browserView.getScrollX(), browserView.getScrollY());
  }


  private class FrontLayerLayout extends FrameLayout {

    public FrontLayerLayout(Context context) {
      super(context);
      this.setWillNotDraw(false);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
      if (pluginMaps == null || pluginMaps.size() == 0) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
          webView.loadUrl("javascript:if(window.cordova){cordova.fireDocumentEvent('plugin_touch', {});}");
        }
        return false;
      }
      MyPluginLayout.this.stopFlag = true;

      int action = event.getAction();
      //Log.d("FrontLayerLayout", "----> action = " + MotionEvent.actionToString(action) + ", isScrolling = " + isScrolling);

      // The scroll action that started in the browser region is end.
      isScrolling = action != MotionEvent.ACTION_UP && isScrolling;
      if (isScrolling) {
        MyPluginLayout.this.stopFlag = false;
        return false;
      }

      PluginMap pluginMap;
      Iterator<Map.Entry<String, PluginMap>> iterator =  pluginMaps.entrySet().iterator();
      Entry<String, PluginMap> entry;
      String mapId;

      PointF clickPoint = new PointF(event.getX(), event.getY());

      int scrollY = browserView.getScrollY();
      RectF drawRect;
      boolean isMapAction = false;


      synchronized (HTMLNodes) {
        while (iterator.hasNext()) {
          entry = iterator.next();
          mapId = entry.getKey();
          pluginMap = entry.getValue();

          //-----------------------
          // Is the map clickable?
          //-----------------------
          if (!pluginMap.isVisible || !pluginMap.isClickable) {
            continue;
          }

          if (pluginMap.mapDivId == null) {
            continue;
          }

          //------------------------------------------------
          // Is the clicked point is in the map rectangle?
          //------------------------------------------------
          drawRect = HTMLNodeRectFs.get(pluginMap.mapDivId);
          if (drawRect == null || !drawRect.contains(clickPoint.x, clickPoint.y)) {
            continue;
          }
          isMapAction = true;

          //-----------------------------------------------------------
          // Is the clicked point is on the html elements in the map?
          //-----------------------------------------------------------
          String domIDs[] = HTMLNodes.keySet().toArray(new String[HTMLNodes.size()]);
          Bundle domInfo = HTMLNodes.get(pluginMap.mapDivId);
          RectF htmlElementRect;
          double mapDivDepth = domInfo.getDouble("depth");

          for (String domId : domIDs) {
            if (pluginMap.mapDivId.equals(domId)) {
              continue;
            }
            if (!HTMLNodes.containsKey(domId)) {
              continue;
            }
            domInfo = HTMLNodes.get(domId);
            if (domInfo == null) {
              continue;
            }
            if (domInfo.getDouble("depth") <= mapDivDepth) {
              continue;
            }

            htmlElementRect = HTMLNodeRectFs.get(domId);
            if (htmlElementRect.width() == 0 || htmlElementRect.height() == 0) {
              continue;
            }

            if (clickPoint.x >= htmlElementRect.left &&
                clickPoint.x <= (htmlElementRect.right) &&
                clickPoint.y >= htmlElementRect.top &&
                clickPoint.y <= htmlElementRect.bottom) {
              //Log.d(TAG, "---> hit = " + domId);
              isMapAction = false;
              break;
            }

          }
          if (isMapAction) {
            break;
          }
        }
      }
      isScrolling = (!isMapAction && action == MotionEvent.ACTION_DOWN) || isScrolling;
      isMapAction = !isScrolling && isMapAction;

      if (!isMapAction) {
        browserView.requestFocus(View.FOCUS_DOWN);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
          webView.loadUrl("javascript:if(window.cordova){cordova.fireDocumentEvent('plugin_touch', {});}");
        }
      }

      MyPluginLayout.this.stopFlag = false;
      return isMapAction;
    }

    @Override
    protected void onDraw(Canvas canvas) {
      if (isSuspended || HTMLNodes.isEmpty() || !isDebug) {
        return;
      }

      PluginMap pluginMap;
      Iterator<Map.Entry<String, PluginMap>> iterator =  pluginMaps.entrySet().iterator();
      Entry<String, PluginMap> entry;
      RectF mapRect;
      synchronized (HTMLNodeRectFs) {
        while (iterator.hasNext()) {
          entry = iterator.next();
          pluginMap = entry.getValue();
          if (pluginMap.mapDivId == null) {
            continue;
          }
          mapRect = HTMLNodeRectFs.get(pluginMap.mapDivId);

          debugPaint.setColor(Color.argb(100, 0, 255, 0));
          canvas.drawRect(mapRect, debugPaint);
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
