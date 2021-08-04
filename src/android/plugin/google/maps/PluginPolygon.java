package plugin.google.maps;

import com.google.android.libraries.maps.GoogleMap;
import com.google.android.libraries.maps.model.Circle;
import com.google.android.libraries.maps.model.LatLng;
import com.google.android.libraries.maps.model.LatLngBounds;
import com.google.android.libraries.maps.model.Polygon;
import com.google.android.libraries.maps.model.PolygonOptions;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//Future implement
//TODO: https://codepen.io/jhawes/pen/ujdgK


public class PluginPolygon extends MyPlugin implements IOverlayPlugin {

    private PluginMap pluginMap;
    public final ConcurrentHashMap<String, MetaPolygon> objects = new ConcurrentHashMap<String, MetaPolygon>();


    public PluginMap getMapInstance(String mapId) {
        return (PluginMap) CordovaGoogleMaps.viewPlugins.get(mapId);
    }
    public PluginPolygon getInstance(String mapId) {
        PluginMap mapInstance = getMapInstance(mapId);
        return (PluginPolygon) mapInstance.plugins.get(String.format("%s-polygon", mapId));
    }

    @Override
    public void setPluginMap(PluginMap pluginMap) {
        this.pluginMap = pluginMap;
    }

    /**
     * Create polygon
     */
    public void create(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

        final PolygonOptions polygonOptions = new PolygonOptions();
        int color;
        final LatLngBounds.Builder builder = new LatLngBounds.Builder();
        final JSONObject properties = new JSONObject();
        final ArrayList<LatLngBounds> holeBounds = new ArrayList<LatLngBounds>();
        final ArrayList<LatLng> path = new ArrayList<LatLng>();
        final ArrayList<ArrayList<LatLng>> holePaths = new ArrayList<ArrayList<LatLng>>();


        JSONObject opts = args.getJSONObject(2);
        final String hashCode = args.getString(3);

        final String polygonId = String.format("polygon_%s", hashCode);
        MetaPolygon meta = new MetaPolygon(polygonId);

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
            polygonOptions.strokeWidth((float)(opts.getDouble("strokeWidth") * density));
        }
        if (opts.has("visible")) {
            meta.isVisible = opts.getBoolean("visible");
            polygonOptions.visible(meta.isVisible);
        }
        if (opts.has("geodesic")) {
            polygonOptions.geodesic(opts.getBoolean("geodesic"));
        }
        if (opts.has("zIndex")) {
            polygonOptions.zIndex(opts.getInt("zIndex"));
        }
        if (opts.has("clickable")) {
            meta.isClickable = opts.getBoolean("clickable");
            properties.put("isClickable", meta.isClickable);
        } else {
            properties.put("isClickable", true);
            meta.isClickable = true;
        }
        properties.put("isVisible", polygonOptions.isVisible());
        properties.put("zIndex", polygonOptions.getZIndex());
        properties.put("isGeodesic", polygonOptions.isGeodesic());

        // Since this plugin uses own detecting process,
        // set false to the clickable property.
        polygonOptions.clickable(false);

