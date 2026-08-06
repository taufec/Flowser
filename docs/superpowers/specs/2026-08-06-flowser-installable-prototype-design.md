# Flowser Installable Prototype Design

## Goal

Deliver a generic Android floating browser prototype that can be built by GitHub Actions, downloaded as a debug APK, installed on Android 8.0+, and tested without Android Studio.

## Approved Scope

- Generic browser, not tied to Hermes or any fixed service.
- User enters a website URL in the launcher screen.
- App stores the most recently used URL locally.
- App requests Android "display over other apps" permission when required.
- Starting Flowser opens a movable floating window above other apps.
- Floating window contains a real interactive WebView, a drag bar, refresh button, and close button.
- HTTPS is the default when the user omits a scheme.
- GitHub Actions builds a debug APK and publishes it as an artifact.

## Architecture

`MainActivity` owns URL entry, validation feedback, saved URL loading, overlay permission flow, and service launch. `UrlNormalizer` is a pure Kotlin utility that turns user input into a valid HTTP(S) URL and is covered by JVM unit tests. `FloatingBrowserService` owns the overlay window, WebView lifecycle, dragging, refresh, and close behavior. The app uses programmatic Android views to keep the prototype small and avoid layout/theme coupling.

## Data Flow

1. User opens Flowser and enters a URL.
2. `UrlNormalizer.normalize()` trims the value and adds `https://` when needed.
3. `MainActivity` saves the normalized URL in `SharedPreferences`.
4. If overlay permission is missing, Android settings is opened.
5. When permission is available, `MainActivity` starts `FloatingBrowserService` with the URL extra.
6. The service creates the overlay and loads the URL in its WebView.

## Error Handling

- Empty or malformed URL input shows an inline error and does not start the service.
- Service exits immediately if overlay permission is unavailable.
- Web navigation stays inside the WebView.
- WebView resources and overlay views are removed in `onDestroy()`.
- Cleartext HTTP is allowed for prototype testing of local dashboards; HTTPS remains the default.

## Build and Verification

- Android Gradle Plugin 8.5.2, Kotlin 2.0.21, Gradle 8.7, Java 17.
- `testDebugUnitTest` verifies URL normalization.
- `assembleDebug` creates `app-debug.apk`.
- GitHub Actions uploads the APK as `flowser-debug-apk`.
- Success requires a green workflow run and a downloadable non-expired artifact.

## Out of Scope

- Multiple simultaneous windows or tabs.
- Resize handles, minimize bubble, bookmarks, ad blocking, download manager, or Play Store signing.
- Production security hardening and release signing.
