#!/usr/bin/env python3
"""Builds the player wiki from wiki/ (see wiki/README.md and docs/wiki.md).

    python3 tools/wiki_build.py check           coverage, links, directives; translations report
    python3 tools/wiki_build.py build [--stubs] build/wiki/, the moddedmc.wiki package (--stubs: unwritten pages too)
    python3 tools/wiki_build.py hash <page>     the `source:` value for a translation of <page>
"""

import hashlib
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WIKI = os.path.join(ROOT, "wiki")
LANG_DIR = os.path.join(ROOT, "src/main/resources/assets/embervoid/lang")
MOD = "embervoid"

LANGUAGES = {"en": "en_us", "ru": "ru_ru", "de": "de_de", "fr": "fr_fr", "zh": "zh_cn"}
KINDS = {"guide", "mechanic", "place", "npc", "boss", "mob", "weapon", "armor", "accessory", "soul", "material",
         "consumable", "tool", "block"}
KEYS = {"title", "id", "ids", "entity", "entities", "biome", "effect", "kind", "order", "since", "status", "source"}
DIRECTIVES = {"recipe", "usage", "drops", "sources", "stats", "set-bonus", "config", "mobs-in", "item"}
DIRECTIVE = re.compile(r"\{\{([a-z-]+)(?:\s+([^}]*?))?\s*\}\}")
LINK = re.compile(r"\[[^\]]*\]\(([^)\s]+)\)")


class Page:
    def __init__(self, lang, rel, front, body):
        self.lang = lang
        self.rel = rel
        self.front = front
        self.body = body

    def ids(self, key, plural):
        value = self.front.get(plural, self.front.get(key, []))
        return value if isinstance(value, list) else [value]

    @property
    def items(self):
        return self.ids("id", "ids")

    @property
    def entities(self):
        return self.ids("entity", "entities")

    @property
    def stub(self):
        return self.front.get("status") == "stub"


def parse_front(text, where):
    if not text.startswith("---\n"):
        raise ValueError(f"{where}: no frontmatter")
    end = text.index("\n---\n", 4)
    front = {}
    for line in text[4:end].splitlines():
        if not line.strip():
            continue
        key, sep, value = line.partition(":")
        if not sep:
            raise ValueError(f"{where}: bad frontmatter line '{line}'")
        value = value.strip()
        if value.startswith("[") and value.endswith("]"):
            value = [v.strip() for v in value[1:-1].split(",") if v.strip()]
        front[key.strip()] = value
    return front, text[end + 5:]


def load_pages():
    pages = {lang: {} for lang in LANGUAGES}
    for lang in LANGUAGES:
        base = os.path.join(WIKI, lang)
        for dirpath, _, files in os.walk(base):
            for name in sorted(files):
                if not name.endswith(".md"):
                    continue
                path = os.path.join(dirpath, name)
                rel = os.path.relpath(path, base)
                with open(path, encoding="utf-8") as f:
                    front, body = parse_front(f.read(), f"{lang}/{rel}")
                pages[lang][rel] = Page(lang, rel, front, body)
    return pages


def load_lang(code):
    path = os.path.join(LANG_DIR, code + ".json")
    if not os.path.exists(path):
        return {}
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def registry():
    """Every id the mod registers that a player can meet, read from the English lang file."""
    ids = set()
    for key in load_lang("en_us"):
        parts = key.split(".")
        if len(parts) == 3 and parts[1] == MOD and parts[0] in ("item", "block", "entity", "biome", "effect"):
            if parts[2].endswith("_spawn_egg"):
                continue
            ids.add(f"{parts[0]}:{MOD}:{parts[2]}")
    return ids


def ignored():
    path = os.path.join(WIKI, "_ignore.txt")
    with open(path, encoding="utf-8") as f:
        return {line.strip() for line in f if line.strip() and not line.startswith("#")}


def config_value(key):
    with open(os.path.join(WIKI, "_data", "config.json"), encoding="utf-8") as f:
        value = json.load(f)
    for part in key.split("."):
        value = value[part]
    return value


def page_hash(path):
    with open(path, "rb") as f:
        return hashlib.sha256(f.read()).hexdigest()[:12]


