# Embervoid — Backlog

Ideas that were discussed and set aside: not planned for any milestone, but worth keeping. Each entry says what
was considered and where it stopped, so a later design pass does not start from zero. Committed work lives in
[ROADMAP.md](ROADMAP.md).

## Respec (DESIGN §10 q5)

**Status:** not now (decided 2026-10-22). Players keep the points they buy.

What the built code means for any future respec:
- Spent XP is not stored: the level is derived from the stat sum (`PlayerStats.levelOf`). Moving points is cheap
  (level and next-point cost stay the same); refunding XP means recomputing it from the cost formula, which goes
  wrong if the config coefficients changed.
- A stat must not drop below 10 (`Stat.BASE`), or a reset hands out free points (`Stat.MIN` is 1).
- Lowering VIT/END must clamp current HP/stamina to the new maximum.

Variants considered:
- **A. Full reset into a point pool** — stats back to 10, level kept, freed points re-spent at the Maiden for
  free. Source: first realm-boss kill, rare repeat drop. Needs an unspent-points field and a screen mode.
- **B. Full reset with XP refund** — level back to 1, XP returned. Little UI, but breaks on config changes, huge
  sums at high level are lost to a second death, opens XP transfer in multiplayer.
- **C. Move one point** — a craftable consumable shifts 1 point between stats. Cheap fix for small mistakes.
- **D. Maiden service** — reset for XP and/or an Ember Heart, limited per completed realm. Collides with Ember
  Heart uses.
- **E. Reset one stat** — that stat back to 10, its points into the pool of A.

Leaning, if it comes back: A (personal item, used at the Maiden, first realm-boss kill), C later as an add-on.
Open: per-player or tradeable item, a lifetime limit, weapon requirements after a reset (penalty, not unequip).

## Maiden as merchant, other currencies (ROADMAP "Later")

**Status:** not now (decided 2026-10-22). Revisit: whether the mod gets merchants at all, and whether vanilla XP
stays the only currency.

What the built code means for any future merchant:
- `MaidenTalk.talk` is the single hook for what talking to the Maiden does; a trade tab or a choice screen goes
  there. The level-up request path (server-side checks: alive, within `AshenMaidenEntity.REACH` of that Maiden,
  enough XP) is the model for a purchase request.
- An XP price needs a custom screen; the vanilla `MerchantScreen` only takes items as payment. A per-player
  `Merchant` object lets several players trade at once, each with their own offers.
- Per-player progress (`PlayerProgress`: beaten bosses, completed realms) is what unlocks stock.

Variants considered:
- **A. Maiden sells for XP** — a trade tab in her screen; stock in datapack JSON (`item`, `price`,
  `requires: boss | realm`); consumables and conveniences, materials only of completed realms. Cheapest.
- **B. A new currency (embers)** — an item dropped by realm mobs and chests; keeps XP for leveling only. Can be
  passed between players and goes into the remnant.
- **C. Ember Heart exchange** — the Maiden turns a boss's heart into one of its uniques (Demon's Souls style).
  Collides with the weapons doc and weapon upgrades.
- **D. A separate Sanctum trader for emeralds** — vanilla trade screen; emeralds tie the loop to villagers and
  clash with the theme.
- **E. Rescued NPCs from realms** — traders found in realms 2–4 move to the Sanctum, each with its own currency.
  Content for realm design passes; expensive.

Never sold, whatever the variant: Ember Hearts and boss uniques (except via C), material or gear of a realm the
player has not completed, Flask Shards (at most one or two per realm, once), anything above the loot ladder.

Leaning, if it comes back: A now-ish, B only if XP turns out overloaded (a `currency` field in the stock JSON),
C with the weapons doc, E with realms 2–4. Open: currency, Maiden or separate NPC, limited or endless stock,
selling back for XP, whether she sells the respec item.

## Weapon upgrades / smithing within a tier (ROADMAP "Later")

