package plugin.google.maps;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;


public class MyPlugin extends CordovaPlugin {
  public final Map<String, Method> methods = new ConcurrentHashMap<String, Method>();

  protected boolean isRemoved = false;
  public static final float density = Resources.getSystem().getDisplayMetrics().density;
  public static final ExecutorService executorService = Executors.newCachedThreadPool();
  protected Handler mainHandler = new Handler(Looper.getMainLooper());
  protected Activity activity;
  protected String TAG;
  private Semaphore semaphore = new Semaphore(10);


  MyPlugin() {
    super();
    TAG = this.getClass().getSimpleName();

    Method[] classMethods = this.getClass().getMethods();
    for (Method classMethod : classMethods) {
      Annotation annotation = classMethod.getAnnotation(PgmPluginMethod.class);
      if (annotation != null) {
//        Log.d(TAG, String.format("-->method = %s", classMethod.getName()));
        methods.put(classMethod.getName(), classMethod);
      }
    }

  }

  @SuppressLint("UseSparseArrays")
  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    activity = cordova.getActivity();
  }

  public String getCurrentUrl() {
    return webView.getUrl();
  }


  @Override
  public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext)  {

    if (!methods.containsKey(action)) {
      return false;
    }

    executorService.submit(new Runnable() {
      @Override
      public void run() {

        if (isRemoved) {
          // Ignore every execute calls.
          if (callbackContext != null) {
            callbackContext.success();
          }
          return;
        }

        try {
          semaphore.acquire();
        } catch (InterruptedException e) {
          callbackContext.error(e.getMessage());
          return;
        }

        synchronized (methods) {
          final Method method = methods.get(action);

          PgmPluginMethod annotation = method.getAnnotation(PgmPluginMethod.class);
          if (annotation.runOnUiThread()) {
            //---------------------------------------------
            // Execute the method in background thread
            //---------------------------------------------
            mainHandler.post(new Runnable() {
              @Override
              public void run() {
                try {
                  method.invoke(MyPlugin.this, args, new CallbackContext(callbackContext.getCallbackId() + "-dummy", webView) {
                    @Override
                    public void sendPluginResult(PluginResult pluginResult) {
                      super.sendPluginResult(pluginResult);

                      callbackContext.sendPluginResult(pluginResult);

                      semaphore.release();
                    }
                  });
                } catch (IllegalAccessException e) {
                  e.printStackTrace();
                  callbackContext.error("Cannot access to the '" + action + "' method.");
                } catch (InvocationTargetException e) {
                  e.printStackTrace();
                  callbackContext.error("Cannot access to the '" + action + "' method.");
                }
              }
            });
          } else {
            //---------------------------------------------
            // Execute the method in background thread
            //---------------------------------------------
            try {
              method.invoke(MyPlugin.this, args, new CallbackContext(callbackContext.getCallbackId() + "-dummy", webView) {
                @Override
                public void sendPluginResult(PluginResult pluginResult) {
                  super.sendPluginResult(pluginResult);

                  callbackContext.sendPluginResult(pluginResult);

                  semaphore.release();
                }
              });
            } catch (IllegalAccessException e) {
              e.printStackTrace();
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

  public void setRemoved(boolean isRemoved) {
    this.isRemoved = isRemoved;
  }

  /*
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
  */
}
