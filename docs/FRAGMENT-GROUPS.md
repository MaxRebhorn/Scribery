# Manuskript-Gruppen und Fundbiome

Erzeugt von `tools/group_manuscripts.py` (nicht von Hand ändern). Maschinenlesbar: `tools/data/fragment-groups.json`.

**1540 Manuskripte**, drei Schwierigkeitsstufen (leicht, mittel, schwer) zu je etwa 513, jede Stufe in 7 Untergruppen (zusammenhängende Textbereiche), jede Untergruppe in genau einem Biom. Jedes Biom hat damit **drei** Untergruppen, je eine pro Stufe, und zwar aus verschiedenen Textbereichen. Die erste Untergruppe jeder Stufe, der Anfang des Evangeliums, liegt in der Wüste.

## Nach Biom

| Biom | Leicht | Mittel | Schwer | Manuskripte |
|---|---|---|---|---|
| Wüste | **L1**: 1:1a bis 4:40b (73) | **M1**: 1:9 bis 4:24 (73) | **S1**: 1:3 bis 3:26b (73) | 219 |
| Ebene | **L2**: 4:42a bis 6:63b (73) | **M6**: 14:2a bis 18:6 (73) | **S4**: 9:3a bis 11:54b (74) | 220 |
| Wald | **L3**: 6:64a bis 8:45 (74) | **M7**: 18:7a bis 21:25a (74) | **S5**: 11:55 bis 16:2 (73) | 221 |
| Taiga | **L4**: 8:46b bis 11:32a (73) | **M2**: 4:27a bis 6:60b (73) | **S6**: 16:4a bis 19:22 (73) | 219 |
| Dschungel | **L5**: 11:32c bis 14:7b (74) | **M3**: 6:64b bis 8:52b (73) | **S7**: 19:23a bis 21:25b (74) | 221 |
| Savanne | **L6**: 14:10a bis 17:17 (73) | **M4**: 8:52d bis 11:14 (74) | **S2**: 3:30 bis 6:15 (73) | 220 |
| Sumpf | **L7**: 17:20 bis 21:24 (74) | **M5**: 11:14-15 bis 13:38a (73) | **S3**: 6:17a bis 9:2b (73) | 220 |

## Alle Untergruppen

| Gruppe | Stufe | Biom | Textbereich | Manuskripte | Wörter |
|---|---|---|---|---|---|
| L1 | Leicht | Wüste | 1:1a bis 4:40b | 73 | 589 |
| L2 | Leicht | Ebene | 4:42a bis 6:63b | 73 | 656 |
| L3 | Leicht | Wald | 6:64a bis 8:45 | 74 | 600 |
| L4 | Leicht | Taiga | 8:46b bis 11:32a | 73 | 646 |
| L5 | Leicht | Dschungel | 11:32c bis 14:7b | 74 | 617 |
| L6 | Leicht | Savanne | 14:10a bis 17:17 | 73 | 698 |
| L7 | Leicht | Sumpf | 17:20 bis 21:24 | 74 | 629 |
| M1 | Mittel | Wüste | 1:9 bis 4:24 | 73 | 759 |
| M2 | Mittel | Taiga | 4:27a bis 6:60b | 73 | 828 |
| M3 | Mittel | Dschungel | 6:64b bis 8:52b | 73 | 802 |
| M4 | Mittel | Savanne | 8:52d bis 11:14 | 74 | 716 |
| M5 | Mittel | Sumpf | 11:14-15 bis 13:38a | 73 | 749 |
| M6 | Mittel | Ebene | 14:2a bis 18:6 | 73 | 815 |
| M7 | Mittel | Wald | 18:7a bis 21:25a | 74 | 769 |
| S1 | Schwer | Wüste | 1:3 bis 3:26b | 73 | 828 |
| S2 | Schwer | Savanne | 3:30 bis 6:15 | 73 | 823 |
| S3 | Schwer | Sumpf | 6:17a bis 9:2b | 73 | 765 |
| S4 | Schwer | Ebene | 9:3a bis 11:54b | 74 | 827 |
| S5 | Schwer | Wald | 11:55 bis 16:2 | 73 | 798 |
| S6 | Schwer | Taiga | 16:4a bis 19:22 | 73 | 857 |
| S7 | Schwer | Dschungel | 19:23a bis 21:25b | 74 | 862 |

## So funktioniert die Einteilung

- **Schwierigkeit** pro Manuskript: Seltenheit der Wörter, Wortlänge und Formenschwierigkeit, gemittelt, dazu ein kleiner Zuschlag für die Länge. Gemeinsame Häufigkeitsbasis über alle Manuskripte. Das ist eine automatische Schätzung, keine geprüfte Einstufung.
- **Untergruppen** sind zusammenhängende Textbereiche *innerhalb* einer Stufe. Wer ein Biom ausräumt, bekommt also zusammenhängende Passagen in einer Schwierigkeit.
- **Pilot:** P1–P21 sind nach der einheitlichen Rechnung eingestuft. Die Stufe in `manuscripts.john1.json` stammt aus der alten Rechnung (nur gegen sich selbst) und weicht bei einigen ab.
- **Biome ändern:** `BIOMES` oben im Skript (Reihenfolge = Textreihenfolge der Untergruppen, Wüste zuerst) und neu laufen lassen.
