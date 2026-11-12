---
entities: [embervoid:lantern_hag, embervoid:fire_flask]
kind: mob
source: 5077934a1bc6
---

{{stats}}

## Wo du sie findest

Die Laternenvettel ist der Fernkampfgegner des [Königsforsts](../../places/royal-woods.md). Sie spawnt allein, und weitere
sind in den Jägerverstecken, den Laternenreihen, dem Jagdhaus und den Außenposten der Straße platziert.

## Verhalten

Sie hält Abstand, etwa 8 bis 12 Blöcke <!-- LanternHagEntity.MIN_RANGE, MAX_RANGE -->: Sie weicht zurück, wenn du näher
kommst, und geht vor, wenn du dich zurückziehst. Von dort wirft sie Feuerphiolen in hohem Bogen.

Jeder Wurf wird angekündigt. Sie hebt ihre Laterne, die in Glut aufflammt, und eine Dreiviertelsekunde später
<!-- LanternHagEntity.THROW_WINDUP --> verlässt die Phiole ihre Hand, gezielt auf die Stelle, an der du in diesem Moment
stehst. Sie platzt und richtet um den Landepunkt Schaden an und hinterlässt eine Feuerlache, die jeden darin für einige
Sekunden in Brand setzt. <!-- FireFlaskEntity.PATCH_TICKS --> Es werden keine Feuerblöcke gesetzt, der Wald brennt also
nicht ab.

Geht die Laterne hoch, bleib seitlich in Bewegung: Die Phiole landet dort, wo du warst. Kämpfe nie in ihrer Feuerlache.
Um sie zu töten, stürme auf sie zu. Auf etwas innerhalb weniger Blöcke um sie herum kann sie nicht werfen
<!-- LanternHagEntity.MIN_THROW_RANGE --> und weicht nur zurück, und sie hat wenig Gesundheit. Auch ein Bogen oder eine
Armbrust funktioniert, da sie beim Zielen stillsteht. Kümmere dich zuerst um die Vettel, wenn sie mit Knechten oder einem
Borkenhohlen kommt.

## Beute

{{drops}}

Zusätzlich zur Tabelle kann eine Vettel {{item embervoid:royal_amber}} fallen lassen, und eine von einem Spieler getötete
Vettel kann ein Teil des [Jägersets](../../items/armor/huntsmans-set.md) oder, selten, die
{{item embervoid:huntsman_greaves}} fallen lassen.
