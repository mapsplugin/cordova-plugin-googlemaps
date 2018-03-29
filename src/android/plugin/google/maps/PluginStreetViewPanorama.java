package plugin.google.maps;

import android.app.Activity;
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

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;


public class PluginStreetViewPanorama extends MyPlugin implements IPluginOverlay {

  private Activity mActivity;
  private Handler mainHandler;
  private StreetViewPanoramaView panoramaView;
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
    divId = args.getString(2);

    StreetViewPanoramaOptions svOptions = new StreetViewPanoramaOptions();
    LatLng position = new LatLng(-33.87365, 151.20689);
    svOptions.position(position);

    panoramaView = new StreetViewPanoramaView(mActivity, svOptions);

    mActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        panoramaView.onCreate(null);

        panoramaView.getStreetViewPanoramaAsync(new OnStreetViewPanoramaReadyCallback() {
          @Override
          public void onStreetViewPanoramaReady(StreetViewPanorama panorama) {
            panoramaView.onResume();


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
}
