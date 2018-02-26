# Cordova GoogleMaps plugin for iOS and Android (crap-free version)

This fork is a crap-free version of the "official" cordova-plugin-googlemaps
The main motivation for this fork is because the official plugin is sooooooo memory and CPU-intensive that it's totally unusable.
In this README, I'll try to explain:
* Why it's not so easy to integrate a native Google Map inside a WebView
* Why the official plugin is not good
* What we changed in our fork

## Why it's not so easy to integrate a native Google Map inside a WebView

We want to mix, in the same screen, a native view (the Google Map) and the Cordova WebView.
It could be easy if we only split the screen in half (for instance, the Google Map on the top half of the screen, and the WebView on the bottom), but we also want to be able to put HTML elements on top of the Map.

### 1st difficulty: how to display them both

To display them both, the native view can be put either in front of the WebView, either behind the WebView.
Putting it in front would prevent any HTML to be displayed on top of the Map.
That's why the only solution is to display it behind, which then requires the WebView to have a transparent "hole" of the size of the Map. In other words, all HTML elements of the WebView that are on top of the Map must be transparent.

### 2nd difficulty: how to route the touch events

When having both a native view and a webview in the same screen, the touch events must be intercepted either by one or the other.

The most natural way of doing it would be to have the WebView intercept all touch events (which is normal, because it's on top of the native view), and route the events to the native view when needed.
Unfortunately, it seems impossible, because in iOS for instance, the Google Maps SDK relies on the native _UIEvent_, that cannot be emulated.

Thus, the native view must intercept all clicks, and route them to the WebView when needed.
For that, we must add another transparent native view, on top of the WebView, which intercepts all touch events and routes them either to the WebView or to the native Map.

To sum up, we have, from front to back:
* A transparent native view that intercepts all clicks
* The Cordova WebView
* 1 native view for each Map

For the transparent native view to know if the touch event must be routed to the WebView or one of the native Maps, it has to know
* the position on screen of the currently visible and clickable Maps (if any)
* the position on screen of HTML elements that are on top of those Maps

Then, it tests:
* If the click is inside the area of a visible and a clickable Map, and there is no HTML element on top of it, then route to native Map
* Else route to WebView

## Why the official plugin is not good

The author of the official plugin wants it to be easy to integrate.
But as explained before, this is NOT easy.
In order to make it easy, the plugin does everything automatically, and for this, it has to put in memory a lot of things, listen to a lot of events, and make a lot of recalculations very frequently.

### What the official plugin does automatically and is OK (or almost)

* When launching the app, the plugin creates the transparent native view that will intercept all touch events (_MyPluginLayout.java_, _MyPluginLayer.m_). Starting from here, all the app is already slower than without the plugin, even if we don't use any Map! But as explained, I don't think it's possible to do differently.
* When attaching a Map to a div, the plugin applies to that div the class _ _gmaps_cdv_ _, which makes it transparent, and it applies this class to all its parents recursively. This creates the "transparent hole" required to display the Map. When removing the Map, it removes those classes. I believe it should be an option for the developer, but hey, let's say it's ok.

### What the official plugin does automatically and is really not OK

* addEventListener("deviceready") to recalculate the DOM positions of all elements
* addEventListener("plugin_touch) to do the same
* addEventListener("orientationchange") to do the same
* addEventListener("backbutton") to do the same (yes, on all screens, even when no map is displayed!)
* addEventListener("transitionend") to do the same (yes, on all transitions, even the ones that are totally unrelated!)
* redefine the very method addEventListener (yes, globally!)
* create a MutationObserver that will also recalculate the DOM positions anytime an HTML node is created/deleted (!)
* resize the Maps every 250ms (Android, _ResizeTask_) or 100ms (iOS, _redrawTimer_)
* browse through all HTML elements of the entire DOM for each touch event, to see if it should reach the Map or not
* overwrite the <meta name="viewport"> of the app; yes, you read me well, whatever you decide to put in that tag will be removed and replaced by the plugin
* maintain a cache of all z-index of all HTML elements of the entire DOM

As a result of all this, the plugin is indeed easy to integrate in your app.
But right after you have integrated it, your app becomes slow, uses a lot of CPU, a lot of memory, crashes randomly, and becomes totally unusable.

### Other small things

If you look at the source code of the official plugin you'll quickly see:
* A lot of code duplication. But I mean, really a lot.
* A lot of useless "utility" methods. For instance, _parseBoolean_, which transforms _"true"_ and _1_ to _true_ and is called every time you use a method which has a boolean parameter. Just in case you code like a monkey.

## What we changed in our fork

### Remove all "magic"

1st, we removed everything that is listed under section "What the official plugin does automatically and is really not OK"

### Drop support for the scrolling Maps 

Then, we decided to drop support for the ability to make the Map scroll:
https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/ReleaseNotes/v2.2.0/scroll.gif

If you look at the GIF, you can see that the official plugin tries to support this feature, but actually fails at it: the Map tries desperately to follow the scroll, but is so late that it overlaps with the rest of the HTML.
The animation shown in version 2.1.1 is almost ridiculous, but even the one with v2.2.0 is really not great. No serious app would want to have this UX.

But in any case, most of the apps absolutely don't want to make their Map scroll. If you really need it, well, you can't use this fork.

### Add an API to update the DOM positions

As explained, the native layer which intercepts all touch events must always know:
* Which Maps are visible and/or clickable
* The DOM position of those maps
* The DOM position of all HTML elements that are inside those maps (those are the HTML elements that we want to be displayed on top of the Maps)

The official plugin does that automatically. For a very high price in CPU and memory.

This fork doesn't, so the developer needs to:
* tell the plugin which maps are "active". For this, we suggest to call _map.setClickable(false)_ whenever the user leaves the screen which contains the Map, and _map.setClickable(true)_ whenever the user enters it.
* call a new API _updateDomPositions(div)_ every time an HTML element is inserted/removed on top of the Map.

## What next?

We will maintain this fork for the years to come, because we use it and we need it.
If some people are interested in contributing to that light version of the plugin, we are happy to welcome them.