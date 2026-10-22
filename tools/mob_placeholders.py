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


# Fields — Cinder Husk: burnt peasant with a pitchfork. Swing: 10-tick raised-arm windup.
def cinder_husk():
    bones = biped(hunch=8)
    bones.append(Bone("tool", [-6, 12, 0], "right_arm", None, [
        Cube([-6.5, 11, -10], [1, 1, 16], "wood"),
        Cube([-7.5, 11, -11], [3, 1, 1], "iron"),
        Cube([-7.5, 11, -14], [1, 1, 3], "iron"),
        Cube([-5.5, 11, -14], [1, 1, 3], "iron"),
    ]))
    animations = {
        "misc.idle": idle(),
        "move.walk": walk(1.1, 25, 15),
        "attack.swing": overhead(0.5, 0.9, ("right_arm", "left_arm")),
    }
    paint = painter({"default": (60, 52, 46, 255), "body": (64, 56, 50, 255), "head": (78, 66, 58, 255),
                     "wood": (92, 70, 48, 255), "iron": (80, 80, 84, 255)},
                    eye=(255, 150, 50, 255), cracks=("body", "limb", "head"), crack_color=(230, 110, 40, 255))
    write_mob("cinder_husk", bones, 64, [2, 2.5], animations, paint)


# Fields — Ash Stalker: lean, crouched, long clawed arms; buried in a furrow until it boils out (20 ticks) and leaps.
def ash_stalker():
    bones = biped(legs=11, body=(7, 10, 4), head=7, arm=(3, 14), leg_w=3, hunch=25)
    animations = {
        "misc.idle": idle(2.0, 5),
        "move.walk": walk(0.6, 40, 30),
        "misc.buried": anim(1.0, {"root": pos(0, [0, -30, 0], 1, [0, -30, 0])}),
        "misc.emerge": anim(1.0, {
            "root": pos(0, [0, -24, 0], 0.3, [0.5, -22, 0], 0.5, [-0.5, -20, 0], 0.7, [0.5, -18, 0],
                        0.9, [0, -16, 0], 1.0, ZERO),
        }, loop=False),
        "attack.leap": anim(0.6, {
            "right_arm": rot(0, [-90, 0, 20], 0.6, [-20, 0, 0]),
            "left_arm": rot(0, [-90, 0, -20], 0.6, [-20, 0, 0]),
            "right_leg": rot(0, [40, 0, 0], 0.6, ZERO),
            "left_leg": rot(0, [40, 0, 0], 0.6, ZERO),
        }, loop=False),
        "attack.swing": anim(0.7, {
            "right_arm": rot(0, ZERO, 0.4, [-130, 0, 30], 0.5, [-10, 0, -20], 0.7, ZERO),
            "body": rot(0, ZERO, 0.4, [0, 20, 0], 0.5, [0, -20, 0], 0.7, ZERO),
        }, loop=False),
    }
    paint = painter({"default": (110, 104, 98, 255), "head": (96, 90, 86, 255)},
                    eye=(255, 120, 40, 255), cracks=("body",), crack_color=(200, 90, 40, 255), noise=16)
    write_mob("ash_stalker", bones, 64, [2, 2], animations, paint)


