# Bücher für Scribery & Crafts: Anforderungen an den Text

Kurzanleitung, damit neue Buchtexte ohne Umbauten im Spiel richtig aussehen. Der Code dazu steht in `BookText.java` (Satz) und `BookScreen.java` (Anzeige).

## 1. Wo die Datei liegt

- Pfad: `mod/src/main/resources/assets/scribery/books/<item-id>.md`
- Kodierung: **UTF-8**, Zeilenenden egal.
- Der Dateiname ist die Item-ID. Ein Buch ohne Datei meldet „This book is still blank“.

| Buch | Dateiname |
|---|---|
| Greek Alphabet Primer | `greek_alphabet_primer.md` (fertig) |
| Greek Grammar Book | `greek_grammar_book.md` |
| Greek Lexicon A–C | `greek_lexicon_a_c.md` |
| Greek Lexicon D–F | `greek_lexicon_d_f.md` |
| Thematic Greek Lexicon | `thematic_greek_lexicon.md` |
| Greek Forms & Grammar Lexicon | `greek_forms_grammar_lexicon.md` |
| Complete Greek Lexicon | `complete_greek_lexicon.md` |

Sprache: derzeit Englisch, passend zum Rest des Spiels.

## 2. Was im Text erlaubt ist

| Zweck | Schreibweise |
|---|---|
| Titel der Titelseite | `# TITEL` |
| Untertitel | `### Untertitel` (steht vor dem ersten `## Page`) |
| Abschnitt | `## Page N — Titel` (die Nummer wird nicht gezeigt, nur der Titel) |
| Unterabschnitt | `### Überschrift` |
| Absatz | Text, durch eine **Leerzeile** vom nächsten getrennt |
| fett / kursiv | `**fett**`, `*kursiv*` |
| Liste | Zeilen mit `- ` am Anfang (keine Verschachtelung) |
| Tabelle | `\| A \| B \|`, danach `\|---\|---\|`, dann die Zeilen |
| Trenner | `---` wird ignoriert (nur zur Lesbarkeit der Datei) |
| Link | `[Text](#anker)`, `[Text](book:id)`, `[Text](book:id#anker)`, siehe Abschnitt 7 |
| fester Anker | am Ende einer Überschrift: `### 1. Alpha — Α α {#alpha}` |

**Nicht unterstützt:** Bilder, Code, Zitate (`>`), nummerierte Listen (`1.` als Listenpunkt), verschachtelte Listen, farbiger Text, Fußnoten, HTML, Links zu Internetseiten. Solche Zeichen erscheinen als normaler Text.

Zeichen: Alles, was die Minecraft-Standardschrift kann, geht: **Griechisch mit Akzenten und Spiritus, Ē, Ō, Gedankenstrich (—), Anführungszeichen.** Unbekannte Zeichen erscheinen als Kästchen. Bitte vorher ein Zeichen im Spiel prüfen, wenn es exotisch ist (zum Beispiel kombinierende Akzente in Sonderfällen).

## 3. Wie der Text gesetzt wird

- Eine Seite hat **114 Pixel Breite** und **14 Zeilen**. Das sind etwa **20–22 Zeichen pro Zeile**, also etwa **300 Zeichen pro Seite**.
- Das Buch zeigt immer eine **Doppelseite** (links und rechts), geblättert wird um beide Seiten.
- Die **Titelseite** (alles vor dem ersten `## Page`) steht allein und ist zentriert.
- Danach fließt der Text weiter. Ein `## Page`-Abschnitt beginnt **nicht** automatisch auf einer neuen Seite.
- **Ein Absatz bleibt auf einer Seite**, wenn er passt. Was nicht passt, rutscht auf die nächste Seite, das lässt unten Lücken. Absätze, die länger als eine ganze Seite (14 Zeilen) sind, werden geteilt.
- Überschriften bleiben mit dem Absatz darunter zusammen, ein Absatz, der mit `:` endet (zum Beispiel „Example:“), bleibt mit dem Folgenden zusammen.
- Tabellen werden bei Bedarf **verkleinert** (bis 60 %), damit sie in die Spalte passen. Läuft eine Tabelle über mehrere Seiten, wird die Kopfzeile wiederholt. Besser: **höchstens 3 Spalten** und **kurze Zellen**.

## 4. Regeln für Buchstabenabschnitte (wie im Primer)

Ein Unterabschnitt, dessen Überschrift mit einer Nummer beginnt (`### 7. Eta — Η η`), bekommt **immer eine eigene Doppelseite**:

