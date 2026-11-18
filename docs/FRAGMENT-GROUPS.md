# Manuskript-Gruppen und Fundbiome

Erzeugt von `tools/group_manuscripts.py` (nicht von Hand ändern). Maschinenlesbar: `tools/data/fragment-groups.json`.

**1528 Manuskripte**, drei Schwierigkeitsstufen (leicht, mittel, schwer) zu je etwa 509, jede Stufe in 7 Untergruppen (zusammenhängende Textbereiche), jede Untergruppe in genau einem Biom. Jedes Biom hat damit **drei** Untergruppen, je eine pro Stufe, und zwar aus verschiedenen Textbereichen. Die erste Untergruppe jeder Stufe, der Anfang des Evangeliums, liegt in der Wüste.

## Nach Biom

| Biom | Leicht | Mittel | Schwer | Manuskripte |
|---|---|---|---|---|
| Wüste | **L1**: 1:1a bis 4:33a (72) | **M1**: 1:9 bis 4:21b (72) | **S1**: 1:3 bis 3:30 (72) | 216 |
| Ebene | **L2**: 4:34a bis 6:63b (73) | **M6**: 14:12a bis 18:7a (73) | **S4**: 9:3a bis 11:52 (72) | 218 |
| Wald | **L3**: 6:64a bis 8:44c (73) | **M7**: 18:8a bis 21:25a (73) | **S5**: 11:53 bis 15:25 (73) | 219 |
| Taiga | **L4**: 8:45 bis 11:32c (73) | **M2**: 4:23c bis 6:64b (73) | **S6**: 16:1 bis 19:20c (73) | 219 |
| Dschungel | **L5**: 11:36 bis 14:10a (73) | **M3**: 6:69-70 bis 8:54a (73) | **S7**: 19:23a bis 21:25b (73) | 219 |
| Savanne | **L6**: 14:10b bis 17:20 (73) | **M4**: 8:56 bis 11:14-15 (72) | **S2**: 3:32 bis 6:15 (73) | 218 |
| Sumpf | **L7**: 17:21a bis 21:24 (73) | **M5**: 11:16 bis 14:9c (73) | **S3**: 6:17a bis 9:2b (73) | 219 |

## Alle Untergruppen

| Gruppe | Stufe | Biom | Textbereich | Manuskripte | Wörter |
|---|---|---|---|---|---|
| L1 | Leicht | Wüste | 1:1a bis 4:33a | 72 | 587 |
| L2 | Leicht | Ebene | 4:34a bis 6:63b | 73 | 653 |
| L3 | Leicht | Wald | 6:64a bis 8:44c | 73 | 591 |
| L4 | Leicht | Taiga | 8:45 bis 11:32c | 73 | 656 |
| L5 | Leicht | Dschungel | 11:36 bis 14:10a | 73 | 632 |
| L6 | Leicht | Savanne | 14:10b bis 17:20 | 73 | 702 |
| L7 | Leicht | Sumpf | 17:21a bis 21:24 | 73 | 615 |
| M1 | Mittel | Wüste | 1:9 bis 4:21b | 72 | 757 |
| M2 | Mittel | Taiga | 4:23c bis 6:64b | 73 | 844 |
| M3 | Mittel | Dschungel | 6:69-70 bis 8:54a | 73 | 804 |
| M4 | Mittel | Savanne | 8:56 bis 11:14-15 | 72 | 712 |
| M5 | Mittel | Sumpf | 11:16 bis 14:9c | 73 | 744 |
| M6 | Mittel | Ebene | 14:12a bis 18:7a | 73 | 819 |
| M7 | Mittel | Wald | 18:8a bis 21:25a | 73 | 775 |
| S1 | Schwer | Wüste | 1:3 bis 3:30 | 72 | 807 |
| S2 | Schwer | Savanne | 3:32 bis 6:15 | 73 | 844 |
| S3 | Schwer | Sumpf | 6:17a bis 9:2b | 73 | 778 |
| S4 | Schwer | Ebene | 9:3a bis 11:52 | 72 | 799 |
| S5 | Schwer | Wald | 11:53 bis 15:25 | 73 | 802 |
| S6 | Schwer | Taiga | 16:1 bis 19:20c | 73 | 857 |
| S7 | Schwer | Dschungel | 19:23a bis 21:25b | 73 | 855 |

## So funktioniert die Einteilung

- **Schwierigkeit** pro Manuskript: Seltenheit der Wörter, Wortlänge und Formenschwierigkeit, gemittelt, dazu ein kleiner Zuschlag für die Länge. Gemeinsame Häufigkeitsbasis über alle Manuskripte. Das ist eine automatische Schätzung, keine geprüfte Einstufung.
- **Untergruppen** sind zusammenhängende Textbereiche *innerhalb* einer Stufe. Wer ein Biom ausräumt, bekommt also zusammenhängende Passagen in einer Schwierigkeit.
- **Pilot:** P1–P21 sind nach der einheitlichen Rechnung eingestuft. Die Stufe in `manuscripts.john1.json` stammt aus der alten Rechnung (nur gegen sich selbst) und weicht bei einigen ab.
- **Biome ändern:** `BIOMES` oben im Skript (Reihenfolge = Textreihenfolge der Untergruppen, Wüste zuerst) und neu laufen lassen.
