#!/usr/bin/env python3
"""Shared toolkit for the scripted 0.4.0 art (PLAN R40.2b-d): GeckoLib models built from boxes, textures painted per
face with ramp palettes, and the PNG / JSON writers.

Not run directly: tools/reach_mob_art.py, tools/reach_boss_art.py and tools/reach_item_art.py use it. Preview the
output with tools/model_preview.py.

Painting model: every box gets its own box-UV region (or shares another box's, mirrored, for left/right limbs). Its
material fills each face from a ramp (dark to light, 4-7 colours) by a level that is the material's base plus a
small light-from-above gradient plus value noise, quantised to the ramp: that gives the flat, banded look of vanilla
textures instead of photographic noise. Decals then draw features in face space: x to the right and y down as the
face is seen from outside it ("front" is the entity's face, "right" its right side).
"""
import json
import math
import os
import random
import struct
import zlib

ROOT = os.path.join("src", "main", "resources", "assets", "embervoid")
CLEAR = (0, 0, 0, 0)


# --- PNG --------------------------------------------------------------------------------------------------------

def write_png(path, width, height, pixels):
    """pixels: list of rows of (r, g, b, a)."""
    raw = b"".join(b"\x00" + b"".join(struct.pack("BBBB", *p) for p in row) for row in pixels)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent="\t")
        f.write("\n")


class Canvas:
    def __init__(self, width, height):
        self.width, self.height = width, height
        self.px = [[CLEAR] * width for _ in range(height)]

    def set(self, x, y, c):
        if 0 <= x < self.width and 0 <= y < self.height:
            self.px[y][x] = c if len(c) == 4 else (c[0], c[1], c[2], 255)

    def get(self, x, y):
        if 0 <= x < self.width and 0 <= y < self.height:
            return self.px[y][x]
        return CLEAR

    def save(self, path):
        write_png(path, self.width, self.height, self.px)


def clamp(v, lo=0, hi=255):
    return max(lo, min(hi, int(v)))


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3)) + ((a[3] if len(a) > 3 else 255),)


def shade(c, d):
    return (clamp(c[0] + d), clamp(c[1] + d), clamp(c[2] + d), c[3] if len(c) > 3 else 255)


def hexc(s):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


def ramp(*hexes):
    return [hexc(h) for h in hexes]


# --- Noise ------------------------------------------------------------------------------------------------------

def hash01(x, y, seed):
    h = (x * 374761393 + y * 668265263 + seed * 2147483647) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0


def value_noise(x, y, seed, cell=3.0):
    gx, gy = x / cell, y / cell
    x0, y0 = math.floor(gx), math.floor(gy)
    fx, fy = gx - x0, gy - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a, b = hash01(x0, y0, seed), hash01(x0 + 1, y0, seed)
    c, d = hash01(x0, y0 + 1, seed), hash01(x0 + 1, y0 + 1, seed)
    return (a + (b - a) * fx) + ((c + (d - c) * fx) - (a + (b - a) * fx)) * fy


# --- Faces ------------------------------------------------------------------------------------------------------

SIDES = ("front", "back", "left", "right")


class Face:
    """One face of a box in its texture region; coordinates in face space (see the module doc)."""

    def __init__(self, tex, glow, name, rect, box):
        self.tex, self.glow, self.name, self.box = tex, glow, name, box
        self.x0, self.y0, self.w, self.h = rect

    def inside(self, x, y):
        return 0 <= x < self.w and 0 <= y < self.h

    def set(self, x, y, c, glow=None):
        if self.inside(x, y):
            self.tex.set(self.x0 + x, self.y0 + y, c)
            if glow is not None:
                self.glow.set(self.x0 + x, self.y0 + y, glow)

    def get(self, x, y):
        return self.tex.get(self.x0 + x, self.y0 + y)

    def clear(self, x, y):
        if self.inside(x, y):
            self.tex.set(self.x0 + x, self.y0 + y, CLEAR)
            self.glow.set(self.x0 + x, self.y0 + y, CLEAR)

    def lit(self, x, y, c, strength=1.0):
        """A glowing pixel. The glowmask is added full bright over the lit texture (AutoGlowingGeoLayer), so the
        texture keeps a dimmed copy (what shows in daylight under the glow) and the glowmask carries the light:
        bright but not white in daylight, still the colour at dusk."""
        self.set(x, y, tuple(clamp(v * 0.45) for v in c[:3]) + (255,),
                 tuple(clamp(v * strength) for v in c[:3]) + (255,))

    def rect(self, x, y, w, h, c, glow=None):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                self.set(xx, yy, c, glow)

    def hline(self, x, y, w, c, glow=None):
        self.rect(x, y, w, 1, c, glow)

    def vline(self, x, y, h, c, glow=None):
        self.rect(x, y, 1, h, c, glow)

    def tint(self, x, y, d):
        """Shifts a pixel lighter (d > 0) or darker."""
        if self.inside(x, y):
            c = self.get(x, y)
            if c[3]:
                self.tex.set(self.x0 + x, self.y0 + y, shade(c, d))

    @property
    def cx(self):
        return self.w // 2

    def side(self):
        return self.name in SIDES


