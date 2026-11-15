# Scribery & Crafts: Umsetzungsplan (v0.2)

Stand: 15.11.2026. Entwurf aus dem Brainstorming. Alles, was mit **[offen]** markiert ist, ist noch nicht entschieden.

## 1. Spielidee

Der Spieler findet zerrissene Fragmente altgriechischer Manuskripte (Johannesevangelium), setzt ihre Wörter mit Bleilettern in lateinischen Buchstaben, färbt und druckt sie, übersetzt sie mit Wörterbüchern und heftet sie in der richtigen Reihenfolge in ein Buch. Ziel: das Evangelium Stück für Stück wiederherstellen und dabei Alphabet und Grundwortschatz des Koine-Griechischen lernen. Fortschritt gibt Achievements.

## 2. Festgelegt

- **Minecraft 1.21.1**, Java 21. Loader-Vorschlag: NeoForge 21.1.x (Fabric wäre auch möglich).
- **UI zuerst.** Das Mod steht und fällt mit dem Spielgefühl der Presse.
- **Text: Johannes**, Griechisch (Koine). Mod-Oberfläche auf Englisch.
- **Übersetzungsanspruch: Interlinear.** Das Spiel prüft eine wörtliche Wort-für-Wort-Wiedergabe in griechischer Wortstellung, keine Sinnübersetzung. Der englische Satz darf holprig klingen und ist trotzdem richtig. Eine glatte Lesefassung (BSB) gibt es höchstens als optionalen Vergleich, nie als „richtige“ Lösung.
- **Daten per Skript** aus dem Urtext erzeugt, nicht von Hand geschrieben.
- **Wörterbuch-Bücher** bei Villagern (Bibliothekar) mit Kontext- und Wortbedeutungen, von KI geschrieben und geprüft.
- **Fundorte** hängen an Biomen und besonderen Blöcken.
- **Fragmente werden einmal generiert** und danach eingefroren: feste Items mit stabilen IDs `P1` bis `Pn` in Textreihenfolge.
- **Stufen und Begriffe:** Fragment (gefunden) → Abzug (lateinisch gedruckt) → **Reinschrift** (fehlerfrei interlinear übersetzt, englisch „fair copy“) → Folio (in ein Kapitel geheftet). Der englische Lesetext heißt **Vergleichstext**, nicht Reinschrift.
- **Gebunden werden können nur fehlerfreie Reinschriften.** Ein Kapitel ist erst komplett, wenn alle seine Fragmente als Reinschrift geheftet sind.
- Visuelle Vorlage: das ChatGPT-Bild (Pixel-Look, leichte Draufsicht, Pult links, Presse Mitte, Fass rechts, Buchstabenbox, Stapel).

### Glossierungsregeln (festlegen, bevor Daten entstehen)

Wörtlich heißt nicht automatisch neutral: jede Glosse enthält Entscheidungen. Darum gibt es ein kurzes Regelblatt, an das sich alle Glossen halten, und der Validator prüft die Einhaltung:
- **Eine kanonische Glosse pro Wort** im Kontext, dazu eine kurze Liste erlaubter Synonyme.
- **Endungen** werden einheitlich wiedergegeben (Genitiv „of“, Dativ „to/in“, Aorist als einfaches Präteritum, Perfekt mit „has“ usw.).
- **Artikel** werden als „the“ wiedergegeben, auch wenn das Englische ihn nicht braucht, und fehlende Artikel bleiben fehlend (z. B. bei θεός in Joh 1,1c).
- **Mehrdeutige Wörter:** Standardfall ist die übliche Interlinear-Glosse (z. B. πρός = „with“ statt „toward“). **[offen]** ob beide akzeptiert werden. Empfehlung: beide, solange sie im Regelblatt stehen.
- **Randnotizen** erklären Streitstellen, entscheiden sie aber nicht.

## 3. Offene Entscheidungen (mit Empfehlung)

