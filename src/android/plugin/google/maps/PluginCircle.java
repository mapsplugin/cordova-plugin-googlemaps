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
  public void create(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final CircleOptions circleOptions = new CircleOptions();
    int color;
    
    JSONObject opts = args.getJSONObject(0);
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

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Circle circle = map.addCircle(circleOptions);
        String id = "circle_" + circle.getId();
        self.objects.put(id, circle);

        JSONObject result = new JSONObject();
        try {
          result.put("hashCode", circle.hashCode());
          result.put("id", id);
          callbackContext.success(result);
        } catch (JSONException e) {
          e.printStackTrace();
          callbackContext.error(e.getMessage() + "");
        }
      }
    });

  }

  /**
   * set center
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  public void setCenter(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    final LatLng center = new LatLng(args.getDouble(1), args.getDouble(2));
    final Circle circle = this.getCircle(id);

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        circle.setCenter(center);
        callbackContext.success();
      }
    });
  }
  
  /**
   * set fill color
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  public void setFillColor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    int color = PluginUtil.parsePluginColor(args.getJSONArray(1));
    this.setInt("setFillColor", id, color, callbackContext);
  }
  
  /**
   * set stroke color
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  public void setStrokeColor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    int color = PluginUtil.parsePluginColor(args.getJSONArray(1));
    this.setInt("setStrokeColor", id, color, callbackContext);
  }
  
  /**
   * set stroke width
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  public void setStrokeWidth(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    float width = (float) args.getDouble(1) * this.density;
    this.setFloat("setStrokeWidth", id, width, callbackContext);
  }
  
  /**
   * set radius
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  public void setRadius(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    float radius = (float) args.getDouble(1);
    this.setDouble("setRadius", id, radius, callbackContext);
  }
  
  /**
   * set z-index
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  public void setZIndex(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    float zIndex = (float) args.getDouble(1);
    this.setFloat("setZIndex", id, zIndex, callbackContext);
  }
  

  /**
   * Set visibility for the object
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void setVisible(JSONArray args, CallbackContext callbackContext) throws JSONException {
    boolean visible = args.getBoolean(1);
    String id = args.getString(0);
    this.setBoolean("setVisible", id, visible, callbackContext);
  }
  
  /**
   * Remove the circle
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void remove(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final String id = args.getString(0);
    final Circle circle = this.getCircle(id);
    if (circle == null) {
      this.sendNoResult(callbackContext);
      return;
    }
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        circle.remove();
        objects.remove(id);
        sendNoResult(callbackContext);
      }
    });
  }
}
