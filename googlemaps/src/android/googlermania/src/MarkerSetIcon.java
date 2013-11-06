package plugin.google.maps;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;

public class MarkerSetIcon extends AsyncTask<String, Void, Bitmap> {
    private Marker marker;
    
    public MarkerSetIcon(Marker target) {
        marker = target;
    }

    protected Bitmap doInBackground(String... urls) {
        try {
            URL url= new URL(urls[0]);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected void onPostExecute(Bitmap image) {
        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(image);
        marker.setIcon(bitmapDescriptor);
    }
}
