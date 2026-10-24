#!/usr/bin/env python3
"""3D in-hand models of the weapons (PLAN 17.2, docs/art-direction.md): big element models drawn in a hand in place of
the 16x icons, which stay for the inventory, the ground and item frames.

Run from the repository root: python3 tools/weapon_model_art.py
Preview: python3 tools/model_preview.py blocks embervoid:item/ashen_scythe_in_hand ... (four angles of the model)

Each weapon is built in the icon's frame: the sprite's plane (z about 8), the grip at the bottom left where a vanilla
handheld icon has it, the business end up and to the right. Parts are authored along +Y from the grip and every
element is turned 45 degrees about Z around the grip, so the weapon lies on the icon's diagonal and vanilla's handheld
display transforms (third and first person) put the grip in the hand as they do the icon; only the weapon is about
twice as long as the icon's diagonal and has depth. Blade segments carry their own extra turn (the allowed 22.5-degree
steps), so a scythe curves.

Textures: one 64x64 sheet per weapon under textures/item/<weapon>_in_hand.png; every face gets its own region, painted
per material (grain along the haft, a lit edge on steel, bevelled bronze, wrapped leather) with the palette's hue
shift. The lash of the Warden's Scourge leaves its handle at the tip ring, the model point client code reads as ROOT (8.8, 8.8, 8).
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from art_kit import ROOT, CLEAR, Canvas, hash01, hexc, mix, ramp, value_noise, write_json  # noqa: E402

ITEM_TEX = os.path.join(ROOT, "textures", "item")
ITEM_MODELS = os.path.join(ROOT, "models", "item")
SHEET = 64
GRIP = (3.0, 3.0, 8.0)
# Vanilla refuses an element whose from/to leaves -16..32. A weapon longer than that is built at the first of SHRINKS it
# fits at, about the block's centre, and its display scales are divided by the same, which draws it exactly as large
# as authored.
BOUNDS = (-16, 32)
SHRINKS = (0.65, 0.6, 0.55, 0.5)
CENTRE = 8.0
# minecraft:item/handheld's display (with what it inherits from item/generated).
HANDHELD_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": 0.85},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": 0.85},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": 0.68},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": 0.68},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": 0.5},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": 1},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": 1},
    "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": 1},
}

# Materials: a ramp (dark to light) and how the face is painted.
DARK_WOOD = ramp("1f1612", "33241b", "4a3528", "63483a", "7c5e4c")
ASH_WOOD = ramp("2a2522", "3d3733", "55504a", "6e6861", "8a847c")
STEEL = ramp("3a3f4a", "5b6270", "848c99", "b3bac4", "e2e7ec")
BLACK_STEEL = ramp("16181d", "272a32", "3b404a", "565c68", "7a8290")
BRONZE = ramp("4a2e14", "7a4f22", "a8733a", "cf9a52", "ecc27a")
GOLD = ramp("5a3a10", "9a6a18", "d0a030", "f0c850", "fff0a0")
LEATHER = ramp("1e120c", "33201a", "4b2f25", "5f3d2f")
CRIMSON = ramp("3a0a12", "6a1420", "9a2030", "c43a44")
BONE = ramp("5a5244", "847a66", "aea48c", "d2c9ad", "ece5cf")
GRAVE_IRON = ramp("1c1b1f", "2e2d33", "45444b", "5e5d65", "7a7983")
EMBER = ramp("8a2a10", "d8521c", "ff8a2a", "ffd27a")


class Box:
    """A box of the weapon in its vertical frame (x, y, z in units, y up from the grip), a material and the extra turn
    (degrees about Z, -22.5 … 22.5 … on top of the diagonal's -45) and an optional own pivot for it."""

    def __init__(self, f, t, mat, turn=0, pivot=None, kind="plain", glow=None, name=None):
        self.f, self.t, self.mat, self.turn, self.pivot, self.kind, self.glow, self.name = f, t, mat, turn, pivot, kind, glow, name


FACES = ("north", "south", "east", "west", "up", "down")


def face_size(face, b):
    dx, dy, dz = b.t[0] - b.f[0], b.t[1] - b.f[1], b.t[2] - b.f[2]
    if face in ("north", "south"):
        return dx, dy
    if face in ("east", "west"):
        return dz, dy
    return dx, dz


class Sheet:
    """Packs face regions into the 64x64 texture, rows of equal height."""

    def __init__(self):
        self.canvas = Canvas(SHEET, SHEET)
        self.x = 0
        self.y = 0
        self.row_h = 0

    def alloc(self, w, h):
        w, h = max(1, int(round(w))), max(1, int(round(h)))
        if self.x + w > SHEET:
            self.x = 0
            self.y += self.row_h
            self.row_h = 0
        if self.y + h > SHEET:
            raise ValueError("texture sheet full")
        rect = (self.x, self.y, w, h)
        self.x += w
        self.row_h = max(self.row_h, h)
        return rect


def paint(canvas, rect, box, face, seed):
    """Paints one face region: the material's ramp with structure (grain, edge light, wrap), light from the top-left."""
    x0, y0, w, h = rect
    ramp_ = box.mat
    n = len(ramp_)
    long_axis_vertical = h >= w
    for y in range(h):
        for x in range(w):
            u = x / max(1, w - 1) if w > 1 else 0.5
            v = y / max(1, h - 1) if h > 1 else 0.5
            if box.kind == "wood":
                g = value_noise(x * 0.6 + (0 if long_axis_vertical else y * 3), y * 0.6 + (x * 3 if long_axis_vertical else 0), seed, 2.5)
                lv = 1.6 + g * 1.8 + (0.4 if (x + y * 7 + seed) % 11 == 0 else 0)
            elif box.kind == "steel":
                # The lit edge along the long axis's left/top, a dark edge on the other side, a fuller down the middle.
                t = u if long_axis_vertical else v
                lv = 2.2 + (1.6 if t < 0.2 else 0) - (1.2 if t > 0.8 else 0) + value_noise(x, y, seed, 3) * 0.6
                if 0.42 < t < 0.58 and (w if long_axis_vertical else h) >= 3:
                    lv -= 0.7
            elif box.kind == "wrap":
                stripe = ((y if long_axis_vertical else x) // 2) % 2
                lv = 1.2 + stripe * 1.3 + value_noise(x, y, seed, 2) * 0.5
            elif box.kind == "metal":
                lv = 1.8 + value_noise(x, y, seed, 2.5) * 1.2
                if x == 0 or y == 0:
                    lv += 1.4
                if x == w - 1 or y == h - 1:
                    lv -= 1.2
            elif box.kind == "ember":
                lv = 1.5 + value_noise(x, y, seed, 2) * 2.2
            else:
                lv = 1.5 + value_noise(x, y, seed, 2.5) * 1.5
            # Light from the top-left: lit on the top/left faces, darker on bottom/right ones.
            if face in ("east", "down"):
                lv -= 0.8
            elif face in ("up", "west"):
                lv += 0.4
            idx = max(0, min(n - 1, int(round(lv))))
            c = ramp_[idx]
            canvas.set(x0 + x, y0 + y, c)


def build(name, boxes, root=None):
    """Writes models/item/<name>_in_hand.json and its sheet; returns the model id."""
    sheet = Sheet()
    elements = []
    seed = sum(ord(ch) for ch in name)
    for i, box in enumerate(boxes):
        faces = {}
        for face in FACES:
            w, h = face_size(face, box)
            if w <= 0 or h <= 0:
                continue
            rect = sheet.alloc(w, h)
            paint(sheet.canvas, rect, box, face, seed + i * 7)
            x0, y0, rw, rh = rect
            # UV is in 16ths of the sheet whatever its size.
            k = 16 / SHEET
            faces[face] = {"uv": [x0 * k, y0 * k, (x0 + rw) * k, (y0 + rh) * k], "texture": "#0"}
        angle = -45 + box.turn
        if angle not in (-45, -22.5, 0, 22.5, 45):
            raise ValueError("element angle %s not allowed" % angle)
        # An element turns once, about one origin. A box with its own pivot is meant to turn by its extra turn about
        # that pivot and then with the weapon by -45 about the grip; that is the same as turning by the sum about the
        # pivot carried onto the diagonal, with the box moved along with its pivot.
        f, t, origin = list(box.f), list(box.t), list(GRIP)
        if box.pivot:
            rad = math.radians(-45)
            dx, dy = box.pivot[0] - GRIP[0], box.pivot[1] - GRIP[1]
            origin = [GRIP[0] + dx * math.cos(rad) - dy * math.sin(rad), GRIP[1] + dx * math.sin(rad) + dy * math.cos(rad),
                      box.pivot[2]]
            shift = [origin[i] - box.pivot[i] for i in range(3)]
            f = [round(f[i] + shift[i], 4) for i in range(3)]
            t = [round(t[i] + shift[i], 4) for i in range(3)]
            origin = [round(c, 4) for c in origin]
        element = {"from": f, "to": t, "faces": faces, "rotation": {"origin": origin, "axis": "z", "angle": angle}}
        if box.name:
            element["name"] = box.name
        elements.append(element)
    sheet.canvas.save(os.path.join(ITEM_TEX, "%s_in_hand.png" % name))
    # No parent: a chain that ends in builtin/generated (item/handheld does) bakes from layer0 and drops the elements,
    # so the model would draw nothing. The handheld display is written out instead.
    model = {
        "textures": {"0": "embervoid:item/%s_in_hand" % name, "particle": "embervoid:item/%s" % name},
        "elements": elements,
    }
    scale = 1.0
    fits = lambda els: all(BOUNDS[0] <= c <= BOUNDS[1] for e in els for c in e["from"] + e["to"])
    if not fits(elements):
        for scale in SHRINKS:
            shrink = lambda p: [round(CENTRE + (c - CENTRE) * scale, 4) for c in p]
            shrunk = [dict(e, **{"from": shrink(e["from"]), "to": shrink(e["to"]),
                                 "rotation": dict(e["rotation"], origin=shrink(e["rotation"]["origin"]))}) for e in elements]
            if fits(shrunk):
                elements[:] = shrunk
                break
        else:
            raise ValueError("%s does not fit the model bounds even at %s" % (name, SHRINKS[-1]))
    model["display"] = {k: {"rotation": v["rotation"], "translation": v["translation"],
                            "scale": [round(v["scale"] / scale, 4)] * 3} for k, v in HANDHELD_DISPLAY.items()}
    write_json(os.path.join(ITEM_MODELS, "%s_in_hand.json" % name), model)
    print("wrote %s_in_hand (%d elements)" % (name, len(elements)))


def along(y0, length, half_w, half_d=None, x=None, z=None):
    """A box along +Y from the grip line: from y0 to y0 + length, centred on the grip's x and z."""
    half_d = half_w if half_d is None else half_d
    cx = GRIP[0] if x is None else x
    cz = GRIP[2] if z is None else z
    return (cx - half_w, y0, cz - half_d), (cx + half_w, y0 + length, cz + half_d)


def ashen_scythe():
    """A long ash-wood snath with a leather grip and a bronze collar, a blackened steel blade off the top that curves
    down and out, its edge lit, a bone cap on the heel."""
    boxes = []
    f, t = along(-1, 34, 1.0)
    boxes.append(Box(f, t, ASH_WOOD, kind="wood", name="snath"))
    f, t = along(1, 6, 1.25)
    boxes.append(Box(f, t, LEATHER, kind="wrap", name="grip"))
    f, t = along(14, 5, 1.25)
    boxes.append(Box(f, t, LEATHER, kind="wrap", name="grip2"))
    f, t = along(32, 2.5, 1.6)
    boxes.append(Box(f, t, BRONZE, kind="metal", name="collar"))
    # The blade: segments off the snath's top, each starting where the last ended (in the vertical frame), turned
    # from 45 degrees up (on the icon: ahead) down to perpendicular, so it curves like a blade.
    import math
    top = 33.5
    p = (GRIP[0] + 1.0, top)
    segments = ((45, 9, 2.6, 1.4, STEEL, "blade1"), (22.5, 8, 2.2, 1.0, STEEL, "blade2"),
                (0, 7, 1.7, 0.6, STEEL, "blade3"), (0, 4, 1.1, 0.2, STEEL, "tip"))
    for turn, length, below, above, mat, name in segments:
        boxes.append(Box((p[0], p[1] - below, 7.65), (p[0] + length, p[1] + above, 8.35), mat, turn=turn, pivot=(p[0], p[1], 8),
                         kind="steel", name=name))
        rad = math.radians(turn)
        p = (p[0] + math.cos(rad) * (length - 0.4), p[1] + math.sin(rad) * (length - 0.4))
    x = GRIP[0] + 1.0
    boxes.append(Box((x - 1.5, top - 2.2, 7.2), (x + 2.5, top + 2.2, 8.8), BONE, kind="metal", name="heel"))
    build("ashen_scythe", boxes)


def royal_ash_rapier():
    """A long slim steel blade with a fuller, a gilded swept cup guard, a crimson-wrapped grip and a gold pommel."""
    boxes = []
    f, t = along(-2.5, 2.5, 1.5)
    boxes.append(Box(f, t, GOLD, kind="metal", name="pommel"))
    f, t = along(0, 6, 1.0)
    boxes.append(Box(f, t, CRIMSON, kind="wrap", name="grip"))
    f, t = along(6, 1.5, 3.2, 3.2)
    boxes.append(Box(f, t, GOLD, kind="metal", name="guard_plate"))
    f, t = along(7.5, 2.5, 2.4, 2.4)
    boxes.append(Box(f, t, GOLD, kind="metal", name="cup"))
    # Swept bars of the guard: thin boxes turned off the blade.
    gx, gy = GRIP[0], 6.5
    boxes.append(Box((gx, gy, 7.4), (gx + 5, gy + 1, 8.6), GOLD, turn=22.5, pivot=(gx, gy, 8), kind="metal", name="sweep1"))
    boxes.append(Box((gx - 5, gy, 7.4), (gx, gy + 1, 8.6), GOLD, turn=67.5, pivot=(gx, gy, 8), kind="metal", name="sweep2"))
    f, t = along(9, 28, 0.8, 0.5)
    boxes.append(Box(f, t, STEEL, kind="steel", name="blade"))
    f, t = along(37, 3, 0.45, 0.35)
    boxes.append(Box(f, t, STEEL, kind="steel", name="point"))
    build("royal_ash_rapier", boxes)


def sanctum_longsword():
    """A broad straight blade with a fuller, a bronze crossguard, a leather grip and a bronze pommel with a coal in it."""
    boxes = []
    f, t = along(-2, 2.2, 1.6)
    boxes.append(Box(f, t, BRONZE, kind="metal", name="pommel"))
    f, t = along(0, 7, 1.1)
    boxes.append(Box(f, t, LEATHER, kind="wrap", name="grip"))
    f, t = along(7, 2, 5.5, 1.3)
    boxes.append(Box(f, t, BRONZE, kind="metal", name="crossguard"))
    f, t = along(9, 24, 1.7, 0.6)
    boxes.append(Box(f, t, STEEL, kind="steel", name="blade"))
    f, t = along(33, 3.5, 1.0, 0.5)
    boxes.append(Box(f, t, STEEL, kind="steel", name="point"))
    build("sanctum_longsword", boxes)


def ashbearers_mace():
    """A dark wooden haft with a leather grip and an iron collar; a flanged iron head cracked with ember."""
    boxes = []
    f, t = along(-1, 24, 1.1)
    boxes.append(Box(f, t, DARK_WOOD, kind="wood", name="haft"))
    f, t = along(0, 6, 1.35)
    boxes.append(Box(f, t, LEATHER, kind="wrap", name="grip"))
    f, t = along(21, 2, 1.7)
    boxes.append(Box(f, t, GRAVE_IRON, kind="metal", name="collar"))
    f, t = along(23, 8, 2.6)
    boxes.append(Box(f, t, GRAVE_IRON, kind="metal", name="head"))
    f, t = along(24, 6, 1.4)
    boxes.append(Box(f, t, EMBER, kind="ember", name="core"))
    # Flanges out of the head on four sides.
    cx, cz, y0 = GRIP[0], GRIP[2], 23.5
    boxes.append(Box((cx + 2.4, y0, cz - 0.8), (cx + 4.6, y0 + 7, cz + 0.8), GRAVE_IRON, kind="metal", name="flange_e"))
    boxes.append(Box((cx - 4.6, y0, cz - 0.8), (cx - 2.4, y0 + 7, cz + 0.8), GRAVE_IRON, kind="metal", name="flange_w"))
    boxes.append(Box((cx - 0.8, y0, cz + 2.4), (cx + 0.8, y0 + 7, cz + 4.6), GRAVE_IRON, kind="metal", name="flange_s"))
    boxes.append(Box((cx - 0.8, y0, cz - 4.6), (cx + 0.8, y0 + 7, cz - 2.4), GRAVE_IRON, kind="metal", name="flange_n"))
    f, t = along(31, 2, 1.2)
    boxes.append(Box(f, t, GRAVE_IRON, kind="metal", name="spike"))
    build("ashbearers_mace", boxes)


def wardens_scourge():
    """The Scourge's handle: a grave-iron grip wrapped in leather, a heavy pommel, and the ring at the tip the lash
    leaves from. The tip ring sits at the icon's (9.5, 9.5): ScourgeLashes.ROOT."""
    boxes = []
    f, t = along(-1.5, 2, 1.6)
    boxes.append(Box(f, t, GRAVE_IRON, kind="metal", name="pommel"))
    f, t = along(0.5, 10, 1.15)
    boxes.append(Box(f, t, LEATHER, kind="wrap", name="grip"))
    f, t = along(1.5, 1, 1.35)
    boxes.append(Box(f, t, CRIMSON, kind="plain", name="band"))
    f, t = along(7.5, 1, 1.35)
    boxes.append(Box(f, t, CRIMSON, kind="plain", name="band2"))
    # The tip ring, centred 11.2 up the frame: on the icon's diagonal that is (8.8, 8.8), ScourgeLashes.ROOT.
    f, t = along(10.5, 1.4, 1.5, 0.7)
    boxes.append(Box(f, t, GRAVE_IRON, kind="metal", name="ring"))
    build("wardens_scourge", boxes)


def main():
    os.makedirs(ITEM_TEX, exist_ok=True)
    os.makedirs(ITEM_MODELS, exist_ok=True)
    ashen_scythe()
    royal_ash_rapier()
    sanctum_longsword()
    ashbearers_mace()
    wardens_scourge()


if __name__ == "__main__":
    main()
