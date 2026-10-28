# Embervoid wiki — source

The player wiki's source. `tools/wiki_build.py` turns it into the package moddedmc.wiki reads; the plan and the
reasons are in [docs/wiki.md](../docs/wiki.md).

```
python3 tools/wiki_build.py check            # coverage, links, directives, translations report
python3 tools/wiki_build.py build            # build/wiki/ (moddedmc.wiki package)
```

## Layout

```
wiki/
  glossary.json         every game term in every language
  _ignore.txt           registry ids that deliberately have no page
  _data/                generated JSON (stats export); never edited by hand
  en/                   the original
    guides/             read in order: walkthrough, mechanics, server topics
    encyclopedia/       looked up: places, npcs, bosses, mobs, items, blocks
  ru/ de/ fr/ zh/       translations, same paths as en/
```

A translated page has exactly the path of its English original. A page missing in a language shows in English.

## A page

```markdown
---
id: embervoid:ashen_scythe
kind: weapon
status: stub
---

{{stats}}

## What makes it itself
...
```

Frontmatter is flat `key: value` lines; a list is `[a, b, c]`.

| Key | Where | Meaning |
|---|---|---|
| `title` | pages without an `id`/`entity`/`biome`; optional otherwise | page title; with an id the title comes from the lang file |
| `id` / `ids` | items, blocks | registry id(s) the page covers (`embervoid:…`); a family page lists every variant |
| `entity` / `entities` | mobs, bosses, npcs | entity id(s) |
| `biome` | places | biome id |
| `effect` | mechanics | status effect id |
| `kind` | all | `guide`, `mechanic`, `place`, `npc`, `boss`, `mob`, `weapon`, `armor`, `accessory`, `soul`, `material`, `consumable`, `tool`, `block` |
| `order` | guides | reading order inside its folder |
| `since` | optional | the mod version that added it |
| `status` | while unwritten | `stub` — the page is skipped by `build` and listed by `check` |
| `source` | translations only | `wiki_build.py hash <en page>` of the original it was translated from |

A folder's title and order: `_folder.md` in it, with only `title:` and `order:`.

## Directives

Written on their own line (or inline for `{{item}}`); `build` expands them per target.

| Directive | Becomes |
|---|---|
| `{{recipe}}` | how the page's `id` is crafted |
| `{{usage}}` | what the page's `id` is used in |
| `{{drops}}` / `{{drops embervoid:<entity>}}` | the entity's loot table as a table (page entity by default) |
| `{{sources}}` | every loot table that drops the page's `id` (mobs, bosses, chests) |
| `{{stats}}` | the infobox from `_data/` for the page's id or entity |
| `{{set-bonus}}` | the armor set's bonus from `_data/` |
| `{{config <key>}}` | the config key's default and range |
| `{{mobs-in}}` | the mobs that spawn in the page's biome |
| `{{item embervoid:<id>}}` | inline: icon + localized name, linked to its page |
| `{{item embervoid:<id> <text>}}` | the same with your own link text, e.g. a declined form: `{{item embervoid:cinder_shard осколков угля}}` |

Numbers that the code knows are never typed by hand: put a directive there. If no directive covers it, write the
number and add the source (`config key`, class) in an HTML comment next to it so a later export can take it over.

## Links

Plain relative Markdown links between source files (`[the Reliquary](../mechanics/reliquary.md)`). `build`
rewrites them; `check` fails on a link to a missing file.

## Style

- Second person, present tense, short paragraphs: "Strike the lanterns before the fog closes in."
- A guide answers *what to do*; an encyclopedia page answers *what it is*. No lore dumps; the mod keeps lore in item
  descriptions and the world.
- Every game term exactly as in `glossary.json` (and so as in the game).
- No spoilers in guide titles; boss strategy goes on the boss page, the walkthrough links to it.
- Screenshots go in `wiki/assets/`; name them after the page (`ashen-scythe-1.png`).

## Translating

1. Copy the English page to the same path under `ru/`, `de/`, `fr/` or `zh/`.
2. Translate the text; leave frontmatter keys, directives and link targets as they are.
3. Use `glossary.json` for every term.
4. Set `source:` to `python3 tools/wiki_build.py hash wiki/en/<path>`. `check` reports the page as stale once the
   English page changes.
