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
| Landing | Return obelisk, Ember Shrine (sanctum's block), Sealed Gate. Protected (unbreakable blocks + no building inside arenas/landing) |
| Player realm data | Own attachment `realm_player` (persistent, copied on death): Sanctum return point, last rested shrine per realm |
| Respawn | `AFTER_RESPAWN`: died in a realm → teleport to that realm's last rested shrine (or landing). Overworld spawn point untouched; beds in realms do not set spawn |
| Progress | Sanctum's `ProgressManager` (on `main`); Realm 1's id is `ProgressManager.REALM_1` |
| Shrine | Sanctum's Ember Shrine calls `ShrineHooks` (`ALLOW_REST`, `RESTED`); realms set the respawn point and keep arena shrines dormant |
| Mob levels | `RealmMobLevels` is a `MobLevels` provider (zone band); bosses get the arena's level via `MobLevels.setLevel` |
| Boss framework | `boss`: `ArenaBoss` interface + `BossFight` (boss bar with level, cylinder fog wall, participants, reset on leave/death, phases, credit, Ember Heart), `ArenaManager` (summon rule from arena structures), `Telegraph`, boss music S2C packet, arena shrine dormant per §10.8 |
| Test boss | `TestBossEntity` (zombie model), two phases, telegraphed slam |

## Tasks

- [x] R1 Plan (this file)
- [x] R2 `RealmLayout` pure math + tests (8 JUnit tests)
- [x] R3 Realm dimension: biome source, falloff density function, dimension/type/noise JSON, placeholder biomes, void push-back
- [x] R4 Final-zone dimension
- [x] R5 `realm_feature` structure + placement: landing, 3 zone arenas, final arena; protection
- [x] R6 Realm blocks, teleports, realm player data, respawn, shrine hooks
- [x] R7 Sealed Gate, Ember Lodestar, zone level bands (decided; applying waits on the mobs API), debug commands
- [x] R8 Boss framework
- [x] R9 Test boss
- [x] R10a Progress stub replaced by `ProgressManager`
- [x] R10b Level bands and boss levels through `MobLevels`; real Ember Shrine instead of the stand-in
- [ ] R10c In-game check (requested), fixes, final merge request

## Contracts used / offered

- Uses: sanctum `progress` API and Ember Shrine block; mobs `MobLevels` (provider for zone bands, `setLevel` for bosses).
- Offers: `realm.api.RealmTeleports.enterRealm(player, realmId, toLastShrine)` for the Sanctum's Void Obelisk;
  `realm.api.ShrineHooks` events for the Ember Shrine.

## Status / notes

- Debug: `/embervoid realm enter <realm> [toShrine] | leave | where | arenas | data`, `/embervoid boss fights`.
- The realm radius is in both the dimension JSON and `Realms` (void push-back); keep them equal.
- Arena shrines look lit for everyone; dormancy is only the refusal to rest. A per-player look needs client-side progress.
- Balance (QA #8): the test boss at Lv 18 hit for 37 melee / 23 slam with 404 HP. Its base values (200 HP, 8 attack,
  10 slam) are level-1 numbers that `MobLevels` then scales; real bosses need base values chosen with that scaling.
- Every arena leaves an Ember Shrine: worldgen places it, and the kill re-places it if it is missing (logged).
