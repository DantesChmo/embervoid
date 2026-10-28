---
entity: embervoid:ash_ghoul
kind: mob
---

{{stats}}

## Where to find it

Ash Ghouls hunt on the overworld surface at night, in any biome, and in dark caves. They come in packs of three to
five <!-- AshGhoulEntity pack spawn -->. They burn in daylight.

## Behaviour

A fast, weak ghoul that relies on numbers. It runs at you and claws. When one ghoul picks you as its target, every other
ghoul within sixteen blocks <!-- AshGhoulEntity.PACK_RANGE --> joins in, and hitting one makes the rest of the pack turn
on you as well.

The trick is its death. A dying ghoul bursts into a cloud of ash about two and a half blocks wide
<!-- AshGhoulEntity.CLOUD_RADIUS -->. Anyone standing in it goes blind for three seconds
<!-- AshGhoulEntity.BLINDNESS_TICKS -->, so killing the first ghoul of a pack in melee blinds you for the rest.

To beat a pack, do not fight it in the middle of the cloud. Kill a ghoul, then step back out of the ash before the
next one reaches you. A bow lets you thin the pack before it arrives, and a corridor or a doorway makes them come one at
a time. In the morning, the sun finishes off any you leave behind.

## Drops

{{drops}}
