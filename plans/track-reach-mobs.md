# Track B — reach-mobs (0.4.0)

Scope: PLAN 10.1–10.7, then 13.3 (Realm 1 bestiary, DESIGN §4.5). Branch `claude/track-reach-mobs-*`.

| # | Task | Status |
|---|---|---|
| 10.1 | Greybox pipeline, shared base, spawns, base levels, basic drops | merged |
| 10.2 | Cinder Husk, Ash Stalker | merged |
| 10.3 | Ashen Carrion | merged |
| 10.4 | Withered Thrall (+ hanging cage), Lantern Hag | merged |
| 10.5 | Bark Hollow | done |
| 10.6 | Restless Bones, Crypt Moth | done |
| 10.7 | Hollow Sentinel, Grave Warden | done |
| 13.3 | Cinder Hound, Sparking Marksman, Furnace Golem, Smoldering Knight | done |

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
- **Placed.** Command tag `embervoid.placed` (the 9.2 contract: reach-world's `PlacedEnemies.spawn` adds it, with
  `SpawnReason.STRUCTURE`) or `markPlaced()`: persistent, home = spot, `setPositionTarget(home, homeRadius)` +
  `GoToWalkTargetGoal`, so it returns to its spot when idle; saved as `Placed` / `Home`. `onPlaced()` hook.
  `placedEnemiesTakeTheirPlacedState` places all 14 mobs through `PlacedEnemies.spawn`.

## Decisions (small, taken here)

- **10.1 registers all ten zone mobs** (types, eggs, models, drops, plain telegraphed melee) so other tracks can
  place and reference them by id at once; 10.2–10.7 give each its real behaviour and tests.
- **Spawns** (`reachMobs.spawns.<id>` weight / group): Fields Cinder Husk 100 (2–4), Ash Stalker 35 (1), Ashen
  Carrion 8 (1); Woods Withered Thrall 100 (1–2), Lantern Hag 40 (1); Necropolis Restless Bones 100 (2–3), Crypt
  Moth 50 (1–3, under cover only). Only in their zone biome (`embervoid:ashen_reach_zone_a|b|c`). Spawn rule:
  ignore sky light (eternal dusk), block light ≤ 7, not peaceful. Tanks and elites have no natural spawn.
- **Base levels** (outside the realm: spawn eggs; `mobs.levels.baseLevels`, separate commits in `mob`): the bottom
  of the zone band — Fields 15, Woods 18, Necropolis 21, Citadel 25. In the realm the zone band decides.
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

## Bark Hollow (10.5)

- Every Bark Hollow starts **dormant** (placed, egg or not): no AI, not pushable, silent, eyes bone hidden. A
  survival player within 6, or any hit, wakes it: **creak** (wooden-door sound, low wood break) and the awaken
  animation for 20 ticks, eyes lit, then it hunts that player. Alone for 15 s at its spot (or anywhere, if not
  placed) it goes dormant again.
- **Sweep:** 20-tick windup with its feet planted, then every living thing (other Bark Hollows excepted) within
  3 blocks of its edge in the 120° in front takes its melee hit.
- **×1.5** from fire damage (`#is_fire`) and from a melee hit with an axe (direct hits only: an arrow shot by a
  player holding an axe does not count). Applied before armor.

## Restless Bones, Crypt Moth (10.6)

- **Restless Bones:** a blow that would kill it (anything but damage that bypasses invulnerability, e.g.
  `/kill`, the void) collapses it into a **bone pile**: 6/16 of its max health (6 HP at level 1), hitbox
  0.8×0.5, no AI. After 5 s the pile stands up at half health. Killing the pile is a real death; so is **light**:
  a pile at block light ≥ 8 crumbles at once, credited to whoever knocked it down (player kill for loot and XP).
  `reform()` is public for the Grave Warden's bell. Base hook `ReachMob.onStateChanged` (both sides) resizes it.
- **Crypt Moth:** drawn to the nearest light within 12 (block luminance ≥ 10, no fluids), rescanned every 2 s. With
  `mobGriefing` it **snuffs** torches (broken, they drop) and lit candles within 1.8; other lights it just haunts.
  A target within 4 draws it away from the light: **pollen burst** when within 3 (wings glow 15 ticks — the
  `glow_left` / `glow_right` bones, shown through the synced state — then Blindness 3 s for players within 3,
  100-tick cooldown), else a bite (6-tick windup). Natural spawns only under cover.

## Hollow Sentinel, Grave Warden (10.7)

- **Hollow Sentinel:** tower shield: melee (direct hits) and projectiles from the front 180° do nothing, axes
  included (no shield break); magic, fire, explosions and shield-bypassing damage go through. Moves: **spear
  thrust** (12-tick draw, 7 damage, reach 3 blocks past its edge) or, against a target within 2.5, **shield
  bash** (shield planted 15 ticks, 6/7 of its attack damage + strong knockback). After a bash the shield hangs
  **open for 30 ticks**: front hits land. Feet planted during windups. Guard pose while it has a target.
- **Grave Warden:** cudgel swing (12-tick windup, 8 damage). In a fight it rings its bell 100 ticks after finding
  a target, then every 240 ticks (12 s): bell raised **20 ticks** (feet planted), then every bone pile within 12
  re-forms; if there are no Restless Bones within 12 it raises 2 (it keeps at most 4 of its own alive; saved with
  it); players within 4 get Slowness IV for 2 s.

