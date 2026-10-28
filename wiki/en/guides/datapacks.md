---
title: Datapacks and compatibility
kind: guide
order: 8
---

Much of Embervoid is data: a datapack can change where its dungeons appear, what its chests hold and how other mods'
content fits in. Paths below are inside a datapack's `data/` folder.

## Worldgen you can override

| File | What it controls |
|---|---|
| `embervoid/worldgen/structure_set/ember_sanctum.json` | how often Ruined Sanctums appear: `spacing` 22 and `separation` 8 chunks by default (about one per 300–400 blocks). Raise both for rarer dungeons. |
| `embervoid/tags/worldgen/biome/has_structure/ember_sanctum.json` | the biomes a Ruined Sanctum can stand in |
| `embervoid/worldgen/structure/ember_sanctum.json`, `embervoid/worldgen/template_pool/sanctum/*` | the dungeon itself and its pieces |
| `embervoid/tags/worldgen/structure/on_sanctum_maps.json` | the structures a Sanctum Map points to |
| `embervoid/worldgen/biome/ashen_reach_*.json` | the realm's look per zone: colors, particles, ambient sound |
| `embervoid/worldgen/configured_feature/ashen_reach/*`, `placed_feature/ashen_reach/*` | the realm's ores, trees, debris and roads |

Be careful with the realm's dimension, noise settings and structure sets (`embervoid/dimension/ashen_reach.json`,
`embervoid/worldgen/noise_settings/ashen_reach.json`, `embervoid/worldgen/structure_set/ashen_reach_*`). The realm's
zones, arenas, gate and Citadel are laid out by code with the same numbers; changing one side alone breaks the realm.
Leave the hub and the Vampire's arena (`embervoid/dimension/hub.json`, `vampire_arena.json`) alone.

The Ashen Reach mobs do not spawn from the biome files (their spawner lists are empty on purpose). Change their
spawns with `reachMobs.spawns` in the [config](configuration.md).

## Loot tables

- `embervoid/loot_tables/chests/sanctum_outer_hall.json`: the Ruined Sanctum's hall chests.
- `embervoid/loot_tables/gameplay/fallen_sister_first_clear.json`: the extra loot of a player's first Sanctum clear.
- `embervoid/loot_tables/chests/ashen_reach/*`: the realm's chests, three kinds per zone (common, arena, treasure).
- `embervoid/loot_tables/entities/*`: mob and boss drops.

Some drops are added by the mod on top of these tables: the realm materials, armor set pieces and unique pieces on the
realm mobs, the rings and artifacts in treasure chests and on elites, and the Sanctum Map in vanilla chests. **If your
datapack replaces one of those tables, these additions are not made to it**: your table decides alone. Their chances
are in the config instead (`gear.loot.*`, `reliquary.drops.*`, `sanctum.mapChance*`).

## Tags

| Tag | Meaning |
|---|---|
| `embervoid:unleveled` (entity types) | mobs that never get a level: the Ender Dragon, the Wither, the Warden, the Elder Guardian. Add other mods' bosses here. |
| `embervoid:bleed_immune` (entity types) | mobs that never bleed |
| `embervoid:breaks_guard` (items) | weapons that break a Hollow's guard like an axe (the Ashbearer's Mace) |
| `embervoid:artifacts`, `embervoid:rings` (items) | what fits the Reliquary's artifact and ring slots |
| `embervoid:realm_ores` (blocks) | the ores the Delver's Ring gives Haste for |
| `embervoid:dead_logs` (blocks, items) | the dead wood logs |
| `embervoid:ashen_reach*` (biomes) | the realm's biomes, all and per zone |

## Other mods

No conflicts are known so far. Things to keep in mind:

- **Mobs from other mods** get a level like vanilla mobs, at the default base level. Give them a proper one in
  `mobs.levels.baseLevels`, and put their bosses into `embervoid:unleveled` if they should stay as their author made
  them.
- **Grave or corpse mods** also act on death. Run one system: either set `remnant.mode` to `off`, or leave the other
  mod out.
- **Damage and difficulty mods:** Embervoid replaces vanilla's difficulty factor for mob damage to players
  (`mobs.difficulty.*`). Another mod that changes the same thing may stack with it.
- Embervoid's stat bonuses use vanilla attributes where vanilla has one (max health, attack speed, knockback
  resistance, armor toughness, luck), so mods that read attributes see them.

Next: [Commands](commands.md)
