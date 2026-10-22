# Balance 0.4.0 — Realm 1 "Ashen Reach"

R40.1a (model), R40.1b (engine checks), R40.1c (tuning). Track `release-balance`.

## 1. What this is

A balance calculator for Realm 1 and the moon nights, in `com.embervoid.balance`:

- `BalanceModel` (main, pure) runs the game's own formula classes with the config defaults: `stats.StatCurve` and
  `DerivedStats` (stat effects), `StatsConfig.Leveling` (level cost), `mob.MobLevelRules` (mob level HP / damage /
  XP, difficulty factors), `boss.BossScaling` + `BossConfig` (2–3 players), vanilla `DamageUtil` (armor and
  toughness), `reach.gear.WeaponRules` and `GearConfig` (Kindling, Smoldering fire reduction).
- `BalanceData` (main) holds the base numbers that live in entity code: HP, armor, attack, XP, move damages. Public
  constants of the entity classes are read directly; the rest is checked against the live entities by the
  `BalanceFightTests` GameTest (R40.1b), so the table cannot drift from the game.
- `BalanceDataTest` (JUnit) checks the world numbers against the game's files: placed enemies in every structure
  template, structure-set weights, natural spawn weights (`reachMobs.spawns`), arena boss levels.
- `BalanceReportTest` writes every table below to `build/balance/tables.md` (`./gradlew test --tests
  'com.embervoid.balance.*'`); the tables here are pasted from it.

Not modelled, all in the player's favour: crits (×1.5), enchantments (Protection, Sharpness), blocking, dodging,
knockback, potions, food. "Trade" is the worst honest case: both sides swing as fast as they can until one dies.

## 2. Assumptions (`BalanceModel.Playthrough.NORMAL`)

- **Sanctum level 10** on the first step into the realm (overworld XP spent at the Maiden: 492 points).
- **Route per open zone:** both road outposts, 6 minor structures (contents by structure-set weight), 25 natural
  kills (by spawn weight), the zone boss. Necropolis also one crypt (each room once). **Depths:** every Citadel piece
  (7 Marksmen, 5 Golems, 4 Knights), 20 Cinder Hounds, the Smoldering Lord. Mob levels uniform over the zone band.
- **All XP into levels** at the Maiden after each zone. Real players also enchant, repair and lose a remnant now and
  then, so real levels sit a few below these: the model's levels are an upper bound.
- **Stat build:** a sword character repeating VIT, DEX, VIT, DEX, END, VIT, DEX, RES (3 of 8 points into VIT and DEX).
- **Gear per stage.** Floor kit: iron armor + iron sword (a zone entered first). Reference kit: full diamond +
  diamond sword. Depths: Smoldering Plate + Ashen Scythe (the ingot needs all three zones' materials, so neither the
  plate nor a realm weapon exists before). Flask: 3 charges + 1 Flask Shard per zone boss beaten, 8 HP each.
- **Normal difficulty** (×1.25 mob damage to players).

## 3. Targets

- **XP pace** (all XP into levels): depths entered at level 28–32, Realm 1 left at 32–37 (DESIGN §5.4: Realm 1
  comfortable 10–30, Realm 2 30–45); the first zone is worth at least 4 levels.
- **Zone order freedom:** any zone entered first, at its band's bottom, against the Sanctum-level player in diamond:
  a natural mob costs at most 40 % of max health traded hit for hit; a tank or elite needs at least 3 hits to kill
  the player and at most 30 of the player's. The Fields (the nearest zone) also in iron with an iron sword: at most
  50 %.
- **Boss hits** (DESIGN §4.5, against full diamond on Normal, no stat effects): light ~4–6, heavy ~8–12,
  unblockable ~12–17. Checked as: no ordinary hit above 12, unblockable hits 12–17, nothing above 17; hazards (pools,
  trails, the storm) are per tick and outside the classes.
- **Boss length:** a zone boss falls to the Sanctum-level player in diamond in at most 70 hits at its best openings;
  the Smoldering Lord to the depths player in at most 100.
- **Citadel:** elites die in at most 30 hits of the depths player and need at least 3 hits to kill them.
- **Moon nights:** a full moon wave near spawn (spot level 4) against a level 5 player in iron with the starting
  flask, traded hit for hit: two Ashen Zombies (two curse stacks) cleared with health + flask to spare, and one
  Ghost alone too. The whole Ghost wave is meant for a shield and kiting ("dying once is cheaper", DESIGN §3.3) and
  ends when its player dies. The Vampire boss at its lowest level hits a level 10 player in iron no harder than
  heavy (12).
- **Loot:** the Smoldering Plate and the realm weapons reachable from a normal route plus a modest amount of mining.

`BalanceTargetsTest` checks all of these on the model (R40.1c).

## 4. Before tuning: what was off (R40.1a)

1. **XP pace far too fast:** depths entered at level 37, Realm 1 left at 47. Realm mobs at levels 20–30 drop 3–4×
   their base XP (`experiencePerLevel` 0.10) and the bosses' base XP (250–500) multiplies the same way: the Lord
   alone gives 1950.
2. **Smoldering Lord hits far above the classes:** swing 13.1 (ordinary, > 12), overhead slam 20.9 and lunge 18.1
   (> 17), landing ring 15.5 and mortar 13.1 (ordinary, > 12). Against the expected depths player (Smoldering Plate,
   32 HP) the slam and the lunge kill in two.
