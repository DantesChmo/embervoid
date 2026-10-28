---
entity: embervoid:crypt_moth
kind: mob
---

{{stats}}

## Where to find it

Crypt Moths live in the [Sunken Necropolis](../../places/sunken-necropolis.md). They spawn naturally only under a roof,
in the crypts and beneath the sinkhole's overhangs, in groups of up to three <!-- config reachMobs.spawns.crypt_moth -->, and are placed in the ossuary pits, the
crypts and the road outposts. The [Crypt Keeper](../../bosses/crypt-keeper.md) sends them after its lit braziers.

## Behaviour

A pale moth drawn to light. It flies to the nearest bright light source nearby <!-- CryptMothEntity.LIGHT_RANGE,
MIN_LUMINANCE --> and, with `mobGriefing` on, snuffs it out: placed torches break and drop, lit candles go out. A pack
of moths can leave you in the dark in a crypt in a few seconds.

When you are close it turns on you. Up close it bites, weakly. Its real attack is the pollen burst: its wings start to
glow, and three quarters of a second later <!-- CryptMothEntity.BURST_WINDUP --> it bursts, blinding every player
within a few blocks for 3 seconds. <!-- CryptMothEntity.BLINDNESS_TICKS -->

When you see glowing wings, back off out of the burst or kill the moth before it goes off. It is fragile, so a quick
sword hit or an arrow is usually enough. Never fight a Hollow Sentinel or a Grave Warden while a moth hovers nearby:
blind, you cannot read their wind-ups. Kill the moths first.

## Drops

{{drops}}

On top of the table, a moth can drop {{item embervoid:raw_grave_iron}}, and when a player kills it, rarely the
{{item embervoid:drowned_sabatons}}.
