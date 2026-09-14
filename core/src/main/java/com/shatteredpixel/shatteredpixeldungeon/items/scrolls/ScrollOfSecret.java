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

package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLevitation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfMindVision;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Scroll of Secret — variant of Scroll of Hint.
 * - 15 energy, costs 2 per use, no fixed use-count (energy is the only limit)
 * - Reveals hidden doors (like Scroll of Hint)
 * - Can identify items when thrown on them - this is free, does not cost energy
 * - Flavor text for potions/scrolls/etc based on their actual type
 */
public class ScrollOfSecret extends ScrollOfHint {

	private static final int MAX_ENERGY = 15;
	private static final int ENERGY_PER_USE = 2;

	private int energy = MAX_ENERGY;

	private static final String ENERGY = "secret_energy";

	{
		stackable = false;
		icon = ItemSpriteSheet.Icons.SCROLL_MAGICMAP; // use same icon as Hint
		anonymous = true;
	}

	@Override
	public String status() {
		return energy + "/" + MAX_ENERGY;
	}

	@Override
	public void doRead() {
		if (energy < ENERGY_PER_USE) {
			GLog.w(Messages.get(this, "no_energy"));
			return;
		}

		// Call parent's doRead for hint functionality
		super.doRead();

		// Always deduct energy when scroll is used
		energy -= ENERGY_PER_USE;
	}

	/**
	 * Hook for the "well of knowledge" throw mechanic: call this whenever an item lands
	 * in a heap (dropped or thrown), passing the cell it landed on and the item itself.
	 * If a Scroll of Secret is already sitting in that same heap, the newly-landed item
	 * gets identified via identifyOnStep() below.
	 *
	 * This only wires up *receiving* the throw - it does not change identifyOnStep() or
	 * getIdentifyMessage(), which remain the scroll's own identification logic.
	 */
	public static boolean tryIdentifyAt(int cell, Item droppedItem) {
		if (Dungeon.level == null || droppedItem == null) return false;

		Heap heap = Dungeon.level.heaps.get(cell);
		if (heap == null) return false;

		for (Item item : heap.items) {
			if (item != droppedItem && item instanceof ScrollOfSecret) {
				return identifyOnStep(droppedItem);
			}
		}
		return false;
	}

	/**
	 * Identify an item when thrown onto this scroll.
	 * Called from Scroll.doThrow() or similar.
	 * Free - does not touch energy or any use-count. Only doRead() (scanning) costs energy.
	 */
	public static boolean identifyOnStep(Item item) {
		if (item == null || item.isIdentified()) return false;

		// Check if there's a ScrollOfSecret at the hero's position
		if (Dungeon.level == null) return false;

		String idMsg = getIdentifyMessage(item);
		if (idMsg != null) {
			item.identify();
			GLog.p(idMsg);
			Badges.validateItemLevelAquired(item);
			return true;
		}
		return false;
	}

