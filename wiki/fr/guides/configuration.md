---
title: Configuration
kind: guide
order: 7
source: 11f390862e36
---

## Où vivent les réglages

| Fichier | Contenu | Qui le lit |
|---|---|---|
| `config/embervoid.json` | les règles de jeu : toutes les clés ci-dessous | le serveur (en solo, votre propre jeu) |
| `config/embervoid-client.json` | vos préférences d'affichage | votre client uniquement |

Les deux fichiers sont créés avec leurs valeurs par défaut au premier démarrage du jeu. Embervoid n'a pas d'écran de
réglages en jeu : modifiez le JSON avec un éditeur de texte **pendant que le jeu ou le serveur est arrêté**, puis
relancez-le. Le fichier est lu au démarrage.

- Une clé manquante est ajoutée avec sa valeur par défaut.
- Une valeur invalide (hors plage, mauvais type) est signalée dans les journaux et remise à sa valeur par défaut.
- Un fichier qui n'est pas du JSON valide est déplacé vers `embervoid.json.broken` et remplacé par les valeurs par défaut.
- Sur un serveur, le `embervoid.json` du serveur est envoyé à chaque joueur qui se connecte et son client le suit
  pendant la session. Les joueurs ne peuvent pas changer les règles avec leur propre fichier.

Quand une nouvelle version change une valeur par défaut, la valeur déjà présente dans votre fichier reste. Supprimez une
clé pour reprendre la nouvelle valeur par défaut.

Un « point entier » de caractéristique suit les plafonds progressifs : les valeurs jusqu'à 20 comptent pleinement, de 21
à 40 pour moitié, de 41 à 99 pour un cinquième.

L'espacement des structures (la fréquence d'apparition des Sanctuaires en ruine) ne se trouve pas dans ce fichier ;
changez-le avec un datapack (voir [Datapacks et compatibilité](datapacks.md)).

## Les clés qui valent d'être changées

- **La dureté de la mort :** `remnant.mode` et `remnant.secondDeath`. Mettez `remnant.secondDeath` sur `burn_all` pour un
  vrai souls-like, ou `remnant.mode` sur `off` pour jouer avec les morts de vanilla.
- **La force des coups des monstres :** `mobs.difficulty.easy`, `normal` et `hard`. Réglez-les sur 0.5, 1.0 et 1.5 pour
  une échelle de difficulté à peu près identique à celle de vanilla.
- **La vitesse de croissance des monstres :** `mobs.levels.healthPerLevel` et `damagePerLevel`, ou `mobs.levels.enabled`
  pour désactiver les niveaux.
- **Les monstres d'autres mods :** ajoutez leurs identifiants d'entité à `mobs.levels.baseLevels`, par exemple
  `"othermod:ogre": 10`.
- **Les nuits de lune :** `moon.enabled` pour les désactiver, `moon.cycleInterval` pour les rendre plus rares ou plus
  fréquentes.
- **Votre fiole :** `flask.baseCharges`, `flask.maxCharges` et `flask.healAmount`.
- **Le rythme de progression :** les trois clés `leveling.cost*`, `leveling.levelCap`.
- **Les boss en groupe :** `boss.healthPerExtraFighter` et `boss.addsPerExtraFighter`.
- **Performances :** `setpieces.animate` sur false arrête toutes les machineries en mouvement.

## Réglages client

Dans `config/embervoid-client.json` :

`staminaBar.hideWhenFull` — masque la barre d'endurance tant que l'endurance est pleine (true par défaut).

`hud.showMobLevel` — affiche « Niv. N » à côté du nom du monstre que vous regardez (true par défaut).

Dans Options → Musique et sons, **Musique des boss** règle le volume des thèmes de boss (100 % par défaut), indépendamment
de Musique. Dans Options → Accessibilité, **Secousses de caméra** règle l'intensité avec laquelle les coups lourds
secouent la caméra, et **Éclats lumineux des coups** active ou désactive les vifs éclairs des coups lourds. Ces réglages
sont enregistrés avec les options de vanilla.

