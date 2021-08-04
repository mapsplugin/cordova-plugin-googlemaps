package plugin.google.maps;

import android.content.res.Resources;
import android.os.AsyncTask;

import com.google.android.libraries.maps.CameraUpdate;
import com.google.android.libraries.maps.CameraUpdateFactory;
import com.google.android.libraries.maps.GoogleMap;
import com.google.android.libraries.maps.model.CameraPosition;
import com.google.android.libraries.maps.model.LatLng;
import com.google.android.libraries.maps.model.LatLngBounds;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static plugin.google.maps.PluginMap.DEFAULT_CAMERA_PADDING;

public class UpdateCameraAction extends AsyncTask<Void, Void, AsyncUpdateCameraPositionResult> {
    private Exception mException = null;
    private CallbackContext mCallbackContext;
    private JSONObject mCameraPos;
    private CameraPosition.Builder mBuilder;

    UpdateCameraAction(CallbackContext callbackContext, JSONObject cameraPos, CameraPosition.Builder builder) {
        super();
        this.mCallbackContext = callbackContext;
        this.mCameraPos = cameraPos;
        this.mBuilder = builder;
    }

    @Override
    protected AsyncUpdateCameraPositionResult doInBackground(Void... voids) {
        AsyncUpdateCameraPositionResult result = new AsyncUpdateCameraPositionResult();

        try {

            result.durationMS = 4000;
            result.cameraPadding = DEFAULT_CAMERA_PADDING;
            if (mCameraPos.has("tilt")) {
                mBuilder.tilt((float) mCameraPos.getDouble("tilt"));
            }
            if (mCameraPos.has("bearing")) {
                mBuilder.bearing((float) mCameraPos.getDouble("bearing"));
            }
            if (mCameraPos.has("zoom")) {
                mBuilder.zoom((float) mCameraPos.getDouble("zoom"));
            }
            if (mCameraPos.has("duration")) {
                result.durationMS = mCameraPos.getInt("duration");
            }
            if (mCameraPos.has("padding")) {
                result.cameraPadding = mCameraPos.getDouble("padding");
            }

            if (!mCameraPos.has("target")) {
                return result;
            }

            //------------------------
            // Create a cameraUpdate
            //------------------------
            result.cameraUpdate = null;
            result.cameraBounds = null;
            CameraPosition newPosition;
            Object target = mCameraPos.get("target");
            @SuppressWarnings("rawtypes")
            Class targetClass = target.getClass();
            JSONObject latLng;
            if ("org.json.JSONArray".equals(targetClass.getName())) {
                JSONArray points = mCameraPos.getJSONArray("target");
                result.cameraBounds = PluginUtil.JSONArray2LatLngBounds(points);
                float density = Resources.getSystem().getDisplayMetrics().density;
                result.cameraUpdate = CameraUpdateFactory.newLatLngBounds(result.cameraBounds, (int)(result.cameraPadding * density));
            } else {
                latLng = mCameraPos.getJSONObject("target");
                mBuilder.target(new LatLng(latLng.getDouble("lat"), latLng.getDouble("lng")));
                newPosition = mBuilder.build();
                result.cameraUpdate = CameraUpdateFactory.newCameraPosition(newPosition);
            }
        } catch (Exception e) {
            mException = e;
            e.printStackTrace();
            this.cancel(true);
            return null;
        }

        return result;
    }

    @Override
    public void onCancelled() {
        if (mException != null) {
            mException.printStackTrace();
        }
        mCallbackContext.error(mException != null ? mException.getMessage() + "" : "");
    }
    @Override
    public void onCancelled(AsyncUpdateCameraPositionResult AsyncUpdateCameraPositionResult) {
        if (mException != null) {
            mException.printStackTrace();
        }
        mCallbackContext.error(mException != null ? mException.getMessage() + "" : "");
    }

}
