---
title: Datenpakete und Kompatibilität
kind: guide
order: 8
source: a2501f21866e
---

Vieles an Embervoid sind Daten: Ein Datenpaket kann ändern, wo seine Dungeons erscheinen, was seine Truhen enthalten und
wie sich die Inhalte anderer Mods einfügen. Die Pfade unten liegen im Ordner `data/` eines Datenpakets.

## Weltgenerierung, die du überschreiben kannst

| Datei | Was sie steuert |
|---|---|
| `embervoid/worldgen/structure_set/ember_sanctum.json` | wie oft Verfallene Heiligtümer erscheinen: standardmäßig `spacing` 22 und `separation` 8 Chunks (etwa eines pro 300–400 Blöcke). Erhöhe beide für seltenere Dungeons. |
| `embervoid/tags/worldgen/biome/has_structure/ember_sanctum.json` | die Biome, in denen ein Verfallenes Heiligtum stehen kann |
| `embervoid/worldgen/structure/ember_sanctum.json`, `embervoid/worldgen/template_pool/sanctum/*` | der Dungeon selbst und seine Teile |
| `embervoid/tags/worldgen/structure/on_sanctum_maps.json` | die Bauwerke, auf die eine Heiligtumskarte zeigt |
| `embervoid/worldgen/biome/ashen_reach_*.json` | das Aussehen des Reichs pro Zone: Farben, Partikel, Umgebungsgeräusche |
| `embervoid/worldgen/configured_feature/ashen_reach/*`, `placed_feature/ashen_reach/*` | die Erze, Bäume, Trümmer und Straßen des Reichs |

Sei vorsichtig mit der Dimension, den Noise-Einstellungen und den Structure-Sets des Reichs
(`embervoid/dimension/ashen_reach.json`, `embervoid/worldgen/noise_settings/ashen_reach.json`,
`embervoid/worldgen/structure_set/ashen_reach_*`). Die Zonen, Arenen, das Tor und die Zitadelle des Reichs werden per
Code mit denselben Zahlen angelegt; wer nur eine Seite ändert, zerstört das Reich. Lass den Hub und die Arena des
Vampirs (`embervoid/dimension/hub.json`, `vampire_arena.json`) in Ruhe.

Die Gegner von Aschenmark spawnen nicht über die Biomdateien (ihre Spawner-Listen sind absichtlich leer). Ändere ihre
Spawns mit `reachMobs.spawns` in der [Konfiguration](configuration.md).

## Beutetabellen

- `embervoid/loot_tables/chests/sanctum_outer_hall.json`: die Hallentruhen des Verfallenen Heiligtums.
- `embervoid/loot_tables/gameplay/fallen_sister_first_clear.json`: die zusätzliche Beute beim ersten Sieg eines Spielers
  im Heiligtum.
- `embervoid/loot_tables/chests/ashen_reach/*`: die Truhen des Reichs, drei Arten pro Zone (gewöhnlich, Arena, Schatz).
- `embervoid/loot_tables/entities/*`: Drops von Gegnern und Bossen.

Manche Drops fügt die Mod zusätzlich zu diesen Tabellen hinzu: die Reichsmaterialien, Rüstungssetteile und einzigartigen
Teile bei den Gegnern des Reichs, die Ringe und Artefakte in Schatztruhen und bei Elitegegnern und die Karte des
Verfallenen Heiligtums in Vanilla-Truhen. **Wenn dein Datenpaket eine dieser Tabellen ersetzt, werden diese Ergänzungen
bei ihr nicht vorgenommen**: Deine Tabelle entscheidet allein. Ihre Chancen stehen stattdessen in der Konfiguration
(`gear.loot.*`, `reliquary.drops.*`, `sanctum.mapChance*`).

## Tags

| Tag | Bedeutung |
|---|---|
| `embervoid:unleveled` (Entitätstypen) | Gegner, die nie eine Stufe bekommen: der Enderdrache, der Wither, der Wärter, der Große Wächter. Füge hier Bosse anderer Mods hinzu. |
| `embervoid:bleed_immune` (Entitätstypen) | Gegner, die nie bluten |
| `embervoid:breaks_guard` (Gegenstände) | Waffen, die die Deckung eines Hohlen wie eine Axt brechen (der Streitkolben des Aschenträgers) |
| `embervoid:artifacts`, `embervoid:rings` (Gegenstände) | was in die Artefakt- und Ringplätze des Reliquiars passt |
| `embervoid:realm_ores` (Blöcke) | die Erze, bei deren Abbau der Ring des Schürfers Eile gibt |
| `embervoid:dead_logs` (Blöcke, Gegenstände) | die Totholzstämme |
| `embervoid:ashen_reach*` (Biome) | die Biome des Reichs, alle und pro Zone |

## Andere Mods

Bisher sind keine Konflikte bekannt. Dinge, die du im Kopf behalten solltest:

- **Gegner aus anderen Mods** bekommen wie Vanilla-Gegner eine Stufe, mit der Standard-Grundstufe. Gib ihnen in
  `mobs.levels.baseLevels` eine passende, und setze ihre Bosse in `embervoid:unleveled`, wenn sie so bleiben sollen, wie
  ihr Autor sie gemacht hat.
- **Grab- oder Leichen-Mods** greifen ebenfalls beim Tod ein. Nutze ein System: Setze entweder `remnant.mode` auf `off`
  oder lass die andere Mod weg.
- **Schadens- und Schwierigkeits-Mods:** Embervoid ersetzt den Schwierigkeitsfaktor von Vanilla für den Schaden von
  Gegnern an Spielern (`mobs.difficulty.*`). Eine andere Mod, die dasselbe ändert, kann sich damit addieren.
- Die Wertboni von Embervoid nutzen Vanilla-Attribute, wo Vanilla eines hat (maximale Gesundheit, Angriffstempo,
  Rückstoßresistenz, Rüstungshärte, Glück), sodass Mods, die Attribute lesen, sie sehen.

Weiter: [Befehle](commands.md)
