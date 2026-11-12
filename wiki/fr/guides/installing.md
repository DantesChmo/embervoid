---
title: Installation
kind: guide
order: 2
source: 2ac1a2032991
---

## Ce qu'il vous faut

| Mod | Version |
|---|---|
| Minecraft | 1.20.1 |
| Java | 17 ou plus récent |
| [Fabric Loader](https://fabricmc.net/use/) | 0.15 ou plus récent |
| [Fabric API](https://modrinth.com/mod/fabric-api) | n'importe quelle version pour 1.20.1 (développé avec 0.92.12+1.20.1) |
| [GeckoLib](https://modrinth.com/mod/geckolib) | 4.8.4 ou plus récent, pour 1.20.1 (monstres, boss et machineries animés) |
| [playerAnimator](https://modrinth.com/mod/playeranimator) | 1.0.2-rc1+1.20 ou plus récent, pour 1.20.1 (animations d'attaque du joueur) |

Les quatre mods sont obligatoires : le jeu refuse de démarrer sans eux. [Mod Menu](https://modrinth.com/mod/modmenu) est
facultatif ; il ne fait qu'afficher la description du mod. Embervoid n'a pas d'écran de configuration en jeu : les
réglages vivent dans un fichier (voir [Configuration](configuration.md)).

## Solo

1. Installez Fabric Loader pour 1.20.1.
2. Placez Embervoid, Fabric API, GeckoLib et playerAnimator dans votre dossier `mods`.
3. Lancez le jeu et créez un nouveau monde, ou ouvrez-en un existant. Embervoid fonctionne dans les anciens mondes
   vanilla, mais ses donjons n'apparaissent que dans les chunks générés après son ajout.

## Serveur dédié

Installez les mêmes quatre mods sur le serveur **et** chez chaque client. Un client sans le mod ne peut pas se connecter.

Le `config/embervoid.json` du serveur est celui qui compte : il est envoyé à chaque joueur à sa connexion, et son client
le suit pendant la session. Les réglages purement client (la barre d'endurance, les étiquettes de niveau des monstres,
le volume de la musique des boss, les secousses de caméra) restent propres à chaque joueur.

## Passer à une nouvelle version

Sauvegardez d'abord votre monde, puis remplacez le jar. Embervoid met le monde à niveau au premier démarrage avec la
nouvelle version.

- **De 0.5.0–0.5.2 à 0.5.3, et de 0.4.x à 0.5.x :** le domaine des Marches cendrées est généré de nouveau avec sa
  nouvelle disposition. Votre progression est conservée (boss vaincus, domaines débloqués et terminés), les Vestiges de
  cendres laissés dans le domaine sont déposés sur l'aire d'arrivée du hub, et les joueurs déconnectés à l'intérieur du
  domaine réapparaissent au hub. Tout ce que vous avez construit ou laissé dans des coffres aux Marches cendrées
  disparaît, et les autels du domaine où vous vous étiez reposé sont oubliés : vous repartez de l'aire d'arrivée.
  L'Overworld, le Nether, l'End et le hub ne sont jamais touchés.
- **Depuis 0.4.x :** les Sanctuaires déjà générés gardent leur ancien cœur avec la Vierge cendrée et les obélisques ;
  ils fonctionnent toujours. De nouveaux Sanctuaires en ruine apparaissent dans les chunks neufs.
- **Config :** les clés manquantes sont ajoutées avec leurs valeurs par défaut. Les clés que vous avez déjà gardent leur
  valeur, même quand une nouvelle version change la valeur par défaut (0.5.3 a réajusté les attaques spéciales des
  armes). Supprimez une clé pour reprendre la nouvelle valeur par défaut.

Suite : [Un Overworld plus dur](walkthrough/harder-overworld.md)
