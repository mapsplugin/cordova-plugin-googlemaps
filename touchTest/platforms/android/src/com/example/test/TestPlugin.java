package com.example.test;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;

import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;

public class TestPlugin extends CordovaPlugin {
  private Activity activity;
  private ViewGroup root;
  private ImageView imageView;
  private MapView mapView;
  
  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    activity = cordova.getActivity();
    root = (ViewGroup) webView.getParent();
    
    Runnable runnable = new Runnable() {
      @SuppressLint("NewApi")
      public void run() {
        root.removeView(webView);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setOnTouchListener(new View.OnTouchListener() {
          
          @Override
          public boolean onTouch(View v, MotionEvent event) {
            //imageView.dispatchTouchEvent(event);
            mapView.dispatchTouchEvent(event);
            Log.d("CordovaLog", "webView = " + event.getX() + ", " + event.getY() + ", action=" + event.getAction());
            return false;
          }
        });
        
        
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        FrameLayout layer = new FrameLayout(activity);
        layer.setLayoutParams(layoutParams);
        
        GoogleMapOptions opts = new GoogleMapOptions();
        
        mapView = new MapView(activity, opts);
        mapView.onCreate(new Bundle());
        mapView.onResume();
        
        /*
        LayoutParams imgSize = new LayoutParams((int)(300 * webView.getScaleX()), (int)(300 * webView.getScaleY()));
        imgSize.leftMargin = 300;
        imgSize.topMargin = 300;
        imageView = new ImageView(activity);
        imageView.setLayoutParams(imgSize);
        imageView.setClickable(true);
        imageView.setFocusable(true);
        imageView.setFocusableInTouchMode(true);
        imageView.setBackgroundColor(Color.GREEN);
        imageView.setOnTouchListener(new View.OnTouchListener() {
          
          @Override
          public boolean onTouch(View v, MotionEvent event) {
            Log.d("CordovaLog", "imageView = " + event.getX() + ", " + event.getY());
            return false;
          }
        });

        Resources res = activity.getResources();
        imageView.setImageDrawable(res.getDrawable(R.drawable.screen));
        */
        layer.addView(mapView);
        layer.addView(webView);
        root.addView(layer);
      }
    };
    cordova.getActivity().runOnUiThread(runnable);
    Log.d("CordovaLog", "Initialized");
  }
  @Override
  public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    callbackContext.success("Hello");
    return true;
  }
  

}
