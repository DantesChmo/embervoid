---
title: Rüstungssets
kind: mechanic
order: 9
source: 09a1ad705725
---

Aschenmark hat vier Rüstungssets und vier einzigartige Teile. Ein Set gibt nur dann einen Bonus, wenn du **alle vier**
seiner Teile trägst; der Tooltip jedes Teils sagt „Volles Set: …“. Einzigartige Teile haben einen eigenen Effekt und
zählen nie für ein Set.

## Die Sets

| Set | Wo | Rüstung | Bonus des vollen Sets |
|---|---|---|---|
| [Schwelende Rüstung](../../encyclopedia/items/armor/smoldering-set.md) | hergestellt (Schmieden) | wie Netherit | Brennen tut dir nicht weh (Lava schon) |
| [Schnitterset](../../encyclopedia/items/armor/reapers-set.md) | Gegner der Aschenfelder | leicht, etwa Eisen | +10 % Bewegungsgeschwindigkeit |
| [Jägerset](../../encyclopedia/items/armor/huntsmans-set.md) | Gegner des Königsforsts | etwas mehr, etwas Härte | +15 % Projektilschaden |
| [Kerkermeisterset](../../encyclopedia/items/armor/wardens-set.md) | Gegner der Versunkenen Nekropole | schwer, wie Diamant; jedes Teil verlangsamt dich leicht | +0,3 Rückstoßresistenz, −15 % Schaden beim Blocken mit einem Schild |

<!-- config gear.sets.*, gear.smoldering.fireReductionPerPiece; armor values GearArmor -->

Die **Schwelende Rüstung** wirkt auch Teil für Teil: Jedes Teil, das du trägst, senkt Feuerschaden um 8 %. Stelle sie am
Schmiedetisch aus einer {{item embervoid:smoldering_template}}, einem Diamantrüstungsteil und einem
{{item embervoid:smoldering_ingot}} her.

Die drei Zonensets fallen von den Monstern ihrer Zone, ein zufälliges Teil nach dem anderen, und nur, wenn ein Spieler
den Kill macht. Die zäheren platzierten Gegner lassen sie öfter fallen als die natürlichen Spawns, und auch die
Schatztruhe jeder Zone kann ein Teil enthalten.

## Einzigartige Teile

| Teil | Platz | Effekt | Wo |
|---|---|---|---|
| {{item embervoid:scarecrow_mask}} | Kopf | Gegner bemerken dich aus halber Entfernung; Aschenpirscher lauern dir nicht auf | Aschenfelder |
| {{item embervoid:huntsman_greaves}} | Beine | +20 % Tempo für 4 s nach einem Kill; kein Fallschaden bis zu 7 Blöcken | Königsforst |
| {{item embervoid:drowned_sabatons}} | Füße | volles Tempo im Wasser | Versunkene Nekropole |
| {{item embervoid:furnace_heart_cuirass}} | Brust | unter 30 % Gesundheit verbrennt eine Feuernova Feinde um dich herum und schleudert sie zurück (einmal pro 60 s) | Schwelende Zitadelle |

<!-- config gear.uniques.* -->

Sie sind seltene Drops der Monster ihrer Zone, wahrscheinlicher vom Elitegegner der Zone (dem Aschenaasgeier, dem
Borkenhohlen, dem Grabglöckner). In der Zitadelle lassen nur der Ofengolem und der Schwelende Ritter den Kürass fallen.

## Teile mischen

- Ein Setbonus braucht alle vier Teile desselben Sets. Drei Teile und ein einzigartiges geben keinen Setbonus.
- Ein einzigartiges Teil auf einem Platz ist ein Tausch: sein Effekt gegen den Setbonus der anderen drei.
- Teile der Schwelenden Rüstung senken Feuerschaden einzeln, also hilft ein Schwelender Brustpanzer mit drei anderen
  Teilen weiterhin gegen Feuer.
- Verzaubere sie, wie du willst; sie nehmen die üblichen Rüstungsverzauberungen an.

## Einstellungen

{{config gear.sets.reaperSpeed}}
{{config gear.sets.huntsmanProjectile}}
{{config gear.sets.wardenKnockbackResist}}
{{config gear.smoldering.fireReductionPerPiece}}

Weiter: [Statuseffekte](status-effects.md)
