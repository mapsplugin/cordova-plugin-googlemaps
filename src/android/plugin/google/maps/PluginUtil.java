package plugin.google.maps;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.util.Base64;

import com.google.android.gms.maps.model.IndoorBuilding;
import com.google.android.gms.maps.model.IndoorLevel;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PluginUtil {
  // Get resource id
  // http://stackoverflow.com/a/37840674
  public static int getAppResource(Activity activity, String name, String type) {
    return activity.getResources().getIdentifier(name, type, activity.getPackageName());
  }

  public static abstract class MyCallbackContext extends CallbackContext {

    public MyCallbackContext(String callbackId, CordovaWebView webView) {
      super(callbackId, webView);
    }
    @Override
    public void sendPluginResult(PluginResult pluginResult) {
      this.onResult(pluginResult);
    }

    abstract public void onResult(PluginResult pluginResult);
  }

  public static boolean isNumeric(String str)
  {
    for (char c : str.toCharArray()) {
      if (!Character.isDigit(c)) {
        return false;
      }
    }
    return true;
  }

  public static LatLngBounds getBoundsFromCircle(LatLng center, double radius) {
    double d2r = Math.PI / 180;   // degrees to radians
    double r2d = 180 / Math.PI;   // radians to degrees
    double earthsradius = 3963.189; // 3963 is the radius of the earth in miles
    radius *= 0.000621371192; // convert to mile

    // find the raidus in lat/lon
    double rlat = (radius / earthsradius) * r2d;
    double rlng = rlat / Math.cos(center.latitude * d2r);

    LatLngBounds.Builder builder = new LatLngBounds.Builder();
    double ex, ey;
    for (int i = 0; i < 360; i+=90) {
      ey = center.longitude + (rlng * Math.cos(i * d2r)); // center a + radius x * cos(theta)
      ex = center.latitude + (rlat * Math.sin(i * d2r)); // center b + radius y * sin(theta)
      builder.include(new LatLng(ex, ey));
    }
    return builder.build();
  }

  public static LatLngBounds getBoundsFromPath(List<LatLng> path) {
    LatLngBounds.Builder builder = new LatLngBounds.Builder();

    for (LatLng aPath : path) {
      builder.include(aPath);
    }
    return builder.build();
  }

  public static String getAbsolutePathFromCDVFilePath(CordovaResourceApi resourceApi, String cdvFilePath) {
    if (cdvFilePath.indexOf("cdvfile://") != 0) {
      return null;
    }

    //CordovaResourceApi resourceApi = webView.getResourceApi();
    Uri fileURL = resourceApi.remapUri(Uri.parse(cdvFilePath));
    File file = resourceApi.mapUriToFile(fileURL);
    if (file == null) {
      return null;
    }

    try {
      return file.getCanonicalPath();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  public static JSONObject location2Json(Location location) throws JSONException {
    JSONObject latLng = new JSONObject();
    latLng.put("lat", location.getLatitude());
    latLng.put("lng", location.getLongitude());

    JSONObject params = new JSONObject();
    params.put("latLng", latLng);

    if (VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      params.put("elapsedRealtimeNanos", location.getElapsedRealtimeNanos());
    } else {
      params.put("elapsedRealtimeNanos", 0);
    }
    params.put("time", location.getTime());
    /*
    SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    Date date = new Date(location.getTime());
    params.put("timeFormatted", format.format(date));
    */
    if (location.hasAccuracy()) {
      params.put("accuracy", location.getAccuracy());
    }
    if (location.hasBearing()) {
      params.put("bearing", location.getBearing());
    }
    if (location.hasAltitude()) {
      params.put("altitude", location.getAltitude());
    }
    if (location.hasSpeed()) {
      params.put("speed", location.getSpeed());
    }
    params.put("provider", location.getProvider());
    params.put("hashCode", location.hashCode());
    return params;
  }

  /**
   * return color integer value
   * @param arrayRGBA
   * @throws JSONException
   */
  public static int parsePluginColor(JSONArray arrayRGBA) throws JSONException {
    return Color.argb(arrayRGBA.getInt(3), arrayRGBA.getInt(0), arrayRGBA.getInt(1), arrayRGBA.getInt(2));
  }

  public static ArrayList<LatLng> JSONArray2LatLngList(JSONArray points) throws JSONException  {
    ArrayList<LatLng> path = new ArrayList<LatLng>();
    JSONObject pointJSON;
    int i = 0;
    for (i = 0; i < points.length(); i++) {
      pointJSON = points.getJSONObject(i);
      path.add(new LatLng(pointJSON.getDouble("lat"), pointJSON.getDouble("lng")));
    }
    return path;
  }
  /*
  public static Set<LatLng> JSONArray2LatLngHash(JSONArray points) throws JSONException  {
    Set<LatLng> path = new HashSet<LatLng>();
    JSONObject pointJSON;
    int i = 0;
    for (i = 0; i < points.length(); i++) {
      pointJSON = points.getJSONObject(i);
      path.add(new LatLng(pointJSON.getDouble("lat"), pointJSON.getDouble("lng")));
    }
    return path;
  }
  */
  public static LatLngBounds JSONArray2LatLngBounds(JSONArray points) throws JSONException {
    List<LatLng> path = JSONArray2LatLngList(points);
    Builder builder = LatLngBounds.builder();
    int i = 0;
    for (i = 0; i < path.size(); i++) {
      builder.include(path.get(i));
    }
    return builder.build();
  }

  public static Bundle Json2Bundle(JSONObject json) {
    Bundle mBundle = new Bundle();
    @SuppressWarnings("unchecked")
    Iterator<String> iter = json.keys();
    Object value;
    while (iter.hasNext()) {
      String key = iter.next();
      try {
        value = json.get(key);
        if (Boolean.class.isInstance(value)) {
          mBundle.putBoolean(key, (Boolean)value);
        } else if (Integer.class.isInstance(value)) {
          mBundle.putInt(key, (Integer)value);
        } else if (Long.class.isInstance(value)) {
          mBundle.putLong(key, (Long)value);
        } else if (Double.class.isInstance(value)) {
          mBundle.putDouble(key, (Double)value);
        } else if (JSONObject.class.isInstance(value)) {
          mBundle.putBundle(key, Json2Bundle((JSONObject)value));
        } else if (JSONArray.class.isInstance(value)) {
          JSONArray values = (JSONArray)value;
          ArrayList<String> strings = new ArrayList<String>();
          for (int i = 0; i < values.length(); i++) {
            strings.add(values.get(i) + "");
          }
          mBundle.putStringArrayList(key, strings);
        } else {
          mBundle.putString(key, json.getString(key));
        }
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
    return mBundle;
  }

  public static Bitmap resizeBitmap(Bitmap bitmap, int newWidth, int newHeight) {
    if (bitmap == null) {
      return null;
    }
    /**
     * http://stackoverflow.com/questions/4821488/bad-image-quality-after-resizing-scaling-bitmap#7468636
     */
    Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Config.ARGB_8888);

    float ratioX = newWidth / (float) bitmap.getWidth();
    float ratioY = newHeight / (float) bitmap.getHeight();
    float middleX = newWidth / 2.0f;
    float middleY = newHeight / 2.0f;

    Matrix scaleMatrix = new Matrix();
    scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

    Canvas canvas = new Canvas(scaledBitmap);
    canvas.setMatrix(scaleMatrix);
    canvas.drawBitmap(bitmap, middleX - bitmap.getWidth() / 2, middleY - bitmap.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));
    bitmap.recycle();

    return scaledBitmap;
  }

  public static Bitmap scaleBitmapForDevice(Bitmap bitmap) {
    if (bitmap == null) {
      return null;
    }

    float density = Resources.getSystem().getDisplayMetrics().density;
    int newWidth = (int)(bitmap.getWidth() * density);
    int newHeight = (int)(bitmap.getHeight() * density);
    /*
    Bitmap resizeBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
    */
    /**
     * http://stackoverflow.com/questions/4821488/bad-image-quality-after-resizing-scaling-bitmap#7468636
     */
    Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Config.ARGB_8888);

    float ratioX = newWidth / (float) bitmap.getWidth();
    float ratioY = newHeight / (float) bitmap.getHeight();
    float middleX = newWidth / 2.0f;
    float middleY = newHeight / 2.0f;

    Matrix scaleMatrix = new Matrix();
    scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

    Canvas canvas = new Canvas(scaledBitmap);
    canvas.setMatrix(scaleMatrix);
    canvas.drawBitmap(bitmap, middleX - bitmap.getWidth() / 2, middleY - bitmap.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));
    bitmap.recycle();

    return scaledBitmap;
  }

  public static Bitmap getBitmapFromBase64encodedImage(String base64EncodedImage) {
    byte[] byteArray= Base64.decode(base64EncodedImage, Base64.DEFAULT);
    Bitmap image= null;
    try {
      image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return image;
  }


  public static JSONObject Bundle2Json(Bundle bundle) {
    JSONObject json = new JSONObject();
    Set<String> keys = bundle.keySet();
    Iterator<String> iter = keys.iterator();
    while (iter.hasNext()) {
      String key = iter.next();
      try {
        Object value = bundle.get(key);
        if (Bundle.class.isInstance(value)) {
          value = Bundle2Json((Bundle)value);
        }
        if (value.getClass().isArray()) {
          JSONArray values = new JSONArray();
          Object[] objects = (Object[])value;
          int i = 0;
          for (i = 0; i < objects.length; i++) {
            if (Bundle.class.isInstance(objects[i])) {
              objects[i] = Bundle2Json((Bundle)objects[i]);
            }
            values.put(objects[i]);
          }
          json.put(key, values);
        } else if (value.getClass() == ArrayList.class) {
          JSONArray values = new JSONArray();
          Iterator<?> listIterator = ((ArrayList<?>)value).iterator();
          while(listIterator.hasNext()) {
            value = listIterator.next();
            if (Bundle.class.isInstance(value)) {
              value = Bundle2Json((Bundle)value);
            }
            values.put(value);
          }
          json.put(key, values);
        } else {
          json.put(key, value);
        }
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
    return json;
  }

  public static  LatLngBounds convertToLatLngBounds(List<LatLng> points) {
    LatLngBounds.Builder latLngBuilder = LatLngBounds.builder();
    Iterator<LatLng> iterator = points.listIterator();
    while (iterator.hasNext()) {
      latLngBuilder.include(iterator.next());
    }
    return latLngBuilder.build();
  }


  public static JSONObject convertIndoorBuildingToJson(IndoorBuilding indoorBuilding) {
    if (indoorBuilding == null) {
      return null;
    }
    JSONObject result = new JSONObject();
    try {
      JSONArray levels = new JSONArray();
      for(IndoorLevel level : indoorBuilding.getLevels()){
        JSONObject levelInfo = new JSONObject();
          levelInfo.put("name",level.getName());

          // TODO Auto-generated catch block
        levelInfo.put("shortName",level.getShortName());
        levels.put(levelInfo);
      }
      result.put("activeLevelIndex",indoorBuilding.getActiveLevelIndex());
      result.put("defaultLevelIndex",indoorBuilding.getDefaultLevelIndex());
      result.put("levels",levels);
      result.put("underground",indoorBuilding.isUnderground());
    } catch (JSONException e) {
      e.printStackTrace();
      return null;
    }
    return result;
  }

  public static ArrayList<File> unpackZipFromBytes(InputStream zipped, String dstPath)
  {
    ArrayList<File> files = new ArrayList<File>();
    try {
      String filename;
      ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(zipped));
      ZipEntry zipEntry;
      byte[] buffer = new byte[1024];
      int count;

      while ((zipEntry = zipInputStream.getNextEntry()) != null)
      {
        filename = zipEntry.getName();

        if (zipEntry.isDirectory()) {
          File directory = new File(dstPath + "/" + filename);
          files.add(directory);
          directory.mkdirs();
          continue;
        }

        files.add(new File(dstPath + "/" + filename));
        FileOutputStream fileOutputStream = new FileOutputStream(dstPath + "/" + filename);

        while ((count = zipInputStream.read(buffer)) != -1) {
          fileOutputStream.write(buffer, 0, count);
        }

        fileOutputStream.close();
        zipInputStream.closeEntry();
      }

      zipInputStream.close();
      zipped.close();

    } catch(IOException e) {

      e.printStackTrace();
    }

    return files;
  }

  public static String getPgmStrings(Activity activity, String key) {
    int resId = PluginUtil.getAppResource(activity, key, "string");
    Resources res = activity.getResources();
    return res.getString(resId);
  }

  public static String encodePath(List<LatLng> path) {
    double plat = 0;
    double plng = 0;
    StringBuilder builder = new StringBuilder();

    for (LatLng location: path) {
      builder.append(_encodePoint(plat, plng, location.latitude, location.longitude));

      plat = location.latitude;
      plng = location.longitude;
    }

    return builder.toString();
  }

  private static String _encodePoint(double plat, double plng, double lat, double lng) {
    long late5 = Math.round(lat * 1e5);
    long plate5 = Math.round(plat * 1e5);

    long lnge5 = Math.round(lng * 1e5);
    long plnge5 = Math.round(plng * 1e5);

    long dlng = lnge5 - plnge5;
    long dlat = late5 - plate5;

    return _encodeSignedNumber(dlat) + _encodeSignedNumber(dlng);
  }
  private static String _encodeSignedNumber(long num) {
    long sgn_num = num << 1;

    if (num < 0) {
      sgn_num = ~(sgn_num);
    }

    return (_encodeNumber(sgn_num));
  }


  private static String _encodeNumber(long num) {
    StringBuilder builder = new StringBuilder();

    while (num >= 0x20) {
      builder.append(Character.toChars((int) (0x20 | (num & 0x1f)) + 63));
      num >>= 5;
    }

    builder.append(Character.toChars((int) num + 63));
    return builder.toString();
  }
}
