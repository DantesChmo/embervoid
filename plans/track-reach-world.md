# Track A — reach-world (0.4.0, milestones 9 and 13.1–13.2)

Branch `claude/track-reach-world-85fd2c`. Scope and rules: [COORDINATION.md](../COORDINATION.md). Tasks: PLAN.md
"0.4.0" 9.1–9.9, 13.1–13.2. Design: DESIGN §4.5.

Owns `com.embervoid.realm` (incl. `realm.worldgen`, `realm.fog`), `com.embervoid.reach.world`,
`com.embervoid.client.realm` / `client.reach.world`, realm `dimension*` / `worldgen/**` JSON,
`structures/ashen_reach/**` except arena contents.

## Tasks

- [x] 9.1 Fog API + per-zone fog
- [x] 9.2 Placed enemies
- [x] 9.3 Zones renamed, eternal dusk, zone looks
- [ ] 9.4 Zone terrain density + arena sites
- [ ] 9.5 Realm 1 blocks and ores
- [ ] 9.6 Landing plaza, roads, milestones, outposts
- [ ] 9.7 Minor structures per zone
- [ ] 9.8 Necropolis crypt jigsaw + water
- [ ] 9.9 World edge
- [ ] 13.1 Depths "Burning City"
- [ ] 13.2 Citadel jigsaw

## 9.1 Fog API

- Pure `realm.fog.FogStack` (JUnit `FogStackTest`): layers by source id, the densest wins (tie: first pushed),
  shown fog eases towards it (time constant 0.8 s; from no fog the colour is taken at once and only the weight fades in).
- Density 0–1 → fog end `8 + 184·(1 − d)²` blocks, start `end·(1 − d)·0.6`; never further than vanilla (blindness,
  darkness, view distance still win). Terrain fog only, open air only (not under water / lava / powder snow).
- Colour: vanilla fog colour blended to the layer colour by the eased weight (also the clear colour, so the horizon matches).
- Clearing factors (Ring of the Fog Stalker): `RealmFog.setClearing(source, 2)` doubles the fog end of clearable layers;
  arena fog is pushed with `clearable = false` and ignores it.
- Client API `client.realm.RealmFog`: `push(source, density, color[, clearable])`, `clear(source)`, `has`,
  `setClearing`, `density()`, `reset()` (on disconnect). Mixin `RealmFogMixin` on `BackgroundRenderer`.
- Zone fog `client.reach.world.ZoneFog`: source `embervoid:zone`, from the biome under the camera, values in
  `reach.world.ReachZones` (Fields 0.35 grey-ochre, Woods 0.60 grey-green, Necropolis 0.45 blue-grey, landing 0.30,
  abyss 0.35, depths 0.40 dark red). Colours are dusk-dark because the sky at time 13500 is near night.
- Not seen: how the fog looks (no client launch). Values are first guesses for QA.

## 9.2 Placed enemies

- Marker block `embervoid:placed_enemy` (`realm.placed.PlacedEnemyMarkerBlock`, horizontal `facing`, invisible, no
  item, no drops) with block NBT `{entity: "<id>", tags: [...], nbt: {…}}` — build it with
  `PlacedEnemies.markerNbt(id, nbt[, tags])`.
- Processor `embervoid:placed_enemies` (`PlacedEnemyProcessor.INSTANCE`): marker → air, spawns the entity once (only the
  pass whose chunk box holds the marker; a template is processed once per chunk it crosses). Facing follows the
  template rotation/mirror (block states are rotated after processors run, so the processor turns the facing itself).
- Spawn (`PlacedEnemies.spawn`): `initialize(…, STRUCTURE)`, then the marker's `nbt` merged in, command tag
  `embervoid.placed` (`PlacedEnemies.isPlaced(entity)`, read by reach-mobs) plus the marker's `tags` (e.g.
  `embervoid.placed.cage` for caged Thralls), `setPersistent()`. Unknown id: one WARN per id, nothing spawned.
- Level: the zone band from `RealmMobLevels` when the mob loads (nothing new needed). Never respawned: it exists once,
  from worldgen; shrine rest does not touch entities.
- Markers built in game with a structure block lose their NBT (no block entity); templates are built in code.
- GameTests (`ReachWorldTests`): spawns once across chunk passes, persistent + tagged + marker gone, unknown id spawns
  nothing, facing follows a 90° rotation, the realm provider gives a placed mob its zone band. The test server never
  brings far realm chunks to entity tracking, so the last test checks `MobLevels.spawnLevel`, not the loaded level.

## 9.3 Zones

- Names (both languages): biomes and zone titles `embervoid.zone.ashen_reach.<landing|zone_a|zone_b|zone_c|depths>` —
  The Crossroads / Ashen Fields / Royal Woods / Sunken Necropolis / Smoldering Citadel. Biome ids stay `zone_a/b/c`.
- Zone title (`reach.world.ZoneTitles`): every 10 ticks, a player who crosses into another zone of a realm gets its
  name as a grey subtitle (fade 15/50/25), like a Souls area name. Nothing past the abyss edge.
- `fixed_time` 13500 in `dimension_type/realm.json` (also the depths until 13.1 gives them their own type).
- Biome looks: colours, particles (Fields ash, Woods mycelium specks, Necropolis white ash), vanilla Nether ambient
  additions as placeholders, no precipitation. Surface: Fields ash soil + burnt coarse-dirt patches; Woods podzol +
  moss + bare coarse dirt; Necropolis gravel + tuff + broken cobbled ash stone (noise patches).
- Vegetation (placed features `embervoid:ashen_reach/*`): Fields dead bushes on the coarse patches; Woods dead trees
  (dead logs, branchy fancy trunks and bare poles, no leaves) + dead bushes / moss carpet / ferns; Necropolis candle
  clusters (mostly lit). Every feature checks its ground block, so nothing grows on arena or landing stone.
- Vanilla `spawners` removed from every realm biome (reach-mobs adds spawns in code).
- GameTests: fixed time 13500, no vanilla spawns in the realm biomes, the zone title follows a player from a zone to
  the landing. Not seen: the looks (colours, particles, sounds, trees).
