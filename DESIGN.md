# Embervoid — Design Document

> The world is fading. Somewhere beneath it, a sanctum still holds an ember — and the obelisks around it
> open onto realms that the void has already taken.

**Pitch:** Demon's Souls / Bloodborne structure (a hub, gated worlds, leveling at an NPC with what you earn
from killing) inside a Divine RPG / Twilight Forest style Minecraft mod (separate dimensions with their own
style, progression tiers, bosses that gate the way forward).

This document is the source of truth for *what* the mod is. [ROADMAP.md](ROADMAP.md) says in what order it
gets built, [PLAN.md](PLAN.md) breaks the next milestones into tasks.

All numbers below are **starting values for balancing**. Gameplay numbers live in the config; worldgen
placement (structure spacing, dimensions) lives in datapack JSON, which other datapacks can override.

Status markers: **Decided** — agreed, build it. **Proposed** — recommended default, can still change.
**Open** — needs a decision before the milestone that depends on it.

---

## 1. Player journey

```
 Overworld (harder)          Ember Sanctum (hub)                 Realm N
 ──────────────────          ───────────────────                 ───────
 survive, mine, craft  ──►   find it (rare, like a village) ──►  obelisk N teleports you in
 stronger vanilla mobs       fight through its outer halls        3 zones, any order, 1 boss each
 + new hostile mobs          reach the core: safe room            ─► Sealed Gate opens
 earn vanilla XP             level up at the Ashen Maiden         final zone + realm boss
          ▲                   obelisks: 1 lit, the rest dark      ─► obelisk N+1 lights up
          └──────────── return home any time, spend XP, repeat ◄──┘
```

1. **Overworld.** The player starts in a normal Minecraft world and plays it as usual. The world is harder
   than vanilla (the stick, §3): vanilla mobs hit harder even on Easy, and new hostile mobs appear. Vanilla XP
   is now worth more than enchanting — it is what the player levels up with.
2. **Finding the hub.** The **Ember Sanctum** is a structure that generates with village-like rarity. The
   player has to go looking for it. The outer part is a real dungeon; the core is a safe room.
3. **The hub.** In the core: the **Ashen Maiden** (leveling NPC), a ring of **Void Obelisks** (one per realm)
   and a rest point. The first obelisk is lit; the others are dark until the previous realm is completed.
4. **Realms.** Each obelisk leads to its own **realm**, a separate dimension with a full visual identity.
   A realm has 4 zones (biomes). The first 3 can be done in any order; each ends in a zone boss. Beating all
   three opens the **Sealed Gate** to the final zone and the realm boss.
5. **Reward (the carrot, §6).** Each realm drops better gear than the previous one and better than what the
   player could get before it. XP from realm mobs feeds leveling.
6. **Loop.** Back to the Sanctum, level up, gear up, next realm.

There is still no storyline and no quest chain. Progression is structural: places, gates, bosses.
Lore lives in item descriptions and the environment.

---

## 2. Ember Sanctum (hub)

| Topic | Design | Status |
|---|---|---|
| Spawning | Overworld structure, any land biome. Spacing ~48 chunks, separation ~16 (rarer than villages: roughly one per 750 blocks). Tunable through the `structure_set` datapack file | Proposed |
| Layout | **Outer halls**: surface ruin + descent, guarded (Hollows, traps, loot). **Core**: hostile-spawn-free room with the Maiden, the obelisk ring, an Ember Shrine | Decided |
| Finding it | Pure exploration, plus a rare **Sanctum Map** (explorer-map style) injected into vanilla chest loot (villages, ruined portals, shipwrecks) | Proposed |
| Attunement | The first time a player reaches a core, they are *attuned*. From then on **any** Sanctum works for them; progress is per player, not per structure | Proposed |
| Return point | Leaving a realm returns you to the Sanctum you entered from | Proposed |
| Protection | Core blocks (obelisks, shrine, floor of the obelisk ring) are unbreakable. The Maiden is invulnerable and never despawns. Outer halls are normal blocks | Proposed |

### Ashen Maiden (leveling NPC)
- Talk → **level-up screen**: all stats, their current derived values (HP, stamina, damage bonus…), a preview
  of what the next point gives, the cost in XP points, and how much XP the player has.
