# 6. Animation bị đánh (Hurt)

Thay vì blood splash, Kohaku chơi hurt animation 3 frames.

## Trigger

`bloodBurstA(PointF from, int damage)` — gọi khi hero nhận damage.

## Flow

```
bloodBurstA(from, damage)
  ├── Turn to face attacker (compute facing from delta)
  ├── Save hurtOriginalSheet (dizzy/monini/normal — sheet ĐANG dùng)
  ├── texture(HURT_PATH)  // switch to hurt sheet
  ├── frame(hurtFilm().get(facing * 3))  // force first hurt frame
  └── play(hurt)  // 3-frame animation

onComplete(hurt)
  ├── hurtTimer = -1
  ├── texture(hurtOriginalSheet)  // restore đúng sheet
  ├── lastFacing = -1  // force re-evaluate
  ├── updateFacing()  // re-check debuff state, apply correct texture
  └── play(idle, true)
```

## Sprite sheet

`kohaku_hurt.png` — 288×384 = 3 cols × 4 rows × 96×96

| Row | Facing | Frames |
|-----|--------|--------|
| 0 | DOWN | 0, 1, 2 |
| 1 | LEFT | 3, 4, 5 |
| 2 | RIGHT | 6, 7, 8 |
| 3 | UP | 9, 10, 11 |

## hurtOriginalSheet save logic

Trước: Luôn save `Dungeon.hero.heroClass.spritesheet()` (kohaku.png) → sai khi đang dizzy.

Sau: Save sheet ĐANG dùng:
```java
if (isDebuffed()) hurtOriginalSheet = DIZZY_PATH;
else if (transformState == MONINI) hurtOriginalSheet = "sprites/kohaku_monini/walk.png";
else hurtOriginalSheet = Dungeon.hero.heroClass.spritesheet();
```

## Điều kiện không play

- `drinkTimer >= 0`: đang uống thuốc → không play hurt
- `curAnim == die`: đang chết → không play hurt
