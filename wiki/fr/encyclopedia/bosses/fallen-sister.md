---
entities: [embervoid:fallen_sister, embervoid:crying_soul]
kind: boss
source: ea0d7d197364
---

{{stats}}

## Comment l'atteindre

La Sœur déchue attend au fond de chaque [Sanctuaire en ruine](../places/ruined-sanctum.md). Frayez-vous un chemin dans
la cage d'escalier et à travers les salles gardées jusqu'à la porte du boss. Un
[Autel de braise](../blocks/functional/ember-shrine.md) se dresse devant : reposez-vous-y avant d'entrer. Puis
franchissez le rideau de brume lumineuse pour pénétrer dans l'arène, une salle sombre avec une
[Stèle fendue](../blocks/functional/cleft-stele.md) froide sur l'estrade du fond.

Elle apparaît dès que vous vous avancez au milieu de la salle. Un mur de brume se referme derrière vous : personne ne
sort et personne d'autre n'entre avant la fin du combat. Si vous mourez ou en sortez autrement, elle disparaît, et la
prochaine fois que vous entrez, elle recommence avec toute sa vie.

La Sœur de chaque Sanctuaire ne tombe qu'une fois par monde. Ensuite, le donjon est nettoyé et elle n'y revient jamais ;
il n'y a pas de revanche. Pour une autre [Âme éplorée](../items/souls/crying-soul.md), trouvez un autre Sanctuaire.

Elle est la gardienne du seuil du mod et est conçue pour être vaincue par un personnage débutant en armure de fer avec
une épée en fer.

## Phases et attaques

Chaque attaque est marquée au sol avant de frapper. Surveillez les flammes sur le sol, pas son corps.

### Phase 1

- **Arcs de l'encensoir.** La chaîne s'enroule autour de son bras, puis elle balance l'encensoir en un large cône
  devant elle, de droite à gauche, et aussitôt en sens inverse. Le premier coup a la préparation la plus longue ; le
  second suit rapidement. Reculez hors du cône, ou passez derrière elle, et attendez les deux coups avant d'engager.
- **Frappe.** Elle abat l'encensoir sur un anneau de flammes juste devant elle. Vous êtes projeté en l'air, et une
  flaque de braises continue de brûler à cet endroit pendant quelques secondes <!-- FallenSisterEntity.PUDDLE_TICKS -->.
  Sortez de l'anneau pendant la préparation et ne combattez pas dans la flaque.
- **Pas en arrière.** Tenez-vous tout près derrière elle et elle se tourne vers vous puis bondit en arrière. Cela ne
  fait aucun dégât, mais interrompt votre enchaînement. Frappez-la de côté, pas directement par-derrière.

### Phase 2 (sous la moitié de sa vie)

- **Voile en feu.** Son voile prend feu. Pendant deux secondes <!-- FallenSisterEntity.VEIL_BURN_TICKS --> elle reste
  immobile et ne subit aucun dégât : ne gaspillez pas votre endurance sur elle, reculez et préparez-vous.
- **Âmes éplorées.** Trois âmes tournoient autour de sa tête <!-- FallenSisterEntity.FIRST_FLOCK -->. L'une après
  l'autre, une âme se cabre en poussant un cri tandis qu'une ligne de feu d'âme marque le sol d'elle jusqu'à vous, puis
  plonge le long de cette ligne. Écartez-vous sur le côté. Après le plongeon, l'âme reste étourdie près du sol pendant
  deux secondes <!-- CryingSoulEntity.DAZED_TICKS --> ; n'importe quel coup la tue. Quand toutes les âmes ont disparu,
  une nouvelle volée de quatre <!-- FallenSisterEntity.LATER_FLOCK --> arrive environ toutes les 20 secondes
  <!-- FallenSisterEntity.FLOCK_INTERVAL -->.
- **Toupie de l'encensoir.** Un anneau de flammes autour d'elle, puis elle fait tournoyer l'encensoir autour d'elle sur
  toute la longueur de sa chaîne. C'est son coup le plus dur, et il vous projette en arrière. Quittez l'anneau dès que
  vous le voyez. Elle l'emploie plus souvent quand deux joueurs ou plus se tiennent près d'elle.
- Les arcs et la frappe restent ; elle frappe plus souvent qu'avant.

Les braseros de la salle s'allument un à un à mesure qu'elle perd de la vie. Ils brûlent ensuite pour de bon quand elle
est vaincue.

## Butin

{{drops}}

Elle n'a pas de table de butin propre. Tout arrive directement dans l'inventaire de chaque joueur présent dans le
combat à sa mort :

- À **chaque** victoire, chaque joueur du combat reçoit une {{item embervoid:crying_soul}}, même s'il a déjà vaincu une
  autre Sœur. Utilisez-la sur une Stèle fendue pour allumer la voie vers le [Sanctuaire des Braises](../places/ember-sanctum.md).
- À votre **première** victoire, vous recevez aussi un {{item embervoid:flask_shard}} et une arme du Sanctuaire,
  l'{{item embervoid:sanctum_longsword}} ou la {{item embervoid:ashbearers_mace}}, plus quelques tirages de fer, d'or,
  de livres, de fioles d'expérience, de pommes dorées et, rarement, un diamant.

## Stratégie

**Au corps à corps.** Restez sur son flanc, juste hors du cône des arcs. Après le deuxième arc ou une frappe, elle a une
courte récupération : deux ou trois coups, puis écartez-vous. Gardez un coup d'épée prêt pour chaque âme étourdie en
phase 2 ; les tuer vite limite les plongeons.

**À distance.** Un arc fonctionne bien en phase 1, car ses attaques ne portent que de quelques blocs. Continuez de vous
déplacer dans la salle pour qu'elle marche au lieu de frapper. En phase 2, les âmes vous poursuivent de toute façon :
approchez-vous donc et tuez-les à la sortie de leur plongeon.

**En groupe.** Elle a plus de vie pour chaque combattant en plus, mais pas de sbires. Dispersez-vous : la toupie punit un
groupe massé contre elle. Un joueur retient son attention de face pendant que les autres frappent ses flancs, et l'un
d'eux se charge de tuer les âmes. Tout le monde dans le combat reçoit une âme : emmenez donc ceux qui en ont encore
besoin.
