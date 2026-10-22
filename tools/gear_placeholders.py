#!/usr/bin/env python3
"""Writes placeholder item textures for the Ashen Reach gear (PLAN milestone 11, track reach-gear).

Run from the repository root: python3 tools/gear_placeholders.py
Each icon is a 16x16 ASCII sketch; a letter is a palette colour, '.' is transparent. Stand-ins until the art pass.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mob_placeholders import Canvas  # noqa: E402

ITEM_DIR = os.path.join("src", "main", "resources", "assets", "embervoid", "textures", "item")

OUTLINE = (34, 28, 30, 255)


def icon(name, rows, palette):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), name
    c = Canvas(16, 16)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                c.set(x, y, OUTLINE if ch == "o" else palette[ch])
    c.save(os.path.join(ITEM_DIR, name + ".png"))


SHARD = [
    "................",
    "..........oo....",
    ".........oLLo...",
    "........oLLMo...",
    ".......oLLMMo...",
    "......oLMMMDo...",
    ".....oLMMMDo....",
    "....oLMMMDDo....",
    "....oMMMDDo.....",
    "...oMMMDDo......",
    "...oMMDDo.......",
    "..oMDDDo........",
    "..oDDDo.........",
    "..oDDo..........",
    "...oo...........",
    "................",
]

GEM = [
    "................",
    "................",
    "......oooo......",
    "....ooLLMMoo....",
    "...oLLLMMMMMo...",
    "...oLLMMMMMMo...",
    "..oLLMMMMMMMDo..",
    "..oLMMMMMMMMDo..",
    "..oMMMMMMMMDDo..",
    "..oMMMMMMMDDDo..",
    "...oMMMMMDDDo...",
    "...oMMMDDDDDo...",
    "....ooDDDDoo....",
    "......oooo......",
    "................",
    "................",
]

NUGGET = [
    "................",
    "................",
    "................",
    ".......ooo......",
    ".....ooLLMoo....",
    "....oLLMMMMMo...",
    "...oLMMMDMMMMo..",
    "...oMMMMMMDMMo..",
    "..oLMMDMMMMMDo..",
    "..oMMMMMMDMMDo..",
    "..oMMDMMMMMDDo..",
    "...oMMMMDDDDo...",
    "....ooDDDDoo....",
    "......oooo......",
    "................",
    "................",
]

INGOT = [
    "................",
    "................",
    "................",
    "................",
    "........oooooo..",
    "......ooLLLLLMo.",
    "....ooLLLMMMMMo.",
    "..ooLLLMMMMMMDo.",
    ".oLLLMMMMMMMDDo.",
    ".oMMMMMMMMMDDo..",
    ".oMMMMMMMMDDo...",
    ".oDDDDDDDDDo....",
    "..ooooooooo.....",
    "................",
    "................",
    "................",
]

VEINED_INGOT = [
    "................",
    "................",
    "................",
    "................",
    "........oooooo..",
    "......ooLLLLLMo.",
    "....ooLLLMEMMMo.",
    "..ooLLLMMEMMMDo.",
    ".oLLLMMEEMMMDDo.",
    ".oMMMEEMMMEDDo..",
    ".oMMEMMMMEDDo...",
    ".oDDDDDEDDDo....",
    "..ooooooooo.....",
    "................",
    "................",
    "................",
]

TEMPLATE = [
    "................",
    "..oooooooooooo..",
    "..oLLLLLLLLLMo..",
    "..oLMMMMMMMMDo..",
    "..oLMMEEMMMMDo..",
    "..oLMEEEEMMMDo..",
    "..oLMMEEMMEMDo..",
    "..oLMMMMMEEMDo..",
    "..oLMMEMMMEMDo..",
    "..oLMEEEMMMMDo..",
    "..oLMMEMMMMMDo..",
    "..oLMMMMMMEMDo..",
    "..oLMMMMMMMMDo..",
    "..oMDDDDDDDDDo..",
    "..oooooooooooo..",
    "................",
]

SOUL = [
    "................",
    ".......oo.......",
    "......oLLo......",
    ".....oLLLo......",
    "......oLLMo.....",
    ".....ooLMMoo....",
    "...ooLLMMMMMoo..",
    "..oLLEEMMMMMMDo.",
    "..oLEEEEMMMMMDo.",
    ".oLMEEEMMMMMDDo.",
    ".oMMMEMMMMMDDDo.",
    ".oMMMMMMMMDDDDo.",
    "..oMMMMMMDDDDo..",
    "...ooDDDDDDoo...",
    ".....oooooo.....",
    "................",
]

ASH = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "........E.......",
    ".......oEo......",
    "......oLELo.....",
    ".....oLLMLMo....",
    "....oLMLMMEMo...",
    "...oLMMEMMMMDo..",
    "..oLMMMMMDMMDDo.",
    "..oMDMMMDDMDDDo.",
    "...oooooooooo...",
    "................",
]

EMBER = [
    "................",
    ".......o........",
    "......oLo.......",
    "......oLLo......",
    ".....oLLLo..o...",
    "...o.oLLMLo.o...",
    "...ooLLMMLooLo..",
    "...oLLMMMMLLLo..",
    "..oLLMMEMMMMLo..",
    "..oLMMEEEMMMLo..",
    "..oLMEEEEEMMDo..",
    "..oMMEEEEEMDDo..",
    "...oMMEEEMMDo...",
    "....ooMMMDoo....",
    "......oooo......",
    "................",
]


def palette(light, mid, dark, extra=(255, 255, 255, 255)):
    return {"L": light, "M": mid, "D": dark, "E": extra}


def materials():
    icon("cinder_shard", SHARD, palette((255, 196, 96, 255), (232, 104, 36, 255), (150, 44, 22, 255)))
    icon("royal_amber", GEM, palette((255, 226, 128, 255), (226, 150, 40, 255), (150, 82, 18, 255)))
    icon("raw_grave_iron", NUGGET, palette((150, 160, 170, 255), (96, 104, 118, 255), (58, 62, 74, 255)))
    icon("grave_iron_ingot", INGOT, palette((176, 188, 204, 255), (112, 122, 140, 255), (66, 72, 88, 255)))
    icon("smoldering_ingot", VEINED_INGOT,
         palette((92, 80, 78, 255), (58, 48, 48, 255), (34, 28, 30, 255), (255, 128, 40, 255)))
    icon("smoldering_template", TEMPLATE,
         palette((96, 86, 84, 255), (62, 54, 54, 255), (40, 34, 36, 255), (240, 110, 36, 255)))


def boss_items():
    souls = {
        "ash_colossus_soul": ((214, 206, 196, 255), (150, 138, 128, 255), (88, 78, 72, 255), (255, 140, 50, 255)),
        "grieving_tree_soul": ((186, 214, 150, 255), (112, 146, 84, 255), (62, 84, 48, 255), (255, 210, 110, 255)),
        "crypt_keeper_soul": ((170, 200, 236, 255), (96, 124, 176, 255), (48, 64, 104, 255), (230, 244, 255, 255)),
        "smoldering_lord_soul": ((255, 150, 96, 255), (196, 54, 34, 255), (110, 22, 20, 255), (255, 226, 140, 255)),
    }
    for name, colours in souls.items():
        icon(name, SOUL, palette(*colours))
    icon("ash_of_rematch", ASH, palette((196, 190, 184, 255), (140, 134, 130, 255), (92, 88, 86, 255), (255, 120, 40, 255)))
    icon("flask_ember", EMBER, palette((255, 210, 110, 255), (246, 128, 40, 255), (170, 52, 24, 255), (255, 246, 210, 255)))


def effects():
    """18x18 status effect icon: a red drop."""
    c = Canvas(18, 18)
    drop = ["........o.........",
            ".......oLo........",
            ".......oLo........",
            "......oLLMo.......",
            "......oLMMo.......",
            ".....oLLMMMo......",
            ".....oLMMMMo......",
            "....oLLMMMMMo.....",
            "....oLMMMMMMo.....",
            "...oLMMMMMMMDo....",
            "...oLMMMMMMMDo....",
            "...oMMMMMMMDDo....",
            "...oMMMMMMDDDo....",
            "....oMMMMDDDo.....",
            ".....ooDDDoo......",
            ".......ooo........",
            "..................",
            ".................."]
    colours = {"L": (236, 96, 96, 255), "M": (176, 20, 24, 255), "D": (104, 8, 12, 255)}
    for y, row in enumerate(drop):
        for x, ch in enumerate(row):
            if ch != ".":
                c.set(x + 1, y + 1, OUTLINE if ch == "o" else colours[ch])
    c.save(os.path.join("src", "main", "resources", "assets", "embervoid", "textures", "mob_effect", "bleeding.png"))


if __name__ == "__main__":
    materials()
    boss_items()
    effects()
    print("Wrote reach gear placeholder textures")
