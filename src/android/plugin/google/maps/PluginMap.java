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
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnIndoorStateChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLoadedCallback;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.VisibleRegion;

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

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PluginMap extends MyPlugin implements OnMarkerClickListener,
    OnInfoWindowClickListener, OnMapClickListener, OnMapLongClickListener,
    OnCameraChangeListener, OnMapLoadedCallback, OnMarkerDragListener,
    OnMyLocationButtonClickListener, OnIndoorStateChangeListener, InfoWindowAdapter {
  private JSONArray _saveArgs = null;
  private CallbackContext _saveCallbackContext = null;
  private LatLngBounds initCameraBounds;
  private Activity activity;
  public GoogleMap map;
  public MapView mapView;
  public String mapId;
  public final String TAG = mapId;
  public final HashMap<String, PluginEntry> plugins = new HashMap<String, PluginEntry>();

  private enum TEXT_STYLE_ALIGNMENTS {
    left, center, right
  }

  private final int SET_MY_LOCATION_ENABLED = 0x7f999990;  //random
  //private final int SET_MY_LOCATION_ENABLED = 0x7f99991; //random

  private final String ANIMATE_CAMERA_DONE = "animate_camera_done";
  private final String ANIMATE_CAMERA_CANCELED = "animate_camera_canceled";

  private class AsyncUpdateCameraPositionResult {
    CameraUpdate cameraUpdate;
    int durationMS;
    LatLngBounds cameraBounds;
  }

  private class AsyncSetOptionsResult {
    boolean myLocationButtonEnabled;
    int MAP_TYPE_ID;
    CameraPosition cameraPosition;
    LatLngBounds cameraBounds;
  }

  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
    activity = cordova.getActivity();
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
        }
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
    mapCtrl.mapViews.put(mapId, mapView);

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mapView.onCreate(null);
        mapView.onResume();

        //background color
        if (params.has("backgroundColor")) {
          JSONArray rgba = null;
          try {
            rgba = params.getJSONArray("backgroundColor");
          } catch (JSONException e) {
            e.printStackTrace();
            callbackContext.error(e.getMessage() + "");
          }

          if (rgba != null && rgba.length() == 4) {
            try {
              int backgroundColor = PluginUtil.parsePluginColor(rgba);
              mapCtrl.mPluginLayout.setBackgroundColor(backgroundColor);
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }

        }


        mapView.getMapAsync(new OnMapReadyCallback() {
          @Override
          public void onMapReady(GoogleMap googleMap) {

            map = googleMap;

            try {
              //controls
              if (params.has("controls")) {
                JSONObject controls = params.getJSONObject("controls");

                if (controls.has("indoorPicker")) {
                  Boolean isEnabled = controls.getBoolean("indoorPicker");
                  map.setIndoorEnabled(isEnabled);
                }
              }

              // Set event listener
              map.setOnCameraChangeListener(PluginMap.this);
              map.setOnInfoWindowClickListener(PluginMap.this);
              map.setOnMapClickListener(PluginMap.this);
              map.setOnMapLoadedCallback(PluginMap.this);
              map.setOnMapLongClickListener(PluginMap.this);
              map.setOnMarkerClickListener(PluginMap.this);
              map.setOnMarkerDragListener(PluginMap.this);
              map.setOnMyLocationButtonClickListener(PluginMap.this);
              map.setOnIndoorStateChangeListener(PluginMap.this);

              //Custom info window
              map.setInfoWindowAdapter(PluginMap.this);
              // ------------------------------
              // Embed the map if a container is specified.
              // ------------------------------
              if (args.length() == 4) {
                JSONObject mapDivLayoutJSON = args.getJSONObject(2);
                float divW = mapCtrl.contentToView(mapDivLayoutJSON.getLong("width"));
                float divH = mapCtrl.contentToView(mapDivLayoutJSON.getLong("height"));
                float divLeft = mapCtrl.contentToView(mapDivLayoutJSON.getLong("left"));
                float divTop = mapCtrl.contentToView(mapDivLayoutJSON.getLong("top"));

                RectF rect = new RectF(divLeft, divTop, divLeft + divW, divTop + divH);
                mapCtrl.mapDivLayouts.put(mapId, rect);

                mapCtrl.mPluginLayout.addMapView(mapId, mapView);
                PluginMap.this.resizeMap(args, callbackContext);
              } else {
                if (initCameraBounds != null) {
                  Handler handler = new Handler();
                  handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                      fitBounds(initCameraBounds);
                    }
                  }, 300);
                }
              }
              if (params.has("controls")) {
                JSONObject controls = params.getJSONObject("controls");

                if (controls.has("myLocationButton")) {
                  final Boolean isEnabled = controls.getBoolean("myLocationButton");
                  Thread thread = new Thread(new Runnable() {
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
                      } else {
                        callbackContext.success();
                      }
                    }
                  });
                  thread.start();

                } else {
                  callbackContext.success();
                }
              }
              callbackContext.success();
            } catch (Exception e) {
              Log.d(TAG, "------->error");
              callbackContext.error(e.getMessage());
            }
          }
        });

      }
    });

  }

  //-----------------------------------
  // Create the instance of class
  //-----------------------------------
  public void loadPlugin(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String serviceName = args.getString(0);
    String pluginName = mapId + "-" + serviceName.toLowerCase();
    //Log.d("PluginMap", "serviceName = " + serviceName + ", pluginName = " + pluginName);

    if (plugins.containsKey(pluginName)) {
      plugins.get(pluginName).plugin.execute("create", args, callbackContext);
      return;
    }

    try {
      String className = "plugin.google.maps.Plugin" + serviceName;
      Class pluginCls = Class.forName(className);

      CordovaPlugin plugin = (CordovaPlugin) pluginCls.newInstance();
      PluginEntry pluginEntry = new PluginEntry(pluginName, plugin);
      this.plugins.put(pluginName, pluginEntry);
      mapCtrl.pluginManager.addService(pluginEntry);

      plugin.privateInitialize(pluginName, this.cordova, webView, null);

      plugin.initialize(this.cordova, webView);
      ((MyPluginInterface)plugin).setPluginMap(this);
      plugins.get(pluginName).plugin.execute("create", args, callbackContext);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void fitBounds(final LatLngBounds cameraBounds) {
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Builder builder = CameraPosition.builder();
        builder.tilt(map.getCameraPosition().tilt);
        builder.bearing(map.getCameraPosition().bearing);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(cameraBounds, (int)density);
        map.moveCamera(cameraUpdate);
        builder.zoom(map.getCameraPosition().zoom);
        builder.target(map.getCameraPosition().target);
        map.moveCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
      }
    });
  }

  //-----------------------------------
  // Create the instance of class
  //-----------------------------------
  @SuppressWarnings("rawtypes")
  public void create(JSONArray args, CallbackContext callbackContext) throws JSONException {
    final String className = args.getString(0);

    if (plugins.containsKey(className)) {
      PluginEntry pluginEntry = plugins.get(className);
      pluginEntry.plugin.execute("create", args, callbackContext);
      return;
    }

    try {
      Class pluginCls = Class.forName("plugin.google.maps.Plugin" + className);

      CordovaPlugin plugin = (CordovaPlugin) pluginCls.newInstance();
      PluginEntry pluginEntry = new PluginEntry(mapId + "-" + className, plugin);
      this.plugins.put(className, pluginEntry);
      pluginMap.mapCtrl.pluginManager.addService(pluginEntry);

      plugin.privateInitialize(className, this.cordova, webView, null);
      plugin.initialize(this.cordova, webView);
      ((MyPluginInterface)plugin).setPluginMap(this);
      pluginEntry.plugin.execute("create", args, callbackContext);


    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void resizeMap(JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (mapCtrl.mPluginLayout == null) {
      //Log.d("PluginMap", "---> resizeMap / mPluginLayout = null");
      callbackContext.success();
      if (initCameraBounds != null) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {
            fitBounds(initCameraBounds);
          }
        }, 100);
      }
      return;
    }
    final JSONObject mapDivLayoutJSON = args.getJSONObject(args.length() - 2);
    RectF rectF;
    if (mapCtrl.mapDivLayouts.containsKey(mapId)) {
      rectF = mapCtrl.mapDivLayouts.get(mapId);
    } else {
      rectF = new RectF();
    }
    rectF.left = mapCtrl.contentToView(mapDivLayoutJSON.getLong("left"));
    rectF.top = mapCtrl.contentToView(mapDivLayoutJSON.getLong("top"));
    rectF.right = rectF.left + mapCtrl.contentToView(mapDivLayoutJSON.getLong("width"));
    rectF.bottom = rectF.top + mapCtrl.contentToView(mapDivLayoutJSON.getLong("height"));
    mapCtrl.mapDivLayouts.put(mapId, rectF);
    //Log.d("PluginMap", "---> resizeMap / rectF = " + rectF.left + "," + rectF.top + "," + rectF.width() + "," + rectF.height());

    JSONArray HTMLs = args.getJSONArray(args.length() - 1);
    JSONObject elemInfo, elemSize;
    String elemId;
    float divW, divH, divLeft, divTop;
    if (mapCtrl.mPluginLayout == null) {
      this.sendNoResult(callbackContext);
      if (initCameraBounds != null) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {
            fitBounds(initCameraBounds);
          }
        }, 100);
      }
      return;
    }
    mapCtrl.mPluginLayout.clearHTMLElement(mapId);

    for (int i = 0; i < HTMLs.length(); i++) {
      elemInfo = HTMLs.getJSONObject(i);
      try {
        elemId = elemInfo.getString("id");
        elemSize = elemInfo.getJSONObject("size");

        divW = mapCtrl.contentToView(elemSize.getLong("width"));
        divH = mapCtrl.contentToView(elemSize.getLong("height"));
        divLeft = mapCtrl.contentToView(elemSize.getLong("left"));
        divTop = mapCtrl.contentToView(elemSize.getLong("top"));
        mapCtrl.mPluginLayout.putHTMLElement(mapId, elemId, divLeft, divTop, divLeft + divW, divTop + divH);
      } catch (Exception e){
        e.printStackTrace();
      }
    }
    mapCtrl.updateMapViewLayout();
    //mapCtrl.mPluginLayout.inValidate();
    if (initCameraBounds != null) {
      Handler handler = new Handler();
      handler.postDelayed(new Runnable() {
        @Override
        public void run() {
          fitBounds(initCameraBounds);
        }
      }, 100);
    }
    this.sendNoResult(callbackContext);
  }

  public void setDiv(JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (args.length() == 0) {
      mapCtrl.mapDivLayouts.remove(mapId);
      mapCtrl.mPluginLayout.removeMapView(mapId);
      this.sendNoResult(callbackContext);
      return;
    }
    if (args.length() == 2) {
      mapCtrl.mPluginLayout.addMapView(mapId, mapView);
      this.resizeMap(args, callbackContext);
    }
  }

  /**
   * Set visibility of the map
   * @param args Parameters given from JavaScript side
   * @param callbackContext Callback contect for sending back the result.
   * @throws JSONException
   */
  public void setVisible(JSONArray args, CallbackContext callbackContext) throws JSONException {
    boolean visible = args.getBoolean(0);
    if (visible) {
      mapView.setVisibility(View.VISIBLE);
    } else {
      mapView.setVisibility(View.INVISIBLE);
    }
    this.sendNoResult(callbackContext);
  }


  /**
   * Destroy the map completely
   * @param args Parameters given from JavaScript side
   * @param callbackContext Callback contect for sending back the result.
   */
  public void remove(JSONArray args, CallbackContext callbackContext) {
    mapCtrl.mPluginLayout.setClickable(false);
    mapCtrl.mPluginLayout.removeMapView(mapId);
    plugins.clear();
    mapCtrl.mapPlugins.remove(mapId);
    if (map != null) {
      map.setMyLocationEnabled(false);
      map.clear();
    }
    if (mapView != null) {
      mapView.onDestroy();
    }
    map = null;
    mapView = null;
    System.gc();
    this.sendNoResult(callbackContext);
  }


  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  @Override
  public View getInfoContents(Marker marker) {
    String title = marker.getTitle();
    String snippet = marker.getSnippet();
    if ((title == null) && (snippet == null)) {
      return null;
    }

    PluginManager pluginManager = this.webView.getPluginManager();
    PluginMarker pluginMarker = (PluginMarker)pluginManager.getPlugin("Marker");

    JSONObject properties = null;
    JSONObject styles = null;
    String propertyId = "marker_property_" + marker.getId();
    if (pluginMarker.objects.containsKey(propertyId)) {
      properties = (JSONObject) pluginMarker.objects.get(propertyId);

      if (properties.has("styles")) {
        try {
          styles = (JSONObject) properties.getJSONObject("styles");
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
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
      } catch (Exception e) {}

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
      } catch (Exception e) {}
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
        String[] tmp = title.split(",");
        Bitmap image = PluginUtil.getBitmapFromBase64encodedImage(tmp[1]);
        image = PluginUtil.scaleBitmapForDevice(image);
        ImageView imageView = new ImageView(this.cordova.getActivity());
        imageView.setImageBitmap(image);

        if (maxWidth > 0) {
          imageView.setMaxWidth(maxWidth);
          imageView.setAdjustViewBounds(true);
        }

        windowLayer.addView(imageView);
      } else {
        TextView textView = new TextView(this.cordova.getActivity());
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
      TextView textView2 = new TextView(this.cordova.getActivity());
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

        try {
          JSONObject params = args.getJSONObject(0);


          // map type
          results.MAP_TYPE_ID = -1;
          if (params.has("mapType")) {
            String typeStr = params.getString("mapType");
            results.MAP_TYPE_ID = typeStr.equals("MAP_TYPE_NORMAL") ? GoogleMap.MAP_TYPE_NORMAL : results.MAP_TYPE_ID;
            results.MAP_TYPE_ID = typeStr.equals("MAP_TYPE_HYBRID") ? GoogleMap.MAP_TYPE_HYBRID : results.MAP_TYPE_ID;
            results.MAP_TYPE_ID = typeStr.equals("MAP_TYPE_SATELLITE") ? GoogleMap.MAP_TYPE_SATELLITE : results.MAP_TYPE_ID;
            results.MAP_TYPE_ID = typeStr.equals("MAP_TYPE_TERRAIN") ? GoogleMap.MAP_TYPE_TERRAIN : results.MAP_TYPE_ID;
            results.MAP_TYPE_ID = typeStr.equals("MAP_TYPE_NONE") ? GoogleMap.MAP_TYPE_NONE : results.MAP_TYPE_ID;
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
          map.moveCamera(CameraUpdateFactory.newCameraPosition(results.cameraPosition));
          if (results.cameraBounds != null) {
            fitBounds(results.cameraBounds);
          }
        }
        if (results.MAP_TYPE_ID != -1) {
          map.setMapType(results.MAP_TYPE_ID);
        }


        JSONObject params = null;
        try {
          params = args.getJSONObject(0);

          //background color
          if (params.has("backgroundColor")) {
            JSONArray rgba = null;
            try {
              rgba = params.getJSONArray("backgroundColor");
            } catch (JSONException e) {
              e.printStackTrace();
              callbackContext.error(e.getMessage() + "");
            }

            int backgroundColor = Color.WHITE;
            if (rgba != null && rgba.length() == 4) {
              try {
                backgroundColor = PluginUtil.parsePluginColor(rgba);
                mapCtrl.mPluginLayout.setBackgroundColor(backgroundColor);
              } catch (JSONException e) {
                e.printStackTrace();
              }
            }
          }

          UiSettings settings = map.getUiSettings();

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
            if (controls.has("myLocationButton")) {
              boolean isEnabled = controls.getBoolean("myLocationButton");
              settings.setMyLocationButtonEnabled(isEnabled);

              JSONArray args = new JSONArray();
              args.put(isEnabled);
              PluginMap.this.setMyLocationEnabled(args, callbackContext);
            } else {
              sendNoResult(callbackContext);
            }
          } else {
            sendNoResult(callbackContext);
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
    IndoorBuilding focusedBuilding = map.getFocusedBuilding();
    if (focusedBuilding != null) {
      JSONObject result = PluginUtil.convertIndoorBuildingToJson(focusedBuilding);
      callbackContext.success(result);
    } else {
      callbackContext.success(-1);
    }
  }

  /**
   * Set center location of the marker
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setCenter(JSONArray args, CallbackContext callbackContext) throws JSONException {
    double lat = args.getDouble(0);
    double lng = args.getDouble(1);

    LatLng latLng = new LatLng(lat, lng);
    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(latLng);
    this.myMoveCamera(cameraUpdate, callbackContext);
  }

  /**
   * Set angle of the map view
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setTilt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float tilt = (float) args.getDouble(1);

    if (tilt > 0 && tilt <= 90) {
      final float finalTilt = tilt;
      cordova.getActivity().runOnUiThread(new Runnable() {
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

    final CameraPosition.Builder builder = CameraPosition.builder();
    final JSONObject cameraPos = args.getJSONObject(0);

    AsyncTask<Void, Void, AsyncUpdateCameraPositionResult> createCameraUpdate = new AsyncTask<Void, Void, AsyncUpdateCameraPositionResult>() {
      private Exception mException = null;

      @Override
      protected AsyncUpdateCameraPositionResult doInBackground(Void... voids) {
        AsyncUpdateCameraPositionResult result = new AsyncUpdateCameraPositionResult();
        try {
          result.durationMS = 4000;
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
            result.cameraUpdate = CameraUpdateFactory.newLatLngBounds(result.cameraBounds, (int)(20 * density));
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
        if (AsyncUpdateCameraPositionResult.cameraUpdate == null) {
          builder.target(map.getCameraPosition().target);
          AsyncUpdateCameraPositionResult.cameraUpdate = CameraUpdateFactory.newCameraPosition(builder.build());
        }

        final LatLngBounds finalCameraBounds = AsyncUpdateCameraPositionResult.cameraBounds;
        PluginUtil.MyCallbackContext myCallback = new PluginUtil.MyCallbackContext("moveCamera", webView) {
          @Override
          public void onResult(final PluginResult pluginResult) {
            if (finalCameraBounds != null && ANIMATE_CAMERA_DONE.equals(pluginResult.getStrMessage())) {
              CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(finalCameraBounds, (int)density);
              map.moveCamera(cameraUpdate);


              Builder builder = CameraPosition.builder();
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
              builder.zoom(map.getCameraPosition().zoom);
              builder.target(map.getCameraPosition().target);
              map.moveCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
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

  /**
   * Set zoom of the map
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setZoom(JSONArray args, CallbackContext callbackContext) throws JSONException {
    Long zoom = args.getLong(0);
    this.myMoveCamera(CameraUpdateFactory.zoomTo(zoom), callbackContext);
  }

  /**
   * Pan by the specified pixel
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void panBy(JSONArray args, CallbackContext callbackContext) throws JSONException {
    int x = args.getInt(0);
    int y = args.getInt(1);
    float xPixel = -x * this.density;
    float yPixel = -y * this.density;
    final CameraUpdate cameraUpdate = CameraUpdateFactory.scrollBy(xPixel, yPixel);

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        map.animateCamera(cameraUpdate);
      }
    });
  }

  /**
   * Move the camera of the map
   * @param cameraPosition
   * @param callbackContext
   */
  public void myMoveCamera(CameraPosition cameraPosition, final CallbackContext callbackContext) {
    CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
    this.myMoveCamera(cameraUpdate, callbackContext);
  }

  /**
   * Move the camera of the map
   * @param cameraUpdate
   * @param callbackContext
   */
  public void myMoveCamera(final CameraUpdate cameraUpdate, final CallbackContext callbackContext) {
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        map.moveCamera(cameraUpdate);
        callbackContext.success();
      }
    });
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
    switch (requestCode) {
      case SET_MY_LOCATION_ENABLED:
        this.setMyLocationEnabled(_saveArgs, _saveCallbackContext);
        break;

      default:
        break;
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
    boolean locationPermission = false;
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
      mapCtrl.requestPermissions(this, SET_MY_LOCATION_ENABLED, new String[]{"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"});
      return;
    }

    final Boolean isEnabled = args.getBoolean(0);
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        map.setMyLocationEnabled(isEnabled);
        map.getUiSettings().setMyLocationButtonEnabled(isEnabled);
        sendNoResult(callbackContext);
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
  public void clear(JSONArray args, CallbackContext callbackContext) throws JSONException {
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

    this.map.clear();
    this.sendNoResult(callbackContext);
  }

  /**
   * Enable Indoor map feature if set true
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setIndoorEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final Boolean isEnabled = args.getBoolean(0);
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        map.setIndoorEnabled(isEnabled);
        sendNoResult(callbackContext);
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
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        map.setTrafficEnabled(isEnabled);
        sendNoResult(callbackContext);
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
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setCompassEnabled(isEnabled);
        sendNoResult(callbackContext);
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
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        map.setMapType(myMapTypeId);
        sendNoResult(callbackContext);
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

    cordova.getActivity().runOnUiThread(new Runnable() {
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
    cordova.getActivity().runOnUiThread(new Runnable() {
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

    cordova.getActivity().runOnUiThread(new Runnable() {
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
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Point point = map.getProjection().toScreenLocation(latLng);
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
    int pointX, pointY;
    pointX = args.getInt(0);
    pointY = args.getInt(1);
    final Point point = new Point();
    point.x = pointX;
    point.y = pointY;
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {

        LatLng latlng = map.getProjection().fromScreenLocation(point);
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
   * Return the visible region of the map
   * Thanks @fschmidt
   */
  public void getVisibleRegion(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          VisibleRegion visibleRegion = map.getProjection().getVisibleRegion();
          LatLngBounds latLngBounds = visibleRegion.latLngBounds;
          JSONObject result = new JSONObject();
          JSONObject northeast = new JSONObject();
          JSONObject southwest = new JSONObject();
          northeast.put("lat", latLngBounds.northeast.latitude);
          northeast.put("lng", latLngBounds.northeast.longitude);
          southwest.put("lat", latLngBounds.southwest.latitude);
          southwest.put("lng", latLngBounds.southwest.longitude);
          result.put("northeast", northeast);
          result.put("southwest", southwest);

          JSONArray latLngArray = new JSONArray();
          latLngArray.put(northeast);
          latLngArray.put(southwest);
          result.put("latLngArray", latLngArray);

          callbackContext.success(result);
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
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setAllGesturesEnabled(isEnabled);
        sendNoResult(callbackContext);
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
    final int left = padding.getInt("left");
    final int top = padding.getInt("top");
    final int bottom = padding.getInt("bottom");
    final int right = padding.getInt("right");
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        map.setPadding(left, top, right, bottom);
        sendNoResult(callbackContext);
      }
    });
  }


  @Override
  public boolean onMarkerClick(Marker marker) {
    this.onMarkerEvent("click", marker);

    JSONObject properties = null;
    String propertyId = "marker_property_" + marker.getId();
    PluginEntry pluginEntry = plugins.get(mapId + "::Marker");
    PluginMarker pluginMarker = (PluginMarker)pluginEntry.plugin;
    if (pluginMarker.objects.containsKey(propertyId)) {
      properties = (JSONObject) pluginMarker.objects.get(propertyId);
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
   */
  public void onMapEvent(final String eventName) {
    String js = String.format(Locale.ENGLISH, "javascript:cordova.fireDocumentEvent('%s', callback:'_onMapEvent', {evtName: '%s'})",
        mapId, eventName);
    jsCallback(js);
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
    String markerId = "marker_" + marker.getId();
    String js = String.format(Locale.ENGLISH, "javascript:cordova.fireDocumentEvent('%s', {evtName: '%s', callback:'_onMarkerEvent', args:['%s']})",
        mapId, eventName, markerId);
    jsCallback(js);
  }
  public void onOverlayEvent(String eventName, String overlayId, LatLng point) {
    String js = String.format(Locale.ENGLISH, "javascript:cordova.fireDocumentEvent('%s', {evtName: '%s', callback:'_onOverlayEvent', args:['%s', new plugin.google.maps.LatLng(%f, %f)]})",
        mapId, eventName, overlayId, point.latitude, point.longitude);
    jsCallback(js);
  }
  public void onPolylineClick(Polyline polyline, LatLng point) {
    String overlayId = "polyline_" + polyline.getId();
    this.onOverlayEvent("overlay_click", overlayId, point);
  }
  public void onPolygonClick(Polygon polygon, LatLng point) {
    String overlayId = "polygon_" + polygon.getId();
    this.onOverlayEvent("overlay_click", overlayId, point);
  }
  public void onCircleClick(Circle circle, LatLng point) {
    String overlayId = "circle_" + circle.getId();
    this.onOverlayEvent("overlay_click", overlayId, point);
  }
  public void onGroundOverlayClick(GroundOverlay groundOverlay, LatLng point) {
    String overlayId = "groundOverlay_" + groundOverlay.getId();
    this.onOverlayEvent("overlay_click", overlayId, point);
  }


  /**
   * Notify map event to JS
   * @param eventName
   * @param point
   */
  public void onMapEvent(final String eventName, final LatLng point) {
    String js = String.format(Locale.ENGLISH, "javascript:cordova.fireDocumentEvent('%s', {evtName: '%s', callback:'_onMapEvent', args:[new plugin.google.maps.LatLng(%f, %f)]})",
        mapId, eventName, point.latitude, point.longitude);
    jsCallback(js);
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
   * Intersects using the Winding Number Algorithm
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
    jsCallback(String.format(Locale.ENGLISH, "javascript:cordova.fireDocumentEvent('%s',{evtName: 'my_location_button_click', callback:'_onMapEvent'})", mapId));
    return false;
  }


  @Override
  public void onMapLoaded() {
    jsCallback(String.format(Locale.ENGLISH, "javascript:cordova.fireDocumentEvent('%s', {evtName: 'map_loaded', callback:'_onMapEvent'})", mapId));
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
      params.put("tilt", position.tilt);
      params.put("zoom", position.zoom);

      JSONObject target = new JSONObject();
      target.put("lat", position.target.latitude);
      target.put("lng", position.target.longitude);
      params.put("target", target);
      jsonStr = params.toString();
    } catch (JSONException e) {
      e.printStackTrace();
    }

    jsCallback(String.format(Locale.ENGLISH, "javascript:cordova.fireDocumentEvent('%s', {evtName:'camera_change', callback:'_onCameraEvent', args: [%s]})", mapId, jsonStr));
  }

  @Override
  public void onIndoorBuildingFocused() {
    jsCallback(String.format(Locale.ENGLISH, "javascript:cordova.fireDocumentEvent('%s', {evtName:'indoor_building_focused', callback:'_onMapEvent'})", mapId));
  }

  @Override
  public void onIndoorLevelActivated(IndoorBuilding building) {
    String jsonStr = "null";
    JSONObject result = PluginUtil.convertIndoorBuildingToJson(building);
    if (result != null) {
      jsonStr = result.toString();
    }
    jsCallback(String.format(Locale.ENGLISH, "javascript:cordova.fireDocumentEvent('%s', {evtName:'indoor_level_activated', callback:'_onMapEvent', args: [%s]})", mapId, jsonStr));
  }

  private void jsCallback(final String js) {
    cordova.getActivity().runOnUiThread(new Runnable() {
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
    final PluginManager pluginManager = this.webView.getPluginManager();

    AsyncTask<Void, Void, HashMap<String, Object>> task = new AsyncTask<Void, Void, HashMap<String, Object>>() {
      @Override
      protected HashMap<String, Object> doInBackground(Void... voids) {
        String key;
        LatLngBounds bounds;
        HashMap<String, Object> results = new HashMap<String, Object>();

        // Polyline
        PluginPolyline polylineClass = (PluginPolyline) pluginManager.getPlugin("Polyline");
        Log.d(TAG, "Polyline = " + polylineClass);
        if (polylineClass != null) {
          for (HashMap.Entry<String, Object> entry : polylineClass.objects.entrySet()) {
            key = entry.getKey();
            Log.d(TAG, "key = " + key);
            if (key.contains("polyline_bounds_")) {
              bounds = (LatLngBounds) entry.getValue();
              if (bounds.contains(point)) {
                key = key.replace("bounds_", "");
                results.put(key, polylineClass.getPolyline(key));
              }
            }
          }
        }

        // Loop through all polygons to check if within the touch point
        PluginPolygon polygonPlugin = (PluginPolygon) pluginManager.getPlugin("Polygon");
        if (polygonPlugin != null) {
          for (HashMap.Entry<String, Object> entry : polygonPlugin.objects.entrySet()) {
            key = entry.getKey();
            Log.d("Polygon", "--key = " + key);
            if (key.contains("polygon_bounds_")) {
              bounds = (LatLngBounds) entry.getValue();
              if (bounds.contains(point)) {
                key = key.replace("_bounds", "");
                results.put(key, polygonPlugin.getPolygon(key));
              }
            }
          }
        }

        return results;
      }

      @Override
      public void onPostExecute(HashMap<String, Object> boundsHitList) {
        boolean hitPoly = false;
        String key;
        Map.Entry<String, Object> entry;

        Set<Map.Entry<String, Object>> entrySet = boundsHitList.entrySet();
        Iterator<Map.Entry<String, Object>> iterator = entrySet.iterator();


        List<LatLng> points ;
        Polyline polyline;
        Polygon polygon;
        Point origin = new Point();
        Point hitArea = new Point();
        hitArea.x = 1;
        hitArea.y = 1;
        Projection projection = map.getProjection();
        double threshold = calculateDistance(
            projection.fromScreenLocation(origin),
            projection.fromScreenLocation(hitArea));


        while(iterator.hasNext()) {
          entry = iterator.next();
          key = entry.getKey();
          Log.d(TAG, key);

          if (key.startsWith("polyline")) {

            polyline = ((Polyline)entry.getValue());
            points = polyline.getPoints();

            if (polyline.isGeodesic()) {
              if (isPointOnTheGeodesicLine(points, point, threshold)) {
                hitPoly = true;
                onPolylineClick(polyline, point);
                break;
              }
            } else {
              if (isPointOnTheLine(points, point)) {
                hitPoly = true;
                onPolylineClick(polyline, point);
                break;
              }
            }

          }

          if (key.startsWith("polygon")) {
            polygon = ((Polygon)entry.getValue());
            if (isPolygonContains(polygon.getPoints(), point)) {
              hitPoly = true;
              onPolygonClick(polygon, point);
              break;
            }
          }
        }
        if (!hitPoly) {
          // Only emit click event if no overlays hit
          onMapEvent("click", point);
        }

      }
    };
    task.execute();


/*
        // Polyline
        PluginEntry polylinePlugin = plugins.get("Polyline");
        Log.d(TAG, "--polyline = " + polylinePlugin);
        if (polylinePlugin != null) {
          PluginPolyline polylineClass = (PluginPolyline) polylinePlugin.plugin;

          List<LatLng> points ;
          Polyline polyline;
          Point origin = new Point();
          Point hitArea = new Point();
          hitArea.x = 1;
          hitArea.y = 1;
          Projection projection = map.getProjection();
          double threshold = calculateDistance(
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
                  if (isPointOnTheGeodesicLine(points, point, threshold)) {
                    hitPoly = true;
                    onPolylineClick(polyline, point);
                  }
                } else {
                  if (isPointOnTheLine(points, point)) {
                    hitPoly = true;
                    onPolylineClick(polyline, point);
                  }
                }
              }
            }
          }
          if (hitPoly) {
            return;
          }
        }
*/


    /*
        PluginEntry polygonPlugin = plugins.get("Polygon");
        Log.d(TAG, "--Polygon = " + polygonPlugin.plugin);
        if (polygonPlugin != null) {
          PluginPolygon polygonClass = (PluginPolygon) polygonPlugin.plugin;

          Log.d(TAG, "--key = " + polygonClass.objects.entrySet());
          for (HashMap.Entry<String, Object> entry : polygonClass.objects.entrySet()) {
            key = entry.getKey();
            Log.d(TAG, "--key = " + key);
            if (key.contains("polygon_bounds_")) {
              bounds = (LatLngBounds) entry.getValue();
              if (bounds.contains(point)) {

                key = key.replace("_bounds", "");
                Polygon polygon = polygonClass.getPolygon(key);

                if (isPolygonContains(polygon.getPoints(), point)) {
                  hitPoly = true;
                  onPolygonClick(polygon, point);
                }
              }
            }
          }
          if (hitPoly) {
            return;
          }
        }

        // Loop through all circles to check if within the touch point
        PluginEntry circlePlugin = plugins.get("Circle");
        Log.d(TAG, "--circlePlugin = " + circlePlugin);
        if (circlePlugin != null) {
          PluginCircle circleClass = (PluginCircle) circlePlugin.plugin;

          for (HashMap.Entry<String, Object> entry : circleClass.objects.entrySet()) {
            Circle circle = (Circle) entry.getValue();
            if (isCircleContains(circle, point)) {
              hitPoly = true;
              onCircleClick(circle, point);
            }
          }
          if (hitPoly) {
            return;
          }
        }

        // Loop through ground overlays to check if within the touch point
        PluginEntry groundOverlayPlugin = plugins.get("GroundOverlay");
        Log.d(TAG, "--GroundOverlay = " + groundOverlayPlugin);
        if (groundOverlayPlugin != null) {
          PluginGroundOverlay groundOverlayClass = (PluginGroundOverlay) groundOverlayPlugin.plugin;

          for (HashMap.Entry<String, Object> entry : groundOverlayClass.objects.entrySet()) {
            key = entry.getKey();
            if (key.contains("groundOverlay_")) {
              GroundOverlay groundOverlay = (GroundOverlay) entry.getValue();
              if (isGroundOverlayContains(groundOverlay, point)) {
                hitPoly = true;
                onGroundOverlayClick(groundOverlay, point);
              }
            }
          }
          if (hitPoly) {
            return;
          }
        }

        // Only emit click event if no overlays hit
        onMapEvent("click", point);
*/

  }
}
