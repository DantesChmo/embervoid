---
title: Befehle
kind: guide
order: 9
source: 35a2e3962f63
---

Die Befehle von Embervoid sind für Serveroperatoren und zum Testen gedacht. Alle beginnen mit `/embervoid` und
erfordern Operatorrechte (Stufe 2); im Einzelspieler schaltest du dafür Cheats ein. Sie sind dazu da, einen
festgesteckten Spieler zu retten oder etwas auszuprobieren, nicht für das normale Spiel, und ihre Ausgabe ist nur auf
Englisch.

`<stat>` ist `vitality`, `endurance`, `strength`, `dexterity`, `kindling`, `resilience`, `fortune` oder der Kurzname
(`vit`, `end`, `str`, `dex`, `kin`, `res`, `for`).

## Spieler

| Befehl | Was er tut |
|---|---|
| `/embervoid stats` | deine Werte, Stufe, die Kosten der nächsten Stufe und was die Werte dir geben |
| `/embervoid levelup <stat>` | einen Punkt kaufen wie bei der Aschenmaid, bezahlt mit Erfahrung |
| `/embervoid setstat <stat> <value>` | einen Wert kostenlos setzen (1–999) |
| `/embervoid stamina` | deine Ausdauer |
| `/embervoid stamina spend <amount>` | Ausdauer verbrauchen, wie es ein Spezialangriff täte |
| `/embervoid stamina set <value>` | deine Ausdauer setzen |
| `/embervoid data` | deine Embervoid-Spielerdaten, wie sie auf dem Server gespeichert sind |
| `/embervoid data flaskChargeUpgrades <n>` | festlegen, wie viele Flakonscherben dein Flakon aufgenommen hat |
| `/embervoid remnant` | deine Gegenstände und Erfahrung in einen Aschenüberrest zu deinen Füßen legen, wie bei einem Tod |
| `/embervoid reliquary <player>` | das Reliquiar eines Spielers anzeigen |
| `/embervoid reliquary <player> open` | das Reliquiar dieses Spielers für ihn öffnen |
| `/embervoid reliquary <player> clear` | das Reliquiar dieses Spielers leeren |

## Fortschritt

| Befehl | Was er tut |
|---|---|
| `/embervoid progress <player>` | die freigeschalteten und abgeschlossenen Reiche und besiegten Bosse eines Spielers anzeigen |
| `/embervoid progress <player> reset` | all das vergessen |
| `/embervoid progress <player> unlock <realm>` | ein Reich freischalten, z. B. `embervoid:realm_1` |
| `/embervoid progress <player> complete <realm>` | ein Reich als abgeschlossen markieren |
| `/embervoid progress <player> boss <id>` | einen Boss als besiegt markieren, z. B. `embervoid:fallen_sister` oder `embervoid:ashen_reach/zone_a` |

Die IDs der Reiche reichen von `embervoid:realm_1` bis `embervoid:realm_4`. Die Boss-IDs: `embervoid:fallen_sister`,
`embervoid:ashen_reach/zone_a` (Aschenkoloss), `zone_b` (Trauernder Baum), `zone_c` (Grufthüter) und `final`
(Schwelender Fürst).

## Reisen

| Befehl | Was er tut |
|---|---|
| `/embervoid hub` | zum Hub gehen |
| `/embervoid hub leave` | den Ersatzausgang des Hubs nehmen und die entfachte Stele ignorieren: dorthin, wo du die Oberwelt zuletzt verlassen hast, sonst zu deinem Bett oder zum Weltspawn |
| `/embervoid hub check` | prüfen, ob der Hub heil ist |
| `/embervoid stele` | wo die entfachte Gespaltene Stele der Welt steht |
| `/embervoid stele kindle <pos>` | die kalte Stele an einer Position entfachen, wie es eine Weinende Seele täte |
| `/embervoid realm enter <realm> [toShrine]` | ein Reich betreten, wie es sein Leerenobelisk täte; mit `true` zu deinem zuletzt dort gerasteten Schrein |
| `/embervoid realm leave` | das Reich zum Hub verlassen, wie es der Rückkehrobelisk tut |
| `/embervoid realm where` | das Reich, die Zone, das Stufenband und der Ort, an dem du stehst |
| `/embervoid realm arenas` | jede Boss-Arena des Reichs und ob du sie besiegt hast |
| `/embervoid realm roads [posts]` | die Straßen, Meilensteine und Außenposten des Reichs (und Laternenpfosten) |
| `/embervoid realm edge` | die Grenzmauer und die zerbrochenen Brücken des Reichs |
| `/embervoid realm border <from> <to>` | die Grenzmauer der letzten Zone |
| `/embervoid realm find <block> <x> <z>` | der höchste solche Block in dieser Spalte, z. B. das Zitadellentor |
| `/embervoid realm data` | deine Reisedaten für Reiche |

## Bosse, Dungeons und Gegner

| Befehl | Was er tut |
|---|---|
| `/embervoid boss fights` | laufende Boss-Kämpfe |
| `/embervoid sanctum arena <x> <z>` | die Boss-Arena des Verfallenen Heiligtums, das in diesem Chunk beginnt (worauf `/locate` zeigt), und ob sie geräumt ist |
| `/embervoid sanctum survey <radius> [<seed> [<spacing> <separation>]]` | die Verfallenen Heiligtümer zählen, die die Weltgenerierung um dich herum platzieren würde, ohne etwas zu erzeugen |
| `/embervoid moblevel <targets>` | die Stufen von Gegnern anzeigen und die Stufe, die ein neuer Spawn hier bekäme |
| `/embervoid moblevel <targets> <level>` | die Stufen von Gegnern setzen |
| `/embervoid gear bleed <targets> [stacks]` | Blutungsstufen hinzufügen, wie es so viele Treffer täten |

Um ein Bauwerk zu finden, nutze das Vanilla-`/locate structure embervoid:ember_sanctum` (ein Verfallenes Heiligtum)
oder in Aschenmark `embervoid:ashen_reach_arena_a`, `_b` und `_c` (die Zonen-Arenen).

## Mondnächte und der Vampir

| Befehl | Was er tut |
|---|---|
| `/embervoid moon status` | die aktuelle oder nächste Mondnacht |
| `/embervoid moon start full` / `start blood` | jetzt einen Vollmond oder einen Blutneumond starten |
| `/embervoid moon stop` | die Mondnacht beenden |
| `/embervoid moon setcycle <n>` | die Uhr auf den Morgen des Mondzyklus n setzen (Tag 8·n) |
| `/embervoid moon curse <player> <stacks>` | den Fluch der Nacht eines Spielers setzen |
| `/embervoid wave status` | die Wellen des Vollmonds |
| `/embervoid wave spawn <players>` / `clear <players>` | die Wellen dieser Spieler schicken oder entfernen |
| `/embervoid vampire status` | die Vampire und der Kampf dieser Nacht |
| `/embervoid vampire provoke` | der nächste Vampir verwandelt sich jetzt |
| `/embervoid vampire arena open` / `close` / `check` | die Arena des Vampirs ohne Kampf |

## Leistung

| Befehl | Was er tut |
|---|---|
| `/embervoid perf ticks` | die Tick-Zeiten des Servers |
| `/embervoid perf chunks <from> <to>` | geladene Chunks in einem Bereich |
| `/embervoid perf entities <from> <to>` | Entitäten, Gegner und platzierte Gegner in einem Bereich |

## Client

`/embervoid_client data` (ohne Operatorrechte) zeigt deine Spielerdaten, so wie dein eigener Client sie sieht.

Weiter: [FAQ und Fehlerbehebung](faq.md)
