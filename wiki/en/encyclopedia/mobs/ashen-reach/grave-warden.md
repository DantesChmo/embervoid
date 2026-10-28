---
entity: embervoid:grave_warden
kind: mob
---

{{stats}}

## Where to find it

The Grave Warden is the elite of the [Sunken Necropolis](../../places/sunken-necropolis.md). It is placed only, one per
chapel and in the warden chambers of the crypts, and once killed it stays dead.

## Behaviour

The bell-ringer: a heavy figure with a cudgel and a bell. Its cudgel swing hits hard, but it raises it slowly before the
blow; step out of reach on the wind-up.

The bell is the real threat. A few seconds after it spots you, and then every twelve seconds
<!-- GraveWardenEntity.FIRST_BELL, BELL_INTERVAL -->, it plants its feet and lifts the bell for a full second
<!-- GraveWardenEntity.BELL_WINDUP -->, then rings it. The ring:

- stands every Restless Bones pile around it back up at once;
- raises two new Restless Bones if none are around, up to four of its own at a time <!-- GraveWardenEntity.MAX_RAISED -->;
- staggers every player within a few blocks <!-- GraveWardenEntity.STAGGER_RANGE --> with heavy Slowness for two
  seconds. <!-- GraveWardenEntity.STAGGER_TICKS -->

When the bell goes up, get away from the Warden so the stagger misses you, and use the second it stands still to land a
ranged hit. Do not waste time chasing the Bones it raises: they come back until the Warden is dead. Focus the Warden,
and finish piles only when it is down, or fight next to a torch so the piles crumble on their own.

## Drops

{{drops}}

The Warden is the Necropolis' elite. A player kill can drop one or two {{item embervoid:raw_grave_iron}}, a piece of
the [Warden's Set](../../items/armor/wardens-set.md), the {{item embervoid:drowned_sabatons}} at the elite rate, and
rarely one of the Necropolis' accessories: the {{item embervoid:gravestone_medallion}}, the
{{item embervoid:ferrymans_coin}}, the {{item embervoid:wardens_chain}} or the {{item embervoid:drowned_mans_ring}}.
