---
title: Configuration
kind: guide
order: 7
---

## Where the settings live

| File | What | Who reads it |
|---|---|---|
| `config/embervoid.json` | the gameplay rules: every key below | the server (in singleplayer, your own game) |
| `config/embervoid-client.json` | your display preferences | your client only |

Both files are created with their defaults the first time the game starts. Embervoid has no in-game settings screen:
edit the JSON with a text editor **while the game or server is stopped**, then start it again. The file is read at
startup.

- A missing key is added with its default.
- An invalid value (out of range, wrong type) is logged and reset to its default.
- A file that is not valid JSON is moved to `embervoid.json.broken` and replaced with defaults.
- On a server, the server's `embervoid.json` is sent to every player who joins and their client follows it for that
  session. Players cannot change the rules with their own file.

When a new version changes a default, the value already in your file stays. Delete a key to take the new default.

A "full point" of a stat follows the soft caps: values up to 20 count fully, 21–40 at half, 41–99 at a fifth.

Structure spacing (how often Ruined Sanctums appear) is not in this file; change it with a datapack (see
[Datapacks and compatibility](datapacks.md)).

## The keys worth changing

- **How harsh death is:** `remnant.mode` and `remnant.secondDeath`. Set `remnant.secondDeath` to `burn_all` for a
  true souls-like, or `remnant.mode` to `off` to play with vanilla deaths.
- **How hard mobs hit:** `mobs.difficulty.easy`, `normal` and `hard`. Set them to 0.5, 1.0 and 1.5 for roughly vanilla's
  difficulty scaling.
- **How fast mobs grow:** `mobs.levels.healthPerLevel` and `damagePerLevel`, or `mobs.levels.enabled` to turn levels
  off.
- **Other mods' mobs:** add their entity ids to `mobs.levels.baseLevels`, for example `"othermod:ogre": 10`.
- **Moon nights:** `moon.enabled` to turn them off, `moon.cycleInterval` to make them rarer or more frequent.
- **Your flask:** `flask.baseCharges`, `flask.maxCharges` and `flask.healAmount`.
- **Leveling pace:** the three `leveling.cost*` keys, `leveling.levelCap`.
- **Party bosses:** `boss.healthPerExtraFighter` and `boss.addsPerExtraFighter`.
- **Performance:** `setpieces.animate` set to false stops every moving set piece.

## Client settings

In `config/embervoid-client.json`:

`staminaBar.hideWhenFull` — hide the stamina bar while stamina is full (default true).

`hud.showMobLevel` — show "Lv. N" next to the name of the mob you look at (default true).

In Options → Music & Sounds, **Boss Music** sets the boss themes' volume (default 100 %), independent of Music. In
Options → Accessibility Settings, **Camera Shake** sets how much heavy blows shake the camera, and **Hit Flashes**
turns the bright flashes of heavy blows on or off. These are saved with the vanilla options.

## Every key

Grouped by section of `embervoid.json`. A key written `a.b.c` is the key `c` inside the object `b` inside `a`.

### remnant: Death and the Ash Remnant

`remnant.mode` — What goes into the Ash Remnant on death: `xp_and_items`, `xp` (items drop as in vanilla) or `off` (vanilla death).
{{config remnant.mode}}

`remnant.secondDeath` — What a new death does to your previous remnant: `burn_xp` (its XP burns, its items stay) or `burn_all` (it burns with everything).
{{config remnant.secondDeath}}

### stamina: Stamina

`stamina.max` — Stamina pool at base Endurance.
{{config stamina.max}}

`stamina.regenPerTick` — Stamina restored per tick once it regenerates, at base Endurance.
{{config stamina.regenPerTick}}

`stamina.regenDelayTicks` — Ticks after spending stamina before it starts to regenerate.
{{config stamina.regenDelayTicks}}

### flask: Healing Flask

`flask.baseCharges` — Charges of a fresh Healing Flask.
{{config flask.baseCharges}}

`flask.maxCharges` — Charge cap with Flask Shards (never below `baseCharges`).
{{config flask.maxCharges}}

`flask.healAmount` — Health per sip, in half-hearts.
{{config flask.healAmount}}

`flask.drinkTicks` — Ticks one sip takes.
{{config flask.drinkTicks}}

### stats: Stats

`stats.vitalityHealth` — Max health (half-hearts) per full Vitality point above 10.
{{config stats.vitalityHealth}}

`stats.enduranceStamina` — Max stamina per full Endurance point.
{{config stats.enduranceStamina}}

