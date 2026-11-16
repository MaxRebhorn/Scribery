# Scribery & Crafts – Roadmap

Reihenfolge der nächsten Schritte. Oben steht, was als Nächstes kommt. Der Stand bezieht sich auf Branch `phase-1-press`.
Das Design steht in `Scribery_and_Crafts_Game_Design_Document.md`, die UI-Regeln in `UI-REQUIREMENTS.md` und `docs/ui-spec.md`.

## Stand heute

Fertig:
- Items: 22 Letter Types, 5 Manuskriptfragmente (Platzhalter), 7 Bücher (ohne Inhalt), Printed/Translated Sheet, Wooden Word Tablet.
- Blöcke: Scribe's Lectern (legt Fragment auf, öffnet Oberfläche), Printing Press und Letter Cabinet (gemeinsame Druck-Oberfläche, zeigt nur, was angrenzt, freier Druck ohne Fragment), Minting Table (Stonecutter-Menü, Eisennuggets → Letter Types).
- Der Letter Cabinet lagert Letter Types, Tintenbeutel und Papier. Die Presse bezieht und verbraucht ihre Vorräte, der Server prüft jede Meldung.
- Übersetzungstisch: eigener Block mit Vorräten, übersetzt den aufgelegten bedruckten Bogen.
- UI-Optik: Tischplatte, Minecraft-Texturen, unsere Letter-Items, ausgefranstes Papier in Textgröße.

Noch Platzhalter: Bookshelf, Manuscript Archive, die Bücher, Rezepte, die Texte (nur 3 Platzhaltertexte aus Johannes 1).

## Reihenfolge

### 1. Bedruckter Bogen als Item – erledigt
- Die Presse gibt pro fertigem Bogen ein Printed Sheet aus (Datenkomponente mit Fragment, Bogen, Zeilen). Der Server nimmt nur so viele Bögen an, wie Papier verbraucht wurde.
- Der Übersetzungstisch nimmt den Bogen (aufgelegt wie in einen Bilderrahmen) und gibt ein Translated Sheet heraus.
- Offen: Die Bögen sind nicht gegen die echte Setzform geprüft, ein Client könnte Papier gegen beliebigen Text eintauschen. Und beide Items nutzen noch Vanilla-Texturen (Papier, Buch).

### 2. Wooden Word Tablets – erledigt
- Item mit Rezept (6 Planks → 3 Tablets) und eigener Textur. Der Tisch lagert Tablets, Tinte und Papier; jedes geschriebene Wort verbraucht ein Tablet, auch ein falsches.
- Eine Übersetzung kostet zusätzlich ein Blatt Papier. Der Server prüft sie gegen die Glossen des Fragments.

### 3. Freie Eingabe von Hand – erledigt (nur am Übersetzungstisch)
- In der Übersetzung klickt man in eine Lücke unter einem Wort und tippt es selbst (Tab/Enter/Rücktaste). Tippfehler verwischen wie ein falsches Plättchen, jedes getippte Wort kostet ein Tablet. Tasten für Ē und Ō erscheinen nur beim Tippen, links neben dem Quill.
- Die Presse bleibt ausdrücklich manuell: Lettern werden gezogen, nicht getippt. Sie merkt sich die gesetzten Lettern (im Schrank gespeichert); Buttons füllen Letter Types bzw. Tinte und Papier aus dem Inventar nach.
- Ausbau offen: Griechische Eingabe mit Akzenten und Spiritus (für Restaurierung und Formenübungen). Eingabeprüfung tolerant: Akzente ignorieren, Buchstaben und Endungen prüfen. Normalisierung (NFC/NFD) beim Vergleichen.

### 4. Echte Texte und Manuskript-IDs – Pilot eingebunden (Johannes 1,1-18)
- `tools/build_manuscripts.py` erzeugt aus den Rohdaten (`tools/data/john-*.tsv`) die Manuskripte: Glossen, erlaubte Varianten, falsche Antworten (Kasus/Zahl bei Nomen, sonst gleiche Wortart), Schwierigkeit in 3 Stufen, zufälliges Biom. `ManuscriptDataTest` prüft, dass alles in die Setzform passt.
- Im Spiel: ein Fragment-Item mit Datenfeld (ID wie P7); der Creative-Tab listet alle. Server und Client laden dieselben Daten (`Manuscripts`). Der Selbsttest nutzt weiter die Platzhalter (Systemeigenschaft `scribery.selftest`).
- Offen: Das ganze Evangelium erzeugen, Glossen stichprobenartig prüfen, Lizenz/Namensnennung der Wortdaten klären, englischen Satz je Manuskript aus der BSB statt aus den Glossen. Die Manuskript-Nummer ist der Index in der Liste: Wird neu erzeugt, ändert sie sich (bestehende Bögen in der Welt würden falsch zugeordnet); dann auf IDs umstellen.

### 5. Rezepte und Fortschritt
- Crafting-Rezepte für alle Blöcke und Hilfsmittel (bisher nur Wooden Word Tablets).
- Gespeicherter Fortschritt je Spieler (übersetzte Manuskripte); heute geht eine halb fertige Übersetzung beim Schließen verloren.

### 6. Fragmente in der Welt
- Fund in Biomen (Loot oder kleine Strukturen). Die Nummer bestimmt die Position im Evangelium, die Schwierigkeit läuft getrennt.
- Die ersten Manuskripte sind kurz und einfach, spätere länger.

### 7. Bücher, Händler und Bookshelf
- Greek Alphabet Primer, Grammar Book und die Lexika als lesbare Bücher mit Inhalt.
- Händler (Villager-Beruf) mit Preisen in Emeralds; ein günstiges Wörterbuch zu Beginn.
- Bookshelf lagert Bücher. Bücher lassen sich nicht in den Arbeits-UIs öffnen: Spieler muss das UI verlassen.
- Zu klären: Welche Lexikon-Inhalte in welche Bücher, Preise, Überschneidungen.

### 8. Manuscript Archive
- Das Puzzle zum Zusammensetzen der Fragmente in die richtige Reihenfolge.
- Zu klären: Wie funktioniert das Verschieben im UI? Was gilt als „richtig“?

### 9. Restaurierungsaufträge, Achievements
- Villager bieten beschädigte Manuskripte an (Buchstabe oder Endung rekonstruieren), Belohnung in Emeralds. Optional.
- Achievements: gefundene, transkribierte, übersetzte Manuskripte, Vollständigkeit des Evangeliums.

### 10. Politur und Optik (ganz am Ende)
- Texturen und Modelle: eigene Textur für den Minting Table, Modelle statt Würfel (zum Beispiel zweiteilige Presse), Pult-Look.
- Optik der Einzelteile wie Kurbel, Hebel, Tintenfass und die Titelleiste.
- Kontroll-Mockups in `art/out/` neu erzeugen.

## Offene Designentscheidungen

Aus dem Design-Dokument und aus der Arbeit bisher:
- Wie der Archiv-Puzzle im UI funktioniert.
- Wann und wie freie Eingabe eingeführt wird.
- Welche Bibelstellen welche sprachlichen Anforderungen haben.
- Wie viele Restaurierungsaufträge ein Villager gleichzeitig anbietet.
- Bücherpreise und Lexikon-Inhalte.
- Ressourcenbalance: Verbrauch von Tinte, Papier und Tablets, Verschleiß der Letter Types, Ausbeute am Minting Table (derzeit 1 Nugget → 2 Letter Types).
- Ob der Minting Table auch Eisenbarren annimmt (Vorschlag: 1 Barren = 9 Nuggets).
