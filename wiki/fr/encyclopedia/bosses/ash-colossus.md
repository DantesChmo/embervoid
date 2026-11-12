---
entity: embervoid:ash_colossus
kind: boss
source: cc207d92bfb8
---

{{stats}}

## Comment l'atteindre

Le Colosse de cendre est le gardien des [Champs cendrés](../places/ashen-fields.md). Son arène est une ancienne place de
marché enfoncée dans un cratère, cernée de dix piliers de grenier brûlés <!-- ReachArenas.PILLAR_COUNT -->, à quelques
centaines de blocs du [Carrefour](../places/crossroads.md). Suivez la route des Champs depuis la place jusqu'à son
bout, ou laissez la {{item embervoid:ember_lodestar}} vous guider.

Un [Autel de braise](../blocks/functional/ember-shrine.md) et un coffre se dressent juste devant le mur de brume, du
côté qui fait face au Carrefour. L'autel reste froid tant que vous n'avez pas vaincu le boss : reposez-vous donc au
dernier autel de la route avant d'entrer.

Entrez dans le cratère et le Colosse se dresse. Un mur de brume ferme l'arène jusqu'à la fin du combat. Si vous mourez
ou partez, il disparaît, et il revient avec toute sa vie la prochaine fois que vous entrez.

## Phases et attaques

Chaque attaque est d'abord annoncée : un cône, un anneau ou une ombre qui tombe sur le sol.

L'endroit où vous le frappez compte plus que tout. De face (dans un angle d'environ 60° autour de son regard), il
subit la moitié des dégâts <!-- AshColossusEntity.FRONT_DAMAGE -->. Par-derrière, il subit encore la moitié de plus
<!-- AshColossusEntity.BACK_DAMAGE -->, et de côté, des dégâts normaux. Combattez-le par-derrière.

### Phase 1

- **Poing.** Un cône devant lui. Le poing s'abat et vous repousse. Sortez du cône sur le côté.
- **Balayage du bras.** Un cône plus large et bas. Le balayage attire tous ceux qu'il attrape vers le Colosse, à portée
  de sa prochaine attaque. Quittez le cône de la même façon.
- **Bond.** Contre une cible à 8 à 22 blocs, il s'accroupit pendant qu'un anneau de flammes marque votre position, puis
  saute et atterrit là. Sortez de l'anneau en courant ; il atterrit où il a marqué, pas où vous allez.
- **Rocher.** Il arrache de la pierre et la lance. Une ombre marque l'endroit où elle tombe. Il lance aussi sur
  quiconque se tient loin : la distance n'est donc pas un abri.
- **Tempête de cendre.** Environ toutes les 20 secondes <!-- AshColossusEntity.STORM_INTERVAL --> il s'arrête et
  rugit. Puis, pendant six secondes <!-- AshColossusEntity.STORM_TICKS -->, tous ceux qu'il voit subissent des dégâts
  magiques constants et sont aveuglés. Rompez la ligne de vue : placez un pilier de grenier ou n'importe quel bloc plein
  entre lui et son torse avant la fin du rugissement.
- **Pilier fracassé.** Si vous vous cachez derrière un pilier alors qu'il est proche, il fracasse le pilier : un cône,
  puis toute la colonne de pierre disparaît. Les piliers reviennent après le combat, pas pendant. Évitez autant que
  possible de vous cacher deux fois derrière le même pilier, et gardez-en quelques-uns debout pour les tempêtes
  suivantes.

### Phase 2 (sous la moitié de sa vie)

Il se défait de ses plaques de pierre. Le cœur de braise dans son dos est désormais à nu : les coups portés par-derrière
infligent donc le double de dégâts <!-- AshColossusEntity.CORE_DAMAGE -->. Son armure baisse, mais il marche plus vite
et enchaîne ses attaques plus rapidement.

- **Cendres ardentes.** Toutes les quelques secondes <!-- AshColossusEntity.CINDER_INTERVAL -->, des cendres ardentes
  tombent près de chaque combattant, chacune marquée par une ombre. Une cendre vous enflamme. Continuez de bouger.
- Toutes les attaques de la phase 1 restent, y compris la tempête de cendre.

## Butin

{{drops}}

Lors de votre première victoire, ces objets arrivent aussi directement dans votre inventaire :

- l'{{item embervoid:ash_colossus_soul}}, qui se place dans la {{item embervoid:ashen_scythe}} ;
- un {{item embervoid:flask_shard}}.

Si vous aidez un ami qui a encore besoin du boss après que vous l'avez vaincu, l'âme vous revient parfois
<!-- boss.repeatSoulChance -->. Un combat lancé avec de la {{item embervoid:ash_of_rematch}} ne donne que la table de
butin et l'expérience, jamais l'âme.

## Stratégie

**Au corps à corps.** Contournez-le jusqu'à son dos et restez-y : il tourne lentement et ses attaques visent là où il
regarde au début de la préparation. Chaque balayage du bras ou poing que vous esquivez vous ouvre une fenêtre de deux ou
trois coups dans son dos. Surveillez le minuteur de la tempête et tenez-vous près d'un pilier quand le rugissement
arrive.

**À distance.** De loin, il bondit et lance des rochers sur vous, et les deux mettent du temps à arriver. Tirez, puis
bougez dès qu'un anneau ou une ombre apparaît sous vous. Les flèches qui touchent son front infligent la moitié des
dégâts : passez donc sur son flanc. Quand la tempête arrive, un pilier entre lui et vous suffit.

**En groupe.** Il a plus de vie pour chaque combattant en plus et amène une [Dépouille calcinée](../mobs/ashen-reach/cinder-husk.md)
pour chaque combattant au-delà du premier. Le Colosse change de cible toutes les quelques secondes, souvent pour celui
qui l'a le plus blessé. Profitez-en : un joueur l'attire pour qu'il regarde ailleurs pendant que les autres frappent
le cœur. En phase 2, les cendres tombent sur tout le monde : ne vous regroupez pas.
