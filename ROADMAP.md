# Embervoid — Roadmap

What the mod is: [DESIGN.md](DESIGN.md). What is built and how content plugs in: [CORE.md](CORE.md). Task breakdown of the next milestones: [PLAN.md](PLAN.md).

Current version: **0.4.1**. Each release includes everything before it. 0.2.0 and 0.3.0 were never published on their
own: 0.4.0 contains them (superseded), and their leftover release tasks ship in 0.4.1.
Milestones are ordered by dependency. Releases group milestones so that every release is a playable loop.
Every user-facing string ships in both `en_us` and `ru_ru`.

| Release | Theme | Milestones | Playable loop |
|---|---|---|---|
| **0.2.0** "Kindling" | Harder overworld, RPG core, the hub | 1–5 | Survive a harder world, find the Ember Sanctum, level up at the Maiden |
| **0.3.0** "Moon Nights" | Realm and boss frameworks, moon nights in the overworld | 6–8 | Survive the full moon and the blood new moon; enter a placeholder realm, beat test bosses, open the gate |
| **0.4.0** "Ashen Reach" | The first obelisk: all of Realm 1 | 9–13 | Enter Realm 1, take the three zones in any order, gear up, go through the gate, kill the Smoldering Lord |
| **0.4.1** | Leftovers of 0.2.0 and 0.3.0 | — | The same loop with Sanctum weapons, final overworld and moon-night art, sounds and a checked dedicated server |
| **0.5.0** | The 0.4.1 playtest: hub in its own dimension, weapons and rebalance, visual overhaul | 14–21 | Clear an overworld dungeon, take the Crying Soul from its boss, kindle a Cleft Stele and reach the hub; boss fights of about 2 minutes, Realm 1 weapons above netherite, animated mobs and bosses, a Citadel behind a fog wall |
| Later | Realms 2–4 and other ideas | — | [BACKLOG.md](BACKLOG.md); each realm gets a design pass first |

Realm 1 was split into 0.3.0 / 0.3.5 while its design was missing. With the design decided (DESIGN §4.5) it ships
whole in 0.4.0; the old split is dropped.

Open questions in [DESIGN.md](DESIGN.md) §10 name the milestone they block. Decide them before that milestone
is done; until then its tasks build the recommended answer and keep it easy to change.

**Where things stand:** milestones 1–13 are built and on `main`; milestones 1–7 were partly checked in game (single
player), everything since only by GameTests and the dedicated-server check. What was not checked in game is marked
"(not verified in game: …)" on its line. 0.4.0 is released locally (tag `v0.4.0`, not pushed). The leftovers of 0.2.0
and 0.3.0 are done as 0.4.1 (PLAN.md "Leftovers of 0.2.0 and 0.3.0"); publishing waits for the user.
The user's own playthrough of 0.4.1 (2026-10-23) set the next release, 0.5.0 (PLAN.md "Playtest 0.4.1"); its state
and how the work is run: COORDINATION.md "Tracks — 0.5.0". 0.5.0 is done on `main` (version, changelog, docs, all
checks green; not seen in game); its tag and push wait for the user.

## Decisions

Full list with status in [DESIGN.md](DESIGN.md). The ones that shape the roadmap:

| Topic | Decision |
|---|---|
| Structure | Hub (Ember Sanctum) in the overworld (in its own dimension from 0.5.0, reached through a Cleft Stele) → obelisks → realm dimensions with 4 zones each; final zone locked behind the 3 zone bosses (a fog wall inside the realm from 0.5.0); realm N+1 locked behind realm N |
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

Required runtime mods: Fabric API, GeckoLib (from milestone 4), playerAnimator (from 0.5.0, player attack animations).

---

# 0.2.0 "Kindling"

## 1. Building blocks

**Status:** Done, partly unverified (not verified in game block by block: drops itself, right tool, creative-tab names; the blocks render and are placed fine inside the Sanctum).

**Goal:** the block palette that structures are built from (used by the Sanctum and Realm 1).

- [x] Ash stone family: ash stone, cobbled, polished, chiseled, plus stairs, slabs and walls
- [x] Ash bricks and cracked ash bricks, plus stairs, slabs and walls
- [x] Ash soil
- [x] Dead wood set: log, stripped log, planks, stairs, slab, fence, fence gate, door, trapdoor
- [x] Ember lantern (animated texture, light source)
- [x] Datagen: models, blockstates, loot tables, tags; creative tab; en_us and ru_ru names