| Frage | Empfehlung |
|---|---|
| Überlappende Fragmente (zwei Funde mit demselben Teiltext)? | **v1: nein.** Jedes Wort steckt in genau einem Fragment. Sonst wird Heften und Fortschritt zweideutig. Später: Doppelfunde als Bonus (z. B. Papier oder Tinte). |
| Fragmentgröße | Steigt mit dem Textverlauf (P1 kurz, später länger), aber **gedeckelt bei etwa 40 Wörtern**. Längere Stellen werden in mehr Fragmente geteilt, nicht in längere. Schnitt an Satzgrenzen, nie über Versgrenzen. Sonst passt ein Fragment nicht mehr in die Setzform. |
| Fundreihenfolge | Zufall **innerhalb** einer Stufe. Stufen (z. B. P1–P10, P11–P25, …) hängen an Biom-Bändern und Schwierigkeit, damit niemand früh auf ein langes, schweres Fragment trifft. Dazu Hinweise (Kartograf oder Lupe), damit niemand ewig auf das letzte Stück wartet. |
| Client oder Server | Phase 1 reiner Client-Screen (schnelle Iteration). Vor dem ersten Multiplayer-Test auf Menu und Pakete umstellen. Bewusst eingeplantes Refactoring. |
| Umfang v1 | Prolog (Joh 1,1–18) komplett und geprüft, danach Kapitel 1. Die Pipeline erzeugt das ganze Evangelium, aber ungeprüfte Teile bleiben gesperrt. |
| Loader | NeoForge, sofern sich nichts Gegenteiliges zeigt. |
| Kopieren und Verkaufen | Reinschriften lassen sich gegen Papier und Tinte kopieren und an einen **Schreiber-Villager** verkaufen. Er verkauft **nie fertige Reinschriften** (sonst überspringt man das Lernen), höchstens Rohfragmente und Hinweise. Der Verkaufswert liegt unter den Kopierkosten, und es gelten die üblichen Handelslimits pro Tag, damit keine Smaragdfarm entsteht. **[offen]** |
| Fortschritt pro Spieler oder geteilt | Empfehlung: pro Spieler, die Wand liest die Spielerdaten. Eine gemeinsame Server-Wand kann später kommen. |

## 4. Architektur

Drei getrennte Teile, damit ich möglichst viel ohne laufendes Minecraft testen kann:

