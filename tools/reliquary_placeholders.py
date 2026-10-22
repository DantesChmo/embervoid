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


if __name__ == "__main__":
    empty_slot_icons()
    button_icon()
    print("Wrote Reliquary placeholder textures")
