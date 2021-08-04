# Installation for ionic/capacitor with ionic

# Common

```
$> git clone git@github.com:mapsplugin/capacitor-ios-project.git

$> ionic start myApp tabs --capacitor

$> ? Framework: (Use arrow keys)
$> ❯ Angular | https://angular.io   <-- use Angular at this time
     React   | https://reactjs.org

$> cd myApp

$> npx cap init [appName] [appId]

$> ionic build

$> npx cap add ios

$> npm install @ionic-native/google-maps@5.27.0-beta-20200630

$> npm install (path to)/capacitor-ios-project

$> npx cap sync
```

Open `(project)/cap.config.json`, then add the below lines to the file.

```js
{
  "appId": "com.example.app",
  "appName": "myapp",
  "bundledWebRuntime": false,
  "npmClient": "npm",
  "webDir": "www",
  "cordova": {},
  "googlemaps": { <-- Add
    "GOOGLE_MAPS_ANDROID_API_KEY": "(API Key)",  <-- Add
    "GOOGLE_MAPS_IOS_API_KEY": "(API Key)"  <-- Add
  } <-- Add
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
$> ionic build

$> npx cap copy
```


## How to upgrade the cordova-plugin-googlemaps-beta in ionic/capacitor project?

```
$> cd (path to)/capacitor-ios-project

$> git pull

$> cd (project dir)

$> npx cap sync
```
