---
title: Boss-Kämpfe
kind: mechanic
order: 7
id: embervoid:fog_wall
source: c5317f7be306
---

Jeder Boss in Embervoid kämpft in einer Arena nach denselben Regeln. Die Angriffe jedes Bosses stehen auf seiner eigenen
Seite in der Enzyklopädie; diese Seite behandelt die Regeln drumherum.

## Einen Kampf beginnen

Betritt die Arena eines Bosses, den du noch nicht besiegt hast, und er erscheint. Jeder, der in diesem Moment in der
Arena ist, wird zum Kämpfer. Eine Bossleiste zeigt seinen Namen und seine Stufe, seine Musik setzt ein, und eine
**Nebelwand** schließt sich um die Arena.

- Kämpfer können nicht durch die Nebelwand hinausgehen.
- Niemand sonst kann hinein, bis der Kampf endet.

Wenn du den Boss schon besiegt hast, bleibt seine Arena leer, und du kannst in Frieden hindurchgehen (außer jemand, der
ihn nicht besiegt hat, betritt sie, oder du benutzt eine Asche der Revanche).

## Gehen und verlieren

Du scheidest aus einem Kampf aus, wenn du stirbst, dich ausloggst oder auf anderem Weg hinauskommst (eine Enderperle, ein
Teleport). Wenn der letzte Kämpfer weg ist, wird der Kampf zurückgesetzt: Der Boss verschwindet und kehrt beim nächsten
Versuch mit voller Gesundheit zurück, und was der Kampf in der Arena verändert hat, wird wiederhergestellt.

Da dein Aschenüberrest dort liegt, wo du gefallen bist, ist er meist in der Arena: Hol ihn dir gleich zu Beginn deines
nächsten Versuchs.

## Gewinnen

Jeder, der **lebt und in der Arena ist**, wenn der Boss stirbt, bekommt Anrechnung.

- **Erster Sieg:** Deine persönliche Belohnung geht direkt in dein Inventar, also kann sie dir niemand wegnehmen: die
  Seele des Bosses und eine Flakonscherbe (beim Schwelenden Fürsten: seine Seele und eine Flakonglut). Der Boss lässt
  außerdem seine Beute und Erfahrung fallen.
- **Wiederholter Sieg**, zum Beispiel wenn du einem Freund hilfst: die Beute und Erfahrung und mit 25 %
  <!-- config boss.repeatSoulChance --> Chance erneut die Seele.
- Der Glutschrein der Arena entfacht sich für dich.

## Bosse in der Gruppe

Jeder Kämpfer über den ersten hinaus macht den Boss zäher:

- +50 % Gesundheit pro zusätzlichem Kämpfer <!-- config boss.healthPerExtraFighter -->, festgelegt beim Start des
  Kampfes;
- ein zusätzlicher Diener pro zusätzlichem Kämpfer <!-- config boss.addsPerExtraFighter -->, einige Stufen unter dem Boss.

Alle 8–10 Sekunden <!-- config boss.targetSwitchMinSeconds / targetSwitchMaxSeconds --> wendet sich der Boss dem zu, der
ihm in der Zwischenzeit am meisten Schaden zugefügt hat. Verteilt euren Schaden, und der Tank kann sich ausruhen.

Der Fortschritt gilt pro Spieler. Ein Freund, der einen Boss besiegt hat, kann dir helfen und bekommt dafür Beute.

## Asche der Revanche

Um einen besiegten Boss eines Reichs noch einmal zu bekämpfen, benutze eine {{item embervoid:ash_of_rematch}} am
Glutschrein vor seiner Arena und betritt dann innerhalb einer Minute die Arena. Ein Rematch gibt Beute und Erfahrung,
nie die Seele oder eine Flakonscherbe. Bei der Gefallenen Schwester (der Boss jedes Verfallenen Heiligtums fällt einmal
pro Welt) oder beim Vampir funktioniert es nicht.

## Bossmusik

Die Bossmusik hat unter Optionen → Musik & Geräusche einen eigenen Regler **Bossmusik**, unabhängig vom Regler Musik.
Schalte Musik aus, und du hörst weiterhin die Boss-Themen; schalte Bossmusik aus, um sie zum Schweigen zu bringen.

## Einstellungen

{{config boss.healthPerExtraFighter}}
{{config boss.addsPerExtraFighter}}
{{config boss.addLevelOffset}}
{{config boss.repeatSoulChance}}

Weiter: [Das Reliquiar](reliquary.md)
