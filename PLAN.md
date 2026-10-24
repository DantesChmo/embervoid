# Embervoid — Work Plan

[ROADMAP.md](ROADMAP.md) broken into tasks. Each task is sized for one working session, ends in a buildable,
testable state, and lists what it depends on. Task IDs are `<milestone>.<n>`.

Status: `todo` · `in progress` · `done` (built, on `main`, checked in game) · `done – not verified in game` · `blocked`
(waiting on an Open question in [DESIGN.md](DESIGN.md) §10)

In-game checks run in single player through the coordinator's QA queue (`.claude/qa-queue.md`, not in git);
"QA #n" below refers to its entries.

Rules that apply to every task:
- Every user-facing string goes into both `en_us.json` and `ru_ru.json`.
- `./gradlew build` passes. Run datagen when assets/data change.
- Logic lives on the server; the client only renders and sends input. Check behavior on a dedicated server (`runServer`) once a task has networking.
- Pure logic (formulas, conversions, soft caps, config parsing) gets JUnit tests in `src/test`. In-world behavior is checked by hand or with Fabric GameTest (`fabric-gametest-api-v1`) where it is cheap.
- A task that depends on an Open question starts with the recommended answer from DESIGN.md §10 unless the question was decided otherwise. Keep that part easy to change.

---

## Milestone 1 — Building blocks

Done (392fd7b). Tracked in ROADMAP.md, not split here.

## Milestone 2 — Core player mechanics

