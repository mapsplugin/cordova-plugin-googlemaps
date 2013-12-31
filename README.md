phonegap-googlemaps-plugin
==========================
This plugin helps you to control [Google Maps Android SDK v2][0] and [Google Maps SDK for iOS][1] from your JavaScript code.
This plugin works with [Apache Cordova][2].

==========================
## Demo
[![ScreenShot](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/android-demo.png)](http://youtu.be/RvvusY-JpXg)
http://www.youtube.com/watch?v=RvvusY-JpXg

==========================
## Manual Android Instration

1. Create a cordova [Create a basic Cordova Android application][3]

2. In the Cordova Android application you will need to put the following in your res/xml/config.xml file as a child to the plugin tag:
<pre>Cordova version 2
&lt;plugin name="GoogleMaps" value="plugin.google.maps.GoogleMaps" /&gt;
</pre>
<pre>Cordova version3
&lt;feature name="GoogleMaps"&gt;
  &lt;param name="android-package" value="plugin.google.maps.GoogleMaps" /&gt;
&lt;/feature&gt;
</pre>

3. You'll need to set up the [Google Play Services SDK][4] and link to it.
![image1](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/google-play-services.png)

4. Add these permissions and elements to your AndroidManifest.xml.
Plese refer the more detailed exlpain at the [Google Maps Document][5].
``` xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="com.google.android.providers.gsf.permission.READ_GSERVICES"/>
<!-- The following two permissions are not required to use
     Google Maps Android API v2, but are recommended. -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<-- OpenGL ES version 2 -->
<uses-feature android:glEsVersion="0x00020000" android:required="true" />
```

[0]: https://developers.google.com/maps/documentation/android/
[1]: https://developers.google.com/maps/documentation/ios/
[2]: http://cordova.apache.org/
[3]: http://cordova.apache.org/docs/en/3.0.0/guide_cli_index.md.html#The%20Command-line%20Interface
[4]: http://developer.android.com/google/play-services/setup.html
[5]: https://developers.google.com/maps/documentation/android/start#specify_app_settings_in_the_application_manifest

