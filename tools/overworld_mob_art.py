#!/usr/bin/env python3
"""Final models, animations and textures for the overworld entities of 0.2.0 and 0.3.0 (PLAN L4): Ash Ghoul,
Hollow, Ember Wisp, Ashen Maiden, Ashen Zombie, Ghost, Vampire and the Vampire boss.

Run from the repository root: python3 tools/overworld_mob_art.py [name ...]
Replaces the placeholders of tools/mob_placeholders.py, tools/maiden_placeholder.py, tools/nightmob_placeholders.py
and tools/vampire_placeholders.py at the same size (tools/model_bounds.json), with the placeholders' bone names,
hierarchy and pivots (the head the renderers turn, the Hollow's right_item bone its sword is drawn at) and the same
animation names and lengths (the Vampire boss telegraphs end where VampireBossEntity lands them: leap 20, nova 30,
mist 12 ticks). Check with
    python3 tools/model_preview.py check <name> && python3 tools/model_preview.py entity <name>

Look: the same kit and banded ramps as the Ashen Reach mobs (tools/reach_mob_art.py), but the overworld under a
normal sky: ash and embers for the creatures of the Ember (ghoul, wisp, zombie, maiden), rusted iron and a faded
ember-red for the Sanctum's fallen guards (Hollow), cold pale blue for the Ghost, black, crimson and bone-white
for the Vampires of the blood moon. The Ember Wisp and the Ghost are drawn translucent (their renderers use
RenderLayer.getEntityTranslucent): their textures carry partial alpha, and so do their glowmasks (GeckoLib's glow
layer blends with alpha, full bright).
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from art_kit import (ZERO, Bone, Box, Material, anim, chain_bones, chain_names, chamfered, clamp, crack, e,  # noqa: E402
                     ember_cracks, hexc, keys, lag_sway, merge, mirror_box, pos, ramp, rot, scatter, shade, sway, tatter,
                     whip, write_model)
from reach_mob_art import (ASH, BONE, BRONZE, CHAR, COLD, COLD_DIM, EMBER, EMBER_DARK, EMBER_DEEP,  # noqa: E402
                           EMBER_HOT, IRON, LEATHER, LINEN, ROPE, RUST, VOID, WOOD, band, bone, clear_all, eyes,
                           holes, lower, mail, open_front, ragged_edge, rivets, trim)

# --- Palettes (dark to light) -------------------------------------------------------------------------------------

GHOUL = ramp("2a2725", "3a3633", "4d4844", "625c56", "79726a", "8f877d")
ZOMBIE = ramp("34312f", "46423e", "5a5550", "706a63", "878077", "9f978c")
CLOTH_BLUE = ramp("1f2228", "2a2f37", "363d47", "434c58", "525c69")
TABARD = ramp("2a1311", "3c1a16", "50231c", "652d23", "7a382b")
MAIDEN_ROBE = ramp("29262a", "363237", "443f45", "534d54", "635c64", "746c75")
MAIDEN_SKIN = ramp("6e6259", "84776c", "998b7f", "ad9f92", "bfb1a3", "cfc2b4")
GHOST_PALE = ramp("4e5a6b", "62708a", "7888a3", "8fa0ba", "a8b8cf", "c3d1e2")
GHOST_DARK = ramp("161a24", "1e2431", "272f3f", "323c4f")
VAMP_SKIN = ramp("8d8590", "a39ba6", "b7b0b9", "c9c3ca", "dad5da", "e8e4e8")
BOSS_SKIN = ramp("4a4049", "5c515b", "6f636d", "82767f", "958a92", "a89ea5")
NOBLE_BLACK = ramp("0f0d10", "171419", "201c22", "29242c", "332d37", "3f3843")
VCRIMSON = ramp("2b070b", "420c12", "5c121a", "771823", "92202c", "ad2a36")
BURGUNDY = ramp("1e0a10", "2b0f17", "39141f", "481a28", "572132")
HAIR_BLACK = ramp("0c0b0e", "141218", "1d1a22", "27232d")
LACE = ramp("8f8a86", "aaa49f", "c2bdb7", "d7d2cc", "e8e4df")
SILVER = ramp("3d3f46", "5a5d66", "7a7e88", "9a9fa9", "bcc0c8", "dde0e6")
GOLD = ramp("5a3d12", "7f5a1a", "a67b26", "c99a36", "e8be55")
HAIR_ASH = ramp("26242a", "34313a", "45414b", "57525e")
MEMBRANE = ramp("1b070b", "28090f", "360d14", "45121a", "561822")

FLAME = ramp("a8320c", "d2501a", "f07424", "ff9a3a", "ffbe5c", "ffdc8c")
CORE = ramp("ffb347", "ffd27a", "ffe6a8", "fff4d6")
BLOOD = hexc("ff2a2a")
BLOOD_DIM = hexc("b0141e")
GHOST_EYE = hexc("8ff0ff")
FANG = hexc("f2eee6")


# --- Helpers ------------------------------------------------------------------------------------------------------

def compose(*fns):
    """Several decals as one (a Box takes one decal per face key)."""
    def fn(face, rng):
        for f in fns:
            f(face, rng)
    return fn


def fade(alpha, bottom=None, glow=0, glow_bottom=None):
    """Translucency for the entities drawn with RenderLayer.getEntityTranslucent: sets the alpha of every painted
    pixel (from alpha at the top of a side face to bottom at its foot), and with glow > 0 gives every pixel that does
    not glow yet a faint glow of its own colour at that alpha (so it still reads at night). Run it last."""
    def fn(face, rng):
        for y in range(face.h):
            k = y / (face.h - 1) if face.side() and face.h > 1 else 0.0
            a = alpha if bottom is None else alpha + (bottom - alpha) * k
            ga = glow if glow_bottom is None else glow + (glow_bottom - glow) * k
            for x in range(face.w):
                c = face.get(x, y)
                if not c[3]:
                    continue
                face.tex.set(face.x0 + x, face.y0 + y, c[:3] + (clamp(a),))
                g = face.glow.get(face.x0 + x, face.y0 + y)
                if g[3]:
                    continue
                if ga > 0:
                    face.glow.set(face.x0 + x, face.y0 + y, c[:3] + (clamp(ga),))
    return fn


def glow_all(alpha=255, scale=1.0):
    """Every painted pixel glows with its own colour (a body made of light)."""
    def fn(face, rng):
        for y in range(face.h):
            for x in range(face.w):
                c = face.get(x, y)
                if c[3]:
                    face.glow.set(face.x0 + x, face.y0 + y,
                                  tuple(clamp(v * scale) for v in c[:3]) + (clamp(alpha),))
    return fn


def flame_tongues(face, rng, depth=3, chance=0.5):
    """Licking flame: clears notches down from the top edge of a side face, deeper here and there."""
    if face.side():
        for x in range(face.w):
            if rng.random() < chance:
                for k in range(rng.randint(1, depth)):
                    face.clear(x, k)


def seams(face, rng, n, length=(2, 4)):
    ember_cracks(face, rng, n, EMBER, EMBER_DEEP, 0.85, length)


def lit_eyes(face, y, color, xs, strength=1.0, socket=None, w=1):
    for x in xs:
        if socket is not None:
            face.rect(x - 1, y - 1, w + 2, 3, socket)
        for k in range(w):
            face.lit(x + k, y, color, strength)


# --- Ash Ghoul ------------------------------------------------------------------------------------------------------
# Fast, weak pack hunter of the night (DESIGN §3.2): a gaunt, hunched corpse of ash with embers still in its cracks,
# arms too long and ending in claws, ribs and spine pushing through the skin, a hanging jaw. It bursts into an ash
# cloud on death, so it looks dry and flaking.

def ash_ghoul():
    skin = Material(GHOUL, base=2.6, noise=0.8, speck=0.06, grad=1.0, top=1.0, edge=0.4)

    def flaking(face, rng, seam=0.7):
        if face.side():
            scatter(face, rng, max(1, face.w * face.h // 18), GHOUL[1])
            if rng.random() < seam:
                ember_cracks(face, rng, 1, EMBER, EMBER_DEEP, 0.8, (1, 2))

    def limb(face, rng):
        flaking(face, rng, 0.35)

    def torso_front(face, rng):
        # Ribs through the skin, a glowing split under them.
        for i, y in enumerate((1, 3, 5)):
            face.hline(1, y, face.w - 2, GHOUL[4])
            face.hline(1, y + 1, face.w - 2, GHOUL[1])
        face.vline(face.cx, 1, 6, GHOUL[0])
        face.lit(face.cx, 7, EMBER, 0.9)
        face.lit(face.cx - 1, 8, EMBER_DEEP, 0.7)
        face.lit(face.cx, 8, EMBER_HOT, 0.9)

    def torso_back(face, rng):
        face.vline(face.cx, 0, face.h, GHOUL[1])
        for y in range(1, face.h, 2):
            face.set(face.cx, y, GHOUL[5])
        flaking(face, rng)

    def head_front(face, rng):
        # Skull-tight face: deep sockets with ember pupils, a slit nose, cheekbones, the jaw hangs below.
        face.hline(0, 1, face.w, GHOUL[4])          # brow ridge
        face.hline(0, 2, face.w, GHOUL[1])
        for ex in (1, face.w - 3):
            face.rect(ex, 2, 2, 2, VOID)
        face.lit(2, 3, EMBER_HOT, 1.0)
        face.lit(face.w - 3, 3, EMBER_HOT, 1.0)
        face.lit(1, 3, EMBER_DEEP, 0.6)
        face.lit(face.w - 2, 3, EMBER_DEEP, 0.6)
        face.set(face.cx, 4, VOID)
        face.set(face.cx, 5, GHOUL[0])
        face.hline(1, 5, 1, GHOUL[4])
        face.hline(face.w - 2, 5, 1, GHOUL[4])
        face.hline(1, 6, face.w - 2, VOID)
        for x in range(1, face.w - 1, 2):
            face.set(x, 6, BONE[2])

    def jaw_front(face, rng):
        face.hline(0, 0, face.w, VOID)
        for x in range(0, face.w, 2):
            face.set(x, 0, BONE[3])
        face.lit(face.cx, 0, EMBER_DEEP, 0.6)

    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 11, 0], "root", [20, 0, 0], [
            Box([-3.5, 11, -1.5], [7, 10, 3], skin, {"front": torso_front, "back": torso_back,
                                                     "left": flaking, "right": flaking}),
            Box([-3.5, 8.5, -1.8], [7, 3, 3.6], Material(LINEN, base=1.2, noise=0.7, grain="v"),
                {"sides": lambda f, r: (tatter(f, r, 2, 0.6), holes(f, r, 1))}, inflate=0.2),
        ] + [Box([-0.5, y, 1.5], [1, 1, 1], skin.but(base=4.0)) for y in (13, 15.5, 18)]),
        Bone("head", [0, 20, -3], "root", None, [
            Box([-3.5, 20, -6.5], [7, 7, 7], skin.but(base=2.8), {"front": head_front, "back": flaking,
                                                                 "left": flaking, "right": flaking}),
            Box([-2.5, 18.5, -6.6], [5, 1.5, 4], skin.but(base=2.0), {"front": jaw_front}),
            Box([-3, 26.5, -5], [6, 1, 5], Material(GHOUL, base=0.8, noise=0.6),
                {"all": lambda f, r: ragged_edge(f, r, "bottom" if f.side() else "top", 1, 0.5)}),
        ]),
    ]
    arm = Box([-6, 6, -4.5], [3, 14, 3], skin, {"sides": limb})
    claws = [Box([-6, 3.5, -4.5], [1, 2.5, 1], Material(BONE, base=1.4, noise=0.4), rotation=[-20, 0, 0],
                 pivot=[-5.5, 6, -4]),
             Box([-4, 3.5, -4.5], [1, 2.5, 1], Material(BONE, base=1.4, noise=0.4), rotation=[-20, 0, 0],
                 pivot=[-3.5, 6, -4]),
             Box([-5, 4.5, -2.5], [1, 1.5, 1], Material(BONE, base=1.0, noise=0.4))]
    bones += [
        Bone("right_arm", [-4.5, 20, -3], "root", [-15, 0, 0], [arm] + claws),
        Bone("left_arm", [4.5, 20, -3], "root", [-15, 0, 0], [mirror_box(arm)] + [mirror_box(c) for c in claws]),
    ]
    leg = Box([-3.5, 0, -1.5], [3, 11, 3], skin.but(base=2.2), {"sides": lambda f, r: flaking(f, r, 0.0)})
    toes = Box([-3.5, 0, -2.5], [3, 1, 1], Material(BONE, base=1.2, noise=0.4))
    bones += [Bone("right_leg", [-2, 11, 0], "root", None, [leg, toes]),
              Bone("left_leg", [2, 11, 0], "root", None, [mirror_box(leg), mirror_box(toes)])]
    animations = {
        "misc.idle": anim(2.0, {
            "body": rot(0, ZERO, 1, [4, 0, 0], 2, ZERO),
            "head": rot(0, ZERO, 0.5, [6, -8, 0], 1.1, [2, 10, 4], 1.6, [8, 0, 0], 2, ZERO),
            "right_arm": rot(0, ZERO, 1, [-8, 0, 4], 2, ZERO),
            "left_arm": rot(0, ZERO, 1, [-5, 0, -4], 2, ZERO),
        }),
        "move.walk": anim(0.6, {
            "right_leg": rot(0, [35, 0, 0], 0.3, [-35, 0, 0], 0.6, [35, 0, 0]),
            "left_leg": rot(0, [-35, 0, 0], 0.3, [35, 0, 0], 0.6, [-35, 0, 0]),
            "right_arm": rot(0, [-35, 0, 5], 0.3, [20, 0, 5], 0.6, [-35, 0, 5]),
            "left_arm": rot(0, [20, 0, -5], 0.3, [-35, 0, -5], 0.6, [20, 0, -5]),
            "body": rot(0, [8, 4, 0], 0.15, [10, 0, 0], 0.3, [8, -4, 0], 0.45, [10, 0, 0], 0.6, [8, 4, 0]),
            "head": rot(0, [-6, 0, 0], 0.15, [-2, 0, 0], 0.3, [-6, 0, 0], 0.45, [-2, 0, 0], 0.6, [-6, 0, 0]),
            "root": pos(0, ZERO, 0.15, [0, 0.6, 0], 0.3, ZERO, 0.45, [0, 0.6, 0], 0.6, ZERO),
        }),
        # A rake with the right claws, then the left.
        "attack.swing": anim(0.4, {
            "right_arm": rot(0, ZERO, 0.12, [-120, 0, 15], 0.22, [-30, 0, -10], 0.4, ZERO),
            "left_arm": rot(0, ZERO, 0.1, [-50, 0, -10], 0.24, [-115, 0, -15], 0.34, [-30, 0, 10], 0.4, ZERO),
            "body": rot(0, ZERO, 0.12, [-4, 10, 0], 0.24, [8, -10, 0], 0.4, ZERO),
            "head": rot(0, ZERO, 0.12, [-12, 0, 0], 0.3, [6, 0, 0], 0.4, ZERO),
        }, loop=False),
    }
    write_model("ash_ghoul", bones, animations, [2, 2.5])


# --- Hollow ---------------------------------------------------------------------------------------------------------
# An undead soldier of the order that kept the Sanctums (DESIGN §3.2): a rusted mail hauberk under a torn tabard of
# faded ember-red with the Sanctum's flame, an open sallet over a mail coif, a dead face with cold pin-prick eyes, a
# small buckler for its guard. Its sword is the held item, drawn at right_item (MobRenderers tunes its angle to this
# arm, so the right arm, its box and the right_item bone stay as in the placeholder).

def hollow():
    plate = Material(IRON, base=2.4, noise=0.6, speck=0.06, edge=0.8, grad=1.0, top=0.8)
    mail_mat = Material(IRON, base=1.8, noise=0.5)

    def rust_spots(face, rng):
        scatter(face, rng, max(1, face.w * face.h // 40), RUST[1])
        if rng.random() < 0.4:
            crack(face, rng, RUST[2], None, 2)

    def hauberk(face, rng):
        mail(face, rng, IRON[1], IRON[3])
        scatter(face, rng, face.w * face.h // 30, RUST[2])

    def face_front(face, rng):
        # Grey dead skin pulled over the skull: dark sockets with cold points, sunken cheeks, a lipless mouth.
        face.rect(1, 2, 2, 2, VOID)
        face.rect(face.w - 3, 2, 2, 2, VOID)
        face.lit(2, 3, COLD, 1.0)
        face.lit(face.w - 3, 3, COLD, 1.0)
        face.set(face.cx - 1, 4, MAIDEN_SKIN[0])     # the nose rotted away
        face.set(face.cx, 4, VOID)
        face.vline(1, 4, 2, BONE[0])
        face.vline(face.w - 2, 4, 2, BONE[0])
        face.hline(2, 6, face.w - 4, VOID)
        for x in range(2, face.w - 2, 2):
            face.set(x, 6, BONE[3])
        face.hline(2, 7, face.w - 4, BONE[1])

    def sallet(face, rng):
        rust_spots(face, rng)
        if face.name == "front":
            # Open below the brow, a nasal bar down the middle.
            for y in range(2, face.h):
                for x in range(1, face.w - 1):
                    face.clear(x, y)
            face.hline(0, 1, face.w, IRON[5])
            face.vline(face.cx, 1, face.h, IRON[4])
        elif face.name == "bottom":
            clear_all(face)
        elif face.side():
            band(face, face.h - 1, IRON[1])
            rivets(face, IRON[5], 3)
        if face.name == "top":
            face.vline(face.cx, 0, face.h, IRON[5])  # ridge

    def coif(face, rng):
        mail(face, rng, IRON[1], IRON[3])
        if face.name == "front":
            open_front(face, 1, 0)
        if face.name in ("top", "bottom"):
            clear_all(face)

    def tabard_deco(face, rng):
        if face.name in ("front", "back"):
            trim(face, BRONZE[2], ("left", "right"))
            ragged_edge(face, rng, "bottom", 3, 0.5)
            holes(face, rng, 2)
            if face.name == "front":
                cx = face.cx
                # The Sanctum's flame, faded: an ember drop over a small brazier.
                face.set(cx, 2, BRONZE[4])
                face.rect(cx - 1, 3, 2, 2, BRONZE[4])
                face.set(cx - 1, 2, BRONZE[3])
                face.hline(cx - 2, 5, 4, BRONZE[2])
                face.set(cx - 1, 6, BRONZE[2])
                face.set(cx, 6, BRONZE[2])
        else:
            clear_all(face)

    def belt(face, rng):
        if face.side():
            face.hline(0, 0, face.w, LEATHER[1])
            face.hline(0, face.h - 1, face.w, LEATHER[0])
        if face.name == "front":
            face.rect(face.cx - 1, 0, 2, 2, IRON[5])
            face.set(face.cx - 1, 1, IRON[2])

    def skirt(face, rng):
        mail(face, rng, IRON[1], IRON[3])
        ragged_edge(face, rng, "bottom", 1, 0.5)

    def sleeve(face, rng):
        hauberk(face, rng)
        lower(face, 4, LEATHER, rng, 2.5)          # leather bracer
        if face.side():
            face.hline(0, face.h - 4, face.w, LEATHER[0])
            face.hline(0, face.h - 1, face.w, IRON[3])  # gauntlet cuff

    def chausses(face, rng):
        mail(face, rng, IRON[1], IRON[3])
        lower(face, 4, LEATHER, rng, 1.8)          # boots
        if face.side():
            face.hline(0, face.h - 5, face.w, LEATHER[0])

    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 24, 0], "root", None, [
            Box([-4, 12, -2], [8, 12, 4], mail_mat, {"all": hauberk}),
            Box([-4.5, 11, -2.5], [9, 3, 5], Material(LEATHER, base=2.4, noise=0.5, grain="h"), {"all": belt}),
            Box([-3, 7.5, -2.6], [6, 15, 0], Material(TABARD, base=2.6, noise=0.6, grain="v"), {"all": tabard_deco}),
            Box([-3, 7.5, 2.6], [6, 15, 0], Material(TABARD, base=2.3, noise=0.6, grain="v"), {"all": tabard_deco}),
            Box([-4.4, 8, -2.4], [8.8, 3, 4.8], mail_mat, {"sides": skirt}),
        ]),
        Bone("head", [0, 24, 0], "root", None, [
            Box([-4, 24, -4], [8, 8, 8], Material(MAIDEN_SKIN, base=1.3, noise=0.5, top=-0.2),
                {"front": face_front}),
            Box([-4.4, 23.5, -4.4], [8.8, 7, 8.8], mail_mat, {"all": coif}),
            Box([-4.6, 28, -4.6], [9.2, 5, 9.2], plate, {"all": sallet}),
            Box([-4.6, 27, 2.6], [9.2, 1, 2.6], plate.but(base=2.0), {"all": rust_spots},
                rotation=[-15, 0, 0], pivot=[0, 28, 2.6]),
        ]),
    ]
    arm = Box([-8, 12, -2], [4, 12, 4], mail_mat, {"all": sleeve})
    pauldron = Box([-8.5, 20, -2.5], [5, 3, 5], plate.but(base=2.8),
                   {"all": lambda f, r: (rust_spots(f, r), band(f, f.h - 1, IRON[1]),
                                         f.hline(0, 0, f.w, IRON[5]) if f.side() else None)})

    def buckler_face(face, rng):
        if face.name in ("left", "right"):
            # A round buckler: a ring of iron, bronze boss, the corners cut.
            for (x, y) in ((0, 0), (face.w - 1, 0), (0, face.h - 1), (face.w - 1, face.h - 1)):
                face.clear(x, y)
            for x in range(1, face.w - 1):
                face.set(x, 0, IRON[2])
                face.set(x, face.h - 1, IRON[1])
            for y in range(1, face.h - 1):
                face.set(0, y, IRON[2])
                face.set(face.w - 1, y, IRON[1])
            face.rect(face.cx - 1, face.h // 2 - 1, 2, 2, BRONZE[4])
            face.set(face.cx - 1, face.h // 2 - 1, BRONZE[5])
            rust_spots(face, rng)

    buckler = Box([8, 13.5, -3], [1, 6, 6], Material(WOOD, base=2.0, noise=0.6, grain="h"), {"all": buckler_face})
    bones += [
        Bone("right_arm", [-5, 22, 0], "root", None, [arm, pauldron]),
        Bone("right_item", [-6, 13, -1], "right_arm"),
        Bone("left_arm", [5, 22, 0], "root", None, [mirror_box(arm), mirror_box(pauldron), buckler]),
    ]
    leg = Box([-3.9, 0, -2], [4, 12, 4], mail_mat, {"all": chausses})
    knee = Box([-3.9, 6, -2.4], [4, 2, 1], plate.but(base=2.8), {"all": rust_spots})
    bones += [Bone("right_leg", [-1.9, 12, 0], "root", None, [leg, knee]),
              Bone("left_leg", [1.9, 12, 0], "root", None, [mirror_box(leg), mirror_box(knee)])]
    block = [-75, -40, 0]
    animations = {
        "misc.idle": anim(3.0, {
            "right_arm": rot(0, [-10, 0, 0], 1.5, [-13, 0, 2], 3, [-10, 0, 0]),
            "left_arm": rot(0, [-5, 0, 0], 1.5, [-8, 0, -2], 3, [-5, 0, 0]),
            "body": rot(0, ZERO, 1.5, [2, 0, 0], 3, ZERO),
            "head": rot(0, [4, 0, 0], 1.5, [8, 4, 0], 3, [4, 0, 0]),
        }),
        "move.walk": anim(1.2, {
            "right_leg": rot(0, [25, 0, 0], 0.6, [-25, 0, 0], 1.2, [25, 0, 0]),
            "left_leg": rot(0, [-25, 0, 0], 0.6, [25, 0, 0], 1.2, [-25, 0, 0]),
            "right_arm": rot(0, [-20, 0, 0], 0.6, [-5, 0, 0], 1.2, [-20, 0, 0]),
            "left_arm": rot(0, [15, 0, 0], 0.6, [-15, 0, 0], 1.2, [15, 0, 0]),
            "body": rot(0, [3, 3, 0], 0.6, [3, -3, 0], 1.2, [3, 3, 0]),
            "root": pos(0, ZERO, 0.3, [0, 0.4, 0], 0.6, ZERO, 0.9, [0, 0.4, 0], 1.2, ZERO),
        }),
        # Raised by 0.3 s, held to the hit at 0.4 s (HollowEntity.WINDUP_TICKS = 8), the blade down by 0.45 s.
        "attack.swing": anim(0.9, {
            "right_arm": rot(0, ZERO, 0.3, [-165, 0, 10], 0.38, [-170, 0, 12], 0.45, [-40, 0, 0], 0.9, ZERO),
            "left_arm": rot(0, ZERO, 0.3, [-30, 0, -15], 0.45, [10, 0, -5], 0.9, ZERO),
            "body": rot(0, ZERO, 0.3, [-5, 15, 0], 0.45, [10, -20, 0], 0.9, ZERO),
            "head": rot(0, ZERO, 0.3, [-8, 0, 0], 0.45, [10, 0, 0], 0.9, ZERO),
        }, loop=False),
        "attack.block": anim(1.0, {
            "right_arm": rot(0, block, 1, block),
            "left_arm": rot(0, [-50, 30, 0], 1, [-50, 30, 0]),
            "body": rot(0, [4, 10, 0], 1, [4, 10, 0]),
            "head": rot(0, [8, 0, 0], 1, [8, 0, 0]),
        }),
    }
    write_model("hollow", bones, animations, [2, 3])


# --- Ember Wisp -----------------------------------------------------------------------------------------------------
# A loose ember of the Ember drifting in the dark under the ground (DESIGN §3.2): a white-hot core with two dark
# eyes inside a licking shell of translucent flame, a flickering crown of tongues and three sparks circling it.
# Drawn translucent and full bright (MobRenderers), so the flame's alpha is its body.

def ember_wisp():
    core = Material(CORE, base=1.6, noise=0.6, grad=0.8, top=1.0, bottom=-0.6)

    def core_front(face, rng):
        # Two dark eyes in the white heat.
        face.vline(0, 1, 2, EMBER_DARK)
        face.vline(face.w - 1, 1, 2, EMBER_DARK)
        face.set(1, 2, EMBER_DEEP)
        face.set(face.w - 2, 2, EMBER_DEEP)

    def flame_mat(base):
        # Hotter at the bottom: the gradient runs the other way (grad < 0).
        return Material(FLAME, base=base, noise=0.8, grain="v", grad=-1.6, top=1.0, bottom=1.4)

    bones = [
        Bone("root", [0, 0, 0]),
        Bone("core", [0, 4, 0], "root", None, [
            Box([-2, 2, -2], [4, 4, 4], core, {"front": compose(core_front, glow_all()),
                                               "back": glow_all(), "left": glow_all(), "right": glow_all(),
                                               "top": glow_all(), "bottom": glow_all()}),
        ]),
        Bone("shell", [0, 4, 0], "root", None, [
            Box([-3, 1, -3], [6, 6, 6], flame_mat(2.6),
                {"all": compose(round_corners, lambda f, r: flame_tongues(f, r, 3, 0.55),
                                lambda f, r: clear_all(f) if f.name == "top" else None,
                                fade(105, 185), glow_all(85, 0.9))}),
        ]),
        Bone("orbit", [0, 4, 0], "root", None, [
            Box([4.5, 3.5, -0.5], [1, 1, 1], Material(CORE, base=1.0, noise=0.4), {"all": glow_all()}),
            Box([-5.5, 4.5, -0.5], [1, 1, 1], Material(FLAME, base=4.0, noise=0.4), {"all": glow_all()}),
            Box([-0.5, 2.5, 4.5], [1, 1, 1], Material(FLAME, base=3.4, noise=0.4), {"all": glow_all()}),
        ]),
        Bone("flame", [0, 7, 0], "root", None, [
            Box([-1, 7, -1], [2, 3, 2], flame_mat(3.4),
                {"all": compose(lambda f, r: flame_tongues(f, r, 1, 0.4), fade(170, 230), glow_all(150))}),
            Box([-2.5, 6.5, 0.5], [1, 2, 1], flame_mat(3.0), {"all": compose(fade(150, 210), glow_all(130))}),
            Box([1.5, 6.5, -1.5], [1, 2.5, 1], flame_mat(3.0), {"all": compose(fade(150, 210), glow_all(130))}),
            Box([0.5, 6.5, 1.5], [1, 1.5, 1], flame_mat(3.6), {"all": compose(fade(160, 220), glow_all(140))}),
        ]),
    ]

    def float_anim(length, bob, flicker):
        n = 8
        flame_scale = []
        for i in range(n + 1):
            s = 1.0 + flicker * (0.5 + 0.5 * ((i * 7) % 5) / 4) if i % n else 1.0
            flame_scale += [length * i / n, [1, s, 1]]
        return anim(length, {
            "root": pos(0, ZERO, length / 2, [0, bob, 0], length, ZERO),
            "orbit": rot(0, ZERO, length / 2, [0, 180, 0], length, [0, 360, 0]),
            "shell": rot(0, ZERO, length / 2, [0, -45, 0], length, [0, -90, 0]),
            "flame": {"scale": keys(*flame_scale), "rotation": sway(length, 6, 2)},
            "core": {"scale": keys(0, [1, 1, 1], length / 2, [1.1, 1.1, 1.1], length, [1, 1, 1])},
        })

    animations = {"misc.idle": float_anim(2.0, 1, 0.35), "move.fly": float_anim(1.0, 1.5, 0.5)}
    write_model("ember_wisp", bones, animations, [1, 1], tex_w=32)


# --- Ashen Maiden ---------------------------------------------------------------------------------------------------
# The keeper of the Sanctum who turns souls into strength (DESIGN §2): a tall, quiet figure in a long ash-grey robe
# with an ember-red embroidered hem, a fringed shawl pinned with an ember, a deep hood, a pale face with closed eyes
# under ash tear-marks, and the ember she holds in cupped hands. She is invulnerable and never fights, so nothing in
# her is armour or weapon.

def ashen_maiden():
    robe = Material(MAIDEN_ROBE, base=2.6, noise=0.5, grain="v", grad=0.8, top=0.6)
    dark = robe.but(base=1.6)
    skin = Material(MAIDEN_SKIN, base=3.3, noise=0.2, grad=0.5, top=0.4)

    def hem(face, rng):
        if face.side():
            folds(face)
            # Two rows of ember-red stitching above a dark hem, a little wear.
            face.hline(0, face.h - 1, face.w, MAIDEN_ROBE[0])
            for x in range(face.w):
                face.set(x, face.h - 3, EMBER_DARK if x % 2 else TABARD[3])
                if x % 4 == 1:
                    face.set(x, face.h - 2, TABARD[4])
            ragged_edge(face, rng, "bottom", 1, 0.25)

    def folds(face):
        for x in range(1, face.w - 1, 3):
            face.vline(x, 0, face.h, shade(face.get(x, face.h // 2), -14))

    def bodice(face, rng):
        if face.name == "front":
            # A crossed wrap over the chest, a sash at the waist.
            for y in range(face.h - 3):
                face.set(min(face.w - 1, 1 + y // 2), y, MAIDEN_ROBE[1])
                face.set(max(0, face.w - 2 - y // 2), y, MAIDEN_ROBE[1])
        if face.side():
            face.hline(0, face.h - 2, face.w, TABARD[2])
            face.hline(0, face.h - 1, face.w, TABARD[1])
            if face.name == "front":
                face.set(face.cx - 1, face.h - 1, EMBER_DARK)

    def shawl(face, rng):
        if face.side():
            ragged_edge(face, rng, "bottom", 2, 0.45)  # fringe
            for x in range(0, face.w, 2):
                face.set(x, face.h - 1, MAIDEN_ROBE[1]) if face.get(x, face.h - 1)[3] else None
        if face.name == "front":
            # The clasp: an ember set in iron.
            face.set(face.cx - 1, 1, IRON[3])
            face.set(face.cx, 1, IRON[3])
            face.lit(face.cx - 1, 2, EMBER, 0.9)
            face.lit(face.cx, 2, EMBER_HOT, 0.9)
            face.set(face.cx - 1, 3, IRON[2])
            face.set(face.cx, 3, IRON[2])

    def face_front(face, rng):
        # Dark grey hair parted under the hood, closed eyes with a faint glow between the lashes, ash tear-marks.
        face.hline(0, 0, face.w, HAIR_ASH[1])
        face.hline(0, 1, face.w, HAIR_ASH[2])
        face.set(face.cx, 1, HAIR_ASH[0])
        face.set(0, 2, HAIR_ASH[1])
        face.set(face.w - 1, 2, HAIR_ASH[1])
        for ex in (1, face.w - 3):
            face.hline(ex, 4, 2, MAIDEN_SKIN[1])     # closed lids, lashes at the outer corner
            face.set(ex if ex == 1 else ex + 1, 4, HAIR_ASH[1])
            face.hline(ex, 3, 2, MAIDEN_SKIN[2])
        face.set(1, 5, ASH[3])                       # an ash tear-mark under one eye
        face.set(1, 6, ASH[4])
        face.set(face.cx, 5, MAIDEN_SKIN[1])
        face.hline(face.cx - 1, 6, 2, MAIDEN_SKIN[1])
        face.set(face.cx - 1, 7, TABARD[3])

    def face_back(face, rng):
        face.rect(0, 0, face.w, face.h, HAIR_ASH[1])
        for x in range(0, face.w, 2):
            face.vline(x, 0, face.h, HAIR_ASH[2])

    def hood(face, rng):
        if face.name == "front":
            # Open, the rim turned back a little.
            open_front(face, 1, 2)
            face.hline(1, 1, face.w - 2, MAIDEN_ROBE[4])
        elif face.name == "bottom":
            clear_all(face)
        elif face.side():
            folds(face)

    def sleeve(face, rng):
        if face.side():
            folds(face)
            face.hline(0, face.h - 1, face.w, TABARD[2])

    def cuff(face, rng):
        if face.side():
            face.hline(0, face.h - 1, face.w, TABARD[3])
            ragged_edge(face, rng, "bottom", 1, 0.3)
        if face.name == "bottom":
            clear_all(face)

    bones = [
        Bone("root", [0, 0, 0]),
        Bone("robe", [0, 12, 0], "root", None, [
            Box([-5, 0, -3.5], [10, 13, 7], robe, {"all": hem}),
            Box([-1.5, 0.5, -3.6], [3, 11, 0], dark, {"all": lambda f, r: (
                trim(f, TABARD[3], ("left", "right")) if f.name in ("front", "back") else clear_all(f))}),
        ]),
        Bone("body", [0, 13, 0], "root", None, [
            Box([-4, 13, -2], [8, 11, 4], robe.but(base=2.2), {"all": bodice}),
        ]),
        Bone("head", [0, 24, 0], "body", None, [
            Box([-3.5, 24, -3.5], [7, 8, 7], skin, {"front": face_front, "back": face_back,
                                                     "top": lambda f, r: f.rect(0, 0, f.w, f.h, HAIR_ASH[1])}),
        ]),
        Bone("hood", [0, 24, 0], "head", None, [
            Box([-4.5, 24, -4.5], [9, 9.5, 9], robe.but(base=2.0), {"all": hood}),
            Box([-3.5, 19, 3.5], [7, 6, 1], robe.but(base=1.8), {"all": lambda f, r: (
                folds(f) if f.side() else None, ragged_edge(f, r, "bottom", 1, 0.4))},
                rotation=[12, 0, 0], pivot=[0, 25, 4]),
        ]),
        Bone("mantle", [0, 23, 0], "body", None, [
            Box([-5, 19, -2.5], [10, 5, 5], robe.but(base=3.0), {"all": shawl}),
        ]),
    ]
    arm = Box([-6.5, 12, -1.5], [3, 11, 3], robe, {"all": sleeve})
    bell = Box([-6.8, 12, -1.8], [3.6, 4, 3.6], robe.but(base=2.4), {"all": cuff})
    hand = Box([-6, 10.5, -1], [2, 2, 2], skin, {"bottom": lambda f, r: f.set(0, 0, MAIDEN_SKIN[1])})
    bones += [
        Bone("right_arm", [-4.5, 22, 0], "body", [-45, -25, 12], [arm, bell, hand]),
        Bone("left_arm", [4.5, 22, 0], "body", [-45, 25, -12], [mirror_box(arm), mirror_box(bell), mirror_box(hand)]),
    ]
    ember_core = Material(CORE, base=1.5, noise=0.5)
    bones.append(Bone("ember", [0, 15, -6], "body", None, [
        Box([-1, 14, -7], [2, 2, 2], ember_core, {"all": glow_all()}),
        Box([-0.5, 16, -6.5], [1, 1.5, 1], Material(FLAME, base=3.4, noise=0.5), {"all": glow_all()}),
        Box([-1.5, 13.5, -7.5], [3, 3, 3], Material(FLAME, base=2.4, noise=0.7),
            {"all": compose(round_corners, lambda f, r: holes_clear(f, r, 2), glow_all(200))}),
    ]))
    pulse = keys(0, [1, 1, 1], 1, [1.25, 1.25, 1.25], 2, [1, 1, 1], 3, [1.2, 1.2, 1.2], 4, [1, 1, 1])
    animations = {
        "misc.idle": anim(4.0, {
            "body": rot(0, ZERO, 2, [2, 0, 0], 4, ZERO),
            "head": rot(0, [5, 0, 0], 2, [8, 3, 0], 4, [5, 0, 0]),
            "hood": rot(0, ZERO, 2, [-1, 0, 0], 4, ZERO),
            "right_arm": rot(0, ZERO, 2, [-2, 0, 0], 4, ZERO),
            "left_arm": rot(0, ZERO, 2, [-2, 0, 0], 4, ZERO),
            "ember": {"position": keys(0, ZERO, 2, [0, 0.5, 0], 4, ZERO), "scale": pulse},
        }),
        # Raises the ember toward the player when they talk to her.
        "misc.interact": anim(1.5, {
            "right_arm": rot(0, ZERO, 0.5, [-25, 0, 0], 1.5, ZERO),
            "left_arm": rot(0, ZERO, 0.5, [-25, 0, 0], 1.5, ZERO),
            "ember": {"position": keys(0, ZERO, 0.5, [0, 2, -1], 1.5, ZERO),
                      "scale": keys(0, [1, 1, 1], 0.5, [1.35, 1.35, 1.35], 1.5, [1, 1, 1])},
            "head": rot(0, ZERO, 0.5, [-10, 0, 0], 1.5, ZERO),
            "body": rot(0, ZERO, 0.5, [-3, 0, 0], 1.5, ZERO),
        }, loop=False),
    }
    write_model("ashen_maiden", bones, animations, [1.5, 2.5])


# --- Ashen Zombie ---------------------------------------------------------------------------------------------------
# What the Curse of the Night raises for each death (DESIGN §3.3): a broad dead man gone grey with ash, the skin
# split along smouldering seams, white-hot embers for eyes, a slack jaw, rags of a blue work shirt and trousers,
# arms reaching forward like a vanilla zombie.

def ashen_zombie():
    skin = Material(ZOMBIE, base=2.4, noise=0.8, speck=0.06, grad=1.0, top=1.0, edge=0.3)
    shirt = Material(CLOTH_BLUE, base=2.4, noise=0.6, grain="v")
    trousers = Material(LINEN, base=1.2, noise=0.6, grain="v")

    def ashy(face, rng, n=1, chance=0.8):
        if face.side():
            scatter(face, rng, max(1, face.w * face.h // 20), ZOMBIE[1])
            scatter(face, rng, max(1, face.w * face.h // 40), ZOMBIE[5])
            for _ in range(n):
                if rng.random() < chance:
                    ember_cracks(face, rng, 1, EMBER, EMBER_DEEP, 0.85, (2, 4))

    def head_front(face, rng):
        face.hline(0, 2, face.w, ZOMBIE[1])
        for ex in (1, face.w - 3):
            face.rect(ex, 3, 2, 2, VOID)
        face.lit(2, 3, EMBER_HOT, 1.0)
        face.lit(face.w - 3, 3, EMBER_HOT, 1.0)
        face.lit(1, 4, EMBER_DEEP, 0.6)
        face.lit(face.w - 2, 4, EMBER_DEEP, 0.6)
        face.set(face.cx - 1, 5, ZOMBIE[0])
        face.set(face.cx, 5, ZOMBIE[1])
        face.hline(2, 6, face.w - 4, VOID)          # slack mouth
        face.lit(face.cx - 1, 6, EMBER_DEEP, 0.6)
        face.hline(2, 7, face.w - 4, ZOMBIE[1])
        face.set(2, 6, BONE[3])
        face.set(face.w - 3, 6, BONE[2])
        crack(face, rng, ZOMBIE[0], None, 3, face.w - 2, 0)

    def head_top(face, rng):
        scatter(face, rng, 6, ZOMBIE[0])
        scatter(face, rng, 4, ZOMBIE[5])

    def torso_front(face, rng):
        # The wound under the torn shirt.
        for y in range(5, 10):
            face.lit(face.cx - 1 + (y % 2), y, EMBER if y % 3 else EMBER_HOT, 0.9)
        face.set(face.cx - 2, 7, EMBER_DARK)
        face.set(face.cx + 1, 6, EMBER_DARK)

    def shirt_deco(face, rng):
        if face.side():
            tatter(face, rng, 3, 0.55)
            holes(face, rng, 1)
            scatter(face, rng, 2, CLOTH_BLUE[0])
            if face.name == "front":
                # Torn open down the front.
                for y in range(3, face.h):
                    for x in range(face.cx - 1, face.cx + 1 + (y > 5)):
                        face.clear(x, y)
                face.vline(face.cx - 2, 3, face.h - 3, CLOTH_BLUE[0])
        if face.name in ("top", "bottom"):
            clear_all(face)

    def trouser_deco(face, rng):
        if face.side():
            tatter(face, rng, 2, 0.5)
        if face.name == "bottom":
            clear_all(face)

    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 24, 0], "root", [6, 0, 0], [
            Box([-4.5, 12, -2.5], [9, 12, 5], skin, {"front": torso_front, "back": ashy, "left": ashy,
                                                     "right": ashy}),
            Box([-4.5, 13, -2.5], [9, 11, 5], shirt, {"all": shirt_deco}, inflate=0.3),
            Box([-4.5, 12, -2.5], [9, 1, 5], Material(LEATHER, base=1.4, noise=0.5),
                {"front": lambda f, r: f.set(f.cx, 0, IRON[3])}, inflate=0.45),
            Box([-3.5, 24.2, -2], [3, 1, 3], skin.but(base=4.0), {"all": lambda f, r: scatter(f, r, 2, ZOMBIE[2])}),
            Box([1, 24.2, -1.5], [2, 1, 2], skin.but(base=4.0)),
        ]),
        Bone("head", [0, 24, -1], "root", None, [
            Box([-4, 24, -5], [8, 8, 8], skin.but(base=2.6), {"front": head_front, "top": head_top,
                                                             "back": ashy, "left": ashy, "right": ashy}),
            Box([-3, 22.5, -5.3], [6, 2, 4], skin.but(base=2.0),
                {"front": lambda f, r: (f.hline(0, 0, f.w, VOID), f.set(1, 0, BONE[3]), f.set(4, 0, BONE[2]))}),
        ]),
    ]
    arm = Box([-8.5, 10, -2], [4, 13, 4], skin, {"sides": lambda f, r: ashy(f, r, 1, 0.35)})
    rag = Box([-8.5, 18, -2], [4, 5, 4], shirt, {"all": shirt_deco}, inflate=0.3)
    bones += [
        Bone("right_arm", [-5.5, 22, 0], "root", [-80, 0, 0], [arm, rag]),
        Bone("left_arm", [5.5, 22, 0], "root", [-80, 0, 0], [mirror_box(arm), mirror_box(rag)]),
    ]
    leg = Box([-4.2, 0, -2], [4, 12, 4], skin.but(base=2.0), {"sides": lambda f, r: ashy(f, r, 0)})
    trouser = Box([-4.2, 4, -2], [4, 8, 4], trousers, {"all": trouser_deco}, inflate=0.25)
    bones += [Bone("right_leg", [-2, 12, 0], "root", None, [leg, trouser]),
              Bone("left_leg", [2, 12, 0], "root", None, [mirror_box(leg), mirror_box(trouser)])]
    animations = {
        "misc.idle": anim(3.0, {
            "head": rot(0, ZERO, 1.5, [4, 6, 0], 3, ZERO),
            "right_arm": rot(0, ZERO, 1.5, [-4, 0, 2], 3, ZERO),
            "left_arm": rot(0, ZERO, 1.5, [4, 0, -2], 3, ZERO),
            "body": rot(0, ZERO, 1.5, [2, 0, 0], 3, ZERO),
        }),
        "move.walk": anim(1.0, {
            "right_leg": rot(0, [28, 0, 0], 0.5, [-28, 0, 0], 1, [28, 0, 0]),
            "left_leg": rot(0, [-28, 0, 0], 0.5, [28, 0, 0], 1, [-28, 0, 0]),
            "right_arm": rot(0, [-6, 0, 0], 0.5, [6, 0, 0], 1, [-6, 0, 0]),
            "left_arm": rot(0, [6, 0, 0], 0.5, [-6, 0, 0], 1, [6, 0, 0]),
            "body": rot(0, [0, 4, 2], 0.5, [0, -4, -2], 1, [0, 4, 2]),
            "head": rot(0, [0, -4, -3], 0.5, [0, 4, 3], 1, [0, -4, -3]),
            "root": pos(0, ZERO, 0.25, [0, 0.4, 0], 0.5, ZERO, 0.75, [0, 0.4, 0], 1, ZERO),
        }),
        "attack.swing": anim(0.5, {
            "right_arm": rot(0, ZERO, 0.2, [-40, 0, 0], 0.3, [30, 0, 0], 0.5, ZERO),
            "left_arm": rot(0, ZERO, 0.2, [-40, 0, 0], 0.3, [30, 0, 0], 0.5, ZERO),
            "body": rot(0, ZERO, 0.2, [-4, 0, 0], 0.3, [10, 0, 0], 0.5, ZERO),
            "head": rot(0, ZERO, 0.2, [-8, 0, 0], 0.3, [8, 0, 0], 0.5, ZERO),
        }, loop=False),
    }
    write_model("ashen_zombie", bones, animations, [2, 2.5])


# --- Ghost ----------------------------------------------------------------------------------------------------------
# What the full moon sends after a player with no curse (DESIGN §3.3): a pale spectre in a torn burial shroud that
# thins into a trailing wisp, a hood with only dark and two cold eyes inside, long sleeves with bony fingers
# reaching out, a broken shackle on one wrist. Drawn translucent, at least half lit (NightMobClient): the shroud
# fades from almost solid at the shoulders to faint at the wisp's tip, and keeps a faint glow of its own.

def ghost():
    shroud = Material(GHOST_PALE, base=3.0, noise=0.6, grain="v", grad=0.8, top=0.8)

    def drape(face, rng):
        if face.side():
            for x in range(1, face.w - 1, 3):
                face.vline(x, 0, face.h, shade(face.get(x, face.h // 2), -18))

    def body_deco(face, rng):
        drape(face, rng)
        holes(face, rng, 1) if face.name == "back" else None

    def mantle(face, rng):
        drape(face, rng)
        if face.side():
            ragged_edge(face, rng, "bottom", 2, 0.55)
        if face.name == "bottom":
            clear_all(face)

    def tail_deco(face, rng):
        drape(face, rng)
        if face.side():
            ragged_edge(face, rng, "left", 1, 0.3)
            ragged_edge(face, rng, "right", 1, 0.3)
        if face.name == "top":
            clear_all(face)

    def wisp_deco(face, rng):
        if face.side():
            ragged_edge(face, rng, "bottom", 2, 0.6)
            ragged_edge(face, rng, "left", 1, 0.4)
        if face.name == "top":
            clear_all(face)

    def face_front(face, rng):
        # Only dark inside the hood: two cold eyes and a faint open mouth.
        face.rect(0, 0, face.w, face.h, GHOST_DARK[0])
        face.lit(1, 3, GHOST_EYE, 1.0)
        face.lit(2, 3, GHOST_EYE, 1.0)
        face.lit(face.w - 3, 3, GHOST_EYE, 1.0)
        face.lit(face.w - 2, 3, GHOST_EYE, 1.0)
        face.lit(face.cx - 1, 6, COLD_DIM, 0.45)     # a faint open mouth
        face.lit(face.cx, 6, COLD_DIM, 0.45)

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
            ragged_edge(face, rng, "bottom", 3, 0.5)
        if face.name == "bottom":
            clear_all(face)

    def fingers_deco(face, rng):
        if face.side():
            face.vline(0, 0, face.h, BONE[2])

    solid = fade(215, 175, 70, 55)
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 14, 0], "root", [10, 0, 0], [
            Box([-4, 14, -2], [8, 11, 4], shroud, {"all": compose(body_deco, solid)}),
            Box([-4.5, 20, -2.5], [9, 5, 5], shroud.but(base=3.4), {"all": compose(mantle, fade(225, 200, 70))},
                inflate=0.2),
        ]),
        Bone("tail", [0, 14, 0], "body", None, [
            Box([-3, 8, -1.5], [6, 6, 3], shroud.but(base=2.6), {"all": compose(tail_deco, fade(175, 120, 55, 35))}),
        ]),
        Bone("wisp", [0, 8, 0], "tail", None, [
            Box([-1.5, 3, -1], [3, 5, 2], shroud.but(base=2.2), {"all": compose(wisp_deco, fade(115, 50, 35, 15))}),
        ]),
        Bone("head", [0, 25, -1], "root", None, [
            Box([-3.5, 25, -4.5], [7, 7, 7], Material(GHOST_DARK, base=0.8, noise=0.4),
                {"front": face_front, "all": fade(235)}),
            Box([-4, 25.5, -5], [8, 7.5, 8], shroud.but(base=3.2), {"all": compose(hood_deco, fade(225, 200, 70))}),
            Box([-1, 32, -1], [2, 2, 4], shroud.but(base=3.6), {"all": fade(220, 200, 60)},
                rotation=[25, 0, 0], pivot=[0, 32, 3]),
        ]),
    ]
    arm = Box([-7, 12, -2], [3, 12, 3], shroud, {"all": compose(sleeve, fade(210, 160, 60, 45))}, inflate=0.15)
    fingers = [Box([-6.8 + 1.1 * i, 9, -1.5 + (i == 1) * 0.5], [0.8, 3.5, 0.8], Material(BONE, base=2.6, noise=0.4),
                   {"all": compose(fingers_deco, fade(235))}) for i in range(3)]
    shackle = Box([-7.3, 13.5, -2.3], [3.6, 1.5, 3.6], Material(IRON, base=3.4, noise=0.6, speck=0.1, top=0.6),
                  {"all": lambda f, r: scatter(f, r, 1, RUST[3])})
    chain = Box([-6, 11, -2.6], [1, 2.5, 1], Material(IRON, base=3.6, noise=0.5),
                {"sides": lambda f, r: f.set(0, 1, IRON[2])}, rotation=[15, 0, 0], pivot=[-5.5, 13.5, -2.2])
    bones += [
        Bone("right_arm", [-5, 24, -1], "root", [-70, 0, 0], [arm, shackle, chain] + fingers),
        Bone("left_arm", [5, 24, -1], "root", [-70, 0, 0], [mirror_box(arm)] + [mirror_box(f) for f in fingers]),
    ]

    def hover(length, bob, sway_deg):
        return anim(length, {
            "root": pos(0, ZERO, length / 2, [0, bob, 0], length, ZERO),
            "tail": rot(0, [sway_deg, 0, 0], length / 2, [-sway_deg, 0, 0], length, [sway_deg, 0, 0]),
            "wisp": rot(0, [-sway_deg, 0, 0], length / 2, [sway_deg, 0, 0], length, [-sway_deg, 0, 0]),
            "right_arm": rot(0, ZERO, length / 2, [-6, 0, 3], length, ZERO),
            "left_arm": rot(0, ZERO, length / 2, [-6, 0, -3], length, ZERO),
            "head": rot(0, ZERO, length / 2, [4, 0, 3], length, ZERO),
        })

    animations = {
        "misc.idle": hover(3.0, 1, 8),
        "move.fly": hover(1.5, 1.5, 18),
        "attack.swing": anim(0.5, {
            "right_arm": rot(0, ZERO, 0.15, [-50, 0, 15], 0.3, [40, 0, -10], 0.5, ZERO),
            "left_arm": rot(0, ZERO, 0.15, [-50, 0, -15], 0.3, [40, 0, 10], 0.5, ZERO),
            "head": rot(0, ZERO, 0.15, [-10, 0, 0], 0.3, [15, 0, 0], 0.5, ZERO),
            "body": rot(0, ZERO, 0.15, [-5, 0, 0], 0.3, [8, 0, 0], 0.5, ZERO),
        }, loop=False),
    }
    write_model("ghost", bones, animations, [2, 3])


# --- Vampire --------------------------------------------------------------------------------------------------------
# The blood new moon's guest, friendly until struck (DESIGN §3.3): a tall pale noble with slicked black hair and a
# widow's peak, red eyes, a thin smile over two fangs, a burgundy brocade doublet with gold buttons, a lace jabot and
# cuffs, a crimson sash, black breeches and riding boots, and a high-collared black cloak lined with crimson.
# misc.transform (2.5 s = VampireEntity.TURN_TICKS, held) lifts it off the ground with the cloak flaring.

def noble_face(face, eye, eye_strength, mouth):
    """Eyes, brows, nose and a fanged mouth on an 8-wide front face (rows 0-2 are the hairline)."""
    face.rect(0, 0, face.w, 2, HAIR_BLACK[1])
    face.set(face.cx - 1, 2, HAIR_BLACK[1])        # widow's peak
    face.set(face.cx, 2, HAIR_BLACK[1])
    face.hline(1, 2, 2, HAIR_BLACK[2])             # brows
    face.hline(face.w - 3, 2, 2, HAIR_BLACK[2])
    for ex in (1, face.w - 3):
        face.set(ex, 3, VAMP_SKIN[5] if eye_strength < 1 else VOID)
        face.lit(ex + (1 if ex == 1 else 0), 3, eye, eye_strength)
        face.set(ex + (0 if ex == 1 else 1), 3, VAMP_SKIN[5] if eye_strength < 1 else BLOOD_DIM)
        face.hline(ex, 4, 2, shade(face.get(ex, 4), -14))  # shadow under the eyes
    face.set(face.cx - 1, 5, shade(face.get(face.cx - 1, 5), -18))
    face.hline(face.cx - 2, 6, 4, mouth)
    face.set(face.cx - 2, 7, FANG)
    face.set(face.cx + 1, 7, FANG)


def vampire():
    skin = Material(VAMP_SKIN, base=2.6, noise=0.3, grad=0.6, top=0.5)
    doublet = Material(BURGUNDY, base=2.4, noise=0.5, grad=0.8)
    black = Material(NOBLE_BLACK, base=2.4, noise=0.5, grain="v", grad=0.8)
    lining = Material(VCRIMSON, base=2.6, noise=0.5, grain="v", grad=0.6)

    def brocade(face, rng):
        if face.side():
            for y in range(1, face.h - 2, 3):
                for x in range((y // 3) % 2, face.w, 2):
                    face.set(x, y, BURGUNDY[4])
        if face.name == "front":
            face.vline(face.cx - 1, 0, face.h, BURGUNDY[0])
            for y in range(1, face.h - 3, 2):
                face.set(face.cx, y, GOLD[3])
        if face.side():
            face.hline(0, face.h - 3, face.w, VCRIMSON[3])   # sash
            face.hline(0, face.h - 2, face.w, VCRIMSON[2])
            face.hline(0, face.h - 1, face.w, NOBLE_BLACK[2])
            if face.name == "left":
                face.vline(1, face.h - 3, 3, VCRIMSON[5])   # the sash's knot

    def head_front(face, rng):
        noble_face(face, BLOOD, 0.75, VCRIMSON[4])

    def hair(face, rng):
        if face.name == "front":
            for y in range(face.h):
                for x in range(face.w):
                    if y > 1 and not (y == 2 and face.cx - 1 <= x <= face.cx):
                        face.clear(x, y)
        elif face.name in ("left", "right"):
            # Swept back over the ears; the front corner (the cheek) stays bare.
            for y in range(2, face.h):
                for k in range(3 if y > 2 else 2):
                    face.clear(k if face.name == "right" else face.w - 1 - k, y)
        elif face.name == "bottom":
            clear_all(face)
        if face.name == "top":
            for y in range(1, face.h, 2):                   # combed back: sheen lines front to back
                face.vline(y, 0, face.h, HAIR_BLACK[3])
        elif face.name in ("back", "left", "right"):
            for x in range(0, face.w, 2):
                face.vline(x, 0, face.h, HAIR_BLACK[2])

    def cloak_deco(face, rng):
        if face.name == "back":
            for x in range(1, face.w, 3):
                face.vline(x, 0, face.h, NOBLE_BLACK[1])
            face.hline(0, face.h - 1, face.w, GOLD[1])
        if face.name in ("left", "right", "bottom"):
            face.rect(0, 0, face.w, face.h, VCRIMSON[1])

    def collar_deco(face, rng):
        if face.name == "front":
            face.rect(0, 0, face.w, face.h, VCRIMSON[3])
            for y in range(face.h):
                face.set(0, y, NOBLE_BLACK[3])
                face.set(face.w - 1, y, NOBLE_BLACK[3])
        elif face.name in ("back", "left", "right", "top"):
            trim(face, NOBLE_BLACK[0], ("top",)) if face.side() else None

    def ring_deco(face, rng):
        if face.name == "front":
            for y in range(face.h):
                for x in range(2, face.w - 2):
                    face.clear(x, y)
        if face.name == "top":
            face.rect(0, 0, face.w, face.h, VCRIMSON[2])
            for y in range(face.h - 2):
                for x in range(1, face.w - 1):
                    face.clear(x, y + 1)
        if face.name == "bottom":
            clear_all(face)

    def jabot(face, rng):
        for y in range(face.h):
            if y % 2:
                face.hline(0, y, face.w, LACE[1])
        if face.name == "front":
            face.set(face.cx, 1, BLOOD_DIM)              # a ruby pin

    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 24, 0], "root", None, [
            Box([-4, 12, -2], [8, 12, 4], doublet, {"all": brocade}),
            Box([-4.5, 23.5, -2.5], [9, 2, 5], black, {"all": ring_deco}),
            Box([-5.5, 24, 1.5], [11, 7, 1], black, {"all": collar_deco}, rotation=[-20, 0, 0], pivot=[0, 24, 2]),
            Box([-1.5, 20, -2.6], [3, 4, 1], Material(LACE, base=3.0, noise=0.4), {"all": jabot}),
        ]),
        Bone("cloak", [0, 24, 2], "body", [6, 0, 0], [
            Box([-5, 3, 2], [10, 21, 1], black, {"front": lambda f, r: f.rect(0, 0, f.w, f.h, VCRIMSON[2]),
                                                 "all": cloak_deco}),
        ]),
        Bone("head", [0, 24, 0], "root", None, [
            Box([-4, 24, -4], [8, 8, 8], skin, {"front": head_front,
                                                "back": lambda f, r: f.rect(0, 0, f.w, f.h - 1, HAIR_BLACK[1])}),
            Box([-4.3, 27, -4.3], [8.6, 5.3, 8.6], Material(HAIR_BLACK, base=1.6, noise=0.5, top=0.5), {"all": hair}),
        ]),
    ]
    sleeve = Box([-7, 11, -1.5], [3, 13, 3], doublet,
                 {"all": lambda f, r: lower(f, 2, VAMP_SKIN, r, 2.6),
                  "bottom": lambda f, r: f.rect(0, 0, f.w, f.h, VAMP_SKIN[2])})
    cuff = Box([-7.2, 13.5, -1.7], [3.4, 1.5, 3.4], Material(LACE, base=1.8, noise=0.4),
               {"sides": lambda f, r: ragged_edge(f, r, "bottom", 1, 0.5), "bottom": clear_all, "top": clear_all})
    bones += [
        Bone("right_arm", [-5, 22, 0], "root", None, [sleeve, cuff]),
        Bone("left_arm", [5, 22, 0], "root", None, [mirror_box(sleeve), mirror_box(cuff)]),
    ]

    def boot(face, rng):
        if face.side():
            for y in range(face.h - 6, face.h):
                for x in range(face.w):
                    face.set(x, y, NOBLE_BLACK[3] if x == 1 else NOBLE_BLACK[1])
            face.hline(0, face.h - 7, face.w, NOBLE_BLACK[4])   # the turned-down top
            face.hline(0, face.h - 1, face.w, NOBLE_BLACK[0])
            if face.name == "front":
                face.set(face.cx, face.h - 3, SILVER[4])

    leg = Box([-4, 0, -2], [4, 12, 4], black.but(base=1.8), {"all": boot})
    bones += [Bone("right_leg", [-2, 12, 0], "root", None, [leg]),
              Bone("left_leg", [2, 12, 0], "root", None, [mirror_box(leg)])]
    animations = {
        "misc.idle": anim(4.0, {
            "body": rot(0, ZERO, 2, [2, 0, 0], 4, ZERO),
            "cloak": rot(0, ZERO, 2, [4, 0, 0], 4, ZERO),
            "head": rot(0, [-3, 0, 0], 2, [-1, 6, 0], 4, [-3, 0, 0]),
            "right_arm": rot(0, [-8, 0, 4], 2, [-12, 0, 5], 4, [-8, 0, 4]),
            "left_arm": rot(0, [-8, 0, -4], 2, [-12, 0, -5], 4, [-8, 0, -4]),
        }),
        "move.walk": anim(1.4, {
            "right_leg": rot(0, [22, 0, 0], 0.7, [-22, 0, 0], 1.4, [22, 0, 0]),
            "left_leg": rot(0, [-22, 0, 0], 0.7, [22, 0, 0], 1.4, [-22, 0, 0]),
            "right_arm": rot(0, [-14, 0, 4], 0.7, [10, 0, 4], 1.4, [-14, 0, 4]),
            "left_arm": rot(0, [10, 0, -4], 0.7, [-14, 0, -4], 1.4, [10, 0, -4]),
            "cloak": rot(0, [12, 0, 0], 0.35, [16, 0, 2], 0.7, [12, 0, 0], 1.05, [16, 0, -2], 1.4, [12, 0, 0]),
            "root": pos(0, ZERO, 0.35, [0, 0.3, 0], 0.7, ZERO, 1.05, [0, 0.3, 0], 1.4, ZERO),
        }),
        # 2.5 s = VampireEntity.TURN_TICKS: rises off the ground, arms spread, head thrown back, then held.
        "misc.transform": anim(2.5, {
            "root": pos(0, ZERO, 0.4, [0, -0.5, 0], 1.5, [0, 3, 0], 2.5, [0, 5, 0]),
            "body": rot(0, ZERO, 0.4, [12, 0, 0], 1.2, [-8, 0, 0], 2.5, [-4, 0, 0]),
            "head": rot(0, ZERO, 0.4, [20, 0, 0], 1.2, [-35, 0, 0], 2.5, [-20, 0, 0]),
            "right_arm": rot(0, ZERO, 0.4, [-20, 0, 10], 1.2, [0, 0, 110], 2.5, [-30, 0, 130]),
            "left_arm": rot(0, ZERO, 0.4, [-20, 0, -10], 1.2, [0, 0, -110], 2.5, [-30, 0, -130]),
            "cloak": rot(0, ZERO, 1.5, [40, 0, 0], 2.0, [62, 0, 4], 2.5, [70, 0, 0]),
            "right_leg": rot(0, ZERO, 2.5, [10, 0, 6]),
            "left_leg": rot(0, ZERO, 2.5, [10, 0, -6]),
        }, loop=False),
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
