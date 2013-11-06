package plugin.google.maps;

import java.util.HashMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Color;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

public class CircleManager extends HashMap<String, Circle> {
    private CordovaInterface cordova;
    public CircleManager(CordovaInterface cordovaInterface) {
        super();
        cordova = cordovaInterface;
    }
    public Boolean addCircle(final GoogleMap map, final JSONArray args, final CallbackContext callbackContext) {
        final CircleOptions circleOptions = new CircleOptions();
        int color;
        
        try {
            JSONObject opts = args.getJSONObject(0);
            if (opts.has("lat") && opts.has("lng")) {
                circleOptions.center(new LatLng(opts.getDouble("lat"), opts.getDouble("lng")));
            }
            if (opts.has("radius")) {
                circleOptions.radius(opts.getDouble("radius"));
            }
            if (opts.has("strokeColor")) {
                color = Color.parseColor(opts.getString("strokeColor"));
                circleOptions.strokeColor(color);
            }
            if (opts.has("fillColor")) {
                color = Color.parseColor(opts.getString("fillColor"));
                circleOptions.fillColor(color);
            }
            if (opts.has("strokeWidth")) {
                circleOptions.strokeWidth(opts.getInt("strokeWidth"));
            }
            if (opts.has("visible")) {
                circleOptions.visible(opts.getBoolean("visible"));
            }
            if (opts.has("zIndex")) {
                circleOptions.zIndex(opts.getInt("zIndex"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            callbackContext.error(e.getMessage());
            return false;
        }
        
        Runnable runnable = new Runnable(){ 
            public void run() {
                Circle circle = map.addCircle(circleOptions);
                CircleManager.this.put(circle.getId(), circle);
                callbackContext.success(circle.getId());
            }
        };
        cordova.getActivity().runOnUiThread(runnable);
        return true;
    }
    
    public Boolean setCenter(final JSONArray args, final CallbackContext callbackContext) {
        Runnable runnable = new Runnable(){ 
            public void run() {
                try {
                    String id = args.getString(0);
                    LatLng center = new LatLng(args.getDouble(1), args.getDouble(2));
                    Circle circle = CircleManager.this.get(id);
                    circle.setCenter(center);
                    callbackContext.success();
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                };
            }
        };
        cordova.getActivity().runOnUiThread(runnable);
        return true;
    }
    

    public Boolean setFillColor(final JSONArray args, final CallbackContext callbackContext) {
        Runnable runnable = new Runnable(){ 
            public void run() {
                try {
                    String id = args.getString(0);
                    int color = Color.parseColor(args.getString(1));
                    Circle circle = CircleManager.this.get(id);
                    circle.setFillColor(color);
                    callbackContext.success();
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                };
            }
        };
        cordova.getActivity().runOnUiThread(runnable);
        return true;
    }
    public Boolean setStrokeColor(final JSONArray args, final CallbackContext callbackContext) {
        Runnable runnable = new Runnable(){ 
            public void run() {
                try {
                    String id = args.getString(0);
                    int color = Color.parseColor(args.getString(1));
                    Circle circle = CircleManager.this.get(id);
                    circle.setStrokeColor(color);
                    callbackContext.success();
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                };
            }
        };
        cordova.getActivity().runOnUiThread(runnable);
        return true;
    }
    public Boolean setStrokeWidth(final JSONArray args, final CallbackContext callbackContext) {
        Runnable runnable = new Runnable(){ 
            public void run() {
                try {
                    String id = args.getString(0);
                    float width = (float) args.getDouble(1);
                    Circle circle = CircleManager.this.get(id);
                    circle.setStrokeWidth(width);
                    callbackContext.success();
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                };
            }
        };
        cordova.getActivity().runOnUiThread(runnable);
        return true;
    }
    public Boolean setRadius(final JSONArray args, final CallbackContext callbackContext) {
        Runnable runnable = new Runnable(){ 
            public void run() {
                try {
                    String id = args.getString(0);
                    float radius = (float) args.getDouble(1);
                    Circle circle = CircleManager.this.get(id);
                    circle.setRadius(radius);
                    callbackContext.success();
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                };
            }
        };
        cordova.getActivity().runOnUiThread(runnable);
        return true;
    }
    public Boolean setZIndex(final JSONArray args, final CallbackContext callbackContext) {
        Runnable runnable = new Runnable(){ 
            public void run() {
                try {
                    String id = args.getString(0);
                    float zIndex = (float) args.getDouble(1);
                    Circle circle = CircleManager.this.get(id);
                    circle.setZIndex(zIndex);
                    callbackContext.success();
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                };
            }
        };
        cordova.getActivity().runOnUiThread(runnable);
        return true;
    }
    public Boolean setVisible(final JSONArray args, final CallbackContext callbackContext) {
        Runnable runnable = new Runnable(){ 
            public void run() {
                try {
                    String id = args.getString(0);
                    boolean visible = args.getBoolean(1);
                    Circle circle = CircleManager.this.get(id);
                    circle.setVisible(visible);
                    callbackContext.success();
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                };
            }
        };
        cordova.getActivity().runOnUiThread(runnable);
        return true;
    }

    
    
    public Boolean remove(final JSONArray args, final CallbackContext callbackContext) {
        Runnable runnable = new Runnable(){ 
            public void run() {
                try {
                    String id = args.getString(0);
                    Circle circle = CircleManager.this.get(id);
                    circle.remove();
                    callbackContext.success();
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                };
            }
        };
        cordova.getActivity().runOnUiThread(runnable);
        return true;
    }
}
