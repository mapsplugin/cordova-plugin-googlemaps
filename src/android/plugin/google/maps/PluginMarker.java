package plugin.google.maps;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

public class PluginMarker extends MyPlugin implements MyPluginInterface  {

  private enum Animation {
    DROP,
    BOUNCE
  }

  private ArrayList<AsyncTask> iconLoadingTasks = new ArrayList<AsyncTask>();
  private ArrayList<String> iconCacheKeys = new ArrayList<String>();
  private ArrayList<Bitmap> icons = new ArrayList<Bitmap>();

  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    this.clear();
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (objects == null) {
          return;
        }
        Set<String> keySet = objects.keySet();
        String[] objectIdArray = keySet.toArray(new String[keySet.size()]);

        for (String objectId : objectIdArray) {
          if (objects.containsKey(objectId)) {
            if (objectId.startsWith("marker_") &&
              !objectId.startsWith("marker_property_")) {
              Marker marker = (Marker) objects.remove(objectId);
              marker.setIcon(null);
              marker.remove();
              marker = null;
            } else {
              Object object = objects.remove(objectId);
              object = null;
            }
          }
        }

        objects.clear();
        objects = null;
      }
    });

  }

  @Override
  protected void clear() {
    //--------------------------------------
    // Cancel tasks
    //--------------------------------------
    cordova.getThreadPool().submit(new Runnable() {
      @Override
      public void run() {
        AsyncTask task;
        int i, ilen=iconLoadingTasks.size();
        for (i = 0; i < ilen; i++) {
          task = iconLoadingTasks.remove(i);
          task.cancel(true);
          task = null;
        }
        iconLoadingTasks = null;
      }
    });

    //--------------------------------------
    // Recycle bitmaps as much as possible
    //--------------------------------------
    String[] cacheKeys = iconCacheKeys.toArray(new String[iconCacheKeys.size()]);
    for (int i = 0; i < cacheKeys.length; i++) {
      AsyncLoadImage.removeBitmapFromMemCahce(iconCacheKeys.remove(0));
    }
    Bitmap[] cachedBitmaps = icons.toArray(new Bitmap[icons.size()]);
    Bitmap image;
    for (int i = 0; i < cachedBitmaps.length; i++) {
      image = icons.remove(0);
      if (image != null && !image.isRecycled()) {
        image.recycle();
      }
      image = null;
    }
    icons.clear();

    //--------------------------------------
    // clean up properties as much as possible
    //--------------------------------------
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (objects == null) {
          return;
        }
        Set<String> keySet = objects.keySet();
        String[] objectIdArray = keySet.toArray(new String[keySet.size()]);

        for (String objectId : objectIdArray) {
          if (objects.containsKey(objectId)) {
            if (objectId.startsWith("marker_") &&
                !objectId.startsWith("marker_property_")) {
              Marker marker = (Marker) objects.remove(objectId);
              marker.setIcon(null);
              marker.remove();
              marker = null;
            } else {
              Object object = objects.remove(objectId);
              object = null;
            }
          }
        }

        objects.clear();
        PluginMarker.super.clear();

      }
    });

  }

  /**
   * Create a marker
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  public void create(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    // Create an instance of Marker class
    final MarkerOptions markerOptions = new MarkerOptions();
    final JSONObject properties = new JSONObject();

    final JSONObject opts = args.getJSONObject(1);
    if (opts.has("position")) {
        JSONObject position = opts.getJSONObject("position");
        markerOptions.position(new LatLng(position.getDouble("lat"), position.getDouble("lng")));
    }
    if (opts.has("title")) {
        markerOptions.title(opts.getString("title"));
    }
    if (opts.has("snippet")) {
        markerOptions.snippet(opts.getString("snippet"));
    }
    if (opts.has("visible")) {
      if (opts.has("icon") && !"".equals(opts.getString("icon"))) {
        markerOptions.visible(false);
        properties.put("isVisible", false);
      } else {
        markerOptions.visible(opts.getBoolean("visible"));
        properties.put("isVisible", markerOptions.isVisible());
      }
    }
    if (opts.has("draggable")) {
      markerOptions.draggable(opts.getBoolean("draggable"));
    }
    if (opts.has("rotation")) {
      markerOptions.rotation((float)opts.getDouble("rotation"));
    }
    if (opts.has("flat")) {
      markerOptions.flat(opts.getBoolean("flat"));
    }
    if (opts.has("opacity")) {
      markerOptions.alpha((float) opts.getDouble("opacity"));
    }
    if (opts.has("zIndex")) {
      markerOptions.zIndex((float) opts.getDouble("zIndex"));
    }

    if (opts.has("styles")) {
      properties.put("styles", opts.getJSONObject("styles"));
    }
    if (opts.has("disableAutoPan")) {
      properties.put("disableAutoPan", opts.getBoolean("disableAutoPan"));
    } else {
      properties.put("disableAutoPan", false);
    }
    if (opts.has("noCache")) {
      properties.put("noCache", opts.getBoolean("noCache"));
    } else {
      properties.put("noCache", false);
    }
    if (opts.has("useHtmlInfoWnd")) {
      properties.put("useHtmlInfoWnd", opts.getBoolean("useHtmlInfoWnd"));
    } else {
      properties.put("useHtmlInfoWnd", false);
    }

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final Marker marker = map.addMarker(markerOptions);
        marker.hideInfoWindow();

        cordova.getThreadPool().execute(new Runnable() {
          @Override
          public void run() {

            try {
              // Store the marker
              String id = "marker_" + marker.getId();
              self.objects.put(id, marker);

              self.objects.put("marker_property_" + marker.getId(), properties);

              // Prepare the result
              final JSONObject result = new JSONObject();
              result.put("hashCode", marker.hashCode());
              result.put("id", id);


              // Load icon
              if (opts.has("icon")) {
                //------------------------------
                // Case: have the icon property
                //------------------------------
                Bundle bundle = null;
                Object value = opts.get("icon");
                if (JSONObject.class.isInstance(value)) {
                  JSONObject iconProperty = (JSONObject) value;
                  bundle = PluginUtil.Json2Bundle(iconProperty);

                  // The `anchor` of the `icon` property
                  if (iconProperty.has("anchor")) {
                    value = iconProperty.get("anchor");
                    if (JSONArray.class.isInstance(value)) {
                      JSONArray points = (JSONArray) value;
                      double[] anchorPoints = new double[points.length()];
                      for (int i = 0; i < points.length(); i++) {
                        anchorPoints[i] = points.getDouble(i);
                      }
                      bundle.putDoubleArray("anchor", anchorPoints);
                    }
                  }

                  // The `infoWindowAnchor` property for infowindow
                  if (opts.has("infoWindowAnchor")) {
                    value = opts.get("infoWindowAnchor");
                    if (JSONArray.class.isInstance(value)) {
                      JSONArray points = (JSONArray) value;
                      double[] anchorPoints = new double[points.length()];
                      for (int i = 0; i < points.length(); i++) {
                        anchorPoints[i] = points.getDouble(i);
                      }
                      bundle.putDoubleArray("infoWindowAnchor", anchorPoints);
                    }
                  }
                } else if (JSONArray.class.isInstance(value)) {
                  float[] hsv = new float[3];
                  JSONArray arrayRGBA = (JSONArray) value;
                  Color.RGBToHSV(arrayRGBA.getInt(0), arrayRGBA.getInt(1), arrayRGBA.getInt(2), hsv);
                  bundle = new Bundle();
                  bundle.putFloat("iconHue", hsv[0]);
                } else {
                  bundle = new Bundle();
                  bundle.putString("url", (String) value);
                }

                if (opts.has("animation")) {
                  bundle.putString("animation", opts.getString("animation"));
                }
                PluginMarker.this.setIcon_(marker, bundle, new PluginAsyncInterface() {

                  @Override
                  public void onPostExecute(final Object object) {
                    cordova.getActivity().runOnUiThread(new Runnable() {
                      @Override
                      public void run() {

                        Marker marker = (Marker) object;
                        if (opts.has("visible")) {
                          try {
                            marker.setVisible(opts.getBoolean("visible"));
                          } catch (JSONException e) {
                          }
                        } else {
                          marker.setVisible(true);
                        }


                        // Animation
                        String markerAnimation = null;
                        if (opts.has("animation")) {
                          try {
                            markerAnimation = opts.getString("animation");
                          } catch (JSONException e) {
                            e.printStackTrace();
                          }
                        }
                        if (markerAnimation != null) {
                          PluginMarker.this.setMarkerAnimation_(marker, markerAnimation, new PluginAsyncInterface() {

                            @Override
                            public void onPostExecute(Object object) {
                              Marker marker = (Marker) object;
                              callbackContext.success(result);
                            }

                            @Override
                            public void onError(String errorMsg) {
                              callbackContext.error(errorMsg);
                            }
                          });
                        } else {
                          callbackContext.success(result);
                        }
                      }
                    });
                  }

                  @Override
                  public void onError(String errorMsg) {
                    callbackContext.error(errorMsg);
                  }

                });
              } else {
                //--------------------------
                // Case: no icon property
                //--------------------------
                String markerAnimation = null;
                if (opts.has("animation")) {
                  markerAnimation = opts.getString("animation");
                }
                if (markerAnimation != null) {
                  // Execute animation
                  PluginMarker.this.setMarkerAnimation_(marker, markerAnimation, new PluginAsyncInterface() {

                    @Override
                    public void onPostExecute(Object object) {
                      callbackContext.success(result);
                    }

                    @Override
                    public void onError(String errorMsg) {
                      callbackContext.error(errorMsg);
                    }

                  });
                } else {
                  // Return the result if does not specify the icon property.
                  callbackContext.success(result);
                }
              }
            } catch (Exception e) {
              e.printStackTrace();
              callbackContext.error("" + e.getMessage());
            }
          }
        });
      }
    });



  }

  private void setDropAnimation_(final Marker marker, final PluginAsyncInterface callback) {
    final long startTime = SystemClock.uptimeMillis();
    final long duration = 100;

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final Handler handler = new Handler();
        final Projection proj = map.getProjection();
        final LatLng markerLatLng = marker.getPosition();
        final Point markerPoint = proj.toScreenLocation(markerLatLng);
        final Point startPoint = new Point(markerPoint.x, 0);

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
          @Override
          public void run() {
            LatLng startLatLng = proj.fromScreenLocation(startPoint);
            long elapsed = SystemClock.uptimeMillis() - startTime;
            float t = interpolator.getInterpolation((float) elapsed / duration);
            double lng = t * markerLatLng.longitude + (1 - t) * startLatLng.longitude;
            double lat = t * markerLatLng.latitude + (1 - t) * startLatLng.latitude;
            marker.setPosition(new LatLng(lat, lng));
            if (t < 1.0) {
              // Post again 16ms later.
              handler.postDelayed(this, 16);
            } else {
              marker.setPosition(markerLatLng);
              callback.onPostExecute(marker);
            }
          }
        });
      }
    });
  }

  /**
   * Bounce animation
   * http://android-er.blogspot.com/2013/01/implement-bouncing-marker-for-google.html
   */
  private void setBounceAnimation_(final Marker marker, final PluginAsyncInterface callback) {
    final long startTime = SystemClock.uptimeMillis();
    final long duration = 2000;
    final Interpolator interpolator = new BounceInterpolator();

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {

        final Handler handler = new Handler();
        final Projection projection = map.getProjection();
        final LatLng markerLatLng = marker.getPosition();
        final Point startPoint = projection.toScreenLocation(markerLatLng);
        startPoint.offset(0, -200);


        handler.post(new Runnable() {
          @Override
          public void run() {
            LatLng startLatLng = projection.fromScreenLocation(startPoint);
            long elapsed = SystemClock.uptimeMillis() - startTime;
            float t = interpolator.getInterpolation((float) elapsed / duration);
            double lng = t * markerLatLng.longitude + (1 - t) * startLatLng.longitude;
            double lat = t * markerLatLng.latitude + (1 - t) * startLatLng.latitude;
            marker.setPosition(new LatLng(lat, lng));

            if (t < 1.0) {
              // Post again 16ms later.
              handler.postDelayed(this, 16);
            } else {
              marker.setPosition(markerLatLng);
              callback.onPostExecute(marker);
            }
          }
        });
      }
    });

  }

  private void setMarkerAnimation_(Marker marker, String animationType, PluginAsyncInterface callback) {
    Animation animation = null;
    try {
      animation = Animation.valueOf(animationType.toUpperCase(Locale.US));
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (animation == null) {
      callback.onPostExecute(marker);
      return;
    }
    switch(animation) {
    case DROP:
      this.setDropAnimation_(marker, callback);
      break;

    case BOUNCE:
      this.setBounceAnimation_(marker, callback);
      break;

    default:
      break;
    }
  }


  public void setActiveMarkerId(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final String id = args.getString(0);

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Marker marker = (Marker) objects.get(id);
        if (marker == null) {
          return;
        }
        pluginMap.activeMarker = marker;
      }
    });
  }
  
  /**
   *
   * http://android-er.blogspot.com/2013/01/implement-bouncing-marker-for-google.html
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setAnimation(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String markerId = args.getString(0);
    String animation = args.getString(1);
    final Marker marker = this.getMarker(markerId);

    this.setMarkerAnimation_(marker, animation, new PluginAsyncInterface() {

      @Override
      public void onPostExecute(Object object) {
        callbackContext.success();
      }
      @Override
      public void onError(String errorMsg) {
        callbackContext.error(errorMsg);
      }

    });
  }

  /**
   * Show the InfoWindow bound with the marker
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void showInfoWindow(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final String id = args.getString(0);
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Marker marker = getMarker(id);
        if (marker != null) {
          marker.showInfoWindow();
        }
        sendNoResult(callbackContext);
      }
    });
  }

  /**
   * Set rotation for the marker
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setRotation(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float rotation = (float)args.getDouble(1);
    String id = args.getString(0);
    this.setFloat("setRotation", id, rotation, callbackContext);
  }

  /**
   * Set opacity for the marker
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setOpacity(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float alpha = (float)args.getDouble(1);
    String id = args.getString(0);
    this.setFloat("setAlpha", id, alpha, callbackContext);
  }

  /**
   * Set zIndex for the marker (dummy code, not available on Android V2)
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setZIndex(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float zIndex = (float)args.getDouble(1);
    String id = args.getString(0);
    Marker marker = getMarker(id);
    this.setFloat("setZIndex", id, zIndex, callbackContext);
  }

  /**
   * set position
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setPosition(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final String id = args.getString(0);
    final LatLng position = new LatLng(args.getDouble(1), args.getDouble(2));
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Marker marker = getMarker(id);
        if (marker != null) {
          marker.setPosition(position);
        }
        sendNoResult(callbackContext);
      }
    });
  }

  /**
   * Set flat for the marker
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setFlat(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    boolean isFlat = args.getBoolean(1);
    String id = args.getString(0);
    this.setBoolean("setFlat", id, isFlat, callbackContext);
  }

  /**
   * Set visibility for the object
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setVisible(JSONArray args, CallbackContext callbackContext) throws JSONException {
    boolean isVisible = args.getBoolean(1);
    String id = args.getString(0);

    Marker marker = this.getMarker(id);
    if (marker == null) {
      this.sendNoResult(callbackContext);
      return;
    }
    String propertyId = "marker_property_" + marker.getId();
    JSONObject properties = null;
    if (self.objects.containsKey(propertyId)) {
      properties = (JSONObject)self.objects.get(propertyId);
    } else {
      properties = new JSONObject();
    }
    properties.put("isVisible", isVisible);
    self.objects.put(propertyId, properties);

    this.setBoolean("setVisible", id, isVisible, callbackContext);
  }
  /**
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setDisableAutoPan(JSONArray args, CallbackContext callbackContext) throws JSONException {
    boolean disableAutoPan = args.getBoolean(1);
    String id = args.getString(0);
    Marker marker = this.getMarker(id);
    if (marker == null) {
      this.sendNoResult(callbackContext);
      return;
    }
    String propertyId = "marker_property_" + marker.getId();
    JSONObject properties = null;
    if (self.objects.containsKey(propertyId)) {
      properties = (JSONObject)self.objects.get(propertyId);
    } else {
      properties = new JSONObject();
    }
    properties.put("disableAutoPan", disableAutoPan);
    self.objects.put(propertyId, properties);
    this.sendNoResult(callbackContext);
  }
  /**
   * Set title for the marker
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setTitle(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String title = args.getString(1);
    String id = args.getString(0);
    this.setString("setTitle", id, title, callbackContext);
  }

  /**
   * Set the snippet for the marker
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setSnippet(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String snippet = args.getString(1);
    String id = args.getString(0);
    this.setString("setSnippet", id, snippet, callbackContext);
  }

  /**
   * Hide the InfoWindow binded with the marker
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void hideInfoWindow(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final String id = args.getString(0);
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Marker marker = getMarker(id);
        if (marker != null) {
          marker.hideInfoWindow();
        }
        sendNoResult(callbackContext);
      }
    });
  }

  /**
   * Remove the marker
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void remove(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final String id = args.getString(0);
    final Marker marker = this.getMarker(id);
    if (marker == null) {
      callbackContext.success();
      return;
    }

    String[] cacheKeys = iconCacheKeys.toArray(new String[iconCacheKeys.size()]);
    for (int i = 0; i < cacheKeys.length; i++) {
      AsyncLoadImage.removeBitmapFromMemCahce(cacheKeys[i]);
    }

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        marker.remove();
        if (objects != null) {
          objects.remove(id);
        }


        String propertyId = "marker_property_" + id;
        objects.remove(propertyId);
        sendNoResult(callbackContext);
      }
    });
  }

  /**
   * Set anchor for the icon of the marker
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setIconAnchor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float anchorX = (float)args.getDouble(1);
    float anchorY = (float)args.getDouble(2);
    String id = args.getString(0);
    Marker marker = this.getMarker(id);

    Bundle imageSize = (Bundle) self.objects.get("imageSize");
    if (imageSize != null) {
      this._setIconAnchor(marker, anchorX, anchorY, imageSize.getInt("width"), imageSize.getInt("height"));
    }
    this.sendNoResult(callbackContext);
  }


  /**
   * Set anchor for the InfoWindow of the marker
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setInfoWindowAnchor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float anchorX = (float)args.getDouble(1);
    float anchorY = (float)args.getDouble(2);
    String id = args.getString(0);
    Marker marker = this.getMarker(id);

    Bundle imageSize = (Bundle) self.objects.get("imageSize");
    if (imageSize != null) {
      this._setInfoWindowAnchor(marker, anchorX, anchorY, imageSize.getInt("width"), imageSize.getInt("height"));
    }
    this.sendNoResult(callbackContext);
  }

  /**
   * Set draggable for the marker
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setDraggable(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Boolean draggable = args.getBoolean(1);
    String id = args.getString(0);
    this.setBoolean("setDraggable", id, draggable, callbackContext);
  }

  /**
   * Set icon of the marker
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setIcon(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    Marker marker = this.getMarker(id);
    Object value = args.get(1);
    Bundle bundle = null;
    if (JSONObject.class.isInstance(value)) {
      JSONObject iconProperty = (JSONObject)value;
      bundle = PluginUtil.Json2Bundle(iconProperty);

      // The `anchor` for icon
      if (iconProperty.has("anchor")) {
        value = iconProperty.get("anchor");
        if (JSONArray.class.isInstance(value)) {
          JSONArray points = (JSONArray)value;
          double[] anchorPoints = new double[points.length()];
          for (int i = 0; i < points.length(); i++) {
            anchorPoints[i] = points.getDouble(i);
          }
          bundle.putDoubleArray("anchor", anchorPoints);
        }
      }
    } else if (JSONArray.class.isInstance(value)) {
      float[] hsv = new float[3];
      JSONArray arrayRGBA = (JSONArray)value;
      Color.RGBToHSV(arrayRGBA.getInt(0), arrayRGBA.getInt(1), arrayRGBA.getInt(2), hsv);
      bundle = new Bundle();
      bundle.putFloat("iconHue", hsv[0]);
    } else if (String.class.isInstance(value)) {
      bundle = new Bundle();
      bundle.putString("url", (String)value);
    }
    if (bundle != null) {
      this.setIcon_(marker, bundle, new PluginAsyncInterface() {

        @Override
        public void onPostExecute(Object object) {
          PluginMarker.this.sendNoResult(callbackContext);
        }

        @Override
        public void onError(String errorMsg) {
          callbackContext.error(errorMsg);
        }
      });
    } else {
      this.sendNoResult(callbackContext);
    }
  }

  private void setIcon_(final Marker marker, final Bundle iconProperty, final PluginAsyncInterface callback) {
    boolean noCaching = false;
    if (iconProperty.containsKey("noCache")) {
      noCaching = iconProperty.getBoolean("noCache");
    }
    if (iconProperty.containsKey("iconHue")) {
      cordova.getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          float hue = iconProperty.getFloat("iconHue");
          marker.setIcon(BitmapDescriptorFactory.defaultMarker(hue));
        }
      });
      callback.onPostExecute(marker);
    }

    String iconUrl = iconProperty.getString("url");
    if (iconUrl == null) {
      callback.onPostExecute(marker);
      return;
    }

    if (!iconUrl.contains("://") &&
      !iconUrl.startsWith("/") &&
      !iconUrl.startsWith("www/") &&
      !iconUrl.startsWith("data:image") &&
        !iconUrl.startsWith("./") &&
        !iconUrl.startsWith("../")) {
      iconUrl = "./" + iconUrl;
    }
    if (iconUrl.startsWith("./")  || iconUrl.startsWith("../")) {
      iconUrl = iconUrl.replace("././", "./");
      String currentPage = CURRENT_PAGE_URL;
      currentPage = currentPage.replaceAll("[^\\/]*$", "");
      iconUrl = currentPage + "/" + iconUrl;
    }

    if (iconUrl == null) {
      callback.onPostExecute(marker);
      return;
    }

    iconProperty.putString("url", iconUrl);

    if (iconUrl.indexOf("http") != 0) {
      //----------------------------------
      // Load icon from local file
      //----------------------------------
      AsyncTask<Void, Void, Bitmap> task = new AsyncTask<Void, Void, Bitmap>() {

        @Override
        protected Bitmap doInBackground(Void... params) {
          String iconUrl = iconProperty.getString("url");
          if (iconUrl == null) {
            return null;
          }

          Bitmap image = null;
          if (iconUrl.indexOf("cdvfile://") == 0) {
            CordovaResourceApi resourceApi = webView.getResourceApi();
            iconUrl = PluginUtil.getAbsolutePathFromCDVFilePath(resourceApi, iconUrl);
          }
          if (iconUrl == null) {
            return null;
          }

          if (iconUrl.indexOf("data:image/") == 0 && iconUrl.contains(";base64,")) {
            String[] tmp = iconUrl.split(",");
            image = PluginUtil.getBitmapFromBase64encodedImage(tmp[1]);
          } else if (iconUrl.indexOf("file://") == 0 &&
              !iconUrl.contains("file:///android_asset/")) {
            iconUrl = iconUrl.replace("file://", "");
            File tmp = new File(iconUrl);
            if (tmp.exists()) {
              image = BitmapFactory.decodeFile(iconUrl);
            } else {
              //if (PluginMarker.this.mapCtrl.mPluginLayout.isDebug) {
                Log.w("GoogleMaps", "icon is not found (" + iconUrl + ")");
              //}
            }
          } else {
            //Log.d(TAG, "iconUrl = " + iconUrl);
            if (iconUrl.indexOf("file:///android_asset/") == 0) {
              iconUrl = iconUrl.replace("file:///android_asset/", "");
            }
            //Log.d(TAG, "iconUrl = " + iconUrl);
            if (iconUrl.contains("./")) {
              try {
                boolean isAbsolutePath = iconUrl.startsWith("/");
                File relativePath = new File(iconUrl);
                iconUrl = relativePath.getCanonicalPath();
                //Log.d(TAG, "iconUrl = " + iconUrl);
                if (!isAbsolutePath) {
                  iconUrl = iconUrl.substring(1);
                }
                //Log.d(TAG, "iconUrl = " + iconUrl);
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
            AssetManager assetManager = PluginMarker.this.cordova.getActivity().getAssets();
            InputStream inputStream;
            try {
              inputStream = assetManager.open(iconUrl);
              image = BitmapFactory.decodeStream(inputStream);
              inputStream.close();
            } catch (IOException e) {
              e.printStackTrace();
              return null;
            }
          }
          if (image == null) {
            return null;
          }
          icons.add(image);

          Boolean isResized = false;
          if (iconProperty.containsKey("size")) {
            Object size = iconProperty.get("size");

            if (Bundle.class.isInstance(size)) {

              Bundle sizeInfo = (Bundle)size;
              int width = sizeInfo.getInt("width", 0);
              int height = sizeInfo.getInt("height", 0);
              if (width > 0 && height > 0) {
                isResized = true;
                width = (int)Math.round(width * PluginMarker.this.density);
                height = (int)Math.round(height * PluginMarker.this.density);
                image = PluginUtil.resizeBitmap(image, width, height);
              }
            }
          }

          if (!isResized) {
            image = PluginUtil.scaleBitmapForDevice(image);
          }
          icons.add(image);

          return image;
        }

        @Override
        protected void onPostExecute(Bitmap image) {
          if (image == null) {
            callback.onPostExecute(marker);
            return;
          }
          if (image.isRecycled()) {
            //Maybe the task was canceled by map.clean()?
            callback.onError("Can not get image for marker. Maybe the task was canceled by map.clean()?");
            return;
          }

          try {
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(image);
            if (bitmapDescriptor == null) {
              callback.onPostExecute(marker);
              return;
            }
            marker.setIcon(bitmapDescriptor);

            // Save the information for the anchor property
            Bundle imageSize = new Bundle();
            imageSize.putInt("width", image.getWidth());
            imageSize.putInt("height", image.getHeight());
            self.objects.put("imageSize", imageSize);


            // The `anchor` of the `icon` property
            if (iconProperty.containsKey("anchor")) {
              double[] anchor = iconProperty.getDoubleArray("anchor");
              if (anchor.length == 2) {
                _setIconAnchor(marker, anchor[0], anchor[1], imageSize.getInt("width"), imageSize.getInt("height"));
              }
            }


            // The `anchor` property for the infoWindow
            if (iconProperty.containsKey("infoWindowAnchor")) {
              double[] anchor = iconProperty.getDoubleArray("infoWindowAnchor");
              if (anchor.length == 2) {
                _setInfoWindowAnchor(marker, anchor[0], anchor[1], imageSize.getInt("width"), imageSize.getInt("height"));
              }
            }

            callback.onPostExecute(marker);

          } catch (Exception e) {
            Log.e(TAG,"PluginMarker: Warning - marker method called when marker has been disposed, wait for addMarker callback before calling more methods on the marker (setIcon etc).");
            //e.printStackTrace();
            try {
              marker.remove();
            } catch (Exception ignore) {
              ignore = null;
            }
            callback.onError(e.getMessage() + "");
          }
        }
      };
      task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      iconLoadingTasks.add(task);


      return;
    }

    if (iconUrl.indexOf("http") == 0) {
      //----------------------------------
      // Load icon from on the internet
      //----------------------------------
      int width = -1;
      int height = -1;
      if (iconProperty.containsKey("size")) {

        Bundle sizeInfo = (Bundle) iconProperty.get("size");
        width = sizeInfo.getInt("width", width);
        height = sizeInfo.getInt("height", height);
      }

      String cacheKey = AsyncLoadImage.getCacheKey(iconUrl, width, height);
      iconCacheKeys.add(cacheKey);

      AsyncLoadImage task = new AsyncLoadImage("Mozilla", width, height, noCaching, new AsyncLoadImageInterface() {

        @Override
        public void onPostExecute(Bitmap image) {

          if (image == null) {
            callback.onPostExecute(marker);
            return;
          }

          if (image.isRecycled()) {
            //Maybe the task was canceled by map.clean()?
            callback.onError("Can not get image for marker. Maybe the task was canceled by map.clean()?");
            return;
          }
          try {
            icons.add(image);
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(image);
            marker.setIcon(bitmapDescriptor);

            // Save the information for the anchor property
            Bundle imageSize = new Bundle();
            imageSize.putInt("width", image.getWidth());
            imageSize.putInt("height", image.getHeight());
            self.objects.put("imageSize", imageSize);

            // The `anchor` of the `icon` property
            if (iconProperty.containsKey("anchor")) {
              double[] anchor = iconProperty.getDoubleArray("anchor");
              if (anchor.length == 2) {
                _setIconAnchor(marker, anchor[0], anchor[1], imageSize.getInt("width"), imageSize.getInt("height"));
              }
            }

            // The `anchor` property for the infoWindow
            if (iconProperty.containsKey("infoWindowAnchor")) {
              double[] anchor = iconProperty.getDoubleArray("infoWindowAnchor");
              if (anchor.length == 2) {
                _setInfoWindowAnchor(marker, anchor[0], anchor[1], imageSize.getInt("width"), imageSize.getInt("height"));
              }
            }

            image.recycle();
            callback.onPostExecute(marker);
          } catch (Exception e) {
            //e.printStackTrace();
            try {
              marker.remove();
            } catch (Exception ignore) {
              ignore = null;
            }
            callback.onError(e.getMessage() + "");
          }
        }
      });
      task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, iconUrl);
      iconLoadingTasks.add(task);
    }
  }

  private void _setIconAnchor(final Marker marker, double anchorX, double anchorY, final int imageWidth, final int imageHeight) {
    // The `anchor` of the `icon` property
    anchorX = anchorX * this.density;
    anchorY = anchorY * this.density;
    final double fAnchorX = anchorX;
    final double fAnchorY = anchorY;
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        marker.setAnchor((float)(fAnchorX / imageWidth), (float)(fAnchorY / imageHeight));
      }
    });
  }
  private void _setInfoWindowAnchor(final Marker marker, double anchorX, double anchorY, final int imageWidth, final int imageHeight) {
    // The `anchor` of the `icon` property
    anchorX = anchorX * this.density;
    anchorY = anchorY * this.density;
    final double fAnchorX = anchorX;
    final double fAnchorY = anchorY;
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        marker.setInfoWindowAnchor((float)(fAnchorX / imageWidth), (float)(fAnchorY / imageHeight));
      }
    });
  }
}
