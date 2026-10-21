# Track 2 — mobs

Scope: Milestone 4 (GeckoLib, Ash Ghoul, Hollow, Ember Wisp) and PLAN 3.5–3.7 (mob levels, difficulty factors,
level display). Branch `claude/track-mobs-*`.

| # | Task | Status |
|---|---|---|
| M1 | GeckoLib 4.8.4 as a required dependency + Ash Ghoul (model pipeline) | done |
| M2 | Bestiary details for Hollow and Ember Wisp (below), sent as a QUESTION | decided (DESIGN §3.2), Hollow base attack 0 |
| M3 | Hollow | done |
| M4 | Ember Wisp | done |
| M5 | Mob levels (3.5) + level API | done |
| M6 | Difficulty factors (3.6) | done |
| M7 | Level display (3.7) | done |

## Layout

- `entity/mob` — entity classes, `MobEntities` (types, attributes, spawn eggs, spawn rules and biome spawns).
- `client/mob` — GeckoLib renderers (`MobRenderers`), datagen for entity loot tables (`MobDataGen`).
- `mob` — levels, difficulty factors, level sync, `mobs` config section, `/embervoid moblevel`.
- Assets: `geo/entity`, `animations/entity`, `textures/entity` (`<name>.png` + `<name>_glowmask.png`).
  All are placeholders written by `tools/mob_placeholders.py`; **they need an art pass** (Blockbench).

## Ash Ghoul

- 12 HP, 2.5 damage, speed 0.33 (zombie 0.23), follow range 32, undead (smite, burns in daylight), 5 XP.
- Spawns: every overworld biome with monsters (not mushroom fields or the deep dark), monster group, weight 12
  (zombie is 95; ~4 ghouls per pick, so about the population weight 30 gave with scattered groups), light rules like zombies (night surface or dark caves). Packs of 3–5: the spawn entry is a
  group of 1 and a natural spawn brings 2–4 more ghouls within 3 blocks (vanilla group spawning scatters groups).
- Pack: when a ghoul picks a target, idle ghouls within 16 blocks take the same target.
- Death: ash cloud (area effect cloud, radius 2.5 shrinking over 4 s) giving Blindness 3 s.
- Loot: 0–2 bones, 0–1 charcoal (+Looting).
- Base level 5 (§5.4).

## Bestiary proposal: Hollow and Ember Wisp

Sent to the coordinator as a QUESTION for the DESIGN.md bestiary; built as written here meanwhile. Numbers are
level-1 base values; levels scale HP, damage and XP (§5.4).

### Hollow (base level 15)

- **Role:** armed undead swordsman. Slow, hard-hitting, telegraphed, blocks. A "mini-knight" fight; rich XP.
- **Stats:** 30 HP, armor 6, knockback resistance 0.6, speed 0.2 (zombie 0.23), follow range 24, base attack 0:
  all damage comes from the weapon (iron sword 70 % → 5, stone sword 30 % → 4; decided). Weapon drops like
  vanilla gear (8.5 %). Balance target for later: ~6 HP per hit through full iron on Normal.
- **Attack:** wind-up of 8 ticks (overhead swing animation) before the hit lands, so it can be dodged by
  stepping out of reach; one attack per 40 ticks (zombie 20).
- **Guard:** when the target is within 5 blocks and the attack is on cooldown, it may raise its guard for 30–60
  ticks: moves at half speed, melee from the front 120° takes 20 % damage, projectiles from the front are
  deflected (0). It lowers the guard to attack. An **axe hit breaks the guard**: 60 ticks staggered (no guard,
  no attack, slowed). Hits from behind are never blocked.
- **Spawns:** overworld surface at night, rare (weight 5, groups of 1, needs open sky). Sanctum outer halls
  place them with `PersistenceRequired`. Does not burn in daylight (helmeted).
- **XP:** 20 base (zombie 5). **Loot:** 1–4 iron nuggets, 0–2 bones, 10 % iron ingot on a player kill.
- Undead (smite works).

### Ember Wisp (base level 8, +3 below y=0 → 11)

- **Role:** small cave flyer that punishes exploring dark caves without light. Light is the counter.
- **Stats:** 8 HP, flying speed 0.5, contact attack 1 damage + sets the target on fire for 4 s, fire immune,
  hurt by water, no fall damage. Hitbox 0.5×0.5.
- **Behaviour:** hovers through caves; dives at a player within 16 blocks it can see. While it has a target, every 8–12 s it
  sets a fire on a block near it (only with `mobGriefing`), so it ignites the cave around the player.
- **Flees light:** at block light ≥ 7 not explained by fire (a torch nearby; its own fires don't count) it drops its target and flies to a darker spot within
  10 blocks; it will not dive at a player standing in light ≥ 7.
- **Spawns:** overworld below y=0 in darkness (block light 0) without open sky, weight 15, groups of 1–2, not in the deep dark.
- **XP:** 6. **Loot:** 0–1 glowstone dust (+Looting), 5 % blaze powder on a player kill.

## Mob levels (PLAN 3.5–3.7)

- **Who:** every `Monster` (vanilla or modded) not in the `embervoid:unleveled` entity type tag (Ender Dragon,
  Wither, Warden, Elder Guardian). Assigned once on `ENTITY_LOAD` when the entity has no level yet; stored in a
  persistent attachment (`embervoid:mob_level`), so it survives restarts and chunk reloads.
- **Default rules** (§5.4 with §10.9): base by type (`mobs.levels.baseLevels`, else `defaultBaseLevel` 4)
  + dimension bonus (Nether 5, End 8) + in the overworld only: +1 per 1000 blocks from world spawn (max +5) and
  +3 below y=0.
- **Effects:** max health through a fixed-UUID `MULTIPLY_BASE` modifier (fraction of health kept, so fresh mobs
  spawn full); damage scaled in `LivingEntity.damage` by the source's attacker (melee, projectile owner, creeper,
  witch potions); XP through `dropXp`.
- **Difficulty (3.6):** difficulty-scaled damage to players uses `mobs.difficulty` (Easy 1.0 / Normal 1.25 /
  Hard 1.75) at the start of `PlayerEntity.damage`; vanilla's branch is switched off on the server.
- **Display (3.7):** S2C `embervoid:mob_level` on `START_TRACKING` and on change; the client draws "Name Lv. N"
  above the leveled mob under the crosshair (24 blocks) or appends it to an existing name tag. Client toggle
  `hud.showMobLevel` in `embervoid-client.json`.
- **API** (`com.embervoid.mob.MobLevels`): `getLevel(entity)` (both sides), `setLevel(living, level)` (server,
  any living entity, rescales HP, resyncs), `registerProvider((mob, world) -> OptionalInt)` to decide levels at
  spawn (first non-empty wins, else the default rules), `defaultLevel(mob, world)`, `canHaveLevel(entity)`.
- **Config limitation:** `baseLevels` / `dimensionBonus` read only the keys present in the defaults (the section
  reader cannot list a JSON object's keys). New keys need either a default entry or a `keys()` method on
  `SectionReader` (player-core).
- **Debug:** `/embervoid moblevel <targets>` shows level, default level here and HP; `/embervoid moblevel
  <targets> <level>` overrides.
