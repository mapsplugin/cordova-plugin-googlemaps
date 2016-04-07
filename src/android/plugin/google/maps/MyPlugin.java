package plugin.google.maps;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.TileOverlay;

public class MyPlugin extends CordovaPlugin implements MyPluginInterface  {
  protected HashMap<String, Object> objects;
  public MyPlugin self = null;
  public final HashMap<String, Method> methods = new HashMap<String, Method>();

  public GoogleMaps mapCtrl = null;
  public GoogleMap map = null;
  public float density = Resources.getSystem().getDisplayMetrics().density;

  public void setMapCtrl(GoogleMaps mapCtrl) {
    this.mapCtrl = mapCtrl;
    this.map = mapCtrl.map;
  }
  protected String TAG = "";

  @SuppressLint("UseSparseArrays")
  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
    this.objects = new HashMap<String, Object>();
    TAG = this.getServiceName();
  }
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    /*
    if ("Map.animateCamera".equals(args.getString(0))) {
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
    */


    if (methods.size() == 0) {
      PluginManager manager = this.webView.getPluginManager();
      GoogleMaps googleMaps = (GoogleMaps) manager.getPlugin("GoogleMaps");
      if (googleMaps.isDebug) {
        if (args != null && args.length() > 0) {
          Log.d(TAG, "(debug)action=" + action + " args[0]=" + args.getString(0));
        } else {
          Log.d(TAG, "(debug)action=" + action);
        }
      }
      self = this;
      this.mapCtrl = googleMaps;
      //googleMaps.loadPlugin(this.getServiceName());
      //self = (MyPlugin) googleMaps.plugins.get(this.getServiceName()).plugin;
      this.map = this.mapCtrl.map;
      Log.d("MyPlugin", "this = " + this);
      self.map = googleMaps.map;
      self.mapCtrl = googleMaps;
      Log.d("MyPlugin", "self = " + self);

      Method[] classMethods = self.getClass().getMethods();
      for (int i = 0; i < classMethods.length; i++) {
        methods.put(classMethods[i].getName(), classMethods[i]);
      }
    }
    //  this.create(args, callbackContext);
    //  return true;
    if (methods.containsKey(action)) {
      if (self.mapCtrl.isDebug) {
        if (args != null && args.length() > 0) {
          Log.d(TAG, "(debug)action=" + action + " args[0]=" + args.getString(0));
        } else {
          Log.d(TAG, "(debug)action=" + action);
        }
      }
      Method method = methods.get(action);
      try {
        method.invoke(self, args, callbackContext);
        return true;
      } catch (Exception e) {
        e.printStackTrace();
        callbackContext.error(e.getMessage());
        return false;
      }
    } else {
      return false;
    }

  }

  protected void create(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    // dummy
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

  private void setValue(String methodName, Class<?> methodClass, String id, final Object value, final CallbackContext callbackContext) throws JSONException {
    final Object object = this.objects.get(id);
    try {
      final Method method = object.getClass().getDeclaredMethod(methodName, methodClass);
      cordova.getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          try {
            method.invoke(object, value);
            sendNoResult(callbackContext);
          } catch (Exception e) {
            e.printStackTrace();
            callbackContext.error(e.getMessage());
          }
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
    }
  }
  public void clear() {
    this.objects.clear();
  }

  protected void sendNoResult(CallbackContext callbackContext) {
    PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
    pluginResult.setKeepCallback(true);
    callbackContext.sendPluginResult(pluginResult);
  }


  protected void onOverlayEvent(String eventName, String overlayId, LatLng point) {
    webView.loadUrl("javascript:plugin.google.maps.Map." +
        "_onOverlayEvent(" +
        "'" + eventName + "','" + overlayId + "', " +
        "new window.plugin.google.maps.LatLng(" + point.latitude + "," + point.longitude + ")" +
        ")");
  }
}
