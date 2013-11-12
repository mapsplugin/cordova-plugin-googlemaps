package plugin.google.maps;

import java.util.HashMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginEntry;
import org.json.JSONArray;
import org.json.JSONException;

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
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
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
  private int CLOSE_LINK_ID;
  private int ABOUT_LINK_ID;

  private JavaScriptInterface jsInterface;

  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
    activity = cordova.getActivity();
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
    if (args != null && args.length() > 0) {
      Log.d(TAG, "action=" + action + " args[0]=" + args.getString(0));
    } else {
      Log.d(TAG, "action=" + action);
    }

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
            
            // Load the class plugin
            if (classMethod.equals(params[0] + ".create" + params[0])) {
              GoogleMaps.this.loadPlugin(params[0]);
            }
            
            
            PluginEntry entry = GoogleMaps.this.plugins.get(params[0]);
            if (params.length == 2 && entry != null) { 
              entry.plugin.execute("execute", args, callbackContext);
            } else {
              callbackContext.error(action + " parameter is invalid length.");
            }
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
    //dialogLayer.setPadding(15, 15, 15, 0);
    dialogLayer.setBackgroundColor(Color.LTGRAY);
    windowLayer.addView(dialogLayer);

    // map frame
    LinearLayout mapFrame = new LinearLayout(activity);
    mapFrame.setPadding(0, 0, 0, 50);
    dialogLayer.addView(mapFrame);
    
    // map
    mapView = new MapView(activity, new GoogleMapOptions());
    mapView.onCreate(null);
    mapView.onResume();
    map = mapView.getMap();
    mapFrame.addView(mapView);
    
    
    // button frame
    LinearLayout buttonFrame = new LinearLayout(activity);
    buttonFrame.setOrientation(LinearLayout.HORIZONTAL);
    buttonFrame.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM);
    LinearLayout.LayoutParams buttonFrameParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
    buttonFrame.setLayoutParams(buttonFrameParams);
    dialogLayer.addView(buttonFrame);
    
    //close button
    LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT, 1.0f);
    TextView closeLink = new TextView(activity);
    closeLink.setText("Close");
    closeLink.setLayoutParams(buttonParams);
    closeLink.setTextColor(Color.BLUE);
    closeLink.setTextSize(20);
    closeLink.setGravity(Gravity.LEFT);
    closeLink.setPadding(10, 0, 0, 10);
    closeLink.setOnClickListener(GoogleMaps.this);
    CLOSE_LINK_ID = closeLink.getId();
    buttonFrame.addView(closeLink);

    //license button
    TextView aboutLink = new TextView(activity);
    aboutLink.setText("About");
    aboutLink.setTextColor(Color.BLUE);
    aboutLink.setLayoutParams(buttonParams);
    aboutLink.setTextSize(20);
    aboutLink.setGravity(Gravity.RIGHT);
    aboutLink.setPadding(10, 10, 10, 10);
    aboutLink.setOnClickListener(GoogleMaps.this);
    ABOUT_LINK_ID = aboutLink.getId();
    buttonFrame.addView(aboutLink);
    
    this.loadPlugin("Map");
    
    callbackContext.success();
    return;
  }

  //-----------------------------------
  // Create the instance of class
  //-----------------------------------
  private void loadPlugin(String serviceName) {
    if (plugins.containsKey(serviceName)) {
      return;
    }
    try {
      @SuppressWarnings("rawtypes")
      Class pluginCls = Class.forName("plugin.google.maps.Plugin" + serviceName);
      
      CordovaPlugin plugin = (CordovaPlugin) pluginCls.newInstance();
      PluginEntry pluginEntry = new PluginEntry("GoogleMaps", plugin);
      this.plugins.put(serviceName, pluginEntry);
      plugin.initialize((CordovaInterface) activity, webView);
      ((MyPlugin)plugin).setMap(map);
      if (map == null) {
        Log.e(TAG, "map is null!");
      }
      
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  @SuppressWarnings("unused")
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
  
  private void closeWindow() {
    root.removeView(baseLayer);
    baseLayer.removeView(webView);
    activity.setContentView(webView);
  }

  @Override
  public void onClick(View view) {
    int viewId = view.getId();
    if (viewId == CLOSE_LINK_ID) {
      closeWindow();
      return;
    }
    if (viewId == ABOUT_LINK_ID) {
      closeWindow();
      return;
    }
  }
}
