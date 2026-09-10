# 8. Diagonal Movement

Hero có thể di chuyển theo 8 hướng (4 cardinal + 4 diagonal).

## Cơ chế

### busyMoving + idleGrace

Khi hero di chuyển, `busyMoving = true` và `idleGrace = 0.12f`. Trong suốt grace period, `idle()` bị short-circuit → walk animation tiếp tục chạy giữa các tile.

### Diagonal detection

```java
// Hero.java — handle() và move()
boolean isDiagonal = Math.abs(deltaX) != 1 || Math.abs(deltaY) != 1;
// Khi diagonal: facing = LEFT hoặc RIGHT (dựa trên horizontal direction)
// Khi cardinal: facing = UP/DOWN/LEFT/RIGHT bình thường
```

### restFacing

- Cardinal movement: `restFacing = -1` (không dùng)
- Diagonal movement: `restFacing = LEFT hoặc RIGHT` (facing khi dừng)

### Speed

`interval = 0.36 - charSpeed × 0.036` (range: 0.08s-0.32s, default charSpeed=5 → 0.18s)

## Flow

```
Hero.move(step)
  ├── Compute facing from delta
  ├── Set restFacing if diagonal
  ├── Sprite.move() → PosTweener
  │     └── busyMoving = true
  │     └── updateFacing() — face LEFT/RIGHT during tween
  └── On stop: idleGrace expires → apply restFacing → idle()
```

## Spritesheet impact

Diagonal movement luôn face LEFT hoặc RIGHT → sprite sheet chỉ cần 2 rows (LEFT/RIGHT) cho diagonal. UP/DOWN chỉ dùng khi đứng yên.
