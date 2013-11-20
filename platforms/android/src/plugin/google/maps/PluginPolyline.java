package plugin.google.maps;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class PluginPolyline extends MyPlugin implements MyPluginInterface  {
  /**
   * Create polyline
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void createPolyline(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final PolylineOptions polylineOptions = new PolylineOptions();
    int color;
    
    JSONObject opts = args.getJSONObject(1);
    if (opts.has("points")) {
      JSONArray points = opts.getJSONArray("points");
      List<LatLng> path = PluginUtil.JSONArray2LatLngList(points);
      int i = 0;
      for (i = 0; i < path.size(); i++) {
        polylineOptions.add(path.get(i));
      }
    }
    if (opts.has("color")) {
      color = PluginUtil.parsePluginColor(opts.getJSONArray("color"));
      polylineOptions.color(color);
    }
    if (opts.has("width")) {
      polylineOptions.width(opts.getInt("width"));
    }
    if (opts.has("visible")) {
      polylineOptions.visible(opts.getBoolean("visible"));
    }
    if (opts.has("geodesic")) {
      polylineOptions.geodesic(opts.getBoolean("geodesic"));
    }
    if (opts.has("zIndex")) {
      polylineOptions.zIndex(opts.getInt("zIndex"));
    }
    
    Polyline polyline = map.addPolyline(polylineOptions);
    this.objects.put(polyline.getId(), polyline);
    callbackContext.success(polyline.getId());
  }
  
  
  /**
   * set color
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void setColor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    int color = PluginUtil.parsePluginColor(args.getJSONArray(2));
    Polyline polyline = this.getPolyline(id);
    polyline.setColor(color);
    callbackContext.success();
  }
  
  /**
   * set width
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void setWidth(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    float width = (float) args.getDouble(2);
    Polyline polyline = this.getPolyline(id);
    polyline.setWidth(width);
    callbackContext.success();
  }
  
  /**
   * set z-index
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void setZIndex(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    float zIndex = (float) args.getDouble(2);
    Polyline polyline = this.getPolyline(id);
    polyline.setZIndex(zIndex);
    callbackContext.success();
  }
  
  /**
   * set visibility
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void setVisible(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    boolean visible = args.getBoolean(2);
    Polyline polyline = this.getPolyline(id);
    polyline.setVisible(visible);
    callbackContext.success();
  }

  /**
   * Remove the polyline
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void remove(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    Polyline polyline = this.getPolyline(id);
    this.objects.remove(id);
    polyline.remove();
    callbackContext.success();
  }
  /**
   * Set points
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void setPoints(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    Polyline polyline = this.getPolyline(id);
    
    JSONArray points = args.getJSONArray(2);
    List<LatLng> path = PluginUtil.JSONArray2LatLngList(points);
    polyline.setPoints(path);
    
    callbackContext.success();
  }
}
