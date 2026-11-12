---
title: Combats de boss
kind: mechanic
order: 7
id: embervoid:fog_wall
source: c5317f7be306
---

Chaque boss d'Embervoid combat dans une arène avec les mêmes règles. Les attaques de chaque boss figurent sur sa propre
page de l'Encyclopédie ; cette page couvre les règles qui les entourent.

## Lancer un combat

Entrez dans l'arène d'un boss que vous n'avez pas encore vaincu, et il apparaît. Tous ceux qui se trouvent dans l'arène à
cet instant deviennent des combattants. Une barre de boss affiche son nom et son niveau, sa musique démarre, et un **mur
de brume** se referme autour de l'arène.

- Les combattants ne peuvent pas sortir à travers le mur de brume.
- Personne d'autre ne peut entrer avant la fin du combat.

Si vous avez déjà vaincu le boss, son arène reste vide et vous pouvez la traverser en paix (sauf si quelqu'un qui ne l'a
pas vaincu y entre, ou si vous utilisez une Cendre de revanche).

## Partir et perdre

Vous quittez un combat quand vous mourez, vous vous déconnectez ou vous en sortez autrement (une perle d'Ender, une
téléportation). Quand le dernier combattant est parti, le combat se réinitialise : le boss disparaît et revient avec
toute sa santé à la tentative suivante, et tout ce que le combat a modifié dans l'arène est remis en place.

Comme votre Vestige de cendres repose là où vous êtes tombé, il est généralement dans l'arène : récupérez-le dès le début
de votre tentative suivante.

## Gagner

Tous ceux qui sont **en vie et dans l'arène** à la mort du boss en reçoivent le crédit.

- **Première victoire :** votre récompense personnelle va directement dans votre inventaire, personne ne peut donc
  ramasser la vôtre : l'âme du boss et un Éclat de fiole (le Seigneur rougeoyant : son âme et une Braise de fiole). Le
  boss lâche aussi son butin et son expérience.
- **Victoire répétée**, par exemple en aidant un ami : le butin et l'expérience, et 25 % de chances
  <!-- config boss.repeatSoulChance --> d'avoir de nouveau l'âme.
- L'Autel de braise de l'arène s'embrase pour vous.

## Les boss en groupe

Chaque combattant au-delà du premier rend le boss plus coriace :

- +50 % de santé par combattant supplémentaire <!-- config boss.healthPerExtraFighter -->, fixés au début du combat ;
- un sbire supplémentaire par combattant supplémentaire <!-- config boss.addsPerExtraFighter -->, de quelques niveaux
  inférieurs à ceux du boss.

Toutes les 8 à 10 secondes <!-- config boss.targetSwitchMinSeconds / targetSwitchMaxSeconds -->, le boss se tourne vers
celui qui l'a le plus blessé entre-temps. Répartissez vos dégâts et celui qui encaisse pourra souffler.

La progression est propre à chaque joueur. Un ami qui a vaincu un boss peut vous aider, et en obtient du butin.

## Cendre de revanche

Pour combattre de nouveau un boss vaincu d'un domaine, utilisez une {{item embervoid:ash_of_rematch}} sur l'Autel de
braise devant son arène, puis entrez dans la minute. Une revanche donne du butin et de l'expérience, jamais l'âme ni un
Éclat de fiole. Elle ne fonctionne pas sur la Sœur déchue (le boss de chaque Sanctuaire en ruine ne tombe qu'une fois par
monde) ni sur le Vampire.

## Musique de boss

La musique de boss a son propre curseur **Musique des boss** dans Options → Musique et sons, indépendant du curseur
Musique. Coupez Musique et vous entendez toujours les thèmes de boss ; coupez Musique des boss pour les faire taire.

## Réglages

{{config boss.healthPerExtraFighter}}
{{config boss.addsPerExtraFighter}}
{{config boss.addLevelOffset}}
{{config boss.repeatSoulChance}}

Suite : [Le Reliquaire](reliquary.md)
