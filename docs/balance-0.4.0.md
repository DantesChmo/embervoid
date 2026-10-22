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
  flask: two Ashen Zombies (two curse stacks) cleared with health + flask to spare; the Vampire boss at its lowest
  level hits a level 10 player in iron no harder than heavy (12).
- **Loot:** the Smoldering Plate and the realm weapons reachable from a normal route plus a modest amount of mining.

`BalanceTargetsTest` checks all of these on the model (R40.1c).

## 4. Before tuning: what is off (R40.1a)

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

What is on target already: every zone is survivable first (worst: Restless Bones 37 % of the diamond player at
level 21; in iron the Fields' worst is the Ash Stalker at 40 %); the zone bosses' hits sit in their classes (Crypt
Keeper's overhead 12.6 is the one unblockable); zone bosses fall in 33–70 hits; the loot covers the Smoldering
Plate and a weapon from the route alone.

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

Result: **no gap**. Every hit, both ways, and every leveled max health matches the model to two decimals, at the
test server's difficulty (Normal). What the GameTests do not cover, and the model takes from code instead: the
second-phase armor drops (Colossus 10 → 3, Lord 12 → 4), the Colossus's back and core factors, the Tree's stun,
the Keeper's darkness (each has its own boss GameTest from PLAN 12–13), and anything about timing (attack
intervals are the moves' cooldowns, checked above; who lands first is the model's assumption).

<details><summary>Measured hits (build/gametest/balance-engine.md, before tuning)</summary>

| Who | Lv | Player | Hit | Model | Engine |
| --- | --- | --- | --- | --- | --- |
| Ashen Zombie | 9 | Lv 5 (VIT 12, DEX 12, END 10, RES 10), iron, iron sword | swing (on player) | 3.78 | 3.78 |
| Ashen Zombie | 9 | Lv 5 (VIT 12, DEX 12, END 10, RES 10), iron, iron sword | player's hit | 6.14 | 6.14 |
| Ghost | 9 | Lv 5 (VIT 12, DEX 12, END 10, RES 10), iron, iron sword | touch (on player) | 9.52 | 9.52 |
| Ghost | 9 | Lv 5 (VIT 12, DEX 12, END 10, RES 10), iron, iron sword | player's hit | 6.24 | 6.24 |
| Vampire boss | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | claw (on player) | 10.32 | 10.32 |
| Vampire boss | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | leap (on player) | 14.87 | 14.87 |
| Vampire boss | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | blood nova (heals 25 %) (on player) | 17.42 | 17.42 |
| Vampire boss | 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | player's hit (side) | 6.15 | 6.15 |
| Cinder Hound | 25 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | bite (+2 s fire) (on player) | 2.24 | 2.24 |
| Cinder Hound | 25 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | player's hit | 8.77 | 8.77 |
| Cinder Hound | 30 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | bite (+2 s fire) (on player) | 2.57 | 2.57 |
| Cinder Hound | 30 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | player's hit | 8.77 | 8.77 |
| Sparking Marksman | 25 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | bolt (+fire) (on player) | 5.67 | 5.67 |
| Sparking Marksman | 25 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | player's hit | 8.77 | 8.77 |
| Sparking Marksman | 30 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | bolt (+fire) (on player) | 6.61 | 6.61 |
| Sparking Marksman | 30 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | player's hit | 8.77 | 8.77 |
| Furnace Golem | 25 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | slam (on player) | 10.28 | 10.28 |
| Furnace Golem | 25 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | fire wave (on player) | 3.37 | 3.37 |
| Furnace Golem | 25 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | player's hit | 6.10 | 6.10 |
| Furnace Golem | 30 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | slam (on player) | 12.12 | 12.12 |
| Furnace Golem | 30 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | fire wave (on player) | 3.90 | 3.90 |
| Furnace Golem | 30 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | player's hit | 6.10 | 6.10 |
| Smoldering Knight | 25 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | combo hit (on player) | 8.61 | 8.61 |
| Smoldering Knight | 25 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | lunge (on player) | 12.08 | 12.08 |
| Smoldering Knight | 25 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | player's hit | 6.80 | 6.80 |
| Smoldering Knight | 30 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | combo hit (on player) | 10.12 | 10.12 |
| Smoldering Knight | 30 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | lunge (on player) | 14.28 | 14.28 |
| Smoldering Knight | 30 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | player's hit | 6.80 | 6.80 |
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
| Smoldering Lord | 30 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | greatsword swing (on player) | 12.12 | 12.12 |
| Smoldering Lord | 30 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | overhead slam (on player) | 19.09 | 19.09 |
| Smoldering Lord | 30 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | lunge (on player) | 16.61 | 16.61 |
| Smoldering Lord | 30 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | landing ring (on player) | 14.28 | 14.28 |
| Smoldering Lord | 30 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | mortar (P0) (on player) | 6.41 | 6.41 |
| Smoldering Lord | 30 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | flame line (P0) (on player) | 5.42 | 5.42 |
| Smoldering Lord | 30 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | fire trail, per 0.5 s (on player) | 0.94 | 0.94 |
| Smoldering Lord | 30 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | player's hit (side) | 6.10 | 6.10 |

</details>

## 5. Tables (before tuning)

### XP by zone (Playthrough[sanctumLevel=10, structuresPerZone=6, cryptsPerZone=1, naturalKillsPerZone=25, naturalKillsDepths=20])

| Zone | Placed | Natural | Boss | Total |
| --- | --- | --- | --- | --- |
| Ashen Fields | 433.5 | 445.9 | 675 | 1554.4 |
| Royal Woods | 605.7 | 650.9 | 840 | 2096.6 |
| Sunken Necropolis | 1767.5 | 640 | 990 | 3397.5 |
| Smoldering Citadel | 2501.5 | 513.3 | 1950 | 4964.8 |

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
| Ashen Fields 2nd (after Royal Woods) | 23 | 29 | 32 | diamond + diamond sword | diamond + diamond sword |
| Ashen Fields 2nd (after Sunken Necropolis) | 28 | 30.5 | 32 | diamond + diamond sword | diamond + diamond sword |
| Ashen Fields 3rd (after Royal Woods + Sunken Necropolis) | 34 | 31.5 | 40 | diamond + diamond sword | diamond + diamond sword |
| Royal Woods 1st | 10 | 24 | 24 | iron + iron sword | diamond + diamond sword |
| Royal Woods 2nd (after Ashen Fields) | 20 | 28 | 32 | diamond + diamond sword | diamond + diamond sword |
| Royal Woods 2nd (after Sunken Necropolis) | 28 | 30.5 | 32 | diamond + diamond sword | diamond + diamond sword |
| Royal Woods 3rd (after Ashen Fields + Sunken Necropolis) | 32 | 31 | 40 | diamond + diamond sword | diamond + diamond sword |
| Sunken Necropolis 1st | 10 | 24 | 24 | iron + iron sword | diamond + diamond sword |
| Sunken Necropolis 2nd (after Ashen Fields) | 20 | 28 | 32 | diamond + diamond sword | diamond + diamond sword |
| Sunken Necropolis 2nd (after Royal Woods) | 23 | 29 | 32 | diamond + diamond sword | diamond + diamond sword |
| Sunken Necropolis 3rd (after Ashen Fields + Royal Woods) | 28 | 30.5 | 40 | diamond + diamond sword | diamond + diamond sword |
| depths | 37 | 32 | 48 | Smoldering Plate + diamond sword | Smoldering Plate + Ashen Scythe |

### Mobs: hits both ways

Per mob: band bottom against the player entering the zone first, in the floor kit and in the reference kit;
band top against the player entering it third in the reference kit. Citadel: the depths player, bottom and top.

| Mob | Lv | Player | Mob HP | Player hit | Hits to kill | Mob hit | Hits to kill player | Trade (% HP) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Cinder Husk | 15 | Lv 10 iron, iron sword | 29.4 | 6.4 | 5 | 3.4 | 8 | 27.9 |
| Cinder Husk | 15 | Lv 10 diamond, diamond sword | 29.4 | 7.4 | 4 | 1.7 | 15 | 14.0 |
| Cinder Husk | 18 | Lv 34 diamond, diamond sword | 32.3 | 8.5 | 4 | 1.9 | 17 | 11.8 |
| Ash Stalker | 15 | Lv 10 iron, iron sword | 25.8 | 6.4 | 5 | 4.8 | 5 | 40.2 |
| Ash Stalker | 15 | Lv 10 diamond, diamond sword | 25.8 | 7.4 | 4 | 2.4 | 10 | 20.2 |
| Ash Stalker | 18 | Lv 34 diamond, diamond sword | 28.3 | 8.5 | 4 | 2.7 | 12 | 17.0 |
| Ashen Carrion | 15 | Lv 10 iron, iron sword | 33.1 | 6.4 | 6 | 4.8 | 5 | 20.1 |
| Ashen Carrion | 15 | Lv 10 diamond, diamond sword | 33.1 | 7.4 | 5 | 2.4 | 10 | 10.1 |
| Ashen Carrion | 18 | Lv 34 diamond, diamond sword | 36.4 | 8.5 | 5 | 2.7 | 12 | 8.5 |
| Withered Thrall | 18 | Lv 10 iron, iron sword | 36.4 | 6.4 | 6 | 4.5 | 6 | 56.6 |
| Withered Thrall | 18 | Lv 10 diamond, diamond sword | 36.4 | 7.4 | 5 | 2.3 | 11 | 18.9 |
| Withered Thrall | 21 | Lv 32 diamond, diamond sword | 39.6 | 8.5 | 5 | 2.5 | 13 | 16.1 |
| Lantern Hag | 18 | Lv 10 iron, iron sword | 32.3 | 6.4 | 6 | 5.4 | 5 | 44.9 |
| Lantern Hag | 18 | Lv 10 diamond, diamond sword | 32.3 | 7.4 | 5 | 2.7 | 9 | 11.3 |
| Lantern Hag | 21 | Lv 32 diamond, diamond sword | 35.2 | 8.5 | 5 | 3.0 | 11 | 9.6 |
| Bark Hollow | 18 | Lv 10 iron, iron sword | 101.0 | 5.1 | 20 | 11.7 | 3 | 242.7 |
| Bark Hollow | 18 | Lv 10 diamond, diamond sword | 101.0 | 6.1 | 17 | 5.8 | 5 | 97.4 |
| Bark Hollow | 21 | Lv 32 diamond, diamond sword | 110.0 | 7.3 | 16 | 6.5 | 5 | 84.0 |
| Restless Bones | 21 | Lv 10 iron, iron sword | 41.2 | 6.4 | 7 | 6.0 | 5 | 74.7 |
| Restless Bones | 21 | Lv 10 diamond, diamond sword | 41.2 | 7.4 | 6 | 3.0 | 9 | 37.4 |
| Restless Bones | 25 | Lv 28 diamond, diamond sword | 45.0 | 8.4 | 6 | 3.4 | 10 | 22.2 |
| Crypt Moth | 21 | Lv 10 iron, iron sword | 22.0 | 6.4 | 4 | 2.5 | 10 | 20.8 |
| Crypt Moth | 21 | Lv 10 diamond, diamond sword | 22.0 | 7.4 | 3 | 1.2 | 20 | 5.2 |
| Crypt Moth | 25 | Lv 28 diamond, diamond sword | 24.4 | 8.4 | 3 | 1.4 | 22 | 4.6 |
| Hollow Sentinel | 21 | Lv 10 iron, iron sword | 99.0 | 4.6 | 22 | 13.0 | 2 | 380.6 |
| Hollow Sentinel | 21 | Lv 10 diamond, diamond sword | 99.0 | 5.6 | 18 | 6.5 | 4 | 163.6 |
| Hollow Sentinel | 25 | Lv 28 diamond, diamond sword | 109.8 | 6.5 | 18 | 7.5 | 5 | 147.3 |
| Grave Warden | 21 | Lv 10 iron, iron sword | 176.0 | 5.1 | 35 | 15.9 | 2 | 728.8 |
| Grave Warden | 21 | Lv 10 diamond, diamond sword | 176.0 | 6.1 | 29 | 8.0 | 4 | 299.1 |
| Grave Warden | 25 | Lv 28 diamond, diamond sword | 195.2 | 7.1 | 28 | 9.2 | 4 | 270.0 |
| Cinder Hound | 25 | Lv 37 Smoldering Plate, Ashen Scythe | 34.2 | 8.8 | 4 | 2.2 | 15 | 21.0 |
| Cinder Hound | 30 | Lv 37 Smoldering Plate, Ashen Scythe | 38.4 | 8.8 | 5 | 2.6 | 13 | 32.1 |
| Sparking Marksman | 25 | Lv 37 Smoldering Plate, Ashen Scythe | 58.6 | 8.8 | 7 | 5.7 | 6 | 35.4 |
| Sparking Marksman | 30 | Lv 37 Smoldering Plate, Ashen Scythe | 65.8 | 8.8 | 8 | 6.6 | 5 | 62.0 |
| Furnace Golem | 25 | Lv 37 Smoldering Plate, Ashen Scythe | 219.6 | 6.1 | 37 | 10.3 | 4 | 417.5 |
| Furnace Golem | 30 | Lv 37 Smoldering Plate, Ashen Scythe | 246.6 | 6.1 | 41 | 12.1 | 3 | 530.2 |
| Smoldering Knight | 25 | Lv 37 Smoldering Plate, Ashen Scythe | 244.0 | 6.8 | 36 | 8.6 | 4 | 834.0 |
| Smoldering Knight | 30 | Lv 37 Smoldering Plate, Ashen Scythe | 274.0 | 6.8 | 41 | 10.1 | 4 | 1106.8 |

### Bosses: the player's hits (solo)

| Boss | Lv | Player | Boss HP | Hit | Hits | Seconds | Best opening | Hit | Hits |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Ash Colossus | 18 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | 363.6 | 5.6 | 59 | 36.3 | back (P2: the bare core) | 9.2 | 33 |
| Ash Colossus | 18 | Lv 34 (VIT 23, DEX 22, END 14, RES 14), diamond, diamond sword | 363.6 | 6.6 | 50 | 29.6 | back (P2: the bare core) | 11.0 | 28 |
| Grieving Tree | 21 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | 374.0 | 1.8 | 210 | 129.3 | stunned (lanterns shot) | 10.6 | 36 |
| Grieving Tree | 21 | Lv 32 (VIT 22, DEX 22, END 14, RES 13), diamond, diamond sword | 374.0 | 2.1 | 180 | 106.6 | stunned (lanterns shot) | 12.2 | 32 |
| Crypt Keeper | 24 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), diamond, diamond sword | 428.4 | 6.1 | 70 | 43.1 | in brazier light (dark: ×0.5) | 6.1 | 70 |
| Crypt Keeper | 24 | Lv 28 (VIT 21, DEX 20, END 13, RES 13), diamond, diamond sword | 428.4 | 7.1 | 62 | 36.9 | in brazier light (dark: ×0.5) | 7.1 | 62 |
| Smoldering Lord | 30 | Lv 37 (VIT 24, DEX 24, END 14, RES 14), Smoldering Plate, Ashen Scythe | 712.4 | 6.1 | 93 | 79.8 | - | 6.1 | 93 |

