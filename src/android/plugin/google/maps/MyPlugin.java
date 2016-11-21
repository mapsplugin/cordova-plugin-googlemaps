package plugin.google.maps;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.content.res.Resources;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.TileOverlay;

public class MyPlugin extends CordovaPlugin implements MyPluginInterface  {
  protected HashMap<String, Object> objects;
  protected HashMap<String, Boolean> tappables;

  public GoogleMaps mapCtrl = null;
  public GoogleMap map = null;
  public float density = Resources.getSystem().getDisplayMetrics().density;
  
  public void setMapCtrl(GoogleMaps mapCtrl) {
    this.mapCtrl = mapCtrl;
    this.map = mapCtrl.map;
  }
  
  @SuppressLint("UseSparseArrays")
  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
    this.objects = new HashMap<String, Object>();
    this.tappables = new HashMap<String, Boolean>();
  }
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    String[] params = args.getString(0).split("\\.");
    try {
      Method method = this.getClass().getDeclaredMethod(params[1], JSONArray.class, CallbackContext.class);
      method.setAccessible(true);
      method.invoke(this, args, callbackContext);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
      return false;
    }
  }
  
  protected boolean isTappable(String id) {
    return this.tappables.containsKey(id);
  }

  protected Circle getCircle(String id) {
    return (Circle)this.objects.get(id);
  }


  protected GroundOverlay getGroundOverlay(String id) {
    return (GroundOverlay)this.objects.get(id);
  }

  protected Marker getMarker(String id) {
    return (Marker)this.objects.get(id);
  }
  protected Polyline getPolyline(String id) {
    return (Polyline)this.objects.get(id);
  }
  protected Polygon getPolygon(String id) {
    return (Polygon)this.objects.get(id);
  }
  protected TileOverlay getTileOverlay(String id) {
    return (TileOverlay)this.objects.get(id);
  }

  protected void setInt(String methodName, String id, int value, final CallbackContext callbackContext) throws JSONException {
    this.setValue(methodName, int.class, id, value, callbackContext);
  }
  protected void setFloat(String methodName, String id, float value, final CallbackContext callbackContext) throws JSONException {
    this.setValue(methodName, float.class, id, value, callbackContext);
  }
  protected void setDouble(String methodName, String id, float value, final CallbackContext callbackContext) throws JSONException {
    this.setValue(methodName, double.class, id, value, callbackContext);
  }
  protected void setString(String methodName, String id, String value, final CallbackContext callbackContext) throws JSONException {
    this.setValue(methodName, String.class, id, value, callbackContext);
  }

  protected void setBoolean(String methodName, String id, Boolean value, final CallbackContext callbackContext) throws JSONException {
    this.setValue(methodName, boolean.class, id, value, callbackContext);
  }
  
  private void setValue(String methodName, Class<?> methodClass, String id, Object value, final CallbackContext callbackContext) throws JSONException {
    Object object = this.objects.get(id);
    try {
      Method method = object.getClass().getDeclaredMethod(methodName, methodClass);
      method.invoke(object, value);
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
    }
    this.sendNoResult(callbackContext);
  }
  public void clear() {
    this.objects.clear();
  }
  
  protected void sendNoResult(CallbackContext callbackContext) {
    PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
    pluginResult.setKeepCallback(true);
    callbackContext.sendPluginResult(pluginResult);
  }
}
