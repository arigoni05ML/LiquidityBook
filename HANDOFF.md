# HANDOFF — The Liquidity Book (Android)

Context document for an AI model (or developer) picking up this project. Accurate as of 2026-07-11.

## What this is

**The Liquidity Book** — a futures trading journal aimed at prop-firm traders (Apex/Topstep-style evals). Single-page web app wrapped for Android with **Capacitor 8**. App ID `com.arigoni.liquiditybook`, portrait-locked, dark theme only.

## Architecture — read this first

- **The entire app lives in `www/index.html`** (~2,200 lines): all CSS, HTML, and JS inline in one file. No framework, no bundler, no build step for the web layer. This is intentional — keep it that way unless the owner asks otherwise.
- `www/js/` contains only vendored Capacitor glue (`capacitor-core.js`, `capacitor-app.js`, `biometric-auth.js`). Do not edit.
- `android/` is the generated Capacitor project. Only hand-edited file: `android/app/src/main/AndroidManifest.xml`.
- Workflow after editing `www/index.html`: `npx cap sync android` → build/run in Android Studio. Manifest changes need a full rebuild.
- The app also runs in a plain browser (no ads, downloads instead of native file writes). Native features are gated via `P(name)` which returns the Capacitor plugin or `null` in browser. Use `?adpreview` URL param to preview banner layout shift in browser.
- **JS conventions:** vanilla ES2020+, template-literal HTML rendering with `innerHTML`, single-letter helpers (`$`=getElementById, `ic`=icon svg, `esc`=HTML-escape, `fmt$`/`fmt$0`=money). Functions are declarations (hoisted). State persists via `persist()` → localStorage → mirrored to native `Preferences` (`natMirror`/`natRestore`) so data survives WebView data loss.

## Data model (localStorage keys)

| Key | Contents |
|---|---|
| `ftj_trades` | Array of trade objects (see below) |
| `ftj_settings` | `{qty, commPerSide, lastBackup, lastAccts[], accounts[], instruments[], setups[]}` |
| `ftj_app` | `{notif, notifTime, bio}` (native prefs UI) |
| `ftj_adNotice` | "1" once the first-launch ad notice was dismissed |
| `ftj_adstate` | `{freeUntil, lastRedeemed}` — rewarded ad-free expiry timestamp + date of last redemption (older fields may linger; ignored) |

**Trade:** `{id, date, time, acct, sym, dir:'L'|'S', qty, slPts, tpPts, outcome:'tp'|'sl'|'manual', resultPts, setup, grade:'A'|'B'|'C', plan:bool, emotion, note, imgs:[fileId], gross, comm, net, r}`. P&L is **points-based**: `net = resultPts * $/point * qty − commission`; `$/point = tickValue/tickSize` from the instrument. `migrateTrade()` upgrades v1 (price-based) records.

**Account:** `{id, name, size, target, maxDD, ddType:'intraday'|'eod'|'static', cap:bool, dailyLoss, minDays, consistPct, adj:[{id,date,amt,note}], phase:'Evaluation'|'Funded'}`. `migrateAccounts()` backfills `adj`/`consistPct` (run on load and after JSON restore).

**Multi-account logging:** one wizard save can create N trade copies (one per selected account, same `imgs` list). Trade `id` = `Date.now()+i`.

## Prop account engine (`acctStatus(a)`) — business rules