| ID  | Task | Depends on | Status |
|-----|------|------------|--------|
| 2.1 | **Config system** | — | done (QA #2) |
| 2.2 | **Persistent player data + sync** | — | done (QA #2, #5) |
| 2.3 | **XP points utility** | — | done (QA #2, #5) |
| 2.4 | **Stamina: server logic** | 2.1, 2.2 | done (QA #5: END → stamina max) |
| 2.5 | **Stamina: HUD bar** | 2.4 | done – not verified in game (not in the QA log) |
| 2.6 | **Special-attack framework + debug weapon** | 2.4, 2.5, special-attack contract | done – not verified in game (not in the QA log) |
| 2.7 | **Healing Flask item** | 2.1, 2.2, DESIGN §10.6 | done (QA #2, #2d) |
| 2.8 | **Flask refill on sleep** | 2.7 | done (QA #2) |
| 2.9 | **Ash Remnant: entity and storage** | 2.1, 2.2, 2.3 | done (QA #2) |
| 2.10 | **Ash Remnant: death flow** | 2.9, DESIGN §10.7 | done (QA #2, #2b–2d) |
| 2.11 | **Milestone 2 check on a dedicated server** | 2.1–2.10 | done in single player; dedicated server not run |

- **2.1 Config system.** `com.embervoid.config.EmbervoidConfig`: a hand-rolled JSON file read with Gson (bundled with Minecraft), with no config library. Defaults are written on first launch; missing keys are filled in, and invalid values are logged and replaced with defaults. Two files, because a client and a dedicated server each have their own:
  - `config/embervoid.json` — **gameplay** values, read by the server (the integrated server in single player). Sections and keys for the planned systems:
    - `remnant.mode` (`off` / `xp` / `xp_and_items`, default `xp_and_items`), `remnant.secondDeath` (see DESIGN §10.7)
    - `stamina.max`, `stamina.regenPerTick`, `stamina.regenDelayTicks`
    - `flask.baseCharges` (3), `flask.maxCharges`, `flask.healAmount`, `flask.drinkTicks`
    - later sections added by their milestones: `leveling.*` (cost coefficients), `mobs.*` (level bonuses, difficulty factors)
  - `config/embervoid-client.json` — **client preferences** only (e.g. `hud.showMobLevel`, added in 3.7). Never read by the server.

  Structure spacing is **not** in the config: it lives in the `structure_set` JSON, which datapacks can already override (DESIGN §9).
  Loaded in `Embervoid.onInitialize` / `EmbervoidClient.onInitializeClient`; static access `EmbervoidConfig.get()`. Values the client must agree with (stamina max) get synced in 2.2/2.4, not here.
  *Done when:* first launch writes readable files with defaults; editing a value and restarting changes it; a broken file logs a warning and the game still starts. JUnit: missing keys filled, invalid values replaced.
- **2.2 Persistent player data + sync.** Per-player state (stamina, flask charges and upgrades, remnant id; leave room for stats, level and progression flags from milestones 3 and 5). Use the Fabric Data Attachment API (`fabric-data-attachment-api-v1` 1.0.2 ships in 0.92.12): persistent attachment with a codec, `copyOnDeath` where the value should survive death. That version has **no built-in sync**, so write our own S2C packet. Send it on join, on respawn, on dimension change and on every change. The client entity is recreated on respawn and dimension change, so the client keeps its copy in a client-side holder, not on `ClientPlayerEntity`.
  *Done when:* a test value survives relog, death and dimension change, and the client sees it after each of them.
- **2.3 XP points utility.** `XpUtil`: total XP points from `experienceLevel` + `experienceProgress`, and setting a player's total points exactly (recomputing level and progress). Vanilla `totalExperience` is **not** reliable (enchanting and anvils lower levels without touching it), and `addExperience(-n)` drifts on floats, so the Remnant (2.9) and leveling (3.4) both go through this.
  *Done when:* JUnit round-trips points ↔ level/progress for levels 0–200; spending points through it after enchanting leaves the exact expected total.
- **2.4 Stamina: server logic.** `StaminaManager`: current/max, `tryConsume(player, amount)`, regen after a delay, synced to the client. Max and regen come from the config through a hook that END will feed in 3.2.
  *Done when:* a debug command spends stamina, which then regens as configured.
- **2.5 Stamina: HUD bar.** Bar above the hotbar, hidden when full, flashes on a failed spend. Programmatic texture.
  *Done when:* the bar reflects the server value in single player and on a server.
- **2.6 Special-attack framework + debug weapon.** Before starting: write the **special-attack contract** section of the weapons doc (DESIGN §8): how an ability is triggered, what it costs, cooldown model, how it shows in the tooltip, what it may not do. The framework implements that contract: interface for right-click abilities (stamina cost, cooldown, tooltip line), plus a dev-only test item that does a simple knockback sweep. Right-click on air needs a C2S packet; right-click on a block goes through `useOnBlock`.
  *Done when:* right-click spends stamina, fails with feedback (HUD flash + sound) when empty, respects cooldown.
- **2.7 Healing Flask item.** Depends on DESIGN §10.6 (where charges live). Recommended answer: charges and upgrades live in player data, the item only shows and spends them, so extra flasks give nothing; one flask is given on first join (and can be crafted again if lost). Drink-use animation, heal over `flask.drinkTicks`, cannot drink while sprinting, charge count in tooltip and item bar (read from synced player data). Texture with full/empty variants.
  *Done when:* drinking heals and spends a charge; with no charges the flask does nothing; a second flask in the inventory does not add charges.
- **2.8 Flask refill on sleep.** The player's charges are refilled when the night was actually skipped while they were in bed (`EntitySleepEvents.STOP_SLEEPING` + time check), including when other players' sleep skipped it.
  *Done when:* sleeping through the night refills; lying down and getting up early does not.
- **2.9 Ash Remnant: entity and storage.** Invulnerable, non-moving entity holding an inventory snapshot and XP points, with particles. Right-click by its owner returns everything (overflow drops on the ground, XP through `XpUtil`). Immune to `/kill`-style damage, explosions and the void; persists across chunk unload and restart.
  Each remnant has an id; the player data holds the id of their current remnant. A remnant that loads and finds it is no longer its owner's current one deals with itself per `remnant.secondDeath` (it cannot be destroyed while its chunk is unloaded).
  *Done when:* a remnant spawned by command holds items and gives them back after a restart; a remnant marked stale while unloaded resolves itself when its chunk loads.
- **2.10 Ash Remnant: death flow.** Depends on DESIGN §10.7 (what a second death does). On death: skip if `keepInventory`; take items before vanilla drops them and XP before vanilla drops orbs; place the remnant per `remnant.mode` at a safe position. The safe position is the **last position on solid ground** tracked while alive (updated every few ticks), not a search around the death point — this covers lava, the void and suffocation. Items with Curse of Vanishing are not saved. A new death applies `remnant.secondDeath` to the previous remnant (with a message). Chat message with coordinates and dimension.
  *Done when:* the full die → walk back → recover loop works; a second death handles the first remnant as configured; dying in lava or the void still leaves a reachable remnant; dying in another dimension works.
- **2.11 Milestone 2 check.** Run through the milestone's "done when" on `runServer` with a client, and fix what breaks.

## Milestone 3 — RPG core

Design: [DESIGN.md](DESIGN.md) §3.1 and §5.

| ID  | Task | Depends on | Status |
|-----|------|------------|--------|
| 3.1 | **Stat storage** | 2.2 | done (QA #5) |
| 3.2 | **Stat effects** | 3.1, 2.4, DESIGN §10.10 | done (QA #5) |
| 3.3 | **Weapon scaling placeholder** | 3.2 | done (QA #5) |
| 3.4 | **Leveling + debug commands** | 3.1, 2.1, 2.3 | done (QA #5) |
| 3.5 | **Mob levels** | 2.1, DESIGN §10.9 | done (QA #6, #8b) |
| 3.6 | **Difficulty factors** | 3.5 | done (QA #6) |
| 3.7 | **Level display** | 3.5 | done (QA #6) |
| 3.8 | **Milestone 3 check on a dedicated server** | 3.1–3.7 | done in single player; dedicated server not run |

- **3.1 Stat storage.** 7 stats in player data, all start at 10, character level derived from their sum, copied on death, synced.
  *Done when:* `/embervoid setstat` changes survive relog and death, and the client sees them.
- **3.2 Stat effects.** Soft-capped stat curve (DESIGN §5.2) as one pure function with JUnit tests. Vanilla attribute modifiers with fixed UUIDs: VIT → `max_health`, DEX → `attack_speed`, END → `knockback_resistance` + stamina max/regen (hook from 2.4), RES → `armor_toughness`, FOR → `luck`. STR/DEX/KIN damage bonuses are **not** an `attack_damage` modifier (3.3 does them at damage time). Whatever DESIGN §10.10 settles for KIN/RES/FOR in 0.2.0 goes here too (recommended: KIN → flask healing, RES → fire damage reduction).
  Recomputed on join, respawn and stat change. Max-health pitfall: on load vanilla clamps health to the max *before* our modifiers apply — remember the saved health and restore it after applying; on respawn set health to the new max.
  *Done when:* all stats at 10 give exactly vanilla attributes; VIT 20 → 30 HP, VIT 40 → 40 HP; a player at 35/40 HP relogs with 35/40.
- **3.3 Weapon scaling placeholder.** STR/DEX bonus by weapon type (swords DEX, axes STR, tridents STR+DEX, bows DEX), applied when damage is dealt: melee through an attack hook, arrows through the projectile's owner and the bow it was shot from. No bonus with an empty hand or a non-weapon.
  *Done when:* the same stat point raises sword damage with DEX and axe damage with STR, but not the other way round; bow arrows scale with DEX.
- **3.4 Leveling + debug commands.** Cost curve from config (pure function, JUnit-tested against the table in DESIGN §5.3). Server `levelUp(player, stat)`: checks the stat cap and XP points (via `XpUtil`), takes the points, raises the stat. Debug commands `/embervoid stats`, `/embervoid levelup <stat>`, `/embervoid setstat <stat> <value>` (op only).
  *Done when:* levelling up takes exactly the table cost; it fails cleanly with too little XP or at the cap.
- **3.5 Mob levels.** Level assigned once per entity: on `ENTITY_LOAD` if the entity has no level yet (a "leveled" NBT flag, since the event fires on every chunk load). Level = base by type (default for unlisted hostiles, including modded) + distance/depth/dimension bonus per DESIGN §5.4 and §10.9. Vanilla bosses (Ender Dragon, Wither, Warden, Elder Guardian) are excluded. HP through a fixed-UUID attribute modifier (spawn at full HP); damage at damage time so it covers melee, projectiles, explosions and potions (the attacker or the projectile's owner decides); XP through a hook on the dropped XP.
  *Done when:* a zombie far from spawn and below y=0 has the expected level after a restart and chunk reload; a high-level skeleton's arrows and creeper's explosion hit harder; XP scales.
- **3.6 Difficulty factors.** Replace vanilla's Easy/Normal/Hard scaling for difficulty-scaled damage from hostile mobs: hook where `PlayerEntity.damage` applies it, so ours replaces vanilla's instead of stacking.
  *Done when:* a level-1-equivalent zombie hits for vanilla-Normal damage on Easy, and the Normal/Hard factors match the table.
- **3.7 Level display.** Level next to the name of the looked-at mob. The level reaches the client through a packet sent when a player starts tracking the entity (`EntityTrackingEvents.START_TRACKING`), not through a `DataTracker` entry added to vanilla mobs. Client toggle `hud.showMobLevel`.
  *Done when:* levels show in single player and on a server, and the toggle hides them.
- **3.8 Milestone 3 check.** Run through the milestone's "done when" on `runServer` with a client, and fix what breaks.

## Milestones 4–7

Split into tasks in the track plans, which keep each task's choices and notes. All merged into `main`.

| Milestone | Tasks | Status |
|---|---|---|
| 4 — Overworld threats | [plans/track-mobs.md](plans/track-mobs.md) M1–M4 | done (QA #1–#1c, #3–#3d); Hollow wind-up/dodge and guard sounds not verified in game |
| 5 — Ember Sanctum | [plans/track-sanctum.md](plans/track-sanctum.md) S1–S9 | done (QA #7–#7e); casing-gap fix (f34a96a) not verified in game, QA #7f queued |
| 6 — Realm framework | [plans/track-realms.md](plans/track-realms.md) R1–R7, R10 | done (QA #8–#8c); 2–3 players on one server not verified in game |
| 7 — Boss framework | [plans/track-realms.md](plans/track-realms.md) R8–R9 | done (QA #8); a second player's personal Ember Heart, the fog wall pushing back an outsider, sounds and boss music not verified in game |

## Milestone 8 — Moon nights

Design: [DESIGN.md](DESIGN.md) §3.3; §10.12 built as recommended. Three tracks (COORDINATION.md); each keeps its
notes in `plans/<track>.md`. Config: track-moon owns the `moon` section; night-mobs uses `nightMobs`, vampire uses
`vampire` (one class per top-level section). Keys written below as `moon.fullMoon.*` / `moon.blood.*` live there.

| ID  | Task | Track | Depends on | Status |
|-----|------|-------|------------|--------|
| 8.1 | **Moon-night state + `moon.api`** | moon | — | done (GameTest) |
| 8.2 | **Night hold, sleep block, fallback, announcements** | moon | 8.1 | done (GameTest) |
| 8.3 | **Blood-red sky sync** | moon | 8.1 | done (GameTest: packet sent; red sky not seen) |
| 8.4 | **Full moon mob modifiers** | moon | 8.1 | done (GameTest) |
| 8.5 | **Curse of the Night** | moon | 8.1 | done (GameTest) |
| 8.6 | **Vampirism** | moon | 8.1 | done (GameTest) |
| 8.7 | **Ashen Zombie** | night-mobs | — | done (GameTest; models not seen) |
| 8.8 | **Ghost** | night-mobs | 8.7 | done (GameTest; models not seen) |
| 8.9 | **Full moon waves** | night-mobs | 8.1, 8.5, 8.7, 8.8 | done (GameTest; models not seen) |
| 8.10 | **Vampire NPC + provoking** | vampire | 8.1 | done (GameTest; models and boss moves not seen) |
| 8.11 | **Vampire boss** | vampire | 8.10 | done (GameTest; models and boss moves not seen) |
| 8.12 | **Vampire arena dimension** | vampire | 8.11 | done (GameTest; models and boss moves not seen) |
| 8.13 | **Milestone 8 check** | coordinator | 8.1–8.12 | done (34 GameTests + server check; visuals not seen) |

- **8.1 Moon-night state + `moon.api`.** World state on the overworld (`PersistentState`): lunar cycle
  `c = day / 8`; events in cycles `c ≥ 1` with `c % moon.cycleInterval == 0` (default 3), so the first full moon
  (day 0) never counts and the first event is the full moon of day 24, the blood new moon on day 28. A night runs
  from dusk to dawn. Contract `moon.api.MoonNights`: `current(server)` → `NONE` / `FULL_MOON` / `BLOOD_NEW_MOON`;
  events `NIGHT_STARTED(server, type)` and `NIGHT_ENDED(server, type, reason)` (`DAWN`, `GOAL_MET`, `TIMEOUT`,
  `COMMAND`); `hold(server, Identifier)` / `release(server, Identifier)` keeps the night going while any hold is
  open. Debug: `/embervoid moon status|start full|start blood|stop|setcycle`. Sent for merge as soon as it compiles.
  *Done when:* the schedule is JUnit-tested (days 0, 8, 16, 24, 28, 48, 72); state survives a restart.
- **8.2 Night hold, sleep block, fallback.** While a hold is open, time stops just before dawn; when the last hold
  is released, dawn comes. `moon.fallbackMinutes` 30 real-time minutes ends the night (`TIMEOUT`). Beds refuse on
  a moon night with a message. Title + chat line at the start and end, in both languages.
- **8.3 Blood-red sky.** S2C packet with the current night on join, dimension change and start/end; client tints
  sky, fog and the moon red on the blood new moon in the overworld only.
- **8.4 Full moon mob modifiers.** Through `MobLevels.registerProvider`: overworld hostiles spawning on a full
  moon get `moon.fullMoon.levelBonus` (3). Follow range ×`moon.fullMoon.followRange` (1.5); a mob with a player
  target in range keeps it. Reverted for mobs still alive at the end (follow range only).
- **8.5 Curse of the Night.** Own player attachment (stacks). A death adds a stack when the killer is a hostile
  mob or its projectile's owner: anywhere, realms and bosses included; not players, falls, drowning,
  suffocation, lava, the void. Reset when the full moon ends. API `CurseOfTheNight.stacks(player)`. Shown in
  `/embervoid moon status` and a chat line on each new stack.
- **8.6 Vampirism.** On the blood new moon, a hostile mob that damages a player heals `moon.blood.vampirism`
  (25 %) of the damage dealt, with a red particle.
- **8.7 Ashen Zombie.** GeckoLib mob (placeholder model via `tools/mob_placeholders.py`), zombie-like melee, does
  not burn in daylight, 20 XP. Level set on spawn by the wave (spot level + `moon.fullMoon.zombieLevelBonus` 5).
- **8.8 Ghost.** GeckoLib flyer, passes through blocks (no clip), 2× Ashen Zombie HP and damage,
  `moon.fullMoon.ghostMissChance` 15 % of hits on it miss, 40 XP.
- **8.9 Full moon waves.** On `NIGHT_STARTED(FULL_MOON)`: per overworld player, `min(stacks, 10)` Ashen Zombies,
  or 5 Ghosts with no stacks, spawned 16–32 blocks away on safe ground. Each player's wave holds the night
  (`hold(server, embervoid:wave/<uuid>)`) until all of it is dead. A player outside the overworld gets the wave
  on return while the night is on. Wave mobs despawn on `NIGHT_ENDED`. Flask Shard for clearing a Ghost wave.
- **8.10 Vampire NPC + provoking.** On `NIGHT_STARTED(BLOOD_NEW_MOON)`: 1–3 friendly Vampires (GeckoLib
  placeholder) near each cluster of overworld players (64 blocks). The first hit on any Vampire turns it (visual
  transformation); all others vanish. After `moon.blood.provokeMinutes` 5 the one nearest to a player turns.
  Holds the night until the boss is beaten.
- **8.11 Vampire boss.** Boss form as an `ArenaBoss` with phases and telegraphs. Level: highest participant
  level, at least 15, capped at the top of the band of the highest realm any participant has unlocked. Shared
  boss bar; loot per §10.12 (first kill / repeat, personal).
- **8.12 Vampire arena dimension.** Own dimension (small void-floored arena, red sky). When the boss forms, every
  overworld player outside a Sanctum core is teleported in, return point remembered, and sent back after the kill.
  A player who dies there respawns as usual and cannot re-enter; their remnant moves to their return point when
  the arena closes. Everyone dead → `NIGHT_ENDED`. The arena resets for the next blood moon.
- **8.13 Milestone 8 check.** Done without a client: 34 GameTests (`MoonTests`, `NightMobTests`, `VampireTests`; the
  GameTest world gets the mod's dimensions through a gametest-only mixin) and the server check. Not seen: the red
  sky, models, animations, the Vampire boss's moves against a real fighter.

## 0.3.0 release tasks

| ID  | Task | Who | Status |
|-----|------|-----|--------|
| R30.1 | **Sealed Gate placeholder**: in 0.3.0 the gate shows "the depths are still forming" instead of opening (config `realm.depthsOpen`, default false) | coordinator | done – not verified in game |
| R30.2 | Balance pass on the moon nights | release-balance (R40.1c) | done (R40.1c) |
| R30.3 | Multiplayer and performance test | release-mp (R40.4) | done (R40.4) |
| R30.4 | Release 0.3.0 | coordinator | superseded: never published on its own; 0.4.0 contains it, the leftovers shipped in 0.4.1 (D1) |

## 0.4.0 "Ashen Reach" — milestones 9–13

Design: [DESIGN.md](DESIGN.md) §4.5; §10.11 built as recommended. Five tracks (COORDINATION.md), each keeps its
notes in `plans/<track>.md`. A track works its own layer of the Citadel (milestone 13) after its zone tasks.
Every task ends with GameTests (or the server check for worldgen) for what it built; models are greybox at
final size with final animation names, art comes in the release pass.

Fixed ids (contracts, so tracks can reference each other's content before it is merged):
- **Mobs** `embervoid:` `cinder_husk`, `ash_stalker`, `ashen_carrion`, `withered_thrall`, `lantern_hag`,
  `bark_hollow`, `restless_bones`, `crypt_moth`, `hollow_sentinel`, `grave_warden`, `cinder_hound`,
  `sparking_marksman`, `furnace_golem`, `smoldering_knight`.
- **Bosses** `embervoid:` `ash_colossus`, `grieving_tree`, `crypt_keeper`, `smoldering_lord`; progress ids stay
  `embervoid:ashen_reach/zone_a|b|c` and the realm boss id in `realm.Realms`.
- **Materials and boss items** (track reach-gear, 11.1): `cinder_shard`, `royal_amber`, `raw_grave_iron`,
  `grave_iron_ingot`, `smoldering_ingot`, `smoldering_template`, souls `ash_colossus_soul`, `grieving_tree_soul`,
  `crypt_keeper_soul`, `smoldering_lord_soul`, `ash_of_rematch`, `flask_ember`.
- **Chest loot tables** (track reach-gear, empty stubs in 11.1): `embervoid:chests/ashen_reach/<zone>_common`,
  `<zone>_treasure`, `<zone>_arena` for zone `fields`, `woods`, `necropolis`; `citadel_common`,
  `citadel_treasure`.
- **Tags:** `embervoid:realm_ores` (blocks, reach-world), `embervoid:bleed_immune` (entities, reach-gear; entries
  `"required": false`), `embervoid:artifacts`, `embervoid:rings` (items, reliquary).

### Milestone 9 — Realm 1: world (track `reach-world`)

| ID  | Task | Depends on | Status |
|-----|------|------------|--------|
| 9.1 | **Fog API + per-zone fog** (contract, first) | — | done – not verified in game (fog look not seen) |
| 9.2 | **Placed enemies** (contract) | — | done (GameTest) |
| 9.3 | **Zones renamed, eternal dusk, zone looks** | — | done (GameTest + server check; zone looks and title not seen) |
| 9.4 | **Zone terrain density + arena sites** | 9.3 | done (GameTest + server check; terrain shapes not seen) |
| 9.5 | **Realm 1 blocks and ores** | 9.3, 11.1 | done (GameTest + server check; textures and particles not seen) |
| 9.6 | **Landing plaza, roads, milestones, outposts** | 9.2, 9.4 | done (GameTest + server check; roads on steep ground and looks not seen) |
| 9.7 | **Minor structures per zone** | 9.2, 9.5, chest ids | done (GameTest + server check; ruin looks not seen) |
| 9.8 | **Necropolis crypt jigsaw + water** | 9.7 | done (GameTest + server check; crypt and pool looks not seen) |
| 9.9 | **World edge** | 9.4 | done (GameTest + server check; border and bridge looks not seen) |

- **9.1 Fog API.** Client mixin on `BackgroundRenderer` with a small API (client side, e.g.
  `ReachFog.push(source, density, color)` / `clear(source)`, strongest wins, eased) so the boss arena fog (12.1)
  and the Ring of the Fog Stalker (11.12) reuse it. Per-zone fog: Fields grey-ochre, Woods dense grey-green,
  Necropolis blue-grey, picked from the biome under the camera. Sent for merge as soon as it works.
- **9.2 Placed enemies.** A marker in templates + structure processor that spawns the named entity at worldgen:
  persistent, never respawns, not reset by shrine rest, level from the zone band. Takes any entity id; an unknown
  id logs a warning and spawns nothing, so templates use the final mob ids from the start. Sent for merge early.
- **9.3 Zones.** Names Ashen Fields / Royal Woods / Sunken Necropolis (ids stay `zone_a/b/c`), both languages;
  `fixed_time` ≈ 13500 in the realm `dimension_type`; zone biome looks (surface, colours, particles, ambient);
  placeholder vanilla `spawners` removed (track reach-mobs adds its own spawns in code).
- **9.4 Terrain.** One zone-aware density function (plain / hills and ravines / sinkhole basin), blended at the
  borders; arenas get a flat site at the right height (arena height offset). Dry everywhere but the Necropolis.
- **9.5 Blocks and ores.** ~6–8 blocks (burnt straw, milestone, gravestone, ember crack, smouldering log, wet
  crypt stone…), datagen, both languages. Cinder Ore, Amber Vein, Grave Iron Ore, each generated only in its
  zone, tag `embervoid:realm_ores`; drops per DESIGN §4.5 (items from 11.1).
- **9.6 Landing and roads.** Landing as the crossroads plaza (shrine, Return Obelisk, Sealed Gate stay where
  they are); road generator: ash-brick road from the landing to each arena, a milestone every ~100 blocks, 2
  outposts per road (toll gate, watchtower) with placed fodder/ranged mobs, never tanks.
- **9.7 Minor structures.** ~20–25 `.nbt` pieces (DESIGN §4.5 table) with placed enemies and chests pointing at
  the chest table ids above (treasure chests: Fields granary vault, Woods hunting lodge).
- **9.8 Crypt.** Necropolis crypt jigsaw (5–7 pieces) below crypt entrances, lower crypt with the treasure chest;
  water only here (sinkhole bottoms, flooded crypts), placed by structures/features.
- **9.9 World edge.** Broken walls and collapsed bridges at the abyss cliffs.

### Milestone 10 — Realm 1: zone bestiary (track `reach-mobs`)

| ID  | Task | Depends on | Status |
|-----|------|------------|--------|
| 10.1 | **Greybox pipeline + shared mob base + spawns** | — | done (GameTest; models and animations not seen) |
| 10.2 | **Cinder Husk, Ash Stalker** | 10.1 | done (GameTest; models not seen) |
| 10.3 | **Ashen Carrion** | 10.1 | done (GameTest; models not seen) |
| 10.4 | **Withered Thrall (+ hanging cage), Lantern Hag** | 10.1 | done (GameTest; models not seen) |
| 10.5 | **Bark Hollow** | 10.1 | done (GameTest; models not seen) |
| 10.6 | **Restless Bones, Crypt Moth** | 10.1 | done (GameTest; models not seen) |
| 10.7 | **Hollow Sentinel, Grave Warden** | 10.6 | done (GameTest; models not seen) |

- **10.1 Pipeline.** Greybox GeckoLib models at final size with draft animations under final names (extend
  `tools/mob_placeholders.py`); a shared base for windup telegraphs (DESIGN §4.5 ticks) and the "placed" state;
  natural spawns for fodder, ranged and the rare Carrion through `BiomeModifications` on the zone biomes only;
  base levels in the mob config defaults; basic vanilla-flavoured drops. Realm loot rolls (materials, set pieces,
  artifacts) are injected by tracks reach-gear and reliquary, not written here.
- **10.2–10.7** Each mob per the DESIGN §4.5 bestiary table: stats, behaviour, telegraph, sounds (vanilla
  placeholders), both languages, GameTests for the behaviour (ambush trigger, dive, cage drop, dormant tree and
  ×1.5 from axes/fire, bone pile re-forming and staying dead in light, torch snuffing, shield front immunity and
  bash window, bell re-forming piles).

### Milestone 11 — Realm 1: loot and accessories (tracks `reach-gear`, `reliquary`)

| ID  | Task | Track | Depends on | Status |
|-----|------|-------|------------|--------|
| 11.1 | **Materials, boss items, chest table stubs** (contract, first) | reach-gear | — | done (GameTest; item textures not seen) |
| 11.2 | **Bleeding + `bleed_immune`** | reach-gear | — | done (GameTest) |
| 11.3 | **Smoldering Plate + Template** | reach-gear | 11.1 | done (GameTest; armor textures not seen) |
| 11.4 | **Mob-drop sets + unique pieces** | reach-gear | 11.1 | done (GameTest; textures not seen) |
| 11.5 | **Ashen Scythe, Warden's Scourge, Royal Ash Rapier** | reach-gear | 11.1, 11.2 | done (GameTest; textures and reach feel not seen) |
| 11.6 | **Huntsman's Crossbow** | reach-gear | 11.1 | done (GameTest; crossbow arm poses fixed by client mixins, not run on a client; textures not seen) |
| 11.7 | **Consumables + Flask Ember** | reach-gear | 11.1, 11.2 | done (GameTest; Lantern Oil gives Night Vision, no dynamic light; textures and bomb visuals not seen) |
| 11.8 | **Chest loot + realm loot on mobs** | reach-gear | 11.3–11.7 | done (GameTest) |
| 11.9 | **Reliquary: data, sync, screen** | reliquary | — | done (GameTest; screen look and button position not seen) |
| 11.10 | **Reliquary: remnant, stat points** | reliquary | 11.9 | done (GameTest) |
| 11.11 | **8 artifacts** | reliquary | 11.10 | done (GameTest; textures and tooltips not seen) |
| 11.12 | **6 rings** | reliquary | 11.9, 9.1 | done (GameTest; slow-fall feel, fog clearing and textures not seen) |
| 11.13 | **Accessory drops** | reliquary | 11.11, 11.12 | done (GameTest) |

- **11.1** Items and recipes for the materials (Smoldering Ingot shapeless, Grave Iron smelting), the four boss
  souls, Ash of Rematch (item and recipe; its use is 12.7), Flask Ember (item only; its effect is 11.7), and empty
  chest loot tables at the fixed ids. Sent for merge first: world, mobs and bosses reference these ids.
- **11.2** Bleeding status effect I–III (+1 per hit, 6 s, refreshes, 0.5/1/1.5 HP/s ignoring armor), tag
  `embervoid:bleed_immune` with optional entries.
- **11.3** Smoldering Template recipe; smithing diamond piece + ingot + template → Smoldering Plate (numbers in
  DESIGN §4.5), −8 % fire damage per piece, set: no burning ticks.
- **11.4** Reaper's Garb, Huntsman's Leathers, Necropolis Warden with set bonuses; Scarecrow's Mask, Huntsman's
  Greaves, Drowned Sabatons, Furnace Heart Cuirass with their effects.
- **11.5** Weapons with passives and specials through the special-attack framework (DESIGN §8.1); reach by a
  server raycast on attack; recipes soul + 2 Smoldering Ingot + zone part.
- **11.6** Crossbow: 2.0 s charge, ≈ 1.5× bolt, strong knockback, pierce 1.
- **11.7** Ash Bomb, Smoldering Resin, cinder fuel, Amber Balm, Amber Lantern Oil, Whetstone (and anvil repair of
  uniques); Flask Ember: +15 % flask healing once (hook in the flask, separate commit in `player`).
- **11.8** Chest tables per DESIGN §4.5 (common / treasure / arena-side; zone weapon 4 % in treasure chests);
  material, set-piece (3 % fodder / 15 % elite, player kill, FOR applies) and unique-piece (0.5 % / 4 %) rolls
  injected into the mob loot tables with `LootTableEvents.MODIFY`.
- **11.9** Reliquary attachment (synced to the owner), own screen with the player model, opened by a button next
  to the recipe book and a keybind, 2 artifact + 2 ring slots; slot kinds and counts from the tags.
- **11.10** Accessories go into the Ash Remnant on death with the inventory; artifact stat points go through the
  stat system without raising level or level cost (hook in `stats`, separate commit).
- **11.11 / 11.12** Artifacts and rings per DESIGN §4.5 (Fog Stalker ring through the 9.1 fog API, not in boss
  arenas; Delver's Ring on `embervoid:realm_ores`).
- **11.13** Artifact (8 % chest, 2 % elite) and ring (5 % chest, 1 % elite) rolls injected into the treasure
  chest tables and elite mob tables.

### Milestone 12 — Realm 1: zone bosses (track `reach-bosses`)

| ID  | Task | Depends on | Status |
|-----|------|------------|--------|
| 12.1 | **Boss framework additions** | 9.1 | done (GameTest; telegraph particles and arena fog not seen) |
| 12.2 | **Three arenas built** | 9.4 | done (GameTest on the templates; used in the world once 9.4 lands; looks not seen) |
| 12.3 | **Crypt Keeper** (first: riskiest) | 12.1, 12.2 | done (GameTest + server check; model, telegraphs, darkness and flood looks not seen) |
| 12.4 | **Ash Colossus** | 12.1, 12.2 | done (GameTest + server check; model, storm and cinders not seen) |
| 12.5 | **Grieving Tree** | 12.1, 12.2 | done (GameTest + server check; model, lanterns and grove fog not seen) |
| 12.6 | **Souls and Flask Shards** | 11.1 | done (GameTest) |
| 12.7 | **Ash of Rematch** (DESIGN §10.11 as recommended) | 11.1, 12.6 | done (GameTest) |

- **12.1** Arena hazard layer (ticked by the fight, restored on reset; real blocks only for water), S2C arena fog
  through the 9.1 API, telegraph shapes (ring at a point, line, cone, falling shadow), shield-breaking hit,
  fight-bound adds (removed with the fight), a non-HP phase, HP ×(1 + 0.5·(n − 1)) + 1 add per extra fighter at
  summon, target switch every 8–10 s or to the top damage dealer. Own GameTests.
- **12.2** The arenas replace the placeholder discs: crater market (r ~40, ≥ 8 smashable pillars), Royal Grove
  (walled garden behind the hunting lodge, r ~48), sinkhole with a switchback stair into the flooded ossuary
  (r ~32, ceiling ≥ 9); arena shrine outside the fog wall and the arena-side chest (`<zone>_arena`). Track
  reach-world owns where an arena sits and the ground under it; this track owns what is built there.
- **12.3–12.5** Each boss per DESIGN §4.5 (moves, phases, hazards, damage table, fixed levels 24 / 18 / 21);
  adds use the fixed mob ids (vanilla stand-ins behind one constant until track reach-mobs merges them).
- **12.6** First kill: the boss's soul as personal loot (25 % for a credited player who had already beaten it) +
  a Flask Shard; replaces the Ember Heart for Realm 1 bosses.
- **12.7** Ash of Rematch used on the arena shrine of a boss the player has beaten summons it again: loot and XP,
  never its soul or Flask Shard.

### Milestone 13 — Realm 1: Smoldering Citadel (all tracks, after their zone tasks)

| ID  | Task | Track | Depends on | Status |
|-----|------|-------|------------|--------|
| 13.1 | **Depths "Burning City": dimension type, terrain** | citadel | 9.4, 9.5 | done (GameTest + server check; sky, fog and hill looks not seen) |
| 13.2 | **Citadel jigsaw (~10–12 pieces)** | citadel | 13.1, 9.2 | done (GameTest + server check; fixed-layout structure, not a vanilla jigsaw; looks not seen) |
| 13.3 | **Cinder Hound, Sparking Marksman, Furnace Golem, Smoldering Knight** | reach-mobs | 10.1 | done (GameTest; models not seen) |
| 13.4 | **Smoldering Lord** | reach-bosses | 12.3–12.5, 13.2 | done (GameTest + server check; model, moves and courtyard look not seen) |
| 13.5 | **Realm completion, Realm 2 obelisk lit, depths open** | coordinator | 13.4 | done (GameTest `RealmLoopTests`) |

- **13.1** Own `dimension_type` for the depths (dark red sky, same dusk), hill terrain, ember cracks and
  smouldering logs only here, radius ~400, levels 25–30.
- **13.2** Route: depths landing in the outskirts → burning streets → city wall and gatehouse → inner ring and
  towers → courtyard below the keep wall (the Lord's arena site; track reach-bosses builds its contents) → keep
  with the treasure chest (`citadel_treasure`). Placed Citadel mobs per DESIGN §4.5.
- **13.4** P0 on the wall (non-HP phase, signal braziers), P1 greatsword, P2 at 50 % with one zone hazard every
  ~25 s; soul and Flask Ember as first-kill personal loot.
- **13.5** `realm.depthsOpen` default true; the Lord's kill completes Realm 1 and lights the Realm 2 obelisk
  (leads nowhere yet); a GameTest of the whole realm loop.

## 0.4.0 release tasks

Four release tracks (COORDINATION.md), then the release itself by the coordinator. The user does not play-test:
every task proves its result with GameTests, the server check, JUnit or rendered previews, and lists what still
needs eyes or ears in its merge notes. 0.3.0's leftovers (R30.2 moon-night balance, R30.3 multiplayer and
performance) are done here with them.

| ID  | Task | Track | Depends on | Status |
|-----|------|-------|------------|--------|
| R40.1a | **Balance model**: expected player level, stats and gear at each stage; mob and boss numbers against it | release-balance | — | done (`docs/balance-0.4.0.md`; feel not played) |
| R40.1b | **In-engine fight checks**: GameTests that measure hits-to-kill both ways for every mob and boss | release-balance | R40.1a | done (`docs/balance-0.4.0.md`; feel not played) |
| R40.1c | **Tuning**: zone bands, boss base values, XP yield vs level cost, loot rates, moon nights (R30.2) | release-balance | R40.1b | done (`docs/balance-0.4.0.md`; feel not played) |
| R40.2a | **Offline model preview**: render GeckoLib models + textures (and item/block textures) to PNG without a client | release-art | — | done (`tools/model_preview.py`) |
| R40.2b | **Zone mobs art**: the 10 Realm 1 zone mobs from greybox to final models, textures, animations | release-art | R40.2a | done (checked by preview; not seen in game) |
| R40.2c | **Citadel mobs and bosses art**: 4 Citadel mobs, 4 bosses (+ tree lantern, fire flask, ash bomb) | release-art | R40.2a | done (checked by preview; not seen in game; two client renderer hooks not run on a client) |
| R40.2d | **Texture consistency**: Realm 1 items, armor layers, blocks, GUI (Reliquary) in one style | release-art | R40.2a | done (checked by preview; Reliquary screen drawing not run on a client) |
| R40.3a | **Sound events**: every Realm 1 mob, boss, weapon, consumable and block gets its sounds (vanilla sound files, own events, subtitles in both languages) | release-audio | — | done (GameTest; 169 events on vanilla files; not heard) |
| R40.3b | **Boss music**: shared zone-boss theme and the Smoldering Lord theme as sound events; placeholder until licensed tracks are approved; `CREDITS.md` | release-audio | R40.3a | done (licensed tracks "For the Black Lord" and "Dark souls type boss theme", CC BY 4.0, `CREDITS.md`; not heard) |
| R40.4a | **Multiplayer GameTests**: 2–3 players through realm progress, boss scaling and adds, personal loot, fog wall, rematch, remnants, accessory sync | release-mp | — | done (GameTest `MultiplayerTests`; found and fixed the realm respawn bug) |
| R40.4b | **Performance on a dedicated server**: generation time of the realm, depths and Citadel; tick time and entity counts with placed enemies loaded; fix hot spots | release-mp | — | done (`docs/perf-0.4.0.md`: 4–9 ms mean tick everywhere, no hot spot) |
| R40.5 | **Release 0.4.0**: version, changelog, README, CORE, release build | coordinator | R40.1–R40.4 | done locally (version 0.4.0, docs, all checks green); the jar is built by CI on the `v0.4.0` tag, which waits for the user to push |

- **R40.1a** A pure balance calculator (JUnit) on the real formulas (stat curve, level cost, mob levels, difficulty
  factors, armor): the player expected at each stage (Sanctum, each zone in either order, depths) by XP earned so
  far and the gear available there; damage per hit both ways and hits to kill for every Realm 1 mob and boss, solo
  and with 2–3 players. Written up in `docs/balance-0.4.0.md` with the targets from DESIGN §4.5 (light hits ~4–6,
  heavy ~8–12, unblockable ~12–17 against full diamond on Normal).
- **R40.1b** GameTests with a real player at the expected level and gear against each mob and boss (fixed seeds):
  measured numbers next to the model's; differences explained or fixed.
- **R40.1c** Changes only through config defaults and base values, each with its reason in the write-up; zone
  order freedom kept (any zone first must be survivable at its band's bottom). Moon nights: levels, curse stacks,
  Ghost waves, Vampire boss against the same model.
- **R40.2a** A Python tool (`tools/model_preview.py`) that renders `.geo.json` + texture (and item/block textures)
  to PNG from several angles, plus animation key poses, so models can be reviewed by reading the images.
- **R40.2b–d** Models at the same size and with the same animation names as the greybox (code relies on them);
  every change checked through the preview; textures 16× style consistent with the building blocks.
- **R40.3a/b** No downloads without the user's approval: vanilla sound files remapped (pitch, volume, variants) for
  effects; for music, a short list of CC-licensed candidates goes to the coordinator, who asks the user; until then
  vanilla music stands in.
- **R40.4a/b** Folds in R30.3. Fixes go into the owning package as separate commits named in the merge request.

## Leftovers of 0.2.0 and 0.3.0

0.2.0 "Kindling" and 0.3.0 "Moon Nights" were never published; 0.4.0 contains both. Their open release items are
finished here on top of 0.4.0 and ship as **0.4.1** (decision D1 below). Coordinator: session `tails-020-030`
(branch `claude/tails-020-030-…`); three tracks (COORDINATION.md "Tracks — leftovers") merge into that branch, the
branch goes to `main` through the 0.4.0 coordinator. Nobody play-tests: every task proves itself with GameTests, JUnit,
the server check or rendered previews; what still needs eyes or ears is listed under "Needs a client".

**Hard rule — 0.4.0 stays as it is.** `BalanceTargetsTest` and `BalanceFightTests` stay green; Realm 1 numbers do not
change. A global change (mob level factors, difficulty factors, level cost, `experiencePerLevel` 0.06) is allowed only
when the overworld cannot be fixed with overworld values, and then the Realm 1 curve (`docs/balance-0.4.0.md`: Sanctum
~10 → depths ~30 → Realm 1 done ~36) is re-run and shown unchanged within ±1 level. Models keep size, bone names and
animation names; new sounds are own `embervoid:` events on vanilla files with subtitles in en_us and ru_ru.

| ID | Task | Owner | Status |
|----|------|-------|--------|
| L1 | **Sanctum weapons** (DESIGN §10.10): Sanctum Longsword and Ashbearer's Mace with special attacks, in the outer-hall loot | left-gear | done (GameTest `SanctumGearTests`, JUnit `SanctumWeaponRulesTest`; icons and specials' feel not seen) |
| L2 | **Overworld balance pass** (0.2.0 release): model and engine check from a fresh level 1 player to the Sanctum | left-gear | done (`docs/balance-overworld.md`, `OverworldTargetsTest`, `BalanceFightTests`; no number changed) |
| L3 | **Sound gaps** (0.2.0 release): overworld mobs, moon-night mobs, Vampires, Sanctum blocks and items, flask, remnant, stamina | coordinator (was left-audio) | done (GameTest `OverworldSoundTests`; 53 events on vanilla files; not heard) |
| L4 | **Art pass** (0.2.0 + 0.3.0 release): Ash Ghoul, Hollow, Ember Wisp, Ashen Maiden, Ashen Zombie, Ghost, Vampire, Vampire boss | left-art | done (checked by preview; JUnit `EntityModelFilesTest`; not seen in game) |
| L5 | **Headless checks for the in-game checks never run**: debug weapon and stamina (2.5, 2.6), Hollow wind-up / dodge / guard, Sanctum casing gaps (QA #7f) | coordinator | done (GameTests `StaminaTests`, `HollowTests`, `SanctumTests.floodedStairwellKeepsWaterOut`; JUnit `StaminaBarTest`) |
| L6 | **Dedicated-server test** (2.11, 3.8, 0.2.0 release): stats sync, leveling through the real packet, death → remnant → recovery, with a bot in `tools/server_check.py` | coordinator | done (`tools/server_check.py` bot step) |
| L7 | **Per-player arena shrine look** (DESIGN §10.8 known limitation) | coordinator | done (GameTest `MultiplayerTests.arenaShrineIsColdForNewcomersOnly`; the cold look not seen) |
| L8 | **README check** (0.2.0 release): features, required mods (GeckoLib is missing), config reference | coordinator | done (config rows checked against a fresh `embervoid.json`) |
| L9 | **Release 0.4.1**: version, changelog, CORE/ROADMAP/PLAN, 0.2.0 and 0.3.0 sections closed as superseded (R30.4 included) | coordinator | done locally (version 0.4.1, docs, all checks green); tag and push wait for the user |

- **L1 Sanctum weapons** (DESIGN §10.10, built as recommended here; numbers are the starting point, L2 fits them to the
  model). New package `com.embervoid.sanctum.gear`, through the special-attack framework (`item.ability`, DESIGN §8.1)
  and `stats.WeaponScaling`:
  - **Sanctum Longsword** (Длинный меч Святилища) — DEX 1. 6.5 damage, speed 1.6 (between the iron and the diamond
    sword). Special **Wide Cleave**: every target in a 90° arc of 3 blocks in front takes 1.25× the weapon's hit,
    25 stamina, 3 s.
  - **Ashbearer's Mace** (Булава пеплоносца) — STR 1. 8 damage, speed 0.9 (below the iron axe's 9). Breaks a raised
    Hollow guard like an axe. Special **Ground Slam**: ring r 3 around the player, 0.75× the weapon's hit, knockback
    and 2 s Slowness, 30 stamina, 5 s.
  - Both: durability ~600, repaired with iron ingots, the same enchantment set as the Realm 1 melee weapons
    (`GearWeapons.SWORD_ENCHANTMENTS`). Each lies in `embervoid:chests/sanctum_outer_hall` at a chance that gives about
    one of the two weapons per two Sanctums (≈ 6 % per chest each; the write-up states the per-Sanctum odds).
  - Must sit below the realm weapons (Ashen Scythe 10, Warden's Scourge 9, Royal Ash Rapier 6 at 2.0): no Sanctum
    weapon beats any realm weapon both per hit and per second, the best realm weapon on each axis beats both, and per
    second the Longsword is no stronger than a diamond sword, the Mace no stronger than a diamond axe; checked in
    `BalanceTargetsTest`. (Below every realm weapon on each axis separately cannot hold: the Rapier is light and fast,
    the Scourge slow with reach, so the Longsword would drop under an iron sword.) Item textures through
    `tools/art_kit.py` (preview with `tools/model_preview.py`), lang in both languages, GameTests for both specials (cost,
    cooldown, hits through `damage()`, guard break, no hit through walls) and the loot.
  *Done* (track notes `plans/left-gear.md`): package `sanctum.gear`, `SanctumWeaponItem` on `reach.gear.GearWeaponItem`
  (STR/DEX scaling, the realm sword enchantments, the special-attack flow; the Whetstone repairs them too); config
  section `sanctumGear`; item tag `#embervoid:breaks_guard` (the Mace breaks a Hollow's guard like an axe); Ground
  Slam gives Slowness II for 2 s. Loot: one pool, 6 % each per chest, never both: a given weapon in ≈ 22 % of
  Sanctums, either in ≈ 40 %. Own sound events for both specials (`sanctum.gear.SanctumGearSounds`).
- **L2 Overworld balance pass.** Extend `com.embervoid.balance` with the overworld stage: a level 1 player (all stats
  10) in leather / iron and later the Sanctum-level player (10, iron) against every overworld hostile at its band
  (base level + distance + below y=0, Nether +5 and End +8, full moon +3) on Easy, Normal and Hard; DESIGN §3.2 target
  "Hollow ~6 HP per hit through full iron on Normal"; overworld mobs 3–20 (DESIGN §5.4); the XP of a plausible route to
  the Sanctum (kills by spawn weight, mining and smelting XP) against the model's assumption of 492 points (level 10)
  and the early level costs. Engine numbers in `BalanceFightTests` for Ash Ghoul, Hollow, Ember Wisp and the vanilla
  zombie, skeleton arrow and creeper. Write-up `docs/balance-overworld.md`; changes through overworld config defaults
  and base values, one commit each with its reason.
  *Done:* every target holds with today's numbers (Hollow 6.0 through iron on Normal, bands 3–20, a route to the
  Sanctum worth 587 XP points, level 10); nothing changed, no global value touched, the Realm 1 curve and
  `BalanceTargetsTest` as in 0.4.0. Outliers kept and explained in `docs/balance-overworld.md` §5 (D13 below).
- **L3 Sound gaps.** Own events (per-package sound class, registered from `ModSounds`) for: Ash Ghoul, Hollow (incl.
  wind-up, guard raise / block / break, stagger), Ember Wisp (incl. dive, ignite), Ashen Zombie, Ghost (incl. a missed
  hit), Vampire (incl. the turn and the vanish), Vampire boss (telegraphs, nova, mist, leap), Ashen Maiden; Ember Shrine
  rest, Flask Shard offering, attunement, Void Obelisk travel and refusal, level-up; Healing Flask drink / empty /
  refill; Ash Remnant placed / claimed / burnt; stamina spend failure; the debug sweep. `SoundTests` extended so every
  one of them resolves, has subtitles and is played by its source.
- **L4 Art pass.** `tools/overworld_mob_art.py` (new, on `tools/art_kit.py`, like `tools/reach_mob_art.py`) replaces
  the placeholders of `tools/mob_placeholders.py`, `tools/maiden_placeholder.py`, `tools/nightmob_placeholders.py` and
  `tools/vampire_placeholders.py` for the eight entities; sizes per `tools/model_bounds.json`, bone and animation names
  unchanged (renderers, held items and glowmasks rely on them); every model reviewed through `tools/model_preview.py`
  (front, side, back, key poses) next to a Realm 1 mob for style.
  *Done* (track notes `plans/left-art.md`): bone names, pivots, animation names and lengths unchanged; JUnit
  `registry.EntityModelFilesTest` checks that every model keeps the animations its class plays, the renderer bones
  (`head`, `right_item`), the telegraph lengths and that every animated bone exists. The old placeholder scripts are
  retired (`tools/maiden_placeholder.py`, `tools/nightmob_placeholders.py` removed; `tools/mob_placeholders.py` keeps
  only its helpers; `tools/vampire_placeholders.py` writes only the Vampire Fang) so they cannot overwrite the art.
- **L5 Headless checks.** GameTests: the debug sweep spends 30 stamina, starts the 1 s cooldown, knocks back mobs, fails
  with 0 stamina (failure packet sent), creative spends nothing; stamina regenerates after the delay and the S2C
  stamina sync reaches its owner. JUnit: the stamina bar's visibility, fill and flash as a pure function taken out of
  `StaminaHud`. GameTests: a Hollow's swing lands only after its 8-tick wind-up and misses a player who left its reach
  during it; the raised guard cuts front melee to 20 % and deflects front arrows; an axe hit breaks it and staggers it
  60 ticks. QA #7f: a Sanctum placed where its shaft cuts through water ends with no water inside the casing.
  QA #8 step 9 (second and third player: personal boss loot, the fog wall pushing back an outsider, independent
  progress) is already covered by `MultiplayerTests` (R40.4a); nothing to add.
- **L6 Dedicated server.** `tools/server_check.py` gets a bot step (the `Bot` of `--perf`): it joins, a stat is set
  and levelled through RCON and through the real level-up packet next to a summoned Maiden (XP taken exactly, max
  health right), the bot receives the player-data and progress sync packets, dies with items and XP, respawns, and gets
  everything back from its remnant by a real interact packet.
  *Done:* the bot also checks the join syncs (player data, progress, stamina, config). XP is compared through
  `/embervoid stats` (XpUtil), not vanilla's `xp query`, which floors the bar.
- **L7 Arena shrine look.** The client draws a dormant arena shrine cold (smoke, no flame) for a player who has not
  beaten its boss; resting is still refused on the server. The server sends each player the dormant shrine positions
  near them (S2C, on entering a realm and on progress change); GameTest that the packet lists the right shrines.
  *Done:* `boss.ArenaShrines` sends each player in a realm the shrines of loaded arenas within 192 blocks with their
  boss ids (on join, dimension change and every 2 s when the list changed); the client (`client.boss.ArenaShrinesClient`)
  decides from its own synced progress, so a kill lights the shrine at once. A cold shrine shows smoke and white ash
  instead of flame. The block's light level stays 15 for everyone (light is not per player).
- **L8 README.** Required mods (Fabric API, GeckoLib), a short feature list per release, config reference checked
  against the config classes (every key and default), the L1–L7 additions.
- **L9 Release 0.4.1.** `mod_version` 0.4.1, CHANGELOG "0.4.1", CORE (Sanctum weapons, sounds, art), ROADMAP and PLAN:
  0.2.0 and 0.3.0 release sections closed, publishing marked superseded by 0.4.0 / 0.4.1. All checks green. No jar
  locally; tag and push wait for the user.

### Decisions for the user

Built as recommended; each is easy to change.

| # | Question | Recommendation (built) |
|---|---|---|
| D1 | Publish 0.2.0 and 0.3.0 on their own? | No: superseded by 0.4.0; the leftovers ship as 0.4.1 after the 0.4.0 tag |
| D2 | DESIGN §10.10 Sanctum weapons | Two weapons as in L1 (a DEX longsword and a STR mace), below the realm weapons |
| D3 | DESIGN §10.10 KIN / RES in 0.2.0 | Keep: KIN → flask healing, RES → toughness + fire reduction |
| D4 | DESIGN §10.1 building in realms | Keep: allowed, except arenas, landings, gates, shrines and obelisks |
| D5 | DESIGN §10.7 second death | Keep `burn_xp` (items stay) as the default, `burn_all` as the option |
| D6 | DESIGN §10.8 arena shrine | Keep dormant outside the fog wall; now also drawn cold per player (L7) |
| D7 | DESIGN §10.9 Nether / End / unlisted mobs | Keep Nether +5, End +8, unlisted hostiles 4, vanilla bosses unleveled (re-checked in L2) |
| D8 | DESIGN §10.2 level cap | None in 0.x (`leveling.levelCap` 0): Realm 1 ends near 36; revisit with Realm 2 |
| D9 | DESIGN §10.3 netherite for Realm 2 | No hard gate: realms stay independent of vanilla progress; netherite is a bonus |
| D10 | DESIGN §10.4 number of realms | 4 as sketched |
| D11 | DESIGN §10.5 respec | Later, through a rare consumable (BACKLOG) |
| D12 | DESIGN §6 "Wanderer's Cloak pieces" in the Sanctum loot | Move to BACKLOG; the Sanctum weapons take that slot |
| D13 | A creeper blast at 3 blocks kills a fresh player in iron on Normal (22 of 20; vanilla 11.5) | Keep (the harder overworld, DESIGN §3.1; a level 10 player survives it); if play shows it is too harsh, `mobs.levels.baseLevels.minecraft:creeper` 6 → 3 makes it survivable (18.5) |

### Needs a client

Cannot be checked headlessly; listed for whoever plays first: how every sound and both boss themes sound; the new
models and textures in game (animations, glowmasks, held items); the stamina bar's look and position; the red sky; the
cold arena shrine's look; the feel of the Hollow's wind-up and of the Sanctum weapons' specials, their icons in hand
and their particles.

## Playtest 0.4.1 — release 0.5.0

The user's own playthrough of 0.4.1 (2026-10-23; verdict 5/10, "looks cheap", the bosses "stools") broken into tasks.
Order from the playtest: the hub first (it settles the dungeon and what the overworld Sanctum becomes), then weapons
and the rebalance in one pass, then the visuals; the two small fixes fit in anywhere. Everything ships as one
release, **0.5.0** (the user, 2026-10-23: no string of small versions); the single release task is 21.5.

| Milestone | Playtest items |
|---|---|
| 14 | boss music volume, road visibility |
| 15, 16 | hub dimension and the Cleft Stele, the overworld dungeon and its boss, dungeon frequency |
| 17, 18 | weapons above netherite (looks, stats, the whip), full rebalance |
| 19, 20, 21 | mob and boss visuals, props and moving set pieces, the Citadel behind a fog wall |

On top of the rules at the top of this file:
- A milestone with a design task (`.0`) starts with it, done with the user in a chat: the big choices it names go to
  the user, everything else is decided there and marked as such. The milestone's build tasks start after it.
- Nobody plays: every task names its check (GameTest, JUnit, the server check, rendered previews) and what stays
  unverified.
- Track sessions do not run the full suite (the user, 2026-10-23): they compile, run `runDatagen` when assets change
  and run their own new or changed JUnit; they still write their GameTests and server-check steps. The coordinator
  runs the full regression (datagen, build, all GameTests, `balanceTargets`, the server check) once per merged batch.
- Worlds from earlier versions keep working: a task that removes or moves something (the Sanctum core, the depths
  dimension) carries its migration and a GameTest on old data.

**Can start now**, independent of each other (at most 4 sessions): 14.1, 14.2, 15.0 (needs the user), 19.1 (tooling,
touches no game code).

### Milestone 14 — quick fixes

| ID | Task | Size | Depends on | Status |
|----|------|------|------------|--------|
| 14.1 | **Boss music volume of its own** | S | — | done (JUnit `BossMusicTest`; the slider not seen, the music not heard) |
| 14.2 | **Roads you can see** | S–M | — | done (JUnit + GameTest + server check: 20 / 27 / 25 posts, all lit; paving previewed next to ash soil; how it reads from afar at dusk not seen) |

- **14.1 Boss music volume.** Boss tracks play in `SoundCategory.MUSIC` (`client.realm.BossMusicClient`), so Music
  at 0 % mutes them; the user keeps vanilla music off but wants the boss themes. A "Boss music" slider in vanilla's
  Music & Sounds screen (client mixin on `SoundOptionsScreen`), saved with the client options, default 100 %,
  en_us + ru_ru. The track plays outside the Music category (e.g. `MASTER` with the instance volume taken from the
  slider, following it while playing). No new `SoundCategory` (extending the enum breaks other mods). Check: JUnit
  on the volume rule (Music 0 / Boss 100 → audible, Boss 0 → silent, Master scales it); the screen not seen, the
  music not heard.
  *Built:* a "Boss Music" slider (`client.realm.BossMusicOption`, options.txt key `embervoid_bossMusic`, default
  100 %) appended to the Music & Sounds per-category sliders (`SoundOptionsScreenMixin`, filling the ninth slider's
  empty half-row) and loaded/saved by `GameOptionsBossMusicMixin` on `GameOptions.accept` (its `Visitor` opened by
  the new `embervoid.accesswidener`). `BossMusicClient` plays a tickable `MASTER` track whose volume is
  `BossMusic.trackVolume(slider)` every tick, so it follows the slider mid-fight; it starts even at 0 % so turning
  the slider up brings it in. Vanilla music is still stopped every tick during a fight. Master at 0 % stops the track
  for the rest of the fight (as the sound system does for any category at 0 %). The screen not seen, the music not
  heard.
- **14.2 Roads you can see.** The trail to the Ash Colossus is the ordinary road (`reach.world.RoadFeature`, PLAN
  9.6): ash bricks on ash soil, 15 % gaps in the edge band, a milestone every 100 blocks. It exists but has no
  contrast, on all three roads. In `RoadFeature.paving` and `RoadLayout`: a paving that stands out from ash soil
  (darker or warmer, a solid kerb, no gaps in the centre), light along it at eternal dusk (fire baskets or lantern
  posts every ~16–24 blocks on alternating sides), milestones every ~50 blocks. Check: JUnit on the layout (post
  spacing, both sides, none inside outposts or arena sites); the server check generates Realm 1 and counts posts
  per road; the paving next to ash soil in `tools/model_preview.py blocks`. How it reads from afar at dusk is not
  seen (19.1 renders it later).

### Milestone 15 — the hub dimension and the Cleft Stele

Brief from the playtest: the player should not depend on finding one generated Sanctum. The overworld Sanctum
becomes a **dungeon** (milestone 16); the Maiden and the obelisks move to a **hub dimension**. The dungeon boss
drops the **Crying Soul**: no crafting and no moon-night ingredients, the soul itself is the activator and is used up.
**Cleft Stele**: a wide flat stone slab the size of a nether portal (5 wide, 6 tall), split top to bottom, the crack is
the passage; wide and flat so it reads as a stele, not a pillar. Built from mod blocks to a pattern, detected like a
nether portal frame. Right-click with the soul: the blocks turn kindled (glowing cracks), mist and particles fill the
crack, a sound plays; right-click on an active stele → the hub. **One active stele per world:** A ⇄ hub; activating
B puts A out. Leaving an obelisk lands in the hub; the hub's exit leads to the active stele. Replaces Attunement,
"any Sanctum works" and "return to the Sanctum you entered from" (DESIGN §2).

| ID | Task | Size | Depends on | Status |
|----|------|------|------------|--------|
| 15.0 | **Design: hub, stele, dungeon, old worlds** (with the user) | M | — | done (DESIGN §1, §2, §10.13) |
| 15.1 | **Hub dimension** | M | 15.0 | done (JUnit `HubLayoutTest`, GameTests `HubTests`, server check: hub whole, one Maiden; not seen in game: the dusk sky and light on the island, the layout at eye level, arriving by `/embervoid hub`) |
| 15.2 | **Realm travel through the hub** | M | 15.1 | done (GameTests `HubTravelTests` + `HubTests`, JUnit `PlayerProgressTest`, server check bot travel; not seen in game: arriving in the hub from a realm, the travel sound there) |
| 15.3 | **Cleft Stele: blocks, frame, activation** | M | 15.0 | done (JUnit `SteleFrameTest` + GameTests `CleftSteleTests`; textures previewed; the glow, mist and particles not seen in game, the sounds not heard) |
| 15.4 | **Active stele and travel** | M | 15.2, 15.3 | done (GameTests `SteleTravelTests` + `CleftSteleTests` + `HubTests`, server check: bot stele round trip; not seen in game: the hub's lit stele on its pad, coming out in front of a stele, the travel sounds not heard) |
| 15.5 | **Worlds from 0.4.x** | S–M | 15.2, 16.1 | todo |

- **15.0 Design.** *Done 2026-10-23:* DESIGN §1, §2 and §10.13. The user picked the **Fallen Sister** as the dungeon
  boss, **one lit stele per world** and an **Ember Shrine at the boss door**; the rest was decided in the pass and is
  listed in §2. The original brief: DESIGN §1 (journey), §2 rewritten, §10 updated. Big choices for the user: the
  dungeon boss (2–3 pitches: concept, arena, moves); one active stele per world or per player (per world: one player's
  new stele puts out another's); whether the dungeon keeps an Ember Shrine. Decided in the pass: the hub's layout and
  size (the Sanctum core: Maiden, obelisk ring, shrine, on an island in a void dimension, protected like arenas), where
  a player lands and the exit, the exit with no active stele (bed or world spawn), death in the hub, the stele pattern
  and block names, dungeon variants (2–3), the Sanctum Map leading to the nearest dungeon, Attunement dropped (unlocks
  stay per-player progress), what happens to Sanctums already generated in old worlds (recommended: their core keeps
  working, nothing is destroyed).
- **15.1 Hub dimension.** Datapack dimension `embervoid:hub` (fixed time, no weather, no natural spawns, bounded);
  the hub structure placed once at a fixed spot when the dimension first loads; Maiden, Void Obelisks with their
  per-player states, Ember Shrine; breaking and building refused like in arenas. GameTests: placed exactly once, the
  Maiden and the obelisk ring present, obelisk states per player, building refused.
  *Done:* package `hub` (`Hub`, `HubLayout`, `HubState`, `/embervoid hub [check]`); template `hub/ember_sanctum`
  (`hub.datagen.HubTemplates`), the open core on a floating island at 0 64 0, placed on the first server start and
  never again. The hub's Cleft Stele goes at `HubLayout.STELE_CRACK_BOTTOM` (0 65 -16, 5 wide along x, an empty spot on
  a chiseled pad); the landing is 0 65 -13 facing the core. The whole dimension refuses building outside creative.
  The sky still dims while it rains in the overworld (no rain or lightning falls), as in the realms.
- **15.2 Realm travel through the hub.** Hub obelisks → realms as now; the Return Obelisk and leaving a realm → the
  hub (`RealmPlayerData.returnPoint` no longer points at a Sanctum); `Attunement` removed or reduced to the first hub
  visit. GameTests: hub → realm → hub, realm respawn unchanged, two players.
  *Built:* `hub.HubTravel`. Leaving a realm (the Return Obelisk, `/embervoid realm leave`) lands at the hub's
  landing; with no hub (broken datapack) it falls back to the hub's way out. `RealmPlayerData` keeps only the shrines;
  its old `return_point` / `return_yaw` are dropped on load. The spot a player last left "the overworld" from (any
  dimension not in the `embervoid` namespace, so never the hub, a realm or the Vampire arena) is recorded by
  `HubTravel.noteDeparture`, called by `HubTravel.enter` (the way into the hub, `/embervoid hub` for now) and
  `RealmTeleports.enterRealm` (an old world's core); a vanilla `/tp` records nothing. Attachment
  `embervoid:overworld_exit` (dimension, exact position, yaw; copied on death). API for 15.4:
  `HubTravel.lastOverworldExit(player)`, `HubTravel.leave(player)` (that spot, else a still-standing bed or charged
  anchor, else the world spawn), `HubTravel.enter(player)`; op command `/embervoid hub leave`. **Attunement is gone**
  (`sanctum.Attunement`, its title, welcome line, sound event and lang keys, `/embervoid progress … attune`): Realm 1
  unlocks without a word on the first arrival in the hub (any world change into it, or joining there); the old
  `attuned` flag is ignored, and players attuned in 0.4.x already have Realm 1 unlocked. Old cores keep the Maiden
  and obelisks; a player who never reached a 0.4.x core sees its Realm 1 obelisk dark until their first hub visit.
  Moon nights only take overworld players, so the hub is left alone; the "in a Sanctum core" exception moved to
  `Sanctum.isInCore` (old cores still count). Not seen in game: arriving in the hub from a realm, the sound there.
- **15.3 Cleft Stele: blocks, frame, activation.** Stele stone and its kindled variant (glowmask), the crack block
  (mist, particles); frame detection 5 × 6 on both axes per the 15.0 pattern; right-click with `crying_soul` on a
  valid frame uses it up and kindles the blocks, with a sound; the soul's only source until 16.3 is creative / a
  command. Cleft Stone is crafted (8 stone bricks around 1 coal or charcoal → 8, DESIGN §2). Art through
  `tools/art_kit.py`, checked with `tools/model_preview.py`. GameTests: valid and invalid frames
  on both axes, the soul taken only on a valid frame, breaking a frame block puts the stele out.
  *Built:* package `hub.stele`. `cleft_stone` with a `kindled` state (kindled: drawn full bright, light 7, embers
  from its cracks; drops itself either way), crafted 8 stone bricks around coal or charcoal → 8. `stele_crack`: a
  thin pane on vanilla's nether portal models with an animated translucent mist and an ember seam, light 11, no
  collision, unbreakable, no item, pistons cannot move it; mist, ash and ember particles and a low hum. `crying_soul`
  (rare, stacks to 16, lore line), creative tab and `/give` only. `SteleFrame` is the pure detector (5 × 6, two stone
  columns each side of an open middle column, X before Z, the lowest frame holding the clicked stone); open means
  air, a replaceable plant or the crack. The soul used on a cold frame kindles it (`CleftStele.kindle`: stones
  kindled, crack filled, sound `block.cleft_stele.kindle`, flame and mist burst) and is used up outside creative;
  on a broken, wrong, blocked or already lit frame nothing happens and it is kept. A kindled stone that goes for
  any reason puts out the kindled stones and crack joined to it (`block.cleft_stele.put_out`). Hooks for 15.4:
  events `CleftStele.KINDLED` (world, frame, player) and `CleftStele.PUT_OUT` (world, lost stone), and
  `CleftStele.onEnter(player, crackPos)`, called every tick a player stands in the crack, empty for now. Art:
  `tools/stele_art.py`. The glow, mist and particles not seen in game, the sounds not heard.
- **15.4 Active stele and travel.** World state (`PersistentState`): the active stele (dimension, position, axis);
  activating B puts A out; right-click on the active stele → hub, the hub exit → the active stele (none: per 15.0); one
  lit stele per world (DESIGN §2). GameTests: round trip, B replaces A (also when another player kindles B), a broken
  active stele, two players, a stele in the Nether.
  *Built:* `hub.stele.SteleState` (overworld `PersistentState` `embervoid_stele`: the lit stele's dimension, frame
  origin and axis, and the players who went through it) and `SteleTravel`. Kindling a stele makes it the world's;
  the old one goes out (`CleftStele.extinguish`: stones cold, crack empty, the put-out sound; stones shared with the
  new frame kept), whoever kindled either, and online players who had used the old one get a chat line
  (`embervoid.stele.replaced`; not the one who kindled). Losing a stone of the lit stele (`PUT_OUT`) leaves none; a
  lit stele found with its crack gone is forgotten when the hub is left. Walking into the crack (`onEnter`) or
  right-clicking a kindled stone or the crack of the world's stele → the hub's landing, the spot remembered by
  `HubTravel.noteDeparture`; the hub's stele → on the ground just in front of the world's stele (whichever side has
  room, facing away; else one block further; else the crack), with none `HubTravel.sendOut` (the last overworld
  exit, else bed or world spawn). After a trip a crack does not take the player for 40 ticks and not until they have
  been out of every crack for 10; a right click always works. Kindling is refused outside "the overworld" (any
  non-`embervoid` world, so the Nether and the End work; realms, the hub and arenas do not): the soul is kept, an
  action-bar line `embervoid.stele.not_here` and the obelisk refuse sound. `HubStele`: the hub's own stele is in the
  hub template (`HubTemplates`), lit, along X at `HubLayout.STELE_CRACK_BOTTOM`; nobody breaks it (creative too), a
  stone taken by a command is relit the next tick, and a hub placed before 15.4 gets it on server start; `Hub.verify`
  checks it. Sounds `block.cleft_stele.enter` (respawn anchor charge, low) and `block.cleft_stele.leave` (endermen
  portal, low), heard at the stele left and where the player lands. `/embervoid stele [kindle <pos>]` (op);
  `/embervoid hub leave` stays as an op/debug command for the fallback way out (it ignores the lit stele). Not seen
  in game.
- **15.5 Worlds from 0.4.x.** Per 15.0: Sanctum cores already generated, players standing in a Sanctum or holding an
  old return point when the world is updated, attunement flags. GameTests on old-format player data and an old
  Sanctum template placed in the test world.

### Milestone 16 — the overworld dungeon

| ID | Task | Size | Depends on | Status |
|----|------|------|------------|--------|
| 16.1 | **Sanctum → dungeon** | M | 15.0 | done (GameTests `SanctumTests` + server check; the arena previewed; not seen in game: the fog wall's ash, the dim light, the arena at eye level) |
| 16.2 | **Dungeon variants and frequency** | M | 16.1 | done – in-game look not seen (`spacing` 22 / `separation` 8; three variants; JUnit `SanctumStructureSetTest`, GameTests `SanctumTests` per variant (build + all 249 GameTests green before the survey's last edit, land-only distances; after it only the `sanctum` JUnit and the server check ran; the full regression is left for the coordinator after merging); server check `/embervoid sanctum survey` on two seeds: 39 and 29 dungeons within 1500 blocks of 0 0, land a mean 171 / 174 blocks from the nearest, 99 % / 98 % within 400; variants seen only in `model_preview`) |
| 16.3 | **The dungeon boss** | L | 15.0, 16.1 | done – look, motion and sound not seen or heard in game (GameTests `FallenSisterTests`, `BalanceFightTests`; server check; her `balanceTargets` rows pass: 31 clean hits, 1.6 min, 20 % uptime; motion judged only in `model_preview` sheets and GIFs) |
| 16.4 | **Overworld balance re-run** | M | 15.1–16.3 | done (the route at the placed dungeon's 4.5 guards and 6 chests: 629 XP, level 11 from 1; tables in `docs/balance-overworld.md` §8 regenerated; `OverworldTargetsTest` green. From 16.2: 24–42 dungeons within 1500 blocks of 0 0 on eight land-rich seeds, about 6 at the old 48 / 16; land a mean 150–190 blocks from the nearest; per dungeon 2–4 guards in the variant's hall, 2 of them always there, 2 chests there, the side halls as before plus the storeroom; placed GameTest dungeons held 5–7 chests and 4–5 Hollows) |

- **16.1 Sanctum → dungeon.** The `ember_sanctum` structure keeps its outer halls, guards (Hollows and the rest) and
  loot (Flask Shards, Sanctum weapons); the core with the Maiden and the obelisks goes. An Ember Shrine at the boss
  door, then a fog wall and the boss arena with a cold, protected Cleft Stele at its far end (DESIGN §2). Structure id
  stays `embervoid:ember_sanctum`; the name becomes Ruined Sanctum. GameTests: a fresh dungeon has no
  Maiden or obelisk, guards and chests as before; `SanctumTests` updated.
  *Built:* the gate hall's far door now leads to one new piece, `sanctum/arena` (21 × 11 × 30, pool `sanctum/arena`,
  `SanctumTemplates.arena`), so the boss wing is placed in the same step as the side halls and nothing later takes
  its place: a 9 × 7 × 10 shrine room with an Ember Shrine (rest, flasks, shards; no respawn point, as everywhere
  outside realms), a 3 × 3 **fog wall** in its far doorway (`fog_wall`, `sanctum.FogWallBlock`: no model, outline or
  collision, white ash like a fight's fog wall, unbreakable, no item; players walk through it) and the arena, the
  core's 21 × 21 hall ruined and dim: no chiseled stone but around the door and the dais, six unlit campfire
  braziers on wall posts for 16.3 to light, a cold Cleft Stele (along X, 24 stones, the crack open) on a dais at the
  far end, tallow candles and one high lantern so every floor block has block light ≥ 1 and nothing spawns. The
  Fallen Sister's spawn is a `minecraft:marker` tagged `embervoid.sanctum_boss_spawn` at the arena centre (template
  10 1 19); 16.3 swaps it for the summon rule. `sanctum.SanctumArena` refuses mining Cleft Stone inside an arena
  piece outside creative (found through the world's structure starts; message `embervoid.sanctum.stele_bound`);
  explosions are not refused (stone-brick resistance). The gate hall keeps its size and blocks, and the old core's
  template and pool `sanctum/core` stay, so Sanctums planned before 0.5.0 finish as they were. Name: Ruined Sanctum
  / Разорённое Святилище on the Sanctum Map, which got a lore line. Checks: GameTests (no Maiden, obelisk or
  flagstone; one shrine, nine fog blocks, a whole cold stele, one marker, guards and chests; the stele refused in
  survival, mined in creative, the same stone outside breaks; twelve fresh starts all with the arena and no core;
  the old template and pool load), the server check (the marker near the located Sanctum, no Maiden), previews in
  `build/model-preview/sanctum-16.1/`. README and CORE still describe the core (release docs, 21.5).
- **16.2 Variants and frequency.** The user found three villages, three shrines and about five ruined portals before
  one Sanctum (`spacing` 48 / `separation` 16 chunks, about one per 768 blocks). Target: about one per 300–400 blocks
  (`spacing` ~22 / `separation` ~8), 2–3 variants (separate start pools sharing the halls) so it does not repeat; the
  Sanctum Map leads to the nearest dungeon. Check: JUnit on the structure set; the server check counts dungeons
  within 1500 blocks on two seeds; variants rendered by 19.1 once it exists.
  *Built:* `structure_set/ember_sanctum.json` 22 / 8 chunks (352-block cells), salt kept. The start pool
  `sanctum/start` picks one of three surface ruins at equal weight (`sanctum.SanctumVariants`); each leads down its own
  stairwell to its own guarded hall, and all three halls lead to the same boss wing (`sanctum/arena`) and side halls:
  the **courtyard ruin** (`sanctum/ruin` → `descent` → `gate_hall`, the 0.4.x pieces, unchanged), the **collapsed
  chapel** (`sanctum/chapel`, 17 × 13 × 21: a roofless nave with pillars, pews, an altar under two tattered standards,
  graves outside, the way down a railed hole in the floor → `descent_chapel` → `nave`, 11 × 8 × 25, coffins in the bays
  between two rows of pillars) and the **sunken cloister** (`sanctum/cloister`, 21 × 11 × 21: a court sunk three
  blocks with a covered walk under a fallen-in roof, a bell tower at one corner, graves and a dead tree, a well in the
  middle that is the way down → `descent_cloister` → `cloister_walk`, 15 × 7 × 21, a walk round a buried court of
  columns, stores in a corner). The new stairwells are the ruin's with props at the bottom. New side hall
  `sanctum/hall_storeroom` (sacks, barrels, a cart, a weapon rack, a chest, a rolled Hollow; weight 2 of 11 in
  `sanctum/halls`). Same guard spots (the nave and cloister walk add rolled ones), loot table, decay processors and
  boss; every new piece dressed with the 20.3 props. Every old template and pool id stays. The Sanctum Map and
  `on_sanctum_maps` are unchanged: one structure id, so the map (search radius 64 chunks) and `/locate` find any
  variant, now about twice as near. Debug command `/embervoid sanctum survey <radius> [<seed> [<spacing>
  <separation>]]` (`sanctum.SanctumSurvey`): plans the dungeons around the source as worldgen would, on any seed and
  trial spacing, without generating chunks; reports the count by variant, the biomes of the refused spots and how far
  the land (the dungeon's biomes) is from the nearest. Tuning, ten seeds: 22 / 8 gives 24–42 dungeons within 1500
  blocks of 0 0 on the eight with 40 % land or more (13 and 14 on two that are three quarters sea), the land a mean
  152–193 blocks from the nearest, 92–100 % within 400; 20 / 8 a little denser, 24 / 8 sparser on most seeds. Checks:
  JUnit `SanctumStructureSetTest` (spacing, separation, salt; the start pool holds the three variants; every pool's
  templates and every template's jigsaw pools exist; the survey's distance maths); GameTests
  `ruinVariantIsWhole` / `chapelVariantIsWhole` / `cloisterVariantIsWhole` (a fresh plan of the variant moved to its own
  spot: its hall, one shrine, nine fog blocks, a whole cold stele, guards and chests, no core, and a player in the arena
  summons the Fallen Sister); the server check surveys seeds 123456789 and 8675309 (at least 20 dungeons, land mean
  ≤ 250, ≥ 90 % within 400). Previews in `build/model-preview/sanctum-16.2/` (top, iso and eye level of each new piece;
  `inside/` from within the halls). Not seen in game: any variant in real terrain, at eye level or by night.
- **16.3 The dungeon boss.** The **Fallen Sister** (DESIGN §2: look, phases, crying souls, drops) on the boss
  framework (`com.embervoid.boss`: arena, fog wall, telegraphs, music, loot), without rematch: each dungeon's boss
  is beaten once per world and every credited player gets a Crying Soul. Model and animations through the art scripts
  at final quality in this task (robe, veil and chain motion; death, hit and phase animations, see milestone 19).
  Fought by a level 1 player in vanilla gear; level band about 8–12; cadence built for about 20 % uptime (DESIGN
  §5.5, `docs/balance-0.5.0.md`). GameTests: start and reset, telegraphs land only
  after their wind-up, the soul drops for every credited player, a cleared dungeon stays cleared; a row in the
  balance model.
  *Built:* package `sanctum.boss`. `embervoid:fallen_sister` (`FallenSisterEntity` on `ReachBoss`, 0.9 × 3 blocks,
  Lv 10, base HP 115, armor 4, XP 60, the shared zone-boss theme) and her adds `embervoid:crying_soul`
  (`CryingSoulEntity`, 1 HP, any hit kills it, driven by her, not saved). P1: two censer arcs (cones, windups 18 and
  12, damage 4), the slam (a ring 2.5 ahead, windup 24, damage 6, then an ember puddle there for 4 s, 1 per 0.5 s),
  a backstep from a player behind her (windup 8, no damage); cooldowns 16–22. P2 at 50 %: the veil burns (40 ticks,
  no damage taken, the `veil` bone hidden after), the spin (a ring 4.5 around her, windup 26, damage 8) a third of her
  picks; crying souls, 3 then 4 every 20 s once gone, circle her and dive one at a time every 50 ticks: a shriek and
  a soul-fire line from the soul to where the target stood for 20 ticks, an 8-tick dive along it (damage 3, hers),
  40 ticks dazed on the floor, back. `CombatFx` on the arcs (trails), the slam (overhead trail, impact) and the spin
  (full-circle trail, impact). Hit reaction between moves only; a 50-tick death animation (her body stays:
  `ReachBoss.deathTicks`, a new hook, 0 for the other bosses). Summon rule `SanctumBossArena`: a player inside 8
  blocks of an uncleared arena's centre (template 10 0 19 through the piece's rotation; 16.1's marker is gone from the
  template) starts her fight (`ArenaManager.start`, fog radius 9). The six braziers light one per seventh of health
  lost (put out again by a reset) and burn for good after her death. Clearing: `ClearedSanctums` (world
  `PersistentState`, by arena centre); she never returns there, no rematch. Rewards (`BossReward` got `soulEveryKill`
  and a first-kill loot table): a Crying Soul to every credited player on every clear; a player's first clear also a
  Flask Shard and `gameplay/fallen_sister_first_clear` (a Sanctum Longsword or Ashbearer's Mace and some iron, gold,
  bottles). Balance: `BalanceData.FALLEN_SISTER(_CADENCE)` read her constants. Art: `tools/sanctum_boss_art.py`
  (18 bones: robe in tiers with four hem panels, veil with hood, front gauze and train, bell sleeves, a three-link
  chain and censer, forearm coils that grow while the chain winds; 10 animations; the soul 7 bones, 3 animations),
  previews `build/model-preview/fallen_sister*`, `crying_soul*`. 15 sound events on vanilla files. Debug command
  `/embervoid sanctum arena <x> <z>` (the server check uses it). GameTests `FallenSisterTests`: summon, braziers and
  reset in a placed Sanctum; clearing by two players (souls, shards, weapons, braziers, no second summon); souls for a
  veteran too; ×1.5 HP with two fighters; arc, spin, slam and the soul's dive land only after their windups; the veil
  and the first flock. Not seen or heard in game: the model, every animation, the particles, the sounds.
- **16.4 Overworld balance re-run.** The overworld route (`docs/balance-overworld.md`) re-run with more dungeons
  (guard XP, Flask Shards, Sanctum weapons) and the boss; the server check covers the hub. Feeds 18.3.

### Milestone 17 — weapons above netherite

Brief from the playtest: every Realm 1 weapon looks bad and feels worse than vanilla. The Royal Ash Rapier (6 damage,
speed 2.0, reach 2.5) has about a diamond sword's damage per second (7, 1.6), less reach than vanilla (3) and no
sweep. Realm 1 uniques go **above netherite**, each with an identity felt in hand; bigger in-hand models; player
attack animations; the Warden's Scourge as a real whip.

| ID | Task | Size | Depends on | Status |
|----|------|------|------------|--------|
| 17.0 | **Design: loot ladder, weapons, fight length** (with the user) | M | — | done (DESIGN §5.5, §6, §6.1, §10.14) |
| 17.1 | **Player attack animations** (playerAnimator) | M | 17.0 | done – not seen in game (playerAnimator 1.0.2-rc1+1.20 required; `com.embervoid.anim.AttackAnimations` sends the clip to the player and everyone tracking them on a swing, a special and the crossbow's load and shot, vanilla weapons untouched; 13 clips by `tools/player_anim_art.py`, swings as long as the attack cooldown; JUnit `WeaponKindTest`, GameTests `AttackAnimationTests`; motion seen only in `model_preview player` sheets) |
| 17.2 | **Weapon models in hand** | M | 17.0 | done – seen only in `model_preview blocks` (3D element models on the icon's diagonal, `tools/weapon_model_art.py`, `client.reach.gear.InHandModels`; the Scourge's lash leaves the handle's tip ring; the overhaul of 2026-10-24) |
| 17.3 | **Weapon stats and identities** | M | 17.0, 18.1 | done – feel not played (DESIGN §6.1 numbers in `GearWeapons`; Scythe 3 stamina back per enemy a sweep or Harvest catches, Rapier ramp `RapierRamp`, Crossbow 1.6 s / bolt 16 / pierce 2 / ×1.5 opener; zone uniques from soul + 4 zone material + part, the Rapier keeps its ingots; Smoldering Plate at netherite; `BalanceTargetsTest.realmUniquesBeatNetherite`, the weapon target of `./gradlew balanceTargets` green; GameTests in `GearTests`; the Scourge's tip waits for 17.4) |
| 17.4 | **Warden's Scourge as a real whip** | L | 17.1 | done – look and feel not seen or played; the coordinator's regression found the crack re-entering itself through Fabric's attack event (a stack overflow on every hit), fixed with `WardensScourgeItem.cracking`, all GameTests green (`WardensScourgeItem`: every mob within 0.3 of the lash's curve (`WhipArc`, hand to the crosshair at 6, bowed up-right) hit once at the swing's charge, the lash stops at the first block, the last 1.5 blocks ×1.6 with `item.wardens_scourge.crack` and a flash, one durability a swing, a click is a crack too; Lash unchanged but no free crack after it; client lash `ScourgeLashes`, a rope of 20 links from the in-hand handle model, laid along the same curve, then physics; JUnit `WhipArcTest`; GameTests `WardensScourgeTests` written, not run here (the coordinator's regression: `runGametest`, full `build`); checked here: compile of all source sets, `WhipArcTest` and `com.embervoid.balance.*`; seen only in `model_preview player whip_crack whip_lash`) |

- **17.0 Design** (one pass for milestones 17 and 18; they share the numbers). *Done 2026-10-23:* DESIGN §5.5 (fight
  length, hit sizes, the wind-up rule), §6 (ladder: Realm 1 about 30 % above netherite, the user's pick; Smoldering
  Plate raised to netherite), §6.1 (weapon identities and starting numbers), §10.14. **Status effects were taken out and
  postponed to 0.6.0** (the user: a big topic of its own; back in BACKLOG); Bleeding stays the placeholder and 18.2 is
  dropped. The original brief: DESIGN §6 loot ladder rewritten with the Realm 1 uniques above netherite, and what that
  does to the Sanctum weapons, the Realm 2+ tiers and the weapon upgrades in BACKLOG; each unique's identity (Ashen
  Scythe, Huntsman's Crossbow, Warden's Scourge, Royal Ash Rapier; the Sanctum Longsword and Ashbearer's Mace a step
  below); target fight lengths for the whole game (bosses about 1.5–2.5 min with dodging, about 20–35 clean hits; every
  mob role; moon nights); a hit taking two thirds of the player's health only after a clear wind-up (minimum ticks).
  **Status effects** (asked by the user 2026-10-22, the mod's own set worked out together, Bleeding first): the list
  (bleeding, rot/poison for Realm 2, frost, curse…), vanilla `StatusEffect` or souls-style buildup meters, how RES
  resists them (DESIGN §5.2 promises fire, poison, wither), immunities by entity tag, cures (consumables, the flask,
  shrines), HUD for buildup. Big choices for the user: how far above netherite; buildup meters or plain effects.
- **17.1 Player attack animations.** New required dependency playerAnimator (Fabric, 1.20.1): `build.gradle`,
  `fabric.mod.json`, README, ROADMAP "Required runtime mods". A small API that plays an attack animation on the
  attacking player for everyone nearby (server-triggered, synced); one set per weapon kind (sword, rapier thrust,
  scythe, mace, whip, crossbow). Check: GameTest that the trigger goes out on a swing and on a special; the
  animations not seen.
- **17.2 Weapon models in hand.** Bigger own models for the six weapons (GeckoLib item renderer or JSON with display
  transforms), first and third person; inventory icons stay readable. Through the art scripts, checked with
  `tools/model_preview.py`; not seen in hand.
- **17.3 Weapon stats and identities.** DESIGN §6.1 (identities, starting numbers, and the zone uniques' new recipes
  without Smoldering Ingots) and §6 (Smoldering Plate to netherite). Numbers from 17.0 / 18.1 (damage, speed, reach, sweep, specials) for the
  Realm 1 and Sanctum weapons, each unique's passive. `BalanceTargetsTest` to the new ladder.
  *Built:* the numbers as §6.1 has them (Scythe 12 / 1.1 / reach 3.5, Rapier 7 / 2.2 / 3.5, Scourge 10 / 1.0 / 6,
  Crossbow 32 ticks, bolt 16 without the vanilla crit roll, pierce 2, Punch III), all in `gear.weapons` /
  `gear.crossbow` config. The Rapier's ramp counts full-charge hits only (a low-charge hit neither builds nor breaks
  it; decided here); a miss is a swing packet with no attack that tick, a hit taken is any damage with an attacker.
  The Crossbow opener is for mobs only (`MobEntity.getTarget()` not the shooter). Harvest and Lunge now mark the
  swing that follows their use, so it is not a free reach hit (the Scythe and Rapier now reach past 3); Lash still
  gets one (17.4's). The Furnace Heart Cuirass follows the Plate (toughness 3, knockback 0.1). Found: at speed 2.2 the
  Rapier's full charge (9.1 ticks) comes before a mob's hurt cooldown ends (10), so on one target it lands at most 2
  full hits a second: 14 per second, about 18 ramped (`docs/balance-0.5.0.md` §4). The zone uniques still repair
  with a Smoldering Ingot. Old config files keep their old `gear.crossbow` charge and pierce until deleted.
- **17.4 Warden's Scourge as a real whip.** Long and segmented, bends through the air on a swing (bone chain with
  secondary motion or a client-side rope); hits along an arc line (server samples along the arc, no hit through
  walls); the tip cracks for more damage with its own sound and particles; Lash still pulls. GameTests: several
  targets on the line, tip bonus only at the tip, walls block, Lash unchanged. Not seen.
  *Built:* a client-side rope, not a GeckoLib bone chain: a rope follows the player's own motion, falls and drags
  (secondary motion a keyed chain cannot have), is laid along the very curve the server hits along (`WhipArc`, one
  JUnit-checked function for both), and needs no item-stack ids or a second trigger for other players (it rides on
  17.1's clip packet). In a hand the Scourge is its handle (`wardens_scourge_in_hand`, swapped the way vanilla swaps
  the trident; the icon stays the coiled whip); the lash starts at the handle's drawn tip (third person in the world
  pass, first person carried over from the hand's projection), about 0.8 blocks hang when idle, a crack pays out the
  full 6 over 2.5 ticks from over the right shoulder (`whip_crack`), the tip lands at tick 4 and the rope is let go,
  falls and coils back from tick 9; Lash flicks it straight out and yanks it back. The hit curve runs from the hand
  (0.3 right, 0.35 down, 0.45 ahead of the eyes) to the crosshair 6 ahead, so whatever is aimed at is on it; a click
  on a mob cancels the single vanilla attack and cracks the whole lash (the clicked mob always counted). Each mob gets
  a normal attack at the swing's own charge (enchantments, DEX scaling, Bleeding), the cooldown put back between
  them. Lash now marks its swing like Harvest and Lunge, so it no longer gets a free crack (17.3's note). Config
  `gear.weapons.whipTipBand` / `whipTipMultiplier`. Not checked in game: the rope's look, its anchor on the hand in
  either view, the link texture's repeat, the crack's sound and flash.

### Milestone 18 — full rebalance

Brief from the playtest: boss fights take 5+ minutes while a boss kills in about two hits. The 0.4.0 model gives
52–73 clean hits per Realm 1 boss (30–60 s of pure hitting), with dodging that is the 5 minutes; souls games sit at
about 20–35. The scope is the whole game: overworld mobs, moon nights, the dungeon, Realm 1 mobs and bosses, player
stats, weapons.

| ID | Task | Size | Depends on | Status |
|----|------|------|------------|--------|
| 18.1 | **Balance model on fight length** | M | 17.0 | done (JUnit `FightLengthTest` in the build; the 0.5.0 targets in `FightLengthTargetsTest`, tag `targets-0.5.0`, run by `./gradlew balanceTargets`: 6 of 7 red, 80 misses listed in `docs/balance-0.5.0.md`; GameTest `bossWindupsAreTheEntities`; nothing played) |
| 18.3 | **Retune: overworld, dungeon, moon nights** | M | 18.1, 16.4 | done (every 0.5.0 target green, now in `./gradlew build`; vanilla types through new `mobs.levels.healthScale` / `damageScale`; the Vampire boss's claws telegraphed; the creeper's blast kept per D13; checks: build, 282 GameTests, server check; not played) |
| 18.4 | **Retune: Realm 1 mobs and bosses, player stats** | L | 18.1, 17.3 | done (every Realm 1 row of the 0.5.0 targets green, now in the build as `RealmOneTargetsTest`; the Tree's bark turns hits outside a 2.2 s stun; boss HP ×0.38–0.97, the Keeper presses on; mob health to the bands; stat curve unchanged; GameTests measure it in the engine. Checks run here: compile, the balance JUnit, `balanceTargets` (17 misses, all 18.3's); one full build and GameTest run (247/247) before the last Tree commit (lantern regrow 8 s); the full regression is the coordinator's after merging. The feel was not played) |

- **18.1 Balance model on fight length.** `com.embervoid.balance` turns hits to kill into seconds (weapon speed, an
  uptime factor per boss for dodging) next to the 17.0 targets, for every stage: overworld, dungeon boss, moon
  nights, the three zones, the Citadel. New `docs/balance-0.5.0.md` (the old write-ups stay as history);
  `BalanceTargetsTest` gets the new targets (red until 18.3 and 18.4).
  *Built:* `BalanceData` has a cadence per boss (each move's wind-up, class and tell; per phase the beats with the
  brain's cooldowns, pauses and downtime), the wind-ups and cooldowns now public constants of the boss classes (no
  value changed); the Fallen Sister as a placeholder. `FightLength` gives seconds, clean hits and uptime solo and with
  2–3 players, the 0.5.0 stages with §5.5's reference players and weapons, and every boss move as a share of the
  weakest expected player's health; the §6.1 uniques sit next to today's as "0.5.0 proposed". The new targets went
  into their own tagged class instead of `BalanceTargetsTest` (which stays on the 0.4.0 targets and green, like
  `OverworldTargetsTest`); 18.3 / 18.4 drop the `excludeTags` line in `build.gradle` once they pass. Findings: the
  derived uptime is 19–22 % for the Colossus, Keeper and Lord (they can meet both bands with HP about ×0.3–0.6), but
  46–63 % for the Tree and the Vampire (their cadence must change first); the Lord is furthest off (72 clean hits,
  5.4–6.4 min); uniques cannot be crafted before the Citadel today (2 Smoldering Ingots), against §5.5's reference.
- **18.3 Retune: overworld, dungeon, moon nights.** Overworld mobs, dungeon guards and boss, moon-night mobs and the
  Vampire boss to the targets; the Vampire boss gets fewer openings first (wind-ups and recoveries on its claws;
  about 63 % uptime today, DESIGN §5.5), then HP. `OverworldTargetsTest`, `BalanceFightTests`, `./gradlew
  balanceTargets` for the stages it covers.
  *Built:* **Vampire boss:** the claws are drawn back for 12 ticks with their own animation (`attack.claw`) and leave a
  20-tick recovery instead of vanilla's traded swing (63 % → 15 % uptime); claw 5 → 3.5 (17 %, light), leap 7 → 5.5
  (31 %, heavy), HP 220 → 100: 30 clean hits in 2.1 min. **Vanilla types** get a factor on their base health / damage
  under the level (`mobs.levels.healthScale` / `damageScale`, applied in `MobLevels`, the model's `OverworldData` reads
  the same defaults so `BalanceFightTests` still compares model and engine): zombie family health ×0.75, creeper
  ×0.85, witch ×0.65 and damage ×0.5, Enderman ×0.8 / ×0.65, Vindicator ×1.3 / ×0.4. **Own mobs:** the Hollow 30 → 25 HP,
  the Ashen Zombie 24 → 11, the Ghost 48 → 36 HP and 8 → 5.5 damage. The creeper's blast stays lethal for a level 1
  player in iron (D13); its hits-on-player row is left out of the targets. Every mob now sits in its band, every
  boss in 20–35 clean hits and 1.5–2.5 min; the tag's exclusion is gone from `build.gradle`. Not played.
- **18.4 Retune: Realm 1.** Work list in `docs/balance-0.5.0.md` §5. The Grieving Tree's cadence first (its stun
  after every sweep is most of the fight; DESIGN §5.5), then zone and Citadel mobs and the four bosses: HP to the
  fight length, damage per hit under
  the wind-up rule, checked by a test over every boss move (damage, wind-up ticks); the player's stat curve (VIT,
  END) only where the targets need it. Measured in engine by `BalanceFightTests`; feel not played.
  *Built:* the **Grieving Tree's** bark turns every hit outside a stun (a shield knock says so) and the stun after a
  broken lantern lasts 44 ticks instead of 100 (stunned animation 2.2 s): with a trunk at ×0.3 its rooted waits alone
  were 60 % open, so shorter stuns could not reach ~20 %; now 16–18 % uptime. Boss HP: Colossus 180 → 105, Tree 170 →
  165, Keeper 180 → 102 (armor 8 → 4, close cooldowns 35 / 25 → 28 / 22: at 22 % uptime the diamond sword and the
  Scythe had no common HP), Lord 260 → 100; the Lord's greatsword swing 8 → 5 (32 % → 16 %, light). Mob base health
  to the bands (elites in the Woods and Necropolis also half their armor; Furnace Golem attack 9 → 7.5). The model
  learnt that a ×0 beat is no opening and that the bone pile scales with level and takes its own hits (a GameTest
  counts them). Every Realm 1 boss now 20–35 clean hits in 1.6–2.2 min at every stage; `RealmOneTargetsTest` holds
  Realm 1's rows in `./gradlew build`, `./gradlew balanceTargets` still lists 18.3's. The stat curve was not needed.
  Several stages sit at a band's edge (35 / 20 hits): the diamond sword's first visit and the uniques' later ones are
  ~1.6–1.9× apart per hit, which leaves little room. Not played.

### Milestone 19 — mobs and bosses that look the part

Brief from the playtest: "looks cheap", "the bosses are stools". None of the 24 animated mobs has a death,
hit-reaction or stagger animation; each has idle, walk and 2–5 attacks. References: L_Ender's Cataclysm and Mowzie's
Mobs for bosses.

| ID | Task | Size | Depends on | Status |
|----|------|------|------------|--------|
| 19.1 | **Offline viewer: structures and motion** | M | — | done (`tools/model_preview.py structure` / `motion` / `self-check`; no three.js page, the GIFs and trail strips are enough) |
| 19.2 | **Combat effects**: camera shake, swing trails, hit flashes | M | — | done (`com.embervoid.fx.CombatFx`: `shake` / `trail` / `flash` / `impact`; Camera Shake slider and Hit Flashes toggle in Accessibility; wired into the Colossus leap and drag, the Lord's landing, swings and slam, the Vampire leap; GameTests on range and on those moves, JUnit on the curves; looks not seen in game) |
| 19.3 | **Death, hit and stagger animations for the 24 mobs** (3 parts) | L | 19.1 | done: bosses (track `bosses`, `ReachBoss` hurt controller, `deathTicks()`, `BossAnimationTests`) and the 21 mobs (wave 2 `mobs`, `overworld-mobs`; `MobAnimationTests`, `OverworldMobAnimationTests` not run yet) |
| 19.4 | **Boss remodels** (one task per boss) | XL | 19.1, 19.2 | done – seen only offline (Colossus 108 / 26, Lord 105 / 28, Keeper 103 / 27, Tree 128 / 35, Sister 78 / 25, Vampire boss 71 / 24 cubes / bones; chamfered forms, layered plates, 2–3 segment secondary bones, eased moves with the windups unchanged, `EmberHaloLayer`; `plans/bosses.md`) |
| 19.5 | **Photo tour** (optional) | M | — | todo |

- **19.1 Offline viewer.** `tools/model_preview.py` already renders our geo models and key poses to PNG (pure
  Python). Add: structure `.nbt` files rendered with real block textures (ours and vanilla from Loom's jar), top-down
  and at eye level, placed-enemy markers shown; animation strips at a fixed frame rate (or a GIF) so motion can be
  judged; a three.js page for live playback in the browser pane only if the images are not enough. Used by every
  visual task, and retroactively for 14.2 and 16.2.
- **19.2 Combat effects.** Client side: camera shake (S2C packet with strength and duration, an accessibility
  toggle), swing trails for heavy boss and player attacks, hit flashes; config toggles. GameTest on the packets;
  looks not seen.
- **19.3 Mob animations.** Every animated mob gets `death`, a hit reaction and `stagger` where it can be staggered;
  the code plays them (GeckoLib triggered animations; death delays removal to the animation's length; a hit reaction
  never cancels a wind-up). Parts: overworld and moon-night mobs; Realm 1 zone mobs; Citadel mobs.
  `EntityModelFilesTest` requires the new animations; previews through 19.1.
- **19.4 Boss remodels.** Ash Colossus, Grieving Tree, Crypt Keeper, Smoldering Lord (the face with tiny eyes reads
  as a joke), the Vampire boss, and the dungeon boss if 16.3 left it short: more detailed models, more bones,
  secondary motion (cloaks, chains, branches), phase-change and death animations, 19.2 effects on heavy hits. Size,
  hitbox and move timings unchanged unless 18.4 changed them.
- **19.5 Photo tour.** A dev-only client mode: loads a test world, flies the camera through set points, saves
  screenshots and quits. Started by the user in their own terminal, never from a Claude session (the client crashes
  the app); no playing. Optional final check before the release.

### Milestone 20 — places: props and moving set pieces

Brief from the playtest: locations are visibly built from the same few blocks and pieces. References: Create for
moving machinery.

| ID | Task | Size | Depends on | Status |
|----|------|------|------------|--------|
| 20.1 | **Animated set-piece framework** | M | 19.1 | done – not seen in game (`block.setpiece`: one block entity type, ALWAYS / TRIGGERED modes, synced state, `SetPieces.trigger`, `ArenaEvents` fired by `BossFight`, chunk cap `setpieces.maxPerChunk`, `setpieces.animate`; `SetPieceTests` written) |
| 20.2 | **Moving set pieces** | L | 20.1 | done – not seen in game (fourteen pieces by `tools/setpiece_art.py`: windmill sails, hoist, scales, ash mill, cage, gear, bell, sluice grate, great brazier, portcullis, bellows, winch, censer; placed in the templates and the four arenas; `plans/setpieces.md`) |
| 20.3 | **3D props** | M–L | 19.1 | done – not verified in game (14 props in `block.prop.Props`, models and textures by `tools/prop_art.py`; shapes checked by `PropShapesTest`, placing, turning and drops by `PropTests`; looks seen only in `model_preview` sheets) |
| 20.4 | **Structure variety** | M | 20.3 | done – seen only in `model_preview structures` (track `art-blocks`: 25 new blocks, every builder rebuilt with masonry helpers in `TemplateBuilder` and zone palettes in `Ruin`; `plans/art-blocks.md`) |

- **20.1 Set-piece framework.** GeckoLib block entities for moving machinery: always on or triggered, no server
  ticking for pure decor, client culling by distance, a cap per chunk. GameTests on the cap and the triggers.
- **20.2 Moving set pieces.** Fields windmill with turning sails (replaces the static build), a granary hoist with a
  swinging chain; swinging bells and cages in the Necropolis; gate gears raising the Citadel portcullis, forge
  bellows and chain lifts in the Citadel; censers, braziers, smoke columns. Placed into the existing templates;
  checked through 19.1.
- **20.3 3D props.** About 12 props with their own shape and a matching hitbox (a sack looks like a sack, not a cube
  with a sack texture): sacks, carts, scarecrows, burnt barrels, coffins, gravestones, bone piles, candles, weapon
  racks, cages, banners. Art script, datagen, both languages, previews.
- **20.4 Structure variety.** Props into the minor structures, outposts and the Citadel; more jigsaw piece variants
  where one piece repeats most (count from the templates first). Rendered through 19.1.

### Milestone 21 — the Citadel behind a fog wall

Brief from the playtest: the Sealed Gate teleport into a separate depths dimension is "a shame". Straight to the full
version, no interim: the Smoldering Citadel becomes part of the realm dimension behind a fog wall, a fog wall in look
and in mechanics. The hard lock (DESIGN §4.1 "Final zone lock") moves to the server.

| ID | Task | Size | Depends on | Status |
|----|------|------|------------|--------|
| 21.0 | **Design: the final zone inside the realm** | S | — | done (DESIGN §4.1, §4.5, §10.15) |
| 21.1 | **Realm framework: final zone without its own dimension** | L | 21.0 | done – depths kept until 21.3 (JUnit `RealmLayoutTest`, `RoadLayoutTest`, `EdgeLayoutTest` on the data files; GameTests `RealmSectorTests`; server check: posts 20 / 28 / 39 and 28 / 37 / 33 on two seeds, all lit, gate road paved; `--perf` against `docs/perf-0.4.0.md`: arenas 0.9–1.0 s and minor structures 0.6–0.8 s per area as before, the landing 2.5 s (was 1.1) on a machine at load 5–6 where the untouched depths landing also went 0.9 → 1.6 s; a top-down zone map with an approximated warp noise looked right; not seen in game: the final sector's sky, fog and terrain, its straight borders at eye level, the gate road) |
| 21.2 | **Fog wall: look and lock** | M | 21.1 | done – the look not seen in game (JUnit `FinalZoneRegionTest`, `EdgeLayoutTest`; GameTests `FinalZoneLockTests`: walking, pearl, chorus, elytra, riptide, boat and horse, bridge, tunnel, cleared, creative, two players at one spot, the border wall; 258 GameTests green; server check PASS: gate at 0 76 21, border wall 50 of 50 stretches, 80×80 blocks of border generated in 1.1–1.3 s; not seen in game: the border wall, the gate's fog wall and the client fog curtain) |
| 21.3 | **Citadel generation in the realm dimension** | L | 21.1 | todo |
| 21.4 | **Worlds from 0.4.x** (realm dimensions regenerated) | M | 21.1, 21.3 | todo |
| 21.5 | **Release 0.5.0** | M | 14.1–21.4 | todo |

- **21.0 Design.** *Done 2026-10-23:* DESIGN §4.1 (layout, lock), §4.5 (Citadel), §10.15. The user picked the
  **fourth sector**: the final zone is a 60° sector of the realm dimension, the open zones 100° each, the radius grows
  1200 → 1300; its one gate (the Citadel Gate) stands at the landing plaza where the Sealed Gate was; the lock is a
  server-side region check, the wall and fog curtain along both borders are its look. Old worlds' realm dimensions
  are regenerated (my call: nothing is published).
- **21.1 Realm framework: four sectors, no depths dimension.** `RealmDefinition` loses `depths`: the final zone is a
  sector (start angle, width) with its own level band, biome and shrine respawn; `RealmLayout.zoneAt` gets
  per-sector widths (60° final, 100° open) and straight final-zone borders (no noise warp there); radius 1300;
  arena sites, roads (`RoadLayout`, `RealmRoads`) and the world edge follow the new angles; `Realms`, realm respawn
  (open zones and the final zone apart), `RealmDimensionLifecycle` and mob levels updated; the Sealed Gate block
  becomes the Citadel Gate at the plaza. JUnit on the layout; GameTests; the server check.
  *Done 2026-10-23, with one change (the coordinator's call, so the game stays completable at every merge):* the
  depths dimension stays until 21.3 and the Citadel Gate still leads into it. `RealmLayout.Sectors` (Reach: Fields
  120–220°, Woods 220–320°, Necropolis 320–60°, final 60–120° centred on +Z where the gate always stood), `zoneAt` →
  `FINAL`; the warp fades out over half an open zone towards the final sector, so its borders are straight rays.
  Biome source and `zone_blend` take `final_zone` / `final_width`; final biome `ashen_reach_citadel` (dark red sky
  and fog colours, burnt-outskirts placeholder terrain, no natural spawns, final band 25–30 through
  `RealmDefinition.levelsOf`). Arena sets: 100° sectors, spread 0.5 → 0.3 (the Woods site would cross a warped
  border). A straight 26-block gate road from the plaza to the sector's mouth (r 48), no other road there; the edge
  wall goes all the way round, the bridges only over the open zones. Block id `sealed_gate` → `citadel_gate`. Realm
  respawn unchanged (open-zone rules in the final sector too). **21.3 removes:** `RealmDefinition.depths` /
  `depthsRadius`, the depths branches in `CitadelGateBlock`, `RealmMobLevels.bandAt` and `ZoneTitles` (key
  `depths`), the depths entry in `RealmDimensionLifecycle`, `RealmConfig.depthsOpen`, the `ashen_reach_depths`
  dimension, type, biome, noise settings and structures; it adds the final sector's shrine respawn in `RealmEvents`.
- **21.2 Citadel Gate and the region lock.** The gate: a fog wall like the arena's, opened per player after the
  three zone bosses. The region check (angle + distance) every tick for players in the realm: a player without
  clearance inside the final zone is pushed back across the nearest border; ender pearls and chorus fruit landing
  inside fail, elytra gliding into it stops, riptide refused. The look: a burnt city wall along both borders
  (worldgen) and a client-drawn ember fog curtain near the player. GameTests for every way in (walk, pearl, chorus,
  elytra, riptide, a bridge built over the wall, a tunnel under it) and for a cleared player passing.
- **21.3 Citadel generation in the sector.** Also removes the `depths` dimension and `RealmDefinition.depths`, which
  21.1 kept so the game stays completable at every merge (its status line lists what to remove). The Citadel jigsaw and terrain (`com.embervoid.reach.citadel`,
  `CitadelLayout`, `CitadelTerrain`) move from `ashen_reach_depths` onto the sector's middle line about 600–800
  blocks out; the Citadel biome (dark red sky and fog colours, ember cracks, smouldering logs), outskirts and
  burning streets from the gate to the hill, burnt suburbs beyond it; the first Citadel shrine just inside the gate;
  Citadel mobs placed as before, Cinder Hounds natural only in the sector. Server check: generation time against
  `docs/perf-0.4.0.md`; GameTests on the pieces; renders through `tools/model_preview.py structure`.
- **21.4 Worlds from 0.4.x.** On the first start with 0.5.0, a world whose realm dimensions were generated with the
  old layout gets them regenerated: the realm and depths dimension folders are cleared before the worlds load
  (never the overworld, the Nether, the End or the hub); per-player progress, realm shrines rested (reset to the
  landing), remnants left in realms (moved to the player's hub landing) and anyone logged out inside (moved to the
  hub) are handled; the depths dimension is removed. A layout version stored in the world. GameTests on old data;
  a server-check step with an old-layout world.
- **21.5 Release 0.5.0.** Version, CHANGELOG, README (the boss-music slider, playerAnimator as a required mod), CORE,
  DESIGN; all checks green; the photo tour (19.5) offered to the user. Tag and push wait for the user; CI builds the
  jar.

## Open follow-ups

- **Needs a client** (nobody plays; for whoever plays first): every sound and both boss themes, all script-made models
  and textures in game, the red sky, the stamina bar, the cold arena shrine, the Reliquary screen, the feel of the
  Hollow's wind-up and of every weapon special. The single-player QA queue items (QA #7f, QA #8 step 9) are covered by
  GameTests now (PLAN L5).
- **Design:** the user's decisions D1–D13 in "Leftovers of 0.2.0 and 0.3.0" (DESIGN §10.1–10.10 and the release
  question), all built as recommended.
- **Publishing:** tags `v0.4.0` and `v0.4.1` and the push wait for the user; CI builds the jars.
