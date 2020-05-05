package plugin.google.maps;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.apache.cordova.CordovaWebView;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("deprecation")
public class MyPluginLayout extends FrameLayout implements ViewTreeObserver.OnScrollChangedListener, ViewTreeObserver.OnGlobalLayoutListener {
  private static final String TAG = "MyPluginLayout";
  private View browserView;
  private Context context;
  private FrontLayerLayout frontLayer;
  private ScrollView scrollView;
  public FrameLayout scrollFrameLayout;
  public  Map<String, IPluginView> pluginOverlays = new ConcurrentHashMap<String, IPluginView>();
  private Map<String, TouchableWrapper> touchableWrappers = new ConcurrentHashMap<String, TouchableWrapper>();
  private boolean isScrolling = false;
  public boolean isDebug = false;
  public final Object _lockHtmlNodes = new Object();
  public Map<String, String> CACHE_FIND_DOM = new ConcurrentHashMap<String, String>();
  public Map<String, Bundle> HTMLNodes = new ConcurrentHashMap<String, Bundle>();
  public Map<String, RectF> HTMLNodeRectFs = new ConcurrentHashMap<String, RectF>();
  private Activity mActivity = null;
  private Paint debugPaint = new Paint();
  public boolean stopFlag = false;
  public boolean needUpdatePosition = false;
  public boolean isSuspended = false;
  private float zoomScale;
  private static final Object timerLock = new Object();

  private static Timer redrawTimer;

  @Override
  public void onGlobalLayout() {
    ViewTreeObserver observer = browserView.getViewTreeObserver();
    observer.removeGlobalOnLayoutListener(this);
    observer.addOnScrollChangedListener(this);
  }

  private Runnable resizeWorker = new Runnable() {
    @Override
    public void run() {
      final int scrollY = browserView.getScrollY();
      final int scrollX = browserView.getScrollX();
      final int webviewWidth = browserView.getWidth();
      final int webviewHeight = browserView.getHeight();

      Set<String> keySet = pluginOverlays.keySet();
      String[] toArrayBuf = new String[pluginOverlays.size()];
      String[] mapIds = keySet.toArray(toArrayBuf);
      String mapId;
      IPluginView pluginOverlay;
      RectF drawRect;
      for (int i = 0; i < mapIds.length; i++) {
        mapId = mapIds[i];
        if (mapId == null) {
          continue;
        }
        pluginOverlay = pluginOverlays.get(mapId);
        if (pluginOverlay == null || pluginOverlay.getDivId() == null) {
          continue;
        }
        drawRect = HTMLNodeRectFs.get(pluginOverlay.getDivId());
        if (drawRect == null || drawRect.left == 0 && drawRect.top == 0 && drawRect.width() == 0 && drawRect.height() == 0) {

          continue;
        }

        int width = (int)drawRect.width();
        int height = (int)drawRect.height();
        int x = (int) drawRect.left;
        int y = (int) drawRect.top + scrollY;



        if (pluginOverlay.getVisible()) {
          if (pluginOverlay.getOverlayId().startsWith("streetview_")) {
            if (y + height >= scrollY &&
                    x + width >= scrollX &&
                    y < scrollY + webviewHeight &&
                    x < scrollX + webviewWidth) {
              MyPluginLayout.this.addPluginOverlay(pluginOverlay);
            } else {

              scrollFrameLayout.removeView(pluginOverlay.getView());
              pluginOverlay.getView().removeView(touchableWrappers.remove(pluginOverlay.getOverlayId()));
            }
          }
        }

        ViewGroup.LayoutParams lParams = pluginOverlay.getView().getLayoutParams();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lParams;
        //Log.d("MyPluginLayout", "-->FrameLayout x = " + x + ", y = " + y + ", w = " + width + ", h = " + height);
        if (params.leftMargin == x && params.topMargin == y &&
                params.width == width && params.height == height) {
          continue;
        }
        params.width = width;
        params.height = height;
        params.leftMargin = x;
        params.topMargin = y;
        pluginOverlay.getView().setLayoutParams(params);


      }
    }
  };

  public void updateMapPositions() {
    mActivity.runOnUiThread(resizeWorker);
  }

  private class ResizeTask extends TimerTask {
    @Override
    public void run() {
      mActivity.runOnUiThread(resizeWorker);
    }
  }

