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

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndRPGDialog;
import com.watabou.noosa.Scene;

/**
 * Debug screen: cycles through RPG Maker-style dialog messages.
 * Tap version on title -> pick "Debug textbox" -> this scene.
 */
public class DebugDialogScene extends Scene {

	private int step = 0;

	@Override
	public void create() {
		super.create();

		showStep(0);
	}

	private void showStep(int index) {

		switch (index) {
			case 0:
				addToFront(new WndRPGDialog("Kohaku", "faces/kohaku_a1.png",
						"Xin chào.",
						nextStep()));
				break;
			case 1:
				addToFront(new WndRPGDialog("Kohaku", "faces/kohaku_a4.png",
						"Mình là _KOHAKU_.",
						nextStep()));
				break;
			case 2:
				addToFront(new WndRPGDialog("Kohaku", "faces/kohaku_a5.png",
						"_Rất vui_ khi được gặp bạn.",
						new Runnable() {
							@Override
							public void run() {
								// fade out and return to title
								ShatteredPixelDungeon.switchScene(TitleScene.class);
							}
						}));
				break;
		}
	}

	private Runnable nextStep() {
		return new Runnable() {
			@Override
			public void run() {
				step++;
				if (step < 3) {
					showStep(step);
				}
			}
		};
	}
}
