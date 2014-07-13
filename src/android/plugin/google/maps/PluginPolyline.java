package plugin.google.maps;

import java.util.List;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.res.Resources;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
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
      polylineOptions.width(opts.getInt("width") * this.density);
    }
    if (opts.has("visible")) {
      polylineOptions.visible(opts.getBoolean("visible"));
    }
    if (opts.has("geodesic")) {
      //polylineOptions.geodesic(opts.getBoolean("geodesic"));
    }
    if (opts.has("zIndex")) {
      polylineOptions.zIndex(opts.getInt("zIndex"));
    }
    
    Polyline polyline = map.addPolyline(polylineOptions);
    String id = "polyline_" + polyline.getId();
    this.objects.put(id, polyline);
    
    JSONObject result = new JSONObject();
    result.put("hashCode", polyline.hashCode());
    result.put("id", id);
    callbackContext.success(result);
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
    this.setInt("setColor", id, color, callbackContext);
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
    float width = (float) args.getDouble(2) * this.density;
    this.setFloat("setWidth", id, width, callbackContext);
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
    this.setFloat("setZIndex", id, zIndex, callbackContext);
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
    if (polyline == null) {
      callbackContext.success();
      return;
    }
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
  /**
   * set geodesic
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void setGeodesic(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    boolean isGeodisic = args.getBoolean(2);
    this.setBoolean("setGeodesic", id, isGeodisic, callbackContext);
  }

  /**
   * Set visibility for the object
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setVisible(JSONArray args, CallbackContext callbackContext) throws JSONException {
    boolean visible = args.getBoolean(2);
    String id = args.getString(1);
    this.setBoolean("setVisible", id, visible, callbackContext);
  }
}
