#!/usr/bin/env python3
"""Textures of the Cleft Stele (PLAN 15.3): Cleft Stone cold and kindled, the crack's mist, the Crying Soul.

Cleft Stone is big dressed ashlar of a cool, violet-grey stone, crossed by fissures that run on from tile to tile, so a
stele of 24 stones reads as one slab split by veins. Kindled, the same fissures burn ember orange (the block is drawn
at full brightness in game, so the stone around them is darkened to keep the cracks the brightest thing on it). The
crack is a translucent animated sheet of pale mist with an ember seam down its middle.

Run from the repository root: python3 tools/stele_art.py
Preview: python3 tools/model_preview.py blocks cleft_stone cleft_stone_kindled stele_crack_ns
         python3 tools/model_preview.py items crying_soul
"""
import json
import math
import os
import random

from art_kit import ROOT, Canvas, CLEAR, hash01, hexc, mix, ramp, shade, value_noise

BLOCK = os.path.join(ROOT, "textures", "block")
ITEM = os.path.join(ROOT, "textures", "item")

STONE = ramp("37343d", "433f4a", "4e4a56", "5a5562", "66616e", "726c7a")
STONE_JOINT = hexc("221f27")
FISSURE = hexc("15131a")
FISSURE_EDGE = hexc("2c2932")
EMBER_CORE = hexc("ffe08a")
EMBER_HOT = hexc("ffaa3c")
EMBER_RIM = hexc("d2521e")
EMBER_SOOT = hexc("5a2a1c")

# Fissures in tile space: each a list of points walked one pixel at a time. They leave the tile on one edge and come
# back on the opposite edge at the same spot, so stacked and side-by-side tiles join into long veins.
FISSURES = [
    [(5, 0), (5, 2), (6, 3), (6, 5), (5, 6), (5, 7), (6, 8), (7, 9), (7, 11), (6, 12), (6, 14), (5, 15)],
    [(12, 4), (13, 5), (13, 6), (14, 7), (15, 8)],
    [(0, 8), (1, 9), (2, 9), (3, 10)],
]


def level(colors, value):
    return colors[max(0, min(len(colors) - 1, int(value * len(colors))))]


def walk(points):
    """Every pixel on the polyline, in order."""
    out = []
    for (x0, y0), (x1, y1) in zip(points, points[1:]):
        steps = max(abs(x1 - x0), abs(y1 - y0))
        for i in range(steps):
            out.append((round(x0 + (x1 - x0) * i / steps), round(y0 + (y1 - y0) * i / steps)))
    out.append(points[-1])
    return out


def ashlar(c, darken):
    """Two courses of big stones, the joints offset: (height, [widths]) per course."""
    y = 0
    for r, (height, widths) in enumerate([(8, [10, 6]), (8, [4, 12])]):
        x = 0
        for s, width in enumerate(widths):
            tone = 0.4 + 0.3 * hash01(r, s, 1503) - darken
            for dy in range(height):
                for dx in range(width):
                    px, py = (x + dx) % 16, y + dy
                    if dx == width - 1 or dy == height - 1:
                        c.set(px, py, STONE_JOINT)
                        continue
                    v = tone + (value_noise(px, py, 1503, 2.2) - 0.5) * 0.4
                    if dy == 0 or dx == 0:
                        v += 0.16
                    elif dy == height - 2 or dx == width - 2:
                        v -= 0.12
                    c.set(px, py, level(STONE, v))
            x += width
        y += height


def chisel(c, rng, darken):
    """Tool marks: short diagonal scores, two pixels long, on the faces of the stones."""
    for _ in range(7):
        x, y = rng.randrange(1, 14), rng.randrange(1, 14)
        if all(c.get(x + k, y + k) != STONE_JOINT for k in range(2)):
            for k in range(2):
                c.set(x + k, y + k, shade(c.get(x + k, y + k), -12 - int(darken * 30)))


def cleft_stone(kindled):
    rng = random.Random(1503)
    c = Canvas(16, 16)
    darken = 0.18 if kindled else 0.0
    ashlar(c, darken)
    chisel(c, rng, darken)
    pixels = [p for f in FISSURES for p in walk(f)]
    on_fissure = set(pixels)
    for x, y in pixels:
        if kindled:
            # The longest vein burns hottest; the short ones smoulder.
            c.set(x, y, EMBER_HOT)
        else:
            c.set(x, y, FISSURE)
    for f_index, f in enumerate(FISSURES):
        for i, (x, y) in enumerate(walk(f)):
            # A rim on the lit (left) side of each fissure pixel where it is still stone.
            rim = (x - 1, y)
            if rim not in on_fissure and 0 <= rim[0] < 16:
                if kindled:
                    c.set(*rim, EMBER_RIM if hash01(x, y, 7) < 0.55 else EMBER_SOOT)
                else:
                    c.set(*rim, FISSURE_EDGE)
            if kindled and f_index == 0 and i % 3 == 1:
                c.set(x, y, EMBER_CORE)
    return c


