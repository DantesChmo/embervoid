# Balance — the overworld, level 1 to the Sanctum (0.2.0 pass)

PLAN L2 (leftovers of 0.2.0), track `left-gear`. Realm 1 and the moon nights: [balance-0.4.0.md](balance-0.4.0.md).

## 1. What this is

The overworld stage of the balance calculator in `com.embervoid.balance`, on the same formulas as the Realm 1 model
(stat curve, mob level HP / damage / XP, difficulty factors, vanilla armor):

- `OverworldData` (main): every overworld hostile at level 1 — vanilla 1.20.1 numbers and the Embervoid mobs (Ash
  Ghoul, Hollow, Ember Wisp); base levels come from the `mobs.levels` config defaults, not from here.
- `OverworldModel` (main): the level bands, fights on Easy / Normal / Hard, an Ash Ghoul pack, the Hollow's swing through
  iron, the XP of a route to the Sanctum, damage per second of a weapon.
- `BalanceModel` got leather armor, the stone sword and iron axe, the two Sanctum weapons (PLAN L1), a `MAGIC` hit kind
  (a mob's potion: difficulty and level, no armor) and `fight(..., difficulty)`. Nothing Realm 1 uses changed.
- `OverworldTargetsTest` (JUnit) checks the targets of §3; `BalanceTargetsTest.sanctumWeaponsSitBelowTheRealm` the
  Sanctum weapons (§6).
- `OverworldReport` writes the tables below to `build/balance/overworld.md` (`./gradlew test --tests
  'com.embervoid.balance.*'`).
- `BalanceFightTests` (GameTest) measures them in the engine (§7).

Not modelled, all in the player's favour: crits, shields, enchantments, dodging (the Hollow's 8-tick wind-up and the
creeper's 1.5 s fuse are made to be dodged), food, potions, terrain. "Trade" is the worst honest case: both sides swing
as fast as they can until one dies.

## 2. Assumptions

- **Players.** Fresh: level 1, every stat 10 (20 HP), in leather (armor 7) with a stone sword, or in full iron (15) with
  an iron sword; the starting flask (3 × 8 HP). Sanctum-level: the Realm 1 model's level 10 (VIT 14, DEX 13, END 11,
  RES 11), iron, iron sword.
- **Levels** (DESIGN §5.4): base by type (`mobs.levels.baseLevels`, any other hostile 4) + 1 per 1000 blocks from spawn
  (max +5) + 3 below y=0; Nether +5, End +8; a full moon +3. "Lowest" is a spawn near world spawn on the surface (the
  Ember Wisp only spawns below y=0, so its lowest is 11); "highest" is 5000+ blocks out and below y=0.
