#!/usr/bin/env python3
"""The shared palette of the 0.5.0 visual overhaul (docs/art-direction.md §1.1) and the drawing helpers every block,
prop and item script builds on. Pure Python like art_kit.py; not run directly except for its self-test:

    python3 tools/palette.py          checks every ramp's hue shift and value span

Ramps run dark to light, 7 colours. Shadows go cooler and more saturated (stone, iron and slate towards blue-violet;
wood, cloth and fire towards red-brown), highlights go warmer and less saturated (towards yellow-white). Index 0 is
the deepest recess (mortar, a crack), 6 the lit chip; the mid tones 2-4 carry a face.

Helpers draw in tile space on an art_kit.Canvas and wrap at the canvas edge, so every generator tiles:
  brick_rows      courses of bricks with variable widths, a bevel per brick, seeded chips and a crack over two bricks
  wood_grain      planks with grain streaks, a joint per plank, knots
  cellular        Worley-like lumps of stone with lit and shaded edges
  dither_fill     a level function quantised with an ordered 2x2 dither, so a 2-step gradient reads as a gradient
  bevel_edges     a 1 px light edge top-left and dark edge bottom-right around a rectangle
  outline         darkens the outer ring of a canvas or rectangle
  tile_check      the seam contrast of a tile against its own interior, plus value span and level count
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from art_kit import Canvas, clamp, hash01, hexc, ramp, shade, value_noise  # noqa: E402

# --- Named ramps --------------------------------------------------------------------------------------------------

# Sculpted dark stone of the kingdom: violet-blue shadows, warm grey light.
ASH_STONE = ramp("1d1b24", "2d2a34", "403c47", "544f59", "6a6469", "807a76", "978f85")
# Fired ash brick: a touch browner than the stone, the same mortar.
ASH_BRICK = ramp("221c20", "342c2f", "473d3f", "5a4f4f", "6e625e", "83766d", "988a7c")
ASH_MORTAR = hexc("15131a")
# Weathered grey wood, bleached silver on top, red-brown in the grain.
DEAD_WOOD = ramp("3b2f28", "4f4237", "63564a", "786c5e", "8d8272", "a2977f", "b5ab92")
# Wood that went through the fire: soot black, scorched brown where the char flaked.
CHARRED_WOOD = ramp("100a0a", "1b1312", "271c18", "362a22", "473a31", "5c4e42", "78695a")
# Grave iron: cold blue-black, a specular line on the lit edge.
GRAVE_IRON = ramp("15151b", "202027", "2c2c34", "3a3a42", "4a4a50", "5c5c5f", "70706d")
GRAVE_IRON_SPECULAR = hexc("8e8e86")
# Gilding of the old kingdom.
GOLD_TRIM = ramp("4a3010", "6c4a16", "8e661f", "b0862c", "cea544", "e6c266", "f6dc93")
BONE = ramp("5c5244", "776c5c", "918572", "aa9f88", "c2b89f", "d6ceb5", "e6e0cb")
# Fire: deep red in the shadow, yellow-white at the core.
EMBER = ramp("3b0d07", "6e1a09", "a83512", "d85b1a", "f0862a", "ffb347", "ffe08c")
AMBER = ramp("562a0a", "83460f", "ad6a17", "d08c22", "e9ab34", "f6c653", "fde18a")
MOSS = ramp("1f2b19", "2f4025", "405636", "526c45", "688356", "829b6a", "9db37f")
CRIMSON_CLOTH = ramp("2a0d0f", "421418", "5b1c1d", "752824", "8f3730", "a84a3c", "bd604a")
ASH_CLOTH = ramp("262226", "363135", "474145", "585154", "6a6262", "7d7572", "908883")
# The Necropolis' blue: candle light on wet stone, the fog over the sinkholes.
GRAVE_BLUE = ramp("171e2b", "232f44", "32455e", "455c78", "5c7592", "7a92aa", "9fb2c4")
BRASS = ramp("3a2a0e", "583f15", "76561e", "946e2a", "b08a3a", "c8a552", "dcbe72")
SLATE = ramp("181b22", "232730", "2f343e", "3d424d", "4c525d", "5d636d", "70767e")
SOOT = ramp("0b090c", "141112", "1c1817", "25201e", "2e2926", "37322e", "403b36")
STRAW = ramp("2c1e11", "402e19", "554024", "6b5431", "816a40", "95804f", "a8945f")
TAR = ramp("09090b", "111013", "191820", "221f28", "2c2832", "393440", "46404d")
COLD_FLAME = ramp("142a44", "1f4c70", "2f7a9a", "4faab8", "86d6d6", "c4f2ee", "f0fdfb")
EMBER_GLASS = ramp("3d140a", "7a2b10", "b24c17", "dd7524", "f29d3d", "fbc466", "ffe59c")
# Crypt stone, always damp: blue-grey, darker and bluer where it drips.
WET_STONE = ramp("171d23", "222a32", "2e3841", "3c4751", "4a5763", "5a6873", "6d7b85")
ASH_SOIL = ramp("201c1c", "2f2a28", "3e3835", "4e4742", "5f5852", "726a62", "887f74")
RUST = ramp("3a1f12", "56301b", "724326", "8c5731", "a46b3c", "b8824b", "c89a5e")
# Sun-faded sandstone of the Kingsroad, the brightest line on the ground.
ROAD_STONE = ramp("5a4433", "6d5440", "80654d", "93775a", "a68a68", "b89d78", "c8af8a")
KERB_STONE = ramp("5e5a58", "726e68", "87817a", "9c958b", "b0a89b", "c2baaa", "d2cbbb")
SANDSTONE_JOINT = hexc("2c2119")
# Cleft Stone of the steles: dressed ashlar of a cool violet-grey, the fissures the darkest colour.
CLEFT_STONE = ramp("262330", "34313d", "43404c", "53505c", "64606a", "77717a", "8c8589")
# Sackcloth gone grey under the ash; blade steel; tallow candles.
BURLAP = ramp("3e3324", "524433", "665642", "7a6a52", "8e7d62", "a09074", "b2a388")
STEEL = ramp("34363e", "4a4d56", "60646d", "787c85", "92969e", "aeb1b7", "cbcdd0")
TALLOW = ramp("7a6a46", "95845b", "ab9b6e", "bfaf80", "d0c293", "ded3a8", "ebe3bf")
CLAY = ramp("3a2217", "50301f", "653f29", "7a4e33", "905e3e", "a46f4a", "b68359")

ALL_RAMPS = {name: value for name, value in list(globals().items())
             if isinstance(value, list) and value and isinstance(value[0], tuple) and name.isupper()}


def luminance(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def hsv(c):
    r, g, b = [v / 255.0 for v in c[:3]]
    mx, mn = max(r, g, b), min(r, g, b)
    d = mx - mn
    if d == 0:
        h = 0.0
    elif mx == r:
        h = (60 * ((g - b) / d) + 360) % 360
    elif mx == g:
        h = 60 * ((b - r) / d) + 120
    else:
        h = 60 * ((r - g) / d) + 240
    return h, (d / mx if mx else 0.0), mx


def yellow_distance(h):
    """Degrees of hue away from yellow (60), the direction every highlight shifts towards."""
    return min(abs(h - 60), 360 - abs(h - 60))


def check_ramps():
    """Every ramp gets lighter at each step, spans at least 35 % of 0-255 in luminance, and its light end is warmer
    (yellower) relative to its brightness than its dark end. Returns the problems."""
    problems = []
    for name, colors in ALL_RAMPS.items():
        lum = [luminance(c) for c in colors]
        if any(b <= a for a, b in zip(lum, lum[1:])):
            problems.append("%s: not monotonic in value" % name)
        if lum[-1] - lum[0] < 0.35 * 255 and name not in ("SOOT", "TAR"):
            problems.append("%s: value span %.0f < %.0f" % (name, lum[-1] - lum[0], 0.35 * 255))
        # Hue shift: the light end sits nearer to yellow than the dark end (a red-brown shadow turns yellow-white, a
        # blue-violet shadow turns warm grey), and the dark end is at least as saturated as the light end.
        h0, s0, _ = hsv(colors[0])
        h1, s1, _ = hsv(colors[-1])
        if s0 > 0.08 or s1 > 0.08:
            if yellow_distance(h1) > yellow_distance(h0) + 2 and name != "COLD_FLAME":
                problems.append("%s: light end further from yellow than the dark end (%.0f -> %.0f deg)" % (name, h0, h1))
            if s1 > s0 + 0.05:
                problems.append("%s: light end more saturated than the dark end (%.2f -> %.2f)" % (name, s0, s1))
    return problems


# --- Levels and quantisation --------------------------------------------------------------------------------------

def level(colors, v):
    """A ramp colour for a level in 0..1 (clamped)."""
    return colors[max(0, min(len(colors) - 1, int(v * len(colors))))]


def step(colors, i):
    """A ramp colour by index, clamped."""
    return colors[max(0, min(len(colors) - 1, int(round(i))))]


BAYER = ((0, 2), (3, 1))


def dither_level(colors, v, x, y):
    """Quantises v (in ramp steps, float) with an ordered 2x2 dither: a level between two steps becomes a checker
    mix of both, so a gradient over two steps reads as a gradient and not as a hard band."""
    i = math.floor(v)
    frac = v - i
    threshold = (BAYER[y % 2][x % 2] + 0.5) / 4
    return step(colors, i + (1 if frac > threshold else 0))


def dither_fill(c, colors, fn, x0=0, y0=0, w=None, h=None):
    """Fills a rectangle from fn(x, y) -> level in ramp steps (float, may be None to leave the pixel alone)."""
    w = c.width if w is None else w
    h = c.height if h is None else h
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            v = fn(x, y)
            if v is not None:
                c.set(x, y, dither_level(colors, v, x, y))


def fill(c, color, x0=0, y0=0, w=None, h=None):
    w = c.width if w is None else w
    h = c.height if h is None else h
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            c.set(x, y, color)


def tint(c, x, y, d):
    """Shifts one pixel lighter (d > 0) or darker, keeping its alpha."""
    p = c.get(x % c.width, y % c.height)
    if p[3]:
        c.set(x % c.width, y % c.height, shade(p, d))


def ramp_index(colors, color):
    """The index of the ramp colour nearest to color."""
    best, best_d = 0, 1e9
    for i, k in enumerate(colors):
        d = sum((a - b) ** 2 for a, b in zip(k[:3], color[:3]))
        if d < best_d:
            best, best_d = i, d
    return best


def shift(c, colors, x, y, d):
    """Moves one pixel d ramp steps along the ramp (quantised, so bevels stay in the palette)."""
    p = c.get(x % c.width, y % c.height)
    if p[3]:
        c.set(x % c.width, y % c.height, step(colors, ramp_index(colors, p) + d))


# --- Edges ------------------------------------------------------------------------------------------------------

def bevel_edges(c, x0, y0, w, h, light=(-1, -1), colors=None, strength=1, wrap=True):
    """A raised rectangle: its top and left edge (towards the light, default top-left) one ramp step lighter, its
    bottom and right edge one step darker; the lit corner two steps lighter. With colors the shift stays on the
    ramp, without it the pixels are tinted. Coordinates may run past the canvas and wrap."""
    lx, ly = light
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            top = (y == y0) if ly < 0 else (y == y0 + h - 1)
            left = (x == x0) if lx < 0 else (x == x0 + w - 1)
            bottom = (y == y0 + h - 1) if ly < 0 else (y == y0)
            right = (x == x0 + w - 1) if lx < 0 else (x == x0)
            d = 0
            if top or left:
                d = strength * (2 if top and left and w > 2 and h > 2 else 1)
            elif bottom or right:
                d = -strength
            if d:
                if colors:
                    shift(c, colors, x, y, d)
                else:
                    tint(c, x, y, d * 18)


def outline(c, k, x0=0, y0=0, w=None, h=None, colors=None):
    """Darkens (k < 0, steps or tint) the outermost pixel ring of a rectangle; the whole canvas by default."""
    w = c.width if w is None else w
    h = c.height if h is None else h
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if x in (x0, x0 + w - 1) or y in (y0, y0 + h - 1):
                if colors:
                    shift(c, colors, x, y, k)
                else:
                    tint(c, x, y, k * 18)


# --- Bricks -----------------------------------------------------------------------------------------------------

def brick_rows(c, colors, mortar, seed, rows, base=3.0, spread=1.6, noise=0.5, chips=3, crack=True, light=(-1, -1),
               width=None):
    """Courses of bricks over the whole canvas width (which wraps): rows is a list of (height, [widths]); each
    course's widths sum to the canvas width (or `width`) and its last pixel column and row are mortar. Every brick
    gets its own tone (base +- spread/2 steps, seeded), gentle value noise inside, a bevel lit from `light`, and the
    tile as a whole gets `chips` knocked corners and, with crack, one crack running down across two bricks.
    Returns the list of bricks as (x, y, w, h, tone) for decals."""
    width = c.width if width is None else width
    bricks = []
    y = 0
    for r, (height, widths) in enumerate(rows):
        assert sum(widths) == width, "course %d of %s sums to %d, not %d" % (r, rows, sum(widths), width)
        x = 0
        for s, w in enumerate(widths):
            tone = base + (hash01(r, s, seed) - 0.5) * spread
            for dy in range(height):
                for dx in range(w):
                    px, py = (x + dx) % c.width, (y + dy) % c.height
                    if dx == w - 1 or dy == height - 1:
                        c.set(px, py, mortar)
                    else:
                        v = tone + (value_noise(px, py, seed + r, 2.6) - 0.5) * 2 * noise
                        c.set(px, py, dither_level(colors, v, px, py))
            bevel_edges(c, x, y, w - 1, height - 1, light, colors)
            bricks.append((x, y, w - 1, height - 1, tone))
            x += w
        y += height
    # Chips: a corner of a brick knocked off to the mortar colour, with a lit edge beside it.
    for k in range(chips):
        bx, by, bw, bh, _ = bricks[int(hash01(k, 1, seed + 31) * len(bricks)) % len(bricks)]
        corner = int(hash01(k, 2, seed + 31) * 4)
        cx = bx if corner in (0, 2) else bx + bw - 1
        cy = by if corner in (0, 1) else by + bh - 1
        dx = 1 if corner in (0, 2) else -1
        dy = 1 if corner in (0, 1) else -1
        c.set(cx % c.width, cy % c.height, mortar)
        c.set((cx + dx) % c.width, cy % c.height, mortar)
        c.set(cx % c.width, (cy + dy) % c.height, mortar)
        shift(c, colors, cx + dx, cy + dy, 1)
    if crack and len(bricks) > 1:
        bx, by, bw, bh, _ = bricks[int(hash01(7, 7, seed + 31) * len(bricks)) % len(bricks)]
        x = bx + 1 + int(hash01(8, 8, seed + 31) * max(1, bw - 2))
        y = by
        for i in range(bh + 2 + int(hash01(9, 9, seed + 31) * 4)):
            c.set(x % c.width, (y + i) % c.height, mortar)
            shift(c, colors, x + 1, y + i, 1)
            x += (1 if hash01(x, y + i, seed + 32) < 0.3 else (-1 if hash01(x, y + i, seed + 33) < 0.3 else 0))
    return bricks


def course_widths(seed, width=16, choices=(3, 4, 5, 6, 7, 8, 9)):
    """A row of brick widths adding up to width, seeded; never two courses the same for consecutive seeds."""
    out = []
    left = width
    k = 0
    while left > 0:
        w = choices[int(hash01(k, left, seed) * len(choices))]
        if w >= left - 2 and w != left:
            w = left if left <= max(choices) else min(choices)
        if w > left:
            w = left
        out.append(w)
        left -= w
        k += 1
    return out


# --- Wood ---------------------------------------------------------------------------------------------------------

def wood_grain(c, colors, seed, plank=4, vertical=False, base=3.0, spread=1.0, knots=1, joint=None, streaks=0.55,
               bevel=True, x0=0, y0=0, w=None, h=None):
    """Planks across a rectangle: along the grain (x unless vertical) each plank is `plank` pixels wide and gets its
    own tone, 1-2 darker grain streaks that wander, a lit edge and a dark edge; the plank joints (the last row of
    each plank) are the joint colour (ramp index 0 by default). Knots: concentric rings around an off-centre point
    inside a plank, with the grain bending around them."""
    w = c.width if w is None else w
    h = c.height if h is None else h
    joint = colors[0] if joint is None else joint
    along = w if not vertical else h
    across = h if not vertical else w
    knot_at = []
    for k in range(knots):
        ka = int(hash01(k, 1, seed + 41) * along)
        kp = int(hash01(k, 2, seed + 41) * (across // plank))
        kb = kp * plank + 1 + int(hash01(k, 3, seed + 41) * max(1, plank - 3))
        knot_at.append((ka, kb))
    for b in range(across):
        p = b // plank
        kk = b % plank
        tone = base + (hash01(p, 0, seed) - 0.5) * spread
        for a in range(along):
            x, y = (a, b) if not vertical else (b, a)
            px, py = (x0 + x) % c.width, (y0 + y) % c.height
            if kk == plank - 1:
                c.set(px, py, joint)
                continue
            # Grain: noise stretched along the plank, bent around the nearest knot.
            aa, bb = a, b
            for ka, kb in knot_at:
                da = (a - ka + along / 2) % along - along / 2
                db = b - kb
                r = math.hypot(da, db)
                if r < 4.5 and r > 0:
                    bb = b + db / r * (4.5 - r) * 0.6
            n = value_noise(aa * 0.45, bb * 3.1 + p * 7, seed + p, 1.0)
            v = tone + (value_noise(aa, bb, seed + 5, 3.0) - 0.5) * 0.6
            if n > 0.5 + streaks * 0.5:
                v -= 1.2
            elif n < 0.5 - streaks * 0.5:
                v += 0.6
            if bevel and plank > 2:
                if kk == 0:
                    v += 0.9
                elif kk == plank - 2:
                    v -= 0.9
            c.set(px, py, dither_level(colors, v, px, py))
    # Knots on top: a dark eye, a ring one step lighter, a dark ring.
    for ka, kb in knot_at:
        for db in range(-2, 3):
            for da in range(-3, 4):
                r = math.hypot(da / 1.4, db)
                if r > 2.2:
                    continue
                a, b = (ka + da) % along, kb + db
                if b < 0 or b >= across or (b % plank) == plank - 1 or b // plank != kb // plank and r > 1.2:
                    continue
                x, y = (a, b) if not vertical else (b, a)
                d = -2 if r < 0.8 else (1 if r < 1.5 else -1)
                shift(c, colors, x0 + x, y0 + y, d)


def end_grain(c, colors, seed, cx=7.5, cy=7.5, base=3.0, rings=1.6, bark=None, bark_width=1.5):
    """A log's end: growth rings around (cx, cy) with an off-centre pith, a bark ring at the edge."""
    for y in range(c.height):
        for x in range(c.width):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx * 1.05, dy) + (value_noise(x, y, seed, 2.5) - 0.5) * 1.2
            edge = min(x, y, c.width - 1 - x, c.height - 1 - y)
            if bark is not None and edge < bark_width:
                v = 2.5 + (hash01(x, y, seed + 3) - 0.5) * 2
                c.set(x, y, dither_level(bark, v, x, y))
                continue
            ring = (r / rings) % 1.0
            v = base + (0.9 if ring < 0.45 else -0.7) + (hash01(x, y, seed + 1) - 0.5) * 0.5
            if r < 1.1:
                v = base - 1.8
            c.set(x, y, dither_level(colors, v, x, y))


