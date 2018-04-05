package plugin.google.maps;

import android.app.Activity;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;

import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaOptions;
import com.google.android.gms.maps.StreetViewPanoramaView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;
import com.google.android.gms.maps.model.StreetViewPanoramaLink;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;
import com.google.android.gms.maps.model.StreetViewPanoramaOrientation;
import com.google.android.gms.maps.model.StreetViewSource;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;


public class PluginStreetViewPanorama extends MyPlugin implements
    IPluginView, StreetViewPanorama.OnStreetViewPanoramaCameraChangeListener,
    StreetViewPanorama.OnStreetViewPanoramaChangeListener,
    StreetViewPanorama.OnStreetViewPanoramaClickListener {

  private Activity mActivity;
  private Handler mainHandler;
  private StreetViewPanoramaView panoramaView;
  private StreetViewPanorama panorama;
  private String panoramaId;
  private boolean isVisible = true;
  private boolean isClickable = true;
  private final String TAG = "StreetView";
  private String divId;

  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
    mActivity = cordova.getActivity();
    mainHandler = new Handler(Looper.getMainLooper());
  }

  public String getDivId() {
    return this.divId;
  }
  public String getOverlayId() {
    return this.panoramaId;
  }
  public ViewGroup getView() {
    return this.panoramaView;
  }

  @Override
  public void remove(JSONArray args, CallbackContext callbackContext) {

  }

  public boolean getVisible() {
    return isVisible;
  }
  public boolean getClickable() {
    return isClickable;
  }
  public void getPanorama(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    panoramaId = args.getString(0);
    JSONObject jsOptions = args.getJSONObject(1);
    divId = args.getString(2);

    StreetViewPanoramaOptions svOptions = new StreetViewPanoramaOptions();
    if (jsOptions.has("camera")) {
      JSONObject cameraOpts = jsOptions.getJSONObject("camera");
      Object target = cameraOpts.get("target");
      if (target instanceof JSONObject) {
        JSONObject targetJson = cameraOpts.getJSONObject("target");
        LatLng position = new LatLng(targetJson.getDouble("lat"), targetJson.getDouble("lng"));

        if (cameraOpts.has("source")) {
          StreetViewSource source = "OUTDOOR".equals(cameraOpts.getString("source")) ?
              StreetViewSource.OUTDOOR : StreetViewSource.DEFAULT;
          if (cameraOpts.has("radius")) {
            svOptions.position(position, cameraOpts.getInt("radius"), source);
          } else {
            svOptions.position(position, source);
          }
        } else {
          if (cameraOpts.has("radius")) {
            svOptions.position(position, cameraOpts.getInt("radius"));
          } else {
            svOptions.position(position);
          }
        }
      } else if (target instanceof String) {
        svOptions.panoramaId(cameraOpts.getString("target"));
      }

      if (cameraOpts.has("bearing") ||
          cameraOpts.has("tilt") ||
          cameraOpts.has("zoom")) {
        StreetViewPanoramaCamera.Builder builder = StreetViewPanoramaCamera.builder();
        if (cameraOpts.has("bearing")) {
          builder.bearing = (float) cameraOpts.getDouble("bearing");
        }
        if (cameraOpts.has("tilt")) {
          builder.tilt = (float) cameraOpts.getDouble("tilt");
        }
        if (cameraOpts.has("zoom")) {
          builder.zoom = (float) cameraOpts.getDouble("zoom");
        }
        svOptions.panoramaCamera(builder.build());
      }
    }

    if (jsOptions.has("gestures")) {
      JSONObject gestures = jsOptions.getJSONObject("gestures");
      if (gestures.has("panning")) {
        svOptions.panningGesturesEnabled(gestures.getBoolean("panning"));
      }
      if (gestures.has("zoom")) {
        svOptions.zoomGesturesEnabled(gestures.getBoolean("zoom"));
      }
    }

    if (jsOptions.has("controls")) {
      JSONObject controls = jsOptions.getJSONObject("controls");
      if (controls.has("navigation")) {
        svOptions.userNavigationEnabled(controls.getBoolean("navigation"));
      }
      if (controls.has("streetNames")) {
        svOptions.streetNamesEnabled(controls.getBoolean("streetNames"));
      }
    }
    panoramaView = new StreetViewPanoramaView(mActivity, svOptions);

    mActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        panoramaView.onCreate(null);

        panoramaView.getStreetViewPanoramaAsync(new OnStreetViewPanoramaReadyCallback() {
          @Override
          public void onStreetViewPanoramaReady(StreetViewPanorama streetViewPanorama) {
            panoramaView.onResume();
            panorama = streetViewPanorama;

            panorama.setOnStreetViewPanoramaCameraChangeListener(PluginStreetViewPanorama.this);
            panorama.setOnStreetViewPanoramaChangeListener(PluginStreetViewPanorama.this);
            panorama.setOnStreetViewPanoramaClickListener(PluginStreetViewPanorama.this);

            // Don't support this because iOS does not support this feature.
            //panorama.setOnStreetViewPanoramaLongClickListener(PluginStreetViewPanorama.this);


            mapCtrl.mPluginLayout.addPluginOverlay(PluginStreetViewPanorama.this);
            callbackContext.success();
          }
        });
      }
    });
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  @Override
  public void onPause(boolean multitasking) {
    super.onPause(multitasking);
    if (panoramaView != null && panoramaView.isActivated()) {
      panoramaView.onPause();
    }
    mapCtrl.mPluginLayout.stopTimer();

    mapCtrl.mPluginLayout.removePluginOverlay(this.panoramaId);

  }
  @Override
  public void onResume(boolean multitasking) {
    super.onResume(multitasking);
    if (panoramaView != null && panoramaView.isActivated()) {
      panoramaView.onResume();
    }
    mapCtrl.mPluginLayout.addPluginOverlay(PluginStreetViewPanorama.this);
    mapCtrl.mPluginLayout.startTimer();
  }

  public void attachToWebView(JSONArray args, final CallbackContext callbackContext) {
    mapCtrl.mPluginLayout.addPluginOverlay(this);
    callbackContext.success();
  }
  public void detachFromWebView(JSONArray args, final CallbackContext callbackContext) {
    mapCtrl.mPluginLayout.removePluginOverlay(this.panoramaId);
    callbackContext.success();
  }


  public void moveCamera(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {

        try {
          final JSONObject cameraPosition = args.getJSONObject(0);
          if (cameraPosition.has("target")) {
            JSONObject target = cameraPosition.getJSONObject("target");
            LatLng latLng = new LatLng(target.getDouble("lat"), target.getDouble("lng"));
            panorama.setPosition(latLng);
          }

          if (cameraPosition.has("bearing") || cameraPosition.has("tilt")) {
            StreetViewPanoramaCamera currentCamera = panorama.getPanoramaCamera();
            float bearing = cameraPosition.has("bearing") ? (float) cameraPosition.getDouble("bearing") : currentCamera.bearing;
            float tilt = cameraPosition.has("tilt") ? (float) cameraPosition.getDouble("tilt") : currentCamera.tilt;
            float zoom = cameraPosition.has("zoom") ? (float) cameraPosition.getDouble("zoom") : currentCamera.zoom;


            StreetViewPanoramaCamera newCamera = new StreetViewPanoramaCamera(bearing, tilt, zoom);
            panorama.animateTo(newCamera, 0);
          }
          callbackContext.success();
        } catch (JSONException e) {
          e.printStackTrace();
          callbackContext.error("" + e.getMessage());
        }
      }
    });
  }

  @Override
  public void onStreetViewPanoramaCameraChange(StreetViewPanoramaCamera streetViewPanoramaCamera) {
    try {
      JSONObject camera = new JSONObject();
      camera.put("bearing", streetViewPanoramaCamera.bearing);
      camera.put("tilt", streetViewPanoramaCamera.tilt);
      camera.put("zoom", streetViewPanoramaCamera.zoom);

//      StreetViewPanoramaOrientation svOrientation = streetViewPanoramaCamera.getOrientation();
//      JSONObject orientation = new JSONObject();
//      orientation.put("bearing", svOrientation.bearing);
//      orientation.put("tilt", svOrientation.tilt);
//      camera.put("orientation", orientation);

      String jsonStr = camera.toString(0);
      jsCallback(
          String.format(
              Locale.ENGLISH,
              "javascript:if('%s' in plugin.google.maps){plugin.google.maps['%s']({evtName:'%s', callback:'_onPanoramaCameraChange', args: [%s]});}",
              panoramaId, panoramaId, "panorama_camera_change", jsonStr));
    } catch (Exception e) {
      // ignore
      e.printStackTrace();
    }

  }

  private void jsCallback(final String js) {
    this.mActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        webView.loadUrl(js);
      }
    });
  }

  @Override
  public void onStreetViewPanoramaChange(StreetViewPanoramaLocation streetViewPanoramaLocation) {

    try {
      JSONObject location = new JSONObject();
      location.put("panoId", streetViewPanoramaLocation.panoId);

      JSONObject position = new JSONObject();
      position.put("lat", streetViewPanoramaLocation.position.latitude);
      position.put("lng", streetViewPanoramaLocation.position.longitude);
      location.put("position", position);

      JSONArray links = new JSONArray();
      for (StreetViewPanoramaLink stLink : streetViewPanoramaLocation.links) {
        JSONObject link = new JSONObject();
        link.put("panoId", stLink.panoId);
        link.put("bearing", stLink.bearing);
        links.put(link);
      }
      location.put("links", links);

      String jsonStr = location.toString(0);
      jsCallback(
          String.format(
              Locale.ENGLISH,
              "javascript:if('%s' in plugin.google.maps){plugin.google.maps['%s']({evtName:'%s', callback:'_onPanoramaLocationChange', args: [%s]});}",
              panoramaId, panoramaId, "panorama_location_change", jsonStr));
    } catch (Exception e) {
      // ignore
      e.printStackTrace();
    }

  }

  @Override
  public void onStreetViewPanoramaClick(StreetViewPanoramaOrientation streetViewPanoramaOrientation) {

    try {
      JSONObject clickInfo  = new JSONObject();
      JSONObject orientation = new JSONObject();
      orientation.put("bearing", streetViewPanoramaOrientation.bearing);
      orientation.put("tilt", streetViewPanoramaOrientation.tilt);
      clickInfo.put("orientation", orientation);

      Point point = panorama.orientationToPoint(streetViewPanoramaOrientation);
      JSONArray pointArray = new JSONArray();
      pointArray.put((int)((double)point.x / (double)density));
      pointArray.put((int)((double)point.y / (double)density));
      clickInfo.put("point", pointArray);

      String jsonStr = clickInfo.toString(0);
      jsCallback(
          String.format(
              Locale.ENGLISH,
              "javascript:if('%s' in plugin.google.maps){plugin.google.maps['%s']({evtName:'%s', callback:'_onPanoramaEvent', args: [%s]});}",
              panoramaId, panoramaId, "panorama_click", jsonStr));
    } catch (Exception e) {
      // ignore
      e.printStackTrace();
    }

  }

}
