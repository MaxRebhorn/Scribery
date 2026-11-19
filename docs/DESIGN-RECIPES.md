# Rezepte der Werkbloecke und Bauteile

Alle Rezepte nutzen Vanilla-Zutaten oder Fundstuecke (Scherben, Papyrusfetzen). Es gibt bewusst keine eigenen Bauteil-Items
ausser Mini-Amboss und Giessform. Die Rezeptbilder: `art/gen_recipe_preview.py` -> `art/out/recipes-preview.png`.

| Ergebnis | Muster | Zutaten |
|---|---|---|
| Papier | formlos | 3 Papyrusfetzen |
| Mini-Amboss | `III` / ` n ` / `nnn` | I Eisenbarren, n Eisennugget (Vanilla-Ambossmuster, eine Stufe kleiner) |
| Giessform | ` s ` / `s s` / ` s ` | s Amphorenscherbe (Plus mit leerer Mitte) |
| Minting Table | ` p ` / `mfa` | p Dekotopf (Tiegel), m Giessform, f Ofen, a Mini-Amboss |
| Scribe's Lectern | ` C ` / `SpS` / ` P ` | C Kerze (Tag), S Holzstufe (Tag), p Papier, P Holzbretter (Tag) |
| Printing Press | `III` / `PXP` / `PPP` | I Eisenbarren, P Holzbretter, X Kolben |
| Letter Cabinet | ` B ` / `PPP` / `T T` | B Fass, P Holzbretter, T Stock |
| Translator's Desk | `ifp` / `PPP` / `P P` | i Tintenbeutel, f Feder, p Papier, P Holzbretter |
| Manuscript Archive | `PPP` / `ppp` / `PPP` | P Holzbretter, p Papier |

Kollisionen: Oellampe (Scherben im Eimermuster) und Amphoren-Lager (8 Scherben im Ring) bleiben unberuehrt; die Giessform
(Plus) kollidiert nicht mit dem Vanilla-Dekotopf, weil Amphorenscherben weder Ziegel noch Topfscherben sind.
Offen: Rezept fuer das eigene Buecherregal (`bookshelf`), Briefkasten-Zutat am Cabinet, Kolben im Press (teuer).
