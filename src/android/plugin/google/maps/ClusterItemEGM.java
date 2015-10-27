package plugin.google.maps;

import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import org.json.JSONObject;
import plugin.google.maps.clustering.clustering.ClusterItem;

/**
 * Created by christian on 07.05.15.
 */
public class ClusterItemEGM implements ClusterItem {

	private final LatLng position;

	private final String TAG = "GoogleMapsPlugin";

	private MarkerOptions options;
	private JSONObject properties;

	ClusterItemEGM(MarkerOptions options, JSONObject properties) {

		this.options = options;
		this.properties = properties;

		if (options.getPosition() != null)
			this.position = options.getPosition();
		else
			this.position = null;

	}

	@Override
	public LatLng getPosition() {
		return this.position;
	}

	public MarkerOptions getOptions() {
		return this.options;
	}

	public JSONObject getProperties() {
		return this.properties;
	}
}
