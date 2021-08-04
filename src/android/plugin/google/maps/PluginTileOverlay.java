package plugin.google.maps;

import android.content.res.AssetManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

import com.google.android.libraries.maps.model.TileOverlay;
import com.google.android.libraries.maps.model.TileOverlayOptions;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

public class PluginTileOverlay extends MyPlugin implements IOverlayPlugin {

  private PluginMap pluginMap;
  public final ConcurrentHashMap<String, MetaTileOverlay> objects = new ConcurrentHashMap<String, MetaTileOverlay>();

  @Override
  public void setPluginMap(PluginMap pluginMap) {
    this.pluginMap = pluginMap;
  }

  public PluginMap getMapInstance(String mapId) {
    return (PluginMap) CordovaGoogleMaps.viewPlugins.get(mapId);
  }
  public PluginTileOverlay getInstance(String mapId) {
    PluginMap mapInstance = getMapInstance(mapId);
    return (PluginTileOverlay) mapInstance.plugins.get(String.format("%s-tileoverlay", mapId));
  }

  /**
   * Create tile overlay
   */
  public void create(final JSONArray args,
    final CallbackContext callbackContext) throws JSONException {


    final JSONObject opts = args.getJSONObject(2);
    final String id = args.getString(3);
    final int tileSize = opts.getInt("tileSize");

    String tileOverlayId = "tileoverlay_" + id;
    final MetaTileOverlay meta = new MetaTileOverlay(tileOverlayId);
    meta.properties = opts;
    objects.put(tileOverlayId, meta);


    final TileOverlayOptions options = new TileOverlayOptions();
    if (opts.has("zIndex")) {
      options.zIndex((float)opts.getDouble("zIndex"));
    }
    if (opts.has("visible")) {
      options.visible(opts.getBoolean("visible"));
    }
    if (opts.has("opacity")) {
      options.transparency((float)(1 - opts.getDouble("opacity")));
    }

    boolean isDebug = false;
    if (opts.has("debug")) {
      isDebug = opts.getBoolean("debug");
    }
    final boolean _isDebug = isDebug;

    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {

        String userAgent = "";
        if (opts.has("userAgent")) {
          try {
            userAgent = opts.getString("userAgent");
          } catch (JSONException e) {
            //e.printStackTrace();
          }
        }
        if ("".equals(userAgent)) {
          userAgent = ((WebView) webView.getEngine().getView()).getSettings().getUserAgentString();
        }

        String currentPageUrl = webView.getUrl();

        AssetManager assetManager = activity.getAssets();
        final PluginTileProvider tileProvider = new PluginTileProvider(pluginMap.getOverlayId(), id, webView, assetManager, currentPageUrl, userAgent, tileSize, _isDebug);
//        tileProvider.setOnCacheClear(new PluginTileProvider.OnCacheClear() {
//          @Override
//          public void onCacheClear(int hashCode) {
//            TileOverlay tileOverlay = (TileOverlay)PluginTileOverlay.this.objects.
//            if (tileOverlay != null) {
//              tileOverlay.clearTileCache();
//              System.gc();
//            }
//          }
//        });
        options.tileProvider(tileProvider);


        TileOverlay tileOverlay = pluginMap.getGoogleMap().addTileOverlay(options);

        meta.tileOverlay = tileOverlay;
        meta.tileProvider = tileProvider;

        try {
          JSONObject result = new JSONObject();
          result.put("hashCode", id);
          result.put("__pgmId", meta.getId());
          callbackContext.success(result);
        } catch (JSONException e) {
          e.printStackTrace();
          callbackContext.error("" + e.getMessage());
        }
      }
    });
  }

  @PgmPluginMethod
  public void onGetTileUrlFromJS(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    PluginTileOverlay instance = getInstance(mapId);
    String tileOverlayId = args.getString(1);
    MetaTileOverlay meta = instance.objects.get(tileOverlayId);

    String urlKey = args.getString(2);
    String tileUrl = args.getString(3);

    Log.d(TAG, "tileOverlayId = " + tileOverlayId + ", meta = " + meta );
    meta.tileProvider.onGetTileUrlFromJS(urlKey, tileUrl);
    callbackContext.success();
  }

  /**
   * set z-index
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setZIndex(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    PluginTileOverlay instance = getInstance(mapId);
    String tileOverlayId = args.getString(1);
    MetaTileOverlay meta = instance.objects.get(tileOverlayId);

    float zIndex = (float) args.getDouble(2);
    meta.tileOverlay.setZIndex(zIndex);
    callbackContext.success();
  }

  /**
   * Set visibility for the object
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setVisible(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    PluginTileOverlay instance = getInstance(mapId);
    String tileOverlayId = args.getString(1);
    MetaTileOverlay meta = instance.objects.get(tileOverlayId);

    boolean visible = args.getBoolean(2);
    meta.tileOverlay.setVisible(visible);
    callbackContext.success();
  }
  /**
   * Remove this tile layer
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void remove(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    PluginTileOverlay instance = getInstance(mapId);
    String tileOverlayId = args.getString(1);
    MetaTileOverlay meta = instance.objects.remove(tileOverlayId);
    meta.tileOverlay.remove();
    meta.tileOverlay = null;
    meta.tileProvider.remove();;
    meta.tileProvider = null;
  }

  /**
   * Set fadeIn for the object
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setFadeIn(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    PluginTileOverlay instance = getInstance(mapId);
    String tileOverlayId = args.getString(1);
    MetaTileOverlay meta = instance.objects.get(tileOverlayId);

    boolean isFadedIn = args.getBoolean(2);
    meta.tileOverlay.setFadeIn(isFadedIn);
    callbackContext.success();
  }
  /**
   * Set opacity for the tile layer
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setOpacity(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    PluginTileOverlay instance = getInstance(mapId);
    String tileOverlayId = args.getString(1);
    MetaTileOverlay meta = instance.objects.get(tileOverlayId);

    double opacity = 1 - args.getDouble(1);
    meta.tileOverlay.setTransparency((float)opacity);
    callbackContext.success();
  }

}
