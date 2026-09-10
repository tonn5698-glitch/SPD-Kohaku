# 1. Cơ chế 4 hướng

Kohaku sử dụng sprite sheet RPG Maker 96×96 với 4 hướng: DOWN(0), LEFT(1), RIGHT(2), UP(3).

## Sprite sheet layout

`kohaku.png` — 2016×384 = 21 cols × 4 rows × 96×96

| Row | Facing | Nội dung |
|-----|--------|----------|
| 0 | DOWN | idle(0) + walk(1-8) + attack(9-20) |
| 1 | LEFT |同上 |
| 2 | RIGHT |同上 |
| 3 | UP |同上 |

## Constants

```java
KOHAKU_FRAME_WIDTH  = 96
KOHAKU_FRAME_HEIGHT = 96
LOGICAL_SIZE        = 16  // rendered size on screen
```

## Facing state

- `Hero.facing` (int): 0=DOWN, 1=LEFT, 2=RIGHT, 3=UP
- `Hero.restFacing` (int): facing áp dụng khi diagonal movement dừng (-1 = không dùng)
- `HeroSprite.lastFacing`: theo dõi facing trước đó để detect thay đổi

## Các vị trí dùng facing

| Vị trí | File | Dòng |
|--------|------|------|
| Attack input | `Hero.java` | ~1917 |
| Move input (cardinal + diagonal) | `Hero.java` | ~1980-2007 |
| `move()` override | `Hero.java` | ~2353-2388 |
| `turnTo()` override | `HeroSprite.java` | ~441-469 |
| `updateFacing()` | `HeroSprite.java` | ~1057-1140 |

## updateFacing() flow

1. Kiểm tra `newFacing == lastFacing && currentDebuffed == lastDebuffed` → skip nếu giống
2. Chọn film: `kohakuFilm()`, `dizzyFilm()`, hoặc `moniniFilm()`
3. Tính `row = facing × colsPerRow` (21 hoặc 8 cols)
4. Rebuild Animation objects: `idle`, `run`, `attack`, `zap`, `operate`, `fly`, `read`, `hurt`
5. Sync texture với film hiện tại
