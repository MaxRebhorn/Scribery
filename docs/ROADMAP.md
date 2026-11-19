# Scribery & Crafts – Roadmap

Reihenfolge der nächsten Schritte. Oben steht, was als Nächstes kommt. Der Stand bezieht sich auf Branch `phase-1-press`, abgeglichen mit dem Code am 19.11.2026.
Das Design steht in `Scribery_and_Crafts_Game_Design_Document.md`, die UI-Regeln in `UI-REQUIREMENTS.md` und `docs/ui-spec.md`, die Stationen in `docs/STATIONS.md`.

## Stand heute

Fertig:
- **Items:** 22 Letter Types, Manuscript Fragment (ein Item, Datenfeld mit Manuskript-ID P1–P1540), 7 Bücher (Alphabet Primer, Grammar Book und Complete Greek Lexicon lesbar, die übrigen ohne Inhalt), Printed Sheet, Translated Sheet, Wooden Word Tablet.
- **Blöcke:** Scribe's Lectern, Printing Press und Letter Cabinet (gemeinsame Druck-Oberfläche, zeigt nur, was angrenzt), Minting Table (Stonecutter-Menü, Eisennuggets → Letter Types), Translator's Desk (eigener Block mit Vorräten, übersetzt den aufgelegten bedruckten Bogen).
- **Server:** Vorräte liegen in Block-Entities, der Client meldet nur Verbrauch, der Server prüft jede Meldung.
- **Texte:** das ganze Johannesevangelium als 1.540 Manuskripte (P1–P21 Pilot, P22–P1540 Rest), erzeugt von `tools/build_manuscripts.py`, beide Dateien werden vom Mod geladen und von `ManuscriptDataTest` geprüft. Alle Glossen und falschen Antworten sind noch `draft`. Das 17-Buchstaben-Wort passt (17 Slots).
- **Eingabe:** Presse manuell (Lettern ziehen), Übersetzung mit Plättchen oder von Hand getippt; Buttons zum Nachfüllen aus dem Inventar.
- **Optik:** Tischplatte, Minecraft-Texturen, unsere Letter-Items, ausgefranstes Papier in Textgröße.

- **Varianten und Fundblöcke (Texturen und Testbarkeit):** Fragment-Varianten verkohlt, wassergeschädigt, verblasst, gefroren (blau getöntes Papyrus; der Codex-Look entfällt), dazu Block für gefrorenes Fragment, Amphore als Nicht-Würfel-Block, Ostrakon, Scherbenhaufen und die drei Fundböden Suspicious Mud, Suspicious Sand und Suspicious Ashes (je 4 Stufen). Welcher Block welche Variante liefert, steht in `docs/DESIGN-FINDS.md`. Kompiliert, Tests grün; im Spiel erst per Selbsttest angesehen.
- **Manuscript Archive:** Block, Dienst (`ArchiveService`), Sammlung pro Spieler (`ArchiveData`, bleibt nach dem Tod) und Screen mit drei Spalten (Urtext, Umschrift, Interlinear), gekoppelt scrollend, Lücken als Platzhalter. Kompiliert und im Selbsttest angesehen, nicht in einer Welt getestet (`docs/DESIGN-ARCHIVE.md`).
- **Funde und Weltgenerierung:** Loot-Funktion `scribery:random_manuscript` mit Pools je Fundart, Loot-Tabellen, Strukturen (Wrack, Karawane, Wanderer, Grab, Katakomben, verschüttete Häuser) und Funde an der Oberfläche; Katakomben ohne Monster. Mit dem Testserver geprüft (außer dem Wrack, das tiefes Wasser braucht), siehe `docs/DESIGN-WORLDGEN.md`.
- **Daten:** Gliederung in 64 Geschichten, 143 Seiten, 878 Verse, Fundbiome thematisch (11 Biome). Alles noch `draft`.

Noch Platzhalter: Bookshelf, die übrigen Bücher (nur Items), die meisten Rezepte. Noch nicht gebaut: Fundhilfen (Erstfund, Schlechtes-Glück-Schutz), Verkauf von Dubletten, Scribe Villager, Übersetzen direkt am Manuskript.

## Als Nächstes, in dieser Reihenfolge

