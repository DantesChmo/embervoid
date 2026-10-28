# Embervoid — Wiki plan

Where the player wiki lives, how it is written and built, and its page tree. No page content yet.
Researched 2026-10-28.

---

## 1. Where mod wikis live today

| Where | Format | Who uses it | Fit for Embervoid |
|---|---|---|---|
| **moddedmc.wiki** (Sinytra) | **MDX** (Markdown + components) in the mod's own GitHub repo; `sinytra-wiki.json` metadata, `_meta.json` per folder for sidebar order and names; recipes and tags as datapack JSON; names from the mod's `lang/*.json`; item icons rendered by a Gradle plugin (WikiToolkit, works with Fabric Loom) | New, made exactly for mod docs; growing list of NeoForge and Fabric mods | **Best.** Docs stay in our repo, recipes come from our datagen JSON, en/ru from our lang files, PR-reviewed, free, no ads |
| **MediaWiki farms**: wiki.gg, Miraheze, Fandom, the old FTB Wiki | **Wikitext**, not Markdown; infobox/recipe-grid templates | Big classic mods (Twilight Forest, Thaumcraft, Botania wikis) | Possible, but the conversion is real work (pandoc md → wikitext, own templates, a bot to upload via the API). Fandom carries heavy ads |
| **GitHub Wiki** of the repo | Markdown in a separate `*.wiki.git` repo | Small mods | Zero conversion (a CI step copies pages), but no recipe grids, no search worth the name, plain look |
| **Static site** (MkDocs Material / VitePress / Docusaurus) on GitHub Pages | Markdown | Many tech and library mods (Create addons, KubeJS, Fabric docs) | Full control, but recipe grids, item icons and i18n are ours to build |
| **Modrinth / CurseForge page** | Markdown (Modrinth) / HTML (CurseForge) | Every mod, as the storefront | Not a wiki: one description page. Its "Wiki" link points at one of the above |
| **In-game guidebook**: Patchouli (JSON), Lavender (Markdown, Fabric, 1.20.1 build exists, needs owo-lib), Modonomicon | JSON or Markdown inside the jar | Botania, AE2 (GuideME, NeoForge only), many tech mods | A later second target: a short guide book generated from the same source. Not the wiki itself |

So the hunch is half right: the classic wikis are wikitext, but the modern ones are Markdown-based. Our source can be
plain Markdown and the converter stays small.

## 2. Decision (Proposed)

- **Source of truth:** `wiki/` in this repo, plain Markdown with YAML frontmatter, one tree per language (§4.1).
  Only hand-written text lives there; anything the code already knows (recipes, stats, drops, config defaults) is
  generated.
- **Primary target:** moddedmc.wiki. `tools/wiki_build.py` turns `wiki/` into the MDX package moddedmc.wiki reads
  (`build/wiki/` locally, a `wiki-dist` branch or the `/wiki-dist` folder in CI).
- **Local preview:** WikiToolkit's `previewDocs` (localhost:3000, needs Node 20).
- **Later, optional:** a second target for an in-game book (Lavender) built from the `guide` pages.
- **Fallback** if moddedmc.wiki does not work out: the same converter's `site` target for MkDocs Material on
  GitHub Pages; the source does not change.

Prerequisites for moddedmc.wiki: a Modrinth or CurseForge project whose "Source code" link is
`https://github.com/DantesChmo/embervoid` (ownership check), the repo stays public (it is), the docs path and branch
registered on the wiki portal.

To check before building the pipeline (unverified):
- WikiToolkit's item-icon export on Loom 1.7.4 / MC 1.20.1 (the plugin lists Fabric Loom, no version range).
- moddedmc.wiki reads recipes from `data/<modid>/recipe/` (1.21 name); ours are `recipes/` (1.20.1). The converter
  renames.
- Custom recipe types (the stele ritual, anything not vanilla crafting/smelting/stonecutting/smithing) need a
  `recipe_type` definition with a background image.

## 3. Pipeline

```
wiki/**/*.md  ──┐
lang/en_us, ru_ru ─┤
generated/recipes, loot_tables, tags ─┤──► tools/wiki_build.py ──► build/wiki/  (moddedmc package: sinytra-wiki.json,
wiki/_data/*.json (exported stats) ─┘         --target moddedmc           docs/, content/, data/, assets/, translated/ru_ru/)
                                              --target site      ──► build/site/ (MkDocs, fallback)
                                              --check            ──► fails on broken links, missing pages, stale data
```

