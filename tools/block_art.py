#!/usr/bin/env python3
"""Every block texture of the mod, drawn to docs/art-direction.md §1 from the shared ramps in tools/palette.py
(track V-A of the 0.5.0 visual overhaul). Replaces reach_world_placeholders.py, road_art.py and the block part of
stele_art.py (which keeps the Crying Soul item).

Run from the repository root: python3 tools/block_art.py [names...]
Preview: python3 tools/model_preview.py blocks            (every block, 3D and the 3x3 tiling)
         python3 tools/model_preview.py texture src/main/resources/assets/embervoid/textures/block/<name>.png

The rules every texture here follows: one ramp per material with a real hue shift, light from the top-left (a 1 px
lit edge on top and left of anything raised, a dark edge bottom and right, recesses darkest at their top-left), at
least four ramp steps on a face and a value span of 35 % or more, variation that is structural (bricks of their own
tone, a chipped corner, a crack over two bricks, grain and knots, a drip) instead of per-pixel noise, accents (ember,
moss, amber, gold) on a few percent of the pixels, and seamless tiling with no obvious repeat in a 3x3. The script
prints a report per texture (seam ratio, value span, colour count, accent share) and flags what misses the bar.

Embers appear only where the art direction allows: Ember Crack, the Smoldering Log, the Citadel's charred log, ember
glass and shrine, and the ores' inclusions. Ash stone is sculpted dark stone with no sprinkles.
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from art_kit import ROOT, CLEAR, Canvas, hash01, hexc, mix, shade, value_noise  # noqa: E402
import palette as P  # noqa: E402
from palette import (bevel_edges, brick_rows, cellular, course_widths, crack_line, dither_fill, dither_level,  # noqa: E402
                     end_grain, fill, moss_patch, outline, rivet, rubble, shift, soot_stain, step, tile_check,
                     tint, wood_grain)

BLOCK = os.path.join(ROOT, "textures", "block")

REGISTRY = {}


def texture(name, accent=None, animated=None):
    """Registers a generator: fn() -> Canvas. accent: ramp colours counted as accent in the report; animated:
    frametime (and interpolate) for an .mcmeta beside the sheet."""
    def deco(fn):
        REGISTRY[name] = (fn, accent, animated)
        return fn
    return deco


# --- Shared pieces ------------------------------------------------------------------------------------------------

def plate(c, colors, x0, y0, w, h, seed, base=3.6, noise=0.35, chamfer=True):
    """A cut slab: a flat face with faint noise, a bevel on its outer ring and a chamfer line inside it."""
    dither_fill(c, colors, lambda x, y: base + (value_noise(x, y, seed, 3.5) - 0.5) * 2 * noise, x0, y0, w, h)
    bevel_edges(c, x0, y0, w, h, colors=colors)
    # The shadow side drops to the recess colour: a slab stands proud of its joint.
    for x in range(x0, x0 + w):
        shift(c, colors, x, y0 + h - 1, -2)
    for y in range(y0, y0 + h):
        shift(c, colors, x0 + w - 1, y, -2)
    if chamfer and w > 4 and h > 4:
        for x in range(x0 + 1, x0 + w - 1):
            shift(c, colors, x, y0 + h - 2, -1)
        for y in range(y0 + 1, y0 + h - 1):
            shift(c, colors, x0 + w - 2, y, -1)


def tool_marks(c, colors, seed, count=5, x0=0, y0=0, w=16, h=16, d=-1):
    """Short diagonal chisel scores, two pixels long, on a dressed face."""
    for k in range(count):
        x = x0 + int(hash01(k, 1, seed) * (w - 2))
        y = y0 + int(hash01(k, 2, seed) * (h - 2))
        for i in range(2):
            shift(c, colors, x + i, y + i, d)


def tiles(c, colors, seed, size=8, base=3.6, spread=1.0, joint=None, chamfer=True):
    """A grid of square tiles with a 1 px joint, each its own tone, bevelled."""
    joint = colors[0] if joint is None else joint
    n = 16 // size
    for j in range(n):
        for i in range(n):
            tone = base + (hash01(i, j, seed) - 0.5) * spread
            plate(c, colors, i * size, j * size, size - 1, size - 1, seed + i * 3 + j, tone, chamfer=chamfer)
    for k in range(16):
        for j in range(n):
            c.set(k, j * size + size - 1, joint)
            c.set(j * size + size - 1, k, joint)


def bark(c, colors, seed, base=2.4, fissures=3, knot=True, light=None):
    """A log's side: vertical bark ridges (each ridge a lit crest, a flat and a shaded flank, wandering a little
    with the grain), deep fissures with a lit right edge, a knot."""
    def fn(x, y):
        wobble = (value_noise(x * 0.5, y * 0.25, seed + 2, 1.0) - 0.5) * 2.0
        phase = (x + wobble) % 4
        v = base + (value_noise(x * 0.6, y * 0.15, seed, 1.0) - 0.5) * 1.4
        if phase < 1:
            v += 1.6  # the crest
        elif phase < 2.5:
            v += 0.2
        else:
            v -= 1.2  # the flank in shadow
        return v + (hash01(x, y // 4, seed + 1) - 0.5) * 0.4
    dither_fill(c, colors, fn)
    for k in range(fissures):
        x = int(hash01(k, 5, seed) * 16)
        y = int(hash01(k, 6, seed) * 16)
        crack_line(c, seed + k, x, y, 5 + int(hash01(k, 7, seed) * 6), colors[0], colors=colors, wander=0.25)
    if knot:
        kx, ky = 3 + int(hash01(1, 9, seed) * 9), 3 + int(hash01(2, 9, seed) * 9)
        for dy in range(-2, 3):
            for dx in range(-1, 2):
                r = math.hypot(dx * 1.6, dy)
                if r <= 2.2:
                    shift(c, colors, kx + dx, ky + dy, -2 if r < 0.9 else 1 if r < 1.6 else -1)
    if light:
        for y in range(16):
            for x in range(16):
                if hash01(x, y, seed + 77) < 0.04:
                    c.set(x, y, light)


def crystals(c, colors, seed, clusters, size=(2, 4), stone=P.ASH_STONE):
    """Ore inclusions: a few crystalline lumps, each a facet of 2-4 pixels with a highlight at its top-left, a
    shadow at its bottom-right and a darker stone pixel beside it (the socket)."""
    for k in range(clusters):
        cx, cy = int(hash01(k, 1, seed) * 16), int(hash01(k, 2, seed) * 16)
        n = size[0] + int(hash01(k, 3, seed) * (size[1] - size[0] + 1))
        cells = [(cx, cy)]
        x, y = cx, cy
        for i in range(n - 1):
            r = hash01(k, 10 + i, seed)
            x, y = (x + 1, y) if r < 0.4 else (x, y + 1) if r < 0.7 else (x + 1, y + 1)
            cells.append((x, y))
        # Lumps, not lines: a second pixel beside most cells.
        for (x, y) in list(cells):
            if hash01(x, y, seed + 20) < 0.6:
                cells.append((x + 1, y) if hash01(x, y, seed + 21) < 0.5 else (x, y + 1))
        cells = list(dict.fromkeys(cells))
        for (x, y) in cells:
            c.set(x % 16, y % 16, step(colors, 3 + (1 if hash01(x, y, seed + 4) < 0.5 else 0)))
        # The socket: the stone to the lower right one step darker.
        for (x, y) in cells:
            for dx, dy in ((1, 0), (0, 1), (1, 1)):
                if (x + dx, y + dy) not in cells:
                    shift(c, stone, x + dx, y + dy, -1)
        hx, hy = cells[0]
        c.set(hx % 16, hy % 16, step(colors, 6))
        lx, ly = cells[-1]
        c.set(lx % 16, ly % 16, step(colors, 1))


def ash_bricks_base(seed, colors=P.ASH_BRICK, chips=3, crack=True, base=3.1):
    c = Canvas(16, 16)
    rows = [(8, [10, 6]), (8, [5, 11])] if seed % 2 == 0 else [(8, [6, 10]), (8, [11, 5])]
    brick_rows(c, colors, P.ASH_MORTAR, seed, rows, base=base, spread=1.6, chips=chips, crack=crack)
    return c


# --- Ash stone family ---------------------------------------------------------------------------------------------

@texture("ash_stone")
def ash_stone():
    c = Canvas(16, 16)
    cellular(c, P.ASH_STONE, 501, points=9, base=3.1, spread=1.3)
    tool_marks(c, P.ASH_STONE, 502, count=3)
    return c


@texture("cobbled_ash_stone")
def cobbled_ash_stone():
    c = Canvas(16, 16)
    rubble(c, P.ASH_STONE, 503, base=2.9)
    return c


@texture("mossy_cobbled_ash_stone", accent=P.MOSS)
def mossy_cobbled_ash_stone():
    c = cobbled_ash_stone()
    moss_patch(c, 504, 3, 12, 3, density=0.9)
    moss_patch(c, 505, 13, 4, 2, density=0.85)
    moss_patch(c, 506, 9, 15, 2, density=0.7)
    return c


@texture("polished_ash_stone")
def polished_ash_stone():
    c = Canvas(16, 16)
    plate(c, P.ASH_STONE, 0, 0, 16, 16, 507, base=3.7)
    tool_marks(c, P.ASH_STONE, 508, count=4, x0=2, y0=2, w=12, h=12)
    return c


@texture("ash_bricks")
def ash_bricks():
    return ash_bricks_base(510)


@texture("cracked_ash_bricks")
def cracked_ash_bricks():
    c = ash_bricks_base(510, chips=6, crack=True)
    crack_line(c, 511, 3, 0, 11, P.ASH_MORTAR, colors=P.ASH_BRICK, wander=0.4)
    crack_line(c, 512, 12, 9, 7, P.ASH_MORTAR, colors=P.ASH_BRICK, wander=0.4)
    # One brick eroded: its face sunk a step, its lit edge gone.
    for y in range(9, 15):
        for x in range(1, 4):
            shift(c, P.ASH_BRICK, x, y, -1)
    return c


@texture("mossy_ash_bricks", accent=P.MOSS)
def mossy_ash_bricks():
    c = ash_bricks_base(510, chips=4)
    # Moss grows out of the mortar: along the lower course's joints and up from the bottom.
    for x in range(16):
        if hash01(x, 0, 513) < 0.55:
            c.set(x, 7, step(P.MOSS, 2 + (1 if hash01(x, 1, 513) < 0.4 else 0)))
        if hash01(x, 2, 513) < 0.4:
            c.set(x, 15, step(P.MOSS, 1 + (1 if hash01(x, 3, 513) < 0.5 else 0)))
    moss_patch(c, 514, 2, 13, 2, density=0.9)
    moss_patch(c, 515, 12, 7, 2, density=0.8)
    moss_patch(c, 516, 6, 1, 1, density=0.9)
    return c


@texture("sooty_ash_bricks")
def sooty_ash_bricks():
    c = ash_bricks_base(510, chips=3)
    soot_stain(c, 517, (4, 15), strength=2, colors=P.ASH_BRICK)
    # Streaks of soot licking up between the lower bricks.
    for x in (3, 9, 14):
        for y in range(6, 15):
            if hash01(x, y, 518) < 0.7:
                c.set(x, y, step(P.SOOT, 2))
    return c


@texture("ash_cobble_bricks")
def ash_cobble_bricks():
    c = Canvas(16, 16)
    rows = [(4, course_widths(520 + r, choices=(3, 4, 5, 6))) for r in range(4)]
    brick_rows(c, P.ASH_BRICK, P.ASH_MORTAR, 521, rows, base=3.2, spread=1.8, chips=4, crack=True)
    return c


@texture("ash_tiles")
def ash_tiles():
    c = Canvas(16, 16)
    tiles(c, P.ASH_STONE, 530, base=3.7, spread=1.1)
    tool_marks(c, P.ASH_STONE, 531, count=3)
    return c


@texture("cracked_ash_tiles")
def cracked_ash_tiles():
    c = ash_tiles()
    crack_line(c, 532, 4, 1, 12, P.ASH_STONE[0], colors=P.ASH_STONE, wander=0.45)
    crack_line(c, 533, 12, 8, 6, P.ASH_STONE[0], colors=P.ASH_STONE, wander=0.3)
    # A knocked corner on the lower-right tile.
    for x, y in ((8, 8), (9, 8), (8, 9)):
        c.set(x, y, P.ASH_STONE[0])
    shift(c, P.ASH_STONE, 10, 9, 1)
    shift(c, P.ASH_STONE, 9, 10, 1)
    return c


@texture("ash_brick_pillar_side")
def ash_brick_pillar():
    """Fluted shaft: four ridges with a groove between, each ridge lit on its left, shaded on its right."""
    c = Canvas(16, 16)
    for x in range(16):
        k = x % 4
        for y in range(16):
            v = 3.4 + (value_noise(x, y, 540, 4.0) - 0.5) * 0.5
            if k == 0:
                v = 0.6  # the groove
            elif k == 1:
                v += 1.2
            elif k == 3:
                v -= 0.9
            c.set(x, y, dither_level(P.ASH_BRICK, v, x, y))
    # A chip out of one ridge.
    c.set(6, 4, P.ASH_MORTAR)
    c.set(6, 5, P.ASH_MORTAR)
    shift(c, P.ASH_BRICK, 7, 5, 1)
    return c


@texture("ash_brick_pillar_top")
def ash_brick_pillar_top():
    c = Canvas(16, 16)
    plate(c, P.ASH_BRICK, 0, 0, 16, 16, 541, base=3.3)
    plate(c, P.ASH_BRICK, 3, 3, 10, 10, 542, base=3.8)
    for k in range(3, 13):
        c.set(k, 2, P.ASH_MORTAR)
        c.set(2, k, P.ASH_MORTAR)
    return c


@texture("chiseled_ash_stone")
def chiseled_ash_stone():
    """A framed relief of the kingdom's sun-flame: a disc with eight short rays, carved into a sunk field."""
    c = Canvas(16, 16)
    plate(c, P.ASH_STONE, 0, 0, 16, 16, 550, base=3.6, chamfer=False)
    # The sunk field: two steps down, darkest along its top-left.
    for y in range(2, 14):
        for x in range(2, 14):
            shift(c, P.ASH_STONE, x, y, -2 if (x == 2 or y == 2) else -1)
    shift(c, P.ASH_STONE, 13, 13, 1)
    for k in range(3, 14):
        shift(c, P.ASH_STONE, k, 13, 1)
        shift(c, P.ASH_STONE, 13, k, 1)
    # The relief, raised: a disc and rays.
    cx, cy = 7.5, 7.5
    for y in range(3, 13):
        for x in range(3, 13):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            ray = r < 5.2 and abs(math.sin(ang * 4)) > 0.86
            if r < 2.6 or ray:
                lit = (dx + dy) < -0.5
                c.set(x, y, step(P.ASH_STONE, 5 if lit else 4 if (dx + dy) < 1.5 else 3))
    c.set(7, 7, step(P.ASH_STONE, 6))
    return c


