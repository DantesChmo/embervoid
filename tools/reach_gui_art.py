#!/usr/bin/env python3
"""The Reliquary screen texture (PLAN R40.2d): textures/gui/reliquary.png, 256x256.

Run from the repository root: python3 tools/reach_gui_art.py
Layout numbers are ReliquaryLayout's (com.embervoid.reliquary): the panel at (0, 0) is WIDTH x HEIGHT, the model
box MODEL_LEFT..MODEL_RIGHT x MODEL_TOP..MODEL_BOTTOM. `--mock PATH` also writes the screen as it looks with 0.4.0's
2 artifact and 2 ring slots and the inventory (slot sprites placed like ReliquaryScreen does, empty-slot icons in). Slots are not baked in, because their number and place are
data (slot kinds); ReliquaryScreen draws a slot sprite at each slot instead: the vanilla inventory slot at
(176, 0) and the accessory slot, framed in ember bronze, at (176, 18), both 18x18. The panel keeps the vanilla
container greys so it sits with the inventory it opens from; the realm shows in the bronze and embers of the model
box and accessory slots. Preview with python3 tools/model_preview.py gui reliquary.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from art_kit import Canvas, hexc  # noqa: E402

OUT = os.path.join("src", "main", "resources", "assets", "embervoid", "textures", "gui", "reliquary.png")
WIDTH, HEIGHT = 176, 166
MODEL = (62, 8, 114, 78)
SLOT = 18

BLACK = hexc("000000")
LIGHT = hexc("ffffff")
PANEL = hexc("c6c6c6")
SHADOW = hexc("555555")
SLOT_FILL = hexc("8b8b8b")
SLOT_DARK = hexc("373737")
BRONZE = [hexc("3f2b14"), hexc("5c3f1d"), hexc("7d5928"), hexc("9e7535"), hexc("bd9145"), hexc("d9ad5c")]
EMBER = hexc("ff8a2a")
EMBER_HOT = hexc("ffd27a")


def rect(c, x, y, w, h, col):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            c.set(xx, yy, col)


def panel(c):
    """The vanilla container panel: black rim with cut corners, white top-left bevel, dark bottom-right."""
    w, h = WIDTH, HEIGHT
    rect(c, 1, 1, w - 2, h - 2, PANEL)
    rect(c, 2, 0, w - 4, 1, BLACK)
    rect(c, 2, h - 1, w - 4, 1, BLACK)
    rect(c, 0, 2, 1, h - 4, BLACK)
    rect(c, w - 1, 2, 1, h - 4, BLACK)
    for x, y in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
        c.set(x, y, BLACK)
    rect(c, 2, 1, w - 5, 2, LIGHT)
    rect(c, 1, 2, 2, h - 5, LIGHT)
    rect(c, 3, h - 3, w - 5, 2, SHADOW)
    rect(c, w - 3, 3, 2, h - 5, SHADOW)
    c.set(w - 3, 2, PANEL)
    c.set(2, h - 3, PANEL)
    c.set(3, 3, LIGHT)
    c.set(w - 4, h - 4, SHADOW)


def model_box(c):
    """The player model's alcove: a dark recess in a bronze frame, embers in the corners."""
    l, t, r, b = MODEL
    rect(c, l - 2, t - 2, r - l + 4, b - t + 4, BRONZE[1])
    rect(c, l - 2, t - 2, r - l + 3, 1, BRONZE[4])
    rect(c, l - 2, t - 2, 1, b - t + 3, BRONZE[4])
    rect(c, l - 1, b + 1, r - l + 3, 1, BRONZE[0])
    rect(c, r + 1, t - 1, 1, b - t + 3, BRONZE[0])
    rect(c, l - 1, t - 1, r - l + 2, b - t + 2, BRONZE[2])
    # The recess: near-black, lighter towards the floor where the ember light falls.
    for y in range(t, b):
        k = (y - t) / max(1, b - t - 1)
        shade = int(8 + 22 * k * k)
        rect(c, l, y, r - l, 1, (shade + 6, shade, shade - 2 if shade > 2 else 0, 255))
    for x in range(l + 2, r - 2, 4):  # a row of ember glow along the floor
        c.set(x, b - 1, BRONZE[2])
    for x, y in ((l - 2, t - 2), (r + 1, t - 2), (l - 2, b + 1), (r + 1, b + 1)):
        rect(c, x, y, 2, 2, BRONZE[5])
        c.set(x + (1 if x < l else 0), y + (1 if y < t else 0), EMBER)
    c.set((l + r) // 2, t - 2, EMBER_HOT)
    c.set((l + r) // 2 - 1, t - 2, EMBER)
    c.set((l + r) // 2 + 1, t - 2, EMBER)


def slot(c, x, y, accessory=False):
    rect(c, x, y, SLOT, SLOT, SLOT_FILL)
    rect(c, x, y, SLOT - 1, 1, SLOT_DARK)
    rect(c, x, y, 1, SLOT - 1, SLOT_DARK)
    rect(c, x + 1, y + SLOT - 1, SLOT - 1, 1, LIGHT)
    rect(c, x + SLOT - 1, y + 1, 1, SLOT - 1, LIGHT)
    if accessory:
        # Bronze corners and a warmer well, so artifact and ring slots read as the Reliquary's own.
        rect(c, x + 1, y + 1, SLOT - 2, SLOT - 2, hexc("8f8579"))
        for cx, cy in ((x, y), (x + SLOT - 3, y), (x, y + SLOT - 3), (x + SLOT - 3, y + SLOT - 3)):
            rect(c, cx, cy, 3, 1, BRONZE[4])
            rect(c, cx, cy, 1, 3, BRONZE[4])
            rect(c, cx + 2, cy, 1, 3, BRONZE[2])
            rect(c, cx, cy + 2, 3, 1, BRONZE[2])
            c.set(cx + 1, cy + 1, BRONZE[3])


def mock(path):
    """The screen with ReliquaryLayout.slots([2, 2]) (columns of up to 3, centred) and the inventory, 4x."""
    from model_preview import texture
    c = Canvas(WIDTH, HEIGHT)
    panel(c)
    model_box(c)
    top = 8 + (3 - 2) * SLOT // 2
    accessories = [(MODEL[0] - SLOT, top + i * SLOT, "artifact") for i in range(2)]
    accessories += [(MODEL[2] + 2, top + i * SLOT, "ring") for i in range(2)]
    for x, y, kind in accessories:
        slot(c, x - 1, y - 1, True)
        icon = texture("embervoid", "item/empty_%s_slot" % kind)
        for yy in range(16):
            for xx in range(16):
                r, g, b, a = icon.get(xx, yy)
                if a:
                    base = c.get(x + xx, y + yy)
                    k = a / 255
                    c.set(x + xx, y + yy, tuple(int(base[i] + ((r, g, b)[i] - base[i]) * k) for i in range(3)) + (255,))
    for i in range(36):
        x = 8 + (i % 9) * SLOT
        y = 142 if i >= 27 else 84 + (i // 9) * SLOT
        slot(c, x - 1, y - 1)
    big = Canvas(WIDTH * 4, HEIGHT * 4)
    for y in range(HEIGHT * 4):
        for x in range(WIDTH * 4):
            big.set(x, y, c.get(x // 4, y // 4))
    big.save(path)
    print("wrote", path)


def main():
    c = Canvas(256, 256)
    panel(c)
    model_box(c)
    slot(c, 176, 0)
    slot(c, 176, 18, True)
    c.save(OUT)
    print("wrote", OUT)
    if "--mock" in sys.argv:
        mock(sys.argv[sys.argv.index("--mock") + 1])


if __name__ == "__main__":
    main()
