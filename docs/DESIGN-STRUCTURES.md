# Entwurf: Strukturen und Fundorte (ENTWURF)

Stand 18.11.2026. Nichts davon ist gebaut außer dem Debug-Wrack (`/scribery_wreck`). Gesammelt aus der Brainstorming-Runde; „entschieden“ heißt: so gewollt, aber noch nicht umgesetzt.

## Prinzip

- Jede Struktur gehört zu einer **Fundart** (Sand, Schlamm, Asche, Eis, Scherben, siehe `DESIGN-FINDS.md`) und liefert ihre Funde (Amphoren, Ostraka, Manuskripte).
- Mögliche Erweiterung: Eine Struktur gibt Manuskripte **aus der Geschichte, die zu ihr passt**, statt zufälliger (Fischerboot am Strand → Fischfang-Kapitel).
- Jede Struktur entsteht zuerst als **Debug-Befehl** mit Code (wie `DebugWreck`), damit Form und Loot getestet werden können. Erst danach kommt die Welt-Erzeugung (wo, wie oft, in welchen Biomen).
- Große Gebäude baut man am besten im Creative-Modus und speichert sie mit dem Structure Block; sie werden dann eingebunden.

## Entschieden

### Wracks (Debug-Befehl vorhanden)
- Schiffswrack aufrecht oder auf der Seite, Ladung von 1 bis 10 Amphoren (`/scribery_wreck [1-10] [upright|side]`).
- Später auch auf dem Grund von Ozeanen, großen Seen und in **gefluteten Höhlen**, außerdem als gestrandetes Wrack in der Wüste.
- Es gibt nur Amphoren; Amphoren stapeln sich nie direkt, darunter liegt ein Scherbenhaufen.

### Eingefrorene Karawane (Taiga, Berge, Fundart Eis)
Wagen, Kisten und Tragetiere-Reste im Eis, die Funde stecken in Eisblöcken mit Fragment.

### Soldatengrab (Sumpf, Fundart Schlamm)
Ein flaches Grab im Moor: Holzreste, verrostete Waffe, Schildrest, ein Helm. Im Grab liegt ein kleines **Amulett mit winziger Schriftrolle**, auf der Bibelstellen stehen (Vorbild: historische Funde von Amuletten mit Bibeltext). Dafür braucht es ein neues Item (Amulett-Kapsel), aus dem man ein Manuskript bekommt.

### Katakomben-Kirche (unter der Erde, in mehreren Versionen je Landschaft)
Die frühen Christen haben sich teils in Katakomben getroffen und dort Gottesdienst gefeiert. Die Anlage soll **als Kirche erkennbar** sein, siehe „Erkennbarkeit“. **Zwei Hauptversionen** mit verschiedenem Gestein: eine für die **Wüste** (Sandstein) und eine für **normale Biome** (je nach Gegend Stein, Tuff oder Lehmziegel). Für das **Eisbiom** vielleicht eine dritte, ähnliche Fassung (noch offen). Der Eingang liegt versteckt (Treppe in einer Mulde, Hinweis durch Suspicious-Blöcke).

### Verschüttete Häuser (kurz unter der Oberfläche)
Eingestürzte Häuser, die nur wenige Blöcke tief unter der Erde liegen. An der Oberfläche sitzen **Suspicious-Blöcke als Hinweis**, dass darunter etwas ist; wer gräbt, findet ein Haus mit Boden, Feuerstelle, Amphoren. Technik: Strukturen können sich in das Gelände eingraben (`terrain_adaptation: bury`), die Hinweis-Blöcke setzt ein eigenes Feature darüber (ähnlich wie bei den Pfadruinen im Spiel).

### Verworfen
- **Verlassene Klöster und Bibliotheksruinen:** zu langweilig.

## Vorschläge (noch nicht bestätigt)

