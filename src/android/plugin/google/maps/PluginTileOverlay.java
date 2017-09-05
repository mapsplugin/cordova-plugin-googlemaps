package plugin.google.maps;

import android.content.res.AssetManager;
import android.view.View;
import android.webkit.WebView;

import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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


    final JSONObject opts = args.getJSONObject(1);
    final int tileSize = opts.getInt("tileSize");


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
    final String id = opts.getString("_id");
    final boolean _isDebug = isDebug;

    cordova.getActivity().runOnUiThread(new Runnable() {
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
          View browserView = webView.getView();
          String browserViewName = browserView.getClass().getName();
          if ("org.xwalk.core.XWalkView".equals(browserViewName) ||
              "org.crosswalk.engine.XWalkCordovaView".equals(browserViewName)) {

            CordovaPreferences preferences = webView.getPreferences();
            // Set xwalk webview settings by Cordova preferences.
            String xwalkUserAgent = preferences == null ? "" : preferences.getString("xwalkUserAgent", "");
            if (!xwalkUserAgent.isEmpty()) {
              userAgent = xwalkUserAgent;
            }

            String appendUserAgent = preferences.getString("AppendUserAgent", "");
            if (!appendUserAgent.isEmpty()) {
              userAgent = userAgent + " " + appendUserAgent;
            }
          } else {
            userAgent = ((WebView) webView.getEngine().getView()).getSettings().getUserAgentString();
          }
        }

        String currentPageUrl = webView.getUrl();

        AssetManager assetManager = cordova.getActivity().getAssets();
        final PluginTileProvider tileProvider = new PluginTileProvider(pluginMap.mapId, id, webView, assetManager, currentPageUrl, userAgent, tileSize, _isDebug);
        tileProvider.setOnCacheClear(new PluginTileProvider.OnCacheClear() {
          @Override
          public void onCacheClear(int hashCode) {
            TileOverlay tileOverlay = (TileOverlay)PluginTileOverlay.this.getTileOverlay(hashCode+"");
            if (tileOverlay != null) {
              tileOverlay.clearTileCache();
              System.gc();
            }
          }
        });
        options.tileProvider(tileProvider);


        TileOverlay tileOverlay = map.addTileOverlay(options);
        //String id = tileOverlay.getId();

        pluginMap.objects.put("tileoverlay_" + id, tileOverlay);
        pluginMap.objects.put("tileprovider_" + id, tileProvider);

        try {
          JSONObject result = new JSONObject();
          result.put("hashCode", tileOverlay.hashCode());
          result.put("id", "tileoverlay_" + id);
          callbackContext.success(result);
        } catch (JSONException e) {
          e.printStackTrace();
          callbackContext.error("" + e.getMessage());
        }
      }
    });
  }

  @SuppressWarnings("unused")
  public void onGetTileUrlFromJS(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    String urlKey = args.getString(1);
    String tileUrl = args.getString(2);
    String pluginId = "tileprovider_" + id;
    if (pluginMap.objects.containsKey(pluginId)) {
      ((PluginTileProvider)(this.pluginMap.objects.get(pluginId))).onGetTileUrlFromJS(urlKey, tileUrl);
    }
    callbackContext.success();
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
    final TileOverlay tileOverlay = (TileOverlay)pluginMap.objects.get(id);
    if (tileOverlay == null) {
      callbackContext.success();
      return;
    }
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {

        tileOverlay.remove();
        tileOverlay.clearTileCache();

        try {
          String id = args.getString(0);
          pluginMap.objects.remove(id);
          id = id.replace("tileoverlay_", "tileprovider_");
          if (pluginMap.objects.containsKey(id)) {
            ((PluginTileProvider)(pluginMap.objects.get(id))).remove();
          }
          //pluginMap.objects.put(id, null);
          pluginMap.objects.remove(id);
          callbackContext.success();
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
    double opacity = 1 - args.getDouble(1);
    String id = args.getString(0);
    this.setFloat("setTransparency", id, (float)opacity, callbackContext);
  }

}
