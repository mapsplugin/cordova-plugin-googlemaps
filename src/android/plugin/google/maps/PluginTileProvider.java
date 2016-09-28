package plugin.google.maps;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import java.io.ByteArrayOutputStream;
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

  public PluginTileProvider(String userAgent, String tileUrlFormat, double opacity, int tileSize) {
    this.tileUrlFormat = tileUrlFormat;
    this.tileSize = tileSize;
    this.tilePaint.setAlpha((int) (opacity * 255));
    this.userAgent = userAgent == null ? "Mozilla" : userAgent;

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
    
    try {
      InputStream inputStream = null;
      if (urlStr.startsWith("http://") || urlStr.startsWith("https://")) {


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
        Tile tile = null;
        if (http != null) {
          inputStream = http.getInputStream();

          Bitmap image = BitmapFactory.decodeStream(inputStream);
          Bitmap tileImage = this.resizeForTile(image);
          tile = new Tile(tileSize, tileSize, bitmapToByteArray(tileImage));
          tileCache.put(cacheKey, tileImage.copy(tileImage.getConfig(), false));
          tileImage.recycle();
          image.recycle();
          http.disconnect();
        }
        inputStream.close();
        return tile;
      }
      
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
  
  public void setOpacity(double opacity) {
    this.tilePaint.setAlpha((int) (opacity * 255));
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
