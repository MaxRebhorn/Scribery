"""Macht aus der Biom-Zuordnung (tools/data/fragment-biomes.json) die Fundart-Pools fuer die Loot-Tabellen.

Aufruf: python tools/export_find_pools.py
Ausgabe: tools/data/find-pools.json  { "pools": { "sand": ["P1", ...], "mud": [...], ... }, "tiers": { "P1": 1, ... } }
tiers: Schwierigkeit je Manuskript, 1 leicht, 2 mittel, 3 schwer (Tooltip im Spiel).

Der Fundblock bestimmt, welches Manuskript herauskommt und wie es aussieht (docs/DESIGN-FINDS.md). Die Zuordnung der
elf Biome zu den fuenf Fundarten ist ein Vorschlag, aendern: BIOME_TO_FIND. Die Reihenfolge der IDs ist die des Evangeliums.
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "tools", "data")

BIOME_TO_FIND = {
    "desert": "sand", "savanna": "sand", "badlands": "sand",
    "river": "mud", "beach": "mud", "swamp": "mud",
    "forest": "ashes", "jungle": "ashes",
    "taiga": "ice", "mountain": "ice",
    "plains": "shards",
}


def main():
    src = json.load(open(os.path.join(DATA, "fragment-biomes.json"), encoding="utf-8"))
    pools = {k: [] for k in ("sand", "mud", "ashes", "ice", "shards")}
    tiers = {}
    unknown = set()
    for mid, info in src["fragments"].items():
        find = BIOME_TO_FIND.get(info["biome"])
        if find is None:
            unknown.add(info["biome"])
            continue
        pools[find].append(mid)
        tiers[mid] = info["tier"]
    if unknown:
        raise SystemExit("Biome ohne Fundart: " + ", ".join(sorted(unknown)))
    out = {"_comment": "Erzeugt von tools/export_find_pools.py aus fragment-biomes.json. Nicht von Hand aendern.", "pools": pools, "tiers": tiers}
    with open(os.path.join(DATA, "find-pools.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(out, f, ensure_ascii=False, separators=(",", ":"))
        f.write("\n")
    print({k: len(v) for k, v in pools.items()}, "gesamt", sum(len(v) for v in pools.values()))


if __name__ == "__main__":
    main()
