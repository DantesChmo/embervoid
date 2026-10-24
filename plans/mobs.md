# Track mobs — the 14 Realm 1 mobs remodelled (PLAN 19.3, the zone and Citadel parts)

Branch `claude/mobs`, a worktree of the main checkout. Rules: [COORDINATION.md](../COORDINATION.md) "Tracks — 0.5.0
visual overhaul". Bar: [docs/art-direction.md](../docs/art-direction.md) §1.8 (entity skins) and §2 (models). Nobody
answers questions: every decision below is mine and final for this track.

Owns `tools/reach_mob_art.py`, the new `tools/mob_kit.py`, the 14 mobs' `geo` / `animations` / `textures`, the entity
classes under `reach/mob`, `client/reach/mob/ReachMobClient`; shared files touched once: `ReachMob` (the plumbing),
`EntityModelFilesTest` (one contiguous block), the GameTest list (`MobAnimationTests`). No lang keys were needed: the
animations and effects add no user-facing string.

## Decisions

- **Branch base.** The worktree sat 106 commits behind `main` when the track started; everything the brief relies on
  (art-direction.md, the 0.5.0 tracks, `EmberParticles`) is on `main`, so `claude/mobs` was reset to `main` before the
  first commit, and `main` merged again once the bosses track landed (its `art_kit.py` helpers and `ReachBoss` pattern
  are reused, not duplicated).
- **Plumbing mirrors `ReachBoss`** name for name: `HURT_CONTROLLER = "hurt"`, `HURT = "misc.hurt"`,
  `STAGGER = "misc.stagger"`, `DEATH = "misc.death"`, `HURT_COOLDOWN = 10`, `hurtAnimation(source, amount)`,
  `hurtAnimations()`, `hurtFlinches()`, `deathTicks()`, `tickDeath(world, deathTime)`, `updatePostDeath` removing the
  body after `deathTicks()`, the death animation as `thenPlayAndHold`. The renderer returns 0 from
  `getDeathMaxRotation` (no vanilla tilt). Controller order movement → state → hurt → action, so a move beats a flinch.
- **Every mob gets all three** of hurt / stagger / death. `tools/model_preview.py check` demands every
  `"misc.*"` literal of the class chain, and `ReachMob` holds the three names; a mob without a stagger state of its
  own plays `misc.stagger` on a heavy hit (≥ 35 % of its max health, `HEAVY_HIT_SHARE`). The mobs with a state use it
  there: the Sentinel hit while its shield hangs open, the Thrall knocked off its cage, the Bones kicked as a pile.
- **Death ticks** are a per-class constant (`DEATH_TICKS`) that `deathTicks()` returns; `EntityModelFilesTest`
  holds `misc.death` to that length (± 10 ticks) and to the 1.5–2.5 s of the brief. The server never reads the
  animation file, so the constant is the contract.
- **Flyers** (Carrion, Moth) get gravity back on death so the body drops out of the air during its fall.
- **Easing** keyframes are written with `art_kit.e(...)`; `model_preview.py` draws them with the same curves since
  the bosses merge.
- **Scythe, not pitchfork** for the Husk (the brief's "the Husk's scythe"); its reach stays within the recorded
  width / depth tolerance, which forced the blade shorter (13 px) and turned 25° out of the hand.

## Per mob

| Mob | Cubes / bones | Seen offline | Not seen offline |
|---|---|---|---|
| Cinder Husk | 57 / 21 | Entity sheet and close-up: the hat, the ember eyes and the teeth, the open smock with the rib embers, the belt with its knot and ends, tatters at the hem, the straw tuft, the fingers; motion sheet: the swing hauls the scythe over the right shoulder and brings it down with the body, the flinch, the reel, the fall forward ending flat with the scythe thrown out | ember particles in idle and the fall, the glowmask under in-game light, the eased curves at game frame rate, the hurt / action controller blending |
| Furnace Golem | 72 / 16 | Entity sheet: the chamfered cast-iron body, the firebox bars behind the hinged door, the eight-sided chimney with its cap, the shoulder chains; motion sheet: the slam hauled overhead and brought down, the heat shudder, the wave flinging the door open; the death from the side (sits down, tips onto its face with the arms flung ahead, the door slammed shut — first pass had the arms and the open door through the ground, fixed) | the heat overlay toggled in game, the fire pouring out on death, the shockwave and burst sizes |
| Smoldering Knight | 76 / 20 | Entity sheet and close-up: the eight-sided great helm with the ember visor and the gold crest, the plume behind it, the breastplate's ridge and ember seams, the layered pauldrons, gauntlets with fingers, the crimson surcoat with the crown and the cape, the greatsword's gold guard and hot edge; motion sheet: the three blows of the combo, the crouch and the level thrust of the lunge (first pass thrust upward, fixed with a sword-bone turn), the death on one knee then onto the face with the sword laid flat | the swing trails and the finisher's scorch in game, the ember stream along the dash, the glowing edge under in-game light |

## Status

Done: all 14 mobs committed one by one on `claude/mobs`; the Golem and the Knight were finished after the usage limit stopped the track (their rows above; earlier mobs are described in their commit messages).
