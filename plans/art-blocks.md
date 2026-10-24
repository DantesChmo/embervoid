# Track V-A `art-blocks` — blocks, textures and structures to the art direction

Branch `claude/art-blocks-efae30`, worktree `track-reach-mobs-b93e7c`. Brief: COORDINATION.md "Tracks — 0.5.0 visual
overhaul", bar: docs/art-direction.md. Nobody answers questions tonight: decisions are written here.

## Task list

1. [x] `tools/palette.py`: the shared ramps and the drawing helpers.
2. [x] `tools/block_art.py`: every block texture redrawn (replaces `reach_world_placeholders.py`, `road_art.py`, the
   block part of `stele_art.py`); `tools/prop_art.py` restyled.
3. [x] About 25 new building blocks with datagen and lang.
4. [x] Every structure builder rebuilt from the wider palette; helpers in `Ruin` / `TemplateBuilder`.
5. [x] Render and judge (`model_preview.py blocks`, `structures`), iterate.
6. [x] MERGE REQUEST to the coordinator.

## Decisions

- **Ramps** are hand-picked hexes in `tools/palette.py`, 7 colours each, dark to light. Shadows lean blue-violet
  (stone, iron, slate) or red-brown (wood, cloth, fire); highlights lean yellow-white. A `check_ramps()` self-test
  asserts the hue shift direction and the value span, so a ramp that only changes brightness fails the script.
- **Brick scale.** Ash bricks are two courses of 8 px per block (big shapes read at a distance, like deepslate
  bricks); the new "ash cobble bricks" are four courses of 4 px, the small-brick look. Tiles are 2×2 of 8 px with a
  bevel. Every brick has its own tone, a 1 px light edge top-left and dark edge bottom-right; mortar is the darkest
  colour. Seeded chips and one crack across two bricks per tile.
- **Ash stone** is sculpted dark stone: cellular (Worley) lumps with a lit top-left edge, no embers. Embers belong
  only to Ember Crack, the Smoldering Log, the Citadel blocks (charred log seams, ember glass) and the ores'
  inclusions.
- **Emissive blocks.** Vanilla has no glowmask for blocks. Rune stone, ember glass, charred log and amber get bright
  saturated pixels plus a block `luminance`; the ramp around them is kept darker so they read as lit. Noted as not
  seen offline (light levels).
- **New block ids** (en / ru in the lang files), where registered:
  - `ModBlocks`: `ash_tiles` (+ stairs, slab), `cracked_ash_tiles`, `mossy_ash_bricks` (+ stairs, slab, wall),
    `ash_brick_pillar`, `carved_ash_stone`, `ash_cobble_bricks` (+ stairs, slab, wall), `sooty_ash_bricks`,
    `mossy_cobbled_ash_stone`, `slate_tiles` (+ stairs, slab), `sanctum_rune_stone`.
  - `ReachBlocks`: `gilded_ash_stone`, `ember_glass`, `charred_planks` (+ stairs, slab), `charred_log`,
    `burnt_thatch`, `bone_bricks` (+ wall), `bone_pillar`, `grave_iron_grate` (pane), `grave_iron_plate`,
    `rusted_brass_block`, `cut_brass`, `tar_block`, `ash_cobble_path`, `amber_block`, `crypt_stone_tiles` (wet,
    drips like Wet Crypt Stone).
- **Render layers:** `ember_glass` translucent, `grave_iron_grate` cutout (the existing `putBlocks` line in
  `EmbervoidClient`).
- **Structures.** Footprints, jigsaws, markers, chests and the depth conventions stay; only the blocks change.
  Helpers added to `TemplateBuilder` (cornice, sill, pitched roof, arch, buttress, grille, blended floor, decay by
  height) and `Ruin` (zone palettes, rubble at wall feet, soot over fires).

