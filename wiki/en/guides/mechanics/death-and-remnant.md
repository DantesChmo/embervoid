---
title: Death and the Ash Remnant
kind: mechanic
order: 5
entity: embervoid:ash_remnant
---

When you die, you do not scatter your things on the ground. They go into an **Ash Remnant** that waits where you fell.

## What goes into it

- all your **XP points**, exactly: your level and your bar come back as they were;
- all your **items**, including armor and the rings and artifacts worn in your Reliquary;
- but never your Healing Flask: it stays in its slot.

The remnant appears on the last solid ground you stood on, never in lava or in the void. Fall off a cliff or into the
void and it waits at the edge you fell from. Chat tells you where it lies: "Your Ash Remnant lies at …".

## Getting it back

Walk to it and **right-click** it. Everything comes back: items to their old slots where they are free, the rest into
your inventory or at your feet. Only you can take it; another player who tries is told whose ashes they are.

A remnant cannot be hurt, killed, blown up or lost to the void, and it does not despawn. It waits as long as it takes.

## Dying again

You have one remnant at a time. If you die before reaching it, by default:

- the **XP** in the old remnant burns away;
- its **items stay** in it until you pick them up;
- your new death leaves a new remnant with what you carried this time.

So run back for your XP before you take risks. Items are safe; XP is not.

## Where you wake

- **Overworld, Nether, End, the hub:** as in vanilla, at your bed or the world spawn.
- **In a realm:** at the last Ember Shrine you rested at in that realm, or at its landing. The Smoldering Citadel
  counts on its own. See [Ember Shrines and respawn](ember-shrines.md).

Your flask is full again when you respawn.

## Other modes

The server can change all of this:

| `remnant.mode` | What happens on death |
|---|---|
| `xp_and_items` (default) | XP and items go into the remnant |
| `xp` | XP goes into the remnant, items drop as in vanilla |
| `off` | vanilla death |

| `remnant.secondDeath` | What a new death does to the old remnant |
|---|---|
| `burn_xp` (default) | its XP burns, its items stay |
| `burn_all` | it burns with everything in it |

With the `keepInventory` game rule on, you keep your items and XP as in vanilla, and no remnant is left.

## Settings

{{config remnant.mode}}
{{config remnant.secondDeath}}

Next: [Ember Shrines and respawn](ember-shrines.md)
