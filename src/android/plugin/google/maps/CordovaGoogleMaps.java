package plugin.google.maps;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.MapsInitializer;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginEntry;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

@SuppressWarnings("deprecation")
public class CordovaGoogleMaps extends CordovaPlugin implements ViewTreeObserver.OnScrollChangedListener{
  private final String TAG = "GoogleMapsPlugin";
  private Activity activity;
  public ViewGroup root;
  public MyPluginLayout mPluginLayout = null;
  public boolean initialized = false;
  public PluginManager pluginManager;
  private static final Object timerLock = new Object();

  @SuppressLint("NewApi") @Override
  public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
    if (root != null) {
      return;
    }
    LOG.setLogLevel(LOG.DEBUG);

    activity = cordova.getActivity();
    final View view = webView.getView();
    view.getViewTreeObserver().addOnScrollChangedListener(CordovaGoogleMaps.this);
    root = (ViewGroup) view.getParent();

    pluginManager = webView.getPluginManager();

    cordova.getActivity().runOnUiThread(new Runnable() {
      @SuppressLint("NewApi")
      public void run() {

        // Enable this, webView makes draw cache on the Android action bar issue.
        //View view = webView.getView();
        //if (Build.VERSION.SDK_INT >= 21 || "org.xwalk.core.XWalkView".equals(view.getClass().getName())){
        //  view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        //  Log.d("Layout", "--> view =" + view.isHardwareAccelerated()); //always false
        //}


        // ------------------------------
        // Check of Google Play Services
        // ------------------------------
        int checkGooglePlayServices = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);

        Log.d(TAG, "----> checkGooglePlayServices = " + (ConnectionResult.SUCCESS == checkGooglePlayServices));

        if (checkGooglePlayServices != ConnectionResult.SUCCESS) {
          // google play services is missing!!!!
          /*
           * Returns status code indicating whether there was an error. Can be one
           * of following in ConnectionResult: SUCCESS, SERVICE_MISSING,
           * SERVICE_VERSION_UPDATE_REQUIRED, SERVICE_DISABLED, SERVICE_INVALID.
           */
          Log.e(TAG, "---Google Play Services is not available: " + GooglePlayServicesUtil.getErrorString(checkGooglePlayServices));

          boolean isNeedToUpdate = false;

          String errorMsg = PluginUtil.getPgmStrings(activity, "pgm_google_play_error");
          switch (checkGooglePlayServices) {
            case ConnectionResult.DEVELOPER_ERROR:
              errorMsg = PluginUtil.getPgmStrings(activity,"pgm_google_play_developer_error");
              break;
            case ConnectionResult.INTERNAL_ERROR:
              errorMsg = PluginUtil.getPgmStrings(activity,"pgm_google_play_internal_error");
              break;
            case ConnectionResult.INVALID_ACCOUNT:
              errorMsg = PluginUtil.getPgmStrings(activity,"pgm_google_play_invalid_account");
              break;
            case ConnectionResult.LICENSE_CHECK_FAILED:
              errorMsg = PluginUtil.getPgmStrings(activity,"pgm_google_play_license_check_failed");
              break;
            case ConnectionResult.NETWORK_ERROR:
              errorMsg = PluginUtil.getPgmStrings(activity,"pgm_google_play_network_error");
              break;
            case ConnectionResult.SERVICE_DISABLED:
              errorMsg = PluginUtil.getPgmStrings(activity,"pgm_google_play_service_disabled");
              break;
            case ConnectionResult.SERVICE_INVALID:
              errorMsg = PluginUtil.getPgmStrings(activity,"pgm_google_play_service_invalid");
              isNeedToUpdate = true;
              break;
            case ConnectionResult.SERVICE_MISSING:
              errorMsg = PluginUtil.getPgmStrings(activity,"pgm_google_play_service_missing");
              isNeedToUpdate = true;
              break;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
              errorMsg = PluginUtil.getPgmStrings(activity,"pgm_google_play_service_update_required");
              isNeedToUpdate = true;
              break;
            case ConnectionResult.SIGN_IN_REQUIRED:
              errorMsg = PluginUtil.getPgmStrings(activity,"pgm_google_play_sign_in_required");
              break;
            default:
              isNeedToUpdate = true;
              break;
          }

          final boolean finalIsNeedToUpdate = isNeedToUpdate;
          AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
          alertDialogBuilder
              .setMessage(errorMsg)
              .setCancelable(false)
              .setPositiveButton(PluginUtil.getPgmStrings(activity,"pgm_google_close_button"), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                  dialog.dismiss();
                  if (finalIsNeedToUpdate) {
                    try {
                      activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.gms")));
                    } catch (android.content.ActivityNotFoundException anfe) {
                      activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.google.android.gms")));
                    }
                  }
                }
              });
          AlertDialog alertDialog = alertDialogBuilder.create();

          // show it
          alertDialog.show();

          Log.e(TAG, "Google Play Services is not available.");
          return;
        }

