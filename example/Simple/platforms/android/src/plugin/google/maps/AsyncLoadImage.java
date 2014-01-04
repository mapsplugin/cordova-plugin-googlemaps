package plugin.google.maps;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

@SuppressWarnings("rawtypes")
public class AsyncLoadImage extends AsyncTask<String, Void, Bitmap> {
  private Object targerClass;
  private final String TAG = "AsyncLoadImage";
  private String targetMethod = "";

  public AsyncLoadImage(Object target, String method) {
    targerClass = target;
    targetMethod = method;
  }

  protected Bitmap doInBackground(String... urls) {
    try {
      URL url= new URL(urls[0]);
      Log.d(TAG, "urls[0]=" + urls[0]);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setDoInput(true);
      connection.connect();
      InputStream input = connection.getInputStream();
      Bitmap myBitmap = BitmapFactory.decodeStream(input);
      return myBitmap;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  protected void onPostExecute(Bitmap image) {
    if (image != null) {
      Log.d(TAG, "image is not null");
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
      Log.d(TAG, "image is null");
      
    }
  }
}
