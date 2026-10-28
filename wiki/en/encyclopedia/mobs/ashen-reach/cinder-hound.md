---
entity: embervoid:cinder_hound
kind: mob
---

{{stats}}

## Where to find it

Cinder Hounds roam the Burning City around the [Smoldering Citadel](../../places/smoldering-citadel.md), the final
sector behind the Citadel Gate. They are its only natural spawn and come in packs of three to five <!-- config reachMobs.spawns.cinder_hound -->, wherever the block
light is low. The [Smoldering Lord](../../bosses/smoldering-lord.md) calls extra hounds into his courtyard when you
fight him in a party.

## Behaviour

A fast, burning hound. When it picks a target it lets out a howl, and every idle hound within a long way
<!-- CinderHoundEntity.HOWL_RANGE --> joins the hunt. One hound that spots you means the whole pack is coming.

It runs faster than you walk and bites with a very short lunge; a bite that lands sets you on fire for a couple of
seconds. <!-- CinderHoundEntity.BITE_FIRE_SECONDS --> Fire does not hurt the hounds themselves, so fire weapons and lava
are wasted on them.

The pack is the problem, not the single hound. When you hear the howl, back into a corner or a narrow street so they
can only reach you from one side, and swing at whatever comes into reach: a hound has little health, and wide sweeping
attacks hit several at once. Fire resistance takes the sting out of their bites. Keep moving between fights in the
city, since a fresh howl brings any hound still idle nearby.

## Drops

{{drops}}
