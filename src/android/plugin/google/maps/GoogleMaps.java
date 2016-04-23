package plugin.google.maps;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.RectF;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginEntry;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation")
public class GoogleMaps extends CordovaPlugin implements View.OnClickListener,
      ViewTreeObserver.OnScrollChangedListener{
  private final String TAG = "GoogleMapsPlugin";
  private float density;
  private HashMap<String, Bundle> bufferForLocationDialog = new HashMap<String, Bundle>();
  private FrameLayout mapFrame = null;

  private final int ACTIVITY_LOCATION_DIALOG = 0x7f999900; // Invite the location dialog using Google Play Services
  private final int ACTIVITY_LOCATION_PAGE = 0x7f999901;   // Open the location settings page

  public HashMap<String, RectF> mapDivLayouts = new HashMap<String, RectF>();
  private Activity activity;
  private LinearLayout windowLayer = null;
  private ViewGroup root;
  private final int CLOSE_LINK_ID = 0x7f999990;  //random
  private final int LICENSE_LINK_ID = 0x7f99991; //random
  private final String PLUGIN_VERSION = "1.3.3";
  public MyPluginLayout mPluginLayout = null;
  public boolean isDebug = true;
  private GoogleApiClient googleApiClient = null;
  private JSONArray _saveArgs = null;
  private CallbackContext _saveCallbackContext = null;
  public final HashMap<String, Method> methods = new HashMap<String, Method>();
  protected HashMap<String, MapView> mapViews = new HashMap<String, MapView>();
  protected HashMap<String, PluginMap> mapPlugins = new HashMap<String, PluginMap>();
  private String DIALOG_MAP_ID = null;
  private boolean initialized = false;
  public PluginManager pluginManager;

  @SuppressLint("NewApi") @Override
  public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
    if (root != null) {
      return;
    }
    activity = cordova.getActivity();
    density = Resources.getSystem().getDisplayMetrics().density;
    final View view = webView.getView();
    view.getViewTreeObserver().addOnScrollChangedListener(GoogleMaps.this);
    root = (ViewGroup) view.getParent();

    Method[] classMethods = this.getClass().getMethods();
    for (int i = 0; i < classMethods.length; i++) {
      methods.put(classMethods[i].getName(), classMethods[i]);
    }

    cordova.getActivity().runOnUiThread(new Runnable() {
      @SuppressLint("NewApi")
      public void run() {
        pluginManager = webView.getPluginManager();

      /*
          try {
            Method method = webView.getClass().getMethod("getSettings");
            WebSettings settings = (WebSettings)method.invoke(null);
            settings.setRenderPriority(RenderPriority.HIGH);
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
          } catch (Exception e) {
            e.printStackTrace();
          }
       */
        if (Build.VERSION.SDK_INT >= 21 || "org.xwalk.core.XWalkView".equals(view.getClass().getName())){
          view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        root.setBackgroundColor(Color.WHITE);
        /*
         * Deprecated the below code for old Android versions.
         *
        if (VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
          activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }
        if (VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
          Log.d(TAG, "Google Maps Plugin reloads the browser to change the background color as transparent.");
          view.setBackgroundColor(0);
            try {
              Method method = webView.getClass().getMethod("reload");
              method.invoke(webView);
            } catch (Exception e) {
              e.printStackTrace();
            }
        }
        */

        mPluginLayout = new MyPluginLayout(webView.getView(), activity);

        // ------------------------------
        // Check of Google Play Services
        // ------------------------------
        int checkGooglePlayServices = GooglePlayServicesUtil
            .isGooglePlayServicesAvailable(activity);

        if (checkGooglePlayServices != ConnectionResult.SUCCESS) {
          // google play services is missing!!!!
          /*
           * Returns status code indicating whether there was an error. Can be one
           * of following in ConnectionResult: SUCCESS, SERVICE_MISSING,
           * SERVICE_VERSION_UPDATE_REQUIRED, SERVICE_DISABLED, SERVICE_INVALID.
           */
          Log.e(TAG, "---Google Play Services is not available: " + GooglePlayServicesUtil.getErrorString(checkGooglePlayServices));

          Dialog errorDialog = null;
          try {
            //errorDialog = GooglePlayServicesUtil.getErrorDialog(checkGooglePlayServices, activity, 1);
            Method getErrorDialogMethod = GooglePlayServicesUtil.class.getMethod("getErrorDialog", int.class, Activity.class, int.class);
            errorDialog = (Dialog)getErrorDialogMethod.invoke(null, checkGooglePlayServices, activity, 1);
          } catch (Exception e) {};

          if (errorDialog != null) {
            errorDialog.show();
          } else {
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
          }

          Log.e(TAG, "Google Play Services is not available.");
          return;
        }

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

      }
    });


  }

  @Override
  public void onScrollChanged() {
    if (mPluginLayout == null) {
      return;
    }
    View view = webView.getView();
    mPluginLayout.scrollTo(view.getScrollX(), view.getScrollY());

    Iterator<Map.Entry<String, RectF>> iterator = mapDivLayouts.entrySet().iterator();
    Map.Entry<String, RectF> entry;
    String mapId;
    RectF rectF;
    while (iterator.hasNext()) {
      entry = iterator.next();
      mapId = entry.getKey();
      rectF = entry.getValue();

      mPluginLayout.setDrawingRect(
          mapId,
          rectF.left,
          rectF.top - view.getScrollY(),
          rectF.left + rectF.width(),
          rectF.top + rectF.height() - view.getScrollY());
    }
  }

  @Override
  public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (isDebug) {
      if (args != null && args.length() > 0) {
        Log.d(TAG, "(debug)action=" + action + " args[0]=" + args.getString(0));
      } else {
        Log.d(TAG, "(debug)action=" + action);
      }
    }

    if (methods.containsKey(action)) {
      if (isDebug) {
        if (args != null && args.length() > 0) {
          Log.d(TAG, "(debug)action=" + action + " args[0]=" + args.getString(0));
        } else {
          Log.d(TAG, "(debug)action=" + action);
        }
      }
      Method method = methods.get(action);
      try {
        method.invoke(this, args, callbackContext);
        return true;
      } catch (Exception e) {
        e.printStackTrace();
        callbackContext.error("" + e.getMessage());
        return false;
      }
    } else {
      return false;
    }

  }


  public float contentToView(long d) {
    return d * this.density;
  }


  @SuppressWarnings("unused")
  private Boolean getLicenseInfo(JSONArray args, CallbackContext callbackContext) {
    String msg = GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(activity);
    callbackContext.success(msg);
    return true;
  }

  public void closeWindow() {
    if (DIALOG_MAP_ID == null) {
      return;
    }

    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        MapView mapView = mapViews.get(DIALOG_MAP_ID);
        if (mapView != null) {
          mapFrame.removeView(mapView);
        }
        mPluginLayout.addMapView(DIALOG_MAP_ID, mapView);
        mPluginLayout.updateViewPosition(DIALOG_MAP_ID);
        root.removeView(windowLayer);
        mapFrame.destroyDrawingCache();
        mapFrame = null;
        windowLayer.destroyDrawingCache();
        windowLayer = null;

        PluginMap pluginMap = mapPlugins.get(DIALOG_MAP_ID);
        pluginMap.onMapEvent("map_close");
      }
    });
  }
  @SuppressWarnings("unused")
  public void showDialog(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (DIALOG_MAP_ID != null) {
      return;
    }
    DIALOG_MAP_ID = args.getString(0);

    // window layout
    windowLayer = new LinearLayout(activity);
    windowLayer.setPadding(0, 0, 0, 0);
    LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
    windowLayer.setLayoutParams(layoutParams);


    // dialog window layer
    final FrameLayout dialogLayer = new FrameLayout(activity);
    dialogLayer.setLayoutParams(layoutParams);
    //dialogLayer.setPadding(15, 15, 15, 0);
    dialogLayer.setBackgroundColor(Color.LTGRAY);
    windowLayer.addView(dialogLayer);

    // map frame
    mapFrame = new FrameLayout(activity);
    mapFrame.setPadding(0, 0, 0, (int)(40 * density));
    dialogLayer.addView(mapFrame);

    this.mPluginLayout.removeMapView(DIALOG_MAP_ID);

    ViewGroup.LayoutParams lParams = mapViews.get(DIALOG_MAP_ID).getLayoutParams();
    if (lParams == null) {
      lParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }
    lParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
    lParams.height = ViewGroup.LayoutParams.MATCH_PARENT;

    final ViewGroup.LayoutParams fLParams = lParams;

    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        MapView mapView = mapViews.get(DIALOG_MAP_ID);

        if (fLParams instanceof AbsoluteLayout.LayoutParams) {
          AbsoluteLayout.LayoutParams params = (AbsoluteLayout.LayoutParams) fLParams;
          params.x = 0;
          params.y = 0;
          mapView.setLayoutParams(params);
        } else if (fLParams instanceof LinearLayout.LayoutParams) {
          LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) fLParams;
          params.topMargin = 0;
          params.leftMargin = 0;
          mapView.setLayoutParams(params);
        } else if (fLParams instanceof FrameLayout.LayoutParams) {
          FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) fLParams;
          params.topMargin = 0;
          params.leftMargin = 0;
          mapView.setLayoutParams(params);
        }

        if (mapView.getParent() != null) {
          ((ViewGroup)(mapView.getParent())).removeView(mapView);
        }
        mapFrame.addView(mapView);

        // button frame
        LinearLayout buttonFrame = new LinearLayout(activity);
        buttonFrame.setOrientation(LinearLayout.HORIZONTAL);
        buttonFrame.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM);
        LinearLayout.LayoutParams buttonFrameParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        buttonFrame.setLayoutParams(buttonFrameParams);
        dialogLayer.addView(buttonFrame);

        //close button
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT, 1.0f);
        TextView closeLink = new TextView(activity);
        closeLink.setText("Close");
        closeLink.setLayoutParams(buttonParams);
        closeLink.setTextColor(Color.BLUE);
        closeLink.setTextSize(20);
        closeLink.setGravity(Gravity.LEFT);
        closeLink.setPadding((int)(10 * density), 0, 0, (int)(10 * density));
        closeLink.setOnClickListener(GoogleMaps.this);
        closeLink.setId(CLOSE_LINK_ID);
        buttonFrame.addView(closeLink);

        //license button
        TextView licenseLink = new TextView(activity);
        licenseLink.setText("Legal Notices");
        licenseLink.setTextColor(Color.BLUE);
        licenseLink.setLayoutParams(buttonParams);
        licenseLink.setTextSize(20);
        licenseLink.setGravity(Gravity.RIGHT);
        licenseLink.setPadding((int)(10 * density), (int)(20 * density), (int)(10 * density), (int)(10 * density));
        licenseLink.setOnClickListener(GoogleMaps.this);
        licenseLink.setId(LICENSE_LINK_ID);
        buttonFrame.addView(licenseLink);

        //webView.getView().setVisibility(View.INVISIBLE);
        root.addView(windowLayer);

        callbackContext.success();
      }
    });
  }


  public void updateMapViewLayout() {
    View view = webView.getView();
    mPluginLayout.scrollTo(view.getScrollX(), view.getScrollY());

    Iterator<Map.Entry<String, RectF>> iterator = mapDivLayouts.entrySet().iterator();
    Map.Entry<String, RectF> entry;
    String mapId;
    RectF rectF;
    while (iterator.hasNext()) {
      entry = iterator.next();
      mapId = entry.getKey();
      rectF = entry.getValue();
      //Log.d(TAG, "---> updateMapViewLayout / " + mapId + " / " + rectF.left + ", " + rectF.top + " - " + rectF.width() + ", " + rectF.height());

      mPluginLayout.setDrawingRect(
          mapId,
          rectF.left,
          rectF.top - webView.getView().getScrollY(),
          rectF.left + rectF.width(),
          rectF.top + rectF.height() - webView.getView().getScrollY());
      mPluginLayout.updateViewPosition(mapId);
    }
  }

  @SuppressWarnings("unused")
  public void closeDialog(final JSONArray args, final CallbackContext callbackContext) {
    this.closeWindow();
    this.sendNoResult(callbackContext);
  }

  @SuppressWarnings("unused")
  public void isAvailable(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    // ------------------------------
    // Check of Google Play Services
    // ------------------------------
    int checkGooglePlayServices = GooglePlayServicesUtil
        .isGooglePlayServicesAvailable(activity);
    if (checkGooglePlayServices != ConnectionResult.SUCCESS) {
      // google play services is missing!!!!
      callbackContext.error("Google Maps Android API v2 is not available, because this device does not have Google Play Service.");
      return;
    }


    // ------------------------------
    // Check of Google Maps Android API v2
    // ------------------------------
    try {
      @SuppressWarnings({ "rawtypes" })
      Class GoogleMapsClass = Class.forName("com.google.android.gms.maps.GoogleMap");
    } catch (Exception e) {
      Log.e("GoogleMaps", "Error", e);
      callbackContext.error(e.getMessage());
      return;
    }

    callbackContext.success();
  }


  public void requestPermissions(CordovaPlugin plugin, int requestCode, String[] permissions) {

    try {
      Method requestPermission = CordovaInterface.class.getDeclaredMethod(
          "requestPermissions", CordovaPlugin.class, int.class, String[].class);

      // If there is no exception, then this is cordova-android 5.0.0+
      requestPermission.invoke(plugin.cordova, plugin, requestCode, permissions);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void onRequestPermissionResult(int requestCode, String[] permissions,
                                        int[] grantResults) throws JSONException {
    PluginResult result;
    for (int r : grantResults) {
      if (r == PackageManager.PERMISSION_DENIED) {
        result = new PluginResult(PluginResult.Status.ERROR, "Geolocation permission request was denied.");
        _saveCallbackContext.sendPluginResult(result);
        return;
      }
    }
    GoogleMaps.this.getMyLocation(_saveArgs, _saveCallbackContext);
  }
  @SuppressWarnings("unused")
  public void getMyLocation(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    // enableHighAccuracy = true -> PRIORITY_HIGH_ACCURACY
    // enableHighAccuracy = false -> PRIORITY_BALANCED_POWER_ACCURACY

    JSONObject params = args.getJSONObject(0);
    boolean isHigh = false;
    if (params.has("enableHighAccuracy")) {
      isHigh = params.getBoolean("enableHighAccuracy");
    }

    // Request geolocation permission.
    boolean locationPermission;
    try {
      Method hasPermission = CordovaInterface.class.getDeclaredMethod("hasPermission", String.class);

      String permission = "android.permission.ACCESS_COARSE_LOCATION";
      locationPermission = (Boolean) hasPermission.invoke(cordova, permission);
    } catch (Exception e) {
      PluginResult result;
      result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION);
      callbackContext.sendPluginResult(result);
      return;
    }

    if (!locationPermission) {
      _saveArgs = args;
      _saveCallbackContext = callbackContext;
      this.requestPermissions(GoogleMaps.this, 0, new String[]{"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"});
      return;
    }


    final boolean enableHighAccuracy = isHigh;

    if (googleApiClient == null) {
      googleApiClient = new GoogleApiClient.Builder(this.activity)
        .addApi(LocationServices.API)
        .addConnectionCallbacks(new com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks() {

          @Override
          public void onConnected(Bundle connectionHint) {
            Log.e(TAG, "===> onConnected");
            GoogleMaps.this.sendNoResult(callbackContext);

            _checkLocationSettings(enableHighAccuracy, callbackContext);
          }

          @Override
          public void onConnectionSuspended(int cause) {
            Log.e(TAG, "===> onConnectionSuspended");
           }

        })
        .addOnConnectionFailedListener(new com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener() {

          @Override
          public void onConnectionFailed(ConnectionResult result) {
            Log.e(TAG, "===> onConnectionFailed");

            PluginResult tmpResult = new PluginResult(PluginResult.Status.ERROR, result.toString());
            tmpResult.setKeepCallback(false);
            callbackContext.sendPluginResult(tmpResult);

            googleApiClient.disconnect();
          }

        })
        .build();
      googleApiClient.connect();
    } else if (googleApiClient.isConnected()) {
      _checkLocationSettings(enableHighAccuracy, callbackContext);
    }

  }

  public void _checkLocationSettings(final boolean enableHighAccuracy, final CallbackContext callbackContext) {

    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();

    LocationRequest locationRequest;
    locationRequest = LocationRequest.create()
        .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    builder.addLocationRequest(locationRequest);

    if (enableHighAccuracy) {
      locationRequest = LocationRequest.create()
          .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
      builder.addLocationRequest(locationRequest);
    }

    PendingResult<LocationSettingsResult> locationSettingsResult =
        LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());

    locationSettingsResult.setResultCallback(new ResultCallback<LocationSettingsResult>() {

      @Override
      public void onResult(LocationSettingsResult result) {
        final Status status = result.getStatus();
        switch (status.getStatusCode()) {
          case LocationSettingsStatusCodes.SUCCESS:
            _requestLocationUpdate(false, enableHighAccuracy, callbackContext);
            break;

          case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
            // Location settings are not satisfied. But could be fixed by showing the user
            // a dialog.
            try {
              //Keep the callback id
              Bundle bundle = new Bundle();
              bundle.putInt("type", ACTIVITY_LOCATION_DIALOG);
              bundle.putString("callbackId", callbackContext.getCallbackId());
              bundle.putBoolean("enableHighAccuracy", enableHighAccuracy);
              int hashCode = bundle.hashCode();

              bufferForLocationDialog.put("bundle_" + hashCode, bundle);
              GoogleMaps.this.sendNoResult(callbackContext);

              // Show the dialog by calling startResolutionForResult(),
              // and check the result in onActivityResult().
              cordova.setActivityResultCallback(GoogleMaps.this);
              status.startResolutionForResult(cordova.getActivity(), hashCode);
            } catch (SendIntentException e) {
              // Show the dialog that is original version of this plugin.
              _showLocationSettingsPage(enableHighAccuracy, callbackContext);
            }
            break;

          case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
            // Location settings are not satisfied. However, we have no way to fix the
            // settings so we won't show the dialog.

            JSONObject jsResult = new JSONObject();
            try {
              jsResult.put("status", false);
              jsResult.put("error_code", "service_not_available");
              jsResult.put("error_message", "This app has been rejected to use Location Services.");
            } catch (JSONException e) {}
            callbackContext.error(jsResult);
            break;
        }
      }

    });
  }

  public void _showLocationSettingsPage(final boolean enableHighAccuracy, final CallbackContext callbackContext) {
    //Ask the user to turn on the location services.
    AlertDialog.Builder builder = new AlertDialog.Builder(this.activity);
    builder.setTitle("Improve location accuracy");
    builder.setMessage("To enhance your Maps experience:\n\n" +
        " - Enable Google apps location access\n\n" +
        " - Turn on GPS and mobile network location");
    builder.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          //Keep the callback id
          Bundle bundle = new Bundle();
          bundle.putInt("type", ACTIVITY_LOCATION_PAGE);
          bundle.putString("callbackId", callbackContext.getCallbackId());
          bundle.putBoolean("enableHighAccuracy", enableHighAccuracy);
          int hashCode = bundle.hashCode();

          bufferForLocationDialog.put("bundle_" + hashCode, bundle);
          GoogleMaps.this.sendNoResult(callbackContext);

          //Launch settings, allowing user to make a change
          cordova.setActivityResultCallback(GoogleMaps.this);
          Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
          activity.startActivityForResult(intent, hashCode);
        }
    });
    builder.setNegativeButton("Skip", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          //No location service, no Activity
          dialog.dismiss();

          JSONObject result = new JSONObject();
          try {
            result.put("status", false);
            result.put("error_code", "service_denied");
            result.put("error_message", "This app has been rejected to use Location Services.");
          } catch (JSONException e) {}
          callbackContext.error(result);
        }
    });
    builder.create().show();
    return;
  }

  public void _requestLocationUpdate(final boolean isRetry, final boolean enableHighAccuracy, final CallbackContext callbackContext) {

    int priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
    if (enableHighAccuracy) {
      priority = LocationRequest.PRIORITY_HIGH_ACCURACY;
    }

    LocationRequest locationRequest= LocationRequest.create()
        .setExpirationTime(5000)
        .setNumUpdates(2)
        .setSmallestDisplacement(0)
        .setPriority(priority)
        .setInterval(5000);


    final PendingResult<Status> result =  LocationServices.FusedLocationApi.requestLocationUpdates(
        googleApiClient, locationRequest, new LocationListener() {

          @Override
          public void onLocationChanged(Location location) {
            /*
            if (callbackContext.isFinished()) {
              return;
            }
            */
            JSONObject result;
            try {
              result = PluginUtil.location2Json(location);
              result.put("status", true);
              callbackContext.success(result);
            } catch (JSONException e) {}

            googleApiClient.disconnect();
          }

        });

    result.setResultCallback(new ResultCallback<Status>() {

      public void onResult(Status status) {
        if (!status.isSuccess()) {
          String errorMsg = status.getStatusMessage();
          PluginResult result = new PluginResult(PluginResult.Status.ERROR, errorMsg);
          callbackContext.sendPluginResult(result);
        } else {
          // no update location
          Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
          if (location != null) {
            try {
              JSONObject result = PluginUtil.location2Json(location);
              result.put("status", true);
              callbackContext.success(result);
            } catch (JSONException e) {
              e.printStackTrace();
            }
            return;
          } else {
            if (isRetry == false) {
              Toast.makeText(activity, "Waiting for location...", Toast.LENGTH_SHORT).show();

              GoogleMaps.this.sendNoResult(callbackContext);

              // Retry
              Handler handler = new Handler();
              handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                  _requestLocationUpdate(true, enableHighAccuracy, callbackContext);
                }
              }, 3000);
            } else {
              // Send back the error result
              JSONObject result = new JSONObject();
              try {
                result.put("status", false);
                result.put("error_code", "cannot_detect");
                result.put("error_message", "Can not detect your location. Try again.");
              } catch (JSONException e) {}
              callbackContext.error(result);
            }
          }
        }
      }
    });
  }

  public void showLicenseText() {
    AsyncLicenseInfo showLicense = new AsyncLicenseInfo(activity);
    showLicense.execute();
  }


  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public void getMap(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    //------------------------------
    // Initialize Google Maps SDK
    //------------------------------
    if (!initialized) {
      try {
        MapsInitializer.initialize(cordova.getActivity());
        initialized = true;
      } catch (Exception e) {
        e.printStackTrace();
        callbackContext.error(e.getMessage());
        return;
      }
    }

    //------------------------------------------
    // Create an instance of PluginMap class.
    //------------------------------------------
    try {
      String mapId = args.getString(0);
      PluginMap pluginMap = new PluginMap();
      pluginMap.privateInitialize(mapId, this.cordova, webView, null);
      pluginMap.initialize(this.cordova, webView);
      pluginMap.mapCtrl = this;

      PluginEntry pluginEntry = new PluginEntry(mapId, pluginMap);
      pluginManager.addService(pluginEntry);

      mapPlugins.put(mapId, pluginMap);

      pluginMap.getMap(args, callbackContext);

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (!bufferForLocationDialog.containsKey("bundle_" + requestCode)) {
      Log.e(TAG, "no key");
      return;
    }
    Bundle query = bufferForLocationDialog.get("bundle_" + requestCode);
    Log.d(TAG, "====> onActivityResult (" + resultCode + ")");

    switch (query.getInt("type")) {
      case ACTIVITY_LOCATION_DIALOG:
        // User was asked to enable the location setting.
        switch (resultCode) {
          case Activity.RESULT_OK:
            // All required changes were successfully made
            _inviteLocationUpdateAfterActivityResult(query);
            break;
          case Activity.RESULT_CANCELED:
              // The user was asked to change settings, but chose not to
            _userRefusedToUseLocationAfterActivityResult(query);
            break;
          default:
            break;
        }
        break;
      case ACTIVITY_LOCATION_PAGE:
        _onActivityResultLocationPage(query);
        break;
    }
  }
  public void _onActivityResultLocationPage(Bundle bundle) {
    String callbackId = bundle.getString("callbackId");
    CallbackContext callbackContext = new CallbackContext(callbackId, this.webView);

    LocationManager locationManager = (LocationManager) this.activity.getSystemService(Context.LOCATION_SERVICE);
    List<String> providers = locationManager.getAllProviders();
    int availableProviders = 0;
    if (isDebug) {
      Log.d(TAG, "---debug at getMyLocation(available providers)--");
    }
    Iterator<String> iterator = providers.iterator();
    String provider;
    boolean isAvailable = false;
    while(iterator.hasNext()) {
      provider = iterator.next();
      isAvailable = locationManager.isProviderEnabled(provider);
      if (isAvailable) {
        availableProviders++;
      }
      if (isDebug) {
        Log.d(TAG, "   " + provider + " = " + (isAvailable ? "" : "not ") + "available");
      }
    }
    if (availableProviders == 0) {
      JSONObject result = new JSONObject();
      try {
        result.put("status", false);
        result.put("error_code", "not_available");
        result.put("error_message", "Since this device does not have any location provider, this app can not detect your location.");
      } catch (JSONException e) {}
      callbackContext.error(result);
      return;
    }

    _inviteLocationUpdateAfterActivityResult(bundle);
  }

  public void _inviteLocationUpdateAfterActivityResult(Bundle bundle) {
    boolean enableHighAccuracy = bundle.getBoolean("enableHighAccuracy");
    String callbackId = bundle.getString("callbackId");
    CallbackContext callbackContext = new CallbackContext(callbackId, this.webView);
    this._requestLocationUpdate(false, enableHighAccuracy, callbackContext);
  }

  public void _userRefusedToUseLocationAfterActivityResult(Bundle bundle) {
    String callbackId = bundle.getString("callbackId");
    CallbackContext callbackContext = new CallbackContext(callbackId, this.webView);
    JSONObject result = new JSONObject();
    try {
      result.put("status", false);
      result.put("error_code", "service_denied");
      result.put("error_message", "This app has been rejected to use Location Services.");
    } catch (JSONException e) {
      e.printStackTrace();
    }
    callbackContext.error(result);
  }

  @Override
  public void onPause(boolean multitasking) {
    for (Map.Entry<String, MapView> stringMapViewEntry : mapViews.entrySet()) {
      (stringMapViewEntry).getValue().onPause();
    }
    super.onPause(multitasking);
  }

  @Override
  public void onResume(boolean multitasking) {
    for (Map.Entry<String, MapView> stringMapViewEntry : mapViews.entrySet()) {
      (stringMapViewEntry).getValue().onResume();
    }
    super.onResume(multitasking);
  }

  @Override
  public void onDestroy() {
    for (Map.Entry<String, MapView> stringMapViewEntry : mapViews.entrySet()) {
      (stringMapViewEntry).getValue().onDestroy();
    }
    super.onDestroy();
  }


  @Override
  public void onClick(View view) {
    int viewId = view.getId();
    if (viewId == CLOSE_LINK_ID) {
      closeWindow();
      return;
    }
    if (viewId == LICENSE_LINK_ID) {
      showLicenseText();
    }
  }

  /**
   * Keep the positions of child dom elements of the map div.
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void pluginLayer_pushHtmlElement(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String domId = args.getString(1);
    JSONObject elemSize = args.getJSONObject(2);
    float left = contentToView(elemSize.getLong("left"));
    float top = contentToView(elemSize.getLong("top"));
    float width = contentToView(elemSize.getLong("width"));
    float height = contentToView(elemSize.getLong("height"));
    mPluginLayout.putHTMLElement(mapId, domId, left, top, (left + width), (top + height));
    this.mPluginLayout.inValidate();
    this.sendNoResult(callbackContext);
  }

  public void pluginLayer_removeHtmlElement(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String domId = args.getString(1);
    mPluginLayout.removeHTMLElement(mapId, domId);
    this.mPluginLayout.inValidate();
    this.sendNoResult(callbackContext);
  }

  /**
   * Set click-ability of the map
   * @param args Parameters given from JavaScript side
   * @param callbackContext Callback contect for sending back the result.
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  public void pluginLayer_setClickable(JSONArray args, CallbackContext callbackContext) throws JSONException {
    boolean clickable = args.getBoolean(0);
    this.mPluginLayout.setClickable(clickable);
    this.sendNoResult(callbackContext);
  }

  /**
   * Set the app background
   * @param args Parameters given from JavaScript side
   * @param callbackContext Callback contect for sending back the result.
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  public void pluginLayer_setBackGroundColor(JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (mPluginLayout != null) {
      JSONArray rgba = args.getJSONArray(0);
      int backgroundColor = Color.WHITE;
      if (rgba != null && rgba.length() == 4) {
        try {
          backgroundColor = PluginUtil.parsePluginColor(rgba);
          this.mPluginLayout.setBackgroundColor(backgroundColor);
        } catch (JSONException e) {}
      }
    }
    this.sendNoResult(callbackContext);
  }

  /**
   * Set the debug flag of myPluginLayer
   * @param args Parameters given from JavaScript side
   * @param callbackContext Callback contect for sending back the result.
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  public void pluginLayer_setDebuggable(JSONArray args, CallbackContext callbackContext) throws JSONException {
    Log.d("GoogleMaps", "pluginLayer_setDebuggable ");
    boolean debuggable = args.getBoolean(0);
    this.mPluginLayout.setDebug(debuggable);
    this.isDebug = debuggable;
    this.sendNoResult(callbackContext);
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
// Checks the orientation of the screen
    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      Toast.makeText(activity, "landscape", Toast.LENGTH_SHORT).show();
    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
      Toast.makeText(activity, "portrait", Toast.LENGTH_SHORT).show();
    }
  }

}
