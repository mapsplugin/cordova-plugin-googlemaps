Cordova GoogleMaps plugin for iOS and Android
==========================
This plugin is a thin wrapper for [Google Maps Android API](https://developers.google.com/maps/documentation/android/) and [Google Maps SDK for iOS](https://developers.google.com/maps/documentation/ios/).
Both [PhoneGap](http://phonegap.com/) and [Apache Cordova](http://cordova.apache.org/) are supported.

-----

### Quick install

Before you install, make sure you've read the [instructions](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v1.4.0/Installation/README.md)

*npm (current stable 1.4.3)*
```bash
$> cordova plugin add cordova-plugin-googlemaps --variable API_KEY_FOR_ANDROID="YOUR_ANDROID_API_KEY_IS_HERE" --variable API_KEY_FOR_IOS="YOUR_IOS_API_KEY_IS_HERE"
```
*v1.4.3 is just small updates.

*Github (current master, potentially unstable)*
```bash
$> cordova plugin add https://github.com/mapsplugin/cordova-plugin-googlemaps --variable API_KEY_FOR_ANDROID="YOUR_ANDROID_API_KEY_IS_HERE" --variable API_KEY_FOR_IOS="YOUR_IOS_API_KEY_IS_HERE"
```

If you re-install the plugin, please always remove the plugin first, then remove the SDK

```bash
$> cordova plugin rm cordova-plugin-googlemaps
$> cordova plugin rm com.googlemaps.ios
$> cordova plugin add cordova-plugin-googlemaps --variable API_KEY_FOR_ANDROID="YOUR_ANDROID_API_KEY_IS_HERE" --variable API_KEY_FOR_IOS="YOUR_IOS_API_KEY_IS_HERE"
```

The SDK-Plugin won't be uninstalled automatically and you will stuck on an old version.

-----

### Join the official community
New versions will be announced through the official community. Stay tuned!

<a href="https://plus.google.com/u/0/communities/117427728522929652853"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v1.4.0/top/Red-signin_Google_base_44dp.png?raw=true" height="40"></a>

### Chat
Join our online chat at<br>
[![Gitter](https://badges.gitter.im/cordova-plugin-googlemaps.svg)](https://gitter.im/nightstomp/cordova-plugin-googlemaps)

### Example
You can see an example here. **(old version, but all most the same)**

 [phonegap-googlemaps-plugin-v1.2.5.apk](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v1.4.0/top/phonegap-googlemaps-plugin-v1.2.5.apk)
```bash
$> adb install phonegap-googlemaps-plugin-v1.2.5.apk
```

![](https://raw.githubusercontent.com/mapsplugin/cordova-plugin-googlemaps-doc/master/v1.4.0/top/example-v1.2.5.gif)

### Documentations

All documentations are moved to https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v1.4.0/README.md

-----


### Version 2.0 Beta

The new version 2.0 supports multiple maps on multiple pages.
Lots of issues are fixed, and the performance are improved.

However, the documentation is not enough for the version 2.0.
For the reason, the new version is still in the beta.
If you are interested in it, you can try the new version.

For more details, please read here.
https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/README.md

You can try the demo application from here.


https://github.com/mapsplugin/v2.0-demo

![](https://github.com/mapsplugin/v2.0-demo/raw/master/image.gif)

-----


### Version 2.0 Beta Roadmap

The version 1.x will be shutdown in 2017.
The date is not decided yet, but we release the v1.4.3 is the last version of v1.x

For more details are announced through the [Official community](https://plus.google.com/u/0/communities/117427728522929652853)

![](https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/roadmap.png?raw=true)
