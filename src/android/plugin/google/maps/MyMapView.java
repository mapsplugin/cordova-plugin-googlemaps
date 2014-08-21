package plugin.google.maps;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;

public class MyMapView extends MapView {
  public interface OnTouchListener {
      public abstract void onDispatchTouchListener();
  }
  
  private OnTouchListener mListener;
  
  public MyMapView(Context context) {
    super(context);
  }
  
  public MyMapView(Context context, GoogleMapOptions options) {
    super(context, options);
  }
  
  public void setOnDispatchTouchListener(OnTouchListener listener) {
    mListener = listener;
    this.addView(new TouchableWrapper(this.getContext()));
  }


  public class TouchableWrapper extends FrameLayout {
    
    public TouchableWrapper(Context context) {
      super(context);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
      int action = event.getAction();
      if (mListener != null &&
          (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP)) {
        mListener.onDispatchTouchListener();
      }
      return super.dispatchTouchEvent(event);
    }
  }
}
