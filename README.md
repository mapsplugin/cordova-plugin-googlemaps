phonegap-googlemaps-plugin
==========================
This plugin helps you to control [Google Maps Android SDK v2][0] and [Google Maps SDK for iOS][1] from your JavaScript code.
This plugin works with [Apache Cordova][2].

## Manual Android Instration
==========================

1. Create a cordova [Create a basic Cordova Android application][3]

2. In the Cordova Android application you will need to put the following in your res/xml/config.xml file as a child to the plugin tag:
``` xml
<!-- for Cordova version 2 -->
<plugin name="GoogleMaps" value="plugin.google.maps.GoogleMaps" />
```
``` xml
<!-- for Cordova version 3 -->
<feature name="GoogleMaps">
  <param name="android-package" value="plugin.google.maps.GoogleMaps" />
</feature>
```

3. You'll need to set up the [Google Play Services SDK][4] and link to it.
![image1](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/google-play-services.png)

4. Add these permissions and elements to your AndroidManifest.xml.
Please refer the more detailed exlpain in the [Google Maps Document][5].
``` xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="com.google.android.providers.gsf.permission.READ_GSERVICES"/>
<!-- The following two permissions are not required to use
     Google Maps Android API v2, but are recommended. -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
```
``` xml
<!-- OpenGL ES version 2 -->
<uses-feature android:glEsVersion="0x00020000" android:required="true" />
<!-- Google Play Services SDK -->
<meta-data android:name="com.google.android.gms.version"
           android:value="@integer/google_play_services_version" />
```
5. Add your Google Maps API key under the &lt;application&gt; tag.
Please refer the more detailed exlpain in the [Google Maps Document][6].
Replace YOUR_GOOGLE_MAPS_ANDROID_API_KEY_IS_HERE with your google maps api key.
``` xml
<meta-data
  android:name="com.google.android.maps.v2.API_KEY"
  android:value="YOUR_GOOGLE_MAPS_ANDROID_API_KEY_IS_HERE" />
<!-- for Google Play Services SDK -->
<meta-data
  android:name="com.google.android.gms.version"
  android:value="@integer/google_play_services_version" />
```

6. From this plugin folder copy the www/googlemaps-cdv-plugin.js and example/Simple/ files into your application's assets/www folder. Overwrite the existing index.html file.

## Manual iOS Instration
==========================
1. First of all, download the latest SDK from [the official page][iOS1].

2. Create a cordova [Create a basic Cordova iOS application][3]


[0]: https://developers.google.com/maps/documentation/android/
[1]: https://developers.google.com/maps/documentation/ios/
[2]: http://cordova.apache.org/
[3]: http://cordova.apache.org/docs/en/3.0.0/guide_cli_index.md.html#The%20Command-line%20Interface
[4]: http://developer.android.com/google/play-services/setup.html
[5]: https://developers.google.com/maps/documentation/android/start#specify_app_settings_in_the_application_manifest
[6]: https://developers.google.com/maps/documentation/android/start#get_an_android_certificate_and_the_google_maps_api_key

[iOS1]: https://developers.google.com/maps/documentation/ios/start#getting_the_google_maps_sdk_for_ios

