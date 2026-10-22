# Track B — reach-mobs (0.4.0)

Scope: PLAN 10.1–10.7, then 13.3 (Realm 1 bestiary, DESIGN §4.5). Branch `claude/track-reach-mobs-*`.

| # | Task | Status |
|---|---|---|
| 10.1 | Greybox pipeline, shared base, spawns, base levels, basic drops | merged |
| 10.2 | Cinder Husk, Ash Stalker | done |
| 10.3 | Ashen Carrion | done |
| 10.4 | Withered Thrall (+ hanging cage), Lantern Hag | done |
| 10.5 | Bark Hollow | todo |
| 10.6 | Restless Bones, Crypt Moth | todo |
| 10.7 | Hollow Sentinel, Grave Warden | todo |
| 13.3 | Cinder Hound, Sparking Marksman, Furnace Golem, Smoldering Knight | todo |

## Layout

- `reach.mob` — `ReachMob` (shared base), `FlyingReachMob`, one `<Name>Entity` per mob, `ReachMobs` (types under the
  fixed ids, attributes, spawn eggs, natural spawns), `ReachMobConfig` (`reachMobs` config section).
- `client.reach.mob` — `ReachMobClient` (GeckoLib renderers with glowmasks, toggled bones), `ReachMobDataGen`
  (entity loot tables).
- Assets: greybox models, animations and textures from `tools/mob_placeholders.py` (all mobs, or
  `python3 tools/mob_placeholders.py <id>...`). **They need the art pass (R40.2).** Models, animations and glow
  have never been seen in game (no client launches on this track).
- Tests: `src/gametest/.../ReachMobTests`.

## Shared base (10.1)

- **Moves.** `ReachMob.Move(animation, windupTicks, cooldownTicks, sound, pitch)`. `startMove` triggers the
  animation on the `action` controller and the sound (the telegraph); `performMove` runs `windupTicks` later on the
  server, unless `cancelMove` (stagger, death). Ticked in `tick()`, so it also runs with the AI off. The cooldown
  starts with the move, windup included. `ReachMeleeGoal` chases and starts `meleeMove()` in reach; a melee move
  lands only if the target is still within reach × 1.3. `rootedDuringWindup()` stops heavy mobs while they wind up.
- **Animations.** Every move's animation contains its own windup and ends where the entity lands the move (ticks /
  20 = seconds), so swapping in art keeps the timing. Controllers: `movement` (walk/idle, flyers `move.fly`),
  `state` (looping animation of a synced state byte: buried, dormant, bone pile, hanging, guard…; pauses
  `movement`), `action` (one-shot move animations). Bones shown only at times (`toggledBones` / `showsBone`) are
  hidden by the client model each frame (moth glow wings, Bark Hollow eyes).
- **Placed.** Command tag `embervoid.placed` (proposed to track reach-world for the 9.2 processor) or
  `markPlaced()`: persistent, home = spot, `setPositionTarget(home, homeRadius)` + `GoToWalkTargetGoal`, so it
  returns to its spot when idle; saved as `Placed` / `Home`. `onPlaced()` hook. Until 9.2 is merged, GameTests add
  the tag themselves.

## Decisions (small, taken here)

- **10.1 registers all ten zone mobs** (types, eggs, models, drops, plain telegraphed melee) so other tracks can
  place and reference them by id at once; 10.2–10.7 give each its real behaviour and tests.
- **Spawns** (`reachMobs.spawns.<id>` weight / group): Fields Cinder Husk 100 (2–4), Ash Stalker 35 (1), Ashen
  Carrion 8 (1); Woods Withered Thrall 100 (1–2), Lantern Hag 40 (1); Necropolis Restless Bones 100 (2–3), Crypt
  Moth 50 (1–3, under cover only). Only in their zone biome (`embervoid:ashen_reach_zone_a|b|c`). Spawn rule:
  ignore sky light (eternal dusk), block light ≤ 7, not peaceful. Tanks and elites have no natural spawn.
