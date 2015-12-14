package plugin.google.maps;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import plugin.google.maps.clustering.clustering.Cluster;
import plugin.google.maps.clustering.clustering.ClusterManager;

/**
 * Created by christian on 05.05.15.
 */
public class GoogleMapsClusterController extends GoogleMapsControllerImpl implements ClusterManager.OnClusterClickListener<ClusterItemEGM>,ClusterManager.OnClusterItemClickListener<ClusterItemEGM> {

	private final String TAG = "GoogleMapsPlugin";

	private ClusterManager clusterManager;

	@Override
	public boolean onClusterClick(Cluster<ClusterItemEGM> cluster) {
		// Send ClusterClick
		this.sendClusterEvent("cluster_click", cluster);

		Log.d(TAG, "Location = " + cluster.getPosition());
		this.map.animateCamera(CameraUpdateFactory.newLatLngZoom(cluster.getPosition(), map.getCameraPosition().zoom + 2));

		return true;
	}

	@Override
	public boolean onClusterItemClick(ClusterItemEGM item) {
		// Send ClusterItemClick
		this.sendClusterItemEvent("click", item);

		JSONObject props = item.getProperties();

		if (props.has("disableAutoPan")) {
			try {
				return props.getBoolean("disableAutoPan");
			} catch (JSONException e) {}
		}

		return false;
	}

	private enum TEXT_STYLE_ALIGNMENTS {
		left, center, right
	}

	GoogleMapsClusterController(MapView mapView, JSONObject controls, Context context) throws JSONException {
		this.mapView = mapView;
		this.mapView.onCreate(null);
		this.mapView.onResume();
		this.setMap(mapView.getMap(), controls);
		this.map.setPadding(-50, -50, -50, -50);

		// Create ClusterManager
		this.clusterManager = new ClusterManager(context, this.map);

		// onResume/onPause replacement
		this.map.setOnMarkerClickListener(this.clusterManager);

		this.clusterManager.setRenderer(new CustomRendererEGM(context, this.map, clusterManager));

		clusterManager.setOnClusterClickListener(this);
		clusterManager.setOnClusterItemClickListener(this);
	}

	public Marker addItem(MarkerOptions options) {
		this.addItem(options, null);
		return null;
	}

	@Override
	public void addItem(MarkerOptions options, JSONObject properties) {
		ClusterItemEGM item = new ClusterItemEGM(options, properties);
		clusterManager.addItem(item);
	}

	public void clear() {
		super.clear();
		this.clusterManager.clearItems();
	}

	public void cluster() {
		this.clusterManager.cluster();
	}

	public ClusterManager getClusterManager() {
		return this.clusterManager;
	}

	private void sendClusterEvent(String eventName, Cluster<ClusterItemEGM> cluster) {
		try {
			// Prepare the information from cluster
			JSONObject obj = new JSONObject();

			obj.put("latitude", cluster.getPosition().latitude);
			obj.put("longitude", cluster.getPosition().longitude);
			obj.put("size", cluster.getSize());

			String event = "javascript:plugin.google.maps.Map._onClusterEvent('" + eventName + "','" + obj.toString() + "')";

			GoogleMaps.getInstance().webView.loadUrl(event);
		} catch (Exception e) {}
    }

	private boolean sendClusterItemEvent(String eventName, ClusterItemEGM item) {
		try {
			int id = item.getOptions().hashCode();

			String event = "javascript:plugin.google.maps.Map._onMarkerEvent('" + eventName + "','" + id + "')";

			GoogleMaps.getInstance().webView.loadUrl(event);

			return true;
		} catch(Exception e) {
			e.printStackTrace();

			return false;
		}
	}
	// End of Cluster Events

	@Override
	public void onCameraChange(CameraPosition cameraPosition) {
		clusterManager.onCameraChange(cameraPosition);
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		Log.d(TAG, "InfoWindow clicked = " + marker.getTitle());
	}

	@Override
	public void onMapClick(LatLng latLng) {

	}

	@Override
	public void onMapLoaded() {
		clusterManager.cluster();
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
		Log.d(TAG, "View get Infowindow - MarkerID: " + marker.getId());
		return null;
	}

