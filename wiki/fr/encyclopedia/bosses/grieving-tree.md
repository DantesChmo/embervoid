---
entities: [embervoid:grieving_tree, embervoid:tree_lantern]
kind: boss
source: 6bfac141eb65
---

{{stats}}

## Comment l'atteindre

L'Arbre endeuillé est le gardien des [Bois royaux](../places/royal-woods.md) : le chêne du roi défunt, haut d'une
dizaine de blocs, aux branches chargées de lanternes. Il se dresse dans le Bosquet royal, un jardin clos de murs derrière
le pavillon de chasse, à quelques centaines de blocs du [Carrefour](../places/crossroads.md). Suivez la route des Bois
jusqu'à son bout, ou laissez la {{item embervoid:ember_lodestar}} vous guider. Le mur du jardin a sa porte du côté qui
fait face au Carrefour.

Un [Autel de braise](../blocks/functional/ember-shrine.md) et un coffre se dressent juste devant le mur de brume.
L'autel reste froid tant que vous n'avez pas vaincu l'Arbre.

Entrez dans le bosquet et le combat commence : le mur de brume se ferme et une brume gris-vert emplit l'arène. Si vous
mourez ou partez, le combat se réinitialise.

## Phases et attaques

**Son écorce renvoie chaque coup.** Épées, haches et flèches sur le tronc ne font rien ; vous entendez un coup sourd. La
seule façon de le blesser est de briser d'abord ses lanternes.

### Les lanternes

Sept lanternes <!-- GrievingTreeEntity.LANTERNS --> pendent à ses branches. Chacune se brise sous :

- n'importe quel projectile : une flèche, un carreau d'arbalète, un trident lancé ;
- un coup au corps à corps, mais seulement sur la lanterne qui pend bas après un balayage de branche (voir plus bas).

Chaque lanterne brisée fait tressaillir l'arbre. Brisez la **dernière** et l'arbre est étourdi pendant dix secondes
<!-- GrievingTreeEntity.STUN_TICKS --> : il cesse d'attaquer, la brume se lève et le tronc subit des dégâts accrus
<!-- GrievingTreeEntity.STUNNED_DAMAGE -->. C'est votre fenêtre : foncez et frappez le tronc de toutes vos forces. Quand
l'étourdissement prend fin, tout un nouveau lot de lanternes pend d'un coup.

### Phase 1 (enraciné)

Il ne bouge jamais de sa place.

- **Lignes de racines.** Le sol tremble et une ligne de racines marque le sol depuis sa base vers chaque combattant,
  puis jaillit le long de cette ligne. Un coup vous projette en l'air et vous ralentit fortement pendant deux secondes.
  Écartez-vous de votre ligne sur le côté.
- **Balayage de branche.** Un large cône devant lui, jusqu'à environ huit blocs. Une grosse branche balaie le sol et
  vous repousse. Ensuite, la lanterne la plus proche du balayage pend bas, à portée d'épée, pendant trois secondes
  <!-- GrievingTreeEntity.LOWERED_TICKS -->. Esquivez le balayage, puis frappez cette lanterne.
- **Serfs desséchés.** Toutes les 25 secondes environ <!-- GrievingTreeEntity.THRALL_INTERVAL -->, deux
  [Serfs desséchés](../mobs/ashen-reach/withered-thrall.md) tombent de ses branches. Tuez-les vite ; ils cessent de
  venir tant que l'arbre est étourdi.

### Phase 2 (sous la moitié de sa vie)

Il arrache ses racines du sol et marche, lentement, à votre poursuite. Les anciennes lanternes tombent ; six nouvelles
<!-- GrievingTreeEntity.P2_LANTERNS --> pendent plus bas à ses propres branches et se déplacent avec lui. La brume
s'épaissit. L'écorce renvoie toujours chaque coup en dehors d'un étourdissement.

- **Cage de racines.** Un anneau autour de vous marque l'endroit ; les racines se referment et immobilisent presque
  complètement quiconque est à l'intérieur pendant trois secondes. Quittez l'anneau avant qu'il ne se referme.
- Les lignes de racines, le balayage de branche et les Serfs restent.

## Butin

{{drops}}

Lors de votre première victoire, ces objets arrivent aussi directement dans votre inventaire :

- l'{{item embervoid:grieving_tree_soul}}, qui se place dans l'{{item embervoid:huntsmans_crossbow}} ;
- un {{item embervoid:flask_shard}}.

Aider un ami plus tard peut de temps en temps rapporter à nouveau l'âme <!-- boss.repeatSoulChance --> ; un combat lancé
avec de la {{item embervoid:ash_of_rematch}} ne le fait jamais.

## Stratégie

**À distance.** Ce combat est fait pour un arc ou une arbalète. Emportez beaucoup de flèches. Tenez-vous en arrière,
hors de portée du balayage, et tirez sur les lanternes l'une après l'autre en esquivant les lignes de racines. Quand la
dernière se brise, approchez-vous pour l'étourdissement, ou continuez de tirer sur le tronc si votre arc frappe plus fort
que votre épée. En phase 2, les lanternes pendent plus bas et se déplacent avec l'arbre : elles sont plus faciles à
atteindre, mais vous avez moins de place.

**Au corps à corps.** Possible, mais lent. Restez devant lui pour provoquer le balayage de branche, esquivez-le et
brisez la lanterne abaissée. Une lanterne par balayage donne un long combat ; emportez au moins quelques flèches ou un
trident pour les dernières lanternes.

**En groupe.** L'arbre a plus de vie par combattant en plus et amène un Serf desséché supplémentaire pour chaque
combattant au-delà du premier. Les lignes de racines visent chaque combattant : dispersez-vous. Laissez les archers
briser les lanternes pendant que les autres tiennent les Serfs à distance, puis que tout le monde frappe le tronc
ensemble pendant l'étourdissement. Prévenez quand la dernière lanterne est sur le point de se briser.
