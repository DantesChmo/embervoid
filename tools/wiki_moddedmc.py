"""The moddedmc.wiki target of tools/wiki_build.py: wiki/ → build/wiki/, a package in moddedmc.wiki's layout
(sinytra-wiki.json, docs/, content/, translated/<lang>/, data/, assets/). Format: https://docs.moddedmc.wiki/.

Not yet checked against the wiki's previewer: links to docs pages ($docs/...), `id` on entity and place pages,
recipes in the 1.20.1 JSON shape.
"""

import glob
import json
import os
import re
import shutil

from wiki_build import DIRECTIVE, LANGUAGES, LINK, MOD, ROOT, WIKI, config_value, load_lang

GENERATED = os.path.join(ROOT, "src/main/generated/data", MOD)
RESOURCES = os.path.join(ROOT, "src/main/resources")
ITEM_TEXTURES = os.path.join(RESOURCES, "assets", MOD, "textures/item")

METADATA = {
    "schema": "1",
    "id": MOD,
    "platforms": {"modrinth": MOD},
    "modid": MOD,
    "owners": ["danteschmo"],
}
ITEM_KINDS = {"weapon", "armor", "accessory", "soul", "material", "consumable", "tool"}
COMMENT = re.compile(r"\s*<!--.*?-->", re.S)

HEADINGS = {
    "en": {"item": "Item", "count": "Count", "chance": "Chance", "dropped_by": "Dropped by", "found_in": "Found in", "default": "default",
           "chest_common": "chests", "chest_treasure": "treasure chest", "chest_arena": "arena chest", "chest_sanctum_outer_hall": "Ruined Sanctum outer halls"},
    "ru": {"item": "Предмет", "count": "Количество", "chance": "Шанс", "dropped_by": "Выпадает с", "found_in": "Лежит в", "default": "по умолчанию",
           "chest_common": "сундуки", "chest_treasure": "сундук с сокровищами", "chest_arena": "сундук арены", "chest_sanctum_outer_hall": "внешние залы Разорённого Святилища"},
    "de": {"item": "Gegenstand", "count": "Anzahl", "chance": "Chance", "dropped_by": "Droppt von", "found_in": "Zu finden in", "default": "Standard",
           "chest_common": "Truhen", "chest_treasure": "Schatztruhe", "chest_arena": "Arenatruhe", "chest_sanctum_outer_hall": "Außenhallen des Zerstörten Heiligtums"},
    "fr": {"item": "Objet", "count": "Quantité", "chance": "Chance", "dropped_by": "Lâché par", "found_in": "Trouvé dans", "default": "par défaut",
           "chest_common": "coffres", "chest_treasure": "coffre au trésor", "chest_arena": "coffre de l'arène", "chest_sanctum_outer_hall": "salles extérieures du Sanctuaire en ruine"},
    "zh": {"item": "物品", "count": "数量", "chance": "概率", "dropped_by": "掉落自", "found_in": "发现于", "default": "默认",
           "chest_common": "箱子", "chest_treasure": "宝藏箱", "chest_arena": "竞技场箱子", "chest_sanctum_outer_hall": "废墟圣所外厅"},
}


def build(pages, out, stubs=False):
    if os.path.exists(out):
        shutil.rmtree(out)
    os.makedirs(out)
    with open(os.path.join(out, "sinytra-wiki.json"), "w", encoding="utf-8") as f:
        json.dump(METADATA, f, indent=2)

    recipes = load_recipes()
    loot = load_loot()
    english = pages["en"]
    for lang, code in LANGUAGES.items():
        names = Names(load_lang(code), load_lang("en_us"))
        base = out if lang == "en" else os.path.join(out, "translated", code)
        for rel, page in pages[lang].items():
            original = english.get(rel)
            if original is None or (original.stub and not stubs):
                continue
            if rel.endswith("_folder.md"):
                continue
            target = os.path.join(base, out_path(rel))
            os.makedirs(os.path.dirname(target), exist_ok=True)
            with open(target, "w", encoding="utf-8") as f:
                f.write(render(page, original, english, names, recipes, loot, lang))
        write_meta(pages[lang], english, base, names, stubs)

    copy_game_data(out, recipes)
    copy_icons(out)
    print(f"built {out}")
    return 0


def out_path(rel):
    rel = rel[:-3] + ".mdx"
    if rel.startswith("guides/"):
        return "docs/" + rel[len("guides/"):]
    return "content/" + rel[len("encyclopedia/"):]


def page_id(page):
    if page.items:
        return page.items[0]
    if page.entities:
        return page.entities[0]
    if "biome" in page.front:
        return page.front["biome"]
    return f"{MOD}:" + os.path.basename(page.rel)[:-3].replace("-", "_")


