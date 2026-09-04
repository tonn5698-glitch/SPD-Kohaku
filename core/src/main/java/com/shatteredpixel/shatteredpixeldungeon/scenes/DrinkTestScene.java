package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.Scene;

/**
 * Test screen: cycles through all drink sprites auto-renew.
 */
public class DrinkTestScene extends Scene {

    private static final String[] COLORS = {
        "amber", "azure", "bistre", "charcoal", "crimson",
        "golden", "indigo", "ivory", "jade", "magenta",
        "silver", "turquoise",
        "exotic_amber", "exotic_azure", "exotic_bistre", "exotic_charcoal",
        "exotic_crimson", "exotic_golden", "exotic_indigo", "exotic_ivory",
        "exotic_jade", "exotic_magenta", "exotic_silver", "exotic_turquoise"
    };

    private static final float PHASE_DURATION = 1.0f;

    private int currentColor = 0;
    private int currentPhase = 0; // 0=held, 1=drinking, 2=good
    private float timer = 0;

    private Image sprite;
    private RenderedTextBlock label;
    private ColorBlock bg;

    private static final float SCALE = 4f; // 96/16 * 4 = render big

    @Override
    public void create() {
        super.create();

        bg = new ColorBlock(Game.width, Game.height, 0xFF222222);
        add(bg);

        float cx = Game.width / 2f;
        float cy = Game.height / 2f;

        sprite = new Image("sprites/kohaku_drink/held_amber.png");
        sprite.origin.set(48, 48);
        sprite.scale.set(SCALE);
        sprite.x = cx;
        sprite.y = cy;
        add(sprite);

        label = new RenderedTextBlock("HELD | amber", 9);
        label.hardlight(0xFFFFFF);
        add(label);

        updateDisplay();
    }

    private void updateDisplay() {
        String color = COLORS[currentColor];
        String phaseName;

        switch (currentPhase) {
            case 0:
                phaseName = "HELD";
                sprite.texture("sprites/kohaku_drink/held_" + color + ".png");
                break;
            case 1:
                phaseName = "DRINK";
                sprite.texture("sprites/kohaku_drink/drinking.png");
                break;
            default:
                phaseName = "GOOD";
                sprite.texture("sprites/kohaku_drink/good.png");
                break;
        }

        sprite.origin.set(48, 48);
        sprite.scale.set(SCALE);

        label.text(phaseName + " | " + color);
        label.setPos(Game.width / 2f - label.width() / 2f, Game.height / 2f + 55);
    }

    @Override
    public void update() {
        super.update();
        timer += Game.elapsed;

        // Squash & stretch during drink phase
        if (currentPhase == 1) {
            float cp = (timer / PHASE_DURATION) * 3f;
            int cyc = Math.min((int) cp, 2);
            float ct = cp - cyc;
            float intensity = 1f / (1f + cyc);
            float sx = 1f - intensity * 0.15f * (float) Math.sin(ct * Math.PI * 2);
            float sy = 1f + intensity * 0.15f * (float) Math.sin(ct * Math.PI * 2 + (float) Math.PI);
            sprite.scale.set(sx * SCALE, sy * SCALE);
        } else if (currentPhase == 2) {
            float pulse = 1f + 0.1f * (float) Math.sin((timer / PHASE_DURATION) * Math.PI * 2);
            sprite.scale.set(pulse * SCALE);
        } else {
            sprite.scale.set(SCALE);
        }

        if (timer >= PHASE_DURATION) {
            timer = 0;
            currentPhase++;
            if (currentPhase >= 3) {
                currentPhase = 0;
                currentColor = (currentColor + 1) % COLORS.length;
            }
            updateDisplay();
        }
    }
}
