# Track C — vampire (PLAN 8.10–8.12)

Branch `claude/track-vampire-6ebd07`. Rules: [COORDINATION.md](../COORDINATION.md). Design: DESIGN §3.3, §10.12.

Owns `com.embervoid.vampire` (main), `com.embervoid.client.vampire` (client), `data/embervoid/dimension*/vampire_arena*`,
Vampire assets (`tools/vampire_placeholders.py`, which reuses the helpers of `tools/mob_placeholders.py`).
Config section `vampire` (`VampireConfig`; PLAN writes the keys as `moon.blood.*`).

## Moon API

Uses `moon.api.MoonNights` from `main`: `NIGHT_STARTED` / `NIGHT_ENDED`, `hold` / `release(server, embervoid:vampire)`
(releasing the last hold ends the night with `GOAL_MET`). Holds are saved with the world, so `resume` re-holding is
harmless. Moon's vampirism (8.6) is overworld-only and skips `ArenaBoss`, so neither the arena nor the boss is
affected. Test with `/embervoid moon start blood | stop | status`.

## Tasks

- [x] **8.10 Vampire NPC + provoking.**
  - `VampireEntity` (friendly form, `PathAwareEntity`, GeckoLib placeholder `vampire`, no level, spawn group MISC):
    wanders near where it appeared, looks at players, right-click → one of four lines. Cannot be hurt; a player hit
    (melee or projectile) provokes it. Void and `/kill` still remove it.
  - `VampireNight`: on `NIGHT_STARTED(BLOOD_NEW_MOON)` the overworld players outside a Sanctum core are grouped
    (`VampireGroups`, chained within `vampire.groupRadius` 64, JUnit-tested), each group gets
    `groupMin`–`groupMax` (1–3) Vampires 6–14 blocks from a random member, at that member's height (caves work),
    and `hold(embervoid:vampire)`. First hit → that one turns (2.5 s `misc.transform`, blood particles, sounds),
    every other Vampire vanishes in smoke. After `vampire.provokeMinutes` 5 the one nearest to a player turns (a new
    one comes to a player if every Vampire is in an unloaded chunk). Night ends otherwise → all vanish.
  - `VampireState` (overworld `PersistentState`): night counter, stage, timer, the turning Vampire. Vampires store
    their night id; one that does not belong to the current stage vanishes on its next tick (unloaded chunks,
    restarts).
  - Debug: `/embervoid vampire status | provoke`.
- [x] **8.11 Vampire boss.**
  - `VampireBossEntity` (`HostileEntity`, GeckoLib placeholder `vampire_boss`, rendered ×1.2): implements `ArenaBoss`
    (phases 60 % / 30 %, `Telegraph` animations) but does not call `ArenaManager.tickBoss` (it has no arena) — no
    hook in `boss` was needed. Base 220 HP, 7 melee, armor 4 (scaled by its level). Moves: claws; Blood Leap
    (1 s crouch, rings mark the landing, 9 in r 3.5); Blood Nova from 60 % (1.5 s shrinking ring r 6, 10 damage, heals
    25 % of what it dealt); Mist Step from 30 % (vanishes in smoke, strikes from behind its target), faster.
    No fall damage. 200 XP before level scaling, like the test boss.
  - `VampireFight` (one per server, state in `VampireState`): the boss forms where the Vampire turned. Players within
    48 blocks then take part; any living player who comes within 48 later joins, except those who died in it.
    Level `VampireLevel.of` (JUnit): highest participant level, ≥ `vampire.bossMinLevel` 15, ≤ top band of the highest
    unlocked realm (Ashen Reach: 30) or `vampire.bossCapNoRealm` 20. Shared bar "Vampire — Lv N", crimson forest
    music. Kill → personal loot into each credited fighter's inventory: first kill (progress id `embervoid:vampire`)
    `firstKillShards` 2 Flask Shards + Vampire Fang (`embervoid:vampire_fang`), repeat `repeatShardChance` 50 % one
    shard; hold released. Every fighter dead, or nobody near for 30 s → the Vampire leaves, hold released. Night
    ended another way → boss leaves, no credit. Restart mid-fight: boss stays (persistent), bar rebuilt.
  - `/embervoid vampire status` shows the fight.
- [x] **8.12 Vampire arena dimension.**
  - Dimension `embervoid:vampire_arena` (`dimension/`, `dimension_type/vampire_arena.json`, biome
    `worldgen/biome/vampire_arena.json`): flat generator with no layers (void), Nether sky effects with a dark-red
    biome fog (the "red sky"), fixed midnight, no skylight, ambient light 0.25, crimson spores and crimson-forest
    ambience, no mob spawns, beds and anchors off.
  - `VampireArena`: arena built in code at 0,64,0 on every opening (so it resets each blood moon): blackstone disc
    r 22 over the void, red nether brick rings, low wall rim, eight pillars topped with shroomlight; chunks
    force-loaded while open. No breaking/placing outside creative.
  - When the boss forms, every overworld player outside a Sanctum core is pulled in (arrival ring r 13 facing the
    centre), return point saved in `VampireState`; the boss forms at the centre and its level comes from them.
  - A fighter who dies respawns as usual and cannot get back (nothing leads there). Everyone pulled in dead → the
    Vampire leaves, the arena closes, the hold is released → the night ends.
  - Kill → credit and loot as in 8.11, "the domain fades in 10 s", then everyone inside goes back to their return
    point. On closing, a remnant left in the arena moves to its owner's return point (owner told the coordinates);
    items lying within 6 blocks of where a fighter died (remnant mode `xp`/`off`) follow that fighter; every other
    entity is removed.
  - A player who logged out inside is sent back on their next login once the arena has closed (their return point
    is kept for that). Restart while closing → closes at once.
  - Without the arena dimension (broken datapack) the fight stays in the overworld as in 8.11 (logged as an error).

## Notes

- No GameTest yet: tracks don't run `runGametest`. Good candidates for 8.13: `VampireArena.open/close` round trip
  (return point, remnant moved), level cap with an attuned player.
- Server check: `execute in embervoid:vampire_arena run forceload add -16 -16 16 16` would prove the dimension loads.

- Players joining the overworld later in the night get no Vampires of their own (the groups form at dusk).
