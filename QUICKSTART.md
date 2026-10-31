# Allai

Native Android AI agent (Opencode-inspired) with terminal, MCP/plugins.

## Setup
1. Open `K:\T3\Allai` in Android Studio.
2. Sync Gradle, then run `./gradlew assembleDebug`.
3. Install on a device/emulator: `./gradlew installDebug`.

## Publish to GitHub
```bash
git init
git add .
git commit -m "Initial commit: Allai Android app"
git remote add origin https://github.com/<YOUR_USERNAME>/allai.git
git branch -M main
git push -u origin main
```

Create the repository at https://github.com/new first (do not initialize with README), then push.

## Publish to Google Play
1. Create a release signing keystore: `keytool -genkeypair -v -keystore allai-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias allai`.
2. Add a `signingConfig` to `app/build.gradle` release buildType.
3. Build the app bundle: `./gradlew bundleRelease`.
4. Upload `app/build/outputs/bundle/release/app-release.aab` to the Google Play Console.
5. Complete store listing, pricing, and review; then publish.
