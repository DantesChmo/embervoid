---
title: The Reliquary
kind: mechanic
order: 8
---

The Reliquary holds the small things you wear besides armor: **artifacts** and **rings**. They all come from Ashen
Reach.

## Opening it

Press **R** (rebind it under Controls as "Open Reliquary"), or click the Reliquary button next to the recipe book in
your inventory. The screen shows your character with the accessory slots beside it.

You have **2 artifact slots** and **2 ring slots** <!-- config reliquary.slots.artifact / reliquary.slots.ring -->.
You cannot wear two of the same item.

## Artifacts: stat points

Each artifact adds stat points while you wear it:

| Artifact | Gives | Found in |
|---|---|---|
| {{item embervoid:last_harvest_grain}} | +3 Vitality | Ashen Fields |
| {{item embervoid:ploughmans_horseshoe}} | +3 Endurance | Ashen Fields |
| {{item embervoid:royal_hound_fang}} | +3 Dexterity | Royal Woods |
| {{item embervoid:amber_tear}} | +3 Kindling | Royal Woods |
| {{item embervoid:gravestone_medallion}} | +3 Resilience | Sunken Necropolis |
| {{item embervoid:ferrymans_coin}} | +3 Fortune | Sunken Necropolis |
| {{item embervoid:wardens_chain}} | +3 Strength | Sunken Necropolis |
| {{item embervoid:seal_of_the_ashen_king}} | +1 to every stat | Smoldering Citadel |

<!-- config reliquary.artifacts.points / sealPoints -->

Artifact points count for everything a stat does, but they do not raise your level, your level cost or the stat cap.
The level-up screen shows them next to each stat.

## Rings: effects

| Ring | Effect | Found in |
|---|---|---|
| {{item embervoid:couriers_ring}} | +15 % sprint speed | Ashen Fields |
| {{item embervoid:ring_of_ash_feathers}} | slower falling, no fall damage below 8 blocks (sneak to drop normally) | Ashen Fields |
| {{item embervoid:ring_of_the_fog_stalker}} | the realm's fog clears twice as far (not in boss arenas) | Royal Woods |
| {{item embervoid:delvers_ring}} | Haste I while mining realm ores | Royal Woods |
| {{item embervoid:drowned_mans_ring}} | water breathing | Sunken Necropolis |
| {{item embervoid:ring_of_embers}} | melee hits set the target on fire for 2 seconds | Smoldering Citadel |

<!-- config reliquary.rings.* -->

## Where they drop

Each zone drops only its own accessories:

- its **treasure chest**: an artifact 8 % of the time, a ring 5 %;
- its **elite**, killed by a player: an artifact 2 %, a ring 1 %. The elites are the Ashen Carrion (Fields), the
  Grave Warden (Necropolis) and the Smoldering Knight (Citadel). The Royal Woods have no elite: its accessories come
  from the hunting lodge's chest only.

<!-- config reliquary.drops.* -->

## On death

Worn accessories go into your [Ash Remnant](death-and-remnant.md) with the rest of your items, and come back to
their slots when you reclaim it. If the server's remnant mode leaves items on the ground, they drop with your
inventory. With `keepInventory` you keep them.

## Settings

{{config reliquary.slots}}
{{config reliquary.artifacts.points}}
{{config reliquary.drops.chestArtifact}}

Next: [Armor sets](armor-sets.md)
