#!/usr/bin/env python3
"""Models, animations and textures of the overworld and moon-night entities: Ash Ghoul, Hollow, Ember Wisp, Ashen
Maiden, Ashen Zombie, Ghost, Vampire (PLAN 19.3, docs/art-direction.md §1.8 and §2: 30–80 cubes, bevels, layered
cloth and plate, hands with claws, hanging parts on their own bones, a breathing idle, a weighted walk, moves keyed as
anticipation → strike → recovery, and misc.hurt / misc.death / misc.stagger that the entity classes play), and the
Vampire boss (the bosses track's remodel, PLAN 19.4, kept as it was merged).

Run from the repository root: python3 tools/overworld_mob_art.py [name ...]
Sizes stay within tools/model_bounds.json, the bone names the code uses stay (the head the renderers turn, the
Hollow's right_item its sword is drawn at), and so do the animation names and the telegraph lengths
(EntityModelFilesTest). Check with
    python3 tools/model_preview.py check <name> && python3 tools/model_preview.py entity <name>
    python3 tools/model_preview.py motion <name>

Look (tools/overworld_kit.py holds the ramps): ash and embers for the creatures of the Ember (ghoul, wisp, zombie,
maiden), cold iron, rust and a faded ember-red for the Sanctum's fallen guard (Hollow), moonlit grey-blue for the
Ghost, black, crimson and bone-white for the Vampires of the blood moon. The Ember Wisp and the Ghost are drawn
translucent (their renderers use RenderLayer.getEntityTranslucent): their textures carry partial alpha, and so do
their glowmasks (GeckoLib's glow layer blends with alpha, full bright).
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from art_kit import (ZERO, Bone, Box, Material, anim, chain_bones, chain_names, chamfered, e, fingers, hexc,  # noqa: E402
                     keys, lag_sway, merge, mirror_box, octagon, pos, ramp, rot, scatter, shade, sutures, sway, whip,
                     write_model)
from overworld_kit import (CLOTH_BLUE, COLD, COLD_DIM, CORE, EMBER, EMBER_DARK, EMBER_DEEP, EMBER_HOT, FLAME,  # noqa: E402
                           GHOST_DARK, GHOST_EYE, GHOST_PALE, GHOUL_SKIN, HOLLOW_IRON, IVORY, LINEN, MAIDEN_ROBE,
                           MAIDEN_SKIN, STRAP, TABARD, VAMP_SKIN, V_BLACK, V_BURGUNDY, V_CRIMSON, V_HAIR, V_LACE,
                           V_SILVER, ZOMBIE_SKIN, GOLD_TRIM, BRASS, DEAD_WOOD, breathe, cloth, compose, drift, ember_seams,
                           fade, flame_tongues, flinch, glow_all, hanging, mail, on, plate_face, round_corners,
                           rust_spots, settle, skin_cracks, tatters, walk)

# --- Kept for vampire_boss() (the bosses track's function, unchanged): the Ashen Reach kit's banded ramps and face ---
# helpers it uses, copied from tools/reach_mob_art.py with the same values so the boss's texture stays byte-identical
# when the script is rerun (the mobs track rewrites that module, which this one therefore no longer imports); the
# seven overworld mobs share clear_all, holes, ragged_edge, trim and open_front with it.

BONE = ramp("5e5544", "7c725c", "9a8f75", "b5aa8e", "ccc2a6", "e0d8c0")
CHAR = ramp("1a1412", "271e1a", "362a23", "47372d", "5b483b", "74604f")
LEATHER = ramp("2a1c14", "3b281c", "4f3726", "654832", "7c5a40", "93704f")
VOID = hexc("0c0a0a")

BOSS_SKIN = ramp("4a4049", "5c515b", "6f636d", "82767f", "958a92", "a89ea5")
NOBLE_BLACK = ramp("0f0d10", "171419", "201c22", "29242c", "332d37", "3f3843")
VCRIMSON = ramp("2b070b", "420c12", "5c121a", "771823", "92202c", "ad2a36")
BURGUNDY = ramp("1e0a10", "2b0f17", "39141f", "481a28", "572132")
HAIR_BLACK = ramp("0c0b0e", "141218", "1d1a22", "27232d")
SILVER = ramp("3d3f46", "5a5d66", "7a7e88", "9a9fa9", "bcc0c8", "dde0e6")
MEMBRANE = ramp("1b070b", "28090f", "360d14", "45121a", "561822")
BLOOD = hexc("ff2a2a")
BLOOD_DIM = hexc("b0141e")
FANG = hexc("f2eee6")


def clear_all(face, rng=None):
    for y in range(face.h):
        for x in range(face.w):
            face.clear(x, y)


def holes(face, rng, count, size=1):
    """Burn holes: transparent pixels with a scorched rim (the cutout shows what is under the cloth)."""
    for _ in range(count):
        x, y = rng.randrange(face.w), rng.randrange(face.h)
        for dx in range(size):
            for dy in range(size):
                face.clear(x + dx, y + dy)
        for dx, dy in ((-1, 0), (size, 0), (0, -1), (0, size)):
            if face.get(x + dx, y + dy)[3]:
                face.set(x + dx, y + dy, CHAR[1])


def open_front(face, inset=1, top_rows=1):
    """A hood's front: cleared except a rim, so the face inside shows."""
    for y in range(top_rows, face.h):
        for x in range(inset, face.w - inset):
            face.clear(x, y)


def ragged_edge(face, rng, side, depth=2, chance=0.6, every=1):
    """Clears notches along one edge of a face: 'bottom', 'top', 'left', 'right'."""
    span = face.w if side in ("top", "bottom") else face.h
    for i in range(0, span, every):
        if rng.random() < chance:
            for k in range(rng.randint(1, depth)):
                if side == "bottom":
                    face.clear(i, face.h - 1 - k)
                elif side == "top":
                    face.clear(i, k)
                elif side == "left":
                    face.clear(k, i)
                else:
                    face.clear(face.w - 1 - k, i)


def trim(face, color, sides=("top", "bottom", "left", "right")):
    """Outlines the face's edges (a rim on a shield, a hem)."""
    for x in range(face.w):
        if "top" in sides:
            face.set(x, 0, color)
        if "bottom" in sides:
            face.set(x, face.h - 1, color)
    for y in range(face.h):
        if "left" in sides:
            face.set(0, y, color)
        if "right" in sides:
            face.set(face.w - 1, y, color)


# --- Ash Ghoul ------------------------------------------------------------------------------------------------------
# Fast, weak pack hunter of the night (DESIGN §3.2): a gaunt, hunched corpse of dry ash, embers still alive in the
# splits of its skin, arms too long ending in bone claws, ribs and spine pushing through, a skull face with sunken
# sockets and a hanging jaw, rags of a shroud at the waist. 57 cubes on 20 bones: chamfered torso and skull, a face
# plate and a rib plate carrying the drawn features, forearms, hands with three claws and a thumb, shins, feet with
# toe claws, four tatters on two-bone chains. It bursts into an ash cloud on death (AshGhoulEntity), so misc.death
# is a collapse onto its face with the ash still rising.