# --- Stone ------------------------------------------------------------------------------------------------------

def cellular(c, colors, seed, points=9, base=3.2, spread=1.4, edge=None, light=(-1, -1), jitter=0.9, noise=0.4,
             x0=0, y0=0, w=None, h=None):
    """Worley-like stones: `points` seed points on a jittered grid (wrapping), every pixel belongs to the nearest,
    each stone has its own tone; the boundary between stones is the edge colour (ramp index 0 by default), and the
    stone pixel next to a boundary gets a lit edge on the light side and a shaded edge on the other."""
    w = c.width if w is None else w
    h = c.height if h is None else h
    edge = colors[0] if edge is None else edge
    n = max(1, int(round(math.sqrt(points))))
    cells = []
    for j in range(n):
        for i in range(n):
            cells.append(((i + 0.5 + (hash01(i, j, seed) - 0.5) * jitter) * w / n,
                          (j + 0.5 + (hash01(i, j, seed + 1) - 0.5) * jitter) * h / n))

    def nearest(x, y):
        best, bi = 1e9, 0
        for k, (cx, cy) in enumerate(cells):
            dx = (x - cx + w / 2) % w - w / 2
            dy = (y - cy + h / 2) % h - h / 2
            d = dx * dx + dy * dy + (value_noise(x + k * 3, y, seed + 9, 2.0) - 0.5) * 3
            if d < best:
                best, bi = d, k
        return bi

    owner = [[nearest(x + 0.5, y + 0.5) for x in range(w)] for y in range(h)]
    lx, ly = light
    for y in range(h):
        for x in range(w):
            k = owner[y][x]
            right = owner[y][(x + 1) % w]
            down = owner[(y + 1) % h][x]
            px, py = (x0 + x) % c.width, (y0 + y) % c.height
            if right != k or down != k:
                c.set(px, py, edge)
                continue
            tone = base + (hash01(k, 3, seed + 2) - 0.5) * spread
            v = tone + (value_noise(x, y, seed + 4, 2.2) - 0.5) * 2 * noise
            # The side of the stone towards the light is lit, the other side shaded.
            lit = owner[(y + ly) % h][x] != k or owner[y][(x + lx) % w] != k
            shaded = owner[(y - ly) % h][x] != k or owner[y][(x - lx) % w] != k
            if lit:
                v += 1.0
            elif shaded:
                v -= 0.8
            c.set(px, py, dither_level(colors, v, px, py))
    return owner


