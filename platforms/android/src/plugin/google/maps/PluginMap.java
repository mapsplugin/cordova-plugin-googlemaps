package plugin.google.maps;

import java.lang.reflect.Method;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class PluginMap extends CordovaPlugin implements MyPlugin  {
  private final String TAG = "PluginMap";
  public GoogleMap map = null;
  private Activity activity;

  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    Log.d(TAG, "Map class initializing");
    this.activity = cordova.getActivity();
  }
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    String[] params = args.getString(0).split("\\.");
    try {
      Method method = this.getClass().getDeclaredMethod(params[1], JSONArray.class, CallbackContext.class);
      return (Boolean) method.invoke(this, args, callbackContext);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return true;
  }

  public void setMap(GoogleMap map) {
    this.map = map;
  }

  /**
   * Set center location of the marker
   * @param args
   * @param callbackContext
   * @return
   */
  @SuppressWarnings("unused")
  private Boolean setCenter(JSONArray args, CallbackContext callbackContext) {
    double lat, lng;

    try {
      lat = args.getDouble(1);
      lng = args.getDouble(2);
      Log.d(TAG, "lat=" + lat + ", lng=" + lng);
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
      return false;
    }

    LatLng latLng = new LatLng(lat, lng);
    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(latLng);
    this.myMoveCamera(cameraUpdate, callbackContext);
    return true;
  }

  /**
   * Set angle of the map view
   * @param args
   * @param callbackContext
   * @return
   */
  @SuppressWarnings("unused")
  private Boolean setTilt(final JSONArray args, final CallbackContext callbackContext) {
    float tilt = -1;
    try {
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

    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
    }
    return true;
  }

  /**
   * Move the camera with animation
   * @param args
   * @param callbackContext
   * @return
   */
  @SuppressWarnings("unused")
  private Boolean animateCamera(JSONArray args, CallbackContext callbackContext) {
    return this.updateCameraPosition("animateCamera", args, callbackContext);
  }
  
  /**
   * Move the camera without animation
   * @param args
   * @param callbackContext
   * @return
   */
  @SuppressWarnings("unused")
  private Boolean moveCamera(JSONArray args, CallbackContext callbackContext) {
    return this.updateCameraPosition("moveCamera", args, callbackContext);
  }
  
  /**
   * move the camera
   * @param action
   * @param args
   * @param callbackContext
   * @return
   */
  private Boolean updateCameraPosition(final String action, final JSONArray args, final CallbackContext callbackContext) {
    try {
      float tilt, bearing, zoom;
      LatLng target;
      int durationMS = 0;
      JSONObject cameraPos = args.getJSONObject(1);
      CameraPosition currentPos = map.getCameraPosition();
      if (cameraPos.has("tilt")) {
        tilt = (float) cameraPos.getDouble("tilt");
      } else {
        tilt = currentPos.tilt;
      }
      if (cameraPos.has("bearing")) {
        bearing = (float) cameraPos.getDouble("bearing");
      } else {
        bearing = currentPos.bearing;
      }
      if (cameraPos.has("zoom")) {
        zoom = (float) cameraPos.getDouble("zoom");
      } else {
        zoom = currentPos.zoom;
      }
      if (cameraPos.has("lat") && cameraPos.has("lng")) {
        target = new LatLng(cameraPos.getDouble("lat"),
            cameraPos.getDouble("lng"));
      } else {
        target = currentPos.target;
      }

      CameraPosition newPosition = new CameraPosition.Builder()
          .target(target).bearing(bearing).zoom(zoom).tilt(tilt).build();

      if (args.length() == 3) {
        durationMS = args.getInt(2);
      }
      if (action.equals("moveCamera")) {
        myMoveCamera(newPosition, callbackContext);
      } else {
        myAnimateCamera(newPosition, durationMS, callbackContext);
      }
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
    }
    return true;
  }

  /**
   * Set zoom of the map
   * @param args
   * @param callbackContext
   * @return
   */
  @SuppressWarnings("unused")
  private Boolean setZoom(JSONArray args, CallbackContext callbackContext) {
    Long zoom;
    try {
      zoom = args.getLong(1);
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
      return false;
    }

    this.myMoveCamera(CameraUpdateFactory.zoomTo(zoom), callbackContext);
    return true;
  }

  /**
   * Move the camera of the map
   * @param cameraPosition
   * @param callbackContext
   */
  private void myMoveCamera(CameraPosition cameraPosition, final CallbackContext callbackContext) {
    CameraUpdate cameraUpdate = CameraUpdateFactory
        .newCameraPosition(cameraPosition);
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
   * @return
   */
  @SuppressWarnings("unused")
  private Boolean setMyLocationEnabled(final JSONArray args, final CallbackContext callbackContext) {
    Boolean isEnable = false;
    try {
      isEnable = args.getBoolean(1);
      map.setMyLocationEnabled(isEnable);
      callbackContext.success();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
      return false;
    }
  }

  /**
   * Enable Indoor map feature if set true
   * @param args
   * @param callbackContext
   * @return
   */
  @SuppressWarnings("unused")
  private Boolean setIndoorEnabled(final JSONArray args, final CallbackContext callbackContext) {
    Boolean isEnable = false;
    try {
      isEnable = args.getBoolean(1);
      map.setIndoorEnabled(isEnable);
      callbackContext.success();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
      return false;
    }
  }

  /**
   * Enable the traffic layer if set true
   * @param args
   * @param callbackContext
   * @return
   */
  @SuppressWarnings("unused")
  private Boolean setTrafficEnabled(final JSONArray args, final CallbackContext callbackContext) {
    Boolean isEnable = false;
    try {
      isEnable = args.getBoolean(1);
      map.setTrafficEnabled(isEnable);
      callbackContext.success();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
      return false;
    }
  }

  /**
   * This feature is not available for Android
   * @param args
   * @param callbackContext
   * @return
   */
  @SuppressWarnings("unused")
  private Boolean setCompassEnabled(final JSONArray args, final CallbackContext callbackContext) {
    Log.d(TAG, "setCompassEnabled is not available in Android");
    callbackContext.success();
    return true;
  }

  /**
   * Change the map type id of the map
   * @param args
   * @param callbackContext
   * @return
   */
  @SuppressWarnings("unused")
  private Boolean setMapTypeId(JSONArray args, final CallbackContext callbackContext) {
    int mapTypeId = 0;
    try {
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

      if (mapTypeId == 0) {
        callbackContext.error("Unknow MapTypeID is specified:" + typeStr);
        return false;
      }
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
      return false;
    }

    final int myMapTypeId = mapTypeId;
    map.setMapType(myMapTypeId);
    callbackContext.success();
    return true;
  }

  /**
   * Move the camera of the map
   * @param cameraPosition
   * @param durationMS
   * @param callbackContext
   */
  private void myAnimateCamera(CameraPosition cameraPosition, int durationMS, final CallbackContext callbackContext) {
    CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
    this.myAnimateCamera(cameraUpdate, durationMS, callbackContext);
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
}
