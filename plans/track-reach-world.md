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
- [x] 9.4 Zone terrain density + arena sites
- [x] 9.5 Realm 1 blocks and ores
- [x] 9.6 Landing plaza, roads, milestones, outposts
- [x] 9.7 Minor structures per zone
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

## 9.4 Terrain and arena sites

- Density function `embervoid:zone_blend` (`realm.worldgen.ZoneBlend`): a terrain shape per zone (JSON density
  functions) chosen by the same angle sectors and warp noise as the biome source, blended over ±48 blocks of arc at the
  borders (at most a third of a sector) and over 40 blocks outside the landing disc. Weights in pure
  `RealmLayout.zoneWeights` / `landingWeight` (JUnit).
- Shapes (`noise_settings/ashen_reach.json`): landing nearly flat; Fields a low rolling plain (0.2 × hills);
  Woods hills (0.55 × hills) cut by ravines (new noise `realm_ravines`, up to ~26 blocks deep); Necropolis a basin
  ~14 blocks lower with sinkholes (new noise `realm_sinkholes`, up to ~38 deep). Sea level stays at the bottom: dry
  everywhere; Necropolis water comes from structures/features (9.8).
- Arena sites: optional `site` in the arena structure JSON (`realm.worldgen.ArenaSite`: radius, height offset,
  ground block, template). The floor height is the median of the ground at the centre and 8 points at ⅔ of the site
  radius, plus the offset; the piece fills the site down to the terrain, clears it to the sky (64 up) and terrain
  adaptation (`beard_box` for the sunken ones) blends the slopes. Site r / offset: Fields 48, −8 (crater); Woods 56, 0;
  Necropolis 40, −18 (sinkhole; the template's rim is at y 18). Fog wall r 40 / 48 / 32, levels 18 / 21 / 24 (from
  reach-bosses' 12.2 numbers).
- Arena template hand-over (track reach-bosses): `embervoid:ashen_reach/arena_fields|arena_woods|arena_necropolis`
  under `data/embervoid/structures/`; placed centred on the site, its y = 0 layer at the floor, its north (−Z) side
  turned towards the landing in 90° steps (`RealmFeaturePiece.rotationTowardsLanding`, same rule as reach-bosses'
  `ArenaFrame`), with the placed-enemy processor. The code puts the shrine block at `shrinePos()` = centre +
  rotated (0, 1, −(fog r + 4)), on the template's north axis. Without a template the placeholder disc stands there.
- GameTest: each arena start made and placed at its real position (the test world does not generate structures); the
  site is solid at the floor and open for 12 blocks above. Server check passes (realm, arenas, depths generate).

## 9.5 Blocks and ores

- `reach.world.ReachBlocks` (block items through `ModItems.register`): Burnt Straw (hay-like pillar, flammable),
  Milestone and Gravestone (`MarkerStoneBlock`, facing, post / thin headstone shapes, hand-made models), Ember Crack
  (magma that burns, luminance 9, lava sparks; Citadel only, 13.1), Smoldering Log (log, luminance 6, smoke; Citadel
  only), Wet Crypt Stone (drips underneath). Ores in ash stone: Cinder Ore (iron tier, 1–3 Cinder Shard, Fortune,
  1–3 XP), Amber Vein (iron tier, 1–2 Royal Amber, Fortune), Grave Iron Ore (diamond tier, Raw Grave Iron like vanilla
  raw ores). Silk touch drops the ore. Tag `embervoid:realm_ores` = the three ores.
- Generation, each only in its zone biome (`underground_ores`): cinder like iron (14 × size 9, y 4–80 trapezoid),
  amber like gold but high for ravine walls and under roots (6 × size 7, y 30–90), grave iron like diamond, deep and in
  sinkhole walls (5 × size 6, y 2–44, 30 % discarded on air). Fields: scattered burnt straw bales; Necropolis: lone
  gravestones between candles.
- Datagen `client.reach.world.ReachWorldDataGen` (models, loot, `realm_ores`); vanilla tool tags go through the main
  block tag provider (`ReachWorldDataGen.vanillaBlockTags`, one line there: two providers cannot write the same file).
  Textures: `tools/reach_world_placeholders.py` (procedural stand-ins).
- GameTests: drop counts with iron / diamond pickaxes, tool tiers, the tag; four chunks at each zone's sector centre hold
  only that zone's ore. Server check passes. Not seen: textures, models, particles.

## 9.6 Landing, roads, outposts

- Landing (`RealmFeaturePiece`, kind landing): the crossroads plaza, brick paving r 21 (some cracked), the old polished
  disc r 9 around the shrine; Return Obelisk, Sealed Gate and shrine where they were. A broken low wall on the rim with a
  gap and two lantern posts where each road leaves. The landing piece (and the protected area) is now r 21 + 7.
- Roads: pure `realm.RoadLayout` (JUnit `RoadLayoutTest`): one road per arena from the plaza edge (r 22) to the site
  edge, a seeded sideways wobble (≤ 24 blocks, zero at both ends), resampled every 2 blocks; a milestone every 100
  blocks, 4 to the right walking out, facing the road; a toll gate at 30 % and a watchtower at 65 % of the length.
  `realm.worldgen.RealmRoads` computes them once per world and seed from `RealmArenas` (now with each site radius).
- Feature `embervoid:realm_roads` (`reach.world.RoadFeature`, placed once per chunk in the landing and zone biomes,
  `surface_structures` step): paves 5 wide on the surface (ash bricks / cracked in the middle, cobbles / ash stone at the
  edges, ~15 % edge gaps), clears 2 above, sets milestones, places outposts whose centre is in the chunk. A road is a
  feature, not a structure: a structure start cannot reach 900 blocks.
- Outposts: datagen templates `embervoid:ashen_reach/outpost/<toll_gate|watchtower>_<fields|woods|necropolis>`
  (`client.reach.world.OutpostTemplates`, written by `ReachTemplateProvider`). The road runs along +Z through the
  template's middle column; centred on the road, turned along it (`RoadFeature.rotationAlong`), foundation under the
  footprint, the feature does not pave under it. Toll gate: two ruined towers, broken arch, half barrier; watchtower: 5×5
  tower with ladder and fenced platform. Each: 2 zone fodder + 1 zone "ranged" placed mob (Fields Ashen Carrion
  perching, Woods Lantern Hag, Necropolis Crypt Moth), a `<zone>_common` chest, a zone accent. Mob table in
  `client.reach.world.ReachZoneKit`.
- Debug: `/embervoid realm roads` (works from the console with `execute in`): lengths, milestones, outposts and whether
  each has its chest. Server check force-loads every outpost and expects all chests.
- GameTests: road 0 paved along its first ~120 blocks, its first milestone, both outposts have their zone chest; the
  plaza (start placed by hand) has the shrine, a wall gap and two lantern posts at each road exit.
- Not seen: how roads look on steep ground (they follow the surface; no bridges or steps), outposts and plaza looks.

## 9.7 Minor structures

- 21 datagen templates (`client.reach.world.MinorTemplates`, drawn with the small `Ruin` helper), ids
  `embervoid:ashen_reach/<zone>/<name>`: Fields farmstead, barn, windmill skeleton, scarecrow row, granary vault
  (treasure, vault 4 below ground), pilgrim camp, gallows, empty village altar; Woods park wall, hunting lodge
  (treasure in the loft), kennels, hunter's hide, hanging cages (3 caged Thralls tagged `embervoid.placed.cage`),
  lantern row, glade (2 Bark Hollows); Necropolis small and large mausoleum, grave rows, chapel (Grave Warden + bell),
  ossuary pit, collapsed tomb (2 below ground). The crypt entrance comes with the crypt jigsaw (9.8).
- Mobs by zone role (`ReachZoneKit`): fodder / ranged / tank / elite = Fields Cinder Husk / Ashen Carrion / Ash Stalker
  (no Fields tank) / Ashen Carrion; Woods Withered Thrall / Lantern Hag / Bark Hollow / Bark Hollow; Necropolis Restless
  Bones / Crypt Moth / Hollow Sentinel / Grave Warden. Chests `<zone>_common`, treasure `<zone>_treasure`.
- Each template is a single-piece `minecraft:jigsaw` structure (`worldgen/structure/ashen_reach/<zone>/<name>.json`,
  pool of one element, processor list `embervoid:ashen_reach/minor` = placed enemies + 12 % cracked bricks), biomes
  `#embervoid:ashen_reach_<zone>` (new biome tags), `beard_thin` (none for the two sunk ones).
- One structure set per zone, weighted (treasure structures and the chapel weight 1), placement
  `embervoid:realm_scatter` (`RealmScatterPlacement`: linear random spread, spacing 9 / separation 3, plus a 24-block
  clearance from road centre lines, arena sites and the plaza; arenas and roads come from the placement calculator, so
  `RealmArenas` / `RealmRoads` now work from a `StructurePlacementCalculator`).
- GameTests: all minor structures built in their zone with no marker left and only `<zone>_*` chest tables; scatter
  starts within 1200 blocks of the landing (≥ 150) never near a road, site or the plaza. Server check locates and
  generates a barn, hanging cages and a chapel.
- Not seen: how the ruins look.
