---
title: Multijoueur et serveurs
kind: guide
order: 6
source: fefd3e5fe6e7
---

Embervoid se joue de la même façon sur un serveur qu'en solo. Installez-le avec les mods requis sur le serveur et chez
chaque client (voir [Installation](installing.md)).

## Ce qui est partagé et ce qui est à vous

**À vous seul :**

- vos caractéristiques, votre niveau et votre endurance, vos charges de fiole et Éclats de fiole, votre Braise de fiole ;
- votre progression : les boss que vous avez vaincus, les domaines que vous avez débloqués et terminés, et donc l'aspect
  que prend chaque Obélisque du Néant à vos yeux ;
- l'ouverture ou non de la Porte de la citadelle pour vous, et le fait que l'autel d'une arène brûle ou non pour vous ;
- votre autel de réapparition dans chaque domaine ;
- votre Vestige de cendres : personne d'autre ne peut le prendre.

**Partagé par tous :**

- le hub : il n'y a qu'un seul Sanctuaire des Braises pour tout le serveur ;
- chaque domaine : un seul Marches cendrées, le même terrain, les mêmes coffres et les mêmes ennemis placés. Un coffre
  pillé par quelqu'un et un garde tué par quelqu'un le restent pour tout le monde ;
- la Stèle fendue allumée du monde : il y en a une par monde, et tout joueur qui en embrase une nouvelle éteint l'ancienne
  pour tout le monde ;
- le boss de chaque Sanctuaire en ruine : il ne tombe qu'une fois par monde, pour ceux qui participent à ce combat.

## Les boss en groupe

- Tous ceux qui sont dans une arène au début du combat sont des combattants ; le mur de brume empêche les retardataires
  d'entrer.
- Chaque combattant supplémentaire donne au boss +50 % de santé et un sbire de plus. Le boss change de cible toutes les
  8 à 10 secondes, se tournant vers celui qui l'a le plus blessé.
- Tous ceux qui sont en vie et présents à la mise à mort en reçoivent le crédit. Les récompenses de première victoire vont
  dans l'inventaire de chaque joueur.
- Aider un ami qui n'a pas encore vaincu un boss : vous obtenez son butin et son expérience, et une chance d'avoir de
  nouveau son âme.
- **La Sœur déchue ne tombe qu'une fois par Sanctuaire.** Descendez ensemble : chaque joueur de ce combat obtient une
  Âme éplorée. Un joueur qui l'a manquée a besoin d'un autre Sanctuaire.

Détails dans [Combats de boss](mechanics/boss-fights.md).

## Les nuits de lune entre amis

- Les vagues de pleine lune sont propres à chaque joueur : chaque joueur de l'Overworld a la sienne, issue de sa propre
  malédiction.
- Les joueurs distants de moins de 64 blocs partagent un même groupe de Vampires amicaux.
- Quand un Vampire se transforme, **tous les joueurs de l'Overworld** sont attirés dans son domaine, où qu'ils soient.
  Prévenez vos amis avant d'en attaquer un.

## Réglages de serveur à connaître

Le `config/embervoid.json` du serveur fixe les règles pour tout le monde ; chaque client qui se connecte le reçoit.
Modifiez-le serveur arrêté : le fichier est lu au démarrage. Les clés qu'un propriétaire de serveur change le plus souvent :

- `remnant.mode` et `remnant.secondDeath` : la dureté de la mort.
- `mobs.difficulty.*` : la force des coups des monstres hostiles à chaque difficulté.
- `boss.healthPerExtraFighter` et `boss.addsPerExtraFighter` : de combien les boss se durcissent en groupe.
- `moon.enabled` : nuits de lune activées ou non.
- `vampire.groupRadius` : la proximité requise entre joueurs pour partager un même groupe de Vampires.

Toutes les clés sont sur [Configuration](configuration.md). Les commandes d'opérateur sont sur [Commandes](commands.md).

Suite : [Configuration](configuration.md)
