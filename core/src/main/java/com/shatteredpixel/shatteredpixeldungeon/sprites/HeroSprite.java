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

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HeroDisguise;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;

public class HeroSprite extends CharSprite {
	
	private static final int FRAME_WIDTH	= 12;
	private static final int FRAME_HEIGHT	= 15;
	
	// Kohaku: original 96x96 sheet frames, rendered downsampled to 16x16.
	// The sprite is kept logically 16x16 (see frame() override) so that all
	// position/center/emitter math matches the actual on-screen size.
	private static final int KOHAKU_FRAME_WIDTH  = 96;
	private static final int KOHAKU_FRAME_HEIGHT = 96;
	private static final int LOGICAL_SIZE        = 16;
	
	// RPG Maker walk: 0.667s full cycle / 8 frames = 0.083s/frame = 12fps
	private static final int RUN_FRAMERATE = 12;
	// Attack: 4 unique frames × 3 repeat, ~0.15s per unique = 20fps
	private static final int ATTACK_FRAMERATE = 30;
	
	private static TextureFilm tiers;
	private static TextureFilm kohakuFilm_; // cached kohaku sheet
	private static TextureFilm dizzyFilm_; // cached dizzy sheet (debuff)
	private static TextureFilm moniniFilm_; // cached monini walk sheet
	private static TextureFilm hurtFilm;  // cached: created once, reused
	private static TextureFilm pullFilm_; // cached pull sheet
	private static Animation hurt;        // rebuilt per-facing from cached film

	private Animation fly;
	private Animation read;
	private Animation pull;
	private int lastFacing = -1;
	private boolean lastDebuffed = false;
	private TransformState lastTransformState = TransformState.NORMAL;

	// Kohaku walk-loop: Hero.ready() calls sprite.idle() between every tile,
	// which resets curFrame and makes the walk cycle restart each tile.
	// While busyMoving we instead keep the run loop alive, and only switch to
	// a real idle if no new move arrives within the grace period.
	private boolean busyMoving = false;
	private float idleGrace = -1;

	// Kohaku drink animation timer
	private static final String DRINK_PATH = "sprites/kohaku_drink/";
	private float drinkTimer = -1;  // -1 = not drinking
	private float drinkDuration;
	private String drinkOriginalSheet;
	private int drinkPhase = -1; // 0=held, 1=heldUp, 2=drink, 3=good
	private String drinkColor;
	private boolean drinkExotic;
	private boolean drinkBeneficial;
	private boolean drinkHurt;
	private Callback drinkEffectCallback;
	private boolean drinkEffectFired;

	// Kohaku hurt ("bị đánh") flash when taking damage
	private static final String HURT_PATH = "sprites/kohaku_hurt.png";
	private static final String PULL_PATH = "sprites/kohaku_pull_down.png";
	private static final String DIZZY_PATH = "sprites/kohaku_dizzy.png";
	private float hurtTimer = -1;  // -1 = not hurt
	private String hurtOriginalSheet;

	// Monini transform system
	private enum TransformState { NORMAL, TRANSFORMING, MONINI, DE_TRANSFORMING }
	private TransformState transformState = TransformState.NORMAL;
	private float transformTimer = -1;
	private int turnsSinceAttack = 0;
	private static final float TRANSFORM_DURATION = 0.5f;

	// Ensure the GL texture matches the current film state (dizzy/monini/normal).
	// Prevents UV mismatch when switching between states.
	private void ensureNormalSheet() {
		if (Dungeon.hero == null || Dungeon.hero.heroClass != HeroClass.DUELIST) return;
		String normal = Dungeon.hero.heroClass.spritesheet();
		SmartTexture normalTex = TextureCache.get( normal );

		// Determine which texture should be bound
		SmartTexture targetTex = normalTex;
		if (transformState == TransformState.MONINI) {
			targetTex = TextureCache.get( "sprites/kohaku_monini/walk.png" );
		} else if (isDebuffed()) {
			targetTex = TextureCache.get( DIZZY_PATH );
		}

		if (texture != targetTex) {
			hurtTimer = -1;
			texture( targetTex );
			lastFacing = -1;
			updateFacing();
		}
	}