        objects.put(polygonId, meta);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                meta.polygon = pluginMap.getGoogleMap().addPolygon(polygonOptions);
                meta.polygon.setTag(polygonId);
                meta.bounds = builder.build();
                meta.path = path;
                meta.holePaths = holePaths;
                meta.properties = properties;
            }
        });

        JSONObject result = new JSONObject();
        try {
            result.put("hashCode", hashCode);
            result.put("__pgmId", polygonId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        callbackContext.success(result);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        objects.clear();
    }

    /**
     * set fill color
     */
    @PgmPluginMethod(runOnUiThread = true)
    public void setFillColor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String mapId = args.getString(0);
        String polygonId = args.getString(1);
        PluginPolygon instance = getInstance(mapId);
        int color = PluginUtil.parsePluginColor(args.getJSONArray(2));
        Polygon polygon = instance.objects.get(polygonId).polygon;
        polygon.setFillColor(color);
        callbackContext.success();
    }

    /**
     * set stroke color
     */
    @PgmPluginMethod(runOnUiThread = true)
    public void setStrokeColor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String mapId = args.getString(0);
        String polygonId = args.getString(1);
        PluginPolygon instance = getInstance(mapId);
        int color = PluginUtil.parsePluginColor(args.getJSONArray(2));
        Polygon polygon = instance.objects.get(polygonId).polygon;
        polygon.setStrokeColor(color);
        callbackContext.success();
    }

    /**
     * set stroke width
     */
    @PgmPluginMethod(runOnUiThread = true)
    public void setStrokeWidth(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String mapId = args.getString(0);
        String polygonId = args.getString(1);
        PluginPolygon instance = getInstance(mapId);
        float width = (float)(args.getDouble(2) * density);
        Polygon polygon = instance.objects.get(polygonId).polygon;
        polygon.setStrokeWidth(width);
        callbackContext.success();
    }

    /**
     * set z-index
     */
    @PgmPluginMethod(runOnUiThread = true)
    public void setZIndex(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String mapId = args.getString(0);
        String polygonId = args.getString(1);
        PluginPolygon instance = getInstance(mapId);
        float zIndex = (float) args.getDouble(2);
        Polygon polygon = instance.objects.get(polygonId).polygon;
        polygon.setZIndex(zIndex);
        callbackContext.success();
    }

    /**
     * set geodesic
     */
    @PgmPluginMethod(runOnUiThread = true)
    public void setGeodesic(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String mapId = args.getString(0);
        String polygonId = args.getString(1);
        PluginPolygon instance = getInstance(mapId);
        boolean isGeodisic = args.getBoolean(2);
        Polygon polygon = instance.objects.get(polygonId).polygon;
        polygon.setGeodesic(isGeodisic);
        callbackContext.success();
    }

    /**
     * Remove the polygon
     */
    @PgmPluginMethod(runOnUiThread = true)
    public void remove(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String mapId = args.getString(0);
        String polygonId = args.getString(1);
        PluginPolygon instance = getInstance(mapId);
        MetaPolygon meta = instance.objects.remove(polygonId);
        if (meta == null) {
            callbackContext.success();
            return;
        }
        Polygon polygon = meta.polygon;
        polygon.remove();
        callbackContext.success();
    }

    /**
     * Set points
     */
    @PgmPluginMethod(runOnUiThread = true)
    public void removePointAt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String mapId = args.getString(0);
        String polygonId = args.getString(1);
        int index = args.getInt(2);
        PluginPolygon instance = getInstance(mapId);
        MetaPolygon meta = instance.objects.get(polygonId);
        Polygon polygon = meta.polygon;


        //------------------------
        // Update the hole list
        //------------------------
        String propertyId = "polygon_path_" + polygonId;
        final ArrayList<LatLng> path = meta.path;
        if (path.size() > 0) {
            path.remove(index);
        }

        //-----------------------------------
        // Recalculate the polygon bounds
        //-----------------------------------
        propertyId = "polygon_bounds_" + polygonId;
        if (path.size() > 0) {
            meta.bounds = PluginUtil.getBoundsFromPath(path);
        } else {
            meta.bounds = null;
        }

        if (path.size() > 0) {
            try {
                polygon.setPoints(path);
            } catch (Exception e) {
                // Ignore this error
                //e.printStackTrace();
            }
        } else {
            meta.isVisible = false;
            polygon.setVisible(false);
        }
        callbackContext.success();
    }

    /**
     * Set points
     */
    @PgmPluginMethod(runOnUiThread = true)
    public void setPoints(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String mapId = args.getString(0);
        String polygonId = args.getString(1);
        PluginPolygon instance = getInstance(mapId);
        MetaPolygon meta = instance.objects.get(polygonId);
        Polygon polygon = meta.polygon;

        JSONArray positionList = args.getJSONArray(2);

        //------------------------
        // Update the hole list
        //------------------------
        String propertyId = "polygon_path_" + polygonId;
        meta.path.clear();
        JSONObject position;
        for (int i = 0; i < positionList.length(); i++) {
            position = positionList.getJSONObject(i);
            meta.path.add(new LatLng(position.getDouble("lat"), position.getDouble("lng")));
        }

        //-----------------------------------
        // Recalculate the polygon bounds
        //-----------------------------------
        meta.bounds = PluginUtil.getBoundsFromPath(meta.path);

        // Update the polygon
        polygon.setPoints(meta.path);
        if (meta.path.size() > 0) {
            meta.isVisible = polygon.isVisible();
            polygon.setVisible(meta.isVisible);
        } else {meta.isVisible = false;
            polygon.setVisible(false);
        }
        callbackContext.success();
    }

    /**
     * Insert a point
     */
    @PgmPluginMethod(runOnUiThread = true)
    public void insertPointAt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String mapId = args.getString(0);
        String polygonId = args.getString(1);
        PluginPolygon instance = getInstance(mapId);
        MetaPolygon meta = instance.objects.get(polygonId);

        int index = args.getInt(2);
        JSONObject position = args.getJSONObject(3);
        final LatLng latLng = new LatLng(position.getDouble("lat"), position.getDouble("lng"));

        //------------------------
        // Update the hole list
        //------------------------
        boolean shouldBeVisible = false;
        if (meta.path.size() == 0) {
            if (meta.properties.getBoolean("isVisible")) {
                shouldBeVisible = true;
            }
        }
        meta.path.add(index, latLng);

        //-----------------------------------
        // Recalculate the polygon bounds
        //-----------------------------------
        meta.bounds = PluginUtil.getBoundsFromPath(meta.path);

        boolean changeToVisible = shouldBeVisible;
        meta.polygon.setPoints(meta.path);
        if (changeToVisible) {
            meta.isVisible = true;
            meta.polygon.setVisible(true);
        }
        callbackContext.success();
    }

    /**
     * Set points
     */
    @PgmPluginMethod(runOnUiThread = true)
    public void setPointAt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String mapId = args.getString(0);
        String polygonId = args.getString(1);
        PluginPolygon instance = getInstance(mapId);
        MetaPolygon meta = instance.objects.get(polygonId);

        int index = args.getInt(2);
        JSONObject position = args.getJSONObject(3);
        final LatLng latLng = new LatLng(position.getDouble("lat"), position.getDouble("lng"));

        //------------------------
        // Update the hole list
        //------------------------
        meta.path.set(index, latLng);

        //-----------------------------------
        // Recalculate the polygon bounds
        //-----------------------------------
        meta.bounds = PluginUtil.getBoundsFromPath(meta.path);

        // Update the polygon
        meta.polygon.setPoints(meta.path);
        callbackContext.success();
    }

    /**
     * Set points
     */
    @PgmPluginMethod(runOnUiThread = true)
    public void setHoles(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String mapId = args.getString(0);
        String polygonId = args.getString(1);
        PluginPolygon instance = getInstance(mapId);
        MetaPolygon meta = instance.objects.get(polygonId);

        JSONArray holeList = args.getJSONArray(2);

        //------------------------
        // Update the hole list
        //------------------------
        for (int i = 0; i < meta.holePaths.size(); i++) {
            meta.holePaths.get(i).clear();
        }
        meta.holePaths.clear();

        JSONObject position;
        for (int i = 0; i < holeList.length(); i++) {
            ArrayList<LatLng> hole = new ArrayList<LatLng>();
            JSONArray holePositions = holeList.getJSONArray(i);
            for (int j = 0; j < holePositions.length(); j++) {
                position = holePositions.getJSONObject(j);
                hole.add(new LatLng(position.getDouble("lat"), position.getDouble("lng")));
            }
            meta.holePaths.add(hole);
        }

        // Update the polygon
        meta.polygon.setHoles(meta.holePaths);
        callbackContext.success();
    }
    /**
     * Set points
     */
    @PgmPluginMethod(runOnUiThread = true)
    public void insertPointOfHoleAt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String mapId = args.getString(0);
        String polygonId = args.getString(1);
        PluginPolygon instance = getInstance(mapId);
        MetaPolygon meta = instance.objects.get(polygonId);

        final int holeIndex = args.getInt(2);
        final int pointIndex = args.getInt(3);
        JSONObject position = args.getJSONObject(4);
        final LatLng latLng = new LatLng(position.getDouble("lat"), position.getDouble("lng"));


        //------------------------
        // Update the hole list
        //------------------------
        ArrayList<LatLng> hole = null;
        if (holeIndex < meta.holePaths.size()) {
            hole = meta.holePaths.get(holeIndex);
        }
        if (hole == null) {
            hole = new ArrayList<LatLng>();
        }
        if (meta.holePaths.size() == 0) {
            meta.holePaths.add(hole);
        }
        hole.add(pointIndex, latLng);

        // Update the polygon
        meta.polygon.setHoles(meta.holePaths);
        callbackContext.success();
    }

    /**
     * Set points
     */
    @PgmPluginMethod(runOnUiThread = true)
    public void setPointOfHoleAt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String mapId = args.getString(0);
        String polygonId = args.getString(1);
        PluginPolygon instance = getInstance(mapId);
        MetaPolygon meta = instance.objects.get(polygonId);

        int holeIndex = args.getInt(2);
        int pointIndex = args.getInt(3);
        JSONObject position = args.getJSONObject(4);
        LatLng latLng = new LatLng(position.getDouble("lat"), position.getDouble("lng"));

        //------------------------
        // Update the hole list
        //------------------------
        ArrayList<LatLng> hole = null;
        if (holeIndex < meta.holePaths.size()) {
            hole = meta.holePaths.get(holeIndex);
        }
        if (hole == null) {
            hole = new ArrayList<LatLng>();
        }
        if (meta.holePaths.size() == 0) {
            meta.holePaths.add(hole);
        }
        hole.set(pointIndex, latLng);

        // Update the polygon
        meta.polygon.setHoles(meta.holePaths);
        callbackContext.success();
    }

    /**
     * Set points
     */
    @PgmPluginMethod(runOnUiThread = true)
    public void removePointOfHoleAt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String mapId = args.getString(0);
        String polygonId = args.getString(1);
        PluginPolygon instance = getInstance(mapId);
        MetaPolygon meta = instance.objects.get(polygonId);

        final int holeIndex = args.getInt(2);
        final int pointIndex = args.getInt(3);

        //------------------------
        // Update the hole list
        //------------------------
        String propertyId = "polygon_holePaths_" + polygonId;
        ArrayList<LatLng> hole = null;
        if (holeIndex < meta.holePaths.size()) {
            hole = meta.holePaths.get(holeIndex);
        }
        if (hole == null) {
            hole = new ArrayList<LatLng>();
        }
        if (meta.holePaths.size() == 0) {
            meta.holePaths.add(hole);
        }
        hole.remove(pointIndex);

        // Update the polygon
        meta.polygon.setHoles(meta.holePaths);
        callbackContext.success();
    }


    @PgmPluginMethod(runOnUiThread = true)
    public void insertHoleAt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String mapId = args.getString(0);
        String polygonId = args.getString(1);
        PluginPolygon instance = getInstance(mapId);
        MetaPolygon meta = instance.objects.get(polygonId);

        final int holeIndex = args.getInt(2);

        JSONArray holeJson = args.getJSONArray(3);
        final ArrayList<LatLng> newHole = PluginUtil.JSONArray2LatLngList(holeJson);

        //------------------------
        // Update the hole list
        //------------------------
        meta.holePaths.add(holeIndex, newHole);

        // Update the polygon
        meta.polygon.setHoles(meta.holePaths);
        callbackContext.success();
    }

    /**
     * Set points
     */
    @PgmPluginMethod(runOnUiThread = true)
    public void setHoleAt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String mapId = args.getString(0);
        String polygonId = args.getString(1);
        PluginPolygon instance = getInstance(mapId);
        MetaPolygon meta = instance.objects.get(polygonId);

        //------------------------
        // Update the hole list
        //------------------------
        int holeIndex = args.getInt(2);
        JSONArray holeJson = args.getJSONArray(3);
        meta.holePaths.set(holeIndex, PluginUtil.JSONArray2LatLngList(holeJson));

        // Update the polygon
        meta.polygon.setHoles(meta.holePaths);
        callbackContext.success();
    }


    /**
     * Set points
     */
    @PgmPluginMethod(runOnUiThread = true)
    public void removeHoleAt(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        String mapId = args.getString(0);
        String polygonId = args.getString(1);
        PluginPolygon instance = getInstance(mapId);
        MetaPolygon meta = instance.objects.get(polygonId);

        //------------------------
        // Update the hole list
        //------------------------
        int holeIndex = args.getInt(2);
        meta.holePaths.remove(holeIndex);

        // Update the polygon
        meta.polygon.setHoles(meta.holePaths);
        callbackContext.success();
    }
    /**
     * Set visibility for the object
     */
    @PgmPluginMethod(runOnUiThread = true)
    public void setVisible(JSONArray args, CallbackContext callbackContext) throws JSONException {
        String mapId = args.getString(0);
        String polygonId = args.getString(1);
        PluginPolygon instance = getInstance(mapId);
        MetaPolygon meta = instance.objects.get(polygonId);

        boolean isVisible = args.getBoolean(2);
        meta.polygon.setVisible(isVisible);
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
        PluginPolygon instance = getInstance(mapId);
        MetaPolygon meta = instance.objects.get(polygonId);

        boolean clickable = args.getBoolean(2);
        meta.isClickable = clickable;
        meta.properties.put("isClickable", clickable);
        callbackContext.success();
    }
}
