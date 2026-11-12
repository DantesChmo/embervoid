---
entity: embervoid:crypt_moth
kind: mob
source: fba5b3112c18
---

{{stats}}

## Wo du sie findest

Gruftmotten leben in der [Versunkenen Nekropole](../../places/sunken-necropolis.md). Sie spawnen natürlich nur unter einem
Dach, in den Krypten und unter den Überhängen des Senklochs, in Gruppen von bis zu drei
<!-- config reachMobs.spawns.crypt_moth -->, und sind in den Beinhausgruben, den Krypten und an den Außenposten der Straße
platziert. Der [Grufthüter](../../bosses/crypt-keeper.md) hetzt sie auf seine brennenden Feuerschalen.

## Verhalten

Eine blasse Motte, vom Licht angezogen. Sie fliegt zur nächsten hellen Lichtquelle in der Nähe <!-- CryptMothEntity.LIGHT_RANGE,
MIN_LUMINANCE --> und löscht sie, wenn `mobGriefing` aktiv ist, aus: Gesetzte Fackeln brechen und fallen, brennende Kerzen
erlöschen. Ein Schwarm Motten kann dich in einer Krypta in wenigen Sekunden im Dunkeln stehen lassen.

Bist du nah, wendet sie sich gegen dich. Aus der Nähe beißt sie, schwach. Ihr eigentlicher Angriff ist der Pollenstoß:
Ihre Flügel beginnen zu glühen, und eine Dreiviertelsekunde später <!-- CryptMothEntity.BURST_WINDUP --> platzt sie und
blendet jeden Spieler im Umkreis von wenigen Blöcken 3 Sekunden lang. <!-- CryptMothEntity.BLINDNESS_TICKS -->

Siehst du glühende Flügel, weiche aus dem Stoß zurück oder töte die Motte, bevor sie losgeht. Sie ist zerbrechlich, ein
schneller Schwerthieb oder ein Pfeil genügt meist. Kämpfe nie gegen eine Hohle Schildwache oder einen Grabglöckner, während
in der Nähe eine Motte schwebt: Blind kannst du ihre Ausholphasen nicht lesen. Töte zuerst die Motten.

## Beute

{{drops}}

Zusätzlich zur Tabelle kann eine Motte {{item embervoid:raw_grave_iron}} fallen lassen, und wenn ein Spieler sie tötet,
selten die {{item embervoid:drowned_sabatons}}.
