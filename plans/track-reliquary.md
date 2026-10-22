# Track D — `track-reliquary` (0.4.0)

PLAN 11.9–11.13: Reliquary screen and slots, remnant and stat hooks, 8 artifacts, 6 rings, their drops.
Design: DESIGN §4.5 "Loot" (Artifacts, Rings, Reliquary). Owns `com.embervoid.reliquary`,
`com.embervoid.client.reliquary`, the `embervoid:artifacts` / `embervoid:rings` tags, their assets, config `reliquary`.

## Status

| Task | Status | Commit |
|---|---|---|
| 11.9 Reliquary: data, sync, screen | merged | 3aa4b5a |
| 11.10 Remnant + stat points (hooks in `player`, `stats`) | merged | da98386 |
| 11.11 8 artifacts | merged | 2ff719e |
| 11.12 6 rings | merged | 1f53184 |
| 11.13 Accessory drops | done | see git log |

## 11.9 — data, sync, screen

- **Slot kinds** (`reliquary.SlotKind`, `SlotKinds`): a kind = name + item tag + default count. `artifact`
  (`#embervoid:artifacts`, 2) and `ring` (`#embervoid:rings`, 2). A later realm adds a kind with one constant.
- **Counts** come from config `reliquary.slots.<kind>` (0–8). Decision: the tags say *what fits*, the config says
  *how many*; that keeps "slot kinds and counts are data" without a reload listener. Items left in slots that no
  longer exist (count lowered, kind removed) are kept in the save and handed back to the inventory on join.
- **Attachment** `embervoid:reliquary` (`ReliquaryManager.RELIQUARY`): stacks by kind name and slot, persistent,
  copied on death (with keepInventory they stay; otherwise 11.10 has already emptied it). Synced to the owner
  (`embervoid:reliquary` S2C) on join, respawn, dimension change and every change. `ReliquaryManager.CHANGED` fires
  after a change and on join/respawn: effects (11.10–11.12) hang off it.
- **Rules** (decided here): one item per slot; the same item cannot be worn in two slots (each accessory counts once).
- **Menu** `ReliquaryScreenHandler` (`embervoid:reliquary` screen handler, extended type carrying slot counts):
  accessory slots first, then main inventory and hotbar. Shift-click equips into the first free fitting slot, or
  moves main ↔ hotbar like vanilla; shift-click on an accessory puts it back into the inventory. Opened by C2S
  `embervoid:open_reliquary` (button, key) or `/embervoid reliquary <player> open`.
- **Screen** `client.reliquary.ReliquaryScreen`: 176×166 like the inventory, drawn with plain fills in vanilla
  container colours (texture in the art pass); player model box in the middle, kinds alternate left/right of it in
  columns of up to 3 (`reliquary.ReliquaryLayout`, pure, JUnit-tested); empty slots show a kind icon
  (`textures/item/empty_<kind>_slot.png`) and name the kind on hover. No title (like the vanilla inventory).
- **Button** on the survival inventory screen right of the recipe book button (same row, follows it when the book
  opens), vanilla button with the reliquary icon (`textures/gui/reliquary_button.png`), tooltip with the key.
  Not on the creative screen (the key works there).
- **Key** `key.embervoid.reliquary`, default **R** (unbound in vanilla), category "Embervoid"; closes the screen too.
- **Debug:** `/embervoid reliquary <player> [open|clear]`.
- **Tests:** `ReliquaryTests` (GameTest; the gametest mod tags rabbit foot / heart of the sea as artifacts and gold
  nugget as a ring): menu equip → attachment, saved in player NBT, sync packet carries it; kind rules, one item,
  no duplicates; unequip to inventory; leftovers handed back on join. `ReliquaryLayoutTest` (JUnit).
- **Not seen:** the screen's look, the model, the button position on the inventory screen (no client launch).
- Placeholder textures: `tools/reliquary_placeholders.py`.

## 11.10 — remnant, stat points

- **Hook in `player`** (own commit): `player.DeathStashes` — item stores outside the vanilla inventory register a
  `Stash` (`takeAll`, `restore(index, stack)`, `isEmpty`) under an id. On death with `remnant.mode` `xp_and_items`
  the Ash Remnant takes every stash with the inventory (a death with only accessories still leaves a remnant); with
  `xp` / `off` stashes drop at the player like the inventory; `keepInventory` keeps them (attachment copied).
  `AshRemnantEntity` saves stashes in NBT (`Stashes`: id → list of stacks with `Index`); reclaiming puts each stack
  back through `restore`, what does not fit (slot taken, slot gone) goes to the inventory or the ground.
- **Hook in `stats`** (own commit): `PlayerStats.BonusSource` / `registerBonusSource`; `bonus(player)`,
  `effective(player, stat)`. `derived` / `derivedWith` include bonus points; `get`, `level`, `levelUpCost`,
  `checkLevelUp` (stat cap) do not. A source calls `StatEffects.apply` when its bonus changes.
