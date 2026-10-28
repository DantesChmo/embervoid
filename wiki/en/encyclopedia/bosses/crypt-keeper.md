---
entity: embervoid:crypt_keeper
kind: boss
---

{{stats}}

## How to reach it

The Crypt Keeper is the guardian of the [Sunken Necropolis](../places/sunken-necropolis.md): four blocks of robe and
bone with a halberd in one hand and a lantern in the other. Its arena is a deep sinkhole; a switchback stair runs down
the wall into a flooded ossuary, half roofed over, half open to the sky. Follow the Necropolis road from the
[Crossroads](../places/crossroads.md) to its end, or let the {{item embervoid:ember_lodestar}} point the way.

An [Ember Shrine](../blocks/functional/ember-shrine.md) and a chest stand outside the fog wall on the rim side facing
the Crossroads. The shrine stays cold until you have beaten the Keeper.

The ossuary floor has a raised dais in the middle, a ring walkway, a cross of walkways and four pedestals on the
diagonals, each with an unlit brazier on top. Remember where they are: the fight is about them.

Walk down into the ossuary and the Keeper appears. A fog wall closes the arena until the fight ends; if you die or
leave, the fight resets and every brazier and drop of water goes back to how it was.

## Phases and moves

Every move is marked first. Its halberd marks are soul-blue; the one red line is the overhead you cannot block.

### Phase 1

- **Halberd combo.** Two quick swings in a cone in front of it, then an overhead blow along a red line. The overhead is
  **unblockable**: it goes through a raised shield and breaks it. Back out of the cone for the swings and step off the
  line for the overhead.
- **Sweep.** A ring around it, then the halberd sweeps all the way round and throws you back. It sweeps more often when
  two or more players stand close. Step out of the ring.
- **Lantern throw.** At a target up to 16 blocks away it throws its lantern's fire. A shadow marks the spot; the hit
  sets you on fire and a pool of flame burns there for five seconds <!-- CryptKeeperEntity.lanternThrow, 100 ticks -->.

### Phase 2 (below 60 % health)

Its lantern shatters.

- **Darkness.** Everyone in the fight gets Darkness for as long as the phase lasts.
- **Water.** The ossuary floods one block deep.
- **Half damage in the dark.** It now takes only half damage <!-- CryptKeeperEntity.DARK_DAMAGE --> unless it stands
  in light: block light 8 or more <!-- CryptKeeperEntity.LIT_LEVEL --> where it stands, or a player within a few blocks
  <!-- CryptKeeperEntity.LANTERN_REACH --> holding an {{item embervoid:ember_lantern}} lit with
  {{item embervoid:amber_lantern_oil}} in the offhand.
- **The braziers.** Now you can light the four braziers on the pedestals: flint and steel, a fire charge or a burning
  arrow. Fight it next to a lit brazier and it takes full damage.
- **Crypt Moths.** While any brazier burns, [Crypt Moths](../mobs/ashen-reach/crypt-moth.md) come every 15 seconds
  <!-- CryptKeeperEntity.MOTH_INTERVAL -->, up to four at once <!-- CryptKeeperEntity.MAX_MOTHS -->. They fly straight
  at the nearest lit brazier and smother it. Kill them before they get there, or light it again.
- **Thrust.** The lantern throw is gone. Instead, at range it lunges along a line and carries itself forward with the
  thrust. Step off the line.
- The combo and the sweep stay, and it attacks faster.

### Phase 3 (below 30 % health)

The water rises to two blocks; only the walkways, the dais and the pedestals stay dry. The bell tolls and
[Restless Bones](../mobs/ashen-reach/restless-bones.md) rise from the water: three, plus one for each fighter beyond
the first <!-- CryptKeeperEntity.RISEN_BONES -->. Darkness, braziers and moths go on as in phase 2.

## Drops

{{drops}}

On your first kill, these come straight into your inventory as well:

- the {{item embervoid:crypt_keeper_soul}}, which goes into the {{item embervoid:wardens_scourge}};
- a {{item embervoid:flask_shard}}.

Helping a friend later can bring the soul again now and then <!-- boss.repeatSoulChance -->; a fight started with
{{item embervoid:ash_of_rematch}} never does.

## Strategy

**Before the fight.** Bring flint and steel or fire charges, and if you have them, an Ember Lantern with Amber Lantern
Oil to hold in your offhand. Light is the whole fight from phase 2 on.

**Melee.** In phase 1, bait the combo, back out of the two swings, sidestep the red line and hit it during the
recovery. When the lantern shatters, run to the nearest pedestal, light its brazier and pull the Keeper to you; fight
it there. Kill each moth that comes for your brazier. In phase 3 stay on the walkways: the water slows you and hides
the Bones.

**Ranged.** Burning arrows light the braziers from afar. Keep your distance in phase 1 and watch for the lantern's
shadow. From phase 2 on its thrust closes the gap fast, so ranged play means a lot of sidestepping. Arrows are good
against the moths.

**In a party.** It has more health per extra fighter, brings one [Restless Bones](../mobs/ashen-reach/restless-bones.md)
for each fighter beyond the first when it appears, and raises more of them in phase 3. Split the jobs: one or two players keep the Keeper next to a lit brazier, another keeps the braziers lit and
shoots the moths. Do not crowd it, or it sweeps.
