# Play Services for Cordova

This is a simple Cordova plugin that adds the [Google Play Services](http://developer.android.com/google/play-services/setup.html)
client library to your app.

## Developer Guide

This plugin should be used by plugin authors as a dependency, if their plugin's Android native code requires the Play Services.
See for example [chrome.identity](https://github.com/MobileChromeApps/mobile-chrome-apps/tree/master/chrome-cordova/plugins/chrome.identity).

## Update Instructions

1. Install the .jar via the Android SDK Manager (`android sdk`)
2. Copy it from `android-sdk/extras/google/google_play_services/libproject/google-play-services_lib/libs/google-play-services.jar`
3. Update the version number in `plugin.xml` to match `Pkg.Revision` from `android-sdk/extras/google/google_play_services/source.properties`
4. Run `plugman publish .` to publish to registry
