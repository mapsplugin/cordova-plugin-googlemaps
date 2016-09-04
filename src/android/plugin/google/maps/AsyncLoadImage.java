package plugin.google.maps;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
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

  public AsyncLoadImage(AsyncLoadImageInterface plugin) {
    targetPlugin = plugin;
    privateInit();
  }

  public AsyncLoadImage(int width, int height, AsyncLoadImageInterface plugin) {
    targetPlugin = plugin;
    mWidth = width;
    mHeight = height;
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
  private void addBitmapToMemoryCache(String key, Bitmap image) {
    if (getBitmapFromMemCache(key) == null) {
      mIconCache.put(key, image.copy(image.getConfig(), true));
    }
  }

  private Bitmap getBitmapFromMemCache(String key) {
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
      URL url= new URL(urls[0]);
      String cacheKey = url.hashCode() + "/" + mWidth + "x" + mHeight;
      Bitmap image = getBitmapFromMemCache(cacheKey);
      if (image != null) {
        return image;
      }
      HttpURLConnection http = (HttpURLConnection)url.openConnection(); 
      http.setRequestMethod("GET");
      http.setUseCaches(true);
      http.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
      http.addRequestProperty("User-Agent", "Mozilla");
      http.setInstanceFollowRedirects(true);
      HttpURLConnection.setFollowRedirects(true);
      
      boolean redirect = false;
      // normally, 3xx is redirect
      int status = http.getResponseCode();
      if (status != HttpURLConnection.HTTP_OK) {
        if (status == HttpURLConnection.HTTP_MOVED_TEMP
          || status == HttpURLConnection.HTTP_MOVED_PERM
            || status == HttpURLConnection.HTTP_SEE_OTHER)
        redirect = true;
      }
      if (redirect) {
        
        // get redirect URL from "location" header field
        String newUrl = http.getHeaderField("Location");
     
        // get the cookie if need, for login
        String cookies = http.getHeaderField("Set-Cookie");
     
        // open the new connection again
        http = (HttpURLConnection) new URL(newUrl).openConnection();
        http.setUseCaches(true);
        http.setRequestProperty("Cookie", cookies);
        http.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
        http.addRequestProperty("User-Agent", "Mozilla");
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
      canvas = null;
      imageBytes = null;

      addBitmapToMemoryCache(cacheKey, scaledBitmap);
      
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

  class BitmapCache extends LruCache<String, Bitmap> {

    public BitmapCache(int maxSize) {
      super(maxSize);
    }

    @Override
    protected int sizeOf(String key, Bitmap bitmap) {
      // The cache size will be measured in kilobytes rather than
      // number of items.
      return bitmap.getByteCount() / 1024;
    }

    @Override
    protected void entryRemoved(boolean evicted, String key, Bitmap oldBitmap, Bitmap newBitmap) {
      if (!oldBitmap.isRecycled()) {
        oldBitmap.recycle();
        oldBitmap = null;
      }
    }
  }
}
