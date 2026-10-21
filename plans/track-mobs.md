# Track 2 — mobs

Scope: Milestone 4 (GeckoLib, Ash Ghoul, Hollow, Ember Wisp) and PLAN 3.5–3.7 (mob levels, difficulty factors,
level display). Branch `claude/track-mobs-*`.

| # | Task | Status |
|---|---|---|
| M1 | GeckoLib 4.8.4 as a required dependency + Ash Ghoul (model pipeline) | done |
| M2 | Bestiary details for Hollow and Ember Wisp (below), sent as a QUESTION | done |
| M3 | Hollow | todo |
| M4 | Ember Wisp | todo |
| M5 | Mob levels (3.5) + level API | todo |
| M6 | Difficulty factors (3.6) | todo |
| M7 | Level display (3.7) | todo |

## Layout

- `entity/mob` — entity classes, `MobEntities` (types, attributes, spawn eggs, spawn rules and biome spawns).
- `client/mob` — GeckoLib renderers (`MobRenderers`), datagen for entity loot tables (`MobDataGen`).
- `mob` — levels, difficulty factors, level sync.
- Assets: `geo/entity`, `animations/entity`, `textures/entity` (`<name>.png` + `<name>_glowmask.png`).
  All are placeholders written by `tools/mob_placeholders.py`; **they need an art pass** (Blockbench).

## Ash Ghoul

- 12 HP, 2.5 damage, speed 0.33 (zombie 0.23), follow range 32, undead (smite, burns in daylight), 5 XP.
- Spawns: every overworld biome with monsters (not mushroom fields or the deep dark), monster group, weight 30
  (zombie is 95), packs of 3–5, light rules like zombies (night surface or dark caves).
- Pack: when a ghoul picks a target, idle ghouls within 16 blocks take the same target.
- Death: ash cloud (area effect cloud, radius 2.5 shrinking over 4 s) giving Blindness 3 s.
- Loot: 0–2 bones, 0–1 charcoal (+Looting).
- Base level 5 (§5.4).
