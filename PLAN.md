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
| R30.4 | Release 0.3.0 | coordinator | todo |

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
| L1 | **Sanctum weapons** (DESIGN §10.10): Sanctum Longsword and Ashbearer's Mace with special attacks, in the outer-hall loot | left-gear | todo |
| L2 | **Overworld balance pass** (0.2.0 release): model and engine check from a fresh level 1 player to the Sanctum | left-gear | todo |
| L3 | **Sound gaps** (0.2.0 release): overworld mobs, moon-night mobs, Vampires, Sanctum blocks and items, flask, remnant, stamina | coordinator (was left-audio) | done (GameTest `OverworldSoundTests`; 53 events on vanilla files; not heard) |
| L4 | **Art pass** (0.2.0 + 0.3.0 release): Ash Ghoul, Hollow, Ember Wisp, Ashen Maiden, Ashen Zombie, Ghost, Vampire, Vampire boss | left-art | todo |
| L5 | **Headless checks for the in-game checks never run**: debug weapon and stamina (2.5, 2.6), Hollow wind-up / dodge / guard, Sanctum casing gaps (QA #7f) | coordinator | done (GameTests `StaminaTests`, `HollowTests`, `SanctumTests.floodedStairwellKeepsWaterOut`; JUnit `StaminaBarTest`) |
| L6 | **Dedicated-server test** (2.11, 3.8, 0.2.0 release): stats sync, leveling through the real packet, death → remnant → recovery, with a bot in `tools/server_check.py` | coordinator | done (`tools/server_check.py` bot step) |
| L7 | **Per-player arena shrine look** (DESIGN §10.8 known limitation) | coordinator | done (GameTest `MultiplayerTests.arenaShrineIsColdForNewcomersOnly`; the cold look not seen) |
| L8 | **README check** (0.2.0 release): features, required mods (GeckoLib is missing), config reference | coordinator | done (config rows checked against a fresh `embervoid.json`) |
| L9 | **Release 0.4.1**: version, changelog, CORE/ROADMAP/PLAN, 0.2.0 and 0.3.0 sections closed as superseded (R30.4 included) | coordinator | todo |

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
  - Must stay below every realm weapon per hit and per second (Ashen Scythe 10, Warden's Scourge 9, Royal Ash Rapier 6 at
    2.0) and no stronger than a diamond sword / axe per second; checked in `BalanceTargetsTest`. Item textures through
    `tools/art_kit.py` (preview with `tools/model_preview.py`), lang in both languages, GameTests for both specials (cost,
    cooldown, hits through `damage()`, guard break, no hit through walls) and the loot.
- **L2 Overworld balance pass.** Extend `com.embervoid.balance` with the overworld stage: a level 1 player (all stats
  10) in leather / iron and later the Sanctum-level player (10, iron) against every overworld hostile at its band
  (base level + distance + below y=0, Nether +5 and End +8, full moon +3) on Easy, Normal and Hard; DESIGN §3.2 target
  "Hollow ~6 HP per hit through full iron on Normal"; overworld mobs 3–20 (DESIGN §5.4); the XP of a plausible route to
  the Sanctum (kills by spawn weight, mining and smelting XP) against the model's assumption of 492 points (level 10)
  and the early level costs. Engine numbers in `BalanceFightTests` for Ash Ghoul, Hollow, Ember Wisp and the vanilla
  zombie, skeleton arrow and creeper. Write-up `docs/balance-overworld.md`; changes through overworld config defaults
  and base values, one commit each with its reason.
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

### Needs a client

Cannot be checked headlessly; listed for whoever plays first: how every sound and both boss themes sound; the new
models and textures in game (animations, glowmasks, held items); the stamina bar's look and position; the red sky; the
cold arena shrine's look; the feel of the Hollow's wind-up and of the Sanctum weapons' specials.

## Open follow-ups

The 0.2.0 / 0.3.0 items below are planned in "Leftovers of 0.2.0 and 0.3.0" (L1–L9).


- **In-game checks not run yet:** QA #7f (Sanctum casing gaps, next batch); QA #8 step 9 (second and third
  player: personal Ember Heart, fog wall pushing back an outsider, independent progress); Hollow wind-up timing
  and dodge, guard block/break sounds; stamina HUD bar and debug weapon (2.5, 2.6); sounds and boss music.
- **Dedicated server:** 2.11 and 3.8 ran in single player only; the dedicated-server run is the 0.2.0 release
  task in ROADMAP.md.
- **Design:** DESIGN §10.1, §10.7, §10.8, §10.9 and the KIN/RES part of §10.10 are built as recommended and wait
  for the user's confirmation; §10.2–10.5 and the Sanctum weapons (§10.10) are open.
- **Art pass:** the overworld mobs (Ash Ghoul, Hollow, Ember Wisp), the moon-night mobs, the Vampires and the Maiden
  still use generated placeholder models; Realm 1 got its art in 0.4.0 (`tools/model_preview.py` can do the same for them).
- **Balance:** the 0.2.0 overworld balance pass (Realm 1 and the moon nights were balanced in 0.4.0, `docs/balance-0.4.0.md`).
- **Known limitation:** a dormant arena shrine looks lit to everyone; only resting is refused (a per-player look
  needs client-side progress).
