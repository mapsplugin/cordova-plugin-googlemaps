phonegap-googlemaps-plugin
==========================
This plugin helps you to control [Google Maps Android SDK v2][0] and [Google Maps SDK for iOS][1] from your JavaScript code.
This plugin works with [Apache Cordova][2].

![ScreenShot](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/android-demo.png)

## Manual Android Instration

1. Create a cordova project and add Android platform.
Please refer [the cordova document][3].
``` bash
$> cordova create ./mydir com.example.googlemap MyProject
$> cd mydir
$> cordova platform add android
```

2. In the Cordova Android application you will need to put the following in your `res/xml/config.xml` file as a child to the plugin tag:
``` xml
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

6. From this plugin folder copy the `www/googlemaps-cdv-plugin.js`and `example/Simple/` files into your application's `assets/www` folder. Overwrite the existing index.html file.

7. From this plugin folder copy the `src/android/` folder into your application's `src` folder.
 
## Manual iOS Instration
1. First of all, download the latest SDK from the [official document][iOS1].

2. Create a cordova project and add iOS platform.
Please refer [the cordova document][3].
``` bash
$> cordova create ./mydir com.example.googlemap MyProject
$> cd mydir
$> cordova platform add ios
```

3. Install the SDK to your project following the [official document][iOS2].
After the setup correctly, the project settings should be [this image][iOS3].

4. From this plugin folder copy the `www/googlemaps-cdv-plugin.js` and `example/Simple/` files into your application's `assets/www` folder. Overwrite the existing index.html file.

5. From this plugin folder copy the `src/ios/` folder into your application's `Plugins` folder.

6. In the Cordova iOS application you will need to put the following in your `config.xml` file as a child to the plugin tag:
``` xml
<feature name="GoogleMaps">
    <param name="ios-package" value="GoogleMaps" />
</feature>
```
7. Under the group Resources, find your **[PROJECTNAME]-Info.plist**, add a new entry.
For the key, add **Google Maps API Key**, and its value is your **Google Maps API Key for iOS**.
![image3](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/ios-project-settings2.png)

## Coding examples

###Initialize a map
To initialize the map plugin, you need to call the **getMap()** method of the Map class.
The map class raises ``MAP_READY` event when the map is initialized.
You can receive the event with either **addEventListener()** or **on()** method.
```js
var map = plugin.google.maps.Map.getMap();
var evt = plugin.google.maps.event.MAP_READY;
map.addEventListener(evt, function() {
  //Something you want to do
});
```

[0]: https://developers.google.com/maps/documentation/android/
[1]: https://developers.google.com/maps/documentation/ios/
[2]: http://cordova.apache.org/
[3]: http://cordova.apache.org/docs/en/3.0.0/guide_cli_index.md.html#The%20Command-line%20Interface
[4]: http://developer.android.com/google/play-services/setup.html
[5]: https://developers.google.com/maps/documentation/android/start#specify_app_settings_in_the_application_manifest
[6]: https://developers.google.com/maps/documentation/android/start#get_an_android_certificate_and_the_google_maps_api_key

[iOS1]: https://developers.google.com/maps/documentation/ios/start#getting_the_google_maps_sdk_for_ios
[iOS2]: https://developers.google.com/maps/documentation/ios/start#adding_the_google_maps_sdk_for_ios_to_your_project
[iOS3]: https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/ios-project-settings.png

