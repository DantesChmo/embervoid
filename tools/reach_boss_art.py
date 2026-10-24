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
from art_kit import (ZERO, Bone, Box, Material, anim, chain_bones, chain_names, chamfered, crack, e,  # noqa: E402
                     ember_cracks, fingers, fold_lines, grain, hexc, keys, lag_sway, merge, mirror_box, octagon, plate, pos,
                     ramp, rivet_ring, rot, scatter, shade, sutures, sway, t, whip, write_model)
from reach_mob_art import (ASH, BARK, BONE, BRONZE, CHAR, COLD, CRIMSON, EMBER, EMBER_DARK, EMBER_DEEP,  # noqa: E402
                           EMBER_HOT, GOLD, IRON, LEATHER, MOSS, ROPE, RUST, SHROUD, VOID, WOOD, AMBER,
                           BLACK_STEEL, band, clear_all, holes, lower, open_front, ragged_edge, rivets, tatter, trim)


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


# --- Ash Colossus: 4.5 blocks of fused ash stone (PLAN 19.4) -------------------------------------------------------
# Hunched, with huge arms and fists; ash drifts on its shoulders and head; magma seams; old granary bricks fused into
# its body; an ember core in the back under stone plates strapped with burnt chains (plates bone; shed in P2 to show
# the core bone). Every big form is chamfered (three boxes), the fists have fingers, the forearms are their own bones,
# chains hang from the shoulder plates, the hips and the back and swing behind the motion.

FUSED = ramp("1f1c1c", "2c2828", "3a3535", "484343", "585252", "6b6464", "7d7676")
ASH_DRIFT = ramp("5e5955", "77716b", "908982", "aaa29a", "c1b9b0")
CORE_RAMP = ramp("8a2a10", "d8521c", "ff8a2a", "ffb347", "ffd27a", "fff0c0")


