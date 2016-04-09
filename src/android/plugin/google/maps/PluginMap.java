package plugin.google.maps;

import java.io.ByteArrayOutputStream;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CameraPosition.Builder;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.VisibleRegion;

public class PluginMap extends MyPlugin {
  private final String ANIMATE_CAMERA_DONE = "animate_camera_done";
  private final String ANIMATE_CAMERA_CANCELED = "animate_camera_canceled";

  private class AsyncUpdateCameraPositionResult {
    CameraUpdate cameraUpdate;
    int durationMS;
    LatLngBounds cameraBounds;
  }

  private class AsyncSetOptionsResult {
    UiSettings uiSettings;
    boolean myLocationButtonEnabled;
    int MAP_TYPE_ID;
  }

  /**
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void setOptions(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final AsyncTask<UiSettings, Void, AsyncSetOptionsResult> task = new AsyncTask<UiSettings, Void, AsyncSetOptionsResult>() {
      private Exception mException = null;
      @Override
      protected AsyncSetOptionsResult doInBackground(UiSettings... settingses) {
        UiSettings settings = settingses[0];
        AsyncSetOptionsResult results = new AsyncSetOptionsResult();

        try {
          JSONObject params = args.getJSONObject(0);

          //controls
          if (params.has("controls")) {
            JSONObject controls = params.getJSONObject("controls");

            if (controls.has("compass")) {
              results.uiSettings.setCompassEnabled(controls.getBoolean("compass"));
            }
            if (controls.has("zoom")) {
              results.uiSettings.setZoomControlsEnabled(controls.getBoolean("zoom"));
            }
            if (controls.has("indoorPicker")) {
              results.uiSettings.setIndoorLevelPickerEnabled(controls.getBoolean("indoorPicker"));
            }
            if (controls.has("myLocationButton")) {
              results.uiSettings.setMyLocationButtonEnabled(controls.getBoolean("myLocationButton"));
            }
          }

          //gestures
          if (params.has("gestures")) {
            JSONObject gestures = params.getJSONObject("gestures");

            if (gestures.has("tilt")) {
              results.uiSettings.setTiltGesturesEnabled(gestures.getBoolean("tilt"));
            }
            if (gestures.has("scroll")) {
              results.uiSettings.setScrollGesturesEnabled(gestures.getBoolean("scroll"));
            }
            if (gestures.has("rotate")) {
              results.uiSettings.setRotateGesturesEnabled(gestures.getBoolean("rotate"));
            }
            if (gestures.has("zoom")) {
              results.uiSettings.setZoomGesturesEnabled(gestures.getBoolean("zoom"));
            }
          }

          // map type
          if (params.has("mapType")) {
            String typeStr = params.getString("mapType");
            results.MAP_TYPE_ID = -1;
            results.MAP_TYPE_ID = typeStr.equals("MAP_TYPE_NORMAL") ? GoogleMap.MAP_TYPE_NORMAL : results.MAP_TYPE_ID;
            results.MAP_TYPE_ID = typeStr.equals("MAP_TYPE_HYBRID") ? GoogleMap.MAP_TYPE_HYBRID : results.MAP_TYPE_ID;
            results.MAP_TYPE_ID = typeStr.equals("MAP_TYPE_SATELLITE") ? GoogleMap.MAP_TYPE_SATELLITE : results.MAP_TYPE_ID;
            results.MAP_TYPE_ID = typeStr.equals("MAP_TYPE_TERRAIN") ? GoogleMap.MAP_TYPE_TERRAIN : results.MAP_TYPE_ID;
            results.MAP_TYPE_ID = typeStr.equals("MAP_TYPE_NONE") ? GoogleMap.MAP_TYPE_NONE : results.MAP_TYPE_ID;
          }

          //controls
          Boolean isEnabled = true;

          // move the camera position
          if (params.has("camera")) {
            LatLngBounds cameraBounds = null;
            JSONObject camera = params.getJSONObject("camera");
            Builder builder = CameraPosition.builder();
            if (camera.has("bearing")) {
              builder.bearing((float) camera.getDouble("bearing"));
            }
            if (camera.has("latLng")) {
              JSONObject latLng = camera.getJSONObject("latLng");
              builder.target(new LatLng(latLng.getDouble("lat"), latLng.getDouble("lng")));
            }

            if (camera.has("target")) {
              CameraPosition newPosition;
              Object target = camera.get("target");
              @SuppressWarnings("rawtypes")
              Class targetClass = target.getClass();
              if ("org.json.JSONArray".equals(targetClass.getName())) {
                JSONArray points = camera.getJSONArray("target");
                cameraBounds = PluginUtil.JSONArray2LatLngBounds(points);
                builder.target(cameraBounds.getCenter());

              } else {
                JSONObject latLng = camera.getJSONObject("target");
                builder.target(new LatLng(latLng.getDouble("lat"), latLng.getDouble("lng")));
              }
            }
            if (camera.has("tilt")) {
              builder.tilt((float) camera.getDouble("tilt"));
            }
            if (camera.has("zoom")) {
              builder.zoom((float) camera.getDouble("zoom"));
            }
            map.moveCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
            if (cameraBounds != null) {
              mapCtrl.fitBounds(cameraBounds);
            }
          }
          
        } catch (Exception e) {
          mException = e;
          this.cancel(true);
          return null;
        }

        return results;
      }

      @Override
      public void onCancelled() {
        if (mException != null) {
          mException.printStackTrace();
          callbackContext.error("" + mException.getMessage());
        } else {
          callbackContext.error("");
        }

      }
      
      @Override
      public void onPostExecute(AsyncSetOptionsResult results) {
        if (results.myLocationButtonEnabled) {
          map.setMyLocationEnabled(true);
        }
        if (results.MAP_TYPE_ID != -1) {
          map.setMapType(results.MAP_TYPE_ID);
        }
        sendNoResult(callbackContext);
      }
    };

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        UiSettings settings = map.getUiSettings();
        task.execute(settings);
      }
    });


    
    
  }
  
  /**
   * Set center location of the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void setCenter(JSONArray args, CallbackContext callbackContext) throws JSONException {
    double lat, lng;

    lat = args.getDouble(1);
    lng = args.getDouble(2);

    LatLng latLng = new LatLng(lat, lng);
    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(latLng);
    this.myMoveCamera(cameraUpdate, callbackContext);
  }

  /**
   * Set angle of the map view
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void setTilt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float tilt = -1;
    tilt = (float) args.getDouble(1);

    if (tilt > 0 && tilt <= 90) {
      CameraPosition currentPos = map.getCameraPosition();
      CameraPosition newPosition = new CameraPosition.Builder()
          .target(currentPos.target).bearing(currentPos.bearing)
          .zoom(currentPos.zoom).tilt(tilt).build();
      myMoveCamera(newPosition, callbackContext);
    } else {
      callbackContext.error("Invalid tilt angle(" + tilt + ")");
    }
  }

  /**
   * Move the camera with animation
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void animateCamera(JSONArray args, CallbackContext callbackContext) throws JSONException {
    this.updateCameraPosition("animateCamera", args, callbackContext);
  }
  
  /**
   * Move the camera without animation
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void moveCamera(JSONArray args, CallbackContext callbackContext) throws JSONException {
    this.updateCameraPosition("moveCamera", args, callbackContext);
  }

  
  /**
   * move the camera
   * @param action
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void updateCameraPosition(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    final CameraPosition.Builder builder = CameraPosition.builder();
    final JSONObject cameraPos = args.getJSONObject(0);

    AsyncTask<Void, Void, AsyncUpdateCameraPositionResult> createCameraUpdate = new AsyncTask<Void, Void, AsyncUpdateCameraPositionResult>() {
      private Exception mException = null;

      @Override
      protected AsyncUpdateCameraPositionResult doInBackground(Void... voids) {
        AsyncUpdateCameraPositionResult result = new AsyncUpdateCameraPositionResult();
        try {
          result.durationMS = 4000;
          if (cameraPos.has("tilt")) {
            builder.tilt((float) cameraPos.getDouble("tilt"));
          }
          if (cameraPos.has("bearing")) {
            builder.bearing((float) cameraPos.getDouble("bearing"));
          }
          if (cameraPos.has("zoom")) {
            builder.zoom((float) cameraPos.getDouble("zoom"));
          }
          if (cameraPos.has("duration")) {
            result.durationMS = cameraPos.getInt("duration");
          }

          if (!cameraPos.has("target")) {
            return result;
          }

          //------------------------
          // Create a cameraUpdate
          //------------------------
          result.cameraUpdate = null;
          result.cameraBounds = null;
          CameraPosition newPosition;
          Object target = cameraPos.get("target");
          @SuppressWarnings("rawtypes")
          Class targetClass = target.getClass();
          JSONObject latLng;
          if ("org.json.JSONArray".equals(targetClass.getName())) {
            JSONArray points = cameraPos.getJSONArray("target");
            result.cameraBounds = PluginUtil.JSONArray2LatLngBounds(points);
            result.cameraUpdate = CameraUpdateFactory.newLatLngBounds(result.cameraBounds, (int)(20 * density));
          } else {
            latLng = cameraPos.getJSONObject("target");
            builder.target(new LatLng(latLng.getDouble("lat"), latLng.getDouble("lng")));
            newPosition = builder.build();
            result.cameraUpdate = CameraUpdateFactory.newCameraPosition(newPosition);
          }
        } catch (Exception e) {
          mException = e;
          e.printStackTrace();
          this.cancel(true);
          return null;
        }

        return result;
      }

      @Override
      public void onCancelled() {
        if (mException != null) {
          mException.printStackTrace();
        }
        callbackContext.error(mException != null ? mException.getMessage() + "" : "");
      }
      @Override
      public void onCancelled(AsyncUpdateCameraPositionResult AsyncUpdateCameraPositionResult) {
        if (mException != null) {
          mException.printStackTrace();
        }
        callbackContext.error(mException != null ? mException.getMessage() + "" : "");
      }

      @Override
      public void onPostExecute(AsyncUpdateCameraPositionResult AsyncUpdateCameraPositionResult) {
        if (AsyncUpdateCameraPositionResult.cameraUpdate == null) {
          builder.target(map.getCameraPosition().target);
          AsyncUpdateCameraPositionResult.cameraUpdate = CameraUpdateFactory.newCameraPosition(builder.build());
        }

        final LatLngBounds finalCameraBounds = AsyncUpdateCameraPositionResult.cameraBounds;
        PluginUtil.MyCallbackContext myCallback = new PluginUtil.MyCallbackContext("moveCamera", webView) {
          @Override
          public void onResult(final PluginResult pluginResult) {
            if (finalCameraBounds != null && ANIMATE_CAMERA_DONE.equals(pluginResult.getStrMessage())) {
              CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(finalCameraBounds, (int)density);
              map.moveCamera(cameraUpdate);


              Builder builder = CameraPosition.builder();
              if (cameraPos.has("tilt")) {
                try {
                  builder.tilt((float) cameraPos.getDouble("tilt"));
                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }
              if (cameraPos.has("bearing")) {
                try {
                  builder.bearing((float) cameraPos.getDouble("bearing"));
                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }
              builder.zoom(map.getCameraPosition().zoom);
              builder.target(map.getCameraPosition().target);
              map.moveCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
            }
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
          }
        };
        if (action.equals("moveCamera")) {
          myMoveCamera(AsyncUpdateCameraPositionResult.cameraUpdate, myCallback);
        } else {
          myAnimateCamera(AsyncUpdateCameraPositionResult.cameraUpdate, AsyncUpdateCameraPositionResult.durationMS, myCallback);
        }

      }
    };
    createCameraUpdate.execute();




  }

  /**
   * Set zoom of the map
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  public void setZoom(JSONArray args, CallbackContext callbackContext) throws JSONException {
    Long zoom;
    zoom = args.getLong(1);

    this.myMoveCamera(CameraUpdateFactory.zoomTo(zoom), callbackContext);
  }

  /**
   * Pan by the specified pixel
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  public void panBy(JSONArray args, CallbackContext callbackContext) throws JSONException {
    int x = args.getInt(1);
    int y = args.getInt(2);
    float xPixel = -x * this.density;
    float yPixel = -y * this.density;
    
    CameraUpdate cameraUpdate = CameraUpdateFactory.scrollBy(xPixel, yPixel);
    map.animateCamera(cameraUpdate);
  }
  
  /**
   * Move the camera of the map
   * @param cameraPosition
   * @param callbackContext
   */
  public void myMoveCamera(CameraPosition cameraPosition, final CallbackContext callbackContext) {
    CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
    this.myMoveCamera(cameraUpdate, callbackContext);
  }

