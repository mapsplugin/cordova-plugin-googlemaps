phonegap-googlemaps-plugin
==========================
このプラグインは [Google Maps Android SDK v2][0] と [Google Maps SDK for iOS][1] をJavaScriptコードから操作することができます。
このプラグインは [Apache Cordova][2]（PhoneGapのオープンソース版）を前提に開発しています。

![ScreenShot](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/phonegap-googlemaps-plugin_small.png)

## Androidへのインストール

1. まずCordovaプロジェクトを作成し、Androidプラットフォームを追加します。
詳しくは [the cordova document][3]を参照してください。
``` bash
$> cordova create ./mydir com.example.googlemap MyProject
$> cd mydir
$> cordova platform add android
```

2. 作成されたCordovaプロジェクト内にある`res/xml/config.xml`ファイルに以下のタグを追加します。
``` xml
<feature name="GoogleMaps">
  <param name="android-package" value="plugin.google.maps.GoogleMaps" />
</feature>
```

3. **Google Play Services SDK**をEclipseに読み込み、リンクします。
 * [Google Play Services SDK for Androidをインストールします][4A]
 * [Google Play Services SDKをEclipseに読み込みます][4B]
 * Google Play Services SDK ライブラリプロジェクトをリンクします。プロジェクトを右クリックしてプロパティを表示し、 'Android'タブを選択します。ダイアログの下部にある'Add'ボタンをクリックし、'google-play-services_lib' をワークスペースから選択します。
![google-play-services](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/google-play-services.png)

4. `AndroidManifest.xml`にパーミッションを追加します。
詳細は [Google Maps Document][5]を参照してください。
``` xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="com.google.android.providers.gsf.permission.READ_GSERVICES"/>
<!-- The following two permissions are not required to use
     Google Maps Android API v2, but are recommended. -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<!-- OpenGL ES version 2 -->
<uses-feature android:glEsVersion="0x00020000" android:required="true" />
<!-- Google Play Services SDK -->
<meta-data android:name="com.google.android.gms.version"
           android:value="@integer/google_play_services_version" />
```
5. 続いてあなたの Google Maps API key を&lt;application&gt; タグの下に追加します。
詳しくは [Google Maps Document][6]を参照してください。
YOUR_GOOGLE_MAPS_ANDROID_API_KEY_IS_HERE の部分をあなたのGoogle Maps APIキーに置き換えてください。
``` xml
<meta-data
  android:name="com.google.android.maps.v2.API_KEY"
  android:value="YOUR_GOOGLE_MAPS_ANDROID_API_KEY_IS_HERE" />
<!-- for Google Play Services SDK -->
<meta-data
  android:name="com.google.android.gms.version"
  android:value="@integer/google_play_services_version" />
```

6. 本プラグインのフォルダから`www/googlemaps-cdv-plugin.js`と `example/Simple/` ファイルを、作成したCordovaプロジェクトの `assets/www` フォルダに追加します。既存のindex.htmlファイルは置き換えてください。

7. 本プラグインのフォルダから`src/android/` フォルダを作成したCordovaプロジェクトの`src`フォルダに追加します。これで準備OKです。
 
## iOSへのインストール
1. まず最新のSDKを [公式ドキュメント][iOS1]からダウンロードしてください。

2. Cordovaプロジェクトを作成し、iOSプラットフォームを追加します。
詳細は [the cordova document][3]を参照してください。
``` bash
$> cordova create ./mydir com.example.googlemap MyProject
$> cd mydir
$> cordova platform add ios
```

3. [公式ドキュメント][iOS2]に従って、SDKをインストールします。
![ios-project-settings](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/ios-project-settings.png)

4. 本プラグインのフォルダから`www/googlemaps-cdv-plugin.js`と `example/Simple/` ファイルを、作成したCordovaプロジェクトの `assets/www` フォルダに追加します。既存のindex.htmlファイルは置き換えてください。

5. 本プラグインのフォルダから`src/ios/` フォルダを作成したCordovaプロジェクトの`Plugins`フォルダに追加します。

6. 作成したCordovaプロジェクトにある `config.xml` ファイルに以下のタグを追加します。
``` xml
<feature name="GoogleMaps">
    <param name="ios-package" value="GoogleMaps" />
</feature>
```
7. XCodeの左ペインにあるツリーからResourcesを開き、**[PROJECTNAME]-Info.plist**を探し、新しいエントリーを追加します。
キーは **Google Maps API Key**で、値は事前に取得した **Google Maps API Key for iOS**を入力します。これで準備はOKです。
![ios-project-settings2](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/ios-project-settings2.png)

