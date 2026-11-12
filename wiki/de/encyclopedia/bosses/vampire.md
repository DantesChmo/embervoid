---
entities: [embervoid:vampire_boss, embervoid:vampire]
kind: boss
source: efb9de070eb1
---

{{stats}}

## So erreichst du ihn

Der Vampir kommt nur in der **Blutneumond**-Nacht, der zweiten der beiden [Mondnächte](../../guides/mechanics/moon-nights.md)
(der vollständige Leitfaden: [Der Vampir](../../guides/mechanics/vampire.md)). Wenn der Himmel sich rot färbt, erscheinen
ein bis drei Vampire <!-- vampire.groupMin, vampire.groupMax --> in der Nähe jeder Spielergruppe in der Oberwelt. Spieler
in einem alten Heiligtumskern bleiben außen vor.

In dieser Gestalt sind sie freundlich. Sie schlendern umher, beobachten dich und reden, wenn du sie ansprichst. Sie können
nicht verletzt werden: Der erste Spieler, der irgendeinen Vampir schlägt, provoziert ihn. Dieser verwandelt sich nach einer
kurzen Verwandlung in den Boss, und alle anderen Vampire verschwinden. Provoziert niemand einen, verwandelt sich der
einem Spieler nächste Vampir nach fünf Minuten <!-- vampire.provokeMinutes --> von selbst.

Sobald der Boss entsteht, wird **jeder** Spieler in der Oberwelt (außerhalb eines Heiligtumskerns) in die Arena des Vampirs
gezogen: eine kleine Arena, die in einer rot vernebelten Leere schwebt und für jeden Blutmond neu gebaut wird. Du kannst
dort keine Blöcke abbauen oder setzen. Es gibt keine Nebelwand; die Leere rund um die Arena ist die Wand.

Seine Stufe ist die höchste Stufe unter den teilnehmenden Spielern, mindestens 15 <!-- vampire.bossMinLevel -->, aber nicht
höher als das obere Ende des Stufenbereichs des am weitesten fortgeschrittenen Reichs, das einer von ihnen freigeschaltet
hat (20, wenn keiner eines hat <!-- vampire.bossCapNoRealm -->).

## Phasen und Angriffe

- **Klauen.** Er jagt dich und zieht seine Klauen mit einem kurzen roten Hinweis zurück, bevor er zuschlägt. Tritt
  während des Hinweises zurück; er reicht trotzdem etwas weiter, als es aussieht.
- **Blutsprung.** Bei einem Ziel zwischen fünf und sechzehn Blöcken Entfernung geht er eine Sekunde lang in die Hocke,
  während zwei blutrote Ringe zeigen, wo er landen wird, springt dann dorthin und trifft alle rings um die Landestelle.
  Lauf aus den Ringen.

### Unter 60 % Gesundheit

- **Blutnova.** Ein Ring bildet sich um ihn und schrumpft anderthalb Sekunden lang, dann platzt er: Alle darin werden
  verletzt und zurückgeschleudert, und er heilt sich um ein Viertel des angerichteten Schadens
  <!-- VampireBossEntity.NOVA_HEAL -->. Verlasse den Ring, und er heilt nichts.
- Er springt öfter.

### Unter 30 % Gesundheit

- **Nebelschritt.** Bist du mehr als drei Blöcke entfernt, löst er sich in Rauch auf und erscheint mit einem Hieb direkt
  hinter dir. Siehst du den Rauch, dreh dich um.
- Er bewegt sich schneller, und die Nova kommt öfter.

## Beute

{{drops}}

Er hat keine Beutetabelle. Stirbt er, erhält jeder noch lebende Kämpfer in der Arena seine Belohnung direkt ins Inventar:

- bei deinem **ersten** Sieg: zwei {{item embervoid:flask_shard Flakonscherben}} <!-- vampire.firstKillShards --> und
  einen {{item embervoid:vampire_fang}};
- bei einem **wiederholten** Sieg: eine Chance von eins zu zwei <!-- vampire.repeatShardChance --> auf eine Flakonscherbe.

Er lässt Erfahrung fallen wie jeder Boss. Zehn Sekunden nach dem Sieg schließt sich die Arena, alle kehren dorthin zurück,
von wo sie gezogen wurden, und der Morgen bricht an.

**Stirbst du in der Arena**, kehrst du wie gewohnt zurück und kannst nicht wieder hinein. Dein
[Aschenüberrest](../../guides/mechanics/death-and-remnant.md) wird beim Schließen der Arena an die Stelle verschoben, von
der du gezogen wurdest. Sterben alle Kämpfer, verlässt der Vampir den Ort, die Arena schließt sich, und die Nacht endet.

## Strategie

**Nahkampf.** Bleib nah genug, dass er kratzt statt zu springen, und weiche bei jedem Hinweis einen Schritt zurück. Ab 60 %
ist die Nova der Angriff, den du respektieren musst: Sobald der Ring erscheint, geh hinaus, dann komm für Treffer zurück,
während er sich erholt. Halte unter 30 % den Rücken nur dann zum Arenarand, wenn du dir deines Stands sicher bist; der
Nebelschritt setzt ihn hinter dich.

**Fernkampf.** Schwer durchzuhalten: Er ist schnell und springt jeden in der Ferne an. Schieße, während er dich jagt, und
weiche jedem Landering seitlich aus.

**In der Gruppe.** Alle in der Oberwelt kämpfen gemeinsam gegen ihn, und seine Stufe folgt dem Stärksten von euch. Es gibt
keine Diener. Verteilt euch, damit eine Nova oder ein Sprung nicht die ganze Gruppe erwischt, und lasst den, den er jagt,
ihn weglocken, während die anderen ihn von hinten treffen. Jeder Kämpfer, der am Ende lebt, erhält seine eigene
Belohnung; haltet also die neueren Spieler am Leben.
