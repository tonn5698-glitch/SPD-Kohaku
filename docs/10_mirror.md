# 10. Mirror Image (Scroll of Mirror Image)

## Vấn đề

MirrorSprite mặc định dùng tier-based 12×15 pixel frames → khi hero dùng kohaku 96×96 sheet → sprite bị phóng to sai.

## Fix

```java
// MirrorSprite.java
isKohaku = Dungeon.hero.heroClass == HeroClass.DUELIST;

if (isKohaku) {
    texture(Dungeon.hero.heroClass.spritesheet());
    updateArmorKohaku(0);
} else {
    // standard mirror behavior
}
```

## updateArmorKohaku()

```java
TextureFilm film = HeroSprite.kohakuFilm(); // shared cached film

idle  = new Animation(1, true);
idle.frames(film, row + 0);  // row 0, frame 0

run   = new Animation(12, true);
run.frames(film, row+1..row+8); // walk cycle

die   = new Animation(20, false);
die.frames(film, row + 1);

attack = new Animation(15, false);
attack.frames(film, row+9..row+12); // first 4 attack frames

scale.set(16f / 96f, 16f / 96f); // downsample to 16×16
```

## MirrorSprite vs HeroSprite

| | HeroSprite | MirrorSprite |
|---|---|---|
| Film | `kohakuFilm()` (shared) | `kohakuFilm()` (shared) |
| Scale | 16×16 (via `frame()` override) | 16×96 → scale to 16×16 |
| Idle | Full 8-frame walk | Single frame |
| Attack | 12 frames | 4 frames |
| Dizzy | Dùng dizzyFilm | Không dùng |
