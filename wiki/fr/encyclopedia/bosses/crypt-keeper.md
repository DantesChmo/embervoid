---
entity: embervoid:crypt_keeper
kind: boss
source: 7cd40610551f
---

{{stats}}

## Comment l'atteindre

Le Gardien de la crypte est le gardien de la [Nécropole engloutie](../places/sunken-necropolis.md) : quatre blocs de
robe et d'os, une hallebarde dans une main et une lanterne dans l'autre. Son arène est un profond gouffre ; un escalier
en lacets longe la paroi jusqu'à un ossuaire inondé, à moitié couvert, à moitié ouvert au ciel. Suivez la route de la
Nécropole depuis le [Carrefour](../places/crossroads.md) jusqu'à son bout, ou laissez la
{{item embervoid:ember_lodestar}} vous guider.

Un [Autel de braise](../blocks/functional/ember-shrine.md) et un coffre se dressent devant le mur de brume, sur le
rebord qui fait face au Carrefour. L'autel reste froid tant que vous n'avez pas vaincu le Gardien.

Le sol de l'ossuaire comporte une estrade surélevée au milieu, un chemin circulaire, une croix de passerelles et quatre
piédestaux sur les diagonales, chacun surmonté d'un brasero éteint. Retenez où ils se trouvent : le combat tourne autour
d'eux.

Descendez dans l'ossuaire et le Gardien apparaît. Un mur de brume ferme l'arène jusqu'à la fin du combat ; si vous
mourez ou partez, le combat se réinitialise et chaque brasero et chaque goutte d'eau retrouvent leur état d'origine.

## Phases et attaques

Chaque attaque est d'abord marquée. Les marques de sa hallebarde sont d'un bleu d'âme ; la seule ligne rouge est le coup
d'en haut que vous ne pouvez pas bloquer.

### Phase 1

- **Enchaînement de hallebarde.** Deux coups rapides dans un cône devant lui, puis un coup d'en haut le long d'une ligne
  rouge. Le coup d'en haut est **imparable** : il traverse un bouclier levé et le brise. Reculez hors du cône pour les
  deux coups et écartez-vous de la ligne pour le coup d'en haut.
- **Balayage.** Un anneau autour de lui, puis la hallebarde balaie tout le tour et vous repousse. Il balaie plus
  souvent quand deux joueurs ou plus se tiennent près de lui. Sortez de l'anneau.
- **Lancer de lanterne.** Sur une cible à 16 blocs au plus, il lance le feu de sa lanterne. Une ombre marque l'endroit ;
  le coup vous enflamme et une mare de flammes y brûle pendant cinq secondes
  <!-- CryptKeeperEntity.lanternThrow, 100 ticks -->.

### Phase 2 (sous 60 % de sa vie)

Sa lanterne se brise.

- **Obscurité.** Tous les participants au combat reçoivent Obscurité pour toute la durée de la phase.
- **Eau.** L'ossuaire est inondé sur un bloc de profondeur.
- **Moitié de dégâts dans le noir.** Il ne subit plus que la moitié des dégâts <!-- CryptKeeperEntity.DARK_DAMAGE -->
  sauf s'il se tient dans la lumière : une lumière de bloc de 8 ou plus <!-- CryptKeeperEntity.LIT_LEVEL --> là où il se
  trouve, ou un joueur à quelques blocs <!-- CryptKeeperEntity.LANTERN_REACH --> tenant dans sa seconde main une
  {{item embervoid:ember_lantern}} allumée avec de l'{{item embervoid:amber_lantern_oil}}.
- **Les braseros.** Vous pouvez maintenant allumer les quatre braseros des piédestaux : briquet, boule de feu ou flèche
  enflammée. Combattez-le près d'un brasero allumé et il subit tous les dégâts.
- **Phalènes de crypte.** Tant qu'un brasero brûle, des [Phalènes de crypte](../mobs/ashen-reach/crypt-moth.md)
  arrivent toutes les 15 secondes <!-- CryptKeeperEntity.MOTH_INTERVAL -->, jusqu'à quatre à la fois
  <!-- CryptKeeperEntity.MAX_MOTHS -->. Elles volent droit vers le brasero allumé le plus proche et l'étouffent. Tuez-les
  avant qu'elles y arrivent, ou rallumez-le.
- **Estoc.** Le lancer de lanterne disparaît. À la place, à distance, il se fend le long d'une ligne et se propulse en
  avant avec l'estoc. Écartez-vous de la ligne.
- L'enchaînement et le balayage restent, et il attaque plus vite.

### Phase 3 (sous 30 % de sa vie)

L'eau monte à deux blocs ; seules les passerelles, l'estrade et les piédestaux restent au sec. La cloche sonne et des
[Ossements sans repos](../mobs/ashen-reach/restless-bones.md) surgissent de l'eau : trois, plus un par combattant
au-delà du premier <!-- CryptKeeperEntity.RISEN_BONES -->. L'obscurité, les braseros et les phalènes continuent comme
en phase 2.

## Butin

{{drops}}

Lors de votre première victoire, ces objets arrivent aussi directement dans votre inventaire :

- l'{{item embervoid:crypt_keeper_soul}}, qui se place dans le {{item embervoid:wardens_scourge}} ;
- un {{item embervoid:flask_shard}}.

Aider un ami plus tard peut de temps en temps rapporter à nouveau l'âme <!-- boss.repeatSoulChance --> ; un combat lancé
avec de la {{item embervoid:ash_of_rematch}} ne le fait jamais.

## Stratégie

**Avant le combat.** Emportez un briquet ou des boules de feu et, si vous en avez, une Lanterne de braise avec de
l'Huile de lanterne ambrée à tenir dans votre seconde main. À partir de la phase 2, la lumière est tout le combat.

**Au corps à corps.** En phase 1, provoquez l'enchaînement, reculez hors des deux coups, esquivez la ligne rouge et
frappez-le pendant sa récupération. Quand la lanterne se brise, courez au piédestal le plus proche, allumez son brasero
et attirez le Gardien vers vous ; combattez-le là. Tuez chaque phalène qui vient pour votre brasero. En phase 3, restez
sur les passerelles : l'eau vous ralentit et cache les Ossements.

**À distance.** Les flèches enflammées allument les braseros de loin. Gardez vos distances en phase 1 et surveillez
l'ombre de la lanterne. À partir de la phase 2, son estoc comble vite la distance : jouer à distance demande donc
beaucoup d'esquives latérales. Les flèches sont efficaces contre les phalènes.

**En groupe.** Il a plus de vie par combattant en plus, amène à son apparition un [Ossements sans repos](../mobs/ashen-reach/restless-bones.md)
par combattant au-delà du premier, et en relève davantage en phase 3. Répartissez les tâches : un ou deux joueurs gardent
le Gardien près d'un brasero allumé, un autre maintient les braseros allumés et abat les phalènes. Ne l'encerclez pas,
sinon il balaie.