### 1. Übersetzungstisch direkt mit dem Manuskript benutzen
- Der Tisch soll auch ein **Manuskript** annehmen, nicht nur einen bedruckten Bogen. Wer das Griechische schon so lesen kann, überspringt die Presse und übersetzt direkt vom griechischen Original.
- Das ist bewusst schwerer: Es gibt keine lateinische Umschrift als Hilfe, die Wörter erscheinen nur griechisch. Als Belohnung spart man Presse, Lettern, Tinte und Papier für den Druck.
- Zu klären: Gibt es einen Unterschied beim Ergebnis (zum Beispiel kein Printed Sheet, aber dasselbe Translated Sheet)? Zählt es gleich für den Fortschritt? Kosten (Tablets, Tinte, Papier) gleich oder anders?
- Technik: Der Tisch kennt dann zwei Eingaben, `PRINTED_SHEET` und `MANUSCRIPT_FRAGMENT`. In `StationNetwork` und `docs/STATIONS.md` die Regel ergänzen. Die Server-Prüfung der Übersetzung nutzt die Wörter des Fragments statt der gedruckten Zeilen.

### 2. Das restliche Johannesevangelium übernehmen
- Alle 21 Kapitel (879 Verse) durch `tools/build_manuscripts.py` erzeugen, nicht nur Kapitel 1. Das Skript ist schon so gebaut, dass es mit einem größeren Pool bessere falsche Antworten findet.
- Rohdaten holen (Abfrage des Bibel-Zugangs in 50-Verse-Schritten oder eine Datei der STEPBible-Daten, das Herunterladen nur mit Okay). Lizenz und Namensnennung vorher klären.
- **Stand 19.11.2026:** Rohdaten Joh 1,19–21,25 aus STEPBible TAGNT gezogen (`tools/data/john-1-19-21-25.tsv`, Lizenz CC BY 4.0, Quelle und Pflichten in `docs/CREDITS.md`), Manuskripte P22 bis P1540 erzeugt (`python tools/build_manuscripts.py --rest` -> `tools/data/manuscripts.john-rest.json`, 1519 Manuskripte, 15 380 Wörter). P1–P21 sind eingefroren und unverändert. **Im Spiel eingebunden** (`mod/build.gradle`, `Manuscripts`, `ManuscriptDataTest`). Alle Glossen `draft`.
- **Fundbiome (thematisch):** jedes Manuskript hat ein Biom, vergeben nach Geschichte (Ort, Bild, Stimmung), bei Bedarf einzelne Seiten (`docs/FRAGMENT-BIOMES.md`, Daten in `tools/data/fragment-biomes.json`, Zuordnung von Hand pflegbar in `tools/data/john-biomes.tsv`, erzeugt von `tools/assign_biomes.py`). 11 Biome mit 116 bis 166 Manuskripten. Die Schwierigkeit bleibt als Eigenschaft erhalten, bestimmt aber nicht mehr den Fundort. Das alte Zufallsfeld `origin.biome` gilt nicht mehr. Biom-Tags (z. B. `#c:is_desert`) sind noch nicht gegen 1.21.1 geprüft.
- **Gliederung für das Manuscript Archive:** 64 Geschichten, 143 Seiten, 878 Verse (`docs/JOHN-STRUCTURE.md`, Daten `tools/data/john-structure.json`, erzeugt von `tools/structure_john.py`). Die Manuskripte werden an Seitengrenzen geschnitten, damit Manuskript < Seite < Geschichte gilt. Geschichten-Grenzen und Titel (`tools/data/john-stories.tsv`) sind ein Entwurf.
- Offen: Textgrundlage (NA28 oder Tyndale), Perikope Joh 7,53–8,11, das 17-Buchstaben-Wort μεθερμηνευόμενον, Prüfung der Glossen und der falschen Antworten.
- Danach: Glossen stichprobenartig prüfen, die Manuskript-Nummer auf feste IDs umstellen (sonst verschieben sich bestehende Bögen), den englischen Satz je Manuskript aus der BSB nehmen.
- Schwierigkeit: 3 Stufen als Eigenschaft jedes Manuskripts; der Fundort richtet sich nicht danach, sondern ist thematisch pro Geschichte vergeben (siehe oben).

