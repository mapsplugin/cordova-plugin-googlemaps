package plugin.google.maps;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class PluginMarkerCluster extends PluginMarker {

  private final static ConcurrentHashMap<String, STATUS> pluginMarkers = new ConcurrentHashMap<String, STATUS>();
  private final static ConcurrentHashMap<String, Integer> waitCntManager = new ConcurrentHashMap<String, Integer>();
  private final static ConcurrentHashMap<String, Boolean> debugFlags = new ConcurrentHashMap<String, Boolean>();
  private final static ArrayList<String> deleteMarkers = new ArrayList<String>();

  private final Object dummyObj = new Object();
  private boolean stopFlag = false;

  enum STATUS {
    WORKING,
    CREATED,
    DELETED
  }

  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
    if (!deleteThread.isAlive()) {
      deleteThread.start();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    stopFlag = true;
    this.clear();

  }

  @Override
  protected void clear() {

  }

  private Thread deleteThread = new Thread(new Runnable() {
    @Override
    public void run() {
      while(!stopFlag) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        if (deleteMarkers.size() == 0) {
          continue;
        }

        cordova.getActivity().runOnUiThread(removeOverlaysOnUiThread);
      }
    }
  });

  private Runnable removeOverlaysOnUiThread = new Runnable() {
    @Override
    public void run() {
      synchronized (deleteMarkers) {
        String markerId;
        Marker marker;
        STATUS status;
        String[] targetIDs = deleteMarkers.toArray(new String[deleteMarkers.size()]);

        for (int i = targetIDs.length - 1; i > -1; i--) {
          markerId = targetIDs[i];

          marker = self.getMarker(markerId);
          synchronized (pluginMarkers) {
            status =  pluginMarkers.get(markerId);
            //Log.d(TAG, "delete : " + markerId + " = " + status);
            if (!STATUS.WORKING.equals(status)) {
              synchronized (self.objects) {
                _removeMarker(marker);
                marker = null;
                objects.remove(markerId);
                objects.remove("marker_property_" + markerId);
              }
              pluginMarkers.remove(markerId);
              deleteMarkers.remove(i);
            } else {
              pluginMarkers.put(markerId, STATUS.DELETED);
            }
          }

        }
      }
    }
  };


  /**
   * Create a marker
   *
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  public void create(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    JSONObject params = args.getJSONObject(1);
    JSONArray positionList = params.getJSONArray("positionList");
    JSONArray geocellList = new JSONArray();
    JSONObject position;

    for (int i = 0; i < positionList.length(); i++) {
      position = positionList.getJSONObject(i);
      geocellList.put(getGeocell(position.getDouble("lat"), position.getDouble("lng"), 12));
    }

    String id = "markercluster_" + callbackContext.hashCode();
    debugFlags.put(id, params.getBoolean("debug"));

    final JSONObject result = new JSONObject();
    try {
      result.put("geocellList", geocellList);
      result.put("hashCode", callbackContext.hashCode());
      result.put("id", id);
    } catch (JSONException e) {
      e.printStackTrace();
    }

    callbackContext.success(result);
  }


  public void redrawClusters(final JSONArray args, final CallbackContext callbackContext) throws JSONException {


    final HashMap<String, String> updateClusterIDs = new HashMap<String, String>();
    final HashMap<String, Bundle> changeProperties = new HashMap<String, Bundle>();
    final String clusterId = args.getString(0);
    final boolean isDebug = debugFlags.get(clusterId);

    synchronized (dummyObj) {
      JSONObject params = args.getJSONObject(1);
      String clusterId_markerId, deleteMarkerId;
      String markerId;
      JSONArray deleteClusters = null;
      if (params.has("delete")) {
        deleteClusters = params.getJSONArray("delete");
      }
      JSONArray new_or_update = null;
      if (params.has("new_or_update")) {
        new_or_update = params.getJSONArray("new_or_update");
      }

      int deleteCnt = 0;
      int new_or_updateCnt = 0;
      if (deleteClusters != null) {
        deleteCnt = deleteClusters.length();
        for (int i = 0; i < deleteCnt; i++) {
          markerId = deleteClusters.getString(i);
          deleteMarkers.add(clusterId + "-" + markerId);
        }
      }
      if (new_or_update != null) {
        new_or_updateCnt = new_or_update.length();
      }

      //---------------------------
      // Determine new or update
      //---------------------------
      JSONObject clusterData, positionJSON;
      Bundle properites;
      for (int i = 0; i < new_or_updateCnt; i++) {
        clusterData = new_or_update.getJSONObject(i);
        positionJSON = clusterData.getJSONObject("position");
        markerId = clusterData.getString("id");
        clusterId_markerId = clusterId + "-" + markerId;

        self.objects.put("marker_property_" + clusterId_markerId, clusterData);

        if (self.objects.containsKey(clusterId_markerId) || pluginMarkers.containsKey(clusterId_markerId)) {
          updateClusterIDs.put(clusterId_markerId, clusterId_markerId);
        } else {
          pluginMarkers.put(clusterId_markerId, STATUS.WORKING);
          updateClusterIDs.put(clusterId_markerId, null);
        }

        properites = new Bundle();
        properites.putDouble("lat", positionJSON.getDouble("lat"));
        properites.putDouble("lng", positionJSON.getDouble("lng"));
        if (clusterData.has("title")) {
          properites.putString("title", clusterData.getString("title"));
        }
        properites.putString("id", clusterId_markerId);

        if (clusterData.has("icon")) {
          Object iconObj = clusterData.get("icon");

          if (iconObj instanceof String) {
            Bundle iconProperties = new Bundle();
            iconProperties.putString("url", (String) iconObj);
            properites.putBundle("icon", iconProperties);

          } else if (iconObj instanceof JSONObject) {
            JSONObject icon = clusterData.getJSONObject("icon");
            Bundle iconProperties = PluginUtil.Json2Bundle(icon);
            if (icon.has("label")) {
              JSONObject label = icon.getJSONObject("label");
              if (isDebug) {
                label.put("text", markerId);
              } else {
                label.put("text", clusterData.getInt("count") + "");
              }
              if (label.has("color")) {
                label.put("color", PluginUtil.parsePluginColor(label.getJSONArray("color")));
              }
              iconProperties.putBundle("label", PluginUtil.Json2Bundle(label));
            } else {
              Bundle label = new Bundle();
              if (isDebug) {
                label.putString("text", markerId);
              } else {
                label.putInt("fontSize", 20);
                label.putBoolean("bold", true);
                label.putString("text", clusterData.getInt("count") + "");
              }
              iconProperties.putBundle("label", label);
            }
            if (icon.has("anchor")) {
              double[] anchor = new double[2];
              if (icon.get("anchor") instanceof String) {
                JSONArray points = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                  points = new JSONArray(icon.getString("anchor"));
                  anchor[0] = points.getDouble(0);
                  anchor[1] = points.getDouble(1);
                }
              } else if (icon.get("anchor") instanceof JSONObject) {
                anchor[0] = icon.getJSONObject("anchor").getDouble("x");
                anchor[1] = icon.getJSONObject("anchor").getDouble("y");
              } else if (icon.get("anchor") instanceof JSONArray) {
                anchor[0] = icon.getJSONArray("anchor").getDouble(0);
                anchor[1] = icon.getJSONArray("anchor").getDouble(1);
              }
              iconProperties.putDoubleArray("anchor", anchor);
            }
            if (icon.has("infoWindowAnchor")) {
              double[] anchor = new double[2];
              if (icon.get("infoWindowAnchor") instanceof String) {
                JSONArray points = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                  points = new JSONArray(icon.getString("infoWindowAnchor"));
                  anchor[0] = points.getDouble(0);
                  anchor[1] = points.getDouble(1);
                }
              } else if (icon.get("infoWindowAnchor") instanceof JSONObject) {
                anchor[0] = icon.getJSONObject("infoWindowAnchor").getDouble("x");
                anchor[1] = icon.getJSONObject("infoWindowAnchor").getDouble("y");
              } else if (icon.get("infoWindowAnchor") instanceof JSONArray) {
                anchor[0] = icon.getJSONArray("infoWindowAnchor").getDouble(0);
                anchor[1] = icon.getJSONArray("infoWindowAnchor").getDouble(1);
              }
              iconProperties.putDoubleArray("infoWindowAnchor", anchor);
            }
            properites.putBundle("icon", iconProperties);
          }
        }

        changeProperties.put(clusterId_markerId, properites);
      }

      //Log.d(TAG, "---> deleteCnt : " + deleteCnt + ", newCnt : " + newCnt + ", updateCnt : " + updateCnt + ", reuseCnt : " + reuseCnt);
    }

    //---------------------------
    // mapping markers on the map
    //---------------------------
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Iterator<String> iterator;
        String markerId;
        Bundle markerProperties;
        Marker marker;
        Boolean isNew;

        //---------
        // reuse or update
        //---------
        waitCntManager.put(clusterId, updateClusterIDs.size());
        iterator = updateClusterIDs.keySet().iterator();
        while (iterator.hasNext()) {
          markerId = iterator.next();
          synchronized (pluginMarkers) {
            pluginMarkers.put(markerId, STATUS.WORKING);
          }
          isNew = !objects.containsKey(markerId);
          markerProperties = changeProperties.get(markerId);
          if (isNew) {
            marker = map.addMarker(new MarkerOptions()
                .position(new LatLng(markerProperties.getDouble("lat"), markerProperties.getDouble("lng")))
                .visible(false));
            if (markerProperties.containsKey("title")) {
              marker.setTitle(markerProperties.getString("title"));
            }
            if (markerProperties.containsKey("snippet")) {
              marker.setSnippet(markerProperties.getString("snippet"));
            }
            marker.setTag(markerProperties.getString("id"));
          } else {
            marker = getMarker(markerId);
          }
          synchronized (self.objects) {
            self.objects.put(markerId, marker);
          }

          if (!isNew) {
            synchronized (pluginMarkers) {
              pluginMarkers.put(markerId, STATUS.CREATED);
            }
            decreaseWaitCnt(clusterId);
            continue;
          }
          if (markerProperties.containsKey("icon")) {
            Bundle icon = markerProperties.getBundle("icon");
            //Log.d(TAG, "---> targetMarkerId = " + targetMarkerId + ", marker = " + marker);
            setIconToClusterMarker(markerId, marker, icon, new PluginAsyncInterface() {
              @Override
              public void onPostExecute(Object object) {
                decreaseWaitCnt(clusterId);
              }

              @Override
              public void onError(String errorMsg) {
                //--------------------------------------
                // Could not read icon for some reason
                //--------------------------------------
                Log.e(TAG, errorMsg);
                decreaseWaitCnt(clusterId);
              }
            });
          } else {
            //--------------------
            // No icon for marker
            //--------------------
            synchronized (pluginMarkers) {
              //Log.d(TAG, "create : " + markerId + " = CREATED");
              pluginMarkers.put(markerId, STATUS.CREATED);
            }
            decreaseWaitCnt(clusterId);
          }
        }
        updateClusterIDs.clear();


      }
    });
    synchronized (dummyObj) {
      try {
        dummyObj.wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    callbackContext.success();

  }

  private void decreaseWaitCnt(String clusterId) {

    //--------------------------------------
    // Icon is set to marker
    //--------------------------------------
    synchronized (waitCntManager) {
      int waitCnt = waitCntManager.get(clusterId);
      waitCnt = waitCnt - 1;
      if (waitCnt == 0) {
        synchronized (dummyObj) {
          dummyObj.notify();
        }

      }
      waitCntManager.put(clusterId, waitCnt);
    }
  }

  private void setIconToClusterMarker(final String markerId, final Marker marker, final Bundle iconProperty, final PluginAsyncInterface callback) {
    synchronized (pluginMarkers) {
      if (STATUS.DELETED.equals(pluginMarkers.get(markerId))) {
        synchronized (self.objects) {
          PluginMarkerCluster.this._removeMarker(marker);
          self.objects.remove(markerId);
          self.objects.remove("marker_property_" + markerId);
        }
        pluginMarkers.remove(markerId);
        callback.onError("marker has been removed");
        return;
      }
    }
    PluginMarkerCluster.super.setIcon_(marker, iconProperty, new PluginAsyncInterface() {
      @Override
      public void onPostExecute(Object object) {
        Marker marker = (Marker) object;
        synchronized (pluginMarkers) {
          if (STATUS.DELETED.equals(pluginMarkers.get(markerId))) {
            synchronized (self.objects) {
              PluginMarkerCluster.this._removeMarker(marker);
              self.objects.remove(markerId);
              self.objects.remove("marker_property_" + markerId);
            }
            pluginMarkers.remove(markerId);
            callback.onPostExecute(null);
            return;
          }
          marker.setVisible(true);

          Log.d(TAG, "create : " + markerId + " = CREATED");
          pluginMarkers.put(markerId, STATUS.CREATED);
          callback.onPostExecute(object);
        }
      }

      @Override
      public void onError(String errorMsg) {
        synchronized (objects) {
          if (marker != null && marker.getTag() != null) {
            PluginMarkerCluster.this._removeMarker(marker);
          }
          self.objects.remove(markerId);
          self.objects.remove("marker_property_" + markerId);
          pluginMarkers.remove(markerId);
          objects.remove(markerId);
        }
        callback.onPostExecute(errorMsg);
      }
    });
  }



  // The maximum *practical* geocell resolution.
  private static final int GEOCELL_GRID_SIZE = 4;
  public static final String GEOCELL_ALPHABET = "0123456789abcdef";

  private String getGeocell(double lat, double lng, int resolution) {
    StringBuilder cell = new StringBuilder();
    double north = 90.0;
    double south = -90.0;
    double east = 180.0;
    double west = -180.0;
    double subcell_lng_span, subcell_lat_span;
    byte x, y;
    while(cell.length() < resolution + 1) {
      subcell_lng_span = (east - west) / GEOCELL_GRID_SIZE;
      subcell_lat_span = (north - south) / GEOCELL_GRID_SIZE;

      x = (byte)Math.min(Math.floor(GEOCELL_GRID_SIZE * (lng - west) / (east - west)), GEOCELL_GRID_SIZE - 1);
      y = (byte)Math.min(Math.floor(GEOCELL_GRID_SIZE * (lat - south) / (north - south)), GEOCELL_GRID_SIZE - 1);
      cell.append(_subdiv_char(x, y));

      south += subcell_lat_span * y;
      north = south + subcell_lat_span;

      west += subcell_lng_span * x;
      east = west + subcell_lng_span;
    }
    return cell.toString();
  }
  private char _subdiv_char(int posX, int posY) {
    return GEOCELL_ALPHABET.charAt(
            (posY & 2) << 2 |
            (posX & 2) << 1 |
            (posY & 1) << 1 |
            (posX & 1) << 0);
  }

  private double[] _subdiv_xy(char cellChar) {
    int charI = GEOCELL_ALPHABET.indexOf(cellChar);
    return new double[]{
            (double)((charI & 4) >> 1 | (charI & 1) >> 0) + 0.0f,
            (double)((charI & 8) >> 2 | (charI & 2) >> 1) + 0.0f
    };
  }

  private LatLngBounds computeBox(String geocell) {

    //String tmp[] = geocell.split("-");
    //geocell = tmp[1];

    double subcell_lng_span, subcell_lat_span;
    double x, y;
    double xy[];
    BoundBox bbox = new BoundBox(90.0, 180.0, -90.0, -180.0);

    while (geocell.length() > 0) {
      subcell_lng_span = (double)(bbox.getEast() - bbox.getWest()) / (double)GEOCELL_GRID_SIZE;
      subcell_lat_span = (double)(bbox.getNorth() - bbox.getSouth()) / (double)GEOCELL_GRID_SIZE;

      xy = _subdiv_xy(geocell.charAt(0));
      x = xy[0];
      y = xy[1];

      bbox = new BoundBox(bbox.getSouth() + subcell_lat_span * (y + 1.0f),
              bbox.getWest() + subcell_lng_span * (double)(x + 1.0f),
              bbox.getSouth() + subcell_lat_span * y,
              bbox.getWest() + subcell_lng_span * x);

      geocell = geocell.substring(1);
    }
    LatLng sw = new LatLng(bbox.getSouth(), bbox.getWest());
    LatLng ne = new LatLng(bbox.getNorth(), bbox.getEast());
    return new LatLngBounds(sw, ne);
  }

  private class BoundBox {
    double north_, south_, east_, west_;

    BoundBox(double north, double east, double south, double west) {
      if (south > north) {
        south_ = north;
        north_ = south;
      } else {
        south_ = south;
        north_ = north;
      }
      west_ = west;
      east_ = east;
    }


    public double getNorth() {
      return north_;
    }

    public double getSouth() {
      return south_;
    }

    public double getWest() {
      return west_;
    }

    public double getEast() {
      return east_;
    }
  }

}
