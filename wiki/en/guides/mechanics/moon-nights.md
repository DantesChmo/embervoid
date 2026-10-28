---
title: Moon nights
kind: mechanic
order: 11
---

Every third lunar cycle the overworld gets two dangerous nights, four days apart: a **full moon** and a **blood new
moon**. Both are announced with a title when they begin and end. On those days your bed refuses you from the evening
on: you cannot sleep through them.

## When they come

A lunar cycle is 8 days. With the default settings the first full moon of a world never counts; the first moon nights
are:

- **day 24**: the full moon,
- **day 28**: the blood new moon,

and then every 24 days after that (days 48 and 52, 72 and 76, …). <!-- MoonSchedule, config moon.cycleInterval -->

Moon nights only touch the overworld. If you are in the hub or a realm when the full moon rises, your wave meets you
when you come back, as long as the night lasts.

## The full moon

"The dead will not rest tonight."

- Overworld hostiles spawn 3 levels higher, follow you from further away and do not lose you while you stay in range.
- **Curse of the Night:** every time a hostile mob, its projectile or a boss kills you, anywhere, you gain a stack. The
  curse lifts when the full moon sets.
- At night's start, a **wave** comes for every player in the overworld, 16–32 blocks away:
  - with a curse: one [Ashen Zombie](../../encyclopedia/mobs/overworld/ashen-zombie.md) per stack, up to 10. They are
    5 levels above the spot's normal level and do not burn in daylight.
  - with no curse: 3 [Ghosts](../../encyclopedia/mobs/overworld/ghost.md). They fly through walls, and some of your
    hits pass through them. Beat all three and they leave a {{item embervoid:flask_shard}}.
- **The night does not end until your wave is beaten.** The clock stops just before dawn and waits. If you die while
  your Ghosts are out, they drift away and the night stops waiting for them. After 30 real minutes the night ends anyway.

How to prepare: die as little as possible in the days before (fewer stacks, fewer zombies), or keep your record clean
and earn a Flask Shard from the Ghosts. Fight near a shelter, and keep your flask full.

## The blood new moon

"The sky bleeds."

- The sky, the fog and the moon turn red.
- Overworld hostiles heal a quarter of the damage they deal to you.
- Pale, friendly **Vampires** step out of the dark near players. They are not friendly for long. See
  [The Vampire](vampire.md).

## Settings

{{config moon.enabled}}
{{config moon.cycleInterval}}
{{config moon.fallbackMinutes}}
{{config moon.fullMoon.levelBonus}}
{{config moon.blood.vampirism}}
{{config nightMobs.maxZombies}}
{{config nightMobs.ghostCount}}

Next: [The Vampire](vampire.md)
