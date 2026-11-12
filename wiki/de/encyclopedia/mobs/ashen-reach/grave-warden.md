---
entity: embervoid:grave_warden
kind: mob
source: 106eb91e3ace
---

{{stats}}

## Wo du ihn findest

Der Grabglöckner ist der Elitegegner der [Versunkenen Nekropole](../../places/sunken-necropolis.md). Er ist nur platziert,
einer pro Kapelle und in den Wächterkammern der Krypten, und bleibt tot, wenn du ihn getötet hast.

## Verhalten

Der Glöckner: eine schwere Gestalt mit Knüppel und Glocke. Sein Knüppelhieb trifft hart, aber er hebt ihn vor dem Schlag
langsam; tritt während der Ausholphase außer Reichweite.

Die Glocke ist die eigentliche Bedrohung. Einige Sekunden, nachdem er dich entdeckt hat, und dann alle zwölf Sekunden
<!-- GraveWardenEntity.FIRST_BELL, BELL_INTERVAL -->, stellt er sich fest hin und hebt die Glocke eine volle Sekunde lang
<!-- GraveWardenEntity.BELL_WINDUP -->, dann läutet er sie. Das Läuten:

- stellt jeden Haufen Ruheloser Gebeine in seiner Umgebung sofort wieder auf;
- erweckt zwei neue Ruhelose Gebeine, falls keine in der Nähe sind, bis zu vier eigene zugleich <!-- GraveWardenEntity.MAX_RAISED -->;
- betäubt jeden Spieler im Umkreis von wenigen Blöcken <!-- GraveWardenEntity.STAGGER_RANGE --> mit starker Verlangsamung
  für zwei Sekunden. <!-- GraveWardenEntity.STAGGER_TICKS -->

Geht die Glocke hoch, entferne dich vom Glöckner, damit die Betäubung dich verfehlt, und nutze die Sekunde, in der er
stillsteht, für einen Fernkampftreffer. Verschwende keine Zeit damit, den Gebeinen nachzujagen, die er erweckt: Sie kommen
zurück, bis der Glöckner tot ist. Konzentriere dich auf den Glöckner und erledige Haufen erst, wenn er gefallen ist, oder
kämpfe neben einer Fackel, damit die Haufen von selbst zerbröckeln.

## Beute

{{drops}}

Der Glöckner ist der Elitegegner der Nekropole. Ein Spielerkill kann ein oder zwei {{item embervoid:raw_grave_iron}}, ein
Teil des [Kerkermeistersets](../../items/armor/wardens-set.md), die {{item embervoid:drowned_sabatons}} zur Elitechance
und selten eines der Accessoires der Nekropole fallen lassen: das {{item embervoid:gravestone_medallion}}, die
{{item embervoid:ferrymans_coin}}, die {{item embervoid:wardens_chain}} oder den {{item embervoid:drowned_mans_ring}}.