  @SuppressLint("NewApi")
  public MyPluginLayout(CordovaWebView webView, Activity activity) {
    super(webView.getView().getContext());
    this.browserView = webView.getView();
    browserView.getViewTreeObserver().addOnGlobalLayoutListener(this);
    mActivity = activity;
    ViewGroup root = (ViewGroup) browserView.getParent();
    this.context = browserView.getContext();
    //if (Build.VERSION.SDK_INT >= 21 || "org.xwalk.core.XWalkView".equals(browserView.getClass().getName())) {
    //  browserView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    //}

    zoomScale = Resources.getSystem().getDisplayMetrics().density;
    Log.e(TAG, "--> zoomScale = " + zoomScale);
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
    startTimer();
  }

  public synchronized void stopTimer() {
    synchronized (timerLock) {
      try {
        if (redrawTimer != null) {
          redrawTimer.cancel();
          redrawTimer.purge();
          ResizeTask task = new ResizeTask();
          task.run();
          isSuspended = true;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      redrawTimer = null;
    }
  }

  public synchronized void startTimer() {
    synchronized (timerLock) {
      if (redrawTimer != null) {
        return;
      }

      redrawTimer = new Timer();
      redrawTimer.scheduleAtFixedRate(new ResizeTask(), 0, 25);
      //mActivity.getWindow().getDecorView().requestFocus();  <--- This changes thread number sometimes. Do not use this!
      isSuspended = false;
    }
  }



  public void clearHtmlElements()  {
    synchronized (_lockHtmlNodes) {
      String[] keys = HTMLNodes.keySet().toArray(new String[HTMLNodes.size()]);
      for (int i = 0; i < HTMLNodes.size(); i++) {
        HTMLNodes.remove(keys[i]);
        HTMLNodeRectFs.remove(keys[i]);
      }
    }
  }

  public void putHTMLElements(JSONObject elements)  {


    Map<String, Bundle> newBuffer = new ConcurrentHashMap<String, Bundle>();
    Map<String, RectF> newBufferRectFs = new ConcurrentHashMap<String, RectF>();

    Bundle elementsBundle = PluginUtil.Json2Bundle(elements);

    Iterator<String> domIDs = elementsBundle.keySet().iterator();
    String domId;
    Bundle domInfo, size;
    while (domIDs.hasNext()) {
      domId = domIDs.next();
      domInfo = elementsBundle.getBundle(domId);
      if (domInfo == null) {
        continue;
      }

      size = domInfo.getBundle("size");
      if (size == null) {
        continue;
      }

      RectF rectF = new RectF();
      rectF.left = (float)(Double.parseDouble(size.get("left") + "") * zoomScale);
      rectF.top = (float)(Double.parseDouble(size.get("top") + "") * zoomScale);
      rectF.right = (float)(Double.parseDouble(size.get("right") + "") * zoomScale);
      rectF.bottom =  (float)(Double.parseDouble(size.get("bottom") + "") * zoomScale);


      newBufferRectFs.put(domId, rectF);

      domInfo.remove("size");
      newBuffer.put(domId, domInfo);
    }

    Map<String, Bundle> oldBuffer = HTMLNodes;
    Map<String, RectF> oldBufferRectFs = HTMLNodeRectFs;

    synchronized (_lockHtmlNodes) {
      HTMLNodes = newBuffer;
      HTMLNodeRectFs = newBufferRectFs;
    }

    String[] keys = oldBuffer.keySet().toArray(new String[oldBuffer.size()]);
    for (int i = 0; i < oldBuffer.size(); i++) {
      oldBuffer.remove(keys[i]);
      oldBufferRectFs.remove(keys[i]);
    }
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

  public IPluginView removePluginOverlay(final String overlayId) {
    if (pluginOverlays == null || !pluginOverlays.containsKey(overlayId)) {
      return null;
    }
    final IPluginView pluginOverlay = pluginOverlays.remove(overlayId);

    mActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          scrollFrameLayout.removeView(pluginOverlay.getView());
          pluginOverlay.getView().removeView(touchableWrappers.remove(overlayId));

          //Log.d("MyPluginLayout", "--> removePluginMap / overlayId = " + overlayId);


          //mActivity.getWindow().getDecorView().requestFocus();
        } catch (Exception e) {
          // ignore
          //e.printStackTrace();
        }
      }
    });
    return pluginOverlay;
  }

  public void addPluginOverlay(final IPluginView pluginOverlay) {
    if (pluginOverlay.getDivId() == null) {
      return;
    }

    if (!HTMLNodes.containsKey(pluginOverlay.getDivId())) {
      Log.e(TAG, "----> 200x200");
      Bundle dummyInfo = new Bundle();
      dummyInfo.putDouble("offsetX", 0);
      dummyInfo.putDouble("offsetY", 3000);
      dummyInfo.putBoolean("isDummy", true);
      HTMLNodes.put(pluginOverlay.getDivId(), dummyInfo);
      HTMLNodeRectFs.put(pluginOverlay.getDivId(), new RectF(0, 3000, 200, 200));
    }
    pluginOverlays.put(pluginOverlay.getOverlayId(), pluginOverlay);

    mActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {

        if (pluginOverlay.getView().getParent() == null) {
          TouchableWrapper wrapper = new TouchableWrapper(context);
          touchableWrappers.put(pluginOverlay.getOverlayId(), wrapper);
          pluginOverlay.getView().addView(wrapper);
          int depth = pluginOverlay.getViewDepth();
          int childCnt = scrollFrameLayout.getChildCount();
          View view;
          int index = childCnt;
          for (int i = childCnt - 1; i >= 0; i--) {
            view = scrollFrameLayout.getChildAt(i);
            if (view.getTag() == null) {
              continue;
            }
            if (Integer.parseInt(view.getTag() + "") < depth) {
              index = i;
              break;
            }
          }

          scrollFrameLayout.addView(pluginOverlay.getView(), (childCnt -  index));
        }

        //mActivity.getWindow().getDecorView().requestFocus();

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

  private class Overflow {
    boolean cropX;
    boolean cropY;
    RectF rect;
  }

  private class FrontLayerLayout extends FrameLayout {

    public FrontLayerLayout(Context context) {
      super(context);
      this.setWillNotDraw(false);
    }

    private String findClickedDom(String domId, PointF clickPoint, boolean isMapChild, Overflow overflow) {
      if (CACHE_FIND_DOM.containsKey(domId)) {
        String cacheResult = CACHE_FIND_DOM.get(domId);
        if ("(null)".equals(cacheResult)) {
          cacheResult = null;
        }
        return cacheResult;
      }

      Overflow overflow1 = null;
      if (overflow != null) {
        overflow1 = new Overflow();
        overflow1.cropX = overflow.cropX;
        overflow1.cropY = overflow.cropY;
        overflow1.rect = new RectF(overflow.rect.left, overflow.rect.top, overflow.rect.right, overflow.rect.bottom);
      }
      //Log.d(TAG, "-(findClickedDom)domId = " + domId + ", clickPoint = " + clickPoint.x + ", " + clickPoint.y);

      String maxDomId = null;
      RectF rect, rect2, rect3;
      Bundle domInfo = HTMLNodes.get(domId);
      Bundle zIndexProp;
      int containMapCnt = 0;
      if (domInfo.containsKey("containMapIDs")) {
        Set<String> keys = domInfo.getBundle("containMapIDs").keySet();
        if (keys != null) {
          containMapCnt = keys.size();
        }
      }
      isMapChild = isMapChild || domInfo.getBoolean("isMap", false);

      String pointerEvents = domInfo.getString("pointerEvents");
      String overflowX = domInfo.getString("overflowX");
      String overflowY = domInfo.getString("overflowY");
      if ("hidden".equals(overflowX) || "scroll".equals(overflowX) ||
              "hidden".equals(overflowY) || "scroll".equals(overflowY)) {
        overflow1 = new Overflow();
        overflow1.cropX = "hidden".equals(overflowX) || "scroll".equals(overflowX);
        overflow1.cropY = "hidden".equals(overflowY) || "scroll".equals(overflowY);
        rect3 = HTMLNodeRectFs.get(domId);
        overflow1.rect = new RectF(rect3.left, rect3.top, rect3.right, rect3.bottom);
      } else if ("visible".equals(overflowX) || "visible".equals(overflowY)) {
        if (overflow1 != null) {
          overflow1.cropX = !"visible".equals(overflowX);
          overflow1.cropY = !"visible".equals(overflowY);
        }
      }

      //if (overflow1 != null) {
      //  Log.d(TAG, "--domId = " + domId + ", domInfo = " + domInfo + "\n" +
      //                   "  overflow1 cropX= " + overflow1.cropX + ", cropY = " + overflow1.cropY + ",\n" +
      //                   "  rect = " + overflow1.rect.toShortString());
      //} else {
      //  Log.d(TAG, "--domId = " + domId + ", domInfo = " + domInfo);
      //}

      zIndexProp = domInfo.getBundle("zIndex");
      ArrayList<String> children = domInfo.getStringArrayList("children");
      if ((containMapCnt > 0 || isMapChild || "none".equals(pointerEvents) || zIndexProp.getBoolean("isInherit")) && children != null && children.size() > 0) {
        int maxZindex = (int) Double.NEGATIVE_INFINITY;
        int zIndexValue;
        String childId, grandChildId;
        ArrayList<String> grandChildren;
        for (int i = children.size() - 1; i >= 0; i--) {
          childId = children.get(i);
          domInfo = HTMLNodes.get(childId);
          if (domInfo == null) {
            continue;
          }

          zIndexProp = domInfo.getBundle("zIndex");
          zIndexValue = zIndexProp.getInt("z");
          if (maxZindex < zIndexValue || zIndexProp.getBoolean("isInherit")) {
            grandChildren = domInfo.getStringArrayList("children");
            if (grandChildren == null || grandChildren.size() == 0) {
              rect = HTMLNodeRectFs.get(childId);
              rect2 = new RectF(rect.left, rect.top, rect.right, rect.bottom);
              if (overflow1 != null && !"root".equals(domId) ) {
                overflow1.rect = new RectF(HTMLNodeRectFs.get(domId));
                if (overflow1.cropX) {
                  rect2.left = Math.max(rect2.left, overflow1.rect.left);
                  rect2.right = Math.min(rect2.right, overflow1.rect.right);
                }
                if (overflow1.cropY) {
                  rect2.top = Math.max(rect2.top, overflow1.rect.top);
                  rect2.bottom = Math.min(rect2.bottom, overflow1.rect.bottom);
                }
              }
              if (!rect2.contains(clickPoint.x, clickPoint.y)) {
                continue;
              }

              //Log.d(TAG, "---childId = " + childId + ", domInfo = " + domInfo);
              if ("none".equals(domInfo.getString("pointerEvents"))) {
                continue;
              }
              //Log.d(TAG, "----childId = " + childId + ", maxZindex = " + maxZindex + ", zIndexValue = " + zIndexValue);
              if (maxZindex < zIndexValue) {
                maxZindex = zIndexValue;
                maxDomId = childId;
                //Log.i(TAG, "-----current = " + childId);
              }
            } else {
              if (zIndexValue < maxZindex) {
                //Log.i(TAG, "-----skip because " + childId + " is " + zIndexValue + " < " + maxZindex);
                continue;
              }
              grandChildId = findClickedDom(childId, clickPoint, isMapChild, overflow1);
              if (grandChildId == null) {
                grandChildId = childId;
              } else {
                zIndexValue = HTMLNodes.get(grandChildId).getBundle("zIndex").getInt("z");
              }
              rect = HTMLNodeRectFs.get(grandChildId);
              //rect2 = new RectF(rect);
              //Log.d(TAG, "---findClickedDom("+ childId + ") -> " + grandChildId + ",rect = " + rect2.toShortString());
              /*
              if (overflow1 != null && !"root".equals(domId) ) {
                overflow1.rect = new RectF(HTMLNodeRectFs.get(domId));
                if (overflow1.cropX) {
                  rect2.left = Math.max(rect2.left, overflow1.rect.left);
                  rect2.right = Math.min(rect2.right, overflow1.rect.right);
                }
                if (overflow1.cropY) {
                  rect2.top = Math.max(rect2.top, overflow1.rect.top);
                  rect2.bottom = Math.min(rect2.bottom, overflow1.rect.bottom);
                }
                //Log.d(TAG, "---overflow(" + domId + ") -> " + overflow1.rect.toShortString() + ", \n"+
                //          "   str=" + HTMLNodeRectFs.get(domId) + ",\n" +
                //        "   overflowX = " + overflowX + ", overflowY = " + overflowY +
                //        "   cropX = " + overflow1.cropX + ", cropY = " + overflow1.cropY);
              }
              */
              if (!rect.contains(clickPoint.x, clickPoint.y)) {
                //Log.e(TAG, "----the click point is not in this element(" +grandChildId + "), rect = " + rect2.toShortString() + ", click = " + clickPoint.x + "," + clickPoint.y );
                continue;
              }

              domInfo = HTMLNodes.get(grandChildId);
              //Log.d(TAG, "----grandChildId = " + grandChildId + ", domInfo = " + domInfo);
              if ("none".equals(domInfo.getString("pointerEvents"))) {
                //Log.e(TAG, "-----grandChildId(" + grandChildId + ") is " + domInfo.getString("pointerEvents"));
                continue;
              }
              //Log.d(TAG, "----grandChildId = " + grandChildId + ", maxZindex = " + maxZindex + ", zIndexValue = " + zIndexValue);
              if (maxZindex < zIndexValue) {
                maxZindex = zIndexValue;
                maxDomId = grandChildId;
                //Log.i(TAG, "-----current = " + grandChildId);
              }
            }
          }
        }
      }
      if (maxDomId == null && !"root".equals(domId)) {
        if ("none".equals(pointerEvents)) {
          CACHE_FIND_DOM.put(domId, "(null)");
          return null;
        }
        rect = HTMLNodeRectFs.get(domId);
        rect2 = new RectF(rect.left, rect.top, rect.right, rect.bottom);
//        if (overflow1 != null ) {
//          overflow1.rect = new RectF(HTMLNodeRectFs.get(domId));
//          if (overflow1.cropX) {
//            rect2.left = Math.max(rect2.left, overflow1.rect.left);
//            rect2.right = Math.min(rect2.right, overflow1.rect.right);
//          }
//          if (overflow1.cropY) {
//            rect2.top = Math.max(rect2.top, overflow1.rect.top);
//            rect2.bottom = Math.min(rect2.bottom, overflow1.rect.bottom);
//          }
//        }
        if (!rect2.contains(clickPoint.x, clickPoint.y)) {
          //Log.e(TAG, "--the click point is not in this element(" +domId + "), rect = " + rect2.toShortString() + ", click = " + clickPoint.x + "," + clickPoint.y );
          CACHE_FIND_DOM.put(domId, "(null)");
          return null;
        }
        maxDomId = domId;
      }
      if (maxDomId == null) {
        CACHE_FIND_DOM.put(domId, "(null)");
      } else {
        CACHE_FIND_DOM.put(domId, maxDomId);
      }
      //Log.i(TAG, "-maxDomId = " + maxDomId + ", domId = " + domId);

      return maxDomId;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
      if (pluginOverlays == null || pluginOverlays.size() == 0) {
        return false;
      }
      MyPluginLayout.this.stopFlag = true;

      int action = event.getAction();
      //Log.d("FrontLayerLayout", "----> action = " + action + ", isScrolling = " + isScrolling);

      // The scroll action that started in the browser region is end.
      isScrolling = action != MotionEvent.ACTION_UP && isScrolling;
      if (isScrolling) {
        MyPluginLayout.this.stopFlag = false;
        return false;
      }


      IPluginView pluginOverlay;
      Iterator<Map.Entry<String, IPluginView>> iterator =  pluginOverlays.entrySet().iterator();
      Entry<String, IPluginView> entry;

      PointF clickPoint = new PointF(event.getX(), event.getY());

      RectF drawRect;

      synchronized (_lockHtmlNodes) {
        CACHE_FIND_DOM.clear();

        String clickedDomId = findClickedDom("root", clickPoint, false, null);
        //Log.d(TAG, "----clickedDomId = " + clickedDomId);
        while (iterator.hasNext()) {
          entry = iterator.next();
          pluginOverlay = entry.getValue();

          //-----------------------
          // Is the map clickable?
          //-----------------------
          if (!pluginOverlay.getVisible() || !pluginOverlay.getClickable()) {
            continue;
          }

          if (pluginOverlay.getDivId() == null) {
            continue;
          }

          //------------------------------------------------
          // Is the clicked point is in the map rectangle?
          //------------------------------------------------
          drawRect = HTMLNodeRectFs.get(pluginOverlay.getDivId());
          if (drawRect == null || !drawRect.contains(clickPoint.x, clickPoint.y)) {
            continue;
          }

          if (pluginOverlay.getDivId().equals(clickedDomId)) {
            return true;
          }

        }
      }
      isScrolling = (action == MotionEvent.ACTION_DOWN) || isScrolling;

      //browserView.requestFocus(View.FOCUS_DOWN);
      return false;
    }


    @Override
    protected void onDraw(Canvas canvas) {
      if (isSuspended || HTMLNodes.isEmpty() || !isDebug) {
        return;
      }

      IPluginView pluginOverlay;
      Iterator<Map.Entry<String, IPluginView>> iterator =  pluginOverlays.entrySet().iterator();
      Entry<String, IPluginView> entry;
      RectF mapRect;
      synchronized (_lockHtmlNodes) {
        while (iterator.hasNext()) {
          entry = iterator.next();
          pluginOverlay = entry.getValue();
          if (pluginOverlay.getDivId() == null) {
            continue;
          }
          mapRect = HTMLNodeRectFs.get(pluginOverlay.getDivId());

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
