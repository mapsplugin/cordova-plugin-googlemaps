package plugin.google.maps;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;

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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PluginMarkerCluster extends PluginMarker {

  private final static ConcurrentHashMap<String, STATUS> pluginMarkers = new ConcurrentHashMap<String, STATUS>();
  private final static ConcurrentHashMap<String, Integer> waitCntManager = new ConcurrentHashMap<String, Integer>();
  private final static ConcurrentHashMap<String, Boolean> debugFlags = new ConcurrentHashMap<String, Boolean>();
  private final static ArrayList<String> deleteMarkers = new ArrayList<String>();

  private final Object semaphore = new Object();
  private boolean stopFlag = false;
  private final Object deleteThreadLock = new Object();

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

  @Override
  protected void clear() {
    super.clear();
    synchronized (semaphore) {
      synchronized (pluginMarkers) {
        synchronized (deleteMarkers) {
          String clusterId_markerId;
          String[] keys = pluginMarkers.keySet().toArray(new String[pluginMarkers.size()]);
          for (int i = 0; i < keys.length; i++) {
            clusterId_markerId = keys[i];
            pluginMarkers.put(clusterId_markerId, STATUS.DELETED);
            deleteMarkers.add(clusterId_markerId);
          }
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
        String cacheKey;
        String[] targetIDs = deleteMarkers.toArray(new String[deleteMarkers.size()]);

        for (int i = targetIDs.length - 1; i > -1; i--) {
          markerId = targetIDs[i];

          marker = self.getMarker(markerId);
          synchronized (pluginMarkers) {
            status =  pluginMarkers.get(markerId);

            if (!STATUS.WORKING.equals(status)) {
              synchronized (pluginMap.objects) {
                _removeMarker(marker);
                marker = null;

                cacheKey = (String) pluginMap.objects.remove("marker_icon_" + markerId);
                if (cacheKey != null && iconCacheKeys.containsKey(cacheKey)) {
                  int count = iconCacheKeys.get(cacheKey);
                  if (count < 1) {
                    iconCacheKeys.remove(cacheKey);
                    AsyncLoadImage.removeBitmapFromMemCahce(cacheKey);
                  } else {
                    iconCacheKeys.put(cacheKey, count - 1);
                  }
                }


                pluginMap.objects.remove(markerId);
                pluginMap.objects.remove("marker_property_" + markerId);
                pluginMap.objects.remove("marker_imageSize_" + markerId);
              }
              pluginMarkers.remove(markerId);
              deleteMarkers.remove(i);
            } else {
              pluginMarkers.put(markerId, STATUS.DELETED);
            }
          }

        }
      }
      System.gc();
    }
  };


  public void remove(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String clusterId = args.getString(0);
    Log.d(TAG, "-->remove = " + clusterId);
    synchronized (debugFlags) {
      debugFlags.remove(clusterId);
      waitCntManager.remove(clusterId);
    }
    synchronized (pluginMarkers) {
      for(String key: pluginMarkers.keySet()) {
        if (key.startsWith(clusterId)) {
          Log.d(TAG, "-->delete = " + key);
          pluginMarkers.put(key, STATUS.CREATED);
          deleteMarkers.add(key);
        }
      }
    }

    /*
    String cacheKey;
    Set<String> keySet = pluginMap.objects.keySet();
    Marker marker;
    String[] objectIdArray = keySet.toArray(new String[keySet.size()]);
    for (String objectId : objectIdArray) {
      if (objectId.contains(clusterId)) {
        if (objectId.startsWith("marker_icon")) {
          cacheKey = (String) pluginMap.objects.remove(objectId);

          if (iconCacheKeys.containsKey(cacheKey)) {
            int count = iconCacheKeys.get(cacheKey);
            if (count < 1) {
              iconCacheKeys.remove(cacheKey);
              AsyncLoadImage.removeBitmapFromMemCahce(cacheKey);
            } else {
              iconCacheKeys.put(cacheKey, count - 1);
            }
          }
        } else if (objectId.startsWith("marker_property_") ||
            objectId.startsWith("marker_imageSize_")) {
          pluginMap.objects.remove(objectId);
        //} else if (objectId.startsWith("marker_")) {
        //  marker = (Marker) pluginMap.objects.remove(objectId);
        //  marker.remove();
        } else {
          pluginMap.objects.remove(objectId);
        }
      }
    }
    */
    synchronized (deleteThreadLock) {
      deleteThreadLock.notify();
    }

    callbackContext.success();
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


    final HashSet<String> updateClusterIDs = new HashSet<String>();
    final HashMap<String, Bundle> changeProperties = new HashMap<String, Bundle>();
    final String clusterId = args.getString(0);
    final boolean isDebug = debugFlags.get(clusterId);
    final JSONObject params = args.getJSONObject(1);

    String clusterId_markerId;
    JSONArray new_or_update = null;
    if (params.has("new_or_update")) {
      new_or_update = params.getJSONArray("new_or_update");
    }

    //---------------------------
    // Determine new or update
    //---------------------------
    int new_or_updateCnt = 0;
    if (new_or_update != null) {
      new_or_updateCnt = new_or_update.length();
    }
    JSONObject clusterData, positionJSON;
    Bundle properties;
    boolean isNew;
    for (int i = 0; i < new_or_updateCnt; i++) {
      clusterData = new_or_update.getJSONObject(i);
      positionJSON = clusterData.getJSONObject("position");
      clusterId_markerId = clusterData.getString("id");

      // Save the marker properties
      pluginMap.objects.put("marker_property_" + clusterId_markerId, clusterData);

      // Set the WORKING status flag
      synchronized (pluginMarkers) {
        pluginMarkers.put(clusterId_markerId, STATUS.WORKING);
      }
      updateClusterIDs.add(clusterId_markerId);

      // Prepare the marker properties for addMarker()
      properties = new Bundle();
      properties.putDouble("lat", positionJSON.getDouble("lat"));
      properties.putDouble("lng", positionJSON.getDouble("lng"));
      if (clusterData.has("title")) {
        properties.putString("title", clusterData.getString("title"));
      }
      properties.putString("id", clusterId_markerId);

      if (clusterData.has("icon")) {
        Object iconObj = clusterData.get("icon");

        if (iconObj instanceof String) {
          Bundle iconProperties = new Bundle();
          iconProperties.putString("url", (String) iconObj);
          properties.putBundle("icon", iconProperties);

        } else if (iconObj instanceof JSONObject) {
          JSONObject icon = clusterData.getJSONObject("icon");
          Bundle iconProperties = PluginUtil.Json2Bundle(icon);
          if (clusterData.has("isClusterIcon") && clusterData.getBoolean("isClusterIcon")) {
            if (icon.has("label")) {
              JSONObject label = icon.getJSONObject("label");
              if (isDebug) {
                label.put("text", clusterId_markerId.replace(clusterId, ""));
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
                label.putString("text", clusterId_markerId.replace(clusterId, ""));
              } else {
                label.putInt("fontSize", 15);
                label.putBoolean("bold", true);
                label.putString("text", clusterData.getInt("count") + "");
              }
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

    //Log.d(TAG, "---> deleteCnt : " + deleteCnt + ", newCnt : " + newCnt + ", updateCnt : " + updateCnt + ", reuseCnt : " + reuseCnt);

    if (updateClusterIDs.size() == 0) {
      deleteProcess(params);
      callbackContext.success();
      return;
    }

    //---------------------------
    // mapping markers on the map
    //---------------------------
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Iterator<String> iterator;
        String clusterId_markerId;
        Bundle markerProperties;
        Marker marker;
        Boolean isNew;

        //---------
        // new or update
        //---------
        int waitCnt = updateClusterIDs.size();
        int currentCnt = 0;
        waitCntManager.put(clusterId, updateClusterIDs.size());
        iterator = updateClusterIDs.iterator();
        while (iterator.hasNext()) {
          currentCnt++;
          clusterId_markerId = iterator.next();
          //Log.d(TAG, "---> progress = " + currentCnt + "/ " + waitCnt + ", " + clusterId_markerId);
          synchronized (pluginMarkers) {
            pluginMarkers.put(clusterId_markerId, STATUS.WORKING);
          }
          isNew = !pluginMap.objects.containsKey(clusterId_markerId);

          // Get the marker properties
          markerProperties = changeProperties.get(clusterId_markerId);
          if (clusterId_markerId.contains("-marker_")) {
            //-------------------
            // regular marker
            //-------------------
            if (isNew) {
              final String fMarkerId = clusterId_markerId;
              JSONObject properties = (JSONObject) pluginMap.objects.get("marker_property_" + clusterId_markerId);
              try {
                _create(clusterId_markerId, properties, new ICreateMarkerCallback() {
                  @Override
                  public void onSuccess(Marker marker) {

                    synchronized (pluginMarkers) {
                      if (pluginMarkers.get(fMarkerId) == STATUS.DELETED) {
                        _removeMarker(marker);
                        pluginMarkers.remove(fMarkerId);
                      } else {
                        pluginMarkers.put(fMarkerId, STATUS.CREATED);
                      }
                    }
                    decreaseWaitCnt(clusterId);
                  }

                  @Override
                  public void onError(String message) {
                    synchronized (pluginMarkers) {
                      pluginMarkers.put(fMarkerId, STATUS.DELETED);
                    }
                    Log.e(TAG, message);
                    decreaseWaitCnt(clusterId);
                    synchronized (deleteMarkers) {
                      deleteMarkers.add(fMarkerId);
                    }
                  }
                });
              } catch (JSONException e) {
                e.printStackTrace();
                decreaseWaitCnt(clusterId);
              }
            } else {
              marker = getMarker(clusterId_markerId);
              //----------------------------------------
              // Set the title and snippet properties
              //----------------------------------------
              if (markerProperties.containsKey("title")) {
                marker.setTitle(markerProperties.getString("title"));
              }
              if (markerProperties.containsKey("snippet")) {
                marker.setSnippet(markerProperties.getString("snippet"));
              }
              synchronized (pluginMarkers) {
                if (pluginMarkers.get(clusterId_markerId) == STATUS.DELETED) {
                  _removeMarker(marker);
                  pluginMarkers.remove(clusterId_markerId);
                } else {
                  pluginMarkers.put(clusterId_markerId, STATUS.CREATED);
                }
              }
              decreaseWaitCnt(clusterId);
            }
            continue;
          }
          //--------------------------
          // cluster icon
          //--------------------------
          if (isNew) {
            // If the requested id is new location, create a marker
            marker = map.addMarker(new MarkerOptions()
                .position(new LatLng(markerProperties.getDouble("lat"), markerProperties.getDouble("lng")))
                .visible(false));
            marker.setTag(markerProperties.getString("id"));

            // Store the marker instance with markerId
            synchronized (pluginMap.objects) {
              pluginMap.objects.put(clusterId_markerId, marker);
            }
          } else {
            marker = getMarker(clusterId_markerId);
          }
          //----------------------------------------
          // Set the title and snippet properties
          //----------------------------------------
          if (markerProperties.containsKey("title")) {
            marker.setTitle(markerProperties.getString("title"));
          }
          if (markerProperties.containsKey("snippet")) {
            marker.setSnippet(markerProperties.getString("snippet"));
          }
/*
          if (!isNew) {
            synchronized (pluginMarkers) {
              pluginMarkers.put(markerId, STATUS.CREATED);
            }
            decreaseWaitCnt(clusterId);
            continue;
          }
*/
          if (markerProperties.containsKey("icon")) {
            Bundle icon = markerProperties.getBundle("icon");
            final String fMarkerId = clusterId_markerId;
            //Log.d(TAG, "---> targetMarkerId = " + targetMarkerId + ", marker = " + marker);
            setIconToClusterMarker(clusterId_markerId, marker, icon, new PluginAsyncInterface() {
              @Override
              public void onPostExecute(Object object) {
                //--------------------------------------
                // Marker was updated
                //--------------------------------------
                decreaseWaitCnt(clusterId);
                synchronized (pluginMarkers) {
                  //Log.d(TAG, "create : " + fMarkerId + " = CREATED");
                  pluginMarkers.put(fMarkerId, STATUS.CREATED);
                }
              }

              @Override
              public void onError(String errorMsg) {
                //--------------------------------------
                // Could not read icon for some reason
                //--------------------------------------
                Log.e(TAG, errorMsg);
                decreaseWaitCnt(clusterId);
                synchronized (deleteMarkers) {
                  deleteMarkers.add(fMarkerId);
                }
                synchronized (pluginMarkers) {
                  pluginMarkers.put(fMarkerId, STATUS.DELETED);
                }
              }
            });
          } else {
            //--------------------
            // No icon for marker
            //--------------------
            synchronized (pluginMarkers) {
              //Log.d(TAG, "create : " + clusterId_markerId + " = CREATED");
              pluginMarkers.put(clusterId_markerId, STATUS.CREATED);
            }
            decreaseWaitCnt(clusterId);
          }
        }
        updateClusterIDs.clear();


      }
    });
    synchronized (semaphore) {
      try {
        semaphore.wait();
        deleteProcess(params);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    callbackContext.success();

  }
  private void deleteProcess(final JSONObject params) {
    if (!params.has("delete")) {
      return;
    }
    cordova.getThreadPool().submit(new Runnable() {
      @Override
      public void run() {

        try {
          JSONArray deleteClusters = params.getJSONArray("delete");
          if (deleteClusters != null) {
            //-------------------------------------
            // delete markers on the delete thread
            //-------------------------------------
            int deleteCnt = deleteClusters.length();
            String clusterId_markerId;
            for (int i = 0; i < deleteCnt; i++) {
              clusterId_markerId = deleteClusters.getString(i);
              deleteMarkers.add(clusterId_markerId);
            }

            synchronized (deleteThreadLock) {
              deleteThreadLock.notify();
            }

          }
        } catch (Exception e) {e.printStackTrace();}
      }
    });
  }

  private void decreaseWaitCnt(String clusterId) {

    //--------------------------------------
    // Icon is set to marker
    //--------------------------------------
    synchronized (waitCntManager) {
      int waitCnt = waitCntManager.get(clusterId);
      waitCnt = waitCnt - 1;
      //Log.d(TAG, "--->waitCnt = " + waitCnt);
      if (waitCnt == 0) {
        synchronized (semaphore) {
          semaphore.notify();
        }

      }
      waitCntManager.put(clusterId, waitCnt);
    }
  }

  private void setIconToClusterMarker(final String markerId, final Marker marker, final Bundle iconProperty, final PluginAsyncInterface callback) {
    synchronized (pluginMarkers) {
      if (STATUS.DELETED.equals(pluginMarkers.get(markerId))) {
        synchronized (pluginMap.objects) {
          PluginMarkerCluster.this._removeMarker(marker);
          pluginMap.objects.remove(markerId);
          pluginMap.objects.remove("marker_property_" + markerId);
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
            synchronized (pluginMap.objects) {
              PluginMarkerCluster.this._removeMarker(marker);
              pluginMap.objects.remove(markerId);
              pluginMap.objects.remove("marker_property_" + markerId);
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
        synchronized (pluginMap.objects) {
          if (marker != null && marker.getTag() != null) {
            PluginMarkerCluster.this._removeMarker(marker);
          }
          pluginMap.objects.remove(markerId);
          pluginMap.objects.remove("marker_property_" + markerId);
          pluginMarkers.remove(markerId);
          pluginMap.objects.remove(markerId);
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

}
