package plugin.google.maps;

import android.util.Log;

import com.dronedeploy.beta.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Leandro Leoncini on 10/13/16.
 */

public class DDPolygon implements GoogleMap.OnMarkerClickListener, GoogleMap.OnMarkerDragListener{

    private Polygon mPolygon;
    private Marker mMarkerCenter;
    private ArrayList<Marker> mPolyMarks = new ArrayList<>();
    private LatLng mCenter;
    private GoogleMap mMap;




    public DDPolygon(Polygon polygon, GoogleMap map) {
        mMap = map;
        mPolygon = polygon;
        addBorderMarkers();
        addCenterMarker();

        mMap.setOnMarkerClickListener(this);
        mMap.setOnMarkerDragListener(this);
    }

    public void setLatLngs(JSONArray params, CallbackContext callbackContext) {
        try {
            List<LatLng> latLngList = getLatLngList(params.getJSONArray(0).getJSONArray(0));
            mPolygon.setPoints(latLngList);
            for (Marker mark : mPolyMarks) {
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
            Marker addedMarker = mMap.addMarker(mark);
            addedMarker.setTag(new MarkerTag());
            mPolyMarks.add(addedMarker);
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

        for (Marker mark : mPolyMarks) {
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
        for (Marker mark : mPolyMarks) {
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
        for (Marker marker : mPolyMarks) {
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

    public void removeMarker(Marker marker) {
        mPolyMarks.remove(marker);
        updateBorder();
    }

    public void addLateralMarkersTo(Marker marker) {

        int index = mPolyMarks.indexOf(marker);

        int before = index == 0 ? mPolyMarks.size() - 1 : index - 1;
        int after = index == mPolyMarks.size() - 1 ? 0 : index + 1;

        Log.i("DDPolygon", "Index: " + index + " Before: " + before + "After: " + after);

        LatLng markerPosition = marker.getPosition();
        LatLng beforePosition = (mPolyMarks.get(before)).getPosition();
        LatLng afterPosition = (mPolyMarks.get(after)).getPosition();

        ArrayList<LatLng> points = new ArrayList();

        points.add(markerPosition);
        points.add(beforePosition);

        LatLng latLngBefore = getPolygonCenterPoint(points);

        points.clear();

        points.add(markerPosition);
        points.add(afterPosition);

        LatLng latLngAfter = getPolygonCenterPoint(points);

        List<LatLng> polyPoints  = mPolygon.getPoints();

        ArrayList<LatLng> newPolyPoints = new ArrayList<>();

        int currentIndex = 0;

        for (LatLng latLng : polyPoints) {

            newPolyPoints.add(latLng);

            if (currentIndex == before) {
                newPolyPoints.add(latLngBefore);
            } else if (currentIndex == after - 1) {
                newPolyPoints.add(latLngAfter);
            }

            currentIndex++;
        }

        mPolygon.setPoints(newPolyPoints);

        ArrayList<Marker> newMarkers = new ArrayList<>();

        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.circle_image);

        for (currentIndex = 0; currentIndex < mPolyMarks.size(); currentIndex++) {

            newMarkers.add(mPolyMarks.get(currentIndex));

            if (currentIndex == before) {
                MarkerOptions mark = new MarkerOptions().position(latLngBefore).draggable(true).
                        icon(icon).anchor(0.5f, 0.5f).alpha(0.5f);
                Marker addMarker = mMap.addMarker(mark);
                addMarker.setTag(new MarkerTag());
                newMarkers.add(addMarker);
            } else if (currentIndex == after - 1) {
                MarkerOptions mark = new MarkerOptions().position(latLngAfter).draggable(true).
                        icon(icon).anchor(0.5f, 0.5f).alpha(0.5f);;
                Marker addMarker = mMap.addMarker(mark);
                addMarker.setTag(new MarkerTag());
                newMarkers.add(addMarker);
            }

        }

        mPolyMarks.clear();
        mPolyMarks.addAll(newMarkers);


    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

        if (isMarkerIdCenter(marker.getId())) {
            moveToCenter(marker.getPosition());
        } else if (isMarkerInBorder(marker.getId())) {
            updateBorder();
        }

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {

        if (isMarkerIdCenter(marker.getId())) {

        } else if (isMarkerInBorder(marker.getId())) {
            MarkerTag tag = (MarkerTag) marker.getTag();
            if (tag.isWasClicked()) {

            } else {
                tag.setWasClicked(true);
                marker.setAlpha(1.0f);
                addLateralMarkersTo(marker);
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        if (isMarkerIdCenter(marker.getId())) {

        } else if (isMarkerInBorder(marker.getId())) {
            MarkerTag tag = (MarkerTag) marker.getTag();
            if (tag.isWasClicked()) {
                removeMarker(marker);
                marker.remove();
            } else {
                tag.setWasClicked(true);
                marker.setAlpha(1.0f);
            }
        }
        return false;
    }
}
