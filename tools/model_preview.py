#!/usr/bin/env python3
"""Renders Embervoid models and textures to PNG contact sheets without a Minecraft client (PLAN R40.2a).

Run from the repository root. Pure Python (no numpy / PIL): a small software rasteriser with a z-buffer.

    python3 tools/model_preview.py entity cinder_husk [more names] [--out DIR] [--anims all|a,b] [--hide a,b]
    python3 tools/model_preview.py entities             every model under geo/entity
    python3 tools/model_preview.py items [names]        item textures (8x), next to vanilla references
    python3 tools/model_preview.py blocks [names]       block models (3D, from their JSON) + 3x3 tiling
    python3 tools/model_preview.py armor [materials]    armor layers on a mannequin
    python3 tools/model_preview.py gui [names]          GUI textures (4x)
    python3 tools/model_preview.py texture PATH...      any PNG, upscaled

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

Vanilla parents and textures (block models, the armor model, reference items) come from Loom's local copy of the
1.20.1 client jar in ~/.gradle/caches; nothing is downloaded.
"""
import glob
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
        """An animated (mcmeta) texture is a vertical strip of square frames."""
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
        _TEXTURES[key] = read_png(data).first_frame() if data else None
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
                    if z >= depth[i]:
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
    for name, (u, v, du, dv) in face_uvs(cube).items():
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


def channel_value(channel, t):
    """A bone channel (rotation / position / scale) at time t: constant, or keyframes interpolated linearly
    (catmullrom drawn as linear)."""
    if isinstance(channel, (list, int, float, str)):
        vec = channel if isinstance(channel, list) else [channel] * 3
        return tuple(molang(c, t) for c in vec)
    frames = []
    for key, value in channel.items():
        try:
            time = float(key)
        except ValueError:
            continue
        if isinstance(value, dict):
            pre = value.get("pre", value.get("post", value.get("vector")))
            post = value.get("post", value.get("pre", value.get("vector")))
        else:
            pre = post = value
        if isinstance(pre, dict):
            pre = pre.get("vector")
        if isinstance(post, dict):
            post = post.get("vector")
        frames.append((time, pre, post))
    if not frames:
        return None
    frames.sort(key=lambda f: f[0])

    def vec(v):
        v = v if isinstance(v, list) else [v] * 3
        return tuple(molang(c, t) for c in v)

    if t <= frames[0][0]:
        return vec(frames[0][1] if t < frames[0][0] else frames[0][2])
    for (ta, _, pa), (tb, qb, _) in zip(frames, frames[1:]):
        if ta <= t <= tb:
            if tb == ta:
                return vec(qb)
            k = (t - ta) / (tb - ta)
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
    geo_data = resource(MOD, "geo/entity/%s.geo.json" % name) or resource(MOD, "geo/%s.geo.json" % name)
    if geo_data is None:
        raise SystemExit("no model geo/entity/%s.geo.json" % name)
    model = GeoModel(json.loads(geo_data))
    tex = prepare(texture(MOD, "entity/" + name), model.tex_w, model.tex_h)
    glow = prepare(texture(MOD, "entity/%s_glowmask" % name), model.tex_w, model.tex_h)
    anim_data = resource(MOD, "animations/entity/%s.animation.json" % name)
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


def element_quads(elements, textures):
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
            out.append(Quad(apply(m, p), apply_dir(m, ea), apply_dir(m, eb), uv0, ua, ub, tex))
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


# --- CLI -----------------------------------------------------------------------------------------------------------

def entity_names():
    return sorted(os.path.basename(p)[:-9] for p in glob.glob(os.path.join(ASSETS, MOD, "geo", "entity", "*.geo.json")))


def main(argv):
    out, hidden, anims, rest = DEFAULT_OUT, (), "all", []
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
