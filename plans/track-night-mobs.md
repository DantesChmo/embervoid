# Track B — night mobs (0.3.0)

Scope: PLAN 8.7–8.9 (Ashen Zombie, Ghost, full moon waves). Branch `claude/track-night-mobs-*`.

| # | Task | Status |
|---|---|---|
| 8.7 | Ashen Zombie | done, merge requested |
| 8.8 | Ghost | done, merge requested |
| 8.9 | Full moon waves | todo |

## Layout

- `nightmob` — entity classes, `NightMobs` (types, attributes, spawn eggs), `NightMobConfig` (`nightMobs` config
  section).
- `client/nightmob` — renderers (`NightMobClient`), loot tables (`NightMobDataGen`).
- Assets: placeholders from `tools/nightmob_placeholders.py` (uses the helpers of `tools/mob_placeholders.py`);
  **they need an art pass**.

## Config

PLAN names the keys `moon.fullMoon.*`, but a config file holds one section per top-level name and `moon` belongs to
track-moon, so this track has its own section `nightMobs` (sent to the coordinator as a QUESTION). Keys:

- `zombieLevelBonus` 5 — Ashen Zombie of a wave: spot level + this.
- `ghostMissChance` 0.15 — share of attacks on a Ghost that pass through it.

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
