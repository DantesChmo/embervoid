---
title: Konfiguration
kind: guide
order: 7
source: 11f390862e36
---

## Wo die Einstellungen liegen

| Datei | Was | Wer sie liest |
|---|---|---|
| `config/embervoid.json` | die Spielregeln: jeder Schlüssel unten | der Server (im Einzelspieler dein eigenes Spiel) |
| `config/embervoid-client.json` | deine Anzeigeeinstellungen | nur dein Client |

Beide Dateien werden beim ersten Start des Spiels mit ihren Standardwerten angelegt. Embervoid hat keinen
Einstellungsbildschirm im Spiel: Bearbeite das JSON mit einem Texteditor, **während das Spiel oder der Server gestoppt
ist**, und starte dann neu. Die Datei wird beim Start gelesen.

- Ein fehlender Schlüssel wird mit seinem Standardwert ergänzt.
- Ein ungültiger Wert (außerhalb des Bereichs, falscher Typ) wird protokolliert und auf den Standardwert zurückgesetzt.
- Eine Datei, die kein gültiges JSON ist, wird nach `embervoid.json.broken` verschoben und durch die Standardwerte ersetzt.
- Auf einem Server wird die `embervoid.json` des Servers an jeden beitretenden Spieler geschickt, und sein Client folgt ihr
  für diese Sitzung. Spieler können die Regeln nicht mit ihrer eigenen Datei ändern.

Wenn eine neue Version einen Standardwert ändert, bleibt der Wert in deiner Datei bestehen. Lösche einen Schlüssel, um
den neuen Standardwert zu übernehmen.

Ein „voller Punkt“ eines Werts folgt den weichen Obergrenzen: Werte bis 20 zählen voll, 21–40 zur Hälfte, 41–99 zu
einem Fünftel.

Der Abstand der Bauwerke (wie oft Verfallene Heiligtümer erscheinen) steht nicht in dieser Datei; ändere ihn mit einem
Datenpaket (siehe [Datenpakete und Kompatibilität](datapacks.md)).

## Die Schlüssel, die sich zu ändern lohnen

- **Wie hart der Tod ist:** `remnant.mode` und `remnant.secondDeath`. Setze `remnant.secondDeath` auf `burn_all` für ein
  echtes Souls-like oder `remnant.mode` auf `off`, um mit Vanilla-Toden zu spielen.
- **Wie hart Gegner zuschlagen:** `mobs.difficulty.easy`, `normal` und `hard`. Setze sie auf 0.5, 1.0 und 1.5 für ungefähr
  die Schwierigkeitsskalierung von Vanilla.
- **Wie schnell Gegner wachsen:** `mobs.levels.healthPerLevel` und `damagePerLevel`, oder `mobs.levels.enabled`, um
  Stufen abzuschalten.
- **Gegner anderer Mods:** Füge ihre Entitäts-IDs zu `mobs.levels.baseLevels` hinzu, zum Beispiel `"othermod:ogre": 10`.
- **Mondnächte:** `moon.enabled`, um sie abzuschalten, `moon.cycleInterval`, um sie seltener oder häufiger zu machen.
- **Dein Flakon:** `flask.baseCharges`, `flask.maxCharges` und `flask.healAmount`.
- **Tempo des Stufenaufstiegs:** die drei Schlüssel `leveling.cost*`, `leveling.levelCap`.
- **Bosse in der Gruppe:** `boss.healthPerExtraFighter` und `boss.addsPerExtraFighter`.
- **Leistung:** `setpieces.animate` auf false hält jede bewegliche Mechanik an.

## Client-Einstellungen

In `config/embervoid-client.json`:

`staminaBar.hideWhenFull` — die Ausdauerleiste ausblenden, solange die Ausdauer voll ist (Standard true).

`hud.showMobLevel` — „St. N“ neben dem Namen des Gegners anzeigen, den du ansiehst (Standard true).

Unter Optionen → Musik & Geräusche stellt **Bossmusik** die Lautstärke der Boss-Themen ein (Standard 100 %), unabhängig
von der Musik. Unter Optionen → Barrierefreiheit stellt **Kamerawackeln** ein, wie stark schwere Schläge die Kamera
erschüttern, und **Trefferblitze** schaltet die hellen Blitze schwerer Schläge ein oder aus. Diese werden mit den
Vanilla-Optionen gespeichert.

