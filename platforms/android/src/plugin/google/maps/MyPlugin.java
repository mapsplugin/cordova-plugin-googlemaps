package plugin.google.maps;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.TileOverlay;

public class MyPlugin extends CordovaPlugin implements MyPluginInterface  {
  protected HashMap<String, Object> objects;
  private final String TAG = "GoogleMapsPlugin";

  public GoogleMaps mapCtrl = null;
  public GoogleMap map = null;
  
  public void setMapCtrl(GoogleMaps mapCtrl) {
    this.mapCtrl = mapCtrl;
    this.map = mapCtrl.map;
  }
  
  @SuppressLint("UseSparseArrays")
  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    this.objects = new HashMap<String, Object>();
  }
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    String[] params = args.getString(0).split("\\.");
    try {
      Method method = this.getClass().getDeclaredMethod(params[1], JSONArray.class, CallbackContext.class);
      method.setAccessible(true);
      method.invoke(this, args, callbackContext);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
      return false;
    }
  }
  
  protected Circle getCircle(String id) {
    return (Circle)this.objects.get(id);
  }


  protected GroundOverlay getGroundOverlay(String id) {
    return (GroundOverlay)this.objects.get(id);
  }

  protected Marker getMarker(String id) {
    return (Marker)this.objects.get(id);
  }
  protected Polyline getPolyline(String id) {
    return (Polyline)this.objects.get(id);
  }
  protected Polygon getPolygon(String id) {
    return (Polygon)this.objects.get(id);
  }
  protected TileOverlay getTileOverlay(String id) {
    return (TileOverlay)this.objects.get(id);
  }

}
