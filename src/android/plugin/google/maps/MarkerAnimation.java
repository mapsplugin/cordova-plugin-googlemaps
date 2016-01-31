/* Copyright 2013 Google Inc.
   Licensed under Apache 2.0: http://www.apache.org/licenses/LICENSE-2.0.html */
// https://gist.github.com/broady/6314689

package plugin.google.maps;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Property;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.lang.Override;

public class MarkerAnimation {
  static void animateMarker(final Marker marker, final LatLng finalPosition, final long durationInMs, final LatLngInterpolator latLngInterpolator, final PluginAsyncInterface callback) {
    int currentapiVersion = android.os.Build.VERSION.SDK_INT;
    if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      MarkerAnimation.animateMarkerToICS(marker, finalPosition, durationInMs, latLngInterpolator, callback);
    } else if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
      MarkerAnimation.animateMarkerToHC(marker, finalPosition, durationInMs, latLngInterpolator, callback);
    } else {
      MarkerAnimation.animateMarkerToGB(marker, finalPosition, durationInMs, latLngInterpolator, callback);
    }
  }

  static void animateMarkerToGB(final Marker marker, final LatLng finalPosition, final long durationInMs, final LatLngInterpolator latLngInterpolator, final PluginAsyncInterface callback) {
    final LatLng startPosition = marker.getPosition();
    final Handler handler = new Handler();
    final long start = SystemClock.uptimeMillis();
    final Interpolator interpolator = new AccelerateDecelerateInterpolator();
    final float duration = (float) durationInMs;

    handler.post(new Runnable() {
      long elapsed;
      float t;
      float v;

      @Override
      public void run() {
        // Calculate progress using interpolator
        elapsed = SystemClock.uptimeMillis() - start;
        t = elapsed / duration;
        v = interpolator.getInterpolation(t);

        marker.setPosition(latLngInterpolator.interpolate(v, startPosition, finalPosition));

        // Repeat till progress is complete.
        if (t < 1) {
          // Post again 16ms later.
          handler.postDelayed(this, 16);
        } else {
          callback.onPostExecute(callback);
        }
      }
    });
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  static void animateMarkerToHC(final Marker marker, final LatLng finalPosition, final long durationInMs, final LatLngInterpolator latLngInterpolator, final PluginAsyncInterface callback) {
    final LatLng startPosition = marker.getPosition();

    ValueAnimator valueAnimator = new ValueAnimator();
    valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        float v = animation.getAnimatedFraction();
        LatLng newPosition = latLngInterpolator.interpolate(v, startPosition, finalPosition);
        marker.setPosition(newPosition);
      }
    });
    valueAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        callback.onPostExecute(marker);
      }
    });
    valueAnimator.setFloatValues(0, 1); // Ignored.
    valueAnimator.setDuration(durationInMs);
    valueAnimator.start();
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  static void animateMarkerToICS(final Marker marker, LatLng finalPosition, final long durationInMs, final LatLngInterpolator latLngInterpolator, final PluginAsyncInterface callback) {
    TypeEvaluator<LatLng> typeEvaluator = new TypeEvaluator<LatLng>() {
      @Override
      public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
        return latLngInterpolator.interpolate(fraction, startValue, endValue);
      }
    };
    Property<Marker, LatLng> property = Property.of(Marker.class, LatLng.class, "position");
    ObjectAnimator animator = ObjectAnimator.ofObject(marker, property, typeEvaluator, finalPosition);
    animator.addListener(new AnimatorListener() {
      @Override
      public void onAnimationStart(Animator animation) {

      }

      @Override
      public void onAnimationRepeat(Animator animation) {

      }

      @Override
      public void onAnimationCancel(Animator animation) {

      }

      @Override
      public void onAnimationEnd(Animator animation) {
        callback.onPostExecute(marker);
      }
    });
    animator.setDuration(durationInMs);
    animator.start();
  }
}