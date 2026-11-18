"""Ordnet jedem Manuskript ein Fundbiom zu: THEMATISCH nach Geschichte (oder einzelner Seite), nicht nach Schwierigkeit.

Aufruf: python tools/assign_biomes.py
Eingabe: tools/data/john-biomes.tsv  (von Hand pflegbar: Geschichte oder Seite -> Biom, mit Begruendung)
Ausgabe: tools/data/fragment-biomes.json  (maschinenlesbar, Quelle der Wahrheit fuer Fundorte)
         docs/FRAGMENT-BIOMES.md          (Uebersicht)

Ein Biom liefert ganze Geschichten. Eine Zeile pgNNN in john-biomes.tsv ueberschreibt die Geschichtenzeile fuer diese eine Seite,
so kann ein Biom auch nur eine Seite einer Geschichte liefern.
Die Schwierigkeit (leicht/mittel/schwer) bleibt als Eigenschaft jedes Manuskripts erhalten (einheitliche Rechnung ueber alle Manuskripte),
bestimmt aber nicht mehr den Fundort.
"""
import json
import os
import sys
from collections import Counter, defaultdict

sys.stdout.reconfigure(encoding="utf-8")
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_manuscripts as B
import structure_john as SJ

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MAP = os.path.join(ROOT, "tools/data/john-biomes.tsv")
OUT_JSON = os.path.join(ROOT, "tools/data/fragment-biomes.json")
OUT_MD = os.path.join(ROOT, "docs/FRAGMENT-BIOMES.md")

# id, Name, Biom-Tag. NICHT gegen Minecraft 1.21.1 / NeoForge geprueft: is_desert, is_plains, is_swamp gibt es meines Wissens nicht in Vanilla,
# dafuer als gemeinsame Tags im Namensraum c (NeoForge). Vor dem Einbinden pruefen.
BIOMES = [
    ("desert", "Wüste", "#c:is_desert"),
    ("river", "Fluss", "#minecraft:is_river"),
    ("beach", "Strand/See", "#minecraft:is_beach"),
    ("plains", "Ebene", "#c:is_plains"),
    ("savanna", "Savanne", "#minecraft:is_savanna"),
    ("forest", "Wald", "#minecraft:is_forest"),
    ("jungle", "Dschungel", "#minecraft:is_jungle"),
    ("swamp", "Sumpf", "#c:is_swamp"),
    ("taiga", "Taiga", "#minecraft:is_taiga"),
    ("mountain", "Gebirge", "#minecraft:is_mountain"),
    ("badlands", "Tonhügel", "#minecraft:is_badlands"),
]
TIERS = {1: "leicht", 2: "mittel", 3: "schwer"}


