# The Liquidity Book — Android build

Capacitor 8 project wrapping `www/index.html` (the journal app).
Already configured: portrait lock, dark status/nav bars, dark splash, app icons,
AdMob plugin (@capacitor-community/admob) with live ad units (banner, interstitial, rewarded).

## Build & run (first time)

1. Install Node.js LTS (nodejs.org) and Android Studio (developer.android.com/studio).
2. In this folder:
       npm install
       npx cap sync android
3. Open the `android/` folder in Android Studio, let Gradle finish, then press Run
   with a device (USB debugging on) or emulator selected.

## Updating the app's HTML

Edit `www/index.html`, then run `npx cap sync android` and rebuild.

## Before releasing to Google Play

AdMob is already wired with live IDs — the App ID in `android/app/src/main/AndroidManifest.xml`,
the ad units in `www/index.html` under `ADS` (banner/interstitial/rewarded), and
`testing: false`. Remaining release steps:

1. Play Console (play.google.com/console, $25 one-time): create the app,
   fill in the data-safety + ads declarations (app contains ads: YES), and add the
   privacy policy URL (host `store-assets/privacy-policy.html`).
2. In Android Studio: Build > Generate Signed App Bundle. Create a keystore and
   BACK IT UP — losing it means you can never update the app. Bump `versionCode`
   in `android/app/build.gradle` for each upload.
3. Upload the .aab, add store listing (icon in `store-assets/`), screenshots, publish.

## Notes

- App ID: com.arigoni.liquiditybook (permanent once published)
- Journal data stays on-device: Android cloud backup and device transfer are disabled
  (`allowBackup="false"` + `data_extraction_rules.xml`). Uninstalling the app erases
  trades — the in-app backup (JSON to Files, share sheet, and auto-backup) is the
  user's safety net.
