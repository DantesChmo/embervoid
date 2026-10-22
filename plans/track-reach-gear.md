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
- [x] **11.5 Ashen Scythe, Warden's Scourge, Royal Ash Rapier.**
  - `GearWeaponItem` (not a sword: no vanilla sweep): `Spec(damage, speed, scaling, kindlingShare, reach, bleed,
    igniteSeconds)`, attribute modifiers from the spec, durability 1800, enchantability 15, repaired with a
    Smoldering Ingot, special attack through `SpecialAbilities`, STR/DEX through `WeaponScaling.addMeleeRule`.
    `GearWeapons`: `ashen_scythe` (8, 1.1, STR 0.6/DEX 0.4), `wardens_scourge` (8, 0.9, reach 6, bleed 1, DEX 1),
    `royal_ash_rapier` (6, 2.0, reach 2.5, bleed 1, fire 3 s, DEX 0.7 + KIN 0.3).
  - "Ember part KIN" read as: 0.3 of the Rapier's melee hit scales with KIN; all ember damage dealt by a player scales
    with KIN in full; 2 % per effective point like STR/DEX (`gear.weapons.kindlingPerPoint`), in `GearDamage`.
  - Reach (`GearReach`): long reach = server raycast on the swing packet (`GearServerPlayNetworkHandlerMixin`, onHandSwing
    TAIL), skipped when a vanilla attack came within the last tick; blocks stop the ray. Short reach = the attack
    event refuses targets beyond it (survival). The attack event also records the swing's charge for weapon effects.
  - Scythe: sweep r 2.5 at 75 % of the charged hit around the target; full charge (> 0.9) → ash wave 6 × 3 ahead, 3
    ember damage. Specials (`GearAbilities`, numbers in `gear.weapons`): Harvest (r 3.5 spin at weapon damage, 25 st,
    4 s), Lash (pulls the first mob within whip reach ~4 blocks by velocity, knockback resistance applies, 20 st, 5 s),
    Lunge (dash ~4 blocks by velocity, thrust lands at once on the first mob within dash + reach: weapon damage, 2
    bleed, fire; 20 st, 3 s).
  - Recipes (shapeless): soul + 2 Smoldering Ingot + zone part — iron hoe (Scythe), chain (Scourge), golden sword (Rapier).
  - Mock test players always answer `isCreative()` true: code checks `getAbilities().creativeMode` instead.
  - Not done: Sharpness, Smite and the like cannot go on these weapons (vanilla `EnchantmentTarget.WEAPON` accepts
    swords only); Unbreaking and Mending work. A follow-up if wanted.
  - Textures: placeholder icons, not seen; the swing feel of 6-block reach not seen in game.
- [x] **11.6 Huntsman's Crossbow.**
  - `HuntsmansCrossbowItem extends CrossbowItem` (`GearWeapons.HUNTSMANS_CROSSBOW`, durability 1200, repaired with
    a Smoldering Ingot; vanilla crossbow enchantments apply). Vanilla reads charge time and makes bolts in static
    methods, so `GearCrossbowItemMixin` adds the extra charge to `getPullTime` (40 ticks, Quick Charge still −5 per
    level) and strengthens the bolt at the end of `createArrow`: damage ×1.5, Punch 3, pierce +1 (config
    `gear.crossbow`). Scaling DEX 0.5 / STR 0.5 through `WeaponScaling.addRangedRule`.
  - Recipe: Grieving Tree soul + 2 Smoldering Ingot + a crossbow. Model states (pull, pulling, charged, firework)
    registered client-side in `GearClient` like vanilla's; models hand-written in resources.
  - Poses: vanilla keys the "hold a loaded crossbow" arm pose and the two-handed first-person crossbow to
    `Items.CROSSBOW`; client mixins `GearPlayerEntityRendererMixin` (getArmPose) and `GearHeldItemRendererMixin`
    (hand render type, charged check, first-person item) wrap those `isOf` checks so
    `HuntsmansCrossbowItem.countsAsCrossbow` answers yes for it. Targets resolved by the mixin annotation processor;
    not seen in game (no client launch). Textures placeholders, not seen.
