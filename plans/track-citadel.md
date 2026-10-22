# Track — citadel (0.4.0, PLAN 13.1–13.2)

Branch `claude/track-citadel-bf1d80`. Rules: [COORDINATION.md](../COORDINATION.md). Design: DESIGN §4.5 "Smoldering
Citadel". Took 13.1–13.2 over from track reach-world.

Owns the depths files (`dimension/ashen_reach_depths*`, `dimension_type/ashen_reach_depths`, the depths noise settings,
biome, arena placement, `worldgen/**/ashen_reach_depths*`, `worldgen/**/ashen_reach/depths/**`),
`structures/ashen_reach/citadel/**`, packages `com.embervoid.reach.citadel` / `client.reach.citadel`. Reuses reach-world's
realm code (placed enemies, arena sites, `realm_feature` placement) without changing it.

## Tasks

- [x] 13.1 Depths "Burning City": dimension type, terrain
- [x] 13.2 Citadel (11 pieces + the courtyard)

## The plan of the Burning City

All numbers in `reach.citadel.CitadelLayout` (pure, JUnit `CitadelLayoutTest`); the terrain, the Citadel structure and the
tests read them from there. The layout is fixed, as DESIGN §4 asks for key places.

- Landing at 0, 0 (reach-world's `realm_feature` landing: shrine, Return Obelisk), ground y 66, in the lower city.
- The route runs north (−Z): outskirts (z −30…−62) → burning street → market → gatehouse in the city wall on the hill's
  south rim (z −128) → inner ring west and east of the courtyard → courtyard (the Smoldering Lord's arena site) → keep.
- The hill is a plateau at y 88: a rounded rectangle x −76…76, z −280…−128, its sides falling ~20 blocks to the lower
  city over 7 blocks (steep, broken by rough noise). The lower city rises from 66 at the landing to ~70 at the hill.
- Courtyard: centre (0, −192), floor 88, site radius 36 (template 73×73), fog wall 30, boss `ashen_reach/final` level 30.

## 13.1 Depths

- Dimension type `embervoid:ashen_reach_depths`: the realm type's dusk (`fixed_time` 13500), `ultrawarm` (water boils
  away, lava runs: the city burns), ambient light 0.1. Sky dark red `#5a1c12`, fog `#3c140c` (biome); reach-world's zone
  fog for the depths (0.40 dark red) still applies by biome.
- Terrain: density function `embervoid:citadel_terrain` (`CitadelTerrain`): ground height from the layout, `(h + 0.5 − y)
  / 8`, plus rough noise away from the terraces (stronger on the hill's sides). Every piece, the landing and the courtyard
  stand on a terrace: exactly flat at their floor height over their rectangle + 4 blocks, blended into the natural ground
  over 10 more. So the arena site's median rule gives exactly 88 at the courtyard. Radius 400 (reach-world's
  `radial_falloff` 336–400 kept).
- Surface: ash soil, blackstone and cobbled ash stone patches (noise `minecraft:surface`), ash stone below.
- Features (only in the depths biome, vegetal step): ember cracks (`minecraft:disk` r 0–2, 3 per chunk, only on the
  natural ground blocks) and `embervoid:smoldering_debris` (`SmolderingDebrisFeature`, 2 per chunk: a fallen smouldering
  beam 3–6 long, a charred post, or a brick rubble heap; only on natural ground, so never on the landing or a piece).
- Courtyard placement: `realm_feature` with angle spread 0 at distance 192 (start angle 90.001° so the floor of the cosine
  is 0, not −1: the centre is exactly 0, −192).
- Mob levels: the realm's depths band 25–30 (reach-world's `RealmMobLevels`, unchanged). Cinder Hounds spawn naturally
  (reach-mobs).
- GameTests `CitadelWorldTests`: own type with fixed dusk, ultrawarm, red sky, level band; terrain heights at the landing,
  courtyard and pieces; ember cracks and smouldering logs in the depths and none in any zone; the courtyard is the one
  depths arena at (0, −192), r 36, level 30.
- Not seen: how the sky, fog and hill look.

## 13.2 Citadel

