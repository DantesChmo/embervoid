"""Helpers for the 0.5.0 mob remodels (PLAN 19.3, docs/art-direction.md §1.8 and §2), on top of tools/art_kit.py
and tools/palette.py.

Not run directly: tools/reach_mob_art.py uses it. The dense-form helpers (chamfered, octagon, plate, chain_bones,
lag_sway, whip, hold, fingers, fold_lines, grain, sutures, rivet_ring), the eased keyframes (e, keys) and
Material(bevel=) live in art_kit.py since the bosses track; the named ramps of the whole mod in palette.py. This
module adds what the 14 Realm 1 mobs need beyond them: the mob-only ramps (flesh, skins, hair, feathers, moth
fur, leather, rope, bark, cast iron, gambeson), a few decals (edge shadow, plate rows, scales, specular), chains
settling after a fall, claws, and the shared humanoid animation builders (breathing idle, weighted walk, hurt
flinch, stagger, death fall, strike curves).

Keyframes: keys(0, ZERO, 0.4, e("easeOutBack", [-90, 0, 0])) writes a GeckoLib keyframe with its easing; the
easing shapes the segment that ends at that key, and tools/model_preview.py draws it the same way.
"""
import colorsys
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from art_kit import (ZERO, Bone, Box, anim, chain_bones, chain_names, chamfered, e, fingers, fold_lines, grain,  # noqa: E402,F401
                     hexc, hold, keys, lag_sway, merge, octagon, plate, pos, rivet_ring, rot, sutures, sway, whip)
from palette import (AMBER as AMBER_RAMP, ASH_CLOTH, BONE, BRASS, BURLAP, CHARRED_WOOD, COLD_FLAME, CRIMSON_CLOTH,  # noqa: E402,F401
                     DEAD_WOOD, EMBER as EMBER_RAMP, EMBER_GLASS, GOLD_TRIM, GRAVE_BLUE, GRAVE_IRON, MOSS, RUST,
                     SLATE, SOOT, STEEL, STRAW, TALLOW)

# --- Hue-shifted ramps (art direction §1.1) --------------------------------------------------------------------------
# One ramp of 6-7 colours per material: shadows cooler and more saturated, highlights warmer and less saturated.

def shifted_ramp(mid, n=6, dark=0.42, light=1.45, cool_hue=0.68, warm_hue=0.12, hue_pull=0.10, sat_pull=0.35):
    """A ramp of n colours around the mid colour (hex): each darker step pulls its hue hue_pull of the way toward
    cool_hue (0..1 on the colour wheel: 0.68 blue-violet for stone and iron, 0.02 red-brown for wood and cloth) and
    gains saturation; each lighter step pulls toward warm_hue (yellow) and loses saturation. dark / light are the
    value factors of the ends."""
    r, g, b, _ = hexc(mid)
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    out = []
    mid_i = (n - 1) / 2
    for i in range(n):
        t = (i - mid_i) / mid_i  # -1 dark .. +1 light
        if t < 0:
            k = -t
            hh = _lerp_hue(h, cool_hue, hue_pull * k)
            ss = min(1.0, s * (1 + sat_pull * k) + 0.05 * k)
            vv = v * (1 + (dark - 1) * k)
        else:
            k = t
            hh = _lerp_hue(h, warm_hue, hue_pull * k)
            ss = max(0.0, s * (1 - sat_pull * k))
            vv = min(1.0, v * (1 + (light - 1) * k))
        rr, gg, bb = colorsys.hsv_to_rgb(hh % 1.0, ss, max(0.0, min(1.0, vv)))
        out.append((int(round(rr * 255)), int(round(gg * 255)), int(round(bb * 255)), 255))
    return out


def _lerp_hue(a, b, t):
    d = (b - a + 0.5) % 1.0 - 0.5
    return (a + d * t) % 1.0


