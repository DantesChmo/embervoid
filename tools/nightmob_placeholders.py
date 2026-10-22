#!/usr/bin/env python3
"""Writes placeholder GeckoLib models, animations and textures for the full moon's mobs (PLAN 8.7, 8.8).

Run from the repository root: python3 tools/nightmob_placeholders.py
Uses the helpers of tools/mob_placeholders.py; same output folders and the same rules (box UV, glowmasks).
Stand-ins until the art pass, like the other mobs.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from mob_placeholders import Bone, Cube, ROOT, anim, keys, paint_box, write_mob  # noqa: E402


# --- Ashen Zombie: broad, burnt-grey zombie, arms forward, smouldering eyes and seams ----------------------------

def ashen_zombie():
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 24, 0], "root", [6, 0, 0], [Cube([-4.5, 12, -2.5], [9, 12, 5], "body")]),
        Bone("head", [0, 24, -1], "root", None, [Cube([-4, 24, -5], [8, 8, 8], "head")]),
        Bone("right_arm", [-5.5, 22, 0], "root", [-80, 0, 0], [Cube([-8.5, 10, -2], [4, 13, 4], "limb")]),
        Bone("left_arm", [5.5, 22, 0], "root", [-80, 0, 0], [Cube([4.5, 10, -2], [4, 13, 4], "limb")]),
        Bone("right_leg", [-2, 12, 0], "root", None, [Cube([-4.2, 0, -2], [4, 12, 4], "limb")]),
        Bone("left_leg", [2, 12, 0], "root", None, [Cube([0.2, 0, -2], [4, 12, 4], "limb")]),
    ]
    zero = [0, 0, 0]
    animations = {
        "misc.idle": anim(3.0, {
            "head": {"rotation": keys(0, zero, 1.5, [4, 6, 0], 3, zero)},
            "right_arm": {"rotation": keys(0, zero, 1.5, [-4, 0, 2], 3, zero)},
            "left_arm": {"rotation": keys(0, zero, 1.5, [4, 0, -2], 3, zero)},
        }),
        "move.walk": anim(1.0, {
            "right_leg": {"rotation": keys(0, [28, 0, 0], 0.5, [-28, 0, 0], 1, [28, 0, 0])},
            "left_leg": {"rotation": keys(0, [-28, 0, 0], 0.5, [28, 0, 0], 1, [-28, 0, 0])},
            "right_arm": {"rotation": keys(0, [-6, 0, 0], 0.5, [6, 0, 0], 1, [-6, 0, 0])},
            "left_arm": {"rotation": keys(0, [6, 0, 0], 0.5, [-6, 0, 0], 1, [6, 0, 0])},
            "body": {"rotation": keys(0, [0, 4, 0], 0.5, [0, -4, 0], 1, [0, 4, 0])},
        }),
        "attack.swing": anim(0.5, {
            "right_arm": {"rotation": keys(0, zero, 0.2, [-40, 0, 0], 0.3, [30, 0, 0], 0.5, zero)},
            "left_arm": {"rotation": keys(0, zero, 0.2, [-40, 0, 0], 0.3, [30, 0, 0], 0.5, zero)},
            "body": {"rotation": keys(0, zero, 0.3, [10, 0, 0], 0.5, zero)},
        }, loop=False),
    }
    ash = (70, 66, 63, 255)
    char = (38, 34, 32, 255)
    ember = (230, 80, 40, 255)

    def paint(tex, glow, bone, cube, rng):
        paint_box(tex, cube, ash, rng, 16)
        faces = cube.faces()
        # Charred patches and a few smouldering seams.
        for _ in range(5):
            x, y, w, h = faces[rng.choice(list(faces))]
            tex.rect(x + rng.randrange(w), y + rng.randrange(h), 2, 1, char)
        for _ in range(2 if cube.tag == "limb" else 4):
            x, y, w, h = faces[rng.choice(["front", "back", "left", "right"])]
            cx, cy = x + rng.randrange(w), y + rng.randrange(h)
            for i in range(rng.randint(2, 3)):
                if x <= cx + i < x + w and y <= cy < y + h:
                    tex.set(cx + i, cy, (170, 60, 30, 255))
                    glow.set(cx + i, cy, ember)
        if cube.tag == "head":
            x, y, w, h = faces["front"]
            tex.rect(x + 1, y + 3, w - 2, 2, char)
            for ex in (x + 1, x + w - 3):
                tex.rect(ex, y + 3, 2, 1, ember)
                glow.rect(ex, y + 3, 2, 1, ember)
            tex.rect(x + 2, y + h - 2, w - 4, 1, char)

    write_mob("ashen_zombie", bones, 64, [2, 2.5], animations, paint)


# --- Ghost: pale hooded torso trailing into a wisp, long arms reaching forward, cold glowing eyes -------------------

def ghost():
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 14, 0], "root", [10, 0, 0], [Cube([-4, 14, -2], [8, 11, 4], "body")]),
        Bone("tail", [0, 14, 0], "body", None, [Cube([-3, 8, -1.5], [6, 6, 3], "body")]),
        Bone("wisp", [0, 8, 0], "tail", None, [Cube([-1.5, 3, -1], [3, 5, 2], "wisp")]),
        Bone("head", [0, 25, -1], "root", None, [
            Cube([-3.5, 25, -4.5], [7, 7, 7], "head"),
            Cube([-4, 25.5, -5], [8, 7.5, 8], "hood"),
        ]),
        Bone("right_arm", [-5, 24, -1], "root", [-70, 0, 0], [Cube([-7, 12, -2], [3, 12, 3], "body")]),
        Bone("left_arm", [5, 24, -1], "root", [-70, 0, 0], [Cube([4, 12, -2], [3, 12, 3], "body")]),
    ]
    zero = [0, 0, 0]

    def hover(length, bob, sway):
        return anim(length, {
            "root": {"position": keys(0, zero, length / 2, [0, bob, 0], length, zero)},
            "tail": {"rotation": keys(0, [sway, 0, 0], length / 2, [-sway, 0, 0], length, [sway, 0, 0])},
            "wisp": {"rotation": keys(0, [-sway, 0, 0], length / 2, [sway, 0, 0], length, [-sway, 0, 0])},
            "right_arm": {"rotation": keys(0, zero, length / 2, [-6, 0, 3], length, zero)},
            "left_arm": {"rotation": keys(0, zero, length / 2, [-6, 0, -3], length, zero)},
        })

    animations = {
        "misc.idle": hover(3.0, 1, 8),
        "move.fly": hover(1.5, 1.5, 18),
        "attack.swing": anim(0.5, {
            "right_arm": {"rotation": keys(0, zero, 0.15, [-50, 0, 15], 0.3, [40, 0, -10], 0.5, zero)},
            "left_arm": {"rotation": keys(0, zero, 0.15, [-50, 0, -15], 0.3, [40, 0, 10], 0.5, zero)},
            "head": {"rotation": keys(0, zero, 0.3, [15, 0, 0], 0.5, zero)},
        }, loop=False),
    }
    pale = (190, 205, 220, 170)
    hood = (120, 135, 160, 190)
    wisp = (170, 200, 230, 90)
    eye = (120, 230, 255, 255)

    def paint(tex, glow, bone, cube, rng):
        color = {"hood": hood, "wisp": wisp}.get(cube.tag, pale)
        paint_box(tex, cube, color, rng, 14)
        faces = cube.faces()
        if cube.tag == "hood":
            # Open front and bottom: the face shows through.
            x, y, w, h = faces["front"]
            tex.rect(x + 1, y + 1, w - 2, h - 1, (0, 0, 0, 0))
            x, y, w, h = faces["bottom"]
            tex.rect(x, y, w, h, (0, 0, 0, 0))
        if cube.tag == "head":
            x, y, w, h = faces["front"]
            tex.rect(x, y, w, h, (30, 34, 48, 220))  # dark face in the hood
            for ex in (x + 1, x + w - 3):
                tex.rect(ex, y + 3, 2, 1, eye)
                glow.rect(ex, y + 3, 2, 1, eye)

    write_mob("ghost", bones, 64, [2, 3], animations, paint)


if __name__ == "__main__":
    ashen_zombie()
    ghost()
    print("Wrote placeholder night mob assets under", ROOT)
