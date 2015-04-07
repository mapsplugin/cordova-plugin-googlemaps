/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/

package org.apache.cordova;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
//import android.webkit.WebBackForwardList;
//import android.webkit.WebHistoryItem;
//import android.webkit.WebChromeClient;
//import android.webkit.WebSettings;
//import android.webkit.WebView;
//import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import org.xwalk.core.XWalkNavigationHistory;
import org.xwalk.core.XWalkNavigationItem;
import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.XWalkView;

/*
 * This class is our web view.
 *
 * @see <a href="http://developer.android.com/guide/webapps/webview.html">WebView guide</a>
 * @see <a href="http://developer.android.com/reference/android/webkit/WebView.html">WebView</a>
 */
public class CordovaWebView extends XWalkView {

    public static final String TAG = "CordovaWebView";
    public static final String CORDOVA_VERSION = "3.6.3";

    private HashSet<Integer> boundKeyCodes = new HashSet<Integer>();

    public PluginManager pluginManager;
    private boolean paused;

    private BroadcastReceiver receiver;


    /** Activities and other important classes **/
    private CordovaInterface cordova;
    CordovaWebViewClient viewClient;
    private CordovaChromeClient chromeClient;

    // Flag to track that a loadUrl timeout occurred
    int loadUrlTimeout = 0;

    private long lastMenuEventTime = 0;

    CordovaBridge bridge;

    private CordovaResourceApi resourceApi;
    private Whitelist internalWhitelist;
    private Whitelist externalWhitelist;

    // The URL passed to loadUrl(), not necessarily the URL of the current page.
    String loadedUrl;
    private CordovaPreferences preferences;

    class ActivityResult {
        
        int request;
        int result;
        Intent incoming;
        
        public ActivityResult(int req, int res, Intent intent) {
            request = req;
            result = res;
            incoming = intent;
        }

        
    }
    
