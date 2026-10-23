# Embervoid — Core

What the mod already does, as built on `main`, and how new content plugs into it. This is the reference for
writing content: every new mob, boss, zone, weapon or structure is described against the systems below.

- What the mod should become and why: [DESIGN.md](DESIGN.md). Build order: [ROADMAP.md](ROADMAP.md).
- Numbers here are the current defaults. Most live in `config/embervoid.json` (README has the full key list).
- "Placeholder" means it works in game but its model, texture or layout is temporary.

---

## 1. The loop as it plays today

1. A harder overworld: every hostile mob has a level, Easy hits like vanilla Normal, three new hostiles.
2. The player finds an **Ember Sanctum** (by exploring or a Sanctum Map), fights through its outer halls, steps
   into the core and is **attuned**. Realm 1 unlocks.
3. In the core: the **Ashen Maiden** sells stat points for vanilla XP points; the **Ember Shrine** heals and
   refills the flask; the lit **Void Obelisk** sends the player to **Ashen Reach** (Realm 1).
4. In the realm: three zones, one boss arena each. Beating all three opens the **Sealed Gate** to the depths and
   the realm boss. Beating it completes the realm and kindles its obelisk in the Sanctum.
5. Death anywhere leaves an **Ash Remnant** with the player's XP and items; in a realm the player respawns at
   the last shrine rested at in that dimension.

Realm 1 (**Ashen Reach**, 0.4.0) is complete: three zones with their own mobs, ruins and ores, three zone bosses,
the Smoldering Citadel and the Smoldering Lord. Beating him lights the Realm 2 obelisk, which leads nowhere yet.
Details per system below; design and numbers in DESIGN §4.5 and `docs/balance-0.4.0.md`.

---

## 2. Player systems

### 2.1 Player data
Per-player state lives in Fabric data attachments, saved with the player, copied on death, synced to the owning
client. Each system owns its own attachment:

| Attachment | Owner | Holds |
|---|---|---|
| `PlayerData` (`player.PlayerDataManager`) | player | stamina, flask charges and upgrades, first-flask flag, current remnant id, stats |
| `PlayerProgress` (`progress.ProgressManager`) | progress | attuned, unlocked / completed realms, beaten bosses |
| `RealmPlayerData` (`realm.RealmPlayerData`) | realms | Sanctum return point, rested shrine per realm dimension |

New per-player state for content goes into a new attachment in its own package, not into `PlayerData`.

### 2.2 Stamina and special attacks
- Pool: `stamina.max` 100, regenerates `stamina.regenPerTick` 1.0 per tick after `stamina.regenDelayTicks` 20.
  Endurance raises both. Bar above the hotbar, hidden when full (client option `staminaBar.hideWhenFull`).
- Stamina is spent **only** by right-click special attacks. Vanilla attacks, sprinting and blocking stay free.
- Contract (DESIGN §8.1): right-click with the weapon; fixed stamina cost; usable while any stamina is left;
  vanilla item cooldown; tooltip "Right-click: <name>" + cost and cooldown; server-side, hits through normal
  `damage()` so armor, mob levels and weapon scaling apply.
- **To add an ability:** implement `item.ability.SpecialAbility` (`id`, `staminaCost`, `cooldownTicks`,
  `perform`), call `SpecialAbilities.use` from the weapon's `use` and `SpecialAbilities.appendTooltip` from its
  tooltip. Lang key `embervoid.ability.<id>`. Example: `KnockbackSweepAbility` on the dev-only `debug_sweep`.
- Weapons with abilities: the Sanctum Longsword and Ashbearer's Mace (`sanctum.gear`, Sanctum loot, DESIGN §10.10) and
  the Realm 1 weapons (`reach.gear`). Weapon requirements (DESIGN §8) are open.

### 2.3 Healing Flask
- One flask given on first join. Charges live on the player, not the item: a second flask adds nothing.
- `flask.baseCharges` 3, cap `flask.maxCharges` 8; one sip heals `flask.healAmount` 8 HP over `flask.drinkTicks`
  32 ticks; can't drink while sprinting. Kindling raises the heal.
- Refilled by resting at an Ember Shrine, by sleeping through the night, and on respawn.
- Never dropped and never put into a remnant: it stays in its slot through death.
- **Flask Shard** (`embervoid:flask_shard`): used on a shrine, +1 max charge (filled at once), up to the cap.
  Found in Sanctum outer-hall chests.
