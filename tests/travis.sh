#!/bin/bash
set -o nounset
set -o errexit

# run tests appropriate for platform
#if [[ "${CORDOVA_PLATFORM}" == "browser" ]]; then
#    npm run test:browser
#fi
if [[ "${CORDOVA_PLATFORM}" == "ios" ]]; then
    sudo gem install cocoapods
    npm install -g ios-sim ios-deploy
    npm run test:ios
fi
echo "CORDOVA_PLATFORM=${CORDOVA_PLATFORM}"
echo "TRAVIS_OS_NAME=${TRAVIS_OS_NAME}"

if [[ "${CORDOVA_PLATFORM}" == "android" ]]; then
    echo "y" | sdkmanager "platforms;android-${ANDROID_API}"; > /dev/null;
    echo "y" | android update sdk -a --no-ui --filter android-${ANDROID_API} > /dev/null
    echo "y" | android update sdk -a --no-ui --filter sys-img-armeabi-v7a-android-${ANDROID_API} > /dev/null
    echo no | android create avd --force -n test -t android-${ANDROID_API} --abi armeabi-v7a > /dev/null
    emulator -avd test -no-skin -no-window &
    android-wait-for-emulator
    adb shell input keyevent 82  &
    adb wait-for-device get-serialno
    npm run test:android
fi
