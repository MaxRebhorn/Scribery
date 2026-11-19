"""Erzeugt die Loot-Tabellen der Fundbloecke (mod/src/main/resources/data/scribery/loot_table/blocks/*.json).

Aufruf: python tools/gen_loot_tables.py

Regeln (docs/DESIGN-FINDS.md):
- Ein Suspicious-Block (Sand, Mud, Ashes) gibt beim Abbauen den NORMALEN Block zurueck (Sand, Mud, Ashes), nicht sich
  selbst, plus mit einer Chance ein Manuskript (es kann also auch keins kommen).
- Der Eisblock mit Fragment gibt nur das Manuskript, das darin steckt, nicht sich selbst.
- Welches Manuskript und wie es aussieht, bestimmt die Fundart des Blocks (Loot-Funktion scribery:random_manuscript).
- Papyrusfetzen sind Beifund (nur Brennstoff). Der Scherbenhaufen gibt Scherben und manchmal ein Ostrakon.
- Die Amphore bleibt nur mit Behutsamkeit (Silk Touch) heil, sonst zerbricht sie in Scherben, dazu Ostraka,
  Manuskripte und Papyrusfetzen mit Chance.
Alle Zahlen sind Schaetzungen und muessen im Spiel abgestimmt werden.
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "mod/src/main/resources/data/scribery/loot_table/blocks")

SURVIVES = {"condition": "minecraft:survives_explosion"}
SILK_TOUCH = {"condition": "minecraft:match_tool", "predicate": {"predicates": {
    "minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}


def chance(p):
    return {"condition": "minecraft:random_chance", "chance": p}


def pool(entry, *conditions):
    return {"rolls": 1, "bonus_rolls": 0, "conditions": [SURVIVES, *conditions], "entries": [entry]}


def item(name, count=None):
    e = {"type": "minecraft:item", "name": name}
    if count:
        e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": count[0], "max": count[1]}}]
    return e


def joke_ostrakon():
    """Ein beschriebener Scherben: der Witz wird beim Fallenlassen gewuerfelt (scribery:random_joke)."""
    return {"type": "minecraft:item", "name": "scribery:ostrakon", "functions": [{"function": "scribery:random_joke"}]}


def manuscript(find):
    return {"type": "minecraft:item", "name": "scribery:manuscript_fragment",
            "functions": [{"function": "scribery:random_manuscript", "find": find}]}


def table(name, pools):
    return {"type": "minecraft:block", "pools": pools, "random_sequence": f"scribery:blocks/{name}"}


TABLES = {
    # Fundboeden: der normale Block, 40 % ein Manuskript, manchmal Papyrusfetzen
    "suspicious_sand": [pool(item("minecraft:sand")), pool(manuscript("sand"), chance(0.40)),
                        pool(item("scribery:papyrus_scrap", (1, 2)), chance(0.35))],
    "suspicious_mud": [pool(item("minecraft:mud")), pool(manuscript("mud"), chance(0.40)),
                       pool(item("scribery:papyrus_scrap", (1, 2)), chance(0.35))],
    "suspicious_ashes": [pool(item("scribery:ashes")), pool(manuscript("ashes"), chance(0.40)),
                         pool(item("scribery:papyrus_scrap", (1, 2)), chance(0.35))],
    # Eisblock mit eingefrorenem Fragment: nur das Manuskript, das darin steckt (der Block selbst geht verloren)
    "frozen_fragment_block": [pool(manuscript("ice"))],
    # Scherbenhaufen: Block zurueck, 1 bis 3 Scherben, selten ein Ostrakon, manchmal ein Manuskript
    "shard_pile": [pool(item("scribery:shard_pile")), pool(item("scribery:amphora_shard", (1, 3))),
                   pool(joke_ostrakon(), chance(0.12)), pool(manuscript("shards"), chance(0.25))],
    # Amphore: mit Silk Touch heil, sonst Scherben, mit Chance Ostrakon, Manuskript und Papyrusfetzen
    "amphora": [pool(item("scribery:amphora"), SILK_TOUCH),
                pool(item("scribery:amphora_shard", (2, 4)), {"condition": "minecraft:inverted", "term": SILK_TOUCH}),
                pool(joke_ostrakon(), {"condition": "minecraft:inverted", "term": SILK_TOUCH}, chance(0.25)),
                pool(manuscript("shards"), {"condition": "minecraft:inverted", "term": SILK_TOUCH}, chance(0.30)),
                pool(item("scribery:papyrus_scrap", (1, 2)), {"condition": "minecraft:inverted", "term": SILK_TOUCH}, chance(0.25))],
}


def broken_amphora(name, shards, ostrakon, manuscript_chance, papyrus):
    """Zerbrochene Amphora-Variante: mit Silk Touch bleibt sie, sonst Scherben und mit Chance Ostrakon, Manuskript, Papyrus."""
    not_silk = {"condition": "minecraft:inverted", "term": SILK_TOUCH}
    return [pool(item(f"scribery:{name}"), SILK_TOUCH),
            pool(item("scribery:amphora_shard", shards), not_silk),
            pool(joke_ostrakon(), not_silk, chance(ostrakon)),
            pool(manuscript("shards"), not_silk, chance(manuscript_chance)),
            pool(item("scribery:papyrus_scrap", (1, 2)), not_silk, chance(papyrus))]


# Die zerbrochenen Varianten (art/gen_amphora_variants.py): je kaputter, desto weniger Inhalt
TABLES.update({
    "amphora_lying": broken_amphora("amphora_lying", (1, 3), 0.22, 0.28, 0.22),
    "amphora_lying_broken": broken_amphora("amphora_lying_broken", (1, 3), 0.20, 0.22, 0.20),
    "amphora_broken_neck": broken_amphora("amphora_broken_neck", (1, 3), 0.20, 0.22, 0.20),
    "amphora_half": broken_amphora("amphora_half", (1, 2), 0.15, 0.15, 0.15),
    "amphora_shards": broken_amphora("amphora_shards", (1, 2), 0.12, 0.10, 0.10),
    "amphora_tilted": broken_amphora("amphora_tilted", (2, 3), 0.22, 0.28, 0.22),
})


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, pools in TABLES.items():
        with open(os.path.join(OUT, name + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(table(name, pools), f, indent=2)
            f.write("\n")
    print("ok", len(TABLES), "Tabellen")


if __name__ == "__main__":
    main()