`stats.enduranceStaminaRegen` — Stamina regeneration per full Endurance point, as a fraction of `stamina.regenPerTick`.
{{config stats.enduranceStaminaRegen}}

`stats.enduranceKnockbackResistance` — Knockback resistance per full Endurance point.
{{config stats.enduranceKnockbackResistance}}

`stats.strengthDamage` — Damage with Strength weapons per full Strength point, as a fraction.
{{config stats.strengthDamage}}

`stats.dexterityDamage` — Damage with Dexterity weapons per full Dexterity point, as a fraction.
{{config stats.dexterityDamage}}

`stats.dexterityAttackSpeed` — Attack speed per full Dexterity point, as a fraction.
{{config stats.dexterityAttackSpeed}}

`stats.kindlingFlaskHeal` — Healing Flask heal per full Kindling point, as a fraction.
{{config stats.kindlingFlaskHeal}}

`stats.resilienceToughness` — Armor toughness per full Resilience point.
{{config stats.resilienceToughness}}

`stats.resilienceFireReduction` — Fire damage removed per full Resilience point.
{{config stats.resilienceFireReduction}}

`stats.resilienceFireReductionCap` — The most fire damage Resilience can remove.
{{config stats.resilienceFireReductionCap}}

`stats.fortuneLuck` — Luck per full Fortune point.
{{config stats.fortuneLuck}}

`stats.fortuneExtraDrop` — Chance per full Fortune point that a mob you kill drops its loot a second time.
{{config stats.fortuneExtraDrop}}

`stats.fortuneExtraDropCap` — The highest extra-drop chance.
{{config stats.fortuneExtraDropCap}}

### leveling: Leveling

`leveling.costBase` — Level cost, constant part: level L to L + 1 costs `costBase + costLinear·L + costQuadratic·L²` XP points.
{{config leveling.costBase}}

`leveling.costLinear` — Level cost, the part that grows with L.
{{config leveling.costLinear}}

`leveling.costQuadratic` — Level cost, the part that grows with L².
{{config leveling.costQuadratic}}

`leveling.statCap` — Highest value a stat can be leveled to.
{{config leveling.statCap}}

`leveling.levelCap` — Highest character level; 0 = no cap.
{{config leveling.levelCap}}

### mobs: Mob levels and difficulty

`mobs.levels.enabled` — Mob levels on or off. Off: no new levels, and existing ones do nothing.
{{config mobs.levels.enabled}}

`mobs.levels.defaultBaseLevel` — Base level of any hostile mob not listed in `baseLevels`, vanilla or modded.
{{config mobs.levels.defaultBaseLevel}}

`mobs.levels.baseLevels` — Base level by entity id, e.g. `"minecraft:zombie": 3`. Add other mods' mobs here.
{{config mobs.levels.baseLevels}}

`mobs.levels.blocksPerLevel` — Overworld: +1 level per this many blocks from the world spawn.
{{config mobs.levels.blocksPerLevel}}

`mobs.levels.maxDistanceBonus` — Overworld: the most levels distance can add.
{{config mobs.levels.maxDistanceBonus}}

`mobs.levels.belowZeroBonus` — Overworld: extra levels below y = 0.
{{config mobs.levels.belowZeroBonus}}

`mobs.levels.dimensionBonus` — Extra levels by dimension id (the Nether and the End by default). Add other mods' dimensions here.
{{config mobs.levels.dimensionBonus}}

`mobs.levels.healthPerLevel` — Health per level above 1, as a fraction of the base.
{{config mobs.levels.healthPerLevel}}

`mobs.levels.damagePerLevel` — Damage per level above 1, as a fraction of the base.
{{config mobs.levels.damagePerLevel}}

`mobs.levels.experiencePerLevel` — Dropped XP per level above 1, as a fraction of the base.
{{config mobs.levels.experiencePerLevel}}

`mobs.levels.healthScale` — A factor on a mob type's base health under its level, by entity id (unlisted: 1).
{{config mobs.levels.healthScale}}

`mobs.levels.damageScale` — A factor on a mob type's base damage under its level, by entity id (unlisted: 1).
{{config mobs.levels.damageScale}}

