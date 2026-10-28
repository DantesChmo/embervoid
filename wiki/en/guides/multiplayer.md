---
title: Multiplayer and servers
kind: guide
order: 6
---

Embervoid plays the same on a server as in singleplayer. Install it with its required mods on the server and on every
client (see [Installing](installing.md)).

## What is shared and what is yours

**Yours alone:**

- your stats, level and stamina, your flask charges and Flask Shards, your Flask Ember;
- your progress: which bosses you have beaten, which realms you have unlocked and completed, and so how each Void
  Obelisk looks to you;
- whether the Citadel Gate opens for you, and whether an arena's shrine burns for you;
- your respawn shrine in each realm;
- your Ash Remnant: nobody else can take it.

**Shared by everyone:**

- the hub: there is one Ember Sanctum for the whole server;
- each realm: one Ashen Reach, the same terrain, the same chests and the same placed enemies. A chest someone looted
  and a guard someone killed stay that way for everyone;
- the world's lit Cleft Stele: there is one per world, and any player kindling a new one puts the old one out for
  everybody;
- each Ruined Sanctum's boss: it falls once per world, for whoever is in that fight.

## Bosses in a party

- Everyone inside an arena when the fight starts is a fighter; the fog wall keeps latecomers out.
- Each extra fighter gives the boss +50 % health and one more add. The boss switches targets every 8–10 seconds,
  turning to whoever hurt it most.
- Everyone alive and inside at the kill gets credit. First-kill rewards go into each player's own inventory.
- Helping a friend who has not beaten a boss: you get its loot and XP, and a chance at its soul again.
- **The Fallen Sister falls once per Sanctum.** Go down together: every player in that fight gets a Crying Soul. A
  player who missed it needs another Sanctum.

Details in [Boss fights](mechanics/boss-fights.md).

## Moon nights with friends

- Full moon waves are per player: each player in the overworld gets their own, from their own curse.
- Players within 64 blocks of each other share one group of friendly Vampires.
- When a Vampire turns, **every player in the overworld** is pulled into its domain, wherever they are. Warn your
  friends before you strike one.

## Server settings worth knowing

The server's `config/embervoid.json` decides the rules for everyone; each joining client receives it. Edit it while the
server is stopped: the file is read at startup. Keys a server owner most often changes:

- `remnant.mode` and `remnant.secondDeath`: how harsh death is.
- `mobs.difficulty.*`: how hard hostile mobs hit on each difficulty.
- `boss.healthPerExtraFighter` and `boss.addsPerExtraFighter`: how much tougher bosses get in a party.
- `moon.enabled`: moon nights on or off.
- `vampire.groupRadius`: how close players must be to share one group of Vampires.

All keys are on [Configuration](configuration.md). The operator commands are on [Commands](commands.md).

Next: [Configuration](configuration.md)
