# Track C — vampire (PLAN 8.10–8.12)

Branch `claude/track-vampire-6ebd07`. Rules: [COORDINATION.md](../COORDINATION.md). Design: DESIGN §3.3, §10.12.

Owns `com.embervoid.vampire` (main), `com.embervoid.client.vampire` (client), `data/embervoid/dimension*/vampire_arena*`,
Vampire assets (`tools/vampire_placeholders.py`, which reuses the helpers of `tools/mob_placeholders.py`).
Config section `vampire` (`VampireConfig`; PLAN writes the keys as `moon.blood.*`).

## Moon API

`moon.api.MoonNights` is not on `main` yet. Until it is, `vampire.moonstub` has a stand-in with the same calls
(`current`, `NIGHT_STARTED`, `NIGHT_ENDED`, `hold`, `release`) and a debug command
`/embervoid vampire stubnight start blood|full | stop | status` (the last released hold ends the night at once, as 8.2
says). After syncing with `main`: switch the imports to `moon.api`, delete `vampire.moonstub`.

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
  - In 8.10 the finished transformation releases the hold (the boss comes in 8.11).
  - Debug: `/embervoid vampire status | provoke`.
- [ ] **8.11 Vampire boss.**
- [ ] **8.12 Vampire arena dimension.**

## Notes

- Players joining the overworld later in the night get no Vampires of their own (the groups form at dusk).
