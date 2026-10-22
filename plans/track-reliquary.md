# Track D — `track-reliquary` (0.4.0)

PLAN 11.9–11.13: Reliquary screen and slots, remnant and stat hooks, 8 artifacts, 6 rings, their drops.
Design: DESIGN §4.5 "Loot" (Artifacts, Rings, Reliquary). Owns `com.embervoid.reliquary`,
`com.embervoid.client.reliquary`, the `embervoid:artifacts` / `embervoid:rings` tags, their assets, config `reliquary`.

## Status

| Task | Status | Commit |
|---|---|---|
| 11.9 Reliquary: data, sync, screen | merged | 3aa4b5a |
| 11.10 Remnant + stat points (hooks in `player`, `stats`) | done | see git log |
| 11.11 8 artifacts | todo | |
| 11.12 6 rings | todo | |
| 11.13 Accessory drops | todo | |

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
- Not done (follow-up): the Maiden's level-up screen still shows own stat values; derived numbers there include
  artifact points.
- **Tests:** accessories into the remnant and back (also through remnant NBT), accessories alone leave a remnant,
  taken slot → inventory, keepInventory keeps them, bonus points change max health but not stat value, level or
  cost (test-only bonus source; real artifacts in 11.11).
