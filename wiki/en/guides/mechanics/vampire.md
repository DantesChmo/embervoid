---
title: The Vampire
kind: mechanic
order: 12
---

On a [blood new moon](moon-nights.md), pale figures step out of the dark near players: **Vampires**. They seem
friendly. They talk, they admire the night, they ask you to stay a while. One of them is about to become a boss.

## When it comes

- On every blood new moon, 1 to 3 friendly Vampires appear near each group of players in the overworld (players
  within 64 blocks of each other share one group). <!-- config vampire.groupMin / groupMax / groupRadius -->
- **Strike one** and it sheds its disguise: it becomes the Vampire boss, and the others vanish.
- If nobody strikes one, the Vampire nearest to a player loses patience after 5 minutes
  <!-- config vampire.provokeMinutes --> and turns anyway.

You cannot avoid the fight by being polite. You can only choose when it starts.

## The Vampire's domain

When the Vampire turns, **every player in the overworld** is dragged into its domain, an arena in a void of its own.
There is no fog wall, but there is also nowhere to go.

- The boss's level matches the strongest player taking part: at least 15, and at most 20 until someone taking part has
  unlocked a realm. <!-- config vampire.bossMinLevel / bossCapNoRealm -->
- Watch for the wind-up before its claws; its moves are on the [Vampire](../../encyclopedia/bosses/vampire.md) page.
- **If you die in the domain, you are out:** you respawn as usual and cannot come back. When the domain closes, your
  remnant is carried out to the spot you were taken from.

## Winning and losing

- **Win:** every fighter still alive gets personal loot straight into the inventory. Your first kill gives a
  {{item embervoid:vampire_fang}} and 2 Flask Shards; later kills a 50 % chance of one Flask Shard
  <!-- config vampire.firstKillShards / repeatShardChance -->. Ten seconds later you are all sent back to where you were
  taken from, and dawn comes.
- **Lose:** when every fighter has died, the Vampire melts into the night, laughing, and the night ends.

## How to prepare

- Expect the blood new moon four days after each full moon, and be ready before dusk: full flask, Flask Shards
  offered, gear repaired.
- Only players in the overworld at the moment it turns are pulled in. A friend in the hub or a realm is safe, and
  of no help.
- In a group, decide who strikes first. The fight starts for everyone in the overworld at once.

## Settings

{{config vampire.provokeMinutes}}
{{config vampire.bossMinLevel}}
{{config vampire.bossCapNoRealm}}
{{config vampire.firstKillShards}}

Next: [Gear guide](../gear-guide.md)
