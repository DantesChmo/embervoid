---
title: Stats and leveling
kind: mechanic
order: 1
---

You have seven stats. Every one starts at 10, and your level is 1 plus every point you have bought: a fresh character
is level 1 and plays like vanilla.

## The stats

| Stat | Short | Each point above 10 gives |
|---|---|---|
| Vitality | VIT | +1 max health (half a heart) |
| Endurance | END | +2 max stamina, +2 % stamina regeneration, a little knockback resistance |
| Strength | STR | +2 % damage with Strength weapons: axes, the Ashbearer's Mace, part of the Ashen Scythe and Huntsman's Crossbow, half for tridents |
| Dexterity | DEX | +2 % damage with Dexterity weapons: swords, bows, crossbows, the Sanctum Longsword, the Warden's Scourge, most of the Royal Ash Rapier, part of the Ashen Scythe and Huntsman's Crossbow, half for tridents; +0.5 % attack speed |
| Kindling | KIN | +2 % Healing Flask heal, +2 % ember damage with the realm weapons that deal it |
| Resilience | RES | +0.1 armor toughness, −1 % fire damage (up to −50 %) |
| Fortune | FOR | +0.1 luck, +0.5 % chance that a mob you kill drops its loot a second time (up to 25 %) |

<!-- per-point amounts: config stats.*; Kindling ember damage: gear.weapons.kindlingPerPoint -->

## Soft caps

Points get weaker as a stat grows:

- from 11 to 20, every point counts in full;
- from 21 to 40, at half;
- from 41 to 99, at a fifth.

<!-- StatCurve -->

A stat stops at 99. There is no level cap by default.

## What a level costs

Levels are bought with vanilla experience **points** (not levels of the XP bar). Going from level L to L + 1 costs
20 + 6·L + 0.15·L² points <!-- leveling.costBase / costLinear / costQuadratic -->: 26 at level 1, 200 at level 20,
695 at level 50.

## Where to level

Only the [Ashen Maiden](../../encyclopedia/npcs/ashen-maiden.md) levels you. She stands in the hub, the
[Ember Sanctum](../walkthrough/ember-sanctum.md) (and in the old Sanctum cores of worlds from 0.4.x). Right-click
her, then click **+** next to a stat. The screen shows what the next point gives before you buy it, and your XP and
the cost. Walk away more than a few blocks and it closes.

There is no respec: a point bought stays.

## Points that do not count

Artifacts worn in the [Reliquary](reliquary.md) add stat points. They count for every effect but not for your level,
the level cost or the 99 cap. The level-up screen shows them next to each stat.

## Settings

{{config stats.vitalityHealth}}
{{config leveling.statCap}}
{{config leveling.levelCap}}

The full list is on [Configuration](../configuration.md).

Next: [Mob levels and difficulty](mob-levels.md)