- Karawanen-Lager in der Wüste mit halb versandeten Zelten und einer Zisterne.
- Fischerboot-Wrack am Strand mit Netzen und Körben.
- Zerfallene Furt oder Brücke am Fluss, Pfahlhütte im Sumpf mit Papyrusbündeln.
- Abgebrannte Schreibstube (Asche) mit heilem Kellergewölbe, Olivenhain mit Ölpresse.
- Berghöhle mit Reihen versiegelter Tonkrüge, Einsiedler-Hütte mit Ostrakon-Haufen.
- Töpferwerkstatt mit Brennofen und Scherbenhaufen (passt zum Scherbenhaufen-Block), Villenruine mit Mosaik und Amphorenkeller.
- Archäologische Grabungsstelle (Graben mit Suspicious Sand).

## Historische Grundlage: frühe Kirchen und Katakomben (Recherche 18.11.2026)

Zusammengefasst aus Nachschlagewerken und Fachartikeln (Quellen unten). Die Forschung ist an manchen Stellen im Fluss, das ist vermerkt.

**Die Katakomben waren vor allem Friedhöfe, keine Geheimkirchen.** Die verbreitete Vorstellung, die ersten Christen hätten sich dort heimlich zum regelmäßigen Gottesdienst getroffen, gilt als nicht belegt. Dafür waren es zu viele Menschen, und die Gänge zu eng. Belegt sind Beerdigungen, **Gedenkmahle für Verstorbene und Märtyrer** (Refrigerium) und Feiern der Eucharistie zu Beerdigungen und Gedenktagen. Die größeren Räume dienten nach einer Lesart (Michael White) den Totenmahlen, teils mit Bänken an den Wänden. Krüge, Brot und Becher auf Bildern und Funden spielen auf diese Mahle an.

**Der normale Gottesdienst fand in Privathäusern statt** (Hauskirchen). Das einzige gut erhaltene Beispiel ist Dura-Europos (um 240 n. Chr.): ein gewöhnliches Haus mit Innenhof, in dem eine Trennwand zwischen zwei Räumen für einen großen Versammlungsraum entfernt wurde, mit einem **erhöhten Podest an einem Ende**, dazu ein kleiner Taufraum. Neuere Studien (2024) bezweifeln, dass es ein reines Kirchengebäude war: Es sei eher ein christliches Haus gewesen, das auch Wohnhaus blieb. Hier ist die Forschung nicht einig.

**Innenraum:** Die Gemeinde **stand**, Sitzbänke gibt es erst im späten Mittelalter. Der Altar war zunächst ein **Tisch**, an dem der Vorsteher dem Volk gegenüberstand. Feste Altäre mit Reliquien kamen erst im 5. Jahrhundert, in den Katakomben dienten **Grabplatten über Märtyrergräbern** als Tisch.

**Aufbau der Katakomben:** Lange schmale **Gänge** mit **Loculi** (waagerechte Grabnischen, übereinander gestapelt, mit Platte verschlossen), dazwischen **Arkosolien** (bogenförmige Nischen mit einer Grabplatte darunter) und **Cubicula** (kleine Familiengrabkammern, oft ausgemalt). Licht kam über Schächte von der Oberfläche (Luminaria) und Öllampen.

**Taufbecken:** Das älteste bekannte (Dura-Europos) ist ein rechteckiges Becken in einem kleinen Raum, tief genug zum Eintauchen, mit Stufen hinunter. Auch frühe Beispiele in Katakomben sind rechteckig (San Ponziano in Rom); achteckige Becken sind später.

### Was das für unsere Katakomben-Kirche bedeutet
Unser erster Entwurf (ein langes, breites Schiff mit Altarnische) ist eher eine **spätere Basilika im Fels** als eine frühe Katakombe. Historisch näher wäre:
- **Ein Netz aus schmalen Gängen** mit vielen Loculi in den Wänden, dazwischen Arkosolien und Cubicula.
- **Eine größere Kammer** für das Gedenkmahl und die Eucharistie: ein **Altartisch aus glattem Stein**, am besten **auf oder an einem Arkosolgrab** (Märtyrergrab als Tisch), davor Platz zum Stehen. Keine Bänke in der Kirche selbst; in einer **Mahlkammer** wären Wandbänke historisch belegt (nicht in der Kirche).
- **Rechteckiges Taufbecken mit Stufen** in einem kleinen Raum (bei uns trocken).
- **Lichtschächte** von der Oberfläche und Öllampen in Nischen.
- Amphoren und Brot als Fund, weil sie die Gedenkmahle andeuten.
Offene Entscheidung: Soll es eine echte **Katakombe mit Gedenkraum** werden (Gänge, mehrere Grabkammern, ein Raum für Mahl und Eucharistie) oder bei der **Hauskirche** (ein Haus über oder neben der Katakombe) bleiben? Beides ist belegt.