	// Check if hero has any active debuff (for dizzy animation).
	private static boolean isDebuffed() {
		if (Dungeon.hero == null) return false;
		for (com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff b : Dungeon.hero.buffs()) {
			if (b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Doom
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion) {
				return true;
			}
		}
		return false;
	}

	/** Returns the correct film for current state: dizzy if debuffed, normal otherwise. */
	private static TextureFilm heroFilm() {
		return isDebuffed() ? dizzyFilm() : kohakuFilm();
	}

	// Speed effect detection for monini transform
	private static boolean isSpeedEffect() {
		if (Dungeon.hero == null) return false;
		for (com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff b : Dungeon.hero.buffs()) {
			if (b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreaterHaste
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Speed) {
				return true;
			}
		}
		return false;
	}

	/** Called when hero attacks — resets turn counter and triggers de-transform if MONINI. */
	public void onAttackAction() {
		turnsSinceAttack = 0;
		if (transformState == TransformState.MONINI) {
			transformState = TransformState.DE_TRANSFORMING;
			transformTimer = 0;
		}
	}

	/** Start transform to monini (called when speed effect starts). */
	public void startTransform() {
		if (transformState == TransformState.NORMAL) {
			transformState = TransformState.TRANSFORMING;
			transformTimer = 0;
			turnsSinceAttack = 0;
		}
	}

	/** Reverse transform back to kohaku (called when speed effect ends). */
	public void reverseTransform() {
		if (transformState == TransformState.MONINI) {
			transformState = TransformState.DE_TRANSFORMING;
			transformTimer = 0;
		}
	}

	public HeroSprite() {
		super();
		
		texture( Dungeon.hero.heroClass.spritesheet() );
		// pre-load textures so the TextureFilm built in updateArmor is valid
		if (Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.DUELIST) {
			TextureCache.get( HURT_PATH );
			TextureCache.get( DIZZY_PATH );
		}
		updateArmor();
		
		link( Dungeon.hero );

		if (ch.isAlive())
			idle();
		else
			die();
	}

	public void disguise(HeroClass cls){
		texture( cls.spritesheet() );
		updateArmor();
	}
	
	public void updateArmor() {

		TextureFilm film;
		int facing = 0;
		boolean isKohaku = Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.DUELIST;
		boolean debuffed = isKohaku && isDebuffed();

		if (isKohaku) {
			// MONINI: monini walk (8 cols). Dizzy: dizzy walk (8 cols). Normal: kohaku (21 cols).
			if (transformState == TransformState.MONINI) {
				film = moniniFilm();
			} else {
				film = debuffed ? dizzyFilm() : kohakuFilm();
			}
			facing = Dungeon.hero.facing;
		} else {
			film = new TextureFilm( tiers(), Dungeon.hero.tier(), FRAME_WIDTH, FRAME_HEIGHT );
			scale.set( 1f );
		}

		// Dizzy has 8 cols/row, kohaku has 21. Use correct stride.
		int colsPerRow = debuffed ? 8 : 21;
		int row = facing * colsPerRow;

		// Idle: RPG Maker stands at frame 0 when not moving
		idle = new Animation( 1, true );
		idle.frames( film, row + 0 );

		// Walk: 8 frames at 30fps.
		// Normal sheet reserves col0 for a dedicated idle pose (cols1-8=walk).
		// Dizzy sheet is only 8 cols total with NO separate idle frame —
		// col0 IS the first walk frame. Requesting row+8 there overflows
		// into the next facing row (wrong sprite content).
		run = new Animation( RUN_FRAMERATE, true );
		if (debuffed) {
			run.frames( film, row + 0, row + 1, row + 2, row + 3,
					row + 4, row + 5, row + 6, row + 7 );
		} else {
			run.frames( film, row + 1, row + 2, row + 3, row + 4,
					row + 5, row + 6, row + 7, row + 8 );
		}

		// Die: use idle
		die = new Animation( 20, false );
		die.frames( film, row + 0 );

		// Attack: 12 frames at 30fps — always use kohakuFilm (21 cols)
		attack = new Animation( ATTACK_FRAMERATE, false );
		int attackRow = facing * 21; // kohaku always 21 cols
		attack.frames( kohakuFilm(), attackRow + 9, attackRow + 10, attackRow + 11,
				attackRow + 12, attackRow + 13, attackRow + 14,
				attackRow + 15, attackRow + 16, attackRow + 17,
				attackRow + 18, attackRow + 19, attackRow + 20 );

		zap = attack.clone();

		// Kohaku: operate — always use kohakuFilm
		operate = new Animation( ATTACK_FRAMERATE, false );
		operate.frames( kohakuFilm(), attackRow + 15, attackRow + 16, attackRow + 17,
				attackRow + 18, attackRow + 19, attackRow + 20 );

// Kohaku hurt ("bị đánh") animation: 3 cols x 4 rows of 96x96,
	// row0=DOWN, row1=LEFT, row2=RIGHT, row3=UP (RPG Maker ordering).
	if (isKohaku) {
		int hurtRow = Dungeon.hero.facing;
		hurt = new Animation( 15, false );
		hurt.frames( hurtFilm(), hurtRow * 3, hurtRow * 3 + 1, hurtRow * 3 + 2 );
	}

	// Kohaku pull ("lấy thuốc từ túi") animation: 3 cols x 4 rows, only use col 0 row 0 (face down).
	if (isKohaku) {
		pull = new Animation( 10, false );
		pull.frames( pullFilm(), 0 ); // row 0 col 0 = face down
	}

		fly = new Animation( 1, true );
		fly.frames( film, row + 0 );

		read = new Animation( 20, false );
		read.frames( film, row + 0 );
		
		if (Dungeon.hero.isAlive())
			idle();
		else
			die();
	}
	
