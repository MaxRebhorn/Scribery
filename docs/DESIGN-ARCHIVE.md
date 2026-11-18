# Entwurf: Manuscript Archive mit drei Spalten

Stand 18.11.2026, Entwurf, noch nichts umgesetzt. Ersetzt die grobe Skizze „Puzzle zum Zusammensetzen der Fragmente“ in `ROADMAP.md`.

## Idee

Das Archiv ist ein Screen mit drei Spalten, die Wort für Wort nebeneinanderliegen, in griechischer Wortstellung:

| Links | Mitte | Rechts |
|---|---|---|
| **Griechischer Urtext** aus den Manuskripten | **Lateinische Umschrift** (aus dem Printed Sheet) | **Englische Interlinear-Übersetzung** (aus dem Translated Sheet) |

Am Ende steht eine durchgehende Interlinear-Ausgabe des Evangeliums, die vollständig aus den eigenen Funden, Abzügen und Übersetzungen entsteht.

## Ablauf

1. **Links einfügen, kein Puzzle:** Ein gefundenes Manuskript rastet **automatisch** an seiner festen Position ein. Falsch legen gibt es nicht. Gliederung: 64 Geschichten, 143 Seiten, 878 Verse (`JOHN-STRUCTURE.md`, `tools/data/john-structure.json`). Der Screen zeigt das Evangelium als **einen großen durchgehenden Text**. Was noch fehlt, erscheint als sichtbare Lücke, so sieht man auf einen Blick, wo etwas fehlt und wie weit man ist.
2. **Mitte und rechts einlegen:** Der passende Bogen (Printed Sheet, Translated Sheet) wird zu einem Abschnitt gelegt. Die Zuordnung läuft über die Manuskript-ID.
3. **Vollständigkeit:** Ein Vers ist vollständig, wenn alle seine Manuskripte vorliegen, eine Seite, wenn alle Verse vollständig sind, eine Geschichte, wenn alle Seiten vollständig sind. Pro Zeile gibt es vier Zustände: Urtext fehlt, nur Urtext, Urtext plus Umschrift, alles komplett.
4. **Folio:** Eine komplette Seite oder Geschichte lässt sich heften (Stufe Folio aus `plan.md`). Nur fehlerfreie Reinschriften lassen sich heften.

## Abhängigkeiten

- **Feste Manuskript-IDs** statt Listenindex (Branch `data-wiring`). Printed und Translated Sheet müssen die ID tragen.
- **Gespeicherte Sammlung pro Spieler** (Branch `collection-save`): pro Manuskript gefunden, gedruckt, übersetzt. Ohne sie ist der Fortschritt beim Schließen weg.
- Geplanter Branch: `archive-screen`, nach den beiden genannten.
- UI-Regeln aus `UI-REQUIREMENTS.md` und `docs/ui-spec.md`. Bei 878 Versen braucht der Screen Scrollen oder eine Auswahl nach Geschichte und Seite.

## Entschieden (18.11.2026)

- **Einrasten statt Puzzle:** Manuskripte rasten automatisch an ihrer Position ein. Es gibt kein Falschlegen und deshalb auch keine Strafe.
- **Ein großer Text mit Lücken:** Der Screen ist der durchgehende Text des Evangeliums. Fehlendes bleibt als Lücke sichtbar, der Fortschritt ist daran ablesbar (nach Vers, Seite und Geschichte gruppiert, mit Anzeige „x von y“).
- Damit ist der Screen zugleich die **Lesefassung**: Wer fertig ist, liest das Evangelium dort in Urtext, Umschrift und Interlinear.

- **Lücken als grauer Platzhalter** in der Länge des fehlenden Textes. So sieht man, wie viel fehlt.
- **Die drei Spalten scrollen gekoppelt:** Wer in einer Spalte scrollt, bewegt die anderen mit. Zeilen, Wörter und Lücken liegen immer auf gleicher Höhe nebeneinander.

## Umsetzung (Stand 18.11.2026, kompiliert und im Selbsttest angesehen, noch nicht in einer Welt getestet)

- **Eintragen am Archiv-Block:** Rechtsklick mit einem Fragment trägt den griechischen Urtext ein, mit einem gedruckten Bogen die Umschrift dieses Bogens, mit einem übersetzten Bogen die Übersetzung (er zählt auch als gedruckt). Der Gegenstand ist dann verbraucht; was das Archiv schon hat, bleibt als Duplikat beim Spieler. Mit leerer Hand öffnet der Block den Text.
- **Sammlung pro Spieler** (`ArchiveData`, Spielerdaten, bleibt nach dem Tod): pro Manuskript ein Fortschritt (gefunden, gedruckte Bögen, übersetzte Bögen als Bits). Der Server schickt sie beim Anmelden, beim Respawn und nach jeder Änderung zum Client (`SyncArchive`).
- **Bögen:** Welches Wort zu welchem Bogen gehört, folgt derselben Zeilenteilung wie in der Presse (`ArchiveModel.pageOfWord`, 17 Slots, 4 Zeilen, höchstens 3 Wörter pro Zeile). Insgesamt 2.205 Bögen für 1.540 Manuskripte.
- **Screen:** ein durchgehender Text in drei Spalten (Urtext, Umschrift, Interlinear), jede Zeile zeigt in allen Spalten dieselben Wörter, ein Scrollen bewegt alle gemeinsam. Was fehlt, ist ein grauer Platzhalter in der Länge des fehlenden Textes. Kapitelköpfe mit Geschichte und Versbereich, links der Vers, oben Statistik mit Balken, unten 21 Kapitel-Knöpfe (grün je nach Fortschritt), rechts eine Übersichtsleiste des ganzen Textes zum Anklicken und Ziehen. Tasten: Bild hoch/runter, Pfeile, Pos1/Ende, Mausrad.
- **Entschieden:** jede Spalte füllt sich für sich (Urtext, Umschrift und Übersetzung hängen nicht voneinander ab), damit man auch Bögen eintragen kann, deren Fragment als Duplikat schon verbraucht ist.

## Noch offen

1. **Füllreihenfolge:** Im Code gilt jetzt, **ohne dass du es bestätigt hast**: Jede Spalte füllt sich unabhängig, auch ein Bogen lässt sich eintragen, wenn das Fragment nicht im Archiv liegt (es steckt ja nur als Duplikat bei dir oder ist verbraucht). Die Alternative wäre, Bögen nur dort anzunehmen, wo das Fragment schon eingetragen ist (passt zu Fragment → Abzug → Reinschrift, ist aber strenger).
2. **Folio:** Ist Heften noch ein eigener Schritt, oder reicht „alles drin“?
3. **Technik:** Gleiche Höhe heißt, dass eine Zeile so hoch ist wie die höchste der drei Spalten. Bei Lücken im Urtext braucht die Umschrift dieselbe Platzhalterlänge.