**Status:** not now (decided 2026-10-22). Revisit with the weapons doc (DESIGN §8), once the Smoldering tier exists.

What the built code means for any future upgrade system:
- Weapon scaling is a damage multiplier applied when damage is dealt (`StatEffects` → `WeaponScaling`), with
  `Scaling(strength, dexterity)` shares and `addMeleeRule` / `addRangedRule` for own weapons: an upgrade or an
  infusion can change the scaling share per stack. No scaling letters exist yet.
- `SpecialAbility.perform` gets the stack, so ability damage can read an upgrade level; `staminaCost()` and
  `cooldownTicks()` take no stack, so cheaper or faster specials per upgrade need a signature change.
- A damage bonus on vanilla weapons: Fabric `ModifyItemAttributeModifiersCallback` (shows in the tooltip) or a
  multiplier at damage time (no tooltip line). Writing NBT `AttributeModifiers` on a vanilla item drops its defaults.
- No gear tier exists yet; the loot ladder (DESIGN §6) is the constraint: a fully upgraded weapon of tier T should
  be about the base weapon of tier T+1.

Variants considered:
- **A. +1…+5 per tier (souls)** — about +3 % base damage per level; shards from the realm's zone mobs, a boss
  material for the last step; each realm's material upgrades only its own tier. Done at the smithing table (custom
  recipe, no new GUI), later a Sanctum forge block or the Maiden. Vanilla diamond/netherite up to +3 with a
  Sanctum material. Cheapest (S–M).
- **B. Infusions** — one per weapon: Heavy (STR), Sharp (DEX), Ember (KIN, needs ember damage), Steady (cheaper /
  faster special). Changes scaling, not the power ceiling; gems per zone. Build identity without classes (M).
- **C. Sockets with rolled gems** (Bloodborne / Diablo) — strongest loot chase, needs a GUI, rolls break the
  loot ladder most easily (L).
- **D. Tempering by use, paid in XP at the Maiden** — the weapon gathers kills, the Maiden tempers it for XP plus
  a tier catalyst. Competes with leveling; grind; switching weapons resets it (M).
- **E. Ascension** — a +max weapon plus the next realm's material reforges into the next tier at +0, keeping its
  infusion. Add-on to A; needs parallel weapon lines per tier.

Leaning, if it comes back: A first (smithing table), B-lite later (Ember once ember damage exists), E decided with
Realm 2 weapons; not C or D. Open: levels, infusions or both; +5 or +10; where it happens; XP on top of
materials; vanilla weapons and their cap; whether upgrades touch the special attack; armor too; ascension.

## Status effects

**Status:** to design (asked by the user 2026-10-22). The mod needs its own set of status effects, worked out
together rather than one per weapon. **Bleeding** is the first: Obelisk 1 weapons already apply it (a plain
damage-over-time placeholder in 0.4.0, DESIGN §4.5).

To decide: the full list (bleeding, rot/poison for Realm 2, frost, curse…); vanilla `StatusEffect` (simple, shows
in the HUD) or souls-style buildup meters that burst; how RES resists them (DESIGN §5.2 already promises
resistance to fire, poison, wither); which mobs are immune (skeletons, constructs: an entity tag); cures
(consumables, the flask, shrines); HUD for buildup.

## Rings and artifacts

**Status:** to design (asked by the user 2026-10-22). Many rings and artifacts across the whole mod, not tied to
one obelisk. Two kinds of worn items: an **artifact** raises a character stat (e.g. +3 Vitality), a **ring** gives
a unique effect (water breathing, sprint speed…).

Already decided for 0.4.0 (DESIGN §4.5): the Realm 1 set (8 artifacts, 6 rings) and the **Reliquary** — our own
screen, no extra mod, 2 artifact + 2 ring slots, slot kinds as data, artifact points not counted in level or cost,
accessories go into the Ash Remnant on death ("for now").

