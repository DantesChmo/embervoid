#!/usr/bin/env python3
"""Writes placeholder item textures for the Ashen Reach gear (PLAN milestone 11, track reach-gear).

The 16x16 ASCII silhouettes of the Ashen Reach gear (a letter is a palette colour, '.' is transparent) and the
Bleeding effect icon. Not run on its own: tools/reach_item_art.py runs these functions and restyles the output
(PLAN R40.2d); armor lives in tools/reach_armor_art.py.
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


SCYTHE = [
    "................",
    "..ooooooo.......",
    ".oLLLLLLMoo.....",
    "..ooooooMMDo....",
    "........oDDo....",
    ".......oWo.oo...",
    "......oWo.......",
    ".....oWo........",
    "....oWo.........",
    "...oWo..........",
    "..oWo...........",
    ".oWo............",
    "oWo.............",
    "oo..............",
    "................",
    "................",
]

WHIP = [
    "................",
    "..........ooo...",
    ".........oLLMo..",
    "........oLo.oDo.",
    ".......oLo...oMo",
    "......oLo....oMo",
    ".....oMo....oDo.",
    "....oMo....oMo..",
    "...ooo....oMo...",
    "..oWWo...oDo....",
    ".oWWo...oEo.....",
    "oWWo....oEo.....",
    "oWo......o......",
    ".o..............",
    "................",
    "................",
]

RAPIER = [
    "..............o.",
    ".............oLo",
    "............oLo.",
    "...........oLo..",
    "..........oLo...",
    ".........oLo....",
    "........oMo.....",
    ".......oMo......",
    "...oo.oMo.......",
    "...oEoMo........",
    "....oEo.........",
    "...oWoEo........",
    "..oWo.oo........",
    ".oWo............",
    ".oo.............",
    "................",
]


def line(c, x0, y0, x1, y1, color):
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        c.set(x0, y0, color)
        if x0 == x1 and y0 == y1:
            return
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def crossbow():
    """Huntsman's Crossbow, pointing up-right like vanilla's sprite: standby, three pulling stages, loaded with an
    arrow or a firework."""
    wood, dark_wood = (120, 84, 50, 255), (78, 52, 30, 255)
    limb, string = (84, 100, 62, 255), (230, 226, 210, 255)
    nock = {"standby": (10, 5), "pulling_0": (9, 6), "pulling_1": (8, 7), "pulling_2": (7, 8), "arrow": (7, 8),
            "firework": (7, 8)}
    for state, (sx, sy) in nock.items():
        c = Canvas(16, 16)
        line(c, 2, 13, 11, 4, wood)
        line(c, 3, 13, 11, 5, dark_wood)
        line(c, 7, 1, 14, 8, limb)
        line(c, 8, 1, 14, 7, limb)
        line(c, 7, 1, sx, sy, string)
        line(c, 14, 8, sx, sy, string)
        if state == "arrow":
            line(c, sx, sy, 13, 2, (170, 170, 176, 255))
            c.set(13, 2, (90, 90, 96, 255))
        if state == "firework":
            line(c, sx, sy, 12, 3, (196, 40, 40, 255))
            c.set(12, 3, (240, 220, 120, 255))
        c.save(os.path.join(ITEM_DIR, "huntsmans_crossbow_" + state + ".png"))


BOMB = [
    "................",
    "..........o.....",
    ".........oEo....",
    "........oEo.....",
    "......ooWoo.....",
    ".....oLLLMMo....",
    "....oLLMMMMDo...",
    "...oLLMMMMMMDo..",
    "...oLMMMMMMMDo..",
    "...oMMMMMMMDDo..",
    "...oMMMMMMDDDo..",
    "....oMMMMDDDo...",
    ".....oDDDDDo....",
    "......ooooo.....",
    "................",
    "................",
]

JAR = [
    "................",
    "................",
    "......oooo......",
    "......oWWo......",
    "......oooo......",
    ".....oLLLMo.....",
    "....oLEEEEMo....",
    "....oLEEEEMo....",
    "....oLEEEEMo....",
    "....oLEEEEMo....",
    "....oLEEEEDo....",
    "....oMEEEEDo....",
    ".....oDDDDo.....",
    "......oooo......",
    "................",
    "................",
]

BOTTLE = [
    "................",
    "......ooo.......",
    "......oWo.......",
    "......oWo.......",
    ".....oLLMo......",
    "....oLLLLMo.....",
    "...oLEEEEEMo....",
    "...oLEEEEEDo....",
    "...oLEEEEEDo....",
    "...oMEEEEEDo....",
    "....oEEEEDo.....",
    ".....ooooo......",
    "................",
    "................",
    "................",
    "................",
]

STONE = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "...........oo...",
    ".........ooLMo..",
    ".......ooLLMMo..",
    ".....ooLLMMMDo..",
    "...ooLLMMMMDo...",
    "..oLLMMMMMDo....",
    "..oMMMMMDDo.....",
    "...oDDDDoo......",
    "....oooo........",
    "................",
    "................",
]


def consumables():
    glass = (200, 220, 230, 255)
    icon("ash_bomb", BOMB, dict(palette((150, 144, 140, 255), (96, 90, 88, 255), (56, 52, 52, 255), (255, 150, 40, 255)),
                                W=(120, 84, 50, 255)))
    icon("smoldering_resin", JAR, dict(palette(glass, (160, 180, 190, 255), (110, 124, 134, 255), (190, 70, 30, 255)),
                                       W=(120, 84, 50, 255)))
    icon("amber_balm", BOTTLE, dict(palette(glass, (160, 180, 190, 255), (110, 124, 134, 255), (236, 160, 50, 255)),
                                    W=(150, 110, 70, 255)))
    icon("amber_lantern_oil", BOTTLE, dict(palette(glass, (160, 180, 190, 255), (110, 124, 134, 255), (190, 120, 30, 255)),
                                           W=(60, 50, 44, 255)))
    icon("whetstone", STONE, palette((176, 188, 204, 255), (112, 122, 140, 255), (66, 72, 88, 255)))


def weapons():
    handle = (92, 60, 40, 255)
    p = palette((214, 214, 220, 255), (150, 150, 160, 255), (90, 90, 100, 255), (255, 120, 40, 255))
    p["W"] = handle
    icon("ashen_scythe", SCYTHE, dict(p, L=(196, 190, 184, 255), M=(140, 134, 130, 255), D=(92, 88, 86, 255)))
    icon("wardens_scourge", WHIP, dict(p, L=(150, 158, 176, 255), M=(98, 106, 124, 255), D=(58, 64, 80, 255),
                                       E=(140, 20, 24, 255), W=(60, 50, 44, 255)))
    icon("royal_ash_rapier", RAPIER, dict(p, E=(232, 180, 60, 255), W=(120, 30, 30, 255)))
    crossbow()


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
    # The silhouettes here are restyled by tools/reach_item_art.py, which runs them; armor is in
    # tools/reach_armor_art.py. Run those instead, so the final art is not replaced by the raw sketches.
    print("Run python3 tools/reach_item_art.py (items) and python3 tools/reach_armor_art.py (armor).")
