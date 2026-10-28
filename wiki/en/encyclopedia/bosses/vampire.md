---
entities: [embervoid:vampire_boss, embervoid:vampire]
kind: boss
---

{{stats}}

## How to reach it

The Vampire comes only on the **blood new moon**, the second of the two [moon nights](../../guides/mechanics/moon-nights.md)
(the full guide: [The Vampire](../../guides/mechanics/vampire.md)). When the sky turns red, one to three Vampires
<!-- vampire.groupMin, vampire.groupMax --> appear near each group of players in the overworld. Players in an old
Sanctum core are left out.

In this form they are friendly. They stroll about, watch you, and talk when you speak to them. They cannot be hurt:
the first player who hits any Vampire provokes it. That one turns into the boss after a short transformation, and all
the other Vampires vanish. If nobody provokes one, the Vampire nearest to a player turns on its own after five minutes
<!-- vampire.provokeMinutes -->.

When the boss forms, **every** player in the overworld (outside a Sanctum core) is pulled into the Vampire's arena: a
small arena floating in a red-fogged void, rebuilt for every blood moon. You cannot break or place blocks there. There
is no fog wall; the void around the arena is the wall.

Its level is the highest level among the players taking part, at least 15 <!-- vampire.bossMinLevel -->, but no
higher than the top of the level band of the furthest realm any of them has unlocked (20 with none
<!-- vampire.bossCapNoRealm -->).

## Phases and moves

- **Claws.** It chases you and draws its claws back with a short red tell before it strikes. Step back during the
  tell; it still reaches a little further than it looks.
- **Blood Leap.** At a target between five and sixteen blocks away it crouches for a second while two blood-red rings
  mark where it will land, then jumps there and hits everyone around the landing. Run out of the rings.

### Below 60 % health

- **Blood Nova.** A ring forms around it and shrinks for a second and a half, then bursts: everyone inside is hurt
  and thrown back, and it heals by a quarter of the damage it dealt <!-- VampireBossEntity.NOVA_HEAL -->. Get out of
  the ring and it heals nothing.
- It leaps more often.

### Below 30 % health

- **Mist Step.** When you are more than three blocks away, it dissolves into smoke and reappears right behind you
  with a strike. When you see the smoke, turn around.
- It moves faster, and the nova comes more often.

## Drops

{{drops}}

It has no loot table. When it dies, every fighter still alive in the arena gets their reward straight into the
inventory:

- your **first** kill: two {{item embervoid:flask_shard}} <!-- vampire.firstKillShards --> and a
  {{item embervoid:vampire_fang}};
- a **repeat** kill: a one-in-two chance <!-- vampire.repeatShardChance --> of one Flask Shard.

It drops XP like any boss. Ten seconds after the kill the arena closes, everyone goes back to where they were pulled
from, and dawn comes.

**If you die in the arena** you respawn as usual and cannot get back in. Your [Ash Remnant](../../guides/mechanics/death-and-remnant.md)
is moved to the spot you were pulled from when the arena closes. If every fighter dies, the Vampire leaves, the arena
closes and the night ends.

## Strategy

**Melee.** Stay close enough that it claws rather than leaps, and back off a step on every tell. From 60 % on, the
nova is the move to respect: the moment the ring appears, walk out of it, then come back in for hits while it
recovers. Below 30 % keep your back to the arena's edge only if you are sure of your footing; Mist Step puts it behind
you.

**Ranged.** Hard to keep up: it is fast and leaps at anyone at range. Shoot while it chases, and side-step each landing
ring.

**In a party.** Everyone in the overworld fights it together, and its level follows the strongest of you. There are no
adds. Spread out so one nova or leap does not catch the whole group, and let whoever it chases kite it while the
others hit it from behind. Every fighter alive at the end gets their own reward, so keep the newer players alive.
