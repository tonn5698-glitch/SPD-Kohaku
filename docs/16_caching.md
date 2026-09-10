# 16. Texture Caching

6 film objects được cache static, tạo 1 lần, dùng lại.

## Cached films

| Film | Asset | Grid | Dùng khi |
|------|-------|------|----------|
| `kohakuFilm_` | `kohaku.png` | 21×4 (96×96) | Normal walk/attack |
| `dizzyFilm_` | `kohaku_dizzy.png` | 8×4 (96×96) | Dizzy walk/idle |
| `dizzyAttackFilm_` | `kohaku_dizzy_attack.png` | 4×4 (96×96) | Dizzy attack |
| `moniniFilm_` | `kohaku_monini/walk.png` | 8×4 (96×96) | Monini walk |
| `hurtFilm` | `kohaku_hurt.png` | 3×4 (96×96) | Hurt flash |
| `pullFilm_` | `kohaku_pull_down.png` | 3×4 (96×96) | Pull from pouch |

## Lazy init pattern

```java
public static TextureFilm kohakuFilm() {
    if (kohakuFilm_ == null) {
        SmartTexture kt = TextureCache.get("sprites/kohaku.png");
        kt.filter(Texture.LINEAR, Texture.LINEAR);
        kohakuFilm_ = new TextureFilm(kt, 96, 96);
    }
    return kohakuFilm_;
}
```

## Tại sao cache

- `TextureFilm` tạo UV coordinates cho mỗi frame → expensive nếu tạo mỗi frame
- Static cache → tạo 1 lần, reuse xuyên suốt game session
- `TextureCache.get()` của libGDX cũng cache texture → không load lại từ disk

## TextureFilm

```java
new TextureFilm(texture, frameWidth, frameHeight);
// Tạo grid UV coordinates: cols = texture.width / frameWidth
// get(index) → RectF UV cho frame tại index
```
