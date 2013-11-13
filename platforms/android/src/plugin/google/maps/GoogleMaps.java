package plugin.google.maps;

import java.util.HashMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginEntry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Location;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.GoogleMap;
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

public class GoogleMaps extends CordovaPlugin implements View.OnClickListener {
  private final String TAG = "GoogleMapsPlugin";
  private final HashMap<String, PluginEntry> plugins = new HashMap<String, PluginEntry>();
  
  private enum METHODS {
    getMap,
    showMap,
    exec
  }
  
  public MapView mapView = null;
  public GoogleMap map = null;
  private Activity activity;
  private FrameLayout baseLayer;
  private ViewGroup root;
  private final int CLOSE_LINK_ID = 0x7f999990;  //random
  private final int LICENSE_LINK_ID = 0x7f99991; //random

  private JavaScriptInterface jsInterface;

  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
    activity = cordova.getActivity();
    jsInterface = new JavaScriptInterface(activity);
    Runnable runnable = new Runnable() {
      public void run() {
        webView.addJavascriptInterface(jsInterface, "jsInterface");
      }
    };
    cordova.getActivity().runOnUiThread(runnable);
  }

  @Override
  public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (args != null && args.length() > 0) {
      Log.d(TAG, "action=" + action + " args[0]=" + args.getString(0));
    } else {
      Log.d(TAG, "action=" + action);
    }

    Runnable runnable = new Runnable() {
      public void run() {
        try {
          switch(METHODS.valueOf(action)) {
          case getMap:
            GoogleMaps.this.getMap(args, callbackContext);
            break;
          case showMap:
            GoogleMaps.this.showMap(args, callbackContext);
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
              callbackContext.error(action + " parameter is invalid length.");
            }
          break;
        
          default:
            callbackContext.error(action + "is not defined.");
            break;
          }
        } catch (Exception e) {
          e.printStackTrace();
          callbackContext.error(action + "is not defined.");
        }
      }
    };
    cordova.getActivity().runOnUiThread(runnable);
    
    return true;
  }


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

    // ------------------------------
    // Initialize Google Maps SDK
    // ------------------------------
    try {
      MapsInitializer.initialize(activity);
    } catch (GooglePlayServicesNotAvailableException e) {
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
      int mapTypeId = 0;
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
      if (mapTypeId != 0) {
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
    if (params.has("controls")) {
      JSONObject controls = params.getJSONObject("controls");

      if (controls.has("myLocationButton")) {
        map.setMyLocationEnabled(controls.getBoolean("myLocationButton"));
      }
    }
    
    // Set event listener
    map.setOnCameraChangeListener(jsInterface);
    map.setOnMapClickListener(jsInterface);
    map.setOnInfoWindowClickListener(jsInterface);
    map.setOnMapLoadedCallback(jsInterface);
    map.setOnMapLongClickListener(jsInterface);
    map.setOnMarkerDragListener(jsInterface);
    map.setOnMyLocationButtonClickListener(jsInterface);
    map.setOnMyLocationChangeListener(jsInterface);
    
    // Load PluginMap class
    this.loadPlugin("Map");

    // ------------------------------
    // Create the map window
    // ------------------------------
    
    //base layout
    baseLayer = new FrameLayout(activity);
    baseLayer.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    
    // window layout
    LinearLayout windowLayer = new LinearLayout(activity);
    windowLayer.setPadding(25, 25, 25, 25);
    LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
    windowLayer.setLayoutParams(layoutParams);
    baseLayer.addView(windowLayer);
    
    // dialog window layer
    FrameLayout dialogLayer = new FrameLayout(activity);
    dialogLayer.setLayoutParams(layoutParams);
    //dialogLayer.setPadding(15, 15, 15, 0);
    dialogLayer.setBackgroundColor(Color.LTGRAY);
    windowLayer.addView(dialogLayer);

    // map frame
    LinearLayout mapFrame = new LinearLayout(activity);
    mapFrame.setPadding(0, 0, 0, 50);
    dialogLayer.addView(mapFrame);
    
    // map
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
    closeLink.setPadding(10, 0, 0, 10);
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
    licenseLink.setPadding(10, 10, 10, 10);
    licenseLink.setOnClickListener(GoogleMaps.this);
    licenseLink.setId(LICENSE_LINK_ID);
    buttonFrame.addView(licenseLink);
    
    callbackContext.success();
    return;
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
      plugin.initialize((CordovaInterface) activity, webView);
      ((MyPlugin)plugin).setMap(map);
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

  private void showMap(final JSONArray args, final CallbackContext callbackContext) {
    root = (ViewGroup) webView.getParent();
    root.removeView(webView);
    baseLayer.addView(webView, 0);
    activity.setContentView(baseLayer);
    callbackContext.success();
  }



  public class JavaScriptInterface implements OnMarkerClickListener,
      OnInfoWindowClickListener, OnMapClickListener, OnMapLongClickListener,
      OnCameraChangeListener, OnMapLoadedCallback, OnMarkerDragListener,
      OnMyLocationButtonClickListener, OnMyLocationChangeListener {
    
    Context mContext;

    /** Instantiate the interface and set the context */
    JavaScriptInterface(Context context) {
      mContext = context;
    }

    /**
     * Notify marker event to JS
     * @param eventName
     * @param marker
     */
    private void onMarkerEvent(final String eventName, final Marker marker) {
      Runnable runnable = new Runnable() {
        public void run() {
          webView.loadUrl(
              "javascript:plugin.google.maps.Map." +
                  "_onMarkerEvent('" + eventName + "'," + marker.hashCode() + ")");
        }
      };
      cordova.getActivity().runOnUiThread(runnable);
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
    private void onMapEvent(final String eventName, final LatLng point) {
      Runnable runnable = new Runnable() {
        public void run() {
          webView.loadUrl("javascript:plugin.google.maps.Map._onMapEvent(" +
              "'" + eventName + "', [" + point.latitude + "," + point.longitude + "])");
        }
      };
      cordova.getActivity().runOnUiThread(runnable);
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
      JSONObject params = new JSONObject();
      String paramsStr = "";
      try {
        params.put("latitude", location.getLatitude());
        params.put("longitude", location.getLongitude());
        params.put("elapsedRealtimeNanos", location.getElapsedRealtimeNanos());
        params.put("time", location.getTime());
        if (location.hasAccuracy()) {
          params.put("accuracy", location.getAccuracy());
        }
        if (location.hasBearing()) {
          params.put("bearing", location.getBearing());
        }
        if (location.hasAltitude()) {
          params.put("altitude", location.getAltitude());
        }
        if (location.hasSpeed()) {
          params.put("speed", location.getSpeed());
        }
        params.put("provider", location.getProvider());
        params.put("hashCode", location.hashCode());
        paramsStr = params.toString();
      } catch (JSONException e) {
        e.printStackTrace();
      }
      final String jsonStr = paramsStr;
      
      Runnable runnable = new Runnable() {
        public void run() {
          webView.loadUrl("javascript:plugin.google.maps.Map._onMyLocationChange(" + jsonStr + ")");
        }
      };
      cordova.getActivity().runOnUiThread(runnable);
    }

    @Override
    public boolean onMyLocationButtonClick() {
      Runnable runnable = new Runnable() {
        public void run() {
          webView.loadUrl("javascript:plugin.google.maps.Map._onMapEvent('my_location_button_click')");
        }
      };
      cordova.getActivity().runOnUiThread(runnable);
      return false;
    }


    @Override
    public void onMapLoaded() {
      Runnable runnable = new Runnable() {
        public void run() {
          webView.loadUrl("javascript:plugin.google.maps.Map._onMapEvent('map_loaded')");
        }
      };
      cordova.getActivity().runOnUiThread(runnable);
    }

    /**
     * Notify the myLocationChange event to JS
     */
    @Override
    public void onCameraChange(CameraPosition position) {
      JSONObject params = new JSONObject();
      String paramsStr = "";
      try {
        params.put("hashCode", position.hashCode());
        params.put("bearing", position.bearing);
        params.put("target", position.target);
        params.put("tilt", position.tilt);
        params.put("zoom", position.zoom);
        paramsStr = params.toString();
      } catch (JSONException e) {
        e.printStackTrace();
      }
      final String jsonStr = paramsStr;
      
      Runnable runnable = new Runnable() {
        public void run() {
          webView.loadUrl("javascript:plugin.google.maps.Map._onCameraChange(" + jsonStr + ")");
        }
      };
      cordova.getActivity().runOnUiThread(runnable);
    }
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
  
  private void closeWindow() {
    root.removeView(baseLayer);
    baseLayer.removeView(webView);
    activity.setContentView(webView);
  }
  
  private void showLicenseText() {
    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
    
    alertDialogBuilder
      .setMessage(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(activity))
      .setCancelable(false)
      .setPositiveButton("Close",new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog,int id) {
          dialog.dismiss();
        }
      });

    // create alert dialog
    AlertDialog alertDialog = alertDialogBuilder.create();

    // show it
    alertDialog.show();
  }

  @Override
  public void onClick(View view) {
    int viewId = view.getId();
    Log.d(TAG, "viewId = " + viewId);
    if (viewId == CLOSE_LINK_ID) {
      closeWindow();
      return;
    }
    if (viewId == LICENSE_LINK_ID) {
      showLicenseText();
      return;
    }
  }
}
