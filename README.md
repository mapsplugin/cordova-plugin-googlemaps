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

Cordova version3
&lt;feature name="GoogleMaps"&gt;
  &lt;param name="android-package" value="plugin.google.maps.GoogleMaps" /&gt;
&lt;/feature&gt;
</pre>

[0]: https://developers.google.com/maps/documentation/android/
[1]: https://developers.google.com/maps/documentation/ios/
[2]: http://cordova.apache.org/
[3]: http://cordova.apache.org/docs/en/3.0.0/guide_cli_index.md.html#The%20Command-line%20Interface
