#!/usr/bin/env python3
"""Sprite sheets of the mod's own particles (docs/art-direction.md §4): embers, ash flakes, soot, cinder sparks, blood
drops, amber motes, cold flames, soul wisps, the ground scorch, the shockwave ring and the burst flash.

Run from the repository root: python3 tools/particle_art.py
Writes textures/particle/<name>_<frame>.png and particles/<name>.json (the frame list the client's sprite provider
reads). Pure Python like art_kit. Small sprites are 8x8 (vanilla's flame and smoke are 8x8), decals 32x32. Colours
are mostly white-ish where the particle class tints them, so one sheet serves several colours.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from art_kit import ROOT, Canvas, hash01, hexc, mix, write_json  # noqa: E402

PARTICLE_DIR = os.path.join(ROOT, "textures", "particle")
DEFS_DIR = os.path.join(ROOT, "particles")


def radial(c, cx, cy, r, inner, outer, falloff=1.0, seed=0, wobble=0.0):
    """Fills a soft disc: colour inner at the centre to outer (with its alpha) at the rim."""
    for y in range(c.height):
        for x in range(c.width):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy) / r
            if wobble:
                a = math.atan2(dy, dx)
                d += wobble * (hash01(int(a * 3) + 7, seed, 11) - 0.5)
            if d >= 1:
                continue
            t = d ** falloff
            col = mix(inner, outer, t)
            alpha = int(outer[3] + (inner[3] - outer[3]) * (1 - t))
            c.set(x, y, (col[0], col[1], col[2], alpha))


def frames(name, count, w, h, draw):
    for i in range(count):
        c = Canvas(w, h)
        draw(c, i)
        c.save(os.path.join(PARTICLE_DIR, f"{name}_{i}.png"))
    write_json(os.path.join(DEFS_DIR, f"{name}.json"), {"textures": [f"embervoid:{name}_{i}" for i in range(count)]})
    print(f"wrote {name} ({count} frames)")


def ember():
    """A hot core with a thin dark crust, burning down over four frames (white-hot to dull red to a black cinder)."""
    hot = [hexc("fff4c0"), hexc("ffc34a"), hexc("ff7a2a"), hexc("8a2a10")]
    crust = [hexc("3a1a10"), hexc("2a1410"), hexc("1e100c"), hexc("120a08")]

    def draw(c, i):
        r = 3.6 - i * 0.5
        radial(c, 4, 4, r + 0.6, crust[i], (crust[i][0], crust[i][1], crust[i][2], 0), 0.6)
        radial(c, 4, 4, r, hot[i], (hot[i][0], hot[i][1], hot[i][2], 200), 1.4)
        if i < 3:
            c.set(3, 3, hexc("ffffff") if i == 0 else hot[0])

    frames("ember", 4, 8, 8, draw)


def ash_flake():
    """Irregular grey flakes, three shapes, a light edge on top."""
    grey = [hexc("3c3a3a"), hexc("5e5b58"), hexc("8a8580"), hexc("b5aea6")]

    def draw(c, i):
        for y in range(8):
            for x in range(8):
                d = math.hypot(x - 3.5, y - 3.5)
                n = hash01(x + i * 13, y + i * 7, 3)
                if d < 2.2 + n * 1.6 and n > 0.18:
                    c.set(x, y, grey[1 + int(n * 2.2)])
        for y in range(1, 8):
            for x in range(8):
                if c.get(x, y)[3] and not c.get(x, y - 1)[3]:
                    c.set(x, y, grey[3])
                if c.get(x, y)[3] and (x == 7 or not c.get(x + 1, y)[3]):
                    c.set(x, y, grey[0])

    frames("ash_flake", 3, 8, 8, draw)


def soot():
    """A puff that spreads and thins over four frames."""
    def draw(c, i):
        r = 2.0 + i * 0.7
        inner = (70, 62, 58, 230 - i * 50)
        outer = (30, 26, 24, 0)
        radial(c, 4, 4, r, inner, outer, 0.8, seed=i, wobble=0.25)

    frames("soot", 4, 8, 8, draw)


def cinder_spark():
    """A bright elongated spark; the second frame is dimmer and shorter."""
    def draw(c, i):
        core = hexc("fff6d0") if i == 0 else hexc("ffb347")
        glow = hexc("ff8a2a") if i == 0 else hexc("c04a14")
        for k, (x, y) in enumerate(((1, 6), (2, 5), (3, 4), (4, 3), (5, 2), (6, 1))):
            if i == 1 and (k == 0 or k == 5):
                continue
            c.set(x, y, core)
            c.set(x + 1, y, (glow[0], glow[1], glow[2], 160))
            c.set(x, y + 1, (glow[0], glow[1], glow[2], 120))

    frames("cinder_spark", 2, 8, 8, draw)


def blood_drop():
    """A dark red drop with one highlight."""
    def draw(c, i):
        radial(c, 4, 4.5, 2.8, hexc("c01e30"), (90, 10, 20, 255), 1.2)
        c.set(4, 1, hexc("a01828"))
        c.set(4, 2, hexc("c01e30"))
        c.set(3, 3, hexc("f06070"))

    frames("blood_drop", 1, 8, 8, draw)


def amber_mote():
    """A four-point warm star; the second frame is the small twinkle."""
    def draw(c, i):
        bright = hexc("ffe9a0")
        mid = hexc("f0b040")
        dim = (200, 120, 30, 120)
        c.set(3, 3, bright)
        c.set(4, 3, bright)
        c.set(3, 4, bright)
        c.set(4, 4, bright)
        reach = 3 if i == 0 else 1
        for k in range(1, reach + 1):
            col = mid if k == 1 else dim
            c.set(3 - k, 3, col)
            c.set(4 + k, 4, col)
            c.set(3, 3 - k, col)
            c.set(4, 4 + k, col)

    frames("amber_mote", 2, 8, 8, draw)


def cold_flame():
    """A blue-white flame lick, four frames of flicker."""
    def draw(c, i):
        for y in range(8):
            for x in range(8):
                sway = math.sin(y * 0.9 + i * 1.6) * 0.8
                dx = x + 0.5 - 4 - sway * (7 - y) / 7
                width = 0.6 + (y / 7) * 2.6
                if abs(dx) < width:
                    t = abs(dx) / width
                    heat = (y / 7) * 0.6 + (1 - t) * 0.6
                    col = mix(hexc("2a5aa8"), hexc("e8fbff"), min(1, heat))
                    c.set(x, y, (col[0], col[1], col[2], 255 if t < 0.7 else 150))

    frames("cold_flame", 4, 8, 8, draw)


def soul_wisp():
    """A pale blue-white wisp with a tail to the lower left, four frames of pulse."""
    def draw(c, i):
        pulse = 0.5 + 0.5 * math.sin(i * math.pi / 2)
        radial(c, 5, 3, 2.4 + pulse * 0.6, hexc("f0fbff"), (120, 200, 255, 0), 1.1)
        for k, (x, y) in enumerate(((3, 4), (2, 5), (1, 6), (0, 7))):
            a = 200 - k * 50
            col = mix(hexc("9fd8ff"), hexc("3c78b0"), k / 3)
            c.set(x, y, (col[0], col[1], col[2], a))
        c.set(5, 2, hexc("ffffff"))

    frames("soul_wisp", 4, 8, 8, draw)


def scorch():
    """A ground decal: a dark splat with a faint ember rim, irregular, 32x32."""
    def draw(c, i):
        for y in range(32):
            for x in range(32):
                dx, dy = x + 0.5 - 16, y + 0.5 - 16
                a = math.atan2(dy, dx)
                edge = 13 + 2.5 * math.sin(a * 5 + 1) + 1.5 * math.sin(a * 9 + 2) + 2 * (hash01(int(a * 4), i, 5) - 0.5)
                d = math.hypot(dx, dy)
                if d > edge:
                    continue
                t = d / edge
                n = hash01(x, y, 9)
                if t > 0.78 and n < 0.5 + (t - 0.78) * 2:
                    col = (120, 40, 14, int(160 * (1 - t) * 4)) if n > 0.6 else (24, 16, 14, 150)
                elif n > 0.85:
                    col = (60, 28, 16, 200)
                else:
                    col = (20 + int(n * 14), 14 + int(n * 10), 12 + int(n * 8), 230 - int(t * 110))
                c.set(x, y, col)

    frames("scorch", 1, 32, 32, draw)


def shockwave():
    """A ring with a soft inner and outer edge, drawn white (tinted by the particle), 32x32."""
    def draw(c, i):
        for y in range(32):
            for x in range(32):
                d = math.hypot(x + 0.5 - 16, y + 0.5 - 16) / 16
                if d > 1:
                    continue
                band = 1 - abs(d - 0.8) / 0.2
                if band <= 0:
                    continue
                a = int(255 * band ** 1.3)
                col = mix(hexc("ffd9a0"), hexc("ffffff"), band)
                c.set(x, y, (col[0], col[1], col[2], a))

    frames("shockwave", 1, 32, 32, draw)


def burst():
    """A radial flash with eight rays, white (tinted), two frames: full and fading."""
    def draw(c, i):
        for y in range(16):
            for x in range(16):
                dx, dy = x + 0.5 - 8, y + 0.5 - 8
                d = math.hypot(dx, dy) / 8
                if d > 1:
                    continue
                a = math.atan2(dy, dx)
                rays = 0.5 + 0.5 * math.cos(a * 8)
                core = max(0, 1 - d * 2.2)
                v = min(1, core + rays * max(0, 1 - d) * 0.9)
                v *= 1 - i * 0.45
                if v < 0.08:
                    continue
                col = mix(hexc("ffc070"), hexc("ffffff"), v)
                c.set(x, y, (col[0], col[1], col[2], int(255 * v)))

    frames("burst", 2, 16, 16, draw)


def whip_line():
    """A short streak for the lash's heat trail: a bright line fading at both ends, 8x8."""
    def draw(c, i):
        for x in range(8):
            v = 1 - abs(x - 3.5) / 4
            v *= 1 - i * 0.5
            col = mix(hexc("ff8a2a"), hexc("fff4c0"), v)
            c.set(x, 3, (col[0], col[1], col[2], int(255 * v)))
            c.set(x, 4, (col[0], col[1], col[2], int(180 * v)))

    frames("whip_line", 2, 8, 8, draw)


def main():
    os.makedirs(PARTICLE_DIR, exist_ok=True)
    os.makedirs(DEFS_DIR, exist_ok=True)
    ember()
    ash_flake()
    soot()
    cinder_spark()
    blood_drop()
    amber_mote()
    cold_flame()
    soul_wisp()
    scorch()
    shockwave()
    burst()
    whip_line()


if __name__ == "__main__":
    main()