# Mob-only ramps (dark to light; the rest come from palette.py). Cool shadows toward blue-violet for iron, ash and
# shrouds; toward red-brown for wood, leather, cloth and flesh.
CHAR = shifted_ramp("3a2c24", 7, dark=0.35, light=1.7, cool_hue=0.02, hue_pull=0.08)
ASH = shifted_ramp("6f6862", 6, dark=0.45, light=1.4, cool_hue=0.68)
LINEN = shifted_ramp("72654f", 6, dark=0.45, light=1.4, cool_hue=0.04)
WOOD = shifted_ramp("5c4b3a", 6, dark=0.45, light=1.45, cool_hue=0.02)
IRON = shifted_ramp("595c63", 7, dark=0.4, light=1.6, cool_hue=0.66)
LEATHER = shifted_ramp("4f3726", 6, dark=0.45, light=1.5, cool_hue=0.0)
ROPE = shifted_ramp("7c6340", 6, dark=0.5, light=1.4, cool_hue=0.04)
BARK = shifted_ramp("3f3024", 7, dark=0.4, light=1.65, cool_hue=0.02, hue_pull=0.06)
SHROUD = shifted_ramp("3a3440", 6, dark=0.45, light=1.5, cool_hue=0.72)
FEATHER = shifted_ramp("2d282f", 6, dark=0.4, light=1.8, cool_hue=0.74)
DUST = shifted_ramp("756855", 6, dark=0.45, light=1.4, cool_hue=0.05)
SKIN_DEAD = shifted_ramp("6a6951", 6, dark=0.5, light=1.4, cool_hue=0.4, warm_hue=0.15)
CLOTH_GREEN = shifted_ramp("3b4330", 6, dark=0.5, light=1.5, cool_hue=0.4)
GAMBESON = shifted_ramp("562d24", 6, dark=0.45, light=1.5, cool_hue=0.98)
CAST = shifted_ramp("2d2c30", 7, dark=0.4, light=1.9, cool_hue=0.7)
BLACK_STEEL = shifted_ramp("27272c", 7, dark=0.4, light=2.0, cool_hue=0.68)
SCORCHED = shifted_ramp("5a3630", 5, dark=0.5, light=1.4, cool_hue=0.98)
ROBE_MOSS = shifted_ramp("394031", 6, dark=0.5, light=1.45, cool_hue=0.42)
SKIN_OLD = shifted_ramp("7a7462", 6, dark=0.5, light=1.35, cool_hue=0.08, warm_hue=0.12)
HAIR = shifted_ramp("7a766f", 5, dark=0.6, light=1.35, cool_hue=0.66)

EMBER_HOT = hexc("ffd27a")
EMBER = hexc("ff8a2a")
EMBER_DEEP = hexc("d8521c")
EMBER_DARK = hexc("7a2a12")
COLD = hexc("9fd4ff")
COLD_DIM = hexc("4f7ea8")
AMBER = hexc("ffb347")
WARM = hexc("ffdc8c")
PALE_GLOW = hexc("d8ffb0")
VOID = hexc("0c0a0a")


def darker(c, f=0.6):
    return tuple(int(v * f) for v in c[:3]) + (255,)


# --- Keyframe helpers beyond art_kit ------------------------------------------------------------------------------

def add(frames, base):
    """Adds a rest rotation to every key of a keyframe dict (plain vectors only)."""
    return {k: [v[j] + base[j] for j in range(3)] for k, v in frames.items()}


def settle(names, t0, amp, axis=0, lag=0.06, gain=1.1, length=1.0, rest=None):
    """Hanging parts coming to rest after a fall: a swing on axis that dies out over length seconds from t0."""
    rest = rest or ZERO
    out = {}
    for i, n in enumerate(names):
        d = lag * i
        a = amp * gain ** i
        pairs = [t0 + d, list(rest)]
        for j, k in enumerate((1.0, -0.6, 0.3, -0.12, 0.0)):
            r = list(rest)
            r[axis] += a * k
            pairs += [t0 + d + length * (j + 1) / 5, e("easeInOutSine", r)]
        out[n] = {"rotation": keys(*pairs)}
    return out


# --- Forms beyond art_kit ----------------------------------------------------------------------------------------

def claws(x, y, z, count, size, mat, spread=0.5, curl=-30, splay=14):
    """Claws hanging from a paw's underside at y, each curled forward and splayed outward."""
    w, l, d = size
    boxes = []
    mid = (count - 1) / 2
    for i in range(count):
        fx = x + i * (w + spread)
        boxes.append(Box([fx, y - l, z], [w, l, d], mat, rotation=[curl, 0, (i - mid) * splay],
                         pivot=[fx + w / 2, y, z + d / 2]))
    return boxes


