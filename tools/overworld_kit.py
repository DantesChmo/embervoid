#!/usr/bin/env python3
"""The overworld-mobs track's own kit (PLAN 19.3, docs/art-direction.md §1.8 and §2): the hue-shifted ramps of the
seven overworld and moon-night mobs, face helpers for skin, cloth, mail and plate with structure on every face,
translucency and glow for the entities drawn translucent, and the animation helpers the seven share (a breathing
idle, a weighted walk, the flinch, the fall, the settle of hanging parts).

Not run directly except for its self-test (python3 tools/overworld_kit.py: every ramp checked by the palette rule).
tools/overworld_mob_art.py builds the mobs on it and on tools/art_kit.py (boxes, materials, chamfers, chains, eased
keys). Nothing here is used by the Realm 1 scripts.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from art_kit import (ZERO, Box, clamp, e, fold_lines, hexc, keys, lag_sway, ramp, rivet_ring, shade,  # noqa: E402
                     sway)
from palette import hsv, luminance, yellow_distance  # noqa: E402

# --- Ramps (dark to light, 6-7 colours; shadows cooler and more saturated, lights warmer and paler) ----------------

# Dry corpse ash: violet-grey in the shadow, a warm bone-grey where the light hits.
GHOUL_SKIN = ramp("221d26", "322b33", "453c41", "594f50", "6e635f", "847a71", "9b9284")
# The Ashen Zombie's crusted skin: the same ash a shade bluer and heavier.
ZOMBIE_SKIN = ramp("262129", "37303a", "4a4248", "5e554f", "746a5f", "8b8171", "a39a84")
# Pale living skin, lit by the ember: lilac shadow, warm ivory light.
MAIDEN_SKIN = ramp("5a4a4e", "766260", "8f7a74", "a89287", "bea89a", "d0bdab", "dfd0bb")
# Ash-grey robe, violet in the folds.
MAIDEN_ROBE = ramp("1f1b24", "2d2832", "3d3740", "4e4850", "605a5e", "746d6c", "88817a")
# The Vampire's bloodless skin.
VAMP_SKIN = ramp("6e6478", "857a8a", "9c929d", "b2a9ae", "c7bfbe", "d9d2cc", "e8e2d8")
# Black velvet and black hair: blue-black in the shadow, warm in the sheen (a black's span is small by nature).
V_BLACK = ramp("0c0a12", "151219", "1f1b21", "2a2529", "363029", "433d33", "51493d")
V_HAIR = ramp("0a0910", "13111a", "1d1a22", "272328", "322d2d", "3d372f")
# Crimson lining and sash; burgundy brocade of the doublet.
V_CRIMSON = ramp("2a060c", "44101a", "5f1823", "7b2429", "963427", "ad4a2e", "c4663a")
V_BURGUNDY = ramp("1c0812", "2d0f1b", "3f1724", "522130", "672d35", "80403b", "9c5a48")
V_LACE = ramp("7f7580", "958c90", "aba39f", "bfb8ae", "d1cbbd", "e0dbcb", "ece8da")
V_SILVER = ramp("353946", "4c505c", "646873", "7d8189", "979a9e", "b2b4b3", "cccdc7")
# The Ghost's shroud: moonlit grey-blue thinning to mist; the dark inside its hood.
GHOST_PALE = ramp("3a4a66", "4d6080", "627893", "7a90a8", "94a8ba", "b0c0cb", "cbd6da")
GHOST_DARK = ramp("0f121c", "171b28", "1f2433", "282e3c")
# Flame: deep red at the root, yellow-white at the core.
FLAME = ramp("8c1c0a", "b83410", "dd5316", "f27a22", "fb9d34", "ffc252", "ffe28a")
CORE = ramp("ffb24a", "ffc868", "ffdb8c", "ffe9ae", "fff3cc", "fffae6")
# Leather straps and boots; linen rags gone grey; the Zombie's blue work shirt.
STRAP = ramp("241610", "352217", "48301f", "5c4029", "71523a", "87664b", "9c7a5e")
LINEN = ramp("352c25", "483d32", "5d5041", "72654f", "887a5f", "9d8f70", "b0a382")
CLOTH_BLUE = ramp("171b26", "222838", "2f3747", "3f4756", "505867", "636a74", "78797c")
# The Hollow's plate and mail: cold iron with a warm grey specular; its tabard's faded ember red.
HOLLOW_IRON = ramp("1e2029", "2c2f3a", "3c404a", "4f5359", "636660", "7a7b70", "959283")
TABARD = ramp("2a0f10", "3f1715", "55221a", "6b2f22", "80402c", "95553a", "aa6d4c")
# Bone, rust and gilt from the shared palette.
from palette import BONE as IVORY, GOLD_TRIM, RUST, BRASS, DEAD_WOOD, EMBER as EMBER_RAMP, COLD_FLAME  # noqa: E402,E401

# Spot colours.
VOID = hexc("0c0a0a")
EMBER = hexc("ff8a2a")
EMBER_DEEP = hexc("d8521c")
EMBER_DARK = hexc("7a2a12")
EMBER_HOT = hexc("ffd27a")
COLD = hexc("9fd4ff")
COLD_DIM = hexc("4f7ea8")
GHOST_EYE = hexc("8ff0ff")
BLOOD = hexc("ff2a2a")
BLOOD_DIM = hexc("b0141e")
FANG = hexc("f2eee6")

ALL_RAMPS = {name: value for name, value in list(globals().items())
             if isinstance(value, list) and value and isinstance(value[0], tuple) and name.isupper()}
# Blacks cannot span 35 % of the value range and stay black; the white-hot core is all light by nature.
SMALL_SPAN = ("V_BLACK", "V_HAIR", "GHOST_DARK", "CORE")


def check_ramps():
    """The palette rule (tools/palette.py): monotonic, a value span of 35 % (but the blacks), the light end nearer
    yellow and no more saturated than the dark end. Returns the problems."""
    problems = []
    for name, colors in ALL_RAMPS.items():
        lum = [luminance(c) for c in colors]
        if any(b <= a for a, b in zip(lum, lum[1:])):
            problems.append("%s: not monotonic in value" % name)
        if lum[-1] - lum[0] < 0.35 * 255 and name not in SMALL_SPAN:
            problems.append("%s: value span %.0f < %.0f" % (name, lum[-1] - lum[0], 0.35 * 255))
        h0, s0, _ = hsv(colors[0])
        h1, s1, _ = hsv(colors[-1])
        if (s0 > 0.08 or s1 > 0.08) and name not in ("COLD_FLAME", "GHOST_PALE", "GHOST_DARK", "CLOTH_BLUE", "V_SILVER"):
            if yellow_distance(h1) > yellow_distance(h0) + 2:
                problems.append("%s: light end further from yellow than the dark end (%.0f -> %.0f deg)" % (name, h0, h1))
            if s1 > s0 + 0.05:
                problems.append("%s: light end more saturated than the dark end (%.2f -> %.2f)" % (name, s0, s1))
    return problems


# --- Face helpers ---------------------------------------------------------------------------------------------------

def compose(*fns):
    """Several decals as one (a Box takes one decal per face key)."""
    def fn(face, rng):
        for f in fns:
            if f is not None:
                f(face, rng)
    return fn


def on(names, fn):
    """A decal that runs only on the named faces ("front", "sides", "top", ...)."""
    wanted = (names,) if isinstance(names, str) else tuple(names)

    def run(face, rng):
        if face.name in wanted or ("sides" in wanted and face.side()):
            fn(face, rng)
    return run


def clear_all(face, rng=None):
    for y in range(face.h):
        for x in range(face.w):
            face.clear(x, y)


def clear_faces(*names):
    """Clears the named faces (the underside of a hood, the top of a skirt that another box covers)."""
    return on(names, clear_all)


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


def tatters(depth=2, chance=0.6):
    """A ragged hem on every side face."""
    return on("sides", lambda f, r: ragged_edge(f, r, "bottom", depth, chance))


def holes(face, rng, count, size=1, rim=None):
    """Burn holes: transparent pixels with a scorched rim (the cutout shows what is under the cloth)."""
    for _ in range(count):
        x, y = rng.randrange(face.w), rng.randrange(face.h)
        for dx in range(size):
            for dy in range(size):
                face.clear(x + dx, y + dy)
        for dx, dy in ((-1, 0), (size, 0), (0, -1), (0, size)):
            c = face.get(x + dx, y + dy)
            if c[3]:
                face.set(x + dx, y + dy, rim or shade(c, -30))


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


def band(face, y, color, h=1):
    if face.side():
        face.rect(0, y, face.w, h, color)


def open_front(face, inset=1, top_rows=1):
    """A hood's front: cleared except a rim, so the face inside shows."""
    for y in range(top_rows, face.h):
        for x in range(inset, face.w - inset):
            face.clear(x, y)


