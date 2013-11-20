package plugin.google.maps;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.UrlTileProvider;

public class PluginTileOverlay extends CordovaPlugin implements MyPluginInterface {
  private final String TAG = "PluginTileOverlay";
  private HashMap<String, TileOverlay> overlays;

  public GoogleMaps mapCtrl = null;
  public GoogleMap map = null;

  public void setMapCtrl(GoogleMaps mapCtrl) {
    this.mapCtrl = mapCtrl;
    this.map = mapCtrl.map;
  }

  @SuppressLint("UseSparseArrays")
  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    Log.d(TAG, "TileOverlay class initializing");
    this.overlays = new HashMap<String, TileOverlay>();
  }

  @Override
  public boolean execute(String action, JSONArray args,
      CallbackContext callbackContext) throws JSONException {
    String[] params = args.getString(0).split("\\.");
    try {
      Method method = this.getClass().getDeclaredMethod(params[1],
          JSONArray.class, CallbackContext.class);
      method.invoke(this, args, callbackContext);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
      return false;
    }
  }

  /**
   * Create tile overlay
   * 
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void createTileOverlay(final JSONArray args,
    final CallbackContext callbackContext) throws JSONException {

    JSONObject opts = args.getJSONObject(1);
    final String tileUrlFormat = opts.getString("tileUrlFormat");
    UrlTileProvider tileProvider = new UrlTileProvider(256, 256) {

      @Override
      public URL getTileUrl(int x, int y, int zoom) {
        String urlStr = tileUrlFormat.replaceAll("<x>", x + "")
                                     .replaceAll("<y>", y + "")
                                     .replaceAll("<zoom>", zoom + "");
        Log.d(TAG, urlStr);
        URL url = null;
        try {
          url = new URL(urlStr);
        } catch (MalformedURLException e) {
          e.printStackTrace();
        }
        return url;
      }
    };

    TileOverlayOptions options = new TileOverlayOptions();
    options.tileProvider(tileProvider);
    if (opts.has("zIndex")) {
      options.zIndex((float)opts.getDouble("zIndex"));
    }
    if (opts.has("visible")) {
      options.visible(opts.getBoolean("visible"));
    }
    TileOverlay tileOverlay = this.map.addTileOverlay(options);
    String tileId = tileOverlay.getId();
    this.overlays.put(tileId, tileOverlay);
    

    callbackContext.success(tileId);
  }

  /**
   * set visibility
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void setVisible(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    boolean visible = args.getBoolean(2);
    TileOverlay tileOverlay = this.overlays.get(id);
    tileOverlay.setVisible(visible);
    callbackContext.success();
  }
}
