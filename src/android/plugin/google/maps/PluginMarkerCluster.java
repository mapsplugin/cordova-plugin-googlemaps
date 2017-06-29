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

public class PluginMarkerCluster extends MyPlugin implements MyPluginInterface  {

  private final static ConcurrentHashMap<String, String> pluginMarkers = new ConcurrentHashMap<String, String>();
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

    String id = "markercluster_" + callbackContext.hashCode();
    final JSONObject result = new JSONObject();
    try {
      result.put("hashCode", callbackContext.hashCode());
      result.put("id", id);
    } catch (JSONException e) {
      e.printStackTrace();
    }

    callbackContext.success(result);
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
        synchronized (pluginMarkers) {
          if (!pluginMarkers.containsKey(clusterId_geocell)) {
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
          //Log.e(TAG, "--> markerId == null : " + clusterId_geocell);
          return;
        }

        PluginEntry pluginEntry = pluginMap.plugins.get(pluginMap.mapId + "-marker");
        PluginMarker pluginMarker = (PluginMarker)pluginEntry.plugin;

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
              //Log.d(TAG, "---> removed: " + clusterId_geocell);

            }
          };
          pluginMarker.remove(args, dummyCallback);

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
        //Log.d(TAG, "---> (contained) " + clusterId_geocell + " : " + pluginMarkers.get(clusterId_geocell));
        return null;
      }
      pluginMarkers.put(clusterId_geocell, "(null)");
    }

    JSONObject markerOpts = new JSONObject();
    markerOpts.put("position", clusterData.getJSONObject("position"));
    markerOpts.put("title", clusterId_geocell);
    if (clusterData.has("icon")) {
      JSONObject icon = clusterData.getJSONObject("icon");
      if (icon.has("label")) {
        JSONObject label = icon.getJSONObject("label");
        label.put("text", clusterData.getInt("count") + "");
        icon.put("label", label);
      }
      markerOpts.put("icon", icon);
    }

    final JSONArray args = new JSONArray();
    args.put("Marker");
    args.put(markerOpts);

    final String callbackId = "add-cluster-" + clusterId_geocell;
    final CallbackContext callback = new CallbackContext(callbackId, webView) {
      @Override
      public void sendPluginResult(PluginResult pluginResult) {
        try {

          JSONObject result = new JSONObject(pluginResult.getMessage());
          final String markerId = result.getString("id");

          synchronized (pluginMarkers) {
            String storedId = pluginMarkers.get(clusterId_geocell);
            if (!"(null)".equals(storedId) ||
              reqResolution != resolutions.get(clusterId)) {
              // The resolution has been changed during creating a cluster marker
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
                        //Log.d(TAG, "---> skip: " + clusterId_geocell);
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

            pluginMarkers.put(clusterId_geocell, markerId);
            //Log.d(TAG, "---> created: " + clusterId_geocell);
          }

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
      }
    };
  }

  /*
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
*/
}
