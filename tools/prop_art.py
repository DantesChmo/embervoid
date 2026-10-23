#!/usr/bin/env python3
"""3D props of the burnt kingdom (PLAN 20.3): block models built from Blockbench-style elements and their 16x
textures, so a sack looks like a sack and not a cube with a sack texture.

Run from the repository root: python3 tools/prop_art.py
Preview: python3 tools/model_preview.py blocks embervoid:block/prop/<model> ... (the _item models show a two-block
prop whole)

Writes models/block/prop/<model>.json and textures/block/prop/<texture>.png. The blockstates, item models, loot and
tags come from datagen (PropDataGen); the hitboxes are in com.embervoid.block.prop.Props, checked against these models
by PropShapesTest.

Textures (0.5.0 art pass) follow docs/art-direction.md §1 on the shared ramps of tools/palette.py: planks with grain
and knots, basket-weave sackcloth, riveted iron with a specular edge, cloth with fold lines, clay with throwing rings.
The models and their UVs are unchanged (PropShapesTest checks the hitboxes against them).

Coordinates are pixels with the prop facing north (its front at z = 0). A two-block prop is built whole and split:
two tall ones (y 0..32) into _lower and _upper, two long ones (z 0..32, the head to the north) into _head and _foot,
plus an _item model of both halves centred on the block for the inventory. Faces get 1:1 UVs: by default the face's
own position in the block (like vanilla when a face has no uv), or an explicit origin where a texture has a picture
on it (a face, a mark, a wheel). Textures follow art_kit's look: ramp palettes, banded levels, value noise.
"""
import json
import math
import os
import random

from art_kit import ROOT, CLEAR, Canvas, hash01, mix, shade, value_noise
import palette as P
from palette import (bevel_edges, cellular, dither_fill, dither_level, moss_patch, rivet, shift, soot_stain, step,
                     wood_grain)

TEXTURES = os.path.join(ROOT, "textures", "block", "prop")
MODELS = os.path.join(ROOT, "models", "block", "prop")
FACES = ("north", "south", "east", "west", "up", "down")


# --- Models -----------------------------------------------------------------------------------------------------

def face_size(face, f, t):
    if face in ("north", "south"):
        return t[0] - f[0], t[1] - f[1]
    if face in ("east", "west"):
        return t[2] - f[2], t[1] - f[1]
    return t[0] - f[0], t[2] - f[2]


def auto_uv(face, f, t):
    """Vanilla's uv for a face without one: where the face lies in the block."""
    (x1, y1, z1), (x2, y2, z2) = f, t
    return {
        "north": (16 - x2, 16 - y2, 16 - x1, 16 - y1),
        "south": (x1, 16 - y2, x2, 16 - y1),
        "east": (16 - z2, 16 - y2, 16 - z1, 16 - y1),
        "west": (z1, 16 - y2, z2, 16 - y1),
        "up": (x1, z1, x2, z2),
        "down": (x1, 16 - z2, x2, 16 - z1),
    }[face]


class Element:
    def __init__(self, f, t, tex, faces, uv, rot, shade_on):
        self.f, self.t = list(f), list(t)
        self.tex = tex if isinstance(tex, dict) else {face: tex for face in faces}
        self.faces = [face for face in faces if face in self.tex]
        self.uv = dict(uv or {})
        self.rot = rot
        self.shade = shade_on

    def copy(self):
        e = Element(self.f, self.t, dict(self.tex), list(self.faces), self.uv, dict(self.rot) if self.rot else None,
                    self.shade)
        return e

    def shifted(self, axis, d):
        e = self.copy()
        e.f[axis] += d
        e.t[axis] += d
        if e.rot:
            o = list(e.rot["origin"])
            o[axis] += d
            e.rot["origin"] = o
        return e

    def bounds(self):
        """Axis-aligned bounds after the element's own rotation."""
        corners = [(x, y, z) for x in (self.f[0], self.t[0]) for y in (self.f[1], self.t[1]) for z in (self.f[2], self.t[2])]
        if self.rot:
            corners = [rotate_point(p, self.rot) for p in corners]
        lo = [min(p[i] for p in corners) for i in range(3)]
        hi = [max(p[i] for p in corners) for i in range(3)]
        return lo, hi


def rotate_point(p, rot):
    a = math.radians(rot["angle"])
    o = rot["origin"]
    x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
    c, s = math.cos(a), math.sin(a)
    if rot["axis"] == "x":
        y, z = y * c - z * s, y * s + z * c
    elif rot["axis"] == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return x + o[0], y + o[1], z + o[2]


# Which uv coordinate runs along a split axis, per face, and whether it starts at the high end of the axis.
UV_ALONG = {1: {"north": (1, True), "south": (1, True), "east": (1, True), "west": (1, True)},
            2: {"east": (0, True), "west": (0, False), "up": (1, False), "down": (1, True)}}


def full_uv(face, e):
    spec = e.uv.get(face)
    if spec is None:
        return auto_uv(face, e.f, e.t)
    if len(spec) == 2:
        w, h = face_size(face, e.f, e.t)
        return spec[0], spec[1], spec[0] + w, spec[1] + h
    return tuple(spec)


def cut_uv(face, e, axis):
    """An explicit uv cut at 16 along axis: (the part below, the part above)."""
    uv = list(full_uv(face, e))
    along = UV_ALONG[axis].get(face)
    if along is None:
        return tuple(uv), tuple(uv)
    k, from_high = along
    c1, c2 = e.f[axis], e.t[axis]
    t = (16 - c1) / (c2 - c1)
    if from_high:
        t = 1 - t
    mid = uv[k] + (uv[k + 2] - uv[k]) * t
    first, second = list(uv), list(uv)
    if from_high:
        first[k], second[k + 2] = mid, mid  # the low part is the far end of the uv
    else:
        first[k + 2], second[k] = mid, mid
    return tuple(first), tuple(second)


