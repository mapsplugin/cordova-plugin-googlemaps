package plugin.google.maps;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class AsyncGetJsonWithURL extends AsyncTask<HashMap<String, String>, Void, JSONObject> {
  private AsyncHttpGetInterface callback;

  private final String TAG = "AsyncHttpGet";
  private String BASE_URI = "";

  public AsyncGetJsonWithURL(String baseUri, AsyncHttpGetInterface callback) {
    this.callback = callback;
    this.BASE_URI = baseUri;
  }

  protected JSONObject doInBackground(HashMap<String, String>... params) {

    //--------------------------------
    // Load image from the Internet
    //--------------------------------
    try {
      Uri.Builder builder = Uri.parse(BASE_URI).buildUpon();
      for (String key: params[0].keySet()) {
        builder.appendQueryParameter(key, params[0].get(key));
      }


      String urlStr = builder.build().toString();
//      Log.d("Elevation", "url=" + urlStr);
      URL url = new URL(urlStr);

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
        http.addRequestProperty("User-Agent", "Mozilla");
        http.setInstanceFollowRedirects(true);
        HttpURLConnection.setFollowRedirects(true);

        // normally, 3xx is redirect
        try {
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
            continue;
          }
          if (status == HttpURLConnection.HTTP_OK) {
            break;
          } else {
            return null;
          }
        } catch (Exception e) {
          Log.e(TAG, "can not connect to " + url.toString(), e);
        }
      }

      InputStream inputStream = http.getInputStream();

      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      int nRead;
      byte[] data = new byte[16384];
      while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }
      buffer.flush();
      inputStream.close();
      return new JSONObject(buffer.toString());
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }

  }


  @Override
  protected void onPostExecute(JSONObject result) {
    this.callback.onPostExecute(result);
  }

}