# Fields — Ashen Carrion: big carrion bird. Shriek (15 ticks, head up, wings spread) before each dive.
def ashen_carrion():
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 8, 0], "root", None, [Cube([-3, 5, -5], [6, 5, 10], "body")]),
        Bone("head", [0, 9, -5], "body", None, [Cube([-2, 8, -9], [4, 4, 4], "head"), Cube([-1, 8, -12], [2, 2, 3], "beak")]),
        Bone("right_wing", [-3, 10, 0], "body", None, [Cube([-17, 9.5, -4], [14, 1, 8], "wing")]),
        Bone("right_wing_tip", [-17, 10, 0], "right_wing", None, [Cube([-25, 9.5, -3], [8, 1, 6], "wing")]),
        Bone("left_wing", [3, 10, 0], "body", None, [Cube([3, 9.5, -4], [14, 1, 8], "wing")]),
        Bone("left_wing_tip", [17, 10, 0], "left_wing", None, [Cube([17, 9.5, -3], [8, 1, 6], "wing")]),
        Bone("tail", [0, 8, 5], "body", None, [Cube([-2, 7, 5], [4, 1, 5], "wing")]),
        Bone("right_leg", [-1.5, 5, 1], "body", None, [Cube([-2, 0, 0.5], [1, 5, 1], "leg")]),
        Bone("left_leg", [1.5, 5, 1], "body", None, [Cube([1, 0, 0.5], [1, 5, 1], "leg")]),
    ]

    def flap(length, angle, tip):
        h = length / 2
        return {
            "right_wing": rot(0, [0, 0, angle], h, [0, 0, -angle], length, [0, 0, angle]),
            "left_wing": rot(0, [0, 0, -angle], h, [0, 0, angle], length, [0, 0, -angle]),
            "right_wing_tip": rot(0, [0, 0, tip], h, [0, 0, -tip], length, [0, 0, tip]),
            "left_wing_tip": rot(0, [0, 0, -tip], h, [0, 0, tip], length, [0, 0, -tip]),
        }

    animations = {
        "move.fly": anim(0.8, flap(0.8, 25, 15)),
        "misc.idle": anim(3.0, {
            "right_wing": rot(0, [0, 0, -80], 3, [0, 0, -80]),
            "left_wing": rot(0, [0, 0, 80], 3, [0, 0, 80]),
            "head": rot(0, ZERO, 1.5, [15, 10, 0], 3, ZERO),
        }),
        "attack.shriek": anim(0.75, {
            "head": rot(0, ZERO, 0.3, [-40, 0, 0], 0.75, [-40, 0, 0]),
            "right_wing": rot(0, ZERO, 0.3, [0, 0, 35], 0.75, [0, 0, 35]),
            "left_wing": rot(0, ZERO, 0.3, [0, 0, -35], 0.75, [0, 0, -35]),
        }, loop=False),
        "attack.dive": anim(1.0, {
            "body": rot(0, [30, 0, 0], 1, [30, 0, 0]),
            "right_wing": rot(0, [0, 55, 0], 1, [0, 55, 0]),
            "left_wing": rot(0, [0, -55, 0], 1, [0, -55, 0]),
        }),
        "attack.peck": anim(0.4, {"head": rot(0, ZERO, 0.15, [-20, 0, 0], 0.25, [40, 0, 0], 0.4, ZERO)}, loop=False),
    }
    paint = painter({"default": (40, 38, 40, 255), "wing": (34, 32, 36, 255), "beak": (150, 140, 110, 255),
                     "leg": (120, 100, 80, 255)}, eye=(255, 70, 40, 255), eye_row=0.25)
    write_mob("ashen_carrion", bones, 64, [3.5, 1.5], animations, paint)


# Woods — Withered Thrall: hanged poacher with the noose still on. Hangs in a cage; drops on a player below.
def withered_thrall():
    bones = biped(legs=13, body=(7, 12, 3), head=7, arm=(3, 13), leg_w=3)
    top = 25
    bones[2].rotation = [0, 0, 18]  # the broken neck
    bones.append(Bone("noose", [0, top, 0], "head", None, [
        Cube([-3.5, top, -2], [7, 1, 4], "rope"),
        Cube([-0.5, top + 7, -0.5], [1, 10, 1], "rope"),
    ]))
    limp = [10, 0, 0]
    animations = {
        "misc.idle": idle(3.0, 3),
        "move.walk": walk(1.2, 25, 10, body=rot(0, [0, 0, 4], 0.6, [0, 0, -4], 1.2, [0, 0, 4])),
        "misc.hang": anim(4.0, {
            "root": rot(0, [0, 0, 3], 2, [0, 0, -3], 4, [0, 0, 3]),
            "right_leg": rot(0, limp, 4, limp), "left_leg": rot(0, [5, 0, 0], 4, [5, 0, 0]),
            "right_arm": rot(0, [0, 0, 5], 4, [0, 0, 5]), "left_arm": rot(0, [0, 0, -5], 4, [0, 0, -5]),
        }),
        "attack.drop": anim(0.5, {
            "right_arm": rot(0, ZERO, 0.2, [-160, 0, 30], 0.5, [-90, 0, 0]),
            "left_arm": rot(0, ZERO, 0.2, [-160, 0, -30], 0.5, [-90, 0, 0]),
            "right_leg": rot(0, ZERO, 0.3, [-30, 0, 0], 0.5, ZERO),
            "left_leg": rot(0, ZERO, 0.3, [-30, 0, 0], 0.5, ZERO),
        }, loop=False),
        "attack.swing": overhead(0.5, 0.8, ("right_arm", "left_arm")),
    }
    paint = painter({"default": (100, 104, 86, 255), "body": (70, 62, 50, 255), "rope": (150, 120, 80, 255)},
                    eye=(220, 220, 120, 255))
    write_mob("withered_thrall", bones, 64, [2, 3], animations, paint)


