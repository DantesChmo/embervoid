#!/usr/bin/env python3
"""Final models, animations and textures for the 14 Ashen Reach mobs (PLAN R40.2b, R40.2c).

Run from the repository root: python3 tools/reach_mob_art.py [name ...]
Replaces the greybox from tools/mob_placeholders.py at the same size (tools/model_bounds.json), with the bone names
the code uses (head, toggled bones) and the same animation names and windup timings: the windup of a telegraphed
move ends where the entity class lands it (DESIGN §4.5 ticks / 20 = seconds). Check with
    python3 tools/model_preview.py check <name> && python3 tools/model_preview.py entity <name>

Look: a burnt kingdom under eternal dusk. Charred, ash-grey and rust tones a little lighter than the terrain
(ash soil #302d2c, ash stone #3e3a3a) so mobs read against it, embers as the only saturated colour, and each zone
with its accent: Fields straw and linen, Woods bark, moss and lantern amber, Necropolis bone, bronze and cold blue.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from art_kit import (ZERO, Bone, Box, Material, anim, chain_bones, chain_names, clamp, crack, e, ember_cracks,  # noqa: E402
                     fingers, hexc, hold, keys, lag_sway, merge, mirror_box, pos, ramp, rot, scatter, shade, stitch,
                     sway, tatter, whip, write_model)
import mob_kit as mk  # noqa: E402

# --- Palettes (dark to light) -------------------------------------------------------------------------------------

CHAR = ramp("1a1412", "271e1a", "362a23", "47372d", "5b483b", "74604f")
ASH = ramp("3a3634", "4d4844", "625c56", "7a736b", "948c83", "aea69c")
LINEN = ramp("3a3128", "4d4235", "625543", "786a54", "8f7f65", "a69577")
STRAW = ramp("3f3020", "56432a", "6e5836", "887043", "a08752", "b89f66")
WOOD = ramp("33291f", "463a2d", "5b4c3c", "715f4c", "88745e")
IRON = ramp("26272b", "37393e", "4a4d53", "5f6369", "777b81", "92969b", "b0b4b8")
RUST = ramp("3e2216", "5a321e", "784528", "955a34", "ad7243")
BONE = ramp("5e5544", "7c725c", "9a8f75", "b5aa8e", "ccc2a6", "e0d8c0")
LEATHER = ramp("2a1c14", "3b281c", "4f3726", "654832", "7c5a40", "93704f")
ROPE = ramp("47361f", "614c2f", "7c6340", "967c53", "ad9467")
BARK = ramp("1f1712", "2c221a", "3b2e23", "4c3c2e", "5e4b3a", "725d49")
MOSS = ramp("27301f", "35422a", "455636", "586b45")
BRONZE = ramp("3f2b14", "5c3f1d", "7d5928", "9e7535", "bd9145", "d9ad5c")
SHROUD = ramp("1c191f", "27232b", "332e38", "403a46", "4f4856", "605867")
FEATHER = ramp("141114", "1e1a1e", "2a2529", "37313a", "4a4350", "625a68")
DUST = ramp("3a3229", "4f453a", "665a4b", "7f715f", "988a75", "b2a38c")
SKIN_DEAD = ramp("39392e", "4a4a3b", "5d5c49", "716f59", "86836a", "9b977c")
CLOTH_GREEN = ramp("1f241b", "2b3224", "39412f", "48523b", "586347")

EMBER_HOT = hexc("ffd27a")
EMBER = hexc("ff8a2a")
EMBER_DEEP = hexc("d8521c")
EMBER_DARK = hexc("7a2a12")
COLD = hexc("9fd4ff")
COLD_DIM = hexc("4f7ea8")
AMBER = hexc("ffb347")
VOID = hexc("0c0a0a")


# --- Shared builders ----------------------------------------------------------------------------------------------

def humanoid(legs=12, body=(8, 12, 4), head=(8, 8, 8), arm=(4, 12, 4), leg=(4, 4), hunch=0, mats=None,
             decals=None, arm_gap=0.0):
    """Root; legs on root; body (pivot at the hips, pitched by hunch) carrying head and arms. Left limbs mirror the
    right ones' texture. Returns the bone list and the shoulder height."""
    mats = mats or {}
    decals = decals or {}
    bw, bh, bd = body
    hw, hh, hd = head
    aw, ah, ad = arm
    lw, ld = leg
    top = legs + bh
    ax = bw / 2 + aw / 2 + arm_gap
    torso = Box([-bw / 2, legs, -bd / 2], [bw, bh, bd], mats["body"], decals.get("body"))
    skull = Box([-hw / 2, top, -hd / 2], [hw, hh, hd], mats["head"], decals.get("head"))
    r_arm = Box([-ax - aw / 2, top - ah, -ad / 2], [aw, ah, ad], mats["arm"], decals.get("arm"))
    r_leg = Box([-lw, 0, -ld / 2], [lw, legs, ld], mats["leg"], decals.get("leg"))
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, legs, 0], "root", [hunch, 0, 0] if hunch else None, [torso]),
        Bone("head", [0, top, 0], "body", None, [skull]),
        Bone("right_arm", [-ax, top - 2, 0], "body", None, [r_arm]),
        Bone("left_arm", [ax, top - 2, 0], "body", None, [mirror_box(r_arm)]),
        Bone("right_leg", [-lw / 2, legs, 0], "root", None, [r_leg]),
        Bone("left_leg", [lw / 2, legs, 0], "root", None, [mirror_box(r_leg)]),
    ]
    return bones, top


def bone(bones, name):
    return next(b for b in bones if b.name == name)


def walk(length=1.0, leg=30, arm=20, bob=0.5, **extra):
    h = length / 2
    q = length / 4
    bones = {
        "right_leg": rot(0, [leg, 0, 0], h, [-leg, 0, 0], length, [leg, 0, 0]),
        "left_leg": rot(0, [-leg, 0, 0], h, [leg, 0, 0], length, [-leg, 0, 0]),
        "right_arm": rot(0, [-arm, 0, 0], h, [arm, 0, 0], length, [-arm, 0, 0]),
        "left_arm": rot(0, [arm, 0, 0], h, [-arm, 0, 0], length, [arm, 0, 0]),
    }
    if bob:
        bones["root"] = pos(0, ZERO, q, [0, bob, 0], h, ZERO, h + q, [0, bob, 0], length, ZERO)
    return anim(length, merge(bones, extra))


def idle(length=3.0, sway_deg=4, breathe=2, **extra):
    h = length / 2
    bones = {
        "head": rot(0, ZERO, h, [sway_deg, sway_deg, 0], length, ZERO),
        "right_arm": rot(0, ZERO, h, [-sway_deg, 0, sway_deg / 2], length, ZERO),
        "left_arm": rot(0, ZERO, h, [-sway_deg, 0, -sway_deg / 2], length, ZERO),
    }
    if breathe:
        bones["body"] = rot(0, ZERO, h, [breathe, 0, 0], length, ZERO)
    return anim(length, merge(bones, extra))


def overhead(windup, length, arms=("right_arm",), body=True, lift=-160):
    """Arm(s) raised over the head until windup (the telegraph), brought down just after it."""
    bones = {a: rot(0, ZERO, windup * 0.7, [lift * 0.9, 0, 12 if a == "right_arm" else -12], windup,
                    [lift, 0, 10 if a == "right_arm" else -10], windup + 0.1, [-25, 0, 0], length, ZERO)
             for a in arms}
    if body:
        bones["body"] = rot(0, ZERO, windup, [-10, 0, 0], windup + 0.1, [18, 0, 0], length, ZERO)
    return anim(length, bones, loop=False)


# --- Face decals ----------------------------------------------------------------------------------------------------

def eyes(face, y, color, x_left=1, gap=None, w=2, socket=None, strength=1.0, pupil=None):
    """Two eyes on a front face: sockets (optional), then lit pixels."""
    gap = face.w - 2 * x_left - 2 * w if gap is None else gap
    for ex in (x_left, x_left + w + gap):
        if socket:
            face.rect(ex - (1 if socket == "wide" else 0), y - 1, w + (2 if socket == "wide" else 0), 3,
                      VOID if socket in (True, "wide") else socket)
        if pupil is not None:
            face.lit(ex + pupil, y, color, strength)
        else:
            for k in range(w):
                face.lit(ex + k, y, color, strength)


