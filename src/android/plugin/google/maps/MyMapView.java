package plugin.google.maps;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;

public class MyMapView extends MapView {

  public MyMapView(Context context) {
    super(context);
  }
  
  public MyMapView(Context context, GoogleMapOptions options) {
    super(context, options);
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
      int action = ev.getAction();
      switch (action) {
      case MotionEvent.ACTION_DOWN:
          // Disallow ScrollView to intercept touch events.
          this.getParent().requestDisallowInterceptTouchEvent(true);
          break;

      case MotionEvent.ACTION_UP:
          // Allow ScrollView to intercept touch events.
          this.getParent().requestDisallowInterceptTouchEvent(false);
          break;
      }
      Log.d("GoogleMaps", "---:onTouchEvent");

      // Handle MapView's touch events.
      super.onTouchEvent(ev);
      return true;
  }
}
