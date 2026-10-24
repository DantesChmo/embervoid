#!/usr/bin/env python3
"""The Crying Soul item of the Cleft Stele (PLAN 15.3). The stele's block textures (Cleft Stone cold and kindled, the
crack's mist) are written by tools/block_art.py since the 0.5.0 art pass.

Run from the repository root: python3 tools/stele_art.py
Preview: python3 tools/model_preview.py items crying_soul
"""
import math
import os

from art_kit import ROOT, Canvas, hexc, ramp, value_noise

ITEM = os.path.join(ROOT, "textures", "item")

# --- The Crying Soul -------------------------------------------------------------------------------------------

SOUL = ramp("3c5a6e", "5e8aa0", "8ab8c8", "b8dde6", "e4f6f8")
SOUL_OUTLINE = hexc("1f2c38")
SOUL_EMBER = hexc("ffb04a")
SOUL_EMBER_CORE = hexc("ffe8a0")


def level(colors, value):
    return colors[max(0, min(len(colors) - 1, int(value * len(colors))))]


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
    crying_soul().save(os.path.join(ITEM, "crying_soul.png"))


if __name__ == "__main__":
    main()
