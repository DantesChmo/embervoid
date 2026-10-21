# Track 3 — Ember Sanctum (Milestone 5)

Design: [DESIGN.md](../DESIGN.md) §1, §2, §4.2. Rules: [COORDINATION.md](../COORDINATION.md).
Owns `com.embervoid.progress`, `com.embervoid.sanctum`, `registry/ModStructures`,
`data/embervoid/worldgen/**` (Sanctum), `data/embervoid/structures/sanctum/**`.

| ID | Task | Depends on | Status |
|----|------|------------|--------|
| S1 | **`progress` API** (contract for realms) | — | done, merge requested |
| S2 | **Ember Shrine** block | S1, flask API (player-core) | done |
| S3 | **Void Obelisks** (dark / lit / kindled per player) | S1 | done |
| S4 | **Sanctum structure** (generated templates, jigsaw, structure_set) | M1 blocks | done, needs in-game check |
| S5 | **Core protection**: no hostile spawns, unbreakable core blocks | S2, S3, S4 | done (flagstone floor is spawn-proof; core blocks unbreakable) |
| S6 | **Attunement** on first reaching a core | S1, S4 | done |
| S7 | **Sanctum Map** in vanilla chest loot; **outer-hall loot** | S4 | done, needs in-game check |
| S8 | **Ashen Maiden** NPC (GeckoLib) | mobs: GeckoLib on `main` | blocked |
| S9 | **Level-up screen** (C2S, one point per click) | player-core: `levelUp`, stat read API, `XpUtil` | blocked |

## Tasks

- **S1 `progress` API.** `com.embervoid.progress.PlayerProgress` (record-like, codec + packet) and
  `ProgressManager` (own Fabric data attachment `embervoid:progress`, persistent, `copyOnDeath`, S2C sync on join,
  respawn, dimension change and every change). Server API: `isAttuned`, `attune`, `isRealmUnlocked`, `unlockRealm`,
  `isRealmCompleted`, `completeRealm`, `hasBeatenBoss`, `markBossBeaten`. Realms and bosses are `Identifier`s.
  Realm ids are owned by the realms track; `ProgressManager.REALM_1` = `embervoid:realm_1` is the default
  first realm, unlocked for every player once attuned. Debug: `/embervoid progress [attune|unlock|complete|boss|reset]`.
  *Done when:* values survive relog and death, the client copy updates, JUnit covers the codec and packet round trip.
- **S2 Ember Shrine.** `sanctum.EmberShrineBlock`: right-click → rest: refills all flasks (through
  `HealingFlaskItem.refill`, player-core's public API), restores nothing else, short cooldown, particles + sound.
  `ShrineRestCallback` event so the realms track can hook "set realm respawn" in milestone 6.
- **S3 Void Obelisks.** `sanctum.VoidObeliskBlock` + block entity storing the realm id it leads to. The block state
  is neutral; the client renders dark / lit / kindled from the local player's synced progress (block entity renderer
  draws the flame/ glow overlay). Right-click: dark → "the flame has not reached it"; lit → realms hook
  (`ObeliskActivateCallback`); with no handler → "the way is still forming". Unbreakable.
- **S4 Sanctum structure.** Templates are generated from Java (`sanctum.gen.SanctumTemplates`, run by the datagen
  entrypoint) into `data/embervoid/structures/sanctum/*.nbt` using M1 blocks: surface ruin, descent stairwell,
  outer hall pieces (corridor, hall with chests, guard room with spawners as placeholder guards), core room. Jigsaw
  `structure`, `template_pool`s, `structure_set` (spacing 48 / separation 16), biome tag `has_structure/ember_sanctum`.
- **S5 Core protection.** Core room marked by a separate structure piece; hostile natural spawns blocked inside a core
  bounding box (spawn predicate via mixin/event); core blocks (shrine, obelisks, core floor block
  `sanctum_core_stone`) have hardness -1.
- **S6 Attunement.** Server tick check: a player inside a core piece's bounds (from the structure start) who is not
  attuned gets attuned + realm 1 unlocked, with a title message and sound.
- **S7 Loot.** `embervoid:sanctum_map` explorer map (via loot function `exploration_map` with destination tag
  `embervoid:on_sanctum_maps`) added to village, ruined portal and shipwreck chest loot through `LootTableEvents.MODIFY`.
  Outer-hall chest loot table `embervoid:chests/sanctum_outer_hall`: vanilla loot + flask shards (item `flask_shard`
  as placeholder upgrade material; 3 shards → +1 flask charge, via the flask API once player-core has upgrades).
- **S8 / S9** after the contracts land on `main`.

## Status log

- started; plan written.
