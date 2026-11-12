---
entity: embervoid:bark_hollow
kind: mob
source: 3375f36d2fdc
---

{{stats}}

## Où le trouver

La Carcasse d'écorce est le tank des [Bois royaux](../../places/royal-woods.md). Elle n'apparaît jamais naturellement :
les Carcasses d'écorce sont placées dans les clairières, le long du mur du parc et au pavillon de chasse, et une fois
tuées elles ne reviennent pas.

## Comportement

Elle reste dormante, ressemblant exactement à un arbre mort : pas un mouvement, pas un bruit, des yeux sombres. Quand
vous approchez à quelques blocs <!-- BarkHollowEntity.WAKE_RANGE --> ou la frappez, elle s'éveille en craquant en une
seconde, ses yeux s'allument, et elle vient sur vous. Laissée tranquille assez longtemps, elle retourne à sa place et
redevient un arbre.

Sa seule attaque est un lourd balayage sur les 120° devant elle, jusqu'à environ 3 blocs
<!-- BarkHollowEntity.SWEEP_RANGE -->. Elle plante ses pieds et se rejette en arrière pendant une pleine seconde
<!-- BarkHollowEntity.SWEEP_WINDUP --> avant de frapper. Cette longue préparation est votre ouverture : reculez hors de
portée, ou passez derrière elle, et frappez-la pendant qu'elle balaie le vide. Elle est lente et difficile à repousser.

Prenez une hache ou du feu. Un coup de hache au corps à corps et tous les types de dégâts de feu lui font une fois et
demie plus mal <!-- BarkHollowEntity.WEAK_FACTOR -->. Traitez les arbres morts suspects des Bois avec prudence, et
réveillez-les à vos conditions, idéalement la zone déjà dégagée.

## Butin

{{drops}}

Étant un tank placé, elle lâche plus de butin de domaine que les monstres communs : un ou deux
{{item embervoid:royal_amber ambres royaux}} et, sur une victoire de joueur, une pièce de l'[ensemble du veneur](../../items/armor/huntsmans-set.md)
et les {{item embervoid:huntsman_greaves}} à un meilleur taux.
