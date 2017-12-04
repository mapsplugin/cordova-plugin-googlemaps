package plugin.google.maps;

import android.os.Bundle;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class PluginKmlOverlay extends MyPlugin implements MyPluginInterface {

  private enum KML_TAG {
    kml,
    style,
    stylemap,
    linestyle,
    colorstyle,
    polystyle,
    balloonstyle,
    labelstyle,
    iconstyle,

    linestring,
    outerboundaryis,
    innerboundaryis,
    placemark,
    point,
    polygon,
    pair,
    multigeometry,
    networklink,
    link,
    groundoverlay,
    latlonbox,
    folder,
    document,

    key,
    styleurl,
    color,
    heading,
    scale,
    outline,
    width,
    fill,
    hotspot,
    name,
    description,
    icon,
    href,
    north,
    south,
    east,
    west,
    visibility,
    open,
    address,
    data,
    displayName,
    value,

    bgcolor,
    textcolor,
    text,

    coordinates
  }

  /**
   * Create kml overlay
   *
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void create(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    final JSONObject opts = args.getJSONObject(1);
    self = this;
    if (!opts.has("url")) {
      callbackContext.error("No kml file is specified");
      return;
    }

    String urlStr = null;

    try {
      urlStr = opts.getString("url");
    } catch (JSONException e) {
      e.printStackTrace();
    }
    if (urlStr == null || urlStr.length() == 0) {
      callbackContext.error("No kml file is specified");
      return;
    }

    if (!urlStr.contains("://") &&
        !urlStr.startsWith("/") &&
        !urlStr.startsWith("www/") &&
        !urlStr.startsWith("data:image") &&
        !urlStr.startsWith("./") &&
        !urlStr.startsWith("../")) {
      urlStr = "./" + urlStr;
    }
    if (urlStr.startsWith("./")  || urlStr.startsWith("../")) {
      urlStr = urlStr.replace("././", "./");
      String currentPage = CURRENT_PAGE_URL;
      currentPage = currentPage.replaceAll("[^\\/]*$", "");
      urlStr = currentPage + "/" + urlStr;
    }
    if (urlStr.startsWith("cdvfile://")) {
      urlStr = PluginUtil.getAbsolutePathFromCDVFilePath(webView.getResourceApi(), urlStr);
    }

    Bundle result = loadKml(urlStr);
    callbackContext.success(PluginUtil.Bundle2Json(result));
  }

  private Bundle loadKml(String urlStr) {

    InputStream inputStream = getKmlContents(urlStr);
    if (inputStream == null) {
      return null;
    }
    try {
      XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
      parser.setInput(inputStream, null);
      Bundle kmlData = parseXML(parser);

      inputStream.close();
      inputStream = null;
      parser = null;
      return kmlData;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }


  private InputStream getKmlContents(String urlStr) {

    InputStream inputStream;
    try {
      if (urlStr.startsWith("http://") || urlStr.startsWith("https://")) {
        Log.d("PluginKmlOverlay", "---> url = " + urlStr);
        URL url = new URL(urlStr);
        boolean redirect = true;
        HttpURLConnection http = null;
        String cookies = null;
        int redirectCnt = 0;
        while(redirect && redirectCnt < 10) {
          redirect = false;
          http = (HttpURLConnection)url.openConnection();
          http.setRequestMethod("GET");
          if (cookies != null) {
            http.setRequestProperty("Cookie", cookies);
          }
          http.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
          http.addRequestProperty("User-Agent", "Mozilla");
          http.setInstanceFollowRedirects(true);
          HttpURLConnection.setFollowRedirects(true);

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
            url = new URL(http.getHeaderField("Location"));

            // get the cookie if need, for login
            cookies = http.getHeaderField("Set-Cookie");

            // Disconnect the current connection
            http.disconnect();
            redirectCnt++;
          }
        }

        inputStream = http.getInputStream();
      } else if (urlStr.indexOf("file://") == 0 && !urlStr.contains("file:///android_asset/") ||
          urlStr.indexOf("/") == 0) {
        urlStr = urlStr.replace("file://", "");
        try {
          boolean isAbsolutePath = urlStr.startsWith("/");
          File relativePath = new File(urlStr);
          urlStr = relativePath.getCanonicalPath();
          //Log.d(TAG, "imgUrl = " + imgUrl);
          if (!isAbsolutePath) {
            urlStr = urlStr.substring(1);
          }
          //Log.d(TAG, "imgUrl = " + imgUrl);
        } catch (Exception e) {
          e.printStackTrace();
        }
        Log.d("PluginKmlOverlay", "---> url = " + urlStr);
        inputStream = new FileInputStream(urlStr);
      } else {
        if (urlStr.indexOf("file:///android_asset/") == 0) {
          urlStr = urlStr.replace("file:///android_asset/", "");
        }


        try {
          boolean isAbsolutePath = urlStr.startsWith("/");
          File relativePath = new File(urlStr);
          urlStr = relativePath.getCanonicalPath();
          //Log.d(TAG, "imgUrl = " + imgUrl);
          if (!isAbsolutePath) {
            urlStr = urlStr.substring(1);
          }
          //Log.d(TAG, "imgUrl = " + imgUrl);
        } catch (Exception e) {
          e.printStackTrace();
        }
        Log.d("PluginKmlOverlay", "---> url = " + urlStr);
        inputStream = cordova.getActivity().getResources().getAssets().open(urlStr);
      }

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }

    return inputStream;

  }


  private Bundle parseXML(XmlPullParser parser) throws XmlPullParserException,IOException
  {
    int eventType = parser.getEventType();
    Bundle result = new Bundle();
    ArrayList<Bundle> nodeStack = new ArrayList<Bundle>();
    Bundle styles = new Bundle();

    Bundle parentNode;
    ArrayList<Bundle> childElements = null;
    ArrayList<ArrayList<Bundle>> childElementsStack = new ArrayList<ArrayList<Bundle>>();
    KML_TAG kmlTag;
    String tagName, styleId, attrName, attrValue;
    int nodeIndex;
    ArrayList<String> styleIDs;

    Bundle currentNode = new Bundle();
    result.putBundle("root", currentNode);

    int i, attrCnt = 0;



    while (eventType != XmlPullParser.END_DOCUMENT){
      kmlTag = null;
      switch (eventType){
        case XmlPullParser.START_DOCUMENT:
          break;
        case XmlPullParser.START_TAG:
          tagName = parser.getName().toLowerCase(Locale.US);
          try {
            kmlTag = KML_TAG.valueOf(tagName);
          } catch(Exception e) {
            Log.e("AsyncKmlParser", "---> tagName = " + tagName + " is not supported in this plugin.");
            // ignore
            //e.printStackTrace();
          }

          if (kmlTag == null) {
            eventType = parser.next();
            continue;
          }

          Log.d("AsyncKmlParser", "---> tagName = " + tagName );
          switch (kmlTag) {
            case stylemap:
            case style:
            case colorstyle:
            case linestyle:
            case labelstyle:
            case polystyle:
            case iconstyle:
              //push
              nodeStack.add(currentNode);

              currentNode = new Bundle();
              currentNode.putString("tagName", tagName);
              styleId = parser.getAttributeValue(null, "id");
              if (styleId == null || "null".equals(styleId)) {
                styleId = "__" + currentNode.hashCode() + "__";
              }
              currentNode.putString("styleId", styleId);


              childElementsStack.add(childElements);
              childElements = new ArrayList<Bundle>();
              break;
            case styleurl:
              if (!currentNode.containsKey("styleIDs")) {
                styleIDs = new ArrayList<String>();
                currentNode.putStringArrayList("styleIDs", styleIDs);
              } else {
                styleIDs = currentNode.getStringArrayList("styleIDs");
              }
              styleId = parser.nextText();
              styleIDs.add(styleId);
              currentNode.putStringArrayList("styleIDs", styleIDs);
              break;
            case pair:
            case multigeometry:
              if (currentNode != null) {
                //push
                nodeStack.add(currentNode);

                currentNode = new Bundle();
                currentNode.putString("tagName", tagName);

                childElementsStack.add(childElements);
                childElements = new ArrayList<Bundle>();
              }
              break;
            case networklink:
            case placemark:
            case groundoverlay:
              //push
              nodeStack.add(currentNode);

              currentNode = new Bundle();
              currentNode.putString("tagName", tagName);

              break;
            case link:
            case point:
            case linestring:
            case outerboundaryis:
            case innerboundaryis:
            case polygon:
            case icon:
            case folder:
            case document:
            case balloonstyle:
            case latlonbox:
              if (currentNode != null) {
                //push
                nodeStack.add(currentNode);

                currentNode = new Bundle();
                currentNode.putString("tagName", tagName);
              }
              break;
            case hotspot:
              if (currentNode != null) {
                //push
                nodeStack.add(currentNode);

                currentNode = new Bundle();
                currentNode.putString("tagName", tagName);

                attrCnt = parser.getAttributeCount();
                for (i = 0; i < attrCnt; i++) {
                  attrName = parser.getAttributeName(i);
                  attrValue = parser.getAttributeValue(i);
                  currentNode.putString(attrName, attrValue);
                }

              }
              break;
            case visibility:
            case north:
            case east:
            case west:
            case south:
            case href:
            case key:
            case name:
            case width:
            case color:
            case heading:
            case scale:
            case outline:
            case fill:
            case bgcolor:
            case textcolor:
            case text:
            case description:
              if (currentNode != null) {
                currentNode.putString(tagName, parser.nextText());
              }
              break;

            case coordinates:
              if (currentNode != null) {

                ArrayList<Bundle> latLngList = new ArrayList<Bundle>();

                String txt = parser.nextText();
                txt = txt.replaceAll("\\s+", "\n");
                txt = txt.replaceAll("\\n+", "\n");
                String lines[] = txt.split("\\n");
                String tmpArry[];
                Bundle latLng;
                for (i = 0; i < lines.length; i++) {
                  lines[i] = lines[i].replaceAll("[^0-9,.\\-]", "");
                  if (!"".equals(lines[i])) {
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

            tagName = parser.getName().toLowerCase(Locale.US);
            kmlTag = null;
            try {
              kmlTag = KML_TAG.valueOf(tagName);
            } catch(Exception e) {
              Log.e("AsyncKmlParser", "---> tagName = " + tagName + " is not supported in this plugin.");
              //e.printStackTrace();
            }

            if (kmlTag == null) {
              eventType = parser.next();
              continue;
            }

            switch (kmlTag) {

              case stylemap:
              case style:
              case colorstyle:
              case linestyle:
              case labelstyle:
              case polystyle:
              case iconstyle:
                styleId = "#" + currentNode.getString("styleId");
                currentNode.remove("styleId");
                Log.d(TAG, "---> " + currentNode);
                if (currentNode.containsKey("children")) {
                  currentNode.getParcelableArrayList("children").addAll(childElements);
                } else {
                  currentNode.putParcelableArrayList("children", childElements);
                }

                styles.putBundle(styleId, currentNode);

                childElementsStack.remove(childElementsStack.size() - 1);
                childElements = childElementsStack.get(childElementsStack.size() - 1);

                //pop
                nodeIndex = nodeStack.size() - 1;
                parentNode = nodeStack.get(nodeIndex);
                nodeStack.remove(nodeIndex);
                currentNode = parentNode;

                if (!currentNode.containsKey("styleIDs")) {
                  styleIDs = new ArrayList<String>();
                  currentNode.putStringArrayList("styleIDs", styleIDs);
                } else {
                  styleIDs = currentNode.getStringArrayList("styleIDs");
                }
                styleIDs.add(styleId);

                currentNode.putStringArrayList("styleIDs", styleIDs);
                break;
              case pair:
                if (currentNode != null) {
                  currentNode.putString("tagName", tagName);
                  childElements.add(currentNode);
                  //pop
                  nodeIndex = nodeStack.size() - 1;
                  parentNode = nodeStack.get(nodeIndex);
                  if (parentNode.containsKey("children")) {
                    parentNode.getParcelableArrayList("children").addAll(childElements);
                  } else {
                    parentNode.putParcelableArrayList("children", childElements);
                  }
                  nodeStack.remove(nodeIndex);
                  currentNode = parentNode;

                  childElementsStack.remove(childElementsStack.size() - 1);
                  childElements = childElementsStack.get(childElementsStack.size() - 1);

                }
                break;
              case multigeometry:
                if (currentNode != null) {
                  childElements.add(currentNode);
                  //pop
                  nodeIndex = nodeStack.size() - 1;
                  parentNode = nodeStack.get(nodeIndex);
                  parentNode.putParcelableArrayList("children", childElements);
                  parentNode.putString("tagName", tagName);
                  nodeStack.remove(nodeIndex);
                  currentNode = parentNode;

                  childElementsStack.remove(childElementsStack.size() - 1);
                  childElements = childElementsStack.get(childElementsStack.size() - 1);

                }
                break;

              case networklink:
              case placemark:
              case groundoverlay:
              case document:
              case folder:
              case latlonbox:
              case icon:
              case point:
              case outerboundaryis:
              case innerboundaryis:
              case link:
              case hotspot:
              case linestring:
              case coordinates:
              case polygon:
                if (currentNode != null) {
                  //pop
                  nodeIndex = nodeStack.size() - 1;
                  parentNode = nodeStack.get(nodeIndex);
                  nodeStack.remove(nodeIndex);

                  if (parentNode.containsKey("children")) {
                    parentNode.getParcelableArrayList("children").add(currentNode);
                  } else {
                    childElementsStack.add(childElements);
                    childElements = new ArrayList<Bundle>();
                    childElements.add(currentNode);
                    parentNode.putParcelableArrayList("children", childElements);
                  }
                  currentNode = parentNode;

                }
                break;
                /*
              case iconstyle:
                if (currentNode != null) {
                  //pop
                  nodeIndex = nodeStack.size() - 1;
                  parentNode = nodeStack.get(nodeIndex);
                  nodeStack.remove(nodeIndex);

                  if (parentNode.containsKey("children")) {
                    parentNode.getParcelableArrayList("children").add(currentNode);
                  } else {
                    childElementsStack.add(childElements);
                    childElements = new ArrayList<Bundle>();
                    childElements.add(currentNode);
                    parentNode.putParcelableArrayList("children", childElements);
                  }
                  currentNode = parentNode;

                  childElementsStack.remove(childElementsStack.size() - 1);
                  childElements = childElementsStack.get(childElementsStack.size() - 1);

                }
                break;
                */
              default:
                break;
            }
          }
          break;
      }
      eventType = parser.next();
    }
    //result.putParcelableArrayList("placeMarks", placeMarks);

    result.putBundle("styles", styles);
    return result;
  }
}
