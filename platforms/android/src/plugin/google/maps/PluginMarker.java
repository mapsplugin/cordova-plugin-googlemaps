package plugin.google.maps;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class PluginMarker extends MyPlugin {
  
  /**
   * Create a marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void createMarker(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    // Create an instance of Marker class
    final MarkerOptions markerOptions = new MarkerOptions();
    String iconUrl = null;
    JSONObject opts = args.getJSONObject(1);
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
      markerOptions.visible(opts.getBoolean("visible"));
    }
    if (opts.has("rotation")) {
      markerOptions.rotation((float)opts.getDouble("rotation"));
    }
    if (opts.has("flat")) {
      markerOptions.flat(opts.getBoolean("flat"));
    }
    if (opts.has("alpha")) {
      markerOptions.alpha((float) opts.getDouble("alpha"));
    }
    if (opts.has("icon")) {
      iconUrl = opts.getString("icon");
    }
    Marker marker = map.addMarker(markerOptions);
    
    // Store the marker
    String id = "marker_" + marker.getId();
    this.objects.put(id, marker);
    
    
    // Load icon
    if (iconUrl != null && iconUrl.length() > 0) {
      if (iconUrl.indexOf("http") == 0) {
          AsyncLoadImage task = new AsyncLoadImage(marker, "setIcon");
          task.execute(iconUrl);
      } else {
          marker.setIcon(BitmapDescriptorFactory.fromAsset(iconUrl));
      }
    }
    
    //Return the result
    JSONObject result = new JSONObject();
    result.put("hashCode", marker.hashCode());
    result.put("id", id);
    callbackContext.success(result);
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
    callbackContext.success();
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
    callbackContext.success();
  }
  
  /**
   * Set alpha for the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setAlpha(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float alpha = (float)args.getDouble(2);
    String id = args.getString(1);
    this.setFloat("setAlpha", id, alpha, callbackContext);
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
    callbackContext.success();
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
    marker.remove();
    this.objects.remove(id);
    callbackContext.success();
  }
  
  /**
   * Set anchor for the InfoWindow of the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setAnchor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float anchorU = (float)args.getDouble(2);
    float anchorV = (float)args.getDouble(3);
    String id = args.getString(1);
    Marker marker = this.getMarker(id);
    marker.setAnchor(anchorU, anchorV);
    callbackContext.success();
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
    String iconUrl = args.getString(2);
    
    if (iconUrl.indexOf("http") == 0) {
        AsyncLoadImage task = new AsyncLoadImage(marker, "setIcon");
        task.execute(iconUrl);
    } else {
        marker.setIcon(BitmapDescriptorFactory.fromAsset(iconUrl));
    }
    callbackContext.success();
  }
  
}
