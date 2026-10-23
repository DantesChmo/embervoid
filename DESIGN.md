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
**Proposed — built as recommended** — an Open question whose recommended answer is in the code; it stands until
the user decides otherwise. **Open** — needs a decision before the milestone that depends on it.

---

## 1. Player journey

```
 Overworld (harder)        Ruined Sanctum (dungeon)     Ember Sanctum (hub)       Realm N
 ──────────────────        ────────────────────────     ───────────────────       ───────
 survive, mine, craft ──►  common, like a village   ──► Maiden: level up     ──►  obelisk N teleports you in
 stronger vanilla mobs     fight through the halls      obelisk ring              3 zones, any order, 1 boss each
 + new hostile mobs        Fallen Sister → Crying Soul  Ember Shrine              ─► the final zone opens
 earn vanilla XP           kindle the Cleft Stele ──────►                         final zone + realm boss
          ▲                                             ▲                         ─► obelisk N+1 lights up
          └─────── hub exit → the lit stele ◄───────────┴──── leaving a realm ◄───┘
```

1. **Overworld.** The player starts in a normal Minecraft world and plays it as usual. The world is harder
   than vanilla (the stick, §3): vanilla mobs hit harder even on Easy, and new hostile mobs appear. Vanilla XP
   is now worth more than enchanting — it is what the player levels up with.
2. **The dungeon.** **Ruined Sanctums** generate about as often as villages. Their outer halls are a real dungeon;
   where the core used to be, the **Fallen Sister** guards a cold **Cleft Stele**. She drops the **Crying Soul**.
3. **The way in.** The Crying Soul kindles a Cleft Stele (the one in the dungeon, or one the player builds); a lit
   stele leads to the hub. One stele burns per world at a time (§2).
4. **The hub.** The **Ember Sanctum** lies in its own dimension: the **Ashen Maiden** (leveling NPC), a ring of
   **Void Obelisks** (one per realm) and an Ember Shrine. The first obelisk is lit; the others are dark until the
   previous realm is completed.
5. **Realms.** Each obelisk leads to its own **realm**, a separate dimension with a full visual identity.
   A realm has 4 zones (biomes). The first 3 can be done in any order; each ends in a zone boss. Beating all
   three opens the final zone and the realm boss.
6. **Reward (the carrot, §6).** Each realm drops better gear than the previous one and better than what the
   player could get before it. XP from realm mobs feeds leveling.
7. **Loop.** Back to the hub, level up, gear up, next realm; out through the lit stele to the overworld.

The first level-up comes only after the Fallen Sister: she is fought by a level 1 player in vanilla gear, the
gatekeeper of the mod (like the Asylum Demon).

There is still no storyline and no quest chain. Progression is structural: places, gates, bosses.
Lore lives in item descriptions and the environment.

---

## 2. Ember Sanctum (hub) and the Ruined Sanctum (dungeon)

Decided 2026-10-23 (the 0.4.1 playtest, PLAN 15.0): the player should not depend on finding one generated Sanctum.
The overworld structure becomes a dungeon, the Maiden and the obelisks move to a hub dimension. Built in 0.5.0
(PLAN milestones 15–16).

### The hub: Ember Sanctum

| Topic | Design | Status |
|---|---|---|
| Place | Dimension `embervoid:hub`: one shared hub for all players, an island in a void under eternal dusk (fixed time, no weather, no natural spawns, bounded). The hub structure is placed once at a fixed spot when the dimension first loads | Decided |
| Contents | The old Sanctum core: the Ashen Maiden, the ring of Void Obelisks, an Ember Shrine (rest, flask refill, Flask Shards; it does not set the respawn point); the hub's own Cleft Stele, always lit, is the exit | Decided |
| Getting in | Through a lit Cleft Stele (below). Leaving a realm, by the Return Obelisk or after the realm boss, always lands in the hub | Decided |
| Exit | The hub's stele leads to the world's lit stele. With none lit (broken or never kindled): to the spot the player last left the overworld from; failing that, their bed or the world spawn | Decided |
| Death | No hostile mobs; a fall into the void kills. Respawn as in the overworld (bed or world spawn) | Decided |
| Protection | The whole hub island: no breaking, no building (like arenas). The Maiden is invulnerable and never despawns | Decided |
| Progress | Per player, as before: obelisk states and unlocks come from `PlayerProgress`. **Attunement is dropped**; there is one hub, so "any Sanctum works" no longer applies | Decided |

### The Cleft Stele