	// Kohaku sheet frames are 96x96 but the hero is drawn at 16x16.
	// Keep the sprite logically 16x16 so worldToCamera()/center()/emitters use
	// the true on-screen box; the 96x96 texture region is downsampled onto the
	// 16x16 quad by the LINEAR texture filter (smarttexture filter set elsewhere).
	@Override
	public void frame( RectF rect ) {
		super.frame( rect );
		if (Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.DUELIST) {
			this.width = LOGICAL_SIZE;
			this.height = LOGICAL_SIZE;
			this.scale.set( 1f, 1f );
			updateVertices();
		}
	}
	
	@Override
	public void place( int p ) {
		super.place( p );
		if (Game.scene() instanceof GameScene) Camera.main.panFollow(this, 5f);
	}

	@Override
	public void move( int from, int to ) {
		busyMoving = true;
		idleGrace = -1;
		ensureNormalSheet();

		// Kohaku: use dynamic speed from settings
		if (Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.DUELIST) {
			turnTo( from, to );
			play( run );
			// Speed 1=slow(0.32s) to 10=fast(0.08s), default 5=0.18s
			float interval = Math.max(0.08f, 0.36f - SPDSettings.charSpeed() * 0.036f);
			motion = new com.watabou.noosa.tweeners.PosTweener( this, worldToCamera( to ), interval );
			motion.listener = this;
			parent.add( motion );
			isMoving = true;
			if (visible && Dungeon.level.water[from] && !ch.flying) {
				GameScene.ripple( from );
			}
		} else {
			super.move( from, to );
		}

		if (ch != null && ch.flying) {
			play( fly );
		}
		Camera.main.panFollow(this, 20f);
	}

	// Kohaku uses a real 4-direction sprite sheet, so the base system's
	// horizontal mirroring must be disabled. turnTo() flips the sprite when
	// moving left/right, which mirrors the LEFT art into a right-facing
	// silhouette (looks like walking backwards) and overrides our facing rows.
	@Override
	public void turnTo( int from, int to ) {
		flipHorizontal = false;

		// Kohaku: face the target based on world direction. The base system only
		// mirrors horizontally (flipHorizontal), which doesn't map to the
		// 4-direction sheet. This covers thrown items, potions, spells, opening,
		// etc. (CharSprite.attack/zap/operate all call turnTo with ch.pos -> cell),
		// just like melee attacks, so the hero turns to face the target.
		if (Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.DUELIST
				&& Dungeon.level != null && from >= 0 && to >= 0 && from != to) {
			int width = Dungeon.level.width();
			int fx = from % width;
			int fy = from / width;
			int tx = to % width;
			int ty = to / width;
			if (tx > fx) {
				Dungeon.hero.facing = 2; // RIGHT
			} else if (tx < fx) {
				Dungeon.hero.facing = 1; // LEFT
			} else if (ty < fy) {
				Dungeon.hero.facing = 3; // UP
			} else if (ty > fy) {
				Dungeon.hero.facing = 0; // DOWN
			} else {
				return;
			}
			updateFacing();
		}
	}

