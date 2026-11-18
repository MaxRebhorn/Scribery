# Quellen, Lizenzen, Namensnennung

Stand: 18.11.2026. Ich bin kein Anwalt; das hier ist die Auswertung der Lizenztexte, die in den Dateien selbst stehen.

## Griechischer Text, Grammatik, Strong-Nummern, englische Glossen

**STEPBible-Data, Datei TAGNT** (Translators Amalgamated Greek NT), erstellt von STEPBible.org auf Grundlage der Arbeit des Tyndale House, Cambridge.

- Lizenz: **CC BY 4.0**. Das steht im Kopf der Datei selbst und in der README des Repos (`STEPBible/STEPBible-Data`).
- Erlaubt: Teile der Daten in Software oder Veröffentlichungen verwenden, ohne zu fragen; umformatieren; ändern (Änderungen vermerken).
- Pflicht: Namensnennung an **Tyndale House und STEPBible.org** und ein Link auf **https://STEPBible.github.io/STEPBible-Data** als Quelle.
- Bitte der Autoren: die Datei nicht als Ganzes woanders veröffentlichen, damit es eine aktuelle Quelle bleibt. **Die TAGNT-Datei liegt deshalb nicht in diesem Repo**, nur der daraus gezogene Johannes-Auszug (`tools/data/john-*.tsv`, siehe Änderungen).
- Die englischen Glossen sind laut Dateikopf „Based on Berean Study Bible, with permission, as at 1-July-2019 and adapted for this work“. Die Berean Standard Bible ist seit dem 30. April 2023 ausdrücklich gemeinfrei (https://berean.bible/terms.htm).

### Namensnennung (für Credits im Spiel, Beschreibung und README)

> Greek text, morphology and English glosses: STEPBible Data (TAGNT), created by STEPBible.org based on work at Tyndale House, Cambridge, licensed CC BY 4.0. https://STEPBible.github.io/STEPBible-Data. Glosses adapted from the Berean Study Bible (public domain).

## Änderungen an den Quelldaten (CC BY verlangt, dass wir sie vermerken)

- Auszug auf Johannes (`tools/extract_tagnt.py`), Textgrundlage **NA28** (siehe unten), Transliteration und Spalten für Varianten weggelassen.
- Unicode normalisiert (NFC): Hochpunkt und griechisches Fragezeichen werden zu `·` und `;`. Führende/folgende Leerzeichen und Absatzzeichen `¶`, Klammern `[ ]` entfernt.
- Glossen bereinigt (`tools/build_manuscripts.py`): Satzzeichen (`? ! : — ;`), Versnummern anderer Zählung (z. B. `{8.1}`) und Punkte entfernt; Klammerwörter in erlaubte Varianten aufgeteilt.
- Falsche Antworten, Schwierigkeitsstufen und Fundbiome sind **von uns erzeugt**, nicht aus der Quelle.
- Alle Glossen haben den Status `draft` und sind **ungeprüft**.

## Restrisiken, die du kennen solltest

- **Frühere Lizenzangaben von STEPBible.** Ältere Seiten (Confluence-Wiki, Spiegel-Repos) nennen CC BY-NC bzw. CC BY-NC-ND. Das Repo und der Dateikopf, die ich vor mir hatte, sagen CC BY 4.0, und die gelten. Wer sicher gehen will (vor allem bei kommerzieller Nutzung): kurz bei STEPBible@gmail.com bestätigen lassen, die Adresse steht in der README.
- **Griechische Ausgaben darunter.** TAGNT enthält laut Beschreibung „all the words from NA27/28, TR, SBL, TH, Byz, WH, Treg“. Die Rechte an NA28 selbst liegen bei der Deutschen Bibelgesellschaft. Tyndale House bietet die Daten trotzdem unter CC BY 4.0 an, und wir nutzen Wörter und Formen, nicht den Satz der Ausgabe. Ein Restzweifel bleibt; zu den Ausgaben, die selbst CC BY sind (SBLGNT, Tyndale House GNT), siehe die jeweilige Seite.
- **Spanische Glossen** (Marvel Bible Project) stehen in der Datei, wir benutzen sie nicht.
- Wörterbuch-Bücher im Spiel (KI-geschrieben): Wenn sie auf Thayer oder Abbott-Smith (gemeinfrei) beruhen, gilt das dort. Noch nicht geprüft, weil noch nicht geschrieben.

## Textgrundlage Johannes

- **Neue Daten (Joh 1,19–21,25): NA28.** Wörter, die nur in TR oder Byzantinischem Text stehen, fehlen (z. B. Joh 5,3b–4: in NA28 nicht enthalten, der Vers 5,4 fehlt deshalb vollständig; 860 von 861 Versen).
- **Joh 7,53–8,11** (die Ehebrecherin) ist enthalten, wie in NA28 in Doppelklammern. Das ist eine inhaltliche Entscheidung. Möchtest du sie lieber ganz weglassen oder als Randnotiz markieren? Die Daten sind so gebaut, dass wir die Verse später ausblenden können.
- **Pilot Joh 1,1–18 weicht an einer Stelle ab:** dort steht in 1,18 zusätzlich das „ὁ“ vor μονογενής, das nicht in NA28, sondern nur bei Tyndale und TR steht. Eingefroren (P1–P21), also nicht angefasst.
- **NA28 und Tyndale House GNT** gehen in Joh 1,19–21,25 an ca. 100 Versen auseinander (meist Kleinigkeiten wie Artikel, Wortstellung, Schreibung). Du musst irgendwann entscheiden, ob NA28 oder Tyndale die Grundlage sein soll.

## Bekannte Probleme in den Daten

- **μεθερμηνευόμενον** (Joh 1,38 und 1,41; P54 und P60): 17 Buchstaben, die Setzform hat 16 Plätze. Entscheidung offen: Zeile verbreitern, Wort vordrucken oder Wort aus dem Setzen nehmen.
- Falsche Antworten sind automatisch erzeugt; bei Funktionswörtern (Relativpronomen, Konjunktionen) sind sie oft schwach oder einfach unpassend. **Prüfen vor dem Spielen.**
- Einzelne Manuskripte sind sehr kurz (2–3 Wörter, an Kapitelgrenzen und Satzenden) oder lang (bis 37 Wörter, ohne Satzzeichen). Die Obergrenze aus dem Plan (ca. 40) wird eingehalten.
