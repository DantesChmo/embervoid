---
title: Installation
kind: guide
order: 2
source: 2ac1a2032991
---

## Was du brauchst

| Mod | Version |
|---|---|
| Minecraft | 1.20.1 |
| Java | 17 oder neuer |
| [Fabric Loader](https://fabricmc.net/use/) | 0.15 oder neuer |
| [Fabric API](https://modrinth.com/mod/fabric-api) | jeder Build für 1.20.1 (entwickelt mit 0.92.12+1.20.1) |
| [GeckoLib](https://modrinth.com/mod/geckolib) | 4.8.4 oder neuer, für 1.20.1 (animierte Gegner, Bosse und Mechanik) |
| [playerAnimator](https://modrinth.com/mod/playeranimator) | 1.0.2-rc1+1.20 oder neuer, für 1.20.1 (Angriffsanimationen des Spielers) |

Alle vier Mods sind Pflicht: Ohne sie startet das Spiel nicht. [Mod Menu](https://modrinth.com/mod/modmenu) ist
optional; es zeigt nur die Beschreibung der Mod an. Embervoid hat keinen Konfigurationsbildschirm im Spiel: Die
Einstellungen stehen in einer Datei (siehe [Konfiguration](configuration.md)).

## Einzelspieler

1. Installiere den Fabric Loader für 1.20.1.
2. Lege Embervoid, Fabric API, GeckoLib und playerAnimator in deinen Ordner `mods`.
3. Starte das Spiel und erstelle eine neue Welt oder öffne eine bestehende. Embervoid funktioniert in alten
   Vanilla-Welten, aber seine Dungeons erscheinen nur in Chunks, die erzeugt werden, nachdem du die Mod hinzugefügt hast.

## Dedizierter Server

Installiere dieselben vier Mods auf dem Server **und** bei jedem Client. Ein Client ohne die Mod kann nicht beitreten.

Maßgeblich ist die `config/embervoid.json` des Servers: Sie wird jedem Spieler beim Beitritt geschickt, und sein Client
folgt ihr für diese Sitzung. Rein clientseitige Einstellungen (die Ausdauerleiste, Gegnerstufen-Anzeigen,
Bossmusik-Lautstärke, Kamerawackeln) bleiben bei jedem Spieler.

## Auf eine neue Version aktualisieren

Sichere zuerst deine Welt und ersetze dann die Jar-Datei. Embervoid stellt eine Welt beim ersten Start mit der neuen
Version um.

- **Von 0.5.0–0.5.2 auf 0.5.3 und von 0.4.x auf 0.5.x:** Das Reich Aschenmark wird mit seinem neuen Aufbau erneut
  erzeugt. Dein Fortschritt bleibt erhalten (besiegte Bosse, freigeschaltete und abgeschlossene Reiche), Aschenüberreste
  im Reich werden auf dem Ankunftsplatz des Hubs abgelegt, und Spieler, die im Reich ausgeloggt haben, treten im Hub
  bei. Alles, was du in Aschenmark gebaut oder in Truhen gelassen hast, ist weg, und die Reichsschreine, an denen du
  gerastet hast, sind vergessen: Du beginnst wieder am Ankunftsplatz. Die Oberwelt, der Nether, das Ende und der Hub
  bleiben unberührt.
- **Von 0.4.x:** Bereits erzeugte Heiligtümer behalten ihren alten Kern mit der Aschenmaid und den Obelisken; sie
  funktionieren weiter. Neue Verfallene Heiligtümer erscheinen in frischen Chunks.
- **Konfiguration:** Fehlende Schlüssel werden mit ihren Standardwerten ergänzt. Schlüssel, die du schon hast, behalten
  ihre Werte, auch wenn eine neue Version den Standardwert ändert (0.5.3 hat die Waffen-Spezialangriffe neu abgestimmt).
  Lösche einen Schlüssel, um den neuen Standardwert zu bekommen.

Weiter: [Eine härtere Oberwelt](walkthrough/harder-overworld.md)
