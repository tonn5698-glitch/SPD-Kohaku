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
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
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
	private static TextureFilm dizzyAttackFilm_; // cached dizzy attack sheet (4 cols x 4 rows)
	private static TextureFilm moniniFilm_; // cached monini walk sheet
	private static TextureFilm hurtFilm;  // cached: created once, reused
	private static TextureFilm pullFilm_; // cached pull sheet
	private static Animation hurt;        // rebuilt per-facing from cached film

	private Animation fly;
	private Animation read;
	private Animation pull;
	private Animation die_;  // Kohaku death animation (3 sheets × 4 rows)
	private int lastFacing = -1;
	private boolean lastDebuffed = false;
	private TransformState lastTransformState = TransformState.NORMAL;

	// Death animation state
	private float dieTimer = -1;  // -1 = not dying
	private int dieSheet = 0;     // which sheet (0=DMZ7, 1=DMZ8, 2=DMZ9)
	private int dieRow = 0;       // current row (0-3)
	private Callback dieCallback; // called when death animation finishes

	// Kohaku walk-loop: Hero.ready() calls sprite.idle() between every tile,
	// which resets curFrame and makes the walk cycle restart each tile.
	// While busyMoving we instead keep the run loop alive, and only switch to
	// a real idle if no new move arrives within the grace period.
	private boolean busyMoving = false;
	private float idleGrace = -1;

	// Kohaku drink animation timer
	private static final String DRINK_PATH = "sprites/kohaku_drink/";
	private static final String FOOD_PATH = "sprites/kohaku_food/";
	private static final String FOOD_DOWN_PATH = "sprites/kohaku_food_down/";
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

	// Food animation (similar to drink but different textures/phases)
	private float foodTimer = -1;  // -1 = not eating
	private float foodDuration;
	private String foodOriginalSheet;
	private int foodPhase = -1; // 0=pull, 1=held, 2=heldUp, 3=good/hurt
	private String foodName;     // e.g. "ration", "meat"
	private boolean foodHarmful; // true for raw meat (hurt instead of good)
	private boolean foodSkipHeldUp; // true for bland_chunks (skip held up)
	private boolean foodSkipGood; // true for light food (no good phase)
	private boolean foodHurt;
	private Callback foodEffectCallback;
	private boolean foodEffectFired;

	// Kohaku hurt ("bị đánh") flash when taking damage
	private static final String HURT_PATH = "sprites/kohaku_hurt.png";
	private static final String PULL_PATH = "sprites/kohaku_pull_down.png";
	private static final String DIZZY_PATH = "sprites/kohaku_dizzy.png";
	private static final String[] DIE_PATHS = {
		"sprites/kohaku_die/$DMZ7.png",
		"sprites/kohaku_die/$DMZ8.png",
		"sprites/kohaku_die/$DMZ9.png"
	};
	private float hurtTimer = -1;  // -1 = not hurt
	private String hurtOriginalSheet;

	// Monini transform system
	private enum TransformState { NORMAL, TRANSFORMING, MONINI, DE_TRANSFORMING }
	private TransformState transformState = TransformState.NORMAL;
	private float transformTimer = -1;
	private int turnsSinceAttack = 0;
	

