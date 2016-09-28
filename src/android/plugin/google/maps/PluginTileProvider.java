package plugin.google.maps;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import org.apache.cordova.CordovaResourceApi;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PluginTileProvider implements TileProvider  {
  private String tileUrlFormat = null;
  private int tileSize = 256;
  private Paint tilePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
  private String userAgent = null;
  private static BitmapCache tileCache = null;
  private OnCacheClear listener = null;
  private String webPageUrl = null;
  private AssetManager assetManager;

  public PluginTileProvider(AssetManager assetManager, String webPageUrl, String userAgent, String tileUrlFormat, int tileSize) {
    this.tileUrlFormat = tileUrlFormat;
    this.tileSize = tileSize;
    //this.tilePaint.setAlpha((int) (opacity * 255));
    this.userAgent = userAgent == null ? "Mozilla" : userAgent;
    this.webPageUrl = webPageUrl;
    this.assetManager = assetManager;

    // Get max available VM memory, exceeding this amount will throw an
    // OutOfMemory exception. Stored in kilobytes as LruCache takes an
    // int in its constructor.
    int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

    // Use 1/8th of the available memory for this memory cache.
    int cacheSize = maxMemory / 8;

    tileCache = new BitmapCache(cacheSize);
  }

  public interface OnCacheClear {
    public void onCacheClear(int hashCode);
  }

  public void setOnCacheClear(OnCacheClear listener) {
    this.listener = listener;
  }
  
  @Override
  public Tile getTile(int x, int y, int zoom) {

    String urlStr = tileUrlFormat.replaceAll("<x>", x + "")
        .replaceAll("<y>", y + "")
        .replaceAll("<zoom>", zoom + "");

    Tile tile = null;
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
        if (cachedImage != null) {
          return new Tile(tileSize, tileSize, bitmapToByteArray(cachedImage));
        }

        HttpURLConnection http = null;
        String cookies = null;
        int redirectCnt = 0;
        while(redirect && redirectCnt < 10) {
          redirect = false;
          http = (HttpURLConnection)url.openConnection();
          http.setRequestMethod("GET");
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
          if (image.getWidth() != tileSize || image.getHeight() != tileSize) {
            Bitmap tileImage = this.resizeForTile(image);
            tile = new Tile(tileSize, tileSize, bitmapToByteArray(tileImage));
            tileCache.put(cacheKey, tileImage.copy(tileImage.getConfig(), false));
            tileImage.recycle();
          } else {
            tile = new Tile(tileSize, tileSize, bitmapToByteArray(image));
            tileCache.put(cacheKey, image.copy(image.getConfig(), false));
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
        if (cachedImage != null) {
          return new Tile(tileSize, tileSize, bitmapToByteArray(cachedImage));
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
          } catch (IOException e) {
            //e.printStackTrace();
            return null;
          }
        }
        Log.d("PluginTileProvider", "cacheKey = " + cacheKey);
        if (image != null) {
          if (image.getWidth() != tileSize || image.getHeight() != tileSize) {
            Bitmap tileImage = this.resizeForTile(image);
            tile = new Tile(tileSize, tileSize, bitmapToByteArray(tileImage));
            tileCache.put(cacheKey, tileImage.copy(tileImage.getConfig(), false));
            tileImage.recycle();
          } else {
            tile = new Tile(tileSize, tileSize, bitmapToByteArray(image));
            tileCache.put(cacheKey, image);
          }
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
  
  //public void setOpacity(double opacity) {
  //  this.tilePaint.setAlpha((int) (opacity * 255));
  //}
  
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
