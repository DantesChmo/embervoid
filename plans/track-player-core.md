# Track 1 — player-core

Branch `claude/track-player-core-af49af`. Scope: contracts (config sections, `XpUtil`, leveling API), Milestone 2
brought in line with PLAN.md, Milestone 3 tasks 3.1–3.4. See [COORDINATION.md](../COORDINATION.md).

Status: `todo` · `in progress` · `done` · `sent` (merge request sent)

## Contracts

| Contract | Status | Where |
|---|---|---|
| Config sections | sent (e52b1d8) | `config/ConfigSection`, `SectionReader`, `ConfigFile`; `EmbervoidConfig.register`, `EmbervoidClientConfig.register` |
| `XpUtil` | sent (e52b1d8) | `player/XpUtil` |
| `levelUp(player, stat)` + stat read API | done | `stats/PlayerStats`, `DerivedStats`, `Stat` |

## Milestone 2 vs. current PLAN.md — findings

M2 was built on the old plan. Diffing the code on `main` (9c871b6) against PLAN 2.1–2.10:

| PLAN | Gap in the code | Fix |
|---|---|---|
| 2.1 | Config was one record: other tracks would have to edit it to add sections | Section API (contract, done) |
| 2.1 | No client preference file | `embervoid-client.json` + `EmbervoidClientConfig` (done; `staminaBar.hideWhenFull`) |
| 2.1 | `structures.*` spacing was in the config (nothing read it); PLAN: spacing lives in `structure_set` JSON | Removed (done) |
| 2.1 | No `remnant.secondDeath` | Add, `burn_xp` (default) / `burn_all` (§10.7) |
| 2.1 / 2.3 | No JUnit / `src/test` | JUnit 5 in `build.gradle` (asked the coordinator), tests for config and `XpUtil` |
| 2.2 | Client keeps its copy as an attachment on `ClientPlayerEntity`; PLAN: client-side holder, since the entity is recreated on respawn / dimension change | `ClientPlayerData` holder; `PlayerDataManager.get` on a client world reads the holder for the local player |
| 2.3 | XP math lived inside `AshRemnantEntity`; reclaiming used `addExperience` (float drift) | `XpUtil` (done); remnant takes/gives XP through it |
| 2.4 | Stamina max/regen read straight from config; PLAN wants a hook END feeds in 3.2 | `StaminaManager.getMax(player)` / regen through stats (3.2) |
| 2.6 | The special-attack contract section of the weapons doc (DESIGN §8) was never written | Draft below, sent to the coordinator (docs are theirs). Code: right-click on air already reaches `Item.use` (vanilla sends the use-item packet itself); on a block `Item.useOnBlock` passes and vanilla falls back to `use`, so no extra C2S packet is needed |
| 2.7 | Charges live on the item stack: five flasks = five times the heals (§10.6) | Charges and upgrades in `PlayerData`; the item only shows and spends them; one flask on first join (`PlayerData.receivedFlask`); a crafting recipe for a lost one |
| 2.7 | Tooltip/bar read stack NBT written by the server | Read the synced player data |
| 2.8 | Refill also when the player merely got up after 23000 ("dawn"), and refills every flask in the inventory | Refill only when the day rolled over while in bed (the night was skipped, also by other sleepers); refills the player's charges |
| 2.9 | A remnant that is no longer current always burns everything | Per `remnant.secondDeath`: `burn_xp` keeps the items (item-only remnant, still reclaimable), `burn_all` burns |
| 2.9 | Not immune to `/kill` or the void; explosions only via `damage` | Override `kill`, `tickInVoid`, `isImmuneToExplosion`, piston behavior |
| 2.10 | Safe spot = search around the death point first, last safe position only as a fallback; PLAN: last position on solid ground | Last safe position first (same dimension), search near death only when there is none, then world spawn |
| 2.10 | Second death always "burned" message | Message per `remnant.secondDeath` |

## Special-attack contract (draft for DESIGN §8 / weapons doc)

