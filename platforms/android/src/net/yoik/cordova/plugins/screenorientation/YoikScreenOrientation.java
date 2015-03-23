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
package net.yoik.cordova.plugins.screenorientation;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.util.Log;

public class YoikScreenOrientation extends CordovaPlugin {

    private static final String TAG = "YoikScreenOrientation";

    /**
     * Screen Orientation Constants
     */

    private static final String UNLOCKED = "unlocked";
    private static final String PORTRAIT_PRIMARY = "portrait-primary";
    private static final String PORTRAIT_SECONDARY = "portrait-secondary";
    private static final String LANDSCAPE_PRIMARY = "landscape-primary";
    private static final String LANDSCAPE_SECONDARY = "landscape-secondary";
    private static final String PORTRAIT = "portrait";
    private static final String LANDSCAPE = "landscape";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {

        Log.d(TAG, "execute action: " + action);

        // Route the Action
        if (action.equals("screenOrientation")) {
            return routeScreenOrientation(args, callbackContext);
        }

        // Action not found
        callbackContext.error("action not recognised");
        return false;
    }

    private boolean routeScreenOrientation(JSONArray args, CallbackContext callbackContext) {

        String action = args.optString(0);

        if (action.equals("set")) {

            String orientation = args.optString(1);

            Log.d(TAG, "Requested ScreenOrientation: " + orientation);

            Activity activity = cordova.getActivity();

            if (orientation.equals(UNLOCKED)) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            } else if (orientation.equals(LANDSCAPE_PRIMARY)) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else if (orientation.equals(PORTRAIT_PRIMARY)) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else if (orientation.equals(LANDSCAPE)) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else if (orientation.equals(PORTRAIT)) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            } else if (orientation.equals(LANDSCAPE_SECONDARY)) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            } else if (orientation.equals(PORTRAIT_SECONDARY)) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
            }

            callbackContext.success();
            return true;

        } else {
            callbackContext.error("ScreenOrientation not recognised");
            return false;
        }
    }
}