### 3. Bücher implementieren
- **Erledigt:** Greek Alphabet Primer. Der Text liegt als Markdown unter `mod/src/main/resources/assets/scribery/books/<id>.md`, die Leseansicht (`BookScreen`, `BookText`) zeigt eine Doppelseite mit der Minecraft-Buchtextur; unterstützt werden Überschriften, Absätze, Listen, fett/kursiv und Tabellen. Jedes weitere Buch braucht nur seine Textdatei (Dateiname = Item-ID).
- Greek Alphabet Primer, Grammar Book und die Lexika werden lesbar, mit echtem Inhalt (Alphabet, Aussprache, Deklinationen, Wortlisten).
- Bücher lassen sich nicht in den Arbeits-UIs öffnen: Man muss das UI verlassen, nachschlagen und zurückkehren.
- Bookshelf als Block zum Lagern und Ordnen.
- Zu klären: Lexikon-Inhalte pro Buch, Überschneidungen, Form der Anzeige (Seiten, Suche?).

- **Überarbeiten:** Beide fertigen Bücher (Alphabet Primer, Grammar Book) kommen noch einmal durch: Text, Aufteilung in Seiten und Länge.
- **Leichter benutzbar machen:** Das Nachschlagen soll nicht nervig sein. Die Regel „Arbeits-UI verlassen, Buch öffnen“ gilt weiter, aber der Weg dorthin ist zu umständlich. Ideen: das Buch merkt sich die letzte Seite, ein Tastenkürzel öffnet das zuletzt gelesene Buch (und danach geht es zurück an die Station), Lesezeichen, schnellerer Zugriff aus der Hotbar. Entscheiden, wie viel Reibung gewollt ist.

### 4. Händler, die die Bücher verkaufen
- Villager verkaufen die Bücher für Emeralds; ein günstiges Wörterbuch zu Beginn.
- Offen: Ein **eigener Villager-Beruf** (mit Arbeitsblock, zum Beispiel dem Bookshelf oder dem Pult) oder ein bestehender Beruf (Bibliothekar) mit zusätzlichen Angeboten? Ein eigener Beruf ist sauberer, aber mehr Aufwand (Beruf, Arbeitsblock, Handelsliste, Texturen).
- **Scribe Villager (Schreiber):** Der Händler für die Bücher bekommt einen Namen und eine Rolle: ein eigener Villager-Beruf „Scribe“.
  - Arbeitsblock: das Pult (Scribe Lectern) oder der Bookshelf, noch zu entscheiden.
  - Handel: verkauft Alphabet Primer, Grammar Book und die Lexika, kauft Manuskript-Dubletten, verkauft Fundkarten und später Restaurierungsaufträge (siehe unten und `docs/DESIGN-FINDS.md`).
  - Vorkommen: lebt in normalen Dörfern; vielleicht ein eigenes kleines Schreibhaus im Dorf.
  - Aufwand: Beruf, Arbeitsblock, Handelsliste, Texturen und Übersetzungen. Noch nicht begonnen.
- Preise und die Reihenfolge, in der Bücher auftauchen, werden später balanciert.
- Derselbe Händler (oder ein Gelehrter) kauft lose Manuskripte-Dubletten und verkauft Fundkarten, siehe `docs/DESIGN-FINDS.md`.

### 5. Wie Fragmente in die Welt kommen
- Verschiedene Arten, wie ein Fragment entsteht oder gefunden wird: Loot in Truhen und Strukturen, verdächtiger Sand und Kies, Fischen, Händler, Funde bei Villagern. Genau das ist noch zu entscheiden.
- Die Manuskript-Nummer ist die Position im Evangelium, die Schwierigkeit läuft getrennt. Jedes Biom liefert ganze Geschichten oder Seiten und enthält leichte, mittlere und schwere Stücke.
- **Entwurf Fundhilfen und Dubletten:** `docs/DESIGN-FINDS.md`. Kern: mehrere Fundquellen (alle 10 bis 15 Minuten ein Fund), Erstfund garantiert, Schlechtes-Glück-Schutz (rund 70 % noch fehlende Manuskripte), Verkauf **loser** Manuskripte (nicht im Archiv) für 1 bis 3 Smaragde nach Stufe mit Tageslimit, Tausch 3 Dubletten gegen ein Manuskript des gewählten Bioms, Wegweiser-Karten. Rechnung dahinter: rund 770 Zufallsfunde für ein ganzes Biom, daher Schutz und Verkauf nötig. Zahlen sind ungetestet.
- Abhängigkeit: der Schutz braucht die gespeicherte Sammlung, also das Manuscript Archive. Biom-Tags (`#c:is_desert` und andere) sind noch gegen 1.21.1 zu prüfen; seltene Biome (Tonhügel, Dschungel) brauchen höhere Fundraten.
- Die ersten Manuskripte sind kurz und einfach, spätere länger. Bereits gefundene Fragmente sollen nicht ständig doppelt auftauchen (Zufallsgewichtung oder Hinweise).

