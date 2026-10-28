---
entity: embervoid:furnace_golem
kind: mob
---

{{stats}}

## Where to find it

The Furnace Golem is the tank of the [Smoldering Citadel](../../places/smoldering-citadel.md). It is placed only, in the
halls and roads of the inner ring, and stays dead once killed.

## Behaviour

A walking furnace: slow, heavily armored, hard to knock back and immune to fire.

Its own attack is a two-armed slam. It plants its feet and raises both fists for a moment before bringing them down in
front of it; step back out of reach during the raise.

The trap is the heat. Every hit it takes stokes it. On the fourth hit <!-- FurnaceGolemEntity.HITS_TO_BLAST --> it stops
whatever it was doing, plants its feet and glows red for over a second <!-- FurnaceGolemEntity.HEAT_TICKS -->, then lets
out a fire wave: everything within a few blocks <!-- FurnaceGolemEntity.WAVE_RADIUS --> takes fire damage, is set alight
and is pushed away. Then it cools down and the count starts again.

So count your hits. Land three, then on the fourth, or as soon as you see the glow, back out of the wave's reach and
wait for the blast before you go in again. Fire resistance helps a lot here. Heavy, slow hits are better than many
light ones, since every hit counts the same toward the blast. Fire weapons do nothing to it.

## Drops

{{drops}}

The Citadel has no ore and no armor set of its own. A Furnace Golem killed by a player can drop the
{{item embervoid:furnace_heart_cuirass}}, at the elite rate.