### Bosses: their hits on the expected player

| Boss | Move | Base | vs diamond ref | Class | vs expected (1st / 3rd) | Hits to kill (1st / 3rd) |
| --- | --- | --- | --- | --- | --- | --- |
| Ash Colossus | fist cone | 8 | 7.1 | light+ | 7.1 / 7.0 | 4 / 5 |
| Ash Colossus | leap ring | 9 | 8.5 | heavy | 8.5 / 8.4 | 3 / 4 |
| Ash Colossus | boulder | 7 | 5.9 | light | 5.8 / 5.8 | 5 / 6 |
| Ash Colossus | arm drag | 6 | 4.7 | light | 4.7 / 4.7 | 6 / 7 |
| Ash Colossus | falling cinder (P2) | 5 | 3.6 | chip | 3.6 / 3.4 | 7 / 10 |
| Ash Colossus | ash storm, per s (magic) | 2 | 2 | chip | 2 / 2 | 12 / 16 |
| Grieving Tree | branch sweep | 8 | 8.0 | light+ | 8.0 / 7.9 | 4 / 4 |
| Grieving Tree | root line | 7 | 6.6 | light+ | 6.5 / 6.5 | 4 / 5 |
| Grieving Tree | root cage (P2) | 4 | 3 | chip | 3.0 / 3.0 | 9 / 11 |
| Crypt Keeper | halberd swing | 5 | 4.5 | light | 4.5 / 4.5 | 6 / 7 |
| Crypt Keeper | overhead (3rd hit) | 10 | 12.6 | unblockable | 12.6 / 12.5 | 2 / 3 |
| Crypt Keeper | sweep ring | 7 | 7.3 | light+ | 7.3 / 7.2 | 4 / 5 |
| Crypt Keeper | thrust | 7 | 7.3 | light+ | 7.3 / 7.2 | 4 / 5 |
| Crypt Keeper | lantern throw | 5 | 4.5 | light | 4.5 / 4.5 | 6 / 7 |
| Crypt Keeper | fire pool, per 0.5 s | 2 | 1.4 | chip | 1.3 / 1.3 | 18 / 24 |
| Smoldering Lord | greatsword swing | 9 | 13.1 | unblockable | 12.1 / 12.1 | 3 / 3 |
| Smoldering Lord | overhead slam | 12 | 20.9 | over | 19.1 / 19.1 | 2 / 2 |
| Smoldering Lord | lunge | 11 | 18.1 | over | 16.6 / 16.6 | 2 / 2 |
| Smoldering Lord | landing ring | 10 | 15.5 | unblockable | 14.3 / 14.3 | 3 / 3 |
| Smoldering Lord | mortar (P0) | 9 | 13.1 | unblockable | 6.4 / 6.4 | 5 / 5 |
| Smoldering Lord | flame line (P0) | 8 | 10.9 | heavy | 5.4 / 5.4 | 6 / 6 |
| Smoldering Lord | fire trail, per 0.5 s | 2 | 1.6 | chip | 0.9 / 0.9 | 35 / 35 |

