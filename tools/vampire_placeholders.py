#!/usr/bin/env python3
"""Writes placeholder GeckoLib models, animations and textures for the blood new moon's Vampires (PLAN 8.10-8.11).

Run from the repository root: python3 tools/vampire_placeholders.py
Uses the helpers of tools/mob_placeholders.py; output goes to the same folders. Stand-ins until the art pass.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mob_placeholders import Bone, Cube, anim, keys, paint_box, write_mob  # noqa: E402

ZERO = [0, 0, 0]
SKIN = (222, 216, 212, 255)
HAIR = (24, 20, 24, 255)
CLOAK = (38, 10, 16, 255)
LINING = (128, 14, 26, 255)
DOUBLET = (52, 26, 46, 255)
TROUSERS = (30, 26, 32, 255)
EYE = (255, 40, 40, 255)


def paint_face(tex, glow, cube, mouth=(120, 20, 30, 255)):
    x, y, w, h = cube.faces()["front"]
    tex.rect(x, y, w, 2, HAIR)  # hairline
    for ex in (x + 1, x + w - 3):
        tex.rect(ex, y + 3, 2, 1, EYE)
        glow.rect(ex, y + 3, 2, 1, EYE)
    tex.rect(x + 3, y + h - 2, w - 6, 1, mouth)
    tex.set(x + 3, y + h - 1, (240, 240, 240, 255))  # fangs
    tex.set(x + w - 4, y + h - 1, (240, 240, 240, 255))
    top = cube.faces()["top"]
    tex.rect(*top, HAIR)
    back = cube.faces()["back"]
    tex.rect(*back, HAIR)


# --- Vampire (friendly form): tall, pale noble in a high-collared cloak ------------------------------------------

def vampire():
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 24, 0], "root", None, [
            Cube([-4, 12, -2], [8, 12, 4], "doublet"),
            Cube([-4.5, 24, -2.5], [9, 3, 5], "collar"),
        ]),
        Bone("cloak", [0, 24, 2], "body", [6, 0, 0], [Cube([-5, 3, 2], [10, 21, 1], "cloak")]),
        Bone("head", [0, 24, 0], "root", None, [Cube([-4, 24, -4], [8, 8, 8], "head")]),
        Bone("right_arm", [-5, 22, 0], "root", None, [Cube([-7, 11, -1.5], [3, 13, 3], "sleeve")]),
        Bone("left_arm", [5, 22, 0], "root", None, [Cube([4, 11, -1.5], [3, 13, 3], "sleeve")]),
        Bone("right_leg", [-2, 12, 0], "root", None, [Cube([-4, 0, -2], [4, 12, 4], "trousers")]),
        Bone("left_leg", [2, 12, 0], "root", None, [Cube([0, 0, -2], [4, 12, 4], "trousers")]),
    ]
    animations = {
        "misc.idle": anim(4.0, {
            "body": {"rotation": keys(0, ZERO, 2, [2, 0, 0], 4, ZERO)},
            "cloak": {"rotation": keys(0, ZERO, 2, [4, 0, 0], 4, ZERO)},
            "right_arm": {"rotation": keys(0, [-8, 0, 4], 2, [-12, 0, 5], 4, [-8, 0, 4])},
            "left_arm": {"rotation": keys(0, [-8, 0, -4], 2, [-12, 0, -5], 4, [-8, 0, -4])},
        }),
        "move.walk": anim(1.4, {
            "right_leg": {"rotation": keys(0, [22, 0, 0], 0.7, [-22, 0, 0], 1.4, [22, 0, 0])},
            "left_leg": {"rotation": keys(0, [-22, 0, 0], 0.7, [22, 0, 0], 1.4, [-22, 0, 0])},
            "right_arm": {"rotation": keys(0, [-14, 0, 4], 0.7, [10, 0, 4], 1.4, [-14, 0, 4])},
            "left_arm": {"rotation": keys(0, [10, 0, -4], 0.7, [-14, 0, -4], 1.4, [10, 0, -4])},
            "cloak": {"rotation": keys(0, [12, 0, 0], 0.7, [16, 0, 0], 1.4, [12, 0, 0])},
        }),
        # 2.5 s = VampireEntity.TURN_TICKS: rises off the ground, arms spread, head thrown back, then held.
        "misc.transform": anim(2.5, {
            "root": {"position": keys(0, ZERO, 1.5, [0, 3, 0], 2.5, [0, 5, 0])},
            "head": {"rotation": keys(0, ZERO, 1.2, [-35, 0, 0], 2.5, [-20, 0, 0])},
            "right_arm": {"rotation": keys(0, ZERO, 1.2, [0, 0, 110], 2.5, [-30, 0, 130])},
            "left_arm": {"rotation": keys(0, ZERO, 1.2, [0, 0, -110], 2.5, [-30, 0, -130])},
            "cloak": {"rotation": keys(0, ZERO, 1.5, [40, 0, 0], 2.5, [70, 0, 0])},
            "right_leg": {"rotation": keys(0, ZERO, 2.5, [10, 0, 6])},
            "left_leg": {"rotation": keys(0, ZERO, 2.5, [10, 0, -6])},
        }, loop=False),
    }

    def paint(tex, glow, bone, cube, rng):
        color = {"doublet": DOUBLET, "collar": LINING, "cloak": CLOAK, "head": SKIN, "sleeve": DOUBLET,
                 "trousers": TROUSERS}[cube.tag]
        paint_box(tex, cube, color, rng, 6)
        faces = cube.faces()
        if cube.tag == "head":
            paint_face(tex, glow, cube)
        elif cube.tag == "cloak":
            tex.rect(*faces["front"], LINING)  # the red lining faces the body
        elif cube.tag == "doublet":
            x, y, w, h = faces["front"]
            tex.rect(x + w // 2 - 1, y, 2, h, (150, 120, 60, 255))  # buttons / trim
            tex.rect(x, y + h - 2, w, 1, (20, 12, 18, 255))  # belt
        elif cube.tag == "sleeve":
            x, y, w, h = faces["front"]
            tex.rect(x, y + h - 3, w, 3, SKIN)  # pale hands
            for name in ("left", "right", "back"):
                fx, fy, fw, fh = faces[name]
                tex.rect(fx, fy + fh - 3, fw, 3, SKIN)

    write_mob("vampire", bones, 64, [2, 3], animations, paint)


if __name__ == "__main__":
    vampire()
    print("Wrote Vampire placeholder assets")