# --- Face decals (art direction §1.8) -------------------------------------------------------------------------------

def edge_shadow(face, rng=None, d=-14):
    """The cube's outline: a 1 px darkening of the whole rim of a side face (separates stacked boxes)."""
    if not face.side() or face.w < 3 or face.h < 3:
        return
    for x in range(face.w):
        face.tint(x, 0, d // 2)
        face.tint(x, face.h - 1, d)
    for y in range(face.h):
        face.tint(0, y, d // 2)
        face.tint(face.w - 1, y, d)


def plate_rows(face, rng, dark, light, step=4, rivet=None):
    """Riveted iron plates: a dark seam every step rows with a lit line under it, rivets along the seams."""
    if not face.side() and face.name != "top":
        return
    for y in range(step - 1, face.h, step):
        face.hline(0, y, face.w, dark)
        if y + 1 < face.h:
            face.hline(0, y + 1, face.w, light)
        if rivet is not None:
            for x in range(1, face.w, 3):
                face.set(x, max(0, y - 1), rivet)


def scales(face, rng, dark, light, step=2):
    """Overlapping scales or feather rows: a lighter tip every few pixels, a dark seam under it, staggered."""
    for y in range(0, face.h, step):
        off = (y // step) % 2
        for x in range(off, face.w, 3):
            face.set(x, y, light)
            if face.inside(x, y + 1):
                face.set(x, y + 1, dark)


def specular(face, rng=None, color=None, x=1, y=1):
    """Metal: a 1 px specular dot on the lit (top-left) side of a face."""
    if face.side() and face.w > 2 and face.h > 2:
        face.set(x, y, color if color is not None else (235, 232, 225, 255))


def lit_pixels(face, points, color, strength=1.0):
    for x, y in points:
        face.lit(x, y, color, strength)


def cutout(face, rng=None, cols=(), rows=()):
    """Clears columns / rows of a face (grilles, ribs, spread fingers)."""
    for x in cols:
        for y in range(face.h):
            face.clear(x, y)
    for y in rows:
        for x in range(face.w):
            face.clear(x, y)


# --- Humanoid animation builders --------------------------------------------------------------------------------------

def breathe(length=3.0, chest=2.0, shoulders=1.5, head=3.0, extra=None, body="body", arms=("right_arm", "left_arm")):
    """Idle that breathes: the chest lifts 1-2 px and pitches a little, the shoulders rise, the head drifts."""
    h = length / 2
    bones = {
        body: {"rotation": keys(0, ZERO, h, e("easeInOutSine", [chest * 0.6, 0, 0]), length, e("easeInOutSine", ZERO)),
               "position": keys(0, ZERO, h, e("easeInOutSine", [0, chest * 0.35, 0]), length, e("easeInOutSine", ZERO))},
        "head": {"rotation": keys(0, ZERO, h * 0.8, e("easeInOutSine", [head * 0.5, head, 0]), length,
                                  e("easeInOutSine", ZERO))},
    }
    for i, a in enumerate(arms):
        s = 1 if i == 0 else -1
        bones[a] = {"rotation": keys(0, ZERO, h, e("easeInOutSine", [-shoulders, 0, s * shoulders]), length,
                                     e("easeInOutSine", ZERO))}
    return anim(length, merge(bones, extra or {}))


def weighted_walk(length=1.0, leg=30, arm=20, bob=0.6, hips=4, torso=5, head_level=True, extra=None,
                  legs=("right_leg", "left_leg"), arms=("right_arm", "left_arm")):
    """Walk with weight: legs and arms swing, the body bobs twice a cycle, the hips (root yaw) and the torso (body
    yaw) counter-rotate, the body pitches into the step, the head stays level."""
    h, q = length / 2, length / 4
    bones = {
        legs[0]: rot(0, [leg, 0, 0], h, e("easeInOutSine", [-leg, 0, 0]), length, e("easeInOutSine", [leg, 0, 0])),
        legs[1]: rot(0, [-leg, 0, 0], h, e("easeInOutSine", [leg, 0, 0]), length, e("easeInOutSine", [-leg, 0, 0])),
        arms[0]: rot(0, [-arm, 0, 0], h, e("easeInOutSine", [arm, 0, 0]), length, e("easeInOutSine", [-arm, 0, 0])),
        arms[1]: rot(0, [arm, 0, 0], h, e("easeInOutSine", [-arm, 0, 0]), length, e("easeInOutSine", [arm, 0, 0])),
        "root": {"position": keys(0, ZERO, q, e("easeOutSine", [0, bob, 0]), h, e("easeInSine", ZERO),
                                  h + q, e("easeOutSine", [0, bob, 0]), length, e("easeInSine", ZERO)),
                 "rotation": keys(0, [0, hips, 0], h, e("easeInOutSine", [0, -hips, 0]), length,
                                  e("easeInOutSine", [0, hips, 0]))},
        "body": {"rotation": keys(0, [3, -torso, 0], q, e("easeInOutSine", [5, 0, 1.5]), h, e("easeInOutSine", [3, torso, 0]),
                                  h + q, e("easeInOutSine", [5, 0, -1.5]), length, e("easeInOutSine", [3, -torso, 0]))},
    }
    if head_level:
        bones["head"] = rot(0, [-3, torso, 0], h, e("easeInOutSine", [-3, -torso, 0]), length,
                            e("easeInOutSine", [-3, torso, 0]))
    return anim(length, merge(bones, extra or {}))


def hurt_flinch(length=0.45, lean=-9, twist=6, head=-14, chains=(), chain_amp=18, chain_axis=0, extra=None):
    """A flinch: the body jerks back and twists, the head snaps back, the arms come up a little, hanging parts
    swing; back to rest by the end. Never touches the legs, so a walk underneath reads on."""
    bones = {
        "body": {"rotation": keys(0, ZERO, 0.08, e("easeOutCubic", [lean, twist, -twist * 0.5]), length,
                                  e("easeOutSine", ZERO)),
                 "position": keys(0, ZERO, 0.08, e("easeOutCubic", [0, -0.5, 1.0]), length, e("easeOutSine", ZERO))},
        "head": rot(0, ZERO, 0.08, e("easeOutCubic", [head, -twist, 0]), length, e("easeOutSine", ZERO)),
        "right_arm": rot(0, ZERO, 0.1, e("easeOutCubic", [-25, 0, 12]), length, e("easeOutSine", ZERO)),
        "left_arm": rot(0, ZERO, 0.1, e("easeOutCubic", [-20, 0, -12]), length, e("easeOutSine", ZERO)),
    }
    if chains:
        bones = merge(bones, whip(chains, 0.04, chain_amp, chain_axis, lag=0.04, settle=length - 0.05))
    return anim(length, merge(bones, extra or {}), loop=False)


def stagger(length=0.9, lean=-24, head=-28, knee=35, chains=(), chain_amp=35, chain_axis=0, extra=None):
    """A heavier reel: thrown back on a bent knee, the arms flung, a step back, then a slow recovery with a little
    overshoot (easeOutBack)."""
    bones = {
        "body": {"rotation": keys(0, ZERO, 0.12, e("easeOutCubic", [lean, 8, 4]), 0.45, e("easeInOutSine", [lean * 0.6, 4, 2]),
                                  length, e("easeOutBack", ZERO)),
                 "position": keys(0, ZERO, 0.12, e("easeOutCubic", [0, -1.5, 2.0]), length, e("easeOutSine", ZERO))},
        "head": rot(0, ZERO, 0.1, e("easeOutCubic", [head, -10, 6]), 0.5, e("easeInOutSine", [head * 0.4, 0, 0]), length,
                    e("easeOutBack", ZERO)),
        "right_arm": rot(0, ZERO, 0.12, e("easeOutCubic", [-70, 0, 30]), length, e("easeOutSine", ZERO)),
        "left_arm": rot(0, ZERO, 0.12, e("easeOutCubic", [-55, 0, -35]), length, e("easeOutSine", ZERO)),
        "right_leg": rot(0, ZERO, 0.12, e("easeOutCubic", [-knee, 0, 0]), length, e("easeOutSine", ZERO)),
        "left_leg": rot(0, ZERO, 0.12, e("easeOutCubic", [knee * 0.6, 0, 0]), length, e("easeOutSine", ZERO)),
        "root": pos(0, ZERO, 0.12, e("easeOutCubic", [0, -2, 2]), 0.5, e("easeInOutSine", [0, -1.5, 3]), length,
                    e("easeOutSine", ZERO)),
    }
    if chains:
        bones = merge(bones, whip(chains, 0.05, chain_amp, chain_axis, lag=0.05, settle=length - 0.1))
    return anim(length, merge(bones, extra or {}), loop=False)


def death_fall(length=2.0, legs=12, forward=True, hips=None, chains=(), chain_amp=30, chain_axis=0, extra=None,
               knees=0.35, down=0.9, head=None, arms_out=True):
    """A humanoid's fall: the knees buckle (knees s), the body drops and tips over the hips to lie flat
    (forward: face down along -z; else on its back), legs folding under, arms flung out, the head lolling, then
    lies still with the hanging parts settling. legs: hip height in px (how far the root drops). hips: the body
    bone's pivot height if it differs from legs."""
    hips = legs if hips is None else hips
    s = 1 if forward else -1
    lie = [s * 88, 0, s * 6]
    bones = {
        "root": {"position": keys(0, ZERO, knees, e("easeInSine", [0, -3, 0]), down, e("easeInQuad", [0, -(hips - 1.5), s * 1.5]),
                                  down + 0.12, e("easeOutSine", [0, -(hips - 2.5), s * 1.5]), down + 0.3,
                                  e("easeOutSine", [0, -(hips - 1.5), s * 1.5]), length, [0, -(hips - 1.5), s * 1.5])},
        "body": {"rotation": keys(0, ZERO, knees, e("easeOutSine", [s * 12, 0, 0]), down, e("easeInQuad", lie),
                                  down + 0.12, e("easeOutSine", [s * 78, 0, s * 6]), down + 0.3, e("easeOutSine", lie), length, lie)},
        "head": rot(0, ZERO, knees, e("easeOutSine", [s * -15, 0, 0]), down, e("easeInQuad", [s * -30, 0, 0]), down + 0.25,
                    e("easeOutBounce", [s * -10, 25, 10] if head is None else head), length,
                    [s * -10, 25, 10] if head is None else head),
        "right_leg": rot(0, ZERO, knees, e("easeOutSine", [40, 0, 0]), down, e("easeInQuad", [s * -70, 0, -8]), down + 0.2,
                         e("easeOutSine", [s * -80, 0, -10]), length, [s * -80, 0, -10]),
        "left_leg": rot(0, ZERO, knees, e("easeOutSine", [20, 0, 0]), down, e("easeInQuad", [s * -55, 0, 12]), down + 0.2,
                        e("easeOutSine", [s * -75, 0, 14]), length, [s * -75, 0, 14]),
    }
    if arms_out:
        bones["right_arm"] = rot(0, ZERO, knees, e("easeOutSine", [-30, 0, 20]), down, e("easeInQuad", [-110, 0, 40]),
                                 down + 0.2, e("easeOutSine", [-95, 0, 55]), length, [-95, 0, 55])
        bones["left_arm"] = rot(0, ZERO, knees, e("easeOutSine", [-20, 0, -15]), down, e("easeInQuad", [-60, 0, -30]),
                                down + 0.2, e("easeOutSine", [-45, 0, -25]), length, [-45, 0, -25])
    if chains:
        bones = merge(bones, settle(chains, down, chain_amp, chain_axis, length=min(1.0, length - down - 0.05)))
    return anim(length, merge(bones, extra or {}), loop=False)


def strike_recovery(bone, t_hit, start, hit, length, rest=None, hit_ease="easeOutBack", recover="easeOutSine",
                    overshoot=None):
    """One bone's attack curve: held at start until just before t_hit, snapped to hit at t_hit (easeOutBack by
    default: the strike is the extreme pose), then a little past it (overshoot) and back to rest by length."""
    rest = rest or ZERO
    pairs = [max(0.0, t_hit - 0.1), start, t_hit, e(hit_ease, hit)]
    if overshoot is not None:
        pairs += [min(length - 0.05, t_hit + 0.12), e("easeOutSine", overshoot)]
    pairs += [length, e(recover, rest)]
    return {"rotation": keys(*pairs)}


def anticipate(t0, t1, start, held, ease="easeInOutSine"):
    """Keys from the rest at t0 into the held windup pose at t1 (anticipation)."""
    return [t0, start, t1, e(ease, held)]


# --- Dense humanoid ------------------------------------------------------------------------------------------------

def chamfer_decals(parts, decals, axes="xyz"):
    """Hands a chamfered form's decals to the parts whose faces show: front / back to the part with the full depth,
    top / bottom to the full-height part, left / right to the full-width part (the other parts' faces of that
    name lie a chamfer deeper, hidden). "all" goes to every outermost face, "sides" to the four side faces."""
    if not decals:
        return parts
    lo = [min(p.origin[i] for p in parts) for i in range(3)]
    hi = [max(p.origin[i] + p.size[i] for p in parts) for i in range(3)]
    eps = 1e-6
    for part in parts:
        flush = {
            "right": abs(part.origin[0] - lo[0]) < eps, "left": abs(part.origin[0] + part.size[0] - hi[0]) < eps,
            "bottom": abs(part.origin[1] - lo[1]) < eps, "top": abs(part.origin[1] + part.size[1] - hi[1]) < eps,
            "front": abs(part.origin[2] - lo[2]) < eps, "back": abs(part.origin[2] + part.size[2] - hi[2]) < eps,
        }
        part.decals = {}
        for key, fn in decals.items():
            if key == "all":
                faces = [f for f, ok in flush.items() if ok]
            elif key == "sides":
                faces = [f for f, ok in flush.items() if ok and f in ("front", "back", "left", "right")]
            else:
                faces = [key] if flush.get(key) else []
            for f in faces:
                part.decals[f] = fn
    return parts


def dense_humanoid(legs=12, body=(8, 12, 4), head=(8, 8, 8), arm=(4, 12, 4), leg=(4, 4), hunch=0, mats=None,
                   decals=None, arm_gap=0.0, chamfer=1.0, elbows=True, knees=True, hands=True, feet=True):
    """A humanoid with the corners knocked off and limbs in two segments: root; legs on root (right_leg /
    left_leg at the hip, right_shin / left_shin at the knee); body (pivot at the hips, pitched by hunch)
    carrying the head and the arms (right_arm / left_arm at the shoulder, right_fore / left_fore at the elbow,
    the hand on the forearm). Left limbs mirror the right ones' textures. mats: body, head, arm, leg (+ fore, shin,
    hand, foot, optional). decals per part the same way. Returns the bone list and the shoulder height."""
    from art_kit import mirror_box
    mats = mats or {}
    decals = decals or {}
    bw, bh, bd = body
    hw, hh, hd = head
    aw, ah, ad = arm
    lw, ld = leg
    top = legs + bh
    ax = bw / 2 + aw / 2 + arm_gap

    def mat(part, fallback):
        return mats.get(part, mats.get(fallback))

    torso = chamfer_decals(chamfered([-bw / 2, legs, -bd / 2], [bw, bh, bd], mats["body"], c=chamfer, axes="xz"),
                           decals.get("body"), axes="xz")
    skull = chamfer_decals(chamfered([-hw / 2, top, -hd / 2], [hw, hh, hd], mats["head"], c=chamfer), decals.get("head"))
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, legs, 0], "root", [hunch, 0, 0] if hunch else None, torso),
        Bone("head", [0, top, 0], "body", None, skull),
    ]
    # Arms: upper arm from the shoulder down to the elbow, forearm and hand below it.
    upper_h = ah / 2 if elbows else ah
    elbow = top - 2 - upper_h
    r_upper = Box([-ax - aw / 2, elbow, -ad / 2], [aw, upper_h + 2, ad], mats["arm"], decals.get("arm"))
    bones.append(Bone("right_arm", [-ax, top - 2, 0], "body", None, [r_upper]))
    bones.append(Bone("left_arm", [ax, top - 2, 0], "body", None, [mirror_box(r_upper)]))
    if elbows:
        fore_h = ah - upper_h - (2 if hands else 0)
        r_fore = Box([-ax - aw / 2, elbow - fore_h, -ad / 2], [aw, fore_h, ad], mat("fore", "arm"), decals.get("fore"),
                     inflate=-0.25)
        fore_boxes = [r_fore]
        if hands:
            fore_boxes.append(Box([-ax - aw / 2, elbow - fore_h - 2, -ad / 2], [aw, 2, ad], mat("hand", "arm"),
                                  decals.get("hand"), inflate=0.1))
        bones.append(Bone("right_fore", [-ax, elbow, 0], "right_arm", None, fore_boxes))
        bones.append(Bone("left_fore", [ax, elbow, 0], "left_arm", None, [mirror_box(b) for b in fore_boxes]))
    # Legs: thigh to the knee, shin and foot below.
    thigh_h = legs / 2 if knees else legs
    knee = legs - thigh_h
    r_thigh = Box([-lw, knee, -ld / 2], [lw, thigh_h, ld], mats["leg"], decals.get("leg"))
    bones.append(Bone("right_leg", [-lw / 2, legs, 0], "root", None, [r_thigh]))
    bones.append(Bone("left_leg", [lw / 2, legs, 0], "root", None, [mirror_box(r_thigh)]))
    if knees:
        shin_boxes = [Box([-lw, 1.5 if feet else 0, -ld / 2], [lw, knee - (1.5 if feet else 0), ld], mat("shin", "leg"),
                          decals.get("shin"), inflate=-0.2)]
        if feet:
            shin_boxes.append(Box([-lw - 0.25, 0, -ld / 2 - 1], [lw + 0.5, 1.5, ld + 1], mat("foot", "leg"), decals.get("foot")))
        bones.append(Bone("right_shin", [-lw / 2, knee, 0], "right_leg", None, shin_boxes))
        bones.append(Bone("left_shin", [lw / 2, knee, 0], "left_leg", None, [mirror_box(b) for b in shin_boxes]))
    return bones, top


