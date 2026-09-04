# AGENTS.md

Mod **spd-duelist**: Shattered Pixel Dungeon locked to Duelist (Kohaku hero), 4-direction sprite system.

## Build

```bash
cd /mnt/sdcard/Download/spd-duelist
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64 ANDROID_HOME=/opt/android-sdk ANDROID_SDK_ROOT=/opt/android-sdk
sh ./gradlew :android:assembleDebug --no-daemon
# output: android/build/outputs/apk/debug/android-debug.apk
```

- **Always `sh ./gradlew`** (sdcard mount blocks direct exec)
- **JDK 17 required** (AGP 9.1)
- Copy APK: `cp android/build/outputs/apk/debug/android-debug.apk /mnt/sdcard/Download/spd-kohaku.apk`

## Versioning

Format: `SPD_VERSION-indevNNN` where NNN = build number (3 digits, zero-padded).

- Base: `3.3.8-indev000`
- After each build: increment NNN by 1
- Example: `3.3.8-indev000` → `3.3.8-indev001` → `3.3.8-indev002`

Bump version in `gradle.properties` (`versionCode`) and display string before each build.

## Key files

| File | Description |
|------|-------------|
| `HeroSprite.java` | All Kohaku sprite logic |
| `Hero.java` | Diagonal, facing, restFacing |
| `Potion.java` | Drink callback |
| `Waterskin.java` | Waterskin drink |
| `SPDSettings.java` | charSpeed, drinkDuration |
| `AndroidLauncher.java` | clearFlags(FLAG_SECURE) |