## Alle Schlüssel

Gruppiert nach Abschnitt von `embervoid.json`. Ein Schlüssel `a.b.c` ist der Schlüssel `c` im Objekt `b` im Objekt `a`.

### remnant: Tod und Aschenüberrest

`remnant.mode` — Was beim Tod in den Aschenüberrest kommt: `xp_and_items`, `xp` (Gegenstände fallen wie in Vanilla) oder `off` (Vanilla-Tod).
{{config remnant.mode}}

`remnant.secondDeath` — Was ein neuer Tod mit deinem vorherigen Überrest macht: `burn_xp` (seine Erfahrung verbrennt, seine Gegenstände bleiben) oder `burn_all` (er verbrennt samt allem).
{{config remnant.secondDeath}}

### stamina: Ausdauer

`stamina.max` — Ausdauervorrat bei Grund-Ausdauer.
{{config stamina.max}}

`stamina.regenPerTick` — Pro Tick wiederhergestellte Ausdauer, sobald sie sich regeneriert, bei Grund-Ausdauer.
{{config stamina.regenPerTick}}

`stamina.regenDelayTicks` — Ticks nach dem Verbrauch von Ausdauer, bevor sie sich zu regenerieren beginnt.
{{config stamina.regenDelayTicks}}

### flask: Heilflakon

`flask.baseCharges` — Ladungen eines frischen Heilflakons.
{{config flask.baseCharges}}

`flask.maxCharges` — Ladungsobergrenze mit Flakonscherben (nie unter `baseCharges`).
{{config flask.maxCharges}}

`flask.healAmount` — Gesundheit pro Schluck, in halben Herzen.
{{config flask.healAmount}}

`flask.drinkTicks` — Ticks, die ein Schluck dauert.
{{config flask.drinkTicks}}

### stats: Werte

`stats.vitalityHealth` — Maximale Gesundheit (halbe Herzen) pro vollem Vitalitätspunkt über 10.
{{config stats.vitalityHealth}}

`stats.enduranceStamina` — Maximale Ausdauer pro vollem Ausdauerpunkt.
{{config stats.enduranceStamina}}

`stats.enduranceStaminaRegen` — Ausdauerregeneration pro vollem Ausdauerpunkt, als Bruchteil von `stamina.regenPerTick`.
{{config stats.enduranceStaminaRegen}}

`stats.enduranceKnockbackResistance` — Rückstoßresistenz pro vollem Ausdauerpunkt.
{{config stats.enduranceKnockbackResistance}}

`stats.strengthDamage` — Schaden mit Stärke-Waffen pro vollem Stärkepunkt, als Bruchteil.
{{config stats.strengthDamage}}

`stats.dexterityDamage` — Schaden mit Geschick-Waffen pro vollem Geschickpunkt, als Bruchteil.
{{config stats.dexterityDamage}}

`stats.dexterityAttackSpeed` — Angriffstempo pro vollem Geschickpunkt, als Bruchteil.
{{config stats.dexterityAttackSpeed}}

`stats.kindlingFlaskHeal` — Heilung des Heilflakons pro vollem Glutpunkt, als Bruchteil.
{{config stats.kindlingFlaskHeal}}

`stats.resilienceToughness` — Rüstungshärte pro vollem Zähigkeitspunkt.
{{config stats.resilienceToughness}}

`stats.resilienceFireReduction` — Entfernter Feuerschaden pro vollem Zähigkeitspunkt.
{{config stats.resilienceFireReduction}}

`stats.resilienceFireReductionCap` — Der meiste Feuerschaden, den Zähigkeit entfernen kann.
{{config stats.resilienceFireReductionCap}}

`stats.fortuneLuck` — Glück pro vollem Fortunapunkt.
{{config stats.fortuneLuck}}

`stats.fortuneExtraDrop` — Chance pro vollem Fortunapunkt, dass ein von dir getöteter Gegner seine Beute ein zweites Mal fallen lässt.
{{config stats.fortuneExtraDrop}}

