---
title: Heilflakon
kind: mechanic
order: 4
source: 553912f4f63c
---

Der {{item embervoid:healing_flask}} ist deine Art, dich im Kampf zu heilen. Du bekommst einen, wenn du zum ersten Mal einer
Welt beitrittst.

## Ladungen

Die Ladungen gehören **dir**, nicht der Flasche. Der Flakon zeigt sie nur an („Ladungen: 3/3“) und verbraucht sie. Ein
zweiter Flakon bringt nichts; er teilt dieselben Ladungen.

Du beginnst mit 3 Ladungen <!-- config flask.baseCharges -->, und Flakonscherben erhöhen sie auf bis zu 8
<!-- config flask.maxCharges -->.

## Trinken

Halte Rechtsklick gedrückt, um zu trinken. Der Beginn eines Schlucks verbraucht eine Ladung; die Heilung kommt dann
gleichmäßig, während du trinkst: 8 Gesundheit (4 Herzen) <!-- config flask.healAmount --> über etwa 1,6 Sekunden
<!-- config flask.drinkTicks: 32 ticks -->. Hörst du früher auf, verlierst du den Rest dieses Schlucks. Nach jedem Schluck
gibt es eine kurze Pause vor dem nächsten.

Du kannst **nicht im Sprint trinken**. Halte an, trinke, lauf weiter.

Die Heilung wächst mit:

- **Glut**: +2 % pro vollem Punkt,
- der **Flakonglut** (unten): +15 % <!-- config gear.consumables.flaskEmberBonus -->.

## Auffüllen

Deine Ladungen kommen vollständig zurück, wenn du:

- **an einem Glutschrein rastest** (Rechtsklick),
- **die Nacht in einem Bett durchschläfst** (frühes Aufstehen zählt nicht, und in Mondnächten kannst du nicht schlafen),
- **nach dem Tod respawnst**.

## Flakonscherben

Nutze eine {{item embervoid:flask_shard}} an einem Glutschrein: Dein Flakon bekommt dauerhaft eine Ladung, und die neue
Ladung kommt gefüllt. An der Obergrenze lehnt der Schrein sie ab. Scherben stammen aus:

- Truhen in den äußeren Hallen des Verfallenen Heiligtums,
- deinem ersten Sieg in einem Verfallenen Heiligtum,
- dem ersten Sieg über jeden Zonenboss von Aschenmark,
- dem Besiegen einer Geisterwelle des Vollmonds,
- dem Vampir (zwei beim ersten Kill, danach manchmal eine).

## Die Flakonglut

Die {{item embervoid:flask_ember}} ist das Geschenk des Schwelenden Fürsten für den ersten Sieg. Benutze sie einmal, und
jeder Schluck heilt dauerhaft mehr. Ein Flakon kann nur eine Glut aufnehmen.

## Deinen Flakon verlieren

Du kannst ihn nicht durch Sterben verlieren: Der Flakon fällt nie aus und kommt nie in deinen Aschenüberrest; er bleibt
in seinem Platz. Wenn du ihn wegwirfst oder er verbrennt, stelle einen neuen aus einer Glasflasche, einem Diamanten und
einer Kohle oder Holzkohle her (formlos). Deine Ladungen gehören weiterhin dir.

## Einstellungen

{{config flask.baseCharges}}
{{config flask.maxCharges}}
{{config flask.healAmount}}
{{config flask.drinkTicks}}

Weiter: [Tod und Aschenüberrest](death-and-remnant.md)