- Recipe for a lost flask: glass bottle + diamond + coal/charcoal (shapeless).

### 2.4 Ash Remnant
- On death the player's XP points (exact, level and bar) and items, except flasks, go into an invulnerable
  remnant at the last solid ground the player stood on (never in lava or the void). Chat gives the coordinates.
- `remnant.mode`: `xp_and_items` (default) / `xp` / `off`. Respects `keepInventory`.
- Dying again: `remnant.secondDeath` `burn_xp` (default; the old remnant loses its XP, items stay) or `burn_all`.
  A stale remnant in an unloaded chunk resolves itself when the chunk loads.
- After a teleport the safe spot resets; a death high in the air lands the remnant on the ground below.

### 2.5 Stats and leveling
Seven stats, all start at 10. **Level = 1 + (sum − 70)**, so a fresh player is level 1 with vanilla numbers.

| Stat | Effect per full point above 10 (default) |
|---|---|
| VIT Vitality / Живучесть | +1 max HP |
| END Endurance / Выносливость | +2 max stamina, +2 % regen, +0.005 knockback resistance |
| STR Strength / Сила | +2 % damage with STR weapons (axes; tridents half) |
| DEX Dexterity / Ловкость | +2 % damage with DEX weapons (swords, bows, crossbows; tridents half), +0.5 % attack speed |
| KIN Kindling / Жар | +2 % flask heal (ember damage comes with ember weapons) |
| RES Resilience / Стойкость | +0.1 armor toughness, −1 % fire damage (cap 50 %) |
| FOR Fortune / Удача | +0.1 luck, +0.5 % chance a mob you kill drops its loot again (cap 25 %) |

- Soft caps: points 11–20 count fully, 21–40 at half, 41–99 at a fifth. Stat cap 99 (`leveling.statCap`),
  level cap off (`leveling.levelCap` 0, DESIGN §10.2 open).
- Cost of level L → L+1 in XP points: `20 + 6·L + 0.15·L²` (26 at level 1, 200 at 20, 695 at 50).
- Paid at the Ashen Maiden: level-up screen with all stats, a now → next preview, cost and current XP; one point
  per click, validated on the server.
- Weapon scaling is applied when damage is dealt, by item type (`stats.WeaponScaling`); arrows carry the
  shooter's scaling. Content weapons get their scaling through this (requirements and per-weapon scaling are
  part of the weapons doc, not built).
- **API for content:** `stats.PlayerStats` (values, derived numbers, `levelUp`), `stats.Stat` enum.

---

## 3. Hostile world

### 3.1 Mob levels
Every `Monster` (vanilla and modded) gets a level once, when it first enters the world; stored on the entity.

- Default level = base by type + bonuses. Base: zombie, husk, drowned, spider 3 · skeleton, stray, slime 4 ·
  Ash Ghoul 5 · creeper, witch 6 · Ember Wisp 8 · enderman 12 · Hollow 15 · anything else 4.
  Bonuses: Nether +5, End +8; overworld only: +1 per 1000 blocks from spawn (max +5), +3 below y=0.
- Per level above 1: HP +6 %, damage +5 % (melee, projectiles, explosions, potions), XP +6 %.
- Excluded (`#embervoid:unleveled`): Ender Dragon, Wither, Warden, Elder Guardian.
- Shown as "Name Lv. N" above the mob under the crosshair (client option `hud.showMobLevel`).
- Config: `mobs.levels.*` (base levels per entity id, bonuses, per-level factors; modded ids can be added).
- **API for content:** `mob.MobLevels` — `registerProvider((mob, world) -> OptionalInt)` decides levels at spawn
  (used by realm zone bands), `setLevel(entity, level)` (used for bosses), `getLevel`, `spawnLevel`.
  A new mob gets a level automatically; give it a base level in `MobConfig` defaults.

### 3.2 Difficulty
Difficulty-scaled damage from mobs to players: Easy ×1.0, Normal ×1.25, Hard ×1.75 (vanilla: ×0.5+1 / ×1 /
×1.5), before level scaling. Config `mobs.difficulty.*`.

### 3.3 Overworld mobs

