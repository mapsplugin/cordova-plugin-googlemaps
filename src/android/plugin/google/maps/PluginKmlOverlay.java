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
    style,
    stylemap,
    linestyle,
    polystyle,
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
    width,
    fill,
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
    ArrayList<Bundle> pairList = null;
    KML_TAG kmlTag;
    String tagName;
    String tmp;
    int nodeIndex;

    Bundle currentNode = new Bundle();
    result.putBundle("root", currentNode);

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
            //Log.d("AsyncKmlParser", "---> tagName = " + tagName + " is not supported in this plugin.");
            // ignore
            //e.printStackTrace();
          }

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
            case networklink:
            case placemark:
            case groundoverlay:
              //push
              nodeStack.add(currentNode);

              currentNode = new Bundle();
              currentNode.putString("tagName", tagName);
              pairList = null;
              break;
            case link:
            case linestyle:
            case polystyle:
            case pair:
            case point:
            case linestring:
            case outerboundaryis:
            case innerboundaryis:
            case polygon:
            case icon:
            case folder:
            case document:
            case latlonbox:
              if (currentNode != null) {
                //push
                nodeStack.add(currentNode);

                currentNode = new Bundle();
                currentNode.putString("tagName", tagName);
              }
              break;
            case visibility:
            case north:
            case east:
            case west:
            case south:
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
                txt = txt.replaceAll("\\s+", "\n");
                txt = txt.replaceAll("\\n+", "\n");
                String lines[] = txt.split("\\n");
                String tmpArry[];
                Bundle latLng;
                int i;
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
              //Log.d("AsyncKmlParser", "---> tagName = " + tagName + " is not supported in this plugin.");
              //e.printStackTrace();
            }

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
    //result.putParcelableArrayList("placeMarks", placeMarks);

    result.putBundle("styles", styles);
    return result;
  }
}
