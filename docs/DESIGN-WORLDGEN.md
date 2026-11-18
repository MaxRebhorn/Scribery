# Weltgenerierung

Wie die Bauwerke, Wracks und Funde in die Welt kommen. Die Daten erzeugt `tools/gen_worldgen.py` (Ausgabe unter `mod/src/main/resources/data/scribery/`), der Code steht in `ScriberyStructure`, `ScriberyPiece` und `ModStructures`.

## Strukturen

Alle Strukturen nutzen **dieselben Generatoren wie die Debug-Befehle** (`DebugStructures`, `DebugWreck`). Eine Struktur der Weltgenerierung besteht aus einem Stück (`ScriberyPiece`), das bei jedem betroffenen Chunk die ganze Struktur neu baut, aber nur in den Bereich dieses Chunks schreibt (`StructureBuilder.CLIP`). Alle Zufallszahlen kommen aus Ursprung und Seed, darum ergibt jeder Durchlauf dasselbe Bauwerk und die Teile an den Chunkgrenzen passen zusammen. Gelesen wird nur innerhalb des Chunks (`StructureBuilder.stateAt`), außerhalb gilt Luft.

| Struktur | Biome | Ort | Abstand / Mindestabstand (Chunks) |
|---|---|---|---|
| `wreck` (Handelsschiff, 25 % auf der Seite, Ladung 2–9) | alle Ozeane | tiefes Wasser (Boden mindestens 4 Blöcke unter dem Meeresspiegel, mindestens 6 Blöcke Wasser) | 24 / 10 |
| `caravan` (gefrorene Karawane) | snowy_plains, ice_spikes, snowy_taiga | trocken an Land | 28 / 12 |
| `wanderer` (gefrorener Wanderer) | snowy_plains, ice_spikes, snowy_taiga, snowy_slopes | trocken an Land | 18 / 7 |
| `grave` (Soldatengrab) | swamp, mangrove_swamp | trocken an Land | 16 / 6 |
| `catacomb` (Katakombenkirche, Ziegel) | plains, sunflower_plains, meadow, Wälder, taiga, savanna | unterirdisch, Treppe an der Oberfläche | 36 / 14 |
| `catacomb_desert` (Sandstein) | desert | unterirdisch | 30 / 12 |
| `house_sand` (verschüttetes Haus) | desert | knapp unter der Oberfläche, Suspicious Sand als Hinweis | 14 / 6 |
| `house_mud` (verschüttetes Haus, Lehm) | swamp, mangrove_swamp, river | knapp unter der Oberfläche, Suspicious Mud als Hinweis | 14 / 6 |

Biome stehen in `data/scribery/tags/worldgen/biome/has_structure/<name>.json`, Abstände in `worldgen/structure_set/<name>.json`. Zum Ändern `tools/gen_worldgen.py` anpassen und neu ausführen.

## Funde (Blöcke an der Oberfläche)

Als `ore`-Features, die einzelne Blöcke der Oberfläche ersetzen, Schritt `top_layer_modification` (nach Eis und Schnee). Biome-Zuordnung über NeoForge-Biome-Modifier (`neoforge/biome_modifier/`).

| Fund | ersetzt | Biome |
|---|---|---|
| Suspicious Sand | Sand | desert |
| Suspicious Mud (Land und unter Wasser) | Schlamm, Erde, Gras, Lehm, Kies, Sand | swamp, mangrove_swamp |
| Suspicious Mud (selten, Wasserschaden) | wie oben | river, beach |
| Frozen Fragment Block | Eis, Packeis, Blaues Eis | ice_spikes, frozen_ocean, deep_frozen_ocean, frozen_river, snowy_plains, snowy_taiga, frozen_peaks, jagged_peaks, snowy_slopes |

**Suspicious Ashes** kommen nicht in die Weltgenerierung. Sie bleiben ein Fund in Bauwerken (zum Beispiel später ein verbranntes Gebäude). Nicht im Nether: Das wäre inhaltlich unpassend.

## Keine Monster in den Katakomben

Die Gänge sind dunkel, weil dort kein Licht brennt (nur ausgeschaltete Öllampen). Damit dort keine Monster spawnen, bricht `CatacombSpawnGuard` natürliches Spawnen und Spawnen bei der Chunk-Erzeugung für Monster ab, wenn in der Nähe (2 Blöcke seitlich, 4 darunter, 1 darüber) ein Block aus dem Tag `scribery:catacomb_blocks` liegt (römische Ziegel, Bodenplatten, Mosaike). Spawner, Spawn-Eier und Befehle sind nicht betroffen.

## Stand der Tests

Mit dem Testserver (flache Welt je Biom, echte Weltgenerierung) geprüft: alle Strukturen außer dem Wrack, die Funde in Wüste, Sumpf und gefrorenem Meer. Das Wrack setzt tiefes Wasser voraus, das die flache Testwelt nicht hat; der Bau selbst ist über `/scribery_wreck` getestet, die Bedingung für die Platzierung (Wassertiefe) noch nicht in einer echten Welt.

## Testen

`gradlew runServer -Pwgtest=scribery:catacomb,scribery:grave` startet einen lokalen Testserver (Welt unter `run/`, Einstellungen in `run/server.properties`), setzt die Strukturen mit `/place structure` über die echte Weltgenerierung, zählt die Blöcke des Mods im Bereich und beendet sich (`WorldgenTest`). Mit `-Pwgtest=features` zählt er stattdessen die Funde in 25 × 25 Chunks. Ohne die Eigenschaft passiert nichts. Für ein Biom nimmt man am besten `level-type=minecraft:single_biome_surface` mit `generator-settings={"biome":"minecraft:desert"}`.
