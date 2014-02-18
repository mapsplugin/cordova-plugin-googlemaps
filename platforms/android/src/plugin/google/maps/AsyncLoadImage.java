package plugin.google.maps;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

@SuppressWarnings("rawtypes")
public class AsyncLoadImage extends AsyncTask<String, Void, Bitmap> {
  private Object targerClass;
  private final String TAG = "AsyncLoadImage";
  private String targetMethod = "";
  private Bundle iconProperty = null;
  private String mUrl = "";
  private HashMap<String, Bitmap> mCache = null;

  public AsyncLoadImage(Object target, String method, Bundle options) {
    targerClass = target;
    targetMethod = method;
    this.iconProperty = options;
  }
  
  public AsyncLoadImage(Object target, String method) {
    targerClass = target;
    targetMethod = method;
  }

  public AsyncLoadImage(Object target, String method, Bundle options, HashMap<String, Bitmap> cache) {
    targerClass = target;
    targetMethod = method;
    this.iconProperty = options;
    mCache = cache;
  }
  
  public AsyncLoadImage(Object target, String method, HashMap<String, Bitmap> cache) {
    targerClass = target;
    targetMethod = method;
    mCache = cache;
  }

  protected Bitmap doInBackground(String... urls) {
    try {
      if (mCache != null && mCache.containsKey(urls[0])) {
        Bitmap myBitmap = mCache.get(urls[0]);
        return myBitmap.copy(Bitmap.Config.ARGB_8888, true);
        //return Bitmap.createBitmap(mCache.get(urls[0]));
      }
      URL url= new URL(urls[0]);
      mUrl = urls[0];
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
        
        // get redirect url from "location" header field
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
      
      InputStream inputStream = http.getInputStream();
      Bitmap myBitmap = BitmapFactory.decodeStream(inputStream);
      if (mCache != null) {
        mCache.put(urls[0], myBitmap.copy(Bitmap.Config.ARGB_8888, true));
      }
      inputStream.close();
      
      return myBitmap;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  protected void onPostExecute(Bitmap image) {
    if (image != null) {
      if (iconProperty != null &&
          iconProperty.containsKey("size") == true) {
          Object size = iconProperty.get("size");
          
        if (Bundle.class.isInstance(size)) {
          
          Bundle sizeInfo = (Bundle)size;
          int width = sizeInfo.getInt("width", 0);
          int height = sizeInfo.getInt("height", 0);
          if (width > 0 && height > 0) {
            image = PluginUtil.resizeBitmap(image, width, height);
          }
        }
      }
      
      image = PluginUtil.scaleBitmapForDevice(image);
      BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(image);
      
      @SuppressWarnings("unchecked")
      Method method;
      try {
        Log.d(TAG, "method=" + targetMethod);
        method = this.targerClass.getClass().getDeclaredMethod(targetMethod, BitmapDescriptor.class);
        method.invoke(this.targerClass, bitmapDescriptor);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      Log.e(TAG, "Unable to load the image: " + mUrl);
      
    }
  }
}