	@Override
	public synchronized void attack( int cell, Callback callback ) {
		ensureNormalSheet();
		onAttackAction(); // track turns for monini de-transform
		super.attack( cell, callback );
	}

	@Override
	public synchronized void zap( int cell, Callback callback ) {
		ensureNormalSheet();
		super.zap( cell, callback );
	}

	@Override
	public synchronized void operate( int cell, Callback callback ) {
		if (drinkTimer < 0) ensureNormalSheet();
		super.operate( cell, callback );
	}

	@Override
	public void idle() {
		// Don't restore normal sheet during drink — drink phases intentionally
		// use different textures (held, heldup, drinking, good, hurt).
		if (drinkTimer < 0) ensureNormalSheet();
		if (busyMoving) {
			idleGrace = 0.12f;
			return;
		}
		idleGrace = -1;
		super.idle();
		if (ch != null && ch.flying) {
			play( fly );
		}
	}

	@Override
	public void jump( int from, int to, float height, float duration,  Callback callback ) {
		super.jump( from, to, height, duration, callback );
		play( fly );
		Camera.main.panFollow(this, 20f);
	}

	public synchronized void read() {
		animCallback = new Callback() {
			@Override
			public void call() {
				idle();
				ch.onOperateComplete();
			}
		};
		play( read );
	}

	@Override
	public synchronized void onComplete( MovieClip.Animation anim ) {
		// When any combat/action animation finishes, clear the busy flag so the
		// subsequent ready() -> idle() is not short-circuited (which would otherwise
		// leave the sprite stuck on the last frame of the animation).
		if (anim == attack || anim == zap || anim == operate) {
			busyMoving = false;
			idleGrace = -1;
		}
		super.onComplete( anim );
		// The base CharSprite only auto-returns to idle for attack/operate.
		// zap isn't handled there, so force the idle return explicitly.
		if (anim == zap) {
			idle();
		}
		// Hurt ("bị đánh") flash done -> restore the normal hero sheet.
		if (anim == hurt) {
			hurtTimer = -1;
			if (drinkTimer < 0) {
				// Restore the normal kohaku texture. No need to rebuild animations
				// (the cached kohakuFilm is always valid; just re-face correctly).
				texture( hurtOriginalSheet );
				lastFacing = -1; // force updateFacing to rebuild frames
				updateFacing();
				idle();
			}
		}
	}

	@Override
	public void bloodBurstA(PointF from, int damage) {
		// Kohaku: play the "bị đánh" (hurt) animation instead of a blood splash.
		if (Dungeon.hero == null || Dungeon.hero.heroClass != HeroClass.DUELIST
				|| hurt == null || curAnim == die) return;

		// turn to face the attacker (from), then flash the hurt animation
		PointF c = center();
		if (from != null) {
			float dx = from.x - c.x;
			float dy = from.y - c.y;
			if (Math.abs(dx) > Math.abs(dy)) {
				Dungeon.hero.facing = (dx > 0) ? 2 : 1; // RIGHT / LEFT
			} else if (Math.abs(dy) > 1e-6f) {
				Dungeon.hero.facing = (dy > 0) ? 0 : 3; // DOWN / UP
			}
			updateFacing();
		}

		if (drinkTimer < 0) {
			hurtOriginalSheet = Dungeon.hero.heroClass.spritesheet();
			// Ensure the hurt animation exists (film is static/cached).
			if (hurt == null || hurt.frames == null || hurt.frames.length == 0) {
				int hr = Dungeon.hero.facing;
				hurt = new Animation( 15, false );
				hurt.frames( hurtFilm(), hr * 3, hr * 3 + 1, hr * 3 + 2 );
			}
			hurtTimer = hurt.frames.length == 0 ? 0 : hurt.frames.length * hurt.delay;
			texture( HURT_PATH );
			// Force frame to the first hurt frame (facing row, col 0) so the
			// full sheet is never accidentally displayed.
			frame( hurtFilm().get( Dungeon.hero.facing * 3 ) );
			play( hurt );
		}
	}