- Structure type `embervoid:citadel` (`CitadelStructure`), structure `embervoid:ashen_reach_depths_citadel`, one start at
  (0, −160) by reach-world's `realm_feature` placement (distance 160, angle spread 0). Its pieces (`CitadelPiece`, a
  `SimpleStructurePiece` with reach-world's placed-enemy processor) are laid at the layout's fixed positions, unrotated.
  **Not a vanilla jigsaw** (decided here): a jigsaw turns its start at random and reaches only 128 blocks, while this
  route must meet a fixed arena site and the hill the terrain makes for it. Every piece is within 8 chunks of the start
  chunk (a structure only generates there; JUnit checks it). Terrain adaptation none: the ground is shaped to the pieces.
- Templates `embervoid:ashen_reach/citadel/<piece>` (datagen `client.reach.citadel.CitadelTemplates`, provider
  `CitadelTemplateProvider`, helper `CityBuilder`); y = 0 of a template is 3 below the piece's floor (foundation, and
  the stairs up from the piece before it).

| Piece | Box (x, z) / floor | What | Placed mobs | Chests |
|---|---|---|---|---|
| outskirts | −16…16, −62…−30 / 67 | road between burnt shacks, broken "city limits" arch | — (hounds natural) | 1 |
| street | −16…16, −95…−63 / 69 | burning street, two rows of burnt townhouses, one collapsed, fallen beams | — | 1 |
| market | −24…24, −120…−96 / 71 | stalls, dry fountain glowing with magma, toppled statue of the king | — | 1 |
| gatehouse | −12…12, −155…−121 / 74 | barbican towers, gate with raised portcullis, stair tunnel up 14 to the plateau, small plaza before the courtyard, roof level with the wall walk | 3 Marksmen on the roof | — |
| wall_west / east | ±(13…64), −133…−128 / 88 | wall 5 thick, walk at +8, merlons, inner stair, a 2-wide breach | 2 Marksmen each | — |
| tower_west / east | ±(65…76), −139…−128 / 88 | corner tower, floors 0 / 8 (walk level) / 15, roof 21, ladder | 1 Knight each (top floor) | 1 each |
| inner_ring_west | −76…−38, −228…−140 / 88 | ring road along the courtyard, the royal foundry (blast furnaces, hearth, lava cauldrons), two burnt mansions | 3 Golems | 1 |
| inner_ring_east | 38…76, −228…−140 / 88 | ring road, the barracks (bunks, weapon racks), two burnt mansions | 2 Golems | 1 |
| keep | −28…28, −272…−229 / 88 | forecourt with braziers, keep 2-thick walls to +20, corner turrets, great hall with pillars and chained lanterns, the empty throne on a gilded dais, the treasury behind it | 2 Knights | treasure + 1 |

- Totals: 7 Sparking Marksmen, 5 Furnace Golems, 4 Smoldering Knights; 7 `citadel_common` chests, 1 `citadel_treasure`.
- GameTest `citadelBuildsAlongTheRoute`: one start at its fixed chunk, 11 pieces with full-size templates, the mobs each
  template's markers name (per piece), the start placed as worldgen does: no marker left, 16 placed mobs spawned (then
  removed), only Citadel chest tables, one treasure chest inside the keep, ≥ 7 common, and the route open at the
  outskirts, market, gate, tunnel, plaza, keep forecourt and hall.
- Server check: locates the Citadel at (0, −160), force-loads x −76…76, z −272…−30, then expects the keep's treasure
  chest at (0, 89, −265) and 7 / 5 / 4 placed Marksmen / Golems / Knights.
- Not seen: how any of it looks (no client). Lighting is campfires, ember lanterns, magma and ember cracks; no fire
  blocks (fire would spread through the burnt planks).

## Agreed with reach-bosses (courtyard)

Sent 2026-10-22: site as above; their template `embervoid:ashen_reach/arena_citadel` (73×73, y 0 = floor, north faces the
landing, i.e. world south). Inside the square is theirs (floor, 3 signal braziers, cover, the keep wall face the Lord
stands on in P0 along the template's south edge, gate x −2…2, y 1…6); my inner ring meets the square's north edge at
x −3…3, my keep starts behind it at world z ≤ −229 with its door at x −2…2. Entity id goes to `smoldering_lord` when
their 13.4 merges. Answer: pending.

## Status / notes

- Started 2026-10-22.
