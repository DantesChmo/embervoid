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
| Config | `MoonConfig`: `enabled`, `cycleInterval` 3, `fallbackMinutes` 30, `fullMoon.{levelBonus 3, followRange 1.5, zombieLevelBonus 5, ghostMissChance 0.15}`, `blood.{vampirism 0.25, provokeMinutes 5}` — keys of all three tracks |
| Debug | `/embervoid moon status | start full | start blood | stop | setcycle <n>` |

Decisions made here (small, easy to change):
- `hold` outside a moon night returns false and holds nothing; holds are cleared when a night ends.
- Holds are saved with the world, so a held night stays held across a restart (owners can reconcile via `holds`).
- `start` during the day moves the clock to dusk of the same day; `setcycle n` sets the clock to 1000 on day 8n.

## Tasks

- [x] 8.1 Moon-night state + `moon.api` (schedule JUnit-tested)
- [ ] 8.2 Night hold (time freeze), dawn on release, sleep block, fallback, announcements
- [ ] 8.3 Blood-red sky sync
- [ ] 8.4 Full moon mob modifiers
- [ ] 8.5 Curse of the Night
- [ ] 8.6 Vampirism