  /**
   * Move the camera of the map
   * @param cameraUpdate
   * @param callbackContext
   */
  public void myMoveCamera(final CameraUpdate cameraUpdate, final CallbackContext callbackContext) {
    map.moveCamera(cameraUpdate);
    callbackContext.success();
  }

  /**
   * Enable MyLocation feature if set true
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void setMyLocationEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Boolean isEnabled = false;
    isEnabled = args.getBoolean(1);
    map.setMyLocationEnabled(isEnabled);
    map.getUiSettings().setMyLocationButtonEnabled(isEnabled);
    this.sendNoResult(callbackContext);
  }

  /**
   * Enable Indoor map feature if set true
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void setIndoorEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Boolean isEnabled = args.getBoolean(1);
    map.setIndoorEnabled(isEnabled);
    this.sendNoResult(callbackContext);
  }

  /**
   * Enable the traffic layer if set true
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void setTrafficEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Boolean isEnabled = args.getBoolean(1);
    map.setTrafficEnabled(isEnabled);
    this.sendNoResult(callbackContext);
  }

  /**
   * Enable the compass if set true
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void setCompassEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Boolean isEnabled = args.getBoolean(1);
    UiSettings uiSettings = map.getUiSettings();
    uiSettings.setCompassEnabled(isEnabled);
    this.sendNoResult(callbackContext);
  }

  /**
   * Change the map type id of the map
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void setMapTypeId(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    int mapTypeId = -1;
    String typeStr = args.getString(1);
    mapTypeId = typeStr.equals("MAP_TYPE_NORMAL") ? GoogleMap.MAP_TYPE_NORMAL
        : mapTypeId;
    mapTypeId = typeStr.equals("MAP_TYPE_HYBRID") ? GoogleMap.MAP_TYPE_HYBRID
        : mapTypeId;
    mapTypeId = typeStr.equals("MAP_TYPE_SATELLITE") ? GoogleMap.MAP_TYPE_SATELLITE
        : mapTypeId;
    mapTypeId = typeStr.equals("MAP_TYPE_TERRAIN") ? GoogleMap.MAP_TYPE_TERRAIN
        : mapTypeId;
    mapTypeId = typeStr.equals("MAP_TYPE_NONE") ? GoogleMap.MAP_TYPE_NONE
        : mapTypeId;

    if (mapTypeId == -1) {
      callbackContext.error("Unknown MapTypeID is specified:" + typeStr);
      return;
    }
    
    final int myMapTypeId = mapTypeId;
    map.setMapType(myMapTypeId);
    this.sendNoResult(callbackContext);
  }


  /**
   * Move the camera of the map
   * @param cameraUpdate
   * @param durationMS
   * @param callbackContext
   */
  public void myAnimateCamera(final CameraUpdate cameraUpdate, final int durationMS, final CallbackContext callbackContext) {
    final GoogleMap.CancelableCallback callback = new GoogleMap.CancelableCallback() {
      @Override
      public void onFinish() {
        callbackContext.success(ANIMATE_CAMERA_DONE);
      }

      @Override
      public void onCancel() {
        callbackContext.success(ANIMATE_CAMERA_CANCELED);
      }
    };

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (durationMS > 0) {
          map.animateCamera(cameraUpdate, durationMS, callback);
        } else {
          map.animateCamera(cameraUpdate, callback);
        }
      }
    });
  }
  

  /**
   * Return the current position of the camera
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void getCameraPosition(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        CameraPosition camera = map.getCameraPosition();
        JSONObject json = new JSONObject();
        JSONObject latlng = new JSONObject();
        try {
          latlng.put("lat", camera.target.latitude);
          latlng.put("lng", camera.target.longitude);
          json.put("target", latlng);
          json.put("zoom", camera.zoom);
          json.put("tilt", camera.tilt);
          json.put("bearing", camera.bearing);
          json.put("hashCode", camera.hashCode());

          callbackContext.success(json);
        } catch (JSONException e) {
          e.printStackTrace();
          callbackContext.error(e.getMessage() + "");
        }
      }
    });
  }
  
  /**
   * Return the image data encoded with base64
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void toDataURL(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    JSONObject params = args.getJSONObject(1);
    boolean uncompress = false;
    if (params.has("uncompress")) {
      uncompress = params.getBoolean("uncompress");
    }
    final boolean finalUncompress = uncompress;


    this.map.snapshot(new GoogleMap.SnapshotReadyCallback() {
      
      @Override
      public void onSnapshotReady(Bitmap image) {
        if (!finalUncompress) {
          float density = Resources.getSystem().getDisplayMetrics().density;
          image = PluginUtil.resizeBitmap(image,
              (int) (image.getWidth() / density),
              (int) (image.getHeight() / density));
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();  
        image.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        String imageEncoded = "data:image/png;base64," + 
                Base64.encodeToString(byteArray, Base64.NO_WRAP);
        
        callbackContext.success(imageEncoded);
      }
    });
    
  }
  public void fromLatLngToPoint(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    double lat, lng;
    lat = args.getDouble(1);
    lng = args.getDouble(2);
    LatLng latLng = new LatLng(lat, lng);
    Point point = map.getProjection().toScreenLocation(latLng);
    JSONArray pointJSON = new JSONArray();
    pointJSON.put(point.x / this.density);
    pointJSON.put(point.y / this.density);
    callbackContext.success(pointJSON);
  }
  
  public void fromPointToLatLng(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    int pointX, pointY;
    pointX = args.getInt(1);
    pointY = args.getInt(2);
    Point point = new Point();
    point.x = pointX;
    point.y = pointY;
    LatLng latlng = map.getProjection().fromScreenLocation(point);
    JSONArray pointJSON = new JSONArray();
    pointJSON.put(latlng.latitude);
    pointJSON.put(latlng.longitude);
    callbackContext.success(pointJSON);
  }
  
  /**
   * Return the visible region of the map
   * Thanks @fschmidt
   */
  public void getVisibleRegion(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    VisibleRegion visibleRegion = map.getProjection().getVisibleRegion();
    LatLngBounds latLngBounds = visibleRegion.latLngBounds;
    JSONObject result = new JSONObject();
    JSONObject northeast = new JSONObject();
    JSONObject southwest = new JSONObject();
    northeast.put("lat", latLngBounds.northeast.latitude);
    northeast.put("lng", latLngBounds.northeast.longitude);
    southwest.put("lat", latLngBounds.southwest.latitude);
    southwest.put("lng", latLngBounds.southwest.longitude);
    result.put("northeast", northeast);
    result.put("southwest", southwest);
    
    JSONArray latLngArray = new JSONArray();
    latLngArray.put(northeast);
    latLngArray.put(southwest);
    result.put("latLngArray", latLngArray);
    
    callbackContext.success(result);
  }
  

  /**
   * Sets the preference for whether all gestures should be enabled or disabled.
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void setAllGesturesEnabled(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Boolean isEnabled = args.getBoolean(1);
    UiSettings uiSettings = map.getUiSettings();
    uiSettings.setAllGesturesEnabled(isEnabled);
    this.sendNoResult(callbackContext);
  }

  /**
   * Sets padding of the map
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void setPadding(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    JSONObject padding = args.getJSONObject(1);
    int left = padding.getInt("left");
    int top = padding.getInt("top");
    int bottom = padding.getInt("bottom");
    int right = padding.getInt("right");
    map.setPadding(left, top, right, bottom);
    this.sendNoResult(callbackContext);
  }
}
