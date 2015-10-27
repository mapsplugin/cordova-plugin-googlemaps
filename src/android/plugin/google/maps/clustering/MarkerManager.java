package plugin.google.maps.clustering;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import plugin.google.maps.MyPlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Keeps track of collections of markers on the map. Delegates all Marker-related events to each
 * collection's individually managed listeners.
 * <p/>
 * All marker operations (adds and removes) should occur via its collection class. That is, don't
 * add a marker via a collection, then remove it via Marker.remove()
 */
public class MarkerManager implements GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnMarkerDragListener, GoogleMap.InfoWindowAdapter {
    private final GoogleMap mMap;

    private final String TAG = "GoogleMapsPlugin";

    private final Map<String, Collection> mNamedCollections = new HashMap<String, Collection>();
    private final Map<Marker, Collection> mAllMarkers = new HashMap<Marker, Collection>();


    public MarkerManager(GoogleMap map) {
        this.mMap = map;
    }

    public Collection newCollection() {
        return new Collection();
    }

    /**
     * Create a new named collection, which can later be looked up by {@link #getCollection(String)}
     * @param id a unique id for this collection.
     */
    public Collection newCollection(String id) {
        if (mNamedCollections.get(id) != null) {
            throw new IllegalArgumentException("collection id is not unique: " + id);
        }
        Collection collection = new Collection();
        mNamedCollections.put(id, collection);
        return collection;
    }

    /**
     * Gets a named collection that was created by {@link #newCollection(String)}
     * @param id the unique id for this collection.
     */
    public Collection getCollection(String id) {
        return mNamedCollections.get(id);
    }

