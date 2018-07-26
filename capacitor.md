# How to install to @ionic/capacitor project

### Preparation
    ```
    $> node -v
    8.3.0 (or higher)

    // update npm
    $> npm -g install npm

    // update npx
    $> npm -g install npx
    ```

#### 1. Install capacitor or create a fresh project
  - Create a fresh project
    ```
    $> mkdir myApp

    $> cd myApp

    // Install @capacitor into local
    $> npx install --save @capacitor/core @capacitor/cli

    // Create a project
    $> npx @capacitor/cli create
    ```

  - Use capacitor for exists project
    ```
    $> cd myApp

    // Install @capacitor into local
    $> npx install --save @capacitor/core @capacitor/cli

    // Add capacitor into your project
    $> npx cap init
    ```

#### 2. Add android/iOS platform
  ```
  // Move to project folder
  $> cd testapp

  // Add Android platform
  $> npx cap add android

  // Add iOS platform
  $> npx cap add ios

  // copy all necessary files into real project directory.
  $> npx cap sync
  ```

#### 3. Install cordova-plugin-googlemaps plugin
  ```
  // install cordova-plugin-googlemaps
  $> npm install cordova-plugin-googlemaps

  // Update the project
  $> npx cap update

  // set variable for Android
  $> npx gmaps variable API_KEY_FOR_ANDROID (your_api_key_for_android)

  // set variable for iOS
  $> npx gmaps variable API_KEY_FOR_IOS (your_api_key_for_ios)
  ```
