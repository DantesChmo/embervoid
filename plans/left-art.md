# left-art — art pass for the overworld entities (PLAN L4)

Track branch `claude/angry-wright-3188de` (from `claude/tails-020-030-46a40d`). Owns `tools/overworld_mob_art.py`
and the `geo`, `animations`, `textures` of Ash Ghoul, Hollow, Ember Wisp, Ashen Maiden, Ashen Zombie, Ghost, Vampire
and the Vampire boss.

## Status

| Entity | Status | Look |
|---|---|---|
| Ash Ghoul | done | flaking ash corpse, ribs and spine through the skin, ember eyes in deep sockets, hanging jaw, rag loincloth, long arms with bone claws |
| Hollow | done | rusted mail hauberk, coif and chausses, torn ember-red tabard with the Sanctum's flame, open sallet with a nasal bar, grey dead face with cold pin-prick eyes, leather belt, bracers and boots, buckler on the left forearm |
| Ember Wisp | done | white-hot core with two dark eyes inside a translucent, open-topped shell of licking flame (red at the tips, yellow at the base), four flickering tongues, three sparks |
| Ashen Maiden | done | ash-grey robe with an ember-red embroidered hem and front panel, crossed bodice and sash, fringed shawl with an ember clasp, deep hood with a back drape, pale face with closed eyes and one ash tear-mark, bell sleeves, hands cupped around the ember |
| Ashen Zombie | done | ash-grey skin split by smouldering seams, ember eyes, slack jaw, a glowing wound under a torn blue work shirt, ragged trousers, ash on the shoulders |
| Ghost | done | translucent burial shroud fading from the shoulders to the wisp's tip, faint glow of its own, dark hood with cold eyes and a faint mouth, ragged mantle and sleeves, bony fingers, a broken iron shackle |
| Vampire | done | pale noble, slicked hair with a widow's peak, red eyes (dimmer than the boss's), thin red lips over fangs, burgundy brocade doublet with gold buttons, lace jabot with a ruby pin and lace cuffs, crimson sash, riding boots, high-collared black cloak lined crimson |
| Vampire boss | done | gaunt grey-violet skin, ribs through a torn doublet, ragged high collar, wild hair, curled horns, pointed ears, burning eyes, fanged maw, long claws; bat wings with an arm spar, a wrist claw, four fingers and a veined membrane with a scalloped trailing edge |

`python3 tools/overworld_mob_art.py [name ...]` writes geo, animation, texture and glowmask (deterministic, like
the Realm 1 scripts). The old placeholder scripts no longer write these entities: `mob_placeholders.py` keeps only its
helpers, `maiden_placeholder.py` and `nightmob_placeholders.py` are gone, `vampire_placeholders.py` writes only the
Vampire Fang item (unchanged, byte-identical).

## What the code relies on — kept

Grepped before changing anything: entity classes (`DefaultAnimations` controllers, `RawAnimation` names,
`Telegraph.windup` names and ticks), the renderers (`MobRenderers`, `NightMobClient`, `VampireClient`,
`AshenMaidenRenderer`: `turnsHead`, `right_item`, translucent layers, scales, glowmask layers). No Java code looks up
bones by name except through those.

- Bone names, hierarchy and pivots as in the placeholders (head on root or body as before). The Hollow's right arm box,
  its pivot and the `right_item` bone are unchanged, so the sword angle measured in game (`MobRenderers`, −55°) still
  fits. Only extra boxes were added; the Ashen Maiden's arm rest rotation was turned inward (−45, ∓25, ±12) so the
  hands cup the ember (no code reads it).
- Animation names and lengths unchanged. Telegraphs: Hollow swing raised by 0.3 s and held to the 0.4 s hit
  (WINDUP_TICKS 8); Vampire boss leap crouch to 0.9 s, jump at 1.1 s (LEAP_WINDUP 20 = 1.0 s), nova spread at 1.4–1.5 s
  (NOVA_WINDUP 30), mist folded at 0.6 s (MIST_WINDUP 12); Vampire transform 2.5 s held (TURN_TICKS 50).
- Every entity keeps a glowmask (all eight renderers add `AutoGlowingGeoLayer`).
- Sizes: `python3 tools/model_preview.py check` passes for all 27 models. Rest-pose sizes now (recorded in
  `tools/model_bounds.json` unchanged, all inside its tolerances): ash_ghoul 12×27.5×12.2, hollow 17.5×33×9.7,
  ember_wisp 11×9×8.5, ashen_maiden 14.2×33.5×14.4, ashen_zombie 17.6×32×16.4, ghost 14.5×30.8×18.3,
  vampire 14.4×32.3×9.5, vampire_boss 34.9×34.8×19.3 (wings +15 % wider: the membrane now spreads past the wrist).
  Hitboxes are code and unchanged.
