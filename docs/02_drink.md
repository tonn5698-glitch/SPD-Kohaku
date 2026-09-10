# 2. Animation uống thuốc

Khi hero uống potion, sprite chơi 5-phase animation thay vì chỉ play operate.

## Flow

```
Potion.drink(hero)
  ├── hero.spend(TIME_TO_DRINK)
  ├── hero.busy()
  ├── hero.sprite.operate(hero.pos)  // stretches operate animation
  └── HeroSprite.startDrink(color, exotic, beneficial, callback)
        └── drinkTimer = 0 → update() handles phases
```

## 5 Phases

| Phase | Thời gian | Texture | Mô tả |
|-------|-----------|---------|-------|
| 0 Pull | 0-10% | `kohaku_pull_down.png` | Lấy thuốc từ túi, forced facing DOWN |
| 1 Held | 10-22% | `held_[exotic_]color.png` | Cầm thuốc |
| 2 HeldUp | 22-38% | `heldup_[exotic_]color.png` | Đưa thuốc ra giữa |
| 3 Drink | 38-85% | `drinking.png` | Uống + squash/stretch animation |
| 4 Good/Bad | 85-100% | `good.png` hoặc `HURT_PATH` | Fires `drinkEffectCallback` |

## Drink phase 3: Squash & Stretch

```java
float cp = ((drinkTimer - heldUpEnd) / (drinkEnd - heldUpEnd)) * 3f;
float intensity = 1f / (1f + cyc);
float sx = 1f - intensity * 0.15f * sin(ct * PI * 2);
float sy = 1f + intensity * 0.15f * sin(ct * PI * 2 + PI);
sprite.scale.set(sx * SCALE, sy * SCALE);
```

## StartDrink parameters

```java
startDrink(String color, boolean exotic, boolean beneficial, Callback effect)
```

- `color`: tên màu (e.g. "amber", "crimson")
- `exotic`: true nếu exotic potion → load `exotic_` prefix
- `beneficial`: true nếu tốt → phase 4 show `good.png`, false → show hurt frame
- `callback`: fired ở phase 4 khi animation xong

## deferred apply

`Potion.drink()` cho DUELIST: `apply(h)` được defer đến callback (phase 4), KHÔNG gọi ngay khi drink. Điều này đảm bảo hiệu ứng potion chỉ xảy ra sau khi animation xong.

## Duration

`drinkDuration = Math.max(0.5f, Math.min(5.0f, SPDSettings.actionDuration()))` (default 1.5s)