- **Ruined roofs** burn from the ridge down in runs along the ridge (`pitchedRoof`'s coverage), never as random holes:
  random holes left stair blocks floating in the air in the first renders.

## Status

- Step 1 committed: 30 ramps, self-checked (`python3 tools/palette.py`).
- Step 2 committed: 72 block textures from `block_art.py` (42 redrawn, 30 for the new blocks of step 3), the 44 prop
  textures restyled; `reach_world_placeholders.py` and `road_art.py` deleted, `stele_art.py` keeps the Crying Soul.
  `block_art.py` prints a report per texture; `--sheet` writes a contact sheet with the 3x3 tiling. Looked at on the
  sheets: every family reads as drawn (bevelled plates, bricks of their own tone, ridged bark, bone, riveted iron).
- Step 3 committed: 25 new blocks (38 registrations with stairs, slabs and walls), datagen for models, loot, tags and
  recipes, en + ru names, render layers; `runDatagen` and `model_preview.py self-check` pass. Pillar sides use the
  `_side` texture name `cube_column` expects.
- Step 4 committed: helpers in `TemplateBuilder` (`cornice`, `innerCornice`, `stringCourse`, `pitchedRoof`, `gable`,
  `arch`, `buttress`, `grille`, `blendedFloor`, `weathered` / `weatheredWalls`, `soot`, `trySet`) and `Ruin`
  (zone `Palette`s FIELDS / WOODS / NECROPOLIS, `facade`, `cornerPiers`, `floor`, `roof`, `rubbleAlong`),
  `CityBuilder` (`facade`, `pillar`, `sootOver`); every builder rebuilt: Sanctum halls get a base course, weathered
  walls, tiled floors, ribbed cornices, pillars with capitals, arched doorways, rune stones and soot over lanterns;
  the three surface ruins get buttresses, pillar quoins, slate remnants; the hub's core walls and path; the Fields
  timber frames with thatch and brass, the Woods lodge with slate, amber and grilles, the Necropolis tombs with bone
  trim, crypt tiles and blue candles; the Citadel on blackstone façades with gilded string courses, ember-glass
  windows, charred timber and slate, iron plates and tar underfoot. `runDatagen`, `model_preview.py self-check`,
  `SanctumStructureSetTest`, `HubLayoutTest`, `PropShapesTest` green; 115 placed enemies as before.
- Step 5 committed: every template rendered and read; roofs reworked twice (runs from the ridge down, then no column
  without a wall under its eave). Ash bricks and ash stone redrawn after the coordinator's review (commit 2b).
- Step 6: MERGE REQUEST sent to the coordinator.

## What was seen, what could not be seen

Seen offline (`tools/block_art.py --sheet`, `model_preview.py blocks`, `model_preview.py structures`, the PNGs read):

- **Blocks.** Every texture on the contact sheets at 4x and as a 3x3: the stone family reads as sculpted lumps,
  flat-toned brick courses with chips and one crack, bevelled slabs and tiles, fluted pillars, the carved and gilded
  reliefs; dead wood as grey planks with grain, knots and nails, logs with ridged bark; charred wood with its
  crackle and ember seams; bone bricks, riveted iron plates, brass with rust blooms, amber lumps, the ember-glass
  lattice; thatch courses and straw; crypt stone with drips; the ores' inclusions with a highlight each. The 3D
  renders of every new block (pillars with their `_side` / `_top`, walls, stairs, the grate pane) show the right
  faces. After the coordinator's review the ash bricks were made four flat courses and the stone's lumps vary.
- **Props.** All 14 props re-rendered from six angles: weave on the sacks, staves and hoops, folds on the cloth,
  lichen on the graves, throwing rings on the urns.
- **Structures.** Eye-level, iso and top renders of all 68 templates. The Sanctum ruins show pillar quoins with
  capitals, weathered walls (moss low, cracks and gaps high), slate remnants, rune-stone rims; the halls their
  arches, ribbed cornices, tiled floors, grilles and soot over the lanterns; the keep its rows of ember-glass
  windows and gilded courses; the Necropolis tombs their bone quoins, crypt tiles and blue candles; the Fields
  pieces their timber frames, thatch and brass; the Woods lodge its slate, amber and grilles. First renders showed
  random roof holes as floating stairs; the roofs now burn from the ridge down in runs (re-rendered and checked).

Not seen offline: block light (the rune stone, ember glass, charred log and amber glow only in game), the
translucent ember glass and the grate's cutout layer, the wet-stone drips, the `.mcmeta` animation of the lantern
and the stele crack, the stair / wall / pane shapes the game recomputes on placement (the renderer draws the saved
state), and how the zone fog sits on the new colours. Nothing was played in game.