### Bosses with 2–3 players (each player like the solo row's 3rd-zone player)

| Boss | Players | Boss HP | Hits each | Adds | Add HP | Add: hits to kill |
| --- | --- | --- | --- | --- | --- | --- |
| Ash Colossus | 1 | 363.6 | 50 | 0 × Cinder Husk Lv 15 | 29.4 | 4 |
| Ash Colossus | 2 | 545.4 | 38 | 1 × Cinder Husk Lv 15 | 29.4 | 4 |
| Ash Colossus | 3 | 727.2 | 34 | 2 × Cinder Husk Lv 15 | 29.4 | 4 |
| Grieving Tree | 1 | 374.0 | 180 | 0 × Withered Thrall Lv 18 | 36.4 | 5 |
| Grieving Tree | 2 | 561.0 | 136 | 1 × Withered Thrall Lv 18 | 36.4 | 5 |
| Grieving Tree | 3 | 748.0 | 120 | 2 × Withered Thrall Lv 18 | 36.4 | 5 |
| Crypt Keeper | 1 | 428.4 | 62 | 0 × Restless Bones Lv 21 | 41.2 | 5 |
| Crypt Keeper | 2 | 642.6 | 47 | 1 × Restless Bones Lv 21 | 41.2 | 5 |
| Crypt Keeper | 3 | 856.8 | 42 | 2 × Restless Bones Lv 21 | 41.2 | 5 |
| Smoldering Lord | 1 | 712.4 | 93 | 0 × Cinder Hound Lv 27 | 35.8 | 5 |
| Smoldering Lord | 2 | 1068.6 | 70 | 1 × Cinder Hound Lv 27 | 35.8 | 5 |
| Smoldering Lord | 3 | 1424.8 | 62 | 2 × Cinder Hound Lv 27 | 35.8 | 5 |