### 5a. Strukturen und Fundblöcke (zu Punkt 5)
Orte und Blöcke, an denen man Fragmente und Hinweise findet. Das ist die Idee, noch nicht entschieden.
- **Verlassene Strukturen:** aufgegebene Außenposten, Lager und Ruinen; auch **vergrabene** Funde (verdächtiger Sand und Kies, Truhen unter der Erde).
- **Versunkene griechische Schiffe:** ein Schiffswrack mit Loot unter Wasser, die Ladung variiert von fast leer bis voller Amphoren (Test: `/scribery_wreck <1-10>`, 1 = eine Amphore, 10 = komplett voll). Später nicht nur auf dem Grund von Ozeanen und großen Seen, sondern auch in **gefluteten Höhlen**. Dafür bräuchte die Welt-Erzeugung ein Feature, das den Boden unter Wasser in der Höhle findet (Platzierung per Umgebungsscan nach unten bis zum festen Boden mit Wasser darüber), statt der Meeresboden-Höhenkarte. Das kleine Wrack aus `DebugWreck` wäre dafür die Vorlage.
- **Ausgetrocknete Schiffe in der Wüste:** dasselbe Schiff als Wrack im Sand, dazu weitere Funde in der Wüste.
- **Amphoren-Blöcke:**
  - ein Block für **Scherben von Amphoren** (Potsherds), der Scherben und Text-Funde fallen lässt,
  - ein **Amphorenblock** als Behälter (Container), der Inhalt droppt: **beschriftete Scherben** (mit einem kurzen Text, einem Hinweis oder einem Wort, das zum Manuskript passen könnte) und **Manuskripte**.
- **Stand:** Texturen und Blöcke für Amphorenscherben (Bleich-Palette), Amphore als Container, Ostrakon und verdächtigen Archivschlamm gibt es (siehe oben); Fund-Logik (`random_manuscript`), Loot-Tabellen und Strukturen gibt es inzwischen (siehe oben und `docs/DESIGN-FINDS.md`, `docs/DESIGN-WORLDGEN.md`); offen bleiben Dubletten-Schutz, Ostraka mit Witzen als eigene Items und das Wrack im echten Tiefwasser.
- Zu klären: Welche Texte auf den Scherben stehen (Wörter aus den Manuskripten? Namen? Hinweise auf Fundorte?), ob Scherben im Archiv eine Rolle spielen, wie die Strukturen erzeugt werden (Strukturdatei, Loot-Tabellen nach Biom), und wie sich Fundort und Aussehen des Fragments verbinden (siehe Punkt 6).

### 6. Ferne Zukunft: unterschiedliches Aussehen je nach Fundort
- **Teilweise erledigt:** vier Zustandsvarianten (verkohlt, wassergeschädigt, verblasst, gefroren) liegen als Texturen vor (`custom_model_data` 1–4, Codex entfällt). Das Aussehen bestimmt der Fundblock (Sand, Schlamm, Asche, Eis, siehe `docs/DESIGN-FINDS.md`). Offen ist, Biom und Block zu verbinden. Das alte Feld `origin.biome` ist obsolet, maßgeblich ist `tools/data/fragment-biomes.json`.
- Fragmente sehen verschieden aus, je nachdem, wo man sie findet: Sumpf, Wüste, Taiga und so weiter, mit eigenen Texturen. Das Datenfeld `origin.biome` ist dafür schon in den Manuskriptdaten vorbereitet.
- Technisch später: Item-Modell-Überschreibungen nach Datenfeld, mehrere Texturen (vier ungenutzte liegen schon im Repo).

## Weitere Schritte, danach

### Rezepte und Fortschritt
- Crafting-Rezepte für alle Blöcke und Hilfsmittel (bisher nur Wooden Word Tablets).
- Der Fortschritt im Archiv ist gespeichert (`ArchiveData`); offen bleibt, dass eine halb fertige Übersetzung am Tisch beim Schließen verloren geht.