`mobs.difficulty.easy` — Damage factor of hostile mobs against players on Easy (replaces vanilla's).
{{config mobs.difficulty.easy}}

`mobs.difficulty.normal` — The same on Normal.
{{config mobs.difficulty.normal}}

`mobs.difficulty.hard` — The same on Hard.
{{config mobs.difficulty.hard}}

### sanctum: Ruined Sanctum

`sanctum.mapChanceCartographer` — Chance of a Sanctum Map in a village cartographer chest.
{{config sanctum.mapChanceCartographer}}

`sanctum.mapChanceTemple` — The same in a village temple chest.
{{config sanctum.mapChanceTemple}}

`sanctum.mapChanceRuinedPortal` — The same in a ruined portal chest.
{{config sanctum.mapChanceRuinedPortal}}

`sanctum.mapChanceShipwreck` — The same in a shipwreck map chest.
{{config sanctum.mapChanceShipwreck}}

`sanctum.mapSearchRadius` — How far, in chunks, a new Sanctum Map looks for a Sanctum.
{{config sanctum.mapSearchRadius}}

`sanctum.hallGuardChance` — Chance that a side-hall guard spot holds a Hollow, rolled at worldgen.
{{config sanctum.hallGuardChance}}

`sanctum.hallArcherChance` — Chance that a side-hall archer spot holds a skeleton, rolled at worldgen.
{{config sanctum.hallArcherChance}}

### sanctumGear: Sanctum weapons

`sanctumGear.cleaveRange` — Wide Cleave (Sanctum Longsword): reach in blocks.
{{config sanctumGear.cleaveRange}}

`sanctumGear.cleaveArc` — Wide Cleave: arc in front of you, in degrees.
{{config sanctumGear.cleaveArc}}

`sanctumGear.cleaveMultiplier` — Wide Cleave: share of the weapon's hit each target takes.
{{config sanctumGear.cleaveMultiplier}}

`sanctumGear.cleaveStamina` — Wide Cleave: stamina cost.
{{config sanctumGear.cleaveStamina}}

`sanctumGear.cleaveCooldown` — Wide Cleave: cooldown in ticks.
{{config sanctumGear.cleaveCooldown}}

`sanctumGear.slamRadius` — Ground Slam (Ashbearer's Mace): ring radius.
{{config sanctumGear.slamRadius}}

`sanctumGear.slamMultiplier` — Ground Slam: share of the weapon's hit.
{{config sanctumGear.slamMultiplier}}

`sanctumGear.slamKnockback` — Ground Slam: knockback strength.
{{config sanctumGear.slamKnockback}}

`sanctumGear.slamSlownessTicks` — Ground Slam: Slowness duration in ticks.
{{config sanctumGear.slamSlownessTicks}}

`sanctumGear.slamSlownessAmplifier` — Ground Slam: Slowness amplifier (1 = Slowness II).
{{config sanctumGear.slamSlownessAmplifier}}

`sanctumGear.slamStamina` — Ground Slam: stamina cost.
{{config sanctumGear.slamStamina}}

`sanctumGear.slamCooldown` — Ground Slam: cooldown in ticks.
{{config sanctumGear.slamCooldown}}

### boss: Bosses

`boss.healthPerExtraFighter` — Boss health bonus per fighter beyond the first: health × (1 + this × (fighters − 1)).
{{config boss.healthPerExtraFighter}}

`boss.addsPerExtraFighter` — Extra adds per fighter beyond the first.
{{config boss.addsPerExtraFighter}}

`boss.addLevelOffset` — How many levels below the boss its adds are.
{{config boss.addLevelOffset}}

`boss.targetSwitchMinSeconds` — Shortest time between two target switches of a boss (with two fighters or more).
{{config boss.targetSwitchMinSeconds}}

`boss.targetSwitchMaxSeconds` — Longest time between two target switches.
{{config boss.targetSwitchMaxSeconds}}

`boss.repeatSoulChance` — Chance of the boss's soul for a fighter who had already beaten it (never in a rematch).
{{config boss.repeatSoulChance}}

### reachMobs: Ashen Reach spawns

`reachMobs.spawns` — Natural spawns of the Ashen Reach mobs in their zone: `weight`, `minGroup` and `maxGroup` per mob. Weight 0 turns a mob's spawning off. Read at startup.
{{config reachMobs.spawns}}

### gear: Realm gear

`gear.bleeding.durationTicks` — How long Bleeding lasts; every new hit starts it again.
{{config gear.bleeding.durationTicks}}

`gear.bleeding.maxLevel` — Highest Bleeding level.
{{config gear.bleeding.maxLevel}}

`gear.bleeding.damagePerLevel` — Bleeding damage per second per level, ignoring armor.
{{config gear.bleeding.damagePerLevel}}

`gear.smoldering.fireReductionPerPiece` — Share of fire damage each worn Smoldering Plate piece takes away.
{{config gear.smoldering.fireReductionPerPiece}}

`gear.sets.reaperSpeed` — Reaper's set: movement speed bonus (share of base).
{{config gear.sets.reaperSpeed}}

`gear.sets.huntsmanProjectile` — Huntsman's set: projectile damage bonus.
{{config gear.sets.huntsmanProjectile}}

`gear.sets.wardenKnockbackResist` — Warden's set: knockback resistance added.
{{config gear.sets.wardenKnockbackResist}}

`gear.sets.wardenBlockingReduction` — Warden's set: share of damage taken away while blocking with a shield.
{{config gear.sets.wardenBlockingReduction}}

`gear.uniques.scarecrowDetection` — Scarecrow's Mask: share of the usual distance at which mobs notice you.
{{config gear.uniques.scarecrowDetection}}

`gear.uniques.greavesSpeedTicks` — Huntsman's Greaves: ticks of speed after a kill.
{{config gear.uniques.greavesSpeedTicks}}

`gear.uniques.greavesSafeFallDamage` — Huntsman's Greaves: fall damage taken away (4 = none from up to 7 blocks).
{{config gear.uniques.greavesSafeFallDamage}}

`gear.uniques.novaHealthFraction` — Furnace Heart Cuirass: the nova goes off below this share of your max health.
{{config gear.uniques.novaHealthFraction}}

`gear.uniques.novaRadius` — Furnace Heart Cuirass: nova radius.
{{config gear.uniques.novaRadius}}

`gear.uniques.novaDamage` — Furnace Heart Cuirass: ember damage to each mob in the nova.
{{config gear.uniques.novaDamage}}

`gear.uniques.novaCooldownTicks` — Furnace Heart Cuirass: ticks between two novas.
{{config gear.uniques.novaCooldownTicks}}

`gear.weapons.kindlingPerPoint` — Ember damage bonus per full Kindling point.
{{config gear.weapons.kindlingPerPoint}}

`gear.weapons.sweepRadius` — Ashen Scythe: sweep radius around the target.
{{config gear.weapons.sweepRadius}}

`gear.weapons.sweepShare` — Ashen Scythe: share of the hit the sweep deals.
{{config gear.weapons.sweepShare}}

`gear.weapons.sweepStamina` — Ashen Scythe: stamina given back per enemy a sweep or Harvest catches.
{{config gear.weapons.sweepStamina}}

`gear.weapons.waveLength` — Ashen Scythe: length of the ash wave from a fully charged hit.
{{config gear.weapons.waveLength}}

`gear.weapons.waveWidth` — Ashen Scythe: ash wave width.
{{config gear.weapons.waveWidth}}

`gear.weapons.waveDamage` — Ashen Scythe: ash wave ember damage.
{{config gear.weapons.waveDamage}}

`gear.weapons.harvestRadius` — Harvest: spin radius.
{{config gear.weapons.harvestRadius}}

`gear.weapons.harvestStamina` — Harvest: stamina cost.
{{config gear.weapons.harvestStamina}}

`gear.weapons.harvestCooldown` — Harvest: cooldown in ticks.
{{config gear.weapons.harvestCooldown}}

`gear.weapons.lashPull` — Lash: blocks the target is pulled.
{{config gear.weapons.lashPull}}

`gear.weapons.lashStamina` — Lash: stamina cost.
{{config gear.weapons.lashStamina}}

`gear.weapons.lashCooldown` — Lash: cooldown in ticks.
{{config gear.weapons.lashCooldown}}

`gear.weapons.lungeDash` — Lunge: dash length in blocks.
{{config gear.weapons.lungeDash}}

`gear.weapons.lungeStamina` — Lunge: stamina cost.
{{config gear.weapons.lungeStamina}}

`gear.weapons.lungeCooldown` — Lunge: cooldown in ticks.
{{config gear.weapons.lungeCooldown}}

`gear.weapons.rampPerHit` — Royal Ash Rapier: damage added per full-charge hit in a row on the same target.
{{config gear.weapons.rampPerHit}}

`gear.weapons.rampMax` — Royal Ash Rapier: the most the ramp adds.
{{config gear.weapons.rampMax}}

`gear.weapons.rampWindowTicks` — Royal Ash Rapier: longest gap between two hits that keeps the ramp.
{{config gear.weapons.rampWindowTicks}}

`gear.weapons.whipTipBand` — Warden's Scourge: the length of the tip, the last blocks of the lash, that cracks.
{{config gear.weapons.whipTipBand}}

`gear.weapons.whipTipMultiplier` — Warden's Scourge: damage factor of a hit with the tip.
{{config gear.weapons.whipTipMultiplier}}

`gear.weapons.whipTipStunTicks` — Warden's Scourge: a tip hit stops the mob and breaks its wind-up for this many ticks.
{{config gear.weapons.whipTipStunTicks}}

`gear.crossbow.chargeTicks` — Huntsman's Crossbow: charge time without Quick Charge.
{{config gear.crossbow.chargeTicks}}

`gear.crossbow.boltDamage` — Huntsman's Crossbow: damage of a bolt at full speed.
{{config gear.crossbow.boltDamage}}

`gear.crossbow.punch` — Huntsman's Crossbow: knockback level of the bolts.
{{config gear.crossbow.punch}}

`gear.crossbow.pierce` — Huntsman's Crossbow: extra mobs a bolt passes through.
{{config gear.crossbow.pierce}}

`gear.crossbow.openerMultiplier` — Huntsman's Crossbow: damage factor into a mob that is not targeting you.
{{config gear.crossbow.openerMultiplier}}

`gear.consumables.bombRadius` — Ash Bomb: radius of the fire ring.
{{config gear.consumables.bombRadius}}

`gear.consumables.bombDamage` — Ash Bomb: ember damage.
{{config gear.consumables.bombDamage}}

`gear.consumables.bombFireSeconds` — Ash Bomb: seconds the mobs it catches burn.
{{config gear.consumables.bombFireSeconds}}

`gear.consumables.resinSeconds` — Smoldering Resin: how long a coated weapon ignites what it hits.
{{config gear.consumables.resinSeconds}}

`gear.consumables.resinFireSeconds` — Smoldering Resin: seconds a target burns.
{{config gear.consumables.resinFireSeconds}}

`gear.consumables.balmRegenTicks` — Amber Balm: duration of its Regeneration.
{{config gear.consumables.balmRegenTicks}}

`gear.consumables.oilMinutes` — Amber Lantern Oil: how long a lantern burns.
{{config gear.consumables.oilMinutes}}

`gear.consumables.whetstoneShare` — Whetstone: share of max durability repaired.
{{config gear.consumables.whetstoneShare}}

`gear.consumables.cinderFuelTicks` — Cinder Shard as furnace fuel, in ticks (coal is 1600). Read at startup.
{{config gear.consumables.cinderFuelTicks}}

`gear.consumables.flaskEmberBonus` — Flask Ember: extra flask healing.
{{config gear.consumables.flaskEmberBonus}}

`gear.loot.fodderMaterial` — Chance of the zone's material from a natural Ashen Reach mob.
{{config gear.loot.fodderMaterial}}

`gear.loot.eliteMaterial` — Chance of 1–2 of the zone's material from a placed tank or elite.
{{config gear.loot.eliteMaterial}}

`gear.loot.fodderSet` — Chance of a set piece from a natural mob (player kills only).
{{config gear.loot.fodderSet}}

`gear.loot.eliteSet` — Chance of a set piece from a placed tank or elite.
{{config gear.loot.eliteSet}}

`gear.loot.fodderUnique` — Chance of the zone's unique piece from its mobs.
{{config gear.loot.fodderUnique}}

`gear.loot.eliteUnique` — Chance of the zone's unique piece from its elite.
{{config gear.loot.eliteUnique}}

`gear.loot.lootingBonus` — Chance added per Looting level (a quarter of it for unique pieces).
{{config gear.loot.lootingBonus}}

### reliquary: Reliquary

`reliquary.slots` — Reliquary slots per kind: `artifact` and `ring` (0 hides a kind, at most 8).
{{config reliquary.slots}}

`reliquary.artifacts.points` — Stat points of a one-stat artifact.
{{config reliquary.artifacts.points}}

`reliquary.artifacts.sealPoints` — Points the Seal of the Ashen King adds to every stat.
{{config reliquary.artifacts.sealPoints}}

`reliquary.rings.courierSprintBonus` — Courier's Ring: extra sprint speed.
{{config reliquary.rings.courierSprintBonus}}

`reliquary.rings.featherSafeFall` — Ring of Ash Feathers: falls up to this many blocks do no damage.
{{config reliquary.rings.featherSafeFall}}

`reliquary.rings.featherMaxFallSpeed` — Ring of Ash Feathers: fastest fall, blocks per tick.
{{config reliquary.rings.featherMaxFallSpeed}}

`reliquary.rings.fogStalkerClearing` — Ring of the Fog Stalker: how many times further out the realm's fog starts and ends.
{{config reliquary.rings.fogStalkerClearing}}

`reliquary.rings.delverHasteLevel` — Delver's Ring: Haste level while mining realm ores.
{{config reliquary.rings.delverHasteLevel}}

`reliquary.rings.embersFireSeconds` — Ring of Embers: seconds a melee hit sets the target on fire.
{{config reliquary.rings.embersFireSeconds}}

`reliquary.drops.chestArtifact` — Chance of an artifact in a zone's treasure chest.
{{config reliquary.drops.chestArtifact}}

`reliquary.drops.chestRing` — Chance of a ring in a zone's treasure chest.
{{config reliquary.drops.chestRing}}

`reliquary.drops.eliteArtifact` — Chance of an artifact from a zone's elite (player kills).
{{config reliquary.drops.eliteArtifact}}

`reliquary.drops.eliteRing` — Chance of a ring from a zone's elite (player kills).
{{config reliquary.drops.eliteRing}}

### moon: Moon nights

`moon.enabled` — Moon nights on or off (the debug command still starts one).
{{config moon.enabled}}

`moon.cycleInterval` — A moon-night cycle every this many lunar cycles of 8 days; the world's first full moon never counts.
{{config moon.cycleInterval}}

`moon.fallbackMinutes` — Real-time minutes after which a moon night ends even if its waves are not beaten.
{{config moon.fallbackMinutes}}

`moon.fullMoon.levelBonus` — Extra levels for overworld hostiles that spawn on a full moon.
{{config moon.fullMoon.levelBonus}}

`moon.fullMoon.followRange` — Follow range multiplier for overworld hostiles on a full moon.
{{config moon.fullMoon.followRange}}

`moon.blood.vampirism` — Share of the damage a hostile deals to a player that heals it on a blood new moon.
{{config moon.blood.vampirism}}

### nightMobs: Full moon waves

`nightMobs.zombieLevelBonus` — Levels an Ashen Zombie of a full moon wave gets on top of the spot's level.
{{config nightMobs.zombieLevelBonus}}

`nightMobs.ghostMissChance` — Share of attacks on a Ghost that pass through it.
{{config nightMobs.ghostMissChance}}

`nightMobs.maxZombies` — Most Ashen Zombies in one player's wave (one per Curse of the Night stack).
{{config nightMobs.maxZombies}}

`nightMobs.ghostCount` — Ghosts sent to a player with no curse.
{{config nightMobs.ghostCount}}

`nightMobs.spawnMinDistance` — Fewest blocks from the player a wave spawns.
{{config nightMobs.spawnMinDistance}}

`nightMobs.spawnMaxDistance` — Most blocks from the player a wave spawns.
{{config nightMobs.spawnMaxDistance}}

### vampire: The Vampire

`vampire.provokeMinutes` — Real-time minutes before the Vampire nearest to a player turns if nobody struck one.
{{config vampire.provokeMinutes}}

`vampire.groupRadius` — Players this close to each other share one group of Vampires.
{{config vampire.groupRadius}}

`vampire.groupMin` — Fewest Vampires in a group.
{{config vampire.groupMin}}

`vampire.groupMax` — Most Vampires in a group.
{{config vampire.groupMax}}

`vampire.bossMinLevel` — The Vampire boss's lowest level.
{{config vampire.bossMinLevel}}

`vampire.bossCapNoRealm` — The boss's highest level while nobody taking part has unlocked a realm.
{{config vampire.bossCapNoRealm}}

`vampire.firstKillShards` — Flask Shards for a player's first Vampire kill (plus a Vampire Fang).
{{config vampire.firstKillShards}}

`vampire.repeatShardChance` — Chance of one Flask Shard on later kills.
{{config vampire.repeatShardChance}}

### setpieces: Set pieces

`setpieces.maxPerChunk` — Most moving set pieces (windmill sails, bells, censers…) one chunk may hold; extras become air.
{{config setpieces.maxPerChunk}}

`setpieces.animate` — Whether set pieces move at all. Off: they stand in their rest pose. Synced to clients.
{{config setpieces.animate}}

Next: [Datapacks and compatibility](datapacks.md)
