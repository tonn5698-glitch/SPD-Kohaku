# Buffs / Thay đổi tỷ lệ

## Mob Drop Rate (+10%)

**File:** `actors/mobs/Mob.java`

Tăng 10% tỷ lệ rơi đồ của tất cả quái vật.

```java
// Trong lootChance()
dropBonus += 0.10f; // +10% mob drop rate
```

Áp dụng cho tất cả mobs, cộng dồn với Ring of Wealth và talents.

---

## Grass Drop Rate (+25%)

**File:** `levels/features/HighGrass.java`

Tăng 25% tỷ lệ rơi vật phẩm khi giẫm cỏ.

### Seed Drop
```java
// Trước: lootChance = 1/(25f - naturalismLevel*4f)
// Sau:
lootChance = 1/(25f - naturalismLevel*4f);
lootChance *= 1.25f; // +25%
```

Tỷ lệ gốc: 1/25 → 1/9 (tùy naturalism)
Sau buff: ~1/20 → ~1/7

### Dew Drop
```java
// Trước: lootChance = 1/(6f - naturalismLevel/2f)
// Sau:
lootChance = 1/(6f - naturalismLevel/2f);
lootChance *= 1.25f; // +25%
```

Tỷ lệ gốc: 1/6 → 1/4 (tùy naturalism)
Sau buff: ~1/5 → ~1/3

---

## Food Spawn Rate (+20%)

**File:** `levels/Level.java`

Tăng 20% chance spawn bonus food mỗi tầng.

```java
addItemToSpawn(Generator.random(Generator.Category.FOOD));
if (Random.Float() < 0.2f) {
    addItemToSpawn(Generator.random(Generator.Category.FOOD));
}
```

---

## Food Generator Probabilities

**File:** `items/Generator.java`

```java
FOOD.defaultProbs = new float[]{ 5, 1.2f, 0 }; // trước: {4, 1, 0}
```

| Item | Trước | Sau |
|------|-------|-----|
| Food (Ration) | 4 | 5 |
| Pasty | 1 | 1.2 |
| MysteryMeat | 0 | 0 |
