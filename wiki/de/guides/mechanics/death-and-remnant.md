---
title: Tod und Aschenüberrest
kind: mechanic
order: 5
entity: embervoid:ash_remnant
source: 9df6c2f65aa0
---

Wenn du stirbst, verstreust du deine Sachen nicht auf dem Boden. Sie kommen in einen **Aschenüberrest**, der dort auf dich
wartet, wo du gefallen bist.

## Was hineinkommt

- alle deine **Erfahrungspunkte**, genau: Deine Stufe und deine Leiste kommen so zurück, wie sie waren;
- alle deine **Gegenstände**, einschließlich Rüstung und der Ringe und Artefakte, die du in deinem Reliquiar trägst;
- aber nie dein Heilflakon: Er bleibt in seinem Platz.

Der Überrest erscheint auf dem letzten festen Boden, auf dem du standest, nie in Lava oder in der Leere. Stürzt du von
einer Klippe oder in die Leere, wartet er an der Kante, von der du gefallen bist. Der Chat sagt dir, wo er liegt: „Dein
Aschenüberrest liegt bei …“.

## Ihn zurückholen

Geh zu ihm und klicke ihn mit **Rechtsklick** an. Alles kommt zurück: Gegenstände in ihre alten Plätze, wo diese frei
sind, der Rest in dein Inventar oder zu deinen Füßen. Nur du kannst ihn nehmen; ein anderer Spieler, der es versucht,
erfährt, wessen Asche es ist.

Ein Überrest kann nicht verletzt, getötet, gesprengt oder an die Leere verloren werden, und er verschwindet nicht. Er
wartet, so lange es dauert.

## Erneut sterben

Du hast immer nur einen Überrest. Wenn du stirbst, bevor du ihn erreichst, gilt standardmäßig:

- die **Erfahrung** im alten Überrest verbrennt;
- seine **Gegenstände bleiben** darin, bis du sie aufnimmst;
- dein neuer Tod hinterlässt einen neuen Überrest mit dem, was du diesmal getragen hast.

Lauf also für deine Erfahrung zurück, bevor du Risiken eingehst. Gegenstände sind sicher; Erfahrung nicht.

## Wo du erwachst

- **Oberwelt, Nether, Ende, der Hub:** wie in Vanilla, an deinem Bett oder am Weltspawn.
- **In einem Reich:** am letzten Glutschrein, an dem du in diesem Reich gerastet hast, oder an seinem Ankunftsplatz. Die
  Schwelende Zitadelle zählt für sich. Siehe [Glutschreine und Respawn](ember-shrines.md).

Dein Flakon ist beim Respawn wieder voll.

## Andere Modi

Der Server kann all das ändern:

| `remnant.mode` | Was beim Tod geschieht |
|---|---|
| `xp_and_items` (Standard) | Erfahrung und Gegenstände kommen in den Überrest |
| `xp` | Erfahrung kommt in den Überrest, Gegenstände fallen wie in Vanilla |
| `off` | Vanilla-Tod |

| `remnant.secondDeath` | Was ein neuer Tod mit dem alten Überrest macht |
|---|---|
| `burn_xp` (Standard) | seine Erfahrung verbrennt, seine Gegenstände bleiben |
| `burn_all` | er verbrennt samt allem darin |

Mit eingeschalteter Spielregel `keepInventory` behältst du deine Gegenstände und Erfahrung wie in Vanilla, und es bleibt
kein Überrest zurück.

## Einstellungen

{{config remnant.mode}}
{{config remnant.secondDeath}}

Weiter: [Glutschreine und Respawn](ember-shrines.md)
