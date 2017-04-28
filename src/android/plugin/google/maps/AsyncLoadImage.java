package plugin.google.maps;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;

public class AsyncLoadImage extends AsyncTask<String, Void, Bitmap> {
  private AsyncLoadImageInterface targetPlugin;
  private int mWidth = -1;
  private int mHeight = -1;
  private float density = Resources.getSystem().getDisplayMetrics().density;

  public static BitmapCache mIconCache;
  private String userAgent = null;
  private boolean noCaching = false;

  public AsyncLoadImage(AsyncLoadImageInterface plugin) {
    targetPlugin = plugin;
    privateInit();
  }

  public AsyncLoadImage(String userAgent, int width, int height, boolean noCaching, AsyncLoadImageInterface plugin) {
    targetPlugin = plugin;
    mWidth = width;
    mHeight = height;
    this.userAgent = userAgent == null ? "Mozilla" : userAgent;
    this.noCaching = noCaching;
    privateInit();
  }
  public AsyncLoadImage(String userAgent, int width, int height, AsyncLoadImageInterface plugin) {
    targetPlugin = plugin;
    mWidth = width;
    mHeight = height;
    this.userAgent = userAgent == null ? "Mozilla" : userAgent;
    privateInit();
  }

  private void privateInit() {
    if (mIconCache != null) {
      return;
    }

    // Get max available VM memory, exceeding this amount will throw an
    // OutOfMemory exception. Stored in kilobytes as LruCache takes an
    // int in its constructor.
    int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

    // Use 1/8th of the available memory for this memory cache.
    int cacheSize = maxMemory / 8;

    mIconCache = new BitmapCache(cacheSize);
  }

  public static String getCacheKey(String url, int width, int height) {
    try {
      return getCacheKey(new URL(url), width, height);
    } catch (MalformedURLException e) {
      return null;
    }
  }
  public static String getCacheKey(URL url, int width, int height) {
    return url.hashCode() + "/" + width + "x" + height;
  }

  public static void addBitmapToMemoryCache(String key, Bitmap image) {
    if (getBitmapFromMemCache(key) == null) {
      mIconCache.put(key, image.copy(image.getConfig(), true));
    }
  }

  public static void removeBitmapFromMemCahce(String key) {
    Log.d("removeBitmap", key);
    Bitmap image = mIconCache.remove(key);
    if (image == null || image.isRecycled()) {
      return;
    }
    image.recycle();
  }

  private static Bitmap getBitmapFromMemCache(String key) {
    Bitmap image = mIconCache.get(key);
    if (image == null || image.isRecycled()) {
      return null;
    }

    return image.copy(image.getConfig(), true);
  }


  @Override
  protected void onCancelled(Bitmap bitmap) {
    super.onCancelled(bitmap);

    if (bitmap != null && !bitmap.isRecycled()) {
      bitmap.recycle();
    }
    bitmap = null;
  }


  @SuppressLint("NewApi")
  protected Bitmap doInBackground(String... urls) {
    try {
      URL url = new URL(urls[0]);
      String cacheKey = getCacheKey(url, mWidth, mHeight);
      if (!noCaching) {
        Bitmap image = getBitmapFromMemCache(cacheKey);
        if (image != null) {
          return image;
        }
      }

      boolean redirect = true;
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


      Bitmap myBitmap = null;
      InputStream inputStream = http.getInputStream();

      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      int nRead;
      byte[] data = new byte[16384];
      while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }
      buffer.flush();
      inputStream.close();
      byte[] imageBytes = buffer.toByteArray();

      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = true;
      myBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
      myBitmap.recycle();

      if (mWidth < 1 && mHeight < 1) {
        mWidth = options.outWidth;
        mHeight = options.outHeight;
      }

      // Resize
      int newWidth = (int)(mWidth * density);
      int newHeight = (int)(mHeight * density);


      /**
       * http://stackoverflow.com/questions/4821488/bad-image-quality-after-resizing-scaling-bitmap#7468636
       */
      Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Config.ARGB_8888);

      float ratioX = newWidth / (float) options.outWidth;
      float ratioY = newHeight / (float) options.outHeight;
      float middleX = newWidth / 2.0f;
      float middleY = newHeight / 2.0f;

      options.inJustDecodeBounds = false;
      //options.inSampleSize = (int) Math.max(ratioX, ratioY);
      options.outWidth = newWidth;
      options.outHeight = newHeight;

      Matrix scaleMatrix = new Matrix();
      scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

      Canvas canvas = new Canvas(scaledBitmap);
      canvas.setMatrix(scaleMatrix);

      myBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
      canvas.drawBitmap(myBitmap, middleX - options.outWidth / 2, middleY - options.outHeight / 2, new Paint(Paint.FILTER_BITMAP_FLAG));
      myBitmap.recycle();
      myBitmap = null;
      canvas = null;
      imageBytes = null;

      if (!noCaching) {
        addBitmapToMemoryCache(cacheKey, scaledBitmap);
      }
      return scaledBitmap;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  protected void onPostExecute(Bitmap image) {
    System.gc();
    this.targetPlugin.onPostExecute(image);
  }

}
