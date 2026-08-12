# Flowser

Flowser is a generic Android browser that opens an interactive website in a floating window above other apps.

## Flowser 0.2 prototype

- Open any HTTP or HTTPS website.
- Missing schemes default to `https://`.
- Remembers the last URL, position, window size, desktop mode and zoom.
- Drag the toolbar title area to move the window.
- Drag the bottom-right handle to resize.
- Maximize and restore from the toolbar or menu.
- Minimize into a draggable 56 dp bubble without destroying the active WebView session.
- Tap the bubble to restore it.
- Bubble snaps to the nearest screen edge.
- Long-press the bubble for Restore and Close.
- Drag the bubble to the bottom close target to close the session.
- Responsive toolbar for narrow phone windows and wide tablet windows.
- Browser menu with address editing, navigation, home, open externally, copy, share, desktop mode and zoom controls.
- Foreground notification with Restore, Minimize and Close actions.
- Touches outside the floating window pass to the app underneath.
- JavaScript, DOM storage, zoom and in-window navigation.
- Supports Android 8.0 (API 26) and newer.

## Install the debug APK

1. Open the repository **Actions** tab.
2. Open the latest successful **Android Build** run for the desired branch.
3. Download the `flowser-debug-apk` artifact.
4. Extract the ZIP and install `app-debug.apk`.
5. Android may ask permission to install apps from the browser or file manager.

The debug APK is prototype-signed and is not a Play Store release build. Version 0.2 uses the same application ID as 0.1, so it can be installed over the previous debug APK while preserving app data when Android accepts the matching debug signature.

## Test controls

1. Open Flowser and enter a website.
2. Grant **Display over other apps** permission.
3. Drag the toolbar title to move the window.
4. Drag the dark handle at the bottom-right to resize.
5. Tap `_` to minimize into a bubble.
6. Drag and release the bubble to test edge snapping.
7. Tap the bubble to restore the same page session.
8. Use `□` to maximize and `❐` to restore.
9. Use `⋮` for browser actions.
10. Test Restore, Minimize and Close from the Android notification.

## Acceptance checks

- Window remains inside usable screen bounds after dragging and resizing.
- Toolbar remains reachable.
- Maximize/restore preserves the previous normal size.
- Minimize/restore does not intentionally reload the page or reset WebView history.
- Bubble can drag, snap, restore and close.
- App behind Flowser remains touchable outside the overlay.
- Narrow windows use a compact toolbar.
- Rotation clamps the window or bubble to the new screen bounds.
- Last URL, geometry, desktop mode and zoom persist.

## Build pipeline

GitHub Actions uses Java 17 and Gradle 8.7 to run:

```text
gradle --no-daemon testDebugUnitTest
gradle --no-daemon assembleDebug
```

The resulting APK is uploaded as `flowser-debug-apk`.

## Current limits

- One browser session and one floating window at a time.
- Resize is from the bottom-right handle only.
- No tabs, bookmarks, full history UI, file picker, download manager, ad blocker or release signing yet.
- Runtime behavior still depends on Android version and device manufacturer overlay policies.