- **Base levels** (outside the realm: spawn eggs; `mobs.levels.baseLevels`, separate commit in `mob`): the bottom of
  the zone band — Fields 15, Woods 18, Necropolis 21 (Citadel 25 in 13.3). In the realm the zone band decides.
- **Windups not given in the bestiary:** Ash Stalker claw 8, Withered Thrall 10, Restless Bones 10, Crypt Moth bite
  6, Grave Warden swing 12 ticks; Lantern Hag speed 0.25, Hollow Sentinel 0.2, Grave Warden 0.22 (KB res 0.5),
  flyers flying speed 0.6 (Carrion) / 0.5 (Moth).
- **Hitboxes (final size):** Husk 0.6×1.95, Stalker 0.6×1.6, Carrion 1.2×0.9, Thrall 0.6×1.95, Hag 0.6×1.8, Bark
  Hollow 1.4×3.2, Bones 0.6×1.99, Moth 0.7×0.6, Sentinel 0.8×2.3, Warden 0.9×2.4.
- **Basic drops** (+Looting): Husk rotten flesh 0–2, charcoal 0–1; Stalker bone 0–2, charcoal 0–1; Carrion
  feather 1–3, rotten flesh 0–1; Thrall rotten flesh 0–2, string 0–2; Hag glass bottle 0–1, charcoal 0–2, lantern
  5 % (player kill); Bark Hollow stick 1–4, charcoal 0–2; Bones bone 0–2; Moth string 0–1, glowstone dust 0–1;
  Sentinel iron nugget 1–4, bone 0–2; Warden iron nugget 2–5, bone 1–3, candle 25 % (player kill).
- **Russian names:** Обугленный остов, Пепельный ловчий, Пепельный падальщик, Иссохший невольник, Карга с фонарём,
  Полый древень, Неупокоенные кости, Склепная моль, Полый часовой, Кладбищенский звонарь.

## Cinder Husk, Ash Stalker (10.2)

- **Cinder Husk:** plain telegraphed melee from the base (10-tick raised-arm windup, 30-tick cooldown); natural
  groups of 2–4 in the Fields.
- **Ash Stalker:** buried state (synced state byte; drawn invisible through `isInvisible`, since vanilla resets the
  invisibility flag from status effects), invulnerable, not pushable, no AI. A survival player within 5 blocks sets
  it off: 20 ticks of boiling ash, hiss and the emerge animation (now visible, still invulnerable), then a leap at
  that player (hits once on contact within 15 ticks), then normal melee (8-tick claw). Without a target for 10 s on
  the ground it buries itself again. Natural and placed ones start buried; spawn eggs do not.
- **Scarecrow's Mask:** `AshStalkerEntity.addAmbushImmunity(Predicate<PlayerEntity>)`; `ReachMobs` registers
  `GearArmor.hiddenFromAmbush` (track reach-gear, 11.4), so a player wearing the mask never sets a stalker off.

## Ashen Carrion (10.3)

- Its own flight (velocity steering, no movement goals; targets from revenge and players). Without a target it
  hovers ~4 blocks up, and after 5 s comes down and **perches** (natural, egg-less spawns and placed ones start
  perched: on scarecrows, outpost walls, wherever the structure puts it). A target, or a hit, makes it take off.
- With a target it **circles** 7 blocks out, 6 up, and **dives**: every 160–240 ticks (8–12 s), or as soon as the
  target is below half health (at most once per 40 ticks). **Telegraph:** 15-tick shriek (ghast scream, high
  pitch) while it hangs in the air and a shadow ring (squid ink + smoke, closing in) gathers at the target's feet.
  The dive flies straight at the target for up to 40 ticks, hits once on contact (peck), then climbs back.

## Withered Thrall, Lantern Hag (10.4)

