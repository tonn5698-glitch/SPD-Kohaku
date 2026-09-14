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
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.ui.Component;

public class HintIndicator extends Component {

	private BitmapText hintText;
	private ColorBlock arrow;
	
	private static final int[] DIR_COLORS = new int[]{
		0x4488FF, // North - Blue
		0xFF8844, // South - Orange
		0x44FF88, // East - Green
		0xFF4488  // West - Pink
	};
	
	private static final String[] DIR_SYMBOLS = new String[]{
		"N", "S", "E", "W"
	};
	
	public HintIndicator() {
		super();
	}
	
	@Override
	protected void createChildren() {
		hintText = new BitmapText(PixelScene.pixelFont);
		hintText.hardlight(0xFFFF88);
		add(hintText);
		
		arrow = new ColorBlock(3, 3, 0xFFFFFF);
		add(arrow);
	}
	
	@Override
	protected void layout() {
		hintText.x = x;
		hintText.y = y;
		PixelScene.align(hintText);
		
		arrow.x = x + hintText.width() + 2;
		arrow.y = y + 2;
	}
	
	public void update() {
		int dir = ScrollOfHint.getHintDirection();
		
		if (dir == ScrollOfHint.DIR_NONE) {
			visible = false;
			return;
		}
		
		visible = true;
		hintText.text("[" + DIR_SYMBOLS[dir] + "]");
		hintText.hardlight(DIR_COLORS[dir]);
		hintText.measure();
		
		arrow.color(DIR_COLORS[dir]);
		
		layout();
	}
	
	public static void clearHint() {
		ScrollOfHint.clearHint();
	}
}
