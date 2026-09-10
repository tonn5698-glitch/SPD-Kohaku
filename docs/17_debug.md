# 17. Debug Dialog Scene

Test scene cho RPG Maker dialog system.

## Truy cập

Title screen → tap version text → chọn "Debug Textbox"

## Flow

```
Step 0: "Xin chào." (Kohaku, face A1) → onFinish →
Step 1: "Mình là _KOHAKU_." (Kohaku, face A4) → onFinish →
Step 2: "_Rất vui_ khi được gặp bạn." (Kohaku, face A5) → onFinish →
Return to TitleScene
```

## Code

```java
// DebugDialogScene.java
case 0:
    addToFront(new WndRPGDialog("Kohaku", "Xin chào.", nextStep()));
case 1:
    addToFront(new WndRPGDialog("Kohaku", "Mình là _KOHAKU_.", nextStep()));
case 2:
    addToFront(new WndRPGDialog("Kohaku", "_Rất vui_ khi được gặp bạn.",
        () -> ShatteredPixelDungeon.switchScene(TitleScene.class)));
```

## Assets

Face portraits trong `assets/faces/`:
- `kohaku_a1.png` — 144×144
- `kohaku_a4.png` — 144×144
- `kohaku_a5.png` — 144×144
