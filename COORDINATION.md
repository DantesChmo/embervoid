# Embervoid — Parallel work

The roadmap ([ROADMAP.md](ROADMAP.md), tasks in [PLAN.md](PLAN.md), design in [DESIGN.md](DESIGN.md)) is worked on
by track sessions at once. Each track has its own git worktree and branch. The **coordinator** session (in the
main checkout; the session that started the tracks, `startedBy` in the session list) merges tracks into `main`,
owns the docs, and is the only place where the game is launched.

## Tracks — 0.3.0 (milestone 8, moon nights)

| Track | Session name | Scope | Owns (packages / files) |
|---|---|---|---|
| A | `track-moon` | PLAN 8.1–8.6: moon-night state and `moon.api`, night hold, sleep block, fallback, red sky, full moon mob modifiers, Curse of the Night, vampirism | `com.embervoid.moon` (main), `com.embervoid.client.moon` (client) |
| B | `track-night-mobs` | PLAN 8.7–8.9: Ashen Zombie, Ghost, full moon waves | `com.embervoid.nightmob` (main), `com.embervoid.client.nightmob` (client), their assets |
| C | `track-vampire` | PLAN 8.10–8.12: Vampire NPC, Vampire boss, Vampire arena dimension | `com.embervoid.vampire` (main), `com.embervoid.client.vampire` (client), `data/embervoid/dimension*/vampire_arena*`, their assets |

All three tracks are merged into `main` (8.1–8.12). Coordinator: R30.1 done; the release tasks remain.
Milestones 4–7 tracks (`track-player-core`, `track-mobs`, `track-sanctum`, `track-realms`) are done and merged.
Packages from them (`player`, `stats`, `mob`, `entity/mob`, `progress`, `sanctum`, `realm`, `boss`) are not owned
by a 0.3.0 track: a track that needs a small hook there (e.g. a new event in `boss`) adds it as a separate commit
and names it in its merge request.

## Contracts between tracks

| Contract | Owner | Used by | Contents |
|---|---|---|---|
| `moon.api.MoonNights` | moon | night-mobs, vampire | `current(server)`; `NIGHT_STARTED` / `NIGHT_ENDED(reason)` events; `hold` / `release(server, Identifier)` |
| `moon.api.CurseOfTheNight` | moon | night-mobs | `stacks(player)` |
| Built APIs | — | all | `mob.MobLevels` (levels), `boss.ArenaBoss` / `BossFight` (boss framework), `progress.ProgressManager` (unlocked realms), `realm.RealmMobLevels` (bands), `sanctum` core check, Ash Remnant (`player`) — see CORE.md |

`track-moon` sends 8.1 (the API) for merge first, as soon as it compiles. Until then the other tracks write
against a small local stub with the same signatures and replace it after syncing with `main`.

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
- **Do not launch the game:** no `runClient`, `runServer` or `runGametest` in a track. In-game checks go through the coordinator's queue (below).
- Open questions in DESIGN.md §10: build the recommended answer, keep it easy to change.
- To get other tracks' merged work: sync the worktree with `main` (the app's `sync_with_base_branch`), never merge another track's branch directly.
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
  The test world has only the vanilla dimensions, so realm checks live in the server check.
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
