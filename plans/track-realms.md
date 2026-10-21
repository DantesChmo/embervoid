# Track 4 — realms (milestones 6–7 on placeholders)

Branch `claude/track-realms-f13672`. Scope and rules: [COORDINATION.md](../COORDINATION.md). Design: DESIGN §4, §9,
§10.1 and §10.8 (recommended answers are built).

Packages owned: `com.embervoid.realm` (dimensions, worldgen, teleports, gate, lodestar), `com.embervoid.boss`
(boss framework), `com.embervoid.client.realm` / `client.boss` (client hooks), realm worldgen JSON under
`data/embervoid/{dimension,dimension_type,worldgen}/**` for `ashen_reach*`.

## Architecture

| Piece | How |
|---|---|
| Realm 1 dimension | `embervoid:ashen_reach`: own `dimension_type`, `noise_settings` (simple hills, no water: sea level at min Y), biome source `embervoid:realm_zones` |
| Zones | `RealmBiomeSource` (codec-registered): landing disc, 3 angle sectors around 0,0 with borders warped by the router's `temperature` noise (seeded), abyss biome past the radius. Math in pure `RealmLayout` |
| Abyss edge | Density function `embervoid:radial_falloff` (codec-registered): `final_density = min(terrain, falloff)`, so terrain ends in cliffs over the void at ~1200 blocks. Past radius + margin the void pushes players back |
| Final zone | Separate dimension `embervoid:ashen_reach_depths`, same type, radius ~400, fixed biome |
| Arena placement | `RealmFeaturePlacement` extends `RandomSpreadStructurePlacement` (so `/locate structure` works): one start chunk per world, angle inside the zone's sector (kept clear of warped borders), distance from the seed |
| Landing / arenas | Code-built structure type `embervoid:realm_feature` (`kind`: landing / arena), one piece type; placeholders until the `.nbt` builds |
| Landing | Return obelisk, Ember Shrine (stand-in), Sealed Gate. Protected (unbreakable blocks + no building inside arenas/landing) |
| Player realm data | Own attachment `realm_player` (persistent, copied on death): Sanctum return point, last rested shrine per realm |
| Respawn | `AFTER_RESPAWN`: died in a realm → teleport to that realm's last rested shrine (or landing). Overworld spawn point untouched; beds in realms do not set spawn |
| Progress | Interface `RealmProgress`; until sanctum's `progress` API is on `main` a transient in-memory stub (no saved copy of that data) |
| Shrine | `ShrineHooks` events (`ALLOW_REST`, `RESTED`) for sanctum's Ember Shrine. Stand-in until it lands: soul campfire in realm dimensions |
| Mob levels | `RealmMobLevels` assigns zone bands on entity load; writes through a stub bridge until the mobs level API is on `main` |
| Boss framework | `boss`: `ArenaBoss` interface + `BossFight` (boss bar with level, cylinder fog wall, participants, reset on leave/death, phases, credit, Ember Heart), `ArenaManager` (summon rule from arena structures), `Telegraph`, boss music S2C packet, arena shrine dormant per §10.8 |
| Test boss | `TestBossEntity` (zombie model), two phases, telegraphed slam |

## Tasks

- [ ] R1 Plan (this file)
- [ ] R2 `RealmLayout` pure math (zone by warped angle, falloff, arena position) + tests
- [ ] R3 Realm dimension: biome source, falloff density function, dimension/type/noise JSON, placeholder biomes (3 zones, landing, abyss), void push-back
- [ ] R4 Final-zone dimension
- [ ] R5 `realm_feature` structure + `RealmFeaturePlacement`: landing and 3 zone arenas + final arena; protection
- [ ] R6 Realm blocks (return obelisk, Sealed Gate), teleports, realm player data, respawn, shrine hooks + stand-in
- [ ] R7 Sealed Gate logic, Ember Lodestar, zone level bands, `/embervoid realm` debug commands
- [ ] R8 Boss framework (bar, fog wall, reset, phases, summon rule, credit, Ember Heart, telegraphs, music, arena shrine)
- [ ] R9 Test boss
- [ ] R10 Swap stubs for `progress` / mob level API when merged; check request; merge request

## Contracts used / offered

- Uses (stubbed until merged): sanctum `progress` API; mobs level API; sanctum Ember Shrine block.
- Offers: `realm.api.RealmTeleports.enterRealm(player, realmId, toLastShrine)` for the Sanctum's Void Obelisk;
  `realm.api.ShrineHooks` events for the Ember Shrine.

## Status / notes

- JUnit is not in `build.gradle` yet (asked the coordinator).
