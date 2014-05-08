package plugin.google.maps;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.net.Uri;

public class PluginExternal extends MyPlugin implements MyPluginInterface {

  /**
   * Send an intent to the navigation applications
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void launchNavigation(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    JSONObject params = args.getJSONObject(1);
    String from = params.getString("from");
    String to = params.getString("to");
    Intent intent = new Intent(android.content.Intent.ACTION_VIEW, 
    Uri.parse("http://maps.google.com/maps?saddr=" + from + "&daddr=" + to ));
    this.cordova.getActivity().startActivity(intent);
  }

}
