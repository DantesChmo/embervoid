#!/usr/bin/env python3
"""Writes the placeholder GeckoLib model, animations and texture for the Ashen Maiden (Sanctum NPC).

Run from the repository root: python3 tools/maiden_placeholder.py
Uses the helpers of tools/mob_placeholders.py (same output layout). A stand-in until the art pass.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mob_placeholders import Bone, Cube, anim, keys, paint_box, write_mob  # noqa: E402


# --- Ashen Maiden: tall hooded figure in a long ash-grey robe, holding an ember in cupped hands -----------------

def ashen_maiden():
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("robe", [0, 12, 0], "root", None, [Cube([-5, 0, -3.5], [10, 13, 7], "robe")]),
        Bone("body", [0, 13, 0], "root", None, [Cube([-4, 13, -2], [8, 11, 4], "robe")]),
        Bone("head", [0, 24, 0], "body", None, [Cube([-3.5, 24, -3.5], [7, 8, 7], "face")]),
        Bone("hood", [0, 24, 0], "head", None, [Cube([-4.5, 24, -4.5], [9, 9.5, 9], "hood")]),
        Bone("mantle", [0, 23, 0], "body", None, [Cube([-5, 19, -2.5], [10, 5, 5], "mantle")]),
        Bone("right_arm", [-4.5, 22, 0], "body", [-40, 15, 0], [Cube([-6.5, 12, -1.5], [3, 11, 3], "sleeve")]),
        Bone("left_arm", [4.5, 22, 0], "body", [-40, -15, 0], [Cube([3.5, 12, -1.5], [3, 11, 3], "sleeve")]),
        Bone("ember", [0, 15, -6], "body", None, [Cube([-1, 14, -7], [2, 2, 2], "ember")]),
    ]
    zero = [0, 0, 0]
    animations = {
        "misc.idle": anim(4.0, {
            "body": {"rotation": keys(0, zero, 2, [2, 0, 0], 4, zero)},
            "head": {"rotation": keys(0, [5, 0, 0], 2, [8, 3, 0], 4, [5, 0, 0])},
            "ember": {"position": keys(0, zero, 2, [0, 0.5, 0], 4, zero),
                      "scale": keys(0, [1, 1, 1], 1, [1.25, 1.25, 1.25], 2, [1, 1, 1], 3, [1.25, 1.25, 1.25], 4, [1, 1, 1])},
        }),
        # Raises the ember toward the player when they talk to her.
        "misc.interact": anim(1.5, {
            "right_arm": {"rotation": keys(0, zero, 0.5, [-25, 0, 0], 1.5, zero)},
            "left_arm": {"rotation": keys(0, zero, 0.5, [-25, 0, 0], 1.5, zero)},
            "ember": {"position": keys(0, zero, 0.5, [0, 2, -1], 1.5, zero)},
            "head": {"rotation": keys(0, zero, 0.5, [-10, 0, 0], 1.5, zero)},
        }, loop=False),
    }
    robe = (92, 86, 84, 255)
    hood = (70, 64, 64, 255)
    skin = (190, 178, 160, 255)
    ember = (255, 150, 50, 255)

    def paint(tex, glow, bone, cube, rng):
        color = {"robe": robe, "mantle": hood, "hood": hood, "sleeve": robe, "face": skin, "ember": ember}[cube.tag]
        paint_box(tex, cube, color, rng, 8 if cube.tag != "ember" else 20)
        faces = cube.faces()
        if cube.tag == "ember":
            paint_box(glow, cube, ember, rng, 20)
        if cube.tag == "hood":
            x, y, w, h = faces["front"]
            tex.rect(x + 1, y + 2, w - 2, h - 2, (0, 0, 0, 0))  # open face
            x, y, w, h = faces["bottom"]
            tex.rect(x, y, w, h, (0, 0, 0, 0))
        if cube.tag == "face":
            x, y, w, h = faces["front"]
            # Closed eyes with faint ember light, ash-smudged cheeks.
            for ex in (x + 1, x + w - 3):
                tex.rect(ex, y + 4, 2, 1, (60, 40, 30, 255))
                glow.set(ex, y + 4, (180, 90, 30, 255))
            tex.rect(x + 1, y + 5, 1, 2, (120, 110, 104, 255))
            tex.rect(x + w - 2, y + 5, 1, 2, (120, 110, 104, 255))
        if cube.tag == "robe":
            # Ember-coloured hem and a few cinder burns.
            x, y, w, h = faces["front"]
            if bone.name == "robe":
                for name in ("front", "back", "left", "right"):
                    fx, fy, fw, fh = faces[name]
                    tex.rect(fx, fy + fh - 1, fw, 1, (150, 70, 30, 255))
            for _ in range(4):
                name = rng.choice(["front", "back", "left", "right"])
                fx, fy, fw, fh = faces[name]
                tex.set(fx + rng.randrange(fw), fy + rng.randrange(fh), (40, 34, 32, 255))

    write_mob("ashen_maiden", bones, 64, [1.5, 2.5], animations, paint)


if __name__ == "__main__":
    ashen_maiden()
    print("Wrote placeholder Ashen Maiden assets")
