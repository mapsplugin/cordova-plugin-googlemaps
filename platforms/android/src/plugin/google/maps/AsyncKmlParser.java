package plugin.google.maps;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.cordova.CallbackContext;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.res.Resources;
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

public class AsyncKmlParser extends AsyncTask<String, Void, Bundle> {
  private XmlPullParser parser;
  private GoogleMap mMap;
  private Activity mActivity;
  private CallbackContext mCallback;
  
  private enum KML_TAG {
    style,
    stylemap,
    linestyle,
    polystyle,
    linestring,
    outerboundaryis,
    placemark,
    point,
    polygon,
    pair,
    multigeometry,

    key,
    styleurl,
    color,
    width,
    fill,
    name,
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
  protected Bundle doInBackground(String... params) {
    
    Bundle kmlData = null;
    try {
      InputStream inputStream = mActivity.getResources().getAssets().open(params[0]);
      parser.setInput(inputStream, null);
      kmlData = parseXML(parser);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    
    
    return kmlData;
  }
  
  private Bundle getStyleById(Bundle styles, String styleId) {
    Bundle style = null;
    Bundle tmpBundle;
    String tagName, tmp;
    ArrayList<Bundle> bundleList;
    Iterator<Bundle> bundleIterator;
    if (styles.containsKey(styleId)) {
      style = styles.getBundle(styleId);
      
      tagName = style.getString("tagName");
      if ("stylemap".equals(tagName)) {

        bundleList = style.getParcelableArrayList("children");
        
        bundleIterator = bundleList.iterator();
        while(bundleIterator.hasNext()) {
          tmpBundle = bundleIterator.next();
          if ("normal".equals(tmpBundle.getString("key")) &&
              tmpBundle.containsKey("styleurl")) {
            
            tmp = tmpBundle.getString("styleurl");
            style = styles.getBundle(tmp);
            
            break;
          }
        }
      }
    }
    return style;
  }
  
  protected void onPostExecute(Bundle parseResult) {
    if (parseResult == null) {
      mCallback.error("KML Parse error");
      return;
    }
    Bundle styles = parseResult.getBundle("styles");
    ArrayList<Bundle> placeMarks = parseResult.getParcelableArrayList("placeMarks");
    float density = Resources.getSystem().getDisplayMetrics().density;
    
    
    String tmp, tagName;
    Bundle node, style, childNode, tmpBundle;
    ArrayList<Bundle> bundleList;
    ArrayList<LatLng> latLngList;
    Iterator<Bundle> iterator = placeMarks.iterator();
    Iterator<Bundle> bundleIterator;
    while(iterator.hasNext()) {
      node = iterator.next();

      bundleList = node.getParcelableArrayList("children");
      bundleIterator = bundleList.iterator();
      while(bundleIterator.hasNext()) {
        childNode = bundleIterator.next();
        
        tagName = childNode.getString("tagName");
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
          style = getStyleById(styles, tmp);
          
          if (style != null) {
            bundleList = style.getParcelableArrayList("children");
            bundleIterator = bundleList.iterator();
            while(bundleIterator.hasNext()) {
              style = bundleIterator.next();
              tagName = style.getString("tagName");
              switch(KML_TAG.valueOf(tagName)) {
              case linestyle:
                if (style.containsKey("color")) {
                  polyline.setColor(parseKMLcolor(style.getString("color")));
                }
                if (style.containsKey("width")) {
                  polyline.setWidth(Integer.parseInt(style.getString("width")) * density);
                }
                break;
              }
            }
          }
          break;
          

        case polygon:
          ArrayList<Bundle> children = childNode.getParcelableArrayList("children");
          childNode = children.get(0);
          PolygonOptions polygonOptions = new PolygonOptions();
          latLngList = childNode.getParcelableArrayList("coordinates");
          polygonOptions.addAll(latLngList);
          polygonOptions.strokeWidth(0);
          polygonOptions.strokeColor(Color.RED);
          polygonOptions.strokeWidth(4);
          mMap.addPolygon(polygonOptions);

          if (node.containsKey("styleurl")) {
            tmp = node.getString("styleurl");
          } else {
            tmp = "#__default__";
          }
          style = getStyleById(styles, tmp);
          if (style != null) {
            bundleList = style.getParcelableArrayList("children");
            bundleIterator = bundleList.iterator();
            while(bundleIterator.hasNext()) {
              style = bundleIterator.next();
              tagName = style.getString("tagName");
              switch(KML_TAG.valueOf(tagName)) {
              case polystyle:
                if (style.containsKey("color")) {
                  polygonOptions.fillColor(parseKMLcolor(style.getString("color")));
                }
                break;
              case linestyle:
                if (style.containsKey("color")) {
                  polygonOptions.strokeColor(parseKMLcolor(style.getString("color")));
                }
                if (style.containsKey("width")) {
                  polygonOptions.strokeWidth(Float.parseFloat(style.getString("width")) * density);
                }
                break;
              }
            }
          } else {
            Log.e("client", "--" + style + " is null");
          }
          break;
        }
      }
      
    }
    this.mCallback.success();
  }
  
