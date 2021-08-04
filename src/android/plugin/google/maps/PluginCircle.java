package plugin.google.maps;

import com.google.android.libraries.maps.GoogleMap;
import com.google.android.libraries.maps.model.Circle;
import com.google.android.libraries.maps.model.CircleOptions;
import com.google.android.libraries.maps.model.LatLng;
import com.google.android.libraries.maps.model.LatLngBounds;
import com.google.android.libraries.maps.model.Marker;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

public class PluginCircle extends MyPlugin implements IOverlayPlugin {

  private PluginMap pluginMap;
  public final ConcurrentHashMap<String, MetaCircle> objects = new ConcurrentHashMap<String, MetaCircle>();

  /**
   * Create circle
   */
  public void create(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final CircleOptions circleOptions = new CircleOptions();
    int color;
    final JSONObject properties = new JSONObject();

    JSONObject opts = args.getJSONObject(2);
    final String hashCode = args.getString(3);
    final String circleId = "circle_" + hashCode;
    final MetaCircle meta = new MetaCircle(circleId);

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
      meta.isVisible = opts.getBoolean("visible");
      circleOptions.visible(meta.isVisible);
    }
    if (opts.has("zIndex")) {
      circleOptions.zIndex(opts.getInt("zIndex"));
    }
    if (opts.has("clickable")) {
      meta.isClickable = opts.getBoolean("clickable");
      properties.put("isClickable", meta.isClickable);
    } else {
      properties.put("isClickable", true);
    }
    properties.put("isVisible", circleOptions.isVisible());

    // Since this plugin provide own click detection,
    // disable default clickable feature.
    circleOptions.clickable(false);
    objects.put(circleId, meta);

    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Circle circle = pluginMap.getGoogleMap().addCircle(circleOptions);
        circle.setTag(circleId);

        meta.circle = circle;
        meta.properties = properties;
        meta.bounds = PluginUtil.getBoundsFromCircle(circleOptions.getCenter(), circleOptions.getRadius());

        JSONObject result = new JSONObject();
        try {
          result.put("hashCode", hashCode);
          result.put("__pgmId", circleId);
          callbackContext.success(result);
        } catch (JSONException e) {
          e.printStackTrace();
          callbackContext.error(e.getMessage() + "");
        }
      }
    });

  }

  @Override
  public void setPluginMap(PluginMap pluginMap) {
    this.pluginMap = pluginMap;
  }

  public PluginMap getMapInstance(String mapId) {
    return (PluginMap) CordovaGoogleMaps.viewPlugins.get(mapId);
  }
  public PluginCircle getInstance(String mapId) {
    PluginMap mapInstance = getMapInstance(mapId);
    return (PluginCircle) mapInstance.plugins.get(String.format("%s-circle", mapId));
  }

  /**
   * set center
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setCenter(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    String mapId = args.getString(0);
    String circleId = args.getString(1);
    JSONObject params = args.getJSONObject(2);

    PluginCircle instance = getInstance(mapId);
    MetaCircle meta = instance.objects.get(circleId);

    double lat = params.getDouble("lat");
    double lng = params.getDouble("lng");
    LatLng center = new LatLng(lat, lng);

    // Recalculate the circle bounds
    meta.bounds = PluginUtil.getBoundsFromCircle(center, meta.circle.getRadius());

    meta.circle.setCenter(center);
    callbackContext.success();
  }

  /**
   * set fill color
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setFillColor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String circleId = args.getString(1);
    PluginCircle instance = getInstance(mapId);
    MetaCircle meta = instance.objects.get(circleId);

    int color = PluginUtil.parsePluginColor(args.getJSONArray(2));
    meta.circle.setFillColor(color);
    callbackContext.success();
  }

  /**
   * set stroke color
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setStrokeColor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String circleId = args.getString(1);
    PluginCircle instance = getInstance(mapId);
    MetaCircle meta = instance.objects.get(circleId);

    int color = PluginUtil.parsePluginColor(args.getJSONArray(2));
    meta.circle.setStrokeColor(color);
    callbackContext.success();
  }

  /**
   * set stroke width
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setStrokeWidth(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String circleId = args.getString(1);
    PluginCircle instance = getInstance(mapId);
    MetaCircle meta = instance.objects.get(circleId);

    float width = (float)(args.getDouble(2) * density);
    meta.circle.setStrokeWidth(width);
    callbackContext.success();
  }

  /**
   * set radius
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setRadius(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String circleId = args.getString(1);
    PluginCircle instance = getInstance(mapId);
    MetaCircle meta = instance.objects.get(circleId);

    double radius = args.getDouble(2);
    meta.circle.setRadius(radius);

    meta.bounds = PluginUtil.getBoundsFromCircle(meta.circle.getCenter(), meta.circle.getRadius());
    callbackContext.success();
  }

  /**
   * set z-index
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setZIndex(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String circleId = args.getString(1);
    PluginCircle instance = getInstance(mapId);
    MetaCircle meta = instance.objects.get(circleId);

    float zIndex = (float)args.getDouble(2);
    meta.circle.setZIndex(zIndex);
    callbackContext.success();
  }


  /**
   * Set visibility for the object
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setVisible(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String circleId = args.getString(1);
    PluginCircle instance = getInstance(mapId);
    MetaCircle meta = instance.objects.get(circleId);

    boolean isVisible = args.getBoolean(2);
    meta.isVisible = isVisible;
    meta.circle.setVisible(isVisible);
    meta.properties.put("isVisible", isVisible);
    callbackContext.success();
  }

  /**
   * Set clickable for the object
   */
  @PgmPluginMethod
  public void setClickable(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String circleId = args.getString(1);
    PluginCircle instance = getInstance(mapId);
    MetaCircle meta = instance.objects.get(circleId);

    boolean clickable = args.getBoolean(2);
    meta.isClickable = clickable;
    meta.properties.put("isClickable", clickable);
    callbackContext.success();
  }


  /**
   * Remove the circle
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void remove(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String circleId = args.getString(1);
    PluginCircle instance = getInstance(mapId);
    MetaCircle meta = instance.objects.remove(circleId);
    meta.circle.remove();
    callbackContext.success();
  }
}
