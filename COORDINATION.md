# Embervoid — Parallel work

The roadmap ([ROADMAP.md](ROADMAP.md), tasks in [PLAN.md](PLAN.md), design in [DESIGN.md](DESIGN.md)) is worked on
by four track sessions at once. Each track has its own git worktree and branch. The **coordinator** session
(`local_d941bf3c-26c0-4e51-b295-fe0ebfed6100`, in the main checkout) merges tracks into `main`, owns the docs,
and is the only place where the game is launched.

## Tracks

| Track | Session name | Scope | Owns (packages / files) |
|---|---|---|---|
| 1 | `track-player-core` | Bring Milestone 2 (already on `main`) in line with the current PLAN.md; then Milestone 3 tasks 3.1–3.4 (stats, stat effects, weapon scaling, leveling + commands) | `config`, `player`, `stats`, `item/ability`, `item/HealingFlaskItem`, `command/ModCommands`, client stamina/HUD |
| 2 | `track-mobs` | Milestone 3 tasks 3.5–3.7 (mob levels, difficulty factors, level display); Milestone 4 (GeckoLib, Ash Ghoul, Hollow, Ember Wisp) | `mob` (levels, difficulty), `entity/mob`, `client/mob`; GeckoLib in `build.gradle`, `gradle.properties`, `fabric.mod.json` |
| 3 | `track-sanctum` | Milestone 5 (Ember Sanctum): progression data, Ember Shrine, Void Obelisks, structure, core protection, attunement, Sanctum Map, loot; Ashen Maiden and level-up screen last | `progress`, `sanctum`, `registry/ModStructures`, `data/embervoid/worldgen/**` for the Sanctum, `data/embervoid/structures/sanctum/**` |
| 4 | `track-realms` | Milestone 6 (realm framework) and Milestone 7 (boss framework), on placeholders | `realm`, `boss`, `data/embervoid/dimension*/**`, realm worldgen JSON |

Not in any track yet: 0.2.0 release tasks, milestones 8–10 (need the weapons and Realm 1 content docs).

## Contracts between tracks

Built early by the owning track and sent for merge as soon as they compile, so the others can build on them:

| Contract | Owner | Used by | Contents |
|---|---|---|---|
| Config sections | player-core | all | A way for a track to add its own section (`mobs.*`, `sanctum.*`, `realm.*`) without editing `EmbervoidConfig`'s body |
| `XpUtil` | player-core | sanctum (level-up screen) | XP points ↔ level + progress (PLAN 2.3) |
| `levelUp(player, stat)` + stat read API | player-core | sanctum (Maiden screen) | Server-side leveling, stat values and derived values for the preview |
| `progress` API | sanctum | realms | Per-player attuned, unlocked realms, completed realms, beaten bosses; own data attachment |
| GeckoLib dependency | mobs | sanctum (Maiden), realms (bosses later) | Dependency in the build + one working model pipeline |
| Mob level API | mobs | realms (zone level bands) | Set or override an entity's level |
| Hollow entity | mobs | sanctum (outer-hall guards) | Until merged, the Sanctum uses a vanilla placeholder |

A track that needs a contract before it lands writes against a small local stub and replaces it after the
merge. It does not edit the owner's packages.

## Shared files

- **Per-player state:** each track keeps its own data attachment in its own package. Only player-core edits `PlayerData`.
- **Registration:** each track has its own registry/initializer class in its package and adds **one line** to `Embervoid.onInitialize` / `EmbervoidClient.onInitializeClient`. Items go through `ModItems.register(...)` so they appear in the creative tab; do not edit `ModItems`, `ModBlocks`, `ModEntities` themselves.
- **Commands:** each track registers its own `/embervoid <sub>` literals from its own class (Brigadier merges them).
- **Datagen:** each track adds its own provider class and one `pack.addProvider` line in `EmbervoidDataGenerator`.
- **Lang (`en_us.json`, `ru_ru.json`), mixin JSONs:** append your keys/entries as one contiguous block. The coordinator resolves merge conflicts there.
- **Build files:** only track-mobs edits `build.gradle` / `gradle.properties` / `fabric.mod.json` (for GeckoLib). Anyone else who needs a change there asks the coordinator.
- **Docs:** `PLAN.md`, `ROADMAP.md`, `DESIGN.md`, `COORDINATION.md` are edited by the coordinator only. A track keeps its task breakdown and status in `plans/<track>.md` (its own file) and sends design questions to the coordinator.

## How a track works

- Work task by task, one commit per task, on the track branch. `./gradlew build` must pass on every commit; `./gradlew runDatagen` when assets or data change. Pure logic gets JUnit tests.
- **Do not launch the game:** no `runClient`, `runServer` or `runGametest` in a track. In-game checks go through the coordinator's queue (below).
- Open questions in DESIGN.md §10: build the recommended answer, keep it easy to change.
- To get other tracks' merged work: sync the worktree with `main` (the app's `sync_with_base_branch`), never merge another track's branch directly.
- Do not push, do not open PRs. The coordinator merges.

## Messages to the coordinator

Send with `SendMessage` to `local_d941bf3c-26c0-4e51-b295-fe0ebfed6100`. Keep working on the next task after sending, unless you are blocked.

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

## Coordinator

- Runs the check queue first in, first out, each in a scratch worktree at the requested commit (`.claude/worktrees/qa`), and replies with what happened (logs, screenshots, and what the user saw when the user tested).
- Merges into `main` in the order requests arrive; resolves lang/mixin conflicts; runs `./gradlew build` after each merge; tells the tracks that depend on the merged work to sync.
- Keeps `PLAN.md` / `ROADMAP.md` checkboxes current from merges.
