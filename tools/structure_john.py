"""Gliedert Johannes fuer das Manuscript Archive: Geschichte > Seite > Vers > Manuskript.

Aufruf: python tools/structure_john.py
Ausgabe: tools/data/john-structure.json  (maschinenlesbar)
         docs/JOHN-STRUCTURE.md          (Uebersicht)

Ebenen
  Geschichte  Abschnitt des Evangeliums (Perikope), Grenzen aus tools/data/john-stories.tsv (von Hand pflegbar).
  Seite       Teil einer Geschichte mit etwa TARGET_PAGE_WORDS Woertern, geschnitten an Versgrenzen. Eine Seite liegt immer in einer Geschichte.
  Vers        Kapitel:Vers. Ein Vers kann aus mehreren Manuskripten bestehen, ein Manuskript kann mehrere Verse enthalten.
  Manuskript  Pn aus tools/data/manuscripts.*.json. Liegt immer innerhalb EINER Seite (build_manuscripts.py --rest schneidet an Seitengrenzen).

Stufe A (Geschichten, Seiten, Verse) braucht nur die Rohdaten. Stufe B (Manuskripte zuordnen, pruefen) laeuft, wenn die Manuskript-Dateien da sind.
build_manuscripts.py importiert verse_to_page() aus diesem Modul.
"""
import json
import os
import sys
from collections import Counter, defaultdict

sys.stdout.reconfigure(encoding="utf-8")
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = [os.path.join(ROOT, "tools/data/john-1-1-18.tsv"), os.path.join(ROOT, "tools/data/john-1-19-21-25.tsv")]
STORIES = os.path.join(ROOT, "tools/data/john-stories.tsv")
FRAGMENTS = [os.path.join(ROOT, "tools/data/manuscripts.john1.json"), os.path.join(ROOT, "tools/data/manuscripts.john-rest.json")]
BIOMES = os.path.join(ROOT, "tools/data/fragment-biomes.json")
OUT_JSON = os.path.join(ROOT, "tools/data/john-structure.json")
OUT_MD = os.path.join(ROOT, "docs/JOHN-STRUCTURE.md")
TARGET_PAGE_WORDS = 110
PUNCT = ",.·;’”"


def key(s):
    c, v = s.split(":")
    return int(c), int(v)


def fmt(k):
    return f"{k[0]}:{k[1]}"


def raw_words():
    """Alle Woerter in Textreihenfolge: (kapitel, vers, griechisch mit Satzzeichen)."""
    out = []
    for p in RAW:
        for line in open(p, encoding="utf-8"):
            if line.startswith("#") or not line.strip():
                continue
            c = line.rstrip("\n").split("\t")
            out.append((key(c[0] if ":" in c[0] else "1:" + c[0]), c[1]))
    return out


def verse_list(words):
    counts, order = Counter(), []
    for k, _ in words:
        if k not in counts:
            order.append(k)
        counts[k] += 1
    return order, counts