# Woods — Lantern Hag: hunched crone with a lantern and fire flasks. Raises the lantern 15 ticks before a throw.
def lantern_hag():
    bones = biped(legs=10, body=(8, 14, 5), head=7, arm=(3, 12), leg_w=3, hunch=20)
    bones.append(Bone("skirt", [0, 10, 0], "root", None, [Cube([-4.5, 1, -3], [9, 10, 6], "robe")]))
    bones.append(Bone("lantern", [5.5, 13, 0], "left_arm", None, [
        Cube([5.5, 12, -0.5], [1, 2, 1], "iron"),
        Cube([4.5, 8, -1.5], [3, 4, 3], "lantern"),
    ]))
    bones.append(Bone("flask", [-5.5, 12, 0], "right_arm", None, [Cube([-6.5, 9, -1], [2, 3, 2], "flask")]))
    animations = {
        "misc.idle": idle(3.0, 4),
        "move.walk": walk(1.2, 20, 8),
        "attack.throw": anim(1.0, {
            "left_arm": rot(0, ZERO, 0.6, [-165, 0, -10], 0.75, [-165, 0, -10], 1.0, ZERO),
            "right_arm": rot(0, ZERO, 0.6, [45, 0, 0], 0.8, [-130, 0, 0], 1.0, ZERO),
            "body": rot(0, ZERO, 0.6, [0, -20, 0], 0.8, [0, 15, 0], 1.0, ZERO),
        }, loop=False),
    }
    paint = painter({"default": (120, 110, 96, 255), "body": (58, 46, 40, 255), "robe": (52, 42, 36, 255),
                     "head": (110, 100, 88, 255), "iron": (70, 70, 74, 255), "lantern": (255, 170, 60, 255),
                     "flask": (220, 120, 50, 255)},
                    eye=(255, 200, 90, 255), glow_tags=("lantern", "flask"))
    write_mob("lantern_hag", bones, 64, [2, 2.5], animations, paint)


# Woods — Bark Hollow: a dead tree that walks. Dormant until a player is within 6; 20-tick windup on the sweep.
def bark_hollow():
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 12, 0], "root", None, [Cube([-7, 12, -5], [14, 26, 10], "bark")]),
        Bone("eyes", [0, 31, -5], "body", None, [Cube([-4, 31, -5.5], [2, 1, 1], "eye"), Cube([2, 31, -5.5], [2, 1, 1], "eye")]),
        Bone("crown", [0, 38, 0], "body", None, [
            Cube([-5, 38, -4], [10, 6, 8], "bark"),
            Cube([-9, 42, -1], [4, 8, 2], "branch"),
            Cube([5, 41, -1], [3, 10, 2], "branch"),
        ]),
        Bone("right_arm", [-9, 34, 0], "body", None, [
            Cube([-12, 12, -2.5], [5, 24, 5], "bark"), Cube([-13, 9, -1], [2, 4, 2], "branch")]),
        Bone("left_arm", [9, 34, 0], "body", None, [
            Cube([7, 12, -2.5], [5, 24, 5], "bark"), Cube([11, 9, -1], [2, 4, 2], "branch")]),
        Bone("right_leg", [-3.5, 12, 0], "root", None, [Cube([-6, 0, -3], [5, 12, 6], "bark")]),
        Bone("left_leg", [3.5, 12, 0], "root", None, [Cube([1, 0, -3], [5, 12, 6], "bark")]),
    ]
    raised_r, raised_l = [0, 0, -35], [0, 0, 35]
    animations = {
        "misc.dormant": anim(2.0, {"right_arm": rot(0, raised_r, 2, raised_r), "left_arm": rot(0, raised_l, 2, raised_l)}),
        "misc.awaken": anim(1.0, {
            "body": rot(0, ZERO, 0.2, [0, 0, 3], 0.4, [0, 0, -3], 0.6, [0, 0, 2], 0.8, ZERO),
            "right_arm": rot(0, raised_r, 1.0, ZERO),
            "left_arm": rot(0, raised_l, 1.0, ZERO),
        }, loop=False),
        "misc.idle": idle(4.0, 2),
        "move.walk": walk(1.6, 18, 10, body=rot(0, [0, 0, 3], 0.8, [0, 0, -3], 1.6, [0, 0, 3])),
        "attack.sweep": anim(1.6, {
            "right_arm": rot(0, ZERO, 1.0, [-70, 0, -70], 1.15, [-80, 0, 50], 1.6, ZERO),
            "body": rot(0, ZERO, 1.0, [0, 40, 0], 1.15, [0, -50, 0], 1.6, ZERO),
        }, loop=False),
    }
    paint = painter({"default": (66, 54, 44, 255), "branch": (80, 66, 52, 255), "eye": (255, 190, 80, 255)},
                    glow_tags=("eye",), noise=14)
    write_mob("bark_hollow", bones, 64, [4, 3.5], animations, paint)