`stats.fortuneExtraDropCap` — Die höchste Chance auf einen Extra-Drop.
{{config stats.fortuneExtraDropCap}}

### leveling: Stufenaufstieg

`leveling.costBase` — Stufenkosten, konstanter Teil: Stufe L auf L + 1 kostet `costBase + costLinear·L + costQuadratic·L²` Erfahrungspunkte.
{{config leveling.costBase}}

`leveling.costLinear` — Stufenkosten, der mit L wachsende Teil.
{{config leveling.costLinear}}

`leveling.costQuadratic` — Stufenkosten, der mit L² wachsende Teil.
{{config leveling.costQuadratic}}

`leveling.statCap` — Höchster Wert, auf den ein Wert gesteigert werden kann.
{{config leveling.statCap}}

`leveling.levelCap` — Höchste Charakterstufe; 0 = keine Obergrenze.
{{config leveling.levelCap}}

### mobs: Gegnerstufen und Schwierigkeit

`mobs.levels.enabled` — Gegnerstufen an oder aus. Aus: keine neuen Stufen, und bestehende bewirken nichts.
{{config mobs.levels.enabled}}

`mobs.levels.defaultBaseLevel` — Grundstufe jedes feindseligen Gegners, der nicht in `baseLevels` steht, ob Vanilla oder Mod.
{{config mobs.levels.defaultBaseLevel}}

`mobs.levels.baseLevels` — Grundstufe nach Entitäts-ID, z. B. `"minecraft:zombie": 3`. Füge hier Gegner anderer Mods hinzu.
{{config mobs.levels.baseLevels}}

`mobs.levels.blocksPerLevel` — Oberwelt: +1 Stufe pro so viele Blöcke Abstand vom Weltspawn.
{{config mobs.levels.blocksPerLevel}}

`mobs.levels.maxDistanceBonus` — Oberwelt: die meisten Stufen, die der Abstand hinzufügen kann.
{{config mobs.levels.maxDistanceBonus}}

`mobs.levels.belowZeroBonus` — Oberwelt: zusätzliche Stufen unter y = 0.
{{config mobs.levels.belowZeroBonus}}

`mobs.levels.dimensionBonus` — Zusätzliche Stufen nach Dimensions-ID (standardmäßig der Nether und das Ende). Füge hier Dimensionen anderer Mods hinzu.
{{config mobs.levels.dimensionBonus}}

`mobs.levels.healthPerLevel` — Gesundheit pro Stufe über 1, als Bruchteil des Grundwerts.
{{config mobs.levels.healthPerLevel}}

`mobs.levels.damagePerLevel` — Schaden pro Stufe über 1, als Bruchteil des Grundwerts.
{{config mobs.levels.damagePerLevel}}

`mobs.levels.experiencePerLevel` — Fallengelassene Erfahrung pro Stufe über 1, als Bruchteil des Grundwerts.
{{config mobs.levels.experiencePerLevel}}

`mobs.levels.healthScale` — Ein Faktor auf die Grundgesundheit eines Gegnertyps unter seiner Stufe, nach Entitäts-ID (nicht aufgeführt: 1).
{{config mobs.levels.healthScale}}

`mobs.levels.damageScale` — Ein Faktor auf den Grundschaden eines Gegnertyps unter seiner Stufe, nach Entitäts-ID (nicht aufgeführt: 1).
{{config mobs.levels.damageScale}}

`mobs.difficulty.easy` — Schadensfaktor feindseliger Gegner gegen Spieler auf Leicht (ersetzt den von Vanilla).
{{config mobs.difficulty.easy}}

`mobs.difficulty.normal` — Dasselbe auf Normal.
{{config mobs.difficulty.normal}}

`mobs.difficulty.hard` — Dasselbe auf Schwer.
{{config mobs.difficulty.hard}}

### sanctum: Verfallenes Heiligtum

`sanctum.mapChanceCartographer` — Chance auf eine Karte des Verfallenen Heiligtums in einer Truhe eines Dorfkartografen.
{{config sanctum.mapChanceCartographer}}

`sanctum.mapChanceTemple` — Dasselbe in einer Truhe eines Dorftempels.
{{config sanctum.mapChanceTemple}}

