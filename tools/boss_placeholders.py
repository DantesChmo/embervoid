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


# --- Ash Colossus: 4.5 blocks of fused stone, hunched, huge arms, an ember core in the back under stone plates -----

def ash_colossus():
    legs, body_h, head = 22, 36, 12
    top = legs + body_h
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("right_leg", [-7, legs, 0], "root", None, [Cube([-13, 0, -6], [12, legs, 12], "stone")]),
        Bone("left_leg", [7, legs, 0], "root", None, [Cube([1, 0, -6], [12, legs, 12], "stone")]),
        Bone("body", [0, legs, 0], "root", [15, 0, 0], [
            Cube([-17, legs, -11], [34, body_h, 20], "stone"),
            Cube([-14, legs + 6, 9], [28, 26, 3], "ash"),
        ]),
        Bone("core", [0, legs + 18, 11], "body", None, [Cube([-6, legs + 12, 9], [12, 12, 4], "core")]),
        Bone("plates", [0, legs + 18, 12], "body", None, [
            Cube([-12, legs + 8, 11], [24, 22, 4], "plate"),
            Cube([-20, top - 6, -9], [8, 8, 16], "plate"),
            Cube([12, top - 6, -9], [8, 8, 16], "plate"),
        ]),
        Bone("head", [0, top, -6], "body", None, [Cube([-6, top - 2, -14], [12, head, 12], "head")]),
        Bone("right_arm", [-20, top - 4, 0], "body", None, [
            Cube([-28, top - 34, -6], [12, 30, 12], "stone"),
            Cube([-29, top - 44, -7], [14, 10, 14], "fist"),
        ]),
        Bone("left_arm", [20, top - 4, 0], "body", None, [
            Cube([16, top - 34, -6], [12, 30, 12], "stone"),
            Cube([15, top - 44, -7], [14, 10, 14], "fist"),
        ]),
    ]
    fist, drag, leap, boulder, smash, storm = t(18), t(16), t(22), t(30), t(20), t(30)
    animations = {
        "misc.idle": anim(4.0, {
            "body": rot(0, [0, 0, 0], 2.0, [3, 0, 0], 4.0, [0, 0, 0]),
            "right_arm": rot(0, ZERO, 2.0, [-4, 0, 3], 4.0, ZERO),
            "left_arm": rot(0, ZERO, 2.0, [-4, 0, -3], 4.0, ZERO),
        }),
        "move.walk": anim(1.8, {
            "right_leg": rot(0, [18, 0, 0], 0.9, [-18, 0, 0], 1.8, [18, 0, 0]),
            "left_leg": rot(0, [-18, 0, 0], 0.9, [18, 0, 0], 1.8, [-18, 0, 0]),
            "right_arm": rot(0, [-14, 0, 0], 0.9, [14, 0, 0], 1.8, [-14, 0, 0]),
            "left_arm": rot(0, [14, 0, 0], 0.9, [-14, 0, 0], 1.8, [14, 0, 0]),
            "root": pos(0, ZERO, 0.45, [0, -1, 0], 0.9, ZERO, 1.35, [0, -1, 0], 1.8, ZERO),
        }),
        "attack.fist": anim(fist + 0.5, {
            "right_arm": rot(0, ZERO, fist, [-150, 0, 20], fist + 0.1, [-60, 0, 0], fist + 0.5, ZERO),
            "body": rot(0, [0, 0, 0], fist, [-15, 15, 0], fist + 0.1, [15, -10, 0], fist + 0.5, [0, 0, 0]),
        }, loop=False),
        "attack.drag": anim(drag + 0.5, {
            "left_arm": rot(0, ZERO, drag, [-60, -60, -40], drag + 0.25, [-60, 50, -10], drag + 0.5, ZERO),
            "body": rot(0, [0, 0, 0], drag, [10, -25, 0], drag + 0.25, [10, 25, 0], drag + 0.5, [0, 0, 0]),
        }, loop=False),
        "attack.leap": anim(leap + 0.9, {
            "root": pos(0, ZERO, leap, [0, -6, 0], leap + 0.1, [0, 2, 0], leap + 0.9, ZERO),
            "right_arm": rot(0, ZERO, leap, [40, 0, 10], leap + 0.3, [-170, 0, 10], leap + 0.8, [-40, 0, 0], leap + 0.9, ZERO),
            "left_arm": rot(0, ZERO, leap, [40, 0, -10], leap + 0.3, [-170, 0, -10], leap + 0.8, [-40, 0, 0], leap + 0.9, ZERO),
        }, loop=False),
        "attack.boulder": anim(boulder + 0.4, {
            "right_arm": rot(0, ZERO, boulder * 0.4, [30, 0, 0], boulder, [-170, 0, 15], boulder + 0.15, [-40, 0, 0], boulder + 0.4, ZERO),
            "body": rot(0, [0, 0, 0], boulder * 0.4, [25, 0, 0], boulder, [-20, 0, 0], boulder + 0.4, [0, 0, 0]),
        }, loop=False),
        "attack.smash": anim(smash + 0.5, {
            "right_arm": rot(0, ZERO, smash, [-170, 0, 10], smash + 0.1, [-30, 0, 0], smash + 0.5, ZERO),
            "left_arm": rot(0, ZERO, smash, [-170, 0, -10], smash + 0.1, [-30, 0, 0], smash + 0.5, ZERO),
        }, loop=False),
        "misc.storm": anim(storm + 0.5, {
            "body": rot(0, [0, 0, 0], storm, [-30, 0, 0], storm + 0.5, [0, 0, 0]),
            "head": rot(0, ZERO, storm, [-35, 0, 0], storm + 0.5, ZERO),
            "right_arm": rot(0, ZERO, storm, [-20, 0, 60], storm + 0.5, ZERO),
            "left_arm": rot(0, ZERO, storm, [-20, 0, -60], storm + 0.5, ZERO),
        }, loop=False),
        "misc.shed": anim(1.5, {
            "body": rot(0, [0, 0, 0], 0.3, [-10, 0, 8], 0.6, [-10, 0, -8], 0.9, [-10, 0, 8], 1.5, [0, 0, 0]),
            "plates": pos(0, ZERO, 0.9, [0, -10, 8]),
        }, loop=False),
    }
    paint = painter({"default": (70, 66, 62, 255), "stone": (78, 74, 70, 255), "ash": (98, 94, 90, 255),
                     "plate": (60, 58, 56, 255), "head": (84, 80, 76, 255), "fist": (66, 62, 58, 255),
                     "core": (255, 140, 40, 255)},
                    eye=(255, 160, 60, 255), eye_tags=("head",), eye_row=0.45, glow_tags=("core",),
                    cracks=("stone", "fist"), crack_color=(240, 120, 40, 255))
    write_mob("ash_colossus", bones, 256, [5, 5.5], animations, paint)