**Done when:** every block is placeable, drops itself, mines with the right tool, and shows up translated in the Embervoid creative tab. `./gradlew build` and datagen run clean.

## 2. Core player mechanics

**Status:** Done. Checked in game in single player; on a dedicated server by the server check's bot player (PLAN L6). The stamina bar and the debug weapon are covered by GameTests (L5), their look not seen.

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

**Status:** Done. Checked in game in single player; stats sync and leveling on a dedicated server by the server check's bot player (PLAN L6).

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

**Status:** Done. Final script-made models and textures since 0.4.1 (PLAN L4, checked in the offline preview only); the Hollow's wind-up, dodge and guard by GameTests (L5).

**Goal:** the new hostiles that push the player to find the Sanctum.

- [x] Add GeckoLib (`build.gradle`, `fabric.mod.json` `depends`)
- [x] GeckoLib pipeline (model, animations, renderer) proven with the **Ash Ghoul**: night/underground packs, blinding ash cloud on death
- [x] **Hollow**: armed undead, blocks, slow and hard-hitting, rich XP. Rare at night; main guard of the Sanctum (wind-up, dodge and guard checked by GameTests; sounds not heard)
- [x] **Ember Wisp**: cave flyer below y=0, only under cover, sets things on fire, flees light
- [x] Spawn rules, loot tables, levels per §5.4 of the design
- [x] Bestiary details per mob go into DESIGN.md before implementing

**Done when:** all three spawn where intended at sane rates, animate, fight as designed, and drop loot. A night in the open with iron gear is dangerous but survivable.

## 5. Ember Sanctum

**Status:** Done, partly unverified. The Sanctum weapons (DESIGN §10.10) came in 0.4.1.

**Goal:** the hub. The 0.2.0 loop is complete here.

