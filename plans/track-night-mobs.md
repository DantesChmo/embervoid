# Track B — night mobs (0.3.0)

Scope: PLAN 8.7–8.9 (Ashen Zombie, Ghost, full moon waves). Branch `claude/track-night-mobs-*`.

| # | Task | Status |
|---|---|---|
| 8.7 | Ashen Zombie | done, merge requested |
| 8.8 | Ghost | todo |
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

## Ashen Zombie (8.7)

- 24 HP, 4 damage, armor 2, speed 0.25 (zombie 0.23), knockback resistance 0.3, follow range 40 (a wave spawns
  16–32 blocks away), undead. Does not burn in daylight. 20 XP before level scaling (§10.12).
- No natural spawns; spawn egg for testing. Without a wave it gets the default level rules (`defaultBaseLevel`,
  no `baseLevels` entry), the wave overrides it with `MobLevels.setLevel`.
- Loot: 0–2 rotten flesh, 0–1 charcoal (+Looting).
