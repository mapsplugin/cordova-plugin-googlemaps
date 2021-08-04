package plugin.google.maps;

import com.google.android.libraries.maps.model.LatLngBounds;
import com.google.android.libraries.maps.model.Polyline;

import org.json.JSONObject;

public class MetaPolyline {

    private String id;
    public Polyline polyline;
    public LatLngBounds bounds;
    public JSONObject properties;
    boolean isClickable = true;
    boolean isVisible = true;

    public MetaPolyline(String id) {
        this.id = id;
    }
    public String getId() {
        return this.id;
    }


}
