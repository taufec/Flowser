# Flowser

Flowser is a generic Android browser that opens an interactive website in a floating window above other apps.

## Flowser 0.3

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
- Permanent release-signing support for future in-place APK updates.

## Install the signed release APK

After the permanent signing secrets are configured:

1. Open the repository **Actions** tab.
2. Open a successful **Android Build** run from `main` or a manually dispatched release build.
3. Download the `flowser-release-apk` artifact.
4. Extract the ZIP and install `app-release.apk`.
5. For later versions, Android should offer to update the installed Flowser while preserving app data when the application ID, signing certificate and version ordering match.

### One-time migration from old prototype APKs

Old Flowser builds used debug signing. A prototype already installed on a device may have a different debug certificate from the new permanent release. If Android refuses the first signed release as an update, uninstall the old prototype once, install the permanently signed release, and sign in to websites again once.

From that point onward, keep the same permanent Flowser signing key for every release. Normal in-place updates preserve Flowser app data and WebView storage unless Flowser or the website explicitly clears or invalidates them.

See [`docs/RELEASE_SIGNING.md`](docs/RELEASE_SIGNING.md) for the permanent key setup and backup procedure.

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
- A signed release with a higher `versionCode` can update the previous permanently signed release without uninstalling it.

## Build pipeline

GitHub Actions uses Java 17 and Gradle 8.7.

Pull requests and verification builds run:

```text
gradle --no-daemon testDebugUnitTest
gradle --no-daemon assembleDebug
```

Main/manual release builds additionally reconstruct the permanent keystore from GitHub Actions secrets and run:

```text
gradle --no-daemon assembleRelease
```

The distributable artifact is `flowser-release-apk`. The workflow fails instead of producing a release APK when permanent signing secrets are missing.

## Current limits

- One browser session and one floating window at a time.
- Resize is from the bottom-right handle only.
- No tabs, bookmarks, full history UI, file picker, download manager, ad blocker or built-in updater yet.
- The permanent signing keystore must be generated once and configured in GitHub Actions before the first permanent release artifact can be produced.
- Runtime behavior still depends on Android version and device manufacturer overlay policies.
