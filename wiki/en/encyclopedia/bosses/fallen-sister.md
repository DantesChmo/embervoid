---
entities: [embervoid:fallen_sister, embervoid:crying_soul]
kind: boss
---

{{stats}}

## How to reach it

The Fallen Sister waits at the bottom of every [Ruined Sanctum](../places/ruined-sanctum.md). Fight your way down the
stairwell and through the guarded halls to the boss door. An [Ember Shrine](../blocks/functional/ember-shrine.md) stands
in front of it: rest there before you go in. Then walk through the glowing fog curtain into the arena, a dim
room with a cold [Cleft Stele](../blocks/functional/cleft-stele.md) on the dais at its far end.

She appears as soon as you step into the middle of the room. A fog wall closes behind you: nobody leaves and nobody
else gets in until the fight is over. If you die or get out some other way, she is gone, and the next time you step in
she starts again at full health.

Each Sanctum's Sister falls once per world. After that the dungeon is cleared and she never comes back there; there is
no rematch. For another [Crying Soul](../items/souls/crying-soul.md), find another Sanctum.

She is the mod's gatekeeper and is built to be beaten by a fresh character in iron with an iron sword.

## Phases and moves

Every move is marked on the floor before it lands. Watch the flames on the ground, not her body.

### Phase 1

- **Censer arcs.** The chain winds around her arm, then she swings the censer in a wide cone in front of her, right to
  left, and at once back again. The first swing has the longer wind-up; the second follows quickly. Step back out of
  the cone, or step behind her, and wait for both swings before you go in.
- **Slam.** She brings the censer down on a ring of flame just in front of her. It knocks you up, and a puddle of embers
  keeps burning on that spot for a few seconds <!-- FallenSisterEntity.PUDDLE_TICKS -->. Get out of the ring during the
  wind-up and do not fight standing in the puddle.
- **Backstep.** Stand close behind her and she turns to you and hops away. It does no damage, but it ends your combo.
  Hit her from the side, not straight from behind.

### Phase 2 (below half health)

- **Veil burn.** Her veil catches fire. For two seconds <!-- FallenSisterEntity.VEIL_BURN_TICKS --> she stands still
  and takes no damage at all: do not waste stamina on her, back off and get ready.
- **Crying souls.** Three souls circle her head <!-- FallenSisterEntity.FIRST_FLOCK -->. One at a time, a soul rears
  back with a shriek while a line of soul fire marks the ground from it to where you stand, then dives along that line.
  Step sideways off the line. After the dive the soul hangs dazed near the floor for two seconds
  <!-- CryingSoulEntity.DAZED_TICKS -->; any hit kills it. Once all souls are gone, a new flock of four
  <!-- FallenSisterEntity.LATER_FLOCK --> comes about every 20 seconds <!-- FallenSisterEntity.FLOCK_INTERVAL -->.
- **Censer spin.** A ring of flame around her, then she swings the censer around her on its full chain. It is her
  hardest hit and throws you back. Leave the ring the moment you see it. She uses it more often when two or more
  players stand close to her.
- Arcs and the slam stay; she slams more often than before.

The braziers around the room light one by one as she loses health. They burn on for good once she is beaten.

## Drops

{{drops}}

She has no loot table of her own. Everything comes straight into the inventory of each player who is in the fight
when she dies:

- On **every** clear, every player in the fight gets a {{item embervoid:crying_soul}}, even if they have beaten another
  Sister before. Use it on a Cleft Stele to light the way to the [Ember Sanctum](../places/ember-sanctum.md).
- On your **first** clear you also get a {{item embervoid:flask_shard}} and one Sanctum weapon, the
  {{item embervoid:sanctum_longsword}} or the {{item embervoid:ashbearers_mace}}, plus a few rolls of iron, gold, books,
  experience bottles, golden apples and, rarely, a diamond.

## Strategy

**Melee.** Stay at her side, just outside the arc cone. After the second arc or a slam she has a short recovery: two or
three hits, then step out. Keep a sword hit ready for each dazed soul in phase 2; killing them fast keeps the dives
rare.

**Ranged.** A bow works well in phase 1, as her moves only reach a few blocks. Keep moving around the room so she
walks rather than strikes. In phase 2 the souls chase you anyway, so close in and kill them as they come out of a dive.

**In a party.** She gets more health for each extra fighter but no adds. Spread out: the spin punishes a group that
stacks next to her. Let one player hold her attention from the front while the others hit her sides, and give one
player the job of killing the souls. Everyone in the fight gets a soul, so bring whoever still needs one.
