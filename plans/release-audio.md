# release-audio (R40.3a–b)

Sound events and boss music for Realm 1. Nothing here has been heard in game: the user does not play-test and the
track never launches a client, so every pitch and volume below is a first guess from the vanilla files.

## Tasks

| Task | Status |
|---|---|
| R40.3a sound events: registry classes, `sounds.json`, subtitles (en_us, ru_ru) | done |
| R40.3a hooks: zone mobs, Citadel mobs, bosses, weapons and consumables, blocks, Reliquary | done |
| R40.3a GameTests: registry ↔ `sounds.json` ↔ lang; telegraphs play their event | done |
| R40.3b music events (placeholders), `CREDITS.md` | done |
| R40.3b CC-licensed music candidates → coordinator (QUESTION) | sent; waiting for the user's choice |
| R40.3b add the approved tracks (`.ogg` + attribution) | waiting for approval |

## How it is built

- `registry/ModSounds.register(name)` registers one event; each package keeps its events in its own class:
  `reach.mob.ReachMobSounds`, `reach.boss.ReachBossSounds` (+ Ash of Rematch, boss music),
  `reach.gear.GearSounds`, `reach.world.ReachBlockSounds` (+ block sound groups), `reliquary.ReliquarySounds`.
  `ModSounds.register()` loads them all during mod init (a sound class first loaded after init would hit the frozen
  registry).
- `assets/embervoid/sounds.json`: every event lists `minecraft:` sound files (no files of our own yet) with its own
  pitch, volume and variants. Event ids follow vanilla (`entity.<mob>.<what>`, `item.<item>.<what>`,
  `block.<block>.<what>`, `ui.reliquary.<what>`, `music.<theme>`).
- Subtitles: `subtitles.embervoid.<event id>` in both lang files. Steps and block break/place/hit use vanilla's
  generic subtitles (`subtitles.block.generic.*`), as vanilla does; music has none.
- Telegraphs: every move's windup sound (`ReachMob.Move.sound`, `Telegraph.windup`) is the creature's own event, so
  each windup has its own subtitle. The pitch lives in `sounds.json`; the code passes 1.0 (a few keep a small random
  spread for repeated sounds such as the Thrall's chain rattle).

## Decisions (small, made here)

- **Files, not event references.** Each event points at concrete vanilla files rather than `"type": "event"`, so
  one variant set can be picked per creature (e.g. only the ghast screams for the Carrion's shriek). Checked once
  against the 1.20.1 asset index: all files exist (see the JUnit-free check in the merge notes).
- **Explosions stay vanilla** (`entity.generic.explode`: landing rings, boulder impact, cover smash) — the vanilla
  subtitle "Explosion" is accurate. Also vanilla: armor equip sounds, the Huntsman's Crossbow (vanilla crossbow
  code), fire extinguish of smothered braziers, the cage trapdoor, the arena fog wall and boss summon.
- **Misleading vanilla layers removed.** Where a moment played two vanilla sounds and the second one's subtitle named
  another mob ("Blaze breathes", "Warden emerges", "Blaze dies", "Fireball whooshes"), the moment now plays its own
  event only: Ash Colossus shedding stone, Crypt Keeper's lantern shattering, Smoldering Lord's armor shattering,
  Flask Ember.
- **New sounds where a moment had none:** the Smoldering Lord's mortar impact and his fog and darkness hazards (the
  ash storm one already roared); Reliquary opening and an accessory going into a slot; block ambience (ember crack,
  smouldering log, wet crypt stone drips).
- **Bosses' ambient, hurt, death and step** are new (they used the generic hostile sounds before).
- **Bark Hollow stays silent while dormant** (its creak would give away the ambush); it creaks once awake.
- **Music placeholders:** zone bosses — Lena Raine's "So Below" and "Rubedo" and C418's "Dead Voxel" (nether
  tracks, one picked per fight); Smoldering Lord — C418's "Boss" (the dragon fight). Streamed, looped by
  `BossMusicClient`.

## Not heard

All of it: pitches, volumes, how far each sound carries, whether windup cues read in a fight, music loudness
against the effects.