`sanctum.mapChanceRuinedPortal` — Dasselbe in der Truhe eines Ruinenportals.
{{config sanctum.mapChanceRuinedPortal}}

`sanctum.mapChanceShipwreck` — Dasselbe in einer Kartentruhe eines Schiffswracks.
{{config sanctum.mapChanceShipwreck}}

`sanctum.mapSearchRadius` — Wie weit, in Chunks, eine neue Karte des Verfallenen Heiligtums nach einem Heiligtum sucht.
{{config sanctum.mapSearchRadius}}

`sanctum.hallGuardChance` — Chance, dass ein Wachplatz in den Seitenhallen einen Hohlen enthält, bei der Weltgenerierung gewürfelt.
{{config sanctum.hallGuardChance}}

`sanctum.hallArcherChance` — Chance, dass ein Bogenschützenplatz in den Seitenhallen ein Skelett enthält, bei der Weltgenerierung gewürfelt.
{{config sanctum.hallArcherChance}}

### sanctumGear: Heiligtumswaffen

`sanctumGear.cleaveRange` — Weiter Spalthieb (Heiligtums-Langschwert): Reichweite in Blöcken.
{{config sanctumGear.cleaveRange}}

`sanctumGear.cleaveArc` — Weiter Spalthieb: Bogen vor dir, in Grad.
{{config sanctumGear.cleaveArc}}

`sanctumGear.cleaveMultiplier` — Weiter Spalthieb: Anteil des Waffentreffers, den jedes Ziel erleidet.
{{config sanctumGear.cleaveMultiplier}}

`sanctumGear.cleaveStamina` — Weiter Spalthieb: Ausdauerkosten.
{{config sanctumGear.cleaveStamina}}

`sanctumGear.cleaveCooldown` — Weiter Spalthieb: Abklingzeit in Ticks.
{{config sanctumGear.cleaveCooldown}}

`sanctumGear.slamRadius` — Erdschlag (Streitkolben des Aschenträgers): Ringradius.
{{config sanctumGear.slamRadius}}

`sanctumGear.slamMultiplier` — Erdschlag: Anteil des Waffentreffers.
{{config sanctumGear.slamMultiplier}}

`sanctumGear.slamKnockback` — Erdschlag: Rückstoßstärke.
{{config sanctumGear.slamKnockback}}

`sanctumGear.slamSlownessTicks` — Erdschlag: Dauer der Verlangsamung in Ticks.
{{config sanctumGear.slamSlownessTicks}}

`sanctumGear.slamSlownessAmplifier` — Erdschlag: Verstärkung der Verlangsamung (1 = Verlangsamung II).
{{config sanctumGear.slamSlownessAmplifier}}

`sanctumGear.slamStamina` — Erdschlag: Ausdauerkosten.
{{config sanctumGear.slamStamina}}

`sanctumGear.slamCooldown` — Erdschlag: Abklingzeit in Ticks.
{{config sanctumGear.slamCooldown}}

### boss: Bosse

`boss.healthPerExtraFighter` — Gesundheitsbonus des Bosses pro Kämpfer über den ersten hinaus: Gesundheit × (1 + dieser Wert × (Kämpfer − 1)).
{{config boss.healthPerExtraFighter}}

`boss.addsPerExtraFighter` — Zusätzliche Diener pro Kämpfer über den ersten hinaus.
{{config boss.addsPerExtraFighter}}

`boss.addLevelOffset` — Um wie viele Stufen seine Diener unter dem Boss liegen.
{{config boss.addLevelOffset}}

`boss.targetSwitchMinSeconds` — Kürzeste Zeit zwischen zwei Zielwechseln eines Bosses (bei zwei oder mehr Kämpfern).
{{config boss.targetSwitchMinSeconds}}

`boss.targetSwitchMaxSeconds` — Längste Zeit zwischen zwei Zielwechseln.
{{config boss.targetSwitchMaxSeconds}}

`boss.repeatSoulChance` — Chance auf die Seele des Bosses für einen Kämpfer, der ihn schon besiegt hatte (nie bei einem Rematch).
{{config boss.repeatSoulChance}}

### reachMobs: Spawns in Aschenmark

