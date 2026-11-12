---
entity: embervoid:restless_bones
kind: mob
source: e4af0464aa91
---

{{stats}}

## Où le trouver

Les Ossements sans repos emplissent la [Nécropole engloutie](../../places/sunken-necropolis.md). Ils apparaissent
naturellement en surface et dans les cryptes en petits groupes, et d'autres sont placés dans les rangées de tombes, les
mausolées, la chapelle, les fosses d'ossuaire, les avant-postes de la route et chaque salle des cryptes. Les Sonneurs
des tombes en relèvent de nouveaux, et le [Gardien de la crypte](../../bosses/crypt-keeper.md) les appelle dans son
arène.

## Comportement

Un squelette avec une épée rouillée. Il attaque comme n'importe quelle chair à canon de mêlée : une épée levée, une
courte préparation, un coup. Reculez pendant la préparation et ripostez après.

Ce qui le rend dangereux, c'est qu'il ne reste pas mort. Le coup qui devrait le tuer le fait s'effondrer en un tas d'os.
Le tas reste là cinq secondes <!-- RestlessBonesEntity.REFORM_TICKS -->, puis se relève avec la moitié de sa vie. Vous
avez deux façons de l'achever pour de bon :

- **Briser le tas.** Le tas a très peu de vie ; un coup de plus avant qu'il ne se relève le tue.
- **L'éclairer.** Un tas posé dans une lumière de bloc de 8 ou plus <!-- RestlessBonesEntity.LIGHT_TO_STAY_DEAD -->
  tombe en poussière, et la victoire vous revient quand même. Combattez les Ossements près d'une torche, ou posez-en une
  près du tas.

Méfiez-vous des Sonneurs des tombes : le tintement de la cloche d'un Sonneur relève d'un coup tous les tas autour de lui.

## Butin

{{drops}}

En plus de la table, les Ossements peuvent lâcher du {{item embervoid:raw_grave_iron}} et, quand un joueur les tue, une
pièce de l'[ensemble du geôlier](../../items/armor/wardens-set.md) ou, rarement, les {{item embervoid:drowned_sabatons}}.
