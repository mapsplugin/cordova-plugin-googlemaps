package plugin.google.maps;

import java.io.ByteArrayOutputStream;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CameraPosition.Builder;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.VisibleRegion;

public class PluginMap extends MyPlugin {
  /**
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setOptions(JSONArray args, CallbackContext callbackContext) throws JSONException {

    
    UiSettings settings = this.map.getUiSettings();
    JSONObject params = args.getJSONObject(1);
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
        settings.setMyLocationButtonEnabled(controls.getBoolean("myLocationButton"));
        //map.setMyLocationEnabled(controls.getBoolean("myLocationButton"));
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
        GoogleMapOptions options = new GoogleMapOptions();
        settings.setZoomGesturesEnabled(gestures.getBoolean("zoom"));
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
        this.map.setMapType(mapTypeId);
      }
    }
    
    //controls
    Boolean isEnabled = true;

    // move the camera position
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
      CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(builder.build());
      map.moveCamera(cameraUpdate);
    }
    
    this.sendNoResult(callbackContext);
  }
  
  /**
   * Set center location of the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setCenter(JSONArray args, CallbackContext callbackContext) throws JSONException {
    double lat, lng;

    lat = args.getDouble(1);
    lng = args.getDouble(2);

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
  @SuppressWarnings("unused")
  private void setTilt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float tilt = -1;
    tilt = (float) args.getDouble(1);

    if (tilt > 0 && tilt <= 90) {
      CameraPosition currentPos = map.getCameraPosition();
      CameraPosition newPosition = new CameraPosition.Builder()
          .target(currentPos.target).bearing(currentPos.bearing)
          .zoom(currentPos.zoom).tilt(tilt).build();
      myMoveCamera(newPosition, callbackContext);
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
  @SuppressWarnings("unused")
  private void animateCamera(JSONArray args, CallbackContext callbackContext) throws JSONException {
    this.updateCameraPosition("animateCamera", args, callbackContext);
  }
  
  /**
   * Move the camera without animation
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void moveCamera(JSONArray args, CallbackContext callbackContext) throws JSONException {
    this.updateCameraPosition("moveCamera", args, callbackContext);
  }
  
  /**
   * move the camera
   * @param action
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  private void updateCameraPosition(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    
    int durationMS = 4000;
    CameraPosition.Builder builder = CameraPosition.builder();
    JSONObject cameraPos = args.getJSONObject(1);
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
      durationMS = cameraPos.getInt("duration");
    }
    CameraPosition newPosition;
    CameraUpdate cameraUpdate = null;
    if (cameraPos.has("target")) {
      Object target = cameraPos.get("target");
      @SuppressWarnings("rawtypes")
      Class targetClass = target.getClass();
      JSONObject latLng;
      if ("org.json.JSONArray".equals(targetClass.getName())) {
        JSONArray points = cameraPos.getJSONArray("target");
        LatLngBounds bounds = PluginUtil.JSONArray2LatLngBounds(points);
        cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, (int)(20 * this.density));
        
      } else {
        latLng = cameraPos.getJSONObject("target");
        builder.target(new LatLng(latLng.getDouble("lat"), latLng.getDouble("lng")));
        newPosition = builder.build();
        cameraUpdate = CameraUpdateFactory.newCameraPosition(newPosition);
      }
    } else {
      builder.target(this.map.getCameraPosition().target);
      cameraUpdate = CameraUpdateFactory.newCameraPosition(builder.build());
    }

    
    if (action.equals("moveCamera")) {
      myMoveCamera(cameraUpdate, callbackContext);
    } else {
      myAnimateCamera(cameraUpdate, durationMS, callbackContext);
    }
  }

  /**
   * Set zoom of the map
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setZoom(JSONArray args, CallbackContext callbackContext) throws JSONException {
    Long zoom;
    zoom = args.getLong(1);

    this.myMoveCamera(CameraUpdateFactory.zoomTo(zoom), callbackContext);
  }
  
  /**
   * Move the camera of the map
   * @param cameraPosition
   * @param callbackContext
   */
  private void myMoveCamera(CameraPosition cameraPosition, final CallbackContext callbackContext) {
    CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
    this.myMoveCamera(cameraUpdate, callbackContext);
  }

  /**
   * Move the camera of the map
   * @param cameraUpdate
   * @param callbackContext
   */
  private void myMoveCamera(final CameraUpdate cameraUpdate, final CallbackContext callbackContext) {
    map.moveCamera(cameraUpdate);
    callbackContext.success();
  }

