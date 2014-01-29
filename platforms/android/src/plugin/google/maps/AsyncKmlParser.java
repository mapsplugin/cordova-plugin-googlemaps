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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class AsyncKmlParser extends AsyncTask<Void, Void, ArrayList<Bundle>> {
  private XmlPullParserFactory pullParserFactory;
  private XmlPullParser parser;
  private GoogleMap mMap;
  private CallbackContext mCallback;
  
  private enum KML_TAG {
    style,
    linestyle,
    linestring,
    placemark,
    
    color,
    width,
    name,
    styleurl,
    
    coordinates
  };
  
  public AsyncKmlParser(Activity activity, GoogleMap map, CallbackContext callbackContext) {
    mCallback = callbackContext;
    mMap = map;
    
    try {
      pullParserFactory = XmlPullParserFactory.newInstance();
      parser = pullParserFactory.newPullParser();
      InputStream in_s = activity.getResources().getAssets().open("www/cta.kml");
      parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
      parser.setInput(in_s, null);

    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error(e.toString());
    }
  }
  @Override
  protected ArrayList<Bundle> doInBackground(Void... arg0) {
    ArrayList<Bundle> kmlData = null;
    try {
      kmlData = parseXML(parser);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    
    
    return kmlData;
  }
  protected void onPostExecute(ArrayList<Bundle> kmlData) {
    if (kmlData == null) {
      mCallback.error("Parse error");
      return;
    }

    Iterator<Bundle> iterator = kmlData.iterator();
    while(iterator.hasNext()) {
      Log.d("KML", iterator.next().toString());
    }
  }
  
  private void implementToMap(Bundle kml) {
    
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
            currentNode.putString("id", parser.getAttributeValue(null, "id"));
            break;
          case placemark:
            currentNode = new Bundle();
            currentNode.putString("tagName", tagName);
            break;
          case linestring:
          case linestyle:
            //push
            nodeStack.add(currentNode);
            
            currentNode = new Bundle();
            currentNode.putString("tagName", tagName);
            break;
          case styleurl:
          case width:
          case color:
            if (currentNode != null) {
              currentNode.putString(tagName, parser.nextText());
            }
            break;
          
          case coordinates:
            if (currentNode != null) {
              String txt = parser.nextText();
              String lines[] = txt.split("\\n");
              ArrayList<Float> coordList = new ArrayList<Float>();
              String tmp[];
              int i;
              for (i = 0; i < lines.length; i++) {
                lines[i] = lines[i].replaceAll("[^0-9,.\\-]", "");
                if ("".equals(lines[i]) == false) {
                  tmp = lines[i].split(",");
                  coordList.add(Float.parseFloat(tmp[0]));
                  coordList.add(Float.parseFloat(tmp[1]));
                  coordList.add(Float.parseFloat(tmp[2]));
                }
              }
              float[] coordinates = new float[coordList.size()];
              for (i = 0; i < coordList.size(); i++) {
                coordinates[i] = coordList.get(i);
              }
              currentNode.putFloatArray(tagName, coordinates);
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
            case placemark:
            case style:
              kmlData.add(currentNode);
              break;
            case linestring:
            case linestyle:
            case coordinates:
              //pop
              nodeIndex = nodeStack.size() - 1;
              parentNode = nodeStack.get(nodeIndex);
              nodeStack.remove(nodeIndex);
              
              parentNode.putBundle(currentNode.getString("tagName"), currentNode);
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
