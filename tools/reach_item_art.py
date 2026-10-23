#!/usr/bin/env python3
"""Final 16x item textures for Realm 1 (PLAN R40.2c-d): materials, boss items, consumables, weapons, artifacts and
rings, and the thrown fire flask. Armor icons come from tools/reach_armor_art.py.

Run from the repository root: python3 tools/reach_item_art.py
Two kinds of icon, one style:
- sketches drawn here (icon(), shaded_icon()): a letter is a palette colour or a shaded region, '.' is transparent, 'o'
  is the outline;
- the silhouettes of the gear and reliquary placeholder scripts, which this script runs first and then restyles
  in place (restyle()).
In both, like vanilla items, the outline is not one black: each outline pixel is a dark shade of the colour it
borders, so a bottle's rim reads as glass and a bomb's as iron; light comes from the top left, with a one-pixel
bevel (lighter on the top-left inner edge, darker on the bottom-right). Preview with
    python3 tools/model_preview.py items <name ...>
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from art_kit import Canvas, clamp, hexc, ramp  # noqa: E402

ITEM_DIR = os.path.join("src", "main", "resources", "assets", "embervoid", "textures", "item")


def darker(c, k=0.45):
    return (clamp(c[0] * k), clamp(c[1] * k), clamp(c[2] * k), 255)


def bevel(c, amount=16):
    """Lighter top-left inner edge, darker bottom-right inner edge, for every opaque non-outline pixel."""
    w, h = c.width, c.height
    solid = [[c.get(x, y)[3] > 0 for x in range(w)] for y in range(h)]
    edge = [[False] * w for _ in range(h)]
    for y in range(h):
        for x in range(w):
            if solid[y][x]:
                edge[y][x] = any(not (0 <= x + dx < w and 0 <= y + dy < h and solid[y + dy][x + dx])
                                 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
    out = {}
    for y in range(h):
        for x in range(w):
            if not solid[y][x] or edge[y][x]:
                continue
            up = (y > 0 and edge[y - 1][x]) or (x > 0 and edge[y][x - 1])
            down = (y < h - 1 and edge[y + 1][x]) or (x < w - 1 and edge[y][x + 1])
            if up and not down:
                out[(x, y)] = amount
            elif down and not up:
                out[(x, y)] = -amount
    for (x, y), d in out.items():
        r, g, b, a = c.get(x, y)
        c.set(x, y, (clamp(r + d), clamp(g + d), clamp(b + d), a))


def recolor_outline(c, is_outline, k=0.45):
    """Outline pixels take a dark shade of the darkest colour they border."""
    w, h = c.width, c.height
    src = [[c.get(x, y) for x in range(w)] for y in range(h)]
    for y in range(h):
        for x in range(w):
            if not is_outline(src[y][x]):
                continue
            near = [src[y + dy][x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1),
                                                       (-1, 1))
                    if 0 <= x + dx < w and 0 <= y + dy < h and src[y + dy][x + dx][3] and
                    not is_outline(src[y + dy][x + dx])]
            if near:
                base = min(near, key=lambda p: p[0] + p[1] + p[2])
                c.set(x, y, darker(base, k))


def load(path):
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    from model_preview import texture_file
    t = texture_file(path)
    c = Canvas(t.width, t.height)
    for y in range(t.height):
        for x in range(t.width):
            c.set(x, y, t.get(x, y))
    return c


PLACEHOLDER_OUTLINES = {(34, 28, 30), (40, 32, 28)}


def restyle(name):
    """Restyles a placeholder icon in place: coloured outline and bevel."""
    path = os.path.join(ITEM_DIR, name + ".png")
    c = load(path)
    recolor_outline(c, lambda p: p[3] and p[:3] in PLACEHOLDER_OUTLINES)
    bevel(c)
    c.save(path)


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
    bevel(c)
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


# --- Boss souls: one heart, four bosses -------------------------------------------------------------------------------

HEART = [
    "................",
    "................",
    "...ooo....ooo...",
    "..oHHHo..oHHHo..",
    ".oHHHHHooHHHHHo.",
    ".oHHHHHHHHHHHHo.",
    ".oHHHHHHHHHHHHo.",
    ".oHHHHHHHHHHHHo.",
    "..oHHHHHHHHHHo..",
    "...oHHHHHHHHo...",
    "....oHHHHHHo....",
    ".....oHHHHo.....",
    "......oHHo......",
    ".......oo.......",
    "................",
    "................",
]


def shaded_icon(name, rows, ramps, details=(), fixed=None):
    """A sketch whose letters are shaded regions: each region's ramp is lit from the top left across its bounding
    box (sphere-like), then details (x, y, colour) are drawn on top."""
    fixed = fixed or {}
    boxes = {}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in ramps:
                x0, y0, x1, y1 = boxes.get(ch, (x, y, x, y))
                boxes[ch] = (min(x0, x), min(y0, y), max(x1, x), max(y1, y))
    grid = [list(r) for r in rows]
    c = Canvas(16, 16)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in ramps:
                x0, y0, x1, y1 = boxes[ch]
                u = (x - x0 + 0.5) / (x1 - x0 + 1)
                v = (y - y0 + 0.5) / (y1 - y0 + 1)
                cols = ramps[ch]
                # Brightest a third in from the top left, darkest at the bottom right.
                d = ((u - 0.33) ** 2 + (v - 0.3) ** 2) ** 0.5
                lv = (len(cols) - 1) * max(0.0, 1.0 - d * 1.35)
                c.set(x, y, cols[clamp(round(lv), 0, len(cols) - 1)])
            elif ch in fixed:
                c.set(x, y, fixed[ch])
    for x, y, col in details:
        c.set(x, y, col)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "o":
                near = [c.get(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                        if 0 <= x + dx < 16 and 0 <= y + dy < 16 and grid[y + dy][x + dx] not in ".o"]
                base = min(near, key=lambda p: p[0] + p[1] + p[2]) if near else hexc("2a2224")
                c.set(x, y, darker(base, 0.45))
    bevel(c, 10)
    c.save(os.path.join(ITEM_DIR, name + ".png"))
    print("wrote", name)


STONE_HEART = ramp("2c2828", "3a3535", "585252", "7a736b", "948c83")
OAK_HEART = ramp("2c221a", "3b2e23", "584737", "7a6248", "9a7d5a")
COLD_HEART = ramp("1d2a3a", "2f4f6e", "4f86b0", "7fc0e6", "c8ecff")
LORD_HEART = ramp("3f1411", "6a1f18", "9a2d1e", "d0482a", "f07a4a")


def souls():
    ember, hot, deep = hexc("ff8a2a"), hexc("ffd27a"), hexc("d8521c")
    gold, gold_d = hexc("e8be55"), hexc("a67b26")
    # Ash Colossus: a heart of fused ash stone, split by a glowing seam.
    shaded_icon("ash_colossus_soul", HEART, {"H": STONE_HEART}, [
        (8, 5, ember), (8, 6, hot), (7, 7, ember), (7, 8, hot), (8, 9, ember), (8, 10, deep), (9, 7, deep),
        (4, 6, hexc("aea69c")), (5, 5, hexc("aea69c"))])
    # Grieving Tree: a knot of the king's oak weeping amber, one withered leaf.
    shaded_icon("grieving_tree_soul", HEART, {"H": OAK_HEART}, [
        (6, 6, hexc("ffb347")), (6, 7, hexc("ffb347")), (6, 8, hexc("c97a2a")), (10, 6, hexc("ffb347")),
        (10, 7, hexc("c97a2a")), (5, 4, hexc("6c5a47")), (11, 9, hexc("6c5a47")),
        (12, 2, hexc("5a5533")), (13, 2, hexc("48462c")), (12, 3, hexc("48462c"))])
    # Crypt Keeper: a heart of cold lantern flame.
    shaded_icon("crypt_keeper_soul", HEART, {"H": COLD_HEART}, [
        (7, 6, hexc("e8fbff")), (8, 7, hexc("e8fbff")), (7, 8, hexc("b8ecff")), (4, 5, hexc("e8fbff"))])
    # Smoldering Lord: a burning heart in a band of the king's gold.
    shaded_icon("smoldering_lord_soul", HEART, {"H": LORD_HEART},
                [(x, 7, gold if x % 2 else gold_d) for x in range(2, 14)] +
                [(x, 6, gold) for x in (3, 6, 9, 12)] + [(7, 9, hot), (8, 10, ember), (8, 4, ember), (4, 5, hot)])


TEMPLATE = [
    "................",
    "..oooooooooooo..",
    ".oSSSSSSSSSSSSo.",
    ".oSSSSSSSSSSSSo.",
    ".oSSSSSSSSSSSSo.",
    ".oSSSSSSSSSSSSo.",
    ".oSSSSSSSSSSSSo.",
    ".oSSSSSSSSSSSSo.",
    ".oSSSSSSSSSSSSo.",
    ".oSSSSSSSSSSSSo.",
    ".oSSSSSSSSSSSSo.",
    ".oSSSSSSSSSSSSo.",
    ".oSSSSSSSSSSSSo.",
    ".oSSSSSSSSSSSo..",
    "..oooooooooooo..",
    "................",
]


def template():
    """Smoldering Template: a slab of ash stone, the corner broken off, a flame carved in it and alight."""
    ember, hot, deep = hexc("ff8a2a"), hexc("ffd27a"), hexc("d8521c")
    carve = hexc("1e1b1c")
    # A carved flame: a dark groove outlining a tongue of fire that glows inside.
    shape = ["...#....", "...##...", "..#.#...", "..#..#..", ".#...#..", ".#....#.", "#.....#.", "#.....#.",
             ".#...#..", "..###..."]
    inner = ["........", "........", "...h....", "...hh...", "..hhE...", "..hEEE..", ".hEEEe..", ".EEEee..",
             "..Eee...", "........"]
    cols = {"h": hot, "E": ember, "e": deep}
    details = []
    for y, (row, fill) in enumerate(zip(shape, inner)):
        for x, (a, b) in enumerate(zip(row, fill)):
            if a == "#":
                details.append((4 + x, 3 + y, carve))
            elif b in cols:
                details.append((4 + x, 3 + y, cols[b]))
    details += [(3, 3, hexc("7a736b")), (12, 12, hexc("2c2828")), (11, 2, hexc("2c2828"))]
    shaded_icon("smoldering_template", TEMPLATE, {"S": ramp("26222a", "322e33", "3e3a3a", "4d4844", "625c56")},
                details)


FLASK_SHARD = [
    "................",
    "................",
    "..........oo....",
    ".........oGGo...",
    "........oGGGo...",
    ".......oGGGEo...",
    "......oGGGEEo...",
    ".....oGGGEEo....",
    "....oGGGEEEo....",
    "....oGGEEEo.....",
    "...oGGEEEo......",
    "...oGEEEo.......",
    "..oGEEo.........",
    "..oEoo..........",
    "...o............",
    "................",
]


def flask_shard():
    """Flask Shard: a curved shard of the Ember Flask's glass, the ember still glowing in it."""
    shaded_icon("flask_shard", FLASK_SHARD, {"G": ramp("4f6a72", "7f9aa2", "b9cdd4", "e6f2f4"),
                                             "E": ramp("7a2a12", "d8521c", "ff8a2a", "ffd27a")},
                [(10, 4, hexc("ffffff")), (9, 5, hexc("e6f2f4"))])


