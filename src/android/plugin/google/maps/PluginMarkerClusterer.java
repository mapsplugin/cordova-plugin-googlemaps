package plugin.google.maps;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginEntry;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PluginMarkerClusterer extends MyPlugin implements MyPluginInterface  {

  private final static HashMap<String, String> pluginMarkers = new HashMap<String, String>();
  private final static ConcurrentHashMap<String, String> pluginPolylines = new ConcurrentHashMap<String, String>();
  private final static ConcurrentHashMap<String, Integer> resolutions = new ConcurrentHashMap<String, Integer>();
  private final static ExecutorService executorService = Executors.newCachedThreadPool();

  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    this.clear();

  }

  @Override
  protected void clear() {

  }

  /**
   * Create a marker
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  public void create(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    executorService.submit(new Runnable() {
      @Override
      public void run() {

        String id = "markerclusterer_" + callbackContext.hashCode();
        final JSONObject result = new JSONObject();
        try {
          result.put("hashCode", callbackContext.hashCode());
          result.put("id", id);
        } catch (JSONException e) {
          e.printStackTrace();
        }

        callbackContext.success(result);
      }
    });

  }


  public synchronized void redrawClusters(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    String clusterId = args.getString(0);
    JSONObject params = args.getJSONObject(1);

    int resolution = params.getInt("resolution");
    resolutions.put(clusterId, resolution);

    JSONArray deleteClusters = params.getJSONArray("delete");
    if (deleteClusters != null && deleteClusters.length() > 0) {
      String geocell;

      for (int i = 0; i < deleteClusters.length(); i++) {
        geocell = deleteClusters.getString(i);
        executorService.submit(deleteOldCluster(clusterId + "-" + geocell));
      }

    }

    JSONArray changeClusters = params.getJSONArray("new_or_update");
    if (changeClusters != null && changeClusters.length() > 0) {

      Runnable task;
      for (int i = 0; i < changeClusters.length(); i++) {
        task = createClusterTask(clusterId, changeClusters.getJSONObject(i), resolution);
        if (task != null) {
          executorService.submit(task);
        }
      }


    }

    callbackContext.success();


  }

  private Runnable deleteOldCluster(final String clusterId_geocell) {

    return new Runnable() {
      @Override
      public void run() {
        String markerId;
        String tmp[] = clusterId_geocell.split("-");
        synchronized (pluginMarkers) {
          if (!pluginMarkers.containsKey(clusterId_geocell)) {
            //Log.e(TAG, "--> not contained : " + clusterId_geocell);
            // The plugin is still loading the icon from the internet.
            pluginMarkers.put(clusterId_geocell, "(deleted)");
            return;
          }
          markerId = pluginMarkers.get(clusterId_geocell);
        }

        if ("(deleted)".equals(markerId) ||
            "(null)".equals(markerId)) {
          return;
        }
        if (markerId == null) {
          Log.e(TAG, "--> markerId == null : " + clusterId_geocell);
          return;
        }

        PluginEntry pluginEntry = pluginMap.plugins.get(pluginMap.mapId + "-marker");
        PluginMarker pluginMarker = (PluginMarker)pluginEntry.plugin;

        //final Object dummyObject = new Object();

        try {
          JSONArray args = new JSONArray();
          args.put(0, markerId);
          CallbackContext dummyCallback = new CallbackContext("remove-marker-" + clusterId_geocell, webView) {
            @Override
            public void sendPluginResult(PluginResult pluginResult) {
              //ignore
              synchronized (pluginMarkers) {
                pluginMarkers.remove(clusterId_geocell);
              }
              Log.d(TAG, "---> removed: " + clusterId_geocell);

            }
          };
          pluginMarker.remove(args, dummyCallback);


          /*
          if (mapCtrl.mPluginLayout.isDebug && pluginPolylines.containsKey(clusterId_geocell)) {
            //---------
            // debug
            //---------
            pluginEntry = pluginMap.plugins.get(pluginMap.mapId + "-polyline");
            PluginPolyline pluginPolyline = (PluginPolyline) pluginEntry.plugin;
            args = new JSONArray();
            args.put(0, pluginPolylines.remove(clusterId_geocell));
            pluginPolyline.remove(args, new CallbackContext("remove-polyline-" + clusterId_geocell, webView) {
              @Override
              public void sendPluginResult(PluginResult pluginResult) {  }
            });
          }
          */
          /*
          synchronized (dummyObject) {
            try {
              dummyObject.wait();
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
          */

        } catch (JSONException e) {
          e.printStackTrace();
        }


      }
    };
  }

  private Runnable createClusterTask(final String clusterId, JSONObject clusterData, final int reqResolution) throws JSONException {
    final String geocell = clusterData.getString("geocell");
    final String clusterId_geocell = clusterId + "-" + geocell;
    synchronized (pluginMarkers) {
      if (pluginMarkers.containsKey(clusterId_geocell)) {
      /*
      String markerId = pluginMarkers.get(clusterId_geocell);
      if ("73".equals(geocell)) {
        Log.d(TAG, "---> 73 = (contained) " + markerId);
      }
      */
        Log.d(TAG, "---> (contained) " + clusterId_geocell + " : " + pluginMarkers.get(clusterId_geocell));
        return null;
      }
      pluginMarkers.put(clusterId_geocell, "(null)");
    }

    /*
    if ("73".equals(geocell)) {
      Log.d(TAG, "---> 73 = (null)");
    }
    */


    //final Object dummyObject = new Object();
    int itemCnt = clusterData.getInt("count");
    final LatLngBounds bounds = computeBox(geocell);

    JSONObject position = new JSONObject();
    JSONObject markerOpts = new JSONObject();
    final JSONArray args = new JSONArray();
    position.put("lat", bounds.getCenter().latitude);
    position.put("lng", bounds.getCenter().longitude);
    markerOpts.put("position", position);
    markerOpts.put("title", clusterId_geocell);
    markerOpts.put("icon", "https://mt.google.com/vt/icon/text=" + geocell.length() + "&psize=16&font=fonts/arialuni_t.ttf&color=ff330000&name=icons/spotlight/spotlight-waypoint-b.png&ax=44&ay=48&scale=1");
    args.put("Marker");
    args.put(markerOpts);

    final String callbackId = "add-cluster-" + clusterId_geocell;
    final CallbackContext callback = new CallbackContext(callbackId, webView) {
      @Override
      public void sendPluginResult(PluginResult pluginResult) {
        try {

          JSONObject result = new JSONObject(pluginResult.getMessage());
          final String markerId = result.getString("id");
          String storedId = pluginMarkers.get(clusterId_geocell);

          synchronized (pluginMarkers) {
          if (!"(null)".equals(storedId) ||
              reqResolution != resolutions.get(clusterId)) {
            // The resolution has been changed during creating a cluster marker
            //synchronized (pluginMarkers) {
            //  pluginMarkers.put(clusterId_geocell, markerId);
            //}
            executorService.submit(new Runnable() {
              @Override
              public void run() {
                try {
                  PluginEntry pluginEntry = pluginMap.plugins.get(pluginMap.mapId + "-marker");
                  PluginMarker pluginMarker = (PluginMarker)pluginEntry.plugin;
                  JSONArray args = new JSONArray();
                  args.put(0, markerId);
                  CallbackContext dummyCallback = new CallbackContext("remove-after-created-" + clusterId_geocell, webView) {
                    @Override
                    public void sendPluginResult(PluginResult pluginResult) { // done, but nothing to do
                      Log.d(TAG, "---> skip: " + clusterId_geocell);
                      synchronized (pluginMarkers) {
                        if (pluginMarkers.containsKey(clusterId_geocell)) {
                          pluginMarkers.remove(clusterId_geocell);
                        }
                      }
                    }
                  };
                  pluginMarker.remove(args, dummyCallback);
                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }
            });
            return;
          }

            //Log.d(TAG, "---> add: " + clusterId + "-" + geocell);
            /*
            if ("73".equals(geocell)) {
              Log.d(TAG, "---> 73 = (add)" + markerId);
            }
            */
            pluginMarkers.put(clusterId_geocell, markerId);
            Log.d(TAG, "---> created: " + clusterId_geocell);
          }
          /*
          synchronized (dummyObject) {
            dummyObject.notify();
          }
          */



/*
          if (mapCtrl.mPluginLayout.isDebug) {
            //---------
            // debug
            //---------
            executorService.submit(new Runnable() {
              @Override
              public void run() {
                try {
                  JSONObject polylineOpts = new JSONObject();
                  JSONArray positions = new JSONArray();
                  positions.put(new JSONObject(String.format(Locale.US, "{\"lat\": %f, \"lng\": %f}", bounds.northeast.latitude, bounds.northeast.longitude)));
                  positions.put(new JSONObject(String.format(Locale.US, "{\"lat\": %f, \"lng\": %f}", bounds.northeast.latitude, bounds.southwest.longitude)));
                  positions.put(new JSONObject(String.format(Locale.US, "{\"lat\": %f, \"lng\": %f}", bounds.southwest.latitude, bounds.southwest.longitude)));
                  positions.put(new JSONObject(String.format(Locale.US, "{\"lat\": %f, \"lng\": %f}", bounds.southwest.latitude, bounds.northeast.longitude)));
                  positions.put(new JSONObject(String.format(Locale.US, "{\"lat\": %f, \"lng\": %f}", bounds.northeast.latitude, bounds.northeast.longitude)));
                  polylineOpts.put("points", positions);
                  polylineOpts.put("visible", "true");
                  polylineOpts.put("color", new JSONArray("[255, 0, 0, 127]"));

                  JSONArray args = new JSONArray();
                  args.put(0, "Polyline");
                  args.put(1, polylineOpts);

                  pluginMap.loadPlugin(args, new CallbackContext("debug-" + markerId, webView) {
                    @Override
                    public void sendPluginResult(PluginResult pluginResult) {
                      try {
                        JSONObject result = new JSONObject(pluginResult.getMessage());
                        final String polylineId = result.getString("id");

                        if (!pluginMarkers.containsKey(clusterId_geocell)) {

                          PluginEntry pluginEntry = pluginMap.plugins.get(pluginMap.mapId + "-polyline");
                          PluginPolyline pluginPolyline = (PluginPolyline) pluginEntry.plugin;
                          JSONArray args = new JSONArray();
                          args.put(0, polylineId);
                          CallbackContext dummyCallback = new CallbackContext("remove-polyline-" + clusterId_geocell, webView) {
                            @Override
                            public void sendPluginResult(PluginResult pluginResult) {  }
                          };
                          pluginPolyline.remove(args, dummyCallback);
                          return;
                        }

                        pluginPolylines.put(clusterId_geocell, polylineId);
                      } catch (JSONException e) {
                        e.printStackTrace();
                      }


                    }
                  });

                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }
            });
          }*/

        } catch (JSONException e) {
          e.printStackTrace();
        }

      }
    };

    return new Runnable() {
      @Override
      public void run() {

        try {
          pluginMap.loadPlugin(args, callback);

        } catch (JSONException e) {
          e.printStackTrace();
        }
        /*
        synchronized (dummyObject) {
          try {
            dummyObject.wait();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        */

      }
    };
  }

  // The maximum *practical* geocell resolution.
  private static final int GEOCELL_GRID_SIZE = 4;
  public static final String GEOCELL_ALPHABET = "0123456789abcdef";

  public static LatLngBounds computeBox(String geocell) {
    String geoChar;
    double north = 90.0;
    double south = -90.0;
    double east = 180.0;
    double west = -180.0;
    int pos;

    double subcell_lng_span, subcell_lat_span;
    int x, y;
    for (int i = 0; i < geocell.length(); i++) {
      geoChar = geocell.substring(i, i + 1);
      pos = GEOCELL_ALPHABET.indexOf(geoChar);

      subcell_lng_span = (east - west) / GEOCELL_GRID_SIZE;
      subcell_lat_span = (north - south) / GEOCELL_GRID_SIZE;

      x = (int) (Math.floor(pos / 4) % 2 * 2 + pos % 2);
      y = (int) (pos - Math.floor(pos / 4) * 4);
      y = y >> 1;
      y += Math.floor(pos / 4) > 1 ? 2 : 0;

      south += subcell_lat_span * y;
      north = south + subcell_lat_span;

      west += subcell_lng_span * x;
      east = west + subcell_lng_span;
    }
    LatLng sw = new LatLng(south, west);
    LatLng ne = new LatLng(north, east);
    return new LatLngBounds(sw, ne);
  }
}
