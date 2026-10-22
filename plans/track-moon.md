# Track A — moon (PLAN 8.1–8.6)

Branch `claude/track-moon-741b09`. Rules: [COORDINATION.md](../COORDINATION.md). Design: DESIGN §3.3, §10.12.
Packages owned: `com.embervoid.moon` (main), `com.embervoid.client.moon` (client). Config section `moon`.

## Architecture

| Piece | How |
|---|---|
| Schedule | `MoonSchedule` (pure, JUnit): day = `floor(timeOfDay / 24000)`, phase = day % 8, cycle = day / 8; events in cycles ≥ 1 with `cycle % moon.cycleInterval == 0`. Night window 13000–23000 of the day |
| State | `MoonNightState` (`PersistentState` on the overworld, `embervoid_moon`): night, its day, elapsed ticks, holds, last ended day (a stopped night does not restart) |
| State machine | `MoonNightManager` on `END_SERVER_TICK`: starts a scheduled night at dusk; ends at dawn when no hold is open (`DAWN`), on the last release (`GOAL_MET`), after `moon.fallbackMinutes` of server ticks (`TIMEOUT`), or by command (`COMMAND`) |
| API | `moon.api.MoonNights` (`current`, `NIGHT_STARTED`, `NIGHT_ENDED`, `hold`, `release`, `holds`), `moon.api.MoonNight` enum, `moon.api.CurseOfTheNight.stacks` |
| Config | `MoonConfig`: `enabled`, `cycleInterval` 3, `fallbackMinutes` 30, `fullMoon.{levelBonus 3, followRange 1.5}`, `blood.vampirism` 0.25 (night-mobs and vampire have their own `nightMobs` / `vampire` sections) |
| Sky sync | `MoonSync` S2C `embervoid:moon_night` (night ordinal) on join, respawn, dimension change, start/end. Client `client.moon.MoonClient` fades a tint 0→1 over 100 ticks on the blood new moon in the overworld; mixins `MoonSkyColorMixin` (`ClientWorld.getSkyColor`), `MoonFogColorMixin` (`BackgroundRenderer.render` before `clearColor`, not under water/lava), `MoonRenderMixin` (shader colour before the moon texture in `WorldRenderer.renderSky`) |
| Full moon mobs | `FullMoonMobs`: `MobLevels` provider (overworld, full moon: default level + `levelBonus`, kept after the night); transient follow-range modifier (×`followRange`, MULTIPLY_TOTAL) on load and on all loaded overworld hostiles at start, removed at the end; `TrackTargetGoalMixin` keeps a player target within follow range when the goal would drop it (only while `mob.getTarget()` is still set, so deliberate clears like the Ember Wisp's flee stay) |
| Curse | `CurseManager`: attachment `curse_of_the_night` {stacks, epoch} (persistent, copied on death); `AFTER_DEATH` of a player whose `source.getAttacker()` is a `Monster` or `ArenaBoss` (not a player) adds a stack + chat line. Stacks count at any time and reset when a full moon ends: the world's `curseEpoch` (in `MoonNightState`) goes up, older stacks read as 0 (works for offline players) |
| Vampirism | `PlayerEntityVampirismMixin` measures health + absorption lost in `PlayerEntity.applyDamage`; `Vampirism` heals the attacker (`source.getAttacker()`, so projectile owners too) by `blood.vampirism` × that, if it is an overworld `Monster`, not an `ArenaBoss`, on the blood new moon; red dust particles |
| Debug | `/embervoid moon status | start full | start blood | stop | setcycle <n> | curse <player> <stacks>` |

Decisions made here (small, easy to change):
- `hold` outside a moon night returns false and holds nothing; holds are cleared when a night ends.
- Holds are saved with the world, so a held night stays held across a restart (owners can reconcile via `holds`).
- Held night: the overworld clock is set back to 22998 of the night's day at the end of every tick, so vanilla's
  advance during the next tick reaches 22999, never dawn (clients may see up to a second of drift between time
  syncs). Release of the last hold moves the clock to dawn (23000).
- Beds refuse from 12000 on the day of a scheduled night (vanilla allows lying down at ~12542, before our dusk at
  13000) and during any moon night; Fabric asks after vanilla's checks, so the respawn point is still set. Sleepers are woken when a night starts.
- Announcements: chat line to every player, title only to overworld players; end title "Dawn" for any reason.
- `start` during the day moves the clock to dusk of the same day; `setcycle n` sets the clock to 1000 on day 8n.

## Tasks

- [x] 8.1 Moon-night state + `moon.api` (schedule JUnit-tested)
- [x] 8.2 Night hold (time freeze), dawn on release, sleep block, fallback, announcements
- [x] 8.3 Blood-red sky sync
- [x] 8.4 Full moon mob modifiers
- [x] 8.5 Curse of the Night
- [x] 8.6 Vampirism

## GameTests (`src/gametest/.../MoonTests.java`, one batch per test)

All pass with `./gradlew runGametest` (15/15 with the other tests):
schedule (day 16 none, day 24 full moon, day 28 blood new moon at dusk); hold stops the clock before dawn even when
pushed past it, release → GOAL_MET at dawn; bed refused on a moon night with the spawn point set; full moon zombie
+3 levels and ×1.5 follow range, range reverted at the end; curse +1 on a zombie kill (kept through respawn), +0 for
/kill, fall and a player kill, 0 after the full moon; vampirism heals 25 % of the damage on the blood moon only;
`embervoid:moon_night` sent on start and end, and on join, respawn, Nether and back; a full moon zombie keeps a
player 30 blocks away behind a wall for 100 ticks while an ordinary-night zombie drops it (fails with the
`TrackTargetGoalMixin` switched off). Found and fixed: the held clock reached dawn for part of every tick.

