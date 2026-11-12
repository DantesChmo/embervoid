---
title: Nuits de lune
kind: mechanic
order: 11
source: c11f937e0721
---

Tous les trois cycles lunaires, l'Overworld connaît deux nuits dangereuses, espacées de quatre jours : une **pleine lune**
et une **nouvelle lune de sang**. Toutes deux sont annoncées par un titre à leur début et à leur fin. Ces jours-là, votre
lit vous refuse dès le soir : vous ne pouvez pas les dormir.

## Quand elles viennent

Un cycle lunaire dure 8 jours. Avec les réglages par défaut, la première pleine lune d'un monde ne compte jamais ; les
premières nuits de lune sont :

- **jour 24** : la pleine lune,
- **jour 28** : la nouvelle lune de sang,

puis tous les 24 jours ensuite (jours 48 et 52, 72 et 76, …). <!-- MoonSchedule, config moon.cycleInterval -->

Les nuits de lune ne touchent que l'Overworld. Si vous êtes dans le hub ou dans un domaine quand la pleine lune se lève,
votre vague vous attend à votre retour, tant que la nuit dure.

## La pleine lune

« Les morts ne reposeront pas cette nuit. »

- Les hostiles de l'Overworld apparaissent 3 niveaux plus haut, vous suivent de plus loin et ne vous perdent pas tant que
  vous restez à portée.
- **Malédiction de la nuit :** chaque fois qu'un monstre hostile, son projectile ou un boss vous tue, où que ce soit, vous
  gagnez un cumul. La malédiction se lève quand la pleine lune se couche.
- Au début de la nuit, une **vague** vient pour chaque joueur de l'Overworld, à 16–32 blocs :
  - avec une malédiction : un [Zombie cendreux](../../encyclopedia/mobs/overworld/ashen-zombie.md) par cumul, jusqu'à 10.
    Ils ont 5 niveaux de plus que le niveau normal de l'endroit et ne brûlent pas à la lumière du jour.
  - sans malédiction : 3 [Spectres](../../encyclopedia/mobs/overworld/ghost.md). Ils volent à travers les murs, et
    certains de vos coups les traversent. Battez les trois et ils laissent un {{item embervoid:flask_shard}}.
- **La nuit ne se termine pas tant que votre vague n'est pas vaincue.** L'horloge s'arrête juste avant l'aube et attend.
  Si vous mourez alors que vos Spectres sont dehors, ils s'éloignent et la nuit ne les attend plus. Au bout de 30
  minutes réelles, la nuit se termine de toute façon.

Comment vous préparer : mourez le moins possible les jours précédents (moins de cumuls, moins de zombies), ou gardez un
casier vierge et gagnez un Éclat de fiole auprès des Spectres. Combattez près d'un abri, et gardez votre fiole pleine.

## La nouvelle lune de sang

« Le ciel saigne. »

- Le ciel, la brume et la lune virent au rouge.
- Les hostiles de l'Overworld se soignent d'un quart des dégâts qu'ils vous infligent.
- Des **Vampires** pâles et amicaux sortent de l'ombre près des joueurs. Ils ne le restent pas longtemps. Voir
  [Le Vampire](vampire.md).

## Réglages

{{config moon.enabled}}
{{config moon.cycleInterval}}
{{config moon.fallbackMinutes}}
{{config moon.fullMoon.levelBonus}}
{{config moon.blood.vampirism}}
{{config nightMobs.maxZombies}}
{{config nightMobs.ghostCount}}

Suite : [Le Vampire](vampire.md)
