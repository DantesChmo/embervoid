---
title: Das Reliquiar
kind: mechanic
order: 8
source: 19d00b168d83
---

Das Reliquiar enthält die kleinen Dinge, die du neben der Rüstung trägst: **Artefakte** und **Ringe**. Sie stammen alle
aus Aschenmark.

## Es öffnen

Drücke **R** (belege es unter Steuerung als „Reliquiar öffnen“ neu) oder klicke in deinem Inventar auf die Schaltfläche
des Reliquiars neben dem Rezeptbuch. Der Bildschirm zeigt deinen Charakter mit den Zubehörplätzen daneben.

Du hast **2 Artefaktplätze** und **2 Ringplätze** <!-- config reliquary.slots.artifact / reliquary.slots.ring -->. Du
kannst nicht zwei gleiche Gegenstände tragen.

## Artefakte: Wertpunkte

Jedes Artefakt fügt Wertpunkte hinzu, solange du es trägst:

| Artefakt | Gibt | Zu finden in |
|---|---|---|
| {{item embervoid:last_harvest_grain}} | +3 Vitalität | Aschenfelder |
| {{item embervoid:ploughmans_horseshoe}} | +3 Ausdauer | Aschenfelder |
| {{item embervoid:royal_hound_fang}} | +3 Geschick | Königsforst |
| {{item embervoid:amber_tear}} | +3 Glut | Königsforst |
| {{item embervoid:gravestone_medallion}} | +3 Zähigkeit | Versunkene Nekropole |
| {{item embervoid:ferrymans_coin}} | +3 Fortuna | Versunkene Nekropole |
| {{item embervoid:wardens_chain}} | +3 Stärke | Versunkene Nekropole |
| {{item embervoid:seal_of_the_ashen_king}} | +1 auf jeden Wert | Schwelende Zitadelle |

<!-- config reliquary.artifacts.points / sealPoints -->

Artefaktpunkte zählen für alles, was ein Wert bewirkt, aber sie erhöhen weder deine Stufe noch deine Stufenkosten noch
die Wertobergrenze. Der Stufenaufstiegs-Bildschirm zeigt sie neben jedem Wert.

## Ringe: Effekte

| Ring | Effekt | Zu finden in |
|---|---|---|
| {{item embervoid:couriers_ring}} | +15 % Sprinttempo | Aschenfelder |
| {{item embervoid:ring_of_ash_feathers}} | langsameres Fallen, kein Fallschaden unter 8 Blöcken (schleiche, um normal zu fallen) | Aschenfelder |
| {{item embervoid:ring_of_the_fog_stalker}} | der Nebel des Reichs lichtet sich doppelt so weit (nicht in Boss-Arenen) | Königsforst |
| {{item embervoid:delvers_ring}} | Eile I beim Abbau von Erzen des Reichs | Königsforst |
| {{item embervoid:drowned_mans_ring}} | Unterwasseratmung | Versunkene Nekropole |
| {{item embervoid:ring_of_embers}} | Nahkampftreffer setzen das Ziel 2 Sekunden lang in Brand | Schwelende Zitadelle |

<!-- config reliquary.rings.* -->

## Wo sie fallen

Jede Zone lässt nur ihr eigenes Zubehör fallen:

- ihre **Schatztruhe**: zu 8 % ein Artefakt, zu 5 % ein Ring;
- ihr **Elitegegner**, von einem Spieler getötet: zu 2 % ein Artefakt, zu 1 % ein Ring. Die Elitegegner sind der
  Aschenaasgeier (Felder), der Grabglöckner (Nekropole) und der Schwelende Ritter (Zitadelle). Der Königsforst hat keinen
  Elitegegner: Sein Zubehör stammt nur aus der Truhe des Jagdhauses.

<!-- config reliquary.drops.* -->

## Beim Tod

Getragenes Zubehör kommt mit dem Rest deiner Gegenstände in deinen [Aschenüberrest](death-and-remnant.md) und kehrt in
seine Plätze zurück, wenn du ihn einforderst. Wenn der Überrest-Modus des Servers Gegenstände auf dem Boden lässt, fallen
sie mit deinem Inventar. Mit `keepInventory` behältst du sie.

## Einstellungen

{{config reliquary.slots}}
{{config reliquary.artifacts.points}}
{{config reliquary.drops.chestArtifact}}

Weiter: [Rüstungssets](armor-sets.md)
