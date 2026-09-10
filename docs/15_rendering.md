# 15. 96×96 → 16×16 Rendering

Kohaku dùng sprite sheet 96×96 pixel nhưng render trên screen ở 16×16.

## Cơ chế

```java
// HeroSprite.frame() override
public void frame(RectF rect) {
    super.frame(rect);  // sets width=96, height=96 from UV
    if (Dungeon.hero.heroClass == HeroClass.DUELIST) {
        this.width = LOGICAL_SIZE;   // 16
        this.height = LOGICAL_SIZE;  // 16
        this.scale.set(1f, 1f);
        updateVertices();
    }
}
```

## Texture filter

```java
// kohakuFilm() — lazy init
SmartTexture kt = TextureCache.get("sprites/kohaku.png");
kt.filter(Texture.LINEAR, Texture.LINEAR);  // GPU downsamples 96→16
```

## Tại sao 16×16

- SPD world uses 16×16 tile grid
- `width/height` = 16 → tất cả position/center/emitter math dùng 16px
- GPU tự downsampling từ 96→16 qua LINEAR filter → sprite mịn

## Impact

- `avatar.width` = 16 (cho positioning trong StatusPane/WndHero)
- `sprite.center()` = center of 16×16 quad
- `sprite.x/y` = top-left of 16×16 quad
