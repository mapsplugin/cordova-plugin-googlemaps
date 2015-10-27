package plugin.google.maps;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Point;
import android.location.Location;
import android.util.Log;
import android.view.View;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.*;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginEntry;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;


/**
 * Created by christian on 05.05.15.
 */
public abstract class GoogleMapsControllerImpl implements GoogleMapsController,
		GoogleMap.OnCameraChangeListener, GoogleMap.OnInfoWindowClickListener,
		GoogleMap.OnMapClickListener, GoogleMap.OnMapLoadedCallback,
		GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener,
		GoogleMap.OnMarkerDragListener, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.InfoWindowAdapter {

	protected final HashMap<String, PluginEntry> plugins = new HashMap<String, PluginEntry>();
	protected final String TAG = "GoogleMapsPlugin";
	protected MapView mapView = null;
	protected GoogleMap map = null;
	protected Activity activity = null;

//	GoogleMapsControllerImpl(MapView mapView, JSONObject controls) throws JSONException {
//		this.mapView = mapView;
//		this.mapView.onCreate(null);
//		this.mapView.onResume();
//		this.setMap(mapView.getMap(), controls);
//	}

	public void setMap(GoogleMap map, JSONObject controls) throws JSONException {
		this.map = map;

		if (this.map != null) {
			this.setMapListener();

			if (controls != null)
				this.setControls(controls);
		}
		else
			Log.e(TAG, "(error)Map was not initialised");

	}

	public void setMap(GoogleMap map) throws JSONException {
		this.setMap(map, null);
	}

	public GoogleMap getMap() {
		if (this.map != null)
			return this.map;
		else
			Log.e(TAG, "(error)Map was not initialised");

		return null;
	}

	public void setActivity(Activity activity) {
		this.activity = activity;
	}

	@Override
	public MapView getMapView() {
		return mapView;
	}

	public void clear() {
		Log.d(TAG, "clearing Map");
		this.plugins.clear();
		this.map.clear();
	}

	public void cluster() {

	}

	protected void setMapListener() {
		// TODO: Add map listender.
		map.setOnCameraChangeListener(this);
		map.setOnInfoWindowClickListener(this);
		map.setOnMapClickListener(this);
		map.setOnMapLoadedCallback(this);
		//map.setOnMapLongClickListener(this);
		map.setOnMarkerClickListener(this);
		map.setOnMarkerDragListener(this);
		map.setOnMyLocationButtonClickListener(this);
		map.setOnInfoWindowClickListener(this);

		// Custom info window
		map.setInfoWindowAdapter(this);
	}

	protected void setControls(JSONObject controls) throws JSONException {
		if (this.map != null) {

			if (controls.has("myLocationButton")) {
				Boolean isEnabled = controls.getBoolean("myLocationButton");
				map.setMyLocationEnabled(isEnabled);
				map.getUiSettings().setMyLocationButtonEnabled(isEnabled);
			}
			if (controls.has("indoorPicker")) {
				Boolean isEnabled = controls.getBoolean("indoorPicker");
				map.setIndoorEnabled(isEnabled);
			}
		}
	}

	protected boolean isPointOnTheGeodesicLine(List<LatLng> points, LatLng point, double threshold) {
		double trueDistance, testDistance1, testDistance2;
		Point p0, p1, touchPoint;
		touchPoint = new Point();
		touchPoint.x = (int) (point.latitude * 100000);
		touchPoint.y = (int) (point.longitude * 100000);

		for (int i = 0; i < points.size() - 1; i++) {
			p0 = new Point();
			p0.x = (int) (points.get(i).latitude * 100000);
			p0.y = (int) (points.get(i).longitude * 100000);
			p1 = new Point();
			p1.x = (int) (points.get(i + 1).latitude * 100000);
			p1.y = (int) (points.get(i + 1).longitude * 100000);
			trueDistance = this.calculateDistance(points.get(i), points.get(i + 1));
			testDistance1 = this.calculateDistance(points.get(i), point);
			testDistance2 = this.calculateDistance(point, points.get(i + 1));
			// the distance is exactly same if the point is on the straight line
			if (Math.abs(trueDistance - (testDistance1 + testDistance2)) < threshold) {
				return true;
			}
		}

		return false;
	}

	protected double calculateDistance(LatLng pt1, LatLng pt2){
		float[] results = new float[1];
		Location.distanceBetween(pt1.latitude, pt1.longitude,
				pt2.latitude, pt2.longitude, results);
		return results[0];
	}

	abstract public void addItem(MarkerOptions options, JSONObject properties);

	@Override
	abstract public void onCameraChange(CameraPosition cameraPosition);

	@Override
	abstract public void onInfoWindowClick(Marker marker);

	@Override
	abstract public void onMapClick(LatLng latLng);

	@Override
	abstract public void onMapLoaded();

	@Override
	abstract public void onMapLongClick(LatLng latLng);

	@Override
	abstract public boolean onMarkerClick(Marker marker);

	@Override
	abstract public void onMarkerDragStart(Marker marker);

	@Override
	abstract public void onMarkerDrag(Marker marker);

	@Override
	abstract public void onMarkerDragEnd(Marker marker);

	@Override
	abstract public boolean onMyLocationButtonClick();

	@Override
	abstract public View getInfoWindow(Marker marker);

	@Override
	abstract public View getInfoContents(Marker marker);
}
