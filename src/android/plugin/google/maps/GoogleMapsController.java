package plugin.google.maps;

import android.app.Activity;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.apache.cordova.CordovaWebView;
import org.json.JSONObject;
import plugin.google.maps.clustering.clustering.ClusterManager;

/**
 * Created by christian on 05.05.15.
 */
public interface GoogleMapsController {

	public GoogleMap getMap();

	public MapView getMapView();

	public ClusterManager getClusterManager();

	public void clear();

	public void cluster();

	public Marker addItem(MarkerOptions options);

	public void addItem(MarkerOptions options, JSONObject properties);

	public void setActivity(Activity activity);
}
