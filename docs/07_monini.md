# 7. Monini Transform

Khi hero có Speed/Haste effect → transform sang Monini form.

## State machine

```
NORMAL → TRANSFORMING → MONINI → DE_TRANSFORMING → NORMAL
```

### Transform phases (5 phases)

| Phase | Thời gian | Asset | Mô tả |
|-------|-----------|-------|-------|
| A | 0-25% | kohaku2 dizzy idle | Dizzy过渡 |
| B | 25-37.5% | kohaku3 row 3 | Chuyển tiếp |
| C | 37.5-50% | kohaku3 row 2 | Chuyển tiếp |
| D | 50-75% | kougeki_henge row 1 | Henge frame |
| E | 75-100% | kougeki_henge rows 2-3 | Hoàn thành |

### De-transform

Reverse sequence từ MONINI về NORMAL.

### MONINI state

- Sprite dùng `kohaku_monini/walk.png` (8 cols × 4 rows × 96×96)
- Walk cycle: 8 frames, 12fps
- Idle: frame 1 (same as dizzy)
- Attack: dùng `kohakuFilm()` (normal kohaku attack frames)

## Điều kiện transform

- Bắt đầu: `isSpeedEffect()` = true (Haste, GreaterHaste, hoặc Speed buff)
- Kết thúc: `isSpeedEffect()` = false HOẶC `turnsSinceAttack > 0` (sau khi tấn công)

## Duration

`transformDuration = SPDSettings.transformDuration()` (default 2.0s, range 0.5-5.0s)

## Assets

```
sprites/kohaku2.png        // dizzy过渡
sprites/kohaku3.png        // transition frames
sprites/kougeki_henge.png  // henge frames
sprites/kohaku_monini/walk.png  // monini walk (8×4)
```
