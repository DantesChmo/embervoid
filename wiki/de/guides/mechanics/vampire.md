---
title: Der Vampir
kind: mechanic
order: 12
source: ff83967294cc
---

Bei einem [Blutneumond](moon-nights.md) treten blasse Gestalten in der Nähe von Spielern aus der Dunkelheit: **Vampire**.
Sie wirken freundlich. Sie reden, sie bewundern die Nacht, sie bitten dich, noch ein wenig zu bleiben. Einer von ihnen
wird gleich zum Boss.

## Wann er kommt

- Bei jedem Blutneumond erscheinen 1 bis 3 freundliche Vampire in der Nähe jeder Spielergruppe in der Oberwelt (Spieler
  im Umkreis von 64 Blöcken teilen sich eine Gruppe). <!-- config vampire.groupMin / groupMax / groupRadius -->
- **Greife einen an**, und er wirft seine Verkleidung ab: Er wird zum Vampir-Boss, und die anderen verschwinden.
- Wenn niemand einen angreift, verliert der Vampir, der einem Spieler am nächsten ist, nach 5 Minuten die Geduld
  <!-- config vampire.provokeMinutes --> und verwandelt sich trotzdem.

Du kannst dem Kampf nicht durch Höflichkeit entgehen. Du kannst nur wählen, wann er beginnt.

## Die Domäne des Vampirs

Wenn sich der Vampir verwandelt, wird **jeder Spieler in der Oberwelt** in seine Domäne gezogen, eine Arena in einer
eigenen Leere. Es gibt keine Nebelwand, aber auch keinen Ort, an den du gehen könntest.

- Die Stufe des Bosses entspricht dem stärksten teilnehmenden Spieler: mindestens 15 und höchstens 20, bis jemand von
  den Teilnehmern ein Reich freigeschaltet hat. <!-- config vampire.bossMinLevel / bossCapNoRealm -->
- Achte auf das Ausholen vor seinen Klauen; seine Angriffe stehen auf der Seite des
  [Vampirs](../../encyclopedia/bosses/vampire.md).
- **Wenn du in der Domäne stirbst, bist du raus:** Du respawnst wie gewohnt und kannst nicht zurückkehren. Wenn sich die
  Domäne schließt, wird dein Überrest an die Stelle getragen, von der du geholt wurdest.

## Gewinnen und verlieren

- **Sieg:** Jeder noch lebende Kämpfer bekommt persönliche Beute direkt ins Inventar. Dein erster Kill gibt einen
  {{item embervoid:vampire_fang}} und 2 Flakonscherben; spätere Kills mit 50 % Chance eine Flakonscherbe
  <!-- config vampire.firstKillShards / repeatShardChance -->. Zehn Sekunden später werdet ihr alle dorthin zurückgeschickt,
  von wo ihr geholt wurdet, und der Morgen bricht an.
- **Niederlage:** Wenn jeder Kämpfer gestorben ist, löst sich der Vampir lachend in der Nacht auf, und die Nacht endet.

## So bereitest du dich vor

- Rechne vier Tage nach jedem Vollmond mit dem Blutneumond und sei vor der Dämmerung bereit: voller Flakon, Flakonscherben
  geopfert, Ausrüstung repariert.
- Nur Spieler, die sich im Moment der Verwandlung in der Oberwelt befinden, werden hineingezogen. Ein Freund im Hub oder
  in einem Reich ist sicher und keine Hilfe.
- Legt in einer Gruppe fest, wer zuerst zuschlägt. Der Kampf beginnt für alle in der Oberwelt auf einmal.

## Einstellungen

{{config vampire.provokeMinutes}}
{{config vampire.bossMinLevel}}
{{config vampire.bossCapNoRealm}}
{{config vampire.firstKillShards}}

Weiter: [Ausrüstungsführer](../gear-guide.md)
