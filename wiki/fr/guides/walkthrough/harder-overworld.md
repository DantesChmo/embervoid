---
title: Un Overworld plus dur
kind: guide
order: 1
source: 5621f6ba8e7b
---

Dès qu'Embervoid est installé, la nuit devient plus dangereuse. Rien de neuf n'est ajouté au jour ; ce qui change, c'est
ce qui vous traque après la tombée de la nuit et sous terre.

## Chaque monstre a un niveau

Chaque monstre hostile reçoit un niveau à son apparition. Regardez-en un : son nom affiche « Niv. N ». Un niveau plus
élevé signifie plus de santé, plus de dégâts et plus d'expérience à sa mort. Les niveaux montent à mesure que vous vous
éloignez du point d'apparition du monde, sous y = 0, dans le Nether et dans l'End. Un zombie près du point d'apparition
est de niveau 3 <!-- MobConfig: minecraft:zombie base level --> ; un enderman commence au niveau 12
<!-- MobConfig: minecraft:enderman base level -->.

Les monstres frappent aussi plus fort à chaque difficulté. Le mode Facile frappe désormais comme le Normal de vanilla ;
le mode Difficile frappe plus fort que le Difficile de vanilla. Les détails sont dans
[Niveaux des monstres et difficulté](../mechanics/mob-levels.md).

Que faire :

- Fabriquez tôt un bouclier et une armure, et ne vous aventurez pas loin du point d'apparition la nuit en cuir.
- Considérez les grottes sous y = 0 comme un cran au-dessus : les monstres y ont quelques niveaux de plus.
- Éclairez vos grottes. La lumière est aussi la réponse à l'un des nouveaux hostiles.

## Trois nouveaux hostiles

- [Goule de cendre](../../encyclopedia/mobs/overworld/ash-ghoul.md) : rapide, elle vient en meute la nuit et dans les
  grottes sombres. Quand l'une vous trouve, les autres alentour s'y joignent. En tuer une laisse un nuage de cendre qui
  vous aveugle : reculez avant de tuer la suivante. Elles brûlent à la lumière du jour.
- [Carcasse](../../encyclopedia/mobs/overworld/hollow.md) : un épéiste lent et cuirassé, rare, en surface la nuit. Elle
  lève une garde qui bloque les coups et les flèches de face. Frappez-la avec une **hache** pour briser sa garde et la
  déséquilibrer, puis attaquez. Surveillez la préparation avant son coup.
- [Feu follet de braise](../../encyclopedia/mobs/overworld/ember-wisp.md) : un petit volant tapi profondément sous
  terre, dans l'obscurité totale. Il plonge sur vous et met le feu à tout. Une vive lumière le chasse : posez des
  torches et il s'enfuit.

## Pourquoi l'expérience compte désormais

L'expérience de vanilla est la monnaie d'Embervoid. Vous la dépensez en caractéristiques auprès de la Vierge cendrée, un
point à la fois, une fois au hub. Gardez donc l'expérience que vous gagnez : chaque monstre, chaque minerai, chaque
fournée compte. L'enchantement existe toujours, mais chaque niveau que vous y dépensez est un niveau que vous ne mettez
pas dans votre personnage.

Si vous mourez, votre expérience n'est pas perdue : elle vous attend dans votre [Vestige de cendres](../mechanics/death-and-remnant.md)
avec vos objets. Retournez-y et faites un clic droit dessus pour tout récupérer.

Vous commencez aussi avec une {{item embervoid:healing_flask}}. Buvez-la au lieu de manger pendant un combat ; se reposer
la remplit. Voir [Fiole de soin](../mechanics/healing-flask.md).

Suite : [Trouver un Sanctuaire en ruine](ruined-sanctum.md)
