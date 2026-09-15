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
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
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
 * - 15 energy, no fixed use-count (energy is the only limit, no regen)
 * - Reveals hidden doors (like Scroll of Hint), costs 1 energy per read
 * - Can identify items when thrown on them (real identify, item.identify() called) -
 *   costs energy too, at a 20% discount vs Scroll of Hint's vague-hint costs
 * - Real, stat-based text for weapon/armor/ring/artifact/wand; flavor text for potions/scrolls
 */
public class ScrollOfSecret extends ScrollOfHint {

	public static final int MAX_ENERGY = 15;
	private static final int ENERGY_PER_USE = 1;
	//20% off ScrollOfHint's identify costs, rounded down
	private static final int IDENTIFY_COST_LOW = (int) (ScrollOfHint.IDENTIFY_COST_LOW * 0.8f);   // 2 -> 1
	private static final int IDENTIFY_COST_HIGH = (int) (ScrollOfHint.IDENTIFY_COST_HIGH * 0.8f); // 5 -> 4

	private int energy = MAX_ENERGY;

	private static final String ENERGY = "secret_energy";

	{
		stackable = false;
		icon = ItemSpriteSheet.Icons.SCROLL_MAGICMAP; // use same icon as Hint
		anonymous = true;
	}

	public void setEnergy(int value) {
		energy = Math.max(0, Math.min(MAX_ENERGY, value));
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
	 * gets identified via identifyOnStep() below - drawing energy from that specific scroll.
	 *
	 * This only wires up *receiving* the throw - it does not change getIdentifyMessage()
	 * flavor text for potions/scrolls, which remain the scroll's own identification logic.
	 */
	public static boolean tryIdentifyAt(int cell, Item droppedItem) {
		if (Dungeon.level == null || droppedItem == null) return false;

		Heap heap = Dungeon.level.heaps.get(cell);
		if (heap == null) return false;

		for (Item item : heap.items) {
			if (item != droppedItem && item instanceof ScrollOfSecret) {
				return ((ScrollOfSecret) item).identifyOnStep(droppedItem);
			}
		}
		return false;
	}

	/**
	 * Identify an item when thrown onto this scroll. Costs energy from THIS specific scroll -
	 * 20% cheaper than Scroll of Hint's vague-hint cost, rounded down.
	 */
	private boolean identifyOnStep(Item item) {
		if (item == null || item.isIdentified()) return false;
		if (Dungeon.level == null) return false;

		int cost = isHighCostItem(item) ? IDENTIFY_COST_HIGH : IDENTIFY_COST_LOW;
		if (energy < cost) {
			GLog.w(Messages.get(this, "no_energy"));
			return false;
		}

		String idMsg = getIdentifyMessage(item);
		if (idMsg != null) {
			item.identify();
			energy -= cost;
			GLog.p(idMsg);
			Badges.validateItemLevelAquired(item);
			return true;
		}
		return false;
	}

	/**
	 * Get identify text for an item. Potions/scrolls: flavor text keyed by real type (via the
	 * shared classifyItem() in ScrollOfHint - IMPORTANT: keyed by class, not item.name(), since
	 * name() returns the scrambled/unidentified display name at this point). Weapon/armor/ring/
	 * artifact/wand: real stat-based text built from the item's own fields.
	 */
	private static String getIdentifyMessage(Item item) {
		if (item instanceof Armor) {
			return armorIdentifyMessage((Armor) item);
		} else if (item instanceof Weapon) {
			return cursedRevealMessage(((Weapon) item).cursed);
		} else if (item instanceof Artifact) {
			return cursedRevealMessage(((Artifact) item).cursed);
		} else if (item instanceof Ring) {
			return Messages.get(ScrollOfSecret.class, "identify_ring", Messages.get(item.getClass(), "name"));
		} else if (item instanceof Wand) {
			return Messages.get(ScrollOfSecret.class, "identify_wand", Messages.get(item.getClass(), "name"));
		} else if (item instanceof Potion || item instanceof Scroll) {
			return Messages.get(ScrollOfSecret.class, classifyItem(item));
		}
		return Messages.get(ScrollOfSecret.class, "item");
	}
	
	private static String armorIdentifyMessage(Armor armor) {
		String glyphPhrase = armor.glyph != null
				? Messages.get(ScrollOfSecret.class, "identify_has_glyph",
						Messages.get(armor.glyph.getClass(), "name", "").trim())
				: Messages.get(ScrollOfSecret.class, "identify_no_glyph");
		return Messages.get(ScrollOfSecret.class, "identify_armor", armor.STRReq(), glyphPhrase);
	}
	
	private static String cursedRevealMessage(boolean cursed) {
		return Messages.get(ScrollOfSecret.class, cursed ? "identify_cursed" : "identify_not_cursed");
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
