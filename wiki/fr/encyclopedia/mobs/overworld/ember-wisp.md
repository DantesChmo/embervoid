---
entity: embervoid:ember_wisp
kind: mob
source: 49021737ec54
---

{{stats}}

## Où le trouver

Les Feux follets de braise vivent au fond du sous-sol : ils n'apparaissent qu'en dessous de y=0, dans l'obscurité
totale et à couvert, seuls ou par deux.

## Comportement

Une petite braise flottante qui plonge sur les joueurs à moins de seize blocs. Un coup est faible, mais vous enflamme
pendant quatre secondes <!-- EmberWispEntity.BURN_SECONDS -->. Après chaque plongeon, il se retire brièvement, puis
revient. De temps en temps, il allume un feu au sol à proximité (seulement si la règle de jeu `mobGriefing` est activée),
ce qui est dangereux dans une mine pleine d'étais en bois.

Il **craint la lumière**. Là où la lumière de bloc est de 7 ou plus <!-- EmberWispEntity.LIGHT_LIMIT -->, il ne plonge
pas sur vous ; il abandonne sa cible et s'enfuit dans le noir. Les feux qu'il allume lui-même ne comptent pas, mais les
torches, les lanternes et la pierre lumineuse, si.

La parade est donc simple : éclairez votre mine. Posez des torches au fur et à mesure et les feux follets ne peuvent pas
vous toucher. Dans le noir, frappez-le au moment où il arrive en plongeon, ou tirez dessus pendant qu'il plane. L'eau
éteint le feu qu'il vous inflige.

## Butin

{{drops}}
