# Track V-B `bosses` — the six boss remodels (PLAN 19.3 for bosses, 19.4)

Branch `claude/bosses-a652a2`. Rules: COORDINATION "Tracks — 0.5.0 visual overhaul"; the bar: docs/art-direction.md §1.8, §2.
Nobody answers questions tonight: decisions are recorded here as they are made.

## Decisions

- **Shared layer first, one commit** (art kit, preview tool, boss base class, tests), then one commit per boss. The
  base class carries what every boss needs the same way, so a boss commit is art + its own code only.
- **Bevels** are `chamfered(...)`: a big form becomes three boxes, each shrunk by the chamfer on two axes; the union
  has all twelve edges knocked off by one or two pixels. Rounder things (heads, lanterns, the censer) get `octagon`:
  the box plus the same box turned 45° and narrowed, on one pivot. Both are cheap in cubes and paint like any box.
- **Secondary motion**: `chain_bones` builds 2–3 hanging segments as a bone chain; `lag_sway` gives them a looping
  sway with each segment a little behind the one above (idle, walk); `whip` flings them after a strike (the strike
  tick unchanged) and settles them. The same helpers serve cape, veil, chains, straps, hair, lantern ropes, branches.
- **Easing**: `keys()` takes `e("easeOutBack", vec)` for a keyframe, written as GeckoLib's `{"vector", "easing"}`
  (easing applies to the segment *into* that key, as GeckoLib's BakedAnimationsAdapter reads it). The preview tool
  interpolates with the same easing functions so the trail sheets show the overshoot.
- **Textures**: `Material(bevel=…)` lightens the top-left pixel edge of a face and darkens the bottom-right by
  that many ramp steps (art direction §1.2); `outline_shadow` stays for contact shadows. Every face gets a decal
  with structure (rivets, folds, grain, sutures); no flat fills.
- **Hurt / death / stagger / phase in code**: `ReachBoss` gets a `hurt` controller (between walk/idle and the move
  controller, so a move's animation wins over a flinch), plays `misc.hurt` on damage taken outside a windup with a
  10-tick cooldown, plays `misc.death` held on the last frame and keeps the body `deathTicks()` ticks (the fight
  credits and ends at once as before; `ArenaManager.tickBoss` is skipped while the body lies there), and exposes
  `hurtFlinches()` for tests. `misc.stagger` where the boss has a stagger state (the Tree's stun keeps its name
  `misc.stunned`; the Colossus staggers when its core takes a hit in P2; the Lord when the armor shatters — reuses
  `misc.shatter`; the Keeper when a lit brazier is near... no: the Keeper has no stagger state, none added).
  The phase animations keep their names (`misc.shed`, `misc.uproot`, `misc.lantern_shatter`, `misc.raise_dead`,
  `misc.shatter`, `misc.veil_burn`); the Vampire boss gets `misc.phase` (its form shifts, the wings flare).
  The Fallen Sister's own hurt/death code moves into the base class unchanged in behaviour.
- **Renderers**: no vanilla death tilt for any boss (own death animations); the emissive layer stays; a translucent
  heat-shimmer layer for the Colossus's bare core and the Lord, a mist layer for the Vampire boss (`GeoRenderLayer`
  drawing the model again, scaled up a little, translucent, on the glow texture) — only where it pays off.
- **Effects**: vanilla particles for now with `// TODO own particle` where an ember / ash / soul particle belongs.
  Every slam, landing, sweep, shatter and death: `CombatFx.impact` / `trail` / `shake` plus particles. Damage,
  timings, hitboxes untouched.
- **Tests**: `EntityModelFilesTest` lists every boss's animations and toggled bones; `BossAnimationTests` (GameTest,
  written and compiled, run by the coordinator): a hurt during a windup leaves the windup and plays no flinch, a hurt
  between moves plays one; a kill keeps the body for `deathTicks()` and then removes it (Colossus, Keeper).
  The Vampire boss is not in a GameTest (its fight needs the whole blood-moon harness); its code follows the same
  pattern and `EntityModelFilesTest` covers its animation names.

## Per boss

| Boss | Cubes | Bones | Seen offline | Not seen |
|---|---|---|---|---|
| Smoldering Lord | 105 | 28 | sheets and poses: fist-sized ember sockets and a jaw under the melted crown, layered plate with pauldrons and tassets, the three-segment cape and tatters, swing windups, the slam overhead, the death on one knee against the planted sword then on his side | the halo layer (EmberHaloLayer: glowmask drawn again, scaled, translucent), particles, the armor flung away in game |
| Vampire boss | 71 (was 40) | 24 (was 9) | sheets and poses: chamfered torso, skull and legs, a three-piece cape with its crimson lining, coat tails, hair strands, a tatter, a jaw with fangs, forearms and clawed hands; the nova spread, the arched kneel and the fold forward of the death | the mist (scale) in game, the 1.2 renderer scale, blood particles, the claw trail on the vanilla melee |
| Fallen Sister | 78 (was 33) | 25 (was 18) | 16.3 left her short of the bar: boxed tiers and bodice, no hands, a two-link chain. Now chamfered tiers, bodice and skull, an octagon hood, circlet geometry, two hair strands, a two-piece train with tassels, hands with fingers that grip the chain, a four-link chain, drips of ember as cubes; her moves keep their keys with easing on the strikes and the new parts whipping; a stagger on a heavy hit. Seen: the slam overhead, the kneel-and-fold death | the veil burning (scale to nothing) in game, the hurt controller under a move, particles |
| Grieving Tree | 128 | 35 | sheets and poses: the weeping face as geometry (brow, sockets, amber tears, a jaw that moans), the iron crown band and points, bark plates, two-segment roots, root legs with toes, four limbs of three segments with ropes, eight hanging curtains; the sweep, the stunned droop, the forward crash | the roots bone hidden in P2, leaf and sap particles, the lanterns (separate entities) |
| Crypt Keeper | 103 | 27 | sheets and poses: the hood's void with two cold-light cubes, bone mantle and pauldrons, keys, skulls and belt chains as geometry, bone hands, the halberd with tassel, the lantern on a chain; overhead and thrust windups, the lantern throw, the kneel and the face-down fall with the halberd laid beside him | the lantern hidden in P2, particles, cold-blue trails |
| Ash Colossus | 108 | 26 | front/side/back/dusk sheets, key poses and trail sheet: chamfered mass, fists with fingers, chains swinging, kneel-then-topple death, fist/boulder/smash windups readable, stagger lunge | the core's shimmer layer, particles, the plates shed in game, the hurt controller under a move |