def ribs(face, y0, count, color, gap_color=None, x0=1, x1=None, glow=None):
    x1 = face.w - 1 if x1 is None else x1
    for i in range(count):
        y = y0 + i * 2
        face.hline(x0, y, x1 - x0, color)
        if gap_color is not None:
            for x in range(x0 + 1, x1 - 1):
                if glow is not None:
                    face.lit(x, y + 1, gap_color, glow)
                else:
                    face.set(x, y + 1, gap_color)
    face.vline(face.w // 2 - (1 if face.w % 2 == 0 else 0), y0, count * 2, shade(color, -30))


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


def char_cracks(face, rng, n=3, length=(2, 3)):
    ember_cracks(face, rng, n, EMBER, EMBER_DEEP, 0.85, length)


# --- Fields: Cinder Husk ---------------------------------------------------------------------------------------------
# A burnt peasant with a scythe: charred flesh split by ember seams, a torn linen smock over a rope belt with tatters
# at the hem, the remains of a straw hat with a tuft hanging off the brim, bare feet, a jaw that hangs open. Swing:
# 10-tick raised-arm windup (0.5 s), the scythe brought down and across.

def cinder_husk():
    flesh = Material(mk.CHAR, base=3.0, noise=0.5, speck=0.03, grad=1.0, top=1.0, bevel=0.5)

    def flesh_cracks(face, rng):
        if face.side():
            if rng.random() < 0.85:
                char_cracks(face, rng, 1, (2, 4))
            mk.edge_shadow(face, rng, -10)

    def head_front(face, rng):
        # Sunken sockets with ember eyes under a heavy brow, a nose hole, a cheek split by a glowing crack.
        cx = face.cx
        face.hline(cx - 3, 1, 6, mk.CHAR[1])
        for ex in (cx - 3, cx + 1):
            face.rect(ex, 2, 2, 2, VOID)
            face.lit(ex + (1 if ex < cx else 0), 2, EMBER_HOT, 1.0)
            face.lit(ex + (0 if ex < cx else 1), 3, EMBER_DEEP, 0.5)
        face.set(cx - 1, 4, mk.CHAR[1])
        face.set(cx, 4, mk.CHAR[0])
        face.lit(cx + 2, 4, EMBER, 0.8)
        face.lit(cx + 2, 5, EMBER_DEEP, 0.6)
        face.set(cx - 3, 5, mk.CHAR[0])

    def jaw_front(face, rng):
        face.hline(0, 0, face.w, mk.CHAR[0])
        for x in range(0, face.w, 2):
            face.set(x, 0, mk.BONE[4])
        face.hline(0, 1, face.w, mk.CHAR[1])

    def jaw_top(face, rng):
        face.rect(0, 0, face.w, face.h, VOID)
        for y in range(1, face.h - 1):
            for x in range(1, face.w - 1):
                face.lit(x, y, EMBER if (x + y) % 2 else EMBER_DEEP, 0.6)

    def torso_front(face, rng):
        # The ribcage shows through the open smock: rib ridges with the fire living between them.
        cx = face.cx
        for i, y in enumerate((3, 5, 7, 9)):
            face.hline(cx - 2, y, 4, mk.CHAR[5])
            face.hline(cx - 2, y + 1, 4, mk.CHAR[0])
            face.lit(cx - 1 + (i % 2), y + 1, EMBER if i != 1 else EMBER_HOT, 0.95)
            face.lit(cx + (i % 2), y + 1, EMBER_DARK, 0.4)
        face.vline(cx - 1, 2, 9, mk.CHAR[1])
        mk.edge_shadow(face, rng, -10)

    bones, top = mk.dense_humanoid(legs=12, body=(8, 12, 4), head=(8, 8, 8), arm=(3, 10, 3), leg=(4, 4), hunch=8,
                                   mats={"body": flesh, "head": flesh.but(base=3.3), "arm": flesh, "leg": flesh,
                                         "hand": flesh.but(base=2.4), "foot": flesh.but(base=2.2)},
                                   decals={"body": {"front": torso_front, "sides": flesh_cracks, "back": flesh_cracks},
                                           "head": {"front": head_front, "back": flesh_cracks, "left": flesh_cracks,
                                                    "right": flesh_cracks},
                                           "arm": {"sides": flesh_cracks}, "fore": {"sides": flesh_cracks},
                                           "leg": {"sides": flesh_cracks}, "shin": {"sides": flesh_cracks}})
    head = mk.bone(bones, "head")
    bones.append(Bone("jaw", [0, top + 1, -1], "head", [8, 0, 0], [
        Box([-3, top - 1, -4.3], [6, 2, 4], flesh.but(base=2.2), {"front": jaw_front, "top": jaw_top,
                                                                 "sides": lambda f, r: mk.edge_shadow(f, r, -10)})]))
    # The straw hat: an eight-sided crown, a leather band, a wide brim burnt through, a tuft of straw hanging off it.
    straw = Material(mk.STRAW, base=3.2, noise=0.6, grain="h", grad=0.6, top=0.6, bevel=0.4)

    def brim(face, rng):
        if face.name in ("top", "bottom"):
            holes(face, rng, 6)
            ragged_edge(face, rng, "top", 2, 0.4)
            ragged_edge(face, rng, "bottom", 2, 0.4)
            ragged_edge(face, rng, "left", 1, 0.4)
            ragged_edge(face, rng, "right", 1, 0.4)
            for y in range(1, face.h - 1, 2):  # woven rings
                face.hline(1, y, face.w - 2, mk.STRAW[1] if y % 4 == 1 else mk.STRAW[4])
                for x in range(y % 4, face.w, 4):
                    face.set(x, y, mk.STRAW[5])

    def crown(face, rng):
        if face.side():
            for y in range(0, face.h, 2):
                for x in range((y // 2) % 2, face.w, 2):
                    face.set(x, y, mk.STRAW[4])
            holes(face, rng, 1)
        if face.name == "top":
            holes(face, rng, 2)
            for y in range(face.h):
                face.set(face.cx, y, mk.STRAW[1])

    head.add(*mk.octagon([-4, top + 6, -4], [8, 4, 8], straw, {"all": crown}, shrink=0.8),
             Box([-4, top + 6.3, -4], [8, 1.2, 8], Material(mk.LEATHER, base=2.0, noise=0.4, bevel=0.5),
                 {"front": lambda f, r: (f.set(f.cx, 0, mk.BRASS[4]))}, inflate=0.6),
             Box([-6.5, top + 5.5, -6.5], [13, 1, 13], straw.but(base=2.6), {"all": brim}),
             Box([2, top + 3, -6.5], [4, 2.5, 1], straw.but(base=2.0), {"sides": lambda f, r: tatter(f, r, 1, 0.5)},
                 rotation=[-35, 0, 0], pivot=[4, top + 5.5, -6]))
    tuft = Material(mk.STRAW, base=3.4, noise=0.5, grain="v")
    bones += chain_bones("straw", "head", [-5.5, top + 5.5, 4.5], 2, 3, tuft,
                         {"sides": lambda f, r: ragged_edge(f, r, "bottom", 2, 0.6)}, thickness=1.5,
                         box=lambda i, o, s: [Box([o[0] - 0.5, o[1], o[2]], [2.5, s[1], 1.5], tuft,
                                                  {"sides": lambda f, r: ragged_edge(f, r, "bottom", 1, 0.5)})])
    # The smock: layered linen with fold lines, a burnt-open front, a yoke at the shoulders, a rope belt with a knot
    # and two loose ends, tatters at the hem that swing.
    linen = Material(mk.LINEN, base=2.6, noise=0.5, grain="v", grad=1.0, bevel=0.3)

    def smock_side(face, rng):
        mk.fold_lines(face, rng, 3, -16)
        tatter(face, rng, 2, 0.5)
        holes(face, rng, 2 if face.w > 4 else 1)
        mk.edge_shadow(face, rng, -8)

    def smock_front(face, rng):
        smock_side(face, rng)
        for y in range(1, face.h):  # burnt open: the ribs show
            for x in range(face.cx - 2, face.cx + 2):
                face.clear(x, y)
        face.set(face.cx - 3, 1, mk.CHAR[1])
        face.set(face.cx + 2, 1, mk.CHAR[1])

    body = mk.bone(bones, "body")
    rope = Material(mk.ROPE, base=2.6, noise=0.5, grain="h", bevel=0.4)
    body.add(
        Box([-4, 15, -2], [8, 8.5, 4], linen, {"front": smock_front, "back": smock_side, "left": smock_side,
                                               "right": smock_side, "top": lambda f, r: holes(f, r, 1)}, inflate=0.45),
        Box([-4.5, 22, -2.5], [9, 2, 5], linen.but(base=3.2), {"sides": lambda f, r: (stitch(f, 1, mk.LINEN[0], 2),
                                                                                       mk.edge_shadow(f, r, -8))},
            inflate=0.55),
        Box([-4, 14.5, -2], [8, 1.5, 4], rope, {"sides": lambda f, r: stitch(f, 0, mk.ROPE[1], 2, 1)}, inflate=0.7),
        Box([1, 13.5, -3.2], [2, 2, 1.2], rope.but(base=3.4), inflate=0.2),
        Box([1.2, 10.5, -3.2], [0.8, 3.2, 0.8], rope, rotation=[0, 0, -12], pivot=[1.6, 13.5, -2.8]),
        Box([2.2, 11, -3.2], [0.8, 2.8, 0.8], rope, rotation=[0, 0, 14], pivot=[2.6, 13.5, -2.8]),
    )
    hem = linen.but(base=2.2)
    for side, x in (("r", -2.5), ("l", 2.5)):
        bones += chain_bones("tatter_" + side, "body", [x, 15, -2.4], 2, 2.5, hem, thickness=0.3,
                             box=lambda i, o, s, x=x: [Box([x - 1.5, o[1], -2.6], [3, s[1], 0.4], hem,
                                                           {"sides": lambda f, r: ragged_edge(f, r, "bottom", 1, 0.6)})])
    bones += chain_bones("tatter_b", "body", [0, 15, 2.4], 2, 2.5, hem, thickness=0.3,
                         box=lambda i, o, s: [Box([-2, o[1], 2.2], [4, s[1], 0.4], hem,
                                                  {"sides": lambda f, r: ragged_edge(f, r, "bottom", 1, 0.6)})])
    # Sleeves over the upper arms; fingers on the hands.
    sleeve = Material(mk.LINEN, base=2.4, noise=0.5, grain="v", bevel=0.3)
    r_sleeve = Box([-7, 17, -1.5], [3, 5, 3], sleeve, {"sides": lambda f, r: (tatter(f, r, 2, 0.6),
                                                                              mk.fold_lines(f, r, 2, -14))}, inflate=0.4)
    mk.bone(bones, "right_arm").add(r_sleeve)
    mk.bone(bones, "left_arm").add(mirror_box(r_sleeve))
    r_fingers = fingers(-7, 12, -1.3, 3, (0.8, 2, 0.8), flesh.but(base=2.0), spread=0.3, curl=25, thumb=(2.6, 0.8))
    mk.bone(bones, "right_fore").add(*r_fingers)
    mk.bone(bones, "left_fore").add(*[mirror_box(b) for b in r_fingers])
    trouser = Material(mk.LINEN, base=1.8, noise=0.5, grain="v", bevel=0.3)
    r_trouser = Box([-4, 5, -2], [4, 7, 4], trouser, {"sides": lambda f, r: (tatter(f, r, 3, 0.6), holes(f, r, 1),
                                                                             mk.fold_lines(f, r, 3, -14))}, inflate=0.3)
    mk.bone(bones, "right_leg").add(r_trouser)
    mk.bone(bones, "left_leg").add(mirror_box(r_trouser))
    # The scythe: an ash snath with two pegs, an iron collar, a long rusted blade with a bright edge, curving down.
    wood = Material(mk.WOOD, base=2.6, noise=0.5, grain="h", bevel=0.4)
    iron = Material(mk.IRON, base=2.4, noise=0.5, speck=0.08, bevel=0.6, decal=lambda f, r: scatter(f, r, 1, mk.RUST[2]))

    def blade_top(face, rng):
        for x in range(face.w):
            if rng.random() < 0.3:
                face.set(x, 0, mk.RUST[3])
        face.hline(0, face.h - 1, face.w, mk.IRON[6])

    def blade_side(face, rng):
        if face.side() and face.w > 4:
            face.hline(0, 0, face.w, mk.IRON[6])
            for _ in range(2):
                face.set(rng.randrange(face.w), face.h - 1, mk.RUST[2])

    # Held low in the right hand, the blade out to the side at shoulder height so the face stays clear.
    bones.append(Bone("tool", [-5.5, 13, 0], "right_fore", [8, 25, 0], [
        Box([-6, 0, -0.5], [1, 22, 1], wood, {"sides": lambda f, r: mk.grain(f, r, mk.WOOD[1], mk.WOOD[5], 1, (2, 5))}),
        Box([-8, 8, -0.5], [2, 1, 1], wood.but(base=3.2)),
        Box([-8, 16, -0.5], [2, 1, 1], wood.but(base=3.2)),
        Box([-6.5, 22, -1], [2, 1.5, 2], iron, {"sides": lambda f, r: mk.specular(f, r, mk.IRON[6], 0, 0)}),
        Box([-5.9, 22.5, -8], [0.6, 2, 8], iron.but(base=2.0), {"all": blade_side, "top": blade_top}),
        Box([-5.9, 22.5, -12.5], [0.6, 1.5, 5], iron.but(base=2.0), {"all": blade_side, "top": blade_top},
            rotation=[-18, 0, 0], pivot=[-5.6, 23.5, -8]),
        Box([-6.1, 23.8, -7.5], [1, 0.7, 6], iron.but(base=3.4)),
    ]))
    chains = chain_names("straw", 2) + chain_names("tatter_r", 2) + chain_names("tatter_l", 2) + chain_names("tatter_b", 2)
    tatters = chain_names("tatter_r", 2) + chain_names("tatter_l", 2) + chain_names("tatter_b", 2)
    swing_whip = merge(whip(chain_names("tatter_r", 2) + chain_names("tatter_l", 2), 0.55, 30, 0, settle=0.38),
                       whip(chain_names("straw", 2), 0.55, 25, 2, settle=0.38),
                       whip(chain_names("tatter_b", 2), 0.55, -25, 0, settle=0.38))
    animations = {
        "misc.idle": mk.breathe(3.0, 2, 1.5, 4, merge(
            lag_sway(chain_names("straw", 2), 3.0, 5, 2, phase=0.1),
            lag_sway(chain_names("tatter_r", 2), 3.0, 5, 0, phase=0.0),
            lag_sway(chain_names("tatter_l", 2), 3.0, 5, 0, phase=0.35),
            lag_sway(chain_names("tatter_b", 2), 3.0, 4, 0, phase=0.6),
            {"jaw": rot(0, ZERO, 1.5, e("easeInOutSine", [5, 0, 0]), 3.0, e("easeInOutSine", ZERO)),
             "tool": rot(0, ZERO, 1.5, e("easeInOutSine", [-3, 0, -3]), 3.0, e("easeInOutSine", ZERO)),
             "right_fore": rot(0, [-6, 0, 0], 1.5, e("easeInOutSine", [-9, 0, 0]), 3.0, e("easeInOutSine", [-6, 0, 0])),
             "left_fore": rot(0, [-6, 0, 0], 1.5, e("easeInOutSine", [-4, 0, 0]), 3.0, e("easeInOutSine", [-6, 0, 0]))})),
        "move.walk": mk.weighted_walk(1.1, 28, 14, 0.5, 4, 5, extra=mk.limb_walk(1.1, 8, 12, merge(
            lag_sway(chain_names("straw", 2), 1.1, 10, 0, phase=0.2),
            lag_sway(chain_names("tatter_r", 2), 1.1, 12, 0, phase=0.0),
            lag_sway(chain_names("tatter_l", 2), 1.1, 12, 0, phase=0.5),
            lag_sway(chain_names("tatter_b", 2), 1.1, 10, 0, phase=0.25),
            {"jaw": rot(0, [3, 0, 0], 0.55, e("easeInOutSine", [8, 0, 0]), 1.1, e("easeInOutSine", [3, 0, 0])),
             "tool": rot(0, [0, 0, -2], 0.55, e("easeInOutSine", [0, 0, 2]), 1.1, e("easeInOutSine", [0, 0, -2]))}))),
        # Both arms haul the scythe up over the right shoulder (anticipation), held at the windup's end, then it
        # comes down and across with the whole body; the tatters whip after it.
        "attack.swing": anim(0.95, merge({
            "right_arm": rot(0, ZERO, 0.4, e("easeInOutSine", [-150, 0, 18]), 0.5, [-160, 0, 12], 0.58,
                             e("easeOutBack", [-25, 0, -8]), 0.95, e("easeOutSine", ZERO)),
            "right_fore": rot(0, ZERO, 0.4, e("easeInOutSine", [-35, 0, 0]), 0.5, [-40, 0, 0], 0.58,
                              e("easeOutCubic", [10, 0, 0]), 0.95, e("easeOutSine", ZERO)),
            "left_arm": rot(0, ZERO, 0.4, e("easeInOutSine", [-120, 0, -30]), 0.5, [-130, 0, -25], 0.58,
                            e("easeOutBack", [-15, 0, 25]), 0.95, e("easeOutSine", ZERO)),
            "left_fore": rot(0, ZERO, 0.4, e("easeInOutSine", [-50, 0, 0]), 0.5, [-55, 0, 0], 0.58,
                             e("easeOutCubic", [-10, 0, 0]), 0.95, e("easeOutSine", ZERO)),
            "body": rot(0, ZERO, 0.4, e("easeInOutSine", [-14, 18, 0]), 0.5, [-16, 22, 0], 0.58,
                        e("easeOutBack", [24, -22, 0]), 0.95, e("easeOutSine", ZERO)),
            "head": rot(0, ZERO, 0.4, e("easeInOutSine", [-12, -10, 0]), 0.5, [-14, -12, 0], 0.58,
                        e("easeOutCubic", [14, 8, 0]), 0.95, e("easeOutSine", ZERO)),
            "jaw": rot(0, ZERO, 0.45, e("easeInOutSine", [12, 0, 0]), 0.58, e("easeOutCubic", [28, 0, 0]), 0.95,
                       e("easeOutSine", ZERO)),
            "tool": rot(0, ZERO, 0.4, e("easeInOutSine", [20, 0, 0]), 0.5, [22, 0, 0], 0.58, e("easeOutBack", [-15, 0, 0]),
                        0.95, e("easeOutSine", ZERO)),
            "root": pos(0, ZERO, 0.4, e("easeInOutSine", [0, 0, 1]), 0.58, e("easeOutCubic", [0, -1.2, -2]), 0.95,
                        e("easeOutSine", ZERO)),
            "right_leg": rot(0, ZERO, 0.4, e("easeInOutSine", [8, 0, 0]), 0.58, e("easeOutCubic", [-18, 0, 0]), 0.95,
                             e("easeOutSine", ZERO)),
            "left_leg": rot(0, ZERO, 0.4, e("easeInOutSine", [-8, 0, 0]), 0.58, e("easeOutCubic", [20, 0, 0]), 0.95,
                            e("easeOutSine", ZERO)),
        }, hold(chains, 0.45, ZERO), swing_whip), loop=False),
        "misc.hurt": mk.hurt_flinch(0.45, chains=chains, extra={
            "jaw": rot(0, ZERO, 0.08, e("easeOutCubic", [20, 0, 0]), 0.45, e("easeOutSine", ZERO)),
            "right_fore": rot(0, ZERO, 0.1, e("easeOutCubic", [-30, 0, 0]), 0.45, e("easeOutSine", ZERO))}),
        "misc.stagger": mk.stagger(0.9, chains=chains, extra={
            "jaw": rot(0, ZERO, 0.1, e("easeOutCubic", [25, 0, 0]), 0.9, e("easeOutSine", ZERO)),
            "right_fore": rot(0, ZERO, 0.12, e("easeOutCubic", [-40, 0, 0]), 0.9, e("easeOutSine", ZERO)),
            "left_fore": rot(0, ZERO, 0.12, e("easeOutCubic", [-50, 0, 0]), 0.9, e("easeOutSine", ZERO)),
            "tool": rot(0, ZERO, 0.12, e("easeOutCubic", [0, 0, 25]), 0.9, e("easeOutSine", ZERO))}),
        # Falls forward on its face; the scythe clatters out of the hand's line, the tatters and the straw settle.
        "misc.death": mk.death_fall(2.0, legs=12, forward=True, chains=tatters, chain_amp=35, extra=merge(
            mk.settle(chain_names("straw", 2), 0.9, 40, 2, length=1.0),
            {"jaw": rot(0, ZERO, 0.9, e("easeInQuad", [30, 0, 0]), 1.2, e("easeOutBounce", [35, 0, 0]), 2.0, [35, 0, 0]),
             "tool": rot(0, ZERO, 0.9, e("easeInQuad", [0, 0, 60]), 1.15, e("easeOutBounce", [10, 0, 75]), 2.0, [10, 0, 75]),
             "right_fore": rot(0, ZERO, 0.9, e("easeInQuad", [-20, 0, 0]), 2.0, [-20, 0, 0]),
             "left_fore": rot(0, ZERO, 0.9, e("easeInQuad", [-60, 0, 0]), 2.0, [-60, 0, 0]),
             "right_shin": rot(0, ZERO, 0.35, e("easeOutSine", [50, 0, 0]), 0.9, e("easeInQuad", [20, 0, 0]), 2.0, [20, 0, 0]),
             "left_shin": rot(0, ZERO, 0.35, e("easeOutSine", [30, 0, 0]), 0.9, e("easeInQuad", [40, 0, 0]), 2.0, [40, 0, 0])})),
    }
    write_model("cinder_husk", bones, animations, [2, 2.5], tex_w=128)


# --- More decals ----------------------------------------------------------------------------------------------------

def band(face, y, color, h=1):
    if face.side():
        face.rect(0, y, face.w, h, color)


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


def clear_all(face, rng=None):
    for y in range(face.h):
        for x in range(face.w):
            face.clear(x, y)


def rivets(face, color, step=3, inset=1):
    for x in range(inset, face.w - inset, step):
        face.set(x, inset, color)
        face.set(x, face.h - 1 - inset, color)


def mail(face, rng, dark, light):
    """Chainmail: rows of links, every other row offset, a highlight on some."""
    for y in range(face.h):
        for x in range(face.w):
            if (x + (y // 2)) % 2 == 0 and y % 2 == 0:
                face.set(x, y, dark)
            elif rng.random() < 0.15:
                face.set(x, y, light)


def lower(face, rows, mat_colors, rng, base=None):
    """Repaints the bottom rows of a side face from another ramp (hands, boots)."""
    if not face.side():
        return
    for y in range(max(0, face.h - rows), face.h):
        for x in range(face.w):
            lv = (len(mat_colors) - 1) / 2 if base is None else base
            lv += (rng.random() - 0.5) * 1.2 - (y - (face.h - rows)) * 0.3
            face.set(x, y, mat_colors[clamp(round(lv), 0, len(mat_colors) - 1)])


def feathers(face, rng, light, dark=None, step=2):
    """Overlapping feather rows: a lighter tip every few pixels, staggered row to row."""
    for y in range(0, face.h, step):
        off = (y // step) % 3
        for x in range(off, face.w, 3):
            face.set(x, y, light)
            if dark is not None and face.inside(x, y + 1):
                face.set(x, y + 1, dark)


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


def open_front(face, inset=1, top_rows=1):
    """A hood's front: cleared except a rim, so the face inside shows."""
    for y in range(top_rows, face.h):
        for x in range(inset, face.w - inset):
            face.clear(x, y)


# --- Fields: Ash Stalker ---------------------------------------------------------------------------------------------
# Lean and crouched, caked in layered ash: crust plates over the shoulders and the back, a ridge of ash spines down
# the spine that sways, long arms ending in three bone claws, a brow plate over two ember slits, a jaw of broken
# teeth, loose ash rags hanging from the forearms. Buried in a furrow until it boils out (20 ticks) and leaps; swing
# windup 8 ticks.

def ash_stalker():
    coat = Material(mk.ASH, base=2.4, noise=0.6, grain="h", speck=0.05, grad=1.0, top=1.0, bevel=0.5)

    def streaks(face, rng):
        if face.side():
            for _ in range(max(1, face.w // 3)):
                x = rng.randrange(face.w)
                face.vline(x, rng.randrange(max(1, face.h // 2)), rng.randint(2, 4), mk.ASH[0])
            if rng.random() < 0.4:
                ember_cracks(face, rng, 1, EMBER_DEEP, EMBER_DARK, 0.6, (1, 2))
            mk.edge_shadow(face, rng, -12)

    def face_front(face, rng):
        # No real face: a dark brow line, two ember slits in sockets, a split down the middle.
        cx = face.cx
        face.hline(0, 1, face.w, mk.ASH[1])
        for ex in (cx - 3, cx + 1):
            face.rect(ex, 2, 2, 1, VOID)
            face.lit(ex + (1 if ex < cx else 0), 2, EMBER_HOT, 0.95)
        face.vline(cx - 1, 3, 3, mk.ASH[0])
        mk.edge_shadow(face, rng, -12)

    def crust(face, rng):
        # Caked ash plates: cracked into cells with a lit rim on the top-left of each.
        if not face.side() and face.name != "top":
            return
        for y in range(1, face.h, 3):
            for x in range((y // 3) % 2, face.w, 3):
                face.set(x, y, mk.ASH[0])
                face.tint(x + 1, y - 1, 18)
        mk.edge_shadow(face, rng, -10)

    bones, top = mk.dense_humanoid(legs=11, body=(7, 10, 4), head=(7, 7, 7), arm=(3, 13, 3), leg=(3, 3), hunch=25,
                                   mats={"body": coat, "head": coat.but(base=2.8), "arm": coat.but(base=2.1),
                                         "leg": coat.but(base=1.9), "hand": coat.but(base=1.6), "foot": coat.but(base=1.5)},
                                   decals={"body": {"sides": streaks, "top": crust}, "head": {"front": face_front,
                                           "sides": streaks, "top": crust},
                                           "arm": {"sides": streaks}, "fore": {"sides": streaks},
                                           "leg": {"sides": streaks}, "shin": {"sides": streaks}},
                                   arm_gap=0.3)
    head = mk.bone(bones, "head")
    plate_mat = coat.but(base=3.2, bevel=0.8)
    head.add(Box([-4, top + 5.8, -4.2], [8, 1.8, 5], plate_mat, {"all": crust}, inflate=0.3),  # brow plate
             Box([-1, top + 6.5, -2], [2, 2, 5], plate_mat, {"all": crust}, rotation=[20, 0, 0], pivot=[0, top + 6.5, 0]))
    teeth = Material(mk.BONE, base=4.0, noise=0.3)

    def jaw_front(face, rng):
        face.hline(0, 0, face.w, VOID)
        for x in range(0, face.w, 2):
            face.set(x, 0, mk.BONE[4])

    bones.append(Bone("jaw", [0, top + 0.5, -1.5], "head", [10, 0, 0], [
        Box([-3, top - 1, -4.2], [6, 2, 4], coat.but(base=1.6), {"front": jaw_front, "sides": streaks}),
        Box([-2, top - 1.5, -4.4], [1, 1, 1], teeth), Box([1, top - 1.5, -4.4], [1, 1, 1], teeth)]))
    body = mk.bone(bones, "body")
    # Crust plates over the shoulders and the back, overlapping like shingles.
    body.add(Box([-4.5, top - 3, -2.5], [9, 3, 5], plate_mat, {"all": crust}, inflate=0.4),
             Box([-3.5, top - 6, 1.2], [7, 4, 1.5], plate_mat, {"all": crust}, inflate=0.2),
             Box([-3, top - 9, 1.4], [6, 3.5, 1.5], plate_mat.but(base=2.8), {"all": crust}, inflate=0.2))
    # The spine ridge: three ash spines in a chain hanging off the back of the neck, swaying as it moves.
    spine = coat.but(base=3.4, top=1.2)
    bones += chain_bones("ridge", "body", [0, top - 0.5, 2.4], 3, 2.8, spine, thickness=1.6,
                         box=lambda i, o, s: [Box([-1, o[1], o[2] - 0.5], [2, s[1], 2 - i * 0.3], spine,
                                                  rotation=[-28, 0, 0], pivot=[0, o[1] + s[1], o[2]])])
    for n in chain_names("ridge", 3):
        mk.bone(bones, n).rotation = [-20, 0, 0] if n.endswith("1") else [-10, 0, 0]
    claw = Material(mk.BONE, base=1.6, noise=0.4, bevel=0.5)
    r_claws = mk.claws(-6.3, 10, -1.2, 3, (0.8, 2.8, 0.8), claw, spread=0.4, curl=-32, splay=12)
    mk.bone(bones, "right_fore").add(*r_claws)
    mk.bone(bones, "left_fore").add(*[mirror_box(b) for b in r_claws])
    rag = Material(mk.ASH_CLOTH, base=2.2, noise=0.5, grain="v")
    for side, parent, x in (("r", "right_fore", -5.3), ("l", "left_fore", 5.3)):
        bones += chain_bones("rag_" + side, parent, [x, 14, 1.6], 2, 2.2, rag, thickness=0.3,
                             box=lambda i, o, s, x=x: [Box([x - 1.2, o[1], 1.5], [2.4, s[1], 0.4], rag,
                                                           {"sides": lambda f, r: ragged_edge(f, r, "bottom", 1, 0.6)})])
    toes = Box([-3.2, 0, -2.8], [3.4, 1, 1.5], claw.but(base=1.2))
    mk.bone(bones, "right_shin").add(toes)
    mk.bone(bones, "left_shin").add(mirror_box(toes))
    ridge = chain_names("ridge", 3)
    rags = chain_names("rag_r", 2) + chain_names("rag_l", 2)
    chains = ridge + rags
    crouch_arms = {"right_fore": [-18, 0, 0], "left_fore": [-18, 0, 0]}
    animations = {
        "misc.idle": mk.breathe(2.0, 2.5, 2, 5, merge(
            lag_sway(ridge, 2.0, 6, 0, phase=0.0),
            lag_sway(chain_names("rag_r", 2), 2.0, 5, 0, phase=0.3),
            lag_sway(chain_names("rag_l", 2), 2.0, 5, 0, phase=0.6),
            {"jaw": rot(0, ZERO, 1.0, e("easeInOutSine", [8, 0, 0]), 2.0, e("easeInOutSine", ZERO)),
             "right_fore": rot(0, crouch_arms["right_fore"], 1.0, e("easeInOutSine", [-24, 0, 0]), 2.0,
                               e("easeInOutSine", crouch_arms["right_fore"])),
             "left_fore": rot(0, crouch_arms["left_fore"], 1.0, e("easeInOutSine", [-12, 0, 0]), 2.0,
                              e("easeInOutSine", crouch_arms["left_fore"]))})),
        "move.walk": mk.weighted_walk(0.6, 40, 30, 0.7, 6, 7, extra=mk.limb_walk(0.6, 14, 16, merge(
            lag_sway(ridge, 0.6, 12, 0, phase=0.1),
            lag_sway(chain_names("rag_r", 2), 0.6, 14, 0, phase=0.0),
            lag_sway(chain_names("rag_l", 2), 0.6, 14, 0, phase=0.5),
            {"jaw": rot(0, [5, 0, 0], 0.3, e("easeInOutSine", [12, 0, 0]), 0.6, e("easeInOutSine", [5, 0, 0]))}))),
        "misc.buried": anim(1.0, {"root": pos(0, [0, -30, 0], 1, [0, -30, 0])}),
        # It boils up out of the furrow: shaking, the claws clawing at the air, then it stands.
        "misc.emerge": anim(1.0, merge({
            "root": pos(0, [0, -26, 0], 0.3, e("easeOutSine", [0.6, -22, 0]), 0.5, e("easeInOutSine", [-0.6, -18, 0]), 0.7,
                        e("easeInOutSine", [0.6, -12, 0]), 0.9, e("easeInOutSine", [0, -5, 0]), 1.0, e("easeOutBack", ZERO)),
            "right_arm": rot(0, [-160, 0, 25], 0.5, e("easeInOutSine", [-150, 0, 35]), 0.9, e("easeInOutSine", [-110, 0, 20]),
                             1.0, e("easeOutSine", ZERO)),
            "left_arm": rot(0, [-160, 0, -25], 0.5, e("easeInOutSine", [-170, 0, -15]), 0.9, e("easeInOutSine", [-110, 0, -20]),
                            1.0, e("easeOutSine", ZERO)),
            "right_fore": rot(0, [-40, 0, 0], 0.3, e("easeInOutSine", [-70, 0, 0]), 0.6, e("easeInOutSine", [-30, 0, 0]), 1.0,
                              e("easeOutSine", crouch_arms["right_fore"])),
            "left_fore": rot(0, [-40, 0, 0], 0.45, e("easeInOutSine", [-70, 0, 0]), 0.75, e("easeInOutSine", [-30, 0, 0]), 1.0,
                             e("easeOutSine", crouch_arms["left_fore"])),
            "head": rot(0, [-35, 0, 0], 0.5, e("easeInOutSine", [-30, 15, 0]), 0.8, e("easeInOutSine", [-25, -15, 0]), 1.0,
                        e("easeOutSine", ZERO)),
            "jaw": rot(0, [25, 0, 0], 1.0, e("easeOutSine", [5, 0, 0])),
            "body": rot(0, [-20, 0, 0], 0.3, e("easeInOutSine", [-15, 0, 4]), 0.6, e("easeInOutSine", [-10, 0, -4]), 1.0,
                        e("easeOutSine", ZERO)),
        }, lag_sway(ridge, 1.0, 14, 0, phase=0.2), lag_sway(rags, 1.0, 16, 0, phase=0.0)), loop=False),
        "attack.leap": anim(0.6, merge({
            "right_arm": rot(0, [-100, 0, 25], 0.35, e("easeOutSine", [-80, 0, 30]), 0.6, e("easeOutSine", [-20, 0, 0])),
            "left_arm": rot(0, [-100, 0, -25], 0.35, e("easeOutSine", [-80, 0, -30]), 0.6, e("easeOutSine", [-20, 0, 0])),
            "right_fore": rot(0, [-50, 0, 0], 0.6, e("easeOutSine", crouch_arms["right_fore"])),
            "left_fore": rot(0, [-50, 0, 0], 0.6, e("easeOutSine", crouch_arms["left_fore"])),
            "right_leg": rot(0, [45, 0, 0], 0.6, e("easeOutSine", ZERO)),
            "left_leg": rot(0, [45, 0, 0], 0.6, e("easeOutSine", ZERO)),
            "right_shin": rot(0, [30, 0, 0], 0.6, e("easeOutSine", ZERO)),
            "left_shin": rot(0, [30, 0, 0], 0.6, e("easeOutSine", ZERO)),
            "head": rot(0, [-30, 0, 0], 0.6, e("easeOutSine", ZERO)),
            "jaw": rot(0, [30, 0, 0], 0.6, e("easeOutSine", ZERO)),
            "body": rot(0, [-15, 0, 0], 0.6, e("easeOutSine", ZERO)),
        }, whip(ridge, 0.0, -25, 0, settle=0.55), whip(rags, 0.0, -30, 0, settle=0.55)), loop=False),
        # The right claw is drawn back and up through the 8-tick windup, the body coiling; then it rakes across.
        "attack.swing": anim(0.7, merge({
            "right_arm": rot(0, ZERO, 0.33, e("easeInOutSine", [-135, 0, 40]), 0.4, [-140, 0, 45], 0.47,
                             e("easeOutBack", [-15, 0, -30]), 0.7, e("easeOutSine", ZERO)),
            "right_fore": rot(0, crouch_arms["right_fore"], 0.33, e("easeInOutSine", [-60, 0, 0]), 0.4, [-65, 0, 0], 0.47,
                              e("easeOutCubic", [-5, 0, 0]), 0.7, e("easeOutSine", crouch_arms["right_fore"])),
            "left_arm": rot(0, ZERO, 0.33, e("easeInOutSine", [-40, 0, -25]), 0.47, e("easeOutCubic", [-60, 0, 10]), 0.7,
                            e("easeOutSine", ZERO)),
            "body": rot(0, ZERO, 0.33, e("easeInOutSine", [-8, 28, 0]), 0.4, [-10, 32, 0], 0.47,
                        e("easeOutBack", [12, -28, 0]), 0.7, e("easeOutSine", ZERO)),
            "head": rot(0, ZERO, 0.33, e("easeInOutSine", [-12, -20, 0]), 0.47, e("easeOutCubic", [8, 14, 0]), 0.7,
                        e("easeOutSine", ZERO)),
            "jaw": rot(0, ZERO, 0.4, e("easeInOutSine", [15, 0, 0]), 0.47, e("easeOutCubic", [30, 0, 0]), 0.7,
                       e("easeOutSine", ZERO)),
            "root": pos(0, ZERO, 0.33, e("easeInOutSine", [0, -1, 1]), 0.47, e("easeOutCubic", [0, 0, -2]), 0.7,
                        e("easeOutSine", ZERO)),
        }, hold(chains, 0.4, ZERO), whip(ridge, 0.45, 25, 0, settle=0.24), whip(rags, 0.45, 35, 0, settle=0.24)),
            loop=False),
        "misc.hurt": mk.hurt_flinch(0.4, lean=-10, twist=8, head=-18, chains=chains, extra={
            "jaw": rot(0, ZERO, 0.08, e("easeOutCubic", [22, 0, 0]), 0.4, e("easeOutSine", ZERO))}),
        "misc.stagger": mk.stagger(0.8, chains=chains, extra={
            "jaw": rot(0, ZERO, 0.1, e("easeOutCubic", [30, 0, 0]), 0.8, e("easeOutSine", ZERO)),
            "right_fore": rot(0, crouch_arms["right_fore"], 0.12, e("easeOutCubic", [-50, 0, 0]), 0.8,
                              e("easeOutSine", crouch_arms["right_fore"])),
            "left_fore": rot(0, crouch_arms["left_fore"], 0.12, e("easeOutCubic", [-50, 0, 0]), 0.8,
                             e("easeOutSine", crouch_arms["left_fore"]))}),
        # It crumples onto its side like a heap of ash: knees first, then over to the right, the ridge settling.
        "misc.death": mk.death_fall(1.8, legs=11, forward=True, chains=rags, chain_amp=30, knees=0.3, down=0.8,
                                    extra=merge(mk.settle(ridge, 0.8, 35, 0, length=0.9), {
                                        "jaw": rot(0, ZERO, 0.8, e("easeInQuad", [32, 0, 0]), 1.8, [32, 0, 0]),
                                        "right_fore": rot(0, crouch_arms["right_fore"], 0.8, e("easeInQuad", [-35, 0, 0]), 1.8,
                                                          [-35, 0, 0]),
                                        "left_fore": rot(0, crouch_arms["left_fore"], 0.8, e("easeInQuad", [-70, 0, 0]), 1.8,
                                                         [-70, 0, 0]),
                                        "right_shin": rot(0, ZERO, 0.3, e("easeOutSine", [45, 0, 0]), 0.8, e("easeInQuad", [25, 0, 0]),
                                                          1.8, [25, 0, 0]),
                                        "left_shin": rot(0, ZERO, 0.3, e("easeOutSine", [30, 0, 0]), 0.8, e("easeInQuad", [40, 0, 0]),
                                                         1.8, [40, 0, 0])})),
    }
    write_model("ash_stalker", bones, animations, [2, 2], tex_w=128)


# --- Fields: Ashen Carrion -------------------------------------------------------------------------------------------
# A big carrion bird of the burnt fields: soot-black feathers, a grey ruff, a bald scorched head with a bone beak.
# Shriek (15 ticks, head up, wings spread) before each dive.

SCORCHED = ramp("2e1c1a", "432824", "5a3630", "72463c", "8a5848")


def ashen_carrion():
    plume = Material(FEATHER, base=2.4, noise=0.6, grain="h", top=0.8)

    def wing_top(face, rng):
        if face.name in ("top", "bottom"):
            feathers(face, rng, FEATHER[4] if face.name == "top" else FEATHER[3], FEATHER[1])

    def wing_trailing(face, rng):
        # Ragged trailing (back) edge of the wing: the top face's first rows are its back (v0 is +z).
        if face.name in ("top", "bottom"):
            ragged_edge(face, rng, "top", 2, 0.5)

    def tip_fingers(face, rng):
        if face.name in ("top", "bottom"):
            feathers(face, rng, FEATHER[4], FEATHER[1])
            # Primary feathers spread like fingers at the outer end (+x is the left of the face on top).
            for y in range(1, face.h, 2):
                for x in range(0, 3):
                    face.clear(x if face.name == "top" else face.w - 1 - x, y)

    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 8, 0], "root", None, [
            Box([-3, 5, -5], [6, 5, 10], plume, {"sides": lambda f, r: feathers(f, r, FEATHER[4]),
                                                 "top": lambda f, r: feathers(f, r, FEATHER[5])}),
            Box([-3.5, 7.5, -6], [7, 3, 3], Material(ASH, base=2.0, noise=0.7, grain="v"),
                {"sides": lambda f, r: ragged_edge(f, r, "bottom", 1, 0.5)}, inflate=0.3),
        ]),
        Bone("head", [0, 9, -5], "body", None, [
            Box([-1, 8.5, -8], [2, 2, 3], Material(SCORCHED, base=2.0, noise=0.6)),
            Box([-2, 9, -10.5], [4, 3, 3], Material(SCORCHED, base=2.4, noise=0.6, top=0.4),
                {"left": lambda f, r: (f.set(1, 1, VOID), f.lit(1, 1, EMBER, 0.9)),
                 "right": lambda f, r: (f.set(f.w - 2, 1, VOID), f.lit(f.w - 2, 1, EMBER, 0.9))}),
            Box([-1, 9, -13], [2, 2, 3], Material(BONE, base=2.6, noise=0.4)),
            Box([-1, 8, -13.5], [2, 1, 1], Material(BONE, base=1.6, noise=0.3)),
        ]),
    ]
    wing = Box([-17, 9.5, -4], [14, 1, 8], plume.but(base=2.2), {"all": wing_top, "top": wing_trailing})
    coverts = Box([-14, 10, -4], [11, 1, 4], plume.but(base=3.2),
                  {"top": lambda f, r: feathers(f, r, FEATHER[5], FEATHER[2])}, inflate=0.15)
    tip = Box([-25, 9.5, -3], [8, 1, 6], plume.but(base=1.8), {"all": tip_fingers})
    bones += [
        Bone("right_wing", [-3, 10, 0], "body", None, [wing, coverts]),
        Bone("right_wing_tip", [-17, 10, 0], "right_wing", None, [tip]),
        Bone("left_wing", [3, 10, 0], "body", None, [mirror_box(wing), mirror_box(coverts)]),
        Bone("left_wing_tip", [17, 10, 0], "left_wing", None, [mirror_box(tip)]),
        Bone("tail", [0, 8, 5], "body", None, [
            Box([-3, 7, 5], [6, 1, 6], plume, {"top": lambda f, r: (feathers(f, r, FEATHER[4]),
                                                                    ragged_edge(f, r, "top", 2, 0.5))})]),
    ]
    leg = [Box([-2, 0.5, 0.5], [1, 4.5, 1], Material(SCORCHED, base=1.5, noise=0.4)),
           Box([-2.5, 0, -0.5], [2, 1, 2], Material(BONE, base=1.2, noise=0.3))]
    bones += [Bone("right_leg", [-1.5, 5, 1], "body", None, leg),
              Bone("left_leg", [1.5, 5, 1], "body", None, [mirror_box(b) for b in leg])]

    def flap(length, angle, tip_angle):
        h = length / 2
        return {
            "right_wing": rot(0, [0, 0, angle], h, [0, 0, -angle], length, [0, 0, angle]),
            "left_wing": rot(0, [0, 0, -angle], h, [0, 0, angle], length, [0, 0, -angle]),
            "right_wing_tip": rot(0, [0, 0, tip_angle], h, [0, 0, -tip_angle], length, [0, 0, tip_angle]),
            "left_wing_tip": rot(0, [0, 0, -tip_angle], h, [0, 0, tip_angle], length, [0, 0, -tip_angle]),
            "tail": rot(0, [-5, 0, 0], h, [5, 0, 0], length, [-5, 0, 0]),
            "root": pos(0, [0, -0.5, 0], h, [0, 0.5, 0], length, [0, -0.5, 0]),
        }

    animations = {
        "move.fly": anim(0.8, flap(0.8, 25, 15)),
        "misc.idle": anim(3.0, {
            "right_wing": rot(0, [0, 0, -80], 3, [0, 0, -80]),
            "left_wing": rot(0, [0, 0, 80], 3, [0, 0, 80]),
            "right_wing_tip": rot(0, [0, 0, -100], 3, [0, 0, -100]),
            "left_wing_tip": rot(0, [0, 0, 100], 3, [0, 0, 100]),
            "head": rot(0, ZERO, 1.5, [15, 10, 0], 3, ZERO),
        }),
        "attack.shriek": anim(0.75, {
            "head": rot(0, ZERO, 0.3, [-40, 0, 0], 0.75, [-40, 0, 0]),
            "right_wing": rot(0, ZERO, 0.3, [0, 0, 35], 0.75, [0, 0, 35]),
            "left_wing": rot(0, ZERO, 0.3, [0, 0, -35], 0.75, [0, 0, -35]),
            "tail": rot(0, ZERO, 0.3, [-20, 0, 0], 0.75, [-20, 0, 0]),
        }, loop=False),
        "attack.dive": anim(1.0, {
            "body": rot(0, [30, 0, 0], 1, [30, 0, 0]),
            "right_wing": rot(0, [0, 55, 0], 1, [0, 55, 0]),
            "left_wing": rot(0, [0, -55, 0], 1, [0, -55, 0]),
            "right_leg": rot(0, [-60, 0, 0], 1, [-60, 0, 0]),
            "left_leg": rot(0, [-60, 0, 0], 1, [-60, 0, 0]),
        }),
        "attack.peck": anim(0.4, {"head": rot(0, ZERO, 0.15, [-20, 0, 0], 0.25, [40, 0, 0], 0.4, ZERO)},
                            loop=False),
    }
    write_model("ashen_carrion", bones, animations, [3.5, 1.5])


# --- Woods: Withered Thrall ------------------------------------------------------------------------------------------
# A hanged poacher: grey-green dead skin, a hunting hood, a leather jerkin with a quiver, the noose and the cut rope.
# Hangs in a cage and drops on a player below (chain rattle 10 ticks before); swing windup 10 ticks.

def withered_thrall():
    skin = Material(SKIN_DEAD, base=2.4, noise=0.6, speck=0.03)
    jerkin = Material(LEATHER, base=2.4, noise=0.6, grain="v", edge=0.6)
    cloth = Material(CLOTH_GREEN, base=2.2, noise=0.6, grain="v")

    def face_front(face, rng):
        # Bulging pale eyes, an open mouth, a bruise from the rope.
        for ex in (1, 4):
            face.set(ex, 3, VOID)
            face.lit(ex + 1, 3, hexc("e8e0a0"), 0.75)
        face.rect(2, 5, 3, 1, VOID)
        face.set(3, 6, SKIN_DEAD[0])
        face.set(0, 6, hexc("4a3a46"))
        face.set(6, 6, hexc("4a3a46"))

    def jerkin_deco(face, rng):
        if face.name == "front":
            face.vline(face.cx, 0, face.h, LEATHER[0])
            for y in range(1, face.h - 1, 3):
                face.set(face.cx - 1, y, ROPE[3])
            face.hline(0, face.h - 3, face.w, LEATHER[1])
            face.set(face.cx + 1, face.h - 3, BRONZE[4])
        if face.side():
            band(face, face.h - 3, LEATHER[1])
            tatter(face, rng, 1, 0.3)

    def sleeve(face, rng):
        lower(face, 3, SKIN_DEAD, rng)
        band(face, face.h - 4, ROPE[2])

    def boots(face, rng):
        lower(face, 4, LEATHER, rng, 1.6)
        band(face, face.h - 5, LEATHER[0])

    bones, top = humanoid(legs=13, body=(7, 12, 3), head=(7, 7, 7), arm=(3, 13, 3), leg=(3, 3),
                          mats={"body": jerkin, "head": skin, "arm": cloth, "leg": cloth.but(base=1.6)},
                          decals={"body": {"all": jerkin_deco}, "head": {"front": face_front},
                                  "arm": {"all": sleeve}, "leg": {"all": boots}})
    bone(bones, "head").rotation = [0, 0, 18]  # the broken neck
    hood = Material(CLOTH_GREEN, base=2.0, noise=0.6, grain="v", top=0.5)
    bone(bones, "head").add(Box([-3.5, top + 1, -3.5], [7, 7, 7], hood,
                                {"front": lambda f, r: open_front(f, 1, 1),
                                 "bottom": clear_all,
                                 "sides": lambda f, r: ragged_edge(f, r, "bottom", 1, 0.4)}, inflate=0.45))
    rope = Material(ROPE, base=2.2, noise=0.6, grain="v")
    bones.append(Bone("noose", [0, top, 0], "head", None, [
        Box([-3.5, top - 0.5, -2], [7, 1, 4], rope, {"front": lambda f, r: f.set(f.cx, 0, ROPE[0])}, inflate=0.3),
        Box([-0.5, top + 7, -0.5], [1, 10, 1], rope,
            {"sides": lambda f, r: (f.set(0, f.h - 1, ROPE[4]), f.set(0, 0, ROPE[0]))}),
        Box([-1, top + 6.5, -1], [2, 1, 2], rope, inflate=0.2),
    ]))
    quiver = Material(LEATHER, base=1.8, noise=0.5, grain="v")
    bone(bones, "body").add(
        Box([-3, 15, 1.5], [3, 7, 2], quiver, {"top": lambda f, r: f.rect(0, 0, f.w, f.h, LEATHER[0])},
            rotation=[0, 0, -20], pivot=[-1.5, 18, 2.5]),
        Box([-2.5, 21, 2], [1, 3, 1], Material(WOOD, base=3.0, noise=0.3),
            {"top": lambda f, r: f.set(0, 0, hexc("8a8a8a"))}, rotation=[0, 0, -20], pivot=[-1.5, 18, 2.5]),
        Box([-1.5, 21, 2], [1, 2, 1], Material(STRAW, base=3.5, noise=0.3), rotation=[0, 0, -20],
            pivot=[-1.5, 18, 2.5]),
    )
    limp = [10, 0, 0]
    animations = {
        "misc.idle": idle(3.0, 3, 2, noose=rot(0, ZERO, 1.5, [0, 0, -4], 3.0, ZERO)),
        "move.walk": walk(1.2, 25, 10, 0.3, body=rot(0, [0, 0, 4], 0.6, [0, 0, -4], 1.2, [0, 0, 4])),
        "misc.hang": anim(4.0, {
            "root": rot(0, [0, 0, 3], 2, [0, 0, -3], 4, [0, 0, 3]),
            "right_leg": rot(0, limp, 4, limp), "left_leg": rot(0, [5, 0, 0], 4, [5, 0, 0]),
            "right_arm": rot(0, [0, 0, 5], 4, [0, 0, 5]), "left_arm": rot(0, [0, 0, -5], 4, [0, 0, -5]),
            "head": rot(0, [15, 0, 0], 2, [18, 0, 0], 4, [15, 0, 0]),
        }),
        "attack.drop": anim(0.5, {
            "right_arm": rot(0, ZERO, 0.2, [-160, 0, 30], 0.5, [-90, 0, 0]),
            "left_arm": rot(0, ZERO, 0.2, [-160, 0, -30], 0.5, [-90, 0, 0]),
            "right_leg": rot(0, ZERO, 0.3, [-30, 0, 0], 0.5, ZERO),
            "left_leg": rot(0, ZERO, 0.3, [-30, 0, 0], 0.5, ZERO),
        }, loop=False),
        "attack.swing": overhead(0.5, 0.8, ("right_arm", "left_arm")),
    }
    write_model("withered_thrall", bones, animations, [2, 3])


# --- Woods: Lantern Hag ----------------------------------------------------------------------------------------------
# A hunched crone in a moss-dark shawl and hood, a hooked nose and grey hair, an amber lantern in the left hand and a
# fire flask in the right. Raises the lantern 15 ticks before a throw.

ROBE_MOSS = ramp("20231c", "2c3126", "394031", "474f3d", "57614b")
SKIN_OLD = ramp("4d4a3e", "625e4f", "787361", "8e8874", "a39d88")
HAIR = ramp("57544f", "706c66", "8c8882", "a7a39c")
GLASS_FIRE = ramp("7a2a12", "b8461a", "e66d22", "ff9a3a", "ffc66a")


def lantern_hag():
    robe = Material(ROBE_MOSS, base=2.2, noise=0.6, grain="v", edge=0.5)
    skin = Material(SKIN_OLD, base=2.8, noise=0.6, speck=0.04)

    def face_front(face, rng):
        for ex in (1, 4):
            face.set(ex, 3, VOID)
            face.lit(ex + (1 if ex == 1 else 0), 3, AMBER, 0.8)
        face.hline(2, 5, 3, SKIN_OLD[0])
        face.set(1, 6, SKIN_OLD[0])
        face.set(5, 6, SKIN_OLD[0])
        face.set(5, 4, hexc("5e6a3a"))  # wart

    def robe_deco(face, rng):
        if face.name == "front":
            face.vline(face.cx, 3, face.h - 3, ROBE_MOSS[0])
            for y in range(4, face.h - 1, 3):
                face.set(face.cx, y, BRONZE[3])
        if face.side():
            band(face, face.h - 4, ROPE[1])

    def sleeve(face, rng):
        lower(face, 2, SKIN_OLD, rng)
        tatter(face, rng, 1, 0.3)

    bones, top = humanoid(legs=10, body=(8, 14, 5), head=(7, 7, 7), arm=(3, 12, 3), leg=(3, 3), hunch=20,
                          mats={"body": robe, "head": skin, "arm": robe.but(base=2.0), "leg": robe.but(base=1.4)},
                          decals={"body": {"all": robe_deco}, "head": {"front": face_front},
                                  "arm": {"all": sleeve}})
    head = bone(bones, "head")
    hood = Material(ROBE_MOSS, base=2.6, noise=0.6, grain="v", top=0.4)
    head.add(Box([-3.5, top + 1, -3.5], [7, 7, 7], hood,
                 {"front": lambda f, r: open_front(f, 1, 1), "bottom": clear_all,
                  "sides": lambda f, r: ragged_edge(f, r, "bottom", 1, 0.4)}, inflate=0.5))
    head.add(Box([-0.5, top + 1.5, -6], [1, 3, 3], skin.but(base=3.2), rotation=[30, 0, 0], pivot=[0, top + 3, -4]))
    hair = Material(HAIR, base=1.6, noise=0.7, grain="v")
    strand = Box([-4, top - 5, -2.5], [1, 7, 3], hair, {"sides": lambda f, r: ragged_edge(f, r, "bottom", 3, 0.6)})
    head.add(strand, mirror_box(strand))
    body = bone(bones, "body")
    shawl = Material(ROBE_MOSS, base=3.0, noise=0.6, grain="h")
    body.add(Box([-4.5, top - 5, -3], [9, 5, 6], shawl,
                 {"sides": lambda f, r: (ragged_edge(f, r, "bottom", 2, 0.6), stitch(f, 1, ROPE[2], 3))},
                 inflate=0.3))
    vial = Material(GLASS_FIRE, base=2.5, noise=0.5, glow=0.5)
    body.add(Box([-3.5, 11, -3.6], [2, 3, 1], vial), Box([1.5, 11.5, -3.6], [2, 2, 1], vial))
    bones.append(Bone("skirt", [0, 10, 0], "root", None, [
        Box([-4.5, 1, -3], [9, 10, 6], robe.but(base=1.8), {"sides": lambda f, r: (tatter(f, r, 3, 0.55),
                                                                                  band(f, 1, ROPE[1]))})]))
    iron = Material(IRON, base=1.8, noise=0.5, speck=0.08)

    def lantern_glass(face, rng):
        if face.side():
            face.vline(0, 0, face.h, IRON[1])
            face.vline(face.w - 1, 0, face.h, IRON[1])
            face.lit(face.cx, face.h // 2, EMBER_HOT, 1.0)

    bones.append(Bone("lantern", [5.5, 13, 0], "left_arm", None, [
        Box([5.5, 12, -0.5], [1, 2, 1], iron),
        Box([4.5, 11, -1.5], [3, 1, 3], iron),
        Box([4.5, 8, -1.5], [3, 3, 3], Material(ramp("a0501a", "e08a2e", "ffb347", "ffd27a"), base=2.0,
                                                noise=0.5, glow=0.9), {"all": lantern_glass}),
        Box([4.5, 7, -1.5], [3, 1, 3], iron),
    ]))
    bones.append(Bone("flask", [-5.5, 12, 0], "right_arm", None, [
        Box([-6.5, 9, -1], [2, 2, 2], Material(GLASS_FIRE, base=2.6, noise=0.5, glow=0.7)),
        Box([-6, 11, -0.5], [1, 1, 1], Material(GLASS_FIRE, base=1.0, noise=0.3)),
        Box([-6, 12, -0.5], [1, 1, 1], Material(LINEN, base=3.0, noise=0.3)),
    ]))
    animations = {
        "misc.idle": idle(3.0, 4, 2, lantern=rot(0, [0, 0, -5], 1.5, [0, 0, 5], 3.0, [0, 0, -5])),
        "move.walk": walk(1.2, 20, 8, 0.3, lantern=rot(0, [-10, 0, 0], 0.6, [10, 0, 0], 1.2, [-10, 0, 0]),
                          skirt=rot(0, [0, 0, 2], 0.6, [0, 0, -2], 1.2, [0, 0, 2])),
        "attack.throw": anim(1.0, {
            "left_arm": rot(0, ZERO, 0.6, [-165, 0, -10], 0.75, [-165, 0, -10], 1.0, ZERO),
            "right_arm": rot(0, ZERO, 0.6, [45, 0, 0], 0.8, [-130, 0, 0], 1.0, ZERO),
            "body": rot(0, ZERO, 0.6, [0, -20, 0], 0.8, [0, 15, 0], 1.0, ZERO),
            "lantern": rot(0, ZERO, 0.6, [160, 0, 0], 0.75, [165, 0, 0], 1.0, ZERO),
        }, loop=False),
    }
    write_model("lantern_hag", bones, animations, [2, 2.5])


# --- Woods: Bark Hollow ----------------------------------------------------------------------------------------------
# A dead oak of the king's park that walks: grooved bark, moss, a knot-hole face whose eyes light when it wakes
# (the eyes bone, shown by the renderer), branch arms with twig fingers, root feet, a crown of dead branches.
# Dormant until a player is within 6; sweep after a 20-tick windup.

def bark_hollow():
    bark = Material(BARK, base=2.4, noise=0.7, grain="v", grad=0.6, top=0.4)

    def bark_deco(face, rng):
        if face.side():
            for _ in range(max(1, face.w // 3)):  # deep grooves
                x = rng.randrange(face.w)
                y = rng.randrange(face.h)
                face.vline(x, y, rng.randint(3, 8), BARK[0])
            for _ in range(face.w * face.h // 60):  # moss
                x, y = rng.randrange(face.w), rng.randrange(face.h)
                face.rect(x, y, rng.randint(1, 3), rng.randint(1, 2), MOSS[rng.randint(1, 3)])
        if face.name == "top":
            for r in range(0, max(face.w, face.h) // 2, 2):  # growth rings on a cut top
                for x in range(face.w):
                    for y in range(face.h):
                        if max(abs(x - face.w // 2), abs(y - face.h // 2)) == r:
                            face.set(x, y, BARK[3])

    def trunk_front(face, rng):
        bark_deco(face, rng)
        # The face: a dark knot hollow for the eyes (y 5..8 from the top: the eyes bone sits at model y 31) and a
        # long gaping mouth below.
        face.rect(3, 4, 8, 5, BARK[0])
        face.rect(4, 5, 6, 3, VOID)
        face.rect(5, 10, 4, 6, BARK[0])
        face.rect(6, 11, 2, 4, VOID)
        face.hline(2, 3, 10, BARK[4])

    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 12, 0], "root", None, [
            Box([-7, 12, -5], [14, 26, 10], bark, {"front": trunk_front, "back": bark_deco, "left": bark_deco,
                                                   "right": bark_deco, "top": bark_deco}),
            Box([-5, 30, 4.5], [4, 3, 2], bark.but(base=3.0), {"sides": lambda f, r: f.rect(1, 1, 2, 1, BARK[0])}),
        ]),
        Bone("eyes", [0, 31, -5], "body", None, [
            Box([-4, 31, -5.5], [2, 1, 1], Material(ramp("ffb347", "ffd27a"), base=0.6, noise=0.3, glow=1.0)),
            Box([2, 31, -5.5], [2, 1, 1], Material(ramp("ffb347", "ffd27a"), base=0.6, noise=0.3, glow=1.0)),
        ]),
    ]
    twig = Material(BARK, base=3.0, noise=0.6, grain="v")
    bones.append(Bone("crown", [0, 38, 0], "body", None, [
        Box([-5, 38, -4], [10, 6, 8], bark, {"sides": bark_deco, "top": bark_deco}),
        Box([-9, 42, -1], [4, 8, 2], twig, rotation=[0, 0, 25], pivot=[-5, 42, 0]),
        Box([-11, 49, -0.5], [2, 5, 1], twig, rotation=[0, 0, 50], pivot=[-9, 49, 0]),
        Box([5, 41, -1], [3, 10, 2], twig, rotation=[0, 0, -15], pivot=[6, 41, 0]),
        Box([7, 50, -0.5], [1, 5, 1], twig, rotation=[0, 0, -45], pivot=[7, 50, 0]),
        Box([-1, 43, 1], [2, 7, 2], twig, rotation=[-20, 0, 0], pivot=[0, 43, 2]),
        Box([-2, 43, -3], [3, 3, 3], Material(MOSS, base=2.0, noise=0.7), inflate=0.2),
    ]))
    arm = Box([-12, 12, -2.5], [5, 24, 5], bark, {"sides": bark_deco})
    fingers = [Box([-13, 9, -1], [2, 4, 2], twig), Box([-10, 8, -1], [1, 5, 1], twig, rotation=[0, 0, -15],
                                                         pivot=[-9.5, 12, -0.5]),
               Box([-12, 8, 1], [1, 4, 1], twig, rotation=[20, 0, 0], pivot=[-11.5, 12, 1.5])]
    bones.append(Bone("right_arm", [-9, 34, 0], "body", None, [arm] + fingers))
    bones.append(Bone("left_arm", [9, 34, 0], "body", None, [mirror_box(arm)] + [mirror_box(f) for f in fingers]))
    leg = Box([-6, 0, -3], [5, 12, 6], bark, {"sides": bark_deco})
    roots = Box([-7, 0, -4.5], [6, 2, 3], bark.but(base=1.6), rotation=[-10, 0, 0], pivot=[-4, 1, -3])
    bones.append(Bone("right_leg", [-3.5, 12, 0], "root", None, [leg, roots]))
    bones.append(Bone("left_leg", [3.5, 12, 0], "root", None, [mirror_box(leg), mirror_box(roots)]))
    raised_r, raised_l = [0, 0, -35], [0, 0, 35]
    animations = {
        "misc.dormant": anim(2.0, {"right_arm": rot(0, raised_r, 2, raised_r), "left_arm": rot(0, raised_l, 2, raised_l)}),
        "misc.awaken": anim(1.0, {
            "body": rot(0, ZERO, 0.2, [0, 0, 3], 0.4, [0, 0, -3], 0.6, [0, 0, 2], 0.8, ZERO),
            "right_arm": rot(0, raised_r, 1.0, ZERO),
            "left_arm": rot(0, raised_l, 1.0, ZERO),
            "crown": rot(0, ZERO, 0.3, [0, 0, -4], 0.6, [0, 0, 4], 1.0, ZERO),
        }, loop=False),
        "misc.idle": anim(4.0, {
            "crown": rot(0, ZERO, 2.0, [2, 0, 2], 4.0, ZERO),
            "right_arm": rot(0, ZERO, 2.0, [-2, 0, 1], 4.0, ZERO),
            "left_arm": rot(0, ZERO, 2.0, [-2, 0, -1], 4.0, ZERO),
            "body": rot(0, ZERO, 2.0, [1, 0, 0], 4.0, ZERO),
        }),
        "move.walk": walk(1.6, 18, 10, 0.6, body=rot(0, [0, 0, 3], 0.8, [0, 0, -3], 1.6, [0, 0, 3]),
                          crown=rot(0, [0, 0, -2], 0.8, [0, 0, 2], 1.6, [0, 0, -2])),
        "attack.sweep": anim(1.6, {
            "right_arm": rot(0, ZERO, 1.0, [-70, 0, -70], 1.15, [-80, 0, 50], 1.6, ZERO),
            "body": rot(0, ZERO, 1.0, [0, 40, 0], 1.15, [0, -50, 0], 1.6, ZERO),
        }, loop=False),
    }
    write_model("bark_hollow", bones, animations, [4, 3.5])


# --- Necropolis: Restless Bones --------------------------------------------------------------------------------------
# A kingdom's dead, burned without rites: yellowed bones, an open ribcage, a scrap of burial shroud on one shoulder,
# a rusted sword. Collapses into a bone pile at 0 HP and re-forms; swing windup 10 ticks.

def restless_bones():
    bonem = Material(BONE, base=3.0, noise=0.6, speck=0.04, grad=0.8, top=0.6)
    dark = Material(BONE, base=1.6, noise=0.5)

    def skull_front(face, rng):
        for ex in (1, 5):
            face.rect(ex, 2, 2, 2, VOID)
        face.lit(2, 3, COLD, 0.9)
        face.lit(5, 3, COLD, 0.9)
        face.set(3, 4, BONE[1])
        face.set(4, 4, BONE[1])
        face.hline(1, 5, 6, BONE[2])
        crack(face, rng, BONE[1], None, 3, 6, 0)

    def jaw_front(face, rng):
        for x in range(face.w):
            face.set(x, 0, BONE[5] if x % 2 else BONE[1])

    def cage(face, rng):
        # Ribs: every other row open (cutout), the sternum and the spine left standing.
        if face.name in ("front", "back"):
            for y in range(1, face.h, 2):
                for x in range(1, face.w - 1):
                    if x != face.cx and x != face.cx - 1:
                        face.clear(x, y)
        elif face.side():
            for y in range(1, face.h, 2):
                for x in range(face.w):
                    face.clear(x, y)
        else:
            clear_all(face)

    def limb(face, rng):
        if face.side():
            face.tint(0, face.h // 2, -30)
            face.tint(face.w - 1, face.h // 2, -30)
            face.hline(0, face.h // 2, face.w, BONE[2])

    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 12, 0], "root", None, [
            Box([-1, 12, -1], [2, 12, 2], dark),
            Box([-3, 12, -1.5], [6, 2, 3], bonem),
            Box([-4, 16, -2], [8, 7, 4], bonem, {"all": cage}),
            Box([-4.5, 23, -1], [9, 1, 2], bonem),
        ]),
        Bone("head", [0, 24, 0], "body", None, [
            Box([-4, 25, -4], [8, 7, 8], bonem, {"front": skull_front,
                                                 "sides": lambda f, r: crack(f, r, BONE[1], None, 3)}),
            Box([-3, 23.5, -4.2], [6, 2, 5], bonem.but(base=2.6), {"front": jaw_front}),
        ]),
    ]
    arm = Box([-6.5, 12, -1], [2, 12, 2], bonem, {"all": limb})
    shroud = Box([-6.5, 16, -2.5], [5, 8, 5], Material(SHROUD, base=2.6, noise=0.6, grain="v"),
                 {"sides": lambda f, r: (tatter(f, r, 3, 0.6), holes(f, r, 1)), "bottom": clear_all},
                 inflate=0.2)
    bones.append(Bone("right_arm", [-5, 22, 0], "body", None, [arm, shroud]))
    bones.append(Bone("left_arm", [5, 22, 0], "body", None, [mirror_box(arm)]))
    leg = Box([-2.5, 0, -1], [2, 12, 2], bonem, {"all": limb})
    bones.append(Bone("right_leg", [-1, 12, 0], "root", None, [leg]))
    bones.append(Bone("left_leg", [1, 12, 0], "root", None, [mirror_box(leg)]))
    rust = Material(RUST, base=2.2, noise=0.8, speck=0.2)

    def blade(face, rng):
        for x in range(face.w):
            if rng.random() < 0.35:
                face.set(x, 0, IRON[3])
        if face.side() and face.w > 3:
            face.clear(face.w - 1 - rng.randrange(3), 0)  # a notch

    bones.append(Bone("sword", [-5, 12, 0], "right_arm", None, [
        Box([-5.5, 11, -12], [1, 1, 11], rust, {"all": blade}),
        Box([-6.5, 11, -2], [3, 1, 1], Material(IRON, base=1.6, noise=0.5)),
        Box([-5.5, 11, -1], [1, 1, 2], Material(LEATHER, base=1.5, noise=0.4)),
    ]))
    pile = {
        "root": pos(0, [0, -11, 0]),
        "body": rot(0, [85, 0, 10]),
        "head": rot(0, [-60, 30, 0]),
        "right_arm": rot(0, [-20, 0, 80]), "left_arm": rot(0, [10, 0, -85]),
        "right_leg": rot(0, [80, 30, 0]), "left_leg": rot(0, [80, -40, 0]),
    }

    def at(time, frames):
        return {b: {k: {str(float(time)): list(v.values())[0]} for k, v in ch.items()} for b, ch in frames.items()}

    standing = {b: {k: {"0.0": ZERO} for k in ch} for b, ch in pile.items()}
    animations = {
        "misc.idle": idle(3.0, 3, 0, head=rot(0, ZERO, 1.5, [3, 6, 0], 3.0, ZERO)),
        "move.walk": walk(1.0, 30, 25, 0.4, head=rot(0, [0, 4, 0], 0.5, [0, -4, 0], 1.0, [0, 4, 0])),
        "attack.swing": overhead(0.5, 0.9),
        "misc.collapse": anim(0.5, merge(standing, at(0.5, pile)), loop=False),
        "misc.pile": anim(1.0, merge(pile, at(1.0, pile))),
        "misc.reform": anim(1.0, merge(pile, at(1.0, standing)), loop=False),
    }
    write_model("restless_bones", bones, animations, [2, 2.5])


# --- Necropolis: Crypt Moth ------------------------------------------------------------------------------------------
# A big dusty moth of the crypts: furry body, feathery antennae, broad wings with eye spots. The glow_* bones (shown
# by the renderer) light the wing pattern 15 ticks before the pollen burst.

PALE_GLOW = hexc("d8ffb0")


def wing_shape(face, rng, kind, outer_left):
    """Rounds a flat wing's outline: clears the corners away from the body. outer_left: the outer (wingtip) end is
    at face x = 0."""
    w, h = face.w, face.h
    for y in range(h):
        for x in range(w):
            ox = x if outer_left else w - 1 - x      # 0 at the tip
            if kind == "fore":
                cut = (ox < 2 and (y < 1 or y > h - 3)) or (ox < 1 and y > h - 4) or (ox > w - 3 and y > h - 2)
            else:
                cut = (ox < 2 and (y < 1 or y > h - 2)) or (ox < 1)
            if cut:
                face.clear(x, y)


def crypt_moth():
    fur = Material(DUST, base=2.6, noise=0.8, speck=0.08)

    def head_front(face, rng):
        face.rect(0, 0, 1, 2, VOID)
        face.rect(face.w - 1, 0, 1, 2, VOID)
        face.lit(0, 0, hexc("cfe6a0"), 0.5)
        face.lit(face.w - 1, 0, hexc("cfe6a0"), 0.5)

    def comb(face, rng):
        # A feathery antenna: a shaft on one column with barbs every other row.
        for y in range(face.h):
            for x in range(face.w):
                if x != 0 and y % 2 == 1:
                    face.clear(x, y)

    def abdomen(face, rng):
        if face.side():
            for x in range(0, face.w, 2):
                face.vline(x, 0, face.h, DUST[1])

    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 5, 0], "root", None, [
            Box([-1.5, 3, -3], [3, 3, 4], fur),
            Box([-1, 3, 1], [2, 2, 4], fur.but(base=2.0), {"all": abdomen}),
            Box([-2, 5, -3.5], [4, 2, 2], fur.but(base=3.6), inflate=0.2),
        ]),
        Bone("head", [0, 5, -3], "body", None, [
            Box([-1.5, 3.5, -5.5], [3, 3, 3], fur.but(base=1.8), {"front": head_front}),
            Box([-2, 6, -6], [2, 5, 0], Material(DUST, base=3.4, noise=0.5), {"all": comb},
                rotation=[-40, 0, 20], pivot=[-1, 6, -5.5]),
            Box([0, 6, -6], [2, 5, 0], Material(DUST, base=3.4, noise=0.5), {"all": comb},
                rotation=[-40, 0, -20], pivot=[1, 6, -5.5]),
        ]),
    ]
    wing_mat = Material(DUST, base=2.8, noise=0.8, cell=2.0)

    def fore_pattern(face, rng, glow_only=False):
        outer_left = face.name == "top"
        if face.name not in ("top", "bottom"):
            return
        w, h = face.w, face.h
        for y in range(h):
            for x in range(w):
                ox = x if outer_left else w - 1 - x
                ring = abs(ox - 3) + abs(y - 3)
                if ring == 2:
                    face.lit(x, y, PALE_GLOW, 0.9) if glow_only else face.set(x, y, DUST[0])
                elif ring == 0:
                    face.lit(x, y, PALE_GLOW, 1.0) if glow_only else face.set(x, y, DUST[5])
                elif ox == 6 and not glow_only:
                    face.set(x, y, DUST[1])
                elif glow_only and ox in (6, 7) and y % 2 == 0:
                    face.lit(x, y, PALE_GLOW, 0.6)
        wing_shape(face, rng, "fore", outer_left)

    def fore(face, rng):
        if face.name in ("top", "bottom"):
            fore_pattern(face, rng)
        else:
            clear_all(face)

    def fore_glow(face, rng):
        clear_all(face)
        if face.name in ("top", "bottom"):
            fore_pattern(face, rng, True)

    def hind(face, rng):
        if face.name in ("top", "bottom"):
            outer_left = face.name == "top"
            for y in range(face.h):
                for x in range(face.w):
                    ox = x if outer_left else face.w - 1 - x
                    if ox == 2 and y in (1, 2, 3):
                        face.set(x, y, DUST[0])
            wing_shape(face, rng, "hind", outer_left)
        else:
            clear_all(face)

    fw = Box([-10.5, 6, -4], [9, 0, 7], wing_mat, {"all": fore})
    fg = Box([-10.5, 6, -4], [9, 0, 7], wing_mat, {"all": fore_glow}, inflate=0.05)
    hw = Box([-7.5, 5, 1], [6, 0, 5], wing_mat.but(base=2.2), {"all": hind})
    bones += [
        Bone("right_wing", [-1.5, 6, -1], "body", None, [fw]),
        Bone("glow_right", [-1.5, 6, -1], "right_wing", None, [fg]),
        Bone("left_wing", [1.5, 6, -1], "body", None, [mirror_box(fw)]),
        Bone("glow_left", [1.5, 6, -1], "left_wing", None, [mirror_box(fg)]),
        Bone("right_wing_low", [-1.5, 5, 2], "body", None, [hw]),
        Bone("left_wing_low", [1.5, 5, 2], "body", None, [mirror_box(hw)]),
    ]

    def flap(length, angle):
        h = length / 2
        return {
            "right_wing": rot(0, [0, 0, angle], h, [0, 0, -angle], length, [0, 0, angle]),
            "left_wing": rot(0, [0, 0, -angle], h, [0, 0, angle], length, [0, 0, -angle]),
            "right_wing_low": rot(0, [0, 0, angle * 0.6], h, [0, 0, -angle * 0.6], length, [0, 0, angle * 0.6]),
            "left_wing_low": rot(0, [0, 0, -angle * 0.6], h, [0, 0, angle * 0.6], length, [0, 0, -angle * 0.6]),
        }

    animations = {
        "move.fly": anim(0.3, merge(flap(0.3, 40), {"root": pos(0, ZERO, 0.15, [0, 0.6, 0], 0.3, ZERO)})),
        "misc.idle": anim(0.6, merge(flap(0.6, 25), {"head": rot(0, ZERO, 0.3, [4, 0, 0], 0.6, ZERO)})),
        "attack.burst": anim(0.75, {
            "right_wing": rot(0, ZERO, 0.1, [0, 0, 8], 0.2, [0, 0, -8], 0.3, [0, 0, 8], 0.4, [0, 0, -8], 0.5, [0, 0, 8],
                              0.6, [0, 0, -8], 0.75, [0, 0, 50]),
            "left_wing": rot(0, ZERO, 0.1, [0, 0, -8], 0.2, [0, 0, 8], 0.3, [0, 0, -8], 0.4, [0, 0, 8], 0.5, [0, 0, -8],
                             0.6, [0, 0, 8], 0.75, [0, 0, -50]),
            "root": pos(0, ZERO, 0.75, [0, 2, 0]),
        }, loop=False),
        "attack.bite": anim(0.4, {"head": rot(0, ZERO, 0.15, [-20, 0, 0], 0.25, [30, 0, 0], 0.4, ZERO)}, loop=False),
    }
    write_model("crypt_moth", bones, animations, [1.5, 1])


# --- Necropolis: Hollow Sentinel -------------------------------------------------------------------------------------
# A crypt guard that never left its post: old rusted plate, a slate-blue tabard with bronze trim, a great helm with
# a cold glow in the slit, a tower shield and a long spear. Thrust after a 12-tick draw; bash after the shield is
# planted for 15 ticks, then open (shield down) for 30 ticks.

SLATE = ramp("1b2029", "252c38", "303947", "3d4757", "4c5668")


def hollow_sentinel():
    plate = Material(IRON, base=2.6, noise=0.6, speck=0.06, edge=0.8, grad=1.0, top=0.8)

    def rust_spots(face, rng):
        scatter(face, rng, face.w * face.h // 60, RUST[1])
        if rng.random() < 0.5:
            crack(face, rng, RUST[2], None, 2)

    def cuirass(face, rng):
        rust_spots(face, rng)
        if face.name == "front":
            face.vline(face.cx, 1, face.h - 2, IRON[5])
            face.hline(1, 4, face.w - 2, IRON[1])

    def greave(face, rng):
        rust_spots(face, rng)
        if face.side():
            face.hline(0, face.h // 2, face.w, IRON[5])
            face.hline(0, face.h // 2 + 1, face.w, IRON[1])

    bones, top = humanoid(legs=13, body=(10, 14, 6), head=(8, 8, 8), arm=(4, 14, 4), leg=(5, 5),
                          mats={"body": plate, "head": Material(BONE, base=1.0, noise=0.4), "arm": plate.but(base=2.3),
                                "leg": plate.but(base=2.2)},
                          decals={"body": {"all": cuirass}, "arm": {"all": rust_spots}, "leg": {"all": greave},
                                  "head": {"front": lambda f, r: (f.rect(0, 0, f.w, f.h, VOID),
                                                                  f.lit(2, 3, COLD, 1.0), f.lit(5, 3, COLD, 1.0))}})

    def helm(face, rng):
        rust_spots(face, rng)
        if face.name == "front":
            face.hline(1, 3, face.w - 2, VOID)     # eye slit
            face.vline(face.cx, 3, 4, VOID)        # breath slot
            for y in (3,):
                for x in range(1, face.w - 1):
                    face.clear(x, y)
            for y in range(4, 7):
                face.clear(face.cx, y)
            rivets(face, IRON[5], 3)
        if face.name == "bottom":
            clear_all(face)

    head = bone(bones, "head")
    head.add(Box([-4.5, top, -4.5], [9, 9, 9], plate, {"all": helm}),
             Box([-0.5, top + 9, -4.5], [1, 2, 9], plate.but(base=3.4)))
    tabard = Material(SLATE, base=2.4, noise=0.5, grain="v")

    def tabard_deco(face, rng):
        if face.name in ("front", "back"):
            trim(face, BRONZE[3], ("left", "right", "top"))
            ragged_edge(face, rng, "bottom", 2, 0.5)
            if face.name == "front":
                face.rect(face.cx - 1, 3, 2, 3, BRONZE[4])  # a crypt key sigil
                face.set(face.cx - 1, 6, BRONZE[3])
                face.set(face.cx - 1, 7, BRONZE[3])
                face.set(face.cx, 7, BRONZE[3])
        else:
            clear_all(face)

    body = bone(bones, "body")
    body.add(Box([-3.5, 8, -3.4], [7, 16, 0], tabard, {"all": tabard_deco}),
             Box([-3.5, 8, 3.4], [7, 16, 0], tabard, {"all": tabard_deco}),
             Box([-5, 10, -3], [10, 4, 6], Material(IRON, base=2.0, noise=0.4),
                 {"sides": lambda f, r: (mail(f, r, IRON[1], IRON[4]), ragged_edge(f, r, "bottom", 1, 0.4))},
                 inflate=0.4))
    pauldron = Box([-10, 23, -3.5], [6, 3, 7], plate.but(base=2.8),
                   {"all": lambda f, r: (rust_spots(f, r), band(f, f.h - 1, BRONZE[2]))}, inflate=0.2)
    bone(bones, "right_arm").add(pauldron)
    bone(bones, "left_arm").add(mirror_box(pauldron))
    knee = Box([-5, 6, -3], [5, 2, 1], plate.but(base=2.6))
    bone(bones, "right_leg").add(knee)
    bone(bones, "left_leg").add(mirror_box(knee))
    wood = Material(WOOD, base=1.8, noise=0.6, grain="h")

    def shield_front(face, rng):
        # Planks behind an iron rim, a bronze boss and a closed-eye sigil of the crypt.
        for x in range(2, face.w - 2, 3):
            face.vline(x, 1, face.h - 2, WOOD[0])
        trim(face, IRON[2])
        for y in range(face.h):
            face.set(1, y, IRON[3])
            face.set(face.w - 2, y, IRON[3])
        face.hline(2, 8, face.w - 4, IRON[3])
        face.hline(2, 16, face.w - 4, IRON[3])
        cx = face.cx
        face.hline(cx - 3, 12, 6, BRONZE[4])
        face.set(cx - 2, 11, BRONZE[3])
        face.set(cx + 1, 11, BRONZE[3])
        face.hline(cx - 1, 13, 2, BRONZE[2])

    shield_mat = Material(WOOD, base=2.2, noise=0.6, grain="v")
    bones.append(Bone("shield", [7, 14, -3], "left_arm", None, [
        Box([-1, 6, -6], [12, 24, 1], shield_mat, {"front": shield_front, "back": shield_front,
                                                   "left": lambda f, r: f.rect(0, 0, f.w, f.h, IRON[2]),
                                                   "right": lambda f, r: f.rect(0, 0, f.w, f.h, IRON[2]),
                                                   "top": lambda f, r: f.rect(0, 0, f.w, f.h, IRON[3]),
                                                   "bottom": lambda f, r: f.rect(0, 0, f.w, f.h, IRON[1])}),
        Box([3, 16, -6.6], [4, 4, 1], Material(BRONZE, base=3.0, noise=0.5, top=0.8), inflate=-0.2),
    ]))
    leaf = Material(IRON, base=3.6, noise=0.4, top=0.6)
    bones.append(Bone("spear", [-7, 13, 0], "right_arm", None, [
        Box([-7.5, 12.5, -20], [1, 1, 30], wood, {"sides": lambda f, r: f.set(f.w - 4, 0, LEATHER[1])}),
        Box([-8, 12, -21], [2, 2, 1], Material(BRONZE, base=2.5, noise=0.4)),
        Box([-8.5, 12.5, -25], [3, 1, 4], leaf),
        Box([-8, 12.5, -27], [2, 1, 2], leaf.but(base=4.2)),
    ]))
    guard = [-25, -20, 0]
    animations = {
        "misc.idle": idle(3.0, 2, 1, left_arm=rot(0, guard, 3, guard)),
        "move.walk": walk(1.2, 20, 0, 0.4, left_arm=rot(0, guard, 1.2, guard)),
        "misc.guard": anim(1.0, {"left_arm": rot(0, [-35, -30, 0], 1, [-35, -30, 0]),
                                 "body": rot(0, [5, 0, 0], 1, [5, 0, 0]),
                                 "head": rot(0, [8, 0, 0], 1, [8, 0, 0])}),
        # The spear is drawn back through the 12-tick windup and driven forward level at 0.6 s.
        "attack.thrust": anim(0.9, {
            "right_arm": rot(0, ZERO, 0.6, [10, 0, 0], 0.65, [-12, 0, 0], 0.9, ZERO),
            "spear": pos(0, ZERO, 0.6, [0, 0, 6], 0.65, [0, 0, -9], 0.8, [0, 0, -6], 0.9, ZERO),
            "body": rot(0, ZERO, 0.6, [0, 25, 0], 0.65, [8, -15, 0], 0.9, ZERO),
            "left_arm": rot(0, guard, 0.9, guard),
        }, loop=False),
        "attack.bash": anim(1.1, {
            "body": rot(0, ZERO, 0.75, [12, 0, 0], 0.85, [-5, 0, 0], 1.1, ZERO),
            # Shield planted square in front through the windup, then driven forward and across.
            "left_arm": rot(0, guard, 0.75, [-35, -5, 0], 0.85, [-40, 30, 0], 1.1, guard),
            "root": pos(0, ZERO, 0.75, [0, 0, 1], 0.85, [0, 0, -4], 1.1, ZERO),
        }, loop=False),
        "misc.open": anim(1.5, {
            "left_arm": rot(0, [20, 0, -25], 1.5, [20, 0, -25]),
            "body": rot(0, [15, 0, 0], 0.75, [18, 0, 0], 1.5, [15, 0, 0]),
            "head": rot(0, [20, 0, 0], 1.5, [20, 0, 0]),
        }),
    }
    write_model("hollow_sentinel", bones, animations, [3, 3])


# --- Necropolis: Grave Warden ----------------------------------------------------------------------------------------
# The bell-ringer of the dead: a hulking hooded figure in a heavy shroud, a bone mask with a warm glow behind it,
# keys at the belt, a bronze bell in the left hand and an iron-banded cudgel. Raises the bell for 20 ticks before it
# rings; swing windup 12 ticks.

WARM = hexc("ffdc8c")


def grave_warden():
    robe = Material(SHROUD, base=2.4, noise=0.6, grain="v", edge=0.5)

    def mask(face, rng):
        face.rect(0, 0, face.w, face.h, BONE[3])
        for x in range(face.w):
            face.set(x, 0, BONE[1])
        for ex in (1, 5):
            face.rect(ex, 3, 2, 2, VOID)
            face.lit(ex + (1 if ex == 1 else 0), 3, WARM, 0.9)
        face.hline(2, 6, 4, BONE[1])
        for x in range(2, 6):
            face.set(x, 7 if x % 2 else 6, BONE[0])  # stitched mouth
        crack(face, rng, BONE[1], None, 3, 6, 1)

    def robe_deco(face, rng):
        if face.name == "front":
            face.vline(face.cx, 0, face.h, SHROUD[0])
        if face.side():
            band(face, 3, ROPE[1])
            if face.name == "front":
                face.set(face.cx - 3, 4, BRONZE[4])
                face.vline(face.cx - 3, 5, 3, BRONZE[3])
                face.set(face.cx - 2, 7, BRONZE[3])
                face.set(face.cx + 2, 4, BRONZE[4])
                face.vline(face.cx + 2, 5, 2, BRONZE[2])

    def sleeve(face, rng):
        lower(face, 3, SKIN_OLD, rng, 1.2)
        band(face, face.h - 4, SHROUD[5])

    bones, top = humanoid(legs=12, body=(12, 16, 7), head=(8, 8, 8), arm=(5, 15, 5), leg=(5, 5), hunch=10,
                          mats={"body": robe, "head": Material(BONE, base=3.0, noise=0.4),
                                "arm": robe.but(base=2.2), "leg": robe.but(base=1.4)},
                          decals={"body": {"all": robe_deco}, "head": {"front": mask}, "arm": {"all": sleeve}})
    hood = Material(SHROUD, base=2.0, noise=0.6, grain="v", top=0.6)
    head = bone(bones, "head")
    head.add(Box([-4.5, top, -4.5], [9, 9, 9], hood, {"front": lambda f, r: open_front(f, 1, 1),
                                                       "bottom": clear_all}),
             Box([-1.5, top + 6, 2], [3, 3, 5], hood.but(base=1.6), rotation=[-35, 0, 0], pivot=[0, top + 8, 3]))
    body = bone(bones, "body")
    body.add(Box([-7, top - 4, -4], [14, 4, 8], robe.but(base=3.0),
                 {"sides": lambda f, r: ragged_edge(f, r, "bottom", 2, 0.5)}, inflate=0.3))
    bones.append(Bone("robe", [0, 12, 0], "root", None, [
        Box([-6.5, 2, -4], [13, 10, 8], robe.but(base=1.9), {"sides": lambda f, r: tatter(f, r, 2, 0.5)})]))
    bronze = Material(BRONZE, base=3.0, noise=0.6, speck=0.08, grad=1.4, top=0.8)

    def bell_face(face, rng):
        if face.side():
            band(face, 1, BRONZE[5])
            scatter(face, rng, 2, hexc("4d6b4a"))  # verdigris

    bones.append(Bone("bell", [8.5, 13, 0], "left_arm", None, [
        Box([8, 9, -0.5], [1, 4, 1], Material(WOOD, base=2.0, noise=0.4)),
        Box([6.5, 8, -2], [4, 1, 4], bronze),
        Box([5.5, 3, -3], [6, 5, 6], bronze, {"all": bell_face}),
        Box([5, 2, -3.5], [7, 1, 7], bronze.but(base=3.6)),
        Box([8, 1, -0.5], [1, 2, 1], Material(IRON, base=1.6, noise=0.3)),
    ]))
    iron = Material(IRON, base=2.2, noise=0.6, speck=0.1)
    bones.append(Bone("cudgel", [-8.5, 13, 0], "right_arm", None, [
        Box([-9.5, 12, -14], [2, 2, 14], Material(WOOD, base=2.0, noise=0.6, grain="h"),
            {"sides": lambda f, r: [f.vline(x, 0, f.h, IRON[2]) for x in range(2, f.w, 5)]}),
        Box([-10.5, 11, -17], [4, 4, 4], iron, {"all": lambda f, r: rivets(f, IRON[5], 2)}),
    ]))
    ring = keys(0, ZERO, 1.0, ZERO, 1.1, [0, 0, 25], 1.2, [0, 0, -25], 1.3, [0, 0, 20], 1.4, ZERO)
    animations = {
        "misc.idle": idle(3.5, 3, 2, bell=rot(0, [0, 0, -3], 1.75, [0, 0, 3], 3.5, [0, 0, -3])),
        "move.walk": walk(1.2, 20, 12, 0.5, robe=rot(0, [0, 0, 2], 0.6, [0, 0, -2], 1.2, [0, 0, 2]),
                          bell=rot(0, [0, 0, 6], 0.6, [0, 0, -6], 1.2, [0, 0, 6])),
        "attack.swing": overhead(0.6, 1.1),
        "attack.bell": anim(1.6, {
            "left_arm": rot(0, ZERO, 1.0, [-170, 0, -10], 1.4, [-170, 0, -10], 1.6, ZERO),
            "bell": {"rotation": ring},
            "head": rot(0, ZERO, 1.0, [-20, 0, 0], 1.6, ZERO),
        }, loop=False),
    }
    write_model("grave_warden", bones, animations, [3, 3])


# --- Citadel: Cinder Hound ------------------------------------------------------------------------------------------
# A lean hound of the burning streets: charred hide split by glowing seams along the ribs, a mane of ash spikes, a
# mouth that glows inside, an ember-tipped tail. Bite after a 6-tick lunge; howls to pull the pack.

def cinder_hound():
    hide = Material(CHAR, base=2.2, noise=0.7, speck=0.05, top=1.0, grad=0.8)

    def ribs_glow(face, rng):
        if face.name in ("left", "right"):
            for x in range(1, face.w - 1, 2):
                face.vline(x, 1, face.h - 3, CHAR[0])
                if rng.random() < 0.6:
                    face.lit(x, rng.randint(2, max(2, face.h - 3)), EMBER, 0.9)
        if face.name == "top":
            char_cracks(face, rng, 1)

    def head_side(face, rng):
        if face.name in ("left", "right"):
            ex = 1 if face.name == "right" else face.w - 2
            face.set(ex, 1, VOID)
            face.lit(ex, 1, EMBER_HOT, 1.0)
        if face.name == "front":
            face.lit(1, 1, EMBER_HOT, 1.0)
            face.lit(face.w - 2, 1, EMBER_HOT, 1.0)
            face.set(1, 2, VOID)
            face.set(face.w - 2, 2, VOID)

    def jaw(face, rng):
        if face.name == "top":
            face.rect(0, 0, face.w, face.h, VOID)
            for y in range(face.h):
                face.lit(face.w // 2, y, EMBER, 0.8)
        if face.side():
            for x in range(0, face.w, 2):
                face.set(x, 0, BONE[4])

    def snout(face, rng):
        if face.name == "bottom":
            for x in range(0, face.w, 2):
                face.set(x, face.h - 1, BONE[4])
        if face.name == "front":
            face.set(face.cx, 0, VOID)

    spike = Material(ASH, base=1.6, noise=0.6, top=1.2)
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 10, 0], "root", None, [
            Box([-3, 6.5, -6.5], [6, 7, 6], hide, {"all": ribs_glow}),
            Box([-2.5, 7.5, -0.5], [5, 5, 7], hide.but(base=2.0), {"all": ribs_glow}),
        ] + [Box([-1, 12.5, z], [2, 2, 2], spike, rotation=[-40, 0, 0], pivot=[0, 13, z + 1])
             for z in (-6, -3.5, -1, 1.5)]),
        Bone("head", [0, 11, -6], "body", None, [
            Box([-2.5, 8.5, -11], [5, 5, 5], hide.but(base=2.6), {"all": head_side}),
            Box([-1.5, 9, -15], [3, 3, 4], hide.but(base=2.4), {"all": snout}),
            Box([-1.5, 8, -14.5], [3, 1, 4], hide.but(base=1.6), {"all": jaw}),
            Box([-2.5, 13, -8.5], [1, 3, 1], spike, rotation=[-25, 0, 10], pivot=[-2, 13, -8]),
            Box([1.5, 13, -8.5], [1, 3, 1], spike, rotation=[-25, 0, -10], pivot=[2, 13, -8]),
        ]),
        Bone("tail", [0, 12, 7], "body", [-30, 0, 0], [
            Box([-1, 11, 6.5], [2, 2, 6], hide, {"sides": lambda f, r: char_cracks(f, r, 1)}),
            Box([-1, 11, 12.5], [2, 2, 2], Material(ramp("a0401a", "e06a22", "ff9a3a", "ffd27a"), base=2.0,
                                                    noise=0.6, glow=0.9)),
        ]),
    ]
    leg = Material(CHAR, base=1.8, noise=0.6, grad=1.2)
    paw = Material(CHAR, base=1.0, noise=0.4)
    for name, x, z in (("front_right_leg", -2, -4), ("front_left_leg", 2, -4), ("back_right_leg", -2, 5),
                       ("back_left_leg", 2, 5)):
        ox = -3 if x < 0 else 1
        oz = -5 if z < 0 else 4
        bones.append(Bone(name, [x, 7, z], "root", None, [
            Box([ox, 1, oz], [2, 6, 2], leg, {"sides": lambda f, r: r.random() < 0.3 and ember_cracks(
                f, r, 1, EMBER_DEEP, EMBER_DARK, 0.7, (1, 2))}),
            Box([ox, 0, oz - 0.5], [2, 1, 2], paw, {"front": lambda f, r: f.set(0, 0, BONE[3])}),
        ]))

    def gait(length, angle):
        h = length / 2
        a, b = [angle, 0, 0], [-angle, 0, 0]
        return {
            "front_right_leg": rot(0, a, h, b, length, a), "back_left_leg": rot(0, a, h, b, length, a),
            "front_left_leg": rot(0, b, h, a, length, b), "back_right_leg": rot(0, b, h, a, length, b),
            "tail": rot(0, [-30, 10, 0], h, [-30, -10, 0], length, [-30, 10, 0]),
            "body": pos(0, ZERO, length / 4, [0, 0.6, 0], h, ZERO, h + length / 4, [0, 0.6, 0], length, ZERO),
            "head": rot(0, [4, 0, 0], h, [-4, 0, 0], length, [4, 0, 0]),
        }

    animations = {
        "misc.idle": anim(2.0, {"tail": rot(0, [-30, 8, 0], 1, [-30, -8, 0], 2, [-30, 8, 0]),
                                "head": rot(0, ZERO, 1, [5, 0, 0], 2, ZERO),
                                "body": pos(0, ZERO, 1, [0, 0.3, 0], 2, ZERO)}),
        "move.walk": anim(0.5, gait(0.5, 40)),
        "attack.bite": anim(0.5, {
            "head": rot(0, ZERO, 0.3, [-25, 0, 0], 0.35, [20, 0, 0], 0.5, ZERO),
            "body": rot(0, ZERO, 0.3, [5, 0, 0], 0.35, [-5, 0, 0], 0.5, ZERO),
        }, loop=False),
        "misc.howl": anim(1.0, {"head": rot(0, ZERO, 0.2, [-55, 0, 0], 0.8, [-55, 0, 0], 1.0, ZERO),
                                "tail": rot(0, [-30, 0, 0], 0.2, [-60, 0, 0], 0.8, [-60, 0, 0], 1.0, [-30, 0, 0])},
                            loop=False),
    }
    write_model("cinder_hound", bones, animations, [1.5, 1.2])


# --- Citadel: Sparking Marksman ------------------------------------------------------------------------------------
# A crossbowman on the burning walls: soot-red quilted gambeson, a kettle hat over a mail hood, a scarf over the
# face, bolts at the hip. A spark lights on the bolt tip (the spark bone) 20 ticks before it fires.

GAMBESON = ramp("2a1714", "3c201b", "512b23", "67372c", "7d4536")


def sparking_marksman():
    quilt = Material(GAMBESON, base=2.4, noise=0.5, edge=0.4)

    def quilted(face, rng):
        if face.side():
            for y in range(1, face.h, 3):
                face.hline(0, y, face.w, GAMBESON[1])
            for x in range(1, face.w, 3):
                for y in range(face.h):
                    if (y // 3) % 2 == 0:
                        face.set(x, y, GAMBESON[1])
        if face.name == "front":
            band(face, face.h - 3, LEATHER[1])
            face.set(face.cx, face.h - 3, BRONZE[4])

    def face_front(face, rng):
        face.rect(0, 0, face.w, 2, IRON[1])  # mail coif edge
        face.set(2, 3, VOID)
        face.set(5, 3, VOID)
        face.lit(2, 3, EMBER, 0.9)
        face.lit(5, 3, EMBER, 0.9)
        face.rect(0, 5, face.w, 3, GAMBESON[3])  # scarf
        face.hline(0, 5, face.w, GAMBESON[4])

    def sleeve(face, rng):
        lower(face, 3, LEATHER, rng, 2.0)
        quilted(face, rng) if face.name != "front" else None

    bones, top = humanoid(legs=12, body=(8, 12, 4), head=(8, 8, 8), arm=(4, 12, 4), leg=(4, 4),
                          mats={"body": quilt, "head": Material(SKIN_OLD, base=1.4, noise=0.4),
                                "arm": quilt.but(base=2.0), "leg": Material(LEATHER, base=1.6, noise=0.5, grain="v")},
                          decals={"body": {"all": quilted}, "head": {"front": face_front}, "arm": {"all": sleeve},
                                  "leg": {"all": lambda f, r: lower(f, 4, IRON, r, 1.6)}})
    coif = Material(IRON, base=2.4, noise=0.4)
    head = bone(bones, "head")
    head.add(Box([-4.5, top - 0.5, -4.5], [9, 8, 9], coif, {"all": mail_hood}, inflate=0.1),
             Box([-4.5, top + 7.5, -4.5], [9, 2, 9], Material(IRON, base=2.8, noise=0.5, top=1.0),
                 {"all": lambda f, r: scatter(f, r, 2, RUST[1])}),
             Box([-5.5, top + 7, -5.5], [11, 1, 11], Material(IRON, base=2.4, noise=0.5),
                 {"top": lambda f, r: trim(f, IRON[4])}))
    bone(bones, "body").add(
        Box([1.5, 14, -3], [3, 4, 2], Material(LEATHER, base=2.0, noise=0.4), rotation=[0, 0, -10],
            pivot=[3, 16, -2]),
        Box([2, 18, -2.5], [1, 2, 1], Material(WOOD, base=3.0, noise=0.3), {"top": lambda f, r: f.set(0, 0, IRON[4])}),
        Box([3, 18, -2.5], [1, 2, 1], Material(WOOD, base=3.0, noise=0.3), {"top": lambda f, r: f.set(0, 0, IRON[4])}),
    )
    wood = Material(WOOD, base=1.6, noise=0.5, grain="h")
    bones.append(Bone("crossbow", [-6, 12, 0], "right_arm", None, [
        Box([-7, 11, -9], [2, 2, 8], wood, {"sides": lambda f, r: f.hline(0, 0, f.w, WOOD[3])}),
        Box([-11, 12, -9], [10, 1, 1], Material(IRON, base=2.4, noise=0.4)),
        Box([-11, 12, -8], [1, 1, 1], Material(IRON, base=1.6, noise=0.3)),
        Box([-2, 12, -8], [1, 1, 1], Material(IRON, base=1.6, noise=0.3)),
        Box([-10, 12.5, -7.5], [8, 0, 1], Material(ROPE, base=3.0, noise=0.2)),
        Box([-6.5, 13, -10.5], [1, 0.5, 4], Material(WOOD, base=3.2, noise=0.2), inflate=0.05),
    ]))
    bones.append(Bone("spark", [-6, 12, -10], "crossbow", None, [
        Box([-6.5, 11.5, -11], [1, 1, 1], Material(ramp("ffb347", "ffe9a0"), base=0.8, noise=0.4, glow=1.0),
            inflate=0.2)]))
    aim = [-90, 0, 0]
    animations = {
        "misc.idle": idle(3.0, 3, 2),
        "move.walk": walk(1.0, 25, 15, 0.4),
        "attack.aim": anim(1.0, {
            "right_arm": rot(0, ZERO, 0.3, aim, 1.0, aim),
            "left_arm": rot(0, ZERO, 0.3, [-80, 30, 0], 1.0, [-80, 30, 0]),
            "head": rot(0, ZERO, 0.3, [5, 0, 0], 1.0, [5, 0, 0]),
            "body": rot(0, ZERO, 0.3, [0, -10, 0], 1.0, [0, -10, 0]),
        }, loop=False),
        "attack.shoot": anim(0.3, {"right_arm": rot(0, aim, 0.1, [-110, 0, 0], 0.3, ZERO),
                                   "crossbow": pos(0, ZERO, 0.1, [0, 0, 1.5], 0.3, ZERO)}, loop=False),
    }
    write_model("sparking_marksman", bones, animations, [2, 2.5])


def mail_hood(face, rng):
    """A mail coif: link texture, the face opening cleared on the front, nothing underneath."""
    if face.name == "bottom":
        clear_all(face)
        return
    mail(face, rng, IRON[1], IRON[4])
    if face.name == "front":
        for y in range(2, face.h):
            for x in range(1, face.w - 1):
                face.clear(x, y)


# --- Citadel: Furnace Golem ------------------------------------------------------------------------------------------
# The king's furnace given legs: riveted soot-black cast iron, a glowing grate in the chest, a chimney for a crown,
# heavy fists. Heats with each hit taken: the heat bone (shown by the renderer) lights the seams for 25 ticks before
# the fire wave.

CAST = ramp("141314", "1e1d1f", "29282b", "353438", "434246", "55545a")


def furnace_golem():
    cast = Material(CAST, base=2.6, noise=0.5, speck=0.05, edge=1.0, grad=0.8, top=0.8)

    def plates(face, rng):
        if face.side() or face.name == "top":
            for y in range(4, face.h, 6):
                face.hline(0, y, face.w, CAST[0])
            rivets(face, CAST[5], 3)
            scatter(face, rng, face.w * face.h // 50, RUST[1])

    def grate(face, rng):
        if face.name == "front":
            face.rect(0, 0, face.w, face.h, CAST[1])
            for x in range(1, face.w - 1, 2):
                for y in range(1, face.h - 1):
                    face.lit(x, y, EMBER if (x + y) % 3 else EMBER_HOT, 1.0)
            trim(face, CAST[4])

    def heat_seams(face, rng):
        # Only the seams and rivet lines glow; the rest of the overlay is clear so the iron shows.
        clear_all(face)
        if face.side() or face.name == "top":
            for y in range(4, face.h, 6):
                for x in range(face.w):
                    if rng.random() < 0.8:
                        face.lit(x, y, EMBER, 1.0)
            ember_cracks(face, rng, max(2, face.w // 3), EMBER_HOT, EMBER, 1.0, (2, 5))

    def helm_front(face, rng):
        face.hline(1, 2, face.w - 2, VOID)
        for x in (2, face.w - 3):
            face.lit(x, 2, EMBER_HOT, 1.0)
        face.lit(face.cx, 2, EMBER, 0.6)

    def chimney(face, rng):
        if face.name == "top":
            face.rect(0, 0, face.w, face.h, CAST[0])
            face.lit(1, 1, EMBER, 0.9)
            face.lit(2, 2, EMBER_HOT, 0.9)
        elif face.side():
            band(face, 0, CAST[5])
            band(face, 1, CAST[0])

    body_box = Box([-8, 12, -6], [16, 18, 12], cast, {"all": plates})
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 12, 0], "root", None, [
            body_box,
            Box([-4, 17, -6.5], [8, 8, 1], cast, {"all": grate}),
            Box([-9, 26, -4], [18, 3, 8], cast.but(base=3.2), {"all": lambda f, r: rivets(f, CAST[5], 2)}),
        ]),
        Bone("heat", [0, 12, 0], "body", None, [Box([-8, 12, -6], [16, 18, 12], cast, {"all": heat_seams},
                                                    inflate=0.3)]),
        Bone("head", [0, 30, 0], "body", None, [
            Box([-4, 30, -4], [8, 6, 8], cast.but(base=2.9), {"front": helm_front}),
            Box([-2, 36, -2], [4, 8, 4], cast.but(base=2.2), {"all": chimney}),
        ]),
    ]
    arm = Box([-12.5, 15, -2.5], [4, 13, 5], cast, {"all": plates})
    fist = Box([-13.5, 8, -3.5], [6, 7, 7], cast.but(base=2.2), {"all": lambda f, r: (rivets(f, CAST[5], 2),
                                                                                    f.hline(0, 1, f.w, CAST[0]))})
    shoulder = Box([-14, 24, -3.5], [6, 5, 7], cast.but(base=3.0), {"all": lambda f, r: rivets(f, CAST[5], 2)})
    bones.append(Bone("right_arm", [-10.5, 28, 0], "body", None, [arm, fist, shoulder]))
    bones.append(Bone("left_arm", [10.5, 28, 0], "body", None, [mirror_box(arm), mirror_box(fist),
                                                                   mirror_box(shoulder)]))
    leg = Box([-7, 0, -3], [6, 12, 6], cast.but(base=2.3), {"all": plates})
    foot = Box([-7.5, 0, -4], [7, 2, 7], cast.but(base=1.8))
    bones.append(Bone("right_leg", [-4, 12, 0], "root", None, [leg, foot]))
    bones.append(Bone("left_leg", [4, 12, 0], "root", None, [mirror_box(leg), mirror_box(foot)]))
    animations = {
        "misc.idle": idle(4.0, 2, 1),
        "move.walk": walk(1.6, 18, 12, 0.8),
        "attack.slam": overhead(0.7, 1.2, ("right_arm", "left_arm")),
        "misc.heat": anim(1.25, {
            "body": rot(0, ZERO, 0.25, [0, 0, 2], 0.5, [0, 0, -2], 0.75, [0, 0, 2], 1.0, [0, 0, -2], 1.25, ZERO),
            "right_arm": rot(0, ZERO, 1.25, [0, 0, 25]), "left_arm": rot(0, ZERO, 1.25, [0, 0, -25]),
            "head": rot(0, ZERO, 1.25, [-15, 0, 0]),
        }, loop=False),
        "attack.wave": anim(0.5, {
            "right_arm": rot(0, [0, 0, 25], 0.1, [0, 0, 70], 0.5, ZERO),
            "left_arm": rot(0, [0, 0, -25], 0.1, [0, 0, -70], 0.5, ZERO),
            "head": rot(0, [-15, 0, 0], 0.5, ZERO),
        }, loop=False),
    }
    write_model("furnace_golem", bones, animations, [3, 3.5], tex_w=128)


# --- Citadel: Smoldering Knight --------------------------------------------------------------------------------------
# The king's guard: blackened plate split by ember seams, a tattered crimson surcoat with the gold crown, a crested
# great helm with a red plume, a greatsword with a glowing edge. 3-hit combo; a lunge after a 15-tick crouch.

CRIMSON = ramp("2a0d0c", "3f1411", "571b16", "70241c", "8a2f22")
GOLD = ramp("5a3d12", "7f5a1a", "a67b26", "c99a36", "e8be55")
BLACK_STEEL = ramp("121214", "1b1b1e", "252529", "303035", "3d3d43", "4e4e55")


def smoldering_knight():
    steel = Material(BLACK_STEEL, base=2.8, noise=0.5, edge=0.8, grad=1.0, top=1.0)

    def seams(face, rng):
        if face.side():
            for y in range(3, face.h, 5):
                face.hline(0, y, face.w, BLACK_STEEL[0])
                if rng.random() < 0.7:
                    face.lit(rng.randrange(face.w), y, EMBER, 0.9)

    def helm(face, rng):
        if face.name == "front":
            face.hline(1, 3, face.w - 2, VOID)
            face.lit(2, 3, EMBER_HOT, 1.0)
            face.lit(face.w - 3, 3, EMBER_HOT, 1.0)
            face.vline(face.cx, 4, 3, BLACK_STEEL[0])
            for x in range(1, face.w - 1, 2):
                face.set(x, 6, BLACK_STEEL[0])
        if face.side():
            band(face, 0, GOLD[2])
        if face.name == "bottom":
            clear_all(face)

    bones, top = humanoid(legs=13, body=(9, 14, 5), head=(8, 8, 8), arm=(4, 14, 4), leg=(4, 4),
                          mats={"body": steel, "head": Material(CHAR, base=1.0, noise=0.4), "arm": steel.but(base=2.5),
                                "leg": steel.but(base=2.4)},
                          decals={"body": {"all": seams}, "arm": {"all": seams}, "leg": {"all": seams}})
    head = bone(bones, "head")
    head.add(Box([-4.5, top, -4.5], [9, 9, 9], steel.but(base=3.0), {"all": helm}),
             Box([-0.5, top + 9, -3], [1, 2, 7], Material(GOLD, base=2.6, noise=0.4)),
             Box([-0.5, top + 10, -1], [1, 3, 7], Material(CRIMSON, base=3.0, noise=0.6, grain="h"),
                 {"sides": lambda f, r: ragged_edge(f, r, "right", 2, 0.5)}, rotation=[-20, 0, 0],
                 pivot=[0, top + 10, 0]))
    surcoat = Material(CRIMSON, base=2.6, noise=0.6, grain="v")

    def coat(face, rng):
        if face.name in ("front", "back"):
            ragged_edge(face, rng, "bottom", 3, 0.5)
            holes(face, rng, 1)
            trim(face, GOLD[1], ("left", "right"))
            if face.name == "front":
                cx = face.cx
                face.hline(cx - 2, 4, 4, GOLD[4])          # the crown of the Ashen King
                for x in (cx - 2, cx, cx + 1):
                    face.set(x, 3, GOLD[3])
                face.set(cx - 1, 2, GOLD[4])
        else:
            clear_all(face)

    body = bone(bones, "body")
    body.add(Box([-3.5, 7, -2.8], [7, 16, 0], surcoat, {"all": coat}),
             Box([-3.5, 7, 2.8], [7, 16, 0], surcoat, {"all": coat}),
             Box([-5, 12, -3], [10, 3, 6], steel.but(base=2.0), {"all": lambda f, r: band(f, 1, GOLD[2])},
                 inflate=0.3))
    pauldron = Box([-9.5, top - 4, -3], [5, 4, 6], steel.but(base=3.2),
                   {"all": lambda f, r: (seams(f, r), band(f, f.h - 1, GOLD[2]))}, inflate=0.3)
    bone(bones, "right_arm").add(pauldron)
    bone(bones, "left_arm").add(mirror_box(pauldron))

    def edge(face, rng):
        if face.name in ("top", "bottom"):
            for y in range(face.h):
                face.lit(0, y, EMBER, 0.8)
                face.lit(face.w - 1, y, EMBER_DEEP, 0.6)
        if face.side():
            face.hline(0, 0, face.w, BLACK_STEEL[5])

    bones.append(Bone("sword", [-6.5, 13, 0], "right_arm", None, [
        Box([-7, 12, -22], [1, 3, 18], Material(BLACK_STEEL, base=3.6, noise=0.4), {"all": edge}),
        Box([-7, 12.5, -23], [1, 2, 1], Material(BLACK_STEEL, base=4.0, noise=0.3)),
        Box([-8.5, 12.5, -4], [4, 2, 1], Material(GOLD, base=2.6, noise=0.4)),
        Box([-7, 12.5, -3], [1, 2, 3], Material(LEATHER, base=1.6, noise=0.4)),
        Box([-7, 12.5, 0], [1, 2, 1], Material(GOLD, base=3.0, noise=0.3)),
    ]))

    def cut(windup, length, start, end, body_y=(0, 0)):
        return anim(length, {
            "right_arm": rot(0, ZERO, windup, start, windup + 0.1, end, length, ZERO),
            "body": rot(0, ZERO, windup, [0, body_y[0], 0], windup + 0.1, [0, body_y[1], 0], length, ZERO),
        }, loop=False)

    animations = {
        "misc.idle": idle(3.0, 2, 1),
        "move.walk": walk(1.1, 22, 10, 0.4),
        "attack.combo1": cut(0.3, 0.6, [-120, 0, 40], [-40, 0, -30], (30, -30)),
        "attack.combo2": cut(0.25, 0.55, [-60, 0, -60], [-60, 0, 50], (-30, 35)),
        "attack.combo3": cut(0.4, 0.8, [-170, 0, 0], [-20, 0, 0]),
        "attack.crouch": anim(0.75, {
            "body": rot(0, ZERO, 0.75, [25, 0, 0]),
            "right_leg": rot(0, ZERO, 0.75, [-40, 0, 0]), "left_leg": rot(0, ZERO, 0.75, [30, 0, 0]),
            "right_arm": rot(0, ZERO, 0.75, [40, 0, 0]),
            "root": pos(0, ZERO, 0.75, [0, -3, 0]),
        }, loop=False),
        "attack.lunge": anim(0.5, {
            "right_arm": rot(0, [40, 0, 0], 0.1, [-95, 0, 0], 0.5, ZERO),
            "body": rot(0, [25, 0, 0], 0.1, [10, 0, 0], 0.5, ZERO),
            "root": pos(0, [0, -3, 0], 0.2, ZERO),
        }, loop=False),
    }
    write_model("smoldering_knight", bones, animations, [3, 3])


CITADEL_MOBS = [
    ("cinder_hound", cinder_hound), ("sparking_marksman", sparking_marksman),
    ("furnace_golem", furnace_golem), ("smoldering_knight", smoldering_knight),
]


ZONE_MOBS = [
    ("cinder_husk", cinder_husk), ("ash_stalker", ash_stalker), ("ashen_carrion", ashen_carrion),
    ("withered_thrall", withered_thrall), ("lantern_hag", lantern_hag), ("bark_hollow", bark_hollow),
    ("restless_bones", restless_bones), ("crypt_moth", crypt_moth), ("hollow_sentinel", hollow_sentinel),
    ("grave_warden", grave_warden),
]
MOBS = ZONE_MOBS + CITADEL_MOBS


if __name__ == "__main__":
    wanted = set(sys.argv[1:])
    for name, build in MOBS:
        if not wanted or name in wanted:
            build()