@texture("carved_ash_stone")
def carved_ash_stone():
    """Geometric relief: a lozenge raised between four sunk triangles, in a bevelled frame."""
    c = Canvas(16, 16)
    plate(c, P.ASH_STONE, 0, 0, 16, 16, 555, base=3.5, chamfer=False)
    for y in range(1, 15):
        for x in range(1, 15):
            d = abs(x - 7.5) + abs(y - 7.5)
            if d <= 6.5:
                # The lozenge: lit on its upper-left faces, shaded on the lower-right.
                lit = (x + y) < 15
                v = 5 if lit and d > 4.5 else 4 if lit else 3 if d > 4.5 else 3.5
                c.set(x, y, dither_level(P.ASH_STONE, v, x, y))
            else:
                shift(c, P.ASH_STONE, x, y, -2 if (x <= 2 or y <= 2) else -1)
    for d in range(16):
        c.set(d, 15 - d if d < 16 else 0, P.ASH_STONE[0]) if False else None
    # The groove around the lozenge.
    for y in range(1, 15):
        for x in range(1, 15):
            if 6.5 < abs(x - 7.5) + abs(y - 7.5) <= 7.5:
                c.set(x, y, P.ASH_STONE[0] if (x + y) < 15 else P.ASH_STONE[1])
    return c


