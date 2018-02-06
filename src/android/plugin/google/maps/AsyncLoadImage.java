package plugin.google.maps;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaWebView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class AsyncLoadImage extends AsyncTask<Void, Void, AsyncLoadImage.AsyncLoadImageResult> {
  private AsyncLoadImageInterface callback;
  private float density = Resources.getSystem().getDisplayMetrics().density;
  private AsyncLoadImageOptions mOptions;
  private String userAgent;
  private String currentPageUrl;

  // Get max available VM memory, exceeding this amount will throw an
  // OutOfMemory exception.
  static int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
  // Use 1/8th of the available memory for this memory cache.
  public static BitmapCache mIconCache = new BitmapCache(maxMemory / 8);

  private final String TAG = "AsyncLoadImage";
  private CordovaWebView webView;
  private CordovaInterface cordova;

  public static class AsyncLoadImageOptions {
    String url;
    int width;
    int height;
    boolean noCaching;
  }

  public static class AsyncLoadImageResult {
    Bitmap image;
    boolean cacheHit;
    String cacheKey;
  }

  public AsyncLoadImage(CordovaInterface cordova, CordovaWebView webView, AsyncLoadImageOptions options, AsyncLoadImageInterface callback) {
    this.callback = callback;
    this.mOptions = options;
    this.webView = webView;
    this.cordova = cordova;
  }

  public static String getCacheKey(String url, int width, int height) {
    if (url == null) {
      return null;
    }
    try {
      return getCacheKey(new URL(url), width, height);
    } catch (MalformedURLException e) {
      return url.hashCode() + "/" + width + "x" + height;
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
    Bitmap image = mIconCache.remove(key);
    if (image == null || image.isRecycled()) {
      return;
    }
    image.recycle();
  }

  public static Bitmap getBitmapFromMemCache(String key) {
    Bitmap image = mIconCache.get(key);
    if (image == null || image.isRecycled()) {
      return null;
    }

    return image.copy(image.getConfig(), true);
  }


  @Override
  protected void onCancelled(AsyncLoadImageResult result) {
    super.onCancelled(result);
    if (result == null) {
      return;
    }

    if (!result.image.isRecycled()) {
      result.image.recycle();
    }
    result.image = null;
  }

  @Override
  protected void onPreExecute() {
    super.onPreExecute();

    //----------------------------------------------------------------
    // If icon url contains "cdvfile://", convert to physical path.
    //----------------------------------------------------------------
    if (mOptions.url.indexOf("cdvfile://") == 0) {
      CordovaResourceApi resourceApi = webView.getResourceApi();
      mOptions.url = PluginUtil.getAbsolutePathFromCDVFilePath(resourceApi, mOptions.url);
    }

    this.currentPageUrl = CordovaGoogleMaps.CURRENT_URL; //webView.getUrl();
    //Log.d(TAG, "-->currentPageUrl = " + this.currentPageUrl);

    //View browserView = webView.getView();
    //String browserViewName = browserView.getClass().getName();
    this.userAgent = "Mozilla";

    /*
    if("org.xwalk.core.XWalkView".equals(browserViewName) ||
        "org.crosswalk.engine.XWalkCordovaView".equals(browserViewName)) {

      CordovaPreferences preferences = webView.getPreferences();
      // Set xwalk webview settings by Cordova preferences.
      String xwalkUserAgent = preferences == null ? "" : preferences.getString("xwalkUserAgent", "");
      if (!xwalkUserAgent.isEmpty()) {
        this.userAgent = xwalkUserAgent;
      }

      String appendUserAgent = preferences.getString("AppendUserAgent", "");
      if (!appendUserAgent.isEmpty()) {
        this.userAgent = this.userAgent + " " + appendUserAgent;
      }
      if ("".equals(this.userAgent)) {
        this.userAgent = "Mozilla";
      }
    } else {
      this.userAgent = ((WebView) webView.getEngine().getView()).getSettings().getUserAgentString();
    }
    */

  }

  protected AsyncLoadImageResult doInBackground(Void... params) {

    int mWidth = mOptions.width;
    int mHeight = mOptions.height;
    String iconUrl = mOptions.url;
    String orgIconUrl = iconUrl;
    Bitmap image = null;

    if (iconUrl == null) {
      return null;
    }

    String cacheKey = null;
    cacheKey = getCacheKey(orgIconUrl, mWidth, mHeight);

    image = getBitmapFromMemCache(cacheKey);
    if (image != null) {
      AsyncLoadImageResult result = new AsyncLoadImageResult();
      result.image = image;
      result.cacheHit = true;
      result.cacheKey = cacheKey;
      return result;
    }

    //Log.d(TAG, "--> iconUrl = " + iconUrl);
    //--------------------------------
    // Load image from local path
    //--------------------------------
    if (!iconUrl.startsWith("data:image")) {
      if (!iconUrl.contains("://") &&
          !iconUrl.startsWith("/") &&
          !iconUrl.startsWith("www/") &&
          !iconUrl.startsWith("data:image") &&
          !iconUrl.startsWith("./") &&
          !iconUrl.startsWith("../")) {
        iconUrl = "./" + iconUrl;
        //Log.d(TAG, "--> iconUrl = " + iconUrl);
      }

      if (iconUrl.startsWith("./") || iconUrl.startsWith("../")) {
        iconUrl = iconUrl.replace("(\\.\\/)+", "./");
        String currentPage = this.currentPageUrl;
        currentPage = currentPage.replaceAll("[^\\/]*$", "");
        currentPage = currentPage.replaceAll("#.*$", "");
        currentPage = currentPage.replaceAll("\\/[^\\/]+\\.[^\\/]+$", "");
        if (!currentPage.endsWith("/")) {
          currentPage = currentPage + "/";
        }
        iconUrl = currentPage + iconUrl;
        iconUrl = iconUrl.replaceAll("(\\/\\.\\/+)+", "/");
        //Log.d(TAG, "--> iconUrl = " + iconUrl);
      }

      if (iconUrl.indexOf("file://") == 0 &&
          !iconUrl.contains("file:///android_asset/")) {
        iconUrl = iconUrl.replace("file://", "");
      } else {
        //Log.d(TAG, "--> iconUrl = " + iconUrl);

//        if (iconUrl.indexOf("file:///android_asset/") == 0) {
//          iconUrl = iconUrl.replace("file:///android_asset/", "");
//        }

        //Log.d(TAG, "iconUrl(222) = " + iconUrl);
        if (iconUrl.contains("./")) {
          try {
            boolean isAbsolutePath = iconUrl.startsWith("/");
            File relativePath = new File(iconUrl);
            iconUrl = relativePath.getCanonicalPath();
            //Log.d(TAG, "iconUrl = " + iconUrl);
            if (!isAbsolutePath) {
              iconUrl = iconUrl.substring(1);
            }
            Log.d(TAG, "iconUrl(232) = " + iconUrl);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    }

    cacheKey = getCacheKey(iconUrl, mWidth, mHeight);

    image = getBitmapFromMemCache(cacheKey);
    if (image != null) {
      AsyncLoadImageResult result = new AsyncLoadImageResult();
      result.image = image;
      result.cacheHit = true;
      result.cacheKey = cacheKey;
      return result;
    }

    cacheKey = getCacheKey(orgIconUrl, mWidth, mHeight);

    if (iconUrl.indexOf("http") == 0) {
      //--------------------------------
      // Load image from the Internet
      //--------------------------------
      try {
        URL url = new URL(iconUrl);

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
          http.addRequestProperty("User-Agent", this.userAgent);
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
            continue;
          }
          if (status == HttpURLConnection.HTTP_OK) {
            break;
          } else {
            return null;
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
        options.inPreferredConfig = Config.ARGB_8888;

        // The below line just checking the bitmap size (width,height).
        // Returned value is always null.
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);

        if (mWidth < 1 && mHeight < 1) {
          mWidth = options.outWidth;
          mHeight = options.outHeight;
        }

        // Resize
        int newWidth = (int)(mWidth * density);
        int newHeight = (int)(mHeight * density);
        if (newWidth > 2000 || newHeight > 2000) {
          float rationResize;
          if (newWidth >=  newHeight) {
            rationResize = 2000.0f / ((float) newWidth);
          } else {
            rationResize = 2000.0f / ((float) newHeight);
          }
          newWidth = (int)(((float)newWidth) * rationResize);
          newHeight = (int)(((float)newHeight) * rationResize);
          Log.w(TAG, "Since the image size is too large, the image size resizes down mandatory");
        }


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

        AsyncLoadImageResult result = new AsyncLoadImageResult();
        result.image = scaledBitmap;
        result.cacheHit = false;
        if (!mOptions.noCaching) {
          result.cacheKey = cacheKey;
          addBitmapToMemoryCache(cacheKey, scaledBitmap);
        }

        return result;
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }


    } else {
      //Log.d(TAG, "--> iconUrl = " + iconUrl);
      if (iconUrl.indexOf("data:image/") == 0 && iconUrl.contains(";base64,")) {
        String[] tmp = iconUrl.split(",");
        image = PluginUtil.getBitmapFromBase64encodedImage(tmp[1]);
      } else {
        try {
          InputStream inputStream = null;
          if (iconUrl.startsWith("file:/android_asset/")) {
            AssetManager assetManager = cordova.getActivity().getAssets();
            iconUrl = iconUrl.replace("file:/android_asset/", "");
            inputStream = assetManager.open(iconUrl);
            //Log.d(TAG, "--> iconUrl = " + iconUrl);
          } else if (iconUrl.startsWith("file:///android_asset/")) {
            AssetManager assetManager = cordova.getActivity().getAssets();
            iconUrl = iconUrl.replace("file:///android_asset/", "");
            inputStream = assetManager.open(iconUrl);
            //Log.d(TAG, "--> iconUrl = " + iconUrl);
          } else if (iconUrl.startsWith("/")) {
            File file = new File(iconUrl);
            inputStream = new FileInputStream(file);
          }
          if (inputStream != null) {
            image = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
          } else {
            Log.e(TAG, "Can not load the file from '" + iconUrl + "'");
            return null;
          }
        } catch (IOException e) {
          e.printStackTrace();
          return null;
        }
      }
      /*
      //--------------------------------
      // Load image from local path
      //--------------------------------
      if (!iconUrl.contains("://") &&
          !iconUrl.startsWith("/") &&
          !iconUrl.startsWith("www/") &&
          !iconUrl.startsWith("data:image") &&
          !iconUrl.startsWith("./") &&
          !iconUrl.startsWith("../")) {
        iconUrl = "./" + iconUrl;
        Log.d(TAG, "--> iconUrl = " + iconUrl);
      }

      if (iconUrl.startsWith("./") || iconUrl.startsWith("../")) {
        iconUrl = iconUrl.replace("(\\.\\/)+", "./");
        String currentPage = this.currentPageUrl;
        currentPage = currentPage.replaceAll("[^\\/]*$", "");
        currentPage = currentPage.replaceAll("#.*$", "");
        currentPage = currentPage.replaceAll("\\/[^\\/]+\\.[^\\/]+$", "");
        if (!currentPage.endsWith("/")) {
          currentPage = currentPage + "/";
        }
        iconUrl = currentPage + iconUrl;
        iconUrl = iconUrl.replaceAll("(\\/\\.\\/+)+", "/");
        Log.d(TAG, "--> iconUrl = " + iconUrl);
      }

      if (iconUrl.indexOf("data:image/") == 0 && iconUrl.contains(";base64,")) {
        cacheKey = getCacheKey(iconUrl, mWidth, mHeight);

        image = getBitmapFromMemCache(cacheKey);
        if (image != null) {
          AsyncLoadImageResult result = new AsyncLoadImageResult();
          result.image = image;
          result.cacheHit = true;
          result.cacheKey = cacheKey;
          return result;
        }

        String[] tmp = iconUrl.split(",");
        image = PluginUtil.getBitmapFromBase64encodedImage(tmp[1]);
      } else if (iconUrl.indexOf("file://") == 0 &&
          !iconUrl.contains("file:///android_asset/")) {
        iconUrl = iconUrl.replace("file://", "");
        File tmp = new File(iconUrl);
        if (tmp.exists()) {
          cacheKey = getCacheKey(iconUrl, mWidth, mHeight);

          image = getBitmapFromMemCache(cacheKey);
          if (image != null) {
            AsyncLoadImageResult result = new AsyncLoadImageResult();
            result.image = image;
            result.cacheHit = true;
            result.cacheKey = cacheKey;
            return result;
          }

          image = BitmapFactory.decodeFile(iconUrl);
        } else {
          //if (PluginMarker.this.mapCtrl.mPluginLayout.isDebug) {
          Log.w(TAG, "icon is not found (" + iconUrl + ")");
          //}
          return null;
        }
      } else {
        Log.d(TAG, "--> iconUrl = " + iconUrl);
        cacheKey = getCacheKey(iconUrl, mWidth, mHeight);
        image = getBitmapFromMemCache(cacheKey);

        if (image != null) {
          AsyncLoadImageResult result = new AsyncLoadImageResult();
          result.image = image;
          result.cacheHit = true;
          result.cacheKey = cacheKey;
          return result;
        }

        Log.d(TAG, "iconUrl = " + iconUrl);
        if (iconUrl.indexOf("file:///android_asset/") == 0) {
          iconUrl = iconUrl.replace("file:///android_asset/", "");
        }

        Log.d(TAG, "iconUrl = " + iconUrl);
        if (iconUrl.contains("./")) {
          try {
            boolean isAbsolutePath = iconUrl.startsWith("/");
            File relativePath = new File(iconUrl);
            iconUrl = relativePath.getCanonicalPath();
            Log.d(TAG, "iconUrl = " + iconUrl);
            if (!isAbsolutePath) {
              iconUrl = iconUrl.substring(1);
            }
            Log.d(TAG, "iconUrl = " + iconUrl);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        */

      //}

      if (mWidth > 0 && mHeight > 0) {
        mWidth = Math.round(mWidth * density);
        mHeight = Math.round(mHeight * density);
        image = PluginUtil.resizeBitmap(image, mWidth, mHeight);
      } else {
        image = PluginUtil.scaleBitmapForDevice(image);
      }

      AsyncLoadImageResult result = new AsyncLoadImageResult();
      result.image = image;
      result.cacheHit = false;
      if (!mOptions.noCaching) {
        result.cacheKey = cacheKey;
        addBitmapToMemoryCache(cacheKey, image);
      }

      return result;
    }
  }


  @Override
  protected void onPostExecute(AsyncLoadImageResult result) {
    this.callback.onPostExecute(result);
  }

}
