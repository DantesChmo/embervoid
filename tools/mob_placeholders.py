#!/usr/bin/env python3
"""Writes placeholder GeckoLib models, animations and textures for the overworld mobs.

Run from the repository root: python3 tools/mob_placeholders.py
Output goes to src/main/resources/assets/embervoid/{geo,animations,textures}/entity/. Everything here is a
stand-in until the art pass (Blockbench); delete this script once real assets replace its output.

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


# --- Ash Ghoul: gaunt, hunched, long arms, ember eyes and cracks --------------------------------------------------

def ash_ghoul():
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 11, 0], "root", [20, 0, 0], [Cube([-3.5, 11, -1.5], [7, 10, 3], "body")]),
        Bone("head", [0, 20, -3], "root", None, [Cube([-3.5, 20, -6.5], [7, 7, 7], "head")]),
        Bone("right_arm", [-4.5, 20, -3], "root", [-15, 0, 0], [Cube([-6, 6, -4.5], [3, 14, 3], "limb")]),
        Bone("left_arm", [4.5, 20, -3], "root", [-15, 0, 0], [Cube([3, 6, -4.5], [3, 14, 3], "limb")]),
        Bone("right_leg", [-2, 11, 0], "root", None, [Cube([-3.5, 0, -1.5], [3, 11, 3], "limb")]),
        Bone("left_leg", [2, 11, 0], "root", None, [Cube([0.5, 0, -1.5], [3, 11, 3], "limb")]),
    ]
    zero = [0, 0, 0]
    animations = {
        "misc.idle": anim(2.0, {
            "body": {"rotation": keys(0, zero, 1, [3, 0, 0], 2, zero)},
            "right_arm": {"rotation": keys(0, zero, 1, [-6, 0, 3], 2, zero)},
            "left_arm": {"rotation": keys(0, zero, 1, [-6, 0, -3], 2, zero)},
        }),
        "move.walk": anim(0.6, {
            "right_leg": {"rotation": keys(0, [35, 0, 0], 0.3, [-35, 0, 0], 0.6, [35, 0, 0])},
            "left_leg": {"rotation": keys(0, [-35, 0, 0], 0.3, [35, 0, 0], 0.6, [-35, 0, 0])},
            "right_arm": {"rotation": keys(0, [-30, 0, 0], 0.3, [20, 0, 0], 0.6, [-30, 0, 0])},
            "left_arm": {"rotation": keys(0, [20, 0, 0], 0.3, [-30, 0, 0], 0.6, [20, 0, 0])},
            "body": {"rotation": keys(0, [5, 0, 0], 0.3, [5, 0, 0], 0.6, [5, 0, 0])},
        }),
        "attack.swing": anim(0.4, {
            "right_arm": {"rotation": keys(0, zero, 0.15, [-110, 0, 10], 0.4, zero)},
            "left_arm": {"rotation": keys(0, zero, 0.1, [-60, 0, -10], 0.25, [-100, 0, -10], 0.4, zero)},
        }, loop=False),
    }
    ash = (107, 101, 96, 255)
    ember = (255, 140, 40, 255)

    def paint(tex, glow, bone, cube, rng):
        paint_box(tex, cube, ash, rng, 14)
        faces = cube.faces()
        # Glowing cracks scattered over the body and limbs.
        for _ in range(3 if cube.tag == "limb" else 6):
            x, y, w, h = faces[rng.choice(["front", "back", "left", "right"])]
            cx, cy = x + rng.randrange(w), y + rng.randrange(h)
            for i in range(rng.randint(2, 4)):
                px, py = cx + (i if rng.random() < 0.5 else 0), cy + i
                if x <= px < x + w and y <= py < y + h:
                    tex.set(px, py, (200, 90, 30, 255))
                    glow.set(px, py, ember)
        if cube.tag == "head":
            x, y, w, h = faces["front"]
            tex.rect(x + 1, y + 2, w - 2, 3, (40, 36, 34, 255))  # sunken eye band
            for ex in (x + 1, x + w - 3):
                tex.rect(ex, y + 3, 2, 1, ember)
                glow.rect(ex, y + 3, 2, 1, ember)
            tex.rect(x + 2, y + h - 2, w - 4, 1, (30, 26, 24, 255))  # mouth

    write_mob("ash_ghoul", bones, 64, [2, 2.5], animations, paint)


# --- Hollow: armoured undead swordsman, bone face, cold eyes -----------------------------------------------------

def hollow():
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 24, 0], "root", None, [
            Cube([-4, 12, -2], [8, 12, 4], "armor"),
            Cube([-4.5, 11, -2.5], [9, 3, 5], "belt"),
        ]),
        Bone("head", [0, 24, 0], "root", None, [
            Cube([-4, 24, -4], [8, 8, 8], "face"),
            Cube([-4.5, 28, -4.5], [9, 5, 9], "helmet"),
        ]),
        Bone("right_arm", [-5, 22, 0], "root", None, [
            Cube([-8, 12, -2], [4, 12, 4], "armor"),
            Cube([-8.5, 20, -2.5], [5, 3, 5], "pauldron"),
        ]),
        Bone("right_item", [-6, 13, -1], "right_arm"),
        Bone("left_arm", [5, 22, 0], "root", None, [
            Cube([4, 12, -2], [4, 12, 4], "armor"),
            Cube([3.5, 20, -2.5], [5, 3, 5], "pauldron"),
        ]),
        Bone("right_leg", [-1.9, 12, 0], "root", None, [Cube([-3.9, 0, -2], [4, 12, 4], "armor")]),
        Bone("left_leg", [1.9, 12, 0], "root", None, [Cube([-0.1, 0, -2], [4, 12, 4], "armor")]),
    ]
    zero = [0, 0, 0]
    block = [-75, -40, 0]
    animations = {
        "misc.idle": anim(3.0, {
            "right_arm": {"rotation": keys(0, [-10, 0, 0], 1.5, [-13, 0, 2], 3, [-10, 0, 0])},
            "left_arm": {"rotation": keys(0, zero, 1.5, [-3, 0, -2], 3, zero)},
        }),
        "move.walk": anim(1.2, {
            "right_leg": {"rotation": keys(0, [25, 0, 0], 0.6, [-25, 0, 0], 1.2, [25, 0, 0])},
            "left_leg": {"rotation": keys(0, [-25, 0, 0], 0.6, [25, 0, 0], 1.2, [-25, 0, 0])},
            "right_arm": {"rotation": keys(0, [-20, 0, 0], 0.6, [-5, 0, 0], 1.2, [-20, 0, 0])},
            "left_arm": {"rotation": keys(0, [15, 0, 0], 0.6, [-15, 0, 0], 1.2, [15, 0, 0])},
        }),
        "attack.swing": anim(0.9, {
            # Wind-up to 0.3 s, the blade comes down at 0.4 s (HollowEntity.WINDUP_TICKS = 8).
            "right_arm": {"rotation": keys(0, zero, 0.3, [-165, 0, 10], 0.45, [-40, 0, 0], 0.9, zero)},
            "body": {"rotation": keys(0, zero, 0.3, [0, 15, 0], 0.45, [0, -20, 0], 0.9, zero)},
        }, loop=False),
        "attack.block": anim(1.0, {
            "right_arm": {"rotation": keys(0, block, 1, block)},
            "left_arm": {"rotation": keys(0, [-50, 30, 0], 1, [-50, 30, 0])},
            "body": {"rotation": keys(0, [0, 10, 0], 1, [0, 10, 0])},
        }),
    }
    iron = (74, 78, 86, 255)
    rust = (96, 70, 52, 255)
    bone_c = (201, 193, 168, 255)
    eye = (150, 200, 255, 255)

    def paint(tex, glow, bone, cube, rng):
        color = {"face": bone_c, "belt": rust, "pauldron": (58, 61, 68, 255)}.get(cube.tag, iron)
        paint_box(tex, cube, color, rng, 12)
        faces = cube.faces()
        if cube.tag == "helmet":
            for name, (x, y, w, h) in faces.items():
                if name == "front":
                    # Open visor: transparent so the bone face shows through.
                    tex.rect(x + 1, y + 1, w - 2, h - 1, (0, 0, 0, 0))
                elif name == "bottom":
                    tex.rect(x, y, w, h, (0, 0, 0, 0))
        if cube.tag == "face":
            x, y, w, h = faces["front"]
            for ex in (x + 1, x + w - 3):
                tex.rect(ex, y + 3, 2, 2, (20, 20, 24, 255))
                tex.set(ex + 1, y + 4, eye)
                glow.set(ex + 1, y + 4, eye)
            tex.rect(x + 2, y + 6, w - 4, 1, (60, 55, 45, 255))
        if cube.tag in ("armor", "pauldron"):
            for _ in range(4):  # rust spots
                x, y, w, h = faces[rng.choice(list(faces))]
                tex.rect(x + rng.randrange(w), y + rng.randrange(h), 1, 1, rust)

    write_mob("hollow", bones, 64, [2, 3], animations, paint)


# --- Ember Wisp: glowing core in a translucent flame shell with orbiting embers ---------------------------------

def ember_wisp():
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("core", [0, 4, 0], "root", None, [Cube([-2, 2, -2], [4, 4, 4], "core")]),
        Bone("shell", [0, 4, 0], "root", None, [Cube([-3, 1, -3], [6, 6, 6], "shell")]),
        Bone("orbit", [0, 4, 0], "root", None, [
            Cube([4.5, 3.5, -0.5], [1, 1, 1], "ember"),
            Cube([-5.5, 4.5, -0.5], [1, 1, 1], "ember"),
            Cube([-0.5, 2.5, 4.5], [1, 1, 1], "ember"),
        ]),
        Bone("flame", [0, 7, 0], "root", None, [Cube([-1, 7, -1], [2, 3, 2], "shell")]),
    ]

    def float_anim(length, bob):
        return anim(length, {
            "root": {"position": keys(0, [0, 0, 0], length / 2, [0, bob, 0], length, [0, 0, 0])},
            "orbit": {"rotation": keys(0, [0, 0, 0], length / 2, [0, 180, 0], length, [0, 360, 0])},
            "shell": {"rotation": keys(0, [0, 0, 0], length / 2, [0, -45, 0], length, [0, -90, 0])},
            "flame": {"scale": keys(0, [1, 1, 1], length / 2, [1, 1.4, 1], length, [1, 1, 1])},
        })

    animations = {"misc.idle": float_anim(2.0, 1), "move.fly": float_anim(1.0, 1.5)}
    colors = {"core": (255, 214, 110, 255), "shell": (255, 122, 26, 140), "ember": (255, 180, 70, 255)}

    def paint(tex, glow, bone, cube, rng):
        color = colors[cube.tag]
        paint_box(tex, cube, color, rng, 18)
        paint_box(glow, cube, color, rng, 18)

    write_mob("ember_wisp", bones, 32, [1, 1], animations, paint)




# --- Realm 1 (Ashen Reach) mobs ---------------------------------------------------------------------------------
# Greybox models at their final size (the hitbox in reach.mob.ReachMobs) with draft animations under their final
# names (PLAN 10.1). The windup of a telegraphed move is part of its animation and ends where the entity class lands
# the move (DESIGN §4.5 ticks / 20 = seconds), so a model swap in the art pass keeps the timing.

ZERO = [0, 0, 0]


def rot(*pairs):
    return {"rotation": keys(*pairs)}


def pos(*pairs):
    return {"position": keys(*pairs)}


def biped(legs=12, body=(8, 12, 4), head=8, arm=(4, 12), leg_w=4, hunch=0):
    """Root, body (pivot at the hips, pitched forward by hunch), head on the body, arms on the body, legs on root.
    Arms hang from the shoulders (top of the body); hands end arm[1] pixels lower."""
    bw, bh, bd = body
    aw, ah = arm
    top = legs + bh
    return [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, legs, 0], "root", [hunch, 0, 0] if hunch else None,
             [Cube([-bw / 2, legs, -bd / 2], [bw, bh, bd], "body")]),
        Bone("head", [0, top, 0], "body", None, [Cube([-head / 2, top, -head / 2], [head, head, head], "head")]),
        Bone("right_arm", [-bw / 2 - aw / 2, top - 2, 0], "body", None,
             [Cube([-bw / 2 - aw, top - ah, -aw / 2], [aw, ah, aw], "limb")]),
        Bone("left_arm", [bw / 2 + aw / 2, top - 2, 0], "body", None,
             [Cube([bw / 2, top - ah, -aw / 2], [aw, ah, aw], "limb")]),
        Bone("right_leg", [-leg_w / 2, legs, 0], "root", None, [Cube([-leg_w, 0, -leg_w / 2], [leg_w, legs, leg_w], "limb")]),
        Bone("left_leg", [leg_w / 2, legs, 0], "root", None, [Cube([0, 0, -leg_w / 2], [leg_w, legs, leg_w], "limb")]),
    ]


def walk(length=1.0, leg=30, arm=20, **extra):
    h = length / 2
    bones = {
        "right_leg": rot(0, [leg, 0, 0], h, [-leg, 0, 0], length, [leg, 0, 0]),
        "left_leg": rot(0, [-leg, 0, 0], h, [leg, 0, 0], length, [-leg, 0, 0]),
        "right_arm": rot(0, [-arm, 0, 0], h, [arm, 0, 0], length, [-arm, 0, 0]),
        "left_arm": rot(0, [arm, 0, 0], h, [-arm, 0, 0], length, [arm, 0, 0]),
    }
    bones.update(extra)
    return anim(length, bones)


def idle(length=3.0, sway=4, **extra):
    h = length / 2
    bones = {
        "head": rot(0, ZERO, h, [sway, sway, 0], length, ZERO),
        "right_arm": rot(0, ZERO, h, [-sway, 0, sway / 2], length, ZERO),
        "left_arm": rot(0, ZERO, h, [-sway, 0, -sway / 2], length, ZERO),
    }
    bones.update(extra)
    return anim(length, bones)


def overhead(windup, length, arms=("right_arm",), body=True):
    """Arm(s) raised over the head until windup (the telegraph), brought down just after it."""
    bones = {a: rot(0, ZERO, windup, [-160, 0, 10 if a == "right_arm" else -10], windup + 0.1, [-25, 0, 0], length, ZERO)
             for a in arms}
    if body:
        bones["body"] = rot(0, ZERO, windup, [-8, 0, 0], windup + 0.1, [15, 0, 0], length, ZERO)
    return anim(length, bones, loop=False)


def painter(colors, eye=None, eye_tags=("head",), eye_row=0.4, glow_tags=(), cracks=(), crack_color=None, noise=12):
    """Flat noisy colours by cube tag, two glowing eyes on the front of eye_tags, glowing cracks on crack tags."""
    def paint(tex, glow, bone, cube, rng):
        color = colors.get(cube.tag, colors["default"])
        paint_box(tex, cube, color, rng, noise)
        if cube.tag in glow_tags:
            paint_box(glow, cube, color, rng, noise)
        faces = cube.faces()
        if cube.tag in cracks:
            for _ in range(4):
                x, y, w, h = faces[rng.choice(["front", "back", "left", "right"])]
                cx, cy = x + rng.randrange(w), y + rng.randrange(h)
                for i in range(rng.randint(2, 4)):
                    px, py = cx + (i if rng.random() < 0.5 else 0), cy + i
                    if x <= px < x + w and y <= py < y + h:
                        tex.set(px, py, crack_color)
                        glow.set(px, py, crack_color)
        if eye and cube.tag in eye_tags:
            x, y, w, h = faces["front"]
            ey = y + int(h * eye_row)
            for ex in (x + 1, x + w - 3) if w >= 6 else (x, x + w - 1):
                ew = 2 if w >= 6 else 1
                tex.rect(ex, ey, ew, 1, eye)
                glow.rect(ex, ey, ew, 1, eye)
    return paint


# Citadel — Cinder Hound: lean burning hound. Bite after a 6-tick lunge; howls to pull the pack.
def cinder_hound():
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 10, 0], "root", None, [Cube([-3, 7, -6], [6, 6, 13], "body")]),
        Bone("head", [0, 11, -6], "body", None, [
            Cube([-3, 8, -12], [6, 6, 6], "head"),
            Cube([-1.5, 8, -15], [3, 3, 3], "snout"),
            Cube([-3, 14, -9], [1, 2, 1], "head"),
            Cube([2, 14, -9], [1, 2, 1], "head"),
        ]),
        Bone("tail", [0, 12, 7], "body", [-30, 0, 0], [Cube([-1, 11, 7], [2, 2, 7], "tail")]),
        Bone("front_right_leg", [-2, 7, -4], "root", None, [Cube([-3, 0, -5], [2, 7, 2], "limb")]),
        Bone("front_left_leg", [2, 7, -4], "root", None, [Cube([1, 0, -5], [2, 7, 2], "limb")]),
        Bone("back_right_leg", [-2, 7, 5], "root", None, [Cube([-3, 0, 4], [2, 7, 2], "limb")]),
        Bone("back_left_leg", [2, 7, 5], "root", None, [Cube([1, 0, 4], [2, 7, 2], "limb")]),
    ]

    def gait(length, angle):
        h = length / 2
        a, b = [angle, 0, 0], [-angle, 0, 0]
        return {
            "front_right_leg": rot(0, a, h, b, length, a), "back_left_leg": rot(0, a, h, b, length, a),
            "front_left_leg": rot(0, b, h, a, length, b), "back_right_leg": rot(0, b, h, a, length, b),
            "tail": rot(0, [-30, 10, 0], h, [-30, -10, 0], length, [-30, 10, 0]),
        }

    animations = {
        "misc.idle": anim(2.0, {"tail": rot(0, [-30, 8, 0], 1, [-30, -8, 0], 2, [-30, 8, 0]),
                                "head": rot(0, ZERO, 1, [5, 0, 0], 2, ZERO)}),
        "move.walk": anim(0.5, gait(0.5, 40)),
        "attack.bite": anim(0.5, {
            "head": rot(0, ZERO, 0.3, [-25, 0, 0], 0.35, [20, 0, 0], 0.5, ZERO),
            "body": rot(0, ZERO, 0.3, [5, 0, 0], 0.35, [-5, 0, 0], 0.5, ZERO),
        }, loop=False),
        "misc.howl": anim(1.0, {"head": rot(0, ZERO, 0.2, [-55, 0, 0], 0.8, [-55, 0, 0], 1.0, ZERO)}, loop=False),
    }
    paint = painter({"default": (40, 34, 30, 255), "snout": (30, 26, 24, 255), "tail": (60, 30, 20, 255)},
                    eye=(255, 140, 40, 255), eye_row=0.3, cracks=("body", "limb", "tail"), crack_color=(255, 110, 30, 255))
    write_mob("cinder_hound", bones, 64, [1.5, 1.2], animations, paint)


# Citadel — Sparking Marksman: crossbowman on the walls. A spark lights on the bolt tip 20 ticks before it fires.
def sparking_marksman():
    bones = biped()
    bones[2].cubes.append(Cube([-4.5, 24, -4.5], [9, 9, 9], "hood"))
    bones.append(Bone("crossbow", [-6, 12, 0], "right_arm", None, [
        Cube([-7, 11, -9], [2, 2, 8], "wood"),
        Cube([-11, 12, -9], [10, 1, 1], "iron"),
    ]))
    bones.append(Bone("spark", [-6, 12, -10], "crossbow", None, [Cube([-6.5, 11.5, -11], [1, 1, 1], "spark", 0.2)]))
    aim = [-90, 0, 0]
    animations = {
        "misc.idle": idle(3.0, 3),
        "move.walk": walk(1.0, 25, 15),
        "attack.aim": anim(1.0, {
            "right_arm": rot(0, ZERO, 0.3, aim, 1.0, aim),
            "left_arm": rot(0, ZERO, 0.3, [-80, 30, 0], 1.0, [-80, 30, 0]),
            "head": rot(0, ZERO, 0.3, [5, 0, 0], 1.0, [5, 0, 0]),
        }, loop=False),
        "attack.shoot": anim(0.3, {"right_arm": rot(0, aim, 0.1, [-110, 0, 0], 0.3, ZERO)}, loop=False),
    }
    paint = painter({"default": (50, 44, 40, 255), "hood": (40, 34, 30, 255), "head": (90, 70, 60, 255),
                     "wood": (80, 60, 44, 255), "iron": (90, 90, 96, 255), "spark": (255, 220, 120, 255)},
                    eye=(255, 160, 60, 255), eye_tags=("hood",), eye_row=0.45, glow_tags=("spark",))
    write_mob("sparking_marksman", bones, 64, [2, 2.5], animations, paint)


# Citadel — Furnace Golem: a walking furnace. Heats with each hit taken; the heat bones glow for 25 ticks before the
# fire wave.
def furnace_golem():
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 12, 0], "root", None, [
            Cube([-8, 12, -6], [16, 18, 12], "iron"),
            Cube([-4, 17, -6.5], [8, 8, 1], "grate"),
        ]),
        Bone("heat", [0, 12, 0], "body", None, [Cube([-8, 12, -6], [16, 18, 12], "heat", 0.3)]),
        Bone("head", [0, 30, 0], "body", None, [
            Cube([-4, 30, -4], [8, 6, 8], "helm"),
            Cube([-2, 36, -2], [4, 8, 4], "chimney"),
        ]),
        Bone("right_arm", [-10.5, 28, 0], "body", None, [Cube([-13, 8, -3], [5, 20, 6], "iron")]),
        Bone("left_arm", [10.5, 28, 0], "body", None, [Cube([8, 8, -3], [5, 20, 6], "iron")]),
        Bone("right_leg", [-4, 12, 0], "root", None, [Cube([-7, 0, -3], [6, 12, 6], "iron")]),
        Bone("left_leg", [4, 12, 0], "root", None, [Cube([1, 0, -3], [6, 12, 6], "iron")]),
    ]
    animations = {
        "misc.idle": idle(4.0, 2),
        "move.walk": walk(1.6, 18, 12),
        "attack.slam": overhead(0.7, 1.2, ("right_arm", "left_arm")),
        "misc.heat": anim(1.25, {
            "body": rot(0, ZERO, 0.25, [0, 0, 2], 0.5, [0, 0, -2], 0.75, [0, 0, 2], 1.0, [0, 0, -2], 1.25, ZERO),
            "right_arm": rot(0, ZERO, 1.25, [0, 0, 25]), "left_arm": rot(0, ZERO, 1.25, [0, 0, -25]),
        }, loop=False),
        "attack.wave": anim(0.5, {
            "right_arm": rot(0, [0, 0, 25], 0.1, [0, 0, 70], 0.5, ZERO),
            "left_arm": rot(0, [0, 0, -25], 0.1, [0, 0, -70], 0.5, ZERO),
        }, loop=False),
    }
    paint = painter({"default": (58, 56, 60, 255), "helm": (52, 50, 54, 255), "grate": (255, 120, 30, 255),
                     "chimney": (40, 38, 40, 255), "heat": (255, 90, 20, 140)},
                    eye=(255, 170, 60, 255), eye_tags=("helm",), eye_row=0.4, glow_tags=("grate", "heat"))
    write_mob("furnace_golem", bones, 128, [3, 3.5], animations, paint)


# Citadel — Smoldering Knight: the king's guard with a greatsword. 3-hit combo, and a lunge after a 15-tick crouch.
def smoldering_knight():
    bones = biped(legs=13, body=(9, 14, 5), head=8, arm=(4, 14), leg_w=4)
    top = 27
    bones[2].cubes.append(Cube([-4.5, top, -4.5], [9, 9, 9], "helmet"))
    bones[2].cubes.append(Cube([-0.5, top + 9, -3], [1, 3, 7], "plume"))
    bones.append(Bone("sword", [-6.5, 13, 0], "right_arm", None, [
        Cube([-7, 12, -22], [1, 3, 18], "steel"),
        Cube([-8.5, 12.5, -4], [4, 2, 1], "steel"),
        Cube([-7, 12.5, -3], [1, 2, 4], "wood"),
    ]))

    def cut(windup, length, start, end, body=(0, 0)):
        return anim(length, {
            "right_arm": rot(0, ZERO, windup, start, windup + 0.1, end, length, ZERO),
            "body": rot(0, ZERO, windup, [0, body[0], 0], windup + 0.1, [0, body[1], 0], length, ZERO),
        }, loop=False)

    animations = {
        "misc.idle": idle(3.0, 2),
        "move.walk": walk(1.1, 22, 10),
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
    paint = painter({"default": (44, 42, 46, 255), "helmet": (38, 36, 40, 255), "plume": (150, 40, 20, 255),
                     "steel": (110, 106, 104, 255), "wood": (70, 50, 36, 255), "head": (60, 40, 30, 255)},
                    eye=(255, 80, 40, 255), eye_tags=("helmet",), eye_row=0.45,
                    cracks=("body", "limb"), crack_color=(255, 110, 30, 255))
    write_mob("smoldering_knight", bones, 64, [3, 3], animations, paint)

# The 10 zone mobs moved to tools/reach_mob_art.py (final art, PLAN R40.2b); the Citadel mobs follow in R40.2c.
REACH_MOBS = [
    ("cinder_hound", cinder_hound), ("sparking_marksman", sparking_marksman),
    ("furnace_golem", furnace_golem), ("smoldering_knight", smoldering_knight),
]


if __name__ == "__main__":
    import sys
    # No arguments: every mob. Otherwise only the named ones, e.g. python3 tools/mob_placeholders.py crypt_moth
    wanted = set(sys.argv[1:])
    for name, build in [("ash_ghoul", ash_ghoul), ("hollow", hollow), ("ember_wisp", ember_wisp)] + REACH_MOBS:
        if not wanted or name in wanted:
            build()
    print("Wrote placeholder mob assets under", ROOT)
