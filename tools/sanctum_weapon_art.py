#!/usr/bin/env python3
"""16x item textures for the Sanctum weapons (PLAN L1): the Sanctum Longsword and the Ashbearer's Mace.

Run from the repository root: python3 tools/sanctum_weapon_art.py
Same style as the Realm 1 icons (tools/reach_item_art.py): drawn like vanilla handheld items, handle at the bottom
left and the business end at the top right, light from the top left, each outline pixel a dark shade of the colour
it borders, a one-pixel bevel. Overworld tier, so iron and bronze rather than realm materials; the Sanctum's ember
shows as one coal in the longsword's pommel and the cracks in the mace's head. Preview with
    python3 tools/model_preview.py items sanctum_longsword ashbearers_mace
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from art_kit import Canvas, hexc  # noqa: E402
from reach_item_art import ITEM_DIR, auto_outline, bevel, line_px  # noqa: E402

STEEL = [hexc("4f545a"), hexc("7d838a"), hexc("aab0b6"), hexc("d6dadd")]
BRONZE = [hexc("6b4f2a"), hexc("9a7440"), hexc("c49a58")]
LEATHER = [hexc("33241a"), hexc("553a28")]
IRON = [hexc("2c2a29"), hexc("454240"), hexc("625e5a"), hexc("86807a"), hexc("aaa39b")]
EMBER = [hexc("b8461a"), hexc("ff8a2a"), hexc("ffd27a")]
WOOD = [hexc("3b2e23"), hexc("5b4c3c"), hexc("7c6340")]


def longsword():
    """A long, narrow blade from the guard to the top right corner, a bronze crossguard, a leather grip and a bronze
    pommel with an ember in it."""
    c = Canvas(16, 16)
    blade = line_px(5, 10, 14, 1)
    for i, (x, y) in enumerate(blade):
        c.set(x, y, STEEL[2])
        c.set(x, y - 1, STEEL[3] if i < len(blade) - 1 else STEEL[2])   # the lit edge
        if i < len(blade) - 1:
            c.set(x + 1, y, STEEL[1])                                     # the shadowed edge
    for x, y in line_px(6, 9, 12, 3):                                     # the fuller down the middle
        c.set(x, y, STEEL[1] if (x + y) % 2 else STEEL[2])
    for i, (x, y) in enumerate(line_px(2, 9, 6, 13)):                     # the crossguard
        c.set(x, y, BRONZE[2] if i < 2 else BRONZE[1])
        c.set(x + 1, y, BRONZE[0] if i in (0, 4) else BRONZE[1])
    for i, (x, y) in enumerate(line_px(3, 12, 1, 14)):                    # the grip, wrapped
        c.set(x, y, LEATHER[1] if i % 2 == 0 else LEATHER[0])
    c.set(0, 15, BRONZE[1])                                               # the pommel and its ember
    c.set(1, 15, BRONZE[0])
    c.set(0, 14, BRONZE[2])
    c.set(1, 14, EMBER[1])
    auto_outline(c)
    bevel(c, 10)
    c.save(os.path.join(ITEM_DIR, "sanctum_longsword.png"))
    print("wrote sanctum_longsword")


def mace():
    """A wooden haft with a leather grip and an iron collar, and a flanged iron head cracked with ember."""
    c = Canvas(16, 16)
    for i, (x, y) in enumerate(line_px(1, 14, 8, 7)):                     # the haft: leather grip, then wood
        c.set(x, y, (LEATHER[1] if i % 2 == 0 else LEATHER[0]) if i < 3 else WOOD[2 if i % 3 else 1])
    c.set(0, 15, IRON[3])                                                 # the butt cap
    c.set(8, 8, IRON[2])                                                  # the collar under the head
    c.set(9, 7, IRON[1])
    cx, cy = 11.0, 4.0
    for y in range(16):                                                   # the head, lit from the top left
        for x in range(16):
            dx, dy = x - cx, y - cy
            if dx * dx + dy * dy <= 6.5:
                level = 3.4 - (dx + dy) * 0.5 - (dx * dx + dy * dy) * 0.12
                c.set(x, y, IRON[max(1, min(len(IRON) - 1, round(level)))])
    for x, y in ((11, 0), (8, 1), (14, 1), (15, 4), (14, 7)):             # the flanges
        c.set(x, y, IRON[4] if x + y < 12 else IRON[3] if x + y < 16 else IRON[2])
    for x, y, k in ((10, 3, 1), (11, 4, 2), (12, 5, 1), (12, 3, 0)):      # ember cracks across the head
        c.set(x, y, EMBER[k])
    auto_outline(c)
    bevel(c, 10)
    c.save(os.path.join(ITEM_DIR, "ashbearers_mace.png"))
    print("wrote ashbearers_mace")


if __name__ == "__main__":
    longsword()
    mace()
