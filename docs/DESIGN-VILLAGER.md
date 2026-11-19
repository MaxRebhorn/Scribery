# Entwurf: Scribe Villager (Schreiber)

Stand 19.11.2026: Beruf, Handel, Dublettenmarkierung und Fundbeutel sind gebaut (Branch `scribe-villager`), im Spiel noch nicht probiert. Zahlen ungetestet. Verknuepft mit `docs/DESIGN-FINDS.md` (Dubletten, Karten) und ROADMAP Abschnitt 4.

## Ziele und Grenzen
- Er ist die **Geld-Senke und Hilfe**: Lernbuecher kaufen, Dubletten loswerden, Wegweiser kaufen.
- Er verkauft **nie** fertige Reinschriften oder Uebersetzungen (sonst faellt das Lernen weg) und nie direkt ein gewuenschtes Fragment.
- Er darf keine Smaragd-Farm ermoeglichen: begrenzte Vorraete pro Tag, Verkaufspreis klar unter dem Wert dessen, was man damit kaufen kann.

## Beruf und Arbeitsblock
- Eigener Beruf `scribery:scribe`, Arbeitsblock **Scribe Lectern** (nicht das Bookshelf).
  - Grund: Das Bookshelf ist ein Deko-/Lagerblock, den Spieler zu Dutzenden platzieren; das Pult ist die eigentliche Station, schwerer zu verwechseln und thematisch richtig.
  - Nebenwirkung, die man akzeptieren muss: Jeder frei platzierte Lectern kann einen arbeitslosen Villager beanspruchen. Das ist gewollt (Spieler kann sich seinen Schreiber selbst bauen), aber Vanilla-Bibliothekare nehmen weiter ihr Vanilla-Lesepult.
- Umsetzung 1.21.1 / NeoForge: `PoiType` mit **allen** Blockstates des Pults (Blickrichtung!), `VillagerProfession` (Registries `POINT_OF_INTEREST_TYPE`, `VILLAGER_PROFESSION`), POI-Tag `minecraft:point_of_interest_type/acquirable_job_site`, Handel ueber `VillagerTradesEvent`, Arbeitssound (Schreibgeraeusch, vorerst Vanilla).

## Vorkommen (ehrlich: das ist das schwache Glied)
- Vanilla-Doerfer enthalten unser Pult nicht. Ohne weitere Massnahme gibt es den Schreiber nur, wenn der Spieler einen Pult neben einen Arbeitslosen stellt.
- **V1:** nur so (billig, funktioniert sofort). Ein Hinweis steht im Alphabet Primer / in der Anleitung.
- **V2:** kleines Schreibhaus als Dorfstruktur (Template-Pool-Injection in alle fuenf Dorftypen) mit einem Pult drin. Mehr Aufwand, erst nach V1.
- Zombie-Heilung und Dorfheld-Rabatte wirken wie ueblich; Rabatt nur auf Smaragdpreise.

## Handelsliste (5 Stufen, Vanilla-XP)
| Stufe | Er verkauft | Er kauft / tauscht |
|---|---|---|
| Novize | Alphabet Primer (1 Smaragd), Tinte/Papier-Kleinkram | Papyrusfetzen (je 8 -> 1 Smaragd), Ostraka |
| Geselle | Grammar Book (ca. 6), Tintenbeutel-Buendel | 3 Dubletten gegen einen **Fundbeutel** (siehe unten) |
| Fachmann | Lexikon A-C und D-F (je ca. 10) | Dubletten einzeln: leicht 1 / mittel 2 / schwer 3 Smaragde |
| Experte | Forms-Grammar-Lexicon, thematisches Lexikon (ca. 18) | |
| Meister | Complete Greek Lexicon (ca. 30), **Fundkarte** zu einer Ruine | |
Die Reihenfolge folgt der Spieler-Progression (zuerst Buchstaben, dann Formen, zuletzt Vollwoerterbuch).

## Dubletten richtig loesen (wichtigstes Problem)
Ein Vanilla-Handel kann nicht erkennen, ob ein Fragment eine Dublette ist. Wuerde er jedes Fragment annehmen, verkauft der Spieler versehentlich **einzige** Funde und verliert sie, denn das Fundstueck ist ja nicht archiviert.
- **Loesung:** Der Archivierungsschritt markiert das Item. Archiviert man ein Fragment, bleibt das Item erhalten und bekommt die Komponente `scribery:catalogued`; war es schon im Archiv, wird es zur **Dublette** (`scribery:duplicate`). Der Schreiber akzeptiert nur Items mit `duplicate`. Fortschritt liegt ohnehin in den Spielerdaten, nichts geht verloren (plan.md).
- Alternativ (schlechter): reine Anzahl-Regel ohne Markierung. Verworfen, weil unfair und missbrauchsanfaellig.
- Handel nutzt `ItemCost` mit Komponenten-Praedikat (1.21: `DataComponentPredicate`).

