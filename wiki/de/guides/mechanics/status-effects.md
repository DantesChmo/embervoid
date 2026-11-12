---
title: Statuseffekte
kind: mechanic
order: 10
effect: embervoid:bleeding
source: f6f67b69063a
---

Embervoid fügt bisher einen Statuseffekt hinzu: **Blutung**.

## Blutung

Blutung verursacht jede Sekunde Schaden und ignoriert Rüstung. Sie hat drei Stufen:

| Stufe | Schaden pro Sekunde |
|---|---|
| I | 0,5 (ein Viertelherz) |
| II | 1 |
| III | 1,5 |

<!-- config gear.bleeding.damagePerLevel, gear.bleeding.maxLevel -->

Jeder Treffer, der Blutung verursacht, erhöht sie um eine Stufe, bis zu III, und startet ihre 6 Sekunden
<!-- config gear.bleeding.durationTicks: 120 ticks --> erneut. Schlage weiter zu, und sie läuft mit voller Stärke weiter;
hör auf, und sie läuft wenige Sekunden später aus. Ein Gegner, der verblutet, zählt als dein Kill.

### Wer sie verursacht

Blutung ist deine Waffe, nicht die deiner Feinde:

- {{item embervoid:wardens_scourge}}: Jeder Treffer fügt eine Stufe hinzu.
- {{item embervoid:royal_ash_rapier}}: Jeder Treffer fügt eine Stufe hinzu, und ihr Ausfall fügt zwei auf einmal hinzu.

Manche Kreaturen haben kein Blut zu verlieren und bluten nie: Ruhelose Gebeine, der Borkenhohle, der Ofengolem, der
Aschenkoloss und der Trauernde Baum. Nutze gegen sie etwas anderes.

Im PvP lassen diese Waffen auch andere Spieler bluten.

### Sie stoppen

- {{item embervoid:amber_balm}} heilt Blutung und Vergiftung und heilt dich dann langsam.
- Ein Eimer Milch löscht sie wie jeden anderen Effekt.
- Oder warte sie ab: Ohne neue Treffer endet sie nach 6 Sekunden.

## Einstellungen

{{config gear.bleeding.durationTicks}}
{{config gear.bleeding.maxLevel}}
{{config gear.bleeding.damagePerLevel}}

Weiter: [Mondnächte](moon-nights.md)
