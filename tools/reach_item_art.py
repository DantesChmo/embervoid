#!/usr/bin/env python3
"""Final 16x item textures for Realm 1 (PLAN R40.2c-d): consumables and the thrown fire flask first.

Run from the repository root: python3 tools/reach_item_art.py [name ...]
Each icon is a 16x16 sketch: a letter is a palette colour, '.' is transparent, 'o' is the outline. Like vanilla
items, the outline is not one black: each outline pixel is a dark shade of the colour it borders, so a bottle's rim
reads as glass and a bomb's as iron. Light comes from the top left. Preview with
    python3 tools/model_preview.py items <name ...>
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from art_kit import Canvas, clamp, hexc  # noqa: E402

ITEM_DIR = os.path.join("src", "main", "resources", "assets", "embervoid", "textures", "item")


def darker(c, k=0.45):
    return (clamp(c[0] * k), clamp(c[1] * k), clamp(c[2] * k), 255)


def icon(name, rows, palette, outline_k=0.42):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), name
    c = Canvas(16, 16)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch not in ".o":
                c.set(x, y, palette[ch])
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "o":
                near = [rows[y + dy][x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1),
                                                             (1, -1), (-1, 1))
                        if 0 <= x + dx < 16 and 0 <= y + dy < 16 and rows[y + dy][x + dx] not in ".o"]
                base = palette[near[0]] if near else hexc("2a2224")
                # The darkest neighbour sets the rim so it never looks lighter than what it outlines.
                if near:
                    base = min((palette[n] for n in near), key=lambda p: p[0] + p[1] + p[2])
                c.set(x, y, darker(base, outline_k))
    c.save(os.path.join(ITEM_DIR, name + ".png"))
    print("wrote", name)


# --- The Lantern Hag's fire flask (an extra model for FireFlaskRenderer, no item) -----------------------------------
# A round flask of burning oil with a rag stuffed in the neck, alight.

FIRE_FLASK = [
    "........F.......",
    ".......fYf......",
    "......fFYFf.....",
    ".......fFf......",
    ".......rRr......",
    "......orRro.....",
    "......oGwgo.....",
    ".....oGwggGo....",
    "....oGwggGGGo...",
    "...ohLLMMMMMlo..",
    "...oHhLMMMMllo..",
    "...oLLMMMMMllo..",
    "...oLMMMMMlldo..",
    "....oMMMMllddo..",
    ".....odllddo....",
    "......oooo......",
]

# --- Ash Bomb: a clay pot of cinders bound in iron, the fuse lit ------------------------------------------------------

ASH_BOMB = [
    "...........yF...",
    "..........Ff....",
    ".........ww.....",
    "........ww......",
    "......oIIo......",
    "....ooAAAAoo....",
    "...oAaAAAAAAo...",
    "..oAaAAAAAAAAo..",
    "..oAAAAEAAAAso..",
    "..oIIIIIIIIIIo..",
    "..oAAAAAAAeAso..",
    "..oAAAAAAAAsso..",
    "...oAAAeAAsso...",
    "...oIIIIIIIIo...",
    ".....osssso.....",
    "......oooo......",
]


def consumables():
    icon("fire_flask", FIRE_FLASK, {
        "F": hexc("ffb347"), "f": hexc("ff7a22"), "Y": hexc("fff0b0"),
        "r": hexc("6b553a"), "R": hexc("a68c66"),
        "G": hexc("8fa3ab"), "g": hexc("b9cdd4"), "w": hexc("e6f2f4"),
        "h": hexc("ffd27a"), "H": hexc("fff0c0"), "L": hexc("ff9a3a"), "M": hexc("e06a22"),
        "l": hexc("b8461a"), "d": hexc("7a2a12"),
    })
    icon("ash_bomb", ASH_BOMB, {
        "y": hexc("fff0b0"), "F": hexc("ffb347"), "f": hexc("ff7a22"), "w": hexc("7c6340"),
        "I": hexc("4a4d53"), "A": hexc("6e6863"), "a": hexc("948c83"), "s": hexc("47413d"),
        "E": hexc("ff8a2a"), "e": hexc("d8521c"),
    })


ITEMS = [("fire_flask", consumables), ("ash_bomb", consumables)]


if __name__ == "__main__":
    wanted = set(sys.argv[1:])
    done = set()
    for name, build in ITEMS:
        if (not wanted or name in wanted) and build not in done:
            build()
            done.add(build)
