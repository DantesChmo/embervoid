---
title: Boss fights
kind: mechanic
order: 7
id: embervoid:fog_wall
---

Every boss in Embervoid fights in an arena with the same rules. Each boss's moves are on its own page in the
Encyclopedia; this page covers the rules around them.

## Starting a fight

Walk into the arena of a boss you have not beaten yet, and it appears. Everyone inside the arena at that moment
becomes a fighter. A boss bar shows its name and level, its music starts, and a **fog wall** closes around the arena.

- Fighters cannot walk out through the fog wall.
- Nobody else can walk in until the fight ends.

If you have already beaten the boss, its arena stays empty and you can walk through in peace (unless someone who has
not beaten it walks in, or you use an Ash of Rematch).

## Leaving and losing

You drop out of a fight when you die, log out, or get out some other way (an ender pearl, a teleport). When the last
fighter is gone, the fight resets: the boss disappears and comes back at full health on the next attempt, and
whatever the fight changed in the arena is put back.

Since your Ash Remnant lies where you fell, it is usually inside the arena: grab it first thing on your next attempt.

## Winning

Everyone who is **alive and inside the arena** when the boss dies gets credit.

- **First kill:** your personal reward goes straight into your inventory, so nobody can pick up yours: the boss's
  soul and a Flask Shard (the Smoldering Lord: his soul and a Flask Ember). The boss also drops its loot and XP.
- **Repeat kill**, for example helping a friend: the loot and XP, and a 25 % chance <!-- config boss.repeatSoulChance -->
  of the soul once more.
- The arena's Ember Shrine kindles for you.

## Bosses in a party

Each fighter beyond the first makes the boss tougher:

- +50 % health per extra fighter <!-- config boss.healthPerExtraFighter -->, fixed when the fight starts;
- one extra add per extra fighter <!-- config boss.addsPerExtraFighter -->, a few levels below the boss.

Every 8–10 seconds <!-- config boss.targetSwitchMinSeconds / targetSwitchMaxSeconds --> the boss turns to whoever hurt
it most in the meantime. Spread your damage and the tank can rest.

Progress is per player. A friend who has beaten a boss can help you, and gets loot for it.

## Ash of Rematch

To fight a beaten boss of a realm again, use an {{item embervoid:ash_of_rematch}} on the Ember Shrine outside its
arena, then step inside within a minute. A rematch gives loot and XP, never the soul or a Flask Shard. It does not work
on the Fallen Sister (each Ruined Sanctum's boss falls once per world) or the Vampire.

## Boss music

Boss music has its own **Boss Music** slider in Options → Music & Sounds, independent of the Music slider. Turn
Music off and you still hear the boss themes; turn Boss Music off to silence them.

## Settings

{{config boss.healthPerExtraFighter}}
{{config boss.addsPerExtraFighter}}
{{config boss.addLevelOffset}}
{{config boss.repeatSoulChance}}

Next: [The Reliquary](reliquary.md)