class Names:
    def __init__(self, lang, fallback):
        self.lang = lang
        self.fallback = fallback

    def get(self, kind, ident):
        key = f"{kind}.{ident.replace(':', '.')}"
        return self.lang.get(key) or self.fallback.get(key) or ident.split(":")[1].replace("_", " ").title()

    def thing(self, ident):
        for kind in ("item", "block", "entity", "biome"):
            key = f"{kind}.{ident.replace(':', '.')}"
            if key in self.lang or key in self.fallback:
                return self.get(kind, ident)
        return self.get("item", ident)


def title(page, original, names):
    if "title" in page.front:
        return page.front["title"]
    if original.items:
        return names.thing(original.items[0])
    if original.entities:
        return names.get("entity", original.entities[0])
    return names.get("biome", original.front["biome"])


def render(page, original, english, names, recipes, loot, lang):
    front = {"title": title(page, original, names)}
    if original.rel.startswith("encyclopedia/"):
        ids = original.items
        front["id"] = ids if len(ids) > 1 else page_id(original)
        kind = original.front["kind"]
        front["type"] = "block" if kind == "block" else "item" if kind in ITEM_KINDS else "other"
    head = "---\n" + "".join(f"{k}: {json.dumps(v, ensure_ascii=False)}\n" for k, v in front.items()) + "---\n"

    body = COMMENT.sub("", page.body)
    body = DIRECTIVE.sub(lambda m: directive(m.group(1), m.group(2), original, names, recipes, loot, lang), body)
    body = LINK.sub(lambda m: link(m, page, english), body)
    return head + re.sub(r"\n{3,}", "\n\n", body).strip() + "\n"


def directive(name, arg, page, names, recipes, loot, lang):
    words = HEADINGS[lang]
    if name == "recipe":
        found = [rid for ident in page.items for rid, r in sorted(recipes.items()) if result_of(r) == ident]
        return "\n\n".join(f'<ProjectRecipe id="{rid}" />' for rid in found)
    if name == "usage":
        return "\n\n".join(f'<RecipeUsage id="{ident}" />' for ident in page.items)
    if name == "item":
        ident, _, text = arg.partition(" ")
        return f"[{text.strip() or names.thing(ident)}](@{ident})"
    if name == "drops":
        table = loot.get(drops_table(arg, page, loot))
        return drops_markdown(table, names, words) if table else ""
    if name == "sources":
        return sources_markdown(page.items, loot, names, words)
    if name == "config":
        return config_markdown(arg, words)
    # stats, set-bonus, mobs-in come from wiki/_data/ once the stats export exists
    return ""


def config_markdown(key, words):
    value = config_value(key)
    if isinstance(value, dict):
        rows = "".join(f"\n  - `{k}`: {json.dumps(v)}" for k, v in value.items())
        return f"- `{key}` — {words['default']}:{rows}"
    return f"- `{key}` — {words['default']} **{json.dumps(value)}**"



def drops_table(arg, page, loot):
    if arg:
        return "entities/" + arg.split(":")[1]
    for ident in page.entities + page.items:
        for kind in ("entities", "blocks"):
            key = f"{kind}/{ident.split(':')[1]}"
            if key in loot:
                return key
    return None


def link(match, page, english):
    target = match.group(1)
    if re.match(r"^[a-z]+:", target) or target.startswith("#"):
        return match.group(0)
    path, _, anchor = target.partition("#")
    rel = os.path.normpath(os.path.join(os.path.dirname(page.rel), path))
    linked = english.get(rel)
    if linked is None:
        return match.group(0)
    if rel.startswith("guides/"):
        new = "$docs/" + rel[len("guides/"):-3]
    else:
        new = "@" + page_id(linked)
    if anchor:
        new += "#" + anchor
    return match.group(0).replace("(" + target + ")", "(" + new + ")")


def write_meta(lang_pages, english, base, names, stubs):
    folders = {}
    for rel, page in english.items():
        if page.stub and not stubs:
            continue
        folder = os.path.dirname(rel)
        if rel.endswith("_folder.md"):
            continue
        while folder:
            folders.setdefault(folder, [])
            folder = os.path.dirname(folder)
    for folder in folders:
        entries = []
        for rel, page in english.items():
            if os.path.dirname(rel) != folder or rel.endswith("_folder.md") or (page.stub and not stubs):
                continue
            shown = lang_pages.get(rel, page)
            entries.append((int(page.front.get("order", 99)), os.path.basename(rel)[:-3] + ".mdx",
                            title(shown, page, names)))
        for sub in folders:
            if os.path.dirname(sub) == folder:
                meta = lang_pages.get(sub + "/_folder.md", english.get(sub + "/_folder.md"))
                entries.append((int(meta.front.get("order", 99)), os.path.basename(sub), meta.front["title"]))
        if folder in ("guides", "encyclopedia"):
            target = os.path.join(base, "docs" if folder == "guides" else "content", "_meta.json")
        else:
            target = os.path.join(base, os.path.dirname(out_path(folder + "/x.md")), "_meta.json")
        os.makedirs(os.path.dirname(target), exist_ok=True)
        with open(target, "w", encoding="utf-8") as f:
            json.dump({name: shown for _, name, shown in sorted(entries)}, f, indent=2, ensure_ascii=False)


