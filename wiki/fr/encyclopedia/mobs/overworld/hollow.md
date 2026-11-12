---
entity: embervoid:hollow
kind: mob
source: c0c9fb0068dc
---

{{stats}}

## Où le trouver

Les Carcasses arpentent la surface de l'Overworld la nuit, rarement, et seulement à ciel ouvert. Elles arrivent seules.
Elles ne brûlent pas au soleil : une Carcasse que vous laissez en vie rôde donc encore le matin. Des Carcasses montent
aussi la garde dans les salles de chaque [Sanctuaire en ruine](../../places/ruined-sanctum.md) ; celles-là sont placées
à la génération du donjon.

## Comportement

Un soldat mort-vivant armé d'une épée en fer ou en pierre : lent, bien armuré et frappant fort. Tous ses dégâts
viennent de l'épée. Il attaque une fois toutes les deux secondes <!-- HollowEntity.ATTACK_INTERVAL_TICKS -->, et chaque
coup a une courte préparation : le bras part en arrière, et le coup n'atteint sa cible un instant plus tard que si vous
êtes encore à portée. Reculez quand vous voyez le bras partir en arrière, puis avancez et frappez.

Entre deux coups, il peut lever sa **garde**. Tant que la garde est levée, il marche à demi-vitesse, les coups de mêlée
reçus de face n'infligent qu'un cinquième de leurs dégâts <!-- HollowEntity.GUARDED_MELEE_FACTOR -->, et les flèches
venues de face ricochent. Les coups portés de côté ou par-derrière ne sont jamais bloqués.

Une **hache** ou la {{item embervoid:ashbearers_mace}} brise la garde : la Carcasse titube pendant trois secondes
<!-- HollowEntity.STAGGER_TICKS -->, ralentie et incapable de frapper. C'est le meilleur moment pour l'achever.

Donc : provoquez le coup, esquivez-le, frappez avant que la garde ne se lève, et gardez une hache dans votre barre
d'accès rapide. Son niveau est élevé pour un monstre de l'Overworld, ce qui le rend long à tuer, mais l'expérience en
vaut la peine.

## Butin

{{drops}}