class Model:
    """Elements of one prop. tex is a texture key or {face: key}; uv maps a face to an origin (u, v), sized from the
    face, or to a full (u1, v1, u2, v2); rot is (axis, angle, origin)."""

    def __init__(self, textures, particle=None, ao=True):
        self.textures = dict(textures)
        self.particle = particle or next(iter(textures))
        self.ao = ao
        self.elements = []

    def box(self, f, t, tex, faces=FACES, uv=None, rot=None, shade_on=True):
        r = None
        if rot:
            axis, angle, origin = rot
            assert angle in (-45, -22.5, 0, 22.5, 45), angle
            r = {"origin": list(origin), "axis": axis, "angle": angle}
        self.elements.append(Element(f, t, tex, faces, uv, r, shade_on))

    def plane_z(self, x1, y1, x2, y2, z, tex, uv=None, rot=None):
        """A cloth or bars plane across z, seen from both sides."""
        self.box((x1, y1, z), (x2, y2, z), tex, ("north", "south"), uv, rot)

    def plane_x(self, z1, y1, z2, y2, x, tex, uv=None, rot=None):
        self.box((x, y1, z1), (x, y2, z2), tex, ("east", "west"), uv, rot)

    def bounds(self):
        bs = [e.bounds() for e in self.elements]
        return ([min(b[0][i] for b in bs) for i in range(3)], [max(b[1][i] for b in bs) for i in range(3)])

    def split(self, axis):
        """The two blocks of a prop built across axis 1 (y: lower, upper) or 2 (z: head to the north, foot)."""
        first, second = self.empty(), self.empty()
        cut_low, cut_high = ("down", "up") if axis == 1 else ("north", "south")
        for e in self.elements:
            if e.t[axis] <= 16:
                first.elements.append(e.copy())
            elif e.f[axis] >= 16:
                second.elements.append(e.shifted(axis, -16))
            else:
                assert not e.rot, "a rotated element crosses the split: %s" % e.f
                a, b = e.copy(), e.copy()
                for face in e.uv:
                    a.uv[face], b.uv[face] = cut_uv(face, e, axis)
                a.t[axis] = 16
                a.faces = [face for face in a.faces if face != cut_high]
                b.f[axis] = 16
                b = b.shifted(axis, -16)
                b.faces = [face for face in b.faces if face != cut_low]
                first.elements.append(a)
                second.elements.append(b)
        return first, second

    def empty(self):
        return Model(self.textures, self.particle, self.ao)

    def json(self, display=None):
        out = {"parent": "minecraft:block/block"}
        if not self.ao:
            out["ambientocclusion"] = False
        if display:
            out["display"] = display
        out["textures"] = {"particle": tex_id(self.textures[self.particle])}
        out["textures"].update({k: tex_id(v) for k, v in self.textures.items()})
        out["elements"] = [self.element_json(e) for e in self.elements]
        return out

    def element_json(self, e):
        out = {"from": [num(v) for v in e.f], "to": [num(v) for v in e.t]}
        if e.rot:
            out["rotation"] = {"origin": [num(v) for v in e.rot["origin"]], "axis": e.rot["axis"], "angle": e.rot["angle"]}
        if not e.shade:
            out["shade"] = False
        faces = {}
        for face in e.faces:
            uv = full_uv(face, e)
            assert all(-0.001 <= v <= 16.001 for v in uv), "uv outside the texture: %s %s %s" % (face, e.f, uv)
            fj = {"uv": [num(v) for v in uv], "texture": "#" + e.tex[face]}
            if face == "down" and e.f[1] == 0 and not e.rot:
                fj["cullface"] = "down"
            faces[face] = fj
        out["faces"] = faces
        return out


def merged(first, second, axis):
    """Both halves of a two-block prop as one model, centred on the block (-8..24 along axis) for the item."""
    m = first.empty()
    for half, d in ((first, -8), (second, 8)):
        for e in half.elements:
            frozen = e.copy()
            frozen.uv = {face: full_uv(face, e) for face in e.faces}
            m.elements.append(frozen.shifted(axis, d))
    return m


def tex_id(name):
    return name if ":" in name else "embervoid:block/prop/" + name


def num(v):
    v = round(v, 4)
    return int(v) if v == int(v) else v


# Two-block props in the inventory: both halves at about the size of one block.
ITEM_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.42, 0.42, 0.42]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.18, 0.18, 0.18]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.36, 0.36, 0.36]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.28, 0.28, 0.28]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.28, 0.28, 0.28]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.28, 0.28, 0.28]},
}


