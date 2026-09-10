# 5. Hệ thống Dizzy

Khi hero bị debuff, gần hết máu, hoặc đứng trong khí độc → sprite chuyển sang dizzy sheet.

## Điều kiện trigger

`isDebuffed()` returns true khi:

1. **Low HP**: `HP > 0 && HP < HT × 15%`
2. **ToxicGas**: `Blob.volumeAt(pos, ToxicGas.class) > 0`
3. **Buffs**: Poison, Burning, Ooze, Vertigo, Slow, Doom, Chill, Frost, Corrosion, Roots

## Sprite sheets

| Sheet | Kích thước | Cols × Rows | Dùng khi |
|-------|-----------|-------------|----------|
| `kohaku_dizzy.png` | 768×384 | 8×4 | walk + idle |
| `kohaku_dizzy_attack.png` | 384×384 | 4×4 | attack + operate |

## Dizzy idle frame

- **Normal idle**: `row + 0` (frame đầu — foot lifted)
- **Dizzy idle**: `row + 1` (frame 2 — standing still)

## Dizzy walk

8 frames: `row + 0` đến `row + 7` (toàn bộ 8 cols của dizzy sheet)

## Dizzy attack

4 frames per facing: `dizzyAttackRow + 0..3` (từ `kohaku_dizzy_attack.png`)

## Texture sync khi dizzy

```java
// updateFacing() sync texture
if (curAnim == attack || curAnim == operate) {
    targetTex = TextureCache.get("sprites/kohaku_dizzy_attack.png");
} else {
    targetTex = TextureCache.get(DIZZY_PATH); // kohaku_dizzy.png
}
```

## Portrait

- **Normal**: `kohaku_portrait.png` (idle face-down từ kohaku.png)
- **Dizzy**: `kohaku_dizzy_portrait.png` (idle face-down từ kohaku_dizzy.png)
- Trigger: low HP <15% HOẶC debuffed HOẶC toxic gas

## StatusPane refresh

```java
// StatusPane.update() — check mỗi frame
boolean isDizzy = HP < HT * 0.15f || toxicGas || debuffed;
if (isDizzy != lastAvatarDizzy) {
    lastAvatarDizzy = isDizzy;
    updateAvatar(); // re-fetch HeroSprite.avatar(hero)
}
```
