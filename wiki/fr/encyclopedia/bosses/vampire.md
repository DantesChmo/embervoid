---
entities: [embervoid:vampire_boss, embervoid:vampire]
kind: boss
source: efb9de070eb1
---

{{stats}}

## Comment l'atteindre

Le Vampire ne vient que lors de la **nouvelle lune de sang**, la seconde des deux [nuits de lune](../../guides/mechanics/moon-nights.md)
(le guide complet : [Le Vampire](../../guides/mechanics/vampire.md)). Quand le ciel devient rouge, un à trois Vampires
<!-- vampire.groupMin, vampire.groupMax --> apparaissent près de chaque groupe de joueurs dans l'Overworld. Les joueurs
dans un ancien noyau de Sanctuaire sont exclus.

Sous cette forme, ils sont amicaux. Ils se promènent, vous observent et parlent quand vous leur adressez la parole. On
ne peut pas les blesser : le premier joueur qui frappe un Vampire quelconque le provoque. Celui-ci se transforme en boss
après une courte transformation, et tous les autres Vampires s'évanouissent. Si personne n'en provoque un, le Vampire le
plus proche d'un joueur se transforme de lui-même au bout de cinq minutes <!-- vampire.provokeMinutes -->.

Quand le boss se forme, **tous** les joueurs de l'Overworld (hors noyau de Sanctuaire) sont attirés dans l'arène du
Vampire : une petite arène flottant dans un Néant à la brume rouge, reconstruite à chaque lune de sang. Vous ne pouvez
y casser ni poser de blocs. Il n'y a pas de mur de brume ; le Néant autour de l'arène fait office de mur.

Son niveau est le plus haut niveau parmi les joueurs participants, au moins 15 <!-- vampire.bossMinLevel -->, mais pas
plus que le sommet de la tranche de niveaux du domaine le plus lointain qu'aucun d'eux ait débloqué (20 si aucun
<!-- vampire.bossCapNoRealm -->).

## Phases et attaques

- **Griffes.** Il vous poursuit et retire ses griffes en arrière avec un bref signal rouge avant de frapper. Reculez
  pendant le signal ; il porte quand même un peu plus loin qu'il n'y paraît.
- **Bond de sang.** Contre une cible entre cinq et seize blocs, il s'accroupit une seconde pendant que deux anneaux rouge
  sang marquent l'endroit où il atterrira, puis saute là et frappe tous ceux qui sont autour du point d'atterrissage.
  Sortez des anneaux en courant.

### Sous 60 % de sa vie

- **Nova de sang.** Un anneau se forme autour de lui et se rétrécit pendant une seconde et demie, puis éclate : tous
  ceux qui sont à l'intérieur sont blessés et repoussés, et il récupère un quart des dégâts infligés
  <!-- VampireBossEntity.NOVA_HEAL -->. Sortez de l'anneau et il ne récupère rien.
- Il bondit plus souvent.

### Sous 30 % de sa vie

- **Pas de brume.** Quand vous êtes à plus de trois blocs, il se dissout en fumée et réapparaît juste derrière vous avec
  une frappe. Quand vous voyez la fumée, retournez-vous.
- Il se déplace plus vite, et la nova revient plus souvent.

## Butin

{{drops}}

Il n'a pas de table de butin. À sa mort, chaque combattant encore en vie dans l'arène reçoit sa récompense directement
dans l'inventaire :

- votre **première** victoire : deux {{item embervoid:flask_shard éclats de fiole}} <!-- vampire.firstKillShards --> et
  un {{item embervoid:vampire_fang}} ;
- une victoire **répétée** : une chance sur deux <!-- vampire.repeatShardChance --> d'obtenir un Éclat de fiole.

Il donne de l'expérience comme n'importe quel boss. Dix secondes après sa mort, l'arène se ferme, tout le monde retourne
là d'où il avait été attiré, et l'aube se lève.

**Si vous mourez dans l'arène**, vous réapparaissez normalement et ne pouvez plus y revenir. Votre
[Vestige de cendres](../../guides/mechanics/death-and-remnant.md) est déplacé à l'endroit d'où vous avez été attiré
quand l'arène se ferme. Si tous les combattants meurent, le Vampire s'en va, l'arène se ferme et la nuit prend fin.

## Stratégie

**Au corps à corps.** Restez assez près pour qu'il griffe plutôt que de bondir, et reculez d'un pas à chaque signal. À
partir de 60 %, la nova est l'attaque à respecter : dès que l'anneau apparaît, sortez-en, puis revenez frapper pendant
sa récupération. Sous 30 %, ne collez votre dos au bord de l'arène que si vous êtes sûr de votre équilibre ; le Pas de
brume le place derrière vous.

**À distance.** Difficile à tenir : il est rapide et bondit sur quiconque est à distance. Tirez pendant qu'il vous
poursuit, et esquivez sur le côté chaque anneau d'atterrissage.

**En groupe.** Tous les joueurs de l'Overworld le combattent ensemble, et son niveau suit le plus fort d'entre vous. Il
n'y a pas de sbires. Dispersez-vous pour qu'une nova ou un bond n'attrape pas tout le groupe, et laissez celui qu'il
poursuit le mener en bateau pendant que les autres le frappent par-derrière. Chaque combattant en vie à la fin reçoit sa
propre récompense : gardez donc les nouveaux joueurs en vie.
