---
entity: embervoid:grave_warden
kind: mob
source: 106eb91e3ace
---

{{stats}}

## Où le trouver

Le Sonneur des tombes est l'élite de la [Nécropole engloutie](../../places/sunken-necropolis.md). Il est uniquement
placé, un par chapelle et dans les chambres du Sonneur des cryptes, et reste mort une fois tué.

## Comportement

Le sonneur de cloche : une lourde silhouette armée d'une massue et d'une cloche. Son coup de massue frappe fort, mais il
la lève lentement avant le coup ; reculez hors de portée pendant la préparation.

La cloche est la vraie menace. Quelques secondes après vous avoir repéré, puis toutes les douze secondes
<!-- GraveWardenEntity.FIRST_BELL, BELL_INTERVAL -->, il plante ses pieds et lève la cloche pendant une pleine seconde
<!-- GraveWardenEntity.BELL_WINDUP -->, puis la sonne. Le tintement :

- relève d'un coup chaque tas d'Ossements sans repos autour de lui ;
- fait lever deux nouveaux Ossements sans repos s'il n'y en a aucun autour, jusqu'à quatre des siens à la fois
  <!-- GraveWardenEntity.MAX_RAISED --> ;
- fait chanceler chaque joueur à quelques blocs <!-- GraveWardenEntity.STAGGER_RANGE --> avec une forte Lenteur pendant
  deux secondes <!-- GraveWardenEntity.STAGGER_TICKS -->.

Quand la cloche se lève, éloignez-vous du Sonneur pour que le vacillement vous rate, et profitez de la seconde où il
reste immobile pour placer un coup à distance. Ne perdez pas de temps à pourchasser les Ossements qu'il relève : ils
reviennent tant que le Sonneur n'est pas mort. Concentrez-vous sur le Sonneur, et achevez les tas seulement quand il est
à terre, ou combattez près d'une torche pour que les tas tombent en poussière d'eux-mêmes.

## Butin

{{drops}}

Le Sonneur est l'élite de la Nécropole. Une victoire de joueur peut lâcher un ou deux {{item embervoid:raw_grave_iron fers funéraires bruts}},
une pièce de l'[ensemble du geôlier](../../items/armor/wardens-set.md), les {{item embervoid:drowned_sabatons}} au taux
d'élite, et rarement l'un des accessoires de la Nécropole : le {{item embervoid:gravestone_medallion}},
l'{{item embervoid:ferrymans_coin}}, la {{item embervoid:wardens_chain}} ou l'{{item embervoid:drowned_mans_ring}}.