# Necropolis — Restless Bones: skeleton with a rusty sword. Collapses into a bone pile at 0 HP and re-forms.
def restless_bones():
    bones = biped(body=(8, 12, 2), arm=(2, 12), leg_w=2)
    bones.append(Bone("sword", [-5, 12, 0], "right_arm", None, [
        Cube([-5.5, 11, -12], [1, 1, 11], "iron"),
        Cube([-6.5, 11, -2], [3, 1, 1], "iron"),
    ]))
    pile = {
        "root": pos(0, [0, -11, 0]),
        "body": rot(0, [85, 0, 10]),
        "head": rot(0, [-60, 30, 0]),
        "right_arm": rot(0, [-20, 0, 80]), "left_arm": rot(0, [10, 0, -85]),
        "right_leg": rot(0, [80, 30, 0]), "left_leg": rot(0, [80, -40, 0]),
    }

    def at(time, frames):
        """Same pose bones with their single keyframe moved to time."""
        return {b: {k: {str(float(time)): list(v.values())[0]} for k, v in ch.items()} for b, ch in frames.items()}

    def merge(*parts):
        out = {}
        for part in parts:
            for b, ch in part.items():
                for k, v in ch.items():
                    out.setdefault(b, {}).setdefault(k, {}).update(v)
        return out

    standing = {b: {k: {"0.0": ZERO} for k in ch} for b, ch in pile.items()}
    animations = {
        "misc.idle": idle(3.0, 3),
        "move.walk": walk(1.0, 30, 25),
        "attack.swing": overhead(0.5, 0.9),
        "misc.collapse": anim(0.5, merge(standing, at(0.5, pile)), loop=False),
        "misc.pile": anim(1.0, merge(pile, at(1.0, pile))),
        "misc.reform": anim(1.0, merge(pile, at(1.0, standing)), loop=False),
    }
    paint = painter({"default": (205, 198, 175, 255), "iron": (110, 100, 90, 255)}, eye=(170, 210, 255, 255))

    def paint_bones(tex, glow, bone, cube, rng):
        paint(tex, glow, bone, cube, rng)
        if cube.tag == "head":
            x, y, w, h = cube.faces()["front"]
            for ex in (x + 1, x + w - 3):
                tex.rect(ex, y + 3, 2, 2, (20, 20, 24, 255))
                tex.set(ex + 1, y + 4, (170, 210, 255, 255))
            tex.rect(x + 2, y + 6, w - 4, 1, (60, 55, 45, 255))

    write_mob("restless_bones", bones, 64, [2, 2.5], animations, paint_bones)


