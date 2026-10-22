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
- [ ] **11.3 Smoldering Plate + Template.**
- [ ] **11.4 Mob-drop sets + unique pieces.**
- [ ] **11.5 Ashen Scythe, Warden's Scourge, Royal Ash Rapier.**
- [ ] **11.6 Huntsman's Crossbow.**
- [ ] **11.7 Consumables + Flask Ember.**
- [ ] **11.8 Chest loot + realm loot on mobs.**
