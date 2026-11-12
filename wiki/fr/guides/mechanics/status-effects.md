---
title: Effets de statut
kind: mechanic
order: 10
effect: embervoid:bleeding
source: f6f67b69063a
---

Embervoid ajoute pour l'instant un seul effet de statut : le **Saignement**.

## Saignement

Le Saignement inflige des dégâts chaque seconde et ignore l'armure. Il a trois niveaux :

| Niveau | Dégâts par seconde |
|---|---|
| I | 0,5 (un quart de cœur) |
| II | 1 |
| III | 1,5 |

<!-- config gear.bleeding.damagePerLevel, gear.bleeding.maxLevel -->

Chaque coup qui cause un Saignement l'élève d'un niveau, jusqu'à III, et relance ses 6 secondes <!-- config
gear.bleeding.durationTicks: 120 ticks -->. Continuez à frapper et il reste à pleine puissance ; arrêtez et il s'éteint
quelques secondes plus tard. Un monstre qui meurt de saignement compte comme votre mise à mort.

### Qui le cause

Le Saignement est votre arme, pas celle de vos ennemis :

- {{item embervoid:wardens_scourge}} : chaque coup ajoute un niveau.
- {{item embervoid:royal_ash_rapier}} : chaque coup ajoute un niveau, et sa Fente en ajoute deux d'un coup.

Certaines créatures n'ont pas de sang à perdre et ne saignent jamais : les Ossements sans repos, la Carcasse d'écorce, le
Golem fournaise, le Colosse de cendre et l'Arbre endeuillé. Utilisez autre chose contre eux.

En JcJ, ces armes font aussi saigner les autres joueurs.

### L'arrêter

- {{item embervoid:amber_balm}} guérit le Saignement et le Poison, puis vous soigne lentement.
- Un seau de lait le dissipe comme tout autre effet.
- Ou attendez qu'il passe : sans nouveaux coups, il s'achève au bout de 6 secondes.

## Réglages

{{config gear.bleeding.durationTicks}}
{{config gear.bleeding.maxLevel}}
{{config gear.bleeding.damagePerLevel}}

Suite : [Nuits de lune](moon-nights.md)
