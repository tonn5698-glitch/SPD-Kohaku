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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.MirrorImage;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.PointF;

public class MirrorSprite extends MobSprite {
	
	private static final int FRAME_WIDTH	= 12;
	private static final int FRAME_HEIGHT	= 15;
	
	// Kohaku: 96x96 frames, same as HeroSprite
	private static final int KOHAKU_FRAME_WIDTH  = 96;
	private static final int KOHAKU_FRAME_HEIGHT = 96;
	
	private boolean isKohaku = false;
	
	public MirrorSprite() {
		super();
		
		isKohaku = Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.DUELIST;
		
		if (isKohaku) {
			texture( Dungeon.hero.heroClass.spritesheet() );
			updateArmorKohaku( 0 );
		} else {
			texture( Dungeon.hero != null ? Dungeon.hero.heroClass.spritesheet() : HeroClass.WARRIOR.spritesheet() );
			updateArmor( 0 );
		}
		idle();
	}
	
	@Override
	public void link( Char ch ) {
		super.link( ch );
		if (isKohaku) {
			updateArmorKohaku( ((MirrorImage)ch).armTier );
		} else {
			updateArmor();
		}
	}

	@Override
	public void bloodBurstA(PointF from, int damage) {
		//do nothing
	}

	public void updateArmor(){
		updateArmor( ((MirrorImage)ch).armTier );
	}
	
	public void updateArmor( int tier ) {
		TextureFilm film = new TextureFilm( HeroSprite.tiers(), tier, FRAME_WIDTH, FRAME_HEIGHT );
		
		idle = new Animation( 1, true );
		idle.frames( film, 0, 0, 0, 1, 0, 0, 1, 1 );
		
		run = new Animation( 20, true );
		run.frames( film, 2, 3, 4, 5, 6, 7 );
		
		die = new Animation( 20, false );
		die.frames( film, 0 );
		
		attack = new Animation( 15, false );
		attack.frames( film, 13, 14, 15, 0 );
		
		idle();
	}
	
	/** Kohaku mirror: use 96x96 frames from kohaku.png, scale down to match mob size. */
	public void updateArmorKohaku( int tier ) {
		TextureFilm film = HeroSprite.kohakuFilm();
		
		// idle: row 0 (DOWN facing), frame 0
		idle = new Animation( 1, true );
		idle.frames( film, 0 );
		
		// run: row 0, frames 1-8 (walk cycle)
		run = new Animation( 12, true );
		run.frames( film, 1, 2, 3, 4, 5, 6, 7, 8 );
		
		// die: row 0, frame 1
		die = new Animation( 20, false );
		die.frames( film, 1 );
		
		// attack: row 0, frames 9-12 (first 4 attack frames)
		attack = new Animation( 15, false );
		attack.frames( film, 9, 10, 11, 12 );
		
		// Scale down 96->16 to match standard mob size (same as HeroSprite LOGICAL_SIZE)
		scale.set( 16f / KOHAKU_FRAME_WIDTH, 16f / KOHAKU_FRAME_HEIGHT );
		
		idle();
	}
}
