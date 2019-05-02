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

```

Open `(project)/cap.config.json`, then add the below lines to the file.

```js
{
  "appId": "com.example.app",
  "appName": "myapp",
  "bundledWebRuntime": false,
  "npmClient": "npm",
  "webDir": "www",
  "GOOGLE_MAPS_ANDROID_API_KEY": "(API Key)",  <-- Add
  "GOOGLE_MAPS_IOS_API_KEY": "(API Key)"  <-- Add
}
```

Then build the project, and synchronize the project.

```
$> npx cap sync
```

## Write your code, like regular ionic application

```ts
import { Component, OnInit } from '@angular/core';
import {
  ToastController,
  Platform,
  LoadingController
} from '@ionic/angular';
import {
  GoogleMaps,
  GoogleMap,
  GoogleMapsEvent,
  Marker,
  GoogleMapsAnimation,
  MyLocation
} from '@ionic-native/google-maps';

@Component({
  selector: 'app-tab2',
  templateUrl: 'tab2.page.html',
  styleUrls: ['tab2.page.scss']
})
export class Tab2Page implements OnInit {

  map: GoogleMap;
  loading: any;

  constructor(
    public loadingCtrl: LoadingController,
    public toastCtrl: ToastController,
    private platform: Platform) { }

  async ngOnInit() {
    // Since ngOnInit() is executed before `deviceready` event,
    // you have to wait the event.
    await this.platform.ready();
    await this.loadMap();
  }

  loadMap() {
    this.map = GoogleMaps.create('map_canvas', {
      camera: {
        target: {
          lat: 43.0741704,
          lng: -89.3809802
        },
        zoom: 18,
        tilt: 30
      }
    });

  }

  async onButtonClick() {
    this.map.clear();

    this.loading = await this.loadingCtrl.create({
      message: 'Please wait...'
    });
    await this.loading.present();

    // Get the location of you
    this.map.getMyLocation().then((location: MyLocation) => {
      this.loading.dismiss();
      console.log(JSON.stringify(location, null ,2));

      // Move the map camera to the location with animation
      this.map.animateCamera({
        target: location.latLng,
        zoom: 17,
        tilt: 30
      });

      // add a marker
      let marker: Marker = this.map.addMarkerSync({
        title: '@ionic-native/google-maps plugin!',
        snippet: 'This plugin is awesome!',
        position: location.latLng,
        animation: GoogleMapsAnimation.BOUNCE
      });

      // show the infoWindow
      marker.showInfoWindow();

      // If clicked it, display the alert
      marker.on(GoogleMapsEvent.MARKER_CLICK).subscribe(() => {
        this.showToast('clicked!');
      });
    })
    .catch(err => {
      this.loading.dismiss();
      this.showToast(err.error_message);
    });
  }

  async showToast(message: string) {
    let toast = await this.toastCtrl.create({
      message: message,
      duration: 2000,
      position: 'middle'
    });

    toast.present();
  }
}
```

Then build and synchronize.

```
$> npm run build

$> npx cap copy
```


## How to upgrade the cordova-plugin-googlemaps in ionic/capacitor project?

```
$> ionic cordova plugin rm cordova-plugin-googlemaps

$> ionic cordova plugin add cordova-plugin-googlemaps

$> npx cap sync
```