## Toutes les clés

Regroupées par section de `embervoid.json`. Une clé écrite `a.b.c` est la clé `c` dans l'objet `b` dans `a`.

### remnant : Mort et Vestige de cendres

`remnant.mode` — Ce qui va dans le Vestige de cendres à la mort : `xp_and_items`, `xp` (les objets tombent comme dans vanilla) ou `off` (mort vanilla).
{{config remnant.mode}}

`remnant.secondDeath` — Ce qu'une nouvelle mort fait à votre vestige précédent : `burn_xp` (son expérience brûle, ses objets restent) ou `burn_all` (il brûle avec tout).
{{config remnant.secondDeath}}

### stamina : Endurance

`stamina.max` — Réserve d'endurance à l'Endurance de base.
{{config stamina.max}}

`stamina.regenPerTick` — Endurance restaurée par tick une fois la régénération lancée, à l'Endurance de base.
{{config stamina.regenPerTick}}

`stamina.regenDelayTicks` — Ticks qui suivent une dépense d'endurance avant le début de la régénération.
{{config stamina.regenDelayTicks}}

### flask : Fiole de soin

`flask.baseCharges` — Charges d'une Fiole de soin neuve.
{{config flask.baseCharges}}

`flask.maxCharges` — Plafond de charges avec les Éclats de fiole (jamais en dessous de `baseCharges`).
{{config flask.maxCharges}}

`flask.healAmount` — Santé par gorgée, en demi-cœurs.
{{config flask.healAmount}}

`flask.drinkTicks` — Ticks que dure une gorgée.
{{config flask.drinkTicks}}

### stats : Caractéristiques

`stats.vitalityHealth` — Santé max (demi-cœurs) par point entier de Vitalité au-dessus de 10.
{{config stats.vitalityHealth}}

`stats.enduranceStamina` — Endurance max par point entier d'Endurance.
{{config stats.enduranceStamina}}

`stats.enduranceStaminaRegen` — Régénération d'endurance par point entier d'Endurance, en fraction de `stamina.regenPerTick`.
{{config stats.enduranceStaminaRegen}}

`stats.enduranceKnockbackResistance` — Résistance au recul par point entier d'Endurance.
{{config stats.enduranceKnockbackResistance}}

`stats.strengthDamage` — Dégâts des armes de Force par point entier de Force, en fraction.
{{config stats.strengthDamage}}

`stats.dexterityDamage` — Dégâts des armes de Dextérité par point entier de Dextérité, en fraction.
{{config stats.dexterityDamage}}

`stats.dexterityAttackSpeed` — Vitesse d'attaque par point entier de Dextérité, en fraction.
{{config stats.dexterityAttackSpeed}}

`stats.kindlingFlaskHeal` — Soin de la Fiole de soin par point entier d'Ardeur, en fraction.
{{config stats.kindlingFlaskHeal}}

`stats.resilienceToughness` — Résistance de l'armure par point entier de Résilience.
{{config stats.resilienceToughness}}

`stats.resilienceFireReduction` — Dégâts de feu retirés par point entier de Résilience.
{{config stats.resilienceFireReduction}}

`stats.resilienceFireReductionCap` — Le maximum de dégâts de feu que la Résilience peut retirer.
{{config stats.resilienceFireReductionCap}}

`stats.fortuneLuck` — Chance par point entier de Chance.
{{config stats.fortuneLuck}}

`stats.fortuneExtraDrop` — Probabilité, par point entier de Chance, qu'un monstre que vous tuez lâche son butin une seconde fois.
{{config stats.fortuneExtraDrop}}

`stats.fortuneExtraDropCap` — La probabilité maximale de butin supplémentaire.
{{config stats.fortuneExtraDropCap}}

### leveling : Montée de niveau

