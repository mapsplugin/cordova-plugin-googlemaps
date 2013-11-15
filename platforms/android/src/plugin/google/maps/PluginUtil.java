package plugin.google.maps;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.location.Location;

public class PluginUtil {

  @SuppressLint("NewApi")
  public static JSONObject location2Json(Location location) throws JSONException {
    JSONObject params = new JSONObject();
    params.put("lat", location.getLatitude());
    params.put("lng", location.getLongitude());
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
}