- **Links:** Überschrift, die Zeile „Latin letter: …“ und die Beschreibung.
- **Rechts:** ab dem Absatz, der mit **`Example`** beginnt, bis zum Ende des Abschnitts.
- Vor dem ersten Buchstaben einer Gruppe (`## Page 3 — The First Six`) steht die Gruppenüberschrift oben auf dieser Doppelseite.
- Nach dem letzten Buchstaben beginnt der nächste Abschnitt auf einer neuen Doppelseite.

**Platz pro Buchstabe:** etwa **26 Zeilen** insgesamt, davon gehen rund 4 Zeilen für Überschriften drauf. Rechne mit **etwa 400–500 Zeichen Fließtext** plus dem Beispiel. Ist ein Buchstabe länger, darf ein Absatz von links nach rechts laufen, wird es noch länger, belegt er eine zweite Doppelseite (der Selbsttest meldet das).

Beispiel für einen Buchstaben:

```
### 7. Eta — Η η
**Latin letter: Ē**

Beschreibung in zwei bis vier kurzen Absätzen.

Example:

- ἡμέρα — HĒMERA — day

Eine kurze Schlussbemerkung.
```

## 5. Hinweise zum Schreiben

- **Kurze Absätze** (3–8 Zeilen) vermeiden Lücken. Ein Absatz mit mehr als 14 Zeilen wird geteilt.
- **Überschriften kurz halten** (eine bis zwei Zeilen), sie brechen sonst um.
- **Beispielzeilen** als Listenpunkt: `- griechisch — LATEIN — Bedeutung`. Sie brechen bei etwa 20 Zeichen um, was bei langen Zeilen zwei Zeilen heißt.
- **Tabellen**: Spaltenbreite nach dem längsten Eintrag. Für ein Lexikon eher zwei oder drei schmale Spalten (Wort, Umschrift, Bedeutung) als eine breite.
- **Lexika** mit sehr vielen Einträgen sind lang: rund **11 Einträge pro Seite** bei einer Zeile je Eintrag. Ein Buch mit 600 Einträgen hätte 50 Seiten. Wenn das zu viel wird, ist eine Tabelle mit kleinerer Schrift sinnvoller (wird automatisch verkleinert).
- **Seitenzahl** zeigt das Buch selbst. Kein „Page N“ im Fließtext verwenden.
- Der Spieler kann im Buch blättern (Pfeile, Mausrad, A/D, Pfeiltasten) und es mit der Inventartaste schließen.

## 7. Querverweise (Links)

Ein Link ist anklickbar (blau, unterstrichen) und springt zur Seite einer Überschrift, auch in ein anderes Buch.

| Ziel | Wirkung |
|---|---|
| `#anker` | Seite im **selben** Buch |
| `book:greek_grammar_book` | erste Seite eines **anderen** Buchs (Dateiname ohne `.md`) |
| `book:greek_grammar_book#anker` | Seite mit diesem Anker im anderen Buch |

- **Anker:** Jede Überschrift hat einen. Fest vergeben mit `{#id}` am Ende (`### 1. Alpha — Α α {#alpha}`); sonst ist es der **Slug** des Texts: Kleinbuchstaben, Buchstaben und Ziffern bleiben (auch Griechisch), alles andere wird zum Bindestrich (`## Page 2 — Meet the Alphabet` → `meet-the-alphabet`). Bei `## Page N — Titel` zählt der Titel. Feste Anker eines Buchs müssen eindeutig sein.
- **Inhaltsverzeichnis:** Ein Anker mit dem Namen **`contents`** (`## Page 0 — Contents {#contents}`) ist das Inhaltsverzeichnis; der Knopf **Contents** oben rechts und die Taste **C** springen dorthin.
- **Zurück:** Oben links steht **< Back**, sobald man einem Link gefolgt ist (Rück-Taste ebenso). Es führt Sprung für Sprung zurück, auch über Bücher hinweg.
- **Links in Listen, Absätzen und Tabellen** sind erlaubt, nicht in Überschriften. `**fett**` und `*kursiv*` dürfen um einen Link stehen. Der Linktext darf keine eckigen Klammern enthalten, das Ziel kein Leerzeichen.
- Ein Ziel, das es nicht gibt, bleibt ohne Wirkung und zeigt kurz eine Meldung („That book is still blank“, „That page is missing“). `BookMarkupTest` prüft, dass jeder Link in den echten Büchern ein Buch und einen Anker hat.

## 6. Prüfen

Nach dem Anlegen oder Ändern einer Datei:

```bash
./gradlew build
./gradlew runSelftest
```

Der Selbsttest öffnet den Primer, macht Screenshots (`run/screenshots/selftest-19…21`) und meldet in `run/logs/latest.log` die Seitenzahl und ob alle Buchstaben je eine Doppelseite haben. Für ein neues Buch kann ich den Test auf die neue Datei erweitern.
