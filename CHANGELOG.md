# Changelog

## 0.5.1 — 2026-10-24

- **Fixed:** 0.5.0 broke the client's resources: the Crypt Keeper's lantern throw had its keyframes out of order, GeckoLib
  refused the file, the game dropped every resource pack and all text turned into boxes. Keyframes are back in time
  order in all 19 mob animations that had them shuffled (cloth, chains and hair no longer swing backwards).
- **Fixed:** the in-hand models of the Ashen Scythe, Royal Ash Rapier, Sanctum Longsword and Ashbearer's Mace reached past
  vanilla's model bounds and did not load; they are built smaller and drawn larger, so they look as before.

## 0.5.0 — 2026-10-24

What the playthrough of 0.4.1 asked for: a hub you can always reach, a dungeon worth clearing, weapons above netherite,
boss fights of about two minutes and a world that looks the part. Requires playerAnimator now (see README).

### The way in
- **Ruined Sanctum:** the overworld Sanctum is a dungeon now, about one per 300–400 blocks, in three variants (the
  courtyard ruin, a collapsed chapel, a sunken cloister). Its halls, guards and loot stay; an Ember Shrine waits at the
  boss door, and behind a fog wall, where the core was, the **Fallen Sister**: the Maiden's burnt-out twin with a
  censer on a chain, crying souls in her second phase. Each dungeon's boss falls once per world; every player in the
  fight gets a **Crying Soul**. The Sanctum Map leads to the nearest dungeon.
- **Cleft Stele:** a 5 × 6 slab of Cleft Stone (crafted from stone bricks and coal), kindled with the Crying Soul;
  a cold one stands in every dungeon arena. One stele burns per world; it leads to the hub.
- **Ember Sanctum, the hub:** its own dimension, an island under eternal dusk with the Ashen Maiden, an Ember Shrine
  and the ring of Void Obelisks. Realm 1 unlocks on the first visit; attunement is gone. Leaving a realm always lands
  in the hub; the hub's stele leads back to the world's lit stele (with none: where you last left the overworld,
  else your bed).

### Ashen Reach
- The realm has four sectors now: the Smoldering Citadel stands in the fourth, behind the **Citadel Gate**, a fog
  wall on the plaza that opens after the three zone bosses (no pearls, chorus, elytra or riptide past it). The
  separate depths dimension is gone, and with it `realm.depthsOpen`.
- Roads stand out from the ash: contrasting paving, lights along them at dusk, more milestones.

### Weapons and balance
- Realm 1 weapons about 30 % above netherite, each with its own identity; the **Warden's Scourge** is a real whip
  that hits along an arc and cracks at the tip. Player attack animations (playerAnimator) and 3D in-hand models.
- Everything retuned on fight length: bosses about 1.5–2.5 minutes with dodging (20–35 clean hits), mobs, the
  overworld, the dungeon and moon nights (the Vampire boss telegraphs its claws). Vanilla mob types get per-type
  `mobs.levels.healthScale` / `damageScale` factors. Targets in `docs/balance-0.5.0.md`, checked by the build.

### Look and sound
- A visual overhaul (`docs/art-direction.md`): every block texture redrawn, 25 new building blocks, every structure
  rebuilt; the mod's own particles; trails and signatures for every weapon; fog walls as glowing volumes.
- Six boss remodels and 21 mob remodels with hit, stagger and death animations; fourteen moving set pieces
  (windmill sails, hoists, bells, censers…) and 14 3D props; camera shake and hit flashes.
- A **Boss Music** slider in Music & Sounds, independent of Music.

### Old worlds
- Worlds from 0.4.x keep working: Sanctum cores already generated keep their Maiden and obelisks; the realm is
  generated again on the first start (progress stays, Ash Remnants left there are laid on the hub's landing, players
  who logged out inside join at the hub, old realm shrines are forgotten); old attunement flags and return points are
  ignored.

### Known
- Checked by GameTests and a dedicated server with a bot player only: nothing of 0.5.0 has been seen, heard or played
  on a client yet. The upgrade was not run on a real 0.4.x save.

## 0.4.1 — 2026-10-23

The loose ends of the first releases. 0.2.0 "Kindling" and 0.3.0 "Moon Nights" were never published on their own: 0.4.0
already contains them, and this version finishes what their release lists left open.

### Added
- **Sanctum weapons:** the **Sanctum Longsword** (Dexterity; right-click *Wide Cleave* hits everything in a wide arc in
  front) and the **Ashbearer's Mace** (Strength; breaks a Hollow's guard like an axe; right-click *Ground Slam* knocks
  back and slows everything around you). Found in the Sanctum's outer-hall chests, so Endurance and stamina matter
  before the first obelisk. They sit between iron and diamond and below the Ashen Reach weapons. Config section
  `sanctumGear`.
- **Sounds** for everything that still borrowed vanilla's: the Ash Ghoul, Hollow (now also raising its guard), Ember
  Wisp (and its dive), Ashen Zombie, Ghost, the Vampire and its boss form, the Ashen Maiden, the Ember Shrine, Flask
  Shards, attunement, level-up, obelisk travel and refusal, the Sealed Gate, the Healing Flask (drinking, empty, refilled
  after sleep), the Ash Remnant, running out of stamina and both Sanctum weapon specials. All with subtitles in both
  languages.

### Changed
- New models, textures and animations for the Ash Ghoul, Hollow, Ember Wisp, Ashen Maiden, Ashen Zombie, Ghost,
  Vampire and the Vampire boss, in the style of the Ashen Reach creatures.
