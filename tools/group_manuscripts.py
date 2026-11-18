"""Teilt alle Manuskripte (P1 bis Pn) nach Schwierigkeit in drei Stufen, jede Stufe in Untergruppen, jede Untergruppe in ein Biom.

Aufruf: python tools/group_manuscripts.py
Ausgabe: tools/data/fragment-groups.json  (maschinenlesbar, Quelle der Wahrheit fuer Fundorte)
         docs/FRAGMENT-GROUPS.md          (lesbare Tabelle)

Verfahren
  1. Schwierigkeit pro Manuskript wie in build_manuscripts.py (Seltenheit, Laenge, Form), aber mit EINER gemeinsamen
     Haeufigkeitsbasis ueber alle Manuskripte. Der Pilot (Joh 1,1-18) wurde damals nur gegen sich selbst gerechnet.
  2. Drei gleich grosse Stufen nach Rang: 1 = leicht (L), 2 = mittel (M), 3 = schwer (S).
  3. Jede Stufe wird in Textreihenfolge in GROUPS gleich grosse Untergruppen geschnitten (zusammenhaengende Textbereiche).
  4. Jede Untergruppe bekommt ein Biom. Die erste Untergruppe jeder Stufe (Anfang des Evangeliums) liegt in der Wueste,
     passend zum Taeufer; die uebrigen sind pro Stufe verschoben verteilt, damit jedes Biom drei verschiedene Textbereiche hat.

Aenderbar: BIOMES (Reihenfolge = Reihenfolge der Untergruppen von vorn nach hinten, Wueste zuerst), GROUPS, SHIFT.
Die Fragment-Dateien (manuscripts.*.json) bleiben unveraendert; ihr altes Feld origin.biome ist ein Zufallswert und gilt nicht mehr.
"""
import json
import os
import sys
from collections import Counter

sys.stdout.reconfigure(encoding="utf-8")
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_manuscripts as B

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FILES = ["tools/data/manuscripts.john1.json", "tools/data/manuscripts.john-rest.json"]
OUT_JSON = os.path.join(ROOT, "tools/data/fragment-groups.json")
OUT_MD = os.path.join(ROOT, "docs/FRAGMENT-GROUPS.md")

# Erstes Biom bekommt jeweils den Textanfang (Wueste: Taeufer). Die Namen sind Minecraft-Biom-Tags.
BIOMES = [
    ("desert", "Wüste", "#minecraft:is_desert"),
    ("plains", "Ebene", "#minecraft:is_plains"),
    ("forest", "Wald", "#minecraft:is_forest"),
    ("taiga", "Taiga", "#minecraft:is_taiga"),
    ("jungle", "Dschungel", "#minecraft:is_jungle"),
    ("savanna", "Savanne", "#minecraft:is_savanna"),
    ("swamp", "Sumpf", "#minecraft:is_swamp"),
]
GROUPS = len(BIOMES)           # Untergruppen je Stufe
SHIFT = {1: 0, 2: 2, 3: 4}     # Verschiebung der Biom-Reihenfolge je Stufe (ohne Wueste)
TIERS = {1: ("L", "Leicht"), 2: ("M", "Mittel"), 3: ("S", "Schwer")}


def load():
    frags = []
    for f in FILES:
        frags += json.load(open(os.path.join(ROOT, f), encoding="utf-8"))["fragments"]
    return sorted(frags, key=lambda f: f["order"])


def difficulty(frags):
    freq = Counter(w["lemma"] for f in frags for w in f["words"])
    score = {}
    for f in frags:
        ws = [{"strongs": w["lemma"], "greek": w["greek"], "morph": w["morph"].split(" + ")[0]} for w in f["words"]]
        score[f["id"]] = sum(B.word_score(w, freq) for w in ws) / len(ws) + 0.004 * len(ws)
    return score


def biome_for(tier, g):
    """g = 0..GROUPS-1 in Textreihenfolge. Gruppe 0 immer Wueste, die uebrigen rotieren je Stufe."""
    if g == 0:
        return BIOMES[0]
    rest = BIOMES[1:]
    return rest[(g - 1 + SHIFT[tier]) % len(rest)]


def short(ref):
    return ref.replace("John ", "")


