# Embervoid — Parallel work

The roadmap ([ROADMAP.md](ROADMAP.md), tasks in [PLAN.md](PLAN.md), design in [DESIGN.md](DESIGN.md)) is worked on
by track sessions at once. Each track has its own git worktree and branch. The **coordinator** session (in the
main checkout; the session that started the tracks, `startedBy` in the session list) merges tracks into `main`,
owns the docs, and is the only place where the game is launched.

## Tracks — 0.5.0 visual overhaul (the night of 2026-10-24)

**Read this first in a new session.** The user (2026-10-24, evening): the mod must look as modern as Create. Better
textures everywhere, denser models, many more blocks used in every structure, moving machinery (the Fields windmill
turning, Create-style), boss arenas that move with the fight, far more effects (the whip and the scythe must be
*very* effective and unique), a far richer fog wall. The bar: [docs/art-direction.md](docs/art-direction.md). The
coordinator was left for the night with no questions allowed: every session decides for itself, the coordinator
merges, regresses once at the end and writes the docs. At most **3 track sessions** at once (the app runs out of
memory above that), the coordinator in the main checkout on `main`.

| Track | Session | Owns | Does |
|---|---|---|---|
| V-A | `art-blocks` | `tools/palette.py`, `tools/block_art.py` (replaces `reach_world_placeholders.py`, `road_art.py`, the block parts of `stele_art.py`), `tools/prop_art.py`, `textures/block/**` except `setpiece/`, `registry/ModBlocks`, `reach/world/ReachBlocks`, the block datagen in `EmbervoidDataGenerator` / `ReachWorldDataGen`, the template builders (`SanctumTemplates`, `HubTemplates`, `MinorTemplates`, `OutpostTemplates`, `CryptTemplates`, `CitadelTemplates`, `CityBuilder`, `Ruin`, `TemplateBuilder`), their JUnit, lang | Every block texture redrawn to the art direction; about 25 new building blocks; every structure rebuilt from the wider palette with decay gradients; props restyled |
| V-B | `bosses` | `tools/reach_boss_art.py`, `tools/sanctum_boss_art.py`, the Vampire boss's art script, the six bosses' `geo` / `animations` / `textures`, `reach/boss/*Entity`, `sanctum/boss/FallenSisterEntity`, `vampire/VampireBossEntity`, their client renderers and layers, `boss/Telegraph*` | Boss remodels to 80–140 cubes with secondary motion, death / hurt / stagger / phase animations, heavier `CombatFx` and particles on every move |
| V-C | `setpieces` | new `block/setpiece` + `client/setpiece`, `tools/setpiece_art.py`, `geo/block/**`, `animations/block/**`, `textures/block/setpiece/**`, `ArenaTemplates`, an arena-events hook in `boss/BossFight` (one small commit), set-piece *placement* lines in V-A's template files (last step, after `git merge main`) | 20.1 framework, 20.2 moving set pieces, boss arenas that react to the fight |
| — | coordinator (this checkout) | `reach/gear/*`, `sanctum/gear/*`, `client/reach/gear/*`, `client/sanctum/gear/*`, `fx/**`, `client/fx/**`, new particles (`fx/particle`, `textures/particle/**`, `particles/**`), `client/realm/FogCurtain`, the fog wall's look (`BossFight`'s wall drawing moves to the client), weapon in-hand models (PLAN 17.2), docs | Weapon signatures and behaviour, own particles, fog walls, merges, the final regression |

Wave 2: `mobs` (the 14 Realm 1 and Citadel mobs to 30–80 cubes with PLAN 19.3's hurt / death / stagger
animations, `tools/reach_mob_art.py`, a new `tools/mob_kit.py`, branch `claude/mobs`) and `overworld-mobs` (the seven
overworld and moon-night mobs, `tools/overworld_mob_art.py`, a new `tools/overworld_kit.py`, branch
`claude/overworld-mobs`), both as the coordinator's background agents in their own worktrees, since the app lets a
session start only from the user's own message. Left for later: `art-items` (every item, armor and GUI texture,
`tools/reach_item_art.py` and friends; the icons already follow vanilla's rules and read well on the contact sheet).

### Merged so far (the night of 2026-10-24)

