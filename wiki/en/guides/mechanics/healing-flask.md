---
title: Healing Flask
kind: mechanic
order: 4
---

The {{item embervoid:healing_flask}} is how you heal in a fight. You get one the first time you join a world.

## Charges

The charges belong to **you**, not to the bottle. The flask only shows them ("Charges: 3/3") and spends them. A second
flask adds nothing; it shares the same charges.

You start with 3 charges <!-- config flask.baseCharges -->, and Flask Shards raise that up to 8
<!-- config flask.maxCharges -->.

## Drinking

Hold right-click to drink. Starting a sip spends a charge; the heal then comes in evenly while you drink, 8 health
(4 hearts) <!-- config flask.healAmount --> over about 1.6 seconds <!-- config flask.drinkTicks: 32 ticks -->. Stop
early and you lose the rest of that sip. After each sip there is a short pause before the next.

You **cannot drink while sprinting**. Stop, drink, move on.

The heal grows with:

- **Kindling**: +2 % per full point,
- the **Flask Ember** (below): +15 % <!-- config gear.consumables.flaskEmberBonus -->.

## Refilling

Your charges come back in full when you:

- **rest at an Ember Shrine** (right-click it),
- **sleep through the night** in a bed (getting up early does not count, and moon nights do not let you sleep),
- **respawn** after dying.

## Flask Shards

Use a {{item embervoid:flask_shard}} on an Ember Shrine: your flask gains one charge for good, and the new charge
comes filled. At the cap the shrine refuses it. Shards come from:

- Ruined Sanctum outer-hall chests,
- your first clear of a Ruined Sanctum,
- the first kill of each Ashen Reach zone boss,
- beating a full moon Ghost wave,
- the Vampire (two on your first kill, sometimes one after).

## The Flask Ember

The {{item embervoid:flask_ember}} is the Smoldering Lord's first-kill gift. Use it once and every sip heals more,
for good. A flask can hold only one ember.

## Losing your flask

You cannot lose it by dying: the flask never drops and never goes into your Ash Remnant; it stays in its slot. If you
throw it away or it burns, craft another from a glass bottle, a diamond and a coal or charcoal (shapeless). Your
charges are still yours.

## Settings

{{config flask.baseCharges}}
{{config flask.maxCharges}}
{{config flask.healAmount}}
{{config flask.drinkTicks}}

Next: [Death and the Ash Remnant](death-and-remnant.md)