`leveling.costBase` — Coût du niveau, partie constante : passer du niveau L à L + 1 coûte `costBase + costLinear·L + costQuadratic·L²` points d'expérience.
{{config leveling.costBase}}

`leveling.costLinear` — Coût du niveau, partie qui croît avec L.
{{config leveling.costLinear}}

`leveling.costQuadratic` — Coût du niveau, partie qui croît avec L².
{{config leveling.costQuadratic}}

`leveling.statCap` — Valeur maximale à laquelle une caractéristique peut être montée.
{{config leveling.statCap}}

`leveling.levelCap` — Niveau maximal du personnage ; 0 = pas de plafond.
{{config leveling.levelCap}}

### mobs : Niveaux des monstres et difficulté

`mobs.levels.enabled` — Niveaux des monstres activés ou non. Désactivés : plus de nouveaux niveaux, et les niveaux existants ne font rien.
{{config mobs.levels.enabled}}

`mobs.levels.defaultBaseLevel` — Niveau de base de tout monstre hostile absent de `baseLevels`, vanilla ou modé.
{{config mobs.levels.defaultBaseLevel}}

`mobs.levels.baseLevels` — Niveau de base par identifiant d'entité, p. ex. `"minecraft:zombie": 3`. Ajoutez ici les monstres d'autres mods.
{{config mobs.levels.baseLevels}}

`mobs.levels.blocksPerLevel` — Overworld : +1 niveau par tant de blocs depuis le point d'apparition du monde.
{{config mobs.levels.blocksPerLevel}}

`mobs.levels.maxDistanceBonus` — Overworld : le maximum de niveaux que la distance peut ajouter.
{{config mobs.levels.maxDistanceBonus}}

`mobs.levels.belowZeroBonus` — Overworld : niveaux supplémentaires sous y = 0.
{{config mobs.levels.belowZeroBonus}}

