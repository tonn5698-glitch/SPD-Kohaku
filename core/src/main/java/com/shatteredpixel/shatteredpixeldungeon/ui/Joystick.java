package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.PointF;

public class Joystick extends Component {

    private static final float RADIUS = 24;
    private static final float KNOB_R = 10;
    private static final int BASE_COL = 0x55FFFFFF;
    private static final int KNOB_COL = 0xBBFFFFFF;

    private ColorBlock base;
    private ColorBlock knob;
    private PointerArea touchArea;

    private PointF center;
    private boolean active = false;
    private float dirX = 0;
    private float dirY = 0;

    private float moveCD = 0;
    private static final float MOVE_DELAY = 0.15f;

    @Override
    protected void createChildren() {
        base = new ColorBlock(RADIUS * 2, RADIUS * 2, BASE_COL);
        base.origin.set(RADIUS, RADIUS);
        add(base);

        knob = new ColorBlock(KNOB_R * 2, KNOB_R * 2, KNOB_COL);
        knob.origin.set(KNOB_R, KNOB_R);
        add(knob);

        touchArea = new PointerArea(0, 0, RADIUS * 8, RADIUS * 8) {
            @Override
            protected void onPointerDown(PointerEvent event) {
                active = true;
                moveKnob(event.current.x, event.current.y);
            }
            @Override
            protected void onDrag(PointerEvent event) {
                if (active) moveKnob(event.current.x, event.current.y);
            }
            @Override
            protected void onPointerUp(PointerEvent event) {
                active = false;
                dirX = 0; dirY = 0;
                resetKnob();
            }
        };
        touchArea.blockLevel = PointerArea.ALWAYS_BLOCK;
        add(touchArea);

        visible = false;
    }

    public void show(float x, float y) {
        center = new PointF(x, y);
        visible = true;
        base.visible = true;
        knob.visible = true;
        base.x = x - RADIUS;
        base.y = y - RADIUS;
        touchArea.x = x - RADIUS * 4;
        touchArea.y = y - RADIUS * 4;
        resetKnob();
    }

    private void resetKnob() {
        if (center != null) {
            knob.x = center.x - KNOB_R;
            knob.y = center.y - KNOB_R;
        }
    }

    private void moveKnob(float x, float y) {
        if (center == null) return;
        float dx = x - center.x;
        float dy = y - center.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > RADIUS) {
            dx = dx / dist * RADIUS;
            dy = dy / dist * RADIUS;
            dist = RADIUS;
        }
        knob.x = center.x + dx - KNOB_R;
        knob.y = center.y + dy - KNOB_R;
        if (dist > 3) {
            dirX = dx / RADIUS;
            dirY = dy / RADIUS;
        } else {
            dirX = 0; dirY = 0;
        }
    }

    public void update() {
        if (!active || Dungeon.hero == null || !Dungeon.hero.ready) return;
        if (Dungeon.hero.curAction != null) return;

        moveCD -= com.watabou.noosa.Game.elapsed;
        if (moveCD > 0) return;

        if (Math.abs(dirX) < 0.3f && Math.abs(dirY) < 0.3f) return;

        // Pick dominant direction
        int dx = 0, dy = 0;
        if (Math.abs(dirX) >= Math.abs(dirY)) {
            dx = dirX > 0 ? 1 : -1;
        } else {
            dy = dirY > 0 ? 1 : -1;
        }

        Hero hero = Dungeon.hero;
        int target = hero.pos + dx + dy * Dungeon.level.width();
        if (target >= 0 && target < Dungeon.level.length()
                && (Dungeon.level.passable[target] || Dungeon.level.avoid[target])) {
            hero.curAction = new HeroAction.Move(target);
            hero.lastAction = null;
            moveCD = MOVE_DELAY;
        }
    }

    public boolean isActive() { return active; }
}