# --- Materials --------------------------------------------------------------------------------------------------

class Material:
    """Fills faces from a ramp. base: ramp index (float) of the middle tone; noise: amplitude in ramp steps;
    cell: noise blotch size; grad: ramp steps lighter at the top of a side face than at its bottom; top / bottom:
    steps added on up / down faces; grain: 'v' or 'h' streaks; speck: chance of a lighter or darker single pixel;
    edge: steps darker on the outermost pixel ring of each face (separates boxes)."""

    def __init__(self, colors, base=None, noise=0.7, cell=2.5, grad=0.8, top=0.6, bottom=-0.8, grain=None,
                 speck=0.0, edge=0.0, seed=0, glow=None, decal=None):
        self.colors = colors
        self.base = (len(colors) - 1) / 2 if base is None else base
        self.noise, self.cell, self.grad, self.top, self.bottom = noise, cell, grad, top, bottom
        self.grain, self.speck, self.edge, self.seed = grain, speck, edge, seed
        self.glow = glow      # when set, every filled pixel also glows with this strength (0..1)
        self.decal = decal    # decal(face, rng) run on every face after the fill

    def but(self, **kw):
        m = Material(self.colors)
        m.__dict__.update(self.__dict__)
        m.__dict__.update(kw)
        return m

    def level(self, face, x, y, seed):
        lv = self.base
        if face.side():
            if face.h > 1:
                lv += self.grad * (0.5 - y / (face.h - 1))
        elif face.name == "top":
            lv += self.top
        else:
            lv += self.bottom
        if self.grain == "v":
            n = value_noise(x * 3.0, y * 0.35, seed, 1.0)
        elif self.grain == "h":
            n = value_noise(x * 0.35, y * 3.0, seed, 1.0)
        else:
            n = value_noise(x, y, seed, self.cell)
        lv += (n - 0.5) * 2 * self.noise
        lv += (hash01(x, y, seed + 7) - 0.5) * 0.5 * self.noise
        if self.speck and hash01(x, y, seed + 11) < self.speck:
            lv += 1.2 if hash01(x, y, seed + 13) < 0.5 else -1.2
        if self.edge and (x == 0 or y == 0 or x == face.w - 1 or y == face.h - 1):
            lv -= self.edge
        return lv

    def color_at(self, lv):
        i = clamp(round(lv), 0, len(self.colors) - 1)
        return self.colors[i]

    def paint(self, face, rng, seed):
        for y in range(face.h):
            for x in range(face.w):
                c = self.color_at(self.level(face, x, y, seed))
                if self.glow:
                    face.lit(x, y, c, self.glow)
                else:
                    face.set(x, y, c)
        if self.decal:
            self.decal(face, rng)


# --- Boxes, bones, models ---------------------------------------------------------------------------------------

class Box:
    """A cube: origin and size in model pixels (Bedrock coordinates: +x is the entity's left, -z its front).
    decals: {face name or 'all' or 'sides': fn(face, rng)} drawn after the material."""

    def __init__(self, origin, size, mat, decals=None, inflate=0.0, rotation=None, pivot=None, share=None,
                 mirror=False, name=None):
        self.origin, self.size, self.mat = list(origin), list(size), mat
        self.decals = decals or {}
        self.inflate, self.rotation, self.pivot = inflate, rotation, pivot
        self.share, self.mirror, self.name = share, mirror, name
        self.uv = None

    def dims(self):
        return tuple(int(math.floor(s)) for s in self.size)

    def uv_size(self):
        w, h, d = self.dims()
        return 2 * (d + w), d + h

    def faces(self):
        """Face-space rectangles in the texture (as model_preview / GeckoLib map them)."""
        u, v = self.uv
        w, h, d = self.dims()
        return {
            "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
        }

    def json(self):
        j = {"origin": [round(c, 4) for c in self.origin], "size": [round(c, 4) for c in self.size],
             "uv": list((self.share or self).uv)}
        if self.inflate:
            j["inflate"] = self.inflate
        if self.rotation:
            j["rotation"] = self.rotation
            j["pivot"] = self.pivot or [self.origin[i] + self.size[i] / 2 for i in range(3)]
        if self.mirror:
            j["mirror"] = True
        return j


