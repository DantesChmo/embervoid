# Embervoid — Roadmap

What the mod is: [DESIGN.md](DESIGN.md). Task breakdown of the next milestones: [PLAN.md](PLAN.md).

Current version: **0.1.0**.
Milestones are ordered by dependency. Releases group milestones so that every release is a playable loop.
Every user-facing string ships in both `en_us` and `ru_ru`.

| Release | Theme | Milestones | Playable loop |
|---|---|---|---|
| **0.2.0** "Kindling" | Harder overworld, RPG core, the hub | 1–5 | Survive a harder world, find the Ember Sanctum, level up at the Maiden |
| **0.3.0** "Ashen Reach" | Realm framework + Realm 1 zones | 6–8, Realm 1 material tier | Enter Realm 1, beat 3 zone bosses, gear up from the zones, open the gate ("the depths are still forming") |
| **0.3.5** "Smoldering Citadel" | Realm 1 final zone | 9–10 | Through the gate, take the Citadel, kill the Smoldering Lord, earn the boss uniques |
| 0.4.0+ | Realms 2–4 | one block per realm | Each realm: design pass → build → release |

Releases are kept small enough to ship: Realm 1 is four bosses, a fortress, around five new mobs and a gear
tier, waiting on two design docs, so it ships in two releases (split is **Proposed**).

Open questions in [DESIGN.md](DESIGN.md) §10 name the milestone they block. Decide them before that milestone
is done; until then its tasks build the recommended answer and keep it easy to change.

**Where things stand:** milestones 1–7 are built and on `main`; most of it was checked in game (single player).
What was not checked in game is marked "(not verified in game: …)" on its line. Not started: the 0.2.0 release
tasks, the art pass, and the Realm 1 content (milestones 8–10).

## Decisions

Full list with status in [DESIGN.md](DESIGN.md). The ones that shape the roadmap:

| Topic | Decision |
|---|---|
| Structure | Hub (Ember Sanctum) in the overworld → obelisks → realm dimensions with 4 zones each; final zone locked behind the 3 zone bosses; realm N+1 locked behind realm N |
| Classes | None. Everyone starts as the Deprived: level 1, all 7 stats at 10 |
| Leveling | At the Ashen Maiden, paid in vanilla XP points, growing cost per level |
| Difficulty | Every hostile mob has a level; vanilla mobs hit harder even on Easy; new overworld hostiles |
| Animated models | **GeckoLib** (required dependency) |
| Biomes | Realm dimensions with our own `BiomeSource`. **TerraBlender dropped** |
| Structures | Jigsaw + `.nbt` templates built in-game; custom placement for guaranteed boss arenas |
| Rest point | Ember Shrines in realms and the Sanctum (refill flask, set realm respawn). Beds in the overworld |
| Stamina | Spent **only** by right-click special attacks |
| Death penalty | Ash Remnant keeps XP and items; dying again burns the old remnant's XP, its items stay (built as recommended, DESIGN §10.7) |
| Progress | Per player. Realm dimensions are shared; bosses re-summon for players who have not beaten them |

Required runtime mods: Fabric API, GeckoLib (from milestone 4).

---

# 0.2.0 "Kindling"

## 1. Building blocks

**Status:** Done.

**Goal:** the block palette that structures are built from (used by the Sanctum and Realm 1).

- [x] Ash stone family: ash stone, cobbled, polished, chiseled, plus stairs, slabs and walls
- [x] Ash bricks and cracked ash bricks, plus stairs, slabs and walls
- [x] Ash soil
- [x] Dead wood set: log, stripped log, planks, stairs, slab, fence, fence gate, door, trapdoor
- [x] Ember lantern (animated texture, light source)
- [x] Datagen: models, blockstates, loot tables, tags; creative tab; en_us and ru_ru names

**Done when:** every block is placeable, drops itself, mines with the right tool, and shows up translated in the Embervoid creative tab. `./gradlew build` and datagen run clean.

## 2. Core player mechanics

**Status:** Done. Checked in game in single player; the dedicated-server run is in the 0.2.0 release list.

**Goal:** the systems everything else plugs into.