- **Reliquary side:** `ArtifactItem` (stat points, tooltip), `ReliquaryEffects` registers the bonus source (worn
  artifacts), `StatEffects.apply` on `ReliquaryManager.CHANGED`, and the Reliquary as stash `embervoid:reliquary`
  (index = slot order, a recovered accessory goes back into its old slot when free).
- Maiden's level-up screen (separate commit in `sanctum`, after 11.13): `StatSheet.Stat.bonus` carries the bonus
  points; the screen shows them in green left of the own value ("+3 10"). Not seen.
- **Tests:** accessories into the remnant and back (also through remnant NBT), accessories alone leave a remnant,
  taken slot → inventory, keepInventory keeps them, bonus points change max health but not stat value, level or
  cost (test-only bonus source; real artifacts in 11.11).

## 11.11 — artifacts

- `reliquary.ReliquaryItems`: Last-Harvest Grain (VIT), Ploughman's Horseshoe (END) — Fields; Royal Hound Fang (DEX),
  Amber Tear (KIN) — Woods; Gravestone Medallion (RES), Ferryman's Coin (FOR), Warden's Chain (STR) — Necropolis;
  Seal of the Ashen King (every stat) — Citadel. Uncommon, the Seal epic; stack size 1.
- Points from config `reliquary.artifacts.points` (3) and `sealPoints` (1), read live (`ArtifactItem` takes a supplier).
- Tooltip: a flavour line (`item.embervoid.<id>.desc`, the Greedy King story), "When worn:" and "+3 Vitality".
- Datagen `client.reliquary.ReliquaryDataGen`: item models, `#embervoid:artifacts`. Textures from
  `tools/reliquary_placeholders.py`.
- **Test:** every artifact is in the tag and only fits artifact slots; worn, it raises exactly its stats by the
  config points (`effective`), own values, level and cost unchanged, max health / luck follow; two stack; off restores.

## 11.12 — rings

`RingItem` (flavour + effect line in the tooltip), `RingEffects` (logic, both sides where the client moves its own
player), mixins `ReliquaryLivingEntityMixin` / `ReliquaryPlayerEntityMixin`, numbers in config `reliquary.rings`.

| Ring | Zone | How |
|---|---|---|
| Courier's Ring | Fields | movement speed modifier ×1.15 (MULTIPLY_TOTAL, on top of vanilla sprint), added/removed in `setSprinting` (both sides) and on ring change |
| Ring of Ash Feathers | Fields | fall distance −5 before vanilla's 3 (no damage up to 8 blocks, 5 less beyond); client caps fall speed at 0.5 b/t — decided: not while sneaking, flying, gliding, swimming, climbing |
| Ring of the Fog Stalker | Woods | client: `RealmFog.setClearing(embervoid:ring/fog_stalker, 2)` (9.1 API, already on main); arena fog is not clearable, so arenas are unaffected |
| Delver's Ring | Woods | `getBlockBreakingSpeed` ×1.2 (Haste I) on `#embervoid:realm_ores`, both sides; no status icon |
| Drowned Man's Ring | Necropolis | `canBreatheInWater` true |
| Ring of Embers | Citadel | `ALLOW_DAMAGE`: the wearer's direct melee hit (player attack) sets the target on fire 2 s |

- `#embervoid:realm_ores` is reach-world's (9.5, not merged yet): an empty tag until then. The gametest mod adds
  `minecraft:iron_ore` to it (optional entry) for the Delver's test.
- Gametest-only accessor `gametest.mixin.ServerPlayerEntityAccessor` (join invulnerability: test players never tick).
- Effect lines in the tooltip name the default numbers (15 %, 8 blocks, 2 s).
- **Tests:** rings fit ring slots only; Courier's speed while sprinting, not walking, removed with the ring; Ash
  Feathers fall damage (7.9 free, 10 → 2) and the fall cap rules; Fog Stalker factor; Delver's ×1.2 on a realm ore,
  not on stone; Drowned Man's air under water (base ticks run by the test); Ring of Embers ignites on melee.
- **Not seen:** the slow fall feel, the fog clearing on screen.

## 11.13 — drops

- `reliquary.ReliquaryLoot`: `LootTableEvents.MODIFY` (built-in tables only, not datapack replacements) adds two
  pools — one artifact, one ring, equal weights within the zone — to:
  - treasure chests `embervoid:chests/ashen_reach/<zone>_treasure` (fields, woods, necropolis, citadel): 8 % / 5 %;
  - elite mob tables `embervoid:entities/<mob>`: 2 % / 1 %, `killed_by_player`.
- Decisions: each zone drops only its own accessories (DESIGN "why each zone is worth doing"); elites are the
  bestiary's elite role — Ashen Carrion (Fields), Grave Warden (Necropolis), Smoldering Knight (Citadel). The Woods
  have no elite, so their accessories come from the hunting lodge chest only. Plain chance (FOR's extra drop already
  rerolls mob loot).
- Chances in config `reliquary.drops`.
- **Tests:** 4000 rolls of every treasure chest table (real tables): rates within 5σ, only the zone's items; 10 000
  rolls of each elite table (real tables for Carrion and Warden, the injection on an empty table for the Knight,
  which does not exist yet) with a player kill, none without.
