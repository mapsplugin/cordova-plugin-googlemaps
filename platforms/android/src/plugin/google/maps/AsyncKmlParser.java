package plugin.google.maps;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.cordova.CallbackContext;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class AsyncKmlParser extends AsyncTask<String, Void, ArrayList<Bundle>> {
  private XmlPullParser parser;
  private GoogleMap mMap;
  private Activity mActivity;
  private CallbackContext mCallback;
  
  private enum KML_TAG {
    style,
    linestyle,
    _polystyle,
    linestring,
    outerboundaryis,
    placemark,
    point,
    polygon,
    
    color,
    width,
    fill,
    name,
    styleurl,
    description,
    
    coordinates
  };
  
  public AsyncKmlParser(Activity activity, GoogleMap map, CallbackContext callbackContext) {
    mCallback = callbackContext;
    mMap = map;
    mActivity = activity;
    
    try {
      parser = XmlPullParserFactory.newInstance().newPullParser();
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.toString());
    }
  }
  @Override
  protected ArrayList<Bundle> doInBackground(String... params) {
    
    ArrayList<Bundle> kmlData = null;
    try {
      StringBuffer buf = new StringBuffer();
      InputStream inputStream = mActivity.getResources().getAssets().open(params[0]);
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      String line;
      
      //XMLPullParser causes an error when the kml contains <PolyStyle>
      while((line = reader.readLine()) != null) {
        if (line.indexOf("PolyStyle>") > -1) {
          line = line.replaceAll("PolyStyle>", "_PolyStyle>");
        }
        buf.append(line);
      }
      //parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
      parser.setInput(new ByteArrayInputStream(buf.toString().getBytes()), null);
      kmlData = parseXML(parser);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    
    
    return kmlData;
  }
  protected void onPostExecute(ArrayList<Bundle> kmlData) {
    if (kmlData == null) {
      mCallback.error("KML Parse error");
      return;
    }

    HashMap<String, Bundle> styles = new HashMap<String, Bundle>();
    
    String tmp;
    Bundle node;
    Bundle childNode;
    Bundle style;
    ArrayList<LatLng> latLngList;
    Iterator<Bundle> iterator = kmlData.iterator();
    while(iterator.hasNext()) {
      node = iterator.next();
      Log.d("client", node.toString());

      String tagName = node.getString("tagName");
      switch (KML_TAG.valueOf(tagName)) {
      case style:
        tmp = node.getString("id");
        if (tmp == null) {
          tmp = "__default__";
        }
        styles.put("#" + tmp, node.getBundle("child"));
        break;

      case polygon:
        childNode = node.getBundle("child");
        //coordinates = childNode.getFloatArray("coordinates");

        childNode = node.getBundle("child");
        tagName = childNode.getString("tagName");
        
        switch(KML_TAG.valueOf(tagName)) {
        case outerboundaryis:
          PolygonOptions polygonOptions = new PolygonOptions();
          latLngList = childNode.getParcelableArrayList("coordinates");
          polygonOptions.addAll(latLngList);

          if (node.containsKey("styleurl")) {
            tmp = node.getString("styleurl");
          } else {
            tmp = "#__default__";
          }
          if (styles.containsKey(tmp)) {
            style = styles.get(tmp);
            if (style.containsKey("color")) {
              int color = Color.parseColor("#" + style.getString("color"));
              polygonOptions.strokeColor(color);
              if ("1".equals(style.getString("fill"))) {
                polygonOptions.fillColor(color);
              }
            }
            if (style.containsKey("width")) {
              polygonOptions.strokeWidth(Float.parseFloat(style.getString("width")));
            }
          } else {
            Log.e("client", tmp + " is not found");
          }
          mMap.addPolygon(polygonOptions);
        }
        break;
      case placemark:
        childNode = node.getBundle("child");
        tagName = childNode.getString("tagName");
        Log.d("client", childNode.toString());
        
        switch(KML_TAG.valueOf(tagName)) {
        case point:
          latLngList = childNode.getParcelableArrayList("coordinates");
          MarkerOptions markerOptions = new MarkerOptions();
          tmp = node.getString("name");
          if (node.containsKey("description")) {
            tmp += "\n\n" + node.getString("description");
          }
          markerOptions.title(tmp);
          markerOptions.position(latLngList.get(0));
          mMap.addMarker(markerOptions);
          
          break;
          
        case linestring:
          
          PolylineOptions polylineOptions = new PolylineOptions();
          latLngList = childNode.getParcelableArrayList("coordinates");
          polylineOptions.addAll(latLngList);
          Polyline polyline = mMap.addPolyline(polylineOptions);

          if (node.containsKey("styleurl")) {
            tmp = node.getString("styleurl");
          } else {
            tmp = "#__default__";
          }
          if (styles.containsKey(tmp)) {
            style = styles.get(tmp);
            if (style.containsKey("color")) {
              polyline.setColor(Color.parseColor("#" + style.getString("color")));
            }
            if (style.containsKey("width")) {
              polyline.setWidth(Integer.parseInt(style.getString("width")));
            }
          }
          break;
        }
        
        break;
      }
    }
    this.mCallback.success();
  }
  
  private ArrayList<Bundle> parseXML(XmlPullParser parser) throws XmlPullParserException,IOException
  {
    ArrayList<Bundle> kmlData = new ArrayList<Bundle>();
    int eventType = parser.getEventType();
    Bundle currentNode = null;
    ArrayList<Bundle> nodeStack = new ArrayList<Bundle>();
    
    Bundle parentNode = null;
    KML_TAG kmlTag = null;
    String tagName = null;
    String tmp;
    int nodeIndex = 0;
    while (eventType != XmlPullParser.END_DOCUMENT){
      tagName = null;
      kmlTag = null;
      switch (eventType){
        case XmlPullParser.START_DOCUMENT:
          break;
        case XmlPullParser.START_TAG:
          tagName = parser.getName().toLowerCase();
          try {
            kmlTag = KML_TAG.valueOf(tagName);
          } catch(Exception e) {}
          
          if (kmlTag == null) {
            eventType = parser.next();
            continue;
          }
          
          switch (kmlTag) {
          case style:
            currentNode = new Bundle();
            currentNode.putString("tagName", tagName);
            tmp = parser.getAttributeValue(null, "id");
            if (tmp == null) {
              tmp = "__default__";
            }
            currentNode.putString("id", tmp);
            break;
          case polygon:
          case placemark:
            currentNode = new Bundle();
            currentNode.putString("tagName", tagName);
            break;
          case point:
          case linestring:
          case linestyle:
          case outerboundaryis:
          case _polystyle:
            //push
            nodeStack.add(currentNode);
            
            currentNode = new Bundle();
            currentNode.putString("tagName", tagName);
            break;
          case name:
          case styleurl:
          case width:
          case color:
          case fill:
          case description:
            if (currentNode != null) {
              currentNode.putString(tagName, parser.nextText());
            }
            break;
          
          case coordinates:
            if (currentNode != null) {

              ArrayList<LatLng> latLngList = new ArrayList<LatLng>();
              
              String txt = parser.nextText();
              String lines[] = txt.split("[\\n\\s]");
              String tmpArry[];
              int i;
              for (i = 0; i < lines.length; i++) {
                lines[i] = lines[i].replaceAll("[^0-9,.\\-]", "");
                if ("".equals(lines[i]) == false) {
                  tmpArry = lines[i].split(",");
                  latLngList.add(new LatLng(Float.parseFloat(tmpArry[1]), Float.parseFloat(tmpArry[0])));
                }
              }
              currentNode.putParcelableArrayList(tagName, latLngList);
            }
            break;
          default:
            break;
          }
          break;
        case XmlPullParser.END_TAG:
          if (currentNode != null) {
            
            tagName = parser.getName().toLowerCase();
            kmlTag = null;
            try {
              kmlTag = KML_TAG.valueOf(tagName);
            } catch(Exception e) {}
            
            if (kmlTag == null) {
              eventType = parser.next();
              continue;
            }
            
            switch (kmlTag) {
            case polygon:
            case placemark:
            case style:
              kmlData.add(currentNode);
              break;
            case point:
            case outerboundaryis:
            case linestring:
            case linestyle:
            case _polystyle:
            case coordinates:
              //pop
              nodeIndex = nodeStack.size() - 1;
              parentNode = nodeStack.get(nodeIndex);
              nodeStack.remove(nodeIndex);
              
              parentNode.putBundle("child", currentNode);
              currentNode = parentNode;
              break;
            default:
              break;
            }
          }
          break;
      }
      eventType = parser.next();
    }
    return kmlData;
  }
}