	@Override
	public View getInfoContents(Marker marker) {
		String title = marker.getTitle();
		String snippet = marker.getSnippet();
		if ((title == null) && (snippet == null)) {
			return null;
		}

//		TODO: take properties you need from clusterManagers plugin hashmap.
		JSONObject styles = null;

		String propertyId = "marker_property_" + marker.getId();
		JSONObject properties = this.getClusterManager().getMarkerCollection().getMarkerProperties(propertyId);
		if (properties.has("styles")) {
			try {
				styles = (JSONObject) properties.getJSONObject("styles");
			} catch (JSONException e) {}
		}

		// Linear layout
		LinearLayout windowLayer = new LinearLayout(activity);
		windowLayer.setPadding(3, 3, 3, 3);
		windowLayer.setOrientation(LinearLayout.VERTICAL);
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
		layoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER;
		windowLayer.setLayoutParams(layoutParams);

		//YASIN
		LinearLayout windowLayer2 = new LinearLayout(activity);
		windowLayer2.setPadding(3, 3, 3, 3);
		windowLayer2.setOrientation(LinearLayout.HORIZONTAL);
		FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
		layoutParams2.gravity = Gravity.BOTTOM | Gravity.CENTER;
		windowLayer2.setLayoutParams(layoutParams2);

		LinearLayout windowLayer3 = new LinearLayout(activity);
		windowLayer3.setPadding(3, 3, 3, 3);
		windowLayer3.setOrientation(LinearLayout.VERTICAL);
		FrameLayout.LayoutParams layoutParams3 = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
		layoutParams3.gravity = Gravity.BOTTOM | Gravity.CENTER;
		windowLayer3.setLayoutParams(layoutParams3);


		//YASIN

		//----------------------------------------
		// text-align = left | center | right
		//----------------------------------------
		int gravity = Gravity.LEFT;
		int textAlignment = View.TEXT_ALIGNMENT_GRAVITY;

		if (styles != null) {
			try {
				String textAlignValue = styles.getString("text-align");

				switch(TEXT_STYLE_ALIGNMENTS.valueOf(textAlignValue)) {
					case left:
						gravity = Gravity.LEFT;
						textAlignment = View.TEXT_ALIGNMENT_GRAVITY;
						break;
					case center:
						gravity = Gravity.CENTER;
						textAlignment = View.TEXT_ALIGNMENT_CENTER;
						break;
					case right:
						gravity = Gravity.RIGHT;
						textAlignment = View.TEXT_ALIGNMENT_VIEW_END;
						break;
				}

			} catch (Exception e) {}
		}

		if (title != null) {
			if (title.indexOf("data:image/") > -1 && title.indexOf(";base64,") > -1) {
				String[] tmp = title.split(",");
				Bitmap image = PluginUtil.getBitmapFromBase64encodedImage(tmp[1]);
				image = PluginUtil.scaleBitmapForDevice(image);
				ImageView imageView = new ImageView(this.activity);
				imageView.setImageBitmap(image);
				windowLayer.addView(imageView);
			} else {



				TextView textView = new TextView(this.activity);
				textView.setText(title);
				textView.setSingleLine(false);

				int titleColor = Color.BLACK;
				if (styles != null && styles.has("color")) {
					try {
						titleColor = PluginUtil.parsePluginColor(styles.getJSONArray("color"));
					} catch (JSONException e) {}
				}
				textView.setTextColor(titleColor);
				textView.setGravity(gravity);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
					textView.setTextAlignment(textAlignment);
				}

				//----------------------------------------
				// font-style = normal | italic
				// font-weight = normal | bold
				//----------------------------------------
				int fontStyle = Typeface.NORMAL;
				if (styles != null) {
					try {
						if ("italic".equals(styles.getString("font-style"))) {
							fontStyle = Typeface.ITALIC;
						}
					} catch (JSONException e) {}
					try {
						if ("bold".equals(styles.getString("font-weight"))) {
							fontStyle = fontStyle | Typeface.BOLD;
						}
					} catch (JSONException e) {}
				}
				textView.setTypeface(Typeface.DEFAULT, fontStyle);

				windowLayer.addView(textView);
			}
		}
		if (snippet != null) {
			//snippet = snippet.replaceAll("\n", "");
			TextView textView2 = new TextView(this.activity);
			textView2.setText(snippet);
			textView2.setTextColor(Color.GRAY);
			textView2.setTextSize((textView2.getTextSize() / 6 * 5) / Resources.getSystem().getDisplayMetrics().density);
			textView2.setGravity(gravity);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
				textView2.setTextAlignment(textAlignment);
			}

