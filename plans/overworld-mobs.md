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

- **Bounds.** Every model stays within `tools/model_bounds.json`'s tolerance; where a first build overshot (the
  Ghoul's splayed thumbs, the Hollow's buckler and sallet tail, the Maiden's folded arms and bowl, the Zombie's reach,
  the Ghost's shroud and hands, the Wisp's tail) the part was pulled in rather than the recorded size changed.
- **Commits.** One per mob, except the Ghost and the Vampire together (their test block in `EntityModelFilesTest`
  references both classes, so a split would not compile in between). `vampire_boss()` and its assets are untouched:
  rerunning the script regenerates them byte-identically.
- **Lang.** No new user-facing strings: no new sounds or messages were needed.

## Per mob

Cubes / bones from `write_model`; every mob rendered with `python3 tools/model_preview.py entity <name>` (six views,
dusk row, atlases, the close sheet) and `motion <name>` (trail cell and frames at 10 fps) and iterated until the
sheets read. "Not seen" is what the offline preview cannot show: in-game light, the glow layer's blend, particles,
GeckoLib's blending between controllers, the renderers' head turn and scale, translucency (the preview draws
cutout).

| Mob | Cubes / bones | Seen offline | Not seen |
|---|---|---|---|
| Ash Ghoul | 56 / 20 | hunched chamfered torso with rib plate and glowing sternum, skull with sunken ember sockets and hanging jaw, clawed hands with a hooked thumb, two-bone tatters front and back; the lope, the two-claw rake, the flinch, the collapse onto its face with the rags settling | the ash cloud, flakes, soot and sparks of the death; the hurt controller under a swing |
| Hollow | 53 / 16 | chamfered hauberk under breastplate, gorget, belt, mail skirt, tabard pieces with the brass flame, dented sallet with visor slit, tail and crest, pauldron lames, couters, bracers, gauntlets, cuisses, greaves, octagonal buckler; the swing held raised to the 0.4 s hit, the breathing guard, the stagger reel over 3 s, the kneel-and-topple death | the sword drawn at `right_item` (now under `right_forearm`), the 1.1 renderer scale, soot and sparks, the stagger held on the stance controller for exactly the 60 ticks |
| Ember Wisp | 30 / 16 | octagonal core and shell, three licking two-bone tongues, the tail streaming in flight, two spark rings; the flare on a hit, the guttering death with the sparks flung off and the core dropping | the translucent flame (cutout here), the embers drifting off it, the final burst |
| Ashen Maiden | 59 / 25 | chamfered skirt, bodice and head, hem pieces with ember stitching, crossed wrap, sash, fringed shawl with the ember clasp, octagonal hood with brow rim and circlet, lace veil over a pale closed-eyed face, bell sleeves with their falls, fingers cupping an octagonal brass bowl with its flame; the six-second idle (breath, look, hem / veil / sleeves out of phase, flame licking, ember pulsing), the bowl lifted in `misc.interact` | her glow at hub light, the head turn toward the player |
| Ashen Zombie | 49 / 18 | chamfered torso and skull, the glowing split chest under the shirt torn open, shirt tails on chains, crust flakes on shoulders and scalp, slack jaw, four hanging fingers per hand, shins and boots; the limping shamble, the two-arm swing, the flinch, the one-second stagger, the stiff forward topple onto its arms | ash flakes off it, the stagger chosen by hit size |
| Ghost | 41 / 21 | chamfered shroud torso and dark skull, octagonal hood with peak and fall, the shroud trailing in three fading segments, sleeve falls, bone forearms and three-fingered hands, shackle with two chain links; the hover, the flight with the shroud streaming, the lunge, the shiver on a hit, the dissolve (rise, turn, shrink to a point) | translucency and the fade to the wisp's tip (drawn opaque here), the half-lit render, cold flame, the souls at the end |
| Vampire | 53 / 20 | chamfered doublet and skull, brocade with gold buttons, jabot with ruby pin, sash with tail, high collar, three-piece cape with crimson lining, gold-trimmed coat tails, slicked hair with widow's peak, red eyes and fangs, lace cuffs, gloved fingers, turned-down boots; the stroll, the transformation rising with the cape flaring, the jolt of the provoking blow, the kneel-and-fold death | blood drops during the turn and the death, the transform's packet timing against `TURN_TICKS` |

## State at hand-over

All seven done, one commit each (Ghost and Vampire together). Checks run here: `python3 tools/model_preview.py check`
over the seven and the untouched Vampire boss (sizes within tolerance, every animation the code plays present, the
head and `right_item` bones present), `./gradlew compileJava compileClientJava compileTestJava compileGametestJava`
green, `./gradlew test --tests 'com.embervoid.registry.EntityModelFilesTest'` green. Not run here (the coordinator's
regression): `OverworldMobAnimationTests` (three GameTests: the Ghoul's body stays for its death animation, the Ghoul
flinches once per cooldown, a hit during the Hollow's wind-up keeps the wind-up and shows no flinch), `runGametest`,
the server check, anything in a client.
