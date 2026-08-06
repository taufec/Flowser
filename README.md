# Flowser

Flowser is a small generic Android browser that opens a real interactive website in a movable floating window above other apps.

## Prototype features

- Enter any HTTP or HTTPS website URL.
- Missing schemes default to `https://`.
- Remembers the last URL on the device.
- Requests Android's **Display over other apps** permission.
- Movable floating WebView with refresh and close controls.
- JavaScript, DOM storage, zoom and in-window navigation.
- Supports Android 8.0 (API 26) and newer.

## Download the debug APK

1. Open the repository's **Actions** tab.
2. Open the latest successful **Android Build** run.
3. Download the `flowser-debug-apk` artifact.
4. Extract the ZIP and install `app-debug.apk` on the Android device.
5. Android may ask permission to install apps from the browser or file manager used to open the APK.

The debug APK is for prototype testing and is not Play Store signed.

## Test on Android

1. Open Flowser.
2. Enter a website, for example `example.com`.
3. Tap **Open floating browser**.
4. Grant **Display over other apps** permission and return to Flowser.
5. Drag the dark top bar to move the window.
6. Use `↻` to reload and `×` to close it.

## Build pipeline

GitHub Actions uses Java 17 and Gradle 8.7 to run:

```text
gradle testDebugUnitTest
gradle assembleDebug
```

The resulting APK is uploaded as the `flowser-debug-apk` workflow artifact.

## Current prototype limits

- One floating window at a time.
- Fixed window size; no resize handle yet.
- No tabs, bookmarks, downloads, ad blocking or release signing yet.
