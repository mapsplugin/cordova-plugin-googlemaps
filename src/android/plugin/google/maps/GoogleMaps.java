package plugin.google.maps;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginEntry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
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
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CameraPosition.Builder;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class GoogleMaps extends CordovaPlugin implements View.OnClickListener, OnMarkerClickListener,
      OnInfoWindowClickListener, OnMapClickListener, OnMapLongClickListener,
      OnCameraChangeListener, OnMapLoadedCallback, OnMarkerDragListener,
      OnMyLocationButtonClickListener, OnMyLocationChangeListener,
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
    exec
  }
  
  private enum EVENTS {
    onScrollChanged
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
    this.locationClient = new LocationClient(activity, this, this);
  }

  @Override
  public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    /*if (args != null && args.length() > 0) {
      Log.d(TAG, "action=" + action + " args[0]=" + args.getString(0));
    } else {
      Log.d(TAG, "action=" + action);
    }*/

    Runnable runnable = new Runnable() {
      public void run() {
        try {
          switch(METHODS.valueOf(action)) {
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
          case exec:
          
            String classMethod = args.getString(0);
            String[] params = classMethod.split("\\.", 0);
            
            // Load the class plugin
            if (classMethod.equals(params[0] + ".create" + params[0])) {
              GoogleMaps.this.loadPlugin(params[0]);
            }
            
            
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
  @SuppressWarnings("unused")
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
      GooglePlayServicesUtil.getErrorDialog(checkGooglePlayServices, activity,
          1122).show();

      callbackContext.error("google play services is missing!!!!");
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
      this.locationClient.connect();
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
    map.setOnMyLocationChangeListener(this);
    
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

  @SuppressWarnings("unused")
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

  private void getMyLocation(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    JSONObject result = null;
    if (this.locationClient.isConnected()) {
      Location location = this.locationClient.getLastLocation();
      result = PluginUtil.location2Json(location);
      callbackContext.success(result);
    } else {
      callbackContext.error("Location client is not connected.");
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

  @Override
  public void onMapClick(LatLng point) {
    this.onMapEvent("click", point);
  }
  
  /**
   * Notify the myLocationChange event to JS
   */
  @SuppressLint("NewApi")
  @Override
  public void onMyLocationChange(Location location) {
    String jsonStr = "";
    try {
      jsonStr = PluginUtil.location2Json(location).toString();
    } catch (JSONException e) {}
    webView.loadUrl("javascript:plugin.google.maps.Map._onMyLocationChange(" + jsonStr + ")");
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
      params.put("hashCode", position.hashCode());
      params.put("bearing", position.bearing);
      params.put("target", position.target);
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
    this.locationClient.disconnect();
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

    // Linear layout
    LinearLayout windowLayer = new LinearLayout(activity);
    windowLayer.setPadding(3, 3, 3, 3);
    windowLayer.setOrientation(LinearLayout.VERTICAL);
    LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    layoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER;
    windowLayer.setLayoutParams(layoutParams);
    
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
        textView.setTextColor(Color.BLACK);
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        Build.VERSION version = new Build.VERSION();
        if (version.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
          textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }
        windowLayer.addView(textView);
      }
    }
    if (snippet != null) {
      float density = Resources.getSystem().getDisplayMetrics().density;
      snippet = snippet.replaceAll("\n", "");
      TextView textView2 = new TextView(this.cordova.getActivity());
      textView2.setText(snippet);
      textView2.setTextColor(Color.GRAY);
      textView2.setTextSize((textView2.getTextSize() / 6 * 5) / density);
      textView2.setGravity(Gravity.CENTER_HORIZONTAL);
      Build.VERSION version = new Build.VERSION();
      if (version.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        textView2.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
      }
      windowLayer.addView(textView2);
    }
    return windowLayer;
  }

  @Override
  public View getInfoWindow(Marker marker) {
    return null;
  }
}
