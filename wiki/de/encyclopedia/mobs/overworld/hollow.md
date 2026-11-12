---
entity: embervoid:hollow
kind: mob
source: c0c9fb0068dc
---

{{stats}}

## Wo du ihn findest

Hohle gehen nachts selten an der Oberfläche der Oberwelt umher, und nur unter freiem Himmel. Sie kommen allein. Sie
brennen nicht in der Sonne, einer, den du am Leben lässt, ist also am Morgen noch da. Hohle stehen auch Wache in den
Hallen jedes [Verfallenen Heiligtums](../../places/ruined-sanctum.md); diese werden beim Generieren des Verlieses
platziert.

## Verhalten

Ein bewaffneter untoter Soldat mit einem Eisen- oder Steinschwert: langsam, gepanzert und hart im Treffer. Sein gesamter
Schaden kommt vom Schwert. Er greift alle zwei Sekunden an <!-- HollowEntity.ATTACK_INTERVAL_TICKS -->, und jeder Hieb hat
eine kurze Ausholphase: Der Arm geht zurück, und der Schlag landet einen Moment später nur, wenn du noch in Reichweite
bist. Tritt zurück, wenn du den Arm zurückgehen siehst, dann tritt vor und triff ihn.

Zwischen den Hieben kann er seine **Deckung** heben. Solange sie oben ist, geht er mit halber Geschwindigkeit, Nahkampftreffer
von vorn richten nur ein Fünftel ihres Schadens an <!-- HollowEntity.GUARDED_MELEE_FACTOR -->, und Pfeile von vorn
prallen ab. Treffer von der Seite oder von hinten werden nie geblockt.

Eine **Axt** oder der {{item embervoid:ashbearers_mace}} bricht die Deckung: Der Hohle taumelt drei Sekunden lang
<!-- HollowEntity.STAGGER_TICKS -->, verlangsamt und unfähig zuzuschlagen. Das ist der beste Moment, ihn zu erledigen.

Also: Locke den Hieb heraus, weiche aus, triff ihn, bevor die Deckung hochgeht, und trage eine Axt in der Schnellleiste.
Seine Stufe ist für einen Oberwelt-Gegner hoch, er ist also langsam zu töten, aber die Erfahrung wert.

## Beute

{{drops}}
