---
entity: embervoid:hollow
kind: mob
---

{{stats}}

## Where to find it

Hollows walk the overworld surface at night, rarely, and only under open sky. They come alone. They do not burn in the
sun, so one you leave alive is still around in the morning. Hollows also stand guard in the halls of every
[Ruined Sanctum](../../places/ruined-sanctum.md); those are placed when the dungeon generates.

## Behaviour

An armed undead soldier with an iron or stone sword: slow, armored and hard-hitting. All its damage comes from the
sword. It attacks once every two seconds <!-- HollowEntity.ATTACK_INTERVAL_TICKS -->, and every swing has a short
wind-up: the arm goes back, and the blow lands a moment later only if you are still in reach. Step back when you see
the arm go back, then step in and hit it.

Between swings it may raise its **guard**. While the guard is up it walks at half speed, melee hits from the front
deal only a fifth of their damage <!-- HollowEntity.GUARDED_MELEE_FACTOR -->, and arrows from the front bounce off.
Hits from the side or behind are never blocked.

An **axe** or the {{item embervoid:ashbearers_mace}} breaks the guard: the Hollow staggers for three seconds
<!-- HollowEntity.STAGGER_TICKS -->, slowed and unable to swing. That is the best time to finish it.

So: bait the swing, dodge it, hit it before the guard goes up, and keep an axe on your hotbar. Its level is high for
an overworld mob, which makes it slow to kill but worth the XP.

## Drops

{{drops}}