  private int parseKMLcolor(String colorStr) {
    String tmp = "";
    for (int j = 2; j < colorStr.length() - 1; j+=2) {
      tmp = colorStr.substring(j, j + 2) + tmp;
    }
    tmp = colorStr.substring(0, 2) + tmp;
    Log.i("client", colorStr + " -> " + tmp);

    return Color.parseColor("#" + tmp);
  }
  
  private Bundle parseXML(XmlPullParser parser) throws XmlPullParserException,IOException
  {
    ArrayList<Bundle> placeMarks = new ArrayList<Bundle>();
    int eventType = parser.getEventType();
    Bundle currentNode = null;
    Bundle result = new Bundle();
    ArrayList<Bundle> nodeStack = new ArrayList<Bundle>();
    Bundle styles = new Bundle();
    
    Bundle parentNode = null;
    ArrayList<Bundle> pairList = null;
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
          case stylemap:
          case style:
            //push
            nodeStack.add(currentNode);
            
            currentNode = new Bundle();
            currentNode.putString("tagName", tagName);
            tmp = parser.getAttributeValue(null, "id");
            if (tmp == null) {
              tmp = "__default__";
            }
            currentNode.putString("id", tmp);
            pairList = new ArrayList<Bundle>();
            break;
          case multigeometry:
            //push
            nodeStack.add(currentNode);
            
            currentNode = new Bundle();
            currentNode.putString("tagName", tagName);
            pairList = new ArrayList<Bundle>();
            break;
          case placemark:
            currentNode = new Bundle();
            currentNode.putString("tagName", tagName);
            pairList = null;
            break;
          case linestyle:
          case polystyle:
          case pair:
            if (pairList != null) {
              currentNode.putParcelableArrayList("children", pairList);
              pairList = new ArrayList<Bundle>();
            }
            nodeStack.add(currentNode);
            
            currentNode = new Bundle();
            currentNode.putString("tagName", tagName);
            break;
          case point:
          case linestring:
          case outerboundaryis:
          case polygon:
            //push
            if (pairList != null) {
              //currentNode.putParcelableArrayList("children", pairList);
              //pairList = null;
            }
            nodeStack.add(currentNode);
            
            currentNode = new Bundle();
            currentNode.putString("tagName", tagName);
            break;
          case key:
          case styleurl:
          case name:
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
            case stylemap:
            case style:
              currentNode.putParcelableArrayList("children", pairList);
              styles.putBundle("#" + currentNode.getString("id"), currentNode);
              //pop
              nodeIndex = nodeStack.size() - 1;
              parentNode = nodeStack.get(nodeIndex);
              nodeStack.remove(nodeIndex);
              currentNode = parentNode;
              break;
            case multigeometry:
              //pop
              nodeIndex = nodeStack.size() - 1;
              parentNode = nodeStack.get(nodeIndex);
              parentNode.putParcelableArrayList("children", pairList);
              nodeStack.remove(nodeIndex);
              currentNode = parentNode;
              pairList = null;
              break;
            case placemark:
              placeMarks.add(currentNode);
              break;
            case pair:
            case linestyle:
            case polystyle:
              pairList.add(currentNode);

              //pop
              nodeIndex = nodeStack.size() - 1;
              parentNode = nodeStack.get(nodeIndex);
              nodeStack.remove(nodeIndex);
              currentNode = parentNode;
              break;
            case point:
            case outerboundaryis:
            case linestring:
            case coordinates:
            case polygon:
              //pop
              nodeIndex = nodeStack.size() - 1;
              parentNode = nodeStack.get(nodeIndex);
              nodeStack.remove(nodeIndex);
              
              if (parentNode.containsKey("children")) {
                pairList = parentNode.getParcelableArrayList("children");
                pairList.add(currentNode);
                parentNode.putParcelableArrayList("children", pairList);
              } else {
                pairList = new ArrayList<Bundle>();
                pairList.add(currentNode);
                parentNode.putParcelableArrayList("children", pairList);
              }
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
    result.putParcelableArrayList("placeMarks", placeMarks);
    result.putBundle("styles", styles);
    return result;
  }
}
