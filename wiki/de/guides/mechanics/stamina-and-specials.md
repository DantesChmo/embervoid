---
title: Ausdauer und Spezialangriffe
kind: mechanic
order: 3
source: d02e492987a2
---

Die Ausdauer in Embervoid hat nur eine Aufgabe: den Spezialangriff deiner Waffe zu bezahlen. Normale Schläge, Sprinten,
Springen und Blocken mit einem Schild bleiben kostenlos, wie in Vanilla.

## Die Ausdauerleiste

Du beginnst mit 100 Ausdauer <!-- config stamina.max -->. Die Leiste sitzt über der Schnellleiste und blendet sich aus,
solange sie voll ist. Nachdem du Ausdauer verbraucht hast, wartet sie eine Sekunde
<!-- config stamina.regenDelayTicks: 20 ticks --> und füllt sich dann schnell wieder auf, anfangs 20 pro Sekunde
<!-- config stamina.regenPerTick: 1 per tick -->.

**Ausdauer** erhöht beides: Jeder volle Punkt gibt +2 maximale Ausdauer und +2 % Regeneration.

## Die Regeln eines Spezialangriffs

Jede Waffe mit Spezialangriff zeigt ihn in ihrem Tooltip: „Rechtsklick: Name“, dann Ausdauerkosten und Abklingzeit.

- Benutze ihn mit **Rechtsklick** mit der Waffe.
- Er kostet eine **feste Menge Ausdauer**, aber du kannst ihn einsetzen, solange du **noch irgendeine Ausdauer hast**: Die
  Leiste sinkt auf null und bleibt dort stehen. Mit leerer Leiste bekommst du „Zu erschöpft“, die Leiste blinkt, und
  nichts geschieht.
- Nach dem Einsatz hat die Waffe eine kurze **Abklingzeit**, die wie bei einer Enderperle auf dem Gegenstand angezeigt
  wird.
- Ein Spezialangriff trifft mit normalem Schaden: Die Rüstung des Ziels, seine Stufe und deine Skalierung mit Stärke
  oder Geschick wirken alle.

Die Kosten sind also deine Grenze: Zwei oder drei Spezialangriffe hintereinander leeren die Leiste, und dann kämpfst du
mit einfachen Schlägen, während sie sich füllt.

## Die Spezialangriffe

| Waffe | Spezialangriff | Was er tut | Ausdauer | Abklingzeit |
|---|---|---|---|---|
| {{item embervoid:sanctum_longsword}} | Weiter Spalthieb | trifft alles in einem 90°-Bogen vor dir | 35 | 0,4 s |
| {{item embervoid:ashbearers_mace}} | Erdschlag | ein Ring um dich: Schaden, Rückstoß und Verlangsamung II | 45 | 0,5 s |
| {{item embervoid:ashen_scythe}} | Ernte | eine volle Drehung, die alles um dich herum trifft; jeder erfasste Feind gibt Ausdauer zurück | 40 | 0,5 s |
| {{item embervoid:wardens_scourge}} | Peitschenhieb | zieht den ersten Feind in Reichweite zu dir | 30 | 0,4 s |
| {{item embervoid:royal_ash_rapier}} | Ausfall | ein Sprint nach vorn, der in einem Stich endet, der doppelt blutet und brennt | 30 | 0,3 s |

<!-- config sanctumGear.cleave*, sanctumGear.slam*, gear.weapons.harvest*, lash*, lunge* -->

Die {{item embervoid:huntsmans_crossbow}} hat keinen Spezialangriff; ihre Stärke sind ihre Bolzen. Die übrigen
Passiveffekte der Waffen stehen auf ihren Enzyklopädie-Seiten.

## Tipps

- Beginne mit dem Spezialangriff und schlage dann weiter zu, während sich die Leiste füllt.
- Die Aschensense zahlt sich in einer Menge selbst: Jeder Feind, den ein Schwung oder die Ernte erfasst, gibt 3 Ausdauer
  zurück <!-- config gear.weapons.sweepStamina -->.
- Der Erdschlag ist dein Ausweg, wenn du umzingelt bist; der Peitschenhieb zieht einen Bogenschützen oder Zauberer von
  seinem Posten.

## Einstellungen

{{config stamina.max}}
{{config stamina.regenPerTick}}
{{config stamina.regenDelayTicks}}
{{config sanctumGear.cleaveStamina}}
{{config sanctumGear.slamStamina}}
{{config gear.weapons.harvestStamina}}
{{config gear.weapons.lashStamina}}
{{config gear.weapons.lungeStamina}}

Die Ausdauerleiste kann auf deinem Client mit `staminaBar.hideWhenFull` in `embervoid-client.json` sichtbar gehalten werden.

Weiter: [Heilflakon](healing-flask.md)