def ash_colossus():
    legs, body_h, head = 22, 36, 12
    top = legs + body_h
    stone = Material(FUSED, base=3.0, noise=0.7, cell=3.5, speck=0.04, edge=0.5, grad=0.8, top=1.0, bevel=0.8)
    dark_stone = stone.but(base=2.2)
    light_stone = stone.but(base=3.6)
    chain_mat = Material(IRON, base=2.2, noise=0.5, speck=0.1, bevel=0.6)

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
            ember_cracks(face, rng, n or max(1, face.w * face.h // 160), EMBER, EMBER_DEEP, 0.9, (3, 7))

    def cracks(face, rng):
        if face.side():
            for _ in range(max(1, face.w * face.h // 120)):
                crack(face, rng, FUSED[0], length=rng.randint(3, 7))

    def masonry(face, rng):
        """A patch of fused granary bricks: two courses in a frame of mortar, joints staggered, one brick chipped."""
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
                        face.set(x, y, FUSED[4] if (x + y) % 3 else FUSED[3])
            face.set(x0 + 1, y0 + 1, FUSED[5])      # a lit chip
            face.set(x0 + 8, y0 + 5, FUSED[1])      # a broken corner

    def body_deco(face, rng):
        drift(face, rng)
        masonry(face, rng)
        cracks(face, rng)
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
        cracks(face, rng)
        face.hline(1, 3, face.w - 2, FUSED[0])  # brow shadow
        for ex in (1, face.w - 5):
            face.rect(ex, 4, 4, 3, VOID)
            face.lit(ex + 1, 4, EMBER_HOT, 1.0)
            face.lit(ex + 2, 4, EMBER_HOT, 1.0)
            face.lit(ex + 1, 5, EMBER, 0.9)
            face.lit(ex + 2, 5, EMBER, 0.9)
            face.lit(ex + 1, 6, EMBER_DEEP, 0.6)
        face.vline(face.cx - 1, 6, 3, FUSED[1])   # a split down the nose line
        face.hline(2, 10, face.w - 4, VOID)
        for x in range(2, face.w - 2, 2):
            face.lit(x, 10, EMBER_DEEP, 0.7)

    def jaw_deco(face, rng):
        if face.name == "front":
            for x in range(1, face.w - 1, 2):   # teeth of stone
                face.set(x, 0, ASH_DRIFT[2])
            face.hline(0, face.h - 1, face.w, FUSED[0])
        if face.name == "top":
            for x in range(face.w):
                face.lit(x, 0, EMBER_DEEP, 0.6)

    def knuckles(face, rng):
        if face.name in ("front", "top"):
            for x in range(1, face.w, 3):
                face.vline(x, 0, face.h, FUSED[1])
        cracks(face, rng)
        seams(face, rng, 1)

    def plate_deco(face, rng):
        drift(face, rng)
        cracks(face, rng)
        if face.name in ("back", "front") and face.h > 8:
            for y in (3, face.h - 4):  # burnt chains strapping the plates on
                for x in range(face.w):
                    face.set(x, y, IRON[1] if x % 2 else IRON[3])
                    if x % 4 == 1:
                        face.set(x, y - 1, IRON[2])
        if face.side():
            ember_cracks(face, rng, 1, EMBER_DEEP, EMBER_DARK, 0.7, (2, 4))

    def links(face, rng):
        if face.side():
            for y in range(face.h):
                face.set(0, y, IRON[4] if y % 2 else IRON[1])
                face.set(face.w - 1, y, IRON[0] if y % 2 else IRON[3])

    def strap(face, rng):
        if face.side():
            fold_lines(face, rng, 2, -14)
            ragged_edge(face, rng, "bottom", 2, 0.5)

    # Animation signs (checked in the preview): -X raises an arm forward, swings a leg forward, tilts the head and
    # body back and opens the jaw; +X bows the body forward and folds a leg back.
    bones = [Bone("root", [0, 0, 0])]
    # Legs: a chamfered shin, a knee boulder, a slab of a foot with toe stones.
    shin = chamfered([-13, 3, -6], [12, legs - 3, 12], stone, {"all": body_deco}, c=1.5)
    foot = chamfered([-14, 0, -8], [14, 4, 15], dark_stone, {"all": seams}, c=1, axes="xz")
    knee = chamfered([-12.5, 12, -8], [11, 7, 3], light_stone, {"all": drift}, c=1)
    toes = [Box([-13.5 + i * 4.5, 0, -9.5], [3.5, 3, 2.5], light_stone, {"all": cracks}) for i in range(3)]
    bones.append(Bone("right_leg", [-7, legs, 0], "root", None, shin + foot + knee + toes))
    bones.append(Bone("left_leg", [7, legs, 0], "root", None, [mirror_box(b) for b in shin + foot + knee + toes]))
    # Body: the chamfered mass with the core cavity, a belly band of fused bricks, hip stones.
    torso = chamfered([-17, legs, -11], [34, body_h, 20], stone, {"all": back}, c=2)
    belly = chamfered([-12, legs - 2, -10], [24, 7, 19], dark_stone, {"all": lambda f, r: (masonry(f, r), seams(f, r))}, c=1, axes="xz")
    hips = [Box([-19, legs - 1, -7], [4, 6, 13], light_stone, {"all": cracks}), Box([15, legs - 1, -7], [4, 6, 13], light_stone, {"all": cracks})]
    bones.append(Bone("body", [0, legs, 0], "root", [15, 0, 0], torso + belly + hips))
    # The chest: a slab of granary wall fused across the front, breathing on its own bone.
    chest = chamfered([-13, legs + 18, -14], [26, 16, 4], light_stone, {"all": body_deco}, c=1)
    chest += [Box([-9, legs + 12, -13.5], [18, 5, 2], light_stone, {"all": lambda f, r: (masonry(f, r), cracks(f, r))})]
    bones.append(Bone("chest", [0, legs + 18, -11], "body", None, chest))
    # The core and its glowing rim.
    bones.append(Bone("core", [0, legs + 18, 11], "body", None, [
        Box([-6, legs + 12, 8], [12, 12, 4], Material(CORE_RAMP, base=3.0, noise=0.9, cell=2.0, glow=1.0)),
        Box([-3, legs + 15, 11], [6, 6, 2], Material(ramp("ffb347", "ffd27a", "fff0c0"), base=1.5, noise=0.6, glow=1.0)),
        Box([-7, legs + 11, 9], [14, 14, 1], Material(CORE_RAMP, base=1.2, noise=0.8, glow=0.6), rotation=[0, 0, 45],
            pivot=[0, legs + 18, 9.5]),
    ]))
    # The stone plates over the core, strapped with chains; two shoulder plates. Shed in P2.
    back_plate = chamfered([-12, legs + 8, 11], [24, 22, 5], stone.but(base=2.6), {"all": plate_deco}, c=1.5)
    shoulder_plates = chamfered([-21, top - 7, -9], [9, 9, 17], light_stone, {"all": plate_deco}, c=1.5)
    plates_boxes = back_plate + shoulder_plates + [mirror_box(b) for b in shoulder_plates]
    plates_boxes += [Box([-14, legs + 6, 13.5], [28, 2, 2], chain_mat, {"all": links}),
                     Box([-14, legs + 26, 13.5], [28, 2, 2], chain_mat, {"all": links})]
    bones.append(Bone("plates", [0, legs + 18, 12], "body", None, plates_boxes))
    # A chain hanging off the back plate, three links long.
    bones += chain_bones("back_chain", "plates", [6, legs + 7, 15], 3, 5, chain_mat, {"all": links}, thickness=1.5)
    # Head: a chamfered skull sunk between the shoulders, a brow ridge, a jaw that opens, ash on top.
    skull = chamfered([-6, top - 2, -14], [12, head, 12], light_stone, {"front": head_front, "top": drift,
                                                                     "left": cracks, "right": cracks, "back": cracks}, c=1.5)
    brow = Box([-7, top + 6, -15.5], [14, 3, 6], light_stone.but(base=3.8), {"all": drift})
    horns = [Box([-8, top + 7, -11], [3, 5, 4], dark_stone, {"all": cracks}, rotation=[0, 0, 25], pivot=[-6.5, top + 7, -9]),
             Box([5, top + 7, -11], [3, 5, 4], dark_stone, {"all": cracks}, rotation=[0, 0, -25], pivot=[6.5, top + 7, -9])]
    bones.append(Bone("head", [0, top, -6], "body", None, skull + [brow] + horns))
    bones.append(Bone("jaw", [0, top - 1, -8], "head", None, [
        Box([-5, top - 4, -15], [10, 3, 8], dark_stone, {"all": jaw_deco}),
    ]))
    # Arms: a shoulder boulder and upper arm, a forearm with iron straps, a fist of five stones.
    shoulder = chamfered([-30, top - 8, -8], [15, 9, 16], light_stone, {"all": body_deco}, c=2)
    upper = chamfered([-27, top - 24, -6], [11, 18, 12], stone, {"all": body_deco}, c=1.5)
    elbow = Box([-28.5, top - 27, -7], [13, 5, 14], dark_stone, {"all": seams}, inflate=0.3)
    bones.append(Bone("right_arm", [-20, top - 4, 0], "body", None, shoulder + upper + [elbow]))
    fore = chamfered([-27, top - 42, -6], [11, 17, 12], stone, {"all": body_deco}, c=1.5)
    bands = [Box([-27.5, top - 33 + k * 5, -6.5], [12, 2, 13], chain_mat, {"all": links}) for k in range(2)]
    bones.append(Bone("right_forearm", [-21.5, top - 25, 0], "right_arm", None, fore + bands))
    palm = chamfered([-29, top - 50, -7], [14, 9, 14], stone.but(base=2.6), {"all": knuckles}, c=1.5)
    digits = fingers(-28.5, top - 50, -9.5, 4, [3, 7, 3.5], light_stone, spread=0.4, curl=75, decals={"all": cracks})
    thumb = Box([-16, top - 50, -5], [3, 6, 3.5], light_stone, {"all": cracks}, rotation=[60, 0, -30], pivot=[-14.5, top - 50, -3])
    bones.append(Bone("right_hand", [-22, top - 42, 0], "right_forearm", None, palm + digits + [thumb]))
    for name in ("right_arm", "right_forearm", "right_hand"):
        src = bone(bones, name)
        bones.append(Bone(name.replace("right", "left"), [-src.pivot[0], src.pivot[1], src.pivot[2]],
                          src.parent.replace("right", "left"), None, [mirror_box(b) for b in src.boxes]))
    # Straps of burnt leather and chain hanging from the shoulders and the hips (secondary motion).
    strap_mat = Material(LEATHER, base=2.0, noise=0.5, grain="v", bevel=0.4)
    for side, sx in (("right", -1), ("left", 1)):
        bones += chain_bones(side + "_strap", side + "_arm", [sx * 29, top - 8, 0], 2, 6, strap_mat, {"all": strap}, thickness=3)
        bones += chain_bones(side + "_hip_chain", "body", [sx * 17.5, legs - 1, 4], 2, 5, chain_mat, {"all": links}, thickness=1.5)
    fist_t, drag, leap, boulder, smash, storm = t(18), t(16), t(22), t(30), t(20), t(30)
    hanging = (chain_names("back_chain", 3), chain_names("right_strap", 2), chain_names("left_strap", 2),
               chain_names("right_hip_chain", 2), chain_names("left_hip_chain", 2))

    def hang_sway(length, amp, phase_step=0.17):
        out = {}
        for i, names in enumerate(hanging):
            out = merge(out, lag_sway(names, length, amp, axis=0 if i != 1 and i != 2 else 2, phase=i * phase_step))
        return out

    def hang_whip(t0, amp, settle=0.6):
        out = {}
        for i, names in enumerate(hanging):
            out = merge(out, whip(names, t0 + 0.03 * i, amp, axis=0 if i != 1 and i != 2 else 2, settle=settle))
        return out

    arms_rest = ZERO
    animations = {
        "misc.idle": anim(4.0, merge(hang_sway(4.0, 4), {
            "body": rot(0, ZERO, 2.0, e("easeInOutSine", [2.5, 0, 0]), 4.0, e("easeInOutSine", ZERO)),
            "chest": pos(0, ZERO, 2.0, e("easeInOutSine", [0, 0.6, -1]), 4.0, e("easeInOutSine", ZERO)),
            "right_arm": rot(0, ZERO, 2.0, e("easeInOutSine", [-4, 0, 3]), 4.0, e("easeInOutSine", ZERO)),
            "left_arm": rot(0, ZERO, 2.0, e("easeInOutSine", [-4, 0, -3]), 4.0, e("easeInOutSine", ZERO)),
            "right_forearm": rot(0, ZERO, 2.0, e("easeInOutSine", [-6, 0, 0]), 4.0, e("easeInOutSine", ZERO)),
            "left_forearm": rot(0, ZERO, 2.0, e("easeInOutSine", [-6, 0, 0]), 4.0, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, 1.3, e("easeInOutSine", [4, 6, 0]), 2.9, e("easeInOutSine", [2, -5, 0]), 4.0, e("easeInOutSine", ZERO)),
            "jaw": rot(0, ZERO, 2.0, e("easeInOutSine", [-8, 0, 0]), 4.0, e("easeInOutSine", ZERO)),
        })),
        "move.walk": anim(1.8, merge(hang_sway(1.8, 9), {
            "right_leg": rot(0, [18, 0, 0], 0.9, e("easeInOutSine", [-18, 0, 0]), 1.8, e("easeInOutSine", [18, 0, 0])),
            "left_leg": rot(0, [-18, 0, 0], 0.9, e("easeInOutSine", [18, 0, 0]), 1.8, e("easeInOutSine", [-18, 0, 0])),
            "right_arm": rot(0, [-14, 0, 2], 0.9, e("easeInOutSine", [14, 0, 2]), 1.8, e("easeInOutSine", [-14, 0, 2])),
            "left_arm": rot(0, [14, 0, -2], 0.9, e("easeInOutSine", [-14, 0, -2]), 1.8, e("easeInOutSine", [14, 0, -2])),
            "right_forearm": rot(0, [-10, 0, 0], 0.9, [-20, 0, 0], 1.8, [-10, 0, 0]),
            "left_forearm": rot(0, [-20, 0, 0], 0.9, [-10, 0, 0], 1.8, [-20, 0, 0]),
            # Weight: a drop on each footfall, the hips roll and the torso twists against the stride; the head stays level.
            "root": pos(0, ZERO, 0.2, e("easeOutQuad", [0, -1.5, 0]), 0.5, e("easeInOutSine", [0, 0.3, 0]), 1.1,
                        e("easeOutQuad", [0, -1.5, 0]), 1.4, e("easeInOutSine", [0, 0.3, 0]), 1.8, ZERO),
            "body": rot(0, [0, -5, 3], 0.9, e("easeInOutSine", [0, 5, -3]), 1.8, e("easeInOutSine", [0, -5, 3])),
            "head": rot(0, [0, 5, -3], 0.9, e("easeInOutSine", [0, -5, 3]), 1.8, e("easeInOutSine", [0, 5, -3])),
            "chest": rot(0, [0, -2, 0], 0.9, [0, 2, 0], 1.8, [0, -2, 0]),
        })),
        # Fist: the right arm draws back and up while the body coils to the right, hangs an instant at the top,
        # then the fist comes down and across; the chains fly after it.
        "attack.fist": anim(fist_t + 0.55, merge(hang_whip(fist_t + 0.05, 28), {
            "right_arm": rot(0, arms_rest, fist_t * 0.55, e("easeInOutSine", [-70, 0, 35]), fist_t, e("easeInSine", [-160, 0, 20]),
                             fist_t + 0.1, e("easeOutBack", [-50, -15, 0]), fist_t + 0.3, [-45, -15, 0], fist_t + 0.55, e("easeInOutSine", arms_rest)),
            "right_forearm": rot(0, ZERO, fist_t * 0.55, e("easeInOutSine", [-70, 0, 0]), fist_t, [-40, 0, 0], fist_t + 0.1,
                                 e("easeOutCubic", [-5, 0, 0]), fist_t + 0.55, e("easeInOutSine", ZERO)),
            "right_hand": rot(0, ZERO, fist_t, [-20, 0, 0], fist_t + 0.1, e("easeOutBack", [20, 0, 0]), fist_t + 0.55, ZERO),
            "left_arm": rot(0, ZERO, fist_t, e("easeInOutSine", [20, 0, -15]), fist_t + 0.1, [-10, 0, -10], fist_t + 0.55, ZERO),
            "body": rot(0, ZERO, fist_t * 0.55, e("easeInOutSine", [-10, 25, -4]), fist_t, [-18, 30, -4], fist_t + 0.1,
                        e("easeOutBack", [18, -15, 3]), fist_t + 0.55, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, fist_t, e("easeInOutSine", [-10, -25, 0]), fist_t + 0.1, [8, 10, 0], fist_t + 0.55, ZERO),
            "jaw": rot(0, ZERO, fist_t, [-25, 0, 0], fist_t + 0.15, [-5, 0, 0], fist_t + 0.55, ZERO),
            "root": pos(0, ZERO, fist_t, [0, 1, 1], fist_t + 0.1, e("easeOutCubic", [0, -2, -3]), fist_t + 0.55, e("easeInOutSine", ZERO)),
        }), loop=False),
        # Arm drag: the left arm swings far out to its side, the body turning away, then it rakes low across the front.
        "attack.drag": anim(drag + 0.6, merge(hang_whip(drag + 0.1, 24, settle=0.5), {
            "left_arm": rot(0, ZERO, drag * 0.6, e("easeInOutSine", [-50, -70, -50]), drag, [-60, -80, -55], drag + 0.22,
                            e("easeOutCubic", [-55, 60, -15]), drag + 0.6, e("easeInOutSine", ZERO)),
            "left_forearm": rot(0, ZERO, drag, [-30, 0, 0], drag + 0.22, e("easeOutCubic", [-70, 0, 0]), drag + 0.6, ZERO),
            "left_hand": rot(0, ZERO, drag, [-30, 0, 0], drag + 0.22, [-80, 0, 0], drag + 0.6, ZERO),
            "right_arm": rot(0, ZERO, drag, [10, 0, 15], drag + 0.22, [-20, 0, 10], drag + 0.6, ZERO),
            "body": rot(0, ZERO, drag * 0.6, e("easeInOutSine", [8, -30, 0]), drag, [12, -35, 0], drag + 0.22,
                        e("easeOutBack", [14, 30, 0]), drag + 0.6, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, drag, [0, 30, 0], drag + 0.22, [5, -25, 0], drag + 0.6, ZERO),
            "root": pos(0, ZERO, drag, [0, -2, 0], drag + 0.22, [0, -3, -2], drag + 0.6, ZERO),
        }), loop=False),
        # Leap: a deep crouch with the arms drawn back, the launch (arms thrown up), the fall with both fists
        # raised, the landing crouch (LEAP_FLIGHT_TICKS 14 after the windup: the server lands it), the rise.
        "attack.leap": anim(leap + 1.3, merge(hang_whip(leap + 0.75, 35, settle=0.5), {
            "root": pos(0, ZERO, leap * 0.7, e("easeInOutSine", [0, -7, 0]), leap, [0, -8, 0], leap + 0.12,
                        e("easeOutCubic", [0, 4, 0]), leap + 0.7, [0, 3, 0], leap + 0.8, e("easeOutCubic", [0, -5, 0]),
                        leap + 1.3, e("easeInOutSine", ZERO)),
            "body": rot(0, ZERO, leap * 0.7, e("easeInOutSine", [30, 0, 0]), leap, [35, 0, 0], leap + 0.12, e("easeOutCubic", [-20, 0, 0]),
                        leap + 0.7, [-10, 0, 0], leap + 0.8, e("easeOutCubic", [30, 0, 0]), leap + 1.3, e("easeInOutSine", ZERO)),
            "right_leg": rot(0, ZERO, leap * 0.7, [-45, 0, 0], leap, [-50, 0, 0], leap + 0.12, [20, 0, 0], leap + 0.7, [15, 0, 0],
                             leap + 0.8, [-40, 0, 0], leap + 1.3, ZERO),
            "left_leg": rot(0, ZERO, leap * 0.7, [-45, 0, 0], leap, [-50, 0, 0], leap + 0.12, [20, 0, 0], leap + 0.7, [15, 0, 0],
                            leap + 0.8, [-40, 0, 0], leap + 1.3, ZERO),
            "right_arm": rot(0, ZERO, leap * 0.7, e("easeInOutSine", [45, 0, 15]), leap, [50, 0, 15], leap + 0.3,
                             e("easeOutCubic", [-175, 0, 15]), leap + 0.7, [-170, 0, 10], leap + 0.82, e("easeOutBack", [-45, 0, 5]),
                             leap + 1.3, e("easeInOutSine", ZERO)),
            "left_arm": rot(0, ZERO, leap * 0.7, e("easeInOutSine", [45, 0, -15]), leap, [50, 0, -15], leap + 0.3,
                            e("easeOutCubic", [-175, 0, -15]), leap + 0.7, [-170, 0, -10], leap + 0.82, e("easeOutBack", [-45, 0, -5]),
                            leap + 1.3, e("easeInOutSine", ZERO)),
            "right_forearm": rot(0, ZERO, leap, [-30, 0, 0], leap + 0.3, [-20, 0, 0], leap + 0.82, [-10, 0, 0], leap + 1.3, ZERO),
            "left_forearm": rot(0, ZERO, leap, [-30, 0, 0], leap + 0.3, [-20, 0, 0], leap + 0.82, [-10, 0, 0], leap + 1.3, ZERO),
            "head": rot(0, ZERO, leap, [-25, 0, 0], leap + 0.3, [-10, 0, 0], leap + 0.82, [20, 0, 0], leap + 1.3, ZERO),
            "jaw": rot(0, ZERO, leap + 0.12, [-30, 0, 0], leap + 0.7, [-25, 0, 0], leap + 0.9, ZERO),
        }), loop=False),
        # Boulder: it bends and rips stone from the ground with its right hand (the hand opens), heaves it
        # overhead, holds it there straining, and hurls it.
        "attack.boulder": anim(boulder + 0.5, merge(hang_whip(boulder + 0.05, 30), {
            "right_arm": rot(0, ZERO, boulder * 0.3, e("easeInOutSine", [-35, 0, 10]), boulder * 0.45, [-40, 0, 10], boulder * 0.8,
                             e("easeInOutSine", [-165, 0, 15]), boulder, [-175, 0, 15], boulder + 0.12, e("easeOutBack", [-50, 0, 0]),
                             boulder + 0.5, e("easeInOutSine", ZERO)),
            "right_forearm": rot(0, ZERO, boulder * 0.3, [-40, 0, 0], boulder * 0.45, [-60, 0, 0], boulder * 0.8, [-30, 0, 0],
                                 boulder + 0.12, e("easeOutCubic", [-5, 0, 0]), boulder + 0.5, ZERO),
            "right_hand": rot(0, ZERO, boulder * 0.3, [-40, 0, 0], boulder * 0.45, [-10, 0, 0], boulder + 0.12, [30, 0, 0], boulder + 0.5, ZERO),
            "left_arm": rot(0, ZERO, boulder * 0.3, e("easeInOutSine", [-30, 0, -10]), boulder * 0.8, e("easeInOutSine", [-120, 0, -20]),
                            boulder, [-130, 0, -20], boulder + 0.12, [-40, 0, -10], boulder + 0.5, ZERO),
            "body": rot(0, ZERO, boulder * 0.3, e("easeInOutSine", [30, 0, 0]), boulder * 0.45, [32, 10, 0], boulder * 0.8,
                        e("easeInOutSine", [-22, 15, 0]), boulder, [-25, 18, 0], boulder + 0.12, e("easeOutBack", [20, -10, 0]),
                        boulder + 0.5, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, boulder * 0.3, [15, 0, 0], boulder * 0.8, [-20, 0, 0], boulder + 0.12, [10, 0, 0], boulder + 0.5, ZERO),
            "jaw": rot(0, ZERO, boulder * 0.8, [-20, 0, 0], boulder + 0.12, [-5, 0, 0], boulder + 0.5, ZERO),
            "root": pos(0, ZERO, boulder * 0.45, [0, -4, 0], boulder * 0.8, [0, 1, 0], boulder + 0.12, [0, -1, -2], boulder + 0.5, ZERO),
        }), loop=False),
        # Smash: both fists go up over the head, hang, and come down together; the whole body drops into it.
        "attack.smash": anim(smash + 0.6, merge(hang_whip(smash + 0.05, 32), {
            "right_arm": rot(0, ZERO, smash * 0.6, e("easeInOutSine", [-150, 0, 20]), smash, [-170, 0, 12], smash + 0.1,
                             e("easeOutBack", [-35, 0, 5]), smash + 0.3, [-30, 0, 5], smash + 0.6, e("easeInOutSine", ZERO)),
            "left_arm": rot(0, ZERO, smash * 0.6, e("easeInOutSine", [-150, 0, -20]), smash, [-170, 0, -12], smash + 0.1,
                            e("easeOutBack", [-35, 0, -5]), smash + 0.3, [-30, 0, -5], smash + 0.6, e("easeInOutSine", ZERO)),
            "right_forearm": rot(0, ZERO, smash, [-35, 0, 0], smash + 0.1, e("easeOutCubic", ZERO)),
            "left_forearm": rot(0, ZERO, smash, [-35, 0, 0], smash + 0.1, e("easeOutCubic", ZERO)),
            "body": rot(0, ZERO, smash * 0.6, e("easeInOutSine", [-15, 0, 0]), smash, [-20, 0, 0], smash + 0.1, e("easeOutBack", [30, 0, 0]),
                        smash + 0.3, [28, 0, 0], smash + 0.6, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, smash, [-20, 0, 0], smash + 0.1, [15, 0, 0], smash + 0.6, ZERO),
            "jaw": rot(0, ZERO, smash, [-30, 0, 0], smash + 0.2, ZERO),
            "root": pos(0, ZERO, smash, [0, 2, 0], smash + 0.1, e("easeOutCubic", [0, -4, -2]), smash + 0.3, [0, -4, -2], smash + 0.6, ZERO),
        }), loop=False),
        # Storm: it rears back, arms wide, jaw open, and roars; the chains stream out as it shakes.
        "misc.storm": anim(storm + 0.6, merge(hang_whip(storm * 0.5, 30, settle=1.0), {
            "body": rot(0, ZERO, storm * 0.5, e("easeInOutSine", [-30, 0, 0]), storm * 0.7, [-32, 0, 3], storm * 0.85, [-30, 0, -3],
                        storm, [-32, 0, 0], storm + 0.6, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, storm * 0.5, e("easeInOutSine", [-35, 0, 0]), storm, [-40, 0, 0], storm + 0.6, ZERO),
            "jaw": rot(0, ZERO, storm * 0.5, e("easeOutBack", [-45, 0, 0]), storm, [-45, 0, 0], storm + 0.6, ZERO),
            "right_arm": rot(0, ZERO, storm * 0.5, e("easeInOutSine", [-30, 0, 70]), storm, [-20, 0, 75], storm + 0.6, ZERO),
            "left_arm": rot(0, ZERO, storm * 0.5, e("easeInOutSine", [-30, 0, -70]), storm, [-20, 0, -75], storm + 0.6, ZERO),
            "right_forearm": rot(0, ZERO, storm * 0.5, [-40, 0, 0], storm + 0.6, ZERO),
            "left_forearm": rot(0, ZERO, storm * 0.5, [-40, 0, 0], storm + 0.6, ZERO),
            "right_hand": rot(0, ZERO, storm * 0.5, [-30, 0, 0], storm + 0.6, ZERO),
            "left_hand": rot(0, ZERO, storm * 0.5, [-30, 0, 0], storm + 0.6, ZERO),
            "chest": pos(0, ZERO, storm * 0.5, [0, 1, -2], storm + 0.6, ZERO),
        }), loop=False),
        # Shed (P2): it heaves, the plates crack loose, and it shrugs them off its back; the core blazes.
        "misc.shed": anim(1.5, merge(hang_whip(0.9, 35, settle=0.6), {
            "body": rot(0, ZERO, 0.3, e("easeInOutSine", [-12, 0, 8]), 0.6, [-12, 0, -8], 0.9, e("easeOutBack", [20, 0, 0]),
                        1.5, e("easeInOutSine", ZERO)),
            "right_arm": rot(0, ZERO, 0.3, [-40, 0, 40], 0.9, e("easeOutBack", [20, 0, 10]), 1.5, ZERO),
            "left_arm": rot(0, ZERO, 0.3, [-40, 0, -40], 0.9, e("easeOutBack", [20, 0, -10]), 1.5, ZERO),
            "head": rot(0, ZERO, 0.3, [-25, 0, 0], 0.9, [15, 0, 0], 1.5, ZERO),
            "jaw": rot(0, ZERO, 0.3, [-40, 0, 0], 1.0, [-40, 0, 0], 1.5, ZERO),
            "plates": {"position": keys(0, ZERO, 0.6, [0, 0.5, 0.5], 0.9, e("easeInQuad", [0, -14, 12]), 1.5, [0, -40, 20]),
                       "rotation": keys(0, ZERO, 0.9, [-30, 0, 0], 1.5, [-80, 0, 15])},
            "core": {"scale": keys(0, [1, 1, 1], 0.9, [1, 1, 1], 1.1, e("easeOutBack", [1.3, 1.3, 1.3]), 1.5, [1, 1, 1])},
        }), loop=False),
        # Hurt: a short recoil of the body and head; the chains jump.
        "misc.hurt": anim(0.4, merge(hang_whip(0.02, 14, settle=0.36), {
            "body": rot(0, ZERO, 0.08, e("easeOutCubic", [-7, 5, 0]), 0.4, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, 0.08, e("easeOutCubic", [-14, -8, 0]), 0.4, e("easeInOutSine", ZERO)),
            "chest": pos(0, ZERO, 0.08, [0, 0, 1], 0.4, ZERO),
            "root": pos(0, ZERO, 0.08, [0, 0, 1.2], 0.4, ZERO),
        }), loop=False),
        # Stagger (P2, a hit into the bare core): it staggers forward a step, arms out, and catches itself.
        "misc.stagger": anim(0.9, merge(hang_whip(0.05, 30, settle=0.8), {
            "root": pos(0, ZERO, 0.15, e("easeOutCubic", [0, -2, -4]), 0.5, [0, -3, -5], 0.9, e("easeInOutSine", ZERO)),
            "body": rot(0, ZERO, 0.15, e("easeOutBack", [28, 0, 6]), 0.5, [24, 0, -4], 0.9, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, 0.15, [-25, 0, 0], 0.5, [-15, 10, 0], 0.9, ZERO),
            "jaw": rot(0, ZERO, 0.15, [-35, 0, 0], 0.9, ZERO),
            "right_arm": rot(0, ZERO, 0.15, e("easeOutBack", [-60, 0, 40]), 0.9, e("easeInOutSine", ZERO)),
            "left_arm": rot(0, ZERO, 0.15, e("easeOutBack", [-60, 0, -40]), 0.9, e("easeInOutSine", ZERO)),
            "right_leg": rot(0, ZERO, 0.15, [-30, 0, 0], 0.5, [-25, 0, 0], 0.9, ZERO),
            "core": {"scale": keys(0, [1, 1, 1], 0.1, [1.25, 1.25, 1.25], 0.5, [1, 1, 1])},
        }), loop=False),
        # Death (3 s): it sags, the legs give and fold back under it as it drops onto its knees, it looks up and
        # tries to push itself up, then the whole mass topples forward onto its fists and face; the head sinks last,
        # the chains settle, the core gutters.
        "misc.death": anim(3.0, merge(hang_whip(1.95, 40, settle=1.0), {
            "root": pos(0, ZERO, 0.5, e("easeInOutSine", [0, -3, 0]), 1.0, e("easeInQuad", [0, -16, 0]), 1.4, e("easeInOutSine", [0, -14, 0]),
                        1.9, e("easeInCubic", [0, -17, -3]), 2.1, e("easeOutBounce", [0, -17, -4]), 3.0, [0, -17, -4]),
            "right_leg": rot(0, ZERO, 0.5, [10, 0, 0], 1.0, e("easeInQuad", [88, 0, 0]), 3.0, [88, 0, 0]),
            "left_leg": rot(0, ZERO, 0.5, [-15, 0, 0], 1.0, e("easeInQuad", [85, 0, 0]), 3.0, [85, 0, 0]),
            "body": rot(0, ZERO, 0.5, e("easeInOutSine", [12, 0, 5]), 1.0, [22, 0, 5], 1.4, e("easeInOutSine", [8, 0, -4]), 1.9,
                        e("easeInCubic", [70, 0, 0]), 2.1, e("easeOutBounce", [76, 0, 0]), 3.0, [76, 0, 0]),
            "chest": pos(0, ZERO, 0.6, [0, 0.4, -0.6], 1.2, ZERO, 1.7, [0, 0.4, -0.6], 2.2, ZERO),
            "head": rot(0, ZERO, 0.5, [8, 0, 0], 1.0, [10, 0, 10], 1.4, e("easeInOutSine", [-25, 0, 0]), 1.9, [-35, 0, 0],
                        2.1, e("easeOutCubic", [-15, 0, 0]), 2.6, e("easeInQuad", [20, 15, 0]), 3.0, [25, 15, 0]),
            "jaw": rot(0, ZERO, 0.5, [-15, 0, 0], 1.4, [-35, 0, 0], 2.1, [-40, 0, 0], 2.8, [-10, 0, 0], 3.0, [-5, 0, 0]),
            # The fists hang at knee height: as it kneels they come forward and plant on the ground; once the body
            # has pitched over they are nearly along it (the arms hang from the pitched body), knuckles down.
            "right_arm": rot(0, ZERO, 0.5, e("easeInOutSine", [-30, 0, 12]), 1.0, e("easeOutCubic", [-52, 0, 14]), 1.4, [-58, 0, 16],
                             1.9, e("easeInCubic", [-14, 0, 10]), 2.1, e("easeOutBounce", [-8, 0, 8]), 3.0, [-8, 0, 8]),
            "left_arm": rot(0, ZERO, 0.5, e("easeInOutSine", [-25, 0, -12]), 1.0, e("easeOutCubic", [-50, 0, -14]), 1.4, [-55, 0, -16],
                            1.9, e("easeInCubic", [-12, 0, -10]), 2.1, e("easeOutBounce", [-6, 0, -8]), 3.0, [-6, 0, -8]),
            "right_forearm": rot(0, ZERO, 0.5, [-20, 0, 0], 1.4, [-10, 0, 0], 2.1, [-5, 0, 0], 3.0, [-5, 0, 0]),
            "left_forearm": rot(0, ZERO, 0.5, [-20, 0, 0], 1.4, [-10, 0, 0], 2.1, [-5, 0, 0], 3.0, [-5, 0, 0]),
            "right_hand": rot(0, ZERO, 1.0, [40, 0, 0], 1.9, [40, 0, 0], 2.1, [70, 0, 0], 3.0, [70, 0, 0]),
            "left_hand": rot(0, ZERO, 1.0, [40, 0, 0], 1.9, [40, 0, 0], 2.1, [70, 0, 0], 3.0, [70, 0, 0]),
            "core": {"scale": keys(0, [1, 1, 1], 2.1, [1, 1, 1], 2.4, [1.2, 1.2, 1.2], 2.7, [0.6, 0.6, 0.6], 3.0, [0.9, 0.9, 0.9])},
        }), loop=False),
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
        "misc.stunned": anim(2.2, {  # GrievingTreeEntity.STUN_TICKS
            "trunk": rot(0, ZERO, 0.3, [12, 0, 0], 1.8, [12, 0, 0], 2.2, ZERO),
            "crown": rot(0, ZERO, 0.3, [8, 0, 0], 1.8, [8, 0, 0], 2.2, ZERO),
        }, loop=False),
        "misc.uproot": anim(2.0, {
            "root": pos(0, ZERO, 0.6, [0, -4, 0], 1.2, [0, 6, 0], 2.0, ZERO),
            "trunk": rot(0, ZERO, 0.6, [0, 0, 8], 1.2, [0, 0, -8], 2.0, ZERO),
        }, loop=False),
    }
    write_model("grieving_tree", bones, animations, [12, 11], tex_w=512)


# --- Smoldering Lord: the greedy king, 3.5 blocks (PLAN 19.4) ----------------------------------------------------
# Blackened royal plate with gold trim and ember seams (the armor bone with its pauldrons, gorget, tassets and belt
# chains; it shatters in P2 and shows the burning body under it), a half-melted gold crown as geometry over a charred
# skull with ember eyes the size of a fist and a jaw that opens, an ash beard, a crimson cape of three segments with
# side tatters burning at the hem, gauntlets with fingers, a flamberge greatsword with a glowing wavy edge and an
# ember in the crossguard, held in the right hand.

def smoldering_lord():
    legs, body_h, head = 22, 22, 11
    top = legs + body_h
    flesh = Material(CHAR, base=2.0, noise=0.7, speck=0.05, top=1.0, bevel=0.4)

    def burning(face, rng):
        if face.side():
            ember_cracks(face, rng, max(2, face.w * face.h // 25), EMBER, EMBER_HOT, 1.0, (3, 6))

    plate_mat = Material(BLACK_STEEL, base=3.0, noise=0.4, edge=0.6, grad=1.0, top=1.0, bevel=0.9)
    gold = Material(GOLD, base=3.0, noise=0.5, bevel=0.8, speck=0.05)

    def plate_deco(face, rng):
        if face.side() or face.name == "top":
            trim(face, GOLD[2], ("top", "bottom"))
            if face.w >= 5 and face.h >= 5:
                rivet_ring(face, BLACK_STEEL[5], BLACK_STEEL[1], inset=1, step=3)
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
        # The charred skull: a heavy brow, two sockets the size of a fist with embers blazing in them, the nose
        # hole, the teeth of the upper jaw along the bottom edge.
        for x in range(face.w):
            face.set(x, 0, CHAR[0])
            face.set(x, 1, CHAR[1])
        for ex in (1, face.w - 5):
            face.rect(ex, 2, 4, 4, VOID)
            face.lit(ex + 1, 3, EMBER_HOT, 1.0)
            face.lit(ex + 2, 3, EMBER_HOT, 1.0)
            face.lit(ex + 1, 4, EMBER, 1.0)
            face.lit(ex + 2, 4, EMBER, 1.0)
            face.lit(ex, 3, EMBER_DEEP, 0.7)
            face.lit(ex + 3, 4, EMBER_DEEP, 0.7)
        face.rect(face.cx - 1, 6, 2, 2, VOID)
        face.lit(face.cx - 1, 7, EMBER_DEEP, 0.5)
        for x in range(1, face.w - 1, 2):
            face.set(x, face.h - 1, BONE[3])
            face.set(x + 1, face.h - 1, CHAR[0])
        burning(face, rng) if False else None
        ember_cracks(face, rng, 2, EMBER, EMBER_DEEP, 0.9, (2, 3))

    def skull_side(face, rng):
        burning(face, rng)
        sutures(face, rng, CHAR[0], 1)

    def jaw_deco(face, rng):
        if face.name == "front":
            for x in range(1, face.w - 1, 2):
                face.set(x, 0, BONE[4])
            face.hline(0, face.h - 1, face.w, CHAR[0])
        elif face.name == "top":
            for x in range(face.w):
                face.lit(x, face.h - 1, EMBER_DEEP, 0.6)
        else:
            burning(face, rng)

    def crown_band(face, rng):
        if face.side():
            trim(face, GOLD[4], ("top",))
            for x in range(1, face.w, 3):
                face.set(x, face.h - 1, GOLD[0])          # soot in the hollows
            for x in range(2, face.w, 5):                # inset gems, dulled
                face.lit(x, face.h // 2, hexc("ff5a3a"), 0.6)
        if face.name == "bottom":
            for x in range(0, face.w, 2):                # melted drips under the band
                face.set(x, rng.randrange(face.h), GOLD[1])

    def crown_point(face, rng):
        if face.side():
            trim(face, GOLD[4], ("top", "left"))
            face.set(face.w - 1, face.h - 1, GOLD[0])

    def drip(face, rng):
        if face.side():
            face.set(face.cx, face.h - 1, GOLD[1])
            face.lit(face.cx, face.h - 2, hexc("ff8a3a"), 0.5)

    def cape(face, rng):
        if face.name in ("front", "back"):
            fold_lines(face, rng, 3, -18)
            holes(face, rng, 1)
            for x in (0, face.w - 1):
                for y in range(face.h):
                    face.set(x, y, GOLD[1])

    def cape_hem(face, rng):
        cape(face, rng)
        if face.name in ("front", "back"):
            for x in range(face.w):
                d = rng.randint(1, 4)
                for k in range(d):
                    face.clear(x, face.h - 1 - k)
                face.lit(x, face.h - 1 - d, EMBER, 0.9)
                if rng.random() < 0.5:
                    face.lit(x, face.h - 2 - d, EMBER_DEEP, 0.7)

    def tatter(face, rng):
        if face.name in ("front", "back"):
            fold_lines(face, rng, 2, -16)
            ragged_edge(face, rng, "bottom", 3, 0.7)
            for x in range(face.w):
                for y in range(face.h - 1, max(0, face.h - 5), -1):
                    if face.get(x, y)[3]:
                        if rng.random() < 0.5:
                            face.lit(x, y, EMBER_DEEP, 0.8)
                        break

    def beard(face, rng):
        if face.name in ("front", "back"):
            for x in range(face.w):
                face.vline(x, 0, face.h, ASH[1 + (x % 3)])
            ragged_edge(face, rng, "bottom", 3, 0.7)
            for x in range(0, face.w, 2):
                face.lit(x, rng.randrange(face.h // 2, face.h), EMBER_DEEP, 0.5)

    def links(face, rng):
        if face.side():
            for y in range(face.h):
                face.set(0, y, IRON[4] if y % 2 else IRON[1])

    def gauntlet(face, rng):
        if face.side():
            trim(face, GOLD[2], ("top",))
            for y in range(2, face.h, 3):
                face.hline(0, y, face.w, BLACK_STEEL[1])

    boot = Material(BLACK_STEEL, base=2.6, noise=0.4, edge=0.6, bevel=0.8)
    crimson = Material(CRIMSON, base=2.8, noise=0.5, grain="v", bevel=0.3)
    iron = Material(IRON, base=2.4, noise=0.5, speck=0.08, bevel=0.5)
    # Animation signs (checked in the preview): -X raises an arm forward, swings a leg forward, tilts the head and
    # body back; +X bows the body forward and folds a leg back; -X opens the jaw.
    bones = [Bone("root", [0, 0, 0])]
    leg_flesh = Box([-7, 0, -3.5], [7, legs, 7], flesh, {"all": burning})
    greave = chamfered([-7, 0, -3.5], [7, 14, 7], boot, {"all": plate_deco}, c=1, inflate=0.5)
    cuisse = chamfered([-7.2, 13, -3.7], [7.4, 9, 7.4], boot.but(base=3.0), {"all": plate_deco}, c=1, inflate=0.4)
    knee = Box([-6.5, 11, -5.2], [6, 4, 2], gold.but(base=2.4), {"all": crown_point})
    toe = Box([-7, 0, -5.5], [7, 3, 2.5], boot.but(base=3.4), {"all": gauntlet})
    bones.append(Bone("right_leg", [-3.5, legs, 0], "root", None, [leg_flesh] + greave + cuisse + [knee, toe]))
    bones.append(Bone("left_leg", [3.5, legs, 0], "root", None, [mirror_box(b) for b in [leg_flesh] + greave + cuisse + [knee, toe]]))
    torso = chamfered([-8, legs, -4.5], [16, body_h, 9], flesh, {"all": burning}, c=1)
    bones.append(Bone("body", [0, legs, 0], "root", None, torso))
    # The cape: three segments down the back, each hanging from the one above, the hem burning; tatters at the sides.
    bones += chain_bones("cape", "body", [0, top - 1, 5.2], 3, 12, crimson, None, thickness=1, down=True,
                         box=lambda i, o, sz: [Box([-7 + i * 0.5, o[1], 5.2 + i * 0.3], [14 - i, 12.5, 0], crimson.but(base=2.8 - i * 0.3),
                                                   {"all": cape_hem if i == 2 else cape})])
    for side, sx in (("right", -1), ("left", 1)):
        bones += chain_bones(side + "_tatter", "body", [sx * 8.6, top - 2, 1], 2, 9, crimson.but(base=2.2), None, thickness=1,
                             box=lambda i, o, sz, sx=sx: [Box([sx * 8.6 - 0.5 * (sx < 0) * 0 - (0 if sx > 0 else 0), o[1], -1.5], [0, 9.5, 5],
                                                              crimson.but(base=2.2), {"all": tatter})])
    # The armor: breastplate, belly plate, back plate, gorget, pauldrons with spikes, belt with hanging chains,
    # tassets that swing. Shattered (hidden) in P2.
    breast = chamfered([-8.5, legs + 6, -5], [17, 16, 10], plate_mat, {"all": plate_deco}, c=1.5, inflate=0.5)
    belly = Box([-8.5, legs, -5], [17, 6, 10], plate_mat.but(base=2.4), {"all": plate_deco}, inflate=0.3)
    gorget = Box([-5.5, top - 1, -5], [11, 3, 9.5], plate_mat.but(base=3.6), {"all": plate_deco}, inflate=0.3)
    belt = Box([-8.8, legs + 4, -5.3], [17.6, 2, 10.6], gold.but(base=2.2), {"all": lambda f, r: rivet_ring(f, GOLD[0], step=2) if f.side() else None})
    pauldron = chamfered([-13, top - 5, -5.5], [8, 6, 11], plate_mat.but(base=3.4), {"all": plate_deco}, c=1.5)
    spike = Box([-12.5, top + 1, -1], [3, 5, 2], gold.but(base=2.6), {"all": crown_point}, rotation=[0, 0, 25], pivot=[-11, top + 1, 0])
    armor_boxes = breast + [belly, gorget, belt] + pauldron + [mirror_box(b) for b in pauldron] + [spike, mirror_box(spike)]
    bones.append(Bone("armor", [0, legs, 0], "body", None, armor_boxes))
    tasset = Box([-9.2, legs - 7, -4], [3, 8, 8], plate_mat.but(base=2.8), {"all": plate_deco})
    bones.append(Bone("right_tasset", [-8, legs + 1, 0], "armor", None, [tasset]))
    bones.append(Bone("left_tasset", [8, legs + 1, 0], "armor", None, [mirror_box(tasset)]))
    for side, sx in (("right", -1), ("left", 1)):
        bones += chain_bones(side + "_belt_chain", "armor", [sx * 6, legs + 4, -5.5], 2, 4, iron, {"all": links}, thickness=1)
    # The head: a chamfered skull, the jaw, the crown band (an octagon of gold) with five points and three melted
    # drips, the ash beard hanging from the jaw.
    skull = chamfered([-6, top, -5.5], [12, head, 11], flesh.but(base=2.6), {"front": face_front, "left": skull_side,
                                                                           "right": skull_side, "back": skull_side}, c=1.5)
    brow = Box([-6.3, top + 7, -6], [12.6, 2.5, 3], flesh.but(base=1.6), {"all": burning})
    sockets = [Box([-5.5 + k * 7, top + 4.5, -6.2], [4, 3.5, 1], Material(ramp("ff8a2a", "ffb347", "ffd27a", "fff0c0"), base=1.6, noise=0.5, glow=1.0))
               for k in (0, 1)]
    crown = octagon([-6.8, top + head - 1.5, -6.3], [13.6, 3, 12.6], gold, {"all": crown_band}, shrink=0.8)
    points = [Box([-6.5 + k * 3.2, top + head + 1.5, -6.5], [2, 2.5 + (k % 2) * 1.5, 1.5], gold.but(base=3.2), {"all": crown_point},
                  rotation=[0, 0, (k - 2) * -8], pivot=[-5.5 + k * 3.2, top + head + 1.5, -6]) for k in range(5)]
    points += [Box([-6.5, top + head + 1.5, 4.5], [13, 2, 1.5], gold.but(base=2.2), {"all": crown_point})]
    drips = [Box([-7.2, top + head - 4, -3 + k * 3.5], [1, 3, 1.5], gold.but(base=1.8), {"all": drip}) for k in range(3)]
    drips += [Box([6.2, top + head - 5, -2], [1, 4, 1.5], gold.but(base=1.8), {"all": drip})]
    bones.append(Bone("head", [0, top, 0], "body", None, skull + [brow] + sockets + crown + points + drips))
    bones.append(Bone("jaw", [0, top + 1, 3], "head", None, [
        Box([-5, top - 1.5, -5.8], [10, 3, 9], flesh.but(base=2.2), {"all": jaw_deco}),
    ]))
    bones.append(Bone("beard", [0, top - 1.5, -4.5], "jaw", None, [
        Box([-4, top - 9.5, -5.6], [8, 8, 0], Material(ASH, base=2.0, noise=0.5), {"all": beard}),
    ]))
    # Arms: upper arm in a rerebrace, an elbow cop, the forearm in a vambrace, a gauntlet with fingers.
    upper = Box([-13, top - 12, -2.5], [5, 12, 5], flesh, {"all": burning})
    rerebrace = chamfered([-13, top - 10, -2.5], [5, 9, 5], boot, {"all": plate_deco}, c=1, inflate=0.5)
    cop = Box([-13.4, top - 14, -2.9], [5.8, 3, 5.8], gold.but(base=2.4), {"all": crown_point})
    # The sword arm rests a little forward (the bone's rotation), so the blade's point just clears the ground.
    bones.append(Bone("right_arm", [-10.5, top - 2, 0], "body", [-12, 0, 0], [upper] + rerebrace + [cop]))
    fore = Box([-13, top - 22, -2.5], [5, 10, 5], flesh, {"all": burning})
    vambrace = chamfered([-13, top - 21, -2.5], [5, 8, 5], boot, {"all": plate_deco}, c=1, inflate=0.5)
    bones.append(Bone("right_forearm", [-10.5, top - 12, 0], "right_arm", None, [fore] + vambrace))
    hand = Box([-13.2, top - 25, -2.7], [5.4, 3.5, 5.4], boot.but(base=3.0), {"all": gauntlet})
    digits = fingers(-13, top - 25, -3.2, 3, [1.5, 3, 1.5], boot.but(base=3.2), spread=0.4, curl=35)
    thumb = Box([-8.2, top - 27, -1], [1.5, 2.5, 1.5], boot.but(base=3.2), rotation=[30, 0, -35], pivot=[-7.5, top - 25, -0.5])
    bones.append(Bone("right_hand", [-10.5, top - 22, 0], "right_forearm", None, [hand] + digits + [thumb]))
    for name in ("right_arm", "right_forearm", "right_hand"):
        src = bone(bones, name)
        bones.append(Bone(name.replace("right", "left"), [-src.pivot[0], src.pivot[1], src.pivot[2]],
                          src.parent.replace("right", "left"), None, [mirror_box(b) for b in src.boxes]))
    bone(bones, "left_arm").rotation = [-8, 0, 0]

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
            for y in range(2, face.h, 7):
                face.set(face.cx, y, BLACK_STEEL[4])

    # Held point-down in the right hand and tilted 40 degrees forward at rest (the bone's rotation; no animation
    # touches it): a low guard at idle, raised over the shoulder in the slam windup, out to the side in the sweeps.
    hand_y = top - 25
    steel = Material(BLACK_STEEL, base=3.8, noise=0.4, bevel=0.6)
    bones.append(Bone("greatsword", [-10.5, hand_y, 0], "right_hand", [-38, 0, 0], [
        Box([-11.5, hand_y - 1, -1], [2, 6, 2], Material(LEATHER, base=1.6, noise=0.4, grain="h")),
        Box([-12, hand_y + 5, -1.5], [3, 2, 3], gold.but(base=2.6)),
        chamfered([-16, hand_y - 3.5, -1.2], [11, 2.5, 2.4], gold.but(base=2.4), {"all": crown_point}, c=0.5)[0],
        Box([-16.5, hand_y - 4, -1], [2, 3.5, 2], gold.but(base=3.2), {"all": crown_point}),
        Box([-6.5, hand_y - 4, -1], [2, 3.5, 2], gold.but(base=3.2), {"all": crown_point}),
        Box([-11.5, hand_y - 3.5, -1.5], [2, 2.5, 3], Material(ramp("ff8a2a", "ffd27a", "fff0c0"), base=1.4, noise=0.5, glow=1.0)),
        Box([-12.5, hand_y - 16, -1], [4, 12.5, 2], steel, {"all": flamberge}),
        Box([-12.2, hand_y - 28, -0.8], [3.4, 12.5, 1.6], steel.but(base=4.2), {"all": flamberge}),
        Box([-11.5, hand_y - 31, -0.5], [2, 3.5, 1], steel.but(base=4.6)),
    ]))
    s1, s2, slam, lunge, mortar, line, descend = t(14), t(10), t(20), t(18), 2.0, t(30), t(30)
    cape_names = chain_names("cape", 3)
    hanging = [cape_names, chain_names("right_tatter", 2), chain_names("left_tatter", 2),
               chain_names("right_belt_chain", 2), chain_names("left_belt_chain", 2), ["beard"]]

    def hang_sway(length, amp, phase_step=0.15):
        out = {}
        for i, names in enumerate(hanging):
            out = merge(out, lag_sway(names, length, amp * (1.3 if i == 0 else 1), axis=0, phase=i * phase_step))
        out = merge(out, lag_sway(["right_tasset", "left_tasset"], length, amp * 0.6, axis=0, phase=0.3))
        return out

    def hang_whip(t0, amp, settle=0.6):
        out = {}
        for i, names in enumerate(hanging):
            out = merge(out, whip(names, t0 + 0.03 * i, amp * (1.3 if i == 0 else 0.8), axis=0, settle=settle))
        return out

    arm_rest = ZERO
    animations = {
        "misc.idle": anim(3.0, merge(hang_sway(3.0, 4), {
            "body": rot(0, ZERO, 1.5, e("easeInOutSine", [2, 0, 0]), 3.0, e("easeInOutSine", ZERO)),
            "armor": pos(0, ZERO, 1.5, e("easeInOutSine", [0, 0.4, -0.5]), 3.0, e("easeInOutSine", ZERO)),
            "right_arm": rot(0, arm_rest, 1.5, e("easeInOutSine", [-4, 0, 2]), 3.0, e("easeInOutSine", arm_rest)),
            "left_arm": rot(0, ZERO, 1.5, e("easeInOutSine", [-3, 0, -4]), 3.0, e("easeInOutSine", ZERO)),
            "left_forearm": rot(0, [-10, 0, 0], 1.5, [-14, 0, 0], 3.0, [-10, 0, 0]),
            "head": rot(0, ZERO, 1.0, e("easeInOutSine", [-3, 6, 0]), 2.2, e("easeInOutSine", [2, -5, 0]), 3.0, e("easeInOutSine", ZERO)),
            "jaw": rot(0, ZERO, 1.5, [-4, 0, 0], 3.0, ZERO),
        })),
        "move.walk": anim(1.2, merge(hang_sway(1.2, 10), {
            "right_leg": rot(0, [25, 0, 0], 0.6, e("easeInOutSine", [-25, 0, 0]), 1.2, e("easeInOutSine", [25, 0, 0])),
            "left_leg": rot(0, [-25, 0, 0], 0.6, e("easeInOutSine", [25, 0, 0]), 1.2, e("easeInOutSine", [-25, 0, 0])),
            "left_arm": rot(0, [15, 0, -3], 0.6, e("easeInOutSine", [-15, 0, -3]), 1.2, e("easeInOutSine", [15, 0, -3])),
            "right_arm": rot(0, [-4, 0, 2], 0.6, [2, 0, 2], 1.2, [-4, 0, 2]),
            "root": pos(0, ZERO, 0.15, e("easeOutQuad", [0, -0.8, 0]), 0.4, e("easeInOutSine", [0, 0.5, 0]), 0.75,
                        e("easeOutQuad", [0, -0.8, 0]), 1.0, e("easeInOutSine", [0, 0.5, 0]), 1.2, ZERO),
            "body": rot(0, [3, -5, 2], 0.6, e("easeInOutSine", [3, 5, -2]), 1.2, e("easeInOutSine", [3, -5, 2])),
            "head": rot(0, [-2, 5, -2], 0.6, e("easeInOutSine", [-2, -5, 2]), 1.2, e("easeInOutSine", [-2, 5, -2])),
        })),
        # Swing 1: the blade drawn back to the right and up, the body coiled, then cut across from right to left.
        "attack.greatsword_1": anim(s1 + 0.4, merge(hang_whip(s1 + 0.05, 30, 0.35), {
            "right_arm": rot(0, arm_rest, s1 * 0.6, e("easeInOutSine", [-80, -60, 10]), s1, [-95, -85, 10], s1 + 0.1,
                             e("easeOutBack", [-85, 65, 0]), s1 + 0.4, e("easeInOutSine", arm_rest)),
            "right_forearm": rot(0, ZERO, s1, [-20, 0, 0], s1 + 0.1, e("easeOutCubic", ZERO)),
            "left_arm": rot(0, ZERO, s1, [-20, 0, -25], s1 + 0.1, [10, 0, -20], s1 + 0.4, ZERO),
            "body": rot(0, ZERO, s1 * 0.6, e("easeInOutSine", [-5, 28, 0]), s1, [-8, 35, 0], s1 + 0.1, e("easeOutBack", [8, -30, 0]),
                        s1 + 0.4, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, s1, [0, -30, 0], s1 + 0.1, [0, 15, 0], s1 + 0.4, ZERO),
            "root": pos(0, ZERO, s1, [0, 0, 0.5], s1 + 0.1, [0, -0.5, -1.5], s1 + 0.4, ZERO),
        }), loop=False),
        # Swing 2: the backhand, from where the first ended, back across to the right.
        "attack.greatsword_2": anim(s2 + 0.4, merge(hang_whip(s2 + 0.05, 28, 0.35), {
            "right_arm": rot(0, [-85, 65, 0], s2, e("easeInOutSine", [-95, 80, 0]), s2 + 0.1, e("easeOutBack", [-90, -70, 5]),
                             s2 + 0.4, e("easeInOutSine", arm_rest)),
            "left_arm": rot(0, [10, 0, -20], s2, [-30, 0, -20], s2 + 0.1, [-10, 0, -25], s2 + 0.4, ZERO),
            "body": rot(0, [8, -30, 0], s2, e("easeInOutSine", [5, -38, 0]), s2 + 0.1, e("easeOutBack", [8, 30, 0]), s2 + 0.4, e("easeInOutSine", ZERO)),
            "head": rot(0, [0, 15, 0], s2, [0, 25, 0], s2 + 0.1, [0, -15, 0], s2 + 0.4, ZERO),
        }), loop=False),
        # Slam: both hands take the sword up over the head, he rises on his toes, hangs, and brings it down into the
        # ground with the whole body; the cape flies over his head and settles.
        "attack.slam": anim(slam + 0.6, merge(hang_whip(slam + 0.05, 45, 0.55), {
            "right_arm": rot(0, arm_rest, slam * 0.6, e("easeInOutSine", [-165, 0, 12]), slam, [-180, 0, 10], slam + 0.08,
                             e("easeOutBack", [-60, 0, 0]), slam + 0.35, [-55, 0, 0], slam + 0.6, e("easeInOutSine", arm_rest)),
            "left_arm": rot(0, ZERO, slam * 0.6, e("easeInOutSine", [-150, -20, -25]), slam, [-170, -20, -25], slam + 0.08,
                            e("easeOutBack", [-55, -10, -10]), slam + 0.35, [-50, -10, -10], slam + 0.6, e("easeInOutSine", ZERO)),
            "right_forearm": rot(0, ZERO, slam, [-25, 0, 0], slam + 0.08, e("easeOutCubic", ZERO)),
            "left_forearm": rot(0, ZERO, slam, [-25, 0, 0], slam + 0.08, e("easeOutCubic", ZERO)),
            "body": rot(0, ZERO, slam * 0.6, e("easeInOutSine", [-10, 0, 0]), slam, [-14, 0, 0], slam + 0.08, e("easeOutBack", [30, 0, 0]),
                        slam + 0.35, [26, 0, 0], slam + 0.6, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, slam, [-15, 0, 0], slam + 0.08, [12, 0, 0], slam + 0.6, ZERO),
            "jaw": rot(0, ZERO, slam, [-20, 0, 0], slam + 0.2, ZERO),
            "root": pos(0, ZERO, slam, [0, 1.5, 0], slam + 0.08, e("easeOutCubic", [0, -2, -1.5]), slam + 0.35, [0, -2, -1.5], slam + 0.6, ZERO),
            "right_leg": rot(0, ZERO, slam, [-5, 0, 0], slam + 0.08, [-25, 0, 0], slam + 0.6, ZERO),
            "left_leg": rot(0, ZERO, slam, [5, 0, 0], slam + 0.08, [20, 0, 0], slam + 0.6, ZERO),
        }), loop=False),
        # Lunge: the sword drawn back low at his hip, a crouch, then the thrust with the whole body behind it.
        "attack.lunge": anim(lunge + 0.5, merge(hang_whip(lunge + 0.08, 40, 0.42), {
            "right_arm": rot(0, arm_rest, lunge * 0.6, e("easeInOutSine", [25, 10, 0]), lunge, [35, 15, 0], lunge + 0.08,
                             e("easeOutBack", [-95, 0, 0]), lunge + 0.3, [-90, 0, 0], lunge + 0.5, e("easeInOutSine", arm_rest)),
            "right_forearm": rot(0, ZERO, lunge, [-30, 0, 0], lunge + 0.08, e("easeOutCubic", [5, 0, 0]), lunge + 0.5, ZERO),
            "left_arm": rot(0, ZERO, lunge, [-30, 0, -30], lunge + 0.08, [30, 0, -20], lunge + 0.5, ZERO),
            "body": rot(0, ZERO, lunge * 0.6, e("easeInOutSine", [-10, 20, 0]), lunge, [-14, 25, 0], lunge + 0.08,
                        e("easeOutBack", [22, -10, 0]), lunge + 0.5, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, lunge, [5, -20, 0], lunge + 0.08, [-10, 5, 0], lunge + 0.5, ZERO),
            "root": pos(0, ZERO, lunge, [0, -2, 2], lunge + 0.1, e("easeOutCubic", [0, 0, -4]), lunge + 0.5, ZERO),
            "right_leg": rot(0, ZERO, lunge, [20, 0, 0], lunge + 0.08, [-40, 0, 0], lunge + 0.5, ZERO),
            "left_leg": rot(0, ZERO, lunge, [-15, 0, 0], lunge + 0.08, [35, 0, 0], lunge + 0.5, ZERO),
        }), loop=False),
        # Mortar (P0, on the wall): the left arm points at the fighters and the order is given; the shells fly.
        "attack.mortar": anim(mortar, merge(hang_sway(mortar, 3), {
            "left_arm": rot(0, ZERO, 0.4, e("easeInOutSine", [-150, 0, -20]), 0.6, e("easeOutBack", [-120, 0, -15]), 1.5, [-120, 0, -15],
                            mortar, e("easeInOutSine", ZERO)),
            "left_forearm": rot(0, ZERO, 0.4, [-20, 0, 0], 0.6, ZERO),
            "head": rot(0, ZERO, 0.4, [-10, 0, 0], 0.6, [8, 0, 0], mortar, ZERO),
            "jaw": rot(0, ZERO, 0.55, [-25, 0, 0], 1.0, ZERO),
            "body": rot(0, ZERO, 0.4, [-8, 10, 0], 0.6, [6, 10, 0], mortar, ZERO),
        }), loop=False),
        # Flame line: the sword raised high, then swept down to the ground along the line.
        "attack.flame_line": anim(line + 0.5, merge(hang_whip(line + 0.05, 35, 0.45), {
            "right_arm": rot(0, arm_rest, line * 0.5, e("easeInOutSine", [-160, 0, 10]), line, [-175, 0, 10], line + 0.1,
                             e("easeOutBack", [-30, 0, 0]), line + 0.5, e("easeInOutSine", arm_rest)),
            "body": rot(0, ZERO, line * 0.5, [-8, 0, 0], line, [-10, 0, 0], line + 0.1, e("easeOutBack", [22, 0, 0]), line + 0.5, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, line, [-15, 0, 0], line + 0.1, [10, 0, 0], line + 0.5, ZERO),
            "left_arm": rot(0, ZERO, line, [-40, 0, -30], line + 0.1, [10, 0, -15], line + 0.5, ZERO),
        }), loop=False),
        # Descend: he crouches on the wall's edge with the sword raised, leaps, and lands in the courtyard on one knee.
        "misc.descend": anim(descend + 0.5, merge(hang_whip(descend + 0.05, 50, 0.5), {
            "root": pos(0, ZERO, descend * 0.6, e("easeInOutSine", [0, -4, 0]), descend * 0.85, [0, -4, 0], descend,
                        e("easeOutCubic", [0, 6, 0]), descend + 0.05, e("easeInQuad", [0, -5, 0]), descend + 0.5, e("easeInOutSine", ZERO)),
            "body": rot(0, ZERO, descend * 0.6, e("easeInOutSine", [25, 0, 0]), descend, [-10, 0, 0], descend + 0.05,
                        e("easeOutCubic", [30, 0, 0]), descend + 0.5, e("easeInOutSine", ZERO)),
            "right_leg": rot(0, ZERO, descend * 0.6, [-40, 0, 0], descend, [15, 0, 0], descend + 0.05, [85, 0, 0], descend + 0.5, ZERO),
            "left_leg": rot(0, ZERO, descend * 0.6, [-40, 0, 0], descend, [15, 0, 0], descend + 0.05, [-50, 0, 0], descend + 0.5, ZERO),
            "right_arm": rot(0, arm_rest, descend * 0.6, e("easeInOutSine", [-150, 0, 15]), descend, [-170, 0, 15], descend + 0.05,
                             e("easeOutCubic", [-20, 0, 25]), descend + 0.5, e("easeInOutSine", arm_rest)),
            "left_arm": rot(0, ZERO, descend * 0.6, [20, 0, -20], descend, [-120, 0, -30], descend + 0.05, [-40, 0, -30], descend + 0.5, ZERO),
            "head": rot(0, ZERO, descend * 0.6, [-10, 0, 0], descend + 0.05, [20, 0, 0], descend + 0.5, ZERO),
        }), loop=False),
        # Shatter (P2): he arches back and roars; the plates burst off him, the armor bone flung down and away.
        "misc.shatter": anim(1.2, merge(hang_whip(0.4, 45, 0.7), {
            "body": rot(0, ZERO, 0.3, e("easeInOutSine", [-22, 0, 0]), 0.4, e("easeOutBack", [-28, 0, 0]), 0.7, [12, 0, 0], 1.2, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, 0.3, [-30, 0, 0], 0.7, [10, 0, 0], 1.2, ZERO),
            "jaw": rot(0, ZERO, 0.3, [-35, 0, 0], 0.8, [-30, 0, 0], 1.2, ZERO),
            "right_arm": rot(0, arm_rest, 0.3, [-40, 0, 50], 0.7, [-10, 0, 20], 1.2, arm_rest),
            "left_arm": rot(0, ZERO, 0.3, [-40, 0, -50], 0.7, [-10, 0, -20], 1.2, ZERO),
            "armor": {"position": keys(0, ZERO, 0.35, [0, 0.5, 0], 0.4, e("easeOutQuad", [0, 2, -4]), 0.8, e("easeInQuad", [0, -16, -10]),
                                       1.2, [0, -40, -14]),
                      "rotation": keys(0, ZERO, 0.4, [-15, 0, 0], 1.2, [-70, 0, 20])},
            "root": pos(0, ZERO, 0.4, [0, 1, 1], 0.7, [0, -1, 0], 1.2, ZERO),
        }), loop=False),
        # Hazard (P2): the sword raised, the free hand opens to the sky, and the zone's curse is called down.
        "misc.hazard": anim(1.5, merge(hang_whip(0.7, 30, 0.8), {
            "right_arm": rot(0, arm_rest, 0.7, e("easeInOutSine", [-150, 0, 20]), 1.1, [-150, 0, 20], 1.5, e("easeInOutSine", arm_rest)),
            "left_arm": rot(0, ZERO, 0.7, e("easeInOutSine", [-100, 0, -60]), 1.1, [-100, 0, -60], 1.5, e("easeInOutSine", ZERO)),
            "left_forearm": rot(0, ZERO, 0.7, [-30, 0, 0], 1.5, ZERO),
            "head": rot(0, ZERO, 0.7, e("easeInOutSine", [-30, 0, 0]), 1.1, [-30, 0, 0], 1.5, ZERO),
            "jaw": rot(0, ZERO, 0.7, [-25, 0, 0], 1.1, [-25, 0, 0], 1.5, ZERO),
            "body": rot(0, ZERO, 0.7, [-10, 0, 0], 1.5, ZERO),
        }), loop=False),
        # Hurt: a short recoil, the head snapping back; the cape jumps.
        "misc.hurt": anim(0.4, merge(hang_whip(0.02, 16, 0.36), {
            "body": rot(0, ZERO, 0.08, e("easeOutCubic", [-8, 6, 0]), 0.4, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, 0.08, e("easeOutCubic", [-16, -10, 0]), 0.4, e("easeInOutSine", ZERO)),
            "root": pos(0, ZERO, 0.08, [0, 0, 1.2], 0.4, ZERO),
            "left_arm": rot(0, ZERO, 0.08, [-20, 0, -15], 0.4, ZERO),
        }), loop=False),
        # Stagger (P2, a heavy hit with the armor gone): he reels back a step, the sword arm flung wide, and recovers.
        "misc.stagger": anim(0.9, merge(hang_whip(0.05, 35, 0.8), {
            "root": pos(0, ZERO, 0.15, e("easeOutCubic", [0, -1, 4]), 0.5, [0, -1.5, 5], 0.9, e("easeInOutSine", ZERO)),
            "body": rot(0, ZERO, 0.15, e("easeOutBack", [-25, 8, 0]), 0.5, [-20, -6, 0], 0.9, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, 0.15, [-25, 10, 0], 0.5, [-15, -10, 0], 0.9, ZERO),
            "jaw": rot(0, ZERO, 0.15, [-30, 0, 0], 0.9, ZERO),
            "right_arm": rot(0, arm_rest, 0.15, e("easeOutBack", [-40, 0, 60]), 0.9, e("easeInOutSine", arm_rest)),
            "left_arm": rot(0, ZERO, 0.15, e("easeOutBack", [-50, 0, -50]), 0.9, e("easeInOutSine", ZERO)),
            "right_leg": rot(0, ZERO, 0.15, [30, 0, 0], 0.5, [25, 0, 0], 0.9, ZERO),
            "left_leg": rot(0, ZERO, 0.15, [-15, 0, 0], 0.9, ZERO),
        }), loop=False),
        # Death (3 s): he staggers, drives the sword into the ground to hold himself up, sinks onto one knee against
        # it, the head drops, then he slides off it and falls on his side; the cape and the beard settle.
        "misc.death": anim(3.0, merge(hang_whip(2.0, 40, 1.0), {
            "root": pos(0, ZERO, 0.4, e("easeOutCubic", [0, -1, 2]), 0.9, e("easeInQuad", [0, -9, 0]), 1.6, [0, -9, 0],
                        2.0, e("easeInCubic", [0, -17, 1]), 2.2, e("easeOutBounce", [0, -18, 2]), 3.0, [0, -18, 2]),
            "right_leg": rot(0, ZERO, 0.4, [-10, 0, 0], 0.9, e("easeInQuad", [-60, 0, 0]), 1.6, [-60, 0, 0], 2.0, e("easeInCubic", [-30, 0, -20]),
                             3.0, [-30, 0, -20]),
            "left_leg": rot(0, ZERO, 0.4, [15, 0, 0], 0.9, e("easeInQuad", [85, 0, 0]), 1.6, [85, 0, 0], 2.0, e("easeInCubic", [60, 0, -30]),
                            3.0, [60, 0, -30]),
            "body": rot(0, ZERO, 0.4, e("easeInOutSine", [-15, 5, 0]), 0.9, [20, 0, 5], 1.6, e("easeInOutSine", [30, 0, 5]), 2.0,
                        e("easeInCubic", [35, 0, -75]), 2.2, e("easeOutBounce", [38, 0, -82]), 3.0, [38, 0, -82]),
            "head": rot(0, ZERO, 0.4, [-20, 0, 0], 0.9, [10, 0, 0], 1.6, e("easeInOutSine", [40, 0, 0]), 2.0, [35, 0, 15], 2.2,
                        e("easeOutCubic", [20, 0, 10]), 2.7, e("easeInQuad", [30, 15, 10]), 3.0, [30, 15, 10]),
            "jaw": rot(0, ZERO, 0.4, [-30, 0, 0], 1.6, [-10, 0, 0], 2.2, [-35, 0, 0], 3.0, [-35, 0, 0]),
            # The sword hand goes down in front to plant the blade; after the fall the arm lies out.
            "right_arm": rot(0, arm_rest, 0.4, e("easeInOutSine", [-70, 0, 10]), 0.9, [-60, 0, 10], 1.6, [-55, 0, 10], 2.0,
                             e("easeInCubic", [-40, 0, 30]), 2.2, e("easeOutBounce", [-35, 0, 35]), 3.0, [-35, 0, 35]),
            "right_forearm": rot(0, ZERO, 0.4, [-20, 0, 0], 0.9, [-35, 0, 0], 2.0, [-10, 0, 0], 3.0, [-10, 0, 0]),
            "left_arm": rot(0, ZERO, 0.4, [-40, 0, -20], 0.9, e("easeInOutSine", [-20, 0, -10]), 1.6, [-10, 0, -8], 2.0,
                            e("easeInCubic", [-20, 0, -40]), 2.2, e("easeOutBounce", [-15, 0, -45]), 3.0, [-15, 0, -45]),
            "left_forearm": rot(0, ZERO, 0.9, [-40, 0, 0], 1.6, [-30, 0, 0], 2.2, [-10, 0, 0], 3.0, [-10, 0, 0]),
            "greatsword": rot(0, ZERO, 0.4, [20, 0, 0], 0.9, [40, 0, 0], 1.6, [40, 0, 0], 2.0, [10, 0, 0], 2.2, [0, 0, -30], 3.0, [0, 0, -30]),
        }), loop=False),
    }
    write_model("smoldering_lord", bones, animations, [3, 4], tex_w=128)


BOSSES = [("crypt_keeper", crypt_keeper), ("ash_colossus", ash_colossus), ("grieving_tree", grieving_tree),
          ("smoldering_lord", smoldering_lord)]


if __name__ == "__main__":
    wanted = set(sys.argv[1:])
    for name, build in BOSSES:
        if not wanted or name in wanted:
            build()
