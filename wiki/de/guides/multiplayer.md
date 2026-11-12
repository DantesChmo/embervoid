---
title: Mehrspieler und Server
kind: guide
order: 6
source: fefd3e5fe6e7
---

Embervoid spielt sich auf einem Server genauso wie im Einzelspieler. Installiere es mit seinen Pflicht-Mods auf dem
Server und bei jedem Client (siehe [Installation](installing.md)).

## Was geteilt wird und was dir gehört

**Nur dir gehören:**

- deine Werte, Stufe und Ausdauer, deine Flakonladungen und Flakonscherben, deine Flakonglut;
- dein Fortschritt: welche Bosse du besiegt hast, welche Reiche du freigeschaltet und abgeschlossen hast und damit, wie
  jeder Leerenobelisk für dich aussieht;
- ob sich das Zitadellentor für dich öffnet und ob der Schrein einer Arena für dich brennt;
- dein Respawn-Schrein in jedem Reich;
- dein Aschenüberrest: Niemand sonst kann ihn nehmen.

**Von allen geteilt:**

- der Hub: Es gibt ein Glutheiligtum für den ganzen Server;
- jedes Reich: ein Aschenmark, dasselbe Gelände, dieselben Truhen und dieselben platzierten Gegner. Eine Truhe, die
  jemand geplündert hat, und eine Wache, die jemand getötet hat, bleiben für alle so;
- die entfachte Gespaltene Stele der Welt: Es gibt eine pro Welt, und jeder Spieler, der eine neue entfacht, löscht die
  alte für alle;
- der Boss jedes Verfallenen Heiligtums: Er fällt einmal pro Welt, für alle, die in diesem Kampf sind.

## Bosse in der Gruppe

- Jeder, der beim Start des Kampfes in der Arena ist, kämpft mit; die Nebelwand hält Nachzügler draußen.
- Jeder zusätzliche Kämpfer gibt dem Boss +50 % Gesundheit und einen weiteren Diener. Der Boss wechselt alle 8–10
  Sekunden das Ziel und wendet sich dem zu, der ihm am meisten Schaden zugefügt hat.
- Jeder, der beim Kill lebt und drinnen ist, bekommt Anrechnung. Belohnungen für den ersten Sieg landen im eigenen
  Inventar jedes Spielers.
- Wenn du einem Freund hilfst, der einen Boss noch nicht besiegt hat: Du bekommst seine Beute und Erfahrung und erneut
  eine Chance auf seine Seele.
- **Die Gefallene Schwester fällt einmal pro Heiligtum.** Geht gemeinsam hinunter: Jeder Spieler in diesem Kampf
  bekommt eine Weinende Seele. Wer es verpasst hat, braucht ein anderes Heiligtum.

Einzelheiten unter [Boss-Kämpfe](mechanics/boss-fights.md).

## Mondnächte mit Freunden

- Die Wellen des Vollmonds gelten pro Spieler: Jeder Spieler in der Oberwelt bekommt seine eigene, aus seinem eigenen
  Fluch.
- Spieler im Umkreis von 64 Blöcken teilen sich eine Gruppe freundlicher Vampire.
- Wenn sich ein Vampir verwandelt, wird **jeder Spieler in der Oberwelt** in seine Domäne gezogen, egal wo er ist. Warne
  deine Freunde, bevor du einen angreifst.

## Servereinstellungen, die du kennen solltest

Die `config/embervoid.json` des Servers bestimmt die Regeln für alle; jeder beitretende Client erhält sie. Bearbeite sie,
solange der Server gestoppt ist: Die Datei wird beim Start gelesen. Schlüssel, die Serverbetreiber am häufigsten ändern:

- `remnant.mode` und `remnant.secondDeath`: wie hart der Tod ist.
- `mobs.difficulty.*`: wie hart feindselige Gegner auf jeder Schwierigkeit zuschlagen.
- `boss.healthPerExtraFighter` und `boss.addsPerExtraFighter`: wie viel zäher Bosse in einer Gruppe werden.
- `moon.enabled`: Mondnächte an oder aus.
- `vampire.groupRadius`: wie nah Spieler beieinander sein müssen, um eine Gruppe Vampire zu teilen.

Alle Schlüssel stehen unter [Konfiguration](configuration.md). Die Operatorbefehle stehen unter [Befehle](commands.md).

Weiter: [Konfiguration](configuration.md)
