"""Erzeugt das Greek Forms & Grammar Lexicon: alle Formen, die im Johannesevangelium vorkommen, mit ihrer Bestimmung.

Aufruf: python tools/gen_forms_lexicon.py
Eingabe: tools/data/manuscripts.john1.json, manuscripts.john-rest.json (jedes Wort mit Form, Grundform und Robinson-Code,
         STEPBible-Wortanalyse CC BY 4.0, siehe docs/CREDITS.md), tools/gen_lexicon.py (Bedeutungen), tools/data/forms-texts.json
Ausgabe: mod/src/main/resources/assets/scribery/books/greek_forms_grammar_lexicon.md

Ein Absatz je Grundform mit mindestens zwei verschiedenen Formen: Grundform, Umschrift, Wortart, Bedeutung, dann die Formen
mit Bestimmung in Kurzschreibweise (siehe Seite "Reading the Codes"). Woerter mit nur einer Form brauchen keine Hilfe.
"""
import collections
import json
import os
import re

import gen_lexicon as GL

ROOT = GL.ROOT
DATA = GL.DATA
OUT = os.path.join(GL.BOOKS, "greek_forms_grammar_lexicon.md")

TENSE = {"P": "pres", "I": "impf", "F": "fut", "A": "aor", "R": "perf", "L": "plup", "X": ""}
VOICE = {"A": "act", "M": "mid", "P": "pass", "E": "m/p", "D": "dep", "O": "p-dep", "N": "dep", "Q": "imps", "X": ""}
MOOD = {"I": "ind", "S": "subj", "M": "imp", "N": "inf", "P": "ptc", "O": "opt", "R": "imp"}
CASE = {"N": "nom", "G": "gen", "D": "dat", "A": "acc", "V": "voc"}
NUM = {"S": "sg", "P": "pl"}
GEN = {"M": "m", "F": "f", "N": "n"}
CASE_ORDER = "NGDAV"
TENSE_ORDER = "PIFARLX"
MOOD_ORDER = "ISMOPN"


def _case_num_gen(tok):
    """'DSF' -> 'dat sg f'; 'NS' -> 'nom sg'; '3GSM' -> '3 gen sg m' (Person, Fall, Zahl, Geschlecht)."""
    out = []
    i = 0
    if tok and tok[0] in "123":
        out.append(tok[0])
        i = 1
    if i < len(tok) and tok[i] in CASE:
        out.append(CASE[tok[i]]); i += 1
    if i < len(tok) and tok[i] in NUM:
        out.append(NUM[tok[i]]); i += 1
    if i < len(tok) and tok[i] in GEN:
        out.append(GEN[tok[i]]); i += 1
    return " ".join(out) if i == len(tok) else None


def parse(morph):
    """Robinson-Code in Kurzschreibweise (Lesbarkeit vor Vollstaendigkeit). Liefert (text, sortierschluessel)."""
    parts = morph.split()[0].split("-")
    pos = parts[0]
    rest = parts[1:]
    if pos == "V" and rest:
        tv = rest[0]
        second = tv.startswith("2")
        tv = tv.lstrip("2")
        if len(tv) >= 3:
            t, v, m = tv[0], tv[1], tv[2]
            bits = [("2" if second else "") + TENSE.get(t, t)]
            if VOICE.get(v):
                bits.append(VOICE[v])
            bits.append(MOOD.get(m, m))
            tail = ""
            if len(rest) > 1:
                tail = _case_num_gen(rest[1]) or ""
                if m not in "P" and len(rest[1]) == 2 and rest[1][0] in "123":
                    tail = rest[1][0] + NUM.get(rest[1][1], "")
            key = (TENSE_ORDER.find(t), MOOD_ORDER.find(m), rest[1] if len(rest) > 1 else "")
            return (" ".join(bits) + (" " + tail if tail else "")).strip(), key
        return morph, (99, 99, "")
    if rest:
        t = _case_num_gen(rest[0])
        if t:
            ci = CASE_ORDER.find(rest[0][1 if rest[0][0] in "123" else 0])
            return t, (0, ci, rest[0])
    return "", (0, 0, "")


def collect():
    """{lemma: {(form, parse): anzahl}} mit Satzzeichen vom Rand der Formen entfernt."""
    out = collections.defaultdict(collections.Counter)
    for f in ("manuscripts.john1.json", "manuscripts.john-rest.json"):
        for p in json.load(open(os.path.join(DATA, f), encoding="utf-8"))["fragments"]:
            for w in p["words"]:
                g = re.sub(r"[·;.,?!·;]+$", "", w["greek"]).strip()
                m = re.fullmatch(r"GG(\d{4})\w?", w["lemma"])
                out["G" + m.group(1)][(g, w["morph"])] += 1
    return out


