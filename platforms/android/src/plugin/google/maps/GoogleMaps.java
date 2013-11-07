package plugin.google.maps;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
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
import android.widget.Button;
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
  
  private enum METHODS {
    getLicenseInfo,
    GoogleMap_setTilt,
    GoogleMap_getMap,
    GoogleMap_setCenter,
    GoogleMap_setZoom,
    GoogleMap_setMapTypeId,
    GoogleMap_addMarker,
    GoogleMap_addCircle,
    GoogleMap_show,
    GoogleMap_animateCamera,
    GoogleMap_moveCamera,
    GoogleMap_setMyLocationEnabled,
    GoogleMap_setIndoorEnabled,
    GoogleMap_setTrafficEnabled,
    GoogleMap_setCompassEnabled,

    Marker_setAnchor,
    Marker_setDraggable,
    Marker_setTitle,
    Marker_setSnippet,
    Marker_showInfoWindow,
    Marker_hideInfoWindow,
    Marker_getPosition,
    Marker_isInfoWindowShown,
    Marker_remove,
    Marker_setIcon,

    Circle_remove,
    Circle_setCenter,
    Circle_setFillColor,
    Circle_setRadius,
    Circle_setStrokeColor,
    Circle_setStrokeWidth,
    Circle_setVisible,
    Circle_setZIndex

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
  public boolean execute(String action, JSONArray args,
      CallbackContext callbackContext) throws JSONException {
    Log.d("CordovaLog", "action=" + action);

    activity = cordova.getActivity();
    if (action.equals("GoogleMap_getMap") == false
        && action.equals("getLicenseInfo") == false && this.map == null) {
      Log.d("CordovaLog", "map is Null(" + action + ")");
      callbackContext.error("Map is null");
      return false;
    }

    switch (METHODS.valueOf(action)) {
    case getLicenseInfo:
      return this.getLicenseInfo(args, callbackContext);

      /*---------------
       * Map
       *---------------*/
    case GoogleMap_getMap:
      return this.getMap(args, callbackContext);
    case GoogleMap_setTilt:
      return this.setTilt(args, callbackContext);
    case GoogleMap_setCenter:
      return this.setCenter(args, callbackContext);
    case GoogleMap_setZoom:
      return this.setZoom(args, callbackContext);
    case GoogleMap_setMapTypeId:
      return this.setMapTypeId(args, callbackContext);
    case GoogleMap_show:
      return this.showDialog(args, callbackContext);
    case GoogleMap_setMyLocationEnabled:
      return this.setMyLocationEnabled(args, callbackContext);
    case GoogleMap_setIndoorEnabled:
      return this.setIndoorEnabled(args, callbackContext);
    case GoogleMap_setTrafficEnabled:
      return this.setTrafficEnabled(args, callbackContext);
    case GoogleMap_setCompassEnabled:
      return this.setCompassEnabled(args, callbackContext);
    case GoogleMap_animateCamera:
      return this.updateCameraPosition("animateCamera", args, callbackContext);
    case GoogleMap_moveCamera:
      return this.updateCameraPosition("moveCamera", args, callbackContext);

      /*---------------
       * Marker
       *---------------*/
    case GoogleMap_addMarker:
      return this.markerManager.addMarker(map, args, callbackContext);
    case Marker_getPosition:
      return markerManager.getPosition(args, callbackContext);
    case Marker_isInfoWindowShown:
      return markerManager.isInfoWindowShown(args, callbackContext);
    case Marker_setAnchor:
      return markerManager.setAnchor(args, callbackContext);
    case Marker_setDraggable:
      return markerManager.setDraggable(args, callbackContext);
    case Marker_setIcon:
      return markerManager.setIcon(args, callbackContext);
    case Marker_setTitle:
      return markerManager.setTitle(args, callbackContext);
    case Marker_setSnippet:
      return markerManager.setSnippet(args, callbackContext);
    case Marker_showInfoWindow:
      return markerManager.showInfoWindow(args, callbackContext);
    case Marker_hideInfoWindow:
      return markerManager.hideInfoWindow(args, callbackContext);
    case Marker_remove:
      return markerManager.remove(args, callbackContext);

      /*---------------
       * Circle
       *---------------*/
    case GoogleMap_addCircle:
      return this.circleManager.addCircle(map, args, callbackContext);
    case Circle_remove:
      return circleManager.remove(args, callbackContext);
    case Circle_setCenter:
      return circleManager.setCenter(args, callbackContext);
    case Circle_setFillColor:
      return circleManager.setFillColor(args, callbackContext);
    case Circle_setRadius:
      return circleManager.setRadius(args, callbackContext);
    case Circle_setStrokeColor:
      return circleManager.setStrokeColor(args, callbackContext);
    case Circle_setStrokeWidth:
      return circleManager.setStrokeWidth(args, callbackContext);
    case Circle_setVisible:
      return circleManager.setVisible(args, callbackContext);
    case Circle_setZIndex:
      return circleManager.setZIndex(args, callbackContext);

    default:
      break;
    }
    return false;
  }

  private Boolean getMap(JSONArray args, final CallbackContext callbackContext) {
    if (map != null) {
      callbackContext.success();
      return true;
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
      return false;
    }

    // ------------------------------
    // Initialize Google Maps SDK
    // ------------------------------
    try {
      MapsInitializer.initialize(activity);
    } catch (GooglePlayServicesNotAvailableException e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
      return false;
    }
    
    
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
    
        
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
        
        callbackContext.success();
      }
    });
    

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

  private Boolean getLicenseInfo(JSONArray args, CallbackContext callbackContext) {
    // Activity context = this.cordova.getActivity();
    String msg = GooglePlayServicesUtil
        .getOpenSourceSoftwareLicenseInfo(activity);

    callbackContext.success(msg);
    return true;
  }

  private Boolean setMyLocationEnabled(final JSONArray args,
      final CallbackContext callbackContext) {
    Runnable runnable = new Runnable() {
      public void run() {
        Boolean isEnable = false;
        try {
          isEnable = args.getBoolean(0);
          map.setMyLocationEnabled(isEnable);
          callbackContext.success();
        } catch (Exception e) {
          e.printStackTrace();
          callbackContext.error(e.getMessage());
        }
      }
    };
    cordova.getActivity().runOnUiThread(runnable);
    return true;
  }

  private Boolean setIndoorEnabled(final JSONArray args,
      final CallbackContext callbackContext) {
    Runnable runnable = new Runnable() {
      public void run() {
        Boolean isEnable = false;
        try {
          isEnable = args.getBoolean(0);
          map.setIndoorEnabled(isEnable);
          callbackContext.success();
        } catch (Exception e) {
          e.printStackTrace();
          callbackContext.error(e.getMessage());
        }
      }
    };
    cordova.getActivity().runOnUiThread(runnable);
    return true;
  }

  private Boolean setTrafficEnabled(final JSONArray args,
      final CallbackContext callbackContext) {
    Runnable runnable = new Runnable() {
      public void run() {
        Boolean isEnable = false;
        try {
          isEnable = args.getBoolean(0);
          map.setTrafficEnabled(isEnable);
          callbackContext.success();
        } catch (Exception e) {
          e.printStackTrace();
          callbackContext.error(e.getMessage());
        }
      }
    };
    cordova.getActivity().runOnUiThread(runnable);
    return true;
  }

  private Boolean setCompassEnabled(final JSONArray args,
      final CallbackContext callbackContext) {
    Log.d(TAG, "setCompassEnabled is not available in Android");
    callbackContext.success();
    return true;
  }

  private Boolean showDialog(final JSONArray args,
      final CallbackContext callbackContext) {
    Runnable runnable = new Runnable() {
      public void run() {

        root = (ViewGroup) webView.getParent();
        root.removeView(webView);
        baseLayer.addView(webView, 0);
        activity.setContentView(baseLayer);
        
        callbackContext.success();
      }
    };
    
    Log.d("CordovaLog", "showDialog");
    cordova.getActivity().runOnUiThread(runnable);
    return true;
  }

  private Boolean setCenter(JSONArray args, CallbackContext callbackContext) {
    double lat, lng;

    try {
      lat = args.getDouble(0);
      lng = args.getDouble(1);
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
      return false;
    }

    LatLng latLng = new LatLng(lat, lng);
    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(latLng);
    this.myMoveCamera(cameraUpdate, callbackContext);
    return true;
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