    static final FrameLayout.LayoutParams COVER_SCREEN_GRAVITY_CENTER =
            new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            Gravity.CENTER);
    
    public CordovaWebView(Context context) {
        this(context, null);
    }

    public CordovaWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Deprecated
    public CordovaWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
    }

    @TargetApi(11)
    @Deprecated
    public CordovaWebView(Context context, AttributeSet attrs, int defStyle, boolean privateBrowsing) {
        super(context, attrs);
    }

    // Use two-phase init so that the control will work with XML layouts.
    public void init(CordovaInterface cordova, CordovaWebViewClient webViewClient, CordovaChromeClient webChromeClient,
            List<PluginEntry> pluginEntries, Whitelist internalWhitelist, Whitelist externalWhitelist,
            CordovaPreferences preferences) {
        if (this.cordova != null) {
            throw new IllegalStateException();
        }
        this.cordova = cordova;
        this.viewClient = webViewClient;
        this.chromeClient = webChromeClient;
        this.internalWhitelist = internalWhitelist;
        this.externalWhitelist = externalWhitelist;
        this.preferences = preferences;
        // There are no super.setWebChromeClient and super.setWebViewClient function in Xwalk.
        // so align with Cordova upstream.
        // https://github.com/apache/cordova-android/commit/705991e5b037743e632934b3c6ee98976e18d3f8#diff-b97e89dfb7e195850e6e2d3b531487feR561
        super.setResourceClient(webViewClient);
        super.setUIClient(webChromeClient);

        pluginManager = new PluginManager(this, this.cordova, pluginEntries);
        bridge = new CordovaBridge(pluginManager, new NativeToJsMessageQueue(this, cordova));
        resourceApi = new CordovaResourceApi(this.getContext(), pluginManager);

        pluginManager.addService("App", "org.apache.cordova.App");
        initWebViewSettings();
        exposeJsInterface();
    }

    @SuppressWarnings("deprecation")
    private void initIfNecessary() {
        if (pluginManager == null) {
            Log.w(TAG, "CordovaWebView.init() was not called. This will soon be required.");
            // Before the refactor to a two-phase init, the Context needed to implement CordovaInterface. 
            CordovaInterface cdv = (CordovaInterface)getContext();
            if (!Config.isInitialized()) {
                Config.init(cdv.getActivity());
            }
            init(cdv, makeWebViewClient(cdv), makeWebChromeClient(cdv), Config.getPluginEntries(), Config.getWhitelist(), Config.getExternalWhitelist(), Config.getPreferences());
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @SuppressWarnings("deprecation")
    private void initWebViewSettings() {
        //this.setInitialScale(0);
        this.setVerticalScrollBarEnabled(false);
        // TODO: The Activity is the one that should call requestFocus().
        if (shouldRequestFocusOnInit()) {
            this.setFocusableInTouchMode(true);
            this.requestFocusFromTouch();
        }

        // Enable JavaScript
        //XWalkSettings settings = this.getSettings();
        //if (settings == null) return;
        // wang16: covered by XWalkPreferences setting in static code.
        //settings.setJavaScriptEnabled(true);
        //settings.setJavaScriptCanOpenWindowsAutomatically(true);
        // nhu: N/A
        //settings.setLayoutAlgorithm(LayoutAlgorithm.NORMAL);

        //We don't save any form data in the application
        // nhu: N/A
        //settings.setSaveFormData(false);
        //settings.setSavePassword(false);
        
        // wang16: covered by XWalkPreferences setting in static code.
        //settings.setAllowUniversalAccessFromFileURLs(true);
        // Enable database
        // We keep this disabled because we use or shim to get around DOM_EXCEPTION_ERROR_16
        String databasePath = getContext().getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
        //settings.setDatabaseEnabled(true);
        //TODO: bring it back when it's ready in the XWalk.
        //settings.setDatabasePath(databasePath);
        
        
        //Determine whether we're in debug or release mode, and turn on Debugging!
        ApplicationInfo appInfo = getContext().getApplicationContext().getApplicationInfo();
        if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            enableRemoteDebugging();
        }
        
        //settings.setGeolocationDatabasePath(databasePath);

        // Enable DOM storage
        // wang16: default value in xwalk is true.
        //settings.setDomStorageEnabled(true);

        // Enable built-in geolocation
        // wang16: default value in xwalk is true.
        //settings.setGeolocationEnabled(true);
        
        // Enable AppCache
        // Fix for CB-2282
        // nhu: N/A
        //settings.setAppCacheMaxSize(5 * 1048576);

        // wang16: setAppCachePath is not implemented in xwalk indeed.
        //settings.setAppCachePath(pathToCache);
        // wang16: default value in xwalk is true.
        //settings.setAppCacheEnabled(true);
    }

    public CordovaChromeClient makeWebChromeClient(CordovaInterface cordova) {
        return new CordovaChromeClient(cordova, this);
    }

    public CordovaWebViewClient makeWebViewClient(CordovaInterface cordova) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return new CordovaWebViewClient(cordova, this);
        }
        return new IceCreamCordovaWebViewClient(cordova, this);
    }
    
    public void enableRemoteDebugging() {
        XWalkPreferences.setValue(XWalkPreferences.REMOTE_DEBUGGING, true);
    }

	/**
	 * Override this method to decide whether or not you need to request the
	 * focus when your application start
	 * 
	 * @return true unless this method is overriden to return a different value
	 */
    protected boolean shouldRequestFocusOnInit() {
		return true;
	}

    private void exposeJsInterface() {
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)) {
            Log.i(TAG, "Disabled addJavascriptInterface() bridge since Android version is old.");
            // Bug being that Java Strings do not get converted to JS strings automatically.
            // This isn't hard to work-around on the JS side, but it's easier to just
            // use the prompt bridge instead.
            return;            
        } 
        this.addJavascriptInterface(new ExposedJsApi(bridge), "_cordovaNative");
    }

    /**
     * Set the WebViewClient.
     * There is no setWebViewClient in xwalk, so don't override setWebViewClient function
     * https://github.com/apache/cordova-android/commit/caeb86843ddca712b5bf1dfbdac9005edce98100
     *
     * @param client
     */
    public void setWebViewClient(CordovaWebViewClient client) {
        this.viewClient = client;
        super.setResourceClient(client);
    }

    /**
     * Set the WebChromeClient.
     *
     * @param client
     */
    public void setWebChromeClient(CordovaChromeClient client) {
        this.chromeClient = client;
        super.setUIClient(client);
    }
    
    public CordovaChromeClient getWebChromeClient() {
        return this.chromeClient;
    }

    
    public Whitelist getWhitelist() {
        return this.internalWhitelist;
    }

    public Whitelist getExternalWhitelist() {
        return this.externalWhitelist;
    }

    /**
     * Load the url into the webview.
     *
     * @param url
     */
    @Override
    public void load(String url, String content) {
        if (url.equals("about:blank") || url.startsWith("javascript:")) {
            this.loadUrlNow(url);
        }
        else {
            this.loadUrlIntoView(url);
        }
    }

    public void loadUrl(String url) {
        load(url, null);
    }

    /**
     * Load the url into the webview after waiting for period of time.
     * This is used to display the splashscreen for certain amount of time.
     *
     * @param url
     * @param time              The number of ms to wait before loading webview
     */
    @Deprecated
    public void loadUrl(final String url, int time) {
        if(url == null)
        {
            this.loadUrlIntoView(Config.getStartUrl());
        }
        else
        {
            this.loadUrlIntoView(url);
        }
    }

    public void loadUrlIntoView(final String url) {
        loadUrlIntoView(url, true);
    }

    /**
     * Load the url into the webview.
     *
     * @param url
     */
    public void loadUrlIntoView(final String url, boolean recreatePlugins) {
        LOG.d(TAG, ">>> loadUrl(" + url + ")");

        initIfNecessary();

        if (recreatePlugins) {
            this.loadedUrl = url;
            if (this.pluginManager != null) {
                this.pluginManager.init();
            }
        }

        // Create a timeout timer for loadUrl
        final CordovaWebView me = this;
        final int currentLoadUrlTimeout = me.loadUrlTimeout;
        final int loadUrlTimeoutValue = Integer.parseInt(this.getProperty("LoadUrlTimeoutValue", "20000"));

        // Timeout error method
        final Runnable loadError = new Runnable() {
            public void run() {
                me.stopLoading();
                LOG.e(TAG, "CordovaWebView: TIMEOUT ERROR!");
                if (viewClient != null) {
                    viewClient.onReceivedLoadError(me, -6, "The connection to the server was unsuccessful.", url);
                }
            }
        };

        // Timeout timer method
        final Runnable timeoutCheck = new Runnable() {
            public void run() {
                try {
                    synchronized (this) {
                        wait(loadUrlTimeoutValue);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // If timeout, then stop loading and handle error
                if (me.loadUrlTimeout == currentLoadUrlTimeout) {
                    me.cordova.getActivity().runOnUiThread(loadError);
                }
            }
        };

        // Load url
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                cordova.getThreadPool().execute(timeoutCheck);
                me.loadUrlNow(url);
            }
        });
    }

    /**
     * Load URL in webview.
     *
     * @param url
     */
    void loadUrlNow(String url) {
        if (LOG.isLoggable(LOG.DEBUG) && !url.startsWith("javascript:")) {
            LOG.d(TAG, ">>> loadUrlNow()");
        }
        if (url.startsWith("file://") || url.startsWith("javascript:") || internalWhitelist.isUrlWhiteListed(url)) {
            super.load(url, null);
        }
    }

    /**
     * Load the url into the webview after waiting for period of time.
     * This is used to display the splashscreen for certain amount of time.
     *
     * @param url
     * @param time              The number of ms to wait before loading webview
     */
    public void loadUrlIntoView(final String url, final int time) {

        // If not first page of app, then load immediately
        // Add support for browser history if we use it.
        if ((url.startsWith("javascript:")) || this.getNavigationHistory().canGoBack()) {
        }

        // If first page, then show splashscreen
        else {

            LOG.d(TAG, "loadUrlIntoView(%s, %d)", url, time);

            // Send message to show splashscreen now if desired
            this.postMessage("splashscreen", "show");
        }

        // Load url
        this.loadUrlIntoView(url);
    }
    
    @Override
    public void stopLoading() {
        chromeClient.isCurrentlyLoading = false;
        super.stopLoading();
    }
    
    public void onScrollChanged(int l, int t, int oldl, int oldt)
    {
        super.onScrollChanged(l, t, oldl, oldt);
        //We should post a message that the scroll changed
        ScrollEvent myEvent = new ScrollEvent(l, t, oldl, oldt, this);
        this.postMessage("onScrollChanged", myEvent);
    }
    
    /**
     * Send JavaScript statement back to JavaScript.
     * Deprecated (https://issues.apache.org/jira/browse/CB-6851)
     * Instead of executing snippets of JS, you should use the exec bridge
     * to create a Java->JS communication channel.
     * To do this:
     * 1. Within plugin.xml (to have your JS run before deviceready):
     *    <js-module><runs/></js-module>
     * 2. Within your .js (call exec on start-up):
     *    require('cordova/channel').onCordovaReady.subscribe(function() {
     *      require('cordova/exec')(win, null, 'Plugin', 'method', []);
     *      function win(message) {
     *        ... process message from java here ...
     *      }
     *    });
     * 3. Within your .java:
     *    PluginResult dataResult = new PluginResult(PluginResult.Status.OK, CODE);
     *    dataResult.setKeepCallback(true);
     *    savedCallbackContext.sendPluginResult(dataResult);
     */
    @Deprecated
    public void sendJavascript(String statement) {
        this.bridge.getMessageQueue().addJavaScript(statement);
    }

    /**
     * Send a plugin result back to JavaScript.
     * (This is a convenience method)
     *
     * @param result
     * @param callbackId
     */
    public void sendPluginResult(PluginResult result, String callbackId) {
        this.bridge.getMessageQueue().addPluginResult(result, callbackId);
    }

    /**
     * Send a message to all plugins.
     *
     * @param id            The message id
     * @param data          The message data
     */
    public void postMessage(String id, Object data) {
        if (this.pluginManager != null) {
            this.pluginManager.postMessage(id, data);
        }
    }


    /**
     * Go to previous page in history.  (We manage our own history)
     *
     * @return true if we went back, false if we are already at top
     */
    public boolean backHistory() {
        // Check webview first to see if there is a history
        // This is needed to support curPage#diffLink, since they are added to appView's history, but not our history url array (JQMobile behavior)
        if (super.getNavigationHistory().canGoBack()) {
            super.getNavigationHistory().navigate(XWalkNavigationHistory.Direction.BACKWARD, 1);
            return true;
        }
        return false;
    }


    /**
     * Load the specified URL in the Cordova webview or a new browser instance.
     *
     * NOTE: If openExternal is false, only URLs listed in whitelist can be loaded.
     *
     * @param url           The url to load.
     * @param openExternal  Load url in browser instead of Cordova webview.
     * @param clearHistory  Clear the history stack, so new page becomes top of history
     * @param params        Parameters for new app
     */
    public void showWebPage(String url, boolean openExternal, boolean clearHistory, HashMap<String, Object> params) {
        LOG.d(TAG, "showWebPage(%s, %b, %b, HashMap", url, openExternal, clearHistory);

        // If clearing history
        if (clearHistory) {
            this.getNavigationHistory().clear();
        }

        // If loading into our webview
        if (!openExternal) {

            // Make sure url is in whitelist
            if (url.startsWith("file://") || internalWhitelist.isUrlWhiteListed(url)) {
                // TODO: What about params?
                // Load new URL
                this.loadUrl(url);
                return;
            }
            // Load in default viewer if not
            LOG.w(TAG, "showWebPage: Cannot load URL into webview since it is not in white list.  Loading into browser instead. (URL=" + url + ")");
        }
        try {
            // Omitting the MIME type for file: URLs causes "No Activity found to handle Intent".
            // Adding the MIME type to http: URLs causes them to not be handled by the downloader.
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(url);
            if ("file".equals(uri.getScheme())) {
                intent.setDataAndType(uri, resourceApi.getMimeType(uri));
            } else {
                intent.setData(uri);
            }
            cordova.getActivity().startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            LOG.e(TAG, "Error loading url " + url, e);
        }
    }

    /**
     * Get string property for activity.
     *
     * @param name
     * @param defaultValue
     * @return the String value for the named property
     */
    public String getProperty(String name, String defaultValue) {
        Bundle bundle = this.cordova.getActivity().getIntent().getExtras();
        if (bundle == null) {
            return defaultValue;
        }
        name = name.toLowerCase(Locale.getDefault());
        Object p = bundle.get(name);
        if (p == null) {
            return defaultValue;
        }
        return p.toString();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if(boundKeyCodes.contains(keyCode))
        {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    this.loadUrl("javascript:cordova.fireDocumentEvent('volumedownbutton');");
                    return true;
            }
            // If volumeup key
            else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    this.loadUrl("javascript:cordova.fireDocumentEvent('volumeupbutton');");
                    return true;
            }
            else
            {
                return super.onKeyDown(keyCode, event);
            }
        }
        else if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            return !(this.startOfHistory()) || isButtonPlumbedToJs(KeyEvent.KEYCODE_BACK);
        }
        else if(keyCode == KeyEvent.KEYCODE_MENU)
        {
            //How did we get here?  Is there a childView?
            View childView = this.getFocusedChild();
            if(childView != null)
            {
                //Make sure we close the keyboard if it's present
                InputMethodManager imm = (InputMethodManager) cordova.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(childView.getWindowToken(), 0);
                cordova.getActivity().openOptionsMenu();
                return true;
            } else {
                return super.onKeyDown(keyCode, event);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return super.dispatchKeyEvent(event);
        }
        int keyCode = event.getKeyCode();
        // If back key
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // A custom view is currently displayed  (e.g. playing a video)
            if (this.hasEnteredFullscreen()) {
                this.leaveFullscreen();
                return true;
            } else {
                // The webview is currently displayed
                // If back key is bound, then send event to JavaScript
                if (isButtonPlumbedToJs(KeyEvent.KEYCODE_BACK)) {
                    this.loadUrl("javascript:cordova.fireDocumentEvent('backbutton');");
                    return true;
                } else {
                    // If not bound
                    // Go to previous page in webview if it is possible to go back
                    if (this.backHistory()) {
                        return true;
                    }
                    // If not, then invoke default behavior
                    else {
                        //this.activityState = ACTIVITY_EXITING;
                        //return false;
                        // If they hit back button when app is initializing, app should exit instead of hang until initialization (CB2-458)
                        this.cordova.getActivity().finish();
                        return false;
                    }
                }
            }
        }
        // Legacy
        else if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (this.lastMenuEventTime < event.getEventTime()) {
                this.loadUrl("javascript:cordova.fireDocumentEvent('menubutton');");
            }
            this.lastMenuEventTime = event.getEventTime();
            return super.dispatchKeyEvent(event);
        }
        // If search key
        else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            this.loadUrl("javascript:cordova.fireDocumentEvent('searchbutton');");
            return true;
        }
        // Request focus on XWalkView
        else if((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) &&
                !isFocused()) {
            requestFocus();
            return super.dispatchKeyEvent(event);
        }

        //Does webkit change this behavior?
        return super.dispatchKeyEvent(event);
    }

    public void setButtonPlumbedToJs(int keyCode, boolean override) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_BACK:
                // TODO: Why are search and menu buttons handled separately?
                if (override) {
                    boundKeyCodes.add(keyCode);
                } else {
                    boundKeyCodes.remove(keyCode);
                }
                return;
            default:
                throw new IllegalArgumentException("Unsupported keycode: " + keyCode);
        }
    }

    @Deprecated // Use setButtonPlumbedToJs() instead.
    public void bindButton(boolean override)
    {
        setButtonPlumbedToJs(KeyEvent.KEYCODE_BACK, override);
    }

    @Deprecated // Use setButtonPlumbedToJs() instead.
    public void bindButton(String button, boolean override) {
        if (button.compareTo("volumeup")==0) {
            setButtonPlumbedToJs(KeyEvent.KEYCODE_VOLUME_UP, override);
        }
        else if (button.compareTo("volumedown")==0) {
            setButtonPlumbedToJs(KeyEvent.KEYCODE_VOLUME_DOWN, override);
        }
    }

    @Deprecated // Use setButtonPlumbedToJs() instead.
    public void bindButton(int keyCode, boolean keyDown, boolean override) {
        setButtonPlumbedToJs(keyCode, override);
    }

    @Deprecated // Use isButtonPlumbedToJs
    public boolean isBackButtonBound()
    {
        return isButtonPlumbedToJs(KeyEvent.KEYCODE_BACK);
    }

    public boolean isButtonPlumbedToJs(int keyCode)
    {
        return boundKeyCodes.contains(keyCode);
    }


    @Override
    public void pauseTimers() {
        // This is called by XWalkViewInternal.onActivityStateChange().
        // We don't want them paused by default though.
    }

    public void pauseTimersForReal() {
        super.pauseTimers();
    }

    public void handlePause(boolean keepRunning)
    {
        LOG.d(TAG, "Handle the pause");
        // Send pause event to JavaScript
        this.loadUrl("javascript:try{cordova.fireDocumentEvent('pause');}catch(e){console.log('exception firing pause event from native');};");

        // Forward to plugins
        if (this.pluginManager != null) {
            this.pluginManager.onPause(keepRunning);
        }

        // If app doesn't want to run in background
        if (!keepRunning) {
            // Pause JavaScript timers (including setInterval)
            this.pauseTimersForReal();
        }
        paused = true;
   
    }
    
    public void handleResume(boolean keepRunning, boolean activityResultKeepRunning)
    {

        this.loadUrl("javascript:try{cordova.fireDocumentEvent('resume');}catch(e){console.log('exception firing resume event from native');};");
        
        // Forward to plugins
        if (this.pluginManager != null) {
            this.pluginManager.onResume(keepRunning);
        }

        // Resume JavaScript timers (including setInterval)
        this.resumeTimers();
        paused = false;
    }
    
    public void handleDestroy()
    {
        // Send destroy event to JavaScript
        this.loadUrl("javascript:try{cordova.require('cordova/channel').onDestroy.fire();}catch(e){console.log('exception firing destroy event from native');};");

        // Load blank page so that JavaScript onunload is called
        this.loadUrl("about:blank");

        // Forward to plugins
        if (this.pluginManager != null) {
            this.pluginManager.onDestroy();
        }

        // unregister the receiver
        if (this.receiver != null) {
            try {
                getContext().unregisterReceiver(this.receiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering configuration receiver: " + e.getMessage(), e);
            }
        }
        this.onDestroy();
    }
    
    @Override
    public boolean onNewIntent(Intent intent)
    {
        if (super.onNewIntent(intent)) return true;
        //Forward to plugins
        if (this.pluginManager != null) {
            this.pluginManager.onNewIntent(intent);
        }
        return false;
    }
    
    public boolean isPaused()
    {
        return paused;
    }

    @Deprecated // This never did anything.
    public boolean hadKeyEvent() {
        return false;
    }

    public void printBackForwardList() {
        XWalkNavigationHistory currentList = this.getNavigationHistory();
        int currentSize = currentList.size();
        for(int i = 0; i < currentSize; ++i)
        {
            XWalkNavigationItem item = currentList.getItemAt(i);
            String url = item.getUrl();
            LOG.d(TAG, "The URL at index: " + Integer.toString(i) + " is " + url );
        }
    }
    
    
    //Can Go Back is BROKEN!
    public boolean startOfHistory()
    {
        XWalkNavigationHistory currentList = this.getNavigationHistory();
        XWalkNavigationItem item = currentList.getItemAt(0);
        if( item!=null){	// Null-fence in case they haven't called loadUrl yet (CB-2458)
	        String url = item.getUrl();
	        String currentUrl = this.getUrl();
	        LOG.d(TAG, "The current URL is: " + currentUrl);
	        LOG.d(TAG, "The URL at item 0 is: " + url);
	        return currentUrl.equals(url);
        }
        return false;
    }

    @Override
    public boolean restoreState(Bundle savedInstanceState)
    {
        boolean result = super.restoreState(savedInstanceState);
        Log.d(TAG, "WebView restoration crew now restoring!");
        //Initialize the plugin manager once more
        if (this.pluginManager != null) {
            this.pluginManager.init();
        }
        return result;
    }

    @Deprecated // This never did anything
    public void storeResult(int requestCode, int resultCode, Intent intent) {
    }
    
    public CordovaResourceApi getResourceApi() {
        return resourceApi;
    }

    public CordovaPreferences getPreferences() {
        return preferences;
    }

    static {
        // XWalkPreferencesInternal.ENABLE_JAVASCRIPT
        XWalkPreferences.setValue("enable-javascript", true);
        // XWalkPreferencesInternal.JAVASCRIPT_CAN_OPEN_WINDOW
        XWalkPreferences.setValue("javascript-can-open-window", true);
        // XWalkPreferencesInternal.ALLOW_UNIVERSAL_ACCESS_FROM_FILE
        XWalkPreferences.setValue("allow-universal-access-from-file", true);
        // XWalkPreferencesInternal.SUPPORT_MULTIPLE_WINDOWS
        XWalkPreferences.setValue("support-multiple-windows", false);
    }
}
