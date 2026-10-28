---
title: Commands
kind: guide
order: 9
---

Embervoid's commands are for server operators and for testing. All of them start with `/embervoid` and need operator
permission (level 2); in singleplayer, turn cheats on. They are meant for fixing a stuck player or trying something
out, not for normal play, and their output is in English only.

`<stat>` is one of `vitality`, `endurance`, `strength`, `dexterity`, `kindling`, `resilience`, `fortune`, or its
short name (`vit`, `end`, `str`, `dex`, `kin`, `res`, `for`).

## Player

| Command | What it does |
|---|---|
| `/embervoid stats` | your stats, level, next level's cost and what the stats give |
| `/embervoid levelup <stat>` | buy one point as the Ashen Maiden would, paying XP |
| `/embervoid setstat <stat> <value>` | set a stat for free (1–999) |
| `/embervoid stamina` | your stamina |
| `/embervoid stamina spend <amount>` | spend stamina as a special attack would |
| `/embervoid stamina set <value>` | set your stamina |
| `/embervoid data` | your Embervoid player data as stored on the server |
| `/embervoid data flaskChargeUpgrades <n>` | set how many Flask Shards your flask has taken |
| `/embervoid remnant` | put your items and XP into an Ash Remnant at your feet, as a death would |
| `/embervoid reliquary <player>` | show a player's Reliquary |
| `/embervoid reliquary <player> open` | open that player's Reliquary for them |
| `/embervoid reliquary <player> clear` | empty that player's Reliquary |

## Progress

| Command | What it does |
|---|---|
| `/embervoid progress <player>` | show a player's unlocked and completed realms and beaten bosses |
| `/embervoid progress <player> reset` | forget all of it |
| `/embervoid progress <player> unlock <realm>` | unlock a realm, e.g. `embervoid:realm_1` |
| `/embervoid progress <player> complete <realm>` | mark a realm completed |
| `/embervoid progress <player> boss <id>` | mark a boss beaten, e.g. `embervoid:fallen_sister` or `embervoid:ashen_reach/zone_a` |

Realm ids are `embervoid:realm_1` to `embervoid:realm_4`. Boss ids: `embervoid:fallen_sister`,
`embervoid:ashen_reach/zone_a` (Ash Colossus), `zone_b` (Grieving Tree), `zone_c` (Crypt Keeper) and `final`
(Smoldering Lord).

## Travel

| Command | What it does |
|---|---|
| `/embervoid hub` | go to the hub |
| `/embervoid hub leave` | take the hub's fallback way out, ignoring the lit stele: to where you last left the overworld, else your bed or the world spawn |
| `/embervoid hub check` | check that the hub is whole |
| `/embervoid stele` | where the world's lit Cleft Stele is |
| `/embervoid stele kindle <pos>` | kindle the cold stele at a position, as a Crying Soul would |
| `/embervoid realm enter <realm> [toShrine]` | enter a realm as its Void Obelisk would; `true` goes to your last rested shrine there |
| `/embervoid realm leave` | leave the realm for the hub, as the Return Obelisk does |
| `/embervoid realm where` | the realm, zone, level band and place where you stand |
| `/embervoid realm arenas` | every boss arena of the realm and whether you have beaten it |
| `/embervoid realm roads [posts]` | the realm's roads, milestones, outposts (and lantern posts) |
| `/embervoid realm edge` | the realm's border wall and broken bridges |
| `/embervoid realm border <from> <to>` | the final zone's border wall |
| `/embervoid realm find <block> <x> <z>` | the highest such block in that column, e.g. the Citadel Gate |
| `/embervoid realm data` | your realm travel data |

## Bosses, dungeons and mobs

| Command | What it does |
|---|---|
| `/embervoid boss fights` | running boss fights |
| `/embervoid sanctum arena <x> <z>` | the boss arena of the Ruined Sanctum that starts in that chunk (where `/locate` points), and whether it is cleared |
| `/embervoid sanctum survey <radius> [<seed> [<spacing> <separation>]]` | count the Ruined Sanctums worldgen would place around you, without generating anything |
| `/embervoid moblevel <targets>` | show mobs' levels, and the level a new spawn here would get |
| `/embervoid moblevel <targets> <level>` | set mobs' levels |
| `/embervoid gear bleed <targets> [stacks]` | add Bleeding levels as that many hits would |

To find a structure, use vanilla `/locate structure embervoid:ember_sanctum` (a Ruined Sanctum) or, in Ashen Reach,
`embervoid:ashen_reach_arena_a`, `_b` and `_c` (the zone arenas).

## Moon nights and the Vampire

| Command | What it does |
|---|---|
| `/embervoid moon status` | the current or next moon night |
| `/embervoid moon start full` / `start blood` | start a full moon or a blood new moon now |
| `/embervoid moon stop` | end the moon night |
| `/embervoid moon setcycle <n>` | set the clock to the morning of lunar cycle n (day 8·n) |
| `/embervoid moon curse <player> <stacks>` | set a player's Curse of the Night |
| `/embervoid wave status` | the full moon waves |
| `/embervoid wave spawn <players>` / `clear <players>` | send or remove those players' waves |
| `/embervoid vampire status` | tonight's Vampires and fight |
| `/embervoid vampire provoke` | the nearest Vampire turns now |
| `/embervoid vampire arena open` / `close` / `check` | the Vampire's arena without a fight |

## Performance

| Command | What it does |
|---|---|
| `/embervoid perf ticks` | server tick times |
| `/embervoid perf chunks <from> <to>` | loaded chunks in an area |
| `/embervoid perf entities <from> <to>` | entities, mobs and placed enemies in an area |

## Client

`/embervoid_client data` (no operator needed) shows your player data as your own client sees it.

Next: [FAQ and troubleshooting](faq.md)
