package plugin.google.maps;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import com.google.android.libraries.maps.MapsInitializer;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Semaphore;

public class CordovaGoogleMaps extends MyPlugin implements ViewTreeObserver.OnScrollChangedListener{
  public ViewGroup root;
  public static PgmPluginLayer mPluginLayout = null;
  public boolean initialized = false;
  private static final Object timerLock = new Object();
  public static final HashMap<String, IPluginView> viewPlugins = new HashMap<String, IPluginView>();

  @Override
  public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
    if (root != null) {
      return;
    }
    LOG.setLogLevel(LOG.DEBUG);

    final View view = webView.getView();
    view.getViewTreeObserver().addOnScrollChangedListener(CordovaGoogleMaps.this);
    root = (ViewGroup) view.getParent();


    activity.runOnUiThread(new Runnable() {
      @SuppressLint("NewApi")
      public void run() {

        webView.getView().setBackgroundColor(Color.TRANSPARENT);
        webView.getView().setOverScrollMode(View.OVER_SCROLL_NEVER);
        mPluginLayout = new PgmPluginLayer(webView, activity);
        mPluginLayout.stopTimer();

        //------------------------------
        // Initialize Google Maps SDK
        //------------------------------
        if (!initialized) {
          try {
            MapsInitializer.initialize(activity);
            initialized = true;
          } catch (Exception e) {

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
            alertDialogBuilder
                    .setMessage(e.getLocalizedMessage())
                    .setCancelable(false)
                    .setPositiveButton(PluginUtil.getPgmStrings(activity,"pgm_google_close_button"), new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog,int id) {
                        dialog.dismiss();
                      }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();

            // show it
            alertDialog.show();
          }
        }

      }
    });
  }

  @Override
  public boolean onOverrideUrlLoading(String url) {
    mPluginLayout.stopTimer();
    return false;
  }


  @Override
  public void onScrollChanged() {
    if (mPluginLayout == null) {
      return;
    }
    View view = webView.getView();
    int scrollX = view.getScrollX();
    int scrollY = view.getScrollY();
    mPluginLayout.scrollTo(scrollX, scrollY);

  }


  @PgmPluginMethod(runOnUiThread = true)
  public void updateMapPositionOnly(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final JSONObject elements = args.getJSONObject(0);

    Bundle elementsBundle = PluginUtil.Json2Bundle(elements);
    float zoomScale = Resources.getSystem().getDisplayMetrics().density;

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

      mPluginLayout.HTMLNodeRectFs.put(domId, rectF);
    }

    if (mPluginLayout.isSuspended) {
      mPluginLayout.updateMapPositions();
    }
    callbackContext.success();
  }

  @PgmPluginMethod(runOnUiThread = true)
  public void backHistory(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (!webView.backHistory()) {
      // If no more history back, exit the app
      activity.finish();
    }
    callbackContext.success();
  }


  @PgmPluginMethod
  public synchronized void pause(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    synchronized (timerLock) {
      if (mPluginLayout == null) {
        callbackContext.success();
        return;
      }
      mPluginLayout.stopTimer();
      callbackContext.success();
    }
  }

  @PgmPluginMethod
  public synchronized void resume(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    synchronized (timerLock) {
      if (mPluginLayout == null) {
        callbackContext.success();
        return;
      }
      if (mPluginLayout.isSuspended) {
        mPluginLayout.startTimer();
      }
      callbackContext.success();

      //On resume reapply background because it might have been changed by some other plugin
      webView.getView().setBackgroundColor(Color.TRANSPARENT);
    }
  }

  @PgmPluginMethod
  public void clearHtmlElements(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (mPluginLayout == null) {
      callbackContext.success();
      return;
    }
    mPluginLayout.clearHtmlElements();
    callbackContext.success();
  }

  @PgmPluginMethod
  public void putHtmlElements(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    final JSONObject elements = args.getJSONObject(0);
    if (mPluginLayout == null) {
      callbackContext.success();
      return;
    }

    //Log.d(TAG, "--->stopFlag = " + mPluginLayout.stopFlag + ", mPluginLayout.needUpdatePosition = " + mPluginLayout.needUpdatePosition);
    if (!mPluginLayout.stopFlag || mPluginLayout.needUpdatePosition) {
      mPluginLayout.putHTMLElements(elements);
    }

    //mPluginLayout.updateMapPositions();
    //mPluginLayout.startTimer();
    callbackContext.success();
  }

  @Override
  public void onReset() {
    super.onReset();
    if (mPluginLayout == null) {
      return;
    }

    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {

        mPluginLayout.setBackgroundColor(Color.WHITE);

        Set<String> mapIds = viewPlugins.keySet();
        IPluginView pluginOverlay;

        // prevent the ConcurrentModificationException error.
        String[] mapIdArray= mapIds.toArray(new String[mapIds.size()]);
        Semaphore semaphore = new Semaphore(1);
        for (String mapId : mapIdArray) {
          if (viewPlugins.containsKey(mapId)) {
            pluginOverlay = mPluginLayout.removePluginOverlay(mapId);

            try {
              semaphore.acquire();
              JSONArray args = new JSONArray();
              args.put(0, mapId);
              pluginOverlay.remove(args, new CallbackContext("dummy", webView) {
                @Override
                public void sendPluginResult(PluginResult pluginResult) {
                  semaphore.release();
                }
              });
            } catch (Exception e) {
              e.printStackTrace();
            }
            pluginOverlay.onDestroy();
            mPluginLayout.HTMLNodes.remove(mapId);
          }
        }
        mPluginLayout.HTMLNodes.clear();
        viewPlugins.clear();

        System.gc();
        Runtime.getRuntime().gc();
      }
    });

  }

  @PgmPluginMethod
  public void removeMap(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    if (!viewPlugins.containsKey(mapId)) {
      callbackContext.success();
      return;
    }

    IPluginView viewPlugin = viewPlugins.remove(mapId);

    viewPlugin.remove(null, null);
    viewPlugin.onDestroy();
    mPluginLayout.HTMLNodes.remove(mapId);


    if (mapId.startsWith("streetview_")) {
      PluginStreetViewPanorama pluginSV = (PluginStreetViewPanorama)viewPlugin;
      pluginSV.isRemoved = true;
    } else {
      PluginMap pluginMap = (PluginMap)viewPlugin;
      pluginMap.isRemoved = true;
    }
    mPluginLayout.removePluginOverlay(mapId);
    mPluginLayout.HTMLNodes.remove(mapId);

    System.gc();
    Runtime.getRuntime().gc();
    callbackContext.success();

  }

  @PgmPluginMethod(runOnUiThread = true)
  public void getMap(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    //------------------------------------------
    // Create an instance of PluginMap class.
    //------------------------------------------
    JSONObject meta = args.getJSONObject(0);
    String mapId = meta.getString("__pgmId");
    PluginMap pluginMap = new PluginMap();
    pluginMap.privateInitialize(mapId, cordova, webView, null);
    pluginMap.initialize(cordova, webView);

    viewPlugins.put(mapId, pluginMap);

    pluginMap.getMap(args, callbackContext);
  }

  @PgmPluginMethod(runOnUiThread = true)
  public void getPanorama(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    //------------------------------------------
    // Create an instance of PluginStreetView class.
    //------------------------------------------
    JSONObject meta = args.getJSONObject(0);
    String mapId = meta.getString("__pgmId");
    PluginStreetViewPanorama pluginStreetView = new PluginStreetViewPanorama();
    pluginStreetView.privateInitialize(mapId, cordova, webView, null);
    pluginStreetView.initialize(cordova, webView);

    pluginStreetView.getPanorama(args, callbackContext);
  }

  @Override
  public void onStart() {
    super.onStart();

    for (IPluginView viewPlugin : viewPlugins.values()) {
      viewPlugin.onStart();
    }
  }
  @Override
  public void onStop() {
    super.onStop();

    for (IPluginView viewPlugin : viewPlugins.values()) {
      viewPlugin.onStop();
    }

  }
  @Override
  public void onPause(boolean multitasking) {
    super.onPause(multitasking);
    if (mPluginLayout != null) {
      mPluginLayout.stopTimer();
    }

    for (IPluginView viewPlugin : viewPlugins.values()) {
      viewPlugin.onPause(multitasking);
    }

  }

  @Override
  public void onResume(boolean multitasking) {
    super.onResume(multitasking);
    for (IPluginView viewPlugin : viewPlugins.values()) {
      viewPlugin.onResume(multitasking);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    for (IPluginView viewPlugin : viewPlugins.values()) {
      viewPlugin.onDestroy();
    }

  }

  /**
   * Called by the system when the device configuration changes while your activity is running.
   *
   * @param newConfig		The new device configuration
   */
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

    /*
    // TODO: Do we still need this code?
    Handler handler = new Handler();
    handler.postDelayed(new Runnable() {
      @Override
      public void run() {

        PluginMap pluginMap;
        Collection<PluginEntry> collection =  pluginManager.getPluginEntries();
        for (PluginEntry entry: collection) {
          if ("plugin.google.maps.PluginMap".equals(entry.pluginClass) && entry.plugin != null) {
            pluginMap = (PluginMap)entry.plugin;
            if (pluginMap.map != null) {

              // Trigger the CAMERA_MOVE_END mandatory
              pluginMap.onCameraIdle();
            }
          }
        }
      }
    }, 500);
    */
  }

  @PgmPluginMethod(runOnUiThread = true)
  public void setBackGroundColor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    JSONArray rgba = args.getJSONArray(0);
    int backgroundColor = Color.WHITE;

    if (rgba != null && rgba.length() == 4) {
      backgroundColor = PluginUtil.parsePluginColor(rgba);
    }

    mPluginLayout.setBackgroundColor(backgroundColor);
    callbackContext.success();
  }


  @PgmPluginMethod
  public Boolean setEnv(JSONArray args, final CallbackContext callbackContext) {
    // stub
    callbackContext.success();
    return true;
  }

  @PgmPluginMethod
  public Boolean getLicenseInfo(JSONArray args, final CallbackContext callbackContext) {
    callbackContext.success("");
    return true;
  }

  @PgmPluginMethod(runOnUiThread = true)
  public void attachToWebView(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    PluginMap instance = (PluginMap)viewPlugins.get(mapId);
    mPluginLayout.addPluginOverlay(instance);
    callbackContext.success();
  }

  @PgmPluginMethod(runOnUiThread = true)
  public void detachFromWebView(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    mPluginLayout.removePluginOverlay(mapId);
    callbackContext.success();
  }


  @PgmPluginMethod(runOnUiThread = true)
  public void setDiv(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    PluginMap instance = (PluginMap)viewPlugins.get(mapId);

    if (args.length() == 1) {
      instance.setDivId(null);
      mPluginLayout.removePluginOverlay(mapId);
      callbackContext.success();
      return;
    } else {
      String mapDivId = args.getString(1);
      instance.setDivId(mapDivId);
      mPluginLayout.addPluginOverlay(instance);
      this.resizeMap(args, callbackContext);
    }
  }

  @PgmPluginMethod(runOnUiThread = true)
  public void resizeMap(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    PluginMap instance = (PluginMap)viewPlugins.get(mapId);
    String mapDivId = instance.getDivId();

    if (mPluginLayout == null || mapDivId == null) {
      //Log.d("PluginMap", "---> resizeMap / mPluginLayout = null");
      callbackContext.success();
      return;
    }

    mPluginLayout.needUpdatePosition = true;

    if (!mPluginLayout.HTMLNodes.containsKey(mapDivId)) {
      Bundle dummyInfo = new Bundle();
      dummyInfo.putBoolean("isDummy", true);
      dummyInfo.putDouble("offsetX", 0);
      dummyInfo.putDouble("offsetY", 3000);

      Bundle dummySize = new Bundle();
      dummySize.putDouble("left", 0);
      dummySize.putDouble("top", 3000);
      dummySize.putDouble("width", 200);
      dummySize.putDouble("height", 200);
      dummyInfo.putBundle("size", dummySize);
      dummySize.putDouble("depth", -999);
      mPluginLayout.HTMLNodes.put(mapDivId, dummyInfo);
    }


    RectF drawRect = mPluginLayout.HTMLNodeRectFs.get(mapDivId);
    if (drawRect != null) {
      final int scrollY = webView.getView().getScrollY();

      int width = (int) drawRect.width();
      int height = (int) drawRect.height();
      int x = (int) drawRect.left;
      int y = (int) drawRect.top + scrollY;
      ViewGroup.LayoutParams lParams = instance.getView().getLayoutParams();
      FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lParams;

      params.width = width;
      params.height = height;
      params.leftMargin = x;
      params.topMargin = y;
      instance.getView().setLayoutParams(params);

      callbackContext.success();
    }
  }

}