	@Override
	public void update() {
		sleeping = ch.isAlive() && ((Hero)ch).resting;

		// CRITICAL: sync texture BEFORE super.update() so that when
		// MovieClip.updateAnimation() calls frame(), the GL texture matches
		// the film's UV indices. Without this, a state change (debuff/monini)
		// sets a new texture but the old film's UVs are still active → full sheet.
		if (Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.DUELIST
				&& drinkTimer < 0 && curAnim != hurt) {
			SmartTexture target;
			if (transformState == TransformState.MONINI) {
				target = TextureCache.get("sprites/kohaku_monini/walk.png");
			} else if (isDebuffed()) {
				target = TextureCache.get(DIZZY_PATH);
			} else {
				target = TextureCache.get(Dungeon.hero.heroClass.spritesheet());
			}
			if (texture != target) {
				texture(target);
			}
		}

		super.update();

		// Kohaku drink timer
		if (drinkTimer >= 0) {
			drinkTimer += Game.elapsed;
			float total = drinkDuration;
			float pullEnd   = total * 0.10f;  // lấy thuốc từ túi (new)
			float heldEnd   = total * 0.22f;  // cầm thuốc
			float heldUpEnd = total * 0.38f;  // đưa thuốc ra giữa
			float drinkEnd  = total * 0.85f;  // uống
			String exotic = drinkExotic ? "exotic_" : "";

			if (drinkTimer >= total) {
				drinkTimer = -1;
				drinkPhase = -1;
				drinkHurt = false;
				drinkEffectCallback = null;
				drinkEffectFired = false;
				if (operate != null) {
					operate.delay = 1f / ATTACK_FRAMERATE;
				}
				// Restore normal kohaku sheet. Use ensureNormalSheet() to
				// guarantee the GL texture matches kohakuFilm before idle()
				// sets frame UVs — prevents full-sheet display (noise).
				ensureNormalSheet();
				lastFacing = -1;
				updateFacing();
				// Force-replay idle: texture() just reset frame to the full
				// sheet (0,0,1,1). If curAnim is already idle (looped), plain
				// play(idle) short-circuits and never calls frame(), leaving
				// the full-sheet UV stuck on screen. force=true guarantees
				// frame(idle.frames[0]) actually runs.
				idleGrace = -1;
				busyMoving = false;
				play( idle, true );
				if (ch != null && ch.flying) play( fly );
			} else {
				String tex = null;
				if (drinkTimer < pullEnd) {
					drinkPhase = 0;
					// "Lấy thuốc từ túi" — single frame, face down
					Dungeon.hero.facing = 0;
					tex = PULL_PATH;
				} else if (drinkTimer < heldEnd) {
					drinkPhase = 1;
					tex = DRINK_PATH + "held_" + exotic + drinkColor + ".png";
				} else if (drinkTimer < heldUpEnd) {
					drinkPhase = 2;
					tex = DRINK_PATH + "heldup_" + exotic + drinkColor + ".png";
				} else if (drinkTimer < drinkEnd) {
					drinkPhase = 3;
					tex = DRINK_PATH + "drinking.png";
				} else {
					drinkPhase = 4;
					// fired once when the character finishes drinking (starts "good")
					if (!drinkEffectFired && drinkEffectCallback != null) {
						drinkEffectFired = true;
						drinkEffectCallback.call();
					}
					if (drinkBeneficial) {
						tex = DRINK_PATH + "good.png";
					} else if (!drinkHurt) {
						// harmful effect: show the "bị đánh" hurt frame facing DOWN.
						drinkHurt = true;
						Dungeon.hero.facing = 0; // DOWN
						texture( HURT_PATH );
						frame( hurtFilm().get( 0 ) ); // row 0 col 0 = DOWN
					}
				}

				if (tex != null && !drinkHurt) {
					SmartTexture drinkTex = TextureCache.get(tex);
					drinkTex.filter(Texture.LINEAR, Texture.LINEAR);
					texture(drinkTex);
					scale.set(1f, 1f);
					origin.set(0f, 0f);
				}

				// Squash & stretch during drink phase, zooming from the sprite center
				if (drinkPhase == 3 && !drinkHurt) {
					origin.set(LOGICAL_SIZE / 2f, LOGICAL_SIZE / 2f);
					float cp = ((drinkTimer - heldUpEnd) / (drinkEnd - heldUpEnd)) * 3f;
					int cyc = Math.min((int) cp, 2);
					float ct = cp - cyc;
					float intensity = 1f / (1f + cyc);
					float sx = 1f - intensity * 0.10f * (float) Math.sin(ct * Math.PI * 2);
					float sy = 1f + intensity * 0.10f * (float) Math.sin(ct * Math.PI * 2 + (float) Math.PI);
					scale.set(sx, sy);
				}
			}
		}

		if (idleGrace > 0) {
			idleGrace -= Game.elapsed;
			if (idleGrace <= 0) {
				idleGrace = -1;
				busyMoving = false;

				// Kohaku: apply rest facing after diagonal movement stops
				if (ch instanceof Hero && ((Hero) ch).restFacing != -1) {
					((Hero) ch).facing = ((Hero) ch).restFacing;
					((Hero) ch).restFacing = -1;
					lastFacing = -1; // force rebuild
					updateFacing();
				}

				super.idle();
				if (ch != null && ch.flying) {
					play( fly );
				}
			}
		}

		// Hurt flash fallback: if the hurt animation was interrupted, restore after
		// a brief moment anyway.
		if (hurtTimer > 0) {
			hurtTimer -= Game.elapsed;
			if (hurtTimer <= 0) {
				hurtTimer = -1;
				if (drinkTimer < 0 && curAnim == hurt) {
					texture( hurtOriginalSheet );
					lastFacing = -1;
					updateFacing();
					idle();
				}
			}
		}

		// === Monini transform state machine ===
		if (transformState == TransformState.TRANSFORMING) {
			transformTimer += Game.elapsed;
			// Phase 1: kohaku3 row3 → row2 (first 0.25s)
			// Phase 2: kougeki_henge row1 → row2 → row3 (remaining)
			float phase1 = 0.25f;
			String tex;
			if (transformTimer < phase1) {
				tex = "sprites/kohaku_transform/kohaku3.png";
			} else {
				tex = "sprites/kohaku_transform/kougeki_henge.png";
			}
			SmartTexture t = TextureCache.get(tex);
			t.filter(Texture.LINEAR, Texture.LINEAR);
			texture(t);
			scale.set(1f, 1f);
			// Select row based on progress
			TextureFilm tf = new TextureFilm(t, KOHAKU_FRAME_WIDTH, KOHAKU_FRAME_HEIGHT);
			int frameIdx;
			if (transformTimer < phase1) {
				// kohaku3: row3→row2
				frameIdx = (transformTimer < phase1 / 2) ? 3 * 3 : 2 * 3; // row3 col0 or row2 col0
			} else {
				float p2 = (transformTimer - phase1) / (TRANSFORM_DURATION - phase1);
				int r = Math.min((int)(p2 * 3), 2); // row 0,1,2 of kougeki_henge
				frameIdx = r * 3; // col 0
			}
			frame(tf.get(frameIdx));
			if (transformTimer >= TRANSFORM_DURATION) {
				transformState = TransformState.MONINI;
				transformTimer = -1;
				lastFacing = -1;
				updateFacing();
				idle();
			}
		} else if (transformState == TransformState.DE_TRANSFORMING) {
			transformTimer += Game.elapsed;
			float phase1 = 0.25f;
			String tex;
			if (transformTimer < phase1) {
				tex = "sprites/kohaku_transform/kougeki_henge2.png";
			} else {
				tex = "sprites/kohaku_transform/kohaku3.png";
			}
			SmartTexture t = TextureCache.get(tex);
			t.filter(Texture.LINEAR, Texture.LINEAR);
			texture(t);
			scale.set(1f, 1f);
			TextureFilm tf = new TextureFilm(t, KOHAKU_FRAME_WIDTH, KOHAKU_FRAME_HEIGHT);
			int frameIdx;
			if (transformTimer < phase1) {
				// kougeki_henge2: row1→row2→row3
				float p1 = transformTimer / phase1;
				int r = Math.min((int)(p1 * 3), 2);
				frameIdx = r * 3;
			} else {
				// kohaku3: row2→row3
				frameIdx = (transformTimer < phase1 + 0.125f) ? 2 * 3 : 3 * 3;
			}
			frame(tf.get(frameIdx));
			if (transformTimer >= TRANSFORM_DURATION) {
				transformState = TransformState.NORMAL;
				transformTimer = -1;
				ensureNormalSheet();
				lastFacing = -1;
				updateFacing();
				idle();
			}
		} else if (transformState == TransformState.MONINI) {
			// Check if speed effect ended → reverse transform
			if (!isSpeedEffect()) {
				reverseTransform();
			} else {
				// Track turns: increment when idle
				turnsSinceAttack++;
				// After 2 turns no attack, stay monini (no action needed)
			}
		} else if (transformState == TransformState.NORMAL) {
			// Check if speed effect started → forward transform
			if (isSpeedEffect() && drinkTimer < 0) {
				startTransform();
			}
		}
	}
	