Still to decide for the system: whether more slots unlock with progress; stacking rules (two copies of one ring);
sources outside obelisks (overworld structures, the Sanctum, moon nights); a catalogue per obelisk and per
overworld source so they do not overlap.

## Realms 2–4 (ROADMAP "Later")

**Status:** not now (decided 2026-10-22). Focus stays on overworld content and Realm 1. Each realm gets its own
design pass when it comes back; the variants below are the first round, none was chosen.

Cross-realm constraints found:
- Gear tier vs netherite: DESIGN §6 puts Realm 2 at about netherite, but if Realm 2 needs netherite (§10 q3) its
  tier must sit above it. Leaning: the Realm 2 tier is a smithing upgrade of netherite gear (a soft gate through
  the recipe, not the obelisk).
- One buildup-status framework, built once in Realm 2, reused later. Three realms with a meter each is too much:
  either fewer meters, or each plays differently (rot = damage over time, beastblood = risk/reward buff, void
  taint = light and visibility).
- Ember Moon sky and visuals could be reused for Realm 3's perpetual night.

### Realm 2 — Mire of Rot
- **1. Valley of Defilement** — swamp / plague village / drowned mines → the Font. Rot as a buildup status.
  Cheapest, closest to DESIGN §4.4.
- **2. Fungal rot** — spore fen / mycelium caves / overgrown hamlet → Heart-Mycelium. Spores in the air; fire and
  ember burn them (ties to KIN).
- **3. The bog itself** — black bog / stilt village / drowned mines. Terrain as the mechanic: sinking mud,
  leeches, light vs fog; no status bar.
- **4. Plague and quarantine** — quarantine town / charnel pits / sewers; contagion spreads between mobs.
  Overlaps Realm 3's gothic city.
- **5. Poisonous bloom** — toxic flowering marsh / hive / sunken garden; bright, a contrast to Realm 1's grey.

Leaning: 1 as the base, spores burned by fire from 2, sinking mud from 3 as zone flavour.

### Realm 3 — Cathedral Ward
Levels from DESIGN §5.4: zones 45–60, realm boss 60; tier above netherite.
- **1. City of the Hunt** — gothic city / frozen cloister / catacombs → the Chapel, perpetual night. Beastblood
  buildup (more damage dealt and taken). Tier: Hallowed Silver (+damage vs `#beast`). About Realm 1 ×1.3.
- **2. Ice cathedral** — eternal winter; cold through vanilla frozen ticks, warmth from ember light and braziers,
  KIN/RES reduce it. Tier: Rimesteel (slow on hit). About Realm 1 ×1.
- **3. Hunters and beasts** — two factions fight each other and the player; a three-way zone boss; hunter mobs
  use player-like specials; per-player truce. About ×1.5.
- **4. Cathedral-mountain** — one giant landmark, zones are its wings, bells open shortcuts and raise danger.
  Clashes with "building allowed" (§10 q1). About ×2.

Leaning: 1 as the base, cold from 2 in the cloister zone, the hunter faction and a three-way boss from 3; not 4.

### Realm 4 — The Void
- **A. Shattered sky** — the DESIGN sketch: starfall plains / floating ruins / inverted sea → Heart of the Void.
  Void taint reset by ember light. Expensive (custom terrain per zone).
- **B. Echoes of the devoured** — zones are void-eaten echoes of realms 1–3, bosses their corrupted echoes; echoes
  of the player's own deaths. Cheap on assets, needs realms 2–3 first.
- **C. Lightless** — an ember lantern with fuel in the offhand; mobs are seen only in its light. Mid cost.
- **D. The Well** — a vertical realm, islands around a shaft; falling takes you deeper instead of killing. Needs
  layer logic instead of angle sectors; expensive.
- **E. The Void at the threshold** — a compact realm; the finale is the Void invading the Sanctum. A 5th realm
  only as optional post-game (§10 q4). Mid cost.

Leaning: A as the base, light and taint from C, the ending from E (final fight under the Sanctum, a "second
circle" NG+, a 5th realm only after 1.0).
