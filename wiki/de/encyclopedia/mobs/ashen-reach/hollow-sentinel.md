---
entity: embervoid:hollow_sentinel
kind: mob
source: 915d0a23b4f0
---

{{stats}}

## Wo du sie findest

Die Hohle Schildwache ist der Tank der [Versunkenen Nekropole](../../places/sunken-necropolis.md). Sie ist nur platziert,
spawnt nie natürlich und kehrt nie zurück: Schildwachen bewachen die Türen der Mausoleen, die eingestürzten Gräber und die
Kryptenhallen.

## Verhalten

Ein Speerträger hinter einem Turmschild. Solange sie dir zugewandt ist, deckt das Schild ihre gesamte Vorderhälfte:
Nahkampftreffer und Pfeile von vorn bewirken überhaupt nichts, auch Äxte nicht. Feuer, Magie und Explosionen verletzen
sie weiterhin aus jeder Richtung.

Sie hat zwei Angriffe und stellt sich für beide fest hin:

- **Speerstoß.** Sie zieht den Speer zurück und stößt dann weit aus, etwa 3 Blöcke über ihren Körper hinaus.
  <!-- HollowSentinelEntity.SPEAR_REACH --> Weiche während des Zurückziehens aus dieser Reichweite oder tritt zur Seite.
- **Schildstoß.** Nur wenn du nah bist: Sie setzt das Schild eine Dreiviertelsekunde lang an
  <!-- HollowSentinelEntity.BASH_WINDUP -->, dann rammt sie es dir mit starkem Rückstoß entgegen.

Nach einem Schildstoß, ob er trifft oder nicht, hängt das Schild anderthalb Sekunden lang offen.
<!-- HollowSentinelEntity.OPEN_TICKS --> Das ist dein Fenster: Weiche dem Stoß aus, dann triff sie von vorn, während sie
taumelt.

Der andere Weg ist, hinter sie zu gelangen, wo das Schild nicht hinreicht. In der Gruppe hält ein Spieler ihre
Aufmerksamkeit, und der andere bearbeitet ihren Rücken. Auch Verbrennen funktioniert: Feuerschaden wird nie geblockt.

## Beute

{{drops}}

Als platzierter Tank lässt sie mehr Reichsbeute fallen als gewöhnliche Gegner: ein oder zwei
{{item embervoid:raw_grave_iron}} und, bei einem Spielerkill, ein Teil des
[Kerkermeistersets](../../items/armor/wardens-set.md) mit der besseren Chance oder selten die
{{item embervoid:drowned_sabatons}}.
