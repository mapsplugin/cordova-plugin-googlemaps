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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import za.co.twyst.tbxml.TBXML;


public class PluginKmlOverlay extends MyPlugin implements MyPluginInterface {
  private HashMap<String, Bundle> styles = new HashMap<String, Bundle>();

  private enum KML_TAG {
    NOT_SUPPORTED,

    kml,
    style,
    styleurl,
    stylemap,
    schema,
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
      if (urlStr.contains(".kmz")) {
        String cacheDirPath = cordova.getActivity().getCacheDir() + "/" + urlStr.hashCode();
        ArrayList<File> files =  (PluginUtil.unpackZipFromBytes(inputStream, cacheDirPath));
        inputStream.close();
        for (File file : files) {
          if (file.getName().contains(".kml")) {
            inputStream = new FileInputStream(file);
            break;
          }
        }
      }


      String line;
      StringBuilder stringBuilder = new StringBuilder();
      InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
      while ((line = bufferedReader.readLine()) != null) {
        stringBuilder.append(line);
        stringBuilder.append("\n");
      }
      bufferedReader.close();


      TBXML tbxml = new TBXML();
      tbxml.parse(stringBuilder.toString());

      KmlParserClass parser = new KmlParserClass();
      Bundle root = parser.parseXml(tbxml, tbxml.rootXMLElement());
      Bundle result = new Bundle();
      result.putBundle("schemas", parser.schemaHolder);
      result.putBundle("styles", parser.styleHolder);
      result.putBundle("root", root);


      tbxml.release();
      inputStreamReader.close();
      inputStream.close();
      inputStream = null;
      return result;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  class KmlParserClass {
    public Bundle styleHolder = new Bundle();
    public Bundle schemaHolder = new Bundle();


    private Bundle parseXml(TBXML tbxml, long rootElement) {
      Bundle result = new Bundle();
      String styleId, schemaId, txt, attrName;
      int i;
      String tagName = tbxml.elementName(rootElement).toLowerCase();
      long childNode;
      Bundle styles, schema, extendedData;
      ArrayList<Bundle> children;
      ArrayList<String> styleIDs;

      Log.d(TAG, "--->tagName = " + tagName + "(" + rootElement + ")");
      result.putString("tagName", tagName);

      KML_TAG kmlTag = null;
      try {
        kmlTag = KML_TAG.valueOf(tagName);
      } catch(Exception e) {
        kmlTag = KML_TAG.NOT_SUPPORTED;
      }

      long[] attributes = tbxml.listAttributesOfElement(rootElement);
      for (i = 0; i < attributes.length; i++) {
        attrName = tbxml.attributeName(attributes[i]);
        result.putString(attrName, tbxml.attributeValue(attributes[i]));
      }


      switch (kmlTag) {

        case styleurl:
          styleId = tbxml.textForElement(rootElement);
          result.putString("styleId", styleId);
          break;

        case stylemap:
        case style:

          // Generate a style id for the tag
          styleId = tbxml.valueOfAttributeNamed("id", rootElement);
          if (styleId == null || styleId.isEmpty()) {
            styleId = "__" + rootElement + "__";
          }
          result.putString("styleId", styleId);

          // Store style information into the styleHolder.
          styles = new Bundle();
          children = new ArrayList<Bundle>();
          childNode = tbxml.firstChild(rootElement);
          while (childNode > 0) {
            Bundle node = this.parseXml(tbxml, childNode);
            if (node != null) {
              if (node.containsKey("value")) {
                styles.putString(node.getString("tagName"), node.getString("value"));
              } else {
                children.add(node);
              }
            }
            childNode = tbxml.nextSibling(childNode);
          }
          if (children.size() > 0) {
            styles.putParcelableArrayList("children", children);
          }
          styleHolder.putBundle(styleId, styles);


          break;

        case schema:

          // Generate a style id for the tag
          schemaId = tbxml.valueOfAttributeNamed("id", rootElement);
          if (schemaId == null || schemaId.isEmpty()) {
            schemaId = "__" + rootElement + "__";
          }

          // Store style information into the styleHolder.
          schema = new Bundle();
          schema.putString("name", tbxml.valueOfAttributeNamed("name", rootElement));
          children = new ArrayList<Bundle>();
          childNode = tbxml.firstChild(rootElement);
          while (childNode > 0) {
            Bundle node = this.parseXml(tbxml, childNode);
            if (node != null) {
              children.add(node);
            }
            childNode = tbxml.nextSibling(childNode);
          }
          if (children.size() > 0) {
            schema.putParcelableArrayList("children", children);
          }
          schemaHolder.putBundle(schemaId, schema);


          break;
        case coordinates:


          ArrayList<Bundle> latLngList = new ArrayList<Bundle>();

          txt = tbxml.textForElement(rootElement);
          txt = txt.replaceAll("\\s+", "\n");
          txt = txt.replaceAll("\\n+", "\n");
          String lines[] = txt.split("\n");
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

          result.putParcelableArrayList(tagName, latLngList);
          break;



        default:

          childNode = tbxml.firstChild(rootElement);
          if (childNode != 0) {
            children = new ArrayList<Bundle>();
            while (childNode != 0) {
              Bundle node = this.parseXml(tbxml, childNode);
              if (node != null) {
                if (node.containsKey("styleId")) {
                  styleIDs = result.getStringArrayList("styleIDs");
                  if (styleIDs == null) {
                    styleIDs = new ArrayList<String>();
                  }
                  styleIDs.add(node.getString("styleId"));
                  result.putStringArrayList("styleIDs", styleIDs);
                } else if (!("schema".equals(node.getString("tagName")))) {
                  children.add(node);
                }
              }
              childNode = tbxml.nextSibling(childNode);
            }

            result.putParcelableArrayList("children", children);
          } else {
            result.putString("value", tbxml.textForElement(rootElement));
          }
          break;
      }

      return result;
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

}