def ash_ghoul():
    skin = Material(GHOUL_SKIN, base=3.0, noise=0.55, cell=2.5, speck=0.04, grad=1.0, top=0.8, bottom=-1.0, edge=0.5,
                    bevel=0.35)
    dark_skin = skin.but(base=2.4)
    ivory = Material(IVORY, base=3.2, noise=0.45, grad=1.0, bevel=0.3)
    rag = Material(LINEN, base=2.4, noise=0.5, grain="v", grad=0.8)

    def hide(face, rng):
        if face.side():
            skin_cracks(face, rng, GHOUL_SKIN[0], max(1, face.w * face.h // 20))
            if face.w * face.h >= 12 and rng.random() < 0.6:
                ember_seams(face, rng, 1, length=(1, 3), dark=GHOUL_SKIN[0])

    def limb(face, rng):
        if face.side():
            skin_cracks(face, rng, GHOUL_SKIN[0], max(1, face.w * face.h // 28), (1, 3))

    def chest(face, rng):
        # Ribs through the skin, the sternum split and glowing under them.
        for y in (1, 3, 5):
            # Each rib curves down from the sternum to the side, its underside in shadow.
            face.hline(1, y, face.w - 2, IVORY[3])
            face.set(1, y + 1, IVORY[2])
            face.set(face.w - 2, y + 1, IVORY[2])
            face.hline(2, y + 1, face.w - 4, GHOUL_SKIN[0])
            face.set(0, y + 1, GHOUL_SKIN[1])
            face.set(face.w - 1, y + 1, GHOUL_SKIN[1])
        face.vline(face.cx, 0, face.h, GHOUL_SKIN[0])
        face.set(face.cx, 1, IVORY[1])
        face.set(face.cx, 3, IVORY[1])
        face.lit(face.cx, 6, EMBER, 0.9)
        face.lit(face.cx - 1, 7, EMBER_DEEP, 0.7)
        face.lit(face.cx, 7, EMBER_HOT, 1.0)
        face.set(face.cx + 1, 7, GHOUL_SKIN[0])

    def face_plate(face, rng):
        # Skull-tight face: a brow ridge, deep sockets with ember pupils, a slit nose between hollow cheeks, the upper
        # teeth over the dark of the mouth (the jaw hangs below on its own bone).
        face.hline(0, 0, face.w, GHOUL_SKIN[4])
        face.hline(0, 1, face.w, GHOUL_SKIN[2])
        for ex in (1, face.w - 3):
            face.rect(ex, 1, 2, 2, VOID)
        face.lit(2, 2, EMBER_HOT, 1.0)
        face.lit(face.w - 3, 2, EMBER_HOT, 1.0)
        face.lit(1, 2, EMBER_DEEP, 0.5)
        face.lit(face.w - 2, 2, EMBER_DEEP, 0.5)
        face.hline(0, 3, face.w, GHOUL_SKIN[3])
        face.set(0, 3, GHOUL_SKIN[1])
        face.set(face.w - 1, 3, GHOUL_SKIN[1])
        face.set(face.cx, 3, VOID)
        face.set(face.cx - 1, 3, GHOUL_SKIN[1])
        face.hline(0, 4, face.w, VOID)
        for x in range(1, face.w, 2):
            face.set(x, 4, IVORY[4])
        face.hline(0, 5, face.w, GHOUL_SKIN[1])

    def jaw_front(face, rng):
        face.hline(0, 0, face.w, VOID)
        for x in range(0, face.w, 2):
            face.set(x, 0, IVORY[5])

    def crust(face, rng):
        scatter(face, rng, max(1, face.w * face.h // 6), GHOUL_SKIN[1])
        scatter(face, rng, max(1, face.w * face.h // 10), GHOUL_SKIN[5])

    tatter_deco = compose(cloth(2, -18, LINEN[0]), tatters(2, 0.6), lambda f, r: holes(f, r, 1) if f.side() else None)

    # Animation signs (checked in the preview): -X raises an arm or swings a leg forward and tilts the head and body
    # back; +X bows the body forward, folds a leg back, opens the jaw (its box lies in front of its pivot).
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 11, 0], "root", [16, 0, 0],
             chamfered([-3.5, 11, -1.5], [7, 10, 3], skin, {"all": hide}, c=0.6)
             + [Box([-3.2, 12, -1.9], [6.4, 8.5, 0.6], skin.but(base=2.8), {"front": chest}),
                Box([-3.5, 19.8, -1.9], [7, 1, 0.8], ivory.but(base=2.6)),
                Box([-3.3, 16.5, 1.3], [2.2, 3, 0.7], ivory.but(base=2.2), {"back": lambda f, r: sutures(f, r, IVORY[0], 1)}),
                Box([1.1, 16.5, 1.3], [2.2, 3, 0.7], ivory.but(base=2.2), {"back": lambda f, r: sutures(f, r, IVORY[0], 1)})]
             + [Box([-0.6, y, 1.3], [1.2, 1.2, 1.2], ivory.but(base=3.6)) for y in (12.5, 14.8, 18.4)]),
        Bone("head", [0, 20.5, -2], "body", [-14, 0, 0],
             chamfered([-3.5, 20.5, -5], [7, 6.5, 7], skin.but(base=3.2), {"all": hide}, c=1.0)
             + [Box([-3.5, 20.5, -5.4], [7, 6, 0.6], skin.but(base=3.0), {"front": face_plate}),
                Box([-3.7, 24.8, -5.6], [7.4, 1.3, 1.2], ivory.but(base=2.4)),
                Box([-3.8, 22.3, -5.2], [1.3, 1.1, 1.4], ivory.but(base=2.2)),
                Box([2.5, 22.3, -5.2], [1.3, 1.1, 1.4], ivory.but(base=2.2)),
                Box([-2.2, 26.8, -4.4], [3.2, 0.6, 3.4], skin.but(base=1.6), {"top": crust}, rotation=[0, 18, 0]),
                Box([-0.8, 26.8, -2.2], [3.4, 0.6, 2.6], skin.but(base=1.8), {"top": crust}, rotation=[0, -25, 0])]),
        Bone("jaw", [0, 21.2, -1.2], "head", None, [
            Box([-2.5, 19.7, -4.9], [5, 1.4, 4.4], dark_skin, {"front": jaw_front, "top": lambda f, r: f.rect(0, 0, f.w, f.h, VOID)}),
            Box([-1.9, 21, -5.2], [0.7, 0.8, 0.7], ivory.but(base=4.0)),
            Box([1.2, 21, -5.2], [0.7, 0.8, 0.7], ivory.but(base=4.0)),
        ]),
    ]
    # Tatters of the shroud at the waist, front and back, each two segments.
    bones += hanging("rag_front", "body", [0, 11.5, -1.9], 2, 3.5, 6, rag, {"all": tatter_deco}, z_step=-0.2, taper=1.2, rotation=[-16, 0, 0])
    bones += hanging("rag_back", "body", [0, 11.5, 1.9], 2, 3.5, 6, rag.but(base=2.0), {"all": tatter_deco}, z_step=0.2, taper=1.2, rotation=[-16, 0, 0])
    # Arms: a shoulder knob, the upper arm, a bony elbow, the forearm, a palm with three claws and a thumb.
    upper = Box([-6.5, 13, -2.5], [3, 7, 3], skin, {"sides": limb})
    knob = Box([-7, 18.8, -2.8], [3.6, 2, 3.6], skin.but(base=3.4), {"sides": limb})
    fore = Box([-6.3, 6.5, -2.3], [2.6, 7.5, 2.6], skin.but(base=2.7), {"sides": limb})
    elbow = Box([-6.6, 12.3, -2.6], [3.2, 1.6, 3.2], ivory.but(base=2.0))
    palm = Box([-6.5, 4.5, -2.3], [3, 2.2, 2.4], skin.but(base=2.6))
    claws = fingers(-6.4, 4.5, -2.4, 3, (0.8, 3, 0.8), ivory.but(base=3.6), spread=0.3, curl=28, thumb=(1.1, -0.9))
    bones += [
        Bone("right_arm", [-4.5, 19.5, -1], "body", [-6, 0, 3], [upper, knob]),
        Bone("right_forearm", [-5, 13.5, -1], "right_arm", [-22, 0, 0], [fore, elbow]),
        Bone("right_hand", [-5, 6.5, -1], "right_forearm", [-8, 0, 0], [palm] + claws),
        Bone("left_arm", [4.5, 19.5, -1], "body", [-6, 0, -3], [mirror_box(upper), mirror_box(knob)]),
        Bone("left_forearm", [5, 13.5, -1], "left_arm", [-22, 0, 0], [mirror_box(fore), mirror_box(elbow)]),
        Bone("left_hand", [5, 6.5, -1], "left_forearm", [-8, 0, 0], [mirror_box(palm)] + [mirror_box(c) for c in claws]),
    ]
    # Legs: thigh, a kneecap on the shin, a foot with two toe claws.
    thigh = Box([-3.3, 5.5, -1.5], [3, 5.8, 3], skin.but(base=2.8), {"sides": limb})
    shin = Box([-3.2, 0.9, -1.4], [2.8, 4.8, 2.8], skin.but(base=2.5), {"sides": limb})
    knee = Box([-3.4, 4.6, -2.0], [3.2, 1.6, 1.0], ivory.but(base=2.2))
    foot = Box([-3.3, 0, -2.6], [3, 1, 4.2], dark_skin)
    toes = [Box([-3.2, 0, -3.6], [0.9, 0.9, 1.1], ivory.but(base=3.4)), Box([-1.7, 0, -3.6], [0.9, 0.9, 1.1], ivory.but(base=3.4))]
    bones += [
        Bone("right_leg", [-1.8, 11, 0], "root", [-8, 0, 0], [thigh]),
        Bone("right_shin", [-1.8, 5.5, 0], "right_leg", [14, 0, 0], [shin, knee]),
        Bone("right_foot", [-1.8, 1, 0], "right_shin", [-6, 0, 0], [foot] + toes),
        Bone("left_leg", [1.8, 11, 0], "root", [-8, 0, 0], [mirror_box(thigh)]),
        Bone("left_shin", [1.8, 5.5, 0], "left_leg", [14, 0, 0], [mirror_box(shin), mirror_box(knee)]),
        Bone("left_foot", [1.8, 1, 0], "left_shin", [-6, 0, 0], [mirror_box(foot)] + [mirror_box(t) for t in toes]),
    ]
    rags = [chain_names("rag_front", 2), chain_names("rag_back", 2)]

    def rags_drift(length, amp, phase=0.0):
        return merge(drift(rags[0], length, amp, phase=phase), drift(rags[1], length, amp * 0.8, phase=phase + 0.3))

    def rags_whip(t0, amp, settle_t=0.4):
        return merge(whip(rags[0], t0, amp, settle=settle_t), whip(rags[1], t0 + 0.03, -amp * 0.8, settle=settle_t))

    arms_rest = [[-6, 0, 3], [-6, 0, -3]]
    fall = {"body": [8, 0, 0], "head": [-10, 28, 12], "jaw": [16, 0, 0], "right_arm": [-75, 0, 35], "left_arm": [-60, 0, -40],
            "right_forearm": [-20, 0, 0], "left_forearm": [-30, 0, 0], "right_hand": [-25, 0, 0], "left_hand": [-25, 0, 0],
            "right_leg": [14, 0, 8], "left_leg": [8, 0, -6], "right_shin": [-30, 0, 0], "left_shin": [-18, 0, 0],
            "right_foot": [20, 0, 0], "left_foot": [25, 0, 0]}
    animations = {
        # Hunched and twitchy: the chest heaves, the head snaps from side to side, the jaw works, the claws flex.
        "misc.idle": anim(2.0, merge(breathe(2.0, chest=2.5, head=0, shoulders=1.5), rags_drift(2.0, 4), {
            "head": rot(0, ZERO, 0.4, e("easeOutCubic", [4, -20, 4]), 0.9, [5, -20, 4], 1.0, e("easeOutCubic", [-3, 16, -3]),
                        1.55, [-2, 16, -3], 2.0, e("easeInOutSine", ZERO)),
            "jaw": rot(0, ZERO, 0.5, [8, 0, 0], 0.7, [2, 0, 0], 1.3, [10, 0, 0], 2.0, ZERO),
            "right_hand": rot(0, ZERO, 1.0, e("easeInOutSine", [-14, 0, 0]), 2.0, e("easeInOutSine", ZERO)),
            "left_hand": rot(0, [-10, 0, 0], 1.0, e("easeInOutSine", ZERO), 2.0, e("easeInOutSine", [-10, 0, 0])),
            "right_forearm": rot(0, ZERO, 1.0, [-6, 0, 0], 2.0, ZERO),
            "left_forearm": rot(0, [-4, 0, 0], 1.0, ZERO, 2.0, [-4, 0, 0]),
        })),
        # A fast lope (speed 0.33): long strides, the arms swinging wide with the forearms bent, the body pitching
        # with each plant, the head level and the rags trailing.
        "move.walk": anim(0.5, merge(walk(0.5, stride=40, swing=32, bob=0.9, lean=6, twist=7, roll=3, arm_rest=arms_rest),
                                     rags_drift(0.5, 10, 0.1), {
            "right_forearm": rot(0, [-30, 0, 0], 0.25, e("easeInOutSine", [-45, 0, 0]), 0.5, e("easeInOutSine", [-30, 0, 0])),
            "left_forearm": rot(0, [-45, 0, 0], 0.25, e("easeInOutSine", [-30, 0, 0]), 0.5, e("easeInOutSine", [-45, 0, 0])),
            "right_shin": rot(0, [10, 0, 0], 0.12, [45, 0, 0], 0.25, e("easeInOutSine", [5, 0, 0]), 0.5, e("easeInOutSine", [10, 0, 0])),
            "left_shin": rot(0, [5, 0, 0], 0.25, e("easeInOutSine", [10, 0, 0]), 0.37, [45, 0, 0], 0.5, e("easeInOutSine", [5, 0, 0])),
            "jaw": rot(0, [6, 0, 0], 0.25, [10, 0, 0], 0.5, [6, 0, 0]),
        })),
        # A rake with the right claws, then the left: both drawn up and back first, raked down and across with a snap,
        # the jaw open at the strike, the rags whipping after it.
        "attack.swing": anim(0.45, merge(rags_whip(0.2, 30, 0.22), {
            "right_arm": rot(0, arms_rest[0], 0.12, e("easeInOutSine", [-150, 0, 28]), 0.2, e("easeOutBack", [-35, 0, -12]), 0.45, e("easeInOutSine", arms_rest[0])),
            "right_forearm": rot(0, ZERO, 0.12, [-45, 0, 0], 0.2, e("easeOutCubic", [8, 0, 0]), 0.45, ZERO),
            "right_hand": rot(0, ZERO, 0.12, [-35, 0, 0], 0.2, [25, 0, 0], 0.45, ZERO),
            "left_arm": rot(0, arms_rest[1], 0.14, [-60, 0, -20], 0.24, e("easeInOutSine", [-145, 0, -28]), 0.32, e("easeOutBack", [-30, 0, 12]), 0.45, e("easeInOutSine", arms_rest[1])),
            "left_forearm": rot(0, ZERO, 0.24, [-45, 0, 0], 0.32, e("easeOutCubic", [8, 0, 0]), 0.45, ZERO),
            "left_hand": rot(0, ZERO, 0.24, [-35, 0, 0], 0.32, [25, 0, 0], 0.45, ZERO),
            "body": rot(0, ZERO, 0.12, e("easeInOutSine", [-8, 16, 0]), 0.2, e("easeOutBack", [12, -12, 0]), 0.32, [10, 10, 0], 0.45, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, 0.12, [-16, 0, 0], 0.22, [10, 0, 0], 0.45, ZERO),
            "jaw": rot(0, ZERO, 0.1, [20, 0, 0], 0.3, [6, 0, 0], 0.45, ZERO),
            "root": pos(0, ZERO, 0.12, [0, 0.5, 0.8], 0.2, e("easeOutCubic", [0, -0.5, -1.5]), 0.45, ZERO),
        }), loop=False),
        # A hit knocks it back: the body and head jerk, the arms come up, the jaw snaps; the rags fly.
        "misc.hurt": anim(0.4, merge(flinch(0.4, amount=1.1), rags_whip(0.04, 18, 0.3), {
            "jaw": rot(0, ZERO, 0.06, [22, 0, 0], 0.4, ZERO),
        }), loop=False),
        # It staggers back, its knees buckle, and it goes down on its face with the arms thrown out; the rags settle.
        "misc.death": anim(1.8, merge(rags_whip(0.95, 25, 0.6), {
            "root": {"position": keys(0, ZERO, 0.25, e("easeOutCubic", [0, 0, 1.5]), 0.55, e("easeInOutSine", [0, -4.5, 1]),
                                      0.95, e("easeOutBounce", [0, -1, 0]), 1.8, [0, -1, 0]),
                     "rotation": keys(0, ZERO, 0.5, ZERO, 0.95, e("easeInQuad", [82, 0, 6]), 1.1, e("easeOutCubic", [84, 0, 6]), 1.8, [84, 0, 6])},
            "body": rot(0, ZERO, 0.25, e("easeOutCubic", [-16, 0, 0]), 0.55, e("easeInOutSine", [18, 0, 0]), 0.95, [10, 0, 0], 1.1, fall["body"], 1.8, fall["body"]),
            "head": rot(0, ZERO, 0.22, e("easeOutCubic", [-22, 0, 0]), 0.55, [-8, 0, 0], 0.95, [-4, 0, 0], 1.15, e("easeOutCubic", fall["head"]), 1.8, fall["head"]),
            "jaw": rot(0, ZERO, 0.22, [24, 0, 0], 0.6, [10, 0, 0], 1.1, fall["jaw"], 1.8, fall["jaw"]),
            "right_arm": rot(0, arms_rest[0], 0.25, e("easeOutCubic", [-45, 0, 25]), 0.55, [-20, 0, 15], 0.95, [-40, 0, 20], 1.1, e("easeOutCubic", fall["right_arm"]), 1.8, fall["right_arm"]),
            "left_arm": rot(0, arms_rest[1], 0.25, e("easeOutCubic", [-40, 0, -25]), 0.55, [-15, 0, -15], 0.95, [-35, 0, -20], 1.1, e("easeOutCubic", fall["left_arm"]), 1.8, fall["left_arm"]),
            "right_forearm": rot(0, ZERO, 0.25, [-30, 0, 0], 0.95, [-10, 0, 0], 1.1, fall["right_forearm"], 1.8, fall["right_forearm"]),
            "left_forearm": rot(0, ZERO, 0.25, [-30, 0, 0], 0.95, [-10, 0, 0], 1.1, fall["left_forearm"], 1.8, fall["left_forearm"]),
            "right_hand": rot(0, ZERO, 0.25, [-30, 0, 0], 1.1, fall["right_hand"], 1.8, fall["right_hand"]),
            "left_hand": rot(0, ZERO, 0.25, [-30, 0, 0], 1.1, fall["left_hand"], 1.8, fall["left_hand"]),
            "right_leg": rot(0, ZERO, 0.3, ZERO, 0.55, e("easeInOutSine", [35, 0, 0]), 1.0, fall["right_leg"], 1.8, fall["right_leg"]),
            "left_leg": rot(0, ZERO, 0.3, ZERO, 0.55, e("easeInOutSine", [28, 0, 0]), 1.0, fall["left_leg"], 1.8, fall["left_leg"]),
            "right_shin": rot(0, ZERO, 0.3, ZERO, 0.55, e("easeInOutSine", [-60, 0, 0]), 1.0, fall["right_shin"], 1.8, fall["right_shin"]),
            "left_shin": rot(0, ZERO, 0.3, ZERO, 0.55, e("easeInOutSine", [-50, 0, 0]), 1.0, fall["left_shin"], 1.8, fall["left_shin"]),
            "right_foot": rot(0, ZERO, 0.55, [20, 0, 0], 1.8, fall["right_foot"]),
            "left_foot": rot(0, ZERO, 0.55, [25, 0, 0], 1.8, fall["left_foot"]),
        }), loop=False),
    }
    write_model("ash_ghoul", bones, animations, [2, 2.5])


# --- Hollow ---------------------------------------------------------------------------------------------------------
# An undead soldier of the order that kept the Sanctums (DESIGN §3.2): a rusted mail hauberk under a riveted
# breastplate, a torn tabard of faded ember-red with the Sanctum's flame hanging from the belt in two pieces, a mail
# skirt, a dented sallet with a slit visor over a mail coif, a dead grey face with cold pin-prick eyes, layered
# pauldrons, bracers, greaves and a round buckler on the left forearm. 66 cubes on 18 bones. Its sword is the held
# item, drawn at right_item (MobRenderers tunes its angle to this arm): the right_arm pivot, the right_item pivot and
# the arm's reach stay as before; right_item now hangs from the new right_forearm, so the blade follows the elbow.
# attack.swing keeps its tell: raised by 0.3 s, held to the hit at 0.4 s (HollowEntity.WINDUP_TICKS = 8), the blade
# down by 0.47 s. attack.block is the guard stance; misc.stagger (3 s = STAGGER_TICKS) the broken guard's reel,
# held on the stance controller; misc.hurt the flinch between swings; misc.death a drop to the knees, then over.

def hollow():
    iron = Material(HOLLOW_IRON, base=3.0, noise=0.45, cell=2.5, speck=0.03, grad=1.0, top=0.9, bottom=-0.8, edge=0.5, bevel=0.5)
    mail_mat = Material(HOLLOW_IRON, base=2.4, noise=0.4, grad=0.8, edge=0.4)
    cloth_mat = Material(TABARD, base=3.0, noise=0.45, grain="v", grad=0.9, edge=0.4)
    leather = Material(STRAP, base=3.0, noise=0.45, grain="h", bevel=0.4)
    skin = Material(MAIDEN_SKIN, base=1.6, noise=0.4, grad=0.6, top=-0.3)
    wood = Material(DEAD_WOOD, base=2.6, noise=0.5, grain="h", bevel=0.3)

    def plate(face, rng):
        plate_face(face, rng, HOLLOW_IRON, rivet_step=3)
        rust_spots(face, rng, max(1, face.w * face.h // 40))

    def small_plate(face, rng):
        plate_face(face, rng, HOLLOW_IRON, rivet_step=2, dent=0.0)
        rust_spots(face, rng, 1)

    def hauberk(face, rng):
        mail(face, rng, HOLLOW_IRON[1], HOLLOW_IRON[5], HOLLOW_IRON[6])
        rust_spots(face, rng, max(1, face.w * face.h // 48))

    def breastplate(face, rng):
        plate(face, rng)
        if face.name == "front":
            # Two lames meeting at a ridge down the middle, a dent on the left of the chest.
            face.vline(face.cx, 0, face.h, HOLLOW_IRON[6])
            face.vline(face.cx - 1, 0, face.h, HOLLOW_IRON[1])
            face.rect(1, 3, 2, 2, HOLLOW_IRON[1])
            face.set(2, 3, HOLLOW_IRON[0])
            face.set(1, 2, HOLLOW_IRON[5])

    def face_front(face, rng):
        # Grey dead skin pulled over the skull: dark sockets with cold points, sunken cheeks, a lipless mouth.
        face.rect(0, 0, face.w, face.h, MAIDEN_SKIN[1])
        face.hline(1, 1, face.w - 2, MAIDEN_SKIN[2])
        face.rect(1, 2, 2, 2, VOID)
        face.rect(face.w - 3, 2, 2, 2, VOID)
        face.lit(2, 3, COLD, 1.0)
        face.lit(face.w - 3, 3, COLD, 1.0)
        face.set(face.cx - 1, 4, MAIDEN_SKIN[0])     # the nose rotted away
        face.set(face.cx, 4, VOID)
        face.vline(0, 3, 3, MAIDEN_SKIN[0])
        face.vline(face.w - 1, 3, 3, MAIDEN_SKIN[0])
        face.hline(2, 6, face.w - 4, VOID)
        for x in range(2, face.w - 2, 2):
            face.set(x, 6, IVORY[4])
        face.hline(2, 7, face.w - 4, MAIDEN_SKIN[0])

    def sallet(face, rng):
        plate(face, rng)
        if face.name == "top":
            face.vline(face.cx, 0, face.h, HOLLOW_IRON[6])      # the crest ridge
            face.vline(face.cx - 1, 0, face.h, HOLLOW_IRON[1])
        if face.name in ("left", "right"):
            # The dent: a dark hollow with a lit rim on its upper left.
            face.rect(face.w - 4, 1, 3, 2, HOLLOW_IRON[1])
            face.set(face.w - 3, 2, HOLLOW_IRON[0])
            face.set(face.w - 4, 1, HOLLOW_IRON[6])

    def visor(face, rng):
        small_plate(face, rng)
        if face.name == "front":
            # The eye slit, and a bent-in corner.
            face.hline(1, 1, face.w - 2, VOID)
            face.set(face.w - 2, 0, HOLLOW_IRON[1])
            face.set(face.w - 2, 1, HOLLOW_IRON[0])

    def coif(face, rng):
        mail(face, rng, HOLLOW_IRON[1], HOLLOW_IRON[5])
        if face.name == "front":
            open_front(face, 1, 0)
        if face.name in ("top", "bottom"):
            clear_all(face)

    def tabard(face, rng):
        if face.name not in ("front", "back"):
            clear_all(face)
            return
        cloth(2, -18, TABARD[0])(face, rng)
        trim(face, BRASS[1], ("left", "right"))
        ragged_edge(face, rng, "bottom", 2, 0.5)
        holes(face, rng, 1)

    def flame_emblem(face, rng):
        tabard(face, rng)
        if face.name == "front":
            cx = face.cx
            # The Sanctum's flame, faded: an ember drop over a small brazier, in brass thread.
            face.set(cx, 0, BRASS[5])
            face.rect(cx - 1, 1, 2, 2, BRASS[5])
            face.set(cx - 1, 0, BRASS[3])
            face.set(cx + 1, 1, BRASS[3])
            face.hline(cx - 2, 3, 4, BRASS[2])
            face.hline(cx - 1, 4, 2, BRASS[1])

    def belt(face, rng):
        if face.side():
            face.hline(0, 0, face.w, STRAP[1])
            face.hline(0, face.h - 1, face.w, STRAP[0])
            for x in range(1, face.w, 3):
                face.set(x, face.h // 2, STRAP[5])        # stitching

    def skirt(face, rng):
        mail(face, rng, HOLLOW_IRON[1], HOLLOW_IRON[5])
        if face.side():
            ragged_edge(face, rng, "bottom", 1, 0.5)
        if face.name in ("top", "bottom"):
            clear_all(face)

    def buckler(face, rng):
        if face.name in ("left", "right"):
            plate_face(face, rng, HOLLOW_IRON, rivet_step=2, dent=0.0)
            for (x, y) in ((0, 0), (face.w - 1, 0), (0, face.h - 1), (face.w - 1, face.h - 1)):
                face.clear(x, y)
            rust_spots(face, rng, 2)
            face.rect(face.cx - 1, face.h // 2 - 1, 2, 2, BRASS[4])
            face.set(face.cx - 1, face.h // 2 - 1, BRASS[6])
        else:
            for y in range(face.h):
                for x in range(face.w):
                    face.set(x, y, HOLLOW_IRON[2])

    def gauntlet(face, rng):
        small_plate(face, rng)
        if face.name == "front":
            for x in range(0, face.w, 2):
                face.set(x, face.h - 1, HOLLOW_IRON[1])       # the knuckles

    # Animation signs (checked in the preview): -X raises an arm or swings a leg forward and tilts the head and body
    # back; +X bows the body forward, folds a leg back.
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 24, 0], "root", None,
             chamfered([-4, 12, -2], [8, 12, 4], mail_mat, {"all": hauberk}, c=0.6)
             + [Box([-3.8, 16, -2.7], [7.6, 7.5, 0.9], iron, {"all": breastplate}),
                Box([-3.8, 16, 1.8], [7.6, 7.5, 0.9], iron.but(base=2.6), {"all": plate}),
                Box([-4.3, 23, -2.3], [8.6, 1.6, 4.6], iron.but(base=2.8), {"all": small_plate}),       # gorget
                Box([-4.4, 11.5, -2.4], [8.8, 2, 4.8], leather, {"all": belt}),
                Box([-1, 11.6, -2.7], [2, 1.8, 0.5], Material(BRASS, base=4.0, noise=0.4, bevel=0.6)),   # buckle
                Box([1.6, 10, -2.6], [2.2, 3.2, 1.2], leather.but(base=2.2), {"all": small_plate}),      # a pouch
                Box([-4.4, 8, -2.4], [8.8, 4, 4.8], mail_mat.but(base=2.2), {"all": skirt})]),
    ]
    bones += hanging("tabard_front", "body", [0, 12, -2.8], 2, 4.8, 6, cloth_mat, {"all": flame_emblem}, z_step=-0.2, taper=0.6)
    bones += hanging("tabard_back", "body", [0, 12, 2.8], 2, 4.8, 6, cloth_mat.but(base=2.6), {"all": tabard}, z_step=0.2, taper=0.6)
    bones += [
        Bone("head", [0, 24, 0], "root", None,
             chamfered([-4, 24, -4], [8, 8, 8], skin, None, c=0.8)
             + [Box([-4, 24, -4.4], [8, 8, 0.6], skin, {"front": face_front}),
                Box([-4.4, 23.5, -4.4], [8.8, 7.2, 8.8], mail_mat, {"all": coif}),
                Box([-4.7, 28, -4.7], [9.4, 4.2, 9.4], iron, {"all": sallet}),
                Box([-3.6, 28.3, -3.6], [7.2, 4.4, 7.2], iron.but(base=3.2), {"all": plate}, rotation=[0, 45, 0]),
                Box([-4.9, 26.8, -5.5], [9.8, 2.6, 1.2], iron.but(base=3.2), {"all": visor}, rotation=[-8, 0, 0], pivot=[0, 28, -5]),
                Box([-4.5, 26.6, 3.4], [9, 1.6, 2.2], iron.but(base=2.6), {"all": small_plate}, rotation=[-28, 0, 0], pivot=[0, 28, 3.5]),
                Box([-0.6, 32.0, -3.5], [1.2, 1.0, 7], iron.but(base=3.6))]),                                   # crest
    ]
    # Arms: a mail sleeve under two pauldron lames, a couter at the elbow, a bracer and a gauntlet on the forearm.
    upper = Box([-8, 16, -2], [4, 6.5, 4], mail_mat, {"all": hauberk})
    pauldron = Box([-8.7, 20, -2.7], [5.4, 2.6, 5.4], iron.but(base=3.2), {"all": plate})
    lame = Box([-8.5, 18.2, -2.5], [5, 1.8, 5], iron.but(base=2.8), {"all": small_plate})
    couter = Box([-8.5, 15, -2.5], [5, 1.6, 5], iron.but(base=2.8), {"all": small_plate})
    fore = Box([-7.9, 11.5, -1.9], [3.8, 5.5, 3.8], mail_mat.but(base=2.2), {"all": hauberk})
    bracer = Box([-8.2, 13, -2.2], [4.4, 3.4, 4.4], iron.but(base=2.9), {"all": small_plate})
    gauntlet_box = Box([-8.1, 10.2, -2.1], [4.2, 2.6, 4.2], iron.but(base=2.6), {"all": gauntlet})
    bones += [
        Bone("right_arm", [-5, 22, 0], "root", [-6, 0, 0], [upper, pauldron, lame, couter]),
        Bone("right_forearm", [-6, 16.5, 0], "right_arm", [-10, 0, 0], [fore, bracer, gauntlet_box]),
        Bone("right_item", [-6, 13, -1], "right_forearm"),
        Bone("left_arm", [5, 22, 0], "root", [-6, 0, -6], [mirror_box(upper), mirror_box(pauldron), mirror_box(lame), mirror_box(couter)]),
        Bone("left_forearm", [6, 16.5, 0], "left_arm", [-14, 0, 0], [
            mirror_box(fore), mirror_box(bracer), mirror_box(gauntlet_box),
            Box([8.3, 9.5, -3.4], [1, 8, 8], wood, {"all": buckler}),
            Box([8.5, 10.6, -2.3], [0.8, 5.8, 5.8], wood.but(base=2.2), {"all": buckler}, rotation=[45, 0, 0]),
            Box([9.2, 12.5, -0.4], [0.8, 2, 2], Material(BRASS, base=4.2, noise=0.4, bevel=0.6))]),
    ]
    thigh = Box([-3.9, 6, -2], [4, 6.5, 4], mail_mat, {"all": hauberk})
    cuisse = Box([-4.2, 7, -2.6], [4.4, 5, 1.2], iron.but(base=2.8), {"all": small_plate})
    knee = Box([-4.1, 5, -2.7], [4.2, 2.2, 1.6], iron.but(base=3.0), {"all": small_plate})
    shin = Box([-3.8, 1.2, -1.9], [3.8, 5.2, 3.8], mail_mat.but(base=2.2), {"all": hauberk})
    greave = Box([-4.1, 1.6, -2.5], [4.2, 4.4, 1.2], iron.but(base=2.8), {"all": small_plate})
    boot = Box([-3.9, 0, -3.2], [4, 1.6, 5.2], leather.but(base=1.8), {"all": lambda f, r: trim(f, STRAP[0], ("bottom",)) if f.side() else None})
    bones += [
        Bone("right_leg", [-1.9, 12, 0], "root", None, [thigh, cuisse]),
        Bone("right_shin", [-1.9, 6, 0], "right_leg", None, [shin, knee, greave, boot]),
        Bone("left_leg", [1.9, 12, 0], "root", None, [mirror_box(thigh), mirror_box(cuisse)]),
        Bone("left_shin", [1.9, 6, 0], "left_leg", None, [mirror_box(shin), mirror_box(knee), mirror_box(greave), mirror_box(boot)]),
    ]
    tabards = [chain_names("tabard_front", 2), chain_names("tabard_back", 2)]

    def tabard_drift(length, amp, phase=0.0):
        return merge(drift(tabards[0], length, amp, phase=phase), drift(tabards[1], length, -amp * 0.8, phase=phase + 0.25))

    def tabard_whip(t0, amp, settle_t=0.45):
        return merge(whip(tabards[0], t0, amp, settle=settle_t), whip(tabards[1], t0 + 0.03, -amp * 0.8, settle=settle_t))

    arms_rest = [[-6, 0, 0], [-6, 0, -6]]
    block = {"right_arm": [-70, -45, 0], "right_forearm": [-30, 0, 0], "left_arm": [-50, 40, -10], "left_forearm": [-35, 0, 0],
             "body": [4, 12, 0], "head": [8, -4, 0]}
    kneel = {"body": [14, 0, 0], "head": [22, 0, 0], "right_arm": [-4, 0, 6], "right_forearm": [2, 0, 0], "left_arm": [-6, 0, -16],
             "left_forearm": [-10, 0, 0], "right_leg": [2, 0, 0], "left_leg": [2, 0, 0], "right_shin": [88, 0, 0], "left_shin": [88, 0, 0]}
    fallen = {"body": [10, 0, 0], "head": [14, 20, 0], "right_arm": [-30, 0, 30], "right_forearm": [-15, 0, 0], "left_arm": [-20, 0, -30],
              "left_forearm": [-40, 0, 0], "right_leg": [-10, 0, 0], "left_leg": [12, 0, 0], "right_shin": [60, 0, 0], "left_shin": [75, 0, 0]}
    animations = {
        # Heavy and slow: a deep breath, the sword arm settling, a slow look to the side.
        "misc.idle": anim(3.0, merge(breathe(3.0, chest=1.5, head=1.0, shoulders=1.0), tabard_drift(3.0, 3), {
            "head": rot(0, [3, 0, 0], 1.2, e("easeInOutSine", [5, 9, 0]), 2.2, e("easeInOutSine", [4, -5, 0]), 3.0, e("easeInOutSine", [3, 0, 0])),
            "right_forearm": rot(0, ZERO, 1.5, e("easeInOutSine", [-4, 0, 0]), 3.0, e("easeInOutSine", ZERO)),
            "left_forearm": rot(0, ZERO, 1.5, e("easeInOutSine", [-3, 0, 2]), 3.0, e("easeInOutSine", ZERO)),
        })),
        # A plodding march (speed 0.2): short strides, the shield held in, the body rolling with each step.
        "move.walk": anim(1.2, merge(walk(1.2, stride=26, swing=12, bob=0.5, lean=3, twist=4, roll=3, arm_rest=arms_rest),
                                     tabard_drift(1.2, 7, 0.1), {
            "right_shin": rot(0, [6, 0, 0], 0.3, [30, 0, 0], 0.6, e("easeInOutSine", [2, 0, 0]), 1.2, e("easeInOutSine", [6, 0, 0])),
            "left_shin": rot(0, [2, 0, 0], 0.6, e("easeInOutSine", [6, 0, 0]), 0.9, [30, 0, 0], 1.2, e("easeInOutSine", [2, 0, 0])),
            "left_forearm": rot(0, [-20, 0, 0], 0.6, [-24, 0, 0], 1.2, [-20, 0, 0]),
        })),
        # The sword drawn up over the shoulder and held there, the body wound back; then the cut down and across
        # with the weight behind it (the hit lands at 0.4 s), and a slow recovery to guard.
        "attack.swing": anim(0.9, merge(tabard_whip(0.42, 28), {
            "right_arm": rot(0, arms_rest[0], 0.3, e("easeInOutSine", [-165, 0, 12]), 0.4, [-172, 0, 14], 0.47, e("easeOutBack", [-38, 0, -8]),
                             0.9, e("easeInOutSine", arms_rest[0])),
            "right_forearm": rot(0, ZERO, 0.3, e("easeInOutSine", [-30, 0, 0]), 0.4, [-35, 0, 0], 0.47, e("easeOutCubic", [-5, 0, 0]), 0.9, e("easeInOutSine", ZERO)),
            "left_arm": rot(0, arms_rest[1], 0.3, e("easeInOutSine", [-30, 0, -20]), 0.4, [-32, 0, -22], 0.47, e("easeOutCubic", [12, 0, -8]), 0.9, e("easeInOutSine", arms_rest[1])),
            "body": rot(0, ZERO, 0.3, e("easeInOutSine", [-6, 20, 0]), 0.4, [-7, 22, 0], 0.47, e("easeOutBack", [14, -24, 0]), 0.9, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, 0.3, e("easeInOutSine", [-10, -8, 0]), 0.4, [-10, -8, 0], 0.47, e("easeOutCubic", [12, 6, 0]), 0.9, e("easeInOutSine", ZERO)),
            "right_leg": rot(0, ZERO, 0.3, e("easeInOutSine", [12, 0, 0]), 0.47, e("easeOutCubic", [-14, 0, 0]), 0.9, e("easeInOutSine", ZERO)),
            "left_leg": rot(0, ZERO, 0.3, e("easeInOutSine", [-10, 0, 0]), 0.47, e("easeOutCubic", [10, 0, 0]), 0.9, e("easeInOutSine", ZERO)),
            "root": pos(0, ZERO, 0.3, e("easeInOutSine", [0, 0, 1]), 0.47, e("easeOutCubic", [0, -0.6, -2]), 0.9, e("easeInOutSine", ZERO)),
        }), loop=False),
        # The guard: the buckler up in front, the sword held across; it breathes behind the shield.
        "attack.block": anim(2.0, merge(tabard_drift(2.0, 2), {
            "right_arm": rot(0, block["right_arm"], 1, e("easeInOutSine", [-72, -45, 0]), 2, e("easeInOutSine", block["right_arm"])),
            "right_forearm": rot(0, block["right_forearm"], 2, block["right_forearm"]),
            "left_arm": rot(0, block["left_arm"], 1, e("easeInOutSine", [-53, 40, -12]), 2, e("easeInOutSine", block["left_arm"])),
            "left_forearm": rot(0, block["left_forearm"], 2, block["left_forearm"]),
            "body": rot(0, block["body"], 1, e("easeInOutSine", [5, 12, 0]), 2, e("easeInOutSine", block["body"])),
            "head": rot(0, block["head"], 2, block["head"]),
            "right_leg": rot(0, [-8, 0, 0], 2, [-8, 0, 0]),
            "left_leg": rot(0, [8, 0, 0], 2, [8, 0, 0]),
        })),
        # A hit between swings rocks it on its heels; the plate takes most of it.
        "misc.hurt": anim(0.45, merge(flinch(0.45, amount=0.8, arms=()), tabard_whip(0.04, 14, 0.35), {
            "right_arm": rot(0, ZERO, 0.08, e("easeOutCubic", [-18, 0, 10]), 0.45, e("easeInOutSine", ZERO)),
            "left_arm": rot(0, ZERO, 0.08, e("easeOutCubic", [-30, 0, -20]), 0.45, e("easeInOutSine", ZERO)),
        }), loop=False),
        # The guard broken (an axe hit, HollowEntity.STAGGER_TICKS = 60): the shield arm flung wide, it reels back
        # and sways on buckling knees for three seconds, the head lolling, and straightens as the stagger ends.
        "misc.stagger": anim(3.0, merge(tabard_whip(0.05, 30, 0.6), {
            "root": {"position": keys(0, ZERO, 0.15, e("easeOutCubic", [0, 0, 2.5]), 0.6, e("easeInOutSine", [0, -2, 1.5]), 1.5, e("easeInOutSine", [0, -1, 1]),
                                      2.4, e("easeInOutSine", [0, -2, 0.5]), 3.0, e("easeInOutSine", ZERO))},
            "body": rot(0, ZERO, 0.15, e("easeOutCubic", [-26, 8, 10]), 0.6, e("easeInOutSine", [-12, -14, -8]), 1.2, e("easeInOutSine", [-4, 10, 7]),
                        1.8, e("easeInOutSine", [-8, -8, -6]), 2.4, e("easeInOutSine", [2, 4, 3]), 3.0, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, 0.12, e("easeOutCubic", [-32, 10, 0]), 0.6, e("easeInOutSine", [18, -16, 8]), 1.2, e("easeInOutSine", [22, 14, -8]),
                        1.8, e("easeInOutSine", [20, -12, 6]), 2.4, e("easeInOutSine", [10, 6, -3]), 3.0, e("easeInOutSine", ZERO)),
            "right_arm": rot(0, ZERO, 0.15, e("easeOutCubic", [-60, 0, 40]), 0.6, e("easeInOutSine", [-10, 0, 25]), 1.5, e("easeInOutSine", [-4, 0, 18]),
                             2.4, e("easeInOutSine", [-2, 0, 10]), 3.0, e("easeInOutSine", ZERO)),
            "right_forearm": rot(0, ZERO, 0.15, [-40, 0, 0], 0.6, [-10, 0, 0], 3.0, e("easeInOutSine", ZERO)),
            "left_arm": rot(0, ZERO, 0.15, e("easeOutCubic", [-40, 0, -80]), 0.6, e("easeInOutSine", [-10, 0, -45]), 1.5, e("easeInOutSine", [-6, 0, -30]),
                            2.4, e("easeInOutSine", [-4, 0, -15]), 3.0, e("easeInOutSine", ZERO)),
            "left_forearm": rot(0, ZERO, 0.15, [-50, 0, 0], 0.6, [-20, 0, 0], 3.0, e("easeInOutSine", ZERO)),
            "right_leg": rot(0, ZERO, 0.15, e("easeOutCubic", [-22, 0, 0]), 0.6, e("easeInOutSine", [-8, 0, 0]), 1.5, e("easeInOutSine", [-14, 0, 0]),
                             2.4, e("easeInOutSine", [-6, 0, 0]), 3.0, e("easeInOutSine", ZERO)),
            "left_leg": rot(0, ZERO, 0.15, e("easeOutCubic", [14, 0, 0]), 0.6, e("easeInOutSine", [4, 0, 0]), 1.5, e("easeInOutSine", [8, 0, 0]),
                            2.4, e("easeInOutSine", [3, 0, 0]), 3.0, e("easeInOutSine", ZERO)),
            "right_shin": rot(0, ZERO, 0.15, [12, 0, 0], 0.6, e("easeInOutSine", [30, 0, 0]), 1.5, e("easeInOutSine", [18, 0, 0]), 2.4, e("easeInOutSine", [26, 0, 0]), 3.0, e("easeInOutSine", ZERO)),
            "left_shin": rot(0, ZERO, 0.6, e("easeInOutSine", [16, 0, 0]), 1.5, e("easeInOutSine", [8, 0, 0]), 2.4, e("easeInOutSine", [14, 0, 0]), 3.0, e("easeInOutSine", ZERO)),
        }), loop=False),
        # Struck down: it rocks back, drops to its knees (the shins fold under it), sways there with the head sinking,
        # then topples onto its right side; the tabard settles over it.
        "misc.death": anim(2.4, merge(tabard_whip(1.75, 22, 0.55), {
            "root": {"position": keys(0, ZERO, 0.3, e("easeOutCubic", [0, 0, 1.5]), 0.85, e("easeInQuad", [0, -6, 1]), 0.95, e("easeOutCubic", [0, -5.6, 1]),
                                      1.6, [0, -5.6, 1], 2.0, e("easeInQuad", [0, -2.5, 1]), 2.4, [0, -2.5, 1]),
                     "rotation": keys(0, ZERO, 1.6, ZERO, 2.0, e("easeInQuad", [0, 0, -78]), 2.15, e("easeOutCubic", [0, 0, -84]), 2.4, [0, 0, -84])},
            "body": rot(0, ZERO, 0.3, e("easeOutCubic", [-16, 0, 0]), 0.85, e("easeInOutSine", kneel["body"]), 1.6, e("easeInOutSine", [26, 0, 4]), 2.0, e("easeInQuad", fallen["body"]), 2.4, fallen["body"]),
            "head": rot(0, ZERO, 0.3, e("easeOutCubic", [-20, 0, 0]), 0.85, e("easeInOutSine", kneel["head"]), 1.6, e("easeInOutSine", [34, 6, 0]), 2.0, e("easeInQuad", fallen["head"]), 2.4, fallen["head"]),
            "right_arm": rot(0, arms_rest[0], 0.3, e("easeOutCubic", [-50, 0, 20]), 0.85, e("easeInOutSine", kneel["right_arm"]), 1.6, [0, 0, 8], 2.0, e("easeInQuad", fallen["right_arm"]), 2.4, fallen["right_arm"]),
            "right_forearm": rot(0, ZERO, 0.3, [-30, 0, 0], 0.85, kneel["right_forearm"], 2.0, fallen["right_forearm"], 2.4, fallen["right_forearm"]),
            "left_arm": rot(0, arms_rest[1], 0.3, e("easeOutCubic", [-45, 0, -40]), 0.85, e("easeInOutSine", kneel["left_arm"]), 1.6, [-2, 0, -12], 2.0, e("easeInQuad", fallen["left_arm"]), 2.4, fallen["left_arm"]),
            "left_forearm": rot(0, ZERO, 0.3, [-40, 0, 0], 0.85, kneel["left_forearm"], 2.0, fallen["left_forearm"], 2.4, fallen["left_forearm"]),
            "right_leg": rot(0, ZERO, 0.3, [-10, 0, 0], 0.85, e("easeInOutSine", kneel["right_leg"]), 1.6, kneel["right_leg"], 2.0, fallen["right_leg"], 2.4, fallen["right_leg"]),
            "left_leg": rot(0, ZERO, 0.3, [8, 0, 0], 0.85, e("easeInOutSine", kneel["left_leg"]), 1.6, kneel["left_leg"], 2.0, fallen["left_leg"], 2.4, fallen["left_leg"]),
            "right_shin": rot(0, ZERO, 0.3, [14, 0, 0], 0.85, e("easeInQuad", kneel["right_shin"]), 1.6, kneel["right_shin"], 2.0, fallen["right_shin"], 2.4, fallen["right_shin"]),
            "left_shin": rot(0, ZERO, 0.3, [6, 0, 0], 0.85, e("easeInQuad", kneel["left_shin"]), 1.6, kneel["left_shin"], 2.0, fallen["left_shin"], 2.4, fallen["left_shin"]),
        }), loop=False),
    }
    write_model("hollow", bones, animations, [2, 3])


# --- Ember Wisp -----------------------------------------------------------------------------------------------------
# A loose ember of the Ember drifting in the dark under the ground (DESIGN §3.2): a white-hot core with two dark
# eyes inside a licking shell of translucent flame, both octagonal, a crown of flame with three tongues on two-bone
# chains that lick and lag, a trailing tail of four fading segments streaming behind it as it flies, and two rings
# of sparks circling it the opposite ways. 30 cubes on 16 bones. Drawn translucent and full bright (MobRenderers),
# so the flame's alpha is its body. misc.hurt is a jolt and a flare; misc.death gutters out: the shell shrinks, the
# core falls and flickers, the sparks fly off (EmberWispEntity keeps the body 24 ticks).

def ember_wisp():
    core = Material(CORE, base=2.6, noise=0.5, grad=0.8, top=1.0, bottom=-0.6)

    def flame_mat(base):
        # Hotter at the bottom: the gradient runs the other way (grad < 0).
        return Material(FLAME, base=base, noise=0.7, grain="v", grad=-1.6, top=1.0, bottom=1.4)

    def core_front(face, rng):
        # Two dark eyes in the white heat, the glow cleared under them.
        for x in (0, face.w - 1):
            for y in (1, 2):
                face.set(x, y, EMBER_DARK)
                face.glow.set(face.x0 + x, face.y0 + y, (0, 0, 0, 0))
        face.set(1, 2, EMBER_DEEP)
        face.set(face.w - 2, 2, EMBER_DEEP)

    shell_deco = compose(round_corners, lambda f, r: flame_tongues(f, r, 3, 0.55), on("top", clear_all), fade(105, 185), glow_all(85, 0.9))
    tongue_deco = compose(lambda f, r: flame_tongues(f, r, 1, 0.4), fade(190, 235), glow_all(170))
    tail_deco = [compose(lambda f, r: flame_tongues(f, r, 1, 0.3), fade(170 - 35 * i, 110 - 25 * i), glow_all(150 - 30 * i, 1.0 - 0.1 * i)) for i in range(4)]
    spark = lambda base, colors=FLAME: Material(colors, base=base, noise=0.3, glow=1.0)

    bones = [
        Bone("root", [0, 0, 0]),
        Bone("core", [0, 4, 0], "root", None,
             octagon([-2, 2, -2], [4, 4, 4], core, {"all": compose(glow_all(), on("front", core_front))}, shrink=0.8)
             + [Box([-1, 3, -1], [2, 2, 2], Material(CORE, base=4.5, noise=0.3, glow=1.0))]),
        Bone("shell", [0, 4, 0], "root", None,
             octagon([-3, 1, -3], [6, 6, 6], flame_mat(2.6), {"all": shell_deco}, shrink=0.8)),
        Bone("flame", [0, 7, 0], "root", None, [
            Box([-1.2, 6.8, -1.2], [2.4, 1.8, 2.4], flame_mat(3.2), {"all": tongue_deco}),
            Box([-2.4, 6.4, 0.2], [1.2, 1.6, 1.2], flame_mat(3.0), {"all": tongue_deco}),
            Box([1.3, 6.4, -1.5], [1.2, 2.0, 1.2], flame_mat(3.0), {"all": tongue_deco}),
            Box([0.4, 6.6, 1.3], [1.0, 1.4, 1.0], flame_mat(3.5), {"all": tongue_deco}),
            Box([-3.6, 3.5, -0.6], [1.2, 2.2, 1.2], flame_mat(2.8), {"all": tongue_deco}, rotation=[0, 0, 25]),
            Box([2.4, 3.2, -0.6], [1.2, 2.2, 1.2], flame_mat(2.8), {"all": tongue_deco}, rotation=[0, 0, -25]),
        ]),
    ]
    # The crown's three tongues: two bones each, licking up from the flame base.
    for name, (x, z), lean in (("tongue_a", (0, 0), [0, 0, 0]), ("tongue_b", (-1.5, 0.8), [6, 0, 18]), ("tongue_c", (1.3, -1.0), [-10, 0, -14])):
        bones += [
            Bone(name + "_1", [x, 8.2, z], "flame", lean, [Box([x - 0.6, 8.2, z - 0.6], [1.2, 1.5, 1.2], flame_mat(3.4), {"all": tongue_deco})]),
            Bone(name + "_2", [x, 9.7, z], name + "_1", None, [Box([x - 0.45, 9.7, z - 0.45], [0.9, 1.1, 0.9], flame_mat(3.8), {"all": tongue_deco})]),
        ]
    # The tail: four segments hanging behind and below, thinner and fainter toward the tip.
    bones += chain_bones("tail", "root", [0, 3.5, 2.0], 4, 1.2, flame_mat(2.4), None, thickness=1.6, rotation=[55, 0, 0],
                         box=lambda i, o, sz: [Box([o[0] + 0.15 * i, o[1], o[2] + 0.15 * i], [1.6 - 0.3 * i, 1.2, 1.6 - 0.3 * i],
                                                   flame_mat(2.4 + 0.3 * i), {"all": tail_deco[i]})])
    bones += [
        Bone("orbit", [0, 4, 0], "root", None, [
            Box([4.8, 3.5, -0.5], [1, 1, 1], spark(1.5, CORE)),
            Box([-5.3, 4.6, -0.5], [1, 1, 1], spark(4.0)),
            Box([-0.5, 2.6, 4.6], [0.8, 0.8, 0.8], spark(3.4)),
            Box([-0.4, 5.2, -4.4], [0.8, 0.8, 0.8], spark(4.5)),
            Box([-3.4, 1.9, -2.6], [0.7, 0.7, 0.7], spark(3.8)),
            Box([3.2, 2.4, 3.2], [0.7, 0.7, 0.7], spark(3.0)),
        ]),
        Bone("orbit2", [0, 4, 0], "root", [0, 0, 28], [
            Box([3.8, 3.6, -0.4], [0.8, 0.8, 0.8], spark(4.2)),
            Box([-4.2, 4.2, -0.4], [0.8, 0.8, 0.8], spark(2.0, CORE)),
            Box([-0.4, 3.8, -3.9], [0.7, 0.7, 0.7], spark(3.6)),
        ]),
    ]
    tongues = [chain_names("tongue_" + k, 2) for k in "abc"]
    tail = chain_names("tail", 4)

    def lick(length, amp, phase=0.0):
        """The tongues licking: each chain sways on X and Z out of phase, the tip lagging behind the root."""
        out = {}
        for i, names in enumerate(tongues):
            out = merge(out, lag_sway(names, length, amp, axis=0, lag=0.12, gain=1.3, phase=phase + i * 0.33))
            z = lag_sway(names, length, amp * 0.7, axis=2, lag=0.12, gain=1.3, phase=phase + i * 0.33 + 0.25)
            for n in names:
                for k, v in z[n]["rotation"].items():
                    out[n]["rotation"][k][2] = v[2]
        return out

    def float_anim(length, bob, flicker, tail_back, tongue_amp):
        n = 8
        flame_scale = []
        for i in range(n + 1):
            s = 1.0 + flicker * (0.5 + 0.5 * ((i * 7) % 5) / 4) if i % n else 1.0
            flame_scale += [length * i / n, [1, s, 1]]
        return anim(length, merge(lick(length, tongue_amp), drift(tail, length, 10, axis=2, lag=0.12, gain=1.1, base=[tail_back, 0, 0]), {
            "root": pos(0, ZERO, length / 2, e("easeInOutSine", [0, bob, 0]), length, e("easeInOutSine", ZERO)),
            "orbit": rot(0, ZERO, length / 2, [0, 180, 0], length, [0, 360, 0]),
            "orbit2": rot(0, ZERO, length / 2, [0, -180, 0], length, [0, -360, 0]),
            "shell": rot(0, ZERO, length / 2, [0, -45, 0], length, [0, -90, 0]),
            "flame": {"scale": keys(*flame_scale), "rotation": sway(length, 6, 2)},
            "core": {"scale": keys(0, [1, 1, 1], length / 2, [1.1, 1.1, 1.1], length, [1, 1, 1])},
        }))

    animations = {
        "misc.idle": float_anim(2.0, 1, 0.35, 0, 9),
        # Flying: a quicker bob, the tail streaming out behind, the tongues blown back.
        "move.fly": float_anim(1.0, 1.5, 0.5, 35, 14),
        # A hit knocks it back a pixel and it flares: the shell swells and the tongues whip.
        "misc.hurt": anim(0.35, merge(whip(tongues[0], 0.0, 40, settle=0.3), whip(tongues[1], 0.03, 35, settle=0.3), whip(tongues[2], 0.02, -38, settle=0.3), {
            "root": pos(0, ZERO, 0.06, e("easeOutCubic", [0, 0.6, 1.8]), 0.35, e("easeInOutSine", ZERO)),
            "shell": {"scale": keys(0, [1, 1, 1], 0.08, e("easeOutBack", [1.35, 1.35, 1.35]), 0.2, [0.85, 0.85, 0.85], 0.35, [1, 1, 1])},
            "core": {"scale": keys(0, [1, 1, 1], 0.06, [1.3, 1.3, 1.3], 0.35, [1, 1, 1])},
            "orbit": {"scale": keys(0, [1, 1, 1], 0.1, [1.4, 1.4, 1.4], 0.35, [1, 1, 1])},
        }), loop=False),
        # It gutters out: the shell flares once and shrinks to nothing, the tongues whip and die, the sparks fly
        # off, and the bare core drops to the ground, flickers, and goes dark.
        "misc.death": anim(1.2, merge(whip(tongues[0], 0.05, 60, settle=0.5), whip(tongues[1], 0.08, 55, settle=0.5), whip(tongues[2], 0.06, -58, settle=0.5), {
            "shell": {"scale": keys(0, [1, 1, 1], 0.12, e("easeOutBack", [1.5, 1.5, 1.5]), 0.55, e("easeInCubic", [0.05, 0.05, 0.05]), 1.2, [0.05, 0.05, 0.05]),
                      "rotation": keys(0, ZERO, 0.55, [0, 160, 0])},
            "flame": {"scale": keys(0, [1, 1, 1], 0.12, [1.4, 1.6, 1.4], 0.5, e("easeInCubic", [0.05, 0.05, 0.05]), 1.2, [0.05, 0.05, 0.05])},
            "orbit": {"scale": keys(0, [1, 1, 1], 0.3, e("easeOutCubic", [2.2, 1.2, 2.2]), 0.7, [0.05, 0.05, 0.05], 1.2, [0.05, 0.05, 0.05]),
                      "rotation": keys(0, ZERO, 0.7, [0, 240, 0])},
            "orbit2": {"scale": keys(0, [1, 1, 1], 0.3, e("easeOutCubic", [2.0, 1.4, 2.0]), 0.7, [0.05, 0.05, 0.05], 1.2, [0.05, 0.05, 0.05]),
                       "rotation": keys(0, ZERO, 0.7, [0, -240, 0])},
            "root": pos(0, ZERO, 0.2, e("easeOutCubic", [0, 1.2, 0]), 0.9, e("easeInQuad", [0, -3.5, 0]), 1.0, e("easeOutBounce", [0, -3.0, 0]), 1.2, [0, -3.0, 0]),
            "core": {"scale": keys(0, [1, 1, 1], 0.5, [1.1, 1.1, 1.1], 0.75, [0.6, 0.6, 0.6], 0.85, [0.9, 0.9, 0.9], 0.95, [0.5, 0.5, 0.5],
                                   1.05, [0.7, 0.7, 0.7], 1.2, e("easeInCubic", [0.05, 0.05, 0.05])),
                     "rotation": keys(0, ZERO, 1.0, [40, 90, 20], 1.2, [40, 90, 20])},
            "tail_1": rot(0, [55, 0, 0], 0.4, e("easeInOutSine", [20, 0, 0]), 1.0, e("easeInQuad", [-10, 0, 0]), 1.2, [-10, 0, 0]),
            "tail_2": rot(0, ZERO, 0.45, [-20, 0, 0], 1.0, [-30, 0, 0], 1.2, [-30, 0, 0]),
            "tail_3": rot(0, ZERO, 0.5, [-25, 0, 0], 1.2, [-30, 0, 0]),
            "tail_4": rot(0, ZERO, 0.55, [-25, 0, 0], 1.2, [-30, 0, 0]),
        }), loop=False),
    }
    write_model("ember_wisp", bones, animations, [1, 1], tex_w=32)


# --- Ashen Maiden ---------------------------------------------------------------------------------------------------
# The keeper of the Sanctum who turns souls into strength (DESIGN §2): a tall, quiet figure in a long ash-grey robe
# with an ember-red embroidered hem, a fringed shawl pinned with an ember, a deep hood with a lace veil over a pale
# calm face with closed eyes and ash tear-marks, bell sleeves, and the brazier bowl of embers she holds in cupped
# hands. 64 cubes on 24 bones: chamfered skirt, bodice and head, the robe's hem in two hanging pieces front and back,
# the veil in two, the sleeves' falls, forearms, hands with fingers, an octagonal bowl with a two-bone flame. She is
# seen for minutes in the hub, so misc.idle is a six-second cycle: a slow breath, a slow look, the veil, hem and
# sleeves drifting out of phase, the flame licking and the ember pulsing. She is invulnerable and never fights:
# no hurt, no death. misc.interact raises the bowl toward the player who talks to her.

def ashen_maiden():
    robe = Material(MAIDEN_ROBE, base=3.2, noise=0.4, grain="v", grad=0.9, top=0.6, edge=0.4)
    dark = robe.but(base=2.2)
    skin = Material(MAIDEN_SKIN, base=3.6, noise=0.2, grad=0.5, top=0.4)
    hair = Material(V_HAIR, base=2.6, noise=0.5, top=0.5)
    iron = Material(HOLLOW_IRON, base=3.0, noise=0.4, bevel=0.5)
    brass = Material(BRASS, base=3.6, noise=0.4, bevel=0.6)
    folds = cloth(3, -16)

    def hem(face, rng):
        if face.name not in ("front", "back"):
            clear_all(face)
            return
        folds(face, rng)
        # Two rows of ember-red stitching above a dark hem, a little wear.
        face.hline(0, face.h - 1, face.w, MAIDEN_ROBE[0])
        for x in range(face.w):
            face.set(x, face.h - 3, EMBER_DARK if x % 2 else TABARD[4])
            if x % 4 == 1:
                face.set(x, face.h - 2, TABARD[5])
        ragged_edge(face, rng, "bottom", 1, 0.25)

    def side_hem(face, rng):
        if face.name in ("left", "right"):
            folds(face, rng)
            face.hline(0, face.h - 1, face.w, MAIDEN_ROBE[0])
            for x in range(face.w):
                face.set(x, face.h - 3, EMBER_DARK if x % 2 else TABARD[4])
        else:
            clear_all(face)

    def skirt(face, rng):
        folds(face, rng)

    def bodice(face, rng):
        folds(face, rng)
        if face.name == "front":
            # A crossed wrap over the chest.
            for y in range(face.h - 2):
                face.set(min(face.w - 1, 1 + y // 2), y, MAIDEN_ROBE[1])
                face.set(max(0, face.w - 2 - y // 2), y, MAIDEN_ROBE[1])
                face.set(min(face.w - 1, 2 + y // 2), y, MAIDEN_ROBE[4])

    def sash(face, rng):
        if face.side():
            face.rect(0, 0, face.w, face.h, TABARD[3])
            face.hline(0, 0, face.w, TABARD[4])
            face.hline(0, face.h - 1, face.w, TABARD[1])
            if face.name == "front":
                face.rect(face.cx - 1, 0, 2, face.h, TABARD[2])    # the knot

    def shawl(face, rng):
        folds(face, rng)
        if face.name == "front":
            # The clasp: an ember set in iron.
            face.rect(face.cx - 1, 1, 2, 3, HOLLOW_IRON[3])
            face.set(face.cx - 1, 1, HOLLOW_IRON[5])
            face.lit(face.cx - 1, 2, EMBER, 0.9)
            face.lit(face.cx, 2, EMBER_HOT, 0.9)

    def fringe(face, rng):
        if face.name in ("front", "back", "left", "right"):
            for x in range(face.w):
                face.set(x, 0, MAIDEN_ROBE[3])
                face.set(x, 1, MAIDEN_ROBE[2] if x % 2 else MAIDEN_ROBE[1])
                if x % 2:
                    face.clear(x, face.h - 1)
        else:
            clear_all(face)

    def face_front(face, rng):
        # Hair parted under the hood; brows; closed eyes with a faint ember glow between the lashes; a straight
        # nose; ash tear-marks under one eye; a calm small mouth.
        face.rect(0, 0, face.w, 2, V_HAIR[2])
        face.set(face.cx, 1, V_HAIR[1])
        face.set(0, 2, V_HAIR[2])
        face.set(face.w - 1, 2, V_HAIR[2])
        face.hline(1, 2, 2, MAIDEN_SKIN[2])
        face.hline(face.w - 3, 2, 2, MAIDEN_SKIN[2])
        for ex in (1, face.w - 3):
            face.hline(ex, 3, 2, MAIDEN_SKIN[1])          # the closed lids
            face.set(ex + (0 if ex == 1 else 1), 3, V_HAIR[3])
            face.lit(ex + (1 if ex == 1 else 0), 4, EMBER_DEEP, 0.35)
        face.set(face.cx, 4, MAIDEN_SKIN[2])
        face.set(face.cx, 5, MAIDEN_SKIN[1])
        face.set(1, 5, MAIDEN_SKIN[1])                    # an ash tear-mark under one eye
        face.set(1, 6, MAIDEN_SKIN[2])
        face.set(face.w - 2, 5, MAIDEN_SKIN[5])           # a touch of light on the other cheek
        face.hline(face.cx - 1, 6, 2, TABARD[4])
        face.set(face.cx - 1, 7, MAIDEN_SKIN[2])
        face.set(face.cx, 7, MAIDEN_SKIN[2])

    def hair_deco(face, rng):
        if face.name == "front":
            clear_all(face)
        elif face.name == "bottom":
            clear_all(face)
        else:
            for x in range(0, face.w, 2):
                face.vline(x, 0, face.h, V_HAIR[3])

    def hood(face, rng):
        folds(face, rng)
        if face.name == "front":
            open_front(face, 1, 1)
            face.hline(1, 0, face.w - 2, MAIDEN_ROBE[5])
        elif face.name == "bottom":
            clear_all(face)

    def veil(face, rng):
        if face.name not in ("front", "back"):
            clear_all(face)
            return
        # Lace: a diagonal lattice with the threads kept, the rim rows solid, the bottom ragged.
        for y in range(1, face.h):
            for x in range(face.w):
                if (x + y) % 3 != 0 and not (y == face.h - 1 and x % 3 == 0):
                    face.clear(x, y)
                else:
                    face.set(x, y, MAIDEN_ROBE[6] if (x + y) % 6 == 0 else MAIDEN_ROBE[5])
        face.hline(0, 0, face.w, MAIDEN_ROBE[6])

    def sleeve(face, rng):
        folds(face, rng)
        if face.side():
            face.hline(0, face.h - 1, face.w, TABARD[2])

    def cuff(face, rng):
        folds(face, rng)
        if face.side():
            face.hline(0, face.h - 1, face.w, TABARD[3])
            face.hline(0, face.h - 2, face.w, TABARD[1])
        if face.name in ("bottom", "top"):
            clear_all(face)

    def bowl(face, rng):
        plate_face(face, rng, BRASS, rivet_step=2, dent=0.0)
        if face.name == "top":
            for y in range(1, face.h - 1):
                for x in range(1, face.w - 1):
                    face.lit(x, y, EMBER_DEEP if (x + y) % 2 else EMBER, 0.8)

    flame = Material(FLAME, base=3.4, noise=0.6, grain="v", grad=-1.2, glow=1.0)
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("robe", [0, 12, 0], "root", None,
             chamfered([-5, 6, -3.5], [10, 7.5, 7], robe, {"all": skirt}, c=0.6)
             + [Box([-5.2, 0.5, -3.3], [0.6, 6.5, 6.6], robe.but(base=2.8), {"all": side_hem}),
                Box([4.6, 0.5, -3.3], [0.6, 6.5, 6.6], robe.but(base=2.8), {"all": side_hem}),
                Box([-1.5, 6.2, -3.9], [3, 7, 0.5], dark, {"front": lambda f, r: trim(f, TABARD[3], ("left", "right"))})]),
    ]
    bones += hanging("hem_front", "robe", [0, 6.5, -3.7], 2, 3.4, 10, robe, {"all": hem}, z_step=-0.2, taper=0.4)
    bones += hanging("hem_back", "robe", [0, 6.5, 3.7], 2, 3.4, 10, robe.but(base=2.8), {"all": hem}, z_step=0.2, taper=0.4)
    bones += [
        Bone("body", [0, 13, 0], "root", None,
             chamfered([-4, 13, -2], [8, 11, 4], robe.but(base=2.8), {"all": bodice}, c=0.6)
             + [Box([-4.3, 12.5, -2.3], [8.6, 2.2, 4.6], Material(TABARD, base=3.0, noise=0.4, grain="h"), {"all": sash}),
                Box([3.2, 7.5, 0.5], [1.2, 5.5, 0.5], Material(TABARD, base=2.8, noise=0.4, grain="v"),
                    {"all": lambda f, r: ragged_edge(f, r, "bottom", 1, 0.5) if f.side() else None}, rotation=[0, 0, -8], pivot=[3.8, 13, 0.75])]),
        Bone("mantle", [0, 23, 0], "body", None, [
            Box([-5, 19, -2.6], [10, 5, 5.2], robe.but(base=4.0), {"all": shawl}, inflate=0.2),
            Box([-5.3, 17.6, -2.9], [10.6, 2, 5.8], robe.but(base=3.4), {"all": fringe}),
            Box([-4.2, 23.6, -2.2], [8.4, 1.2, 4.4], robe.but(base=3.0), {"all": folds}),
        ]),
        Bone("head", [0, 24, 0], "body", None,
             chamfered([-3.5, 24, -3.5], [7, 8, 7], skin, None, c=0.7)
             + [Box([-3.5, 24, -3.9], [7, 8, 0.6], skin, {"front": face_front}),
                Box([-3.7, 25, -3.7], [7.4, 7.3, 7.4], hair, {"all": hair_deco}),
                Box([-4.1, 24.5, -2.5], [0.8, 5, 2.5], hair.but(base=2.2)),
                Box([3.3, 24.5, -2.5], [0.8, 5, 2.5], hair.but(base=2.2))]),
        Bone("hood", [0, 24, 0], "head", None,
             octagon([-4.6, 24.2, -4.6], [9.2, 8.8, 9.2], robe.but(base=3.4), {"all": hood}, shrink=0.72)
             + [Box([-4.8, 31.0, -5.2], [9.6, 1.6, 2.2], robe.but(base=3.8), {"all": folds}, rotation=[14, 0, 0], pivot=[0, 32, -4]),
                Box([-3.6, 18.5, 3.0], [7.2, 6.5, 1], robe.but(base=2.2), {"all": compose(folds, tatters(1, 0.4))}, rotation=[10, 0, 0], pivot=[0, 25, 3.5]),
                Box([-4.9, 30.2, -5.0], [9.8, 0.8, 9.8], iron.but(base=2.6), {"all": lambda f, r: f.set(f.cx, 0, BRASS[5]) if f.name == "front" else None})]),
    ]
    bones += hanging("veil", "hood", [0, 30.6, -5.1], 2, 3.4, 7, robe.but(base=4.2), {"all": veil}, z_step=-0.15, taper=0.2)
    upper = Box([-6.5, 16, -1.5], [3, 6.5, 3], robe, {"all": sleeve})
    shoulder = Box([-6.9, 20.3, -1.9], [3.8, 2.2, 3.8], robe.but(base=3.4), {"all": folds})
    fore = Box([-6.3, 11, -1.3], [2.6, 5.8, 2.6], robe.but(base=2.8), {"all": sleeve})
    bell = Box([-7.3, 10.2, -2.3], [4.6, 3.4, 4.6], robe.but(base=2.6), {"all": cuff})
    palm = Box([-6, 9.2, -1], [2, 2, 2], skin)
    digits = fingers(-6, 9.2, -1, 3, (0.5, 1.6, 0.5), skin.but(base=3.2), spread=0.25, curl=40)
    bones += [
        Bone("right_arm", [-4.5, 22, 0], "body", [-25, -25, 12], [upper, shoulder]),
        Bone("right_forearm", [-5, 16.5, 0], "right_arm", [-35, 0, 0], [fore, bell]),
        Bone("right_hand", [-5, 11, 0], "right_forearm", [-30, 0, 0], [palm] + digits),
        Bone("left_arm", [4.5, 22, 0], "body", [-25, 25, -12], [mirror_box(upper), mirror_box(shoulder)]),
        Bone("left_forearm", [5, 16.5, 0], "left_arm", [-35, 0, 0], [mirror_box(fore), mirror_box(bell)]),
        Bone("left_hand", [5, 11, 0], "left_forearm", [-30, 0, 0], [mirror_box(palm)] + [mirror_box(d) for d in digits]),
    ]
    bones += hanging("sleeve_right", "right_forearm", [-5, 10.4, 2.0], 2, 2.6, 4.2, robe.but(base=2.6), {"all": compose(folds, tatters(1, 0.3))}, z_step=0.15, taper=0.4)
    bones += hanging("sleeve_left", "left_forearm", [5, 10.4, 2.0], 2, 2.6, 4.2, robe.but(base=2.6), {"all": compose(folds, tatters(1, 0.3))}, z_step=0.15, taper=0.4)
    bones += [
        Bone("ember", [0, 14, -7.5], "body", None,
             octagon([-2.6, 13.2, -10.1], [5.2, 2.2, 5.2], brass, {"all": bowl}, shrink=0.8)
             + [Box([-1.1, 14.4, -8.6], [2.2, 2, 2.2], Material(CORE, base=3.2, noise=0.5, glow=1.0)),
                Box([-2.4, 16.0, -7.7], [0.7, 0.7, 0.7], Material(FLAME, base=5.0, noise=0.3, glow=1.0)),
                Box([1.6, 15.6, -9.5], [0.6, 0.6, 0.6], Material(FLAME, base=4.4, noise=0.3, glow=1.0))]),
        Bone("flame_1", [0, 16.2, -7.5], "ember", None, [Box([-0.8, 16.2, -8.3], [1.6, 1.6, 1.6], flame, {"all": lambda f, r: flame_tongues(f, r, 1, 0.5)})]),
        Bone("flame_2", [0, 17.8, -7.5], "flame_1", None, [Box([-0.5, 17.8, -8.0], [1.0, 1.3, 1.0], flame.but(base=4.4), {"all": lambda f, r: flame_tongues(f, r, 1, 0.5)})]),
    ]
    hems = [chain_names("hem_front", 2), chain_names("hem_back", 2)]
    veils = chain_names("veil", 2)
    sleeves = [chain_names("sleeve_right", 2), chain_names("sleeve_left", 2)]
    flames = ["flame_1", "flame_2"]

    def flicker(names, length, amp, cycles, phase=0.0):
        """A fast lick for the flame inside a long cycle: cycles sine periods over the length, the tip lagging."""
        out = {}
        for i, n in enumerate(names):
            pairs = []
            steps = int(cycles * 6)
            for k in range(steps + 1):
                t = length * k / steps
                a = 2 * math.pi * (cycles * k / steps + phase - 0.1 * i)
                pairs += [t, [amp * 1.3 ** i * math.sin(a), 0, amp * 0.7 * 1.3 ** i * math.cos(a * 0.7)]]
            out[n] = {"rotation": keys(*pairs)}
        return out

    def loose_drift(length, amp, phase=0.0):
        return merge(drift(hems[0], length, amp, phase=phase), drift(hems[1], length, -amp, phase=phase + 0.3),
                     drift(veils, length, amp * 0.8, phase=phase + 0.15), drift(sleeves[0], length, amp * 1.2, phase=phase + 0.4),
                     drift(sleeves[1], length, amp * 1.2, phase=phase + 0.55))

    pulse = keys(0, [1, 1, 1], 1.0, e("easeInOutSine", [1.2, 1.2, 1.2]), 2.0, e("easeInOutSine", [1, 1, 1]), 3.5, e("easeInOutSine", [1.15, 1.15, 1.15]),
                 4.5, e("easeInOutSine", [1, 1, 1]), 5.2, [1.1, 1.1, 1.1], 6.0, [1, 1, 1])
    animations = {
        "misc.idle": anim(6.0, merge(breathe(6.0, chest=1.2, head=0, shoulders=0.5), loose_drift(6.0, 2.5), flicker(flames, 6.0, 10, 4), {
            "head": rot(0, [4, 0, 0], 1.6, e("easeInOutSine", [3, 9, 1]), 3.0, e("easeInOutSine", [5, 4, 0]), 4.4, e("easeInOutSine", [2, -7, -1]), 6.0, e("easeInOutSine", [4, 0, 0])),
            "hood": rot(0, ZERO, 3.0, e("easeInOutSine", [-1.5, 0, 0]), 6.0, e("easeInOutSine", ZERO)),
            "right_forearm": rot(0, ZERO, 3.0, e("easeInOutSine", [-2.5, 0, 0]), 6.0, e("easeInOutSine", ZERO)),
            "left_forearm": rot(0, ZERO, 3.0, e("easeInOutSine", [-2.5, 0, 0]), 6.0, e("easeInOutSine", ZERO)),
            "right_hand": rot(0, ZERO, 2.0, e("easeInOutSine", [-4, 0, 0]), 4.5, e("easeInOutSine", [2, 0, 0]), 6.0, e("easeInOutSine", ZERO)),
            "left_hand": rot(0, [2, 0, 0], 2.5, e("easeInOutSine", [-3, 0, 0]), 5.0, e("easeInOutSine", [2, 0, 0]), 6.0, [2, 0, 0]),
            "ember": {"position": keys(0, ZERO, 3.0, e("easeInOutSine", [0, 0.5, 0]), 6.0, e("easeInOutSine", ZERO)), "scale": pulse},
            "mantle": rot(0, ZERO, 3.0, e("easeInOutSine", [1, 0, 0]), 6.0, e("easeInOutSine", ZERO)),
        })),
        # She lifts the bowl toward the player who speaks to her and bows her head over it; the flame flares.
        "misc.interact": anim(1.5, merge(flicker(flames, 1.5, 16, 2), {
            "right_arm": rot(0, ZERO, 0.5, e("easeInOutSine", [-22, 4, 0]), 1.0, [-22, 4, 0], 1.5, e("easeInOutSine", ZERO)),
            "left_arm": rot(0, ZERO, 0.5, e("easeInOutSine", [-22, -4, 0]), 1.0, [-22, -4, 0], 1.5, e("easeInOutSine", ZERO)),
            "right_forearm": rot(0, ZERO, 0.5, e("easeInOutSine", [-8, 0, 0]), 1.0, [-8, 0, 0], 1.5, e("easeInOutSine", ZERO)),
            "left_forearm": rot(0, ZERO, 0.5, e("easeInOutSine", [-8, 0, 0]), 1.0, [-8, 0, 0], 1.5, e("easeInOutSine", ZERO)),
            "ember": {"position": keys(0, ZERO, 0.5, e("easeInOutSine", [0, 3.5, -2.5]), 1.0, [0, 3.5, -2.5], 1.5, e("easeInOutSine", ZERO)),
                      "scale": keys(0, [1, 1, 1], 0.5, e("easeOutBack", [1.4, 1.4, 1.4]), 1.0, [1.3, 1.3, 1.3], 1.5, [1, 1, 1])},
            "head": rot(0, ZERO, 0.5, e("easeInOutSine", [-10, 0, 0]), 0.9, e("easeInOutSine", [8, 0, 0]), 1.5, e("easeInOutSine", ZERO)),
            "body": rot(0, ZERO, 0.5, e("easeInOutSine", [-3, 0, 0]), 1.5, e("easeInOutSine", ZERO)),
            "veil_1": rot(0, ZERO, 0.5, [-6, 0, 0], 1.0, [4, 0, 0], 1.5, ZERO),
            "veil_2": rot(0, ZERO, 0.6, [-8, 0, 0], 1.1, [5, 0, 0], 1.5, ZERO),
            "sleeve_right_1": rot(0, ZERO, 0.55, [10, 0, 0], 1.5, ZERO),
            "sleeve_left_1": rot(0, ZERO, 0.55, [10, 0, 0], 1.5, ZERO),
        }), loop=False),
    }
    write_model("ashen_maiden", bones, animations, [1.5, 2.5])


# --- Ashen Zombie ---------------------------------------------------------------------------------------------------
# What the Curse of the Night raises for each death (DESIGN §3.3): a broad dead man gone grey with ash, the skin
# split along smouldering seams, white-hot embers for eyes, a slack jaw on its own bone, the rags of a blue work shirt
# torn open over the wound and hanging in two tails, trousers in tatters, flakes of ash crust lifting off the
# shoulders and scalp, arms reaching forward with the fingers hanging. 50 cubes on 18 bones: chamfered torso and
# skull, forearms and hands with four fingers, shins and boots. misc.hurt flinches, misc.stagger (a quarter of its
# health in one hit) reels it back a second, misc.death topples it forward stiff onto its arms (AshenZombieEntity
# keeps the body 40 ticks, ash flaking off it).

def ashen_zombie():
    skin = Material(ZOMBIE_SKIN, base=3.0, noise=0.55, cell=2.5, speck=0.04, grad=1.0, top=0.9, bottom=-1.0, edge=0.5, bevel=0.35)
    shirt = Material(CLOTH_BLUE, base=3.0, noise=0.45, grain="v", grad=0.8, edge=0.3)
    trousers = Material(LINEN, base=2.4, noise=0.5, grain="v", grad=0.8)
    crust = Material(ZOMBIE_SKIN, base=1.8, noise=0.6, speck=0.1)
    ivory = Material(IVORY, base=3.4, noise=0.4, bevel=0.3)

    def ashy(face, rng, seams_n=1, chance=0.8):
        if face.side():
            skin_cracks(face, rng, ZOMBIE_SKIN[0], max(1, face.w * face.h // 22))
            scatter(face, rng, max(1, face.w * face.h // 40), ZOMBIE_SKIN[6])
            for _ in range(seams_n):
                if rng.random() < chance and face.w * face.h >= 12:
                    ember_seams(face, rng, 1, length=(2, 4), dark=ZOMBIE_SKIN[0])

    def limb(face, rng):
        ashy(face, rng, 1, 0.35)

    def face_plate(face, rng):
        # A heavy brow, sockets with white-hot embers, a broken nose, the upper teeth over the slack mouth.
        face.hline(0, 0, face.w, ZOMBIE_SKIN[4])
        face.hline(0, 1, face.w, ZOMBIE_SKIN[1])
        for ex in (1, face.w - 3):
            face.rect(ex, 2, 2, 2, VOID)
        face.lit(2, 3, EMBER_HOT, 1.0)
        face.lit(face.w - 3, 3, EMBER_HOT, 1.0)
        face.lit(1, 3, EMBER_DEEP, 0.5)
        face.lit(face.w - 2, 3, EMBER_DEEP, 0.5)
        face.set(face.cx - 1, 4, ZOMBIE_SKIN[1])
        face.set(face.cx, 4, ZOMBIE_SKIN[0])
        face.set(face.cx, 5, ZOMBIE_SKIN[1])
        face.set(0, 5, ZOMBIE_SKIN[1])
        face.set(face.w - 1, 5, ZOMBIE_SKIN[1])
        face.hline(1, 6, face.w - 2, VOID)
        for x in range(2, face.w - 2, 2):
            face.set(x, 6, IVORY[3])
        face.hline(1, 7, face.w - 2, ZOMBIE_SKIN[1])
        skin_cracks(face, rng, ZOMBIE_SKIN[0], 2, (1, 3))

    def jaw_front(face, rng):
        face.hline(0, 0, face.w, VOID)
        for x in range(1, face.w, 2):
            face.set(x, 0, IVORY[4])

    def wound(face, rng):
        # The split chest under the torn shirt: a glowing seam with scorched edges.
        for y in range(1, face.h - 1):
            x = face.cx - 1 + (y % 2)
            face.lit(x, y, EMBER if y % 3 else EMBER_HOT, 0.9)
            face.set(x - 1, y, ZOMBIE_SKIN[0])
            face.set(x + 1, y, ZOMBIE_SKIN[0])
        ashy(face, rng, 0)

    def shirt_front(face, rng):
        cloth(3, -16)(face, rng)
        # Torn open down the middle; buttons on one side, a pocket on the other.
        for y in range(face.h):
            for x in range(face.cx - 1 - (y > 3), face.cx + 1 + (y > 5)):
                face.clear(x, y)
        face.vline(face.cx - 2 - 0, 0, face.h, CLOTH_BLUE[0])
        for y in range(1, face.h, 3):
            face.set(face.cx + 2, y, IVORY[5])
        face.rect(1, 2, 2, 2, CLOTH_BLUE[2])
        ragged_edge(face, rng, "bottom", 2, 0.5)
        holes(face, rng, 1)

    def shirt_back(face, rng):
        cloth(3, -16)(face, rng)
        ragged_edge(face, rng, "bottom", 2, 0.5)
        holes(face, rng, 2)
        scatter(face, rng, 3, ZOMBIE_SKIN[5])          # ash on the shoulders

    def rag(face, rng):
        if face.name in ("front", "back"):
            cloth(2, -16)(face, rng)
            ragged_edge(face, rng, "bottom", 2, 0.6)
            holes(face, rng, 1)
        else:
            clear_all(face)

    def sleeve(face, rng):
        if face.side():
            cloth(2, -14)(face, rng)
            ragged_edge(face, rng, "bottom", 2, 0.6)
        else:
            clear_all(face)

    def trouser(face, rng):
        if face.side():
            cloth(3, -14)(face, rng)
            ragged_edge(face, rng, "bottom", 2, 0.5)
            holes(face, rng, 1)
        else:
            clear_all(face)

    def flake(face, rng):
        scatter(face, rng, max(1, face.w * face.h // 5), ZOMBIE_SKIN[0])
        scatter(face, rng, max(1, face.w * face.h // 8), ZOMBIE_SKIN[6])

    def belt(face, rng):
        if face.side():
            face.hline(0, 0, face.w, STRAP[1])
            face.hline(0, face.h - 1, face.w, STRAP[0])

    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 24, 0], "root", [6, 0, 0],
             chamfered([-4.5, 12, -2.5], [9, 12, 5], skin, {"all": ashy}, c=0.6)
             + [Box([-2.5, 14, -2.9], [5, 8, 0.6], skin.but(base=2.6), {"front": wound}),
                Box([-4.7, 13.5, -3.1], [9.4, 10, 0.7], shirt, {"front": shirt_front}),
                Box([-4.7, 13.5, 2.4], [9.4, 10, 0.7], shirt.but(base=2.6), {"back": shirt_back}),
                Box([-4.9, 20.5, -2.9], [2.6, 3.2, 5.8], shirt.but(base=3.2), {"all": cloth(2, -14)}),           # the shoulders of the shirt
                Box([2.3, 20.5, -2.9], [2.6, 3.2, 5.8], shirt.but(base=3.2), {"all": cloth(2, -14)}),
                Box([-4.7, 12, -2.7], [9.4, 1.6, 5.4], Material(STRAP, base=2.4, noise=0.4), {"all": belt}),
                Box([-0.8, 12.1, -2.9], [1.6, 1.4, 0.4], Material(HOLLOW_IRON, base=4.0, noise=0.4, bevel=0.5)),
                Box([-3.6, 24.0, -2.2], [3.2, 0.8, 3.4], crust, {"top": flake}, rotation=[0, 20, 8]),
                Box([0.8, 24.0, -1.0], [2.6, 0.8, 2.8], crust, {"top": flake}, rotation=[0, -15, -10]),
                Box([-1.2, 19.5, 2.6], [3.6, 0.7, 2.6], crust, {"all": flake}, rotation=[-30, 10, 0], pivot=[0, 19.5, 2.6])]),
        Bone("head", [0, 24, -1], "body", [-6, 0, 0],
             chamfered([-4, 24, -5], [8, 8, 8], skin.but(base=3.2), {"all": ashy}, c=0.8)
             + [Box([-4, 24, -5.4], [8, 8, 0.6], skin.but(base=3.0), {"front": face_plate}),
                Box([-4.3, 31.6, -4.6], [4.6, 0.8, 4], crust, {"top": flake}, rotation=[0, 12, 0]),
                Box([0.2, 31.6, -2.4], [3.8, 0.8, 4.6], crust, {"top": flake}, rotation=[0, -22, 0]),
                Box([-4.3, 26, -2], [0.8, 2.2, 1.6], skin.but(base=2.4)),                                         # an ear left
                ]),
        Bone("jaw", [0, 25.2, -1.2], "head", None, [
            Box([-3, 23.2, -5.2], [6, 2, 4.6], skin.but(base=2.5), {"front": jaw_front, "top": lambda f, r: f.rect(0, 0, f.w, f.h, VOID)}),
        ]),
    ]
    bones += hanging("tail_front", "body", [-2.8, 13.5, -3.1], 2, 2.6, 3.6, shirt.but(base=2.8), {"all": rag}, z_step=-0.2, taper=0.4)
    bones += hanging("tail_back", "body", [1.5, 13.5, 2.9], 2, 2.6, 4.4, shirt.but(base=2.4), {"all": rag}, z_step=0.2, taper=0.4)
    upper = Box([-8.5, 16, -2], [4, 7, 4], skin, {"sides": limb})
    sleeve_box = Box([-8.8, 18.5, -2.3], [4.6, 4.8, 4.6], shirt.but(base=2.8), {"all": sleeve})
    fore = Box([-8.3, 9.8, -1.8], [3.6, 6.5, 3.6], skin.but(base=2.7), {"sides": limb})
    palm = Box([-8.5, 7.4, -2], [4, 2.6, 4], skin.but(base=2.5))
    digits = fingers(-8.4, 7.4, -2.0, 4, (0.8, 2.4, 0.8), skin.but(base=2.2), spread=0.2, curl=12)
    bones += [
        Bone("right_arm", [-5.5, 22, 0], "body", [-75, 0, 4], [upper, sleeve_box]),
        Bone("right_forearm", [-6.5, 16, 0], "right_arm", [-6, 0, 0], [fore]),
        Bone("right_hand", [-6.5, 10, 0], "right_forearm", [52, 0, 0], [palm] + digits),
        Bone("left_arm", [5.5, 22, 0], "body", [-75, 0, -4], [mirror_box(upper), mirror_box(sleeve_box)]),
        Bone("left_forearm", [6.5, 16, 0], "left_arm", [-6, 0, 0], [mirror_box(fore)]),
        Bone("left_hand", [6.5, 10, 0], "left_forearm", [52, 0, 0], [mirror_box(palm)] + [mirror_box(d) for d in digits]),
    ]
    thigh = Box([-4.2, 6, -2], [4, 6.5, 4], skin.but(base=2.8), {"sides": limb})
    trouser_box = Box([-4.5, 6.5, -2.3], [4.6, 6, 4.6], trousers, {"all": trouser})
    shin = Box([-4, 1.2, -1.9], [3.8, 5.2, 3.8], skin.but(base=2.5), {"sides": limb})
    boot = Box([-4.2, 0, -3.2], [4.2, 1.6, 5.2], Material(STRAP, base=1.8, noise=0.4), {"all": lambda f, r: trim(f, STRAP[0], ("bottom",)) if f.side() else None})
    bones += [
        Bone("right_leg", [-2, 12, 0], "root", None, [thigh, trouser_box]),
        Bone("right_shin", [-2, 6, 0], "right_leg", None, [shin, boot]),
        Bone("left_leg", [2, 12, 0], "root", None, [mirror_box(thigh), mirror_box(trouser_box)]),
        Bone("left_shin", [2, 6, 0], "left_leg", None, [mirror_box(shin), mirror_box(boot)]),
    ]
    rags = [chain_names("tail_front", 2), chain_names("tail_back", 2)]

    def rags_drift(length, amp, phase=0.0):
        return merge(drift(rags[0], length, amp, phase=phase), drift(rags[1], length, -amp * 0.9, phase=phase + 0.3))

    def rags_whip(t0, amp, settle_t=0.4):
        return merge(whip(rags[0], t0, amp, settle=settle_t), whip(rags[1], t0 + 0.03, -amp * 0.9, settle=settle_t))

    arms_rest = [[-75, 0, 4], [-75, 0, -4]]
    fallen = {"body": [6, 0, 0], "head": [-20, 25, 10], "jaw": [18, 0, 0], "right_arm": [-70, 0, 30], "left_arm": [-80, 0, -38],
              "right_forearm": [10, 0, 0], "left_forearm": [-5, 0, 0], "right_hand": [-15, 0, 0], "left_hand": [-20, 0, 0],
              "right_leg": [6, 0, 6], "left_leg": [-4, 0, -4], "right_shin": [10, 0, 0], "left_shin": [22, 0, 0]}
    animations = {
        # Swaying on its feet, the reaching arms drifting, the jaw working, the head rolling slowly.
        "misc.idle": anim(3.0, merge(breathe(3.0, chest=2.0, head=0, shoulders=1.0), rags_drift(3.0, 3), {
            "head": rot(0, ZERO, 1.0, e("easeInOutSine", [5, 8, 4]), 2.0, e("easeInOutSine", [-3, -6, -3]), 3.0, e("easeInOutSine", ZERO)),
            "jaw": rot(0, ZERO, 0.8, [10, 0, 0], 1.4, [3, 0, 0], 2.2, [12, 0, 0], 3.0, ZERO),
            "right_arm": rot(0, arms_rest[0], 1.5, e("easeInOutSine", [-84, 0, 7]), 3.0, e("easeInOutSine", arms_rest[0])),
            "left_arm": rot(0, arms_rest[1], 1.5, e("easeInOutSine", [-76, 0, -7]), 3.0, e("easeInOutSine", arms_rest[1])),
            "right_hand": rot(0, ZERO, 1.5, e("easeInOutSine", [-8, 0, 0]), 3.0, e("easeInOutSine", ZERO)),
            "left_hand": rot(0, [-6, 0, 0], 1.5, e("easeInOutSine", ZERO), 3.0, e("easeInOutSine", [-6, 0, 0])),
        })),
        # A shamble (speed 0.25): a limp on the right, the arms held out and swaying, the body rolling.
        "move.walk": anim(1.0, merge(walk(1.0, stride=28, swing=8, bob=0.6, lean=4, twist=5, roll=5, arm_rest=arms_rest), rags_drift(1.0, 8, 0.1), {
            "right_leg": rot(0, [22, 0, 0], 0.5, e("easeInOutSine", [-34, 0, 0]), 1.0, e("easeInOutSine", [22, 0, 0])),
            "right_shin": rot(0, [8, 0, 0], 0.25, [36, 0, 0], 0.5, e("easeInOutSine", [4, 0, 0]), 1.0, e("easeInOutSine", [8, 0, 0])),
            "left_shin": rot(0, [4, 0, 0], 0.5, e("easeInOutSine", [8, 0, 0]), 0.75, [26, 0, 0], 1.0, e("easeInOutSine", [4, 0, 0])),
            "jaw": rot(0, [6, 0, 0], 0.5, [12, 0, 0], 1.0, [6, 0, 0]),
            "right_hand": rot(0, ZERO, 0.5, [-10, 0, 0], 1.0, ZERO),
            "left_hand": rot(0, [-10, 0, 0], 0.5, ZERO, 1.0, [-10, 0, 0]),
        })),
        # Both arms drawn up and back, then brought down together with the whole body behind them.
        "attack.swing": anim(0.5, merge(rags_whip(0.26, 28, 0.24), {
            "right_arm": rot(0, arms_rest[0], 0.16, e("easeInOutSine", [-125, 0, 14]), 0.28, e("easeOutBack", [-40, 0, 4]), 0.5, e("easeInOutSine", arms_rest[0])),
            "left_arm": rot(0, arms_rest[1], 0.16, e("easeInOutSine", [-125, 0, -14]), 0.28, e("easeOutBack", [-40, 0, -4]), 0.5, e("easeInOutSine", arms_rest[1])),
            "right_forearm": rot(0, ZERO, 0.16, [-30, 0, 0], 0.28, e("easeOutCubic", [6, 0, 0]), 0.5, ZERO),
            "left_forearm": rot(0, ZERO, 0.16, [-30, 0, 0], 0.28, e("easeOutCubic", [6, 0, 0]), 0.5, ZERO),
            "right_hand": rot(0, ZERO, 0.16, [-30, 0, 0], 0.28, [20, 0, 0], 0.5, ZERO),
            "left_hand": rot(0, ZERO, 0.16, [-30, 0, 0], 0.28, [20, 0, 0], 0.5, ZERO),
            "body": rot(0, ZERO, 0.16, e("easeInOutSine", [-8, 0, 0]), 0.28, e("easeOutBack", [16, 0, 0]), 0.5, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, 0.16, [-14, 0, 0], 0.28, [10, 0, 0], 0.5, ZERO),
            "jaw": rot(0, ZERO, 0.12, [22, 0, 0], 0.35, [6, 0, 0], 0.5, ZERO),
            "root": pos(0, ZERO, 0.16, [0, 0.4, 0.8], 0.28, e("easeOutCubic", [0, -0.6, -1.2]), 0.5, ZERO),
        }), loop=False),
        "misc.hurt": anim(0.45, merge(flinch(0.45, amount=1.0, arms=()), rags_whip(0.04, 16, 0.3), {
            "right_arm": rot(0, ZERO, 0.08, e("easeOutCubic", [-22, 0, 12]), 0.45, e("easeInOutSine", ZERO)),
            "left_arm": rot(0, ZERO, 0.08, e("easeOutCubic", [-22, 0, -12]), 0.45, e("easeInOutSine", ZERO)),
            "jaw": rot(0, ZERO, 0.06, [20, 0, 0], 0.45, ZERO),
        }), loop=False),
        # A heavy hit: it reels back a step with the arms flung up, a knee buckling, and comes back forward.
        "misc.stagger": anim(1.0, merge(rags_whip(0.05, 30, 0.6), {
            "root": pos(0, ZERO, 0.15, e("easeOutCubic", [0, 0, 2.5]), 0.5, e("easeInOutSine", [0, -1.5, 2]), 1.0, e("easeInOutSine", ZERO)),
            "body": rot(0, ZERO, 0.15, e("easeOutCubic", [-28, 10, 8]), 0.5, e("easeInOutSine", [-10, -8, -4]), 1.0, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, 0.12, e("easeOutCubic", [-30, 12, 0]), 0.5, e("easeInOutSine", [14, -10, 6]), 1.0, e("easeInOutSine", ZERO)),
            "jaw": rot(0, ZERO, 0.1, [24, 0, 0], 0.6, [10, 0, 0], 1.0, ZERO),
            "right_arm": rot(0, ZERO, 0.15, e("easeOutCubic", [-50, 0, 40]), 0.5, e("easeInOutSine", [-10, 0, 20]), 1.0, e("easeInOutSine", ZERO)),
            "left_arm": rot(0, ZERO, 0.15, e("easeOutCubic", [-45, 0, -45]), 0.5, e("easeInOutSine", [-8, 0, -22]), 1.0, e("easeInOutSine", ZERO)),
            "right_leg": rot(0, ZERO, 0.15, e("easeOutCubic", [-20, 0, 0]), 0.5, e("easeInOutSine", [-6, 0, 0]), 1.0, e("easeInOutSine", ZERO)),
            "left_leg": rot(0, ZERO, 0.15, e("easeOutCubic", [14, 0, 0]), 0.5, e("easeInOutSine", [4, 0, 0]), 1.0, e("easeInOutSine", ZERO)),
            "right_shin": rot(0, ZERO, 0.5, e("easeInOutSine", [28, 0, 0]), 1.0, e("easeInOutSine", ZERO)),
        }), loop=False),
        # It topples forward stiff like a felled post, lands on its outstretched arms, the head bouncing once, and
        # lies there with the jaw hanging; the rags settle.
        "misc.death": anim(2.0, merge(rags_whip(0.95, 25, 0.6), {
            "root": {"position": keys(0, ZERO, 0.3, e("easeOutCubic", [0, 0, 1.2]), 0.95, e("easeInQuad", [0, -1.5, 0]), 1.1, e("easeOutBounce", [0, -1, 0]), 2.0, [0, -1, 0]),
                     "rotation": keys(0, ZERO, 0.3, ZERO, 0.95, e("easeInQuad", [86, 0, 4]), 1.1, e("easeOutCubic", [88, 0, 4]), 2.0, [88, 0, 4])},
            "body": rot(0, ZERO, 0.3, e("easeOutCubic", [-14, 0, 0]), 0.95, [2, 0, 0], 1.15, e("easeOutCubic", fallen["body"]), 2.0, fallen["body"]),
            "head": rot(0, ZERO, 0.3, e("easeOutCubic", [-18, 0, 0]), 0.95, [-8, 0, 0], 1.05, e("easeOutCubic", [-30, 20, 8]), 1.3, e("easeOutBounce", fallen["head"]), 2.0, fallen["head"]),
            "jaw": rot(0, ZERO, 0.3, [20, 0, 0], 0.95, [8, 0, 0], 1.2, fallen["jaw"], 2.0, fallen["jaw"]),
            "right_arm": rot(0, arms_rest[0], 0.3, e("easeOutCubic", [-95, 0, 10]), 0.95, [-88, 0, 6], 1.15, e("easeOutCubic", fallen["right_arm"]), 2.0, fallen["right_arm"]),
            "left_arm": rot(0, arms_rest[1], 0.3, e("easeOutCubic", [-95, 0, -10]), 0.95, [-88, 0, -6], 1.15, e("easeOutCubic", fallen["left_arm"]), 2.0, fallen["left_arm"]),
            "right_forearm": rot(0, ZERO, 0.95, ZERO, 1.15, fallen["right_forearm"], 2.0, fallen["right_forearm"]),
            "left_forearm": rot(0, ZERO, 0.95, ZERO, 1.15, fallen["left_forearm"], 2.0, fallen["left_forearm"]),
            "right_hand": rot(0, ZERO, 0.95, ZERO, 1.15, fallen["right_hand"], 2.0, fallen["right_hand"]),
            "left_hand": rot(0, ZERO, 0.95, ZERO, 1.15, fallen["left_hand"], 2.0, fallen["left_hand"]),
            "right_leg": rot(0, ZERO, 0.3, [-6, 0, 0], 0.95, ZERO, 1.2, fallen["right_leg"], 2.0, fallen["right_leg"]),
            "left_leg": rot(0, ZERO, 0.3, [4, 0, 0], 0.95, ZERO, 1.2, fallen["left_leg"], 2.0, fallen["left_leg"]),
            "right_shin": rot(0, ZERO, 0.95, ZERO, 1.2, fallen["right_shin"], 2.0, fallen["right_shin"]),
            "left_shin": rot(0, ZERO, 0.95, ZERO, 1.2, fallen["left_shin"], 2.0, fallen["left_shin"]),
        }), loop=False),
    }
    write_model("ashen_zombie", bones, animations, [2, 2.5])


# --- Ghost ----------------------------------------------------------------------------------------------------------
# What the full moon sends after a player with no curse (DESIGN §3.3): a pale spectre in a torn burial shroud that
# thins into a trailing wisp, a hood with only dark and two cold eyes inside, long sleeves with bony fingers reaching
# out, a broken shackle and two links of chain on one wrist. 42 cubes on 20 bones: chamfered torso and skull, an
# octagonal hood with its fall on a two-bone chain, the shroud trailing in three segments, the sleeves' falls on
# chains, forearms and bone hands. Drawn translucent, at least half lit (NightMobClient): the shroud fades from
# almost solid at the shoulders to faint at the wisp's tip, and keeps a faint glow of its own. misc.hurt is a
# flinch and a shiver; misc.death a dissolve: it rises, turns and shrinks to nothing over 32 ticks (GhostEntity).

def ghost():
    shroud = Material(GHOST_PALE, base=3.4, noise=0.5, grain="v", grad=0.8, top=0.8, edge=0.3)
    dark = Material(GHOST_DARK, base=1.0, noise=0.4)
    bone_mat = Material(IVORY, base=3.8, noise=0.4, grad=1.0)
    iron = Material(HOLLOW_IRON, base=3.4, noise=0.5, speck=0.08, bevel=0.5)
    drape = cloth(3, -18)
    solid = fade(215, 175, 70, 55)

    def body_deco(face, rng):
        drape(face, rng)
        if face.name == "back":
            holes(face, rng, 1)

    def mantle(face, rng):
        drape(face, rng)
        if face.side():
            ragged_edge(face, rng, "bottom", 2, 0.55)
        if face.name == "bottom":
            clear_all(face)

    def segment(i):
        def fn(face, rng):
            drape(face, rng)
            if face.side():
                ragged_edge(face, rng, "bottom", 2, 0.5 + 0.1 * i)
                ragged_edge(face, rng, "left", 1, 0.3)
                ragged_edge(face, rng, "right", 1, 0.3)
            if face.name == "top":
                clear_all(face)
            fade(185 - 40 * i, 130 - 40 * i, 55 - 12 * i, 35 - 10 * i)(face, rng)
        return fn

    def face_front(face, rng):
        # Only dark inside the hood: two cold eyes, a faint open mouth below them.
        face.rect(0, 0, face.w, face.h, GHOST_DARK[0])
        for x in range(face.w):
            if (x + 1) % 3 == 0:
                face.set(x, 0, GHOST_DARK[1])
        face.lit(1, 2, GHOST_EYE, 1.0)
        face.lit(2, 2, GHOST_EYE, 1.0)
        face.lit(face.w - 3, 2, GHOST_EYE, 1.0)
        face.lit(face.w - 2, 2, GHOST_EYE, 1.0)
        face.lit(1, 3, COLD_DIM, 0.3)
        face.lit(face.w - 2, 3, COLD_DIM, 0.3)
        face.lit(face.cx - 1, 5, COLD_DIM, 0.45)
        face.lit(face.cx, 5, COLD_DIM, 0.45)
        face.set(face.cx - 1, 6, GHOST_DARK[1])
        face.set(face.cx, 6, GHOST_DARK[1])

    def hood_deco(face, rng):
        drape(face, rng)
        if face.name == "front":
            open_front(face, 1, 1)
        elif face.name == "bottom":
            clear_all(face)
        elif face.side():
            ragged_edge(face, rng, "bottom", 1, 0.5)

    def sleeve(face, rng):
        drape(face, rng)
        if face.side():
            ragged_edge(face, rng, "bottom", 2, 0.5)
        if face.name == "bottom":
            clear_all(face)

    def rag(face, rng):
        if face.name in ("front", "back"):
            drape(face, rng)
            ragged_edge(face, rng, "bottom", 2, 0.6)
            holes(face, rng, 1)
        else:
            clear_all(face)

    def finger_deco(face, rng):
        if face.side():
            face.vline(0, 0, face.h, IVORY[2])

    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 14, 0], "root", [10, 0, 0],
             chamfered([-4, 14, -2], [8, 11, 4], shroud, {"all": compose(body_deco, solid)}, c=0.6)
             + [Box([-4.6, 20, -2.6], [9.2, 5, 5.2], shroud.but(base=3.8), {"all": compose(mantle, fade(225, 200, 70))}, inflate=0.2),
                Box([-4.9, 18.6, -2.9], [9.8, 1.8, 5.8], shroud.but(base=3.6), {"all": compose(mantle, fade(220, 190, 65))}),
                Box([-3.2, 23.8, -2.2], [6.4, 1.6, 4.4], shroud.but(base=3.0), {"all": compose(drape, fade(225))}),
                Box([-4.3, 13.5, -2.3], [8.6, 1.4, 4.6], Material(LINEN, base=2.2, noise=0.5, grain="h"), {"all": fade(215)})]),
    ]
    # The shroud trailing below in three segments, each thinner and fainter.
    bones += chain_bones("shroud", "body", [0, 14, 0], 3, 4.2, shroud, None, thickness=6, rotation=[-10, 0, 0],
                         box=lambda i, o, sz: [Box([-3.2 + 0.7 * i, o[1], -1.6 + 0.4 * i], [6.4 - 1.4 * i, 4.4, 3.2 - 0.8 * i],
                                                   shroud.but(base=3.0 - 0.3 * i), {"all": segment(i)})])
    bones += [
        Bone("head", [0, 25, -1], "root", None,
             chamfered([-3.5, 25, -4.5], [7, 7, 7], dark, {"all": fade(235)}, c=0.7)
             + [Box([-3.5, 25, -4.9], [7, 7, 0.6], dark, {"front": face_front, "all": fade(235)})]),
        Bone("hood", [0, 25, -1], "head", None,
             octagon([-4, 25.5, -5], [8, 7.5, 8], shroud.but(base=3.6), {"all": compose(hood_deco, fade(225, 200, 70))}, shrink=0.78)
             + [Box([-1, 32.5, -2], [2, 2, 4], shroud.but(base=4.0), {"all": fade(220, 200, 60)}, rotation=[25, 0, 0], pivot=[0, 32.5, 1.5]),
                Box([-4.2, 31.2, -5.4], [8.4, 1.6, 2], shroud.but(base=4.2), {"all": compose(drape, fade(230))}, rotation=[10, 0, 0], pivot=[0, 32, -4])]),
    ]
    bones += hanging("hood_fall", "hood", [0, 26, 3.1], 2, 3.6, 6.4, shroud.but(base=3.2), {"all": compose(rag, fade(210, 150, 60, 40))}, z_step=0.2, taper=1.0)
    upper = Box([-7, 18, -2], [3, 6.5, 3], shroud, {"all": compose(sleeve, fade(210, 170, 60, 45))}, inflate=0.15)
    cuff = Box([-7.7, 15, -2.7], [4.4, 3.6, 4.4], shroud.but(base=3.2), {"all": compose(sleeve, fade(205, 150, 55, 40))})
    fore = Box([-6.4, 12, -1.4], [1.8, 5, 1.8], bone_mat.but(base=3.2), {"all": fade(235)})
    palm = Box([-6.7, 10.3, -1.7], [2.4, 1.7, 2.4], bone_mat, {"all": fade(235)})
    digits = fingers(-6.6, 10.3, -1.6, 3, (0.6, 3.2, 0.6), bone_mat.but(base=4.2), spread=0.3, curl=18, decals={"all": compose(finger_deco, fade(235))})
    shackle = Box([-7.1, 11.6, -2.1], [3.2, 1.4, 3.2], iron, {"all": lambda f, r: rust_spots(f, r, 1)})
    bones += [
        Bone("right_arm", [-5, 24, -1], "body", [-62, 0, 0], [upper, cuff]),
        Bone("right_forearm", [-5.5, 18, -0.5], "right_arm", [-6, 0, 0], [fore, shackle]),
        Bone("right_hand", [-5.5, 12, -0.5], "right_forearm", [-4, 0, 0], [palm] + digits),
        Bone("left_arm", [5, 24, -1], "body", [-62, 0, 0], [mirror_box(upper), mirror_box(cuff)]),
        Bone("left_forearm", [5.5, 18, -0.5], "left_arm", [-6, 0, 0], [mirror_box(fore)]),
        Bone("left_hand", [5.5, 12, -0.5], "left_forearm", [-4, 0, 0], [mirror_box(palm)] + [mirror_box(d) for d in digits]),
    ]
    bones += chain_bones("chain", "right_forearm", [-5.5, 11.6, 0.9], 2, 1.6, iron, None, thickness=0.8, rotation=[-50, 0, 0],
                         box=lambda i, o, sz: [Box([o[0], o[1], o[2]], [0.8, 1.6, 0.8], iron.but(base=3.0 + i * 0.4))])
    bones += hanging("sleeve_right", "right_arm", [-5.5, 15.3, 0.6], 2, 2.6, 4, shroud.but(base=3.0), {"all": compose(rag, fade(200, 140, 55, 40))}, z_step=0.2, taper=0.6)
    bones += hanging("sleeve_left", "left_arm", [5.5, 15.3, 0.6], 2, 2.6, 4, shroud.but(base=3.0), {"all": compose(rag, fade(200, 140, 55, 40))}, z_step=0.2, taper=0.6)
    tail = chain_names("shroud", 3)
    loose = [chain_names("hood_fall", 2), chain_names("sleeve_right", 2), chain_names("sleeve_left", 2), chain_names("chain", 2)]

    def loose_drift(length, amp, phase=0.0):
        out = {}
        for i, names in enumerate(loose):
            out = merge(out, drift(names, length, amp * (1.4 if i == 3 else 1), phase=phase + i * 0.2))
        return out

    def loose_whip(t0, amp, settle_t=0.4):
        out = {}
        for i, names in enumerate(loose):
            out = merge(out, whip(names, t0 + 0.02 * i, amp * (1.3 if i == 3 else 1), settle=settle_t))
        return out

    def hover(length, bob, sway_deg, stream):
        return anim(length, merge(drift(tail, length, sway_deg, lag=0.14, gain=1.25, base=[stream, 0, 0]), loose_drift(length, 4 + stream * 0.2), {
            "root": pos(0, ZERO, length / 2, e("easeInOutSine", [0, bob, 0]), length, e("easeInOutSine", ZERO)),
            "body": rot(0, ZERO, length / 2, e("easeInOutSine", [-2 + stream * 0.3, 0, 0]), length, e("easeInOutSine", ZERO)),
            "right_arm": rot(0, ZERO, length / 2, e("easeInOutSine", [-8, 0, 4]), length, e("easeInOutSine", ZERO)),
            "left_arm": rot(0, [-5, 0, -2], length / 2, e("easeInOutSine", [2, 0, -5]), length, e("easeInOutSine", [-5, 0, -2])),
            "right_hand": rot(0, ZERO, length / 2, [-12, 0, 0], length, ZERO),
            "left_hand": rot(0, [-10, 0, 0], length / 2, ZERO, length, [-10, 0, 0]),
            "head": rot(0, ZERO, length * 0.3, e("easeInOutSine", [4, 7, 3]), length * 0.7, e("easeInOutSine", [2, -6, -2]), length, e("easeInOutSine", ZERO)),
        }))

    rising = {"body": [-30, 0, 0], "head": [-35, 0, 0], "right_arm": [-150, 0, 40], "left_arm": [-150, 0, -40], "right_hand": [-30, 0, 0], "left_hand": [-30, 0, 0]}
    animations = {
        "misc.idle": hover(3.0, 1, 8, 0),
        "move.fly": hover(1.5, 1.5, 16, 30),
        # The arms drawn back over the head, then a lunge with both claws down through the target; the shroud whips.
        "attack.swing": anim(0.5, merge(loose_whip(0.26, 25, 0.24), whip(tail, 0.26, 30, settle=0.24), {
            "right_arm": rot(0, ZERO, 0.15, e("easeInOutSine", [-150, 0, 20]), 0.26, e("easeOutBack", [-30, 0, -12]), 0.5, e("easeInOutSine", ZERO)),
            "left_arm": rot(0, ZERO, 0.15, e("easeInOutSine", [-150, 0, -20]), 0.26, e("easeOutBack", [-30, 0, 12]), 0.5, e("easeInOutSine", ZERO)),
            "right_hand": rot(0, ZERO, 0.15, [-30, 0, 0], 0.26, [25, 0, 0], 0.5, ZERO),
            "left_hand": rot(0, ZERO, 0.15, [-30, 0, 0], 0.26, [25, 0, 0], 0.5, ZERO),
            "head": rot(0, ZERO, 0.15, [-14, 0, 0], 0.26, [16, 0, 0], 0.5, ZERO),
            "body": rot(0, ZERO, 0.15, e("easeInOutSine", [-8, 0, 0]), 0.26, e("easeOutBack", [14, 0, 0]), 0.5, e("easeInOutSine", ZERO)),
            "root": pos(0, ZERO, 0.15, [0, 0.8, 1.2], 0.26, e("easeOutCubic", [0, -0.6, -2.5]), 0.5, e("easeInOutSine", ZERO)),
        }), loop=False),
        # A hit that lands goes through it with a shiver: it jerks back and wavers.
        "misc.hurt": anim(0.4, merge(flinch(0.4, amount=1.0), loose_whip(0.04, 14, 0.3), whip(tail, 0.04, 16, settle=0.3), {
            "body": {"rotation": keys(0, ZERO, 0.08, e("easeOutCubic", [-14, 6, 5]), 0.2, [-6, 3, 2], 0.4, e("easeInOutSine", ZERO)),
                     "scale": keys(0, [1, 1, 1], 0.06, [1.08, 0.94, 1.08], 0.14, [0.94, 1.06, 0.94], 0.22, [1.04, 0.97, 1.04], 0.4, [1, 1, 1])},
        }), loop=False),
        # The dissolve: it lifts, the arms rising and spreading, the head thrown back, turning slowly as the shroud
        # streams down and the whole of it shrinks away to a point.
        "misc.death": anim(1.6, merge(settle(tail, 0.1, -35, length=1.0), loose_whip(0.1, 30, 0.8), {
            "root": {"position": keys(0, ZERO, 0.3, e("easeOutCubic", [0, -1.5, 0]), 1.6, e("easeInSine", [0, 9, 0])),
                     "rotation": keys(0, ZERO, 0.3, ZERO, 1.6, e("easeInSine", [0, 150, 0])),
                     "scale": keys(0, [1, 1, 1], 0.3, [1.06, 0.96, 1.06], 0.8, e("easeInOutSine", [0.9, 1.15, 0.9]), 1.6, e("easeInCubic", [0.04, 0.04, 0.04]))},
            "body": rot(0, ZERO, 0.3, e("easeOutCubic", [12, 0, 0]), 1.2, e("easeInOutSine", rising["body"]), 1.6, rising["body"]),
            "head": rot(0, ZERO, 0.3, e("easeOutCubic", [10, 0, 0]), 1.2, e("easeInOutSine", rising["head"]), 1.6, rising["head"]),
            "right_arm": rot(0, ZERO, 0.3, e("easeOutCubic", [-30, 0, 10]), 1.2, e("easeInOutSine", rising["right_arm"]), 1.6, rising["right_arm"]),
            "left_arm": rot(0, ZERO, 0.3, e("easeOutCubic", [-30, 0, -10]), 1.2, e("easeInOutSine", rising["left_arm"]), 1.6, rising["left_arm"]),
            "right_hand": rot(0, ZERO, 1.2, rising["right_hand"], 1.6, rising["right_hand"]),
            "left_hand": rot(0, ZERO, 1.2, rising["left_hand"], 1.6, rising["left_hand"]),
        }), loop=False),
    }
    write_model("ghost", bones, animations, [2, 3])


# --- Vampire --------------------------------------------------------------------------------------------------------
# The blood new moon's guest, friendly until struck (DESIGN §3.3): a tall pale noble with slicked black hair and a
# widow's peak, red eyes, a thin smile over two fangs, a burgundy brocade doublet with gold buttons, a lace jabot and
# cuffs, a crimson sash with its tail, black breeches and riding boots with turned-down tops, and a high-collared
# black cloak lined with crimson falling in three pieces, with the doublet's coat tails in two pieces each. 52 cubes
# on 20 bones: chamfered doublet and skull, a face plate, hair cap, peak, strands and ears, forearms, gloved hands
# with fingers, shins and boots. misc.transform (2.5 s = VampireEntity.TURN_TICKS, held) lifts it off the ground with
# the cloak flaring; misc.hurt is the jolt of the blow that provokes it; misc.death (a /kill: it cannot die
# otherwise) a collapse to the knees and over (VampireEntity keeps the body 40 ticks).

def vampire():
    skin = Material(VAMP_SKIN, base=3.2, noise=0.25, grad=0.6, top=0.5)
    doublet = Material(V_BURGUNDY, base=3.0, noise=0.4, grad=0.8, edge=0.4, bevel=0.3)
    black = Material(V_BLACK, base=3.0, noise=0.45, grain="v", grad=0.8)
    lining = Material(V_CRIMSON, base=3.0, noise=0.45, grain="v", grad=0.6)
    lace = Material(V_LACE, base=3.4, noise=0.4)
    hair = Material(V_HAIR, base=2.6, noise=0.5, top=0.6)
    gold = Material(GOLD_TRIM, base=4.0, noise=0.4, bevel=0.6)

    def brocade(face, rng):
        if face.side():
            # The weave: a lattice of lighter thread; the fold lines of the cloth under it.
            cloth(4, -12)(face, rng)
            for y in range(1, face.h - 1, 3):
                for x in range((y // 3) % 2, face.w, 2):
                    face.set(x, y, V_BURGUNDY[5])

    def front_panel(face, rng):
        brocade(face, rng)
        face.vline(face.cx - 1, 0, face.h, V_BURGUNDY[0])
        for y in range(1, face.h - 1, 2):
            face.set(face.cx, y, GOLD_TRIM[4])
            face.set(face.cx + 1, y, GOLD_TRIM[2])

    def sash(face, rng):
        if face.side():
            face.rect(0, 0, face.w, face.h, V_CRIMSON[3])
            face.hline(0, 0, face.w, V_CRIMSON[4])
            face.hline(0, face.h - 1, face.w, V_CRIMSON[1])
            if face.name == "left":
                face.rect(1, 0, 2, face.h, V_CRIMSON[5])   # the knot

    def face_front(face, rng):
        # The hairline with the widow's peak, brows, red eyes under heavy lids, a straight nose, the thin smile
        # with two fangs.
        face.rect(0, 0, face.w, 2, V_HAIR[1])
        face.set(face.cx - 1, 2, V_HAIR[1])
        face.set(face.cx, 2, V_HAIR[1])
        face.hline(1, 2, 2, V_HAIR[3])
        face.hline(face.w - 3, 2, 2, V_HAIR[3])
        for ex in (1, face.w - 3):
            face.set(ex, 3, VAMP_SKIN[6])
            face.set(ex + 1, 3, VAMP_SKIN[6])
            face.lit(ex + (1 if ex == 1 else 0), 3, BLOOD, 0.8)
            face.hline(ex, 4, 2, VAMP_SKIN[2])
        face.set(face.cx, 4, VAMP_SKIN[2])
        face.set(face.cx, 5, VAMP_SKIN[1])
        face.set(face.cx - 1, 5, VAMP_SKIN[4])
        face.hline(face.cx - 2, 6, 4, V_CRIMSON[3])
        face.set(face.cx - 2, 7, FANG)
        face.set(face.cx + 1, 7, FANG)
        face.set(0, 5, VAMP_SKIN[1])
        face.set(face.w - 1, 5, VAMP_SKIN[1])

    def hair_cap(face, rng):
        if face.name in ("front", "bottom"):
            clear_all(face)
        elif face.name in ("left", "right"):
            # Swept back over the ears; the front corner (the cheek) stays bare.
            for y in range(1, face.h):
                for k in range(3 if y > 1 else 2):
                    face.clear(k if face.name == "right" else face.w - 1 - k, y)
            for x in range(0, face.w, 2):
                face.vline(x, 0, face.h, V_HAIR[3])
        elif face.name == "top":
            for y in range(1, face.h, 2):
                face.vline(y, 0, face.h, V_HAIR[3])
        else:
            for x in range(0, face.w, 2):
                face.vline(x, 0, face.h, V_HAIR[3])

    def cloak_deco(face, rng):
        if face.name == "front":          # the lining, seen from behind it
            face.rect(0, 0, face.w, face.h, V_CRIMSON[2])
            cloth(3, -16)(face, rng)
        elif face.name == "back":
            cloth(3, -14)(face, rng)
        else:
            clear_all(face)
            return
        face.hline(0, face.h - 1, face.w, GOLD_TRIM[2])

    def tail_deco(face, rng):
        if face.name in ("front", "back"):
            brocade(face, rng)
            trim(face, GOLD_TRIM[1], ("left", "right", "bottom"))
        else:
            clear_all(face)

    def collar_deco(face, rng):
        if face.name == "front":
            face.rect(0, 0, face.w, face.h, V_CRIMSON[3])
            for y in range(face.h):
                face.set(0, y, V_BLACK[4])
                face.set(face.w - 1, y, V_BLACK[4])
        elif face.side():
            cloth(2, -14)(face, rng)

    def jabot(face, rng):
        for y in range(face.h):
            if y % 2:
                face.hline(0, y, face.w, V_LACE[2])
            else:
                for x in range(1, face.w, 2):
                    face.set(x, y, V_LACE[6])
        if face.name == "front":
            face.set(face.cx, 1, BLOOD_DIM)              # a ruby pin

    def cuff(face, rng):
        if face.side():
            for x in range(face.w):
                face.set(x, face.h - 1, V_LACE[2] if x % 2 else V_LACE[5])
            ragged_edge(face, rng, "bottom", 1, 0.3)
        else:
            clear_all(face)

    def boot(face, rng):
        if face.side():
            cloth(3, -12)(face, rng)
            face.hline(0, face.h - 1, face.w, V_BLACK[0])
            if face.name == "front":
                face.set(face.cx, 1, V_SILVER[5])       # the spur strap's buckle

    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 24, 0], "root", None,
             chamfered([-4, 12, -2], [8, 12, 4], doublet, {"all": brocade}, c=0.6)
             + [Box([-3, 14.5, -2.5], [6, 9, 0.6], doublet, {"front": front_panel}),
                Box([-4.3, 12.4, -2.3], [8.6, 2.4, 4.6], Material(V_CRIMSON, base=3.0, noise=0.4, grain="h"), {"all": sash}),
                Box([3.6, 7.5, 0.2], [1.2, 5.2, 0.6], Material(V_CRIMSON, base=3.0, noise=0.4, grain="v"),
                    {"all": lambda f, r: ragged_edge(f, r, "bottom", 1, 0.5) if f.side() else None}, rotation=[0, 0, -6], pivot=[4.2, 12.6, 0.5]),
                Box([-1.5, 20, -2.7], [3, 4, 0.8], lace, {"all": jabot}),
                Box([-4.4, 23.2, -2.4], [8.8, 1.4, 4.8], black, {"all": lambda f, r: cloth(2, -12)(f, r)}),
                Box([-5.5, 24, 1.6], [11, 7, 1], black, {"all": collar_deco}, rotation=[-18, 0, 0], pivot=[0, 24, 2]),
                Box([-5.5, 24, -2.4], [1, 5.5, 4], black, {"all": collar_deco}, rotation=[0, 0, 12], pivot=[-5, 24, -0.4]),
                Box([4.5, 24, -2.4], [1, 5.5, 4], black, {"all": collar_deco}, rotation=[0, 0, -12], pivot=[5, 24, -0.4])]),
    ]
    bones += chain_bones("cape", "body", [0, 26, 2.6], 3, 7, black, None, thickness=1,
                         box=lambda i, o, sz: [Box([-5.5 + 0.3 * i, o[1], 2.6 + 0.25 * i], [11 - 0.6 * i, 7.3, 0], black.but(base=3.0 - 0.2 * i), {"all": cloak_deco})])
    for side, sx in (("right", -1), ("left", 1)):
        bones += chain_bones("coat_" + side, "body", [sx * 2.4, 12, 2.3], 2, 4.5, doublet, None, thickness=1,
                             box=lambda i, o, sz, sx=sx: [Box([min(sx * 0.4, sx * 4.4) + 0.2 * i, o[1], 2.3 + 0.2 * i], [4 - 0.4 * i, 4.8, 0],
                                                              doublet.but(base=2.6), {"all": tail_deco})])
    ear = Box([-4.6, 27, -1.5], [0.8, 2.2, 1.6], skin.but(base=2.8))
    bones += [
        Bone("head", [0, 24, 0], "root", None,
             chamfered([-4, 24, -4], [8, 8, 8], skin, None, c=0.8)
             + [Box([-4, 24, -4.4], [8, 8, 0.6], skin, {"front": face_front}),
                Box([-4.3, 27, -4.3], [8.6, 5.3, 8.6], hair, {"all": hair_cap}),
                Box([-1.2, 30.8, -4.6], [2.4, 1.4, 1.2], hair.but(base=2.2)),                       # the widow's peak
                Box([-4.5, 25.5, 1.5], [1.0, 5, 3], hair.but(base=2.2), {"all": lambda f, r: cloth(2, -10)(f, r)}),
                Box([3.5, 25.5, 1.5], [1.0, 5, 3], hair.but(base=2.2), {"all": lambda f, r: cloth(2, -10)(f, r)}),
                Box([-4.1, 23.6, 2.4], [8.2, 3.8, 2.0], hair.but(base=2.0), {"all": lambda f, r: ragged_edge(f, r, "bottom", 1, 0.5) if f.side() else None}),
                ear, mirror_box(ear)]),
    ]
    upper = Box([-7, 16.5, -1.5], [3, 6, 3], doublet, {"all": brocade})
    roll = Box([-7.4, 20.6, -1.9], [3.8, 2, 3.8], doublet.but(base=3.4), {"all": brocade})
    fore = Box([-6.8, 11.5, -1.3], [2.6, 5.5, 2.6], doublet.but(base=2.8), {"all": brocade})
    cuff_box = Box([-7.3, 10.8, -1.8], [3.6, 2.2, 3.6], lace, {"all": cuff})
    glove = Box([-6.7, 9.2, -1.2], [2.4, 2, 2.4], Material(V_BLACK, base=3.6, noise=0.4, bevel=0.4))
    digits = fingers(-6.6, 9.2, -1.1, 3, (0.6, 1.9, 0.6), Material(V_BLACK, base=3.8, noise=0.3), spread=0.3, curl=14)
    bones += [
        Bone("right_arm", [-5, 22, 0], "root", [-4, 0, 3], [upper, roll]),
        Bone("right_forearm", [-5.5, 16.5, 0], "right_arm", [-12, 0, 0], [fore, cuff_box]),
        Bone("right_hand", [-5.5, 11, 0], "right_forearm", [-6, 0, 0], [glove] + digits),
        Bone("left_arm", [5, 22, 0], "root", [-4, 0, -3], [mirror_box(upper), mirror_box(roll)]),
        Bone("left_forearm", [5.5, 16.5, 0], "left_arm", [-12, 0, 0], [mirror_box(fore), mirror_box(cuff_box)]),
        Bone("left_hand", [5.5, 11, 0], "left_forearm", [-6, 0, 0], [mirror_box(glove)] + [mirror_box(d) for d in digits]),
    ]
    thigh = Box([-3.9, 6, -2], [3.8, 6.5, 3.8], black.but(base=2.6), {"all": lambda f, r: cloth(3, -12)(f, r)})
    shin = Box([-3.8, 1.5, -1.9], [3.6, 5, 3.6], black.but(base=2.2), {"all": boot})
    top = Box([-4.2, 5.2, -2.3], [4.4, 1.8, 4.4], black.but(base=3.6), {"all": lambda f, r: cloth(2, -10)(f, r)})
    foot = Box([-3.9, 0, -3.2], [3.8, 1.6, 5.2], black.but(base=2.0), {"all": lambda f, r: trim(f, V_BLACK[0], ("bottom",)) if f.side() else None})
    bones += [
        Bone("right_leg", [-2, 12, 0], "root", None, [thigh]),
        Bone("right_shin", [-2, 6, 0], "right_leg", None, [shin, top, foot]),
        Bone("left_leg", [2, 12, 0], "root", None, [mirror_box(thigh)]),
        Bone("left_shin", [2, 6, 0], "left_leg", None, [mirror_box(shin), mirror_box(top), mirror_box(foot)]),
    ]
    loose = [chain_names("cape", 3), chain_names("coat_right", 2), chain_names("coat_left", 2)]

    def loose_drift(length, amp, phase=0.0, base=None):
        out = {}
        for i, names in enumerate(loose):
            out = merge(out, drift(names, length, amp * (1.3 if i == 0 else 1), phase=phase + i * 0.2, base=base if i == 0 else None))
        return out

    def loose_whip(t0, amp, settle_t=0.5):
        out = {}
        for i, names in enumerate(loose):
            out = merge(out, whip(names, t0 + 0.03 * i, amp * (1.3 if i == 0 else 0.9), settle=settle_t))
        return out

    arms_rest = [[-4, 0, 3], [-4, 0, -3]]
    kneel = {"body": [18, 0, 0], "head": [26, 0, 0], "right_arm": [-6, 0, 10], "left_arm": [-6, 0, -10], "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0],
             "right_shin": [88, 0, 0], "left_shin": [88, 0, 0]}
    fallen = {"body": [12, 0, 0], "head": [10, -25, 0], "right_arm": [-25, 0, 24], "left_arm": [-30, 0, -40], "right_forearm": [-20, 0, 0], "left_forearm": [-35, 0, 0],
              "right_leg": [-8, 0, 0], "left_leg": [10, 0, 0], "right_shin": [55, 0, 0], "left_shin": [70, 0, 0]}
    animations = {
        # Composed and still: a slow breath, a slow regard of the players, the fingers flexing, the cloak barely moving.
        "misc.idle": anim(4.0, merge(breathe(4.0, chest=1.5, head=0, shoulders=0.6), loose_drift(4.0, 2.5), {
            "head": rot(0, [-3, 0, 0], 1.4, e("easeInOutSine", [-1, 8, 1]), 2.6, e("easeInOutSine", [-2, -6, -1]), 4.0, e("easeInOutSine", [-3, 0, 0])),
            "right_hand": rot(0, ZERO, 2.0, e("easeInOutSine", [-10, 0, 0]), 4.0, e("easeInOutSine", ZERO)),
            "left_hand": rot(0, [-6, 0, 0], 2.0, e("easeInOutSine", ZERO), 4.0, e("easeInOutSine", [-6, 0, 0])),
            "right_forearm": rot(0, ZERO, 2.0, e("easeInOutSine", [-3, 0, 0]), 4.0, e("easeInOutSine", ZERO)),
        })),
        # A measured stroll (speed 0.22): a long stride, the arms barely swinging, the cloak and tails lifting behind.
        "move.walk": anim(1.4, merge(walk(1.4, stride=24, swing=12, bob=0.35, lean=2, twist=4, roll=2, arm_rest=arms_rest), loose_drift(1.4, 8, 0.1, base=[8, 0, 0]), {
            "right_shin": rot(0, [4, 0, 0], 0.35, [28, 0, 0], 0.7, e("easeInOutSine", [2, 0, 0]), 1.4, e("easeInOutSine", [4, 0, 0])),
            "left_shin": rot(0, [2, 0, 0], 0.7, e("easeInOutSine", [4, 0, 0]), 1.05, [28, 0, 0], 1.4, e("easeInOutSine", [2, 0, 0])),
            "right_forearm": rot(0, [-14, 0, 0], 1.4, [-14, 0, 0]),
            "left_forearm": rot(0, [-14, 0, 0], 1.4, [-14, 0, 0]),
        })),
        # 2.5 s = VampireEntity.TURN_TICKS: it sinks, then rises off the ground with the arms thrown wide and the
        # head back, the cloak and the coat tails flaring out behind it; held.
        "misc.transform": anim(2.5, merge({
            "root": pos(0, ZERO, 0.4, e("easeInOutSine", [0, -0.8, 0]), 1.5, e("easeInOutSine", [0, 3, 0]), 2.5, e("easeOutSine", [0, 5, 0])),
            "body": rot(0, ZERO, 0.4, e("easeInOutSine", [12, 0, 0]), 1.2, e("easeInOutSine", [-8, 0, 0]), 2.5, [-4, 0, 0]),
            "head": rot(0, ZERO, 0.4, e("easeInOutSine", [20, 0, 0]), 1.2, e("easeInOutSine", [-35, 0, 0]), 2.5, [-20, 0, 0]),
            "right_arm": rot(0, arms_rest[0], 0.4, e("easeInOutSine", [-20, 0, 10]), 1.2, e("easeOutBack", [0, 0, 110]), 2.5, [-30, 0, 130]),
            "left_arm": rot(0, arms_rest[1], 0.4, e("easeInOutSine", [-20, 0, -10]), 1.2, e("easeOutBack", [0, 0, -110]), 2.5, [-30, 0, -130]),
            "right_forearm": rot(0, ZERO, 0.4, [-30, 0, 0], 1.2, e("easeOutCubic", [-10, 0, 0]), 2.5, ZERO),
            "left_forearm": rot(0, ZERO, 0.4, [-30, 0, 0], 1.2, e("easeOutCubic", [-10, 0, 0]), 2.5, ZERO),
            "right_hand": rot(0, ZERO, 0.4, [30, 0, 0], 1.2, [-40, 0, 0], 2.5, [-40, 0, 0]),
            "left_hand": rot(0, ZERO, 0.4, [30, 0, 0], 1.2, [-40, 0, 0], 2.5, [-40, 0, 0]),
            "right_leg": rot(0, ZERO, 0.4, [-6, 0, 0], 2.5, [10, 0, 6]),
            "left_leg": rot(0, ZERO, 0.4, [-6, 0, 0], 2.5, [10, 0, -6]),
            "right_shin": rot(0, ZERO, 0.4, [12, 0, 0], 2.5, [8, 0, 0]),
            "left_shin": rot(0, ZERO, 0.4, [12, 0, 0], 2.5, [8, 0, 0]),
            "cape_1": rot(0, ZERO, 0.5, [-5, 0, 0], 1.5, e("easeInOutSine", [40, 0, 0]), 2.0, e("easeOutBack", [62, 0, 4]), 2.5, [70, 0, 0]),
            "cape_2": rot(0, ZERO, 0.6, [-6, 0, 0], 1.6, e("easeInOutSine", [20, 0, 0]), 2.1, e("easeOutBack", [30, 0, 3]), 2.5, [25, 0, 0]),
            "cape_3": rot(0, ZERO, 0.7, [-6, 0, 0], 1.7, e("easeInOutSine", [15, 0, 0]), 2.2, e("easeOutBack", [25, 0, -3]), 2.5, [15, 0, 0]),
            "coat_right_1": rot(0, ZERO, 0.5, [-4, 0, 0], 1.6, e("easeInOutSine", [35, 0, 8]), 2.5, [50, 0, 10]),
            "coat_left_1": rot(0, ZERO, 0.5, [-4, 0, 0], 1.6, e("easeInOutSine", [35, 0, -8]), 2.5, [50, 0, -10]),
            "coat_right_2": rot(0, ZERO, 1.7, e("easeInOutSine", [20, 0, 0]), 2.5, [20, 0, 0]),
            "coat_left_2": rot(0, ZERO, 1.7, e("easeInOutSine", [20, 0, 0]), 2.5, [20, 0, 0]),
        }), loop=False),
        # The blow that provokes it: a jolt, the head snapping round to the one who struck.
        "misc.hurt": anim(0.4, merge(flinch(0.4, amount=0.9), loose_whip(0.04, 16, 0.3), {
            "head": rot(0, ZERO, 0.06, e("easeOutCubic", [-10, 28, 0]), 0.3, [-4, 24, 0], 0.4, e("easeInOutSine", ZERO)),
        }), loop=False),
        # Struck down (only a /kill can): it sinks to its knees with the head bowed and folds over sideways; the
        # cloak settles over it.
        "misc.death": anim(2.0, merge(loose_whip(1.45, 22, 0.55), {
            "root": {"position": keys(0, ZERO, 0.25, e("easeOutCubic", [0, 0, 1]), 0.75, e("easeInQuad", [0, -6, 1]), 0.85, e("easeOutCubic", [0, -5.6, 1]),
                                      1.3, [0, -5.6, 1], 1.7, e("easeInQuad", [0, -2.5, 1]), 2.0, [0, -2.5, 1]),
                     "rotation": keys(0, ZERO, 1.3, ZERO, 1.7, e("easeInQuad", [0, 0, 78]), 1.85, e("easeOutCubic", [0, 0, 84]), 2.0, [0, 0, 84])},
            "body": rot(0, ZERO, 0.25, e("easeOutCubic", [-12, 0, 0]), 0.75, e("easeInOutSine", kneel["body"]), 1.3, e("easeInOutSine", [28, 0, -4]), 1.7, e("easeInQuad", fallen["body"]), 2.0, fallen["body"]),
            "head": rot(0, ZERO, 0.25, e("easeOutCubic", [-16, 0, 0]), 0.75, e("easeInOutSine", kneel["head"]), 1.3, e("easeInOutSine", [34, -6, 0]), 1.7, e("easeInQuad", fallen["head"]), 2.0, fallen["head"]),
            "right_arm": rot(0, arms_rest[0], 0.25, e("easeOutCubic", [-40, 0, 20]), 0.75, e("easeInOutSine", kneel["right_arm"]), 1.3, [0, 0, 8], 1.7, e("easeInQuad", fallen["right_arm"]), 2.0, fallen["right_arm"]),
            "left_arm": rot(0, arms_rest[1], 0.25, e("easeOutCubic", [-40, 0, -20]), 0.75, e("easeInOutSine", kneel["left_arm"]), 1.3, [0, 0, -8], 1.7, e("easeInQuad", fallen["left_arm"]), 2.0, fallen["left_arm"]),
            "right_forearm": rot(0, ZERO, 0.25, [-30, 0, 0], 0.75, kneel["right_forearm"], 1.7, fallen["right_forearm"], 2.0, fallen["right_forearm"]),
            "left_forearm": rot(0, ZERO, 0.25, [-30, 0, 0], 0.75, kneel["left_forearm"], 1.7, fallen["left_forearm"], 2.0, fallen["left_forearm"]),
            "right_leg": rot(0, ZERO, 0.75, [2, 0, 0], 1.3, [2, 0, 0], 1.7, fallen["right_leg"], 2.0, fallen["right_leg"]),
            "left_leg": rot(0, ZERO, 0.75, [2, 0, 0], 1.3, [2, 0, 0], 1.7, fallen["left_leg"], 2.0, fallen["left_leg"]),
            "right_shin": rot(0, ZERO, 0.25, [10, 0, 0], 0.75, e("easeInQuad", kneel["right_shin"]), 1.3, kneel["right_shin"], 1.7, fallen["right_shin"], 2.0, fallen["right_shin"]),
            "left_shin": rot(0, ZERO, 0.25, [6, 0, 0], 0.75, e("easeInQuad", kneel["left_shin"]), 1.3, kneel["left_shin"], 1.7, fallen["left_shin"], 2.0, fallen["left_shin"]),
        }), loop=False),
    }
    write_model("vampire", bones, animations, [2, 3])


# --- Vampire boss ---------------------------------------------------------------------------------------------------
# The noble after the turn: gaunt grey-violet skin over the ribs, a torn doublet, the cloak's high collar in rags,
# wild swept-back hair, horns curling back, pointed ears, a fanged maw and burning red eyes, long claws, and bat
# wings (a spar, three fingers, a scalloped membrane of dark crimson veined with black). PLAN 19.4: the torso, skull
# and legs are chamfered, the coat tails, a three-piece cape, the hair and a tatter of the doublet hang on their own
# bones and swing behind the motion, the forearms and clawed hands are their own bones, the jaw opens. The
# telegraphs keep their timings: attack.leap crouches until 1.0 s (LEAP_WINDUP 20), attack.nova spreads until 1.5 s
# (NOVA_WINDUP 30), misc.mist folds by 0.6 s (MIST_WINDUP 12); misc.hurt, misc.stagger, misc.death and misc.phase
# are new (VampireBossEntity plays them).

def vampire_boss():
    import math
    skin = Material(BOSS_SKIN, base=2.6, noise=0.6, grad=0.8, top=0.5, speck=0.03)
    doublet = Material(BURGUNDY, base=2.0, noise=0.6, grad=0.8)
    black = Material(NOBLE_BLACK, base=2.2, noise=0.6, grain="v")
    horn = Material(BONE, base=1.6, noise=0.5, grad=1.2, grain="h")
    claw_mat = Material(NOBLE_BLACK, base=3.4, noise=0.4)

    def torso(face, rng):
        if face.name == "front":
            # Torn open: the gaunt chest with its ribs, a blood stain glowing faintly.
            for y in range(1, face.h - 2):
                for x in range(1, face.w - 1):
                    if abs(x - (face.w - 1) / 2) < 2.6 - (y % 3 == 0):
                        face.set(x, y, BOSS_SKIN[2])
            for y in (2, 4, 6):
                face.hline(2, y, face.w - 4, BOSS_SKIN[4])
                face.hline(2, y + 1, face.w - 4, BOSS_SKIN[1])
            face.vline(face.cx - 1, 2, 6, BOSS_SKIN[0])
            face.lit(face.cx - 2, 8, BLOOD_DIM, 0.6)
            face.lit(face.cx + 1, 9, BLOOD_DIM, 0.5)
        if face.side():
            ragged_edge(face, rng, "bottom", 1, 0.3)
            face.hline(0, face.h - 3, face.w, VCRIMSON[2])
            holes(face, rng, 1)

    def collar(face, rng):
        if face.name == "front":
            for y in range(face.h):
                for x in range(2, face.w - 2):
                    face.clear(x, y)
            for y in range(face.h):
                face.set(1, y, VCRIMSON[4])
                face.set(face.w - 2, y, VCRIMSON[4])
        elif face.name == "top":
            for y in range(1, face.h):
                for x in range(1, face.w - 1):
                    face.clear(x, y)
        elif face.name == "bottom":
            clear_all(face)
        if face.side():
            ragged_edge(face, rng, "top", 2, 0.5)

    def head_front(face, rng):
        face.rect(0, 0, face.w, 2, HAIR_BLACK[1])
        face.hline(0, 2, face.w, BOSS_SKIN[0])         # heavy brow
        for ex in (1, face.w - 3):
            face.lit(ex, 3, BLOOD, 1.0)
            face.lit(ex + 1, 3, BLOOD, 1.0)
            face.lit(ex + (1 if ex == 1 else 0), 3, hexc("ff9a7a"), 1.0)
            face.hline(ex, 4, 2, BOSS_SKIN[1])
        face.set(face.cx - 1, 4, BOSS_SKIN[0])         # bat nose
        face.set(face.cx, 4, BOSS_SKIN[0])
        face.hline(1, 5, face.w - 2, VOID)             # the maw
        face.hline(2, 6, face.w - 4, VCRIMSON[1])
        face.lit(face.cx - 1, 6, BLOOD_DIM, 0.5)
        for x in (1, face.w - 2):
            face.set(x, 5, FANG)
            face.set(x, 6, FANG)
        face.set(face.cx - 2, 5, FANG)
        face.set(face.cx + 1, 5, FANG)
        face.hline(1, 7, face.w - 2, BOSS_SKIN[1])

    def hair(face, rng):
        if face.name == "front":
            for y in range(1, face.h):
                for x in range(face.w):
                    if not (y == 1 and face.cx - 1 <= x <= face.cx):
                        face.clear(x, y)
        elif face.name in ("left", "right"):
            for y in range(1, face.h):
                for k in range(3):
                    face.clear(k if face.name == "right" else face.w - 1 - k, y)
            ragged_edge(face, rng, "bottom", 2, 0.6)
        elif face.name == "back":
            ragged_edge(face, rng, "bottom", 2, 0.6)
        elif face.name == "bottom":
            clear_all(face)

    def belt(face, rng):
        if face.side():
            trim(face, LEATHER[0], ("top", "bottom"))
            if face.name == "front":
                face.set(face.cx, face.h // 2, SILVER[4])

    def rag(face, rng):
        if face.name in ("front", "back"):
            for x in range(1, face.w - 1, 3):
                face.vline(x, 0, face.h, NOBLE_BLACK[1])
            ragged_edge(face, rng, "bottom", 3, 0.7)
            holes_clear(face, rng, 1)

    def cape_deco(face, rng):
        if face.name == "front":          # the lining, seen from behind him: crimson with the folds
            face.rect(0, 0, face.w, face.h, VCRIMSON[2])
            for x in range(1, face.w - 1, 3):
                face.vline(x, 0, face.h, VCRIMSON[1])
        elif face.name == "back":
            for x in range(1, face.w - 1, 3):
                face.vline(x, 0, face.h, NOBLE_BLACK[1])
        if face.name in ("front", "back"):
            ragged_edge(face, rng, "bottom", 2, 0.5)
            holes_clear(face, rng, 1)

    cape_mat = Material(NOBLE_BLACK, base=2.4, noise=0.5, grain="v", bevel=0.3)
    # Animation signs (checked in the preview): -X raises an arm forward and opens the jaw, swings a leg forward,
    # tilts the head and body back; +X bows the body forward and folds a leg back.
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 24, 0], "root", [8, 0, 0],
             chamfered([-4, 12, -2], [8, 12, 4], doublet, {"all": torso}, c=0.5)
             + [Box([-5, 24, -3], [10, 4, 6], black, {"all": collar}),
                Box([-5, 24, 2], [10, 6, 1], black, {"all": lambda f, r: (
                    f.rect(0, 0, f.w, f.h, VCRIMSON[2]) if f.name == "front" else None,
                    ragged_edge(f, r, "top", 2, 0.5) if f.side() else None)},
                    rotation=[-20, 0, 0], pivot=[0, 24, 2.5]),
                Box([-4.3, 12, -2.3], [8.6, 2, 4.6], Material(LEATHER, base=1.8, noise=0.4, bevel=0.4), {"all": belt}),
                Box([-1, 19, -2.6], [2, 2, 0.8], Material(ramp("7a0a14", "b0141e", "ff2a2a", "ff9a7a"), base=1.8, noise=0.5, glow=1.0)),
                Box([-4.4, 17, -2.4], [8.8, 1, 0.6], Material(BONE, base=2.0, noise=0.4)),
                Box([-4.4, 20, -2.4], [8.8, 1, 0.6], Material(BONE, base=2.2, noise=0.4))]),
    ]
    # The coat tails hang from the belt at the back in two pieces each; the cape from the collar in three.
    for side, sx in (("right", -1), ("left", 1)):
        bones += chain_bones("coat_" + side, "body", [sx * 2.5, 12, 2.2], 2, 5.5, doublet, None, thickness=1,
                             box=lambda i, o, sz, sx=sx: [Box([min(sx * 0.5, sx * 4.5) + i * 0.3, o[1], 2.2 + i * 0.2], [4 - i * 0.8, 5.8, 0],
                                                              doublet.but(base=1.8), {"all": rag})])
    bones += chain_bones("cape", "body", [0, 27, 3.3], 3, 7, cape_mat, None, thickness=1,
                         box=lambda i, o, sz: [Box([-5.5 + i * 0.3, o[1], 3.3 + i * 0.25], [11 - i * 0.6, 7.3, 0], cape_mat.but(base=2.4 - i * 0.2),
                                                   {"all": cape_deco})])
    bones.append(Bone("tatter_front", [0, 12, -2.3], "body", None, [Box([-3, 8, -2.4], [6, 4.2, 0], doublet.but(base=1.6), {"all": rag})]))
    # Wings: an arm spar from the shoulder to the wrist, four fingers fanning out from the wrist, a membrane plane
    # stretched between them with a scalloped trailing edge.
    spar_y, wrist_x, root_x = 21.5, -12.0, -4.0
    fingers = [(-19.5, 18.0), (-18.5, 8.0), (-12.5, 7.0), (-7.5, 9.5)]   # where each finger ends (x, y)
    body_end = (-4.0, 14.0)
    left, bottom = min(x for x, _ in fingers), min(y for _, y in fingers)

    def outline():
        """The membrane's outline in model x / y: spar, leading digit, scallops between the finger ends."""
        pts = [(root_x, spar_y), (wrist_x, spar_y), fingers[0]]
        ends = fingers + [body_end]
        for (ax, ay), (bx, by) in zip(ends, ends[1:]):
            mx, my = (ax + bx) / 2, (ay + by) / 2
            # Pull the middle of each span toward the wrist: a scallop.
            dx, dy = wrist_x - mx, spar_y - my
            d = math.hypot(dx, dy) or 1
            depth = 0.0 if (ax, ay) == fingers[0] else min(2.8, math.hypot(bx - ax, by - ay) / 2.2)
            for i in range(1, 9):
                k = i / 9
                x = ax + (bx - ax) * k + dx / d * depth * math.sin(math.pi * k)
                y = ay + (by - ay) * k + dy / d * depth * math.sin(math.pi * k)
                pts.append((x, y))
            pts.append((bx, by))
        return pts

    def inside(poly, x, y):
        c = False
        for (x1, y1), (x2, y2) in zip(poly, poly[1:] + poly[:1]):
            if (y1 > y) != (y2 > y) and x < x1 + (y - y1) * (x2 - x1) / (y2 - y1):
                c = not c
        return c

    poly = outline()

    def membrane(face, rng):
        if face.name not in ("front", "back"):
            return
        w, h = face.w, face.h
        for fy in range(h):
            for fx in range(w):
                mx, my = left + fx + 0.5, spar_y - fy - 0.5   # front face: x grows toward +x (the body)
                xx = fx if face.name == "front" else w - 1 - fx
                if not inside(poly, mx, my):
                    face.clear(xx, fy)
        # Veins along each finger, a few fine ones between.
        for ex, ey in fingers:
            n = int(max(abs(ex - wrist_x), abs(ey - spar_y))) + 1
            for i in range(2, n, 1):
                x = wrist_x + (ex - wrist_x) * i / n
                y = spar_y + (ey - spar_y) * i / n
                fx, fy = int(x - left), int(spar_y - y)
                xx = fx if face.name == "front" else w - 1 - fx
                if face.get(xx, fy)[3] and i % 4:
                    face.set(xx, fy, MEMBRANE[0])
        for _ in range(8):
            x, y = rng.randrange(w), rng.randrange(h)
            if face.get(x, y)[3]:
                face.set(x, y, MEMBRANE[4])

    membrane_mat = Material(MEMBRANE, base=2.4, noise=0.6, grad=1.0)
    wing_boxes = [
        Box([left, bottom, 2.5], [root_x - left, spar_y - bottom, 0], membrane_mat, {"all": membrane}),
        Box([wrist_x, spar_y - 0.5, 2], [root_x - wrist_x + 1, 1.5, 1], black.but(base=3.0)),
        Box([wrist_x - 0.5, spar_y, 2], [1, 2.5, 1], horn.but(base=3.0), rotation=[0, 0, 25],
            pivot=[wrist_x, spar_y, 2.5]),
    ]
    for x, y in fingers:
        dx, dy = x - wrist_x, spar_y - y
        length = math.hypot(dx, dy)
        angle = -math.degrees(math.atan2(dx, dy))
        wing_boxes.append(Box([wrist_x - 0.5, spar_y - length, 2], [1, length, 1], black.but(base=2.6),
                              rotation=[0, 0, round(angle, 2)], pivot=[wrist_x, spar_y, 2.5]))
    bones += [
        Bone("right_wing", [-3, 22, 2], "body", [0, 30, 0], wing_boxes),
        Bone("left_wing", [3, 22, 2], "body", [0, -30, 0], [mirror_box(b) for b in wing_boxes]),
    ]
    ear = Box([-5, 28, -3], [1, 3, 2], skin, rotation=[0, -20, -15], pivot=[-4, 28, -2])
    horn_base = Box([-4.5, 31, -2], [2, 3, 2], horn, rotation=[-30, 0, -10], pivot=[-3.5, 31, -1])
    horn_tip = Box([-4.2, 33, 0.5], [1, 3, 1], horn.but(base=3.4), rotation=[-65, 0, -10], pivot=[-3.7, 33, 1])
    # The head: a chamfered skull, the brow ridge, the cap of hair, the jaw with its fangs on its own bone, ears,
    # horns, and the swept-back hair hanging down the neck in two strands.
    bones.append(Bone("head", [0, 25, -1], "root", None,
                      chamfered([-4, 25, -5], [8, 8, 8], skin, {"front": head_front,
                                                                 "back": lambda f, r: f.rect(0, 0, f.w, f.h - 2, HAIR_BLACK[1])}, c=0.6)
                      + [Box([-4.3, 29, -5.3], [8.6, 4.5, 8.6], Material(HAIR_BLACK, base=1.6, noise=0.6, top=0.4), {"all": hair}),
                         Box([-4.2, 28.6, -5.5], [8.4, 1.2, 1.2], skin.but(base=1.6)),
                         ear, mirror_box(ear), horn_base, mirror_box(horn_base), horn_tip, mirror_box(horn_tip)]))
    bones.append(Bone("jaw", [0, 26.5, -1], "head", None, [
        Box([-3, 24.8, -5.3], [6, 2, 4.6], skin.but(base=2.0), {"front": lambda f, r: (f.hline(0, 0, f.w, VCRIMSON[1]),
                                                                                         f.set(1, 0, FANG), f.set(f.w - 2, 0, FANG))}),
    ]))
    bones += chain_bones("hair", "head", [0, 32, 3.2], 2, 4.5, Material(HAIR_BLACK, base=1.4, noise=0.6), None, thickness=1,
                         box=lambda i, o, sz: [Box([-3 + i * 0.8, o[1], 3.2 + i * 0.3], [6 - i * 1.6, 4.8, 0], Material(HAIR_BLACK, base=1.4, noise=0.6),
                                                   {"all": lambda f, r: ragged_edge(f, r, "bottom", 2, 0.6) if f.name in ("front", "back") else None})])

    def arm_deco(face, rng):
        if face.side():
            # Sleeve torn off above the elbow; the forearm bony, the hand dark at the knuckles.
            for y in range(0, 5):
                for x in range(face.w):
                    face.set(x, y, BURGUNDY[1 + (x + y) % 2])
            ragged_edge_rows(face, rng, 5)
            face.hline(0, face.h - 1, face.w, BOSS_SKIN[1])

    # Arms: the torn sleeve on the upper arm, a bony forearm, a hand with three claws (its own bone).
    upper = Box([-7.5, 15, -1.5], [3, 8, 3], skin, {"all": arm_deco})
    sleeve_rag = Box([-7.8, 19, -1.8], [3.6, 4.5, 3.6], doublet.but(base=1.8), {"all": lambda f, r: ragged_edge(f, r, "bottom", 2, 0.6) if f.side() else None})
    fore = Box([-7.5, 7.5, -1.5], [3, 8, 3], skin.but(base=2.3), {"sides": lambda f, r: f.hline(0, f.h - 1, f.w, BOSS_SKIN[1])})
    hand = Box([-7.5, 6, -1.5], [3, 2, 3], skin.but(base=2.0))
    claws = [Box([-7.5 + 1 * i, 2.5, -1.5 - (i == 1) * 0.5], [1, 3.5, 1], claw_mat, rotation=[-15, 0, 0],
                 pivot=[-7 + i, 6, -1]) for i in range(3)]
    bones += [
        Bone("right_arm", [-5, 23, 0], "root", [-10, 0, 8], [upper, sleeve_rag]),
        Bone("right_forearm", [-6, 15.5, 0], "right_arm", None, [fore]),
        Bone("right_hand", [-6, 8, 0], "right_forearm", None, [hand] + claws),
        Bone("left_arm", [5, 23, 0], "root", [-10, 0, -8], [mirror_box(upper), mirror_box(sleeve_rag)]),
        Bone("left_forearm", [6, 15.5, 0], "left_arm", None, [mirror_box(fore)]),
        Bone("left_hand", [6, 8, 0], "left_forearm", None, [mirror_box(hand)] + [mirror_box(c) for c in claws]),
    ]

    def leg_deco(face, rng):
        if face.side():
            ragged_edge_rows(face, rng, face.h - 4, lambda x, y: BOSS_SKIN[1 + (x + y) % 2])
            face.hline(0, face.h - 1, face.w, BOSS_SKIN[0])

    leg = chamfered([-4, 0, -2], [4, 12, 4], black.but(base=1.8), {"all": leg_deco}, c=0.5)
    knee = Box([-4.2, 5.5, -2.5], [4.4, 2.2, 1], black.but(base=2.6))
    toes = Box([-4, 0, -3.5], [4, 1, 1.5], claw_mat, {"front": lambda f, r: [f.set(x, 0, BONE[3]) for x in (0, 2)]})
    spur = Box([-2.5, 1, 2], [1, 2, 1.5], claw_mat, rotation=[25, 0, 0], pivot=[-2, 2, 2])
    bones += [Bone("right_leg", [-2, 12, 0], "root", None, leg + [knee, toes, spur]),
              Bone("left_leg", [2, 12, 0], "root", None, [mirror_box(b) for b in leg + [knee, toes, spur]])]
    loose = [chain_names("cape", 3), chain_names("coat_right", 2), chain_names("coat_left", 2), chain_names("hair", 2), ["tatter_front"]]

    def loose_sway(length, amp, phase=0.0):
        out = {}
        for i, names in enumerate(loose):
            out = merge(out, lag_sway(names, length, amp * (1.3 if i == 0 else 1), axis=0, phase=phase + i * 0.15))
        return out

    def loose_whip(t0, amp, settle=0.5):
        out = {}
        for i, names in enumerate(loose):
            out = merge(out, whip(names, t0 + 0.03 * i, amp * (1.3 if i == 0 else 0.9), axis=0, settle=settle))
        return out

    animations = {
        "misc.idle": anim(2.0, merge(loose_sway(2.0, 4), {
            "right_wing": rot(0, ZERO, 1, e("easeInOutSine", [0, 12, 4]), 2, e("easeInOutSine", ZERO)),
            "left_wing": rot(0, ZERO, 1, e("easeInOutSine", [0, -12, -4]), 2, e("easeInOutSine", ZERO)),
            "body": rot(0, ZERO, 1, e("easeInOutSine", [3, 0, 0]), 2, e("easeInOutSine", ZERO)),
            "head": rot(0, [4, 0, 0], 1, e("easeInOutSine", [0, 0, 3]), 2, e("easeInOutSine", [4, 0, 0])),
            "jaw": rot(0, ZERO, 1, [-5, 0, 0], 2, ZERO),
            "right_arm": rot(0, ZERO, 1, e("easeInOutSine", [-5, 0, 3]), 2, e("easeInOutSine", ZERO)),
            "left_arm": rot(0, ZERO, 1, e("easeInOutSine", [-5, 0, -3]), 2, e("easeInOutSine", ZERO)),
            "right_hand": rot(0, ZERO, 1, [-10, 0, 0], 2, ZERO),
            "left_hand": rot(0, ZERO, 1, [-10, 0, 0], 2, ZERO),
        })),
        "move.walk": anim(0.8, merge(loose_sway(0.8, 9, 0.1), {
            "right_leg": rot(0, [30, 0, 0], 0.4, e("easeInOutSine", [-30, 0, 0]), 0.8, e("easeInOutSine", [30, 0, 0])),
            "left_leg": rot(0, [-30, 0, 0], 0.4, e("easeInOutSine", [30, 0, 0]), 0.8, e("easeInOutSine", [-30, 0, 0])),
            "right_arm": rot(0, [-25, 0, 0], 0.4, e("easeInOutSine", [15, 0, 0]), 0.8, e("easeInOutSine", [-25, 0, 0])),
            "left_arm": rot(0, [15, 0, 0], 0.4, e("easeInOutSine", [-25, 0, 0]), 0.8, e("easeInOutSine", [15, 0, 0])),
            "right_forearm": rot(0, [-20, 0, 0], 0.8, [-20, 0, 0]),
            "left_forearm": rot(0, [-20, 0, 0], 0.8, [-20, 0, 0]),
            "right_wing": rot(0, [0, 20, 0], 0.4, e("easeInOutSine", [0, 35, 0]), 0.8, e("easeInOutSine", [0, 20, 0])),
            "left_wing": rot(0, [0, -20, 0], 0.4, e("easeInOutSine", [0, -35, 0]), 0.8, e("easeInOutSine", [0, -20, 0])),
            "root": pos(0, ZERO, 0.1, e("easeOutQuad", [0, -0.4, 0]), 0.25, [0, 0.5, 0], 0.5, e("easeOutQuad", [0, -0.4, 0]), 0.65, [0, 0.5, 0], 0.8, ZERO),
            "body": rot(0, [0, -4, 2], 0.4, e("easeInOutSine", [0, 4, -2]), 0.8, e("easeInOutSine", [0, -4, 2])),
            "head": rot(0, [4, 4, -2], 0.4, e("easeInOutSine", [4, -4, 2]), 0.8, e("easeInOutSine", [4, 4, -2])),
        })),
        # Claws: both arms drawn up and back, then raked down across; the hands open wide into it.
        "attack.swing": anim(0.5, merge(loose_whip(0.22, 25, 0.28), {
            "right_arm": rot(0, ZERO, 0.18, e("easeInOutSine", [-135, 20, 0]), 0.3, e("easeOutBack", [-30, -20, 0]), 0.5, e("easeInOutSine", ZERO)),
            "left_arm": rot(0, ZERO, 0.22, e("easeInOutSine", [-125, -20, 0]), 0.34, e("easeOutBack", [-30, 20, 0]), 0.5, e("easeInOutSine", ZERO)),
            "right_forearm": rot(0, ZERO, 0.18, [-40, 0, 0], 0.3, e("easeOutCubic", [10, 0, 0]), 0.5, ZERO),
            "left_forearm": rot(0, ZERO, 0.22, [-40, 0, 0], 0.34, e("easeOutCubic", [10, 0, 0]), 0.5, ZERO),
            "right_hand": rot(0, ZERO, 0.18, [-30, 0, 0], 0.3, [25, 0, 0], 0.5, ZERO),
            "left_hand": rot(0, ZERO, 0.22, [-30, 0, 0], 0.34, [25, 0, 0], 0.5, ZERO),
            "body": rot(0, ZERO, 0.18, e("easeInOutSine", [-6, 12, 0]), 0.32, e("easeOutBack", [10, -12, 0]), 0.5, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, 0.18, [-12, 0, 0], 0.32, [10, 0, 0], 0.5, ZERO),
            "jaw": rot(0, ZERO, 0.18, [-30, 0, 0], 0.4, ZERO),
        }), loop=False),
        # 1 s windup (LEAP_WINDUP 20 ticks): a deep crouch with the wings drawn up and the claws back, then the jump
        # with everything thrown forward; the cape and coat stream behind.
        "attack.leap": anim(1.4, merge(loose_whip(1.0, 45, 0.4), {
            "body": rot(0, ZERO, 0.7, e("easeInOutSine", [32, 0, 0]), 1.0, [36, 0, 0], 1.1, e("easeOutCubic", [-12, 0, 0]), 1.4, e("easeInOutSine", ZERO)),
            "root": pos(0, ZERO, 0.7, e("easeInOutSine", [0, -3, 0]), 1.0, [0, -3.5, 0], 1.1, e("easeOutCubic", [0, 1, 0]), 1.4, ZERO),
            "right_wing": rot(0, ZERO, 0.7, e("easeInOutSine", [-10, 45, 35]), 1.0, [-12, 48, 38], 1.1, e("easeOutBack", [0, -25, -12]), 1.4, e("easeInOutSine", ZERO)),
            "left_wing": rot(0, ZERO, 0.7, e("easeInOutSine", [-10, -45, -35]), 1.0, [-12, -48, -38], 1.1, e("easeOutBack", [0, 25, 12]), 1.4, e("easeInOutSine", ZERO)),
            "right_arm": rot(0, ZERO, 0.7, e("easeInOutSine", [40, 0, 30]), 1.0, [45, 0, 32], 1.1, e("easeOutBack", [-150, 0, 0]), 1.4, e("easeInOutSine", ZERO)),
            "left_arm": rot(0, ZERO, 0.7, e("easeInOutSine", [40, 0, -30]), 1.0, [45, 0, -32], 1.1, e("easeOutBack", [-150, 0, 0]), 1.4, e("easeInOutSine", ZERO)),
            "right_hand": rot(0, ZERO, 1.0, [-20, 0, 0], 1.1, [20, 0, 0], 1.4, ZERO),
            "left_hand": rot(0, ZERO, 1.0, [-20, 0, 0], 1.1, [20, 0, 0], 1.4, ZERO),
            "right_leg": rot(0, ZERO, 0.7, [-32, 0, 0], 1.0, [-35, 0, 0], 1.1, [20, 0, 0], 1.4, ZERO),
            "left_leg": rot(0, ZERO, 0.7, [-32, 0, 0], 1.0, [-35, 0, 0], 1.1, [20, 0, 0], 1.4, ZERO),
            "head": rot(0, ZERO, 0.7, [-30, 0, 0], 1.0, [-32, 0, 0], 1.1, [10, 0, 0], 1.4, ZERO),
            "jaw": rot(0, ZERO, 1.0, [-10, 0, 0], 1.1, [-35, 0, 0], 1.4, ZERO),
        }), loop=False),
        # 1.5 s windup (NOVA_WINDUP 30 ticks): arms and wings spread wider and wider, the head thrown back, then the burst.
        "attack.nova": anim(1.8, merge(loose_whip(1.5, 50, 0.3), {
            "right_arm": rot(0, ZERO, 1.4, e("easeInOutSine", [0, 0, 100]), 1.5, e("easeOutBack", [0, 0, 140]), 1.8, e("easeInOutSine", ZERO)),
            "left_arm": rot(0, ZERO, 1.4, e("easeInOutSine", [0, 0, -100]), 1.5, e("easeOutBack", [0, 0, -140]), 1.8, e("easeInOutSine", ZERO)),
            "right_hand": rot(0, ZERO, 1.4, [-20, 0, 0], 1.5, [-40, 0, 0], 1.8, ZERO),
            "left_hand": rot(0, ZERO, 1.4, [-20, 0, 0], 1.5, [-40, 0, 0], 1.8, ZERO),
            # Wings opened flat to the front (rest is swept back 30 degrees) and raised.
            "right_wing": rot(0, ZERO, 1.4, e("easeInOutSine", [0, -25, 20]), 1.5, e("easeOutBack", [0, -30, 28]), 1.8, e("easeInOutSine", ZERO)),
            "left_wing": rot(0, ZERO, 1.4, e("easeInOutSine", [0, 25, -20]), 1.5, e("easeOutBack", [0, 30, -28]), 1.8, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, 1.4, e("easeInOutSine", [-30, 0, 0]), 1.5, [-38, 0, 0], 1.8, e("easeInOutSine", ZERO)),
            "jaw": rot(0, ZERO, 1.0, [-15, 0, 0], 1.5, [-40, 0, 0], 1.8, ZERO),
            "body": rot(0, ZERO, 1.4, e("easeInOutSine", [-12, 0, 0]), 1.5, [-18, 0, 0], 1.8, e("easeInOutSine", ZERO)),
            "root": pos(0, ZERO, 1.4, [0, 0.5, 0], 1.5, e("easeOutBack", [0, 2, 0]), 1.8, ZERO),
        }), loop=False),
        # 0.6 s windup (MIST_WINDUP 12 ticks): folds into its wings and the cape, and thins to nothing.
        "misc.mist": anim(0.8, {
            "right_wing": rot(0, ZERO, 0.6, e("easeInOutSine", [0, -60, 0]), 0.8, ZERO),
            "left_wing": rot(0, ZERO, 0.6, e("easeInOutSine", [0, 60, 0]), 0.8, ZERO),
            "cape_1": rot(0, ZERO, 0.6, [-40, 0, 0], 0.8, ZERO),
            "cape_2": rot(0, ZERO, 0.6, [-30, 0, 0], 0.8, ZERO),
            "root": {"scale": keys(0, [1, 1, 1], 0.6, e("easeInQuad", [0.8, 1.1, 0.8]), 0.8, [1, 1, 1])},
            "head": rot(0, ZERO, 0.6, [20, 0, 0], 0.8, ZERO),
            "right_arm": rot(0, ZERO, 0.6, [-60, 0, -40], 0.8, ZERO),
            "left_arm": rot(0, ZERO, 0.6, [-60, 0, 40], 0.8, ZERO),
        }, loop=False),
        # Hurt: a snap of the head and shoulders, the cape and hair jumping.
        "misc.hurt": anim(0.4, merge(loose_whip(0.02, 16, 0.36), {
            "body": rot(0, ZERO, 0.08, e("easeOutCubic", [-8, 6, 0]), 0.4, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, 0.08, e("easeOutCubic", [-16, -10, 0]), 0.4, e("easeInOutSine", ZERO)),
            "jaw": rot(0, ZERO, 0.08, [-20, 0, 0], 0.4, ZERO),
            "root": pos(0, ZERO, 0.08, [0, 0, 1], 0.4, ZERO),
            "right_wing": rot(0, ZERO, 0.08, [0, 15, 8], 0.4, ZERO),
            "left_wing": rot(0, ZERO, 0.08, [0, -15, -8], 0.4, ZERO),
        }), loop=False),
        # Stagger (a heavy hit): thrown back a step, wings flung open, a hand to the wound, then it recovers.
        "misc.stagger": anim(0.9, merge(loose_whip(0.05, 35, 0.8), {
            "root": pos(0, ZERO, 0.15, e("easeOutCubic", [0, -1, 3]), 0.5, [0, -1.5, 4], 0.9, e("easeInOutSine", ZERO)),
            "body": rot(0, ZERO, 0.15, e("easeOutBack", [-25, 8, 0]), 0.5, [-18, -6, 0], 0.9, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, 0.15, [-25, 10, 0], 0.5, [-10, -10, 0], 0.9, ZERO),
            "jaw": rot(0, ZERO, 0.15, [-35, 0, 0], 0.9, ZERO),
            "right_wing": rot(0, ZERO, 0.15, e("easeOutBack", [-10, -30, 30]), 0.9, e("easeInOutSine", ZERO)),
            "left_wing": rot(0, ZERO, 0.15, e("easeOutBack", [-10, 30, -30]), 0.9, e("easeInOutSine", ZERO)),
            "right_arm": rot(0, ZERO, 0.15, e("easeOutBack", [-40, 0, 50]), 0.9, e("easeInOutSine", ZERO)),
            "left_arm": rot(0, ZERO, 0.15, e("easeOutBack", [-70, 30, 0]), 0.5, [-75, 35, 0], 0.9, e("easeInOutSine", ZERO)),
            "left_forearm": rot(0, ZERO, 0.15, [-60, 0, 0], 0.9, ZERO),
            "right_leg": rot(0, ZERO, 0.15, [30, 0, 0], 0.5, [25, 0, 0], 0.9, ZERO),
        }), loop=False),
        # Phase (a threshold crossed): it arches back with a shriek, the wings and cape flung wide, then snaps forward.
        "misc.phase": anim(1.2, merge(loose_whip(0.45, 45, 0.7), {
            "body": rot(0, ZERO, 0.4, e("easeInOutSine", [-28, 0, 0]), 0.55, [-30, 0, 0], 0.75, e("easeOutBack", [14, 0, 0]), 1.2, e("easeInOutSine", ZERO)),
            "head": rot(0, ZERO, 0.4, [-35, 0, 0], 0.55, [-38, 0, 0], 0.75, e("easeOutBack", [12, 0, 0]), 1.2, ZERO),
            "jaw": rot(0, ZERO, 0.4, [-40, 0, 0], 0.75, [-30, 0, 0], 1.2, ZERO),
            "right_wing": rot(0, ZERO, 0.4, e("easeInOutSine", [-15, -35, 40]), 0.75, [-5, -10, 10], 1.2, e("easeInOutSine", ZERO)),
            "left_wing": rot(0, ZERO, 0.4, e("easeInOutSine", [-15, 35, -40]), 0.75, [-5, 10, -10], 1.2, e("easeInOutSine", ZERO)),
            "right_arm": rot(0, ZERO, 0.4, e("easeInOutSine", [-40, 0, 70]), 0.75, [-20, 0, 20], 1.2, e("easeInOutSine", ZERO)),
            "left_arm": rot(0, ZERO, 0.4, e("easeInOutSine", [-40, 0, -70]), 0.75, [-20, 0, -20], 1.2, e("easeInOutSine", ZERO)),
            "root": pos(0, ZERO, 0.4, [0, 1, 1], 0.75, [0, -0.5, -1], 1.2, ZERO),
        }), loop=False),
        # Death (3 s): a hand to the heart, it staggers back, sinks to its knees with the wings spread and the
        # head thrown back, then folds forward onto its hands; the wings droop and the cape settles.
        "misc.death": anim(3.0, merge(loose_whip(2.0, 35, 0.9), {
            "root": pos(0, ZERO, 0.5, e("easeOutCubic", [0, -0.5, 2]), 1.0, e("easeInQuad", [0, -8, 1]), 1.6, [0, -8, 1],
                        2.0, e("easeInCubic", [0, -9, -1]), 2.2, e("easeOutBounce", [0, -9.5, -1.5]), 3.0, [0, -9.5, -1.5]),
            "right_leg": rot(0, ZERO, 0.5, [8, 0, 0], 1.0, e("easeInQuad", [85, 0, 0]), 3.0, [85, 0, 0]),
            "left_leg": rot(0, ZERO, 0.5, [-10, 0, 0], 1.0, e("easeInQuad", [88, 0, 0]), 3.0, [88, 0, 0]),
            "body": rot(0, ZERO, 0.5, e("easeInOutSine", [-15, 5, 0]), 1.0, [-25, 0, 3], 1.6, e("easeInOutSine", [-30, 0, 3]), 2.0,
                        e("easeInCubic", [60, 0, 0]), 2.2, e("easeOutBounce", [66, 0, 0]), 3.0, [66, 0, 0]),
            "head": rot(0, ZERO, 0.5, [-10, 0, 0], 1.0, [-40, 0, 0], 1.6, [-45, 0, 8], 2.0, e("easeInCubic", [10, 0, 10]), 2.2,
                        e("easeOutCubic", [20, 0, 10]), 2.7, e("easeInQuad", [28, 12, 10]), 3.0, [28, 12, 10]),
            "jaw": rot(0, ZERO, 0.5, [-20, 0, 0], 1.6, [-45, 0, 0], 2.2, [-35, 0, 0], 3.0, [-30, 0, 0]),
            "left_arm": rot(0, ZERO, 0.5, e("easeInOutSine", [-80, 35, 0]), 1.6, [-85, 30, 0], 2.0, e("easeInCubic", [-30, 0, -15]),
                            2.2, e("easeOutBounce", [-25, 0, -18]), 3.0, [-25, 0, -18]),
            "left_forearm": rot(0, ZERO, 0.5, [-70, 0, 0], 1.6, [-70, 0, 0], 2.0, [-10, 0, 0], 3.0, [-10, 0, 0]),
            "right_arm": rot(0, ZERO, 0.5, e("easeInOutSine", [-20, 0, 40]), 1.6, [-40, 0, 55], 2.0, e("easeInCubic", [-30, 0, 15]),
                             2.2, e("easeOutBounce", [-25, 0, 18]), 3.0, [-25, 0, 18]),
            "right_hand": rot(0, ZERO, 1.6, [-30, 0, 0], 2.2, [40, 0, 0], 3.0, [40, 0, 0]),
            "left_hand": rot(0, ZERO, 1.6, [-30, 0, 0], 2.2, [40, 0, 0], 3.0, [40, 0, 0]),
            "right_wing": rot(0, ZERO, 1.0, e("easeInOutSine", [-15, -35, 45]), 1.6, [-15, -40, 50], 2.0, e("easeInCubic", [20, 10, -10]),
                              2.3, e("easeOutBounce", [25, 15, -20]), 3.0, [25, 15, -20]),
            "left_wing": rot(0, ZERO, 1.0, e("easeInOutSine", [-15, 35, -45]), 1.6, [-15, 40, -50], 2.0, e("easeInCubic", [20, -10, 10]),
                             2.3, e("easeOutBounce", [25, -15, 20]), 3.0, [25, -15, 20]),
        }), loop=False),
    }
    write_model("vampire_boss", bones, animations, [3, 3.5])


def ragged_edge_rows(face, rng, y0, color=None):
    """A ragged hem across a side face at row y0: rows above it are cloth, a jagged line where it ends; with color
    (fn(x, y)) the rows below are repainted (bare skin under torn trousers)."""
    for x in range(face.w):
        cut = y0 + rng.choice((0, 0, 1, -1))
        if color is not None:
            for y in range(max(0, cut), face.h):
                face.set(x, y, color(x, y))
        face.set(x, cut, shade(face.get(x, cut), -25))


def round_corners(face, rng=None):
    """Clears the corner pixels of a face (and the two below the top corners of a side face): a rounder
    silhouette for a cube of flame or mist."""
    for x, y in ((0, 0), (face.w - 1, 0), (0, face.h - 1), (face.w - 1, face.h - 1)):
        face.clear(x, y)
    if face.side():
        face.clear(0, 1)
        face.clear(face.w - 1, 1)


def holes_clear(face, rng, count):
    for _ in range(count):
        face.clear(rng.randrange(face.w), rng.randrange(face.h))


ENTITIES = [
    ("ash_ghoul", ash_ghoul), ("hollow", hollow), ("ember_wisp", ember_wisp), ("ashen_maiden", ashen_maiden),
    ("ashen_zombie", ashen_zombie), ("ghost", ghost), ("vampire", vampire), ("vampire_boss", vampire_boss),
]


if __name__ == "__main__":
    wanted = set(sys.argv[1:])
    for name, build in ENTITIES:
        if not wanted or name in wanted:
            build()