###地図の初期化
本地図プラグインを初期化するには、Mapクラスの**getMap()**を最初に実行します。
初期化が完了すると`MAP_READY`イベントが発行します。
このイベントを **addEventListener()** または **on()** メソッドを使って受信します。
```js
var map = plugin.google.maps.Map.getMap();
map.addEventListener(plugin.google.maps.event.MAP_READY, function() {
  //やりたいことをここに書く
});
```

###パラメータ付きで地図の初期化
最初に地図を表示する位置などのパラメータ付きで地図を初期化したい場合、次のコードのようにすることができます。
```js
var map = plugin.google.maps.Map.getMap({
  'mapType': plugin.google.maps.MapTypeId.HYBRID,
  'controls': {
    'compass': true,
    'myLocationButton': true,
    'indoorPicker': true,
    'zoom': true
  },
  'gestures': {
    'scroll': true,
    'tilt': true,
    'rotate': true
  },
  'camera': {
    'latLng': GORYOKAKU_JAPAN,
    'tilt': 30,
    'zoom': 15,
    'bearing': 50
  }
});
```

###イベントの受信
地図がクリックされたなど、いくつかのイベントを受信することができます。
Mapクラスに対して発行されるイベントは以下のとおりです。
 * MAP_CLICK
 * MAP_LONG_CLICK
 * MY_LOCATION_CHANGE(Android)
 * MY_LOCATION_BUTTON_CLICK
 * CAMERA_CHANGE 
 * MAP_READY
 * MAP_LOADED(Android)
 * MAP_WILL_MOVE(iOS)

Markerクラスに対して発行されるイベントは以下のとおりです。
 * MARKER_CLICK
 * INFO_CLICK
 * MARKER_DRAG
 * MARKER_DRAG_START
 * MARKER_DRAG_END
 
```js
var evtName = plugin.google.maps.event.MAP_LONG_CLICK;
map.on(evtName, function(latLng) {
  alert("地図が長押しされた.\n" +
        latLng.toUrlValue());
});
```

###地図ダイアログの表示
本プラグインは地図をダイアログ表示します。地図を表示するには **showDialog()** メソッドを実行します。
```js
map.showDialog();
```

###地図ダイアログを閉じる
地図ダイアログを閉じたい場合は、**closeDialog()** メソッドを実行します。
```js
map.closeDialog();
```

###地図タイプの変更
地図タイプの変更は **setMapTypeId()** メソッドを使用します。
利用可能な地図タイプは`ROADMAP`, `SATELLITE`, `HYBRID`, `TERRAIN` と `NONE`です。
```js
map.setMapTypeId(plugin.google.maps.HYBRID);
```
![map_type](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/map_type.png)