## Fundbeutel statt "waehle dein Biom"
Ein Vanilla-Handel hat ein festes Ergebnis, kein Zufalls- oder Auswahlergebnis.
- 3 Dubletten -> **ein Fundbeutel** (5 Items, je Fundtyp: Sand, Schlamm, Asche, Eis, Scherben). Rechtsklick oeffnet ihn und gibt ein Fragment aus dem Pool dieses Typs, mit dem Schlechtes-Glueck-Schutz (rund 70 % noch fehlende), serverseitig ueber die Spielerdaten.
- So waehlt der Spieler den Biom-Typ (Wunsch aus DESIGN-FINDS) ohne dynamische Handelsliste. Fuenf Handelsplaetze, einer pro Beutel, pro Beutel Tageslimit 2.
- Kontrolle gegen Schleife: 3 Dubletten kosten ihn mindestens 3 Funde und bringen 1 Fund zurueck. Der Verkauf einzeln (1 bis 3 Smaragde) darf nie mehr bringen als die Karten/Buecher an Wert haben, daher Tageslimits und Preise unter dem Lexikonpreis.

## Fundkarten
- Vanilla-Karten zeigen nur auf Strukturen, nicht auf Biome. Daher: Karte zeigt auf eine unserer Ruinen/Katakomben (Struktur-Tag `#scribery:scribe_map_targets`). Biom-Karten sind **nicht** moeglich ohne eigene Suchlogik; vorerst weglassen.
- Erst ab Meister; Preis hoch (ca. 16 Smaragde + Kompass), damit sie Besonderes bleiben.

## Aussehen
- Texturen (Vanilla-Layout 64x32/64x64): `textures/entity/villager/profession/scribe.png`, Stufenanzeige ueber `profession_level` (Vanilla-Abzeichen nutzen, nicht kopieren).
- Look: heller Leinen-Kittel, braune Schreiberschuerze mit Tintenflecken, Hut/Haarreif in Pergamentton, Gelehrten-Gürtel mit Schreibrohr und Papyrusrolle. Palette aus `art/palette.json` (wood_*, parchment_*, ink_*), keine Vanilla-Kopien.
- Entstehung wie die Werkbloecke: Skript `art/gen_scribe_villager.py` mit Vorschau. Offene Frage: Vanilla-Villager-UV-Vorlage muss aus dem Spiel/Referenz geholt werden (Netz im Workspace gesperrt).

## Kritik und Risiken
1. Ohne Dorfstruktur (V2) bleibt der Schreiber ein Selbstbau-Feature. Wer ihn nicht findet, hat kein Dublettenventil. Deshalb muss der Alternativweg (Dubletten-Tausch am Archiv oder an einer Truhe) vor V2 mitgedacht werden.
2. Markierung der Dubletten braucht eine Aenderung im Archiv (Item bleibt erhalten). Das beruehrt bestehende Archivlogik und Tests.
3. Fuenf Fundbeutel + Dubletten-Items = viele neue Items und Texturen. Pragmatisch: ein Beutel-Item mit `custom_model_data`.
4. Handels-UI ist die Vanilla-Oberflaeche: keine Anzeige "du hast X Dubletten". Ein Tooltip auf dem Item mitgeben.
5. Preise stammen aus dem Bauch. Erst nach einer Spielsimulation (Funde pro Stunde, Dublettenanteil) festlegen.

## Offene Entscheidungen
- Arbeitsblock Pult (Empfehlung) oder Bookshelf?
- Dublettenmarkierung ueber Archiv (Empfehlung) oder anderer Weg?
- V2 Schreibhaus im Dorf: ja/nein, wann?
- Fundbeutel akzeptieren, oder lieber freie Auswahl ueber ein Archiv-GUI?

