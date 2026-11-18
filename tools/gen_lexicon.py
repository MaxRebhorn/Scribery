"""Erzeugt das Complete Greek Lexicon (alle Woerter des Johannesevangeliums) als Buchtext.

Aufruf: python tools/gen_lexicon.py
Eingabe: tools/data/manuscripts.john1.json, manuscripts.john-rest.json (Woerter mit Strong-Nummer),
         tools/data/src/tbesg.txt (STEPBible TBESG, CC BY 4.0, siehe docs/CREDITS.md)
Ausgabe: mod/src/main/resources/assets/scribery/books/complete_greek_lexicon.md

Ein Eintrag je Grundform (zusammengefasst ueber alle Formen), sortiert nach griechischem Alphabet, je Anfangsbuchstabe
ein Kapitel auf neuer Seite. Eintrag: Grundform, Umschrift, Wortart, kurze Bedeutung, Haeufigkeit im Johannesevangelium.
"""
import collections
import json
import os
import re
import unicodedata

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "tools", "data")
OUT = os.path.join(ROOT, "mod", "src", "main", "resources", "assets", "scribery", "books", "complete_greek_lexicon.md")

LETTERS = [("alpha", "Α α", "α"), ("beta", "Β β", "β"), ("gamma", "Γ γ", "γ"), ("delta", "Δ δ", "δ"), ("epsilon", "Ε ε", "ε"),
           ("zeta", "Ζ ζ", "ζ"), ("eta", "Η η", "η"), ("theta", "Θ θ", "θ"), ("iota", "Ι ι", "ι"), ("kappa", "Κ κ", "κ"),
           ("lambda", "Λ λ", "λ"), ("mu", "Μ μ", "μ"), ("nu", "Ν ν", "ν"), ("xi", "Ξ ξ", "ξ"), ("omicron", "Ο ο", "ο"),
           ("pi", "Π π", "π"), ("rho", "Ρ ρ", "ρ"), ("sigma", "Σ σ", "σ"), ("tau", "Τ τ", "τ"), ("upsilon", "Υ υ", "υ"),
           ("phi", "Φ φ", "φ"), ("chi", "Χ χ", "χ"), ("psi", "Ψ ψ", "ψ"), ("omega", "Ω ω", "ω")]
LETTER_NAMES = {k: k.capitalize() for k, _, _ in LETTERS}


def norm(s):
    s = unicodedata.normalize("NFD", s)
    s = "".join(c for c in s if not unicodedata.combining(c)).lower().replace("ς", "σ")
    return s


def pos_of(tag, gloss):
    t = tag.split(":", 1)[-1]
    t = re.split(r"[ /]", t.strip())[0].upper() if not t.startswith("N-") else t
    if t in ("K", "F") or re.fullmatch(r"F-\d", t):
        return ""
    if t.startswith("N-M-P") or t.startswith("N-F-P") or t.startswith("N-N-P") or t.startswith("N-"):
        if "-P" in t or t.endswith("-P") or t.startswith("N-PRI") or t.startswith("N-LI"):
            return "name"
        return {"N-M": "m.", "N-F": "f.", "N-N": "n."}.get(t[:3], "noun")
    return {"V": "v.", "ADJ": "adj.", "ADV": "adv.", "PREP": "prep.", "CONJ": "conj.", "PRT": "part.", "PRON": "pron.",
            "ART": "art.", "COND": "cond.", "INJ": "interj.", "T": "art.", "D": "pron.", "A": "adj.", "P": "pron.",
            "C": "conj.", "X": "part.", "R": "pron.", "I": "interj.", "N": "noun"}.get(t.split("-")[0], t.lower())


def main():
    cnt = collections.Counter()
    for f in ("manuscripts.john1.json", "manuscripts.john-rest.json"):
        for p in json.load(open(os.path.join(DATA, f), encoding="utf-8"))["fragments"]:
            for w in p["words"]:
                m = re.fullmatch(r"GG(\d{4})\w?", w["lemma"])
                assert m, w["lemma"]
                cnt["G" + m.group(1)] += 1
    rows = collections.defaultdict(list)
    for line in open(os.path.join(DATA, "src", "tbesg.txt"), encoding="utf-8"):
        c = line.rstrip("\n").split("\t")
        if len(c) > 6 and re.fullmatch(r"G\d{4}\w?", c[0]):
            rows[c[0][:5]].append(c)
    entries = collections.defaultdict(list)
    missing = []
    for key, n in cnt.items():
        rs = rows.get(key)
        if not rs:
            missing.append(key)
            continue
        main_row = next((r for r in rs if "Meaning of" not in r[1]), rs[0])
        lemma, tr, tag, gloss = main_row[3], main_row[4], main_row[5], main_row[6]
        extra = []
        for r in rs:
            g = r[6]
            if r is main_row or g == gloss:
                continue
            tail = g.split(": ", 1)[-1]
            if tail not in extra and tail not in gloss and len(extra) < 2:
                extra.append(tail)
        if extra and ": " in gloss:
            gloss = gloss + "; " + "; ".join(extra)
        elif extra:
            gloss = gloss + "; " + "; ".join(extra)
        # "to perceive: see; know" -> "see; know" (der Teil vor dem Doppelpunkt ist nur eine Oberkategorie)
        gloss = gloss.split(": ", 1)[-1] if ": " in gloss else gloss
        first = norm(lemma)[:1]
        letter = next((k for k, _, g in LETTERS if g == first), None)
        assert letter, (lemma, first)
        entries[letter].append((norm(lemma), lemma, tr, pos_of(tag, gloss), gloss, n))
    assert not missing, missing
    total = sum(len(v) for v in entries.values())

    out = []
    out.append("# THE COMPLETE GREEK LEXICON")
    out.append("### Every Word of the Gospel of John")
    out.append("")
    out.append("*From alpha to omega: all the words you will meet, with what they mean and how often they turn up.*")
    out.append("")
    out.append("---")
    out.append("")
    out.append("## Page 0 — Contents {#contents}")
    out.append("")
    out.append(f"{total} words, in the order of the Greek alphabet. Each entry gives the word as a dictionary lists it, its sounds in Latin letters, what kind of word it is, its meaning and how often it appears in John (like this: ×12).")
    out.append("")
    out.append("   ".join(f"[{g.replace(' ', chr(0xa0))}](#{k})" for k, g, _ in LETTERS if entries.get(k)))
    out.append("")
    out.append("---")
    for k, g, _ in LETTERS:
        es = sorted(entries.get(k, []))
        if not es:
            continue
        out.append("")
        out.append(f"## Page {len(out)} — {g} {{#{k}}}")
        out.append("")
        for _, lemma, tr, pos, gloss, n in es:
            gloss = re.sub(r"\s+", " ", gloss).strip()
            p = f" *{pos}*" if pos else ""
            # Das Griechische ohne Fettdruck: In der fetten Minecraft-Schrift verschmieren die Zeichen (siehe docs/BOOK-FORMAT.md)
            out.append(f"{lemma} **{tr}**{p} — {gloss} ×{n}")
            out.append("")
    text = "\n".join(out).rstrip() + "\n"
    open(OUT, "w", encoding="utf-8", newline="\n").write(text)
    print("Eintraege:", total, "Buchstaben:", sum(1 for v in entries.values() if v))
    pc = collections.Counter(e[3] for v in entries.values() for e in v)
    print(pc.most_common(20))


if __name__ == "__main__":
    main()
