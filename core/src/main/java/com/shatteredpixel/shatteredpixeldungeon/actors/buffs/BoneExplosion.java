package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

/**
 * Volatile skeleton bone explosion buff.
 *
 * Mechanics:
 * - 15% chance on spawn to be marked volatile
 * - When hero attacks → dark particles emit (3-15 based on HP)
 * - When HP < 30% → warning sound + message
 * - When HP < 50% after warning → drain skeleton HP + speed reduction
 * - On death:
 *   - Low (6-12): immediate explosion
 *   - Medium (12-24): 1 turn delay, red square indicator
 *   - Max (24-46): 1 turn delay, red square indicator
 * - 85% chance = normal (no explosion)
 */
public class BoneExplosion extends Buff {

    private static final String VOLATILE = "volatile";
    private static final String WARNED   = "warned";
    private static final String TIER     = "tier";

    public boolean volatile_ = false;
    private boolean warned = false;
    private int tier = 0; // 0=none, 1=low, 2=medium, 3=max

    // explosion damage ranges per tier
    public static final int[] MIN_DMG = {6, 6, 12, 24};
    public static final int[] MAX_DMG = {12, 12, 24, 46};

    public void setVolatile(boolean v) {
        volatile_ = v;
    }

    public boolean isVolatile() {
        return volatile_;
    }

    /**
     * Determine explosion tier based on damage dealt to skeleton.
     * Called when skeleton takes damage.
     */
    public void updateTier(int damageDealt, int skeletonHP, int skeletonHT) {
        if (!volatile_) return;

        float hpRatio = (float) skeletonHP / skeletonHT;

        // Tier based on damage relative to max HP
        float dmgRatio = (float) damageDealt / skeletonHT;
        if (dmgRatio >= 0.5f) {
            tier = 3; // max
        } else if (dmgRatio >= 0.25f) {
            tier = 2; // medium
        } else {
            tier = 1; // low
        }

        // Warning at HP < 30%
        if (hpRatio < 0.3f && !warned) {
            warned = true;
            warn();
        }

        // Drain + speed reduction at HP < 50% after warning (medium/max)
        if (warned && hpRatio < 0.5f && tier >= 2) {
            drainSkeleton();
        }
    }

    private void warn() {
        if (target == null || !(target instanceof Mob)) return;
        Mob mob = (Mob) target;

        String msg;
        switch (tier) {
            case 3:
                msg = Messages.get(this, "warn_max");
                break;
            case 2:
                msg = Messages.get(this, "warn_medium");
                break;
            default:
                msg = Messages.get(this, "warn_low");
                break;
        }

        if (Dungeon.level.heroFOV[mob.pos]) {
            GLog.w(msg);
            Sample.INSTANCE.play(Assets.Sounds.HIT, 0.5f, 0.5f, 1.5f);
        }
    }

    private void drainSkeleton() {
        if (target == null || !(target instanceof Mob)) return;
        Mob mob = (Mob) target;

        // Drain 20% of current HP (can kill)
        int drain = Math.max(1, mob.HP / 5);
        mob.HP -= drain;

        // Reduce movement speed by applying Cripple
        Buff.affect(mob, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple.class);

        if (Dungeon.level.heroFOV[mob.pos]) {
            GLog.w(Messages.get(this, "drain"));
        }

        // If HP <= 0, skeleton dies (gives EXP)
        if (mob.HP <= 0) {
            mob.die(null);
        }
    }

    /**
     * Called when the volatile skeleton dies.
     * Returns the explosion damage range based on tier.
     */
    public int getMinDamage() {
        return MIN_DMG[tier];
    }

    public int getMaxDamage() {
        return MAX_DMG[tier];
    }

    public int getTier() {
        return tier;
    }

    /**
     * Create delayed explosion for medium/max tier.
     * The explosion happens after 1 turn.
     */
    public static void delayedExplosion(final int pos, final int minDmg, final int maxDmg) {
        Actor.addDelayed(new Actor() {
            {
                actPriority = MOB_PRIO - 1;
            }

            @Override
            protected boolean act() {
                // Emit explosion particles
                if (Dungeon.level.heroFOV[pos]) {
                    CellEmitter.get(pos).burst(Speck.factory(Speck.BONE), 15);
                    Sample.INSTANCE.play(Assets.Sounds.BLAST);
                }

                // Deal damage to adjacent cells
                boolean heroKilled = false;
                for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
                    Char ch = Actor.findChar(pos + PathFinder.NEIGHBOURS8[i]);
                    if (ch != null && ch.isAlive()) {
                        int damage = Math.round(Random.NormalIntRange(minDmg, maxDmg));
                        damage = Math.max(0, damage - (ch.drRoll() + ch.drRoll()));
                        ch.damage(damage, BoneExplosion.class);
                        if (ch == Dungeon.hero && !ch.isAlive()) {
                            heroKilled = true;
                        }
                    }
                }

                if (heroKilled) {
                    Dungeon.fail(null);
                    GLog.n(Messages.get(BoneExplosion.class, "explo_kill"));
                }

                return true;
            }
        }, 1f); // 1 turn delay
    }

    @Override
    public int icon() {
        return BuffIndicator.NONE;
    }

    @Override
    public String name() {
        return Messages.get(this, "name");
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc");
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(VOLATILE, volatile_);
        bundle.put(WARNED, warned);
        bundle.put(TIER, tier);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        volatile_ = bundle.getBoolean(VOLATILE);
        warned = bundle.getBoolean(WARNED);
        tier = bundle.getInt(TIER);
    }
}