	/**
	 * Get flavor text for identifying an item.
	 * IMPORTANT: this checks the item's actual class, not item.name() - name() returns the
	 * scrambled/unidentified display name at this point (identify() hasn't run yet), so a
	 * substring check against it never matches anything and always falls through.
	 */
	private static String getIdentifyMessage(Item item) {
		if (item instanceof Potion) {
			if (item instanceof ExoticPotion) {
				return Messages.get(ScrollOfSecret.class, "potion_exotic");
			} else if (item instanceof PotionOfHealing) {
				return Messages.get(ScrollOfSecret.class, "potion_golden");
			} else if (item instanceof PotionOfExperience) {
				return Messages.get(ScrollOfSecret.class, "potion_silver");
			} else if (item instanceof PotionOfFrost) {
				return Messages.get(ScrollOfSecret.class, "potion_azure");
			} else if (item instanceof PotionOfToxicGas) {
				return Messages.get(ScrollOfSecret.class, "potion_bistre");
			} else if (item instanceof PotionOfLiquidFlame) {
				return Messages.get(ScrollOfSecret.class, "potion_crimson");
			} else if (item instanceof PotionOfInvisibility) {
				return Messages.get(ScrollOfSecret.class, "potion_indigo");
			} else if (item instanceof PotionOfPurity) {
				return Messages.get(ScrollOfSecret.class, "potion_ivory");
			} else if (item instanceof PotionOfStrength) {
				return Messages.get(ScrollOfSecret.class, "potion_jade");
			} else if (item instanceof PotionOfHaste) {
				return Messages.get(ScrollOfSecret.class, "potion_magenta");
			} else if (item instanceof PotionOfLevitation) {
				return Messages.get(ScrollOfSecret.class, "potion_turquoise");
			} else if (item instanceof PotionOfMindVision) {
				return Messages.get(ScrollOfSecret.class, "potion_teal");
			} else if (item instanceof PotionOfParalyticGas) {
				return Messages.get(ScrollOfSecret.class, "potion_slate");
			} else {
				return Messages.get(ScrollOfSecret.class, "potion_amber");
			}
		} else if (item instanceof Weapon) {
			return Messages.get(ScrollOfSecret.class, "weapon");
		} else if (item instanceof Armor) {
			return Messages.get(ScrollOfSecret.class, "armor");
		} else if (item instanceof Ring) {
			return Messages.get(ScrollOfSecret.class, "ring");
		} else if (item instanceof Wand) {
			return Messages.get(ScrollOfSecret.class, "wand");
		} else if (item instanceof Scroll) {
			if (item instanceof ExoticScroll) {
				return Messages.get(ScrollOfSecret.class, "scroll_exotic");
			} else if (item instanceof ScrollOfUpgrade) {
				return Messages.get(ScrollOfSecret.class, "scroll_upgrade");
			} else if (item instanceof ScrollOfIdentify) {
				return Messages.get(ScrollOfSecret.class, "scroll_identify");
			} else if (item instanceof ScrollOfRemoveCurse) {
				return Messages.get(ScrollOfSecret.class, "scroll_removecurse");
			} else if (item instanceof ScrollOfMagicMapping) {
				return Messages.get(ScrollOfSecret.class, "scroll_magicmapping");
			} else if (item instanceof ScrollOfTeleportation) {
				return Messages.get(ScrollOfSecret.class, "scroll_teleport");
			} else if (item instanceof ScrollOfRecharging) {
				return Messages.get(ScrollOfSecret.class, "scroll_recharging");
			} else if (item instanceof ScrollOfMirrorImage) {
				return Messages.get(ScrollOfSecret.class, "scroll_mirrorimage");
			} else if (item instanceof ScrollOfRage) {
				return Messages.get(ScrollOfSecret.class, "scroll_rage");
			} else if (item instanceof ScrollOfRetribution) {
				return Messages.get(ScrollOfSecret.class, "scroll_retribution");
			} else if (item instanceof ScrollOfTerror) {
				return Messages.get(ScrollOfSecret.class, "scroll_terror");
			} else if (item instanceof ScrollOfTransmutation) {
				return Messages.get(ScrollOfSecret.class, "scroll_transmutation");
			} else if (item instanceof ScrollOfLullaby) {
				return Messages.get(ScrollOfSecret.class, "scroll_lullaby");
			} else if (item instanceof ScrollOfSecret) {
				//check before ScrollOfHint - this class is a subclass of it
				return Messages.get(ScrollOfSecret.class, "scroll_secret");
			} else if (item instanceof ScrollOfHint) {
				return Messages.get(ScrollOfSecret.class, "scroll_hint");
			} else {
				return Messages.get(ScrollOfSecret.class, "scroll_unknown");
			}
		}
		return Messages.get(ScrollOfSecret.class, "item");
	}

	@Override
	public boolean isKnown() {
		return true;
	}

	@Override
	public void reset() {
		super.reset();
		image = Random.element(new Integer[]{
				ItemSpriteSheet.SCROLL_BERKANAN,
				ItemSpriteSheet.SCROLL_ODAL,
				ItemSpriteSheet.SCROLL_TIWAZ
		});
	}

	@Override
	public int value() {
		return isKnown() ? 50 * quantity : super.value();
	}

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
	
	//upgrades a Scroll of Hint into a Scroll of Secret. Cost depends on the input scroll's
	//own energy stat (see ScrollOfHint.energy) - a fuller scroll converts more cheaply.
	//Scroll of Secret has no recipe of its own - it can only be obtained this way.
	public static class HintToSecret extends Recipe {
		
		private static final int LOW_ENERGY_COST = 6;
		private static final int HIGH_ENERGY_COST = 4;
		private static final int HIGH_ENERGY_THRESHOLD = 7; //7-10 energy -> cheaper upgrade
		
		@Override
		public boolean testIngredients(ArrayList<Item> ingredients) {
			//exact class match only - excludes ScrollOfSecret itself, which extends ScrollOfHint
			return ingredients.size() == 1
					&& ingredients.get(0).getClass() == ScrollOfHint.class
					&& ingredients.get(0).quantity() == 1;
		}
		
		@Override
		public int cost(ArrayList<Item> ingredients) {
			if (!testIngredients(ingredients)) return 0;
			int energy = ((ScrollOfHint) ingredients.get(0)).getEnergy();
			return energy >= HIGH_ENERGY_THRESHOLD ? HIGH_ENERGY_COST : LOW_ENERGY_COST;
		}
		
		@Override
		public Item brew(ArrayList<Item> ingredients) {
			if (!testIngredients(ingredients)) return null;
			
			Item hint = ingredients.get(0);
			hint.quantity(hint.quantity() - 1);
			
			return new ScrollOfSecret();
		}
		
		@Override
		public Item sampleOutput(ArrayList<Item> ingredients) {
			return new ScrollOfSecret();
		}
	}
}