- New JUnit `registry.EntityModelFilesTest`: every entity model has its animation file and texture and animates only
  its own bones; the eight overworld entities have the animations their classes play, the bones their renderers use
  (head, right_item) and telegraphs at least as long as their windups. Checked that it fails on a renamed animation and
  a misspelled bone.

## Animation changes

Secondary motion only, timings as above: head and body in idles and walks (ghoul twitch and lurch, Hollow breathing,
zombie sway, Vampire's cloak flutter), Hollow swing with a left-arm counterbalance and head follow, ghoul rake with a
body twist, Wisp's flame flicker (8-step scale + sway) and core pulse, Maiden's ember pulse and a slight lean on
interact, Ghost head tilt and a body lunge on the swing, Vampire transform with a short crouch before it rises.
Vampire boss: nova now opens the wings flat to the front and raises them (the placeholder's +70° yaw swept them
behind its back, since the rest pose is already 30° back); leap raises the wings and bends the legs in the crouch.

## Translucency (Ember Wisp, Ghost)

Both render with `RenderLayer.getEntityTranslucent`, and GeckoLib's glow layer blends with alpha (checked in the
4.8.4 sources: `AutoGlowingTexture`, translucent transparency, full bright). The textures carry partial alpha (Wisp
shell 105–185, tongues 150–230; Ghost shroud 215→175 on the body, 175→120 on the tail, 115→50 at the wisp's tip, hood
225, face 235) and the glowmasks a faint copy (Wisp 85–150, Ghost 70→15) so the Ghost reads at night. Shackle and
fingers stay opaque-ish (235). `tools/model_preview.py` draws alpha ≥ 0.1 as opaque, so for these two I also rendered
with a scratch copy of it that blends translucent texels back to front (not committed; copy in the notes folder).

## Review (model_preview, all eight)

Each model: rest pose from front, front-right, right, back-right, back and above with the hitbox, a dusk row, the
close-up (two large views + dusk), texture and glowmask atlases, and every animation's key poses; next to Cinder Husk
and Restless Bones for style. Fixed along the way:

- Ash Ghoul: the two eye sockets merged into one black band → separate sockets under a brow ridge; ember cracks on
  the legs and feet read as glowing toes → none on legs, fewer on arms.
- Hollow: chainmail pattern too contrasty (polka dots) → softer links; nose read as a pig snout → a single dark hole.
- Ember Wisp: the shell's top face floated as a lid over the cleared tongues → open top; cube silhouette → rounded
  corners; eyes read as orange marks → dark eyes.
- Ashen Maiden: hands far to the sides of the ember → arms turned inward (computed with the preview's transform);
  ember-lit eyes and dark tear streaks made the face a mask → closed lids with lashes, one small tear-mark, smoother
  skin.
- Ashen Zombie: dark hole rims dotted the shirt and trousers → fewer holes with cloth-coloured rims; arm seams too busy.
- Ghost: the shackle read as a brown blob → bright iron; the mouth read as a drip → a faint two-pixel mouth.
- Vampire: the standing collar hid behind the head → taller and wider, tilted back; white hands and cuffs read as
  mittens → smaller lace cuff, two rows of hand; dotted boots → one highlight column; hair top sheen lines.
- Vampire boss: the first wing had the wrist at the very tip with fingers fanning inward → wrist at mid-span, four
  fingers fanning out, membrane cut along a polygon with scallops; bat nose merged with the maw → shaded nose.

Preview PNGs (not committed): `side-session-notes/local_d3cc66d8-…/left-art-previews/` (and the session scratchpad
`…/scratchpad/final/`), `translucent/` for the blended Wisp and Ghost, `reference/` for the Realm 1 comparison.

## Not verified

Checked by preview only, not seen in game: real light levels, fog, the blood-moon sky tint on the Vampires, the
Ghost's translucency sorting against the world, head turning, animation blending.

## Notes for the coordinator

- `NightMobClient`'s class Javadoc still says the placeholders come from `tools/nightmob_placeholders.py` (deleted);
  ROADMAP's "Art pass" line and CORE §3.3 / §3.4 ("all models are placeholders", "`tools/mob_placeholders.py` writes
  placeholder assets") are now stale. Not edited here (docs and that class are not this track's).
