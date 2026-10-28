---
entity: embervoid:bark_hollow
kind: mob
---

{{stats}}

## Where to find it

The Bark Hollow is the tank of the [Royal Woods](../../places/royal-woods.md). It never spawns naturally: Bark Hollows
are placed in the glades, along the park wall and at the hunting lodge, and once killed they do not come back.

## Behaviour

It stands dormant, looking exactly like a dead tree: no movement, no sound, dark eyes. When you come within a few
blocks <!-- BarkHollowEntity.WAKE_RANGE --> or hit it, it creaks awake over a second, its eyes light up, and it comes
for you. Left alone long enough, it walks back to its spot and turns into a tree again.

Its only attack is a heavy sweep across the 120° in front of it, about 3 blocks out. <!-- BarkHollowEntity.SWEEP_RANGE -->
It plants its feet and draws back for a full second <!-- BarkHollowEntity.SWEEP_WINDUP --> before swinging. That long
wind-up is the opening: step back out of reach, or get behind it, and hit it while it swings at empty air. It is slow
and hard to knock back.

Bring an axe or fire. A melee hit with an axe and every kind of fire damage hurt it half again as much.
<!-- BarkHollowEntity.WEAK_FACTOR --> Treat suspicious dead trees in the Woods with care, and wake them on your own
terms, ideally with the area already cleared.

## Drops

{{drops}}

As a placed tank it drops more realm loot than common mobs: one or two {{item embervoid:royal_amber}} and, on a player kill, a piece of the
[Huntsman's Set](../../items/armor/huntsmans-set.md) and the {{item embervoid:huntsman_greaves}} at the better rate.
