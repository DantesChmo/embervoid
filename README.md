# Embervoid

A dark fantasy adventure mod for **Minecraft 1.20.1 (Fabric)** in the spirit of Demon's Souls: a harder overworld, a
hidden hub where you level up, and obelisks that lead into realms with zone bosses and a realm boss. See
[CONCEPT.md](CONCEPT.md); what changed per version is in [CHANGELOG.md](CHANGELOG.md).

Languages: English, Russian.

## Requirements
- Minecraft 1.20.1, Java 17
- Fabric Loader ≥ 0.15
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [GeckoLib](https://modrinth.com/mod/geckolib) 4.8.4 or newer (animated mobs and bosses)
- [playerAnimator](https://modrinth.com/mod/playeranimator) 1.0.2-rc1+1.20 or newer for 1.20.1 (player attack animations)

Install on both the client and the server.

## Features
- **A harder overworld:** every hostile mob has a level (HP, damage and XP grow with it, shown above its head); Easy
  hits like vanilla Normal; new hostiles: the Ash Ghoul (packs, a blinding ash cloud), the Hollow (a guarded
  swordsman with a telegraphed swing) and the Ember Wisp (a cave flyer that sets fires and flees light).
- **Souls-style growth:** no classes, seven stats bought one point at a time with vanilla XP from the Ashen Maiden;
  a Healing Flask with charges instead of extra flasks; stamina spent only by weapon special attacks; on death your
  XP and items wait in an Ash Remnant where you fell.
- **The Ember Sanctum:** a rare overworld dungeon (a Sanctum Map in some vanilla chests leads to one) with a guarded
  descent, loot halls with Flask Shards and two Sanctum weapons with special attacks, and the core: the Ashen Maiden,
  an Ember Shrine and the Void Obelisks.
- **Moon nights:** every third lunar cycle a full moon (stronger hostiles, Curse of the Night waves of Ashen Zombies
  or Ghosts) and a blood new moon (a red sky, vampirism, and a Vampire who becomes a boss).
- **Ashen Reach**, the first obelisk's realm: three zones in any order (Ashen Fields, Royal Woods, Sunken Necropolis)
  with their own mobs, ruins, ores and bosses, then the Sealed Gate to the Smoldering Citadel and the Smoldering
  Lord. Realm gear: the Smoldering Plate, three armour sets, four weapons, and the Reliquary with artifacts and rings.
- **Multiplayer:** progress, boss kills and loot are per player; boss health and adds grow with the number of
  fighters.

## Development
```bash
./gradlew runClient    # launch the game with the mod
./gradlew runDatagen   # generate JSON resources
./gradlew build        # build the jar into build/libs/
./gradlew runGametest  # in-world tests on a headless server
python3 tools/server_check.py  # a real dedicated server: worldgen, a bot player, the log scanned for errors
```

## Configuration
The mod reads `config/embervoid.json` (created with defaults on first launch). Missing keys are filled in, and invalid values are logged and reset to defaults. If the file is not valid JSON, it is moved to `embervoid.json.broken` and replaced with defaults. On a server, the server's config is sent to each joining client and used for that session, so the client follows the server's values.

| Key | Default | Range | Meaning |
|-----|---------|-------|---------|
| `remnant.mode` | `xp_and_items` | `off`, `xp`, `xp_and_items` | What goes into the Ash Remnant on death |
| `remnant.secondDeath` | `burn_xp` | `burn_xp`, `burn_all` | What dying again does to the previous remnant: its XP burns and the items stay, or it burns with everything |
| `stamina.max` | 100 | 1–10000 | Stamina pool size |
| `stamina.regenPerTick` | 1.0 | 0.01–1000 | Stamina restored per tick while regenerating |
| `stamina.regenDelayTicks` | 20 | 0–1200 | Ticks after spending stamina before it regenerates |
| `flask.baseCharges` | 3 | 1–20 | Charges of a fresh Healing Flask |
| `flask.maxCharges` | 8 | 1–20 | Charge cap after upgrades (at least `baseCharges`) |
| `flask.healAmount` | 8.0 | 0.5–1000 | Health per sip, in half-hearts |
| `flask.drinkTicks` | 32 | 1–200 | Ticks one sip takes |
| `stats.vitalityHealth` | 1.0 | 0–100 | Max health (half-hearts) per full Vitality point above 10 |
| `stats.enduranceStamina` | 2.0 | 0–1000 | Max stamina per full Endurance point |
| `stats.enduranceStaminaRegen` | 0.02 | 0–10 | Stamina regen per full Endurance point, as a fraction of `stamina.regenPerTick` |
| `stats.enduranceKnockbackResistance` | 0.005 | 0–1 | Knockback resistance per full Endurance point |
| `stats.strengthDamage` | 0.02 | 0–10 | Damage with STR weapons (axes, tridents half) per full Strength point, as a fraction |
| `stats.dexterityDamage` | 0.02 | 0–10 | Damage with DEX weapons (swords, bows, crossbows, tridents half) per full Dexterity point |
| `stats.dexterityAttackSpeed` | 0.005 | 0–10 | Attack speed per full Dexterity point, as a fraction |
| `stats.kindlingFlaskHeal` | 0.02 | 0–10 | Healing Flask heal per full Kindling point, as a fraction |
| `stats.resilienceToughness` | 0.1 | 0–100 | Armor toughness per full Resilience point |
| `stats.resilienceFireReduction` | 0.01 | 0–1 | Fire damage removed per full Resilience point |
| `stats.resilienceFireReductionCap` | 0.5 | 0–1 | Most fire damage Resilience can remove |
| `stats.fortuneLuck` | 0.1 | 0–100 | Luck per full Fortune point |
| `stats.fortuneExtraDrop` | 0.005 | 0–1 | Chance per full Fortune point that a mob you kill drops its loot a second time |
| `stats.fortuneExtraDropCap` | 0.25 | 0–1 | Highest extra-drop chance |
| `leveling.costBase` / `costLinear` / `costQuadratic` | 20 / 6 / 0.15 | ≥ 0 | Cost of level L → L+1 in XP points: `costBase + costLinear·L + costQuadratic·L²` |
| `leveling.statCap` | 99 | 10–999 | Highest value a stat can be levelled to |
| `leveling.levelCap` | 0 | ≥ 0 | Highest character level reachable by leveling; 0 = no cap |
| `mobs.levels.enabled` | true | true, false | Mob levels on or off |
| `mobs.levels.defaultBaseLevel` | 4 | 1–999 | Base level of a hostile mob not listed in `baseLevels` (vanilla or modded) |
| `mobs.levels.baseLevels.<entity id>` | per mob | 1–999 | Base level by entity id, e.g. `minecraft:zombie` 3, `embervoid:hollow` 15; add modded ids here |
| `mobs.levels.blocksPerLevel` / `maxDistanceBonus` | 1000 / 5 | 0–1000000 / 0–999 | Overworld: +1 level per this many blocks from world spawn, up to the bonus |
| `mobs.levels.belowZeroBonus` | 3 | 0–999 | Overworld: extra levels below y = 0 |
| `mobs.levels.dimensionBonus.<dimension id>` | Nether 5, End 8 | 0–999 | Extra levels in a dimension |
| `mobs.levels.healthPerLevel` / `damagePerLevel` / `experiencePerLevel` | 0.06 / 0.05 / 0.06 | 0–10 | Per level above 1: mob health, damage and dropped XP, as a fraction of the base |
| `mobs.levels.healthScale.<entity id>` / `damageScale.<entity id>` | per mob | 0.01–100 | A factor on a type's base health / damage under its level (unlisted: 1); e.g. zombie health 0.75, witch damage 0.5, Vindicator health 1.3; add modded ids here |
| `mobs.difficulty.easy` / `normal` / `hard` | 1.0 / 1.25 / 1.75 | 0–100 | Damage factor of hostile mobs against players per difficulty (replaces vanilla's) |
| `sanctum.hallGuardChance` / `hallArcherChance` | 0.6 / 0.3 | 0–1 | Chance of a Hollow at each side-hall spot / of a skeleton archer in a chamber, rolled at worldgen |
| `sanctum.mapChanceCartographer` / `mapChanceTemple` / `mapChanceRuinedPortal` / `mapChanceShipwreck` | 0.25 / 0.1 / 0.08 / 0.25 | 0–1 | Chance of a Sanctum Map in those vanilla chests |
| `sanctum.mapSearchRadius` | 64 | 8–200 | How far (in chunks) a Sanctum Map looks for a Sanctum |
| `sanctumGear.cleaveRange` / `cleaveArc` / `cleaveMultiplier` | 3 / 90 / 1.25 | 1–8 / 10–360 / 0–10 | Sanctum Longsword's Wide Cleave: reach (blocks), arc in front (degrees), share of the weapon's hit each target takes |
| `sanctumGear.cleaveStamina` / `cleaveCooldown` | 25 / 60 | 0–1000 / 0–1200 | Wide Cleave's stamina cost and cooldown (ticks) |
| `sanctumGear.slamRadius` / `slamMultiplier` / `slamKnockback` | 3 / 0.75 / 0.8 | 1–8 / 0–10 / 0–4 | Ashbearer's Mace's Ground Slam: ring radius, share of the weapon's hit, knockback strength |
| `sanctumGear.slamSlownessTicks` / `slamSlownessAmplifier` | 40 / 1 | 0–1200 / 0–9 | Ground Slam's Slowness: duration (ticks) and amplifier (1 = Slowness II) |
| `sanctumGear.slamStamina` / `slamCooldown` | 30 / 100 | 0–1000 / 0–1200 | Ground Slam's stamina cost and cooldown (ticks) |
| `realm.depthsOpen` | true | true, false | Whether an unsealed Sealed Gate leads into the depths (off: "the depths are still forming"; was off in 0.3.0) |
| `boss.healthPerExtraFighter` | 0.5 | 0–10 | Boss health bonus per fighter beyond the first: HP × (1 + this·(n − 1)) |
| `boss.addsPerExtraFighter` | 1 | 0–10 | Extra adds per fighter beyond the first |
| `boss.addLevelOffset` | 3 | −50–50 | Level of a boss's adds relative to the boss |
| `boss.targetSwitchMinSeconds` / `targetSwitchMaxSeconds` | 8 / 10 | 0.5–600 | How often a boss switches to another fighter |
| `boss.repeatSoulChance` | 0.25 | 0–1 | Chance of the boss's soul for a fighter who has beaten it before (never in a rematch) |
| `reachMobs.spawns.<mob>.weight` / `minGroup` / `maxGroup` | per mob | ≥ 0 | Natural spawns of Ashen Reach mobs in their zone; weight 0 turns a mob's spawning off |
| `gear.bleeding.*`, `gear.smoldering.*`, `gear.sets.*`, `gear.uniques.*` | | | Bleeding (duration, levels, damage); Smoldering Plate fire reduction; armour set bonuses; unique pieces |
| `gear.weapons.*`, `gear.crossbow.*`, `gear.consumables.*` | | | Realm weapons' passives and specials (stamina, cooldowns, reach effects); Huntsman's Crossbow; consumables |
| `gear.loot.*` | | | Material, set-piece and unique-piece drop chances from Ashen Reach mobs |
| `reliquary.slots.<kind>` | 2 | 0–8 | Reliquary slots per kind (`artifact`, `ring`) |
| `reliquary.artifacts.points` / `sealPoints` | 3 / 1 | 0–99 | Stat points of a one-stat artifact / of the Seal of the Ashen King (each stat) |
| `reliquary.rings.*` | | | Ring effects: sprint bonus, safe fall, fog clearing, Haste level, ignite seconds |
| `reliquary.drops.chestArtifact` / `chestRing` / `eliteArtifact` / `eliteRing` | 0.08 / 0.05 / 0.02 / 0.01 | 0–1 | Accessory drop chances in treasure chests and from elites |
| `moon.enabled` | true | true, false | Moon nights on or off |
| `moon.cycleInterval` | 3 | 1–1000 | A moon-night cycle every N lunar cycles (8 days each); the world's first full moon never counts |
| `moon.fallbackMinutes` | 30 | 1–1440 | Real-time minutes after which a moon night ends even if its goal is not met |
| `moon.fullMoon.levelBonus` | 3 | 0–100 | Extra levels for overworld hostiles spawning on a full moon |
| `moon.fullMoon.followRange` | 1.5 | 1–10 | Follow-range multiplier for hostiles on a full moon |
| `moon.blood.vampirism` | 0.25 | 0–10 | Share of the damage a hostile deals to a player that heals it on a blood new moon |
| `nightMobs.zombieLevelBonus` | 5 | 0–max level | Ashen Zombie level = spot level + this |
| `nightMobs.ghostMissChance` | 0.15 | 0–1 | Chance that a hit on a Ghost misses |
| `nightMobs.maxZombies` | 10 | 0–100 | Most Ashen Zombies in one player's wave (one per curse stack) |
| `nightMobs.ghostCount` | 3 | 0–100 | Ghosts sent to a player with no curse |
| `nightMobs.spawnMinDistance` / `spawnMaxDistance` | 16 / 32 | 1–64 | How far from the player a wave spawns |
| `vampire.provokeMinutes` | 5 | 0.1–60 | Minutes before the Vampire nearest to a player turns if nobody provoked one |
| `vampire.groupRadius` | 64 | 8–512 | Players this close share one group of Vampires |
| `vampire.groupMin` / `groupMax` | 1 / 3 | 1–10 | Vampires per group |
| `vampire.bossMinLevel` | 15 | 1–999 | Lowest Vampire boss level |
| `vampire.bossCapNoRealm` | 20 | 1–999 | Highest Vampire boss level when nobody taking part has unlocked a realm |
| `vampire.firstKillShards` | 2 | 0–64 | Flask Shards for a player's first Vampire kill |
| `vampire.repeatShardChance` | 0.5 | 0–1 | Chance of one Flask Shard on later kills |

Every key, with its current value, is written to `config/embervoid.json`; the grouped rows above list the sections
whose keys are documented in the file itself.

A "full point" follows the soft caps: stat values up to 20 count fully, 21–40 at half, 41–99 at a fifth.

Structure spacing is not in the config: it lives in the `structure_set` JSON files, which datapacks can override.

Client preferences live in `config/embervoid-client.json` (same rules; never read by a server, not synced):

| Key | Default | Range | Meaning |
|-----|---------|-------|---------|
| `staminaBar.hideWhenFull` | true | true, false | Hide the stamina bar while stamina is full |

## Debug commands
- `/embervoid data` (operator) — show your Embervoid player data as stored on the server.
- `/embervoid data flaskChargeUpgrades <n>` (operator) — set a test value and sync it to the client.
- `/embervoid stamina` (operator) — show your stamina.
- `/embervoid stamina spend <amount>` (operator) — spend stamina as an action would (survival/adventure only).
- `/embervoid stamina set <value>` (operator) — set your stamina.
- `/embervoid stats` (operator) — show your stats, level, next level cost and what the stats give.
- `/embervoid levelup <stat>` (operator) — level a stat up as the Maiden will, paying XP points.
- `/embervoid setstat <stat> <value>` (operator) — set a stat for free (1–999).
- `/embervoid remnant` (operator) — move your items and XP into an Ash Remnant at your feet, as a death would.
- `/embervoid moon status | start full | start blood | stop | setcycle <n> | curse <player> <stacks>` (operator) — moon nights: state, start or stop one now, jump the clock to cycle n, set Curse of the Night stacks.
- `/embervoid wave status | spawn <players> | clear <players>` (operator) — full moon waves.
- `/embervoid vampire status | provoke` (operator) — blood new moon Vampires and the Vampire fight.
- `/embervoid realm enter <realm> [toShrine] | leave | where | arenas | roads | data | edge` (operator) — realm travel and layout: enter a realm, leave it for the hub, where you are, its boss arenas, roads and world edge.
- `/embervoid hub [check | leave]` (operator) — go to the hub, check it is whole, or take the hub's fallback way out, ignoring the lit stele (to where you last left the overworld, else your bed or the world spawn).
- `/embervoid stele [kindle <pos>]` (operator) — show the world's lit Cleft Stele, or kindle the cold stele at a position as a Crying Soul would.
- `/embervoid progress <player> [reset | unlock | complete | boss <id>]` (operator) — show or change realm progress.
- `/embervoid boss fights` (operator) — running boss fights.
- `/embervoid reliquary <player> [open|clear]` (operator) — show, open or empty a player's Reliquary.
- `/embervoid gear bleed <targets> [stacks]` (operator) — add Bleeding levels as that many hits would.
- `/embervoid moblevel <targets> [level]` (operator) — show mob levels (and the level a new spawn here would get), or override them.
- `/embervoid perf ticks | chunks | entities` (operator) — tick time, loaded chunks and entity counts (used by `tools/server_check.py --perf`).
- `/embervoid_client data` — show the player data as your client sees it.
- `embervoid:debug_sweep` (dev environment only, `/give` or the creative tab) — test weapon for the special-attack framework: right-click does a knockback sweep for 30 stamina with a 1 s cooldown.
