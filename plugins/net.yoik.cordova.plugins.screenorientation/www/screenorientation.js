/*
The MIT License (MIT)

Copyright (c) 2014

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
var screenOrientation = {},
    Orientations = [
        'portrait-primary',
        // The orientation is in the primary portrait mode.
        'portrait-secondary',
        // The orientation is in the secondary portrait mode.
        'landscape-primary',
        // The orientation is in the primary landscape mode.
        'landscape-secondary',
        // The orientation is in the secondary landscape mode.
        'portrait',
        // The orientation is either portrait-primary or portrait-secondary.
        'landscape'
        // The orientation is either landscape-primary or landscape-secondary.
    ];

screenOrientation.Orientations = Orientations;
screenOrientation.currOrientation = 'unlocked';

screenOrientation.setOrientation = function(orientation) {
    //platform specific files override this function
    console.log('setOrientation not supported on device');
};

function addScreenOrientationApi(obj) {
    if (obj.unlockOrientation || obj.lockOrientation) {
        return;
    }

    obj.lockOrientation = function(orientation) {
        if (Orientations.indexOf(orientation) == -1) {
            console.log('INVALID ORIENTATION', orientation);
            return;
        }
        screenOrientation.currOrientation = orientation;
        screenOrientation.setOrientation(orientation);
    };

    obj.unlockOrientation = function() {
        screenOrientation.currOrientation = 'unlocked';
        screenOrientation.setOrientation('unlocked');
    };
}

addScreenOrientationApi(screen);
orientationChange();

function orientationChange() {
    var orientation;

    switch (window.orientation) {
        case 0:
             orientation = 'portrait-primary';
             break;
        case 90:
            orientation = 'landscape-secondary';
            break;
        case 180:
            orientation = 'portrait-secondary';
            break;
        case -90:
            orientation = 'landscape-primary';
            break;
        default:
            orientation = 'unknown';
    }

    screen.orientation = orientation;
}

window.addEventListener("orientationchange", orientationChange, true);

module.exports = screenOrientation;