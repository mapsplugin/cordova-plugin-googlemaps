package plugin.google.maps;

import android.view.View;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.json.JSONException;
import org.json.JSONObject;
import plugin.google.maps.clustering.clustering.ClusterManager;

/**
 * Created by christian on 05.05.15.
 */
public class GoogleMapsDefaultController extends GoogleMapsControllerImpl {


	GoogleMapsDefaultController(MapView mapView, JSONObject controls) throws JSONException {
		this.mapView = mapView;
		this.mapView.onCreate(null);
		this.mapView.onResume();
		this.setMap(mapView.getMap(), controls);
	}

	@Override
	public Marker addItem(MarkerOptions options) {
		return map.addMarker(options);
	}

	public void addItem(MarkerOptions options, JSONObject properties) {
		// TODO: implement...
	}

	public ClusterManager getClusterManager() {
		return null;
	}

	@Override
	public void onCameraChange(CameraPosition cameraPosition) {

	}

	@Override
	public void onInfoWindowClick(Marker marker) {

	}

	@Override
	public void onMapClick(LatLng latLng) {

	}

	@Override
	public void onMapLoaded() {

	}

	@Override
	public void onMapLongClick(LatLng latLng) {

	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		return false;
	}

	@Override
	public void onMarkerDragStart(Marker marker) {

	}

	@Override
	public void onMarkerDrag(Marker marker) {

	}

	@Override
	public void onMarkerDragEnd(Marker marker) {

	}

	@Override
	public boolean onMyLocationButtonClick() {
		return false;
	}

	@Override
	public View getInfoWindow(Marker marker) {
		return null;
	}

	@Override
	public View getInfoContents(Marker marker) {
		return null;
	}
}
