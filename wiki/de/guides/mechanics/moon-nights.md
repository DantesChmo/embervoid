---
title: Mondnächte
kind: mechanic
order: 11
source: c11f937e0721
---

In jedem dritten Mondzyklus bekommt die Oberwelt zwei gefährliche Nächte, vier Tage voneinander entfernt: einen
**Vollmond** und einen **Blutneumond**. Beide werden beim Beginn und Ende mit einem Titel angekündigt. An diesen Tagen
verweigert dir dein Bett ab dem Abend den Dienst: Du kannst sie nicht verschlafen.

## Wann sie kommen

Ein Mondzyklus dauert 8 Tage. Mit den Standardeinstellungen zählt der erste Vollmond einer Welt nie; die ersten
Mondnächte sind:

- **Tag 24**: der Vollmond,
- **Tag 28**: der Blutneumond,

und danach alle 24 Tage (Tage 48 und 52, 72 und 76, …). <!-- MoonSchedule, config moon.cycleInterval -->

Mondnächte betreffen nur die Oberwelt. Wenn du im Hub oder in einem Reich bist, während der Vollmond aufgeht, trifft
dich deine Welle, wenn du zurückkommst, solange die Nacht dauert.

## Der Vollmond

„Heute Nacht finden die Toten keine Ruhe.“

- Feindselige Gegner der Oberwelt spawnen 3 Stufen höher, folgen dir aus größerer Entfernung und verlieren dich nicht,
  solange du in Reichweite bleibst.
- **Fluch der Nacht:** Jedes Mal, wenn dich ein feindseliger Gegner, sein Projektil oder ein Boss irgendwo tötet,
  bekommst du einen Stapel. Der Fluch weicht, wenn der Vollmond untergeht.
- Zu Beginn der Nacht kommt eine **Welle** für jeden Spieler in der Oberwelt, 16–32 Blöcke entfernt:
  - mit Fluch: ein [Aschenzombie](../../encyclopedia/mobs/overworld/ashen-zombie.md) pro Stapel, bis zu 10. Sie liegen
    5 Stufen über der normalen Stufe des Ortes und brennen nicht im Tageslicht.
  - ohne Fluch: 3 [Geister](../../encyclopedia/mobs/overworld/ghost.md). Sie fliegen durch Wände, und manche deiner
    Treffer gehen durch sie hindurch. Besiege alle drei, und sie hinterlassen eine {{item embervoid:flask_shard}}.
- **Die Nacht endet nicht, bis deine Welle besiegt ist.** Die Uhr bleibt kurz vor der Morgendämmerung stehen und wartet.
  Wenn du stirbst, während deine Geister draußen sind, treiben sie davon, und die Nacht wartet nicht mehr auf sie. Nach
  30 echten Minuten endet die Nacht trotzdem.

So bereitest du dich vor: Stirb in den Tagen davor so wenig wie möglich (weniger Stapel, weniger Zombies), oder halte
deine Bilanz sauber und verdiene dir eine Flakonscherbe von den Geistern. Kämpfe nahe einem Unterschlupf und halte
deinen Flakon voll.

## Der Blutneumond

„Der Himmel blutet.“

- Der Himmel, der Nebel und der Mond werden rot.
- Feindselige Gegner der Oberwelt heilen ein Viertel des Schadens, den sie dir zufügen.
- Blasse, freundliche **Vampire** treten in der Nähe von Spielern aus der Dunkelheit. Sie sind es nicht lange. Siehe
  [Der Vampir](vampire.md).

## Einstellungen

{{config moon.enabled}}
{{config moon.cycleInterval}}
{{config moon.fallbackMinutes}}
{{config moon.fullMoon.levelBonus}}
{{config moon.blood.vampirism}}
{{config nightMobs.maxZombies}}
{{config nightMobs.ghostCount}}

Weiter: [Der Vampir](vampire.md)
