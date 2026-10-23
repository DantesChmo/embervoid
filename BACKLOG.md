# Embervoid — Backlog

Ideas that were discussed and set aside: not planned for any milestone, but worth keeping. Each entry says what
was considered and where it stopped, so a later design pass does not start from zero. Committed work lives in
[ROADMAP.md](ROADMAP.md).

## Playtest 0.4.1

**Status:** to do, not scheduled yet (the user's own playthrough of 0.4.1, 2026-10-23; overall verdict 5/10, "looks
cheap"). Unlike the rest of this file these are changes the user wants made, broken down and agreed in the same chat;
they move to ROADMAP as a release once scheduled. Items 8 and 10 replace decisions in DESIGN (final-zone lock, hub in
the overworld); DESIGN is updated when they are scheduled. Sizes: S / M / L / XL.

Suggested order: 10 first (it settles 9 and what the overworld Sanctum becomes), then 4 + 5 in one pass, then the
visual items 6, 7, 8; 1 and 2 fit in anywhere.

### 1. Boss music volume of its own (S)
Boss tracks play in `SoundCategory.MUSIC` (`BossMusicClient`), so muting music in the options mutes them too. The user
keeps vanilla music off but likes the boss tracks. Fix: a "Boss music" slider in the vanilla Sound options screen,
independent of Music, default 100 %.

### 2. The trail to the Ash Colossus is not visible (S–M)
In the Fields the way to the Colossus arena cannot be made out. Not yet checked what the trail is built from. Fix: a
material that stands out from the ground, fire baskets or lanterns along it, milestones like the roads from the
crossroads plaza.

### 3. Smoldering Lord's face
Merged into item 8.

### 4. Weapons: look and stats (L)
Every Realm 1 weapon looks bad and feels worse than vanilla. Measured: the Royal Ash Rapier (6 damage, speed 2.0,
reach 2.5) has about the damage per second of a diamond sword (7, 1.6), a shorter reach than vanilla (3) and no sweep.
- Realm 1 uniques sit **above netherite**, each with an identity felt in hand (reshapes the loot ladder, DESIGN §6,
  together with item 5).
- New visuals: bigger, own in-hand models. Player attack animations need a player-animation library (playerAnimator,
  a new dependency).
- **Warden's Scourge** rebuilt as a real whip: long, segmented, bends through the air on a swing; hits along an arc
  line, the tip cracks for more damage; Lash still pulls.

### 5. Full rebalance (L)
Boss fights take 5+ minutes while a boss kills in about two hits. The model (docs/balance-0.4.0.md) gives 52–73 clean
hits per Realm 1 boss (30–60 s of pure hitting); with dodging that is the 5 minutes. Souls reference: about 20–35 hits.
Scope is the whole game, not only bosses: overworld mobs, moon nights, Realm 1 mobs and bosses, player stats, weapons
(item 4). One target fight length (bosses about 1.5–2.5 min with dodging) and every number recalculated to it; a hit
taking two thirds of the player's health only with a clear wind-up.

### 6. Visual overhaul: mobs, bosses and locations (XL)
"Looks cheap", "the bosses are stools". None of the 24 animated mobs has a death, hit-reaction or stagger animation;
each has idle, walk and 2–5 attacks. References: L_Ender's Cataclysm and Mowzie's Mobs for bosses, Create for moving
machinery.
- Mobs and bosses: more detailed models, more bones, secondary motion (cloaks, chains), death, phase-change and
  hit-reaction animations; effects: particles, camera shake, swing trails.
- Moving set pieces in locations, Create-style: the Fields windmill with turning sails (the current one is a static
  build), a granary hoist with a swinging chain, swinging bells and cages in the Necropolis, gate gears raising the
  Citadel portcullis, forge bellows and chain lifts in the Citadel, censers, braziers, smoke columns. Built as
  animated GeckoLib block entities, count capped for performance.
- How to check it without playing (no client from a Claude session, see COORDINATION "Automated checks"):
  1. A model viewer (tools/, three.js): renders our geo models with textures and plays their animations; opened in
     the built-in browser pane and checked from screenshots.
  2. The same viewer renders structure `.nbt` files with real block textures (top-down and eye level): trails,
     decor variety, moving set pieces.
  3. Logic (rotation, portals, deactivation): GameTests as usual.
  4. Optional final check per release: a dev "photo tour" — the client loads a test world, flies the camera through
     set points, saves screenshots and quits. Started by the user in their own terminal; no playing.

### 7. Decoration: blocks and 3D props (M–L)
Locations are visibly built from the same few blocks and pieces. Not only more blocks: props with their own shape and
matching hitbox (a sack looks like a sack, not a cube with a sack texture): sacks, carts, scarecrows, burnt barrels,
coffins, gravestones, bone piles, candles, weapon racks, cages, banners. More jigsaw piece variants as well.

### 8. Final zone behind a fog wall (L)
The Sealed Gate teleport into a separate depths dimension is "a shame". Going straight to the full version, no
interim fog-looking teleport: the Smoldering Citadel becomes part of the realm dimension behind a fog wall, fog-wall
both in look and in mechanics. The hard lock (DESIGN §4, "Final zone lock") moves to the server: the wall does not let
a player through before the three zone bosses, ender pearls and elytra do not work over the Citadel. The Citadel's
generation is rebuilt for this. The Smoldering Lord's model is redone here too (the face with tiny eyes reads as a
joke).

### 9. The dungeon spawns more often
The user found three villages, three shrines and about five ruined portals before one Sanctum (now `spacing` 48 /
`separation` 16 chunks, about one per 768×768 blocks, plus the Sanctum Map in chests). With item 10 this is about the
overworld dungeon: about one per 300–400 blocks, with several variants so it does not repeat.

### 10. The hub in its own dimension (XXL, design change)
The player should not depend on finding one generated Sanctum.
- The overworld Sanctum structure becomes a **dungeon** (outer halls, guards, loot stay); the Maiden and the obelisks
  move to a **hub dimension**. The dungeon has no boss today (only guards: Hollows and the rest), so its boss is
  designed from scratch. The Sanctum Map leads to the nearest dungeon.
- The boss drops the **Crying Soul**. No moon-night ingredients (too grindy) and no crafting: the soul itself is the
  activator and is used up on activation.
- **Portal: the Cleft Stele** (proposed shape). A wide, flat stone slab the size of a nether portal (5 wide, 6 tall),
  split top to bottom; the crack is the passage. Wide and flat, not a pillar, so it reads as a stele and not as a
  phallus. Built from mod blocks to a pattern, detected like a nether portal frame.
- Right-click with the soul: the blocks switch to kindled textures with glowing cracks, mist and particles fill the
  crack, a sound plays. Right-click on an active stele takes the player to the hub.
- **One active stele per world:** A ⇄ hub; activating B switches A off, then only B ⇄ hub. Leaving an obelisk lands
  in the hub; the hub's exit leads to the active stele. Multiplayer drawback: one player's new stele switches off
  another's; "one active stele per player" is the alternative if that matters.
- Replaces: Attunement and "any Sanctum works" (DESIGN §2), the return point "the Sanctum you entered from".

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

## Wanderer's Cloak (Sanctum outer-hall armour)

**Status:** idea, moved out of the Sanctum loot in 0.4.1 (PLAN D12): DESIGN §6 once listed "Wanderer's Cloak pieces"
in the outer halls next to the Flask Shards. The Sanctum weapons (DESIGN §10.10) took that slot. If it comes back: a
light pre-realm set between leather and iron, no set bonus beyond vanilla, so Realm 1's sets stay the first real armour.

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
