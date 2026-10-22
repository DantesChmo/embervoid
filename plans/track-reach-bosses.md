# Track E — reach-bosses (PLAN 12.1–12.7, 13.4)

Branch `claude/track-reach-bosses-31cdf3`. Rules: [COORDINATION.md](../COORDINATION.md). Design: DESIGN §4.3, §4.5
"Bosses", §10.11 (built as recommended). Framework reference: CORE.md §6.

Owns `com.embervoid.boss` (framework), `com.embervoid.reach.boss`, `com.embervoid.client.boss` (framework client:
arena fog receiver), `com.embervoid.client.reach.boss`, arena contents (`structures/ashen_reach/arena_*`, the Citadel
courtyard piece), boss assets and loot. Config section `boss`.

## Tasks

- [x] **12.1 Boss framework additions** (below; GameTests `BossFrameworkTests`, JUnit `TelegraphShapeTest`, `BossScalingTest`)
- [ ] **12.2 Three arenas built**
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
| Arena fog | S2C `embervoid:arena_fog` (source id, density 0–1, colour; negative density = clear). `fight.setFog(density, colour)` / `clearFog()`; sent to every fighter, cleared when a fighter leaves and when the fight ends. Client: `client.boss.ArenaFogClient` pushes it into reach-world's `RealmFog` (PLAN 9.1) as not clearable, source `embervoid:arena/<boss path>`. Stub (no-op) until 9.1 is on `main` |
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

## Status / notes

- Started 2026-10-22.
- GameTests use `BossArena` (stone floor, real joined players, fights through `ArenaManager.start`) with the framework's
  test boss, AI off. One batch per test (fights are global).
- `BossConfig.SECTION` is loaded in `BossInit`, so a config parsed before any fight (tests, the client) has the `boss` keys.
