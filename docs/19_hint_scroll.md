# 19. Scroll of Hint + Scroll of Secret + Food Buff

## Tóm tắt

**Food buff**: Thức ăn xuất hiện 20% nhiều hơn.

**Scroll of Hint**: Cuộn giấy gợi ý. Spawn tự nhiên trong loot table. Khi đọc, BFS trên room-graph để tính đúng cửa cần đi + số phòng cách. Đọc vô hạn, có energy để chế tạo.

**Scroll of Secret**: Cuộn giấy bí mật. Ném/thả item vào ô đang có scroll này để tự động identify item đó. Nhận từ Scroll of Hint qua giả kim.

## Files

| File | Mô tả |
|------|-------|
| `items/scrolls/ScrollOfHint.java` | Cuộn gợi ý - BFS room-graph, energy, alchemy |
| `items/scrolls/ScrollOfSecret.java` | Cuộn bí mật - identify khi ném/thả item vào ô |
| `items/Generator.java` | Food buff + register scrolls |
| `items/Recipe.java` | Công thức giả kim mới |
| `levels/Level.java` | 20% bonus food + hook ScrollOfSecret |
| `levels/RegularLevel.java` | (đã xóa spawn explicit) |
| `ui/StatusPane.java` | HintCompass indicator |
| `ui/HintCompass.java` | Compass N/S/E/W highlight |
| `messages/items/items.properties` | English messages |
| `messages/items/items_vi.properties` | Vietnamese messages |

## Mechanics

### Food Buff (+20%)

**Generator.java:**
```java
FOOD.defaultProbs = new float[]{ 5, 1.2f, 0 }; // trước: {4, 1, 0}
```

**Level.java:**
```java
addItemToSpawn(Generator.random(Generator.Category.FOOD));
if (Random.Float() < 0.2f) {
    addItemToSpawn(Generator.random(Generator.Category.FOOD));
}
```

### Scroll of Hint

**Spawn:** Tự nhiên trong loot table (prob = 1, thấp nhưng có thể spawn ở bất kỳ đâu).

**Đọc:** BFS trên room-graph để tìm phòng bí mật gần nhất:
1. Nếu không có secret room → "Cuộn giấy phát sáng nhẹ nhưng không tìm thấy gì..."
2. Nếu có → BFS tính số phòng cách + cửa cần đi qua
3. Hiển thị hướng trên compass (N/S/E/W highlight xanh)

**Format gợi ý:**
- Cách 1 phòng: "Nó ở ngay phía nam."
- Cách 2+ phòng: "Hãy tìm cánh cửa phía đông, cách 2 căn phòng."

**Energy:** Mặc định 10, hiện ở status. Không ảnh hưởng việc đọc.

**Compass:** Luôn hiện N, S, E, W. Hướng đúng highlight màu xanh lá.

### Scroll of Secret

**Cơ chế:** Ném hoặc thả item vào ô đang có Scroll of Secret → tự động identify item đó.

**Miễn phí:** Không tốn energy/uses.

**Hook:** Gọi `ScrollOfSecret.tryIdentifyAt(cell, item)` trong `Level.drop()`.

**Lưu ý:** Potion ném thẳng sẽ vỡ trước khi vào heap (hành vi gốc của engine). Phải thả từ inventory.

### Alchemy Recipes

| Công thức | Nguyên liệu | Kết quả | Năng lượng |
|-----------|-------------|---------|------------|
| StoneToScroll | 1 Đá thấu thị | Scroll of Hint | 2 energy/tối đa 5 đá |
| HintToSecret | 1 Scroll of Hint | Scroll of Secret | 4 (energy ≥7) hoặc 6 (energy <7) |

### Compass (HintCompass)

Luôn hiển thị N, S, E, W ở góc trên trái. Khi đọc Scroll of Hint, hướng đúng chuyển màu xanh lá (`#44FF88`).

```java
//_hintCompass.java
private static final int COLOR_DEFAULT = 0xCCCCCC;
private static final int COLOR_HIGHLIGHT = 0x44FF88;
```

### Death Animation Fix

**Không spawn bia mộ khi có Ankh:**
```java
boolean hasAnkh = false;
for (Ankh ankh : hero.belongings.getAllItems(Ankh.class)) {
    hasAnkh = true; break;
}
if (!hasAnkh) { // spawn tombstone }
```

**Fix 2 Kohaku khi resurrect:**
```java
heroSprite.visible = true;
heroSprite.place(pos);
```

### Bundle (Save/Load)

ScrollOfHint lưu `energy`:
```java
private static final String ENERGY = "energy";

@Override
public void storeInBundle(Bundle bundle) {
    super.storeInBundle(bundle);
    bundle.put(ENERGY, energy);
}

@Override
public void restoreFromBundle(Bundle bundle) {
    super.restoreFromBundle(bundle);
    energy = bundle.getInt(ENERGY);
}
```

## Messages (EN)

```properties
# Scroll of Hint
items.scrolls.scrollofhint.name=scroll of hint
items.scrolls.scrollofhint.desc=A rare scroll that can sense secret rooms. Has unlimited uses.
items.scrolls.scrollofhint.no_secret=The scroll glows faintly but finds nothing to reveal...

# Scroll of Secret
items.scrolls.scrollofsecret.name=scroll of secrets
items.scrolls.scrollofsecret.desc=A rare scroll that can identify items by proximity.
items.scrolls.scrollofsecret.identify=The %s was identified by the scroll's magic!
```

## Messages (VI)

```properties
# Cuộn giấy gợi ý
items.scrolls.scrollofhint.name=cuộn giấy gợi ý
items.scrolls.scrollofhint.desc=Một cuộn giấy hiếm có thể cảm nhận phòng bí mật. Đọc vô hạn.
items.scrolls.scrollofhint.no_secret=Cuộn giấy phát sáng nhẹ nhưng không tìm thấy gì để hé lộ...

# Cuộn giấy bí mật
items.scrolls.scrollofsecret.name=cuộn giấy bí mật
items.scrolls.scrollofsecret.desc=Một cuộn giấy hiếm có thể định danh vật phẩm bằngproximity.
items.scrolls.scrollofsecret.identify=%s đã được định danh bởi phép thuật của cuộn giấy!
```

## Backup

Các file backup nằm trong `/backup/`:
- `Generator.java.bak`
- `Level.java.bak`
- `RegularLevel.java.bak`
- `ItemSpriteSheet.java.bak`
- `items.properties.bak`
- `items_vi.properties.bak`
