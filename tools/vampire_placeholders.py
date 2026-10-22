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


# --- Vampire boss: the same noble, grown gaunt, with bat wings and claws ------------------------------------------

def vampire_boss():
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 24, 0], "root", [8, 0, 0], [
            Cube([-4, 12, -2], [8, 12, 4], "torso"),
            Cube([-5, 24, -3], [10, 4, 6], "collar"),
        ]),
        Bone("right_wing", [-3, 22, 2], "body", [0, 30, 0], [Cube([-17, 8, 2], [14, 15, 1], "wing")]),
        Bone("left_wing", [3, 22, 2], "body", [0, -30, 0], [Cube([3, 8, 2], [14, 15, 1], "wing")]),
        Bone("head", [0, 25, -1], "root", None, [
            Cube([-4, 25, -5], [8, 8, 8], "head"),
            Cube([-4.5, 31, -1], [2, 4, 2], "horn"),
            Cube([2.5, 31, -1], [2, 4, 2], "horn"),
        ]),
        Bone("right_arm", [-5, 23, 0], "root", [-10, 0, 8], [
            Cube([-7.5, 8, -1.5], [3, 15, 3], "arm"),
            Cube([-7.5, 5, -2], [3, 3, 4], "claw"),
        ]),
        Bone("left_arm", [5, 23, 0], "root", [-10, 0, -8], [
            Cube([4.5, 8, -1.5], [3, 15, 3], "arm"),
            Cube([4.5, 5, -2], [3, 3, 4], "claw"),
        ]),
        Bone("right_leg", [-2, 12, 0], "root", None, [Cube([-4, 0, -2], [4, 12, 4], "leg")]),
        Bone("left_leg", [2, 12, 0], "root", None, [Cube([0, 0, -2], [4, 12, 4], "leg")]),
    ]
    animations = {
        "misc.idle": anim(2.0, {
            "right_wing": {"rotation": keys(0, ZERO, 1, [0, 12, 0], 2, ZERO)},
            "left_wing": {"rotation": keys(0, ZERO, 1, [0, -12, 0], 2, ZERO)},
            "body": {"rotation": keys(0, ZERO, 1, [3, 0, 0], 2, ZERO)},
        }),
        "move.walk": anim(0.8, {
            "right_leg": {"rotation": keys(0, [30, 0, 0], 0.4, [-30, 0, 0], 0.8, [30, 0, 0])},
            "left_leg": {"rotation": keys(0, [-30, 0, 0], 0.4, [30, 0, 0], 0.8, [-30, 0, 0])},
            "right_arm": {"rotation": keys(0, [-25, 0, 0], 0.4, [15, 0, 0], 0.8, [-25, 0, 0])},
            "left_arm": {"rotation": keys(0, [15, 0, 0], 0.4, [-25, 0, 0], 0.8, [15, 0, 0])},
            "right_wing": {"rotation": keys(0, [0, 20, 0], 0.4, [0, 35, 0], 0.8, [0, 20, 0])},
            "left_wing": {"rotation": keys(0, [0, -20, 0], 0.4, [0, -35, 0], 0.8, [0, -20, 0])},
        }),
        "attack.swing": anim(0.5, {
            "right_arm": {"rotation": keys(0, ZERO, 0.2, [-130, 20, 0], 0.35, [-30, -20, 0], 0.5, ZERO)},
            "left_arm": {"rotation": keys(0, ZERO, 0.25, [-120, -20, 0], 0.4, [-30, 20, 0], 0.5, ZERO)},
        }, loop=False),
        # 1 s windup (LEAP_WINDUP 20 ticks): crouch, wings up, then the jump.
        "attack.leap": anim(1.4, {
            "body": {"rotation": keys(0, ZERO, 0.9, [35, 0, 0], 1.1, [-10, 0, 0], 1.4, ZERO)},
            "root": {"position": keys(0, ZERO, 0.9, [0, -3, 0], 1.1, ZERO)},
            "right_wing": {"rotation": keys(0, ZERO, 0.9, [-40, 60, 0], 1.1, [0, 0, 0], 1.4, ZERO)},
            "left_wing": {"rotation": keys(0, ZERO, 0.9, [-40, -60, 0], 1.1, [0, 0, 0], 1.4, ZERO)},
            "right_arm": {"rotation": keys(0, ZERO, 0.9, [40, 0, 30], 1.1, [-150, 0, 0], 1.4, ZERO)},
            "left_arm": {"rotation": keys(0, ZERO, 0.9, [40, 0, -30], 1.1, [-150, 0, 0], 1.4, ZERO)},
        }, loop=False),
        # 1.5 s windup (NOVA_WINDUP 30 ticks): arms and wings spread wide, then the burst.
        "attack.nova": anim(1.8, {
            "right_arm": {"rotation": keys(0, ZERO, 1.4, [0, 0, 100], 1.5, [0, 0, 140], 1.8, ZERO)},
            "left_arm": {"rotation": keys(0, ZERO, 1.4, [0, 0, -100], 1.5, [0, 0, -140], 1.8, ZERO)},
            "right_wing": {"rotation": keys(0, ZERO, 1.4, [0, 70, 0], 1.8, ZERO)},
            "left_wing": {"rotation": keys(0, ZERO, 1.4, [0, -70, 0], 1.8, ZERO)},
            "head": {"rotation": keys(0, ZERO, 1.4, [-30, 0, 0], 1.8, ZERO)},
        }, loop=False),
        # 0.6 s windup (MIST_WINDUP 12 ticks): folds into its wings.
        "misc.mist": anim(0.8, {
            "right_wing": {"rotation": keys(0, ZERO, 0.6, [0, -60, 0], 0.8, ZERO)},
            "left_wing": {"rotation": keys(0, ZERO, 0.6, [0, 60, 0], 0.8, ZERO)},
            "root": {"scale": keys(0, [1, 1, 1], 0.6, [0.8, 1.1, 0.8], 0.8, [1, 1, 1])},
        }, loop=False),
    }
    skin = (196, 186, 190, 255)
    membrane = (70, 18, 26, 255)

    def paint(tex, glow, bone, cube, rng):
        color = {"torso": DOUBLET, "collar": LINING, "wing": membrane, "head": skin, "horn": HAIR,
                 "arm": skin, "claw": (30, 26, 28, 255), "leg": TROUSERS}[cube.tag]
        paint_box(tex, cube, color, rng, 8)
        faces = cube.faces()
        if cube.tag == "head":
            paint_face(tex, glow, cube, mouth=(160, 10, 20, 255))
        elif cube.tag == "wing":
            for name in ("front", "back"):
                x, y, w, h = faces[name]
                for i in range(0, w, 4):  # wing bones
                    tex.rect(x + i, y, 1, h, (30, 10, 14, 255))
        elif cube.tag == "torso":
            x, y, w, h = faces["front"]
            for _ in range(5):  # blood stains that glow faintly
                px, py = x + rng.randrange(w), y + rng.randrange(h)
                tex.set(px, py, (170, 10, 20, 255))
                glow.set(px, py, (120, 0, 10, 255))

    write_mob("vampire_boss", bones, 64, [3, 3.5], animations, paint)


