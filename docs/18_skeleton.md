# 18. Skeleton — Bone Explosion Feature

## Tóm tắt

Skeleton có **15% chance** spawn với marker "volatile". Khi đánh → dark particles. Khi chết → explosion damage 6-46.

## Files

| File | Mô tả |
|------|-------|
| `actors/mobs/Skeleton.java` | Volatile spawn, particles, explosion |
| `actors/buffs/BoneExplosion.java` | Buff marker + explosion delay |

## Mechanics

### Spawn
- **15% chance** = volatile (BoneExplosion buff)
- **85% chance** = normal (không nổ, không damage khi chết)

### Dark particles (khi đánh)
```
HP 100% → 3 particles
HP 75%  → 6 particles
HP 50%  → 9 particles
HP 25%  → 12 particles
HP 10%  → 14 particles
```

### Warning (HP < 30%)
```
Low:    "Bộ xương: Xì Xì"
Medium: "Bộ xương: XÌ, XÌ"
Max:    "Bộ xương: XÌ XÌ PHÙNG PHÙNG - Có lẽ bạn muốn chạy đấy."
```

### Drain (Medium/Max, HP < 50% sau warning)
- Drain 20% HP của skeleton
- Apply Cripple debuff (giảm tốc độ 50%)
- Player có thể chạy thoát

### Explosion khi chết

| Tier | Trigger | Damage | Timing |
|------|---------|--------|--------|
| Low (1) | Damage < 25% HT | 6-12 | Immediate |
| Medium (2) | Damage 25-50% HT | 12-24 | 1 turn delay + red square |
| Max (3) | Damage > 50% HT | 24-46 | 1 turn delay + red square |

### Red square indicator
```java
CellEmitter.get(pos).burst(Speck.factory(Speck.LIGHT), 8);
```

### Delayed explosion
```java
Actor.addDelayed(new Actor() {
    // explosion after 1 turn
}, 1f);
```

### DR interaction
Giống vanilla: DR applied 2x (Rock Armor, Earthroot, Shield of Light, Holy Ward, natural DR).
