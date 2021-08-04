package plugin.google.maps;

import com.google.android.libraries.maps.CameraUpdate;
import com.google.android.libraries.maps.model.LatLngBounds;

public class AsyncUpdateCameraPositionResult {
    CameraUpdate cameraUpdate;
    int durationMS;
    LatLngBounds cameraBounds;
    double cameraPadding;
}