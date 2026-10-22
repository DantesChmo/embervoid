# Embervoid — Parallel work

The roadmap ([ROADMAP.md](ROADMAP.md), tasks in [PLAN.md](PLAN.md), design in [DESIGN.md](DESIGN.md)) is worked on
by track sessions at once. Each track has its own git worktree and branch. The **coordinator** session (in the
main checkout; the session that started the tracks, `startedBy` in the session list) merges tracks into `main`,
owns the docs, and is the only place where the game is launched.

## Tracks — 0.4.0 release

Tasks in PLAN.md "0.4.0 release tasks". At most 4 track sessions at once: release-audio starts when the last
milestone track (`track-reach-bosses`) is done.

| Track | Session name | Scope | Owns |
|---|---|---|---|
| R1 | `release-balance` | R40.1a–c (+ R30.2) | `docs/balance-0.4.0.md`, `com.embervoid.balance` (calculator, tests); number changes in any package (config defaults, base attributes), each a small commit |
| R2 | `release-art` | R40.2a–d | `tools/model_preview.py`, `tools/*_placeholders.py`, `assets/embervoid/geo/**`, `animations/**`, `textures/**` |
| R3 | `release-audio` | R40.3a–b | `assets/embervoid/sounds.json`, `assets/embervoid/sounds/**`, `registry/ModSounds` + per-package sound classes, `CREDITS.md`; sound hooks in entity/item classes as small separate commits |
| R4 | `release-mp` | R40.4a–b (+ R30.3) | multiplayer GameTests, `tools/server_check.py` perf options; fixes in the owning package as separate commits |

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
