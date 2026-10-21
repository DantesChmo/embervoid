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

## Milestones 8 and later

Split into tasks when the Realm 1 content doc and the weapons doc (DESIGN §8) exist. Checklists in
[ROADMAP.md](ROADMAP.md) are the starting point.

## Open follow-ups

- **In-game checks not run yet:** QA #7f (Sanctum casing gaps, next batch); QA #8 step 9 (second and third
  player: personal Ember Heart, fog wall pushing back an outsider, independent progress); Hollow wind-up timing
  and dodge, guard block/break sounds; stamina HUD bar and debug weapon (2.5, 2.6); sounds and boss music.
- **Dedicated server:** 2.11 and 3.8 ran in single player only; the dedicated-server run is the 0.2.0 release
  task in ROADMAP.md.
- **Design:** DESIGN §10.1, §10.7, §10.8, §10.9 and the KIN/RES part of §10.10 are built as recommended and wait
  for the user's confirmation; §10.2–10.5 and the Sanctum weapons (§10.10) are open.
- **Art pass:** every mob and the Maiden use generated placeholder models and textures.
- **Balance:** real bosses need base values picked with level scaling in mind (the test boss at Lv 18 hit for
  37 melee with 404 HP, QA #8); the 0.2.0 balance pass.
- **Known limitation:** a dormant arena shrine looks lit to everyone; only resting is refused (a per-player look
  needs client-side progress).