def tail_chain(name, parent, pivot, segments, length, mat, decals=None, thickness=1.0, rotation=None, box=None,
               taper=1.0):
    """A chain of bones running back along +z (a tail, a plume behind the head): like art_kit.chain_bones with
    down=False but behind the mob, each segment taper times thinner."""
    bones = []
    px, py, pz = pivot
    th = thickness
    for i in range(segments):
        bone_name = "%s_%d" % (name, i + 1)
        par = parent if i == 0 else "%s_%d" % (name, i)
        origin = [px - th / 2, py - th / 2, pz]
        size = [th, th, length]
        boxes = box(i, origin, size) if box else [Box(origin, size, mat, decals)]
        bones.append(Bone(bone_name, [px, py, pz], par, rotation if i == 0 else None, boxes))
        pz += length
        th *= taper
    return bones


def bone(bones, name):
    return next(b for b in bones if b.name == name)


def limb_walk(length, fore=8, shin=12, extra=None):
    """Elbow and knee bends on top of weighted_walk: the forearms stay a little bent and lag the swing, the shins
    fold under on the back-swing."""
    h, q = length / 2, length / 4
    return merge({
        "right_fore": rot(0, [-fore, 0, 0], q, e("easeInOutSine", [-fore * 2, 0, 0]), h, e("easeInOutSine", [-fore, 0, 0]),
                          h + q, e("easeInOutSine", [-fore * 0.5, 0, 0]), length, e("easeInOutSine", [-fore, 0, 0])),
        "left_fore": rot(0, [-fore, 0, 0], q, e("easeInOutSine", [-fore * 0.5, 0, 0]), h, e("easeInOutSine", [-fore, 0, 0]),
                         h + q, e("easeInOutSine", [-fore * 2, 0, 0]), length, e("easeInOutSine", [-fore, 0, 0])),
        "right_shin": rot(0, ZERO, q, e("easeInOutSine", [shin * 0.4, 0, 0]), h, e("easeInOutSine", [shin * 2, 0, 0]),
                          h + q, e("easeInOutSine", [shin, 0, 0]), length, e("easeInOutSine", ZERO)),
        "left_shin": rot(0, [shin * 2, 0, 0], q, e("easeInOutSine", [shin, 0, 0]), h, e("easeInOutSine", ZERO),
                         h + q, e("easeInOutSine", [shin * 0.4, 0, 0]), length, e("easeInOutSine", [shin * 2, 0, 0])),
    }, extra or {})