Merges trades + payouts chronologically (payouts sort after that day's trades):
- Trailing drawdown: `hwmTrade` (trade-to-trade) or `hwmEOD` (end-of-day closes); static = `size − maxDD` (fixed).
- **Payouts shift balance AND trailing HWM down equally** → drawdown buffer is preserved at payout time. Static level does NOT shift. `cap` (trailing stops at start balance) becomes `size − paidOut` in shifted space.
- `cycle = bal − size` (progress toward target since start, net of payouts; drives target bar + `passed`). `profit = cycle + paidOut` (lifetime trade P&L; shown in header).
- **Consistency rule:** `consistPct` (0=off). Best single day's net must be ≤ X% of total lifetime profit. Returns `consistPct/consistOk/consistNeed/bestDay/bestDayDate`; breach shows a gold CONSISTENCY badge + bar with "profit $X more to fix".

## Feature map (section banner comments in index.html)

Tabs: Journal (today + log wizard), Dashboard (stats/equity/insights per account), Calendar (daily P&L, tap day → shareable P&L card canvas), History (filters + CSV export/import), Settings.

- **Log wizard** (`openWizard`, 5 steps: Accounts→Market→Trade→Context→Note). Step 5 includes screenshots (max 6).
- **Screenshots:** downscaled to ≤1400px JPEG dataURLs; stored via Filesystem plugin in `DATA/ftj_img/` (native) or IndexedDB db `ftj` store `imgs` (browser). Refcounted across multi-account copies (`imgRefCount`) — files deleted only when no trade references them. **Not included in JSON backups.** Viewer: `viewShots(tradeId)` → `#shotModal`.
- **CSV import** (`buildImport`): fuzzy header matching (own export round-trips losslessly; generic broker exports need date+symbol+net or points). Handles `$`, commas, parens-negatives, `MM/DD/YYYY`+AM/PM, futures month codes (`MESU5`→`MES` via `matchSym`). Unknown symbols are skipped and reported (user must add instrument first). Duplicate detection: same date+time+sym+qty+net(±0.01). Preview modal `#impModal` with account override. New setups are auto-registered.
- **Backups:** `exportJSONToDevice()` → `Documents/The Liquidity Book/` via Filesystem (permission request on Android ≤10; manifest has READ/WRITE_EXTERNAL_STORAGE with maxSdkVersion). Fallback → share sheet. `exportJSON()` = share sheet. Restore replaces everything.
- **Settings UI:** prop accounts, add-account form, and instruments are `<details class="fold">` accordions. Per-account fold open-state is tracked in `openFolds` Set (survives the frequent `renderSettings()` re-renders); static folds don't need tracking.
- **Native extras:** biometric lock (`ftj_app.bio`), daily log reminder (LocalNotifications id 1), haptics on toast, app shortcuts via deep links (`://log`, `://stats`).

## Ads (`@capacitor-community/admob`) — CURRENT STATE & RELEASE CHECKLIST

State: **LIVE for release** — real APPLICATION_ID in the manifest; real android units: banner "BottomBanner" `.../2247759500`, interstitial "PopUp" `.../7650305595`, rewarded `.../5924029066`; `ADS.testing:false`. iOS entries remain Google samples (iOS untested).

Behavior:
- Adaptive banner bottom-anchored (skipped while ad-free); height flows into CSS var `--ad-h`.
- **Interstitial:** one per app launch, `INTER_DELAY_MS` (120s; 15s while `ADS.testing`) after open. Preloaded at init (`prepareInter`); `tryLaunchInterstitial` retries every 30s (max 5) if the ad isn't loaded or show fails. Skipped while the rewarded ad-free day is active. Guard flag: `interShownThisLaunch`.
- **Rewarded:** Settings → App → "Watch an ad — hide banner ads for 12h" → banner removed + interstitials skipped for 12 hours (`adState.freeUntil`); redeemable once per calendar day (`adState.lastRedeemed`). Persists via Preferences mirror. Reward event: `onRewardedVideoAdReward` (verified against plugin source).
- Consent: UMP consent form + iOS ATT in `initAds`; `manageConsent()` re-opens it.
- **Gotcha (verified in plugin source):** with `isTesting:true`, emulators/registered test devices use the REAL ad id, not the test id — real units on a mismatched App ID never fill. That's why sample IDs are in place for now.

Before release:
1. Ad config is fully live (App ID + all three android units, testing:false) — nothing left in code.
2. Play Console: ads declaration YES; privacy policy URL required (host `store-assets/privacy-policy.html`); keystore must be backed up (losing it = can never update); bump `versionCode` in `android/app/build.gradle` per upload.
3. Note: fresh ad units can take a few hours to start filling, and emulators may not serve live ads — test on a real device.

## Testing / verification conventions

No test infrastructure. Verification pattern used so far: extract or reconstruct changed JS into a temp file, `node --check` for syntax, and run small logic harnesses with stubs (`acctStatus`, CSV parsing, ad gating all have passing harnesses in the session history). UI changes: manual on device/emulator.

**Environment quirk:** if working in a sandboxed shell against this folder, large-file mounts may truncate `www/index.html` (~105KB cap observed). Trust the direct file-tool reads, not the shell mount, and verify via reconstructed harnesses.

## Known issues / notes

- `trades` and `settings` are globals reassigned by restore/import — never cache references across those calls.
- `#adNotice` inner div reuses `.card` class; `#adNotice .card` sets `display:block` to defeat the stat-card flex row. Don't remove.
- Insights need ≥5 trades; equity curve ≥2.
- `screen.orientation.lock` fails silently outside fullscreen Android; native lock comes from Capacitor config.
- localStorage AND Preferences are cleared on uninstall → backups are the only true safety net. A future auto-backup to Documents was discussed but not built.
- iOS units in `ADS` are Google samples; iOS build has never been tested.
