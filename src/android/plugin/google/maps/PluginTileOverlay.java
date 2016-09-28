package plugin.google.maps;

import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

public class PluginTileOverlay extends MyPlugin implements MyPluginInterface {


  /**
   * Create tile overlay
   * 
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void create(final JSONArray args,
    final CallbackContext callbackContext) throws JSONException {

    final JSONObject properties = new JSONObject();
    JSONObject opts = args.getJSONObject(1);
    final int tileSize = opts.getInt("tileSize");
    final String tileUrlFormat = opts.getString("tileUrlFormat");

    double opacity = 1;
    if (opts.has("opacity")) {
      opacity = opts.getDouble("opacity");
    }
    final PluginTileProvider tileProvider = new PluginTileProvider(tileUrlFormat, opacity, tileSize);
    final String id = "" + tileProvider.hashCode();

    final TileOverlayOptions options = new TileOverlayOptions();
    options.tileProvider(tileProvider);
    if (opts.has("zIndex")) {
      options.zIndex((float)opts.getDouble("zIndex"));
    }
    if (opts.has("visible")) {
      options.visible(opts.getBoolean("visible"));
    }
    Log.d("TileOverlay", "---> transparency = " + options.isVisible());

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {

        TileOverlay tileOverlay = map.addTileOverlay(options);

        self.objects.put("tileoverlay_" + id, tileOverlay);
        self.objects.put("tileprovider_" + id, tileProvider);

        try {
          JSONObject result = new JSONObject();
          result.put("hashCode", tileOverlay.hashCode());
          result.put("id", id);
          callbackContext.success(result);
        } catch (JSONException e) {
          e.printStackTrace();
          callbackContext.error("" + e.getMessage());
        }
      }
    });
  }

  /**
   * set z-index
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  public void setZIndex(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    float zIndex = (float) args.getDouble(1);
    this.setFloat("setZIndex", id, zIndex, callbackContext);
  }

  /**
   * Set visibility for the object
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void setVisible(JSONArray args, CallbackContext callbackContext) throws JSONException {
    boolean visible = args.getBoolean(1);
    String id = args.getString(0);
    this.setBoolean("setVisible", id, visible, callbackContext);
  }
  /**
   * Remove this tile layer
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void remove(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    final TileOverlay tileOverlay = (TileOverlay)self.objects.get(id);
    if (tileOverlay == null) {
      this.sendNoResult(callbackContext);
      return;
    }
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {

        tileOverlay.remove();
        tileOverlay.clearTileCache();

        try {
          String id = args.getString(0);
          id = id.replace("tileoverlay_", "tileprovider_");
          self.objects.put(id, null);
          self.objects.remove(id);
          sendNoResult(callbackContext);
        } catch (JSONException e) {
          e.printStackTrace();
          callbackContext.error("" + e.getMessage());
          
        }
      }
    });
  }

  /**
   * Set fadeIn for the object
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void setFadeIn(JSONArray args, CallbackContext callbackContext) throws JSONException {
    boolean visible = args.getBoolean(1);
    String id = args.getString(0);
    this.setBoolean("setFadeIn", id, visible, callbackContext);
  }
  /**
   * Set opacity for the tile layer
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void setOpacity(JSONArray args, CallbackContext callbackContext) throws JSONException {
    double opacity = args.getDouble(1);
    String id = args.getString(0);
    this.setFloat("setTransparency", id, (float)opacity, callbackContext);
  }

}
