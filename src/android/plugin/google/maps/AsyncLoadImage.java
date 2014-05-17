package plugin.google.maps;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

public class AsyncLoadImage extends AsyncTask<String, Void, Bitmap> {
  private HashMap<String, Bitmap> mCache = null;
  private AsyncLoadImageInterface targetPlugin;

  public AsyncLoadImage(AsyncLoadImageInterface plugin) {
    targetPlugin = plugin;
  }
  
  public AsyncLoadImage(AsyncLoadImageInterface plugin, HashMap<String, Bitmap> cache) {
    mCache = cache;
    targetPlugin = plugin;
  }
  
  protected Bitmap doInBackground(String... urls) {
    try {
      if (mCache != null && mCache.containsKey(urls[0])) {
        Bitmap myBitmap = mCache.get(urls[0]);
        return myBitmap.copy(Bitmap.Config.ARGB_8888, true);
        //return Bitmap.createBitmap(mCache.get(urls[0]));
      }
      URL url= new URL(urls[0]);
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
      image = PluginUtil.scaleBitmapForDevice(image);
    }
    this.targetPlugin.onPostExecute(image);
  }
}