def vampire_fang():
    """16x16 item texture: a curved white fang with a red tip."""
    from mob_placeholders import Canvas
    c = Canvas(16, 16)
    outline = (60, 50, 50, 255)
    pixels = [(7, 2), (8, 2), (6, 3), (7, 3), (8, 3), (9, 3), (6, 4), (7, 4), (8, 4), (9, 4), (6, 5), (7, 5), (8, 5),
              (9, 5), (6, 6), (7, 6), (8, 6), (9, 6), (7, 7), (8, 7), (9, 7), (7, 8), (8, 8), (9, 8), (8, 9), (9, 9),
              (8, 10), (9, 10), (9, 11), (9, 12)]
    for x, y in pixels:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in pixels:
                c.set(x + dx, y + dy, outline)
    for x, y in pixels:
        shade = 250 - (y * 6)
        c.set(x, y, (shade, shade - 4, shade - 10, 255))
    for x, y in ((9, 11), (9, 12), (8, 10)):
        c.set(x, y, (180, 20, 30, 255))
    c.save(os.path.join("src", "main", "resources", "assets", "embervoid", "textures", "item", "vampire_fang.png"))


if __name__ == "__main__":
    vampire()
    vampire_boss()
    vampire_fang()
    print("Wrote Vampire placeholder assets")