def write_model(name, model, display=None):
    data = model.json(display)
    # One element per line keeps the files short enough to read.
    lines = ["{"]
    keys = [k for k in data if k != "elements"]
    for k in keys:
        lines.append("\t%s: %s," % (json.dumps(k), json.dumps(data[k])))
    lines.append('\t"elements": [')
    lines.append(",\n".join("\t\t" + json.dumps(e) for e in data["elements"]))
    lines.append("\t]")
    lines.append("}")
    path = os.path.join(MODELS, name + ".json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write("\n".join(lines) + "\n")


def write_split(name, model, axis, parts):
    a, b = model.split(axis)
    write_model(name + "_" + parts[0], a)
    write_model(name + "_" + parts[1], b)
    write_model(name + "_item", merged(a, b, axis), ITEM_DISPLAY)


# --- Palettes ---------------------------------------------------------------------------------------------------
# All ramps come from tools/palette.py (the art direction's shared palette); HOT / WARM are the ember accents.

FIELD_WOOD = P.DEAD_WOOD
CHARRED = P.CHARRED_WOOD
COFFIN = P.CHARRED_WOOD
BURLAP = P.BURLAP
IRON = P.GRAVE_IRON
RUST = P.RUST
STEEL = P.STEEL
GRAVE = P.KERB_STONE
LICHEN = P.MOSS[1:4]
BONE = P.BONE
ASH = P.ASH_SOIL
TALLOW = P.TALLOW
CLOTH = P.CRIMSON_CLOTH
GILT = P.GOLD_TRIM
CLAY = P.CLAY
FELT = P.SOOT
STRAW = P.STRAW
HOT = P.EMBER[4]
WARM = P.EMBER[2]


def level(colors, v):
    return colors[max(0, min(len(colors) - 1, int(v * len(colors))))]


def paint(fn, colors, w=16, h=16):
    """A canvas from fn(x, y) -> level in 0..1 (None leaves the pixel clear), quantised with the ordered dither."""
    c = Canvas(w, h)
    for y in range(h):
        for x in range(w):
            v = fn(x, y)
            if v is not None:
                c.set(x, y, dither_level(colors, v * len(colors) - 0.5, x, y))
    return c


def planks(seed, colors, plank=4, vertical=False, base=3.4, knots=1, w=16, h=16, spread=1.0):
    """Boards with grain streaks, a joint per board and a knot (palette.wood_grain)."""
    c = Canvas(w, h)
    wood_grain(c, colors, seed, plank=plank, vertical=vertical, base=base, spread=spread, knots=knots)
    return c


def burlap(seed, base=3.2, w=16, h=16):
    """Sackcloth: a basket weave of 2 px threads, each over-thread lit on its upper-left, the weave sagging a little
    with noise; a loose thread or two."""
    c = Canvas(w, h)
    for y in range(h):
        for x in range(w):
            over = ((x // 2) + (y // 2)) % 2 == 0
            k = (x % 2, y % 2)
            v = base + (value_noise(x, y, seed, 4.0) - 0.5) * 1.2
            if over:
                v += 0.9 if k == (0, 0) else 0.3 if k[0] == 0 else -0.5
            else:
                v += 0.4 if k == (0, 0) else -0.2 if k[1] == 0 else -1.0
            c.set(x, y, dither_level(BURLAP, v, x, y))
    for k in range(2):
        x = int(hash01(k, 1, seed + 7) * w)
        for y in range(int(hash01(k, 2, seed + 7) * h), h):
            if hash01(x, y, seed + 8) < 0.7:
                c.set(x, y, step(BURLAP, 1))
    return c


def stone(seed, colors, base=3.3, points=9, w=16, h=16):
    c = Canvas(w, h)
    cellular(c, colors, seed, points=points, base=base, spread=1.2)
    return c


def lichen(c, rng, count):
    for _ in range(count):
        moss_patch(c, rng.randrange(1 << 16), rng.randrange(16), rng.randrange(16), 1 + rng.randrange(2),
                   colors=P.MOSS, density=0.85, base=2.2)


def soot(c, rng, rows, strength=40):
    """Darkens toward the last of rows = (first, last): ash creeping up from the ground."""
    soot_stain(c, rng.randrange(1 << 16), rows, strength=max(1, strength // 20))


def embers(c, rng, count, length=(2, 4)):
    for _ in range(count):
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(rng.randint(*length)):
            c.set(x, y, HOT if rng.random() < 0.5 else WARM)
            y += 1
            x += rng.choice((-1, 0, 1))


def tear(c, rng, bottom, depth=3, holes=0):
    """Ragged bottom edge from row bottom down, plus burnt-through holes with a scorched rim."""
    for x in range(16):
        d = rng.randint(0, depth)
        for y in range(bottom - d, 16):
            c.set(x, y, CLEAR)
        if c.get(x, bottom - d - 1)[3]:
            c.set(x, bottom - d - 1, shade(c.get(x, bottom - d - 1), -35))
    for _ in range(holes):
        x, y = rng.randrange(1, 15), rng.randrange(1, bottom - 3)
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            if rng.random() < 0.85:
                c.set(x + dx, y + dy, CLEAR)
        for dx in range(-1, 3):
            for dy in range(-1, 3):
                p = c.get(x + dx, y + dy)
                if p[3]:
                    c.set(x + dx, y + dy, shade(p, -28))


def folds(c, seed, colors, count=3, w=16, h=16):
    """Cloth folds: 1-2 px darker lines that hang down and drift, a lit ridge beside each."""
    for k in range(count):
        x = int(hash01(k, 1, seed) * w)
        for y in range(h):
            if c.get(x % w, y)[3]:
                shift(c, colors, x % w, y, -2)
                shift(c, colors, (x + 1) % w, y, 1)
            if hash01(x, y, seed + k) < 0.2:
                x += 1 if hash01(x, y, seed + 9) < 0.5 else -1


def iron_plate(seed, rivets=True, rust=3, w=16, h=16):
    """A rusted iron plate: flat face with a specular line on its lit edges, rivets, rust blooms from the bottom."""
    c = Canvas(w, h)
    dither_fill(c, IRON, lambda x, y: 3.0 + (value_noise(x, y, seed, 3.0) - 0.5) * 1.0, 0, 0, w, h)
    bevel_edges(c, 0, 0, w, h, colors=IRON)
    for x in range(1, w - 1, 3):
        c.set(x, 0, P.GRAVE_IRON_SPECULAR)
    if rivets:
        for (x, y) in ((2, 2), (w - 4, 2), (2, h - 4), (w - 4, h - 4)):
            rivet(c, x, y, IRON)
    for k in range(rust):
        x, y = int(hash01(k, 1, seed + 3) * w), h - 1 - int(hash01(k, 2, seed + 3) * (h // 2))
        for dy in range(-1, 2):
            for dx in range(-2, 3):
                if hash01(x + dx, y + dy, seed + 4) < 0.7:
                    c.set((x + dx) % w, max(0, min(h - 1, y + dy)), step(RUST, 1 + int(hash01(x + dx, y + dy, seed + 5) * 3)))
    return c


# --- Textures ---------------------------------------------------------------------------------------------------

def textures(rng):
    t = {}

    t["sack"] = burlap(11)
    sack = burlap(11)
    # A faded grain-tithe mark: a wheat sheaf under a crown, stencilled in soot.
    ink = step(P.SOOT, 3)
    for x, y in ((7, 1), (9, 1), (11, 1), (7, 2), (8, 2), (9, 2), (10, 2), (11, 2)):
        sack.set(x, y + 5, mix(sack.get(x, y + 5), ink, 0.8))
    for y in range(4, 9):
        sack.set(9, y + 4, mix(sack.get(9, y + 4), ink, 0.8))
    for x, y in ((8, 10), (10, 10), (7, 9), (11, 9), (8, 12), (10, 12)):
        sack.set(x, y - 1, mix(sack.get(x, y - 1), ink, 0.75))
    t["sack_marked"] = sack
    t["cord"] = paint(lambda x, y: 0.35 + 0.4 * ((x + y) % 3 == 0) + (hash01(x, y, 4) - 0.5) * 0.2, STRAW)

    staves = planks(21, CHARRED, plank=4, vertical=True, base=3.3, knots=0)
    soot(staves, rng, (0, 4), 25)
    embers(staves, rng, 1, (2, 3))
    t["barrel_side"] = staves
    top = stone(22, ASH, base=3.0, points=12)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 5.6:
                top.set(x, y, dither_level(CHARRED, 2.0 + (hash01(x, y, 5) - 0.5) * 1.6, x, y))
            elif d < 2.2 and hash01(x, y, 6) < 0.45:
                top.set(x, y, HOT if hash01(x, y, 7) < 0.4 else WARM)
    t["barrel_top"] = top

    t["rusted_iron"] = iron_plate(31)

    t["cart_wood"] = planks(41, FIELD_WOOD, plank=5, vertical=False, base=3.6)
    wheel = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            rim = 4.6 < d < 6.2
            spoke = d < 4.8 and min(abs(math.sin(ang * 3)), 1) < 0.9 / max(d, 0.6) * 1.6
            hub = d < 1.6
            if rim or spoke or hub:
                v = 3.6 + (hash01(x, y, 42) - 0.5) * 1.4 - (1.2 if d > 5.6 else 0) + (1.0 if dx + dy < -2 and rim else 0)
                wheel.set(x, y, dither_level(FIELD_WOOD, v, x, y))
            if 5.7 < d < 6.3 and hash01(x, y, 43) < 0.6:
                wheel.set(x, y, dither_level(IRON, 2.5 + hash01(x, y, 44) * 2, x, y))
            if hub:
                wheel.set(x, y, dither_level(IRON, 2.2 + hash01(x, y, 45) * 2 + (1.5 if dx + dy < -0.5 else 0), x, y))
    for x, y in ((12, 3), (13, 4), (13, 5)):
        wheel.set(x, y, CLEAR)
    t["cart_wheel"] = wheel

    t["post_wood"] = planks(51, FIELD_WOOD, plank=16, vertical=True, base=3.3, knots=1)
    face = burlap(52, base=3.8)
    stitch = step(P.SOOT, 2)
    for x, y in ((6, 6), (8, 6), (7, 7), (6, 8), (8, 8), (11, 6), (13, 6), (12, 7), (11, 8), (13, 8)):
        face.set(x - 1, y - 1, stitch)  # X eyes
    for x in range(6, 12):
        face.set(x - 1, 10 - (1 if x in (6, 11) else 0), stitch)  # a stitched grin
        if x % 2 == 0:
            face.set(x - 1, 11 - (1 if x in (6, 11) else 0), shade(stitch, 20))
    t["scarecrow_face"] = face
    coat = paint(lambda x, y: 0.45 + (value_noise(x, y, 53, 3) - 0.5) * 0.3, CLOTH)
    for x in range(16):
        for y in range(16):
            coat.set(x, y, mix(coat.get(x, y), BURLAP[1], 0.4))
    folds(coat, 53, CLOTH, count=3)
    for y in range(2, 14, 3):
        coat.set(8, y, GILT[2])  # tarnished buttons
        coat.set(9, y + 1, step(CLOTH, 0))
    for x in range(10, 14):  # a patch
        for y in range(3, 7):
            coat.set(x, y, dither_level(BURLAP, 2.6 + hash01(x, y, 54) * 1.6, x, y))
    t["scarecrow_coat"] = coat
    rag = paint(lambda x, y: 0.42 + (value_noise(x, y, 55, 3) - 0.5) * 0.3, CLOTH)
    for x in range(16):
        for y in range(16):
            rag.set(x, y, mix(rag.get(x, y), BURLAP[1], 0.4))
    folds(rag, 55, CLOTH, count=4)
    tear(rag, rng, 15, depth=8, holes=2)
    t["scarecrow_rag"] = rag
    t["straw"] = paint(lambda x, y: 0.5 + (hash01(x, y // 3, 61) - 0.5) * 0.8, STRAW)
    t["felt"] = paint(lambda x, y: 0.5 + (value_noise(x, y, 62, 3) - 0.5) * 0.4, FELT)

    t["coffin_wood"] = planks(71, COFFIN, plank=6, vertical=False, base=3.6, knots=1)
    lid = planks(72, COFFIN, plank=4, vertical=True, base=3.8, knots=0)
    bevel_edges(lid, 0, 0, 16, 16, colors=COFFIN)
    t["coffin_lid"] = lid
    t["coffin_inside"] = paint(lambda x, y: 0.1 + hash01(x, y, 73) * 0.2, COFFIN)

    grave = stone(81, GRAVE, base=3.4, points=6)
    lichen(grave, rng, 3)
    t["grave_stone"] = grave
    carved = Canvas(16, 16)
    dither_fill(carved, GRAVE, lambda x, y: 3.6 + (value_noise(x, y, 82, 3.5) - 0.5) * 0.8)
    bevel_edges(carved, 0, 0, 16, 16, colors=GRAVE)
    groove = GRAVE[0]
    # A cross over three worn lines of an inscription, each groove lit along its lower-right.
    for y in range(1, 6):
        carved.set(4, y, groove)
        shift(carved, GRAVE, 5, y, 1)
    for x in range(2, 7):
        carved.set(x, 2, groove)
        shift(carved, GRAVE, x, 3, 1) if x != 4 else None
    for row, (x0, x1) in enumerate(((1, 8), (2, 7), (1, 6))):
        for x in range(x0, x1):
            if hash01(x, row, 83) < 0.7:
                carved.set(x, 7 + row * 2, groove)
    lichen(carved, rng, 2)
    soot(carved, rng, (12, 15), 25)
    t["grave_carved"] = carved
    earth = stone(84, ASH, base=3.0, points=20)
    for _ in range(8):
        earth.set(rng.randrange(16), rng.randrange(16), rng.choice(STRAW[1:4]))
    t["grave_earth"] = earth

    bone = Canvas(16, 16)
    dither_fill(bone, BONE, lambda x, y: 3.8 + (value_noise(x * 0.4, y, 91, 2) - 0.5) * 1.6)
    for y in range(16):
        shift(bone, BONE, 0, y, 1)
        shift(bone, BONE, 15, y, -2)
    t["bone"] = bone
    skull = Canvas(16, 16)
    dither_fill(skull, BONE, lambda x, y: 4.0 + (value_noise(x, y, 92, 2.5) - 0.5) * 1.4 - (1.5 if x + y > 20 else 0))
    hole = step(P.SOOT, 1)
    for x, y in ((1, 1), (2, 1), (1, 2), (2, 2), (5, 1), (6, 1), (5, 2), (6, 2), (3, 3), (4, 3)):
        skull.set(x + 4, y + 4, hole)  # sockets and the nose, at (4..11, 4..8)
    for x in range(5, 11):
        skull.set(x, 9, hole if x % 2 else BONE[1])
    # A suture across the brow.
    for i in range(6):
        skull.set(5 + i, 2 + (i % 2), BONE[1])
    t["skull"] = skull
    ash = stone(93, ASH, base=3.0, points=20)
    for _ in range(4):
        ash.set(rng.randrange(16), rng.randrange(16), BONE[3])
    t["ash_heap"] = ash

    tallow = Canvas(16, 16)
    dither_fill(tallow, TALLOW, lambda x, y: 3.8 + (value_noise(x, y * 0.4, 101, 2) - 0.5) * 1.4)
    for y in range(16):
        shift(tallow, TALLOW, 0, y, 1)
        shift(tallow, TALLOW, 15, y, -2)
    for x in range(16):
        if hash01(x, 0, 102) < 0.4:  # runs of wax
            for y in range(rng.randint(2, 6)):
                tallow.set(x, y, TALLOW[6])
                shift(tallow, TALLOW, x + 1, y, -1)
    t["tallow"] = tallow
    t["wick"] = paint(lambda x, y: 0.1 + hash01(x, y, 103) * 0.2, CHARRED)
    t["wick_lit"] = paint(lambda x, y: 0.5 + hash01(x, y, 104) * 0.5, P.EMBER[2:])

    t["rack_wood"] = planks(111, CHARRED, plank=8, vertical=True, base=3.6, knots=0)
    steel = Canvas(16, 16)
    dither_fill(steel, STEEL, lambda x, y: 3.2 + (value_noise(x * 0.3, y, 112, 2) - 0.5) * 2.2)
    for x in range(16):
        steel.set(x, 0, STEEL[6])
    t["steel"] = steel
    t["leather"] = paint(lambda x, y: 0.4 + 0.3 * (y % 2) + (hash01(x, y, 113) - 0.5) * 0.2, P.CLAY[:4])

    bars = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            if x % 3 == 1 or y in (0, 15):
                v = 2.6 + (hash01(x, y, 121) - 0.5) * 1.0 + (1.6 if y == 0 or (x % 3 == 1 and y % 4 == 1) else 0)
                bars.set(x, y, dither_level(IRON, v, x, y) if hash01(x, y, 122) > 0.12 else RUST[2])
            elif y in (7, 8) and x % 3 == 0:
                bars.set(x, y, step(IRON, 2))
    t["cage_bars"] = bars

    for name, rows in (("standard_cloth_top", None), ("standard_cloth_bottom", 7)):
        cloth = paint(lambda x, y: 0.55 + (value_noise(x, y, 131 if rows is None else 132, 3) - 0.5) * 0.3
                      - (0.15 if x in (0, 10) else 0), CLOTH, 11, 16)
        folds(cloth, 131 if rows is None else 132, CLOTH, count=2, w=11)
        if rows is None:
            # Gold trim at the top and the Greedy King's crown holding a flame.
            for x in range(11):
                cloth.set(x, 0, GILT[5])
                cloth.set(x, 1, GILT[2])
            crown = ["...........", "..#.#.#....", "..#####....", "..#####....", "...........",
                     "....o......", "...ooo.....", "...o*o.....", "....o......"]
            for y, row in enumerate(crown):
                for x, ch in enumerate(row):
                    if ch == "#":
                        cloth.set(x + 1, y + 2, GILT[5] if hash01(x, y, 133) > 0.3 else GILT[3])
                    elif ch == "o":
                        cloth.set(x + 1, y + 2, P.EMBER[3])
                    elif ch == "*":
                        cloth.set(x + 1, y + 2, P.EMBER[5])
            for x in range(11):
                for y in range(11, 16):
                    if hash01(x, y, 134) < 0.08:
                        cloth.set(x, y, CLEAR)
        else:
            img = Canvas(16, 16)
            for x in range(11):
                for y in range(16):
                    img.set(x, y, cloth.get(x, y))
            tear(img, rng, rows, depth=4, holes=1)
            cloth = img
        full = Canvas(16, 16)
        for x in range(16):
            for y in range(16):
                full.set(x, y, cloth.get(x, y))
        t[name] = full
    gilt = Canvas(16, 16)
    dither_fill(gilt, GILT, lambda x, y: 3.8 + (value_noise(x, y, 135, 3) - 0.5) * 1.2)
    bevel_edges(gilt, 0, 0, 16, 16, colors=GILT)
    t["gilt"] = gilt

    def thrown(seed, base=3.4):
        """Thrown clay: horizontal throwing rings, one in three a step lighter, a chip or two."""
        c = Canvas(16, 16)
        dither_fill(c, CLAY, lambda x, y: base + (value_noise(x, y, seed, 3) - 0.5) * 0.8 + (0.8 if y % 3 == 0 else -0.3 if y % 3 == 2 else 0))
        return c

    clay = thrown(141)
    for _ in range(3):  # chips
        x, y = rng.randrange(16), rng.randrange(16)
        clay.set(x, y, CLAY[6])
        clay.set(x + 1, y + 1, CLAY[0])
    t["urn_clay"] = clay
    band = thrown(144)
    for x in range(16):
        band.set(x, 3, step(P.SOOT, 1))
        band.set(x, 4, step(P.SOOT, 3))
        if x % 3 == 1:
            band.set(x, 6, step(P.SOOT, 2))
    t["urn_band"] = band
    mouth = thrown(142)
    for y in range(16):
        for x in range(16):
            if math.hypot(x - 7.5, y - 7.5) < 3.2:
                mouth.set(x, y, dither_level(ASH, 2.4 + hash01(x, y, 143) * 1.4, x, y))
    t["urn_mouth"] = mouth
    return t


# --- Prop builders ----------------------------------------------------------------------------------------------

def sack(m, x0, y0, z0, along="x", flip=False, marked=False):
    """A sack lying on its side, 12.5 long, 6 high, 7 deep, tied off at one end."""
    length = 12.5

    def put(u1, v1, w1, u2, v2, w2, tex, uv=None):
        if flip:
            u1, u2 = length - u2, length - u1
        if along == "x":
            m.box((x0 + u1, y0 + v1, z0 + w1), (x0 + u2, y0 + v2, z0 + w2), tex, uv=uv)
        else:
            m.box((x0 + w1, y0 + v1, z0 + u1), (x0 + w2, y0 + v2, z0 + u2), tex, uv=uv)

    put(0, 1, 1, 1, 4, 6, "sack")
    put(1, 0, 0, 9, 5, 7, "sack", {"north": (3.5, 5.5)} if marked else None)
    put(2, 5, 1, 8, 6, 6, "sack")
    put(9, 0.5, 0.5, 10, 4.5, 6.5, "sack")
    put(10, 1.5, 2.5, 11, 3.5, 4.5, "cord")
    put(11, 1, 2, 12.5, 4, 5, "sack")


def sacks(count):
    m = Model({"sack": "sack", "sack_marked": "sack_marked", "cord": "cord"}, "sack")
    if count == 1:
        sack(m, 1.5, 0, 4.5, marked=True)
    else:
        sack(m, 1.5, 0, 1, marked=True)
        sack(m, 2, 0, 8.5, flip=True)
    if count == 3:
        sack(m, 4.5, 5.5, 1.5, along="z")
    return m


def burnt_barrel():
    m = Model({"side": "barrel_side", "top": "barrel_top", "iron": "rusted_iron"}, "side")
    side = {f: "side" for f in FACES}
    m.box((3, 0, 3), (13, 3, 13), dict(side, up="top"))
    m.box((2, 3, 2), (14, 10, 14), side)
    m.box((3, 10, 3), (13, 12.5, 13), dict(side, up="top"))
    # Hoops, the middle one sprung loose on one side.
    m.box((2.75, 1.5, 2.75), (13.25, 2.5, 13.25), "iron", ("north", "south", "east", "west"))
    m.box((1.75, 6, 1.75), (14.25, 7, 14.25), "iron", ("north", "south", "east", "west", "down"))
    m.box((2.75, 10.5, 2.75), (13.25, 11.5, 13.25), "iron", ("north", "south", "east", "west"))
    # What the fire left of the rim: stave stubs at uneven heights.
    m.box((3, 12.5, 3), (9, 14, 4), "side")
    m.box((12, 12.5, 5), (13, 15, 13), "side")
    m.box((3, 12.5, 3), (4, 13.5, 8), "side")
    m.box((6, 12.5, 12), (12, 13.5, 13), "side")
    return m


def broken_cart():
    m = Model({"wood": "cart_wood", "wheel": "cart_wheel", "iron": "rusted_iron", "sack": "sack", "cord": "cord"},
              "wood")
    m.box((2, 7, 6), (14, 8, 30), "wood")                    # bed
    m.box((4, 6, 6), (5, 7, 30), "wood")                     # runners
    m.box((11, 6, 6), (12, 7, 30), "wood")
    m.box((2, 8, 6), (3, 12, 30), "wood")                    # west board
    m.box((13, 8, 6), (14, 12, 21), "wood")                  # east board, broken off
    m.box((13, 8, 21), (14, 9.5, 30), "wood")
    m.box((3, 8, 6), (13, 11, 7), "wood")                    # front board
    m.box((0.5, 5.5, 23.5), (15.5, 6.5, 24.5), "iron")       # axle
    for x in (1, 15):
        m.plane_x(18, 0, 30, 12, x, "wheel", {"east": (2, 2), "west": (2, 2)})
    m.box((0, 5, 23), (1, 7, 25), "iron")                    # hubs
    m.box((15, 5, 23), (16, 7, 25), "iron")
    # Shafts sloping to the ground, joined by a yoke bar.
    rot = ("x", 22.5, (8, 7.5, 6))
    m.box((4.5, 7, 0), (5.5, 8, 6), "wood", rot=rot)
    m.box((10.5, 7, 0), (11.5, 8, 6), "wood", rot=rot)
    m.box((4.5, 7, 0.5), (11.5, 8, 1.5), "wood", rot=rot)
    sack(m, 4, 8, 9, along="z")
    return m


def scarecrow():
    m = Model({"post": "post_wood", "coat": "scarecrow_coat", "rag": "scarecrow_rag", "face": "scarecrow_face",
               "sack": "sack", "straw": "straw", "felt": "felt", "cord": "cord"}, "post")
    m.box((7, 0, 7), (9, 27, 9), "post")
    m.box((0.5, 19, 7.25), (15.5, 20.5, 8.75), "post")      # crossbar
    m.box((4.5, 12, 5.5), (11.5, 21, 10.5), "coat")          # coat
    m.plane_z(4.5, 8, 11.5, 12, 5.25, "rag", {"north": (4.5, 8), "south": (4.5, 8)})
    m.plane_z(4.5, 8, 11.5, 12, 10.75, "rag", {"north": (2, 8), "south": (2, 8)})
    m.plane_x(5.5, 8, 10.5, 12, 4.25, "rag", {"east": (9, 8), "west": (9, 8)})
    m.plane_x(5.5, 8, 10.5, 12, 11.75, "rag", {"east": (6, 8), "west": (6, 8)})
    m.box((1, 18, 6.5), (4.5, 21, 9.5), "coat")              # sleeves
    m.box((11.5, 18, 6.5), (15, 21, 9.5), "coat")
    m.plane_z(1, 15, 4.5, 18, 8, "rag", {"north": (1, 9), "south": (1, 9)})
    m.plane_z(11.5, 15, 15, 18, 8, "rag", {"north": (11, 9), "south": (11, 9)})
    m.box((0, 18.5, 7), (1, 20.5, 9), "straw")               # straw hands
    m.box((15, 18.5, 7), (16, 20.5, 9), "straw")
    m.box((6.5, 21, 6.5), (9.5, 22, 9.5), "cord")            # tied neck
    m.box((5, 22, 5), (11, 28, 11), dict({f: "sack" for f in FACES}, north="face"), uv={"north": (5, 5)})
    m.box((3.5, 27.5, 3.5), (12.5, 28.5, 12.5), "felt")      # hat
    m.box((5.5, 28.5, 5.5), (10.5, 31, 10.5), "felt")
    m.box((6.5, 31, 6), (9.5, 32, 9), "felt")
    m.box((4.5, 23, 4.5), (5, 26.5, 6), "straw")             # straw under the hat
    m.box((11, 22.5, 9), (11.5, 25, 10.5), "straw")
    return m


def coffin():
    m = Model({"wood": "coffin_wood", "lid": "coffin_lid", "inside": "coffin_inside", "iron": "rusted_iron",
               "bone": "bone"}, "wood")
    body = {f: "wood" for f in FACES}
    body["up"] = "inside"
    lid = {f: "wood" for f in FACES}
    lid["up"] = "lid"
    steps = ((1, 3, 5, 11), (3, 9, 3, 13), (9, 24, 4, 12), (24, 31, 5, 11))
    for z1, z2, x1, x2 in steps:
        m.box((x1, 0, z1), (x2, 6, z2), body)
    # The lid, pushed aside: a dark gap along the west edge.
    for z1, z2, x1, x2 in steps:
        m.box((x1 + 1, 6, z1 - 0.5), (x2 + 1.5, 7.5, z2 + 0.5), lid)
    m.box((9, 7.5, 6), (10, 8, 20), "iron")                  # iron cross on the lid
    m.box((6.5, 7.5, 9), (12.5, 8, 10), "iron")
    m.box((4, 5.5, 12), (5.5, 6.5, 15), "bone")              # fingers out of the gap
    m.box((3.5, 5.5, 12.5), (4, 6, 14), "bone")
    return m


def grave_cross():
    m = Model({"stone": "grave_stone", "carved": "grave_carved"}, "stone")
    m.box((3, 0, 5), (13, 2, 11), "stone")
    m.box((4, 2, 6), (12, 3, 10), "stone")
    m.box((6.5, 3, 6.5), (9.5, 15.5, 9.5), dict({f: "stone" for f in FACES}, north="carved"), uv={"north": (3, 1)})
    m.box((2.5, 10, 6.5), (6.5, 13, 9.5), "stone")           # west arm
    m.box((9.5, 10, 6.5), (12, 13, 9.5), "stone")            # east arm, its tip broken off
    m.box((9.5, 2, 9), (12, 3, 11), "stone", rot=("y", 22.5, (10.5, 2, 10)))
    return m


def leaning_gravestone():
    m = Model({"stone": "grave_stone", "carved": "grave_carved", "earth": "grave_earth"}, "stone")
    m.box((4, 0, 0.5), (12, 1.5, 6.5), "earth")              # the grave mound
    m.box((5, 1.5, 1.5), (11, 2, 5.5), "earth")
    m.box((3, 0, 6.5), (13, 1, 11), "stone")                 # sunken foot
    lean = ("x", 22.5, (8, 0, 10))
    m.box((3.5, 0, 7.5), (12.5, 12, 10), dict({f: "stone" for f in FACES}, north="carved"), uv={"north": (3.5, 2)},
          rot=lean)
    m.box((4.5, 12, 7.5), (11.5, 13.5, 10), "stone", rot=lean)
    m.box((6, 13.5, 7.5), (10, 14.5, 10), "stone", rot=lean)
    return m


def bone(m, x1, x2, y, z, rot=None):
    """A long bone along x with knobbed ends."""
    m.box((x1 + 1, y + 0.25, z - 0.5), (x2 - 1, y + 1.25, z + 0.5), "bone", rot=rot)
    m.box((x1, y, z - 1), (x1 + 1, y + 1.5, z + 1), "bone", rot=rot)
    m.box((x2 - 1, y, z - 1), (x2, y + 1.5, z + 1), "bone", rot=rot)


def skull(m, x, y, z, rot=None):
    """A skull 4.5 wide facing north, its front at z."""
    tex = {f: "bone" for f in FACES}
    tex["north"] = "skull"
    m.box((x, y + 0.5, z), (x + 4.5, y + 4.5, z + 4), tex, uv={"north": (5.5, 3.5)}, rot=rot)
    m.box((x + 0.5, y + 1, z + 4), (x + 4, y + 4, z + 5), "bone", rot=rot)
    m.box((x + 0.5, y, z - 0.5), (x + 4, y + 1, z + 2.5), tex, uv={"north": (5.5, 8.5)}, rot=rot)


def bone_pile():
    m = Model({"ash": "ash_heap", "bone": "bone", "skull": "skull"}, "bone")
    m.box((2, 0, 2), (14, 1, 14), "ash")
    m.box((4, 1, 4), (12, 1.5, 12), "ash")
    skull(m, 5, 1.5, 3.5, ("y", 22.5, (7.25, 1.5, 5.5)))
    bone(m, 3, 12, 1, 11, ("y", 22.5, (7.5, 1, 11)))
    bone(m, 6, 14, 1.5, 9, ("y", -45, (10, 1.5, 9)))
    bone(m, 2.5, 9.5, 1, 7, ("y", -22.5, (6, 1, 7)))
    bone(m, 5, 12, 2.5, 10, ("y", 45, (8.5, 2.5, 10)))
    return m


CANDLES = {
    1: [(8, 8, 8, 3)],
    2: [(6.5, 7, 8, 3), (10.5, 9.5, 5, 2)],
    3: [(6.5, 6.5, 8, 3), (10.5, 8, 6, 2), (7, 11, 4, 2)],
    4: [(6, 6, 8, 3), (10, 6.5, 6, 2), (6.5, 10.5, 4, 2), (10.5, 10.5, 5, 2)],
}


def tallow_candles(count, lit):
    """Candles (x, z, height, width): fat tallow ones that ran down into a puddle at the foot."""
    m = Model({"tallow": "tallow", "wick": "wick_lit" if lit else "wick"}, "tallow", ao=False)
    for x, z, h, w in CANDLES[count]:
        r = w / 2
        m.box((x - r, 0, z - r), (x + r, h, z + r), "tallow")
        m.box((x - r - 0.5, 0, z - r - 0.5), (x + r + 0.5, 1, z + r + 0.5), "tallow")
        m.box((x + r, 1, z - 0.5), (x + r + 0.5, 1 + h * 0.4, z + 0.5), "tallow")
        m.box((x - 0.5, h, z - 0.5), (x + 0.5, h + 1.5, z + 0.5), "wick", shade_on=not lit)
    return m


def weapon_rack():
    m = Model({"wood": "rack_wood", "steel": "steel", "leather": "leather", "iron": "rusted_iron"}, "wood")
    m.box((1, 0, 3), (15, 1.5, 11), "wood")
    m.box((1, 1.5, 8.5), (2.5, 15, 10), "wood")
    m.box((13.5, 1.5, 8.5), (15, 15, 10), "wood")
    m.box((2.5, 12.5, 8.5), (13.5, 14, 10), "wood")
    m.box((2.5, 4, 8), (13.5, 5, 10), "wood")
    lean = lambda x: ("x", 22.5, (x, 1.5, 4))
    # A sword point down.
    m.box((4.5, 1.5, 3.75), (5.5, 11, 4.25), "steel", rot=lean(5))
    m.box((3, 11, 3.5), (7, 12, 4.5), "iron", rot=lean(5))
    m.box((4.5, 12, 3.6), (5.5, 14.5, 4.4), "leather", rot=lean(5))
    m.box((4.25, 14.5, 3.5), (5.75, 15.5, 4.5), "iron", rot=lean(5))
    # A spear.
    m.box((7.5, 1.5, 3.5), (8.5, 13, 4.5), "wood", rot=lean(8))
    m.box((7.25, 13, 3.75), (8.75, 15, 4.25), "steel", rot=lean(8))
    m.box((7.75, 15, 3.75), (8.25, 16, 4.25), "steel", rot=lean(8))
    # An axe.
    m.box((10.5, 1.5, 3.5), (11.5, 13, 4.5), "wood", rot=lean(11))
    m.box((11.5, 9.5, 3.75), (13.5, 12.5, 4.25), "steel", rot=lean(11))
    m.box((13.5, 9, 3.75), (14, 13, 4.25), "steel", rot=lean(11))
    return m


def iron_cage(hanging):
    m = Model({"iron": "rusted_iron", "bars": "cage_bars", "bone": "bone", "skull": "skull",
               "chain": "minecraft:block/chain"}, "iron", ao=False)
    m.box((3, 0, 3), (13, 1, 13), "iron")
    m.box((3, 12, 3), (13, 13, 13), "iron")
    m.box((5, 13, 5), (11, 14, 11), "iron")
    for x in (3, 12):
        for z in (3, 12):
            m.box((x, 1, z), (x + 1, 12, z + 1), "iron")
    full = (2, 1)
    m.plane_z(4, 1, 12, 12, 3.5, "bars", {"north": full, "south": full})
    m.plane_z(4, 1, 12, 12, 12.5, "bars", {"north": full, "south": full})
    m.plane_x(4, 1, 12, 12, 3.5, "bars", {"east": full, "west": full})
    m.plane_x(4, 1, 12, 12, 12.5, "bars", {"east": full, "west": full})
    m.box((7, 5, 2.75), (9, 7.5, 3.25), "iron")              # lock
    skull(m, 6, 1, 6, ("y", -22.5, (8, 1, 8)))
    bone(m, 4.5, 11, 1, 11, ("y", 22.5, (8, 1, 11)))
    if hanging:
        m.box((6.5, 14, 8), (9.5, 16, 8), "chain", ("north", "south"), {"north": (3, 0, 0, 2), "south": (0, 0, 3, 2)},
              ("y", 45, (8, 8, 8)), shade_on=False)
        m.box((8, 14, 6.5), (8, 16, 9.5), "chain", ("east", "west"), {"east": (3, 0, 6, 2), "west": (6, 0, 3, 2)},
              ("y", 45, (8, 8, 8)), shade_on=False)
    else:
        m.box((7, 14, 7.5), (9, 15.5, 8.5), "iron")           # handle
    return m


def tattered_standard():
    m = Model({"post": "post_wood", "top": "standard_cloth_top", "bottom": "standard_cloth_bottom", "gilt": "gilt",
               "stone": "embervoid:block/polished_ash_stone"}, "post", ao=False)
    m.box((5, 0, 5), (11, 2, 11), "stone")
    m.box((6, 2, 6), (10, 3, 10), "stone")
    m.box((7.5, 3, 8), (8.5, 30, 9), "post")
    m.box((2, 27, 7), (14, 28, 8), "post")
    m.box((1, 26.75, 6.75), (2, 28.25, 8.25), "gilt")
    m.box((14, 26.75, 6.75), (15, 28.25, 8.25), "gilt")
    m.box((7.25, 30, 7.75), (8.75, 31, 9.25), "gilt")
    m.box((7.75, 31, 8.25), (8.25, 32, 8.75), "gilt")
    m.plane_z(2.5, 16, 13.5, 27, 6.75, "top", {"north": (0, 0), "south": (0, 0)})
    m.plane_z(2.5, 9, 13.5, 16, 6.75, "bottom", {"north": (0, 0), "south": (0, 0)}, ("x", 22.5, (8, 16, 6.75)))
    return m


def urn(m, x, z, small=False, lying=False):
    """A funerary urn standing at (x, z), or lying with its mouth to the east and its ash spilled."""
    parts = ([(1, 0, 1), (2, 1, 4), (1.5, 4, 5), (1, 5, 5.5), (1.5, 5.5, 6.5)] if small else
             [(1.5, 0, 1), (3, 1, 6), (2.5, 6, 7), (1.5, 7, 8), (2, 8, 9)])
    for i, (r, y1, y2) in enumerate(parts):
        top = i == len(parts) - 1
        # The painted band runs round a standing urn only (on a lying one it would turn into stripes).
        sides = () if lying else ("north", "south", "east", "west")
        tex = {f: "band" if i == 1 and f in sides else "clay" for f in FACES}
        if top:
            tex["east" if lying else "up"] = "mouth"
        uv = {f: (8 - r, 1) for f in sides} if i == 1 and sides else None
        if lying:
            # Height runs along +x; the urn rests on its belly.
            belly = parts[1][0]
            m.box((x + y1, belly - r, z - r), (x + y2, belly + r, z + r), tex, uv=uv)
        else:
            m.box((x - r, y1, z - r), (x + r, y2, z + r), tex, uv=uv)
    if not small and not lying:
        m.box((x - 1.75, 9, z - 1.75), (x + 1.75, 9.5, z + 1.75), "clay")
        m.box((x - 0.5, 9.5, z - 0.5), (x + 0.5, 10.5, z + 0.5), "clay")


def ash_urns(count):
    m = Model({"clay": "urn_clay", "band": "urn_band", "mouth": "urn_mouth", "ash": "ash_heap"}, "clay")
    if count == 1:
        urn(m, 8, 8)
    elif count == 2:
        urn(m, 6, 7)
        urn(m, 11.5, 10.5, small=True)
    else:
        urn(m, 5.5, 5.5)
        urn(m, 11.5, 6, small=True)
        urn(m, 4, 12, small=True, lying=True)
        m.box((10.5, 0, 10.5), (13.5, 0.5, 14), "ash")
        m.box((11, 0.5, 11), (12.5, 1, 13), "ash")
    return m


def rubble_pile():
    m = Model({"ash": "ash_heap", "bricks": "embervoid:block/ash_bricks", "cracked": "embervoid:block/cracked_ash_bricks",
               "cobble": "embervoid:block/cobbled_ash_stone", "log": "embervoid:block/dead_log"}, "cobble")
    m.box((1, 0, 2), (14, 1, 15), "ash")
    m.box((2.5, 0, 3), (8.5, 4, 8), "bricks", rot=("y", 22.5, (5.5, 0, 5.5)))
    m.box((8, 0, 6.5), (13.5, 3, 12.5), "cracked", rot=("y", -22.5, (10.75, 0, 9.5)))
    m.box((5, 3, 5.5), (10, 6, 10.5), "cobble", rot=("y", 45, (7.5, 3, 8)))
    m.box((11, 0, 2), (13.5, 1.5, 4.5), "cobble")
    m.box((2, 0, 10.5), (4.5, 1, 13), "cobble")
    m.box((1.5, 1, 11), (14.5, 3, 13), "log", rot=("y", 22.5, (8, 1, 12)))
    return m


def main():
    rng = random.Random(2003)
    for name, img in textures(rng).items():
        img.save(os.path.join(TEXTURES, name + ".png"))

    for count in (1, 2, 3):
        write_model("grain_sacks_%d" % count, sacks(count))
        write_model("ash_urns_%d" % count, ash_urns(count))
    for count in CANDLES:
        write_model("tallow_candles_%d" % count, tallow_candles(count, False))
        write_model("tallow_candles_%d_lit" % count, tallow_candles(count, True))
    write_model("burnt_barrel", burnt_barrel())
    write_model("grave_cross", grave_cross())
    write_model("leaning_gravestone", leaning_gravestone())
    write_model("bone_pile", bone_pile())
    write_model("weapon_rack", weapon_rack())
    write_model("iron_cage", iron_cage(False))
    write_model("iron_cage_hanging", iron_cage(True))
    write_model("rubble_pile", rubble_pile())
    write_split("scarecrow", scarecrow(), 1, ("lower", "upper"))
    write_split("tattered_standard", tattered_standard(), 1, ("lower", "upper"))
    write_split("coffin", coffin(), 2, ("head", "foot"))
    write_split("broken_cart", broken_cart(), 2, ("head", "foot"))


if __name__ == "__main__":
    main()
