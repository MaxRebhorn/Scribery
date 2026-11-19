# Scribery & Crafts – Roadmap

Stand: 19.11.2026, Branch `main`. Design: `Scribery_and_Crafts_Game_Design_Document.md`, Stationen `docs/STATIONS.md`, Fundorte `docs/DESIGN-FINDS.md` und `docs/DESIGN-STRUCTURES.md`, Händler `docs/DESIGN-VILLAGER.md`, Bücher `docs/BOOK-FORMAT.md`, Quellen `docs/CREDITS.md`.

## Fertig (Version 1.0)

- **Stationen:** Scribe's Lectern, Printing Press, Letter Cabinet, Translator's Desk, Minting Table (Ofen mit Brennstoff), Manuscript Archive, Bookshelf (Block, noch ohne Funktion).
- **Texte:** das ganze Johannesevangelium als 1.540 Manuskripte (Griechisch, Umschrift, Englisch), Archiv-Ansicht, Fundbiome thematisch.
- **Bücher:** Alphabet Primer, Grammar Book, Lexikon A–C, D–F, Thematic, Forms & Grammar, Complete Greek Lexicon.
- **Ostraka:** 30 echte Philogelos-Witze auf Griechisch, übersetzbar am Pult und am Tisch.
- **Scribe Villager:** eigener Beruf, fünf Handelsstufen, Fundbeutel, Fundkarte, Dubletten über das Archiv. Schreiberhaus in Ebene-, Taiga- und Schnee-Dörfern.
- **Fundorte:** Katakomben (nur unter der Erde), Wracks (Meer, andere Gewässer, geflutete Höhlen; folgen dem Gelände), Gräber, Karawane, Wanderer, eingestürzte Häuser in Sand und Schlamm (folgen dem Gelände), Suspicious Sand / Mud / Ice, Amphoren, Scherbenhaufen.
- **Daten und Tests:** Skripte unter `tools/`, Tests in `core/` (Daten, Ostraka, Gelände-Anpassung), Selbsttest für Oberflächen, Worldgen-Test auf dem Server.

## Vor oder kurz nach 1.0

- Im Spiel durchtesten: Schreiberhaus, Händlerpreise, Fundraten, Wracks und Häuser am Hang, Minting Table, Lampe.
- Preise und Fundraten balancieren (bisher geschätzt).
- Glossen der Ostraka prüfen (Entwurf).

## Danach (1.1 und später)

- **Umbau der Strukturen:** Bauwerke als NBT-Vorlagen statt als Java-Code, ein gemeinsames Platzier-Werkzeug (Wasser, Stützen, Chunk-Grenzen), `Debug…`-Klassen umbenennen, schnelle Worldgen-Tests.
- **Bookshelf:** Bücher lagern und ordnen, Nachschlagen aus der Arbeits-Oberfläche heraus (letzte Seite merken, Tastenkürzel).
- **Eigene Schrift** für φ und Akzente.
- **Suspicious Ashes** in der Welt platzieren.
- **Restaurierungsaufträge**, Erfolge, griechische Eingabe, ein Geschichtsbuch.
- **Übersetzungstisch direkt am Manuskript** (ohne Presse, schwerer, ohne Umschrift).
- **Fabric-Version:** erst den Spielinhalt in ein Modul ohne Loader-Code ziehen, dann portieren.
