---
title: Werte und Stufenaufstieg
kind: mechanic
order: 1
source: d42780e9bb0f
---

Du hast sieben Werte. Jeder beginnt bei 10, und deine Stufe ist 1 plus jeder Punkt, den du gekauft hast: Ein frischer
Charakter hat Stufe 1 und spielt sich wie Vanilla.

## Die Werte

| Wert | Kürzel | Jeder Punkt über 10 gibt |
|---|---|---|
| Vitalität | VIT | +1 maximale Gesundheit (ein halbes Herz) |
| Ausdauer | AUS | +2 maximale Ausdauer, +2 % Ausdauerregeneration, ein wenig Rückstoßresistenz |
| Stärke | STÄ | +2 % Schaden mit Stärke-Waffen: Äxte, der Streitkolben des Aschenträgers, ein Teil der Aschensense und der Jägerarmbrust, die Hälfte bei Dreizacken |
| Geschick | GES | +2 % Schaden mit Geschick-Waffen: Schwerter, Bögen, Armbrüste, das Heiligtums-Langschwert, die Geißel des Kerkermeisters, der größte Teil des Königlichen Aschenrapiers, ein Teil der Aschensense und der Jägerarmbrust, die Hälfte bei Dreizacken; +0,5 % Angriffstempo |
| Glut | GLT | +2 % Heilung des Heilflakons, +2 % Glutschaden bei den Reichswaffen, die ihn austeilen |
| Zähigkeit | ZÄH | +0,1 Rüstungshärte, −1 % Feuerschaden (bis zu −50 %) |
| Fortuna | FOR | +0,1 Glück, +0,5 % Chance, dass ein von dir getöteter Gegner seine Beute ein zweites Mal fallen lässt (bis zu 25 %) |

<!-- per-point amounts: config stats.*; Kindling ember damage: gear.weapons.kindlingPerPoint -->

## Weiche Obergrenzen

Punkte werden schwächer, je weiter ein Wert wächst:

- von 11 bis 20 zählt jeder Punkt voll;
- von 21 bis 40 zur Hälfte;
- von 41 bis 99 zu einem Fünftel.

<!-- StatCurve -->

Ein Wert endet bei 99. Standardmäßig gibt es keine Stufenobergrenze.

## Was eine Stufe kostet

Stufen werden mit Vanilla-Erfahrungs**punkten** gekauft (nicht mit Stufen der Erfahrungsleiste). Der Aufstieg von Stufe L
auf L + 1 kostet 20 + 6·L + 0,15·L² Punkte <!-- leveling.costBase / costLinear / costQuadratic -->: 26 bei Stufe 1,
200 bei Stufe 20, 695 bei Stufe 50.

## Wo du aufsteigst

Nur die [Aschenmaid](../../encyclopedia/npcs/ashen-maiden.md) lässt dich aufsteigen. Sie steht im Hub, dem
[Glutheiligtum](../walkthrough/ember-sanctum.md) (und in den alten Heiligtumskernen von Welten ab 0.4.x). Klicke sie mit
Rechtsklick an und dann auf **+** neben einem Wert. Der Bildschirm zeigt, was der nächste Punkt gibt, bevor du ihn
kaufst, sowie deine Erfahrung und die Kosten. Gehst du mehr als ein paar Blöcke weg, schließt er sich.

Es gibt keine Umverteilung: Ein gekaufter Punkt bleibt.

## Punkte, die nicht zählen

Artefakte, die du im [Reliquiar](reliquary.md) trägst, fügen Wertpunkte hinzu. Sie zählen für jeden Effekt, aber nicht
für deine Stufe, die Stufenkosten oder die Obergrenze von 99. Der Stufenaufstiegs-Bildschirm zeigt sie neben jedem Wert.

## Einstellungen

{{config stats.vitalityHealth}}
{{config leveling.statCap}}
{{config leveling.levelCap}}

Die vollständige Liste steht unter [Konfiguration](../configuration.md).

Weiter: [Gegnerstufen und Schwierigkeit](mob-levels.md)
