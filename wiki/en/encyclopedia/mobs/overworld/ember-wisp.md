---
entity: embervoid:ember_wisp
kind: mob
---

{{stats}}

## Where to find it

Ember Wisps live deep underground: they spawn only below y=0, in total darkness and under cover, alone or in pairs.

## Behaviour

A small floating ember that dives at players within sixteen blocks. A hit is weak but sets you on fire for four seconds
<!-- EmberWispEntity.BURN_SECONDS -->. After each dive it pulls back briefly, then comes again. Every so often it lights a
fire on the ground nearby (only when the `mobGriefing` game rule is on), which is dangerous in a mine full of wooden
supports.

It **fears light**. Where the block light is 7 or more <!-- EmberWispEntity.LIGHT_LIMIT --> it will not dive at you;
it drops its target and flies off into the dark. The fires it lights itself do not count, but torches, lanterns and
glowstone do.

So the counter is simple: light your mine. Place torches as you go and wisps cannot touch you. In the dark, hit it as
it comes in on a dive, or shoot it while it hovers. Water puts out the burning it gives you.

## Drops

{{drops}}
