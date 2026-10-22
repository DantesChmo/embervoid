# Track — citadel (0.4.0, PLAN 13.1–13.2)

Branch `claude/track-citadel-bf1d80`. Rules: [COORDINATION.md](../COORDINATION.md). Design: DESIGN §4.5 "Smoldering
Citadel". Took 13.1–13.2 over from track reach-world.

Owns the depths files (`dimension/ashen_reach_depths*`, `dimension_type/ashen_reach_depths`, the depths noise settings,
biome, arena placement, `worldgen/**/ashen_reach_depths*`, `worldgen/**/ashen_reach/depths/**`),
`structures/ashen_reach/citadel/**`, packages `com.embervoid.reach.citadel` / `client.reach.citadel`. Reuses reach-world's
realm code (placed enemies, arena sites, `realm_feature` placement) without changing it.

## Tasks

- [x] 13.1 Depths "Burning City": dimension type, terrain
- [ ] 13.2 Citadel (~10–12 pieces)

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

## Agreed with reach-bosses (courtyard)

Sent 2026-10-22: site as above; their template `embervoid:ashen_reach/arena_citadel` (73×73, y 0 = floor, north faces the
landing, i.e. world south). Inside the square is theirs (floor, 3 signal braziers, cover, the keep wall face the Lord
stands on in P0 along the template's south edge, gate x −2…2, y 1…6); my inner ring meets the square's north edge at
x −3…3, my keep starts behind it at world z ≤ −229 with its door at x −2…2. Entity id goes to `smoldering_lord` when
their 13.4 merges. Answer: pending.

## Status / notes

- Started 2026-10-22.
