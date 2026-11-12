---
entity: embervoid:smoldering_knight
kind: mob
source: 2686221509c3
---

{{stats}}

## Où le trouver

Le Chevalier rougeoyant est l'élite de la [Citadelle rougeoyante](../../places/smoldering-citadel.md). Les Chevaliers
sont uniquement placés, dans les tours et le donjon, et restent morts une fois tués.

## Comportement

La garde du roi, armée d'une épée à deux mains : lourdement armurée et insensible au feu.

- **Enchaînement en trois coups.** À portée, il enchaîne trois coups : deux rapides balayages latéraux, puis un lourd
  coup d'en haut qui frappe le sol devant lui. Les préparations sont courtes, et chaque coup touche même juste après le
  précédent : votre immunité aux coups ne vous sauve donc pas. Ne l'affrontez pas coup pour coup : reculez hors de
  portée dès que le premier balayage commence, ou parez avec un bouclier, et avancez après le coup final d'en haut.
- **Fente.** Quand vous êtes à quelques blocs <!-- SmolderingKnightEntity.LUNGE_MIN, LUNGE_MAX -->, il peut s'accroupir
  pendant trois quarts de seconde <!-- SmolderingKnightEntity.CROUCH_TICKS -->, puis s'élancer d'environ six blocs droit
  sur vous et vous transpercer, plus fort qu'un coup de l'enchaînement. L'accroupissement est votre signal : écartez-vous
  de la ligne de l'élan.

La meilleure fenêtre est juste après le lourd coup final de l'enchaînement ou une fente manquée. Ne traînez pas à
moyenne distance, où il se fend ; restez soit hors de sa vue, soit tout contre lui.

## Butin

{{drops}}

Le Chevalier est l'élite de la Citadelle. Une victoire de joueur peut lâcher la {{item embervoid:furnace_heart_cuirass}}
au taux d'élite et, rarement, l'un des accessoires de la Citadelle : le {{item embervoid:seal_of_the_ashen_king}} ou
l'{{item embervoid:ring_of_embers}}.
