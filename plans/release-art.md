# release-art — 0.4.0 art pass (PLAN R40.2a–d)

Track branch `claude/release-art-4ffe4e`. Owns `tools/model_preview.py`, the `tools/*_placeholders.py` scripts,
`assets/embervoid/geo/**`, `animations/**`, `textures/**`.

## Status

| Task | Status | Notes |
|---|---|---|
| R40.2a Offline model preview | done | `tools/model_preview.py` |
| R40.2b Zone mobs art | done | `tools/reach_mob_art.py` (+ `tools/art_kit.py`) |
| R40.2c Citadel mobs, bosses, small entities | done | `tools/reach_mob_art.py`, `tools/reach_boss_art.py`, `tools/reach_item_art.py` |
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

`python3 tools/model_preview.py check [names]` checks what code relies on: every animation name the entity
class (and its superclasses) plays, every bone it toggles, the head bone (a note, not a failure: Bark Hollow and
the Grieving Tree have none and do not turn their heads), every move's windup ≤ its animation's length, every
animated bone exists, and the rest-pose size against `tools/model_bounds.json` (recorded from the greybox: height
±12 %, width/depth ±35 %, at least 2 px). Every entity sheet also writes `<name>_close.png` (two large views and
one at dusk).

Validated on every greybox model (27 entities), all blocks, items and armor layers. Findings on the greybox it
already shows: the Hollow Sentinel thrust points the spear up (arm raised forward with the spear along it).

## R40.2b — zone mobs

`python3 tools/reach_mob_art.py [name]` builds the final models on `tools/art_kit.py`: boxes with box UV (left
limbs share the right ones' region, mirrored), each face filled from a hand-picked ramp palette by a quantised
level (base + light from above + value noise) so it bands like vanilla textures, then face-space decals (eyes,
ribs, trims, tatters, burn holes, feathers). Glowing pixels keep a dim copy in the texture and the light in the
glowmask (added full bright in game). Skeletons, pivots, toggled bones and animation names/timings are the
greybox's; the zone mobs left `tools/mob_placeholders.py`.

- Cinder Husk: charred peasant, ember gash over the ribs, torn smock, rope belt, burnt straw hat, pitchfork.
- Ash Stalker: layered ash coat, faceless head with ember slits and a toothed jaw, spine flakes, two claws + thumb.
- Ashen Carrion: soot feathers with staggered tips, grey ruff, bald scorched head, bone beak, fingered wingtips.
- Withered Thrall: dead skin, hunting hood, leather jerkin with a quiver, noose and cut rope.
- Lantern Hag: moss-dark hood and shawl, hooked nose, grey hair, amber lantern, fire vials, ragged skirt.
- Bark Hollow: grooved bark with moss, knot-hollow face and mouth, twig fingers, root feet, dead-branch crown.
- Restless Bones: yellowed bones, see-through ribcage, burial shroud on one shoulder, rusted notched sword.
- Crypt Moth: dusty fur, comb antennae, flat wings with cut outline and eye spots; the glow bones carry only the
  pale-green pattern.
- Hollow Sentinel: rusted plate, slate tabard with bronze trim and key sigil, mail skirt, slit great helm, plank
  tower shield with iron rim and bronze boss, leaf spear.
- Grave Warden: shroud robe and mantle, bone mask with warm eyes and stitched mouth, keys, bronze bell with
  verdigris, banded cudgel.

Animation changes (names, lengths and windup keyframe times unchanged): secondary motion on the new parts (hat,
noose, lantern, bell, skirt, crown, walk bob); Hollow Sentinel thrust drives the spear forward level (the greybox
raised it upright) and the bash swings the shield forward and across instead of lifting it flat; Bark Hollow idle
no longer animates a `head` bone it does not have; Ash Stalker claws reach up while it emerges.

## R40.2c — Citadel mobs, bosses, small entities

Citadel mobs in `tools/reach_mob_art.py`, bosses in `tools/reach_boss_art.py` (replaces the deleted
`tools/boss_placeholders.py`; the Realm 1 part of `tools/mob_placeholders.py` is gone, so no greybox script can
overwrite final art).

- Cinder Hound: charred hide with glowing rib seams, ash-spike mane, glowing mouth, ember-tipped tail.
- Sparking Marksman: quilted soot-red gambeson, mail coif under a kettle hat, scarf, bolts; spark bone on the tip.
- Furnace Golem: riveted cast iron, glowing chest grate, chimney with embers; the heat bone lights only the seams.
- Smoldering Knight: blackened plate with ember seams, crimson surcoat with the king's gold crown, crested helm and
  plume, greatsword with a glowing edge.
- Crypt Keeper: grave-blue robe wrapped in a chain, faceless hood with cold eyes, mantle and pauldrons of bones,
  skull trophies and keys, crescent halberd, cold-flame lantern (the lantern bone).
- Ash Colossus: fused ash stone with ash drifts, magma seams and patches of granary brick, glowing brow eyes;
  chained stone plates over a cavity with the ember core (plates / core bones).
- Grieving Tree: dead oak with moss, weeping face with amber sap tears, the king's iron crown band, a withered
  canopy with a weeping fringe, 4 animated limbs + 4 fixed ones with frayed ropes; roots bone spread around.
- Smoldering Lord: black-and-gold royal plate (armor bone) over a burning body, half-melted crown, ash beard,
  burning crimson cape, flamberge with a glowing wavy edge and an ember in the guard.
- Small entities: the fire flask flies as its own sprite (`textures/item/fire_flask.png` through an extra item
  model, `client.reach.mob.FireFlaskRenderer`, full bright); the tree lanterns are the realm's hanging Ember
  Lantern (`TreeLanternRenderer`); new Ash Bomb texture (thrown and item). The two Java hooks are separate commits.

Animation changes: none in timing; Smoldering Lord's greatsword now hangs point-down from the hand, tilted 40°
forward by the bone's rest rotation (the greybox held it pointing up past the shoulder, so swings pointed it
backward): low guard at idle, over the shoulder in the slam windup, forward in the lunge. Some idle/walk secondary
motion (head, crown, branches, walk bob).

## Decisions

- Preview PNGs are never committed; merge notes point at the scratchpad copies.
- Glow: texture keeps 45 % of the colour, glowmask the full colour (bright, not white, in daylight).
- Moth glow bones: only the wing pattern glows (the rest of the overlay is transparent).
- Shapes may grow by a few pixels where details stick out (hat brim, spear tip, crown twigs); the check keeps them
  inside the tolerances, and hitboxes are code (unchanged).
- Grieving Tree canopy and limbs reach ~80 px (5 blocks) out so the lanterns (spawned 5.5 blocks out, 7-8.5 up)
  hang at the weeping fringe instead of in the air; width/depth recorded anew in `tools/model_bounds.json`
  (height unchanged). Its hitbox is still 3×10 (code), so frustum culling follows the trunk as before.
- Smoldering Lord depth grew (sword tilted forward); recorded anew.
- Zero-thickness planes (tabards, surcoats, cape, moth wings, antennae): in game the later-drawn of the two
  coincident faces shows (GeckoLib order west, east, north, south, up, down; depth test LEQUAL), so `art_kit`
  copies each plane's first face onto its twin, mirrored to look the same in the world; the preview draws the
  same way.
- Texture noise is seeded by a CRC of the model name (Python's `hash()` is salted per run): reruns are identical.