`reachMobs.spawns` — Natürliche Spawns der Gegner von Aschenmark in ihrer Zone: `weight`, `minGroup` und `maxGroup` pro Gegner. Gewicht 0 schaltet das Spawnen eines Gegners ab. Wird beim Start gelesen.
{{config reachMobs.spawns}}

### gear: Reichsausrüstung

`gear.bleeding.durationTicks` — Wie lange Blutung anhält; jeder neue Treffer startet sie erneut.
{{config gear.bleeding.durationTicks}}

`gear.bleeding.maxLevel` — Höchste Blutungsstufe.
{{config gear.bleeding.maxLevel}}

`gear.bleeding.damagePerLevel` — Blutungsschaden pro Sekunde und Stufe, Rüstung ignorierend.
{{config gear.bleeding.damagePerLevel}}

`gear.smoldering.fireReductionPerPiece` — Anteil des Feuerschadens, den jedes getragene Teil der Schwelenden Rüstung abnimmt.
{{config gear.smoldering.fireReductionPerPiece}}

`gear.sets.reaperSpeed` — Schnitterset: Bonus auf die Bewegungsgeschwindigkeit (Anteil des Grundwerts).
{{config gear.sets.reaperSpeed}}

`gear.sets.huntsmanProjectile` — Jägerset: Bonus auf den Projektilschaden.
{{config gear.sets.huntsmanProjectile}}

`gear.sets.wardenKnockbackResist` — Kerkermeisterset: hinzugefügte Rückstoßresistenz.
{{config gear.sets.wardenKnockbackResist}}

`gear.sets.wardenBlockingReduction` — Kerkermeisterset: Anteil des Schadens, der beim Blocken mit einem Schild abgenommen wird.
{{config gear.sets.wardenBlockingReduction}}

`gear.uniques.scarecrowDetection` — Vogelscheuchenmaske: Anteil der üblichen Entfernung, aus der Gegner dich bemerken.
{{config gear.uniques.scarecrowDetection}}

`gear.uniques.greavesSpeedTicks` — Jägerbeinschienen: Ticks an Tempo nach einem Kill.
{{config gear.uniques.greavesSpeedTicks}}

`gear.uniques.greavesSafeFallDamage` — Jägerbeinschienen: abgenommener Fallschaden (4 = keiner bei bis zu 7 Blöcken).
{{config gear.uniques.greavesSafeFallDamage}}

`gear.uniques.novaHealthFraction` — Ofenherz-Kürass: Die Nova geht unterhalb dieses Anteils deiner maximalen Gesundheit los.
{{config gear.uniques.novaHealthFraction}}

`gear.uniques.novaRadius` — Ofenherz-Kürass: Radius der Nova.
{{config gear.uniques.novaRadius}}

`gear.uniques.novaDamage` — Ofenherz-Kürass: Glutschaden an jedem Gegner in der Nova.
{{config gear.uniques.novaDamage}}

`gear.uniques.novaCooldownTicks` — Ofenherz-Kürass: Ticks zwischen zwei Novas.
{{config gear.uniques.novaCooldownTicks}}

`gear.weapons.kindlingPerPoint` — Glutschaden-Bonus pro vollem Glutpunkt.
{{config gear.weapons.kindlingPerPoint}}

`gear.weapons.sweepRadius` — Aschensense: Radius des Schwungs um das Ziel.
{{config gear.weapons.sweepRadius}}

`gear.weapons.sweepShare` — Aschensense: Anteil des Treffers, den der Schwung austeilt.
{{config gear.weapons.sweepShare}}

`gear.weapons.sweepStamina` — Aschensense: zurückgegebene Ausdauer pro Feind, den ein Schwung oder die Ernte erfasst.
{{config gear.weapons.sweepStamina}}

`gear.weapons.waveLength` — Aschensense: Länge der Aschewelle eines voll aufgeladenen Treffers.
{{config gear.weapons.waveLength}}

`gear.weapons.waveWidth` — Aschensense: Breite der Aschewelle.
{{config gear.weapons.waveWidth}}

`gear.weapons.waveDamage` — Aschensense: Glutschaden der Aschewelle.
{{config gear.weapons.waveDamage}}

`gear.weapons.harvestRadius` — Ernte: Radius der Drehung.
{{config gear.weapons.harvestRadius}}

