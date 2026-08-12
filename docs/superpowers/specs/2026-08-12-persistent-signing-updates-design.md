# Persistent Signing and In-Place Updates Design

## Goal

Make future Flowser APK releases install as Android in-place updates so app data, WebView cookies, DOM storage, preferences, and browser sessions are preserved whenever the website/session itself remains valid.

## Design

- Keep the permanent application ID `com.flowser.app`.
- Stop distributing GitHub Actions debug APKs as user-installable releases.
- Use one long-lived release keystore for every future release.
- Store the keystore and passwords only in GitHub Actions secrets; never commit signing material.
- Build `assembleRelease` in CI and fail before building if any signing secret is missing.
- Increment `versionCode` for every installable release.
- Do not add any application code that clears WebView cookies, cache, DOM storage, or app data during upgrade.
- Keep a debug build for development/testing only.

## Required GitHub Secrets

- `FLOWSER_KEYSTORE_BASE64`
- `FLOWSER_KEYSTORE_PASSWORD`
- `FLOWSER_KEY_ALIAS`
- `FLOWSER_KEY_PASSWORD`

## Migration

The currently installed prototype may have been signed by a different ephemeral debug key. If so, the first permanently signed release cannot overwrite it. The user must uninstall once, install the first permanent release, sign in once, and all subsequent releases signed by the same keystore can update in place.

## Verification

- Unit tests pass.
- Debug APK still builds for development.
- Release configuration reads signing credentials only from environment variables.
- CI refuses to produce a release artifact when signing secrets are incomplete.
- CI release artifact is `app-release.apk`.
- `applicationId` remains `com.flowser.app`.
- New release uses a higher `versionCode` than 0.2.
