package plugin.google.maps;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginEntry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class GoogleMaps extends CordovaPlugin implements View.OnClickListener {
  private final String TAG = "GoogleMapsPlugin";
  private final HashMap<String, PluginEntry> plugins = new HashMap<String, PluginEntry>();
  
  private enum METHODS {
    getMap,
    showMap,
    exec
  }
  
  public MapView mapView = null;
  public GoogleMap map = null;
  private Activity activity;
  private FrameLayout baseLayer;
  private ViewGroup root;

  private JavaScriptInterface jsInterface;
  private MarkerManager markerManager;
  private CircleManager circleManager;

  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
    activity = cordova.getActivity();
    markerManager = new MarkerManager(cordova);
    circleManager = new CircleManager(cordova);
    jsInterface = new JavaScriptInterface(activity);
    Runnable runnable = new Runnable() {
      public void run() {
        webView.addJavascriptInterface(jsInterface, "jsInterface");
      }
    };
    cordova.getActivity().runOnUiThread(runnable);
  }

  @Override
  public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Log.d("CordovaLog", "action=" + action);
/*
    activity = cordova.getActivity();
    if (action.equals("GoogleMap_getMap") == false
        && action.equals("getLicenseInfo") == false && this.map == null) {
      Log.d("CordovaLog", "map is Null(" + action + ")");
      callbackContext.error("Map is null");
      return false;
    }
    */

    Runnable runnable = new Runnable() {
      public void run() {
        switch(METHODS.valueOf(action)) {
        case getMap:
          GoogleMaps.this.getMap(args, callbackContext);
          break;
        case showMap:
          GoogleMaps.this.showMap(args, callbackContext);
          break;
        case exec:
          try {
            String classMethod = args.getString(0);
            String[] params = classMethod.split("\\.", 0);
            Log.d(TAG, "param[0] = " + params[0]);
            
            PluginEntry entry = GoogleMaps.this.plugins.get(params[0]);
            if (params.length == 2 && entry != null) { 
              entry.plugin.execute("execute", args, callbackContext);
            }
            
            callbackContext.success();
          } catch (Exception e) {
            e.printStackTrace();
            callbackContext.error(action + "is not defined.");
          }
          break;
        
        default:
          callbackContext.error(action + "is not defined.");
          break;
        }
      }
    };
    cordova.getActivity().runOnUiThread(runnable);
    
    /*
    if (action.equals("getMap")) {
      
      PluginEntry entry = this.plugins.get("Map");
      entry.plugin.execute(action, args, callbackContext);
    }
    */
    
    
    return true;
  }


  private Boolean setTilt(final JSONArray args,
      final CallbackContext callbackContext) {
    Runnable runnable = new Runnable() {
      public void run() {
        float tilt = -1;
        try {
          tilt = (float) args.getDouble(0);

          if (tilt > 0 && tilt <= 90) {
            CameraPosition currentPos = map.getCameraPosition();
            CameraPosition newPosition = new CameraPosition.Builder()
                .target(currentPos.target).bearing(currentPos.bearing)
                .zoom(currentPos.zoom).tilt(tilt).build();
            myMoveCamera(newPosition, callbackContext);
          } else {
            callbackContext.error("Invalid tilt angle(" + tilt + ")");
          }

        } catch (Exception e) {
          e.printStackTrace();
          callbackContext.error(e.getMessage());
        }
        ;

      }
    };
    cordova.getActivity().runOnUiThread(runnable);
    return true;
  }

  private Boolean updateCameraPosition(final String action,
      final JSONArray args, final CallbackContext callbackContext) {
    Runnable runnable = new Runnable() {
      public void run() {
        try {
          float tilt, bearing, zoom;
          LatLng target;
          int durationMS = 0;
          JSONObject cameraPos = args.getJSONObject(0);
          CameraPosition currentPos = map.getCameraPosition();
          if (cameraPos.has("tilt")) {
            tilt = (float) cameraPos.getDouble("tilt");
          } else {
            tilt = currentPos.tilt;
          }
          if (cameraPos.has("bearing")) {
            bearing = (float) cameraPos.getDouble("bearing");
          } else {
            bearing = currentPos.bearing;
          }
          if (cameraPos.has("zoom")) {
            zoom = (float) cameraPos.getDouble("zoom");
          } else {
            zoom = currentPos.zoom;
          }
          if (cameraPos.has("lat") && cameraPos.has("lng")) {
            target = new LatLng(cameraPos.getDouble("lat"),
                cameraPos.getDouble("lng"));
          } else {
            target = currentPos.target;
          }

          CameraPosition newPosition = new CameraPosition.Builder()
              .target(target).bearing(bearing).zoom(zoom).tilt(tilt).build();

          if (args.length() == 2) {
            durationMS = args.getInt(1);
          }
          if (action.equals("moveCamera")) {
            myMoveCamera(newPosition, callbackContext);
          } else {
            myAnimateCamera(newPosition, durationMS, callbackContext);
          }
        } catch (Exception e) {
          e.printStackTrace();
          callbackContext.error(e.getMessage());
        }
        ;

      }
    };
    cordova.getActivity().runOnUiThread(runnable);
    return true;
  }

  private void getMap(JSONArray args, final CallbackContext callbackContext) {
    if (map != null) {
      callbackContext.success();
      return;
    }
    // ------------------------------
    // Check of Google Play Services
    // ------------------------------
    int checkGooglePlayServices = GooglePlayServicesUtil
        .isGooglePlayServicesAvailable(activity);
    if (checkGooglePlayServices != ConnectionResult.SUCCESS) {
      // google play services is missing!!!!
      /*
       * Returns status code indicating whether there was an error. Can be one
       * of following in ConnectionResult: SUCCESS, SERVICE_MISSING,
       * SERVICE_VERSION_UPDATE_REQUIRED, SERVICE_DISABLED, SERVICE_INVALID.
       */
      GooglePlayServicesUtil.getErrorDialog(checkGooglePlayServices, activity,
          1122).show();

      callbackContext.error("google play services is missing!!!!");
      return;
    }

    // ------------------------------
    // Initialize Google Maps SDK
    // ------------------------------
    try {
      MapsInitializer.initialize(activity);
    } catch (GooglePlayServicesNotAvailableException e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
      return;
    }
    
    //base layout
    baseLayer = new FrameLayout(activity);
    baseLayer.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    
    // window layout
    LinearLayout windowLayer = new LinearLayout(activity);
    windowLayer.setPadding(25, 25, 25, 25);
    LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
    windowLayer.setLayoutParams(layoutParams);
    baseLayer.addView(windowLayer);
    
    // dialog window layer
    FrameLayout dialogLayer = new FrameLayout(activity);
    dialogLayer.setLayoutParams(layoutParams);
    dialogLayer.setPadding(15, 15, 15, 0);
    dialogLayer.setBackgroundColor(Color.LTGRAY);
    windowLayer.addView(dialogLayer);

    // map frame
    LinearLayout mapFrame = new LinearLayout(activity);
    mapFrame.setPadding(0, 0, 0, 75);
    dialogLayer.addView(mapFrame);
    
    // map
    mapView = new MapView(activity, new GoogleMapOptions());
    mapView.onCreate(null);
    mapView.onResume();
    map = mapView.getMap();
    mapFrame.addView(mapView);
    
    
    // button frame
    LinearLayout buttonFrame = new LinearLayout(activity);
    buttonFrame.setGravity(Gravity.BOTTOM);
    LinearLayout.LayoutParams buttonFrameParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
    buttonFrame.setLayoutParams(buttonFrameParams);
    dialogLayer.addView(buttonFrame);
    
    //close button
    TextView closeText = new TextView(activity);
    closeText.setText("Close");
    closeText.setTextColor(Color.BLUE);
    closeText.setTextSize(20);
    closeText.setPadding(0, 0, 0, 20);
    closeText.setOnClickListener(GoogleMaps.this);
    buttonFrame.addView(closeText);
    
    
    //-----------------------------------
    // Create the instance of Map class
    //-----------------------------------
    try {
      @SuppressWarnings("rawtypes")
      Class pluginCls = Class.forName("plugin.google.maps.Map");
      
      CordovaPlugin plugin = (CordovaPlugin) pluginCls.newInstance();
      PluginEntry pluginEntry = new PluginEntry("GoogleMaps", plugin);
      this.plugins.put("Map", pluginEntry);
      plugin.initialize((CordovaInterface) activity, webView);
      ((MyPlugin)plugin).setMap(map);
      
    } catch (Exception e) {
        e.printStackTrace();
    }
    
    callbackContext.success();
    return;
  }
  
  
  private Boolean getLicenseInfo(JSONArray args, CallbackContext callbackContext) {
    // Activity context = this.cordova.getActivity();
    String msg = GooglePlayServicesUtil
        .getOpenSourceSoftwareLicenseInfo(activity);

    callbackContext.success(msg);
    return true;
  }

  private void showMap(final JSONArray args, final CallbackContext callbackContext) {
    root = (ViewGroup) webView.getParent();
    root.removeView(webView);
    baseLayer.addView(webView, 0);
    activity.setContentView(baseLayer);
    callbackContext.success();
  }


  private Boolean setZoom(JSONArray args, CallbackContext callbackContext) {
    Long zoom;
    try {
      zoom = args.getLong(0);
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
      return false;
    }

    this.myMoveCamera(CameraUpdateFactory.zoomTo(zoom), callbackContext);
    return true;
  }

  private Boolean setMapTypeId(JSONArray args,
      final CallbackContext callbackContext) {
    int mapTypeId = 0;
    try {
      String typeStr = args.getString(0);
      mapTypeId = typeStr.equals("MAP_TYPE_NORMAL") ? GoogleMap.MAP_TYPE_NORMAL
          : mapTypeId;
      mapTypeId = typeStr.equals("MAP_TYPE_HYBRID") ? GoogleMap.MAP_TYPE_HYBRID
          : mapTypeId;
      mapTypeId = typeStr.equals("MAP_TYPE_SATELLITE") ? GoogleMap.MAP_TYPE_SATELLITE
          : mapTypeId;
      mapTypeId = typeStr.equals("MAP_TYPE_TERRAIN") ? GoogleMap.MAP_TYPE_TERRAIN
          : mapTypeId;
      mapTypeId = typeStr.equals("MAP_TYPE_NONE") ? GoogleMap.MAP_TYPE_NONE
          : mapTypeId;

      if (mapTypeId == 0) {
        callbackContext.error("Unknow MapTypeID is specified:" + typeStr);
        return false;
      }
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
      return false;
    }

    final int myMapTypeId = mapTypeId;

    Runnable runnable = new Runnable() {
      public void run() {
        map.setMapType(myMapTypeId);
        callbackContext.success();
      }
    };
    cordova.getActivity().runOnUiThread(runnable);
    return true;
  }

  private void myMoveCamera(CameraPosition cameraPosition,
      final CallbackContext callbackContext) {
    CameraUpdate cameraUpdate = CameraUpdateFactory
        .newCameraPosition(cameraPosition);
    this.myMoveCamera(cameraUpdate, callbackContext);
  }

  private void myMoveCamera(final CameraUpdate cameraUpdate,
      final CallbackContext callbackContext) {
    Runnable runnable = new Runnable() {
      public void run() {
        map.moveCamera(cameraUpdate);
        callbackContext.success();
      }
    };
    cordova.getActivity().runOnUiThread(runnable);
  }

  private void myAnimateCamera(CameraPosition cameraPosition, int durationMS,
      final CallbackContext callbackContext) {
    CameraUpdate cameraUpdate = CameraUpdateFactory
        .newCameraPosition(cameraPosition);
    this.myAnimateCamera(cameraUpdate, durationMS, callbackContext);
  }

  private void myAnimateCamera(final CameraUpdate cameraUpdate,
      final int durationMS, final CallbackContext callbackContext) {
    Runnable runnable = new Runnable() {
      public void run() {
        GoogleMap.CancelableCallback callback = new GoogleMap.CancelableCallback() {
          @Override
          public void onFinish() {
            callbackContext.success();
          }

          @Override
          public void onCancel() {
            callbackContext.success();
          }
        };

        if (durationMS > 0) {
          map.animateCamera(cameraUpdate, durationMS, callback);
        } else {
          map.animateCamera(cameraUpdate, callback);
        }
      }
    };
    cordova.getActivity().runOnUiThread(runnable);
  }

  public class JavaScriptInterface implements OnMarkerClickListener,
      OnInfoWindowClickListener, OnMapClickListener, OnMapLongClickListener {
    Context mContext;

    /** Instantiate the interface and set the context */
    JavaScriptInterface(Context c) {
      mContext = c;
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {

      Runnable runnable = new Runnable() {
        public void run() {
          webView.loadUrl("javascript:plugin.google.maps.Map._onMarkerClick("
              + marker.hashCode() + ")");
        }
      };
      cordova.getActivity().runOnUiThread(runnable);
      return false;
    }

    @Override
    public void onInfoWindowClick(final Marker marker) {
      Runnable runnable = new Runnable() {
        public void run() {
          webView
              .loadUrl("javascript:plugin.google.maps.Map._onInfoWndClick('"
                  + marker.hashCode() + "')");
        }
      };
      cordova.getActivity().runOnUiThread(runnable);
    }

    @Override
    public void onMapLongClick(LatLng point) {
      this.onClicked("onMapLongClick", point);
    }

    @Override
    public void onMapClick(LatLng point) {
      this.onClicked("onMapClick", point);
    }

    private void onClicked(final String callback, final LatLng point) {
      Runnable runnable = new Runnable() {
        public void run() {
          webView.loadUrl("javascript:plugin.google.maps.Map._" + callback
              + "('" + point.latitude + "," + point.longitude + "')");
        }
      };
      cordova.getActivity().runOnUiThread(runnable);
    }
  }

  @Override
  public void onPause(boolean multitasking) {
      if (mapView != null) {
          mapView.onPause();
      }
      super.onPause(multitasking);
  }

  @Override
  public void onResume(boolean multitasking) {
      if (mapView != null) {
          mapView.onResume();
      }
      super.onResume(multitasking);
  }

  @Override
  public void onDestroy() {
      if (mapView != null) {
          mapView.onDestroy();
      }
      super.onDestroy();
  }

  @Override
  public void onClick(View v) {
    Runnable runnable = new Runnable() {
      public void run() {
        root.removeView(baseLayer);
        baseLayer.removeView(webView);
        activity.setContentView(webView);
      }
    };
    cordova.getActivity().runOnUiThread(runnable);
  }
}
