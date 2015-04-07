#Cordova Screen Orientation Plugin

Cordova plugin to set/lock the screen orientation in a common way for iOS, Android and Blackberry 10.  From version 1.0.0 the interface is based on the [Screen Orientation API](http://www.w3.org/TR/screen-orientation/).

The plugin adds the following to the screen object:

__lockOrientation(ORIENTATION_STRING)__
lock the device orientation

__unlockOrientation()__
unlock the orientation

__orientation__
current orientation (ORIENTATION_STRING)

##Install

cordova plugin add net.yoik.cordova.plugins.screenorientation

###Source
https://github.com/yoik/cordova-yoik-screenorientation


##Supported Orientations

__portrait-primary__
The orientation is in the primary portrait mode.

__portrait-secondary__
The orientation is in the secondary portrait mode.

__landscape-primary__
The orientation is in the primary landscape mode.

__landscape-secondary__
The orientation is in the secondary landscape mode.

__portrait__
The orientation is either portrait-primary or portrait-secondary (sensor).

__landscape__
The orientation is either landscape-primary or landscape-secondary (sensor).

##Usage

    // set to either landscape
    screen.lockOrientation('landscape');

    // allow user rotate
    screen.unlockOrientation();

    // access current orientation
    console.log('Orientation is ' + screen.orientation);

##Events

Both android and iOS will fire the orientationchange event on the window object.
For this version of the plugin use the window object if you require notification.


For this plugin to follow the full API events should be fired on the screen object.
iOS and BB10 do not currently support events on the _screen_ object so custom event
handling will need to be added (Suggestions welcome!).

##Android Notes

The __screen.orientation__ property will not update when the phone is [rotated 180 degrees](http://www.quirksmode.org/dom/events/orientationchange.html).

##iOS Notes

The iOS version is a combination of the cordova JS callback _window.shouldRotateToOrientation_ and the workaround to recheck the orientation as implemented in https://github.com/Adlotto/cordova-plugin-recheck-screen-orientation.

__If you have a custom implementation of the _window.shouldRotateToOrientation_ it will have to be removed for the plugin to function as expected.__

####iOS6

There has been a few cases where the rotation does not change the width of the viewport

Issue [#1](https://github.com/yoik/cordova-yoik-screenorientation/issues/1) @dokterbob

>It seems to be related to having width=device-width, height=device-height in the meta viewport (which is part of the boilerplate phonegap/cordova app). It can be solved by updating the viewport with width=device-height, height=device-width or simply removing width and height altogether.

####iOS8

Versions prior to 1.2.0 will cause an application crash in iOS8 due to a change in presentViewController timing.

##BB10 Notes

Wraps the com.blackberry.app plugin functions, auto installed as a dependancy.

#Changelog

##1.3.2

* [#33](https://github.com/yoik/cordova-yoik-screenorientation/issues/33) iOS8 Delay Block

##1.3.0

* [#23](https://github.com/yoik/cordova-yoik-screenorientation/issues/23) iOS8 flicker

##1.2.0-1.2.1

* [#19](https://github.com/yoik/cordova-yoik-screenorientation/issues/19) iOS8 Crash



Pull requests welcome.