	public void sprint( float speed ) {
		run.delay = 1f / speed / RUN_FRAMERATE;
	}

	/**
	 * Start Kohaku drink animation by swapping sprite texture.
	 * No new Image added to scene — just retextures the hero sprite.
	 */
	public void startDrink(String color, boolean exotic, boolean beneficial, Callback effect) {
		if (Dungeon.hero.heroClass != HeroClass.DUELIST) return;
		drinkOriginalSheet = Dungeon.hero.heroClass.spritesheet();
		drinkDuration = Math.max(0.5f, Math.min(5.0f, SPDSettings.drinkDuration()));
		drinkColor = color;
		drinkExotic = exotic;
		drinkBeneficial = beneficial;
		drinkHurt = false;
		drinkEffectCallback = effect;
		drinkEffectFired = false;
		drinkTimer = 0;
		drinkPhase = -1;
		// operate now has 6 frames (second half of attack). Stretch its delay so it
		// completes once the whole drink is done, keeping the hero busy throughout.
		if (operate != null) {
			operate.delay = drinkDuration / operate.frames.length;
		}
	}

	public void updateFacing() {
		if (Dungeon.hero == null) return;
		
		int newFacing = Dungeon.hero.facing;
		boolean currentDebuffed = isDebuffed();
		
		// Rebuild if direction OR debuff/monini state changed
		if (newFacing == lastFacing && currentDebuffed == lastDebuffed
				&& transformState == lastTransformState) return;
		lastFacing = newFacing;
		lastDebuffed = currentDebuffed;
		lastTransformState = transformState;

		TextureFilm film;
		if (transformState == TransformState.MONINI) {
			film = moniniFilm();
		} else {
			film = currentDebuffed ? dizzyFilm() : kohakuFilm();
		}
		int colsPerRow = (transformState == TransformState.MONINI || currentDebuffed) ? 8 : 21;
		int row = newFacing * colsPerRow;

		// Mutate existing Animation objects in place.
		idle.frames( film, row + 0 );
		if (currentDebuffed || transformState == TransformState.MONINI) {
			run.frames( film, row + 0, row + 1, row + 2, row + 3,
					row + 4, row + 5, row + 6, row + 7 );
		} else {
			run.frames( film, row + 1, row + 2, row + 3, row + 4,
					row + 5, row + 6, row + 7, row + 8 );
		}
		die.frames( film, row + 1 );

		// Attack/operate always use kohakuFilm (dizzy has no attack frames)
		int attackRow = newFacing * 21;
		attack.frames( kohakuFilm(), attackRow + 9, attackRow + 10, attackRow + 11,
				attackRow + 12, attackRow + 13, attackRow + 14,
				attackRow + 15, attackRow + 16, attackRow + 17,
				attackRow + 18, attackRow + 19, attackRow + 20 );
		zap = attack.clone();
		operate.frames( kohakuFilm(), attackRow + 15, attackRow + 16, attackRow + 17,
				attackRow + 18, attackRow + 19, attackRow + 20 );
		fly.frames( film, row + 1 );
		read.frames( film, row + 1 );

		// Hurt sheet uses row = facing (0=DOWN,1=LEFT,2=RIGHT,3=UP), 3 cols each.
		if (hurt != null) {
			int hurtRow = newFacing;
			hurt.frames( hurtFilm(), hurtRow * 3, hurtRow * 3 + 1, hurtRow * 3 + 2 );
		}

		// Sync texture with current film state
		SmartTexture targetTex;
		if (transformState == TransformState.MONINI) {
			targetTex = TextureCache.get( "sprites/kohaku_monini/walk.png" );
		} else if (currentDebuffed) {
			targetTex = TextureCache.get( DIZZY_PATH );
		} else {
			targetTex = TextureCache.get( Dungeon.hero.heroClass.spritesheet() );
		}
		if (texture != targetTex) {
			texture( targetTex );
		}
	}
	
