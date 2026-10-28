---
title: Mob levels and difficulty
kind: mechanic
order: 2
---

Every hostile mob, vanilla or from another mod, gets a level once, when it first enters the world. Look at a mob and
its name shows "Lv. N".

## How a mob's level is picked

In the overworld, the Nether and the End, the level is the mob type's **base level** plus bonuses:

| Base level | Mobs |
|---|---|
| 3 | zombie, zombie villager, husk, drowned, spider |
| 4 | skeleton, stray, slime, and any hostile not listed |
| 5 | Ash Ghoul |
| 6 | creeper, witch |
| 8 | Ember Wisp |
| 12 | enderman |
| 15 | Hollow |

<!-- config mobs.levels.baseLevels, mobs.levels.defaultBaseLevel -->

| Bonus | Levels |
|---|---|
| Overworld: every 1000 blocks from the world spawn | +1, up to +5 |
| Overworld: below y = 0 | +3 |
| The Nether | +5 |
| The End | +8 |
| Overworld on a full moon night | +3 |

<!-- config mobs.levels.blocksPerLevel, maxDistanceBonus, belowZeroBonus, dimensionBonus, moon.fullMoon.levelBonus -->

In a realm, a mob's level comes from the zone it spawns in instead: in Ashen Reach, 15–18 in the Ashen Fields, 18–21 in
the Royal Woods, 21–25 in the Sunken Necropolis and 25–30 in the Smoldering Citadel. Bosses have fixed levels.

The Ender Dragon, the Wither, the Warden and the Elder Guardian have no level and stay as in vanilla.

## What a level changes

For every level above 1, a mob gets:

- +6 % health,
- +5 % damage (melee, arrows, explosions and potions alike),
- +6 % XP when it dies.

<!-- config mobs.levels.healthPerLevel / damagePerLevel / experiencePerLevel -->

So a level 11 mob has 60 % more health and hits 50 % harder than at level 1. Some vanilla types are scaled down first
so that they do not outgrow the rest: zombies and husks have less base health, witches and endermen less base damage.

## Difficulty

Embervoid replaces vanilla's difficulty factor on the damage hostile mobs deal to players:

| Difficulty | Embervoid | Vanilla |
|---|---|---|
| Easy | ×1.0 | about ×0.5 |
| Normal | ×1.25 | ×1.0 |
| Hard | ×1.75 | ×1.5 |

<!-- config mobs.difficulty.* -->

Easy now hits like vanilla Normal. The factor applies before the level bonus.

## Settings

{{config mobs.levels.enabled}}
{{config mobs.levels.baseLevels}}
{{config mobs.difficulty.normal}}

Mobs from other mods get the default base level; give them their own in `mobs.levels.baseLevels`. The mob level label
can be hidden on your client with `hud.showMobLevel` in `embervoid-client.json`. Every key is on
[Configuration](../configuration.md).

Next: [Stamina and special attacks](stamina-and-specials.md)
