#!/usr/bin/env python3
"""Renders Embervoid models, textures, structure templates and animation motion to PNG / GIF without a Minecraft client
(PLAN R40.2a, 19.1).

Run from the repository root. Pure Python (no numpy / PIL): a small software rasteriser with a z-buffer.

    python3 tools/model_preview.py entity cinder_husk [more names] [--out DIR] [--anims all|a,b] [--hide a,b]
    python3 tools/model_preview.py entities             every model under geo/entity
    python3 tools/model_preview.py items [names]        item textures (8x), next to vanilla references
    python3 tools/model_preview.py blocks [names]       block models (3D, from their JSON) + 3x3 tiling
    python3 tools/model_preview.py armor [materials]    armor layers on a mannequin
    python3 tools/model_preview.py gui [names]          GUI textures (4x)
    python3 tools/model_preview.py texture PATH...      any PNG, upscaled
    python3 tools/model_preview.py check [names]        what code relies on: animations, toggled bones, head,
                                                        windups, size against tools/model_bounds.json; without
                                                        names the self-check below runs first
    python3 tools/model_preview.py self-check           the tool's own parts (GIF coder, cameras) and every
                                                        structure template read, its block states resolved
    python3 tools/model_preview.py motion NAME... [--anims a,b] [--fps 10] [--gif-fps 20] [--yaw 35] [--no-gif]
    python3 tools/model_preview.py structure NAME... [--view top,iso,eye|all] [--eye X,Y,Z] [--look N|E|S|W|YAW]
                                                     [--pitch DEG] [--ground Y|none]
    python3 tools/model_preview.py structures [PREFIX...]  every template (whose path starts with a prefix)
    python3 tools/model_preview.py player [CLIP...] [--no-gif]   player attack animations (assets/embervoid/
                                                     player_animation, PLAN 17.1) on the vanilla player model

Output goes to build/model-preview/ unless --out is given. For an entity it writes
  <name>.png        rest pose from six angles with the hitbox (green wireframe, read from ReachMobs / ModEntities
                    registrations) and a 1-block ground grid; a second row at dusk light so the glowmask shows;
                    the texture and glowmask atlases
  <name>_anims.png  one row per animation: key poses at the animation's keyframe times (front-right view)

What it follows from GeckoLib 4 (so the picture matches the game's geometry): Bedrock geometry with box or per-face
UV, X mirrored into render space, bone rotation order Z-Y-X around the pivot, animation rotation added to the rest
rotation (X and Y negated), animation position (X negated), scale; hidden bones hide their children; the base layer
is cutout without culling, the glowmask is added full bright (AutoGlowingGeoLayer). Lighting is vanilla's two
entity lights (ambient 0.4). What it cannot show: in-game light levels, fog, particles, render-time code (head
turning, bones the entity class hides or shows at times), animation blending and timing feel.

motion writes (PLAN 19.1, to judge timing, secondary motion, hit and death animations, not only key poses)
  <name>_motion.png         per animation a TRAIL cell (the first frame dimmed under the outlines of 8 evenly spaced
                            frames, blue at the start to orange at the end: where outlines fan out, that part moves),
                            then every frame at --fps (10 a row at 10 fps), one camera and scale for the whole sheet
  <name>_<anim>.gif         the animation looping at --gif-fps (a one-shot holds its last frame half a second), with
                            the time and a progress bar; a pure-Python GIF writer (median-cut palette, no dithering)
Keyframes are interpolated with their GeckoLib easing (a key's easing shapes the segment into it; catmullrom
drawn as linear); molang is evaluated per frame.

structure renders a template (.nbt under data/embervoid/structures, named by its path or a unique tail of it:
ashen_reach/fields/barn, fields/barn, barn) into build/model-preview/structures/:
  <path>_top.png            straight down, north up, rulers in blocks (the X / Z to give --eye), lower surfaces
                            darker with an edge where the height steps; the placed-enemy key on the right
  <path>_iso.png            orthographic from above the south-east and the north-west corner, the template's box
  <path>_eye.png            perspective at eye level (1.62 above the ground, vertical FOV 70, fog from 24 blocks)
                            walking up from each side; with --eye X,Y,Z (feet in that block, Y = the ground layer
                            + 1 to stand on it) one large view from there, facing --look (default: the middle) and
                            --pitch (> 0 looks down)
Placed enemies (realm.placed markers, PLAN 9.2) and template entities are numbered pins: a stalk two blocks up from
the feet (on top views a dot), a tick toward the facing, dashed with a hollow label when something is in front of or
above it. Blocks come from their blockstates and models (variants, multipart, x / y rotation, element rotation, biome
tint by a fixed colour), faces against an opaque full cube are culled; chests, skulls and fluids are boxes with a
texture, any other model without elements a full cube with its particle texture. The ground plane sits at the top of
the ground layer (y = 0; START_DEPTH and the Citadel's FOUNDATION are read from the template classes), --ground none
leaves it out. Not shown: processors (decay rules, the casing gap), uvlock (a turned stair's top shows its texture
turned), light levels and ambient occlusion, emissive textures, entity models (only pins), translucency (water is
opaque).

player renders each playerAnimator clip (all without names) on Steve as playerAnimator poses him: the keyed parts
set absolutely (degrees, Z-Y-X like a ModelPart), "body" turning the whole player around a point 0.7 blocks up,
keys interpolated with their easing, held after the last key and blended back into the standing pose (the right arm
holding an item) from endTick to stopTick. The held item is a sword-sized bar through the vanilla handheld
third-person transform and the clip's rightItem turn, not the weapon's own model; the whip clips hold the Warden's
Scourge's handle and its lash (WhipArc's swing and ScourgeLashes' rope, ported here, the player looking level ahead).
Into build/model-preview/:
  player_<clip>.png         per view (SIDE from the front right, ABOVE from behind and above) a trail cell and every
                            tick (every second tick past 24)
  player_<clip>.gif         both views side by side at 20 fps (game time)
Not shown: the fade-in from the pose before (2 ticks), the client stretching a clip to the attack speed, mirroring
for the left hand, bends, first person (stays vanilla), walking legs under the clip.

Vanilla parents and textures (block models, the armor model, reference items) come from Loom's local copy of the
1.20.1 client jar in ~/.gradle/caches; nothing is downloaded.
"""
import collections
import glob
import gzip
import json
import math
import os
import re
import struct
import sys
import zipfile
import zlib

ASSETS = os.path.join("src", "main", "resources", "assets")
GENERATED = os.path.join("src", "main", "generated", "assets")
MOD = "embervoid"
DEFAULT_OUT = os.path.join("build", "model-preview")
BACKGROUND = (54, 58, 66)
DUSK_BACKGROUND = (22, 18, 22)
DUSK_LIGHT = 0.32


# --- PNG ----------------------------------------------------------------------------------------------------------

class Texture:
    """RGBA pixels, row-major bytearray."""

    def __init__(self, width, height, data=None):
        self.width, self.height = width, height
        self.data = data if data is not None else bytearray(width * height * 4)

    def get(self, x, y):
        i = (y * self.width + x) * 4
        d = self.data
        return d[i], d[i + 1], d[i + 2], d[i + 3]

    def first_frame(self):
        """An animated texture (one with a .mcmeta) is a vertical strip of square frames."""
        if self.height > self.width and self.height % self.width == 0:
            return Texture(self.width, self.width, self.data[:self.width * self.width * 4])
        return self


def read_png(data):
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG")
    pos, idat, palette, trns = 8, b"", None, None
    width = height = depth = ctype = interlace = 0
    while pos < len(data):
        length, tag = struct.unpack(">I4s", data[pos:pos + 8])
        chunk = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if tag == b"IHDR":
            width, height, depth, ctype, _, _, interlace = struct.unpack(">IIBBBBB", chunk)
        elif tag == b"PLTE":
            palette = [tuple(chunk[i:i + 3]) for i in range(0, len(chunk), 3)]
        elif tag == b"tRNS":
            trns = chunk
        elif tag == b"IDAT":
            idat += chunk
        elif tag == b"IEND":
            break
    if interlace:
        raise ValueError("interlaced PNG not supported")
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    bits = depth * channels
    stride = (width * bits + 7) // 8
    bpp = max(1, bits // 8)
    raw = zlib.decompress(idat)
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(height):
        f = raw[i]
        line = bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        if f == 1:
            for x in range(bpp, stride):
                line[x] = (line[x] + line[x - bpp]) & 255
        elif f == 2:
            for x in range(stride):
                line[x] = (line[x] + prev[x]) & 255
        elif f == 3:
            for x in range(stride):
                left = line[x - bpp] if x >= bpp else 0
                line[x] = (line[x] + ((left + prev[x]) >> 1)) & 255
        elif f == 4:
            for x in range(stride):
                a = line[x - bpp] if x >= bpp else 0
                b = prev[x]
                c = prev[x - bpp] if x >= bpp else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if pa <= pb and pa <= pc else (b if pb <= pc else c)
                line[x] = (line[x] + pr) & 255
        rows.append(line)
        prev = line
    out = bytearray(width * height * 4)
    o = 0
    for line in rows:
        if depth == 8 and ctype == 6:
            out[o:o + width * 4] = line
            o += width * 4
            continue
        for x in range(width):
            if depth == 8:
                s = line[x * channels:(x + 1) * channels]
                if ctype == 2:
                    px = (s[0], s[1], s[2], 255)
                    if trns and len(trns) >= 6 and (s[0], s[1], s[2]) == struct.unpack(">HHH", trns[:6]):
                        px = (s[0], s[1], s[2], 0)
                elif ctype == 0:
                    px = (s[0], s[0], s[0], 255)
                elif ctype == 4:
                    px = (s[0], s[0], s[0], s[1])
                else:
                    idx = s[0]
                    r, g, b = palette[idx]
                    px = (r, g, b, trns[idx] if trns and idx < len(trns) else 255)
            else:  # sub-byte palette or grey
                per = 8 // depth
                byte = line[x // per]
                v = (byte >> (8 - depth * (x % per + 1))) & ((1 << depth) - 1)
                if ctype == 3:
                    r, g, b = palette[v]
                    px = (r, g, b, trns[v] if trns and v < len(trns) else 255)
                else:
                    g = v * 255 // ((1 << depth) - 1)
                    px = (g, g, g, 255)
            out[o:o + 4] = bytes(px)
            o += 4
    return Texture(width, height, out)


def write_png(path, width, height, rgb):
    """rgb: flat list of (r, g, b) tuples."""
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        for r, g, b in rgb[y * width:(y + 1) * width]:
            raw += bytes((r, g, b))

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 6)) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


# --- Resources: this mod's assets, then the vanilla client jar ------------------------------------------------------

_JAR = None


def vanilla_jar():
    global _JAR
    if _JAR is None:
        candidates = sorted(glob.glob(os.path.expanduser("~/.gradle/caches/fabric-loom/1.20.1/minecraft-client.jar")))
        _JAR = zipfile.ZipFile(candidates[0]) if candidates else False
    return _JAR or None


def resource(namespace, path):
    """Bytes of assets/<namespace>/<path> from the mod (resources, then datagen output) or the vanilla jar."""
    for base in (ASSETS, GENERATED):
        p = os.path.join(base, namespace, path)
        if os.path.exists(p):
            with open(p, "rb") as f:
                return f.read()
    jar = vanilla_jar()
    if jar:
        try:
            return jar.read("assets/%s/%s" % (namespace, path))
        except KeyError:
            pass
    return None


def split_id(ident, default_ns="minecraft"):
    if ":" in ident:
        ns, path = ident.split(":", 1)
        return ns, path
    return default_ns, ident


_TEXTURES = {}


def texture(namespace, path):
    """A texture by namespace and path below textures/ without .png; None when missing."""
    key = (namespace, path)
    if key not in _TEXTURES:
        data = resource(namespace, "textures/%s.png" % path)
        tex = read_png(data) if data else None
        if tex and resource(namespace, "textures/%s.png.mcmeta" % path):
            tex = tex.first_frame()
        _TEXTURES[key] = tex
    return _TEXTURES[key]


def texture_file(path):
    with open(path, "rb") as f:
        return read_png(f.read())


# --- Image, text, lines --------------------------------------------------------------------------------------------

FONT = {  # 3x5 glyphs, rows top to bottom, 1 = ink
    "A": "010101111101101", "B": "110101110101110", "C": "011100100100011", "D": "110101101101110",
    "E": "111100110100111", "F": "111100110100100", "G": "011100101101011", "H": "101101111101101",
    "I": "111010010010111", "J": "001001001101010", "K": "101101110101101", "L": "100100100100111",
    "M": "101111111101101", "N": "110101101101101", "O": "010101101101010", "P": "110101110100100",
    "Q": "010101101110011", "R": "110101110101101", "S": "011100010001110", "T": "111010010010010",
    "U": "101101101101111", "V": "101101101101010", "W": "101101111111101", "X": "101101010101101",
    "Y": "101101010010010", "Z": "111001010100111", "0": "111101101101111", "1": "010110010010111",
    "2": "110001010100111", "3": "110001010001110", "4": "101101111001001", "5": "111100110001110",
    "6": "011100111101111", "7": "111001010010010", "8": "111101111101111", "9": "111101111001110",
    ".": "000000000000010", "_": "000000000000111", ":": "000010000010000", "-": "000000111000000",
    "/": "001001010100100", "(": "010100100100010", ")": "010001001001010", ",": "000000000010100",
    "%": "101001010100101", "+": "000010111010000", "=": "000111000111000", "x": "000101010101000",
    " ": "000000000000000", "#": "101111101111101", "'": "010010000000000",
}