- [x] Sanctum structure (jigsaw, size 4): surface ruin on a foundation, descent through a rock-cased shaft sealed from caves, outer halls with a light Hollow guard and loot, the core room. Overworld spacing ~48 / separation ~16 in the `structure_set` file (datapack-overridable) (the fix that turns water in the shaft casing's gaps into rock, QA #7f, checked by a GameTest in 0.4.1)
- [x] Core: no hostile spawns, unbreakable core blocks
- [x] **Ashen Maiden** NPC: invulnerable, no despawn, GeckoLib model (placeholder model and texture)
- [x] **Level-up screen**: stats, derived values, next-point preview, cost and current XP; buys one point per click through the server
- [x] **Ember Shrine** block: rest → refill flasks; a Flask Shard offered at the shrine adds a charge (realm respawn hooks came with milestone 6)
- [x] **Void Obelisks**: ring of obelisk blocks with dark / lit / kindled states from per-player progress. The Realm 1 obelisk now leads into Realm 1 (milestone 6)
- [x] Attunement on first reaching a core; per-player progression data (attuned, unlocked realms, beaten bosses)
- [x] **Sanctum Map** injected into vanilla chest loot
- [x] Sanctum outer-hall loot: flask shards, vanilla loot
- [x] 1–2 weapons with a special attack in the outer-hall loot so END matters: Sanctum Longsword and Ashbearer's Mace (0.4.1, PLAN L1; DESIGN §10.10 built as recommended)

**Done when:** in a fresh survival world you can find a Sanctum (by exploring or a map), fight through it, reach the Maiden, and spend XP on stats. A second Sanctum recognizes you as attuned.

## 0.2.0 release

**Status:** Superseded. 0.2.0 was never published on its own; 0.4.0 contains it, and its leftover tasks were done in
0.4.1 (PLAN.md "Leftovers of 0.2.0 and 0.3.0").

- [x] Balance pass on mob levels, damage factors and the cost curve (a fresh playthrough to the Sanctum) — modelled and checked in the engine, `docs/balance-overworld.md` (PLAN L2); no number needed changing
- [x] Sounds: each mob, block and item gets its sounds in its own milestone; this pass only fills gaps — 0.4.1 (PLAN L3, not heard)
- [x] Dedicated-server test: stats sync, leveling, remnants — the server check's bot player (PLAN L6)
- [x] README (English): features, required mods, config reference (PLAN L8)
- [x] ~~Bump to 0.2.0, changelog, Modrinth / CurseForge pages, release build via GitHub Actions~~ — superseded by 0.4.0 / 0.4.1
- [x] Art pass: Ash Ghoul, Hollow, Ember Wisp and Ashen Maiden — script-made final models with `tools/overworld_mob_art.py` (PLAN L4, checked in the offline preview, not seen in game)

---

# 0.3.0 "Moon Nights"

Everything outside the obelisks: the technology every realm uses (proven with placeholder zones and a test boss)
and the moon nights in the overworld (DESIGN §3.3). All Realm 1 content is 0.4.0.
Milestones 6 and 7 were built on the recommended answers to DESIGN §10.1 (building in realms) and §10.8 (arena
shrines).

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

**Status:** Done, partly unverified. Proven with a test boss (zombie model); real bosses come with milestones 12–13.

- [x] Boss bar with level, fog wall arena, reset on leave/death, phase thresholds, no despawn
- [x] Summon rule: boss appears when a player who has not beaten it enters; credit to everyone inside (not verified in game: summon and credit with a second player, the fog wall pushing back a player outside)
- [x] First-kill unique drop (*Ember Heart*) as personal loot for each credited player, lesser loot on repeat kills (not verified in game: a second player's personal Ember Heart)
- [x] Telegraph helpers (windup animation + sound + particles), boss music playback/stop (not verified in game: sounds and music; vanilla placeholders for now)
- [x] Arena Ember Shrine per DESIGN §10.8 (built as recommended: outside the fog wall, dormant for players who have not beaten the boss; since 0.4.1 it also looks cold to them, PLAN L7)

**Done when:** a test boss can be fought, reset, beaten, re-fought by a second player, and leaves a shrine.

## 8. Moon nights

**Status:** Done, checked by GameTests and the server check (PLAN 8.13). Final script-made models since 0.4.1 (PLAN L4). Not seen in game: red sky, models, animations, boss moves. DESIGN §10.12 built as recommended.

- [x] World moon-night state: every third lunar cycle, first full moon skipped, no sleeping, 30-minute fallback, config
- [x] Full moon: +3 levels, longer follow range, Curse of the Night stacks, **Ashen Zombie** and **Ghost**
- [x] Blood new moon: red sky sync, vampirism on vanilla hostiles, friendly **Vampires**, Vampire boss in its own arena dimension

## 0.3.0 release

**Status:** Superseded. 0.3.0 was never published on its own; 0.4.0 contains it, and its leftovers were done in 0.4.0 and
0.4.1.

- [x] Sealed Gate opens onto a placeholder ("the depths are still forming"; `realm.depthsOpen`)
- [x] Balance pass on the moon nights (levels, curse, Ghosts, Vampire boss) — done in the 0.4.0 balance pass
- [x] Multiplayer and performance test (realm worldgen time, entity counts) — done in 0.4.0 (R40.4)
- [x] Art pass: Ashen Zombie, Ghost, Vampire and the Vampire boss — 0.4.1 (PLAN L4)
- [x] ~~Release 0.3.0 (version bumped, jar built; publishing waits for the user)~~ — superseded by 0.4.0 / 0.4.1 (PLAN R30.4)

---

# 0.4.0 "Ashen Reach" — the first obelisk

The full Realm 1 per [DESIGN.md](DESIGN.md) §4.5. Design is decided; DESIGN §10.11 (rematch item) is built as
recommended. Tasks and tracks: PLAN.md "0.4.0", COORDINATION.md. Order: zones A–C, roads and the three
arenas first, the Citadel last. New mobs and bosses start on script-generated greybox models at final size; art
comes in the release pass.

## 9. Realm 1: world

**Status:** Done (PLAN 9.1–9.9; GameTests + server check; looks not seen in game).

- [x] Zones renamed: **Ashen Fields**, **Royal Woods**, **Sunken Necropolis** (ids stay `zone_a/b/c`); eternal dusk (`fixed_time` ≈ 13500)
- [x] Zone-aware terrain density function (plain / hills and ravines / sinkhole basin), blended at borders
- [x] Per-zone fog density (client mixin on `BackgroundRenderer`, reused for arena fog)
- [x] New blocks (~6–8): burnt straw, milestone, gravestone, ember crack, smouldering log, wet crypt stone…
- [x] Ores: Cinder Ore (Fields), Amber Vein (Woods), Grave Iron Ore (Necropolis), each only in its zone
- [x] Landing as the crossroads plaza; roads to each arena with milestones and 2 outposts per road
- [x] Minor structures per zone (~20–25 `.nbt` pieces) and the Necropolis crypt jigsaw (5–7 pieces); water only in the Necropolis
- [x] World edge: broken walls and collapsed bridges at the abyss cliffs

## 10. Realm 1: zone bestiary

**Status:** Done (PLAN 10.1–10.7; GameTests; greybox models, not seen in game).

- [x] Zone spawners list only Realm 1 mobs (placeholder zombies go); placed enemies at worldgen that never respawn
- [x] Fields: **Cinder Husk**, **Ash Stalker** (buried ambush), **Ashen Carrion** (diving flyer)
- [x] Woods: **Withered Thrall** (incl. hanging cages), **Lantern Hag** (fire flask), **Bark Hollow** (dormant tree)
- [x] Necropolis: **Restless Bones** (bone pile, re-forms, light keeps it dead), **Crypt Moth** (seeks light, snuffs torches), **Hollow Sentinel** (tower shield, bash window), **Grave Warden** (bell-ringer)
- [x] Drops: basics + material and set-piece rolls per §4.5

## 11. Realm 1: loot and accessories

**Status:** Done (PLAN 11.1–11.13; GameTests; textures and screens not seen in game).

- [x] Materials: Cinder Shard, Royal Amber, Grave Iron, **Smoldering Ingot**, Smoldering Template
- [x] **Smoldering Plate** (smithing table, like netherite) with its set bonus
- [x] Mob-drop sets **Reaper's Garb**, **Huntsman's Leathers**, **Necropolis Warden** with set bonuses; unique pieces Scarecrow's Mask, Huntsman's Greaves, Drowned Sabatons, Furnace Heart Cuirass
- [x] Weapons: **Ashen Scythe**, **Huntsman's Crossbow**, **Warden's Scourge**, **Royal Ash Rapier** (recipes from boss souls + chest drops); server raycast for reach
- [x] **Bleeding** placeholder status effect and the `embervoid:bleed_immune` tag
- [x] **Reliquary**: own screen (button + keybind), 2 artifact + 2 ring slots, player attachment, accessories into the Ash Remnant on death
- [x] 8 artifacts and 6 rings
- [x] Consumables: Ash Bomb, Smoldering Resin, Amber Balm, Amber Lantern Oil, Whetstone, Flask Ember
- [x] Chest loot tables: ordinary, treasure and arena-side chests

## 12. Realm 1: zone bosses

**Status:** Done (PLAN 12.1–12.7; GameTests; greybox models, moves not seen in game).

- [x] Boss framework additions: arena hazard layer (restored on reset), S2C arena fog, telegraph shapes (ring, line, cone, falling shadow), shield-breaking hit, fight-bound adds, non-HP phase, HP scaling HP ×(1 + 0.5·(n − 1)) + 1 add per extra fighter
- [x] **Crypt Keeper** (first: riskiest) in the flooded ossuary: light, rising water, braziers, moths, Restless Bones
- [x] **Ash Colossus** in the crater market: smashable pillars, ash storm, exposed core
- [x] **Grieving Tree** in the Royal Grove: lanterns, arena fog, uproot phase
- [x] Per-boss souls (first-kill personal drop, 25 % when helping), Flask Shard per zone boss
- [x] Rematch item (*Ash of Rematch*) at a beaten boss's arena shrine
- [x] Shared zone-boss music theme

## 13. Realm 1: Smoldering Citadel

**Status:** Done (PLAN 13.1–13.5; GameTests + server check; greybox models, looks not seen in game).

- [x] Depths dimension "Burning City": own `dimension_type` (dark red sky, dusk), hill terrain, ember cracks and smouldering logs
- [x] Citadel jigsaw (~10–12 pieces): outskirts → burning streets → wall and gatehouse → inner ring and towers → courtyard → keep
- [x] Mobs: **Cinder Hound**, **Sparking Marksman**, **Furnace Golem**, **Smoldering Knight**
- [x] Realm boss **Smoldering Lord**: wall phase with signal braziers, greatsword phase, replayed zone hazards; own music theme
- [x] Realm completion → Realm 2 obelisk lit (leads nowhere yet)

## 0.4.0 release

**Status:** Done on `main` except publishing (tag `v0.4.0` + push; CI builds the jar). Nothing was played in game.

- [x] Balance: zone level bands vs expected player level, boss difficulty, XP yield, loot rates — `docs/balance-0.4.0.md`
- [x] Art pass: realm mobs and bosses from greybox to final models; textures consistency (script-generated, checked with `tools/model_preview.py`; not seen in game)
- [x] Sounds and music (`CREDITS.md` for licensed tracks) — own sound events on vanilla files; two CC BY boss themes
- [x] Multiplayer and performance test (realm worldgen time, entity counts, placed enemies) — `docs/perf-0.4.0.md`
- [ ] Release 0.4.0 (version, changelog and docs done, all checks green; publishing: push the `v0.4.0` tag, CI builds the jar — waits for the user)

**Done when:** a fresh survival playthrough goes overworld → Sanctum → Realm 1 → Smoldering Lord without creative mode, feels fair, and breaks nothing on client or server.

---

# 0.4.1 — leftovers of 0.2.0 and 0.3.0

Tasks L1–L9 in PLAN.md "Leftovers of 0.2.0 and 0.3.0"; decisions for the user D1–D13 there.

**Status:** Done on the coordinator branch, merged into `main` by the 0.4.0 coordinator; publishing (tag `v0.4.1`
after `v0.4.0`, CI builds the jar) waits for the user. Nothing was played in game.

- [x] Sanctum weapons with special attacks in the outer-hall loot (L1)
- [x] Overworld balance pass, modelled (L2)
- [x] Sound gaps (L3), art for the eight placeholder entities (L4)
- [x] Headless checks for the in-game checks never run (L5), dedicated-server player check (L6)
- [x] Arena shrines look cold per player (L7), README (L8)
- [x] Version 0.4.1, changelog, docs (L9)

---

# 0.5.0 — the 0.4.1 playtest

The user's own playthrough of 0.4.1 (2026-10-23, verdict 5/10, "looks cheap"), shipped as one release. Tasks:
PLAN.md "Playtest 0.4.1", milestones 14–21. Order: the hub first, then weapons and the rebalance in one pass, then the
visuals; the quick fixes fit in anywhere. Milestones 15, 17 and 21 start with a design pass with the user.

## 14. Quick fixes

**Status:** Done (PLAN 14.1–14.2; JUnit, GameTests, server check; not seen or heard in game).

- [x] "Boss music" slider in the Music & Sounds screen, independent of Music (default 100 %)
- [x] Roads in Realm 1 that stand out from the ash: contrasting paving, light along them at dusk, denser milestones

## 15. Hub dimension and the Cleft Stele

**Status:** Done (PLAN 15.0–15.5; not seen in game; the old-world path not run on a real 0.4.x save).

- [x] Design: hub, stele, dungeon boss (**Fallen Sister**), old worlds — DESIGN §1, §2, §10.13
- [x] Hub dimension with the Ashen Maiden, the obelisk ring and an Ember Shrine; realms return to it
- [x] **Cleft Stele**: a 5 × 6 slab frame, kindled by the Crying Soul, one active stele per world, stele ⇄ hub
- [x] Worlds from 0.4.x keep working (old cores, old player data; GameTests)

## 16. The overworld dungeon

**Status:** Done (PLAN 16.1–16.4; not seen in game).

- [x] The overworld Sanctum becomes a dungeon: outer halls, guards and loot stay, the core gives way to a boss arena
- [x] 2–3 variants, about one per 300–400 blocks; the Sanctum Map leads to the nearest
- [x] Dungeon boss **Fallen Sister**, drops the **Crying Soul**
- [x] Overworld balance re-run

## 17. Weapons above netherite

**Status:** Built (PLAN 17.0–17.4 done; feel not played, the in-hand models seen only in the offline preview).

- [x] Design: loot ladder with Realm 1 uniques about 30 % above netherite, weapon identities, target fight lengths — DESIGN §5.5, §6, §6.1
- [x] Player attack animations (new dependency: playerAnimator)
- [x] Bigger in-hand weapon models — 3D element models for the five melee weapons and the Scourge's handle (`tools/weapon_model_art.py`, `InHandModels`); every weapon also has its signature in the air: swing trails, the mod's own particles, the Scourge's crack, the Scythe's souls and ash wave (the visual overhaul, 2026-10-24)
- [x] New weapon stats and identities
- [x] **Warden's Scourge** as a real whip: segmented, hits along an arc, the tip cracks

## 18. Full rebalance

**Status:** Done (PLAN 18.1, 18.3, 18.4; every 0.5.0 target green in the build; not played).

- [x] Balance model on fight length (bosses about 1.5–2.5 min with dodging, about 20–35 clean hits)
- [x] Retune Realm 1 mobs, bosses and the Grieving Tree's cadence
- [x] Retune overworld, dungeon and moon nights (the Vampire boss's cadence)

## 19. Mobs and bosses that look the part

**Status:** Tooling, effects and the boss remodels done (PLAN 19.1, 19.2, 19.4; 19.3 for the bosses); the mobs' animations and remodels done (the overhaul's wave 2); nothing seen in game.

- [x] Offline viewer for structures and animation motion (`tools/model_preview.py` extended)
- [x] Combat effects: camera shake, swing trails, hit flashes
- [x] Death, hit-reaction and stagger animations for all 24 animated mobs — the six bosses and the 21 mobs (the overhaul's wave 2, remodels to 30–80 cubes with them)
- [x] Boss remodels with secondary motion, phase and death animations (the Smoldering Lord's face included) — 71–128 cubes each, eased moves, `EmberHaloLayer`; seen only in the offline preview
- [ ] Optional: a dev photo tour the user starts in their own terminal

## 20. Props and moving set pieces

**Status:** Done (PLAN 20.1–20.4; the overhaul rebuilt every structure from 25 new blocks and placed fourteen moving set pieces); nothing seen in game.

- [x] Animated set pieces (GeckoLib block entities, capped): windmill sails, hoists, bells, cages, portcullis gears, bellows, chain lifts, censers, smoke — fourteen pieces, arenas react to the fight (`block.setpiece`, `ArenaEvents`)
- [x] 14 3D props with matching hitboxes
- [x] Props in the structures, more piece variants — every builder rebuilt from the wider palette with cornices, pitched roofs, arches, buttresses, grilles and decay gradients (docs/art-direction.md §3)

## Visual overhaul (the night of 2026-10-24)

**Status:** Done except the look in game; the bar is [docs/art-direction.md](docs/art-direction.md).

- [x] Art direction: hue-shifted palettes, light from the top-left, structural detail, dense models, motion everywhere
- [x] Every block texture redrawn, 25 new building blocks, every structure rebuilt (track `art-blocks`)
- [x] The mod's own particles (ember, ash, soot, sparks, blood, amber, cold flame, souls, shockwave, scorch, burst)
- [x] Weapon signatures: trails per weapon kind, the Scourge's crack and heat, the Scythe's souls and ash wave, the Rapier's ramp glow, the Crossbow's ember bolts, Wide Cleave and Ground Slam
- [x] Fog walls as volumes (arena wall and Citadel border): scrolling ember cloud, light streaks, ground ring, embers, flares, a hum
- [x] Six boss remodels (track `bosses`), fourteen moving set pieces and arenas that move (track `setpieces`)
- [x] 21 mob remodels with hurt / death / stagger (wave 2: `mobs`, `overworld-mobs`)
- [ ] A look in game (nobody plays; the photo tour 19.5 is still optional)

## 21. The Citadel behind a fog wall

**Status:** Done (PLAN 21.0–21.4; the city not seen in game, the upgrade not run on a real 0.4.x save).

- [x] Design: the final zone as the realm's fourth sector behind the Citadel Gate — DESIGN §4.1, §4.5
- [x] Four sectors (final zone 60°, radius 1300), the Citadel Gate at the plaza
- [x] The Citadel generated in its sector; the depths dimension removed
- [x] Fog wall: look and server lock (no pearls, chorus, elytra or riptide past it); the look redone as a volume in the overhaul (`FogWallDraw`)
- [x] Old worlds' realm dimensions regenerated (progress kept)
- [x] The Citadel pulled in: the city starts about 100 blocks past the gate, no wastes before it (PLAN 21.6; realm layout 3)

## 0.5.0 release

**Status:** Done locally (PLAN 21.5; tag, push and the CI jar wait for the user).

- [x] Release 0.5.0: version, changelog, README, CORE, DESIGN; all checks green (tag and push wait for the user, CI builds the jar)

**Done when:** a fresh survival playthrough goes overworld → dungeon → Cleft Stele → hub → Realm 1 → Smoldering Lord
without creative mode, boss fights take about two minutes, and nothing breaks on client or server.

---

## Later (ideas, not committed)

First-round variants and where each discussion stopped: [BACKLOG.md](BACKLOG.md).

- Realms 2–4 (*Mire of Rot*, *Cathedral Ward*, *The Void*), each with its own design pass
- Respec consumable, Maiden as merchant
- Weapon upgrades / smithing within a tier
- Status effects system for 0.6.0 (Bleeding stays the Realm 1 placeholder until then); rings and artifacts across the mod
