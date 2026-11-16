# Stationen und was sie voneinander brauchen

Alle Stationsblöcke (Pult, Presse, Lettern-Schrank, Übersetzungstisch), die sich **waagerecht berühren**, bilden ein Netz
(`StationNetwork` im Client). Jede Oberfläche fragt dort, worauf sie Zugriff hat, und zeigt nur diese Teile. Ohne Nachbar
fehlt der Teil, er wird nicht mit Platzhaltern gefüllt.

| Station | Rechtsklick | Eigener Zustand | Braucht von Nachbarn | Ohne Nachbar |
|---|---|---|---|---|
| Scribe's Lectern | Fragment auflegen / Ansicht öffnen | das aufgelegte Fragment | nichts | zeigt nur das Fragment (oder leer) |
| Printing Press | Druck-Oberfläche | Setzform (im Schrank gespeichert) | Lettern-Schrank für Lettern, Tinte, Papier; Pult für ein Fragment | nur Setzform, Ablage, Kurbel, Hebel; ohne Pult freier Druck |
| Letter Cabinet | Druck-Oberfläche | Vorrat an Letter Types, Tinte, Papier, gesetzte Lettern | Presse zum Drucken | nur Buchstabenbox, Fass, Papiervorrat |
| Translator's Desk | Übersetzung | aufgelegter Bogen, Tinte, Papier, Tablets | Pult mit demselben Fragment für das Original | nur der gedruckte Bogen und das Blatt; keine Manuskript-Wörter |
| Minting Table | Menü | nichts (Eingabeslot im Menü) | nichts | alleinstehend |

## Was jede Oberfläche zeigt

- **Presse mit Cabinet:** Setzform + Buchstabenbox, Fass, Papiervorrat. Mit Pult kommt links das Fragment dazu (geführter Druck), ohne Pult druckt man frei.
- **Übersetzung:** Der Tisch zeigt das Manuskript auf der linken Seite nur, wenn ein Pult mit genau diesem Fragment angrenzt. Das Pult ändert nichts am Bogen selbst: Der Bogen kommt aus der Presse als Item.
- **Vorräte der Station:** Presse bezieht sie aus dem Cabinet, der Tisch hat eigene (Tinte, Papier, Tablets).

## Kosten

| Aktion | Verbraucht |
|---|---|
| Bogen drucken | 1 Papier, Tinte fürs Einfärben, Verschleiß an Lettern |
| Übersetzung schreiben (Feder) | 1 Tinte pro Schreiben, 1 Tablet je Wort **mit Plättchen** (auch wenn falsch) |
| Wort von Hand getippt | nichts außer der Tinte fürs Schreiben (Groß-/Kleinschreibung egal, Rand wird gekürzt) |
| Übersetzung fertigstellen | 1 Papier, der Bogen wird zum Translated Sheet |

## Technik

- Client: `StationNetwork.scan(level, pos)` liefert Pult, Fragment, Cabinet-Position, ob Presse oder Tisch da sind.
  `ClientHooks` baut daraus die passende Oberfläche, `Stations` ist die abgespeckte Form für die Screens.
- Server: Alle Vorräte liegen in Block-Entities (`CabinetBlockEntity`, `DeskBlockEntity`). Der Client meldet nur, was er verbraucht hat; der Server prüft jede Meldung (`ModNetwork`).
- Neue Station hinzufügen: Block + Block-Entity, in `StationNetwork` einen Zweig für den Block, in dieser Tabelle eintragen.
