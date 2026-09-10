# 9. Portrait / Avatar

## Normal portrait

- **Asset**: `sprites/kohaku_portrait.png` (96×96)
- **Source**: Idle face-down frame (col 0, row 0) từ `kohaku.png`
- **Scale**: `FRAME_HEIGHT / KOHAKU_FRAME_HEIGHT` = 15/96 ≈ 0.156

## Dizzy portrait

- **Asset**: `sprites/kohaku_dizzy_portrait.png` (96×96)
- **Source**: Idle face-down frame (col 0, row 0) từ `kohaku_dizzy.png`
- **Trigger**: low HP <15% HOẶC debuffed HOẶC toxic gas

## avatar(Hero) logic

```java
if (hero.heroClass == DUELIST) {
    boolean lowHP = hero.HP > 0 && hero.HP < hero.HT * 0.15f;
    boolean debuffed = toxicGas || hasDebuff(); // 10 buff types
    if (lowHP || debuffed) {
        return new Image("sprites/kohaku_dizzy_portrait.png"); // scaled
    }
}
return avatar(hero.heroClass, hero.tier()); // normal portrait
```

## StatusPane refresh

- `lastAvatarDizzy` field theo dõi state trước đó
- Mỗi frame: check low HP + toxicGas + debuffs → nếu thay đổi → `updateAvatar()`
- `updateAvatar()` gọi lại `HeroSprite.avatar(hero)` và copy kết quả

## Usage

- `StatusPane` — HUD portrait (góc trên bên trái)
- `WndHero` — Hero info popup portrait
- `WndRanking`, `WndGameInProgress` — Using same `avatar()` method
