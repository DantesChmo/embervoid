# Track E — reach-bosses (PLAN 12.1–12.7, 13.4)

Branch `claude/track-reach-bosses-31cdf3`. Rules: [COORDINATION.md](../COORDINATION.md). Design: DESIGN §4.3, §4.5
"Bosses", §10.11 (built as recommended). Framework reference: CORE.md §6.

Owns `com.embervoid.boss` (framework), `com.embervoid.reach.boss`, `com.embervoid.client.boss` (framework client:
arena fog receiver), `com.embervoid.client.reach.boss`, arena contents (`structures/ashen_reach/arena_*`, the Citadel
courtyard piece), boss assets and loot. Config section `boss`.

## Tasks

- [x] **12.1 Boss framework additions** (below; GameTests `BossFrameworkTests`, JUnit `TelegraphShapeTest`, `BossScalingTest`)
- [x] **12.2 Three arenas built** (templates; placed by reach-world's arena site code, 9.4; GameTests `ArenaTemplateTests`)
- [ ] **12.3 Crypt Keeper**
- [ ] **12.4 Ash Colossus**
- [ ] **12.5 Grieving Tree**
- [ ] **12.6 Souls and Flask Shards**
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

## Status / notes

- Started 2026-10-22.
- GameTests use `BossArena` (stone floor, real joined players, fights through `ArenaManager.start`) with the framework's
  test boss, AI off. One batch per test (fights are global).
- `BossConfig.SECTION` is loaded in `BossInit`, so a config parsed before any fight (tests, the client) has the `boss` keys.
