---
title: Le Vampire
kind: mechanic
order: 12
source: ff83967294cc
---

Lors d'une [nouvelle lune de sang](moon-nights.md), des silhouettes pâles sortent de l'ombre près des joueurs : des
**Vampires**. Ils semblent amicaux. Ils parlent, ils admirent la nuit, ils vous demandent de rester un peu. L'un d'eux
est sur le point de devenir un boss.

## Quand il vient

- À chaque nouvelle lune de sang, 1 à 3 Vampires amicaux apparaissent près de chaque groupe de joueurs de l'Overworld (les
  joueurs distants de moins de 64 blocs partagent un même groupe). <!-- config vampire.groupMin / groupMax / groupRadius -->
- **Frappez-en un** et il abandonne son déguisement : il devient le boss Vampire, et les autres disparaissent.
- Si personne n'en frappe un, le Vampire le plus proche d'un joueur perd patience au bout de 5 minutes
  <!-- config vampire.provokeMinutes --> et se transforme quand même.

Vous ne pouvez pas éviter le combat en étant poli. Vous pouvez seulement choisir quand il commence.

## Le domaine du Vampire

Quand le Vampire se transforme, **tous les joueurs de l'Overworld** sont entraînés dans son domaine, une arène dans un
néant à elle. Il n'y a pas de mur de brume, mais il n'y a pas non plus d'endroit où aller.

- Le niveau du boss correspond à celui du joueur participant le plus fort : au moins 15, et au plus 20 tant qu'aucun
  participant n'a débloqué de domaine. <!-- config vampire.bossMinLevel / bossCapNoRealm -->
- Surveillez la préparation avant ses griffes ; ses attaques sont sur la page du [Vampire](../../encyclopedia/bosses/vampire.md).
- **Si vous mourez dans le domaine, vous êtes éliminé :** vous réapparaissez comme d'habitude et ne pouvez pas revenir.
  Quand le domaine se ferme, votre vestige est emporté à l'endroit d'où l'on vous a pris.

## Gagner et perdre

- **Victoire :** chaque combattant encore en vie reçoit un butin personnel directement dans son inventaire. Votre première
  mise à mort donne un {{item embervoid:vampire_fang}} et 2 Éclats de fiole ; les suivantes, 50 % de chances d'un Éclat de
  fiole <!-- config vampire.firstKillShards / repeatShardChance -->. Dix secondes plus tard, vous êtes tous renvoyés à
  l'endroit d'où l'on vous a pris, et l'aube se lève.
- **Défaite :** quand tous les combattants sont morts, le Vampire se fond dans la nuit en riant, et la nuit se termine.

## Comment vous préparer

- Attendez-vous à la nouvelle lune de sang quatre jours après chaque pleine lune, et soyez prêt avant le crépuscule :
  fiole pleine, Éclats de fiole offerts, équipement réparé.
- Seuls les joueurs de l'Overworld au moment de la transformation sont attirés. Un ami dans le hub ou dans un domaine est à
  l'abri, et d'aucun secours.
- En groupe, décidez qui frappe en premier. Le combat commence en même temps pour tous les joueurs de l'Overworld.

## Réglages

{{config vampire.provokeMinutes}}
{{config vampire.bossMinLevel}}
{{config vampire.bossCapNoRealm}}
{{config vampire.firstKillShards}}

Suite : [Guide de l'équipement](../gear-guide.md)
