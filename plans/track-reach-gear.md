# Track C — reach-gear (PLAN 11.1–11.8)

Branch `claude/track-reach-gear-8a94a4`. Rules: [COORDINATION.md](../COORDINATION.md). Design: DESIGN §4.5 "Loot",
§8.1, §10.11.

Owns `com.embervoid.reach.gear` (main), `com.embervoid.client.reach.gear` (client, datagen in `GearDataGen`),
`loot_tables/chests/ashen_reach/**`, its recipes, tags and assets (`tools/gear_placeholders.py` draws the item
textures). Config section `gear`. Tests: `GearTests` (GameTests), JUnit under `src/test/.../reach/gear`.

## Tasks

- [x] **11.1 Materials, boss items, chest table stubs.**
  - `GearItems`: `cinder_shard`, `royal_amber`, `raw_grave_iron`, `grave_iron_ingot`, `smoldering_ingot`
    (uncommon, fireproof), `smoldering_template` (a vanilla `SmithingTemplateItem`, armor only, like the netherite
    upgrade; its recipe and the smithing recipes are 11.3), the four souls `<boss>_soul` (epic, fireproof, stack 16),
    `ash_of_rematch` (stack 16; its use is reach-bosses' 12.7), `flask_ember` (`FlaskEmberItem`, epic; effect in 11.7).
  - Souls, ingot, Ash of Rematch and Flask Ember carry one grey lore line (`LoreItem`, key `<item key>.tooltip`):
    the realm's story told in item descriptions.
  - Recipes (datagen): Smoldering Ingot shapeless 4 Grave Iron Ingot + 2 Cinder Shard + 2 Royal Amber; Grave Iron
    smelting (1.0 XP, 200 t) and blasting (100 t); Ash of Rematch shapeless 4 Cinder Shard + 1 Royal Amber + 1 bone
    (§10.11 as recommended).
  - `GearLootTables`: ids of the 11 chest tables (`common` / `treasure` / `arena` per zone, `citadel_common`,
    `citadel_treasure`), generated empty; filled in 11.8.
  - Decided: Russian names "Осколок золы", "Королевский янтарь", "Могильное железо", "Тлеющий слиток", "Пепел
    реванша", "Уголёк фляги"; souls "Душа <boss>" with my own boss names (Пепельный колосс, Скорбящее древо,
    Хранитель склепа, Тлеющий владыка) — reach-bosses' entity names should match or the coordinator aligns them.
  - Textures: placeholders, not seen in game.
- [x] **11.2 Bleeding + `embervoid:bleed_immune`.**
  - `Bleeding` (status effect `embervoid:bleeding`, harmful): `Bleeding.apply(target, stacks, source)` adds levels
    (cap `gear.bleeding.maxLevel` 3) and refreshes the duration (`durationTicks` 120); `Bleeding.level(entity)`.
    Damage `damagePerLevel` 0.5 × level once a second, timed by the entity's age so refreshing on every hit neither
    adds nor skips ticks. Numbers in `BleedRules` (JUnit).
  - Damage type `embervoid:bleeding` in `bypasses_armor`, `bypasses_cooldown` (the hurt cooldown of melee hits
    would swallow it otherwise) and `no_impact`; death messages "bled out" (with the last attacker if any).
  - Kill credit: `apply` with a player source calls `setAttacking(player)`, so a bleed kill drops XP and player-kill
    loot like a hit would.
  - Tag `embervoid:bleed_immune` (datagen, all entries optional): restless_bones, furnace_golem, bark_hollow,
    ash_colossus, grieving_tree. Checked in `apply` and on every damage tick (an `/effect` command cannot bypass it).
  - Debug: `/embervoid gear bleed <targets> [stacks]`. Effect icon: placeholder, not seen.