- **Generated data.** A dev-only export (a JUnit test or Gradle task run on the registry and config, the same way the
  balance model reads them) writes `wiki/_data/`: weapon damage/speed/reach/scaling/special cost, armor values and
  set bonuses, ring/artifact effects, mob HP/levels/damage per difficulty, boss HP per phase, config keys with
  defaults. Committed, so a diff shows when a number on the wiki moves.
- **Directives** in the Markdown, expanded per target (moddedmc → its components, site → tables):
  `{{recipe}}` / `{{usage}}` (the page's `id`), `{{drops <entity>}}`, `{{stats}}`, `{{set-bonus <set>}}`,
  `{{config <key>}}`, `{{mobs-in <biome>}}`, `{{item <id>}}` (inline icon + name link).
- **Check (CI, in the regular build):** every registered item, block (except building variants), entity and biome has
  a page or is listed in `wiki/_ignore.txt`; no translated page without its `en` original; links resolve; no
  `{{…}}` left unexpanded; `_data` matches a fresh export. In the spirit of `ClientAssetRulesTest`. Missing or stale
  translations are a report, not a failure (§4.1).
- **Release:** the wiki follows the latest release, not `main`: the release workflow runs the build and pushes the
  package. Each page carries `since:` (the version that added it).

## 4. Source format

```
wiki/
  README.md               how to write pages (frontmatter, directives, style)
  _data/                  generated JSON (do not edit)
  _ignore.txt             ids deliberately without a page (spawn eggs, debug items, slabs/stairs)
  glossary.json           every game term in every language (§4.1)
  en/                     the original; every other tree mirrors its paths
  ru/
  de/
  fr/
  zh/
```

Frontmatter:

```yaml
---
title: Ashen Scythe
id: embervoid:ashen_scythe    # registry id; content pages only
kind: weapon                  # guide | mechanic | place | weapon | armor | accessory | material | consumable | block | mob | boss | npc
since: 0.4.0
order: 3                      # guides only: reading order in the sidebar
---
```

Page title and item names come from the lang files where the page has an `id`; the converter writes `_meta.json`
from `order` and `title`.

### 4.1 Languages

English, Russian, German, French, Simplified Chinese. moddedmc.wiki supports all five: a translated page goes to
`translated/<code>/` with the same path as the original (`ru_ru`, `de_de`, `fr_fr`, `zh_cn`), and a page with no
translation shows in English.

- **English is the original.** Pages are written in English first; the others are translations of it.
  A translated page's frontmatter holds `source: <hash of the en page>`; `--check` reports translations whose
  original changed since (stale) and missing ones, per language.
- **Names come from the mod's lang files**, so the mod needs `de_de.json`, `fr_fr.json` and `zh_cn.json` next to
  `en_us` / `ru_ru` (items, blocks, mobs, biomes, effects, tooltips) — the mod itself gets translated too, not
  only the wiki. Without them the German wiki shows English item names.
- **`glossary.json`** fixes every game term per language (Ember, Ash Remnant, Cleft Stele, stat names, boss names)
  before any page is translated, and is the source for the lang files' wording; `--check` flags a page using a term
  outside it.
- **Generated parts translate themselves:** tables, infoboxes and section headings come from per-language strings in
  the converter, so only the hand text needs a translator.
- **Who translates:** drafted by Claude from the English page plus the glossary; a native speaker's review when one
  turns up (moddedmc.wiki also takes community fixes as PRs). Russian is reviewed by the user.

## 5. Page tree

Two halves, as moddedmc.wiki splits them: **Guides** (read in order) and **Encyclopedia** (look up).
`(gen)` — the page is mostly or wholly generated; `(hand)` — written by hand around a few directives.

### 5.1 Guides (`docs/`)

```
1  Welcome                         what Embervoid is, the loop in one picture, links to the start   (hand)
2  Installing                      Fabric 1.20.1, Fabric API, GeckoLib, playerAnimator, Mod Menu;   (hand)
                                   singleplayer vs server; updating worlds between versions
3  Walkthrough
   3.1 A harder overworld          stronger vanilla mobs, new hostiles, why XP matters now          (hand)
   3.2 Finding a Ruined Sanctum    Sanctum Map, what the halls hold, flask shards                    (hand)
   3.3 The Fallen Sister           first boss, how to prepare at level 1, the Crying Soul            (hand)
   3.4 Kindling the Cleft Stele    the dungeon stele, building your own circle, one lit per world   (hand)
   3.5 The Ember Sanctum           the hub tour: Ashen Maiden, obelisk ring, Ember Shrine           (hand)
   3.6 Ashen Reach                 entering, the three zones in any order, suggested order and why  (hand)
   3.7 The Smoldering Citadel      opening the final zone, the city, the Smoldering Lord            (hand)
   3.8 After the realm             what unlocks, farming, rematches                                  (hand)
4  Mechanics
   4.1 Stats and leveling          the stats, what each does, level cost, respec if any             (hand + gen tables)
   4.2 Mob levels and difficulty   how mob level is picked, what it changes                         (hand + gen)
   4.3 Stamina and special attacks the right-click contract, costs, cooldowns                     (hand + gen)
   4.4 Healing Flask               charges, refills, Flask Shards, Flask Ember, re-crafting a lost flask
   4.5 Death and the Ash Remnant   what drops, second death, config modes
   4.6 Ember Shrines and respawn
   4.7 Boss fights                 fog walls, leaving and returning, Ash of Rematch, multiplayer
   4.8 The Reliquary               ring and artifact slots, what dies with you
   4.9 Armor sets                  full-set bonuses, mixing pieces                                  (hand + gen)
   4.10 Status effects             Bleeding (and later ones)
   4.11 Moon nights                the moon phases and what each brings
   4.12 The Vampire                when it comes, how to deal with it
5  Gear guide                      the loot ladder vs vanilla, which unique fits which stats,       (hand + gen)
                                   where each piece comes from
6  Multiplayer and servers         shared realms, bosses in a party, server config                 (hand)
7  Configuration                   every config key with its default and meaning                    (gen + short hand notes)
8  Datapacks and compatibility     worldgen JSON others can override, tags, known mod conflicts    (hand)
9  Commands                        operator commands                                                 (gen + hand)
10 FAQ and troubleshooting                                                                           (hand)
11 Changelog                       from CHANGELOG.md                                                 (gen)
```

### 5.2 Encyclopedia (`content/`)

One page per thing a player can hold, meet or visit. Armor is one page per set (pieces as a table), building
blocks one page per family.

```
places/
  ruined-sanctum                   the dungeon: layout, halls, loot, the boss room
  ember-sanctum                    the hub dimension
  ashen-reach                      realm overview: map of zones, shrines, the gate
    crossroads
    ashen-fields
    royal-woods
    sunken-necropolis
    kingdoms-edge
    smoldering-citadel

npcs/
  ashen-maiden

bosses/                            arena, phases, moves and tells, drops, tips   (hand + gen stats/drops)
  fallen-sister
  vampire
  ash-colossus
  grieving-tree                    (+ Hanged Lantern as a section)
  crypt-keeper
  smoldering-lord

mobs/                              where found, level range, behaviour, drops    (gen stats/drops + short hand)
  overworld/  hollow, ember-wisp, ash-ghoul, ashen-zombie, ghost
  ashen-reach/  cinder-husk, ash-stalker, ashen-carrion, withered-thrall, lantern-hag, bark-hollow,
                restless-bones, crypt-moth, hollow-sentinel, grave-warden, cinder-hound,
                sparking-marksman, furnace-golem, smoldering-knight
  (zone of each mob filled from the biome spawn data, not by hand)

items/
  weapons/      sanctum-longsword, ashbearers-mace, ashen-scythe, wardens-scourge,
                royal-ash-rapier, huntsmans-crossbow
  armor/        smoldering-set, reapers-set, huntsmans-set, wardens-set,
                uniques (Scarecrow's Mask, Huntsman's Greaves, Drowned Sabatons, Furnace Heart Cuirass)
  accessories/  rings (Courier's, Ash Feathers, Fog Stalker, Delver's, Drowned Man's, Embers),
                artifacts (Last-Harvest Grain, Ploughman's Horseshoe, Royal Hound Fang, Amber Tear,
                Gravestone Medallion, Ferryman's Coin, Warden's Chain, Seal of the Ashen King)
                — one page each, plus an index table
  souls/        crying-soul, ember-heart, the four realm souls
  materials/    cinder-shard, royal-amber, grave-iron (raw + ingot), smoldering-ingot,
                smoldering-template, smoldering-resin
  consumables/  healing-flask, flask-shard, flask-ember, ash-bomb, amber-balm, amber-lantern-oil,
                whetstone, ash-of-rematch, vampire-fang
  navigation/   ruined-sanctum-map, ember-lodestar

blocks/
  functional/   ember-shrine, void-obelisk, return-obelisk, cleft-stele (+ Cleft Stone), sanctum-flagstone,
                citadel-gate, ember-lantern
  building/     ash-stone, ash-bricks, ash-tiles, ash-cobble, dead-wood, charred-wood, bone-bricks,
                grave-iron, brass, slate-tiles, misc (thatch, tar, amber block, ember glass, crypt tiles)
                — each a family table of variants with their recipes   (gen)
  ores/         whatever ores the zones carry   (gen)
```

Not on the wiki: spawn eggs, `debug_sweep`, Test Champion, set-piece blocks (they are scenery; one line on the
place page), anything in BACKLOG.

### 5.3 Page templates

| Kind | Sections |
|---|---|
| Weapon | infobox (gen: damage, speed, reach, scaling, special cost/cooldown) · What makes it itself · Special attack · How to get it (`{{recipe}}`, drop sources) · Used in (`{{usage}}`) · Tips |
| Armor set | piece table (gen) · Set bonus · How to get each piece · Tips |
| Accessory | effect (gen) · source · stacking notes |
| Material / consumable | what it does · `{{recipe}}` · sources (`{{drops}}` reverse lookup) · `{{usage}}` |
| Mob | infobox (gen: HP, damage, levels, spawn biomes/conditions) · Behaviour · Drops (`{{drops}}`) |
| Boss | infobox (gen: HP per phase, arena, rematch) · How to reach · Phases and moves (tells, how to dodge) · Drops · Strategy (melee / ranged / party) |
| Place | how to find it · map or screenshot · what is there (mobs, loot, shrines) · boss |
| Guide | free text; ends with "Next:" to the following guide |

Images: item icons from WikiToolkit's export; mob/boss/place screenshots taken by hand into `wiki/assets/`
(screenshots are the one part only a person in game can produce).

## 6. Work order (to plan as tasks later)

1. Converter skeleton + `--check` + a one-page proof published to moddedmc.wiki (also settles the open checks in §2).
2. Stats export into `wiki/_data/`; generated pages: blocks, materials, config, commands, changelog.
3. Encyclopedia stubs for every item/mob/boss (generated parts only) — the wiki is useful from here.
4. Glossary and the mod's `de_de` / `fr_fr` / `zh_cn` lang files (can run alongside 2–3).
5. Hand text: walkthrough and bosses first, then mechanics, then the rest. English first; each batch then
   translated into the four other languages.
6. Optional: in-game book target.

## Sources

- moddedmc.wiki: [about](https://moddedmc.wiki/en/about), [author guide](https://docs.moddedmc.wiki/),
  [folder structure](https://docs.moddedmc.wiki/docs/folder_structure/overview),
  [game data](https://docs.moddedmc.wiki/docs/folder_structure/game_data),
  [translations](https://docs.moddedmc.wiki/docs/folder_structure/translations),
  [components](https://docs.moddedmc.wiki/docs/components/interactive),
  [publishing](https://docs.moddedmc.wiki/docs/portal/publishing), [WikiToolkit](https://github.com/Sinytra/WikiToolkit)
- [wiki.gg on mediawiki.org](https://www.mediawiki.org/wiki/Wiki.gg/dtp),
  [Minecraft Wiki leaving Fandom](https://minecraft.fandom.com/wiki/Minecraft_Wiki:Moving_from_Fandom/First_discussion)
- Lavender: [Modrinth](https://modrinth.com/mod/lavender)
