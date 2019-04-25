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

$> ionic cordova plugin add cordova-plugin-googlemaps
...
CREATE config.xml
[OK] Integration cordova added!
> cordova plugin add cordova-plugin-googlemaps --save
Adding cordova-plugin-googlemaps to package.json

$> npx cap sync

```

Open `(project)/config.xml`, then add the below lines to the file.

```
<widget ...>
  ...
  <preference name="GOOGLE_MAPS_ANDROID_API_KEY" value="(api key)" />
  <preference name="GOOGLE_MAPS_IOS_API_KEY" value="(api key)" />
</widget>
```

Then build the project, and synchronize the project.

```
$> npm run build  // Do not "ionic cordova build android"

$> npx cap copy   // copy the www directory to capacitor project
```