@texture("gilded_ash_stone", accent=P.GOLD_TRIM)
def gilded_ash_stone():
    """Polished ash stone with an inlaid gold frame and a centre stud, the Citadel's trim."""
    c = Canvas(16, 16)
    plate(c, P.ASH_STONE, 0, 0, 16, 16, 560, base=3.4)
    for k in range(3, 13):
        for (x, y) in ((k, 3), (k, 12), (3, k), (12, k)):
            lit = y == 3 or x == 3
            c.set(x, y, step(P.GOLD_TRIM, 5 if lit else 3))
    for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        c.set(x, y, step(P.GOLD_TRIM, 6 if y == 3 and x == 3 else 4))
    for (x, y, v) in ((7, 7, 6), (8, 7, 5), (7, 8, 4), (8, 8, 2)):
        c.set(x, y, step(P.GOLD_TRIM, v))
    # The groove the inlay sits in: a dark line below and right of the gold.
    for k in range(3, 13):
        shift(c, P.ASH_STONE, k, 13, -2)
        shift(c, P.ASH_STONE, 13, k, -2)
    return c


@texture("sanctum_rune_stone", accent=P.COLD_FLAME)
def sanctum_rune_stone():
    """A polished slab with the Sanctum's rune cut into it: the groove dark, its floor lit with cold flame."""
    c = Canvas(16, 16)
    plate(c, P.ASH_STONE, 0, 0, 16, 16, 565, base=3.2)
    rune = ["................", "................", ".....#....#.....", "....#.#..#.#....", "....#..##..#....",
            ".....#....#.....", "......####......", ".......##.......", ".......##.......", "......####......",
            ".....#....#.....", "....#......#....", "....#......#....", "................", "................",
            "................"]
    for y, row in enumerate(rune):
        for x, ch in enumerate(row):
            if ch == "#":
                c.set(x, y, step(P.COLD_FLAME, 4 if hash01(x, y, 566) < 0.6 else 5))
                shift(c, P.ASH_STONE, x - 1, y - 1, -2)
                shift(c, P.ASH_STONE, x + 1, y + 1, 1)
    return c


@texture("sanctum_flagstone")
def sanctum_flagstone():
    """The core's unbreakable floor: one big flag, a ring worn into it by the pilgrims' feet."""
    c = Canvas(16, 16)
    plate(c, P.ASH_STONE, 0, 0, 16, 16, 570, base=3.9)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if 4.6 < r < 5.8:
                shift(c, P.ASH_STONE, x, y, -1 if (x + y) < 15 else 1)
    tool_marks(c, P.ASH_STONE, 571, count=3, x0=5, y0=5, w=6, h=6)
    return c


# --- Slate, crypt and wet stone -----------------------------------------------------------------------------------

@texture("slate_tiles")
def slate_tiles():
    """Roof slates: courses of 4 px, every slate 4 wide with a rounded, shaded lower edge, each course offset."""
    c = Canvas(16, 16)
    for row in range(4):
        off = 2 if row % 2 else 0
        for s in range(5):
            x0 = s * 4 + off - 2
            tone = 3.4 + (hash01(row, s, 580) - 0.5) * 1.6
            for dy in range(4):
                for dx in range(4):
                    x, y = (x0 + dx) % 16, row * 4 + dy
                    v = tone + (value_noise(x, y, 581, 2.5) - 0.5) * 0.5
                    if dy == 0:
                        v += 1.1   # the lit upper edge of each slate
                    if dy == 3:
                        v = 0.5    # the shadow under its lower edge
                    if dx == 3:
                        v -= 1.0   # the shaded side edge
                    if dy == 3 and dx in (0, 3):
                        v = 0.2
                    c.set(x, y, dither_level(P.SLATE, v, x, y))
    # One cracked slate.
    crack_line(c, 582, 9, 4, 3, P.SLATE[0], colors=P.SLATE)
    return c


@texture("wet_crypt_stone")
def wet_crypt_stone():
    c = Canvas(16, 16)
    rows = [(8, [7, 9]), (8, [12, 4])]
    brick_rows(c, P.WET_STONE, P.WET_STONE[0], 590, rows, base=3.0, spread=1.4, chips=2)
    # Water runs down from the joints: darker, bluer streaks, with a wet glint at their top.
    for x in (2, 6, 11, 14):
        y0 = 1 + int(hash01(x, 0, 591) * 7)
        for y in range(y0, min(16, y0 + 5 + int(hash01(x, 1, 591) * 6))):
            shift(c, P.WET_STONE, x, y, -1)
        c.set(x, y0, step(P.WET_STONE, 6))
    return c


@texture("crypt_stone_tiles")
def crypt_stone_tiles():
    c = Canvas(16, 16)
    tiles(c, P.WET_STONE, 595, base=3.3, spread=1.2)
    # A puddle in the lower-right tile, a sheen along the lit edges.
    for y in range(10, 15):
        for x in range(9, 15):
            if hash01(x, y, 596) < 0.6:
                shift(c, P.WET_STONE, x, y, -2)
    for (x, y) in ((1, 1), (9, 1), (1, 9), (2, 1)):
        c.set(x, y, step(P.WET_STONE, 6))
    return c


# --- Soil, path, tar ----------------------------------------------------------------------------------------------

@texture("ash_soil")
def ash_soil():
    c = Canvas(16, 16)
    cellular(c, P.ASH_SOIL, 600, points=25, base=3.2, spread=1.6, jitter=1.0, noise=0.6)
    # Char and bone bits lying in the ash.
    for (x, y) in ((3, 4), (11, 9), (6, 13)):
        c.set(x, y, step(P.SOOT, 1))
        c.set(x + 1, y, step(P.SOOT, 2))
    c.set(13, 2, step(P.BONE, 3))
    c.set(14, 2, step(P.BONE, 2))
    return c


@texture("ash_cobble_path")
def ash_cobble_path():
    c = Canvas(16, 16)
    cellular(c, P.ASH_SOIL, 605, points=25, base=3.0, spread=1.2, jitter=1.0, noise=0.5)
    # Stones trodden into the ash: a few lumps of ash stone, each with its lit top-left.
    for k, (x, y, w, h) in enumerate(((1, 2, 4, 3), (7, 1, 5, 4), (12, 6, 4, 4), (3, 9, 5, 4), (9, 11, 4, 3), (14, 13, 3, 3))):
        tone = 3.0 + (hash01(k, 1, 606) - 0.5) * 1.4
        for dy in range(h):
            for dx in range(w):
                if (dx in (0, w - 1)) and (dy in (0, h - 1)):
                    continue
                c.set((x + dx) % 16, (y + dy) % 16, dither_level(P.ASH_STONE, tone, x + dx, y + dy))
        bevel_edges(c, x, y, w, h, colors=P.ASH_STONE)
    return c


@texture("tar_block")
def tar_block():
    """Cooled tar: big smooth swells, a gloss line along the lit side of each."""
    c = Canvas(16, 16)
    owner = cellular(c, P.TAR, 610, points=6, base=2.6, spread=1.0, jitter=0.8, noise=0.3)
    for y in range(16):
        for x in range(16):
            k = owner[y][x]
            if owner[(y - 2) % 16][x] != k and owner[(y - 1) % 16][x] == k and hash01(x, y, 611) < 0.6:
                c.set(x, y, step(P.TAR, 6))
    return c


# --- Ores ---------------------------------------------------------------------------------------------------------

@texture("cinder_ore", accent=P.EMBER)
def cinder_ore():
    c = Canvas(16, 16)
    cellular(c, P.ASH_STONE, 620, points=9, base=2.9, spread=1.2)
    crystals(c, P.EMBER, 621, clusters=4, size=(2, 4))
    return c


@texture("amber_vein", accent=P.AMBER)
def amber_vein():
    c = Canvas(16, 16)
    cellular(c, P.ASH_STONE, 625, points=9, base=3.0, spread=1.2)
    crystals(c, P.AMBER, 626, clusters=3, size=(3, 5))
    return c


