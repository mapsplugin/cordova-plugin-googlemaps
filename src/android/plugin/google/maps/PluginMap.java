package plugin.google.maps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.location.Location;
import android.os.AsyncTask;
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

import androidx.annotation.NonNull;
import androidx.core.content.PermissionChecker;

import com.google.android.libraries.maps.CameraUpdate;
import com.google.android.libraries.maps.CameraUpdateFactory;
import com.google.android.libraries.maps.GoogleMap;
import com.google.android.libraries.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.libraries.maps.GoogleMap.OnIndoorStateChangeListener;
import com.google.android.libraries.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.libraries.maps.GoogleMap.OnMapClickListener;
import com.google.android.libraries.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.libraries.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.libraries.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.libraries.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.libraries.maps.GoogleMapOptions;
import com.google.android.libraries.maps.MapView;
import com.google.android.libraries.maps.OnMapReadyCallback;
import com.google.android.libraries.maps.Projection;
import com.google.android.libraries.maps.UiSettings;
import com.google.android.libraries.maps.model.CameraPosition;
import com.google.android.libraries.maps.model.CameraPosition.Builder;
import com.google.android.libraries.maps.model.Circle;
import com.google.android.libraries.maps.model.GroundOverlay;
import com.google.android.libraries.maps.model.IndoorBuilding;
import com.google.android.libraries.maps.model.LatLng;
import com.google.android.libraries.maps.model.LatLngBounds;
import com.google.android.libraries.maps.model.MapStyleOptions;
import com.google.android.libraries.maps.model.Marker;
import com.google.android.libraries.maps.model.PointOfInterest;
import com.google.android.libraries.maps.model.Polygon;
import com.google.android.libraries.maps.model.Polyline;
import com.google.android.libraries.maps.model.VisibleRegion;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class PluginMap extends MyPlugin implements OnMarkerClickListener,
    OnInfoWindowClickListener, OnMapClickListener, OnMapLongClickListener,
    OnMarkerDragListener, GoogleMap.OnMapLoadedCallback,
    OnMyLocationButtonClickListener, OnIndoorStateChangeListener, InfoWindowAdapter,
    GoogleMap.OnCameraIdleListener, GoogleMap.OnCameraMoveCanceledListener,
    GoogleMap.OnCameraMoveListener, GoogleMap.OnCameraMoveStartedListener,
    GoogleMap.OnInfoWindowLongClickListener, GoogleMap.OnInfoWindowCloseListener,
    GoogleMap.OnMyLocationClickListener, GoogleMap.OnPoiClickListener, IPluginView {

  public Map<String, IOverlayPlugin> plugins = new ConcurrentHashMap<String, IOverlayPlugin>();
  public Marker activeMarker = null;
  private boolean isDragging = false;
  private ImageView dummyMyLocationButton;
  public static final Object semaphore = new Object();

  private enum TEXT_STYLE_ALIGNMENTS {
    left, center, right
  }

  private final String ANIMATE_CAMERA_DONE = "animate_camera_done";
  private final String ANIMATE_CAMERA_CANCELED = "animate_camera_canceled";
  public static final float DEFAULT_CAMERA_PADDING = 20;

  private GoogleMap map;
  private MapView mapView;
  private boolean clickableIcons;
  private MetaPluginView metaPluginView;

  public void setDivId(String divId) { metaPluginView.divId = divId; }
  public String getDivId() {
    return metaPluginView.divId;
  }
  public ViewGroup getView() {
    return mapView;
  }

  @Override
  public MetaPluginView getMeta() {
    return metaPluginView;
  }


  public boolean getVisible() {
    return metaPluginView.isVisible;
  }
  public boolean getClickable() {
    return metaPluginView.isClickable;
  }
  public void setVisible(boolean isVisible) {
    this.metaPluginView.isVisible = isVisible;
  }
  public void setClickable(boolean isClickable) {
    this.metaPluginView.isClickable = isClickable;
  }
  public void setClickableIcons(boolean clickableIcons) {
    this.clickableIcons = clickableIcons;
  }
  public String getMapId() {
    return metaPluginView.getPluginId();
  }
  public GoogleMap getGoogleMap() {
    return this.map;
  }
  public void setActiveMarker(Marker marker) {
    this.activeMarker = marker;
  }

  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
    mainHandler = new Handler(Looper.getMainLooper());
  }

  public PluginMap getInstance(String mapId) {
    return (PluginMap) CordovaGoogleMaps.viewPlugins.get(mapId);
  }


  public String getOverlayId() {
    return this.getServiceName();
  }

  public void getMap(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    GoogleMapOptions options = new GoogleMapOptions();
    JSONObject metaJS = args.getJSONObject(0);


    String pluginId = metaJS.getString("__pgmId");
    metaPluginView = new MetaPluginView(pluginId);
    metaPluginView.viewDepth = metaJS.getInt("depth");
    metaPluginView.isClickable = true;
    metaPluginView.isVisible = true;

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


      if (controls.has("myLocationButton") || controls.has("myLocation")) {

        // Request geolocation permission.
        boolean locationPermission = PermissionChecker.checkSelfPermission(cordova.getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PermissionChecker.PERMISSION_GRANTED;
        //Log.d(TAG, "---> (235) hasPermission =  " + locationPermission);

        if (!locationPermission) {
          //_saveArgs = args;
          //_saveCallbackContext = callbackContext;
          synchronized (semaphore) {
            cordova.requestPermissions(PluginMap.this, callbackContext.hashCode(), new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION
            });
            try {
              semaphore.wait();
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
          locationPermission = PermissionChecker.checkSelfPermission(cordova.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PermissionChecker.PERMISSION_GRANTED;

          //Log.d(TAG, "---> (252)setMyLocationEnabled, hasPermission =  " + locationPermission);

        }
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
    LatLngBounds initCameraBounds = null;
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

    final LatLngBounds finalInitCameraBounds = null;

    mapView = new MapView(activity, options);

    mapView.onCreate(null);
    mapView.setTag(metaPluginView);

    mapView.getMapAsync(new OnMapReadyCallback() {
      @Override
      public void onMapReady(GoogleMap googleMap) {

        dummyMyLocationButton = new ImageView(activity);
        FrameLayout.LayoutParams lParams = new FrameLayout.LayoutParams((int)(48 * density), (int)(48 * density));
        lParams.gravity = Gravity.RIGHT;
        lParams.rightMargin = (int)(6 * density);
        lParams.topMargin = (int)(6 * density);
        lParams.leftMargin = 0;
        dummyMyLocationButton.setClickable(true);
        dummyMyLocationButton.setAlpha(0.75f);
        dummyMyLocationButton.setVisibility(View.GONE);
        dummyMyLocationButton.setLayoutParams(lParams);

        int buttonImgId = PluginUtil.getAppResource(activity, "dummy_my_location_button", "drawable");
        dummyMyLocationButton.setImageBitmap(BitmapFactory.decodeResource(activity.getResources(), buttonImgId));

        int shadowXmlId = PluginUtil.getAppResource(activity, "dummy_mylocation_button_shadow", "drawable");
        dummyMyLocationButton.setBackground(activity.getResources().getDrawable(shadowXmlId));

        dummyMyLocationButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            PluginMap.this.onMyLocationButtonClick();
          }
        });
        mapView.addView(dummyMyLocationButton);

        map = googleMap;

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

            if (controls.has("myLocationButton") || controls.has("myLocation")) {
              boolean locationPermission = PermissionChecker.checkSelfPermission(cordova.getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PermissionChecker.PERMISSION_GRANTED;
              //Log.d(TAG, "---> (314) hasPermission =  " + locationPermission);

              if (locationPermission) {
                Boolean isMyLocationEnabled = false;
                if (controls.has("myLocation")) {
                  isMyLocationEnabled = controls.getBoolean("myLocation");
                  map.setMyLocationEnabled(isMyLocationEnabled);
                }

                Boolean isMyLocationButtonEnabled = false;
                if (controls.has("myLocationButton")) {
                  isMyLocationButtonEnabled = controls.getBoolean("myLocationButton");
                  map.getUiSettings().setMyLocationButtonEnabled(isMyLocationButtonEnabled);
                }
                //Log.d(TAG, "--->isMyLocationButtonEnabled = " + isMyLocationButtonEnabled + ", isMyLocationEnabled = " + isMyLocationEnabled);
                if (!isMyLocationEnabled && isMyLocationButtonEnabled) {
                  dummyMyLocationButton.setVisibility(View.VISIBLE);
                } else {
                  dummyMyLocationButton.setVisibility(View.GONE);
                }
              }
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

              FrameLayout.LayoutParams lParams2 = (FrameLayout.LayoutParams) dummyMyLocationButton.getLayoutParams();
              lParams2.rightMargin = right + (int)(5 * density);
              lParams2.topMargin = top + (int)(5 * density);
              dummyMyLocationButton.setLayoutParams(lParams2);

            }

            if (preferences.has("restriction")) {
              JSONObject restriction = preferences.getJSONObject("restriction");
              LatLng sw = new LatLng(restriction.getDouble("south"), restriction.getDouble("west"));
              LatLng ne = new LatLng(restriction.getDouble("north"), restriction.getDouble("east"));
              LatLngBounds bounds = new LatLngBounds(sw, ne);

              map.setLatLngBoundsForCameraTarget(bounds);

              map.setMinZoomPreference((float)restriction.getDouble("minZoom"));
              map.setMaxZoomPreference((float)restriction.getDouble("maxZoom"));

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


            if (preferences.has("clickableIcons")) {
              clickableIcons = preferences.getBoolean("clickableIcons");
            }


            if (preferences.has("building")) {
              map.setBuildingsEnabled(preferences.getBoolean("building"));
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
          map.setOnMyLocationClickListener(PluginMap.this);
          map.setOnPoiClickListener(PluginMap.this);

          //Custom info window
          map.setInfoWindowAdapter(PluginMap.this);


          mapView.onResume();


          // ------------------------------
          // Embed the map if a container is specified.
          // ------------------------------
          if (args.length() == 3) {
            setDivId(args.getString(2));
          }

          CordovaGoogleMaps.mPluginLayout.addPluginOverlay(PluginMap.this);
          if (finalInitCameraBounds != null) {
            map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
              @Override
              public void onCameraIdle() {
                mapView.setVisibility(View.INVISIBLE);
                PluginMap.this.onCameraIdle();
                map.setOnCameraIdleListener(PluginMap.this);


                double CAMERA_PADDING = DEFAULT_CAMERA_PADDING;
                try {
                  if (params.has("camera")) {
                    JSONObject camera = params.getJSONObject("camera");
                    if (camera.has("padding")) {
                      CAMERA_PADDING = camera.getDouble("padding");
                    }
                  }
                } catch (Exception e) {
                  e.printStackTrace();
                }
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(finalInitCameraBounds, (int) (CAMERA_PADDING * density)));

                CameraPosition.Builder builder = CameraPosition.builder(map.getCameraPosition());

                try {
                  if (params.has("camera")) {
                    Boolean additionalParams = false;
                    JSONObject camera = params.getJSONObject("camera");
                    if (camera.has("bearing")) {
                      builder.bearing((float) camera.getDouble("bearing"));
                      additionalParams = true;
                    }
                    if (camera.has("tilt")) {
                      builder.tilt((float) camera.getDouble("tilt"));
                      additionalParams = true;
                    }
                    if (additionalParams) {
                      map.moveCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
                    }
                  }
                } catch (Exception e) {
                  e.printStackTrace();
                }
                mapView.setVisibility(View.VISIBLE);

                PluginMap.this.onMapLoaded();
                callbackContext.success();
              }
            });
          } else {
            mapView.setVisibility(View.VISIBLE);
            PluginMap.this.onCameraEvent("camera_move_end");
            callbackContext.success();
          }
        } catch (Exception e) {
          callbackContext.error(e.getMessage());
          e.printStackTrace();
        }
      }
    });

  }

  @Override
  public void onStart() {
    super.onStart();

//    if (mapView != null) {
//      mapView.onStart();
//    }
  }

  @Override
  public void onStop() {
    super.onStop();
//    if (mapView != null) {
//      mapView.onStop();
//    }
  }

  @Override
  public void onPause(boolean multitasking) {
    super.onPause(multitasking);
//    if (mapView != null && mapView.isActivated()) {
//      mapView.onPause();
//    }
  }
  @Override
  public void onResume(boolean multitasking) {
    super.onResume(multitasking);
//    mapView.onResume();
  }


  //-----------------------------------
  // Create the instance of class
  //-----------------------------------
  @PgmPluginMethod
  public void loadPlugin(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    PluginMap instance = this.getInstance(mapId);

    final String serviceName = args.getString(1);
    final String pluginId = mapId + "-" + serviceName.toLowerCase();

    try {
      IOverlayPlugin plugin;
      if (!instance.plugins.containsKey(pluginId)) {
        Class pluginCls = Class.forName("plugin.google.maps.Plugin" + serviceName);
        plugin = (IOverlayPlugin) pluginCls.newInstance();
        ((MyPlugin)plugin).privateInitialize(pluginId, cordova, webView, null);
        plugin.initialize(cordova, webView);
        plugin.setPluginMap(instance);
        instance.plugins.put(pluginId, plugin);
      } else {
        plugin = instance.plugins.get(pluginId);
      }
      plugin.create(args, callbackContext);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void _fitBounds(final LatLngBounds cameraBounds, int padding) {
    Builder builder = CameraPosition.builder();
    builder.tilt(map.getCameraPosition().tilt);
    builder.bearing(map.getCameraPosition().bearing);

    // Fit the camera to the cameraBounds with 20px padding.
    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(cameraBounds, padding / (int)density);
    try {
        map.moveCamera(cameraUpdate);
        builder.zoom(map.getCameraPosition().zoom);
        builder.target(map.getCameraPosition().target);
        map.moveCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
    } catch (Exception e) {
        e.printStackTrace();
    }
  }


  /**
   * Set clickable of the map
   * @param args Parameters given from JavaScript side
   * @param callbackContext Callback contect for sending back the result.
   */

  @PgmPluginMethod()
  public void setClickable(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    PluginMap instance = this.getInstance(mapId);
    instance.setClickable(args.getBoolean(1));
    callbackContext.success();
  }

  /**
   * Set visibility of the map
   * @param args Parameters given from JavaScript side
   * @param callbackContext Callback contect for sending back the result.
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setVisible(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    PluginMap instance = this.getInstance(mapId);
    instance.setVisible(args.getBoolean(1));
    if (instance.getVisible()) {
      ((MapView)instance.getView()).setVisibility(View.VISIBLE);
    } else {
      ((MapView)instance.getView()).setVisibility(View.INVISIBLE);
    }
    callbackContext.success();
  }

  /**
   * Destroy the map completely
   * @param args Parameters given from JavaScript side
   * @param callbackContext Callback contect for sending back the result.
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void remove(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    PluginMap instance = this.getInstance(mapId);

    instance.setClickable(false);
    instance.setRemoved(true);

    PluginMap.this.clear(null, new PluginUtil.MyCallbackContext(mapId + "_remove", webView) {

      @SuppressLint("MissingPermission")
      @Override
      public void onResult(PluginResult pluginResult) {
        CordovaGoogleMaps.mPluginLayout.removePluginOverlay(instance.getMapId());

        GoogleMap instanceMap = instance.getGoogleMap();
        MapView instanceMapView = (MapView) instance.getView();
        //Log.d("pluginMap", "--> map = " + map);
        if (instanceMap != null) {
          instanceMap.setIndoorEnabled(false);
          // instanceMap.setMyLocationEnabled(false);
          instanceMap.setOnPolylineClickListener(null);
          instanceMap.setOnPolygonClickListener(null);
          instanceMap.setOnIndoorStateChangeListener(null);
          instanceMap.setOnCircleClickListener(null);
          instanceMap.setOnGroundOverlayClickListener(null);
          instanceMap.setOnCameraIdleListener(null);
          instanceMap.setOnCameraMoveCanceledListener(null);
          instanceMap.setOnCameraMoveListener(null);
          instanceMap.setOnInfoWindowClickListener(null);
          instanceMap.setOnInfoWindowCloseListener(null);
          instanceMap.setOnMapClickListener(null);
          instanceMap.setOnMapLongClickListener(null);
          instanceMap.setOnMarkerClickListener(null);
          instanceMap.setOnMyLocationButtonClickListener(null);
          instanceMap.setOnMapLoadedCallback(null);
          instanceMap.setOnMarkerDragListener(null);
          instanceMap.setOnMyLocationClickListener(null);
          instanceMap.setOnPoiClickListener(null);
        }
        if (instanceMapView != null) {
          instanceMapView.clearAnimation();
          //instanceMapView.onCancelPendingInputEvents();   // Android 4.2 crashes
          instanceMapView.onPause();
          instanceMapView.onDestroy();
        }
        if (plugins.size() > 0) {
          for (IOverlayPlugin plugin : instance.plugins.values()) {
            ((MyPlugin)plugin).onDestroy();
          }
        }
        //Log.d("pluginMap", "--> mapView = " + mapView);
//        instance.projection = null;
//        plugins = null;
//        map = null;
//        mapView = null;
//        initCameraBounds = null;
//        activity = null;
//        mapDivId = null;
//        activeMarker = null;

        System.gc();
        Runtime.getRuntime().gc();
        if (callbackContext != null) {
          callbackContext.success();
        }
        PluginMap.this.onDestroy();
      }
    });
  }


  @Override
  public View getInfoContents(Marker marker) {

    activeMarker = marker;
    String title = marker.getTitle();
    String snippet = marker.getSnippet();
    if ((title == null) && (snippet == null)) {
      return null;
    }

    String clusterId_markerId = (String) marker.getTag();
    String tmp[] = clusterId_markerId.split("_");
    String className = tmp[0];

    String mapId = getMapId();

    PluginMarker pluginMarker = (PluginMarker)plugins.get(mapId + "-" + className);
    if (pluginMarker == null) {
      return null;
    }

    JSONObject styles = null;
    MetaMarker metaMarker = pluginMarker.objects.get(clusterId_markerId);

    try {
      if (metaMarker.properties.has("styles")) {
          styles = (JSONObject) metaMarker.properties.getJSONObject("styles");
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }

    if (clusterId_markerId.startsWith("markercluster_")){
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
      if (styles.has("width")) {
        try {
          int width = 0;
          String widthString = styles.getString("width");

          if (widthString.endsWith("%")) {
            double widthDouble = Double.parseDouble(widthString.replace("%", ""));

            width = (int) ((double) mapView.getWidth() * (widthDouble / 100));
          } else if (PluginUtil.isNumeric(widthString)) {
            double widthDouble = Double.parseDouble(widthString);

            if (widthDouble <= 1.0) {  // for percentage values (e.g. 0.5 = 50%).
              width = (int) ((double) mapView.getWidth() * (widthDouble));
            } else {
              width = (int) widthDouble;
            }
          }

          if (width > 0) {
            layoutParams.width = width;
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      if (styles.has("maxWidth")) {
        try {
          String widthString = styles.getString("maxWidth");

          if (widthString.endsWith("%")) {
            double widthDouble = Double.parseDouble(widthString.replace("%", ""));

            maxWidth = (int) ((double) mapView.getWidth() * (widthDouble / 100));

            // make sure to take padding into account.
            maxWidth -= (windowLayer.getPaddingLeft() + windowLayer.getPaddingRight());
          } else if (PluginUtil.isNumeric(widthString)) {
            double widthDouble = Double.parseDouble(widthString);

            if (widthDouble <= 1.0) {  // for percentage values (e.g. 0.5 = 50%).
              maxWidth = (int) ((double) mapView.getWidth() * (widthDouble));
            } else {
              maxWidth = (int) widthDouble;
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    windowLayer.setLayoutParams(layoutParams);


    //----------------------------------------
    // text-align = left | center | right
    //----------------------------------------
    int gravity = Gravity.LEFT;
    int textAlignment = View.TEXT_ALIGNMENT_GRAVITY;

    if (styles != null) {
      if (styles.has("text-align")) {
        try {
          String textAlignValue = styles.getString("text-align");

          switch (TEXT_STYLE_ALIGNMENTS.valueOf(textAlignValue)) {
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
        textView.setTextAlignment(textAlignment);

        //----------------------------------------
        // font-style = normal | italic
        // font-weight = normal | bold
        //----------------------------------------
        int fontStyle = Typeface.NORMAL;
        if (styles != null) {
          if (styles.has("font-style")) {
            try {
              if ("italic".equals(styles.getString("font-style"))) {
                fontStyle = Typeface.ITALIC;
              }
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
          if (styles.has("font-weight")) {
            try {
              if ("bold".equals(styles.getString("font-weight"))) {
                fontStyle = fontStyle | Typeface.BOLD;
              }
            } catch (JSONException e) {
              e.printStackTrace();
            }
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
      textView2.setTextAlignment(textAlignment);

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


    String markerTag = (String) marker.getTag();
    String tmp[] = markerTag.split("_");
    String className = tmp[0];
    tmp = markerTag.split("-");

    String mapId = getMapId();
    String markerId = tmp[tmp.length - 1];

    PluginMarker pluginMarker = (PluginMarker)plugins.get(mapId + "-" + className);
    if (pluginMarker == null) {
      return null;
    }

    try {
      if (marker.getTitle() == null && marker.getSnippet() == null) {

        syncInfoWndPosition();

        if (markerTag.startsWith("markercluster_")){
          this.onClusterEvent("info_open", marker);
        } else {
          this.onMarkerEvent("info_open", marker);
        }

        int resId = PluginUtil.getAppResource(activity, "dummy_infowindow", "layout");
        return activity.getLayoutInflater().inflate(resId, null);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }


  /**
   * Sets options
   */
  @PgmPluginMethod
  public void setOptions(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final String mapId = args.getString(0);
    PluginMap instance = this.getInstance(mapId);

    final AsyncSetOptionsResult results = new AsyncSetOptionsResult();
    results.cameraPadding = DEFAULT_CAMERA_PADDING;

    try {
      final JSONObject params = args.getJSONObject(1);

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




      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          GoogleMap instanceMap = instance.getGoogleMap();

          try {
            if (results.cameraPosition != null) {
              try {
                instance.getGoogleMap().moveCamera(CameraUpdateFactory.newCameraPosition(results.cameraPosition));
              } catch (Exception e) {
                e.printStackTrace();
              }
              if (results.cameraBounds != null) {
                instance._fitBounds(results.cameraBounds, (int)(results.cameraPadding * density));
              }
            }

            //styles
            if (results.styles != null) {
              MapStyleOptions styleOptions = new MapStyleOptions(results.styles);
              instanceMap.setMapStyle(styleOptions);
              instanceMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            } else if (results.MAP_TYPE_ID != -1) {
              instanceMap.setMapType(results.MAP_TYPE_ID);
            }

            UiSettings settings = instanceMap.getUiSettings();

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
                instanceMap.setPadding(left, top, right, bottom);

                FrameLayout.LayoutParams lParams2 = (FrameLayout.LayoutParams) dummyMyLocationButton.getLayoutParams();
                lParams2.rightMargin = right + (int)(5 * density);
                lParams2.topMargin = top + (int)(5 * density);
                dummyMyLocationButton.setLayoutParams(lParams2);
              }

              if (preferences.has("zoom")) {
                if (!"null".equals(preferences.getString("zoom"))) {
                  JSONObject zoom = preferences.getJSONObject("zoom");
                  if (zoom.has("minZoom")) {
                    instanceMap.setMinZoomPreference((float) zoom.getDouble("minZoom"));
                  }
                  if (zoom.has("maxZoom")) {
                    instanceMap.setMaxZoomPreference((float) zoom.getDouble("maxZoom"));
                  }
                } else {
                  instanceMap.setMinZoomPreference(2);
                  instanceMap.setMaxZoomPreference(23);
                }
              }

              if (preferences.has("building")) {
                instanceMap.setBuildingsEnabled(preferences.getBoolean("building"));
              }

              if (preferences.has("clickableIcons")) {
                instance.setClickableIcons(preferences.getBoolean("clickableIcons"));
              }

              if (preferences.has("restriction")) {
                if (!"null".equals(preferences.getString("restriction"))) {

                  JSONObject restriction = preferences.getJSONObject("restriction");
                  LatLngBounds.Builder builder = new LatLngBounds.Builder();
                  builder.include(new LatLng(restriction.getDouble("south"), restriction.getDouble("west")));
                  builder.include(new LatLng(restriction.getDouble("north"), restriction.getDouble("east")));
                  instanceMap.setLatLngBoundsForCameraTarget(builder.build());

                  instanceMap.setMaxZoomPreference((float)restriction.getDouble("maxZoom"));
                  instanceMap.setMinZoomPreference((float)restriction.getDouble("minZoom"));
                } else {

                  if (preferences.has("zoom") && !"null".equals(preferences.getString("zoom"))) {
                    JSONObject zoom = preferences.getJSONObject("zoom");
                    if (zoom.has("minZoom")) {
                      instanceMap.setMinZoomPreference((float) zoom.getDouble("minZoom"));
                    } else {
                      instanceMap.setMinZoomPreference(2);
                    }
                    if (zoom.has("maxZoom")) {
                      instanceMap.setMaxZoomPreference((float) zoom.getDouble("maxZoom"));
                    } else {
                      instanceMap.setMaxZoomPreference(23);
                    }
                  } else {
                    instanceMap.setMinZoomPreference(2);
                    instanceMap.setMaxZoomPreference(23);
                  }
                  instanceMap.setLatLngBoundsForCameraTarget(null);
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
              final JSONObject controls = params.getJSONObject("controls");

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
              if (controls.has("myLocation") || controls.has("myLocationButton")) {
                JSONArray args = new JSONArray();
                args.put(mapId);
                args.put(controls);
                instance.setMyLocationEnabled(args, callbackContext);
              } else {
                callbackContext.success();
              }
            } else {
              callbackContext.success();
            }
          } catch (Exception e) {
            e.printStackTrace();
            callbackContext.error("error at map.setOptions()");

          }
        }
      });

    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error("error at map.setOptions()");
    }



  }

  @PgmPluginMethod(runOnUiThread = true)
  public void getFocusedBuilding(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    PluginMap instance = this.getInstance(args.getString(0));
    IndoorBuilding focusedBuilding = instance.getGoogleMap().getFocusedBuilding();
    if (focusedBuilding != null) {
      JSONObject result = PluginUtil.convertIndoorBuildingToJson(focusedBuilding);
      callbackContext.success(result);
    } else {
      callbackContext.success(-1);
    }
  }

  /**
   * Set center location of the marker
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setCameraTarget(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    PluginMap instance = this.getInstance(mapId);
    double lat = args.getDouble(1);
    double lng = args.getDouble(2);

    LatLng latLng = new LatLng(lat, lng);
    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(latLng);
    instance.myMoveCamera(mapId, cameraUpdate, callbackContext);
  }

  /**
   * Set angle of the map view
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setCameraTilt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    PluginMap instance = this.getInstance(mapId);
    float tilt = (float) args.getDouble(1);

    if (tilt > 0 && tilt <= 90) {
      CameraPosition currentPos = instance.getGoogleMap().getCameraPosition();
      CameraPosition newPosition = new CameraPosition.Builder()
          .target(currentPos.target).bearing(currentPos.bearing)
          .zoom(currentPos.zoom).tilt(tilt).build();
      instance.myMoveCamera(mapId, newPosition, callbackContext);
    } else {
      callbackContext.error("Invalid tilt angle(" + tilt + ")");
    }
  }

  @PgmPluginMethod(runOnUiThread = true)
  public void setCameraBearing(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    PluginMap instance = this.getInstance(mapId);
    final float bearing = (float) args.getDouble(1);

    CameraPosition currentPos = instance.getGoogleMap().getCameraPosition();
    CameraPosition newPosition = new CameraPosition.Builder()
      .target(currentPos.target).bearing(bearing)
      .zoom(currentPos.zoom).tilt(currentPos.tilt).build();
    instance.myMoveCamera(mapId, newPosition, callbackContext);
  }

  /**
   * Move the camera with animation
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void animateCamera(JSONArray args, CallbackContext callbackContext) throws JSONException {
    PluginMap instance = this.getInstance(args.getString(0));
    instance.updateCameraPosition("animateCamera", args, callbackContext);
  }

  /**
   * Move the camera without animation
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void moveCamera(JSONArray args, CallbackContext callbackContext) throws JSONException {
    PluginMap instance = this.getInstance(args.getString(0));
    instance.updateCameraPosition("moveCamera", args, callbackContext);
  }


  /**
   * move the camera
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void updateCameraPosition(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final String mapId = args.getString(0);
    PluginMap instance = this.getInstance(mapId);
    if (instance.isRemoved) {
      return;
    }
    final JSONObject cameraPos = args.getJSONObject(1);
    CameraPosition.Builder builder = CameraPosition.builder(instance.getGoogleMap().getCameraPosition());

    @SuppressLint("StaticFieldLeak")
    UpdateCameraAction cameraAction = new UpdateCameraAction(callbackContext, cameraPos, builder) {

      @Override
      public void onPostExecute(AsyncUpdateCameraPositionResult AsyncUpdateCameraPositionResult) {
        if (isRemoved) {
          return;
        }


        if (AsyncUpdateCameraPositionResult.cameraUpdate == null) {
          CameraPosition.Builder builder = CameraPosition.builder(instance.map.getCameraPosition());
          builder.target(instance.map.getCameraPosition().target);
          AsyncUpdateCameraPositionResult.cameraUpdate = CameraUpdateFactory.newCameraPosition(builder.build());
        }

        final AsyncUpdateCameraPositionResult finalCameraPosition = AsyncUpdateCameraPositionResult;
        PluginUtil.MyCallbackContext myCallback = new PluginUtil.MyCallbackContext("moveCamera", webView) {
          @Override
          public void onResult(final PluginResult pluginResult) {
            if (finalCameraPosition.cameraBounds != null && ANIMATE_CAMERA_DONE.equals(pluginResult.getStrMessage())) {


              final CameraPosition.Builder builder = CameraPosition.builder(map.getCameraPosition());
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
                instance.map.moveCamera(cameraUpdate);
              } catch (Exception e) {
                e.printStackTrace();
              }
              instance.map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                @Override
                public void onCameraIdle() {
                  PluginMap.this.onCameraIdle();
                  instance.map.setOnCameraIdleListener(PluginMap.this);
                  builder.zoom(instance.map.getCameraPosition().zoom);
                  builder.target(instance.map.getCameraPosition().target);
                  instance.map.moveCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
                }
              });
            } else {
              final CameraPosition.Builder builder = CameraPosition.builder(instance.map.getCameraPosition());
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
                instance.map.moveCamera(finalCameraPosition.cameraUpdate);
              } catch (Exception e) {
                e.printStackTrace();
              }

              builder.zoom(instance.map.getCameraPosition().zoom);
              builder.target(instance.map.getCameraPosition().target);

              instance.map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                @Override
                public void onCameraIdle() {
                  PluginMap.this.onCameraIdle();
                  instance.map.setOnCameraIdleListener(PluginMap.this);
                  builder.zoom(instance.map.getCameraPosition().zoom);
                  builder.target(instance.map.getCameraPosition().target);
                  instance.map.moveCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
                }
              });
            }
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
          }
        };
        if (action.equals("moveCamera")) {
          myMoveCamera(mapId, AsyncUpdateCameraPositionResult.cameraUpdate, myCallback);
        } else {
          myAnimateCamera(mapId, AsyncUpdateCameraPositionResult.cameraUpdate, AsyncUpdateCameraPositionResult.durationMS, myCallback);
        }

      }
    };
    cameraAction.execute();
  }


  /**
   * Set zoom of the map
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setCameraZoom(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    Long zoom = args.getLong(1);
    this.myMoveCamera(mapId, CameraUpdateFactory.zoomTo(zoom), callbackContext);
  }


  /**
   * Stop camera animation
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void stopAnimation(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    PluginMap instance = this.getInstance(args.getString(0));
    if (instance.getGoogleMap() != null) {
      instance.getGoogleMap().stopAnimation();
    }
    callbackContext.success();
  }

  /**
   * Pan by the specified pixel
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void panBy(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    PluginMap instance = this.getInstance(args.getString(0));
    int x = args.getInt(1);
    int y = args.getInt(2);
    float xPixel = -x * density;
    float yPixel = -y * density;

    CameraUpdate cameraUpdate = CameraUpdateFactory.scrollBy(xPixel, yPixel);
    instance.getGoogleMap().animateCamera(cameraUpdate);
    callbackContext.success();
  }

  /**
   * Move the camera of the map
   */
  public void myMoveCamera(String mapId, CameraPosition cameraPosition, final CallbackContext callbackContext) {
    CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
    myMoveCamera(mapId, cameraUpdate, callbackContext);
  }

  /**
   * Move the camera of the map
   */
  public void myMoveCamera(String mapId, CameraUpdate cameraUpdate, CallbackContext callbackContext) {
    PluginMap instance = this.getInstance(mapId);
    try {
      instance.getGoogleMap().moveCamera(cameraUpdate);
    } catch (Exception e) {
        e.printStackTrace();
    }
    callbackContext.success();
  }


  /**
   * Enable MyLocation feature if set true
   */
  @PgmPluginMethod
  public void setMyLocationEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    PluginMap instance = this.getInstance(args.getString(0));
    final JSONObject params = args.getJSONObject(1);

    boolean locationPermission = PermissionChecker.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PermissionChecker.PERMISSION_GRANTED;
    //Log.d(TAG, "---> setMyLocationEnabled, hasPermission =  " + locationPermission);

    if (!locationPermission) {
      //_saveArgs = args;
      //_saveCallbackContext = callbackContext;
      synchronized (semaphore) {
        cordova.requestPermissions(this, callbackContext.hashCode(), new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION
        });
        try {
          semaphore.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      locationPermission = PermissionChecker.checkSelfPermission(cordova.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PermissionChecker.PERMISSION_GRANTED;

      //Log.d(TAG, "---> (1720)setMyLocationEnabled, hasPermission =  " + locationPermission);

      if (!locationPermission) {
        callbackContext.error(PluginUtil.getPgmStrings(activity,"pgm_location_rejected_by_user"));
        return;
      }

    }

    activity.runOnUiThread(new Runnable() {
      @SuppressLint("MissingPermission")
      @Override
      public void run() {
        try {
          GoogleMap instanceMap = instance.getGoogleMap();
          boolean isMyLocationEnabled = false;
          if (params.has("myLocation")) {
            //Log.d(TAG, "--->myLocation = " + params.getBoolean("myLocation"));
            isMyLocationEnabled = params.getBoolean("myLocation");
            instanceMap.setMyLocationEnabled(isMyLocationEnabled);
          }

          Boolean isMyLocationButtonEnabled = false;
          if (params.has("myLocationButton")) {
            //Log.d(TAG, "--->myLocationButton = " + params.getBoolean("myLocationButton"));
            isMyLocationButtonEnabled = params.getBoolean("myLocationButton");
            instanceMap.getUiSettings().setMyLocationButtonEnabled(isMyLocationButtonEnabled);
          }
          //Log.d(TAG, "--->isMyLocationButtonEnabled = " + isMyLocationButtonEnabled + ", isMyLocationEnabled = " + isMyLocationEnabled);
          if (!isMyLocationEnabled && isMyLocationButtonEnabled) {
            dummyMyLocationButton.setVisibility(View.VISIBLE);
          } else {
            dummyMyLocationButton.setVisibility(View.GONE);
          }

        } catch (Exception e) {
          e.printStackTrace();
        }
        callbackContext.success();
      }
    });
  }

  /**
   * Clear all markups
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void clear(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    PluginMap instance = this.getInstance(args.getString(0));
    String pluginIDs[] = instance.plugins.keySet().toArray(new String[plugins.size()]);
    Semaphore semaphore = new Semaphore(instance.plugins.size());

    for (int i = 0; i < pluginIDs.length; i++) {
      try {
        semaphore.acquire();
        CallbackContext dummy = new CallbackContext("dummy", webView) {
          @Override
          public void sendPluginResult(PluginResult pluginResult) {
            semaphore.release();
          }
        };
        ((MyPlugin)(instance.plugins.remove(pluginIDs[i]))).onDestroy();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    semaphore.release();
    callbackContext.success();
  }

  /**
   * Enable Indoor map feature if set true
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setIndoorEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    PluginMap instance = this.getInstance(args.getString(0));
    boolean isEnabled = args.getBoolean(1);
    instance.getGoogleMap().setIndoorEnabled(isEnabled);
    callbackContext.success();
  }

  /**
   * Enable the traffic layer if set true
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setTrafficEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    PluginMap instance = this.getInstance(args.getString(0));
    boolean isEnabled = args.getBoolean(1);
    instance.getGoogleMap().setTrafficEnabled(isEnabled);
    callbackContext.success();
  }

  /**
   * Enable the compass if set true
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setCompassEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    PluginMap instance = this.getInstance(args.getString(0));
    boolean isEnabled = args.getBoolean(1);
    UiSettings uiSettings = instance.getGoogleMap().getUiSettings();
    uiSettings.setCompassEnabled(isEnabled);
    callbackContext.success();
  }

  /**
   * Change the map type id of the map
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setMapTypeId(JSONArray args, final CallbackContext callbackContext) throws JSONException {

    int mapTypeId = -1;
    String typeStr = args.getString(1);
    mapTypeId = typeStr.equals("MAP_TYPE_NORMAL") ? GoogleMap.MAP_TYPE_NORMAL : mapTypeId;
    mapTypeId = typeStr.equals("MAP_TYPE_HYBRID") ? GoogleMap.MAP_TYPE_HYBRID : mapTypeId;
    mapTypeId = typeStr.equals("MAP_TYPE_SATELLITE") ? GoogleMap.MAP_TYPE_SATELLITE : mapTypeId;
    mapTypeId = typeStr.equals("MAP_TYPE_TERRAIN") ? GoogleMap.MAP_TYPE_TERRAIN : mapTypeId;
    mapTypeId = typeStr.equals("MAP_TYPE_NONE") ? GoogleMap.MAP_TYPE_NONE : mapTypeId;

    if (mapTypeId == -1) {
      callbackContext.error("Unknown MapTypeID is specified:" + typeStr);
      return;
    }

    PluginMap instance = this.getInstance(args.getString(0));
    instance.getGoogleMap().setMapType(mapTypeId);
    callbackContext.success();
  }


  /**
   * Move the camera of the map
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void myAnimateCamera(final String mapId, final CameraUpdate cameraUpdate, final int durationMS, final CallbackContext callbackContext) {
    PluginMap instance = this.getInstance(mapId);
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

    if (durationMS > 0) {
      instance.getGoogleMap().animateCamera(cameraUpdate, durationMS, callback);
    } else {
      instance.getGoogleMap().animateCamera(cameraUpdate, callback);
    }
  }



  /**
   * Return the image data encoded with base64
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void toDataURL(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    PluginMap instance = this.getInstance(args.getString(0));
    JSONObject params = args.getJSONObject(1);
    boolean uncompress = false;
    if (params.has("uncompress")) {
      uncompress = params.getBoolean("uncompress");
    }
    final boolean finalUncompress = uncompress;
    instance.getGoogleMap().snapshot(new GoogleMap.SnapshotReadyCallback() {

      @Override
      public void onSnapshotReady(final Bitmap image) {
        AsyncTask.execute(new Runnable() {
          @Override
          public void run() {
            Bitmap image2 = image;
            if (!finalUncompress) {
              image2 = PluginUtil.resizeBitmap(image,
                  (int) (image2.getWidth() * density),
                  (int) (image2.getHeight() * density));
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            image2.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            byte[] byteArray = outputStream.toByteArray();

            callbackContext.success("data:image/png;base64," +
                Base64.encodeToString(byteArray, Base64.NO_WRAP));
          }
        });
      }
    });

  }

  @PgmPluginMethod(runOnUiThread = true)
  public void fromLatLngToPoint(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    PluginMap instance = this.getInstance(args.getString(0));
    JSONObject params = args.getJSONObject(1);
    double lat = params.getDouble("lat");
    double lng = params.getDouble("lng");
    LatLng latLng = new LatLng(lat, lng);

    Projection projection = instance.getGoogleMap().getProjection();
    Point point = projection.toScreenLocation(latLng);
    try {
      JSONArray pointJSON = new JSONArray();
      pointJSON.put((int)((double)point.x / (double)density));
      pointJSON.put((int)((double)point.y / (double)density));
      callbackContext.success(pointJSON);
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage() + "");
    }
  }

  @PgmPluginMethod(runOnUiThread = true)
  public void fromPointToLatLng(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    PluginMap instance = this.getInstance(args.getString(0));
    JSONObject params = args.getJSONObject(1);
    double pointX = params.getDouble("x");
    double pointY = params.getDouble("y");
    Point point = new Point();
    point.x = (int)(pointX * density);
    point.y = (int)(pointY * density);
    Projection projection = instance.getGoogleMap().getProjection();
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



  /**
   * Sets the preference for whether all gestures should be enabled or disabled.
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setAllGesturesEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    PluginMap instance = this.getInstance(args.getString(0));
    Boolean isEnabled = args.getBoolean(1);
    UiSettings uiSettings = instance.getGoogleMap().getUiSettings();
    uiSettings.setAllGesturesEnabled(isEnabled);
    callbackContext.success();
  }

  /**
   * Sets padding of the map
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setPadding(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    PluginMap instance = this.getInstance(args.getString(0));
    JSONObject padding = args.getJSONObject(1);
    final int left = (int)(padding.getInt("left") * density);
    final int top = (int)(padding.getInt("top") * density);
    final int bottom = (int)(padding.getInt("bottom") * density);
    final int right = (int)(padding.getInt("right") * density);
    instance.getGoogleMap().setPadding(left, top, right, bottom);

    FrameLayout.LayoutParams lParams2 = (FrameLayout.LayoutParams) dummyMyLocationButton.getLayoutParams();
    lParams2.rightMargin = right + (int)(5 * density);
    lParams2.topMargin = top + (int)(5 * density);
    dummyMyLocationButton.setLayoutParams(lParams2);

    callbackContext.success();
  }

  /**
   * update the active marker (for internal use)
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setActiveMarkerId(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    PluginMap instance = this.getInstance(args.getString(0));
    String markerId = args.getString(1);
    PluginMarker pluginMarker = (PluginMarker)instance.plugins.get(instance.getMapId() + "-marker");
    MetaMarker metaMarker = pluginMarker.objects.get(markerId);

    instance.setActiveMarker(metaMarker.marker);
    callbackContext.success();
  }


  @Override
  public boolean onMarkerClick(Marker marker) {

    String clusterId_markerId = marker.getTag() + "";
    if (clusterId_markerId.contains("markercluster_")) {
      if (clusterId_markerId.contains("-marker_")) {
        setActiveMarker(marker);
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
      setActiveMarker(marker);
    }

    String tmp[] = clusterId_markerId.split("_");
    String className = tmp[0];

    String mapId = this.getMapId();
    PluginMarker markerPlugin = (PluginMarker) this.plugins.get(String.format("%s-%s", mapId, className));
    if (markerPlugin == null) {
      return true;
    }

    MetaMarker meta = markerPlugin.objects.get(clusterId_markerId);
    if (meta == null) {
      return true;
    }

    if (meta.properties.has("disableAutoPan")) {
      boolean disableAutoPan = false;
      try {
        disableAutoPan = meta.properties.getBoolean("disableAutoPan");
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

    return true;
  }

  @Override
  public void onInfoWindowClick(Marker marker) {
    activeMarker = marker;
    syncInfoWndPosition();
    String markerTag = marker.getTag() + "";
    Log.d(TAG, "--->markerTag = " + markerTag);
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
    //Log.d(TAG, "--->onInfoWindowClose");
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
    } else {
      this.onMarkerEvent("info_close", marker);
    }
    //activeMarker = null; // <-- This causes HTMLinfoWindow is not able to close when you tap on the map.
  }

  @Override
  public void onMapLoaded() {
    this.onCameraEvent("camera_move_end");
  }



  /********************************************************
   * Callbacks
   ********************************************************/

  /**
   * Notify marker event to JS
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
    String mapId = this.getMapId();
    String js = String.format(Locale.ENGLISH, "javascript:if('%s' in plugin.google.maps){plugin.google.maps['%s']({evtName: '%s', callback:'_onMarkerEvent', args:['%s', new plugin.google.maps.LatLng(%f, %f)]});}",
          mapId, mapId, eventName, markerId, latLng.latitude, latLng.longitude);
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
    String mapId = this.getMapId();
    String js = String.format(Locale.ENGLISH, "javascript:if('%s' in plugin.google.maps){plugin.google.maps['%s']({evtName: '%s', callback:'_onClusterEvent', args:['%s', '%s', new plugin.google.maps.LatLng(%f, %f)]});}",
            mapId, mapId, eventName, clusterId, markerId, latLng.latitude, latLng.longitude);
    jsCallback(js);
  }
  public void syncInfoWndPosition() {
    if (activeMarker == null) {
      Log.d("PluginMap", "--->no active marker");
      return;
    }
    LatLng latLng = activeMarker.getPosition();
    Point point = this.map.getProjection().toScreenLocation(latLng);

    String mapId = this.getMapId();
    String js = String.format(Locale.ENGLISH, "javascript:if('%s' in plugin.google.maps){plugin.google.maps['%s']({evtName: 'syncPosition', callback:'_onSyncInfoWndPosition', args:[{'x': %d, 'y': %d}]});}",
        mapId, mapId, (int)(point.x / density), (int)(point.y / density));
    jsCallback(js);
  }

  public void onOverlayEvent(String eventName, String overlayId, LatLng point) {
    String mapId = this.getMapId();
    String js = String.format(Locale.ENGLISH, "javascript:if('%s' in plugin.google.maps){plugin.google.maps['%s']({evtName: '%s', callback:'_onOverlayEvent', args:['%s', new plugin.google.maps.LatLng(%f, %f)]});}",
        mapId, mapId, eventName, overlayId, point.latitude, point.longitude);
    jsCallback(js);
  }
  public void onPolylineClick(Polyline polyline, LatLng point) {
    String overlayId = polyline.getTag().toString();
    this.onOverlayEvent("polyline_click", overlayId, point);
  }
  public void onPolygonClick(Polygon polygon, LatLng point) {
    String overlayId =  polygon.getTag().toString();
    this.onOverlayEvent("polygon_click", overlayId, point);
  }
  public void onCircleClick(Circle circle, LatLng point) {
    String overlayId = circle.getTag().toString();
    this.onOverlayEvent("circle_click", overlayId, point);
  }
  public void onGroundOverlayClick(GroundOverlay groundOverlay, LatLng point) {
    String overlayId = groundOverlay.getTag().toString();
    this.onOverlayEvent("groundoverlay_click", overlayId, point);
  }

  /**
   * Notify map event to JS
   */
  public void onMapEvent(final String eventName) {
    String mapId = this.getMapId();
    String js = String.format(Locale.ENGLISH, "javascript:if('%s' in plugin.google.maps){plugin.google.maps['%s']({evtName: '%s', callback:'_onMapEvent', args:[]});}",
            mapId, mapId, eventName);
    jsCallback(js);
  }

  /**
   * Notify map event to JS
   */
  public void onMapEvent(final String eventName, final LatLng point) {
    String mapId = this.getMapId();
    String js = String.format(Locale.ENGLISH, "javascript:if('%s' in plugin.google.maps){plugin.google.maps['%s']({evtName: '%s', callback:'_onMapEvent', args:[new plugin.google.maps.LatLng(%f, %f)]});}",
        mapId, mapId, eventName, point.latitude, point.longitude);
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
   */
  private LatLng isPointOnTheLine(Projection projection, List<LatLng> points, LatLng point) {
    double Sx, Sy;
    Point p0, p1, touchPoint;
    touchPoint = projection.toScreenLocation(point);

    p0 = projection.toScreenLocation(points.get(0));
    for (int i = 1; i < points.size(); i++) {
      p1 = projection.toScreenLocation(points.get(i));
      Sx = ((double)touchPoint.x - (double)p0.x) / ((double)p1.x - (double)p0.x);
      Sy = ((double)touchPoint.y - (double)p0.y) / ((double)p1.y - (double)p0.y);
      if (Math.abs(Sx - Sy) < 0.05 && Sx < 1 && Sx > 0) {
        return points.get(i);
      }
      p0 = p1;
    }
    return null;
  }

  /**
   * Intersection for geodesic line
   * @ref http://my-clip-devdiary.blogspot.com/2014/01/html5canvas.html
   *
   */
  private LatLng isPointOnTheGeodesicLine(Projection projection, List<LatLng> points, final LatLng point, double threshold) {

    double trueDistance, testDistance1, testDistance2;
    Point p0, p1;
    int fingerSize = (int)(20 * density); // assume finger size is 20px

    // clicked point(latlng) -> pixels
    Point touchPoint = projection.toScreenLocation(point);
    LatLngBounds possibleBounds = new LatLngBounds(point, point);
    Point nePoint = new Point(touchPoint.x - fingerSize, touchPoint.y - fingerSize);
    Point swPoint = new Point(touchPoint.x + fingerSize, touchPoint.y + fingerSize);

    possibleBounds = possibleBounds.including(projection.fromScreenLocation(nePoint));
    possibleBounds = possibleBounds.including(projection.fromScreenLocation(swPoint));

    //--------------------------
    // debug: draw rectangle
    //--------------------------
//    PolylineOptions polylineOptions = new PolylineOptions();
//    polylineOptions.add(possibleBounds.northeast);
//    polylineOptions.add(new LatLng(possibleBounds.northeast.latitude, possibleBounds.southwest.longitude));
//    polylineOptions.add(possibleBounds.southwest);
//    polylineOptions.add(new LatLng(possibleBounds.southwest.latitude, possibleBounds.northeast.longitude));
//    polylineOptions.add(possibleBounds.northeast);
//    map.addPolyline(polylineOptions);

    //----------------------------------------------------------------
    // Detect the clicked-point is closer to the line or not
    //----------------------------------------------------------------
    LatLng start = null, finish = null;
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
        if (i == 0) {
          start = points.get(0);
          finish = points.get(1);
        } else if (i == points.size() - 1) {
          start = points.get(i - 1);
          finish = points.get(i);
        } else {
          start = points.get(i);
          finish = points.get(i + 1);
        }
        break;
      }
    }

    if (start == null) {
      return null;
    }

    if (start.longitude > finish.longitude) {
      LatLng tmp = start;
      start = finish;
      finish = tmp;
    }

    //--------------------------
    // debug: draw rectangle
    //--------------------------
//    LatLngBounds _targetBounds = new LatLngBounds(start, finish);
//    PolylineOptions polylineOptions2 = new PolylineOptions();
//    polylineOptions2.add(_targetBounds.northeast);
//    polylineOptions2.add(new LatLng(_targetBounds.northeast.latitude, _targetBounds.southwest.longitude));
//    polylineOptions2.add(_targetBounds.southwest);
//    polylineOptions2.add(new LatLng(_targetBounds.southwest.latitude, _targetBounds.northeast.longitude));
//    polylineOptions2.add(_targetBounds.northeast);
//    map.addPolyline(polylineOptions2);


    //----------------------------------------------------------------
    // Calculate waypoints from start to finish on geodesic line
    // @ref http://jamesmccaffrey.wordpress.com/2011/04/17/drawing-a-geodesic-line-for-bing-maps-ajax/
    //----------------------------------------------------------------

    // convert to radians
    double lat1 = start.latitude * (Math.PI / 180.0);
    double lng1 = start.longitude * (Math.PI / 180.0);
    double lat2 = finish.latitude * (Math.PI / 180.0);
    double lng2 = finish.longitude * (Math.PI / 180.0);

    double d = 2 * Math.asin(Math.sqrt(Math.pow((Math.sin((lat1 - lat2) / 2)), 2) +
        Math.cos(lat1) * Math.cos(lat2) * Math.pow((Math.sin((lng1 - lng2) / 2)), 2)));
    List<LatLng> wayPoints = new ArrayList<LatLng>();
    double f = 0.00000000f; // fraction of the curve
    double finc = 0.01000000f; // fraction increment

    while (f <= 1.00000000f) {
      double A = Math.sin((1.0 - f) * d) / Math.sin(d);
      double B = Math.sin(f * d) / Math.sin(d);

      double x = A * Math.cos(lat1) * Math.cos(lng1) + B * Math.cos(lat2) * Math.cos(lng2);
      double y = A * Math.cos(lat1) * Math.sin(lng1) + B * Math.cos(lat2) * Math.sin(lng2);
      double z = A * Math.sin(lat1) + B * Math.sin(lat2);
      double lat = Math.atan2(z, Math.sqrt((x*x) + (y*y)));
      double lng = Math.atan2(y, x);

      LatLng wp = new LatLng(lat / (Math.PI / 180.0), lng / ( Math.PI / 180.0));
      if (possibleBounds.contains(wp)) {
        wayPoints.add(wp);
        //map.addMarker(new MarkerOptions().position(wp));
      }

      f += finc;
    } // while

    // break into waypoints with negative longitudes and those with positive longitudes
    List<LatLng> negLons = new ArrayList<LatLng>(); // lat-lons where the lon part is negative
    List<LatLng> posLons = new ArrayList<LatLng>();
    List<LatLng> connect = new ArrayList<LatLng>();

    for (int i = 0; i < wayPoints.size(); ++i) {
      if (wayPoints.get(i).longitude <= 0.0f)
        negLons.add(wayPoints.get(i));
      else
        posLons.add(wayPoints.get(i));
    }

    // we may have to connect over 0.0 longitude
    for (int i = 0; i < wayPoints.size() - 1; ++i) {
      if (wayPoints.get(i).longitude <= 0.0f && wayPoints.get(i+1).longitude >= 0.0f ||
          wayPoints.get(i).longitude >= 0.0f && wayPoints.get(i+1).longitude <= 0.0f) {
        if (Math.abs(wayPoints.get(i).longitude) + Math.abs(wayPoints.get(i+1).longitude) < 100.0f) {
          connect.add(wayPoints.get(i));
          connect.add(wayPoints.get(i+1));
        }
      }
    }

    ArrayList<LatLng> inspectPoints = new ArrayList<LatLng>();
    if (negLons.size() >= 2) {
      inspectPoints.addAll(negLons);
    }
    if (posLons.size() >= 2) {
      inspectPoints.addAll(posLons);
    }
    if (connect.size() >= 2) {
      inspectPoints.addAll(connect);
    }

    if (inspectPoints.size() == 0) {
      return null;
    }


    double minDistance = 999999999;
    double distance;
    LatLng mostClosePoint = null;

    for (int i = 0; i < inspectPoints.size(); i++) {
      distance = this.calculateDistance(point, inspectPoints.get(i));
      if (distance < minDistance) {
        minDistance = distance;
        mostClosePoint = inspectPoints.get(i);
      }
    }
    return mostClosePoint;
  }

  /**
   * Intersects using the Winding Number Algorithm
   * @ref http://www.nttpc.co.jp/company/r_and_d/technology/number_algorithm.html
   */
  private boolean isPolygonContains(Projection projection, List<LatLng> path, LatLng point) {
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
   */
  private boolean isGroundOverlayContains(GroundOverlay groundOverlay, LatLng point) {
    LatLngBounds groundOverlayBounds = groundOverlay.getBounds();

    return groundOverlayBounds.contains(point);
  }

  @Override
  public boolean onMyLocationButtonClick() {
    String mapId = this.getMapId();
    jsCallback(String.format(Locale.ENGLISH, "javascript:if('%s' in plugin.google.maps){plugin.google.maps['%s']({evtName: 'my_location_button_click', callback:'_onMapEvent'});}",mapId, mapId));
    return false;
  }

  @Override
  public void onMyLocationClick(@NonNull Location location) {
    PluginLocationService.setLastLocation(location);
    String mapId = this.getMapId();
    try {
      JSONObject result = PluginUtil.location2Json(location);
      jsCallback(String.format(Locale.ENGLISH, "javascript:if('%s' in plugin.google.maps){plugin.google.maps['%s']({evtName: 'my_location_click', callback:'_onMapEvent', args: [%s]});}", mapId, mapId, result.toString(0)));
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  /**
   * Notify the myLocationChange event to JS
   */
  private void onCameraEvent(final String eventName) {

    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Projection projection = PluginMap.this.map.getProjection();

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

        String mapId = PluginMap.this.getMapId();

        jsCallback(
            String.format(
                Locale.ENGLISH,
                "javascript:if('%s' in plugin.google.maps){plugin.google.maps['%s']({evtName:'%s', callback:'_onCameraEvent', args: [%s]});}",
                mapId, mapId, eventName, jsonStr));
      }
    });

    if (activeMarker != null) {
      syncInfoWndPosition();
    }
  }

  @Override
  public void onCameraIdle() {
    if (this.isDragging) {
      onMapEvent("map_drag_end");
    }
    this.isDragging = false;
    onCameraEvent("camera_move_end");
  }

  @Override
  public void onCameraMoveCanceled() {
    if (this.isDragging) {
      onMapEvent("map_drag_end");
    }
    this.isDragging = false;
    onCameraEvent("camera_move_end");
  }

  @Override
  public void onCameraMove() {
    if (this.isDragging) {
      onMapEvent("map_drag");
    }
    onCameraEvent("camera_move");
  }

  @Override
  public void onCameraMoveStarted(final int reason) {

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
    String mapId = this.getMapId();
    String jsonStr = "undefined";
    if (building != null) {
      JSONObject result = PluginUtil.convertIndoorBuildingToJson(building);
      if (result != null) {
        jsonStr = result.toString();
      }
    }
    jsCallback(String.format(Locale.ENGLISH, "javascript:if('%s' in plugin.google.maps){plugin.google.maps['%s']({evtName:'indoor_building_focused', callback:'_onMapEvent', args: [%s]});}", mapId, mapId, jsonStr));
  }

  @Override
  public void onIndoorLevelActivated(IndoorBuilding building) {
    String jsonStr = "null";
    String mapId = this.getMapId();
    if (building != null) {
      JSONObject result = PluginUtil.convertIndoorBuildingToJson(building);
      if (result != null) {
        jsonStr = result.toString();
      }
    }
    jsCallback(String.format(Locale.ENGLISH, "javascript:if('%s' in plugin.google.maps){plugin.google.maps['%s']({evtName:'indoor_level_activated', callback:'_onMapEvent', args: [%s]});}", mapId, mapId, jsonStr));
  }
  @Override
  public void onPoiClick(PointOfInterest pointOfInterest) {
    if (!this.clickableIcons) {
      this.onMapClick(pointOfInterest.latLng);
      return;
    }

    String mapId = this.getMapId();
    String js = String.format(Locale.ENGLISH, "javascript:if('%s' in plugin.google.maps){plugin.google.maps['%s']({evtName: '%s', callback:'_onMapEvent', args:['%s', \"%s\", new plugin.google.maps.LatLng(%f, %f)]});}",
    mapId, mapId, "poi_click", pointOfInterest.placeId, pointOfInterest.name, pointOfInterest.latLng.latitude, pointOfInterest.latLng.longitude);
    jsCallback(js);
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
   */
  public void onMapClick(final LatLng point) {

    if (plugins.size() == 0) {
      onMapEvent("map_click", point);
      return;
    }

    Projection projection = map.getProjection();
    Point origin = new Point();
    Point hitArea = new Point();
    hitArea.x = 1;
    hitArea.y = 1;
    LatLng touchPoint = null;
    Object hitOverlay = null;
    float maxZIndex = -1;
    float zIndex = -1;

    //-------------------------------------------------
    // Pick up overlays that contain the given point
    //-------------------------------------------------
    final HashMap<String, Object> boundsHitList = new HashMap<String, Object>();

    // Polyline
    if (plugins.containsKey(getMapId() + "-polyline")) {
      PluginPolyline pluginPolyline = (PluginPolyline) plugins.get(getMapId() + "-polyline");
      for (String polylineId: pluginPolyline.objects.keySet()) {
        MetaPolyline meta = pluginPolyline.objects.get(polylineId);
        if (!meta.isClickable || !meta.isVisible) {
          continue;
        }
        zIndex = meta.polyline.getZIndex();
        if (zIndex < maxZIndex) {
          continue;
        }

        List<LatLng> points = meta.polyline.getPoints();
        if (meta.polyline.isGeodesic()) {
          hitArea.x = (int)(meta.polyline.getWidth() * density);
          hitArea.y = hitArea.x;
          double threshold = calculateDistance(
                  projection.fromScreenLocation(origin),
                  projection.fromScreenLocation(hitArea));
          LatLng polyTouchPoint = isPointOnTheGeodesicLine(projection, points, point, threshold);
          if (polyTouchPoint != null) {
            touchPoint = polyTouchPoint;
            hitOverlay = meta.polyline;
            maxZIndex = zIndex;
          }
        } else {
          LatLng polyTouchPoint = isPointOnTheLine(projection, points, point);
          if (polyTouchPoint != null) {
            touchPoint = polyTouchPoint;
            hitOverlay = meta.polyline;
            maxZIndex = zIndex;
          }
        }
      }
    }

    // Polygon
    if (plugins.containsKey(getMapId() + "-polygon")) {
      PluginPolygon pluginPolygon = (PluginPolygon) plugins.get(getMapId() + "-polygon");
      for (String polygonId : pluginPolygon.objects.keySet()) {
        MetaPolygon meta = pluginPolygon.objects.get(polygonId);
        if (!meta.isClickable || !meta.isVisible) {
          continue;
        }
        zIndex = meta.polygon.getZIndex();
        if (zIndex < maxZIndex) {
          continue;
        }

        if (isPolygonContains(projection, meta.polygon.getPoints(), point)) {
          touchPoint = point;
          hitOverlay = meta.polygon;
          maxZIndex = zIndex;
        }
      }
    }

    // Circle
    if (plugins.containsKey(getMapId() + "-circle")) {
      PluginCircle pluginCircle = (PluginCircle) plugins.get(getMapId() + "-circle");
      for (String circleId : pluginCircle.objects.keySet()) {
        MetaCircle meta = pluginCircle.objects.get(circleId);
        if (!meta.isClickable || !meta.isVisible) {
          continue;
        }
        zIndex = meta.circle.getZIndex();
        if (zIndex < maxZIndex) {
          continue;
        }
        if (isCircleContains(meta.circle, point)) {
          touchPoint = point;
          hitOverlay = meta.circle;
          maxZIndex = zIndex;
        }
      }
    }

    // GroundOverlay
    if (plugins.containsKey(getMapId() + "-groundoverlay")) {
      PluginGroundOverlay pluginGroundOverlay = (PluginGroundOverlay) plugins.get(getMapId() + "-groundoverlay");
      for (String groundOverlayId : pluginGroundOverlay.objects.keySet()) {
        MetaGroundOverlay meta = pluginGroundOverlay.objects.get(groundOverlayId);
        if (!meta.isClickable || !meta.isVisible) {
          continue;
        }
        zIndex = meta.groundOverlay.getZIndex();
        if (zIndex < maxZIndex) {
          continue;
        }
        if (isGroundOverlayContains(meta.groundOverlay, point)) {
          touchPoint = point;
          hitOverlay = meta.groundOverlay;
          maxZIndex = zIndex;
        }
      }
    }


    final Object finalHitOverlay = hitOverlay;
    final LatLng finalTouchPoint = touchPoint;

    //Log.d("PluginMap", "---> hitOverlay = " + finalHitOverlay);
    if (finalHitOverlay instanceof Polygon) {
      onPolygonClick((Polygon)finalHitOverlay, finalTouchPoint);
    } else if (finalHitOverlay instanceof Polyline) {
      onPolylineClick((Polyline)finalHitOverlay, finalTouchPoint);
    } else if (finalHitOverlay instanceof Circle) {
      onCircleClick((Circle)finalHitOverlay, finalTouchPoint);
    } else if (finalHitOverlay != null) {
      onGroundOverlayClick((GroundOverlay)finalHitOverlay, finalTouchPoint);
    } else {
      // Only emit click event if no overlays are hit
      onMapEvent("map_click", point);
    }
  }

  public void onRequestPermissionResult(int requestCode, String[] permissions,
                                        int[] grantResults) throws JSONException {
    //Log.d(TAG, "---> onRequestPermissionResult");

    synchronized (semaphore) {
      semaphore.notify();
    }
  }


}
