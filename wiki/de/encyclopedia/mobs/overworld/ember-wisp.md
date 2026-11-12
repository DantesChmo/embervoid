---
entity: embervoid:ember_wisp
kind: mob
source: 49021737ec54
---

{{stats}}

## Wo du es findest

Glutirrlichter leben tief unter der Erde: Sie spawnen nur unterhalb von y=0, in völliger Dunkelheit und unter einer
Decke, allein oder zu zweit.

## Verhalten

Eine kleine schwebende Glut, die auf Spieler im Umkreis von sechzehn Blöcken herabstößt. Ein Treffer ist schwach, setzt
dich aber vier Sekunden lang in Brand <!-- EmberWispEntity.BURN_SECONDS -->. Nach jedem Sturzflug zieht es sich kurz
zurück und kommt dann erneut. Hin und wieder entzündet es in der Nähe ein Feuer am Boden (nur wenn die Spielregel
`mobGriefing` aktiv ist), was in einer Mine voller Holzstützen gefährlich ist.

Es **fürchtet Licht**. Wo das Blocklicht 7 oder mehr beträgt <!-- EmberWispEntity.LIGHT_LIMIT -->, stößt es nicht auf
dich herab; es lässt sein Ziel fallen und fliegt ins Dunkel davon. Die Feuer, die es selbst entzündet, zählen nicht, aber
Fackeln, Laternen und Glowstone schon.

Das Gegenmittel ist also einfach: Beleuchte deine Mine. Setze Fackeln beim Vorankommen, und Irrlichter können dir nichts
anhaben. Im Dunkeln triffst du es, wenn es zum Sturzflug ansetzt, oder schießt es, während es schwebt. Wasser löscht das
Brennen, das es verursacht.

## Beute

{{drops}}
