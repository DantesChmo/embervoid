---
entity: embervoid:crypt_moth
kind: mob
source: fba5b3112c18
---

{{stats}}

## Où le trouver

Les Phalènes de crypte vivent dans la [Nécropole engloutie](../../places/sunken-necropolis.md). Elles n'apparaissent
naturellement que sous un toit, dans les cryptes et sous les surplombs du gouffre, en groupes de trois au plus
<!-- config reachMobs.spawns.crypt_moth -->, et sont placées dans les fosses d'ossuaire, les cryptes et les avant-postes
de la route. Le [Gardien de la crypte](../../bosses/crypt-keeper.md) les envoie sur ses braseros allumés.

## Comportement

Une pâle phalène attirée par la lumière. Elle vole vers la source de lumière vive la plus proche
<!-- CryptMothEntity.LIGHT_RANGE,
MIN_LUMINANCE --> et, avec `mobGriefing` activé, l'éteint : les torches posées se brisent et tombent, les bougies
allumées s'éteignent. Une nuée de phalènes peut vous laisser dans le noir dans une crypte en quelques secondes.

Quand vous êtes proche, elle se retourne contre vous. De près, elle mord, faiblement. Sa vraie attaque est l'explosion de
pollen : ses ailes se mettent à luire, et trois quarts de seconde plus tard <!-- CryptMothEntity.BURST_WINDUP --> elle
éclate, aveuglant tous les joueurs à quelques blocs pendant 3 secondes <!-- CryptMothEntity.BLINDNESS_TICKS -->.

Quand vous voyez des ailes luisantes, reculez hors de l'explosion ou tuez la phalène avant qu'elle ne se déclenche. Elle
est fragile : un coup d'épée rapide ou une flèche suffit généralement. Ne combattez jamais une Sentinelle carcasse ou un
Sonneur des tombes tant qu'une phalène plane à proximité : aveugle, vous ne pouvez pas lire leurs préparations. Tuez
d'abord les phalènes.

## Butin

{{drops}}

En plus de la table, une phalène peut lâcher du {{item embervoid:raw_grave_iron}} et, quand un joueur la tue, rarement
les {{item embervoid:drowned_sabatons}}.