## Citadel mobs (13.3)

Base level 25 (bottom of the depths band). Hitboxes: Hound 0.8×0.9, Marksman 0.6×1.95, Golem 1.4×2.9, Knight
0.7×2.2. All four are fire immune (a burning city).

- **Cinder Hound:** 14 HP, 3 damage, speed 0.35 (chases at ×1.2), XP 7. Bite after a 6-tick lunge; a hit sets the
  target alight for 2 s. When it picks a new target it **howls** and every idle hound within 24 takes the same
  target (no chain howls). Natural packs of 3–5 in the depths biome (`embervoid:ashen_reach_depths`, weight 100).
- **Sparking Marksman:** 24 HP, XP 20, placed only. No movement goals and no knockback: it **never leaves its
  post**, only turns. With a clear line within 24 it **aims for 20 ticks** — the `spark` bone on the bolt tip
  shows, sparks and small flames at the tip — then fires a burning crossbow bolt (an arrow at 3.15 blocks/tick,
  6 damage, sets alight, cannot be picked up). 60-tick cycle.
- **Furnace Golem:** 90 HP, armor 12, KB res 0.9, 9 damage (slam, 14-tick windup), speed 0.15, XP 45, placed only.
  Every hit it takes stokes it; the **4th** makes it glow (`heat` bone, flames and smoke) for **25 ticks** with
  its feet planted, then a **fire wave r 4**: 0.7 × its attack damage as fire damage, 4 s burning, a push. Then it
  is cold again.
- **Smoldering Knight:** 100 HP, armor 10, KB res 0.6, 8 damage (not in the bestiary; chosen), speed 0.25, XP 80,
  placed only. **3-hit combo** with windups 6 / 5 / 8 ticks; blows 2 and 3 land inside the target's hurt cooldown
  (else the combo would be eaten by i-frames). A visible target 4–7 blocks away draws (1 in 10 per tick) the
  **lunge**: a 15-tick crouch (feet planted), then an 8-tick dash (~6 blocks) that hits the first time it touches
  its target for 1.25 × damage.
- **Drops:** Hound bone 0–2, charcoal 0–1; Marksman arrow 0–2, string 0–1, crossbow 4 % (player kill); Golem iron
  nugget 2–6, coal 1–3; Knight iron nugget 2–5, charcoal 1–2, iron ingot 15 % (player kill).
- Russian: Угольная гончая, Искрящий стрелок, Печной голем, Тлеющий рыцарь.

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
| `barkHollowWakesWhenAPlayerComesClose` | dormant (eyes off) with a player at ~10; at ~4 it stirs (eyes on), 20 ticks later it is up and targets the player |
| `barkHollowTakesMoreFromAxesAndFire` | armor off: bare hand 4 → 4 lost, axe 4 → 6, fire 4 → 6 |
| `barkHollowSweepsInFront` | nothing at 19 ticks; at 22 the dummies in front and front-left are hit, the one behind is not |
| `restlessBonesCollapseAndReform` | lethal blow → pile at 6/16 health, under 1 block tall; still a pile at 95 ticks, standing at half health at 105 |
| `finishedPileAndKillStayDead` | killing the pile is a death; `/kill` kills a standing one outright |
| `litPileStaysDead` | a pile next to a torch crumbles within 5 ticks |
| `cryptMothSnuffsTorches` / `…LeavesTorchesWithoutGriefing` | flies to a torch 5 blocks off and snuffs it; with `mobGriefing` off it hovers there and the torch stays |
| `cryptMothPollenBurstBlinds` | wings glow during the 15-tick windup; then the player 2 blocks off is blind, the one 8 off is not |
| `sentinelShieldStopsFrontHits` | front punch, axe and arrow do nothing; a hit from behind lands |
| `sentinelBashOpensTheShield` | no damage before 15 ticks; then damage + knockback, shield open, a front hit lands; 30 ticks later front hits are blocked again |
| `wardenBellReformsPilesAndStaggers` | a pile 8 blocks off re-forms at the ring (not before), a player 3 blocks off gets Slowness IV |
| `wardenBellRaisesBonesUpToFour` | no bones around: a ring raises 2; again 2; a third ring raises none (cap 4) |
| `placedEnemiesTakeTheirPlacedState` | all 14 mobs through `PlacedEnemies.spawn`: placed, persistent; stalker buried, carrion perched, cage thrall hanging, Bark Hollow dormant |
| `houndHowlPullsThePack` | a hound taking a target: idle hounds within 24 take it, one 32 off does not |
| `houndBiteSetsAlight` | no bite before 6 ticks; then damage and fire |
| `marksmanSparksThenFiresABurningBolt` | spark shown and no bolt during the 20-tick aim; then one burning bolt; it hits a target 10 blocks off |
| `marksmanNeverLeavesItsPost` | placed, with a target and a knockback: still on its spot after 5 s |
| `furnaceGolemBlastsAfterFourHits` | 3 hits: heat 3, no glow; 4th: glowing; 25 ticks later the cow 3 blocks off is hurt and burning, the one 7 off untouched; cold again |
| `knightComboLandsThreeBlows` | one combo: three separate blows land within 19 ticks |
| `knightLungesAfterACrouch` | still during the 15-tick crouch; then it has dashed > 3 blocks and hit the target 6 blocks off |