def line_px(x0, y0, x1, y1):
    n = max(abs(x1 - x0), abs(y1 - y0))
    return [(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n)) for i in range(n + 1)] if n else [(x0, y0)]


def auto_outline(c, k=0.45):
    """Outlines every opaque shape: transparent pixels next to it take a dark shade of their darkest neighbour."""
    src = [[c.get(x, y) for x in range(16)] for y in range(16)]
    for y in range(16):
        for x in range(16):
            if src[y][x][3]:
                continue
            near = [src[y + dy][x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                    if 0 <= x + dx < 16 and 0 <= y + dy < 16 and src[y + dy][x + dx][3]]
            if near:
                c.set(x, y, darker(min(near, key=lambda p: p[0] + p[1] + p[2]), k))


def crossbow():
    """Huntsman's Crossbow, six states: stock from the lower left, the bow across its head, the string relaxed,
    drawn back in three steps, then loaded with a bolt or a firework."""
    wood = [hexc("3b2e23"), hexc("5b4c3c"), hexc("7c6340"), hexc("967c53")]
    limb = [hexc("27301f"), hexc("455636"), hexc("586b45"), hexc("7d8f63")]
    iron, string = hexc("777b81"), hexc("e8dcc0")
    tips = ((6, 1), (14, 9))
    nocks = {"standby": (10, 5), "pulling_0": (8, 7), "pulling_1": (7, 8), "pulling_2": (6, 9),
             "arrow": (6, 9), "firework": (6, 9)}
    for state, nock in nocks.items():
        c = Canvas(16, 16)
        for i, (x, y) in enumerate(line_px(1, 14, 11, 4)):           # the stock, lit on its upper side
            c.set(x, y, wood[2] if i % 3 else wood[3])
            c.set(x + 1, y + 1, wood[1])
        c.set(1, 14, wood[0])
        c.set(2, 15, wood[0])
        for (x, y) in line_px(3, 12, 4, 13):                          # the trigger
            c.set(x, y + 1, iron)
        bow = line_px(6, 1, 9, 2) + line_px(9, 2, 12, 4) + line_px(12, 4, 13, 7) + line_px(13, 7, 14, 9)
        for i, (x, y) in enumerate(bow):
            c.set(x, y, limb[2] if i < len(bow) // 2 else limb[1])
        c.set(11, 4, iron)
        c.set(12, 5, iron)
        auto_outline(c)
        for tip in tips:                                              # the string over everything, not outlined
            for x, y in line_px(tip[0], tip[1], nock[0], nock[1]):
                if c.get(x, y)[:3] not in (wood[1][:3], wood[2][:3], wood[3][:3]):
                    c.set(x, y, string)
        if state == "arrow":
            for x, y in line_px(nock[0], nock[1], 12, 3):
                c.set(x, y, hexc("cdb88e"))
            c.set(13, 2, iron)
            c.set(12, 2, iron)
            c.set(nock[0], nock[1], hexc("b8463a"))
        if state == "firework":
            for x, y in line_px(nock[0], nock[1], 11, 4):
                c.set(x, y, hexc("b8463a"))
            c.set(12, 3, hexc("e8be55"))
            c.set(13, 2, hexc("ffd27a"))
        bevel(c, 10)
        c.save(os.path.join(ITEM_DIR, "huntsmans_crossbow_%s.png" % state))
    print("wrote huntsmans_crossbow states")


# --- Warden's Scourge in a hand (PLAN 17.4): the handle alone, the lash is drawn by the client (ScourgeLashes) ------
# Held like a sword: the grip runs up from the bottom left into an iron ferrule; the lash leaves its top, pixel (9, 6)
# (ScourgeLashes.ROOT). The inventory icon (wardens_scourge) stays the whole whip.

SCOURGE_HANDLE = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "........oo......",
    ".......oIio.....",
    "......oIio......",
    ".....oWWo.......",
    "....oWwo........",
    "...oWWo.........",
    "..oWwo..........",
    ".oWWo...........",
    "oIIo............",
    ".oo.............",
    "................",
]

# The lash's texture (textures/entity, drawn on two crossed ribbons, one repeat every 0.3 blocks along the lash): the
# left half grave-iron links, one face on with its hole cut out and one edge on; the right half the popper at the tip,
# a frayed tongue of blood-red leather.
SCOURGE_LASH = [
    "..oLLo..oEEEEo..",
    ".oL..Mo.oEeEEo..",
    ".oL..Mo.oEeEEo..",
    ".oL..Mo.oEeEdo..",
    ".oL..Do.oEeEdo..",
    ".oM..Do.oEEEdo..",
    "..oMDo..oEEEdo..",
    "...oo...oEEEdo..",
    "...LM...oEEddo..",
    "...LM...oEEddo..",
    "...MD...oEEddo..",
    "...MD...oEdddo..",
    "...LM...oEd.do..",
    "...MD...oE.d.o..",
    "...MD...o.d.do..",
    "...oo...o.....o.",
]


def scourge():
    icon("wardens_scourge_handle", SCOURGE_HANDLE, dict(W=(60, 50, 44, 255), w=(84, 70, 60, 255),
                                                        I=(98, 106, 124, 255), i=(150, 158, 176, 255)))
    pal = dict(L=(150, 158, 176, 255), M=(98, 106, 124, 255), D=(58, 64, 80, 255),
               E=(150, 24, 28, 255), e=(196, 52, 50, 255), d=(96, 14, 18, 255))
    c = Canvas(16, 16)
    for y, row in enumerate(SCOURGE_LASH):
        for x, ch in enumerate(row):
            if ch == "o":
                c.set(x, y, darker(pal["E"] if x >= 8 else pal["M"], 0.42))
            elif ch != ".":
                c.set(x, y, pal[ch])
    c.save(os.path.join("src", "main", "resources", "assets", "embervoid", "textures", "entity", "wardens_scourge_lash.png"))
    print("wrote wardens_scourge_lash")


def ensure_placeholder_bases():
    """Runs the silhouettes this script restyles (gear materials, boss items, consumables, weapons, the crossbow
    states; reliquary artifacts and rings) and the icons kept as they are (Bleeding effect, empty-slot icons,
    Reliquary button)."""
    import gear_placeholders as gear
    import reliquary_placeholders as rel
    gear.materials()
    gear.boss_items()
    gear.effects()
    gear.weapons()
    gear.consumables()
    rel.empty_slot_icons()
    rel.button_icon()
    rel.artifacts()
    rel.rings()


RESTYLED = ["cinder_shard", "royal_amber", "raw_grave_iron", "grave_iron_ingot", "smoldering_ingot",
            "ash_of_rematch", "flask_ember", "smoldering_resin", "amber_balm", "amber_lantern_oil", "whetstone",
            "ashen_scythe", "wardens_scourge", "royal_ash_rapier",
            "last_harvest_grain", "ploughmans_horseshoe", "royal_hound_fang", "amber_tear", "gravestone_medallion",
            "ferrymans_coin", "wardens_chain", "seal_of_the_ashen_king", "couriers_ring", "ring_of_ash_feathers",
            "ring_of_the_fog_stalker", "delvers_ring", "drowned_mans_ring", "ring_of_embers"]


if __name__ == "__main__":
    ensure_placeholder_bases()
    for n in RESTYLED:
        restyle(n)
    print("restyled", len(RESTYLED))
    souls()
    template()
    flask_shard()
    crossbow()
    consumables()
    scourge()
