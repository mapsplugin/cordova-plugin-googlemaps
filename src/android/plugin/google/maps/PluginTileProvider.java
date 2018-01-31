package plugin.google.maps;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import org.apache.cordova.CordovaWebView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

public class PluginTileProvider implements TileProvider  {
  private final String TAG = "TileProvider";
  private int tileSize = 512;
  private Paint tilePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
  private Paint debugPaint = null;
  private TextPaint debugTextPaint = null;
  private String userAgent = null;
  private static BitmapCache tileCache = null;
  private OnCacheClear listener = null;
  private String webPageUrl = null;
  private AssetManager assetManager;
  private CordovaWebView webView;
  private String mapId, pluginId;
  private final HashMap<String, String> tileUrlMap = new HashMap<String, String>();
  private boolean isDebug = false;
  private Handler handler;
  private final Object semaphore = new Object();
  private Bitmap emptyBitmap = null;
  private final HashSet<String> cacheKeys = new HashSet<String>();
  private boolean isRemoved = false;

  @SuppressLint({"NewApi", "JavascriptInterface"})
  public PluginTileProvider(String mapId, String pluginId, CordovaWebView webView, AssetManager assetManager, String webPageUrl, String userAgent, int tileSize, boolean isDebug) {
    this.tileSize = tileSize;
    //this.tilePaint.setAlpha((int) (opacity * 255));
    this.userAgent = userAgent == null ? "Mozilla" : userAgent;
    this.webPageUrl = webPageUrl;
    this.assetManager = assetManager;
    this.webView = webView;
    this.mapId = mapId;
    this.pluginId = pluginId;

    // Get max available VM memory, exceeding this amount will throw an
    // OutOfMemory exception. Stored in kilobytes as LruCache takes an
    // int in its constructor.
    int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

    // Use 1/8th of the available memory for this memory cache.
    int cacheSize = maxMemory / 8;

    tileCache = new BitmapCache(cacheSize);

    this.isDebug = isDebug;
    if (isDebug) {
      debugPaint = new Paint();
      debugPaint.setTextSize(20);
      debugPaint.setColor(Color.RED);
      debugPaint.setStrokeWidth(1);
      debugPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

      debugTextPaint = new TextPaint();
      debugTextPaint.setTextSize(20);
      debugTextPaint.setColor(Color.RED);
      debugTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
    }

    handler = new Handler(Looper.getMainLooper());

  }

  public interface OnCacheClear {
    public void onCacheClear(int hashCode);
  }


  public void onGetTileUrlFromJS(String urlKey, String tileUrl) {
    synchronized (tileUrlMap) {
      tileUrlMap.put(urlKey, tileUrl);
    }
    synchronized (semaphore) {
      semaphore.notify();
    }
  }

  public void remove() {
    isRemoved = true;
    synchronized (cacheKeys) {
      Iterator<String> iterator = cacheKeys.iterator();
      String cacheKey;
      Bitmap image;
      while(iterator.hasNext()) {
        cacheKey = iterator.next();
        image = tileCache.remove(cacheKey);
        if (image != null && !image.isRecycled()) {
          image.recycle();
          image = null;
        }
      }
    }
    tileCache.evictAll();

  }
  public void setOnCacheClear(OnCacheClear listener) {
    this.listener = listener;
  }