@texture("grave_iron_ore", accent=P.GRAVE_IRON)
def grave_iron_ore():
    c = Canvas(16, 16)
    cellular(c, P.ASH_STONE, 630, points=9, base=2.8, spread=1.2)
    crystals(c, P.GRAVE_IRON[2:] + [P.GRAVE_IRON_SPECULAR, P.GRAVE_IRON_SPECULAR, P.GRAVE_IRON_SPECULAR], 631,
             clusters=4, size=(2, 4))
    # Iron is told from the stone by its colder, flatter sheen: a specular pixel on each lump.
    for (x, y) in ((4, 3), (11, 10), (2, 12)):
        c.set(x, y, P.GRAVE_IRON_SPECULAR)
    return c


# --- Dead wood ----------------------------------------------------------------------------------------------------

@texture("dead_planks")
def dead_planks():
    c = Canvas(16, 16)
    wood_grain(c, P.DEAD_WOOD, 640, plank=4, base=3.5, spread=1.1, knots=1)
    # Two iron nails at the plank ends.
    for (x, y) in ((2, 1), (13, 9)):
        c.set(x, y, step(P.GRAVE_IRON, 4))
        shift(c, P.DEAD_WOOD, x + 1, y + 1, -2)
    return c


@texture("dead_log")
def dead_log():
    c = Canvas(16, 16)
    bark(c, P.DEAD_WOOD, 645, base=2.3, fissures=3, knot=True)
    return c


@texture("dead_log_top")
def dead_log_top():
    c = Canvas(16, 16)
    end_grain(c, P.DEAD_WOOD, 646, cx=8.2, cy=7.4, base=3.6, rings=1.7, bark=P.DEAD_WOOD, bark_width=1.5)
    for y in range(16):
        for x in range(16):
            if min(x, y, 15 - x, 15 - y) < 1.5:
                shift(c, P.DEAD_WOOD, x, y, -2)
    return c


@texture("stripped_dead_log")
def stripped_dead_log():
    c = Canvas(16, 16)
    wood_grain(c, P.DEAD_WOOD, 650, plank=16, vertical=True, base=3.9, spread=0.5, knots=1, bevel=False, streaks=0.6)
    for y in range(16):
        shift(c, P.DEAD_WOOD, 0, y, 1)
        shift(c, P.DEAD_WOOD, 15, y, -1)
    return c


@texture("stripped_dead_log_top")
def stripped_dead_log_top():
    c = Canvas(16, 16)
    end_grain(c, P.DEAD_WOOD, 651, cx=7.6, cy=8.1, base=3.9, rings=1.6)
    # Drying cracks radiating from the pith.
    for (x, y, dx, dy) in ((8, 7, 1, -1), (7, 9, -1, 1), (9, 9, 1, 1)):
        for i in range(4):
            c.set(x + dx * i, y + dy * i, P.DEAD_WOOD[0])
    bevel_edges(c, 0, 0, 16, 16, colors=P.DEAD_WOOD)
    return c


def door_half(top):
    """A plank door with a raised stile frame and a sunk panel; the top half has a barred window."""
    c = Canvas(16, 16)
    wood_grain(c, P.DEAD_WOOD, 660 if top else 661, plank=16, vertical=True, base=3.3, spread=0.4, knots=0, bevel=False)
    # Stiles and rails, raised: 3 px wide at the sides, a rail at the bottom (and the top of the top half).
    rails = [(0, 0, 3, 16), (13, 0, 3, 16)]
    rails.append((0, 13, 16, 3) if not top else (0, 0, 16, 3))
    rails.append((0, 7, 16, 2) if not top else (0, 12, 16, 2))
    for (x, y, w, h) in rails:
        dither_fill(c, P.DEAD_WOOD, lambda xx, yy: 3.9 + (value_noise(xx, yy * 0.3, 662, 1.0) - 0.5) * 0.8, x, y, w, h)
        bevel_edges(c, x, y, w, h, colors=P.DEAD_WOOD)
    # The panels, sunk: darkest along their top-left.
    panels = [(3, 0, 10, 7), (3, 9, 10, 4)] if not top else [(3, 3, 10, 9), (3, 14, 10, 2)]
    for (x, y, w, h) in panels:
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                shift(c, P.DEAD_WOOD, xx, yy, -2 if (xx == x or yy == y) else -1)
    if top:
        # A barred window in the upper panel.
        for yy in range(4, 11):
            for xx in range(5, 11):
                c.set(xx, yy, step(P.SOOT, 1 if hash01(xx, yy, 663) < 0.7 else 2))
        for xx in (6, 9):
            for yy in range(4, 11):
                c.set(xx, yy, step(P.GRAVE_IRON, 4 if yy % 3 else 5))
        for yy in (4, 10):
            for xx in range(5, 11):
                c.set(xx, yy, step(P.GRAVE_IRON, 3))
    # Iron hinge straps and nails.
    for yy in ((2, 11) if top else (3, 10)):
        for xx in range(0, 7):
            c.set(xx, yy, step(P.GRAVE_IRON, 4 if xx % 2 else 3))
        c.set(1, yy, P.GRAVE_IRON_SPECULAR)
    return c


@texture("dead_door_bottom")
def dead_door_bottom():
    return door_half(False)


@texture("dead_door_top")
def dead_door_top():
    return door_half(True)


@texture("dead_trapdoor")
def dead_trapdoor():
    """Four planks with two iron battens across them, a ring pull."""
    c = Canvas(16, 16)
    wood_grain(c, P.DEAD_WOOD, 665, plank=4, vertical=True, base=3.4, spread=1.0, knots=1)
    for y in (3, 12):
        for x in range(1, 15):
            c.set(x, y, step(P.GRAVE_IRON, 4))
            c.set(x, y + 1, step(P.GRAVE_IRON, 2))
        c.set(1, y, P.GRAVE_IRON_SPECULAR)
        for x in (2, 6, 9, 13):
            rivet(c, x, y, P.GRAVE_IRON)
    for (x, y) in ((7, 7), (8, 7), (6, 8), (9, 8), (7, 9), (8, 9)):
        c.set(x, y, step(P.GRAVE_IRON, 5 if y == 7 else 3))
    return c


# --- Charred wood (Citadel) ---------------------------------------------------------------------------------------

def char_crackle(c, seed, colors=P.CHARRED_WOOD, ember=None, share=0.0):
    """The alligator crackle of charred wood: a cellular net of cracks over the grain, lit edges on the scales, a
    few cracks glowing with ember when asked."""
    tmp = Canvas(16, 16)
    owner = cellular(tmp, colors, seed, points=12, base=3.0, spread=0.4, jitter=0.9, noise=0.0)
    for y in range(16):
        for x in range(16):
            k = owner[y][x]
            if owner[y][(x + 1) % 16] != k or owner[(y + 1) % 16][x] != k:
                if ember is not None and hash01(x, y, seed + 1) < share:
                    c.set(x, y, step(ember, 4 if hash01(x, y, seed + 2) < 0.7 else 5))
                else:
                    c.set(x, y, colors[0])
            elif owner[(y - 1) % 16][x] != k or owner[y][(x - 1) % 16] != k:
                shift(c, colors, x, y, 1)


@texture("charred_planks")
def charred_planks():
    c = Canvas(16, 16)
    wood_grain(c, P.CHARRED_WOOD, 670, plank=4, base=3.2, spread=1.0, knots=0, streaks=0.5)
    char_crackle(c, 671)
    return c


@texture("charred_log", accent=P.EMBER)
def charred_log():
    c = Canvas(16, 16)
    bark(c, P.CHARRED_WOOD, 675, base=2.8, fissures=2, knot=False)
    char_crackle(c, 676, ember=P.EMBER, share=0.22)
    return c


@texture("charred_log_top", accent=P.EMBER)
def charred_log_top():
    c = Canvas(16, 16)
    end_grain(c, P.CHARRED_WOOD, 677, cx=7.5, cy=7.5, base=3.4, rings=1.8, bark=P.SOOT, bark_width=1.5)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if 2.2 < r < 3.4 and hash01(x, y, 678) < 0.6:
                c.set(x, y, step(P.EMBER, 3 if hash01(x, y, 679) < 0.6 else 5))
    return c


@texture("smoldering_log", accent=P.EMBER)
def smoldering_log():
    c = Canvas(16, 16)
    bark(c, P.CHARRED_WOOD, 680, base=2.4, fissures=2, knot=False)
    char_crackle(c, 681, ember=P.EMBER, share=0.5)
    return c