def rubble(c, colors, seed, x0=0, y0=0, w=None, h=None, base=2.8):
    """Loose small stones: like cellular with many points and no lit edge, for cobble and gravel."""
    cellular(c, colors, seed, points=20, base=base, spread=1.8, light=(-1, -1), jitter=1.0, noise=0.5,
             x0=x0, y0=y0, w=w, h=h)


# --- Decals -----------------------------------------------------------------------------------------------------

def crack_line(c, seed, x, y, length, color, lit=None, colors=None, wander=0.35):
    """A jagged crack going mostly down from (x, y); the pixel to its right one step lighter when colors is given."""
    for i in range(length):
        c.set(x % c.width, (y + i) % c.height, color)
        if colors:
            shift(c, colors, x + 1, y + i, 1)
        elif lit:
            c.set((x + 1) % c.width, (y + i) % c.height, lit)
        r = hash01(x, y + i, seed)
        x += 1 if r < wander / 2 else (-1 if r < wander else 0)


def moss_patch(c, seed, x, y, radius, colors=MOSS, density=0.75, base=2.5):
    """Moss growing from (x, y): a blob with ragged edges, lighter at its upper-left."""
    for dy in range(-radius, radius + 1):
        for dx in range(-radius, radius + 1):
            r = math.hypot(dx, dy) / radius + (hash01(x + dx, y + dy, seed) - 0.5) * 0.7
            if r < density:
                v = base + (0.8 if dx + dy < -radius * 0.4 else 0) - (0.8 if dx + dy > radius * 0.5 else 0)
                c.set((x + dx) % c.width, (y + dy) % c.height, dither_level(colors, v, x + dx, y + dy))


