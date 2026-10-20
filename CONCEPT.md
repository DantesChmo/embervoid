# Embervoid — Concept

> The world is fading. Ash, ruins, and whatever sleeps beneath them.

**Genre:** dark fantasy, inspired by souls-like games (mood and feel — no copying).
**Format:** pure sandbox. No storyline — players choose where to go and what to fight.
**Platform:** Minecraft 1.20.1, Fabric.

## Pillars
1. **Exploration** — dungeons of varying scale and danger generate across the world.
2. **Bosses** — each with its own arena, phases, special attacks and unique drops.
3. **Gear** — new weapons and armor worth the risk of going after them.
4. **Atmosphere** — grim blocks, a biome, sounds. Vanilla combat stays intact.

## Directions (details later)
| System     | Idea                                                                     |
|------------|--------------------------------------------------------------------------|
| Structures | Small ruins → underground crypts → surface fortresses → deep chasms      |
| Enemies    | Undead and ash-spawn living in dungeons                                  |
| Bosses     | Crypt mini-bosses, fortress lords, deep-dwelling horrors                 |
| Gear       | New boss-dropped material, weapons with special attacks, armor sets      |
| World      | Ash wasteland biome with dead trees                                      |
| Mechanics  | Bonfire-style rest point, rechargeable healing flask                     |

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