- [x] **11.7 Consumables + Flask Ember.** (config `gear.consumables`)
  - Hook commit `ed8e17b` in `item.HealingFlaskItem` (player-core's): `addHealMultiplier(ToDoubleFunction<PlayerEntity>)`,
    multiplied on top of the stats multiplier.
  - `flask_ember` (`FlaskEmberItem`): right-click once per player → +15 % flask healing for good; own attachment
    `embervoid:flask_ember` (persistent, copied on death), synced to the owner (join, respawn, world change) only for
    the flask tooltip; a second one is refused with a message.
  - `ash_bomb` (thrown, entity `embervoid:ash_bomb`, `FlyingItemEntityRenderer`): bursts on impact, 4 ember damage
    and 3 s fire to mobs within r 2, flame-ring particles. Decided: no fire blocks (no griefing, no spreading fires).
  - `smoldering_resin`: coats the weapon in the other hand (swords, axes, tridents, realm weapons; NBT
    `EmbervoidResinUntil`), for 60 s its landed melee hits ignite for 4 s (`GearEffects.afterDamage`).
  - Cinder fuel: the Cinder Shard itself burns 3200 ticks (= 2 coal) — no extra item.
  - `amber_balm` (drink, 32 ticks, leaves the bottle): cures Bleeding and Poison, Regeneration I 8 s.
  - `amber_lantern_oil`: poured into an Ember Lantern held in the offhand (NBT `EmbervoidOilUntil`) it burns 10 min;
    a lit offhand lantern gives Night Vision (refreshed every second). No dynamic light (vanilla has none; placing
    light blocks was judged too risky). `GearConsumables.carriesLitLantern(player)` for the Crypt Keeper / Crypt Moths.
  - `whetstone`: repairs 25 % of a realm weapon (melee or the crossbow) in the other hand. Realm weapons also repair
    on an anvil with a Smoldering Ingot (since 11.5).
  - Recipes (shapeless): bomb 2 Cinder + gunpowder → 2; resin 2 Cinder + charcoal; balm 2 Amber + bottle; oil Amber +
    bottle + coal/charcoal; whetstone Grave Iron Ingot + 2 cobbled ash stone → 2.
  - Client (`GearClient`): bomb renderer, Flask Ember sync receiver, tooltip lines for burning resin / oil.
  - Textures placeholders, not seen; bomb flight and burst not seen.
- [x] **11.8 Chest loot + realm loot on mobs.**
  - Chest tables (datagen, `GearDataGen.ChestLoot`): common = 3–5 rolls of food, arrows, iron, gold, torches,
    bones, string, Ash Bombs, Amber Balm + the zone material (Necropolis also lantern oil); treasure = 3–5 rolls of
    gold, diamonds, emeralds, consumables, materials + Smoldering Ingot 12 %, template 25 %, a zone set piece 30 %, the
    zone weapon 4 % (reliquary adds artifacts and rings); arena = a template, an Ash of Rematch, 2–3 zone consumables.
    Citadel common = supplies + all three materials; Citadel treasure = ingot 30 %, template 35 %, any set piece 35 %,
    the Rapier 4 %.
  - `GearLoot` (`LootTableEvents.MODIFY`, built-in tables only, like `ReliquaryLoot`): per mob id the zone material
    (10 % natural / 50 % ×1–2 placed), a set piece (3 % natural / 15 % placed tank or elite, player kills) and the
    unique (0.5 % / 4 % from the zone's elite — Ashen Carrion, Grave Warden, and the Bark Hollow for the elite-less
    Woods; Furnace Golem and Smoldering Knight 4 % for the Cuirass). Looting adds a little; Fortune's extra roll
    covers these pools. Config `gear.loot`.
  - Ash Stalker and Crypt Moth (not in any set's list) drop materials and the unique only.