def build():
    frags = load()
    score = difficulty(frags)
    ranked = sorted(frags, key=lambda f: (score[f["id"]], f["order"]))
    n = len(ranked)
    tier = {f["id"]: 1 + min(2, i * 3 // n) for i, f in enumerate(ranked)}

    groups = []
    for t in (1, 2, 3):
        members = [f for f in frags if tier[f["id"]] == t]     # Textreihenfolge
        m = len(members)
        for g in range(GROUPS):
            part = members[g * m // GROUPS:(g + 1) * m // GROUPS]
            bid, bname, btag = biome_for(t, g)
            letter, tname = TIERS[t]
            groups.append({
                "id": f"{letter}{g + 1}", "tier": t, "tier_name": tname, "index": g + 1,
                "biome": bid, "biome_name": bname, "biome_tag": btag,
                "from": part[0]["ref"], "to": part[-1]["ref"],
                "count": len(part), "words": sum(len(f["words"]) for f in part),
                "fragments": [f["id"] for f in part],
            })
    return frags, score, tier, groups


def check(frags, groups):
    ids = [i for g in groups for i in g["fragments"]]
    assert sorted(ids, key=lambda x: int(x[1:])) == [f["id"] for f in frags], "nicht jedes Manuskript genau einmal"
    for b in BIOMES:
        assert sum(1 for g in groups if g["biome"] == b[0]) == 3, f"{b[0]} hat nicht drei Untergruppen"
    sizes = [g["count"] for g in groups if g["tier"] == 1]
    assert max(sizes) - min(sizes) <= 1


def write(frags, score, tier, groups):
    by_frag = {i: {"tier": g["tier"], "group": g["id"], "biome": g["biome_tag"]} for g in groups for i in g["fragments"]}
    data = {
        "schema": 1,
        "note": "Quelle der Wahrheit fuer Schwierigkeitsstufe und Fundbiom. Das alte Feld origin.biome in manuscripts.*.json ist ein Zufallswert und gilt nicht mehr. Erzeugt von tools/group_manuscripts.py.",
        "biomes": [{"id": b[0], "name": b[1], "tag": b[2]} for b in BIOMES],
        "groups": groups,
        "fragments": by_frag,
    }
    json.dump(data, open(OUT_JSON, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))

    L = ["# Manuskript-Gruppen und Fundbiome", "",
         "Erzeugt von `tools/group_manuscripts.py` (nicht von Hand ändern). Maschinenlesbar: `tools/data/fragment-groups.json`.", "",
         f"**{len(frags)} Manuskripte**, drei Schwierigkeitsstufen (leicht, mittel, schwer) zu je etwa {len(frags) // 3}, jede Stufe in {GROUPS} Untergruppen "
         "(zusammenhängende Textbereiche), jede Untergruppe in genau einem Biom. Jedes Biom hat damit **drei** Untergruppen, je eine pro Stufe, "
         "und zwar aus verschiedenen Textbereichen. Die erste Untergruppe jeder Stufe, der Anfang des Evangeliums, liegt in der Wüste.", "",
         "## Nach Biom", "", "| Biom | Leicht | Mittel | Schwer | Manuskripte |", "|---|---|---|---|---|"]
    for b in BIOMES:
        cells, tot = [], 0
        for t in (1, 2, 3):
            g = next(x for x in groups if x["tier"] == t and x["biome"] == b[0])
            cells.append(f"**{g['id']}**: {short(g['from'])} bis {short(g['to'])} ({g['count']})")
            tot += g["count"]
        L.append(f"| {b[1]} | " + " | ".join(cells) + f" | {tot} |")
    L += ["", "## Alle Untergruppen", "", "| Gruppe | Stufe | Biom | Textbereich | Manuskripte | Wörter |", "|---|---|---|---|---|---|"]
    for g in groups:
        L.append(f"| {g['id']} | {g['tier_name']} | {g['biome_name']} | {short(g['from'])} bis {short(g['to'])} | {g['count']} | {g['words']} |")
    L += ["", "## So funktioniert die Einteilung", "",
          "- **Schwierigkeit** pro Manuskript: Seltenheit der Wörter, Wortlänge und Formenschwierigkeit, gemittelt, dazu ein kleiner Zuschlag für die Länge. "
          "Gemeinsame Häufigkeitsbasis über alle Manuskripte. Das ist eine automatische Schätzung, keine geprüfte Einstufung.",
          "- **Untergruppen** sind zusammenhängende Textbereiche *innerhalb* einer Stufe. Wer ein Biom ausräumt, bekommt also zusammenhängende Passagen in einer Schwierigkeit.",
          "- **Pilot:** P1–P21 sind nach der einheitlichen Rechnung eingestuft. Die Stufe in `manuscripts.john1.json` stammt aus der alten Rechnung (nur gegen sich selbst) und weicht bei einigen ab.",
          "- **Biome ändern:** `BIOMES` oben im Skript (Reihenfolge = Textreihenfolge der Untergruppen, Wüste zuerst) und neu laufen lassen."]
    open(OUT_MD, "w", encoding="utf-8").write("\n".join(L) + "\n")


def main():
    frags, score, tier, groups = build()
    check(frags, groups)
    write(frags, score, tier, groups)
    print(f"{len(frags)} Manuskripte -> {len(groups)} Untergruppen ->", os.path.relpath(OUT_JSON, ROOT), "und", os.path.relpath(OUT_MD, ROOT))
    for g in groups:
        print(f"  {g['id']:<3} {g['tier_name']:<7} {g['biome_name']:<10} {g['from'][5:]:<9} bis {g['to'][5:]:<9} {g['count']:>3} Manuskripte {g['words']:>5} Woerter")


if __name__ == "__main__":
    main()
