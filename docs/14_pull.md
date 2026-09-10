# 14. Pull Animation

Animation "lấy thuốc/túi nước từ túi" — dùng cho cả drink và food.

## Asset

`sprites/kohaku_pull_down.png` — 96×96 single frame

## Dùng trong

- **Drink phase 0** (0-10%): `texture(PULL_PATH)`, forced facing DOWN
- **Food phase 0** (0-10%): tương tự

## Film

`pullFilm()` — cached 3×4 grid từ `kohaku_pull_down.png`
- Frame 0 = face down (dùng cho pull)
- Frames 1-2, 3-5, 6-8, 9-11: các facing khác (không dùng trong practice)
