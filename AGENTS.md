# AGENTS.md

Mod **spd-duelist**: Shattered Pixel Dungeon locked to Duelist (Kohaku hero), 4-direction sprite sheet, custom drink/hurt animations.

## Build (ARM64 proot-distro)

```bash
cd /mnt/sdcard/Download/spd-duelist
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64 ANDROID_HOME=/opt/android-sdk ANDROID_SDK_ROOT=/opt/android-sdk
sh ./gradlew :android:assembleDebug --no-daemon
# output: android/build/outputs/apk/debug/android-debug.apk
```

- **Always `sh ./gradlew`** (sdcard mount blocks direct exec).
- **JDK 17 required** (AGP 9.1). `local.properties` already set.
- Copy APK after build: `cp android/build/outputs/apk/debug/android-debug.apk /mnt/sdcard/Download/spd-kohaku.apk`

### aapt2 ARM64 patch (one-time)

Google ships x86-64 aapt2 only. Patch with ARM64 binary from Termux — see [existing script in AGENTS.md](#bắt-buộc-patch-aapt2-cho-arm64). **Never delete `~/.gradle/caches`** after patching.

## Architecture

### Sprite system (Kohaku)

- **Sheet**: `sprites/kohaku.png` (2016x384 = 21 cols x 4 rows of 96x96, row = facing x 21)
- **Logical size override**: `HeroSprite.frame()` forces `width=height=16`, `scale=1` — GPU downsamples 96→16 via LINEAR filter. All position/center/emitter math uses 16x16.
- **Film caching**: `kohakuFilm()`, `hurtFilm()`, `pullFilm()` are static — created once, reused. Never call `new TextureFilm(...)` for kohaku in hot paths.
- **Facing**: `Hero.facing` (0=DOWN,1=LEFT,2=RIGHT,3=UP). `HeroSprite.updateFacing()` mutates animations in-place when facing changes.

### Drink phases (5 phases, `HeroSprite.update()`)

| Phase | Time | Sprite |
|-------|------|--------|
| 0 Pull | 0-10% | `kohaku_pull_down.png` (single frame, face down) |
| 1 Held | 10-22% | `held_{color}.png` / `held_exotic_{color}.png` |
| 2 HeldUp | 22-38% | `heldup_{color}.png` / `heldup_exotic_{color}.png` |
| 3 Drink | 38-85% | `drinking.png` (squash/stretch from center) |
| 4 Good/Bad | 85-100% | `good.png` (beneficial) or hurt face-down (harmful) |

- `Potion.drink()` defers `apply(hero)` via callback until phase 3→4 transition.
- Harmful = `mustThrowPots.contains(getClass())`.
- `operate.delay` is stretched to `drinkDuration / 6` during drink, reset after.

### Hurt system

- **Sheet**: `sprites/kohaku_hurt.png` (288x384 = 3 cols x 4 rows, row = facing)
- **Trigger**: `bloodBurstA(from, damage)` — turns to face attacker, plays 3-frame hurt animation.
- **Cached**: `hurtFilm()` static. `onComplete(hurt)` restores texture + `updateFacing()` (no `updateArmor()` call — avoids attack stretching bug).

### Attack animation

- 12 frames: `row+9` to `row+20` at 30fps.
- `operate` = second half of attack (`row+15..20`, 6 frames) — used for open/search/interact.

### Diagonal movement

- During tween: face LEFT/RIGHT.
- On stop: `restFacing` = LEFT/RIGHT (matching horizontal direction). **No UP/DOWN rest facing.**

### TurnTo

`HeroSprite.turnTo(from, to)` computes facing from world coordinates (prefers horizontal). Covers attack/zap/operate/throw/spell — hero always faces target.

## Key files

| File | What it does |
|------|-------------|
| `HeroSprite.java` | All Kohaku sprite logic: 16x16 override, drink timer, hurt, facing, animations |
| `Hero.java` | Diagonal detection, `restFacing`, `facing` field |
| `Potion.java` | `drink()` defers apply via callback, passes beneficial flag |
| `SPDSettings.java` | `charSpeed()`, `drinkDuration()` settings |
| `WndCharSettings.java` | Speed/drink duration sliders |
| `DrinkTestScene.java` | Test screen for drink sprites (tap version text on title) |
| `AndroidLauncher.java` | `clearFlags(FLAG_SECURE)` for screenshot/recording |

## Gotchas

- **`operate` must always be a valid Animation** — null causes freeze (actor never released).
- **Don't call `updateArmor()` in `onComplete(hurt)`** — causes attack stretching. Use `updateFacing()` instead.
- **`frame()` override** sets width=16 for ALL frames (kohaku + drink + hurt). Any code that reads `sprite.width()` gets 16, not 96.
- **Drink textures** are single 96x96 PNGs (not sheets). Only `kohaku.png`, `kohaku_hurt.png`, `kohaku_pull.png` are sheets.
- **Exotic potions** use `held_exotic_*` / `heldup_exotic_*` variants. `drinkColor` is base color (e.g. "amber"), exotic flag passed separately.

## Testing

- Drink test: tap version text on title screen → `DrinkTestScene` cycles through all drink sprites.
- Diagonal movement: move diagonally, stop → should face LEFT/RIGHT (not UP/DOWN).
- Hurt: get hit → hero turns to attacker + 3-frame hurt flash.
- Rapid attack after hurt: attack immediately after hurt finishes → should NOT stretch.