| Mob | Base Lv | Stats (level 1) | Behaviour | Spawns | Drops |
|---|---|---|---|---|---|
| **Ash Ghoul** | 5 | 12 HP, 2.5 dmg, fast (0.33) | Packs of 3–5; one targeting a player pulls in ghouls within 16 blocks; on death an ash cloud (r 2.5) gives Blindness 3 s; burns in daylight | Overworld night surface and dark caves, weight 12 | 0–2 bones, 0–1 charcoal, 5 XP |
| **Hollow** | 15 | 30 HP, armor 6, slow (0.2), damage from its iron/stone sword only | 8-tick telegraphed swing, one attack per 2 s; raises a guard (front melee −80 %, front arrows deflected); an axe hit breaks the guard and staggers it 3 s; never burns | Overworld surface at night under open sky, rare (weight 5); guards in the Sanctum | 1–4 iron nuggets, 0–2 bones, 10 % iron ingot, 20 XP |
| **Ember Wisp** | 8 | 8 HP flyer, 1 dmg + 4 s fire | Dives at players within 16 blocks; lights fires nearby (mobGriefing); flees and drops its target at block light ≥ 7 except from its own fire — torches counter it | Below y=0, block light 0, under cover, weight 15, groups 1–2 | 0–1 glowstone dust, 5 % blaze powder, 6 XP |

**To add a mob:** entity class in `entity/mob` (copy `AshGhoulEntity` for a GeckoLib mob), register type,
attributes, spawn egg and spawn rules in `MobEntities`, renderer in `client/mob/MobRenderers`, loot table in
`client/mob/MobDataGen`, assets `geo/entity/<id>.geo.json`, `animations/entity/<id>.animation.json`,
`textures/entity/<id>.png` (+ `_glowmask.png` for glowing parts), lang `entity.embervoid.<id>` in en_us and
ru_ru, base level in `MobConfig`, sound events in a sound class of its package (`entity.mob.MobSounds`). Models and
textures of the three are made by `tools/overworld_mob_art.py` (on `tools/art_kit.py`; check with
`tools/model_preview.py`); `tools/mob_placeholders.py` keeps the helpers for a greybox of a new mob.

---

### 3.4 Moon nights (overworld)
Design: DESIGN §3.3, §10.12. Models by `tools/overworld_mob_art.py` (checked in the offline preview only). Config
sections `moon`, `nightMobs`, `vampire`.

- **Schedule** (`moon.MoonSchedule`): lunar cycle `c = day / 8`; events in cycles `c ≥ 1` with
  `c % moon.cycleInterval == 0` (3): full moon on day 24, blood new moon on day 28, then every 24 days. Dusk to
  dawn; no sleeping from 12000 on an event day. Title + chat at start and end. Fallback `moon.fallbackMinutes` 30.
- **API** (`moon.api`): `MoonNights.current(server)`, events `NIGHT_STARTED` / `NIGHT_ENDED(reason)`,
  `hold` / `release(server, id)`: while a hold is open the clock stops just before dawn; the last release brings
  dawn (`GOAL_MET`). `CurseOfTheNight.stacks(player)`. A new night goal takes a hold on `NIGHT_STARTED` and
  releases it when met.
- **Full moon:** overworld hostiles spawn `moon.fullMoon.levelBonus` +3 levels, follow range ×1.5, keep their
  target in range. **Curse of the Night**: a death to a hostile mob (or its projectile, or an `ArenaBoss`) adds a
  stack, anywhere; reset when a full moon ends. Waves (`nightmob.NightWaves`): per overworld player
  `min(stacks, 10)` **Ashen Zombies** (spot level + 5, don't burn, 20 XP) or, with no curse, 3 **Ghosts** (no-clip
  flyers, 2× zombie HP/damage, 15 % of hits miss, 40 XP; clearing them gives a Flask Shard), 16–32 blocks away.
  Each wave holds the night (`embervoid:wave/<uuid>`); a player away from the overworld gets it on return.
- **Blood new moon:** red sky, fog and moon (client, overworld only). Overworld hostiles heal 25 % of damage
  dealt to players (`moon.blood.vampirism`, not `ArenaBoss`). 1–3 friendly **Vampires** per cluster of players
  (64 blocks); the first hit turns one (others vanish), else the nearest turns after 5 min. Everyone in the
  overworld outside a Sanctum core is pulled into `embervoid:vampire_arena` (void arena rebuilt each opening).
  **Vampire boss** (`vampire.VampireBossEntity`, an `ArenaBoss` run by `vampire.VampireFight`, no fog wall): level
  = highest participant, ≥ 15, ≤ top band of the highest unlocked realm (20 with none). Personal loot: first kill
  2 Flask Shards + Vampire Fang, repeat 50 % a shard. A fighter who dies stays out; their remnant moves to their
  return point when the arena closes. Kill → back after 10 s, dawn.

