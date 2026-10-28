---
entity: embervoid:hollow_sentinel
kind: mob
---

{{stats}}

## Where to find it

The Hollow Sentinel is the tank of the [Sunken Necropolis](../../places/sunken-necropolis.md). It is placed only, never
spawns naturally and never comes back: Sentinels guard the mausoleum doors, the collapsed tombs and the crypt halls.

## Behaviour

A spearman behind a tower shield. While it faces you, the shield covers its whole front half: melee hits and arrows from
the front do nothing at all, axes included. Fire, magic and explosions still hurt it from any side.

It has two moves, and it plants its feet for both:

- **Spear thrust.** It draws the spear back, then thrusts a long way out, about 3 blocks past its body.
  <!-- HollowSentinelEntity.SPEAR_REACH --> Back out of that range during the draw, or sidestep.
- **Shield bash.** Only when you are close: it plants the shield for three quarters of a second
  <!-- HollowSentinelEntity.BASH_WINDUP -->, then slams it into you with strong knockback.

After a bash, whether it hits or not, the shield hangs open for a second and a half. <!-- HollowSentinelEntity.OPEN_TICKS -->
That is your window: dodge the bash, then hit it from the front while it staggers.

The other way is to get behind it, where the shield does not reach. In a party, one player keeps its attention and the
other works on its back. Burning it also works: fire damage is never blocked.

## Drops

{{drops}}

As a placed tank it drops more realm loot than common mobs: one or two {{item embervoid:raw_grave_iron}} and, on a
player kill, a piece of the [Warden's Set](../../items/armor/wardens-set.md) at the better rate, or rarely the
{{item embervoid:drowned_sabatons}}.
