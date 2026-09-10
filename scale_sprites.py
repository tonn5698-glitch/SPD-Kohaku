#!/usr/bin/env python3
"""Scale Kohaku sprites from 96x96 to 12x15 using LANCZOS (smooth, no pixelation)."""
from PIL import Image
import os, glob

WALK = "/mnt/sdcard/Download/Kohaku sprites/pixel dungeon/walk/walk4.png"
ATTACK_DIRS = {
    0: "/mnt/sdcard/Download/Kohaku sprites/pixel dungeon/attack-down",
    1: "/mnt/sdcard/Download/Kohaku sprites/pixel dungeon/attack-left",
    2: "/mnt/sdcard/Download/Kohaku sprites/pixel dungeon/attack-right",
    3: "/mnt/sdcard/Download/Kohaku sprites/pixel dungeon/attack-up",
}
OUT = "core/src/main/assets/sprites/kohaku.png"
FW, FH = 12, 15

walk = Image.open(WALK).convert("RGBA")
rw, rh = 96, 96
out = Image.new("RGBA", (256, 60), (0, 0, 0, 0))

for direction in range(4):
    row_y = direction * FH

    # IDLE: walk frame index 1
    idle_src_x = 1 * rw
    idle_src_y = direction * rh
    idle = walk.crop((idle_src_x, idle_src_y, idle_src_x + rw, idle_src_y + rh))
    idle_small = idle.resize((FW, FH), Image.LANCZOS)
    out.paste(idle_small, (0, row_y))

    # WALK: frames 0-7
    for i in range(8):
        sx = i * rw
        sy = direction * rh
        frame = walk.crop((sx, sy, sx + rw, sy + rh))
        frame_small = frame.resize((FW, FH), Image.LANCZOS)
        out.paste(frame_small, ((1 + i) * FW, row_y))

    # ATTACK: 4 frames x 3 repeat = 12 frames
    attack_files = sorted(glob.glob(os.path.join(ATTACK_DIRS[direction], "*.png")))
    for i, af in enumerate(attack_files):
        aframe = Image.open(af).convert("RGBA")
        aframe_small = aframe.resize((FW, FH), Image.LANCZOS)
        for rep in range(3):
            col = 9 + i * 3 + rep
            out.paste(aframe_small, (col * FW, row_y))

    # DIE: idle repeated (21-27)
    for i in range(7):
        out.paste(idle_small, ((21 + i) * FW, row_y))

    # OPERATE: idle (28-29)
    out.paste(idle_small, (28 * FW, row_y))
    out.paste(idle_small, (29 * FW, row_y))

out.save(OUT)
print(f"Saved: {OUT} ({out.size})")
