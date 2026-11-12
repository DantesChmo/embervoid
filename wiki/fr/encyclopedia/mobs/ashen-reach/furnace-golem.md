---
entity: embervoid:furnace_golem
kind: mob
source: 034a7cc2049a
---

{{stats}}

## Où le trouver

Le Golem fournaise est le tank de la [Citadelle rougeoyante](../../places/smoldering-citadel.md). Il est uniquement
placé, dans les salles et les routes de l'anneau intérieur, et reste mort une fois tué.

## Comportement

Une fournaise ambulante : lente, lourdement armurée, difficile à repousser et insensible au feu.

Son attaque propre est une frappe à deux bras. Il plante ses pieds et lève les deux poings un instant avant de les
abattre devant lui ; reculez hors de portée pendant qu'il les lève.

Le piège, c'est la chaleur. Chaque coup qu'il reçoit l'attise. Au quatrième coup <!-- FurnaceGolemEntity.HITS_TO_BLAST -->,
il interrompt ce qu'il faisait, plante ses pieds et rougeoie pendant plus d'une seconde
<!-- FurnaceGolemEntity.HEAT_TICKS -->, puis libère une onde de feu : tout ce qui est à quelques blocs
<!-- FurnaceGolemEntity.WAVE_RADIUS --> subit des dégâts de feu, est enflammé et repoussé. Ensuite, il refroidit et le
décompte repart.

Comptez donc vos coups. Portez-en trois, puis au quatrième, ou dès que vous voyez la lueur, reculez hors de portée de
l'onde et attendez l'explosion avant de revenir. La résistance au feu aide beaucoup ici. Les coups lourds et lents valent
mieux que beaucoup de coups légers, puisque chaque coup compte pareil pour l'explosion. Les armes de feu ne lui font
rien.

## Butin

{{drops}}

La Citadelle n'a ni minerai ni ensemble d'armure qui lui soit propre. Un Golem fournaise tué par un joueur peut lâcher la
{{item embervoid:furnace_heart_cuirass}}, au taux d'élite.
