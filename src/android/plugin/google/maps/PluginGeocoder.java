package plugin.google.maps;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.model.LatLngBounds;

public class PluginGeocoder extends CordovaPlugin {

  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) {
    try {
      Method method = this.getClass().getDeclaredMethod(action, JSONArray.class, CallbackContext.class);
      if (method.isAccessible() == false) {
        method.setAccessible(true);
      }
      method.invoke(this, args, callbackContext);
      return true;
    } catch (Exception e) {
      Log.e("CordovaLog", "An error occurred", e);
      callbackContext.error(e.toString());
      return false;
    }
  }
  
  @SuppressWarnings("unused")
  private void geocode(final JSONArray args,
      final CallbackContext callbackContext) throws JSONException, IOException {

    JSONObject opts = args.getJSONObject(0);
    Geocoder geocoder = new Geocoder(this.cordova.getActivity());
    List<Address> geoResults;
    JSONArray results = new JSONArray();
    Iterator<Address> iterator = null;

    // Geocoding
    if (opts.has("position") == false && opts.has("address")) {
      String address = opts.getString("address");
      if (opts.has("bounds")) {
        if (opts.has("bounds") == true) {
          JSONArray points = opts.getJSONArray("bounds");
          LatLngBounds bounds = PluginUtil.JSONArray2LatLngBounds(points);
          try {
            geoResults = geocoder.getFromLocationName(address, 20,
                bounds.southwest.latitude, bounds.southwest.longitude,
                bounds.northeast.latitude, bounds.northeast.longitude);
          }catch (Exception e) {
            callbackContext.error("Geocoder service is not available.");
            return;
          }
          if (geoResults.size() == 0) {
            callbackContext.error("Not found");
            return;
          }
          iterator = geoResults.iterator();
        }
      } else {
        try {
          geoResults = geocoder.getFromLocationName(address, 20);
        }catch (Exception e) {
          callbackContext.error("Geocoder service is not available.");
          return;
        }
        if (geoResults.size() == 0) {
          callbackContext.error("Not found");
          return;
        }
        iterator = geoResults.iterator();
      }
    }

    // Reverse geocoding
    if (opts.has("position") && opts.has("address") == false) {
      JSONObject position = opts.getJSONObject("position");
      try {
        geoResults = geocoder.getFromLocation(
            position.getDouble("lat"), 
            position.getDouble("lng"), 20);
      } catch (Exception e) {
        callbackContext.error("Geocoder service is not available.");
        return;
      }
      if (geoResults.size() == 0) {
        callbackContext.error("Not found");
        return;
      }
      iterator = geoResults.iterator();
    }
    
    if (iterator == null) {
      callbackContext.error( "Invalid request for geocoder");
      return;
    }
    
    while(iterator.hasNext()) {
      JSONObject result = new JSONObject();
      Address addr = iterator.next();

      JSONObject position = new JSONObject();
      position.put("lat", addr.getLatitude());
      position.put("lng", addr.getLongitude());
      result.put("position", position);

      result.put("locality", addr.getLocality());
      result.put("adminArea", addr.getAdminArea());
      result.put("country", addr.getCountryName());
      result.put("countryCode", addr.getCountryCode());
      result.put("locale", addr.getLocale());
      result.put("postalCode", addr.getPostalCode());
      result.put("subAdminArea", addr.getSubAdminArea());
      result.put("subLocality", addr.getSubLocality());
      result.put("subThoroughfare", addr.getSubThoroughfare());
      result.put("thoroughfare", addr.getThoroughfare());
      

      JSONObject extra = new JSONObject();
      extra.put("featureName", addr.getFeatureName());
      extra.put("phone", addr.getPhone());
      extra.put("permises", addr.getPremises());
      extra.put("url", addr.getUrl());

      JSONArray lines = new JSONArray();
      for (int i = 0; i < addr.getMaxAddressLineIndex(); i++) {
         lines.put(addr.getAddressLine(i));
      }
      extra.put("lines", lines);

      
      Bundle extraInfo = addr.getExtras();
      if (extraInfo != null) {
        Set<String> keys = extraInfo.keySet();
        Iterator<String> keyIterator = keys.iterator();
        String key;
        while(keyIterator.hasNext()) {
          key = keyIterator.next();
          extra.put(key, extraInfo.get(key));
        }
      }
      result.put("extra", extra);
      results.put(result);
    }
    callbackContext.success(results);
    
  }

}
