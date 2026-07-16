# The Liquidity Book — Android build

Capacitor 8 project wrapping `www/index.html` (the journal app).
Already configured: portrait lock, dark status/nav bars, dark splash, app icons,
AdMob plugin (@capacitor-community/admob) with Google TEST ad IDs.

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

1. AdMob (apps.admob.com): create an app + a banner and an interstitial ad unit.
   - Put your real **App ID** in `android/app/src/main/AndroidManifest.xml`
     (replace the TEST id at the marked TODO).
   - Put your real **ad unit IDs** in `www/index.html` (ADS config, marked TODO)
     and set `testing: false`. Run `npx cap sync android` after.
2. Play Console (play.google.com/console, $25 one-time): create the app,
   fill in the data-safety + ads declarations (app contains ads: YES).
3. In Android Studio: Build > Generate Signed App Bundle. Create a keystore and
   BACK IT UP — losing it means you can never update the app.
4. Upload the .aab, add store listing (icon in `store-assets/`), screenshots, publish.

## Notes

- App ID: com.arigoni.liquiditybook (permanent once published)
- Trades stay in the WebView's localStorage on-device. Uninstalling the app erases
  them — the in-app CSV backup/export is the user's safety net.
