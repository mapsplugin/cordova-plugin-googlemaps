package plugin.google.maps;

import com.google.android.libraries.maps.model.Circle;
import com.google.android.libraries.maps.model.LatLngBounds;

import org.json.JSONObject;

public class MetaCircle  {
    private String id;
    Circle circle;
    JSONObject properties;
    LatLngBounds bounds;
    public MetaCircle(String id) {
        this.id = id;
    }
    public String getId() {
        return this.id;
    }
    boolean isClickable = true;
    boolean isVisible = true;


}
