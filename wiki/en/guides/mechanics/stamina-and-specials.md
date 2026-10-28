---
title: Stamina and special attacks
kind: mechanic
order: 3
---

Embervoid's stamina has one job: paying for your weapon's special attack. Normal swings, sprinting, jumping and
blocking with a shield stay free, as in vanilla.

## The stamina bar

You start with 100 stamina <!-- config stamina.max -->. The bar sits above the hotbar and hides itself while it is
full. After you spend stamina, it waits a second <!-- config stamina.regenDelayTicks: 20 ticks --> and then refills
quickly, 20 per second at the start <!-- config stamina.regenPerTick: 1 per tick -->.

**Endurance** raises both: each full point gives +2 max stamina and +2 % regeneration.

## The rules of a special attack

Every weapon with a special attack shows it in its tooltip: "Right-click: name", then its stamina cost and cooldown.

- **Right-click** with the weapon to use it.
- It costs a **fixed amount of stamina**, but you can use it as long as you have **any stamina left**: the bar drops
  to zero and stops there. With an empty bar you get "Too exhausted", the bar flashes and nothing happens.
- After use, the weapon has a short **cooldown**, shown on the item like an ender pearl's.
- A special hits through normal damage: the target's armor, its level and your
  Strength or Dexterity scaling all apply.

So the cost is what limits you: two or three specials in a row empty the bar, and then you fight with plain swings
while it refills.

## The specials

| Weapon | Special | What it does | Stamina | Cooldown |
|---|---|---|---|---|
| {{item embervoid:sanctum_longsword}} | Wide Cleave | hits everything in a 90° arc in front of you | 35 | 0.4 s |
| {{item embervoid:ashbearers_mace}} | Ground Slam | a ring around you: damage, knockback and Slowness II | 45 | 0.5 s |
| {{item embervoid:ashen_scythe}} | Harvest | a full spin hitting everything around you; each enemy caught gives stamina back | 40 | 0.5 s |
| {{item embervoid:wardens_scourge}} | Lash | pulls the first enemy in reach toward you | 30 | 0.4 s |
| {{item embervoid:royal_ash_rapier}} | Lunge | a dash forward ending in a thrust that bleeds twice and burns | 30 | 0.3 s |

<!-- config sanctumGear.cleave*, sanctumGear.slam*, gear.weapons.harvest*, lash*, lunge* -->

The {{item embervoid:huntsmans_crossbow}} has no special; its strength is its bolts. The weapons' other passives are
on their encyclopedia pages.

## Tips

- Open with the special, then keep swinging while the bar refills.
- The Ashen Scythe pays for itself in a crowd: every enemy a sweep or Harvest catches returns 3 stamina
  <!-- config gear.weapons.sweepStamina -->.
- Ground Slam is your way out when you are surrounded; Lash pulls an archer or a caster off its perch.

## Settings

{{config stamina.max}}
{{config stamina.regenPerTick}}
{{config stamina.regenDelayTicks}}
{{config sanctumGear.cleaveStamina}}
{{config sanctumGear.slamStamina}}
{{config gear.weapons.harvestStamina}}
{{config gear.weapons.lashStamina}}
{{config gear.weapons.lungeStamina}}

The stamina bar can be kept visible on your client with `staminaBar.hideWhenFull` in `embervoid-client.json`.

Next: [Healing Flask](healing-flask.md)
