package plugin.google.maps;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.model.LatLngBounds;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PluginGeocoder extends CordovaPlugin {

  // In order to prevent the TOO_MANY_REQUEST_ERROR (block by Google because too many request in short period),
  // restricts the number of parallel threads.
  //
  // According from my tests,  5 threads are the best setting.
  private static ExecutorService executorService = Executors.newFixedThreadPool(5);
  private static Geocoder geocoder = null;


  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
    if (geocoder == null) {
      geocoder = new Geocoder(cordova.getActivity());
    }
  }


  @Override
  public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) {
    executorService.submit(new Runnable() {
      @Override
      public void run() {
        try {
          if ("geocode".equals(action)) {
            PluginGeocoder.this.geocode(args, callbackContext);
          } else {
            callbackContext.error("Method: Geocoder." + action + "() is not found.");
          }
        } catch (Exception e) {
          e.printStackTrace();
          callbackContext.error(e.getMessage() + "");
        }
      }
    });
    return true;
  }

  @SuppressWarnings("unused")
  private void geocode(final JSONArray args,
      final CallbackContext callbackContext) throws JSONException, IOException {

    JSONObject opts = args.getJSONObject(0);
    List<Address> geoResults = null;
    JSONArray results = new JSONArray();
    Iterator<Address> iterator = null;
    int retryLimit = 10;

    // Geocoding
    if (!opts.has("position") && opts.has("address")) {
      String address = opts.getString("address");
      if (opts.has("bounds")) {
        if (opts.has("bounds")) {
          JSONArray points = opts.getJSONArray("bounds");
          LatLngBounds bounds = PluginUtil.JSONArray2LatLngBounds(points);

          boolean retry = true;
          while (retry && retryLimit > 0) {
            retry = false;
            try {
              geoResults = geocoder.getFromLocationName(address, 5,
                  bounds.southwest.latitude, bounds.southwest.longitude,
                  bounds.northeast.latitude, bounds.northeast.longitude);
            } catch (IOException e) {
              if ("Timed out waiting for response from server".equals(e.getMessage())) {
                retry = true;
                retryLimit--;
                try {
                  Thread.sleep((int)(150 + Math.random() * 100));  // wait (150 + random)ms before retry
                } catch (InterruptedException e1) {
                  //e1.printStackTrace();
                }
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
          if (geoResults.size() == 0) {
            JSONObject methodResult = new JSONObject();
            methodResult.put("idx", opts.getInt("idx"));
            methodResult.put("results", new JSONArray());

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, methodResult);
            callbackContext.sendPluginResult(pluginResult);

            return;
          }
          iterator = geoResults.iterator();
        }
      } else {

        boolean retry = true;
        while (retry) {
          retry = false;
          try {
            geoResults = geocoder.getFromLocationName(address, 5);
          } catch (IOException e) {
            if ("Timed out waiting for response from server".equals(e.getMessage())) {
              retry = true;
              try {
                Thread.sleep((int)(150 + Math.random() * 100));  // wait (150 + random)ms before retry
              } catch (InterruptedException e1) {
                //e1.printStackTrace();
              }
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        if (geoResults == null || geoResults.size() == 0) {
          JSONObject methodResult = new JSONObject();
          methodResult.put("idx", opts.getInt("idx"));
          methodResult.put("results", new JSONArray());

          PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, methodResult);
          callbackContext.sendPluginResult(pluginResult);

          return;
        }
        iterator = geoResults.iterator();
      }
    }

    // Reverse geocoding
    if (opts.has("position") && !opts.has("address")) {
      JSONObject position = opts.getJSONObject("position");

      boolean retry = true;
      while (retry && retryLimit > 0) {
        retry = false;
        try {
          geoResults = geocoder.getFromLocation(
              position.getDouble("lat"),
              position.getDouble("lng"), 5);
        } catch (IOException e) {
          if ("Timed out waiting for response from server".equals(e.getMessage())) {
            retry = true;
            retryLimit--;
            try {
              Thread.sleep((int)(150 + Math.random() * 100));  // wait (150 + random)ms before retry
            } catch (InterruptedException e1) {
              //e1.printStackTrace();
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      if (geoResults == null || geoResults.size() == 0) {
        //-------------
        // No results
        //-------------
        JSONObject methodResult = new JSONObject();
        methodResult.put("idx", opts.getInt("idx"));
        methodResult.put("results", new JSONArray());

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, methodResult);
        callbackContext.sendPluginResult(pluginResult);

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

    JSONObject methodResult = new JSONObject();
    methodResult.put("idx", opts.getInt("idx"));
    methodResult.put("results", results);

    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, methodResult);
    callbackContext.sendPluginResult(pluginResult);
  }

}
