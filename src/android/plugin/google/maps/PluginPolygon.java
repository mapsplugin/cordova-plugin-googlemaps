package plugin.google.maps;

import java.util.List;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

public class PluginPolygon extends MyPlugin implements MyPluginInterface  {

    /**
     * Create polygon
     * @param args
     * @param callbackContext
     * @throws JSONException
     */
    @SuppressWarnings("unused")
    private void createPolygon(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        final PolygonOptions polygonOptions = new PolygonOptions();
        int color;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        JSONObject opts = args.getJSONObject(1);
        if (opts.has("points")) {
            JSONArray points = opts.getJSONArray("points");
            List<LatLng> path = PluginUtil.JSONArray2LatLngList(points);
            int i = 0;
            for (i = 0; i < path.size(); i++) {
                polygonOptions.add(path.get(i));
                builder.include(path.get(i));
            }
        }
        if (opts.has("strokeColor")) {
            color = PluginUtil.parsePluginColor(opts.getJSONArray("strokeColor"));
            polygonOptions.strokeColor(color);
        }
        if (opts.has("fillColor")) {
            color = PluginUtil.parsePluginColor(opts.getJSONArray("fillColor"));
            polygonOptions.fillColor(color);
        }
        if (opts.has("strokeWidth")) {
            polygonOptions.strokeWidth(opts.getInt("strokeWidth") * this.density);
        }
        if (opts.has("visible")) {
            polygonOptions.visible(opts.getBoolean("visible"));
        }
        if (opts.has("geodesic")) {
            polygonOptions.geodesic(opts.getBoolean("geodesic"));
        }
        if (opts.has("zIndex")) {
            polygonOptions.zIndex(opts.getInt("zIndex"));
        }
        if (opts.has("addHole")) {
            JSONArray points = opts.getJSONArray("addHole");
            List<LatLng> path = PluginUtil.JSONArray2LatLngList(points);
            if(path.size() > 0) {
                polygonOptions.addHole(path);
            }
        }

        Polygon polygon = map.addPolygon(polygonOptions);
        String id = "polygon_"+ polygon.getId();
        this.objects.put(id, polygon);

        String boundsId = "polygon_bounds_" + polygon.getId();
        this.objects.put(boundsId, builder.build());

        JSONObject result = new JSONObject();
        result.put("hashCode", polygon.hashCode());
        result.put("id", id);
        callbackContext.success(result);
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
     * Remove the polygon
     * @param args
     * @param callbackContext
     * @throws JSONException
     */
    @SuppressWarnings("unused")
    private void remove(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String id = args.getString(1);
        Polygon polygon = this.getPolygon(id);
        if (polygon == null) {
            this.sendNoResult(callbackContext);
            return;
        }
        this.objects.remove(id);

        id = "polygon_bounds_" + polygon.getId();
        this.objects.remove(id);

        polygon.remove();
        this.sendNoResult(callbackContext);
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
        Polygon polygon = this.getPolygon(id);

        JSONArray points = args.getJSONArray(2);
        List<LatLng> path = PluginUtil.JSONArray2LatLngList(points);
        polygon.setPoints(path);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (int i = 0; i < path.size(); i++) {
            builder.include(path.get(i));
        }
        this.objects.put("polygon_bounds_" + polygon.getId(), builder.build());
        this.sendNoResult(callbackContext);
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
