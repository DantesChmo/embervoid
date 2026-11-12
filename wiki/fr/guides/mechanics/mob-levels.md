---
title: Niveaux des monstres et difficulté
kind: mechanic
order: 2
source: 27bec0020add
---

Chaque monstre hostile, vanilla ou issu d'un autre mod, reçoit un niveau une seule fois, quand il entre pour la première
fois dans le monde. Regardez un monstre : son nom affiche « Niv. N ».

## Comment est choisi le niveau d'un monstre

Dans l'Overworld, le Nether et l'End, le niveau est le **niveau de base** du type de monstre plus des bonus :

| Niveau de base | Monstres |
|---|---|
| 3 | zombie, villageois zombie, zombie momifié, noyé, araignée |
| 4 | squelette, vagabond, slime, et tout hostile non listé |
| 5 | Goule de cendre |
| 6 | creeper, sorcière |
| 8 | Feu follet de braise |
| 12 | enderman |
| 15 | Carcasse |

<!-- config mobs.levels.baseLevels, mobs.levels.defaultBaseLevel -->

| Bonus | Niveaux |
|---|---|
| Overworld : tous les 1000 blocs depuis le point d'apparition du monde | +1, jusqu'à +5 |
| Overworld : sous y = 0 | +3 |
| Le Nether | +5 |
| L'End | +8 |
| Overworld pendant une nuit de pleine lune | +3 |

<!-- config mobs.levels.blocksPerLevel, maxDistanceBonus, belowZeroBonus, dimensionBonus, moon.fullMoon.levelBonus -->

Dans un domaine, le niveau d'un monstre vient plutôt de la zone où il apparaît : aux Marches cendrées, 15–18 dans les
Champs cendrés, 18–21 dans les Bois royaux, 21–25 dans la Nécropole engloutie et 25–30 dans la Citadelle rougeoyante. Les
boss ont des niveaux fixes.

L'Ender Dragon, le Wither, le Warden et l'Elder Guardian n'ont pas de niveau et restent comme dans vanilla.

## Ce qu'un niveau change

Pour chaque niveau au-dessus de 1, un monstre gagne :

- +6 % de santé,
- +5 % de dégâts (mêlée, flèches, explosions et potions confondues),
- +6 % d'expérience à sa mort.

<!-- config mobs.levels.healthPerLevel / damagePerLevel / experiencePerLevel -->

Un monstre de niveau 11 a donc 60 % de santé en plus et frappe 50 % plus fort qu'au niveau 1. Certains types vanilla sont
d'abord réduits pour ne pas dépasser les autres : les zombies et les zombies momifiés ont moins de santé de base, les
sorcières et les endermen moins de dégâts de base.

## Difficulté

Embervoid remplace le facteur de difficulté de vanilla sur les dégâts que les monstres hostiles infligent aux joueurs :

| Difficulté | Embervoid | Vanilla |
|---|---|---|
| Facile | ×1,0 | environ ×0,5 |
| Normal | ×1,25 | ×1,0 |
| Difficile | ×1,75 | ×1,5 |

<!-- config mobs.difficulty.* -->

Le mode Facile frappe désormais comme le Normal de vanilla. Le facteur s'applique avant le bonus de niveau.

## Réglages

{{config mobs.levels.enabled}}
{{config mobs.levels.baseLevels}}
{{config mobs.difficulty.normal}}

Les monstres d'autres mods reçoivent le niveau de base par défaut ; donnez-leur le leur dans `mobs.levels.baseLevels`.
L'étiquette de niveau des monstres peut être masquée sur votre client avec `hud.showMobLevel` dans
`embervoid-client.json`. Toutes les clés sont sur [Configuration](../configuration.md).

Suite : [Endurance et attaques spéciales](stamina-and-specials.md)
