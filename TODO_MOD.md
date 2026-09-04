# SPD Kohaku Mod - Tổng hợp issues còn lại

## Files đã sửa/thêm
- `android/build.gradle` — package: `com.tonsgame.spdkohaku`
- `HeroSprite.java` — scale 96→16, diagonal move, drink timer fields
- `Hero.java` — facing, restFacing, diagonal detection, handle() immediate facing
- `SPDSettings.java` — charSpeed, drinkDuration
- `WndGame.java` — "Cài đặt nhân vật" button
- `WndCharSettings.java` — speed slider + drink duration slider
- `KohakuDrinkEffect.java` — **BỊ LỖI, CẦN XÓA HOẶC VIẾT LẠI**
- `Potion.java` — hiện tại đã xóa drink effect call, cần restore lại

## BUG CHÍNH: Freeze khi uống thuốc

### Root cause (user giải thích)
SPD dùng actor system: `sprite.play(anim)` → actor chờ → `onComplete()` → `ch.next()` → actor release. Nếu animation không complete đúng cách → actor không release → freeze.

### Flow hiện tại của Potion.drink():
```java
hero.spend( TIME_TO_DRINK );  // advance time
hero.busy();                    // ready = false
apply( hero );
hero.sprite.operate( hero.pos ); // plays operate animation
```
`operate` animation → `onComplete(anim)` → `ch.onOperateComplete()` → `next()` → actor release

### Lỗi: 
- `KohakuDrinkEffect` thêm Image vào scene → phá vỡ animation chain
- `HeroSprite.startDrink()` swap texture trong `update()` → có thể phá animation
- Cần **KHÔNG thêm Image mới vào scene**, chỉ swap texture trên hero sprite hiện có

### Giải pháp cần làm:
1. **Xóa hoàn toàn `KohakuDrinkEffect.java`**
2. **Trong `Potion.drink()`**: restore lại call `hero.sprite.operate()` bình thường
3. **Trong `HeroSprite`**: Thêm method `startDrink(color)` chỉ swap texture + set timer
4. **Trong `HeroSprite.update()`**: Timer swap held→drinking→good, KHÔNG can thiệp animation
5. **Quan trọng**: `operate()` animation phải hoàn thành đúng → `onComplete()` → `next()`. Không được block animation.

## BUG: Vị trí sprite sai
- Sprite hiện ở góc phải thay vì đúng vị trí hero
- Fix: dùng `hero.sprite.center()` thay vì tính toán thủ công

## Đã fix xong
- ✅ Package name: `com.tonsgame.spdkohaku`
- ✅ Idle frame: `row + 0` (standing) thay vì `row + 1` (walking)
- ✅ Diagonal detection: `Math.abs(delta) != 1 && != width` (bỏ width±1)
- ✅ Diagonal facing: LEFT/RIGHT khi tween, UP/DOWN khi dừng (restFacing)
- ✅ Speed setting trong WndCharSettings
- ✅ Drink duration setting
- ✅ Drink test scene (tap version text ở title screen)
- ✅ Screen recording (bỏ targetSandboxVersion)

## Cấu trúc sprite uống thuốc
```
sprites/kohaku_drink/
├── held_{color}.png     (96x96, 12 màu thường + 12 exotic)
├── drinking.png         (96x96)
├── good.png             (96x96, cho beneficial potions)
```

## Cài đặt
- Char Speed: 1(slow)-10(fast), default 5, interval = `0.36 - speed*0.036` seconds
- Drink Duration: 0.5s-5.0s, default 1.5s (stored as int×10)

## Diaolog/Campo
- `WARRIOR SPRITE` dùng icon `Icons.TARGET` cho nút "Cài đặt nhân vật"
