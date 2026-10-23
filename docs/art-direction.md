# Embervoid — Art direction (the 0.5.0 visual overhaul)

The user's verdict on 0.4.1 (2026-10-23) and again on 2026-10-24: the mod "looks like an old Minecraft version"
and "cheap" next to Create. This document is the bar every visual task is held to, for the scripted art (`tools/`),
the models, the structures and the effects. Read it before touching any asset. The rules are concrete on purpose:
a track checks its own output against them in `tools/model_preview.py` renders before it reports.

What "modern" means here, taken from Create, Cataclysm and Mowzie's Mobs: textures that are *drawn* (big shapes,
hue-shifted ramps, a light direction), models that are *dense* (bevels, layered plates, parts that trail behind the
motion), places built from *many* blocks with gradients of decay, and *motion everywhere*: machinery turns, cloth
swings, every weapon has a signature in the air. Vanilla's 16 px per block stays: Create is 16 px too. The cheap look
is not resolution, it is flat noise, few blocks and still scenes.

## 1. Textures

Scale: **16 px per block everywhere** (blocks, items, props, entities at 1 px = 1/16 block). Never mix densities on
one model.

1. **Palette with hue shift.** One ramp of 5–7 colours per material. Shadows go *cooler and more saturated* (stone
   towards blue-violet, wood and cloth towards red-brown, fire towards deep red), highlights go *warmer and less
   saturated* (towards yellow-white). A ramp that only changes brightness is wrong. `tools/palette.py` holds the
   named ramps the whole mod shares (ash stone, ash brick, dead wood, charred wood, grave iron, gold trim, bone,
   ember, amber, moss, crimson cloth, ash cloth, grave blue, brass); a script copies a ramp only until that module
   is on `main`.
2. **Light from the top-left.** Raised forms get a 1 px light edge on top and left and a 1 px dark edge on bottom
   and right; mortar, gaps and recesses are darker; a recess is darkest on its top-left. Metal gets a 1 px specular
   dot or line on the lit side.
3. **Contrast.** Every face shows at least four ramp steps. A block's values span at least 35 % of 0–255 (ash stone:
   about 30 in the mortar to 170 on a lit chip). No mud: per-pixel noise of one ramp step over a whole face is the
   look we are leaving behind. Variation is *structural*: bricks of different shades, a chipped corner, a crack that
   runs across two bricks, grain lines and knots, a drip of soot.
4. **Shapes first.** Big shapes (brick rows, planks, plates) read at a distance; details (cracks, rivets, nails)
   second; colour accents (ember glow, moss, amber) third and on at most 5–8 % of the pixels.
5. **Tiling.** A block tiles with no seam and no obvious repeat in a 3×3: rows of different brick widths, one or
   two broken bricks, an off-centre knot. Check the 3×3 in `model_preview.py blocks`.
6. **Items.** The silhouette reads at 16 px. The outline is a dark shade of the colour it borders (glass rims read
   as glass, iron as iron), never one flat black. Two or three materials per item, each with its own ramp. Weapons
   lie on the diagonal (grip bottom-left, tip top-right) like vanilla's. A 1 px bevel: lighter top-left inner edge,
   darker bottom-right.
7. **Emissive.** Embers, eyes, runes and cold flames go on the glowmask; emissive pixels are bright and saturated,
   and the non-glowing pixels of the same ramp are darker than they would be otherwise, so the glow pops.
8. **Entity skins.** Every face has structure: plates with rivets and a bevelled edge, cloth with 1–2 px darker fold
   lines that follow the body, bark with grain and knots, bone with sutures, fur and feathers as jagged edges. The
   cube's outline gets a 1 px darkening (`outline_shadow`). A flat noisy fill on any face is a defect.

## 2. Models

Budget (cubes / bones): **bosses 80–140 / 20–40**, elites 50–80 / 15–25, ordinary mobs 30–60 / 12–20, set pieces
20–80. Today's bosses sit at about 24 / 9 ("the bosses are stools").

- **Bevels.** Big forms (head, shoulders, chest, fists) lose their box corners: an inset smaller cube on each
  corner, or a second cube rotated 45° (or 22.5°) on the same pivot. A silhouette with corners knocked off reads as
  a sculpted form.