- **Withered Thrall:** natural ones walk and claw (10-tick windup). A placed one with the extra tag
  `embervoid.placed.cage` (contract with reach-world, recorded in COORDINATION) **hangs** where it was placed: no
  gravity, no AI, not pushable, silent. A survival player within 1.5 blocks horizontally and up to 16 below sets
  it off: **chain rattle for 10 ticks** (still hanging), then it drops (opening a closed trapdoor right under it, if
  the cage has one), hits a player it lands on and fights on. Shot or hit while hanging, it drops at once.
  **For reach-world's cages:** the cage floor must be open or a trapdoor; the thrall stands on the cage floor
  block's top.
- **Lantern Hag:** keeps 8–12 blocks from her target (backs off closer, walks up farther or out of sight) and
  throws when she sees it at 4–14 blocks. **Telegraph:** lantern raised 15 ticks (flame particles at it), then the
  flask flies in the witch's arc at where the target stands. **Fire flask** (`embervoid:fire_flask`, its own
  entity, drawn as a splash potion item): 4 damage within 1.2 of the impact, then a 3 s fire patch r 1.5 (the same
  entity, invisible, flame particles) that sets everything standing in it alight, the hag excepted. No fire blocks.

## GameTests (`ReachMobTests`, `ReachFieldsTests`, …)

Test API traps: `TestContext.spawnMob` clears the mob's goals (AI tests use `ReachTests.mob`, i.e.
`spawnEntity`; `ReachTests.still` when only the mob's own tick logic should run), and the mock test player always
says `isCreative()`, so mobs that skip creative players ignore it (tests use `ReachTests.player`, a real joined
survival player). Test areas are reused by later batches and entities outlive their test (a leftover iron golem
once drew another suite's zombie), so everything a test spawns goes through `ReachTests` and `ReachTests.finish`
removes it; `ReachTests.guard` does the same when an assertion fails. Targets are `ReachTests.dummy`: a 100 HP cow
with no AI (nothing hunts cows). Tests where a mob reacts to players or chases run in a batch of their own. Relative
y 0 is the ground; a mob without AI spawned at y 1 floats.

| Test | Checks |
|---|---|
| `meleeLandsAfterTheWindup` | Cinder Husk swing: no hit after 9 ticks, hit after the 10-tick windup; no second move during a windup |
| `cancelledOrOutOfReachMoveDoesNotLand` | a cancelled windup and a target out of reach take no damage |
| `placedTagMakesAGuard` | tag → placed, persistent, guards its spot; survives save/load; ordinary spawn not placed |
| `naturalSpawnsOnlyInTheirZone` | spawn entries only in the right zone biome; tanks/elites nowhere |
| `everyMobHasBasicDrops` | every mob has a loot table |
| `cinderHuskChasesAndHitsAfterTheWindup` | with AI on the husk walks up and hits no sooner than its windup |
| `ashStalkerAmbushesAPlayerWithinFive` | buried: invisible, no damage; player at 7 nothing; at 3 → boils 20 ticks (invulnerable) → out, targets the player |
| `ashStalkerReburiesWhenAlone` | out with nothing to hunt → buried again after 10 s |
| `scarecrowMaskKeepsTheStalkerBuried` | a player wearing the Scarecrow's Mask does not set it off |
| `carrionDivesEveryEightToTwelveSeconds` | first shriek ≥ 155 ticks in, dive 15 ticks after the shriek, the target is hit by the dive |
| `carrionDivesAtAWoundedTarget` | a target below half health draws a shriek at once |
| `carrionPerchesAndTakesOff` | no target: perched after 10 s; a target makes it take off |
| `cagedThrallDropsOnAPlayerPassingUnder` | hangs with a player 3 blocks aside; a player under it → rattle, still hanging at 9 ticks, then falls and targets the player |
| `cagedThrallOpensTheTrapdoorUnderIt` | a trapdoor cage floor opens and the thrall falls through |
| `lanternHagThrowsAFlaskThatBurns` | no flask during the 15-tick lantern raise, a flask after it; the target is hurt and set alight |
| `lanternHagKeepsHerDistance` | a target 3 blocks away: she backs off to ≥ 6 |
