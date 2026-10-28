---
title: Armor sets
kind: mechanic
order: 9
---

Ashen Reach has four armor sets and four unique pieces. A set gives a bonus only when you wear **all four** of its
pieces; the tooltip of each piece says "Full set: …". Unique pieces have an effect of their own and never count toward
a set.

## The sets

| Set | Where | Armor | Full-set bonus |
|---|---|---|---|
| [Smoldering Plate](../../encyclopedia/items/armor/smoldering-set.md) | crafted (smithing) | as netherite | burning does not hurt you (lava still does) |
| [Reaper's Set](../../encyclopedia/items/armor/reapers-set.md) | Ashen Fields mobs | light, about iron | +10 % movement speed |
| [Huntsman's Set](../../encyclopedia/items/armor/huntsmans-set.md) | Royal Woods mobs | a little more, some toughness | +15 % projectile damage |
| [Warden's Set](../../encyclopedia/items/armor/wardens-set.md) | Sunken Necropolis mobs | heavy, as diamond; each piece slows you slightly | +0.3 knockback resistance, −15 % damage while blocking with a shield |

<!-- config gear.sets.*, gear.smoldering.fireReductionPerPiece; armor values GearArmor -->

The **Smoldering Plate** also works piece by piece: every piece you wear cuts fire damage by 8 %. Make it on a
smithing table from a {{item embervoid:smoldering_template}}, a diamond armor piece and a
{{item embervoid:smoldering_ingot}}.

The three zone sets drop from their zone's monsters, one random piece at a time, and only when a player makes the
kill. The tougher placed enemies drop them more often than the natural spawns, and each zone's treasure chest can hold
a piece too.

## Unique pieces

| Piece | Slot | Effect | Where |
|---|---|---|---|
| {{item embervoid:scarecrow_mask}} | head | mobs notice you at half the distance; Ash Stalkers do not ambush you | Ashen Fields |
| {{item embervoid:huntsman_greaves}} | legs | +20 % speed for 4 s after a kill; no fall damage up to 7 blocks | Royal Woods |
| {{item embervoid:drowned_sabatons}} | feet | full speed in water | Sunken Necropolis |
| {{item embervoid:furnace_heart_cuirass}} | chest | below 30 % health, a fire nova burns and throws back foes around you (once per 60 s) | Smoldering Citadel |

<!-- config gear.uniques.* -->

They are rare drops from their zone's monsters, likelier from the zone's elite (the Ashen Carrion, the Bark Hollow, the
Grave Warden). In the Citadel only the Furnace Golem and the Smoldering Knight drop the cuirass.

## Mixing pieces

- A set bonus needs all four pieces of the same set. Three pieces and a unique give no set bonus.
- A unique piece in one slot is a trade: its effect against the set bonus of the other three.
- Smoldering Plate pieces cut fire damage one by one, so a Smoldering chestplate with three other pieces still
  helps against fire.
- Enchant them as you like; they take the usual armor enchantments.

## Settings

{{config gear.sets.reaperSpeed}}
{{config gear.sets.huntsmanProjectile}}
{{config gear.sets.wardenKnockbackResist}}
{{config gear.smoldering.fireReductionPerPiece}}

Next: [Status effects](status-effects.md)
