package plugin.google.maps;

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

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginEntry;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyPlugin extends CordovaPlugin implements MyPluginInterface {
  protected HashMap<String, Object> objects;
  public MyPlugin self = null;
  public final HashMap<String, Method> methods = new HashMap<String, Method>();
  private static ExecutorService executorService = null;

  public CordovaGoogleMaps mapCtrl = null;
  public GoogleMap map = null;
  public PluginMap pluginMap = null;
  protected boolean isRemoved = false;
  public float density = Resources.getSystem().getDisplayMetrics().density;
  public String CURRENT_PAGE_URL;

  public void setPluginMap(PluginMap pluginMap) {
    this.pluginMap = pluginMap;
    this.mapCtrl = pluginMap.mapCtrl;
    this.map = pluginMap.map;
  }
  protected String TAG = "";

  @SuppressLint("UseSparseArrays")
  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
    this.objects = new HashMap<String, Object>();
    TAG = this.getServiceName();
    if (executorService == null) {
      executorService = Executors.newFixedThreadPool(5);
    }
  }
  @Override
  public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext)  {
    self = this;
    executorService.execute(new Runnable() {
      @Override
      public void run() {

        if (isRemoved) {
          // Ignore every execute calls.
          return;
        }

        if (methods.size() == 0) {
          TAG = MyPlugin.this.getServiceName();
          //Log.d("MyPlugin", "TAG = " + TAG);
          if (!TAG.contains("-")) {
            mapCtrl.mPluginLayout.pluginMaps.put(TAG, (PluginMap) MyPlugin.this);
          } else {
            PluginEntry pluginEntry = new PluginEntry(TAG, MyPlugin.this);
            pluginMap.plugins.put(TAG, pluginEntry);
          }


          //CordovaPlugin plugin = mapCtrl.webView.getPluginManager().getPlugin(this.getServiceName());
          //    Log.d("MyPlugin", "---> this = " + this);
          //    Log.d("MyPlugin", "---> plugin = " + plugin);

          Method[] classMethods = self.getClass().getMethods();
          for (Method classMethod : classMethods) {
            methods.put(classMethod.getName(), classMethod);
          }
        }
        //  this.create(args, callbackContext);
        //  return true;
        if (methods.containsKey(action)) {
          if (self.mapCtrl.mPluginLayout.isDebug) {
            try {
              if (args != null && args.length() > 0) {
                Log.d(TAG, "(debug)action=" + action + " args[0]=" + args.getString(0));
              } else {
                Log.d(TAG, "(debug)action=" + action);
              }
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
          Method method = methods.get(action);
          try {
            if (isRemoved) {
              // Ignore every execute calls.
              return;
            }
            method.invoke(self, args, callbackContext);
          } catch (IllegalAccessException e) {
            e.printStackTrace();
            callbackContext.error("Cannot access to the '" + action + "' method.");
          } catch (InvocationTargetException e) {
            e.printStackTrace();
            callbackContext.error("Cannot access to the '" + action + "' method.");
          }
        }
      }
    });
    return true;

  }

  protected void create(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    // dummy
  }

  protected Circle getCircle(String id) {
    return (Circle)this.objects.get(id);
  }


  protected GroundOverlay getGroundOverlay(String id) { return (GroundOverlay)this.objects.get(id);}
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
  protected void clear() {
    String[] keys = objects.keySet().toArray(new String[objects.size()]);
    Object object;
    for (String key : keys) {
      object = objects.remove(key);
      object = null;
    }
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