        webView.getView().setBackgroundColor(Color.TRANSPARENT);
        webView.getView().setOverScrollMode(View.OVER_SCROLL_NEVER);
        mPluginLayout = new MyPluginLayout(webView, activity);
        mPluginLayout.stopTimer();


        // Check the API key
        ApplicationInfo appliInfo = null;
        try {
          appliInfo = activity.getPackageManager().getApplicationInfo(activity.getPackageName(), PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {}

        String API_KEY = appliInfo.metaData.getString("com.google.android.maps.v2.API_KEY");
        if ("API_KEY_FOR_ANDROID".equals(API_KEY)) {

          AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);

          alertDialogBuilder
              .setMessage(PluginUtil.getPgmStrings(activity,"pgm_api_key_error"))
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


        //------------------------------
        // Initialize Google Maps SDK
        //------------------------------
        if (!initialized) {
          try {
            MapsInitializer.initialize(cordova.getActivity());
            initialized = true;
          } catch (Exception e) {
            e.printStackTrace();
          }
        }

      }
    });


  }

  @Override
  public boolean onOverrideUrlLoading(String url) {
    mPluginLayout.stopTimer();
    /*
    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        webView.loadUrl("javascript:if(window.cordova){cordova.fireDocumentEvent('plugin_url_changed', {});}");
      }
    });
    */
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

  @Override
  public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    cordova.getThreadPool().submit(new Runnable() {
      @Override
      public void run() {
        try {
          if (action.equals("putHtmlElements")) {
            CordovaGoogleMaps.this.putHtmlElements(args, callbackContext);
          } else if ("clearHtmlElements".equals(action)) {
            CordovaGoogleMaps.this.clearHtmlElements(args, callbackContext);
          } else if ("pause".equals(action)) {
            CordovaGoogleMaps.this.pause(args, callbackContext);
          } else if ("resume".equals(action)) {
            CordovaGoogleMaps.this.resume(args, callbackContext);
          } else if ("getMap".equals(action)) {
            CordovaGoogleMaps.this.getMap(args, callbackContext);
          } else if ("getPanorama".equals(action)) {
            CordovaGoogleMaps.this.getPanorama(args, callbackContext);
          } else if ("removeMap".equals(action)) {
            CordovaGoogleMaps.this.removeMap(args, callbackContext);
          } else if ("backHistory".equals(action)) {
            CordovaGoogleMaps.this.backHistory(args, callbackContext);
          } else if ("updateMapPositionOnly".equals(action)) {
            CordovaGoogleMaps.this.updateMapPositionOnly(args, callbackContext);
          }

        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });
    return true;

  }

  public void updateMapPositionOnly(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final JSONObject elements = args.getJSONObject(0);

    Bundle elementsBundle = PluginUtil.Json2Bundle(elements);
    float zoomScale = Resources.getSystem().getDisplayMetrics().density;

    Iterator<String> domIDs = elementsBundle.keySet().iterator();
    String domId;
    Bundle domInfo, size, currentDomInfo;
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
  public void backHistory(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (!webView.backHistory()) {
          // If no more history back, exit the app
          cordova.getActivity().finish();
        }
      }
    });
  }



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
  public void clearHtmlElements(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (mPluginLayout == null) {
      callbackContext.success();
      return;
    }
    mPluginLayout.clearHtmlElements();
    callbackContext.success();
  }
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
    if (mPluginLayout == null || mPluginLayout.pluginOverlays == null) {
      return;
    }

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {

        mPluginLayout.setBackgroundColor(Color.WHITE);

        Set<String> mapIds = mPluginLayout.pluginOverlays.keySet();
        IPluginView pluginOverlay;

        // prevent the ConcurrentModificationException error.
        String[] mapIdArray= mapIds.toArray(new String[mapIds.size()]);
        for (String mapId : mapIdArray) {
          if (mPluginLayout.pluginOverlays.containsKey(mapId)) {
            pluginOverlay = mPluginLayout.removePluginOverlay(mapId);
            pluginOverlay.remove(null, null);
            pluginOverlay.onDestroy();
            mPluginLayout.HTMLNodes.remove(mapId);
          }
        }
        mPluginLayout.HTMLNodes.clear();
        mPluginLayout.pluginOverlays.clear();

        System.gc();
        Runtime.getRuntime().gc();
      }
    });

  }

  public void removeMap(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    if (mPluginLayout.pluginOverlays.containsKey(mapId)) {
      IPluginView pluginOverlay = mPluginLayout.removePluginOverlay(mapId);
      if (pluginOverlay != null) {
        pluginOverlay.remove(null, null);
        pluginOverlay.onDestroy();
        mPluginLayout.HTMLNodes.remove(mapId);
        pluginOverlay = null;
      }

      try {
        Field pluginMapField = pluginManager.getClass().getDeclaredField("pluginMap");
        pluginMapField.setAccessible(true);
        LinkedHashMap<String, CordovaPlugin> pluginMapInstance = (LinkedHashMap<String, CordovaPlugin>) pluginMapField.get(pluginManager);
        pluginMapInstance.remove(mapId);
        Field entryMapField = pluginManager.getClass().getDeclaredField("entryMap");
        entryMapField.setAccessible(true);
        LinkedHashMap<String, PluginEntry> entryMapInstance = (LinkedHashMap<String, PluginEntry>) entryMapField.get(pluginManager);
        entryMapInstance.remove(mapId);
      } catch (Exception e) {
        e.printStackTrace();
      }


    }



    System.gc();
    Runtime.getRuntime().gc();
    callbackContext.success();
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public void getMap(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    //------------------------------------------
    // Create an instance of PluginMap class.
    //------------------------------------------
    JSONObject meta = args.getJSONObject(0);
    String mapId = meta.getString("__pgmId");
    PluginMap pluginMap = new PluginMap();
    pluginMap.privateInitialize(mapId, cordova, webView, null);
    pluginMap.initialize(cordova, webView);
    pluginMap.mapCtrl = CordovaGoogleMaps.this;
    pluginMap.self = pluginMap;

    PluginEntry pluginEntry = new PluginEntry(mapId, pluginMap);
    pluginManager.addService(pluginEntry);

    pluginMap.getMap(args, callbackContext);
  }


  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public void getPanorama(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    //------------------------------------------
    // Create an instance of PluginStreetView class.
    //------------------------------------------
    JSONObject meta = args.getJSONObject(0);
    String mapId = meta.getString("__pgmId");
    Log.d(TAG, "---> mapId = " + mapId);
    PluginStreetViewPanorama pluginStreetView = new PluginStreetViewPanorama();
    pluginStreetView.privateInitialize(mapId, cordova, webView, null);
    pluginStreetView.initialize(cordova, webView);
    pluginStreetView.mapCtrl = CordovaGoogleMaps.this;
    pluginStreetView.self = pluginStreetView;

    PluginEntry pluginEntry = new PluginEntry(mapId, pluginStreetView);
    pluginManager.addService(pluginEntry);

    pluginStreetView.getPanorama(args, callbackContext);
  }

  @Override
  public void onStart() {
    super.onStart();

    Collection<PluginEntry>pluginEntries = pluginManager.getPluginEntries();
    for (PluginEntry pluginEntry: pluginEntries) {
      if (pluginEntry.service.startsWith("map_")) {
        pluginEntry.plugin.onStart();
      }
    }

  }
  @Override
  public void onStop() {
    super.onStop();

    Collection<PluginEntry>pluginEntries = pluginManager.getPluginEntries();
    for (PluginEntry pluginEntry: pluginEntries) {
      if (pluginEntry.service.startsWith("map_")) {
        pluginEntry.plugin.onStop();
      }
    }

  }
  @Override
  public void onPause(boolean multitasking) {
    super.onPause(multitasking);
    if (mPluginLayout != null) {
      mPluginLayout.stopTimer();
    }

    Collection<PluginEntry>pluginEntries = pluginManager.getPluginEntries();
    for (PluginEntry pluginEntry: pluginEntries) {
      if (pluginEntry.service.startsWith("map_")) {
        pluginEntry.plugin.onPause(multitasking);
      }
    }

  }

  @Override
  public void onResume(boolean multitasking) {
    Collection<PluginEntry>pluginEntries = pluginManager.getPluginEntries();
    for (Iterator<PluginEntry> iterator = pluginEntries.iterator(); iterator.hasNext();) {
      PluginEntry pluginEntry = iterator.next();
      if (pluginEntry.service.startsWith("map_")) {
        pluginEntry.plugin.onResume(multitasking);
      }
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    Collection<PluginEntry>pluginEntries = pluginManager.getPluginEntries();
    for (PluginEntry pluginEntry: pluginEntries) {
      if (pluginEntry.service.startsWith("map_")) {
        pluginEntry.plugin.onDestroy();
      }
    }

  }

 /**
   * Called by the system when the device configuration changes while your activity is running.
   *
   * @param newConfig		The new device configuration
   */
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

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
    /*
    // Checks the orientation of the screen
    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      Toast.makeText(activity, "landscape", Toast.LENGTH_SHORT).show();
    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
      Toast.makeText(activity, "portrait", Toast.LENGTH_SHORT).show();
    }
    */
  }

}