### Manuscript Archive
- Screen mit drei Spalten: links die Manuskripte (griechischer Urtext) in der Reihenfolge des Evangeliums zusammenfügen, in der Mitte die lateinische Umschrift (Printed Sheet), rechts die englische Interlinear-Übersetzung (Translated Sheet). Am Ende steht eine durchgehende Interlinear-Ausgabe. Entwurf: `docs/DESIGN-ARCHIVE.md`.
- **Umgesetzt** (Block, Sammlung pro Spieler, Screen; Branch `archive-screen`), nicht in einer Welt getestet.
- Entschieden: kein Puzzle, Manuskripte rasten automatisch ein; das Evangelium erscheint als ein großer Text, Lücken zeigen, was fehlt und wie weit man ist.
- Entschieden: Lücken als grauer Platzhalter in der Länge des fehlenden Textes; die drei Spalten scrollen gekoppelt.
- Zu klären (im Code gilt vorläufig: jede Spalte füllt sich unabhängig): Füllreihenfolge der Spalten und ob Heften noch ein eigener Schritt bleibt (siehe Entwurf).

### Restaurierungsaufträge, Achievements
- Villager bieten beschädigte Manuskripte an (Buchstabe oder Endung rekonstruieren), Belohnung in Emeralds. Optional.
- Achievements: gefundene, transkribierte, übersetzte Manuskripte, Vollständigkeit des Evangeliums.

### Eingabe ausbauen
- Griechische Eingabe mit Akzenten und Spiritus (für Restaurierung und Formenübungen). Eingabeprüfung tolerant: Akzente ignorieren, Buchstaben und Endungen prüfen. Normalisierung (NFC/NFD) beim Vergleichen.

### Politur und Optik (ganz am Ende)
- Texturen und Modelle: eigene Textur für den Minting Table, Modelle statt Würfel (zum Beispiel zweiteilige Presse), Pult-Look, eigene Texturen für Printed/Translated Sheet.
- Optik der Einzelteile wie Kurbel, Hebel und Tintenfass.
- Kontroll-Mockups in `art/out/` neu erzeugen.

## Bekannte Schwächen

- Printed Sheets sind nicht gegen die echte Setzform geprüft, ein Client könnte Papier gegen beliebigen Text eintauschen.
- Die Manuskript-Nummer ist der Index in der Liste (john1, dann john-rest). Das Archiv speichert nach diesem Index, die IDs (P1–P1540) sind stabil, aber wird neu erzeugt oder umsortiert, verschieben sich gespeicherte Sammlungen. Vor dem ersten echten Spielstand auf IDs umstellen.
- Eine halb fertige Übersetzung geht beim Schließen verloren.
- Der Selbsttest nutzt Platzhalter, die echten Daten laden dort nur über einen eigenen Schritt (`Manuscripts.useReal`).
- Neue Fundquellen, Erstfund, Schlechtes-Glück-Schutz und Dubletten-Verkauf sind nur entworfen (`docs/DESIGN-FINDS.md`), nicht gebaut. Die Biom-Tags (`#c:is_desert` und andere) sind nicht gegen 1.21.1 geprüft.

## Offene Designentscheidungen

- Wie der Archiv-Screen mit den drei Spalten im UI funktioniert (drei offene Fragen in `docs/DESIGN-ARCHIVE.md`).
- Wie genau Übersetzen direkt vom Manuskript (ohne Druck) belohnt oder bewertet wird.
- Scribe Villager: Arbeitsblock (Pult oder Bookshelf) und ob es ein eigenes Schreibhaus im Dorf gibt.
- Wo und wie Fragmente gefunden werden (Spawn-Arten, Strukturen, Amphoren und Scherben).
- Wie viel Reibung das Nachschlagen in den Büchern haben soll, ohne dass es nervt.
- Welche Bibelstellen welche sprachlichen Anforderungen haben.
- Wie viele Restaurierungsaufträge ein Villager gleichzeitig anbietet.
- Bücherpreise und Lexikon-Inhalte.
- Ressourcenbalance: Verbrauch von Tinte, Papier und Tablets, Verschleiß der Letter Types, Ausbeute am Minting Table (derzeit 1 Nugget → 2 Letter Types).
- Ob der Minting Table auch Eisenbarren annimmt (Vorschlag: 1 Barren = 9 Nuggets).
