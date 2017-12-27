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
  public static String CURRENT_URL;

  @SuppressLint("NewApi") @Override
  public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
    if (root != null) {
      return;
    }
    LOG.setLogLevel(LOG.ERROR);

    activity = cordova.getActivity();
    final View view = webView.getView();
    view.getViewTreeObserver().addOnScrollChangedListener(CordovaGoogleMaps.this);
    root = (ViewGroup) view.getParent();

    pluginManager = webView.getPluginManager();

    cordova.getActivity().runOnUiThread(new Runnable() {
      @SuppressLint("NewApi")
      public void run() {
        CURRENT_URL = webView.getUrl();

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

          String errorMsg = "Google Maps Android API v2 is not available for some reason on this device. Do you install the latest Google Play Services from Google Play Store?";
          switch (checkGooglePlayServices) {
            case ConnectionResult.DEVELOPER_ERROR:
              errorMsg = "The application is misconfigured. This error is not recoverable and will be treated as fatal. The developer should look at the logs after this to determine more actionable information.";
              break;
            case ConnectionResult.INTERNAL_ERROR:
              errorMsg = "An internal error of Google Play Services occurred. Please retry, and it should resolve the problem.";
              break;
            case ConnectionResult.INVALID_ACCOUNT:
              errorMsg = "You attempted to connect to the service with an invalid account name specified.";
              break;
            case ConnectionResult.LICENSE_CHECK_FAILED:
              errorMsg = "The application is not licensed to the user. This error is not recoverable and will be treated as fatal.";
              break;
            case ConnectionResult.NETWORK_ERROR:
              errorMsg = "A network error occurred. Please retry, and it should resolve the problem.";
              break;
            case ConnectionResult.SERVICE_DISABLED:
              errorMsg = "The installed version of Google Play services has been disabled on this device. Please turn on Google Play Services.";
              break;
            case ConnectionResult.SERVICE_INVALID:
              errorMsg = "The version of the Google Play services installed on this device is not authentic. Please update the Google Play Services from Google Play Store.";
              isNeedToUpdate = true;
              break;
            case ConnectionResult.SERVICE_MISSING:
              errorMsg = "Google Play services is missing on this device. Please install the Google Play Services.";
              isNeedToUpdate = true;
              break;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
              errorMsg = "The installed version of Google Play services is out of date. Please update the Google Play Services from Google Play Store.";
              isNeedToUpdate = true;
              break;
            case ConnectionResult.SIGN_IN_REQUIRED:
              errorMsg = "You attempted to connect to the service but you are not signed in. Please check the Google Play Services configuration";
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
              .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                  dialog.dismiss();
                  if (finalIsNeedToUpdate) {
                    try {
                      activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.gms")));
                    } catch (android.content.ActivityNotFoundException anfe) {
                      activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=appPackageName")));
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
        mPluginLayout.isSuspended = true;


        // Check the API key
        ApplicationInfo appliInfo = null;
        try {
          appliInfo = activity.getPackageManager().getApplicationInfo(activity.getPackageName(), PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {}

        String API_KEY = appliInfo.metaData.getString("com.google.android.maps.v2.API_KEY");
        if ("API_KEY_FOR_ANDROID".equals(API_KEY)) {

          AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);

          alertDialogBuilder
              .setMessage("Please replace 'API_KEY_FOR_ANDROID' in the platforms/android/AndroidManifest.xml with your API Key!")
              .setCancelable(false)
              .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                  dialog.dismiss();
                }
              });
          AlertDialog alertDialog = alertDialogBuilder.create();

          // show it
          alertDialog.show();
        }

        CURRENT_URL = webView.getUrl();


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
    mPluginLayout.isSuspended = true;
    /*
    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        webView.loadUrl("javascript:if(window.cordova){cordova.fireDocumentEvent('plugin_url_changed', {});}");
      }
    });
    */
    CURRENT_URL = url;
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

      domInfo.remove("size");

      currentDomInfo = mPluginLayout.HTMLNodes.get(domId);
      currentDomInfo.putInt("zIndex", domInfo.getInt("zIndex"));
      mPluginLayout.HTMLNodes.put(domId, currentDomInfo);
    }

    if (mPluginLayout.isSuspended) {
      mPluginLayout.isSuspended = false;
      synchronized (mPluginLayout.timerLock) {
        mPluginLayout.timerLock.notify();
      }
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



  public void pause(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (mPluginLayout == null) {
      callbackContext.success();
      return;
    }
    mPluginLayout.isSuspended = true;
    callbackContext.success();
  }
  public void resume(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (mPluginLayout == null) {
      callbackContext.success();
      return;
    }
    if (mPluginLayout.isSuspended) {
      mPluginLayout.isSuspended = false;
      synchronized (mPluginLayout.timerLock) {
        mPluginLayout.timerLock.notify();
      }
    }
    callbackContext.success();
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

    synchronized (mPluginLayout.timerLock) {
      mPluginLayout.timerLock.notify();
    }
    callbackContext.success();
  }

  @Override
  public void onReset() {
    super.onReset();
    if (mPluginLayout == null || mPluginLayout.pluginMaps == null) {
      return;
    }

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        CURRENT_URL = webView.getUrl();

        mPluginLayout.setBackgroundColor(Color.WHITE);

        Set<String> mapIds = mPluginLayout.pluginMaps.keySet();
        PluginMap pluginMap;

        // prevent the ConcurrentModificationException error.
        String[] mapIdArray= mapIds.toArray(new String[mapIds.size()]);
        for (String mapId : mapIdArray) {
          if (mPluginLayout.pluginMaps.containsKey(mapId)) {
            pluginMap = mPluginLayout.removePluginMap(mapId);
            pluginMap.remove(null, null);
            pluginMap.onDestroy();
            mPluginLayout.HTMLNodes.remove(mapId);
          }
        }
        mPluginLayout.HTMLNodes.clear();
        mPluginLayout.pluginMaps.clear();

        System.gc();
        Runtime.getRuntime().gc();
      }
    });

  }

  public void removeMap(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    if (mPluginLayout.pluginMaps.containsKey(mapId)) {
      PluginMap pluginMap = mPluginLayout.removePluginMap(mapId);
      if (pluginMap != null) {
        pluginMap.remove(null, null);
        pluginMap.onDestroy();
        pluginMap.objects.clear();
        pluginMap.objects.destroy();
        mPluginLayout.HTMLNodes.remove(mapId);
        pluginMap = null;
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
    String mapId = args.getString(0);
    PluginMap pluginMap = new PluginMap();
    pluginMap.privateInitialize(mapId, cordova, webView, null);
    pluginMap.initialize(cordova, webView);
    pluginMap.mapCtrl = CordovaGoogleMaps.this;
    pluginMap.self = pluginMap;
    ((MyPlugin)pluginMap).CURRENT_PAGE_URL = CURRENT_URL;

    PluginEntry pluginEntry = new PluginEntry(mapId, pluginMap);
    pluginManager.addService(pluginEntry);

    pluginMap.getMap(args, callbackContext);
  }

  @Override
  public void onPause(boolean multitasking) {
    super.onPause(multitasking);
    cordova.getThreadPool().submit(new Runnable() {
      @Override
      public void run() {

        Set<String> mapIds = mPluginLayout.pluginMaps.keySet();
        PluginMap pluginMap;

        // prevent the ConcurrentModificationException error.
        String[] mapIdArray= mapIds.toArray(new String[mapIds.size()]);
        for (String mapId : mapIdArray) {
          if (mPluginLayout.pluginMaps.containsKey(mapId)) {
            pluginMap = mPluginLayout.pluginMaps.get(mapId);
            pluginMap.mapView.onPause();
          }
        }
      }
    });
  }

  @Override
  public void onResume(boolean multitasking) {
    super.onResume(multitasking);
    if (mPluginLayout != null) {
      mPluginLayout.isSuspended = false;

      if (mPluginLayout.pluginMaps.size() > 0) {
        this.activity.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            CURRENT_URL = webView.getUrl();
            webView.loadUrl("javascript:if(window.cordova){cordova.fireDocumentEvent('plugin_touch', {});}");
          }
        });
      }
    }

    cordova.getThreadPool().submit(new Runnable() {
      @Override
      public void run() {

        Set<String> mapIds = mPluginLayout.pluginMaps.keySet();
        PluginMap pluginMap;

        // prevent the ConcurrentModificationException error.
        String[] mapIdArray= mapIds.toArray(new String[mapIds.size()]);
        for (String mapId : mapIdArray) {
          if (mPluginLayout.pluginMaps.containsKey(mapId)) {
            pluginMap = mPluginLayout.pluginMaps.get(mapId);
            pluginMap.mapView.onResume();
          }
        }
      }
    });
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    cordova.getThreadPool().submit(new Runnable() {
      @Override
      public void run() {

        Set<String> mapIds = mPluginLayout.pluginMaps.keySet();
        PluginMap pluginMap;

        // prevent the ConcurrentModificationException error.
        String[] mapIdArray= mapIds.toArray(new String[mapIds.size()]);
        for (String mapId : mapIdArray) {
          if (mPluginLayout.pluginMaps.containsKey(mapId)) {
            pluginMap = mPluginLayout.pluginMaps.get(mapId);
            pluginMap.mapView.onDestroy();
          }
        }
      }
    });
  }

  protected void sendNoResult(CallbackContext callbackContext) {
    PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
    pluginResult.setKeepCallback(true);
    callbackContext.sendPluginResult(pluginResult);
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
