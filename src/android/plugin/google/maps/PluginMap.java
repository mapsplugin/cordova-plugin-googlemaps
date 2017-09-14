package plugin.google.maps;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnIndoorStateChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CameraPosition.Builder;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.IndoorBuilding;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.VisibleRegion;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginEntry;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class PluginMap extends MyPlugin implements OnMarkerClickListener,
    OnInfoWindowClickListener, OnMapClickListener, OnMapLongClickListener,
    OnMarkerDragListener, GoogleMap.OnMapLoadedCallback,
    OnMyLocationButtonClickListener, OnIndoorStateChangeListener, InfoWindowAdapter,
    GoogleMap.OnCameraIdleListener, GoogleMap.OnCameraMoveCanceledListener,
    GoogleMap.OnCameraMoveListener, GoogleMap.OnCameraMoveStartedListener,
    GoogleMap.OnInfoWindowLongClickListener, GoogleMap.OnInfoWindowCloseListener {

  private LatLngBounds initCameraBounds;
  private Activity activity;
  public GoogleMap map;
  public MapView mapView;
  public String mapId;
  public boolean isVisible = true;
  public boolean isClickable = true;
  public final String TAG = mapId;
  public String mapDivId;
  public HashMap<String, PluginEntry> plugins = new HashMap<String, PluginEntry>();
  private final float DEFAULT_CAMERA_PADDING = 20;
  private Projection projection = null;
  public Marker activeMarker = null;
  private boolean isDragging = false;
  public static final ConcurrentHashMap<String, Object> objects = new ConcurrentHashMap<String, Object>();


  private enum TEXT_STYLE_ALIGNMENTS {
    left, center, right
  }

  private final String ANIMATE_CAMERA_DONE = "animate_camera_done";
  private final String ANIMATE_CAMERA_CANCELED = "animate_camera_canceled";

  private Handler mainHandler;

  private class AsyncUpdateCameraPositionResult {
    CameraUpdate cameraUpdate;
    int durationMS;
    LatLngBounds cameraBounds;
    double cameraPadding;
  }

  private class AsyncSetOptionsResult {
    int MAP_TYPE_ID;
    CameraPosition cameraPosition;
    LatLngBounds cameraBounds;
    double cameraPadding;
    String styles;
  }

  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
    activity = cordova.getActivity();
    mainHandler = new Handler(Looper.getMainLooper());
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public void getMap(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    GoogleMapOptions options = new GoogleMapOptions();
    mapId = args.getString(0);
    final JSONObject params = args.getJSONObject(1);

    //controls
    if (params.has("controls")) {
      JSONObject controls = params.getJSONObject("controls");

      if (controls.has("compass")) {
        options.compassEnabled(controls.getBoolean("compass"));
      }
      if (controls.has("zoom")) {
        options.zoomControlsEnabled(controls.getBoolean("zoom"));
      }
      if (controls.has("mapToolbar")) {
        options.mapToolbarEnabled(controls.getBoolean("mapToolbar"));
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
    if (!params.has("styles") &&  params.has("mapType")) {
      String typeStr = params.getString("mapType");
      int mapTypeId = -1;
      mapTypeId = typeStr.equals("MAP_TYPE_NORMAL") ? GoogleMap.MAP_TYPE_NORMAL : mapTypeId;
      mapTypeId = typeStr.equals("MAP_TYPE_HYBRID") ? GoogleMap.MAP_TYPE_HYBRID : mapTypeId;
      mapTypeId = typeStr.equals("MAP_TYPE_SATELLITE") ? GoogleMap.MAP_TYPE_SATELLITE : mapTypeId;
      mapTypeId = typeStr.equals("MAP_TYPE_TERRAIN") ? GoogleMap.MAP_TYPE_TERRAIN : mapTypeId;
      mapTypeId = typeStr.equals("MAP_TYPE_NONE") ? GoogleMap.MAP_TYPE_NONE : mapTypeId;
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
      if (camera.has("target")) {
        Object target = camera.get("target");
        @SuppressWarnings("rawtypes")
        Class targetClass = target.getClass();
        if ("org.json.JSONArray".equals(targetClass.getName())) {
          JSONArray points = camera.getJSONArray("target");
          initCameraBounds = PluginUtil.JSONArray2LatLngBounds(points);
          builder.target(initCameraBounds.getCenter());

        } else {
          JSONObject latLng = camera.getJSONObject("target");
          builder.target(new LatLng(latLng.getDouble("lat"), latLng.getDouble("lng")));
          if (camera.has("zoom")) {
            builder.zoom((float) camera.getDouble("zoom"));
          }
        }
      } else {
        builder.target(new LatLng(0, 0));
      }
      if (camera.has("tilt")) {
        builder.tilt((float) camera.getDouble("tilt"));
      }
      options.camera(builder.build());
    }

    mapView = new MapView(activity, options);

    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mapView.onCreate(null);

        mapView.getMapAsync(new OnMapReadyCallback() {
          @Override
          public void onMapReady(GoogleMap googleMap) {

            map = googleMap;
            projection = map.getProjection();

            try {
              //styles
              if (params.has("styles")) {
                String styles = params.getString("styles");
                MapStyleOptions styleOptions = new MapStyleOptions(styles);
                map.setMapStyle(styleOptions);
                map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
              }

              //controls
              if (params.has("controls")) {
                JSONObject controls = params.getJSONObject("controls");

                if (controls.has("indoorPicker")) {
                  Boolean isEnabled = controls.getBoolean("indoorPicker");
                  map.setIndoorEnabled(isEnabled);
                }

              }
              //preferences
              if (params.has("preferences")) {
                JSONObject preferences = params.getJSONObject("preferences");

                if (preferences.has("padding")) {
                  JSONObject padding = preferences.getJSONObject("padding");
                  int left = 0, top = 0, bottom = 0, right = 0;
                  if (padding.has("left")) {
                    left = (int) (padding.getInt("left") * density);
                  }
                  if (padding.has("top")) {
                    top = (int) (padding.getInt("top") * density);
                  }
                  if (padding.has("bottom")) {
                    bottom = (int) (padding.getInt("bottom") * density);
                  }
                  if (padding.has("right")) {
                    right = (int) (padding.getInt("right") * density);
                  }
                  map.setPadding(left, top, right, bottom);
                }

                if (preferences.has("zoom")) {
                  JSONObject zoom = preferences.getJSONObject("zoom");
                  if (zoom.has("minZoom")) {
                    map.setMinZoomPreference((float)zoom.getDouble("minZoom"));
                  }
                  if (zoom.has("maxZoom")) {
                    map.setMaxZoomPreference((float)zoom.getDouble("maxZoom"));
                  }
                }
              }

              // Set event listener
              map.setOnCameraIdleListener(PluginMap.this);
              map.setOnCameraMoveCanceledListener(PluginMap.this);
              map.setOnCameraMoveListener(PluginMap.this);
              map.setOnCameraMoveStartedListener(PluginMap.this);
              map.setOnMapClickListener(PluginMap.this);
              map.setOnMapLongClickListener(PluginMap.this);
              map.setOnMarkerClickListener(PluginMap.this);
              map.setOnMarkerDragListener(PluginMap.this);
              map.setOnMyLocationButtonClickListener(PluginMap.this);
              map.setOnMapLoadedCallback(PluginMap.this);
              map.setOnIndoorStateChangeListener(PluginMap.this);
              map.setOnInfoWindowClickListener(PluginMap.this);
              map.setOnInfoWindowLongClickListener(PluginMap.this);
              map.setOnInfoWindowCloseListener(PluginMap.this);

              //Custom info window
              map.setInfoWindowAdapter(PluginMap.this);


              mapView.onResume();

              if (params.has("controls")) {
                JSONObject controls = params.getJSONObject("controls");

                if (controls.has("myLocationButton")) {
                  final Boolean isEnabled = controls.getBoolean("myLocationButton");
                  cordova.getThreadPool().submit(new Runnable() {
                    @Override
                    public void run() {
                      if (isEnabled) {
                        try {
                          JSONArray args = new JSONArray();
                          args.put(isEnabled);
                          PluginMap.this.setMyLocationEnabled(args, callbackContext);
                        } catch (JSONException e) {
                          e.printStackTrace();
                          callbackContext.error(e.getMessage() + "");
                        }
                      }
                    }
                  });
                }
              }

              // ------------------------------
              // Embed the map if a container is specified.
              // ------------------------------
              if (args.length() == 3) {
                mapDivId = args.getString(2);

                mapCtrl.mPluginLayout.addPluginMap(PluginMap.this);
                PluginMap.this.resizeMap(args, new PluginUtil.MyCallbackContext("dummy-" + map.hashCode(), webView) {
                  @Override
                  public void onResult(PluginResult pluginResult) {

                    if (initCameraBounds != null) {
                      map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                        @Override
                        public void onCameraIdle() {
                          mapView.setVisibility(View.INVISIBLE);
                          PluginMap.this.onCameraIdle();
                          map.setOnCameraIdleListener(PluginMap.this);
                          Handler handler = new Handler();
                          handler.postDelayed(new AdjustInitCamera(params, callbackContext), 500);
                        }
                      });
                    } else {
                      mapView.setVisibility(View.VISIBLE);
                      PluginMap.this.onCameraEvent("camera_end");
                      callbackContext.success();
                    }
                  }
                });
              } else {
                if (initCameraBounds != null) {
                  map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                    @Override
                    public void onCameraIdle() {
                      PluginMap.this.onCameraIdle();
                      map.setOnCameraIdleListener(PluginMap.this);
                      mapView.setVisibility(View.INVISIBLE);
                      Handler handler = new Handler();
                      handler.postDelayed(new AdjustInitCamera(params, callbackContext), 500);
                    }
                  });
                } else {
                  mapView.setVisibility(View.VISIBLE);
                  PluginMap.this.onCameraEvent("camera_end");
                  callbackContext.success();
                  //if (map.getMapType() == GoogleMap.MAP_TYPE_NONE) {
                    PluginMap.this.onMapLoaded();
                  //}
                }
              }
            } catch (Exception e) {
              callbackContext.error(e.getMessage());
            }
          }
        });


      }
    });
  }

  private class AdjustInitCamera implements Runnable {
    private JSONObject mParams;
    private CallbackContext mCallback;
    public AdjustInitCamera(JSONObject params, CallbackContext callbackContext) {
      mParams = params;
      mCallback = callbackContext;
    }
    @Override
    public void run() {

      double CAMERA_PADDING = DEFAULT_CAMERA_PADDING;
      try {
        if (mParams.has("camera")) {
          JSONObject camera = mParams.getJSONObject("camera");
          if (camera.has("padding")) {
            CAMERA_PADDING = camera.getDouble("padding");
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      map.moveCamera(CameraUpdateFactory.newLatLngBounds(initCameraBounds, (int) (CAMERA_PADDING * density)));

      CameraPosition.Builder builder = CameraPosition.builder(map.getCameraPosition());

      try {
        if (mParams.has("camera")) {
          JSONObject camera = mParams.getJSONObject("camera");
          if (camera.has("bearing")) {
            builder.bearing((float) camera.getDouble("bearing"));
          }
          if (camera.has("tilt")) {
            builder.tilt((float) camera.getDouble("tilt"));
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      map.moveCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
      mapView.setVisibility(View.VISIBLE);
      mCallback.success();

      //if (map.getMapType() == GoogleMap.MAP_TYPE_NONE) {
        PluginMap.this.onMapLoaded();
      //}

      //fitBounds(initCameraBounds, CAMERA_PADDING);
    }
  }

  //-----------------------------------
  // Create the instance of class
  //-----------------------------------
  public synchronized void loadPlugin(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final String serviceName = args.getString(0);
    final String pluginName = mapId + "-" + serviceName.toLowerCase();
    //Log.d("PluginMap", "serviceName = " + serviceName + ", pluginName = " + pluginName);

    try {

      if (plugins.containsKey(pluginName)) {
        //Log.d("PluginMap", "--> useCache");
        MyPlugin myPlugin = (MyPlugin) plugins.get(pluginName).plugin;
        myPlugin.create(args, callbackContext);
        return;
      }

      //Log.d("PluginMap", "--> create new instance");
      String className = "plugin.google.maps.Plugin" + serviceName;
      Class pluginCls = Class.forName(className);

      CordovaPlugin plugin = (CordovaPlugin) pluginCls.newInstance();
      PluginEntry pluginEntry = new PluginEntry(pluginName, plugin);
      plugins.put(pluginName, pluginEntry);
      mapCtrl.pluginManager.addService(pluginEntry);

      plugin.privateInitialize(pluginName, cordova, webView, null);

      plugin.initialize(cordova, webView);
      ((MyPluginInterface)plugin).setPluginMap(PluginMap.this);
      MyPlugin myPlugin = (MyPlugin) plugin;
      myPlugin.self = (MyPlugin)plugin;
      myPlugin.CURRENT_PAGE_URL = CURRENT_PAGE_URL;
      myPlugin.create(args, callbackContext);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public void fitBounds(final LatLngBounds cameraBounds, int padding) {
    Builder builder = CameraPosition.builder();
    builder.tilt(map.getCameraPosition().tilt);
    builder.bearing(map.getCameraPosition().bearing);

    // Fit the camera to the cameraBounds with 20px padding.
    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(cameraBounds, padding * (int)density);
    try {
        map.moveCamera(cameraUpdate);
        builder.zoom(map.getCameraPosition().zoom);
        builder.target(map.getCameraPosition().target);
        map.moveCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
    } catch (Exception e) {
        e.printStackTrace();
    }
  }


  //-----------------------------------
  // Create the instance of class
  //-----------------------------------
  @SuppressWarnings("rawtypes")
  public void create(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final String className = args.getString(0);


    try {
      if (plugins.containsKey(className)) {
        PluginEntry pluginEntry = plugins.get(className);
        pluginEntry.plugin.execute("create", args, callbackContext);
        return;
      }

      Class pluginCls = Class.forName("plugin.google.maps.Plugin" + className);

      CordovaPlugin plugin = (CordovaPlugin) pluginCls.newInstance();
      PluginEntry pluginEntry = new PluginEntry(mapId + "-" + className, plugin);
      plugins.put(className, pluginEntry);
      pluginMap = PluginMap.this;
      pluginMap.mapCtrl.pluginManager.addService(pluginEntry);

      plugin.privateInitialize(className, cordova, webView, null);
      plugin.initialize(cordova, webView);
      ((MyPluginInterface)plugin).setPluginMap(PluginMap.this);
      pluginEntry.plugin.execute("create", args, callbackContext);


    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public void resizeMap(JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (mapCtrl.mPluginLayout == null || mapDivId == null) {
      //Log.d("PluginMap", "---> resizeMap / mPluginLayout = null");
      callbackContext.success();
      if (initCameraBounds != null) {
        mainHandler.postDelayed(new Runnable() {
          @Override
          public void run() {
          }
        }, 100);
      }
      return;
    }

    mapCtrl.mPluginLayout.needUpdatePosition = true;

    if (!mapCtrl.mPluginLayout.HTMLNodes.containsKey(mapDivId)) {
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
      mapCtrl.mPluginLayout.HTMLNodes.put(mapDivId, dummyInfo);
    }

    //mapCtrl.mPluginLayout.updateViewPosition(mapId);

    //mapCtrl.mPluginLayout.inValidate();
    callbackContext.success();
  }

  public void setDiv(JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (args.length() == 0) {
      PluginMap.this.mapDivId = null;
      mapCtrl.mPluginLayout.removePluginMap(mapId);
      callbackContext.success();
      return;
    }
    PluginMap.this.mapDivId = args.getString(0);
    mapCtrl.mPluginLayout.addPluginMap(PluginMap.this);
    this.resizeMap(args, callbackContext);
  }

  /**
   * Set clickable of the map
   * @param args Parameters given from JavaScript side
   * @param callbackContext Callback contect for sending back the result.
   * @throws JSONException
   */
  public void setClickable(JSONArray args, CallbackContext callbackContext) throws JSONException {
    boolean clickable = args.getBoolean(0);
    this.isClickable = clickable;
    //mapCtrl.mPluginLayout.setClickable(mapId, clickable);
    callbackContext.success();
  }

  /**
   * Set visibility of the map
   * @param args Parameters given from JavaScript side
   * @param callbackContext Callback contect for sending back the result.
   * @throws JSONException
   */
  public void setVisible(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final boolean visible = args.getBoolean(0);
    this.isVisible = visible;
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (visible) {
          mapView.setVisibility(View.VISIBLE);
        } else {
          mapView.setVisibility(View.INVISIBLE);
        }
        callbackContext.success();
      }
    });
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    //this.remove(null, null);
  }

  /**
   * Destroy the map completely
   * @param args Parameters given from JavaScript side
   * @param callbackContext Callback contect for sending back the result.
   */
  public void remove(JSONArray args, final CallbackContext callbackContext) {
    this.isClickable = false;
    this.isRemoved = true;

    try {
      PluginMap.this.clear(null, new PluginUtil.MyCallbackContext(mapId + "_remove", webView) {

        @Override
        public void onResult(PluginResult pluginResult) {
          cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              mapCtrl.mPluginLayout.removePluginMap(mapId);

              //Log.d("pluginMap", "--> map = " + map);
              if (map != null) {
                try {
                  map.setIndoorEnabled(false);
                  map.setMyLocationEnabled(false);
                  map.setOnPolylineClickListener(null);
                  map.setOnPolygonClickListener(null);
                  map.setOnIndoorStateChangeListener(null);
                  map.setOnCircleClickListener(null);
                  map.setOnGroundOverlayClickListener(null);
                  map.setOnCameraIdleListener(null);
                  map.setOnCameraMoveCanceledListener(null);
                  map.setOnCameraMoveListener(null);
                  map.setOnInfoWindowClickListener(null);
                  map.setOnInfoWindowCloseListener(null);
                  map.setOnMapClickListener(null);
                  map.setOnMapLongClickListener(null);
                  map.setOnMarkerClickListener(null);
                  map.setOnMyLocationButtonClickListener(null);
                  map.setOnMapLoadedCallback(null);
                  map.setOnMarkerDragListener(null);
                } catch (SecurityException e) {
                  e.printStackTrace();
                }
              }
              if (mapView != null) {
                try {
                  mapView.clearAnimation();
                  //mapView.onCancelPendingInputEvents();   // Android 4.2 crashes
                  mapView.onPause();
                  mapView.onDestroy();
                  //Log.d("pluginMap", "--> mapView.onDestroy()");
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
              String[] pluginNames = plugins.keySet().toArray(new String[plugins.size()]);
              PluginEntry pluginEntry;
              for (int i = 0; i < pluginNames.length; i++) {
                pluginEntry = plugins.remove(pluginNames[i]);
                if (pluginEntry == null) {
                  continue;
                }
                pluginEntry.plugin.onDestroy();
                ((MyPlugin)pluginEntry.plugin).map = null;
                ((MyPlugin)pluginEntry.plugin).mapCtrl = null;
                //((MyPlugin)pluginEntry.plugin).pluginMap = null; // Do not clear at here.
                pluginEntry = null;
              }
              //Log.d("pluginMap", "--> mapView = " + mapView);
              projection = null;
              plugins = null;
              map = null;
              mapView = null;
              initCameraBounds = null;
              activity = null;
              mapId = null;
              mapDivId = null;
              activeMarker = null;

              System.gc();
              Runtime.getRuntime().gc();
              if (callbackContext != null) {
                callbackContext.success();
              }
              PluginMap.this.onDestroy();
            }
          });
        }
      });
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }


  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  @Override
  public View getInfoContents(Marker marker) {
    //Log.d(TAG, "--->getInfoContents");
    activeMarker = marker;
    String title = marker.getTitle();
    String snippet = marker.getSnippet();
    if ((title == null) && (snippet == null)) {
      return null;
    }

    String markerTag = (String) marker.getTag();
    String tmp[] = markerTag.split("_");
    String className = tmp[0];
    tmp = markerTag.split("-");
    String markerId = tmp[tmp.length - 1];

    PluginEntry pluginEntry = plugins.get(mapId + "-" + className);
    if (pluginEntry == null) {
      //Log.d(TAG, "---> getInfoContents / marker.title = " + marker.getTitle());
      return null;
    }
    MyPlugin myPlugin = (MyPlugin)pluginEntry.plugin;

    JSONObject properties = null;
    JSONObject styles = null;
    String propertyId = "marker_property_" + markerTag;
    //Log.d(TAG, "---> getInfoContents / propertyId = " + propertyId);

    if (objects.containsKey(propertyId)) {
      properties = (JSONObject) objects.get(propertyId);

      try {
        if (properties.has("styles")) {
            styles = (JSONObject) properties.getJSONObject("styles");
        }
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    if ((marker.getTag() + "").startsWith("markercluster_")){
      this.onClusterEvent("info_open", marker);
    } else {
      this.onMarkerEvent("info_open", marker);
    }

    // Linear layout
    LinearLayout windowLayer = new LinearLayout(activity);
    windowLayer.setPadding(3, 3, 3, 3);
    windowLayer.setOrientation(LinearLayout.VERTICAL);
    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
    layoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER;

    int maxWidth = 0;

    if (styles != null) {
      try {
        int width = 0;
        String widthString = styles.getString("width");

        if (widthString.endsWith("%")) {
          double widthDouble = Double.parseDouble(widthString.replace ("%", ""));

          width = (int)((double)mapView.getWidth() * (widthDouble / 100));
        } else if (PluginUtil.isNumeric(widthString)) {
          double widthDouble = Double.parseDouble(widthString);

          if (widthDouble <= 1.0) {	// for percentage values (e.g. 0.5 = 50%).
            width = (int)((double)mapView.getWidth() * (widthDouble));
          } else {
            width = (int)widthDouble;
          }
        }

        if (width > 0) {
          layoutParams.width = width;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

      try {
        String widthString = styles.getString("maxWidth");

        if (widthString.endsWith("%")) {
          double widthDouble = Double.parseDouble(widthString.replace ("%", ""));

          maxWidth = (int)((double)mapView.getWidth() * (widthDouble / 100));

          // make sure to take padding into account.
          maxWidth -= (windowLayer.getPaddingLeft() + windowLayer.getPaddingRight());
        } else if (PluginUtil.isNumeric(widthString)) {
          double widthDouble = Double.parseDouble(widthString);

          if (widthDouble <= 1.0) {	// for percentage values (e.g. 0.5 = 50%).
            maxWidth = (int)((double)mapView.getWidth() * (widthDouble));
          } else {
            maxWidth = (int)widthDouble;
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

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

      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (title != null) {
      if (title.contains("data:image/") && title.contains(";base64,")) {
        tmp = title.split(",");
        Bitmap image = PluginUtil.getBitmapFromBase64encodedImage(tmp[1]);
        image = PluginUtil.scaleBitmapForDevice(image);
        ImageView imageView = new ImageView(this.activity);
        imageView.setImageBitmap(image);

        if (maxWidth > 0) {
          imageView.setMaxWidth(maxWidth);
          imageView.setAdjustViewBounds(true);
        }

        windowLayer.addView(imageView);
      } else {
        TextView textView = new TextView(this.activity);
        textView.setText(title);
        textView.setSingleLine(false);

        int titleColor = Color.BLACK;
        if (styles != null && styles.has("color")) {
          try {
            titleColor = PluginUtil.parsePluginColor(styles.getJSONArray("color"));
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }
        textView.setTextColor(titleColor);
        textView.setGravity(gravity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
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
          } catch (JSONException e) {
            e.printStackTrace();
          }
          try {
            if ("bold".equals(styles.getString("font-weight"))) {
              fontStyle = fontStyle | Typeface.BOLD;
            }
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }
        textView.setTypeface(Typeface.DEFAULT, fontStyle);

        if (maxWidth > 0) {
          textView.setMaxWidth(maxWidth);
        }

        windowLayer.addView(textView);
      }
    }
    if (snippet != null) {
      //snippet = snippet.replaceAll("\n", "");
      TextView textView2 = new TextView(this.activity);
      textView2.setText(snippet);
      textView2.setTextColor(Color.GRAY);
      textView2.setTextSize((textView2.getTextSize() / 6 * 5) / density);
      textView2.setGravity(gravity);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        textView2.setTextAlignment(textAlignment);
      }

      if (maxWidth > 0) {
        textView2.setMaxWidth(maxWidth);
      }

      windowLayer.addView(textView2);
    }

    return windowLayer;
  }

  @Override
  public View getInfoWindow(Marker marker) {
    //Log.d(TAG, "--->getInfoWindow");
    activeMarker = marker;

    JSONObject properties = null;

    String markerTag = (String) marker.getTag();
    String tmp[] = markerTag.split("_");
    String className = tmp[0];
    tmp = markerTag.split("-");
    String markerId = tmp[tmp.length - 1];

    String propertyId = "marker_property_" + markerTag;

    //Log.e(TAG, "---> getInfoWindow / propertyId = " + propertyId);
    //Log.e(TAG, "---> getInfoWindow / pluginEntryId = " + mapId + "-" + className);
    PluginEntry pluginEntry = plugins.get(mapId + "-" + className);
    if (pluginEntry == null) {
      Log.e(TAG, "---> getInfoWindow / pluginEntry is null");
      return null;
    }
    MyPlugin myPlugin = (MyPlugin)pluginEntry.plugin;

    if (objects.containsKey(propertyId)) {
      properties = (JSONObject) objects.get(propertyId);
      try {
        if (marker.getTitle() == null && marker.getSnippet() == null) {

          syncInfoWndPosition();

          if ((marker.getTag() + "").startsWith("markercluster_")){
            this.onClusterEvent("info_open", marker);
          } else {
            this.onMarkerEvent("info_open", marker);
          }

          int resId = PluginUtil.getAppResource(cordova.getActivity(), "dummy_infowindow", "layout");
          return cordova.getActivity().getLayoutInflater().inflate(resId, null);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      Log.e(TAG, "---> getInfoWindow / can not find the property");
    }
    return null;
  }


  /**
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setOptions(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final AsyncTask<Void, Void, AsyncSetOptionsResult> task = new AsyncTask<Void, Void, AsyncSetOptionsResult>() {
      private Exception mException = null;
      @Override
      protected AsyncSetOptionsResult doInBackground(Void... Void) {
        AsyncSetOptionsResult results = new AsyncSetOptionsResult();
        results.cameraPadding = DEFAULT_CAMERA_PADDING;

        try {
          JSONObject params = args.getJSONObject(0);

          if (params.has("styles")) {
            results.styles = params.getString("styles");
          } else {
            // map type
            results.MAP_TYPE_ID = -1;
            if (!params.has("styles") && params.has("mapType")) {
              String typeStr = params.getString("mapType");
              results.MAP_TYPE_ID = typeStr.equals("MAP_TYPE_NORMAL") ? GoogleMap.MAP_TYPE_NORMAL : results.MAP_TYPE_ID;
              results.MAP_TYPE_ID = typeStr.equals("MAP_TYPE_HYBRID") ? GoogleMap.MAP_TYPE_HYBRID : results.MAP_TYPE_ID;
              results.MAP_TYPE_ID = typeStr.equals("MAP_TYPE_SATELLITE") ? GoogleMap.MAP_TYPE_SATELLITE : results.MAP_TYPE_ID;
              results.MAP_TYPE_ID = typeStr.equals("MAP_TYPE_TERRAIN") ? GoogleMap.MAP_TYPE_TERRAIN : results.MAP_TYPE_ID;
              results.MAP_TYPE_ID = typeStr.equals("MAP_TYPE_NONE") ? GoogleMap.MAP_TYPE_NONE : results.MAP_TYPE_ID;
            }
          }


          // move the camera position
          if (params.has("camera")) {
            LatLngBounds cameraBounds = null;
            JSONObject camera = params.getJSONObject("camera");
            Builder builder = CameraPosition.builder();
            if (camera.has("bearing")) {
              builder.bearing((float) camera.getDouble("bearing"));
            }
            if (camera.has("latLng")) {
              JSONObject latLng = camera.getJSONObject("latLng");
              builder.target(new LatLng(latLng.getDouble("lat"), latLng.getDouble("lng")));
            }

            if (camera.has("padding")) {
              results.cameraPadding = camera.getDouble("padding");
            }

            if (camera.has("target")) {
              Object target = camera.get("target");
              @SuppressWarnings("rawtypes")
              Class targetClass = target.getClass();
              if ("org.json.JSONArray".equals(targetClass.getName())) {
                JSONArray points = camera.getJSONArray("target");
                cameraBounds = PluginUtil.JSONArray2LatLngBounds(points);
                builder.target(cameraBounds.getCenter());

              } else {
                JSONObject latLng = camera.getJSONObject("target");
                builder.target(new LatLng(latLng.getDouble("lat"), latLng.getDouble("lng")));
              }
            }
            if (camera.has("tilt")) {
              builder.tilt((float) camera.getDouble("tilt"));
            }
            if (camera.has("zoom")) {
              builder.zoom((float) camera.getDouble("zoom"));
            }
            results.cameraPosition = builder.build();
            results.cameraBounds = cameraBounds;

          }

        } catch (Exception e) {
          mException = e;
          this.cancel(true);
          return null;
        }

        return results;
      }

      @Override
      public void onCancelled() {
        if (mException != null) {
          mException.printStackTrace();
          callbackContext.error("" + mException.getMessage());
        } else {
          callbackContext.error("");
        }

      }

      @Override
      public void onPostExecute(AsyncSetOptionsResult results) {
        if (results.cameraPosition != null) {
          try {
              map.moveCamera(CameraUpdateFactory.newCameraPosition(results.cameraPosition));
          } catch (Exception e) {
              e.printStackTrace();
          }
          if (results.cameraBounds != null) {
            fitBounds(results.cameraBounds, (int)(results.cameraPadding * density));
          }
        }

        //styles
        if (results.styles != null) {
          MapStyleOptions styleOptions = new MapStyleOptions(results.styles);
          map.setMapStyle(styleOptions);
          map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        } else if (results.MAP_TYPE_ID != -1) {
          map.setMapType(results.MAP_TYPE_ID);
        }


        JSONObject params = null;
        try {
          params = args.getJSONObject(0);
          UiSettings settings = map.getUiSettings();

          //preferences
          if (params.has("preferences")) {
            JSONObject preferences = params.getJSONObject("preferences");

            if (preferences.has("padding")) {
              JSONObject padding = preferences.getJSONObject("padding");
              int left = 0, top = 0, bottom = 0, right = 0;
              if (padding.has("left")) {
                left = (int) (padding.getInt("left") * density);
              }
              if (padding.has("top")) {
                top = (int) (padding.getInt("top") * density);
              }
              if (padding.has("bottom")) {
                bottom = (int) (padding.getInt("bottom") * density);
              }
              if (padding.has("right")) {
                right = (int) (padding.getInt("right") * density);
              }
              map.setPadding(left, top, right, bottom);
            }

            if (preferences.has("zoom")) {
              JSONObject zoom = preferences.getJSONObject("zoom");
              if (zoom.has("minZoom")) {
                map.setMinZoomPreference((float)zoom.getDouble("minZoom"));
              }
              if (zoom.has("maxZoom")) {
                map.setMaxZoomPreference((float)zoom.getDouble("maxZoom"));
              }
            }
          }

          //gestures
          if (params.has("gestures")) {
            JSONObject gestures = params.getJSONObject("gestures");

            if (gestures.has("tilt")) {
              settings.setTiltGesturesEnabled(gestures.getBoolean("tilt"));
            }
            if (gestures.has("scroll")) {
              settings.setScrollGesturesEnabled(gestures.getBoolean("scroll"));
            }
            if (gestures.has("rotate")) {
              settings.setRotateGesturesEnabled(gestures.getBoolean("rotate"));
            }
            if (gestures.has("zoom")) {
              settings.setZoomGesturesEnabled(gestures.getBoolean("zoom"));
            }
          }

          //controls
          if (params.has("controls")) {
            JSONObject controls = params.getJSONObject("controls");

            if (controls.has("compass")) {
              settings.setCompassEnabled(controls.getBoolean("compass"));
            }
            if (controls.has("zoom")) {
              settings.setZoomControlsEnabled(controls.getBoolean("zoom"));
            }
            if (controls.has("indoorPicker")) {
              settings.setIndoorLevelPickerEnabled(controls.getBoolean("indoorPicker"));
            }
            if (controls.has("mapToolbar")) {
              settings.setMapToolbarEnabled(controls.getBoolean("mapToolbar"));
            }
            if (controls.has("myLocationButton")) {
              boolean isEnabled = controls.getBoolean("myLocationButton");
              settings.setMyLocationButtonEnabled(isEnabled);

              JSONArray args = new JSONArray();
              args.put(isEnabled);
              PluginMap.this.setMyLocationEnabled(args, callbackContext);
            } else {
              callbackContext.success();
            }
          } else {
            callbackContext.success();
          }

        } catch (JSONException e) {
          e.printStackTrace();
          callbackContext.error("" + e.getMessage());
        }
      }
    };
    task.execute();

  }

  public void getFocusedBuilding(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        IndoorBuilding focusedBuilding = map.getFocusedBuilding();
        if (focusedBuilding != null) {
          JSONObject result = PluginUtil.convertIndoorBuildingToJson(focusedBuilding);
          callbackContext.success(result);
        } else {
          callbackContext.success(-1);
        }
      }
    });
  }

  /**
   * Set center location of the marker
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setCameraTarget(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    double lat = args.getDouble(0);
    double lng = args.getDouble(1);

    LatLng latLng = new LatLng(lat, lng);
    final CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(latLng);
    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        myMoveCamera(cameraUpdate, callbackContext);
      }
    });
  }

  /**
   * Set angle of the map view
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setCameraTilt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float tilt = (float) args.getDouble(0);

    if (tilt > 0 && tilt <= 90) {
      final float finalTilt = tilt;
      this.activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          CameraPosition currentPos = map.getCameraPosition();
          CameraPosition newPosition = new CameraPosition.Builder()
              .target(currentPos.target).bearing(currentPos.bearing)
              .zoom(currentPos.zoom).tilt(finalTilt).build();
          myMoveCamera(newPosition, callbackContext);
        }
      });
    } else {
      callbackContext.error("Invalid tilt angle(" + tilt + ")");
    }
  }

  public void setCameraBearing(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final float bearing = (float) args.getDouble(0);

    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        CameraPosition currentPos = map.getCameraPosition();
        CameraPosition newPosition = new CameraPosition.Builder()
          .target(currentPos.target).bearing(bearing)
          .zoom(currentPos.zoom).tilt(currentPos.tilt).build();
        myMoveCamera(newPosition, callbackContext);
      }
    });
  }

  /**
   * Move the camera with animation
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void animateCamera(JSONArray args, CallbackContext callbackContext) throws JSONException {
    this.updateCameraPosition("animateCamera", args, callbackContext);
  }

  /**
   * Move the camera without animation
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void moveCamera(JSONArray args, CallbackContext callbackContext) throws JSONException {
    this.updateCameraPosition("moveCamera", args, callbackContext);
  }


  /**
   * move the camera
   * @param action
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void updateCameraPosition(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (this.isRemoved) {
      return;
    }
    final JSONObject cameraPos = args.getJSONObject(0);

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {

        final CameraPosition.Builder builder = CameraPosition.builder(map.getCameraPosition());

        AsyncTask<Void, Void, AsyncUpdateCameraPositionResult> createCameraUpdate = new AsyncTask<Void, Void, AsyncUpdateCameraPositionResult>() {
          private Exception mException = null;

          @Override
          protected AsyncUpdateCameraPositionResult doInBackground(Void... voids) {
            AsyncUpdateCameraPositionResult result = new AsyncUpdateCameraPositionResult();
            if (isRemoved) {
              this.cancel(true);
              return null;
            }

            try {

              result.durationMS = 4000;
              result.cameraPadding = DEFAULT_CAMERA_PADDING;
              if (cameraPos.has("tilt")) {
                builder.tilt((float) cameraPos.getDouble("tilt"));
              }
              if (cameraPos.has("bearing")) {
                builder.bearing((float) cameraPos.getDouble("bearing"));
              }
              if (cameraPos.has("zoom")) {
                builder.zoom((float) cameraPos.getDouble("zoom"));
              }
              if (cameraPos.has("duration")) {
                result.durationMS = cameraPos.getInt("duration");
              }
              if (cameraPos.has("padding")) {
                result.cameraPadding = cameraPos.getDouble("padding");
              }

              if (!cameraPos.has("target")) {
                return result;
              }

              //------------------------
              // Create a cameraUpdate
              //------------------------
              result.cameraUpdate = null;
              result.cameraBounds = null;
              CameraPosition newPosition;
              Object target = cameraPos.get("target");
              @SuppressWarnings("rawtypes")
              Class targetClass = target.getClass();
              JSONObject latLng;
              if ("org.json.JSONArray".equals(targetClass.getName())) {
                JSONArray points = cameraPos.getJSONArray("target");
                result.cameraBounds = PluginUtil.JSONArray2LatLngBounds(points);
                result.cameraUpdate = CameraUpdateFactory.newLatLngBounds(result.cameraBounds, (int)(result.cameraPadding * density));
              } else {
                latLng = cameraPos.getJSONObject("target");
                builder.target(new LatLng(latLng.getDouble("lat"), latLng.getDouble("lng")));
                newPosition = builder.build();
                result.cameraUpdate = CameraUpdateFactory.newCameraPosition(newPosition);
              }
            } catch (Exception e) {
              mException = e;
              e.printStackTrace();
              this.cancel(true);
              return null;
            }

            return result;
          }

          @Override
          public void onCancelled() {
            if (mException != null) {
              mException.printStackTrace();
            }
            callbackContext.error(mException != null ? mException.getMessage() + "" : "");
          }
          @Override
          public void onCancelled(AsyncUpdateCameraPositionResult AsyncUpdateCameraPositionResult) {
            if (mException != null) {
              mException.printStackTrace();
            }
            callbackContext.error(mException != null ? mException.getMessage() + "" : "");
          }

          @Override
          public void onPostExecute(AsyncUpdateCameraPositionResult AsyncUpdateCameraPositionResult) {
            if (isRemoved) {
              return;
            }


            if (AsyncUpdateCameraPositionResult.cameraUpdate == null) {
              CameraPosition.Builder builder = CameraPosition.builder(map.getCameraPosition());
              builder.target(map.getCameraPosition().target);
              AsyncUpdateCameraPositionResult.cameraUpdate = CameraUpdateFactory.newCameraPosition(builder.build());
            }

            final AsyncUpdateCameraPositionResult finalCameraPosition = AsyncUpdateCameraPositionResult;
            PluginUtil.MyCallbackContext myCallback = new PluginUtil.MyCallbackContext("moveCamera", webView) {
              @Override
              public void onResult(final PluginResult pluginResult) {
                if (finalCameraPosition.cameraBounds != null && ANIMATE_CAMERA_DONE.equals(pluginResult.getStrMessage())) {


                  final Builder builder = CameraPosition.builder(map.getCameraPosition());
                  if (cameraPos.has("tilt")) {
                    try {
                      builder.tilt((float) cameraPos.getDouble("tilt"));
                    } catch (JSONException e) {
                      e.printStackTrace();
                    }
                  }
                  if (cameraPos.has("bearing")) {
                    try {
                      builder.bearing((float) cameraPos.getDouble("bearing"));
                    } catch (JSONException e) {
                      e.printStackTrace();
                    }
                  }

                  CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(finalCameraPosition.cameraBounds, (int)(finalCameraPosition.cameraPadding * density));
                  try {
                    map.moveCamera(cameraUpdate);
                  } catch (Exception e) {
                    e.printStackTrace();
                  }
                  map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                    @Override
                    public void onCameraIdle() {
                      PluginMap.this.onCameraIdle();
                      map.setOnCameraIdleListener(PluginMap.this);
                      builder.zoom(map.getCameraPosition().zoom);
                      builder.target(map.getCameraPosition().target);
                      map.moveCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
                    }
                  });
                } else {
                  final Builder builder = CameraPosition.builder(map.getCameraPosition());
                  if (cameraPos.has("tilt")) {
                    try {
                      builder.tilt((float) cameraPos.getDouble("tilt"));
                    } catch (JSONException e) {
                      e.printStackTrace();
                    }
                  }
                  if (cameraPos.has("bearing")) {
                    try {
                      builder.bearing((float) cameraPos.getDouble("bearing"));
                    } catch (JSONException e) {
                      e.printStackTrace();
                    }
                  }

                  try {
                    map.moveCamera(finalCameraPosition.cameraUpdate);
                  } catch (Exception e) {
                    e.printStackTrace();
                  }

                  builder.zoom(map.getCameraPosition().zoom);
                  builder.target(map.getCameraPosition().target);

                  map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                    @Override
                    public void onCameraIdle() {
                      PluginMap.this.onCameraIdle();
                      map.setOnCameraIdleListener(PluginMap.this);
                      builder.zoom(map.getCameraPosition().zoom);
                      builder.target(map.getCameraPosition().target);
                      map.moveCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
                    }
                  });
                }
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
              }
            };
            if (action.equals("moveCamera")) {
              myMoveCamera(AsyncUpdateCameraPositionResult.cameraUpdate, myCallback);
            } else {
              myAnimateCamera(mapId, AsyncUpdateCameraPositionResult.cameraUpdate, AsyncUpdateCameraPositionResult.durationMS, myCallback);
            }

          }
        };
        createCameraUpdate.execute();

      }
    });




  }

  /**
   * Set zoom of the map
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setCameraZoom(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final Long zoom = args.getLong(0);
    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        myMoveCamera(CameraUpdateFactory.zoomTo(zoom), callbackContext);
      }
    });
  }

  /**
   * Pan by the specified pixel
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void panBy(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    int x = args.getInt(0);
    int y = args.getInt(1);
    float xPixel = -x * density;
    float yPixel = -y * density;
    final CameraUpdate cameraUpdate = CameraUpdateFactory.scrollBy(xPixel, yPixel);

    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        map.animateCamera(cameraUpdate);
        callbackContext.success();
      }
    });
  }

  /**
   * Move the camera of the map
   * @param cameraPosition
   * @param callbackContext
   */
  public void myMoveCamera(CameraPosition cameraPosition, final CallbackContext callbackContext) {
    final CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        myMoveCamera(cameraUpdate, callbackContext);
      }
    });
  }

  /**
   * Move the camera of the map
   * @param cameraUpdate
   * @param callbackContext
   */
  public void myMoveCamera(CameraUpdate cameraUpdate, CallbackContext callbackContext) {
    try {
        map.moveCamera(cameraUpdate);
    } catch (Exception e) {
        e.printStackTrace();
    }
    callbackContext.success();
  }

  public void onRequestPermissionResult(int requestCode, String[] permissions,
                                        int[] grantResults) throws JSONException {
    synchronized (CordovaGoogleMaps.semaphore) {
      CordovaGoogleMaps.semaphore.notify();
    }
  }

  /**
   * Enable MyLocation feature if set true
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setMyLocationEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    // Request geolocation permission.
    boolean locationPermission = cordova.hasPermission("android.permission.ACCESS_COARSE_LOCATION");
    Log.d(TAG, "---> setMyLocationEnabled, hasPermission =  " + locationPermission);

    if (!locationPermission) {
      //_saveArgs = args;
      //_saveCallbackContext = callbackContext;
      cordova.requestPermissions(this, callbackContext.hashCode(), new String[]{"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"});
      synchronized (CordovaGoogleMaps.semaphore) {
        try {
          CordovaGoogleMaps.semaphore.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      if (!cordova.hasPermission("android.permission.ACCESS_COARSE_LOCATION")) {
        callbackContext.error("Geolocation permission request was denied.");
        return;
      }

    }

    final Boolean isEnabled = args.getBoolean(0);
    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          map.setMyLocationEnabled(isEnabled);
        } catch (SecurityException e) {
          e.printStackTrace();
        }
        map.getUiSettings().setMyLocationButtonEnabled(isEnabled);
        callbackContext.success();
      }
    });
  }

  /**
   * Clear all markups
   * @param args Parameters given from JavaScript side
   * @param callbackContext Callback contect for sending back the result.
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  public void clear(JSONArray args, final CallbackContext callbackContext) throws JSONException {

    cordova.getThreadPool().submit(new Runnable() {
      @Override
      public void run() {

        Set<String> pluginNames = plugins.keySet();
        Iterator<String> iterator = pluginNames.iterator();
        String pluginName;
        PluginEntry pluginEntry;
        while(iterator.hasNext()) {
          pluginName = iterator.next();
          if (!"Map".equals(pluginName)) {
            pluginEntry = plugins.get(pluginName);
            ((MyPlugin) pluginEntry.plugin).clear();
          }
        }

        activity.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            boolean isSuccess = false;
            while (!isSuccess) {
              try {
                map.clear();
                isSuccess = true;
              } catch (Exception e) {
                e.printStackTrace();
                isSuccess = false;
              }
            }
            if (callbackContext != null) {
              callbackContext.success();
            }
          }
        });

      }
    });
  }

  /**
   * Enable Indoor map feature if set true
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setIndoorEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final Boolean isEnabled = args.getBoolean(0);
    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        map.setIndoorEnabled(isEnabled);
        callbackContext.success();
      }
    });
  }

  /**
   * Enable the traffic layer if set true
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setTrafficEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final Boolean isEnabled = args.getBoolean(0);
    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        map.setTrafficEnabled(isEnabled);
        callbackContext.success();
      }
    });
  }

  /**
   * Enable the compass if set true
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setCompassEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final Boolean isEnabled = args.getBoolean(0);
    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setCompassEnabled(isEnabled);
        callbackContext.success();
      }
    });
  }

  /**
   * Change the map type id of the map
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setMapTypeId(JSONArray args, final CallbackContext callbackContext) throws JSONException {

    int mapTypeId = -1;
    String typeStr = args.getString(0);
    mapTypeId = typeStr.equals("MAP_TYPE_NORMAL") ? GoogleMap.MAP_TYPE_NORMAL : mapTypeId;
    mapTypeId = typeStr.equals("MAP_TYPE_HYBRID") ? GoogleMap.MAP_TYPE_HYBRID : mapTypeId;
    mapTypeId = typeStr.equals("MAP_TYPE_SATELLITE") ? GoogleMap.MAP_TYPE_SATELLITE : mapTypeId;
    mapTypeId = typeStr.equals("MAP_TYPE_TERRAIN") ? GoogleMap.MAP_TYPE_TERRAIN : mapTypeId;
    mapTypeId = typeStr.equals("MAP_TYPE_NONE") ? GoogleMap.MAP_TYPE_NONE : mapTypeId;

    if (mapTypeId == -1) {
      callbackContext.error("Unknown MapTypeID is specified:" + typeStr);
      return;
    }

    final int myMapTypeId = mapTypeId;
    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        map.setMapType(myMapTypeId);
        callbackContext.success();
      }
    });
  }


  /**
   * Move the camera of the map
   * @param cameraUpdate
   * @param durationMS
   * @param callbackContext
   */
  public void myAnimateCamera(final String mapId, final CameraUpdate cameraUpdate, final int durationMS, final CallbackContext callbackContext) {
    final GoogleMap.CancelableCallback callback = new GoogleMap.CancelableCallback() {
      @Override
      public void onFinish() {
        callbackContext.success(ANIMATE_CAMERA_DONE);
      }

      @Override
      public void onCancel() {
        callbackContext.success(ANIMATE_CAMERA_CANCELED);
      }
    };

    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (durationMS > 0) {
          map.animateCamera(cameraUpdate, durationMS, callback);
        } else {
          map.animateCamera(cameraUpdate, callback);
        }
      }
    });
  }


  /**
   * Return the current position of the camera
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void getCameraPosition(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        CameraPosition camera = map.getCameraPosition();
        JSONObject json = new JSONObject();
        JSONObject latlng = new JSONObject();
        try {
          latlng.put("lat", camera.target.latitude);
          latlng.put("lng", camera.target.longitude);
          json.put("target", latlng);
          json.put("zoom", camera.zoom);
          json.put("tilt", camera.tilt);
          json.put("bearing", camera.bearing);
          json.put("hashCode", camera.hashCode());

          callbackContext.success(json);
        } catch (JSONException e) {
          e.printStackTrace();
          callbackContext.error(e.getMessage() + "");
        }
      }
    });
  }

  /**
   * Return the image data encoded with base64
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void toDataURL(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    JSONObject params = args.getJSONObject(0);
    boolean uncompress = false;
    if (params.has("uncompress")) {
      uncompress = params.getBoolean("uncompress");
    }
    final boolean finalUncompress = uncompress;

    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {

        map.snapshot(new GoogleMap.SnapshotReadyCallback() {

          @Override
          public void onSnapshotReady(final Bitmap image) {

            AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
              @Override
              protected String doInBackground(Void... voids) {
                Bitmap image2 = image;
                if (!finalUncompress) {
                  float density = Resources.getSystem().getDisplayMetrics().density;
                  image2 = PluginUtil.resizeBitmap(image,
                      (int) (image2.getWidth() / density),
                      (int) (image2.getHeight() / density));
                }
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                image2.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                byte[] byteArray = outputStream.toByteArray();
                return "data:image/png;base64," +
                    Base64.encodeToString(byteArray, Base64.NO_WRAP);
              }

              @Override
              public void onPostExecute(String imageEncoded) {
                callbackContext.success(imageEncoded);
              }
            };
            task.execute();
          }
        });
      }
    });

  }
  public void fromLatLngToPoint(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    double lat, lng;
    lat = args.getDouble(0);
    lng = args.getDouble(1);
    final LatLng latLng = new LatLng(lat, lng);
    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        projection = map.getProjection();
        Point point = projection.toScreenLocation(latLng);
        try {
          JSONArray pointJSON = new JSONArray();
          pointJSON.put(point.x / density);
          pointJSON.put(point.y / density);
          callbackContext.success(pointJSON);
        } catch (JSONException e) {
          e.printStackTrace();
          callbackContext.error(e.getMessage() + "");
        }
      }
    });
  }

  public void fromPointToLatLng(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    double pointX, pointY;
    pointX = args.getDouble(0);
    pointY = args.getDouble(1);
    final Point point = new Point();
    point.x = (int)(pointX * density);
    point.y = (int)(pointY * density);
    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {

        projection = map.getProjection();
        LatLng latlng = projection.fromScreenLocation(point);
        try {
          JSONArray pointJSON = new JSONArray();
          pointJSON.put(latlng.latitude);
          pointJSON.put(latlng.longitude);
          callbackContext.success(pointJSON);
        } catch (JSONException e) {
          e.printStackTrace();
          callbackContext.error(e.getMessage() + "");
        }
      }
    });
  }



  /**
   * Sets the preference for whether all gestures should be enabled or disabled.
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setAllGesturesEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final Boolean isEnabled = args.getBoolean(0);
    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setAllGesturesEnabled(isEnabled);
        callbackContext.success();
      }
    });
  }

  /**
   * Sets padding of the map
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setPadding(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    JSONObject padding = args.getJSONObject(0);
    final int left = (int)(padding.getInt("left") * density);
    final int top = (int)(padding.getInt("top") * density);
    final int bottom = (int)(padding.getInt("bottom") * density);
    final int right = (int)(padding.getInt("right") * density);
    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        map.setPadding(left, top, right, bottom);
        callbackContext.success();
      }
    });
  }

  /**
   * update the active marker (for internal use)
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setActiveMarkerId(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final String id = args.getString(0);

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Marker marker = (Marker) objects.get(id);
        if (marker != null) {
          activeMarker = marker;
        }
        callbackContext.success();
      }
    });
  }


  @Override
  public boolean onMarkerClick(Marker marker) {
    //Log.d(TAG, "---> onMarkerClick / marker.tag = " + marker.getTag());

    JSONObject properties = null;
    String clusterId_markerId = marker.getTag() + "";
    if (clusterId_markerId.contains("markercluster_")) {
      if (clusterId_markerId.contains("-marker_")) {
        activeMarker = marker;
        this.onClusterEvent("marker_click", activeMarker);
      } else {
        if (activeMarker != null) {
          this.onMarkerEvent("info_close", activeMarker);
        }
      }
      this.onClusterEvent("cluster_click", marker);
    } else {
      webView.loadUrl("javascript:if(window.cordova){cordova.fireDocumentEvent('plugin_touch', {});}");
      this.onMarkerEvent("marker_click", marker);
      activeMarker = marker;
    }

    String tmp[] = clusterId_markerId.split("_");
    String className = tmp[0];

    PluginEntry pluginEntry = plugins.get(mapId + "-" + className);
    if (pluginEntry == null) {
      return true;
    }
    MyPlugin myPlugin = (MyPlugin)pluginEntry.plugin;
    String propertyId = "marker_property_" + clusterId_markerId;
    //Log.d(TAG, "---> onMarkerClick / propertyId = " + propertyId);
    if (objects.containsKey(propertyId)) {
      properties = (JSONObject) objects.get(propertyId);
      if (properties.has("disableAutoPan")) {
        boolean disableAutoPan = false;
        try {
          disableAutoPan = properties.getBoolean("disableAutoPan");
        } catch (JSONException e) {
          e.printStackTrace();
        }
        if (disableAutoPan) {
          marker.showInfoWindow();
          return true;
        } else {
          marker.showInfoWindow();
          return false;
        }
      }
    }

    marker.showInfoWindow();
    return true;
    //return false;
  }

  @Override
  public void onInfoWindowClick(Marker marker) {
    activeMarker = marker;
    syncInfoWndPosition();
    String markerTag = marker.getTag() + "";
    if (markerTag.startsWith("markercluster_")){
      this.onClusterEvent("info_click", marker);
    } else {
      this.onMarkerEvent("info_click", marker);
    }
  }

  @Override
  public void onMarkerDrag(Marker marker) {
    if (marker.equals(activeMarker)) {
      syncInfoWndPosition();
    }
    if ((marker.getTag() + "").startsWith("markercluster_")){
      this.onClusterEvent("marker_drag", marker);
    } else {
      this.onMarkerEvent("marker_drag", marker);
    }
  }

  @Override
  public void onMarkerDragEnd(Marker marker) {
    if (marker.equals(activeMarker)) {
      syncInfoWndPosition();
    }
    if ((marker.getTag() + "").startsWith("markercluster_")){
      this.onClusterEvent("marker_drag_end", marker);
    } else {
      this.onMarkerEvent("marker_drag_end", marker);
    }
  }

  @Override
  public void onMarkerDragStart(Marker marker) {
    if (marker.equals(activeMarker)) {
      syncInfoWndPosition();
    }
    if ((marker.getTag() + "").startsWith("markercluster_")){
      this.onClusterEvent("marker_drag_start", marker);
    } else {
      this.onMarkerEvent("marker_drag_start", marker);
    }
  }

  @Override
  public void onInfoWindowLongClick(Marker marker) {
    activeMarker = marker;
    syncInfoWndPosition();
    if ((marker.getTag() + "").startsWith("markercluster_")){
      this.onClusterEvent("info_long_click", marker);
    } else {
      this.onMarkerEvent("info_long_click", marker);
    }
  }

  @Override
  public void onInfoWindowClose(Marker marker) {
    boolean useHtmlInfoWnd = marker.getTitle() == null &&
                             marker.getSnippet() == null;
    if (useHtmlInfoWnd) {
      String markerTag = marker.getTag() + "";
      if (markerTag.startsWith("markercluster_")){
        if (markerTag.contains("-marker_")) {
          this.onClusterEvent("info_close", marker);
        }
      } else {
        this.onMarkerEvent("info_close", marker);
      }
    }
    //activeMarker = null; // <-- This causes HTMLinfoWindow is not able to close when you tap on the map.
  }

  @Override
  public void onMapLoaded() {
    this.onCameraEvent("camera_end");
  }



  /********************************************************
   * Callbacks
   ********************************************************/

  /**
   * Notify marker event to JS
   * @param eventName
   * @param marker
   */
  public void onMarkerEvent(String eventName, Marker marker) {
    if (marker.getTag() == null) {
      return;
    }
    LatLng latLng = marker.getPosition();

    String markerTag = (String) marker.getTag();
    String tmp[] = markerTag.split("_");
    tmp = markerTag.split("-");
    String markerId = tmp[tmp.length - 1];
    String js = String.format(Locale.ENGLISH, "javascript:if(window.cordova){cordova.fireDocumentEvent('%s', {evtName: '%s', callback:'_onMarkerEvent', args:['%s', new plugin.google.maps.LatLng(%f, %f)]});}",
          mapId, eventName, markerId, latLng.latitude, latLng.longitude);
    jsCallback(js);
  }
  public void onClusterEvent(String eventName, Marker marker) {
    if (marker.getTag() == null) {
      return;
    }
    LatLng latLng = marker.getPosition();

    String markerTag = (String) marker.getTag();
    String tmp[] = markerTag.split("-");
    String clusterId = tmp[0];
    String markerId = tmp[1];
    String js = String.format(Locale.ENGLISH, "javascript:if(window.cordova){cordova.fireDocumentEvent('%s', {evtName: '%s', callback:'_onClusterEvent', args:['%s', '%s', new plugin.google.maps.LatLng(%f, %f)]});}",
          mapId, eventName, clusterId, markerId, latLng.latitude, latLng.longitude);
    jsCallback(js);
  }
  public void syncInfoWndPosition() {
    if (activeMarker == null) {
      Log.d(TAG, "--->no active marker");
      return;
    }
    LatLng latLng = activeMarker.getPosition();
    Point point = projection.toScreenLocation(latLng);

    String js = String.format(Locale.ENGLISH, "javascript:if(window.cordova){cordova.fireDocumentEvent('%s', {evtName: 'syncPosition', callback:'_onSyncInfoWndPosition', args:[{'x': %d, 'y': %d}]});}",
        mapId, (int)(point.x / density), (int)(point.y / density));
    jsCallback(js);
  }

  public void onOverlayEvent(String eventName, String overlayId, LatLng point) {
    String js = String.format(Locale.ENGLISH, "javascript:if(window.cordova){cordova.fireDocumentEvent('%s', {evtName: '%s', callback:'_onOverlayEvent', args:['%s', new plugin.google.maps.LatLng(%f, %f)]});}",
        mapId, eventName, overlayId, point.latitude, point.longitude);
    jsCallback(js);
  }
  public void onPolylineClick(Polyline polyline, LatLng point) {
    String overlayId = "polyline_" + polyline.getId();
    this.onOverlayEvent("polyline_click", overlayId, point);
  }
  public void onPolygonClick(Polygon polygon, LatLng point) {
    String overlayId = "polygon_" + polygon.getId();
    this.onOverlayEvent("polygon_click", overlayId, point);
  }
  public void onCircleClick(Circle circle, LatLng point) {
    String overlayId = "circle_" + circle.getId();
    this.onOverlayEvent("circle_click", overlayId, point);
  }
  public void onGroundOverlayClick(GroundOverlay groundOverlay, LatLng point) {
    String overlayId = "groundoverlay_" + groundOverlay.getId();
    this.onOverlayEvent("groundoverlay_click", overlayId, point);
  }

  /**
   * Notify map event to JS
   * @param eventName
   */
  public void onMapEvent(final String eventName) {
    String js = String.format(Locale.ENGLISH, "javascript:if(window.cordova){cordova.fireDocumentEvent('%s', {evtName: '%s', callback:'_onMapEvent', args:[]});}",
            mapId, eventName);
    jsCallback(js);
  }

  /**
   * Notify map event to JS
   * @param eventName
   * @param point
   */
  public void onMapEvent(final String eventName, final LatLng point) {
    String js = String.format(Locale.ENGLISH, "javascript:if(window.cordova){cordova.fireDocumentEvent('%s', {evtName: '%s', callback:'_onMapEvent', args:[new plugin.google.maps.LatLng(%f, %f)]});}",
        mapId, eventName, point.latitude, point.longitude);
    jsCallback(js);
  }

  @Override
  public void onMapLongClick(LatLng point) {
    this.onMapEvent("map_long_click", point);
  }

  private double calculateDistance(LatLng pt1, LatLng pt2){
    float[] results = new float[1];
    Location.distanceBetween(pt1.latitude, pt1.longitude,
        pt2.latitude, pt2.longitude, results);
    return results[0];
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
   * Intersection for geodesic line
   * @ref http://my-clip-devdiary.blogspot.com/2014/01/html5canvas.html
   *
   * @param points
   * @param point
   * @param threshold
   * @return boolean
   */
  private boolean isPointOnTheGeodesicLine(List<LatLng> points, LatLng point, double threshold) {
    double trueDistance, testDistance1, testDistance2;
    Point p0, p1, touchPoint;
    //touchPoint = new Point();
    //touchPoint.x = (int) (point.latitude * 100000);
    //touchPoint.y = (int) (point.longitude * 100000);

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
   * Intersects using the Winding Number Algorithm
   * @ref http://www.nttpc.co.jp/company/r_and_d/technology/number_algorithm.html
   * @param path
   * @param point
   * @return
   */
  private boolean isPolygonContains(List<LatLng> path, LatLng point) {
    int wn = 0;
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
   * @param circle Instance of Circle class
   * @param point LatLng
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
    jsCallback(String.format(Locale.ENGLISH, "javascript:if(window.cordova){cordova.fireDocumentEvent('%s',{evtName: 'my_location_button_click', callback:'_onMapEvent'});}", mapId));
    return false;
  }


  /**
   * Notify the myLocationChange event to JS
   */
  private void onCameraEvent(final String eventName) {
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {

        CameraPosition position = map.getCameraPosition();
        JSONObject params = new JSONObject();
        String jsonStr = "";
        try {
          params.put("bearing", position.bearing);
          params.put("tilt", position.tilt);
          params.put("zoom", position.zoom);

          JSONObject target = new JSONObject();
          target.put("lat", position.target.latitude);
          target.put("lng", position.target.longitude);
          params.put("target", target);

          VisibleRegion visibleRegion = projection.getVisibleRegion();
          LatLngBounds latLngBounds = visibleRegion.latLngBounds;

          JSONObject northeast = new JSONObject();
          northeast.put("lat", latLngBounds.northeast.latitude);
          northeast.put("lng", latLngBounds.northeast.longitude);
          params.put("northeast", northeast);

          JSONObject southwest = new JSONObject();
          southwest.put("lat", latLngBounds.southwest.latitude);
          southwest.put("lng", latLngBounds.southwest.longitude);
          params.put("southwest", southwest);

          JSONObject nearLeft = new JSONObject();
          nearLeft.put("lat", visibleRegion.nearLeft.latitude);
          nearLeft.put("lng", visibleRegion.nearLeft.longitude);
          params.put("nearLeft", nearLeft);

          JSONObject nearRight = new JSONObject();
          nearRight.put("lat", visibleRegion.nearRight.latitude);
          nearRight.put("lng", visibleRegion.nearRight.longitude);
          params.put("nearRight", nearRight);

          JSONObject farLeft = new JSONObject();
          farLeft.put("lat", visibleRegion.farLeft.latitude);
          farLeft.put("lng", visibleRegion.farLeft.longitude);
          params.put("farLeft", farLeft);

          JSONObject farRight = new JSONObject();
          farRight.put("lat", visibleRegion.farRight.latitude);
          farRight.put("lng", visibleRegion.farRight.longitude);
          params.put("farRight", farRight);

          jsonStr = params.toString();
        } catch (JSONException e) {
          e.printStackTrace();
        }

        jsCallback(
            String.format(
                Locale.ENGLISH,
                "javascript:if(window.cordova){cordova.fireDocumentEvent('%s', {evtName:'%s', callback:'_onCameraEvent', args: [%s]});}",
                mapId, eventName, jsonStr));
      }
    });

    if (activeMarker != null) {
      syncInfoWndPosition();
    }
  }

  @Override
  public void onCameraIdle() {
    projection = map.getProjection();
    if (this.isDragging) {
      onMapEvent("map_drag_end");
    }
    this.isDragging = false;
    onCameraEvent("camera_move_end");
  }

  @Override
  public void onCameraMoveCanceled() {
    projection = map.getProjection();
    if (this.isDragging) {
      onMapEvent("map_drag_end");
    }
    this.isDragging = false;
    onCameraEvent("camera_move_end");
  }

  @Override
  public void onCameraMove() {
    projection = map.getProjection();
    if (this.isDragging) {
      onMapEvent("map_drag");
    }
    onCameraEvent("camera_move");
  }

  @Override
  public void onCameraMoveStarted(final int reason) {
    projection = map.getProjection();

    // In order to pass the gesture parameter to the callbacks,
    // use the _onMapEvent callback instead of the _onCameraEvent callback.
    this.isDragging = reason == REASON_GESTURE;

    if (this.isDragging) {
      onMapEvent("map_drag_start");
    }
    onCameraEvent("camera_move_start");


  }

  @Override
  public void onIndoorBuildingFocused() {
    IndoorBuilding building = map.getFocusedBuilding();
    String jsonStr = "undefined";
    if (building != null) {
      JSONObject result = PluginUtil.convertIndoorBuildingToJson(building);
      if (result != null) {
        jsonStr = result.toString();
      }
    }
    jsCallback(String.format(Locale.ENGLISH, "javascript:if(window.cordova){cordova.fireDocumentEvent('%s', {evtName:'indoor_building_focused', callback:'_onMapEvent', args: [%s]});}", mapId, jsonStr));
  }

  @Override
  public void onIndoorLevelActivated(IndoorBuilding building) {
    String jsonStr = "null";
    if (building != null) {
      JSONObject result = PluginUtil.convertIndoorBuildingToJson(building);
      if (result != null) {
        jsonStr = result.toString();
      }
    }
    jsCallback(String.format(Locale.ENGLISH, "javascript:if(window.cordova){cordova.fireDocumentEvent('%s', {evtName:'indoor_level_activated', callback:'_onMapEvent', args: [%s]});}", mapId, jsonStr));
  }

  private void jsCallback(final String js) {
    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        webView.loadUrl(js);
      }
    });
  }

  /**
   * Notify map click event to JS, also checks for click on a polygon and triggers onPolygonEvent
   * @param point
   */
  public void onMapClick(final LatLng point) {
    cordova.getThreadPool().submit(new Runnable() {
      @Override
      public void run() {

        //----------------------------------------------------------------------
        // Pick up overlays that contains the touchpoint in the hit area bounds
        //----------------------------------------------------------------------
        LatLngBounds bounds;
        final HashMap<String, Object> boundsHitList = new HashMap<String, Object>();

        PluginEntry pluginEntry;
        MyPlugin myPlugin;
        String[] keys;
        JSONObject properties;
        String pluginName, key;
        //String pluginNames[] = plugins.keySet().toArray(new String[plugins.size()]);
        int i, j;
        try {
          //for (i = 0; i < pluginNames.length; i++) {
            //pluginName = pluginNames[i];

            //if (pluginName.contains("marker")) {
            //  continue;
            //}
            //pluginEntry = plugins.get(pluginName);
            //myPlugin = (MyPlugin) pluginEntry.plugin;

            keys = objects.keySet().toArray(new String[objects.size()]);
            for (j = 0; j < keys.length; j++) {
              key = keys[j];
              if (key.contains("marker")) {
                continue;
              }
              if (key.contains("property")) {
                properties = (JSONObject) objects.get(key);
                try {
                  //Log.d("PluginMap", "-----> key = " + key + ", " + properties.toString(2));
                  //Log.d("PluginMap", "-----> key = " + key + ", isVisible = " + properties.getBoolean("isVisible") + ", isClickable = " + properties.getBoolean("isClickable"));
                  // skip invisible overlay
                  if (!properties.getBoolean("isVisible") ||
                      !properties.getBoolean("isClickable")) {
                    continue;
                  }
                } catch (JSONException e) {
                  e.printStackTrace();
                }
                bounds = (LatLngBounds) objects.get(key.replace("property", "bounds"));
                if (bounds.contains(point)) {
                  //Log.d("PluginMap", "-----> add key = " + key.replace("property_", ""));
                  boundsHitList.put(key, objects.get(key.replace("property_", "")));
                }

              }
            }

          //}
        } catch (Exception e) {
          //e.printStackTrace();
        }

        cordova.getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {

            //Log.d(TAG, "---> onMapClick : " + activeMarker);
            if (activeMarker != null) {
              //Log.d(TAG, "---> activeMarker.getTag() : " + activeMarker.getTag());
              String markerTag = activeMarker.getTag() + "";
              if (markerTag.contains("markercluster")) {
                if (markerTag.contains("-marker_")) {
                  onClusterEvent("info_close", activeMarker);
                }
              } else {
                onInfoWindowClose(activeMarker);
              }
              activeMarker = null;
            }
            String key;
            Map.Entry<String, Object> entry;

            Set<Map.Entry<String, Object>> entrySet = boundsHitList.entrySet();
            Iterator<Map.Entry<String, Object>> iterator = entrySet.iterator();


            List<LatLng> points ;
            Point origin = new Point();
            Point hitArea = new Point();
            hitArea.x = 1;
            hitArea.y = 1;
            double threshold = calculateDistance(
                projection.fromScreenLocation(origin),
                projection.fromScreenLocation(hitArea));

            float zIndex = -1;
            float maxZIndex = -1;
            Object hitOverlay = null;
            Object overlay;

            while(iterator.hasNext()) {
              entry = iterator.next();
              key = entry.getKey();
              overlay = entry.getValue();

              if (key.startsWith("polyline")) {

                Polyline polyline = (Polyline)overlay;
                if (polyline == null) {
                  continue;
                }
                zIndex = polyline.getZIndex();
                if (zIndex < maxZIndex) {
                  continue;
                }

                points = polyline.getPoints();

                if (polyline.isGeodesic()) {
                  if (isPointOnTheGeodesicLine(points, point, threshold)) {
                    hitOverlay = polyline;
                    maxZIndex = zIndex;
                    continue;
                  }
                } else {
                  if (isPointOnTheLine(points, point)) {
                    hitOverlay = polyline;
                    maxZIndex = zIndex;
                    continue;
                  }
                }
              }

              if (key.startsWith("polygon")) {
                Polygon polygon = (Polygon)overlay;
                if (polygon == null) {
                  continue;
                }
                zIndex = polygon.getZIndex();
                if (zIndex < maxZIndex) {
                  continue;
                }
                if (isPolygonContains(polygon.getPoints(), point)) {
                  hitOverlay = polygon;
                  maxZIndex = zIndex;
                  continue;
                }
              }


              if (key.startsWith("circle")) {
                Circle circle = (Circle)overlay;
                if (circle == null) {
                  continue;
                }
                zIndex = circle.getZIndex();
                if (zIndex < maxZIndex) {
                  continue;
                }
                if (isCircleContains(circle, point)) {
                  hitOverlay = circle;
                  maxZIndex = zIndex;
                  continue;
                }
              }
              if (key.startsWith("groundoverlay")) {
                GroundOverlay groundOverlay = (GroundOverlay)overlay;
                if (groundOverlay == null) {
                  continue;
                }
                zIndex = groundOverlay.getZIndex();
                if (zIndex < maxZIndex) {
                  continue;
                }
                if (isGroundOverlayContains(groundOverlay, point)) {
                  hitOverlay = groundOverlay;
                  maxZIndex = zIndex;
                  //continue;
                }
              }
            }

            //Log.d("PluginMap", "---> hitOverlay = " + hitOverlay);
            if (hitOverlay instanceof Polygon) {
              onPolygonClick((Polygon)hitOverlay, point);
            } else if (hitOverlay instanceof Polyline) {
              onPolylineClick((Polyline)hitOverlay, point);
            } else if (hitOverlay instanceof Circle) {
              onCircleClick((Circle)hitOverlay, point);
            } else if (hitOverlay instanceof GroundOverlay) {
              onGroundOverlayClick((GroundOverlay)hitOverlay, point);
            } else {
              // Only emit click event if no overlays are hit
              onMapEvent("map_click", point);
            }
          }
        });
      }
    });

  }

}