def soot_stain(c, seed, rows, strength=2, colors=None):
    """Darkens towards the rows given ((first, last), soot creeping from a fire below or a doorway above): the
    nearer the last row, the more pixels drop by `strength` steps (quantised when colors is given)."""
    first, last = rows
    span = max(1, last - first)
    for y in range(min(first, last), max(first, last) + 1):
        t = abs(y - first) / span
        for x in range(c.width):
            if hash01(x, y, seed) < 0.2 + 0.7 * t:
                if colors:
                    shift(c, colors, x, y, -strength)
                else:
                    tint(c, x, y, -strength * 16)


def rivet(c, x, y, colors, specular=None):
    """A round-headed rivet: lit top-left pixel, dark bottom-right."""
    shift(c, colors, x, y, 1)
    shift(c, colors, x + 1, y + 1, -2)
    shift(c, colors, x + 1, y, -1)
    shift(c, colors, x, y + 1, -1)
    if specular:
        c.set(x % c.width, y % c.height, specular)


# --- Checks -----------------------------------------------------------------------------------------------------

def tile_check(c, accent=None):
    """Reports how a tile tiles and how much it says: seam = mean absolute luminance step across the wrap-around
    seams divided by the mean step between interior neighbours (near 1 tiles cleanly, well above 1 shows a seam);
    span = max - min luminance of opaque pixels; levels = distinct colours; accent = share of pixels whose colour
    is in the accent list (or None). Returns a dict."""
    w, h = c.width, c.height
    lum = [[luminance(c.get(x, y)) if c.get(x, y)[3] else None for x in range(w)] for y in range(h)]

    def diff(a, b):
        return None if a is None or b is None else abs(a - b)

    interior, seam = [], []
    for y in range(h):
        for x in range(w):
            d1 = diff(lum[y][x], lum[y][(x + 1) % w])
            d2 = diff(lum[y][x], lum[(y + 1) % h][x])
            for d, is_seam in ((d1, x == w - 1), (d2, y == h - 1)):
                if d is None:
                    continue
                (seam if is_seam else interior).append(d)
    mean_in = sum(interior) / max(1, len(interior))
    mean_seam = sum(seam) / max(1, len(seam))
    values = [v for row in lum for v in row if v is not None]
    colors = {c.get(x, y) for y in range(h) for x in range(w) if c.get(x, y)[3]}
    out = {"seam": mean_seam / mean_in if mean_in else 0.0,
           "span": (max(values) - min(values)) if values else 0,
           "levels": len(colors)}
    if accent is not None:
        acc = set(accent)
        out["accent"] = sum(1 for y in range(h) for x in range(w) if c.get(x, y) in acc) / float(w * h)
    return out


def main():
    problems = check_ramps()
    for p in problems:
        print("palette:", p)
    print("%d ramps, %s" % (len(ALL_RAMPS), "all pass" if not problems else "%d problems" % len(problems)))
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
