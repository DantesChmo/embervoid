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
    - Sounds (coordinator's request after L3): own events in `SanctumGearSounds` (registered from `ModSounds`),
      `item.sanctum_longsword.wide_cleave` (vanilla sweeps at pitch 0.8) and `item.ashbearers_mace.ground_slam`
      (explosions at volume 0.5, pitch 1.4); subtitles en + ru in the track's lang block; checked in
      `SanctumGearTests` through `SoundLog` and by `SoundTests` (every event resolves, has subtitles). Not heard.
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
- [x] **L2 Overworld balance pass.** Write-up: [docs/balance-overworld.md](../docs/balance-overworld.md).
  - `OverworldData` (ten hostiles: zombie, spider, skeleton, creeper, witch, enderman, vindicator, Ash Ghoul, Hollow,
    Ember Wisp; base levels from the `mobs` config defaults) and `OverworldModel` (bands, fights per difficulty, a
    pack, the Hollow through iron, route XP, damage per second) on `BalanceModel`'s formulas. `BalanceModel` got
    leather, stone sword, iron axe, the Sanctum weapons, `HitKind.MAGIC` and `fight(..., difficulty)` — no Realm 1
    number moved (`BalanceTargetsTest` green, the Realm 1 tables unchanged).
  - Targets in `OverworldTargetsTest`: Hollow 5.5–6.5 through iron on Normal (6.0); bands 3–20; common mobs ≤ 30 %
    of a fresh iron player (Normal) and ≤ 50 % of a fresh leather one (Easy); a creeper at 4 blocks ≤ 60 %; one
    Hollow and a ghoul pack of 4 within health + starting flask; route XP 400–650 and level 10 ± 1 (587 → 10).
    All held with the current numbers: **no number changed**, no global change.
  - Outliers documented and kept (write-up §5): the creeper at 3 blocks kills a fresh iron player on Normal (22 vs
    vanilla 11.5; base level 3 would fix it if play says so), a provoked enderman, vindicators, witches, a ghoul pack
    against leather on Normal. D7 re-checked: keep.
  - Sanctum weapon target `BalanceTargetsTest.sanctumWeaponsSitBelowTheRealm`: the literal "below every realm weapon
    per hit and per second" is impossible without going under iron (Rapier weakest per hit, Scourge per second);
    built as "beats no realm weapon on both, the realm's best beats it on each, per second ≤ diamond sword / axe".
    Sent to the coordinator as a QUESTION; L1 numbers unchanged.
  - Engine: `BalanceFightTests.overworldNumbersAreTheEntities` and `overworldMobsHitAsModelled` (Ash Ghoul, Hollow
    for level 1 and 10, Ember Wisp, zombie, a real skeleton arrow, the creeper's own explosion at 2 blocks with
    distance and exposure checked); absorption soaks hits above the player's health. All match to two decimals.
  - Test fix (own commit): `ReachMobTests.placedEnemiesTakeTheirPlacedState` laid 14 mobs in a row out to x = 40;
    after new tests shifted the GameTest grid the last ones fell into a chunk that does not tick entities and never
    took their placed state (the coordinator branch alone passes). Now a 4 × 4 grid inside the test area.

## Needs a client

The weapons' look in hand, the specials' particles and sounds and their feel.