###カメラの移動
Google Maps for mobileはビューカメラを使用しています。
実際に画面に表示される地図はこのカメラを通して撮影されています。つまり任意の場所を表示したければ、カメラを移動させる必要があります。
本プラグインでは、 **animateCamera()** と **moveCamera()** メソッドを使用して行います。
**animateCamera()** はアニメーション付きで移動します。
**moveCamera()**はアニメーションなしで移動します。
デフォルトの **animateCamera()** メソッドのアニメーション時間は4秒です。
```js
map.animateCamera({
  'target': GOOGLE,
  'tilt': 60,
  'zoom': 18,
  'bearing': 140
});
```
[![map.animateCamera](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/animateCamera.gif)](http://www.youtube.com/watch?v=QMLWrOxfgRw)

http://www.youtube.com/watch?v=QMLWrOxfgRw

どちらのメソッドも第2引数にコールバック関数を取ることができます。
このコールバック関数は移動が終了した時に呼び出されます。
```js
map.moveCamera({
  'target': STATUE_OF_LIBERTY,
  'zoom': 17,
  'tilt': 30
}, function() {
  var mapType = plugin.google.maps.MapTypeId.HYBRID;
  map.setMapTypeId(mapType);
  map.showDialog();
});
```
![camera](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/camera.png)

###アニメーション時間の変更
**animateCamera()** メソッドの時間を変更したい場合は、**duration** オプションにミリ秒で指定します。
```js
map.animateCamera({
  'target': GOOGLE,
  'tilt': 60,
  'zoom': 18,
  'bearing': 140,
  'duration': 10000
});
```

###カメラ位置の取得
カメラの位置を知りたい場合は、**getCameraPosition()** メソッドを実行するだけです。
```js
map.getCameraPosition(function(camera) {
  var buff = ["Current camera position:\n"
      "latitude:" + camera.target.lat,
      "longitude:" + camera.target.lng,
      "zoom:" + camera.zoom,
      "tilt:" + camera.tilt,
      "bearing:" + camera.bearing].join("\n");
  alert(buff);
});
```

###現在位置の取得
もし現在位置を知りたいなら、**getMyLocation()**メソッドを実行するだけです。
```js
map.getMyLocation(function(location) {
  var msg = ["Current your location:\n",
    "latitude:" + location.latLng.lat,
    "longitude:" + location.latLng.lng,
    "speed:" + location.speed,
    "time:" + location.time,
    "bearing:" + location.bearing].join("\n");
  
  map.addMarker({
    'position': location.latLng,
    'title': msg
  }, function(marker) {
    marker.showInfoWindow();
  });
});
```
![image](https://raw2.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/mylocation.png)

###マーカーの追加
マーカーの追加は、**addMarker()** メソッドを使用します。
```js
map.addMarker({
  'position': GOOGLE,
  'title': "Hello GoogleMap for Cordova!"
});
```
![marker0](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/marker0.png)

###複数行をマーカーに追加
マーカーをクリックした時に複数行を表示したい場合、**title** オプションにそのまま渡してください。
プラグインがうまいこと表示します。
標準のGoogle Maps SDKでは、改行は無視されてしまいます。
```js
map.addMarker({
  'position': GOOGLE_NY,
  'title': ["Hello Google Map", "for", "Cordova!"].join("\n"),
  'snippet': "This plugin is awesome!"
}, function(marker) {
  marker.showInfoWindow();
});
```
![marker3](https://raw2.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/marker3.png)

###addMarker()のコールバック関数
**addMarker()**メソッドは第2引数にコールバック関数を取ることができます。
このコールバック関数はmarkerが作成された時に呼び出されます。
本プラグインは、引数にmarkerのインスタンスを渡します。
```js
map.addMarker({
  'position': STATUE_OF_LIBERTY,
  'title': "Statue of Liberty"
}, function(marker) {
  marker.showInfoWindow();
});
```
![marker1](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/marker1.png)




###マーカのアイコンを変更する
マーカのアイコンを変更したいなら、アイコンのファイルパスかURLを **addMarker()** メソッドに渡すだけです。
```js
map.addMarker({
  'position': GOOGLE_TOKYO,
  'title': 'Google Tokyo!'
  'icon': 'www/images/google_tokyo_icon.png'
});
```
![marker2](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/marker2.png)

もしくは拡大／縮小することもできます。
```js
map.addMarker({
  'position': GOOGLE_TOKYO,
  'title': 'Google Tokyo!',
  'icon': {
    'url': 'www/images/google_tokyo_icon.png',
    'size': {
      'width': 74,
      'height': 126
    }
  }
});
```
![marker2b](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/marker2b.png)

###Base64エンコードされた画像をマーカーに使用する
これはすごくオススメな機能です！
**icon** と **title**オプションにbase64エンコードされた画像データをセットすることができます。
つまりHTML5/Canvasの技術を使って、動的に画像を生成することができます。
```js
var canvas = document.getElementById("canvas");
map.addMarker({
  'position': GOOGLE_TOKYO,
  'title': canvas.toDataURL(),
  'icon': "data:image/png;base64,iVBORw0KGgoA...",
}, function(marker) {
  marker.showInfoWindow();
});
```
![marker_base64](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/marker_base64.png)

###マーカーの削除
マーカーの削除は、 **remove()** メソッドを実行します。
```js
marker.remove();
```

###クリックイベント
markerとinfoWindowのクリックイベントもサポートします。
addEventListener() / on()メソッドを使って、イベントを受信することも可能です。
```js
map.addMarker({
  'position': GOOGLE_SYDNEY,
  'title': "Google Sydney",
  'snippet': "click, then remove",
  'draggable': true,
  'markerClick': function(marker) {
    marker.showInfoWindow();
  },
  'infoClick': function(marker) {
    marker.remove();
  }
});
```
![marker_click](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/marker_click.png)

###ポリライン・ポリゴン・円の追加
ポリラインの追加は **addPolyline()** メソッドを使用します。
```js
map.addPolyline({
  points: [
    HND_AIR_PORT,
    SFO_AIR_PORT
  ],
  'color' : '#AA00FF',
  'width': 10,
  'geodesic': true
});
```

ポリゴンの追加は、 **addPolygon()** メソッドを使用します。
```js
map.addPolygon({
  points: GORYOKAKU_POINTS,
  'strokeColor' : '#AA00FF',
  'strokeWidth': 5,
  'fillColor' : '#880000'
}, function(polygon) {
  map.animateCamera({
    'target': polygon.getPoints()
  });
});
```

円の追加は、**addCircle()** メソッドを使用します。
```js
map.addCircle({
  'center': GOOGLE,
  'radius': 300,
  'strokeColor' : '#AA00FF',
  'strokeWidth': 5,
  'fillColor' : '#880000'
});
```
![image7](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/overlays.png)


###グラウンドオーバーレイの追加
地図上の任意の地域に画像を固定して表示することができます。 **addGroundOverlay()** メソッドを使って地図に画像を追加します。
```js
var bounds = [
  new plugin.google.maps.LatLng(40.712216,-74.22655),
  new plugin.google.maps.LatLng(40.773941,-74.12544)
];

map.addGroundOverlay({
  'url': "http://www.lib.utexas.edu/maps/historical/newark_nj_1922.jpg",
  'bounds': bounds,
  'opacity': 0.5
}, function(groundOverlay) {
  map.showDialog();
  map.animateCamera({
    'target': bounds
  });
});
```
![image8](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/ground_overlay.png)

###グラウンド・オーバーレイの削除
グラウンド・オーバーレイを削除するには、**remove()** メソッドを実行します。
```js
groundOverlay.remove();
```


###タイル・オーバーレイの追加
タイル・オーバーレイは画像を地図として、ベース地図の上に表示することができます。
追加するには、**addTileOverlay()** メソッドを実行します。
地図画像のURLには `<x>`,`<y>` と `<zoom>`を含んでください。
これらは実際の値に置き換えられます。
```js
map.addTileOverlay({
  // <x>,<y> and <zoom> are replaced with values
  tileUrlFormat: "http://tile.stamen.com/watercolor/<zoom>/<x>/<y>.jpg"
}, function(tileOverlay) {
  mTileOverlay = tileOverlay;
  map.showDialog();
});
```
![image9](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/tile_overlay.png)

###タイル・オーバーレイの削除
タイル・オーバーレイを削除するには、**remove()** メソッドを実行します。
```js
tileOverlay.remove();
```


###ジオコーディング
このプラグインではジオコーディングもサポートします。住所やランドスケープ名を緯度経度に変換することができます。
Android内ではGoogle Play Servicesの機能を、iOSではiOSの機能を使用しています (iOSではGoogleのジオコーダーを使用していません)。
```js
var request = {
  'address': "Kyoto, Japan"
};
map.geocode(request, function(results) {
  if (results.length) {
    map.showDialog();
    var result = results[0];
    var position = result.position; 
    
    map.addMarker({
      'position': position
    });
  } else {
    alert("Not found");
  }
});
```
![geocoding](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/geocoding.png)


###リバース・ジオコーディング
このプラグインではリバース・ジオコーディングもサポートします。緯度経度から住所などに変換することができます。
Android内ではGoogle Play Servicesの機能を、iOSではiOSの機能を使用しています (iOSではGoogleのジオコーダーを使用していません)。
```js
var request = {
  'position': GOOGLE
};
map.geocode(request, function(results) {
  if (results.length) {
    var result = results[0];
    var position = result.position; 
    var address = [
      result.subThoroughfare || "",
      result.thoroughfare || "",
      result.locality || "",
      result.adminArea || "",
      result.postalCode || "",
      result.country || ""].join(", ");
    
    map.addMarker({
      'position': position,
      'title':  address
    });
  } else {
    alert("Not found");
  }
});
```
![reverse_geocoding](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/reverse_geocoding.png)

[0]: https://developers.google.com/maps/documentation/android/
[1]: https://developers.google.com/maps/documentation/ios/
[2]: http://cordova.apache.org/
[3]: http://cordova.apache.org/docs/en/3.0.0/guide_cli_index.md.html#The%20Command-line%20Interface
[4A]: http://developer.android.com/google/play-services/setup.html#Install
[4B]: http://developer.android.com/tools/projects/projects-eclipse.html#ReferencingLibraryProject
[5]: https://developers.google.com/maps/documentation/android/start#specify_app_settings_in_the_application_manifest
[6]: https://developers.google.com/maps/documentation/android/start#get_an_android_certificate_and_the_google_maps_api_key

[iOS1]: https://developers.google.com/maps/documentation/ios/start#getting_the_google_maps_sdk_for_ios
[iOS2]: https://developers.google.com/maps/documentation/ios/start#adding_the_google_maps_sdk_for_ios_to_your_project
[iOS3]: https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/ios-project-settings.png
