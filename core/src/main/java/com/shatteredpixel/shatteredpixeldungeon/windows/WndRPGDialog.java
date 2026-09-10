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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.PointerArea;

/**
 * RPG Maker / visual-novel style message window (v3 — no face, 1/4 screen height).
 *
 * Layout:
 *  - box docked to the BOTTOM of the screen, full width, height = 1/4 screen
 *  - name tag at top-left (other characters) or top-right (Kohaku)
 *  - text spans full box width
 *  - bouncing continue arrow at bottom-right
 *
 * Supports:
 *  - Typewriter text (char-by-char reveal)
 *  - _text_ / **text** highlighting (pink-red color)
 *  - Multi-page messages (String[]), one tap-to-continue arrow per page
 *  - Open fade-in animation on the chrome/name tag
 *  - Bouncing "continue" arrow while waiting for input
 *  - Tap: while typing -> skip to end; done -> next page or close
 */
public class WndRPGDialog extends Window {

	protected static final int SIDE_MARGIN   = 8;
	protected static final int BOTTOM_MARGIN = 6;
	protected static final int PAD           = 10;

	protected static final int HIGHLIGHT_COLOR = 0xFF5858;

	protected static final int TEXT_FONT_SIZE = 8;
	protected static final int NAME_FONT_SIZE = 8;

	// typewriter speed: characters per second
	protected static final float CHAR_PER_SEC = 30f;

	// open animation
	protected static final float OPEN_DURATION = 0.12f;

	// continue-arrow bob/pulse
	protected static final String ARROW_ASSET = "interfaces/rpg_arrow_down.png";
	protected static final float ARROW_SPEED  = 6f;
	protected static final float ARROW_BOB    = 2f;

	protected String[] messages;
	protected int msgIndex = 0;

	protected String fullText;
	protected int textIndex = 0;
	protected float timer = 0;
	protected boolean typing = false;

	protected RenderedTextBlock textBlock;

	protected float textOffsetY = PAD;

	// optional name tag
	protected NinePatch nameChrome;
	protected RenderedTextBlock nameBlock;

	// continue indicator
	protected Image continueArrow;
	protected float arrowBaseX, arrowBaseY;
	protected float arrowTimer = 0;

	// open fade-in
	protected float openTimer = 0;

	protected Runnable onFinish;

	public WndRPGDialog(String speaker, String message) {
		this(speaker, new String[]{message}, null);
	}

	public WndRPGDialog(String speaker, String message, Runnable onFinish) {
		this(speaker, new String[]{message}, onFinish);
	}

	public WndRPGDialog(String speaker, String[] messages) {
		this(speaker, messages, null);
	}

	public WndRPGDialog(String speaker, String[] messages, Runnable onFinish) {
		super(dialogWidth(), dialogHeight(), Chrome.get(Chrome.Type.RPG_WINDOW));

		this.messages = messages;
		this.onFinish = onFinish;

		// dock the box to the bottom of the screen
		offset(0, (int) (PixelScene.uiCamera.height / 2f - BOTTOM_MARGIN - height / 2f));

		// chrome fades in instead of popping instantly
		chrome.alpha(0);

		// name tag (optional) — left for others, right for Kohaku
		if (speaker != null && !speaker.isEmpty()) {
			nameBlock = PixelScene.renderTextBlock(speaker, NAME_FONT_SIZE);
			nameBlock.hardlight(0xFFFFFF);
			nameBlock.setHightlighting(false);

			float nameW = nameBlock.width() + PAD * 2;
			float nameH = nameBlock.height() + PAD;

			nameChrome = Chrome.get(Chrome.Type.RPG_WINDOW);
			nameChrome.size(nameW, nameH);
			nameChrome.alpha(0);

			boolean isKohaku = speaker.equalsIgnoreCase("Kohaku");
			if (isKohaku) {
				// right side
				nameChrome.x = width - PAD - nameW;
			} else {
				// left side (default)
				nameChrome.x = PAD;
			}
			nameChrome.y = -chrome.marginTop() + 2;
			add(nameChrome);

			nameBlock.setPos(nameChrome.x + PAD, nameChrome.y + PAD / 2f);
			add(nameBlock);

			// shift content down to make room
			textOffsetY = nameH + PAD;
		}

		// message text — full box width
		textBlock = PixelScene.renderTextBlock(TEXT_FONT_SIZE);
		textBlock.maxWidth(width - PAD * 2);
		textBlock.setHightlighting(true, HIGHLIGHT_COLOR);
		textBlock.setPos(PAD, textOffsetY);
		add(textBlock);

		// continue / advance indicator, bottom-right corner
		continueArrow = new Image(ARROW_ASSET);
		continueArrow.alpha(0);
		arrowBaseX = width - PAD - continueArrow.width();
		arrowBaseY = height - PAD - continueArrow.height();
		continueArrow.x = arrowBaseX;
		continueArrow.y = arrowBaseY;
		add(continueArrow);

		// tap anywhere on the box: skip typing / advance page / close
		PointerArea advancer = new PointerArea(0, 0, width, height) {
			@Override
			protected void onClick(PointerEvent event) {
				onBackPressed();
			}
		};
		add(advancer);

		setPage(0);
	}

	protected static int dialogWidth() {
		return (int) PixelScene.uiCamera.width - SIDE_MARGIN * 2;
	}

	protected static int dialogHeight() {
		return (int) (PixelScene.uiCamera.height / 4f);
	}

	protected void setPage(int index) {
		msgIndex = index;
		fullText = (messages != null && index < messages.length) ? messages[index] : "";
		textIndex = 0;
		timer = 0;
		typing = fullText != null && fullText.length() > 0;
		textBlock.text("");
	}

	/** Backwards-compatible single-string setter (restarts as a 1-page message). */
	public void setText(String text) {
		messages = new String[]{text};
		setPage(0);
	}

	protected boolean hasNextPage() {
		return messages != null && msgIndex + 1 < messages.length;
	}

	@Override
	public void update() {
		super.update();

		// open fade-in for chrome / name tag
		if (openTimer < OPEN_DURATION) {
			openTimer += Game.elapsed;
			float p = Math.min(1f, openTimer / OPEN_DURATION);
			chrome.alpha(p);
			if (nameChrome != null) nameChrome.alpha(p);
		}

		if (typing && fullText != null) {
			timer += Game.elapsed;
			int newIndex = (int) (timer * CHAR_PER_SEC);
			if (newIndex > textIndex) {
				textIndex = Math.min(newIndex, fullText.length());
				textBlock.text(fullText.substring(0, textIndex));
			}
			if (textIndex >= fullText.length()) {
				typing = false;
			}
		}

		// bouncing/pulsing continue arrow while waiting for a tap
		if (!typing && fullText != null) {
			arrowTimer += Game.elapsed;
			continueArrow.y = arrowBaseY + (float) Math.sin(arrowTimer * ARROW_SPEED) * ARROW_BOB;
			continueArrow.alpha(0.55f + 0.45f * (float) Math.sin(arrowTimer * ARROW_SPEED));
		} else {
			continueArrow.alpha(0);
		}
	}

	@Override
	public void onBackPressed() {
		if (typing) {
			// skip straight to the full line
			textIndex = fullText.length();
			textBlock.text(fullText);
			typing = false;
		} else if (hasNextPage()) {
			// advance to the next page, same window
			arrowTimer = 0;
			setPage(msgIndex + 1);
		} else {
			hide();
			if (onFinish != null) onFinish.run();
		}
	}
}