- **core/** (reines Java, keine Minecraft-Abhängigkeit): Umschrift Griechisch zu Lateinisch, Fragment-Modell, Prüfung der Abzüge und Übersetzungen, Fortschrittslogik. Hat Unit-Tests, die ich in meiner Umgebung laufen lassen kann (sofern Java und Gradle dort verfügbar sind).
- **mod/** (NeoForge-Schicht): Blöcke, Items, Screens, Animationen, Loot, Villager-Handel, Netzwerk, Speicherung.
- **tools/** (Python): Datenpipeline, siehe Abschnitt 7.

```
scribery/
  core/      Umschrift, Modelle, Regeln, Tests
  mod/       NeoForge-Mod (src/main/java, resources)
  tools/     Pipeline-Skripte, Quelldaten, Validierung
  art/       Palette, Textur-Skripte, Referenzbilder
  docs/      dieser Plan, Designnotizen
```

Die Umschrift (`translit`) liegt in einer Version in Python (Pipeline) und einer in Java (core). Beide laufen gegen dieselbe **Golden-Datei** (Griechisch → erwartete lateinische Form). Wenn sie voneinander abweichen, schlagen die Tests an.

## 5. Phasen (UI zuerst)

### Phase 0: Spezifikation (kein Code)
- UI-Raster in GUI-Pixeln festlegen (Größen von Bleiletter, Slot, Papierstreifen, Fass, Walze, Kerze).
- Palette und Pixelgröße festlegen, damit nichts wie im ersten Gemini-Bild mischt.
- Datenformat (Abschnitt 7) mit **Platzhalterinhalt** festlegen, damit das UI von Anfang an gegen die echte Struktur gebaut wird.
- **Fertig, wenn:** eine Seite mit Raster, Maßen und Palette liegt, plus ein JSON-Beispiel mit drei Platzhalter-Fragmenten.

### Phase 1: Presse (der Kern)
- Debug-Befehl (`/scribery debug`), der die Presse ohne Welt-Vorbereitung öffnet.
- Interaktion siehe Abschnitt 6. Alles zunächst mit Platzhaltertext (`ΕΝ ΑΡΧΗ ΗΝ Ο ΛΟΓΟΣ`).
- **Fertig, wenn:** Setzen, Einfärben, Hebelzug und Abzug (richtig und verwischt) sich im Spiel gut anfühlen und ein Testspieler ohne Erklärung durchkommt.

### Phase 2: Restliche Stationen
- Station-Wechsel mit den Pfeilen links und rechts (Lupe, Presse, Plättchen, Heftung). Mitte = aktuelle Station. Das Wörterbuch ist ein Buch-Item, keine eigene Station.
- Lupe: Fragment groß und lesbar, Randfarben-Vergleich.
- **Plättchen-Station** (ersetzt die Optionsliste): Holzkästen mit zweiseitigen Plättchen (z. B. Vorderseite „was“, Rückseite „is“). Linksklick nimmt auf und legt ab, Rechtsklick dreht. Die Plättchen kommen in Slots auf dem **Interlinear-Blatt**: unter jedem lateinischen Wort ein Slot, feste Reihenfolge wie im Griechischen, kein freies Anordnen. Die Feder schreibt das gelegte Wort, bei einem falschen Plättchen verwischt es.
  - Die Kästen enthalten pro Fragment nur die Plättchen dieses Fragments (Schubladen pro Wort), sonst fehlt der Platz.
  - Zwei Lernschritte: Das Wörterbuch (Villager-Buch) nennt die Grundform, der Kasten fragt nach der Form (Zeit, Zahl, Fall). Der Kasten darf die Grundform nicht schon verraten.
  - Falsche, aber plausible Plättchen (z. B. „words“ statt „word“), anfangs grob, später fein. Die Endungs-Lupe hilft.
  - Die Interlinear-Regeln (Abschnitt 2) legen fest, welche englischen Formen auf Plättchen stehen.
- Heftung: Reihenfolge der Funde. **Nur fehlerfreie Reinschriften lassen sich heften.** Nach dem Heften erscheint die Interlinear-Zeile des Fragments (wörtlich, griechische Wortstellung). Eine BSB-Lesefassung ist nur ein optionaler Vergleich.
- **Fertig, wenn:** ein Fragment mit Platzhalterdaten komplett durchgespielt werden kann.

### Phase 3: Welt
- Fragmente als Items, Fundorte und Loot (Abschnitt 8), Schreiber-Villager, Achievement-Seite, Fortschrittswand, Speicherstand pro Spieler.
- **Tutorialbuch** als Startitem beim ersten Fragment: ein durchgerechnetes Beispiel (Joh 1,1 Wort für Wort), dazu der Hinweis „Das Spiel prüft Wörter, nicht Stil“. Es soll verhindern, dass Spieler einen holprigen, aber richtigen englischen Satz für einen eigenen Fehler halten. Die Fragmentkarte trägt zusätzlich ein Siegel „Interlinear“.
- **Fertig, wenn:** man in einer Testwelt ein Fragment findet und es durch alle Stationen bringen kann.

### Phase 4: Echte Daten
- Pipeline (Abschnitt 7) erzeugt Fragmente aus dem Urtext. Prolog wird geprüft und freigegeben.
- **Fertig, wenn:** Prolog komplett spielbar, alle Wörter geprüft.

### Phase 5: Politur und Ausbau
- Sounds, Partikel, Texturen, Balancing von Tinte und Eisen, weitere Kapitel.

## 6. Spezifikation der Presse

Ablauf pro Zeile:
1. **Buchstaben aufnehmen.** Klick auf eine Letter in der Buchstabenbox, sie hängt am Cursor wie ein gehaltenes Item. Unter jeder Letter steht der Vorrat (Minecraft-Stapelzahl). Nur die Buchstaben, die das Alphabet braucht: A B C D E Ē G H I K L M N O Ō P R S T U X Z.
2. **Setzen.** Die Letter wird in ein Slot des Metallrahmens (Setzform) gedrückt, so dass sie fest sitzt. Der Rahmen fasst eine **Zeile**, nicht nur ein Wort.
3. **Einfärben.** Ein Fass mit Walze. Klick auf die Walze nimmt sie heraus, sie wird am Cursor größer, man streicht 2 bis 3 Mal über die Lettern (Fortschritt sichtbar), legt sie zurück. Jedes Eintauchen verbraucht Tinte. **Der Füllstand zeigt sich an der Farbe des Fasses** (satt = voll, blass = leer).
4. **Papier einlegen** und an der **Kurbel** drehen (Papier läuft in die Presse).
5. **Hebel ziehen.** Die Presse senkt sich, der Abzug erscheint.
6. **Ergebnis.** Richtig: sauberer Abzug. Falsches Wort: dieses Wort ist verwischt. Der Spieler erfährt nicht, welcher Buchstabe falsch ist.

Regeln:
- Das Einfärben ist **kein Zufallsfaktor**. Verwischt wird nur bei falschen Buchstaben, sonst kann der Spieler Fehler nicht mehr deuten.
- Lettern nutzen sich beim Drucken zufällig ab. Neue werden aus Eisen-Nuggets gegossen.
- Proportionen: Bleiletter in Presse und Box gleich groß; das „Blatt“ ist ein schmaler Streifen für eine Zeile, kein DIN-A4.
- Animationen (jeweils kurz und mit Ton): Letter einsetzen, Walze streichen, Kurbel, Hebel, Papier gleitet, Stempel senkt sich, Abzug erscheint, Verwischen. Kerze flackert dauerhaft.
- Beginner-Hilfe: Villager-Buch mit der Buchstabentafel (Umschrifttabelle), optional.

Der Abzug wird **automatisch** geprüft: das Ziel (lateinische Form) wird aus dem griechischen Wort per Umschrift berechnet, nichts davon muss von Hand erfasst werden.

## 7. Daten und Pipeline (Skripte)

Schritte in `tools/`:

1. **Quelle laden und normalisieren.** Griechischer Text mit Morphologie (Wort, Grundform, Grammatik) plus englischer Vergleichstext. Quelle und Lizenz sind **[offen]**: STEPBible-Daten und MorphGNT sind Kandidaten, Lizenz jeweils prüfen. Ergebnis: `words.json`.
2. **Segmentieren.** Schnitt an Satzgrenzen, Größe steigt mit dem Textverlauf und ist bei etwa 40 Wörtern gedeckelt, nie über Versgrenzen. IDs `P1` bis `Pn` in Textreihenfolge, **einmal erzeugt und eingefroren** (spätere Änderungen nur per Migration, damit Spielstände gültig bleiben). Ergebnis: `fragments.json`.
3. **Glossen erzeugen** nach den Glossierungsregeln aus Abschnitt 2. Pro Wort im Kontext: kanonische Glosse, erlaubte Synonyme, falsche Alternativen, Status `draft` oder `reviewed`. Entwürfe von KI, aus freien Lexika (z. B. Thayer, Abbott-Smith). Nur `reviewed` kommt ins Spiel, `draft` bleibt gesperrt.
4. **Fundorte zuordnen.** Tabelle Kapitel-Band → Biom und Behälter.
5. **Validieren.** Prüft unter anderem:
   - jedes Wort steckt in genau einem Fragment, keine Überlappung
   - alle 879 Verse abgedeckt
   - jede lateinische Form besteht nur aus Buchstaben der Box
   - jedes Wort hat mindestens eine akzeptierte Glosse
   - keine falsche Alternative ist gleichzeitig akzeptiert
   - Golden-Test der Umschrift in Python und Java stimmt überein

Beispiel für ein Fragment:

```json
{
  "id": "jhn-001-a",
  "order": 0,
  "ref": "John 1:1-2",
  "reference_en": "optional, nur als Vergleich: In the beginning was the Word ...",
  "origin": { "biome": "#minecraft:is_desert", "container": "suspicious_sand" },
  "words": [
    { "i": 0, "greek": "Ἐν", "lemma": "ἐν", "morph": "PREP",
      "gloss": { "ok": ["in"], "wrong": ["through", "under", "against"], "status": "draft" } }
  ]
}
```

Die zu setzenden Lücken-Wörter werden per Regel bestimmt (kurze Funktionswörter sind vorgedruckt), mit optionaler Überschreibung pro Wort. Das Buchstabeninventar eines Fragments lässt sich aus dem Text berechnen, z. B. für Loot-Balance.

## 8. Welt, Fundorte, Fortschritt

Fundorte nach Biom (Ideen, teils offen):

| Biom | Behälter |
|---|---|
| Wüste | Verdächtiger Sand mit Pinsel (Vanilla-Archäologie, Loot per Loot-Modifier erweitern) |
| Schneebiome | Eisblöcke mit Fragment darin |
| Sumpf | Versiegelte Tonkrüge in morschem Holz, in Sumpf-Features |
| Weitere | Truhen in Dörfern und Ruinen als Basis-Quelle |

- Jedes Kapitel (oder jede Gruppe) bekommt ein Biom-Band, damit Spieler erkunden müssen. Das Johannesevangelium beginnt thematisch in der Wüste (Täufer), das passt für Kapitel 1.
- **Bibliothekar-Villager** verkauft Wörterbücher, Kontextbücher und Hinweise auf Fundorte. Inhalt von KI geschrieben und geprüft, kurz und überprüfbar.
- **Achievements** über das Vanilla-Advancement-System (per Datenpaket): erster Abzug, alle Buchstaben benutzt, Kapitel vollendet, Prolog wiederhergestellt, ganzes Evangelium.

### Achievement-Seite und Fortschrittswand

- **Eigene Achievement-Seite**: ein eigener Tab im Vanilla-Fortschrittsmenü für die Meilensteine des Mods.
- **Fortschrittswand** (Block mit eigenem Screen): ein riesiges Puzzle des Evangeliums, eine Zelle pro Fragment (P1 bis Pn), nach Kapiteln gruppiert. Jede Zelle hat fünf Zustände: unbekannt, gefunden, gedruckt, übersetzt, geheftet. Das Bild füllt sich, je mehr man hat.
- Dazu vier Fortschrittsbalken: **gefunden**, **gedruckt** (lateinisch), **fehlerfrei übersetzt** (Reinschrift), **geheftet** (im Kapitel).
- **Der Fortschritt liegt in den Spielerdaten, nicht in den Items.** Items sind Schlüssel für die Stationen (Fragment → Abzug → Reinschrift → Folio), die Wand liest die Spielerdaten. So verliert man seinen Fortschritt nicht, wenn man ein Item abgibt oder verkauft.

## 9. Risiken

- **Qualität der Glossen.** Automatisch erzeugte Bedeutungen enthalten Fehler (Beispiel: „heiliger Atem“ statt „Heiliger Geist“). Darum Prüfstatus pro Wort, und nur geprüfte Teile sind spielbar. Fachliche Prüfung kostet am meisten Zeit.
- **Umstrittene Stellen** (z. B. Joh 1,1c, Joh 7,53–8,11, 1,18) brauchen neutrale Randnotizen und eine sichtbare Entscheidung, welche Textgrundlage gilt.
- **Interlinear ist nicht automatisch neutral.** Schon die Wahl der Glosse (z. B. „with“ oder „toward“) ist eine Entscheidung. Darum Regelblatt, ein einheitlicher Maßstab und Randnotizen bei Streitstellen.
- **Fragment-IDs müssen stabil bleiben.** Ändert sich die Segmentierung später, sind Spielstände kaputt. Darum einmal erzeugen, einfrieren, nur per Migration ändern.
- **Reinschriften kaufbar machen** würde das Lernen überspringbar machen. Der Schreiber-Villager verkauft sie deshalb nicht.
- **Lizenzen** von Quelltext, Lexika und Übersetzung vor jeder Veröffentlichung prüfen.
- **Grind.** Zu viele Fragmente mit zufälligen Fundorten können nerven. Gegenmittel: Biom-Bänder, Hinweise, feste Mindestfunde.
- **API-Wechsel.** GUI-Code wechselt von 1.21.x zu 26.x stark. Deshalb 1.21.1 für das Original, Portierung später.
- **Pixel-Look.** Die Texturen entstehen per Skript aus einer festen Palette. Künstlerisch wird das einfacher aussehen als die Konzeptbilder.

## 10. Arbeitsweise

- Ich schreibe Code und Daten, du startest das Spiel (`./gradlew runClient`) und schickst mir Fehlerlogs und Screenshots. Das ist der langsame Teil, also: kleine Schritte, je ein klarer Test.
- Jede Phase hat ein „Fertig, wenn“. Erst danach die nächste.
- Entscheidungen aus Abschnitt 3 werden hier nachgetragen, damit das Dokument aktuell bleibt.
