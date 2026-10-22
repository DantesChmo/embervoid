# release-mp — multiplayer and performance (0.4.0)

Tasks: PLAN.md R40.4a (multiplayer GameTests), R40.4b (performance on a dedicated server); they close R30.3.

## R40.4a — multiplayer GameTests (`src/gametest/.../MultiplayerTests.java`)

Real players joined through the player manager (no mock players), 2–3 per test, each test its own batch.

| Test | What it proves |
|---|---|
| `progressAndTheGateArePerPlayer` | A beats the three zone bosses alone, then helps B with one: beaten bosses, Realm 1 obelisk glow, Flask Shards and souls stay per player; progress syncs only reach their owner; the Sealed Gate takes A to the depths and stays sealed for B (2 left) and C (never attuned, 3 left) |
| `zoneBossForTwoWithAnOutsider` | Real Necropolis arena, summon rule: a veteran alone walks through it; a newcomer stepping in with the veteran summons the Crypt Keeper for both (HP ×1.5 against a lone fighter's boss, one Restless Bones); an outsider is pushed out at the wall and from inside (pearl/teleport) and never becomes a fighter; the kill credits both fighters, each with a soul in their own inventory (none on the ground), the shard only for the newcomer, nothing for the outsider; afterwards nobody who beat it summons it again |
| `rematchPrimedByOneForThree` | One veteran primes Ash of Rematch, three step in (two veterans, one newcomer): HP ×2, two adds; veterans get no soul (rematch), the newcomer gets their first-kill soul and shard |
| `realmDeathsKeepToTheirOwners` | Two players rested at different shrines in the realm die on the same spot: one remnant each, the other player cannot take from it; each respawns at their own shrine; each gets their own items and XP back |
| `syncGoesToItsOwnerOnly` | Reliquary, stats, flask and progress changes of one player send no mod packet to another player next to them |
| `moonWavesArePerPlayer` | Full moon with a cursed (2 stacks) and an uncursed player: 2 Ashen Zombies after the first, 5 Ghosts after the second, each wave holding the night; clearing one leaves the night on, the second ends it; only the Ghost wave gives a Flask Shard |

Bugs found:
- **Realm respawn (realm package):** during `AFTER_RESPAWN` the network handler still holds the dead player (vanilla
  hands the connection to the new player only after `respawnPlayer` returns), so `RealmTeleports.teleport` moved the
  dead entity; the new player sat at the overworld spawn's coordinates inside the realm until the client confirmed
  the teleport. Fixed by also placing the new player itself. `TestPlayers.respawn` now hands the connection over
  after the respawn, as vanilla does.

## R40.4b — performance (`tools/server_check.py --perf [--profile]`, `/embervoid perf`)

Results and method in `docs/perf-0.4.0.md`. In short: every area, with and without a player, and every boss fight
runs at 4–9 ms mean tick time; the realm generates faster than vanilla terrain; the mod's code is under 0.5 % of the
profile samples. No hot spot needed a fix.

- `--perf`: phase 1 generates each area alone (no players), phase 2 sends a headless bot player to each, phase 3 loads
  everything with three bots, phase 4 starts each boss fight with a bot. `--profile` adds a JFR recording and the
  mod's busiest methods.
- The bots (`Bot`, protocol 763, no compression) must report their position: the server moves a player's chunk
  tickets only on a movement packet, not on a teleport. Without that, phase 2 waited on chunks that never ticked.

## Decisions (small, made here)

- A newcomer in a rematch gets their first-kill reward (soul + shard): it is their first kill, and they could have
  summoned the boss themselves anyway. Veterans in a rematch get no soul, as built.
- `/embervoid perf ticks|chunks|entities` is an operator debug command in `com.embervoid.command.PerfCommands`
  (ships in the jar like the other debug commands); the perf mode needs numbers vanilla RCON cannot give (tick time).
- `server_check.py` deletes `run/server-check/config/embervoid.json` with the world, so a fresh run always uses the
  current defaults (an old file kept `realm.depthsOpen=false` from 0.3.0).
- Tests that depend on a config value set it (`BossArena` JSON) instead of trusting `build/gametest/config`, which
  survives between runs (here it still had 0.3.0's `depthsOpen=false`).
