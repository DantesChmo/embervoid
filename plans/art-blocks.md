# Track V-A `art-blocks` — blocks, textures and structures to the art direction

Branch `claude/art-blocks-efae30`, worktree `track-reach-mobs-b93e7c`. Brief: COORDINATION.md "Tracks — 0.5.0 visual
overhaul", bar: docs/art-direction.md. Nobody answers questions tonight: decisions are written here.

## Task list

1. [x] `tools/palette.py`: the shared ramps and the drawing helpers.
2. [x] `tools/block_art.py`: every block texture redrawn (replaces `reach_world_placeholders.py`, `road_art.py`, the
   block part of `stele_art.py`); `tools/prop_art.py` restyled.
3. [x] About 25 new building blocks with datagen and lang.
4. [ ] Every structure builder rebuilt from the wider palette; helpers in `Ruin` / `TemplateBuilder`.
5. [ ] Render and judge (`model_preview.py blocks`, `structures`), iterate.
6. [ ] MERGE REQUEST to the coordinator.

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

## Status

- Step 1 committed: 30 ramps, self-checked (`python3 tools/palette.py`).
- Step 2 committed: 72 block textures from `block_art.py` (42 redrawn, 30 for the new blocks of step 3), the 44 prop
  textures restyled; `reach_world_placeholders.py` and `road_art.py` deleted, `stele_art.py` keeps the Crying Soul.
  `block_art.py` prints a report per texture; `--sheet` writes a contact sheet with the 3x3 tiling. Looked at on the
  sheets: every family reads as drawn (bevelled plates, bricks of their own tone, ridged bark, bone, riveted iron).
- Step 3 committed: 25 new blocks (38 registrations with stairs, slabs and walls), datagen for models, loot, tags and
  recipes, en + ru names, render layers; `runDatagen` and `model_preview.py self-check` pass. Pillar sides use the
  `_side` texture name `cube_column` expects.

## What was seen, what could not be seen

(filled in at step 5)