- Points are bought **one at a time** and applied immediately.
- Later (not 0.x): respec item, merchant role.

### Void Obelisks
- One per realm, placed in a ring. States: **dark** (locked, tooltip hint "the flame has not reached it"),
  **lit** (unlocked), **kindled** (realm completed).
- Right-click a lit obelisk → teleport to that realm's landing point (or to the last Ember Shrine you rested
  at in that realm — the player chooses).

---

## 3. The stick: a harder world

### 3.1 Stronger vanilla mobs
Every hostile mob gets a **level** (§5.4). Level drives HP, damage and XP. On top of that, difficulty no
longer goes soft:

| Difficulty | Vanilla damage | Embervoid damage (before level scaling) | Status |
|---|---|---|---|
| Easy | ×0.5 + 1 | ×1.0 (vanilla Normal) | Proposed |
| Normal | ×1.0 | ×1.25 | Proposed |
| Hard | ×1.5 | ×1.75 | Proposed |

So a zombie on Easy already hits like vanilla Normal, and higher-level mobs hit harder still.

### 3.2 New overworld hostiles
Purpose: make the overworld dangerous enough that the player *wants* to find the Sanctum and level up.

| Mob | Where / when | Role | Status |
|---|---|---|---|
| **Ash Ghoul** | Surface at night, any biome; underground | Fast, weak, packs of 3–5. Bursts into a blinding ash cloud on death | Decided (was planned) |
| **Hollow** | Surface at night (rare), Sanctum outer halls | Armed undead swordsman/spearman. Slow, hard-hitting, blocks. Drops lots of XP | Decided |
| **Ember Wisp** | Caves below y=0 | Small flyer, sets blocks and the player on fire, flees light | Decided |

The Hollow Sentinel and Crypt Moth from the old plan move into Realm 1.

#### Bestiary (Decided)

**Hollow** — base level 15, undead.
- 30 HP, armor 6, knockback resistance 0.6, speed 0.2. Base attack 0: damage comes from the weapon only
  (iron sword 70% / stone sword 30%).
- Telegraphed attack: 8-tick wind-up, one attack per 40 ticks.
- **Guard:** with a target within 5 blocks and its attack on cooldown, it raises its guard for 30–60 ticks: half
  speed, melee from the front 120° takes 20% damage, projectiles from the front are deflected. An axe hit breaks
  the guard and staggers it for 60 ticks.
- Spawns on the overworld surface at night under open sky, weight 5, alone; placed persistent in the Sanctum
  outer halls. Does not burn in daylight.
- XP 20 base. Loot: 1–4 iron nuggets, 0–2 bones, 10% iron ingot on a player kill.
- Target for the 0.2.0 balance pass: ~6 HP per hit through full iron on Normal (with base attack 2 it was
  ~10–12, two hits to kill a fresh player).

**Ember Wisp** — base level 8 (+3 below y=0 from §5.4).
- 8 HP flyer. Contact hit: 1 damage + 4 s of fire. Fire immune, hurt by water.
- Dives at players within 16 blocks. Every 8–12 s lights a fire nearby (only with `mobGriefing`).
- **Flees light:** at block light ≥ 7 it drops its target and flies to a darker spot, and it does not dive at a
  player standing in light ≥ 7. Torches are the counter.
- Spawns below y=0 at block light 0, weight 15, groups of 1–2, not in the Deep Dark.
- XP 6. Loot: 0–1 glowstone dust, 5% blaze powder on a player kill.

### 3.3 Later ideas
- **Ember Moon** night event (rare): mobs +5 levels, red sky, more XP. Bloodborne's blood moon.

---

## 4. Realms

### 4.1 Structure of a realm

```
              ┌───────────── realm_N (one dimension) ─────────────┐
              │                                                    │
              │     Zone A ─┐                     ┌─ Zone C        │
              │             │      Landing        │                │
              │             └──── (return obelisk, ┘               │
              │                    Ember Shrine)                   │
              │                       │                            │
              │                    Zone B                          │
              │                                                    │
              │   ~1200 block radius; edge = abyss / impassable    │
              │   Sealed Gate stands at the landing                │
              └──────────────────────┬─────────────────────────────┘
                                     │ gate opens after A, B, C bosses
                                     ▼
                       realm_N_depths (separate dimension)
                       final zone + realm boss
```

