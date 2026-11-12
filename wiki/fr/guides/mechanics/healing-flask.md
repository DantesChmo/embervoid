---
title: Fiole de soin
kind: mechanic
order: 4
source: 553912f4f63c
---

La {{item embervoid:healing_flask}} est votre moyen de vous soigner en combat. Vous en recevez une la première fois que
vous rejoignez un monde.

## Charges

Les charges vous appartiennent, à **vous**, pas à la bouteille. La fiole ne fait que les afficher (« Charges : 3/3 ») et
les dépenser. Une seconde fiole n'apporte rien ; elle partage les mêmes charges.

Vous commencez avec 3 charges <!-- config flask.baseCharges -->, et les Éclats de fiole les portent jusqu'à 8
<!-- config flask.maxCharges -->.

## Boire

Maintenez le clic droit pour boire. Commencer une gorgée dépense une charge ; le soin arrive ensuite régulièrement
pendant que vous buvez, 8 points de santé (4 cœurs) <!-- config flask.healAmount --> en environ 1,6 seconde
<!-- config flask.drinkTicks: 32 ticks -->. Arrêtez plus tôt et vous perdez le reste de cette gorgée. Après chaque gorgée,
il y a une courte pause avant la suivante.

Vous **ne pouvez pas boire en sprintant**. Arrêtez-vous, buvez, repartez.

Le soin augmente avec :

- l'**Ardeur** : +2 % par point entier,
- la **Braise de fiole** (voir plus bas) : +15 % <!-- config gear.consumables.flaskEmberBonus -->.

## Remplir

Vos charges reviennent au complet quand vous :

- **vous reposez à un Autel de braise** (clic droit dessus),
- **dormez pendant la nuit** dans un lit (se lever tôt ne compte pas, et les nuits de lune ne vous laissent pas dormir),
- **réapparaissez** après une mort.

## Éclats de fiole

Utilisez un {{item embervoid:flask_shard}} sur un Autel de braise : votre fiole gagne une charge pour de bon, et la
nouvelle charge arrive remplie. Au plafond, l'autel le refuse. Les éclats proviennent :

- des coffres de la salle extérieure des Sanctuaires en ruine,
- de votre première victoire dans un Sanctuaire en ruine,
- de la première mise à mort de chaque boss de zone des Marches cendrées,
- de la victoire sur une vague de Spectres de pleine lune,
- du Vampire (deux à votre première mise à mort, parfois un ensuite).

## La Braise de fiole

La {{item embervoid:flask_ember}} est le cadeau de première victoire sur le Seigneur rougeoyant. Utilisez-la une fois et
chaque gorgée soigne davantage, pour de bon. Une fiole ne peut contenir qu'une seule braise.

## Perdre sa fiole

Vous ne pouvez pas la perdre en mourant : la fiole n'est jamais lâchée et ne va jamais dans votre Vestige de cendres ;
elle reste dans son emplacement. Si vous la jetez ou qu'elle brûle, fabriquez-en une autre avec une bouteille en verre,
un diamant et un charbon ou un charbon de bois (sans forme). Vos charges restent les vôtres.

## Réglages

{{config flask.baseCharges}}
{{config flask.maxCharges}}
{{config flask.healAmount}}
{{config flask.drinkTicks}}

Suite : [Mort et Vestige de cendres](death-and-remnant.md)