def round_corners(face, rng=None):
    """Clears the corner pixels of a face (and the two below the top corners of a side face): a rounder
    silhouette for a cube of flame or mist."""
    for x, y in ((0, 0), (face.w - 1, 0), (0, face.h - 1), (face.w - 1, face.h - 1)):
        face.clear(x, y)
    if face.side():
        face.clear(0, 1)
        face.clear(face.w - 1, 1)


def mail(face, rng, dark, light, specular=None):
    """Chainmail: rows of links, every other row offset, a highlight on some; a specular row near the top."""
    for y in range(face.h):
        for x in range(face.w):
            if (x + (y // 2)) % 2 == 0 and y % 2 == 0:
                face.set(x, y, dark)
            elif rng.random() < 0.15:
                face.set(x, y, light)
    if specular is not None and face.side() and face.h > 3:
        for x in range(0, face.w, 2):
            face.set(x, 1, specular)


def lower(face, rows, colors, rng, base=None):
    """Repaints the bottom rows of a side face from another ramp (hands, boots)."""
    if not face.side():
        return
    for y in range(max(0, face.h - rows), face.h):
        for x in range(face.w):
            lv = (len(colors) - 1) / 2 if base is None else base
            lv += (rng.random() - 0.5) * 1.2 - (y - (face.h - rows)) * 0.3
            face.set(x, y, colors[clamp(round(lv), 0, len(colors) - 1)])


def plate_face(face, rng, colors, rivet_step=3, specular=True, dent=0.3):
    """A metal plate (art direction §1.8): a bevelled edge, rivets around it with a lit pixel, a specular line on the
    lit top-left, a dent or two."""
    if face.w < 3 or face.h < 3:
        return
    for x in range(face.w):
        face.tint(x, 0, 22)
        face.tint(x, face.h - 1, -26)
    for y in range(face.h):
        face.tint(0, y, 18)
        face.tint(face.w - 1, y, -22)
    if specular and face.name != "bottom":
        for x in range(1, max(2, face.w // 2)):
            face.set(x, 1, colors[-1])
    if face.w >= 5 and face.h >= 5:
        rivet_ring(face, colors[1], colors[-2], inset=1, step=rivet_step)
    for _ in range(int(dent * max(1, face.w * face.h / 12))):
        x, y = rng.randrange(1, face.w - 1), rng.randrange(1, face.h - 1)
        face.tint(x, y, -20)
        face.tint(x + 1, y + 1, 14)


def rust_spots(face, rng, count=None):
    n = count if count is not None else max(1, face.w * face.h // 28)
    for _ in range(n):
        x, y = rng.randrange(face.w), rng.randrange(face.h)
        if face.get(x, y)[3]:
            face.set(x, y, RUST[1 + rng.randrange(3)])
            if rng.random() < 0.4 and face.get(x, y + 1)[3]:
                face.set(x, y + 1, RUST[0])


def cloth(step=3, d=-16, hem=None, direction="v"):
    """Cloth with fold lines that follow the body (art direction §1.8) and an optional darker hem row."""
    def fn(face, rng):
        if not face.side():
            return
        fold_lines(face, rng, step, d, direction)
        if hem is not None:
            face.hline(0, face.h - 1, face.w, hem)
    return fn


def skin_cracks(face, rng, dark, count=None, length=(2, 4)):
    """Split dry skin: short dark cracks wandering down, a lighter flake beside some."""
    n = count if count is not None else max(1, face.w * face.h // 24)
    for _ in range(n):
        x, y = rng.randrange(face.w), rng.randrange(face.h)
        for _ in range(rng.randint(*length)):
            if not face.inside(x, y) or not face.get(x, y)[3]:
                break
            face.set(x, y, dark)
            if rng.random() < 0.3 and face.inside(x + 1, y) and face.get(x + 1, y)[3]:
                face.tint(x + 1, y, 18)
            y += 1
            x += rng.choice((-1, 0, 0, 1))


def ember_seams(face, rng, count, hot=EMBER, warm=EMBER_DEEP, strength=0.9, length=(2, 4), dark=None):
    """Glowing seams in the skin: a dark rim, a hot core, a warm pixel beside it here and there."""
    for _ in range(count):
        x, y = rng.randrange(face.w), rng.randrange(face.h)
        for _ in range(rng.randint(*length)):
            if not face.inside(x, y):
                break
            if dark is not None:
                for dx in (-1, 1):
                    if face.inside(x + dx, y) and face.get(x + dx, y)[3] and not face.glow.get(face.x0 + x + dx, face.y0 + y)[3]:
                        face.set(x + dx, y, dark)
            face.lit(x, y, hot, strength)
            if rng.random() < 0.35 and face.inside(x + 1, y):
                face.lit(x + 1, y, warm, strength * 0.6)
            y += 1
            x += rng.choice((-1, 0, 0, 1))


def lit_eyes(face, y, color, xs, strength=1.0, socket=None, w=1, socket_w=None):
    """Glowing eyes with dark sockets around them (the non-glowing ring stays dark, so the glow pops)."""
    for x in xs:
        if socket is not None:
            sw = socket_w or (w + 2)
            face.rect(x - (sw - w) // 2, y - 1, sw, 3, socket)
        for k in range(w):
            face.lit(x + k, y, color, strength)


def fade(alpha, bottom=None, glow=0, glow_bottom=None):
    """Translucency for entities drawn with RenderLayer.getEntityTranslucent: sets the alpha of every painted pixel
    (from alpha at the top of a side face to bottom at its foot), and with glow > 0 gives every pixel that does not
    glow yet a faint glow of its own colour at that alpha (so it still reads at night). Run it last."""
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


# --- Forms ----------------------------------------------------------------------------------------------------------

def hanging(name, parent, pivot, segments, length, width, mat, decal=None, z_step=0.2, taper=0.0, rotation=None,
            thickness=0):
    """A hanging strip of cloth as a chain of bones (a tatter, a sleeve's fall, a veil's fall, a coat tail): segment
    i is a plane width - i * taper wide and length tall, hanging from where the one above ends; each later segment
    sits z_step further out so they never coincide. Pivot at the top centre of the strip, the planes facing -z."""
    from art_kit import chain_bones

    def box(i, origin, size):
        w = width - i * taper
        return [Box([pivot[0] - w / 2, origin[1], pivot[2] + i * z_step - thickness / 2], [w, length, thickness], mat, decal)]
    return chain_bones(name, parent, pivot, segments, length, mat, None, down=True, rotation=rotation, box=box)


# --- Animation helpers ---------------------------------------------------------------------------------------------

def breathe(length, chest=2.0, head=1.5, shoulders=1.0, body="body", head_bone="head", arms=("right_arm", "left_arm"),
            arm_axis=2, phase=0.0):
    """An idle that breathes: the chest lifts and tilts back a little, the head bobs a hair later, the shoulders spread
    out of phase (art direction §2)."""
    out = {}
    if body:
        out[body] = {"rotation": sway(length, chest, 0, phase), "position": keys(*_pairs(length, lambda t: [0, 0.35 * math.sin(2 * math.pi * (t / length + phase)) + 0.35, 0]))}
    if head_bone:
        out[head_bone] = {"rotation": sway(length, head, 0, phase - 0.08)}
    for i, arm in enumerate(arms):
        sign = -1 if i == 0 else 1
        out[arm] = {"rotation": keys(*_pairs(length, lambda t, s=sign: [1.5 * math.sin(2 * math.pi * (t / length + phase - 0.12)), 0,
                                                                        s * shoulders * (0.5 - 0.5 * math.cos(2 * math.pi * (t / length + phase - 0.12)))]))}
    return out


def _pairs(length, fn, steps=4):
    out = []
    for i in range(steps + 1):
        t = length * i / steps
        out += [t, fn(t)]
    return out


def walk(length, legs=("right_leg", "left_leg"), arms=("right_arm", "left_arm"), stride=30, swing=25, bob=0.6,
         body="body", head="head", hips=None, lean=4, arm_rest=None, twist=5, roll=2, ease="easeInOutSine"):
    """A walk with weight: legs scissor, arms counter-swing, the body bobs twice a cycle (down at each foot plant),
    torso and hips counter-rotate about Y, the body leans forward lean degrees and rolls a little, the head stays
    level (counter to the body's pitch) and counters the yaw."""
    half = length / 2
    out = {}
    if legs:
        out[legs[0]] = {"rotation": keys(0, [stride, 0, 0], half, e(ease, [-stride, 0, 0]), length, e(ease, [stride, 0, 0]))}
        out[legs[1]] = {"rotation": keys(0, [-stride, 0, 0], half, e(ease, [stride, 0, 0]), length, e(ease, [-stride, 0, 0]))}
    rest = arm_rest or [ZERO, ZERO]
    if arms:
        for i, arm in enumerate(arms):
            r = rest[i]
            s = -1 if i == 0 else 1
            out[arm] = {"rotation": keys(0, [r[0] + s * swing, r[1], r[2]], half, e(ease, [r[0] - s * swing, r[1], r[2]]),
                                         length, e(ease, [r[0] + s * swing, r[1], r[2]]))}
    q = length / 4
    out["root"] = {"position": keys(0, [0, 0, 0], q, e("easeOutQuad", [0, bob, 0]), half, e("easeInQuad", [0, 0, 0]),
                                    3 * q, e("easeOutQuad", [0, bob, 0]), length, e("easeInQuad", [0, 0, 0]))}
    if body:
        out[body] = {"rotation": keys(0, [lean, twist, roll], half, e(ease, [lean, -twist, -roll]), length, e(ease, [lean, twist, roll]))}
    if hips:
        out[hips] = {"rotation": keys(0, [0, -twist, 0], half, e(ease, [0, twist, 0]), length, e(ease, [0, -twist, 0]))}
    if head:
        out[head] = {"rotation": keys(0, [-lean, -twist * 0.6, 0], half, e(ease, [-lean, twist * 0.6, 0]), length, e(ease, [-lean, -twist * 0.6, 0]))}
    return out


def flinch(length=0.45, body="body", head="head", arms=("right_arm", "left_arm"), amount=1.0, side=1, root=True,
           extra=None):
    """misc.hurt: a short recoil the hit knocks into the mob — the body jerks back and to one side, the head snaps
    with it, the arms come up a touch, the root hops back a pixel — and eases back to rest. Never long enough to
    hide a windup (the move controllers win on the bones they animate)."""
    a = amount
    out = {}
    if body:
        out[body] = {"rotation": keys(0, ZERO, 0.08, e("easeOutCubic", [-12 * a, 6 * a * side, 5 * a * side]), 0.2, [-6 * a, 3 * a * side, 2 * a * side],
                                      length, e("easeInOutSine", ZERO))}
    if head:
        out[head] = {"rotation": keys(0, ZERO, 0.06, e("easeOutCubic", [-18 * a, -10 * a * side, 0]), 0.22, [-4 * a, -2 * a * side, 0],
                                      length, e("easeInOutSine", ZERO))}
    for i, arm in enumerate(arms or ()):
        s = -1 if i == 0 else 1
        out[arm] = {"rotation": keys(0, ZERO, 0.08, e("easeOutCubic", [-25 * a, 0, s * 12 * a]), length, e("easeInOutSine", ZERO))}
    if root:
        out["root"] = {"position": keys(0, ZERO, 0.08, e("easeOutCubic", [0, 0.4 * a, 1.2 * a]), length, e("easeInOutSine", ZERO))}
    if extra:
        out.update(extra)
    return out


def settle(names, t0, amp, axis=0, lag=0.06, gain=1.1, rest=None, length=0.7):
    """Hanging parts settling after a fall: flung amp on axis, swinging back smaller twice, resting by t0 + length."""
    rest = rest or ZERO
    out = {}
    for i, n in enumerate(names):
        d = lag * i
        a = amp * gain ** i

        def v(k):
            r = list(rest)
            r[axis] += k
            return r

        out[n] = {"rotation": keys(t0 + d, rest, t0 + d + 0.14, e("easeOutCubic", v(a)), t0 + d + 0.36, e("easeInOutSine", v(-a * 0.4)),
                                   t0 + d + 0.55, e("easeInOutSine", v(a * 0.15)), t0 + d + length, e("easeOutSine", rest))}
    return out


def hold_from(bones_rot, t_from, t_to):
    """Holds the given rotations from t_from to t_to (the last frame of a death, kept while the body lies)."""
    out = {}
    for n, r in bones_rot.items():
        out[n] = {"rotation": keys(t_from, r, t_to, r)}
    return out


def drift(names, length, amp, axis=0, lag=0.1, gain=0.9, phase=0.0, base=None):
    """lag_sway with a name that says what it is for: hanging parts drifting in idle and walk, out of phase."""
    return lag_sway(names, length, amp, axis, lag, gain, phase, 4, base)


if __name__ == "__main__":
    problems = check_ramps()
    for p in problems:
        print("PROBLEM", p)
    print("%d ramps, %s" % (len(ALL_RAMPS), "all pass" if not problems else "%d problems" % len(problems)))
    sys.exit(1 if problems else 0)