# Necropolis — Crypt Moth: big dusty moth. Its wings glow (the glow bones, shown by the renderer) 15 ticks before
# the pollen burst.
def crypt_moth():
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 5, 0], "root", None, [Cube([-1.5, 3, -3], [3, 3, 7], "body")]),
        Bone("head", [0, 5, -3], "body", None, [
            Cube([-1.5, 3.5, -5.5], [3, 3, 3], "head"),
            Cube([-1.5, 6.5, -6], [1, 3, 1], "body"),
            Cube([0.5, 6.5, -6], [1, 3, 1], "body"),
        ]),
        Bone("right_wing", [-1.5, 6, -1], "body", None, [Cube([-10.5, 6, -4], [9, 1, 7], "wing")]),
        Bone("glow_right", [-1.5, 6, -1], "right_wing", None, [Cube([-10.5, 6, -4], [9, 1, 7], "glow", 0.05)]),
        Bone("left_wing", [1.5, 6, -1], "body", None, [Cube([1.5, 6, -4], [9, 1, 7], "wing")]),
        Bone("glow_left", [1.5, 6, -1], "left_wing", None, [Cube([1.5, 6, -4], [9, 1, 7], "glow", 0.05)]),
        Bone("right_wing_low", [-1.5, 5, 2], "body", None, [Cube([-7.5, 5, 1], [6, 1, 5], "wing")]),
        Bone("left_wing_low", [1.5, 5, 2], "body", None, [Cube([1.5, 5, 1], [6, 1, 5], "wing")]),
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
        "move.fly": anim(0.3, flap(0.3, 40)),
        "misc.idle": anim(0.6, flap(0.6, 25)),
        "attack.burst": anim(0.75, {
            "right_wing": rot(0, ZERO, 0.1, [0, 0, 8], 0.2, [0, 0, -8], 0.3, [0, 0, 8], 0.4, [0, 0, -8], 0.5, [0, 0, 8],
                              0.6, [0, 0, -8], 0.75, [0, 0, 50]),
            "left_wing": rot(0, ZERO, 0.1, [0, 0, -8], 0.2, [0, 0, 8], 0.3, [0, 0, -8], 0.4, [0, 0, 8], 0.5, [0, 0, -8],
                             0.6, [0, 0, 8], 0.75, [0, 0, -50]),
            "root": pos(0, ZERO, 0.75, [0, 2, 0]),
        }, loop=False),
        "attack.bite": anim(0.4, {"head": rot(0, ZERO, 0.15, [-20, 0, 0], 0.25, [30, 0, 0], 0.4, ZERO)}, loop=False),
    }
    paint = painter({"default": (120, 110, 96, 255), "wing": (140, 130, 116, 255), "glow": (220, 255, 160, 255)},
                    eye=(230, 240, 180, 255), glow_tags=("glow",), eye_row=0.3, noise=10)
    write_mob("crypt_moth", bones, 64, [1.5, 1], animations, paint)


# Necropolis — Hollow Sentinel: armoured spearman behind a tower shield. Thrust after a 12-tick draw; bash after the
# shield is planted for 15 ticks, then open (shield down) for 30 ticks.
def hollow_sentinel():
    bones = biped(legs=13, body=(10, 14, 6), head=8, arm=(4, 14), leg_w=5)
    top = 27
    bones[2].cubes.append(Cube([-4.5, top, -4.5], [9, 9, 9], "helmet"))
    bones.append(Bone("shield", [7, 14, -3], "left_arm", None, [Cube([-1, 6, -6], [12, 24, 1], "shield")]))
    bones.append(Bone("spear", [-7, 13, 0], "right_arm", None, [
        Cube([-7.5, 13, -20], [1, 1, 30], "wood"),
        Cube([-8, 12.5, -23], [2, 2, 3], "iron"),
    ]))
    guard = [-25, -20, 0]
    animations = {
        "misc.idle": idle(3.0, 2, left_arm=rot(0, guard, 3, guard)),
        "move.walk": walk(1.2, 20, 0, left_arm=rot(0, guard, 1.2, guard)),
        "misc.guard": anim(1.0, {"left_arm": rot(0, [-35, -30, 0], 1, [-35, -30, 0]), "body": rot(0, [5, 0, 0], 1, [5, 0, 0])}),
        "attack.thrust": anim(0.9, {
            "right_arm": rot(0, ZERO, 0.6, [35, 0, 0], 0.65, [-90, 0, 0], 0.9, ZERO),
            "body": rot(0, ZERO, 0.6, [0, 25, 0], 0.65, [0, -15, 0], 0.9, ZERO),
            "left_arm": rot(0, guard, 0.9, guard),
        }, loop=False),
        "attack.bash": anim(1.1, {
            "body": rot(0, ZERO, 0.75, [12, 0, 0], 0.85, [-5, 0, 0], 1.1, ZERO),
            "left_arm": rot(0, guard, 0.75, [-40, -10, 0], 0.85, [-85, 0, 0], 1.1, guard),
            "root": pos(0, ZERO, 0.75, [0, 0, 1], 0.85, [0, 0, -4], 1.1, ZERO),
        }, loop=False),
        "misc.open": anim(1.5, {
            "left_arm": rot(0, [20, 0, -25], 1.5, [20, 0, -25]),
            "body": rot(0, [15, 0, 0], 0.75, [18, 0, 0], 1.5, [15, 0, 0]),
            "head": rot(0, [20, 0, 0], 1.5, [20, 0, 0]),
        }),
    }
    paint = painter({"default": (70, 74, 82, 255), "helmet": (62, 66, 74, 255), "shield": (88, 80, 70, 255),
                     "wood": (90, 72, 52, 255), "iron": (120, 120, 126, 255), "head": (190, 182, 160, 255)},
                    eye=(150, 200, 255, 255), eye_tags=("helmet",), eye_row=0.45)
    write_mob("hollow_sentinel", bones, 64, [3, 3], animations, paint)


