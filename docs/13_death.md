# 13. Death Animation

Khi hero chết, chơi 12-row animation từ 3 sprite sheets.

## Sprite sheets

```
sprites/kohaku_die/DMZ7.png   — rows 0-3
sprites/kohaku_die/DMZ8.png   — rows 4-7
sprites/kohaku_die/DMZ9.png   — rows 8-11
```

Mỗi sheet: 4 rows (DOWN/LEFT/RIGHT/UP), mỗi row = 1 frame 96×96.

## Flow

```
startDeath(callback)
  ├── dieSheet = 0, dieRow = 0
  ├── texture(DIE_PATHS[0])  // DMZ7.png
  └── frame(first frame)

update() — death state
  ├── dieTimer += elapsed
  ├── duration = SPDSettings.loseAnimDuration() (default 3.0s)
  ├── Total: 12 rows × (duration/12) per row
  ├── Every row: load sheet texture, set frame
  ├── After all 12 rows: callback → hero.die()
```

## Duration

`loseAnimDuration = SPDSettings.loseAnimDuration()` (default 3.0s, range 0.5-5.0s)