### Moon nights

| Who | Lv | Player | HP | Player hit | Hits to kill | Its hit | Hits to kill player | Trade (% HP) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Ashen Zombie | 9 | Lv 5 (VIT 12, DEX 12, END 10, RES 10), iron, iron sword | 35.5 | 6.1 | 6 | 3.8 | 6 | 68.7 |
| Ghost | 9 | Lv 5 (VIT 12, DEX 12, END 10, RES 10), iron, iron sword | 71.0 | 6.2 | 12 | 9.5 | 3 | 302.9 |
| Ashen Zombie | 14 | Lv 5 (VIT 12, DEX 12, END 10, RES 10), iron, iron sword | 42.7 | 6.1 | 7 | 4.7 | 5 | 84.8 |
| Ghost | 14 | Lv 5 (VIT 12, DEX 12, END 10, RES 10), iron, iron sword | 85.4 | 6.2 | 14 | 12.0 | 2 | 492.8 |
| Ashen Zombie | 9 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | 35.5 | 6.3 | 6 | 3.8 | 7 | 62.8 |
| Ghost | 9 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | 71.0 | 6.4 | 12 | 9.5 | 3 | 276.3 |
| Ashen Zombie | 14 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | 42.7 | 6.3 | 7 | 4.6 | 6 | 77.4 |
| Ghost | 14 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | 85.4 | 6.4 | 14 | 12.0 | 3 | 449.2 |
| Ashen Zombie | 9 | Lv 15 (VIT 16, DEX 15, END 12, RES 11), iron, iron sword | 35.5 | 6.5 | 6 | 3.8 | 7 | 58.0 |
| Ghost | 9 | Lv 15 (VIT 16, DEX 15, END 12, RES 11), iron, iron sword | 71.0 | 6.6 | 11 | 9.5 | 3 | 255.0 |
| Ashen Zombie | 14 | Lv 15 (VIT 16, DEX 15, END 12, RES 11), iron, iron sword | 42.7 | 6.5 | 7 | 4.6 | 6 | 71.5 |
| Ghost | 14 | Lv 15 (VIT 16, DEX 15, END 12, RES 11), iron, iron sword | 85.4 | 6.6 | 13 | 12.0 | 3 | 368.5 |

