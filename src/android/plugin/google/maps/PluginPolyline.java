package plugin.google.maps;

import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PluginPolyline extends MyPlugin implements MyPluginInterface  {
  private String polylineHashCode;

  /**
   * Create polyline
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void create(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    self = this;

    final PolylineOptions polylineOptions = new PolylineOptions();
    int color;
    final LatLngBounds.Builder builder = new LatLngBounds.Builder();
    final JSONObject properties = new JSONObject();

    JSONObject opts = args.getJSONObject(1);
    final String hashCode = args.getString(2);
    polylineHashCode = hashCode;

    if (opts.has("points")) {
      JSONArray points = opts.getJSONArray("points");
      List<LatLng> path = PluginUtil.JSONArray2LatLngList(points);
      int i = 0;
      for (i = 0; i < path.size(); i++) {
        polylineOptions.add(path.get(i));
        builder.include(path.get(i));
      }
    }
    if (opts.has("color")) {
      color = PluginUtil.parsePluginColor(opts.getJSONArray("color"));
      polylineOptions.color(color);
    }
    if (opts.has("width")) {
      polylineOptions.width((float)(opts.getDouble("width") * density));
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
    if (opts.has("clickable")) {
      properties.put("isClickable", opts.getBoolean("clickable"));
    } else {
      properties.put("isClickable", true);
    }
    properties.put("isVisible", polylineOptions.isVisible());

    // Since this plugin provide own click detection,
    // disable default clickable feature.
    polylineOptions.clickable(false);

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {

        Polyline polyline = map.addPolyline(polylineOptions);
        polyline.setTag(hashCode);
        String id = "polyline_" + hashCode;
        pluginMap.objects.put(id, polyline);

        String boundsId = "polyline_bounds_" + hashCode;
        pluginMap.objects.put(boundsId, builder.build());

        String propertyId = "polyline_property_" + hashCode;
        pluginMap.objects.put(propertyId, properties);

        try {
          JSONObject result = new JSONObject();
          result.put("hashCode", hashCode);
          result.put("__pgmId", id);
          callbackContext.success(result);
        } catch (JSONException e) {
          e.printStackTrace();
          callbackContext.error("" + e.getMessage());
        }
      }

    });
  }


  /**
   * set color
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setStrokeColor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    int color = PluginUtil.parsePluginColor(args.getJSONArray(1));
    this.setInt("setColor", id, color, callbackContext);
  }

  /**
   * set width
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setStrokeWidth(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    float width = (float)(args.getDouble(1) * density);
    this.setFloat("setWidth", id, width, callbackContext);
  }

  /**
   * set z-index
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setZIndex(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    float zIndex = (float) args.getDouble(1);
    this.setFloat("setZIndex", id, zIndex, callbackContext);
  }


  /**
   * Remove the polyline
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void remove(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    final Polyline polyline = this.getPolyline(id);
    if (polyline == null) {
      callbackContext.success();
      return;
    }
    pluginMap.objects.remove(id);

    id = "polyline_bounds_" + polylineHashCode;
    pluginMap.objects.remove(id);

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        polyline.remove();
        callbackContext.success();
      }
    });
  }
  public void setPoints(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    String id = args.getString(0);
    final JSONArray positionList = args.getJSONArray(1);

    final Polyline polyline = this.getPolyline(id);
    // Recalculate the polygon bounds
    final String propertyId = "polyline_bounds_" + polylineHashCode;

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {

        try {
          List<LatLng> path = polyline.getPoints();
          path.clear();
          JSONObject position;
          for (int i = 0; i < positionList.length(); i++) {
            position = positionList.getJSONObject(i);
            path.add(new LatLng(position.getDouble("lat"), position.getDouble("lng")));
          }
          polyline.setPoints(path);
          pluginMap.objects.put(propertyId, PluginUtil.getBoundsFromPath(path));
        } catch (JSONException e) {
          e.printStackTrace();
        }
        callbackContext.success();
      }
    });
  }
  public void removePointAt(final JSONArray args, CallbackContext callbackContext) throws JSONException {

    String id = args.getString(0);
    final int index = args.getInt(1);
    final Polyline polyline = this.getPolyline(id);

    // Recalculate the polygon bounds
    final String propertyId = "polyline_bounds_" + polylineHashCode;

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        List<LatLng> path = polyline.getPoints();
        if (path.size() > index) {
          path.remove(index);
          if (path.size() > 0) {
            pluginMap.objects.put(propertyId, PluginUtil.getBoundsFromPath(path));
          } else {
            pluginMap.objects.remove(propertyId);
          }

          polyline.setPoints(path);
        }
      }
    });
    callbackContext.success();
  }
  public void insertPointAt(final JSONArray args, CallbackContext callbackContext) throws JSONException {

    String id = args.getString(0);
    final int index = args.getInt(1);
    JSONObject position = args.getJSONObject(2);
    final LatLng latLng = new LatLng(position.getDouble("lat"), position.getDouble("lng"));

    final Polyline polyline = this.getPolyline(id);


    // Recalculate the polygon bounds
    final String propertyId = "polyline_bounds_" + polylineHashCode;

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        List<LatLng> path = polyline.getPoints();
        if (path.size() >= index) {
          path.add(index, latLng);
          polyline.setPoints(path);
          pluginMap.objects.put(propertyId, PluginUtil.getBoundsFromPath(path));
        }
      }
    });
    callbackContext.success();
  }
  public void setPointAt(final JSONArray args, CallbackContext callbackContext) throws JSONException {

    String id = args.getString(0);
    final int index = args.getInt(1);
    JSONObject position = args.getJSONObject(2);
    final LatLng latLng = new LatLng(position.getDouble("lat"), position.getDouble("lng"));


    final Polyline polyline = this.getPolyline(id);

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        List<LatLng> path = polyline.getPoints();
        if (path.size() > index) {
          path.set(index, latLng);

          // Recalculate the polygon bounds
          String propertyId = "polyline_bounds_" + polylineHashCode;
          pluginMap.objects.put(propertyId, PluginUtil.getBoundsFromPath(path));

          polyline.setPoints(path);
        }
      }
    });
    callbackContext.success();
  }

  /**
   * set geodesic
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setGeodesic(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    boolean isGeodisic = args.getBoolean(1);
    this.setBoolean("setGeodesic", id, isGeodisic, callbackContext);
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

    final Polyline polyline = this.getPolyline(id);

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        polyline.setVisible(isVisible);
      }
    });
    String propertyId = "polyline_property_" + polylineHashCode;
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
    String propertyId = id.replace("polyline_", "polyline_property_");
    JSONObject properties = (JSONObject)pluginMap.objects.get(propertyId);
    properties.put("isClickable", clickable);
    pluginMap.objects.put(propertyId, properties);
    callbackContext.success();
  }
}
