---
entity: embervoid:bark_hollow
kind: mob
source: 3375f36d2fdc
---

{{stats}}

## Wo du ihn findest

Der Borkenhohle ist der Tank des [Königsforsts](../../places/royal-woods.md). Er spawnt nie natürlich: Borkenhohle sind auf
den Lichtungen, entlang der Parkmauer und am Jagdhaus platziert, und einmal getötet, kehren sie nicht zurück.

## Verhalten

Er steht ruhend da und sieht genau aus wie ein toter Baum: keine Bewegung, kein Laut, dunkle Augen. Kommst du auf wenige
Blöcke heran <!-- BarkHollowEntity.WAKE_RANGE --> oder schlägst du ihn, erwacht er über eine Sekunde knarrend, seine Augen
leuchten auf, und er geht auf dich los. Lässt man ihn lange genug in Ruhe, geht er zu seinem Platz zurück und wird wieder
zum Baum.

Sein einziger Angriff ist ein schwerer Hieb über die 120° vor ihm, etwa 3 Blöcke weit.
<!-- BarkHollowEntity.SWEEP_RANGE --> Er stellt sich fest hin und holt eine volle Sekunde lang aus
<!-- BarkHollowEntity.SWEEP_WINDUP -->, bevor er zuschlägt. Diese lange Ausholphase ist deine Lücke: Tritt außer Reichweite
oder geh hinter ihn, und triff ihn, während er ins Leere schlägt. Er ist langsam und schwer zurückzustoßen.

Bring eine Axt oder Feuer mit. Ein Nahkampftreffer mit einer Axt und jede Art von Feuerschaden verletzen ihn um die
Hälfte mehr. <!-- BarkHollowEntity.WEAK_FACTOR --> Geh mit verdächtigen toten Bäumen im Forst vorsichtig um und wecke sie
zu deinen Bedingungen, am besten, wenn das Gebiet schon geräumt ist.

## Beute

{{drops}}

Als platzierter Tank lässt er mehr Reichsbeute fallen als gewöhnliche Gegner: ein oder zwei
{{item embervoid:royal_amber}} und, bei einem Spielerkill, ein Teil des [Jägersets](../../items/armor/huntsmans-set.md)
und die {{item embervoid:huntsman_greaves}} mit der besseren Chance.
