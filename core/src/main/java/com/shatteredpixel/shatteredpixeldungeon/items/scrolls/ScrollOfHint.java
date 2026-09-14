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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfClairvoyance;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.watabou.utils.Rect;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

public class ScrollOfHint extends Scroll {

	public static final int DIR_NONE = -1;
	public static final int DIR_NORTH = 0;
	public static final int DIR_SOUTH = 1;
	public static final int DIR_EAST = 2;
	public static final int DIR_WEST = 3;
	
	private static int hintDirection = DIR_NONE;
	
	//quality stat only - does NOT gate reading (doRead() has no limit). Set at creation
	//(alchemy recipe, or MAX_ENERGY by default for loot-found scrolls). Used only by
	//StoneToScroll (crafting) and HintToSecret (upgrading) recipes below.
	public static final int MAX_ENERGY = 10;
	private int energy = MAX_ENERGY;
	
	// Identification: 3 uses, costs 3 energy per identification
	private int idUses = 3;
	private int maxIdUses = 3;
	
	{
		stackable = false;
		icon = ItemSpriteSheet.Icons.SCROLL_MAGICMAP;
		anonymous = true;
	}
	
	public int getEnergy() {
		return energy;
	}
	
	public void setEnergy(int value) {
		energy = Math.max(0, Math.min(MAX_ENERGY, value));
	}
	
	@Override
	public String status() {
		return energy + "/" + MAX_ENERGY;
	}
	
	public static int getHintDirection() {
		return hintDirection;
	}
	
	public static void clearHint() {
		hintDirection = DIR_NONE;
	}
	
	public int getIdUses() { return idUses; }
	public int getMaxIdUses() { return maxIdUses; }
	
	/**
	 * Identify an item using this scroll.
	 * Returns true if identification was successful.
	 */
	public boolean identifyItem(Item item) {
		if (idUses <= 0) {
			GLog.w(Messages.get(this, "no_id_uses"));
			return false;
		}
		
		if (item == null || item.isIdentified()) {
			return false;
		}
		
		item.identify();
		idUses--;
		GLog.p(Messages.get(this, "id_success", item.title()));
		return true;
	}
	
	//indexed by DIR_NORTH/DIR_SOUTH/DIR_EAST/DIR_WEST (0-3) - keep in sync with those constants
	private static final String[] DOOR_KEYS    = {"hint_door_north", "hint_door_south", "hint_door_east", "hint_door_west"};
	private static final String[] DIRWORD_KEYS = {"dir_word_north", "dir_word_south", "dir_word_east", "dir_word_west"};
	private static final String[] DIR_LABELS   = {"N", "S", "E", "W"};

	private static final String[] HERE_KEYS   = {"hint_here_1", "hint_here_2", "hint_here_3"};
	private static final String[] ADJACENT_KEYS = {"hint_adjacent_1", "hint_adjacent_2", "hint_adjacent_3"};
	private static final String[] FLAVOR_KEYS = {"hint_flavor_1", "hint_flavor_2", "hint_flavor_3", "hint_flavor_4"};