- **Layers.** Armor plates, pauldrons, bark, feathers, scales, cloth are *separate thin cubes* (1–2 px thick, with
  `inflate`) over the body, overlapping like shingles, not painted on the body cube.
- **Secondary motion.** Everything that hangs is a chain of 2–3 bones with lagging sway in idle and walk and a
  whip-back on attacks: cloaks, veils, hair, tails, chains, straps, lantern ropes, tatters, branches, the Lord's
  cape, the Keeper's chains, the Tree's limbs. Mowzie's Mobs is the reference.
- **Keep what the code relies on:** the hitbox and size (`tools/model_bounds.json`, `tools/model_preview.py check`),
  the bones the code hides or shows, the animation names (`EntityModelFilesTest`), and move timings (the 18.4
  balance). Add bones and cubes freely.
- **Animation rules.** Idle breathes (chest 1–2 px, shoulders) with the secondary parts swaying out of phase. Walk
  has weight: body bob, hips and torso counter-rotate, the head stays level. Every attack is anticipation → strike →
  recovery with easing (`easeInOutSine` in, `easeOutBack`/`easeOutCubic` on the strike, GeckoLib keyframe
  `"easing"`); the strike frame is the extreme pose with the secondary parts whipping after it. Every mob gets
  `misc.hurt` (a flinch that never cancels a wind-up), `misc.death` (a fall that the code waits for before removing
  the entity) and `misc.stagger` where it can be staggered; bosses also a phase-change animation.

## 3. Blocks and places

- **Variety.** A façade uses at least six block types: a base course (cobbled, darker), the main wall, a trim or
  pillar material (polished, chiseled, pillars), a detail (carved stone, a rune block, iron grates), a roof material
  (slate tiles, charred planks, burnt thatch), a floor of two or more blended materials, then props. Stairs, slabs,
  walls and trapdoors as mouldings, sills and cornices; fences, chains and iron bars for railings and grilles.
- **Decay gradients.** Cracked and mossy variants grow with height and towards edges and corners; rubble piles at
  the base of broken walls; soot above fire; ember cracks only in the Citadel. A ruin tells how it fell.
- **Colour.** Each zone has an accent material (Fields: burnt straw and brass; Woods: amber and moss; Necropolis:
  bone and grave-blue candles; Citadel: gilded trim and ember glass) on the few spots the eye should land.
- **Motion.** Set pieces (GeckoLib block entities) wherever the place would move: sails, hoists, bells, cages,
  gears and portcullises, bellows, chain lifts, censers, smoke. Boss arenas move with the fight (a bell tolls at
  `raise_dead`, a portcullis slams at the summon, braziers ignite per phase).

## 4. Effects

- **Own particles** (`embervoid:` particle types with 8×8 sprite sheets): ember (lit, rises, flickers out), ash
  flake (grey, drifts), soot puff, cinder spark (fast, gravity), soul wisp (pale blue, seeks a point), blood drop,
  amber mote, cold flame, ground scorch. Vanilla particles only for things vanilla already shows well (smoke).
- **Every weapon has a signature** (DESIGN §6.1 identities): what it draws in the air on a swing, on a hit, on a
  kill and on its special, with its own colour and sound. The Scourge cracks (a bright whip-line, a shockwave ring
  at the tip, sparks, a camera kick); the Scythe reaps (a wide crescent trail, an ash wave you can see, souls drawn
  out of what it catches into the player); the Rapier's ramp glows up the blade; the Crossbow's bolt trails embers
  and bursts.
- **Fog walls** are volumes, not a sheet: layered scrolling noise, embers streaming up, a scorched ground ring,
  flickers of light inside, a hum. The arena wall and the Citadel border share the look.

## 5. Checking

Every visual task renders its output before reporting: `python3 tools/model_preview.py entity <name>` /
`motion` / `blocks` / `items` / `structure` and looks at the PNGs. A track reports what it saw and what cannot be
seen offline (in-game light, particles, render layers). Nothing is played in game by a Claude session.
