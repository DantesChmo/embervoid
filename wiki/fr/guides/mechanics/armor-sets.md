---
title: Ensembles d'armure
kind: mechanic
order: 9
source: 09a1ad705725
---

Les Marches cendrées comptent quatre ensembles d'armure et quatre pièces uniques. Un ensemble ne donne un bonus que si
vous portez ses **quatre** pièces ; l'infobulle de chaque pièce indique « Ensemble complet : … ». Les pièces uniques ont
un effet propre et ne comptent jamais pour un ensemble.

## Les ensembles

| Ensemble | Provenance | Armure | Bonus d'ensemble complet |
|---|---|---|---|
| [Armure rougeoyante](../../encyclopedia/items/armor/smoldering-set.md) | fabriquée (forge) | comme la netherite | les brûlures ne vous blessent pas (la lave, si) |
| [Ensemble du moissonneur](../../encyclopedia/items/armor/reapers-set.md) | monstres des Champs cendrés | légère, environ le fer | +10 % de vitesse de déplacement |
| [Ensemble du veneur](../../encyclopedia/items/armor/huntsmans-set.md) | monstres des Bois royaux | un peu plus, un peu de résistance | +15 % de dégâts de projectiles |
| [Ensemble du geôlier](../../encyclopedia/items/armor/wardens-set.md) | monstres de la Nécropole engloutie | lourde, comme le diamant ; chaque pièce vous ralentit légèrement | +0,3 de résistance au recul, −15 % de dégâts en parant avec un bouclier |

<!-- config gear.sets.*, gear.smoldering.fireReductionPerPiece; armor values GearArmor -->

L'**Armure rougeoyante** fonctionne aussi pièce par pièce : chaque pièce portée réduit les dégâts de feu de 8 %. Fabriquez-la
à l'établi de forge à partir d'un {{item embervoid:smoldering_template}}, d'une pièce d'armure en diamant et d'un
{{item embervoid:smoldering_ingot}}.

Les trois ensembles de zone sont lâchés par les monstres de leur zone, une pièce aléatoire à la fois, et seulement quand
c'est un joueur qui porte le coup fatal. Les ennemis placés plus coriaces les lâchent plus souvent que les apparitions
naturelles, et le coffre au trésor de chaque zone peut aussi contenir une pièce.

## Pièces uniques

| Pièce | Emplacement | Effet | Provenance |
|---|---|---|---|
| {{item embervoid:scarecrow_mask}} | tête | les monstres vous remarquent à moitié moins loin ; les Traqueurs de cendre ne vous prennent pas en embuscade | Champs cendrés |
| {{item embervoid:huntsman_greaves}} | jambes | +20 % de vitesse pendant 4 s après une mise à mort ; pas de dégâts de chute jusqu'à 7 blocs | Bois royaux |
| {{item embervoid:drowned_sabatons}} | pieds | pleine vitesse dans l'eau | Nécropole engloutie |
| {{item embervoid:furnace_heart_cuirass}} | torse | sous 30 % de santé, une nova de feu brûle et repousse les ennemis autour de vous (une fois par 60 s) | Citadelle rougeoyante |

<!-- config gear.uniques.* -->

Ce sont des butins rares des monstres de leur zone, plus probables sur l'élite de la zone (le Charognard cendré, la
Carcasse d'écorce, le Sonneur des tombes). Dans la Citadelle, seuls le Golem fournaise et le Chevalier rougeoyant lâchent
la cuirasse.

## Mélanger les pièces

- Un bonus d'ensemble demande les quatre pièces du même ensemble. Trois pièces et une pièce unique ne donnent aucun bonus
  d'ensemble.
- Une pièce unique dans un emplacement est un compromis : son effet contre le bonus d'ensemble des trois autres.
- Les pièces de l'Armure rougeoyante réduisent les dégâts de feu une à une : un plastron rougeoyant avec trois autres
  pièces aide donc déjà contre le feu.
- Enchantez-les à votre guise ; elles acceptent les enchantements d'armure habituels.

## Réglages

{{config gear.sets.reaperSpeed}}
{{config gear.sets.huntsmanProjectile}}
{{config gear.sets.wardenKnockbackResist}}
{{config gear.smoldering.fireReductionPerPiece}}

Suite : [Effets de statut](status-effects.md)
