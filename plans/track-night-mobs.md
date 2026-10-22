# Track B — night mobs (0.3.0)

Scope: PLAN 8.7–8.9 (Ashen Zombie, Ghost, full moon waves). Branch `claude/track-night-mobs-*`.

| # | Task | Status |
|---|---|---|
| 8.7 | Ashen Zombie | done, merge requested |
| 8.8 | Ghost | done, merge requested |
| 8.9 | Full moon waves | done on the real `moon.api`, merge requested |

## Layout

- `nightmob` — entity classes, `NightMobs` (types, attributes, spawn eggs), `NightMobConfig` (`nightMobs` config
  section).
- `client/nightmob` — renderers (`NightMobClient`), loot tables (`NightMobDataGen`).
- Assets: placeholders from `tools/nightmob_placeholders.py` (uses the helpers of `tools/mob_placeholders.py`);
  **they need an art pass**.

## Config

PLAN names the keys `moon.fullMoon.*`, but a config file holds one section per top-level name and `moon` belongs to
track-moon, so this track has its own section `nightMobs` (approved by the coordinator). Keys:

- `zombieLevelBonus` 5 — Ashen Zombie of a wave: spot level + this.
- `ghostMissChance` 0.15 — share of attacks on a Ghost that pass through it.
- `maxZombies` 10, `ghostCount` 5 — wave size.
- `spawnMinDistance` 16, `spawnMaxDistance` 32 — ring around the player where a wave spawns.

## Ashen Zombie (8.7)

- 24 HP, 4 damage, armor 2, speed 0.25 (zombie 0.23), knockback resistance 0.3, follow range 40 (a wave spawns
  16–32 blocks away), undead. Does not burn in daylight. 20 XP before level scaling (§10.12).
- No natural spawns; spawn egg for testing. Without a wave it gets the default level rules (`defaultBaseLevel`,
  no `baseLevels` entry), the wave overrides it with `MobLevels.setLevel`.
- Loot: 0–2 rotten flesh, 0–1 charcoal (+Looting).

## Ghost (8.8)

- 48 HP, 8 damage (2× Ashen Zombie), follow range 40, undead, 40 XP before level scaling (§10.12).
- Flies like a vex: no gravity, no block collision (`noClip` during its own tick), its own move control at speed
  factor 0.4 (about 4.4 blocks/s; a vex is 1.0). Targets players through walls (no line of sight needed). Chases
  the target's body and strikes in reach once a second; without a target it drifts 1–4 blocks above the ground.
- Immune to suffocation, drowning and falls.
- Miss: an attack from an entity (melee or projectile; not `/kill`-type damage) that would count (outside the hurt
  cooldown) passes through at `ghostMissChance` — no damage, a poof and a dull "no damage" sound; an arrow bounces
  off.
- Translucent placeholder, lit at least to block light 8, glowing eyes. Loot: 0–1 phantom membrane (+Looting).
- Known: a ghost inside a wall can hit you while your crosshair hits the wall (like a vex). It comes out to chase.

## Full moon waves (8.9)

- Built first on a local stub of `moon.api`, replaced by the real API after syncing with main.
- `NightWaves` listens to `MoonNights.NIGHT_STARTED` / `NIGHT_ENDED` (FULL_MOON only). `WaveState` (overworld
  `PersistentState` `embervoid_night_waves`): full moon night counter, living wave mobs per owner, who was served
  this night, Flask Shards owed to offline owners.
- Every second while the full moon is on, each overworld player (not spectators) who has not had a wave this
  night gets one: `WaveRules.waveFor(CurseOfTheNight.stacks, maxZombies, ghostCount)`. This one check covers the
  night's start, joining, returning from another dimension and respawning.
- Placement: zombies on safe ground (vanilla on-ground spawn rules, empty box, loaded chunk) in the ring, within 8
  blocks of the player's height (so cave players get cave zombies); then the ring 6–16; a mob that still finds no
  spot is dropped with a warning in the log. Ghosts anywhere in the ring 2–5 blocks above the player's feet,
  open air preferred. Wave mobs are persistent, cannot use portals, and start with the player as their target.
- Level: `MobLevels.defaultLevel` at the spot + `zombieLevelBonus`, for Ghosts too (so they stay 2× a zombie);
  set after spawning, so it replaces the full moon's +3 from 8.4. They still get 8.4's follow range and target keeping.
- A wave mob carries a persistent attachment `embervoid:wave_night` (the night counter). Killed or discarded
  (removal reason that destroys) → leaves its wave; the last one clears the wave: chat line, hold
  `embervoid:wave/<uuid>` released, and for a Ghost wave whose last mob was killed a Flask Shard (inventory or
  dropped; later if the owner is offline).
- `NIGHT_ENDED`: waves forgotten, loaded wave mobs vanish with smoke; unloaded ones vanish when their chunk loads
  (attachment from another night, or not in any wave). After a restart with no full moon on, leftover waves are
  cleared the same way; with one on, the holds are re-opened.
- Debug (op): `/embervoid wave status`, `/embervoid wave spawn <players>` (a wave now, full moon or not; it holds
  the night only when one is on), `/embervoid wave clear <players>`.
- Known: a wave owner who logs off leaves the night held until their wave dies or the 30-minute fallback.
