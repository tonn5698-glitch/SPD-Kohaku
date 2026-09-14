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

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfHint;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Group;

public class HintCompass extends Group {

	private BitmapText north, south, east, west;
	
	private static final int COLOR_DEFAULT = 0xCCCCCC;
	private static final int COLOR_HIGHLIGHT = 0x44FF88;
	
	public HintCompass() {
		super();
		
		north = createLabel("N");
		south = createLabel("S");
		east = createLabel("E");
		west = createLabel("W");
	}
	
	private BitmapText createLabel(String text) {
		BitmapText label = new BitmapText(PixelScene.pixelFont);
		label.text(text);
		label.hardlight(COLOR_DEFAULT);
		label.measure();
		add(label);
		return label;
	}
	
	@Override
	public void update() {
		super.update();
		
		int dir = ScrollOfHint.getHintDirection();
		
		north.hardlight(dir == ScrollOfHint.DIR_NORTH ? COLOR_HIGHLIGHT : COLOR_DEFAULT);
		south.hardlight(dir == ScrollOfHint.DIR_SOUTH ? COLOR_HIGHLIGHT : COLOR_DEFAULT);
		east.hardlight(dir == ScrollOfHint.DIR_EAST ? COLOR_HIGHLIGHT : COLOR_DEFAULT);
		west.hardlight(dir == ScrollOfHint.DIR_WEST ? COLOR_HIGHLIGHT : COLOR_DEFAULT);
	}
	
	public void setPos(float x, float y) {
		// N at top
		north.x = x + 8 - north.width() / 2f;
		north.y = y;
		PixelScene.align(north);
		
		// S at bottom
		south.x = x + 8 - south.width() / 2f;
		south.y = y + 22;
		PixelScene.align(south);
		
		// E at right
		east.x = x + 18;
		east.y = y + 11;
		PixelScene.align(east);
		
		// W at left
		west.x = x - 4;
		west.y = y + 11;
		PixelScene.align(west);
	}
}