def read_map(stories, pages):
    ids = {b[0] for b in BIOMES}
    story_ids = {s["id"] for s in stories}
    page_ids = {p["id"] for p in pages}
    by_story, by_page, why = {}, {}, {}
    for line in open(MAP, encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        unit, biome, reason = (line.rstrip("\n").split("\t") + [""])[:3]
        if biome not in ids:
            raise SystemExit(f"{unit}: unbekanntes Biom {biome}")
        if unit in story_ids:
            by_story[unit] = biome
        elif unit in page_ids:
            by_page[unit] = biome
        else:
            raise SystemExit(f"{unit}: weder Geschichte noch Seite")
        why[unit] = reason
    missing = story_ids - set(by_story)
    if missing:
        raise SystemExit(f"Geschichten ohne Biom: {sorted(missing)}")
    return by_story, by_page, why


def difficulty_tiers(frags):
    freq = Counter(w["lemma"] for f in frags for w in f["words"])
    score = {}
    for f in frags:
        ws = [{"strongs": w["lemma"], "greek": w["greek"], "morph": w["morph"].split(" + ")[0]} for w in f["words"]]
        score[f["id"]] = sum(B.word_score(w, freq) for w in ws) / len(ws) + 0.004 * len(ws)
    ranked = sorted(frags, key=lambda f: (score[f["id"]], f["order"]))
    n = len(ranked)
    return {f["id"]: 1 + min(2, i * 3 // n) for i, f in enumerate(ranked)}


def main():
    words, order, counts, stories, pages = SJ.build_a()
    frags = SJ.load_fragments()
    info = SJ.build_b(words, stories, pages, frags)
    tier = difficulty_tiers(frags)
    by_story, by_page, why = read_map(stories, pages)
    story_of = {p["id"]: p["story"] for p in pages}
    page_biome = {p["id"]: by_page.get(p["id"], by_story[p["story"]]) for p in pages}

    fr = {fid: {"biome": page_biome[i["page"]], "tier": tier[fid], "story": i["story"], "page": i["page"]} for fid, i in info.items()}
    bio = {b[0]: {"id": b[0], "name": b[1], "tag": b[2], "stories": [], "pages": [], "fragments": 0, "words": 0, "tiers": {1: 0, 2: 0, 3: 0}} for b in BIOMES}
    for p in pages:
        b = bio[page_biome[p["id"]]]
        b["pages"].append(p["id"])
        if p["story"] not in b["stories"]:
            b["stories"].append(p["story"])
    for fid, f in fr.items():
        b = bio[f["biome"]]
        b["fragments"] += 1
        b["words"] += info[fid]["words"]
        b["tiers"][f["tier"]] += 1
    # Pruefung
    assert len(fr) == len(frags)
    assert sum(b["fragments"] for b in bio.values()) == len(frags)
    assert all(bio[b[0]]["fragments"] > 0 for b in BIOMES), "Biom ohne Manuskripte"

    data = {
        "schema": 2,
        "note": "Quelle der Wahrheit fuer Fundbiome und Schwierigkeitsstufen. Das alte Feld origin.biome in manuscripts.*.json ist ein Zufallswert und gilt nicht mehr. Erzeugt von tools/assign_biomes.py aus tools/data/john-biomes.tsv.",
        "tiers": {str(k): v for k, v in TIERS.items()},
        "biomes": list(bio.values()),
        "stories": {s["id"]: by_story[s["id"]] for s in stories},
        "page_overrides": by_page,
        "fragments": fr,
    }
    json.dump(data, open(OUT_JSON, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))

    title = {s["id"]: s for s in stories}
    pg = {p["id"]: p for p in pages}

    def rng(s):
        return f"{SJ.fmt(s['from'])}–{SJ.fmt(s['to'])}"

    L = ["# Fundbiome der Manuskripte", "",
         "Erzeugt von `tools/assign_biomes.py` (nicht von Hand ändern; die Zuordnung steht in `tools/data/john-biomes.tsv`). "
         "Maschinenlesbar: `tools/data/fragment-biomes.json`.", "",
         f"**{len(frags)} Manuskripte in {len(BIOMES)} Biomen.** Die Zuordnung ist **thematisch**: Ein Biom liefert ganze Geschichten (Ort, Bild oder Stimmung passen), "
         "bei Bedarf auch nur eine einzelne Seite. Wer in einem Biom sammelt, bekommt also zusammenhängende Passagen, nicht Streutreffer. "
         "Die Schwierigkeit (leicht, mittel, schwer) ist eine Eigenschaft jedes Manuskripts geblieben und bestimmt nicht mehr den Fundort.", "",
         "## Überblick", "", "| Biom | Tag | Geschichten | Seiten | Manuskripte | davon leicht / mittel / schwer |", "|---|---|---|---|---|---|"]
    for b in bio.values():
        t = b["tiers"]
        L.append(f"| {b['name']} | `{b['tag']}` | {len(b['stories'])} | {len(b['pages'])} | {b['fragments']} | {t[1]} / {t[2]} / {t[3]} |")
    L += ["", "## Was in welchem Biom liegt", ""]
    for b in bio.values():
        L.append(f"### {b['name']} ({b['fragments']} Manuskripte)")
        L.append("")
        for sid in b["stories"]:
            s = title[sid]
            mine = [p for p in s["pages"] if page_biome[p] == b["id"]]
            part = "" if len(mine) == len(s["pages"]) else f" – nur Seite {', '.join(mine)}"
            reason = "; ".join(dict.fromkeys(why.get(x, "") for x in [p for p in mine if p in by_page] or [sid]))
            L.append(f"- **{sid}** {s['title']} ({rng(s)}){part} – {reason}".rstrip(" –"))
        L.append("")
    # Biome pro Kapitel
    chap = defaultdict(set)
    for fid, i in info.items():
        pass
    for p in pages:
        for v in p["verses"]:
            chap[v[0]].add(page_biome[p["id"]])
    names = {b[0]: b[1] for b in BIOMES}
    L += ["## Welche Biome braucht ein Kapitel?", "", "| Kapitel | Biome |", "|---|---|"]
    for c in sorted(chap):
        L.append(f"| {c} | {', '.join(names[x] for x in [b[0] for b in BIOMES] if x in chap[c])} |")
    L += ["", "## Hinweise", "",
          "- **Biom-Tags nicht geprüft.** Vanilla kennt meines Wissens `is_forest`, `is_taiga`, `is_jungle`, `is_savanna`, `is_river`, `is_beach`, `is_mountain` und `is_badlands`. "
          "Für Wüste, Ebene und Sumpf gibt es unter NeoForge gemeinsame Tags im Namensraum `c` (`c:is_desert` usw.). Das muss beim Einbinden gegen 1.21.1 geprüft werden.",
          "- **Die thematische Zuordnung ist ein Entwurf** (siehe Begründung je Geschichte). Zum Ändern `tools/data/john-biomes.tsv` bearbeiten und die Skripte neu laufen lassen.",
          "- **Seiten einzeln zuordnen:** eine Zeile `pgNNN<TAB>biom<TAB>Grund` in der Datei überschreibt die Geschichte für diese Seite.",
          "- Die Zuordnung ist nicht gleich auf alle Biome verteilt, aber ausgewogen (siehe Überblick)."]
    open(OUT_MD, "w", encoding="utf-8").write("\n".join(L) + "\n")
    print(f"{len(frags)} Manuskripte -> {len(BIOMES)} Biome -> {os.path.relpath(OUT_JSON, ROOT)}, {os.path.relpath(OUT_MD, ROOT)}")
    for b in bio.values():
        t = b["tiers"]
        print(f"  {b['name']:<11} {b['fragments']:>4} Ms  {len(b['stories']):>2} Geschichten {len(b['pages']):>3} Seiten  L{t[1]:>3} M{t[2]:>3} S{t[3]:>3}")


if __name__ == "__main__":
    main()
