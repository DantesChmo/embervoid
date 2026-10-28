---
entity: embervoid:ash_colossus
kind: boss
---

{{stats}}

## How to reach it

The Ash Colossus is the guardian of the [Ashen Fields](../places/ashen-fields.md). Its arena is an old market square
sunk into a crater, ringed by ten burnt granary pillars <!-- ReachArenas.PILLAR_COUNT -->, some hundreds of blocks out
from the [Crossroads](../places/crossroads.md). Follow the Fields road from the plaza to its end, or let the
{{item embervoid:ember_lodestar}} point the way.

An [Ember Shrine](../blocks/functional/ember-shrine.md) and a chest stand just outside the fog wall, on the side facing
the Crossroads. The shrine stays cold until you have beaten the boss, so rest at the last shrine on the road before
you go in.

Step into the crater and the Colossus rises. A fog wall closes the arena until the fight ends. If you die or leave, it
is gone, and it comes back at full health the next time you step in.

## Phases and moves

Every move is marked first: a cone, a ring or a falling shadow on the ground.

Where you hit it matters more than anything else. From the front (within about 60° of its facing) it takes half
damage <!-- AshColossusEntity.FRONT_DAMAGE -->. From behind it takes half again more
<!-- AshColossusEntity.BACK_DAMAGE -->, and from its sides normal damage. Fight from behind.

### Phase 1

- **Fist.** A cone in front of it. The fist comes down and knocks you away. Step out of the cone to the side.
- **Arm drag.** A wider, low cone. The sweep pulls everyone it catches towards the Colossus, into reach of its next
  move. Leave the cone the same way.
- **Leap.** At a target 8 to 22 blocks away it crouches while a ring of flame marks your spot, then jumps and lands
  there. Run out of the ring; it lands where it marked, not where you go.
- **Boulder.** It rips up stone and throws it. A shadow marks where it falls. It also throws at anyone standing far
  away, so distance is no shelter.
- **Ash storm.** About every 20 seconds <!-- AshColossusEntity.STORM_INTERVAL --> it stops and roars. Then for six
  seconds <!-- AshColossusEntity.STORM_TICKS --> everyone it can see takes steady magic damage and goes blind. Break
  line of sight: put a granary pillar or any solid block between you and its chest before the roar ends.
- **Pillar smash.** If you hide behind a pillar while it is close, it smashes the pillar: a cone, then the whole column
  of stone is gone. The pillars come back after the fight, not during it. Do not hide behind the same pillar twice if
  you can help it, and keep some standing for the later storms.

### Phase 2 (below half health)

It sheds its stone plates. The ember core in its back is bare now, so hits from behind deal double damage
<!-- AshColossusEntity.CORE_DAMAGE -->. Its armor drops, but it walks faster and picks its moves more quickly.

- **Cinders.** Every few seconds <!-- AshColossusEntity.CINDER_INTERVAL --> burning cinders fall near each fighter, each
  marked by a shadow. A cinder sets you on fire. Keep moving.
- All phase 1 moves stay, including the ash storm.

## Drops

{{drops}}

On your first kill, these come straight into your inventory as well:

- the {{item embervoid:ash_colossus_soul}}, which goes into the {{item embervoid:ashen_scythe}};
- a {{item embervoid:flask_shard}}.

If you help a friend who still needs the boss after you have beaten it, the soul sometimes answers you again
<!-- boss.repeatSoulChance -->. A fight started with {{item embervoid:ash_of_rematch}} gives the loot table and XP only,
never the soul.

## Strategy

**Melee.** Circle to its back and stay there: it turns slowly and its moves aim where it faces when the wind-up
starts. Every arm drag or fist you dodge is a window for two or three hits on its back. Keep an eye on the storm
timer and be near a pillar when the roar comes.

**Ranged.** From range it leaps and throws boulders at you, and both are slow to land. Shoot, then move as soon as a
ring or shadow appears under you. Arrows that hit its front do half damage, so work around to its side. When the storm
comes, a pillar between you and it is enough.

**In a party.** It has more health for each extra fighter and brings one [Cinder Husk](../mobs/ashen-reach/cinder-husk.md)
for each fighter beyond the first. The Colossus switches targets every few seconds, often to whoever hurt it most.
Use that: one player draws it to face away while the others strike the core. In phase 2 the cinders fall on everyone,
so do not bunch up.