| What | Commits on `main` |
|---|---|
| Coordinator: own particles, every weapon's signature, fog walls as volumes, in-hand weapon models, `ArenaWallTests` | `0eb5222`, `983b2a3`, `673d76d`, `b23d58c`, `d8bfb86` |
| V-C `setpieces`: 20.1 framework, 20.2 fourteen moving set pieces, arenas that move, placement into the templates | merge `efd9d3c` |
| V-A `art-blocks`: `tools/palette.py`, every block texture redrawn, 25 new blocks, every structure rebuilt (conflicts with V-C's placement lines resolved by keeping both, templates regenerated) | merge `50b90b6` |
| V-B `bosses`: the six bosses at 71–128 cubes with secondary motion, hurt / stagger / death / phase animations, eased moves, `EmberHaloLayer`; art-kit helpers (`chamfered`, `octagon`, `chain_bones`, `lag_sway`, `whip`, eased keys) | merge `c1c19d5` |

Not yet merged: `mobs` (`claude/mobs`, 14 Realm 1 mobs) and `overworld-mobs`
(`claude/overworld-mobs`, the seven overworld and moon-night mobs; both run as the coordinator's background agents in
their own worktrees). Nothing of the overhaul has been seen in game; the full regression runs once at the end.

### Rules for the overhaul tracks

Everything under "How the work runs" above, plus:

- **Never run** `./gradlew build`, `runGametest`, `tools/server_check.py`, a client or HeadlessMc. Check with
  `./gradlew compileJava compileClientJava compileTestJava compileGametestJava`, `./gradlew runDatagen` whenever
  assets or data change (commit `src/main/generated`), and `./gradlew test --tests '<your classes>'`. Write
  GameTests for new behaviour; the coordinator runs them once at the end.
- **Look at your output.** Render with `tools/model_preview.py` and read the PNGs; iterate until it meets
  docs/art-direction.md. A track that reports without having looked has not finished.
- **No questions, no waiting.** Decide, note the decision in `plans/<track>.md`, go on. Nothing needs approval.
- **Keep the game as it plays:** hitboxes, move timings, damage, loot, structure footprints, jigsaw names, placed
  enemies and chests stay; `EntityModelFilesTest`, the template JUnit and `python3 tools/model_preview.py check`
  stay green.
- **Shared files:** lang keys as one contiguous block per track; one registration line each in `Embervoid` /
  `EmbervoidClient` / the datagen entry point / the GameTest list; `src/main/generated` is regenerated, never edited.
- **Done:** commit per task on the own branch, then `SendMessage` to the coordinator with `MERGE REQUEST`, the branch,
  the commits and what was not seen offline. Then stop; the coordinator deletes the session after the merge.

## Tracks — 0.5.0 (the 0.4.1 playtest)

**Read this first in a new session.** State as of 2026-10-24. Tasks and their status lines: PLAN.md "Playtest 0.4.1 —
release 0.5.0" (milestones 14–21, one release: the user asked for no string of small versions). Decisions:
DESIGN.md §1, §2, §4.1, §4.5, §5.5, §6, §6.1 and §10.13–10.15. Release checklist: ROADMAP.md "0.5.0".

### How the work runs

- **Coordinator:** a session in the main checkout, on `main` itself (no coordinator branch this time). It writes the
  design passes with the user (15.0, 17.0, 21.0 were done that way), starts track sessions, merges them into `main`
  after the user says yes, runs the full regression and keeps the docs. Nothing is pushed until the user asks; tags
  `v0.4.0` / `v0.4.1` are still unpushed.
- **Tracks:** at most 4 sessions at once, each in its own worktree on a branch `claude/<slug>-<hash>`, briefed with:
  the PLAN task and the DESIGN sections to read, what is already on `main`, what it owns and must not touch, the
  rules below, and "report your branch and a summary". Finished sessions are deleted after their merge.
- **Track rules:** commit only on the own branch, never merge or push; author DantesChmo <doni-rio@yandex.ru>, no
  Co-Authored-By / Claude / "Generated with" lines; never `./gradlew clean` (it wipes the tracked
  `src/main/generated`); never launch the client or HeadlessMc; every string in en_us and ru_ru; visuals reported as
  not seen. **No full test run in a track** (the user, 2026-10-23): compile all source sets, `runDatagen` when assets
  change, own new or changed JUnit only; GameTests and server-check steps are written but run by the coordinator.
- **Merging a batch** (coordinator): check authors and attribution (`git log main..branch`), find conflicts without
  touching `main` (`git merge-tree --write-tree`), ask the user, `git merge --no-ff` per branch. So far conflicts were
  only both sides adding a line to the same list (registration in `Embervoid.java` / `EmbervoidClient.java`, the
  datagen entry point, the GameTest list in `src/gametest/resources/fabric.mod.json`, a generated tag) — keep both —
  and doc paragraphs (merge by hand).
- **Full regression after every merged batch:** `./gradlew runDatagen` (must leave `src/main/generated` unchanged),
  `./gradlew build`, `./gradlew runGametest`, `./gradlew balanceTargets` (the 0.5.0 balance targets; red rows are
  18.3's until it is done), `python3 tools/server_check.py`. The server check needs the accepted EULA in
  `run/server-check`, which exists only in the main checkout: do not run it from a temporary worktree.
- What the regression found so far: the Scourge's crack re-entered itself through Fabric's attack event (stack
  overflow on every whip hit), fixed in `ac990f8`. A track that skips GameTests can merge a crash; read the failure
  list, not only the exit code.

### Batches merged into `main`

| Batch | Tasks | Merges |
|---|---|---|
| 1 | 14.1 boss music volume, 14.2 roads, 19.1 offline viewer | `58d3d6d`, `a58a5ef`, `b1e383f` |
| 2 | 15.1 hub dimension, 15.3 Cleft Stele blocks, 19.2 combat effects, 20.3 props | `ba5cc89`, `238613a`, `8a5314c`, `8e71b4e` |
| 3 | 15.2 travel through the hub, 16.1 Sanctum → dungeon, 17.1 attack animations (playerAnimator), 18.1 balance model | `51c099d`, `97d957b`, `260350d`, `a1e72e7` |
| 4 | 15.4 lit stele and travel, 16.3 Fallen Sister, 17.3 weapon stats, 21.1 four sectors | `722f3b7`, `8dfe82e`, `f51f005`, `1e98a20` |
| 5 | 16.2 dungeon variants, 17.4 whip, 18.4 Realm 1 retune, 21.2 Citadel Gate and lock | `61d03b2`, `8f38200`, `a062129`, `aee6b00` (+ fix `ac990f8`) |

Design passes with the user, committed on `main`: 15.0 hub (`2cb7828`), 17.0 weapons and fight length (`abdc04d`),
21.0 final zone (`55db32b`), the answers to 18.1's findings (`80548f2`).

After batch 5: build, all 268 GameTests and the server check green; `balanceTargets` has 17 misses, all 18.3's
(the Vampire boss's cadence and hit sizes, creeper / witch / Enderman / Vindicator / zombie, the moon-night mobs, the
dungeon Hollow).

### What is left

| Task | Depends on | Note |
|---|---|---|
| 15.5 Worlds from 0.4.x | — | old Sanctum cores keep working, old player data (DESIGN §2 "Worlds from 0.4.x") |
| 16.4 Overworld balance re-run | — | 16.2's counts: 24–42 dungeons within 1500 blocks |
| 17.2 Weapon models in hand | — | the Scourge's lash is done in 17.4 |
| 18.3 Retune overworld, dungeon, moon nights | 16.4 | closes the remaining `balanceTargets` misses |
| 19.3 Death, hit, stagger animations (3 parts) | — | overworld + moon mobs after 18.3 (same entity classes) |
| 19.4 Boss remodels (one per boss) | 19.3 for shared code | Colossus, Grieving Tree, Crypt Keeper, Smoldering Lord (face), Vampire |
| 19.5 Photo tour (optional) | — | the user starts it in their own terminal |
| 20.1 Set-piece framework, 20.2 moving set pieces | 20.1 → 20.2 | |
| 20.4 Props into structures, more variants | — | after 21.3 for the Citadel pieces |
| 21.3 Citadel in its sector, remove the depths dimension | — | 21.1 kept depths so the game stays completable |
| 21.4 Old worlds' realm dimensions regenerated | 21.3 | the coordinator's call (nothing is published); it touches the user's playtest world |
| 21.5 Release 0.5.0 | all | version, changelog, docs; tag and push wait for the user |

Proposed next batch (not started): 16.4 + 18.3 in one session, 17.2, 19.3 for the Realm 1 zone mobs, 21.3.

Open loose ends: the 21.2 session (`citadel-lock`) could not be deleted yet (a queued message); its branch is merged.
Nothing of 0.5.0 has been seen or played in game.

## Tracks — leftovers of 0.2.0 and 0.3.0 (0.4.1)

Tasks in PLAN.md "Leftovers of 0.2.0 and 0.3.0". Coordinator: session `tails-020-030` on its own branch (not the main
checkout); tracks branch from it and send MERGE REQUESTs to it, never to `main`. The coordinator sends one MERGE
REQUEST for the whole branch to the 0.4.0 coordinator, who merges it into `main`. At most 4 sessions at once,
the coordinator included.

| Track | Session name | Scope | Owns |
|---|---|---|---|
| L-A | `left-gear` (done) | L1–L2: Sanctum weapons, overworld balance pass | `com.embervoid.sanctum.gear`, `com.embervoid.balance`, `docs/balance-overworld.md`, the `sanctum_outer_hall` loot table, weapon item textures; overworld number changes (`mob`, `entity/mob`, `stats` config defaults) as small commits |
| L-B | `left-audio` (not started: done by the coordinator) | L3: sound gaps | `sounds.json`, the new per-package sound classes, subtitles, `SoundTests`; sound hooks in entity / item / block classes as small separate commits |
| L-C | `left-art` (done) | L4: art pass for the eight placeholder entities | `tools/overworld_mob_art.py`, the eight entities' `geo`, `animations`, `textures` |
| — | coordinator | L5–L9 | GameTests and JUnit for L5, `tools/server_check.py`, the shrine look (`boss`, `sanctum`, client), README, docs, version |

Overlaps: left-gear and left-audio may both touch `HollowEntity` (numbers vs. sound hooks) and the lang files — keep
each edit small and in its own commit, lang keys as one contiguous block per track.

## Tracks — 0.4.0 release

Tasks in PLAN.md "0.4.0 release tasks". At most 4 track sessions at once: release-audio starts when the last
milestone track (`track-reach-bosses`) is done.

| Track | Session name | Scope | Owns |
|---|---|---|---|
| R1 | `release-balance` (done) | R40.1a–c (+ R30.2) | `docs/balance-0.4.0.md`, `com.embervoid.balance` (calculator, tests); number changes in any package (config defaults, base attributes), each a small commit |
| R2 | `release-art` (done) | R40.2a–d | `tools/model_preview.py`, `tools/*_placeholders.py`, `assets/embervoid/geo/**`, `animations/**`, `textures/**` |
| R3 | `release-audio` (done) | R40.3a–b | `assets/embervoid/sounds.json`, `assets/embervoid/sounds/**`, `registry/ModSounds` + per-package sound classes, `CREDITS.md`; sound hooks in entity/item classes as small separate commits |
| R4 | `release-mp` (done) | R40.4a–b (+ R30.3) | multiplayer GameTests, `tools/server_check.py` perf options; fixes in the owning package as separate commits |

Overlaps: release-balance and release-audio both touch entity classes (numbers vs. sound hooks) — keep each edit
small and in its own commit; the coordinator resolves conflicts. Models keep their size, bone names and animation
names (code and hitboxes rely on them).

## Tracks — 0.4.0 (milestones 9–13, Realm 1 "Ashen Reach") — done

Tasks in PLAN.md "0.4.0"; design in DESIGN §4.5. Each track does its zone tasks first, then its layer of the
Smoldering Citadel (milestone 13).

| Track | Session name | Scope | Owns (packages / files) |
|---|---|---|---|
| A | `track-reach-world` (done) | PLAN 9.1–9.9 (13.1–13.2 moved to track-citadel): fog API, placed enemies, zones, terrain, blocks and ores, landing and roads, minor structures, crypt, world edge; depths dimension and Citadel jigsaw | `com.embervoid.realm` (incl. `realm.worldgen`), `com.embervoid.reach.world`, `com.embervoid.client.realm` / `client.reach.world`; realm `dimension*` / `worldgen/**` JSON; `structures/ashen_reach/**` except arena contents; its blocks' assets |
| B | `track-reach-mobs` (done) | PLAN 10.1–10.7, 13.3: greybox pipeline, the 10 zone mobs, then the 4 Citadel mobs | `com.embervoid.reach.mob`, `com.embervoid.client.reach.mob`, their assets and entity loot tables, `tools/mob_placeholders.py` |
| C | `track-reach-gear` (done) | PLAN 11.1–11.8: materials and boss items, Bleeding, Smoldering Plate, mob sets and uniques, weapons, consumables, chest loot and realm loot on mobs | `com.embervoid.reach.gear`, `com.embervoid.client.reach.gear`, `loot_tables/chests/ashen_reach/**`, its recipes, tags and assets |
| D | `track-reliquary` (done) | PLAN 11.9–11.13 (all merged, plus the Maiden screen's bonus points): Reliquary screen and slots, remnant and stat hooks, 8 artifacts, 6 rings, their drops | `com.embervoid.reliquary`, `com.embervoid.client.reliquary`, its tags and assets |
| E | `track-reach-bosses` (done) | PLAN 12.1–12.7, 13.4: boss framework additions, three arenas, Crypt Keeper, Ash Colossus, Grieving Tree, souls, Ash of Rematch; then the Smoldering Lord | `com.embervoid.boss`, `com.embervoid.reach.boss`, `com.embervoid.client.reach.boss`, arena contents (`structures/ashen_reach/arena_*`, the Citadel courtyard piece), boss assets and loot |
| F | `track-citadel` (done) | PLAN 13.1–13.2: depths "Burning City" dimension, Citadel jigsaw, the courtyard site for the Smoldering Lord | `com.embervoid.reach.citadel`, `client.reach.citadel`; depths `dimension*` / noise settings / biome; `structures/ashen_reach/citadel/**` |

At most **4 track sessions run at once**; a new track starts only when another is done, and a finished track's
session is deleted. Coordinator: PLAN 13.5 and the 0.4.0 release tasks. Earlier tracks (0.2.0: `track-player-core`, `track-mobs`,
`track-sanctum`, `track-realms`; 0.3.0: `track-moon`, `track-night-mobs`, `track-vampire`) are done and merged.
Packages not owned by a 0.4.0 track (`player`, `stats`, `mob`, `entity/mob`, `progress`, `sanctum`, `moon`,
`nightmob`, `vampire`): a track that needs a small hook there (e.g. Flask Ember in the flask, artifact points in
`stats`) adds it as a separate commit and names it in its merge request.

**Arena boundary:** reach-world decides where an arena sits and the ground under it (placement, height,
blending); reach-bosses builds what stands on it (the arena itself, shrine spot, arena-side chest, hazards).
**Smoldering Lord courtyard** (track-citadel, 13.1–13.2 merged): centre (0, −192), floor y 88, ArenaSite r 36, fog r 30,
template `embervoid:ashen_reach/arena_citadel` (reach-bosses builds it). Everything inside the 73×73 square around
the centre is reach-bosses', including the keep-wall face with a gate at x −2…2; the Citadel pieces stay outside it
and the keep door lines up with that gate.

## Contracts between tracks

Fixed ids for mobs, bosses, materials, chest loot tables and tags are listed in PLAN.md "0.4.0": reference them
by id before the owner merges (an unknown id must not fail datapack loading: optional tag entries, a warning
instead of an error, or a vanilla stand-in behind one constant).

| Contract | Owner | Used by | Contents |
|---|---|---|---|
| Fog API (PLAN 9.1, merged) | reach-world | reach-bosses (arena fog), reliquary (Fog Stalker ring) | `client.realm.RealmFog`: `push(source, density 0–1, rgb[, clearable])`, `clear`, `has`, `setClearing(source, factor)` (clearable layers only; arena fog pushes `clearable=false`), `reset` on disconnect; strongest wins, eased; per-zone fog is source `embervoid:zone` |
| Placed enemies (PLAN 9.2, merged: marker `embervoid:placed_enemy`, `PlacedEnemies.markerNbt`, processor `embervoid:placed_enemies`) | reach-world | reach-mobs (placed behaviour), reach-bosses (arena guards if any) | Template marker + processor spawning an entity id at worldgen, persistent, never respawns, zone-band level. A placed mob gets the command tag `embervoid.placed` (cage Thralls also `embervoid.placed.cage`), read by `reach.mob.ReachMob` (proposed by reach-mobs, 10.1 merged) |
| Items and chest stubs (PLAN 11.1, merged: `reach.gear.GearItems`, `reach.gear.GearLootTables`) | reach-gear | reach-world (ore drops, chests), reach-bosses (souls, rematch, Flask Ember), reliquary (chest injection) | Item ids and empty chest tables from PLAN.md |
| `embervoid:realm_ores` | reach-world | reliquary (Delver's Ring) | Block tag of the three realm ores |
| Mob ids | reach-mobs | reach-world (markers), reach-bosses (adds), reach-gear / reliquary (loot injection) | Entity ids from PLAN.md |
| Built APIs | — | all | `mob.MobLevels`, `boss.ArenaBoss` / `BossFight` / `Telegraph`, `progress.ProgressManager`, `realm.RealmMobLevels`, special attacks (`item.ability`), Ash Remnant (`player`), stats — see CORE.md |

`track-reach-world` sends 9.1 and 9.2 for merge first, `track-reach-gear` sends 11.1 first, as soon as each
compiles. Until then the other tracks write against a small local stub with the same signatures (or the fixed
ids) and replace it after syncing with `main`.

**Config:** one top-level section per track: reach-world keeps `realm`, reach-mobs `reachMobs`, reach-gear
`gear`, reliquary `reliquary`, reach-bosses keeps `boss` (+ per-boss keys inside it).

## Shared files

- **Per-player state:** each track keeps its own data attachment in its own package. Only player-core edits `PlayerData`.
- **Registration:** each track has its own registry/initializer class in its package and adds **one line** to `Embervoid.onInitialize` / `EmbervoidClient.onInitializeClient`. Items go through `ModItems.register(...)` so they appear in the creative tab; do not edit `ModItems`, `ModBlocks`, `ModEntities` themselves.
- **Commands:** each track registers its own `/embervoid <sub>` literals from its own class (Brigadier merges them).
- **Datagen:** each track adds its own provider class and one `pack.addProvider` line in `EmbervoidDataGenerator`.
- **Lang (`en_us.json`, `ru_ru.json`), mixin JSONs:** append your keys/entries as one contiguous block. The coordinator resolves merge conflicts there.
- **Build files:** only the coordinator edits `build.gradle` / `gradle.properties` / `fabric.mod.json`. A track who needs a change there asks the coordinator.
- **Docs:** `PLAN.md`, `ROADMAP.md`, `DESIGN.md`, `COORDINATION.md` are edited by the coordinator only. A track keeps its task breakdown and status in `plans/<track>.md` (its own file) and sends design questions to the coordinator.

## How a track works

- Never run `./gradlew clean`: it deletes the tracked datagen output `src/main/generated`.
- Work task by task, one commit per task, on the track branch. `./gradlew build` must pass on every commit; `./gradlew runDatagen` when assets or data change. Pure logic gets JUnit tests.
- **Do not launch a client:** no `runClient` and no `tools/client_check.py` (it crashes the Claude app). A track may run `./gradlew runGametest` and `python3 tools/server_check.py` (headless servers). The user does not play-test: a behaviour is checked by a GameTest or the server check, written by the track that built it.
- Open questions in DESIGN.md §10: build the recommended answer, keep it easy to change.
- To get other tracks' merged work: `git merge main` (the local `main` branch; it is not pushed, so the app's `sync_with_base_branch`, which reads `origin/main`, does not see it). Never merge another track's branch directly.
- Do not push, do not open PRs. The coordinator merges.

## Messages to the coordinator

Send with `SendMessage` to the coordinator (the session that started you). Keep working on the next task after sending, unless you are blocked.

**Check request** — the coordinator queues it, launches the game on that commit, and sends back the result:

```
CHECK REQUEST
track: <track>
commit: <full sha on the track branch>
run: client | server+client
setup: <commands or world setup, e.g. "/gamerule keepInventory false", "new superflat world">
steps: <numbered steps to do in game>
expected: <what should happen at each step>
```

**Merge request** — the coordinator builds the commit, merges it into `main`, and tells the other tracks:

```
MERGE REQUEST
track: <track>
commit: <full sha>
contains: <tasks / contracts in it>
notes: <anything another track must know: new APIs, renamed classes, lang keys>
```

**Question** — a design question or a blocker: `QUESTION` + the question and your recommended answer.

## Automated checks (before every QA batch)

Run in the main checkout (or the QA worktree) on the commit that goes to QA; a batch goes to the user only when all
pass. None of them needs a game account or a window.

- `./gradlew runGametest` — Fabric GameTests in `src/gametest` (its own mod `embervoid-gametest`, never in the
  jar) on a headless superflat server: mob levels, starting flask, shrine rest, death → Ash Remnant → recovery,
  Sanctum generation and contents. Fails the build on any failed test; report in `build/gametest/junit.xml`.
  The test world gets the mod's dimensions (realms, Vampire arena) through a gametest-only mixin
  (`gametest.mixin.TestServerDimensionsMixin`). Use the real (non-creative) player join from `MoonTests` when mobs
  must target a player, and one `batchId` per test that touches the global moon state.
  Mock players from `TestContext.createMockCreativeServerPlayerInWorld` always answer `isCreative()` true (even
  after `changeGameMode(SURVIVAL)`): code that tests must cover checks `getAbilities().creativeMode`. They have no
  connection, so `playerTick()` (equipment changes, attack cooldown, item use) runs only when a test calls it.
  `TestContext.spawnMob` clears a mob's goals; test areas are reused by later batches and entities outlive their
  test, so remove everything a test spawns. `./gradlew build` does not compile `src/gametest` (`runGametest` does).
  The test world does not generate structures or bring far realm chunks to entity tracking: place structure
  starts by hand.
  `shouldTickEntity(pos)` turns true before a chunk is loaded for `RealmFeatures.at`: wait for `isChunkLoaded` too.
  `runGametest` starts from the default config (it deletes `build/gametest/config/embervoid.json`): read tunable
  numbers from the config in tests instead of hardcoding them.
- `python3 tools/server_check.py` — a real dedicated server (`runServerCheck`, run dir `run/server-check`, fresh
  world each time): boots to Done, generates a real Sanctum, the Ashen Reach landing, the three zone arenas and the
  depths over RCON, then fails on any crash, mixin failure, ERROR line or datapack load error in the log. Client
  mixins and rendering are not covered here.
- `python3 tools/client_check.py [scenario.txt]` — a real client, headless (HeadlessMc + the hmc-specifics mod, no
  window, offline, game dir `run/hmc`): fresh world with cheats, then a scenario of hmc-specifics commands (chat
  commands, `gui` screen dumps, `click-text <button>`, `wait <s>`), chat and screens printed, client log scanned.
  Default scenario: stats, realm travel, death → death screen → Respawn → remnant. Covers client mixins, client
  init and screens as data; nothing is rendered, so looks stay with in-game QA. Setup once: `headlessmc` on PATH,
  `run/hmc/downloads/hmc-specifics-fabric-1.20.1-1.2.2.jar`; `tools/hmc.sh` runs HeadlessMc with these settings.

A batch item that these checks cover (counts, messages, no-crash) is not sent to the user; QA keeps what has to be
seen or felt in game.

## In-game checks (QA batching)

The game is launched by the QA session, never once per merge. The coordinator collects fixes and sends QA
**one batch** when no track has more fixes queued. Each batch item lists only the changed behaviour to verify
(e.g. "S3: no water in the gate hall; S2: shaft casing screenshot"), not the whole original check. One client
launch per batch.

## Coordinator

- Runs the check queue first in, first out, each in a scratch worktree at the requested commit (`.claude/worktrees/qa`), and replies with what happened (logs, screenshots, and what the user saw when the user tested).
- Merges into `main` in the order requests arrive; resolves lang/mixin conflicts; runs `./gradlew build` after each merge; tells the tracks that depend on the merged work to sync.
- Keeps `PLAN.md` / `ROADMAP.md` checkboxes current from merges.
