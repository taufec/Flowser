# Flowser permanent release signing

Flowser must use the same release signing key for every installable version. Android treats that signing certificate as part of the app's identity. Losing or changing the key breaks the normal in-place update chain.

## 1. Generate the keystore once

Run this on a trusted local machine and keep the resulting file somewhere backed up securely:

```bash
keytool -genkeypair \
  -v \
  -keystore flowser-release.jks \
  -alias flowser \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Record the keystore password, key alias and key password. Do not commit the `.jks` file or passwords to Git.

## 2. Back it up

Keep at least two secure copies of `flowser-release.jks` outside the repository. Anyone with this key and its passwords can sign APKs that Android accepts as Flowser updates.

## 3. Convert the keystore to one-line base64

macOS:

```bash
base64 < flowser-release.jks | tr -d '\n'
```

Linux:

```bash
base64 -w 0 flowser-release.jks
```

## 4. Add GitHub Actions repository secrets

Create these four Actions secrets in the Flowser repository:

- `FLOWSER_KEYSTORE_BASE64` = the one-line base64 output
- `FLOWSER_KEYSTORE_PASSWORD` = keystore password
- `FLOWSER_KEY_ALIAS` = `flowser` unless a different alias was chosen
- `FLOWSER_KEY_PASSWORD` = key password

The workflow reconstructs the keystore only inside the temporary GitHub Actions runner directory. The file is not committed to the repository or uploaded as an artifact.

## 5. Release behavior

- Pull requests run unit tests and a debug build only.
- Pushes to `main` run tests and then build the signed release APK.
- Manual `workflow_dispatch` can also build a signed release from the selected branch.
- The distributable artifact is `flowser-release-apk`, containing `app-release.apk`.
- Release builds fail if any signing secret is missing.

## 6. Versioning rule

Every installable release must have a `versionCode` higher than every previous installed release. `versionName` is human-readable; `versionCode` is what Android uses for upgrade ordering.

Example:

```text
0.2.0 -> versionCode 2
0.3.0 -> versionCode 3
0.3.1 -> versionCode 4
0.4.0 -> versionCode 5
```

Do not reuse or decrease a `versionCode` for an APK intended to update an installed copy.

## 7. One-time migration from prototype builds

The old prototype APKs were built with debug signing. GitHub-hosted runners can use different ephemeral debug keystores, so the currently installed prototype may not share a signing certificate with the new permanent release.

If Android refuses the first permanently signed APK as an update:

1. Uninstall the old prototype once.
2. Install the first permanently signed Flowser release.
3. Sign in to websites again once.
4. Keep using the same permanent signing key for all later versions.

After that migration, normal in-place updates preserve Flowser app data and WebView storage unless the app or website explicitly clears/invalidates them.

## 8. Local signed release build

With the four values available locally:

```bash
export FLOWSER_KEYSTORE_PATH="$PWD/flowser-release.jks"
export FLOWSER_KEYSTORE_PASSWORD='your-keystore-password'
export FLOWSER_KEY_ALIAS='flowser'
export FLOWSER_KEY_PASSWORD='your-key-password'
gradle --no-daemon assembleRelease
```

The release task intentionally fails when these signing variables are missing, preventing accidental unsigned/disposable release APKs.
