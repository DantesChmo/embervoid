---
title: Status effects
kind: mechanic
order: 10
effect: embervoid:bleeding
---

Embervoid adds one status effect so far: **Bleeding**.

## Bleeding

Bleeding deals damage every second and ignores armor. It has three levels:

| Level | Damage per second |
|---|---|
| I | 0.5 (a quarter heart) |
| II | 1 |
| III | 1.5 |

<!-- config gear.bleeding.damagePerLevel, gear.bleeding.maxLevel -->

Every hit that causes Bleeding raises it by one level, up to III, and starts its 6 seconds <!-- config
gear.bleeding.durationTicks: 120 ticks --> again. Keep hitting and it keeps running at full strength; stop and it runs
out a few seconds later. A mob that bleeds to death counts as your kill.

### Who causes it

Bleeding is your weapon, not your enemies':

- {{item embervoid:wardens_scourge}}: every hit adds a level.
- {{item embervoid:royal_ash_rapier}}: every hit adds a level, and its Lunge adds two at once.

Some creatures have no blood to lose and never bleed: Restless Bones, the Bark Hollow, the Furnace Golem, the Ash
Colossus and the Grieving Tree. Use something else on them.

In PvP, these weapons make other players bleed too.

### Stopping it

- {{item embervoid:amber_balm}} cures Bleeding and Poison, then heals you slowly.
- A bucket of milk clears it like any other effect.
- Or wait it out: without new hits it ends after 6 seconds.

## Settings

{{config gear.bleeding.durationTicks}}
{{config gear.bleeding.maxLevel}}
{{config gear.bleeding.damagePerLevel}}

Next: [Moon nights](moon-nights.md)
