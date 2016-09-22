package plugin.google.maps;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class PluginPolygon extends MyPlugin implements MyPluginInterface  {

    /**
     * Create polygon
     * @param args
     * @param callbackContext
     * @throws JSONException
     */
    @Override
    public void create(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        self = this;

        final PolygonOptions polygonOptions = new PolygonOptions();
        int color;
        final LatLngBounds.Builder builder = new LatLngBounds.Builder();
        final JSONObject properties = new JSONObject();

        JSONObject opts = args.getJSONObject(1);
        if (opts.has("points")) {
            JSONArray points = opts.getJSONArray("points");
            List<LatLng> path = PluginUtil.JSONArray2LatLngList(points);
            for (int i = 0; i < path.size(); i++) {
                polygonOptions.add(path.get(i));
                builder.include(path.get(i));
            }
        }

        if (opts.has("holes")) {
            JSONArray holes = opts.getJSONArray("holes");
            int i;
            JSONArray latLngArray;
            for (i = 0; i < holes.length(); i++) {
                latLngArray = holes.getJSONArray(i);
                polygonOptions.addHole(PluginUtil.JSONArray2LatLngList(latLngArray));
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
        if (opts.has("clickable")) {
            properties.put("isClickable", opts.getBoolean("clickable"));
        } else {
            properties.put("isClickable", true);
        }
        properties.put("isVisible", polygonOptions.isVisible());
        properties.put("zIndex", polygonOptions.getZIndex());
        properties.put("isGeodesic", polygonOptions.isGeodesic());

        // Since this plugin uses own detecting process,
        // set false to the clickable property.
        polygonOptions.clickable(false);

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Polygon polygon = map.addPolygon(polygonOptions);
                String id = "polygon_"+ polygon.getId();
                self.objects.put(id, polygon);

                String boundsId = "polygon_bounds_" + polygon.getId();
                self.objects.put(boundsId, builder.build());

                String propertyId = "polygon_property_" + polygon.getId();
                self.objects.put(propertyId, properties);

                JSONObject result = new JSONObject();
                try {
                    result.put("hashCode", polygon.hashCode());
                    result.put("id", id);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                callbackContext.success(result);
            }
        });
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Set<String> keySet = objects.keySet();
                String[] objectIdArray = keySet.toArray(new String[keySet.size()]);

                for (String objectId : objectIdArray) {
                    if (objects.containsKey(objectId)) {
                        if (objectId.startsWith("polygon_") &&
                            !objectId.contains("bounds")) {
                            Polygon polygon = (Polygon) objects.remove(objectId);
                            polygon.remove();
                        } else {
                            Object object = objects.remove(objectId);
                            object = null;
                        }
                    }
                }

                objects.clear();
                objects = null;
            }
        });

    }
    
    /**
     * set fill color
     * @param args
     * @param callbackContext
     * @throws JSONException
     */
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
     * set z-index
     * @param args
     * @param callbackContext
     * @throws JSONException
     */
    @SuppressWarnings("unused")
    public void setZIndex(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String id = args.getString(0);
        final float zIndex = (float) args.getDouble(1);
        this.setFloat("setZIndex", id, zIndex, callbackContext);
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
     * Remove the polygon
     * @param args
     * @param callbackContext
     * @throws JSONException
     */
    public void remove(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String id = args.getString(0);
        final Polygon polygon = this.getPolygon(id);
        if (polygon == null) {
            this.sendNoResult(callbackContext);
            return;
        }
        self.objects.remove(id);

        id = "polygon_bounds_" + polygon.getId();
        self.objects.remove(id);

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                polygon.remove();
                sendNoResult(callbackContext);
            }
        });
    }

    /**
     * Set holes
     * @param args
     * @param callbackContext
     * @throws JSONException
     */
    public void setHoles(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String id = args.getString(0);

        final Polygon polygon = this.getPolygon(id);

        JSONArray holesJSONArray = args.getJSONArray(1);
        final List<List<LatLng>> holes = new LinkedList<List<LatLng>>();

        for (int i = 0; i < holesJSONArray.length(); i++) {
            JSONArray holeJSONArray = holesJSONArray.getJSONArray(i);
            holes.add(PluginUtil.JSONArray2LatLngList(holeJSONArray));
        }

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                polygon.setHoles(holes);
                PluginPolygon.this.sendNoResult(callbackContext);
            }
        });

    }

    /**
     * Set points
     * @param args
     * @param callbackContext
     * @throws JSONException
     */
    public void setPoints(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String id = args.getString(0);
        final Polygon polygon = this.getPolygon(id);

        JSONArray points = args.getJSONArray(1);
        final List<LatLng> path = PluginUtil.JSONArray2LatLngList(points);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (int i = 0; i < path.size(); i++) {
            builder.include(path.get(i));
        }
        self.objects.put("polygon_bounds_" + polygon.getId(), builder.build());

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                polygon.setPoints(path);
                sendNoResult(callbackContext);
            }
        });
        this.sendNoResult(callbackContext);
    }

    /**
     * Set visibility for the object
     * @param args
     * @param callbackContext
     * @throws JSONException
     */
    public void setVisible(JSONArray args, CallbackContext callbackContext) throws JSONException {
        final boolean isVisible = args.getBoolean(1);
        String id = args.getString(0);

        final Polygon polygon = this.getPolygon(id);

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                polygon.setVisible(isVisible);
            }
        });
        String propertyId = "polygon_property_" + polygon.getId();
        JSONObject properties = (JSONObject)self.objects.get(propertyId);
        properties.put("isVisible", isVisible);
        self.objects.put(propertyId, properties);
        this.sendNoResult(callbackContext);
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
        String propertyId = id.replace("polygon_", "polygon_property_");
        JSONObject properties = (JSONObject)self.objects.get(propertyId);
        properties.put("isClickable", clickable);
        self.objects.put(propertyId, properties);
        this.sendNoResult(callbackContext);
    }
}