class Image:
    def __init__(self, width, height, color=BACKGROUND):
        self.width, self.height = width, height
        self.px = [color] * (width * height)

    def set(self, x, y, c):
        if 0 <= x < self.width and 0 <= y < self.height:
            self.px[y * self.width + x] = c

    def blend(self, x, y, c, a):
        if 0 <= x < self.width and 0 <= y < self.height:
            i = y * self.width + x
            o = self.px[i]
            self.px[i] = (int(o[0] + (c[0] - o[0]) * a), int(o[1] + (c[1] - o[1]) * a), int(o[2] + (c[2] - o[2]) * a))

    def fill(self, x, y, w, h, c):
        for yy in range(max(0, y), min(self.height, y + h)):
            row = yy * self.width
            for xx in range(max(0, x), min(self.width, x + w)):
                self.px[row + xx] = c

    def blit(self, other, x, y):
        for yy in range(other.height):
            ty = y + yy
            if 0 <= ty < self.height:
                src = other.px[yy * other.width:(yy + 1) * other.width]
                x0, x1 = max(0, x), min(self.width, x + other.width)
                if x1 > x0:
                    self.px[ty * self.width + x0:ty * self.width + x1] = src[x0 - x:x1 - x]

    def text(self, x, y, s, c=(230, 230, 230), scale=2):
        for ch in s:
            glyph = FONT.get(ch) or FONT.get(ch.upper()) or FONT["#"]
            for i, bit in enumerate(glyph):
                if bit == "1":
                    self.fill(x + (i % 3) * scale, y + (i // 3) * scale, scale, scale, c)
            x += 4 * scale
        return x

    def line(self, x0, y0, x1, y1, c, a=1.0, dash=0):
        steps = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
        for i in range(steps + 1):
            if dash and (i // dash) % 2:
                continue
            t = i / steps
            self.blend(int(round(x0 + (x1 - x0) * t)), int(round(y0 + (y1 - y0) * t)), c, a)

    def save(self, path):
        write_png(path, self.width, self.height, self.px)
        print("wrote", path)


def text_width(s, scale=2):
    return len(s) * 4 * scale


def checker(x, y, size=4):
    return (90, 90, 96) if ((x // size) + (y // size)) % 2 else (120, 120, 128)


def upscale(tex, scale, bg=checker):
    """A texture drawn at scale x scale per texel over a checkerboard (transparent texels show it)."""
    img = Image(tex.width * scale, tex.height * scale)
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = tex.get(x // scale, y // scale)
            base = bg(x, y, max(2, scale)) if bg else BACKGROUND
            t = a / 255
            img.px[y * img.width + x] = (int(base[0] + (r - base[0]) * t), int(base[1] + (g - base[1]) * t),
                                         int(base[2] + (b - base[2]) * t))
    return img


# --- Matrices (3x4 affine, row-major) -------------------------------------------------------------------------------

IDENT = (1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0)


def mul(a, b):
    return (
        a[0] * b[0] + a[1] * b[4] + a[2] * b[8], a[0] * b[1] + a[1] * b[5] + a[2] * b[9],
        a[0] * b[2] + a[1] * b[6] + a[2] * b[10], a[0] * b[3] + a[1] * b[7] + a[2] * b[11] + a[3],
        a[4] * b[0] + a[5] * b[4] + a[6] * b[8], a[4] * b[1] + a[5] * b[5] + a[6] * b[9],
        a[4] * b[2] + a[5] * b[6] + a[6] * b[10], a[4] * b[3] + a[5] * b[7] + a[6] * b[11] + a[7],
        a[8] * b[0] + a[9] * b[4] + a[10] * b[8], a[8] * b[1] + a[9] * b[5] + a[10] * b[9],
        a[8] * b[2] + a[9] * b[6] + a[10] * b[10], a[8] * b[3] + a[9] * b[7] + a[10] * b[11] + a[11],
    )


def translate(x, y, z):
    return (1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z)


def scale_m(x, y, z):
    return (x, 0, 0, 0, 0, y, 0, 0, 0, 0, z, 0)


def rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return (1, 0, 0, 0, 0, c, -s, 0, 0, s, c, 0)


def rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return (c, 0, s, 0, 0, 1, 0, 0, -s, 0, c, 0)


def rot_z(a):
    c, s = math.cos(a), math.sin(a)
    return (c, -s, 0, 0, s, c, 0, 0, 0, 0, 1, 0)


def apply(m, p):
    x, y, z = p
    return (m[0] * x + m[1] * y + m[2] * z + m[3], m[4] * x + m[5] * y + m[6] * z + m[7],
            m[8] * x + m[9] * y + m[10] * z + m[11])


def apply_dir(m, p):
    x, y, z = p
    return (m[0] * x + m[1] * y + m[2] * z, m[4] * x + m[5] * y + m[6] * z, m[8] * x + m[9] * y + m[10] * z)


def zyx(rx, ry, rz):
    """GeckoLib's rotateMatrixAroundBone: Z, then Y, then X on the pose stack (radians)."""
    m = IDENT
    if rz:
        m = mul(m, rot_z(rz))
    if ry:
        m = mul(m, rot_y(ry))
    if rx:
        m = mul(m, rot_x(rx))
    return m


# --- Quads and the rasteriser --------------------------------------------------------------------------------------

class Quad:
    """A planar parallelogram P(a, b) = p0 + a*ea + b*eb (a, b in [0, 1]) with texture coordinates
    uv(a, b) = uv0 + a*ua + b*ub in texels, clamped to the face's texel rectangle (u_lo..u_hi, v_lo..v_hi)."""
    __slots__ = ("p0", "ea", "eb", "uv0", "ua", "ub", "clamp", "tex", "glow", "shade", "color")

    def __init__(self, p0, ea, eb, uv0, ua, ub, tex, glow=None, shade=1.0, color=None):
        self.p0, self.ea, self.eb = p0, ea, eb
        self.uv0, self.ua, self.ub = uv0, ua, ub
        us = (uv0[0], uv0[0] + ua[0], uv0[0] + ub[0], uv0[0] + ua[0] + ub[0])
        vs = (uv0[1], uv0[1] + ua[1], uv0[1] + ub[1], uv0[1] + ua[1] + ub[1])
        self.clamp = (min(us), max(us), min(vs), max(vs))
        self.tex, self.glow, self.shade, self.color = tex, glow, shade, color


def normal_of(q):
    ax, ay, az = q.ea
    bx, by, bz = q.eb
    n = (ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx)
    length = math.sqrt(n[0] ** 2 + n[1] ** 2 + n[2] ** 2) or 1
    return (n[0] / length, n[1] / length, n[2] / length)


L0 = (0.2, 1.0, -0.7)
L1 = (-0.2, 1.0, 0.7)
_l0 = math.sqrt(sum(c * c for c in L0))
_l1 = math.sqrt(sum(c * c for c in L1))
L0 = tuple(c / _l0 for c in L0)
L1 = tuple(c / _l1 for c in L1)


def entity_light(n):
    """Vanilla entity diffuse lighting (two lights, ambient 0.4), cull-less: the side facing the viewer is lit as if
    its normal faced out of it."""
    d0 = n[0] * L0[0] + n[1] * L0[1] + n[2] * L0[2]
    d1 = n[0] * L1[0] + n[1] * L1[1] + n[2] * L1[2]
    return min(1.0, 0.4 + 0.6 * (max(0.0, d0) + max(0.0, d1)))


def block_light(n):
    """Vanilla block face shading: up 1.0, down 0.5, north/south 0.8, east/west 0.6 (blended for tilted faces)."""
    return min(1.0, n[1] * n[1] * (1.0 if n[1] > 0 else 0.5) + n[0] * n[0] * 0.6 + n[2] * n[2] * 0.8)


class View:
    """Orthographic camera: yaw turns the model (0 = its front faces the viewer), pitch > 0 looks down at it."""

    def __init__(self, yaw, pitch, label=""):
        self.yaw, self.pitch, self.label = yaw, pitch, label
        self.m = mul(rot_x(-math.radians(pitch)), rot_y(math.radians(yaw)))

    def project(self, p):
        x, y, z = apply(self.m, p)
        return -x, y, z


class Frame:
    def __init__(self, width, height, bg):
        self.width, self.height = width, height
        n = width * height
        self.depth = [math.inf] * n
        self.base = [bg] * n
        self.glow = [None] * n

    def draw(self, quads, view, scale, cx, cy, light=1.0, lighting=entity_light, world_light=True):
        W, H = self.width, self.height
        depth, base, glowbuf = self.depth, self.base, self.glow
        for q in quads:
            p0 = view.project(q.p0)
            pa = view.project((q.p0[0] + q.ea[0], q.p0[1] + q.ea[1], q.p0[2] + q.ea[2]))
            pb = view.project((q.p0[0] + q.eb[0], q.p0[1] + q.eb[1], q.p0[2] + q.eb[2]))
            sx0, sy0 = cx + p0[0] * scale, cy - p0[1] * scale
            ax, ay = (pa[0] - p0[0]) * scale, -(pa[1] - p0[1]) * scale
            bx, by = (pb[0] - p0[0]) * scale, -(pb[1] - p0[1]) * scale
            det = ax * by - ay * bx
            if abs(det) < 1e-6:
                continue
            za, zb = pa[2] - p0[2], pb[2] - p0[2]
            # (px - sx0, py - sy0) = a * (ax, ay) + b * (bx, by)
            ia, ib = by / det, -bx / det   # a = ia*dx + ib*dy
            ja, jb = -ay / det, ax / det   # b = ja*dx + jb*dy
            xs = (sx0, sx0 + ax, sx0 + bx, sx0 + ax + bx)
            ys = (sy0, sy0 + ay, sy0 + by, sy0 + ay + by)
            x_lo, x_hi = max(0, int(min(xs))), min(W - 1, int(max(xs)) + 1)
            y_lo, y_hi = max(0, int(min(ys))), min(H - 1, int(max(ys)) + 1)
            if x_lo > x_hi or y_lo > y_hi:
                continue
            n = normal_of(q)
            if world_light:
                # A face seen from behind is lit like its other side would be (no culling, normals point out).
                vn = apply_dir(view.m, n)
                if vn[2] > 0:
                    shade = lighting((-n[0], -n[1], -n[2]))
                else:
                    shade = lighting(n)
            else:
                shade = 1.0
            shade *= q.shade * light
            tex, gl, color = q.tex, q.glow, q.color
            u0, v0 = q.uv0
            uax, uay = q.ua
            ubx, uby = q.ub
            cu_lo, cu_hi, cv_lo, cv_hi = q.clamp
            cu_hi = max(cu_lo, math.ceil(cu_hi) - 1)
            cv_hi = max(cv_lo, math.ceil(cv_hi) - 1)
            cu_lo, cv_lo = int(math.floor(cu_lo)), int(math.floor(cv_lo))
            if tex is not None:
                tw, th, td = tex.width, tex.height, tex.data
                sx_t, sy_t = tex.sx, tex.sy
            if gl is not None:
                gw, gd = gl.width, gl.data
            for py in range(y_lo, y_hi + 1):
                dy = py + 0.5 - sy0
                a_row, b_row = ib * dy, jb * dy
                # Solve 0 <= a < 1 and 0 <= b < 1 for px on this row.
                lo, hi = x_lo + 0.5, x_hi + 0.5
                for k, row in ((ia, a_row), (ja, b_row)):
                    if abs(k) < 1e-12:
                        if row < 0 or row >= 1:
                            lo, hi = 1, 0
                            break
                        continue
                    t0, t1 = (0 - row) / k + sx0, (1 - row) / k + sx0
                    if t0 > t1:
                        t0, t1 = t1, t0
                    lo, hi = max(lo, t0), min(hi, t1)
                if lo > hi:
                    continue
                px0 = max(x_lo, int(math.ceil(lo - 0.5)))
                px1 = min(x_hi, int(math.floor(hi - 0.5)))
                rowbase = py * W
                for px in range(px0, px1 + 1):
                    dx = px + 0.5 - sx0
                    a = ia * dx + a_row
                    b = ja * dx + b_row
                    if a < 0 or a > 1 or b < 0 or b > 1:
                        continue
                    z = p0[2] + za * a + zb * b
                    i = rowbase + px
                    if z > depth[i] + 1e-7:  # GL_LEQUAL, as Minecraft draws entities
                        continue
                    if color is not None:
                        c = color(a, b)
                        if c is None:
                            continue
                        depth[i] = z
                        base[i] = c
                        glowbuf[i] = None
                        continue
                    u = u0 + uax * a + ubx * b
                    v = v0 + uay * a + uby * b
                    tu = int(math.floor(u))
                    tv = int(math.floor(v))
                    if tu < cu_lo:
                        tu = cu_lo
                    elif tu > cu_hi:
                        tu = cu_hi
                    if tv < cv_lo:
                        tv = cv_lo
                    elif tv > cv_hi:
                        tv = cv_hi
                    xi = int(tu * sx_t)
                    yi = int(tv * sy_t)
                    if xi < 0 or yi < 0 or xi >= tw or yi >= th:
                        continue
                    o = (yi * tw + xi) * 4
                    if td[o + 3] < 26:  # cutout at alpha 0.1
                        continue
                    depth[i] = z
                    base[i] = (int(td[o] * shade), int(td[o + 1] * shade), int(td[o + 2] * shade))
                    if gl is not None:
                        go = (yi * gw + xi) * 4
                        glowbuf[i] = (gd[go], gd[go + 1], gd[go + 2]) if gd[go + 3] else None
                    else:
                        glowbuf[i] = None

    def image(self):
        img = Image(self.width, self.height)
        out = img.px
        for i, c in enumerate(self.base):
            g = self.glow[i]
            out[i] = c if g is None else (min(255, c[0] + g[0]), min(255, c[1] + g[1]), min(255, c[2] + g[2]))
        return img


def prepare(tex, logical_w, logical_h):
    """Texel scale between the model's texture size and the PNG's (a 2x texture for a 64x64 model)."""
    if tex is not None:
        tex.sx = tex.width / logical_w
        tex.sy = tex.height / logical_h
    return tex


# --- GeckoLib model ----------------------------------------------------------------------------------------------

class GeoModel:
    def __init__(self, data):
        geo = data["minecraft:geometry"][0]
        desc = geo["description"]
        self.tex_w, self.tex_h = desc.get("texture_width", 64), desc.get("texture_height", 64)
        self.bones = geo.get("bones", [])
        self.by_name = {b["name"]: b for b in self.bones}
        self.children = {}
        for b in self.bones:
            self.children.setdefault(b.get("parent"), []).append(b["name"])

    def quads(self, pose=None, hidden=(), tex=None, glow=None):
        """Render-space quads (pixels; X mirrored like GeckoLib) for a pose {bone: (rot_deg, pos, scale)}."""
        pose = pose or {}
        out = []

        def visit(name, parent_m):
            bone = self.by_name[name]
            if name in hidden:
                return
            px, py, pz = bone.get("pivot", [0, 0, 0])
            pivot = (-px, py, pz)
            rx, ry, rz = bone.get("rotation", [0, 0, 0])
            arot, apos, ascale = pose.get(name, ((0, 0, 0), (0, 0, 0), (1, 1, 1)))
            rot = (math.radians(-rx - arot[0]), math.radians(-ry - arot[1]), math.radians(rz + arot[2]))
            m = mul(parent_m, translate(-apos[0], apos[1], apos[2]))
            m = mul(m, translate(*pivot))
            m = mul(m, zyx(*rot))
            m = mul(m, scale_m(*ascale))
            m = mul(m, translate(-pivot[0], -pivot[1], -pivot[2]))
            for cube in bone.get("cubes", []):
                out.extend(cube_quads(cube, m, tex, glow, bone.get("inflate", 0)))
            for child in self.children.get(name, []):
                visit(child, m)

        for root in self.children.get(None, []):
            visit(root, IDENT)
        return out


def face_uvs(cube):
    """Per face (u0, v0, du, dv) in texels; None for a face without UV."""
    sx, sy, sz = cube["size"]
    uv = cube.get("uv", [0, 0])
    if isinstance(uv, dict):
        faces = {}
        for name in ("north", "south", "east", "west", "up", "down"):
            f = uv.get(name)
            if f:
                (u, v), (w, h) = f["uv"], f.get("uv_size", [0, 0])
                faces[name] = (u, v, w, h)
        return faces
    u, v = uv
    w, h, d = math.floor(sx), math.floor(sy), math.floor(sz)
    faces = {
        "north": (u + d, v + d, w, h), "south": (u + d + w + d, v + d, w, h),
        "east": (u, v + d, d, h), "west": (u + d + w, v + d, d, h),
        "up": (u + d, v, w, d), "down": (u + d + w, v + d, w, -d),
    }
    if cube.get("mirror"):
        faces = {k: (f[0] + f[2], f[1], -f[2], f[3]) for k, f in faces.items()}
        faces["east"], faces["west"] = faces["west"], faces["east"]
    return faces


def cube_quads(cube, bone_m, tex, glow, bone_inflate=0):
    ox, oy, oz = cube["origin"]
    sx, sy, sz = cube["size"]
    inf = cube.get("inflate", bone_inflate)
    x0, y0, z0 = -(ox + sx) - inf, oy - inf, oz - inf
    x1, y1, z1 = -ox + inf, oy + sy + inf, oz + sz + inf
    m = bone_m
    if "rotation" in cube:
        cpx, cpy, cpz = cube.get("pivot", [0, 0, 0])
        cp = (-cpx, cpy, cpz)
        crx, cry, crz = cube["rotation"]
        m = mul(m, translate(*cp))
        m = mul(m, zyx(math.radians(-crx), math.radians(-cry), math.radians(crz)))
        m = mul(m, translate(-cp[0], -cp[1], -cp[2]))
    w, h, d = x1 - x0, y1 - y0, z1 - z0
    # Corner and edges per face: a runs along u, b along v; as GeckoLib's VertexSet + GeoQuad.build (box UV: the
    # first column of north / up / down is on +X, of east on +Z, of west and the first row of down on -Z).
    geometry = {
        "north": ((x1, y1, z0), (-w, 0, 0), (0, -h, 0)),
        "south": ((x0, y1, z1), (w, 0, 0), (0, -h, 0)),
        "east": ((x1, y1, z1), (0, 0, -d), (0, -h, 0)),
        "west": ((x0, y1, z0), (0, 0, d), (0, -h, 0)),
        "up": ((x1, y1, z1), (-w, 0, 0), (0, 0, -d)),
        "down": ((x1, y0, z0), (-w, 0, 0), (0, 0, d)),
    }
    out = []
    faces = face_uvs(cube)
    # GeckoLib's quad order (BakedModelFactory): with the depth test passing on equal depth, the later of two
    # coincident faces (a zero-thickness plane) is the one that shows.
    for name in ("west", "east", "north", "south", "up", "down"):
        if name not in faces:
            continue
        u, v, du, dv = faces[name]
        p, ea, eb = geometry[name]
        out.append(Quad(apply(m, p), apply_dir(m, ea), apply_dir(m, eb), (u, v), (du, 0), (0, dv), tex, glow))
    return out


# --- Animations ----------------------------------------------------------------------------------------------------

def molang(value, t):
    if isinstance(value, (int, float)):
        return float(value)
    expr = str(value).replace("query.anim_time", str(t)).replace("q.anim_time", str(t))
    expr = re.sub(r"math\.(sin|cos)\(", lambda m: "_%s(" % m.group(1), expr)
    expr = expr.replace("math.", "_math.")
    try:
        return float(eval(expr, {"__builtins__": {}}, {
            "_sin": lambda d: math.sin(math.radians(d)), "_cos": lambda d: math.cos(math.radians(d)),
            "_math": math}))
    except Exception:
        return 0.0


def ease(name, k):
    """GeckoLib's easing functions (EasingType), k in 0..1."""
    name = (name or "linear").lower()
    if name == "step":
        return 0.0 if k < 1 else 1.0
    if name in ("linear", "catmullrom"):
        return k
    c1 = 1.70158
    c3 = c1 + 1
    pows = {"quad": 2, "cubic": 3, "quart": 4, "quint": 5}
    for key, n in pows.items():
        if name == "easein" + key:
            return k ** n
        if name == "easeout" + key:
            return 1 - (1 - k) ** n
        if name == "easeinout" + key:
            return 2 ** (n - 1) * k ** n if k < 0.5 else 1 - (-2 * k + 2) ** n / 2
    if name == "easeinsine":
        return 1 - math.cos(k * math.pi / 2)
    if name == "easeoutsine":
        return math.sin(k * math.pi / 2)
    if name == "easeinoutsine":
        return -(math.cos(math.pi * k) - 1) / 2
    if name == "easeinexpo":
        return 0 if k == 0 else 2 ** (10 * k - 10)
    if name == "easeoutexpo":
        return 1 if k == 1 else 1 - 2 ** (-10 * k)
    if name == "easeinoutexpo":
        if k in (0, 1):
            return k
        return 2 ** (20 * k - 10) / 2 if k < 0.5 else (2 - 2 ** (-20 * k + 10)) / 2
    if name == "easeincirc":
        return 1 - math.sqrt(max(0, 1 - k * k))
    if name == "easeoutcirc":
        return math.sqrt(max(0, 1 - (k - 1) ** 2))
    if name == "easeinoutcirc":
        return (1 - math.sqrt(max(0, 1 - (2 * k) ** 2))) / 2 if k < 0.5 else (math.sqrt(max(0, 1 - (-2 * k + 2) ** 2)) + 1) / 2
    if name == "easeinback":
        return c3 * k ** 3 - c1 * k * k
    if name == "easeoutback":
        return 1 + c3 * (k - 1) ** 3 + c1 * (k - 1) ** 2
    if name == "easeinoutback":
        c2 = c1 * 1.525
        return ((2 * k) ** 2 * ((c2 + 1) * 2 * k - c2)) / 2 if k < 0.5 else ((2 * k - 2) ** 2 * ((c2 + 1) * (k * 2 - 2) + c2) + 2) / 2
    if name == "easeinelastic":
        return k if k in (0, 1) else -(2 ** (10 * k - 10)) * math.sin((k * 10 - 10.75) * 2 * math.pi / 3)
    if name == "easeoutelastic":
        return k if k in (0, 1) else 2 ** (-10 * k) * math.sin((k * 10 - 0.75) * 2 * math.pi / 3) + 1
    if name == "easeinoutelastic":
        if k in (0, 1):
            return k
        c5 = 2 * math.pi / 4.5
        return -(2 ** (20 * k - 10) * math.sin((20 * k - 11.125) * c5)) / 2 if k < 0.5 else (2 ** (-20 * k + 10) * math.sin((20 * k - 11.125) * c5)) / 2 + 1

    def bounce_out(x):
        n1, d1 = 7.5625, 2.75
        if x < 1 / d1:
            return n1 * x * x
        if x < 2 / d1:
            x -= 1.5 / d1
            return n1 * x * x + 0.75
        if x < 2.5 / d1:
            x -= 2.25 / d1
            return n1 * x * x + 0.9375
        x -= 2.625 / d1
        return n1 * x * x + 0.984375
    if name == "easeoutbounce":
        return bounce_out(k)
    if name == "easeinbounce":
        return 1 - bounce_out(1 - k)
    if name == "easeinoutbounce":
        return (1 - bounce_out(1 - 2 * k)) / 2 if k < 0.5 else (1 + bounce_out(2 * k - 1)) / 2
    return k


def channel_value(channel, t):
    """A bone channel (rotation / position / scale) at time t: constant, or keyframes interpolated with the easing
    of the key the segment runs into (GeckoLib; catmullrom drawn as linear)."""
    if isinstance(channel, (list, int, float, str)):
        vec = channel if isinstance(channel, list) else [channel] * 3
        return tuple(molang(c, t) for c in vec)
    frames = []
    for key, value in channel.items():
        try:
            time = float(key)
        except ValueError:
            continue
        easing = None
        if isinstance(value, dict):
            pre = value.get("pre", value.get("post", value.get("vector")))
            post = value.get("post", value.get("pre", value.get("vector")))
            easing = value.get("easing")
        else:
            pre = post = value
        if isinstance(pre, dict):
            pre = pre.get("vector")
        if isinstance(post, dict):
            post = post.get("vector")
        frames.append((time, pre, post, easing))
    if not frames:
        return None
    frames.sort(key=lambda f: f[0])

    def vec(v):
        v = v if isinstance(v, list) else [v] * 3
        return tuple(molang(c, t) for c in v)

    if t <= frames[0][0]:
        return vec(frames[0][1] if t < frames[0][0] else frames[0][2])
    for (ta, _, pa, _), (tb, qb, _, easing) in zip(frames, frames[1:]):
        if ta <= t <= tb:
            if tb == ta:
                return vec(qb)
            k = ease(easing, (t - ta) / (tb - ta))
            a, b = vec(pa), vec(qb)
            return tuple(a[i] + (b[i] - a[i]) * k for i in range(3))
    return vec(frames[-1][2])


def pose_at(animation, t):
    pose = {}
    for bone, channels in animation.get("bones", {}).items():
        rot = channel_value(channels["rotation"], t) if "rotation" in channels else None
        pos = channel_value(channels["position"], t) if "position" in channels else None
        scl = channel_value(channels["scale"], t) if "scale" in channels else None
        pose[bone] = (rot or (0, 0, 0), pos or (0, 0, 0), scl or (1, 1, 1))
    return pose


def key_times(animation, limit=8):
    """The animation's keyframe times (the poses it was keyed in), at most limit, evenly picked."""
    length = float(animation.get("animation_length", 0) or 0)
    times = {0.0}
    for channels in animation.get("bones", {}).values():
        for channel in channels.values():
            if isinstance(channel, dict):
                for key in channel:
                    try:
                        times.add(round(float(key), 3))
                    except ValueError:
                        pass
    if length:
        times.add(round(length, 3))
    loop = animation.get("loop") in (True, "loop", "true")
    times = sorted(times)
    if loop and len(times) > 1 and length and times[-1] >= length:
        times = times[:-1]  # the end of a loop is its start
    if len(times) < 3 and length:
        times = sorted(set(times) | {round(length * k / 4, 3) for k in range(4 if loop else 5)})
    if len(times) > limit:
        step = (len(times) - 1) / (limit - 1)
        times = [times[round(i * step)] for i in range(limit)]
    return times


# --- Hitboxes from the Java registrations ------------------------------------------------------------------------

def hitboxes():
    """Entity id path -> (width, height) in blocks, scraped from register(...) / EntityDimensions calls."""
    out = {}
    for path in glob.glob(os.path.join("src", "main", "java", "**", "*.java"), recursive=True):
        with open(path, encoding="utf-8") as f:
            src = f.read()
        for m in re.finditer(r'register\(\s*"([a-z0-9_]+)"\s*,[^;]*?;', src, re.S):
            body = m.group(0)
            d = re.search(r"(?:changing|fixed)\(\s*([0-9.]+)f?\s*,\s*([0-9.]+)f?\s*\)", body)
            if not d:
                d = re.search(r",\s*([0-9.]+)f\s*,\s*([0-9.]+)f\s*\)", body)
            if d:
                out.setdefault(m.group(1), (float(d.group(1)), float(d.group(2))))
    return out


# --- Entity sheets -------------------------------------------------------------------------------------------------

VIEWS = [View(0, 12, "FRONT"), View(40, 18, "FRONT-RIGHT"), View(90, 8, "RIGHT"), View(150, 18, "BACK-RIGHT"),
         View(180, 12, "BACK"), View(-35, 50, "ABOVE")]
POSE_VIEW = View(35, 15)


def bounds(quads, view):
    xs, ys = [], []
    for q in quads:
        for p in (q.p0, (q.p0[0] + q.ea[0], q.p0[1] + q.ea[1], q.p0[2] + q.ea[2]),
                  (q.p0[0] + q.eb[0], q.p0[1] + q.eb[1], q.p0[2] + q.eb[2]),
                  (q.p0[0] + q.ea[0] + q.eb[0], q.p0[1] + q.ea[1] + q.eb[1], q.p0[2] + q.ea[2] + q.eb[2])):
            sx, sy, _ = view.project(p)
            xs.append(sx)
            ys.append(sy)
    if not xs:
        return -8, 8, 0, 16
    return min(xs), max(xs), min(ys), max(ys)


def box_points(hitbox):
    w, h = hitbox
    r = w * 8
    return [(x, y, z) for x in (-r, r) for y in (0, h * 16) for z in (-r, r)]


def ground_quad(radius_blocks, light=1.0):
    r = radius_blocks * 16

    def color(a, b):
        gx, gz = int(a * radius_blocks * 2), int(b * radius_blocks * 2)
        on_line = (a * radius_blocks * 2) % 1 < 0.04 or (b * radius_blocks * 2) % 1 < 0.04
        c = (78, 82, 88) if (gx + gz) % 2 else (70, 74, 80)
        if on_line:
            c = (96, 100, 108)
        return tuple(int(v * light) for v in c)

    return Quad((-r, -0.01, -r), (2 * r, 0, 0), (0, 0, 2 * r), (0, 0), (0, 0), (0, 0), None, color=color)


def render_cell(quads, view, cell_w, cell_h, fit, hitbox=None, light=1.0, bg=BACKGROUND, ground=True):
    """fit: (scale, centre x in model units, centre y) shared by a sheet so cells compare."""
    scale, mx, my = fit
    frame = Frame(cell_w, cell_h, bg)
    cx = cell_w / 2 - mx * scale
    cy = cell_h / 2 + my * scale
    if ground:
        radius = max(1, math.ceil((hitbox[0] if hitbox else 1) / 2 + 1))
        frame.draw([ground_quad(radius, light if light < 1 else 1.0)], view, scale, cx, cy, world_light=False)
    frame.draw(quads, view, scale, cx, cy, light)
    img = frame.image()
    if hitbox:
        pts = [view.project(p) for p in box_points(hitbox)]
        sp = [(cx + p[0] * scale, cy - p[1] * scale) for p in pts]
        for i in range(8):
            for j in range(i + 1, 8):
                if bin(i ^ j).count("1") == 1:
                    img.line(sp[i][0], sp[i][1], sp[j][0], sp[j][1], (120, 255, 120), 0.6, dash=3)
    return img


def fit_for(quad_sets, views, cell_w, cell_h, hitbox, margin=0.88):
    xs, ys = [], []
    for quads in quad_sets:
        for v in views:
            b = bounds(quads, v)
            xs += [b[0], b[1]]
            ys += [b[2], b[3]]
            if hitbox:
                for p in box_points(hitbox):
                    sx, sy, _ = v.project(p)
                    xs.append(sx)
                    ys.append(sy)
    w, h = max(xs) - min(xs), max(ys) - min(ys)
    scale = min(cell_w * margin / max(w, 1e-3), (cell_h - 24) * margin / max(h, 1e-3))
    return scale, (max(xs) + min(xs)) / 2, (max(ys) + min(ys)) / 2 + 12 / scale


def load_entity(name):
    """An entity model, or a block model (PLAN 20.1 set pieces) named by its path under geo/block: setpiece/great_bell."""
    folder = "entity"
    geo_data = resource(MOD, "geo/entity/%s.geo.json" % name) or resource(MOD, "geo/%s.geo.json" % name)
    if geo_data is None and resource(MOD, "geo/block/%s.geo.json" % name) is not None:
        folder = "block"
        geo_data = resource(MOD, "geo/block/%s.geo.json" % name)
    if geo_data is None:
        raise SystemExit("no model geo/entity/%s.geo.json or geo/block/%s.geo.json" % (name, name))
    if "/" in name:
        os.makedirs(os.path.join(DEFAULT_OUT, os.path.dirname(name)), exist_ok=True)
    model = GeoModel(json.loads(geo_data))
    tex = prepare(texture(MOD, folder + "/" + name), model.tex_w, model.tex_h)
    glow = prepare(texture(MOD, "%s/%s_glowmask" % (folder, name)), model.tex_w, model.tex_h)
    anim_data = resource(MOD, "animations/%s/%s.animation.json" % (folder, name))
    animations = json.loads(anim_data)["animations"] if anim_data else {}
    return model, tex, glow, animations


def entity_sheet(name, out, hidden=(), anims="all", cell=250):
    model, tex, glow, animations = load_entity(name)
    if tex is None:
        print("warning: no texture for", name)
    hb = hitboxes().get(name)
    rest = model.quads(hidden=hidden, tex=tex, glow=glow)
    cell_w, cell_h = cell, int(cell * 1.25)
    fit = fit_for([rest], VIEWS, cell_w, cell_h, hb)
    header = 30
    atlas_scale = max(1, min(6, (cell * 2) // max(model.tex_w, model.tex_h)))
    atlas_h = model.tex_h * atlas_scale
    sheet = Image(cell_w * len(VIEWS), header + cell_h * 2 + max(0, atlas_h - cell_h) + 30)
    title = "%s  tex %dx%d  bones %d%s" % (name.upper(), model.tex_w, model.tex_h, len(model.bones),
                                          "  hitbox %.2fx%.2f" % hb if hb else "")
    if hidden:
        title += "  hidden: " + ",".join(hidden)
    sheet.text(8, 8, title)
    for i, view in enumerate(VIEWS):
        img = render_cell(rest, view, cell_w, cell_h, fit, hb)
        img.text(6, cell_h - 16, view.label, (200, 200, 200))
        sheet.blit(img, i * cell_w, header)
    # Dusk row: two views at low light so the glowmask reads, then the atlases.
    y = header + cell_h
    for i, view in enumerate(VIEWS[1:3]):
        img = render_cell(rest, view, cell_w, cell_h, fit, None, DUSK_LIGHT, DUSK_BACKGROUND)
        img.text(6, cell_h - 16, "DUSK " + view.label, (200, 180, 160))
        sheet.blit(img, i * cell_w, y)
    x = cell_w * 2 + 10
    for t, label in ((tex, "TEXTURE"), (glow, "GLOWMASK")):
        if t is not None:
            up = upscale(t, max(1, atlas_scale * model.tex_w // t.width))
            sheet.blit(up, x, y + 20)
            sheet.text(x, y + 4, label)
            x += up.width + 10
    sheet.save(os.path.join(out, name + ".png"))
    # Close-up: front-right and back-left large enough to read single texels, lit and at dusk.
    big = [View(25, 12), View(-150, 15)]
    cfit = fit_for([rest], big, 420, 520, None, 0.94)
    close = Image(420 * 3, 520)
    for i, view in enumerate(big):
        close.blit(render_cell(rest, view, 420, 520, cfit, None, ground=False), i * 420, 0)
    close.blit(render_cell(rest, big[0], 420, 520, cfit, None, DUSK_LIGHT, DUSK_BACKGROUND, False), 840, 0)
    close.save(os.path.join(out, name + "_close.png"))
    if animations and anims:
        anim_sheet(name, model, tex, glow, animations, out, hidden, anims, hb)


def anim_sheet(name, model, tex, glow, animations, out, hidden, anims, hb, cell=170, cols=8):
    names = list(animations) if anims == "all" else [a for a in anims.split(",") if a in animations]
    rows = []
    for an in names:
        a = animations[an]
        times = key_times(a, cols)
        rows.append((an, a, times, [model.quads(pose_at(a, t), hidden, tex, glow) for t in times]))
    all_sets = [qs for _, _, _, sets in rows for qs in sets]
    cell_w, cell_h = cell, int(cell * 1.2)
    fit = fit_for(all_sets, [POSE_VIEW], cell_w, cell_h, hb, 0.92)
    label_h = 22
    used = max([len(times) for _, _, times, _ in rows] + [4])
    sheet = Image(cell_w * used, (cell_h + label_h) * len(rows) + 4)
    for r, (an, a, times, sets) in enumerate(rows):
        y = r * (cell_h + label_h)
        length = a.get("animation_length", 0)
        loop = "LOOP" if a.get("loop") in (True, "loop", "true") else "ONCE"
        sheet.fill(0, y, sheet.width, label_h, (34, 36, 42))
        sheet.text(6, y + 4, "%s  %.2fS  %s" % (an, length, loop))
        for c, (t, qs) in enumerate(zip(times, sets)):
            img = render_cell(qs, POSE_VIEW, cell_w, cell_h, fit, hb)
            img.text(4, cell_h - 14, "T %.2f" % t, (210, 210, 210))
            sheet.blit(img, c * cell_w, y + label_h)
    sheet.save(os.path.join(out, name + "_anims.png"))


# --- Block models --------------------------------------------------------------------------------------------------

def load_block_model(ident, default_ns=MOD):
    """Resolved (textures, elements) of a block/item model JSON, following parents through the mod and vanilla."""
    textures, elements = {}, None
    chain = []
    ns, path = split_id(ident, default_ns)
    while path:
        data = resource(ns, "models/%s.json" % path)
        if data is None:
            break
        model = json.loads(data)
        chain.append(model)
        parent = model.get("parent")
        if not parent or parent.startswith("builtin/"):
            break
        ns, path = split_id(parent)
    for model in reversed(chain):
        textures.update(model.get("textures", {}))
        if "elements" in model:
            elements = model["elements"]
    return textures, elements or []


def resolve_texture(textures, ref, depth=0):
    while ref and ref.startswith("#") and depth < 10:
        ref = textures.get(ref[1:])
        depth += 1
    if not ref or ref.startswith("#"):
        return None
    ns, path = split_id(ref)
    return texture(ns, path)


class BlockQuad(Quad):
    """A block model face in block space: its outward normal, cullface direction, biome tint and shade flag."""
    __slots__ = ("n", "cull", "tint", "flat")


def element_quads(elements, textures, block_space=False):
    """Quads of model elements: in the entity frame used by the views, or (block_space) as BlockQuads in block space
    (0..16, x east, y up, z south)."""
    out = []
    for el in elements:
        (x1, y1, z1), (x2, y2, z2) = el["from"], el["to"]
        m = IDENT
        r = el.get("rotation")
        if r:
            o = r.get("origin", [8, 8, 8])
            a = math.radians(r.get("angle", 0))
            rm = {"x": rot_x, "y": rot_y, "z": rot_z}[r.get("axis", "y")](a)
            m = mul(mul(translate(*o), rm), translate(-o[0], -o[1], -o[2]))
        geometry = {
            "north": ((x2, y2, z1), (x1 - x2, 0, 0), (0, y1 - y2, 0), (16 - x2, 16 - y2, 16 - x1, 16 - y1)),
            "south": ((x1, y2, z2), (x2 - x1, 0, 0), (0, y1 - y2, 0), (x1, 16 - y2, x2, 16 - y1)),
            "east": ((x2, y2, z2), (0, 0, z1 - z2), (0, y1 - y2, 0), (16 - z2, 16 - y2, 16 - z1, 16 - y1)),
            "west": ((x1, y2, z1), (0, 0, z2 - z1), (0, y1 - y2, 0), (z1, 16 - y2, z2, 16 - y1)),
            "up": ((x1, y2, z1), (x2 - x1, 0, 0), (0, 0, z2 - z1), (x1, z1, x2, z2)),
            "down": ((x1, y1, z2), (x2 - x1, 0, 0), (0, 0, z1 - z2), (x1, 16 - z2, x2, 16 - z1)),
        }
        for name, face in el.get("faces", {}).items():
            p, ea, eb, default_uv = geometry[name]
            u1, v1, u2, v2 = face.get("uv", default_uv)
            tex = resolve_texture(textures, face.get("texture"))
            if tex is None:
                continue
            prepare(tex, 16, 16)
            corners = [(u1, v1), (u2, v1), (u2, v2), (u1, v2)]  # at P(0,0), P(1,0), P(1,1), P(0,1)
            k = (face.get("rotation", 0) // 90) % 4
            corners = corners[-k:] + corners[:-k] if k else corners
            uv0 = corners[0]
            ua = (corners[1][0] - uv0[0], corners[1][1] - uv0[1])
            ub = (corners[3][0] - uv0[0], corners[3][1] - uv0[1])
            # Texels are 1/16 of the face's 16 uv units for a 16x texture; prepare() scales larger ones.
            if not block_space:
                out.append(Quad(apply(m, p), apply_dir(m, ea), apply_dir(m, eb), uv0, ua, ub, tex))
                continue
            q = BlockQuad(apply(m, p), apply_dir(m, ea), apply_dir(m, eb), uv0, ua, ub, tex)
            q.n, q.cull = apply_dir(m, FACE_DIRS[name]), face.get("cullface")
            q.tint, q.flat = "tintindex" in face, el.get("shade") is False
            out.append(q)
    if block_space:
        return out
    # Block space (0..16, x east, z south) to the entity frame used by the views: centre on x/z, north = front.
    for q in out:
        q.p0 = (8 - q.p0[0], q.p0[1], q.p0[2] - 8)
        q.ea = (-q.ea[0], q.ea[1], q.ea[2])
        q.eb = (-q.eb[0], q.eb[1], q.eb[2])
    return out


BLOCK_VIEWS = [View(45, 30, "NE"), View(-45, 30, "NW"), View(135, 30, "SE"), View(0, -25, "BELOW N")]


def block_sheet(names, out, title="blocks"):
    cell = 150
    tile = 16 * 3 * 3  # 3x3 tiles at 3x
    rows = []
    for name in names:
        textures, elements = load_block_model(name if ":" in name else "%s:block/%s" % (MOD, name))
        rows.append((name, textures, elements))
    sheet = Image(cell * len(BLOCK_VIEWS) + tile + 20 + 300, (cell + 26) * len(rows) + 4)
    for r, (name, textures, elements) in enumerate(rows):
        y = r * (cell + 26)
        sheet.fill(0, y, sheet.width, 22, (34, 36, 42))
        sheet.text(6, y + 4, name.upper())
        quads = element_quads(elements, textures)
        if quads:
            fit = fit_for([quads], BLOCK_VIEWS, cell, cell, None, 0.8)
            for c, view in enumerate(BLOCK_VIEWS):
                frame = Frame(cell, cell, BACKGROUND)
                scale, mx, my = fit
                frame.draw(quads, view, scale, cell / 2 - mx * scale, cell / 2 + my * scale, 1.0, block_light)
                sheet.blit(frame.image(), c * cell, y + 24)
        # The textures this model uses: 3x3 tiling of the first, then each at 4x.
        x = cell * len(BLOCK_VIEWS) + 10
        seen = []
        for key in textures:
            t = resolve_texture(textures, "#" + key)
            if t is not None and all(t is not s for s in seen) and key != "particle":
                seen.append(t)
        if seen:
            t = seen[0]
            up = upscale(t, 3, None)
            for ty in range(3):
                for tx in range(3):
                    sheet.blit(up, x + tx * up.width, y + 24 + ty * up.height)
            x += up.width * 3 + 10
            for t in seen[1:5]:
                up = upscale(t, 4)
                sheet.blit(up, x, y + 24)
                x += up.width + 6
    sheet.save(os.path.join(out, title + ".png"))


def mod_block_names():
    names = set()
    for base in (ASSETS, GENERATED):
        for path in glob.glob(os.path.join(base, MOD, "blockstates", "*.json")):
            names.add(os.path.basename(path)[:-5])
    out = []
    for name in sorted(names):
        # The block's own model (stairs, slabs: the plain one), its inventory model (walls), else the blockstate's first.
        own = [m for m in (name, name + "_inventory") if resource(MOD, "models/block/%s.json" % m)]
        if own:
            out.append("%s:block/%s" % (MOD, own[0]))
            continue
        bs = json.loads(resource(MOD, "blockstates/%s.json" % name))
        model = None
        if "variants" in bs:
            first = next(iter(bs["variants"].values()))
            model = (first[0] if isinstance(first, list) else first)["model"]
        elif "multipart" in bs:
            first = bs["multipart"][0]["apply"]
            model = (first[0] if isinstance(first, list) else first)["model"]
        if model:
            out.append(model)
    return out


# --- Items ---------------------------------------------------------------------------------------------------------

VANILLA_ITEMS = ["iron_ingot", "diamond", "netherite_scrap", "blaze_powder", "iron_sword", "crossbow_standby",
                 "lantern", "potion", "iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots"]


def item_layers(name):
    data = resource(MOD, "models/item/%s.json" % name)
    if data is None:
        t = texture(MOD, "item/" + name)
        return [t] if t else []
    textures, _ = load_block_model("%s:item/%s" % (MOD, name))
    layers = []
    for i in range(4):
        ref = textures.get("layer%d" % i)
        if ref:
            ns, path = split_id(ref)
            t = texture(ns, path)
            if t:
                layers.append(t)
    return layers


def composite(layers):
    if not layers:
        return None
    w, h = layers[0].width, layers[0].height
    out = Texture(w, h)
    for t in layers:
        for y in range(min(h, t.height)):
            for x in range(min(w, t.width)):
                r, g, b, a = t.get(x, y)
                if a:
                    i = (y * w + x) * 4
                    k = a / 255
                    o = out.data
                    o[i] = int(o[i] + (r - o[i]) * k)
                    o[i + 1] = int(o[i + 1] + (g - o[i + 1]) * k)
                    o[i + 2] = int(o[i + 2] + (b - o[i + 2]) * k)
                    o[i + 3] = max(o[i + 3], a)
    return out


def slot_bg(x, y, size):
    return (139, 139, 139)


def item_sheet(names, out, title="items", per_row=8, scale=8):
    cell = 16 * scale + 16
    entries = []
    for name in names:
        if name.startswith("minecraft:"):
            t = texture("minecraft", "item/" + name.split(":", 1)[1])
            entries.append((name.split(":", 1)[1] + " (V)", t))
        else:
            entries.append((name, composite(item_layers(name))))
    rows = (len(entries) + per_row - 1) // per_row
    sheet = Image(cell * per_row, rows * (cell + 26) + 4)
    for i, (name, t) in enumerate(entries):
        x, y = (i % per_row) * cell, (i // per_row) * (cell + 26)
        if t is not None:
            sheet.blit(upscale(t, scale * 16 // max(16, t.width), slot_bg), x + 8, y + 8)
            # 1x and 2x as they appear in the inventory, next to each other under the big one.
            small = upscale(t, max(1, 32 // t.width), slot_bg)
            sheet.blit(small, x + 8, y + cell - 4)
        label = name.upper()[:16]
        sheet.text(x + 44, y + cell + 2, label[:10], (220, 220, 220), 1)
        sheet.text(x + 44, y + cell + 10, label[10:], (220, 220, 220), 1)
    sheet.save(os.path.join(out, title + ".png"))


def mod_item_names():
    names = set()
    for base in (ASSETS, GENERATED):
        for path in glob.glob(os.path.join(base, MOD, "models", "item", "*.json")):
            names.add(os.path.basename(path)[:-5])
    # Flat items only: no spawn eggs, no datagen trim variants, no block items (they have no layer0).
    return sorted(n for n in names if not n.endswith("_spawn_egg") and "_trim" not in n and item_layers(n))


# --- Armor ---------------------------------------------------------------------------------------------------------

def biped(layer, inflate, parts):
    """Vanilla armor model parts as Bedrock bones (64x32 texture)."""
    defs = {
        "head": ([-4, 24, -4], [8, 8, 8], [0, 0], False),
        "body": ([-4, 12, -2], [8, 12, 4], [16, 16], False),
        "right_arm": ([-8, 12, -2], [4, 12, 4], [40, 16], False),
        "left_arm": ([4, 12, -2], [4, 12, 4], [40, 16], True),
        "right_leg": ([-4, 0, -2], [4, 12, 4], [0, 16], False),
        "left_leg": ([0, 0, -2], [4, 12, 4], [0, 16], True),
    }
    bones = []
    for p in parts:
        origin, size, uv, mirror = defs[p]
        bones.append({"name": p, "pivot": [0, 0, 0], "cubes": [
            {"origin": origin, "size": size, "uv": uv, "inflate": inflate, "mirror": mirror}]})
    return {"minecraft:geometry": [{"description": {"texture_width": 64, "texture_height": 32}, "bones": bones}]}


def mannequin_quads():
    grey = Texture(64, 64)
    for i in range(0, len(grey.data), 4):
        grey.data[i:i + 4] = bytes((150, 146, 140, 255))
    prepare(grey, 64, 32)
    model = GeoModel(biped(None, 0, ["head", "body", "right_arm", "left_arm", "right_leg", "left_leg"]))
    return model.quads(tex=grey)


SLOT_PARTS = {"helmet": (1, ["head"]), "chestplate": (1, ["body", "right_arm", "left_arm"]),
              "leggings": (2, ["body", "right_leg", "left_leg"]), "boots": (1, ["right_leg", "left_leg"])}


def armor_quads(material, slots):
    quads = []
    for slot in slots:
        layer, parts = SLOT_PARTS[slot]
        t = texture("minecraft", "models/armor/%s_layer_%d" % (material, layer))
        if t is None:
            continue
        prepare(t, 64, 32)
        inflate = 1.0 if layer == 1 else 0.5
        if slot == "boots":
            inflate = 1.0
        quads += GeoModel(biped(layer, inflate, parts)).quads(tex=t)
    return quads


def armor_materials():
    names = set()
    for base in (ASSETS, GENERATED):
        for path in glob.glob(os.path.join(base, "minecraft", "textures", "models", "armor", "*_layer_*.png")):
            names.add(re.sub(r"_layer_\d\.png$", "", os.path.basename(path)))
    return sorted(names)


def armor_sheet(materials, out, title="armor"):
    views = [View(0, 10, "FRONT"), View(40, 15, "FRONT-RIGHT"), View(90, 5, "RIGHT"), View(180, 10, "BACK")]
    cell_w, cell_h = 150, 210
    base = mannequin_quads()
    rows = []
    for material in materials:
        slots = [s for s in SLOT_PARTS if texture("minecraft", "models/armor/%s_layer_%d" % (material, SLOT_PARTS[s][0]))]
        rows.append((material, armor_quads(material, slots)))
    fit = fit_for([base], views, cell_w, cell_h, (0.6, 1.95))
    atlas = 2
    sheet = Image(cell_w * len(views) + 64 * atlas * 2 + 30, (cell_h + 24) * len(rows) + 4)
    for r, (material, quads) in enumerate(rows):
        y = r * (cell_h + 24)
        sheet.fill(0, y, sheet.width, 22, (34, 36, 42))
        sheet.text(6, y + 4, material.upper())
        for c, view in enumerate(views):
            img = render_cell(base + quads, view, cell_w, cell_h, fit, None)
            sheet.blit(img, c * cell_w, y + 24)
        x = cell_w * len(views) + 10
        for layer in (1, 2):
            t = texture("minecraft", "models/armor/%s_layer_%d" % (material, layer))
            if t:
                up = upscale(t, atlas * 64 // t.width)
                sheet.blit(up, x, y + 30)
                sheet.text(x, y + 30 + up.height + 4, "LAYER %d" % layer, (200, 200, 200), 1)
            x += 64 * atlas + 10
    sheet.save(os.path.join(out, title + ".png"))


# --- NBT -----------------------------------------------------------------------------------------------------------

def read_nbt(data):
    """An NBT file (gzipped or not) as Python values: compounds as dicts, lists and arrays as lists."""
    if data[:2] == b"\x1f\x8b":
        data = gzip.decompress(data)
    pos = 0
    scalars = {1: ">b", 2: ">h", 3: ">i", 4: ">q", 5: ">f", 6: ">d"}

    def take(n):
        nonlocal pos
        pos += n
        return data[pos - n:pos]

    def string():
        return take(struct.unpack(">H", take(2))[0]).decode("utf-8", "replace")

    def payload(tag):
        if tag in scalars:
            fmt = scalars[tag]
            return struct.unpack(fmt, take(struct.calcsize(fmt)))[0]
        if tag == 8:
            return string()
        if tag == 9:
            inner, n = struct.unpack(">bi", take(5))
            return [payload(inner) for _ in range(n)]
        if tag == 10:
            out = {}
            while True:
                inner = take(1)[0]
                if inner == 0:
                    return out
                key = string()
                out[key] = payload(inner)
        n = struct.unpack(">i", take(4))[0]
        fmt = {7: "b", 11: "i", 12: "q"}[tag]
        return list(struct.unpack(">%d%s" % (n, fmt), take(n * struct.calcsize(fmt))))

    tag = take(1)[0]
    string()
    return payload(tag)


# --- Structure templates -------------------------------------------------------------------------------------------

FACE_DIRS = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0),
             "down": (0, -1, 0)}
DIR_NAMES = {v: k for k, v in FACE_DIRS.items()}
STRUCTURE_ROOTS = [os.path.join("src", "main", base, "data", MOD, d) for base in ("resources", "generated")
                   for d in ("structures", "structure")]
NOT_DRAWN = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:structure_void",
             "minecraft:barrier", "minecraft:light", "minecraft:structure_block"}
MARKER_BLOCK = MOD + ":placed_enemy"
# Blocks the game draws in code (block entities, fluids): a box in block pixels and the texture to put on it.
CODE_DRAWN = {
    "chest": ((1, 0, 1, 15, 14, 15), "minecraft:block/oak_planks"),
    "trapped_chest": ((1, 0, 1, 15, 14, 15), "minecraft:block/oak_planks"),
    "ender_chest": ((1, 0, 1, 15, 14, 15), "minecraft:block/obsidian"),
    "skeleton_skull": ((4, 0, 4, 12, 8, 12), "minecraft:block/bone_block_side"),
    "wither_skeleton_skull": ((4, 0, 4, 12, 8, 12), "minecraft:block/blackstone"),
    "water": ((0, 0, 0, 16, 14, 16), "minecraft:block/water_still"),
    "lava": ((0, 0, 0, 16, 14, 16), "minecraft:block/lava_still"),
}
FOLIAGE_TINT, GRASS_TINT, WATER_TINT = (72, 181, 24), (145, 189, 89), (63, 118, 228)
SKY_TOP, SKY_HORIZON = (44, 46, 62), (118, 108, 112)
PIN_COLORS = [(255, 96, 80), (255, 204, 64), (110, 216, 255), (196, 136, 255), (120, 236, 140), (255, 140, 214),
              (255, 164, 90), (210, 210, 210)]
NEAR = 0.05


def parse_state(s):
    """'minecraft:stone_stairs[facing=east,half=top]' -> (name, {property: value})."""
    m = re.match(r"([^\[]+)(?:\[(.*)\])?$", s.strip())
    props = dict(kv.split("=", 1) for kv in (m.group(2) or "").split(",") if "=" in kv)
    return m.group(1), props


def yaw_facing(yaw):
    return ("south", "west", "north", "east")[int(round(yaw / 90)) % 4]


class Marker:
    """A placed enemy (or a template entity): feet position in block units, entity id, facing, extra tags."""

    def __init__(self, pos, entity, facing, tags):
        self.pos, self.entity, self.facing, self.tags = pos, entity, facing, tags


class Template:
    """A structure template: drawn blocks by position (a jigsaw block as its final state), placed-enemy markers
    (PLAN 9.2, realm.placed) and template entities as markers, loot chests counted."""

    def __init__(self, name, path):
        with open(path, "rb") as f:
            nbt = read_nbt(f.read())
        self.name, self.path = name, path
        self.size = tuple(nbt["size"])
        palette = nbt.get("palette") or (nbt.get("palettes") or [[]])[0]
        states = [(s["Name"], s.get("Properties", {})) for s in palette]
        self.blocks, self.markers, self.chests = {}, [], 0
        for b in nbt.get("blocks", []):
            name, props = states[b["state"]]
            extra = b.get("nbt") or {}
            x, y, z = b["pos"]
            if name == MARKER_BLOCK:
                self.markers.append(Marker((x + 0.5, y, z + 0.5), extra.get("entity", "?"),
                                           props.get("facing", "north"), extra.get("tags", [])))
                continue
            if name == "minecraft:jigsaw":
                name, props = parse_state(extra.get("final_state", "minecraft:air"))
            if "LootTable" in extra:
                self.chests += 1
            if name not in NOT_DRAWN:
                self.blocks[(x, y, z)] = (name, props)
        for e in nbt.get("entities", []):
            data = e.get("nbt", {})
            yaw = (data.get("Rotation") or [0])[0]
            self.markers.append(Marker(tuple(e["pos"]), data.get("id", "?"), yaw_facing(yaw), ["entity"]))


def structure_names():
    names = set()
    for root in STRUCTURE_ROOTS:
        for p in glob.glob(os.path.join(root, "**", "*.nbt"), recursive=True):
            names.add(os.path.relpath(p, root)[:-4].replace(os.sep, "/"))
    return sorted(names)


def structure_file(name):
    """Path of a template: its id path (ashen_reach/fields/barn), a unique tail of it (fields/barn, barn) or a file."""
    if name.endswith(".nbt") and os.path.exists(name):
        return name, os.path.basename(name)[:-4]
    name = split_id(name, MOD)[1]
    names = structure_names()
    found = [n for n in names if n == name] or [n for n in names if n.endswith("/" + name)]
    if len(found) != 1:
        raise SystemExit("structure %s: %s" % (name, "ambiguous: " + ", ".join(found) if found else "not found"))
    for root in STRUCTURE_ROOTS:
        p = os.path.join(root, found[0] + ".nbt")
        if os.path.exists(p):
            return p, found[0]


def start_depths():
    """Template path part -> how far its y = 0 lies below the ground layer: START_DEPTH maps in the template classes,
    the Citadel's FOUNDATION (CitadelLayout) for every citadel/ piece."""
    out = {}
    for path in glob.glob(os.path.join("src", "**", "*Templates.java"), recursive=True):
        with open(path, encoding="utf-8") as f:
            src = f.read()
        for m in re.finditer(r"START_DEPTH\s*=\s*Map\.of\(([^)]*)\)", src):
            for k, v in re.findall(r'"([a-z0-9_/]+)"\s*,\s*(\d+)', m.group(1)):
                out[k] = int(v)
    for path in glob.glob(os.path.join("src", "**", "CitadelLayout.java"), recursive=True):
        with open(path, encoding="utf-8") as f:
            m = re.search(r"\bFOUNDATION\s*=\s*(\d+)\s*;", f.read())
        if m:
            out["citadel/"] = int(m.group(1))
    return out


def ground_level(tpl):
    """The y of the ground surface around a template: the top of its ground layer (y = 0 unless start_depths says)."""
    for part, depth in start_depths().items():
        if "/" + part in "/" + tpl.name:
            return depth + 1
    return 1


def when_holds(cond, props):
    if not cond:
        return True
    if "OR" in cond:
        return any(when_holds(c, props) for c in cond["OR"])
    if "AND" in cond:
        return all(when_holds(c, props) for c in cond["AND"])
    return all(props.get(k) in (str(v).lower() if isinstance(v, bool) else str(v)).split("|") for k, v in cond.items())


def blockstate_models(name, props):
    """[(model id, x, y)] the blockstate file picks for these properties (variants: the first key that matches,
    multipart: every part whose condition holds; the first of weighted alternatives); None without a blockstate."""
    ns, path = split_id(name)
    data = resource(ns, "blockstates/%s.json" % path)
    if data is None:
        return None
    bs = json.loads(data)
    picks = []
    for key, v in bs.get("variants", {}).items():
        if all(props.get(k) == val for k, val in (kv.split("=", 1) for kv in key.split(",") if kv)):
            picks.append(v)
            break
    for part in bs.get("multipart", []):
        if when_holds(part.get("when"), props):
            picks.append(part["apply"])
    out = []
    for v in picks:
        v = v[0] if isinstance(v, list) else v
        out.append((v["model"], v.get("x", 0), v.get("y", 0)))
    return out


def turn(v, rx, ry, centre=0):
    """A block-space point (centre 8) or direction (centre 0) turned like a blockstate's x, then y rotation
    (x = 90 turns north to down, y = 90 turns north to east)."""
    x, y, z = v[0] - centre, v[1] - centre, v[2] - centre
    for _ in range(rx // 90 % 4):
        y, z = z, -y
    for _ in range(ry // 90 % 4):
        x, z = -z, x
    return (x + centre, y + centre, z + centre)


_SHAPES = {}
_OPAQUE = {}
_MISSING = None


def missing_texture():
    global _MISSING
    if _MISSING is None:
        _MISSING = Texture(16, 16)
        for y in range(16):
            for x in range(16):
                i = (y * 16 + x) * 4
                _MISSING.data[i:i + 4] = bytes((248, 0, 248, 255) if (x // 8 + y // 8) % 2 else (0, 0, 0, 255))
        prepare(_MISSING, 16, 16)
    return _MISSING


def opaque(tex):
    if id(tex) not in _OPAQUE:
        _OPAQUE[id(tex)] = tex is not None and min(tex.data[3::4]) == 255
    return _OPAQUE[id(tex)]


def block_shape(name, props):
    """(BlockQuads in block space, opaque full cube?) of a block state, cached. Models come from the blockstate
    (x / y rotation; uvlock is not applied, so a turned stair's top shows its texture turned). A block the game draws
    in code (chest, skull, fluid) is a box with a texture; any other model without elements a full cube with its
    particle texture, a block without a blockstate a magenta cube."""
    key = (name, tuple(sorted(props.items())))
    if key in _SHAPES:
        return _SHAPES[key]
    path = split_id(name)[1]
    models = blockstate_models(name, props) or []
    quads, full, particle = [], False, None
    for model_id, rx, ry in models:
        textures, elements = load_block_model(model_id, "minecraft")
        particle = particle or textures.get("particle")
        while particle and particle.startswith("#"):
            particle = textures.get(particle[1:])
        mq = element_quads(elements, textures, True)
        for q in mq:
            if rx or ry:
                q.p0, q.ea, q.eb = turn(q.p0, rx, ry, 8), turn(q.ea, rx, ry), turn(q.eb, rx, ry)
                q.n = turn(q.n, rx, ry)
                q.cull = DIR_NAMES.get(turn(FACE_DIRS[q.cull], rx, ry)) if q.cull else None
        quads += mq
        if len(models) == 1 and len(elements) == 1 and "rotation" not in elements[0]:
            full = (elements[0]["from"], elements[0]["to"]) == ([0, 0, 0], [16, 16, 16]) and len(mq) == 6 \
                   and all(opaque(q.tex) for q in mq)
    if not quads:
        box, ref = CODE_DRAWN.get(path, ((0, 0, 0, 16, 16, 16), particle))
        element = {"from": list(box[:3]), "to": list(box[3:]), "faces": {f: {"texture": "#t"} for f in FACE_DIRS}}
        quads = element_quads([element], {"t": ref}, True) if ref else []
        if not quads:
            quads = element_quads([element], {"t": "minecraft:block/stone"}, True)
            for q in quads:
                q.tex = missing_texture()
        for q in quads:
            q.tint = path == "water"
    _SHAPES[key] = (quads, full)
    return _SHAPES[key]


def tint_for(name):
    path = split_id(name)[1]
    if path == "water":
        return WATER_TINT
    if "leaves" in path or path == "vine":
        return FOLIAGE_TINT
    return GRASS_TINT


def structure_quads(tpl):
    """World quads (block units) of a template; faces against an opaque full cube neighbour are left out."""
    shapes = {pos: block_shape(name, props) for pos, (name, props) in tpl.blocks.items()}
    out = []
    for (x, y, z), (quads, _) in shapes.items():
        tint = tint_for(tpl.blocks[(x, y, z)][0])
        for q in quads:
            if q.cull:
                d = FACE_DIRS[q.cull]
                neighbour = shapes.get((x + d[0], y + d[1], z + d[2]))
                if neighbour and neighbour[1]:
                    continue
            w = BlockQuad((x + q.p0[0] / 16, y + q.p0[1] / 16, z + q.p0[2] / 16),
                          (q.ea[0] / 16, q.ea[1] / 16, q.ea[2] / 16), (q.eb[0] / 16, q.eb[1] / 16, q.eb[2] / 16),
                          q.uv0, q.ua, q.ub, q.tex)
            w.n, w.cull, w.flat, w.tint = q.n, None, q.flat, tint if q.tint else None
            out.append(w)
    return out


def ground_plane(x0, z0, x1, z1, y):
    """A flat ash-grey ground with a 1-block grid, just under y."""

    def color(a, b):
        gx, gz = x0 + a * (x1 - x0), z0 + b * (z1 - z0)
        if gx % 1 < 0.05 or gz % 1 < 0.05:
            return (98, 94, 90)
        return (84, 80, 76) if (math.floor(gx) + math.floor(gz)) % 2 else (79, 75, 71)

    q = BlockQuad((x0, y - 0.003, z0), (x1 - x0, 0, 0), (0, 0, z1 - z0), (0, 0), (0, 0), (0, 0), None, color=color)
    q.n, q.cull, q.flat, q.tint = (0, 1, 0), None, True, None
    return q


# --- Camera and scene rasteriser -----------------------------------------------------------------------------------

class Camera:
    """A camera in block units (x east, y up, z south). yaw as in Minecraft (0 looks south, 90 west, 180 north),
    pitch > 0 looks down. With fov (vertical degrees) it is a perspective camera, else orthographic at scale pixels
    per block, shift (camera-space x, y) at the image centre."""

    def __init__(self, eye, yaw, pitch, width, height, fov=None, scale=16.0, shift=(0.0, 0.0)):
        ya, pa = math.radians(yaw), math.radians(pitch)
        self.f = (-math.sin(ya) * math.cos(pa), -math.sin(pa), math.cos(ya) * math.cos(pa))
        self.r = (-math.cos(ya), 0.0, -math.sin(ya))
        r, f = self.r, self.f
        self.u = (r[1] * f[2] - r[2] * f[1], r[2] * f[0] - r[0] * f[2], r[0] * f[1] - r[1] * f[0])
        self.eye, self.yaw, self.pitch = eye, yaw, pitch
        self.width, self.height = width, height
        self.focal = height / 2 / math.tan(math.radians(fov) / 2) if fov else None
        self.scale, self.shift = scale, shift

    def cam(self, p):
        dx, dy, dz = p[0] - self.eye[0], p[1] - self.eye[1], p[2] - self.eye[2]
        r, u, f = self.r, self.u, self.f
        return (dx * r[0] + dy * r[1] + dz * r[2], dx * u[0] + dy * u[1] + dz * u[2],
                dx * f[0] + dy * f[1] + dz * f[2])

    def screen(self, c):
        """Screen position of a camera-space point; None behind a perspective camera."""
        if self.focal:
            if c[2] < NEAR * 0.5:
                return None
            return self.width / 2 + c[0] * self.focal / c[2], self.height / 2 - c[1] * self.focal / c[2]
        return (self.width / 2 + (c[0] - self.shift[0]) * self.scale,
                self.height / 2 - (c[1] - self.shift[1]) * self.scale)


def clip_near(poly):
    """A camera-space polygon of (x, y, z, a, b) cut to z >= NEAR."""
    out = []
    for i, p in enumerate(poly):
        q = poly[(i + 1) % len(poly)]
        if p[2] >= NEAR:
            out.append(p)
        if (p[2] >= NEAR) != (q[2] >= NEAR):
            t = (NEAR - p[2]) / (q[2] - p[2])
            out.append(tuple(p[k] + (q[k] - p[k]) * t for k in range(5)))
    return out


def draw_scene(frame, quads, cam, light=1.0):
    """BlockQuads into a Frame through a Camera: back faces culled (as the game culls block faces), perspective-
    correct texturing, cutout at alpha 0.1, vanilla block face shading, biome tint; the depth buffer keeps
    camera-space depth in blocks."""
    W, H = frame.width, frame.height
    depth, base = frame.depth, frame.base
    persp = cam.focal is not None
    ex, ey, ez = cam.eye
    fx, fy, fz = cam.f
    work = []
    for q in quads:
        n = q.n
        if persp:
            if (q.p0[0] - ex) * n[0] + (q.p0[1] - ey) * n[1] + (q.p0[2] - ez) * n[2] >= 0:
                continue
        elif fx * n[0] + fy * n[1] + fz * n[2] >= -1e-9:
            continue
        p0, ea, eb = q.p0, q.ea, q.eb
        corners = ((p0, 0, 0), ((p0[0] + ea[0], p0[1] + ea[1], p0[2] + ea[2]), 1, 0),
                   ((p0[0] + ea[0] + eb[0], p0[1] + ea[1] + eb[1], p0[2] + ea[2] + eb[2]), 1, 1),
                   ((p0[0] + eb[0], p0[1] + eb[1], p0[2] + eb[2]), 0, 1))
        poly = [cam.cam(p) + (a, b) for p, a, b in corners]
        if persp:
            poly = clip_near(poly)
            if len(poly) < 3:
                continue
        work.append((sum(p[2] for p in poly) / len(poly), q, poly))
    work.sort(key=lambda w: w[0])  # near first: hidden texels fail the depth test before they are looked up
    for _, q, poly in work:
        verts = []
        for x, y, z, a, b in poly:
            sx, sy = cam.screen((x, y, z))
            if persp:
                w = 1 / z
                verts.append((sx, sy, w, a * w, b * w, z))
            else:
                verts.append((sx, sy, 1.0, a, b, z))
        xs, ys = [v[0] for v in verts], [v[1] for v in verts]
        if max(xs) < 0 or min(xs) >= W or max(ys) < 0 or min(ys) >= H:
            continue
        shade = (1.0 if q.flat else block_light(q.n)) * light
        tex, color, tint = q.tex, q.color, q.tint
        u0, v0 = q.uv0
        uax, uay = q.ua
        ubx, uby = q.ub
        cu_lo, cu_hi, cv_lo, cv_hi = q.clamp
        cu_hi = max(cu_lo, math.ceil(cu_hi) - 1)
        cv_hi = max(cv_lo, math.ceil(cv_hi) - 1)
        cu_lo, cv_lo = int(math.floor(cu_lo)), int(math.floor(cv_lo))
        if tex is not None:
            tw, th, td = tex.width, tex.height, tex.data
            sx_t, sy_t = tex.sx, tex.sy
        if tint:
            tr, tg, tb = tint[0] / 255 * shade, tint[1] / 255 * shade, tint[2] / 255 * shade
        else:
            tr = tg = tb = shade
        for k in range(1, len(verts) - 1):
            A, B, C = verts[0], verts[k], verts[k + 1]
            area = (B[0] - A[0]) * (C[1] - A[1]) - (B[1] - A[1]) * (C[0] - A[0])
            if abs(area) < 1e-9:
                continue
            x_lo, x_hi = max(0, int(min(A[0], B[0], C[0]))), min(W - 1, int(max(A[0], B[0], C[0])) + 1)
            y_lo, y_hi = max(0, int(min(A[1], B[1], C[1]))), min(H - 1, int(max(A[1], B[1], C[1])) + 1)
            # Barycentric weights of A and B are linear in the pixel position.
            da_x, db_x = -(C[1] - B[1]) / area, -(A[1] - C[1]) / area
            for py in range(y_lo, y_hi + 1):
                cy = py + 0.5
                cx = x_lo + 0.5
                wa = ((C[0] - B[0]) * (cy - B[1]) - (C[1] - B[1]) * (cx - B[0])) / area
                wb = ((A[0] - C[0]) * (cy - C[1]) - (A[1] - C[1]) * (cx - C[0])) / area
                row = py * W
                for px in range(x_lo, x_hi + 1):
                    if px > x_lo:
                        wa += da_x
                        wb += db_x
                    wc = 1 - wa - wb
                    if wa < -1e-7 or wb < -1e-7 or wc < -1e-7:
                        continue
                    if persp:
                        qq = wa * A[2] + wb * B[2] + wc * C[2]
                        z = 1 / qq
                        a = (wa * A[3] + wb * B[3] + wc * C[3]) * z
                        b = (wa * A[4] + wb * B[4] + wc * C[4]) * z
                    else:
                        z = wa * A[5] + wb * B[5] + wc * C[5]
                        a = wa * A[3] + wb * B[3] + wc * C[3]
                        b = wa * A[4] + wb * B[4] + wc * C[4]
                    i = row + px
                    if z > depth[i] + 1e-6:
                        continue
                    a = 0.0 if a < 0 else (1.0 if a > 1 else a)
                    b = 0.0 if b < 0 else (1.0 if b > 1 else b)
                    if color is not None:
                        c = color(a, b)
                        if c is not None:
                            depth[i] = z
                            base[i] = c
                        continue
                    tu = int(math.floor(u0 + uax * a + ubx * b))
                    tv = int(math.floor(v0 + uay * a + uby * b))
                    tu = cu_lo if tu < cu_lo else (cu_hi if tu > cu_hi else tu)
                    tv = cv_lo if tv < cv_lo else (cv_hi if tv > cv_hi else tv)
                    xi, yi = int(tu * sx_t), int(tv * sy_t)
                    if xi < 0 or yi < 0 or xi >= tw or yi >= th:
                        continue
                    o = (yi * tw + xi) * 4
                    if td[o + 3] < 26:
                        continue
                    depth[i] = z
                    base[i] = (min(255, int(td[o] * tr)), min(255, int(td[o + 1] * tg)), min(255, int(td[o + 2] * tb)))


# --- Structure views -----------------------------------------------------------------------------------------------

def sky_frame(width, height):
    frame = Frame(width, height, SKY_TOP)
    for y in range(height):
        t = min(1.0, y / (height * 0.55))
        c = tuple(int(SKY_TOP[k] + (SKY_HORIZON[k] - SKY_TOP[k]) * t) for k in range(3))
        frame.base[y * width:(y + 1) * width] = [c] * width
    return frame


def fogged(frame, start, end, strength=0.65):
    """The frame as an image, faded toward the horizon colour with depth (the Realm's fog, roughly)."""
    img = frame.image()
    for i, d in enumerate(frame.depth):
        if d != math.inf and d > start:
            t = min(1.0, (d - start) / (end - start)) * strength
            c = img.px[i]
            img.px[i] = tuple(int(c[k] + (SKY_HORIZON[k] - c[k]) * t) for k in range(3))
    return img


def relief(frame):
    """A top-down frame as an image: lower surfaces darker, a dark edge where the height steps."""
    img = frame.image()
    W, H = frame.width, frame.height
    depth = frame.depth
    finite = [d for d in depth if d != math.inf]
    if not finite:
        return img
    lo, hi = min(finite), max(finite)
    span = max(1.0, hi - lo)
    for i, d in enumerate(depth):
        if d == math.inf:
            continue
        k = 1.0 - 0.35 * (d - lo) / span
        x, y = i % W, i // W
        for j in (i + 1 if x + 1 < W else None, i + W if y + 1 < H else None, i - 1 if x else None,
                  i - W if y else None):
            if j is not None and depth[j] < d - 0.6:
                k *= 0.55
                break
        c = img.px[i]
        img.px[i] = (int(c[0] * k), int(c[1] * k), int(c[2] * k))
    return img


def pin_colors(markers):
    out = {}
    for m in markers:
        out.setdefault(m.entity, PIN_COLORS[len(out) % len(PIN_COLORS)])
    return out


def hidden_from(frame, cam, point):
    """True when the depth buffer has something in front of a world point."""
    c = cam.cam(point)
    s = cam.screen(c)
    if s is None:
        return True
    x, y = int(s[0]), int(s[1])
    if not (0 <= x < frame.width and 0 <= y < frame.height):
        return False
    return frame.depth[y * frame.width + x] < c[2] - 0.3


def draw_pins(img, frame, cam, markers, colors, top=False):
    """Numbered pins over a rendered view: a stalk from the feet up two blocks (on top views a dot), a tick toward
    the facing; dashed stalk and hollow label when the spot is behind or under something."""
    for i, m in enumerate(markers, 1):
        x, y, z = m.pos
        color = colors[m.entity]
        foot = cam.screen(cam.cam((x, y, z)))
        head = cam.screen(cam.cam((x, y + (0 if top else 2.2), z)))
        if foot is None or head is None:
            continue
        hidden = hidden_from(frame, cam, (x, y + 0.9, z))
        d = FACE_DIRS.get(m.facing, (0, 0, 0))
        tip = cam.screen(cam.cam((x + d[0] * 0.9, y, z + d[2] * 0.9)))
        if tip:
            img.line(foot[0], foot[1], tip[0], tip[1], color, 1.0)
            img.line(foot[0] + 1, foot[1], tip[0] + 1, tip[1], color, 1.0)
        if not top:
            img.line(foot[0], foot[1], head[0], head[1], color, 0.9, dash=3 if hidden else 0)
        img.fill(int(foot[0]) - 2, int(foot[1]) - 2, 5, 5, color)
        label = str(i)
        w, h = text_width(label) + 4, 14
        lx, ly = int(head[0]) - w // 2, int(head[1]) - (h + 4 if not top else h // 2 + 10)
        if hidden:
            img.fill(lx, ly, w, h, (24, 24, 28))
            for xx in range(lx, lx + w):
                img.set(xx, ly, color)
                img.set(xx, ly + h - 1, color)
            for yy in range(ly, ly + h):
                img.set(lx, yy, color)
                img.set(lx + w - 1, yy, color)
            img.text(lx + 3, ly + 2, label, color)
        else:
            img.fill(lx, ly, w, h, color)
            img.text(lx + 3, ly + 2, label, (16, 16, 20))


def legend(markers, colors, width):
    """The pins' key: number, entity, feet position, facing, tags."""
    img = Image(width, 30 + 16 * max(1, len(markers)), (34, 36, 42))
    img.text(8, 8, "PLACED ENEMIES %d" % len(markers))
    if not markers:
        img.text(8, 30, "NONE", (170, 170, 170))
    for i, m in enumerate(markers, 1):
        y = 30 + (i - 1) * 16
        img.fill(8, y, 10, 10, colors[m.entity])
        tags = ",".join(t.replace(MOD + ".placed.", "") for t in m.tags if t != MOD + ".placed")
        pos = "%g,%g,%g" % (math.floor(m.pos[0]), m.pos[1], math.floor(m.pos[2]))
        line = "%d %s %s %s %s" % (i, split_id(m.entity, MOD)[1], pos, m.facing[0].upper(), tags)
        img.text(24, y, line[:(width - 30) // 8], (225, 225, 225))
    return img


def template_box(tpl):
    sx, sy, sz = tpl.size
    return [(x, y, z) for x in (0, sx) for y in (0, sy) for z in (0, sz)]


def draw_box(img, cam, tpl, color=(240, 200, 120)):
    pts = [cam.screen(cam.cam(p)) for p in template_box(tpl)]
    for i in range(8):
        for j in range(i + 1, 8):
            if bin(i ^ j).count("1") == 1 and pts[i] and pts[j]:
                img.line(pts[i][0], pts[i][1], pts[j][0], pts[j][1], color, 0.5, dash=4)


def top_view(tpl, quads, ground, max_px=1100):
    """Straight down, north up, rulers in blocks along the top and left edges."""
    sx, sy, sz = tpl.size
    margin = 2
    s = max(4, min(32, int(min(max_px / (sx + 2 * margin), max_px / (sz + 2 * margin)))))
    W, H = (sx + 2 * margin) * s, (sz + 2 * margin) * s
    cam = Camera((sx / 2, sy + 64, sz / 2), 180, 90, W, H, scale=s)
    frame = Frame(W, H, (30, 32, 38))
    scene = list(quads)
    if ground is not None:
        scene.append(ground_plane(-margin, -margin, sx + margin, sz + margin, ground))
    draw_scene(frame, scene, cam)
    img = relief(frame)
    step = 1 if s >= 8 else 2
    label_step = 4 if s >= 12 else (8 if s >= 6 else 16)
    small = 1 if s < 12 else 2
    for x in range(0, sx + 1, step):
        px = int(W / 2 + (x - sx / 2) * s)
        img.line(px, 0, px, 4 if x % label_step else 9, (220, 220, 220))
        if x % label_step == 0 and x < sx:
            img.text(px + 2, 2, str(x), (220, 220, 220), small)
    for z in range(0, sz + 1, step):
        py = int(H / 2 + (z - sz / 2) * s)
        img.line(0, py, 4 if z % label_step else 9, py, (220, 220, 220))
        if z % label_step == 0 and z < sz:
            img.text(2, py + 2, str(z), (220, 220, 220), small)
    img.text(W - 26, 6, "N", (255, 255, 255))
    img.line(W - 22, 18, W - 22, 34, (255, 255, 255))
    return img, frame, cam


def iso_view(tpl, quads, ground, yaw, width, height):
    """Orthographic from above a corner, fitted to the template's box."""
    sx, sy, sz = tpl.size
    centre = (sx / 2, sy / 2, sz / 2)
    probe = Camera(centre, yaw, 30, width, height)
    cs = [probe.cam(p) for p in template_box(tpl)]
    xs, ys = [c[0] for c in cs], [c[1] for c in cs]
    s = min(width * 0.9 / max(1e-3, max(xs) - min(xs)), (height - 30) * 0.9 / max(1e-3, max(ys) - min(ys)))
    cam = Camera(centre, yaw, 30, width, height, scale=s,
                 shift=((max(xs) + min(xs)) / 2, (max(ys) + min(ys)) / 2 - 12 / s))
    frame = Frame(width, height, BACKGROUND)
    scene = list(quads)
    if ground is not None:
        scene.append(ground_plane(-3, -3, sx + 3, sz + 3, ground))
    draw_scene(frame, scene, cam)
    img = frame.image()
    draw_box(img, cam, tpl)
    return img, frame, cam


def look_yaw(dx, dz):
    """Minecraft yaw looking along (dx, dz)."""
    return math.degrees(math.atan2(-dx, dz))


def approach_cameras(tpl, ground, width, height):
    """Eye-level cameras walking up to the template from each side, aimed at its middle."""
    sx, sy, sz = tpl.size
    out = []
    for label, dx, dz, half_w, half_d in (("FROM SOUTH", 0, 1, sx / 2, sz / 2), ("FROM EAST", 1, 0, sz / 2, sx / 2),
                                          ("FROM NORTH", 0, -1, sx / 2, sz / 2), ("FROM WEST", -1, 0, sz / 2, sx / 2)):
        dist = half_d + max(6, half_w * 1.05, sy - ground - 1)  # the whole facade in a 70 degree, 16:10 view
        eye = (sx / 2 + dx * dist, ground + 1.62, sz / 2 + dz * dist)
        target = ground + max(0, sy - ground) * 0.35
        pitch = max(-30, min(15, -math.degrees(math.atan2(target - eye[1], dist))))
        out.append((label, Camera(eye, look_yaw(-dx, -dz), pitch, width, height, fov=70)))
    return out


def eye_render(tpl, quads, ground, cam):
    frame = sky_frame(cam.width, cam.height)
    scene = list(quads)
    if ground is not None:
        sx, _, sz = tpl.size
        scene.append(ground_plane(-96, -96, sx + 96, sz + 96, ground))
    draw_scene(frame, scene, cam)
    return fogged(frame, 24, 120), frame


def structure_sheets(name, out, views=("top", "iso", "eye"), eye=None, look=None, pitch=0.0, ground="auto"):
    path, name = structure_file(name)
    tpl = Template(name, path)
    flat = name.replace("/", "_")
    gy = ground_level(tpl) if ground == "auto" else (None if ground == "none" else float(ground))
    quads = structure_quads(tpl)
    colors = pin_colors(tpl.markers)
    sx, sy, sz = tpl.size
    title = "%s  %dx%dx%d  blocks %d  faces %d  enemies %d  loot %d  ground %s" % (
        name.upper(), sx, sy, sz, len(tpl.blocks), len(quads), len(tpl.markers), tpl.chests,
        "none" if gy is None else "%g" % gy)
    target = os.path.join(out, "structures")
    if "top" in views:
        img, frame, cam = top_view(tpl, quads, gy)
        draw_pins(img, frame, cam, tpl.markers, colors, top=True)
        key = legend(tpl.markers, colors, 380)
        sheet = Image(img.width + key.width + 10, max(img.height, key.height) + 30)
        sheet.text(8, 8, title + "  TOP, N UP")
        sheet.blit(img, 0, 30)
        sheet.blit(key, img.width + 10, 30)
        sheet.save(os.path.join(target, flat + "_top.png"))
    if "iso" in views:
        cells = []
        for yaw, label in ((135, "FROM SE"), (-45, "FROM NW")):
            img, frame, cam = iso_view(tpl, quads, gy, yaw, 760, 560)
            draw_pins(img, frame, cam, tpl.markers, colors)
            img.text(6, img.height - 16, label, (220, 220, 220))
            cells.append(img)
        key = legend(tpl.markers, colors, 380)
        sheet = Image(760 * 2 + key.width + 10, max(560, key.height) + 30)
        sheet.text(8, 8, title + "  ISO")
        for i, img in enumerate(cells):
            sheet.blit(img, i * 760, 30)
        sheet.blit(key, 1530, 30)
        sheet.save(os.path.join(target, flat + "_iso.png"))
    if "eye" in views:
        if eye is not None:
            ex, ey, ez = eye
            feet = (ex + 0.5, ey, ez + 0.5)
            yaw = look if look is not None else look_yaw(sx / 2 - feet[0], sz / 2 - feet[2] or 1e-6)
            cam = Camera((feet[0], ey + 1.62, feet[2]), yaw, pitch, 1280, 720, fov=70)
            img, frame = eye_render(tpl, quads, gy, cam)
            draw_pins(img, frame, cam, tpl.markers, colors)
            sheet = Image(1280, 750)
            sheet.text(8, 8, title + "  EYE %g,%g,%g YAW %d PITCH %d" % (ex, ey, ez, round(yaw), round(pitch)))
            sheet.blit(img, 0, 30)
            suffix = "_eye_%d_%d_%d" % (ex, ey, ez)
        else:
            sheet = Image(640 * 2, 400 * 2 + 30)
            sheet.text(8, 8, title + "  EYE LEVEL, PINS AS ON TOP")
            for i, (label, cam) in enumerate(approach_cameras(tpl, 1 if gy is None else gy, 640, 400)):
                img, frame = eye_render(tpl, quads, gy, cam)
                draw_pins(img, frame, cam, tpl.markers, colors)
                img.text(6, img.height - 16, label, (240, 240, 240))
                sheet.blit(img, (i % 2) * 640, 30 + (i // 2) * 400)
            suffix = "_eye"
        sheet.save(os.path.join(target, flat + suffix + ".png"))


# --- Motion: animation strips and GIFs -----------------------------------------------------------------------------

def frame_times(animation, fps):
    """(length, loops?, times) sampling an animation at a fixed rate; a loop leaves out its end (= its start), a
    one-shot ends on its last frame."""
    length = float(animation.get("animation_length", 0) or 0) or max(key_times(animation, 999)) or 1.0
    loop = animation.get("loop") in (True, "loop", "true")
    times = [k / fps for k in range(int(math.floor(length * fps + 1e-6)) + 1)]
    if loop and len(times) > 1 and length - times[-1] < 1e-6:
        times.pop()
    elif not loop and length - times[-1] > 1e-6:
        times.append(length)
    return length, loop, times


def trail_tint(t):
    return (int(70 + 185 * t), int(150 - 10 * t), int(255 - 205 * t))


def trail_cell(sets, view, w, h, fit, hb, ghosts=8):
    """The first frame dimmed, under the outlines of up to `ghosts` evenly spaced frames coloured blue (start) to
    orange (end): where the outlines fan out, that part moves."""
    scale, mx, my = fit
    n = len(sets)
    picks = sorted({round(i * (n - 1) / (ghosts - 1)) for i in range(ghosts)}) if n > 1 else [0]
    img = render_cell(sets[0], view, w, h, fit, hb)
    img.px = [(c[0] * 9 // 20, c[1] * 9 // 20, c[2] * 9 // 20) for c in img.px]
    for k in picks:
        frame = Frame(w, h, BACKGROUND)
        frame.draw(sets[k], view, scale, w / 2 - mx * scale, h / 2 + my * scale)
        d, inf = frame.depth, math.inf
        color = trail_tint(k / max(1, n - 1))
        for i, z in enumerate(d):
            if z == inf:
                continue
            x = i % w
            if x == 0 or x == w - 1 or i < w or i >= len(d) - w or inf in (d[i - 1], d[i + 1], d[i - w], d[i + w]):
                img.px[i] = color
    for x in range(w - 12):
        img.fill(6 + x, h - 6, 1, 3, trail_tint(x / (w - 13)))
    return img


def motion_sheet(name, out, hidden=(), anims="all", fps=10, yaw=35, gif=True, gif_fps=20, cols=10, cell=170):
    """<name>_motion.png: per animation a trail cell, then frames at a fixed rate (cols per row); and per animation a
    looping GIF at gif_fps (a one-shot holds its last frame half a second)."""
    model, tex, glow, animations = load_entity(name)
    hb = hitboxes().get(name)
    view = View(yaw, 15)
    names = list(animations) if anims == "all" else [a for a in anims.split(",") if a in animations]
    if not names:
        raise SystemExit("%s: no animations %s" % (name, anims))
    rows = []
    for an in names:
        length, loop, times = frame_times(animations[an], fps)
        rows.append((an, length, loop, times, [model.quads(pose_at(animations[an], t), hidden, tex, glow) for t in times]))
    cell_w, cell_h = cell, int(cell * 1.2)
    all_sets = [qs for row in rows for qs in row[4]]
    fit = fit_for(all_sets, [view], cell_w, cell_h, hb, 0.9)
    label_h = 22
    lines = [math.ceil((len(row[3]) + 1) / (cols + 1)) for row in rows]
    sheet = Image(cell_w * (cols + 1), sum(label_h + cell_h * k for k in lines) + 4)
    y = 0
    for (an, length, loop, times, sets), k in zip(rows, lines):
        sheet.fill(0, y, sheet.width, label_h, (34, 36, 42))
        sheet.text(6, y + 4, "%s  %.2fS  %s  %d FPS, %d FRAMES" % (an, length, "LOOP" if loop else "ONCE", fps,
                                                                    len(times)))
        cells = [trail_cell(sets, view, cell_w, cell_h, fit, hb)]
        cells[0].text(4, cell_h - 22, "TRAIL", (230, 230, 230))
        for t, qs in zip(times, sets):
            img = render_cell(qs, view, cell_w, cell_h, fit, hb)
            img.text(4, cell_h - 14, "T %.2f" % t, (210, 210, 210))
            cells.append(img)
        for i, img in enumerate(cells):
            sheet.blit(img, (i % (cols + 1)) * cell_w, y + label_h + (i // (cols + 1)) * cell_h)
        y += label_h + cell_h * k
    sheet.save(os.path.join(out, name + "_motion.png"))
    if not gif:
        return
    gw, gh = 320, 400
    gfit = fit_for(all_sets, [view], gw, gh, hb, 0.9)
    for an, length, loop, _, _ in rows:
        _, _, times = frame_times(animations[an], gif_fps)
        frames = []
        for t in times:
            img = render_cell(model.quads(pose_at(animations[an], t), hidden, tex, glow), view, gw, gh, gfit, hb)
            img.text(6, 6, an, (235, 235, 235))
            img.text(6, gh - 30, "T %.2f / %.2f" % (t, length), (220, 220, 220))
            img.fill(6, gh - 12, int((gw - 12) * (t / length if length else 0)), 4, (255, 180, 80))
            frames.append(img.px)
        delays = [max(2, round(100 / gif_fps))] * len(frames)
        if not loop:
            delays[-1] += 50
        path = os.path.join(out, "%s_%s.gif" % (name, an.replace(".", "_")))
        write_gif(path, gw, gh, frames, delays)
        print("wrote", path)


# --- Player attack animations (playerAnimator, PLAN 17.1) ----------------------------------------------------------

PLAYER_ANIMS = os.path.join(ASSETS, MOD, "player_animation")
# Vanilla player model parts (wide arms, 64x64 skin): cube origin and size in Bedrock units (y up, feet at 0), box UV,
# and the part's pivot in vanilla model space (y down, 24 at the feet).
PLAYER_PARTS = {
    "head": ([-4, 24, -4], [8, 8, 8], [0, 0], (0, 0, 0)),
    "torso": ([-4, 12, -2], [8, 12, 4], [16, 16], (0, 0, 0)),
    "rightArm": ([-8, 12, -2], [4, 12, 4], [40, 16], (-5, 2, 0)),
    "leftArm": ([4, 12, -2], [4, 12, 4], [32, 48], (5, 2, 0)),
    "rightLeg": ([-3.9, 0, -2], [4, 12, 4], [0, 16], (-1.9, 12, 0)),
    "leftLeg": ([-0.1, 0, -2], [4, 12, 4], [16, 48], (1.9, 12, 0)),
}
# The pose a part has when a clip does not key it: standing, the right arm holding an item (vanilla ArmPose.ITEM).
PLAYER_REST = {"rightArm": {"pitch": -18}}
# Vanilla model space -> render space (x mirrored and y flipped as LivingEntityRenderer does, feet at y = 0) and back.
MODEL_TO_RENDER = mul(translate(0, 24, 0), scale_m(-1, -1, 1))
RENDER_TO_MODEL = mul(scale_m(-1, -1, 1), translate(0, -24, 0))
EASINGS = {
    "linear": lambda f: f, "step": lambda f: 0.0, "constant": lambda f: 0.0,
    "insine": lambda f: 1 - math.cos(f * math.pi / 2), "outsine": lambda f: math.sin(f * math.pi / 2),
    "inoutsine": lambda f: -(math.cos(math.pi * f) - 1) / 2,
    "inquad": lambda f: f * f, "outquad": lambda f: 1 - (1 - f) ** 2,
    "inoutquad": lambda f: 2 * f * f if f < 0.5 else 1 - (-2 * f + 2) ** 2 / 2,
    "incubic": lambda f: f ** 3, "outcubic": lambda f: 1 - (1 - f) ** 3,
    "inoutcubic": lambda f: 4 * f ** 3 if f < 0.5 else 1 - (-2 * f + 2) ** 3 / 2,
}


def player_clip_names():
    return sorted(os.path.basename(p)[:-5] for p in glob.glob(os.path.join(PLAYER_ANIMS, "*.json")))


def load_player_clip(name):
    """An Emotecraft-format clip as {part: {axis: [(tick, value, ease)]}} plus its end and stop ticks."""
    with open(os.path.join(PLAYER_ANIMS, name + ".json"), encoding="utf-8") as f:
        emote = json.load(f)["emote"]
    if not emote.get("degrees", True):
        raise SystemExit("%s: radians are not supported here" % name)
    tracks = {}
    for move in emote["moves"]:
        ease = move.get("easing", "linear").lower()
        ease = ease[4:] if ease.startswith("ease") else ease
        for part, axes in move.items():
            if part in ("tick", "easing", "turn", "comment"):
                continue
            for axis, value in axes.items():
                tracks.setdefault(part, {}).setdefault(axis, []).append((move["tick"], float(value), ease))
    for axes in tracks.values():
        for keys in axes.values():
            keys.sort(key=lambda k: k[0])
    end = emote["endTick"]
    stop = emote.get("stopTick", end)
    stop = stop if stop > end else end + 3
    return tracks, end, stop, emote.get("easeBeforeKeyframe", False)


def player_axis(keys, t, end, stop, rest, ease_before):
    """One axis at tick t, as playerAnimator's KeyframeAnimationPlayer: held before the first and after the last key,
    and from endTick to stopTick blended linearly back into the vanilla value."""
    if t >= end:
        last = keys[-1][1]
        return last + (rest - last) * min(1.0, (t - end) / max(1, stop - end))
    if t <= keys[0][0]:
        return keys[0][1]
    for (ta, va, ea), (tb, vb, eb) in zip(keys, keys[1:]):
        if ta <= t <= tb:
            f = (t - ta) / (tb - ta) if tb > ta else 1.0
            return va + (vb - va) * EASINGS.get(eb if ease_before else ea, EASINGS["linear"])(f)
    return keys[-1][1]


def player_pose(clip, t):
    """{part: {axis: value}} at tick t: keyed parts from the clip, the rest at the standing pose."""
    tracks, end, stop, ease_before = clip
    pose = {}
    for part in list(PLAYER_PARTS) + ["body", "rightItem"]:
        rest = PLAYER_REST.get(part, {})
        defaults = {"x": 0.0, "y": 0.0, "z": 0.0}
        if part in PLAYER_PARTS and part != "torso":
            defaults = dict(zip("xyz", PLAYER_PARTS[part][3]))
        values = {}
        for axis in ("pitch", "yaw", "roll", "x", "y", "z"):
            base = rest.get(axis, defaults.get(axis, 0.0))
            keys = tracks.get(part, {}).get(axis)
            values[axis] = player_axis(keys, t, end, stop, base, ease_before) if keys else base
        pose[part] = values
    return pose


def part_rotation(v):
    """A vanilla ModelPart rotation (rotationZYX(roll, yaw, pitch)), degrees."""
    return mul(mul(rot_z(math.radians(v["roll"])), rot_y(math.radians(v["yaw"]))), rot_x(math.radians(v["pitch"])))


def stick_quads(m, a, b, width, color):
    """A square bar from a to b (item model pixels) through matrix m, flat colour."""
    d = (b[0] - a[0], b[1] - a[1], b[2] - a[2])
    length = math.sqrt(sum(c * c for c in d)) or 1
    u = (-d[1] / length * width, d[0] / length * width, 0) if abs(d[2]) < length * 0.9 else (width, 0, 0)
    w = (0, 0, width)
    p = apply(m, (a[0] - (u[0] + w[0]) / 2, a[1] - (u[1] + w[1]) / 2, a[2] - (u[2] + w[2]) / 2))
    ed, eu, ew = apply_dir(m, d), apply_dir(m, u), apply_dir(m, w)
    far = tuple(p[i] + ed[i] + eu[i] + ew[i] for i in range(3))
    neg = tuple(-c for c in ed), tuple(-c for c in eu), tuple(-c for c in ew)

    def quad(o, e1, e2, shade):
        c = tuple(int(x * shade) for x in color)
        return Quad(o, e1, e2, (0, 0), (0, 0), (0, 0), None, color=lambda _a, _b: c)

    return [quad(p, ed, eu, 0.8), quad(p, eu, ew, 0.7), quad(p, ew, ed, 0.95),
            quad(far, neg[0], neg[1], 0.8), quad(far, neg[1], neg[2], 0.7), quad(far, neg[2], neg[0], 0.95)]


def player_quads(pose, skin, scourge=False, root=None):
    """Render-space quads of the player in a pose, with a sword-sized bar for the item in the right hand (the vanilla
    handheld transform, then the clip's rightItem turn). With scourge, the Warden's Scourge's handle instead, and
    root (a list) gets the render-space point the lash leaves it (ScourgeLashes.ROOT)."""
    b = pose["body"]
    # The whole-body turn: around a point 0.7 blocks up, in the entity's own frame (y up), position in blocks.
    body_m = translate(b["x"] * 16, b["y"] * 16 + 11.2, b["z"] * 16)
    body_m = mul(body_m, mul(mul(rot_z(math.radians(b["roll"])), rot_y(math.radians(b["yaw"]))), rot_x(math.radians(b["pitch"]))))
    body_m = mul(body_m, translate(0, -11.2, 0))
    quads, arm_model = [], None
    for part, (origin, size, uv, rest_pivot) in PLAYER_PARTS.items():
        v = pose[part]
        pivot = (v["x"], v["y"], v["z"]) if part != "torso" else rest_pivot
        model = mul(translate(*pivot), part_rotation(v))
        if part == "rightArm":
            arm_model = model
        bone = mul(mul(mul(body_m, MODEL_TO_RENDER), mul(model, translate(-rest_pivot[0], -rest_pivot[1], -rest_pivot[2]))),
                   RENDER_TO_MODEL)
        quads += cube_quads({"origin": origin, "size": size, "uv": uv}, bone, skin, None)
    # HeldItemFeatureRenderer: X -90, Y 180, (1, 2, -10) px; playerAnimator's rightItem; the handheld display transform
    # (thirdperson_righthand: translation (0, 4, 0.5), rotation XYZ (0, -90, 55), scale 0.85); the item centred.
    m = mul(arm_model, mul(rot_x(math.radians(-90)), rot_y(math.radians(180))))
    m = mul(m, translate(1, 2, -10))
    m = mul(m, part_rotation(pose["rightItem"]))
    m = mul(m, translate(0, 4, 0.5))
    m = mul(m, mul(mul(rot_x(0), rot_y(math.radians(-90))), rot_z(math.radians(55))))
    m = mul(m, mul(scale_m(0.85, 0.85, 0.85), translate(-8, -8, -8)))
    m = mul(mul(body_m, MODEL_TO_RENDER), m)
    if scourge:
        quads += stick_quads(m, (1, 1, 8), (8, 8, 8), 1.6, (70, 56, 48))          # grip (wardens_scourge_handle)
        quads += stick_quads(m, (8, 8, 8), (9.5, 9.5, 8), 1.8, (120, 128, 146))   # iron ferrule
        if root is not None:
            root.append(apply(m, (9.5, 9.5, 8)))
        return quads
    quads += stick_quads(m, (1, 1, 8), (4, 4, 8), 1.6, (110, 70, 40))     # grip
    quads += stick_quads(m, (4, 4, 8), (15.5, 15.5, 8), 1.2, (205, 210, 220))  # blade, tip at the sprite's top right
    return quads


# --- The Warden's Scourge's lash (PLAN 17.4): WhipArc and ScourgeLashes ported, to see the lash with the whip clips --

WHIP_REACH, WHIP_HAND, WHIP_BOW = 6.0, (0.3, -0.35, 0.45), (0.25, 0.45)
WHIP_UNROLL, WHIP_LAG, WHIP_OVERSHOOT, WHIP_OVERSHOOT_TICKS = 2.0, 2.0, 12.0, 1.0
WHIP_WINDUP = {"whip_crack": 135.0, "whip_lash": 35.0}
WHIP_YANK = {"whip_crack": 0.0, "whip_lash": 0.8}
WHIP_RETRACT_FROM = {"whip_crack": 9.0, "whip_lash": 5.0}
LASH_LINKS, LASH_IDLE, LASH_PAYOUT, LASH_RETRACT = 20, 0.8, 2.5, 0.6
LASH_STEP, LASH_GRAVITY, LASH_DRAG, LASH_ITERATIONS = 0.25, 0.06, 0.9, 12
EYE_PX = 1.62 * 16


def v_add(a, b):
    return tuple(x + y for x, y in zip(a, b))


def v_sub(a, b):
    return tuple(x - y for x, y in zip(a, b))


def v_mul(a, k):
    return tuple(x * k for x in a)


def v_len(a):
    return math.sqrt(sum(x * x for x in a))


def v_cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


class WhipArcPy:
    """WhipArc in the look frame (x right, y up, z ahead, blocks from the eyes)."""

    def __init__(self, reach=WHIP_REACH, table=128):
        self.points = [self.curve(i / table, reach) for i in range(table + 1)]
        self.lengths = [0.0]
        for a, b in zip(self.points, self.points[1:]):
            self.lengths.append(self.lengths[-1] + v_len(v_sub(b, a)))
        frm = (0.5, 1.0, 0.0)
        n = v_len(frm)
        frm = v_mul(frm, 1 / n)
        axis = v_cross((0, 0, 1), frm)
        self.axis = v_mul(axis, 1 / v_len(axis))

    @staticmethod
    def curve(t, reach):
        u = 1 - t
        bow = (WHIP_BOW[0], WHIP_BOW[1], reach / 2)
        return tuple(WHIP_HAND[i] * u * u + bow[i] * 2 * u * t + (0, 0, reach)[i] * t * t for i in range(3))

    def length(self):
        return self.lengths[-1]

    def point(self, along):
        if along <= 0:
            return self.points[0]
        if along >= self.length():
            return self.points[-1]
        i = max(k for k, l in enumerate(self.lengths) if l <= along)
        f = (along - self.lengths[i]) / (self.lengths[i + 1] - self.lengths[i])
        return v_add(self.points[i], v_mul(v_sub(self.points[i + 1], self.points[i]), f))

    def crack_age(self, along):
        return WHIP_LAG * min(1.0, max(0.0, along / self.length())) + WHIP_UNROLL

    def swing(self, age, along, windup):
        x = (age - WHIP_LAG * min(1.0, max(0.0, along / self.length()))) / WHIP_UNROLL
        if x <= 0:
            angle = windup
        elif x <= 1:
            angle = windup * (1 - x * x)
        else:
            angle = -WHIP_OVERSHOOT * min(1.0, (x - 1) * WHIP_UNROLL / WHIP_OVERSHOOT_TICKS)
        v = v_sub(self.point(along), WHIP_HAND)
        a = math.radians(angle)
        k = self.axis
        dot = sum(p * q for p, q in zip(k, v))
        r = v_add(v_add(v_mul(v, math.cos(a)), v_mul(v_cross(k, v), math.sin(a))), v_mul(k, dot * (1 - math.cos(a))))
        return v_add(WHIP_HAND, r)


def look_to_render(p):
    """Look frame (blocks; the player looks level along -z in render space, right is +x) -> render pixels."""
    return (p[0] * 16, EYE_PX + p[1] * 16, -p[2] * 16)


def lash_frames(clip_name, roots, ticks):
    """The lash's links (render pixels) at each of ticks, as ScourgeLashes steps it: laid along the swing until the tip
    has cracked, then let go (gravity, drag, links at their length, the ground at y = 0) and coiled back in."""
    arc = WhipArcPy()
    windup, yank = WHIP_WINDUP.get(clip_name, 0.0), WHIP_YANK.get(clip_name, 0.0)
    swinging = clip_name in WHIP_WINDUP
    blocks = [v_mul(r, 1 / 16) for r in roots]          # render pixels -> blocks
    pos = [v_add(blocks[0], (0, -LASH_IDLE * i / LASH_LINKS, 0)) for i in range(LASH_LINKS + 1)]
    speed = [(0.0, 0.0, 0.0)] * (LASH_LINKS + 1)
    length, kinematic, out = LASH_IDLE, False, {}
    hand = v_mul(look_to_render(WHIP_HAND), 1 / 16)
    per_tick = int(round(1 / LASH_STEP))
    for n in range(max(ticks) * per_tick + 1):
        age = n * LASH_STEP
        anchor = blocks[min(len(blocks) - 1, int(round(age)))]
        if swinging and age < arc.crack_age(arc.length()) + 0.5:
            length = min(arc.length(), LASH_IDLE + (arc.length() - LASH_IDLE) * age / LASH_PAYOUT)
            offset = v_sub(anchor, hand)
            new = [anchor]
            for i in range(1, LASH_LINKS + 1):
                share = i / LASH_LINKS
                p = v_mul(look_to_render(arc.swing(age, share * length, windup)), 1 / 16)
                new.append(v_add(p, v_mul(offset, 1 - share)))
            speed = [v_mul(v_sub(a, b), 1 / LASH_STEP) for a, b in zip(new, pos)]
            pos, kinematic = new, True
        elif n > 0:
            if kinematic and yank > 0:
                for i in range(1, LASH_LINKS + 1):
                    d = v_sub(anchor, pos[i])
                    speed[i] = v_add(speed[i], v_mul(d, yank / (v_len(d) or 1)))
            kinematic = False
            if not swinging or age > WHIP_RETRACT_FROM[clip_name]:
                length = max(LASH_IDLE, length - LASH_RETRACT * LASH_STEP)
            keep = LASH_DRAG ** LASH_STEP
            nxt = [anchor]
            for i in range(1, LASH_LINKS + 1):
                speed[i] = v_mul(v_add(speed[i], (0, -LASH_GRAVITY * LASH_STEP, 0)), keep)
                nxt.append(v_add(pos[i], v_mul(speed[i], LASH_STEP)))
            rest = length / LASH_LINKS
            for _ in range(LASH_ITERATIONS):
                for i in range(LASH_LINKS):
                    d = v_sub(nxt[i + 1], nxt[i])
                    ln = v_len(d)
                    if ln < 1e-6:
                        continue
                    fix = v_mul(d, (ln - rest) / ln)
                    if i == 0:
                        nxt[1] = v_sub(nxt[1], fix)
                    else:
                        nxt[i] = v_add(nxt[i], v_mul(fix, 0.5))
                        nxt[i + 1] = v_sub(nxt[i + 1], v_mul(fix, 0.5))
            for i in range(1, LASH_LINKS + 1):
                p = nxt[i]
                grounded = p if p[1] >= 0.03 else (p[0], 0.03, p[2])
                moved = v_mul(v_sub(grounded, pos[i]), 1 / LASH_STEP)
                speed[i] = moved if grounded is p else (moved[0] * 0.5, 0.0, moved[2] * 0.5)
                pos[i] = grounded
        if n % per_tick == 0:
            out[n // per_tick] = [v_mul(p, 16) for p in pos]
    return out


def lash_quads(links):
    """The links as bars, iron narrowing to the tip, the last one the red popper."""
    quads = []
    ident = translate(0, 0, 0)
    for i, (a, b) in enumerate(zip(links, links[1:])):
        if v_len(v_sub(b, a)) < 1e-3:
            continue
        popper = i == len(links) - 2
        width = 0.06 * 16 if popper else (0.075 + (0.035 - 0.075) * i / LASH_LINKS) * 16
        quads += stick_quads(ident, a, b, width, (170, 34, 36) if popper else (128, 136, 154))
    return quads


def player_motion_sheet(names, out, gif=True, cols=12, cell=150):
    """player_<clip>.png: per clip a trail cell and every frame from the side and from above; player_<clip>.gif."""
    skin = texture("minecraft", "entity/player/wide/steve")
    if skin is None:
        raise SystemExit("no vanilla skin: the 1.20.1 client jar is not in ~/.gradle/caches/fabric-loom")
    prepare(skin, 64, 64)
    views = [View(35, 15, "SIDE"), View(180, 80, "ABOVE")]
    for name in names:
        clip = load_player_clip(name)
        stop = clip[2]
        step = 1 if stop <= 24 else 2
        ticks = list(range(0, stop + 1, step))
        if ticks[-1] != stop:
            ticks.append(stop)
        whip = name.startswith("whip_")
        lash = {}
        if whip:
            roots = []
            for t in range(stop + 1):
                player_quads(player_pose(clip, t), skin, True, roots)
            lash = lash_frames(name, roots, range(stop + 1))
        sets = [player_quads(player_pose(clip, t), skin, whip) + (lash_quads(lash[t]) if whip else []) for t in ticks]
        cell_w, cell_h = cell, int(cell * 1.2)
        fit = fit_for(sets, views, cell_w, cell_h, None, 0.85)
        rows = math.ceil((len(ticks) + 1) / (cols + 1))
        label_h = 22
        sheet = Image(cell_w * (cols + 1), (label_h + cell_h * rows) * len(views) + 4)
        y = 0
        for view in views:
            sheet.fill(0, y, sheet.width, label_h, (34, 36, 42))
            sheet.text(6, y + 4, "%s  %d TICKS (%.2fS)  KEYS END AT %d  %s" % (name, stop, stop / 20, clip[1], view.label))
            cells = [trail_cell(sets, view, cell_w, cell_h, fit, None)]
            cells[0].text(4, cell_h - 22, "TRAIL", (230, 230, 230))
            for t, qs in zip(ticks, sets):
                img = render_cell(qs, view, cell_w, cell_h, fit)
                img.text(4, cell_h - 14, "T %d" % t, (210, 210, 210))
                cells.append(img)
            for i, img in enumerate(cells):
                sheet.blit(img, (i % (cols + 1)) * cell_w, y + label_h + (i // (cols + 1)) * cell_h)
            y += label_h + cell_h * rows
        os.makedirs(out, exist_ok=True)
        sheet.save(os.path.join(out, "player_%s.png" % name))
        if not gif:
            continue
        gw, gh = 260, 320
        gfit = fit_for(sets, views, gw, gh, None, 0.85)
        frames = []
        for t in range(stop + 1):
            qs = player_quads(player_pose(clip, t), skin, whip) + (lash_quads(lash[t]) if whip else [])
            img = Image(gw * 2, gh)
            for k, view in enumerate(views):
                img.blit(render_cell(qs, view, gw, gh, gfit), k * gw, 0)
            img.text(6, 6, name, (235, 235, 235))
            img.text(6, gh - 30, "T %d / %d" % (t, stop), (220, 220, 220))
            img.fill(6, gh - 12, int((gw * 2 - 12) * t / stop), 4, (255, 180, 80))
            frames.append(img.px)
        delays = [5] * len(frames)
        delays[-1] += 50
        path = os.path.join(out, "player_%s.gif" % name)
        write_gif(path, gw * 2, gh, frames, delays)
        print("wrote", path)


# --- GIF -----------------------------------------------------------------------------------------------------------

def median_cut(hist, colors=256):
    """A palette for a histogram of 15-bit colours: boxes split at the weighted median of their widest channel,
    largest (pixels x extent) first. Returns (palette, lookup from 15-bit colour to palette index)."""

    def chan(k, axis):
        return (k >> (10 - 5 * axis)) & 31

    def score(items):
        extent = max(max(chan(k, a) for k, _ in items) - min(chan(k, a) for k, _ in items) for a in range(3))
        return extent * sum(c for _, c in items) if len(items) > 1 else -1

    boxes = [(score(list(hist.items())), list(hist.items()))]
    while len(boxes) < colors:
        boxes.sort(key=lambda b: b[0])
        if boxes[-1][0] <= 0:
            break
        _, items = boxes.pop()
        ranges = [max(chan(k, a) for k, _ in items) - min(chan(k, a) for k, _ in items) for a in range(3)]
        axis = ranges.index(max(ranges))
        items.sort(key=lambda kc: chan(kc[0], axis))
        half, acc, cut = sum(c for _, c in items) / 2, 0, 1
        for i, (_, c) in enumerate(items):
            acc += c
            if acc >= half:
                cut = i + 1
                break
        cut = min(max(cut, 1), len(items) - 1)
        boxes += [(score(items[:cut]), items[:cut]), (score(items[cut:]), items[cut:])]
    palette, lut = [], [0] * 32768
    for idx, (_, items) in enumerate(boxes):
        total = sum(c for _, c in items)
        palette.append(tuple(int(sum((chan(k, a) * 8 + 4) * c for k, c in items) / total) for a in range(3)))
        for k, _ in items:
            lut[k] = idx
    return palette, lut


def lzw_encode(indices, min_size=8):
    """GIF LZW (variable code size up to 12 bits, a clear code when the table is full), packed LSB first."""
    clear, end = 1 << min_size, (1 << min_size) + 1
    size, next_code = min_size + 1, end + 1
    table = {}
    out = bytearray()
    acc, nbits = clear, size
    prefix = indices[0]
    for k in indices[1:]:
        key = prefix << 8 | k
        c = table.get(key)
        if c is not None:
            prefix = c
            continue
        acc |= prefix << nbits
        nbits += size
        while nbits >= 8:
            out.append(acc & 255)
            acc >>= 8
            nbits -= 8
        if next_code < 4096:
            table[key] = next_code
            next_code += 1
            if next_code > (1 << size) and size < 12:
                size += 1
        else:
            acc |= clear << nbits
            nbits += size
            table = {}
            size, next_code = min_size + 1, end + 1
        prefix = k
    for code in (prefix, end):
        acc |= code << nbits
        nbits += size
        while nbits >= 8:
            out.append(acc & 255)
            acc >>= 8
            nbits -= 8
        if code == prefix and next_code < 4096:
            next_code += 1  # the decoder adds the last entry on reading this code, which may widen the end code
            if next_code > (1 << size) and size < 12:
                size += 1
    if nbits:
        out.append(acc & 255)
    return bytes(out)


def lzw_decode(data, min_size=8):
    """The inverse of lzw_encode (the self-check runs one through the other)."""
    clear, end = 1 << min_size, (1 << min_size) + 1
    size, next_code = min_size + 1, end + 1
    table = [bytes((i,)) for i in range(clear)] + [b"", b""]
    out = bytearray()
    acc = nbits = pos = 0
    prev = None
    while True:
        while nbits < size:
            if pos >= len(data):
                return bytes(out)
            acc |= data[pos] << nbits
            pos += 1
            nbits += 8
        code = acc & ((1 << size) - 1)
        acc >>= size
        nbits -= size
        if code == clear:
            size, next_code = min_size + 1, end + 1
            del table[end + 1:]
            prev = None
            continue
        if code == end:
            return bytes(out)
        if prev is None:
            entry = table[code]
        else:
            entry = table[code] if code < next_code else prev + prev[:1]
            if next_code < 4096:
                table.append(prev + entry[:1])
                next_code += 1
                if next_code == (1 << size) and size < 12:
                    size += 1
        out += entry
        prev = entry


def write_gif(path, width, height, frames, delays):
    """An animated GIF that loops: frames are flat lists of (r, g, b), one median-cut palette for all of them (no
    dithering), delays in 1/100 s."""
    keyed = [[(r >> 3) << 10 | (g >> 3) << 5 | b >> 3 for r, g, b in px] for px in frames]
    hist = {}
    for keys in keyed:
        for k, c in collections.Counter(keys).items():
            hist[k] = hist.get(k, 0) + c
    palette, lut = median_cut(hist)
    palette += [(0, 0, 0)] * (256 - len(palette))
    data = bytearray(b"GIF89a" + struct.pack("<HHBBB", width, height, 0xF7, 0, 0))
    for c in palette:
        data += bytes(c)
    data += b"\x21\xff\x0bNETSCAPE2.0\x03\x01\x00\x00\x00"
    for keys, delay in zip(keyed, delays):
        data += b"\x21\xf9\x04\x04" + struct.pack("<H", delay) + b"\x00\x00"
        data += b"\x2c" + struct.pack("<HHHHB", 0, 0, width, height, 0) + b"\x08"
        lz = lzw_encode(bytes(map(lut.__getitem__, keys)))
        for i in range(0, len(lz), 255):
            data += bytes((len(lz[i:i + 255]),)) + lz[i:i + 255]
        data += b"\x00"
    data += b"\x3b"
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "wb") as f:
        f.write(data)


# --- Asset check: what code relies on ------------------------------------------------------------------------------

BOUNDS_FILE = os.path.join("tools", "model_bounds.json")
DEFAULT_ANIMS = {"IDLE": "misc.idle", "LIVING": "misc.living", "WALK": "move.walk", "RUN": "move.run",
                 "FLY": "move.fly", "SWIM": "move.swim", "ATTACK_SWING": "attack.swing", "ATTACK_SHOOT": "attack.shoot"}


def java_sources():
    out = {}
    for path in glob.glob(os.path.join("src", "main", "java", "**", "*.java"), recursive=True):
        with open(path, encoding="utf-8") as f:
            out[os.path.basename(path)[:-5]] = f.read()
    return out


def entity_classes(sources):
    """Entity id path -> its class name, from register("id", ... Class::new ...)."""
    out = {}
    for src in sources.values():
        for m in re.finditer(r'register\(\s*"([a-z0-9_]+)"\s*,(.*?);', src, re.S):
            c = re.search(r"([A-Z][A-Za-z0-9]*)::new", m.group(2))
            if c:
                out.setdefault(m.group(1), c.group(1))
    return out


def class_chain(name, sources):
    chain = []
    while name in sources and name not in chain:
        chain.append(name)
        m = re.search(r"class\s+%s\b[^{]*?extends\s+([A-Z][A-Za-z0-9]*)" % name, sources[name])
        name = m.group(1) if m else None
    return chain


def int_constant(src, name):
    m = re.search(r"\b%s\s*=\s*(\d+)\s*;" % re.escape(name), src)
    return int(m.group(1)) if m else None


def rest_bounds(model):
    xs, ys, zs = [], [], []
    for q in model.quads():
        for p in (q.p0, (q.p0[0] + q.ea[0] + q.eb[0], q.p0[1] + q.ea[1] + q.eb[1], q.p0[2] + q.ea[2] + q.eb[2])):
            xs.append(p[0])
            ys.append(p[1])
            zs.append(p[2])
    return [round(max(xs) - min(xs), 2), round(max(ys) - min(ys), 2), round(max(zs) - min(zs), 2)]


def self_check():
    """The tool's own parts, cheaply: the GIF LZW coder round trip, a stone cube through the top-down and the eye
    camera, every structure template read and each of its block states resolved to a model (a mod block without a
    blockstate is a problem). Returns problems."""
    problems = []
    seed, noise = 12345, bytearray()
    for i in range(70000):
        seed = (seed * 1103515245 + 12345) & 0x7FFFFFFF
        noise.append((seed >> 16) & 255 if i % 3 else 7)
    for sample in (bytes(noise) + bytes(9000), b"\x05", bytes(range(256)) * 3):
        if lzw_decode(lzw_encode(sample)) != sample:
            problems.append("self-check: GIF LZW round trip of %d bytes differs" % len(sample))
    cube = Template.__new__(Template)
    cube.blocks = {(0, 0, 0): ("minecraft:stone", {})}
    quads = structure_quads(cube)
    for label, cam, want in (("top", Camera((0.5, 10, 0.5), 180, 90, 41, 41, scale=10), 9.0),
                             ("eye", Camera((0.5, 0.5, -3), 0, 0, 41, 41, fov=70), 3.0),
                             ("eye looking away", Camera((0.5, 0.5, -3), 180, 0, 41, 41, fov=70), math.inf)):
        frame = Frame(41, 41, BACKGROUND)
        draw_scene(frame, quads, cam)
        got = frame.depth[20 * 41 + 20]
        if not (got == want or abs(got - want) < 1e-3):
            problems.append("self-check: cube seen %s at depth %s, expected %s" % (label, got, want))
    known = set(entity_classes(java_sources()))
    states, boxes, enemies, unknown = set(), set(), 0, set()
    names = structure_names()
    for name in names:
        path, _ = structure_file(name)
        tpl = Template(name, path)
        for (x, y, z), state in tpl.blocks.items():
            if not (0 <= x < tpl.size[0] and 0 <= y < tpl.size[1] and 0 <= z < tpl.size[2]):
                problems.append("%s: block at %d,%d,%d outside its size" % (name, x, y, z))
            key = (state[0], tuple(sorted(state[1].items())))
            if key in states:
                continue
            states.add(key)
            ns, block = split_id(state[0])
            models = blockstate_models(*state)
            if models is None and (ns == MOD or vanilla_jar()):
                problems.append("%s: no blockstate for %s" % (name, state[0]))
            elif not models or not any(load_block_model(m, "minecraft")[1] for m, _, _ in models):
                boxes.add(block)
        enemies += len(tpl.markers)
        unknown |= {m.entity for m in tpl.markers if m.entity.startswith(MOD + ":") and split_id(m.entity)[1] not in known}
    print("self-check: %d templates, %d block states (as boxes: %s), %d placed enemies" % (
        len(names), len(states), ", ".join(sorted(boxes)) or "none", enemies))
    for e in sorted(unknown):
        print("note: placed enemy %s is not registered (it spawns nothing until it is)" % e)
    if not vanilla_jar():
        print("note: no Minecraft client jar in ~/.gradle/caches, vanilla blocks drawn as missing")
    return problems


def check(names, write_bounds=False):
    """Fails (exit 1) when a model lacks an animation its entity class plays, a bone the class toggles or the head
    bone, an animation is shorter than its move's windup, or the rest pose drifted from the recorded size."""
    sources = java_sources()
    classes = entity_classes(sources)
    try:
        with open(BOUNDS_FILE) as f:
            recorded = json.load(f)
    except FileNotFoundError:
        recorded = {}
    problems = []
    for name in names:
        model, tex, glow, animations = load_entity(name)
        bones = set(model.by_name)
        cls = classes.get(name)
        chain = class_chain(cls, sources) if cls else []
        src = "\n".join(sources[c] for c in chain)
        wanted = set(re.findall(r'"((?:attack|misc|move)\.[a-z0-9_]+)"', src))
        # Default walk / idle / fly: from the classes up to the first that picks the movement animation.
        for c in chain:
            wanted |= {DEFAULT_ANIMS[k] for k in re.findall(r"DefaultAnimations\.([A-Z_]+)", sources[c])
                       if k in DEFAULT_ANIMS}
            if "movementAnimation(" in sources[c]:
                break
        for a in sorted(wanted - set(animations)):
            problems.append("%s: animation %s is played by %s but missing" % (name, a, cls))
        for m in re.finditer(r'new Move\("([a-z0-9_.]+)",\s*([A-Z0-9_]+)', src):
            ticks = int(m.group(2)) if m.group(2).isdigit() else int_constant(src, m.group(2))
            a = animations.get(m.group(1))
            if a and ticks is not None and float(a.get("animation_length", 0)) + 1e-6 < ticks / 20:
                problems.append("%s: %s is %.2fs, shorter than its %d-tick windup" % (name, m.group(1),
                                                                                    a["animation_length"], ticks))
        body = re.search(r"toggledBones\(\)\s*\{(.*?)\n\t\}", src, re.S)
        toggled = set()
        if body:
            toggled |= set(re.findall(r'"([a-z0-9_]+)"', body.group(1)))
            for const in re.findall(r"\b([A-Z][A-Z0-9_]+)\b", body.group(1)):
                d = re.search(r"\b%s\s*=\s*List\.of\(([^)]*)\)" % const, src)
                if d:
                    toggled |= set(re.findall(r'"([a-z0-9_]+)"', d.group(1)))
        for b in sorted(toggled - bones):
            problems.append("%s: bone %s is toggled by %s but missing" % (name, b, cls))
        if "head" not in bones:
            print("note: %s has no head bone (it will not turn its head)" % name)
        for an, a in animations.items():
            for b in a.get("bones", {}):
                if b not in bones:
                    problems.append("%s: %s animates missing bone %s" % (name, an, b))
        size = rest_bounds(model)
        if write_bounds:
            recorded[name] = size
        elif name in recorded:
            ref = recorded[name]
            for axis, label, tol in ((1, "height", 0.12), (0, "width", 0.35), (2, "depth", 0.35)):
                if abs(size[axis] - ref[axis]) > max(2.0, ref[axis] * tol):
                    problems.append("%s: rest %s %.1f px, recorded %.1f px" % (name, label, size[axis], ref[axis]))
        print("%-20s %-24s anims %2d  bones %2d  size %s" % (name, cls, len(animations), len(bones), size))
    if write_bounds:
        with open(BOUNDS_FILE, "w") as f:
            json.dump(recorded, f, indent=1, sort_keys=True)
            f.write("\n")
        print("recorded", BOUNDS_FILE)
    for p in problems:
        print("PROBLEM", p)
    return not problems


# --- CLI -----------------------------------------------------------------------------------------------------------

def entity_names():
    return sorted(os.path.basename(p)[:-9] for p in glob.glob(os.path.join(ASSETS, MOD, "geo", "entity", "*.geo.json")))


def main(argv):
    out, hidden, anims, rest = DEFAULT_OUT, (), "all", []
    views, eye, look, pitch, ground = ("top", "iso", "eye"), None, None, 0.0, "auto"
    fps, yaw, gif, gif_fps = 10, 35, True, 20
    i = 0
    while i < len(argv):
        a = argv[i]
        if a == "--out":
            out, i = argv[i + 1], i + 2
        elif a == "--hide":
            hidden, i = tuple(argv[i + 1].split(",")), i + 2
        elif a == "--anims":
            anims, i = argv[i + 1], i + 2
        elif a == "--no-anims":
            anims, i = "", i + 1
        elif a == "--view":
            views, i = ("top", "iso", "eye") if argv[i + 1] == "all" else tuple(argv[i + 1].split(",")), i + 2
        elif a == "--eye":
            eye, i = tuple(float(c) for c in argv[i + 1].split(",")), i + 2
        elif a == "--look":
            sides = {"S": 0.0, "W": 90.0, "N": 180.0, "E": 270.0}
            v = argv[i + 1].upper()
            look, i = sides[v] if v in sides else float(v), i + 2
        elif a == "--pitch":
            pitch, i = float(argv[i + 1]), i + 2
        elif a == "--ground":
            ground, i = argv[i + 1], i + 2
        elif a == "--fps":
            fps, i = int(argv[i + 1]), i + 2
        elif a == "--gif-fps":
            gif_fps, i = int(argv[i + 1]), i + 2
        elif a == "--yaw":
            yaw, i = float(argv[i + 1]), i + 2
        elif a == "--no-gif":
            gif, i = False, i + 1
        else:
            rest.append(a)
            i += 1
    if not rest:
        print(__doc__)
        return
    cmd, names = rest[0], rest[1:]
    if cmd == "entity":
        for n in names:
            entity_sheet(n, out, hidden, anims)
    elif cmd == "entities":
        for n in names or entity_names():
            entity_sheet(n, out, hidden, anims)
    elif cmd == "motion":
        for n in names:
            motion_sheet(n, out, hidden, anims, fps, yaw, gif, gif_fps)
    elif cmd == "player":
        player_motion_sheet(names or player_clip_names(), out, gif)
    elif cmd == "structure":
        for n in names:
            structure_sheets(n, out, views, eye, look, pitch, ground)
    elif cmd == "structures":
        for n in structure_names():
            if not names or any(n.startswith(p) for p in names):
                structure_sheets(n, out, views, None, None, 0.0, ground)
    elif cmd == "self-check":
        problems = self_check()
        for p in problems:
            print("PROBLEM", p)
        if problems:
            sys.exit(1)
    elif cmd in ("check", "record-bounds"):
        problems = self_check() if cmd == "check" and not names else []
        for p in problems:
            print("PROBLEM", p)
        if not check(names or entity_names(), cmd == "record-bounds") or problems:
            sys.exit(1)
    elif cmd == "items":
        names = names or mod_item_names()
        item_sheet(names, out, "items" if len(names) > 1 else "item_" + names[0])
    elif cmd == "blocks":
        names = names or mod_block_names()
        for k in range(0, len(names), 12):
            block_sheet(names[k:k + 12], out, "blocks" if len(names) <= 12 else "blocks_%d" % (k // 12 + 1))
    elif cmd == "armor":
        armor_sheet(names or armor_materials(), out)
    elif cmd == "gui":
        for n in names or [os.path.basename(p)[:-4] for p in glob.glob(os.path.join(ASSETS, MOD, "textures", "gui", "*.png"))]:
            t = texture(MOD, "gui/" + n)
            upscale(t, max(1, min(8, 1024 // max(t.width, t.height)))).save(os.path.join(out, "gui_%s.png" % n))
    elif cmd == "texture":
        for p in names:
            t = texture_file(p)
            upscale(t, max(1, min(16, 1024 // max(t.width, t.height)))).save(
                os.path.join(out, "tex_" + os.path.basename(p)))
    else:
        print(__doc__)


if __name__ == "__main__":
    main(sys.argv[1:])
