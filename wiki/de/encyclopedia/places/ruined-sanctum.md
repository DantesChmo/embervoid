---
title: Verfallenes Heiligtum
kind: place
source: 978b7c3e0be5
---

## So findest du es

Das Verfallene Heiligtum ist das Verlies der Oberwelt. Es generiert sich in jedem Land-Biom, etwa alle 300 bis 400 Blöcke
<!-- structure_set ember_sanctum: spacing 22, separation 8 -->, ein kurzer Ritt in eine beliebige Richtung findet also
meist eines. Von der Oberfläche siehst du nur eine Ruine. Es gibt drei Erscheinungsformen: eine Hofruine, eine
eingestürzte Kapelle und einen versunkenen Kreuzgang. Jede hat ihr eigenes Treppenhaus nach unten.

Findest du keines, such nach einer {{item embervoid:sanctum_map}} in den Truhen von Dorfkartografen und Tempeln,
Ruinenportalen und Schiffswracks. Sie markiert das nächste Verfallene Heiligtum mit einem roten X.
`/locate structure embervoid:ember_sanctum` funktioniert ebenfalls.

## Im Inneren

Ein Treppenhaus führt durch einen versiegelten, in Fels gefassten Schacht hinab in die Torhalle. Von dort zweigen
Seitenhallen ab: Gänge, Kammern, Krypten und Lagerräume.

- **Wachen.** Die meisten Wachposten der Hallen halten einen [Hohlen](../mobs/overworld/hollow.md), und in manchen
  Kammern steht ein Skelettbogenschütze. Sie werden beim Generieren des Verlieses platziert und kehren nie zurück, wenn
  du sie getötet hast. Bring eine Axt gegen die Wache der Hohlen mit.
- **Beute.** Die Truhen der Hallen enthalten gewöhnliche Verlies-Beute, {{item embervoid:flask_shard Flakonscherben}}
  und hin und wieder eine der zwei Waffen des Heiligtums: das {{item embervoid:sanctum_longsword}} oder den
  {{item embervoid:ashbearers_mace}}.
- **Die Bosstür.** Am Ende der Hallen steht ein [Glutschrein](../blocks/functional/ember-shrine.md) vor einem
  leuchtenden Vorhang aus Glutnebel. Rast dort: Er heilt dich und füllt deinen Flakon auf. Er verschiebt deinen
  Wiederbelebungspunkt nicht, der bleibt an deinem Bett.
- **Die Arena.** Hinter dem Nebel liegt eine dämmrige Halle, ringsum unentzündete Feuerschalen, mit einer kalten
  [Gespaltenen Stele](../blocks/functional/cleft-stele.md) auf dem Podest am hinteren Ende. Tritt ein, und die
  [Gefallene Schwester](../bosses/fallen-sister.md) erscheint.

Die Schwester jedes Heiligtums wird einmal pro Welt besiegt. Alle im Kampf erhalten eine
{{item embervoid:crying_soul}}, und bei deinem ersten Sieg zusätzlich eine Flakonscherbe und eine Waffe des Heiligtums.
Danach bleibt das Verlies geräumt; sie kehrt nicht zurück.

Die Weinende Seele entfacht eine Gespaltene Stele: die in der Arena oder eine, die du selbst baust. Eine leuchtende
Stele bringt dich ins [Glutheiligtum](ember-sanctum.md). Pro Welt brennt immer nur eine Stele.

In Welten, die vor 0.5.0 begonnen wurden, kann noch ein altes Heiligtum mit intaktem Kern stehen: eine Aschenmaid und ein
Ring aus Obelisken statt einer Bossarena. Beides funktioniert weiterhin.
