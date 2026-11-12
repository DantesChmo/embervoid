---
title: Caractéristiques et niveaux
kind: mechanic
order: 1
source: d42780e9bb0f
---

Vous avez sept caractéristiques. Chacune commence à 10, et votre niveau vaut 1 plus chaque point acheté : un personnage
neuf est de niveau 1 et se joue comme dans vanilla.

## Les caractéristiques

| Caractéristique | Abrév. | Chaque point au-dessus de 10 donne |
|---|---|---|
| Vitalité | VIT | +1 santé max (un demi-cœur) |
| Endurance | END | +2 endurance max, +2 % de régénération d'endurance, un peu de résistance au recul |
| Force | FOR | +2 % de dégâts avec les armes de Force : haches, Masse du porte-cendre, une part de la Faux cendrée et de l'Arbalète du veneur, la moitié pour les tridents |
| Dextérité | DEX | +2 % de dégâts avec les armes de Dextérité : épées, arcs, arbalètes, Épée longue du Sanctuaire, Fléau du geôlier, l'essentiel de la Rapière de cendre royale, une part de la Faux cendrée et de l'Arbalète du veneur, la moitié pour les tridents ; +0,5 % de vitesse d'attaque |
| Ardeur | ARD | +2 % de soin de la Fiole de soin, +2 % de dégâts de braise avec les armes de domaine qui en infligent |
| Résilience | RÉS | +0,1 de résistance de l'armure, −1 % de dégâts de feu (jusqu'à −50 %) |
| Chance | CHA | +0,1 de chance, +0,5 % de probabilité qu'un monstre que vous tuez lâche son butin une seconde fois (jusqu'à 25 %) |

<!-- per-point amounts: config stats.*; Kindling ember damage: gear.weapons.kindlingPerPoint -->

## Plafonds progressifs

Les points s'affaiblissent à mesure que la caractéristique grandit :

- de 11 à 20, chaque point compte pleinement ;
- de 21 à 40, pour moitié ;
- de 41 à 99, pour un cinquième.

<!-- StatCurve -->

Une caractéristique s'arrête à 99. Il n'y a pas de plafond de niveau par défaut.

## Ce que coûte un niveau

Les niveaux s'achètent avec des **points** d'expérience de vanilla (pas des niveaux de la barre d'expérience). Passer du
niveau L à L + 1 coûte 20 + 6·L + 0,15·L² points <!-- leveling.costBase / costLinear / costQuadratic --> : 26 au niveau
1, 200 au niveau 20, 695 au niveau 50.

## Où monter de niveau

Seule la [Vierge cendrée](../../encyclopedia/npcs/ashen-maiden.md) vous fait monter de niveau. Elle se tient dans le hub,
le [Sanctuaire des Braises](../walkthrough/ember-sanctum.md) (et dans les anciens cœurs de Sanctuaire des mondes créés
en 0.4.x). Faites un clic droit sur elle, puis cliquez sur **+** à côté d'une caractéristique. L'écran montre ce que
donne le point suivant avant que vous l'achetiez, ainsi que votre expérience et le coût. Éloignez-vous de plus de
quelques blocs et il se ferme.

Il n'y a pas de réattribution : un point acheté reste acquis.

## Les points qui ne comptent pas

Les artefacts portés dans le [Reliquaire](reliquary.md) ajoutent des points de caractéristique. Ils comptent pour tous
les effets, mais pas pour votre niveau, le coût du niveau ni le plafond de 99. L'écran de montée de niveau les affiche à
côté de chaque caractéristique.

## Réglages

{{config stats.vitalityHealth}}
{{config leveling.statCap}}
{{config leveling.levelCap}}

La liste complète est sur [Configuration](../configuration.md).

Suite : [Niveaux des monstres et difficulté](mob-levels.md)
