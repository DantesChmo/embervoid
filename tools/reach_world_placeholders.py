#!/usr/bin/env python3
"""Writes placeholder block textures for the Ashen Reach blocks and ores (PLAN 9.5, track reach-world).

Run from the repository root: python3 tools/reach_world_placeholders.py
Procedural 16x16 stand-ins (seeded, so the output never changes) until the art pass.
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mob_placeholders import Canvas  # noqa: E402

BLOCK_DIR = os.path.join("src", "main", "resources", "assets", "embervoid", "textures", "block")


def shade(color, amount):
    return tuple(max(0, min(255, c + amount)) for c in color[:3]) + (255,)


def noisy(c, base, rng, spread=12):
    for y in range(16):
        for x in range(16):
            c.set(x, y, shade(base, rng.randint(-spread, spread)))


def stone(rng, base=(88, 84, 80)):
    c = Canvas(16, 16)
    noisy(c, base, rng)
    return c


def specks(c, rng, color, clusters, size=3):
    for _ in range(clusters):
        cx, cy = rng.randint(1, 14), rng.randint(1, 14)
        for _ in range(size):
            x = max(0, min(15, cx + rng.randint(-1, 1)))
            y = max(0, min(15, cy + rng.randint(-1, 1)))
            c.set(x, y, shade(color, rng.randint(-20, 20)))


def cracks(c, rng, color, count, length=7):
    for _ in range(count):
        x, y = rng.randint(0, 15), rng.randint(0, 15)
        for _ in range(length):
            c.set(x, y, shade(color, rng.randint(-25, 10)))
            x = max(0, min(15, x + rng.choice((-1, 0, 1))))
            y = max(0, min(15, y + rng.choice((0, 1))))


def bricks(c, rng, mortar, base, rows=4):
    noisy(c, base, rng, 8)
    for y in range(0, 16, 16 // rows):
        for x in range(16):
            c.set(x, y, mortar)
    for row in range(rows):
        offset = 0 if row % 2 == 0 else 4
        for y in range(row * (16 // rows), (row + 1) * (16 // rows)):
            for x in (offset, offset + 8):
                c.set(x % 16, y, mortar)


def rings(c, rng, outer, inner, core):
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            color = outer if d > 6 else (core if d < 2 else (inner if int(d) % 2 else shade(inner, -18)))
            c.set(x, y, shade(color, rng.randint(-6, 6)))


def save(name, c):
    c.save(os.path.join(BLOCK_DIR, name + ".png"))


def main():
    rng = random.Random(9_5)

    c = stone(rng)
    specks(c, rng, (214, 92, 38), 5)
    save("cinder_ore", c)

    c = stone(rng)
    cracks(c, rng, (226, 156, 44), 3, 6)
    specks(c, rng, (244, 186, 70), 2, 2)
    save("amber_vein", c)

    c = stone(rng, (62, 60, 62))
    specks(c, rng, (168, 178, 190), 4)
    save("grave_iron_ore", c)

    c = stone(rng, (34, 28, 26))
    cracks(c, rng, (236, 110, 34), 4, 9)
    save("ember_crack", c)

    c = Canvas(16, 16)
    bricks(c, rng, (44, 50, 56, 255), (78, 88, 96))
    for _ in range(10):
        x, y = rng.randint(0, 15), rng.randint(0, 15)
        for dy in range(rng.randint(2, 5)):
            c.set(x, min(15, y + dy), (52, 66, 80, 255))
    save("wet_crypt_stone", c)

    c = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            base = (58, 44, 30) if x % 3 else (40, 30, 22)
            c.set(x, y, shade(base, rng.randint(-10, 10)))
    specks(c, rng, (190, 80, 30), 3, 1)
    save("burnt_straw_side", c)
    c = Canvas(16, 16)
    rings(c, rng, (40, 30, 22), (62, 48, 32), (30, 22, 16))
    save("burnt_straw_top", c)

    c = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            base = (36, 30, 28) if x % 4 else (24, 20, 20)
            c.set(x, y, shade(base, rng.randint(-8, 8)))
    cracks(c, rng, (222, 98, 30), 3, 5)
    save("smoldering_log", c)
    c = Canvas(16, 16)
    rings(c, rng, (30, 24, 22), (70, 36, 22), (236, 120, 40))
    save("smoldering_log_top", c)

    for name, front in (("milestone", True), ("gravestone", True)):
        base = (96, 92, 86) if name == "milestone" else (104, 106, 110)
        c = stone(rng, base)
        save(name, c)
        c = stone(rng, base)
        dark = shade(base, -40)
        if name == "milestone":
            for x in range(5, 11):
                c.set(x, 5, dark)
                c.set(x, 9, dark)
            for y in range(5, 10):
                c.set(7, y, dark)
        else:
            for y in range(3, 12):
                c.set(7, y, dark)
                c.set(8, y, dark)
            for x in range(5, 11):
                c.set(x, 5, dark)
        save(name + "_front", c)
    c = stone(rng, (96, 92, 86))
    save("milestone_top", c)


if __name__ == "__main__":
    main()