- **Hits.** Skeleton arrow 4 (vanilla's 2 + its difficulty roll, × launch speed 1.6, rounded up), one every 60 ticks
  (40 on Hard). Creeper: its blast at 2 blocks (a player who stands and fights it) and at 4 (one who backs off during
  the fuse); vanilla's explosion formula, power 3. Witch: its worst throw, Harming (6, magic, ignores armor), every
  60 ticks; it opens with Slowness or Poison. Hollow: iron sword 5 (70 %) / stone sword 4 (30 %), one swing per 40
  ticks. Ember Wisp: touch 1 per dive (20–40 ticks) plus the 4 s of fire it leaves (1 per second, ignores armor).
  Vindicator (raids and mansions only, default level 4): 5 + its iron axe's 8.
- **Route to the Sanctum** (`OverworldModel.Route.TYPICAL`), a plausible first trip of about three in-game days: 30
  night kills on the surface near spawn and 20 kills in caves below y=0 (both mixed by spawn weight × group size);
  40 coal, 6 redstone, 4 lapis and 2 diamond ore mined; 30 iron (full armor, sword, tools) and 4 gold smelted; at the
  Sanctum its guards killed on the way in (3.5 Hollows and 1 skeleton, CORE §4 "2–5 Hollows") and 4 outer-hall chests
  (Bottles o' Enchanting, 2.9 XP per chest on average).

## 3. Targets

- **Hollow** (DESIGN §3.2): at its base level ~6 per hit through full iron on Normal — checked as 5.5–6.5 for its
  weapon mix.
- **Bands** (DESIGN §5.4): every overworld hostile's natural band (no full moon) within 3–20.
- **Common mobs near spawn are fair** (zombie, spider, skeleton, Ash Ghoul, Ember Wisp at their lowest level, traded
  hit for hit): at most 30 % of a fresh iron player's health on Normal, at most 50 % of a fresh leather player's on
  Easy. A creeper's blast at 4 blocks: at most 60 % of the fresh iron player's health on Normal.
- **The tough ones are beatable with the flask:** one Hollow, and an Ash Ghoul pack of four attacking at once, cost a
  fresh iron player on Normal no more than health + the starting flask (44).
- **XP:** the route gives 400–650 points, buying the Realm 1 model's Sanctum level (10, 492 points) within ±1.

## 4. Results

> **0.5.0 (PLAN 16.4, 18.3):** this section and §5 are the 0.2.0 pass. Since then the zombie family, creeper,
> witch, Enderman and Vindicator carry per-type factors (`mobs.levels.healthScale` / `damageScale`) to the 0.5.0
> bands, the Hollow has 25 HP, and the route counts the placed dungeon's 4.5 guards and 6 chests (629 XP, level 11).
> The witch's, Enderman's and Vindicator's outliers below are gone; the creeper's stays (D13). §8 is current.

Every target holds with the current numbers; **no number was changed** (table in §5 says why for each outlier).

| Target | Result |
|---|---|
| Hollow through full iron, Normal | **6.0** (iron sword 6.5, stone sword 4.8); Easy 4.5, Hard 9.5. Before 0.2.0's weapon-only change it was ~10–12 |
| Bands | 3–20 for all; the Hollow and the Enderman top out at exactly 20, the Wisp at 16. Full moon adds 3 (Hollow, Enderman 23) |
| Common mobs, fresh iron, Normal | zombie 20 %, spider 13 %, skeleton 15 %, Ash Ghoul 18 %, Ember Wisp 12 % |
| Common mobs, fresh leather, Easy | zombie 39 %, spider 17 %, skeleton 19 %, Ash Ghoul 23 %, Ember Wisp 13 % |
| Creeper at 4 blocks, fresh iron, Normal | 11.1 of 20 (56 %) |
| One Hollow, fresh iron, Normal | 26 of a 44 budget (it takes 11 swings of an iron sword; it needs 4 hits to kill) |
| Ash Ghoul pack of 4, fresh iron, Normal | 33 of 44 |
| Route XP | **587** points: surface kills 191, caves 141, mining 82, smelting 25, Sanctum guards and chests 147 → level 10 (11 is 587.0; one more zombie). The model's 492 sits inside a normal first trip |

The early level costs (26, 33, 39, 46 … 95 at level 10) mean the first level comes after about four night kills,
and each later one after one more night's work: the Maiden pays off from the first visit.

## 5. Outliers, and why they stay

| What | Numbers | Why not changed |
|---|---|---|
| **Creeper blast at 3 blocks** (where the fuse starts) kills a fresh player in iron on Normal | 22.0 of 20 (vanilla 11.5); at 2 blocks 33 (vanilla 21.1, also lethal); Easy 16 | The difficulty factor (×1.25) and level 6 (×1.25) together, and big hits go through armor almost whole. A level 10 player (24 HP) survives it. The stick of DESIGN §3.1 ("higher-level mobs hit harder still"); stepping back during the fuse costs 11 at 4 blocks. Lowering the creeper to base level 3 would make 3 blocks survivable in iron (18.5) — a one-line config change if play shows it is too harsh. |
| **A provoked Enderman** costs a fresh iron player 319 % traded on Normal (66 HP, 9.1 per hit) | vanilla: 3.8 per hit, ~90 % | Base 12 is DESIGN §5.4's (End endermen at 20, the top of the band, D7). Neutral until looked at; water or a two-block-high gap ends the fight, as in vanilla. |
| **Vindicator** 14.5 per hit through iron on Normal | vanilla 8.6 | No natural spawn (raids are started by the player, mansions are far and late). The difficulty factor, not its level 4. |
| **Witch** Harming 9.4 per throw, ignores armor | vanilla 6 | Weight 5, alone; it opens with Slowness or Poison. |
| **Ash Ghoul pack vs a fresh leather player on Normal** 56 of 44 | Easy 44 of 44 | Ghouls burn at dawn and a pack is 3 % of night spawns; in iron it is 33 of 44. Packs are the design (DESIGN §3.2). |
| **Hollow on the surface vs leather** 198 % traded on Normal | in iron 130 %, beatable with the flask | Rare (weight 5 of ~1700 weighted spawns); telegraphed swing, shield and axe counters. |
| **D7** (Nether +5, End +8, unlisted 4, vanilla bosses unleveled) | End enderman 20, unlisted Nether mobs 9 (no distance bonus there) | Re-checked: all inside the band. Keep. |

Global numbers (mob level factors, difficulty factors, level cost, `experiencePerLevel` 0.06) were not touched: the
Realm 1 curve (Sanctum 10 → depths 30 → Realm 1 done 36) and every `BalanceTargetsTest` target are as in
[balance-0.4.0.md](balance-0.4.0.md).

## 6. Sanctum weapons (PLAN L1)

| | Sanctum Longsword | Ashbearer's Mace |
|---|---|---|
| Damage / speed / scaling | 6.5 / 1.6 / DEX | 8 / 0.9 / STR |
| Special | Wide Cleave: 90° arc, 3 blocks, 1.25× (8.6 at armor 0), 25 stamina, 3 s | Ground Slam: ring r 3, 0.75× (6), knockback, Slowness II 2 s, 30 stamina, 5 s; breaks a Hollow's guard like an axe |
| Per hit / per second, armor 10, level 10 | 5.1 / 8.3 | 6.1 / 5.6 |
| Next to | iron sword 4.6 / 7.5, diamond sword 5.6 / 9.0 | iron axe 7.0 / 6.4, diamond axe 7.0 / 7.1 |
| Durability, repair | 600, iron ingots | 600, iron ingots |

Realm weapons for the same player: Scythe 8.2 / 9.2, Scourge 7.5 / 6.9, Rapier 4.5 / 9.2. A literal "below every
realm weapon per hit and per second" cannot hold without putting the Sanctum weapons under iron: the Rapier is the
weakest per hit and the Scourge per second (they pay with reach, Bleeding and fire). The target built
(`sanctumWeaponsSitBelowTheRealm`, sent to the coordinator as a QUESTION): neither Sanctum weapon beats any realm
weapon both per hit and per second; the realm's best per hit (Scythe) and per second (Rapier, Scythe) beat both; per
second the longsword stays below the diamond sword and the mace below the diamond axe. The longsword sits between the
iron and diamond sword as DESIGN §10.10 asks; the mace is an iron axe's sidegrade (one less damage, 2.4× the
durability, the slam). Not changed.

**Loot:** one pool in `chests/sanctum_outer_hall` — longsword 6, mace 6, nothing 88: 6 % each per chest, never both.
A Sanctum has 3–5 chests (4 on average): a given weapon in 1 − 0.94⁴ ≈ **22 %** of Sanctums, either in ≈ **40 %**,
0.48 weapons per Sanctum — about one per two Sanctums.

## 7. Engine check

`BalanceFightTests.overworldNumbersAreTheEntities`: health, armor, attack and XP of all ten overworld mobs from the
live entity types, the Hollow's and the Ember Wisp's attack intervals, the creeper's explosion power.
`overworldMobsHitAsModelled`: a real survival player (level 1 in iron; the Hollow also against the level 10 player)
takes one hit — the mob's own `tryAttack`, a real arrow ticked into the player with the skeleton as owner, the
creeper's own explosion at 2 blocks (distance and exposure checked) — and deals one fully charged hit back.
`gearNumbersAreTheItems` now also covers leather, the stone sword, the iron axe and both Sanctum weapons.

Result: **no gap** — every hit both ways matches the model to two decimals at the test server's difficulty (Normal).

| Who | Lv | Player | Hit | Model | Engine |
| --- | --- | --- | --- | --- | --- |
| Ash Ghoul | 5 | Lv 1 (VIT 10, DEX 10, END 10, RES 10), iron, iron sword | claw (on player) | 1.78 | 1.78 |
| Ash Ghoul | 5 | Lv 1 (VIT 10, DEX 10, END 10, RES 10), iron, iron sword | player's hit | 6.00 | 6.00 |
| Hollow | 15 | Lv 1 (VIT 10, DEX 10, END 10, RES 10), iron, iron sword | iron sword (on player) | 6.51 | 6.51 |
| Hollow | 15 | Lv 1 (VIT 10, DEX 10, END 10, RES 10), iron, iron sword | player's hit | 5.28 | 5.28 |
| Ember Wisp | 11 | Lv 1 (VIT 10, DEX 10, END 10, RES 10), iron, iron sword | touch (+4 s fire) (on player) | 0.82 | 0.82 |
| Ember Wisp | 11 | Lv 1 (VIT 10, DEX 10, END 10, RES 10), iron, iron sword | player's hit | 6.00 | 6.00 |
| Zombie | 3 | Lv 1 (VIT 10, DEX 10, END 10, RES 10), iron, iron sword | swing (on player) | 1.99 | 1.99 |
| Zombie | 3 | Lv 1 (VIT 10, DEX 10, END 10, RES 10), iron, iron sword | player's hit | 5.90 | 5.90 |
| Skeleton | 4 | Lv 1 (VIT 10, DEX 10, END 10, RES 10), iron, iron sword | arrow (on player) | 2.96 | 2.96 |
| Skeleton | 4 | Lv 1 (VIT 10, DEX 10, END 10, RES 10), iron, iron sword | player's hit | 6.00 | 6.00 |
| Creeper | 6 | Lv 1 (VIT 10, DEX 10, END 10, RES 10), iron, iron sword | explosion at 2 blocks (on player) | 33.00 | 33.00 |
| Creeper | 6 | Lv 1 (VIT 10, DEX 10, END 10, RES 10), iron, iron sword | player's hit | 6.00 | 6.00 |
| Hollow | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | iron sword (on player) | 6.48 | 6.48 |
| Hollow | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | player's hit | 5.64 | 5.64 |

## 8. Tables (from `build/balance/overworld.md`)

After PLAN 16.4 / 18.3 (the vanilla types' `healthScale` / `damageScale`, the dungeon route at 4.5 guards and 6 chests).

### Level bands (DESIGN §5.4)

| Mob | Base | Lowest | Highest | Full moon highest | HP at lowest / highest | XP at lowest / highest |
| --- | --- | --- | --- | --- | --- | --- |
| Zombie | 3 | 3 | 11 | 14 | 16.8 / 24.0 | 6 / 8 |
| Spider | 3 | 3 | 11 | 14 | 17.9 / 25.6 | 6 / 8 |
| Skeleton | 4 | 4 | 12 | 15 | 23.6 / 33.2 | 6 / 8 |
| Creeper | 6 | 6 | 14 | 17 | 22.1 / 30.3 | 7 / 9 |
| Witch | 6 | 6 | 14 | 17 | 22.0 / 30.1 | 7 / 9 |
| Enderman | 12 | 12 | 20 | 23 | 53.1 / 68.5 | 8 / 11 |
| Vindicator | 4 | 4 | 9 | 12 | 36.8 / 46.2 | 6 / 7 |
| Ash Ghoul | 5 | 5 | 13 | 16 | 14.9 / 20.6 | 6 / 9 |
| Hollow | 15 | 15 | 20 | 23 | 46.0 / 53.5 | 37 / 43 |
| Ember Wisp | 8 | 11 | 16 | 19 | 12.8 / 15.2 | 10 / 11 |

### Fights, traded hit for hit (cells: its hit / its hits to kill you / your hits to kill it / % of your health it costs)

| Mob | Lv | Player | Easy | Normal | Hard |
| --- | --- | --- | --- | --- | --- |
| Zombie | 3 | Lv 1 leather, stone sword | 2.6 / 8 / 4 / 26 % | 3.3 / 7 / 4 / 33 % | 4.8 / 5 / 4 / 48 % |
| Zombie | 3 | Lv 1 iron, iron sword | 1.5 / 14 / 3 / 15 % | 2.0 / 11 / 3 / 20 % | 3.0 / 7 / 3 / 30 % |
| Zombie | 3 | Lv 10 iron, iron sword | 1.5 / 16 / 3 / 13 % | 2.0 / 13 / 3 / 17 % | 3.0 / 9 / 3 / 25 % |
| Zombie | 11 | Lv 1 leather, stone sword | 3.6 / 6 / 5 / 55 % | 4.7 / 5 / 5 / 70 % | 6.9 / 3 / 5 / 104 % |
| Zombie | 11 | Lv 1 iron, iron sword | 2.2 / 10 / 5 / 33 % | 2.9 / 7 / 5 / 43 % | 4.4 / 5 / 5 / 66 % |
| Zombie | 11 | Lv 10 iron, iron sword | 2.2 / 11 / 4 / 18 % | 2.9 / 9 / 4 / 24 % | 4.4 / 6 / 4 / 36 % |
| Spider | 3 | Lv 1 leather, stone sword | 1.7 / 12 / 4 / 17 % | 2.1 / 10 / 4 / 21 % | 3.1 / 7 / 4 / 31 % |
| Spider | 3 | Lv 1 iron, iron sword | 1.0 / 21 / 3 / 10 % | 1.3 / 16 / 3 / 13 % | 1.8 / 11 / 3 / 18 % |
| Spider | 3 | Lv 10 iron, iron sword | 1.0 / 25 / 3 / 8 % | 1.2 / 20 / 3 / 10 % | 1.8 / 14 / 3 / 15 % |
| Spider | 11 | Lv 1 leather, stone sword | 2.3 / 9 / 6 / 47 % | 3.0 / 7 / 6 / 60 % | 4.3 / 5 / 6 / 87 % |
| Spider | 11 | Lv 1 iron, iron sword | 1.4 / 15 / 5 / 21 % | 1.8 / 12 / 5 / 27 % | 2.7 / 8 / 5 / 40 % |
| Spider | 11 | Lv 10 iron, iron sword | 1.4 / 18 / 5 / 17 % | 1.8 / 14 / 5 / 22 % | 2.6 / 10 / 5 / 33 % |
| Skeleton | 4 | Lv 1 leather, stone sword | 3.7 / 6 / 5 / 19 % | 4.8 / 5 / 5 / 24 % | 7.1 / 3 / 5 / 35 % |
| Skeleton | 4 | Lv 1 iron, iron sword | 2.3 / 9 / 4 / 11 % | 3.0 / 7 / 4 / 15 % | 4.5 / 5 / 4 / 23 % |
| Skeleton | 4 | Lv 10 iron, iron sword | 2.3 / 11 / 4 / 9 % | 3.0 / 9 / 4 / 12 % | 4.5 / 6 / 4 / 19 % |
| Skeleton | 12 | Lv 1 leather, stone sword | 5.2 / 4 / 7 / 52 % | 6.8 / 3 / 7 / 68 % | 10.2 / 2 / 7 / 102 % |
| Skeleton | 12 | Lv 1 iron, iron sword | 3.2 / 7 / 6 / 32 % | 4.3 / 5 / 6 / 43 % | 6.7 / 3 / 6 / 67 % |
| Skeleton | 12 | Lv 10 iron, iron sword | 3.2 / 8 / 6 / 27 % | 4.3 / 6 / 6 / 36 % | 6.7 / 4 / 6 / 56 % |
| Creeper | 6 | Lv 1 leather, stone sword | 28.3 / 1 / 5 / 142 % | 35.4 / 1 / 5 / 177 % | 49.6 / 1 / 5 / 248 % |
| Creeper | 6 | Lv 1 iron, iron sword | 26.4 / 1 / 4 / 132 % | 33 / 1 / 4 / 165 % | 46.2 / 1 / 4 / 231 % |
| Creeper | 6 | Lv 10 iron, iron sword | 26.4 / 1 / 4 / 110 % | 33 / 1 / 4 / 138 % | 46.2 / 1 / 4 / 193 % |
| Creeper | 14 | Lv 1 leather, stone sword | 37.4 / 1 / 7 / 187 % | 46.7 / 1 / 7 / 234 % | 65.4 / 1 / 7 / 327 % |
| Creeper | 14 | Lv 1 iron, iron sword | 34.8 / 1 / 6 / 174 % | 43.6 / 1 / 6 / 218 % | 61.0 / 1 / 6 / 305 % |
| Creeper | 14 | Lv 10 iron, iron sword | 34.8 / 1 / 5 / 145 % | 43.6 / 1 / 5 / 182 % | 61.0 / 1 / 5 / 254 % |
| Witch | 6 | Lv 1 leather, stone sword | 3.8 / 6 / 5 / 19 % | 4.7 / 5 / 5 / 23 % | 6.6 / 4 / 5 / 33 % |
| Witch | 6 | Lv 1 iron, iron sword | 3.8 / 6 / 4 / 19 % | 4.7 / 5 / 4 / 23 % | 6.6 / 4 / 4 / 33 % |
| Witch | 6 | Lv 10 iron, iron sword | 3.8 / 7 / 4 / 16 % | 4.7 / 6 / 4 / 20 % | 6.6 / 4 / 4 / 27 % |
| Witch | 14 | Lv 1 leather, stone sword | 5.0 / 5 / 7 / 50 % | 6.2 / 4 / 7 / 62 % | 8.7 / 3 / 7 / 87 % |
| Witch | 14 | Lv 1 iron, iron sword | 5.0 / 5 / 6 / 50 % | 6.2 / 4 / 6 / 62 % | 8.7 / 3 / 6 / 87 % |
| Witch | 14 | Lv 10 iron, iron sword | 5.0 / 5 / 5 / 21 % | 6.2 / 4 / 5 / 26 % | 8.7 / 3 / 5 / 36 % |
| Enderman | 12 | Lv 1 leather, stone sword | 6.1 / 4 / 11 / 213 % | 7.9 / 3 / 11 / 277 % | 11.7 / 2 / 11 / 408 % |
| Enderman | 12 | Lv 1 iron, iron sword | 3.8 / 6 / 9 / 114 % | 5.1 / 4 / 9 / 152 % | 8.0 / 3 / 9 / 239 % |
| Enderman | 12 | Lv 10 iron, iron sword | 3.8 / 7 / 9 / 79 % | 5.1 / 5 / 9 / 105 % | 7.9 / 4 / 9 / 166 % |
| Enderman | 20 | Lv 1 leather, stone sword | 8.0 / 3 / 14 / 358 % | 10.4 / 2 / 14 / 470 % | 14.7 / 2 / 14 / 660 % |
| Enderman | 20 | Lv 1 iron, iron sword | 5.1 / 4 / 12 / 179 % | 6.9 / 3 / 12 / 241 % | 11.0 / 2 / 12 / 386 % |
| Enderman | 20 | Lv 10 iron, iron sword | 5.1 / 5 / 11 / 149 % | 6.9 / 4 / 11 / 200 % | 11.0 / 3 / 11 / 320 % |
| Vindicator | 4 | Lv 1 leather, stone sword | 5.0 / 4 / 8 / 126 % | 6.5 / 4 / 8 / 162 % | 9.7 / 3 / 8 / 243 % |
| Vindicator | 4 | Lv 1 iron, iron sword | 3.1 / 7 / 7 / 62 % | 4.1 / 5 / 7 / 82 % | 6.4 / 4 / 7 / 128 % |
| Vindicator | 4 | Lv 10 iron, iron sword | 3.1 / 8 / 6 / 52 % | 4.1 / 6 / 6 / 68 % | 6.3 / 4 / 6 / 106 % |
| Vindicator | 9 | Lv 1 leather, stone sword | 6.3 / 4 / 10 / 189 % | 8.2 / 3 / 10 / 246 % | 12.0 / 2 / 10 / 361 % |
| Vindicator | 9 | Lv 1 iron, iron sword | 4.0 / 6 / 8 / 99 % | 5.3 / 4 / 8 / 132 % | 8.3 / 3 / 8 / 209 % |
| Vindicator | 9 | Lv 10 iron, iron sword | 4.0 / 7 / 8 / 82 % | 5.3 / 5 / 8 / 110 % | 8.3 / 3 / 8 / 173 % |
| Ash Ghoul | 5 | Lv 1 leather, stone sword | 2.3 / 9 / 3 / 23 % | 3.0 / 7 / 3 / 30 % | 4.3 / 5 / 3 / 43 % |
| Ash Ghoul | 5 | Lv 1 iron, iron sword | 1.4 / 15 / 3 / 14 % | 1.8 / 12 / 3 / 18 % | 2.7 / 8 / 3 / 27 % |
| Ash Ghoul | 5 | Lv 10 iron, iron sword | 1.4 / 18 / 3 / 11 % | 1.8 / 14 / 3 / 15 % | 2.6 / 10 / 3 / 22 % |
| Ash Ghoul | 13 | Lv 1 leather, stone sword | 3.2 / 7 / 5 / 48 % | 4.1 / 5 / 5 / 61 % | 6.0 / 4 / 5 / 90 % |
| Ash Ghoul | 13 | Lv 1 iron, iron sword | 1.9 / 11 / 4 / 19 % | 2.5 / 8 / 4 / 25 % | 3.8 / 6 / 4 / 38 % |
| Ash Ghoul | 13 | Lv 10 iron, iron sword | 1.9 / 13 / 4 / 16 % | 2.5 / 10 / 4 / 21 % | 3.8 / 7 / 4 / 31 % |
| Hollow | 15 | Lv 1 leather, stone sword | 7.6 / 3 / 11 / 151 % | 9.9 / 3 / 11 / 198 % | 14.0 / 2 / 11 / 281 % |
| Hollow | 15 | Lv 1 iron, iron sword | 4.8 / 5 / 9 / 73 % | 6.5 / 4 / 9 / 98 % | 10.4 / 2 / 9 / 156 % |
| Hollow | 15 | Lv 10 iron, iron sword | 4.8 / 5 / 9 / 60 % | 6.5 / 4 / 9 / 81 % | 10.3 / 3 / 9 / 129 % |
| Hollow | 20 | Lv 1 leather, stone sword | 8.9 / 3 / 13 / 178 % | 11.5 / 2 / 13 / 230 % | 16.1 / 2 / 13 / 322 % |
| Hollow | 20 | Lv 1 iron, iron sword | 5.8 / 4 / 11 / 116 % | 7.8 / 3 / 11 / 157 % | 12.6 / 2 / 11 / 253 % |
| Hollow | 20 | Lv 10 iron, iron sword | 5.8 / 5 / 10 / 72 % | 7.8 / 4 / 10 / 98 % | 12.6 / 2 / 10 / 157 % |
| Ember Wisp | 11 | Lv 1 leather, stone sword | 1.1 / 18 / 3 / 13 % | 1.4 / 15 / 3 / 15 % | 2.0 / 10 / 3 / 18 % |
| Ember Wisp | 11 | Lv 1 iron, iron sword | 0.6 / 32 / 3 / 11 % | 0.8 / 25 / 3 / 12 % | 1.2 / 17 / 3 / 13 % |
| Ember Wisp | 11 | Lv 10 iron, iron sword | 0.6 / 38 / 3 / 9 % | 0.8 / 30 / 3 / 10 % | 1.2 / 21 / 3 / 11 % |
| Ember Wisp | 16 | Lv 1 leather, stone sword | 1.3 / 16 / 4 / 28 % | 1.7 / 12 / 4 / 32 % | 2.4 / 9 / 4 / 39 % |
| Ember Wisp | 16 | Lv 1 iron, iron sword | 0.8 / 27 / 3 / 11 % | 1.0 / 21 / 3 / 12 % | 1.4 / 15 / 3 / 15 % |
| Ember Wisp | 16 | Lv 10 iron, iron sword | 0.8 / 32 / 3 / 9 % | 1.0 / 25 / 3 / 10 % | 1.4 / 18 / 3 / 12 % |

| Ash Ghoul pack of 4, lowest level, all attacking | Easy | Normal | Hard | Budget (health + flask) |
| --- | --- | --- | --- | --- |
| Lv 1 leather, stone sword | 43.9 | 55.9 | 81.2 | 44 |
| Lv 1 iron, iron sword | 25.9 | 33.4 | 49.7 | 44 |

Second hits (one each, lowest level, level 1 in iron):

| Mob | Hit | Easy | Normal | Hard |
| --- | --- | --- | --- | --- |
| Creeper | explosion at 4 blocks | 8.1 | 11.1 | 18.3 |
| Hollow | stone sword | 3.6 | 4.8 | 7.6 |

### The Hollow's swing through full iron (DESIGN §3.2: ~6 on Normal), weapons 70 % iron / 30 % stone sword

| Level | Easy | Normal | Hard |
| --- | --- | --- | --- |
| 15 | 4.5 | 6.0 | 9.5 |
| 20 | 5.4 | 7.2 | 11.6 |
| 23 | 5.9 | 8.0 | 12.9 |

### XP on the way to the Sanctum (Route[surfaceKills=30, caveKills=20, coalOre=40, ironSmelted=30, goldSmelted=4, redstoneOre=6, lapisOre=4, diamondOre=2, sanctumHollows=4.5, sanctumArchers=1.0, sanctumChests=6])

| Surface kills | Cave kills | Mining | Smelting | Sanctum guards + chests | Total | Level bought from 1 |
| --- | --- | --- | --- | --- | --- | --- |
| 191.0 | 141.4 | 82 | 25 | 190 | 629.4 | 11 |

Mean XP per kill: surface 6.4, below y=0 7.1.

| Level | Cost of the next | Total to reach |
| --- | --- | --- |
| 1 | 26 | 0 |
| 2 | 33 | 26 |
| 3 | 39 | 59 |
| 4 | 46 | 98 |
| 5 | 54 | 144 |
| 6 | 61 | 198 |
| 7 | 69 | 259 |
| 8 | 78 | 328 |
| 9 | 86 | 406 |
| 10 | 95 | 492 |
| 11 | 104 | 587 |
| 12 | 114 | 691 |

### Weapons for the Sanctum-level player (Lv 10) against armor 10 and armor 0

| Weapon | Damage | Speed | Hit (armor 10) | Per second (armor 10) | Hit (armor 0) | Per second (armor 0) |
| --- | --- | --- | --- | --- | --- | --- |
| iron sword | 6 | 1.6 | 4.6 | 7.5 | 6.4 | 10.3 |
| diamond sword | 7 | 1.6 | 5.6 | 9.0 | 7.4 | 12.1 |
| iron axe | 9 | 0.9 | 7.0 | 6.4 | 9 | 8.2 |
| diamond axe | 9 | 1 | 7.0 | 7.1 | 9 | 9.1 |
| Sanctum Longsword | 6.5 | 1.6 | 5.1 | 8.3 | 6.9 | 11.2 |
| Ashbearer's Mace | 8 | 0.9 | 6.1 | 5.6 | 8 | 7.3 |
| Ashen Scythe | 12 | 1.1 | 10.4 | 11.6 | 12.3 | 13.7 |
| Warden's Scourge | 10 | 1 | 8.6 | 8.7 | 10.6 | 10.8 |
| Royal Ash Rapier | 7 | 2.2 | 5.4 | 12.1 | 7.3 | 16.3 |

Specials (armor 0): Wide Cleave 8.6 to every mob in the arc for 25 stamina / 3 s; Ground Slam 6 to every mob around for 30 stamina / 5 s.

