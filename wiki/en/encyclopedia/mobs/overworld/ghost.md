---
entity: embervoid:ghost
kind: mob
---

{{stats}}

## Where to find it

Ghosts never spawn on their own. They come on the **full moon** (see [Moon nights](../../../guides/mechanics/moon-nights.md))
to every overworld player who carries **no** Curse of the Night: three of them <!-- nightMobs.ghostCount --> appear
16 to 32 blocks away <!-- nightMobs.spawnMinDistance, nightMobs.spawnMaxDistance --> and come for you. A player with
curse stacks gets [Ashen Zombies](ashen-zombie.md) instead.

## Behaviour

A pale flying spirit, much tougher than an Ashen Zombie. It flies, drifts straight through walls and blocks, and sees
you through them, so you cannot hide or wall yourself in. It strikes when it touches you, about once every two
seconds <!-- GhostEntity.ATTACK_COOLDOWN_TICKS -->.

Some of your hits pass right through it with a puff of mist and do nothing (about one in seven
<!-- nightMobs.ghostMissChance -->). Expect a long fight and bring food and your flask.

Fight them in the open, where you can see all three coming, and keep moving so only one reaches you at a time.

Your wave holds the night: dawn does not come until all three are gone. If you clear the wave you get a
{{item embervoid:flask_shard}}. If you die, your Ghost wave ends there, and the death gives you a curse stack instead.
Any Ghosts left when the night ends vanish.

## Drops

{{drops}}