`gear.weapons.harvestStamina` — Ernte: Ausdauerkosten.
{{config gear.weapons.harvestStamina}}

`gear.weapons.harvestCooldown` — Ernte: Abklingzeit in Ticks.
{{config gear.weapons.harvestCooldown}}

`gear.weapons.lashPull` — Peitschenhieb: Blöcke, um die das Ziel herangezogen wird.
{{config gear.weapons.lashPull}}

`gear.weapons.lashStamina` — Peitschenhieb: Ausdauerkosten.
{{config gear.weapons.lashStamina}}

`gear.weapons.lashCooldown` — Peitschenhieb: Abklingzeit in Ticks.
{{config gear.weapons.lashCooldown}}

`gear.weapons.lungeDash` — Ausfall: Länge des Sprints in Blöcken.
{{config gear.weapons.lungeDash}}

`gear.weapons.lungeStamina` — Ausfall: Ausdauerkosten.
{{config gear.weapons.lungeStamina}}

`gear.weapons.lungeCooldown` — Ausfall: Abklingzeit in Ticks.
{{config gear.weapons.lungeCooldown}}

`gear.weapons.rampPerHit` — Königliches Aschenrapier: Schaden, der pro voll aufgeladenem Treffer in Folge auf dasselbe Ziel hinzukommt.
{{config gear.weapons.rampPerHit}}

`gear.weapons.rampMax` — Königliches Aschenrapier: das Meiste, was die Steigerung hinzufügt.
{{config gear.weapons.rampMax}}

`gear.weapons.rampWindowTicks` — Königliches Aschenrapier: längste Pause zwischen zwei Treffern, die die Steigerung erhält.
{{config gear.weapons.rampWindowTicks}}

`gear.weapons.whipTipBand` — Geißel des Kerkermeisters: die Länge der Spitze, der letzten Blöcke des Peitschenhiebs, die knallt.
{{config gear.weapons.whipTipBand}}

`gear.weapons.whipTipMultiplier` — Geißel des Kerkermeisters: Schadensfaktor eines Treffers mit der Spitze.
{{config gear.weapons.whipTipMultiplier}}

`gear.weapons.whipTipStunTicks` — Geißel des Kerkermeisters: Ein Treffer mit der Spitze hält den Gegner an und unterbricht seine Ausholbewegung für so viele Ticks.
{{config gear.weapons.whipTipStunTicks}}

`gear.crossbow.chargeTicks` — Jägerarmbrust: Ladezeit ohne Schnellladen.
{{config gear.crossbow.chargeTicks}}

`gear.crossbow.boltDamage` — Jägerarmbrust: Schaden eines Bolzens bei voller Geschwindigkeit.
{{config gear.crossbow.boltDamage}}

`gear.crossbow.punch` — Jägerarmbrust: Rückstoßstufe der Bolzen.
{{config gear.crossbow.punch}}

`gear.crossbow.pierce` — Jägerarmbrust: zusätzliche Gegner, die ein Bolzen durchschlägt.
{{config gear.crossbow.pierce}}

`gear.crossbow.openerMultiplier` — Jägerarmbrust: Schadensfaktor gegen einen Gegner, der nicht auf dich zielt.
{{config gear.crossbow.openerMultiplier}}

`gear.consumables.bombRadius` — Aschebombe: Radius des Feuerrings.
{{config gear.consumables.bombRadius}}

`gear.consumables.bombDamage` — Aschebombe: Glutschaden.
{{config gear.consumables.bombDamage}}

`gear.consumables.bombFireSeconds` — Aschebombe: Sekunden, die die erfassten Gegner brennen.
{{config gear.consumables.bombFireSeconds}}

`gear.consumables.resinSeconds` — Schwelharz: wie lange eine überzogene Waffe entzündet, was sie trifft.
{{config gear.consumables.resinSeconds}}

`gear.consumables.resinFireSeconds` — Schwelharz: Sekunden, die ein Ziel brennt.
{{config gear.consumables.resinFireSeconds}}

`gear.consumables.balmRegenTicks` — Bernsteinbalsam: Dauer seiner Regeneration.
{{config gear.consumables.balmRegenTicks}}