def covered(page):
    ids = set()
    for i in page.items:
        ids.update({"item:" + i, "block:" + i})
    ids.update("entity:" + e for e in page.entities)
    for key in ("biome", "effect"):
        if key in page.front:
            ids.add(f"{key}:{page.front[key]}")
    return ids


def check_page(page, errors):
    where = f"{page.lang}/{page.rel}"
    if page.rel.endswith("_folder.md"):
        if set(page.front) - {"title", "order"}:
            errors.append(f"{where}: a folder file holds only title and order")
        return
    for key in set(page.front) - KEYS:
        errors.append(f"{where}: unknown frontmatter key '{key}'")
    if page.front.get("kind") not in KINDS:
        errors.append(f"{where}: kind must be one of {sorted(KINDS)}")
    named = page.items or page.entities or "biome" in page.front
    if not named and "title" not in page.front:
        errors.append(f"{where}: a page without an id needs a title")
    for match in DIRECTIVE.finditer(page.body):
        name, arg = match.group(1), match.group(2)
        if name not in DIRECTIVES:
            errors.append(f"{where}: unknown directive {{{{{name}}}}}")
        elif name in ("recipe", "usage", "sources", "set-bonus") and not page.items:
            errors.append(f"{where}: {{{{{name}}}}} needs an id")
        elif name == "drops" and not (arg or page.entities or page.items):
            errors.append(f"{where}: {{{{drops}}}} needs an entity")
        elif name == "mobs-in" and "biome" not in page.front:
            errors.append(f"{where}: {{{{mobs-in}}}} needs a biome")
        elif name in ("config", "item") and not arg:
            errors.append(f"{where}: {{{{{name}}}}} needs an argument")
        elif name == "config":
            try:
                config_value(arg)
            except (KeyError, TypeError):
                errors.append(f"{where}: no config key {arg}")
    base = os.path.dirname(os.path.join(WIKI, page.lang, page.rel))
    for target in LINK.findall(page.body):
        if re.match(r"^[a-z]+:", target) or target.startswith("#"):
            continue
        path = os.path.normpath(os.path.join(base, target.split("#")[0]))
        if not os.path.exists(path) and not os.path.exists(path.replace(f"/{page.lang}/", "/en/", 1)):
            errors.append(f"{where}: broken link {target}")


def check():
    pages = load_pages()
    errors = []
    for lang in LANGUAGES:
        for page in pages[lang].values():
            check_page(page, errors)

    english = pages["en"]
    seen = set()
    for page in english.values():
        seen |= covered(page)
    known = registry()
    for missing in sorted(known - seen - ignored()):
        errors.append(f"no page for {missing}")
    for name in sorted(seen - known):
        kind, _, rest = name.partition(":")
        if kind in ("item", "block") and {"item:" + rest, "block:" + rest} & known:
            continue
        errors.append(f"a page names {name}, which the mod does not register")

    report = []
    stubs = sum(1 for p in english.values() if p.stub)
    content = [p for p in english.values() if not p.rel.endswith("_folder.md")]
    report.append(f"en: {len(content) - stubs} of {len(content)} pages written")
    for lang in LANGUAGES:
        if lang == "en":
            continue
        missing = stale = 0
        for rel, page in english.items():
            if page.stub or rel.endswith("_folder.md"):
                continue
            translated = pages[lang].get(rel)
            if translated is None:
                missing += 1
            elif translated.front.get("source") != page_hash(os.path.join(WIKI, "en", rel)):
                stale += 1
        for rel in pages[lang]:
            if rel not in english:
                errors.append(f"{lang}/{rel}: no English original")
        report.append(f"{lang}: {missing} missing, {stale} stale")

    for line in report:
        print(line)
    for error in errors:
        print("ERROR " + error)
    return 1 if errors else 0


def main(argv):
    if len(argv) < 2 or argv[1] not in ("check", "build", "hash"):
        print(__doc__)
        return 2
    if argv[1] == "check":
        return check()
    if argv[1] == "hash":
        print(page_hash(argv[2]))
        return 0
    import wiki_moddedmc
    status = check()
    if status:
        return status
    return wiki_moddedmc.build(load_pages(), os.path.join(ROOT, "build", "wiki"), "--stubs" in argv)


if __name__ == "__main__":
    sys.exit(main(sys.argv))