# --- Grieving Tree: the king's oak, ~10 blocks, a weeping face in the bark, roots spread while it is rooted ---------

def grieving_tree():
    trunk_h = 104
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("roots", [0, 0, 0], "root", None, [
            Cube([-24, 0, -4], [16, 6, 8], "bark"), Cube([8, 0, -4], [16, 6, 8], "bark"),
            Cube([-4, 0, -24], [8, 6, 16], "bark"), Cube([-4, 0, 8], [8, 6, 16], "bark"),
        ]),
        Bone("right_root_leg", [-8, 20, 0], "root", None, [Cube([-14, 0, -6], [12, 20, 12], "bark")]),
        Bone("left_root_leg", [8, 20, 0], "root", None, [Cube([2, 0, -6], [12, 20, 12], "bark")]),
        Bone("trunk", [0, 20, 0], "root", None, [
            Cube([-12, 20, -12], [24, trunk_h, 24], "bark"),
            Cube([-6, 80, -13], [12, 10, 1], "face"),
        ]),
        Bone("crown", [0, 124, 0], "trunk", None, [Cube([-36, 124, -36], [72, 26, 72], "leaves", inflate=0)]),
    ]
    branches = [("branch_front", [0, 0, -1]), ("branch_back", [0, 0, 1]), ("branch_left", [1, 0, 0]), ("branch_right", [-1, 0, 0])]
    for name, (dx, _, dz) in branches:
        x0 = 10 if dx > 0 else (-50 if dx < 0 else -3)
        z0 = 10 if dz > 0 else (-50 if dz < 0 else -3)
        size = [40 if dx else 6, 6, 40 if dz else 6]
        bones.append(Bone(name, [dx * 10, 104, dz * 10], "trunk", None, [Cube([x0, 104, z0], size, "bark")]))
    roots, branch, cage = t(26), t(20), t(24)
    animations = {
        "misc.idle": anim(5.0, {
            "trunk": rot(0, ZERO, 2.5, [1.5, 0, 1], 5.0, ZERO),
            "crown": rot(0, ZERO, 2.5, [0, 3, 0], 5.0, ZERO),
        }),
        "move.walk": anim(2.4, {
            "right_root_leg": rot(0, [15, 0, 0], 1.2, [-15, 0, 0], 2.4, [15, 0, 0]),
            "left_root_leg": rot(0, [-15, 0, 0], 1.2, [15, 0, 0], 2.4, [-15, 0, 0]),
            "trunk": rot(0, [0, 0, 3], 1.2, [0, 0, -3], 2.4, [0, 0, 3]),
        }),
        "attack.roots": anim(roots + 0.5, {
            "trunk": rot(0, ZERO, roots, [-6, 0, 0], roots + 0.1, [4, 0, 0], roots + 0.5, ZERO),
            "roots": pos(0, ZERO, roots, [0, -2, 0], roots + 0.1, [0, 2, 0], roots + 0.5, ZERO),
        }, loop=False),
        "attack.branch": anim(branch + 0.6, {
            "branch_front": rot(0, ZERO, branch, [-40, -50, 0], branch + 0.15, [55, 40, 0], branch + 0.6, [55, 40, 0]),
            "trunk": rot(0, ZERO, branch, [0, 15, 0], branch + 0.15, [6, -15, 0], branch + 0.6, ZERO),
        }, loop=False),
        "attack.cage": anim(cage + 0.5, {
            "crown": rot(0, ZERO, cage, [-10, 0, 0], cage + 0.5, ZERO),
            "trunk": rot(0, ZERO, cage, [8, 0, 0], cage + 0.1, [-4, 0, 0], cage + 0.5, ZERO),
        }, loop=False),
        "misc.thralls": anim(1.0, {
            "crown": rot(0, ZERO, 0.3, [0, 0, 6], 0.6, [0, 0, -6], 1.0, ZERO),
        }, loop=False),
        "misc.stunned": anim(5.0, {
            "trunk": rot(0, ZERO, 0.4, [12, 0, 0], 4.5, [12, 0, 0], 5.0, ZERO),
            "crown": rot(0, ZERO, 0.4, [8, 0, 0], 4.5, [8, 0, 0], 5.0, ZERO),
        }, loop=False),
        "misc.uproot": anim(2.0, {
            "root": pos(0, ZERO, 0.6, [0, -4, 0], 1.2, [0, 6, 0], 2.0, ZERO),
            "trunk": rot(0, ZERO, 0.6, [0, 0, 8], 1.2, [0, 0, -8], 2.0, ZERO),
        }, loop=False),
    }
    paint = painter({"default": (64, 54, 44, 255), "bark": (70, 58, 46, 255), "leaves": (58, 66, 50, 255),
                     "face": (40, 32, 26, 255)},
                    eye=(255, 200, 120, 255), eye_tags=("face",), eye_row=0.3, cracks=("bark",),
                    crack_color=(120, 96, 60, 255))
    write_mob("grieving_tree", bones, 512, [8, 11], animations, paint)