`gear.consumables.oilMinutes` — Bernstein-Laternenöl: wie lange eine Laterne brennt.
{{config gear.consumables.oilMinutes}}

`gear.consumables.whetstoneShare` — Wetzstein: Anteil der maximalen Haltbarkeit, der repariert wird.
{{config gear.consumables.whetstoneShare}}

`gear.consumables.cinderFuelTicks` — Schlackenscherbe als Ofenbrennstoff, in Ticks (Kohle sind 1600). Wird beim Start gelesen.
{{config gear.consumables.cinderFuelTicks}}

`gear.consumables.flaskEmberBonus` — Flakonglut: zusätzliche Heilung des Flakons.
{{config gear.consumables.flaskEmberBonus}}

`gear.loot.fodderMaterial` — Chance auf das Material der Zone von einem natürlichen Gegner von Aschenmark.
{{config gear.loot.fodderMaterial}}

`gear.loot.eliteMaterial` — Chance auf 1–2 Stück des Materials der Zone von einem platzierten Tank oder Elitegegner.
{{config gear.loot.eliteMaterial}}

`gear.loot.fodderSet` — Chance auf ein Setteil von einem natürlichen Gegner (nur bei Kills durch Spieler).
{{config gear.loot.fodderSet}}

`gear.loot.eliteSet` — Chance auf ein Setteil von einem platzierten Tank oder Elitegegner.
{{config gear.loot.eliteSet}}

`gear.loot.fodderUnique` — Chance auf das einzigartige Teil der Zone von ihren Gegnern.
{{config gear.loot.fodderUnique}}

`gear.loot.eliteUnique` — Chance auf das einzigartige Teil der Zone von ihrem Elitegegner.
{{config gear.loot.eliteUnique}}

`gear.loot.lootingBonus` — Chance, die pro Plünderungsstufe hinzukommt (ein Viertel davon bei einzigartigen Teilen).
{{config gear.loot.lootingBonus}}

### reliquary: Reliquiar

`reliquary.slots` — Plätze des Reliquiars pro Art: `artifact` und `ring` (0 blendet eine Art aus, höchstens 8).
{{config reliquary.slots}}

`reliquary.artifacts.points` — Wertpunkte eines Artefakts mit einem Wert.
{{config reliquary.artifacts.points}}

`reliquary.artifacts.sealPoints` — Punkte, die das Siegel des Aschenkönigs jedem Wert hinzufügt.
{{config reliquary.artifacts.sealPoints}}

`reliquary.rings.courierSprintBonus` — Ring des Kuriers: zusätzliches Sprinttempo.
{{config reliquary.rings.courierSprintBonus}}

`reliquary.rings.featherSafeFall` — Ring der Aschefedern: Stürze bis zu so vielen Blöcken richten keinen Schaden an.
{{config reliquary.rings.featherSafeFall}}

`reliquary.rings.featherMaxFallSpeed` — Ring der Aschefedern: schnellster Fall, Blöcke pro Tick.
{{config reliquary.rings.featherMaxFallSpeed}}

`reliquary.rings.fogStalkerClearing` — Ring des Nebelpirschers: um das Wievielfache weiter außen der Nebel des Reichs beginnt und endet.
{{config reliquary.rings.fogStalkerClearing}}

`reliquary.rings.delverHasteLevel` — Ring des Schürfers: Eile-Stufe beim Abbau von Erzen des Reichs.
{{config reliquary.rings.delverHasteLevel}}

`reliquary.rings.embersFireSeconds` — Ring der Glut: Sekunden, die ein Nahkampftreffer das Ziel in Brand setzt.
{{config reliquary.rings.embersFireSeconds}}

`reliquary.drops.chestArtifact` — Chance auf ein Artefakt in der Schatztruhe einer Zone.
{{config reliquary.drops.chestArtifact}}

`reliquary.drops.chestRing` — Chance auf einen Ring in der Schatztruhe einer Zone.
{{config reliquary.drops.chestRing}}

`reliquary.drops.eliteArtifact` — Chance auf ein Artefakt vom Elitegegner einer Zone (Kills durch Spieler).
{{config reliquary.drops.eliteArtifact}}

`reliquary.drops.eliteRing` — Chance auf einen Ring vom Elitegegner einer Zone (Kills durch Spieler).
{{config reliquary.drops.eliteRing}}

