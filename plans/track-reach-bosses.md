# Track E — reach-bosses (PLAN 12.1–12.7, 13.4)

Branch `claude/track-reach-bosses-31cdf3`. Rules: [COORDINATION.md](../COORDINATION.md). Design: DESIGN §4.3, §4.5
"Bosses", §10.11 (built as recommended). Framework reference: CORE.md §6.

Owns `com.embervoid.boss` (framework), `com.embervoid.reach.boss`, `com.embervoid.client.boss` (framework client:
arena fog receiver), `com.embervoid.client.reach.boss`, arena contents (`structures/ashen_reach/arena_*`, the Citadel
courtyard piece), boss assets and loot. Config section `boss`.

## Tasks

- [x] **12.1 Boss framework additions** (below; GameTests `BossFrameworkTests`, JUnit `TelegraphShapeTest`, `BossScalingTest`)
- [x] **12.2 Three arenas built** (templates; placed by reach-world's arena site code, 9.4; GameTests `ArenaTemplateTests`)
- [x] **12.3 Crypt Keeper** (GameTests `CryptKeeperTests` in the real ossuary template)
- [x] **12.4 Ash Colossus** (GameTests `AshColossusTests` in the real crater market template)
- [x] **12.5 Grieving Tree** (GameTests `GrievingTreeTests` in the real Royal Grove template)
- [x] **12.6 Souls and Flask Shards** (GameTests `BossRewardTests`)
- [ ] **12.7 Ash of Rematch**
- [ ] **13.4 Smoldering Lord**

## 12.1 Framework additions — design

All in `com.embervoid.boss`; a boss reaches its fight with `BossFight.of(boss)`.

| Piece | How |
|---|---|
| Fights anywhere | `ArenaManager` ticks fights in every world (the summon rule still only looks at realm arenas); `ArenaManager.start(world, spec, center)` starts one directly (GameTests, rematch) |
| Hazard layer | `ArenaHazard` (`tick(fight)` → keep going?, `end(fight)`), added with `fight.addHazard`, ticked by the fight, ended with it. `AreaHazard`: a `Telegraph.Shape` that hurts players inside every N ticks for a duration (fire pool, fire trail, root line). Real blocks only through `fight.blocks()` (`ArenaBlocks`: remembers the first state of every block it changes, restores all of them when the fight ends — kill or reset) |
| Arena fog | S2C `embervoid:arena_fog` (source id, density 0–1, colour; negative density = clear). `fight.setFog(density, colour)` / `clearFog()`; sent to every fighter, cleared when a fighter leaves and when the fight ends. Client: `client.boss.ArenaFogClient` pushes it into reach-world's `RealmFog` (PLAN 9.1) as not clearable, source `embervoid:arena/<boss path>`. |
| Telegraph shapes | `TelegraphShape`: `Ring` (disc at a point), `Line` (from, to, half width), `Cone` (apex, direction, range, half angle), `Shadow` (falling shadow: a filled disc that darkens/closes as the windup runs out). Each draws itself for the windup and answers `contains(pos)`; `Telegraph.windup(caster, shape, ticks, animation, sound, particle, attack)` and `Telegraph.playersIn(world, shape)` |
| Shield-breaking hit | `BossHits.shieldBreaking(attacker, target, amount)`: a blocking player's shield goes on cooldown (vanilla axe disable, always) and the hit lands in full |
| Fight-bound adds | `fight.spawnAdd(type, pos)`: persistent, level = boss level − `boss.addLevelOffset` (3), aimed at a fighter, discarded when the fight ends; `fight.adds()` |
| Non-HP phase | `ArenaBoss.scriptedPhases()` (default 0): the first N phases end only by `fight.advancePhase()`; HP thresholds count after them |
| Multiplayer scaling | At summon with n fighters: max HP ×(1 + `boss.healthPerExtraFighter`·(n − 1)) (fixed-UUID modifier, full health), `boss.addsPerExtraFighter`·(n − 1) adds of `ArenaBoss.scalingAdd()` |
| Target switching | Every `boss.targetSwitchMinSeconds`–`MaxSeconds` (8–10) with ≥ 2 fighters: the boss turns to whoever dealt it the most damage in that window, or to another fighter if nobody did. `ArenaBoss.switchesTargets()` opts out |

Fixed decisions (small, mine):
- Arena blocks are restored on every end of a fight (kill too), so a rematch or the next player finds the arena whole.
- Adds are 3 levels below the boss: Colossus 18 → 15, Tree 21 → 18, Keeper 24 → 21, Lord 30 → 27 (each the bottom of
  its zone band).
- Fighters are fixed at summon (as before); HP and adds scale to that count.

## 12.2 Arenas — design

Templates `embervoid:ashen_reach/arena_fields|arena_woods|arena_necropolis` (`data/embervoid/structures/ashen_reach/*.nbt`),
written by datagen (`client.reach.boss.datagen.ArenaTemplates`, through the Sanctum's `TemplateBuilder`). Contract with
reach-world (their `ArenaSite` / `placeTemplate`): square of side 2·site + 1, centred, y = 0 the floor, north (−Z) turned
towards the landing in 90° steps. Layout numbers in `reach.boss.arena.ReachArenas`, used by datagen and by the bosses;
`ArenaFrame` turns layout offsets into world positions with the same rotation rule.

| Arena | Fog wall r / site r | Contents |
|---|---|---|
| Fields — crater market | 40 / 48 | market paving, brick curb at the fog wall, 10 burnt-granary pillars (3×3, 8 high, dead log corners) on a ring r 24, 8 ruined stalls, rubble on the crater floor |
| Woods — Royal Grove | 48 / 56 | podzol/moss garden, gravel cross and ring paths, hedge rows, 12 lantern posts, benches, brick garden wall at r 51–52 (5 high) with a 7-wide gate north |
| Necropolis — sinkhole ossuary | 32 / 40 | lower floor (tuff, cracked bricks, moss), walkways 2 high (centre dais, cross, ring r 11.5–13.5, 4 brazier pedestals) that stay dry in 2 blocks of water, 4 unlit campfire braziers, bone columns and a roof ring r 19–31 at y 10 (9 blocks headroom, fallen in places), switchback stair of half-block steps from the rim (y 18) down the pit wall |

Every arena: shrine platform on the north axis at fog radius + 4 (the arena structure puts the shrine block there), the
arena-side chest two blocks east of it (`chests/ashen_reach/<zone>_arena`). The boss spawns on the lowest free spot over
the centre (the Necropolis dais).

Asked reach-world: `shrinePos()` on the template's north axis (centre + rotation(0, 1, −(r + 4))); arena JSON boss radius
40 / 48 / 32, level 18 / 21 / 24, site radius 48 / 56 / 40.

## Shared boss base

`reach.boss.ReachBoss` (HostileEntity + `ArenaBoss` + GeoEntity): synced phase byte, `movement` + `action` controllers,
a brain that walks to the target and calls `chooseMove` when its cooldown ends, stands still during a windup,
`telegraph(shape, ticks, animation, sound, particle, hit)` hits the players in the marked shape, `afterStrike` chains
moves. No fall, drowning or in-wall damage, not pushable, never despawns. Phase effects run in `tickBoss` every server
tick (also with the AI off, as in the GameTests). Renderers in `client.reach.boss.ReachBossClient` (glowmask, toggled
bones). Greybox assets: `python3 tools/boss_placeholders.py [name]` (helpers from `tools/mob_placeholders.py`). Music:
`music.nether.basalt_deltas` as the shared zone-boss placeholder (`ReachBoss.ZONE_BOSS_MUSIC`).

## 12.3 Crypt Keeper

`embervoid:crypt_keeper`, 1.6 × 4.0, base HP 180 (~428 at Lv 24), armor 8, KB res 1, speed 0.22 / 0.24 / 0.26 by phase,
300 XP before level scaling. Loot table (every kill): 2–4 Raw Grave Iron, 3–6 bones, 1–2 candles.

| Move | Telegraph | Base dmg |
|---|---|---|
| Halberd 1 / 2 | cone r 4.8, ±55°, 14 / 10 ticks | 5 each |
| Overhead (3rd hit) | red line 6.5 × 2.2, 22 ticks; shield-breaking | 10 |
| Sweep | ring r 5 around it, 24 ticks; knockback | 7 |
| Lantern throw (P1) | falling shadow r 2.5 at the target, 20 ticks; then a fire pool 5 s (2 per 0.5 s, burning) | 5 |
| Thrust (P2, P3, far target) | line 9 long, 18 ticks; lunges forward | 7 |

- P2 (60 %): lantern shatters (bone hidden), Darkness on fighters (refreshed every 2 s), water to 1 block on the lower
  floor over 3 s (`FloodHazard`, radius fog wall + 7, so the whole sinkhole floor), half damage unless block light ≥ 8
  where it stands (braziers are campfires: flint and steel, fire charge, burning arrow; a torch works too). Crypt Moths:
  a wave every 15 s while a brazier burns (2 if two or more burn, at most 4 alive), steered at the nearest lit brazier,
  smother it at 1.6 blocks.
- P3 (30 %): water to 2 blocks; 3 Restless Bones (+1 per extra fighter) rise on the floor.
- Adds: Restless Bones per extra fighter at summon.
- Braziers are found at summon (every campfire in the arena) and remembered, so a reset or kill leaves them unlit.
- Gear interplay (not built here): Amber Lantern Oil's offhand lantern (11.7) is not block light; if it should light
  the Keeper, reach-gear can tell me and `isLit` takes a held-light check.

## 12.4 Ash Colossus

`embervoid:ash_colossus`, 2.4 × 4.5, base HP 180 (~364 at Lv 18), armor 10 → 3 in P2, KB res 1, speed 0.2 → 0.26,
250 XP before level scaling. Loot (every kill): 3–6 Cinder Shard, 2–5 charcoal. Adds per extra fighter: Cinder Husk.

| Move | Telegraph | Base dmg |
|---|---|---|
| Fist | cone r 5.5 ±45°, 18 ticks; knockback | 8 |
| Arm drag | cone r 6 ±70°, 16 ticks; pulls players in | 6 |
| Leap (target 8–22 away) | ring r 4.5 at the target's spot, 22-tick crouch, then the leap; hits on landing | 9 |
| Boulder | falling shadow r 2.5 at the target, 30 ticks | 7 |
| Pillar smash | cone at the pillar, 20 ticks; every block within 2 of the hit column, floor + 1 to + 10, goes (back after the fight) | 8 to players in the cone |
| Ash storm (every 20 s, first after 10 s) | 30-tick roar, then 6 s: 2 HP/s magic (flat, not level-scaled) + Blindness to every fighter it can see | — |
| Cinders (P2, every 2.5 s) | a falling shadow r 2 near each fighter, 30 ticks; fire, burning 3 s | 5 |

- Cover: a block between its core (80 % of its height) and the player's eyes. When its target is behind cover within 5
  blocks it smashes it; farther away it walks up to it.
- Damage by direction (from the damage source's position, against its facing): front ±60° ×0.5, back beyond ±120° ×1.5
  (core covered, P1) / ×2 (core bare, P2), sides ×1.
- Model bones `plates` (P1) and `core` (P2) toggle with the phase.

## 12.5 Grieving Tree

`embervoid:grieving_tree`, 3 × 10, base HP 170 (~374 at Lv 21), armor 6, KB res 1, rooted in P1 (speed 0), 0.12 in P2,
280 XP before level scaling. Loot (every kill): 3–6 Royal Amber, 2–5 dead logs. Adds per extra fighter: Withered Thrall.

- Lanterns: `embervoid:tree_lantern` (`TreeLanternEntity`, not saved, follows its tree at an angle/distance/height,
  drawn as a lit hanging lantern with chain). 5 in P1 at 7–8.5 up, 5.5 out; any projectile breaks one (snowballs too); a
  melee hit only while lowered (to 1.6 up for 3 s, the one nearest the end of a branch sweep). With fewer than 2 left, one
  grows back every 30 s.
- Broken lantern: stun 5 s (trunk ×1.5, no moves, windups cancelled), fog lifts 6 s. Unstunned trunk ×0.3 (both phases).
- Fog: `fight.setFog(0.55, 0x3c4640)` at summon, 0.75 in P2.

| Move | Telegraph | Base dmg |
|---|---|---|
| Root lines | a line 18 long, ±1 wide, from its base to every fighter, 26 ticks; Slowness III 2 s, tossed up | 7 |
| Branch sweep | cone r 8 ±50°, 20 ticks; knockback; lowers a lantern | 8 |
| Thralls | every 25 s (not while stunned): 2 Withered Thralls drop from 6 up as fight adds | — |
| Root cage (P2) | ring r 2.5 at the target, 24 ticks; Slowness VI 3 s | 4 |

- P2 (50 %): uproots (animation, dirt), old lanterns fall, 4 new ones on its own branches (5.5–7 up, 3.5 out) walk with
  it, thicker fog. Model bone `roots` shows only while rooted.

## 12.6 Souls and Flask Shards

- Framework: `boss.BossReward(soul, firstKill extras)` registered per boss progress id (`BossReward.register`); the
  fight hands it out instead of the Ember Heart. First kill: soul + extras, message "%s has fallen. Yours: …". A credited
  fighter who had already beaten the boss gets the soul at `boss.repeatSoulChance` (0.25), never in a rematch
  (`BossFight.isRematch`). Bosses without a reward keep the Ember Heart (test boss, any later realm).
- Realm 1 (`ReachBosses`): zone_a Ash Colossus soul, zone_b Grieving Tree soul, zone_c Crypt Keeper soul, each + 1 Flask
  Shard. The Smoldering Lord's (soul + Flask Ember) comes with 13.4.

## Status / notes

- Started 2026-10-22.
- GameTests use `BossArena` (stone floor, real joined players, fights through `ArenaManager.start`) with the framework's
  test boss, AI off. One batch per test (fights are global).
- Server check: not run in this worktree (its run folder needs the Minecraft EULA accepted; left to the coordinator).
- `BossConfig.SECTION` is loaded in `BossInit`, so a config parsed before any fight (tests, the client) has the `boss` keys.
