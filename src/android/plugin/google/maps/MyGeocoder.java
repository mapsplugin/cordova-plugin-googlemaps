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
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLngBounds;

public class MyGeocoder extends CordovaPlugin {

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
  private void geocode(final JSONArray args, final CallbackContext callbackContext) {

    final JSONObject opts = args.getJSONObject(0);

    new AsyncTask<Void, Void, AsyncTaskResult<List<Address>>>()
    {
        @Override
        protected AsyncTaskResult<List<Address>> doInBackground(Void... params) {
          Geocoder geocoder = new Geocoder(MyGeocoder.this.cordova.getActivity());
          try {
            // Geocoding
            if (opts.has("address") && opts.has("position") == false) {
              String address = opts.getString("address");

              if (opts.has("bounds") == true) {
                JSONArray points = opts.getJSONArray("bounds");
                LatLngBounds bounds = PluginUtil.JSONArray2LatLngBounds(points);

                return new AsyncTaskResult<List<Address>>(geocoder.getFromLocationName(address, 20,
                    bounds.southwest.latitude, bounds.southwest.longitude,
                    bounds.northeast.latitude, bounds.northeast.longitude));
              } else
                return new AsyncTaskResult<List<Address>>(geocoder.getFromLocationName(address, 20));
            }
            // Reverse geocoding
            else if (opts.has("position") && opts.has("address") == false) {
              JSONObject position = opts.getJSONObject("position");
              return new AsyncTaskResult<List<Address>>(geocoder.getFromLocation(
                  position.getDouble("lat"),
                  position.getDouble("lng"), 20));
            }
          } catch (JSONException e) {
            return new AsyncTaskResult<List<Address>>(e);
          } catch (IOException  e) {
            return new AsyncTaskResult<List<Address>>(e);
          } catch (Exception e) {
            return null;
          }

          callbackContext.error("Invalid request for geocoder");
          return null;
        }

        @Override
        protected void onPostExecute(AsyncTaskResult<List<Address>> res) {
          if (res == null) {
            if (!callbackContext.isFinished())
              callbackContext.error("Geocoder service is not available.");

            return;
          }

          if (res.getError() != null) {
            res.getError().printStackTrace();
            callbackContext.error(res.getError().toString());
            return;
          }

          if (res.getResult().size() == 0) {
            callbackContext.error("Not found");
            return;
          }

          Iterator<Address> iterator = res.getResult().iterator();

          JSONArray results = new JSONArray();

          while(iterator.hasNext()) {
            try {
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
            } catch(JSONException e) {
              //TODO: Handle Exception
              return;
            }
          }
          callbackContext.success(results);

          //super.onPostExecute(result);
          // This is called when your operation is completed
        }
    }.execute();
  }
}
