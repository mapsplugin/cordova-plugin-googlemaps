package plugin.google.maps;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class PluginLatLngBounds extends MyPlugin {

  @SuppressWarnings("unused")
  private void contains(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    JSONArray points = args.getJSONArray(1);
    LatLngBounds bounds = PluginUtil.JSONArray2LatLngBounds(points);
    
    JSONObject position = args.getJSONObject(2);
    LatLng latLng = new LatLng(position.getDouble("lat"), position.getDouble("lng"));
    Boolean isContains = (bounds.contains(latLng));
    callbackContext.success(isContains ? "true" : "false");
  }
}