## 4. Ember Sanctum (hub)

- Overworld structure, any land biome, `structure_set` spacing 48 / separation 16 (about one per 750 blocks;
  change it with a datapack). Jigsaw depth 4.
- **Layout:** surface ruin on a foundation → spiral stairwell in a sealed shaft (rock-cased where caves expose
  it) → gate hall → 0–several side halls (corridor, chamber, crypt) → core. No world water inside any piece.
- **Guards:** gate hall always 1 Hollow + 1 skeleton; each side-hall spot has a Hollow at
  `sanctum.hallGuardChance` 0.6 and chambers an archer at `sanctum.hallArcherChance` 0.3, rolled at worldgen.
  Typical Sanctum: 2–5 Hollows, 1 skeleton, 3–5 chests.
- **Loot:** `embervoid:chests/sanctum_outer_hall` — vanilla loot, Flask Shards and, at 6 % each per chest (never
  both), a Sanctum weapon: the **Sanctum Longsword** (DEX; Wide Cleave, a 90° arc) or the **Ashbearer's Mace** (STR;
  breaks a Hollow's guard; Ground Slam, a ring with Slowness). Numbers in config section `sanctumGear`;
  `docs/balance-overworld.md` places them between iron and diamond and below the realm weapons.
- **Core:** no hostile spawns (spawn-proof Sanctum Flagstone floor); shrine, obelisks and floor unbreakable.
  First step onto the floor attunes the player ("Attuned") and unlocks Realm 1; any later Sanctum greets them.
- **Ashen Maiden:** invulnerable NPC facing the entrance; right-click opens the level-up screen (closes beyond
  8 blocks). GeckoLib, script-made model (`tools/overworld_mob_art.py`).
- **Ember Shrine** (`embervoid:ember_shrine`): rest = full health + flask refill; in a realm it also sets the
  respawn point. Used by the Sanctum core, realm landings and arenas.
