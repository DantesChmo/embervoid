---
entity: embervoid:smoldering_lord
kind: boss
source: 79ca67c69bba
---

{{stats}}

## Comment l'atteindre

Le Seigneur rougeoyant est le boss du domaine des [Marches cendrées](../places/ashen-reach.md) : le roi cupide, haut de
trois blocs et demi, en armure ardente, armé d'une épée à deux mains. Il attend dans la cour sous le mur du donjon, au
cœur de la [Citadelle rougeoyante](../places/smoldering-citadel.md).

Pour y arriver, vous devez d'abord vaincre les trois gardiens de zone : le [Colosse de cendre](ash-colossus.md),
l'[Arbre endeuillé](grieving-tree.md) et le [Gardien de la crypte](crypt-keeper.md). Alors la
[Porte de la citadelle](../blocks/functional/citadel-gate.md), du côté sud du [Carrefour](../places/crossroads.md),
vous laisse passer. Suivez la route de la porte vers le sud à travers la cité en flammes : les faubourgs, la rue et le
marché, le corps de garde dans le rempart, puis l'anneau intérieur. La cour se trouve à environ 300 blocs de l'aire
d'arrivée, devant le donjon. Le premier autel de la Citadelle se dresse là où s'arrête la route de la porte ; un autre
se dresse devant le mur de brume de la cour et reste froid tant que vous ne l'avez pas vaincu.

Entrez dans la cour et le combat commence. Un mur de brume la ferme, et sa propre musique retentit. Si vous mourez ou
partez, le combat se réinitialise.

## Phases et attaques

### Phase 0 : sur le rempart

Il commence en hauteur sur le mur du donjon avec deux [Tireurs étincelants](../mobs/ashen-reach/sparking-marksman.md) à
ses côtés. De là-haut, on ne peut pas le blesser. Toutes les trois secondes <!-- SmolderingLordEntity.WALL_ATTACK_INTERVAL -->
il attaque :

- **Anneaux de mortier.** Un anneau marque le sol sous deux combattants au plus ; deux secondes plus tard
  <!-- SmolderingLordEntity.MORTAR_WINDUP --> le feu tombe là et vous enflamme. Continuez de bouger et vous n'êtes jamais
  dessous.
- **Ligne de flammes.** Une ligne court du pied du mur à travers toute la cour vers un combattant, puis éclate en feu et
  continue de brûler quelques secondes. Écartez-vous et ne la traversez pas tant qu'elle brûle.

Trois **braseros de signal** se dressent sur des piédestaux dans la cour. Allumez-les tous les trois (briquet, boule de
feu ou flèche enflammée) et il descend : un large anneau <!-- SmolderingLordEntity.LANDING_RADIUS --> marque le milieu
de la cour, et il y atterrit avec une explosion. Ne vous tenez pas dedans.

### Phase 1

- **Enchaînement d'épée à deux mains.** Deux coups dans un cône devant lui qui vous enflamment, puis une frappe sur un
  anneau juste devant lui. La frappe traverse un bouclier levé et le brise. Reculez hors du cône, puis hors de l'anneau.
- **Fente.** Contre une cible plus éloignée, il se fend le long d'une ligne marquée et laisse une traînée de feu
  dessus. Écartez-vous de la ligne et ne marchez pas dans la traînée.

Les Tireurs restent sur le rempart pendant tout le combat. Abattez-les ou gardez un pilier entre eux et vous.

### Phase 2 (sous la moitié de sa vie)

Son armure éclate. Il subit désormais un quart de dégâts en plus <!-- SmolderingLordEntity.SHATTERED_DAMAGE -->, mais
il se déplace et attaque plus vite. Environ toutes les 25 secondes <!-- SmolderingLordEntity.HAZARD_INTERVAL -->, il
déchaîne tour à tour le fléau d'un gardien de zone :

1. **La tempête de cendre du Colosse.** Six secondes de dégâts et de cécité pour tous ceux qu'il voit. Rompez la ligne de
   vue : un piédestal ou un mur suffit.
2. **La brume du Bosquet.** Une épaisse brume rouge-brun pendant vingt secondes <!-- SmolderingLordEntity.FOG_TICKS -->.
   Repérez où il se trouve avant qu'elle ne se referme.
3. **L'obscurité de la Nécropole.** Obscurité pour tout le monde pendant quinze secondes <!-- SmolderingLordEntity.DARK_TICKS -->.
   Tant qu'elle dure, il subit la moitié des dégâts sauf s'il se tient dans une lumière de 8 ou plus
   <!-- SmolderingLordEntity.LIT_LEVEL -->. Les braseros de signal brûlent toujours : attirez-le près de l'un d'eux.

L'enchaînement et la fente restent.

## Butin

{{drops}}

Lors de votre première victoire, ces objets arrivent aussi directement dans votre inventaire :

- l'{{item embervoid:smoldering_lord_soul}}, qui se place dans la {{item embervoid:royal_ash_rapier}} ;
- une {{item embervoid:flask_ember}}.

Le vaincre achève les Marches cendrées pour tous les joueurs du combat : l'obélisque du domaine dans le
[Sanctuaire des Braises](../places/ember-sanctum.md) est embrasé, et l'obélisque suivant s'allume (le domaine suivant
n'est pas encore construit). Aider un ami plus tard peut de temps en temps rapporter à nouveau l'âme
<!-- boss.repeatSoulChance --> ; un combat lancé avec de la {{item embervoid:ash_of_rematch}} ne le fait jamais.

## Stratégie

**Phase 0.** Ne restez pas immobile. Allumez d'abord le brasero le plus proche, puis les autres, en vous déplaçant entre
les marques. Un arc avec des flèches enflammées les allume depuis où vous êtes. Si vous le pouvez, tuez les Tireurs d'en
bas au passage ; ils continuent de tirer pendant tout le reste du combat.

**Au corps à corps.** Restez sur son flanc et esquivez chaque enchaînement, puis frappez-le pendant la récupération
après la frappe. Quand il se fend, écartez-vous sur le côté et punissez la fin de l'attaque. En phase 2, combattez-le
près d'un brasero de signal pour que l'obscurité ne réduise pas vos dégâts de moitié, et gardez un piédestal à portée
pour la tempête de cendre.

**À distance.** Gardez-le à moyenne distance et il se fend, ce qui est facile à lire mais laisse du feu derrière lui ;
déplacez le combat dans la cour pour ne pas vous enfermer avec des traînées. Abattez tôt les Tireurs.

**En groupe.** Il a plus de vie pour chaque combattant en plus et amène un [Limier calciné](../mobs/ashen-reach/cinder-hound.md)
pour chaque combattant au-delà du premier. En phase 0, séparez-vous : chaque joueur prend un brasero. Ensuite, un joueur
le retient pendant que les autres frappent ses flancs ; dispersez-vous pour le mortier et la tempête.