	@Override
	public void doRead() {
		if (!(Dungeon.level instanceof RegularLevel)) {
			GLog.i(Messages.get(this, "no_secret"));
			hintDirection = DIR_NONE;
			readAnimation();
			return;
		}
		
		RegularLevel level = (RegularLevel) Dungeon.level;
		
		// Find secret rooms that haven't been discovered yet
		ArrayList<Room> secretRooms = new ArrayList<>();
		for (Room r : level.rooms()) {
			if (r instanceof SecretRoom) {
				// Check if the secret room's entrance has been discovered
				boolean discovered = false;
				if (r.connected.isEmpty()) {
					// Room not connected yet, treat as undiscovered
					discovered = false;
				} else {
					// Check if any door to this room has been discovered
					for (Room neighbor : r.connected.keySet()) {
						Room.Door door = r.connected.get(neighbor);
						if (door != null) {
							int doorPos = level.pointToCell(new Point(door.x, door.y));
							// If the door is no longer SECRET_DOOR, it has been discovered
							if (doorPos >= 0 && doorPos < level.length()
									&& level.map[doorPos] != Terrain.SECRET_DOOR) {
								discovered = true;
								break;
							}
						}
					}
				}
				
				if (!discovered) {
					secretRooms.add(r);
				}
			}
		}
		
		if (secretRooms.isEmpty()) {
			GLog.i(Messages.get(this, "no_secret"));
			hintDirection = DIR_NONE;
		} else {
			Room secret = Random.element(secretRooms);
			Room heroRoom = level.room(Dungeon.hero.pos);
			
			RoomPath path = findPathToRoom(heroRoom, secret);
			
			if (path == null) {
				//no known route through generated room connections (e.g. hero is standing in a corridor tile)
				//fall back to a straight-line direction so the compass still points somewhere useful
				Point heroPos = Dungeon.level.cellToPoint(Dungeon.hero.pos);
				Point secretCenter = secret.center();
				int dir = getDirection(secretCenter.x - heroPos.x, secretCenter.y - heroPos.y);
				
				hintDirection = dir;
				GLog.p(Messages.get(this, "hint_unclear", Messages.get(this, DIRWORD_KEYS[dir])));
				GLog.i(Messages.get(this, "hint_dir", DIR_LABELS[dir]));
				
			} else if (path.roomsAway == 0) {
				//secret room and hero's room are the same - no direction to travel, just search here
				Point heroPos = Dungeon.level.cellToPoint(Dungeon.hero.pos);
				Point secretCenter = secret.center();
				hintDirection = getDirection(secretCenter.x - heroPos.x, secretCenter.y - heroPos.y);
				
				GLog.p(Messages.get(this, HERE_KEYS[Random.Int(HERE_KEYS.length)]));
				GLog.i(Messages.get(this, "hint_dir", DIR_LABELS[hintDirection]));
				
			} else {
				int dir = doorSideToDir(path.doorSide);
				hintDirection = dir;
				
				if (path.roomsAway == 1) {
					//right through the next door - saying "1 room away" reads as far, so skip the count entirely
					String dirWord = Messages.get(this, dir != DIR_NONE ? DIRWORD_KEYS[dir] : "dir_word_unknown");
					String template = Messages.get(this, ADJACENT_KEYS[Random.Int(ADJACENT_KEYS.length)]);
					GLog.p(Messages.format(template, dirWord));
				} else {
					String doorPhrase = Messages.get(this, dir != DIR_NONE ? DOOR_KEYS[dir] : "hint_door_unknown");
					String distPhrase = Messages.get(this, "hint_rooms_many", path.roomsAway);
					String template = Messages.get(this, FLAVOR_KEYS[Random.Int(FLAVOR_KEYS.length)]);
					GLog.p(Messages.format(template, doorPhrase, distPhrase));
				}
				
				if (dir != DIR_NONE) {
					GLog.i(Messages.get(this, "hint_dir", DIR_LABELS[dir]));
				}
			}
		}
		
		SpellSprite.show(curUser, SpellSprite.MAP);
		Sample.INSTANCE.play(Assets.Sounds.READ);
		
		readAnimation();
	}
	
	private int getDirection(int dx, int dy) {
		boolean horizontal = Math.abs(dx) > Math.abs(dy);
		if (horizontal) {
			return dx > 0 ? DIR_EAST : DIR_WEST;
		} else {
			return dy > 0 ? DIR_SOUTH : DIR_NORTH;
		}
	}
	
	private static int doorSideToDir(int doorSide) {
		if (doorSide == Room.TOP)         return DIR_NORTH;
		else if (doorSide == Room.BOTTOM) return DIR_SOUTH;
		else if (doorSide == Room.RIGHT)  return DIR_EAST;
		else if (doorSide == Room.LEFT)   return DIR_WEST;
		else                              return DIR_NONE;
	}
	
