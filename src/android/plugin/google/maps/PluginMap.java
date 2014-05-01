package plugin.google.maps;

import java.io.ByteArrayOutputStream;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.Base64;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class PluginMap extends MyPlugin {

  /**
   * Clear all markups
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void clear(JSONArray args, CallbackContext callbackContext) throws JSONException {
    this.map.clear();
    callbackContext.success();
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
        cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 10);
        
      } else {
        latLng = cameraPos.getJSONObject("target");
        builder.target(new LatLng(latLng.getDouble("lat"), latLng.getDouble("lng")));
        newPosition = builder.build();
        cameraUpdate = CameraUpdateFactory.newCameraPosition(newPosition);
      }
    } else {
      callbackContext.error("Error: moveCamera() or animateCamera() are required 'target' option.");
      return;
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
      if (!mapCtrl.locationClient.isConnected()) {
        mapCtrl.locationClient.connect();
      }
    } else {
      if (mapCtrl.locationClient.isConnected()) {
        mapCtrl.locationClient.disconnect();
      }
    }
    callbackContext.success();
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
    callbackContext.success();
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
    callbackContext.success();
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
    
    callbackContext.success();
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
    callbackContext.success();
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
}