@texture("smoldering_log_top", accent=P.EMBER)
def smoldering_log_top():
    c = Canvas(16, 16)
    end_grain(c, P.CHARRED_WOOD, 682, cx=7.5, cy=7.5, base=2.8, rings=1.8, bark=P.SOOT, bark_width=1.5)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if r < 4.2:
                v = 6 - r * 0.9 + (hash01(x, y, 683) - 0.5)
                c.set(x, y, dither_level(P.EMBER, v, x, y))
    return c


# --- Straw and thatch ---------------------------------------------------------------------------------------------

@texture("burnt_straw_side")
def burnt_straw_side():
    """Stalks of a bale, bound by two cords, the top third burnt black."""
    c = Canvas(16, 16)
    dither_fill(c, P.STRAW, lambda x, y: 3.2 + (value_noise(x * 0.3, y * 2.5, 690, 1.0) - 0.5) * 2.6 + (hash01(x // 2, y, 691) - 0.5) * 0.6)
    for x in range(16):
        burn = 4 + int(hash01(x, 0, 692) * 4)
        for y in range(burn):
            c.set(x, y, step(P.SOOT, 1 + (1 if hash01(x, y, 693) < 0.5 else 0)))
        tint(c, x, burn, -24)
    for y in (6, 12):
        for x in range(16):
            c.set(x, y, step(P.DEAD_WOOD, 1 if x % 3 else 2))
    return c


@texture("burnt_straw_top")
def burnt_straw_top():
    """The cut ends of the stalks: a field of dots, burnt at one corner."""
    c = Canvas(16, 16)
    dither_fill(c, P.STRAW, lambda x, y: 2.6 + (hash01(x, y, 695) - 0.5) * 2.4)
    for y in range(16):
        for x in range(16):
            if (x + y) % 2 == 0 and hash01(x, y, 696) < 0.5:
                shift(c, P.STRAW, x, y, 1)
            if x + y < 9 and hash01(x, y, 697) < 0.8:
                c.set(x, y, step(P.SOOT, 1 + (x + y) % 2))
    return c


@texture("burnt_thatch_side")
def burnt_thatch():
    """Thatch courses: bundles 4 px deep with ragged lower edges, soot creeping up from below."""
    c = Canvas(16, 16)
    for row in range(4):
        for x in range(16):
            tone = 3.4 + (hash01(x // 3 + row * 7, row, 700) - 0.5) * 1.6
            ragged = int(hash01(x, row, 701) * 2)
            for dy in range(4):
                y = row * 4 + dy
                v = tone + (hash01(x, y, 702) - 0.5) * 0.6
                if dy == 0:
                    v += 1.0
                if dy >= 3 - ragged:
                    v -= 2.2
                c.set(x, y, dither_level(P.STRAW, v, x, y))
    soot_stain(c, 703, (9, 15), strength=2, colors=P.STRAW)
    for x in range(16):
        if hash01(x, 9, 704) < 0.5:
            c.set(x, 15, step(P.SOOT, 1))
    return c


@texture("burnt_thatch_top")
def burnt_thatch_top():
    c = Canvas(16, 16)
    dither_fill(c, P.STRAW, lambda x, y: 3.2 + (value_noise(x * 2.0, y * 0.4, 705, 1.0) - 0.5) * 3.4)
    for y in range(16):
        for x in range(16):
            if hash01(x, y, 706) < 0.12:
                shift(c, P.STRAW, x, y, -2)
            elif hash01(x, y, 708) < 0.06:
                shift(c, P.STRAW, x, y, 2)
    soot_stain(c, 707, (0, 5), strength=2, colors=P.STRAW)
    return c


# --- Bone, iron, brass, amber, glass (zone accents) ---------------------------------------------------------------

@texture("bone_bricks")
def bone_bricks():
    c = Canvas(16, 16)
    rows = [(8, [7, 9]), (8, [11, 5])]
    brick_rows(c, P.BONE, P.BONE[0], 710, rows, base=3.6, spread=1.3, chips=2, crack=False)
    # Sutures: a jagged dark line across one brick in each course.
    for (x0, y) in ((2, 3), (12, 11)):
        x = x0
        for i in range(5):
            c.set((x + i) % 16, y + (i % 2), step(P.BONE, 1))
    return c


@texture("bone_pillar_side")
def bone_pillar():
    """Two long bones side by side, their knobbed ends at the top and the bottom."""
    c = Canvas(16, 16)
    fill(c, step(P.SOOT, 2))
    for x0 in (1, 9):
        for y in range(16):
            end = y < 3 or y > 12
            w = 6 if end else 4
            xs = x0 if end else x0 + 1
            for dx in range(w):
                x = xs + dx
                v = 3.8 + (value_noise(x, y, 711, 2.0) - 0.5) * 0.8
                if dx == 0:
                    v += 1.3
                elif dx == w - 1:
                    v -= 1.4
                if end and y in (2, 13):
                    v -= 0.8
                c.set(x, y, dither_level(P.BONE, v, x, y))
    return c


@texture("bone_pillar_top")
def bone_pillar_top():
    c = Canvas(16, 16)
    fill(c, step(P.SOOT, 2))
    for cx in (4.5, 11.5):
        for y in range(16):
            for x in range(16):
                r = math.hypot(x + 0.5 - cx, y + 0.5 - 8)
                if r < 3.6:
                    v = 3.4 + (1.5 if r < 1.6 else 0) - (1.2 if (x + y) > cx + 8 else 0.0)
                    c.set(x, y, dither_level(P.BONE, v, x, y))
    return c


@texture("grave_iron_plate")
def grave_iron_plate():
    """Four riveted plates with a specular line along their lit edges."""
    c = Canvas(16, 16)
    tiles(c, P.GRAVE_IRON, 715, base=3.2, spread=0.8, joint=P.GRAVE_IRON[0], chamfer=False)
    for (x0, y0) in ((0, 0), (8, 0), (0, 8), (8, 8)):
        for (x, y) in ((x0 + 1, y0 + 1), (x0 + 5, y0 + 1), (x0 + 1, y0 + 5), (x0 + 5, y0 + 5)):
            rivet(c, x, y, P.GRAVE_IRON)
        for k in range(1, 5):
            c.set(x0 + k, y0, P.GRAVE_IRON_SPECULAR)
    # A scratch.
    for i in range(5):
        shift(c, P.GRAVE_IRON, 9 + i, 11 + i // 2, 1)
    return c


@texture("grave_iron_grate")
def grave_iron_grate():
    """A lattice of bars for the pane block: 2 px bars every 5 px, rivets at the crossings, open between."""
    c = Canvas(16, 16)
    bars = (0, 1, 7, 8, 14, 15)
    for y in range(16):
        for x in range(16):
            if x in bars or y in bars:
                lit = (x in (0, 7, 14)) if x in bars else (y in (0, 7, 14))
                v = 3.8 if lit else 2.2
                v += (hash01(x, y, 720) - 0.5) * 0.6
                c.set(x, y, dither_level(P.GRAVE_IRON, v, x, y))
    for (x, y) in ((0, 0), (7, 0), (14, 0), (0, 7), (7, 7), (14, 7), (0, 14), (7, 14), (14, 14)):
        c.set(x, y, P.GRAVE_IRON_SPECULAR)
        c.set(x + 1, y + 1, step(P.GRAVE_IRON, 0))
    # Rust at a few crossings.
    for (x, y) in ((8, 1), (1, 8), (15, 15)):
        c.set(x, y, step(P.RUST, 2))
    return c


@texture("grave_iron_grate_top")
def grave_iron_grate_top():
    c = Canvas(16, 16)
    dither_fill(c, P.GRAVE_IRON, lambda x, y: 3.0 + (value_noise(x, y, 721, 3.0) - 0.5) * 0.8)
    for x in range(16):
        c.set(x, 0, P.GRAVE_IRON_SPECULAR if x % 4 else step(P.GRAVE_IRON, 5))
        c.set(x, 15, step(P.GRAVE_IRON, 0))
    return c


@texture("rusted_brass_block", accent=P.RUST)
def rusted_brass_block():
    c = Canvas(16, 16)
    tiles(c, P.BRASS, 725, base=3.4, spread=1.0, joint=P.BRASS[0], chamfer=False)
    for (x0, y0) in ((0, 0), (8, 0), (0, 8), (8, 8)):
        rivet(c, x0 + 1, y0 + 1, P.BRASS)
        rivet(c, x0 + 5, y0 + 5, P.BRASS)
        c.set(x0 + 2, y0, step(P.BRASS, 6))
        c.set(x0 + 3, y0, step(P.BRASS, 6))
    # Rust blooms spreading from the lower joints.
    for (x, y, r) in ((4, 13, 2), (12, 6, 2), (9, 15, 1)):
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                if math.hypot(dx, dy) <= r + (hash01(x + dx, y + dy, 726) - 0.5) and hash01(x + dx, y + dy, 727) < 0.85:
                    c.set((x + dx) % 16, (y + dy) % 16, step(P.RUST, 2 + int(hash01(x + dx, y + dy, 728) * 3)))
    return c


@texture("cut_brass")
def cut_brass():
    c = Canvas(16, 16)
    tiles(c, P.BRASS, 730, size=8, base=3.8, spread=0.6, joint=P.BRASS[0])
    for (x0, y0) in ((0, 0), (8, 0), (0, 8), (8, 8)):
        for k in range(1, 6):
            c.set(x0 + k, y0, step(P.BRASS, 6))
        c.set(x0, y0 + 1, step(P.BRASS, 6))
    return c


@texture("amber_block")
def amber_block():
    """Royal amber: lumps of resin with a bright core, a spark of light on each, dark seams between."""
    c = Canvas(16, 16)
    owner = cellular(c, P.AMBER, 735, points=6, base=3.2, spread=1.0, jitter=0.8, noise=0.4)
    for y in range(16):
        for x in range(16):
            k = owner[y][x]
            if owner[(y - 1) % 16][x] != k and owner[y][(x - 1) % 16] != k and owner[y][x] == owner[(y + 1) % 16][(x + 1) % 16]:
                c.set((x + 1) % 16, (y + 1) % 16, step(P.AMBER, 6))
    # An insect caught in one lump.
    for (x, y) in ((6, 9), (7, 9), (7, 10)):
        c.set(x, y, step(P.SOOT, 2))
    return c


@texture("ember_glass", accent=P.EMBER_GLASS)
def ember_glass():
    """Citadel windows: a lead lattice of diamonds, the panes translucent ember glass, brighter towards the middle."""
    c = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            d = abs(((x + y) % 8) - 4) + 0
            lead = (x + y) % 8 == 0 or (x - y) % 8 == 0
            if lead:
                c.set(x, y, step(P.GRAVE_IRON, 3 if (x + y) % 2 else 4))
                continue
            centre = 1.0 - (abs(x - 7.5) + abs(y - 7.5)) / 15.0
            v = 2.6 + centre * 2.4 + (value_noise(x, y, 740, 3.0) - 0.5) * 1.0
            col = dither_level(P.EMBER_GLASS, v, x, y)
            c.set(x, y, (col[0], col[1], col[2], 196))
    for (x, y) in ((4, 4), (12, 4), (4, 12), (12, 12), (8, 8)):
        c.set(x, y, step(P.GRAVE_IRON, 5))
    return c


# --- Ember crack, lantern, shrine, obelisk --------------------------------------------------------------------------

@texture("ember_crack")
def ember_crack():
    """Ground split by fire: plates of cooled slag with ember glowing in every gap, hottest at the seams' middle."""
    c = Canvas(16, 16)
    owner = cellular(c, P.TAR, 745, points=7, base=2.4, spread=1.2, jitter=0.9, noise=0.3)
    for y in range(16):
        for x in range(16):
            k = owner[y][x]
            edge = owner[y][(x + 1) % 16] != k or owner[(y + 1) % 16][x] != k
            if edge:
                hot = hash01(x, y, 746)
                c.set(x, y, step(P.EMBER, 5 if hot < 0.3 else 4 if hot < 0.8 else 3))
            elif owner[y][(x - 1) % 16] != k or owner[(y - 1) % 16][x] != k:
                c.set(x, y, step(P.EMBER, 1))
    return c


def lantern_frame(t):
    """One 16x16 frame of the ember lantern in vanilla's lantern layout: the body's side at (0,2)-(5,8), its top at
    (0,9)-(5,14), the cap's side at (1,0)-(4,1), its top at (1,10)-(4,13), the hook at (11,1)-(13,2) and (11,10)-
    (13,11) (the hanging hook goes on down to row 4 / 13). t in 0..1 is the pulse of the ember inside."""
    c = Canvas(16, 16)
    iron = P.GRAVE_IRON
    # Body side: an iron frame around a window of ember light.
    for y in range(2, 9):
        for x in range(0, 6):
            frame = x in (0, 5) or y in (2, 8)
            if frame:
                c.set(x, y, step(iron, 5 if (x == 0 or y == 2) else 2))
            else:
                cx, cy = abs(x - 2.5), abs(y - 5)
                v = 5.2 - (cx + cy) * 0.55 + t * 1.2 + (hash01(x, y, 750 + int(t * 10)) - 0.5) * 0.5
                c.set(x, y, dither_level(P.EMBER, v, x, y))
    c.set(0, 2, P.GRAVE_IRON_SPECULAR)
    c.set(2, 5, step(P.EMBER, 6))
    # A bar across the window.
    for x in range(1, 5):
        c.set(x, 5, step(iron, 3))
    # Body top and bottom: a plate with a rivet in each corner.
    for y in range(9, 15):
        for x in range(0, 6):
            c.set(x, y, step(iron, 4 if (x == 0 or y == 9) else 3 if (x < 5 and y < 14) else 1))
    for (x, y) in ((1, 10), (4, 10), (1, 13), (4, 13)):
        rivet(c, x, y, iron)
    # Cap side and top.
    for y in range(0, 2):
        for x in range(1, 5):
            c.set(x, y, step(iron, 5 if y == 0 else 2))
    for y in range(10, 14):
        for x in range(1, 5):
            c.set(x, y, step(iron, 4 if (x == 1 or y == 10) else 2))
    # Hooks: a chain link, lit on the left.
    for (x0, y0) in ((11, 1), (11, 10)):
        for y in range(y0, y0 + 4):
            for x in range(x0, x0 + 3):
                c.set(x, y, step(iron, 5 if x == x0 else 2) if (x != x0 + 1 or y in (y0, y0 + 3)) else CLEAR)
    return c


@texture("ember_lantern", accent=P.EMBER, animated={"frametime": 8})
def ember_lantern():
    sheet = Canvas(16, 48)
    for i, t in enumerate((0.0, 0.6, 0.3)):
        f = lantern_frame(t)
        for y in range(16):
            for x in range(16):
                sheet.set(x, i * 16 + y, f.get(x, y))
    return sheet


@texture("ember_shrine_side", accent=P.EMBER)
def ember_shrine_side():
    """The brazier's drum (rows 4-11, columns 3-12 on the model) and its rim band (rows 0-1): dark iron with ember
    showing through four slits."""
    c = Canvas(16, 16)
    dither_fill(c, P.GRAVE_IRON, lambda x, y: 3.0 + (value_noise(x, y, 755, 3.0) - 0.5) * 1.0)
    for y in range(16):
        for x in range(16):
            if y < 2:
                c.set(x, y, step(P.GRAVE_IRON, 5 if y == 0 else 2))
            elif y in (2, 3):
                c.set(x, y, step(P.GRAVE_IRON, 1))
    for x in (4, 8, 11):
        for y in range(5, 11):
            c.set(x, y, step(P.EMBER, 5 if y in (7, 8) else 4 if y in (6, 9) else 2))
            shift(c, P.GRAVE_IRON, x - 1, y, -1)
            shift(c, P.GRAVE_IRON, x + 1, y, 1)
    for (x, y) in ((3, 4), (12, 4), (3, 11), (12, 11)):
        rivet(c, x, y, P.GRAVE_IRON)
    for y in range(12, 16):
        for x in range(16):
            c.set(x, y, step(P.GRAVE_IRON, 2 if y < 15 else 0))
    return c


@texture("ember_shrine_top")
def ember_shrine_top():
    """The bowl: iron rim, coals glowing brightest in the middle (rows 2-13 are what the model shows)."""
    c = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if r > 6.2:
                c.set(x, y, step(P.GRAVE_IRON, 4 if (x + y) < 15 else 2))
            else:
                v = 5.6 - r * 0.55 + (value_noise(x, y, 760, 1.8) - 0.5) * 2.0
                c.set(x, y, dither_level(P.EMBER, v, x, y))
    return c


@texture("void_obelisk")
def void_obelisk():
    """The shaft (columns 4-11, rows 0-12): dark violet stone with a carved groove down each side, notched."""
    c = Canvas(16, 16)
    dither_fill(c, P.TAR, lambda x, y: 3.6 + (value_noise(x, y, 765, 3.0) - 0.5) * 1.2)
    for y in range(16):
        c.set(4, y, step(P.TAR, 6))
        c.set(11, y, step(P.TAR, 1))
        c.set(5, y, step(P.TAR, 0) if y % 4 == 1 else step(P.TAR, 5))
        c.set(10, y, step(P.TAR, 0) if y % 4 == 3 else step(P.TAR, 2))
    for y in range(2, 13, 3):
        for x in range(6, 10):
            shift(c, P.TAR, x, y, -2)
            shift(c, P.TAR, x, y + 1, 1)
    return c


@texture("void_obelisk_top")
def void_obelisk_top():
    c = Canvas(16, 16)
    dither_fill(c, P.TAR, lambda x, y: 3.4 + (value_noise(x, y, 766, 2.0) - 0.5) * 1.0)
    bevel_edges(c, 4, 4, 8, 8, colors=P.TAR)
    for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)):
        c.set(x, y, step(P.TAR, 6 if x == 7 and y == 7 else 1))
    return c


@texture("void_obelisk_runes")
def void_obelisk_runes():
    """Rune strokes on a transparent sheet (the renderer tints it): pale, lit along their top-left."""
    c = Canvas(16, 16)
    glyphs = ["................", "....#......#....", "....#..##..#....", "....#.#..#.#....", "....##....##....",
              "....#......#....", "................", ".....#....#.....", "......####......", ".......##.......",
              ".......##.......", "......#..#......", ".....#....#.....", "....#......#....", "................",
              "................"]
    for y, row in enumerate(glyphs):
        for x, ch in enumerate(row):
            if ch == "#":
                c.set(x, y, (240, 232, 255, 230))
                if c.get(x + 1, y + 1)[3] == 0:
                    c.set(x + 1, y + 1, (120, 100, 160, 120))
    return c


@texture("void_obelisk_core")
def void_obelisk_core():
    """The floating shard: a faceted crystal, each face lit from its top-left (tinted by the renderer)."""
    c = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            facet = ((x // 8) + (y // 8)) % 2
            d = (x % 8) + (y % 8)
            v = 190 + (40 if facet else 0) - d * 5
            c.set(x, y, (v, v - 6, min(255, v + 10), 255))
    for k in range(16):
        c.set(k, 0, (255, 250, 255, 255))
        c.set(0, k, (255, 250, 255, 255))
        c.set(k, 15, (120, 110, 150, 255))
        c.set(15, k, (120, 110, 150, 255))
    return c


# --- Markers: milestone and gravestone ------------------------------------------------------------------------------

@texture("milestone")
def milestone():
    c = Canvas(16, 16)
    dither_fill(c, P.KERB_STONE, lambda x, y: 3.4 + (value_noise(x, y, 770, 3.0) - 0.5) * 1.8)
    plate(c, P.KERB_STONE, 3, 2, 10, 14, 770, base=3.6)
    tool_marks(c, P.KERB_STONE, 771, count=3, x0=4, y0=3, w=8, h=12, d=-2)
    soot_stain(c, 772, (12, 15), strength=1, colors=P.KERB_STONE)
    return c


@texture("milestone_front")
def milestone_front():
    """The post's face: a carved crown over a numeral line, the kingdom's road mark."""
    c = milestone()
    mark = ["..........", "..........", "..#.#.#...", "..#####...", "..........", "..#####...", "..........",
            "...#.#....", "...#.#....", "..........", "..#.#.#...", ".........."]
    for y, row in enumerate(mark):
        for x, ch in enumerate(row):
            if ch == "#":
                c.set(x + 3, y + 3, P.KERB_STONE[0])
                shift(c, P.KERB_STONE, x + 4, y + 4, 1)
    return c


@texture("milestone_top")
def milestone_top():
    c = Canvas(16, 16)
    dither_fill(c, P.KERB_STONE, lambda x, y: 3.6 + (value_noise(x, y, 773, 3.0) - 0.5) * 1.2)
    bevel_edges(c, 3, 3, 10, 10, colors=P.KERB_STONE)
    moss_patch(c, 774, 5, 5, 1, density=0.9)
    return c


@texture("gravestone", accent=P.MOSS)
def gravestone():
    c = Canvas(16, 16)
    cellular(c, P.ASH_STONE, 775, points=6, base=3.4, spread=0.9, noise=0.3)
    moss_patch(c, 776, 3, 13, 2, density=0.8)
    return c


@texture("gravestone_front", accent=P.MOSS)
def gravestone_front():
    """The headstone's face: a dressed slab with a cross and two worn lines of a name."""
    c = Canvas(16, 16)
    plate(c, P.ASH_STONE, 0, 0, 16, 16, 777, base=3.5)
    for y in range(3, 11):
        c.set(7, y, P.ASH_STONE[0])
        c.set(8, y, P.ASH_STONE[1])
        shift(c, P.ASH_STONE, 9, y, 1)
    for x in range(5, 11):
        c.set(x, 5, P.ASH_STONE[0])
        shift(c, P.ASH_STONE, x, 6, 1) if x not in (7, 8) else None
    for row, (x0, x1) in enumerate(((4, 12), (5, 11))):
        for x in range(x0, x1):
            if hash01(x, row, 778) < 0.7:
                c.set(x, 12 + row * 2, P.ASH_STONE[1])
    moss_patch(c, 779, 2, 14, 2, density=0.8)
    return c


# --- Roads --------------------------------------------------------------------------------------------------------

@texture("kingsroad_flagstones")
def kingsroad_flagstones():
    c = Canvas(16, 16)
    brick_rows(c, P.ROAD_STONE, P.SANDSTONE_JOINT, 780, [(9, [11, 5]), (7, [6, 10])], base=3.4, spread=1.5, chips=3,
               crack=True)
    soot_stain(c, 781, (13, 15), strength=1, colors=P.ROAD_STONE)
    return c


@texture("kingsroad_kerb")
def kingsroad_kerb():
    c = Canvas(16, 16)
    brick_rows(c, P.KERB_STONE, P.SANDSTONE_JOINT, 785, [(8, [16]), (8, [8, 8])], base=3.6, spread=1.0, chips=2,
               crack=False)
    soot_stain(c, 786, (11, 15), strength=2, colors=P.KERB_STONE)
    return c


# --- The Cleft Stele (from stele_art.py) ---------------------------------------------------------------------------

# Fissures in tile space, each leaving the tile on one edge and coming back on the opposite edge at the same spot, so
# stacked tiles join into long veins.
FISSURES = [
    [(5, 0), (5, 2), (6, 3), (6, 5), (5, 6), (5, 7), (6, 8), (7, 9), (7, 11), (6, 12), (6, 14), (5, 15)],
    [(12, 4), (13, 5), (13, 6), (14, 7), (15, 8)],
    [(0, 8), (1, 9), (2, 9), (3, 10)],
]


def walk(points):
    out = []
    for (x0, y0), (x1, y1) in zip(points, points[1:]):
        steps = max(abs(x1 - x0), abs(y1 - y0))
        for i in range(steps):
            out.append((round(x0 + (x1 - x0) * i / steps), round(y0 + (y1 - y0) * i / steps)))
    out.append(points[-1])
    return out


def cleft_stone(kindled):
    c = Canvas(16, 16)
    base = 3.0 - (0.9 if kindled else 0)
    brick_rows(c, P.CLEFT_STONE, P.CLEFT_STONE[0], 1503, [(8, [10, 6]), (8, [4, 12])], base=base, spread=1.2, chips=2,
               crack=False)
    tool_marks(c, P.CLEFT_STONE, 1504, count=6)
    pixels = [p for f in FISSURES for p in walk(f)]
    on = set(pixels)
    for x, y in pixels:
        c.set(x, y, step(P.EMBER, 4) if kindled else P.CLEFT_STONE[0])
    for fi, f in enumerate(FISSURES):
        for i, (x, y) in enumerate(walk(f)):
            rim = (x - 1, y)
            if rim not in on and 0 <= rim[0] < 16:
                if kindled:
                    c.set(*rim, step(P.EMBER, 2) if hash01(x, y, 7) < 0.55 else step(P.EMBER, 0))
                else:
                    shift(c, P.CLEFT_STONE, rim[0], rim[1], 1)
            if kindled and fi == 0 and i % 3 == 1:
                c.set(x, y, step(P.EMBER, 6))
    return c


@texture("cleft_stone")
def cleft_stone_cold():
    return cleft_stone(False)


@texture("cleft_stone_kindled", accent=P.EMBER)
def cleft_stone_kindled():
    return cleft_stone(True)


MIST = hexc("d9d2cc")
MIST_WARM = hexc("e8b48a")
SEAM = hexc("ffb04a")
SEAM_CORE = hexc("ffe6a8")
CRACK_FRAMES = 16


def mist_frame(t):
    """One frame of the stele crack: drifting wisps, denser toward the middle; a flickering ember seam down the
    centre. The sheet rises one pixel a frame, so 16 frames loop."""
    c = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            sy = (y + t) % 16
            n = 0.0
            for (cell, w) in ((4.0, 0.6), (2.0, 0.4)):
                a = value_noise(x, sy, 77, cell) if sy < 16 - cell else (
                    value_noise(x, sy, 77, cell) * (16 - sy) / cell + value_noise(x, sy - 16, 77, cell) * (sy - 16 + cell) / cell)
                n += a * w
            centre = 1.0 - abs(x - 7.5) / 8.0
            density = n * 0.75 + centre * 0.45
            alpha = int(max(0.0, min(1.0, (density - 0.4) * 1.5)) * 135)
            if alpha < 18:
                c.set(x, y, CLEAR)
                continue
            col = mix(MIST, MIST_WARM, max(0.0, centre - 0.45) * 1.6)
            c.set(x, y, (col[0], col[1], col[2], alpha))
    for y in range(16):
        sy = (y + t) % 16
        x = 7 + (1 if hash01(0, sy // 3, 91) < 0.4 else 0)
        pulse = 0.5 + 0.5 * math.sin((sy + t * 2) * math.pi / 8)
        c.set(x, y, mix(SEAM, SEAM_CORE, pulse)[:3] + (230,))
        c.set(x + 1 if x == 7 else x - 1, y, SEAM[:3] + (int(120 + 80 * pulse),))
    return c


@texture("stele_crack", animated={"frametime": 3, "interpolate": True})
def stele_crack():
    sheet = Canvas(16, 16 * CRACK_FRAMES)
    for t in range(CRACK_FRAMES):
        frame = mist_frame(t)
        for y in range(16):
            for x in range(16):
                sheet.set(x, t * 16 + y, frame.get(x, y))
    return sheet


# --- Main -----------------------------------------------------------------------------------------------------------

# Textures that are not tiled in the world (the renderer's overlays, animated sheets) and black materials.
NO_TILE = {"void_obelisk_runes", "void_obelisk_core", "stele_crack", "ember_lantern", "grave_iron_grate"}
DARK = {"tar_block", "void_obelisk", "void_obelisk_top", "ember_crack"}
# Cut slabs and plates: their lit top-left edge meets their shadowed bottom-right edge at the seam, which is the
# joint between two blocks and the look intended (polished deepslate does the same).
JOINTED = {"polished_ash_stone", "ash_tiles", "cracked_ash_tiles", "ash_brick_pillar_top", "chiseled_ash_stone",
           "carved_ash_stone", "sanctum_flagstone", "crypt_stone_tiles", "stripped_dead_log", "bone_bricks",
           "grave_iron_plate", "grave_iron_grate_top", "rusted_brass_block", "cut_brass", "gravestone_front",
           "kingsroad_kerb", "milestone", "milestone_front", "milestone_top", "gilded_ash_stone", "sanctum_rune_stone"}


def sheet(names):
    """A contact sheet of the textures named: each at 4x beside its 3x3 tiling at 2x, into build/model-preview/."""
    from art_kit import write_png
    cell_w, cell_h = 16 * 4 + 8 + 16 * 6 + 12, 16 * 6 + 14
    cols = 4
    rows = (len(names) + cols - 1) // cols
    W, H = cols * cell_w, rows * cell_h
    px = [[(40, 42, 48, 255)] * W for _ in range(H)]
    for i, name in enumerate(names):
        c = REGISTRY[name][0]()
        ox, oy = (i % cols) * cell_w, (i // cols) * cell_h + 12
        for y in range(16):
            for x in range(16):
                p = c.get(x, y)
                if p[3] == 0:
                    p = (70, 60, 80, 255) if (x + y) % 2 else (90, 80, 100, 255)
                elif p[3] < 255:
                    a = p[3] / 255.0
                    p = tuple(int(p[k] * a + (60 if k == 2 else 40) * (1 - a)) for k in range(3)) + (255,)
                for dy in range(4):
                    for dx in range(4):
                        px[oy + y * 4 + dy][ox + x * 4 + dx] = p
                for ty in range(3):
                    for tx in range(3):
                        for dy in range(2):
                            for dx in range(2):
                                px[oy + (ty * 16 + y) * 2 + dy][ox + 16 * 4 + 8 + (tx * 16 + x) * 2 + dx] = p
        # The name in a 3x5 bitmap font would be work; a coloured tab marks the index instead.
        for k in range(min(i + 1, 40)):
            for dy in range(3):
                px[oy - 8 + dy][ox + k * 3] = (220, 200, 120, 255)
    out = os.path.join("build", "model-preview", "block_art_sheet.png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    write_png(out, W, H, px)
    print("wrote %s (%s)" % (out, ", ".join("%d=%s" % (i + 1, n) for i, n in enumerate(names))))


def report(name, c, accent):
    """Prints the tile's numbers and what misses the bar (sheets and transparent textures are only summarised)."""
    frame = c
    if c.height > 16:
        frame = Canvas(16, 16)
        for y in range(16):
            for x in range(16):
                frame.set(x, y, c.get(x, y))
    r = tile_check(frame, accent)
    flags = []
    opaque = sum(1 for y in range(16) for x in range(16) if frame.get(x, y)[3] == 255)
    # A bevelled slab or brick has a joint at the seam by design, so only a strong step is flagged; the eye judges
    # the 3x3 on the sheet. Black materials (tar, the obelisk's stone) are allowed a narrower span.
    if opaque == 256 and name not in NO_TILE:
        if r["seam"] > 3.0 and name not in JOINTED:
            flags.append("seam %.2f" % r["seam"])
        if r["span"] < (60 if name in DARK else 89):
            flags.append("span %d" % r["span"])
    if r["levels"] < 4 and name not in NO_TILE:
        flags.append("levels %d" % r["levels"])
    if accent is not None and r.get("accent", 0) > 0.30:
        flags.append("accent %.0f%%" % (r["accent"] * 100))
    print("%-24s seam %.2f  span %3d  colours %2d%s%s" % (
        name, r["seam"], r["span"], r["levels"],
        "  accent %2.0f%%" % (r["accent"] * 100) if "accent" in r else "",
        "   <-- " + ", ".join(flags) if flags else ""))
    return flags


def main(names):
    if names and names[0] == "--sheet":
        sheet(names[1:] or list(REGISTRY))
        return 0
    problems = check = P.check_ramps()
    for p in check:
        print("palette:", p)
    todo = names or list(REGISTRY)
    flagged = []
    for name in todo:
        fn, accent, animated = REGISTRY[name]
        c = fn()
        c.save(os.path.join(BLOCK, name + ".png"))
        meta = os.path.join(BLOCK, name + ".png.mcmeta")
        if animated:
            with open(meta, "w") as f:
                json.dump({"animation": animated}, f, indent="\t")
                f.write("\n")
        if report(name, c, accent):
            flagged.append(name)
    print("wrote %d textures to %s%s" % (len(todo), BLOCK, ("; flagged: " + ", ".join(flagged)) if flagged else ""))
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