### moon: Mondnächte

`moon.enabled` — Mondnächte an oder aus (der Debug-Befehl startet trotzdem eine).
{{config moon.enabled}}

`moon.cycleInterval` — Ein Mondnacht-Zyklus alle so viele Mondzyklen von 8 Tagen; der erste Vollmond der Welt zählt nie.
{{config moon.cycleInterval}}

`moon.fallbackMinutes` — Echtzeit-Minuten, nach denen eine Mondnacht endet, auch wenn ihre Wellen nicht besiegt sind.
{{config moon.fallbackMinutes}}

`moon.fullMoon.levelBonus` — Zusätzliche Stufen für feindselige Gegner der Oberwelt, die bei Vollmond spawnen.
{{config moon.fullMoon.levelBonus}}

`moon.fullMoon.followRange` — Multiplikator der Verfolgungsreichweite für feindselige Gegner der Oberwelt bei Vollmond.
{{config moon.fullMoon.followRange}}

`moon.blood.vampirism` — Anteil des Schadens, den ein Feind einem Spieler zufügt und der ihn bei einem Blutneumond heilt.
{{config moon.blood.vampirism}}

### nightMobs: Wellen des Vollmonds

`nightMobs.zombieLevelBonus` — Stufen, die ein Aschenzombie einer Vollmondwelle zusätzlich zur Stufe des Ortes bekommt.
{{config nightMobs.zombieLevelBonus}}

`nightMobs.ghostMissChance` — Anteil der Angriffe auf einen Geist, die durch ihn hindurchgehen.
{{config nightMobs.ghostMissChance}}

`nightMobs.maxZombies` — Die meisten Aschenzombies in der Welle eines Spielers (einer pro Stapel Fluch der Nacht).
{{config nightMobs.maxZombies}}

`nightMobs.ghostCount` — Geister, die einem Spieler ohne Fluch geschickt werden.
{{config nightMobs.ghostCount}}

`nightMobs.spawnMinDistance` — Mindestzahl an Blöcken vom Spieler, in der eine Welle spawnt.
{{config nightMobs.spawnMinDistance}}

`nightMobs.spawnMaxDistance` — Höchstzahl an Blöcken vom Spieler, in der eine Welle spawnt.
{{config nightMobs.spawnMaxDistance}}

### vampire: Der Vampir

`vampire.provokeMinutes` — Echtzeit-Minuten, bevor sich der Vampir verwandelt, der einem Spieler am nächsten ist, wenn niemand einen angegriffen hat.
{{config vampire.provokeMinutes}}

`vampire.groupRadius` — Spieler, die so nah beieinander sind, teilen sich eine Gruppe Vampire.
{{config vampire.groupRadius}}

`vampire.groupMin` — Wenigste Vampire in einer Gruppe.
{{config vampire.groupMin}}

`vampire.groupMax` — Meiste Vampire in einer Gruppe.
{{config vampire.groupMax}}

`vampire.bossMinLevel` — Niedrigste Stufe des Vampir-Bosses.
{{config vampire.bossMinLevel}}

`vampire.bossCapNoRealm` — Höchste Stufe des Bosses, solange keiner der Teilnehmer ein Reich freigeschaltet hat.
{{config vampire.bossCapNoRealm}}

`vampire.firstKillShards` — Flakonscherben für den ersten Vampir-Kill eines Spielers (plus ein Vampirzahn).
{{config vampire.firstKillShards}}

`vampire.repeatShardChance` — Chance auf eine Flakonscherbe bei späteren Kills.
{{config vampire.repeatShardChance}}

### setpieces: Mechanik

`setpieces.maxPerChunk` — Meiste bewegliche Mechanik (Windmühlenflügel, Glocken, Weihrauchfässer …), die ein Chunk enthalten darf; überzählige werden zu Luft.
{{config setpieces.maxPerChunk}}

`setpieces.animate` — Ob sich die Mechanik überhaupt bewegt. Aus: Sie steht in ihrer Ruhepose. Mit den Clients synchronisiert.
{{config setpieces.animate}}

Weiter: [Datenpakete und Kompatibilität](datapacks.md)
