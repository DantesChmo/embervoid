---
entity: embervoid:restless_bones
kind: mob
source: e4af0464aa91
---

{{stats}}

## Wo du sie findest

Ruhelose Gebeine füllen die [Versunkene Nekropole](../../places/sunken-necropolis.md). Sie spawnen natürlich an der
Oberfläche und in den Krypten in kleinen Gruppen, und weitere sind in den Grabreihen, den Mausoleen, der Kapelle, den
Beinhausgruben, an den Außenposten der Straße und in jedem Raum der Krypten platziert. Grabglöckner erwecken neue, und der
[Grufthüter](../../bosses/crypt-keeper.md) ruft sie in seine Arena.

## Verhalten

Ein Skelett mit einem rostigen Schwert. Es greift an wie jedes Nahkampf-Kanonenfutter: ein erhobenes Schwert, eine kurze
Ausholphase, ein Hieb. Tritt bei der Ausholphase zurück und antworte danach.

Gefährlich macht es, dass es nicht tot bleibt. Der Schlag, der es töten sollte, lässt es stattdessen zu einem Haufen
Knochen zusammenfallen. Der Haufen liegt fünf Sekunden da <!-- RestlessBonesEntity.REFORM_TICKS -->, dann steht er mit
halber Gesundheit wieder auf. Du hast zwei Wege, es endgültig zu erledigen:

- **Den Haufen zerschlagen.** Der Haufen hat sehr wenig Gesundheit; ein weiterer Treffer, bevor er aufsteht, tötet ihn.
- **Ihn beleuchten.** Ein Haufen, der in Blocklicht von 8 oder mehr liegt <!-- RestlessBonesEntity.LIGHT_TO_STAY_DEAD -->,
  zerfällt zu Staub, und der Kill gehört trotzdem dir. Kämpfe gegen Gebeine neben einer Fackel oder setze eine neben den
  Haufen.

Achte auf Grabglöckner: Das Läuten der Glocke eines Glöckners stellt jeden Haufen in seiner Umgebung sofort wieder auf.

## Beute

{{drops}}

Zusätzlich zur Tabelle können Gebeine {{item embervoid:raw_grave_iron}} fallen lassen, und wenn ein Spieler sie tötet, ein
Teil des [Kerkermeistersets](../../items/armor/wardens-set.md) oder, selten, die {{item embervoid:drowned_sabatons}}.