	public static TextureFilm tiers() {
		if (tiers == null) {
			SmartTexture texture = TextureCache.get( Assets.Sprites.ROGUE );
			tiers = new TextureFilm( texture, texture.width, FRAME_HEIGHT );
		}
		
		return tiers;
	}

	/** Cached kohaku sheet film (21 cols x 4 rows of 96x96). Created once, reused. */
	public static TextureFilm kohakuFilm() {
		if (kohakuFilm_ == null) {
			SmartTexture kt = TextureCache.get( "sprites/kohaku.png" );
			kt.filter( Texture.LINEAR, Texture.LINEAR );
			kohakuFilm_ = new TextureFilm( kt, KOHAKU_FRAME_WIDTH, KOHAKU_FRAME_HEIGHT );
		}
		return kohakuFilm_;
	}

	/** Cached dizzy sheet film (8 cols x 4 rows of 96x96). Used when debuffed. */
	public static TextureFilm dizzyFilm() {
		if (dizzyFilm_ == null) {
			SmartTexture dt = TextureCache.get( "sprites/kohaku_dizzy.png" );
			dt.filter( Texture.LINEAR, Texture.LINEAR );
			dizzyFilm_ = new TextureFilm( dt, KOHAKU_FRAME_WIDTH, KOHAKU_FRAME_HEIGHT );
		}
		return dizzyFilm_;
	}

