package plugin.google.maps;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.libraries.maps.model.LatLng;
import com.google.android.libraries.maps.model.LatLngBounds;
import com.google.android.libraries.maps.model.Marker;
import com.google.android.libraries.maps.model.MarkerOptions;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class PluginMarkerCluster extends PluginMarker {

  public final Map<String, STATUS> pluginMarkers = new ConcurrentHashMap<String, STATUS>();
  public final ArrayList<String> deleteMarkers = new ArrayList<String>();

  private boolean stopFlag = false;
  public final Object deleteThreadLock = new Object();

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
  }

  protected void clear() {
    synchronized (deleteMarkers) {
      String clusterId_markerId;
      if (pluginMarkers.size() > 0) {
        String[] keys = pluginMarkers.keySet().toArray(new String[pluginMarkers.size()]);
        for (int i = 0; i < keys.length; i++) {
          clusterId_markerId = keys[i];
          pluginMarkers.put(clusterId_markerId, STATUS.DELETED);
          deleteMarkers.add(clusterId_markerId);
        }
      }
    }
  }

  //---------------------
  // Delete thread
  //---------------------
  private Thread deleteThread = new Thread(new Runnable() {
    @Override
    public void run() {
      while(!stopFlag) {
        synchronized (deleteThreadLock) {
          try {
            deleteThreadLock.wait();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        if (deleteMarkers.size() == 0) {
          continue;
        }

        activity.runOnUiThread(removeOverlaysOnUiThread);
      }
    }
  });

  private Runnable removeOverlaysOnUiThread = new Runnable() {
    @Override
    public void run() {
      synchronized (deleteMarkers) {
        MetaMarker meta;
        STATUS status;

        if (deleteMarkers.size() == 0) {
          return;
        }

        String mapId = getServiceName().split("-")[0];
        String[] targetIDs = deleteMarkers.toArray(new String[deleteMarkers.size()]);
        Log.d(TAG, "--->mapId = " + mapId);
        PluginMap pluginMap = getMapInstance(mapId);
        String activeMarkerId = null;
        if (pluginMap.activeMarker != null) {
          activeMarkerId = (String) pluginMap.activeMarker.getTag();
          if (deleteMarkers.contains(activeMarkerId)) {
            pluginMap.activeMarker = null;
          }
        }

        for (int i = targetIDs.length - 1; i > -1; i--) {
          String markerId = targetIDs[i];

          if (!objects.containsKey(markerId)) {
            continue;
          }
          meta = objects.get(markerId);

          synchronized (pluginMarkers) {
            status =  pluginMarkers.get(markerId);

            if (!STATUS.WORKING.equals(status)) {
              synchronized (objects) {
                meta.marker.hideInfoWindow();

                _removeMarker(PluginMarkerCluster.this, meta);

                if (meta.iconCacheKey != null && iconCacheKeys.containsKey(meta.iconCacheKey)) {
                  int count = iconCacheKeys.get(meta.iconCacheKey);
                  if (count < 1) {
                    iconCacheKeys.remove(meta.iconCacheKey);
                    AsyncLoadImage.removeBitmapFromMemCahce(meta.iconCacheKey);
                  } else {
                    iconCacheKeys.put(meta.iconCacheKey, count - 1);
                  }
                }


                objects.remove(markerId);
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


  public void remove(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String clusterId = args.getString(1);
    PluginMarkerCluster instance = (PluginMarkerCluster) this.getInstance(mapId, clusterId);

    //Log.d(TAG, "-->remove = " + clusterId);
    synchronized (instance.pluginMarkers) {
      for(String key: instance.pluginMarkers.keySet()) {
        if (key.startsWith(clusterId)) {
          //Log.d(TAG, "-->delete = " + key);
          instance.pluginMarkers.put(key, STATUS.CREATED);
          instance.deleteMarkers.add(key);
        }
      }
    }

    synchronized (instance.deleteThreadLock) {
      instance.deleteThreadLock.notify();
    }

    callbackContext.success();
  }
  /**
   * Create a marker
   */
  @Override
  public void create(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    JSONObject params = args.getJSONObject(2);
    String hashCode = args.getString(3);
    JSONArray positionList = params.getJSONArray("positionList");
    JSONArray geocellList = new JSONArray();
    JSONObject position;

    for (int i = 0; i < positionList.length(); i++) {
      position = positionList.getJSONObject(i);
      geocellList.put(getGeocell(position.getDouble("lat"), position.getDouble("lng"), 12));
    }

    String id = String.format("markercluster_%s", hashCode);

    final JSONObject result = new JSONObject();
    try {
      result.put("geocellList", geocellList);
      result.put("hashCode", hashCode);
      result.put("__pgmId", id);
    } catch (JSONException e) {
      e.printStackTrace();
    }

    callbackContext.success(result);
  }

  @PgmPluginMethod
  public void redrawClusters(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    final String mapId = args.getString(0);
    final String clusterId = args.getString(1);
    final JSONObject params = args.getJSONObject(2);


    PluginMarkerCluster instance = (PluginMarkerCluster) this.getInstance(mapId, clusterId);
    HashSet<String> updateClusterIDs = new HashSet<String>();
    HashMap<String, Bundle> changeProperties = new HashMap<String, Bundle>();
    if (!params.has("new_or_update")) {
      instance._deleteProcess(clusterId, params);
      callbackContext.success();
      return;
    }
    //--------------------------
    // Determine new or update
    //--------------------------
    JSONArray new_or_update = params.getJSONArray("new_or_update");
    int cnt = new_or_update.length();
    for (int i = 0; i < cnt; i++) {
      JSONObject clusterData = new_or_update.getJSONObject(i);
      JSONObject positionJson = clusterData.getJSONObject("position");
      String markerId = clusterData.getString("__pgmId");
      String clusterId_markerId = String.format("%s-%s", clusterId, markerId);
      synchronized (instance.objects) {
        if (instance.objects.containsKey(clusterId_markerId)) {
          continue;
        }
      }

      // Save the marker properties
      MetaMarker meta = new MetaMarker(markerId);
      Log.d(TAG, String.format("-->put(%s) = %s", clusterId_markerId, meta));
      instance.objects.put(clusterId_markerId, meta);

      // Save the WORKING status flag
      synchronized (instance.pluginMarkers) {
        instance.pluginMarkers.put(clusterId_markerId, STATUS.WORKING);
      }
      updateClusterIDs.add(clusterId_markerId);

      // Prepare the mater properties for addMarker()
      meta.properties = clusterData;

      Bundle properties = new Bundle();
      properties.putDouble("lat", positionJson.getDouble("lat"));
      properties.putDouble("lng", positionJson.getDouble("lng"));

      if (clusterData.has("icon")) {
        Object iconObj = clusterData.get("icon");

        if (iconObj instanceof String) {
          Bundle iconProperties = new Bundle();
          iconProperties.putString("url", clusterData.getString("icon"));
          properties.putBundle("icon", iconProperties);
        } else if (iconObj instanceof JSONObject) {

          JSONObject icon = clusterData.getJSONObject("icon");

          Bundle iconProperties = PluginUtil.Json2Bundle(icon);
          if (clusterData.has("isClusterIcon") && clusterData.getBoolean("isClusterIcon")) {
            if (icon.has("label")) {
              JSONObject label = icon.getJSONObject("label");
              label.put("text", String.format("%s", clusterData.getInt("count")));
              if (label.has("color")) {
                label.put("color", PluginUtil.parsePluginColor(label.getJSONArray("color")));
              }
              iconProperties.putBundle("label", PluginUtil.Json2Bundle(label));
            } else {
              Bundle label = new Bundle();
              label.putInt("fontSize", 15);
              label.putBoolean("bold", true);
              label.putString("text", clusterData.getInt("count") + "");
              iconProperties.putBundle("label", label);
            }
          }
          if (icon.has("anchor")) {
            double[] anchor = new double[2];
            anchor[0] = icon.getJSONArray("anchor").getDouble(0);
            anchor[1] = icon.getJSONArray("anchor").getDouble(1);
            iconProperties.putDoubleArray("anchor", anchor);
          }
          if (icon.has("infoWindowAnchor")) {
            double[] anchor = new double[2];
            anchor[0] = icon.getJSONArray("infoWindowAnchor").getDouble(0);
            anchor[1] = icon.getJSONArray("infoWindowAnchor").getDouble(1);
            iconProperties.putDoubleArray("infoWindowAnchor", anchor);
          }
          properties.putBundle("icon", iconProperties);
        }
      }
      changeProperties.put(clusterId_markerId, properties);
    }

    //---------------------------
    // mapping markers on the map
    //---------------------------

    final JSONObject allResults = new JSONObject();
    final CountDownSemaphore localSemaphore = new CountDownSemaphore(updateClusterIDs.size());

    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {


        Iterator<String> iterator;

        //---------
        // new or update
        //---------
        iterator = updateClusterIDs.iterator();
        while (iterator.hasNext()) {

          final String clusterId_markerId = iterator.next();
          //Log.d(TAG, "---> progress = " + currentCnt + "/ " + waitCnt + ", " + clusterId_markerId);
          synchronized (instance.pluginMarkers) {
            instance.pluginMarkers.put(clusterId_markerId, STATUS.WORKING);
          }
          MetaMarker meta = instance.objects.get(clusterId_markerId);
          boolean isNew = meta.marker == null;

          Bundle markerProperties = changeProperties.get(clusterId_markerId);
          if (clusterId_markerId.contains("-marker_")) {
            //------------------
            // regular marker
            //------------------
            if (isNew) {
              try {
                instance._create(clusterId_markerId, meta.properties, new ICreateMarkerCallback() {
                  @Override
                  public void onSuccess(MetaMarker meta1) {
                    synchronized (instance.pluginMarkers) {
                      if (instance.pluginMarkers.get(clusterId_markerId) == STATUS.DELETED) {
                        _removeMarker(mapId, clusterId_markerId);
                        instance.pluginMarkers.remove(clusterId_markerId);
                      } else {
                        instance.pluginMarkers.put(clusterId_markerId, STATUS.CREATED);

                        meta.marker = meta1.marker;

                        JSONObject result = new JSONObject();
                        if (instance.icons.containsKey(clusterId_markerId)) {
                          Bitmap icon = instance.icons.get(clusterId_markerId);
                          try {
                            result.put("width", icon.getWidth() / density);
                            result.put("height", icon.getHeight() / density);
                          } catch (Exception e) {
                            e.printStackTrace();
                          }
                        } else {
                          try {
                            result.put("width", 24);
                            result.put("height", 42);
                          } catch (Exception e) {
                            e.printStackTrace();
                          }
                        }
                        try {
                          allResults.put(clusterId_markerId.split("-")[1], result);
                        } catch (JSONException e) {
                          e.printStackTrace();
                        }
                      }

                      localSemaphore.releaseOne();
                    }
                  }

                  @Override
                  public void onError(String message) {

                    synchronized (instance.pluginMarkers) {
                      instance.pluginMarkers.put(clusterId_markerId, STATUS.DELETED);
                    }
                    synchronized (instance.deleteMarkers) {
                      instance.deleteMarkers.add(clusterId_markerId);
                    }
                    localSemaphore.releaseOne();
                  }
                });
              } catch (JSONException e) {
                e.printStackTrace();
                localSemaphore.releaseOne();
              }
            } else {
              if (markerProperties.containsKey("title")) {
                meta.marker.setTitle(markerProperties.getString("title"));
              }
              if (markerProperties.containsKey("snippet")) {
                meta.marker.setSnippet(markerProperties.getString("snippet"));
              }
              synchronized (instance.pluginMarkers) {
                if (instance.pluginMarkers.get(clusterId_markerId) == STATUS.DELETED) {
                  _removeMarker(mapId, clusterId_markerId);
                  instance.pluginMarkers.remove(clusterId_markerId);
                } else {
                  instance.pluginMarkers.put(clusterId_markerId, STATUS.CREATED);
                }
              }
              localSemaphore.releaseOne();
            }
            continue;

          }

          //--------------------------
          // cluster icon
          //--------------------------
          if (isNew) {
            // If the requested id is new location, create a marker
            meta.marker = instance.getMapInstance(mapId).getGoogleMap().addMarker(new MarkerOptions()
                    .position(new LatLng(markerProperties.getDouble("lat"), markerProperties.getDouble("lng")))
                    .visible(false));
            meta.marker.setTag(clusterId_markerId);
          }
          //----------------------------------------
          // Set the title and snippet properties
          //----------------------------------------
          if (markerProperties.containsKey("title")) {
            meta.marker.setTitle(markerProperties.getString("title"));
          }
          if (markerProperties.containsKey("snippet")) {
            meta.marker.setSnippet(markerProperties.getString("snippet"));
          }

          //----------------------------------------
          // Set icon
          //----------------------------------------
          if (markerProperties.containsKey("icon")) {
            Bundle icon = markerProperties.getBundle("icon");

            //Log.d(TAG, "---> targetMarkerId = " + targetMarkerId + ", marker = " + marker);
            _setIconToClusterMarker(mapId, clusterId_markerId, meta, icon, new PluginAsyncInterface() {
              @Override
              public void onPostExecute(Object object) {
                //--------------------------------------
                // Marker was updated
                //--------------------------------------
                synchronized (instance.pluginMarkers) {
                  //Log.d(TAG, "create : " + fMarkerId + " = CREATED");
                  instance.pluginMarkers.put(clusterId_markerId, STATUS.CREATED);
                }
                localSemaphore.releaseOne();
              }

              @Override
              public void onError(String errorMsg) {
                //--------------------------------------
                // Could not read icon for some reason
                //--------------------------------------
                Log.e(TAG, errorMsg);
                synchronized (deleteMarkers) {
                  deleteMarkers.add(clusterId_markerId);
                }
                synchronized (pluginMarkers) {
                  pluginMarkers.put(clusterId_markerId, STATUS.DELETED);
                }
                localSemaphore.releaseOne();
              }
            });
          } else {
            //--------------------
            // No icon for marker
            //--------------------
            synchronized (instance.pluginMarkers) {
              //Log.d(TAG, "create : " + clusterId_markerId + " = CREATED");
              instance.pluginMarkers.put(clusterId_markerId, STATUS.CREATED);
            }
            localSemaphore.releaseOne();
          }
        }

      }
    });


    instance._deleteProcess(clusterId, params);
    localSemaphore.waitLock();

    callbackContext.success(allResults);
  }
  private void _deleteProcess(final String clusterId, final JSONObject params) {
    if (!params.has("delete")) {
      return;
    }



    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {

        try {
          JSONArray deleteClusters = params.getJSONArray("delete");

          if (deleteClusters == null) {
            return;
          }
          //-------------------------------------
          // delete markers on the delete thread
          //-------------------------------------
          int deleteCnt = deleteClusters.length();
          String clusterId_markerId;
          for (int i = 0; i < deleteCnt; i++) {
            clusterId_markerId = String.format("%s-%s", clusterId, deleteClusters.getString(i));
            if (!PluginMarkerCluster.this.objects.containsKey(clusterId_markerId)) {
              continue;
            }
            MetaMarker meta = PluginMarkerCluster.this.objects.get(clusterId_markerId);
            Log.d(TAG, String.format("--> delete %s / %s", clusterId_markerId, meta));
            _removeMarker(PluginMarkerCluster.this, meta);
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });
  }

  private void _setIconToClusterMarker(final String mapId, final String markerId, final MetaMarker meta, final Bundle iconProperty, final PluginAsyncInterface callback) {
    synchronized (pluginMarkers) {
      if (STATUS.DELETED.equals(pluginMarkers.get(markerId))) {
        synchronized (objects) {
          PluginMarkerCluster.this._removeMarker(mapId, markerId);
          objects.remove(markerId);
        }
        pluginMarkers.remove(markerId);
        callback.onError("marker has been removed");
        return;
      }
    }
    _setIcon(this, meta, iconProperty, new PluginAsyncInterface() {
      @Override
      public void onPostExecute(Object object) {
        Marker marker = (Marker) object;
        synchronized (pluginMarkers) {
          if (STATUS.DELETED.equals(pluginMarkers.get(markerId))) {
            synchronized (objects) {
              PluginMarkerCluster.this._removeMarker(mapId, markerId);
              objects.remove(markerId);
            }
            pluginMarkers.remove(markerId);
            callback.onPostExecute(null);
            return;
          }
          marker.setVisible(true);

          //Log.d(TAG, "create : " + markerId + " = CREATED");
          pluginMarkers.put(markerId, STATUS.CREATED);
          callback.onPostExecute(object);
        }
      }

      @Override
      public void onError(String errorMsg) {
        synchronized (objects) {
          if (meta.marker != null && meta.marker.getTag() != null) {
            PluginMarkerCluster.this._removeMarker(mapId, markerId);
          }
          objects.remove(markerId);
          pluginMarkers.remove(markerId);
          pluginMarkers.put(markerId, STATUS.DELETED);
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


  class CountDownSemaphore {
    int count;
    final Object semaphore = new Object();
    public CountDownSemaphore(int countDownInit) {
      this.count = countDownInit;
    }

    public void releaseOne() {
      this.count--;
      if (this.count <= 0) {
        synchronized (this.semaphore) {
          this.semaphore.notify();
        }
      }
    }
    public void waitLock()  {
      if (this.count > 0) {
        synchronized (this.semaphore) {
          try {
            this.semaphore.wait();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }
  }
}
