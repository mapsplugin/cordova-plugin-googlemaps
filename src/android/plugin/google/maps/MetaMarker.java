package plugin.google.maps;

import android.util.Size;

import com.google.android.libraries.maps.model.LatLng;
import com.google.android.libraries.maps.model.Marker;

import org.json.JSONObject;

public class MetaMarker {
    private String id;
    Marker marker;
    JSONObject properties;
    String iconCacheKey;
    Size iconSize;
    boolean isClickable = true;
    boolean isVisible = true;
    double lat;
    double lng;

    public MetaMarker(String id) {
        this.id = id;
    }
    public String getId() {
        return this.id;
    }
}
