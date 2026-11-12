---
entity: embervoid:ghost
kind: mob
source: 3972120c6791
---

{{stats}}

## Où le trouver

Les Spectres n'apparaissent jamais d'eux-mêmes. Ils viennent lors de la **pleine lune** (voir les
[nuits de lune](../../../guides/mechanics/moon-nights.md)) pour chaque joueur de l'Overworld qui ne porte **aucune**
Malédiction de la nuit : trois d'entre eux <!-- nightMobs.ghostCount --> apparaissent à 16 à 32 blocs
<!-- nightMobs.spawnMinDistance, nightMobs.spawnMaxDistance --> et foncent sur vous. Un joueur avec des cumuls de
malédiction reçoit à la place des [Zombies cendreux](ashen-zombie.md).

## Comportement

Un pâle esprit volant, bien plus coriace qu'un Zombie cendreux. Il vole, traverse murs et blocs, et vous voit à travers
eux : impossible donc de vous cacher ou de vous emmurer. Il frappe quand il vous touche, environ toutes les deux
secondes <!-- GhostEntity.ATTACK_COOLDOWN_TICKS -->.

Certains de vos coups le traversent dans un souffle de brume et ne font rien (environ un sur sept
<!-- nightMobs.ghostMissChance -->). Attendez-vous à un long combat et prenez de la nourriture et votre fiole.

Combattez-les à découvert, où vous pouvez voir les trois arriver, et continuez de bouger pour qu'un seul vous atteigne
à la fois.

Votre vague retient la nuit : l'aube ne vient pas avant que les trois aient disparu. Si vous venez à bout de la vague,
vous recevez un {{item embervoid:flask_shard}}. Si vous mourez, votre vague de Spectres s'arrête là, et la mort vous
donne un cumul de malédiction à la place. Les Spectres restants à la fin de la nuit s'évanouissent.

## Butin

{{drops}}
