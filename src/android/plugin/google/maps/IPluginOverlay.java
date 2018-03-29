package plugin.google.maps;

import android.view.View;
import android.view.ViewGroup;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;

public interface IPluginOverlay {
  boolean getVisible();
  boolean getClickable();
  String getDivId();
  String getOverlayId();
  ViewGroup getView();
  void onDestroy();
  void onStart();
  void onStop();
  void onPause(boolean multitasking);
  void onResume(boolean multitasking);
  void remove(JSONArray args, final CallbackContext callbackContext);
  void attachToWebView(JSONArray args, final CallbackContext callbackContext);
  void detachFromWebView(JSONArray args, final CallbackContext callbackContext);
}
