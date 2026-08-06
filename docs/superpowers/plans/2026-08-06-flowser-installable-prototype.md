# Flowser Installable Prototype Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and publish an installable Android debug APK for a generic movable floating WebView browser.

**Architecture:** A launcher activity validates and stores the URL, then starts an overlay service after Android permission is granted. A pure URL normalizer is unit-tested. GitHub Actions runs unit tests, builds the debug APK, and uploads it as an artifact.

**Tech Stack:** Kotlin 2.0.21, Android Gradle Plugin 8.5.2, Gradle 8.7, Java 17, Android SDK 35, JUnit 4, GitHub Actions.

## Global Constraints

- Minimum Android version: API 26 (Android 8.0).
- Generic browser only; no Hermes-specific behavior.
- Default missing URL scheme to HTTPS.
- Store only the last URL in local SharedPreferences.
- Overlay includes drag, refresh, close, and interactive WebView.
- Debug APK only; no release signing.

---

### Task 1: Add URL normalization test and implementation

**Files:**
- Create: `app/src/test/java/com/flowser/app/UrlNormalizerTest.kt`
- Create: `app/src/main/java/com/flowser/app/UrlNormalizer.kt`
- Modify: `app/build.gradle`

**Interfaces:**
- Produces: `object UrlNormalizer { fun normalize(input: String): String? }`

- [ ] Add JUnit 4 dependency.
- [ ] Write tests for trimming, HTTPS defaulting, preserving HTTP(S), and rejecting blank/unsupported schemes.
- [ ] Run `gradle testDebugUnitTest` and confirm RED because `UrlNormalizer` is missing.
- [ ] Add the minimal implementation.
- [ ] Run `gradle testDebugUnitTest` and confirm GREEN.
- [ ] Commit.

### Task 2: Replace launcher activity with URL and permission flow

**Files:**
- Modify: `app/src/main/java/com/flowser/app/MainActivity.kt`
- Delete: `app/src/main/java/com/flowser/app/SettingsActivity.kt`
- Delete: `app/src/main/res/layout/activity_main.xml`

**Interfaces:**
- Consumes: `UrlNormalizer.normalize(input)`.
- Produces: service intent extra `FloatingBrowserService.EXTRA_URL`.

- [ ] Build launcher UI programmatically with title, URL field, validation text, and start button.
- [ ] Load and save `last_url` in SharedPreferences.
- [ ] Request overlay permission through `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`.
- [ ] Start the service after permission is available.
- [ ] Commit.

### Task 3: Implement movable floating browser service

**Files:**
- Replace: `app/src/main/java/com/flowser/app/FloatingService.kt`
- Delete: `app/src/main/res/layout/floating_window.xml`

**Interfaces:**
- Produces: `class FloatingBrowserService : Service` with `const val EXTRA_URL = "extra_url"`.

- [ ] Create programmatic overlay with a top drag bar and WebView body.
- [ ] Add refresh and close buttons.
- [ ] Enable JavaScript, DOM storage, zoom, and in-WebView navigation.
- [ ] Implement drag updates through `WindowManager.updateViewLayout()`.
- [ ] Destroy WebView and remove overlay in `onDestroy()`.
- [ ] Commit.

### Task 4: Correct manifest and build configuration

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/build.gradle`
- Create: `gradle.properties`

**Interfaces:**
- Registers `.FloatingBrowserService` as non-exported.

- [ ] Add INTERNET and SYSTEM_ALERT_WINDOW permissions.
- [ ] Use built-in Android theme to avoid missing resource errors.
- [ ] Allow cleartext traffic for local dashboard testing.
- [ ] Add Java/Kotlin 17 compilation settings.
- [ ] Commit.

### Task 5: Make GitHub Actions self-contained and publish APK

**Files:**
- Modify: `.github/workflows/android.yml`

**Interfaces:**
- Produces artifact named `flowser-debug-apk` containing `app-debug.apk`.

- [ ] Install Gradle 8.7 explicitly because the repository has no wrapper.
- [ ] Run `gradle testDebugUnitTest`.
- [ ] Run `gradle assembleDebug`.
- [ ] Upload `app/build/outputs/apk/debug/app-debug.apk` with `actions/upload-artifact@v4`.
- [ ] Trigger branch workflow and inspect job logs.
- [ ] Fix failures until workflow is green.
- [ ] Commit.

### Task 6: Merge verified prototype

**Files:**
- Modify: `README.md`

**Interfaces:**
- Documents install and test steps.

- [ ] Update README with artifact download and Android overlay permission instructions.
- [ ] Confirm workflow success and artifact presence.
- [ ] Open PR from `prototype/installable-v0.1` to `main`.
- [ ] Merge after verification.
- [ ] Confirm main branch workflow succeeds and artifact is available.
