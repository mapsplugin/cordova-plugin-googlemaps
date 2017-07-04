package plugin.google.maps;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginEntry;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PluginMarkerCluster extends PluginMarker {

  private final static ConcurrentHashMap<String, STATUS> pluginMarkers = new ConcurrentHashMap<String, STATUS>();
  private final static ConcurrentHashMap<String, Integer> resolutions = new ConcurrentHashMap<String, Integer>();
  private final static ConcurrentHashMap<String, Integer> waitCntManager = new ConcurrentHashMap<String, Integer>();

  final Object dummyObj = new Object();

  enum STATUS {
    WORKING,
    CREATED,
    DELETED
  }

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
   *
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  public void create(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    JSONObject markerClusterOptions = args.getJSONObject(1);
    JSONArray positionList = markerClusterOptions.getJSONArray("positionList");
    JSONArray geocellList = new JSONArray();
    JSONObject position;

    for (int i = 0; i < positionList.length(); i++) {
      position = positionList.getJSONObject(i);
      geocellList.put(getGeocell(position.getDouble("lat"), position.getDouble("lng"), 9));
    }

    String id = "markercluster_" + callbackContext.hashCode();
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
    final ArrayList<String> deleteClusterIDs = new ArrayList<String>();
    final HashMap<String, Bundle> changeProperties = new HashMap<String, Bundle>();
    final String clusterId = args.getString(0);

    synchronized (dummyObj) {
      JSONObject params = args.getJSONObject(1);
      String geocell, clusterId_geocell, deleteGeocell;

      int resolution = params.getInt("resolution");
      resolutions.put(clusterId, resolution);


      JSONArray deleteClusters = params.getJSONArray("delete");
      JSONArray new_or_update = params.getJSONArray("new_or_update");

      int deleteCnt = 0;
      int new_or_updateCnt = 0;
      int reuseCnt = 0;
      if (deleteClusters != null) {
        deleteCnt = deleteClusters.length();
      }
      if (new_or_update != null) {
        new_or_updateCnt = new_or_update.length();
      }
      for (int i = 0; i < deleteCnt; i++) {
        geocell = deleteClusters.getString(i);
        deleteClusterIDs.add(clusterId + "-" + geocell);
      }

      //---------------------------
      // Determine new or update
      //---------------------------
      JSONObject clusterData, positionJSON;
      Bundle properites;
      for (int i = 0; i < new_or_updateCnt; i++) {
        clusterData = new_or_update.getJSONObject(i);
        positionJSON = clusterData.getJSONObject("position");
        geocell = clusterData.getString("geocell");
        clusterId_geocell = clusterId + "-" + geocell;
        if (self.objects.containsKey(clusterId_geocell) || pluginMarkers.containsKey(clusterId_geocell)) {
          updateClusterIDs.put(clusterId_geocell, clusterId_geocell);
        } else {
          if (reuseCnt < deleteCnt) {
            //---------------
            // Reuse a marker
            //---------------
            deleteGeocell = deleteClusterIDs.remove(0);
            deleteCnt--;
            updateClusterIDs.put(deleteGeocell, clusterId_geocell);
            reuseCnt++;
          } else {
            pluginMarkers.put(clusterId_geocell, STATUS.WORKING);
            updateClusterIDs.put(clusterId_geocell, null);
          }
        }

        properites = new Bundle();
        properites.putDouble("lat", positionJSON.getDouble("lat"));
        properites.putDouble("lng", positionJSON.getDouble("lng"));
        properites.putString("title", clusterId_geocell);

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
              label.put("text", clusterData.getInt("count") + "");
              if (label.has("color")) {
                label.put("color", PluginUtil.parsePluginColor(label.getJSONArray("color")));
              }
              iconProperties.putBundle("label", PluginUtil.Json2Bundle(label));
            }
            properites.putBundle("icon", iconProperties);
          }
        }

        changeProperties.put(clusterId_geocell, properites);
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
      String oldGeocell, newGeocell, markerGeocell;
      Bundle markerProperties;
      Marker marker;
      Polygon polygon;
      LatLngBounds bounds;

      //---------
      // reuse or update
      //---------
      waitCntManager.put(clusterId, updateClusterIDs.size());
      iterator = updateClusterIDs.keySet().iterator();
      while (iterator.hasNext()) {
        oldGeocell = iterator.next();
        newGeocell = updateClusterIDs.get(oldGeocell);
        if (newGeocell == null) {
          markerGeocell = oldGeocell;
          markerProperties = changeProperties.get(oldGeocell);
          marker = map.addMarker(new MarkerOptions()
              .position(new LatLng(markerProperties.getDouble("lat"), markerProperties.getDouble("lng")))
              .title(oldGeocell));
          marker.setTag("markercluster");

          bounds = computeBox(oldGeocell);
          polygon = map.addPolygon(new PolygonOptions()
                  .add(bounds.northeast)
                  .add(new LatLng(bounds.northeast.latitude, bounds.southwest.longitude))
                  .add(bounds.southwest)
                  .add(new LatLng(bounds.southwest.latitude, bounds.northeast.longitude))
                  .visible(true)
                  .strokeColor(Color.BLUE)
                  .strokeWidth(2 * density));
          polygon.setTag("polygon");

          synchronized (self.objects) {
            self.objects.put(oldGeocell, marker);
            self.objects.put("polygon" + oldGeocell, polygon);
            pluginMarkers.put(oldGeocell, STATUS.WORKING);
          }
        } else {
          //synchronized (pluginMarkers) {
            if (STATUS.DELETED.equals(pluginMarkers.get(oldGeocell))) {
              continue;
            }
          //}
          marker = null;
          polygon = null;
          while (marker == null) {
            //synchronized (self.objects) {
              marker = self.getMarker(oldGeocell);
              polygon = self.getPolygon("polygon" + oldGeocell);
            //}
            if (marker == null || marker.getTag() == null) {
              if (deleteClusterIDs.size() > 0) {
                oldGeocell = deleteClusterIDs.remove(0);
              } else {
                marker = map.addMarker(new MarkerOptions()
                    .position(new LatLng(0, 0)));
                marker.setTag("markercluster");
              }
            }
          }
          synchronized (marker) {
            markerProperties = changeProperties.get(newGeocell);
            //synchronized (pluginMarkers) {
              if (STATUS.DELETED.equals(pluginMarkers.get(newGeocell))) {
                continue;
              }
            //}
            marker.setPosition(new LatLng(markerProperties.getDouble("lat"), markerProperties.getDouble("lng")));
            marker.setTitle(newGeocell);
            markerGeocell = newGeocell;
            bounds = computeBox(newGeocell);
            ArrayList<LatLng> points = new ArrayList<LatLng>();
            if (polygon == null) {

              polygon = map.addPolygon(new PolygonOptions()
                      .add(bounds.northeast)
                      .add(new LatLng(bounds.northeast.latitude, bounds.southwest.longitude))
                      .add(bounds.southwest)
                      .add(new LatLng(bounds.southwest.latitude, bounds.northeast.longitude))
                      .visible(true)
                      .strokeColor(Color.BLUE)
                      .strokeWidth(2 * density));
              polygon.setTag("polygon");
            } else {
              points.add(bounds.northeast);
              points.add(new LatLng(bounds.northeast.latitude, bounds.southwest.longitude));
              points.add(bounds.southwest);
              points.add(new LatLng(bounds.southwest.latitude, bounds.northeast.longitude));
              polygon.setPoints(points);
            }

            if (oldGeocell.hashCode() != newGeocell.hashCode()) {
              //Log.d(TAG, "reuse : " + oldGeocell + " -> " + newGeocell);
              synchronized (self.objects) {
                self.objects.put(newGeocell, marker);
                self.objects.put("polygon" + newGeocell, polygon);
                pluginMarkers.put(newGeocell, STATUS.CREATED);
                self.objects.remove(oldGeocell);
                self.objects.remove("polygon" + oldGeocell);
                pluginMarkers.remove(oldGeocell);
              }
            } else {
              //Log.d(TAG, "update : " + newGeocell);
              synchronized (self.objects) {
                self.objects.put(newGeocell, marker);
                self.objects.put("polygon" + newGeocell, polygon);
              }
            }
          }
        }

        Bundle icon = markerProperties.getBundle("icon");
        if (icon != null) {
          setIconToClusterMarker(markerGeocell, marker, icon, new PluginAsyncInterface() {
            @Override
            public void onPostExecute(Object object) {

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

            @Override
            public void onError(String errorMsg) {
              Log.e(TAG, errorMsg);
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
          });
        } else {
          marker.setIcon(null);
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
      }
      updateClusterIDs.clear();


      //---------
      // delete
      //---------
      iterator = deleteClusterIDs.iterator();
      while (iterator.hasNext()) {
        oldGeocell = iterator.next();
        //Log.d(TAG, "delete : " + oldGeocell);
        marker = self.getMarker(oldGeocell);
        polygon = self.getPolygon("polygon" + oldGeocell);
        synchronized (self.objects) {
          objects.remove(oldGeocell);
          objects.remove("polygon" + oldGeocell);
          pluginMarkers.remove(oldGeocell);
        }
        synchronized (pluginMarkers) {
          if (!STATUS.WORKING.equals(pluginMarkers.get(oldGeocell))) {
            _removeMarker(marker);
            if (polygon != null) {
              polygon.setTag(null);
              polygon.remove();
            }
            pluginMarkers.remove(oldGeocell);
          } else {
            pluginMarkers.put(oldGeocell, STATUS.DELETED);
          }
        }
      }
      deleteClusterIDs.clear();

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

  private void setIconToClusterMarker(final String geocell, final Marker marker, final Bundle iconProperty, final PluginAsyncInterface callback) {
    //synchronized (pluginMarkers) {
      if (STATUS.DELETED.equals(pluginMarkers.get(geocell))) {
        PluginMarkerCluster.this._removeMarker(marker);
        pluginMarkers.remove(geocell);
        callback.onError("marker has been removed");
        return;
      }
      pluginMarkers.put(geocell, STATUS.WORKING);
    //}
    PluginMarkerCluster.super.setIcon_(marker, iconProperty, new PluginAsyncInterface() {
      @Override
      public void onPostExecute(Object object) {
        Marker marker = (Marker) object;
        //synchronized (pluginMarkers) {
          if (STATUS.DELETED.equals(pluginMarkers.get(geocell))) {
            PluginMarkerCluster.this._removeMarker(marker);
            pluginMarkers.remove(geocell);
            callback.onPostExecute(null);
            return;
          }
        //}

        pluginMarkers.put(geocell, STATUS.CREATED);
        callback.onPostExecute(object);
      }

      @Override
      public void onError(String errorMsg) {
        try {
          synchronized (marker) {
            if (marker.getTag() == null) {
              PluginMarkerCluster.this._removeMarker(marker);
              marker.setTag(null);
              marker.remove();
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
        synchronized (objects) {
          pluginMarkers.remove(geocell);
          objects.remove(geocell);
          objects.remove("polygon" + geocell);
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

    String tmp[] = geocell.split("-");
    geocell = tmp[1];

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