- An arena's Ember Shrine looks cold (smoke and ash, no flame) to a player who has not beaten its boss yet, and lights
  up as soon as they have.
- README: required mods (GeckoLib), a feature list and the missing `mobs`, `sanctum` and `sanctumGear` config keys.

### Checked
- The overworld was balanced on paper and in the engine from a fresh level 1 player to the Sanctum
  (`docs/balance-overworld.md`); no number needed to change.
- A dedicated server with a joined player: stats sync, buying a point at the Maiden, death and recovering the Ash
  Remnant.

### Known
- Models and textures were checked only in an offline preview, sounds were not heard: nothing here was played on a
  client yet.

## 0.4.0 "Ashen Reach" — 2026-10-22

The first obelisk is open. **Ashen Reach** is a burnt kingdom under eternal dusk: three districts you can take in any
order, each with its own monsters, ruins, ore and boss, then the Sealed Gate to the burning capital and its king.
Numbers are in `config/embervoid.json` (sections `realm`, `reachMobs`, `gear`, `reliquary`, `boss`).

### The realm
- **Ashen Fields** (Lv 15–18), **Royal Woods** (18–21) and **Sunken Necropolis** (21–25), each with its own terrain,
  fog, blocks and ore: Cinder Ore, Amber Vein, Grave Iron Ore.
- The landing is the kingdom's crossroads: ash-brick roads with milestones lead to each zone's boss arena, past toll
  gates and watchtowers. Farmsteads, a windmill, the hunting lodge, mausoleums, chapels and a crypt beneath the
  Necropolis hold placed enemies (they never come back) and chests. The kingdom ends in broken walls and fallen
  bridges over the abyss.
- **Smoldering Citadel** behind the Sealed Gate: a burning city on a hill, from the outskirts through the gatehouse
  and the inner ring to the keep and its treasury.

### Enemies
- 14 new creatures: Cinder Husk, Ash Stalker, Ashen Carrion, Withered Thrall, Lantern Hag, Bark Hollow, Restless
  Bones, Crypt Moth, Hollow Sentinel, Grave Warden; in the Citadel Cinder Hound, Sparking Marksman, Furnace Golem,
  Smoldering Knight. Every attack is telegraphed and has its own sound.
- Four bosses whose arenas fight with them: the **Ash Colossus** (smashable pillars, ash storm, exposed core), the
  **Grieving Tree** (lanterns to shoot down, fog, uprooting), the **Crypt Keeper** (darkness, braziers, rising water)
  and the **Smoldering Lord**, who replays all three. Boss health and adds grow with the number of fighters.
- First kill of a boss: its soul and a Flask Shard (the Lord: a Flask Ember), personal for every fighter. An **Ash of
  Rematch** used at a beaten boss's shrine calls it back for loot and XP.
- Beating the Smoldering Lord completes the realm and lights the next obelisk (its realm is still forming).

### Gear
- Smoldering Ingot from all three ores; **Smoldering Plate** on the smithing table; three mob-drop armour sets with
  set bonuses and four unique pieces.
- Four weapons from boss souls: Ashen Scythe, Warden's Scourge (reach 6, Bleeding), Royal Ash Rapier, Huntsman's
  Crossbow. They take Sharpness and the other sword enchantments.
- **Bleeding**, consumables (Ash Bomb, Smoldering Resin, Amber Balm, Lantern Oil, Whetstone) and the Flask Ember.
- **Reliquary:** a screen next to the inventory (button or key R) with 2 artifact and 2 ring slots. 8 artifacts add
  stat points without raising your level; 6 rings with their own effects. Accessories follow you into the Ash Remnant.

### Changed
- The Sealed Gate opens into the depths again (`realm.depthsOpen` is on).
- Mobs give less extra XP per level (+6 % instead of +10 %); Realm 1 ends around level 36.
- Full moon: 3 Ghosts instead of 5, attacking half as often; a Ghost wave ends when its player dies.
- The Vampire boss hits a little softer; the level-up screen shows artifact points next to each stat.
- Dying in a realm puts you at your own shrine right away.

### Known
- Models and textures are made by scripts and were checked only in an offline preview; nothing here was played on a
  client yet. Boss music: see `CREDITS.md`.

## 0.3.0 "Moon Nights" — 2026-10-22

Every third lunar cycle the overworld gets two dangerous nights, four days apart: a full moon and a blood new
moon. Neither can be slept through. All numbers are in `config/embervoid.json` (sections `moon`, `nightMobs`,
`vampire`). Models of the new creatures are placeholders.

### Full moon
- Hostiles spawn 3 levels higher, follow you further and do not lose you while you stay in range.
- **Curse of the Night:** every death to a hostile mob adds a stack, wherever it happens. On the full moon, one
  **Ashen Zombie** per stack (up to 10) comes for you. With no curse, 5 **Ghosts** come instead: tougher, flying
  through walls, and some hits pass through them. Clearing a Ghost wave leaves a Flask Shard. The curse lifts when
  the full moon ends.
- The night does not end until your wave is beaten (or after 30 minutes).

### Blood new moon
- The sky turns red, and hostiles heal from the damage they deal to you.
- Friendly **Vampires** appear near players. Strike one and it becomes the **Vampire**, a boss that pulls everyone
  in the overworld into its own arena. First kill: a Vampire Fang and 2 Flask Shards; later kills: a chance of a
  Flask Shard. A player who dies there stays out, and their remnant waits at the spot they were taken from.

### Changed
- The Sealed Gate in Ashen Reach no longer opens into the depths: they are still forming (`realm.depthsOpen`).
