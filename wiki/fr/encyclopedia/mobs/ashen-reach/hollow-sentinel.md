---
entity: embervoid:hollow_sentinel
kind: mob
source: 915d0a23b4f0
---

{{stats}}

## Où le trouver

La Sentinelle carcasse est le tank de la [Nécropole engloutie](../../places/sunken-necropolis.md). Elle est uniquement
placée, n'apparaît jamais naturellement et ne revient jamais : les Sentinelles gardent les portes des mausolées, les
tombeaux effondrés et les salles des cryptes.

## Comportement

Un lancier derrière un bouclier-tour. Tant qu'elle vous fait face, le bouclier couvre toute sa moitié avant : les coups
de mêlée et les flèches reçus de face ne font absolument rien, haches comprises. Le feu, la magie et les explosions la
blessent quand même de tous les côtés.

Elle a deux attaques, et elle plante ses pieds pour les deux :

- **Coup de lance.** Elle ramène la lance en arrière, puis frappe loin devant, à environ 3 blocs au-delà de son corps
  <!-- HollowSentinelEntity.SPEAR_REACH -->. Reculez hors de cette portée pendant le mouvement en arrière, ou
  écartez-vous.
- **Coup de bouclier.** Seulement quand vous êtes proche : elle plante le bouclier pendant trois quarts de seconde
  <!-- HollowSentinelEntity.BASH_WINDUP -->, puis vous le plaque dessus avec un fort recul.

Après un coup de bouclier, qu'il touche ou non, le bouclier reste ouvert pendant une seconde et demie
<!-- HollowSentinelEntity.OPEN_TICKS -->. C'est votre fenêtre : esquivez le coup de bouclier, puis frappez-la de face
pendant qu'elle chancelle.

L'autre solution est de passer derrière elle, là où le bouclier ne porte pas. En groupe, un joueur retient son attention
et l'autre travaille dans son dos. La brûler marche aussi : les dégâts de feu ne sont jamais bloqués.

## Butin

{{drops}}

Étant un tank placé, elle lâche plus de butin de domaine que les monstres communs : un ou deux
{{item embervoid:raw_grave_iron fers funéraires bruts}} et, sur une victoire de joueur, une pièce de l'[ensemble du geôlier](../../items/armor/wardens-set.md)
à un meilleur taux, ou rarement les {{item embervoid:drowned_sabatons}}.
