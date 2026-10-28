---
entities: [embervoid:grieving_tree, embervoid:tree_lantern]
kind: boss
---

{{stats}}

## How to reach it

The Grieving Tree is the guardian of the [Royal Woods](../places/royal-woods.md): the dead king's oak, about ten blocks
tall, with lanterns hanging from its branches. It stands in the Royal Grove, a walled garden behind the hunting lodge,
some hundreds of blocks from the [Crossroads](../places/crossroads.md). Follow the Woods road to its end, or let the
{{item embervoid:ember_lodestar}} point the way. The garden wall has its gate on the side facing the Crossroads.

An [Ember Shrine](../blocks/functional/ember-shrine.md) and a chest stand just outside the fog wall. The shrine stays
cold until you have beaten the Tree.

Step into the grove and the fight begins: the fog wall closes and a grey-green fog fills the arena. If you die or
leave, the fight resets.

## Phases and moves

**Its bark turns every blow.** Swords, axes and arrows on the trunk do nothing; you hear a dull knock. The only way to
hurt it is to break its lanterns first.

### The lanterns

Seven lanterns <!-- GrievingTreeEntity.LANTERNS --> hang in its branches. Each one breaks to:

- any projectile: an arrow, a crossbow bolt, a thrown trident;
- a melee hit, but only on the lantern that hangs low after a branch sweep (see below).

Each broken lantern makes the tree flinch. Break the **last** one and the tree is stunned for ten seconds
<!-- GrievingTreeEntity.STUN_TICKS -->: it stops attacking, the fog lifts, and the trunk takes extra damage
<!-- GrievingTreeEntity.STUNNED_DAMAGE -->. That is your window: run in and hit the trunk with everything you have. When
the stun ends, a whole new set of lanterns hangs at once.

### Phase 1 (rooted)

It never moves from its spot.

- **Root lines.** The ground shakes and a line of roots marks the floor from its base towards every fighter, then
  bursts up along it. A hit knocks you up and slows you hard for two seconds. Step sideways off your line.
- **Branch sweep.** A wide cone in front of it, up to about eight blocks out. A big branch sweeps the ground and throws
  you back. Afterwards the lantern nearest the sweep hangs low, within sword reach, for three seconds
  <!-- GrievingTreeEntity.LOWERED_TICKS -->. Dodge the sweep, then strike that lantern.
- **Withered Thralls.** Every 25 seconds or so <!-- GrievingTreeEntity.THRALL_INTERVAL --> two
  [Withered Thralls](../mobs/ashen-reach/withered-thrall.md) drop from its branches. Kill them quickly; they stop
  coming while the tree is stunned.

### Phase 2 (below half health)

It tears its roots free and walks, slowly, after you. The old lanterns fall; six new ones
<!-- GrievingTreeEntity.P2_LANTERNS --> hang lower on its own branches and move with it. The fog grows thicker. The bark
still turns every blow outside a stun.

- **Root cage.** A ring around you marks the spot; roots close in and hold whoever is inside nearly still for three
  seconds. Leave the ring before it closes.
- Root lines, the branch sweep and the Thralls stay.

## Drops

{{drops}}

On your first kill, these come straight into your inventory as well:

- the {{item embervoid:grieving_tree_soul}}, which goes into the {{item embervoid:huntsmans_crossbow}};
- a {{item embervoid:flask_shard}}.

Helping a friend later can bring the soul again now and then <!-- boss.repeatSoulChance -->; a fight started with
{{item embervoid:ash_of_rematch}} never does.

## Strategy

**Ranged.** This fight is made for a bow or crossbow. Bring plenty of arrows. Stand back, out of sweep range, and shoot
the lanterns one after another while sidestepping the root lines. When the last one breaks, close in for the stun, or
keep shooting the trunk if your bow hits harder than your sword. In phase 2 the lanterns hang lower and walk with the
tree, so they are easier to reach but you have less room.

**Melee.** Possible, but slow. Stay in front of it to bait the branch sweep, dodge it, and break the lowered lantern.
One lantern per sweep means a long fight; carry at least a few arrows or a trident for the last lanterns.

**In a party.** The tree has more health per extra fighter and brings one extra Withered Thrall for each fighter
beyond the first. Root lines come at every fighter, so spread out. Let the archers break lanterns while the others keep
the Thralls off them, then everyone hits the trunk together during the stun. Call out when the last lantern is about
to break.