`mobs.levels.dimensionBonus` — Niveaux supplémentaires par identifiant de dimension (le Nether et l'End par défaut). Ajoutez ici les dimensions d'autres mods.
{{config mobs.levels.dimensionBonus}}

`mobs.levels.healthPerLevel` — Santé par niveau au-dessus de 1, en fraction de la base.
{{config mobs.levels.healthPerLevel}}

`mobs.levels.damagePerLevel` — Dégâts par niveau au-dessus de 1, en fraction de la base.
{{config mobs.levels.damagePerLevel}}

`mobs.levels.experiencePerLevel` — Expérience lâchée par niveau au-dessus de 1, en fraction de la base.
{{config mobs.levels.experiencePerLevel}}

`mobs.levels.healthScale` — Un facteur appliqué à la santé de base d'un type de monstre avant son niveau, par identifiant d'entité (absent : 1).
{{config mobs.levels.healthScale}}

`mobs.levels.damageScale` — Un facteur appliqué aux dégâts de base d'un type de monstre avant son niveau, par identifiant d'entité (absent : 1).
{{config mobs.levels.damageScale}}

`mobs.difficulty.easy` — Facteur de dégâts des monstres hostiles contre les joueurs en Facile (remplace celui de vanilla).
{{config mobs.difficulty.easy}}

`mobs.difficulty.normal` — Le même en Normal.
{{config mobs.difficulty.normal}}

`mobs.difficulty.hard` — Le même en Difficile.
{{config mobs.difficulty.hard}}

### sanctum : Sanctuaire en ruine

`sanctum.mapChanceCartographer` — Probabilité d'une Carte du Sanctuaire dans un coffre de cartographe de village.
{{config sanctum.mapChanceCartographer}}

`sanctum.mapChanceTemple` — La même dans un coffre de temple de village.
{{config sanctum.mapChanceTemple}}

`sanctum.mapChanceRuinedPortal` — La même dans un coffre de portail en ruine.
{{config sanctum.mapChanceRuinedPortal}}

`sanctum.mapChanceShipwreck` — La même dans un coffre à carte d'épave.
{{config sanctum.mapChanceShipwreck}}

`sanctum.mapSearchRadius` — Jusqu'où, en chunks, une nouvelle Carte du Sanctuaire cherche un Sanctuaire.
{{config sanctum.mapSearchRadius}}

`sanctum.hallGuardChance` — Probabilité qu'un emplacement de garde d'une salle latérale contienne une Carcasse, tirée à la génération du monde.
{{config sanctum.hallGuardChance}}

`sanctum.hallArcherChance` — Probabilité qu'un emplacement d'archer d'une salle latérale contienne un squelette, tirée à la génération du monde.
{{config sanctum.hallArcherChance}}

### sanctumGear : Armes du Sanctuaire

`sanctumGear.cleaveRange` — Taille large (Épée longue du Sanctuaire) : allonge en blocs.
{{config sanctumGear.cleaveRange}}

`sanctumGear.cleaveArc` — Taille large : arc devant vous, en degrés.
{{config sanctumGear.cleaveArc}}

`sanctumGear.cleaveMultiplier` — Taille large : part du coup de l'arme que subit chaque cible.
{{config sanctumGear.cleaveMultiplier}}

`sanctumGear.cleaveStamina` — Taille large : coût en endurance.
{{config sanctumGear.cleaveStamina}}

`sanctumGear.cleaveCooldown` — Taille large : recharge en ticks.
{{config sanctumGear.cleaveCooldown}}

`sanctumGear.slamRadius` — Frappe au sol (Masse du porte-cendre) : rayon de l'anneau.
{{config sanctumGear.slamRadius}}

`sanctumGear.slamMultiplier` — Frappe au sol : part du coup de l'arme.
{{config sanctumGear.slamMultiplier}}

`sanctumGear.slamKnockback` — Frappe au sol : force du recul.
{{config sanctumGear.slamKnockback}}

`sanctumGear.slamSlownessTicks` — Frappe au sol : durée de Lenteur en ticks.
{{config sanctumGear.slamSlownessTicks}}

`sanctumGear.slamSlownessAmplifier` — Frappe au sol : amplificateur de Lenteur (1 = Lenteur II).
{{config sanctumGear.slamSlownessAmplifier}}

`sanctumGear.slamStamina` — Frappe au sol : coût en endurance.
{{config sanctumGear.slamStamina}}

`sanctumGear.slamCooldown` — Frappe au sol : recharge en ticks.
{{config sanctumGear.slamCooldown}}

### boss : Boss

`boss.healthPerExtraFighter` — Bonus de santé du boss par combattant au-delà du premier : santé × (1 + ceci × (combattants − 1)).
{{config boss.healthPerExtraFighter}}

`boss.addsPerExtraFighter` — Sbires supplémentaires par combattant au-delà du premier.
{{config boss.addsPerExtraFighter}}

`boss.addLevelOffset` — De combien de niveaux les sbires sont en dessous du boss.
{{config boss.addLevelOffset}}

`boss.targetSwitchMinSeconds` — Délai minimal entre deux changements de cible d'un boss (à deux combattants ou plus).
{{config boss.targetSwitchMinSeconds}}

`boss.targetSwitchMaxSeconds` — Délai maximal entre deux changements de cible.
{{config boss.targetSwitchMaxSeconds}}

`boss.repeatSoulChance` — Probabilité d'obtenir l'âme du boss pour un combattant qui l'avait déjà vaincu (jamais lors d'une revanche).
{{config boss.repeatSoulChance}}

### reachMobs : Apparitions des Marches cendrées

`reachMobs.spawns` — Apparitions naturelles des monstres des Marches cendrées dans leur zone : `weight`, `minGroup` et `maxGroup` par monstre. Un poids de 0 désactive l'apparition d'un monstre. Lu au démarrage.
{{config reachMobs.spawns}}

### gear : Équipement de domaine

`gear.bleeding.durationTicks` — Durée du Saignement ; chaque nouveau coup le relance.
{{config gear.bleeding.durationTicks}}

