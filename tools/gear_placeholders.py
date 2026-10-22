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


HELMET = [
    "................",
    "................",
    "................",
    "....oooooooo....",
    "...oLLLLLLLMo...",
    "..oLLMMMMMMMDo..",
    "..oLMMMMMMMMDo..",
    "..oLMMMMMMMMDo..",
    "..oLMooooooDMo..",
    "..oLMo....oDMo..",
    "..oMDo....oDDo..",
    "...oo......oo...",
    "................",
    "................",
    "................",
    "................",
]

CHESTPLATE = [
    "................",
    "..ooo......ooo..",
    ".oLLLoooooooLMo.",
    ".oLMMLLLLLLMMDo.",
    ".oLMMMMEEMMMMDo.",
    "..ooLMMEEMMMoo..",
    "....oLMMMMDo....",
    "....oLMMMMDo....",
    "....oLMMMMDo....",
    "....oLMMMMDo....",
    "....oMMMMMDo....",
    "....oMMMMDDo....",
    "....oDDDDDDo....",
    ".....oooooo.....",
    "................",
    "................",
]

LEGGINGS = [
    "................",
    "................",
    "...oooooooooo...",
    "...oLLLLLLLMo...",
    "...oLMMEEMMDo...",
    "...oLMMooMMDo...",
    "...oLMDooLMDo...",
    "...oLMDooLMDo...",
    "...oLMDooLMDo...",
    "...oLMDooLMDo...",
    "...oLMDooLMDo...",
    "...oMDDooMDDo...",
    "...ooooooooooo..",
    "................",
    "................",
    "................",
]

BOOTS = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "...oooo..oooo...",
    "...oLMo..oLMo...",
    "...oLMo..oLMo...",
    "...oLMo..oLMo...",
    "...oLMo..oLMo...",
    "..oLMMo..oLMMo..",
    ".oLMMDo..oLMMDo.",
    ".oMDDDo..oMDDDo.",
    ".oooooo..oooooo.",
    "................",
    "................",
]


def armor(name, colours, rng_seed=1):
    """Item icons <name>_helmet|chestplate|leggings|boots and the worn layers."""
    for piece, rows in (("helmet", HELMET), ("chestplate", CHESTPLATE), ("leggings", LEGGINGS), ("boots", BOOTS)):
        icon(name + "_" + piece, rows, palette(*colours))
    layers(name, colours, rng_seed)


def layers(name, colours, rng_seed=1):
    """Worn armor layers (minecraft namespace, see GearArmorMaterial): a plain suit in the material's colours, one
    accent band. The game only draws the parts of the slots the material has pieces for."""
    import random
    light, mid, dark, accent = colours
    rng = random.Random(rng_seed)
    layer_dir = os.path.join("src", "main", "resources", "assets", "minecraft", "textures", "models", "armor")
    one = Canvas(64, 32)
    one.rect(0, 0, 32, 16, mid, 10, rng)  # head
    one.rect(8, 0, 16, 8, light, 10, rng)  # head top / front band
    one.rect(16, 16, 24, 16, mid, 10, rng)  # body
    one.rect(20, 20, 8, 3, accent)  # chest band
    one.rect(40, 16, 16, 16, dark, 10, rng)  # arms
    one.rect(0, 26, 16, 6, dark, 10, rng)  # boots: lower legs
    one.rect(8, 16, 4, 4, dark)  # boot soles
    one.save(os.path.join(layer_dir, "embervoid_" + name + "_layer_1.png"))
    two = Canvas(64, 32)
    two.rect(0, 16, 16, 16, mid, 10, rng)  # legs
    two.rect(16, 16, 24, 16, dark, 10, rng)  # hips
    two.rect(20, 26, 8, 2, accent)  # belt
    two.save(os.path.join(layer_dir, "embervoid_" + name + "_layer_2.png"))


def armor_sets():
    armor("smoldering", ((104, 92, 90, 255), (64, 54, 54, 255), (38, 32, 34, 255), (246, 112, 36, 255)))
    armor("reaper", ((170, 156, 128, 255), (118, 104, 82, 255), (72, 62, 50, 255), (196, 70, 40, 255)))
    armor("huntsman", ((122, 140, 90, 255), (84, 100, 62, 255), (52, 62, 40, 255), (214, 160, 70, 255)))
    armor("warden", ((150, 158, 176, 255), (98, 106, 124, 255), (58, 64, 80, 255), (180, 196, 230, 255)), 3)
    uniques = (
        ("scarecrow_mask", "scarecrow_mask", HELMET, ((214, 190, 120, 255), (160, 134, 74, 255), (96, 78, 44, 255), (40, 30, 24, 255))),
        ("huntsman_greaves", "huntsman_greaves", LEGGINGS, ((150, 116, 76, 255), (108, 80, 50, 255), (66, 48, 30, 255), (226, 186, 90, 255))),
        ("drowned_sabatons", "drowned_sabatons", BOOTS, ((110, 160, 150, 255), (64, 110, 104, 255), (34, 66, 64, 255), (170, 220, 210, 255))),
        ("furnace_heart_cuirass", "furnace_heart", CHESTPLATE, ((120, 80, 70, 255), (76, 46, 40, 255), (44, 26, 24, 255), (255, 150, 40, 255))),
    )
    for item, material, rows, colours in uniques:
        icon(item, rows, palette(*colours))
        layers(material, colours, 7)


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


def weapons():
    handle = (92, 60, 40, 255)
    p = palette((214, 214, 220, 255), (150, 150, 160, 255), (90, 90, 100, 255), (255, 120, 40, 255))
    p["W"] = handle
    icon("ashen_scythe", SCYTHE, dict(p, L=(196, 190, 184, 255), M=(140, 134, 130, 255), D=(92, 88, 86, 255)))
    icon("wardens_scourge", WHIP, dict(p, L=(150, 158, 176, 255), M=(98, 106, 124, 255), D=(58, 64, 80, 255),
                                       E=(140, 20, 24, 255), W=(60, 50, 44, 255)))
    icon("royal_ash_rapier", RAPIER, dict(p, E=(232, 180, 60, 255), W=(120, 30, 30, 255)))


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
    armor_sets()
    weapons()
    print("Wrote reach gear placeholder textures")
