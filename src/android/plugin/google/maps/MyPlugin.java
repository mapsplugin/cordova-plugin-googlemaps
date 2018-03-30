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
import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyPlugin extends CordovaPlugin implements MyPluginInterface {
  public MyPlugin self = null;
  public final Map<String, Method> methods = new ConcurrentHashMap<String, Method>();
  protected static ExecutorService executorService = null;

  public CordovaGoogleMaps mapCtrl = null;
  public GoogleMap map = null;
  public PluginMap pluginMap = null;
  protected boolean isRemoved = false;
  protected static float density = Resources.getSystem().getDisplayMetrics().density;
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
    TAG = this.getServiceName();
    if (executorService == null) {
      cordova.getThreadPool().execute(new Runnable() {
        @Override
        public void run() {
          executorService = Executors.newCachedThreadPool();
        }
      });
    }
  }
  @Override
  public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext)  {
    self = this;
    executorService.submit(new Runnable() {
      @Override
      public void run() {

        if (isRemoved) {
          // Ignore every execute calls.
          return;
        }

        synchronized (methods) {
          if (methods.size() == 0) {
            TAG = MyPlugin.this.getServiceName();
            if (!TAG.contains("-")) {
              if (TAG.startsWith("map")) {
                mapCtrl.mPluginLayout.pluginOverlays.put(TAG, (PluginMap) MyPlugin.this);
              } else if (TAG.startsWith("streetview")) {
                mapCtrl.mPluginLayout.pluginOverlays.put(TAG, (PluginStreetViewPanorama) MyPlugin.this);
              }
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
      }
    });
    return true;

  }


  protected void create(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    // dummy
  }

  protected synchronized Circle getCircle(String id) {
    if (!pluginMap.objects.containsKey(id)) {
      //Log.e(TAG, "---> can not find the circle : " + id);
      return null;
    }
    return (Circle)pluginMap.objects.get(id);
  }
  protected synchronized GroundOverlay getGroundOverlay(String id) {
    if (!pluginMap.objects.containsKey(id)) {
      //Log.e(TAG, "---> can not find the ground overlay : " + id);
      return null;
    }
    return (GroundOverlay)pluginMap.objects.get(id);
  }
  protected synchronized Marker getMarker(String id) {
    if (!pluginMap.objects.containsKey(id)) {
      //Log.e(TAG, "---> can not find the maker : " + id);
      return null;
    }
    return (Marker)pluginMap.objects.get(id);
  }
  protected synchronized Polyline getPolyline(String id) {
    if (!pluginMap.objects.containsKey(id)) {
      //Log.e(TAG, "---> can not find the polyline : " + id);
      return null;
    }
    return (Polyline)pluginMap.objects.get(id);
  }
  protected synchronized Polygon getPolygon(String id) {
    if (!pluginMap.objects.containsKey(id)) {
      //Log.e(TAG, "---> can not find the polygon : " + id);
      return null;
    }
    return (Polygon)pluginMap.objects.get(id);
  }
  protected synchronized TileOverlay getTileOverlay(String id) {
    if (!pluginMap.objects.containsKey(id)) {
      //Log.e(TAG, "---> can not find the tileoverlay : " + id);
      return null;
    }
    return (TileOverlay)pluginMap.objects.get(id);
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
    if (!pluginMap.objects.containsKey(id)) {
      return;
    }
    final Object object = pluginMap.objects.get(id);
    try {
      final Method method = object.getClass().getDeclaredMethod(methodName, methodClass);
      cordova.getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          try {
            method.invoke(object, value);
            callbackContext.success();
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
    String[] keys = pluginMap.objects.keys.toArray(new String[pluginMap.objects.size()]);
    Object object;
    for (String key : keys) {
      object = pluginMap.objects.remove(key);
      object = null;
    }
    pluginMap.objects.clear();
  }


  protected void onOverlayEvent(String eventName, String overlayId, LatLng point) {
    webView.loadUrl("javascript:plugin.google.maps.Map." +
        "_onOverlayEvent(" +
        "'" + eventName + "','" + overlayId + "', " +
        "new window.plugin.google.maps.LatLng(" + point.latitude + "," + point.longitude + ")" +
        ")");
  }
}
