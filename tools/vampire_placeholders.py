#!/usr/bin/env python3
"""Writes the Vampire Fang item texture (PLAN 8.11).

Run from the repository root: python3 tools/vampire_placeholders.py
The Vampire and the Vampire boss have final art in tools/overworld_mob_art.py; only the item is left here.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))


def vampire_fang():
    """16x16 item texture: a curved white fang with a red tip."""
    from mob_placeholders import Canvas
    c = Canvas(16, 16)
    outline = (60, 50, 50, 255)
    pixels = [(7, 2), (8, 2), (6, 3), (7, 3), (8, 3), (9, 3), (6, 4), (7, 4), (8, 4), (9, 4), (6, 5), (7, 5), (8, 5),
              (9, 5), (6, 6), (7, 6), (8, 6), (9, 6), (7, 7), (8, 7), (9, 7), (7, 8), (8, 8), (9, 8), (8, 9), (9, 9),
              (8, 10), (9, 10), (9, 11), (9, 12)]
    for x, y in pixels:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in pixels:
                c.set(x + dx, y + dy, outline)
    for x, y in pixels:
        shade = 250 - (y * 6)
        c.set(x, y, (shade, shade - 4, shade - 10, 255))
    for x, y in ((9, 11), (9, 12), (8, 10)):
        c.set(x, y, (180, 20, 30, 255))
    c.save(os.path.join("src", "main", "resources", "assets", "embervoid", "textures", "item", "vampire_fang.png"))


if __name__ == "__main__":
    vampire_fang()
    print("Wrote the Vampire Fang texture")