- **Void Obelisks:** four in the core, for `embervoid:realm_1`…`realm_4`, drawn per player as dark ("The flame
  has not reached it"), lit (teleports: click = last rested shrine of that realm if any, sneak = landing) or
  kindled (realm completed). Only Realm 1 exists.
- **Sanctum Map:** explorer map with a red X, injected into cartographer, temple, ruined portal and shipwreck
  chests (`sanctum.mapChance*`).
- Templates are generated from Java by datagen (`sanctum/datagen/SanctumTemplates`), not built in game.

---

## 5. Realms

### 5.1 Ashen Reach (Realm 1)
- Dimension `embervoid:ashen_reach`, entered from the Sanctum obelisk. Bounded: terrain ends in cliffs at
  ~1200 blocks; past ~1230 the void pushes survival players back; falling in is a void death.
- **Landing** at 0,0: Ember Shrine in the centre, Return Obelisk north (back to the Sanctum the player came
  from), Sealed Gate south. Mob level 15. Protected from building.
- **Zones:** three 120° sectors around the landing with noise-warped borders, decided by the biome source
  `embervoid:realm_zones`: **Ashen Fields** (zone_a, Lv 15–18, low plain), **Royal Woods** (zone_b, 18–21, hills and
  ravines), **Sunken Necropolis** (zone_c, 21–25, sinkhole basin, the only water), terrain from the density function
  `embervoid:zone_blend`; eternal dusk (`fixed_time` 13500) and per-zone fog (`client.realm.RealmFog`, source
  `embervoid:zone`). Each zone has its own ore (`#embervoid:realm_ores`) and blocks (`reach.world.ReachBlocks`).
- **Roads and ruins:** ash-brick roads from the crossroads plaza to each arena (`reach.world.RoadFeature`,
  milestone every 100 blocks, toll gate and watchtower outposts); 21 minor structures (`<zone>/<name>` templates,
  placement `embervoid:realm_scatter`, kept off roads and arenas); the Necropolis crypt jigsaw; the broken border
  wall and fallen bridges at the abyss (`embervoid:realm_edge`). Debug: `/embervoid realm roads | edge`.
- **Placed enemies:** marker block `embervoid:placed_enemy` in templates, processor `embervoid:placed_enemies`
  (`realm.placed.PlacedEnemies`): spawned once at worldgen, persistent, never respawn, command tag
  `embervoid.placed` (+ the marker's tags, e.g. `embervoid.placed.cage`).
- **Arenas:** one per zone, 400–900 blocks out, inside its sector, placed by seed (`/locate structure
  embervoid:ashen_reach_arena_a|b|c`). The arena JSON's `site` (`ArenaSite`) shapes the ground; the arena itself
  is a template (`embervoid:ashen_reach/arena_fields|woods|necropolis`, track reach-bosses): crater market, Royal
  Grove, sinkhole ossuary, each with its Ember Shrine outside the fog wall and an arena-side chest. Protected.
- **Sealed Gate:** opens per player after all three zone bosses → `embervoid:ashen_reach_depths` (own dimension type
  `embervoid:ashen_reach_depths`, red dusk sky, radius ~400, levels 25–30): the **Smoldering Citadel**
  (`reach.citadel`, fixed layout `CitadelLayout`, 11 template pieces from the outskirts to the keep, placed Citadel
  mobs, treasury in the keep) and the Smoldering Lord's courtyard at (0, −192). Beating the Lord completes Realm 1
  and unlocks the next realm in `ProgressManager.REALM_ORDER` (its obelisk lights; entering says the realm is still
  forming).
  `realm.depthsOpen` (default true since 0.4.0, false in 0.3.0): when off, an unsealed gate only says the depths are still
  forming; `/embervoid realm` commands still reach them.
- **Ember Lodestar** (`embervoid:ember_lodestar`): compass needle to the nearest unbeaten arena, right-click
  gives the distance. Cold outside realms.
- **Respawn:** death in a realm dimension → last shrine rested at in that same dimension, else its landing.
  The overworld bed spawn is untouched; beds in realms don't set spawn. Building and mining are allowed outside
  landings and arenas.

### 5.2 Where realm content goes
- **Realm definition** (`realm.Realms`): ids of the realm, its two dimensions, zone boss ids, realm boss id,
  level band per zone / landing / depths, radii. A new realm is a new `RealmDefinition` plus its datapack.
- **Datapack** (`data/embervoid/`): `dimension/`, `dimension_type/realm.json`, `worldgen/noise_settings/`,
  `worldgen/biome/ashen_reach_*.json` (look, particles, mob spawners), `worldgen/structure/` +
  `structure_set/` for landing and arenas.
- **Zone mobs:** natural spawns added in code (`reach.mob.ReachMobs`, `BiomeModifications`, config
  `reachMobs.spawns`); the biome JSON `spawners` stay empty. Levels come from the zone band automatically. Tanks and
  elites are only placed (placed enemies above). Shared base `reach.mob.ReachMob` (telegraphed moves, placed state).
- **Zone look:** biome colours, particles, ambient sound, surface blocks, zone features (trees, candles, straw).
- **Arena:** structure JSON `"feature": "arena"` with `"boss": {id, entity, level, radius}`.

---

## 6. Boss framework

- **A boss is an entity** that implements `boss.ArenaBoss` and calls `ArenaManager.tickBoss` from its server
  tick. It owns its moves; the framework owns the fight.
  - `phaseThresholds()` (e.g. `{0.5f}`) + `onPhaseChange(phase)`; `playAnimation(name)` for GeckoLib;
    `music()` → sound id played to the fighters.
- **Placed by the arena JSON** (`boss.ArenaSpec`): `id` (progress id, e.g. `embervoid:ashen_reach/zone_a`),
  `entity`, `level` (also sets its HP/damage through mob levels), `radius` of the fog wall.
- **The fight (`BossFight`):** a player who hasn't beaten that boss entering the arena summons it; boss bar
  "Name — Lv N"; a fog wall closes the arena (fighters can't leave, outsiders are pushed back); leaving or
  dying ends the fight and removes the boss, re-entering summons a fresh one; everyone inside at the kill gets
  credit.
- **Rewards:** first kill per player gives a personal **Ember Heart** (`embervoid:ember_heart`, tagged with the
  boss id) straight into the inventory, plus the boss's loot table and XP; repeat kills give loot and XP only.
  Arena shrine is dormant ("The flame does not answer you") until that player beats the boss, and that player's
  client draws it cold (`boss.ArenaShrines` sends the arena shrines near a player with their boss ids).
- **Telegraphs:** `boss.Telegraph.windup(caster, ticks, animation, sound, …)` and `ring(world, centre, radius,
  particle)` for readable windups and danger zones; `isWindingUp` to gate attacks.
- **Test boss** (`embervoid:test_boss`, zombie model): base 200 HP, 8 melee, 10 slam (scaled by level: at Lv 18
  that is 404 HP, ~37 melee); telegraphed slam with a flame ring; enrages at 50 %. Real bosses need base
  values chosen with level scaling in mind.

**To add a boss:** entity class implementing `ArenaBoss` (GeckoLib model as for mobs), register it in
`boss.BossEntities`, loot table, lang `entity.embervoid.<id>` and `boss.embervoid.<boss id path with dots>` for
the bar name, point the arena JSON's `boss.entity` at it, add its id to the realm's `RealmDefinition`.

---

## 7. Blocks and items

- **Building palette:** ash stone (plain, cobbled, polished, chiseled; stairs, slabs, walls), ash bricks and
  cracked ash bricks (stairs, slabs, walls), ash soil, dead wood set (log, stripped log, planks, stairs, slab,
  fence, gate, door, trapdoor), ember lantern (animated, light source).
- **Functional blocks:** Ember Shrine, Void Obelisk, Sanctum Flagstone, Return Obelisk, Sealed Gate.
- **Items:** Healing Flask, Flask Shard, Sanctum Map, Ember Lodestar, Ember Heart, the two Sanctum weapons, spawn
  eggs; dev-only `debug_sweep`. Realm 1 gear: §5 and DESIGN §4.5.
- **Sounds:** every mod sound is an own `embervoid:` event on vanilla files (`assets/embervoid/sounds.json`), one sound
  class per package registered from `registry.ModSounds`, subtitles in both languages; `SoundTests` and
  `OverworldSoundTests` check them. Boss music: `CREDITS.md`.

---

## 8. Conventions for content

- Every user-facing string in `en_us.json` and `ru_ru.json`.
- New content in its own package and registry class, one line in `Embervoid` / `EmbervoidClient`; items via
  `ModItems.register` so they show in the creative tab; datagen providers for models, loot and tags.
- Tunable numbers through a config section (`EmbervoidConfig.register(ConfigSection.of(...))`).
- Pure logic gets JUnit tests (`src/test`). Behaviour on real entities, players and worldgen gets a GameTest in
  `src/gametest/java/com/embervoid/gametest` (a class implementing `FabricGameTest`, listed under `fabric-gametest`
  in `src/gametest/resources/fabric.mod.json`; `TestPlayers` makes a survival server player). What has to be seen
  in game goes through the QA batch (COORDINATION.md).
- Never discard or spawn entities inside `ENTITY_LOAD` (it crashed the server once); decide at worldgen or
  defer to the end of the tick.

## 9. Debug commands (operator)

| Command | Does |
|---|---|
| `/embervoid stats`, `levelup <stat>`, `setstat <stat> <value>` | Stats and leveling |
| `/embervoid stamina [spend|set]`, `/embervoid data` | Stamina and player data |
| `/embervoid remnant` | Put your items and XP into a remnant here |
| `/embervoid moblevel <targets> [level]` | Show or set mob levels |
| `/embervoid progress <player> [attune|reset|unlock|complete|boss <id>]` | Progress flags |
| `/embervoid realm enter <realm> [toShrine] | leave | where | arenas | data` | Realm travel and layout |
| `/embervoid boss fights` | Running boss fights |
| `/embervoid moon status | start full|blood | stop | setcycle <n> | curse <player> <stacks>` | Moon nights |
| `/embervoid wave status | spawn <players> | clear <players>` | Full moon waves |
| `/embervoid vampire status | provoke` | Blood new moon Vampires and fight |

## 10. Placeholders and gaps (what content has to fill)

| Area | Now | Needed |
|---|---|---|
| Overworld, moon-night, Vampire and Maiden models | final script-made models (0.4.1), checked in the offline preview only | a look in game |
| Realm 1 art | final script-made models and textures, checked in the offline preview only | a look in game |
| Sounds | own sound events on vanilla files everywhere; boss music in `CREDITS.md` | a listen in game |
| Realms 2–4 | obelisks only (Realm 2 lights after Realm 1) | each realm's design pass |