// Ensure the GL texture matches the current film state.
	// After texture(), force re-apply the current animation so frame UVs
	// are correct — otherwise the sprite shows full-sheet or stretched.
	private void ensureNormalSheet() {
		if (Dungeon.hero == null || Dungeon.hero.heroClass != HeroClass.DUELIST) return;
		// Don't restore during drink/food — they intentionally use different textures
		if (drinkTimer >= 0 || foodTimer >= 0) return;
		// Don't fight the transform state machine — TRANSFORMING/DE_TRANSFORMING
		// set texture+frame directly every tick themselves.
		if (transformState == TransformState.TRANSFORMING || transformState == TransformState.DE_TRANSFORMING) return;

		// Determine which texture should be bound for current state
		String normal = Dungeon.hero.heroClass.spritesheet();
		SmartTexture targetTex;
		if (transformState == TransformState.MONINI) {
			targetTex = TextureCache.get("sprites/kohaku_monini/walk.png");
		} else if (isDebuffed()) {
			targetTex = TextureCache.get(DIZZY_PATH);
		} else {
			targetTex = TextureCache.get(normal);
		}

		if (texture != targetTex) {
			hurtTimer = -1;
			texture(targetTex);
			lastFacing = -1;
			updateFacing();
			// texture() reset frame to full-sheet UV (0,0,1,1).
			// updateFacing() rebuilds Animation arrays but doesn't re-apply frame.
			// Force re-apply so the correct UV is on screen immediately.
			if (curAnim != null && curAnim.frames != null && curAnim.frames.length > 0) {
				play(curAnim, true);
			} else {
				play(idle, true);
			}
		}
	}

	// Check if hero has any active debuff (for dizzy animation).
	private static boolean isDebuffed() {
		if (Dungeon.hero == null) return false;
		// Low HP also triggers dizzy
		if (Dungeon.hero.HP > 0 && Dungeon.hero.HP < Dungeon.hero.HT * 0.15f) return true;
		// Standing in toxic gas triggers dizzy
		if (Dungeon.level != null && Dungeon.hero.pos >= 0
				&& Dungeon.level.blobs.containsKey( com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas.class )
				&& Dungeon.level.blobs.get( com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas.class ).volume > 0
				&& Blob.volumeAt( Dungeon.hero.pos, com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas.class ) > 0) return true;
		for (com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff b : Dungeon.hero.buffs()) {
			if (b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Doom
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots) {
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
		// Same fix as ensureNormalSheet: texture() resets frame to full-sheet,
		// must force re-apply so the correct UV is on screen.
		if (curAnim != null && curAnim.frames != null && curAnim.frames.length > 0) {
			play( curAnim, true );
		} else {
			play( idle, true );
		}
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

		// Dizzy and monini both have 8 cols/row, kohaku has 21. Use correct stride.
		boolean shortSheet = debuffed || transformState == TransformState.MONINI;
		int colsPerRow = shortSheet ? 8 : 21;
		int row = facing * colsPerRow;

		// Idle: normal kohaku uses frame 0; dizzy uses frame 1 (standing still,
		// not the "lifted foot" frame 0 which is the start of the walk cycle).
		idle = new Animation( 1, true );
		if (debuffed) {
			// dizzy: frame 1 = idle standing
			idle.frames( film, row + 1 );
		} else {
			idle.frames( film, row + 0 );
		}

		// Walk: 8 frames at 30fps.
		// Normal sheet reserves col0 for a dedicated idle pose (cols1-8=walk).
		// Dizzy/monini sheets are only 8 cols total with NO separate idle
		// frame — col0 IS the first walk frame. Requesting row+8 there
		// overflows into the next facing row (wrong sprite content).
		run = new Animation( RUN_FRAMERATE, true );
		if (shortSheet) {
			run.frames( film, row + 0, row + 1, row + 2, row + 3,
					row + 4, row + 5, row + 6, row + 7 );
		} else {
			run.frames( film, row + 1, row + 2, row + 3, row + 4,
					row + 5, row + 6, row + 7, row + 8 );
		}

		// Die: use idle
		die = new Animation( 20, false );
		die.frames( film, row + 0 );

		// Attack: use dizzy attack film when debuffed, kohakuFilm otherwise
		attack = new Animation( ATTACK_FRAMERATE, false );
		if (debuffed) {
			// Dizzy attack: 4 cols x 4 rows of 96x96
			int dizzyAttackRow = facing * 4;
			attack.frames( dizzyAttackFilm(), dizzyAttackRow, dizzyAttackRow + 1,
					dizzyAttackRow + 2, dizzyAttackRow + 3 );
		} else {
			int attackRow = facing * 21;
			attack.frames( kohakuFilm(), attackRow + 9, attackRow + 10, attackRow + 11,
					attackRow + 12, attackRow + 13, attackRow + 14,
					attackRow + 15, attackRow + 16, attackRow + 17,
					attackRow + 18, attackRow + 19, attackRow + 20 );
		}

		zap = attack.clone();

		// Kohaku: operate
		operate = new Animation( ATTACK_FRAMERATE, false );
		if (debuffed) {
			int dizzyAttackRow = facing * 4;
			operate.frames( dizzyAttackFilm(), dizzyAttackRow, dizzyAttackRow + 1,
					dizzyAttackRow + 2, dizzyAttackRow + 3 );
		} else {
			int attackRow = facing * 21;
			operate.frames( kohakuFilm(), attackRow + 15, attackRow + 16, attackRow + 17,
					attackRow + 18, attackRow + 19, attackRow + 20 );
		}

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
				// Restore the normal kohaku texture after hurt flash.
				// play(idle, true) forces frame re-apply so UV matches texture.
				texture( hurtOriginalSheet );
				lastFacing = -1;
				updateFacing();
				play( idle, true );
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
			// Save the CURRENT sheet (dizzy or normal) so onComplete(hurt) restores correctly
			if (isDebuffed()) {
				hurtOriginalSheet = DIZZY_PATH;
			} else if (transformState == TransformState.MONINI) {
				hurtOriginalSheet = "sprites/kohaku_monini/walk.png";
			} else {
				hurtOriginalSheet = Dungeon.hero.heroClass.spritesheet();
			}
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
		// the film's UV indices. Delegates to ensureNormalSheet() so the
		// frame()-force fix lives in one place instead of being duplicated
		// (a duplicate here previously reintroduced the full-sheet bug,
		// since it swapped texture without ever forcing frame() back).
		if (Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.DUELIST
				&& drinkTimer < 0 && foodTimer < 0 && curAnim != hurt) {
			ensureNormalSheet();
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
						// Show heal splash for beneficial drink (waterskin etc.)
						if (!drinkEffectFired && ch != null && ch.isAlive()) {
						}
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

		// Kohaku food timer
		if (foodTimer >= 0) {
			foodTimer += Game.elapsed;
			float total = foodDuration;
			float pullEnd    = total * 0.10f;
			float heldEnd    = total * 0.30f;
			float heldUpEnd  = foodSkipHeldUp ? heldEnd : total * 0.60f;
			float goodEnd    = foodSkipHeldUp ? total * 0.70f : total * 0.80f;

			if (foodTimer >= total) {
				foodTimer = -1;
				foodPhase = -1;
				foodHurt = false;
				foodEffectCallback = null;
				foodEffectFired = false;
				if (operate != null) {
					operate.delay = 1f / ATTACK_FRAMERATE;
				}
				ensureNormalSheet();
				lastFacing = -1;
				updateFacing();
				play( idle, true );
			} else {
				String tex = null;
				if (foodTimer < pullEnd) {
					foodPhase = 0;
					Dungeon.hero.facing = 0;
					tex = PULL_PATH;
				} else if (foodTimer < heldEnd) {
					foodPhase = 1;
					tex = FOOD_PATH + foodName + ".png";
				} else if (foodTimer < heldUpEnd) {
					foodPhase = 2;
					tex = FOOD_DOWN_PATH + foodName + ".png";
				} else {
					foodPhase = 3;
					if (!foodEffectFired && foodEffectCallback != null) {
						foodEffectFired = true;
						foodEffectCallback.call();
					}
					if (foodHarmful && !foodHurt) {
						// Harmful: show hurt face down
						foodHurt = true;
						Dungeon.hero.facing = 0;
						texture( HURT_PATH );
						frame( hurtFilm().get( 0 ) );
					} else if (!foodSkipGood) {
						// Normal food: show good
						tex = DRINK_PATH + "good.png";
						// Show heal splash for food
						if (!foodEffectFired && ch != null && ch.isAlive()) {
						}
					}
					// Light food (foodSkipGood=true): no tex → stays on held up → idle
				}

				if (tex != null && !foodHurt) {
					SmartTexture foodTex = TextureCache.get(tex);
					foodTex.filter(Texture.LINEAR, Texture.LINEAR);
					texture(foodTex);
					scale.set(1f, 1f);
					origin.set(0f, 0f);
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
					play( idle, true );
				}
			}
		}

		// === Monini transform state machine ===
		if (transformState == TransformState.TRANSFORMING) {
			transformTimer += Game.elapsed;
			float dur = SPDSettings.transformDuration();
			float p = Math.min(transformTimer / dur, 1f); // 0..1 progress
			// Sequence: kohaku2(25%) → kohaku3 row3(12.5%) → row2(12.5%) → henge row1(25%) → henge rows2-3(25%)
			String tex;
			int frameIdx = 0;
			if (p < 0.25f) {
				// Phase A: kohaku2 first frame (dizzy idle)
				tex = DIZZY_PATH;
				SmartTexture t = TextureCache.get(tex);
				t.filter(Texture.LINEAR, Texture.LINEAR);
				texture(t);
				scale.set(1f, 1f);
				TextureFilm tf = new TextureFilm(t, KOHAKU_FRAME_WIDTH, KOHAKU_FRAME_HEIGHT);
				frame(tf.get(0)); // row 0 col 0
			} else if (p < 0.375f) {
				// Phase B: kohaku3 row 3 (last row)
				tex = "sprites/kohaku_transform/kohaku3.png";
				SmartTexture t = TextureCache.get(tex);
				t.filter(Texture.LINEAR, Texture.LINEAR);
				texture(t);
				scale.set(1f, 1f);
				TextureFilm tf = new TextureFilm(t, KOHAKU_FRAME_WIDTH, KOHAKU_FRAME_HEIGHT);
				frame(tf.get(3 * 3)); // row 3 col 0
			} else if (p < 0.5f) {
				// Phase C: kohaku3 row 2
				tex = "sprites/kohaku_transform/kohaku3.png";
				SmartTexture t = TextureCache.get(tex);
				t.filter(Texture.LINEAR, Texture.LINEAR);
				texture(t);
				scale.set(1f, 1f);
				TextureFilm tf = new TextureFilm(t, KOHAKU_FRAME_WIDTH, KOHAKU_FRAME_HEIGHT);
				frame(tf.get(2 * 3)); // row 2 col 0
			} else if (p < 0.75f) {
				// Phase D: kougeki_henge row 1
				tex = "sprites/kohaku_transform/kougeki_henge.png";
				SmartTexture t = TextureCache.get(tex);
				t.filter(Texture.LINEAR, Texture.LINEAR);
				texture(t);
				scale.set(1f, 1f);
				TextureFilm tf = new TextureFilm(t, KOHAKU_FRAME_WIDTH, KOHAKU_FRAME_HEIGHT);
				frame(tf.get(1 * 3)); // row 1 col 0
			} else {
				// Phase E: kougeki_henge rows 2-3 (remaining)
				tex = "sprites/kohaku_transform/kougeki_henge.png";
				SmartTexture t = TextureCache.get(tex);
				t.filter(Texture.LINEAR, Texture.LINEAR);
				texture(t);
				scale.set(1f, 1f);
				TextureFilm tf = new TextureFilm(t, KOHAKU_FRAME_WIDTH, KOHAKU_FRAME_HEIGHT);
				float pE = (p - 0.75f) / 0.25f; // 0..1 within phase E
				int r = pE < 0.5f ? 2 : 3; // row 2 or row 3
				frame(tf.get(r * 3)); // col 0
			}
			if (transformTimer >= dur) {
				transformState = TransformState.MONINI;
				transformTimer = -1;
				lastFacing = -1;
				ensureNormalSheet();
				updateFacing();
				play( idle, true );
			}
		} else if (transformState == TransformState.DE_TRANSFORMING) {
			// Reverse of transform TO, using kougeki_henge (NOT henge2)
			transformTimer += Game.elapsed;
			float dur = SPDSettings.transformDuration();
			float p = Math.min(transformTimer / dur, 1f);
			// Reverse: henge rows2-3(25%) → henge row1(25%) → kohaku3 row2(12.5%) → row3(12.5%) → kohaku2(25%)
			String tex;
			if (p < 0.25f) {
				// Phase A: kougeki_henge rows 2-3 (reverse)
				tex = "sprites/kohaku_transform/kougeki_henge.png";
				SmartTexture t = TextureCache.get(tex);
				t.filter(Texture.LINEAR, Texture.LINEAR);
				texture(t);
				scale.set(1f, 1f);
				TextureFilm tf = new TextureFilm(t, KOHAKU_FRAME_WIDTH, KOHAKU_FRAME_HEIGHT);
				int r = p < 0.125f ? 3 : 2; // row 3 then row 2
				frame(tf.get(r * 3));
			} else if (p < 0.5f) {
				// Phase B: kougeki_henge row 1
				tex = "sprites/kohaku_transform/kougeki_henge.png";
				SmartTexture t = TextureCache.get(tex);
				t.filter(Texture.LINEAR, Texture.LINEAR);
				texture(t);
				scale.set(1f, 1f);
				TextureFilm tf = new TextureFilm(t, KOHAKU_FRAME_WIDTH, KOHAKU_FRAME_HEIGHT);
				frame(tf.get(1 * 3)); // row 1 col 0
			} else if (p < 0.625f) {
				// Phase C: kohaku3 row 2
				tex = "sprites/kohaku_transform/kohaku3.png";
				SmartTexture t = TextureCache.get(tex);
				t.filter(Texture.LINEAR, Texture.LINEAR);
				texture(t);
				scale.set(1f, 1f);
				TextureFilm tf = new TextureFilm(t, KOHAKU_FRAME_WIDTH, KOHAKU_FRAME_HEIGHT);
				frame(tf.get(2 * 3)); // row 2 col 0
			} else if (p < 0.75f) {
				// Phase D: kohaku3 row 3
				tex = "sprites/kohaku_transform/kohaku3.png";
				SmartTexture t = TextureCache.get(tex);
				t.filter(Texture.LINEAR, Texture.LINEAR);
				texture(t);
				scale.set(1f, 1f);
				TextureFilm tf = new TextureFilm(t, KOHAKU_FRAME_WIDTH, KOHAKU_FRAME_HEIGHT);
				frame(tf.get(3 * 3)); // row 3 col 0
			} else {
				// Phase E: kohaku2 first frame (dizzy idle)
				tex = DIZZY_PATH;
				SmartTexture t = TextureCache.get(tex);
				t.filter(Texture.LINEAR, Texture.LINEAR);
				texture(t);
				scale.set(1f, 1f);
				TextureFilm tf = new TextureFilm(t, KOHAKU_FRAME_WIDTH, KOHAKU_FRAME_HEIGHT);
				frame(tf.get(0)); // row 0 col 0
			}
			if (transformTimer >= dur) {
				transformState = TransformState.NORMAL;
				transformTimer = -1;
				ensureNormalSheet();
				lastFacing = -1;
				updateFacing();
				play( idle, true );
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

		// === Death animation ===
		if (dieTimer >= 0) {
			dieTimer += Game.elapsed;
			float totalDur = SPDSettings.loseAnimDuration();
			float ROW_DURATION = totalDur / 12f; // 12 rows total
			float sheetDuration = ROW_DURATION * 4; // 4 rows per sheet
			float totalDuration = sheetDuration * 3; // 3 sheets

			if (dieTimer >= totalDuration) {
				// Death animation complete
				dieTimer = -1;
				Callback cb = dieCallback;
				dieCallback = null;
				if (cb != null) cb.call();
			} else {
				int sheet = (int)(dieTimer / sheetDuration);
				float rowProgress = (dieTimer % sheetDuration) / ROW_DURATION;
				int row = Math.min((int)rowProgress, 3);

				if (sheet != dieSheet || row != dieRow) {
					dieSheet = sheet;
					dieRow = row;
					SmartTexture t = TextureCache.get(DIE_PATHS[sheet]);
					t.filter(Texture.LINEAR, Texture.LINEAR);
					texture(t);
					scale.set(1f, 1f);
					TextureFilm tf = new TextureFilm(t, KOHAKU_FRAME_WIDTH, KOHAKU_FRAME_HEIGHT);
					// Set curAnim=null FIRST so updateAnimation() won't override this frame
					curAnim = null;
					frame(tf.get(row * 3)); // col 0 of current row
				}
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
		drinkDuration = Math.max(0.5f, Math.min(5.0f, SPDSettings.actionDuration()));
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

	/**
	 * Start Kohaku food eating animation.
	 * @param foodName e.g. "ration", "meat", "berry"
	 * @param harmful true for raw meat (hurt instead of good)
	 * @param skipHeldUp true for bland_chunks (skip held up phase)
	 * @param skipGood true for light food (no good phase)
	 * @param effect callback when food effect applies
	 */
	public void startFood(String foodName, boolean harmful, boolean skipHeldUp, boolean skipGood, Callback effect) {
		if (Dungeon.hero.heroClass != HeroClass.DUELIST) return;
		foodOriginalSheet = Dungeon.hero.heroClass.spritesheet();
		foodDuration = Math.max(0.5f, Math.min(5.0f, SPDSettings.actionDuration()));
		this.foodName = foodName;
		foodHarmful = harmful;
		this.foodSkipHeldUp = skipHeldUp;
		this.foodSkipGood = skipGood;
		foodHurt = false;
		foodEffectCallback = effect;
		foodEffectFired = false;
		foodTimer = 0;
		foodPhase = -1;
		if (operate != null) {
			operate.delay = foodDuration / operate.frames.length;
		}
	}

	/**
	 * Start death animation. Plays 3 sheets × 4 rows, each row 0.25s.
	 * Total: 3.0s. After completion, calls callback (for death flow).
	 */
	public void startDeath(Callback callback) {
		if (Dungeon.hero.heroClass != HeroClass.DUELIST) return;
		dieCallback = callback;
		dieSheet = 0;
		dieRow = 0;
		dieTimer = 0;
		// Play first frame
		SmartTexture t = TextureCache.get(DIE_PATHS[0]);
		t.filter(Texture.LINEAR, Texture.LINEAR);
		texture(t);
		scale.set(1f, 1f);
		TextureFilm tf = new TextureFilm(t, KOHAKU_FRAME_WIDTH, KOHAKU_FRAME_HEIGHT);
		frame(tf.get(0)); // row 0 col 0
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
		// Dizzy idle = frame 1 (standing), normal idle = frame 0
		if (currentDebuffed) {
			idle.frames( film, row + 1 );
		} else {
			idle.frames( film, row + 0 );
		}
		if (currentDebuffed || transformState == TransformState.MONINI) {
			run.frames( film, row + 0, row + 1, row + 2, row + 3,
					row + 4, row + 5, row + 6, row + 7 );
		} else {
			run.frames( film, row + 1, row + 2, row + 3, row + 4,
					row + 5, row + 6, row + 7, row + 8 );
		}
		die.frames( film, row + 1 );

		// Attack/operate: use dizzy attack film when debuffed, kohakuFilm otherwise
		if (currentDebuffed) {
			// Dizzy attack: 4 cols x 4 rows of 96x96
			int dizzyAttackRow = newFacing * 4;
			attack.frames( dizzyAttackFilm(), dizzyAttackRow, dizzyAttackRow + 1,
					dizzyAttackRow + 2, dizzyAttackRow + 3 );
			zap = attack.clone();
			operate.frames( dizzyAttackFilm(), dizzyAttackRow, dizzyAttackRow + 1,
					dizzyAttackRow + 2, dizzyAttackRow + 3 );
		} else {
			int attackRow = newFacing * 21;
			attack.frames( kohakuFilm(), attackRow + 9, attackRow + 10, attackRow + 11,
					attackRow + 12, attackRow + 13, attackRow + 14,
					attackRow + 15, attackRow + 16, attackRow + 17,
					attackRow + 18, attackRow + 19, attackRow + 20 );
			zap = attack.clone();
			operate.frames( kohakuFilm(), attackRow + 15, attackRow + 16, attackRow + 17,
					attackRow + 18, attackRow + 19, attackRow + 20 );
		}
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
			// Use dizzy attack texture when attacking, dizzy walk texture otherwise
			if (curAnim == attack || curAnim == zap || curAnim == operate) {
				targetTex = TextureCache.get( "sprites/kohaku_dizzy_attack.png" );
			} else {
				targetTex = TextureCache.get( DIZZY_PATH );
			}
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

	/** Cached dizzy attack sheet film (4 cols x 4 rows of 96x96). Row=facing, col=frame. */
	public static TextureFilm dizzyAttackFilm() {
		if (dizzyAttackFilm_ == null) {
			SmartTexture dt = TextureCache.get( "sprites/kohaku_dizzy_attack.png" );
			dt.filter( Texture.LINEAR, Texture.LINEAR );
			dizzyAttackFilm_ = new TextureFilm( dt, KOHAKU_FRAME_WIDTH, KOHAKU_FRAME_HEIGHT );
		}
		return dizzyAttackFilm_;
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
			// Kohaku: use dizzy portrait when debuffed or low HP
			if (hero.heroClass == HeroClass.DUELIST) {
				boolean lowHP = hero.HP > 0 && hero.HP < hero.HT * 0.15f;
				boolean debuffed = false;
				// Check toxic gas
				if (Dungeon.level != null && hero.pos >= 0
						&& Dungeon.level.blobs.containsKey( com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas.class )
						&& Blob.volumeAt( hero.pos, com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas.class ) > 0) {
					debuffed = true;
				}
				if (!debuffed) {
					for (com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff b : hero.buffs()) {
						if (b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison
							|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning
							|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze
							|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo
							|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow
							|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Doom
							|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill
							|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost
							|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion
							|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots) {
							debuffed = true;
							break;
						}
					}
				}
				if (lowHP || debuffed) {
					Image avatar = new Image( "sprites/kohaku_dizzy_portrait.png" );
					avatar.scale.set( FRAME_HEIGHT / (float) KOHAKU_FRAME_HEIGHT );
					return avatar;
				}
			}
			return avatar(hero.heroClass, hero.tier());
		}
	}
	
	public static Image avatar( HeroClass cl, int armorTier ) {

		// Kohaku: use dedicated portrait sprite (96x96 idle face-down),
		// scaled to match standard 12x15 avatar slot.
		if (cl == HeroClass.DUELIST) {
			Image avatar = new Image( "sprites/kohaku_portrait.png" );
			// Scale so visual size matches the standard avatar (~28px tall).
			// frame() already set width=height=96 from the 96x96 texture.
			avatar.scale.set( FRAME_HEIGHT / (float) KOHAKU_FRAME_HEIGHT );
			return avatar;
		}

		RectF patch = tiers().get( armorTier );
		Image avatar = new Image( cl.spritesheet() );
		RectF frame = avatar.texture.uvRect( 1, 0, FRAME_WIDTH, FRAME_HEIGHT );
		frame.shift( patch.left, patch.top );
		avatar.frame( frame );

		return avatar;
	}
}
