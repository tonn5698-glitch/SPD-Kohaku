/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BoneExplosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ShieldOfLight;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Chasm;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot;
import com.shatteredpixel.shatteredpixeldungeon.sprites.SkeletonSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Skeleton extends Mob {

	// 25% chance to be volatile on spawn
	private static final float VOLATILE_CHANCE = 0.25f;

	{
		spriteClass = SkeletonSprite.class;

		HP = HT = 25;
		defenseSkill = 9;

		EXP = 5;
		maxLvl = 10;

		loot = Generator.Category.WEAPON;
		lootChance = 0.1667f; //by default, see lootChance()

		properties.add(Property.UNDEAD);
		properties.add(Property.INORGANIC);
	}

	@Override
	protected void onAdd() {
		super.onAdd();
		// 15% chance to be volatile on spawn
		if (Random.Float() < VOLATILE_CHANCE) {
			Buff.affect(this, BoneExplosion.class).setVolatile(true);
		}
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 2, 10 );
	}

	@Override
	public int defenseProc(Char enemy, int damage) {
		BoneExplosion be = buff(BoneExplosion.class);
		if (be != null && be.isVolatile() && sprite != null && sprite.visible) {
			// Emit dark particles when hit
			emitDarkParticles(be);
			// Update tier based on damage dealt
			be.updateTier(damage, HP, HT);
		}
		return super.defenseProc(enemy, damage);
	}

	/**
	 * Emit dark particles from the skeleton.
	 * More particles as HP decreases.
	 */
	private void emitDarkParticles(BoneExplosion be) {
		if (sprite == null) return;

		float hpRatio = (float) HP / HT;
		// 3-15 particles based on HP
		int count = (int)(3 + (1f - hpRatio) * 12);

		sprite.emitter().burst(Speck.factory(Speck.BONE), count);
	}

	@Override
	public void die( Object cause ) {

		BoneExplosion be = buff(BoneExplosion.class);

		super.die( cause );

		if (cause == Chasm.class) return;

		if (be != null && be.isVolatile()) {
			int tier = be.getTier();
			int minDmg = be.getMinDamage();
			int maxDmg = be.getMaxDamage();

			if (tier >= 2) {
				// Medium/Max: delayed explosion (1 turn)
				if (Dungeon.level.heroFOV[pos]) {
					GLog.w(Messages.get(this, "exploding"));
					// Red square indicator on the cell
					CellEmitter.get(pos).burst(Speck.factory(Speck.LIGHT), 8);
				}
				BoneExplosion.delayedExplosion(pos, minDmg, maxDmg);
			} else {
				// Low: immediate explosion
				explodeNow(pos, minDmg, maxDmg);
			}
		} else {
			// Normal skeleton: vanilla explosion (6-12)
			explodeNow(pos, 6, 12);
		}
	}

	/**
	 * Immediate explosion at the given position.
	 */
	private void explodeNow(int cell, int minDmg, int maxDmg) {
		boolean heroKilled = false;
		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
			Char ch = findChar( cell + PathFinder.NEIGHBOURS8[i] );
			if (ch != null && ch.isAlive()) {
				int damage = Math.round(Random.NormalIntRange(minDmg, maxDmg));
				damage = Math.round( damage * AscensionChallenge.statModifier(this));

				//all sources of DR are 2x effective vs. bone explosion
				WandOfLivingEarth.RockArmor rockArmor = ch.buff(WandOfLivingEarth.RockArmor.class);
				if (rockArmor != null) {
					int preDmg = damage;
					damage = rockArmor.absorb(damage);
					damage *= Math.round(damage/(float)preDmg);
				}

				Earthroot.Armor armor = ch.buff( Earthroot.Armor.class );
				if (damage > 0 && armor != null) {
					int preDmg = damage;
					damage = armor.absorb( damage );
					damage -= (preDmg - damage);
				}

				if (ch.buff(MagicImmune.class) == null) {
					ShieldOfLight.ShieldOfLightTracker shield = ch.buff(ShieldOfLight.ShieldOfLightTracker.class);
					if (shield != null && shield.object == id()) {
						int min = 1 + Dungeon.hero.pointsInTalent(Talent.SHIELD_OF_LIGHT);
						damage -= Random.NormalIntRange(min, 2 * min);
						damage -= Random.NormalIntRange(min, 2 * min);
						damage = Math.max(damage, 0);
					} else if (ch == Dungeon.hero
							&& Dungeon.hero.heroClass != HeroClass.CLERIC
							&& Dungeon.hero.hasTalent(Talent.SHIELD_OF_LIGHT)
							&& TargetHealthIndicator.instance.target() == this) {
						if (Random.Int(6) < 1 + Dungeon.hero.pointsInTalent(Talent.SHIELD_OF_LIGHT)) {
							damage -= 2;
						}
					}

					if (ch.buff(HolyWard.HolyArmBuff.class) != null){
						damage -= Dungeon.hero.subClass == HeroSubClass.PALADIN ? 6 : 2;
					}
				}

				damage = Math.max( 0,  damage - (ch.drRoll() + ch.drRoll()) );
				ch.damage( damage, this );
				if (ch == Dungeon.hero && !ch.isAlive()) {
					heroKilled = true;
				}
			}
		}

		if (Dungeon.level.heroFOV[cell]) {
			Sample.INSTANCE.play( Assets.Sounds.BONES );
		}

		if (heroKilled) {
			Dungeon.fail( this );
			GLog.n( Messages.get(this, "explo_kill") );
		}
	}

	@Override
	public float lootChance() {
		return super.lootChance() * (float)Math.pow(1/3f, Dungeon.LimitedDrops.SKELE_WEP.count);
	}

	@Override
	public Item createLoot() {
		Dungeon.LimitedDrops.SKELE_WEP.count++;
		return super.createLoot();
	}

	@Override
	public int attackSkill( Char target ) {
		return 12;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 5);
	}

	@Override
	public String info() {
		String desc = super.info();

		BoneExplosion be = buff(BoneExplosion.class);
		if (be != null && be.isVolatile()) {
			int tier = be.getTier();
			String tierName;
			int tierColor;
			switch (tier) {
				case 3:
					tierName = Messages.get(this, "tier_max");
					tierColor = 0xFF4444; // red
					break;
				case 2:
					tierName = Messages.get(this, "tier_medium");
					tierColor = 0xFF8800; // orange
					break;
				default:
					tierName = Messages.get(this, "tier_low");
					tierColor = 0xFFFF44; // yellow
					break;
			}
			desc += "\n\n_" + tierName + "_";
		}

		return desc;
	}

}