`gear.bleeding.maxLevel` — Niveau maximal de Saignement.
{{config gear.bleeding.maxLevel}}

`gear.bleeding.damagePerLevel` — Dégâts de Saignement par seconde et par niveau, sans tenir compte de l'armure.
{{config gear.bleeding.damagePerLevel}}

`gear.smoldering.fireReductionPerPiece` — Part des dégâts de feu que retire chaque pièce portée de l'Armure rougeoyante.
{{config gear.smoldering.fireReductionPerPiece}}

`gear.sets.reaperSpeed` — Ensemble du moissonneur : bonus de vitesse de déplacement (part de la base).
{{config gear.sets.reaperSpeed}}

`gear.sets.huntsmanProjectile` — Ensemble du veneur : bonus de dégâts de projectiles.
{{config gear.sets.huntsmanProjectile}}

`gear.sets.wardenKnockbackResist` — Ensemble du geôlier : résistance au recul ajoutée.
{{config gear.sets.wardenKnockbackResist}}

`gear.sets.wardenBlockingReduction` — Ensemble du geôlier : part des dégâts retirée en parant avec un bouclier.
{{config gear.sets.wardenBlockingReduction}}

`gear.uniques.scarecrowDetection` — Masque d'épouvantail : part de la distance habituelle à laquelle les monstres vous remarquent.
{{config gear.uniques.scarecrowDetection}}

`gear.uniques.greavesSpeedTicks` — Grèves du veneur : ticks de vitesse après une mise à mort.
{{config gear.uniques.greavesSpeedTicks}}