- **Trigger:** right-click (use) with the weapon in the main hand or offhand, in air or on a block. No new keybind.
- **Cost:** a fixed stamina amount per use, from the weapon. Allowed while any stamina is left (the pool bottoms
  out at 0, souls-style); with 0 stamina the use fails: HUD bar flashes, action-bar message, fizzle sound.
  Creative/spectator never spend.
- **Cooldown:** vanilla item cooldown per item type, started on a successful use; shown as the vanilla overlay.
- **Tooltip:** "Right-click: <name>" and "<cost> stamina, <cooldown>s cooldown".
- **Limits:** runs on the server only; no blocks broken or placed; no damage through walls; hits go through
  normal `damage()` with the player as attacker (so armor, mob levels and weapon scaling apply); no teleporting
  through blocks; vanilla left-click attacks, sprinting and blocking stay free.

## Task list

| # | Task | Status |
|---|---|---|
| C1 | Config-section API, client config, `XpUtil` | sent |
| C2 | Tests for config, `XpUtil`, stats, leveling, sleep refill (JUnit setup came from main) | done |
| M2a | Client-side player data holder (2.2) | done |
| M2b | Flask charges in player data, first-join flask, recipe (2.7) | done |
| M2c | Sleep refill only on a skipped night (2.8) | done |
| M2d | Remnant: `secondDeath`, `XpUtil`, immunity, stale resolve, safe spot (2.9, 2.10) | done |
| 3.1 | Stat storage | done |
| 3.2 | Stat effects | done |
| 3.3 | Weapon scaling at damage time | done |
| 3.4 | Leveling + debug commands, `levelUp` API → merge request | done |

## Choices made along the way (easy to change)

- Flask charges also refill on respawn after death (souls convention); not in PLAN.
- Flask recipe: shapeless glass bottle + diamond + coal/charcoal (decided) (`data/embervoid/recipes/healing_flask.json`).
- Sleep refill needs `doDaylightCycle` (with it off the time never jumps, so a skipped night cannot be told apart).
- Stat effect sizes (`stats.*` defaults): END +2 stamina, +2% regen, +0.005 knockback res; DEX +0.5% attack speed;
  STR/DEX +2% weapon damage; KIN +2% flask heal; RES +0.1 toughness, −1% fire damage (cap 50%); FOR +0.1 luck,
  all per full point. FOR's "bonus chance on mob drops" from §10.10 is not built.
- Stat modifiers are temporary (not saved); stats are the only source of truth, and the saved health is restored
  after they apply on join. Respawn after death: full health at the new max.
- Weapon scaling: melee = main-hand item when the source is `player_attack` (sweeps included, enchantment damage
  included in the scaled amount); projectiles carry a command tag `embervoid.scaling:<str>:<dex>` from the
  weapon that fired them.
- Russian stat names: Живучесть, Выносливость, Сила, Ловкость, Жар, Стойкость, Удача (ЖИВ ВЫН СИЛ ЛВК ЖАР СТК УДЧ).
- `leveling.levelCap` = 0 (no cap) until DESIGN §10.2 is decided.

## Stat API (for track-sanctum's Maiden screen)

`com.embervoid.stats.PlayerStats` — reads work on both sides (client: local player, synced):
`get(player, stat)`, `getAll(player)`, `level(player)`, `levelOf(int[])`, `derived(player)`,
`derivedWith(player, stat, value)` (preview), `derivedOf(int[])`, `levelUpCost(player)`,
`checkLevelUp(player, stat)` → `LevelUpResult` (`SUCCESS`, `STAT_CAP`, `LEVEL_CAP`, `NOT_ENOUGH_XP`, each with
`message()`); server: `levelUp(ServerPlayerEntity, stat)`, `set(ServerPlayerEntity, stat, value)`.
`Stat` has `id()`, `shortId()`, `displayName()`, `shortName()`, `byId(String)`. `DerivedStats` holds max health,
stamina, regen, knockback resistance, attack speed bonus, toughness, luck, fire reduction, flask multiplier,
STR/DEX damage bonuses. XP points: `XpUtil.getPoints(player)`.
