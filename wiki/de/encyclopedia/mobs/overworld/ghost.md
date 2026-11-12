---
entity: embervoid:ghost
kind: mob
source: 3972120c6791
---

{{stats}}

## Wo du ihn findest

Geister spawnen nie von allein. Sie kommen bei **Vollmond** (siehe [Mondnächte](../../../guides/mechanics/moon-nights.md))
zu jedem Oberwelt-Spieler, der **keinen** Fluch der Nacht trägt: Drei von ihnen <!-- nightMobs.ghostCount --> erscheinen
16 bis 32 Blöcke entfernt <!-- nightMobs.spawnMinDistance, nightMobs.spawnMaxDistance --> und kommen auf dich zu. Ein
Spieler mit Fluchstufen bekommt stattdessen [Aschenzombies](ashen-zombie.md).

## Verhalten

Ein blasser fliegender Geist, viel zäher als ein Aschenzombie. Er fliegt, treibt mitten durch Wände und Blöcke und sieht
dich durch sie hindurch, du kannst dich also weder verstecken noch einmauern. Er schlägt zu, wenn er dich berührt, etwa
alle zwei Sekunden <!-- GhostEntity.ATTACK_COOLDOWN_TICKS -->.

Manche deiner Treffer gehen mit einem Nebelhauch einfach durch ihn hindurch und bewirken nichts (etwa einer von sieben
<!-- nightMobs.ghostMissChance -->). Rechne mit einem langen Kampf und bring Essen und deinen Flakon mit.

Kämpfe im Freien, wo du alle drei kommen siehst, und bleib in Bewegung, damit dich immer nur einer erreicht.

Deine Welle hält die Nacht fest: Der Morgen bricht erst an, wenn alle drei fort sind. Räumst du die Welle, erhältst du eine
{{item embervoid:flask_shard}}. Stirbst du, endet deine Geisterwelle dort, und der Tod gibt dir stattdessen eine
Fluchstufe. Alle Geister, die übrig sind, wenn die Nacht endet, verschwinden.

## Beute

{{drops}}
