# Design: Funde, Dubletten, Smaragde (ENTWURF)

Status: Vorschlag aus der Brainstorming-Runde, nichts davon ist gebaut oder kompiliert. Alle Zahlen sind Schätzungen und müssen im Spiel getestet werden.

## Ausgangsproblem

Ein Biom hat rund 140 Manuskripte (`docs/FRAGMENT-BIOMES.md`). Bei rein zufälligen Funden:

- die Hälfte des Bioms nach etwa 100 Funden
- die letzten 14 Manuskripte brauchen allein etwa 450 weitere Funde
- ein Biom komplett: im Schnitt etwa 770 Funde, ganz Johannes über 8.000

Der Grind steckt im hinteren Drittel, und es entstehen viele Dubletten. Darum: Fundhilfen und Verkauf der Dubletten.

## Fundhilfen (Quick Wins)

1. **Mehr Quellen:** verdächtiger Schlamm und Scherben, kleine Chance in Truhen von Dörfern und Ruinen, Archäologie-Ruinen, Fischen im passenden Biom, Villager-Trades. Ziel: etwa alle 10 bis 15 Minuten ein Fund, egal was man tut.
2. **Erstfund garantiert:** das erste Fund-Ereignis in einem Biom liefert ein leichtes Manuskript.
3. **Schlechtes-Glück-Schutz:** etwa 70 % der Funde ziehen ein Manuskript, das der Spieler noch nicht hat (braucht die gespeicherte Sammlung, also das Manuskriptarchiv).
4. **Seiten abschließen:** weil jede Seite in genau einem Biom liegt, kann man dort eine Seite wirklich fertig bekommen.
5. **Wegweiser:** Kartograf oder Gelehrter verkauft eine Karte zum nächsten Biom, das die gesuchte Geschichte liefert.
6. **Meilensteine:** Belohnung für eine fertige Seite, nicht erst für eine ganze Geschichte.

## Dubletten verkaufen

- Preis nach Stufe: leicht 1, mittel 2, schwer 3 Smaragde, begrenzter Vorrat pro Tag.
- Kein direkter Rückkauf von Fragmenten (sonst Smaragd-Kreislauf). Stattdessen Tausch 3 Dubletten gegen ein zufälliges Manuskript eines gewählten Bioms, und Fundkarten gegen Smaragde.
- Vanilla-Villager können Dubletten nicht erkennen. Empfohlen: nur **lose** Manuskripte (nicht im Archiv eingetragen) sind verkaufbar. Alternative: eigener Händler-Block oder NPC mit eigener Oberfläche (aufwendiger).

## Fundblöcke und Aussehen des Fragments (entschieden 18.11.2026)

Der **Block**, in dem ein Fragment steckt, bestimmt sein Aussehen. Der Codex-Look entfällt. Es sind 4 Zustandsvarianten (`custom_model_data` 1 bis 4) plus das normale Fragment.

| Fundblock | Fragment sieht aus wie | Wo (Biome) |
|---|---|---|
| Suspicious Sand | normal, zufällig verblasst | Wüste |
| Suspicious Mud | wassergeschädigt | Fluss, Strand, Sumpf |
| Suspicious Ashes | verkohlt (uralte, kalte Asche, nichts glüht) | **[Vorschlag]** Wald, Dschungel |
| Eisblock mit Fragment | gefroren | **[Vorschlag]** Berg (kalt) |
| Scherbenhaufen | normal | **[Vorschlag]** Ebene, Savanne |
| Amphore | normal (Behälter, siehe Roadmap Punkt 5a) | überall in Ruinen |

- Der Dschungel ist laut Ansage egal und passt zum Schlamm oder zur Asche. Die Zuordnung der übrigen Biome ist noch nicht festgelegt.
- Alle drei „Suspicious“-Blöcke nutzen dieselbe Block-Klasse (`SuspiciousFindBlock`) mit dem Zustand `dusted` 0 bis 3. Pinsel-Logik gibt es noch nicht.
- Offen: **Wie genau entscheidet der Biom-Eintrag den Block?** Vorschlag: pro Biom eine Gewichtung (Wüste 100 % Sand, Sumpf 100 % Schlamm, Wald Asche und Schlamm gemischt) in `tools/data/fragment-biomes.json`.

## Loot-Tabellen (Stand 18.11.2026, im Mod, noch nicht im Spiel geladen)

Erzeugt von `tools/gen_loot_tables.py`, die Pools von `tools/export_find_pools.py` (`tools/data/find-pools.json`). Alle Zahlen sind Schätzungen.

| Block | Gibt zurück |
|---|---|
| Suspicious Sand, Mud, Ashes | der Block selbst; 40 % ein Manuskript der Fundart; 35 % 1 bis 2 Papyrusfetzen |
| Eisblock mit Fragment | der Block selbst; 60 % ein gefrorenes Manuskript |
| Scherbenhaufen | der Block selbst; 1 bis 3 Amphorenscherben; 12 % ein Ostrakon; 25 % ein Manuskript |
| Amphore | mit Behutsamkeit (Silk Touch) die Amphore, sonst 2 bis 4 Scherben, 30 % ein Manuskript, 25 % Papyrusfetzen |

- **Loot-Funktion `scribery:random_manuscript`** (`"find": "sand" | "mud" | "ashes" | "ice" | "shards"`): zieht ein zufälliges Manuskript aus dem Pool der Fundart und setzt das Aussehen (Schlamm wassergeschädigt, Asche verkohlt, Eis gefroren, Sand zu 35 % verblasst, Scherben normal).
- **Pools:** Sand 388, Schlamm 438, Eis 295, Asche 272, Scherben 147 Manuskripte (Summe 1.540). Die Zuordnung der elf Biome steht in `BIOME_TO_FIND` in `tools/export_find_pools.py`.
- **Papyrusfetzen:** nur Brennstoff (100 Ticks), sonst zu nichts zu gebrauchen.
- **Noch offen:** Ostraka mit Witzen auf Altgriechisch (Joke-Items, keine Manuskripte), Schutz vor Dubletten (braucht die gespeicherte Sammlung), Welt-Erzeugung der Blöcke.

## Offene Risiken

- Zu großzügig, und der Reiz geht verloren; die Menge allein ermüdet auch ohne Grind.
- Smaragde sind in Vanilla leicht zu bekommen, ein Rückkauf lässt sich nie ganz sperren; das 3:1-Verhältnis bremst nur.
- Die Spawn-Häufigkeit der Biome (Tonhügel, Dschungel selten) sollte in die Fundraten einfließen.

## Reihenfolge der Umsetzung (Vorschlag)

1. Erstfund garantiert, 2. Schlechtes-Glück-Schutz, 3. Verkauf loser Manuskripte, danach Simulation der Funde bis zur ersten fertigen Geschichte.
