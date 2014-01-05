package plugin.google.maps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;

public class PluginUtil {

  @SuppressLint("NewApi")
  public static JSONObject location2Json(Location location) throws JSONException {
    JSONObject latLng = new JSONObject();
    latLng.put("lat", location.getLatitude());
    latLng.put("lng", location.getLongitude());
    
    JSONObject params = new JSONObject();
    params.put("latLng", latLng);
    params.put("elapsedRealtimeNanos", location.getElapsedRealtimeNanos());
    params.put("time", location.getTime());
    if (location.hasAccuracy()) {
      params.put("accuracy", location.getAccuracy());
    }
    if (location.hasBearing()) {
      params.put("bearing", location.getBearing());
    }
    if (location.hasAltitude()) {
      params.put("altitude", location.getAltitude());
    }
    if (location.hasSpeed()) {
      params.put("speed", location.getSpeed());
    }
    params.put("provider", location.getProvider());
    params.put("hashCode", location.hashCode());
    return params;
  }
  
  /**
   * return color integer value
   * @param arrayRGBA
   * @throws JSONException
   */
  public static int parsePluginColor(JSONArray arrayRGBA) throws JSONException {
    return Color.argb(arrayRGBA.getInt(3), arrayRGBA.getInt(0), arrayRGBA.getInt(1), arrayRGBA.getInt(2));
  }

  public static List<LatLng> JSONArray2LatLngList(JSONArray points) throws JSONException  {
    List<LatLng> path = new ArrayList<LatLng>();
    JSONObject pointJSON;
    int i = 0;
    for (i = 0; i < points.length(); i++) {
      pointJSON = points.getJSONObject(i);
      path.add(new LatLng(pointJSON.getDouble("lat"), pointJSON.getDouble("lng")));
    }
    return path;
  }
  /*
  public static Set<LatLng> JSONArray2LatLngHash(JSONArray points) throws JSONException  {
    Set<LatLng> path = new HashSet<LatLng>();
    JSONObject pointJSON;
    int i = 0;
    for (i = 0; i < points.length(); i++) {
      pointJSON = points.getJSONObject(i);
      path.add(new LatLng(pointJSON.getDouble("lat"), pointJSON.getDouble("lng"))); 
    }
    return path;
  }
  */
  public static LatLngBounds JSONArray2LatLngBounds(JSONArray points) throws JSONException {
    List<LatLng> path = JSONArray2LatLngList(points);
    Builder builder = LatLngBounds.builder();
    int i = 0;
    for (i = 0; i < path.size(); i++) {
      builder.include(path.get(i));
    }
    return builder.build();
  }
}