Was blocked by: DESIGN §10.6 (flask charges, Decided), §10.7 (second death, built as recommended).

- [x] Config files (`config/embervoid.json` for gameplay, `config/embervoid-client.json` for client preferences; hand-rolled, no config library)
- [x] Persistent per-player data (Fabric Data Attachment API) + our own client sync, designed to also hold stats, level and progression flags
- [x] XP points utility (points ↔ level + progress), used by the Remnant and leveling
- [x] **Stamina**: server value, HUD bar, regen delay, consume API. Max/regen read from a hook that END will feed in milestone 3
- [x] Special-attack contract written (weapons doc, DESIGN §8.1), then the **special-attack framework** + debug weapon
- [x] **Healing Flask** with charges (storage per DESIGN §10.6: player data); refill on sleep and on respawn; kept on death; crafting recipe for a lost flask
- [x] **Ash Remnant**: XP and items kept in a remnant at the last safe position, second death per DESIGN §10.7, config modes, respects `keepInventory`
- [x] Unit tests for the pure logic (config parsing, XP conversion)

**Done when:** in survival you can spend stamina through the debug weapon, drink and refill the flask, die, walk back and recover everything. Extra flasks do not add heals. Works on a dedicated server.

## 3. RPG core

**Status:** Done. Checked in game in single player; the dedicated-server run is in the 0.2.0 release list.

**Goal:** stats, character level, mob levels, and the harder difficulty — all testable through commands before the hub exists.

Was blocked by: DESIGN §10.9 (Nether/End bonuses, unlisted mobs) and §10.10 (what stats do in 0.2.0), both
built as recommended; FOR's extra mob-drop chance is Decided.

- [x] 7 stats (VIT, END, STR, DEX, KIN, RES, FOR) stored per player, all start at 10, level derived from their sum
- [x] Stat effects as vanilla attribute modifiers with soft caps (HP, attack speed, armor toughness, knockback resistance, luck); END → stamina; KIN/RES/FOR effects in 0.2.0 per DESIGN §10.10. Health is preserved across relog when max HP is above 20
- [x] Vanilla weapon scaling placeholder, applied when damage is dealt (not an `attack_damage` modifier): swords/bows DEX, axes STR, tridents STR+DEX
- [x] Level cost curve (`20 + 6·L + 0.15·L²` XP points, coefficients in config); server-side `levelUp(player, stat)` validating and taking XP
- [x] Debug commands: `/embervoid stats`, `/embervoid levelup <stat>`, `/embervoid setstat`
- [x] **Mob levels**: level assigned once (base per type, default for unlisted hostiles, + distance/depth/dimension bonus), saved on the entity; vanilla bosses excluded; HP, damage (melee, projectiles, explosions, potions) and XP scale by level
- [x] **Difficulty curve**: Easy/Normal/Hard damage factors replace vanilla's (Easy no longer halves damage)
- [x] Mob level shown next to the name when looked at (client config toggle)
- [x] Unit tests: soft-cap curve, cost curve against the DESIGN §5.3 table

**Done when:** a level 1 player with all stats at 10 has exactly vanilla numbers; leveling VIT through commands adds HP with the soft caps; zombies show their level and hit harder than vanilla on Easy; skeleton arrows and creeper explosions scale with level too; higher-level mobs drop more XP. Survives relog and death.

## 4. Overworld threats

**Status:** Done, partly unverified. Models, animations and textures are placeholders (art pass in the 0.2.0 release list).

**Goal:** the new hostiles that push the player to find the Sanctum.

- [x] Add GeckoLib (`build.gradle`, `fabric.mod.json` `depends`)
- [x] GeckoLib pipeline (model, animations, renderer) proven with the **Ash Ghoul**: night/underground packs, blinding ash cloud on death
- [x] **Hollow**: armed undead, blocks, slow and hard-hitting, rich XP. Rare at night; main guard of the Sanctum (not verified in game: wind-up timing and dodging it, guard block and guard break sounds)
- [x] **Ember Wisp**: cave flyer below y=0, only under cover, sets things on fire, flees light
- [x] Spawn rules, loot tables, levels per §5.4 of the design
- [x] Bestiary details per mob go into DESIGN.md before implementing

