#!/usr/bin/env python3
"""Models, animations and textures for the moving set pieces (PLAN 20.2): GeckoLib block models, 16 px per block,
20–80 cubes each (docs/art-direction.md §2), built on tools/art_kit.py.

Run from the repository root: python3 tools/setpiece_art.py [name ...]

Writes per piece
  assets/embervoid/geo/block/setpiece/<name>.geo.json           the model (block centre at the origin, facing north)
  assets/embervoid/animations/block/setpiece/<name>.animation.json
                                                                 idle (loop), state.<s> (loop), action.<a> (one-shot);
                                                                 sound keyframes carry a word the client maps to a
                                                                 vanilla sound (creak, toll, chain, slam, grind, clink)
  assets/embervoid/textures/block/setpiece/<name>.png (+ _glowmask)
  assets/embervoid/textures/item/setpiece/<name>.png             a 16 px icon (front projection of the model)
  assets/embervoid/models/block/setpiece/<name>.json             a particle-only block model (breaking particles)

Conventions: Bedrock model space, y up, -z is the front (north when the block faces north), 16 px = 1 block; the
block itself spans x -8..8, y 0..16, z -8..8. GeckoLib mirrors x into render space, so what the preview shows
(python3 tools/model_preview.py entity setpiece/<name>, motion setpiece/<name>) is what the game shows.
The kinds in com.embervoid.block.setpiece.SetPieces name the same animations; SetPieceTests checks the two agree.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from art_kit import (Bone, Box, Canvas, Face, Material, ROOT, anim, clamp, hexc, keys, merge, next_pow2, ramp,  # noqa: E402
                     write_json, write_model)

FOLDER = "block/setpiece"

# --- Ramps (art direction §1: shadows cooler and more saturated, highlights warmer and paler) ----------------------
# tools/palette.py (track V-A) holds the shared ramps once it is on main; until then these are this script's copies.
DEAD_WOOD = ramp("2a1a18", "3f2a22", "573f2f", "6f573d", "8a7250", "a89066", "c4ad82")
CHARRED = ramp("110c0e", "1e1414", "2d1f1a", "3f2d24", "54402f", "6a5540")
GRAVE_IRON = ramp("1a1824", "292733", "393945", "4c4d58", "63656d", "7e8086", "9c9ea0")
BRONZE = ramp("3a2010", "5a3818", "7c5222", "9c6e30", "bf8f44", "d9ad5e", "f0cf86")
ROPE = ramp("3a281a", "56402a", "715839", "8f7350", "a88c66", "c2a67e")
SAILCLOTH = ramp("38302c", "4d443c", "655a4d", "7e7262", "988b77", "ae9f8a", "c2b49d")
BONE = ramp("5e5244", "7d7060", "9c8f78", "b8ab90", "d2c7a9", "e8e0c4")
ASH_STONE = ramp("1e1d2a", "2c2b38", "3b3a47", "4e4d58", "63626a", "7b7a7f", "949390")
EMBER = ramp("5a1408", "9a2a0c", "d4531a", "f08a2e", "ffc257", "fff0b0")
RUST = ramp("3a1c12", "56301a", "744424", "915a30", "ab7440")
CRIMSON = ramp("2a0c0e", "41130f", "5b1c14", "762a1b", "8f3d26", "a85735")
GOLD_TRIM = ramp("5a3a10", "8a5e1c", "b8862e", "dcae48", "f3d06e")
SOOT = ramp("0c0a0c", "16131a", "211d24", "2c2730")

# Materials.
WOOD = Material(DEAD_WOOD, base=2.8, noise=0.55, grain="v", edge=0.6, grad=0.6, speck=0.03)
WOOD_H = WOOD.but(grain="h")
PLANK = Material(DEAD_WOOD, base=3.0, noise=0.5, grain="h", edge=0.8, speck=0.04)
CHAR = Material(CHARRED, base=2.6, noise=0.6, grain="v", edge=0.5)
IRON = Material(GRAVE_IRON, base=2.6, noise=0.5, cell=2.0, edge=0.9, speck=0.05, top=0.9, bottom=-0.6)
IRON_DARK = IRON.but(base=1.8)
BRASS = Material(BRONZE, base=3.2, noise=0.6, cell=3.0, edge=0.8, speck=0.06, top=1.0, bottom=-0.9)
CLOTH = Material(SAILCLOTH, base=3.2, noise=0.7, cell=2.5, edge=0.4, grad=0.9)
ROPE_M = Material(ROPE, base=2.4, noise=0.6, grain="h", edge=0.3)
STONE = Material(ASH_STONE, base=3.0, noise=0.7, cell=2.5, edge=0.8, speck=0.05)
STONE_DARK = STONE.but(base=2.0)
BONE_M = Material(BONE, base=3.0, noise=0.6, grain="v", edge=0.5, top=0.8)
COALS = Material(EMBER, base=2.2, noise=1.0, cell=1.5, glow=0.8)
FLAME = Material(EMBER, base=3.4, noise=1.0, cell=1.6, glow=1.0)
RED_CLOTH = Material(CRIMSON, base=2.8, noise=0.6, grain="v", edge=0.5, grad=1.0)
ASH_BED = Material(SOOT, base=1.6, noise=0.8, cell=1.5)


# --- Decals -----------------------------------------------------------------------------------------------------

def rivet_row(face, rng, step=3, y=None, color=None):
    """A row of rivets with a light dot on the top-left and a dark pixel under each."""
    c = color or GRAVE_IRON[5]
    yy = face.h // 2 if y is None else y
    for x in range(1, face.w - 1, step):
        face.set(x, yy, c)
        face.set(x, yy + 1, GRAVE_IRON[1])


def iron_band(face, rng, y, h=1):
    """A strap of iron across a wooden face."""
    for yy in range(y, y + h):
        for x in range(face.w):
            face.set(x, yy, GRAVE_IRON[2 + (x + yy) % 2])
    for x in range(1, face.w, 4):
        face.set(x, y, GRAVE_IRON[5])


def spec_line(face, rng):
    """A specular line down the lit (left) side of a metal face (§1.2)."""
    if face.side() and face.h > 2:
        for y in range(1, face.h - 1):
            face.tint(0, y, 22)
            face.tint(face.w - 1, y, -18)
    if face.name == "top":
        for x in range(face.w):
            face.tint(x, 0, 14)


def wood_shade(face, rng):
    """Light from the top-left: a light edge on top and left, a dark one bottom and right (§1.2)."""
    if face.w < 2 or face.h < 2:
        return
    for x in range(face.w):
        face.tint(x, 0, 16)
        face.tint(x, face.h - 1, -18)
    for y in range(face.h):
        face.tint(0, y, 12)
        face.tint(face.w - 1, y, -14)


def knots(face, rng, n=1):
    for _ in range(n):
        if face.w >= 3 and face.h >= 3:
            x, y = rng.randrange(1, face.w - 1), rng.randrange(1, face.h - 1)
            face.set(x, y, DEAD_WOOD[0])
            face.set(x - 1, y, DEAD_WOOD[1])
            face.set(x + 1, y, DEAD_WOOD[1])


def soot_top(face, rng, rows=2):
    """Soot creeping down from the top of a face."""
    for y in range(rows):
        for x in range(face.w):
            if rng.random() < 0.8 - y * 0.35:
                face.set(x, y, SOOT[1 + (x % 2)])


def cloth_folds(face, rng, step=4):
    """Darker fold lines following the drape of a sail or banner."""
    for x in range(step // 2, face.w, step):
        for y in range(face.h):
            if rng.random() < 0.8:
                face.tint(x, y, -22)
        face.tint(x + 1, 0, 10)


def tears(face, rng, holes=3, ragged=True):
    """A torn sail: holes and a ragged lower edge (cutout, the frame shows through)."""
    for _ in range(holes):
        x, y = rng.randrange(face.w), rng.randrange(face.h)
        w, h = rng.randint(1, 3), rng.randint(2, 5)
        for dx in range(w):
            for dy in range(h):
                face.clear(x + dx, y + dy)
    if ragged:
        for x in range(face.w):
            for k in range(rng.randint(0, 4)):
                face.clear(x, face.h - 1 - k)


def pierced(face, rng):
    """A pierced metal band: glowing holes where the fire inside shows."""
    for x in range(1, face.w, 2):
        for y in range(0, face.h, 2):
            face.lit(x, y, EMBER[3], 0.9)


def grain_stripes(face, rng):
    """Alternating lighter plank stripes along a long face."""
    if face.w > 4:
        for x in range(0, face.w, 4):
            for y in range(face.h):
                face.tint(x, y, -14)


def stone_cracks(face, rng, n=2):
    for _ in range(n):
        x, y = rng.randrange(face.w), rng.randrange(face.h)
        for _ in range(rng.randint(2, 5)):
            face.set(x, y, ASH_STONE[0])
            x += rng.choice((-1, 0, 0, 1))
            y += 1


def ash_dust(face, rng):
    """Pale ash caught on the top of a stone."""
    if face.name == "top":
        for y in range(face.h):
            for x in range(face.w):
                if rng.random() < 0.3:
                    face.set(x, y, SAILCLOTH[4 + rng.randint(0, 2)])


# --- Model helpers -----------------------------------------------------------------------------------------------

def bevel(origin, size, mat, decals=None, pivot=None, axis="y", inflate=0.0, name=None):
    """A box with its corners knocked off: the box and a copy turned 45° around its centre (an octagon in section)."""
    centre = [origin[i] + size[i] / 2 for i in range(3)]
    rotation = {"x": [45, 0, 0], "y": [0, 45, 0], "z": [0, 0, 45]}[axis]
    first = Box(origin, size, mat, decals, inflate=inflate, name=name)
    second = Box(origin, size, mat, decals, inflate=inflate - 0.05, rotation=rotation, pivot=pivot or centre)
    return [first, second]


def ring(pivot, radius, segments, length_pad, thickness, depth, mat, axis="z", decals=None, offset_deg=0.0, inflate=0.0):
    """A polygon rim around pivot: segments tangent at radius (an octagon for 8), thickness radially, depth along the axis."""
    boxes = []
    seg_len = 2 * radius * math.tan(math.pi / segments) + length_pad
    for i in range(segments):
        angle = offset_deg + 360.0 * i / segments
        if axis == "z":
            origin = [pivot[0] - seg_len / 2, pivot[1] + radius - thickness / 2, pivot[2] - depth / 2]
            size = [seg_len, thickness, depth]
            rotation = [0, 0, angle]
        elif axis == "x":
            origin = [pivot[0] - depth / 2, pivot[1] + radius - thickness / 2, pivot[2] - seg_len / 2]
            size = [depth, thickness, seg_len]
            rotation = [angle, 0, 0]
        else:
            origin = [pivot[0] - seg_len / 2, pivot[1] - depth / 2, pivot[2] + radius - thickness / 2]
            size = [seg_len, depth, thickness]
            rotation = [0, angle, 0]
        boxes.append(Box(origin, size, mat, decals, inflate=inflate, rotation=rotation, pivot=list(pivot)))
    return boxes


def spokes(pivot, inner, outer, count, width, depth, mat, axis="z", decals=None, offset_deg=0.0):
    """Bars from inner to outer radius around pivot."""
    boxes = []
    for i in range(count):
        angle = offset_deg + 360.0 * i / count
        length = outer - inner
        if axis == "z":
            origin = [pivot[0] - width / 2, pivot[1] + inner, pivot[2] - depth / 2]
            size = [width, length, depth]
            rotation = [0, 0, angle]
        elif axis == "x":
            origin = [pivot[0] - depth / 2, pivot[1] + inner, pivot[2] - width / 2]
            size = [depth, length, width]
            rotation = [angle, 0, 0]
        else:
            origin = [pivot[0] - width / 2, pivot[1] - depth / 2, pivot[2] + inner]
            size = [width, depth, length]
            rotation = [0, angle, 0]
        boxes.append(Box(origin, size, mat, decals, rotation=rotation, pivot=list(pivot)))
    return boxes


def chain_links(x, y_top, z, count, link=4, mat=None, vertical=True):
    """A chain hanging from (x, y_top, z): alternating flat and edge-on links, each link px tall, overlapping a little."""
    boxes = []
    m = mat or IRON
    for i in range(count):
        y = y_top - (i + 1) * (link - 1)
        if i % 2 == 0:
            boxes.append(Box([x - 1.5, y, z - 0.5], [3, link, 1], m, {"all": spec_line}))
        else:
            boxes.append(Box([x - 0.5, y, z - 1.5], [1, link, 3], m, {"all": spec_line}))
    return boxes


def diagonal(a, b, thickness, mat, decals=None):
    """A bar from point a to point b: a box along y turned about x, then about z (Bedrock's Z-Y-X order), so the
    same helper serves cords, braces and rope stays in any plane. Signs match GeckoLib's render (checked in
    tools/model_preview.py: the windmill's rope stays run tip to tip, the scale cords reach the pans)."""
    dx, dy, dz = b[0] - a[0], b[1] - a[1], b[2] - a[2]
    length = math.sqrt(dx * dx + dy * dy + dz * dz)
    mid = [(a[i] + b[i]) / 2 for i in range(3)]
    ux, uy, uz = dx / length, dy / length, dz / length
    alpha = math.degrees(math.asin(max(-1.0, min(1.0, uz))))
    gamma = math.degrees(math.atan2(-ux, uy))
    return Box([mid[0] - thickness / 2, mid[1] - length / 2, mid[2] - thickness / 2], [thickness, length, thickness],
               mat, decals, rotation=[-alpha, 0, -gamma], pivot=mid)


def ek(*entries):
    """Eased keyframes: ek(t, [x,y,z], "easeInOutSine", t2, [...], "linear", ...) -> {"t": {"vector": ..., "easing": ...}}."""
    out = {}
    for i in range(0, len(entries), 3):
        time, vector, easing = entries[i], entries[i + 1], entries[i + 2]
        key = str(float(round(time, 4)))
        frame = {"vector": [round(v, 3) for v in vector]}
        if easing and easing != "linear":
            frame["easing"] = easing
        out[key] = frame
    return out


def sine_loop(length, amp, axis=0, phase=0.0, steps=8, offset=0.0):
    """A smooth looping sway on one axis, sampled at steps keys with sine easing between them."""
    pairs = []
    for i in range(steps + 1):
        time = length * i / steps
        v = [0.0, 0.0, 0.0]
        v[axis] = offset + amp * math.sin(2 * math.pi * (i / steps + phase))
        pairs += [time, v, "linear"]
    return ek(*pairs)


def sway2(length, ax, az, phase=0.0, steps=8, lag=0.0):
    """A sway on x and z (a hanging thing circling a little), out of phase."""
    out = {}
    for i in range(steps + 1):
        time = length * i / steps
        k = i / steps + phase - lag
        out[str(float(round(time, 4)))] = {"vector": [round(ax * math.sin(2 * math.pi * k), 3), 0,
                                                       round(az * math.sin(2 * math.pi * (k + 0.25)), 3)]}
    return out


def sounds(animation, *pairs):
    """Attaches sound keyframes: sounds(a, 0.0, "creak", 8.0, "creak")."""
    animation["sound_effects"] = {str(float(round(pairs[i], 3))): {"effect": pairs[i + 1]} for i in range(0, len(pairs), 2)}
    return animation


def hold(bone_values):
    """A one-key loop that holds a pose (a state loop with no motion)."""
    return anim(1.0, {bone: {"position": keys(0, value)} if channel == "position" else {"rotation": keys(0, value)}
                      for bone, (channel, value) in bone_values.items()}, loop=True)


# --- Pieces -----------------------------------------------------------------------------------------------------

def windmill_sails(torn=False):
    """The sail wheel on its axle block: four 3.6-block blades, sailcloth on one side of each spar, a tail and rope
    stays between the tips. The wheel turns about the axle (z) once in 16 s; a creak now and then."""
    bones = []
    hub_mat = WOOD.but(decal=knots)
    hub = Bone("hub", [0, 8, 0])
    hub.add(*bevel([-5, 3, -7], [10, 10, 10], hub_mat, {"all": wood_shade}, axis="z"))
    hub.add(Box([-2.5, 5.5, 2], [5, 5, 7], WOOD_H, {"all": wood_shade}))                    # axle into the tower
    hub.add(Box([-3, 4, 1], [6, 8, 2], IRON, {"all": spec_line}))                           # iron collar
    hub.add(Box([-1, -3, 1], [2, 12, 10], WOOD, {"all": wood_shade}, rotation=[35, 0, 0], pivot=[0, 8, 2]))  # tail pole
    hub.add(Box([-4, -9, 7], [8, 9, 1], PLANK, {"all": grain_stripes}, rotation=[35, 0, 0], pivot=[0, 8, 2]))  # tail vane
    bones.append(hub)
    wheel = Bone("wheel", [0, 8, -3], parent="hub")
    bones.append(wheel)
    spar_len = 58
    for i in range(4):
        blade = Bone("blade_%d" % i, [0, 8, -3], parent="wheel", rotation=[0, 0, 90 * i])
        blade.add(Box([-1.5, 8, -4.5], [3, spar_len, 3], WOOD, {"all": wood_shade}))             # spar
        blade.add(Box([-2, 8 + spar_len - 2, -5], [4, 4, 4], IRON, {"all": spec_line}))            # tip cap
        for y in (22, 36, 50):                                                                     # cross bars
            blade.add(Box([0, y, -4], [13, 1.5, 1.5], WOOD_H))
        blade.add(Box([12, 20, -4], [1.5, 33, 1.5], WOOD))                                        # outer lath
        blade.add(Box([6, 20, -3.8], [1, 33, 1], WOOD))                                           # middle lath
        missing = torn and i == 1
        if not missing:
            decals = {"all": (lambda f, r: (cloth_folds(f, r), tears(f, r, 4, True)))} if torn and i != 3 \
                else {"all": cloth_folds}
            h = 20 if torn and i == 3 else 36
            blade.add(Box([0.5, 20 + (36 - h), -3.2], [12, h, 0], CLOTH, decals))                  # sailcloth
        bones.append(blade)
    # Rope stays from tip to tip: a square around the wheel.
    tip = 8 + spar_len - 2
    tips = []
    for i in range(4):
        a = math.radians(90 * i)
        tips.append([-(tip - 8) * math.sin(a), 8 + (tip - 8) * math.cos(a), -5.5])
    for i in range(4):
        wheel.add(diagonal(tips[i], tips[(i + 1) % 4], 1, ROPE_M))
        inner = [[(tips[j][0]) * 0.55, 8 + (tips[j][1] - 8) * 0.55, -5.5] for j in range(4)]
        wheel.add(diagonal(inner[i], inner[(i + 1) % 4], 1, ROPE_M))
    idle = anim(16.0, {"wheel": {"rotation": keys(0, [0, 0, 0], 16.0, [0, 0, 360])},
                       "hub": {"rotation": sine_loop(16.0, 1.5, axis=1)}}, loop=True)
    sounds(idle, 0.0, "creak", 8.0, "creak")
    return bones, {"idle": idle}, (9, 9)


def granary_hoist():
    """A beam out of a granary wall with a pulley at its end; a chain and a crate sway under it."""
    bones = []
    beam = Bone("beam", [0, 12, 8])
    beam.add(Box([-3, 10, -22], [6, 5, 30], WOOD_H, {"all": (lambda f, r: (wood_shade(f, r), knots(f, r)))}))
    beam.add(Box([-3.5, 9.5, -20], [7, 6, 2], IRON, {"all": spec_line}))      # iron strap near the pulley
    beam.add(Box([-3.5, 9.5, 2], [7, 6, 2], IRON, {"all": spec_line}))        # strap at the wall
    beam.add(Box([-4, 6, 6], [8, 10, 3], STONE, {"all": stone_cracks}))       # wall bracket stone
    beam.add(diagonal([0, 2, 7], [0, 10, -12], 2.5, WOOD))                    # brace under the beam
    beam.add(Box([-1, 15, -4], [2, 2, 4], IRON))                              # cleat on top
    bones.append(beam)
    pulley = Bone("pulley", [0, 9, -18], parent="beam")
    pulley.add(*bevel([-3.5, 5.5, -19.5], [7, 7, 3], WOOD, axis="z"))
    pulley.add(Box([-1, 8, -20], [2, 2, 4], IRON))                            # axle pin
    bones.append(pulley)
    chain1 = Bone("chain1", [0, 7, -18], parent="beam")
    chain1.add(*chain_links(0, 7, -18, 3))
    bones.append(chain1)
    chain2 = Bone("chain2", [0, -2, -18], parent="chain1")
    chain2.add(*chain_links(0, -2, -18, 3))
    bones.append(chain2)
    crate = Bone("crate", [0, -11, -18], parent="chain2")
    crate.add(Box([-1, -13, -19], [2, 3, 2], IRON))                           # hook ring
    crate.add(Box([-5, -23, -23], [10, 9, 10], PLANK, {"all": grain_stripes}))
    for y in (-22, -16):                                                       # plank bands
        crate.add(Box([-5.2, y, -23.2], [10.4, 1.5, 10.4], WOOD_H, inflate=0.1))
    crate.add(diagonal([-5.6, -22, -23.4], [5.6, -15, -23.4], 1.2, WOOD))     # cross batten, front
    crate.add(diagonal([0, -15, -13.4], [0, -22, -22.6], 1.2, WOOD))          # rope round the crate
    crate.add(Box([-5.6, -13.5, -23.6], [11.2, 0.8, 11.2], ROPE_M))            # rope loop on top
    bones.append(crate)
    idle = anim(6.0, merge({"chain1": {"rotation": sway2(6.0, 4, 3)}},
                           {"chain2": {"rotation": sway2(6.0, 3, 2, lag=0.08)}},
                           {"crate": {"rotation": sine_loop(6.0, 10, axis=1, phase=0.3)}},
                           {"pulley": {"rotation": sine_loop(6.0, 25, axis=2)}}), loop=True)
    swing = anim(2.5, {"chain1": {"rotation": ek(0, [0, 0, 0], "easeOutCubic", 0.4, [-16, 0, 5], "easeInOutSine",
                                                 1.2, [12, 0, -4], "easeInOutSine", 1.9, [-5, 0, 2], "easeInOutSine",
                                                 2.5, [0, 0, 0], "easeOutSine")},
                       "chain2": {"rotation": ek(0, [0, 0, 0], "easeOutCubic", 0.55, [-12, 0, 4], "easeInOutSine",
                                                 1.35, [10, 0, -3], "easeInOutSine", 2.0, [-4, 0, 1], "easeInOutSine",
                                                 2.5, [0, 0, 0], "easeOutSine")},
                       "crate": {"rotation": ek(0, [0, 0, 0], "easeOutCubic", 0.7, [-6, 20, 3], "easeInOutSine",
                                                1.5, [5, -15, -2], "easeInOutSine", 2.5, [0, 0, 0], "easeOutSine")}}, loop=False)
    sounds(swing, 0.1, "chain", 1.3, "creak")
    return bones, {"idle": idle, "action.swing": swing}, (3, 3)


def hanging_scales():
    """Market scales on a hook: a brass beam, two pans on cords, weights on one and a sack on the other."""
    bones = []
    hook = Bone("hook", [0, 16, 0])
    hook.add(Box([-1, 12, -1], [2, 4, 2], IRON, {"all": spec_line}))
    hook.add(Box([-2, 11, -1], [4, 1.5, 2], IRON))
    bones.append(hook)
    beam = Bone("beam", [0, 11, 0], parent="hook")
    beam.add(Box([-9, 10, -0.75], [18, 1.5, 1.5], BRASS, {"all": spec_line}))
    beam.add(Box([-1, 7, -1], [2, 5, 2], BRASS, {"all": spec_line}))                # centre post
    beam.add(Box([-1.5, 6, -1.5], [3, 1, 3], BRASS))                                # pointer plate
    beam.add(Box([-9.5, 9.5, -1], [1.5, 2.5, 2], BRASS))                            # end knobs
    beam.add(Box([8, 9.5, -1], [1.5, 2.5, 2], BRASS))
    bones.append(beam)
    for side, name in ((-1, "pan_l"), (1, "pan_r")):
        pan = Bone(name, [side * 8.5, 10, 0], parent="beam")
        top = [side * 8.5, 10, 0]
        for dx, dz in ((-2.5, -2.5), (2.5, -2.5), (0, 3)):
            pan.add(diagonal(top, [side * 8.5 + dx, 0, dz], 1, ROPE_M))
        pan.add(*bevel([side * 8.5 - 3.5, -1, -3.5], [7, 1, 7], BRASS, {"all": spec_line}, axis="y"))
        pan.add(Box([side * 8.5 - 3.5, 0, -3.5], [7, 1, 7], BRASS, inflate=-0.2))
        if side < 0:
            pan.add(Box([side * 8.5 - 1.5, 0, -1.5], [3, 2, 3], IRON_DARK, {"all": spec_line}))
            pan.add(Box([side * 8.5 + 0.5, 0, 0.5], [2, 1.5, 2], IRON_DARK))
        else:
            pan.add(Box([side * 8.5 - 2.5, 0, -2], [5, 3, 4], ROPE_M.but(grain=None, noise=0.9), {"all": soot_top}))
        bones.append(pan)
    idle = anim(5.0, merge({"hook": {"rotation": sway2(5.0, 2, 1.5)}},
                           {"beam": {"rotation": sine_loop(5.0, 3, axis=2)}},
                           {"pan_l": {"rotation": sine_loop(5.0, -3, axis=2)}},
                           {"pan_r": {"rotation": sine_loop(5.0, -3, axis=2)}}), loop=True)
    swing = anim(2.4, {"hook": {"rotation": ek(0, [0, 0, 0], "easeOutCubic", 0.4, [-14, 0, 6], "easeInOutSine",
                                               1.1, [10, 0, -5], "easeInOutSine", 1.8, [-4, 0, 2], "easeInOutSine",
                                               2.4, [0, 0, 0], "easeOutSine")},
                       "beam": {"rotation": ek(0, [0, 0, 0], "easeOutCubic", 0.5, [0, 0, 9], "easeInOutSine",
                                               1.3, [0, 0, -7], "easeInOutSine", 2.4, [0, 0, 0], "easeOutSine")},
                       "pan_l": {"rotation": ek(0, [0, 0, 0], "linear", 0.5, [4, 0, -9], "easeInOutSine", 1.3, [-3, 0, 7], "easeInOutSine", 2.4, [0, 0, 0], "easeOutSine")},
                       "pan_r": {"rotation": ek(0, [0, 0, 0], "linear", 0.5, [-4, 0, -9], "easeInOutSine", 1.3, [3, 0, 7], "easeInOutSine", 2.4, [0, 0, 0], "easeOutSine")}}, loop=False)
    sounds(swing, 0.5, "clink", 1.3, "clink")
    return bones, {"idle": idle, "action.swing": swing}, (2, 2)


def ash_mill():
    """A great stone wheel on a crosswise axle in a wooden frame, standing two blocks below its block: still, or
    spinning up with a grind when the storm blows."""
    bones = []
    frame = Bone("frame", [0, 8, 0])
    stone_deco = {"all": (lambda f, r: (stone_cracks(f, r), ash_dust(f, r)))}
    for x in (-9, 9):
        for z in (-13, 13):
            frame.add(Box([x - 2.5, -32, z - 2.5], [5, 54, 5], WOOD, {"all": wood_shade}))      # posts
    for z in (-13, 13):
        frame.add(Box([-13, 20, z - 2.5], [26, 4, 5], WOOD_H, {"all": wood_shade}))              # top beams across
        frame.add(Box([-11, -32, z - 3.5], [22, 5, 7], STONE_DARK, stone_deco))                  # sills on the ground
    for x in (-9, 9):
        frame.add(Box([x - 2.5, 20, -15], [5, 4, 30], WOOD, {"all": wood_shade}))                # lengthwise beams
        frame.add(diagonal([x, -4, -13], [x, 18, 0], 2.5, WOOD))                                  # braces
        frame.add(diagonal([x, -4, 13], [x, 18, 0], 2.5, WOOD))
        frame.add(Box([x - 3.5, 4, -4.5], [7, 8, 9], IRON, {"all": spec_line}))                  # axle bearings
    frame.add(Box([-14, 8 - 2, -2], [28, 4, 4], IRON_DARK, {"all": spec_line}))                  # axle along x
    bones.append(frame)
    wheel = Bone("wheel", [0, 8, 0], parent="frame")
    wheel.add(Box([-15, -7, -4], [30, 30, 8], STONE, stone_deco))
    wheel.add(Box([-15, -7, -4], [30, 30, 8], STONE, stone_deco, rotation=[0, 0, 45], pivot=[0, 8, 0], inflate=-0.1))
    wheel.add(*ring([0, 8, 0], 19, 12, 0.6, 11, 8, STONE, decals=stone_deco, offset_deg=15))
    wheel.add(*bevel([-6, 2, -5], [12, 12, 10], IRON, {"all": spec_line}, axis="z"))
    wheel.add(*ring([0, 8, 0], 23.5, 8, 0, 2, 10, IRON_DARK, decals={"all": rivet_row}, offset_deg=22.5))  # iron tyre
    bones.append(wheel)
    spinning = anim(1.6, {"wheel": {"rotation": keys(0, [0, 0, 0], 1.6, [0, 0, 360])},
                          "frame": {"rotation": sine_loop(0.4, 0.4, axis=0)}}, loop=True)
    sounds(spinning, 0.0, "grind")
    spin_up = anim(2.0, {"wheel": {"rotation": ek(0, [0, 0, 0], "easeInQuad", 1.0, [0, 0, 120], "easeInQuad",
                                                  2.0, [0, 0, 480], "linear")},
                         "frame": {"rotation": ek(0, [0, 0, 0], "linear", 0.3, [1.5, 0, 0], "easeInOutSine",
                                                  0.6, [-1.2, 0, 0], "easeInOutSine", 1.0, [0.6, 0, 0], "easeInOutSine",
                                                  2.0, [0, 0, 0], "easeOutSine")}}, loop=False)
    sounds(spin_up, 0.0, "creak", 0.6, "grind", 1.4, "grind")
    return bones, {"state.spinning": spinning, "action.spin_up": spin_up}, (4, 5)


def hanging_cage():
    """An iron gibbet cage on its chain under a beam, a bundle of bones in it (the bones bone, hidden by NBT)."""
    bones = []
    anchor = Bone("anchor", [0, 16, 0])
    anchor.add(Box([-2, 14, -2], [4, 2, 4], IRON, {"all": spec_line}))
    anchor.add(Box([-1, 11, -1], [2, 3, 2], IRON))
    bones.append(anchor)
    chain = Bone("chain", [0, 11, 0], parent="anchor")
    chain.add(*chain_links(0, 11, 0, 3))
    bones.append(chain)
    cage = Bone("cage", [0, 2, 0], parent="chain")
    cage.add(Box([-1.5, 0.5, -1.5], [3, 2, 3], IRON))                                           # top ring
    cage.add(*ring([0, 0, 0], 5.5, 8, 0.3, 1, 1, IRON, axis="y"))                               # top band
    cage.add(*ring([0, -9, 0], 6, 8, 0.3, 1, 1, IRON, axis="y", offset_deg=22.5))               # middle band
    cage.add(*ring([0, -19, 0], 5.5, 8, 0.3, 1, 1, IRON, axis="y"))                              # bottom band
    for i in range(8):                                                                            # bars
        a = math.radians(45 * i + 22.5)
        cage.add(Box([5.5 * math.cos(a) - 0.5, -20, 5.5 * math.sin(a) - 0.5], [1, 21, 1], IRON_DARK))
    cage.add(*bevel([-4, -20.5, -4], [8, 1, 8], IRON_DARK, axis="y"))                              # floor
    cage.add(Box([-1, -20, 5.5], [2, 5, 1], RUST and IRON.but(colors=RUST, base=2.2)))            # a rusted hasp
    bones.append(cage)
    bundle = Bone("bones", [0, -19, 0], parent="cage")
    bundle.add(Box([-2, -19.5, -2], [4, 4, 4], BONE_M, {"front": (lambda f, r: (f.rect(0, 1, 1, 1, SOOT[0]), f.rect(2, 1, 1, 1, SOOT[0])))}))  # skull
    bundle.add(Box([-3, -19.5, 1], [6, 1, 1], BONE_M, rotation=[0, 20, 0]))
    bundle.add(Box([-1, -19.5, -3.5], [1, 1, 6], BONE_M, rotation=[0, -15, 0]))
    bundle.add(Box([-2.5, -18.5, -2.5], [5, 2, 1], BONE_M, rotation=[0, 35, 0]))                  # ribs
    bundle.add(Box([1, -19.5, -3], [1, 5, 1], BONE_M, rotation=[0, 0, 60]))
    bones.append(bundle)
    idle = anim(7.0, merge({"chain": {"rotation": sway2(7.0, 3, 2)}},
                           {"cage": {"rotation": sway2(7.0, 2, 1.5, lag=0.1)}},
                           {"bones": {"rotation": sine_loop(7.0, 6, axis=1, phase=0.4)}}), loop=True)
    swing = anim(3.0, {"chain": {"rotation": ek(0, [0, 0, 0], "easeOutCubic", 0.5, [-18, 0, 6], "easeInOutSine",
                                                1.4, [14, 0, -5], "easeInOutSine", 2.2, [-6, 0, 2], "easeInOutSine",
                                                3.0, [0, 0, 0], "easeOutSine")},
                       "cage": {"rotation": ek(0, [0, 0, 0], "easeOutCubic", 0.65, [-10, 25, 4], "easeInOutSine",
                                               1.55, [8, -20, -3], "easeInOutSine", 2.35, [-3, 8, 1], "easeInOutSine",
                                               3.0, [0, 0, 0], "easeOutSine")}}, loop=False)
    sounds(swing, 0.1, "chain", 1.5, "clink")
    return bones, {"idle": idle, "action.swing": swing}, (2, 3)


def great_gear():
    """A great wooden cog on an iron mount, flush with a wall: still, or turning through the fight."""
    bones = []
    mount = Bone("mount", [0, 8, 0])
    mount.add(Box([-5, 3, 2], [10, 10, 6], IRON, {"all": (lambda f, r: (spec_line(f, r), rivet_row(f, r, 3, 1)))}))
    mount.add(Box([-7, 1, 6], [14, 14, 2], STONE_DARK, {"all": stone_cracks}))
    mount.add(Box([-2, 5, -6], [4, 6, 10], IRON_DARK, {"all": spec_line}))      # axle
    bones.append(mount)
    gear = Bone("gear", [0, 8, -3], parent="mount")
    gear.add(*bevel([-5, 3, -6], [10, 10, 6], WOOD, {"all": wood_shade}, axis="z"))
    gear.add(Box([-3, 6, -7], [6, 4, 1.5], IRON, {"all": spec_line}))
    gear.add(*spokes([0, 8, -3], 4, 20, 8, 3, 3, WOOD, decals={"all": wood_shade}))
    gear.add(*ring([0, 8, -3], 20.5, 8, 0.4, 4, 4, WOOD_H, decals={"all": (lambda f, r: (wood_shade(f, r), rivet_row(f, r, 4, 1)))}))
    gear.add(*spokes([0, 8, -3], 22, 27, 8, 4.5, 4, WOOD, decals={"all": wood_shade}, offset_deg=22.5))   # teeth
    bones.append(gear)
    turning = anim(8.0, {"gear": {"rotation": keys(0, [0, 0, 0], 8.0, [0, 0, 360])}}, loop=True)
    sounds(turning, 0.0, "creak", 4.0, "creak")
    return bones, {"state.turning": turning}, (4, 4)


def great_bell():
    """A bronze bell hanging from a yoke in the block above it: a slow sway, two tolls when the fight says so."""
    bones = []
    yoke = Bone("yoke", [0, 12, 0])
    yoke.add(Box([-8, 12, -3], [16, 4, 6], WOOD_H, {"all": (lambda f, r: (wood_shade(f, r), knots(f, r)))}))
    for x in (-6.5, 4.5):
        yoke.add(Box([x, 11.5, -3.5], [2, 5, 7], IRON, {"all": spec_line}))                       # straps
        yoke.add(Box([x + 0.5, 11, -0.5], [1, 1, 1], GRAVE_IRON and IRON.but(base=4.0)))         # rivets
        yoke.add(Box([x + 0.5, 15.5, -0.5], [1, 1, 1], IRON.but(base=4.0)))
    for x in (-7, 5):
        yoke.add(Box([x, 6, -1.5], [2, 7, 3], IRON, {"all": spec_line}))                           # hangers
    yoke.add(Box([-7.5, 8, -1], [15, 2, 2], IRON_DARK))                                            # axle
    bones.append(yoke)
    bell = Bone("bell", [0, 9, 0], parent="yoke")
    bz = {"all": spec_line}
    bell.add(Box([-1.5, 6.5, -1.5], [3, 3, 3], BRASS, bz))                                        # crown loop
    bell.add(*bevel([-4, 3.5, -4], [8, 3.5, 8], BRASS, bz, axis="y"))                              # head
    bell.add(*bevel([-6, -1, -6], [12, 4.5, 12], BRASS, bz, axis="y"))                             # shoulder
    bell.add(*bevel([-7, -9, -7], [14, 8, 14], BRASS, bz, axis="y"))                               # waist
    bell.add(*bevel([-8, -12, -8], [16, 3, 16], BRASS.but(base=2.6), bz, axis="y", inflate=0.1))   # lip
    bell.add(*ring([0, -5, 0], 7.3, 8, 0.4, 0.8, 1.2, BRASS.but(base=4.2), axis="y", offset_deg=22.5))  # rib
    bones.append(bell)
    clapper = Bone("clapper", [0, 4, 0], parent="bell")
    clapper.add(Box([-0.5, -9, -0.5], [1, 13, 1], IRON_DARK))
    clapper.add(Box([-1.5, -12, -1.5], [3, 3, 3], IRON, bz))
    bones.append(clapper)
    idle = anim(6.0, merge({"bell": {"rotation": sine_loop(6.0, 2.5, axis=0)}},
                           {"clapper": {"rotation": sine_loop(6.0, -2, axis=0, phase=0.08)}}), loop=True)
    toll = anim(2.6, {"bell": {"rotation": ek(0, [0, 0, 0], "easeInOutSine", 0.45, [-38, 0, 0], "easeInOutSine",
                                              1.05, [36, 0, 0], "easeInOutSine", 1.6, [-22, 0, 0], "easeInOutSine",
                                              2.1, [10, 0, 0], "easeInOutSine", 2.6, [0, 0, 0], "easeOutSine")},
                      "clapper": {"rotation": ek(0, [0, 0, 0], "linear", 0.3, [12, 0, 0], "easeInOutSine",
                                                 0.55, [-28, 0, 0], "easeOutBack", 0.9, [-10, 0, 0], "easeInOutSine",
                                                 1.15, [30, 0, 0], "easeOutBack", 1.5, [8, 0, 0], "easeInOutSine",
                                                 1.75, [-18, 0, 0], "easeOutBack", 2.2, [6, 0, 0], "easeInOutSine",
                                                 2.6, [0, 0, 0], "easeOutSine")}}, loop=False)
    sounds(toll, 0.5, "toll", 1.1, "toll")
    return bones, {"idle": idle, "action.toll": toll}, (2, 2)


def sluice_grate():
    """An iron grate in a stone arch, lifted on its chains when the flood rises."""
    bones = []
    frame = Bone("frame", [0, 8, 0])
    sd = {"all": stone_cracks}
    frame.add(Box([-8, 13, -3], [16, 3, 6], STONE, sd))                      # lintel
    frame.add(Box([-8, 0, -3], [3, 13, 6], STONE_DARK, sd))                  # jambs
    frame.add(Box([5, 0, -3], [3, 13, 6], STONE_DARK, sd))
    frame.add(Box([-8, 0, -3], [16, 1.5, 6], STONE_DARK, {"all": ash_dust}))  # sill
    frame.add(Box([-6, 11, -4], [2, 2, 2], STONE, rotation=[0, 0, 45]))      # corbels
    frame.add(Box([4, 11, -4], [2, 2, 2], STONE, rotation=[0, 0, 45]))
    frame.add(Box([-3, 14, -4.5], [1, 1.5, 1], IRON))                         # chain eyes
    frame.add(Box([2, 14, -4.5], [1, 1.5, 1], IRON))
    bones.append(frame)
    grate = Bone("grate", [0, 1.5, 0], parent="frame")
    gd = {"all": spec_line}
    for x in (-4, -1.5, 1, 3.5):
        grate.add(Box([x - 0.5, 1.5, -0.5], [1, 11.5, 1], IRON_DARK, gd))     # bars
        grate.add(Box([x - 0.75, 1, -0.75], [1.5, 1.5, 1.5], IRON_DARK))      # spikes' feet
    for y in (4, 8):
        grate.add(Box([-5, y, -0.75], [10, 1, 1.5], IRON, gd))                # cross bars
    grate.add(Box([-5, 12, -1], [10, 1.5, 2], IRON, gd))                       # lifting bar
    grate.add(Box([-3, 13.5, -1], [1, 1.5, 1], IRON))                          # short chains
    grate.add(Box([2, 13.5, -1], [1, 1.5, 1], IRON))
    bones.append(grate)
    opened = hold({"grate": ("position", [0, 11, 0])})
    lift = anim(2.5, {"grate": {"position": ek(0, [0, 0, 0], "easeInOutSine", 0.3, [0, 0.5, 0], "easeInOutSine",
                                               2.2, [0, 11, 0], "easeOutSine", 2.5, [0, 11, 0], "linear")}}, loop=False)
    sounds(lift, 0.1, "chain", 1.0, "chain", 1.9, "chain")
    return bones, {"state.open": opened, "action.lift": lift}, (1, 2)


def great_brazier():
    """A great iron brazier on four clawed legs, its bowl in its own block and its legs around the campfire below:
    an ash bed when cold, coals and a flame (the flame bones, hidden when cold) when lit."""
    bones = []
    body = Bone("body", [0, 8, 0])
    bd = {"all": (lambda f, r: (spec_line(f, r), soot_top(f, r, 1)))}
    body.add(*bevel([-7, 2, -7], [14, 6, 14], IRON, bd, axis="y"))                 # bowl
    body.add(*bevel([-8, 8, -8], [16, 2, 16], IRON, {"all": (lambda f, r: (spec_line(f, r), rivet_row(f, r, 3, 0)))}, axis="y", inflate=0.1))  # rim
    body.add(*bevel([-5, 1, -5], [10, 1, 10], IRON_DARK, axis="y"))                 # base plate
    for i in range(4):
        a = math.radians(45 + 90 * i)
        x, z = 5.5 * math.cos(a), 5.5 * math.sin(a)
        leg = Box([x - 1, -16, z - 1], [2, 18, 2], IRON_DARK, {"all": spec_line},
                  rotation=[-6 * math.sin(a), 0, 6 * math.cos(a)], pivot=[x, 2, z])
        body.add(leg)
        body.add(Box([x - 1.5, -16, z - 1.5], [3, 2, 3], IRON, {"all": spec_line}))   # claw foot
    for i in range(4):                                                                 # strap bands
        body.add(Box([-7.5, 4, -7.5], [15, 1, 15], IRON_DARK, {"all": rivet_row}, rotation=[0, 45 * (i % 2), 0], inflate=0.15 + 0.05 * i)) if i < 2 else None
    body.add(Box([-5, 7, -5], [10, 1.5, 10], ASH_BED))                                   # ash bed
    bones.append(body)
    coals = Bone("flame_coals", [0, 8, 0], parent="body")
    coals.add(*bevel([-5, 7.5, -5], [10, 2, 10], COALS, axis="y"))
    bones.append(coals)
    flame = Bone("flame", [0, 9, 0], parent="body")
    flame.add(Box([-5, 9, 0], [10, 12, 0], FLAME, {"all": (lambda f, r: tears(f, r, 2, True))}))
    flame.add(Box([0, 9, -5], [0, 12, 10], FLAME, {"all": (lambda f, r: tears(f, r, 2, True))}))
    flame.add(Box([-4, 9, 0], [8, 9, 0], FLAME.but(base=4.2), {"all": (lambda f, r: tears(f, r, 1, True))}, rotation=[0, 45, 0], pivot=[0, 9, 0]))
    flame.add(Box([0, 9, -4], [0, 9, 8], FLAME.but(base=4.2), {"all": (lambda f, r: tears(f, r, 1, True))}, rotation=[0, 45, 0], pivot=[0, 9, 0]))
    flame.add(Box([-2, 9, 0], [4, 6, 0], FLAME.but(base=4.8), rotation=[0, 20, 0], pivot=[0, 9, 0]))
    bones.append(flame)
    lit = anim(1.2, merge({"flame": {"scale": ek(0, [1, 1, 1], "easeInOutSine", 0.3, [1.05, 1.25, 1.05], "easeInOutSine",
                                                 0.6, [0.95, 0.9, 0.95], "easeInOutSine", 0.9, [1.03, 1.15, 1.03], "easeInOutSine",
                                                 1.2, [1, 1, 1], "easeInOutSine"),
                                     "rotation": sine_loop(1.2, 8, axis=1)}},
                          {"flame_coals": {"scale": ek(0, [1, 1, 1], "easeInOutSine", 0.6, [1.0, 1.3, 1.0], "easeInOutSine", 1.2, [1, 1, 1], "easeInOutSine")}}), loop=True)
    return bones, {"state.lit": lit}, (2, 3)


def portcullis():
    """A gate hanging in a gatehouse lintel: seven bars wide (the Citadel gatehouse's gate; in the courtyard's
    five-wide keep gate the outer bars sit inside the wall), five and a half tall, spiked; a winding drum and two gear
    wheels on the wall face. Open it hides in the wall; the slam drops it in half a second."""
    bones = []
    frame = Bone("frame", [0, 8, 0])
    frame.add(Box([-8, 0, -8], [16, 16, 16], STONE, {"all": stone_cracks}))                       # the lintel block
    frame.add(Box([-20, 6, -12], [40, 4, 4], IRON_DARK, {"all": spec_line}))                      # axle across the face
    for x in (-19, 17):
        frame.add(Box([x, 2, -11], [2, 12, 3], IRON, {"all": (lambda f, r: (spec_line(f, r), rivet_row(f, r, 2, 0)))}))  # wall brackets
    bones.append(frame)
    drum = Bone("drum", [0, 8, -10], parent="frame")
    drum.add(*bevel([-9, 4, -14], [18, 8, 8], WOOD_H, {"all": wood_shade}, axis="x"))
    drum.add(Box([-9.5, 3.5, -14.5], [1, 9, 9], IRON, {"all": spec_line}))
    drum.add(Box([8.5, 3.5, -14.5], [1, 9, 9], IRON, {"all": spec_line}))
    bones.append(drum)
    for side, name in ((-1, "gear_l"), (1, "gear_r")):
        cx = side * 15
        gear = Bone(name, [cx, 8, -10], parent="frame")
        gear.add(*bevel([cx - 3, 5, -13], [6, 6, 4], IRON, {"all": spec_line}, axis="z"))
        gear.add(*spokes([cx, 8, -11], 2, 7.5, 6, 2, 2, IRON_DARK))
        gear.add(*ring([cx, 8, -11], 8, 6, 0.3, 2, 3, IRON, decals={"all": spec_line}))
        gear.add(*spokes([cx, 8, -11], 8.8, 11, 6, 2.5, 3, IRON, offset_deg=30))                 # teeth
        bones.append(gear)
    gate = Bone("gate", [0, 0, 0], parent="frame")
    gd = {"all": spec_line}
    for x in (-48, -32, -16, 0, 16, 32, 48):                                                        # seven bars: the gatehouse gate; the courtyard's five-wide gate hides the outer two in its wall
        gate.add(Box([x - 1.5, -90, -9.5], [3, 92, 3], IRON_DARK, gd))                             # bars
        gate.add(Box([x - 2, -95, -10], [4, 5, 4], IRON, gd, inflate=-0.3))                          # spikes
    for y in (-10, -28, -46, -64, -82):
        gate.add(Box([-52, y, -9], [104, 3, 2], IRON, {"all": (lambda f, r: (spec_line(f, r), rivet_row(f, r, 6, 1)))}))  # cross bars
    gate.add(Box([-10, 1, -9.5], [1.5, 6, 1.5], IRON))                                              # chains to the drum
    gate.add(Box([8.5, 1, -9.5], [1.5, 6, 1.5], IRON))
    bones.append(gate)
    open_hold = hold({"gate": ("position", [0, 86, 0])})
    closed_hold = hold({"gate": ("position", [0, 0, 0])})
    slam = anim(0.9, {"gate": {"position": ek(0, [0, 86, 0], "easeInQuad", 0.45, [0, -2, 0], "easeOutBack", 0.65, [0, 0, 0], "linear", 0.9, [0, 0, 0], "linear")},
                      "gear_l": {"rotation": ek(0, [0, 0, 0], "easeInQuad", 0.45, [0, 0, -720], "linear", 0.9, [0, 0, -740], "easeOutSine")},
                      "gear_r": {"rotation": ek(0, [0, 0, 0], "easeInQuad", 0.45, [0, 0, 720], "linear", 0.9, [0, 0, 740], "easeOutSine")},
                      "drum": {"rotation": ek(0, [0, 0, 0], "easeInQuad", 0.45, [-900, 0, 0], "linear", 0.9, [-920, 0, 0], "easeOutSine")},
                      "frame": {"rotation": ek(0, [0, 0, 0], "linear", 0.45, [0, 0, 0], "linear", 0.5, [0.8, 0, 0], "easeOutSine", 0.7, [-0.4, 0, 0], "easeInOutSine", 0.9, [0, 0, 0], "easeOutSine")}}, loop=False)
    sounds(slam, 0.0, "chain", 0.47, "slam")
    raise_ = anim(3.2, {"gate": {"position": ek(0, [0, 0, 0], "easeInOutSine", 0.4, [0, 2, 0], "easeInOutSine", 3.0, [0, 86, 0], "easeOutSine", 3.2, [0, 86, 0], "linear")},
                        "gear_l": {"rotation": ek(0, [0, 0, 0], "easeInSine", 3.0, [0, 0, 540], "easeOutSine")},
                        "gear_r": {"rotation": ek(0, [0, 0, 0], "easeInSine", 3.0, [0, 0, -540], "easeOutSine")},
                        "drum": {"rotation": ek(0, [0, 0, 0], "easeInSine", 3.0, [700, 0, 0], "easeOutSine")}}, loop=False)
    sounds(raise_, 0.2, "chain", 1.1, "chain", 2.0, "chain", 2.9, "clink")
    return bones, {"state.open": open_hold, "state.closed": closed_hold, "action.slam": slam, "action.raise": raise_}, (6, 7)


def forge_bellows():
    """Forge bellows on a plank stand, the nozzle pointing north into the hearth: the top board lifts slowly and
    falls, the leather bag between the boards swelling with it."""
    bones = []
    base = Bone("base", [0, 0, 0])
    base.add(Box([-7, 1.5, -7], [14, 1.5, 14], PLANK, {"all": grain_stripes}))
    for x in (-6, 4):
        for z in (-6, 4):
            base.add(Box([x, 0, z], [2, 1.5, 2], WOOD))
    base.add(Box([-6, 3, -7], [12, 1.5, 14], WOOD_H, {"all": wood_shade}))                      # lower board
    base.add(Box([-1, 3.5, -16], [2, 2, 9], IRON, {"all": spec_line}))                           # nozzle
    base.add(Box([-1.5, 3, -17.5], [3, 3, 2], IRON_DARK, {"all": soot_top}))                     # nozzle mouth
    base.add(Box([-2, 2.5, -8.5], [4, 3, 2], IRON, {"all": spec_line}))                          # nozzle collar
    bones.append(base)
    bag = Bone("bag", [0, 4.5, 0])
    leather = Material(CHARRED, base=3.2, noise=0.5, grain="h", edge=0.6)
    for i in range(3):
        bag.add(Box([-5.5 + (i % 2) * 0.3, 4.5 + i * 0.8, -6.5 + (i % 2) * 0.3], [11 - (i % 2) * 0.6, 0.8, 13 - (i % 2) * 0.6], leather))
    bones.append(bag)
    upper = Bone("upper", [0, 7, 6.5])
    upper.add(Box([-6, 7, -7], [12, 1.8, 14], WOOD_H, {"all": (lambda f, r: (wood_shade(f, r), knots(f, r)))}))  # top board
    upper.add(Box([-6.5, 6.8, -3], [13, 2.2, 2], IRON, {"all": spec_line}))                                     # strap
    upper.add(Box([-0.75, 8.8, -5], [1.5, 1.5, 9], WOOD))                                                        # handle stem
    upper.add(Box([-3, 8.6, -6.5], [6, 2, 2], WOOD, {"all": wood_shade}))                                        # handle grip
    for x, z in ((-5, -6), (4, -6), (-5, 5), (4, 5)):
        upper.add(Box([x, 8.8, z], [1, 0.6, 1], IRON.but(base=4.0)))                                             # nails
    bones.append(upper)
    idle = anim(3.0, {"upper": {"rotation": ek(0, [0, 0, 0], "easeInOutSine", 1.3, [-18, 0, 0], "easeInOutSine",
                                               2.0, [-16, 0, 0], "easeInQuad", 2.6, [0, 0, 0], "easeOutBack", 3.0, [0, 0, 0], "linear")},
                      "bag": {"scale": ek(0, [1, 1, 1], "easeInOutSine", 1.3, [1.02, 2.6, 1.02], "easeInOutSine",
                                          2.0, [1.02, 2.4, 1.02], "easeInQuad", 2.6, [1, 1, 1], "easeOutSine", 3.0, [1, 1, 1], "linear")}}, loop=True)
    sounds(idle, 0.2, "creak", 2.6, "wood")
    return bones, {"idle": idle}, (2, 1)


def chain_winch():
    """A winch over a shaft: the drum turns one way, pays the bucket out, and turns back."""
    bones = []
    frame = Bone("frame", [0, 0, 0])
    for x in (-6.5, 4.5):
        frame.add(Box([x, 0, -1.5], [2, 16, 3], WOOD, {"all": wood_shade}))                       # posts
        frame.add(diagonal([x + 1, 0, 6], [x + 1, 9, 0], 1.5, WOOD))                              # braces
    frame.add(Box([-7.5, 15, -2], [15, 1.5, 4], WOOD_H, {"all": wood_shade}))                     # top bar
    frame.add(Box([6, 9, -0.5], [4, 1, 1], IRON))                                                 # crank arm
    frame.add(Box([9, 9, -2.5], [1, 1, 4], IRON))                                                 # crank
    frame.add(Box([8.5, 8, -3], [2, 3, 1.5], WOOD))                                               # crank grip
    frame.add(Box([-8, 8, -0.5], [1.5, 1.5, 1], IRON))                                            # pawl
    bones.append(frame)
    drum = Bone("drum", [0, 10, 0], parent="frame")
    drum.add(*bevel([-4.5, 6, -4], [9, 8, 8], WOOD_H, {"all": wood_shade}, axis="x"))
    drum.add(Box([-5.5, 5, -5], [1, 10, 10], IRON, {"all": spec_line}))
    drum.add(Box([4.5, 5, -5], [1, 10, 10], IRON, {"all": spec_line}))
    drum.add(Box([-4, 9, -1], [8, 2, 2], IRON_DARK, rotation=[45, 0, 0]))                        # chain wound on
    bones.append(drum)
    chain = Bone("chain", [0, 6, 0], parent="frame")
    chain.add(*chain_links(0, 6, 0, 8))
    bones.append(chain)
    bucket = Bone("bucket", [0, -18, 0], parent="chain")
    bucket.add(Box([-0.75, -19, -0.75], [1.5, 2, 1.5], IRON))
    bucket.add(diagonal([-4, -22, 0], [0, -18.5, 0], 1, IRON))                                   # bail
    bucket.add(diagonal([4, -22, 0], [0, -18.5, 0], 1, IRON))
    bucket.add(*bevel([-4, -30, -4], [8, 8, 8], PLANK, {"all": grain_stripes}, axis="y"))
    bucket.add(*ring([0, -24, 0], 4.3, 8, 0.3, 0.8, 1.2, IRON, axis="y"))
    bucket.add(Box([-3.5, -22.5, -3.5], [7, 0.6, 7], ASH_BED))                                   # ash in the bucket
    bones.append(bucket)
    idle = anim(8.0, merge({"drum": {"rotation": ek(0, [0, 0, 0], "easeInOutSine", 4.0, [200, 0, 0], "easeInOutSine", 8.0, [0, 0, 0], "easeInOutSine")}},
                           {"bucket": {"position": ek(0, [0, 0, 0], "easeInOutSine", 4.0, [0, -10, 0], "easeInOutSine", 8.0, [0, 0, 0], "easeInOutSine"),
                                       "rotation": sine_loop(8.0, 12, axis=1)}},
                           {"chain": {"rotation": sway2(8.0, 2, 1.5)}}), loop=True)
    sounds(idle, 0.0, "creak", 4.0, "creak", 2.0, "chain", 6.0, "chain")
    return bones, {"idle": idle}, (2, 3)


def censer():
    """A brass censer on a short chain from a ring: a slow sway; lit, the pierced band glows and smoke rises."""
    bones = []
    hook = Bone("hook", [0, 16, 0])
    hook.add(Box([-1.5, 14.5, -1.5], [3, 1.5, 3], IRON, {"all": spec_line}))
    hook.add(Box([-0.75, 12.5, -0.75], [1.5, 2.5, 1.5], IRON))
    bones.append(hook)
    chain = Bone("chain", [0, 12.5, 0], parent="hook")
    chain.add(*chain_links(0, 12.5, 0, 2, link=3))
    bones.append(chain)
    body = Bone("body", [0, 7, 0], parent="chain")
    bd = {"all": spec_line}
    body.add(Box([-0.5, 7, -0.5], [1, 1.5, 1], BRASS))                                    # finial
    body.add(*bevel([-2.5, 5.5, -2.5], [5, 1.5, 5], BRASS, bd, axis="y"))                 # lid
    body.add(*bevel([-3.5, 4.5, -3.5], [7, 1, 7], BRASS, bd, axis="y"))                   # lid rim
    body.add(Box([-3.5, 2.5, -3.5], [7, 2, 7], BRASS.but(base=2.2), {"all": pierced}))    # pierced band
    body.add(*bevel([-3.5, 0.5, -3.5], [7, 2, 7], BRASS, bd, axis="y"))                   # bowl
    body.add(*bevel([-2, -0.5, -2], [4, 1, 4], BRASS, axis="y"))                          # foot
    body.add(Box([-1, -1.5, -1], [2, 1, 2], BRASS))                                       # drop
    for i in range(3):
        a = math.radians(120 * i + 30)
        body.add(diagonal([0, 7.5, 0], [3.2 * math.cos(a), 3.5, 3.2 * math.sin(a)], 1, IRON))
        body.add(Box([3.2 * math.cos(a) - 0.5, 3.0, 3.2 * math.sin(a) - 0.5], [1, 1, 1], IRON.but(base=4.0)))  # chain studs
    bones.append(body)
    glow = Bone("flame_glow", [0, 3, 0], parent="body")
    glow.add(Box([-2.5, 2.7, -2.5], [5, 1.6, 5], COALS, inflate=0.05))
    bones.append(glow)
    idle = anim(5.0, merge({"chain": {"rotation": sway2(5.0, 4, 3)}},
                           {"body": {"rotation": sway2(5.0, 3, 2, lag=0.1)}},
                           {"hook": {"rotation": sine_loop(5.0, 5, axis=1)}}), loop=True)
    return bones, {"idle": idle}, (1, 1)


PIECES = {
    "windmill_sails": lambda: windmill_sails(False),
    "windmill_sails_torn": lambda: windmill_sails(True),
    "granary_hoist": granary_hoist,
    "hanging_scales": hanging_scales,
    "ash_mill": ash_mill,
    "hanging_cage": hanging_cage,
    "great_gear": great_gear,
    "great_bell": great_bell,
    "sluice_grate": sluice_grate,
    "great_brazier": great_brazier,
    "portcullis": portcullis,
    "forge_bellows": forge_bellows,
    "chain_winch": chain_winch,
    "censer": censer,
}
TEX_WIDTH = {"windmill_sails": 128, "windmill_sails_torn": 128, "portcullis": 128, "ash_mill": 128}
# The particle (breaking) texture's material per piece.
PARTICLE = {"windmill_sails": CLOTH, "windmill_sails_torn": CLOTH, "granary_hoist": WOOD, "hanging_scales": BRASS,
            "ash_mill": STONE, "hanging_cage": IRON, "great_gear": WOOD, "great_bell": BRASS, "sluice_grate": IRON,
            "great_brazier": IRON, "portcullis": IRON, "forge_bellows": WOOD, "chain_winch": WOOD, "censer": BRASS}


# --- Icons and particle models ----------------------------------------------------------------------------------

def box_corners(box):
    """World-space corners of a box (its rotation applied about its pivot), in model pixels."""
    o, s = box.origin, box.size
    pts = [[o[0] + dx * s[0], o[1] + dy * s[1], o[2] + dz * s[2]] for dx in (0, 1) for dy in (0, 1) for dz in (0, 1)]
    if box.rotation:
        piv = box.pivot or [o[i] + s[i] / 2 for i in range(3)]
        rx, ry, rz = [math.radians(a) for a in box.rotation]
        out = []
        for p in pts:
            x, y, z = p[0] - piv[0], p[1] - piv[1], p[2] - piv[2]
            y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
            x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
            x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
            out.append([x + piv[0], y + piv[1], z + piv[2]])
        pts = out
    return pts


def bone_offset(bones, bone):
    """Rest rotation of a bone chain is ignored for the icon; only the boxes' own placement counts."""
    return [0, 0, 0]


def icon(name, bones, hidden=()):
    """A 16 px item icon: the model's front view, each box a flat rectangle of its material's middle tone with a light
    top-left and a dark bottom-right edge, nearer boxes on top."""
    boxes = []
    for bone in bones:
        if bone.name in hidden:
            continue
        for box in bone.boxes:
            pts = box_corners(box)
            xs, ys, zs = [p[0] for p in pts], [p[1] for p in pts], [p[2] for p in pts]
            boxes.append((min(zs), min(xs), max(xs), min(ys), max(ys), box.mat))
    if not boxes:
        return Canvas(16, 16)
    x0, x1 = min(b[1] for b in boxes), max(b[2] for b in boxes)
    y0, y1 = min(b[3] for b in boxes), max(b[4] for b in boxes)
    span = max(x1 - x0, y1 - y0, 1e-3)
    scale = 14.0 / span
    ox = (16 - (x1 - x0) * scale) / 2
    oy = (16 - (y1 - y0) * scale) / 2
    canvas = Canvas(16, 16)
    for _, bx0, bx1, by0, by1, mat in sorted(boxes, key=lambda b: -b[0]):
        px0 = int(math.floor(ox + (bx0 - x0) * scale))
        px1 = int(math.ceil(ox + (bx1 - x0) * scale)) - 1
        py0 = int(math.floor(oy + (y1 - by1) * scale))
        py1 = int(math.ceil(oy + (y1 - by0) * scale)) - 1
        px1, py1 = max(px0, px1), max(py0, py1)
        mid = mat.colors[clamp(round(mat.base), 0, len(mat.colors) - 1)]
        light = mat.colors[clamp(round(mat.base) + 1, 0, len(mat.colors) - 1)]
        dark = mat.colors[clamp(round(mat.base) - 2, 0, len(mat.colors) - 1)]
        for y in range(py0, py1 + 1):
            for x in range(px0, px1 + 1):
                if 0 <= x < 16 and 0 <= y < 16:
                    c = light if (y == py0 or x == px0) else dark if (y == py1 or x == px1) else mid
                    if mat.glow:
                        c = tuple(clamp(v * 1.15) for v in c[:3]) + (255,)
                    canvas.set(15 - x, y, c)   # x mirrored like the render
    return canvas


def particle_texture(mat, seed):
    canvas, glow = Canvas(16, 16), Canvas(16, 16)
    import random as _random
    face = Face(canvas, glow, "front", (0, 0, 16, 16), None)
    mat.but(decal=None, glow=None).paint(face, _random.Random(seed), seed)
    return canvas


def write_piece(name):
    bones, animations, bounds = PIECES[name]()
    widest = max(box.uv_size()[0] for bone in bones for box in bone.boxes)
    write_model(name, bones, animations, bounds, tex_w=max(TEX_WIDTH.get(name, 64), next_pow2(widest)), folder=FOLDER)
    icon(name, bones).save(os.path.join(ROOT, "textures", "item", "setpiece", name + ".png"))
    particle_texture(PARTICLE[name], len(name)).save(os.path.join(ROOT, "textures", "block", "setpiece", name + "_particle.png"))
    write_json(os.path.join(ROOT, "models", "block", "setpiece", name + ".json"),
               {"textures": {"particle": "embervoid:block/setpiece/%s_particle" % name}})


def main(argv):
    names = argv or list(PIECES)
    for name in names:
        if name not in PIECES:
            raise SystemExit("unknown piece %s; have %s" % (name, ", ".join(PIECES)))
        write_piece(name)


if __name__ == "__main__":
    main(sys.argv[1:])