3. **Citadel elites are sponges:** Furnace Golem and Smoldering Knight take 37 hits each at level 25 (41 at 30)
   from the depths player with the Scythe.
4. **Full-moon Ghosts cannot be beaten:** five Ghosts, each with twice an Ashen Zombie's HP and damage and attacking
   once a second through walls, deal ~1240 to a level 5 player in iron while it clears them (budget: 46 health +
   flask). Even one Ghost costs ~250 % of max health. The wave holds the night, so an uncursed player keeps dying for
   up to 30 minutes.
5. **Vampire boss vs an iron player** (the kit of day 28): leap 14.9 and blood nova 17.4 at level 15.
6. **Realm weapons trail vanilla:** per second against armor 10, Scythe 7.9, Scourge 7.6, Rapier 11.0 against a
   diamond sword's 11.4 and an enchanted netherite sword's 20.2; they cannot take Sharpness (PLAN follow-up).

What was on target already: every zone is survivable first (worst: Restless Bones 37 % of the diamond player at
level 21; in iron the Fields' worst is the Ash Stalker at 40 %); the zone bosses' hits sit in their classes (Crypt
Keeper's overhead 12.6 is the one unblockable); zone bosses fall in 33–70 hits; the loot covers the Smoldering
Plate and a weapon from the route alone.

## 5. Tuning (R40.1c)

Changes only to config defaults and base values (plus two small rule changes the coordinator approved), one commit
each. All targets of §3 now hold (`BalanceTargetsTest`), and `BalanceFightTests` measures the tuned numbers in the
engine.

| Change | Was → now | Why |
|---|---|---|
| Smoldering Lord base damage | swing 9 → 8, slam 12 → 10, lunge 11 → 8, landing 10 → 8, mortar 9 → 8; the slam now breaks a raised shield (`BossHits.shieldBreaking`, like the Crypt Keeper's overhead) | Against full diamond his ordinary hits were 13.1–18.1 and the slam 20.9; now 10.9 (heavy) and 15.5 (unblockable). The depths player in the Smoldering Plate takes 10.1 / 14.3 (4 / 3 hits to die). |
| Vampire boss base damage | claws 7 → 5, leap 9 → 7, nova 10 → 7.5 | At its lowest level (15) the leap (14.9) and nova (17.4) took two thirds of a level 10 iron player's health each. Now 6.5 / 10.3 / 11.4; at level 30 against diamond 5.4 / 8.8 / 9.8. |
| Boss base XP | Colossus 250 → 150, Tree 280 → 170, Keeper 300 → 190, Lord 500 → 300 | XP pace (§4.1). |
| Tank and elite base XP | Bark Hollow and Hollow Sentinel 30 → 22, Grave Warden 60 → 40, Sparking Marksman 20 → 15, Furnace Golem 45 → 30, Smoldering Knight 80 → 50 | XP pace; placed elites at levels 21–30 were the biggest XP source besides the bosses. |
| `mobs.levels.experiencePerLevel` | 0.10 → 0.06 (coordinator's answer; DESIGN §5.4) | The realm-local cuts alone left 34 / 41. Overworld: a level 15 Hollow drops 37 XP instead of 48, a level 3 zombie is unchanged. |
| Citadel elites' base health | Furnace Golem 90 → 70, Smoldering Knight 100 → 75 | 37 hits at level 25 for the depths player; 29 / 28 after this change, 22 / 21 with the stronger Scythe below. |
| `nightMobs.ghostCount` | 5 → 3 (coordinator's answer) | Five Ghosts: ~1240 damage to clear against a 46 budget. |
| Ghost attack cooldown | 20 → 40 ticks (coordinator's answer) | One Ghost traded hit for hit cost ~375 % of a level 5 iron player's health; now ~190 % with health + flask covering it. |
| A Ghost wave ends when its player dies | new rule in `nightmob.NightWaves` (coordinator's answer) | Ghosts pass walls and the wave held the night: a player who lost died again and again, burning remnant XP, for up to 30 minutes. The death still adds its curse stack (which, like every stack, resets when the full moon ends). |
| Realm melee weapons take sword enchantments | Sharpness, Smite, Bane of Arthropods, Fire Aspect, Looting, Knockback on the Scythe, Scourge and Rapier, anvil and table (coordinator's answer; PLAN follow-up) | Unenchantable, they lost to an enchanted netherite sword (20.2 damage per second against armor 10 vs 7.6–11). Not Sweeping Edge: only swords sweep the vanilla way, and the Scythe has its own sweep. |
| Ashen Scythe / Warden's Scourge damage | 8 → 10 / 8 → 9 (coordinator's answer; DESIGN §4.5) | A slow weapon now out-hits the fast diamond sword: 8.9 per hit each against armor 10 vs 6.5. |

**Level curve after tuning** (all XP into levels; real players, who also enchant, repair and lose a remnant, sit a
few levels lower):

| Stage | Level |
|---|---|
| Sanctum (first step into the realm) | 10 |
| After one zone | 17 (Fields), 19 (Woods), 22 (Necropolis) |
| After two zones | 22–27 |
| After three zones = depths entered | 30 |
| Realm 1 done (after the Smoldering Lord) | 36 |

Every zone stays survivable first at its band's bottom (§3; the Restless Bones 37 % is the worst natural mob, the
Grave Warden the worst elite at 4 hits to kill a level 10 player in diamond and 29 of the player's). Not changed,
and why:

- **Zone mobs and zone bosses:** inside every target already.
- **Loot rates:** the route alone (chests and realm loot on mobs, nothing mined) gives the Smoldering Plate plus one
  weapon; the plate and all three zone weapons need about 15 Grave Iron ore blocks, 3 Cinder and 6 Amber more. Grave
  Iron generates far richer than its "like diamond" note (5 veins of ~6 per Necropolis chunk), which the 40+ ingots
  of a full playthrough need. No change.
- **Level cost curve, difficulty factors, boss multiplayer scaling:** the tables of §8 (2–3 players: each player
  needs ~75 % / ~67 % of the solo hits, plus one add per extra player) sit where DESIGN put them.
- **Ashen Zombie waves:** two at spot level 9 cost a level 5 iron player 42 of a 46 budget; `zombieLevelBonus` stays 5.

## 6. Engine check (R40.1b)

`BalanceFightTests` (GameTest, `./gradlew runGametest`) measures the model's numbers in the engine:

- `modelNumbersAreTheEntities`: every Realm 1 mob and boss, the Ashen Zombie, Ghost and Vampire boss: base health,
  armor, attack and XP of the live entity type; the melee move cooldown is the model's attack interval; every boss
  move's base damage is the entity's own constant (read by reflection), so `BalanceData` cannot drift.
- `gearNumbersAreTheItems`: armor and toughness of the iron, diamond, Warden and Smoldering sets on a player;
  damage and speed of the vanilla swords and axe and the three realm melee weapons in hand.
- `zoneMobsHitAsModelled`, `citadelMobsHitAsModelled`, `bossesHitAsModelled`, `moonNightsHitAsModelled`: a real
  survival player at the model's stats (`PlayerStats.set`) and gear for the stage takes one hit of every mob and
  boss move at a fixed level (the mob's own `tryAttack` for its melee, the move's damage source otherwise: mob
  attack, the boss's fire, the storm's magic) and deals one fully charged, non-critical melee hit back
  (`player.attack`); max health of the leveled mob and both hits must match the model within float rounding.
  One tick each, nothing random (a Ghost's dodged hit is retried).

Result: **no gap**, before and after tuning (171 GameTests pass on the merged branch). Every hit, both ways, and every leveled max health matches the model to two decimals, at the
test server's difficulty (Normal). What the GameTests do not cover, and the model takes from code instead: the
second-phase armor drops (Colossus 10 → 3, Lord 12 → 4), the Colossus's back and core factors, the Tree's stun,
the Keeper's darkness (each has its own boss GameTest from PLAN 12–13), and anything about timing (attack
intervals are the moves' cooldowns, checked above; who lands first is the model's assumption).

<details><summary>Measured hits after tuning (build/gametest/balance-engine.md)</summary>

| Who | Lv | Player | Hit | Model | Engine |
| --- | --- | --- | --- | --- | --- |
| Ashen Zombie | 9 | Lv 5 (VIT 12, DEX 12, END 10, RES 10), iron, iron sword | swing (on player) | 3.78 | 3.78 |
| Ashen Zombie | 9 | Lv 5 (VIT 12, DEX 12, END 10, RES 10), iron, iron sword | player's hit | 6.14 | 6.14 |
| Ghost | 9 | Lv 5 (VIT 12, DEX 12, END 10, RES 10), iron, iron sword | touch (on player) | 9.52 | 9.52 |
| Ghost | 9 | Lv 5 (VIT 12, DEX 12, END 10, RES 10), iron, iron sword | player's hit | 6.24 | 6.24 |
| Vampire boss | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | claw (on player) | 6.48 | 6.48 |
| Vampire boss | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | leap (on player) | 10.32 | 10.32 |
| Vampire boss | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | blood nova (heals 25 %) (on player) | 11.39 | 11.39 |
| Vampire boss | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | player's hit (side) | 6.15 | 6.15 |
| Cinder Hound | 25 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | bite (+2 s fire) (on player) | 2.25 | 2.25 |
| Cinder Hound | 25 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | player's hit | 10.84 | 10.84 |
| Cinder Hound | 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | bite (+2 s fire) (on player) | 2.58 | 2.58 |
| Cinder Hound | 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | player's hit | 10.84 | 10.84 |
| Sparking Marksman | 25 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | bolt (+fire) (on player) | 5.68 | 5.68 |
| Sparking Marksman | 25 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | player's hit | 10.84 | 10.84 |
| Sparking Marksman | 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | bolt (+fire) (on player) | 6.63 | 6.63 |
| Sparking Marksman | 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | player's hit | 10.84 | 10.84 |
| Furnace Golem | 25 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | slam (on player) | 10.31 | 10.31 |
| Furnace Golem | 25 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | fire wave (on player) | 3.43 | 3.43 |
| Furnace Golem | 25 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | player's hit | 7.99 | 7.99 |
| Furnace Golem | 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | slam (on player) | 12.15 | 12.15 |
| Furnace Golem | 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | fire wave (on player) | 3.96 | 3.96 |
| Furnace Golem | 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | player's hit | 7.99 | 7.99 |
| Smoldering Knight | 25 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | combo hit (on player) | 8.63 | 8.63 |
| Smoldering Knight | 25 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | lunge (on player) | 12.11 | 12.11 |
| Smoldering Knight | 25 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | player's hit | 8.85 | 8.85 |
| Smoldering Knight | 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | combo hit (on player) | 10.15 | 10.15 |
| Smoldering Knight | 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | lunge (on player) | 14.33 | 14.33 |
| Smoldering Knight | 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | player's hit | 8.85 | 8.85 |
| Cinder Husk | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | swing (on player) | 1.68 | 1.68 |
| Cinder Husk | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | player's hit | 7.42 | 7.42 |
| Ash Stalker | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | swing / leap (on player) | 2.42 | 2.42 |
| Ash Stalker | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | player's hit | 7.42 | 7.42 |
| Ashen Carrion | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | dive (on player) | 2.42 | 2.42 |
| Ashen Carrion | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | player's hit | 7.42 | 7.42 |
| Withered Thrall | 18 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | swing (on player) | 2.27 | 2.27 |
| Withered Thrall | 18 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | player's hit | 7.42 | 7.42 |
| Lantern Hag | 18 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | fire flask (on player) | 2.70 | 2.70 |
| Lantern Hag | 18 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | player's hit | 7.42 | 7.42 |
| Bark Hollow | 18 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | sweep (on player) | 5.84 | 5.84 |
| Bark Hollow | 18 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | player's hit | 6.15 | 6.15 |
| Restless Bones | 21 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | swing (on player) | 2.99 | 2.99 |
| Restless Bones | 21 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | player's hit | 7.42 | 7.42 |
| Crypt Moth | 21 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | bite (on player) | 1.25 | 1.25 |
| Crypt Moth | 21 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | player's hit | 7.42 | 7.42 |
| Hollow Sentinel | 21 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | spear thrust (on player) | 6.54 | 6.54 |
| Hollow Sentinel | 21 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | shield bash (on player) | 5.24 | 5.24 |
| Hollow Sentinel | 21 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | player's hit | 5.55 | 5.55 |
| Grave Warden | 21 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | swing (on player) | 7.98 | 7.98 |
| Grave Warden | 21 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | player's hit | 6.15 | 6.15 |
| Cinder Husk | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | swing (on player) | 3.35 | 3.35 |
| Cinder Husk | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | player's hit | 6.36 | 6.36 |
| Ash Stalker | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | swing / leap (on player) | 4.83 | 4.83 |
| Ash Stalker | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | player's hit | 6.36 | 6.36 |
| Ashen Carrion | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | dive (on player) | 4.83 | 4.83 |
| Ashen Carrion | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | player's hit | 6.36 | 6.36 |
| Ash Colossus | 18 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | fist cone (on player) | 7.10 | 7.10 |
| Ash Colossus | 18 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | leap ring (on player) | 8.47 | 8.47 |
| Ash Colossus | 18 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | boulder (on player) | 5.84 | 5.84 |
| Ash Colossus | 18 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | arm drag (on player) | 4.69 | 4.69 |
| Ash Colossus | 18 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | falling cinder (P2) (on player) | 3.59 | 3.59 |
| Ash Colossus | 18 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | ash storm, per s (magic) (on player) | 2.00 | 2.00 |
| Ash Colossus | 18 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | player's hit (side) | 5.55 | 5.55 |
| Grieving Tree | 21 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | branch sweep (on player) | 7.98 | 7.98 |
| Grieving Tree | 21 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | root line (on player) | 6.54 | 6.54 |
| Grieving Tree | 21 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | root cage (P2) (on player) | 2.99 | 2.99 |
| Grieving Tree | 21 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | player's hit (side) | 1.79 | 1.79 |
| Crypt Keeper | 24 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | halberd swing (on player) | 4.48 | 4.48 |
| Crypt Keeper | 24 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | overhead (3rd hit) (on player) | 12.55 | 12.55 |
| Crypt Keeper | 24 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | sweep ring (on player) | 7.28 | 7.28 |
| Crypt Keeper | 24 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | thrust (on player) | 7.28 | 7.28 |
| Crypt Keeper | 24 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | lantern throw (on player) | 4.48 | 4.48 |
| Crypt Keeper | 24 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | fire pool, per 0.5 s (on player) | 1.35 | 1.35 |
| Crypt Keeper | 24 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | player's hit (side) | 6.15 | 6.15 |
| Smoldering Lord | 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | greatsword swing (on player) | 10.15 | 10.15 |
| Smoldering Lord | 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | overhead slam (on player) | 14.33 | 14.33 |
| Smoldering Lord | 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | lunge (on player) | 10.15 | 10.15 |
| Smoldering Lord | 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | landing ring (on player) | 10.15 | 10.15 |
| Smoldering Lord | 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | mortar (P0) (on player) | 5.52 | 5.52 |
| Smoldering Lord | 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | flame line (P0) (on player) | 5.52 | 5.52 |
| Smoldering Lord | 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | fire trail, per 0.5 s (on player) | 0.95 | 0.95 |
| Smoldering Lord | 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | player's hit (side) | 7.99 | 7.99 |

</details>

## 7. Left for play (no QA run by the user; noted for the merge)

- The model trades hits without dodging, blocking or crits; how long fights feel with real telegraphs (the Lord at
  ~63 s of pure swinging solo, the Keeper ~40 s) needs eyes.
- Whether the three-Ghost wave is fun with a shield, and whether ending it on death reads clearly (chat line only).
- XP pace with real play (enchanting and deaths eat XP): the curve above is an upper bound.

## 8. Tables (after tuning)

### XP by zone (Playthrough[sanctumLevel=10, structuresPerZone=6, cryptsPerZone=1, naturalKillsPerZone=25, naturalKillsDepths=20])

| Zone | Placed | Natural | Boss | Total |
| --- | --- | --- | --- | --- |
| Ashen Fields | 326.9 | 336.5 | 303 | 966.4 |
| Royal Woods | 406.5 | 483.9 | 374 | 1264.4 |
| Sunken Necropolis | 1050.4 | 470 | 452 | 1972.4 |
| Smoldering Citadel | 1178.2 | 363.3 | 822 | 2363.5 |

| Level | Cost of the next | Total to reach |
| --- | --- | --- |
| 10 | 95 | 492 |
| 15 | 144 | 1061 |
| 20 | 200 | 1889 |
| 25 | 264 | 3013 |
| 30 | 335 | 4471 |
| 35 | 414 | 6300 |
| 40 | 500 | 8538 |

### Expected player per stage

| Stage | Level | HP | Flask HP | Floor kit | Reference kit |
| --- | --- | --- | --- | --- | --- |
| Ashen Fields 1st | 10 | 24 | 24 | iron + iron sword | diamond + diamond sword |
| Ashen Fields 2nd (after Royal Woods) | 19 | 27 | 32 | diamond + diamond sword | diamond + diamond sword |
| Ashen Fields 2nd (after Sunken Necropolis) | 22 | 28 | 32 | diamond + diamond sword | diamond + diamond sword |
| Ashen Fields 3rd (after Royal Woods + Sunken Necropolis) | 27 | 30 | 40 | diamond + diamond sword | diamond + diamond sword |
| Royal Woods 1st | 10 | 24 | 24 | iron + iron sword | diamond + diamond sword |
| Royal Woods 2nd (after Ashen Fields) | 17 | 26 | 32 | diamond + diamond sword | diamond + diamond sword |
| Royal Woods 2nd (after Sunken Necropolis) | 22 | 28 | 32 | diamond + diamond sword | diamond + diamond sword |
| Royal Woods 3rd (after Ashen Fields + Sunken Necropolis) | 26 | 30 | 40 | diamond + diamond sword | diamond + diamond sword |
| Sunken Necropolis 1st | 10 | 24 | 24 | iron + iron sword | diamond + diamond sword |
| Sunken Necropolis 2nd (after Ashen Fields) | 17 | 26 | 32 | diamond + diamond sword | diamond + diamond sword |
| Sunken Necropolis 2nd (after Royal Woods) | 19 | 27 | 32 | diamond + diamond sword | diamond + diamond sword |
| Sunken Necropolis 3rd (after Ashen Fields + Royal Woods) | 23 | 29 | 40 | diamond + diamond sword | diamond + diamond sword |
| depths | 30 | 30.5 | 48 | Smoldering Plate + diamond sword | Smoldering Plate + Ashen Scythe |

### Mobs: hits both ways

Per mob: band bottom against the player entering the zone first, in the floor kit and in the reference kit;
band top against the player entering it third in the reference kit. Citadel: the depths player, bottom and top.

| Mob | Lv | Player | Mob HP | Player hit | Hits to kill | Mob hit | Hits to kill player | Trade (% HP) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Cinder Husk | 15 | Lv 10 iron, iron sword | 29.4 | 6.4 | 5 | 3.4 | 8 | 27.9 |
| Cinder Husk | 15 | Lv 10 diamond, diamond sword | 29.4 | 7.4 | 4 | 1.7 | 15 | 14.0 |
| Cinder Husk | 18 | Lv 27 diamond, diamond sword | 32.3 | 8.4 | 4 | 1.9 | 17 | 12.4 |
| Ash Stalker | 15 | Lv 10 iron, iron sword | 25.8 | 6.4 | 5 | 4.8 | 5 | 40.2 |
| Ash Stalker | 15 | Lv 10 diamond, diamond sword | 25.8 | 7.4 | 4 | 2.4 | 10 | 20.2 |
| Ash Stalker | 18 | Lv 27 diamond, diamond sword | 28.3 | 8.4 | 4 | 2.7 | 12 | 17.9 |
| Ashen Carrion | 15 | Lv 10 iron, iron sword | 33.1 | 6.4 | 6 | 4.8 | 5 | 20.1 |
| Ashen Carrion | 15 | Lv 10 diamond, diamond sword | 33.1 | 7.4 | 5 | 2.4 | 10 | 10.1 |
| Ashen Carrion | 18 | Lv 27 diamond, diamond sword | 36.4 | 8.4 | 5 | 2.7 | 12 | 9.0 |
| Withered Thrall | 18 | Lv 10 iron, iron sword | 36.4 | 6.4 | 6 | 4.5 | 6 | 56.6 |
| Withered Thrall | 18 | Lv 10 diamond, diamond sword | 36.4 | 7.4 | 5 | 2.3 | 11 | 18.9 |
| Withered Thrall | 21 | Lv 26 diamond, diamond sword | 39.6 | 8.3 | 5 | 2.5 | 12 | 16.7 |
| Lantern Hag | 18 | Lv 10 iron, iron sword | 32.3 | 6.4 | 6 | 5.4 | 5 | 44.9 |
| Lantern Hag | 18 | Lv 10 diamond, diamond sword | 32.3 | 7.4 | 5 | 2.7 | 9 | 11.3 |
| Lantern Hag | 21 | Lv 26 diamond, diamond sword | 35.2 | 8.3 | 5 | 3.0 | 11 | 9.9 |
| Bark Hollow | 18 | Lv 10 iron, iron sword | 101.0 | 5.1 | 20 | 11.7 | 3 | 242.7 |
| Bark Hollow | 18 | Lv 10 diamond, diamond sword | 101.0 | 6.1 | 17 | 5.8 | 5 | 97.4 |
| Bark Hollow | 21 | Lv 26 diamond, diamond sword | 110.0 | 7.0 | 16 | 6.5 | 5 | 86.7 |
| Restless Bones | 21 | Lv 10 iron, iron sword | 41.2 | 6.4 | 7 | 6.0 | 5 | 74.7 |
| Restless Bones | 21 | Lv 10 diamond, diamond sword | 41.2 | 7.4 | 6 | 3.0 | 9 | 37.4 |
| Restless Bones | 25 | Lv 23 diamond, diamond sword | 45.0 | 8.1 | 6 | 3.4 | 9 | 35.1 |
| Crypt Moth | 21 | Lv 10 iron, iron sword | 22.0 | 6.4 | 4 | 2.5 | 10 | 20.8 |
| Crypt Moth | 21 | Lv 10 diamond, diamond sword | 22.0 | 7.4 | 3 | 1.2 | 20 | 5.2 |
| Crypt Moth | 25 | Lv 23 diamond, diamond sword | 24.4 | 8.1 | 4 | 1.4 | 21 | 9.6 |
| Hollow Sentinel | 21 | Lv 10 iron, iron sword | 99.0 | 4.6 | 22 | 13.0 | 2 | 380.6 |
| Hollow Sentinel | 21 | Lv 10 diamond, diamond sword | 99.0 | 5.6 | 18 | 6.5 | 4 | 163.6 |
| Hollow Sentinel | 25 | Lv 23 diamond, diamond sword | 109.8 | 6.2 | 18 | 7.5 | 4 | 155.4 |
| Grave Warden | 21 | Lv 10 iron, iron sword | 176.0 | 5.1 | 35 | 15.9 | 2 | 728.8 |
| Grave Warden | 21 | Lv 10 diamond, diamond sword | 176.0 | 6.1 | 29 | 8.0 | 4 | 299.1 |
| Grave Warden | 25 | Lv 23 diamond, diamond sword | 195.2 | 6.8 | 29 | 9.2 | 4 | 284.9 |
| Cinder Hound | 25 | Lv 30 Smoldering Plate, Ashen Scythe | 34.2 | 10.8 | 4 | 2.2 | 14 | 22.1 |
| Cinder Hound | 30 | Lv 30 Smoldering Plate, Ashen Scythe | 38.4 | 10.8 | 4 | 2.6 | 12 | 25.3 |
| Sparking Marksman | 25 | Lv 30 Smoldering Plate, Ashen Scythe | 58.6 | 10.8 | 6 | 5.7 | 6 | 37.2 |
| Sparking Marksman | 30 | Lv 30 Smoldering Plate, Ashen Scythe | 65.8 | 10.8 | 7 | 6.6 | 5 | 43.5 |
| Furnace Golem | 25 | Lv 30 Smoldering Plate, Ashen Scythe | 170.8 | 8.0 | 22 | 10.3 | 3 | 270.3 |
| Furnace Golem | 30 | Lv 30 Smoldering Plate, Ashen Scythe | 191.8 | 8.0 | 25 | 12.2 | 3 | 358.7 |
| Smoldering Knight | 25 | Lv 30 Smoldering Plate, Ashen Scythe | 183.0 | 8.9 | 21 | 8.6 | 4 | 509.4 |
| Smoldering Knight | 30 | Lv 30 Smoldering Plate, Ashen Scythe | 205.5 | 8.9 | 24 | 10.1 | 4 | 665.4 |

### Bosses: the player's hits (solo)

| Boss | Lv | Player | Boss HP | Hit | Hits | Seconds | Best opening | Hit | Hits |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Ash Colossus | 18 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | 363.6 | 5.6 | 59 | 36.3 | back (P2: the bare core) | 9.2 | 33 |
| Ash Colossus | 18 | Lv 27 (VIT 20, DEX 20, END 13, RES 13), diamond, diamond sword | 363.6 | 6.5 | 52 | 31.0 | back (P2: the bare core) | 10.7 | 29 |
| Grieving Tree | 21 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | 374.0 | 1.8 | 210 | 129.3 | stunned (lanterns shot) | 10.6 | 36 |
| Grieving Tree | 21 | Lv 26 (VIT 20, DEX 19, END 13, RES 13), diamond, diamond sword | 374.0 | 2.0 | 188 | 112.4 | stunned (lanterns shot) | 11.8 | 32 |
| Crypt Keeper | 24 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | 428.4 | 6.1 | 70 | 43.1 | in brazier light (dark: ×0.5) | 6.1 | 70 |
| Crypt Keeper | 24 | Lv 23 (VIT 19, DEX 18, END 13, RES 12), diamond, diamond sword | 428.4 | 6.8 | 64 | 38.5 | in brazier light (dark: ×0.5) | 6.8 | 64 |
| Smoldering Lord | 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), Smoldering Plate, Ashen Scythe | 712.4 | 8.0 | 73 | 63.1 | - | 8.0 | 73 |

### Bosses: their hits on the expected player

| Boss | Move | Base | vs diamond ref | Class | vs expected (1st / 3rd) | Hits to kill (1st / 3rd) |
| --- | --- | --- | --- | --- | --- | --- |
| Ash Colossus | fist cone | 8 | 7.1 | light+ | 7.1 / 7.1 | 4 / 5 |
| Ash Colossus | leap ring | 9 | 8.5 | heavy | 8.5 / 8.4 | 3 / 4 |
| Ash Colossus | boulder | 7 | 5.9 | light | 5.8 / 5.8 | 5 / 6 |
| Ash Colossus | arm drag | 6 | 4.7 | light | 4.7 / 4.7 | 6 / 7 |
| Ash Colossus | falling cinder (P2) | 5 | 3.6 | chip | 3.6 / 3.5 | 7 / 9 |
| Ash Colossus | ash storm, per s (magic) | 2 | 2 | chip | 2 / 2 | 12 / 15 |
| Grieving Tree | branch sweep | 8 | 8.0 | light+ | 8.0 / 7.9 | 4 / 4 |
| Grieving Tree | root line | 7 | 6.6 | light+ | 6.5 / 6.5 | 4 / 5 |
| Grieving Tree | root cage (P2) | 4 | 3 | chip | 3.0 / 3.0 | 9 / 11 |
| Crypt Keeper | halberd swing | 5 | 4.5 | light | 4.5 / 4.5 | 6 / 7 |
| Crypt Keeper | overhead (3rd hit) | 10 | 12.6 | unblockable | 12.6 / 12.5 | 2 / 3 |
| Crypt Keeper | sweep ring | 7 | 7.3 | light+ | 7.3 / 7.3 | 4 / 4 |
| Crypt Keeper | thrust | 7 | 7.3 | light+ | 7.3 / 7.3 | 4 / 4 |
| Crypt Keeper | lantern throw | 5 | 4.5 | light | 4.5 / 4.5 | 6 / 7 |
| Crypt Keeper | fire pool, per 0.5 s | 2 | 1.4 | chip | 1.3 / 1.3 | 18 / 22 |
| Smoldering Lord | greatsword swing | 8 | 10.9 | heavy | 10.1 / 10.1 | 4 / 4 |
| Smoldering Lord | overhead slam | 10 | 15.5 | unblockable | 14.3 / 14.3 | 3 / 3 |
| Smoldering Lord | lunge | 8 | 10.9 | heavy | 10.1 / 10.1 | 4 / 4 |
| Smoldering Lord | landing ring | 8 | 10.9 | heavy | 10.1 / 10.1 | 4 / 4 |
| Smoldering Lord | mortar (P0) | 8 | 10.9 | heavy | 5.5 / 5.5 | 6 / 6 |
| Smoldering Lord | flame line (P0) | 8 | 10.9 | heavy | 5.5 / 5.5 | 6 / 6 |
| Smoldering Lord | fire trail, per 0.5 s | 2 | 1.6 | chip | 1.0 / 1.0 | 33 / 33 |

### Bosses with 2–3 players (each player like the solo row's 3rd-zone player)

| Boss | Players | Boss HP | Hits each | Adds | Add HP | Add: hits to kill |
| --- | --- | --- | --- | --- | --- | --- |
| Ash Colossus | 1 | 363.6 | 52 | 0 × Cinder Husk Lv 15 | 29.4 | 4 |
| Ash Colossus | 2 | 545.4 | 39 | 1 × Cinder Husk Lv 15 | 29.4 | 4 |
| Ash Colossus | 3 | 727.2 | 34 | 2 × Cinder Husk Lv 15 | 29.4 | 4 |
| Grieving Tree | 1 | 374.0 | 188 | 0 × Withered Thrall Lv 18 | 36.4 | 5 |
| Grieving Tree | 2 | 561.0 | 140 | 1 × Withered Thrall Lv 18 | 36.4 | 5 |
| Grieving Tree | 3 | 748.0 | 126 | 2 × Withered Thrall Lv 18 | 36.4 | 5 |
| Crypt Keeper | 1 | 428.4 | 64 | 0 × Restless Bones Lv 21 | 41.2 | 6 |
| Crypt Keeper | 2 | 642.6 | 48 | 1 × Restless Bones Lv 21 | 41.2 | 6 |
| Crypt Keeper | 3 | 856.8 | 43 | 2 × Restless Bones Lv 21 | 41.2 | 6 |
| Smoldering Lord | 1 | 712.4 | 73 | 0 × Cinder Hound Lv 27 | 35.8 | 4 |
| Smoldering Lord | 2 | 1068.6 | 55 | 1 × Cinder Hound Lv 27 | 35.8 | 4 |
| Smoldering Lord | 3 | 1424.8 | 49 | 2 × Cinder Hound Lv 27 | 35.8 | 4 |

### Moon nights

| Who | Lv | Player | HP | Player hit | Hits to kill | Its hit | Hits to kill player | Trade (% HP) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Ashen Zombie | 9 | Lv 5 (VIT 12, DEX 12, END 10, RES 10), iron, iron sword | 35.5 | 6.1 | 6 | 3.8 | 6 | 68.7 |
| Ghost | 9 | Lv 5 (VIT 12, DEX 12, END 10, RES 10), iron, iron sword | 71.0 | 6.2 | 12 | 9.5 | 3 | 173.1 |
| Ashen Zombie | 14 | Lv 5 (VIT 12, DEX 12, END 10, RES 10), iron, iron sword | 42.7 | 6.1 | 7 | 4.7 | 5 | 84.8 |
| Ghost | 14 | Lv 5 (VIT 12, DEX 12, END 10, RES 10), iron, iron sword | 85.4 | 6.2 | 14 | 12.0 | 2 | 273.8 |
| Ashen Zombie | 9 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | 35.5 | 6.3 | 6 | 3.8 | 7 | 62.8 |
| Ghost | 9 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | 71.0 | 6.4 | 12 | 9.5 | 3 | 157.9 |
| Ashen Zombie | 14 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | 42.7 | 6.3 | 7 | 4.6 | 6 | 77.4 |
| Ghost | 14 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | 85.4 | 6.4 | 14 | 12.0 | 3 | 249.5 |
| Ashen Zombie | 9 | Lv 15 (VIT 16, DEX 15, END 12, RES 11), iron, iron sword | 35.5 | 6.5 | 6 | 3.8 | 7 | 58.0 |
| Ghost | 9 | Lv 15 (VIT 16, DEX 15, END 12, RES 11), iron, iron sword | 71.0 | 6.6 | 11 | 9.5 | 3 | 145.7 |
| Ashen Zombie | 14 | Lv 15 (VIT 16, DEX 15, END 12, RES 11), iron, iron sword | 42.7 | 6.5 | 7 | 4.6 | 6 | 71.5 |
| Ghost | 14 | Lv 15 (VIT 16, DEX 15, END 12, RES 11), iron, iron sword | 85.4 | 6.6 | 13 | 12.0 | 3 | 184.3 |

| Vampire boss Lv | Player | Boss HP | Hit | Hits | Seconds | Claw / leap / nova |
| --- | --- | --- | --- | --- | --- | --- |
| 15 | Lv 5 (VIT 12, DEX 12, END 10, RES 10), iron, iron sword | 404.8 | 6.0 | 68 | 42.1 | 6.5 / 10.4 / 11.5 |
| 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | 404.8 | 6.2 | 66 | 40.6 | 6.5 / 10.3 / 11.4 |
| 15 | Lv 15 (VIT 16, DEX 15, END 12, RES 11), iron, iron sword | 404.8 | 6.4 | 64 | 39.0 | 6.5 / 10.3 / 11.4 |
| 20 | Lv 20 (VIT 18, DEX 17, END 12, RES 12), diamond, diamond sword | 470.8 | 7.7 | 61 | 36.8 | 3.9 / 6.3 / 7.0 |
| 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), diamond, diamond sword | 602.8 | 8.2 | 74 | 43.9 | 5.4 / 8.8 / 9.8 |

### Loot along the route (expected counts)

| Where | Cinder | Amber | Raw Grave Iron | Grave Iron Ingot | Smoldering Ingot | Template | Set piece |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Ashen Fields | 16.6 | 0.0 | 0.0 | 0.6 | 0.0 | 1.1 | 1.8 |
| Royal Woods | 0.0 | 11.2 | 0.0 | 0.6 | 0.0 | 1.1 | 1.7 |
| Sunken Necropolis | 0.0 | 0.0 | 21.5 | 1.9 | 0.1 | 1.3 | 2.8 |
| Smoldering Citadel | 5.3 | 3.7 | 1.7 | 1.6 | 0.3 | 0.4 | 0.4 |

| Goal (before the Lord) | Grave Iron needed | Cinder needed | Amber needed | Ore blocks to mine (GI / Cinder / Amber) |
| --- | --- | --- | --- | --- |
| Smoldering Plate + 0 weapon(s) | 15.2 | 11.2 | 7.6 | 0 / 0 / 0 |
| Smoldering Plate + 1 weapon(s) | 23.2 | 15.2 | 11.6 | 0 / 0 / 0 |
| Smoldering Plate + 3 weapon(s) | 39.2 | 23.2 | 19.6 | 15 / 3 / 6 |

### Weapons against a Citadel elite (armor 10) for the depths player

| Weapon | Damage | Speed | Hit (armor 10) | Per second |
| --- | --- | --- | --- | --- |
| diamond sword | 7 | 1.6 | 6.5 | 11.0 |
| netherite sword | 8 | 1.6 | 7.7 | 12.9 |
| netherite sword, Sharpness V | 11 | 1.6 | 11.5 | 19.4 |
| diamond axe | 9 | 1 | 7.0 | 7.4 |
| Ashen Scythe | 10 | 1.1 | 8.9 | 10.3 |
| Warden's Scourge | 9 | 0.9 | 8.9 | 8.4 |
| Royal Ash Rapier | 6 | 2 | 5.1 | 10.7 |

