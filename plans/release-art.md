# release-art — 0.4.0 art pass (PLAN R40.2a–d)

Track branch `claude/release-art-4ffe4e`. Owns `tools/model_preview.py`, the `tools/*_placeholders.py` scripts,
`assets/embervoid/geo/**`, `animations/**`, `textures/**`.

## Status

| Task | Status | Notes |
|---|---|---|
| R40.2a Offline model preview | done | `tools/model_preview.py` |
| R40.2b Zone mobs art | todo | |
| R40.2c Citadel mobs, bosses, small entities | todo | |
| R40.2d Texture consistency (items, armor, blocks, Reliquary GUI) | todo | |

## R40.2a — model preview

`python3 tools/model_preview.py entity <name>` (or `entities`, `items`, `blocks`, `armor`, `gui`, `texture <png>`),
output in `build/model-preview/` or `--out DIR`. Pure Python, no numpy/PIL (none installed, and no downloads).

- Geometry follows GeckoLib 4.8.4 (checked against its sources in the Gradle cache: `BakedModelFactory`,
  `GeoQuad`, `RenderUtils`, `BakedAnimationsAdapter`): X mirrored into render space, rest rotation and keyframe
  rotation X/Y negated, bone order translate(position) → pivot → Z·Y·X → scale → back, box-UV face regions and
  their orientation (incl. the flipped down face), mirror, per-face UV, cube and bone inflate.
- Base layer cutout at alpha 0.1, no culling; glowmask added full bright (AutoGlowingGeoLayer); vanilla entity
  lighting (two lights, ambient 0.4); a "dusk" row at 0.32 light so the glowmask reads like in the realm.
- Hitbox wireframe scraped from the `register("id", …, w, h)` / `EntityDimensions.fixed(w, h)` calls.
- Animations: linear interpolation (catmullrom drawn as linear), simple molang (`query.anim_time`, `math.*`);
  key poses = the animation's keyframe times, at most 8 per row.
- Blocks: model JSON with parents resolved through the mod and Loom's local `minecraft-client.jar` (read only),
  element rotation and face UV rotation, vanilla face shading; 3×3 tiling of the main texture.
- Items: layer composite at 8× and 2×; vanilla references with `minecraft:<item>`.
- Armor: vanilla armor model (layer 1 inflate 1.0, layer 2 inflate 0.5) on a grey mannequin.
- Not shown: in-game light levels, fog, particles, head turning, bones the entity class hides or shows at runtime
  (all bones are drawn unless `--hide a,b`), animation blending, timing feel.

Validated on every greybox model (27 entities), all blocks, items and armor layers. Findings on the greybox it
already shows: the Hollow Sentinel thrust points the spear up (arm raised forward with the spear along it).

## Decisions

- Preview PNGs are never committed; merge notes point at the scratchpad copies.
