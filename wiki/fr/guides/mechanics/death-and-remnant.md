---
title: Mort et Vestige de cendres
kind: mechanic
order: 5
entity: embervoid:ash_remnant
source: 9df6c2f65aa0
---

Quand vous mourez, vous ne dispersez pas vos affaires par terre. Elles vont dans un **Vestige de cendres** qui vous
attend là où vous êtes tombé.

## Ce qu'il contient

- tous vos **points d'expérience**, exactement : votre niveau et votre barre reviennent tels qu'ils étaient ;
- tous vos **objets**, y compris l'armure et les anneaux et artefacts portés dans votre Reliquaire ;
- mais jamais votre Fiole de soin : elle reste dans son emplacement.

Le vestige apparaît sur le dernier sol ferme où vous vous teniez, jamais dans la lave ni dans le néant. Si vous tombez
d'une falaise ou dans le néant, il attend au bord d'où vous êtes tombé. Le chat vous dit où il se trouve : « Votre
Vestige de cendres repose en … ».

## Le récupérer

Rendez-vous sur place et faites un **clic droit** dessus. Tout revient : les objets à leurs anciens emplacements quand
ils sont libres, le reste dans votre inventaire ou à vos pieds. Vous seul pouvez le prendre ; un autre joueur qui essaie
apprend à qui sont ces cendres.

Un vestige ne peut être ni blessé, ni tué, ni détruit par une explosion, ni perdu dans le néant, et il ne disparaît pas.
Il attend aussi longtemps qu'il le faut.

## Mourir de nouveau

Vous n'avez qu'un vestige à la fois. Si vous mourez avant de l'atteindre, par défaut :

- l'**expérience** de l'ancien vestige brûle ;
- ses **objets restent** dedans jusqu'à ce que vous les ramassiez ;
- votre nouvelle mort laisse un nouveau vestige avec ce que vous portiez cette fois-ci.

Retournez donc chercher votre expérience avant de prendre des risques. Les objets sont à l'abri ; l'expérience, non.

## Où vous vous réveillez

- **Overworld, Nether, End, hub :** comme dans vanilla, à votre lit ou au point d'apparition du monde.
- **Dans un domaine :** au dernier Autel de braise où vous vous êtes reposé dans ce domaine, ou à son aire d'arrivée. La
  Citadelle rougeoyante compte à part. Voir [Autels de braise et réapparition](ember-shrines.md).

Votre fiole est de nouveau pleine à la réapparition.

## Autres modes

Le serveur peut tout changer :

| `remnant.mode` | Ce qui se passe à la mort |
|---|---|
| `xp_and_items` (par défaut) | l'expérience et les objets vont dans le vestige |
| `xp` | l'expérience va dans le vestige, les objets tombent comme dans vanilla |
| `off` | mort vanilla |

| `remnant.secondDeath` | Ce qu'une nouvelle mort fait à l'ancien vestige |
|---|---|
| `burn_xp` (par défaut) | son expérience brûle, ses objets restent |
| `burn_all` | il brûle avec tout son contenu |

Avec la règle de jeu `keepInventory` activée, vous gardez vos objets et votre expérience comme dans vanilla, et aucun
vestige n'est laissé.

## Réglages

{{config remnant.mode}}
{{config remnant.secondDeath}}

Suite : [Autels de braise et réapparition](ember-shrines.md)
