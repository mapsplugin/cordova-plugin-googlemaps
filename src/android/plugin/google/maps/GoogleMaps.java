package plugin.google.maps;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginEntry;
import org.apache.cordova.PluginResult;
import org.apache.cordova.ScrollEvent;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import plugin.http.request.HttpRequest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.RenderPriority;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLoadedCallback;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CameraPosition.Builder;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.VisibleRegion;

@SuppressWarnings("deprecation")
public class GoogleMaps extends CordovaPlugin implements View.OnClickListener, OnMarkerClickListener,
      OnInfoWindowClickListener, OnMapClickListener, OnMapLongClickListener,
      OnCameraChangeListener, OnMapLoadedCallback, OnMarkerDragListener,
      OnMyLocationButtonClickListener, InfoWindowAdapter {
  private final String TAG = "GoogleMapsPlugin";
  private final HashMap<String, PluginEntry> plugins = new HashMap<String, PluginEntry>();
  private float density;
  
  private enum EVENTS {
    onScrollChanged
  }
  private enum TEXT_STYLE_ALIGNMENTS {
    left, center, right
  }
  
  private JSONObject mapDivLayoutJSON = null;
  private MapView mapView = null;
  public GoogleMap map = null;
  private Activity activity;
  private LinearLayout windowLayer = null;
  private ViewGroup root;
  private final int CLOSE_LINK_ID = 0x7f999990;  //random
  private final int LICENSE_LINK_ID = 0x7f99991; //random
  private final String PLUGIN_VERSION = "1.2.2";
  private MyPluginLayout mPluginLayout = null;
  private boolean isDebug = false;
  private GoogleApiClient googleApiClient = null;
  
  @SuppressLint("NewApi") @Override
  public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
    activity = cordova.getActivity();
    density = Resources.getSystem().getDisplayMetrics().density;
    root = (ViewGroup) webView.getParent();

    // Is this app in debug mode?
    try {
      PackageManager manager = activity.getPackageManager();
      ApplicationInfo appInfo = manager.getApplicationInfo(activity.getPackageName(), 0);
      isDebug = (appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE;
    } catch (Exception e) {}
    
    Log.i("CordovaLog", "This app uses phonegap-googlemaps-plugin version " + PLUGIN_VERSION);

    if (isDebug) {
      cordova.getThreadPool().execute(new Runnable() {
        @Override
        public void run() {
  
          try {
            
            JSONArray params = new JSONArray();
            params.put("get");
            params.put("http://plugins.cordova.io/api/plugin.google.maps");
            HttpRequest httpReq = new HttpRequest();
            httpReq.initialize(cordova, null);
            httpReq.execute("execute", params, new CallbackContext("version_check", webView) {
              @Override
              public void sendPluginResult(PluginResult pluginResult) {
                if (pluginResult.getStatus() == PluginResult.Status.OK.ordinal()) {
                  try {
                    JSONObject result = new JSONObject(pluginResult.getStrMessage());
                    JSONObject distTags = result.getJSONObject("dist-tags");
                    String latestVersion = distTags.getString("latest");
                    if (latestVersion.equals(PLUGIN_VERSION) == false) {
                      Log.i("CordovaLog", "phonegap-googlemaps-plugin version " + latestVersion + " is available.");
                    }
                  } catch (JSONException e) {}
                  
                }
              }
            });
          } catch (Exception e) {}
        }
      });
    }

    cordova.getActivity().runOnUiThread(new Runnable() {
      @SuppressLint("NewApi")
      public void run() {
        
        webView.getSettings().setRenderPriority(RenderPriority.HIGH);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        if (Build.VERSION.SDK_INT >= 11){
          webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        
        root.setBackgroundColor(Color.WHITE);
        if (VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
          activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }
        if (VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
          Log.d(TAG, "Google Maps Plugin reloads the browser to change the background color as transparent.");
          webView.setBackgroundColor(0);
          webView.reload();
        }
      }
    });
    
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

    Runnable runnable = new Runnable() {
      public void run() {
        if (("getMap".equals(action) == false && "isAvailable".equals(action) == false) &&
            GoogleMaps.this.map == null) {
          Log.w(TAG, "Can not execute '" + action + "' because the map is not created.");
          return;
        }
        if ("exec".equals(action)) {
          
          try {
            String classMethod = args.getString(0);
            String[] params = classMethod.split("\\.", 0);
            
            if ("Map.setOptions".equals(classMethod)) {
              
              JSONObject jsonParams = args.getJSONObject(1);
              if (jsonParams.has("backgroundColor")) {
                JSONArray rgba = jsonParams.getJSONArray("backgroundColor");
                int backgroundColor = Color.WHITE;
                if (rgba != null && rgba.length() == 4) {
                  try {
                    backgroundColor = PluginUtil.parsePluginColor(rgba);
                    mPluginLayout.setBackgroundColor(backgroundColor);
                  } catch (JSONException e) {}
                }
              }
              
            }
            
            // Load the class plugin
            GoogleMaps.this.loadPlugin(params[0]);
            
            PluginEntry entry = GoogleMaps.this.plugins.get(params[0]);
            if (params.length == 2 && entry != null) { 
              entry.plugin.execute("execute", args, callbackContext);
            } else {
              callbackContext.error("'" + action + "' parameter is invalid length.");
            }
          } catch (Exception e) {
            e.printStackTrace();
            callbackContext.error("Java Error\n" + e.getCause().getMessage());
          }
        } else {
          try {
            Method method = GoogleMaps.this.getClass().getDeclaredMethod(action, JSONArray.class, CallbackContext.class);
            if (method.isAccessible() == false) {
              method.setAccessible(true);
            }
            method.invoke(GoogleMaps.this, args, callbackContext);
          } catch (Exception e) {
            e.printStackTrace();
            callbackContext.error("'" + action + "' is not defined in GoogleMaps plugin.");
          }
        }
      }
    };
    cordova.getActivity().runOnUiThread(runnable);
    
    return true;
  }

  @SuppressWarnings("unused")
  private void setDiv(JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (args.length() == 0) {
      this.mapDivLayoutJSON = null;
      mPluginLayout.detachMyView();
      this.sendNoResult(callbackContext);
      return;
    }
    if (args.length() == 2) {
      mPluginLayout.attachMyView(mapView);
      this.resizeMap(args, callbackContext);
    }
  }

  /**
   * Set visibility of the map
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setVisible(JSONArray args, CallbackContext callbackContext) throws JSONException {
    boolean visible = args.getBoolean(0);
    if (this.windowLayer == null) {
      if (visible) {
        mapView.setVisibility(View.VISIBLE);
      } else {
        mapView.setVisibility(View.INVISIBLE);
      }
    }
    this.sendNoResult(callbackContext);
  }
  
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  private void getMap(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (map != null) {
      callbackContext.success();
      return;
    }
    
    mPluginLayout = new MyPluginLayout(webView, activity);
    
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
      Log.e("CordovaLog", "---Google Play Services is not available: " + GooglePlayServicesUtil.getErrorString(checkGooglePlayServices));

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

      callbackContext.error("Google Play Services is not available.");
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

    // ------------------------------
    // Initialize Google Maps SDK
    // ------------------------------
    try {
      MapsInitializer.initialize(activity);
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
      return;
    }
    GoogleMapOptions options = new GoogleMapOptions();
    final JSONObject params = args.getJSONObject(0);
    //background color
    if (params.has("backgroundColor")) {
      JSONArray rgba = params.getJSONArray("backgroundColor");
      
      int backgroundColor = Color.WHITE;
      if (rgba != null && rgba.length() == 4) {
        try {
          backgroundColor = PluginUtil.parsePluginColor(rgba);
          this.mPluginLayout.setBackgroundColor(backgroundColor);
        } catch (JSONException e) {}
      }
      
    }
    
    //controls
    if (params.has("controls")) {
      JSONObject controls = params.getJSONObject("controls");

      if (controls.has("compass")) {
        options.compassEnabled(controls.getBoolean("compass"));
      }
      if (controls.has("zoom")) {
        options.zoomControlsEnabled(controls.getBoolean("zoom"));
      }
    }
    
    //gestures
    if (params.has("gestures")) {
      JSONObject gestures = params.getJSONObject("gestures");

      if (gestures.has("tilt")) {
        options.tiltGesturesEnabled(gestures.getBoolean("tilt"));
      }
      if (gestures.has("scroll")) {
        options.scrollGesturesEnabled(gestures.getBoolean("scroll"));
      }
      if (gestures.has("rotate")) {
        options.rotateGesturesEnabled(gestures.getBoolean("rotate"));
      }
      if (gestures.has("zoom")) {
        options.zoomGesturesEnabled(gestures.getBoolean("zoom"));
      }
    }
    
    // map type
    if (params.has("mapType")) {
      String typeStr = params.getString("mapType");
      int mapTypeId = -1;
      mapTypeId = typeStr.equals("MAP_TYPE_NORMAL") ? GoogleMap.MAP_TYPE_NORMAL
          : mapTypeId;
      mapTypeId = typeStr.equals("MAP_TYPE_HYBRID") ? GoogleMap.MAP_TYPE_HYBRID
          : mapTypeId;
      mapTypeId = typeStr.equals("MAP_TYPE_SATELLITE") ? GoogleMap.MAP_TYPE_SATELLITE
          : mapTypeId;
      mapTypeId = typeStr.equals("MAP_TYPE_TERRAIN") ? GoogleMap.MAP_TYPE_TERRAIN
          : mapTypeId;
      mapTypeId = typeStr.equals("MAP_TYPE_NONE") ? GoogleMap.MAP_TYPE_NONE
          : mapTypeId;
      if (mapTypeId != -1) {
        options.mapType(mapTypeId);
      }
    }

    // initial camera position
    if (params.has("camera")) {
      JSONObject camera = params.getJSONObject("camera");
      Builder builder = CameraPosition.builder();
      if (camera.has("bearing")) {
        builder.bearing((float) camera.getDouble("bearing"));
      }
      if (camera.has("latLng")) {
        JSONObject latLng = camera.getJSONObject("latLng");
        builder.target(new LatLng(latLng.getDouble("lat"), latLng.getDouble("lng")));
      }
      if (camera.has("tilt")) {
        builder.tilt((float) camera.getDouble("tilt"));
      }
      if (camera.has("zoom")) {
        builder.zoom((float) camera.getDouble("zoom"));
      }
      options.camera(builder.build());
    }
    
    mapView = new MapView(activity, options);
    mapView.onCreate(null);
    mapView.onResume();
    mapView.getMapAsync(new OnMapReadyCallback() {
      @Override
      public void onMapReady(GoogleMap googleMap) {
        
        map = googleMap;

        try {
          //controls
          if (params.has("controls")) {
            JSONObject controls = params.getJSONObject("controls");
  
            if (controls.has("myLocationButton")) {
              Boolean isEnabled = controls.getBoolean("myLocationButton");
              map.setMyLocationEnabled(isEnabled);
              map.getUiSettings().setMyLocationButtonEnabled(isEnabled);
            }
            if (controls.has("indoorPicker")) {
              Boolean isEnabled = controls.getBoolean("indoorPicker");
              map.setIndoorEnabled(isEnabled);
            }
          }
          
          // Set event listener
          map.setOnCameraChangeListener(GoogleMaps.this);
          map.setOnInfoWindowClickListener(GoogleMaps.this);
          map.setOnMapClickListener(GoogleMaps.this);
          map.setOnMapLoadedCallback(GoogleMaps.this);
          map.setOnMapLongClickListener(GoogleMaps.this);
          map.setOnMarkerClickListener(GoogleMaps.this);
          map.setOnMarkerDragListener(GoogleMaps.this);
          map.setOnMyLocationButtonClickListener(GoogleMaps.this);
          
          // Load PluginMap class
          GoogleMaps.this.loadPlugin("Map");
          //Custom info window
          map.setInfoWindowAdapter(GoogleMaps.this);
  
          // ------------------------------
          // Embed the map if a container is specified.
          // ------------------------------
          if (args.length() == 3) {
            GoogleMaps.this.mapDivLayoutJSON = args.getJSONObject(1);
            mPluginLayout.attachMyView(mapView);
            GoogleMaps.this.resizeMap(args, callbackContext);
          }
          callbackContext.success();
        } catch (Exception e) {
          callbackContext.error(e.getMessage());
        }
      }
    });
    
  }
  
  private float contentToView(long d) {
    return d * this.density;
  }
  
  //-----------------------------------
  // Create the instance of class
  //-----------------------------------
  @SuppressWarnings("rawtypes")
  private void loadPlugin(String serviceName) {
    if (plugins.containsKey(serviceName)) {
      return;
    }
    try {
      Class pluginCls = Class.forName("plugin.google.maps.Plugin" + serviceName);
      
      CordovaPlugin plugin = (CordovaPlugin) pluginCls.newInstance();
      PluginEntry pluginEntry = new PluginEntry("GoogleMaps", plugin);
      this.plugins.put(serviceName, pluginEntry);
      
      try {
        Class cordovaPref = Class.forName("org.apache.cordova.CordovaPreferences");
        if (cordovaPref != null) {
          Method privateInit = CordovaPlugin.class.getMethod("privateInitialize", CordovaInterface.class, CordovaWebView.class, cordovaPref);
          if (privateInit != null) {
            privateInit.invoke(plugin, this.cordova, webView, null);
          }
        }
      } catch (Exception e2) {}
      
      plugin.initialize(this.cordova, webView);
      ((MyPluginInterface)plugin).setMapCtrl(this);
      if (map == null) {
        Log.e(TAG, "map is null!");
      }
      
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  @SuppressWarnings("unused")
  private Boolean getLicenseInfo(JSONArray args, CallbackContext callbackContext) {
    String msg = GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(activity);
    callbackContext.success(msg);
    return true;
  }

  @Override
  public Object onMessage(String id, Object data) {
    EVENTS event = null;
    try {
      event = EVENTS.valueOf(id);
    }catch(Exception e) {}
    
    if (event == null) {
      return null;
    }
    
    switch(event) {
    case onScrollChanged:
      ScrollEvent scrollEvent = (ScrollEvent)data;
      if (mPluginLayout != null) {
        mPluginLayout.scrollTo(scrollEvent.nl, scrollEvent.nt);
        if (mapDivLayoutJSON != null) {
          try {
            float divW = contentToView(mapDivLayoutJSON.getLong("width"));
            float divH = contentToView(mapDivLayoutJSON.getLong("height"));
            float divLeft = contentToView(mapDivLayoutJSON.getLong("left"));
            float divTop = contentToView(mapDivLayoutJSON.getLong("top"));
    
            mPluginLayout.setDrawingRect(
                divLeft,
                divTop - scrollEvent.nt, 
                divLeft + divW, 
                divTop + divH - scrollEvent.nt);
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }
      }
      
      break;
    }
    
    return null;
  }

  private void closeWindow() {
    webView.hideCustomView();
  }
  @SuppressWarnings("unused")
  private void showDialog(final JSONArray args, final CallbackContext callbackContext) {
    if (windowLayer != null) {
      return;
    }
    
    // window layout
    windowLayer = new LinearLayout(activity);
    windowLayer.setPadding(0, 0, 0, 0);
    LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
    windowLayer.setLayoutParams(layoutParams);
    
    
    // dialog window layer
    FrameLayout dialogLayer = new FrameLayout(activity);
    dialogLayer.setLayoutParams(layoutParams);
    //dialogLayer.setPadding(15, 15, 15, 0);
    dialogLayer.setBackgroundColor(Color.LTGRAY);
    windowLayer.addView(dialogLayer);
    
    // map frame
    final FrameLayout mapFrame = new FrameLayout(activity);
    mapFrame.setPadding(0, 0, 0, (int)(40 * density));
    dialogLayer.addView(mapFrame);
    
    if (this.mPluginLayout != null && 
        this.mPluginLayout.getMyView() != null) {
      this.mPluginLayout.detachMyView();
    }
    
    ViewGroup.LayoutParams lParams = (ViewGroup.LayoutParams) mapView.getLayoutParams();
    if (lParams == null) {
      lParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }
    lParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
    lParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
    if (lParams instanceof AbsoluteLayout.LayoutParams) {
      AbsoluteLayout.LayoutParams params = (AbsoluteLayout.LayoutParams) lParams;
      params.x = 0;
      params.y = 0;
      mapView.setLayoutParams(params);
    } else if (lParams instanceof LinearLayout.LayoutParams) {
      LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lParams;
      params.topMargin = 0;
      params.leftMargin = 0;
      mapView.setLayoutParams(params);
    } else if (lParams instanceof FrameLayout.LayoutParams) {
      FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lParams;
      params.topMargin = 0;
      params.leftMargin = 0;
      mapView.setLayoutParams(params);
    } 
    mapFrame.addView(this.mapView);
    
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
    
    webView.setVisibility(View.GONE);
    root.addView(windowLayer);
    
    //Dummy view for the back-button event
    FrameLayout dummyLayout = new FrameLayout(activity);
    this.webView.showCustomView(dummyLayout, new WebChromeClient.CustomViewCallback() {

      @Override
      public void onCustomViewHidden() {
        mapFrame.removeView(mapView);
        if (mPluginLayout != null &&
            mapDivLayoutJSON != null) {
          mPluginLayout.attachMyView(mapView);
          mPluginLayout.updateViewPosition();
        }
        root.removeView(windowLayer);
        webView.setVisibility(View.VISIBLE);
        windowLayer = null;
        
        
        GoogleMaps.this.onMapEvent("map_close");
      }
    });

    this.sendNoResult(callbackContext);
  }

  private void resizeMap(JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (mPluginLayout == null) {
      callbackContext.success();
      return;
    }
    mapDivLayoutJSON = args.getJSONObject(args.length() - 2);
    JSONArray HTMLs = args.getJSONArray(args.length() - 1);
    JSONObject elemInfo, elemSize;
    String elemId;
    float divW, divH, divLeft, divTop;
    if (mPluginLayout == null) {
      this.sendNoResult(callbackContext);
      return;
    }
    this.mPluginLayout.clearHTMLElement();
    
    for (int i = 0; i < HTMLs.length(); i++) {
      elemInfo = HTMLs.getJSONObject(i);
      try {
        elemId = elemInfo.getString("id");
        elemSize = elemInfo.getJSONObject("size");
        
        divW = contentToView(elemSize.getLong("width"));
        divH = contentToView(elemSize.getLong("height"));
        divLeft = contentToView(elemSize.getLong("left"));
        divTop = contentToView(elemSize.getLong("top"));
        mPluginLayout.putHTMLElement(elemId, divLeft, divTop, divLeft + divW, divTop + divH);
      } catch (Exception e){
        e.printStackTrace();
      }
    }
    //mPluginLayout.inValidate();
    updateMapViewLayout();
    this.sendNoResult(callbackContext);
  }

  
  private void updateMapViewLayout() {
    if (mPluginLayout == null) {
      return;
    }
    try {
      float divW = contentToView(mapDivLayoutJSON.getLong("width"));
      float divH = contentToView(mapDivLayoutJSON.getLong("height"));
      float divLeft = contentToView(mapDivLayoutJSON.getLong("left"));
      float divTop = contentToView(mapDivLayoutJSON.getLong("top"));

      // Update the plugin drawing view rect
      mPluginLayout.setDrawingRect(
          divLeft,
          divTop - webView.getScrollY(), 
          divLeft + divW,
          divTop + divH - webView.getScrollY());
      mPluginLayout.updateViewPosition();
      mapView.requestLayout();
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }
  
  @SuppressWarnings("unused")
  private void closeDialog(final JSONArray args, final CallbackContext callbackContext) {
    this.closeWindow();
    this.sendNoResult(callbackContext);
  }

  @SuppressWarnings("unused")
  private void isAvailable(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    
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

  @SuppressWarnings("unused")
  private void getMyLocation(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    
    LocationManager locationManager = (LocationManager) this.activity.getSystemService(Context.LOCATION_SERVICE);
    List<String> providers = locationManager.getAllProviders();
    ArrayList<String> availableProviders = new ArrayList<String>();
    if (providers.size() == 0) {
      JSONObject result = new JSONObject();
      result.put("status", false);
      result.put("error_code", "not_available");
      result.put("error_message", "Since this device does not have any location provider, this app can not detect your location.");
      callbackContext.error(result);
      return;
    } else {
      if (isDebug) {
        Log.d("CordovaLog", "---debug at getMyLocation(available providers)--");
      }
      Iterator<String> iterator = providers.iterator();
      String provider;
      boolean isAvailable = false;
      while(iterator.hasNext()) {
        provider = iterator.next();
        isAvailable = locationManager.isProviderEnabled(provider);
        if (isAvailable) {
          availableProviders.add(provider);
        }
        if (isDebug) {
          Log.d("CordovaLog", "   " + provider + " = " + (isAvailable ? "" : "not ") + "available");
        }
      }
    }
    
    // enableHighAccuracy = true -> PRIORITY_HIGH_ACCURACY
    // enableHighAccuracy = false -> PRIORITY_BALANCED_POWER_ACCURACY
    
    JSONObject params = args.getJSONObject(0);
    boolean isHigh = false;
    if (params.has("enableHighAccuracy")) {
      isHigh = params.getBoolean("enableHighAccuracy");
    }
    final boolean enableHighAccuracy = isHigh;
    
    String useProvider = null;
    if (enableHighAccuracy == true) {
      // GPS is not available
      if (availableProviders.indexOf(LocationManager.GPS_PROVIDER) == -1) {
        useProvider = null;
      } else {
        useProvider = LocationManager.GPS_PROVIDER;
      }
    } else {
      if (availableProviders.indexOf(LocationManager.GPS_PROVIDER) > -1) {
        useProvider = LocationManager.GPS_PROVIDER;
      } else if (availableProviders.indexOf(LocationManager.NETWORK_PROVIDER) > -1) {
        useProvider = LocationManager.NETWORK_PROVIDER;
      } else if (availableProviders.indexOf(LocationManager.PASSIVE_PROVIDER) > -1) {
        useProvider = LocationManager.PASSIVE_PROVIDER;
        Toast.makeText(activity, "Recommend: Enabling location is better result.", Toast.LENGTH_LONG).show();
      }
    }
    if (useProvider == null) {
      //Ask the user to turn on the location services.
      AlertDialog.Builder builder = new AlertDialog.Builder(this.activity);
      builder.setTitle("Improve location accuracy");
      builder.setMessage("To enhance your Maps experience:\n\n" +
          " - Enable Google apps location access\n\n" +
          " - Turn on GPS and mobile network location");
      builder.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
              //Launch settings, allowing user to make a change
              Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
              activity.startActivity(intent);

              JSONObject result = new JSONObject();
              try {
                result.put("status", false);
                result.put("error_code", "open_settings");
                result.put("error_message", "User opened the settings of location service. So try again.");
              } catch (JSONException e) {}
              callbackContext.error(result);
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
                result.put("error_message", "This app has rejected to use Location Services.");
              } catch (JSONException e) {}
              callbackContext.error(result);
          }
      });
      builder.create().show();
      return;
    }
    
    
    if (googleApiClient == null) {
      googleApiClient = new GoogleApiClient.Builder(this.activity)
        .addApi(LocationServices.API)
        .addConnectionCallbacks(new com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks() {
  
          @Override
          public void onConnected(Bundle connectionHint) {
            Log.e("Marker", "===> onConnected");
            PluginResult tmpResult = new PluginResult(PluginResult.Status.NO_RESULT);
            tmpResult.setKeepCallback(true);
            callbackContext.sendPluginResult(tmpResult);
            
            _requestLocationUpdate(enableHighAccuracy, callbackContext);
          }
  
          @Override
          public void onConnectionSuspended(int cause) {
            Log.e("Marker", "===> onConnectionSuspended");
           }
          
        })
        .addOnConnectionFailedListener(new com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener() {
  
          @Override
          public void onConnectionFailed(ConnectionResult result) {
            Log.e("Marker", "===> onConnectionFailed");
            
            PluginResult tmpResult = new PluginResult(PluginResult.Status.ERROR, result.toString());
            tmpResult.setKeepCallback(false);
            callbackContext.sendPluginResult(tmpResult);
            
            googleApiClient.disconnect();
          }
          
        })
        .build();
      googleApiClient.connect();
    } else if (googleApiClient.isConnected()) {
      _requestLocationUpdate(enableHighAccuracy, callbackContext);
    }
    
  }
  
  private void _requestLocationUpdate(boolean enableHighAccuracy, final CallbackContext callbackContext) {

    int priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
    if (enableHighAccuracy) {
      priority = LocationRequest.PRIORITY_HIGH_ACCURACY;
    }
    LocationRequest locationRequest = LocationRequest.create()
        .setExpirationTime(5000)
        .setNumUpdates(1)
        .setSmallestDisplacement(0)
        .setPriority(priority).setInterval(5000);
    
    final PendingResult<Status> result =  LocationServices.FusedLocationApi.requestLocationUpdates(
        googleApiClient, locationRequest, new LocationListener() {

          @Override
          public void onLocationChanged(Location location) {
            Log.e("CordovaLog", "===> onLocationChanged");
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
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            return;
          } else {
            Log.e("CordovaLog", "====> waiting onLocationChanged");
            Toast.makeText(activity, "Waiting for location...", Toast.LENGTH_SHORT).show();
          }
        }
      }
    });
  }
  
  private void showLicenseText() {
    AsyncLicenseInfo showLicense = new AsyncLicenseInfo(activity);
    showLicense.execute();
  }

  /********************************************************
   * Callbacks
   ********************************************************/

  /**
   * Notify marker event to JS
   * @param eventName
   * @param marker
   */
  private void onMarkerEvent(String eventName, Marker marker) {
    String markerId = "marker_" + marker.getId();
    webView.loadUrl("javascript:plugin.google.maps.Map." +
                  "_onMarkerEvent('" + eventName + "','" + markerId + "')");
  }
  private void onOverlayEvent(String eventName, String overlayId, LatLng point) {
    webView.loadUrl("javascript:plugin.google.maps.Map." +
        "_onOverlayEvent(" +
          "'" + eventName + "','" + overlayId + "', " +
          "new window.plugin.google.maps.LatLng(" + point.latitude + "," + point.longitude + ")" +
        ")");
  }
  private void onPolylineClick(Polyline polyline, LatLng point) {
    String overlayId = "polyline_" + polyline.getId();
    this.onOverlayEvent("overlay_click", overlayId, point);
  }
  private void onPolygonClick(Polygon polygon, LatLng point) {
    String overlayId = "polygon_" + polygon.getId();
    this.onOverlayEvent("overlay_click", overlayId, point);
  }
  private void onCircleClick(Circle circle, LatLng point) {
    String overlayId = "circle_" + circle.getId();
    this.onOverlayEvent("overlay_click", overlayId, point);
  }
  private void onGroundOverlayClick(GroundOverlay groundOverlay, LatLng point) {
    String overlayId = "groundOverlay_" + groundOverlay.getId();
    this.onOverlayEvent("overlay_click", overlayId, point);
  }

  @Override
  public boolean onMarkerClick(Marker marker) {
    this.onMarkerEvent("click", marker);
    
    JSONObject properties = null;
    String propertyId = "marker_property_" + marker.getId();
    PluginEntry pluginEntry = this.plugins.get("Marker");
    PluginMarker pluginMarker = (PluginMarker)pluginEntry.plugin;
    if (pluginMarker.objects.containsKey(propertyId)) {
      properties = (JSONObject) pluginMarker.objects.get(propertyId);
      if (properties.has("disableAutoPan")) {
        boolean disableAutoPan = false;
        try {
          disableAutoPan = properties.getBoolean("disableAutoPan");
        } catch (JSONException e) {}
        if (disableAutoPan) {
          marker.showInfoWindow();
          return true;
        }
      }
    }
    
    return false;
  }
  
  @Override
  public void onInfoWindowClick(Marker marker) {
    this.onMarkerEvent("info_click", marker);
  }

  @Override
  public void onMarkerDrag(Marker marker) {
    this.onMarkerEvent("drag", marker);
  }

  @Override
  public void onMarkerDragEnd(Marker marker) {
    this.onMarkerEvent("drag_end", marker);
  }

  @Override
  public void onMarkerDragStart(Marker marker) {
    this.onMarkerEvent("drag_start", marker);
  }


  /**
   * Notify map event to JS
   * @param eventName
   * @param point
   */
  private void onMapEvent(final String eventName) {
    webView.loadUrl("javascript:plugin.google.maps.Map._onMapEvent('" + eventName + "')");
  }
  
  /**
   * Notify map event to JS
   * @param eventName
   * @param point
   */
  private void onMapEvent(final String eventName, final LatLng point) {
    webView.loadUrl("javascript:plugin.google.maps.Map._onMapEvent(" +
            "'" + eventName + "', new window.plugin.google.maps.LatLng(" + point.latitude + "," + point.longitude + "))");
  }

  @Override
  public void onMapLongClick(LatLng point) {
    this.onMapEvent("long_click", point);
  }

  private double calculateDistance(LatLng pt1, LatLng pt2){
    float[] results = new float[1];
    Location.distanceBetween(pt1.latitude, pt1.longitude,
                            pt2.latitude, pt2.longitude, results);
    return results[0];
  }
  /**
   * Notify map click event to JS, also checks for click on a polygon and triggers onPolygonEvent
   * @param point
   */
  public void onMapClick(LatLng point) {
    boolean hitPoly = false;
    String key;
    LatLngBounds bounds;
    
    // Polyline
    PluginEntry polylinePlugin = this.plugins.get("Polyline");
    if(polylinePlugin != null) {
      PluginPolyline polylineClass = (PluginPolyline) polylinePlugin.plugin;

      List<LatLng> points ;
      Polyline polyline;
      Point origin = new Point();
      Point hitArea = new Point();
      hitArea.x = 1;
      hitArea.y = 1;
      Projection projection = map.getProjection();
      double threshold = this.calculateDistance(
          projection.fromScreenLocation(origin),
          projection.fromScreenLocation(hitArea));

      for (HashMap.Entry<String, Object> entry : polylineClass.objects.entrySet()) {
        key = entry.getKey();
        if (key.contains("polyline_bounds_")) {
          bounds = (LatLngBounds) entry.getValue();
          if (bounds.contains(point)) {
            key = key.replace("bounds_", "");
            
            polyline = polylineClass.getPolyline(key);
            points = polyline.getPoints();
            
            if (polyline.isGeodesic()) {
              if (this.isPointOnTheGeodesicLine(points, point, threshold)) {
                hitPoly = true;
                this.onPolylineClick(polyline, point);
              }
            } else {
                if (this.isPointOnTheLine(points, point)) {
                  hitPoly = true;
                  this.onPolylineClick(polyline, point);
                }
            }
          }
        }
      }
      if (hitPoly) {
        return;
      }
    }
    
    // Loop through all polygons to check if within the touch point
    PluginEntry polygonPlugin = this.plugins.get("Polygon");
    if (polygonPlugin != null) {
      PluginPolygon polygonClass = (PluginPolygon) polygonPlugin.plugin;
    
      for (HashMap.Entry<String, Object> entry : polygonClass.objects.entrySet()) {
        key = entry.getKey();
        if (key.contains("polygon_bounds_")) {
          bounds = (LatLngBounds) entry.getValue();
          if (bounds.contains(point)) {
            
            key = key.replace("_bounds", "");
            Polygon polygon = polygonClass.getPolygon(key);
            
            if (this.isPolygonContains(polygon.getPoints(), point)) {
              hitPoly = true;
              this.onPolygonClick(polygon, point);
            }
          }
        }
      }
      if (hitPoly) {
        return;
      }
    }
    
    // Loop through all circles to check if within the touch point
    PluginEntry circlePlugin = this.plugins.get("Circle");
    if (circlePlugin != null) {
      PluginCircle circleClass = (PluginCircle) circlePlugin.plugin;
    
      for (HashMap.Entry<String, Object> entry : circleClass.objects.entrySet()) {
        Circle circle = (Circle) entry.getValue();
        if (this.isCircleContains(circle, point)) {
          hitPoly = true;
          this.onCircleClick(circle, point);
        }
      }
      if (hitPoly) {
        return;
      }
    }
    
    // Loop through ground overlays to check if within the touch point
    PluginEntry groundOverlayPlugin = this.plugins.get("GroundOverlay");
    if (groundOverlayPlugin != null) {
      PluginGroundOverlay groundOverlayClass = (PluginGroundOverlay) groundOverlayPlugin.plugin;
    
      for (HashMap.Entry<String, Object> entry : groundOverlayClass.objects.entrySet()) {
        GroundOverlay groundOverlay = (GroundOverlay) entry.getValue();
        if (this.isGroundOverlayContains(groundOverlay, point)) {
          hitPoly = true;
          this.onGroundOverlayClick(groundOverlay, point);
        }
      }
      if (hitPoly) {
        return;
      }
    }
    
    // Only emit click event if no overlays hit
    this.onMapEvent("click", point);
  }
  
  /**
   * Intersection for geodesic line
   * @ref http://my-clip-devdiary.blogspot.com/2014/01/html5canvas.html
   * 
   * @param points
   * @param point
   * @param threshold
   * @return
   */
  private boolean isPointOnTheGeodesicLine(List<LatLng> points, LatLng point, double threshold) {
    double trueDistance, testDistance1, testDistance2;
    Point p0, p1, touchPoint;
    touchPoint = new Point();
    touchPoint.x = (int) (point.latitude * 100000);
    touchPoint.y = (int) (point.longitude * 100000);
    
    for (int i = 0; i < points.size() - 1; i++) {
      p0 = new Point();
      p0.x = (int) (points.get(i).latitude * 100000);
      p0.y = (int) (points.get(i).longitude * 100000);
      p1 = new Point();
      p1.x = (int) (points.get(i + 1).latitude * 100000);
      p1.y = (int) (points.get(i + 1).longitude * 100000);
      trueDistance = this.calculateDistance(points.get(i), points.get(i + 1));
      testDistance1 = this.calculateDistance(points.get(i), point);
      testDistance2 = this.calculateDistance(point, points.get(i + 1));
      // the distance is exactly same if the point is on the straight line
      if (Math.abs(trueDistance - (testDistance1 + testDistance2)) < threshold) {
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * Intersection for non-geodesic line
   * @ref http://movingahead.seesaa.net/article/299962216.html
   * @ref http://www.softsurfer.com/Archive/algorithm_0104/algorithm_0104B.htm#Line-Plane
   * 
   * @param points
   * @param point
   * @return
   */
  private boolean isPointOnTheLine(List<LatLng> points, LatLng point) {
    double Sx, Sy;
    Projection projection = map.getProjection();
    Point p0, p1, touchPoint;
    touchPoint = projection.toScreenLocation(point);

    p0 = projection.toScreenLocation(points.get(0));
    for (int i = 1; i < points.size(); i++) {
      p1 = projection.toScreenLocation(points.get(i));
      Sx = ((double)touchPoint.x - (double)p0.x) / ((double)p1.x - (double)p0.x);
      Sy = ((double)touchPoint.y - (double)p0.y) / ((double)p1.y - (double)p0.y);
      if (Math.abs(Sx - Sy) < 0.05 && Sx < 1 && Sx > 0) {
        return true;
      }
      p0 = p1;
    }
    return false;
  }

  /**
   * Intersects using thw Winding Number Algorithm
   * @ref http://www.nttpc.co.jp/company/r_and_d/technology/number_algorithm.html
   * @param path
   * @param point
   * @return
   */
  private boolean isPolygonContains(List<LatLng> path, LatLng point) {
    int wn = 0;
    Projection projection = map.getProjection();
    VisibleRegion visibleRegion = projection.getVisibleRegion();
    LatLngBounds bounds = visibleRegion.latLngBounds;
    Point sw = projection.toScreenLocation(bounds.southwest);
    
    Point touchPoint = projection.toScreenLocation(point);
    touchPoint.y = sw.y - touchPoint.y;
    double vt;
    
    for (int i = 0; i < path.size() - 1; i++) {
      Point a = projection.toScreenLocation(path.get(i));
      a.y = sw.y - a.y;
      Point b = projection.toScreenLocation(path.get(i + 1));
      b.y = sw.y - b.y;
      
      if ((a.y <= touchPoint.y) && (b.y > touchPoint.y)) {
        vt = ((double)touchPoint.y - (double)a.y) / ((double)b.y - (double)a.y);
        if (touchPoint.x < ((double)a.x + (vt * ((double)b.x - (double)a.x)))) {
          wn++;
        }
      } else if ((a.y > touchPoint.y) && (b.y <= touchPoint.y)) {
        vt = ((double)touchPoint.y - (double)a.y) / ((double)b.y - (double)a.y);
        if (touchPoint.x < ((double)a.x + (vt * ((double)b.x - (double)a.x)))) {
          wn--;
        }
      }
    }
    
    return (wn != 0);
  }
  
  /**
   * Check if a circle contains a point
   * @param circle
   * @param point
   */
  private boolean isCircleContains(Circle circle, LatLng point) {
    double r = circle.getRadius();
    LatLng center = circle.getCenter();
    double cX = center.latitude;
    double cY = center.longitude;
    double pX = point.latitude;
    double pY = point.longitude;
    
    float[] results = new float[1];
    
    Location.distanceBetween(cX, cY, pX, pY, results);
    
    if(results[0] < r) {
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * Check if a ground overlay contains a point
   * @param groundOverlay
   * @param point
   */
  private boolean isGroundOverlayContains(GroundOverlay groundOverlay, LatLng point) {
    LatLngBounds groundOverlayBounds = groundOverlay.getBounds();
    
    return groundOverlayBounds.contains(point);
  }
  
  @Override
  public boolean onMyLocationButtonClick() {
    webView.loadUrl("javascript:plugin.google.maps.Map._onMapEvent('my_location_button_click')");
    return false;
  }


  @Override
  public void onMapLoaded() {
    webView.loadUrl("javascript:plugin.google.maps.Map._onMapEvent('map_loaded')");
  }

  /**
   * Notify the myLocationChange event to JS
   */
  @Override
  public void onCameraChange(CameraPosition position) {
    JSONObject params = new JSONObject();
    String jsonStr = "";
    try {
      JSONObject target = new JSONObject();
      target.put("lat", position.target.latitude);
      target.put("lng", position.target.longitude);
      params.put("target", target);
      params.put("hashCode", position.hashCode());
      params.put("bearing", position.bearing);
      params.put("tilt", position.tilt);
      params.put("zoom", position.zoom);
      jsonStr = params.toString();
    } catch (JSONException e) {
      e.printStackTrace();
    }
    webView.loadUrl("javascript:plugin.google.maps.Map._onCameraEvent('camera_change', " + jsonStr + ")");
  }

  @Override
  public void onPause(boolean multitasking) {
    if (mapView != null) {
      mapView.onPause();
    }
    super.onPause(multitasking);
  }

  @Override
  public void onResume(boolean multitasking) {
    if (mapView != null) {
      mapView.onResume();
    }
    super.onResume(multitasking);
  }

  @Override
  public void onDestroy() {
    if (mapView != null) {
      mapView.onDestroy();
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
      return;
    }
  }
  
  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  @Override
  public View getInfoContents(Marker marker) {
    String title = marker.getTitle();
    String snippet = marker.getSnippet();
    if ((title == null) && (snippet == null)) {
      return null;
    }
    
    JSONObject properties = null;
    JSONObject styles = null;
    String propertyId = "marker_property_" + marker.getId();
    PluginEntry pluginEntry = this.plugins.get("Marker");
    PluginMarker pluginMarker = (PluginMarker)pluginEntry.plugin;
    if (pluginMarker.objects.containsKey(propertyId)) {
      properties = (JSONObject) pluginMarker.objects.get(propertyId);

      if (properties.has("styles")) {
        try {
          styles = (JSONObject) properties.getJSONObject("styles");
        } catch (JSONException e) {}
      }
    }
    

    // Linear layout
    LinearLayout windowLayer = new LinearLayout(activity);
    windowLayer.setPadding(3, 3, 3, 3);
    windowLayer.setOrientation(LinearLayout.VERTICAL);
    LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    layoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER;
    windowLayer.setLayoutParams(layoutParams);

    //----------------------------------------
    // text-align = left | center | right
    //----------------------------------------
    int gravity = Gravity.LEFT;
    int textAlignment = View.TEXT_ALIGNMENT_GRAVITY;
    
    if (styles != null) {
      try {
        String textAlignValue = styles.getString("text-align");
        
        switch(TEXT_STYLE_ALIGNMENTS.valueOf(textAlignValue)) {
        case left:
          gravity = Gravity.LEFT;
          textAlignment = View.TEXT_ALIGNMENT_GRAVITY;
          break;
        case center:
          gravity = Gravity.CENTER;
          textAlignment = View.TEXT_ALIGNMENT_CENTER;
          break;
        case right:
          gravity = Gravity.RIGHT;
          textAlignment = View.TEXT_ALIGNMENT_VIEW_END;
          break;
        }
        
      } catch (Exception e) {}
    }
    
    if (title != null) {
      if (title.indexOf("data:image/") > -1 && title.indexOf(";base64,") > -1) {
        String[] tmp = title.split(",");
        Bitmap image = PluginUtil.getBitmapFromBase64encodedImage(tmp[1]);
        image = PluginUtil.scaleBitmapForDevice(image);
        ImageView imageView = new ImageView(this.cordova.getActivity());
        imageView.setImageBitmap(image);
        windowLayer.addView(imageView);
      } else {
        TextView textView = new TextView(this.cordova.getActivity());
        textView.setText(title);
        textView.setSingleLine(false);
        
        int titleColor = Color.BLACK;
        if (styles != null && styles.has("color")) {
          try {
            titleColor = PluginUtil.parsePluginColor(styles.getJSONArray("color"));
          } catch (JSONException e) {}
        }
        textView.setTextColor(titleColor);
        textView.setGravity(gravity);
        if (VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
          textView.setTextAlignment(textAlignment);
        }
        
        //----------------------------------------
        // font-style = normal | italic
        // font-weight = normal | bold
        //----------------------------------------
        int fontStyle = Typeface.NORMAL;
        if (styles != null) {
          try {
            if ("italic".equals(styles.getString("font-style"))) {
              fontStyle = Typeface.ITALIC;
            }
          } catch (JSONException e) {}
          try {
            if ("bold".equals(styles.getString("font-weight"))) {
              fontStyle = fontStyle | Typeface.BOLD;
            }
          } catch (JSONException e) {}
        }
        textView.setTypeface(Typeface.DEFAULT, fontStyle);
        
        windowLayer.addView(textView);
      }
    }
    if (snippet != null) {
      //snippet = snippet.replaceAll("\n", "");
      TextView textView2 = new TextView(this.cordova.getActivity());
      textView2.setText(snippet);
      textView2.setTextColor(Color.GRAY);
      textView2.setTextSize((textView2.getTextSize() / 6 * 5) / density);
      textView2.setGravity(gravity);
      if (VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        textView2.setTextAlignment(textAlignment);
      }

      windowLayer.addView(textView2);
    }
    return windowLayer;
  }

  @Override
  public View getInfoWindow(Marker marker) {
    return null;
  }

  /**
   * Clear all markups
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void clear(JSONArray args, CallbackContext callbackContext) throws JSONException {
    Set<String> pluginNames = plugins.keySet();
    Iterator<String> iterator = pluginNames.iterator();
    String pluginName;
    PluginEntry pluginEntry;
    while(iterator.hasNext()) {
      pluginName = iterator.next();
      if ("Map".equals(pluginName) == false) {
        pluginEntry = plugins.get(pluginName);
        ((MyPlugin) pluginEntry.plugin).clear();
      }
    }
    
    this.map.clear();
    this.sendNoResult(callbackContext);
  }

  @SuppressWarnings("unused")
  private void pluginLayer_pushHtmlElement(JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (mPluginLayout != null) {
      String domId = args.getString(0);
      JSONObject elemSize = args.getJSONObject(1);
      float left = contentToView(elemSize.getLong("left"));
      float top = contentToView(elemSize.getLong("top"));
      float width = contentToView(elemSize.getLong("width"));
      float height = contentToView(elemSize.getLong("height"));
      mPluginLayout.putHTMLElement(domId, left, top, (left + width), (top + height));
      this.mPluginLayout.inValidate();
    }
    this.sendNoResult(callbackContext);
  }
  @SuppressWarnings("unused")
  private void pluginLayer_removeHtmlElement(JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (mPluginLayout != null) {
      String domId = args.getString(0);
      mPluginLayout.removeHTMLElement(domId);
      this.mPluginLayout.inValidate();
    }
    this.sendNoResult(callbackContext);
  }

  /**
   * Set click-ability of the map
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void pluginLayer_setClickable(JSONArray args, CallbackContext callbackContext) throws JSONException {
    boolean clickable = args.getBoolean(0);
    if (mapView != null && this.windowLayer == null) {
      this.mPluginLayout.setClickable(clickable);
    }
    this.sendNoResult(callbackContext);
  }
  
  /**
   * Set the app background
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void pluginLayer_setBackGroundColor(JSONArray args, CallbackContext callbackContext) throws JSONException {
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
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void pluginLayer_setDebuggable(JSONArray args, CallbackContext callbackContext) throws JSONException {
    boolean debuggable = args.getBoolean(0);
    if (mapView != null && this.windowLayer == null) {
      this.mPluginLayout.setDebug(debuggable);
    }
    this.sendNoResult(callbackContext);
  }
  
  /**
   * Destroy the map completely
   * @param args
   * @param callbackContext
   */
  @SuppressWarnings("unused")
  private void remove(JSONArray args, CallbackContext callbackContext) {
    if (mPluginLayout != null) {
      this.mPluginLayout.setClickable(false);
      this.mPluginLayout.detachMyView();
      this.mPluginLayout = null;
    }
    plugins.clear();
    if (map != null) {
      map.setMyLocationEnabled(false);
      map.clear();
    }
    if (mapView != null) {
      mapView.onDestroy();
    }
    map = null;
    mapView = null;
    windowLayer = null;
    mapDivLayoutJSON = null;
    System.gc();
    this.sendNoResult(callbackContext);
  }

  protected void sendNoResult(CallbackContext callbackContext) {
    PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
    callbackContext.sendPluginResult(pluginResult);
  }

}
