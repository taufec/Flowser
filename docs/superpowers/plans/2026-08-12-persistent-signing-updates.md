# Persistent Signing Updates Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce update-safe Flowser release APKs signed by one permanent keystore so future installs preserve app/WebView data.

**Architecture:** `app/build.gradle` reads release signing values from environment variables. GitHub Actions validates four repository secrets, reconstructs the keystore in the runner temp directory, runs tests, builds `assembleRelease`, and uploads only the signed release APK. README documents one-time migration from prototype debug signing.

**Tech Stack:** Android Gradle Plugin, Gradle 8.7, Java 17, GitHub Actions.

## Global Constraints

- Application ID stays exactly `com.flowser.app`.
- Signing keys and passwords must never be committed.
- `versionCode` must increase for every installable release.
- GitHub Actions must fail if signing secrets are incomplete.
- No WebView/app-data clearing is introduced.

---

### Task 1: Release signing configuration

**Files:**
- Modify: `app/build.gradle`
- Create: `.gitignore`

- [ ] Add release signing config sourced from `FLOWSER_KEYSTORE_PATH`, `FLOWSER_KEYSTORE_PASSWORD`, `FLOWSER_KEY_ALIAS`, and `FLOWSER_KEY_PASSWORD`.
- [ ] Keep debug signing unchanged for development.
- [ ] Bump installable release to `versionCode 3`, `versionName '0.3.0'`.
- [ ] Ignore `*.jks` and `*.keystore`.
- [ ] Run unit tests and debug build.

### Task 2: Signed release CI

**Files:**
- Modify: `.github/workflows/android.yml`

- [ ] Validate all four signing secrets before release build.
- [ ] Decode `FLOWSER_KEYSTORE_BASE64` to `$RUNNER_TEMP/flowser-release.jks`.
- [ ] Export the signing environment variables for Gradle.
- [ ] Run `gradle --no-daemon testDebugUnitTest`.
- [ ] Run `gradle --no-daemon assembleRelease`.
- [ ] Upload `app/build/outputs/apk/release/app-release.apk` as `flowser-release-apk`.

### Task 3: Release documentation

**Files:**
- Modify: `README.md`
- Create: `docs/RELEASE_SIGNING.md`

- [ ] Replace debug-APK install instructions with signed-release instructions.
- [ ] Document generation and secure storage of the permanent keystore.
- [ ] Document the four GitHub secrets and base64 conversion commands.
- [ ] State clearly that the first permanent release may require one final uninstall if the installed prototype signature differs.
- [ ] Document that losing the permanent keystore breaks the update chain.

### Task 4: Verification

- [ ] Confirm repository search finds no committed `.jks` or `.keystore` material.
- [ ] Confirm `applicationId` remains `com.flowser.app`.
- [ ] Confirm release version is higher than v0.2.
- [ ] Review the branch diff before opening a PR.
