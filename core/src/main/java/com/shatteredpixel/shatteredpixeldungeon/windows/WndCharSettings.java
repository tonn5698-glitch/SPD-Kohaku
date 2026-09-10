package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.OptionSlider;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

public class WndCharSettings extends Window {

	private static final int WIDTH = 120;
	private static final int SLIDER_HEIGHT = 21;
	private static final int GAP = 2;

	public WndCharSettings() {
		super();

		float pos = 0;

		// Character speed slider: 1 (slow) to 10 (fast), default 5
		OptionSlider speedSlider = new OptionSlider(
				Messages.get(this, "char_speed"),
				Messages.get(this, "slow"),
				Messages.get(this, "fast"),
				1, 10) {
			@Override
			protected void onChange() {
				SPDSettings.charSpeed(getSelectedValue());
			}
		};
		speedSlider.setSelectedValue(SPDSettings.charSpeed());
		add(speedSlider);
		speedSlider.setRect(0, pos, WIDTH, SLIDER_HEIGHT);
		pos += SLIDER_HEIGHT + GAP;

		// Drink animation duration slider: 5 (0.5s) to 50 (5.0s), default 15 (1.5s)
		OptionSlider drinkSlider = new OptionSlider(
				Messages.get(this, "action_duration"),
				"0.5s",
				"5.0s",
				5, 50) {
			@Override
			protected void onChange() {
				SPDSettings.actionDuration(getSelectedValue() / 10f);
			}
		};
		drinkSlider.setSelectedValue((int)(SPDSettings.actionDuration() * 10));
		add(drinkSlider);
		drinkSlider.setRect(0, pos, WIDTH, SLIDER_HEIGHT);
		pos += SLIDER_HEIGHT + GAP;

		// Transform animation duration slider: 5 (0.5s) to 50 (5.0s), default 20 (2.0s)
		OptionSlider transformSlider = new OptionSlider(
				Messages.get(this, "transform_duration"),
				"0.5s",
				"5.0s",
				5, 50) {
			@Override
			protected void onChange() {
				SPDSettings.transformDuration(getSelectedValue() / 10f);
			}
		};
		transformSlider.setSelectedValue((int)(SPDSettings.transformDuration() * 10));
		add(transformSlider);
		transformSlider.setRect(0, pos, WIDTH, SLIDER_HEIGHT);
		pos += SLIDER_HEIGHT + GAP;

		// Lose animation duration slider: 5 (0.5s) to 50 (5.0s), default 30 (3.0s)
		OptionSlider loseSlider = new OptionSlider(
				Messages.get(this, "lose_anim_duration"),
				"0.5s",
				"5.0s",
				5, 50) {
			@Override
			protected void onChange() {
				SPDSettings.loseAnimDuration(getSelectedValue() / 10f);
			}
		};
		loseSlider.setSelectedValue((int)(SPDSettings.loseAnimDuration() * 10));
		add(loseSlider);
		loseSlider.setRect(0, pos, WIDTH, SLIDER_HEIGHT);
		pos += SLIDER_HEIGHT + GAP;

		resize(WIDTH, (int) pos);
	}
}