    @Override
    public View getInfoWindow(Marker marker) {
        Collection collection = mAllMarkers.get(marker);
        if (collection != null && collection.mInfoWindowAdapter != null) {
            return collection.mInfoWindowAdapter.getInfoWindow(marker);
        }
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        Collection collection = mAllMarkers.get(marker);
        if (collection != null && collection.mInfoWindowAdapter != null) {
            return collection.mInfoWindowAdapter.getInfoContents(marker);
        }
        return null;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Collection collection = mAllMarkers.get(marker);
        if (collection != null && collection.mInfoWindowClickListener != null) {
            collection.mInfoWindowClickListener.onInfoWindowClick(marker);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Collection collection = mAllMarkers.get(marker);
        if (collection != null && collection.mMarkerClickListener != null) {
            return collection.mMarkerClickListener.onMarkerClick(marker);
        }
        return false;
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        Collection collection = mAllMarkers.get(marker);
        if (collection != null && collection.mMarkerDragListener != null) {
            collection.mMarkerDragListener.onMarkerDragStart(marker);
        }
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        Collection collection = mAllMarkers.get(marker);
        if (collection != null && collection.mMarkerDragListener != null) {
            collection.mMarkerDragListener.onMarkerDrag(marker);
        }
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        Collection collection = mAllMarkers.get(marker);
        if (collection != null && collection.mMarkerDragListener != null) {
            collection.mMarkerDragListener.onMarkerDragEnd(marker);
        }
    }

    /**
     * Removes a marker from its collection.
     *
     * @param marker the marker to remove.
     * @return true if the marker was removed.
     */
    public boolean remove(Marker marker) {
        Collection collection = mAllMarkers.get(marker);
        return collection != null && collection.remove(marker);
    }

    public class Collection {
        private final Set<Marker> mMarkers = new HashSet<Marker>();
        private GoogleMap.OnInfoWindowClickListener mInfoWindowClickListener;
        private GoogleMap.OnMarkerClickListener mMarkerClickListener;
        private GoogleMap.OnMarkerDragListener mMarkerDragListener;
        private GoogleMap.InfoWindowAdapter mInfoWindowAdapter;

        private final HashMap<String, JSONObject> mMarkerProperties = new HashMap<String, JSONObject>();
        private final HashMap<String, MarkerOptions> mMarkerOptions = new HashMap<String, MarkerOptions>();
//        private final JSONArray markersJSON = new JSONArray();

        public Collection() {
        }

        public void addPropeties(JSONObject prop, Marker marker, MarkerOptions options) throws JSONException {

            mMarkerProperties.put(marker.getId(), prop);
            mMarkerOptions.put(marker.getId(), options);
//            mAllMarkers.put(marker, Collection.this);

        }

        public Marker getMarkerById(String id) {
            Log.d(TAG, "searching for Marker: " + id);
            for (Marker m : mMarkers) {
                if (("marker_" + m.getId()).equals(id)) {
                    return m;
                }
            }
            return null;
        }

        public Marker addMarker(MarkerOptions opts) {
            Marker marker = mMap.addMarker(opts);
            mMarkers.add(marker);
            mAllMarkers.put(marker, Collection.this);
            return marker;
        }

        @SuppressLint("NewApi")
        public boolean remove(Marker marker) {
//            Log.d(TAG, "remove Markers");
            if (mMarkers.remove(marker)) {
                mAllMarkers.remove(marker);
                mMarkerProperties.remove(marker.getId());
                mMarkerOptions.remove(marker.getId());

                marker.remove();
                return true;
            }
            return false;
        }

        @SuppressLint("NewApi")
        public void clear() {
            for (Marker marker : mMarkers) {
                marker.remove();
                mAllMarkers.remove(marker);
            }
            mMarkers.clear();
            mMarkerProperties.clear();
            mMarkerOptions.clear();
//            mMarkersJSON.clear();
        }

//        private int getMarkerJSONIndex(Marker marker) {
//            for (int i = 0; i < markersJSON.length(); i++) {
//                try {
//                    if ( ( (JSONObject) markersJSON.get(i) ).get("id").equals(marker.getId())) {
//                        return i;
//                    }
//                } catch (JSONException e) {
//                    Log.e(TAG, "Error while searching for MarkerJson in markersJSON JSONArray: attribute [id] is not set");
//                    e.printStackTrace();
//                }
//            }
//            return -1;
//        }

        private JSONObject markerOptionsToJSONObject(MarkerOptions options) throws JSONException {
            JSONObject result = new JSONObject();

            result.put("opacity", options.getAlpha());

            JSONObject position = new JSONObject();
            position.put("lat", options.getPosition().latitude);
            position.put("lng", options.getPosition().longitude);
            result.put("position", position);

            JSONArray anchor = new JSONArray();
            anchor.put(options.getAnchorU());
            anchor.put(options.getAnchorV());
            result.put("anchor", anchor);

            result.put("draggable", options.isDraggable());

            result.put("icon", options.getIcon());

            result.put("snippet", options.getSnippet());

            result.put("title", options.getTitle());

            result.put("visible", options.isVisible());

            result.put("flat", options.isFlat());

            result.put("rotation", options.getRotation());

            return result;
        }

        public java.util.Collection<Marker> getMarkers() {
            return Collections.unmodifiableCollection(mMarkers);
        }

        public JSONArray getMarkersJSON () throws JSONException {
            JSONArray result = new JSONArray();
            for (Marker marker : mMarkers) {
                JSONObject obj = new JSONObject();
                obj.put("hashCode", marker.hashCode());
                obj.put("id", "marker_" + marker.getId());
                obj.put("markerId", mMarkerProperties.get(marker.getId()).getInt("markerId"));
                obj.put("markerOptions", this.markerOptionsToJSONObject(mMarkerOptions.get(marker.getId())));
                result.put(obj);
            }

            return result;
        }

        public JSONObject getMarkerProperties (String propertyId) {
            return mMarkerProperties.get(propertyId);
        }

        public void setOnInfoWindowClickListener(GoogleMap.OnInfoWindowClickListener infoWindowClickListener) {
            mInfoWindowClickListener = infoWindowClickListener;
        }

        public void setOnMarkerClickListener(GoogleMap.OnMarkerClickListener markerClickListener) {
            mMarkerClickListener = markerClickListener;
        }

        public void setOnMarkerDragListener(GoogleMap.OnMarkerDragListener markerDragListener) {
            mMarkerDragListener = markerDragListener;
        }

        public void setOnInfoWindowAdapter(GoogleMap.InfoWindowAdapter infoWindowAdapter) {
            mInfoWindowAdapter = infoWindowAdapter;
        }
    }
}