  /**
   * Enable MyLocation feature if set true
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setMyLocationEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Boolean isEnabled = false;
    isEnabled = args.getBoolean(1);
    map.setMyLocationEnabled(isEnabled);
    if (isEnabled) {
      if (!mapCtrl.googleApiClient.isConnected()) {
        mapCtrl.googleApiClient.connect();
      }
    } else {
      if (mapCtrl.googleApiClient.isConnected()) {
        mapCtrl.googleApiClient.disconnect();
      }
    }
    this.sendNoResult(callbackContext);
  }

  /**
   * Enable Indoor map feature if set true
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setIndoorEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Boolean isEnabled = args.getBoolean(1);
    map.setIndoorEnabled(isEnabled);
    this.sendNoResult(callbackContext);
  }

  /**
   * Enable the traffic layer if set true
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setTrafficEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Boolean isEnabled = args.getBoolean(1);
    map.setTrafficEnabled(isEnabled);
    this.sendNoResult(callbackContext);
  }

  /**
   * Enable the compass if set true
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setCompassEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Boolean isEnabled = args.getBoolean(1);
    UiSettings uiSettings = map.getUiSettings();
    uiSettings.setCompassEnabled(isEnabled);
    this.sendNoResult(callbackContext);
  }

  /**
   * Change the map type id of the map
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setMapTypeId(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    int mapTypeId = -1;
    String typeStr = args.getString(1);
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

    if (mapTypeId == -1) {
      callbackContext.error("Unknow MapTypeID is specified:" + typeStr);
      return;
    }
    
    final int myMapTypeId = mapTypeId;
    map.setMapType(myMapTypeId);
    this.sendNoResult(callbackContext);
  }


  /**
   * Move the camera of the map
   * @param cameraUpdate
   * @param durationMS
   * @param callbackContext
   */
  private void myAnimateCamera(final CameraUpdate cameraUpdate, final int durationMS, final CallbackContext callbackContext) {
    GoogleMap.CancelableCallback callback = new GoogleMap.CancelableCallback() {
      @Override
      public void onFinish() {
        callbackContext.success();
      }

      @Override
      public void onCancel() {
        callbackContext.success();
      }
    };

    if (durationMS > 0) {
      map.animateCamera(cameraUpdate, durationMS, callback);
    } else {
      map.animateCamera(cameraUpdate, callback);
    }
  }
  

  /**
   * Return the current position of the camera
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void getCameraPosition(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    CameraPosition camera = map.getCameraPosition();
    JSONObject json = new JSONObject();
    JSONObject latlng = new JSONObject();
    latlng.put("lat", camera.target.latitude);
    latlng.put("lng", camera.target.longitude);
    json.put("target", latlng);
    json.put("zoom", camera.zoom);
    json.put("tilt", camera.tilt);
    json.put("bearing", camera.bearing);
    json.put("hashCode", camera.hashCode());
    
    callbackContext.success(json);
  }
  
  /**
   * Return the image data encoded with base64
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void toDataURL(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    this.map.snapshot(new GoogleMap.SnapshotReadyCallback() {
      
      @Override
      public void onSnapshotReady(Bitmap image) {
        float density = Resources.getSystem().getDisplayMetrics().density;
        image = PluginUtil.resizeBitmap(image,
                                        (int)(image.getWidth() / density),
                                        (int)(image.getHeight() / density));
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();  
        image.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        String imageEncoded = "data:image/png;base64," + 
                Base64.encodeToString(byteArray, Base64.DEFAULT);
        
        callbackContext.success(imageEncoded);
      }
    });
    
  }
  @SuppressWarnings({ "unused", "deprecation" })
  private void fromLatLngToPoint(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    double lat, lng;
    lat = args.getDouble(1);
    lng = args.getDouble(2);
    LatLng latLng = new LatLng(lat, lng);
    Point point = map.getProjection().toScreenLocation(latLng);
    JSONArray pointJSON = new JSONArray();
    pointJSON.put(point.x / webView.getScale());
    pointJSON.put(point.y / webView.getScale());
    callbackContext.success(pointJSON);
  }
  
  @SuppressWarnings("unused")
  private void fromPointToLatLng(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    int pointX, pointY;
    pointX = args.getInt(1);
    pointY = args.getInt(2);
    Point point = new Point();
    point.x = pointX;
    point.y = pointY;
    LatLng latlng = map.getProjection().fromScreenLocation(point);
    JSONArray pointJSON = new JSONArray();
    pointJSON.put(latlng.latitude);
    pointJSON.put(latlng.longitude);
    callbackContext.success(pointJSON);
  }
  
  /**
   * Return the visible region of the map
   * Thanks @fschmidt
   */
  @SuppressWarnings("unused")
  private void getVisibleRegion(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
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
  }
  

  /**
   * Sets the preference for whether all gestures should be enabled or disabled.
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setAllGesturesEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Boolean isEnabled = args.getBoolean(1);
    UiSettings uiSettings = map.getUiSettings();
    uiSettings.setAllGesturesEnabled(isEnabled);
    this.sendNoResult(callbackContext);
  }

}
