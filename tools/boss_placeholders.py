#!/usr/bin/env python3
"""Writes greybox GeckoLib models, animations and textures for the Ashen Reach bosses (PLAN 12.3-12.5, 13.4).

Run from the repository root: python3 tools/boss_placeholders.py [name ...]
Uses the helpers of tools/mob_placeholders.py; output goes to the same folders. Models are at final size and the
animations carry the final names and timings (each attack animation contains its windup and lands at the tick the
server lands the move), so the art pass can replace the shapes without touching the code.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mob_placeholders import Bone, Cube, anim, keys, painter, write_mob  # noqa: E402

ZERO = [0, 0, 0]


def rot(*pairs):
    return {"rotation": keys(*pairs)}


def pos(*pairs):
    return {"position": keys(*pairs)}


def t(ticks):
    """Server ticks to animation seconds."""
    return round(ticks / 20, 3)


# --- Crypt Keeper: 4 blocks, hooded gaoler of the ossuary, halberd in the right hand, lantern in the left --------

def crypt_keeper():
    legs, body_h, head = 26, 24, 13
    top = legs + body_h
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("right_leg", [-3.5, legs, 0], "root", None, [Cube([-7, 0, -3.5], [7, legs, 7], "robe")]),
        Bone("left_leg", [3.5, legs, 0], "root", None, [Cube([0, 0, -3.5], [7, legs, 7], "robe")]),
        Bone("body", [0, legs, 0], "root", None, [
            Cube([-8, legs, -5], [16, body_h, 10], "body"),
            Cube([-9, 8, -6], [18, 18, 12], "robe"),
            Cube([-9, top - 4, -6], [18, 4, 12], "mantle"),
        ]),
        Bone("head", [0, top, 0], "body", None, [Cube([-6.5, top, -6.5], [13, head, 13], "hood")]),
        Bone("right_arm", [-11, top - 2, 0], "body", None, [Cube([-14, top - 26, -3], [6, 26, 6], "limb")]),
        Bone("left_arm", [11, top - 2, 0], "body", None, [Cube([8, top - 26, -3], [6, 26, 6], "limb")]),
        Bone("halberd", [-11, top - 24, 0], "right_arm", None, [
            Cube([-12, 2, -1], [2, 62, 2], "wood"),
            Cube([-17, 50, -0.5], [5, 10, 1], "iron"),
            Cube([-10, 54, -0.5], [3, 4, 1], "iron"),
            Cube([-11.5, 64, -0.5], [1, 5, 1], "iron"),
        ]),
        Bone("lantern", [11, top - 26, 0], "left_arm", None, [
            Cube([10.5, top - 32, -0.5], [1, 6, 1], "iron"),
            Cube([8, top - 40, -3], [6, 8, 6], "flame"),
        ]),
    ]
    swing1, swing2, overhead, sweep, throw, thrust = t(14), t(10), t(22), t(24), t(20), t(18)
    animations = {
        "misc.idle": anim(3.0, {
            "body": rot(0, ZERO, 1.5, [2, 0, 0], 3.0, ZERO),
            "lantern": rot(0, [0, 0, -4], 1.5, [0, 0, 4], 3.0, [0, 0, -4]),
        }),
        "move.walk": anim(1.4, {
            "right_leg": rot(0, [22, 0, 0], 0.7, [-22, 0, 0], 1.4, [22, 0, 0]),
            "left_leg": rot(0, [-22, 0, 0], 0.7, [22, 0, 0], 1.4, [-22, 0, 0]),
            "left_arm": rot(0, [10, 0, 0], 0.7, [-10, 0, 0], 1.4, [10, 0, 0]),
            "lantern": rot(0, [-8, 0, 0], 0.7, [8, 0, 0], 1.4, [-8, 0, 0]),
        }),
        # Wide swing right to left: the arm drawn out to the side through the windup.
        "attack.halberd_1": anim(swing1 + 0.35, {
            "right_arm": rot(0, ZERO, swing1, [-80, -70, 0], swing1 + 0.1, [-80, 60, 0], swing1 + 0.35, ZERO),
            "body": rot(0, ZERO, swing1, [0, 25, 0], swing1 + 0.1, [0, -30, 0], swing1 + 0.35, ZERO),
        }, loop=False),
        # Backhand left to right, shorter windup.
        "attack.halberd_2": anim(swing2 + 0.3, {
            "right_arm": rot(0, [-80, 60, 0], swing2, [-85, 70, 0], swing2 + 0.1, [-80, -60, 0], swing2 + 0.3, ZERO),
            "body": rot(0, [0, -30, 0], swing2, [0, -35, 0], swing2 + 0.1, [0, 30, 0], swing2 + 0.3, ZERO),
        }, loop=False),
        # The unblockable overhead: both hands high for over a second, then down.
        "attack.overhead": anim(overhead + 0.5, {
            "right_arm": rot(0, ZERO, overhead, [-175, 0, 15], overhead + 0.08, [-40, 0, 0], overhead + 0.5, ZERO),
            "left_arm": rot(0, ZERO, overhead, [-175, 0, -15], overhead + 0.08, [-40, 0, 0], overhead + 0.5, ZERO),
            "body": rot(0, ZERO, overhead, [-12, 0, 0], overhead + 0.08, [25, 0, 0], overhead + 0.5, ZERO),
        }, loop=False),
        # Coiled, then a full turn with the halberd out.
        "attack.sweep": anim(sweep + 0.4, {
            "body": rot(0, ZERO, sweep, [0, 70, 0], sweep + 0.25, [0, -290, 0], sweep + 0.4, [0, -360, 0]),
            "right_arm": rot(0, ZERO, sweep, [-90, 0, 0], sweep + 0.4, ZERO),
        }, loop=False),
        "attack.lantern_throw": anim(throw + 0.3, {
            "left_arm": rot(0, ZERO, throw, [40, 0, -20], throw + 0.1, [-130, 0, -10], throw + 0.3, ZERO),
        }, loop=False),
        "attack.thrust": anim(thrust + 0.4, {
            "right_arm": rot(0, ZERO, thrust, [30, 0, 0], thrust + 0.08, [-95, 0, 0], thrust + 0.4, ZERO),
            "body": rot(0, ZERO, thrust, [-10, 0, 0], thrust + 0.08, [20, 0, 0], thrust + 0.4, ZERO),
            "root": pos(0, ZERO, thrust, [0, 0, 2], thrust + 0.1, [0, 0, -6], thrust + 0.4, ZERO),
        }, loop=False),
        "misc.lantern_shatter": anim(1.2, {
            "left_arm": rot(0, ZERO, 0.5, [-170, 0, -10], 0.6, [-150, 0, -25], 0.7, [-170, 0, 5], 1.2, ZERO),
            "head": rot(0, ZERO, 0.5, [-25, 0, 0], 1.2, ZERO),
        }, loop=False),
        "misc.raise_dead": anim(1.5, {
            "right_arm": rot(0, ZERO, 0.8, [-150, 0, 30], 1.5, ZERO),
            "left_arm": rot(0, ZERO, 0.8, [-150, 0, -30], 1.5, ZERO),
            "head": rot(0, ZERO, 0.8, [-30, 0, 0], 1.5, ZERO),
        }, loop=False),
    }
    paint = painter({"default": (52, 54, 62, 255), "robe": (40, 42, 50, 255), "body": (58, 60, 68, 255),
                     "mantle": (70, 66, 60, 255), "hood": (34, 34, 40, 255), "limb": (48, 50, 58, 255),
                     "wood": (78, 62, 46, 255), "iron": (110, 112, 118, 255), "flame": (120, 220, 255, 255)},
                    eye=(150, 230, 255, 255), eye_tags=("hood",), eye_row=0.5, glow_tags=("flame",))
    write_mob("crypt_keeper", bones, 128, [3, 4.5], animations, paint)


BOSSES = [("crypt_keeper", crypt_keeper)]


if __name__ == "__main__":
    wanted = set(sys.argv[1:])
    for name, build in BOSSES:
        if not wanted or name in wanted:
            build()
    print("Wrote greybox boss assets")
