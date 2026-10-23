#!/usr/bin/env python3
"""Final models, animations and textures for the four Ashen Reach bosses (PLAN R40.2c).

Run from the repository root: python3 tools/reach_boss_art.py [name ...]
Replaces the greybox of the removed tools/boss_placeholders.py at the same size, with the bones the code toggles
(Ash Colossus plates / core, Crypt Keeper lantern, Grieving Tree roots, Smoldering Lord armor) and the same
animation names and timings: each attack animation contains its windup and lands at the tick the server lands the
move. Check with python3 tools/model_preview.py check <name> && python3 tools/model_preview.py entity <name>.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from art_kit import ZERO, Bone, Box, Material, anim, ember_cracks, hexc, keys, mirror_box, pos, ramp, rot, scatter, \
    shade, t, write_model  # noqa: E402
from reach_mob_art import (ASH, BARK, BONE, BRONZE, CHAR, COLD, CRIMSON, EMBER, EMBER_DARK, EMBER_DEEP,  # noqa: E402
                           EMBER_HOT, GOLD, IRON, LEATHER, MOSS, ROPE, RUST, SHROUD, VOID, WOOD, AMBER,
                           BLACK_STEEL, band, clear_all, crack, holes, lower, open_front, ragged_edge, rivets, tatter,
                           trim)


def bone(bones, name):
    return next(b for b in bones if b.name == name)


# --- Crypt Keeper: 4 blocks, the hooded gaoler of the ossuary --------------------------------------------------------
# A gaunt giant in a heavy grave-blue robe wrapped in chains, a faceless hood with cold eyes, a mantle of bones, keys
# and skull trophies at the belt, a long halberd and a cold-flame lantern (the lantern bone; it shatters in P2).

GRAVE_ROBE = ramp("15181f", "1d212b", "262c38", "313846", "3e4656", "4d5668")
COLD_FLAME = ramp("2f6f9a", "4fa3d1", "7fd0f5", "b8ecff", "e8fbff")


def crypt_keeper():
    legs, body_h, head = 26, 24, 13
    top = legs + body_h
    robe = Material(GRAVE_ROBE, base=2.4, noise=0.6, grain="v", edge=0.5, grad=0.8)

    def chains(face, rng):
        if face.name in ("front", "back"):
            for i in range(face.h):  # a chain across the chest, corner to corner
                x = int(i * face.w / face.h)
                if 0 <= x < face.w and i % 2 == 0:
                    face.set(x, i, IRON[4])
                    if face.inside(x + 1, i):
                        face.set(x + 1, i, IRON[2])
        if face.side():
            band(face, face.h - 4, LEATHER[1])
            band(face, face.h - 3, LEATHER[0])

    def belt(face, rng):
        if face.name == "front":
            for x in range(2, face.w - 2, 4):  # keys on a ring
                face.vline(x, 1, 3, BRONZE[4])
                face.set(x + 1, 3, BRONZE[3])
        if face.side():
            tatter(face, rng, 3, 0.5)

    def hood_inner(face, rng):
        face.rect(0, 0, face.w, face.h, VOID)
        if face.name == "front":
            for ex in (3, face.w - 5):
                face.lit(ex, 5, COLD, 1.0)
                face.lit(ex + 1, 5, COLD, 1.0)
                face.lit(ex, 6, hexc("4f7ea8"), 0.6)
            for x in range(3, face.w - 3, 2):  # a jaw of teeth deep in the dark
                face.set(x, 9, BONE[1])

    def bone_mantle(face, rng):
        if face.side() or face.name == "top":
            for x in range(0, face.w, 2):
                face.vline(x, 0, face.h, BONE[3] if (x // 2) % 2 else BONE[2])
            for x in range(1, face.w, 4):
                face.set(x, face.h - 1, VOID)

    def skull(face, rng):
        if face.name == "front":
            face.set(1, 1, VOID)
            face.set(face.w - 2, 1, VOID)
            face.hline(1, face.h - 1, face.w - 2, BONE[1])

    bones = [
        Bone("root", [0, 0, 0]),
        Bone("right_leg", [-3.5, legs, 0], "root", None, [
            Box([-7, 0, -3.5], [7, legs, 7], robe.but(base=1.8), {"sides": lambda f, r: lower(f, 4, LEATHER, r, 1.4)})]),
        Bone("left_leg", [3.5, legs, 0], "root", None, []),
        Bone("body", [0, legs, 0], "root", None, [
            Box([-8, legs, -5], [16, body_h, 10], robe, {"all": chains}),
            Box([-9, 8, -6], [18, 18, 12], robe.but(base=2.0), {"all": belt}),
            Box([-9, top - 4, -6], [18, 4, 12], Material(BONE, base=2.6, noise=0.5), {"all": bone_mantle}),
            Box([-7, legs - 3, -6.8], [4, 4, 4], Material(BONE, base=3.0, noise=0.5), {"all": skull}),
            Box([3, legs - 4, -6.6], [4, 4, 4], Material(BONE, base=2.6, noise=0.5), {"all": skull}),
        ]),
        Bone("head", [0, top, 0], "body", None, [
            Box([-5.5, top, -5.5], [11, 11, 11], robe, {"all": hood_inner}),
            Box([-6.5, top, -6.5], [13, 13, 13], robe.but(base=2.8, top=0.6),
                {"front": lambda f, r: open_front(f, 2, 2), "bottom": clear_all,
                 "left": lambda f, r: ragged_edge(f, r, "bottom", 1, 0.4),
                 "right": lambda f, r: ragged_edge(f, r, "bottom", 1, 0.4)}),
            Box([-2.5, top + 9, 3], [5, 5, 6], robe.but(base=2.2), rotation=[-30, 0, 0], pivot=[0, top + 11, 4]),
        ]),
    ]
    bone(bones, "left_leg").add(mirror_box(bone(bones, "right_leg").boxes[0]))
    arm = Box([-14, top - 26, -3], [6, 26, 6], robe.but(base=2.2),
              {"sides": lambda f, r: (lower(f, 4, BONE, r, 1.6), band(f, f.h - 5, IRON[3]))})
    pauldron = Box([-14.5, top - 7, -3.5], [7, 6, 7], Material(BONE, base=3.0, noise=0.5, top=0.6),
                   {"all": lambda f, r: (bone_mantle(f, r), f.side() and f.name == "right" and skull(f, r))})
    bones.append(Bone("right_arm", [-11, top - 2, 0], "body", None, [arm, pauldron]))
    bones.append(Bone("left_arm", [11, top - 2, 0], "body", None, [mirror_box(arm), mirror_box(pauldron)]))
    wood = Material(WOOD, base=1.4, noise=0.5, grain="v")
    iron = Material(IRON, base=2.8, noise=0.6, speck=0.08, top=0.6)

    def blade(face, rng):
        if face.name in ("front", "back"):
            # A crescent: the corners on the shaft side cut away, a bright edge on the outer side.
            w, h = face.w, face.h
            outer = 0 if face.name == "front" else w - 1   # the blade's outer edge is at -x of the shaft
            inner = w - 1 - outer
            for y in range(h):
                if y < 2 or y > h - 3:
                    face.clear(inner, y)
                    if y == 0 or y == h - 1:
                        face.clear(inner - (1 if inner else -1), y)
                face.set(outer, y, IRON[5])
            scatter(face, rng, 3, RUST[2])

    bones.append(Bone("halberd", [-11, top - 24, 0], "right_arm", None, [
        Box([-12, 2, -1], [2, 62, 2], wood, {"sides": lambda f, r: [f.hline(0, y, f.w, IRON[2]) for y in (6, 20, 40)]}),
        Box([-12.5, 46, -1.5], [3, 2, 3], Material(BRONZE, base=3.0, noise=0.4)),
        Box([-19, 48, -0.5], [7, 12, 1], iron, {"all": blade}),
        Box([-10, 54, -0.5], [3, 4, 1], iron.but(base=2.2)),
        Box([-11.5, 64, -0.5], [1, 5, 1], iron.but(base=3.4)),
    ]))
    flame = Material(COLD_FLAME, base=2.5, noise=0.8, glow=0.9)

    def cage(face, rng):
        if face.side():
            face.vline(0, 0, face.h, IRON[1])
            face.vline(face.w - 1, 0, face.h, IRON[1])
            face.hline(0, face.h // 2, face.w, IRON[2])
            face.lit(face.cx, face.h // 2 - 1, hexc("e8fbff"), 1.0)

    bones.append(Bone("lantern", [11, top - 26, 0], "left_arm", None, [
        Box([10.5, top - 32, -0.5], [1, 6, 1], iron.but(base=1.8)),
        Box([7.5, top - 33, -3.5], [7, 1, 7], iron.but(base=2.2)),
        Box([8, top - 40, -3], [6, 7, 6], flame, {"all": cage}),
        Box([7.5, top - 41, -3.5], [7, 1, 7], iron.but(base=1.8)),
    ]))
    swing1, swing2, overhead, sweep, throw, thrust = t(14), t(10), t(22), t(24), t(20), t(18)
    animations = {
        "misc.idle": anim(3.0, {
            "body": rot(0, ZERO, 1.5, [2, 0, 0], 3.0, ZERO),
            "lantern": rot(0, [0, 0, -4], 1.5, [0, 0, 4], 3.0, [0, 0, -4]),
            "head": rot(0, ZERO, 1.5, [4, -3, 0], 3.0, ZERO),
        }),
        "move.walk": anim(1.4, {
            "right_leg": rot(0, [22, 0, 0], 0.7, [-22, 0, 0], 1.4, [22, 0, 0]),
            "left_leg": rot(0, [-22, 0, 0], 0.7, [22, 0, 0], 1.4, [-22, 0, 0]),
            "left_arm": rot(0, [10, 0, 0], 0.7, [-10, 0, 0], 1.4, [10, 0, 0]),
            "lantern": rot(0, [-8, 0, 0], 0.7, [8, 0, 0], 1.4, [-8, 0, 0]),
            "root": pos(0, ZERO, 0.35, [0, 0.8, 0], 0.7, ZERO, 1.05, [0, 0.8, 0], 1.4, ZERO),
        }),
        "attack.halberd_1": anim(swing1 + 0.35, {
            "right_arm": rot(0, ZERO, swing1, [-80, -70, 0], swing1 + 0.1, [-80, 60, 0], swing1 + 0.35, ZERO),
            "body": rot(0, ZERO, swing1, [0, 25, 0], swing1 + 0.1, [0, -30, 0], swing1 + 0.35, ZERO),
        }, loop=False),
        "attack.halberd_2": anim(swing2 + 0.3, {
            "right_arm": rot(0, [-80, 60, 0], swing2, [-85, 70, 0], swing2 + 0.1, [-80, -60, 0], swing2 + 0.3, ZERO),
            "body": rot(0, [0, -30, 0], swing2, [0, -35, 0], swing2 + 0.1, [0, 30, 0], swing2 + 0.3, ZERO),
        }, loop=False),
        "attack.overhead": anim(overhead + 0.5, {
            "right_arm": rot(0, ZERO, overhead, [-175, 0, 15], overhead + 0.08, [-40, 0, 0], overhead + 0.5, ZERO),
            "left_arm": rot(0, ZERO, overhead, [-175, 0, -15], overhead + 0.08, [-40, 0, 0], overhead + 0.5, ZERO),
            "body": rot(0, ZERO, overhead, [-12, 0, 0], overhead + 0.08, [25, 0, 0], overhead + 0.5, ZERO),
        }, loop=False),
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
    write_model("crypt_keeper", bones, animations, [3, 4.5], tex_w=128)


# --- Ash Colossus: 4.5 blocks of fused ash stone ---------------------------------------------------------------------
# Hunched, with huge arms and fists; ash drifts on its shoulders and head; magma seams; old granary bricks fused into
# its body; an ember core in the back under stone plates strapped with burnt chains (plates bone; shed in P2 to show
# the core bone).

FUSED = ramp("1f1c1c", "2c2828", "3a3535", "484343", "585252", "6b6464")
ASH_DRIFT = ramp("5e5955", "77716b", "908982", "aaa29a", "c1b9b0")


def ash_colossus():
    legs, body_h, head = 22, 36, 12
    top = legs + body_h
    stone = Material(FUSED, base=2.7, noise=0.8, cell=3.5, speck=0.04, edge=0.8, grad=0.8, top=1.2)

    def drift(face, rng):
        """Ash settled on top faces and on the upper rows of side faces."""
        if face.name == "top":
            for y in range(face.h):
                for x in range(face.w):
                    if rng.random() < 0.8:
                        face.set(x, y, ASH_DRIFT[min(4, 1 + int(rng.random() * 3))])
        elif face.side():
            for x in range(face.w):
                for y in range(rng.randint(0, 2)):
                    face.set(x, y, ASH_DRIFT[1 + y])

    def seams(face, rng, n=None):
        if face.side():
            ember_cracks(face, rng, n or max(1, face.w * face.h // 200), EMBER, EMBER_DEEP, 0.9, (3, 7))

    def masonry(face, rng):
        """A patch of fused granary bricks: two courses in a frame of mortar, joints staggered."""
        if face.side() and face.w >= 12 and face.h >= 10:
            x0, y0 = rng.randrange(face.w - 10), rng.randrange(face.h - 7)
            for y in (y0, y0 + 3, y0 + 6):
                face.hline(x0, y, 10, FUSED[0])
            for y in range(y0, y0 + 7):
                face.set(x0, y, FUSED[0])
                face.set(x0 + 9, y, FUSED[0])
            for y in (y0 + 1, y0 + 2):
                face.set(x0 + 5, y, FUSED[0])
            for y in (y0 + 4, y0 + 5):
                face.set(x0 + 3, y, FUSED[0])
                face.set(x0 + 7, y, FUSED[0])
            for y in range(y0 + 1, y0 + 6):
                for x in range(x0 + 1, x0 + 9):
                    if face.get(x, y)[:3] != FUSED[0][:3]:
                        face.set(x, y, FUSED[3] if (x + y) % 3 else FUSED[2])

    def body_deco(face, rng):
        drift(face, rng)
        masonry(face, rng)
        seams(face, rng)

    def back(face, rng):
        body_deco(face, rng)
        if face.name == "back":  # the cavity the core sits in, with a glowing rim
            cx, cy = face.cx, face.h // 2 - 2
            for y in range(cy - 7, cy + 7):
                for x in range(cx - 7, cx + 7):
                    if abs(x - cx + 0.5) + abs(y - cy + 0.5) < 9:
                        face.set(x, y, VOID)
                    elif abs(x - cx + 0.5) + abs(y - cy + 0.5) < 10:
                        face.lit(x, y, EMBER_DEEP, 0.8)

    def head_front(face, rng):
        face.hline(1, 4, face.w - 2, FUSED[0])  # brow shadow
        for ex in (2, face.w - 5):
            face.rect(ex, 5, 3, 2, VOID)
            face.lit(ex + 1, 5, EMBER_HOT, 1.0)
            face.lit(ex + 1, 6, EMBER, 0.8)
        face.hline(3, 9, face.w - 6, VOID)
        for x in range(3, face.w - 3, 2):
            face.lit(x, 9, EMBER_DEEP, 0.7)

    def fist(face, rng):
        drift(face, rng) if face.name == "top" else None
        if face.name in ("front", "bottom"):
            for x in range(0, face.w, 4):  # knuckles
                face.vline(x, 0, face.h, FUSED[0])
        seams(face, rng, 1)

    bones = [
        Bone("root", [0, 0, 0]),
        Bone("right_leg", [-7, legs, 0], "root", None, [
            Box([-13, 2, -6], [12, legs - 2, 12], stone, {"all": body_deco}),
            Box([-14, 0, -7.5], [14, 3, 14], stone.but(base=2.0), {"all": seams}),
            Box([-12, 10, -7], [10, 6, 2], stone.but(base=3.2), {"all": drift}),
        ]),
        Bone("left_leg", [7, legs, 0], "root", None, []),
        Bone("body", [0, legs, 0], "root", [15, 0, 0], [
            Box([-17, legs, -11], [34, body_h, 20], stone, {"all": back}),
            Box([-13, legs + 18, -13], [26, 16, 3], stone.but(base=3.2), {"all": body_deco}),
            Box([-11, legs - 2, -9], [22, 6, 17], stone.but(base=2.2), {"all": seams}),
        ]),
        Bone("core", [0, legs + 18, 11], "body", None, [
            Box([-6, legs + 12, 8], [12, 12, 4], Material(ramp("8a2a10", "d8521c", "ff8a2a", "ffb347", "ffd27a",
                                                               "fff0c0"), base=3.0, noise=0.9, cell=2.0, glow=1.0)),
            Box([-3, legs + 15, 11], [6, 6, 2], Material(ramp("ffb347", "ffd27a", "fff0c0"), base=1.5, noise=0.6,
                                                         glow=1.0)),
        ]),
    ]
    bone(bones, "left_leg").boxes = [mirror_box(b) for b in bone(bones, "right_leg").boxes]

    def plate(face, rng):
        drift(face, rng)
        if face.name in ("back", "front"):
            for y in (3, face.h - 4):  # burnt chains strapping the plates on
                for x in range(face.w):
                    face.set(x, y, IRON[1] if x % 2 else IRON[3])
        if face.side():
            ember_cracks(face, rng, 1, EMBER_DEEP, EMBER_DARK, 0.7, (2, 4))

    bones.append(Bone("plates", [0, legs + 18, 12], "body", None, [
        Box([-12, legs + 8, 11], [24, 22, 4], stone.but(base=2.4), {"all": plate}),
        Box([-20, top - 6, -9], [8, 8, 16], stone.but(base=3.0), {"all": plate}),
        Box([12, top - 6, -9], [8, 8, 16], stone.but(base=3.0), {"all": plate}),
    ]))
    bones.append(Bone("head", [0, top, -6], "body", None, [
        Box([-6, top - 2, -14], [12, head, 12], stone.but(base=3.0), {"front": head_front, "top": drift,
                                                                      "left": drift, "right": drift}),
        Box([-7, top + 6, -15], [14, 3, 5], stone.but(base=3.4), {"all": drift}),
        Box([-5, top - 3, -15], [10, 3, 7], stone.but(base=2.2), {"all": seams}),
    ]))
    arm = Box([-28, top - 34, -6], [12, 30, 12], stone, {"all": body_deco})
    shoulder = Box([-30, top - 8, -8], [15, 9, 16], stone.but(base=3.2), {"all": body_deco})
    hand = Box([-29, top - 44, -7], [14, 10, 14], stone.but(base=2.4), {"all": fist})
    bones.append(Bone("right_arm", [-20, top - 4, 0], "body", None, [arm, shoulder, hand]))
    bones.append(Bone("left_arm", [20, top - 4, 0], "body", None, [mirror_box(b) for b in (arm, shoulder, hand)]))
    fist_t, drag, leap, boulder, smash, storm = t(18), t(16), t(22), t(30), t(20), t(30)
    animations = {
        "misc.idle": anim(4.0, {
            "body": rot(0, [0, 0, 0], 2.0, [3, 0, 0], 4.0, [0, 0, 0]),
            "right_arm": rot(0, ZERO, 2.0, [-4, 0, 3], 4.0, ZERO),
            "left_arm": rot(0, ZERO, 2.0, [-4, 0, -3], 4.0, ZERO),
            "head": rot(0, ZERO, 2.0, [4, 5, 0], 4.0, ZERO),
        }),
        "move.walk": anim(1.8, {
            "right_leg": rot(0, [18, 0, 0], 0.9, [-18, 0, 0], 1.8, [18, 0, 0]),
            "left_leg": rot(0, [-18, 0, 0], 0.9, [18, 0, 0], 1.8, [-18, 0, 0]),
            "right_arm": rot(0, [-14, 0, 0], 0.9, [14, 0, 0], 1.8, [-14, 0, 0]),
            "left_arm": rot(0, [14, 0, 0], 0.9, [-14, 0, 0], 1.8, [14, 0, 0]),
            "root": pos(0, ZERO, 0.45, [0, -1, 0], 0.9, ZERO, 1.35, [0, -1, 0], 1.8, ZERO),
            "body": rot(0, [0, 0, 3], 0.9, [0, 0, -3], 1.8, [0, 0, 3]),
        }),
        "attack.fist": anim(fist_t + 0.5, {
            "right_arm": rot(0, ZERO, fist_t, [-150, 0, 20], fist_t + 0.1, [-60, 0, 0], fist_t + 0.5, ZERO),
            "body": rot(0, [0, 0, 0], fist_t, [-15, 15, 0], fist_t + 0.1, [15, -10, 0], fist_t + 0.5, [0, 0, 0]),
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
            "right_arm": rot(0, ZERO, boulder * 0.4, [30, 0, 0], boulder, [-170, 0, 15], boulder + 0.15, [-40, 0, 0],
                             boulder + 0.4, ZERO),
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
    write_model("ash_colossus", bones, animations, [5, 5.5], tex_w=256)


# --- Grieving Tree: the king's oak, ~10 blocks -----------------------------------------------------------------------
# Grey dead bark with moss, a weeping face whose sap tears glow amber, the king's iron crown band nailed round the
# trunk, a withered canopy, frayed lantern ropes, and limbs reaching out over the lantern ring (the lanterns hang
# 5.5 blocks out at 7-8.5 blocks while rooted, 3.5 out at 5.5-7 once it walks). The roots bone shows while rooted.

DEAD_OAK = ramp("1b1713", "27211b", "352d25", "443a30", "54493d", "665a4c")
WITHERED = ramp("1c1c15", "27271c", "333223", "403d2a", "4f4a32", "5f583b")


def grieving_tree():
    trunk_h = 104
    bark = Material(DEAD_OAK, base=2.6, noise=0.7, grain="v", grad=0.5, top=0.4, speck=0.03)

    def bark_deco(face, rng):
        if face.side():
            for _ in range(max(1, face.w // 3)):
                x, y = rng.randrange(face.w), rng.randrange(face.h)
                face.vline(x, y, rng.randint(4, 12), DEAD_OAK[0])
            for _ in range(face.w * face.h // 70):
                x, y = rng.randrange(face.w), rng.randrange(face.h)
                face.rect(x, y, rng.randint(1, 4), rng.randint(1, 2), MOSS[rng.randint(0, 3)])

    def trunk_deco(face, rng):
        bark_deco(face, rng)
        if face.side():
            # The crown band: tarnished iron with points, 34-37 px below the top of the trunk.
            y = face.h - 54
            face.rect(0, y, face.w, 3, IRON[2])
            face.hline(0, y, face.w, IRON[4])
            for x in range(1, face.w, 4):
                face.vline(x, y - 2, 2, IRON[3])
                face.set(x, y - 3, GOLD[2])
        if face.name == "front":
            weeping_face(face, rng)

    def weeping_face(face, rng):
        # Rows from the top: the face sits around model y 80-92 (trunk spans 20-124).
        y0 = 124 - 92
        cx = face.cx
        for ex in (cx - 7, cx + 3):
            face.rect(ex, y0, 4, 3, DEAD_OAK[0])
            face.rect(ex + 1, y0 + 1, 2, 2, VOID)
            face.lit(ex + 1, y0 + 2, AMBER, 0.9)
            for k in range(3, rng.randint(9, 14)):  # sap tears
                face.lit(ex + 2, y0 + k, AMBER if k < 7 else hexc("c97a2a"), 1.0 - k * 0.05)
        face.hline(cx - 8, y0 - 2, 6, DEAD_OAK[5])  # sorrowing brows, tilted
        face.hline(cx + 2, y0 - 2, 6, DEAD_OAK[5])
        face.set(cx - 8, y0 - 1, DEAD_OAK[5])
        face.set(cx + 7, y0 - 1, DEAD_OAK[5])
        face.rect(cx - 3, y0 + 7, 6, 5, DEAD_OAK[0])  # the mouth, open in a moan
        face.rect(cx - 2, y0 + 8, 4, 4, VOID)

    leaves = Material(WITHERED, base=2.4, noise=0.9, cell=2.0, speck=0.08, top=0.8, grad=0.6)

    def curtain(face, rng):
        """The weeping fringe: strands of dead leaves and moss hanging from the canopy, ragged and gappy."""
        if face.name in ("left", "right"):
            for x in range(face.w):
                if x % 3 == 2 or rng.random() < 0.15:
                    for y in range(face.h):
                        face.clear(x, y)
                    continue
                for k in range(rng.randint(0, face.h // 2)):
                    face.clear(x, face.h - 1 - k)
                if rng.random() < 0.3:
                    face.vline(x, rng.randrange(face.h // 2), 3, MOSS[1])

    def canopy(face, rng):
        for y in range(face.h):
            for x in range(face.w):
                if rng.random() < (0.22 if face.name == "bottom" else 0.1):
                    face.clear(x, y)
        if face.side():
            ragged_edge(face, rng, "bottom", 3, 0.5)
            for _ in range(face.w // 6):  # hanging moss strands
                x = rng.randrange(face.w)
                face.vline(x, face.h - 3, 3, MOSS[1])

    bones = [
        Bone("root", [0, 0, 0]),
        Bone("roots", [0, 0, 0], "root", None, [
            Box([-30, 0, -4], [22, 6, 8], bark, {"all": bark_deco}, rotation=[0, 0, -8], pivot=[-10, 3, 0]),
            Box([8, 0, -4], [22, 6, 8], bark, {"all": bark_deco}, rotation=[0, 0, 8], pivot=[10, 3, 0]),
            Box([-4, 0, -30], [8, 6, 22], bark, {"all": bark_deco}, rotation=[8, 0, 0], pivot=[0, 3, -10]),
            Box([-4, 0, 8], [8, 6, 22], bark, {"all": bark_deco}, rotation=[-8, 0, 0], pivot=[0, 3, 10]),
        ] + [Box([-3, 0, 8], [6, 5, 18], bark.but(base=2.2), {"all": bark_deco}, rotation=[-6, a, 0],
                 pivot=[0, 2, 0]) for a in (45, 135, -45, -135)]),
        Bone("right_root_leg", [-8, 20, 0], "root", None, [
            Box([-14, 0, -6], [12, 20, 12], bark, {"all": bark_deco}),
            Box([-16, 0, -8], [16, 4, 16], bark.but(base=2.0), {"all": bark_deco})]),
        Bone("left_root_leg", [8, 20, 0], "root", None, []),
        Bone("trunk", [0, 20, 0], "root", None, [
            Box([-12, 20, -12], [24, trunk_h, 24], bark, {"all": trunk_deco}),
            Box([-14, 20, -14], [28, 10, 28], bark.but(base=2.0), {"all": bark_deco}),
            Box([-10, 112, -10], [20, 16, 20], bark.but(base=2.8), {"all": bark_deco}),
        ] + [Box([10, 114, -3], [58, 6, 6], bark.but(base=2.8), {"all": bark_deco}, rotation=[0, a, -12],
                 pivot=[0, 117, 0]) for a in (45, 135, -45, -135)]),
        Bone("crown", [0, 124, 0], "trunk", None, [
            Box([-34, 128, -34], [68, 18, 68], leaves, {"all": canopy}),
            Box([-24, 144, -24], [48, 8, 48], leaves.but(base=3.0), {"all": canopy}),
        ] + [Box([24, 123 + (k % 2) * 6, -18], [52 - (k % 2) * 10, 14, 36], leaves.but(base=2.3 + (k % 2) * 0.4),
                 {"all": canopy}, rotation=[0, k * 45, 0], pivot=[0, 130, 0]) for k in range(8)]
          + [Box([74 - (k % 2) * 10, 100 + (k % 2) * 6, -17], [0, 24, 34], leaves.but(base=1.8), {"all": curtain},
                 rotation=[0, k * 45, 0], pivot=[0, 130, 0]) for k in range(8)]),
    ]
    bone(bones, "left_root_leg").boxes = [mirror_box(b) for b in bone(bones, "right_root_leg").boxes]
    rope = Material(ROPE, base=1.8, noise=0.6, grain="v")
    branches = [("branch_front", [0, 0, -1]), ("branch_back", [0, 0, 1]), ("branch_left", [1, 0, 0]),
                ("branch_right", [-1, 0, 0])]
    for name, (dx, _, dz) in branches:
        # A limb out to 60 px (the P2 lanterns hang 56 px out), rising a little, with a frayed rope at its end.
        yaw = {(0, -1): -90, (0, 1): 90, (1, 0): 180, (-1, 0): 0}[(dx, dz)]
        limb = Box([-62, 102, -3], [52, 6, 6], bark.but(base=3.0), {"all": bark_deco}, rotation=[0, yaw, -10],
                   pivot=[0, 105, 0])
        twig = Box([-60, 108, -2], [4, 18, 4], bark.but(base=3.2), rotation=[0, yaw, -35], pivot=[0, 105, 0])
        cord = Box([-58, 92, -0.5], [1, 12, 1], rope, {"sides": lambda f, r: ragged_edge(f, r, "bottom", 2, 0.7)},
                   rotation=[0, yaw, 0], pivot=[0, 105, 0])
        bones.append(Bone(name, [dx * 10, 104, dz * 10], "trunk", None, [limb, twig, cord]))
    roots_t, branch, cage = t(26), t(20), t(24)
    animations = {
        "misc.idle": anim(5.0, {
            "trunk": rot(0, ZERO, 2.5, [1.5, 0, 1], 5.0, ZERO),
            "crown": rot(0, ZERO, 2.5, [0, 3, 0], 5.0, ZERO),
            "branch_left": rot(0, ZERO, 2.5, [0, 0, 2], 5.0, ZERO),
            "branch_right": rot(0, ZERO, 2.5, [0, 0, -2], 5.0, ZERO),
        }),
        "move.walk": anim(2.4, {
            "right_root_leg": rot(0, [15, 0, 0], 1.2, [-15, 0, 0], 2.4, [15, 0, 0]),
            "left_root_leg": rot(0, [-15, 0, 0], 1.2, [15, 0, 0], 2.4, [-15, 0, 0]),
            "trunk": rot(0, [0, 0, 3], 1.2, [0, 0, -3], 2.4, [0, 0, 3]),
            "crown": rot(0, [0, 0, -2], 1.2, [0, 0, 2], 2.4, [0, 0, -2]),
        }),
        "attack.roots": anim(roots_t + 0.5, {
            "trunk": rot(0, ZERO, roots_t, [-6, 0, 0], roots_t + 0.1, [4, 0, 0], roots_t + 0.5, ZERO),
            "roots": pos(0, ZERO, roots_t, [0, -2, 0], roots_t + 0.1, [0, 2, 0], roots_t + 0.5, ZERO),
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
        "misc.stunned": anim(2.5, {  # GrievingTreeEntity.STUN_TICKS
            "trunk": rot(0, ZERO, 0.3, [12, 0, 0], 2.1, [12, 0, 0], 2.5, ZERO),
            "crown": rot(0, ZERO, 0.3, [8, 0, 0], 2.1, [8, 0, 0], 2.5, ZERO),
        }, loop=False),
        "misc.uproot": anim(2.0, {
            "root": pos(0, ZERO, 0.6, [0, -4, 0], 1.2, [0, 6, 0], 2.0, ZERO),
            "trunk": rot(0, ZERO, 0.6, [0, 0, 8], 1.2, [0, 0, -8], 2.0, ZERO),
        }, loop=False),
    }
    write_model("grieving_tree", bones, animations, [12, 11], tex_w=512)


# --- Smoldering Lord: the greedy king, 3.5 blocks --------------------------------------------------------------------
# Blackened royal plate with gold trim and ember seams (the armor bone; it shatters in P2 and shows the burning body
# under it), a half-melted gold crown on a charred face, a crimson cape burning at the hem, a flamberge greatsword
# with a glowing edge and an ember in the crossguard.

def smoldering_lord():
    legs, body_h, head = 22, 22, 10
    top = legs + body_h
    flesh = Material(CHAR, base=2.0, noise=0.7, speck=0.05, top=1.0)

    def burning(face, rng):
        if face.side():
            ember_cracks(face, rng, max(2, face.w * face.h // 25), EMBER, EMBER_HOT, 1.0, (3, 6))

    plate = Material(BLACK_STEEL, base=3.0, noise=0.5, edge=0.9, grad=1.0, top=1.0)

    def plate_deco(face, rng):
        if face.side() or face.name == "top":
            trim(face, GOLD[2], ("top", "bottom"))
            for y in range(4, face.h - 2, 5):
                face.hline(0, y, face.w, BLACK_STEEL[1])
                for x in range(face.w):
                    if rng.random() < 0.12:
                        face.lit(x, y, EMBER_DEEP, 0.8)
            if rng.random() < 0.4:
                ember_cracks(face, rng, 1, EMBER, EMBER_DEEP, 0.9, (2, 3))
        if face.name == "front" and face.w >= 15:
            cx = face.cx  # the royal sun on the breastplate
            for dx, dy in ((0, -2), (0, 2), (-2, 0), (2, 0), (-1, -1), (1, 1), (-1, 1), (1, -1)):
                face.set(cx + dx, 6 + dy, GOLD[4])
            face.lit(cx, 6, EMBER_HOT, 1.0)

    def face_front(face, rng):
        for ex in (2, face.w - 4):
            face.rect(ex, 4, 2, 2, VOID)
            face.lit(ex, 4, EMBER_HOT, 1.0)
            face.lit(ex + 1, 5, EMBER, 0.8)
        face.hline(3, 7, face.w - 6, VOID)
        face.lit(face.cx, 7, EMBER, 0.8)
        for x in range(1, face.w - 1):  # ash beard
            if rng.random() < 0.6:
                face.set(x, face.h - 1, ASH[2 + rng.randint(0, 2)])

    def crown(face, rng):
        if face.side():
            for x in range(face.w):
                face.set(x, face.h - 1, GOLD[2])
                if x % 3 == 1:
                    for y in range(face.h - 1):
                        face.set(x, y, GOLD[4 if y == 0 else 3])
                else:
                    for y in range(face.h - 1):
                        face.clear(x, y)
            for x in range(1, face.w, 5):  # melted drips
                face.set(x, face.h - 1, GOLD[1])
            face.lit(face.cx, face.h - 1, hexc("ff5a3a"), 0.8)
        elif face.name == "top":
            clear_all(face)
        elif face.name == "bottom":
            clear_all(face)

    def cape(face, rng):
        if face.name in ("front", "back"):
            for x in range(face.w):
                d = rng.randint(1, 4)
                for k in range(d):
                    face.clear(x, face.h - 1 - k)
                face.lit(x, face.h - 1 - d, EMBER, 0.9)
                if rng.random() < 0.5:
                    face.lit(x, face.h - 2 - d, EMBER_DEEP, 0.7)
            holes(face, rng, 2)
            for x in (0, face.w - 1):
                for y in range(face.h):
                    face.set(x, y, GOLD[1])

    boot = Material(BLACK_STEEL, base=2.6, noise=0.5, edge=0.8)
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("right_leg", [-3.5, legs, 0], "root", None, [
            Box([-7, 0, -3.5], [7, legs, 7], flesh, {"all": burning}),
            Box([-7, 0, -3.5], [7, legs, 7], boot, {"all": plate_deco}, inflate=0.4),
            Box([-7, 12, -3.8], [7, 3, 1], boot.but(base=3.4)),
        ]),
        Bone("left_leg", [3.5, legs, 0], "root", None, []),
        Bone("body", [0, legs, 0], "root", None, [
            Box([-8, legs, -4.5], [16, body_h, 9], flesh, {"all": burning}),
            Box([-7, 6, 5.2], [14, 38, 0], Material(CRIMSON, base=2.8, noise=0.6, grain="v"), {"all": cape}),
        ]),
        Bone("armor", [0, legs, 0], "body", None, [
            Box([-8.5, legs + 6, -5], [17, 16, 10], plate, {"all": plate_deco}, inflate=0.5),
            Box([-12, top - 4, -5], [7, 5, 10], plate.but(base=3.4), {"all": plate_deco}),
            Box([5, top - 4, -5], [7, 5, 10], plate.but(base=3.4), {"all": plate_deco}),
            Box([-8.5, legs, -5], [17, 6, 10], plate.but(base=2.4), {"all": plate_deco}, inflate=0.3),
            Box([-5, top - 1, -5], [10, 2, 9], plate.but(base=3.6), inflate=0.3),
        ]),
        Bone("head", [0, top, 0], "body", None, [
            Box([-5, top, -5], [10, head, 10], flesh.but(base=2.6), {"front": face_front,
                                                                     "left": burning, "right": burning}),
            Box([-5.5, top + head - 1, -5.5], [11, 4, 11], Material(GOLD, base=3.0, noise=0.5), {"all": crown}),
        ]),
    ]
    bone(bones, "left_leg").boxes = [mirror_box(b) for b in bone(bones, "right_leg").boxes]
    arm = Box([-13, top - 22, -2.5], [5, 22, 5], flesh, {"all": burning})
    gauntlet = Box([-13, top - 22, -2.5], [5, 20, 5], boot, {"all": plate_deco}, inflate=0.4)
    bones.append(Bone("right_arm", [-10.5, top - 2, 0], "body", None, [arm, gauntlet]))
    bones.append(Bone("left_arm", [10.5, top - 2, 0], "body", None, [mirror_box(arm), mirror_box(gauntlet)]))

    def flamberge(face, rng):
        if face.name in ("front", "back"):
            # A wavy edge: every few pixels the outline steps in and out; the edge glows.
            for y in range(face.h):
                wave = (y // 3) % 2
                if wave:
                    face.clear(0, y)
                face.lit(1 - wave if face.inside(1 - wave, y) else 0, y, EMBER, 0.9)
                face.lit(face.w - 1, y, EMBER_DEEP, 0.7)
            face.vline(face.cx, 0, face.h, BLACK_STEEL[1])  # fuller

    # Held point-down along the arm and tilted 40 degrees forward at rest (the bone's rotation; no animation
    # touches it): a low guard at idle, raised over the shoulder in the slam windup, out to the side in the sweeps.
    hand = top - 21
    bones.append(Bone("greatsword", [-10.5, hand, 0], "right_arm", [-40, 0, 0], [
        Box([-11.5, hand - 1, -1], [2, 6, 2], Material(LEATHER, base=1.6, noise=0.4)),
        Box([-11.5, hand + 5, -1], [2, 1, 2], Material(GOLD, base=3.0, noise=0.3)),
        Box([-15, hand - 3, -1], [9, 2, 2], Material(GOLD, base=2.6, noise=0.4),
            {"front": lambda f, r: f.lit(f.cx, 0, hexc("ff5a3a"), 1.0)}),
        Box([-12.5, hand - 37, -1], [4, 34, 2], Material(BLACK_STEEL, base=3.8, noise=0.4), {"all": flamberge}),
        Box([-11.5, hand - 40, -0.5], [2, 3, 1], Material(BLACK_STEEL, base=4.2, noise=0.3)),
    ]))
    s1, s2, slam, lunge, mortar, line, descend = t(14), t(10), t(20), t(18), 1.0, t(30), t(30)
    animations = {
        "misc.idle": anim(3.0, {
            "body": rot(0, ZERO, 1.5, [2, 0, 0], 3.0, ZERO),
            "right_arm": rot(0, [-20, 0, 0], 1.5, [-24, 0, 0], 3.0, [-20, 0, 0]),
            "head": rot(0, ZERO, 1.5, [-3, 4, 0], 3.0, ZERO),
        }),
        "move.walk": anim(1.2, {
            "right_leg": rot(0, [25, 0, 0], 0.6, [-25, 0, 0], 1.2, [25, 0, 0]),
            "left_leg": rot(0, [-25, 0, 0], 0.6, [25, 0, 0], 1.2, [-25, 0, 0]),
            "left_arm": rot(0, [15, 0, 0], 0.6, [-15, 0, 0], 1.2, [15, 0, 0]),
            "right_arm": rot(0, [-20, 0, 0], 1.2, [-20, 0, 0]),
            "root": pos(0, ZERO, 0.3, [0, 0.6, 0], 0.6, ZERO, 0.9, [0, 0.6, 0], 1.2, ZERO),
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
    write_model("smoldering_lord", bones, animations, [3, 4], tex_w=128)


BOSSES = [("crypt_keeper", crypt_keeper), ("ash_colossus", ash_colossus), ("grieving_tree", grieving_tree),
          ("smoldering_lord", smoldering_lord)]


if __name__ == "__main__":
    wanted = set(sys.argv[1:])
    for name, build in BOSSES:
        if not wanted or name in wanted:
            build()