class Bone:
    def __init__(self, name, pivot, parent=None, rotation=None, boxes=()):
        self.name, self.pivot, self.parent, self.rotation = name, list(pivot), parent, rotation
        self.boxes = list(boxes)

    def add(self, *boxes):
        self.boxes.extend(boxes)
        return self


def mirror_box(box, mat=None):
    """The same box on the other side of x = 0, sharing the texture region mirrored."""
    o, s = box.origin, box.size
    rot = None
    piv = None
    if box.rotation:
        rot = [box.rotation[0], -box.rotation[1], -box.rotation[2]]
        p = box.pivot or [o[i] + s[i] / 2 for i in range(3)]
        piv = [-p[0], p[1], p[2]]
    return Box([-(o[0] + s[0]), o[1], o[2]], s, mat or box.mat, inflate=box.inflate, rotation=rot, pivot=piv,
               share=box.share or box, mirror=not box.mirror)


def pack(bones, tex_w):
    """Shelf packing, tallest regions first. Returns the used height."""
    boxes = [b for bone in bones for b in bone.boxes if b.share is None]
    boxes.sort(key=lambda b: (-b.uv_size()[1], -b.uv_size()[0]))
    x = y = row_h = 0
    for b in boxes:
        w, h = b.uv_size()
        if w > tex_w:
            raise ValueError("box wider than the texture: %s" % (b.size,))
        if x + w > tex_w:
            x, y, row_h = 0, y + row_h, 0
        b.uv = (x, y)
        x += w
        row_h = max(row_h, h)
    return y + row_h


def twin_planes(box, tex, glow):
    """A box with no thickness has two coincident faces and in game the one drawn later shows from both sides
    (GeckoLib draws west, east, north, south, up, down; Minecraft's depth test passes on equal depth). Copies the
    first face onto the later one so both look the same in the world: front onto back and left onto right
    mirrored left-right, top onto bottom mirrored top-bottom."""
    w, h, d = box.dims()
    f = box.faces()
    pairs = []
    if d == 0:
        pairs.append((f["front"], f["back"], "x"))
    if w == 0:
        pairs.append((f["left"], f["right"], "x"))
    if h == 0:
        pairs.append((f["top"], f["bottom"], "y"))
    for (sx, sy, sw, sh), (dx, dy, _, _), flip in pairs:
        for canvas in (tex, glow):
            src = [[canvas.get(sx + x, sy + y) for x in range(sw)] for y in range(sh)]
            for y in range(sh):
                for x in range(sw):
                    c = src[y][sw - 1 - x] if flip == "x" else src[sh - 1 - y][x]
                    canvas.set(dx + x, dy + y, c)


def next_pow2(n):
    return 1 << max(0, (n - 1).bit_length())


def write_model(name, bones, animations, bounds, tex_w=64, seed=None, glow_extra=None, folder="entity"):
    """Packs, paints and writes geo, animation, texture and glowmask. bounds: visible bounds (width, height) in
    blocks, kept from the greybox."""
    tex_w = tex_w
    used = pack(bones, tex_w)
    while next_pow2(used) > tex_w * 2 and tex_w < 512:
        tex_w *= 2
        used = pack(bones, tex_w)
    tex_h = max(16, next_pow2(used))
    tex, glow = Canvas(tex_w, tex_h), Canvas(tex_w, tex_h)
    rng = random.Random(seed if seed is not None else name)
    for i, bone in enumerate(bones):
        for j, box in enumerate(bone.boxes):
            if box.share is not None:
                continue
            for face_name, rect in box.faces().items():
                if rect[2] <= 0 or rect[3] <= 0:
                    continue
                face = Face(tex, glow, face_name, rect, box)
                # A stable seed (str hash() is salted per process): reruns give the same texture.
                fseed = (zlib.crc32(name.encode()) & 0xFFFF) + i * 131 + j * 17 + list(box.faces()).index(face_name) * 3
                box.mat.paint(face, rng, fseed)
                for key, fn in box.decals.items():
                    if key == face_name or key == "all" or (key == "sides" and face.side()):
                        fn(face, rng)
            twin_planes(box, tex, glow)
    if glow_extra:
        glow_extra(tex, glow, bones)
    out = []
    for bone in bones:
        b = {"name": bone.name, "pivot": [round(c, 4) for c in bone.pivot]}
        if bone.parent:
            b["parent"] = bone.parent
        if bone.rotation:
            b["rotation"] = bone.rotation
        if bone.boxes:
            b["cubes"] = [box.json() for box in bone.boxes]
        out.append(b)
    geo = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.embervoid." + name,
                "texture_width": tex_w, "texture_height": tex_h,
                "visible_bounds_width": bounds[0], "visible_bounds_height": bounds[1],
                "visible_bounds_offset": [0, bounds[1] / 2, 0],
            },
            "bones": out,
        }],
    }
    write_json(os.path.join(ROOT, "geo", folder, name + ".geo.json"), geo)
    write_json(os.path.join(ROOT, "animations", folder, name + ".animation.json"),
               {"format_version": "1.8.0", "animations": animations})
    tex.save(os.path.join(ROOT, "textures", folder, name + ".png"))
    glow.save(os.path.join(ROOT, "textures", folder, name + "_glowmask.png"))
    print("wrote %s (%dx%d, %d bones, %d boxes)" % (name, tex_w, tex_h, len(bones),
                                                    sum(len(b.boxes) for b in bones)))


