"""Baut aus den Rohdaten (tools/data/john-*.tsv) die Manuskripte fuer das Spiel.

Aufruf:  python tools/build_manuscripts.py
Ausgabe: tools/data/manuscripts.john1.json  (Format wie tools/examples/fragments.example.json, mehr Felder)

Schritte
  1. Einlesen: pro Wort Vers, Griechisch, Strong-Nummer, Glosse, Grammatikcode.
  2. Glossen saeubern: Klammern und Satzzeichen weg, erlaubte Varianten ableiten ("In [the]" -> in / in the).
  3. Zerschneiden: an Satz- und Teilsatzgrenzen in Manuskripte von MIN_WORDS bis MAX_WORDS Woertern.
  4. Schwierigkeit: Punktzahl pro Wort (Seltenheit, Laenge, Form), pro Manuskript gemittelt, dann in 3 Stufen.
  5. Falsche Antworten: Kasus- und Zahlformen bei Nomen, sonst Glossen anderer Woerter derselben Wortart.
  6. Pruefen und ausgeben. Ob alles in die Setzform passt, prueft core/.../ManuscriptDataTest.

Die Zufallsentscheidungen haengen nur vom Seed ab: gleiche Eingabe, gleiche Ausgabe.
"""
import json
import math
import os
import random
import re
import sys
from collections import Counter, defaultdict

sys.stdout.reconfigure(encoding="utf-8")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, "tools/data/john-1-1-18.tsv")
OUT = os.path.join(ROOT, "tools/data/manuscripts.john1.json")
SEED = 20261116
MIN_WORDS, MAX_WORDS = 5, 16
WRONG_PER_WORD = 3
BIOMES = ["#minecraft:is_desert", "#minecraft:is_forest", "#minecraft:is_plains", "#minecraft:is_taiga",
          "#minecraft:is_jungle", "#minecraft:is_savanna", "#minecraft:is_swamp"]
PUNCT_END = "·;.?!"      # Satzende
PUNCT_CLAUSE = ","       # Teilsatz

# ---------------------------------------------------------------------------------------------- Einlesen


