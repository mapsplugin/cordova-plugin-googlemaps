package plugin.google.maps;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class PluginMarker extends CordovaPlugin implements MyPluginInterface  {
  private final String TAG = "PluginMarker";
  private HashMap<Integer, Marker> markers;

  public GoogleMaps mapCtrl = null;
  public GoogleMap map = null;
  
  public void setMapCtrl(GoogleMaps mapCtrl) {
    this.mapCtrl = mapCtrl;
    this.map = mapCtrl.map;
  }
  
  @SuppressLint("UseSparseArrays")
  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    Log.d(TAG, "Marker class initializing");
    this.markers = new HashMap<Integer, Marker>();
  }
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    String[] params = args.getString(0).split("\\.");
    try {
      Method method = this.getClass().getDeclaredMethod(params[1], JSONArray.class, CallbackContext.class);
      method.invoke(this, args, callbackContext);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
      return false;
    }
  }
  
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
    if (opts.has("lat") && opts.has("lng")) {
        markerOptions.position(new LatLng(opts.getDouble("lat"), opts.getDouble("lng")));
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
    this.markers.put(marker.hashCode(), marker);
    
    
    // Load icon
    if (iconUrl != null && iconUrl.length() > 0) {
      Log.d(TAG, iconUrl);
      if (iconUrl.indexOf("http") == 0) {
          MarkerSetIcon task = new MarkerSetIcon(marker);
          task.execute(iconUrl);
      } else {
          marker.setIcon(BitmapDescriptorFactory.fromAsset(iconUrl));
      }
    }
    
    //Return the result
    JSONObject result = new JSONObject();
    result.put("hashCode", marker.hashCode());
    callbackContext.success(marker.hashCode());
  }
  

  /**
   * Show the InfoWindow binded with the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void showInfoWindow(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    int hashCode = args.getInt(1);
    Marker marker = this.markers.get(hashCode);
    marker.showInfoWindow();
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
    int hashCode = args.getInt(1);
    float alpha = (float)args.getDouble(2);
    Marker marker = this.markers.get(hashCode);
    marker.setAlpha(alpha);
    callbackContext.success();
  }
  /**
   * Set flat for the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setFlat(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    int hashCode = args.getInt(1);
    boolean isFlat = args.getBoolean(2);
    Marker marker = this.markers.get(hashCode);
    marker.setFlat(isFlat);
    callbackContext.success();
  }
  
  /**
   * Set title for the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setTitle(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    int hashCode = args.getInt(1);
    String title = args.getString(2);
    Marker marker = this.markers.get(hashCode);
    marker.setTitle(title);
    callbackContext.success();
  }
  
  /**
   * Set the snippet for the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setSnippet(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    int hashCode = args.getInt(1);
    String snippet = args.getString(2);
    Marker marker = this.markers.get(hashCode);
    marker.setSnippet(snippet);
    callbackContext.success();
  }
  
  /**
   * Hide the InfoWindow binded with the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void hideInfoWindow(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    int hashCode = args.getInt(1);
    Marker marker = this.markers.get(hashCode);
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
    int hashCode = args.getInt(1);
    Marker marker = this.markers.get(hashCode);
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
    int hashCode = args.getInt(1);
    Marker marker = this.markers.get(hashCode);
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
    int hashCode = args.getInt(1);
    Marker marker = this.markers.get(hashCode);
    markers.remove(marker.hashCode());
    marker.remove();
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
    int hashCode = args.getInt(1);
    float anchorU = (float)args.getDouble(2);
    float anchorV = (float)args.getDouble(3);
    Marker marker = this.markers.get(hashCode);
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
    int hashCode = args.getInt(1);
    Boolean draggable = args.getBoolean(2);
    Marker marker = this.markers.get(hashCode);
    marker.setDraggable(draggable);
    callbackContext.success();
  }
  
  /**
   * Set icon of the marker
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setIcon(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    int hashCode = args.getInt(1);
    String iconUrl = args.getString(2);
    
    Marker marker = this.markers.get(hashCode);
    if (iconUrl.indexOf("http") == 0) {
        MarkerSetIcon task = new MarkerSetIcon(marker);
        task.execute(iconUrl);
    } else {
        marker.setIcon(BitmapDescriptorFactory.fromAsset(iconUrl));
    }
    callbackContext.success();
  }
}