def collect_rows():
    """Alle Grundformen mit mindestens zwei Formen: [(sortierschluessel, eintrag, formen)], nach dem Alphabet sortiert."""
    forms = collect()
    key_of = {}
    for _letter, es in GL.load_entries(with_key=True).items():
        for e in es:
            key_of[e[6]] = e
    rows = []
    for strong, fm in forms.items():
        e = key_of.get(strong)
        if not e:
            continue
        if len({g for (g, _m) in fm}) < 2:
            continue
        rows.append((GL.norm(e[1]), e, fm))
    rows.sort(key=lambda r: r[0])
    return rows


def letter_key(sort_key):
    return next(k for k, _g, ch in GL.LETTERS if ch == sort_key[:1])


def entry_paragraph(e, fm):
    """Ein Absatz: Grundform, Umschrift, Wortart, Bedeutung, dann die Formen mit Bestimmung."""
    _k, lemma, tr, pos, gloss, _n, _s = e
    parsed = collections.defaultdict(list)
    for (g, m), _cnt in fm.items():
        p, key = parse(m)
        parsed[g].append((key, p))
    items = []
    for g, lst in parsed.items():
        lst.sort()
        ps = []
        for _k2, p2 in lst:
            if p2 and p2 not in ps:
                ps.append(p2)
        items.append((lst[0][0], g, ps))
    items.sort()
    pp = f" *{pos}*" if pos else ""
    body = " · ".join(f"{g} {' / '.join(ps)}".strip() for _k3, g, ps in items)
    return f"{lemma} **{tr}**{pp} — {gloss}: {body}"


def main():
    T = json.load(open(os.path.join(DATA, "forms-texts.json"), encoding="utf-8"))
    rows = collect_rows()

    out = []
    n = [0]

    def page(title, anchor=None):
        out.append("")
        a = f" {{#{anchor}}}" if anchor else ""
        out.append(f"## Page {n[0]} — {title}{a}")
        out.append("")
        n[0] += 1

    def para(t):
        out.append(t)
        out.append("")

    def block(text):
        for chunk in text.strip().split("\n\n"):
            para(chunk.strip())

    out.append(f"# {T['title']}")
    out.append(f"### {T['subtitle']}")
    out.append("")
    out.append("**By John Koine**")
    out.append("")
    out.append(T["tagline"])
    out.append("")
    out.append("---")
    page("Contents", "contents")
    for text, anchor in (("A few words before the words", "preface"), ("Reading the codes", "codes"),
                         ("The forms, from alpha to omega", "forms-alpha")):
        out.append(f"- [{text}](#{anchor})")
    out.append("")
    out.append(GL_letters_line(rows))
    out.append("")
    out.append("- [Greek Alphabet Primer](book:greek_alphabet_primer#contents)")
    out.append("- [Greek Grammar Book](book:greek_grammar_book#contents)")
    out.append("- [Complete Greek Lexicon](book:complete_greek_lexicon#contents)")
    out.append("")
    out.append("---")
    page(T["preface_title"], "preface")
    block(T["preface"].replace("{lemmas}", str(len(rows))).replace("{forms}", str(sum(len({g for g, _ in fm}) for _, _, fm in rows))))
    page("Reading the Codes", "codes")
    block(T["codes_intro"])
    for a, b in T["codes_table"]:      # als Liste, nicht als Tabelle: lange Erklaerungen wuerden die Seite sprengen
        out.append(f"- **{a}** — {b}")
    out.append("")
    block(T["codes_outro"])
    first = True
    letters = {k: g for k, g, _ in GL.LETTERS}
    cur = None
    for sk, e, fm in rows:
        letter = letter_key(sk)
        if letter != cur:
            cur = letter
            page(letters[letter], "forms-alpha" if first else "forms-" + letter)
            first = False
        para(entry_paragraph(e, fm))
    page("One Last Word")
    block(T["outro"])
    para(T["signoff"])
    open(OUT, "w", encoding="utf-8", newline="\n").write("\n".join(out).rstrip() + "\n")
    chars = sum(len(x) for x in out)
    print("Grundformen:", len(rows), "Zeichen:", chars, "grob Seiten:", chars // 280)


def GL_letters_line(rows):
    have = set()
    for _n, _e, _f in rows:
        have.add(next(k for k, _g, ch in GL.LETTERS if ch == _n[:1]))
    first = True
    items = []
    for k, g, _ in GL.LETTERS:
        if k in have:
            anchor = "forms-alpha" if first else "forms-" + k
            first = False
            items.append(f"[{g.replace(' ', chr(0xa0))}](#{anchor})")
    return "   ".join(items)


if __name__ == "__main__":
    main()
