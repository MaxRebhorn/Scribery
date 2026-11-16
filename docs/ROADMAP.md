# Scribery & Crafts – Roadmap

Reihenfolge der nächsten Schritte. Oben steht, was als Nächstes kommt. Der Stand bezieht sich auf Branch `phase-1-press`.
Das Design steht in `Scribery_and_Crafts_Game_Design_Document.md`, die UI-Regeln in `UI-REQUIREMENTS.md` und `docs/ui-spec.md`.

## Stand heute

Fertig:
- Items: 22 Letter Types, 5 Manuskriptfragmente (Platzhalter), 7 Bücher (ohne Inhalt).
- Blöcke: Scribe's Lectern (legt Fragment auf, öffnet Oberfläche), Printing Press und Letter Cabinet (gemeinsame Druck-Oberfläche, zeigt nur, was angrenzt, freier Druck ohne Fragment), Minting Table (Stonecutter-Menü, Eisennuggets → Letter Types).
- Der Letter Cabinet lagert Letter Types, Tintenbeutel und Papier. Die Presse bezieht und verbraucht ihre Vorräte, der Server prüft jede Meldung.
- Übersetzungstisch: nur über Pfeile vom Pult aus erreichbar, nur mit Fragment.
- UI-Optik: Tischplatte, Minecraft-Texturen, unsere Letter-Items, ausgefranstes Papier in Textgröße.

Noch Platzhalter: Bookshelf, Manuscript Archive, die Bücher, Rezepte, die Texte (nur 3 Platzhaltertexte aus Johannes 1).

## Reihenfolge

### 1. Bedruckter Bogen als Item
- Der fertige Abzug kommt als Item aus der Presse und trägt seinen Text und seine Manuskript-ID.
- Auch im freien Druck (ohne Fragment) entsteht ein Zettel, auf dem das Gesetzte steht.
- Der Übersetzungstisch nimmt den Bogen entgegen, statt ihn aus dem Presse-Zustand zu holen.
- Damit entfällt der flüchtige Arbeitsstand im Client (`Workbench`), der beim Schließen verloren geht.
- Zu klären: Darf ein Bogen mehrfach übersetzt werden? Wie wird er beschriftet, wenn er schon übersetzt ist?

### 2. Wooden Word Tablets
- Item plus Rezept (Vorschlag: 6 Wooden Planks → 3 Wooden Word Tablets).
- Jede Wortposition in der Übersetzung verbraucht ein Tablet, auch bei falscher Antwort.
- Der Übersetzungstisch bekommt dafür einen Vorrat (Block oder Anschluss an den Schrank).
- Textur für das Item, der Rest nutzt Vanilla-Optik.

### 3. Freie Eingabe von Hand
- Der Spieler kann Wörter oder Endungen selbst eintippen, statt nur Antworten zu wählen (Design-Dokument 5.2).
- Zu entscheiden: Wo genau (nur Übersetzung, auch Restaurierung, auch freier Druck)? Als Alternative oder als Ersatz zum Multiple Choice?
- Eingabeprüfung tolerant: Akzente und Spiritus ignorieren, Buchstaben und Endungen prüfen, Groß- und Kleinschreibung beachten.
- Ein dezenter Button, der nicht stört, öffnet eine Palette mit den Sonderzeichen, die nicht auf der normalen Tastatur liegen:
  - griechische Buchstaben, Akzente (Akut, Gravis, Zirkumflex), Spiritus (asper, lenis), Iota subscriptum, Trema,
  - Makron-Buchstaben wie Ē und Ō, soweit nötig.
- Ort für den Button: klein am Rand der Eingabe oder der Titelleiste, nur sichtbar, wenn Eingabe aktiv ist. Muss noch ausprobiert werden.
- Technisch: griechische Unicode-Zeichen im Textfeld, Normalisierung (NFC/NFD) beim Vergleichen.

### 4. Echte Texte und Manuskript-IDs
- Ein Fragment-Item trägt eine Manuskript-ID (P1, P2, …) statt fünf fester Items.
- Wörter, Glossen und Schwierigkeit kommen aus einer Datendatei; `manuscript_order` und `difficulty` sind getrennt.
- Validator: jedes Wort muss in die Setzform passen (offener Punkt aus der UI-Spezifikation), Umschrift (`γγ` → `GG`, `υ` → `U`) prüfen.
- Max liefert die Johannes-Texte und Glossen, das Werkzeug dafür baue ich.

### 5. Rezepte und Übersetzungstisch fertig
- Crafting-Rezepte für alle Blöcke und Hilfsmittel.
- Rechtsklick direkt auf den Translator's Desk öffnet die Übersetzung, ohne Umweg über das Pult.
- Gespeicherter Fortschritt je Spieler (übersetzte Manuskripte).

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
