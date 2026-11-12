---
title: Datapacks et compatibilité
kind: guide
order: 8
source: a2501f21866e
---

Une grande partie d'Embervoid est de la donnée : un datapack peut changer l'endroit où apparaissent ses donjons, le
contenu de ses coffres et la façon dont le contenu d'autres mods s'y intègre. Les chemins ci-dessous se trouvent dans le
dossier `data/` d'un datapack.

## Génération du monde que vous pouvez remplacer

| Fichier | Ce qu'il contrôle |
|---|---|
| `embervoid/worldgen/structure_set/ember_sanctum.json` | la fréquence des Sanctuaires en ruine : `spacing` 22 et `separation` 8 chunks par défaut (environ un tous les 300 à 400 blocs). Augmentez les deux pour des donjons plus rares. |
| `embervoid/tags/worldgen/biome/has_structure/ember_sanctum.json` | les biomes où peut se dresser un Sanctuaire en ruine |
| `embervoid/worldgen/structure/ember_sanctum.json`, `embervoid/worldgen/template_pool/sanctum/*` | le donjon lui-même et ses pièces |
| `embervoid/tags/worldgen/structure/on_sanctum_maps.json` | les structures vers lesquelles pointe une Carte du Sanctuaire |
| `embervoid/worldgen/biome/ashen_reach_*.json` | l'aspect du domaine par zone : couleurs, particules, sons d'ambiance |
| `embervoid/worldgen/configured_feature/ashen_reach/*`, `placed_feature/ashen_reach/*` | les minerais, arbres, débris et routes du domaine |

Soyez prudent avec la dimension, les réglages de bruit et les ensembles de structures du domaine
(`embervoid/dimension/ashen_reach.json`, `embervoid/worldgen/noise_settings/ashen_reach.json`,
`embervoid/worldgen/structure_set/ashen_reach_*`). Les zones, arènes, porte et Citadelle du domaine sont disposées par le
code avec les mêmes valeurs ; changer un seul côté casse le domaine. Ne touchez pas au hub ni à l'arène du Vampire
(`embervoid/dimension/hub.json`, `vampire_arena.json`).

Les monstres des Marches cendrées n'apparaissent pas depuis les fichiers de biome (leurs listes d'apparition sont vides
exprès). Changez leurs apparitions avec `reachMobs.spawns` dans la [config](configuration.md).

## Tables de butin

- `embervoid/loot_tables/chests/sanctum_outer_hall.json` : les coffres de la salle du Sanctuaire en ruine.
- `embervoid/loot_tables/gameplay/fallen_sister_first_clear.json` : le butin supplémentaire de la première victoire d'un
  joueur au Sanctuaire.
- `embervoid/loot_tables/chests/ashen_reach/*` : les coffres du domaine, trois types par zone (commun, arène, trésor).
- `embervoid/loot_tables/entities/*` : le butin des monstres et des boss.

Certains butins sont ajoutés par le mod en plus de ces tables : les matériaux de domaine, les pièces d'ensemble d'armure
et les pièces uniques sur les monstres du domaine, les anneaux et artefacts dans les coffres au trésor et sur les élites,
et la Carte du Sanctuaire dans les coffres vanilla. **Si votre datapack remplace l'une de ces tables, ces ajouts ne lui
sont pas appliqués** : votre table décide seule. Leurs probabilités se règlent plutôt dans la config (`gear.loot.*`,
`reliquary.drops.*`, `sanctum.mapChance*`).

## Tags

| Tag | Signification |
|---|---|
| `embervoid:unleveled` (types d'entités) | les monstres qui n'obtiennent jamais de niveau : l'Ender Dragon, le Wither, le Warden, l'Elder Guardian. Ajoutez-y les boss d'autres mods. |
| `embervoid:bleed_immune` (types d'entités) | les monstres qui ne saignent jamais |
| `embervoid:breaks_guard` (objets) | les armes qui brisent la garde d'une Carcasse comme une hache (la Masse du porte-cendre) |
| `embervoid:artifacts`, `embervoid:rings` (objets) | ce qui entre dans les emplacements d'artefact et d'anneau du Reliquaire |
| `embervoid:realm_ores` (blocs) | les minerais pour lesquels l'Anneau du fouisseur donne Célérité |
| `embervoid:dead_logs` (blocs, objets) | les bûches de bois mort |
| `embervoid:ashen_reach*` (biomes) | les biomes du domaine, tous et par zone |

## Autres mods

Aucun conflit connu pour l'instant. À garder en tête :

- **Les monstres d'autres mods** reçoivent un niveau comme les monstres vanilla, au niveau de base par défaut. Donnez-leur
  un niveau adapté dans `mobs.levels.baseLevels`, et placez leurs boss dans `embervoid:unleveled` s'ils doivent rester
  tels que leur auteur les a conçus.
- **Les mods de tombes ou de cadavres** agissent aussi à la mort. N'utilisez qu'un seul système : soit réglez
  `remnant.mode` sur `off`, soit renoncez à l'autre mod.
- **Les mods de dégâts et de difficulté :** Embervoid remplace le facteur de difficulté de vanilla pour les dégâts des
  monstres aux joueurs (`mobs.difficulty.*`). Un autre mod qui modifie la même chose peut se cumuler avec lui.
- Les bonus de caractéristiques d'Embervoid utilisent les attributs vanilla quand vanilla en possède un (santé max,
  vitesse d'attaque, résistance au recul, résistance de l'armure, chance), de sorte que les mods qui lisent les attributs
  les voient.

Suite : [Commandes](commands.md)
