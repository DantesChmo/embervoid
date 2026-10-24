#!/usr/bin/env python3
"""Model, animations and textures of the Ruined Sanctum's boss, the Fallen Sister, and her crying souls (PLAN 16.3,
DESIGN §2).

Run from the repository root: python3 tools/sanctum_boss_art.py [fallen_sister] [crying_soul]
Check with python3 tools/model_preview.py check fallen_sister crying_soul, then
python3 tools/model_preview.py entity fallen_sister and python3 tools/model_preview.py motion fallen_sister.

Attack animations hold their windup and land at the tick the server lands the move (the constants of
sanctum.boss.FallenSisterEntity and CryingSoulEntity, repeated below). The veil bones are hidden by the code once the
veil has burnt (phase 2); the coils on the forearm (chain_wrap) are scaled to nothing by the looping animations and
grow only while the chain winds up for a swing.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from art_kit import (ZERO, Bone, Box, Material, anim, chain_bones, chain_names, chamfered, e, ember_cracks,  # noqa: E402
                     fingers, hexc, keys, lag_sway, merge, mirror_box, octagon, pos, ramp, rot, scatter, t, whip,
                     write_model)
from reach_mob_art import (BRONZE, CHAR, EMBER, EMBER_DARK, EMBER_DEEP, EMBER_HOT, IRON, VOID, clear_all,  # noqa: E402
                           holes, open_front, ragged_edge, trim)
from overworld_mob_art import HAIR_ASH, glow_all, round_corners  # noqa: E402

# --- Server timings (sanctum.boss.FallenSisterEntity / CryingSoulEntity), in ticks --------------------------------
ARC_1_WINDUP = 18
ARC_2_WINDUP = 12
SLAM_WINDUP = 24
SPIN_WINDUP = 26
BACKSTEP_WINDUP = 8
VEIL_BURN_TICKS = 40
DEATH_TICKS = 50
HURT_TICKS = 8
DIVE_WINDUP = 20
DIVE_TICKS = 8


def bone(bones, name):
    return next(b for b in bones if b.name == name)


# --- Fallen Sister: 3 blocks, the Maiden's burnt-out twin ----------------------------------------------------------
# Tall and thin in a ragged ash-grey robe; a lace veil over a small gaunt face (embers weep from its eyes and drip
# from under the veil); a broken circlet; long bell sleeves; a censer of blackened bronze on a long chain in her right
# hand, held out in front of her, glowing through its piercings. Robe hems, the veil's train, the sleeves and the
# chain are separate bones for secondary motion. Phase 2 burns the veil away (bones veil and veil_train).

SISTER_ROBE = ramp("221f25", "2f2b33", "3d3842", "4c4652", "5c5663", "6e6776")
VEIL = ramp("57514e", "6d6662", "857d78", "9e958f", "b6ada6", "cbc2ba")
SISTER_SKIN = ramp("443c38", "574e48", "6b615a", "80756c", "94887e", "a89b90")


def sister():
    robe = Material(SISTER_ROBE, base=2.5, noise=0.55, grain="v", grad=0.9, top=0.6, edge=0.4, bevel=0.3)
    dark = robe.but(base=1.5)
    veil = Material(VEIL, base=2.6, noise=0.5, grain="v", grad=0.7, bevel=0.3)
    skin = Material(SISTER_SKIN, base=2.6, noise=0.35, grad=0.6, top=0.4, bevel=0.3)

    def folds(face, step=3):
        for x in range(1, face.w - 1, step):
            for y in range(face.h):
                face.tint(x, y, -16)

    def singed(face, rng, depth=2, chance=0.55):
        """A burnt hem: ragged, with embers still glowing along what is left of the edge."""
        if not face.side():
            return
        ragged_edge(face, rng, "bottom", depth, chance)
        for x in range(face.w):
            for y in range(face.h - 1, max(-1, face.h - depth - 3), -1):
                if face.get(x, y)[3]:
                    if rng.random() < 0.45:
                        face.lit(x, y, EMBER if rng.random() < 0.6 else EMBER_DEEP, 0.85)
                    elif rng.random() < 0.5:
                        face.set(x, y, CHAR[1])
                    break

    def skirt(face, rng):
        if face.side():
            folds(face)
            holes(face, rng, max(1, face.w * face.h // 70))
            singed(face, rng, 3, 0.6)
            ember_cracks(face, rng, 1, EMBER_DEEP, EMBER_DARK, 0.7, (2, 3))
        elif face.name == "bottom":
            clear_all(face)

    def panel(face, rng):
        if face.name in ("front", "back"):
            folds(face, 2)
            holes(face, rng, 2)
            singed(face, rng, 4, 0.7)
            for x in (0, face.w - 1):
                for y in range(face.h):
                    if face.get(x, y)[3] and rng.random() < 0.35:
                        face.clear(x, y)

    def bodice(face, rng):
        if face.name == "front":
            for y in range(face.h - 3):  # a crossed wrap like her sister's, frayed
                face.set(min(face.w - 1, 1 + y // 2), y, SISTER_ROBE[1])
                face.set(max(0, face.w - 2 - y // 2), y, SISTER_ROBE[1])
            # Where her flame burnt through: a hole over the heart, embers still in it.
            cx, cy = face.cx, 4
            for dx, dy in ((0, 0), (-1, 0), (0, 1), (-1, 1)):
                face.lit(cx + dx, cy + dy, EMBER_HOT if dy == 0 else EMBER, 1.0)
            for dx, dy in ((-2, 0), (1, 0), (-2, 1), (1, 1), (-1, -1), (0, -1), (-1, 2), (0, 2)):
                face.set(cx + dx, cy + dy, CHAR[0])
            face.lit(cx - 1, cy + 3, EMBER_DEEP, 0.7)  # a drip running down
            face.lit(cx - 1, cy + 4, EMBER_DARK, 0.6)
        if face.side():
            folds(face, 4)

    def sash(face, rng):
        if face.side():
            trim(face, EMBER_DARK, ("bottom",))
            if face.name == "front":
                face.vline(face.cx, 0, face.h, SISTER_ROBE[0])
                face.set(face.cx - 1, face.h - 1, EMBER_DARK)

    def mantle(face, rng):
        if face.side():
            folds(face, 2)
            ragged_edge(face, rng, "bottom", 2, 0.5)
        if face.name == "top":
            scatter(face, rng, 4, VEIL[1])

    def face_front(face, rng):
        # Ash hair parted over a gaunt face; burnt-out eyes that weep embers down the cheeks; a thin dark mouth.
        face.hline(0, 0, face.w, HAIR_ASH[1])
        face.set(0, 1, HAIR_ASH[1])
        face.set(face.w - 1, 1, HAIR_ASH[1])
        for ex in (0, face.w - 2):
            face.rect(ex, 2, 2, 1, VOID)
            face.lit(ex + (1 if ex == 0 else 0), 2, EMBER_HOT, 1.0)
            tx = ex + (1 if ex == 0 else 0)
            face.lit(tx, 3, EMBER, 0.9)
            face.lit(tx, 4, EMBER_DEEP, 0.75)
            face.lit(tx, 5, EMBER_DARK, 0.55)
        face.set(face.cx, 3, SISTER_SKIN[1])
        face.hline(face.cx - 1, 5, 2, VOID)

    def face_back(face, rng):
        face.rect(0, 0, face.w, face.h, HAIR_ASH[1])
        for x in range(0, face.w, 2):
            face.vline(x, 0, face.h, HAIR_ASH[2])

    def hair(face, rng):
        if face.name in ("front", "back"):
            face.rect(0, 0, face.w, face.h, HAIR_ASH[1])
            for x in range(0, face.w, 2):
                face.vline(x, 0, face.h, HAIR_ASH[2 if x % 4 else 0])
            ragged_edge(face, rng, "bottom", 3, 0.6)

    def lace(face, rng, holes_from=2):
        """Gauze: a net of small holes, denser toward the hem."""
        for y in range(holes_from, face.h):
            for x in range(face.w):
                if (x + y) % 2 == 0 and (y % 3 == 0 or rng.random() < 0.15 * y / face.h):
                    face.clear(x, y)

    def face_veil(face, rng):
        if face.name in ("front", "back"):
            folds(face, 2)
            lace(face, rng, 9)
            # The eyes glow through the gauze, and embers drip from under it.
            for ex in (1, face.w - 2):  # her eyes behind it
                face.lit(ex, 4, EMBER_DEEP, 0.6)
                face.lit(ex, 5, EMBER_DARK, 0.45)
            for x in range(face.w):
                if rng.random() < 0.5:
                    face.clear(x, face.h - 1)
                elif rng.random() < 0.6:
                    face.lit(x, face.h - 1, EMBER if rng.random() < 0.5 else EMBER_DEEP, 0.85)
            face.hline(0, 0, face.w, VEIL[4])

    def hood(face, rng):
        if face.name == "front":
            open_front(face, 1, 2)
            face.hline(0, 0, face.w, IRON[2])  # the band of blackened silver, an ember set in it
            face.hline(0, 1, face.w, IRON[3])
            face.lit(face.cx, 1, EMBER_HOT, 1.0)
        elif face.name in ("left", "right", "back"):
            folds(face, 2)
            face.hline(0, 1, face.w, IRON[3])
            ragged_edge(face, rng, "bottom", 1, 0.4)
        elif face.name == "bottom":
            clear_all(face)

    def drape(face, rng):
        if face.name in ("left", "right"):
            folds(face, 2)
            lace(face, rng, face.h * 2 // 3)
            ragged_edge(face, rng, "bottom", 2, 0.5)

    def train(face, rng):
        if face.name in ("front", "back"):
            folds(face, 2)
            lace(face, rng, face.h // 2)
            ragged_edge(face, rng, "bottom", 3, 0.6)
            for x in range(face.w):
                if rng.random() < 0.3:
                    face.lit(x, face.h - 4, EMBER_DEEP, 0.6)

    def circlet(face, rng):
        if face.side():
            for x in range(face.w):
                face.set(x, face.h - 1, IRON[2])
                if x % 2 == 0 and not (face.name == "back" and x == face.cx):  # one spike broken off at the back
                    for y in range(face.h - 1):
                        face.set(x, y, IRON[4 if y == 0 else 3])
                else:
                    for y in range(face.h - 1):
                        face.clear(x, y)
            if face.name == "front":
                face.lit(face.cx, face.h - 1, EMBER_HOT, 1.0)
        else:
            clear_all(face)

    def sleeve_top(face, rng):
        if face.side():
            folds(face, 2)
        elif face.name == "bottom":
            clear_all(face)

    def sleeve(face, rng):
        if face.side():
            folds(face, 2)
            singed(face, rng, 3, 0.65)
        elif face.name == "bottom":
            clear_all(face)

    def arm_skin(face, rng):
        if face.side():
            scatter(face, rng, 2, CHAR[2])
            ember_cracks(face, rng, 1, EMBER_DEEP, EMBER_DARK, 0.6, (1, 2))

    iron = Material(IRON, base=2.4, noise=0.5, speck=0.08)

    def links(face, rng):
        if face.side():
            for y in range(face.h):
                face.set(0, y, IRON[4] if y % 2 else IRON[1])

    bronze = Material(BRONZE, base=1.4, noise=0.5, speck=0.06, top=0.8, edge=0.6)

    def pierced(face, rng):
        if face.side():
            # Cross-shaped piercings with the embers inside glowing through, soot above them.
            for x in range(1, face.w - 1, 2):
                for y in (1, 3):
                    if (x // 2 + y // 2) % 2 == 0:
                        face.lit(x, y, EMBER_HOT if y == 1 else EMBER, 1.0)
                        face.set(x, y - 1, CHAR[1]) if y == 1 else None
            trim(face, BRONZE[3], ("top",))
        elif face.name == "top":
            face.lit(face.cx, face.h // 2, EMBER_HOT, 1.0)
            face.lit(face.cx - 1, face.h // 2, EMBER, 0.9)

    def lid(face, rng):
        if face.side():
            for x in range(face.w):
                if x % 2:
                    face.lit(x, face.h - 1, EMBER_DEEP, 0.8)
            trim(face, BRONZE[4], ("top",))

    W = 26  # the waist
    ember_drip = Material(ramp("d8521c", "ff8a2a", "ffb347", "ffd27a"), base=1.8, noise=0.5, glow=1.0)
    # Animation signs (checked in the preview): -X raises an arm forward, tilts the head and body back; +X bows the
    # body forward. Big forms are chamfered (three boxes) so the silhouette reads sculpted, not boxed (PLAN 19.4).
    bones = [
        Bone("root", [0, 0, 0]),
        # The robe widens in three chamfered tiers to the floor; panels over it hide the steps and flare out.
        Bone("skirt", [0, W, 0], "root", None,
             chamfered([-4, 19, -3], [8, 8, 6], robe, {"all": lambda f, r: folds(f) if f.side() else None}, c=0.5)
             + chamfered([-5, 9, -3.75], [10, 10.5, 7.5], robe.but(base=2.4), {"all": lambda f, r: (
                 folds(f) if f.side() else None, holes(f, r, 1) if f.side() else None)}, c=0.6)
             + chamfered([-6, 0, -4.5], [12, 9.5, 9], robe.but(base=2.1), {"all": skirt}, c=0.7, axes="xz")
             + [Box([-4.5, -0.5, -4.8], [1, 2, 0.6], ember_drip), Box([3, -0.3, -4.8], [1, 1.5, 0.6], ember_drip.but(base=1.2))]),
        Bone("hem_front", [0, 20, -4], "skirt", [-4, 0, 0], [
            Box([-4.5, 0.5, -4.7], [9, 19.5, 0], dark.but(base=1.9), {"all": panel}),
        ]),
        Bone("hem_back", [0, 22, 4], "skirt", [7, 0, 0], [
            Box([-5.5, 0, 4.6], [11, 22, 0], robe.but(base=2.2), {"all": panel}),
        ]),
        Bone("hem_right", [-5, 20, 0], "skirt", [0, 0, -5], [
            Box([-5.6, 0.5, -4], [0, 19.5, 8], robe.but(base=2.0), {"all": panel}),
        ]),
        Bone("hem_left", [5, 20, 0], "skirt", [0, 0, 5], [
            Box([5.6, 0.5, -4], [0, 19.5, 8], robe.but(base=2.0), {"all": panel}),
        ]),
        Bone("body", [0, W, 0], "root", [4, 0, 0],
             chamfered([-3.5, W, -2], [7, 12, 4], robe.but(base=2.5), {"all": bodice}, c=0.5)
             + [Box([-4, W, -2.5], [8, 2.5, 5], dark, {"all": sash})]
             + chamfered([-5, W + 8, -2.8], [10, 4, 5.6], robe.but(base=3.2), {"all": mantle}, c=0.6)
             + [Box([-0.75, W + 9.5, -3.1], [1.5, 1.5, 0.5], ember_drip.but(base=2.5))]),
        # The head: a chamfered skull on a thin neck, the ash hair down her back in two strands that swing, and the
        # broken circlet of blackened silver (seen once the veil has burnt).
        Bone("head", [0, W + 12, 0], "body", [-4, 0, 0],
             [Box([-1, W + 12, -1], [2, 1.5, 2], skin.but(base=1.6))]
             + chamfered([-2.5, W + 13.5, -2.5], [5, 6, 5], skin, {"front": face_front, "back": face_back,
                                                                  "left": lambda f, r: f.rect(0, 0, 1, 3, HAIR_ASH[1]),
                                                                  "right": lambda f, r: f.rect(f.w - 1, 0, 1, 3, HAIR_ASH[1]),
                                                                  "top": lambda f, r: f.rect(0, 0, f.w, f.h, HAIR_ASH[1])}, c=0.5)
             + octagon([-2.9, W + 18.2, -2.9], [5.8, 1.2, 5.8], iron.but(base=2.6), {"all": circlet}, shrink=0.8)
             + [Box([-0.6 + k * 1.8 - 1.8, W + 19.4, -3.0], [1, 1.5 + (k == 1) * 0.8, 0.8], iron.but(base=3.2)) for k in range(3)]
             + [Box([-0.5, W + 19.4, -3.1], [1, 0.8, 0.8], ember_drip.but(base=2.6))]),
    ]
    bones += chain_bones("hair", "head", [0, W + 17, 2.6], 2, 5.5, Material(HAIR_ASH, base=1.5, noise=0.4), None, thickness=1,
                         box=lambda i, o, sz: [Box([-2.6 + i * 0.6, o[1], 2.7 + i * 0.2], [5.2 - i * 1.2, 6, 0], Material(HAIR_ASH, base=1.5, noise=0.4),
                                                   {"all": hair})])
    # A mourning veil: a hood (an octagon, so it reads round) with a band of blackened silver, gauze down to the
    # chest in front and to the shoulders at the sides, embers dripping from its hem, a long train down her back in
    # two pieces, and a tassel of gauze at each shoulder.
    bones += [
        Bone("veil", [0, W + 19, 0], "head", None,
             octagon([-3.2, W + 13.6, -3.2], [6.4, 6.6, 6.4], veil, {"all": hood}, shrink=0.8)
             + [Box([-3.3, W + 5, -3.5], [6.6, 15.2, 0], veil.but(base=3.0), {"all": face_veil}),
                Box([-3.4, W + 7, -3.2], [0, 12.5, 6.4], veil.but(base=2.6), {"all": drape}),
                Box([3.4, W + 7, -3.2], [0, 12.5, 6.4], veil.but(base=2.6), {"all": drape}),
                Box([-2.5, W + 3.5, -3.7], [0.8, 1.8, 0.5], ember_drip), Box([0.2, W + 4, -3.7], [0.8, 1.2, 0.5], ember_drip.but(base=1.0)),
                Box([2.2, W + 3.8, -3.7], [0.8, 1.5, 0.5], ember_drip.but(base=2.4))]),
        Bone("veil_train", [0, W + 19, 3.2], "veil", [8, 0, 0], [
            Box([-3.2, W + 9, 3.4], [6.4, 10, 0], veil.but(base=2.4), {"all": train}),
        ]),
        Bone("veil_train_2", [0, W + 9, 3.4], "veil_train", None, [
            Box([-2.8, W, 3.5], [5.6, 9.2, 0], veil.but(base=2.1), {"all": train}),
        ]),
        Bone("veil_tassel_right", [-3.4, W + 12, 0], "veil", None, [Box([-3.6, W + 5, -1.5], [0, 7, 3], veil.but(base=2.2), {"all": drape})]),
        Bone("veil_tassel_left", [3.4, W + 12, 0], "veil", None, [Box([3.6, W + 5, -1.5], [0, 7, 3], veil.but(base=2.2), {"all": drape})]),
    ]
    sh = W + 11  # shoulders
    hand = sh - 15
    arm = Box([-5.5, hand + 2, -1], [2, 13, 2], skin.but(base=2.2), {"all": arm_skin})
    sleeve_box = Box([-6.4, sh - 8, -1.8], [3.8, 8, 3.6], robe.but(base=2.8), {"all": sleeve_top})
    bell_box = Box([-7.2, sh - 13, -2.6], [5.4, 5.5, 5.2], robe.but(base=2.5), {"all": sleeve})
    palm = Box([-5.6, hand - 0.5, -1.1], [2.2, 2.5, 2.2], skin.but(base=2.0), {"all": arm_skin})
    digits = fingers(-5.6, hand - 0.5, -1.2, 3, [0.6, 2.2, 0.7], skin.but(base=2.4), spread=0.2, curl=25, thumb=(2.0, 0.3))
    bones += [
        Bone("right_arm", [-4.5, sh, 0], "body", [-30, 0, 0], [arm]),
        Bone("right_sleeve", [-4.5, sh, 0], "right_arm", None, [sleeve_box, bell_box]),
        Bone("right_hand", [-4.5, hand + 2, 0], "right_arm", None, [palm] + digits),
        Bone("chain_wrap", [-4.5, hand + 4, 0], "right_arm", None, [
            Box([-6, hand + 1, -1.5], [3, 1, 3], iron.but(base=3.0), {"all": links}),
            Box([-6, hand + 3.5, -1.5], [3, 1, 3], iron.but(base=3.0), {"all": links}),
            Box([-6, hand + 6, -1.5], [3, 1, 3], iron.but(base=3.0), {"all": links}),
        ]),
        # The chain: four links on two bones (each link a thin box, turned 90 degrees from the one above).
        Bone("chain1", [-4.5, hand, 0], "right_arm", [30, 0, 0], [
            Box([-5, hand - 3, -0.4], [1, 3, 0.8], iron, {"all": links}),
            Box([-4.9, hand - 6, -0.5], [0.8, 3, 1], iron.but(base=2.0), {"all": links}),
        ]),
        Bone("chain2", [-4.5, hand - 6, 0], "chain1", None, [
            Box([-5, hand - 9, -0.4], [1, 3, 0.8], iron, {"all": links}),
            Box([-4.9, hand - 12, -0.5], [0.8, 3, 1], iron.but(base=2.0), {"all": links}),
        ]),
        Bone("censer", [-4.5, hand - 12, 0], "chain2", None,
             [Box([-5, hand - 13, -0.5], [1, 1, 1], iron.but(base=3.2)),
              Box([-6.5, hand - 14.5, -2], [4, 1.5, 4], bronze.but(base=2.2), {"all": lid})]
             + octagon([-7, hand - 19, -2.5], [5, 4.5, 5], bronze, {"all": pierced}, shrink=0.75)
             + [Box([-6, hand - 20, -1.5], [3, 1, 3], bronze.but(base=0.8)),
                Box([-5.2, hand - 21.5, -0.6], [0.8, 1.6, 0.6], ember_drip), Box([-3.8, hand - 21, -1.2], [0.6, 1.2, 0.6], ember_drip.but(base=1.0)),
                Box([-6.4, hand - 21.2, 0.4], [0.6, 1.4, 0.6], ember_drip.but(base=2.6))]),
        Bone("left_arm", [4.5, sh, 0], "body", [-35, 15, 8], [mirror_box(arm)]),
        Bone("left_sleeve", [4.5, sh, 0], "left_arm", None, [mirror_box(sleeve_box), mirror_box(bell_box)]),
        Bone("left_hand", [4.5, hand + 2, 0], "left_arm", None, [mirror_box(b) for b in [palm] + digits]),
    ]
    animations = sister_animations()
    write_model("fallen_sister", bones, animations, [3, 3.5], tex_w=128)


def sister_animations():
    hidden = [0, 0, 0]
    unwound = {"chain_wrap": {"scale": keys(0, hidden)}}

    def cloth(length, amp=1.0, phase=0.0):
        """The robe, veil and sleeves following the body a beat late: a looping sway per bone, each later than the
        one it hangs from."""
        def wave(a, ph, axis=0):
            pairs = []
            for i in range(5):
                v = [0, 0, 0]
                v[axis] = a * amp * math.sin(2 * math.pi * (i / 4 + ph + phase))
                pairs += [length * i / 4, v]
            return keys(*pairs)
        return merge({
            "hem_front": {"rotation": wave(-5, 0.10)},
            "hem_back": {"rotation": wave(6, 0.25)},
            "veil_train": {"rotation": wave(5, 0.35)},
            "veil_train_2": {"rotation": wave(6, 0.45)},
            "veil_tassel_right": {"rotation": wave(4, 0.3, 2)},
            "veil_tassel_left": {"rotation": wave(-4, 0.4, 2)},
            "right_sleeve": {"rotation": wave(4, 0.2, 2)},
            "left_sleeve": {"rotation": wave(-4, 0.3, 2)},
        }, lag_sway(chain_names("hair", 2), length, 5 * amp, axis=0, phase=0.3 + phase))

    def pendulum(length, amp, phase=0.0):
        """The censer swinging on its chain: the lower link and the censer lag the upper one."""

        def swing(a, ph):
            pairs = []
            for i in range(5):
                pairs += [length * i / 4, [a * math.sin(2 * math.pi * (i / 4 + ph + phase)), 0,
                                           a * 0.4 * math.cos(2 * math.pi * (i / 4 + ph + phase))]]
            return keys(*pairs)
        return {"chain1": {"rotation": swing(amp, 0)}, "chain2": {"rotation": swing(amp * 0.6, 0.12)},
                "censer": {"rotation": swing(amp * 0.4, 0.22)}}

    a1, a2, slam, spin, step = t(ARC_1_WINDUP), t(ARC_2_WINDUP), t(SLAM_WINDUP), t(SPIN_WINDUP), t(BACKSTEP_WINDUP)
    burn, death, hurt = t(VEIL_BURN_TICKS), t(DEATH_TICKS), t(HURT_TICKS)
    rest_arm = [0, 0, 0]
    loose = [chain_names("hair", 2), ["veil_train_2"], ["veil_tassel_right"], ["veil_tassel_left"]]

    def loose_whip(t0, amp, settle=0.5):
        """Hair, train and tassels flung after a strike at t0."""
        out = {}
        for i, names in enumerate(loose):
            out = merge(out, whip(names, t0 + 0.03 * i, amp, axis=0 if i < 2 else 2, settle=settle))
        return out

    def grip(t0, t1, curl=-25):
        """The censer hand closes on the chain for the swing and opens again."""
        return {"right_hand": rot(0, ZERO, t0, [curl, 0, 0], t1, [curl, 0, 0], t1 + 0.2, ZERO)}

    animations = {
        "misc.idle": anim(3.2, merge(unwound, cloth(3.2, 0.8), pendulum(3.2, 9), {
            "body": rot(0, ZERO, 1.6, [3, 0, 0], 3.2, ZERO),
            "head": rot(0, [8, 0, 4], 1.6, [12, -4, -2], 3.2, [8, 0, 4]),
            "root": pos(0, ZERO, 1.6, [0, -0.3, 0], 3.2, ZERO),
            "left_arm": rot(0, ZERO, 1.6, [-4, 0, 0], 3.2, ZERO),
        })),
        # She glides: no legs under the robe, the hems and the chain carry the motion.
        "move.walk": anim(1.6, merge(unwound, cloth(1.6, 1.6, 0.1), pendulum(1.6, 16, 0.1), {
            "root": pos(0, ZERO, 0.4, [0, 0.6, 0], 0.8, ZERO, 1.2, [0, 0.6, 0], 1.6, ZERO),
            "skirt": rot(0, [4, 3, 0], 0.8, [4, -3, 0], 1.6, [4, 3, 0]),
            "body": rot(0, [6, -3, 0], 0.8, [6, 3, 0], 1.6, [6, -3, 0]),
            "head": rot(0, [6, 3, 0], 0.8, [6, -3, 0], 1.6, [6, 3, 0]),
            "left_arm": rot(0, [8, 0, 0], 0.8, [-8, 0, 0], 1.6, [8, 0, 0]),
        })),
        # The chain winds around her forearm as she draws the censer back to her right (the coils grow, the chain
        # shortens), then it lashes out across her front from right to left and runs loose again.
        "attack.censer_arc_1": anim(a1 + 0.45, merge(loose_whip(a1 + 0.05, 30), grip(a1 * 0.3, a1 + 0.1), {
            "right_arm": rot(0, rest_arm, a1 * 0.7, [-60, -55, -35], a1, [-70, -65, -40],
                             a1 + 0.1, e("easeOutBack", [-80, 60, -20]), a1 + 0.45, e("easeInOutSine", rest_arm)),
            "body": rot(0, ZERO, a1, e("easeInOutSine", [4, 35, 0]), a1 + 0.1, e("easeOutBack", [0, -30, 0]), a1 + 0.45, e("easeInOutSine", ZERO)),
            "skirt": rot(0, ZERO, a1, [0, 12, 0], a1 + 0.15, [0, -14, 0], a1 + 0.45, ZERO),
            "chain_wrap": {"scale": keys(0, hidden, a1 * 0.25, [0.4, 0.4, 0.4], a1 * 0.8, [1, 1, 1], a1, [1, 1, 1],
                                         a1 + 0.05, hidden)},
            "chain1": rot(0, ZERO, a1 * 0.8, [-90, 0, 40], a1, [-100, 0, 50], a1 + 0.1, [-20, 0, -70],
                          a1 + 0.25, [15, 0, -20], a1 + 0.45, ZERO),
            "chain2": rot(0, ZERO, a1 * 0.8, [-60, 0, 20], a1 + 0.1, [0, 0, -30], a1 + 0.3, [10, 0, 10], a1 + 0.45, ZERO),
            "censer": rot(0, ZERO, a1, [0, 0, 20], a1 + 0.15, [0, 0, -40], a1 + 0.45, ZERO),
            "hem_front": rot(0, ZERO, a1 + 0.1, [-6, 0, 0], a1 + 0.3, [10, 0, 0], a1 + 0.45, ZERO),
            "veil_train": rot(0, ZERO, a1, [6, 0, -8], a1 + 0.2, [14, 0, 12], a1 + 0.45, ZERO),
            "right_sleeve": rot(0, ZERO, a1, [0, 0, 10], a1 + 0.15, [0, 0, -18], a1 + 0.45, ZERO),
        }), loop=False),
        # The backhand: from where the first arc ended, a shorter windup back across to the right.
        "attack.censer_arc_2": anim(a2 + 0.5, merge(loose_whip(a2 + 0.05, 28), grip(0, a2 + 0.1), {
            "right_arm": rot(0, [-80, 60, -20], a2, e("easeInOutSine", [-85, 75, -15]), a2 + 0.1, e("easeOutBack", [-70, -55, -30]), a2 + 0.5, e("easeInOutSine", rest_arm)),
            "body": rot(0, [0, -30, 0], a2, e("easeInOutSine", [0, -38, 0]), a2 + 0.1, e("easeOutBack", [0, 30, 0]), a2 + 0.5, e("easeInOutSine", ZERO)),
            "skirt": rot(0, [0, -14, 0], a2 + 0.15, [0, 14, 0], a2 + 0.5, ZERO),
            "chain_wrap": {"scale": keys(0, hidden, a2 * 0.5, [0.7, 0.7, 0.7], a2, [1, 1, 1], a2 + 0.05, hidden)},
            "chain1": rot(0, [15, 0, -20], a2, [-80, 0, -50], a2 + 0.1, [-20, 0, 70], a2 + 0.3, [10, 0, 20], a2 + 0.5, ZERO),
            "chain2": rot(0, ZERO, a2, [-40, 0, -20], a2 + 0.12, [0, 0, 30], a2 + 0.5, ZERO),
            "censer": rot(0, ZERO, a2 + 0.15, [0, 0, 40], a2 + 0.5, ZERO),
            "veil_train": rot(0, [14, 0, 12], a2 + 0.2, [14, 0, -12], a2 + 0.5, ZERO),
            "left_sleeve": rot(0, ZERO, a2 + 0.15, [0, 0, 16], a2 + 0.5, ZERO),
        }), loop=False),
        # Both hands raise the censer over her head and she rises on the robe, the chain winding up short; then it
        # comes down in front of her into the floor, she bows over it, and straightens slowly.
        "attack.slam": anim(slam + 0.9, merge(loose_whip(slam + 0.05, 40), grip(slam * 0.5, slam + 0.1, -35), {
            "right_arm": rot(0, rest_arm, slam * 0.75, [-160, 0, 10], slam, [-170, 0, 12], slam + 0.08, e("easeOutBack", [-45, 0, 5]),
                             slam + 0.55, [-45, 0, 5], slam + 0.9, rest_arm),
            "left_arm": rot(0, ZERO, slam * 0.75, [-125, -15, -20], slam, [-135, -15, -20], slam + 0.08, [-15, -10, -10],
                            slam + 0.55, [-15, -10, -10], slam + 0.9, ZERO),
            "body": rot(0, ZERO, slam, e("easeInOutSine", [-14, 0, 0]), slam + 0.08, e("easeOutBack", [28, 0, 0]), slam + 0.55, [24, 0, 0], slam + 0.9, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, slam, [-20, 0, 0], slam + 0.1, [15, 0, 0], slam + 0.9, ZERO),
            "root": pos(0, ZERO, slam, [0, 2.5, 0], slam + 0.08, [0, -1.5, 0], slam + 0.55, [0, -1.5, 0], slam + 0.9, ZERO),
            "chain_wrap": {"scale": keys(0, hidden, slam * 0.6, [1, 1, 1], slam, [1, 1, 1], slam + 0.04, hidden)},
            "chain1": rot(0, ZERO, slam * 0.75, [-150, 0, 0], slam, [-160, 0, 0], slam + 0.08, [-40, 0, 0],
                          slam + 0.55, [-30, 0, 0], slam + 0.9, ZERO),
            "chain2": rot(0, ZERO, slam * 0.75, [-20, 0, 0], slam + 0.08, [-10, 0, 0], slam + 0.2, [25, 0, 0],
                          slam + 0.9, ZERO),
            "censer": rot(0, ZERO, slam + 0.08, [30, 0, 0], slam + 0.25, [-20, 0, 0], slam + 0.9, ZERO),
            "hem_front": rot(0, ZERO, slam, [-6, 0, 0], slam + 0.12, [18, 0, 0], slam + 0.5, [8, 0, 0], slam + 0.9, ZERO),
            "hem_back": rot(0, ZERO, slam, [10, 0, 0], slam + 0.12, [-6, 0, 0], slam + 0.9, ZERO),
            "veil_train": rot(0, ZERO, slam, [-10, 0, 0], slam + 0.15, [30, 0, 0], slam + 0.9, ZERO),
            "right_sleeve": rot(0, ZERO, slam, [20, 0, 0], slam + 0.15, [-15, 0, 0], slam + 0.9, ZERO),
            "left_sleeve": rot(0, ZERO, slam, [20, 0, 0], slam + 0.15, [-15, 0, 0], slam + 0.9, ZERO),
        }), loop=False),
        # She sinks low and lets the chain run out, the censer starting to circle at her side; then she turns twice
        # on the spot with the chain straight out, the robe flaring, and slows to a stop.
        "attack.spin": anim(spin + 1.0, merge(loose_whip(spin + 0.3, 45), grip(spin * 0.5, spin + 0.8, -30), {
            "root": {"rotation": keys(0, ZERO, spin, e("easeInOutSine", [0, 25, 0]), spin + 0.5, e("easeInQuad", [0, -720, 0]), spin + 0.7, [0, -740, 0],
                                      spin + 1.0, e("easeOutSine", [0, -720, 0])),
                     "position": keys(0, ZERO, spin, [0, -2, 0], spin + 0.5, [0, -1, 0], spin + 1.0, ZERO)},
            "right_arm": rot(0, rest_arm, spin * 0.5, [-40, 0, -50], spin, [-60, 0, -75], spin + 0.5, [-80, 0, -85],
                             spin + 1.0, rest_arm),
            "left_arm": rot(0, ZERO, spin, [-40, 0, 40], spin + 0.5, [-60, 0, 60], spin + 1.0, ZERO),
            "chain1": rot(0, ZERO, spin * 0.3, [-20, 30, 0], spin * 0.6, [-30, -60, 30], spin, [-40, 0, 60],
                          spin + 0.5, [-10, 0, 85], spin + 0.8, [0, 0, 30], spin + 1.0, ZERO),
            "chain2": rot(0, ZERO, spin * 0.6, [0, 0, 20], spin + 0.5, [0, 0, 5], spin + 0.85, [0, 0, -20], spin + 1.0, ZERO),
            "censer": rot(0, ZERO, spin, [0, 90, 0], spin + 0.5, [0, 360, 0], spin + 1.0, ZERO),
            "skirt": rot(0, ZERO, spin, [0, 15, 0], spin + 0.25, [0, -10, 0], spin + 0.55, [0, 20, 0], spin + 1.0, ZERO),
            "hem_front": rot(0, ZERO, spin, [-10, 0, 0], spin + 0.5, [-35, 0, 0], spin + 0.75, [5, 0, 0], spin + 1.0, ZERO),
            "hem_back": rot(0, ZERO, spin, [15, 0, 0], spin + 0.5, [45, 0, 0], spin + 0.75, [5, 0, 0], spin + 1.0, ZERO),
            "veil_train": rot(0, ZERO, spin, [10, 0, 0], spin + 0.5, [55, 0, 0], spin + 0.8, [10, 0, 0], spin + 1.0, ZERO),
            "right_sleeve": rot(0, ZERO, spin + 0.5, [0, 0, -35], spin + 1.0, ZERO),
            "left_sleeve": rot(0, ZERO, spin + 0.5, [0, 0, 35], spin + 1.0, ZERO),
        }), loop=False),
        # A crouch and a hop back from someone behind her: the hems trail forward as she lands.
        "misc.backstep": anim(step + 0.6, merge(loose_whip(step + 0.2, 25), {
            "root": pos(0, ZERO, step, [0, -1.5, 0], step + 0.2, [0, 3, 3], step + 0.4, [0, 0, 1], step + 0.6, ZERO),
            "body": rot(0, ZERO, step, [12, 0, 0], step + 0.2, [-10, 0, 0], step + 0.6, ZERO),
            "hem_front": rot(0, ZERO, step + 0.2, [-25, 0, 0], step + 0.45, [8, 0, 0], step + 0.6, ZERO),
            "hem_back": rot(0, ZERO, step + 0.2, [-8, 0, 0], step + 0.45, [20, 0, 0], step + 0.6, ZERO),
            "veil_train": rot(0, ZERO, step + 0.2, [-15, 0, 0], step + 0.45, [25, 0, 0], step + 0.6, ZERO),
            "chain1": rot(0, ZERO, step + 0.2, [-35, 0, 0], step + 0.45, [20, 0, 0], step + 0.6, ZERO),
        }), loop=False),
        # Phase 2: she clutches her head as the veil catches fire and burns away upward (shrinks to nothing), then
        # throws her head back in a scream (the souls come out of it).
        "misc.veil_burn": anim(burn + 0.4, {
            # The censer hand drops and shakes; the other claws at the burning veil.
            "right_arm": rot(0, rest_arm, 0.35, [25, 0, -15], burn * 0.4, [20, 0, -10], burn * 0.6, [28, 0, -18],
                             burn, [-30, 0, -40], burn + 0.4, rest_arm),
            "left_arm": rot(0, ZERO, 0.35, [-150, -20, -30], burn * 0.7, [-145, -25, -25], burn, [-60, 0, 30],
                            burn + 0.4, ZERO),
            "head": rot(0, ZERO, 0.35, [20, 0, 0], burn * 0.4, [25, 8, 0], burn * 0.6, [25, -8, 0], burn * 0.75, [-35, 0, 0],
                        burn, [-40, 0, 0], burn + 0.4, ZERO),
            "body": rot(0, ZERO, 0.35, [15, 0, 0], burn * 0.7, [18, 0, 0], burn * 0.8, [-15, 0, 0], burn + 0.4, ZERO),
            "veil": {"scale": keys(0, [1, 1, 1], burn * 0.2, [1.05, 1.05, 1.05], burn * 0.4, [0.95, 0.8, 0.95],
                                   burn * 0.6, [0.7, 0.45, 0.7], burn * 0.75, [0.3, 0.15, 0.3], burn * 0.8, hidden,
                                   burn + 0.4, hidden),
                     "position": keys(0, ZERO, burn * 0.75, [0, 2, 0], burn * 0.8, [0, 2, 0])},
            "veil_train": rot(0, ZERO, burn * 0.3, [30, 0, 0], burn * 0.6, [60, 0, 10]),
            "hem_front": rot(0, ZERO, burn * 0.8, [-12, 0, 0], burn + 0.4, ZERO),
            "hem_back": rot(0, ZERO, burn * 0.8, [18, 0, 0], burn + 0.4, ZERO),
            "chain1": rot(0, ZERO, 0.35, [-30, 0, 15], burn * 0.5, [-15, 0, -10], burn, [-20, 0, 25], burn + 0.4, ZERO),
            "chain2": rot(0, ZERO, 0.5, [-15, 0, 0], burn * 0.6, [10, 0, 0], burn + 0.4, ZERO),
        }, loop=False),
        # A flinch that never stops a windup (the code plays it only between moves).
        "misc.hurt": anim(hurt, merge(loose_whip(0.02, 14), {
            "body": rot(0, ZERO, hurt * 0.3, [-9, 6, 0], hurt, ZERO),
            "head": rot(0, ZERO, hurt * 0.3, [-14, -8, 0], hurt, ZERO),
            "root": pos(0, ZERO, hurt * 0.3, [0, 0, 0.8], hurt, ZERO),
            "hem_front": rot(0, ZERO, hurt * 0.4, [-12, 0, 0], hurt, ZERO),
            "veil_train": rot(0, ZERO, hurt * 0.45, [14, 0, 0], hurt, ZERO),
            "chain1": rot(0, ZERO, hurt * 0.4, [-18, 0, 8], hurt, ZERO),
        }), loop=False),
        # Stagger (a heavy hit): she is thrown back a step, the chain flung out, and gathers herself.
        "misc.stagger": anim(0.9, merge(loose_whip(0.03, 35, 0.8), {
            "root": pos(0, ZERO, 0.15, e("easeOutCubic", [0, -1, 3]), 0.5, [0, -1.5, 4], 0.9, e("easeInOutSine", ZERO)),
            "body": rot(0, ZERO, 0.15, e("easeOutBack", [-22, 10, 0]), 0.5, [-16, -8, 0], 0.9, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, 0.15, [-25, 12, 0], 0.5, [-15, -10, 0], 0.9, ZERO),
            "right_arm": rot(0, rest_arm, 0.15, e("easeOutBack", [-50, 0, -50]), 0.9, e("easeInOutSine", rest_arm)),
            "left_arm": rot(0, ZERO, 0.15, e("easeOutBack", [-60, 0, 40]), 0.9, e("easeInOutSine", ZERO)),
            "chain1": rot(0, ZERO, 0.15, [-40, 0, -30], 0.45, [20, 0, 20], 0.9, ZERO),
            "chain2": rot(0, ZERO, 0.2, [-30, 0, -10], 0.5, [25, 0, 10], 0.9, ZERO),
            "hem_front": rot(0, ZERO, 0.2, [-20, 0, 0], 0.55, [8, 0, 0], 0.9, ZERO),
            "hem_back": rot(0, ZERO, 0.2, [-10, 0, 0], 0.55, [18, 0, 0], 0.9, ZERO),
            "skirt": rot(0, ZERO, 0.15, [-6, 0, 0], 0.9, ZERO),
        }), loop=False),
        # She sways, the censer slips from her hand to the floor, she sinks to her knees in the robe and folds
        # forward over it; the last embers go out (the server bursts them into ash at the end).
        "misc.death": anim(death, merge(loose_whip(death * 0.55, 30), {
            "root": pos(0, ZERO, death * 0.25, [0, 0, 0.5], death * 0.55, [0, -9, 0], death * 0.8, [0, -11, -1],
                        death, [0, -12, -1]),
            "skirt": {"scale": keys(0, [1, 1, 1], death * 0.55, [1.15, 0.55, 1.15], death, [1.25, 0.45, 1.25])},
            "body": rot(0, ZERO, death * 0.2, [-8, 0, 6], death * 0.55, [10, 0, -4], death * 0.8, [45, 0, 0], death, [55, 0, 0]),
            "head": rot(0, ZERO, death * 0.2, [-20, 10, 0], death * 0.6, [25, 0, 0], death, [35, -10, 0]),
            "right_arm": rot(0, rest_arm, death * 0.25, [-50, 0, -20], death * 0.4, [10, 0, -10], death, [20, 0, -6]),
            "left_arm": rot(0, ZERO, death * 0.25, [-60, -10, 15], death * 0.55, [5, 0, 10], death, [15, 0, 6]),
            "chain1": rot(0, ZERO, death * 0.25, [-30, 0, 0], death * 0.4, [25, 0, 0], death * 0.6, [60, 0, 0], death, [70, 0, 0]),
            "chain2": rot(0, ZERO, death * 0.4, [30, 0, 0], death * 0.6, [40, 0, 0], death, [20, 0, 0]),
            "censer": {"rotation": keys(0, ZERO, death * 0.45, [40, 0, 70], death, [80, 0, 90])},
            "hem_front": rot(0, ZERO, death * 0.55, [-30, 0, 0], death, [-45, 0, 0]),
            "hem_back": rot(0, ZERO, death * 0.55, [30, 0, 0], death, [50, 0, 0]),
            "veil_train": rot(0, ZERO, death * 0.6, [-30, 0, 0], death, [-10, 0, 0]),
            "right_sleeve": rot(0, ZERO, death, [10, 0, 0]),
            "left_sleeve": rot(0, ZERO, death, [10, 0, 0]),
        }), loop=False),
    }
    return animations


# --- Crying soul: one of her tears, about half a block ------------------------------------------------------------
# A small weeping face of pale soul-light under a scrap of veil, embers for tears, trailing a tail of mist in three
# links; two wisps for arms. Circles the Sister, rears back with a shriek before it dives, hangs dazed after it.

SOUL = ramp("3c5a6e", "5e8aa0", "8ab8c8", "b8dde6", "e4f6f8")


def crying_soul():
    light = Material(SOUL, base=2.6, noise=0.5, grad=0.8, top=0.6)

    def soul_face(face, rng):
        if face.name == "front":
            for ex in (1, face.w - 2):
                face.set(ex, 2, hexc("1f2c38"))      # hollow eyes, embers running from them
                face.lit(ex, 3, EMBER, 1.0)
                face.lit(ex, 4, EMBER_DEEP, 0.8)
            face.hline(face.cx - 1, 4, 2, hexc("1f2c38"))  # the mouth, crying out
        if face.name != "bottom":
            round_corners(face)

    def tail(face, rng):
        if face.w >= 3 and face.h >= 3:
            round_corners(face)

    def scrap(face, rng):
        if face.name == "front":
            face.hline(0, face.h - 1, face.w, VEIL[1])
        elif face.name == "bottom":
            clear_all(face)
        elif face.side():
            ragged_edge(face, rng, "bottom", 1, 0.5)

    glow = glow_all(255, 0.85)
    dim = glow_all(255, 0.6)
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 6, 0], "root", None, [
            Box([-3, 3, -3], [6, 6, 6], light, {"all": lambda f, r: (soul_face(f, r), glow(f, r))}),
            # A scrap of her veil over its crown and down its back.
            Box([-3.3, 8, -3.3], [6.6, 1.5, 6.6], Material(VEIL, base=2.8, noise=0.5, grain="v"), {"all": scrap}),
            Box([-3, 4, 3.4], [6, 5, 0], Material(VEIL, base=2.4, noise=0.5, grain="v"),
                {"all": lambda f, r: ragged_edge(f, r, "bottom", 2, 0.5)}),
        ]),
        Bone("tail1", [0, 5.5, 3], "body", None, [
            Box([-2, 3.5, 3], [4, 4, 3.5], light.but(base=2.0), {"all": lambda f, r: (tail(f, r), dim(f, r))}),
        ]),
        Bone("tail2", [0, 5, 6.5], "tail1", None, [
            Box([-1.5, 4, 6.5], [3, 2.5, 3.5], light.but(base=1.6), {"all": lambda f, r: (tail(f, r), dim(f, r))}),
        ]),
        Bone("tail3", [0, 5, 10], "tail2", None, [
            Box([-0.75, 4.25, 10], [1.5, 1.5, 3.5], light.but(base=1.2), {"all": lambda f, r: (tail(f, r), dim(f, r))}),
        ]),
    ]
    wisp = Box([-4, 2.5, -0.75], [1.5, 4, 1.5], light.but(base=2.2), {"all": lambda f, r: (tail(f, r), dim(f, r))})
    bones += [
        Bone("right_wisp", [-2.5, 6, 0], "body", [0, 0, 15], [wisp]),
        Bone("left_wisp", [2.5, 6, 0], "body", [0, 0, -15], [mirror_box(wisp)]),
    ]
    tail_wave = {
        "tail1": rot(0, [0, 12, 0], 0.3, [8, 0, 0], 0.6, [0, -12, 0], 0.9, [-8, 0, 0], 1.2, [0, 12, 0]),
        "tail2": rot(0, [0, -6, 0], 0.3, [0, 16, 0], 0.6, [0, 6, 0], 0.9, [0, -16, 0], 1.2, [0, -6, 0]),
        "tail3": rot(0, [0, -16, 0], 0.3, [0, -6, 0], 0.6, [0, 16, 0], 0.9, [0, 6, 0], 1.2, [0, -16, 0]),
    }
    dive = t(DIVE_WINDUP)
    animations = {
        "misc.idle": anim(1.2, merge(tail_wave, {
            "body": {"position": keys(0, ZERO, 0.6, [0, 1, 0], 1.2, ZERO), "rotation": keys(0, [6, 0, 0], 0.6, [-4, 0, 0], 1.2, [6, 0, 0])},
            "right_wisp": rot(0, ZERO, 0.6, [0, 0, 20], 1.2, ZERO),
            "left_wisp": rot(0, ZERO, 0.6, [0, 0, -20], 1.2, ZERO),
        })),
        # Rears back with its mouth open (the shriek), the tail lashing and the wisps spread, then pitches forward.
        "attack.dive": anim(dive + t(DIVE_TICKS), {
            "body": rot(0, ZERO, dive * 0.6, [-40, 0, 0], dive * 0.8, [-35, 0, 0], dive, [-45, 0, 0], dive + 0.1, [50, 0, 0],
                        dive + t(DIVE_TICKS), [40, 0, 0]),
            "tail1": rot(0, ZERO, dive * 0.3, [0, 25, 0], dive * 0.6, [30, -25, 0], dive, [40, 0, 0], dive + 0.1, [-20, 0, 0]),
            "tail2": rot(0, ZERO, dive * 0.3, [0, -30, 0], dive * 0.6, [20, 30, 0], dive, [25, 0, 0], dive + 0.15, [-25, 0, 0]),
            "tail3": rot(0, ZERO, dive * 0.4, [0, 35, 0], dive * 0.7, [0, -35, 0], dive, [20, 0, 0], dive + 0.2, [-30, 0, 0]),
            "right_wisp": rot(0, ZERO, dive * 0.6, [-30, 0, 70], dive, [-40, 0, 80], dive + 0.1, [40, 0, 10]),
            "left_wisp": rot(0, ZERO, dive * 0.6, [-30, 0, -70], dive, [-40, 0, -80], dive + 0.1, [40, 0, -10]),
        }, loop=False),
        "misc.dazed": anim(0.8, {
            "body": rot(0, [0, 0, -15], 0.4, [0, 0, 15], 0.8, [0, 0, -15]),
            "tail1": rot(0, [20, 0, 0], 0.4, [30, 0, 0], 0.8, [20, 0, 0]),
            "tail2": rot(0, [15, 0, 0], 0.8, [15, 0, 0]),
            "right_wisp": rot(0, [0, 0, -10], 0.8, [0, 0, -10]),
            "left_wisp": rot(0, [0, 0, 10], 0.8, [0, 0, 10]),
        }),
    }
    write_model("crying_soul", bones, animations, [1, 1])


MODELS = [("fallen_sister", sister), ("crying_soul", crying_soul)]


if __name__ == "__main__":
    wanted = set(sys.argv[1:])
    for name, build in MODELS:
        if not wanted or name in wanted:
            build()
