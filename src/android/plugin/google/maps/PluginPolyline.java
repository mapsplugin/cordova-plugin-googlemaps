package plugin.google.maps;

import com.google.android.libraries.maps.GoogleMap;
import com.google.android.libraries.maps.model.LatLng;
import com.google.android.libraries.maps.model.LatLngBounds;
import com.google.android.libraries.maps.model.Polyline;
import com.google.android.libraries.maps.model.PolylineOptions;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PluginPolyline extends MyPlugin implements IOverlayPlugin {

  private PluginMap pluginMap;
  public final ConcurrentHashMap<String, MetaPolyline> objects = new ConcurrentHashMap<String, MetaPolyline>();

  public PluginMap getMapInstance(String mapId) {
    return (PluginMap) CordovaGoogleMaps.viewPlugins.get(mapId);
  }
  public PluginPolyline getInstance(String mapId) {
    PluginMap mapInstance = getMapInstance(mapId);
    return (PluginPolyline) mapInstance.plugins.get(String.format("%s-polyline", mapId));
  }
  @Override
  public void setPluginMap(PluginMap pluginMap) {
    this.pluginMap = pluginMap;
  }


  /**
   * Create polyline
   */
  public void create(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    final PolylineOptions polylineOptions = new PolylineOptions();
    int color;
    final LatLngBounds.Builder builder = new LatLngBounds.Builder();
    final JSONObject properties = new JSONObject();

    JSONObject opts = args.getJSONObject(2);
    final String hashCode = args.getString(3);

    final String polylineId = "polyline_" + hashCode;
    final MetaPolyline meta = new MetaPolyline(polylineId);

    int pointCnt = 0;
    if (opts.has("points")) {
      JSONArray points = opts.getJSONArray("points");
      List<LatLng> path = PluginUtil.JSONArray2LatLngList(points);
      for (int i = 0; i < path.size(); i++) {
        polylineOptions.add(path.get(i));
        builder.include(path.get(i));
      }
      pointCnt = path.size();
    }
    final int finalPointCnt = pointCnt;
    if (opts.has("color")) {
      color = PluginUtil.parsePluginColor(opts.getJSONArray("color"));
      polylineOptions.color(color);
    }
    if (opts.has("width")) {
      polylineOptions.width((float)(opts.getDouble("width") * density));
    }
    if (opts.has("visible")) {
      meta.isVisible = opts.getBoolean("visible");
      polylineOptions.visible(meta.isVisible);
    }
    if (opts.has("geodesic")) {
      polylineOptions.geodesic(opts.getBoolean("geodesic"));
    }
    if (opts.has("zIndex")) {
      polylineOptions.zIndex(opts.getInt("zIndex"));
    }
    if (opts.has("clickable")) {
      meta.isClickable = opts.getBoolean("clickable");
      properties.put("isClickable", meta.isClickable);
    } else {
      meta.isClickable = true;
      properties.put("isClickable", true);
    }
    properties.put("isVisible", meta.isVisible);

    // Since this plugin provide own click detection,
    // disable default clickable feature.
    polylineOptions.clickable(false);

    meta.properties = properties;
    objects.put(polylineId, meta);

    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {

        Polyline polyline = pluginMap.getGoogleMap().addPolyline(polylineOptions);
        polyline.setTag(polylineId);
        meta.polyline = polyline;

        if (finalPointCnt > 0) {
          meta.bounds = builder.build();
        } else {
          meta.bounds = new LatLngBounds(new LatLng(360, 360), new LatLng(360, 360));
        }
      }
    });

    try {
      JSONObject result = new JSONObject();
      result.put("hashCode", hashCode);
      result.put("__pgmId", polylineId);
      callbackContext.success(result);
    } catch (JSONException e) {
      e.printStackTrace();
      callbackContext.error("" + e.getMessage());
    }
  }


  @Override
  public void onDestroy() {
    super.onDestroy();
    objects.clear();
  }

  /**
   * set color
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setStrokeColor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String polygonId = args.getString(1);
    PluginPolyline instance = getInstance(mapId);
    MetaPolyline meta = instance.objects.get(polygonId);

    int color = PluginUtil.parsePluginColor(args.getJSONArray(2));
    meta.polyline.setColor(color);;
    callbackContext.success();
  }

  /**
   * set width
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setStrokeWidth(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String polygonId = args.getString(1);
    PluginPolyline instance = getInstance(mapId);
    MetaPolyline meta = instance.objects.get(polygonId);

    float width = (float)(args.getDouble(2) * density);
    meta.polyline.setWidth(width);
    callbackContext.success();
  }

  /**
   * set z-index
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setZIndex(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String polygonId = args.getString(1);
    PluginPolyline instance = getInstance(mapId);
    MetaPolyline meta = instance.objects.get(polygonId);

    float zIndex = (float) args.getDouble(2);
    meta.polyline.setZIndex(zIndex);
    callbackContext.success();
  }


  /**
   * Remove the polyline
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void remove(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String polygonId = args.getString(1);
    PluginPolyline instance = getInstance(mapId);

    MetaPolyline meta = instance.objects.remove(polygonId);
    meta.polyline.remove();
    callbackContext.success();
  }

  @PgmPluginMethod(runOnUiThread = true)
  public void setPoints(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String polygonId = args.getString(1);
    PluginPolyline instance = getInstance(mapId);
    MetaPolyline meta = instance.objects.get(polygonId);

    JSONArray positionList = args.getJSONArray(2);

    List<LatLng> path = meta.polyline.getPoints();
    path.clear();
    JSONObject position;
    for (int i = 0; i < positionList.length(); i++) {
      position = positionList.getJSONObject(i);
      path.add(new LatLng(position.getDouble("lat"), position.getDouble("lng")));
    }
    meta.polyline.setPoints(path);
    meta.bounds = PluginUtil.getBoundsFromPath(path);
    callbackContext.success();
  }

  @PgmPluginMethod(runOnUiThread = true)
  public void removePointAt(final JSONArray args, CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String polygonId = args.getString(1);
    PluginPolyline instance = getInstance(mapId);
    MetaPolyline meta = instance.objects.get(polygonId);

    int index = args.getInt(2);

    List<LatLng> path = meta.polyline.getPoints();
    if (path.size() > index) {
      path.remove(index);
      if (path.size() > 0) {
        meta.bounds = PluginUtil.getBoundsFromPath(path);
      } else {
        meta.bounds = new LatLngBounds(new LatLng(360, 360), new LatLng(360, 360));
      }

      meta.polyline.setPoints(path);
    }
    callbackContext.success();
  }

  @PgmPluginMethod(runOnUiThread = true)
  public void insertPointAt(final JSONArray args, CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String polygonId = args.getString(1);
    PluginPolyline instance = getInstance(mapId);
    MetaPolyline meta = instance.objects.get(polygonId);

    int index = args.getInt(2);
    JSONObject position = args.getJSONObject(3);
    final LatLng latLng = new LatLng(position.getDouble("lat"), position.getDouble("lng"));

    List<LatLng> path = meta.polyline.getPoints();
    if (path.size() >= index) {
      path.add(index, latLng);
      meta.polyline.setPoints(path);
      meta.bounds = PluginUtil.getBoundsFromPath(path);
    }
    callbackContext.success();
  }

  @PgmPluginMethod(runOnUiThread = true)
  public void setPointAt(final JSONArray args, CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String polygonId = args.getString(1);
    PluginPolyline instance = getInstance(mapId);
    MetaPolyline meta = instance.objects.get(polygonId);

    final int index = args.getInt(2);
    JSONObject position = args.getJSONObject(3);
    final LatLng latLng = new LatLng(position.getDouble("lat"), position.getDouble("lng"));


    List<LatLng> path = meta.polyline.getPoints();
    if (path.size() > index) {
      path.set(index, latLng);

      // Recalculate the polygon bounds
      meta.bounds = PluginUtil.getBoundsFromPath(path);

      meta.polyline.setPoints(path);
    }
    callbackContext.success();
  }

  /**
   * set geodesic
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setGeodesic(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String polygonId = args.getString(1);
    PluginPolyline instance = getInstance(mapId);
    MetaPolyline meta = instance.objects.get(polygonId);

    boolean isGeodisic = args.getBoolean(2);
    meta.polyline.setGeodesic(isGeodisic);
    callbackContext.success();
  }

  /**
   * Set visibility for the object
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setVisible(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String polygonId = args.getString(1);
    PluginPolyline instance = getInstance(mapId);
    MetaPolyline meta = instance.objects.get(polygonId);

    boolean isVisible = args.getBoolean(2);
    meta.polyline.setVisible(isVisible);
    meta.isVisible = isVisible;
    meta.properties.put("isVisible", isVisible);
    callbackContext.success();
  }

  /**
   * Set clickable for the object
   */
  @PgmPluginMethod
  public void setClickable(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String polygonId = args.getString(1);
    PluginPolyline instance = getInstance(mapId);
    MetaPolyline meta = instance.objects.get(polygonId);

    boolean clickable = args.getBoolean(2);
    meta.isClickable = clickable;
    meta.properties.put("isClickable", clickable);
    callbackContext.success();
  }
}
