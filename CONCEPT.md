# Embervoid — Concept

> The world is fading. Ash, ruins, and whatever sleeps beneath them.

**Genre:** dark fantasy, inspired by souls-like games (mood and feel — no copying).
**Format:** Demon's Souls / Bloodborne structure in a Divine RPG / Twilight Forest style mod. No storyline —
progression comes from places, gates and bosses. Full design: [DESIGN.md](DESIGN.md).
**Platform:** Minecraft 1.20.1, Fabric.

## Pillars
1. **A harder world** — vanilla mobs hit harder even on Easy, new hostiles roam the overworld, every mob has a level.
2. **The hub** — the Ember Sanctum, a rare overworld dungeon with the leveling NPC and the obelisks.
3. **Realms** — each obelisk opens a dimension with 4 zones; 3 zone bosses unlock the final zone and the realm boss, which unlocks the next realm.
4. **Growth** — souls-style stats bought with vanilla XP, no classes; each realm drops a better gear tier.
5. **Atmosphere** — grim blocks, sounds, a distinct style per realm. Vanilla combat stays intact; stamina is spent only by weapon special attacks.

## Directions (order in [ROADMAP.md](ROADMAP.md))
| System     | Idea                                                                          |
|------------|-------------------------------------------------------------------------------|
| Overworld  | Mob levels, harder difficulty, Ash Ghoul, Hollow, Ember Wisp                  |
| Hub        | Ember Sanctum: Ashen Maiden (leveling), Void Obelisks, Ember Shrine           |
| RPG        | 7 stats (VIT END STR DEX KIN RES FOR), level paid in XP points, no classes    |
| Realm 1    | Ashen Reach: Ashen Fields, Royal Woods, Sunken Necropolis → Smoldering Citadel |
| Gear       | One material tier per realm, boss uniques, set bonuses (weapons: separate doc) |
| Mechanics  | Stamina for specials, healing flask, Ash Remnant on death, shrines in realms  |

## Dependencies
Fabric API, GeckoLib (animated mobs and bosses). No other required mods.

## Code layout
```
src/main/java/com/embervoid/
  Embervoid.java        — common entrypoint
  registry/             — ModItems, ModBlocks, ModEntities, ModSounds, ModItemGroups, ModStructures
  item/ block/ entity/ world/ util/ mixin/
src/client/java/com/embervoid/
  client/EmbervoidClient.java        — rendering, HUD, keybinds
  client/EmbervoidDataGenerator.java — datagen
src/main/resources/
  assets/embervoid/  — textures, models, translations (en_us, ru_ru), sounds
  data/embervoid/    — recipes, loot, worldgen (structures: jigsaw + .nbt)
```
