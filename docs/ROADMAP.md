# Scribery & Crafts – Roadmap

Reihenfolge der nächsten Schritte. Oben steht, was als Nächstes kommt. Der Stand bezieht sich auf Branch `phase-1-press`.
Das Design steht in `Scribery_and_Crafts_Game_Design_Document.md`, die UI-Regeln in `UI-REQUIREMENTS.md` und `docs/ui-spec.md`, die Stationen in `docs/STATIONS.md`.

## Stand heute

Fertig:
- **Items:** 22 Letter Types, Manuscript Fragment (ein Item, Datenfeld mit Manuskript-ID P1–P21), 7 Bücher (ohne Inhalt), Printed Sheet, Translated Sheet, Wooden Word Tablet.
- **Blöcke:** Scribe's Lectern, Printing Press und Letter Cabinet (gemeinsame Druck-Oberfläche, zeigt nur, was angrenzt), Minting Table (Stonecutter-Menü, Eisennuggets → Letter Types), Translator's Desk (eigener Block mit Vorräten, übersetzt den aufgelegten bedruckten Bogen).
- **Server:** Vorräte liegen in Block-Entities, der Client meldet nur Verbrauch, der Server prüft jede Meldung.
- **Texte:** Pilot Johannes 1,1–18 als 21 Manuskripte, erzeugt von `tools/build_manuscripts.py`, geprüft von `ManuscriptDataTest`.
- **Eingabe:** Presse manuell (Lettern ziehen), Übersetzung mit Plättchen oder von Hand getippt; Buttons zum Nachfüllen aus dem Inventar.
- **Optik:** Tischplatte, Minecraft-Texturen, unsere Letter-Items, ausgefranstes Papier in Textgröße.

Noch Platzhalter: Bookshelf, Manuscript Archive, die Bücher (nur Items), die meisten Rezepte, die Fundorte der Fragmente (bisher nur der Creative-Tab).

## Als Nächstes, in dieser Reihenfolge

### 1. Übersetzungstisch direkt mit dem Manuskript benutzen
- Der Tisch soll auch ein **Manuskript** annehmen, nicht nur einen bedruckten Bogen. Wer das Griechische schon so lesen kann, überspringt die Presse und übersetzt direkt vom griechischen Original.
- Das ist bewusst schwerer: Es gibt keine lateinische Umschrift als Hilfe, die Wörter erscheinen nur griechisch. Als Belohnung spart man Presse, Lettern, Tinte und Papier für den Druck.
- Zu klären: Gibt es einen Unterschied beim Ergebnis (zum Beispiel kein Printed Sheet, aber dasselbe Translated Sheet)? Zählt es gleich für den Fortschritt? Kosten (Tablets, Tinte, Papier) gleich oder anders?
- Technik: Der Tisch kennt dann zwei Eingaben, `PRINTED_SHEET` und `MANUSCRIPT_FRAGMENT`. In `StationNetwork` und `docs/STATIONS.md` die Regel ergänzen. Die Server-Prüfung der Übersetzung nutzt die Wörter des Fragments statt der gedruckten Zeilen.

### 2. Das restliche Johannesevangelium übernehmen
- Alle 21 Kapitel (879 Verse) durch `tools/build_manuscripts.py` erzeugen, nicht nur Kapitel 1. Das Skript ist schon so gebaut, dass es mit einem größeren Pool bessere falsche Antworten findet.
- Rohdaten holen (Abfrage des Bibel-Zugangs in 50-Verse-Schritten oder eine Datei der STEPBible-Daten, das Herunterladen nur mit Okay). Lizenz und Namensnennung vorher klären.
- Danach: Glossen stichprobenartig prüfen, die Manuskript-Nummer auf feste IDs umstellen (sonst verschieben sich bestehende Bögen), den englischen Satz je Manuskript aus der BSB nehmen.
- Schwierigkeit bleibt in 3 Stufen, zufällig gemischt. Verteilung auf Biome als Mischung.

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
- Preise und die Reihenfolge, in der Bücher auftauchen, werden später balanciert.

