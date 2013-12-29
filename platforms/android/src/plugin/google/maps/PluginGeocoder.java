package plugin.google.maps;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.LatLngBounds;

import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

public class PluginGeocoder extends MyPlugin implements MyPluginInterface {

  @SuppressWarnings("unused")
  private void createGeocoder(final JSONArray args,
      final CallbackContext callbackContext) throws JSONException, IOException {

    JSONObject opts = args.getJSONObject(1);
    Geocoder geocoder = new Geocoder(this.cordova.getActivity());
    List<Address> geoResults;
    JSONArray results = new JSONArray();
    Iterator<Address> iterator = null;

    // Geocoding
    if (opts.has("location") == false && opts.has("address")) {
      String address = opts.getString("address");
      if (opts.has("bounds")) {
        if (opts.has("bounds") == true) {
          JSONArray points = opts.getJSONArray("bounds");
          LatLngBounds bounds = PluginUtil.JSONArray2LatLngBounds(points);
          geoResults = geocoder.getFromLocationName(address, 20,
                bounds.southwest.latitude, bounds.southwest.longitude,
                bounds.northeast.latitude, bounds.northeast.longitude);
          iterator = geoResults.iterator();
        }
      } else {
        geoResults = geocoder.getFromLocationName(address, 20);
        iterator = geoResults.iterator();
      }
      
      while(iterator.hasNext()) {
        JSONObject result = new JSONObject();
        Address addr = iterator.next();

        JSONObject location = new JSONObject();
        location.put("lat", addr.getLatitude());
        location.put("lng", addr.getLongitude());
        result.put("location", location);

        result.put("locality", addr.getLocality());
        result.put("adminArea", addr.getAdminArea());
        result.put("country", addr.getCountryCode());
        result.put("featureName", addr.getFeatureName());
        result.put("locale", addr.getLocale());
        result.put("phone", addr.getPhone());
        result.put("permises", addr.getPremises());
        result.put("postalCode", addr.getPostalCode());
        result.put("subAdminArea", addr.getSubAdminArea());
        result.put("subLocality", addr.getSubLocality());
        result.put("subThoroughfare", addr.getSubThoroughfare());
        result.put("thoroughfare", addr.getThoroughfare());
        
        results.put(result);
      }
      callbackContext.success(results);
      return;
    }

    // Reverse geocoding
    if (opts.has("location") && opts.has("address") == false) {
      JSONObject position = opts.getJSONObject("position");
      // markerOptions.position(new LatLng(position.getDouble("lat"),
      // position.getDouble("lng")));
    }

    if (iterator == null) {
      callbackContext.error("Invalid request for geocoder");
      return;
    }
    
  }

}
