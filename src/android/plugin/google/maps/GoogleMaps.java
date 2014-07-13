package plugin.google.maps;

import java.lang.reflect.Constructor;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
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
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CameraPosition.Builder;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.VisibleRegion;

@SuppressWarnings("deprecation")
public class GoogleMaps extends CordovaPlugin implements View.OnClickListener, OnMarkerClickListener,
      OnInfoWindowClickListener, OnMapClickListener, OnMapLongClickListener,
      OnCameraChangeListener, OnMapLoadedCallback, OnMarkerDragListener,
      OnMyLocationButtonClickListener, 
      ConnectionCallbacks, OnConnectionFailedListener, InfoWindowAdapter {
  private final String TAG = "GoogleMapsPlugin";
  private final HashMap<String, PluginEntry> plugins = new HashMap<String, PluginEntry>();
  private float density;
  
  private enum METHODS {
    setVisible,
    setDiv,
    resizeMap,
    getMap,
    showDialog,
    closeDialog,
    getMyLocation,
    exec,
    isAvailable,
    getLicenseInfo,
    clear
  }
  
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
  public LocationClient locationClient = null;

  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
    activity = cordova.getActivity();
    density = Resources.getSystem().getDisplayMetrics().density;
  }

  @Override
  public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    /*
    if (args != null && args.length() > 0) {
      Log.d(TAG, "action=" + action + " args[0]=" + args.getString(0));
    } else {
      Log.d(TAG, "action=" + action);
    }
    */

    Runnable runnable = new Runnable() {
      public void run() {
        if (("getMap".equals(action) == false && "isAvailable".equals(action) == false) &&
            GoogleMaps.this.map == null) {
          Log.e(TAG, "Can not execute '" + action + "' because the map is not created.");
          return;
        }
        try {
          switch(METHODS.valueOf(action)) {
          case getLicenseInfo:
            GoogleMaps.this.getLicenseInfo(args, callbackContext);
            break;
          case setVisible:
            GoogleMaps.this.setVisible(args, callbackContext);
            break;
          case setDiv:
            if (mapView.getParent() != null) {
              GoogleMaps.this.mapDivLayoutJSON = null;
              webView.removeView(mapView);
            }
            if (args.length() == 1) {
              webView.addView(mapView);
              GoogleMaps.this.resizeMap(args, callbackContext);
            }
            break;
          case resizeMap:
            GoogleMaps.this.resizeMap(args, callbackContext);
            break;
          case getMap:
            GoogleMaps.this.getMap(args, callbackContext);
            break;
          case showDialog:
            GoogleMaps.this.showDialog(args, callbackContext);
            break;
          case closeDialog:
            GoogleMaps.this.closeDialog(args, callbackContext);
            break;
          case getMyLocation:
            GoogleMaps.this.getMyLocation(args, callbackContext);
            break;
          case isAvailable:
            GoogleMaps.this.isAvailable(args, callbackContext);
            break;
          case clear:
            GoogleMaps.this.clear(args, callbackContext);
            break;
          case exec:
          
            String classMethod = args.getString(0);
            String[] params = classMethod.split("\\.", 0);
            
            // Load the class plugin
            GoogleMaps.this.loadPlugin(params[0]);
            
            PluginEntry entry = GoogleMaps.this.plugins.get(params[0]);
            if (params.length == 2 && entry != null) { 
              entry.plugin.execute("execute", args, callbackContext);
            } else {
              callbackContext.error("'" + action + "' parameter is invalid length.");
            }
            break;
        
          default:
            callbackContext.error("'" + action + "' is not defined in GoogleMaps plugin.");
            break;
          }
        } catch (Exception e) {
          e.printStackTrace();
          callbackContext.error("Java Error\n" + e.getMessage());
        }
      }
    };
    cordova.getActivity().runOnUiThread(runnable);
    
    return true;
  }


  /**
   * Set visibility of the map
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  private void setVisible(JSONArray args, CallbackContext callbackContext) throws JSONException {
    boolean visible = args.getBoolean(0);
    if (this.windowLayer == null) {
      if (visible && this.mapView.getParent() == null) {
        this.webView.addView(mapView);
      }
      if (!visible && this.mapView.getParent() != null) {
        this.webView.removeView(mapView);
      }
    }
    callbackContext.success();
  }
  
  
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  private void getMap(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (map != null) {
      callbackContext.success();
      return;
    }
    
    
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
      AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
      
      alertDialogBuilder
        .setMessage("Google Maps Android API v2 is not available, because this device does not have Google Play Service.")
        .setCancelable(false)
        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog,int id) {
            dialog.dismiss();
          }
        }); 
      AlertDialog alertDialog = alertDialogBuilder.create();
      
      // show it
      alertDialog.show();

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
    JSONObject params = args.getJSONObject(0);
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
    map = mapView.getMap();
    
    //controls
    Boolean isEnabled = true;
    if (params.has("controls")) {
      JSONObject controls = params.getJSONObject("controls");

      if (controls.has("myLocationButton")) {
        isEnabled = controls.getBoolean("myLocationButton");
        map.setMyLocationEnabled(isEnabled);
      }
    }
    if (isEnabled) {
      
      try {
        Constructor<LocationClient> constructor = LocationClient.class.getConstructor(Context.class, GooglePlayServicesClient.ConnectionCallbacks.class,  GooglePlayServicesClient.OnConnectionFailedListener.class);
        this.locationClient = constructor.newInstance(this.activity, this, this);
      } catch (Exception e) {}
      
      //this.locationClient = new LocationClient(this.activity, this, this);
      if (this.locationClient != null) {
        // The LocationClient class is available. 
        this.locationClient.connect();
      }
    }
    
    // Set event listener
    map.setOnCameraChangeListener(this);
    map.setOnInfoWindowClickListener(this);
    map.setOnMapClickListener(this);
    map.setOnMapLoadedCallback(this);
    map.setOnMapLongClickListener(this);
    map.setOnMarkerClickListener(this);
    map.setOnMarkerDragListener(this);
    map.setOnMyLocationButtonClickListener(this);
    
    // Load PluginMap class
    this.loadPlugin("Map");

    // ------------------------------
    // Embed the map if a container is specified.
    // ------------------------------
    if (args.length() == 2) {
      this.mapDivLayoutJSON = args.getJSONObject(1);
      this.webView.addView(mapView);
      this.updateMapViewLayout();
    }
    
    //Custom info window
    map.setInfoWindowAdapter(this);
    
    callbackContext.success();
    return;
  }
  private int contentToView(long d) {
    return Math.round(d * webView.getScale());
  }
  
  //-----------------------------------
  // Create the instance of class
  //-----------------------------------
  private void loadPlugin(String serviceName) {
    if (plugins.containsKey(serviceName)) {
      return;
    }
    try {
      @SuppressWarnings("rawtypes")
      Class pluginCls = Class.forName("plugin.google.maps.Plugin" + serviceName);
      
      CordovaPlugin plugin = (CordovaPlugin) pluginCls.newInstance();
      PluginEntry pluginEntry = new PluginEntry("GoogleMaps", plugin);
      this.plugins.put(serviceName, pluginEntry);
      plugin.initialize(this.cordova, webView);
      ((MyPluginInterface)plugin).setMapCtrl(this);
      if (map == null) {
        Log.e(TAG, "map is null!");
      }
      
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
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
      if (android.os.Build.VERSION.SDK_INT < 11) {
        // Force redraw the map
        mapView.requestLayout();
      }
      break;
    }
    
    return null;
  }

  private void closeWindow() {
    webView.hideCustomView();
  }
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
    
    if (this.mapView.getParent() != null) {
      this.webView.removeView(mapView);
    }
    
    ViewGroup.LayoutParams mapLayout = (ViewGroup.LayoutParams) mapView.getLayoutParams();
    if (mapLayout == null) {
      mapLayout = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }
    mapLayout.width = ViewGroup.LayoutParams.MATCH_PARENT;
    mapLayout.height = ViewGroup.LayoutParams.MATCH_PARENT;
    mapView.setLayoutParams(mapLayout);
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
    
    
    root = (ViewGroup) webView.getParent();
    webView.setVisibility(View.GONE);
    root.addView(windowLayer);
    
    //Dummy view for the backbutton event
    FrameLayout dummyLayout = new FrameLayout(activity);
    this.webView.showCustomView(dummyLayout, new WebChromeClient.CustomViewCallback() {

      @Override
      public void onCustomViewHidden() {
        mapFrame.removeView(mapView);
        if (mapDivLayoutJSON != null) {
          webView.addView(mapView);
          updateMapViewLayout();
        }
        root.removeView(windowLayer);
        webView.setVisibility(View.VISIBLE);
        windowLayer = null;
        
        
        GoogleMaps.this.onMapEvent("map_close");
      }
    });

    
    callbackContext.success();
  }

  private void resizeMap(JSONArray args, CallbackContext callbackContext) throws JSONException {
    mapDivLayoutJSON = args.getJSONObject(0);
    updateMapViewLayout();
    callbackContext.success();
  }
  
  private void updateMapViewLayout() {
    try {
      int divW = contentToView(mapDivLayoutJSON.getLong("width") );
      int divH = contentToView(mapDivLayoutJSON.getLong("height"));
      int divLeft = contentToView(mapDivLayoutJSON.getLong("left"));
      int divTop = contentToView(mapDivLayoutJSON.getLong("top"));
      
      ViewGroup.LayoutParams lParams = mapView.getLayoutParams();
      if (lParams instanceof android.widget.AbsoluteLayout.LayoutParams) {
        AbsoluteLayout.LayoutParams params = (AbsoluteLayout.LayoutParams) lParams;
        params.width = divW;
        params.height = divH;
        params.y = divTop;
        params.x = divLeft;
        mapView.setLayoutParams(params);
        return;
      }

      if (lParams instanceof android.widget.LinearLayout.LayoutParams) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lParams;
        params.width = divW;
        params.height = divH;
        params.topMargin = divTop;
        params.leftMargin = divLeft;
        mapView.setLayoutParams(params);
      }
      
    } catch (JSONException e) {}
  }
  
  private void closeDialog(final JSONArray args, final CallbackContext callbackContext) {
    this.closeWindow();
    callbackContext.success();
  }

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
      @SuppressWarnings({ "rawtypes", "unused" })
      Class GoogleMapsClass = Class.forName("com.google.android.gms.maps.GoogleMap");
    } catch (Exception e) {
      Log.e("GoogleMaps", "Error", e);
      callbackContext.error(e.getMessage());
      return;
    }
    
    callbackContext.success();
  }
  private void getMyLocation(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    JSONObject result = null;
    
    if (this.locationClient == null) {
      
      LocationManager locationManager = (LocationManager) this.activity.getSystemService(Context.LOCATION_SERVICE);
      if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
          //Ask the user to enable GPS
          AlertDialog.Builder builder = new AlertDialog.Builder(this.activity);
          builder.setTitle(this.activity.getApplication().getApplicationInfo().name);
          builder.setMessage("Would you like to enable GPS?");
          builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                  //Launch settings, allowing user to make a change
                  Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                  activity.startActivity(intent);
              }
          });
          builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                  //No location service, no Activity
                  dialog.dismiss();
                  callbackContext.error("The GPS is disabled.");
              }
          });
          builder.create().show();
          return;
      }

      PackageManager pm = this.activity.getPackageManager();
      boolean hasGPS = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
      if (hasGPS == false) {
        callbackContext.error("This device don't have GPS.");
        return;
      }
      
      Criteria criteria = new Criteria();
      String bestProvider = locationManager.getBestProvider(criteria, false);
      Location location = locationManager.getLastKnownLocation(bestProvider);
      if (location != null) {
        result = PluginUtil.location2Json(location);
        callbackContext.success(result);
      } else {
        callbackContext.error("Can not detect your location");
      }
      return;
    }
    
    if (this.locationClient.isConnected()) {
      Location location = this.locationClient.getLastLocation();
      result = PluginUtil.location2Json(location);
      callbackContext.success(result);
    } else {
      callbackContext.error("Location client is not available.");
      /*
      JSONObject latLng = new JSONObject();
      latLng.put("lat", 0);
      latLng.put("lng", 0);
      
      result = new JSONObject();
      result.put("latLng", latLng);
      result.put("speed", null);
      result.put("bearing", null);
      result.put("altitude", 0);
      result.put("accuracy", null);
      result.put("provider", null);
      result.put("elapsedRealtimeNanos", System.nanoTime());
      result.put("time", System.currentTimeMillis());
      result.put("hashCode", -1);
      */
    }
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
  private void onMarkerEvent(final String eventName, final Marker marker) {
    String markerId = "marker_" + marker.getId();
    PluginEntry markerPlugin = this.plugins.get("Marker");
    PluginMarker markerClass = (PluginMarker) markerPlugin.plugin;
    if (markerClass.objects.containsKey(markerId)) {
      webView.loadUrl("javascript:plugin.google.maps.Map." +
                  "_onMarkerEvent('" + eventName + "','" + markerId + "')");
    } else {

      Set<String> keySet = markerClass.objects.keySet();
      Iterator<String> iterator = keySet.iterator();
      String key = null;
      Boolean isHit = false;
      while(iterator.hasNext()) {
        key = iterator.next();
        if (key.endsWith(markerId)) {
          isHit = true;
          break;
        }
      }
      if (isHit) {
        String[] tmp = key.split(":");
        
        webView.loadUrl("javascript:plugin.google.maps.Map." +
                  "_onMarkerEvent('" + eventName + "','" + tmp[1] + "')");
      }
    }
    
  }
  
  /**
   * Notify overlay event to JS
   * @param eventName
   * @param polygon
   */
  private void onPolygonEvent(final String eventName, final Polygon polygon) {
    String polygonId = "polygon_" + polygon.getId();
    PluginEntry polygonPlugin = this.plugins.get("Polygon");
    PluginPolygon polygonClass = (PluginPolygon) polygonPlugin.plugin;
    if (polygonClass.objects.containsKey(polygonId)) {
      webView.loadUrl("javascript:plugin.google.maps.Map." +
                  "_onOverlayEvent('" + eventName + "','" + polygonId + "')");
    } else {

      Set<String> keySet = polygonClass.objects.keySet();
      Iterator<String> iterator = keySet.iterator();
      String key = null;
      Boolean isHit = false;
      while(iterator.hasNext()) {
        key = iterator.next();
        if (key.endsWith(polygonId)) {
          isHit = true;
          break;
        }
      }
      if (isHit) {
        String[] tmp = key.split(":");
        
        Log.d("GoogleMaps", "clicked = " + polygonId + ", hit = " + key);
        
        webView.loadUrl("javascript:plugin.google.maps.Map." +
                  "_onOverlayEvent('" + eventName + "','" + tmp[1] + "')");
      }
    }
    
  }
  
  /**
   * Notify overlay event to JS
   * @param eventName
   * @param circle
   */
  private void onCircleEvent(final String eventName, final Circle circle) {
    String circleId = "circle_" + circle.getId();
    PluginEntry circlePlugin = this.plugins.get("Circle");
    PluginCircle circleClass = (PluginCircle) circlePlugin.plugin;
    if (circleClass.objects.containsKey(circleId)) {
      webView.loadUrl("javascript:plugin.google.maps.Map." +
                  "_onOverlayEvent('" + eventName + "','" + circleId + "')");
    } else {

      Set<String> keySet = circleClass.objects.keySet();
      Iterator<String> iterator = keySet.iterator();
      String key = null;
      Boolean isHit = false;
      while(iterator.hasNext()) {
        key = iterator.next();
        if (key.endsWith(circleId)) {
          isHit = true;
          break;
        }
      }
      if (isHit) {
        String[] tmp = key.split(":");
        
        webView.loadUrl("javascript:plugin.google.maps.Map." +
                  "_onOverlayEvent('" + eventName + "','" + tmp[1] + "')");
      }
    } 
  }
  
  /**
   * Notify overlay event to JS
   * @param eventName
   * @param groundOverlay
   */
  private void onGroundOverlayEvent(final String eventName, final GroundOverlay groundOverlay) {
    String groundOverlayId = "groundOverlay_" + groundOverlay.getId();
  
    PluginEntry groundOverlayPlugin = this.plugins.get("GroundOverlay");
    PluginGroundOverlay groundOverlayClass = (PluginGroundOverlay) groundOverlayPlugin.plugin;
    if (groundOverlayClass.objects.containsKey(groundOverlayId)) {
      webView.loadUrl("javascript:plugin.google.maps.Map." +
                  "_onOverlayEvent('" + eventName + "','" + groundOverlayId + "')");
    } else {

      Set<String> keySet = groundOverlayClass.objects.keySet();
      Iterator<String> iterator = keySet.iterator();
      String key = null;
      Boolean isHit = false;
      while(iterator.hasNext()) {
        key = iterator.next();
        if (key.endsWith(groundOverlayId)) {
          isHit = true;
          break;
        }
      }
      if (isHit) {
        String[] tmp = key.split(":");
        
        webView.loadUrl("javascript:plugin.google.maps.Map." +
                  "_onOverlayEvent('" + eventName + "','" + tmp[1] + "')");
      }
    } 
  }

  @Override
  public boolean onMarkerClick(Marker marker) {
    this.onMarkerEvent("click", marker);
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

  /**
   * Notify map click event to JS, also checks for click on a polygon and triggers onPolygonEvent
   * @param point
   */
  public void onMapClick(LatLng point) {
    boolean hitPoly = false;

    // Loop through all polygons to check if within the touch point
    PluginEntry polylinePlugin = this.plugins.get("Polyline");
    if(polylinePlugin != null) {
      PluginPolyline polylineClass = (PluginPolyline) polylinePlugin.plugin;

      LatLngBounds bounds;
      List<LatLng> points, hitArea;
      int i = 0;
      PolygonOptions polygonOptions;
      Polyline polyline;
      hitArea = new ArrayList<LatLng>();
      LatLng latLng;

      for (HashMap.Entry<String, Object> entry : polylineClass.objects.entrySet()) {
        polyline = (Polyline) entry.getValue();
        points = polyline.getPoints();
        /*
        bounds = convertToLatLngBounds(points);
        if (bounds.contains(point) == false) {
          continue;
        }
        */
        
        for (i = 0; i < points.size() - 1; i++) {
          latLng = points.get(i);
          hitArea.add(new LatLng(latLng.latitude + 3, latLng.longitude));
          hitArea.add(new LatLng(latLng.latitude - 3, latLng.longitude));
          latLng = points.get(i + 1);
          hitArea.add(new LatLng(latLng.latitude - 3, latLng.longitude));
          hitArea.add(new LatLng(latLng.latitude + 3, latLng.longitude));
          
          polygonOptions = new PolygonOptions();
          polygonOptions.addAll(hitArea);
          polygonOptions.fillColor(Color.TRANSPARENT);
          polygonOptions.strokeColor(Color.BLUE);
          polygonOptions.strokeWidth(4);
          Polygon hitAreaPolygon = map.addPolygon(polygonOptions);
          

          if (this.polygonContainsPoint(hitArea, point)) {
            Log.d("GoogleMaps", "clicked!");
            hitPoly = true;
            hitAreaPolygon.setFillColor(Color.GREEN);
            //this.onPolylineEvent("overlay_click", polyline);
          }
          polygonOptions = null;
          hitArea.clear();
        }
        
        
        polyline = null;
      }
    }
    // Loop through all polygons to check if within the touch point
    PluginEntry polygonPlugin = this.plugins.get("Polygon");
    if (polygonPlugin != null) {
      PluginPolygon polygonClass = (PluginPolygon) polygonPlugin.plugin;
    
      for (HashMap.Entry<String, Object> entry : polygonClass.objects.entrySet()) {
        Polygon polygon = (Polygon) entry.getValue();
        if (this.polygonContainsPoint(polygon.getPoints(), point)) {
          hitPoly = true;
          this.onPolygonEvent("overlay_click", polygon);
        }
      }
    }
    
    // Loop through all circles to check if within the touch point
    PluginEntry circlePlugin = this.plugins.get("Circle");
    if (circlePlugin != null) {
      PluginCircle circleClass = (PluginCircle) circlePlugin.plugin;
    
      for (HashMap.Entry<String, Object> entry : circleClass.objects.entrySet()) {
        Circle circle = (Circle) entry.getValue();
        if (this.circleContainsPoint(circle, point)) {
          hitPoly = true;
          this.onCircleEvent("overlay_click", circle);
        }
      }
    }
    
    // Loop through ground overlays to check if within the touch point
    PluginEntry groundOverlayPlugin = this.plugins.get("GroundOverlay");
    if (groundOverlayPlugin != null) {
      PluginGroundOverlay groundOverlayClass = (PluginGroundOverlay) groundOverlayPlugin.plugin;
    
      for (HashMap.Entry<String, Object> entry : groundOverlayClass.objects.entrySet()) {
        GroundOverlay groundOverlay = (GroundOverlay) entry.getValue();
        if (this.groundOverlayContainsPoint(groundOverlay, point)) {
          hitPoly = true;
          this.onGroundOverlayEvent("overlay_click", groundOverlay);
        }
      }
    }
    
    // Only emit click event if no polygons hit
    //if (!hitPoly) {
      this.onMapEvent("click", point);
    //}
  }
  private LatLngBounds convertToLatLngBounds(List<LatLng> points) {
    LatLngBounds.Builder latLngBuilder = LatLngBounds.builder();
    Iterator<LatLng> iterator = points.listIterator();
    while (iterator.hasNext()) {
      latLngBuilder.include(iterator.next());
    }
    return latLngBuilder.build();
  }
  /*
  private boolean intersects(LatLngBounds bounds) {
    final boolean latIntersects =
            (bounds.northeast.latitude >= southwest.latitude) && (bounds.southwest.latitude <= northeast.latitude);
    final boolean lngIntersects =
            (bounds.northeast.longitude >= southwest.longitude) && (bounds.southwest.longitude <= northeast.longitude);

    return latIntersects && lngIntersects;
  }*/

  /**
   * Check if a polygon contains a point, uses ray-casting
   * @param polygon
   * @param point
   */
  private boolean polygonContainsPoint(List<LatLng> path, LatLng point) {
    int cn = 0;
    Projection projection = map.getProjection();
    VisibleRegion visibleRegion = projection.getVisibleRegion();
    LatLngBounds bounds = visibleRegion.latLngBounds;
    Point sw = projection.toScreenLocation(bounds.southwest);
    
    Point touchPoint = projection.toScreenLocation(point);
    touchPoint.y = sw.y - touchPoint.y;
    float vt;
    
    for (int i = 0; i < path.size() - 1; i++) {
      Point a = projection.toScreenLocation(path.get(i));
      a.y = sw.y - a.y;
      Point b = projection.toScreenLocation(path.get(i + 1));
      b.y = sw.y - b.y;
      
      if (((a.y <= touchPoint.y) && (b.y > touchPoint.y)) ||
          ((a.y > touchPoint.y) && (b.y <= touchPoint.y))) {
        
        vt = ((float)touchPoint.y - (float)a.y) / ((float)b.y - (float)a.y);
        Log.d("GoogleMaps", "(" + touchPoint.y + " - " + a.y+ ") / (" + b.y + " - " + a.y + ") = " + vt);
        if ((float)touchPoint.x < ((float)((float)a.x + ((float)vt * ((float)b.x - (float)a.x))))) {
          cn++;
        }
      }
    }
    
    Log.d("GoogleMaps", cn + " % 2 = " + (cn % 2));
    return (cn % 2 == 1);
  }
  
  /**
   * Used to ray-cast a point against each line segment
   * @param point
   * @param a
   * @param b
   */
  private boolean rayCrossesSegment(Point point, Point a, Point b) {
    double pX = point.x;
    double pY = point.y;
    double aX = a.x;
    double aY = a.y;
    double bX = b.x;
    double bY = b.y;
    Log.d("GoogleMaps", "touch= " +  point + " / a= " + a + " / b= " + b);
    //if ( (aY>pY && bY>pY) || (aY<pY && bY<pY) || (aX<pX && bX<pX) ) {
    if ( (aY>pY && bY>pY) || (aY<pY && bY<pY) ) {
          return false;
    }
    
    double m = (aY-bY) / (aX-bX);
    double bee = (-aX) * m + aY;
    double x = (pY - bee) / m;

    return x > pX;
  }
  
  /**
   * Check if a circle contains a point
   * @param circle
   * @param point
   */
  private boolean circleContainsPoint(Circle circle, LatLng point) {
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
  private boolean groundOverlayContainsPoint(GroundOverlay groundOverlay, LatLng point) {
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
    if (this.locationClient != null) {
      this.locationClient.disconnect();
    }
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
  

  @Override
  public void onConnectionFailed(ConnectionResult result) {}

  @Override
  public void onConnected(Bundle connectionHint) {}

  @Override
  public void onDisconnected() {}

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  @Override
  public View getInfoContents(Marker marker) {
    String title = marker.getTitle();
    String snippet = marker.getSnippet();
    if ((title == null) && (snippet == null)) {
      return null;
    }
    
    JSONObject styles = null;
    String styleId = "marker_style_" + marker.getId();
    PluginEntry pluginEntry = this.plugins.get("Marker");
    PluginMarker pluginMarker = (PluginMarker)pluginEntry.plugin;
    if (pluginMarker.objects.containsKey(styleId)) {
      styles = (JSONObject) pluginMarker.objects.get(styleId);
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
    callbackContext.success();
  }
}