	/** Cached monini walk sheet film (8 cols x 4 rows of 96x96). Used during speed effects. */
	public static TextureFilm moniniFilm() {
		if (moniniFilm_ == null) {
			SmartTexture mt = TextureCache.get( "sprites/kohaku_monini/walk.png" );
			mt.filter( Texture.LINEAR, Texture.LINEAR );
			moniniFilm_ = new TextureFilm( mt, KOHAKU_FRAME_WIDTH, KOHAKU_FRAME_HEIGHT );
		}
		return moniniFilm_;
	}

	/** Cached hurt film (3x4 grid of 96x96). Created once, reused for all facing. */
	public static TextureFilm hurtFilm() {
		if (hurtFilm == null) {
			SmartTexture ht = TextureCache.get( HURT_PATH );
			ht.filter( Texture.LINEAR, Texture.LINEAR );
			hurtFilm = new TextureFilm( ht, KOHAKU_FRAME_WIDTH, KOHAKU_FRAME_HEIGHT );
		}
		return hurtFilm;
	}

	/** Cached pull sheet film (3x4 grid of 96x96). Created once, reused. */
	public static TextureFilm pullFilm() {
		if (pullFilm_ == null) {
			SmartTexture pt = TextureCache.get( PULL_PATH );
			pt.filter( Texture.LINEAR, Texture.LINEAR );
			pullFilm_ = new TextureFilm( pt, KOHAKU_FRAME_WIDTH, KOHAKU_FRAME_HEIGHT );
		}
		return pullFilm_;
	}

	public static Image avatar( Hero hero ){
		if (hero.buff(HeroDisguise.class) != null){
			return avatar(hero.buff(HeroDisguise.class).getDisguise(), hero.tier());
		} else {
			return avatar(hero.heroClass, hero.tier());
		}
	}
	
	public static Image avatar( HeroClass cl, int armorTier ) {
		
		RectF patch = tiers().get( armorTier );
		Image avatar = new Image( cl.spritesheet() );
		RectF frame = avatar.texture.uvRect( 1, 0, FRAME_WIDTH, FRAME_HEIGHT );
		frame.shift( patch.left, patch.top );
		avatar.frame( frame );
		
		return avatar;
	}
}
