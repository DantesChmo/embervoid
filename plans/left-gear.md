# Track L-A — left-gear (PLAN L1–L2)

Branch `claude/track-reach-world-85fd2c` (worktree), synced from the coordinator branch `claude/tails-020-030-46a40d`.
Rules: [COORDINATION.md](../COORDINATION.md) "Tracks — leftovers". Design: DESIGN §3.2, §5, §8.1, §10.10.

Owns `com.embervoid.sanctum.gear` (main), `com.embervoid.client.sanctum.gear` (datagen), `com.embervoid.balance`,
`docs/balance-overworld.md`, the `sanctum_outer_hall` loot table, the two weapon textures
(`tools/sanctum_weapon_art.py`). Config section `sanctumGear`. Tests: `SanctumGearTests` (GameTests),
`SanctumWeaponRulesTest` (JUnit), the balance tests.

## Tasks

- [x] **L1 Sanctum weapons.**
  - `SanctumWeapons`: `sanctum_longsword` (6.5 dmg, speed 1.6, DEX 1, Wide Cleave) and `ashbearers_mace` (8 dmg,
    speed 0.9, STR 1, Ground Slam); durability 600, uncommon, not fireproof.
  - `SanctumWeaponItem extends reach.gear.GearWeaponItem`: so the weapons get their STR/DEX scaling (the
    `GearWeaponItem` melee rule in `WeaponScaling`), the realm weapons' sword enchantments
    (`GearWeapons.takesSwordEnchantment`) and the right-click flow without touching those classes. Repaired with iron
    ingots, enchantability 14 (iron). Side effect, accepted: the Whetstone (a realm consumable) also repairs them.
  - `SanctumAbilities`: both through `item.ability` (server only, `damage()` with `playerAttack`, no blocks, nothing
    the player cannot see is hit — `canSee`, so no hits through walls). The "weapon's hit" is the player's attack
    damage attribute with the weapon in the main hand (the weapon's own damage from the offhand); STR/DEX scaling then
    applies as for a melee hit. Marker armor stands skipped like vanilla's sweep.
    - Wide Cleave: arc 90°, 3 blocks, ±1 block up/down, 1.25×, 25 stamina, 60 ticks.
    - Ground Slam: ring r 3, ±1 block, 0.75×, knockback 0.8 (vanilla's damage knockback 0.4 comes on top), Slowness
      **II** (amplifier 1) for 40 ticks — decided: Slowness I (−15 %) is hardly felt in 2 s; 30 stamina, 100 ticks.
    - All numbers in the new config section `sanctumGear` (README config reference: coordinator, L8).
    - Sounds: vanilla events (sweep; a quiet high-pitched explosion for the slam). Own `embervoid:` events are
      left-audio's call (L3).
  - Guard break: new item tag `#embervoid:breaks_guard` (the mace), `HollowEntity.breaksGuard(stack)` = an axe or the
    tag. One-line change in `HollowEntity.damage` plus the tag constant and the helper (separate from left-audio's
    sound hooks).
  - Loot: a new pool in `chests/sanctum_outer_hall`: longsword 6, mace 6, empty 88 — 6 % each per chest, never both
    in one chest. A typical Sanctum has 3–5 chests (CORE §4; mean 4): per Sanctum a given weapon 1 − 0.94⁴ ≈ 22 %,
    either weapon ≈ 40 % (1 − 0.88⁴), 0.48 weapons expected — about one per two Sanctums.
  - Lang en_us + ru_ru: one block after `item.embervoid.sanctum_map`. Russian: «Длинный меч Святилища», «Булава
    пеплоносца», «Широкий взмах», «Удар оземь».
  - Textures: `tools/sanctum_weapon_art.py` (on `art_kit` / `reach_item_art` helpers), checked with
    `tools/model_preview.py items`. Not seen in game.
  - Tests: `SanctumGearTests` — cleave arc (front and 27° in, beside / behind / 4 blocks out, wall), slam ring
    (three sides hit, slowed, knocked away; 5 blocks out and a walled mob not), stamina cost, exact cooldown, refusal
    with 0 stamina, the mace breaking a raised Hollow guard (the longsword not), item numbers, iron repair, sword
    enchantments, the loot rate over 4000 chests. JUnit `SanctumWeaponRulesTest`.
- [ ] **L2 Overworld balance pass.**

## Needs a client

The weapons' look in hand, the specials' particles and sounds and their feel.
