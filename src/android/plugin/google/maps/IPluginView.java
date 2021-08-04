package plugin.google.maps;

import android.view.ViewGroup;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;

public interface IPluginView {
//  boolean getVisible();
//  boolean getClickable();
//  String getDivId();
  ViewGroup getView();
//  int getViewDepth();
  MetaPluginView getMeta();
  void onDestroy();
  void onStart();
  void onStop();
  void onPause(boolean multitasking);
  void onResume(boolean multitasking);
  void remove(JSONArray args, final CallbackContext callbackContext) throws JSONException;
}
