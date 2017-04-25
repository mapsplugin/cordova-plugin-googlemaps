package plugin.google.maps;

import android.graphics.Color;

import com.dronedeploy.beta.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Leandro Leoncini on 10/13/16.
 */

public class DDPolygon {

    private com.google.android.gms.maps.model.Polygon mPolygon;
    private com.google.android.gms.maps.model.Marker mMarkerCenter;
    private ArrayList<com.google.android.gms.maps.model.Marker> mPolyMarks = new ArrayList<>();
    private LatLng mCenter;
    private GoogleMap mMap;




    public DDPolygon(Polygon polygon, GoogleMap map) {
        mMap = map;
        mPolygon = polygon;
        addBorderMarkers();
        addCenterMarker();
    }

    public void setLatLngs(JSONArray params, CallbackContext callbackContext) {
        try {
            List<LatLng> latLngList = getLatLngList(params.getJSONArray(0).getJSONArray(0));
            mPolygon.setPoints(latLngList);
            for (com.google.android.gms.maps.model.Marker mark : mPolyMarks) {
                mark.remove();
            }
            addBorderMarkers();
            addCenterMarker();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getId() {
        return mPolygon.getId();
    }

    private void addCenterMarker() {
        mCenter = getPolygonCenterPoint(mPolygon.getPoints());
        MarkerOptions markerOptions = new MarkerOptions().position(mCenter).draggable(true).
                icon(BitmapDescriptorFactory.fromResource(R.drawable.move)).
                anchor(0.5f, 0.5f);
        if (mMarkerCenter != null) {
            mMarkerCenter.remove();
        }
        mMarkerCenter = mMap.addMarker(markerOptions);
    }


    private void addBorderMarkers() {
        mPolyMarks.clear();
        List<LatLng> points  = mPolygon.getPoints();

        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.circle_image);

        for (LatLng p : points) {
            MarkerOptions mark = new MarkerOptions().position(p).draggable(true).
                    icon(icon).anchor(0.5f, 0.5f);
            mPolyMarks.add(mMap.addMarker(mark));
        }
    }

    private LatLng getPolygonCenterPoint(List<LatLng> polygonPointsList){
        LatLng centerLatLng;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for(LatLng point : polygonPointsList) {
            builder.include(point);
        }

        LatLngBounds bounds = builder.build();
        centerLatLng =  bounds.getCenter();

        return centerLatLng;
    }

    public void moveToCenter(LatLng center) {
        double offsetY = center.latitude - mCenter.latitude;
        double offsetX = center.longitude - mCenter.longitude;

        for (com.google.android.gms.maps.model.Marker mark : mPolyMarks) {
            LatLng position = new LatLng(mark.getPosition().latitude + offsetY, mark.getPosition().
                    longitude + offsetX);
            mark.setPosition(position);
        }
        List<LatLng> points  = mPolygon.getPoints();
        ArrayList<LatLng> newPoints = new ArrayList();
        for (LatLng p : points) {
            LatLng position = new LatLng(p.latitude + offsetY, p.longitude + offsetX);
            newPoints.add(position);
        }
        mPolygon.setPoints(newPoints);
        mCenter = center;
    }

    public void updateBorder() {
        ArrayList<LatLng> newPoints = new ArrayList();
        for (com.google.android.gms.maps.model.Marker mark : mPolyMarks) {
            LatLng position = new LatLng(mark.getPosition().latitude, mark.getPosition().longitude);
            newPoints.add(position);
        }
        mPolygon.setPoints(newPoints);
        mCenter = getPolygonCenterPoint(newPoints);
        mMarkerCenter.setPosition(mCenter);
    }

    public boolean isMarkerIdCenter(String id) {
        if (mMarkerCenter.getId().equals(id)) {
            return true;
        }
        return false;
    }

    public boolean isMarkerInBorder(String id) {
        for (com.google.android.gms.maps.model.Marker marker : mPolyMarks) {
            if (marker.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static List<LatLng> getLatLngList(JSONArray jsonArray) throws JSONException {
        List<LatLng> list = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject location;
            location = jsonArray.getJSONObject(i);
            list.add(new LatLng(location.getDouble("lat"), location.getDouble("lng")));
        }

        return list;
    }

}
