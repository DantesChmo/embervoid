---
entity: embervoid:smoldering_lord
kind: boss
---

{{stats}}

## How to reach it

The Smoldering Lord is the realm boss of [Ashen Reach](../places/ashen-reach.md): the greedy king, three and a half
blocks tall in burning armor, with a greatsword. He waits in the courtyard below the keep wall, at the heart of the
[Smoldering Citadel](../places/smoldering-citadel.md).

To get there you must first beat the three zone guardians: the [Ash Colossus](ash-colossus.md), the
[Grieving Tree](grieving-tree.md) and the [Crypt Keeper](crypt-keeper.md). Then the
[Citadel Gate](../blocks/functional/citadel-gate.md) on the south side of the [Crossroads](../places/crossroads.md)
lets you through. Follow the gate road south through the burning city: the outskirts, the street and the market, the
gatehouse in the city wall, then the inner ring. The courtyard lies about 300 blocks from the landing, in front of the
keep. The first Citadel shrine stands where the gate road ends; another stands outside the courtyard's fog wall and
stays cold until you have beaten him.

Step into the courtyard and the fight begins. A fog wall closes it, and his own music plays. If you die or leave, the
fight resets.

## Phases and moves

### Phase 0: on the wall

He starts high on the keep wall with two [Sparking Marksmen](../mobs/ashen-reach/sparking-marksman.md) beside him.
Up there he cannot be hurt. Every three seconds <!-- SmolderingLordEntity.WALL_ATTACK_INTERVAL --> he attacks:

- **Mortar rings.** A ring marks the ground under up to two fighters; two seconds later
  <!-- SmolderingLordEntity.MORTAR_WINDUP --> fire falls there and sets you alight. Keep moving and you are never
  under it.
- **Flame line.** A line runs from the foot of the wall across the whole courtyard towards one fighter, then bursts
  into fire and keeps burning for a few seconds. Step off it and do not cross it while it burns.

Three **signal braziers** stand on pedestals in the courtyard. Light all three (flint and steel, a fire charge or a
burning arrow) and he comes down: a wide ring <!-- SmolderingLordEntity.LANDING_RADIUS --> marks the
middle of the courtyard, and he lands there with a blast. Do not stand in it.

### Phase 1

- **Greatsword combo.** Two swings in a cone in front of him that set you on fire, then a slam on a ring just ahead of
  him. The slam goes through a raised shield and breaks it. Back out of the cone, then out of the ring.
- **Lunge.** At a target further away he lunges along a marked line and leaves a trail of fire on it. Step off the
  line, and do not walk through the trail.

The Marksmen stay on the wall the whole fight. Shoot them down or keep a pillar between you and them.

### Phase 2 (below half health)

His armor bursts off. He takes a quarter more damage <!-- SmolderingLordEntity.SHATTERED_DAMAGE --> from now on, but he
moves and attacks faster. About every 25 seconds <!-- SmolderingLordEntity.HAZARD_INTERVAL --> he calls down one
zone guardian's hazard, in turn:

1. **The Colossus's ash storm.** Six seconds of damage and blindness to everyone he can see. Break line of sight: a
   pedestal or a wall is enough.
2. **The Grove's fog.** A thick red-brown fog for twenty seconds <!-- SmolderingLordEntity.FOG_TICKS -->. Keep track of
   where he is before it closes in.
3. **The Necropolis's darkness.** Darkness on everyone for fifteen seconds <!-- SmolderingLordEntity.DARK_TICKS -->.
   While it lasts he takes half damage unless he stands in light 8 or more <!-- SmolderingLordEntity.LIT_LEVEL -->. The
   signal braziers still burn: pull him next to one.

The combo and the lunge stay.

## Drops

{{drops}}

On your first kill, these come straight into your inventory as well:

- the {{item embervoid:smoldering_lord_soul}}, which goes into the {{item embervoid:royal_ash_rapier}};
- a {{item embervoid:flask_ember}}.

Beating him completes Ashen Reach for every player in the fight: the realm's obelisk in the
[Ember Sanctum](../places/ember-sanctum.md) is kindled, and the next obelisk lights up (the next realm is not built
yet). Helping a friend later can bring the soul again now and then <!-- boss.repeatSoulChance -->; a fight started
with {{item embervoid:ash_of_rematch}} never does.

## Strategy

**Phase 0.** Do not stand still. Light the nearest brazier first, then the others, moving between the marks. A bow with
burning arrows lights them from where you stand. If you can, kill the Marksmen from below while you are at it; they
keep shooting for the rest of the fight.

**Melee.** Stay at his side and dodge out of each combo, then hit him during the recovery after the slam. When he lunges,
step sideways and punish the end of it. In phase 2, fight him next to a signal brazier so the darkness does not halve
your damage, and keep a pedestal close for the ash storm.

**Ranged.** Keep him at mid range and he lunges, which is easy to read but leaves fire behind; move the fight around the
courtyard so you do not box yourself in with trails. Shoot the Marksmen early.

**In a party.** He has more health for each extra fighter and brings one
[Cinder Hound](../mobs/ashen-reach/cinder-hound.md) for each fighter beyond the first. In phase 0 split up: each player
takes a brazier. Later, one player holds him while the others hit his sides; spread out for the mortar and the storm.
