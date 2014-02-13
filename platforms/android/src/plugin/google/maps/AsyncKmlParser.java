package plugin.google.maps;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class AsyncKmlParser extends AsyncTask<String, Void, Bundle> {
  private XmlPullParser parser;
  private GoogleMaps mMapCtrl;
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
    icon,
    href,
    
    coordinates
  };
  
  public AsyncKmlParser(Activity activity, GoogleMaps mapCtrl, CallbackContext callbackContext) {
    mCallback = callbackContext;
    mMapCtrl = mapCtrl;
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
      InputStream inputStream = null;
      String urlStr = params[0];
      if (urlStr.startsWith("http://") || urlStr.startsWith("https://")) {
        Log.e("Map", urlStr);
        URL url = new URL(urlStr);
        HttpURLConnection http = (HttpURLConnection)url.openConnection(); 
        http.setRequestMethod("GET");
        http.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
        http.addRequestProperty("User-Agent", "Mozilla");
        http.setInstanceFollowRedirects(true);
        HttpURLConnection.setFollowRedirects(true);
        
        boolean redirect = false;
        // normally, 3xx is redirect
        int status = http.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
          if (status == HttpURLConnection.HTTP_MOVED_TEMP
            || status == HttpURLConnection.HTTP_MOVED_PERM
              || status == HttpURLConnection.HTTP_SEE_OTHER)
          redirect = true;
        }
        if (redirect) {
          
          // get redirect url from "location" header field
          String newUrl = http.getHeaderField("Location");
       
          // get the cookie if need, for login
          String cookies = http.getHeaderField("Set-Cookie");
       
          // open the new connnection again
          http = (HttpURLConnection) new URL(newUrl).openConnection();
          http.setRequestProperty("Cookie", cookies);
          http.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
          http.addRequestProperty("User-Agent", "Mozilla");
        }
        
        inputStream = http.getInputStream();
      } else {
        inputStream = mActivity.getResources().getAssets().open(urlStr);
      }
      
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
    
    Bundle options;
    JSONObject optionsJSON;
    
    Random random = new Random();
    String kmlId = "kml" + random.nextInt();
    String tmp, tagName;
    Bundle node, style, childNode;
    ArrayList<Bundle> bundleList;
    ArrayList<Bundle> children;
    ArrayList<Bundle> latLngList;
    Iterator<Bundle> iterator = placeMarks.iterator();
    Iterator<Bundle> bundleIterator;
    Iterator<Bundle> childrenIterator;
    while(iterator.hasNext()) {
      node = iterator.next();

      children = node.getParcelableArrayList("children");
      if (children == null) {
        continue;
      }
      childrenIterator = children.iterator();
      while(childrenIterator.hasNext()) {
        childNode = childrenIterator.next();
        
        tagName = childNode.getString("tagName");
        switch(KML_TAG.valueOf(tagName)) {
        case point:
          //-----------------
          // Marker
          //-----------------
          options = new Bundle();
          //position
          latLngList = childNode.getParcelableArrayList("coordinates");
          options.putBundle("position", latLngList.get(0));
          
          //title
          tmp = node.getString("name");
          if (node.containsKey("description")) {
            tmp += "\n\n" + node.getString("description");
          }
          options.putString("title", tmp);
          
          //icon
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
              if ("icon".equals(tagName)) {;
                options.putString("icon", style.getString("href"));
              }
            }
          }
          this.implementToMap("Marker", options, kmlId);
          break;
          
        case linestring:
          //-----------------
          // Polyline
          //-----------------
          
          //points
          options = new Bundle();
          latLngList = childNode.getParcelableArrayList("coordinates");
          options.putParcelableArrayList("points", latLngList);

          options.putBoolean("visible", true);
          options.putBoolean("geodesic", true);
          
          //Bundle -> JSON
          optionsJSON = PluginUtil.Bundle2Json(options);
          
          //color, width
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
                  try {
                    optionsJSON.put("color", kmlColor2PluginColor(style.getString("color")));
                  } catch (JSONException e) {}
                }
                if (style.containsKey("width")) {
                  try {
                    optionsJSON.put("width", (int) (Integer.parseInt(style.getString("width")) * density));
                  } catch (Exception e) {}
                }
                break;
              }
            }
          }
          this.implementToMap("Polyline", optionsJSON, kmlId);
          break;
          

        case polygon:
          //-----------------
          // Polygon
          //-----------------
          children = childNode.getParcelableArrayList("children");
          childNode = children.get(0);
          
          options = new Bundle();
          latLngList = childNode.getParcelableArrayList("coordinates");
          options.putParcelableArrayList("points", latLngList);

          options.putBoolean("visible", true);
          options.putInt("strokeWidth", 0);
          
          //Bundle -> JSON
          optionsJSON = PluginUtil.Bundle2Json(options);

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
                  try {
                    optionsJSON.put("fillColor", kmlColor2PluginColor(style.getString("color")));
                  } catch (JSONException e) {}
                }
                break;
              case linestyle:
                if (style.containsKey("color")) {
                  try {
                    optionsJSON.put("strokeColor", kmlColor2PluginColor(style.getString("color")));
                  } catch (JSONException e) {};
                }
                if (style.containsKey("width")) {
                  try {
                    optionsJSON.put("strokeWidth", (int)Float.parseFloat(style.getString("width")) * density);
                  } catch (Exception e) {}
                }
                break;
              }
            }
          } else {
            Log.e("client", "--" + style + " is null");
          }
          this.implementToMap("Polygon", optionsJSON, kmlId);
          break;
          
        }
      }
      
    }
    this.mCallback.success(kmlId);
  }

  private void implementToMap(String className, JSONObject optionsJSON, String kmlId) {

    JSONArray params = new JSONArray();
    params.put(className + ".create" + className);
    params.put(optionsJSON);
    params.put(kmlId + "-");
    CallbackContext callback2 = new CallbackContext(kmlId + "_callback", this.mMapCtrl.webView);
    
    try {
      mMapCtrl.execute("exec", params, callback2);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }
  private void implementToMap(String className, Bundle options, String kmlId) {
    // Load the class plugin
    this.implementToMap(className, PluginUtil.Bundle2Json(options), kmlId);
  }
  
  private JSONArray kmlColor2PluginColor(String colorStr) {
    JSONArray rgba = new JSONArray();
    for (int i = 2; i < 8; i+=2) {
      rgba.put(Integer.parseInt(colorStr.substring(i, i + 2), 16));
    }
    rgba.put(Integer.parseInt(colorStr.substring(0, 2), 16));
    return rgba;
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
            if (currentNode != null) {
              //push
              nodeStack.add(currentNode);
              
              currentNode = new Bundle();
              currentNode.putString("tagName", tagName);
              pairList = new ArrayList<Bundle>();
            }
            break;
          case placemark:
            currentNode = new Bundle();
            currentNode.putString("tagName", tagName);
            pairList = null;
            break;
          case linestyle:
          case polystyle:
          case pair:
          case point:
          case linestring:
          case outerboundaryis:
          case polygon:
          case icon:
            if (currentNode != null) {
              //push
              nodeStack.add(currentNode);
              
              currentNode = new Bundle();
              currentNode.putString("tagName", tagName);
            }
            break;
          case href:
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

              ArrayList<Bundle> latLngList = new ArrayList<Bundle>();
              
              String txt = parser.nextText();
              String lines[] = txt.split("[\\n\\s]");
              String tmpArry[];
              Bundle latLng;
              int i;
              for (i = 0; i < lines.length; i++) {
                lines[i] = lines[i].replaceAll("[^0-9,.\\-]", "");
                if ("".equals(lines[i]) == false) {
                  tmpArry = lines[i].split(",");
                  latLng = new Bundle();
                  latLng.putFloat("lat", Float.parseFloat(tmpArry[1]));
                  latLng.putFloat("lng", Float.parseFloat(tmpArry[0]));
                  latLngList.add(latLng);
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
              if (currentNode != null) {
                //pop
                nodeIndex = nodeStack.size() - 1;
                parentNode = nodeStack.get(nodeIndex);
                parentNode.putParcelableArrayList("children", pairList);
                parentNode.putString("tagName", tagName);
                nodeStack.remove(nodeIndex);
                currentNode = parentNode;
                pairList = null;
              }
              break;
            case placemark:
              placeMarks.add(currentNode);
              currentNode = null;
              break;
            case pair:
            case linestyle:
            case polystyle:
              if (currentNode != null) {
                pairList.add(currentNode);
  
                //pop
                nodeIndex = nodeStack.size() - 1;
                parentNode = nodeStack.get(nodeIndex);
                nodeStack.remove(nodeIndex);
                currentNode = parentNode;
              }
              break;
            case icon:
            case point:
            case outerboundaryis:
            case linestring:
            case coordinates:
            case polygon:
              if (currentNode != null) {
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
              }
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
