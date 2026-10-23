# Balance 0.5.0 — fight length and hit sizes (the measuring stick)

PLAN 18.1. The targets are DESIGN §5.5 (fight length, hit sizes, the wind-up rule) and §6.1 (weapons above
netherite). This write-up measures **today's game** against them and lists every miss: it is the work list for 18.3
(overworld, dungeon, moon nights) and 18.4 (Realm 1). No game number was changed here. The earlier write-ups stay as
history: [balance-0.4.0.md](balance-0.4.0.md) (Realm 1, R40.1) and [balance-overworld.md](balance-overworld.md).

## 1. What this is

On top of the 0.4.0 model in `com.embervoid.balance` (same formulas, same expected players):

- `BalanceData` (main) now has a **cadence** for every boss: each move's wind-up and §5.5 class, what the player gets
  to read it by (animation, sound, marked shape), and per phase a loop of **beats** (a move, the brain's cooldown
  after it, how often it comes), fixed pauses and **downtime** (a storm behind a pillar, adds to kill). Wind-ups and
  cooldowns are the entity classes' own public constants (they were literals; made constants with no value changed,
  in a separate commit). The **Fallen Sister** (not built yet, PLAN 16.3) is there with placeholder numbers, marked as
  such.
- `FightLength` (main, pure) turns hits to kill into seconds of a fight with dodging (§2), solo and with 2–3 players,
  and holds the 0.5.0 stages: who fights each boss and mob, with which reference weapon (§5.5), and every boss move as
  a share of that player's max health.
- `BalanceModel` had the §6.1 starting numbers of the uniques as "0.5.0 proposed" next to the 0.4.0 ones; PLAN 17.3
  built them, so the model now has only the uniques as they are (and the Smoldering Plate at netherite's toughness).
- `FightLengthTargetsTest` (JUnit, tag `targets-0.5.0`) checks the targets of §3 on today's numbers. It is **red** and
  kept out of `./gradlew build`; `./gradlew balanceTargets` runs it alone and lists every miss. 18.3 / 18.4 drop the
  `excludeTags` line in `build.gradle` once it is green. `FightTargets` holds the checks (shared with the report).
- `FightLengthTest` (JUnit, in the build): the data is whole (a timing per boss move, phases going down), the openings
  follow the dodge, every fight is finite and shorter with more players. (The check that the §6.1 numbers meet §6.1
  moved to `BalanceTargetsTest.realmUniquesBeatNetherite` once 17.3 built them.)
- `BalanceReportTest` writes the tables of §7 to `build/balance/fight-length.md`.
- `BalanceFightTests.bossWindupsAreTheEntities` (GameTest) starts every boss move by hand and reads its wind-up back
  from `Telegraph` (new read-only `Telegraph.windups`), so the model's wind-ups are the ones the moves run.

`BalanceTargetsTest` and `OverworldTargetsTest` are green and hold the 0.4.0 targets until 18.3 / 18.4 replace them;
17.3 moved `BalanceTargetsTest`'s weapons to the 0.5.0 ladder (`realmUniquesBeatNetherite`: every unique above
netherite as in §3; the Sanctum weapons still between iron and diamond).

## 2. Assumptions

### The reference player per stage (§5.5)

| Stage | Player | Weapon |
|---|---|---|
| Overworld mobs, Ruined Sanctum guards, the Fallen Sister | level 1, all stats 10 (20 HP), full iron | iron sword |
| Full moon waves (spot level 4 + 5) | level 5, iron | iron sword |
| Blood new moon, the Vampire boss (level 15, its floor) | the Sanctum-level player: level 10 (24 HP), iron | iron sword |
| A zone entered first, its boss and its mobs at the band's bottom | level 10, diamond | diamond sword |
| A zone entered 2nd / 3rd, its boss | the 0.4.0 playthrough's level (17–27), diamond | each melee unique of a boss beaten before: Ashen Scythe (Fields), Warden's Scourge (Necropolis); the Woods give the Huntsman's Crossbow, not modelled, so after the Woods alone the diamond sword |
| A zone's mobs at the band's top | the player entering it 3rd | the first unique they hold |
| The Citadel, the Smoldering Lord | level 30, Smoldering Plate (30.5 HP) | Ashen Scythe and Warden's Scourge |

Levels, stat build and flasks are the 0.4.0 playthrough's (`Playthrough.NORMAL`), Normal difficulty. Bosses are judged
for hit sizes against the **first (weakest) player who fights them**: the Sister and the Vampire against their
players above, zone bosses against level 10 in diamond, the Lord against the depths player.

**Conflict found, settled:** §5.5 fights the 2nd and 3rd zone bosses with a unique, but in 0.4.0 a unique was crafted
from its soul and **2 Smoldering Ingots**, and an ingot needs all three zones' materials (DESIGN §4.5), so no unique
existed before the Citadel. The user's answer (DESIGN §6.1 "Uniques during the realm, not after it"), built in 17.3:
a zone boss's unique is its soul + 4 of its zone's material + the zone part (the Scythe 4 Cinder Shard, the Crossbow
4 Royal Amber, the Scourge 4 Grave Iron Ingot); the Rapier keeps its 2 Smoldering Ingots. The "no melee unique yet"
rows are a player whose only earlier boss was the Grieving Tree (its Crossbow is not modelled).

### Fight length

A boss fight is a loop of beats per phase. The player swings at full charge (the weapon's speed, DEX bonus included)
whenever the boss is open:

- **Melee move aimed at the player:** they keep swinging through its wind-up until **10 ticks** before the hit
  (`ESCAPE_TICKS`: about 2.5 blocks out of a cone or ring at a sprint), and from **16 ticks** after it (`REENGAGE_TICKS`:
  back into reach, about 9, and a reaction, about 7) through the rest of the brain's cooldown. A combo opens only before
  its first hit and after its last.
- **Closing move** (a leap, a lunge, a thrust from range): nothing before it lands, the recovery after it less 16.
- **Ranged move** (a boulder, the lantern throw, a backstep): nothing; the boss is away.
- **Not aimed at this player** (2–3 players: the boss aims at each for an equal share): a cone or line at someone
  else is all opening; rings and root lines still make everyone dodge.
- **Out of melee:** 20 % of the boss's picks happen with the player out of reach (knocked off, drinking, a dodge that
  went too far) and so are its ranged or closing moves (`OUT_OF_MELEE`).
- **Downtime** stops all swinging: a fixed share of the phase (the storm) or a one-off (bones rising), adds killed at
  the boss's add level (2–3 players share them).

Fight seconds = the boss's HP ÷ (fighters × damage per loop) × loop ticks ÷ (1 − downtime share) + pauses. The share of
the fight spent swinging is the **uptime**. DESIGN §5.5 expected "about 25–35 %"; derived from the code it is:

| Boss | Uptime (solo) | Why |
|---|---|---|
| Ash Colossus | **19 %** | Melee loop: fist (18-tick wind-up, 32 cooldown) and drag open 22–24 of ~50 ticks; 1 pick in 5 a boulder or leap from range. The ash storm takes 150 of every 400 ticks (roar, then behind a pillar). P2: cooldowns 22, so only 12–14 open, plus a step out of a cinder shadow now and then. |
| Grieving Tree | **46–53 %** | Rooted, it waits 40–50 ticks between moves, and **every branch sweep lowers a lantern a melee hit breaks**: a 100-tick stun at ×1.5 follows two moves in three. 72 % of its loop is open (the stun's 100 ticks less the walk back in); two Withered Thralls every 500 ticks cost about a quarter. The trunk outside stuns takes ×0.3, so the stun is the fight. |
| Crypt Keeper | **21–22 %** | The halberd combo chains 14 + 10 + 22 ticks of wind-ups: open are 4 ticks before the first swing and 19 of the 35-tick cooldown (23 of 81). The sweep ring makes everyone leave (33 of 59), the lantern throw is from range. P2 / P3 cooldowns 25; moths at a lit brazier every 300 ticks; three Restless Bones at P3. |
| Smoldering Lord | **19 %** | 13.5 s on the wall first (3 braziers: a run of ~30 ticks, a light of 10, a dodged wall attack of 40, then the 30-tick landing). The greatsword combo is 14 + 10 + 20 ticks with 18 of 74 open; P2 cooldowns 22 (10 of 66 open) and a zone hazard every 25 s (the storm 150 ticks, the fog about 100, the darkness none: he is fought under the signal braziers). |
| Vampire boss | **63 %** | A chaser that trades claws (vanilla melee, no wind-up, one per 20 ticks): every claw tick is an opening. Only the nova (30-tick ring, 4 open) and its leaps when the player backs off break it. |
| Fallen Sister (placeholder) | 25 % | Placeholder cadence (two censer arcs 18 + 12, a slam with an ember puddle she fights from, a backstep; P2 censer spin and crying soul dives). |

Not modelled, all in the player's favour: crits, blocking, enchantments, potions, the Scourge's tip and the Rapier's
ramp in boss fights (they count only in the weapon check), the Scythe's stamina back per enemy a sweep or Harvest
catches. Also not modelled: flask sips, the Colossus's front (×0.5)
and back (×1.5; a clean hit is a flank hit), arrows at the Tree's lanterns (it is fought in melee), the Lord's
marksmen, water slowing the player in the Keeper's P3. The Tree's lantern supply (5, then 4, plus one per 30 s when
fewer than two hang) is not a limit at today's HP: the fight needs about three stuns per phase.

## 3. Targets (DESIGN §5.5, §6.1)

| Target | Value | Test |
|---|---|---|
| Boss fight, solo, with dodging | 1.5–2.5 min | `bossFightsLastOneAndAHalfToTwoAndAHalfMinutes` |
| Boss clean hits, solo, reference weapon | 20–35 | `bossFightsTakeTwentyToThirtyFiveCleanHits` |
| Mob hits to kill | fodder 2–3, ambushers / ranged / flyers 3–4, tanks and elites 6–10 | `mobsDieInTheirBandsHits` |
| Mob hits to kill the player from full | fodder ≥ 6, ambushers / ranged / flyers ≥ 5, elites ≥ 4 | `mobsNeedTheirBandsHitsToKillThePlayer` |
| Boss move sizes | light ≤ 20 % of max HP, heavy ≤ 35 %, the biggest ≤ ⅔ | `bossMovesStayWithinTheirClass` |
| Wind-up rule | ≥ ⅓: wind-up ≥ 12 ticks and a tell; ≥ ½: ≥ 20 ticks and a marked shape or a sound | `bigBossHitsAreWoundUpAndTold` |
| Uniques above netherite (12.8 per second) | ≥ +20 % in own use (about +30 % meant), ≥ netherite on one target | `realmUniquesBeatNetherite` |

Decided in this task (small, marked here): flyers sit with ambushers and ranged, tanks with elites (§5.5 names only
fodder, ambushers and ranged, elites); the Ghost (twice an Ashen Zombie, the full moon's punishment) counts as an
elite; "≥ 5" for the middle band between fodder's 6 and elites' 4. A move's class is its role: combo hits and quick
swings light, telegraphed areas, lunges and throws heavy, each boss's unblockable or signature move its biggest.
Own use: the Scythe on two enemies (its sweep hits the second at 75 %), the Scourge at its tip (×1.6; the tip is PLAN
17.4, until then it hits one target plainly), the Rapier fully ramped (+30 %, built in 17.3). Multiplayer has no target in §5.5; its
fights are shown, not tested.

## 4. Results: today against the targets

### The two boss bands only meet at low uptime

Clean hits = seconds × uptime ÷ swing time. For 20–35 clean hits to fill 1.5–2.5 min the boss needs an uptime of
**8–24 %** with a sword (0.6 s a swing) and **13–43 %** with a 1.0–1.1-speed unique. So:

- The **Colossus, the Keeper and the Lord** (18–22 %) can meet both (table "What 1.5–2.5 min means"): with the
  diamond sword 27–35 clean hits at the Colossus and 32–35 at the Keeper, with the Scourge 20–29 at the Colossus and
  20–28 at the Lord, with the Scythe 22–35 at the Keeper and 20–31 at the Lord. Their HP goes **down**, to about
  ×0.47–0.61 (Colossus, diamond sword; ×0.57–0.82 with the Scourge), ×0.46–0.50 (Keeper, diamond sword; ×0.61–0.96
  with the Scythe) and ×0.35–0.45 (Lord, both uniques) of today; the first visit with the diamond sword is the
  tighter bound.
- The **Grieving Tree** (46–53 %), the **Vampire** (63 %) and the **Fallen Sister** placeholder (25 % with a sword)
  cannot: at their uptime 1.5 min takes 36–90 clean hits. Their HP alone cannot fix it; their cadence must close
  openings (the Tree: shorter or rarer stuns, or a recovery that is not an opening; the Vampire: wind-ups and
  recoveries on its claws; the Sister in 16.3: about 20 % uptime) — or the target accepts ~1 min for them.

This is the main finding for 18.4 and 16.3, and the reason both bands are tested separately.

### Bosses (solo)

| Boss | Today (first player) | Against the target |
|---|---|---|
| Smoldering Lord | 58–63 clean hits, **4.4–5.2 min** | furthest off: 1.7–1.8× the hits, 1.8–2.1× the time |
| Crypt Keeper | 70 hits with the diamond sword, 36–37 with the Scythe, **2.4–3.2 min** | 2× the hits at its first stage |
| Ash Colossus | 58 hits with the diamond sword, 34–35 with the Scourge, **2.9–3.2 min** | |
| Grieving Tree | 31–54 hits, **0.8–1.1 min** | wrong shape: too many hits and too short at once (the stun loop) |
| Vampire boss | 66 hits, **1.1 min** | wrong shape: a trade fight |
| Fallen Sister (placeholder) | 30 hits, 1.2 min | placeholder: its hits meet the band, its time does not (see above) |

The uniques at §6.1's numbers (17.3) took the later stages from 37–46 hits to 31–37 and the Lord from 72 to 58–63
(the table above is with them). With 2–3 players every fight gets shorter (HP ×1.5 / ×2 split two or three ways, and a fighter the boss is not aiming
at swings through cones and lines): the Colossus 3.2 → 1.5 → 1.2 min, the Lord 5.4 → 1.8 → 1.4 min.

### Boss hit sizes and the wind-up rule

Three moves break their class, all against the weakest player who meets them:

- **Vampire boss claw** 27 % of a level 10 player in iron (light, ≤ 20 %), with no wind-up at all (vanilla melee). It
  stays under ⅓, so the wind-up rule holds, by a hair.
- **Vampire boss leap** 43 % (heavy, ≤ 35 %).
- **Smoldering Lord greatsword swing** 32 % of the depths player (light, ≤ 20 %; 33 % before the Smoldering Plate got
  netherite's toughness in 17.3); its second swing winds up 10 ticks, so at 34 % it would also break the wind-up rule.

The wind-up rule holds everywhere today. The biggest hits: the Crypt Keeper's overhead 52 % of a level 10 player in
diamond (22-tick wind-up, a line: allowed), the Lord's slam 44 % of the depths player, the Vampire's nova 47 % of a
level 10 player in iron. The overhead and any heavy hit after it kill a fresh level 10 player in diamond: the
playtest's "a boss kills in about two hits", which §5.5 allows only for the biggest move plus a heavy one.

### Mobs

27 mobs miss their hits-to-kill band and 6 kill the player too fast (§7 lists them; with 17.3's uniques, 29 and 7
before). The furthest off:

- **Elites take 2–3× their hits:** the Grave Warden 29 at the Necropolis first (17 later), the Furnace Golem and the
  Smoldering Knight 17–19, the Hollow Sentinel 18 / 11, the Bark Hollow 17 at the Woods first (10 later, in its band);
  the Ruined Sanctum's Hollow 11 and the Enderman 12 for a level 1 player.
- **Fodder takes about twice its hits:** Restless Bones 6 / 4 (the bone pile included), the Ashen Zombie 6, the
  Withered Thrall 5 / 4, the Cinder Husk and the overworld zombie 4. The Crypt Moth now dies in 2 to the Scythe (3–4
  wanted).
- **Too deadly:** the creeper's blast at 2 blocks kills a level 1 player in iron outright (33 HP), the Vindicator in 2,
  the witch, the Enderman and the full moon Ghost in 3, the Furnace Golem at level 30 kills the depths player in 3.

### Weapons (§6.1)

Built in 17.3 at §6.1's starting numbers, and they pass: the Scythe 13.2 per second on one target and 23.1 on two
(+80 %), the Scourge 16 at the tip (+25 %), the Rapier 15.4 and 20 ramped (+56 %), against netherite's 12.8. The 0.4.0
uniques all missed (the Scythe 11 on one target, the Scourge 8.1, the Rapier 12). The Sanctum Longsword (10.4) and
Ashbearer's Mace (7.2) stay between iron and diamond as §6.1 wants.

Two things the per-second numbers do not show:

- **The Scourge's tip is PLAN 17.4.** Until it lands the Scourge hits one target plainly, 10 per second, below
  netherite; the check above counts the tip as designed.
- **The Rapier is capped by the hurt cooldown.** A mob takes a second hit within 10 ticks of the first only for the
  difference, and at speed 2.2 a full charge comes every 9.1 ticks, so on one target the Rapier lands at most 2 full
  hits per second: 14 per second, about 18 ramped (still +9 % / +42 % over netherite). 18.4 can raise its damage a
  little if that feels short.

## 5. Work list for 18.3 / 18.4 (and 16.3, 17.3)

1. **Lord, Keeper, Colossus HP** down to the overlap of both bands (§4, about ×0.35–0.6, with 17.3's weapons in).
   Lord first: furthest off.
2. **Tree and Vampire cadence:** fewer openings, then HP. The Tree's stun after every sweep is most of the fight.
3. **Uniques before the Citadel:** done in 17.3 (§2: soul + 4 of the zone's material + the part).
4. **Vampire claw and leap, Lord's greatsword swing** under their class limits (and the swing's 10-tick follow-up if
   it stays near ⅓).
5. **Mob HP:** elites to 6–10 hits, fodder to 2–3 (Realm 1 in 18.4; overworld, dungeon guards and the moon waves in
   18.3, where vanilla mobs are touched only through their level curve or not at all — 18.3 decides).
6. **Mob damage:** the creeper, Vindicator, witch, Enderman (overworld, 18.3), the Ghost (moon), the Furnace Golem.
7. **16.3:** the Fallen Sister's placeholder is a start: 30 clean hits and every move inside its class, but 1.2 min.
8. **17.3:** done — the §6.1 numbers as proposed, the Scythe's stamina back, the Rapier's ramp, the Crossbow's opener,
   the Smoldering Plate at netherite. The Scourge's tip waits for 17.4.

## 6. Running it

```bash
./gradlew balanceTargets
```

fails with every miss, grouped by target (also in `build/reports/tests/balanceTargets/`). The tables:
`./gradlew test --tests 'com.embervoid.balance.*'` writes `build/balance/fight-length.md`. The wind-up cross-check is
the GameTest `bossWindupsAreTheEntities` (`./gradlew runGametest`).

## 7. Tables (from `build/balance/fight-length.md`)

### Boss cadence: openings per beat (escape 10 ticks, re-engage 16, out of melee 20 % of picks)

| Boss | Phase | Beat | Lead / busy / recovery | Weight | Open (aimed at you / not) | Range, aim |
| --- | --- | --- | --- | --- | --- | --- |
| Fallen Sister (placeholder) | P1 (censer) | two censer arcs | 18 / 30 / 26 | 0.6 | 18 / 56 | melee, target |
| Fallen Sister (placeholder) | P1 (censer) | censer slam | 22 / 22 / 30 | 0.3 | 26 / 52 | melee, target |
| Fallen Sister (placeholder) | P1 (censer) | backstep | 10 / 10 / 20 | 0.1 | 0 / 0 | ranged, target |
| Fallen Sister (placeholder) | P2 (veil burnt, crying souls) | two censer arcs | 18 / 30 / 26 | 0.7 | 18 / 56 | melee, target |
| Fallen Sister (placeholder) | P2 (veil burnt, crying souls) | censer spin | 24 / 24 / 30 | 0.3 | 28 / 28 | melee, all |
| Vampire boss | P0 | claws, traded | 0 / 0 / 20 | 0.8 | 20 / 20 | melee, none |
| Vampire boss | P0 | leap | 34 / 34 / 0 | 0.2 | 0 / 0 | closing, target |
| Vampire boss | P1 (nova) | claws, traded | 0 / 0 / 20 | 5.2 | 20 / 20 | melee, none |
| Vampire boss | P1 (nova) | blood nova | 30 / 30 / 0 | 1 | 4 / 4 | melee, all |
| Vampire boss | P1 (nova) | leap / mist step | 34 / 34 / 0 | 1.3 | 0 / 0 | closing, target |
| Vampire boss | P2 (mist step) | claws, traded | 0 / 0 / 20 | 3.6 | 20 / 20 | melee, none |
| Vampire boss | P2 (mist step) | blood nova | 30 / 30 / 0 | 1 | 4 / 4 | melee, all |
| Vampire boss | P2 (mist step) | leap / mist step | 34 / 34 / 0 | 0.9 | 0 / 0 | closing, target |
| Ash Colossus | P1 | fist cone | 18 / 18 / 32 | 0.5 | 24 / 50 | melee, target |
| Ash Colossus | P1 | arm drag | 16 / 16 / 32 | 0.3 | 22 / 48 | melee, target |
| Ash Colossus | P1 | leap ring | 36 / 36 / 50 | 0.1 | 34 / 34 | closing, target |
| Ash Colossus | P1 | boulder | 30 / 30 / 50 | 0.1 | 0 / 0 | ranged, target |
| Ash Colossus | P2 (stone shed) | fist cone | 18 / 18 / 22 | 0.5 | 14 / 40 | melee, target |
| Ash Colossus | P2 (stone shed) | arm drag | 16 / 16 / 22 | 0.3 | 12 / 38 | melee, target |
| Ash Colossus | P2 (stone shed) | leap ring | 36 / 36 / 35 | 0.1 | 19 / 19 | closing, target |
| Ash Colossus | P2 (stone shed) | boulder | 30 / 30 / 35 | 0.1 | 0 / 0 | ranged, target |
| Grieving Tree | P1 (rooted) | branch sweep | 20 / 20 / 40 | 0.7 | 34 / 60 | melee, target |
| Grieving Tree | P1 (rooted) | root lines | 26 / 26 / 50 | 0.3 | 50 / 50 | melee, all |
| Grieving Tree | P1 (rooted) | stun (lowered lantern broken) | 0 / 0 / 100 | 0.7 | 84 / 84 | melee, all |
| Grieving Tree | P2 (walking) | branch sweep | 20 / 20 / 40 | 0.7 | 34 / 60 | melee, target |
| Grieving Tree | P2 (walking) | root cage | 24 / 24 / 45 | 0.1 | 43 / 69 | melee, target |
| Grieving Tree | P2 (walking) | root lines | 26 / 26 / 50 | 0.2 | 50 / 50 | melee, all |
| Grieving Tree | P2 (walking) | stun (lowered lantern broken) | 0 / 0 / 100 | 0.7 | 84 / 84 | melee, all |
| Crypt Keeper | P1 | halberd combo (2 swings + overhead) | 14 / 46 / 35 | 0.5 | 23 / 81 | melee, target |
| Crypt Keeper | P1 | sweep ring | 24 / 24 / 35 | 0.3 | 33 / 33 | melee, all |
| Crypt Keeper | P1 | lantern throw | 20 / 20 / 40 | 0.2 | 0 / 0 | ranged, target |
| Crypt Keeper | P2 (dark, braziers) | halberd combo (2 swings + overhead) | 14 / 46 / 25 | 0.5 | 13 / 71 | melee, target |
| Crypt Keeper | P2 (dark, braziers) | sweep ring | 24 / 24 / 25 | 0.3 | 23 / 23 | melee, all |
| Crypt Keeper | P2 (dark, braziers) | thrust | 18 / 18 / 30 | 0.2 | 14 / 14 | closing, target |
| Crypt Keeper | P3 (flooded) | halberd combo (2 swings + overhead) | 14 / 46 / 25 | 0.5 | 13 / 71 | melee, target |
| Crypt Keeper | P3 (flooded) | sweep ring | 24 / 24 / 25 | 0.3 | 23 / 23 | melee, all |
| Crypt Keeper | P3 (flooded) | thrust | 18 / 18 / 30 | 0.2 | 14 / 14 | closing, target |
| Smoldering Lord | P1 | greatsword combo (2 swings + slam) | 14 / 44 / 30 | 0.8 | 18 / 74 | melee, target |
| Smoldering Lord | P1 | lunge | 18 / 18 / 40 | 0.2 | 24 / 24 | closing, target |
| Smoldering Lord | P2 (armor shattered) | greatsword combo (2 swings + slam) | 14 / 44 / 22 | 0.8 | 10 / 66 | melee, target |
| Smoldering Lord | P2 (armor shattered) | lunge | 18 / 18 / 30 | 0.2 | 14 / 14 | closing, target |

| Boss | Phase | From | Pause | Loop ticks | Open in the loop | Downtime (each) |
| --- | --- | --- | --- | --- | --- | --- |
| Fallen Sister (placeholder) | P1 (censer) | 100 % | 0 | 52.2 | 36 % | ember puddle: waiting for it to cool: 40 per 175 |
| Fallen Sister (placeholder) | P2 (veil burnt, crying souls) | 50 % | 40 | 55.4 | 38 % | crying souls: 3.5 dives dodged, each soul killed with one swing: 147 per 400 |
| Vampire boss | P0 | 100 % | 0 | 22.8 | 70 % | - |
| Vampire boss | P1 (nova) | 60 % | 0 | 178.2 | 61 % | - |
| Vampire boss | P2 (mist step) | 30 % | 0 | 132.6 | 57 % | - |
| Ash Colossus | P1 | 100 % | 0 | 56.1 | 39 % | ash storm: roar, then behind a pillar: 150 per 400 |
| Ash Colossus | P2 (stone shed) | 50 % | 30 | 45.1 | 28 % | ash storm: roar, then behind a pillar: 150 per 400; falling cinders: a step out of the shadow: 5.3 per 50 |
| Grieving Tree | P1 (rooted) | 100 % | 0 | 132 | 72 % | Withered Thralls dropping (2): 123.2 per 500 |
| Grieving Tree | P2 (walking) | 50 % | 40 | 131.2 | 72 % | Withered Thralls dropping (2): 123.2 per 500 |
| Crypt Keeper | P1 | 100 % | 0 | 70.9 | 30 % | - |
| Crypt Keeper | P2 (dark, braziers) | 60 % | 30 | 60.5 | 26 % | Crypt Moths at a lit brazier (1.5): 55.4 per 300 |
| Crypt Keeper | P3 (flooded) | 30 % | 30 | 60.5 | 26 % | Crypt Moths at a lit brazier (1.5): 55.4 per 300; Restless Bones rising: 221.7 once |
| Smoldering Lord | P0 (on the wall) | 100 % | 270 | 0 | - | - |
| Smoldering Lord | P1 | 100 % | 30 | 70.8 | 27 % | - |
| Smoldering Lord | P2 (armor shattered) | 50 % | 30 | 62.4 | 17 % | ash storm: roar, then behind cover: 150 per 1500; fog: a quarter of it spent finding him: 100 per 1500 |

### Boss fights, solo (today's numbers)

| Boss | Stage | Player | Boss HP | Clean hit (P1) | Clean hits | Swinging (s) | Uptime | Fight (min) | Hits / time ok |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Fallen Sister (placeholder) | Ruined Sanctum | Lv 1 iron, iron sword | 169.4 | 5.8 | 30 | 18.4 | 25 % | 1.2 | yes / **no** |
| Vampire boss | blood new moon | Lv 10 iron, iron sword | 404.8 | 6.2 | 66 | 40.5 | 63 % | 1.1 | **no** / **no** |
| Ash Colossus | Ashen Fields 1st | Lv 10 diamond, diamond sword | 363.6 | 5.6 | 58 | 35.6 | 19 % | 3.2 | **no** / **no** |
| Ash Colossus | Ashen Fields 2nd (after Royal Woods), no melee unique yet | Lv 19 diamond, diamond sword | 363.6 | 6.1 | 54 | 32.2 | 19 % | 2.9 | **no** / **no** |
| Ash Colossus | Ashen Fields 2nd (after Sunken Necropolis) | Lv 22 diamond, Warden's Scourge | 363.6 | 9.7 | 35 | 33.6 | 18 % | 3.0 | yes / **no** |
| Ash Colossus | Ashen Fields 3rd (after Royal Woods + Sunken Necropolis) | Lv 27 diamond, Warden's Scourge | 363.6 | 10.1 | 34 | 32.0 | 18 % | 2.9 | yes / **no** |
| Grieving Tree | Royal Woods 1st | Lv 10 diamond, diamond sword | 374.0 | 1.8 | 54 | 33.0 | 53 % | 1.0 | **no** / **no** |
| Grieving Tree | Royal Woods 2nd (after Ashen Fields) | Lv 17 diamond, Ashen Scythe | 374.0 | 3.2 | 32 | 27.8 | 55 % | 0.8 | yes / **no** |
| Grieving Tree | Royal Woods 2nd (after Sunken Necropolis) | Lv 22 diamond, Warden's Scourge | 374.0 | 2.9 | 35 | 32.8 | 48 % | 1.1 | yes / **no** |
| Grieving Tree | Royal Woods 3rd (after Ashen Fields + Sunken Necropolis) | Lv 26 diamond, Ashen Scythe | 374.0 | 3.2 | 31 | 26.8 | 55 % | 0.8 | yes / **no** |
| Grieving Tree | Royal Woods 3rd (after Ashen Fields + Sunken Necropolis) | Lv 26 diamond, Warden's Scourge | 374.0 | 2.9 | 34 | 32.1 | 49 % | 1.1 | yes / **no** |
| Crypt Keeper | Sunken Necropolis 1st | Lv 10 diamond, diamond sword | 428.4 | 6.1 | 70 | 42.9 | 22 % | 3.2 | **no** / **no** |
| Crypt Keeper | Sunken Necropolis 2nd (after Ashen Fields) | Lv 17 diamond, Ashen Scythe | 428.4 | 11.7 | 37 | 32.3 | 22 % | 2.4 | **no** / yes |
| Crypt Keeper | Sunken Necropolis 2nd (after Royal Woods), no melee unique yet | Lv 19 diamond, diamond sword | 428.4 | 6.7 | 64 | 38.6 | 22 % | 2.9 | **no** / **no** |
| Crypt Keeper | Sunken Necropolis 3rd (after Ashen Fields + Royal Woods) | Lv 23 diamond, Ashen Scythe | 428.4 | 11.9 | 36 | 31.4 | 22 % | 2.4 | **no** / yes |
| Smoldering Lord | depths | Lv 30 Smoldering Plate, Ashen Scythe | 712.4 | 10.1 | 58 | 49.9 | 19 % | 4.4 | **no** / **no** |
| Smoldering Lord | depths | Lv 30 Smoldering Plate, Warden's Scourge | 712.4 | 9.2 | 63 | 59.8 | 19 % | 5.2 | **no** / **no** |

### Boss fights with 2–3 players (each like the stage's player; HP ×1.5 / ×2, one add per extra fighter)

| Boss | Stage | Players | Boss HP | Clean hits each | Uptime | Adds (s) | Fight (min) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Fallen Sister (placeholder) | Ruined Sanctum, iron sword | 1 | 169.4 | 29.4 | 25 % | 0 | 1.2 |
| Fallen Sister (placeholder) | Ruined Sanctum, iron sword | 2 | 254.1 | 22.1 | 41 % | 0 | 0.6 |
| Fallen Sister (placeholder) | Ruined Sanctum, iron sword | 3 | 338.8 | 19.6 | 46 % | 0 | 0.4 |
| Vampire boss | blood new moon, iron sword | 1 | 404.8 | 65.8 | 63 % | 0 | 1.1 |
| Vampire boss | blood new moon, iron sword | 2 | 404.8 | 32.9 | 63 % | 0 | 0.5 |
| Vampire boss | blood new moon, iron sword | 3 | 404.8 | 21.9 | 63 % | 0 | 0.4 |
| Ash Colossus | Ashen Fields 1st, diamond sword | 1 | 363.6 | 57.8 | 19 % | 0 | 3.2 |
| Ash Colossus | Ashen Fields 1st, diamond sword | 2 | 545.4 | 43.4 | 30 % | 1.2 | 1.5 |
| Ash Colossus | Ashen Fields 1st, diamond sword | 3 | 727.2 | 38.6 | 34 % | 1.6 | 1.2 |
| Ash Colossus | Ashen Fields 3rd (after Royal Woods + Sunken Necropolis), Warden's Scourge | 1 | 363.6 | 33.6 | 18 % | 0 | 2.9 |
| Ash Colossus | Ashen Fields 3rd (after Royal Woods + Sunken Necropolis), Warden's Scourge | 2 | 545.4 | 25.2 | 30 % | 1.4 | 1.3 |
| Ash Colossus | Ashen Fields 3rd (after Royal Woods + Sunken Necropolis), Warden's Scourge | 3 | 727.2 | 22.4 | 33 % | 1.9 | 1.1 |
| Grieving Tree | Royal Woods 1st, diamond sword | 1 | 374.0 | 53.5 | 53 % | 0 | 1.0 |
| Grieving Tree | Royal Woods 1st, diamond sword | 2 | 561.0 | 43.0 | 64 % | 1.5 | 0.7 |
| Grieving Tree | Royal Woods 1st, diamond sword | 3 | 748.0 | 39.1 | 67 % | 2.1 | 0.6 |
| Grieving Tree | Royal Woods 3rd (after Ashen Fields + Sunken Necropolis), Warden's Scourge | 1 | 374.0 | 33.6 | 49 % | 0 | 1.1 |
| Grieving Tree | Royal Woods 3rd (after Ashen Fields + Sunken Necropolis), Warden's Scourge | 2 | 561.0 | 27.0 | 61 % | 1.9 | 0.7 |
| Grieving Tree | Royal Woods 3rd (after Ashen Fields + Sunken Necropolis), Warden's Scourge | 3 | 748.0 | 24.5 | 64 % | 2.6 | 0.6 |
| Crypt Keeper | Sunken Necropolis 1st, diamond sword | 1 | 428.4 | 69.7 | 22 % | 0 | 3.2 |
| Crypt Keeper | Sunken Necropolis 1st, diamond sword | 2 | 642.6 | 52.3 | 42 % | 1.8 | 1.3 |
| Crypt Keeper | Sunken Necropolis 1st, diamond sword | 3 | 856.8 | 46.5 | 49 % | 2.5 | 1.0 |
| Crypt Keeper | Sunken Necropolis 3rd (after Ashen Fields + Royal Woods), Ashen Scythe | 1 | 428.4 | 35.9 | 22 % | 0 | 2.4 |
| Crypt Keeper | Sunken Necropolis 3rd (after Ashen Fields + Royal Woods), Ashen Scythe | 2 | 642.6 | 26.9 | 40 % | 1.7 | 1.0 |
| Crypt Keeper | Sunken Necropolis 3rd (after Ashen Fields + Royal Woods), Ashen Scythe | 3 | 856.8 | 23.9 | 46 % | 2.3 | 0.8 |
| Smoldering Lord | depths, Ashen Scythe | 1 | 712.4 | 57.7 | 19 % | 0 | 4.4 |
| Smoldering Lord | depths, Ashen Scythe | 2 | 1068.6 | 43.3 | 42 % | 1.3 | 1.5 |
| Smoldering Lord | depths, Ashen Scythe | 3 | 1424.8 | 38.5 | 47 % | 1.7 | 1.2 |
| Smoldering Lord | depths, Warden's Scourge | 1 | 712.4 | 63.0 | 19 % | 0 | 5.2 |
| Smoldering Lord | depths, Warden's Scourge | 2 | 1068.6 | 47.2 | 43 % | 1.4 | 1.7 |
| Smoldering Lord | depths, Warden's Scourge | 3 | 1424.8 | 42.0 | 49 % | 1.9 | 1.4 |

### What 1.5–2.5 min means in clean hits (at each fight's own uptime and swing)

| Boss | Stage | Weapon | Swing (s) | Uptime | Clean hits for 1.5 min | for 2.5 min | Boss HP for 1.5–2.5 min (× today) | Hits meeting both bands | Boss HP for those (× today) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Fallen Sister (placeholder) | Ruined Sanctum | iron sword | 0.6 | 25 % | 36.1 | 60.6 | ×1.23–2.06 | **none** | - |
| Vampire boss | blood new moon | iron sword | 0.6 | 63 % | 92.0 | 153.4 | ×1.40–2.33 | **none** | - |
| Ash Colossus | Ashen Fields 1st | diamond sword | 0.6 | 19 % | 27.1 | 45.5 | ×0.47–0.79 | 27.1–35 | ×0.47–0.61 |
| Ash Colossus | Ashen Fields 2nd (after Royal Woods), no melee unique yet | diamond sword | 0.6 | 19 % | 27.6 | 46.3 | ×0.52–0.87 | 27.6–35 | ×0.52–0.66 |
| Ash Colossus | Ashen Fields 2nd (after Sunken Necropolis) | Warden's Scourge | 1.0 | 18 % | 17.1 | 28.7 | ×0.49–0.82 | 20–28.7 | ×0.57–0.82 |
| Ash Colossus | Ashen Fields 3rd (after Royal Woods + Sunken Necropolis) | Warden's Scourge | 1.0 | 18 % | 17.3 | 29.0 | ×0.51–0.86 | 20–29.0 | ×0.60–0.86 |
| Grieving Tree | Royal Woods 1st | diamond sword | 0.6 | 53 % | 77.7 | 130.7 | ×1.45–2.44 | **none** | - |
| Grieving Tree | Royal Woods 2nd (after Ashen Fields) | Ashen Scythe | 0.9 | 55 % | 56.7 | 95.3 | ×1.80–3.03 | **none** | - |
| Grieving Tree | Royal Woods 2nd (after Sunken Necropolis) | Warden's Scourge | 1.0 | 48 % | 45.7 | 76.9 | ×1.34–2.25 | **none** | - |
| Grieving Tree | Royal Woods 3rd (after Ashen Fields + Sunken Necropolis) | Ashen Scythe | 0.9 | 55 % | 57.7 | 97.1 | ×1.88–3.16 | **none** | - |
| Grieving Tree | Royal Woods 3rd (after Ashen Fields + Sunken Necropolis) | Warden's Scourge | 1.0 | 49 % | 46.0 | 77.4 | ×1.37–2.31 | **none** | - |
| Crypt Keeper | Sunken Necropolis 1st | diamond sword | 0.6 | 22 % | 32.0 | 54.1 | ×0.46–0.78 | 32.0–35 | ×0.46–0.50 |
| Crypt Keeper | Sunken Necropolis 2nd (after Ashen Fields) | Ashen Scythe | 0.9 | 22 % | 22.1 | 37.4 | ×0.61–1.02 | 22.1–35 | ×0.61–0.96 |
| Crypt Keeper | Sunken Necropolis 2nd (after Royal Woods), no melee unique yet | diamond sword | 0.6 | 22 % | 32.6 | 55.0 | ×0.51–0.86 | 32.6–35 | ×0.51–0.55 |
| Crypt Keeper | Sunken Necropolis 3rd (after Ashen Fields + Royal Woods) | Ashen Scythe | 0.9 | 22 % | 22.3 | 37.7 | ×0.62–1.05 | 22.3–35 | ×0.62–0.98 |
| Smoldering Lord | depths | Ashen Scythe | 0.9 | 19 % | 17.2 | 31.2 | ×0.30–0.54 | 20–31.2 | ×0.35–0.54 |
| Smoldering Lord | depths | Warden's Scourge | 1.0 | 19 % | 15.7 | 28.4 | ×0.25–0.45 | 20–28.4 | ×0.32–0.45 |

### Boss moves as a share of the expected player's max health (the first, weakest player who fights them)

| Boss | Move | Class | Windup | Tell | Player | Damage | Share | Limit | Breaks |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Fallen Sister (placeholder) | censer swing | light | 12 | shape | Lv 1 iron, iron sword (20 HP) | 4.0 | 20 % | 20 % | - |
| Fallen Sister (placeholder) | censer slam | heavy | 22 | shape | Lv 1 iron, iron sword (20 HP) | 6.7 | 34 % | 35 % | - |
| Fallen Sister (placeholder) | crying soul dive (P2) | light | 20 | shape | Lv 1 iron, iron sword (20 HP) | 2.8 | 14 % | 20 % | - |
| Fallen Sister (placeholder) | censer spin (P2) | biggest | 24 | shape | Lv 1 iron, iron sword (20 HP) | 6.7 | 34 % | 67 % | - |
| Vampire boss | claw | light | 0 | none | Lv 10 iron, iron sword (24 HP) | 6.5 | 27 % | 20 % | **light above 20 %** |
| Vampire boss | leap | heavy | 20 | shape | Lv 10 iron, iron sword (24 HP) | 10.3 | 43 % | 35 % | **heavy above 35 %** |
| Vampire boss | blood nova (heals 25 %) | biggest | 30 | shape | Lv 10 iron, iron sword (24 HP) | 11.4 | 47 % | 67 % | - |
| Ash Colossus | fist cone | heavy | 18 | shape | Lv 10 diamond, diamond sword (24 HP) | 7.1 | 30 % | 35 % | - |
| Ash Colossus | leap ring | biggest | 22 | shape | Lv 10 diamond, diamond sword (24 HP) | 8.5 | 35 % | 67 % | - |
| Ash Colossus | boulder | heavy | 30 | shape | Lv 10 diamond, diamond sword (24 HP) | 5.8 | 24 % | 35 % | - |
| Ash Colossus | arm drag | light | 16 | shape | Lv 10 diamond, diamond sword (24 HP) | 4.7 | 20 % | 20 % | - |
| Ash Colossus | falling cinder (P2) | light | 30 | shape | Lv 10 diamond, diamond sword (24 HP) | 3.6 | 15 % | 20 % | - |
| Grieving Tree | branch sweep | biggest | 20 | shape | Lv 10 diamond, diamond sword (24 HP) | 8.0 | 33 % | 67 % | - |
| Grieving Tree | root line | heavy | 26 | shape | Lv 10 diamond, diamond sword (24 HP) | 6.5 | 27 % | 35 % | - |
| Grieving Tree | root cage (P2) | light | 24 | shape | Lv 10 diamond, diamond sword (24 HP) | 3.0 | 12 % | 20 % | - |
| Crypt Keeper | halberd swing | light | 10 | shape | Lv 10 diamond, diamond sword (24 HP) | 4.5 | 19 % | 20 % | - |
| Crypt Keeper | overhead (3rd hit) | biggest | 22 | shape | Lv 10 diamond, diamond sword (24 HP) | 12.6 | 52 % | 67 % | - |
| Crypt Keeper | sweep ring | heavy | 24 | shape | Lv 10 diamond, diamond sword (24 HP) | 7.3 | 30 % | 35 % | - |
| Crypt Keeper | thrust | heavy | 18 | shape | Lv 10 diamond, diamond sword (24 HP) | 7.3 | 30 % | 35 % | - |
| Crypt Keeper | lantern throw | light | 20 | shape | Lv 10 diamond, diamond sword (24 HP) | 4.5 | 19 % | 20 % | - |
| Smoldering Lord | greatsword swing | light | 10 | shape | Lv 30 Smoldering Plate, Ashen Scythe (30.5 HP) | 9.6 | 32 % | 20 % | **light above 20 %** |
| Smoldering Lord | overhead slam | biggest | 20 | shape | Lv 30 Smoldering Plate, Ashen Scythe (30.5 HP) | 13.5 | 44 % | 67 % | - |
| Smoldering Lord | lunge | heavy | 18 | shape | Lv 30 Smoldering Plate, Ashen Scythe (30.5 HP) | 9.6 | 32 % | 35 % | - |
| Smoldering Lord | landing ring | heavy | 30 | shape | Lv 30 Smoldering Plate, Ashen Scythe (30.5 HP) | 9.6 | 32 % | 35 % | - |
| Smoldering Lord | mortar (P0) | heavy | 40 | shape | Lv 30 Smoldering Plate, Ashen Scythe (30.5 HP) | 5.3 | 17 % | 35 % | - |
| Smoldering Lord | flame line (P0) | heavy | 30 | shape | Lv 30 Smoldering Plate, Ashen Scythe (30.5 HP) | 5.3 | 17 % | 35 % | - |

### Hazards (per tick, outside the classes) on the same player

| Boss | Hazard | Damage | Share |
| --- | --- | --- | --- |
| Fallen Sister (placeholder) | ember puddle, per 0.5 s | 0.8 | 4 % |
| Ash Colossus | ash storm, per s (magic) | 2 | 8 % |
| Crypt Keeper | fire pool, per 0.5 s | 1.3 | 6 % |
| Smoldering Lord | fire trail, per 0.5 s | 0.9 | 3 % |

### Mobs: hits both ways against the stage's player

| Stage | Mob | Lv | Band | Player | Mob HP | Hits to kill (want) | Its hit | Hits to kill the player (want) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| overworld | Zombie | 3 | fodder | Lv 1 iron, iron sword | 22.4 | **4** (2–3) | 2.0 | 11 (≥ 6) |
| overworld | Spider | 3 | fodder | Lv 1 iron, iron sword | 17.9 | 3 (2–3) | 1.3 | 16 (≥ 6) |
| overworld | Skeleton | 4 | skirmisher | Lv 1 iron, iron sword | 23.6 | 4 (3–4) | 3.0 | 7 (≥ 5) |
| overworld | Creeper | 6 | skirmisher | Lv 1 iron, iron sword | 26.0 | **5** (3–4) | 33 | **1** (≥ 5) |
| overworld | Witch | 6 | skirmisher | Lv 1 iron, iron sword | 33.8 | **6** (3–4) | 9.4 | **3** (≥ 5) |
| overworld | Enderman | 12 | elite | Lv 1 iron, iron sword | 66.4 | **12** (6–10) | 9.1 | **3** (≥ 4) |
| overworld | Vindicator | 4 | elite | Lv 1 iron, iron sword | 28.3 | **5** (6–10) | 14.5 | **2** (≥ 4) |
| overworld | Ash Ghoul | 5 | fodder | Lv 1 iron, iron sword | 14.9 | 3 (2–3) | 1.8 | 12 (≥ 6) |
| overworld | Ember Wisp | 11 | skirmisher | Lv 1 iron, iron sword | 12.8 | 3 (3–4) | 0.8 | 25 (≥ 5) |
| Ruined Sanctum | Hollow | 15 | elite | Lv 1 iron, iron sword | 55.2 | **11** (6–10) | 6.5 | 4 (≥ 4) |
| Ruined Sanctum | Skeleton | 4 | skirmisher | Lv 1 iron, iron sword | 23.6 | 4 (3–4) | 3.0 | 7 (≥ 5) |
| full moon | Ashen Zombie | 9 | fodder | Lv 5 iron, iron sword | 35.5 | **6** (2–3) | 3.8 | 6 (≥ 6) |
| full moon | Ghost | 9 | elite | Lv 5 iron, iron sword | 71.0 | **12** (6–10) | 9.5 | **3** (≥ 4) |
| Ashen Fields 1st | Cinder Husk | 15 | fodder | Lv 10 diamond, diamond sword | 29.4 | **4** (2–3) | 1.7 | 15 (≥ 6) |
| Ashen Fields 3rd (after Royal Woods + Sunken Necropolis) | Cinder Husk | 18 | fodder | Lv 27 diamond, Warden's Scourge | 32.3 | 3 (2–3) | 1.9 | 17 (≥ 6) |
| Ashen Fields 1st | Ash Stalker | 15 | skirmisher | Lv 10 diamond, diamond sword | 25.8 | 4 (3–4) | 2.4 | 10 (≥ 5) |
| Ashen Fields 3rd (after Royal Woods + Sunken Necropolis) | Ash Stalker | 18 | skirmisher | Lv 27 diamond, Warden's Scourge | 28.3 | 3 (3–4) | 2.7 | 12 (≥ 5) |
| Ashen Fields 1st | Ashen Carrion | 15 | skirmisher | Lv 10 diamond, diamond sword | 33.1 | **5** (3–4) | 2.4 | 10 (≥ 5) |
| Ashen Fields 3rd (after Royal Woods + Sunken Necropolis) | Ashen Carrion | 18 | skirmisher | Lv 27 diamond, Warden's Scourge | 36.4 | 4 (3–4) | 2.7 | 12 (≥ 5) |
| Royal Woods 1st | Withered Thrall | 18 | fodder | Lv 10 diamond, diamond sword | 36.4 | **5** (2–3) | 2.3 | 11 (≥ 6) |
| Royal Woods 3rd (after Ashen Fields + Sunken Necropolis) | Withered Thrall | 21 | fodder | Lv 26 diamond, Ashen Scythe | 39.6 | **4** (2–3) | 2.5 | 12 (≥ 6) |
| Royal Woods 1st | Lantern Hag | 18 | skirmisher | Lv 10 diamond, diamond sword | 32.3 | **5** (3–4) | 2.7 | 9 (≥ 5) |
| Royal Woods 3rd (after Ashen Fields + Sunken Necropolis) | Lantern Hag | 21 | skirmisher | Lv 26 diamond, Ashen Scythe | 35.2 | 3 (3–4) | 3.0 | 11 (≥ 5) |
| Royal Woods 1st | Bark Hollow | 18 | elite | Lv 10 diamond, diamond sword | 101.0 | **17** (6–10) | 5.8 | 5 (≥ 4) |
| Royal Woods 3rd (after Ashen Fields + Sunken Necropolis) | Bark Hollow | 21 | elite | Lv 26 diamond, Ashen Scythe | 110.0 | 10 (6–10) | 6.5 | 5 (≥ 4) |
| Sunken Necropolis 1st | Restless Bones | 21 | fodder | Lv 10 diamond, diamond sword | 41.2 | **6** (2–3) | 3.0 | 9 (≥ 6) |
| Sunken Necropolis 3rd (after Ashen Fields + Royal Woods) | Restless Bones | 25 | fodder | Lv 23 diamond, Ashen Scythe | 45.0 | **4** (2–3) | 3.4 | 9 (≥ 6) |
| Sunken Necropolis 1st | Crypt Moth | 21 | skirmisher | Lv 10 diamond, diamond sword | 22.0 | 3 (3–4) | 1.2 | 20 (≥ 5) |
| Sunken Necropolis 3rd (after Ashen Fields + Royal Woods) | Crypt Moth | 25 | skirmisher | Lv 23 diamond, Ashen Scythe | 24.4 | **2** (3–4) | 1.4 | 21 (≥ 5) |
| Sunken Necropolis 1st | Hollow Sentinel | 21 | elite | Lv 10 diamond, diamond sword | 99.0 | **18** (6–10) | 6.5 | 4 (≥ 4) |
| Sunken Necropolis 3rd (after Ashen Fields + Royal Woods) | Hollow Sentinel | 25 | elite | Lv 23 diamond, Ashen Scythe | 109.8 | **11** (6–10) | 7.5 | 4 (≥ 4) |
| Sunken Necropolis 1st | Grave Warden | 21 | elite | Lv 10 diamond, diamond sword | 176.0 | **29** (6–10) | 8.0 | 4 (≥ 4) |
| Sunken Necropolis 3rd (after Ashen Fields + Royal Woods) | Grave Warden | 25 | elite | Lv 23 diamond, Ashen Scythe | 195.2 | **17** (6–10) | 9.2 | 4 (≥ 4) |
| depths | Cinder Hound | 25 | fodder | Lv 30 Smoldering Plate, Ashen Scythe | 34.2 | 3 (2–3) | 2.2 | 14 (≥ 6) |
| depths | Cinder Hound | 30 | fodder | Lv 30 Smoldering Plate, Ashen Scythe | 38.4 | 3 (2–3) | 2.5 | 13 (≥ 6) |
| depths | Sparking Marksman | 25 | skirmisher | Lv 30 Smoldering Plate, Ashen Scythe | 58.6 | **5** (3–4) | 5.4 | 6 (≥ 5) |
| depths | Sparking Marksman | 30 | skirmisher | Lv 30 Smoldering Plate, Ashen Scythe | 65.8 | **6** (3–4) | 6.3 | 5 (≥ 5) |
| depths | Furnace Golem | 25 | elite | Lv 30 Smoldering Plate, Ashen Scythe | 170.8 | **17** (6–10) | 9.8 | 4 (≥ 4) |
| depths | Furnace Golem | 30 | elite | Lv 30 Smoldering Plate, Ashen Scythe | 191.8 | **19** (6–10) | 11.5 | **3** (≥ 4) |
| depths | Smoldering Knight | 25 | elite | Lv 30 Smoldering Plate, Ashen Scythe | 183.0 | **17** (6–10) | 8.2 | 4 (≥ 4) |
| depths | Smoldering Knight | 30 | elite | Lv 30 Smoldering Plate, Ashen Scythe | 205.5 | **19** (6–10) | 9.6 | 4 (≥ 4) |

### Weapons per second (attribute damage × speed, no stats; DESIGN §6.1, netherite 12.8)

| Weapon | Damage | Speed | Per second | Own use | Per second in own use | vs netherite | One target |
| --- | --- | --- | --- | --- | --- | --- | --- |
| iron sword | 6 | 1.6 | 9.6 | ×1 | 9.6 | -25 % | 9.6 |
| diamond sword | 7 | 1.6 | 11.2 | ×1 | 11.2 | -13 % | 11.2 |
| netherite sword | 8 | 1.6 | 12.8 | ×1 | 12.8 | +0 % | 12.8 |
| Ashen Scythe | 12 | 1.1 | 13.2 | ×1.75 | 23.1 | +80 % | 13.2 |
| Warden's Scourge | 10 | 1 | 10 | ×1.6 | 16 | +25 % | 16 |
| Royal Ash Rapier | 7 | 2.2 | 15.4 | ×1.3 | 20.0 | +56 % | 20.0 |
| Sanctum Longsword | 6.5 | 1.6 | 10.4 | ×1 | 10.4 | -19 % | 10.4 |
| Ashbearer's Mace | 8 | 0.9 | 7.2 | ×1 | 7.2 | -44 % | 7.2 |

The Scourge's ×1.6 is its tip crack, built in PLAN 17.4; until then it hits one target at ×1.

### Misses of the 0.5.0 targets (today's numbers; `./gradlew balanceTargets`)

**Boss fight length, solo (1.5–2.5 min)**: 15 misses.

- Fallen Sister (placeholder) (Ruined Sanctum, Lv 1 iron, iron sword): 1.2 min, want 1.5–2.5
- Vampire boss (blood new moon, Lv 10 iron, iron sword): 1.1 min, want 1.5–2.5
- Ash Colossus (Ashen Fields 1st, Lv 10 diamond, diamond sword): 3.2 min, want 1.5–2.5
- Ash Colossus (Ashen Fields 2nd (after Royal Woods), no melee unique yet, Lv 19 diamond, diamond sword): 2.9 min, want 1.5–2.5
- Ash Colossus (Ashen Fields 2nd (after Sunken Necropolis), Lv 22 diamond, Warden's Scourge): 3.0 min, want 1.5–2.5
- Ash Colossus (Ashen Fields 3rd (after Royal Woods + Sunken Necropolis), Lv 27 diamond, Warden's Scourge): 2.9 min, want 1.5–2.5
- Grieving Tree (Royal Woods 1st, Lv 10 diamond, diamond sword): 1.0 min, want 1.5–2.5
- Grieving Tree (Royal Woods 2nd (after Ashen Fields), Lv 17 diamond, Ashen Scythe): 0.8 min, want 1.5–2.5
- Grieving Tree (Royal Woods 2nd (after Sunken Necropolis), Lv 22 diamond, Warden's Scourge): 1.1 min, want 1.5–2.5
- Grieving Tree (Royal Woods 3rd (after Ashen Fields + Sunken Necropolis), Lv 26 diamond, Ashen Scythe): 0.8 min, want 1.5–2.5
- Grieving Tree (Royal Woods 3rd (after Ashen Fields + Sunken Necropolis), Lv 26 diamond, Warden's Scourge): 1.1 min, want 1.5–2.5
- Crypt Keeper (Sunken Necropolis 1st, Lv 10 diamond, diamond sword): 3.2 min, want 1.5–2.5
- Crypt Keeper (Sunken Necropolis 2nd (after Royal Woods), no melee unique yet, Lv 19 diamond, diamond sword): 2.9 min, want 1.5–2.5
- Smoldering Lord (depths, Lv 30 Smoldering Plate, Ashen Scythe): 4.4 min, want 1.5–2.5
- Smoldering Lord (depths, Lv 30 Smoldering Plate, Warden's Scourge): 5.2 min, want 1.5–2.5

**Boss clean hits, solo (20–35)**: 10 misses.

- Vampire boss (blood new moon, Lv 10 iron, iron sword): 66 clean hits, want 20–35
- Ash Colossus (Ashen Fields 1st, Lv 10 diamond, diamond sword): 58 clean hits, want 20–35
- Ash Colossus (Ashen Fields 2nd (after Royal Woods), no melee unique yet, Lv 19 diamond, diamond sword): 54 clean hits, want 20–35
- Grieving Tree (Royal Woods 1st, Lv 10 diamond, diamond sword): 54 clean hits, want 20–35
- Crypt Keeper (Sunken Necropolis 1st, Lv 10 diamond, diamond sword): 70 clean hits, want 20–35
- Crypt Keeper (Sunken Necropolis 2nd (after Ashen Fields), Lv 17 diamond, Ashen Scythe): 37 clean hits, want 20–35
- Crypt Keeper (Sunken Necropolis 2nd (after Royal Woods), no melee unique yet, Lv 19 diamond, diamond sword): 64 clean hits, want 20–35
- Crypt Keeper (Sunken Necropolis 3rd (after Ashen Fields + Royal Woods), Lv 23 diamond, Ashen Scythe): 36 clean hits, want 20–35
- Smoldering Lord (depths, Lv 30 Smoldering Plate, Ashen Scythe): 58 clean hits, want 20–35
- Smoldering Lord (depths, Lv 30 Smoldering Plate, Warden's Scourge): 63 clean hits, want 20–35

**Mob hits to kill (fodder 2–3, skirmishers 3–4, elites 6–10)**: 27 misses.

- Zombie Lv 3 (overworld, Lv 1 iron, iron sword): dies in 4, want 2–3
- Creeper Lv 6 (overworld, Lv 1 iron, iron sword): dies in 5, want 3–4
- Witch Lv 6 (overworld, Lv 1 iron, iron sword): dies in 6, want 3–4
- Enderman Lv 12 (overworld, Lv 1 iron, iron sword): dies in 12, want 6–10
- Vindicator Lv 4 (overworld, Lv 1 iron, iron sword): dies in 5, want 6–10
- Hollow Lv 15 (Ruined Sanctum, Lv 1 iron, iron sword): dies in 11, want 6–10
- Ashen Zombie Lv 9 (full moon, Lv 5 iron, iron sword): dies in 6, want 2–3
- Ghost Lv 9 (full moon, Lv 5 iron, iron sword): dies in 12, want 6–10
- Cinder Husk Lv 15 (Ashen Fields 1st, Lv 10 diamond, diamond sword): dies in 4, want 2–3
- Ashen Carrion Lv 15 (Ashen Fields 1st, Lv 10 diamond, diamond sword): dies in 5, want 3–4
- Withered Thrall Lv 18 (Royal Woods 1st, Lv 10 diamond, diamond sword): dies in 5, want 2–3
- Withered Thrall Lv 21 (Royal Woods 3rd (after Ashen Fields + Sunken Necropolis), Lv 26 diamond, Ashen Scythe): dies in 4, want 2–3
- Lantern Hag Lv 18 (Royal Woods 1st, Lv 10 diamond, diamond sword): dies in 5, want 3–4
- Bark Hollow Lv 18 (Royal Woods 1st, Lv 10 diamond, diamond sword): dies in 17, want 6–10
- Restless Bones Lv 21 (Sunken Necropolis 1st, Lv 10 diamond, diamond sword): dies in 6, want 2–3
- Restless Bones Lv 25 (Sunken Necropolis 3rd (after Ashen Fields + Royal Woods), Lv 23 diamond, Ashen Scythe): dies in 4, want 2–3
- Crypt Moth Lv 25 (Sunken Necropolis 3rd (after Ashen Fields + Royal Woods), Lv 23 diamond, Ashen Scythe): dies in 2, want 3–4
- Hollow Sentinel Lv 21 (Sunken Necropolis 1st, Lv 10 diamond, diamond sword): dies in 18, want 6–10
- Hollow Sentinel Lv 25 (Sunken Necropolis 3rd (after Ashen Fields + Royal Woods), Lv 23 diamond, Ashen Scythe): dies in 11, want 6–10
- Grave Warden Lv 21 (Sunken Necropolis 1st, Lv 10 diamond, diamond sword): dies in 29, want 6–10
- Grave Warden Lv 25 (Sunken Necropolis 3rd (after Ashen Fields + Royal Woods), Lv 23 diamond, Ashen Scythe): dies in 17, want 6–10
- Sparking Marksman Lv 25 (depths, Lv 30 Smoldering Plate, Ashen Scythe): dies in 5, want 3–4
- Sparking Marksman Lv 30 (depths, Lv 30 Smoldering Plate, Ashen Scythe): dies in 6, want 3–4
- Furnace Golem Lv 25 (depths, Lv 30 Smoldering Plate, Ashen Scythe): dies in 17, want 6–10
- Furnace Golem Lv 30 (depths, Lv 30 Smoldering Plate, Ashen Scythe): dies in 19, want 6–10
- Smoldering Knight Lv 25 (depths, Lv 30 Smoldering Plate, Ashen Scythe): dies in 17, want 6–10
- Smoldering Knight Lv 30 (depths, Lv 30 Smoldering Plate, Ashen Scythe): dies in 19, want 6–10

**Mob hits to kill the player (fodder ≥ 6, skirmishers ≥ 5, elites ≥ 4)**: 6 misses.

- Creeper Lv 6 (overworld, Lv 1 iron, iron sword): kills in 1 (33 of 20), want ≥ 5
- Witch Lv 6 (overworld, Lv 1 iron, iron sword): kills in 3 (9.4 of 20), want ≥ 5
- Enderman Lv 12 (overworld, Lv 1 iron, iron sword): kills in 3 (9.1 of 20), want ≥ 4
- Vindicator Lv 4 (overworld, Lv 1 iron, iron sword): kills in 2 (14.5 of 20), want ≥ 4
- Ghost Lv 9 (full moon, Lv 5 iron, iron sword): kills in 3 (9.5 of 22), want ≥ 4
- Furnace Golem Lv 30 (depths, Lv 30 Smoldering Plate, Ashen Scythe): kills in 3 (11.5 of 30.5), want ≥ 4

**Boss hit sizes (light ≤ 20 %, heavy ≤ 35 %, biggest ≤ ⅔)**: 3 misses.

- Vampire boss claw: 27 % of Lv 10 iron, iron sword (6.5 HP), light wants ≤ 20 %
- Vampire boss leap: 43 % of Lv 10 iron, iron sword (10.3 HP), heavy wants ≤ 35 %
- Smoldering Lord greatsword swing: 32 % of Lv 30 Smoldering Plate, Ashen Scythe (9.6 HP), light wants ≤ 20 %

**Wind-up rule (≥ ⅓: 12 ticks + tell; ≥ ½: 20 ticks + shape or sound)**: none.

**Uniques above netherite (≥ +20 % in own use, ≥ netherite on one target)**: none.