| Topic | Design | Status |
|---|---|---|
| Shape | A wide flat slab the size of a nether portal, **5 wide and 6 tall**, split top to bottom: two columns of Cleft Stone on each side, the middle column is the crack (the passage). Wide and flat so it reads as a stele, not a pillar. Detected like a nether portal frame, on either axis | Decided |
| Blocks | **Cleft Stone** (24 per stele): crafted, 8 stone bricks around 1 coal or charcoal → 8. Cheap on purpose: the soul is the gate, not the stone. A cold stele already stands in every dungeon arena (protected, not minable) | Decided |
| Kindling | Right-click a valid frame with the **Crying Soul**: the soul is used up, the stone turns kindled (glowing cracks), mist and particles fill the crack, a sound plays. No crafting and no moon-night ingredients | Decided |
| Use | Right-click on a lit stele (or walk into the crack) → the hub | Decided |
| **One per world** | The world has at most one lit stele (besides the hub's own). Kindling another puts the old one out. On a server one player's new stele puts it out for everyone; accepted (the user, 2026-10-23) | Decided |
| Breaking | Breaking any stone of a lit stele puts it out; the hub exit then falls back as above | Decided |
| Where | Overworld, Nether and End alike; not in realms or the hub | Decided |

### The dungeon: Ruined Sanctum

| Topic | Design | Status |
|---|---|---|
| Spawning | Overworld structure, any land biome, about one per 300–400 blocks (`spacing` ~22 / `separation` ~8 chunks; was 48 / 16). Structure id stays `embervoid:ember_sanctum`, so `/locate` and old Sanctum Maps keep working. Tunable through the `structure_set` datapack file | Decided |
| Variants | 2–3 start pools sharing the halls, so it does not repeat; one boss for all | Decided |
| Layout | **Outer halls** as before: surface ruin + descent (a closed shaft cased in rock), guards (Hollows, skeleton archers), traps, loot (Flask Shards, the Sanctum weapons). An **Ember Shrine at the boss door** (rest, flask refill, Flask Shards; respawn stays at the bed). Behind a fog wall, the **boss arena** in place of the old core, with a cold Cleft Stele at its far end | Decided |
| Finding it | Exploration, plus the rare **Sanctum Map** in vanilla chest loot (villages, ruined portals, shipwrecks), now pointing at the nearest dungeon | Decided |
| Clearing | Each dungeon's boss is its own: beaten once per world (by whoever is in the fight), then the dungeon is cleared and the boss does not return. Every credited player gets a Crying Soul. Frequent dungeons mean a new soul is a short trip away | Decided |
| Names | The dungeon is the **Ruined Sanctum** (Разорённое Святилище), the hub the **Ember Sanctum** (Святилище Углей) | Decided |

### Fallen Sister (dungeon boss)

The Maiden's twin, burnt out trying to keep the flame alive. Her tears are what is left of her: the Crying Soul.
Concept decided (the user, 2026-10-23); moves and numbers are the first draft for PLAN 16.3.

- **Look:** tall (about 3 blocks), thin, a ragged ash-grey robe and veil, a censer on a long chain; embers drip from
  under the veil. Secondary motion on the robe, veil and chain; death, hit-reaction and phase-change animations
  from the start (PLAN milestone 19 sets the bar).
- **Fight:** a level 1 player in iron or diamond, solo, about 1.5–2 minutes with dodging (about 20–30 clean hits
  with an iron sword). No hit takes two thirds of the player's health without a clear wind-up; nothing one-shots a
  full-health player in iron on Normal.
- **Phase 1:** censer swings in wide arcs (wind-up: the chain winds around her arm), a slam that leaves a puddle of
  embers for a few seconds, a short backstep away from a player behind her.
- **Phase 2** (below half health): the veil burns away; 3–4 **crying souls** circle her and dive at the player one
  by one, each with a shriek and a glowing trail before the dive (dodgeable, killable with one hit); the censer
  spins around her (ring telegraph).
- **Arena:** the old core room, dim, with the cold stele behind her; braziers that light up as she loses health.
- **Drops:** the Crying Soul for every credited player; Sanctum-tier loot, a Flask Shard on the first clear; XP like a
  strong overworld mob pack. Level band and HP through the balance model (PLAN 16.3 / 18.3).
- **Music:** the shared zone-boss theme until she gets her own.

### Worlds from 0.4.x

Nothing generated is destroyed. Sanctum cores already in the world keep their Maiden (she still levels players)
and obelisks (they still lead to realms); leaving a realm lands in the hub, and the hub exit, with no lit stele,
leads back to the spot the player last left the overworld from, which is that old core. Attunement flags and old
return points are ignored.

### Ashen Maiden (leveling NPC)
- Talk → **level-up screen**: all stats, their current derived values (HP, stamina, damage bonus…), a preview
  of what the next point gives, the cost in XP points, and how much XP the player has.
- Points are bought **one at a time** and applied immediately.
- Stands in the hub (and in old worlds' Sanctum cores).
- Later (not 0.x): respec item, merchant role ([BACKLOG.md](BACKLOG.md)).

### Void Obelisks
- One per realm, placed in a ring in the hub. States: **dark** (locked, tooltip hint "the flame has not reached it"),
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
| **Ember Wisp** | Caves below y=0, under cover only | Small flyer, sets blocks and the player on fire, flees light | Decided |

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
- Spawns below y=0 at block light 0 and only under cover (no open sky), weight 15, groups of 1–2, not in the
  Deep Dark.
- XP 6. Loot: 0–1 glowstone dust, 5% blaze powder on a player kill.

### 3.3 Moon nights (Decided, 0.3.0)
Replaces the old "Ember Moon" sketch (rare night, +5 levels, red sky). Two vanilla moon phases become events; the
other six stay vanilla. Decided by the user on 2026-10-22; numbers marked *start* are starting values, all in config.

**When:** every third lunar cycle (8 days), so each event comes once per 24 days; the full moon and the blood new
moon of that cycle are 4 days apart. The first full moon of a world (day 0) does not count; counting starts after
it. Neither night can be slept through. If the night's goal is not met within **30 minutes** real time, it ends
anyway.

**Full moon** (phase 0, no visual change):
- Vanilla hostiles spawn **+3 levels** (*start*), follow range ×1.5, and do not lose a player who stays in range.
- **Curse of the Night:** each death from a direct kill by a hostile mob adds one stack (deaths in realms and to
  bosses count; falling, drowning, suffocation and being knocked into lava or off a cliff do not). Stacks reset
  after the full moon.
- On the full moon one **Ashen Zombie** per stack (at most 10) spawns near the cursed player; its level is the
  spot's level + 5 (*start*). A player with no curse gets **3 Ghosts** instead (was 5): twice an Ashen Zombie's HP and
  damage, one touch every 2 s, they fly, pass through walls, and 15 % of attacks miss them. Dying at least once is the
  cheaper option; a Ghost wave ends when its player dies, so a lost fight costs one death (**Proposed — built as recommended**, R40.1 balance pass, `docs/balance-0.4.0.md`).
- The night ends when all these zombies and ghosts are dead. A player outside the overworld that night: the
  zombies wait for them to come back.

**Blood new moon** (phase 4): blood-red sky.
- One or more **Vampires** spawn near the player, friendly by default. Hitting one turns it into the **Vampire
  boss** (visual transformation); the other Vampires vanish. If none is provoked within 5 minutes, the one nearest
  to the player turns.
- One shared boss. Its level: the highest level among the players taking part, at least 15, capped at the top of
  the band of the highest realm they have unlocked (*start*). When it forms, every player is teleported into a
  separate Vampire arena dimension and returned after the kill. A player killed there leaves all their loot there
  and cannot get back in; if everyone dies, the night ends.
- All vanilla hostiles get **vampirism**: they heal 25 % (*start*) of the damage they deal to a player.
- The night ends when the Vampire boss is beaten.

**§10.12** (rewards, player kills, the four conflicts with built systems): proposed answers built as recommended,
see §10.12. Cost: L (Ashen Zombie, Ghost, Vampire and its boss form, the curse, world moon-night state and sky
sync, night hold, sleep block, vampirism, a field boss without a fog-wall arena).

---

## 4. Realms

### 4.1 Structure of a realm

```
                 ┌───────────── realm_N (one dimension) ──────────────┐
                 │                 Zone A                             │
                 │        ╲                        ╱                  │
                 │  Zone C ── Landing (crossroads plaza:    ▓▓▓▓▓▓▓   │
                 │        ╱   return obelisk, shrine,  ┃   ▓ Final ▓  │
                 │            Citadel Gate) ───────────┃── ▓ zone  ▓  │
                 │                 Zone B         fog  ┃   ▓▓▓▓▓▓▓    │
                 │                                wall                │
                 │  ~1300 block radius; edge = abyss / impassable     │
                 └────────────────────────────────────────────────────┘
   Three open zones and the final zone are four angle sectors around the landing. The final zone's sector is
   walled off along both of its borders; its one gate stands at the plaza and opens after the A, B, C bosses.
```

Decided 2026-10-23 (PLAN 21.0, the user's pick of the fourth sector over "across the abyss" and "in the centre"):
the final zone is no longer a separate dimension. Built in 0.5.0 (PLAN milestone 21).

| Topic | Design | Status |
|---|---|---|
| Count | 4 realms planned, built one at a time. Realm 1 first | Proposed |
| Zones | 4 per realm: 3 open zones + 1 final zone, all in one dimension. Each zone is one or more biomes with its own style, mobs and structures | Decided |
| Order | The 3 open zones can be done in any order (like the archstones in Demon's Souls). Their level bands differ, so the player picks easier first | Proposed |
| Unlock | Realm N+1 unlocks when the realm N boss dies. Final zone unlocks when the 3 zone bosses of that realm die | Decided |
| Generation | Random but structured: terrain, caves, decoration and minor structures are seeded; the **layout** (where each zone is, where its boss arena is) is fixed by design | Decided |
| Zone layout | Custom `BiomeSource`: zone by angle sector around the landing. **Four sectors:** the final zone 60°, each open zone 100°. Borders between open zones are noise-warped so they do not look like pie slices; the final zone's two borders are straight (the wall runs along them) | Decided (0.5.0) |
| Size | Bounded: **~1300 blocks radius** (was 1200; the final zone's sector would otherwise shrink the open zones by about a sixth), ending in an abyss. Realms are places, not infinite worlds | Decided (0.5.0) |
| Final zone lock | **Fog wall, enforced by the server.** The final zone's sector is a region by angle and distance. The **Citadel Gate** at the landing plaza (where the Sealed Gate stood) is its one opening: a fog wall that lets a player through once they have beaten the 3 zone bosses (per player). Along both sector borders, from the plaza to the abyss, runs a burnt city wall with a curtain of ember fog, drawn by the client only near the player. A player without clearance whose position enters the region is pushed back out; ender pearls and chorus fruit landing inside fail, elytra gliding into it stops, riptide is refused; building over or tunnelling under the wall does not help because the check is the region, not the blocks | Decided (0.5.0) |
| Key structures | Each zone has one guaranteed boss arena (custom structure placement, position derived from the seed within the zone's sector) plus random minor structures | Proposed |
| Building | Realms are survival-normal: mine, build, place beds (beds do not set the respawn point there). Boss arenas, gates, shrines and obelisks are unbreakable, and nothing can be built inside an arena or the landing | Proposed — built as recommended (§10.1) |
| Navigation | A realm-bound item (working name *Ember Lodestar*) points to the nearest boss the player has not beaten | Proposed |

### 4.2 Ember Shrines, death and respawn
Demon's Souls gives an archstone shard after each boss. We do the same:

- Each zone boss leaves an **Ember Shrine** in its arena. The landing has one from the start. An arena shrine
  stands outside the fog wall and is dormant (cannot be rested at) for players who have not beaten that boss
  (**Proposed — built as recommended**, §10.8).
- Resting at a shrine: refills flasks, sets the **realm respawn point**, allows teleport to the hub.
- Dying **in a realm**: respawn at the last shrine rested at in that part of the realm (the open zones and
  the final zone count separately); with no rested shrine there, at the landing (open zones) or at the Citadel's
  first shrine just inside the gate (final zone, lit for a player once the gate has let them through). Your
  overworld bed spawn is not touched. (**Decided**)
- Dying **in the overworld**: vanilla respawn.
- The **Ash Remnant** (§7) applies everywhere: your XP — your currency — waits where you died.
- This reverses the old "no bonfire" decision. Shrines exist only in realms, the hub and at the dungeon's boss
  door (the last two never set a respawn point); the overworld keeps beds.

### 4.3 Bosses and multiplayer
Progress (beaten bosses, unlocked realms) is **per player**. A realm dimension is shared.

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
| 1 | **Ashen Reach** | Ashen Fields · Royal Woods · Sunken Necropolis → final: **Smoldering Citadel** | A burnt kingdom under eternal dusk. Full design in §4.5 | Decided (0.4.0) |
| 2 | *Mire of Rot* | swamp, plague village, drowned mines → final: the Font | Poison, disease, Valley of Defilement | Sketch |
| 3 | *Cathedral Ward* | gothic city, frozen cloister, catacombs → final: the Chapel | Bloodborne's Yharnam, beasts, hunters | Sketch |
| 4 | *The Void* | starfall plains, floating ruins, inverted sea → final: the Heart of the Void | Cosmic horror, the reason for the mod's name | Sketch |

Realms 2–4 are only themes. Each gets its own design pass (mobs, bosses, loot) before it is built; the first
round of variants for each is in [BACKLOG.md](BACKLOG.md).

### 4.5 Realm 1 — Ashen Reach (Decided, 0.4.0)

Decided by the user on 2026-10-22. All numbers are starting values for the balance pass. In Russian the realms
are called **обелиски** ("first obelisk" = Realm 1), not "реалмы".

#### Concept
- **One burnt kingdom under eternal dusk.** The zones are its districts, the Smoldering Citadel is its burning
  capital. No storyline: places, item descriptions and bosses tell it.
- **Tragedy — "The Greedy King".** The king wanted the Ember for himself and locked the flame in his citadel. The
  fire broke loose and burned the kingdom; the king became the **Smoldering Lord** and still "holds" the fire in
  the capital. Hints: empty village altars the flame was taken from and gallows for "hiders of the flame" in the
  Fields; the king's hunting grounds and his oak (the Grieving Tree) in the Woods; the kingdom's dead, burned
  without rites, in the Necropolis; the hoarded flame in the keep.
- **No realm-wide rule or meter.** Identity comes from atmosphere and from per-zone and per-arena hazards; the
  Smoldering Lord replays the three arena hazards as the final exam.
- **Only its own content:** no overworld mobs (Ash Ghoul, Hollow, Ember Wisp), no vanilla mobs, no mobs of other
  realms. **Eternal dusk:** no day/night cycle, `fixed_time` ≈ 13500 (surface monster spawning starts at ≈ 13188);
  nothing burns in daylight.
- **First 5 minutes:** the landing is the kingdom's crossroads plaza (shrine, Return Obelisk, Citadel Gate), low red
  sun. Three ash-brick roads with milestones, an outpost visible at each road head. Nearest is the Fields: first
  farm ruin, first Cinder Husk, first chest. The Lodestar is the backup.

Not chosen (kept for reference): tragedies "Scorched earth", "Funeral kingdom", "The Ember came from here"; rules
"ash storms as weather", "fire as a resource", "Citadel glow growing per boss"; location layouts "Canon", "Bowl",
"Eternal Smoulder", "Fog and Silence"; Citadel shapes "island in an ash sea", "chasm", "volcano"; Citadel places
(0.5.0) "across the abyss over one bridge", "in the centre with the zones as a ring".

#### Zones and places
Zone ids stay `zone_a/b/c`; the names change.

| Zone | Levels | Terrain | Look | Minor structures (placed enemies + chests) |
|---|---|---|---|---|
| **Ashen Fields** / Пепельные пашни | 15–18 | open low rolling plain, field terraces | ash soil, burnt furrows, grey-ochre fog | farmsteads, barns, windmill skeletons, scarecrow rows, granary ruins, pilgrim camp |
| **Royal Woods** / Королевский лес | 18–21 | hilly, ravines, dry stream beds | dead-wood forest, podzol, moss, dense grey-green fog | broken park wall, hunting lodge, kennels, hunters' hides, hanging cages and lanterns |
| **Sunken Necropolis** / Провалившийся некрополь | 21–25 | basin with karst sinkholes | gravel, tuff, cracked ash bricks, candles, blue-grey fog | mausoleums, grave rows, crypt entrances (crypt jigsaw below), chapels on sinkhole rims |

- **Terrain shape per zone:** one zone-aware density function (like `radial_falloff`), blended at the borders.
- **Water only in the Necropolis** (sinkhole bottoms, flooded crypts, the arena), placed by structures and
  features, not by sea level. Elsewhere dry.
- **Fog density per zone:** a client mixin on `BackgroundRenderer`, shared with the boss arena fog.
- **World edge:** the kingdom border — broken walls and collapsed bridges ending at the abyss cliffs.
- **Roads** from the landing to each arena: ash-brick road, a milestone every ~100 blocks, 2 ruined outposts per
  road (toll gate, watchtower) with placed enemies. Roads are the main navigation, side ruins sit off the road.
- **Smoldering Citadel** (the final zone's sector, levels 25–30; was its own depths dimension until 0.5.0) —
  "Burning City": the burning capital in the fourth sector, seen from the open zones over its wall. The Citadel
  Gate at the plaza opens onto the outskirts; a road runs out through burning streets to the Citadel hill on the
  sector's middle line, about 600–800 blocks from the landing. Ember-crack blocks and smouldering logs only here;
  the Citadel biome gives the dark red sky and fog (biome sky and fog colours, not a dimension type). Jigsaw route
  as before: outskirts → burning streets → city wall and gatehouse → inner ring and towers → courtyard below the
  keep wall (Smoldering Lord arena) → keep. The sector's outer part beyond the hill is burnt suburbs down to the
  abyss.
- **Cost:** ~6–8 new blocks (burnt straw, milestone, gravestone, ember crack, smouldering log, wet crypt stone…),
  ~20–25 minor `.nbt` pieces, 3 zone arenas, crypt jigsaw 5–7 pieces, Citadel jigsaw ~10–12 pieces. Java: zone
  density function, road generator, fog mixin, arena height offset.

#### Bestiary
Rules:
- **Placed enemies** in structures are set at worldgen (persistent, like Sanctum guards) and **never respawn**;
  resting at a shrine does not reset them.
- **Natural spawning** only for fodder and ranged mobs (+ the rare Ashen Carrion), so zones do not empty out.
  Tanks and elites are placed only. Zone biome `spawners` list only this realm's mobs.
- Road outposts hold the zone's fodder and ranged mobs, never tanks, so the first tank is met in its own structure.

Base stats at level 1; mob levels scale them (§5.4).

| Zone | Mob | Role | Spawn | Base | Behaviour and telegraph |
|---|---|---|---|---|---|
| Fields | **Cinder Husk** | fodder | natural, groups 2–4 | 16 HP, 3 dmg, speed 0.23, XP 6 | Burnt peasant with a tool. Plain melee, 10-tick raised-arm windup |
| Fields | **Ash Stalker** | ambusher | natural, alone | 14 HP, 4 dmg, 0.30, XP 8 | Buried in a furrow (invisible, invulnerable). Player within 5: ash boils + hiss for 20 ticks, then it leaps out |
| Fields | **Ashen Carrion** | elite flyer | natural, rare; perches on scarecrows, outposts | 18 HP, 4 dmg, XP 15 | Circles; dives when the player is below 50 % HP or every 8–12 s. Shadow on the ground + shriek 15 ticks before the dive |
| Woods | **Withered Thrall** | fodder, ambush | natural + placed in hanging cages | 18 HP, 3.5 dmg, 0.24, XP 8 | Hanged poacher; caged ones drop on a player passing under (chain rattle 10 ticks before) |
| Woods | **Lantern Hag** | ranged | natural, alone | 16 HP, XP 12 | Keeps 8–12 blocks; throws a fire flask in an arc (4 dmg + 3 s fire patch r 1.5). Raises the lantern 15 ticks before |
| Woods | **Bark Hollow** | tank | placed (park, glades, lodge) | 50 HP, armor 8, KB res 0.8, 7 dmg, 0.18, XP 22 | Looks like a dead tree until a player is within 6 (creak, eyes light). 3-block sweep, 20-tick windup. ×1.5 damage from axes and fire |
| Necropolis | **Restless Bones** | fodder | natural (surface and crypts), groups 2–3 | 16 HP, 4 dmg, 0.25, XP 8 | At 0 HP collapses into a bone pile (6 HP), re-forms after 5 s at half HP. Finish the pile, or light it (block light ≥ 8) and it stays dead |
| Necropolis | **Crypt Moth** | harasser flyer | natural under cover, groups 1–3 | 10 HP, 2 dmg, XP 8 | Flies to the nearest light within 12. Pollen burst: Blindness 3 s, glowing wings 15 ticks before. Snuffs placed torches (`mobGriefing`) |
| Necropolis | **Hollow Sentinel** | tank | placed (crypt halls, mausoleum doors) | 45 HP, armor 10, KB res 0.9, XP 22 | Tower shield: front 180° immune to melee and arrows, not broken by axes. Spear thrust 7 dmg (12-tick windup); shield bash 6 dmg + knockback (shield planted 15 ticks before), then open for 30 ticks |
| Necropolis | **Grave Warden** | elite | placed, 1 per crypt chamber or chapel | 80 HP, armor 8, 8 dmg, XP 40 | Bell-ringer. Every 12 s raises the bell (20 ticks): re-forms all bone piles within 12, raises 2 Restless Bones if none (max 4), staggers players within 4 (Slowness IV 2 s) |
| Citadel | **Cinder Hound** | pack fodder | natural in the streets, packs 3–5 | 14 HP, 3 dmg + 2 s fire, 0.35, XP 7 | Fast; a howl pulls the pack |
| Citadel | **Sparking Marksman** | ranged | placed on walls and gatehouse | 24 HP, bolt 6 dmg + fire, XP 15 | Crossbow; a spark on the bolt tip 20 ticks before firing. Never leaves the wall |
| Citadel | **Furnace Golem** | tank | placed (inner ring) | 70 HP, armor 12, 9 dmg, 0.15, XP 30 | Heats up with each hit taken; at 4 hits glows 25 ticks, then a fire wave r 4 |
| Citadel | **Smoldering Knight** | elite | placed (towers, keep) | 75 HP, armor 10, 8 dmg, XP 50 | 3-hit combo with short windups; 6-block lunge after a 15-tick crouch |

Drops: vanilla-flavoured basics (bones, charcoal, rotten flesh, string where it fits) plus the realm loot below.
Cost: 14 GeckoLib models (10 for the zones, 4 for the Citadel), ~70 animations.

#### Bosses
Line-up: giants with spectacle, and arenas that take part in the fight.

| Zone | Boss | Level | Base HP → at level | Arena | What it teaches |
|---|---|---|---|---|---|
| Fields | **Ash Colossus** (4.5 blocks, ember core in the back) | 18 | 180 → ~364 | market square sunk into a crater, burnt-granary pillars, r ~40 | use cover, strike the back |
| Woods | **Grieving Tree** (the king's oak, ~10 blocks, hanged lanterns) | 21 | 170 → ~374 | Royal Grove: walled garden behind the hunting lodge, arena fog, r ~48 | shoot the lanterns, keep orientation in fog |
| Necropolis | **Crypt Keeper** (4 blocks, halberd + lantern) | 24 | 180 → ~428 | sinkhole open to the sky, switchback stair down into a flooded ossuary, r ~32 | light: braziers against darkness and moths |
| Citadel | **Smoldering Lord** (3.5 blocks, greatsword) | 30 | 260 → ~712 | courtyard below the keep wall | the exam: replays the three arena hazards |

- **Ash Colossus.** P1: fist cone, leap ring, thrown boulder, arm drag; every ~20 s an ash storm (2 HP/s magic +
  Blindness unless behind a pillar; he smashes pillars, so the arena needs ≥ 8). P2 at 50 %: sheds stone, core
  exposed, faster, falling cinders. Front takes −50 %, the core ×2.
- **Grieving Tree.** P1: rooted, trunk takes −70 % unless stunned; 4–5 lanterns (any projectile, or melee on the
  branch lowered after a sweep) → 5 s stun, trunk ×1.5, fog lifts for 6 s; root lines, branch cone, Withered
  Thralls drop. P2 at 50 %: uproots and walks slowly, lanterns on its own branches, thicker fog, root cage.
- **Crypt Keeper.** P1: halberd 3-hit (the third is an unblockable overhead that breaks the shield), sweep ring,
  lantern throw → fire pool. P2 at 60 %: the lantern shatters → Darkness, water rises 1 block, 4 braziers; in the
  dark he takes −50 %, in brazier light normal; Crypt Moths smother lit braziers. P3 at 30 %: water 2 blocks on
  the lower floor, Restless Bones rise, only walkways and pedestals are dry. Ossuary ceiling ≥ 9.
- **Smoldering Lord.** P0 on the wall (untouchable): mortar rings, flame lines, Sparking Marksmen; lighting 3
  signal braziers brings him down (landing ring r 6). P1: greatsword combos, lunge line with a fire trail. P2 at
  50 %: armor shatters (+25 % damage taken, faster); every ~25 s one zone hazard: ash storm, fog or darkness.
- **Damage:** level × Normal 1.25 gives ×2.31 / ×2.5 / ×2.69 / ×3.06; base 5–11 per move. Against full diamond on
  Normal: light hits ~4–6, heavy ~8–12, unblockable ~12–17 HP.
- **Boss XP** (before level scaling): Ash Colossus 150, Grieving Tree 170, Crypt Keeper 190, Smoldering Lord 300;
  tank and elite XP and Citadel elite health in the bestiary table were lowered in the same pass (**Proposed — built as recommended**, R40.1 balance pass, `docs/balance-0.4.0.md`).
- **Levels are fixed** (18 / 21 / 24 / 30), not scaled by progress: the player picks the easier zone first, as in
  Demon's Souls.
- **Multiplayer:** HP ×(1 + 0.5·(n − 1)) at summon, +1 add per extra fighter, damage unchanged; target switch every
  8–10 s or to the top damage dealer.
- **Rematch:** a consumable (working name *Ash of Rematch*) used at the arena shrine of a beaten boss summons it
  again for loot and XP, never its soul. Source and price: Proposed — built as recommended (§10.11).
- **Models:** script-generated greybox models at final size with draft animations under the final animation names
  (like `tools/mob_placeholders.py`); fights are tuned on them, art later.
- **Music:** one shared zone-boss theme and a separate Smoldering Lord theme (CC-licensed, `CREDITS.md`).
- **Framework additions:** arena hazard layer (ticked by the fight, restored on reset; real blocks only for water),
  S2C arena fog, telegraphs (ring at a point, line, cone, falling shadow), shield-breaking hit, fight-bound adds,
  a non-HP phase (the Lord on the wall), HP scaling hook at fight start.

#### Loot
The Minecraft way: an ore in each open zone → material → consumables and armor. All weapons are unique.

**Ores** (each generates only in its zone; none in the Citadel):

| Zone | Ore | Drop | Pickaxe | Rarity | Main use |
|---|---|---|---|---|---|
| Fields | Cinder Ore (in ash stone) | 1–3 Cinder Shard (Fortune works) | iron | like iron | fire consumables, fuel, template, ingot |
| Woods | Amber Vein (ravine walls, under roots) | 1–2 Royal Amber | iron | like gold | healing and cure consumables, ingot |
| Necropolis | Grave Iron Ore (deep, sinkhole walls, crypts) | Raw Grave Iron → Grave Iron Ingot | diamond | like diamond | armor metal, ingot |

- **Smoldering Ingot** = 4 Grave Iron Ingot + 2 Cinder Shard + 2 Royal Amber (shapeless): all three zones needed.
- No realm tools in 0.4.0: netherite tools stay the best.

**Armor**, three kinds:
1. **Crafted — Smoldering Plate:** a diamond piece + Smoldering Ingot + Smoldering Template on the smithing table
   (like netherite). Template: 1 diamond + 6 Cinder Shard + 2 ash stone → 1. Diamond protection (3/8/6/3),
   toughness 2.5 per piece, durability ~1.15× diamond, −8 % fire damage per piece. Set: no burning ticks (not
   lava). A full set ≈ 16 Grave Iron + 4 diamonds for templates.
2. **Mob-drop sets, one per zone** (a piece: 3 % from zone fodder on a player kill, 15 % from the zone elite;
   FOR applies):
   - Fields — **Reaper's Garb** (Cinder Husk, Ashen Carrion): armor 15, toughness 0; set +10 % move speed.
   - Woods — **Huntsman's Leathers** (Lantern Hag, Withered Thrall, Bark Hollow): armor 17, toughness 1 per piece;
     set +15 % projectile damage.
   - Necropolis — **Necropolis Warden** (Restless Bones, Hollow Sentinel, Grave Warden): armor 20, toughness 2 per
     piece, −5 % speed; set +0.3 knockback resistance, −15 % damage while blocking with a shield.
3. **Unique pieces**, one per slot (0.5 % from that zone's mobs, 4 % from its elite, player kill only):
   - Helmet **Scarecrow's Mask** (Fields): mobs notice you at half distance; Ash Stalkers do not ambush you.
   - Leggings **Huntsman's Greaves** (Woods): +20 % speed for 4 s after a kill; no fall damage up to 7 blocks.
   - Boots **Drowned Sabatons** (Necropolis): normal speed in water, no slow in shallow water.
   - Chestplate **Furnace Heart Cuirass** (Citadel: Furnace Golem, Smoldering Knight): below 30 % HP releases a fire
     nova (r 4, knockback, 6 ember damage), once per 60 s.

**Weapons** — all unique; each comes from one boss's soul or, rarely, from a treasure chest of its zone.
- **Soul** = the boss's first-kill personal drop (the Ember Heart of §4.3, one per boss). Helping a friend on a
  repeat kill: 25 % chance of the soul. Recipe: soul + 2 Smoldering Ingot + a zone part, crafting table (0.4.0;
  from 0.5.0 a zone boss's unique needs no Smoldering Ingot, §6.1).
- **Chest:** 4 % in the zone's treasure chest (Fields granary vault, Woods hunting lodge, Necropolis lower crypt,
  Citadel keep treasury); first come, first served in the shared dimension.
- The numbers below are 0.4.0's; 0.5.0 moves the weapons above netherite with new identities (§6.1).

| Weapon | Soul / zone | Numbers | Passive | Special (right-click, §8.1) | Scaling |
|---|---|---|---|---|---|
| **Ashen Scythe** | Ash Colossus / Fields | 10 dmg, speed 1.1 | sweep r 2.5 at 75 % damage; a fully charged hit sends an ash wave 6 blocks forward, 3 wide, 3 ember dmg | Harvest: 360° spin r 3.5, 25 stamina, 4 s | STR 0.6 / DEX 0.4 |
| **Huntsman's Crossbow** | Grieving Tree / Woods | charge 2.0 s, bolt ≈ 1.5× vanilla crossbow | strong knockback (≈ Punch III), pierces 1 | none (right-click loads) | DEX 0.5 / STR 0.5 |
| **Warden's Scourge** (whip) | Crypt Keeper / Necropolis | 9 dmg, speed 0.9, reach 6, single target | Bleeding +1 per hit | Lash: pulls the target 4 blocks toward you, 20 stamina, 5 s | DEX 1 |
| **Royal Ash Rapier** | Smoldering Lord / Citadel | 6 dmg, speed 2.0, reach 2.5 | Bleeding +1 per hit, sets on fire 3 s | Lunge: 4-block dash + thrust, double bleed, 20 stamina, 3 s | DEX 0.7, ember part KIN |

- Reach (whip 6, rapier 2.5) needs a server raycast on attack: 1.20.1 has no reach attribute.
- Scythe, Scourge and Rapier take Sharpness, Smite, Bane of Arthropods, Fire Aspect, Looting and Knockback (table and
  anvil); Sweeping Edge stays sword-only. Scythe 10 and Scourge 9 damage (were 8) so that unenchanted they beat a
  diamond sword per hit (**Proposed — built as recommended**, R40.1 balance pass, `docs/balance-0.4.0.md`).
- **Bleeding** (placeholder; the status-effects system is a 0.6.0 topic, [BACKLOG.md](BACKLOG.md)): a vanilla status effect,
  levels I–III (each hit +1, 6 s, refreshes), 0.5 / 1 / 1.5 HP per second, ignores armor. Immune: entity tag
  `embervoid:bleed_immune` (Restless Bones, Furnace Golem, Bark Hollow, Ash Colossus, Grieving Tree).

**Artifacts** (+ stat points; not counted in level or level cost) — 8: Fields: Last-Harvest Grain +3 VIT,
Ploughman's Horseshoe +3 END. Woods: Royal Hound Fang +3 DEX, Amber Tear +3 KIN. Necropolis: Gravestone Medallion
+3 RES, Ferryman's Coin +3 FOR, Warden's Chain +3 STR. Citadel: Seal of the Ashen King +1 to every stat.

**Rings** (unique effects) — 6: Fields: Courier's Ring (+15 % sprint speed), Ring of Ash Feathers (slow fall, no
fall damage under 8 blocks). Woods: Ring of the Fog Stalker (fog clears twice as far, not in boss arenas), Delver's
Ring (Haste I while mining realm ores). Necropolis: Drowned Man's Ring (water breathing). Citadel: Ring of Embers
(melee hits ignite 2 s).

Artifacts drop mainly from treasure chests (8 % per chest) and elites (2 %); rings from chests (5 %) and elites (1 %).

**Reliquary** (accessory slots, our own, no extra mod): a separate screen, opened by a button next to the recipe
book on the inventory screen and by a keybind. It shows the player model, 2 artifact slots and 2 ring slots in
0.4.0; slot kinds and counts are data (tags `embervoid:artifacts`, `embervoid:rings`), so later realms can add
slots or kinds. Stored in a player attachment synced to the owner; effects are attribute modifiers with fixed
UUIDs, artifact stat points go through the stat system without raising level or cost. On death accessories go into
the Ash Remnant with the rest of the inventory and follow its rules (decided "for now"). Not extra slots in the
vanilla inventory: that breaks other inventory mods and the creative screen.

**Consumables:**
- Cinder: Ash Bomb (thrown, fire ring r 2, 4 ember dmg), Smoldering Resin (the weapon ignites targets for 60 s),
  fuel (= 2 coal).
- Amber: Amber Balm (cures bleeding and poison, Regeneration I 8 s), Amber Lantern Oil (an offhand ember lantern
  burns 10 min: for the Necropolis darkness and the Crypt Keeper).
- Grave Iron: Whetstone (repairs 25 % of a realm weapon; uniques also repair on an anvil with an ingot).
- Flask: each zone boss's first kill also gives a Flask Shard (+1 charge, personal, 3 in total); the Smoldering
  Lord gives a Flask Ember (+15 % healing per sip, once).

**Chests** (shared, first come, like the Sanctum's):

| Structure | Contents |
|---|---|
| Road outposts, farmsteads, barns, kennels, graves | zone ore drops, vanilla food, arrows, iron and gold, Ash Bombs, Balm |
| Treasure chests: granary vault, hunting lodge, lower crypt, Citadel keep | Smoldering Ingot (rare), templates, mob-set pieces, artifacts 8 %, rings 5 %, the zone weapon 4 % |
| Chest by the arena | 1 template + zone consumables |

**Why each zone is worth doing:** Fields — Cinder, Reaper's Garb, Scarecrow's Mask, Scythe, 2 artifacts, 2 rings.
Woods — Amber, Huntsman's Leathers and Greaves, Crossbow, 2 artifacts, 2 rings. Necropolis — Grave Iron (the armor
metal), the Warden set, Sabatons, Scourge, 3 artifacts, 1 ring. Citadel — no ore; Cuirass, Rapier, the Seal, Ring of
Embers, Flask Ember.

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

What stats do in 0.2.0, before weapons and ember damage exist (§10.10): FOR → luck + an extra mob-drop chance
(**Decided**); KIN → flask healing, RES → armor toughness + fire damage reduction (**Proposed — built as
recommended**). Two Sanctum weapons with a special attack, so END matters early, came in 0.4.1 (**Proposed — built as
recommended**, §10.10).

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
| XP dropped | base × (1 + 0.06·(L − 1)) — was 0.10; lowered in the 0.4.0 balance pass so Realm 1 ends near Lv 36 |

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
| Any other hostile (vanilla or modded) | 4 — Proposed — built as recommended (§10.9) |
| Ender Dragon, Wither, Warden, Elder Guardian | no level, vanilla stats — Proposed — built as recommended (§10.9) |

| Bonus | Value | Status |
|---|---|---|
| Distance from world spawn | +1 per 1000 blocks, max +5 | Proposed |
| Below y=0 | +3 | Proposed |
| Nether | +5 | Proposed — built as recommended (§10.9) |
| End | +8 | Proposed — built as recommended (§10.9) |

With these bonuses an End enderman is level 20, the top of the overworld band (was +10 / +20, which put it at
32, above the Realm 1 boss — see §10.9).

**Realm levels** are set per zone. Realm 1: Ashen Fields 15–18, Royal Woods 18–21, Sunken Necropolis 21–25,
final zone 25–30. Bosses have fixed levels at the top of their zone: 18 / 21 / 24, realm boss 30 (§4.5).

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

### 5.5 Fight length and hit sizes (Decided, 0.5.0)

The 0.4.1 playtest: boss fights took 5+ minutes while a boss killed in about two hits (the 0.4.0 model gave 52–73
clean hits per Realm 1 boss; souls games sit at about 20–35). Decided 2026-10-23 (PLAN 17.0); the whole game is
retuned to these targets in 0.5.0 (PLAN milestone 18). "Expected player" is the balance model's player at that
stage with that stage's reference weapon, Normal difficulty.

| Target | Value |
|---|---|
| Boss fight, solo, with dodging | **1.5–2.5 min**; **20–35 clean hits** with the stage's reference weapon (dungeon boss: iron sword at level 1; first zone boss: diamond sword at level 10; later zone bosses and the Citadel: a Realm 1 unique) |
| Mobs, hits to kill | fodder 2–3, ambushers and ranged 3–4, elites 6–10 |
| Hits to kill the player from full HP | fodder ≥ 6, elites ≥ 4; boss light moves ≥ 5 (≤ 20 % of max HP), heavy ≥ 3 (≤ 35 %), the biggest move ≤ ⅔ — never a one-shot |
| Wind-up rule | a hit of ≥ ⅓ of the player's max HP needs a wind-up of ≥ 12 ticks with a visible tell; ≥ ½ needs ≥ 20 ticks plus a telegraph shape (ring, line, cone, falling shadow) or a sound cue. Checked by a test over every boss move |

How: boss HP down to the hit count, boss damage down by move class; the player's HP scale stays vanilla-like
(hearts stay readable) and the VIT curve changes only if the targets cannot be met otherwise. The balance model
turns hits into seconds with an uptime factor per boss from its move cadence. The model (PLAN 18.1,
`docs/balance-0.5.0.md`) shows both targets meet only at an uptime of about 8–24 % with a sword and 13–43 % with a
slower unique, so **bosses that give too many openings get fewer** rather than a shorter target (the user,
2026-10-23): the Grieving Tree (stunned after every sweep, about 50 % uptime), the Vampire boss (about 63 %) and the
Fallen Sister (built at about 20 %) are reworked in their cadence; the others only lose HP.

## 6. The carrot: loot ladder

Each realm has its own **material tier** and gear set. Rough position against vanilla:

| Source | Tier | Status |
|---|---|---|
| Overworld | Vanilla up to diamond / netherite | — |
| Ruined Sanctum (dungeon) | The Crying Soul from the Fallen Sister (§2); in the outer halls flask shards and the Sanctum Longsword and Ashbearer's Mace (§10.10), between iron and diamond; vanilla loot; the Wanderer's Cloak moved to [BACKLOG.md](BACKLOG.md) | Decided |
| Realm 1 | **Above netherite** (0.5.0): unique weapons about **+30 % damage per second** over an unenchanted netherite sword in their own use (§6.1); Smoldering Plate raised to netherite (protection 3/8/6/3, toughness 3, knockback resistance 0.1 per piece) plus −8 % fire damage per piece; rings and artifacts (§4.5) | Decided |
| Realm 2 | about +25 % over Realm 1 | Proposed |
| Realm 3 | about +25 % over Realm 2 | Proposed |
| Realm 4 | top tier | Proposed |

Enchantments keep mattering on every tier: a unique is meant to beat netherite unenchanted, not to make
enchanting pointless. Decided 2026-10-23 (the user's pick of about 30 % over a far jump of about 60 %).

**Open:** should Realm 2 require netherite gear (the Nether as a soft gate), or stay independent of vanilla
progress?

Loot per realm:
- **Material** from zone ores, mobs and chests; crafted into armor (and, from Realm 2 on, maybe weapons) of that tier.
- **Boss uniques** (from *Ember Hearts*, the boss's soul): signature weapons. In Realm 1 every weapon is unique
  and comes from a soul or a rare chest; armor comes as a crafted set, mob-drop sets and unique pieces (§4.5).
- **Accessories:** artifacts (+ stat points) and rings (unique effects), worn in the Reliquary (§4.5).
- **Consumables and upgrades**: flask shards (more charges / more healing), realm-specific items.

---

### 6.1 Realm 1 weapons above netherite (Decided, 0.5.0)

The 0.4.1 playtest: every Realm 1 weapon looked bad and felt worse than vanilla (the Rapier, 6 damage at speed 2.0
with reach 2.5, matched a diamond sword per second, had less reach than vanilla and no sweep). Each unique now has an
identity felt in hand, a bigger in-hand model and its own player attack animation (PLAN 17.1–17.4). Reference:
netherite sword 8 damage at 1.6 = 12.8 per second; the target for a unique in its own use is about 16–17.
Numbers are the starting point for PLAN 17.3; the balance model (PLAN 18.1) may move them a little.

| Weapon | Identity | Starting numbers | What makes it itself |
|---|---|---|---|
| **Ashen Scythe** (STR 0.6 / DEX 0.4) | the crowd cutter | 12 dmg, speed 1.1, reach 3.5 | every hit sweeps r 2.5 at 75 %; a fully charged hit sends the ash wave; each enemy caught by a sweep or by Harvest refunds 3 stamina. Weaker than the others on one target, by far the best on groups |
| **Royal Ash Rapier** (DEX 0.7, ember part KIN) | the duellist | 7 dmg, speed 2.2, reach 3.5 | a thrust, no sweep; hits on the same target within 1.5 s ramp +10 % each up to +30 % (lost on a miss or a hit taken); fire 3 s; Lunge. About 15 per second, about 20 ramped |
| **Warden's Scourge** (DEX 1) | the whip | 10 dmg, speed 1.0, reach 6 | hits every enemy along the swing's arc; the tip (last 1.5 blocks) cracks for ×1.6; Lash still pulls. About 16 per second at the tip |
| **Huntsman's Crossbow** (DEX 0.5 / STR 0.5) | the hunter's bolt | charge 1.6 s, bolt 16, pierces 2 | strong knockback; a bolt into a target that is not targeting you deals ×1.5 (the opener); right-click loads |

Bleeding stays the 0.4.0 placeholder (a vanilla effect, §4.5) until the status-effects system in 0.6.0; the
rebalance counts it as it is. The Sanctum Longsword and Ashbearer's Mace keep their place between iron and diamond.

**Uniques during the realm, not after it** (the user, 2026-10-23): the balance model found that every unique needed
2 Smoldering Ingots, and an ingot needs all three zones' materials, so no unique existed before the Citadel. A zone
boss's unique is now crafted from its soul + its zone's material + the zone part (Ashen Scythe: soul + 4 Cinder
Shard + the part; Huntsman's Crossbow: soul + 4 Royal Amber + the part; Warden's Scourge: soul + 4 Grave Iron Ingot
+ the part), so the 2nd and 3rd zone bosses are fought with a unique. The Royal Ash Rapier, the Smoldering Lord's,
keeps the Smoldering Ingots. The 4 % chest drop stays.

## 7. Core mechanics (carried over)

| Mechanic | Design | Status |
|---|---|---|
| Stamina | Spent **only** by right-click special attacks. Vanilla hits, sprinting and blocking stay free. Max and regen from END | Decided |
| Healing Flask | Charges (start 3), heal over time, cannot drink while sprinting. Refilled by resting at an Ember Shrine, sleeping through the night in a bed, and on respawn after death. The flask is never dropped or put into the Ash Remnant: it stays in the inventory on death, full after respawn. Charges live in player data (built per §10.6). A lost flask is crafted again from a glass bottle + a diamond + coal or charcoal (shapeless); a second flask adds no charges | Decided |
| Flask Shards | Permanent +1 charge or +healing per sip, capped | Decided |
| Ash Remnant | On death XP and all items except the Healing Flask (Reliquary accessories included, §4.5) go into a remnant at a safe spot (the last position on solid ground). Modes `off` / `xp` / `xp_and_items`. With XP as the leveling currency this is our bloodstain. Dying again before reaching it burns the old remnant's XP; its items stay in it until picked up (config `remnant.secondDeath` = `burn_xp` default / `burn_all`, §10.7) | Decided (second death: Proposed — built as recommended) |
| Full-set bonuses | Armor sets grant a bonus when all 4 pieces are worn | Decided |

---

## 8. Separate design topics

These are big enough to get their own documents. Each is needed before the milestone that builds it.

| Topic | Covers | Needed by |
|---|---|---|
| **Weapons** | Weapon classes, STR/DEX/KIN scaling and requirements, special attacks, how vanilla weapons fit in, upgrades | **Special-attack contract** (trigger, cost, cooldown, tooltip, limits): done (§8.1). Realm 1 weapons: done (§4.5). Upgrades: [BACKLOG.md](BACKLOG.md). Sanctum weapons: done (§10.10). Requirements: open |
| **Realm 1 content** | Per-zone mobs, structures, the 4 bosses (moves and phases), loot tables | Done (§4.5) |
| **Status effects** | Bleeding and later effects, RES resistance, immunities, cures | Realm 1 ships a placeholder Bleeding (§4.5); the system: 0.6.0, [BACKLOG.md](BACKLOG.md) |
| **Rings and artifacts** | Catalogue across the mod, more slots, stacking, sources outside realms | Realm 1 set and the Reliquary: done (§4.5); the rest: [BACKLOG.md](BACKLOG.md) |
| **Bestiary (overworld)** | Hollow and Ember Wisp in detail, behaviors | Overworld threats — done (§3.2) |
| **Realms 2–4** | Each realm's zones, mobs, bosses, material | Each realm |

### 8.1 Special-attack contract (Decided)

The part of the weapons doc every special attack follows:

- **Trigger:** right-click (use) with the weapon in the main hand or offhand, in air or on a block. No new keybind.
- **Cost:** a fixed stamina amount per use, set by the weapon. Allowed while any stamina is left (the pool
  bottoms out at 0, souls-style). With 0 stamina the use fails: the HUD bar flashes, an action-bar message and
  a fizzle sound. Creative and spectator never spend stamina.
- **Cooldown:** the vanilla item cooldown per item type, started on a successful use and shown as the vanilla
  overlay.
- **Tooltip:** "Right-click: <name>" and "<cost> stamina, <cooldown>s cooldown".
- **Limits:** runs on the server only; breaks and places no blocks; does no damage through walls; hits go
  through the normal `damage()` call with the player as attacker, so armor, mob levels and weapon scaling apply;
  no teleporting through blocks. Vanilla left-click attacks, sprinting and blocking stay free.

---

## 9. Technical notes

- **Minecraft 1.20.1, Fabric.** Required: Fabric API, GeckoLib (animated mobs and bosses), playerAnimator
  (player attack animations, from 0.5.0).
- **TerraBlender is no longer needed.** The Ash Wasteland was an overworld biome; now it lives in Realm 1's
  own dimension with our own `BiomeSource`. The overworld gets no new biomes.
- Realm dimensions: datapack `dimension` / `dimension_type` / noise settings + Java `BiomeSource` and a custom
  `StructurePlacement` for guaranteed per-zone arenas. Final zones are their own dimensions.
- Teleports between dimensions with `FabricDimensions.teleport` and a `TeleportTarget` (no portal search).
- Player data (stats, level, beaten bosses, unlocked realms, realm respawn points, flask
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
   **Proposed — built as recommended:** building is allowed; arenas, the landing, gates, shrines and obelisks
   are protected.
2. Character level cap for 0.x (none / 120)?
3. Does Realm 2 require netherite (Nether as a soft gate)?
4. Number of realms for 1.0: 4 as sketched, or 5 like Demon's Souls?
5. Can players respec? Recommendation: later, through a rare consumable.
6. **Where do flask charges live?** On the item, a player can carry five flasks and get five times the
   heals; "refill all flasks in the inventory" makes that worse. Nothing says yet how a player gets a
   flask. Recommendation: charges and upgrades live in player data; the flask item only shows and spends
   them, so a second flask adds nothing. One flask is given on first join and can be crafted again if lost.
   *Needed by:* milestone 2 (PLAN 2.7).
   **Decided:** charges and upgrades live in player data. The flask is kept on death (never dropped or put into
   a remnant) and refilled on respawn. A lost flask is crafted from a glass bottle + a diamond + coal or
   charcoal (shapeless). See §7.
7. **What does a second death do to the first remnant?** As written it destroys everything, items
   included: dying on the way back loses your diamond gear for good, which is harsher than both vanilla and
   Demon's Souls (there only the souls are lost). Recommendation: the XP burns; the items stay in the old
   remnant, which becomes an item-only remnant until picked up. Config `remnant.secondDeath`
   = `burn_xp` (default) / `burn_all`.
   *Needed by:* milestone 2 (PLAN 2.10).
   **Proposed — built as recommended:** `remnant.secondDeath` with `burn_xp` as the default and `burn_all` as
   the option.
8. **How does an arena shrine work for players who have not beaten its boss?** The shrine is a block in a
   shared dimension: a player who has not beaten the boss could rest in the arena where the boss summons
   for them. Recommendation: the shrine is part of the arena structure, stands outside the fog wall, and is
   dormant (cannot be rested at, looks unlit to them) for players who have not beaten that boss.
   *Needed by:* milestone 7.
   **Proposed — built as recommended:** outside the fog wall and dormant ("The ember here is cold to you"); since
   0.4.1 it also looks cold (smoke, no flame) to a player who has not beaten its boss. Its light level stays the same
   for everyone.
9. **Nether and End level bonuses, unlisted mobs.** The first values, +10 / +20, put End endermen at level 32 (above the Realm 1
   boss) and Nether mobs in the Realm 1 band, which breaks the "overworld 3–20" band in §5.4. Nether mobs,
   illagers and modded hostiles have no base level. Recommendation: Nether +5, End +8 (End endermen ≈ 20, the
   top of the overworld band); base level 4 for any hostile not listed; vanilla bosses keep vanilla stats.
   *Needed by:* milestone 3 (PLAN 3.5).
   **Proposed — built as recommended:** Nether +5, End +8, unlisted hostiles 4, vanilla bosses unleveled (the
   `embervoid:unleveled` entity tag). All in the `mobs` config section.
10. **Which stats do something in 0.2.0?** Stamina is spent only by special attacks, and the only one in
    0.2.0 is on a debug weapon, so END does almost nothing. KIN has no ember damage yet, RES status
    resistance is a system that does not exist yet, and vanilla `luck` barely affects anything (fishing and a
    few loot tables). Points put there in 0.2.0 feel wasted. Recommendation: KIN → flask healing from 0.2.0;
    RES → armor toughness + fire damage reduction; FOR → luck + a small bonus chance on mob drops; ship 1–2
    real weapons with a special attack in the Sanctum outer-hall loot so END matters.
    *Needed by:* milestone 3 (PLAN 3.2), and milestone 5 for the weapons.
    **Decided:** FOR → luck + a small bonus chance on mob drops in 0.2.0. **Proposed — built as recommended:**
    KIN → flask healing, RES → armor toughness + fire damage reduction; the Sanctum weapons (0.4.1): the **Sanctum
    Longsword** (DEX, 6.5 damage, speed 1.6; Wide Cleave: a 90° arc of 3 blocks at 1.25× its hit, 25 stamina, 3 s) and
    the **Ashbearer's Mace** (STR, 8 damage, speed 0.9, breaks a Hollow's guard like an axe; Ground Slam: a ring of 3
    blocks at 0.75× its hit with knockback and 2 s Slowness II, 30 stamina, 5 s), 6 % each per outer-hall chest (one of
    them in about 40 % of Sanctums). Between the iron and the diamond tier and below the realm weapons: neither beats
    a realm weapon both per hit and per second (`docs/balance-overworld.md`).
11. **Rematch item for Realm 1 bosses** (§4.5): where does *Ash of Rematch* come from and what does it cost (a
    craft from realm materials, a chest drop, XP at the Maiden)?
    *Needed by:* milestone 12.
    **Proposed — built as recommended:** crafted, shapeless: 4 Cinder Shard + 1 Royal Amber + 1 bone → 1 Ash of
    Rematch (recipe JSON, easy to change); one also lies in each zone's arena-side chest. Used on the arena shrine
    of a boss the player has beaten: summons it again for loot and XP, never its soul or Flask Shard.
12. **Moon nights** (§3.3): rewards for both nights; whether a player kill counts toward the curse; and the four
    conflicts listed in §3.3 (remnant in the Vampire arena, who is pulled into it, Vampires in multiplayer, one
    shared night).
    *Needed by:* milestone 8.
    **Proposed — built as recommended** (all numbers in the `moon` config section):
    - *Rewards.* Ashen Zombie 20 XP, Ghost 40 XP (before level scaling). A player who clears a Ghost wave gets a
      Flask Shard. Vampire boss, personal loot for every credited player: first kill 2 Flask Shards + a *Vampire
      Fang* (trophy, a material for later); repeat kills 50 % one Flask Shard. Boss XP like a zone boss.
    - *Player kills* do not add curse stacks; only a direct kill by a hostile mob (or its projectile) does.
    - *Remnant in the arena.* When the arena closes, a remnant left there moves to its owner's return point (where
      they were pulled from). Nothing is destroyed. A player killed there respawns as usual and stays out.
    - *Who is pulled.* Every player in the overworld outside a Sanctum core (from 0.5.0: in the overworld; the hub
      is its own dimension). Players in realms, the Nether, the
      End or a boss fight are not pulled and do not take part.
    - *Vampires in multiplayer.* One Vampire group per cluster of overworld players (players within 64 blocks
      share one). The first hit on any Vampire turns that one; all the others vanish.
    - *One shared night.* The night is held while any overworld player's zombies or ghosts are alive. A player
      outside the overworld gets their wave on return only while the night is still on. The 30-minute fallback
      ends it for everyone.
13. **The hub and the dungeon** (the 0.4.1 playtest): the Maiden and the obelisks move out of the overworld
    Sanctum. Where to, how the player gets in, who guards the way, multiplayer, old worlds.
    *Needed by:* milestone 15 (PLAN 15.0).
    **Decided** (2026-10-23, §2): a hub dimension reached through a **Cleft Stele** kindled by the **Crying Soul**;
    the overworld structure is the **Ruined Sanctum** dungeon guarded by the **Fallen Sister** (the user's pick of
    three concepts); **one lit stele per world** (the user's pick over one per player); an **Ember Shrine at the boss
    door** (the user's pick). The rest (hub layout, exit fallback, Cleft Stone recipe, per-dungeon clearing, names,
    old worlds) decided in the same pass, easy to change.
14. **Weapons and fight length** (the 0.4.1 playtest): how far above netherite Realm 1 goes, what each unique is,
    how long fights last and how big a hit may be.
    *Needed by:* milestones 17–18 (PLAN 17.0).
    **Decided** (2026-10-23, §5.5, §6, §6.1): Realm 1 uniques about **30 % above netherite** per second (the user's
    pick), Smoldering Plate raised to netherite; bosses 1.5–2.5 min and 20–35 hits; the wind-up rule; the weapon
    identities and starting numbers decided in the same pass, easy to change. **Status effects** postponed to 0.6.0
    (the user: a big topic of its own); Bleeding stays the placeholder.
15. **Where the final zone goes** (the 0.4.1 playtest: the teleport into a separate depths dimension is "a shame").
    *Needed by:* milestone 21 (PLAN 21.0).
    **Decided** (2026-10-23, §4.1, §4.5): the **fourth sector** of the realm dimension (the user's pick over "across
    the abyss" and "in the centre"): final zone 60°, open zones 100° each, radius 1200 → 1300; the Citadel Gate at the
    plaza; a server-enforced region lock with the wall drawn along both borders. Worlds from before 0.5.0: their
    realm dimensions are regenerated (nothing has been published; progress, inventories and remnants carried out
    stay, players inside are moved to the hub), decided in the same pass, easy to change.
