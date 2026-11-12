---
entities: [embervoid:lantern_hag, embervoid:fire_flask]
kind: mob
source: 5077934a1bc6
---

{{stats}}

## Où la trouver

La Mégère à la lanterne est l'ennemie à distance des [Bois royaux](../../places/royal-woods.md). Elle apparaît seule, et
d'autres sont placées dans les affûts de chasseurs, les rangées de lanternes, le pavillon de chasse et les avant-postes
de la route.

## Comportement

Elle garde ses distances, à 8 à 12 blocs environ <!-- LanternHagEntity.MIN_RANGE, MAX_RANGE --> : elle recule quand vous
vous approchez et avance quand vous vous éloignez. De là, elle lance des flacons de feu en un grand arc.

Chaque lancer est annoncé. Elle lève sa lanterne, qui flambe de braises, et trois quarts de seconde plus tard
<!-- LanternHagEntity.THROW_WINDUP --> le flacon quitte sa main, visant l'endroit où vous vous tenez à cet instant. Il
éclate en infligeant des dégâts autour de son point de chute et laisse une nappe de feu qui enflamme quiconque s'y tient
pendant quelques secondes <!-- FireFlaskEntity.PATCH_TICKS -->. Aucun bloc de feu n'est posé : les bois ne brûlent donc
pas.

Quand la lanterne se lève, continuez de vous déplacer sur le côté : le flacon tombe là où vous étiez. Ne combattez
jamais dans sa nappe de feu.

Pour la tuer, foncez sur elle. Elle ne peut pas lancer sur quelque chose à quelques blocs d'elle
<!-- LanternHagEntity.MIN_THROW_RANGE --> et se contente de reculer, et elle a peu de vie. Un arc ou une arbalète
fonctionne aussi, puisqu'elle reste immobile pendant qu'elle vise. Occupez-vous de la Mégère en premier quand elle vient
avec des Serfs ou une Carcasse d'écorce.

## Butin

{{drops}}

En plus de la table, une Mégère peut lâcher de l'{{item embervoid:royal_amber}}, et une Mégère tuée par un joueur peut
lâcher une pièce de l'[ensemble du veneur](../../items/armor/huntsmans-set.md) ou, rarement, les
{{item embervoid:huntsman_greaves}}.
