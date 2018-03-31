package plugin.google.maps;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PluginCircle extends MyPlugin implements MyPluginInterface {

  /**
   * Create circle
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void create(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final CircleOptions circleOptions = new CircleOptions();
    int color;
    final JSONObject properties = new JSONObject();

    JSONObject opts = args.getJSONObject(1);
    final String hashCode = args.getString(2);
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
      circleOptions.strokeWidth((int)(opts.getDouble("strokeWidth") * density));
    }
    if (opts.has("visible")) {
      circleOptions.visible(opts.getBoolean("visible"));
    }
    if (opts.has("zIndex")) {
      circleOptions.zIndex(opts.getInt("zIndex"));
    }
    if (opts.has("clickable")) {
      properties.put("isClickable", opts.getBoolean("clickable"));
    } else {
      properties.put("isClickable", true);
    }
    properties.put("isVisible", circleOptions.isVisible());

    // Since this plugin provide own click detection,
    // disable default clickable feature.
    circleOptions.clickable(false);

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Circle circle = map.addCircle(circleOptions);
        pluginMap.objects.put("circle_" + hashCode, circle);

        pluginMap.objects.put("circle_property_" + hashCode, properties);

        // Recalculate the circle bounds
        LatLngBounds bounds = PluginUtil.getBoundsFromCircle(circleOptions.getCenter(), circleOptions.getRadius());
        pluginMap.objects.put("circle_bounds_" + hashCode, bounds);

        JSONObject result = new JSONObject();
        try {
          result.put("hashCode", hashCode);
          result.put("id", "circle_" + hashCode);
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
        // Recalculate the circle bounds
        String propertyId = "circle_bounds_" + circle.getId();
        LatLngBounds bounds = PluginUtil.getBoundsFromCircle(circle.getCenter(), circle.getRadius());
        pluginMap.objects.put(propertyId, bounds);

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
    float width = (float)(args.getDouble(1) * density);
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
    final float radius = (float) args.getDouble(1);
    final Circle circle = this.getCircle(id);

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {

        // Recalculate the circle bounds
        String propertyId = "circle_bounds_" + circle.getId();
        LatLngBounds bounds = PluginUtil.getBoundsFromCircle(circle.getCenter(), circle.getRadius());
        pluginMap.objects.put(propertyId, bounds);

        // Update the overlay
        circle.setRadius(radius);
        callbackContext.success();
      }
    });

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
    String id = args.getString(0);
    final boolean isVisible = args.getBoolean(1);

    final Circle circle = this.getCircle(id);

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        circle.setVisible(isVisible);
      }
    });
    String propertyId = "circle_property_" + circle.getId();
    JSONObject properties = (JSONObject)pluginMap.objects.get(propertyId);
    properties.put("isVisible", isVisible);
    pluginMap.objects.put(propertyId, properties);
    callbackContext.success();
  }

  /**
   * Set clickable for the object
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setClickable(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    final boolean clickable = args.getBoolean(1);
    String propertyId = id.replace("circle_", "circle_property_");
    JSONObject properties = (JSONObject)pluginMap.objects.get(propertyId);
    properties.put("isClickable", clickable);
    pluginMap.objects.put(propertyId, properties);
    callbackContext.success();
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
      callbackContext.success();
      return;
    }
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        circle.remove();
        if (pluginMap.objects != null) {
          pluginMap.objects.remove(id);
        }
        callbackContext.success();
      }
    });
  }
}
