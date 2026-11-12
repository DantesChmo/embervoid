---
title: Gegnerstufen und Schwierigkeit
kind: mechanic
order: 2
source: 27bec0020add
---

Jeder feindselige Gegner, ob Vanilla oder aus einer anderen Mod, bekommt einmal eine Stufe, wenn er zum ersten Mal in die
Welt kommt. Sieh einen Gegner an, und sein Name zeigt „St. N“.

## Wie die Stufe eines Gegners bestimmt wird

In der Oberwelt, im Nether und im Ende ist die Stufe die **Grundstufe** des Gegnertyps plus Boni:

| Grundstufe | Gegner |
|---|---|
| 3 | Zombie, Zombie-Dorfbewohner, Wüstenzombie, Ertrunkener, Spinne |
| 4 | Skelett, Eiswanderer, Schleim und jeder nicht aufgeführte Feind |
| 5 | Aschenghul |
| 6 | Creeper, Hexe |
| 8 | Glutirrlicht |
| 12 | Enderman |
| 15 | Hohler |

<!-- config mobs.levels.baseLevels, mobs.levels.defaultBaseLevel -->

| Bonus | Stufen |
|---|---|
| Oberwelt: alle 1000 Blöcke vom Weltspawn | +1, bis zu +5 |
| Oberwelt: unter y = 0 | +3 |
| Der Nether | +5 |
| Das Ende | +8 |
| Oberwelt in einer Vollmondnacht | +3 |

<!-- config mobs.levels.blocksPerLevel, maxDistanceBonus, belowZeroBonus, dimensionBonus, moon.fullMoon.levelBonus -->

In einem Reich kommt die Stufe eines Gegners stattdessen aus der Zone, in der er spawnt: in Aschenmark 15–18 in den
Aschenfeldern, 18–21 im Königsforst, 21–25 in der Versunkenen Nekropole und 25–30 in der Schwelenden Zitadelle. Bosse
haben feste Stufen.

Der Enderdrache, der Wither, der Wärter und der Große Wächter haben keine Stufe und bleiben wie in Vanilla.

## Was eine Stufe verändert

Für jede Stufe über 1 bekommt ein Gegner:

- +6 % Gesundheit,
- +5 % Schaden (Nahkampf, Pfeile, Explosionen und Tränke gleichermaßen),
- +6 % Erfahrung, wenn er stirbt.

<!-- config mobs.levels.healthPerLevel / damagePerLevel / experiencePerLevel -->

Ein Gegner der Stufe 11 hat also 60 % mehr Gesundheit und schlägt 50 % härter zu als bei Stufe 1. Manche Vanilla-Typen
werden zuerst herunterskaliert, damit sie den Rest nicht überwachsen: Zombies und Wüstenzombies haben weniger
Grundgesundheit, Hexen und Endermen weniger Grundschaden.

## Schwierigkeit

Embervoid ersetzt den Schwierigkeitsfaktor von Vanilla auf den Schaden, den feindselige Gegner Spielern zufügen:

| Schwierigkeit | Embervoid | Vanilla |
|---|---|---|
| Leicht | ×1,0 | etwa ×0,5 |
| Normal | ×1,25 | ×1,0 |
| Schwer | ×1,75 | ×1,5 |

<!-- config mobs.difficulty.* -->

Leicht schlägt jetzt wie Vanilla Normal. Der Faktor gilt vor dem Stufenbonus.

## Einstellungen

{{config mobs.levels.enabled}}
{{config mobs.levels.baseLevels}}
{{config mobs.difficulty.normal}}

Gegner aus anderen Mods bekommen die Standard-Grundstufe; gib ihnen in `mobs.levels.baseLevels` ihre eigene. Die
Gegnerstufen-Anzeige kann auf deinem Client mit `hud.showMobLevel` in `embervoid-client.json` ausgeblendet werden.
Jeder Schlüssel steht unter [Konfiguration](../configuration.md).

Weiter: [Ausdauer und Spezialangriffe](stamina-and-specials.md)
