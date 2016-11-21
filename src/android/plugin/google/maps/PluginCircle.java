package plugin.google.maps;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

public class PluginCircle extends MyPlugin  {

  /**
   * Create circle
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void createCircle(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final CircleOptions circleOptions = new CircleOptions();
    int color;
    
    JSONObject opts = args.getJSONObject(1);
    if (opts.has("center")) {
      JSONObject center = opts.getJSONObject("center");
      circleOptions.center(new LatLng(center.getDouble("lat"), center.getDouble("lng")));
    }
    if (opts.has("radius")) {
      circleOptions.radius(opts.getDouble("radius"));
    }
    if (opts.has("strokeColor")) {
      color = PluginUtil.parsePluginColor(opts.getJSONArray("strokeColor"));
      circleOptions.strokeColor(color);
    }
    if (opts.has("fillColor")) {
      color = PluginUtil.parsePluginColor(opts.getJSONArray("fillColor"));
      circleOptions.fillColor(color);
    }
    if (opts.has("strokeWidth")) {
      circleOptions.strokeWidth(opts.getInt("strokeWidth") * this.density);
    }
    if (opts.has("visible")) {
      circleOptions.visible(opts.getBoolean("visible"));
    }
    if (opts.has("zIndex")) {
      circleOptions.zIndex(opts.getInt("zIndex"));
    }
    Circle circle = map.addCircle(circleOptions);
    String id = "circle_" + circle.getId();
    this.objects.put(id, circle);
    
    if (opts.has("tappable") && opts.getBoolean("tappable")) {
      this.tappables.put(id, true);
    }

    JSONObject result = new JSONObject();
    result.put("hashCode", circle.hashCode());
    result.put("id", id);
    callbackContext.success(result);
  }

  /**
   * set center
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void setCenter(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    LatLng center = new LatLng(args.getDouble(2), args.getDouble(3));
    Circle circle = this.getCircle(id);
    circle.setCenter(center);
    callbackContext.success();
  }
  
  /**
   * set fill color
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void setFillColor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    int color = PluginUtil.parsePluginColor(args.getJSONArray(2));
    this.setInt("setFillColor", id, color, callbackContext);
  }
  
  /**
   * set stroke color
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void setStrokeColor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    int color = PluginUtil.parsePluginColor(args.getJSONArray(2));
    this.setInt("setStrokeColor", id, color, callbackContext);
  }
  
  /**
   * set stroke width
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void setStrokeWidth(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    float width = (float) args.getDouble(2) * this.density;
    this.setFloat("setStrokeWidth", id, width, callbackContext);
  }
  
  /**
   * set radius
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void setRadius(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    float radius = (float) args.getDouble(2);
    this.setDouble("setRadius", id, radius, callbackContext);
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
   * Remove the circle
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void remove(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    Circle circle = this.getCircle(id);
    if (circle == null) {
      this.sendNoResult(callbackContext);
      return;
    }
    circle.remove();
    this.objects.remove(id);
    this.tappables.remove(id);
    this.sendNoResult(callbackContext);
  }
}