# --- The crack's mist ------------------------------------------------------------------------------------------

MIST = hexc("d9d2cc")
MIST_WARM = hexc("e8b48a")
SEAM = hexc("ffb04a")
SEAM_CORE = hexc("ffe6a8")
FRAMES = 16


def mist_frame(t):
    """One frame of the crack: drifting wisps, denser toward the middle; a flickering ember seam down the centre. The
    sheet rises one pixel a frame, so 16 frames loop."""
    c = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            sy = (y + t) % 16
            # Wisps: noise stretched vertically, tiled every 16 pixels so the loop has no seam.
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
    # The seam: two pixels wide, wandering a little, brighter in pulses.
    for y in range(16):
        sy = (y + t) % 16
        x = 7 + (1 if hash01(0, sy // 3, 91) < 0.4 else 0)
        pulse = 0.5 + 0.5 * math.sin((sy + t * 2) * math.pi / 8)
        c.set(x, y, mix(SEAM, SEAM_CORE, pulse)[:3] + (230,))
        c.set(x + 1 if x == 7 else x - 1, y, SEAM[:3] + (int(120 + 80 * pulse),))
    return c


def stele_crack():
    sheet = Canvas(16, 16 * FRAMES)
    for t in range(FRAMES):
        frame = mist_frame(t)
        for y in range(16):
            for x in range(16):
                sheet.set(x, t * 16 + y, frame.get(x, y))
    return sheet


# --- The Crying Soul -------------------------------------------------------------------------------------------

SOUL = ramp("3c5a6e", "5e8aa0", "8ab8c8", "b8dde6", "e4f6f8")
SOUL_OUTLINE = hexc("1f2c38")
SOUL_EMBER = hexc("ffb04a")
SOUL_EMBER_CORE = hexc("ffe8a0")


def crying_soul():
    """A tear of pale soul-light, pointed at the top, an ember still burning in it; a drop falling beside it."""
    c = Canvas(16, 16)
    cx, cy, r = 7.5, 9.5, 4.8

    def inside(x, y):
        px, py = x + 0.5, y + 0.5
        if py >= cy:
            return (px - cx) ** 2 + (py - cy) ** 2 <= r * r
        # Above the centre the sides close in to a point at y = 2.
        half = r * max(0.0, (py - 2.0) / (cy - 2.0)) ** 1.1
        return half >= 0.5 and abs(px - cx) <= half

    body = {(x, y) for y in range(16) for x in range(16) if inside(x, y)}
    for x, y in body:
        # Light from the upper left, the core brightest.
        d = math.hypot(x + 0.5 - (cx - 1.2), y + 0.5 - (cy - 0.5)) / (r + 1.5)
        c.set(x, y, level(SOUL, 1.0 - d * 1.1 + (value_noise(x, y, 5, 2.0) - 0.5) * 0.15))
    for x, y in body:
        if any((x + dx, y + dy) not in body for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            c.set(x, y, SOUL_OUTLINE)
    # The ember inside, low in the tear.
    for x, y, col in ((7, 10, SOUL_EMBER_CORE), (8, 10, SOUL_EMBER), (7, 11, SOUL_EMBER), (8, 11, SOUL_EMBER)):
        c.set(x, y, col)
    # A highlight on the upper left of the bulb.
    c.set(5, 8, SOUL[4])
    c.set(5, 9, SOUL[4])
    # A drop falling beside the bulb.
    for x, y, col in ((13, 13, SOUL[3]), (13, 14, SOUL_OUTLINE)):
        c.set(x, y, col)
    return c


def main():
    cleft_stone(False).save(os.path.join(BLOCK, "cleft_stone.png"))
    cleft_stone(True).save(os.path.join(BLOCK, "cleft_stone_kindled.png"))
    stele_crack().save(os.path.join(BLOCK, "stele_crack.png"))
    with open(os.path.join(BLOCK, "stele_crack.png.mcmeta"), "w") as f:
        json.dump({"animation": {"frametime": 3, "interpolate": True}}, f, indent="\t")
        f.write("\n")
    crying_soul().save(os.path.join(ITEM, "crying_soul.png"))


if __name__ == "__main__":
    main()
