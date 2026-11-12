---
title: Le Reliquaire
kind: mechanic
order: 8
source: 19d00b168d83
---

Le Reliquaire contient les petits objets que vous portez en plus de l'armure : des **artefacts** et des **anneaux**. Ils
viennent tous des Marches cendrées.

## L'ouvrir

Appuyez sur **R** (reconfigurez la touche dans Contrôles sous « Ouvrir le Reliquaire »), ou cliquez sur le bouton du
Reliquaire à côté du livre de recettes dans votre inventaire. L'écran montre votre personnage avec les emplacements
d'accessoires à côté.

Vous avez **2 emplacements d'artefact** et **2 emplacements d'anneau** <!-- config reliquary.slots.artifact / reliquary.slots.ring -->.
Vous ne pouvez pas porter deux fois le même objet.

## Artefacts : points de caractéristique

Chaque artefact ajoute des points de caractéristique tant que vous le portez :

| Artefact | Donne | Trouvé dans |
|---|---|---|
| {{item embervoid:last_harvest_grain}} | +3 Vitalité | Champs cendrés |
| {{item embervoid:ploughmans_horseshoe}} | +3 Endurance | Champs cendrés |
| {{item embervoid:royal_hound_fang}} | +3 Dextérité | Bois royaux |
| {{item embervoid:amber_tear}} | +3 Ardeur | Bois royaux |
| {{item embervoid:gravestone_medallion}} | +3 Résilience | Nécropole engloutie |
| {{item embervoid:ferrymans_coin}} | +3 Chance | Nécropole engloutie |
| {{item embervoid:wardens_chain}} | +3 Force | Nécropole engloutie |
| {{item embervoid:seal_of_the_ashen_king}} | +1 à chaque caractéristique | Citadelle rougeoyante |

<!-- config reliquary.artifacts.points / sealPoints -->

Les points d'artefact comptent pour tout ce que fait une caractéristique, mais ils n'augmentent ni votre niveau, ni le
coût du niveau, ni le plafond de caractéristique. L'écran de montée de niveau les affiche à côté de chaque
caractéristique.

## Anneaux : effets

| Anneau | Effet | Trouvé dans |
|---|---|---|
| {{item embervoid:couriers_ring}} | +15 % de vitesse de sprint | Champs cendrés |
| {{item embervoid:ring_of_ash_feathers}} | chute plus lente, aucun dégât de chute sous 8 blocs (accroupissez-vous pour tomber normalement) | Champs cendrés |
| {{item embervoid:ring_of_the_fog_stalker}} | la brume du domaine se dissipe deux fois plus loin (pas dans les arènes de boss) | Bois royaux |
| {{item embervoid:delvers_ring}} | Célérité I en minant les minerais du domaine | Bois royaux |
| {{item embervoid:drowned_mans_ring}} | respiration aquatique | Nécropole engloutie |
| {{item embervoid:ring_of_embers}} | les coups de mêlée enflamment la cible pendant 2 secondes | Citadelle rougeoyante |

<!-- config reliquary.rings.* -->

## Où ils tombent

Chaque zone ne lâche que ses propres accessoires :

- son **coffre au trésor** : un artefact 8 % du temps, un anneau 5 % ;
- son **élite**, tuée par un joueur : un artefact 2 %, un anneau 1 %. Les élites sont le Charognard cendré (Champs), le
  Sonneur des tombes (Nécropole) et le Chevalier rougeoyant (Citadelle). Les Bois royaux n'ont pas d'élite : leurs
  accessoires ne viennent que du coffre du pavillon de chasse.

<!-- config reliquary.drops.* -->

## À la mort

Les accessoires portés vont dans votre [Vestige de cendres](death-and-remnant.md) avec le reste de vos objets, et
reviennent à leurs emplacements quand vous le récupérez. Si le mode vestige du serveur laisse les objets au sol, ils
tombent avec votre inventaire. Avec `keepInventory`, vous les gardez.

## Réglages

{{config reliquary.slots}}
{{config reliquary.artifacts.points}}
{{config reliquary.drops.chestArtifact}}

Suite : [Ensembles d'armure](armor-sets.md)
