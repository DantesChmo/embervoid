---
entity: embervoid:furnace_golem
kind: mob
source: 034a7cc2049a
---

{{stats}}

## Wo du ihn findest

Der Ofengolem ist der Tank der [Schwelenden Zitadelle](../../places/smoldering-citadel.md). Er ist nur platziert, in den
Hallen und Straßen des inneren Rings, und bleibt tot, wenn du ihn getötet hast.

## Verhalten

Ein gehender Ofen: langsam, schwer gepanzert, schwer zurückzustoßen und immun gegen Feuer.

Sein eigener Angriff ist ein beidarmiger Schlag. Er stellt sich fest hin und hebt einen Moment lang beide Fäuste, bevor er
sie vor sich niederfahren lässt; tritt während des Hebens außer Reichweite.

Die Falle ist die Hitze. Jeder Treffer, den er einsteckt, heizt ihn an. Beim vierten Treffer
<!-- FurnaceGolemEntity.HITS_TO_BLAST --> hält er inne, was er gerade tat, stellt sich fest hin und glüht über eine Sekunde
lang rot <!-- FurnaceGolemEntity.HEAT_TICKS -->, dann stößt er eine Feuerwelle aus: Alles im Umkreis von wenigen Blöcken
<!-- FurnaceGolemEntity.WAVE_RADIUS --> erleidet Feuerschaden, wird in Brand gesetzt und weggestoßen. Dann kühlt er ab, und
das Zählen beginnt von vorn.

Zähle also deine Treffer. Lande drei, dann beim vierten, oder sobald du das Glühen siehst, weiche aus der Reichweite der
Welle zurück und warte den Ausbruch ab, bevor du wieder hineingehst. Feuerresistenz hilft hier sehr. Schwere, langsame
Treffer sind besser als viele leichte, da jeder Treffer gleich für den Ausbruch zählt. Feuerwaffen bewirken bei ihm
nichts.

## Beute

{{drops}}

Die Zitadelle hat weder Erz noch ein eigenes Rüstungsset. Ein von einem Spieler getöteter Ofengolem kann mit der
Elitechance den {{item embervoid:furnace_heart_cuirass}} fallen lassen.
