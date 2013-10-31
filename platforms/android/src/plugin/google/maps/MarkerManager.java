package plugin.google.maps;

import java.util.HashMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MarkerManager extends HashMap<String, Marker> {
    private CordovaInterface cordova;
    public MarkerManager(CordovaInterface cordovaInterface) {
        super();
        cordova = cordovaInterface;
    }
    public Boolean addMarker(final GoogleMap map, final JSONArray args, final CallbackContext callbackContext) {
        final MarkerOptions markerOptions = new MarkerOptions();
        String iconVal = null;
        try {
            JSONObject opts = args.getJSONObject(0);
            if (opts.has("lat") && opts.has("lng")) {
                markerOptions.position(new LatLng(opts.getDouble("lat"), opts.getDouble("lng")));
            }
            if (opts.has("title")) {
                markerOptions.title(opts.getString("title"));
            }
            if (opts.has("snippet")) {
                markerOptions.snippet(opts.getString("snippet"));
            }
            if (opts.has("visible")) {
                markerOptions.visible(opts.getBoolean("visible"));
            }
            if (opts.has("icon")) {
                iconVal = opts.getString("icon");
            }
        } catch (Exception e) {
            e.printStackTrace();
            callbackContext.error(e.getMessage());
            return false;
        }
        final String iconUrl = iconVal;
        
        
        Runnable runnable = new Runnable(){ 
            public void run() {
                Marker marker = map.addMarker(markerOptions);
                MarkerManager.this.put(marker.getId(), marker);
                
                if (iconUrl != null) {
                    Log.d("CordovaLog", iconUrl);
                    if (iconUrl.indexOf("http") == 0) {
                        MarkerSetIcon task = new MarkerSetIcon(marker);
                        task.execute(iconUrl);
                    } else {
                        marker.setIcon(BitmapDescriptorFactory.fromAsset(iconUrl));
                    }
                }
                JSONObject result = new JSONObject();
                try {
                    result.put("id", marker.getId());
                    result.put("hashCode", marker.hashCode());
                    callbackContext.success(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                }
            }
        };
        cordova.getActivity().runOnUiThread(runnable);
        return true;
    }
    public Boolean setTitle(final JSONArray args, final CallbackContext callbackContext) {
        Runnable runnable = new Runnable(){ 
            public void run() {
                try {
                    String id = args.getString(0);
                    String title = args.getString(1);
                    Marker marker = MarkerManager.this.get(id);
                    marker.setTitle(title);
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
    public Boolean setSnippet(final JSONArray args, final CallbackContext callbackContext) {
        Runnable runnable = new Runnable(){ 
            public void run() {
                try {
                    String id = args.getString(0);
                    String snippet = args.getString(1);
                    Marker marker = MarkerManager.this.get(id);
                    marker.setSnippet(snippet);
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
    

    public Boolean showInfoWindow(final JSONArray args, final CallbackContext callbackContext) {
        Runnable runnable = new Runnable(){ 
            public void run() {
                try {
                    String id = args.getString(0);
                    Marker marker = MarkerManager.this.get(id);
                    marker.showInfoWindow();
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
    public Boolean hideInfoWindow(final JSONArray args, final CallbackContext callbackContext) {
        Runnable runnable = new Runnable(){ 
            public void run() {
                try {
                    String id = args.getString(0);
                    Marker marker = MarkerManager.this.get(id);
                    marker.hideInfoWindow();
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

    public Boolean getPosition(final JSONArray args, final CallbackContext callbackContext) {
        Runnable runnable = new Runnable(){ 
            public void run() {
                try {
                    String id = args.getString(0);
                    Marker marker = MarkerManager.this.get(id);
                    LatLng position = marker.getPosition();
                    
                    JSONArray result = new JSONArray();
                    result.put(position.latitude);
                    result.put(position.longitude);
                    callbackContext.success(result);
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                };
            }
        };
        cordova.getActivity().runOnUiThread(runnable);
        return true;
    }
    public Boolean isInfoWindowShown(final JSONArray args, final CallbackContext callbackContext) {
        Runnable runnable = new Runnable(){ 
            public void run() {
                try {
                    String id = args.getString(0);
                    Marker marker = MarkerManager.this.get(id);
                    Boolean isInfoWndShown = marker.isInfoWindowShown();
                    
                    callbackContext.success(isInfoWndShown ? 1 : 0);
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
                    Marker marker = MarkerManager.this.get(id);
                    marker.remove();
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
    public Boolean setAnchor(final JSONArray args, final CallbackContext callbackContext) {
        Runnable runnable = new Runnable(){ 
            public void run() {
                try {
                    String id = args.getString(0);
                    float anchorU = (float)args.getDouble(1);
                    float anchorV = (float)args.getDouble(2);
                    Marker marker = MarkerManager.this.get(id);
                    marker.setAnchor(anchorU, anchorV);
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
    public Boolean setDraggable(final JSONArray args, final CallbackContext callbackContext) {
        Runnable runnable = new Runnable(){ 
            public void run() {
                try {
                    String id = args.getString(0);
                    Boolean draggable = args.getBoolean(1);
                    Marker marker = MarkerManager.this.get(id);
                    marker.setDraggable(draggable);
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
    public Boolean setIcon(final JSONArray args, final CallbackContext callbackContext) {
        Runnable runnable = new Runnable(){ 
            public void run() {
                try {
                    String id = args.getString(0);
                    String iconUrl = args.getString(1);
                    
                    Marker marker = MarkerManager.this.get(id);
                    if (iconUrl.indexOf("http") == 0) {
                        MarkerSetIcon task = new MarkerSetIcon(marker);
                        task.execute(iconUrl);
                    } else {
                        marker.setIcon(BitmapDescriptorFactory.fromAsset(iconUrl));
                    }
                    
                    
                    
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