# --- Animation helpers ------------------------------------------------------------------------------------------

ZERO = [0, 0, 0]


def keys(*pairs):
    """keys(0, [x,y,z], 0.5, [...]) -> {"0.0": [...], "0.5": [...]}"""
    return {str(float(round(pairs[i], 4))): [round(v, 3) for v in pairs[i + 1]] for i in range(0, len(pairs), 2)}


def rot(*pairs):
    return {"rotation": keys(*pairs)}


def pos(*pairs):
    return {"position": keys(*pairs)}


def chan(**channels):
    """chan(rotation=keys(...), position=keys(...)) for a bone with several channels."""
    return channels


def anim(length, bones, loop=True):
    a = {"animation_length": round(length, 4), "bones": bones}
    if loop:
        a["loop"] = True
    return a


def merge(*parts):
    """Merges bone dictionaries; channels of the same bone combine (later keys win)."""
    out = {}
    for part in parts:
        for bone, channels in part.items():
            for ch, frames in channels.items():
                out.setdefault(bone, {}).setdefault(ch, {}).update(frames)
    return out


def sway(length, amp, axis=0, phase=0.0, steps=4):
    """A looping sine sway sampled at steps keyframes per cycle (degrees on one axis)."""
    pairs = []
    for i in range(steps + 1):
        t = length * i / steps
        v = [0, 0, 0]
        v[axis] = amp * math.sin(2 * math.pi * (i / steps + phase))
        pairs += [t, v]
    return keys(*pairs)


def t(ticks):
    """Server ticks to animation seconds."""
    return round(ticks / 20, 3)


# --- Common decals ----------------------------------------------------------------------------------------------

def scatter(face, rng, count, color, glow=None, size=1):
    for _ in range(count):
        x, y = rng.randrange(face.w), rng.randrange(face.h)
        face.rect(x, y, size, size, color, glow)


def crack(face, rng, color, glow=None, length=4, x=None, y=None):
    """A short jagged crack going mostly down."""
    x = rng.randrange(face.w) if x is None else x
    y = rng.randrange(face.h) if y is None else y
    for _ in range(length):
        face.set(x, y, color, glow)
        y += 1
        x += rng.choice((-1, 0, 0, 1))


def ember_cracks(face, rng, count, hot, warm, strength=0.9, length=(2, 5)):
    """Glowing cracks: a hot core with a warm rim pixel here and there."""
    for _ in range(count):
        x, y = rng.randrange(face.w), rng.randrange(face.h)
        for _ in range(rng.randint(*length)):
            if not face.inside(x, y):
                break
            face.lit(x, y, hot, strength)
            if rng.random() < 0.35 and face.inside(x + 1, y):
                face.lit(x + 1, y, warm, strength * 0.6)
            y += 1
            x += rng.choice((-1, 0, 0, 1))


def tatter(face, rng, depth=2, chance=0.5):
    """Ragged bottom edge: clears pixels upward from the bottom row."""
    if not face.side():
        return
    for x in range(face.w):
        if rng.random() < chance:
            for k in range(rng.randint(1, depth)):
                face.clear(x, face.h - 1 - k)


def stitch(face, y, color, step=2, x0=0):
    for x in range(x0, face.w, step):
        face.set(x, y, color)


def outline_shadow(face, d=-18):
    """Darkens the bottom row and the side columns of a side face a little (contact shadow)."""
    if not face.side():
        return
    for x in range(face.w):
        face.tint(x, face.h - 1, d)
