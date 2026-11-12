---
title: FAQ und Fehlerbehebung
kind: guide
order: 10
source: 1cfe4f2fab32
---

## Spielen

**Gegner schlagen viel zu hart zu, sogar auf Leicht.**
Das ist Absicht: Leicht schlägt wie Vanilla Normal, und Gegner gewinnen Stufen, je weiter du dich vom Spawn entfernst
und je tiefer du gräbst. Siehe [Gegnerstufen und Schwierigkeit](mechanics/mob-levels.md). Ein Serverbetreiber kann es
mit `mobs.difficulty.*` in der [Konfiguration](configuration.md) abschwächen.

**Ich bin gestorben. Wo sind meine Sachen?**
In deinem Aschenüberrest, auf dem letzten festen Boden, auf dem du standest. Der Chat hat dir die Koordinaten genannt.
Geh dorthin und klicke ihn mit Rechtsklick an. Siehe [Tod und Aschenüberrest](mechanics/death-and-remnant.md).

**Ich bin auf dem Weg noch einmal gestorben, und meine Erfahrung ist weg.**
Standardmäßig verbrennt ein zweiter Tod die Erfahrung in deinem alten Überrest; seine Gegenstände bleiben darin. Hol sie
dir.

**Mein Flakon trinkt nicht.**
Du sprintest, oder er hat keine Ladungen mehr. Hör auf zu rennen; rasten an einem Glutschrein, eine Nacht durchschlafen
oder Wiederbeleben füllt ihn auf. Siehe [Heilflakon](mechanics/healing-flask.md).

**Ich habe meinen Flakon verloren.**
Er fällt beim Tod nie aus, aber wenn du ihn weggeworfen hast, stelle einen neuen her: Glasflasche, Diamant, Kohle oder
Holzkohle. Deine Ladungen bleiben erhalten.

**Mein Bett sagt, ich kann nicht schlafen.**
Es ist eine Mondnacht. Siehe [Mondnächte](mechanics/moon-nights.md).

**Die Nacht endet nicht.**
Bei Vollmond wartet die Nacht, bis die Welle jedes Spielers besiegt ist. Such deine Aschenzombies oder Geister und
erledige sie. Nach 30 echten Minuten endet die Nacht trotzdem.

**Mein Spezialangriff tut nichts.**
„Zu erschöpft“ heißt, dass deine Ausdauerleiste leer ist: Warte, bis sie sich füllt. Eine graue Überlagerung auf dem
Gegenstand bedeutet, dass die Abklingzeit noch läuft.

**Wo steige ich eine Stufe auf?**
Nur bei der Aschenmaid im Hub. In den Hub gelangst du durch eine Gespaltene Stele, die du mit einer Weinenden Seele
entfachst; die Gefallene Schwester lässt sie am Grund eines Verfallenen Heiligtums fallen. Beginne mit
[Ein Verfallenes Heiligtum finden](walkthrough/ruined-sanctum.md).

**Die Weinende Seele entfacht meine Stele nicht.**
Prüfe die Form: ein Mittelstein, vier Säulen aus je 3 Gespaltenen Steinen, die zwei Blöcke entfernt an jeder Seite
stehen, und nichts in den 3 Blöcken über der Mitte. Sie muss in der Oberwelt, im Nether oder im Ende stehen, nicht in
einem Reich oder im Hub. Siehe [Die Gespaltene Stele entfachen](walkthrough/cleft-stele.md).

**Meine Stele ist erloschen.**
Jemand hat eine andere entfacht (pro Welt brennt nur eine Stele), oder einer ihrer Steine wurde abgebaut. Du brauchst
eine neue Weinende Seele, um wieder eine zu entzünden. Der Hub lässt dich trotzdem hinaus: dorthin, wo du die Oberwelt
zuletzt verlassen hast, oder zu deinem Bett.

**Ich kann keine Blöcke abbauen oder setzen.**
Der Hub, der Ankunftsplatz des Reichs und die Boss-Arenen sind geschützt. Bau woanders.

**Der Obelisk ist dunkel.**
Du hast das Reich nicht freigeschaltet. Aschenmark wird bei deinem ersten Besuch im Hub freigeschaltet; der nächste
Obelisk leuchtet nach dem Schwelenden Fürsten, aber sein Reich entsteht in dieser Version noch.

**Das Zitadellentor öffnet sich nicht.**
Es öffnet sich für dich, sobald du selbst alle drei Zonenbosse besiegt hast; klicke es mit Rechtsklick an, um zu sehen,
wie viele noch fehlen.

**Der Schrein der Arena „antwortet nicht“.**
Du hast den Boss dieser Arena noch nicht besiegt.

**Der Boss kam mit voller Gesundheit zurück.**
Alle im Kampf sind gestorben oder gegangen, also wurde er zurückgesetzt. Dein Überrest liegt in der Arena.

**Die Gefallene Schwester ist weg, und ich habe keine Weinende Seele bekommen.**
Der Boss jedes Heiligtums fällt einmal pro Welt, und nur die Spieler in diesem Kampf bekommen eine Seele. Such ein
anderes Verfallenes Heiligtum.

## Technik

**Der ganze Text wurde zu Kästchen / das Spiel stürzte ab, als ich ein Reich betrat / Waffen sind in meiner Hand unsichtbar.**
Das waren Fehler in 0.5.0 und 0.5.1, behoben in 0.5.1 und 0.5.2. Aktualisiere auf die neueste Version.

**Ich habe die Konfiguration geändert, und nichts ist passiert.**
Die Datei wird beim Start des Spiels oder Servers gelesen: Starte neu. Auf einem Server zählt nur die Datei des Servers;
deine eigene `embervoid.json` wird ignoriert, solange du verbunden bist.

**Die Bossmusik ist zu laut, oder ich höre keine.**
Sie hat unter Optionen → Musik & Geräusche einen eigenen Regler „Bossmusik“, unabhängig von der Musik.

**Das Spiel läuft in der Nähe der Maschinerie des Reichs langsam.**
Setze `setpieces.animate` in der Konfiguration auf false, um die bewegliche Mechanik anzuhalten.

**Ich habe aktualisiert, und meine Sachen in Aschenmark sind weg.**
Beim Update von 0.5.0–0.5.2 (oder von 0.4.x) wird das Reich neu erzeugt. Fortschritt und Überreste bleiben erhalten;
Gebäude und Truheninhalte im Reich nicht. Siehe [Installation](installing.md).

**Kann ich ohne GeckoLib oder playerAnimator spielen?**
Nein, beide sind Pflicht. Installiere sie auf dem Server und bei jedem Client.
