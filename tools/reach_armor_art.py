#!/usr/bin/env python3
"""Final armor for Realm 1 (PLAN R40.2d): the worn layers and the 20 armor item icons.

Run from the repository root: python3 tools/reach_armor_art.py [material ...]
Worn layers go to assets/minecraft/textures/models/armor/embervoid_<material>_layer_1|2.png (1.20.1 builds that
path from GearArmorMaterial.getName()), icons to assets/embervoid/textures/item/. The layers are painted on the
vanilla armor model's box-UV regions with the same materials and decals as the mobs (tools/art_kit.py): head 8³ at
(0,0), body 8x12x4 at (16,16), arm 4x12x4 at (40,16), leg 4x12x4 at (0,16), the left limbs mirrored. Layer 1 is
worn inflated by 1.0 (helmet, chestplate with arms, boots on the lower legs), layer 2 by 0.5 (leggings: hips and
legs). Like vanilla, a piece covers only part of its region (boots the feet, a chest its plates); the rest is clear.
Preview with python3 tools/model_preview.py armor.
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from art_kit import CLEAR, Box, Canvas, Face, Material, clamp, ember_cracks, hexc, ramp, scatter  # noqa: E402
from reach_item_art import icon  # noqa: E402
from reach_mob_art import (BLACK_STEEL, BRONZE, CAST, CLOTH_GREEN, CRIMSON, EMBER, EMBER_DEEP, EMBER_HOT, GOLD,  # noqa: E402
                           IRON, LEATHER, LINEN, RUST, SLATE, STRAW, ROPE, VOID, band, holes, mail, ragged_edge,
                           rivets, stitch, tatter, trim)

LAYER_DIR = os.path.join("src", "main", "resources", "assets", "minecraft", "textures", "models", "armor")
REGIONS = {"head": ((0, 0), (8, 8, 8)), "body": ((16, 16), (8, 12, 4)), "arm": ((40, 16), (4, 12, 4)),
           "leg": ((0, 16), (4, 12, 4))}


class Layer:
    def __init__(self, seed):
        self.tex, self.glow = Canvas(64, 32), Canvas(64, 32)
        self.rng = random.Random(seed)

    def part(self, region, mat, decal=None, rows=None, faces=("front", "back", "left", "right", "top", "bottom")):
        """Paints one region. rows: (first, last) rows of the side faces to paint (top rows of a head face are its
        crown); decal(face, rng) runs after the fill."""
        (u, v), size = REGIONS[region]
        box = Box([0, 0, 0], list(size), mat)
        box.uv = (u, v)
        for name, rect in box.faces().items():
            if name not in faces or rect[2] <= 0 or rect[3] <= 0:
                continue
            face = Face(self.tex, self.glow, name, rect, box)
            mat.paint(face, self.rng, u * 7 + v * 13 + len(name))
            if decal:
                decal(face, self.rng)
            if rows and face.side():
                for y in range(face.h):
                    if not rows[0] <= y <= rows[1]:
                        for x in range(face.w):
                            face.clear(x, y)

    def save(self, material, n):
        self.tex.save(os.path.join(LAYER_DIR, "embervoid_%s_layer_%d.png" % (material, n)))


def plates(face, rng, color, step=4):
    if face.side():
        for y in range(step - 1, face.h, step):
            face.hline(0, y, face.w, color)


# --- Reaper's Garb (Fields): a reaper's linen and burlap, light ---------------------------------------------------

def reaper():
    linen = Material(LINEN, base=2.8, noise=0.6, grain="v", edge=0.6)
    burlap = Material(STRAW, base=2.2, noise=0.8, grain="h", edge=0.6)
    one = Layer("reaper1")

    def hood(face, rng):
        if face.name == "front":
            for y in range(2, face.h):
                for x in range(1, face.w - 1):
                    face.clear(x, y)
        if face.side():
            ragged_edge(face, rng, "bottom", 2, 0.5)
            stitch(face, 1, ROPE[1], 3)
        if face.name == "bottom":
            for y in range(face.h):
                for x in range(face.w):
                    face.clear(x, y)

    one.part("head", burlap, hood)

    def smock(face, rng):
        if face.name == "front":
            face.vline(face.cx, 0, 4, LINEN[0])  # laced collar
            for y in (1, 3):
                face.set(face.cx - 1, y, ROPE[2])
                face.set(face.cx + 1, y, ROPE[2])
        if face.side():
            band(face, 8, ROPE[2])
            band(face, 9, ROPE[0])
            face.set(1, 8, CRIMSON[3]) if face.name == "front" else None  # a red harvest token
            holes(face, rng, 1)
            ragged_edge(face, rng, "bottom", 1, 0.4)

    one.part("body", linen, smock, faces=("front", "back", "left", "right", "top"))

    def sleeve(face, rng):
        if face.side():
            ragged_edge(face, rng, "bottom", 2, 0.6)

    one.part("arm", linen.but(base=2.4), sleeve, rows=(0, 7), faces=("front", "back", "left", "right", "top"))

    def wraps(face, rng):
        if face.side():
            for y in range(face.h):
                if y % 2 == 0:
                    face.hline(0, y, face.w, LINEN[1])
            face.hline(0, face.h - 1, face.w, LEATHER[1])

    one.part("leg", linen.but(base=2.0), wraps, rows=(8, 11), faces=("front", "back", "left", "right", "bottom"))
    one.save("reaper", 1)
    two = Layer("reaper2")

    def trousers(face, rng):
        if face.side():
            if rng.random() < 0.6:
                face.rect(rng.randrange(face.w), rng.randrange(face.h - 3), 2, 2, STRAW[3])  # patches
            stitch(face, 0, ROPE[1], 2)

    two.part("leg", burlap.but(base=1.8), trousers, rows=(0, 9), faces=("front", "back", "left", "right", "top"))
    two.part("body", burlap.but(base=1.8), lambda f, r: band(f, 8, ROPE[2]), rows=(8, 11),
             faces=("front", "back", "left", "right"))
    two.save("reaper", 2)


# --- Huntsman's Leathers (Woods): studded leather and hunting green -----------------------------------------------

def huntsman():
    leather = Material(LEATHER, base=3.0, noise=0.6, grain="v", edge=0.8)
    green = Material(CLOTH_GREEN, base=2.6, noise=0.6, grain="v", edge=0.6)
    one = Layer("huntsman1")

    def cap(face, rng):
        if face.side():
            for y in range(4, face.h):
                for x in range(face.w):
                    if face.name != "back" or y > 5:
                        face.clear(x, y)
            band(face, 3, LEATHER[1])
        if face.name == "right":  # a pheasant feather in the band
            for k, c in enumerate((hexc("b8463a"), hexc("d89a4a"), hexc("e8dcc0"))):
                face.set(face.w - 2 - k, 1 + k, c)
                face.set(face.w - 2 - k, 2 + k, c)
        if face.name == "bottom":
            for y in range(face.h):
                for x in range(face.w):
                    face.clear(x, y)

    one.part("head", green, cap)

    def jerkin(face, rng):
        if face.side():
            rivets(face, BRONZE[4], 3)
            band(face, 9, LEATHER[0])
        if face.name == "front":
            for y in range(face.h):  # the quiver strap across the chest
                x = face.w - 1 - y * face.w // face.h
                face.set(x, y, LEATHER[1])
                face.set(x - 1, y, LEATHER[0])
            face.set(2, 9, BRONZE[4])
        if face.name == "back":
            for y in range(face.h):
                face.set(y * face.w // face.h, y, LEATHER[1])

    one.part("body", leather, jerkin, faces=("front", "back", "left", "right", "top"))

    def bracer(face, rng):
        if face.side():
            for y in (8, 10):
                face.hline(0, y, face.w, LEATHER[1])

    one.part("arm", green.but(base=2.2), lambda f, r: (bracer(f, r), plates(f, r, CLOTH_GREEN[0], 5)),
             rows=(0, 11), faces=("front", "back", "left", "right", "top"))

    def boot(face, rng):
        if face.side():
            face.hline(0, 6, face.w, LEATHER[5])   # folded cuff
            face.hline(0, 7, face.w, LEATHER[1])
            face.hline(0, face.h - 1, face.w, LEATHER[0])

    one.part("leg", leather.but(base=2.4), boot, rows=(6, 11), faces=("front", "back", "left", "right", "bottom"))
    one.save("huntsman", 1)
    two = Layer("huntsman2")

    def trousers(face, rng):
        if face.side():
            if face.name == "front":
                face.rect(0, 5, face.w, 2, LEATHER[3])   # knee pad
                face.hline(0, 4, face.w, LEATHER[1])
            stitch(face, 0, LEATHER[1], 2)

    two.part("leg", green.but(base=2.0), trousers, rows=(0, 9), faces=("front", "back", "left", "right", "top"))
    two.part("body", leather.but(base=2.2), lambda f, r: (band(f, 9, LEATHER[0]),
                                                           f.name == "front" and f.set(f.cx, 9, BRONZE[4])),
             rows=(8, 11), faces=("front", "back", "left", "right"))
    two.save("huntsman", 2)


# --- Necropolis Warden: old plate, mail and the slate tabard ---------------------------------------------------------

def warden():
    plate = Material(IRON, base=3.2, noise=0.5, speck=0.05, edge=0.9, grad=1.0, top=0.8)
    one = Layer("warden1")

    def helm(face, rng):
        if face.name == "front":
            face.hline(1, 3, face.w - 2, VOID)
            face.vline(face.cx - 1, 4, 3, VOID)
            for x in range(1, face.w - 1):
                face.clear(x, 3)
            for y in (4, 5, 6):
                face.clear(face.cx - 1, y)
            rivets(face, IRON[5], 3)
        if face.side():
            band(face, face.h - 1, BRONZE[3])
            scatter(face, rng, 2, RUST[1])
        if face.name == "top":
            face.vline(face.cx - 1, 0, face.h, IRON[5])
        if face.name == "bottom":
            for y in range(face.h):
                for x in range(face.w):
                    face.clear(x, y)

    one.part("head", plate, helm)

    def cuirass(face, rng):
        if face.side():
            plates(face, rng, IRON[1], 4)
            scatter(face, rng, 2, RUST[1])
        if face.name == "front":
            face.rect(2, 4, 4, 8, SLATE[3])        # the tabard over the plate
            trim(Face(face.tex, face.glow, face.name, (face.x0 + 2, face.y0 + 4, 4, 8), face.box), BRONZE[3],
                 ("left", "right"))
            face.set(face.cx - 1, 6, BRONZE[4])
            face.set(face.cx, 6, BRONZE[4])

    one.part("body", plate, cuirass, faces=("front", "back", "left", "right", "top"))

    def pauldron(face, rng):
        if face.side():
            plates(face, rng, IRON[1], 3)
            band(face, 0, IRON[5])

    one.part("arm", plate.but(base=3.0), pauldron, rows=(0, 5), faces=("front", "back", "left", "right", "top"))
    one.part("leg", plate.but(base=2.6), lambda f, r: (band(f, 7, IRON[5]), rivets(f, IRON[5], 2)), rows=(7, 11),
             faces=("front", "back", "left", "right", "bottom"))
    one.save("warden", 1)
    two = Layer("warden2")

    def chausses(face, rng):
        if face.side():
            mail(face, rng, IRON[1], IRON[4])
            if face.name == "front":
                face.rect(0, 4, face.w, 2, IRON[4])   # knee cop
                face.hline(0, 3, face.w, IRON[2])

    two.part("leg", Material(IRON, base=2.4, noise=0.4), chausses, rows=(0, 9),
             faces=("front", "back", "left", "right", "top"))
    two.part("body", Material(IRON, base=2.4, noise=0.4), lambda f, r: (mail(f, r, IRON[1], IRON[4]),
                                                                       band(f, 8, LEATHER[1])),
             rows=(8, 11), faces=("front", "back", "left", "right"))
    two.save("warden", 2)


# --- Smoldering Plate (crafted): black steel, gold trim, ember seams -------------------------------------------------

def smoldering():
    steel = Material(BLACK_STEEL, base=3.2, noise=0.5, edge=0.9, grad=1.0, top=1.0)
    one = Layer("smoldering1")

    def seams(face, rng):
        if face.side():
            for y in range(3, face.h, 4):
                face.hline(0, y, face.w, BLACK_STEEL[0])
                for x in range(face.w):
                    if rng.random() < 0.25:
                        face.set(x, y, EMBER_DEEP)
            if rng.random() < 0.5:
                ember_cracks(face, rng, 1, EMBER, EMBER_DEEP, 0.0, (2, 3))

    def helm(face, rng):
        seams(face, rng)
        if face.name == "front":
            face.hline(1, 3, face.w - 2, VOID)
            for x in range(1, face.w - 1):
                face.clear(x, 3)
            face.set(2, 3, EMBER_HOT)
            face.set(face.w - 3, 3, EMBER_HOT)
        if face.side():
            band(face, 0, GOLD[2])
        if face.name == "bottom":
            for y in range(face.h):
                for x in range(face.w):
                    face.clear(x, y)

    one.part("head", steel, helm)

    def chest(face, rng):
        seams(face, rng)
        if face.side():
            band(face, 0, GOLD[2])
        if face.name == "front":
            face.set(face.cx - 1, 5, EMBER_HOT)
            face.set(face.cx, 5, EMBER)
            face.set(face.cx - 1, 4, GOLD[4])
            face.set(face.cx, 6, GOLD[4])

    one.part("body", steel, chest, faces=("front", "back", "left", "right", "top"))
    one.part("arm", steel.but(base=3.0), lambda f, r: (seams(f, r), band(f, 0, GOLD[2])), rows=(0, 11),
             faces=("front", "back", "left", "right", "top"))
    one.part("leg", steel.but(base=2.8), lambda f, r: (seams(f, r), band(f, 6, GOLD[2])), rows=(6, 11),
             faces=("front", "back", "left", "right", "bottom"))
    one.save("smoldering", 1)
    two = Layer("smoldering2")
    two.part("leg", steel.but(base=2.6), lambda f, r: (seams(f, r), f.name == "front" and f.rect(0, 4, f.w, 2, BLACK_STEEL[5])),
             rows=(0, 9), faces=("front", "back", "left", "right", "top"))
    two.part("body", steel.but(base=2.4), lambda f, r: (band(f, 8, GOLD[2]), seams(f, r)), rows=(8, 11),
             faces=("front", "back", "left", "right"))
    two.save("smoldering", 2)


# --- Uniques ---------------------------------------------------------------------------------------------------------

def scarecrow_mask():
    """A burlap sack over the head, button eyes, a stitched grin, straw poking out under it."""
    sack = Material(STRAW, base=2.6, noise=0.8, grain="h", edge=0.5)
    one = Layer("scarecrow")

    def mask(face, rng):
        if face.name == "front":
            for ex in (1, 5):
                face.rect(ex, 2, 2, 2, hexc("2a2018"))
                face.set(ex, 2, hexc("6b553a"))
            for x in range(1, 7):
                face.set(x, 5 + (x % 2), hexc("2a2018"))
        if face.side():
            band(face, face.h - 2, ROPE[1])
            for x in range(0, face.w, 2):
                face.set(x, face.h - 1, STRAW[5] if rng.random() < 0.5 else STRAW[4])
        if face.name == "top":
            face.set(face.cx, face.cx, ROPE[0])
        if face.name == "bottom":
            for y in range(face.h):
                for x in range(face.w):
                    face.clear(x, y)

    one.part("head", sack, mask)
    one.save("scarecrow_mask", 1)
    Layer("scarecrow2").save("scarecrow_mask", 2)


def huntsman_greaves():
    """Fine hunting greaves: dyed leather with fur at the top, laced shins."""
    leather = Material(LEATHER, base=3.2, noise=0.5, grain="v", edge=0.8)
    two = Layer("greaves")

    def greave(face, rng):
        if face.side():
            face.rect(0, 0, face.w, 2, hexc("8c7a62"))   # fur trim
            for x in range(face.w):
                if rng.random() < 0.5:
                    face.set(x, 2, hexc("6e5e4a"))
            if face.name == "front":
                for y in range(4, face.h, 2):
                    face.set(1, y, ROPE[3])
                    face.set(2, y + 1, ROPE[3])
            face.hline(0, 6, face.w, GOLD[2])

    two.part("leg", leather, greave, rows=(0, 9), faces=("front", "back", "left", "right", "top"))
    two.part("body", leather.but(base=2.6), lambda f, r: band(f, 9, GOLD[2]), rows=(8, 11),
             faces=("front", "back", "left", "right"))
    two.save("huntsman_greaves", 2)
    Layer("greaves1").save("huntsman_greaves", 1)


DROWNED = ramp("1f2e2c", "2b4140", "385653", "4a6c66", "5f857c", "7fa395")


def drowned_sabatons():
    """Sabatons of a drowned knight: corroded plate gone green, barnacles, a strand of weed."""
    bronze = Material(DROWNED, base=3.0, noise=0.7, speck=0.1, edge=0.8)
    one = Layer("sabatons")

    def sabaton(face, rng):
        if face.side():
            plates(face, rng, DROWNED[0], 2)
            scatter(face, rng, 2, hexc("c8c2b0"))     # barnacles
            if rng.random() < 0.6:
                face.vline(rng.randrange(face.w), 0, 3, hexc("3e5a2a"))

    one.part("leg", bronze, sabaton, rows=(7, 11), faces=("front", "back", "left", "right", "bottom"))
    one.save("drowned_sabatons", 1)
    Layer("sabatons2").save("drowned_sabatons", 2)


def furnace_heart():
    """The Furnace Heart Cuirass: a cast-iron breast with a glowing grate where the heart is."""
    cast = Material(CAST, base=3.2, noise=0.5, edge=1.0, grad=0.8)
    one = Layer("furnace")

    def cuirass(face, rng):
        if face.side():
            rivets(face, CAST[5], 2)
        if face.name == "front":
            face.rect(2, 3, 4, 4, CAST[1])
            for x in (2, 4):
                for y in range(3, 7):
                    face.set(x, y, EMBER if y % 2 else EMBER_HOT)
            face.hline(1, 2, 6, CAST[5])
            face.hline(1, 7, 6, CAST[5])

    one.part("body", cast, cuirass, faces=("front", "back", "left", "right", "top"))
    one.part("arm", cast.but(base=3.0), lambda f, r: (rivets(f, CAST[5], 2), band(f, 4, CAST[0])), rows=(0, 4),
             faces=("front", "back", "left", "right", "top"))
    one.save("furnace_heart", 1)
    Layer("furnace2").save("furnace_heart", 2)


# --- Item icons ------------------------------------------------------------------------------------------------------
# One silhouette per slot (like vanilla), letters L/M/D light-mid-dark of the material, A/a an accent; the uniques
# draw their own.

HELMET = [
    "................",
    "................",
    "................",
    ".....oooooo.....",
    "....oLLLLMMo....",
    "...oLMMAMMMDo...",
    "..oLMMMAMMMMDo..",
    "..oLMMMMMMMMDo..",
    "..oLMMooooMMDo..",
    "..oLMo....oMDo..",
    "..oMDo....oDDo..",
    "...oo......oo...",
    "................",
    "................",
    "................",
    "................",
]

CHESTPLATE = [
    "................",
    "..oooo....oooo..",
    ".oLLMMooooMMMDo.",
    ".oLMMMLLLLMMMDo.",
    ".oLMMMMAAMMMMDo.",
    "..ooLMMAAMMDoo..",
    "....oLMMMMDo....",
    "....oLMMMMDo....",
    "....oLaaaaDo....",
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
    "...oLaaaaaaDo...",
    "...oLMMMMMMDo...",
    "...oLMMooMMDo...",
    "...oLMDooLMDo...",
    "...oLMDooLMDo...",
    "...oLADooLADo...",
    "...oLMDooLMDo...",
    "...oLMDooLMDo...",
    "...oMDDooMDDo...",
    "...oooo..oooo...",
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
    "...oAao..oAao...",
    "...oLMo..oLMo...",
    "...oLMo..oLMo...",
    "..oLMMo..oLMMo..",
    ".oLMMDo..oLMMDo.",
    ".oMDDDo..oMDDDo.",
    ".oooooo..oooooo.",
    "................",
    "................",
]

SCARECROW_MASK = [
    "................",
    "................",
    ".......oo.......",
    ".....ooLLoo.....",
    "....oLLLLLMo....",
    "...oLMMMMMMMo...",
    "...oLBbMMBbDo...",
    "...oLMMMMMMDo...",
    "...oLMSMSMSDo...",
    "...oMMMSMSMDo...",
    "...oRRRRRRRRo...",
    "..oYyYoYyoyYYo..",
    "...oo.o..o.oo...",
    "................",
    "................",
    "................",
]

FURNACE_CUIRASS = [
    "................",
    "..oooo....oooo..",
    ".oLLMMooooMMMDo.",
    ".oLMMMLLLLMMMDo.",
    ".oLMMoooooMMMDo.",
    "..ooLoHEHoMDoo..",
    "....oLEHEoDo....",
    "....oLoooooo....",
    "....oLMMMMDo....",
    "....oLrMMrDo....",
    "....oMMMMMDo....",
    "....oMMMMDDo....",
    "....oDDDDDDo....",
    ".....oooooo.....",
    "................",
    "................",
]


def armor_icons():
    sets = {
        "reaper": (LINEN[5], LINEN[3], LINEN[1], CRIMSON[3], ROPE[2]),
        "huntsman": (CLOTH_GREEN[4], LEATHER[3], LEATHER[1], BRONZE[4], LEATHER[0]),
        "warden": (IRON[6], IRON[4], IRON[2], SLATE[4], BRONZE[3]),
        "smoldering": (BLACK_STEEL[5], BLACK_STEEL[3], BLACK_STEEL[1], EMBER, GOLD[3]),
    }
    for name, (light, mid, dark, accent, accent2) in sets.items():
        pal = {"L": light, "M": mid, "D": dark, "A": accent, "a": accent2}
        for piece, rows in (("helmet", HELMET), ("chestplate", CHESTPLATE), ("leggings", LEGGINGS),
                            ("boots", BOOTS)):
            icon(name + "_" + piece, rows, pal)
    icon("scarecrow_mask", SCARECROW_MASK, {
        "L": STRAW[5], "M": STRAW[3], "D": STRAW[1], "B": hexc("2a2018"), "b": hexc("6b553a"),
        "S": hexc("3a2c1e"), "R": ROPE[1], "Y": STRAW[5], "y": STRAW[3]})
    icon("huntsman_greaves", LEGGINGS, {"L": LEATHER[5], "M": LEATHER[3], "D": LEATHER[1], "A": GOLD[3],
                                        "a": hexc("8c7a62")})
    icon("drowned_sabatons", BOOTS, {"L": DROWNED[5], "M": DROWNED[3], "D": DROWNED[1], "A": hexc("c8c2b0"),
                                     "a": DROWNED[0]})
    icon("furnace_heart_cuirass", FURNACE_CUIRASS, {"L": CAST[5], "M": CAST[3], "D": CAST[1], "H": EMBER_HOT,
                                                    "E": EMBER, "r": CAST[5]})


LAYERS = [("reaper", reaper), ("huntsman", huntsman), ("warden", warden), ("smoldering", smoldering),
          ("scarecrow_mask", scarecrow_mask), ("huntsman_greaves", huntsman_greaves),
          ("drowned_sabatons", drowned_sabatons), ("furnace_heart", furnace_heart)]


if __name__ == "__main__":
    wanted = set(sys.argv[1:])
    for name, build in LAYERS:
        if not wanted or name in wanted:
            build()
            print("wrote layers", name)
    if not wanted or "icons" in wanted:
        armor_icons()