def load_recipes():
    recipes = {}
    for folder in (os.path.join(GENERATED, "recipes"), os.path.join(RESOURCES, "data", MOD, "recipes")):
        for path in glob.glob(os.path.join(folder, "*.json")):
            with open(path, encoding="utf-8") as f:
                recipes[f"{MOD}:{os.path.basename(path)[:-5]}"] = json.load(f)
    return recipes


def result_of(recipe):
    result = recipe.get("result")
    if isinstance(result, dict):
        return result.get("item") or result.get("id")
    return result


def load_loot():
    tables = {}
    for folder in (os.path.join(GENERATED, "loot_tables"), os.path.join(RESOURCES, "data", MOD, "loot_tables")):
        for path in glob.glob(os.path.join(folder, "**", "*.json"), recursive=True):
            with open(path, encoding="utf-8") as f:
                tables[os.path.relpath(path, folder)[:-5]] = json.load(f)
    return tables


def entries(table):
    """(item, count text, chance) for every item entry of a loot table."""
    rows = []
    for pool in table.get("pools", []):
        chance = 1.0
        for condition in pool.get("conditions", []):
            chance *= condition.get("chance", 1.0)
        items = [e for e in pool.get("entries", []) if e.get("type") == "minecraft:item"]
        total = sum(e.get("weight", 1) for e in pool.get("entries", []))
        for entry in items:
            share = chance * entry.get("weight", 1) / total
            for condition in entry.get("conditions", []):
                share *= condition.get("chance", 1.0)
            rows.append((entry["name"], count_text(entry), share))
    return rows


def count_text(entry):
    for function in entry.get("functions", []):
        if function.get("function") == "minecraft:set_count":
            count = function["count"]
            if isinstance(count, dict):
                return f"{count['min']:g}–{count['max']:g}"
            return f"{count:g}"
    return "1"


def drops_markdown(table, names, words):
    rows = entries(table)
    if not rows:
        return ""
    lines = [f"| {words['item']} | {words['count']} | {words['chance']} |", "|---|---|---|"]
    for item, count, chance in rows:
        lines.append(f"| [{names.thing(item)}](@{item}) | {count} | {chance:.0%} |")
    return "\n".join(lines)


def sources_markdown(ids, loot, names, words):
    mobs, chests = [], {}
    for key, table in sorted(loot.items()):
        if not any(item in ids for item, _, _ in entries(table)):
            continue
        kind, _, name = key.partition("/")
        if kind == "entities":
            ident = f"{MOD}:{name}"
            mobs.append(f"[{names.get('entity', ident)}](@{ident})")
        elif kind == "chests":
            place, kind = chest_place(name, names, words)
            chests.setdefault(place, []).append(kind)
    lines = []
    if mobs:
        lines.append(f"**{words['dropped_by']}:** " + ", ".join(mobs))
    if chests:
        found = [place + (f" ({', '.join(kinds)})" if kinds != [None] else "") for place, kinds in chests.items()]
        lines.append(f"**{words['found_in']}:** " + "; ".join(found))
    return "\n\n".join(lines)


CHEST_ZONES = {"fields": "ashen_reach_zone_a", "woods": "ashen_reach_zone_b", "necropolis": "ashen_reach_zone_c",
               "citadel": "ashen_reach_citadel"}


def chest_place(table, names, words):
    """ashen_reach/fields_treasure → ("[Ashen Fields](@…)", "treasure chest"); a table outside a realm has no kind."""
    if "/" not in table:
        return words["chest_" + table], None
    zone, _, kind = table.split("/")[1].rpartition("_")
    biome = f"{MOD}:{CHEST_ZONES[zone]}"
    return f"[{names.get('biome', biome)}](@{biome})", words["chest_" + kind]


def copy_game_data(out, recipes):
    recipe_dir = os.path.join(out, "data", MOD, "recipe")
    os.makedirs(recipe_dir)
    for rid, recipe in recipes.items():
        result = recipe.get("result")
        if isinstance(result, dict) and "item" in result and "id" not in result:
            recipe = dict(recipe, result=dict(result, id=result["item"]))
        with open(os.path.join(recipe_dir, rid.split(":")[1] + ".json"), "w", encoding="utf-8") as f:
            json.dump(recipe, f, indent=2)
    shutil.copytree(os.path.join(GENERATED, "tags"), os.path.join(out, "data", MOD, "tags"))
    shutil.copytree(os.path.join(RESOURCES, "assets", MOD, "lang"), os.path.join(out, "assets", MOD, "lang"))


def copy_icons(out):
    """Item textures stand in for icons until WikiToolkit's exporter renders them (blocks have none yet)."""
    target = os.path.join(out, "assets", MOD, "item")
    os.makedirs(target)
    for path in glob.glob(os.path.join(ITEM_TEXTURES, "*.png")):
        shutil.copy(path, target)
