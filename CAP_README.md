# Installation for ionic/capacitor with ionic

# Common

```
$> npm -g install ionic

$> ionic start myApp tabs

$> cd myApp

$> npm run build

$> npm install --save @capacitor/cli @capacitor/core

$> npx cap init

$> npx cap add android

$> npx cap add ios

$> npm install --save \@ionic-native/core \@ionic-native/google-maps

$> ionic cordova plugin add https://github.com/mapsplugin/cordova-plugin-googlemaps#cap2
...
CREATE config.xml
[OK] Integration cordova added!
> cordova plugin add cordova-plugin-googlemaps --save
Adding cordova-plugin-googlemaps to package.json

$> npx cap sync
✔ Copying web assets from www to ios/App/public in 249.24ms
✔ Copying native bridge in 574.92μp
✔ Copying capacitor.config.json in 637.82μp
  Found 1 Cordova plugin for ios
    CordovaPluginGooglemaps
✔ copy in 312.09ms
✔ Updating iOS plugins in 19.48ms
  Found 0 Capacitor plugins for ios:
  Found 1 Cordova plugin for ios
    CordovaPluginGooglemaps
✔ Updating iOS native dependencies in 3.38s
✔ update ios in 3.46s
✔ copy in 231.11μp
✔ update web in 7.02μp
Sync finished in 3.781s

$> npx cap open ios
```

Open `(project)/cap.config.json`, then add the below lines to the file.

```js
{
  ...
  "GOOGLE_MAPS_ANDROID_API_KEY": "(API Key)",
  "GOOGLE_MAPS_IOS_API_KEY": "(API Key)"
}
```

Then build the project, and synchronize the project.

```
$> npm run build  // Do not "ionic cordova build android"

$> npx cap copy   // copy the www directory to capacitor project
```


## How to upgrade the cordova-plugin-googlemaps in ionic/capacitor project?

```
$> ionic cordova plugin rm cordova-plugin-googlemaps

$> ionic cordova plugin add cordova-plugin-googlemaps

$> npx cap sync
```