def read_raw(path):
    words = []
    for line in open(path, encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        verse, greek, strongs, gloss, morph = line.rstrip("\n").split("\t")
        punct = ""
        while greek and greek[-1] in PUNCT_END + PUNCT_CLAUSE + "’”":
            punct = greek[-1] + punct
            greek = greek[:-1]
        words.append({"verse": int(verse), "greek": greek, "punct": punct, "strongs": strongs.rstrip("GH"),
                      "gloss_raw": gloss, "morph": morph})
    return words


# ---------------------------------------------------------------------------------------------- Glossen


OVERRIDE = {  # Glossen, bei denen die Klammer zum Sinn gehoert
    "no [one]": "no one", "[His] own": "his own", "The [One]": "the one", "one [thing]": "one thing",
}
BRACKET_OK = {"the", "his", "her", "their", "it", "him", "one", "thing", "born"}


def clean_gloss(raw, morph):
    """Liefert (kanonische Glosse, Liste erlaubter Glossen). Alles kleingeschrieben ausser Namen und Titeln.

    Kanonisch ist die Form ohne ergaenzte Klammerwoerter ("[the] name" -> "name"); die Form mit Klammerwoertern
    ("the name") wird zusaetzlich akzeptiert, wenn die Klammer nur Artikel oder Pronomen enthaelt.
    """
    raw_g = raw.replace("’", "").replace("”", "").strip(" .,;·")
    g = raw_g.replace("<", "").replace(">", "")
    full = re.sub(r"\[([^\]]*)\]", lambda m: m.group(1), g)            # Klammerinhalt mit
    short = re.sub(r"\s*\[[^\]]*\]", "", g)                            # Klammerinhalt ohne
    canon = OVERRIDE.get(raw_g, short) or full
    names = morph.endswith("-P") or morph.endswith("-T")

    def norm(x):
        x = re.sub(r"\s+", " ", x).strip()
        return x if names else x.lower()

    canon = norm(canon)
    allowed = [canon]
    bracket_words = {w.lower() for m in re.finditer(r"\[([^\]]*)\]", g) for w in m.group(1).split()}
    variants = [short]
    if bracket_words <= BRACKET_OK:
        variants.append(full)
    for v in variants:
        v = norm(v)
        if v and v not in allowed:
            allowed.append(v)
    return canon, allowed


# ---------------------------------------------------------------------------------------------- Englisch


IRREGULAR_PLURAL = {"man": "men", "woman": "women", "child": "children", "foot": "feet"}
IRREGULAR_SINGULAR = {v: k for k, v in IRREGULAR_PLURAL.items()}


def pluralize(w):
    if w in IRREGULAR_PLURAL:
        return IRREGULAR_PLURAL[w]
    if re.search(r"(s|x|ch|sh)$", w):
        return w + "es"
    if re.search(r"[^aeiou]y$", w):
        return w[:-1] + "ies"
    return w + "s"


def singularize(w):
    if w in IRREGULAR_SINGULAR:
        return IRREGULAR_SINGULAR[w]
    if w.endswith("ies"):
        return w[:-3] + "y"
    if re.search(r"(ses|xes|ches|shes)$", w):
        return w[:-2]
    if w.endswith("s") and not w.endswith("ss"):
        return w[:-1]
    return w


def noun_variants(gloss, morph):
    """Andere Kasus und Zahlen desselben Nomens, als Kandidaten fuer falsche Antworten."""
    m = re.match(r"^(of|to|a|an|the)\s+(.*)$", gloss)
    prefix, core = (m.group(1), m.group(2)) if m else ("", gloss)
    parts = morph.split("-")
    number = parts[1][1] if len(parts) > 1 and len(parts[1]) > 1 else "S"
    first, last = core.rsplit(" ", 1) if " " in core else ("", core)

    def join(pre, noun):
        return " ".join(x for x in (pre, first, noun) if x)

    base = singularize(last) if number == "P" else last
    plural = pluralize(base)
    out = [join("", base), join("of", base), join("to", base), join("", plural), join("of", plural)]
    out = [o for o in out if o != gloss]
    if last in MASS or base in MASS:           # unzaehlbar: kein Plural
        out = [o for o in out if not o.endswith(pluralize(base))]
    return out


# ---------------------------------------------------------------------------------------------- Schwierigkeit


def morph_weight(morph):
    p = morph.split("-")
    cls = p[0]
    if cls in ("CONJ", "PREP", "PRT", "ADV", "T", "COND"):
        return 0.0
    if cls in ("P", "D", "R", "K", "A", "N") and len(p) > 1:
        case = p[1][0]
        w = {"N": 0.2, "V": 0.2, "A": 0.3, "G": 0.6, "D": 0.6}.get(case, 0.3)
        return w + (0.0 if cls == "N" else 0.15)
    if cls == "V":
        tense_mood = p[1]
        tense, mood = tense_mood[0], tense_mood[2] if len(tense_mood) > 2 else "I"
        w = {"P": 0.3, "I": 0.4, "A": 0.6, "2": 0.7, "R": 0.9, "F": 0.7}.get(tense_mood[0], 0.6)
        if len(tense_mood) > 1 and tense_mood[0] == "2":
            w = 0.8
        w += {"I": 0.0, "S": 0.3, "N": 0.3, "P": 0.4, "O": 0.4, "M": 0.4}.get(tense_mood[-1] if len(tense_mood) > 2 else "I", 0.2)
        return min(w, 1.3)
    return 0.3


def word_score(w, freq):
    f = freq[w["strongs"]]
    rarity = 1.0 / (1.0 + math.log(f))            # selten = nah an 1
    length = min(len(w["greek"]) / 12.0, 1.0)
    return 0.45 * rarity + 0.25 * length + 0.30 * morph_weight(w["morph"])


# ---------------------------------------------------------------------------------------------- Zerschneiden


def chunk(words):
    """Teilt in Manuskripte: an Satzenden und, wenn es zu lang wuerde, an Kommas."""
    units, cur = [], []
    for w in words:
        cur.append(w)
        if w["punct"]:
            units.append(cur)
            cur = []
    if cur:
        units.append(cur)
    out, cur = [], []
    for u in units:
        size = sum(len(x) for x in [cur, u])
        if cur and size > MAX_WORDS:
            out.append(cur)
            cur = []
        cur = cur + u
        ends_sentence = u[-1]["punct"][:1] in PUNCT_END
        if ends_sentence and len(cur) >= MIN_WORDS:
            out.append(cur)
            cur = []
    if cur:
        if out and len(cur) < MIN_WORDS:
            out[-1] = out[-1] + cur
        else:
            out.append(cur)
    return out


def refs(chunks):
    """John 1:1a, 1:2, 1:3-4 ... Teile eines Verses bekommen Buchstaben."""
    per_verse = Counter()
    for c in chunks:
        if c[0]["verse"] == c[-1]["verse"]:
            per_verse[c[0]["verse"]] += 1
    seen, out = Counter(), []
    for c in chunks:
        a, b = c[0]["verse"], c[-1]["verse"]
        if a == b:
            letter = ""
            if per_verse[a] > 1:
                letter = "abcdefghij"[seen[a]]
                seen[a] += 1
            out.append(f"John 1:{a}{letter}")
        else:
            out.append(f"John 1:{a}-{b}")
    return out


# ---------------------------------------------------------------------------------------------- Falsche Antworten


SUBJECTS = {"he", "she", "it", "they", "we", "i", "you"}
FALLBACK = {
    "N": ["house", "way", "day", "river", "voice"], "V": ["went", "said", "saw", "has gone", "heard"],
    "A": ["good", "great", "first", "other"], "PREP": ["by", "under", "upon", "before", "after"],
    "CONJ": ["or", "so", "then", "yet", "also"], "T": ["this", "that", "a", "these"],
    "PRT": ["never", "no", "also", "even"], "K": ["whoever", "all who", "those who"],
    "N-P": ["Peter", "David", "Abraham", "Israel"], "D": ["these", "those", "such a one", "they"],
    "ADV": ["never", "once", "already", "still", "now"],
    "P": ["me", "you", "them", "us"], "R": ["which", "whom", "whose"],
}


MASS = {"flesh", "grace", "truth", "glory", "light", "life", "darkness", "love", "blood", "authority", "fullness", "will"}


def pcls(morph):
    """Klasse fuer die Auswahl: Eigennamen und Titel (God, Father) bilden eine eigene."""
    cls = morph.split("-")[0]
    return "N-P" if cls == "N" and morph.endswith(("-P", "-T")) else cls


def wrong_answers(w, ok, pool, rng, same_lemma=()):
    cls = pcls(w["morph"])
    taken = {o.lower() for o in ok}
    cands = []
    if cls == "N":
        cands += noun_variants(ok[0], w["morph"])
    elif cls == "N-P":  # Namen: Kasusform und andere Namen
        cands += [f"of {ok[0]}"] if not ok[0].startswith("of ") else [ok[0][3:]]
    def core(x):  # Verben: "he was" und "was" sind dieselbe Antwort
        return " ".join(t for t in x.lower().split() if t not in SUBJECTS) if cls == "V" else x.lower()

    taken |= {core(o) for o in ok}
    taken |= {g.lower() for g in same_lemma}      # andere Uebersetzungen desselben Wortes sind keine falschen Antworten
    taken |= {core(g) for g in same_lemma}
    pool_c = [g for g in pool.get(cls, []) if g.lower() not in taken and core(g) not in taken]
    shape = w["morph"].split("-")[1] if "-" in w["morph"] else ""
    pool_c.sort(key=lambda g: (0 if shape and shape == pool_shape(g, pool, cls) else 1, rng.random()))
    cands += pool_c
    cands += [g for g in FALLBACK.get(cls, []) if g.lower() not in taken]
    out = []
    for c in cands:
        if c.lower() in taken or core(c) in taken or c.lower() in {x.lower() for x in out}:
            continue
        out.append(c)
        if len(out) == WRONG_PER_WORD:
            break
    return out


_shape_of = {}


def pool_shape(gloss, pool, cls):
    return _shape_of.get((cls, gloss), "")


# ---------------------------------------------------------------------------------------------- Hauptprogramm


def main():
    rng = random.Random(SEED)
    words = read_raw(RAW)
    freq = Counter(w["strongs"] for w in words)

    for w in words:
        w["gloss"], w["allowed"] = clean_gloss(w["gloss_raw"], w["morph"])
    # Dasselbe Wort in derselben Form darf auch mit den anderen Glossen uebersetzt werden, die es im Text bekam.
    by_form = defaultdict(list)
    by_lemma = defaultdict(set)
    for w in words:
        by_form[(w["strongs"], w["morph"])] += [a for a in w["allowed"] if a not in by_form[(w["strongs"], w["morph"])]]
        by_lemma[w["strongs"]] |= set(w["allowed"])
    for w in words:
        if w["morph"].startswith("V-"):   # nur Verben: "became"/"came into being"; Funktionswoerter bleiben eng
            w["allowed"] = list(dict.fromkeys(w["allowed"] + by_form[(w["strongs"], w["morph"])]))

    pool = defaultdict(list)
    for w in words:
        cls = pcls(w["morph"])
        if w["gloss"] not in pool[cls]:
            pool[cls].append(w["gloss"])
            _shape_of[(cls, w["gloss"])] = w["morph"].split("-")[1] if "-" in w["morph"] else ""

    chunks = chunk(words)
    names = refs(chunks)
    scores = [sum(word_score(w, freq) for w in c) / len(c) + 0.004 * len(c) for c in chunks]
    order = sorted(scores)
    third = len(order) / 3.0

    def tier(s):
        rank = order.index(s)
        return 1 if rank < third else 2 if rank < 2 * third else 3

    fragments, problems = [], []
    for n, (c, ref, score) in enumerate(zip(chunks, names, scores), start=1):
        entries = []
        for i, w in enumerate(c):
            wrong = wrong_answers(w, w["allowed"], pool, rng, by_lemma[w["strongs"]])
            if len(wrong) < WRONG_PER_WORD:
                problems.append(f"{ref} Wort {i} {w['greek']}: nur {len(wrong)} falsche Antworten")
            entries.append({
                "i": i, "greek": w["greek"], "lemma": "G" + w["strongs"], "morph": w["morph"],
                "gloss": {"ok": w["allowed"], "wrong": wrong, "status": "draft"},
            })
        fragments.append({
            "id": f"P{n}", "ref": ref, "chapter": 1, "order": n,
            "tier": tier(score), "difficulty": round(score * 100, 1),
            "origin": {"biome": rng.choice(BIOMES), "container": "loot"},
            "reference_en": " ".join(w["gloss"] for w in c),
            "notes": ["Glossen automatisch erzeugt, ungeprueft"],
            "words": entries,
        })

    data = {
        "schema": 1, "placeholder": False,
        "source": {"text": "STEPBible-Wortanalyse mit BSB-Glossen, Johannes 1,1-18 (Pilot)",
                   "license": "Namensnennung vor Veroeffentlichung pruefen"},
        "fragments": fragments,
    }
    json.dump(data, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)

    print(f"{len(words)} Woerter, {len(fragments)} Manuskripte -> {os.path.relpath(OUT, ROOT)}")
    for f in fragments:
        print(f"  {f['id']:>4} {f['ref']:<14} {len(f['words']):>2} Woerter  Stufe {f['tier']}  Schwierigkeit {f['difficulty']:>5}  {f['origin']['biome'][11:]}")
    tiers = Counter(f["tier"] for f in fragments)
    print("Stufen:", dict(sorted(tiers.items())))
    if problems:
        print("PROBLEME:")
        for p in problems:
            print("  ", p)
        sys.exit(1)


if __name__ == "__main__":
    main()
