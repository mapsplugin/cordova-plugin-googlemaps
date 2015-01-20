package plugin.google.maps;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class PluginMarker extends MyPlugin {
  private HashMap<String, Bitmap> cache = null;
  
  /**
   * Create a marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void createMarker(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (cache == null) {
      cache = new HashMap<String, Bitmap>();
    }
    
    // Create an instance of Marker class
    final MarkerOptions markerOptions = new MarkerOptions();
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
      if (opts.has("icon") && "".equals(opts.getString("icon")) == false) {
        markerOptions.visible(false);
      } else {
        markerOptions.visible(opts.getBoolean("visible"));
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
    Marker marker = map.addMarker(markerOptions);

    
    // Store the marker
    String id = "marker_" + marker.getId();
    this.objects.put(id, marker);

    JSONObject properties = new JSONObject();
    if (opts.has("styles")) {
      properties.put("styles", opts.getJSONObject("styles"));
    }
    if (opts.has("disableAutoPan")) {
      properties.put("disableAutoPan", opts.getBoolean("disableAutoPan"));
    } else {
      properties.put("disableAutoPan", false);
    }
    this.objects.put("marker_property_" + marker.getId(), properties);

    // Prepare the result
    final JSONObject result = new JSONObject();
    result.put("hashCode", marker.hashCode());
    result.put("id", id);
    
    // Load icon
    if (opts.has("icon")) {
      Bundle bundle = null;
      Object value = opts.get("icon");
      if (JSONObject.class.isInstance(value)) {
        JSONObject iconProperty = (JSONObject)value;
        bundle = PluginUtil.Json2Bundle(iconProperty);
        
        // The `anchor` of the `icon` property
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

        // The `infoWindowAnchor` property for infowindow
        if (opts.has("infoWindowAnchor")) {
          value = opts.get("infoWindowAnchor");
          if (JSONArray.class.isInstance(value)) {
            JSONArray points = (JSONArray)value;
            double[] anchorPoints = new double[points.length()];
            for (int i = 0; i < points.length(); i++) {
              anchorPoints[i] = points.getDouble(i);
            }
            bundle.putDoubleArray("infoWindowAnchor", anchorPoints);
          }
        }
      } else {
        bundle = new Bundle();
        bundle.putString("url", (String)value);
      }
      this.setIcon_(marker, bundle, new PluginMarkerInterface() {

        @Override
        public void onMarkerIconLoaded(Marker marker) {
          if (opts.has("visible")) {
            try {
              marker.setVisible(opts.getBoolean("visible"));
            } catch (JSONException e) {}
          } else {
            marker.setVisible(true);
          }
          callbackContext.success(result);
        }
        
      });
    } else {
      // Return the result if does not specify the icon property.
      callbackContext.success(result);
    }
    
  }
  

  /**
   * Show the InfoWindow binded with the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void showInfoWindow(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    Marker marker = this.getMarker(id);
    marker.showInfoWindow();
    this.sendNoResult(callbackContext);
  }

  /**
   * Set rotation for the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setRotation(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float rotation = (float)args.getDouble(2);
    String id = args.getString(1);
    this.setFloat("setRotation", id, rotation, callbackContext);
  }
  
  /**
   * Set opacity for the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setOpacity(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float alpha = (float)args.getDouble(2);
    String id = args.getString(1);
    this.setFloat("setAlpha", id, alpha, callbackContext);
  }
  
  /**
   * set position
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void setPosition(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    LatLng position = new LatLng(args.getDouble(2), args.getDouble(3));
    Marker marker = this.getMarker(id);
    marker.setPosition(position);
    this.sendNoResult(callbackContext);
  }
  
  /**
   * Set flat for the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setFlat(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    boolean isFlat = args.getBoolean(2);
    String id = args.getString(1);
    this.setBoolean("setFlat", id, isFlat, callbackContext);
  }

  /**
   * Set visibility for the object
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  protected void setVisible(JSONArray args, CallbackContext callbackContext) throws JSONException {
    boolean visible = args.getBoolean(2);
    String id = args.getString(1);
    this.setBoolean("setVisible", id, visible, callbackContext);
  }
  /**
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  protected void setDisableAutoPan(JSONArray args, CallbackContext callbackContext) throws JSONException {
    boolean disableAutoPan = args.getBoolean(2);
    String id = args.getString(1);
    Marker marker = this.getMarker(id);
    String propertyId = "marker_property_" + marker.getId();
    JSONObject properties = null;
    if (this.objects.containsKey(propertyId)) {
      properties = (JSONObject)this.objects.get(propertyId);
    } else {
      properties = new JSONObject();
    }
    properties.put("disableAutoPan", disableAutoPan);
    this.objects.put(propertyId, properties);
    this.sendNoResult(callbackContext);
  }
  /**
   * Set title for the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setTitle(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String title = args.getString(2);
    String id = args.getString(1);
    this.setString("setTitle", id, title, callbackContext);
  }
  
  /**
   * Set the snippet for the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setSnippet(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String snippet = args.getString(2);
    String id = args.getString(1);
    this.setString("setSnippet", id, snippet, callbackContext);
  }
  
  /**
   * Hide the InfoWindow binded with the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void hideInfoWindow(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    Marker marker = this.getMarker(id);
    marker.hideInfoWindow();
    this.sendNoResult(callbackContext);
  }

  /**
   * Return the position of the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void getPosition(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    Marker marker = this.getMarker(id);
    LatLng position = marker.getPosition();
    
    JSONObject result = new JSONObject();
    result.put("lat", position.latitude);
    result.put("lng", position.longitude);
    callbackContext.success(result);
  }
  
  /**
   * Return 1 if the InfoWindow of the marker is shown
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void isInfoWindowShown(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    Marker marker = this.getMarker(id);
    Boolean isInfoWndShown = marker.isInfoWindowShown();
    callbackContext.success(isInfoWndShown ? 1 : 0);
  }
  
  /**
   * Remove the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void remove(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    Marker marker = this.getMarker(id);
    if (marker == null) {
      callbackContext.success();
      return;
    }
    marker.remove();
    this.objects.remove(id);
    
    String propertyId = "marker_property_" + id;
    this.objects.remove(propertyId);
    this.sendNoResult(callbackContext);
  }
  
  /**
   * Set anchor for the icon of the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setIconAnchor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float anchorX = (float)args.getDouble(2);
    float anchorY = (float)args.getDouble(3);
    String id = args.getString(1);
    Marker marker = this.getMarker(id);
    
    Bundle imageSize = (Bundle) this.objects.get("imageSize");
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
  @SuppressWarnings("unused")
  private void setInfoWindowAnchor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float anchorX = (float)args.getDouble(2);
    float anchorY = (float)args.getDouble(3);
    String id = args.getString(1);
    Marker marker = this.getMarker(id);
    
    Bundle imageSize = (Bundle) this.objects.get("imageSize");
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
  @SuppressWarnings("unused")
  private void setDraggable(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Boolean draggable = args.getBoolean(2);
    String id = args.getString(1);
    this.setBoolean("setDraggable", id, draggable, callbackContext);
  }
  
  /**
   * Set icon of the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setIcon(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    Marker marker = this.getMarker(id);
    Object value = args.get(2);
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
      
    } else if (String.class.isInstance(value)) {
      bundle = new Bundle();
      bundle.putString("url", (String)value);
    }
    if (bundle != null) {
      this.setIcon_(marker, bundle, new PluginMarkerInterface() {

        @Override
        public void onMarkerIconLoaded(Marker marker) {
          PluginMarker.this.sendNoResult(callbackContext);
        }
        
      });
    } else {
      this.sendNoResult(callbackContext);
    }
  }
  
  private void setIcon_(final Marker marker, final Bundle iconProperty, final PluginMarkerInterface callback) {
    String iconUrl = iconProperty.getString("url");
    if (iconUrl == null) {
      callback.onMarkerIconLoaded(marker);
      return;
    }
    
    if (iconUrl.indexOf("http") == -1) {
      Bitmap image = null;
      
      if (iconUrl.indexOf("data:image/") > -1 && iconUrl.indexOf(";base64,") > -1) {
        String[] tmp = iconUrl.split(",");
        image = PluginUtil.getBitmapFromBase64encodedImage(tmp[1]);
      } else {
        AssetManager assetManager = this.cordova.getActivity().getAssets();
        InputStream inputStream;
        try {
          inputStream = assetManager.open(iconUrl);
          image = BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
          e.printStackTrace();
          callback.onMarkerIconLoaded(marker);
          return;
        }
      }
      if (image == null) {
        callback.onMarkerIconLoaded(marker);
        return;
      }
      
      Boolean isResized = false;
      if (iconProperty.containsKey("size") == true) {
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
      
      if (isResized == false) {
        image = PluginUtil.scaleBitmapForDevice(image);
      }
      BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(image);
      marker.setIcon(bitmapDescriptor);
      
      // Save the information for the anchor property
      Bundle imageSize = new Bundle();
      imageSize.putInt("width", image.getWidth());
      imageSize.putInt("height", image.getHeight());
      this.objects.put("imageSize", imageSize);
      

      // The `anchor` of the `icon` property
      if (iconProperty.containsKey("anchor") == true) {
        double[] anchor = iconProperty.getDoubleArray("anchor");
        if (anchor.length == 2) {
          _setIconAnchor(marker, anchor[0], anchor[1], imageSize.getInt("width"), imageSize.getInt("height"));
        }
      }
      

      // The `anchor` property for the infoWindow
      if (iconProperty.containsKey("infoWindowAnchor") == true) {
        double[] anchor = iconProperty.getDoubleArray("infoWindowAnchor");
        if (anchor.length == 2) {
          _setInfoWindowAnchor(marker, anchor[0], anchor[1], imageSize.getInt("width"), imageSize.getInt("height"));
        }
      }
      callback.onMarkerIconLoaded(marker);
      return;
    }
    
    if (iconUrl.indexOf("http") == 0) {
      int width = -1;
      int height = -1;
      if (iconProperty.containsKey("size") == true) {
          
        Bundle sizeInfo = (Bundle) iconProperty.get("size");
        width = sizeInfo.getInt("width", width);
        height = sizeInfo.getInt("height", height);
      }
      
      AsyncLoadImage task = new AsyncLoadImage(width, height, new AsyncLoadImageInterface() {

        @Override
        public void onPostExecute(Bitmap image) {
          if (image == null) {
            callback.onMarkerIconLoaded(marker);
            return;
          }
            
          BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(image);
          marker.setIcon(bitmapDescriptor);
          
          // Save the information for the anchor property
          Bundle imageSize = new Bundle();
          imageSize.putInt("width", image.getWidth());
          imageSize.putInt("height", image.getHeight());
          PluginMarker.this.objects.put("imageSize", imageSize);
          
          // The `anchor` of the `icon` property
          if (iconProperty.containsKey("anchor") == true) {
            double[] anchor = iconProperty.getDoubleArray("anchor");
            if (anchor.length == 2) {
              _setIconAnchor(marker, anchor[0], anchor[1], imageSize.getInt("width"), imageSize.getInt("height"));
            }
          }

          // The `anchor` property for the infoWindow
          if (iconProperty.containsKey("infoWindowAnchor") == true) {
            double[] anchor = iconProperty.getDoubleArray("infoWindowAnchor");
            if (anchor.length == 2) {
              _setInfoWindowAnchor(marker, anchor[0], anchor[1], imageSize.getInt("width"), imageSize.getInt("height"));
            }
          }

          image.recycle();
          callback.onMarkerIconLoaded(marker);
        }
        
      }, cache);
      task.execute(iconUrl);
    }
  }

  private void _setIconAnchor(Marker marker, double anchorX, double anchorY, int imageWidth, int imageHeight) {
    // The `anchor` of the `icon` property
    anchorX = anchorX * this.density;
    anchorY = anchorY * this.density;
    marker.setAnchor((float)(anchorX / imageWidth), (float)(anchorY / imageHeight));
  }
  private void _setInfoWindowAnchor(Marker marker, double anchorX, double anchorY, int imageWidth, int imageHeight) {
    // The `anchor` of the `icon` property
    anchorX = anchorX * this.density;
    anchorY = anchorY * this.density;
    marker.setInfoWindowAnchor((float)(anchorX / imageWidth), (float)(anchorY / imageHeight));
  }
}
