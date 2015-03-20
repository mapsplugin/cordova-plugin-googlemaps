package plugin.google.maps;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.cordova.CordovaResourceApi;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
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

public class PluginUtil {
  
  public static String getAbsolutePathFromCDVFilePath(CordovaResourceApi resourceApi, String cdvFilePath) {
    if (cdvFilePath.indexOf("cdvfile://") != 0) {
      return null;
    }
    
    //CordovaResourceApi resourceApi = webView.getResourceApi();
    Uri fileURL = resourceApi.remapUri(Uri.parse(cdvFilePath));
    File file = resourceApi.mapUriToFile(fileURL);
    return file.getAbsolutePath();
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

  public static List<LatLng> JSONArray2LatLngList(JSONArray points) throws JSONException  {
    List<LatLng> path = new ArrayList<LatLng>();
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
        } else if (Double.class.isInstance(value)) {
          mBundle.putDouble(key, (Double)value);
        } else if (Integer.class.isInstance(value)) {
          mBundle.putInt(key, (Integer)value);
        } else if (Long.class.isInstance(value)) {
          mBundle.putLong(key, (Long)value);
        } else if (JSONObject.class.isInstance(value)) {
          mBundle.putBundle(key, Json2Bundle((JSONObject)value));
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
}
