package plugin.google.maps;

import java.lang.reflect.Method;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class Map extends CordovaPlugin implements MyPlugin  {
  private final String TAG = "MapClass";
  public GoogleMap map = null;
  private Activity activity;

  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    Log.d(TAG, "Map class initializing");
  }
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    String[] params = args.getString(0).split("\\.");
    Log.d(TAG, "method = " + params[1]);
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
    Log.d(TAG, "Map.setMap");
  }
  
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

  private void myMoveCamera(CameraPosition cameraPosition, final CallbackContext callbackContext) {
    CameraUpdate cameraUpdate = CameraUpdateFactory
        .newCameraPosition(cameraPosition);
    this.myMoveCamera(cameraUpdate, callbackContext);
  }

  private void myMoveCamera(final CameraUpdate cameraUpdate, final CallbackContext callbackContext) {
    map.moveCamera(cameraUpdate);
    callbackContext.success();
  }

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

  private Boolean setCompassEnabled(final JSONArray args, final CallbackContext callbackContext) {
    Log.d(TAG, "setCompassEnabled is not available in Android");
    callbackContext.success();
    return true;
  }

}