## Umsetzung (Stand 19.11.2026)
- **Beruf:** `ModVillagers` (POI-Typ mit allen Pult-Zustaenden, Beruf `scribery:scribe`, Tag `acquirable_job_site`).
- **Papier:** je Dorfbewohner nach Zufall entweder kauft er Papier (24 gegen 1 Smaragd) oder verkauft es (6 pro Smaragd). Tinte verkauft er nicht. Der Alphabet Primer steht als erstes Angebot.
- **Handel:** `core/ScribeTrades` (reine Daten, JUnit-getestet), Zahlen in `core/ScribePrices`, Umsetzung in `ScribeTradeEvents`. Vanilla waehlt je Stufe nur zwei zufaellige Eintraege aus der Liste; deshalb steht je Stufe ein einziger Eintrag (`LevelListing`) in der Liste, der alle Angebote der Stufe selbst an die Angebotsliste anhaengt. Tageslimit = `maxUses`.
- **Dubletten (Entscheidung Max):** Das Archiv speichert das Fragment-Item. Rechtsklick mit einem Fragment legt es ab (Item weg, Eintrag im Archiv, ID in der Liste `stored_fragments` des Spielers); Schleichen + Rechtsklick mit leerer Hand nimmt das zuletzt abgelegte wieder heraus, der Fortschritt bleibt. Was ein Spieler lose bei sich hat, ist damit tauschbar: der Scribe nimmt jedes lose Fragment (kein Zeichen am Item, keine Komponenten). Einzelverkauf ein Preis fuer alle (`ScribePrices.FRAGMENT_SELL`). Risiko: wer ein noch nie abgelegtes Fragment verkauft, verliert es; die Meldung am Archiv weist darauf hin.
- **Fundbeutel:** fuenf Items `find_pouch_<sand|mud|ashes|ice|shards>`. Rechtsklick gibt ein Fragment aus dem Pool der Fundart; `core/FindLuck`: 70 % der Zuege waehlen ein im Archiv fehlendes Manuskript. Handel: 3 lose Fragmente gegen einen Beutel, Tageslimit 2 je Beutel (Stufe 2).
- **Fundkarte (Meister):** zeigt auf die naechste Struktur im Tag `scribery:scribe_map_targets` (Katakomben, Wuesten-Katakomben, Sand- und Schlammhaus). Findet er innerhalb von 100 Chunks keine, fehlt das Angebot. Biom-Karten bleiben offen (keine eigene Suchlogik).
- **Offen:** Dorfstruktur mit Schreibhaus (V2), Alternativweg fuer Dubletten ohne Schreiber, Preise nach Spielsimulation, Handels-UI zeigt keine Dublettenzahl (nur Tooltip am Item).

**Aenderung 19.11.2026:** Grammar Book jetzt schon auf Stufe 1 (neben dem Alphabet Primer); Stufe 3 verkauft die Lexika A-C, D-F und das Forms-Grammar-Lexikon; thematisches Lexikon Stufe 4; das Complete Greek Lexicon bleibt Stufe 5, bis das Buch fertig ueberarbeitet ist.

**Stand Buecher 19.11.2026:** Lexikon A-C, D-F, Thematic und Complete sind geschrieben (`tools/gen_lexicon.py`, Vorworte in `tools/data/lexicon-texts.json`, Sachgebiete in `tools/lexicon_themes.py`). Das Forms & Grammar Lexicon ist noch leer und steht deshalb nicht im Handel; es kommt auf Stufe 3 zurueck, sobald der Text steht.

**Nachtrag:** Das Forms & Grammar Lexicon ist jetzt geschrieben (`tools/gen_forms_lexicon.py`, Daten: Wortformen mit Robinson-Code aus den Manuskriptdaten) und steht wieder auf Stufe 3. Das Complete Greek Lexicon enthaelt es als Teil 3.

**Ostraka zum Uebersetzen (19.11.2026):** 30 echte Philogelos-Witze (Griechisch aus dem Eulogikon-Korpus, englische Fassungen eigen) liegen als Scherben vor. Ein Ostrakon laesst sich wie ein Manuskript auf das Pult legen und an Presse und Uebersetzungstisch bearbeiten (Index ab `Manuscripts.OSTRAKA_BASE`, Glossen von Hand in `tools/ostraka_glosses.py`, erzeugt von `tools/build_ostraka.py` nach `tools/data/ostraka.json`). Uebersetzte Ostraka-Boegen lassen sich nicht im Archiv ablegen. Schleichen + Rechtsklick auf den Scherben zeigt die Loesung.

**Schreiberhaus im Dorf (19.11.2026, V2 gebaut):** Ein fertiges Haus (9x10x11) mit Pult, Presse mit Lettern-Schrank daneben (die Presse liegt weit vom Pult, damit beide getrennte Stationen bleiben), vier Buecherregalen in zwei Lagen, Bett, Teppich, Laternen und Kerze. Es haengt ueber den Jigsaw `minecraft:building_entrance` an die Dorfstrasse und hat einen Dorfbewohner-Platz (`minecraft:bottom`). Vorlagen: `data/scribery/structure/village_scribe_house_{plains,taiga,snowy}.nbt` (gebaut von `DebugVillageHouse`, Aufruf `-Pwgtest=house:plains` usw.). `VillageInjection` haengt sie beim Serverstart mit Gewicht 4 in die Haeuser-Pools der Doerfer Ebene, Taiga und Schnee; Wueste und Savanne (Akazien) bekommen es nicht. Pool-Test: `-Pwgtest=jigsaw:minecraft:village/plains/houses` (etwa 2 bis 4 von 40 gesetzten Haeusern sind das Schreiberhaus).
