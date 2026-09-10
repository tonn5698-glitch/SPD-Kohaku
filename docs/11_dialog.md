# 11. RPG Maker Dialog

`WndRPGDialog` — message window kiểu RPG Maker / visual novel.

## Tính năng

- **Typewriter text**: 30 chars/giây
- **Highlighting**: `_text_` hoặc `**text**` → màu hồng đỏ `0xFF5858`
- **Multi-page**: `String[] messages` — tap sang trang kế, hết trang cuối → close
- **Name tag**: trái cho nhân vật khác, phải cho Kohaku
- **Fade-in**: chrome + name tag fade alpha 0→1 trong 0.12s
- **Continue arrow**: mũi tên nảy nhấp nháy ở góc dưới-phải

## Layout

```
┌─────────────────────────────────────┐
│  [Name Tag]                         │  ← name tag (optional)
├─────────────────────────────────────┤
│  Text typewriter...                 │
│                                     │
│                          ▼ (arrow)  │
└─────────────────────────────────────┘
```

- **Width**: `uiCamera.width - SIDE_MARGIN * 2` (full screen width)
- **Height**: `uiCamera.height / 4` (1/4 screen height)
- **Position**: Docked to bottom (offset tính từ center)
- **Name tag position**: Left (default) / Right (Kohaku)

## Constructor

```java
// Single page
new WndRPGDialog("Kohaku", "Xin chào!");

// Multi-page
new WndRPGDialog("Kohaku", new String[]{
    "Mình là _KOHAKU_.",
    "Rất vui được gặp bạn!"
});

// With callback
new WndRPGDialog("Kohaku", "Text", () -> { /* on close */ });
```

## Tap behavior

1. Đang gõ chữ → skip đến hết dòng
2. Đã gõ xong + còn trang → sang trang kế
3. Đã gõ xong + trang cuối → close + onFinish callback

## Chrome

Dùng `Chrome.Type.RPG_WINDOW` — 96×96 NinePatch từ `Window.png` của RPG Maker game gốc, margin 8px.
