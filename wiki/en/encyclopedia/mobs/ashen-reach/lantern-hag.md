---
entities: [embervoid:lantern_hag, embervoid:fire_flask]
kind: mob
---

{{stats}}

## Where to find it

The Lantern Hag is the ranged enemy of the [Royal Woods](../../places/royal-woods.md). She spawns alone, and more are
placed in the hunters' hides, the lantern rows, the hunting lodge and the road outposts.

## Behaviour

She keeps her distance, about 8 to 12 blocks <!-- LanternHagEntity.MIN_RANGE, MAX_RANGE -->: she backs away when you
close in and walks up when you fall back. From there she throws fire flasks in a high arc.

Each throw is telegraphed. She raises her lantern, which flares with embers, and three quarters of a second later
<!-- LanternHagEntity.THROW_WINDUP --> the flask leaves her hand, aimed at where you stand at that moment. It bursts
for damage around the point it lands and leaves a patch of fire that sets anyone standing in it alight for a few
seconds. <!-- FireFlaskEntity.PATCH_TICKS --> No fire blocks are placed, so the woods do not burn.

When the lantern goes up, keep moving sideways: the flask lands where you were. Never fight in her fire patch.

To kill her, rush her. She cannot throw at something within a few blocks of her <!-- LanternHagEntity.MIN_THROW_RANGE -->
and only backs away, and she has little health. A bow or crossbow also works, since she stands still while she aims.
Deal with the Hag first when she comes with Thralls or a Bark Hollow.

## Drops

{{drops}}

On top of the table, a Hag can drop {{item embervoid:royal_amber}}, and a Hag killed by a player can drop a piece of
the [Huntsman's Set](../../items/armor/huntsmans-set.md) or, rarely, the {{item embervoid:huntsman_greaves}}.