### 5. Wie Fragmente in die Welt kommen
- Verschiedene Arten, wie ein Fragment entsteht oder gefunden wird: Loot in Truhen und Strukturen, verdächtiger Sand und Kies, Fischen, Händler, Funde bei Villagern. Genau das ist noch zu entscheiden.
- Die Manuskript-Nummer ist die Position im Evangelium, die Schwierigkeit läuft getrennt. Jedes Biom bekommt eine Mischung aus leichten, mittleren und schweren Stücken.
- Die ersten Manuskripte sind kurz und einfach, spätere länger. Bereits gefundene Fragmente sollen nicht ständig doppelt auftauchen (Zufallsgewichtung oder Hinweise).

### 5a. Strukturen und Fundblöcke (zu Punkt 5)
Orte und Blöcke, an denen man Fragmente und Hinweise findet. Das ist die Idee, noch nicht entschieden.
- **Verlassene Strukturen:** aufgegebene Außenposten, Lager und Ruinen; auch **vergrabene** Funde (verdächtiger Sand und Kies, Truhen unter der Erde).
- **Versunkene griechische Schiffe:** ein kleines Schiffswrack im Ozean, mit Loot unter Wasser.
- **Ausgetrocknete Schiffe in der Wüste:** dasselbe Schiff als Wrack im Sand, dazu weitere Funde in der Wüste.
- **Amphoren-Blöcke:**
  - ein Block für **Scherben von Amphoren** (Potsherds), der Scherben und Text-Funde fallen lässt,
  - ein **Amphorenblock** als Behälter (Container), der Inhalt droppt: **beschriftete Scherben** (mit einem kurzen Text, einem Hinweis oder einem Wort, das zum Manuskript passen könnte) und **Manuskripte**.
- Zu klären: Welche Texte auf den Scherben stehen (Wörter aus den Manuskripten? Namen? Hinweise auf Fundorte?), ob Scherben im Archiv eine Rolle spielen, wie die Strukturen erzeugt werden (Strukturdatei, Loot-Tabellen nach Biom), und wie sich Fundort und Aussehen des Fragments verbinden (siehe Punkt 6).

### 6. Ferne Zukunft: unterschiedliches Aussehen je nach Fundort
- Fragmente sehen verschieden aus, je nachdem, wo man sie findet: Sumpf, Wüste, Taiga und so weiter, mit eigenen Texturen. Das Datenfeld `origin.biome` ist dafür schon in den Manuskriptdaten vorbereitet.
- Technisch später: Item-Modell-Überschreibungen nach Datenfeld, mehrere Texturen (vier ungenutzte liegen schon im Repo).

## Weitere Schritte, danach

### Rezepte und Fortschritt
- Crafting-Rezepte für alle Blöcke und Hilfsmittel (bisher nur Wooden Word Tablets).
- Gespeicherter Fortschritt je Spieler (übersetzte Manuskripte); heute geht eine halb fertige Übersetzung beim Schließen verloren.

### Manuscript Archive
- Das Puzzle zum Zusammensetzen der Fragmente in die richtige Reihenfolge.
- Zu klären: Wie funktioniert das Verschieben im UI? Was gilt als „richtig“?

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
- Die Manuskript-Nummer ist der Index in der Liste: Wird neu erzeugt, verschieben sich bestehende Bögen. Vor dem ganzen Evangelium auf IDs umstellen.
- Eine halb fertige Übersetzung geht beim Schließen verloren.
- Der Selbsttest nutzt die Platzhalter, die echten Daten laden nur über einen eigenen Schritt (`Manuscripts.useReal`).

## Offene Designentscheidungen

- Wie der Archiv-Puzzle im UI funktioniert.
- Wie genau Übersetzen direkt vom Manuskript (ohne Druck) belohnt oder bewertet wird.
- Ob ein eigener Villager-Beruf entsteht.
- Wo und wie Fragmente gefunden werden (Spawn-Arten, Strukturen, Amphoren und Scherben).
- Wie viel Reibung das Nachschlagen in den Büchern haben soll, ohne dass es nervt.
- Welche Bibelstellen welche sprachlichen Anforderungen haben.
- Wie viele Restaurierungsaufträge ein Villager gleichzeitig anbietet.
- Bücherpreise und Lexikon-Inhalte.
- Ressourcenbalance: Verbrauch von Tinte, Papier und Tablets, Verschleiß der Letter Types, Ausbeute am Minting Table (derzeit 1 Nugget → 2 Letter Types).
- Ob der Minting Table auch Eisenbarren annimmt (Vorschlag: 1 Barren = 9 Nuggets).
