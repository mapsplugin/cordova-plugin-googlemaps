package plugin.google.maps;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginEntry;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;
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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncKmlParser extends AsyncTask<String, Void, Bundle> {
  private XmlPullParser parser;
  private PluginMap mPluginMap;
  private Activity mActivity;
  private CallbackContext mCallback;
  private String kmlId = null;
  private static ExecutorService executorService = Executors.newCachedThreadPool();

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
    networklink,
    link,

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
  private Bundle mOption = null;

  public AsyncKmlParser(Activity activity, PluginMap pluginMap, String kmlId, Bundle option, CallbackContext callbackContext) {
    this.kmlId = kmlId;
    mCallback = callbackContext;
    mPluginMap = pluginMap;
    mActivity = activity;
    mOption = option;


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
        Log.d("AsyncKmlParser", "---> url = " + urlStr);
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
        Log.d("AsyncKmlParser", "---> url = " + urlStr);
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
        Log.d("AsyncKmlParser", "---> url = " + urlStr);
        inputStream = mActivity.getResources().getAssets().open(urlStr);
      }

      parser.setInput(inputStream, null);
      kmlData = parseXML(parser);
      inputStream.close();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }



/*

    if (kmlData == null) {
      mCallback.error("KML Parse error");
      return null;
    }
    final Bundle styles = kmlData.getBundle("styles");
    ArrayList<Bundle> placeMarks = kmlData.getParcelableArrayList("placeMarks");
    Bundle[] tags = placeMarks.toArray(new Bundle[placeMarks.size()]);
    ExecutorService executor = Executors.newCachedThreadPool();
    List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
    int i = 0;
    for (final Bundle node : tags) {

      final String placeMarkId = "placeMark-" + (i++);
      Callable<Void> callable = new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          parsePlaceMark(placeMarkId, styles, node);
          return null;
        }
      };
      tasks.add(callable);
    }
    try {
      executor.invokeAll(tasks);
    } catch (Exception e) {
      e.printStackTrace();
    }

*/
    return kmlData;
  }

  private void parsePlaceMark(String placeMarkId, Bundle styles, Bundle node) {
    String tmp, tagName;
    Bundle style;
    ArrayList<Bundle> bundleList;
    ArrayList<Bundle> children;
    ArrayList<Bundle> latLngList;
    Bundle options;
    JSONObject optionsJSON, latLngJSON;
    Iterator<Bundle> bundleIterator;
    Iterator<Bundle> childrenIterator;


    tagName = node.getString("tagName");
    if ("networklink".equals(tagName)) {

      bundleList = node.getParcelableArrayList("children");
      bundleIterator = bundleList.iterator();
      while(bundleIterator.hasNext()) {
        final Bundle childNode = bundleIterator.next();
        tagName = childNode.getString("tagName");
        if ("link".equals(tagName)) {
          AsyncKmlParser kmlParser = new AsyncKmlParser(mActivity, mPluginMap, kmlId, mOption, mCallback);
          kmlParser.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, childNode.getString("href"));
          return;
        }
      }
      return;
    }

    children = node.getParcelableArrayList("children");
    if (children == null) {
      return;
    }
    Bundle childNode;
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
          this.implementToMap("Marker", options, kmlId, placeMarkId);
          break;

        case linestring:
          //-----------------
          // Polyline
          //-----------------

          //points
          options = new Bundle();
          latLngList = childNode.getParcelableArrayList("coordinates");
          options.putParcelableArrayList("points", latLngList);

          //add LatLng array to the defaultViewport
          bundleIterator = latLngList.iterator();
          //while(bundleIterator.hasNext()) {
          //  defaultViewport.put(PluginUtil.Bundle2Json(bundleIterator.next()));
          //}

          //latLngJSON = PluginUtil.Bundle2Json(latLngList.get(0));
          //defaultViewport.put(latLngJSON);

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
                    } catch (JSONException e) {
                      e.printStackTrace();
                    }
                  }
                  if (style.containsKey("width")) {
                    try {
                      optionsJSON.put("width", (int) (Integer.parseInt(style.getString("width"))));
                    } catch (Exception e) {
                      e.printStackTrace();
                    }
                  }
                  try {
                    optionsJSON.put("zIndex", 4);
                  } catch (Exception e) {
                    e.printStackTrace();
                  }
                  break;
                default:
                  break;
              }
            }
          }
          this.implementToMap("Polyline", optionsJSON, kmlId, placeMarkId);
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

          //add LatLng array to the defaultViewport
          //bundleIterator = latLngList.iterator();
          //while(bundleIterator.hasNext()) {
          //  defaultViewport.put(PluginUtil.Bundle2Json(bundleIterator.next()));
          //}
          options.putBoolean("visible", true);
          options.putInt("strokeWidth", 4);

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
                    } catch (JSONException e) {
                      e.printStackTrace();
                    }
                  }
                  break;
                case linestyle:
                  if (style.containsKey("color")) {
                    try {
                      optionsJSON.put("strokeColor", kmlColor2PluginColor(style.getString("color")));
                    } catch (JSONException e) {
                      e.printStackTrace();
                    }
                  }
                  if (style.containsKey("width")) {
                    try {
                      optionsJSON.put("strokeWidth", (int)Float.parseFloat(style.getString("width")));
                    } catch (Exception e) {
                      e.printStackTrace();
                    }
                  }
                  break;
                default:
                  break;
              }
            }
          } else {
            Log.e("client", "--  style  is null");
          }
          try {
            optionsJSON.put("zIndex", 2);
          } catch (Exception e) {
            e.printStackTrace();
          }
          this.implementToMap("Polygon", optionsJSON, kmlId, placeMarkId);
          break;
        default:
          break;

      }
    }
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
    this.mCallback.success(PluginUtil.Bundle2Json(parseResult));
  }



  private void execOtherClassMethod(final JSONArray params, final CallbackContext callback) {
    executorService.submit(new Runnable() {
      @Override
      public void run() {
        try {
          mPluginMap.loadPlugin(params, callback);
        } catch (JSONException e) {
          e.printStackTrace();
          callback.error(e.getMessage() + "");
        }
      }
    });
  }
  private void implementToMap(final String className, final Bundle options, final String kmlId, final String placeMarkId) {
    this.implementToMap(className, PluginUtil.Bundle2Json(options), kmlId, placeMarkId);
  }

  private void implementToMap(final String className, final JSONObject options, final String kmlId, final String placeMarkId) {
    JSONArray params = new JSONArray();
    params.put(className);
    params.put(options);
    AsyncKmlParser.this.execOtherClassMethod(params, new PluginUtil.MyCallbackContext(kmlId +"_callback", mPluginMap.webView) {

      @Override
      public void onResult(PluginResult pluginResult) {
        String jsStr = String.format(Locale.ENGLISH,
          "javascript:if(window.cordova){cordova.fireDocumentEvent('%s_%s', {class:'%s', placeMarkId:'%s', options:%s});}",
          mPluginMap.mapId, kmlId, className, placeMarkId, options.toString());

        Log.d("AsyncKmlParse", "--> " + jsStr);
          mPluginMap.webView.loadUrl(jsStr);
      }

    });

  }

  private JSONArray kmlColor2PluginColor(String colorStr) {
    JSONArray rgba = new JSONArray();
    colorStr = colorStr.replace("#", "");
    for (int i = 2; i < 8; i += 2) {
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

    Bundle parentNode;
    ArrayList<Bundle> pairList = null;
    KML_TAG kmlTag;
    String tagName;
    String tmp;
    int nodeIndex;
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
            case networklink:
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
    result.putParcelableArrayList("placeMarks", placeMarks);
    result.putBundle("styles", styles);
    return result;
  }
}
