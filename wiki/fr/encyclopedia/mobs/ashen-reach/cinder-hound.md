---
entity: embervoid:cinder_hound
kind: mob
source: 9c3823c0cdec
---

{{stats}}

## Où le trouver

Les Limiers calcinés rôdent dans la cité en flammes autour de la [Citadelle rougeoyante](../../places/smoldering-citadel.md),
le dernier secteur derrière la Porte de la citadelle. Ce sont ses seuls monstres à apparition naturelle et ils viennent
en meutes de trois à cinq <!-- config reachMobs.spawns.cinder_hound -->, partout où la lumière de bloc est faible. Le
[Seigneur rougeoyant](../../bosses/smoldering-lord.md) appelle des limiers supplémentaires dans sa cour quand vous le
combattez en groupe.

## Comportement

Un limier rapide et brûlant. Quand il choisit une cible, il pousse un hurlement, et tout limier inactif dans un grand
rayon <!-- CinderHoundEntity.HOWL_RANGE --> se joint à la chasse. Un seul limier qui vous repère, c'est toute la meute
qui arrive.

Il court plus vite que vous ne marchez et mord d'une très courte fente ; une morsure qui touche vous enflamme pendant
quelques secondes <!-- CinderHoundEntity.BITE_FIRE_SECONDS -->. Le feu ne blesse pas les limiers eux-mêmes : les armes de
feu et la lave sont donc gaspillées contre eux.

Le problème, c'est la meute, pas le limier seul. Quand vous entendez le hurlement, reculez dans un coin ou une ruelle
étroite pour qu'ils ne puissent vous atteindre que d'un côté, et frappez tout ce qui entre à portée : un limier a peu de
vie, et les larges attaques de balayage en touchent plusieurs d'un coup. La résistance au feu atténue leurs morsures.
Continuez de bouger entre les combats dans la cité, car un nouveau hurlement attire tout limier encore inactif à
proximité.

## Butin

{{drops}}