- [x] **11.3 Smoldering Plate + Template.**
  - `GearArmorMaterial` (record implementing `ArmorMaterial`; name `embervoid_<name>`, so the worn layers live in
    `assets/minecraft/textures/models/armor/embervoid_<name>_layer_1|2.png` — 1.20.1 builds that path without a
    namespace). `GearArmorItem`: an `ArmorItem` with optional extra attribute modifiers (fixed name-based UUIDs) and
    tooltip lines `embervoid.gear.<key>.piece` / `.set`.
  - `GearArmor.SMOLDERING`: 3/8/6/3, toughness 2.5, durability multiplier 38 (diamond 33 × 1.15), enchantability 12,
    netherite equip sound, repaired with Smoldering Ingot. Items `smoldering_helmet|chestplate|leggings|boots`
    (fireproof). `GearArmor.pieces(entity, material)`, `fullSet(...)`.
  - Template: shaped C S C / C D C / C S C (6 Cinder Shard, 2 ash stone, 1 diamond). Smithing transform per piece
    (`smoldering_<piece>_smithing`), enchantments kept. Item models with vanilla trim overrides (`registerArmor`).
  - `GearDamage`: −`gear.smoldering.fireReductionPerPiece` (0.08) of `#is_fire` damage per piece, applied at the
    start of `LivingEntity.damage` through `mixin.GearLivingEntityMixin` (one `@ModifyVariable`, like the stats one);
    full set: `on_fire` damage cancelled (`ALLOW_DAMAGE`); lava and fire blocks still hurt.
  - Textures: placeholder icons and layers, not seen in game.
- [x] **11.4 Mob-drop sets + unique pieces.**
  - Sets (`GearArmor`, items `<set>_helmet|chestplate|leggings|boots`): Reaper's Garb `reaper` 2/6/5/2 (15),
    toughness 0, repaired with Cinder Shard; Huntsman's Leathers `huntsman` 2/7/5/3 (17), toughness 1; Necropolis
    Warden `warden` 3/8/6/3 (20), toughness 2, −1.25 % speed per piece (−5 % full). Durability ×15 / ×20 / ×30.
  - Set bonuses (config `gear.sets`): Reaper +10 % speed and Warden +0.3 knockback resistance are attribute modifiers
    toggled on `ServerEntityEvents.EQUIPMENT_CHANGE` (`GearEffects`); Huntsman +15 % projectile damage from the wearer
    and Warden −15 % damage while blocking in `GearDamage.modifyIncoming`.
  - Uniques (rare, fireproof, each with its own material so it never counts towards a set; config `gear.uniques`):
    `scarecrow_mask` — `getAttackDistanceScalingFactor` × 0.5 (the vanilla mob-head mechanism) and
    `GearArmor.hiddenFromAmbush(entity)` for reach-mobs' Ash Stalker; `huntsman_greaves` — Speed I 4 s after a kill,
    fall damage −4 (a fall of up to 7 blocks does not hurt at all); `drowned_sabatons` — Depth Strider III
    (`GearEnchantmentHelperMixin`); `furnace_heart_cuirass` — below 30 % health a fire nova (r 4, knockback, 6 ember
    damage), 60 s vanilla item cooldown shown on the cuirass. Players only for the nova.
  - Damage type `embervoid:ember` (`GearDamage.ember(attacker, source)`): the realm's flame, armor applies; used by
    the nova, later the scythe's ash wave and Ash Bombs.
  - Mixins: `GearLivingEntityMixin` gained a `damage` RETURN hook and the mask's scaling; new `GearEnchantmentHelperMixin`.
  - Test players have no connection, so tests call `playerTick()` where a real player's tick matters.
  - Textures: placeholder icons and layers, not seen in game.
- [ ] **11.5 Ashen Scythe, Warden's Scourge, Royal Ash Rapier.**
- [ ] **11.6 Huntsman's Crossbow.**
- [ ] **11.7 Consumables + Flask Ember.**
- [ ] **11.8 Chest loot + realm loot on mobs.**
