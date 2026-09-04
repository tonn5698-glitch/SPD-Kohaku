# SPD Kohaku

Mod of [Shattered Pixel Dungeon](https://shatteredpixel.com/shatteredpd/) — locked to **Duelist (Kohaku)** hero with 4-direction RPG Maker sprite system.

## Features

- **4-direction sprite sheet** (96×96 frames, rendered at 16×16 via GPU scaling)
- **5-phase drink animation**: pull from bag → held → held up → drinking → good/bad
- **Waterskin drink** with same animation, always good
- **Hurt flash** when taking damage, facing attacker
- **Dizzy walk** when debuffed (Poison, Burning, Ooze, Vertigo, Slow, Doom, Chill, Frost, Corrosion)
- **Monini transform** for speed effects (Haste, GreaterHaste, Speed): kohaku → transform → monini form
- **Hero faces target** on attack/zap/operate/throw/spell
- **Diagonal movement** keeps LEFT/RIGHT idle (no UP/DOWN rest facing)
- **Speed setting** (1–10) and **drink duration setting** (0.5–5.0s)
- **Screenshot/screen recording** enabled (FLAG_SECURE cleared)

## Build (ARM64 proot-distro)

```bash
cd /path/to/SPD-Kohaku
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64 ANDROID_HOME=/opt/android-sdk ANDROID_SDK_ROOT=/opt/android-sdk
sh ./gradlew :android:assembleDebug --no-daemon
# output: android/build/outputs/apk/debug/android-debug.apk
```

- **Always `sh ./gradlew`** (sdcard mount blocks direct exec)
- **JDK 17 required** (AGP 9.1)
- See [AGENTS.md](AGENTS.md) for full build instructions including aapt2 ARM64 patch

## Sprite Structure

| Sheet | Size | Frames | Usage |
|-------|------|--------|-------|
| `kohaku.png` | 2016×384 | 21×4 (96×96) | Walk (1–8), Attack (9–20), Idle (0) |
| `kohaku_dizzy.png` | 768×384 | 8×4 | Debuff walk |
| `kohaku_hurt.png` | 288×384 | 3×4 | Take damage |
| `kohaku_pull_down.png` | 96×96 | 1 | Pull potion from bag |
| `kohaku_monini/walk.png` | 768×384 | 8×4 | Speed form walk |
| `kohaku_monini/hurt.png` | 288×384 | 3×4 | Speed form hurt |
| `kohaku_transform/` | — | — | Transform animation sheets |
| `kohaku_drink/` | 96×96 each | 1/frame | Held, heldup, drinking, good, waterskin |

Facing: 0=DOWN, 1=LEFT, 2=RIGHT, 3=UP. Row = facing × colsPerRow.

## Key Files

| File | Description |
|------|-------------|
| `HeroSprite.java` | All Kohaku sprite logic: 16×16 override, drink timer, hurt, dizzy, monini, facing |
| `Hero.java` | Diagonal detection, `restFacing`, `facing` field |
| `Potion.java` | `drink()` defers apply via callback, passes beneficial flag |
| `Waterskin.java` | Waterskin drink animation trigger |
| `SPDSettings.java` | `charSpeed()`, `drinkDuration()` |
| `WndCharSettings.java` | Speed/drink duration sliders |
| `DrinkTestScene.java` | Test screen (tap version text on title) |
| `AndroidLauncher.java` | `clearFlags(FLAG_SECURE)` |

## Credits

Based on [Shattered Pixel Dungeon](https://github.com/00-Evan/shattered-pixel-dungeon) by Evan Debenham. Kohaku hero sprites from RPG Maker assets.