**Done when:** all three spawn where intended at sane rates, animate, fight as designed, and drop loot. A night in the open with iron gear is dangerous but survivable.

## 5. Ember Sanctum

**Status:** Done, partly unverified. The Sanctum weapons (DESIGN §10.10) are not built.

**Goal:** the hub. The 0.2.0 loop is complete here.

- [x] Sanctum structure (jigsaw, size 4): surface ruin on a foundation, descent through a rock-cased shaft sealed from caves, outer halls with a light Hollow guard and loot, the core room. Overworld spacing ~48 / separation ~16 in the `structure_set` file (datapack-overridable) (not verified in game: the fix that turns water in the shaft casing's gaps into rock, QA #7f, queued for the next batch)
- [x] Core: no hostile spawns, unbreakable core blocks
- [x] **Ashen Maiden** NPC: invulnerable, no despawn, GeckoLib model (placeholder model and texture)
- [x] **Level-up screen**: stats, derived values, next-point preview, cost and current XP; buys one point per click through the server
- [x] **Ember Shrine** block: rest → refill flasks; a Flask Shard offered at the shrine adds a charge (realm respawn hooks came with milestone 6)
- [x] **Void Obelisks**: ring of obelisk blocks with dark / lit / kindled states from per-player progress. The Realm 1 obelisk now leads into Realm 1 (milestone 6)
- [x] Attunement on first reaching a core; per-player progression data (attuned, unlocked realms, beaten bosses)
- [x] **Sanctum Map** injected into vanilla chest loot
- [x] Sanctum outer-hall loot: flask shards, vanilla loot
- [ ] 1–2 weapons with a special attack in the outer-hall loot so END matters in 0.2.0 (DESIGN §10.10, still open)

**Done when:** in a fresh survival world you can find a Sanctum (by exploring or a map), fight through it, reach the Maiden, and spend XP on stats. A second Sanctum recognizes you as attuned.

## 0.2.0 release

**Status:** Not started.

- [ ] Balance pass on mob levels, damage factors and the cost curve (a fresh playthrough to the Sanctum)
- [ ] Sounds: each mob, block and item gets its sounds in its own milestone; this pass only fills gaps
- [ ] Dedicated-server test: stats sync, leveling, remnants
- [ ] README (English): features, required mods, config reference
- [ ] Bump to 0.2.0, changelog, Modrinth / CurseForge pages, release build via GitHub Actions
- [ ] Art pass: Ash Ghoul, Hollow, Ember Wisp and Ashen Maiden models, animations and textures are generated placeholders (`tools/mob_placeholders.py`, `tools/maiden_placeholder.py`); redo them in Blockbench

---

# 0.3.0 "Ashen Reach"

Before this block: **weapons design doc** and **Realm 1 content doc** (DESIGN.md §8), and answers to
DESIGN §10.1 (building in realms, blocks milestone 6) and §10.8 (arena shrines, blocks milestone 7).
Milestones 6 and 7 were built ahead on placeholders with the recommended answers to §10.1 and §10.8; the two
design docs are still needed for milestones 8–10.

## 6. Realm framework

**Status:** Done, partly unverified (not verified in game: two or three players on one server progressing independently).

**Goal:** the technology for every realm, proven with placeholder zones.

- [x] Realm dimension (datapack dimension, type, noise settings) + custom `BiomeSource` assigning 3 zones by noise-warped angle sectors around the landing; bounded radius with an abyss edge
- [x] Final-zone dimension per realm
- [x] Guaranteed per-zone boss arena placement (custom `StructurePlacement`)
- [x] Landing platform: return obelisk, Ember Shrine, Sealed Gate
- [x] Obelisk teleport Sanctum ↔ realm (landing or last shrine), return to the Sanctum the player came from
- [x] Realm respawn: death in a realm respawns at the last rested shrine in that realm dimension (open zones and depths apart), else that dimension's landing; overworld bed spawn untouched
- [x] Sealed Gate: opens for a player who has beaten the 3 zone bosses of that realm
- [x] *Ember Lodestar*: points to the nearest unbeaten boss
- [x] Mob level bands per zone

**Done when:** with placeholder biomes and dummy "bosses" (killable markers) you can do the full realm loop: enter, find 3 arenas, unlock the gate, reach the final zone, mark the realm complete, see the next obelisk light up. Two players on a server progress independently.

## 7. Boss framework

**Status:** Done, partly unverified. Proven with a test boss (zombie model); real bosses come with milestones 8–9.

- [x] Boss bar with level, fog wall arena, reset on leave/death, phase thresholds, no despawn
- [x] Summon rule: boss appears when a player who has not beaten it enters; credit to everyone inside (not verified in game: summon and credit with a second player, the fog wall pushing back a player outside)
- [x] First-kill unique drop (*Ember Heart*) as personal loot for each credited player, lesser loot on repeat kills (not verified in game: a second player's personal Ember Heart)
- [x] Telegraph helpers (windup animation + sound + particles), boss music playback/stop (not verified in game: sounds and music; vanilla placeholders for now)
- [x] Arena Ember Shrine per DESIGN §10.8 (built as recommended: outside the fog wall, dormant for players who have not beaten the boss; it looks lit to everyone, only resting is refused)

**Done when:** a test boss can be fought, reset, beaten, re-fought by a second player, and leaves a shrine.

## 8. Realm 1: zones and their bosses

**Status:** Not started (needs the Realm 1 content doc).

Per the Realm 1 content doc. Starting point from the old plan:

- [ ] **Ash Wasteland** zone: ash soil surface, dead trees, boulders, falling-ash particles, ambient loop, ruins
- [ ] **Dead Woods** zone and its boss (new, from the content doc)
- [ ] **Sunken Crypt** zone: crypt jigsaw, **Hollow Sentinel**, **Crypt Moth**, boss **Crypt Keeper** (two phases: halberd and lantern fire → darkness and summoned dead)
- [ ] Ash Wasteland boss (new, from the content doc)
- [ ] Zone loot tables: Smoldering material, flask shards, Wanderer's Cloak pieces
- [ ] **Smoldering Ingot** tier: basic crafted weapons and armor (boss uniques and set bonuses come with milestone 10)

## 0.3.0 release

**Status:** Not started.

- [ ] Sealed Gate opens onto a placeholder ("the depths are still forming")
- [ ] Balance pass: zone level bands vs expected player level, zone bosses, XP yield
- [ ] Multiplayer and performance test (realm worldgen time, entity counts)
- [ ] Release 0.3.0

---

# 0.3.5 "Smoldering Citadel"

## 9. Realm 1: Smoldering Citadel

**Status:** Not started (needs the Realm 1 content doc).

- [ ] Final zone dimension terrain + **Ash Fortress** jigsaw (walls, towers, courtyard, keep, throne arena)
- [ ] **Sparking Marksman** on the walls
- [ ] Realm boss **Smoldering Lord** (combos and lunge → cracked armor, fire zones)
- [ ] Realm completion → Realm 2 obelisk lit (leads nowhere until 0.4.0)

## 10. Realm 1 gear, polish and 0.3.5 release

**Status:** Not started (needs the weapons doc).

- [ ] Full Smoldering tier (≈ diamond+) weapons and armor per the weapons doc (candidates from the old plan: Keeper's Halberd, Smoldering Greatsword, Ash Dagger, Ember Talisman; sets: Keeper's Garb, Smoldering Plate, Wanderer's Cloak)
- [ ] Boss uniques via Ember Hearts, full-set bonuses
- [ ] Balance: realm level bands vs expected player level, boss difficulty, XP yield per realm
- [ ] Sounds, music (`CREDITS.md` for licensed tracks), textures consistency pass
- [ ] Multiplayer and performance test (realm worldgen time, entity counts)
- [ ] Release 0.3.5

**Done when:** a fresh survival playthrough goes overworld → Sanctum → Realm 1 → Smoldering Lord without creative mode, feels fair, and breaks nothing on client or server.

---

## Later (ideas, not committed)

- Realms 2–4 (*Mire of Rot*, *Cathedral Ward*, *The Void*), each with its own design pass
- Ember Moon night event
- Respec consumable, Maiden as merchant
- Weapon upgrades / smithing within a tier
