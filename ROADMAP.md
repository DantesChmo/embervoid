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
| Death penalty | Ash Remnant keeps XP and items; what dying again destroys is Open (DESIGN §10.7) |
| Progress | Per player. Realm dimensions are shared; bosses re-summon for players who have not beaten them |

Required runtime mods: Fabric API, GeckoLib (from milestone 4).

---

# 0.2.0 "Kindling"

## 1. Building blocks *(in progress, parallel session)*

**Goal:** the block palette that structures are built from (used by the Sanctum and Realm 1).

- [ ] Ash stone family: ash stone, cobbled, polished, chiseled, plus stairs, slabs and walls
- [ ] Ash bricks and cracked ash bricks, plus stairs, slabs and walls
- [ ] Ash soil
- [ ] Dead wood set: log, stripped log, planks, stairs, slab, fence, fence gate, door, trapdoor
- [ ] Ember lantern (animated texture, light source)
- [ ] Datagen: models, blockstates, loot tables, tags; creative tab; en_us and ru_ru names

**Done when:** every block is placeable, drops itself, mines with the right tool, and shows up translated in the Embervoid creative tab. `./gradlew build` and datagen run clean.

## 2. Core player mechanics

**Goal:** the systems everything else plugs into.

Blocked by: DESIGN §10.6 (flask charges), §10.7 (second death).

- [ ] Config files (`config/embervoid.json` for gameplay, `config/embervoid-client.json` for client preferences; hand-rolled, no config library)
- [ ] Persistent per-player data (Fabric Data Attachment API) + our own client sync, designed to also hold stats, level and progression flags
- [ ] XP points utility (points ↔ level + progress), used by the Remnant and leveling
- [ ] **Stamina**: server value, HUD bar, regen delay, consume API. Max/regen read from a hook that END will feed in milestone 3
- [ ] Special-attack contract written (weapons doc, DESIGN §8), then the **special-attack framework** + debug weapon
- [ ] **Healing Flask** with charges (storage per DESIGN §10.6); refill on sleep (shrine refill comes with milestone 5)
- [ ] **Ash Remnant**: XP and items kept in a remnant at the last safe position, second death per DESIGN §10.7, config modes, respects `keepInventory`
- [ ] Unit tests for the pure logic (config parsing, XP conversion)

**Done when:** in survival you can spend stamina through the debug weapon, drink and refill the flask, die, walk back and recover everything. Extra flasks do not add heals. Works on a dedicated server.

## 3. RPG core

**Goal:** stats, character level, mob levels, and the harder difficulty — all testable through commands before the hub exists.

- [ ] 7 stats (VIT, END, STR, DEX, KIN, RES, FOR) stored per player, all start at 10, level derived from their sum
Blocked by: DESIGN §10.9 (Nether/End bonuses, unlisted mobs), §10.10 (what stats do in 0.2.0).

- [ ] Stat effects as vanilla attribute modifiers with soft caps (HP, attack speed, armor toughness, knockback resistance, luck); END → stamina; KIN/RES/FOR effects in 0.2.0 per DESIGN §10.10. Health is preserved across relog when max HP is above 20
- [ ] Vanilla weapon scaling placeholder, applied when damage is dealt (not an `attack_damage` modifier): swords/bows DEX, axes STR, tridents STR+DEX
- [ ] Level cost curve (`20 + 6·L + 0.15·L²` XP points, coefficients in config); server-side `levelUp(player, stat)` validating and taking XP
- [ ] Debug commands: `/embervoid stats`, `/embervoid levelup <stat>`, `/embervoid setstat`
- [ ] **Mob levels**: level assigned once (base per type, default for unlisted hostiles, + distance/depth/dimension bonus), saved on the entity; vanilla bosses excluded; HP, damage (melee, projectiles, explosions, potions) and XP scale by level
- [ ] **Difficulty curve**: Easy/Normal/Hard damage factors replace vanilla's (Easy no longer halves damage)
- [ ] Mob level shown next to the name when looked at (client config toggle)
- [ ] Unit tests: soft-cap curve, cost curve against the DESIGN §5.3 table

**Done when:** a level 1 player with all stats at 10 has exactly vanilla numbers; leveling VIT through commands adds HP with the soft caps; zombies show their level and hit harder than vanilla on Easy; skeleton arrows and creeper explosions scale with level too; higher-level mobs drop more XP. Survives relog and death.

## 4. Overworld threats

**Goal:** the new hostiles that push the player to find the Sanctum.

- [ ] Add GeckoLib (`build.gradle`, `fabric.mod.json` `depends`)
- [ ] GeckoLib pipeline (model, animations, renderer) proven with the **Ash Ghoul**: night/underground packs, blinding ash cloud on death
- [ ] **Hollow**: armed undead, blocks, slow and hard-hitting, rich XP. Rare at night; main guard of the Sanctum
- [ ] **Ember Wisp**: cave flyer below y=0, sets things on fire, flees light
- [ ] Spawn rules, loot tables, levels per §5.4 of the design
- [ ] Bestiary details per mob go into DESIGN.md before implementing

**Done when:** all three spawn where intended at sane rates, animate, fight as designed, and drop loot. A night in the open with iron gear is dangerous but survivable.

## 5. Ember Sanctum

**Goal:** the hub. The 0.2.0 loop is complete here.