			windowLayer.addView(textView2);
		}
		//TEST
		String bild = "data:image/;base64,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";
		//String bild = "data:image/;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAAeCAYAAAA7MK6iAAAAAXNSR0IArs4c6QAAAAlwSFlzAAC8swAAvLMBkkKopAAAActpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IlhNUCBDb3JlIDUuNC4wIj4KICAgPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4KICAgICAgPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIKICAgICAgICAgICAgeG1sbnM6eG1wPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvIgogICAgICAgICAgICB4bWxuczp0aWZmPSJodHRwOi8vbnMuYWRvYmUuY29tL3RpZmYvMS4wLyI+CiAgICAgICAgIDx4bXA6Q3JlYXRvclRvb2w+d3d3Lmlua3NjYXBlLm9yZzwveG1wOkNyZWF0b3JUb29sPgogICAgICAgICA8dGlmZjpPcmllbnRhdGlvbj4xPC90aWZmOk9yaWVudGF0aW9uPgogICAgICA8L3JkZjpEZXNjcmlwdGlvbj4KICAgPC9yZGY6UkRGPgo8L3g6eG1wbWV0YT4KGMtVWAAABqlJREFUSA2NV1tsVFUU3efeOzOlD6OBIBVbKEoCKFQETCAQCokfmhj9gQ8aKP6oxKJG44cGEiQIMfrXIvAFqDQGfvQHRRIoBjBgwD6AKjEUOiBqxAgF2t7Xca1zZqZ3mCm6k9659zz2Ovu1zq6ScqJFyQdNrmzqDDk9ee+M8a5KLdexs1QpPQtDtfir5xxkAKt/07HqU058LNLBkWstP98wM/tXuLLiQIx5bb4TD5V4t6/7xZWVEvFj6u4nJkWu97rSull5qkGlHbNGR1q0H5t3jinXquGYDnW/1tLhpeO2/lXn/jCLeICVB4xO841HMXBiQf3uxtXi6K1OpfsolFkgJSHOnj+9m1NiFSpjl2cO4imJ70ZXof69gZbuL8y6TU1e3oP8HgVOTNTtnd3mVLitAqNiPw7tIoUfTZNzn7kDMCxWcCCFHbAXDxwgJVith6O2gZbeN8ySBIbdlLC0bu+cfW6NtyoehJkCC0W8gmqzO/fgbB4yOc53zuX2OjWeigbDjmxLT7MZzWEx7NZJGK3b29jmVrut8Z2QLqVaunPUJrPzfzwsMPczDBrgXnQraM+u7V1vdkOjI53IXkj9njlrnIxqjW+bROZQWVDqjOFNq5vLyoj1BJdQh6L3nHFuq8kbLj+wwjFLpuyaWasrUqdUxqnTI7GPqTT+SizVAMyolFSqjNzVIzKsA3EY+rFk1PKAMUfWX3VT8Xxmu6mPuMJb51S5BA2gI4W/EoMImlKeZOOb0jP8g/yt70iNM04ibcuqLPao5R6TlBUS+o5xt7LkkD6NE00rAk4agmN4yjWgs9xa9ez4Jfqrv76TX8M+me09pW7oW5rzRuinJGGMmhABw4PV/ZH4C/CWblIpA8qje/grcbGLVPBQG4F/ST6Zv1l//Nw26ViyS8SZIL3hOT3RfVACHZlsLALlSQoGgPNAMCQisqCjlV6qUmaWWVVYxj1JoRkUz+HZRBZMnS9nln2NHVXS5Z+Xh3PgZrLsAxwAAoLVQupFjNWTJApIsYsSm0NYwzzOpKfJ6lOtcnbgJzP7dP1cObv8IHa6AD8jDznV94s5A2DKgXzPtJ5M7jWwFj4BiVc4HoIs9uUxd4JcDXrVvKMvSVe226ybWz9XnVl+WKqcOjkfXpdqVaFYbmOJjs1crYOfOh2Q6RDEcutzicKyuRnflcbMIi3I7OePNsvp/h+pX9PyQ4v3oMAHWGa6AiXHKrhHOOAyzpB6U073LBjzk0G6EQ3K4opn5Pqt8/L9leMSRKxAkYbxDTLRm4XM/wdl55a1IamYrs4iq+lq3ptjCi1wkN0T3Afk+M3DsnXWR/Lawlck5abED33p6PpS/gwuyExvkiEXHrIg9pVPlhSHB+jqa+Y+tYFPVqDdhxjTawStAmN1DZ5QW6ZvkzcXt0p1pkpGghHVfmKHvPvL2zIjs0CGYt/GOIFrzM9pNlhoHACvz/H6gpQExYxiA3WMU2m54J+WzY9/qAlama6U4WBEtp/cqd/pe0vmVC2UEVBoCCZnMpYItedqkt2Ko7Q6pgODacmjZIdlrQglxbvmxRkvwNJqGQqG5dOTO4WgjTWL5LYeNiRSwt0Fc3BXayFz4WqKj3mh8jtdP92PC6KhQJl0e+LQERA9UjhY8ZuLhwyJHLz4rXHv7OqFMhgPia/D8hcG9RhwzeaAnQwoMzhi1KPj2OxWeRvjOyZFLTUlgPNOYMJc9rsZLXOwmYjpcByoALRRYml+U85i/ERupetFQ/GW7JrujQbEccId8R21tuhavMdq6uHQ9PQ8VGoAwsjIEEglQkz/A5QmBA6uRfZhbhxtpy5HjjZ5V1b3XYfODRyApGGY5e1CfOwEg+Rjincy72OAktbs5L1Pu5eT7EJ4x4Bg1PuXXz7/u6D3otsKHG1anxq0PoMhrxoayH6rGB5fxhn3AySc1WvaGae6XOtDxWzAINmW7vVszNAjkXoYBh+/VgkXUKB0TCvzK+xRuddzqgDKZi/fb7FvB6atYDbbBfCe5mgwake8BX9sgXjqoHAAWmJSy6KY9/yYBSSHhtxLHdHtqL24wzSu5/kTkmhz2fxhdotpidjQj8Rkd8bLEgEvFfvO0FtoWAgwB5e9oEKyGN0wsLbnM7Mu0VPbbWY08UiAT/l8Zm0ce+vg2mZ2KaZh4EXGf2F4o0HI84YGcQwSEcYvgdf3OcPhjiuvImkpCZ3mG49ii/OjPD/b3mWdJjnYl7FFYrcC3p6N8nkEXq3jcijI4v0aYM+RBUlIhX/aUDHS1GkTNa879/svgBoYXqGG0awAAAAASUVORK5CYII=";
		String[] tmp = bild.split(",");
		Bitmap buffimage = PluginUtil.getBitmapFromBase64encodedImage(tmp[1]);
		buffimage = PluginUtil.scaleBitmapForDevice(buffimage);
		int h = buffimage.getHeight();
		int w = buffimage.getWidth();
		Bitmap image = Bitmap.createScaledBitmap(buffimage, w / 17, h / 17, true);
		ImageView imageView = new ImageView(this.activity);
		//imageView.setAdjustViewBounds(true);
		imageView.setImageBitmap(image);
		//windowLayer.setOrientation(LinearLayout.HORIZONTAL);

		String bild2 = "data:image/;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAAeCAYAAAA7MK6iAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAAACXBIWXMAALyzAAC8swGSQqikAAABy2lUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iWE1QIENvcmUgNS40LjAiPgogICA8cmRmOlJERiB4bWxuczpyZGY9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyMiPgogICAgICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIgogICAgICAgICAgICB4bWxuczp0aWZmPSJodHRwOi8vbnMuYWRvYmUuY29tL3RpZmYvMS4wLyIKICAgICAgICAgICAgeG1sbnM6eG1wPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvIj4KICAgICAgICAgPHRpZmY6T3JpZW50YXRpb24+MTwvdGlmZjpPcmllbnRhdGlvbj4KICAgICAgICAgPHhtcDpDcmVhdG9yVG9vbD53d3cuaW5rc2NhcGUub3JnPC94bXA6Q3JlYXRvclRvb2w+CiAgICAgIDwvcmRmOkRlc2NyaXB0aW9uPgogICA8L3JkZjpSREY+CjwveDp4bXBtZXRhPgoE1OjLAAAAJ0lEQVRIDe3QMQEAAADCoPVP7W8GiEBhwIABAwYMGDBgwIABAwZ+YA4uAAGtLNHUAAAAAElFTkSuQmCC";
		String[] tmp2 = bild2.split(",");
		Bitmap buffimage2 = PluginUtil.getBitmapFromBase64encodedImage(tmp2[1]);
		buffimage2 = PluginUtil.scaleBitmapForDevice(buffimage2);
		int h2 = buffimage2.getHeight();
		int w2 = buffimage2.getWidth();
		Bitmap image2 = Bitmap.createScaledBitmap(buffimage2, w / 80 , h / 80, true);
		ImageView imageView2 = new ImageView(this.activity);
		//imageView.setAdjustViewBounds(true);
		imageView2.setImageBitmap(image2);

		windowLayer2.addView(windowLayer);
		windowLayer3.addView(imageView2);
		windowLayer3.addView(imageView);
		windowLayer2.addView(windowLayer3);




		//TEST
		return windowLayer2;
	}
}
