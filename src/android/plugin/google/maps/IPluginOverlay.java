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

  void remove(JSONArray args, final CallbackContext callbackContext);

  void onDestroy();
  void onStart();
  void onStop();
  void onPause(boolean multitasking);
  void onResume(boolean multitasking);
}
