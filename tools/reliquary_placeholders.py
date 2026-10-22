#!/usr/bin/env python3
"""Writes placeholder textures for the Reliquary (PLAN 11.9-11.12): the inventory button icon, the empty-slot icons,
and the artifact and ring item textures.

Run from the repository root: python3 tools/reliquary_placeholders.py
Uses the PNG helpers of tools/mob_placeholders.py. Stand-ins until the art pass.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mob_placeholders import Canvas  # noqa: E402

ASSETS = os.path.join("src", "main", "resources", "assets", "embervoid", "textures")
OUTLINE = (40, 32, 28, 255)
# Vanilla empty-slot icons are a flat light silhouette on the slot grey.
SLOT_ICON = (198, 198, 198, 140)


def save(canvas, *path):
    out = os.path.join(ASSETS, *path)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    canvas.save(out)


def disc(cx, cy, r_out, r_in=0.0):
    """Pixels of a disc (or ring when r_in > 0) centred on a pixel-grid point."""
    pixels = []
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r_in <= d <= r_out:
                pixels.append((x, y))
    return pixels


def outlined(canvas, pixels, color_at):
    """Fills pixels with color_at(x, y) and draws a 1 px outline around the shape."""
    shape = set(pixels)
    for x, y in shape:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, y + dy)
            if n not in shape and 0 <= n[0] < 16 and 0 <= n[1] < 16:
                canvas.set(n[0], n[1], OUTLINE)
    for x, y in shape:
        canvas.set(x, y, color_at(x, y))


def empty_slot_icons():
    c = Canvas(16, 16)
    # Artifact: a medallion on a short chain.
    for x, y in disc(8, 9.5, 5.0) + [(8, 1), (8, 2), (7, 3), (9, 3)]:
        c.set(x, y, SLOT_ICON)
    save(c, "item", "empty_artifact_slot.png")
    c = Canvas(16, 16)
    for x, y in disc(8, 9, 5.5, 3.3) + [(7, 2), (8, 2), (9, 2), (8, 3)]:
        c.set(x, y, SLOT_ICON)
    save(c, "item", "empty_ring_slot.png")


def button_icon():
    """A small ember reliquary: a dark casket with a glowing ember window."""
    c = Canvas(16, 16)
    body = [(x, y) for x in range(3, 13) for y in range(5, 14)] + [(x, y) for x in range(4, 12) for y in range(3, 5)]
    outlined(c, body, lambda x, y: (92 - y * 2, 64 - y * 2, 48 - y, 255))
    for x, y in disc(8, 9, 2.6):
        c.set(x, y, (255, 150 + (y - 7) * 20, 40, 255))
    c.set(7, 8, (255, 230, 150, 255))
    save(c, "gui", "reliquary_button.png")


def shaded(base, x, y, light=18):
    """Simple top-left lighting on a base colour."""
    d = light - (x + y) * 2
    return (max(0, min(255, base[0] + d)), max(0, min(255, base[1] + d)), max(0, min(255, base[2] + d)), 255)


def item(name, pixels, base, extra=()):
    c = Canvas(16, 16)
    outlined(c, pixels, lambda x, y: shaded(base, x, y))
    for x, y, color in extra:
        c.set(x, y, color)
    save(c, "item", name + ".png")


def rect_pixels(x0, y0, x1, y1):
    return [(x, y) for x in range(x0, x1 + 1) for y in range(y0, y1 + 1)]


def artifacts():
    # Fields
    grain = [(8, y) for y in range(3, 14)] + [(7, y) for y in range(4, 10, 2)] + [(9, y) for y in range(5, 11, 2)] \
        + [(6, y) for y in range(4, 9, 2)] + [(10, y) for y in range(5, 10, 2)]
    item("last_harvest_grain", grain, (214, 170, 70), [(8, 2, (240, 210, 120, 255))])
    shoe = [p for p in disc(8, 8, 6.0, 3.4) if p[1] <= 11] + rect_pixels(2, 11, 4, 13) + rect_pixels(11, 11, 13, 13)
    item("ploughmans_horseshoe", shoe, (120, 116, 112), [(4, 6, (40, 36, 34, 255)), (11, 6, (40, 36, 34, 255))])
    # Woods
    fang = [(6, 3), (7, 3), (8, 3), (9, 3), (10, 3)] + rect_pixels(6, 4, 10, 6) + rect_pixels(7, 7, 10, 9) \
        + rect_pixels(8, 10, 10, 11) + [(9, 12), (10, 12), (10, 13)]
    item("royal_hound_fang", fang, (232, 226, 206), [(6, 3, (200, 160, 40, 255)), (10, 3, (200, 160, 40, 255))])
    tear = disc(8, 10, 4.0) + [(8, 3), (8, 4), (7, 5), (8, 5), (9, 5)]
    item("amber_tear", tear, (230, 140, 30), [(7, 8, (255, 230, 150, 255)), (6, 9, (255, 210, 120, 255))])
    # Necropolis
    medal = disc(8, 9.5, 5.0) + [(8, 2), (8, 3)]
    item("gravestone_medallion", medal, (130, 136, 140),
         [(8, 7, (60, 64, 70, 255)), (7, 8, (60, 64, 70, 255)), (8, 8, (60, 64, 70, 255)), (9, 8, (60, 64, 70, 255)),
          (8, 9, (60, 64, 70, 255)), (8, 10, (60, 64, 70, 255))])
    coin = disc(8, 8, 5.5)
    item("ferrymans_coin", coin, (190, 150, 60), [(x, y, (120, 90, 30, 255)) for x, y in disc(8, 8, 3.2, 2.2)])
    chain = []
    for i in range(4):
        cx, cy = 4 + i * 2.6, 4 + i * 2.6
        chain += disc(cx, cy, 2.2, 1.0)
    item("wardens_chain", chain, (90, 92, 98))
    # Citadel
    seal = disc(8, 8, 6.0)
    item("seal_of_the_ashen_king", seal, (160, 40, 30),
         [(x, y, (255, 170, 60, 255)) for x, y in [(8, 4), (6, 6), (10, 6), (8, 6), (7, 7), (9, 7), (8, 8), (6, 10),
                                                   (7, 10), (8, 10), (9, 10), (10, 10), (8, 11)]])


def ring(name, band, gem=None):
    c = Canvas(16, 16)
    outlined(c, disc(8, 9.5, 5.0, 2.8), lambda x, y: shaded(band, x, y))
    if gem:
        outlined(c, disc(8, 4.5, 2.2), lambda x, y: shaded(gem, x, y, 30))
    save(c, "item", name + ".png")


def rings():
    gold, iron, dark = (212, 170, 60), (150, 150, 156), (70, 66, 64)
    ring("couriers_ring", gold, (220, 220, 230))
    ring("ring_of_ash_feathers", (200, 196, 190), (110, 106, 100))
    ring("ring_of_the_fog_stalker", iron, (150, 190, 170))
    ring("delvers_ring", dark, (230, 140, 30))
    ring("drowned_mans_ring", (90, 120, 120), (60, 150, 190))
    ring("ring_of_embers", gold, (240, 80, 30))


if __name__ == "__main__":
    # tools/reach_item_art.py runs these and restyles the artifacts and rings; run it instead.
    print("Run python3 tools/reach_item_art.py.")