def read_stories(verses):
    stories = []
    for line in open(STORIES, encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        a, b, title = line.rstrip("\n").split("\t")
        stories.append({"from": key(a), "to": key(b), "title": title})
    owner = {}
    for v in verses:
        hit = [i for i, s in enumerate(stories) if s["from"] <= v <= s["to"]]
        if len(hit) != 1:
            raise SystemExit(f"Vers {fmt(v)} liegt in {len(hit)} Geschichten (muss genau eine sein)")
        owner[v] = hit[0]
    for i, s in enumerate(stories):
        s["verses"] = [v for v in verses if owner[v] == i]
        if not s["verses"]:
            raise SystemExit(f"Geschichte {s['title']} hat keine Verse")
        s["id"] = f"st{i + 1:02d}"
    return stories


def split_pages(verses, counts):
    """Schneidet eine Geschichte an Versgrenzen in Seiten mit etwa TARGET_PAGE_WORDS Woertern."""
    total = sum(counts[v] for v in verses)
    n = max(1, min(len(verses), round(total / TARGET_PAGE_WORDS)))
    cum, c = [], 0
    for v in verses:
        c += counts[v]
        cum.append(c)                       # cum[i] = Woerter bis einschliesslich Vers i
    cuts, last = [], -1                     # Schnitt nach Vers-Index i
    for k in range(1, n):
        target = k * total / n
        lo, hi = last + 1, len(verses) - 1 - (n - k)    # genug Verse fuer die restlichen Seiten
        best = min(range(lo, hi + 1), key=lambda i: abs(cum[i] - target))
        cuts.append(best)
        last = best
    pages, start = [], 0
    for cut in cuts + [len(verses) - 1]:
        pages.append(verses[start:cut + 1])
        start = cut + 1
    return pages


def build_a():
    words = raw_words()
    order, counts = verse_list(words)
    stories = read_stories(order)
    pages, n = [], 0
    for s in stories:
        s["pages"] = []
        for pv in split_pages(s["verses"], counts):
            n += 1
            pid = f"pg{n:03d}"
            pages.append({"id": pid, "story": s["id"], "verses": pv, "words": sum(counts[v] for v in pv)})
            s["pages"].append(pid)
        s["words"] = sum(counts[v] for v in s["verses"])
    return words, order, counts, stories, pages


def verse_to_page():
    """Fuer build_manuscripts.py: (kapitel, vers) -> Seiten-ID."""
    _, _, _, _, pages = build_a()
    return {v: p["id"] for p in pages for v in p["verses"]}


def load_fragments():
    frags = []
    for f in FRAGMENTS:
        if not os.path.exists(f):
            return None
        frags += json.load(open(f, encoding="utf-8"))["fragments"]
    return sorted(frags, key=lambda f: f["order"])


def build_b(words, stories, pages, frags):
    """Ordnet Manuskripte Woertern, Versen, Seiten und Geschichten zu und prueft die Hierarchie."""
    page_of = {v: p["id"] for p in pages for v in p["verses"]}
    story_of_page = {p["id"]: p["story"] for p in pages}
    if sum(len(f["words"]) for f in frags) != len(words):
        raise SystemExit("Wortzahl der Manuskripte passt nicht zu den Rohdaten")
    pos, info, problems = 0, {}, []
    for f in frags:
        chunk = words[pos:pos + len(f["words"])]
        pos += len(f["words"])
        for (k, g), w in zip(chunk, f["words"]):
            if g.rstrip(PUNCT) != w["greek"]:
                problems.append(f"{f['id']}: {g} passt nicht zu {w['greek']}")
        vs = list(dict.fromkeys(k for k, _ in chunk))
        ps = list(dict.fromkeys(page_of[v] for v in vs))
        if len(ps) != 1:
            problems.append(f"{f['id']} liegt auf mehreren Seiten: {ps}")
        info[f["id"]] = {"verses": [fmt(v) for v in vs], "page": ps[0], "story": story_of_page[ps[0]], "words": len(f["words"]), "ref": f["ref"]}
    if problems:
        raise SystemExit("\n".join(problems[:15]))
    return info


def main():
    words, order, counts, stories, pages = build_a()
    frags = load_fragments()
    info = build_b(words, stories, pages, frags) if frags else {}
    groups = json.load(open(BIOMES, encoding="utf-8")) if os.path.exists(BIOMES) else None
    frag_group = groups["fragments"] if groups else {}

    by_verse, by_page = defaultdict(list), defaultdict(list)
    for fid, i in info.items():
        for v in i["verses"]:
            by_verse[v].append(fid)
        by_page[i["page"]].append(fid)

    def biomes_of(fids):
        return sorted({frag_group[i]["biome"] for i in fids if i in frag_group})

    data = {
        "schema": 1,
        "note": "Hierarchie Geschichte > Seite > Vers > Manuskript. Erzeugt von tools/structure_john.py aus john-stories.tsv und den Rohdaten.",
        "stories": [{"id": s["id"], "title": s["title"], "from": fmt(s["from"]), "to": fmt(s["to"]), "verses": len(s["verses"]),
                     "words": s["words"], "pages": s["pages"], "biomes": biomes_of(sum((by_page[p] for p in s["pages"]), [])),
                     "fragments": sum(len(by_page[p]) for p in s["pages"])} for s in stories],
        "pages": [{"id": p["id"], "story": p["story"], "from": fmt(p["verses"][0]), "to": fmt(p["verses"][-1]),
                   "verses": [fmt(v) for v in p["verses"]], "words": p["words"], "fragments": by_page[p["id"]]} for p in pages],
        "verses": {fmt(v): {"page": next(p["id"] for p in pages if v in p["verses"]), "words": counts[v], "fragments": by_verse[fmt(v)]} for v in order},
        "fragments": info,
    }
    json.dump(data, open(OUT_JSON, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))

    # Kennzahlen
    pw = [p["words"] for p in pages]
    multi = sum(1 for i in info.values() if len(i["verses"]) > 1)
    split = sum(1 for v in by_verse.values() if len(v) > 1)
    spread = [len(biomes_of(by_page[p["id"]])) for p in pages] if groups else []
    L = ["# Gliederung des Johannesevangeliums für das Manuscript Archive", "",
         "Erzeugt von `tools/structure_john.py` (nicht von Hand ändern; die Geschichten-Grenzen stehen in `tools/data/john-stories.tsv`). "
         "Maschinenlesbar: `tools/data/john-structure.json`.", "",
         "**Ebenen:** Geschichte → Seite → Vers → Manuskript. Eine Seite liegt immer in genau einer Geschichte, ein Manuskript immer auf genau einer Seite. "
         "Ein Vers kann aus mehreren Manuskripten bestehen, ein Manuskript kann mehrere Verse enthalten.", "",
         f"- **{len(stories)} Geschichten**, **{len(pages)} Seiten** (je etwa {TARGET_PAGE_WORDS} Wörter, {min(pw)}–{max(pw)}), **{len(order)} Verse**, "
         f"**{len(info)} Manuskripte**, {len(words)} Wörter.",
         f"- {split} Verse bestehen aus mehr als einem Manuskript; {multi} Manuskripte reichen über eine Versgrenze.",
         "- Vers 5,4 fehlt (steht in NA28 nicht im Text, siehe `docs/CREDITS.md`)."]
    if spread:
        L.append(f"- Fundorte: Biome sind thematisch pro Geschichte vergeben (`docs/FRAGMENT-BIOMES.md`). Eine Seite liegt in durchschnittlich "
                 f"{sum(spread) / len(spread):.2f} Biomen (Maximum {max(spread)}); eine Geschichte in {sum(len(s['biomes']) for s in data['stories']) / len(stories):.2f}.")
    L += ["", "## Geschichten", "", "| ID | Titel | Verse | Wörter | Seiten | Manuskripte | Biom |", "|---|---|---|---|---|---|---|"]
    for s in data["stories"]:
        pg = s["pages"][0] if len(s["pages"]) == 1 else f"{s['pages'][0]}–{s['pages'][-1]}"
        L.append(f"| {s['id']} | {s['title']} | {s['from']}–{s['to']} | {s['words']} | {pg} | {s['fragments']} | {', '.join(s['biomes'])} |")
    L += ["", "## Hinweise", "",
          "- Die **Geschichten-Grenzen sind ein Entwurf** aus gängigen Gliederungen, die Titel sind eigene. Bitte durchsehen und in `john-stories.tsv` ändern, danach alles neu erzeugen "
          "(`build_manuscripts.py --rest`, `assign_biomes.py`, `structure_john.py`).",
          "- Die **Seiten** sind rein rechnerisch: etwa gleich lang, geschnitten an Versgrenzen. Für Prolog-Seiten (P1–P21, eingefroren) war das möglich, weil kein Pilot-Manuskript eine Versgrenze überschreitet.",
          "- **Für das Archiv:** Ein Vers ist vollständig, wenn alle seine Manuskripte vorliegen (`verses.<kap:vers>.fragments`). Eine Seite ist vollständig, wenn alle ihre Verse vollständig sind, eine Geschichte, wenn alle ihre Seiten vollständig sind."]
    open(OUT_MD, "w", encoding="utf-8").write("\n".join(L) + "\n")
    print(f"{len(stories)} Geschichten, {len(pages)} Seiten ({min(pw)}-{max(pw)} Woerter), {len(order)} Verse, {len(info)} Manuskripte; "
          f"Verse aus mehreren Manuskripten: {split}; Manuskripte ueber Versgrenze: {multi}")
    if spread:
        print("Biome pro Seite: mittel %.1f, min %d, max %d" % (sum(spread) / len(spread), min(spread), max(spread)))


if __name__ == "__main__":
    main()
