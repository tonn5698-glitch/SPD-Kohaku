# SPD-Kohaku Mod

Mod Shattered Pixel Dungeon — hero Kohaku (Duelist) với sprite RPG Maker 96×96, animation 4 hướng, dialog RPG Maker.

## Tính năng chính

| # | Tính năng | File chính |
|---|-----------|------------|
| 1 | [Cơ chế 4 hướng](01_4direction.md) | `HeroSprite.java`, `Hero.java` |
| 2 | [Animation uống thuốc](02_drink.md) | `HeroSprite.java`, `Potion.java` |
| 3 | [Animation uống nước](03_waterskin.md) | `Waterskin.java` |
| 4 | [Animation ăn đồ ăn](04_food.md) | `HeroSprite.java` |
| 5 | [Hệ thống Dizzy](05_dizzy.md) | `HeroSprite.java` |
| 6 | [Animation bị đánh (Hurt)](06_hurt.md) | `HeroSprite.java` |
| 7 | [Monini Transform](07_monini.md) | `HeroSprite.java` |
| 8 | [Diagonal Movement](08_diagonal.md) | `HeroSprite.java`, `Hero.java` |
| 9 | [Portrait / Avatar](09_portrait.md) | `HeroSprite.java`, `StatusPane.java` |
| 10 | [Mirror Image](10_mirror.md) | `MirrorSprite.java` |
| 11 | [RPG Maker Dialog](11_dialog.md) | `WndRPGDialog.java` |
| 12 | [Cài đặt nhân vật](12_settings.md) | `SPDSettings.java`, `WndCharSettings.java` |
| 13 | [Death Animation](13_death.md) | `HeroSprite.java` |
| 14 | [Pull Animation](14_pull.md) | `HeroSprite.java` |
| 15 | [96×96 → 16×16 Rendering](15_rendering.md) | `HeroSprite.java` |
| 16 | [Texture Caching](16_caching.md) | `HeroSprite.java` |
| 17 | [Debug Dialog Scene](17_debug.md) | `DebugDialogScene.java` |

## Sprite sheets

```
sprites/
├── kohaku.png              2016×384  (21 cols × 4 rows × 96×96)
├── kohaku_hurt.png         288×384   (3 cols × 4 rows × 96×96)
├── kohaku_dizzy.png        768×384   (8 cols × 4 rows × 96×96)
├── kohaku_dizzy_attack.png 384×384   (4 cols × 4 rows × 96×96)
├── kohaku_portrait.png     96×96     idle face-down
├── kohaku_dizzy_portrait.png 96×96   dizzy idle face-down
├── kohaku_pull_down.png    96×96     pull from pouch
├── kohaku_drink/           held, heldup, drinking, good
├── kohaku_food/            held food sprites
├── kohaku_food_down/       held-down food sprites
├── kohaku_die/             DMZ7, DMZ8, DMZ9 (death sheets)
└── kohaku_dizzy_attack/    per-frame dizzy attack (down/left/right/up × 4)
```

## Build

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
export ANDROID_HOME=/opt/android-sdk
sh ./gradlew :android:assembleDebug --no-daemon
```