`gear.uniques.greavesSafeFallDamage` — Grèves du veneur : dégâts de chute retirés (4 = aucun jusqu'à 7 blocs).
{{config gear.uniques.greavesSafeFallDamage}}

`gear.uniques.novaHealthFraction` — Cuirasse cœur-de-fournaise : la nova se déclenche sous cette part de votre santé max.
{{config gear.uniques.novaHealthFraction}}

`gear.uniques.novaRadius` — Cuirasse cœur-de-fournaise : rayon de la nova.
{{config gear.uniques.novaRadius}}

`gear.uniques.novaDamage` — Cuirasse cœur-de-fournaise : dégâts de braise à chaque monstre dans la nova.
{{config gear.uniques.novaDamage}}

`gear.uniques.novaCooldownTicks` — Cuirasse cœur-de-fournaise : ticks entre deux novas.
{{config gear.uniques.novaCooldownTicks}}

`gear.weapons.kindlingPerPoint` — Bonus de dégâts de braise par point entier d'Ardeur.
{{config gear.weapons.kindlingPerPoint}}

`gear.weapons.sweepRadius` — Faux cendrée : rayon du balayage autour de la cible.
{{config gear.weapons.sweepRadius}}

`gear.weapons.sweepShare` — Faux cendrée : part du coup que le balayage inflige.
{{config gear.weapons.sweepShare}}

`gear.weapons.sweepStamina` — Faux cendrée : endurance rendue par ennemi touché par un balayage ou une Moisson.
{{config gear.weapons.sweepStamina}}

`gear.weapons.waveLength` — Faux cendrée : longueur de la vague de cendre d'un coup pleinement chargé.
{{config gear.weapons.waveLength}}

`gear.weapons.waveWidth` — Faux cendrée : largeur de la vague de cendre.
{{config gear.weapons.waveWidth}}

`gear.weapons.waveDamage` — Faux cendrée : dégâts de braise de la vague de cendre.
{{config gear.weapons.waveDamage}}

`gear.weapons.harvestRadius` — Moisson : rayon de la rotation.
{{config gear.weapons.harvestRadius}}

`gear.weapons.harvestStamina` — Moisson : coût en endurance.
{{config gear.weapons.harvestStamina}}

`gear.weapons.harvestCooldown` — Moisson : recharge en ticks.
{{config gear.weapons.harvestCooldown}}

`gear.weapons.lashPull` — Coup de fouet : nombre de blocs dont la cible est attirée.
{{config gear.weapons.lashPull}}

`gear.weapons.lashStamina` — Coup de fouet : coût en endurance.
{{config gear.weapons.lashStamina}}

`gear.weapons.lashCooldown` — Coup de fouet : recharge en ticks.
{{config gear.weapons.lashCooldown}}

`gear.weapons.lungeDash` — Fente : longueur de l'élan en blocs.
{{config gear.weapons.lungeDash}}

`gear.weapons.lungeStamina` — Fente : coût en endurance.
{{config gear.weapons.lungeStamina}}

`gear.weapons.lungeCooldown` — Fente : recharge en ticks.
{{config gear.weapons.lungeCooldown}}

`gear.weapons.rampPerHit` — Rapière de cendre royale : dégâts ajoutés par coup à pleine charge enchaîné sur la même cible.
{{config gear.weapons.rampPerHit}}

`gear.weapons.rampMax` — Rapière de cendre royale : le maximum qu'ajoute la montée en puissance.
{{config gear.weapons.rampMax}}

`gear.weapons.rampWindowTicks` — Rapière de cendre royale : plus long intervalle entre deux coups qui conserve la montée en puissance.
{{config gear.weapons.rampWindowTicks}}

`gear.weapons.whipTipBand` — Fléau du geôlier : longueur de la pointe, les derniers blocs du fouet, qui claque.
{{config gear.weapons.whipTipBand}}

`gear.weapons.whipTipMultiplier` — Fléau du geôlier : facteur de dégâts d'un coup de pointe.
{{config gear.weapons.whipTipMultiplier}}

`gear.weapons.whipTipStunTicks` — Fléau du geôlier : un coup de pointe arrête le monstre et interrompt sa préparation d'attaque pendant ce nombre de ticks.
{{config gear.weapons.whipTipStunTicks}}

`gear.crossbow.chargeTicks` — Arbalète du veneur : temps de chargement sans Charge rapide.
{{config gear.crossbow.chargeTicks}}

`gear.crossbow.boltDamage` — Arbalète du veneur : dégâts d'un carreau à pleine vitesse.
{{config gear.crossbow.boltDamage}}

`gear.crossbow.punch` — Arbalète du veneur : niveau de recul des carreaux.
{{config gear.crossbow.punch}}

`gear.crossbow.pierce` — Arbalète du veneur : monstres supplémentaires qu'un carreau traverse.
{{config gear.crossbow.pierce}}

`gear.crossbow.openerMultiplier` — Arbalète du veneur : facteur de dégâts sur un monstre qui ne vous cible pas.
{{config gear.crossbow.openerMultiplier}}

`gear.consumables.bombRadius` — Bombe de cendre : rayon de l'anneau de feu.
{{config gear.consumables.bombRadius}}

`gear.consumables.bombDamage` — Bombe de cendre : dégâts de braise.
{{config gear.consumables.bombDamage}}

`gear.consumables.bombFireSeconds` — Bombe de cendre : secondes pendant lesquelles brûlent les monstres touchés.
{{config gear.consumables.bombFireSeconds}}

`gear.consumables.resinSeconds` — Résine rougeoyante : durée pendant laquelle une arme enduite enflamme ce qu'elle touche.
{{config gear.consumables.resinSeconds}}

`gear.consumables.resinFireSeconds` — Résine rougeoyante : secondes pendant lesquelles brûle une cible.
{{config gear.consumables.resinFireSeconds}}

`gear.consumables.balmRegenTicks` — Baume d'ambre : durée de sa Régénération.
{{config gear.consumables.balmRegenTicks}}

`gear.consumables.oilMinutes` — Huile de lanterne ambrée : durée pendant laquelle brûle une lanterne.
{{config gear.consumables.oilMinutes}}

`gear.consumables.whetstoneShare` — Pierre à aiguiser : part de la durabilité max réparée.
{{config gear.consumables.whetstoneShare}}

`gear.consumables.cinderFuelTicks` — Éclat de scorie comme combustible de four, en ticks (le charbon vaut 1600). Lu au démarrage.
{{config gear.consumables.cinderFuelTicks}}

`gear.consumables.flaskEmberBonus` — Braise de fiole : soin supplémentaire de la fiole.
{{config gear.consumables.flaskEmberBonus}}

`gear.loot.fodderMaterial` — Probabilité d'obtenir le matériau de la zone sur un monstre naturel des Marches cendrées.
{{config gear.loot.fodderMaterial}}

`gear.loot.eliteMaterial` — Probabilité d'obtenir 1 à 2 matériaux de la zone sur un tank placé ou une élite.
{{config gear.loot.eliteMaterial}}

`gear.loot.fodderSet` — Probabilité d'une pièce d'ensemble sur un monstre naturel (mises à mort par un joueur uniquement).
{{config gear.loot.fodderSet}}

`gear.loot.eliteSet` — Probabilité d'une pièce d'ensemble sur un tank placé ou une élite.
{{config gear.loot.eliteSet}}

`gear.loot.fodderUnique` — Probabilité de la pièce unique de la zone sur ses monstres.
{{config gear.loot.fodderUnique}}

`gear.loot.eliteUnique` — Probabilité de la pièce unique de la zone sur son élite.
{{config gear.loot.eliteUnique}}

`gear.loot.lootingBonus` — Probabilité ajoutée par niveau de Butin (un quart pour les pièces uniques).
{{config gear.loot.lootingBonus}}

### reliquary : Reliquaire

`reliquary.slots` — Emplacements du Reliquaire par type : `artifact` et `ring` (0 masque un type, 8 au maximum).
{{config reliquary.slots}}

`reliquary.artifacts.points` — Points de caractéristique d'un artefact à une seule caractéristique.
{{config reliquary.artifacts.points}}

`reliquary.artifacts.sealPoints` — Points que le Sceau du Roi cendré ajoute à chaque caractéristique.
{{config reliquary.artifacts.sealPoints}}

`reliquary.rings.courierSprintBonus` — Anneau du coursier : vitesse de sprint supplémentaire.
{{config reliquary.rings.courierSprintBonus}}

`reliquary.rings.featherSafeFall` — Anneau de plumes de cendre : les chutes jusqu'à ce nombre de blocs ne font aucun dégât.
{{config reliquary.rings.featherSafeFall}}

`reliquary.rings.featherMaxFallSpeed` — Anneau de plumes de cendre : chute la plus rapide, en blocs par tick.
{{config reliquary.rings.featherMaxFallSpeed}}

`reliquary.rings.fogStalkerClearing` — Anneau du traqueur de brume : nombre de fois où la brume du domaine commence et finit plus loin.
{{config reliquary.rings.fogStalkerClearing}}

`reliquary.rings.delverHasteLevel` — Anneau du fouisseur : niveau de Célérité en minant les minerais du domaine.
{{config reliquary.rings.delverHasteLevel}}

`reliquary.rings.embersFireSeconds` — Anneau de braises : secondes pendant lesquelles un coup de mêlée enflamme la cible.
{{config reliquary.rings.embersFireSeconds}}

`reliquary.drops.chestArtifact` — Probabilité d'un artefact dans un coffre au trésor de zone.
{{config reliquary.drops.chestArtifact}}

`reliquary.drops.chestRing` — Probabilité d'un anneau dans un coffre au trésor de zone.
{{config reliquary.drops.chestRing}}

`reliquary.drops.eliteArtifact` — Probabilité d'un artefact sur l'élite d'une zone (mises à mort par un joueur).
{{config reliquary.drops.eliteArtifact}}

`reliquary.drops.eliteRing` — Probabilité d'un anneau sur l'élite d'une zone (mises à mort par un joueur).
{{config reliquary.drops.eliteRing}}

### moon : Nuits de lune

`moon.enabled` — Nuits de lune activées ou non (la commande de débogage peut toujours en lancer une).
{{config moon.enabled}}

`moon.cycleInterval` — Un cycle de nuit de lune tous les tant de cycles lunaires de 8 jours ; la première pleine lune du monde ne compte jamais.
{{config moon.cycleInterval}}

`moon.fallbackMinutes` — Minutes en temps réel au bout desquelles une nuit de lune se termine même si ses vagues ne sont pas vaincues.
{{config moon.fallbackMinutes}}

`moon.fullMoon.levelBonus` — Niveaux supplémentaires des monstres hostiles de l'Overworld qui apparaissent pendant une pleine lune.
{{config moon.fullMoon.levelBonus}}

`moon.fullMoon.followRange` — Multiplicateur de portée de poursuite des monstres hostiles de l'Overworld pendant une pleine lune.
{{config moon.fullMoon.followRange}}

`moon.blood.vampirism` — Part des dégâts qu'un monstre hostile inflige à un joueur qui le soigne pendant une nouvelle lune de sang.
{{config moon.blood.vampirism}}

### nightMobs : Vagues de pleine lune

`nightMobs.zombieLevelBonus` — Niveaux qu'un Zombie cendreux d'une vague de pleine lune reçoit en plus du niveau de l'endroit.
{{config nightMobs.zombieLevelBonus}}

`nightMobs.ghostMissChance` — Part des attaques contre un Spectre qui le traversent.
{{config nightMobs.ghostMissChance}}

`nightMobs.maxZombies` — Maximum de Zombies cendreux dans la vague d'un joueur (un par cumul de Malédiction de la nuit).
{{config nightMobs.maxZombies}}

`nightMobs.ghostCount` — Spectres envoyés à un joueur sans malédiction.
{{config nightMobs.ghostCount}}

`nightMobs.spawnMinDistance` — Nombre minimal de blocs entre le joueur et l'apparition d'une vague.
{{config nightMobs.spawnMinDistance}}

`nightMobs.spawnMaxDistance` — Nombre maximal de blocs entre le joueur et l'apparition d'une vague.
{{config nightMobs.spawnMaxDistance}}

### vampire : Le Vampire

`vampire.provokeMinutes` — Minutes en temps réel avant que le Vampire le plus proche d'un joueur ne se transforme si personne n'en a frappé un.
{{config vampire.provokeMinutes}}

`vampire.groupRadius` — Les joueurs aussi proches les uns des autres partagent un même groupe de Vampires.
{{config vampire.groupRadius}}

`vampire.groupMin` — Nombre minimal de Vampires dans un groupe.
{{config vampire.groupMin}}

`vampire.groupMax` — Nombre maximal de Vampires dans un groupe.
{{config vampire.groupMax}}

`vampire.bossMinLevel` — Niveau minimal du boss Vampire.
{{config vampire.bossMinLevel}}

`vampire.bossCapNoRealm` — Niveau maximal du boss tant qu'aucun participant n'a débloqué de domaine.
{{config vampire.bossCapNoRealm}}

`vampire.firstKillShards` — Éclats de fiole pour la première mise à mort d'un Vampire par un joueur (plus un Croc de vampire).
{{config vampire.firstKillShards}}

`vampire.repeatShardChance` — Probabilité d'un Éclat de fiole lors des mises à mort suivantes.
{{config vampire.repeatShardChance}}

### setpieces : Machineries

`setpieces.maxPerChunk` — Maximum de machineries en mouvement (ailes de moulin, cloches, encensoirs…) qu'un chunk peut contenir ; les surplus deviennent de l'air.
{{config setpieces.maxPerChunk}}

`setpieces.animate` — Si les machineries bougent ou non. Désactivé : elles restent en position de repos. Synchronisé avec les clients.
{{config setpieces.animate}}

Suite : [Datapacks et compatibilité](datapacks.md)