# Necropolis — Grave Warden: robed bell-ringer with a cudgel. Raises the bell for 20 ticks before it rings.
def grave_warden():
    bones = biped(legs=12, body=(12, 16, 7), head=8, arm=(5, 15), leg_w=5, hunch=10)
    top = 28
    bones[2].cubes.append(Cube([-4.5, top, -4.5], [9, 9, 9], "hood"))
    bones.append(Bone("robe", [0, 12, 0], "root", None, [Cube([-6.5, 2, -4], [13, 10, 8], "robe")]))
    bones.append(Bone("bell", [8.5, 13, 0], "left_arm", None, [
        Cube([8, 9, -0.5], [1, 4, 1], "wood"),
        Cube([5.5, 3, -3], [6, 6, 6], "bell"),
        Cube([5, 2, -3.5], [7, 1, 7], "bell"),
    ]))
    bones.append(Bone("cudgel", [-8.5, 13, 0], "right_arm", None, [
        Cube([-9.5, 12, -14], [2, 2, 14], "wood"),
        Cube([-10.5, 11, -17], [4, 4, 4], "iron"),
    ]))
    ring = keys(0, ZERO, 1.0, ZERO, 1.1, [0, 0, 25], 1.2, [0, 0, -25], 1.3, [0, 0, 20], 1.4, ZERO)
    animations = {
        "misc.idle": idle(3.5, 3),
        "move.walk": walk(1.2, 20, 12),
        "attack.swing": overhead(0.6, 1.1),
        "attack.bell": anim(1.6, {
            "left_arm": rot(0, ZERO, 1.0, [-170, 0, -10], 1.4, [-170, 0, -10], 1.6, ZERO),
            "bell": {"rotation": ring},
            "head": rot(0, ZERO, 1.0, [-20, 0, 0], 1.6, ZERO),
        }, loop=False),
    }
    paint = painter({"default": (48, 46, 54, 255), "hood": (40, 38, 46, 255), "robe": (44, 42, 50, 255),
                     "head": (170, 165, 150, 255), "limb": (52, 50, 58, 255), "wood": (90, 72, 52, 255),
                     "iron": (100, 100, 106, 255), "bell": (150, 110, 50, 255)},
                    eye=(255, 220, 140, 255), eye_tags=("hood",), eye_row=0.45)
    write_mob("grave_warden", bones, 64, [3, 3], animations, paint)


REACH_MOBS = [
    ("cinder_husk", cinder_husk), ("ash_stalker", ash_stalker), ("ashen_carrion", ashen_carrion),
    ("withered_thrall", withered_thrall), ("lantern_hag", lantern_hag), ("bark_hollow", bark_hollow),
    ("restless_bones", restless_bones), ("crypt_moth", crypt_moth), ("hollow_sentinel", hollow_sentinel),
    ("grave_warden", grave_warden),
]


if __name__ == "__main__":
    import sys
    # No arguments: every mob. Otherwise only the named ones, e.g. python3 tools/mob_placeholders.py crypt_moth
    wanted = set(sys.argv[1:])
    for name, build in [("ash_ghoul", ash_ghoul), ("hollow", hollow), ("ember_wisp", ember_wisp)] + REACH_MOBS:
        if not wanted or name in wanted:
            build()
    print("Wrote placeholder mob assets under", ROOT)
