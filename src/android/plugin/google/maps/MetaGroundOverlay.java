package plugin.google.maps;

import com.google.android.libraries.maps.model.GroundOverlay;
import com.google.android.libraries.maps.model.LatLngBounds;

import org.json.JSONObject;

public class MetaGroundOverlay {
    private String id;
    public LatLngBounds bounds;
    public JSONObject properties;
    public JSONObject initOpts;
    public GroundOverlay groundOverlay;
    boolean isClickable = true;
    boolean isVisible = true;

    public MetaGroundOverlay(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

}