  @Override
  public Tile getTile(int x, int y, int zoom) {
    if (isRemoved) {
      return null;
    }

    String urlStr = null;
    String originalUrlStr = null;
    final String urlKey = String.format(Locale.US, "%s-%s-%d-%d-%d", mapId, pluginId, x, y, zoom);
    synchronized (semaphore) {
      final String js = String.format(Locale.ENGLISH, "javascript:if(window.cordova){cordova.fireDocumentEvent('%s-%s-tileoverlay', {key: \"%s\", x: %d, y: %d, zoom: %d});}",
              mapId, pluginId, urlKey, x, y, zoom);

      handler.post(new Runnable() {
        @Override
        public void run() {
          webView.loadUrl(js);
        }
      });
      try {
        semaphore.wait(1000); // Maximum wait 1sec
      } catch (InterruptedException e) {
        e.printStackTrace();
        return null;
      }
    }
    synchronized (tileUrlMap) {
      urlStr = tileUrlMap.remove(urlKey);
    }
    originalUrlStr = urlStr;

    Tile tile = null;
    if (urlStr == null || "(null)".equals(urlStr)) {
      if (isDebug) {
        if (emptyBitmap == null) {
          emptyBitmap = Bitmap.createBitmap(tileSize, tileSize, Config.ARGB_8888);
        }
        Bitmap dummyBitmap = emptyBitmap.copy(Config.ARGB_8888, true);
        drawDebugInfo(dummyBitmap, x, y, zoom, originalUrlStr);
        tile = new Tile(tileSize, tileSize, bitmapToByteArray(dummyBitmap));
        dummyBitmap.recycle();
        return tile;
      } else {
        return null;
      }
    }

    try {
      InputStream inputStream = null;
      if (urlStr.startsWith("http://") || urlStr.startsWith("https://")) {
        //-------------------------------
        // load image from the internet
        //-------------------------------

        boolean redirect = true;
        URL url = new URL(urlStr);
        String cacheKey = url.hashCode() + "";
        Bitmap cachedImage = tileCache.get(cacheKey);
        if (cachedImage != null && !cachedImage.isRecycled()) {
          if (isDebug) {
            Bitmap copyImage = cachedImage.copy(Config.ARGB_8888, true);
            drawDebugInfo(copyImage, x, y, zoom, originalUrlStr);
            tile = new Tile(tileSize, tileSize, bitmapToByteArray(copyImage));
            copyImage.recycle();
          } else {
            tile = new Tile(tileSize, tileSize, bitmapToByteArray(cachedImage));
          }
          //cachedImage.recycle(); // Don't recycle it. Need to keep the bitmap instance
          return tile;
        }

        HttpURLConnection http = null;
        String cookies = null;
        int redirectCnt = 0;
        while(redirect && redirectCnt < 10) {
          redirect = false;
          http = (HttpURLConnection)url.openConnection();
          http.setRequestMethod("GET");
          http.setReadTimeout(3000);
          http.setConnectTimeout(3000);
          if (cookies != null) {
            http.setRequestProperty("Cookie", cookies);
          }
          http.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
          http.addRequestProperty("User-Agent", userAgent);
          http.setInstanceFollowRedirects(true);
          HttpURLConnection.setFollowRedirects(true);

          // normally, 3xx is redirect
          int status = http.getResponseCode();
          if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
              || status == HttpURLConnection.HTTP_MOVED_PERM
              || status == HttpURLConnection.HTTP_SEE_OTHER)
              redirect = true;
          }
          if (redirect) {
            // get redirect url from "location" header field
            url = new URL(http.getHeaderField("Location"));

            // get the cookie if need, for login
            cookies = http.getHeaderField("Set-Cookie");

            // Disconnect the current connection
            http.disconnect();
            redirectCnt++;
          }
        }
        if (http != null) {
          inputStream = http.getInputStream();

          Bitmap image = BitmapFactory.decodeStream(inputStream);
          if (image != null) {
            if (image.getWidth() != tileSize || image.getHeight() != tileSize) {
              Bitmap tileImage = this.resizeForTile(image);
              tileCache.put(cacheKey, tileImage.copy(Config.ARGB_8888, true));
              if (isDebug) {
                drawDebugInfo(tileImage, x, y, zoom, originalUrlStr);
              }
              tile = new Tile(tileSize, tileSize, bitmapToByteArray(tileImage));
              tileImage.recycle();
            } else {
              tileCache.put(cacheKey, image.copy(Config.ARGB_8888, true));

              if (isDebug) {
                drawDebugInfo(image, x, y, zoom, originalUrlStr);
              }
              tile = new Tile(tileSize, tileSize, bitmapToByteArray(image));
            }
            cacheKeys.add(cacheKey);
            image.recycle();
          }
          http.disconnect();
        }
        inputStream.close();
      } else {
        //---------------------------------
        // load image from the local path
        //---------------------------------

        if (!urlStr.contains("://") &&
          !urlStr.startsWith("/") &&
          !urlStr.startsWith("www/") &&
          !urlStr.startsWith("./") &&
          !urlStr.startsWith("../")) {
          urlStr = "./" + urlStr;
        }
        if (urlStr.startsWith("./")  || urlStr.startsWith("../")) {
          urlStr = urlStr.replace("././", "./");
          String currentPage = webPageUrl;
          currentPage = currentPage.replaceAll("[^\\/]*$", "");
          urlStr = currentPage + "/" + urlStr;
        }
        String cacheKey = new File(urlStr).hashCode() + "";
        Bitmap cachedImage = tileCache.get(cacheKey);
        if (cachedImage != null && !cachedImage.isRecycled()) {
          if (isDebug) {
            Bitmap copyImage = cachedImage.copy(Config.ARGB_8888, true);
            drawDebugInfo(copyImage, x, y, zoom, originalUrlStr);
            cachedImage.recycle();
            tile = new Tile(tileSize, tileSize, bitmapToByteArray(copyImage));
            copyImage.recycle();
          } else {
            tile = new Tile(tileSize, tileSize, bitmapToByteArray(cachedImage));
          }
          //cachedImage.recycle(); // Don't recycle it. Need to keep the bitmap instance
          return tile;
        }

        Bitmap image = null;
        if (urlStr.indexOf("file://") == 0 &&
          !urlStr.contains("file:///android_asset/")) {
          urlStr = urlStr.replace("file://", "");
          File tmp = new File(urlStr);
          if (tmp.exists()) {
            image = BitmapFactory.decodeFile(urlStr);
          } else {
            //Log.w("PluginTileProvider", "image is not found (" + urlStr + ")");
            return null;
          }
        } else {
          //Log.d(TAG, "imgUrl = " + imgUrl);
          if (urlStr.indexOf("file:///android_asset/") == 0) {
            urlStr = urlStr.replace("file:///android_asset/", "");
          }
          if (urlStr.contains("./")) {
            try {
              boolean isAbsolutePath = urlStr.startsWith("/");
              File relativePath = new File(urlStr);
              urlStr = relativePath.getCanonicalPath();
              //Log.d(TAG, "imgUrl = " + imgUrl);
              if (!isAbsolutePath) {
                urlStr = urlStr.substring(1);
              }
              //Log.d(TAG, "imgUrl = " + imgUrl);
            } catch (Exception e) {
              //e.printStackTrace();
            }
          }
          //Log.d("PluginTileProvider", "urlStr = " + urlStr);
          try {
            inputStream = assetManager.open(urlStr);
            image = BitmapFactory.decodeStream(inputStream);
            Bitmap tmpImage = image.copy(Config.ARGB_8888, true);
            image.recycle();
            image = tmpImage;
          } catch (IOException e) {
            //e.printStackTrace();
            return null;
          }
        }
        if (image != null) {
          if (image.getWidth() != tileSize || image.getHeight() != tileSize) {
            Bitmap tileImage = this.resizeForTile(image);
            tileCache.put(cacheKey, tileImage.copy(Config.ARGB_8888, true));
            if (isDebug) {
              drawDebugInfo(tileImage, x, y, zoom, originalUrlStr);
            }
            tile = new Tile(tileSize, tileSize, bitmapToByteArray(tileImage));
            tileImage.recycle();
          } else {
            tileCache.put(cacheKey, image.copy(Config.ARGB_8888, true));
            if (isDebug) {
              drawDebugInfo(image, x, y, zoom, originalUrlStr);
            }
            tile = new Tile(tileSize, tileSize, bitmapToByteArray(image));
          }
          image.recycle();
          cacheKeys.add(cacheKey);
        }

      }
      return tile;

    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private byte[] bitmapToByteArray(Bitmap bitmap) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.WEBP, 99, outputStream);
    return  outputStream.toByteArray();
  }

  private void drawDebugInfo(Bitmap bitmap, int x, int y, int zoom, String url) {
    Canvas canvas = new Canvas(bitmap);
    canvas.drawLine(0, 0, tileSize, 0, debugPaint);
    canvas.drawLine(0, 0, 0, tileSize, debugPaint);
    canvas.drawText(String.format(Locale.US, "x = %d, y = %d, zoom = %d", x, y, zoom), 30, 30, debugPaint);
    if (url != null) {
      StaticLayout mTextLayout = new StaticLayout(url, debugTextPaint,
          tileSize * 4 / 5, Layout.Alignment.ALIGN_NORMAL,
          1.0f, 0.0f, false);
      canvas.save();
      canvas.translate(30, 60);
      mTextLayout.draw(canvas);
      canvas.restore();
    }
  }

  private Bitmap resizeForTile(Bitmap bitmap) {

    if (bitmap == null) {
      return null;
    }
    /**
     * http://stackoverflow.com/questions/4821488/bad-image-quality-after-resizing-scaling-bitmap#7468636
     */
    Bitmap scaledBitmap = Bitmap.createBitmap(tileSize, tileSize, Config.ARGB_8888);

    float ratioX = tileSize / (float) bitmap.getWidth();
    float ratioY = tileSize / (float) bitmap.getHeight();
    float middleX = tileSize / 2.0f;
    float middleY = tileSize / 2.0f;

    Matrix scaleMatrix = new Matrix();
    scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

    Canvas canvas = new Canvas(scaledBitmap);
    canvas.setMatrix(scaleMatrix);
    canvas.drawBitmap(bitmap, middleX - bitmap.getWidth() / 2, middleY - bitmap.getHeight() / 2, tilePaint);
    bitmap.recycle();

    return scaledBitmap;
  }

}
