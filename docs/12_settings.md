# 12. Cài đặt nhân vật

4 settings cho Kohaku, truy cập từ menu game.

## Settings

| Setting | Key | Default | Range | Mô tả |
|---------|-----|---------|-------|-------|
| Char Speed | `char_speed` | 5 | 1-10 | Tốc độ di chuyển |
| Drink Duration | `drink_duration` | 1.5s | 0.5-5.0s | Thời gian animation uống thuốc |
| Transform Duration | `transform_duration` | 2.0s | 0.5-5.0s | Thời gian Monini transform |
| Lose Anim Duration | `lose_anim_duration` | 3.0s | 0.5-5.0s | Thời gian death animation |

## Speed → Interval

```
interval = 0.36 - charSpeed × 0.036
charSpeed=1  → 0.324s
charSpeed=5  → 0.180s
charSpeed=10 → 0.000s (capped at 0.08s min)
```

## Storage

- Drink/Transform/Lose duration: stored as `int × 10` (e.g. 1.5s → 15)
- Char speed: stored directly as int

## UI

`WndCharSettings` — 4 `OptionSlider` trong một cửa sổ. Truy cập từ `WndGame` → "Cài đặt nhân vật".