| Vampire boss Lv | Player | Boss HP | Hit | Hits | Seconds | Claw / leap / nova |
| --- | --- | --- | --- | --- | --- | --- |
| 15 | Lv 5 (VIT 12, DEX 12, END 10, RES 10), iron, iron sword | 404.8 | 6.0 | 68 | 42.1 | 10.4 / 15.0 / 17.5 |
| 15 | Lv 10 (VIT 14, DEX 13, END 11, RES 11), iron, iron sword | 404.8 | 6.2 | 66 | 40.6 | 10.3 / 14.9 / 17.4 |
| 15 | Lv 15 (VIT 16, DEX 15, END 12, RES 11), iron, iron sword | 404.8 | 6.4 | 64 | 39.0 | 10.3 / 14.9 / 17.4 |
| 20 | Lv 20 (VIT 18, DEX 17, END 12, RES 12), diamond, diamond sword | 470.8 | 7.7 | 61 | 36.8 | 6.3 / 9.1 / 10.7 |
| 30 | Lv 30 (VIT 21, DEX 21, END 14, RES 13), diamond, diamond sword | 602.8 | 8.2 | 74 | 43.9 | 8.8 / 13.0 / 15.3 |

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
| diamond sword | 7 | 1.6 | 6.7 | 11.4 |
| netherite sword | 8 | 1.6 | 7.9 | 13.4 |
| netherite sword, Sharpness V | 11 | 1.6 | 11.9 | 20.2 |
| diamond axe | 9 | 1 | 7.0 | 7.4 |
| Ashen Scythe | 8 | 1.1 | 6.8 | 7.9 |
| Warden's Scourge | 8 | 0.9 | 7.9 | 7.6 |
| Royal Ash Rapier | 6 | 2 | 5.2 | 11.0 |