| Topic | Design | Status |
|---|---|---|
| Count | 4 realms planned, built one at a time. Realm 1 first | Proposed |
| Zones | 4 per realm: 3 open zones + 1 final zone. Each zone is one or more biomes with its own style, mobs and structures | Decided |
| Order | The 3 open zones can be done in any order (like the archstones in Demon's Souls). Their level bands differ, so the player picks easier first | Proposed |
| Unlock | Realm N+1 unlocks when the realm N boss dies. Final zone unlocks when the 3 zone bosses of that realm die | Decided |
| Generation | Random but structured: terrain, caves, decoration and minor structures are seeded; the **layout** (where each zone is, where its boss arena is) is fixed by design | Decided |
| Zone layout | Custom `BiomeSource`: zone by angle sector around the landing, with noise-warped borders so they do not look like pie slices | Proposed |
| Size | Bounded: ~1200 blocks radius, ending in an abyss. Realms are places, not infinite worlds | Proposed |
| Final zone lock | The final zone is a **separate dimension** reached through the Sealed Gate. That makes the lock hard: no ender pearls, elytra or tunneling around it | Proposed |
| Key structures | Each zone has one guaranteed boss arena (custom structure placement, position derived from the seed within the zone's sector) plus random minor structures | Proposed |
| Building | Realms are survival-normal: mine, build, place beds. Boss arenas, gates, shrines and obelisks are unbreakable | **Open** |
| Navigation | A realm-bound item (working name *Ember Lodestar*) points to the nearest boss the player has not beaten | Proposed |

### 4.2 Ember Shrines, death and respawn
Demon's Souls gives an archstone shard after each boss. We do the same:

- Each zone boss leaves an **Ember Shrine** in its arena. The landing has one from the start. How an arena
  shrine behaves for players who have not beaten that boss is **Open** (§10.8).
- Resting at a shrine: refills flasks, sets the **realm respawn point**, allows teleport to the Sanctum.
- Dying **in a realm**: respawn at the last shrine rested at in that realm. Your overworld bed spawn is not
  touched.
- Dying **in the overworld**: vanilla respawn.
- The **Ash Remnant** (§7) applies everywhere: your XP — your currency — waits where you died.
- This reverses the old "no bonfire" decision. Shrines exist only in realms and the Sanctum; the overworld
  keeps beds.

### 4.3 Bosses and multiplayer
Progress (attunement, beaten bosses, unlocked realms) is **per player**. A realm dimension is shared.

- Entering an arena whose boss is not alive summons it **if any player inside has not beaten it yet**.
  A fog wall closes the arena until the fight ends.
- Every player inside the arena at the kill gets credit.
- **First kill** per player drops the boss's unique item (the "demon soul" — working name *Ember Heart*),
  used to obtain that boss's signature gear. Repeat kills (helping a friend) drop normal loot and XP only.
  The Ember Heart is **personal loot**: each credited player gets their own, straight into the inventory, so
  one player cannot pick up everyone's.
- A player who has beaten the boss walks through an empty arena.

### 4.4 Realm sketches

| # | Realm | Zones | Feel | Status |
|---|---|---|---|---|
| 1 | **Ashen Reach** | Ash Wasteland · Dead Woods · Sunken Crypt → final: **Smoldering Citadel** | Grey ash, dead trees, crypts, a burning fortress. Absorbs the old 0.2.0 content: Crypt Keeper, Smoldering Lord, Hollow Sentinel, Crypt Moth, Sparking Marksman | Proposed |
| 2 | *Mire of Rot* | swamp, plague village, drowned mines → final: the Font | Poison, disease, Valley of Defilement | Sketch |
| 3 | *Cathedral Ward* | gothic city, frozen cloister, catacombs → final: the Chapel | Bloodborne's Yharnam, beasts, hunters | Sketch |
| 4 | *The Void* | starfall plains, floating ruins, inverted sea → final: the Heart of the Void | Cosmic horror, the reason for the mod's name | Sketch |

Realms 2–4 are only themes. Each gets its own design pass (mobs, bosses, loot) before it is built.

---

## 5. RPG system

### 5.1 No classes
Every character starts as the **Deprived**: level 1, every stat 10. Build identity comes only from where the
player puts points. Baseline 10 in every stat = vanilla numbers, so a level 1 player feels exactly like
vanilla — while the world is already harder.

### 5.2 Stats

| Stat | Short | Effect (per point above 10) | Status |
|---|---|---|---|
| **Vitality** | VIT | Max HP | Proposed |
| **Endurance** | END | Max stamina and stamina regen; small knockback resistance | Proposed |
| **Strength** | STR | Damage with STR-scaling weapons (axes, heavy weapons); weapon requirements | Proposed |
| **Dexterity** | DEX | Damage with DEX-scaling weapons (swords, daggers, bows); small attack speed | Proposed |
| **Kindling** | KIN | Ember (fire/magic) damage: talismans, ember weapons; flask healing | Proposed |
| **Resilience** | RES | Armor toughness; resistance to fire, poison, wither | Proposed |
| **Fortune** | FOR | Luck attribute: loot quality, rare drops | Proposed |

Soft caps keep any single stat from scaling forever:

| Stat value | Gain per point (relative) |
|---|---|
| 10–20 | 100% |
| 21–40 | 50% |
| 41–99 | 20% |

Example, Vitality (+1 HP per point at 100%): 10 → 20 HP, 20 → 30 HP, 40 → 40 HP, 99 → ~52 HP.

Implementation: each stat applies vanilla attribute modifiers (fixed UUIDs) — `generic.max_health`,
`generic.attack_speed`, `generic.armor_toughness`, `generic.knockback_resistance`, `generic.luck` — plus our
own values for stamina, ember damage and status resistance. Recomputed on load, respawn and level-up. That
keeps compatibility with other mods that read vanilla attributes.

STR/DEX/KIN damage bonuses are **not** a `generic.attack_damage` modifier: that attribute applies to any held
item and does not touch arrows. They are applied when damage is dealt, by the weapon (or the bow a projectile
came from).

Which stats actually do something in 0.2.0, before weapons and ember damage exist, is **Open** (§10.10).

How weapons scale with STR/DEX/KIN and what requirements look like is part of the weapons design (§8).
Until then vanilla weapons use: swords DEX, axes STR, bows DEX, tridents STR+DEX.

### 5.3 Character level and cost

- **Level = 1 + (sum of all stats − 70).** One level = one stat point.
- Paid at the Maiden with **vanilla XP points** (not vanilla levels). Vanilla XP stays the single currency, so
  enchanting and anvils compete with leveling — a real choice, like souls in Demon's Souls. Points are
  computed from the player's level and progress bar, not from vanilla's `totalExperience`, which enchanting
  and anvils do not lower.
- Cost to go from level L to L+1: **`20 + 6·L + 0.15·L²`** XP points (coefficients in config).

| Level | Cost of the next level | Total spent to reach it |
|---|---|---|
| 1 | 26 | 0 |
| 10 | 95 | ~490 |
| 20 | 200 | ~1 890 |
| 30 | 335 | ~4 470 |
| 50 | 695 | ~14 400 |
| 99 | 2 084 | ~78 800 |

For scale: vanilla level 0 → 30 is 1 395 XP points.
- Each stat caps at 99, so the character level tops out at 624. **Open:** cap the character
  level at e.g. 120 for the 0.x versions.

### 5.4 Mob levels
Every hostile mob — vanilla, overworld-new, realm — has a level. The level drives:

| Value | Formula (start) |
|---|---|
| HP | base × (1 + 0.06·(L − 1)) |
| Damage | base × difficulty factor (§3.1) × (1 + 0.05·(L − 1)) |
| XP dropped | base × (1 + 0.10·(L − 1)) |

Damage scaling covers everything a leveled mob deals: melee, projectiles, explosions, potions. It is applied
when damage is dealt, by the attacker or the projectile's owner.

**Overworld levels** = base level by mob type + situational bonus:

| Mob | Base level |
|---|---|
| Zombie, husk, drowned, spider | 3 |
| Skeleton, stray, slime | 4 |
| Creeper, witch | 6 |
| Ash Ghoul | 5 |
| Ember Wisp | 8 |
| Enderman | 12 |
| Hollow | 15 |
| Any other hostile (vanilla or modded) | 4 — **Open** (§10.9) |
| Ender Dragon, Wither, Warden, Elder Guardian | no level, vanilla stats |

| Bonus | Value | Status |
|---|---|---|
| Distance from world spawn | +1 per 1000 blocks, max +5 | Proposed |
| Below y=0 | +3 | Proposed |
| Nether | +10 | **Open** (§10.9) |
| End | +20 | **Open** (§10.9) |

With these bonuses an End enderman is level 32, above the Realm 1 boss — see §10.9.

**Realm levels** are set per zone. Realm 1: zones 15–25 (each zone its own band), final zone 25–30, realm
boss 30. Zone bosses sit at the top of their zone's band.

| Realm | Mob levels | Player level that should be comfortable |
|---|---|---|
| Overworld | 3–20 | 1–15 |
| 1 — Ashen Reach | 15–30 | 10–30 |
| 2 | 30–45 | 30–45 |
| 3 | 45–60 | 45–60 |
| 4 | 60–80 | 60–80 |

Display: the level shows next to the mob's name when you look at it (and in the boss bar for bosses).
Config toggle to hide it.

There is **no** hidden "level difference" modifier. Mob level → mob stats, player level → player stats.
What you see is what you get.

---

## 6. The carrot: loot ladder

Each realm has its own **material tier** and gear set. Rough position against vanilla:

| Source | Tier | Status |
|---|---|---|
| Overworld | Vanilla up to diamond / netherite | — |
| Sanctum outer halls | Flask shards, Wanderer's Cloak pieces, vanilla loot | Proposed |
| Realm 1 | Smoldering Ingot ≈ diamond+, with properties | Proposed |
| Realm 2 | ≈ netherite | Proposed |
| Realm 3 | above netherite | Proposed |
| Realm 4 | top tier | Proposed |

**Open:** should Realm 2 require netherite gear (the Nether as a soft gate), or stay independent of vanilla
progress?

Loot per realm:
- **Material** from zone mobs/chests; crafted into armor and weapons of that tier.
- **Boss uniques** (from *Ember Hearts*): signature weapons and armor sets with set bonuses.
- **Consumables and upgrades**: flask shards (more charges / more healing), realm-specific items.

---

## 7. Core mechanics (carried over)

| Mechanic | Design | Status |
|---|---|---|
| Stamina | Spent **only** by right-click special attacks. Vanilla hits, sprinting and blocking stay free. Max and regen from END | Decided |
| Healing Flask | Charges (start 3), heal over time, cannot drink while sprinting. Refilled by resting at an Ember Shrine or sleeping in a bed. Where the charges live (item or player) is **Open** (§10.6) | Decided (shrine refill new; charge storage Open) |
| Flask Shards | Permanent +1 charge or +healing per sip, capped | Decided |
| Ash Remnant | On death XP and all items go into a remnant at a safe spot (the last position on solid ground). Modes `off` / `xp` / `xp_and_items`. With XP as the leveling currency this is our bloodstain. What dying again before reaching it does is **Open** (§10.7) | Decided (second death Open) |
| Full-set bonuses | Armor sets grant a bonus when all 4 pieces are worn | Decided |

---

## 8. Separate design topics

These are big enough to get their own documents. Each is needed before the milestone that builds it.

| Topic | Covers | Needed by |
|---|---|---|
| **Weapons** | Weapon classes, STR/DEX/KIN scaling and requirements, special attacks, how vanilla weapons fit in, upgrades | **Special-attack contract** (trigger, cost, cooldown, tooltip, limits): before the special-attack framework (PLAN 2.6). The rest: Realm 1 loot |
| **Realm 1 content** | Per-zone mobs, structures, the 4 bosses (moves and phases), loot tables | Realm 1 |
| **Bestiary (overworld)** | Hollow and Ember Wisp in detail, behaviors | Overworld threats |
| **Realms 2–4** | Each realm's zones, mobs, bosses, material | Each realm |

---

## 9. Technical notes

- **Minecraft 1.20.1, Fabric.** Required: Fabric API, GeckoLib (animated mobs and bosses).
- **TerraBlender is no longer needed.** The Ash Wasteland was an overworld biome; now it lives in Realm 1's
  own dimension with our own `BiomeSource`. The overworld gets no new biomes.
- Realm dimensions: datapack `dimension` / `dimension_type` / noise settings + Java `BiomeSource` and a custom
  `StructurePlacement` for guaranteed per-zone arenas. Final zones are their own dimensions.
- Teleports between dimensions with `FabricDimensions.teleport` and a `TeleportTarget` (no portal search).
- Player data (stats, level, attunement, beaten bosses, unlocked realms, realm respawn points, flask
  charges and upgrades, remnant id) is saved on the player, copied across death and dimension change, synced
  to the owning client. Storage: Fabric Data Attachment API (in Fabric API 0.92.x it persists and copies on
  death, but does not sync — sync is our own S2C packet, re-sent on join, respawn and dimension change).
- Config is split: `config/embervoid.json` holds gameplay values read by the server, `embervoid-client.json`
  holds client preferences. Structure spacing is datapack data, not config.
- Mob levels reach the client through a packet when a player starts tracking the entity, not through extra
  `DataTracker` entries on vanilla mobs.
- All logic server-side. Level-up is a C2S request validated on the server (enough XP, near a Maiden).
- Every user-facing string in `en_us` and `ru_ru`.

## 10. Open questions

Each question names the milestone that cannot start without an answer.

1. Building and mining inside realms: fully allowed (Twilight Forest / Divine RPG style) or restricted
   (Demon's Souls style)? Recommendation: allowed, except arenas, gates, shrines and obelisks.
   *Needed by:* milestone 6 (block protection is part of the realm framework).
2. Character level cap for 0.x (none / 120)?
3. Does Realm 2 require netherite (Nether as a soft gate)?
4. Number of realms for 1.0: 4 as sketched, or 5 like Demon's Souls?
5. Can players respec? Recommendation: later, through a rare consumable.
6. **Where do flask charges live?** On the item, a player can carry five flasks and get five times the
   heals; "refill all flasks in the inventory" makes that worse. Nothing says yet how a player gets a
   flask. Recommendation: charges and upgrades live in player data; the flask item only shows and spends
   them, so a second flask adds nothing. One flask is given on first join and can be crafted again if lost.
   *Needed by:* milestone 2 (PLAN 2.7).
7. **What does a second death do to the first remnant?** As written it destroys everything, items
   included: dying on the way back loses your diamond gear for good, which is harsher than both vanilla and
   Demon's Souls (there only the souls are lost). Recommendation: the XP burns; the items stay in the old
   remnant, which becomes an item-only remnant until picked up. Config `remnant.secondDeath`
   = `burn_xp` (default) / `burn_all`.
   *Needed by:* milestone 2 (PLAN 2.10).
8. **How does an arena shrine work for players who have not beaten its boss?** The shrine is a block in a
   shared dimension: a player who has not beaten the boss could rest in the arena where the boss summons
   for them. Recommendation: the shrine is part of the arena structure, stands outside the fog wall, and is
   dormant (cannot be rested at, looks unlit to them) for players who have not beaten that boss.
   *Needed by:* milestone 7.
9. **Nether and End level bonuses, unlisted mobs.** +10 / +20 put End endermen at level 32 (above the Realm 1
   boss) and Nether mobs in the Realm 1 band, which breaks the "overworld 3–20" band in §5.4. Nether mobs,
   illagers and modded hostiles have no base level. Recommendation: Nether +5, End +8 (End endermen ≈ 20, the
   top of the overworld band); base level 4 for any hostile not listed; vanilla bosses keep vanilla stats.
   *Needed by:* milestone 3 (PLAN 3.5).
10. **Which stats do something in 0.2.0?** Stamina is spent only by special attacks, and the only one in
    0.2.0 is on a debug weapon, so END does almost nothing. KIN has no ember damage yet, RES status
    resistance is a system that does not exist yet, and vanilla `luck` barely affects anything (fishing and a
    few loot tables). Points put there in 0.2.0 feel wasted. Recommendation: KIN → flask healing from 0.2.0;
    RES → armor toughness + fire damage reduction; FOR → luck + a small bonus chance on mob drops; ship 1–2
    real weapons with a special attack in the Sanctum outer-hall loot so END matters.
    *Needed by:* milestone 3 (PLAN 3.2), and milestone 5 for the weapons.