### Quellen
- [Dura-Europos church (Wikipedia)](https://en.wikipedia.org/wiki/Dura-Europos_church) und [Debating the domus ecclesiae at Dura-Europos (Universität Manchester)](https://research.manchester.ac.uk/en/publications/debating-the-domus-ecclesiae-at-dura-europos-the-christian-buildi/)
- [The Structure of the Catacombs (International Catacomb Society)](https://www.catacombsociety.org/vom/catacombs_structure.html) und [Glossar der Katakomben](https://catacombsociety.org/ics-resources/glossary/)
- [Catacomb (Britannica)](https://www.britannica.com/topic/catacomb), [Refrigerium (Wikipedia)](https://en.wikipedia.org/wiki/Refrigerium), [Catacombs: Archives of Imagery](https://www.catacombsociety.org/vom/catacombs_arch_image.html)
- [Baptisteries and Baptismal Fonts (Encyclopedia.com)](https://www.encyclopedia.com/religion/encyclopedias-almanacs-transcripts-and-maps/baptisteries-and-baptismal-fonts) und [Dura-Europos Christian Building (Yale)](https://duraeuropos.artgallery.yale.edu/christian-building)
- [What did the worship space look like? (Christian History Institute)](https://christianhistoryinstitute.org/magazine/article/what-did-the-worship-space-look-like)

## Erkennbarkeit der Katakomben-Kirche

Ohne Hinweise ist ein Raum im Fels nur ein Raum. Entschieden (18.11.2026):
- **Altartisch** am Ende des Mittelraums, **aus glattem Stein** (Smooth Stone); in der Wüste-Version aus glattem Sandstein.
- **Keine Wandbilder.** Die frühchristlichen Symbole (Fisch, Anker, Taube) entfallen.
- **Grabnischen** (Loculi) in den Seitenwänden, mit verschlossenen Platten. Das ist eine Idee, die gefällt.
- Ein **ausgetrocknetes Taufbecken** in einem Nebenraum: ein steinernes Becken ohne Wasser.
- **Keine Bänke und keine Sitzplätze:** die frühen Kirchen hatten keine, die Gemeinde stand (Bänke kamen erst viel später, selbst im Mittelalter stand man noch).
- **Grundriss einer Kirche:** ein langer, freier Mittelraum (Schiff), am Ende die Nische (Apsis) mit dem Altartisch.

Als Vorschlag noch offen:
- **Lampen:** kleine Öllampen in Wandnischen (die Lampe von Kapitel 1).
- Ein **Gemeinschaftstisch** (Liebesmahl) mit Krügen und ein paar Amphoren als Fund.
- Ostraka mit einfachen Inschriften als Fund (kein Witz-Ostrakon).

Noch zu klären: Wie viel Dunkelheit und ob die Loculi Inhalt haben dürfen (ohne Knochen).

### Eisbiom: der erfrorene Wanderer (statt einer Höhle)
Für das Eisbiom keine Katakombe, sondern die Spur eines Wanderers, der unterwegs erfroren ist: eine kleine Fundstelle im Schnee mit **gefrorenen Dingen**, die dort liegen. Die Manuskripte stecken in Eisblöcken mit Fragment (Fundart Eis), sodass sie wie Gepäck im Eis eingefroren wirken. Dazu braucht es **Dinge, die herumliegen können** (Dekoration), zum Beispiel:
- ein halb eingeschneiter Wanderstab, ein Bündel oder Sack, eine Decke (Teppich), ein erloschenes Lagerfeuer,
- eine Amphore oder ein Wasserkrug, gefroren (Eis darum),
- ein paar Ostraka oder Papyrusfetzen im Schnee, Fußspuren (Schneeschichten),
- Steinhaufen (Cairn) als Wegmarke.
Das lässt sich mit der **eingefrorenen Karawane** verbinden: die Karawane ist die große Version, der Wanderer die kleine, häufige.