	//shortest path (in rooms) from 'from' to 'to', using the level's generated room connections.
	//returns null if 'from' is null or if no known route exists (e.g. secret room not yet linked).
	private static RoomPath findPathToRoom(Room from, Room to) {
		if (from == null) return null;
		if (from == to) return new RoomPath(0, -1);
		
		HashMap<Room, Room> parent = new HashMap<>();
		HashMap<Room, Integer> dist = new HashMap<>();
		ArrayDeque<Room> queue = new ArrayDeque<>();
		
		parent.put(from, null);
		dist.put(from, 0);
		queue.add(from);
		
		while (!queue.isEmpty()) {
			Room cur = queue.poll();
			if (cur == to) break;
			for (Room next : cur.connected.keySet()) {
				if (!dist.containsKey(next)) {
					dist.put(next, dist.get(cur) + 1);
					parent.put(next, cur);
					queue.add(next);
				}
			}
		}
		
		if (!dist.containsKey(to)) return null;
		
		//backtrack to find which of 'from's neighbouring rooms starts the shortest path
		Room firstHop = to;
		while (parent.get(firstHop) != from) {
			firstHop = parent.get(firstHop);
		}
		
		Rect edge = from.intersect(firstHop);
		int doorSide;
		if      (edge.width()  == 0 && edge.left   == from.left)   doorSide = Room.LEFT;
		else if (edge.height() == 0 && edge.top    == from.top)    doorSide = Room.TOP;
		else if (edge.width()  == 0 && edge.right  == from.right)  doorSide = Room.RIGHT;
		else if (edge.height() == 0 && edge.bottom == from.bottom) doorSide = Room.BOTTOM;
		else doorSide = -1;
		
		return new RoomPath(dist.get(to), doorSide);
	}
	
	private static class RoomPath {
		final int roomsAway;
		final int doorSide;
		RoomPath(int roomsAway, int doorSide) {
			this.roomsAway = roomsAway;
			this.doorSide = doorSide;
		}
	}
	
	@Override
	public boolean isKnown() {
		return true;
	}
	
	@Override
	public void reset() {
		super.reset();
		image = Random.element(new Integer[]{
				ItemSpriteSheet.SCROLL_KAUNAN, ItemSpriteSheet.SCROLL_SOWILO,
				ItemSpriteSheet.SCROLL_LAGUZ, ItemSpriteSheet.SCROLL_YNGVI,
				ItemSpriteSheet.SCROLL_GYFU, ItemSpriteSheet.SCROLL_RAIDO,
				ItemSpriteSheet.SCROLL_ISAZ, ItemSpriteSheet.SCROLL_MANNAZ,
				ItemSpriteSheet.SCROLL_NAUDIZ, ItemSpriteSheet.SCROLL_BERKANAN,
				ItemSpriteSheet.SCROLL_ODAL, ItemSpriteSheet.SCROLL_TIWAZ
		});
	}
	
	@Override
	public int value() {
		return 50;
	}
	
	private static final String ENERGY = "hintEnergy";
	private static final String ID_USES = "idUses";
	
	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(ENERGY, energy);
		bundle.put(ID_USES, idUses);
	}
	
	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		energy = bundle.contains(ENERGY) ? bundle.getInt(ENERGY) : MAX_ENERGY;
		idUses = bundle.getInt(ID_USES);
	}
	
	//crafts a Scroll of Hint from Stones of Clairvoyance: 1 stone = 2 energy.
	//quantity of stones provided determines the resulting scroll's energy (capped at MAX_ENERGY).
	public static class StoneToScroll extends Recipe {
		
		private static final int ENERGY_PER_STONE = 2;
		
		@Override
		public boolean testIngredients(ArrayList<Item> ingredients) {
			return ingredients.size() == 1
					&& ingredients.get(0) instanceof StoneOfClairvoyance
					&& ingredients.get(0).quantity() >= 1;
		}
		
		@Override
		public int cost(ArrayList<Item> ingredients) {
			return 0;
		}
		
		private int stonesUsed(ArrayList<Item> ingredients) {
			return Math.min(ingredients.get(0).quantity(), MAX_ENERGY / ENERGY_PER_STONE);
		}
		
		@Override
		public Item brew(ArrayList<Item> ingredients) {
			if (!testIngredients(ingredients)) return null;
			
			Item stones = ingredients.get(0);
			int used = stonesUsed(ingredients);
			stones.quantity(stones.quantity() - used);
			
			ScrollOfHint result = new ScrollOfHint();
			result.setEnergy(used * ENERGY_PER_STONE);
			return result;
		}
		
		@Override
		public Item sampleOutput(ArrayList<Item> ingredients) {
			ScrollOfHint result = new ScrollOfHint();
			if (ingredients != null && ingredients.size() == 1 && ingredients.get(0) instanceof StoneOfClairvoyance) {
				result.setEnergy(stonesUsed(ingredients) * ENERGY_PER_STONE);
			}
			return result;
		}
	}
}
