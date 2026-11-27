# Cryogenic Launcher — source code and building

Minecraft: Java Edition launcher for Android. Main author: RussoViller.

## Authorship, license and origin

**Cryogenic Launcher was created by RussoViller**, with its own name, visual identity, screens and features. Official repository: https://github.com/RussoViller/Cryogenic-Launcher

The launcher uses the code of [Zalith Launcher 2](https://github.com/ZalithLauncher/ZalithLauncher2) (Copyright © 2025-2026 MovTery and contributors, [GNU GPL v3](LICENSE)) as its base, plus code and ideas from other projects listed in the credits. Because of that, following the Zalith Launcher 2 terms, it is identified as an unofficial modified version of it. It is not affiliated with or endorsed by the Zalith Launcher 2 developers.

Terms that apply to every copy (GPL v3, section 7; details in `NOTICE` and in the RVCLapk1.1 license, at `CryogenicLauncher/src/main/assets/legal/RVCL.md`):

1. Every copy, modified or not, must keep visible in the app: **"Cryogenic Launcher — created by RussoViller"**, the official repository link and the creator credits.
2. No one may claim to be the creator or owner of Cryogenic Launcher or present a copy as the official one. Modified versions must state: **"Based on Cryogenic Launcher by RussoViller."**
3. Republishing under another name while removing these credits is prohibited.
4. The Zalith Launcher 2 terms still apply: a name different from the original, a notice that it is an unofficial modified version, and keeping the copyright notices.

## Building

Requirements:
- JDK 21
- Android SDK (platform 37.2, build-tools 37) and NDK 25.2.9519653
- Rust with `cargo-ndk` and the `aarch64-linux-android` and `armv7-linux-androideabi` targets (PetLib module)

```bash
# arm64-v8a APK
./gradlew :CryogenicLauncher:assembleRelease -Darch=arm64
# armeabi-v7a APK
./gradlew :CryogenicLauncher:assembleRelease -Darch=arm
# "IS" version (game files in internal storage)
./gradlew :CryogenicLauncher:assembleRelease -Darch=arm64 -Dstorage=is
```

- Without the official keystore (`CryogenicLauncher/cryogenic_launcher.jks`, not included in the repository), the APK is not signed with the official signature: sign it with your own key.
- Microsoft account login: set the `OAUTH_CLIENT_ID` environment variable (or a `.oauth_client_id.txt` file in the root).

## Automatic debug APKs

Every update to the source code on `main` builds the debug APKs (arm64-v8a, armeabi-v7a and the IS versions) with GitHub Actions: open **Actions › Debug APKs › (latest run) › Artifacts**. Debug APKs install alongside the official version (package `.debug` / `.is.debug`) and are signed with a public CI debug key (`.github/ci/debug.keystore`), not the official key.

## Launcher Mods (.cmod)

To make mods for the launcher itself, see [`CryoMod-Tutorial.txt`](CryoMod-Tutorial.txt), the [`cmod-sdk`](cmod-sdk) folder and the ready-made example [`CryoMod-Example.cmod`](CryoMod-Example.cmod).
