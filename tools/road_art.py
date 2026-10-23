#!/usr/bin/env python3
"""Textures of the Kingsroad paving (PLAN 14.2): warm ochre flagstones for the middle of a realm road and a pale
limestone kerb for its edges, so the road stands out from ash soil, podzol and gravel at eternal dusk.

Run from the repository root: python3 tools/road_art.py
Preview: python3 tools/model_preview.py blocks kingsroad_flagstones kingsroad_kerb ash_soil
"""
import os
import random

from art_kit import ROOT, Canvas, hash01, hexc, ramp, shade, value_noise

BLOCK = os.path.join(ROOT, "textures", "block")

# Sun-faded sandstone of the old kingdom, dusted with soot: lighter and warmer than every zone's ground.
FLAGSTONE = ramp("6a5340", "7b6249", "8c7154", "9b7f5f", "a88c6a", "b59a77")
FLAGSTONE_JOINT = hexc("2f241b")
# Bone-pale limestone with a grey cast: the brightest line on the ground.
KERB = ramp("85817a", "98948b", "aaa59b", "bab4a9", "c8c2b6", "d4cec2")
KERB_JOINT = hexc("4a4640")


def level(colors, value):
    return colors[max(0, min(len(colors) - 1, int(value * len(colors))))]


def stones(c, colors, joint, rows, seed, rng):
    """Courses of stones: rows is a list of (height, [widths]); each stone gets its own tone, a bevel lit from the
    top-left and value noise; joints are one pixel wide."""
    y = 0
    for r, (height, widths) in enumerate(rows):
        x = 0
        for s, width in enumerate(widths):
            tone = 0.35 + 0.35 * hash01(r, s, seed)
            for dy in range(height):
                for dx in range(width):
                    px, py = (x + dx) % 16, y + dy
                    if dx == width - 1 or dy == height - 1:
                        c.set(px, py, joint)
                        continue
                    v = tone + (value_noise(px, py, seed, 2.5) - 0.5) * 0.45
                    if dx == 0 or dy == 0:
                        v += 0.18
                    elif dx == width - 2 or dy == height - 2:
                        v -= 0.15
                    c.set(px, py, level(colors, v))
            x += width
        y += height
    # A few worn chips, two pixels long so they read as wear, not as dots.
    for _ in range(5):
        px, py = rng.randrange(15), rng.randrange(16)
        if c.get(px, py) != joint and c.get(px + 1, py) != joint:
            c.set(px, py, shade(c.get(px, py), -16))
            c.set(px + 1, py, shade(c.get(px + 1, py), -16))


def flagstones(rng):
    c = Canvas(16, 16)
    stones(c, FLAGSTONE, FLAGSTONE_JOINT, [(9, [11, 5]), (7, [6, 10])], 41, rng)
    return c


def kerb(rng):
    c = Canvas(16, 16)
    stones(c, KERB, KERB_JOINT, [(8, [16]), (8, [8, 8])], 77, rng)
    # Soot creeping up from the ground along the bottom course.
    for x in range(16):
        for y in range(13, 15):
            if hash01(x, y, 5) < 0.3 * (y - 12):
                c.set(x, y, shade(c.get(x, y), -30))
    return c


def main():
    rng = random.Random(1402)
    flagstones(rng).save(os.path.join(BLOCK, "kingsroad_flagstones.png"))
    kerb(rng).save(os.path.join(BLOCK, "kingsroad_kerb.png"))


if __name__ == "__main__":
    main()
