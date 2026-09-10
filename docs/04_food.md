# 4. Animation ăn đồ ăn

Tương tự drink nhưng dùng cho food items.

## Flow

```
Food.eat(hero)
  └── HeroSprite.startFood(foodName, harmful, skipHeldUp, skipGood, callback)
        └── foodTimer = 0 → update() handles phases
```

## 4 Phases

| Phase | Thời gian | Texture | Mô tả |
|-------|-----------|---------|-------|
| 0 Pull | 0-10% | `kohaku_pull_down.png` | Lấy đồ ăn, forced DOWN |
| 1 Held | 10-30% | `food/foodName.png` | Cầm đồ ăn |
| 2 HeldUp | 30-60% | `food_down/foodName.png` | Giơ lên (bỏ qua nếu `skipHeldUp`) |
| 3 Good/Hurt | 60-100% | `good.png` hoặc `HURT_PATH` | Fires callback |

## Parameters

```java
startFood(String foodName, boolean harmful, boolean skipHeldUp, boolean skipGood, Callback effect)
```

- `foodName`: tên asset (e.g. "ration", "meat", "berry")
- `harmful`: true → show hurt frame ở phase 3 (thịt sống)
- `skipHeldUp`: true → bỏ qua phase 2 (bland_chunks)
- `skipGood`: true → không show texture ở phase 3 (light food)

## Asset paths

```
sprites/kohaku_food/foodName.png       held food (phase 1)
sprites/kohaku_food_down/foodName.png  held-down food (phase 2)
```
