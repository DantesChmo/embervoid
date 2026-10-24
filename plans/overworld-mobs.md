# Track `overworld-mobs` — the seven overworld and moon-night mobs (PLAN 19.3, art direction §1.8 / §2)

Branch `claude/overworld-mobs`. Rules: COORDINATION "Tracks — 0.5.0 visual overhaul"; the bar: docs/art-direction.md.
Nobody answers questions: decisions are recorded here as they are made.

## Decisions

- **Base.** The worktree started 116 commits behind `main` (before the particles, the art direction and the bosses
  merge). The branch was moved onto `main` (`c1c19d5`, then `2597d2d`) before the first commit, so the coordinator's
  merge is a plain fast-forward-ish merge and `art_kit`'s dense-form helpers, `EmberParticles` and `ReachBoss` are
  reused, not copied.
- **`tools/overworld_kit.py`** carries what this track needs and nothing of the other tracks': the hue-shifted ramps
  of the seven mobs (checked with `palette.check_ramps`' rule), the few face helpers copied from
  `reach_mob_art.py` with the same values (so `vampire_boss()`'s output stays byte-identical while
  `overworld_mob_art.py` no longer imports a module the `mobs` track is rewriting), translucency and glow helpers,
  and the animation helpers shared by the seven (breath, a weighted walk, flinch, fall, settle). The dense forms
  (`chamfered`, `octagon`, `chain_bones`, `lag_sway`, `whip`, `fingers`, eased keys) come from `art_kit`.
- **Java plumbing** mirrors `ReachBoss` by name: `entity.mob.MobAnimations` holds the controller name `hurt`, the
  animation names `misc.hurt` / `misc.stagger` / `misc.death`, the 10-tick flinch cooldown and two factory methods
  (the hurt controller, the death controller holding the last frame). Each mob keeps its own `lastHurtAnimation` /
  `hurtFlinches` and overrides `damage`, `onDeath` and `updatePostDeath` (`deathTicks()` from the animation's
  length; vanilla removes a body at 20 ticks). Renderers return 0 from `getDeathMaxRotation` (no vanilla tilt).
- **Who gets what.** Hurt: Ghoul, Hollow, Wisp, Zombie, Ghost, Vampire (on the blow that provokes it, under the
  transform). Stagger: the Hollow (its broken guard, a 3 s held reel on the stance controller) and the Zombie (a hit of
  a quarter of its health or more). Death: Ghoul (collapse, ash), Hollow (to its knees, then over), Wisp (gutters
  out), Zombie (falls forward), Ghost (dissolves upward), Vampire (a collapse, for `/kill` only — it cannot die
  otherwise). The Maiden is invulnerable: no hurt, no death; her idle and interact are the work.
- **Balance untouched:** hitboxes, the Hollow's 8-tick windup and 40-tick interval, the Ghoul's blinding cloud at
  the moment of death, every damage number. Death only delays the body's removal; loot and XP drop at death as before.

## Per mob

(filled in as each mob is done: cubes / bones, what was seen in the preview, what cannot be seen offline)