- [ ] Sanctum structure (jigsaw): surface ruin, descent, outer halls with Hollows and loot, the core room. Overworld spacing ~48 / separation ~16 in the `structure_set` file (datapack-overridable)
- [ ] Core: no hostile spawns, unbreakable core blocks
- [ ] **Ashen Maiden** NPC: invulnerable, no despawn, GeckoLib model
- [ ] **Level-up screen**: stats, derived values, next-point preview, cost and current XP; buys one point per click through the server
- [ ] **Ember Shrine** block: rest → refill flasks (realm respawn hooks come in milestone 6)
- [ ] **Void Obelisks**: ring of obelisk blocks with dark / lit / kindled states from per-player progress. Realm 1 obelisk lit but leads nowhere yet ("the way is still forming")
- [ ] Attunement on first reaching a core; per-player progression data (attuned, unlocked realms, beaten bosses)
- [ ] **Sanctum Map** injected into vanilla chest loot
- [ ] Sanctum outer-hall loot: flask shards, vanilla loot, and (if DESIGN §10.10 goes that way) 1–2 weapons with a special attack so END matters in 0.2.0

**Done when:** in a fresh survival world you can find a Sanctum (by exploring or a map), fight through it, reach the Maiden, and spend XP on stats. A second Sanctum recognizes you as attuned.

## 0.2.0 release

- [ ] Balance pass on mob levels, damage factors and the cost curve (a fresh playthrough to the Sanctum)
- [ ] Sounds: each mob, block and item gets its sounds in its own milestone; this pass only fills gaps
- [ ] Dedicated-server test: stats sync, leveling, remnants
- [ ] README (English): features, required mods, config reference
- [ ] Bump to 0.2.0, changelog, Modrinth / CurseForge pages, release build via GitHub Actions

---

# 0.3.0 "Ashen Reach"

Before this block: **weapons design doc** and **Realm 1 content doc** (DESIGN.md §8), and answers to
DESIGN §10.1 (building in realms, blocks milestone 6) and §10.8 (arena shrines, blocks milestone 7).

## 6. Realm framework

**Goal:** the technology for every realm, proven with placeholder zones.

- [ ] Realm dimension (datapack dimension, type, noise settings) + custom `BiomeSource` assigning 3 zones by noise-warped angle sectors around the landing; bounded radius with an abyss edge
- [ ] Final-zone dimension per realm
- [ ] Guaranteed per-zone boss arena placement (custom `StructurePlacement`)
- [ ] Landing platform: return obelisk, Ember Shrine, Sealed Gate
- [ ] Obelisk teleport Sanctum ↔ realm (landing or last shrine), return to the Sanctum the player came from
- [ ] Realm respawn: death in a realm respawns at the last rested shrine there; overworld bed spawn untouched
- [ ] Sealed Gate: opens for a player who has beaten the 3 zone bosses of that realm
- [ ] *Ember Lodestar*: points to the nearest unbeaten boss
- [ ] Mob level bands per zone

**Done when:** with placeholder biomes and dummy "bosses" (killable markers) you can do the full realm loop: enter, find 3 arenas, unlock the gate, reach the final zone, mark the realm complete, see the next obelisk light up. Two players on a server progress independently.

## 7. Boss framework

- [ ] Boss bar with level, fog wall arena, reset on leave/death, phase thresholds, no despawn
- [ ] Summon rule: boss appears when a player who has not beaten it enters; credit to everyone inside
- [ ] First-kill unique drop (*Ember Heart*) as personal loot for each credited player, lesser loot on repeat kills
- [ ] Telegraph helpers (windup animation + sound + particles), boss music playback/stop
- [ ] Arena Ember Shrine per DESIGN §10.8 (recommended: outside the fog wall, dormant for players who have not beaten the boss)

**Done when:** a test boss can be fought, reset, beaten, re-fought by a second player, and leaves a shrine.

## 8. Realm 1: zones and their bosses

Per the Realm 1 content doc. Starting point from the old plan:

- [ ] **Ash Wasteland** zone: ash soil surface, dead trees, boulders, falling-ash particles, ambient loop, ruins
- [ ] **Dead Woods** zone and its boss (new, from the content doc)
- [ ] **Sunken Crypt** zone: crypt jigsaw, **Hollow Sentinel**, **Crypt Moth**, boss **Crypt Keeper** (two phases: halberd and lantern fire → darkness and summoned dead)
- [ ] Ash Wasteland boss (new, from the content doc)
- [ ] Zone loot tables: Smoldering material, flask shards, Wanderer's Cloak pieces
- [ ] **Smoldering Ingot** tier: basic crafted weapons and armor (boss uniques and set bonuses come with milestone 10)

## 0.3.0 release

- [ ] Sealed Gate opens onto a placeholder ("the depths are still forming")
- [ ] Balance pass: zone level bands vs expected player level, zone bosses, XP yield
- [ ] Multiplayer and performance test (realm worldgen time, entity counts)
- [ ] Release 0.3.0

---

# 0.3.5 "Smoldering Citadel"

## 9. Realm 1: Smoldering Citadel

- [ ] Final zone dimension terrain + **Ash Fortress** jigsaw (walls, towers, courtyard, keep, throne arena)
- [ ] **Sparking Marksman** on the walls
- [ ] Realm boss **Smoldering Lord** (combos and lunge → cracked armor, fire zones)
- [ ] Realm completion → Realm 2 obelisk lit (leads nowhere until 0.4.0)

## 10. Realm 1 gear, polish and 0.3.5 release

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
