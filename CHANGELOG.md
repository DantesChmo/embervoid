# Changelog

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
