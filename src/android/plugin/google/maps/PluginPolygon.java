package plugin.google.maps;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

//Future implement
//TODO: https://codepen.io/jhawes/pen/ujdgK


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
        final ArrayList<LatLngBounds> holeBounds = new ArrayList<LatLngBounds>();
        final ArrayList<LatLng> path = new ArrayList<LatLng>();
        final ArrayList<ArrayList<LatLng>> holePaths = new ArrayList<ArrayList<LatLng>>();

        JSONObject opts = args.getJSONObject(1);
        if (opts.has("points")) {
            JSONArray points = opts.getJSONArray("points");
            ArrayList<LatLng> path2 = PluginUtil.JSONArray2LatLngList(points);
            for (int i = 0; i < path2.size(); i++) {
                polygonOptions.add(path2.get(i));
                path.add(path2.get(i));
                builder.include(path2.get(i));
            }
        }

        if (opts.has("holes")) {
            JSONArray holes = opts.getJSONArray("holes");
            int i;
            JSONArray latLngArray;
            ArrayList<LatLng> hole;
            Iterator<LatLng> iterator;
            LatLng latLng;
            LatLngBounds.Builder builder2;
            for (i = 0; i < holes.length(); i++) {
                latLngArray = holes.getJSONArray(i);
                hole = PluginUtil.JSONArray2LatLngList(latLngArray);
                polygonOptions.addHole(hole);
                holePaths.add(hole);
                iterator = hole.iterator();
                builder2 = new LatLngBounds.Builder();
                while (iterator.hasNext()) {
                    latLng = iterator.next();
                    builder2.include(latLng);
                }
                holeBounds.add(builder2.build());
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
                String id = polygon.getId();
                self.objects.put("polygon_"+ id, polygon);
                self.objects.put("polygon_bounds_" + id, builder.build());
                self.objects.put("polygon_path_" + id, path);
                self.objects.put("polygon_holePaths_" + id, holePaths);
                self.objects.put("polygon_property_" + id, properties);

                JSONObject result = new JSONObject();
                try {
                    result.put("hashCode", polygon.hashCode());
                    result.put("id", "polygon_"+ id);
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
                if (objects == null) {
                    return;
                }
                Set<String> keySet = objects.keySet();
                String[] objectIdArray = keySet.toArray(new String[keySet.size()]);

                for (String objectId : objectIdArray) {
                    if (objects.containsKey(objectId)) {
                        if (objectId.contains("property")) {
                            Polygon polygon = (Polygon) objects.remove(objectId.replace("property_", ""));
                            if (polygon != null) {
                                polygon.remove();
                            }
                        }
                        Object object = objects.remove(objectId);
                        object = null;

                    }
                }

                objects.clear();
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

        id = polygon.getId();
        self.objects.remove("polygon_bounds_" + id);
        self.objects.remove("polygon_property_" + id);
        self.objects.remove("polygon_path_" + id);
        self.objects.remove("polygon_holePaths_" + id);

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                polygon.remove();
                sendNoResult(callbackContext);
            }
        });
    }

    /**
     * Set points
     * @param args
     * @param callbackContext
     * @throws JSONException
     */
    public void removePointAt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String id = args.getString(0);
        final int index = args.getInt(1);
        final Polygon polygon = this.getPolygon(id);


        //------------------------
        // Update the hole list
        //------------------------
        String propertyId = "polygon_path_" + polygon.getId();
        final ArrayList<LatLng> path = (ArrayList<LatLng>)self.objects.get(propertyId);
        if (path.size() > 0) {
            path.remove(index);
        }
        self.objects.put(propertyId, path);

        //-----------------------------------
        // Recalculate the polygon bounds
        //-----------------------------------
        propertyId = "polygon_bounds_" + polygon.getId();
        if (path.size() > 0) {
            self.objects.put(propertyId, PluginUtil.getBoundsFromPath(path));
        } else {
            self.objects.remove(propertyId);
        }

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (path.size() > 0) {
                    try {
                        polygon.setPoints(path);
                    } catch (Exception e) {
                        // Ignore this error
                        //e.printStackTrace();
                    }
                } else {
                    polygon.setVisible(false);
                }
                sendNoResult(callbackContext);
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
        final JSONArray positionList = args.getJSONArray(1);


        final Polygon polygon = this.getPolygon(id);

        //------------------------
        // Update the hole list
        //------------------------
        String propertyId = "polygon_path_" + polygon.getId();
        final ArrayList<LatLng> path = (ArrayList<LatLng>)self.objects.get(propertyId);
        path.clear();
        JSONObject position;
        for (int i = 0; i < positionList.length(); i++) {
            position = positionList.getJSONObject(i);
            path.add(new LatLng(position.getDouble("lat"), position.getDouble("lng")));
        }
        self.objects.put(propertyId, path);

        //-----------------------------------
        // Recalculate the polygon bounds
        //-----------------------------------
        propertyId = "polygon_bounds_" + polygon.getId();
        self.objects.put(propertyId, PluginUtil.getBoundsFromPath(path));

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update the polygon
                polygon.setPoints(path);
                if (path.size() > 0) {
                    polygon.setVisible(true);
                } else {
                    polygon.setVisible(false);
                }
                sendNoResult(callbackContext);
            }
        });
    }

    /**
     * Insert a point
     * @param args
     * @param callbackContext
     * @throws JSONException
     */
    public void insertPointAt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String id = args.getString(0);
        final int index = args.getInt(1);
        JSONObject position = args.getJSONObject(2);
        final LatLng latLng = new LatLng(position.getDouble("lat"), position.getDouble("lng"));

        final Polygon polygon = this.getPolygon(id);

        //------------------------
        // Update the hole list
        //------------------------
        boolean shouldBeVisible = false;
        String propertyId = "polygon_path_" + polygon.getId();
        final ArrayList<LatLng> path = (ArrayList<LatLng>)self.objects.get(propertyId);
        if (path.size() == 0) {
            JSONObject properties = (JSONObject)self.objects.get("polygon_property_" + polygon.getId());
            if (properties.getBoolean("isVisible")) {
                shouldBeVisible = true;
            }
        }
        path.add(index, latLng);
        self.objects.put(propertyId, path);

        //-----------------------------------
        // Recalculate the polygon bounds
        //-----------------------------------
        propertyId = "polygon_bounds_" + polygon.getId();
        self.objects.put(propertyId, PluginUtil.getBoundsFromPath(path));

        final boolean changeToVisible = shouldBeVisible;
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update the polygon
                polygon.setPoints(path);
                if (changeToVisible) {
                    polygon.setVisible(true);
                }
                sendNoResult(callbackContext);
            }
        });
    }

    /**
     * Set points
     * @param args
     * @param callbackContext
     * @throws JSONException
     */
    public void setPointAt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String id = args.getString(0);
        final int index = args.getInt(1);
        JSONObject position = args.getJSONObject(2);
        final LatLng latLng = new LatLng(position.getDouble("lat"), position.getDouble("lng"));

        final Polygon polygon = this.getPolygon(id);

        //------------------------
        // Update the hole list
        //------------------------
        String propertyId = "polygon_path_" + polygon.getId();
        final ArrayList<LatLng> path = (ArrayList<LatLng>)self.objects.get(propertyId);
        path.set(index, latLng);
        self.objects.put(propertyId, path);

        //-----------------------------------
        // Recalculate the polygon bounds
        //-----------------------------------
        propertyId = "polygon_bounds_" + polygon.getId();
        self.objects.put(propertyId, PluginUtil.getBoundsFromPath(path));

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update the polygon
                polygon.setPoints(path);
                sendNoResult(callbackContext);
            }
        });
    }

    /**
     * Set points
     * @param args
     * @param callbackContext
     * @throws JSONException
     */
    public void setHoles(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String id = args.getString(0);
        final JSONArray holeList = args.getJSONArray(1);
        final Polygon polygon = this.getPolygon(id);

        //------------------------
        // Update the hole list
        //------------------------
        String propertyId = "polygon_holePaths_" + polygon.getId();
        final ArrayList<ArrayList<LatLng>> holes = (ArrayList<ArrayList<LatLng>>) self.objects.get(propertyId);
        for (int i = 0; i < holes.size(); i++) {
            holes.get(i).clear();
        }
        holes.clear();

        JSONObject position;
        for (int i = 0; i < holeList.length(); i++) {
            ArrayList<LatLng> hole = new ArrayList<LatLng>();
            JSONArray holePositions = holeList.getJSONArray(i);
            for (int j = 0; j < holePositions.length(); j++) {
                position = holePositions.getJSONObject(j);
                hole.add(new LatLng(position.getDouble("lat"), position.getDouble("lng")));
            }
            holes.add(hole);
        }


        self.objects.put(propertyId, holes);

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update the polygon
                polygon.setHoles(holes);
                sendNoResult(callbackContext);
            }
        });
    }
    /**
     * Set points
     * @param args
     * @param callbackContext
     * @throws JSONException
     */
    public void insertPointOfHoleAt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String id = args.getString(0);
        final int holeIndex = args.getInt(1);
        final int pointIndex = args.getInt(2);
        JSONObject position = args.getJSONObject(3);
        final LatLng latLng = new LatLng(position.getDouble("lat"), position.getDouble("lng"));

        final Polygon polygon = this.getPolygon(id);

        //------------------------
        // Update the hole list
        //------------------------
        String propertyId = "polygon_holePaths_" + polygon.getId();
        final ArrayList<ArrayList<LatLng>> holes = (ArrayList<ArrayList<LatLng>>) self.objects.get(propertyId);
        ArrayList<LatLng> hole = null;
        if (holeIndex < holes.size()) {
            hole = holes.get(holeIndex);
        }
        if (hole == null) {
            hole = new ArrayList<LatLng>();
        }
        if (holes.size() == 0) {
            holes.add(hole);
        }
        hole.add(pointIndex, latLng);
        self.objects.put(propertyId, holes);

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update the polygon
                polygon.setHoles(holes);
                sendNoResult(callbackContext);
            }
        });
    }

    /**
     * Set points
     * @param args
     * @param callbackContext
     * @throws JSONException
     */
    public void setPointOfHoleAt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String id = args.getString(0);
        final int holeIndex = args.getInt(1);
        final int pointIndex = args.getInt(2);
        JSONObject position = args.getJSONObject(3);
        final LatLng latLng = new LatLng(position.getDouble("lat"), position.getDouble("lng"));

        final Polygon polygon = this.getPolygon(id);

        //------------------------
        // Update the hole list
        //------------------------
        String propertyId = "polygon_holePaths_" + polygon.getId();
        final ArrayList<ArrayList<LatLng>> holes = (ArrayList<ArrayList<LatLng>>) self.objects.get(propertyId);
        ArrayList<LatLng> hole = null;
        if (holeIndex < holes.size()) {
            hole = holes.get(holeIndex);
        }
        if (hole == null) {
            hole = new ArrayList<LatLng>();
        }
        if (holes.size() == 0) {
            holes.add(hole);
        }
        hole.set(pointIndex, latLng);
        self.objects.put(propertyId, holes);

        final ArrayList<LatLng> newHole = hole;


        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update the polygon
                polygon.setHoles(holes);
                sendNoResult(callbackContext);
            }
        });
    }

    /**
     * Set points
     * @param args
     * @param callbackContext
     * @throws JSONException
     */
    public void removePointOfHoleAt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String id = args.getString(0);
        final int holeIndex = args.getInt(1);
        final int pointIndex = args.getInt(2);

        final Polygon polygon = this.getPolygon(id);

        //------------------------
        // Update the hole list
        //------------------------
        String propertyId = "polygon_holePaths_" + polygon.getId();
        final ArrayList<ArrayList<LatLng>> holes = (ArrayList<ArrayList<LatLng>>) self.objects.get(propertyId);
        ArrayList<LatLng> hole = null;
        if (holeIndex < holes.size()) {
            hole = holes.get(holeIndex);
        }
        if (hole == null) {
            hole = new ArrayList<LatLng>();
        }
        if (holes.size() == 0) {
            holes.add(hole);
        }
        hole.remove(pointIndex);
        self.objects.put(propertyId, holes);

        final ArrayList<LatLng> newHole = hole;

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update the polygon
                polygon.setHoles(holes);
                sendNoResult(callbackContext);
            }
        });
    }


    public void insertHoleAt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String id = args.getString(0);
        final int holeIndex = args.getInt(1);

        JSONArray holeJson = args.getJSONArray(2);
        final ArrayList<LatLng> newHole = PluginUtil.JSONArray2LatLngList(holeJson);

        final Polygon polygon = this.getPolygon(id);

        //------------------------
        // Update the hole list
        //------------------------
        String propertyId = "polygon_holePaths_" + polygon.getId();
        final ArrayList<ArrayList<LatLng>> holes = (ArrayList<ArrayList<LatLng>>) self.objects.get(propertyId);
        holes.add(holeIndex, newHole);
        self.objects.put(propertyId, holes);

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update the polygon
                try {
                    polygon.setHoles(holes);
                } catch (Exception e) {
                    // Ignore this error
                    //e.printStackTrace();
                }
                sendNoResult(callbackContext);
            }
        });
    }

    /**
     * Set points
     * @param args
     * @param callbackContext
     * @throws JSONException
     */
    public void setHoleAt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String id = args.getString(0);
        final int holeIndex = args.getInt(1);

        JSONArray holeJson = args.getJSONArray(2);
        final ArrayList<LatLng> newHole = PluginUtil.JSONArray2LatLngList(holeJson);

        final Polygon polygon = this.getPolygon(id);

        //------------------------
        // Update the hole list
        //------------------------
        String propertyId = "polygon_holePaths_" + polygon.getId();
        final ArrayList<ArrayList<LatLng>> holes = (ArrayList<ArrayList<LatLng>>) self.objects.get(propertyId);
        holes.set(holeIndex, newHole);
        self.objects.put(propertyId, holes);

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update the polygon
                polygon.setHoles(holes);
                sendNoResult(callbackContext);
            }
        });
    }


    /**
     * Set points
     * @param args
     * @param callbackContext
     * @throws JSONException
     */
    public void removeHoleAt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String id = args.getString(0);
        final int holeIndex = args.getInt(1);

        final Polygon polygon = this.getPolygon(id);

        //------------------------
        // Update the hole list
        //------------------------
        String propertyId = "polygon_holePaths_" + polygon.getId();
        final ArrayList<ArrayList<LatLng>> holes = (ArrayList<ArrayList<LatLng>>) self.objects.get(propertyId);
        holes.remove(holeIndex);
        self.objects.put(propertyId, holes);

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update the polygon
                polygon.setHoles(holes);
                sendNoResult(callbackContext);
            }
        });
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
