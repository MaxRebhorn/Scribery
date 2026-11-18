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
