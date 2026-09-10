# 3. Animation uống nước (Waterskin)

Tương tự drink animation nhưng dùng cho Waterskin item.

## Flow

```
Waterskin.execute(hero, AC_DRINK)
  ├── Validate volume >= dropsToConsume
  ├── HeroSprite.startDrink("waterskin", false, true, callback)
  ├── hero.spend(TIME_TO_DRINK)
  ├── hero.busy()
  └── hero.sprite.operate(hero.pos)

callback (phase 4):
  ├── volume -= drops
  ├── Dewdrop.consumeDew(drops, hero, true)  // apply heal
  └── updateQuickslot()
```

## Fix double-heal

Trước: `consumeDew()` được gọi 2 lần — 1 lần ở line 117 (ngay khi chọn uống), 1 lần ở callback.

Sau: Chỉ check `volume >= dropsToConsume` ở đầu, heal duy nhất ở callback (phase 4).

## Fix quickslot update

Trước: `updateQuickslot()` chạy trước callback (ngay sau `operate()`).

Sau: `updateQuickslot()` chạy SAU callback để hiển thị số nước mới.
