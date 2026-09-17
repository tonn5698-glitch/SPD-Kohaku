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

---

## Statue/Golem Nerf (-20%)

**Files:** `actors/mobs/Statue.java`, `ArmoredStatue.java`, `Golem.java`

Giảm 20% sức mạnh của tượng đá trong phòng khoá và golem ở tầng ≥ 15.

### Statue
| Stat | Trước | Sau |
|------|-------|-----|
| HP | 15 + depth×5 | ×0.8 |
| Defense | 4 + depth | ×0.8 |
| Attack | (9+depth)×weapon | ×0.8 |

### ArmoredStatue
| Stat | Trước | Sau |
|------|-------|-----|
| HP | 30 + depth×10 | ×0.8 |

### Golem
| Stat | Trước | Sau |
|------|-------|-----|
| HP | 120 | 96 |
| Damage | 25-30 | 20-24 |
| Defense | 15 | 12 |
| Attack | 28 | 22 |

---

## Duelist - Sword Split Ability

**Files:** `items/weapon/melee/MeleeWeapon.java`, `Sword.java`

Thêm kĩ năng thứ hai "Split" (Phân luồng) cho Kiếm. Tấn công tất cả kẻ địch xung quanh mục tiêu.

### MeleeWeapon
- Thêm `AC_ABILITY2` action
- Thêm `duelistAbility2()`, `hasSecondAbility()`, `ability2ChargeUse()`

### Sword
```java
@Override
protected boolean hasSecondAbility(){
    return true;
}

@Override
protected void duelistAbility2(Hero hero, Integer target) {
    int dmgBoost = augment.damageFactor(3 + buffedLvl());
    Sword.splitAbility(hero, target, 0.75f, dmgBoost, this);
}
```

Split tấn công tất cả kẻ địch trong phạm vi 1 ô quanh mục tiêu, sát thương ×0.75.

---

## Dart Trap Kill Badge

**File:** `Badges.java`, `WornDartTrap.java`

Thành tựu "Bạn thật là thông minh" / "You're So Smart" khi giết quái vật bằng bẫy phi tiêu.

```java
// WornDartTrap.java - khi mob chết
if (finalTarget instanceof Mob && !finalTarget.isAlive()){
    Badges.validateDartTrapKill();
}
```