# --- Smoldering Lord: the greedy king, 3.5 blocks, burning plate (shatters in P2), a crown, a greatsword ------------

def smoldering_lord():
    legs, body_h, head = 22, 22, 10
    top = legs + body_h
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("right_leg", [-3.5, legs, 0], "root", None, [Cube([-7, 0, -3.5], [7, legs, 7], "body")]),
        Bone("left_leg", [3.5, legs, 0], "root", None, [Cube([0, 0, -3.5], [7, legs, 7], "body")]),
        Bone("body", [0, legs, 0], "root", None, [Cube([-8, legs, -4.5], [16, body_h, 9], "body")]),
        Bone("armor", [0, legs, 0], "body", None, [
            Cube([-8.5, legs + 6, -5], [17, 16, 10], "plate", inflate=0.5),
            Cube([-12, top - 4, -5], [7, 5, 10], "plate"),
            Cube([5, top - 4, -5], [7, 5, 10], "plate"),
        ]),
        Bone("head", [0, top, 0], "body", None, [
            Cube([-5, top, -5], [10, head, 10], "head"),
            Cube([-5.5, top + head, -5.5], [11, 3, 11], "crown"),
        ]),
        Bone("right_arm", [-10.5, top - 2, 0], "body", None, [Cube([-13, top - 22, -2.5], [5, 22, 5], "body")]),
        Bone("left_arm", [10.5, top - 2, 0], "body", None, [Cube([8, top - 22, -2.5], [5, 22, 5], "body")]),
        Bone("greatsword", [-10.5, top - 20, 0], "right_arm", None, [
            Cube([-11.5, top - 24, -1], [2, 6, 2], "hilt"),
            Cube([-14, top - 18, -1], [7, 2, 2], "hilt"),
            Cube([-12, top - 16, -1.5], [3, 36, 3], "blade"),
        ]),
    ]
    s1, s2, slam, lunge, mortar, line, descend = t(14), t(10), t(20), t(18), 1.0, t(30), t(30)
    animations = {
        "misc.idle": anim(3.0, {
            "body": rot(0, ZERO, 1.5, [2, 0, 0], 3.0, ZERO),
            "right_arm": rot(0, [-20, 0, 0], 1.5, [-24, 0, 0], 3.0, [-20, 0, 0]),
        }),
        "move.walk": anim(1.2, {
            "right_leg": rot(0, [25, 0, 0], 0.6, [-25, 0, 0], 1.2, [25, 0, 0]),
            "left_leg": rot(0, [-25, 0, 0], 0.6, [25, 0, 0], 1.2, [-25, 0, 0]),
            "left_arm": rot(0, [15, 0, 0], 0.6, [-15, 0, 0], 1.2, [15, 0, 0]),
            "right_arm": rot(0, [-20, 0, 0], 1.2, [-20, 0, 0]),
        }),
        "attack.greatsword_1": anim(s1 + 0.35, {
            "right_arm": rot(0, [-20, 0, 0], s1, [-90, -80, 0], s1 + 0.1, [-90, 70, 0], s1 + 0.35, [-20, 0, 0]),
            "body": rot(0, ZERO, s1, [0, 30, 0], s1 + 0.1, [0, -30, 0], s1 + 0.35, ZERO),
        }, loop=False),
        "attack.greatsword_2": anim(s2 + 0.3, {
            "right_arm": rot(0, [-90, 70, 0], s2, [-95, 80, 0], s2 + 0.1, [-90, -70, 0], s2 + 0.3, [-20, 0, 0]),
            "body": rot(0, [0, -30, 0], s2, [0, -35, 0], s2 + 0.1, [0, 30, 0], s2 + 0.3, ZERO),
        }, loop=False),
        "attack.slam": anim(slam + 0.5, {
            "right_arm": rot(0, [-20, 0, 0], slam, [-180, 0, 10], slam + 0.08, [-60, 0, 0], slam + 0.5, [-20, 0, 0]),
            "left_arm": rot(0, ZERO, slam, [-170, 0, -10], slam + 0.08, [-60, 0, 0], slam + 0.5, ZERO),
            "body": rot(0, ZERO, slam, [-10, 0, 0], slam + 0.08, [25, 0, 0], slam + 0.5, ZERO),
        }, loop=False),
        "attack.lunge": anim(lunge + 0.5, {
            "right_arm": rot(0, [-20, 0, 0], lunge, [20, 0, 0], lunge + 0.08, [-90, 0, 0], lunge + 0.5, [-20, 0, 0]),
            "body": rot(0, ZERO, lunge, [-15, 0, 0], lunge + 0.08, [25, 0, 0], lunge + 0.5, ZERO),
            "root": pos(0, ZERO, lunge, [0, -2, 2], lunge + 0.1, [0, 0, -4], lunge + 0.5, ZERO),
        }, loop=False),
        "attack.mortar": anim(mortar, {
            "left_arm": rot(0, ZERO, 0.4, [-170, 0, -20], 0.6, [-140, 0, -20], mortar, ZERO),
        }, loop=False),
        "attack.flame_line": anim(line + 0.4, {
            "right_arm": rot(0, [-20, 0, 0], line, [-170, 0, 0], line + 0.1, [-10, 0, 0], line + 0.4, [-20, 0, 0]),
        }, loop=False),
        "misc.descend": anim(descend + 0.3, {
            "root": pos(0, ZERO, descend * 0.6, [0, -4, 0], descend, [0, 6, 0], descend + 0.3, ZERO),
            "right_arm": rot(0, [-20, 0, 0], descend, [-170, 0, 0], descend + 0.3, [-20, 0, 0]),
        }, loop=False),
        "misc.shatter": anim(1.2, {
            "body": rot(0, ZERO, 0.3, [-15, 0, 0], 0.6, [10, 0, 0], 1.2, ZERO),
            "armor": pos(0, ZERO, 0.4, [0, 0, 3], 0.6, [0, -6, 4]),
        }, loop=False),
        "misc.hazard": anim(1.5, {
            "right_arm": rot(0, [-20, 0, 0], 0.7, [-30, 0, 70], 1.5, [-20, 0, 0]),
            "left_arm": rot(0, ZERO, 0.7, [-30, 0, -70], 1.5, ZERO),
            "head": rot(0, ZERO, 0.7, [-30, 0, 0], 1.5, ZERO),
        }, loop=False),
    }
    paint = painter({"default": (52, 40, 36, 255), "body": (48, 36, 32, 255), "plate": (70, 58, 50, 255),
                     "head": (40, 30, 26, 255), "crown": (200, 150, 50, 255), "hilt": (90, 70, 40, 255),
                     "blade": (120, 70, 50, 255)},
                    eye=(255, 140, 40, 255), eye_tags=("head",), eye_row=0.45, glow_tags=("blade",),
                    cracks=("plate", "body"), crack_color=(255, 120, 30, 255))
    write_mob("smoldering_lord", bones, 128, [3, 4], animations, paint)


BOSSES = [("crypt_keeper", crypt_keeper), ("ash_colossus", ash_colossus),
          ("grieving_tree", grieving_tree),
          ("smoldering_lord", smoldering_lord)]


if __name__ == "__main__":
    wanted = set(sys.argv[1:])
    for name, build in BOSSES:
        if not wanted or name in wanted:
            build()
    print("Wrote greybox boss assets")
