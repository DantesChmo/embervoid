---
entity: embervoid:restless_bones
kind: mob
---

{{stats}}

## Where to find it

Restless Bones fill the [Sunken Necropolis](../../places/sunken-necropolis.md). They spawn naturally on the surface and
in the crypts in small groups, and more are placed in the grave rows, the mausoleums, the chapel, the ossuary pits, the
road outposts and every room of the crypts. Grave Wardens raise new ones, and the
[Crypt Keeper](../../bosses/crypt-keeper.md) calls them into its arena.

## Behaviour

A skeleton with a rusty sword. It attacks like any melee fodder: a raised sword, a short wind-up, one blow. Step back
on the wind-up and answer after it.

What makes it dangerous is that it does not stay dead. The blow that should kill it collapses it into a heap of bones
instead. The heap lies there for five seconds <!-- RestlessBonesEntity.REFORM_TICKS -->, then stands up again at half
health. You have two ways to finish it for good:

- **Break the heap.** The pile has very little health; one more hit before it stands up kills it.
- **Light it.** A pile lying in block light of 8 or more <!-- RestlessBonesEntity.LIGHT_TO_STAY_DEAD --> crumbles to
  dust, and the kill is still yours. Fight Bones next to a torch, or place one by the heap.

Watch for Grave Wardens: the ring of a Warden's bell stands every pile around it back up at once.

## Drops

{{drops}}

On top of the table, Bones can drop {{item embervoid:raw_grave_iron}}, and when a player kills them, a piece of the
[Warden's Set](../../items/armor/wardens-set.md) or, rarely, the {{item embervoid:drowned_sabatons}}.
