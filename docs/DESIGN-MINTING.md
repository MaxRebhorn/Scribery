# Design: Minting Table mit Befeuerung

**Entscheidung 19.11.2026:** Es wird nichts geschmolzen. Der Ofen muss brennen, damit man den Tisch benutzen kann; sonst bleibt alles wie im bisherigen Menü (Nuggets rein, Letter Types raus). Gebaut: Zustand `lit`, `MintBlockEntity` mit Restbrenndauer, Brennstoff per Rechtsklick (normale Ofen-Brennwerte, Papyrusfetzen 100 Ticks über `data/neoforge/data_maps/item/furnace_fuels.json`), Modell `minting_table_off` ohne Flamme, Licht 7 nur bei `lit`. Brennstoffplatz im Menü (Brennstoff wird erst verbraucht, wenn man Letter Types entnimmt und der Tisch kalt ist; Rechtsklick mit Brennstoff zündet ihn weiterhin direkt an). Töne beim Entnehmen: Blubbern, dann zweimal Amboss hoch (Werte nach Gehör im Spiel anpassen). Der Rest dieser Datei ist der frühere Entwurf mit Schmelzen und gilt nicht mehr.

# (Früherer Entwurf) Minting Table mit Befeuerung und Schmelzen

Stand 19.11.2026. Das **Aussehen** ist umgesetzt (Steinblock mit offenem Ofenmaul vorn, Flamme schlägt heraus, Tiegel oben, kleine Gussform, kleiner Amboss, siehe `art/gen_stations.py`). Die **Funktion** ist noch die alte: Rechtsklick öffnet das Stonecutter-Menü (`MintMenu`), Eisennuggets werden in Letter Types umgewandelt (1 Nugget → 2 Letter Types).

## Gewünschter Ablauf

1. Der Tisch wird **befeuert** (Kohle, Holzkohle, Holz, was Vanilla als Brennstoff kennt). Solange er brennt, ist er „an“: Flamme sichtbar, Licht, Funken.
2. Nur während er brennt, lassen sich **Eisennuggets einlegen und schmelzen**. Das dauert eine Weile.
3. Aus dem **geschmolzenen** Eisen wird in der Gussform ein Letter Type gegossen und ausgeformt (die Auswahl der Buchstaben bleibt wie im bisherigen Menü).
4. **Ton:** erst ein Gieß-/Blubbergeräusch („Bulululut“), dann zwei kurze Hammerschläge („Bing-Bing“).

## Was dafür gebaut werden müsste

- **Block-Entity** (`MintBlockEntity`): Restbrenndauer, Schmelzfortschritt, Menge geschmolzenes Eisen, eingelegte Nuggets. Nur ein Ticker, solange der Tisch brennt, danach nichts mehr.
- **Blockzustand `lit`:** ein Modell für an und eins für aus. Die Flamme und die Lichtstufe (jetzt immer 7) hängen am Zustand. Die Teilchen in `MintBlock.animateTick` nur im Zustand an.
- **Menü:** Slots für Brennstoff und Nuggets, Anzeige für Brenndauer und Schmelzfortschritt wie beim Ofen. Das Buchstaben-Auswählen bleibt wie jetzt, verbraucht aber geschmolzenes Eisen statt Nuggets.
- **Server prüft alles** (wie bei den anderen Stationen), der Client bekommt nur den Zustand zur Anzeige.
- **Töne:** Beim Gießen ein Blubbern (Kandidat: Lava-Plopp oder das Leeren eines Lavaeimers), danach zwei Schläge (Kandidat: Amboss-Geräusch mit hoher Tonlage). Welche Vanilla-Töne gut klingen, kann ich hier nicht hören. Das muss im Spiel probiert werden.

## Wo ich Reibung sehe

- **Wartezeit:** Wer 100 Buchstaben braucht, will nicht ständig warten. Das Schmelzen sollte in Mengen gehen (zum Beispiel 9 Nuggets in wenigen Sekunden) und der Tisch einen kleinen Vorrat an Geschmolzenem halten, damit man danach schnell gießen kann.
- **Brennstoff:** Er sollte billig sein (jeder Vanilla-Brennstoff zählt, Holzkohle reicht für viele Buchstaben). Sonst wird er ein Ärgernis und kein Hingucker.
- **Brennen ohne Spieler:** Der Tisch darf nicht endlos weiterbrennen, wenn man weggeht. Er geht nach der Brenndauer aus.
- **Inhalt:** Letter Types sind im Spiel aus Blei gedacht, geschmolzen wird aber Eisen. Das ist eine Frage der Geschichte, nicht der Technik. Ich würde es so lassen, solange es keinen Blei-Gegenstand gibt.
- **Aufwand:** Das ist die größte Änderung an der Station: Block-Entity, Zustand, Menü, Netzwerk, Töne, Tests. Sinnvoll erst, wenn Aussehen und Verbrauch (Nuggets pro Letter) stehen.

## Offen

- Wie lange brennt ein Brennstoff, wie lange schmilzt eine Portion?
- Was passiert mit eingelegten Nuggets, wenn der Tisch ausgeht (zurückgeben oder warten)?
- Zählt der Hammer-Ton als eigener Schritt (Klick, Fortschritt) oder nur als Effekt beim Gießen?
