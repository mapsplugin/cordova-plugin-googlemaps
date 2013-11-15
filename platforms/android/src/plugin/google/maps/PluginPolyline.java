      package plugin.google.maps;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class PluginPolyline extends CordovaPlugin implements MyPlugin  {
  private final String TAG = "PluginPolyline";
  public GoogleMap map = null;
  private HashMap<String, Polyline> polylines;

  @SuppressLint("UseSparseArrays")
  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    Log.d(TAG, "Polyline class initializing");
    this.polylines = new HashMap<String, Polyline>();
  }
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    String[] params = args.getString(0).split("\\.");
    try {
      Method method = this.getClass().getDeclaredMethod(params[1], JSONArray.class, CallbackContext.class);
      method.invoke(this, args, callbackContext);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
      return false;
    }
  }

  public void setMap(GoogleMap map) {
    this.map = map;
  }
  

  /**
   * Create polyline
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void createPolyline(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final PolylineOptions polylineOptions = new PolylineOptions();
    int color;
    
    JSONObject opts = args.getJSONObject(1);
    if (opts.has("points")) {
      JSONArray points = opts.getJSONArray("points");
      LatLng[] path = new LatLng[points.length()];
      JSONObject pointJSON;
      int i = 0;
      for (i = 0; i < points.length(); i++) {
        pointJSON = points.getJSONObject(i);
        path[i] = new LatLng(pointJSON.getDouble("lat"), pointJSON.getDouble("lng"));
      }
      polylineOptions.add(path);
    }
    if (opts.has("color")) {
      color = GoogleMaps.parsePluginColor(opts.getJSONArray("color"));
      polylineOptions.color(color);
    }
    if (opts.has("width")) {
      polylineOptions.width(opts.getInt("width"));
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
    
    Polyline polyline = map.addPolyline(polylineOptions);
    this.polylines.put(polyline.getId(), polyline);
    callbackContext.success(polyline.getId());
  }
  
  
  /**
   * set color
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void setColor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    int color = Color.parseColor(args.getString(2));
    Polyline polyline = this.polylines.get(id);
    polyline.setColor(color);
    callbackContext.success();
  }
  
  /**
   * set width
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void setWidth(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    float width = (float) args.getDouble(2);
    Polyline polyline = this.polylines.get(id);
    polyline.setWidth(width);
    callbackContext.success();
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
    Polyline polyline = this.polylines.get(id);
    polyline.setZIndex(zIndex);
    callbackContext.success();
  }
  
  /**
   * set visibility
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void setVisible(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    boolean visible = args.getBoolean(2);
    Polyline polyline = this.polylines.get(id);
    polyline.setVisible(visible);
    callbackContext.success();
  }

  /**
   * Remove the polyline
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void remove(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    Polyline polyline = this.polylines.get(id);
    this.polylines.remove(id);
    polyline.remove();
    callbackContext.success();
  }
}
