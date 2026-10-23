#!/usr/bin/env python3
"""Greybox helpers: placeholder GeckoLib models, animations and textures for a new mob, before its art pass.

Not run directly any more: every entity has final art (tools/overworld_mob_art.py, tools/reach_mob_art.py,
tools/reach_boss_art.py), so no placeholder here can overwrite it. tools/gear_placeholders.py,
tools/reach_world_placeholders.py, tools/reliquary_placeholders.py and tools/vampire_placeholders.py use its Canvas;
a new mob's greybox can use write_mob() until its art is scripted on tools/art_kit.py.

Models use Bedrock geometry 1.12.0 with box UV. Each cube gets its own UV region, packed in rows, and the texture
paints those regions: a base colour with noise, plus per-cube details (eyes, cracks). Glowmask textures (for
GeckoLib's AutoGlowingGeoLayer) light up eyes and embers.
"""
import json
import os
import random
import struct
import zlib

ROOT = os.path.join("src", "main", "resources", "assets", "embervoid")


# --- PNG --------------------------------------------------------------------------------------------------------

def write_png(path, width, height, pixels):
    """pixels: list of rows, each a list of (r, g, b, a)."""
    raw = b"".join(b"\x00" + b"".join(struct.pack("BBBB", *p) for p in row) for row in pixels)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


class Canvas:
    def __init__(self, width, height):
        self.width, self.height = width, height
        self.px = [[(0, 0, 0, 0)] * width for _ in range(height)]

    def set(self, x, y, color):
        if 0 <= x < self.width and 0 <= y < self.height:
            self.px[y][x] = color

    def rect(self, x, y, w, h, color, noise=0, rng=None):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                c = color
                if noise and rng:
                    d = rng.randint(-noise, noise)
                    c = (clamp(color[0] + d), clamp(color[1] + d), clamp(color[2] + d), color[3])
                self.set(xx, yy, c)

    def save(self, path):
        write_png(path, self.width, self.height, self.px)


def clamp(v):
    return max(0, min(255, v))


# --- Models -----------------------------------------------------------------------------------------------------

class Cube:
    def __init__(self, origin, size, tag=None, inflate=0.0):
        self.origin, self.size, self.tag, self.inflate = origin, size, tag, inflate
        self.uv = None

    def uv_size(self):
        w, h, d = (int(round(s)) for s in self.size)
        return 2 * (d + w), d + h

    def faces(self):
        """Box-UV face rectangles: name -> (x, y, w, h)."""
        u, v = self.uv
        w, h, d = (int(round(s)) for s in self.size)
        return {
            "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
        }


class Bone:
    def __init__(self, name, pivot, parent=None, rotation=None, cubes=()):
        self.name, self.pivot, self.parent, self.rotation, self.cubes = name, pivot, parent, rotation, list(cubes)


def pack_uvs(bones, tex_w):
    x = y = row_h = 0
    for bone in bones:
        for cube in bone.cubes:
            w, h = cube.uv_size()
            if x + w > tex_w:
                x, y, row_h = 0, y + row_h, 0
            cube.uv = (x, y)
            x += w
            row_h = max(row_h, h)
    return y + row_h


def geo_json(name, bones, tex_w, tex_h, bounds):
    out = []
    for bone in bones:
        b = {"name": bone.name, "pivot": bone.pivot}
        if bone.parent:
            b["parent"] = bone.parent
        if bone.rotation:
            b["rotation"] = bone.rotation
        if bone.cubes:
            b["cubes"] = []
            for c in bone.cubes:
                cj = {"origin": c.origin, "size": c.size, "uv": list(c.uv)}
                if c.inflate:
                    cj["inflate"] = c.inflate
                b["cubes"].append(cj)
        out.append(b)
    return {
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


def anim(length, bones, loop=True):
    a = {"animation_length": length, "bones": bones}
    if loop:
        a["loop"] = True
    return a


def keys(*pairs):
    """keys(0, [x,y,z], 0.5, [...]) -> {"0.0": [...], "0.5": [...]}"""
    return {str(float(pairs[i])): pairs[i + 1] for i in range(0, len(pairs), 2)}


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent="\t")
        f.write("\n")


def write_mob(name, bones, tex_w, bounds, animations, paint):
    tex_h = pack_uvs(bones, tex_w)
    tex_h = 1 << (tex_h - 1).bit_length()  # next power of two
    write_json(os.path.join(ROOT, "geo", "entity", name + ".geo.json"), geo_json(name, bones, tex_w, tex_h, bounds))
    write_json(os.path.join(ROOT, "animations", "entity", name + ".animation.json"),
               {"format_version": "1.8.0", "animations": animations})
    tex, glow = Canvas(tex_w, tex_h), Canvas(tex_w, tex_h)
    rng = random.Random(name)
    for bone in bones:
        for cube in bone.cubes:
            paint(tex, glow, bone, cube, rng)
    tex.save(os.path.join(ROOT, "textures", "entity", name + ".png"))
    glow.save(os.path.join(ROOT, "textures", "entity", name + "_glowmask.png"))


def paint_box(tex, cube, color, rng, noise=10):
    for x, y, w, h in cube.faces().values():
        tex.rect(x, y, w, h, color, noise, rng)


# The overworld mobs have final art in tools/overworld_mob_art.py; the Realm 1 (Ashen Reach) mobs and bosses in
# tools/reach_mob_art.py and tools/reach_boss_art.py.
