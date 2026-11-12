---
title: Commandes
kind: guide
order: 9
source: 35a2e3962f63
---

Les commandes d'Embervoid s'adressent aux opérateurs de serveur et aux tests. Toutes commencent par `/embervoid` et
demandent la permission d'opérateur (niveau 2) ; en solo, activez les triches. Elles servent à dépanner un joueur
bloqué ou à essayer quelque chose, pas au jeu normal, et leur sortie n'existe qu'en anglais.

`<stat>` est l'une des valeurs `vitality`, `endurance`, `strength`, `dexterity`, `kindling`, `resilience`, `fortune`,
ou son nom court (`vit`, `end`, `str`, `dex`, `kin`, `res`, `for`).

## Joueur

| Commande | Effet |
|---|---|
| `/embervoid stats` | vos caractéristiques, votre niveau, le coût du niveau suivant et ce que donnent les caractéristiques |
| `/embervoid levelup <stat>` | achète un point comme le ferait la Vierge cendrée, en payant de l'expérience |
| `/embervoid setstat <stat> <value>` | fixe une caractéristique gratuitement (1–999) |
| `/embervoid stamina` | votre endurance |
| `/embervoid stamina spend <amount>` | dépense de l'endurance comme le ferait une attaque spéciale |
| `/embervoid stamina set <value>` | fixe votre endurance |
| `/embervoid data` | vos données de joueur Embervoid telles que le serveur les stocke |
| `/embervoid data flaskChargeUpgrades <n>` | fixe le nombre d'Éclats de fiole absorbés par votre fiole |
| `/embervoid remnant` | dépose vos objets et votre expérience dans un Vestige de cendres à vos pieds, comme une mort |
| `/embervoid reliquary <player>` | affiche le Reliquaire d'un joueur |
| `/embervoid reliquary <player> open` | ouvre le Reliquaire de ce joueur à sa place |
| `/embervoid reliquary <player> clear` | vide le Reliquaire de ce joueur |

## Progression

| Commande | Effet |
|---|---|
| `/embervoid progress <player>` | affiche les domaines débloqués et terminés d'un joueur ainsi que les boss vaincus |
| `/embervoid progress <player> reset` | oublie tout cela |
| `/embervoid progress <player> unlock <realm>` | débloque un domaine, p. ex. `embervoid:realm_1` |
| `/embervoid progress <player> complete <realm>` | marque un domaine comme terminé |
| `/embervoid progress <player> boss <id>` | marque un boss comme vaincu, p. ex. `embervoid:fallen_sister` ou `embervoid:ashen_reach/zone_a` |

Les identifiants de domaine vont de `embervoid:realm_1` à `embervoid:realm_4`. Identifiants de boss :
`embervoid:fallen_sister`, `embervoid:ashen_reach/zone_a` (Colosse de cendre), `zone_b` (Arbre endeuillé), `zone_c`
(Gardien de la crypte) et `final` (Seigneur rougeoyant).

## Déplacements

| Commande | Effet |
|---|---|
| `/embervoid hub` | va au hub |
| `/embervoid hub leave` | prend la sortie de secours du hub, sans tenir compte de la stèle allumée : là où vous avez quitté l'Overworld pour la dernière fois, sinon votre lit ou le point d'apparition du monde |
| `/embervoid hub check` | vérifie que le hub est intact |
| `/embervoid stele` | où se trouve la Stèle fendue allumée du monde |
| `/embervoid stele kindle <pos>` | embrase la stèle éteinte à une position, comme le ferait une Âme éplorée |
| `/embervoid realm enter <realm> [toShrine]` | entre dans un domaine comme le ferait son Obélisque du Néant ; `true` mène à votre dernier autel où vous vous êtes reposé |
| `/embervoid realm leave` | quitte le domaine pour le hub, comme le fait l'Obélisque du retour |
| `/embervoid realm where` | le domaine, la zone, la tranche de niveaux et le lieu où vous vous trouvez |
| `/embervoid realm arenas` | toutes les arènes de boss du domaine et si vous les avez vaincues |
| `/embervoid realm roads [posts]` | les routes du domaine, les bornes milliaires, les avant-postes (et les poteaux à lanterne) |
| `/embervoid realm edge` | le mur d'enceinte du domaine et les ponts rompus |
| `/embervoid realm border <from> <to>` | le mur frontière de la dernière zone |
| `/embervoid realm find <block> <x> <z>` | le bloc de ce type le plus haut dans cette colonne, p. ex. la Porte de la citadelle |
| `/embervoid realm data` | vos données de déplacement dans les domaines |

## Boss, donjons et monstres

| Commande | Effet |
|---|---|
| `/embervoid boss fights` | les combats de boss en cours |
| `/embervoid sanctum arena <x> <z>` | l'arène de boss du Sanctuaire en ruine qui commence dans ce chunk (là où pointe `/locate`), et si elle est nettoyée |
| `/embervoid sanctum survey <radius> [<seed> [<spacing> <separation>]]` | compte les Sanctuaires en ruine que la génération du monde placerait autour de vous, sans rien générer |
| `/embervoid moblevel <targets>` | affiche le niveau des monstres, et le niveau qu'aurait une nouvelle apparition ici |
| `/embervoid moblevel <targets> <level>` | fixe le niveau des monstres |
| `/embervoid gear bleed <targets> [stacks]` | ajoute des niveaux de Saignement, comme autant de coups |

Pour trouver une structure, utilisez le `/locate structure embervoid:ember_sanctum` de vanilla (un Sanctuaire en ruine)
ou, aux Marches cendrées, `embervoid:ashen_reach_arena_a`, `_b` et `_c` (les arènes de zone).

## Nuits de lune et Vampire

| Commande | Effet |
|---|---|
| `/embervoid moon status` | la nuit de lune en cours ou la prochaine |
| `/embervoid moon start full` / `start blood` | lance dès maintenant une pleine lune ou une nouvelle lune de sang |
| `/embervoid moon stop` | met fin à la nuit de lune |
| `/embervoid moon setcycle <n>` | règle l'horloge sur le matin du cycle lunaire n (jour 8·n) |
| `/embervoid moon curse <player> <stacks>` | fixe la Malédiction de la nuit d'un joueur |
| `/embervoid wave status` | les vagues de pleine lune |
| `/embervoid wave spawn <players>` / `clear <players>` | envoie ou retire les vagues de ces joueurs |
| `/embervoid vampire status` | les Vampires et le combat de cette nuit |
| `/embervoid vampire provoke` | le Vampire le plus proche se transforme maintenant |
| `/embervoid vampire arena open` / `close` / `check` | l'arène du Vampire, sans combat |

## Performances

| Commande | Effet |
|---|---|
| `/embervoid perf ticks` | les temps de tick du serveur |
| `/embervoid perf chunks <from> <to>` | les chunks chargés dans une zone |
| `/embervoid perf entities <from> <to>` | les entités, monstres et ennemis placés dans une zone |

## Client

`/embervoid_client data` (sans besoin d'être opérateur) affiche vos données de joueur telles que votre propre client les voit.

Suite : [FAQ et dépannage](faq.md)
