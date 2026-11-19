"""Erzeugt die Lexika (alle Woerter des Johannesevangeliums) als Buchtexte.

Aufruf: python tools/gen_lexicon.py
Eingabe: tools/data/manuscripts.john1.json, manuscripts.john-rest.json (Woerter mit Strong-Nummer),
         tools/data/src/tbesg.txt (STEPBible TBESG, CC BY 4.0, siehe docs/CREDITS.md), tools/lexicon_themes.py (Sachgebiete)
Ausgabe (mod/src/main/resources/assets/scribery/books/): greek_lexicon_a_c.md, greek_lexicon_d_f.md,
         thematic_greek_lexicon.md, complete_greek_lexicon.md

Ein Eintrag je Grundform (zusammengefasst ueber alle Formen), sortiert nach griechischem Alphabet, je Anfangsbuchstabe
ein Kapitel auf neuer Seite. Eintrag: Grundform, Umschrift, Wortart, kurze Bedeutung. Nur die Hauptbedeutung; die weiteren
Zeilen des Woerterbuchs gehoeren oft zu anderen, gleich geschriebenen Woertern und sind irrefuehrend.
Das Complete Greek Lexicon enthaelt alles der anderen: den Teil nach dem Alphabet und den Teil nach Sachgebieten.
Die Vorworte stehen in tools/data/lexicon-texts.json (Ton wie in Primer und Grammar Book: John Koine).
"""
import collections
import json
import os
import re
import unicodedata

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "tools", "data")
BOOKS = os.path.join(ROOT, "mod", "src", "main", "resources", "assets", "scribery", "books")

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


def load_entries():
    """Alle Eintraege: {buchstabe: [(sortierschluessel, grundform, umschrift, wortart, bedeutung, anzahl)]}."""
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
        # Nur die Hauptbedeutung: die weiteren Zeilen des Woerterbuchs gehoeren oft zu anderen, gleich geschriebenen Woertern
        # (zum Beispiel "sebaceous formation" bei eutheos) und sind fuer Leser des Johannesevangeliums irrefuehrend.
        # "to perceive: see; know" -> "see; know" (der Teil vor dem Doppelpunkt ist nur eine Oberkategorie)
        gloss = gloss.split(": ", 1)[-1] if ": " in gloss else gloss
        first = norm(lemma)[:1]
        letter = next((k for k, _, g in LETTERS if g == first), None)
        assert letter, (lemma, first)
        entries[letter].append((norm(lemma), lemma, tr, pos_of(tag, gloss), gloss, n))
    assert not missing, missing
    return entries


def entry_line(e):
    _, lemma, tr, pos, gloss, _n = e
    gloss = re.sub(r"\s+", " ", gloss).strip()
    p = f" *{pos}*" if pos else ""
    # Das Griechische ohne Fettdruck: In der fetten Minecraft-Schrift verschmieren die Zeichen (siehe docs/BOOK-FORMAT.md)
    return f"{lemma} **{tr}**{p} — {gloss}"


class Book:
    """Sammelt Zeilen eines Buchs; Seiten werden fortlaufend nummeriert (die Nummer zeigt das Buch nicht)."""

    def __init__(self):
        self.out = []
        self.n = 0

    def line(self, t=""):
        self.out.append(t)

    def para(self, t):
        self.out.append(t)
        self.out.append("")

    def page(self, title, anchor=None):
        self.out.append("")
        a = f" {{#{anchor}}}" if anchor else ""
        self.out.append(f"## Page {self.n} \u2014 {title}{a}")
        self.out.append("")
        self.n += 1

    def text(self):
        return "\n".join(self.out).rstrip() + "\n"


def paragraphs(book, block):
    for chunk in block.strip().split("\n\n"):
        book.para(chunk.strip())


def letter_pages(book, entries, keys):
    for k, g, _ in LETTERS:
        if k in keys and entries.get(k):
            book.page(g, k)
            for e in sorted(entries[k]):
                book.para(entry_line(e))


def letters_line(keys):
    return "   ".join(f"[{g.replace(' ', chr(0xa0))}](#{k})" for k, g, _ in LETTERS if k in keys)


def front(book, T, title_key, contents_items, extra_contents=""):
    t = T[title_key]
    book.line(f"# {t['title']}")
    book.line(f"### {t['subtitle']}")
    book.line("")
    book.line("**By John Koine**")
    book.line("")
    book.line(t["tagline"])
    book.line("")
    book.line("---")
    book.page("Contents", "contents")
    for text, anchor in contents_items:
        book.line(f"- [{text}](#{anchor})")
    book.line("")
    if extra_contents:
        book.para(extra_contents)
    book.line("---")


def main():
    import json
    import lexicon_themes as TH
    T = json.load(open(os.path.join(DATA, "lexicon-texts.json"), encoding="utf-8"))
    entries = load_entries()
    allw = sorted((x for v in entries.values() for x in v), key=lambda x: x[0])
    total = len(allw)
    groups = TH.assign(allw)
    assert not groups.get("leftover"), groups.get("leftover")
    themes = [(s, title, intro) for s, title, intro, _w in TH.THEMES]
    others = {
        "greek_lexicon_a_c": "Greek Lexicon A\u2013C", "greek_lexicon_d_f": "Greek Lexicon D\u2013F",
        "thematic_greek_lexicon": "Thematic Greek Lexicon", "complete_greek_lexicon": "Complete Greek Lexicon",
    }

    def next_links(skip):
        return "\n".join(f"- [{name}](book:{bid}#contents)" for bid, name in others.items() if bid != skip)

    def theme_pages(book, prefix="", with_intro=True):
        for s, title, intro in themes:
            book.page(title, prefix + s)
            if with_intro:
                book.para(f"*{intro}*")
            for e in sorted(groups[s], key=lambda x: norm(x[2])):
                book.para(entry_line(e))

    # ---------------------------------------------------------------- A-C und D-F
    for bid, keys, key in (("greek_lexicon_a_c", ("alpha", "beta", "gamma"), "a_c"),
                           ("greek_lexicon_d_f", ("delta", "epsilon", "zeta"), "d_f")):
        n = sum(len(entries[k]) for k in keys)
        b = Book()
        front(b, T, key, [("A few words before the words", "preface")], letters_line(keys) + "\n\n"
              + next_links(bid))
        b.page(T[key]["preface_title"], "preface")
        paragraphs(b, T[key]["preface"].replace("{n}", str(n)))
        letter_pages(b, entries, keys)
        b.page("What to Read Next")
        paragraphs(b, T[key]["outro"])
        b.para(next_links(bid))
        b.para(T[key]["signoff"])
        open(os.path.join(BOOKS, bid + ".md"), "w", encoding="utf-8", newline="\n").write(b.text())
        print(bid, n, "Eintraege", b.n, "Abschnitte")

    # ---------------------------------------------------------------- Thematic
    b = Book()
    front(b, T, "thematic", [("A few words before the words", "preface")] + [(title, s) for s, title, _i in themes],
          next_links("thematic_greek_lexicon"))
    b.page(T["thematic"]["preface_title"], "preface")
    paragraphs(b, T["thematic"]["preface"].replace("{n}", str(total)))
    theme_pages(b)
    b.page("What to Read Next")
    paragraphs(b, T["thematic"]["outro"])
    b.para(next_links("thematic_greek_lexicon"))
    b.para(T["thematic"]["signoff"])
    open(os.path.join(BOOKS, "thematic_greek_lexicon.md"), "w", encoding="utf-8", newline="\n").write(b.text())
    print("thematic", total, "Eintraege", b.n, "Abschnitte")

    # ---------------------------------------------------------------- Complete
    b = Book()
    cont = [("A few words (the long version)", "preface"), ("Entry key", "entry-key")]
    cont += [("Part One: The words from alpha to omega", "alpha"), ("Part Two: The words by theme", "theme-" + themes[0][0])]
    front(b, T, "complete", cont, letters_line({k for k, _, _ in LETTERS}))
    b.page(T["complete"]["preface_title"], "preface")
    paragraphs(b, T["complete"]["preface"].replace("{n}", str(total)))
    b.page("Entry Key", "entry-key")
    b.para(T["complete"]["key_intro"].replace("{n}", str(total)))
    b.line("| Mark | Meaning |")
    b.line("|---|---|")
    for mark, meaning in (("m. / f. / n.", "Masculine / feminine / neuter noun"), ("v.", "Verb"), ("adj. / adv.", "Adjective / adverb"),
                          ("prep. / conj.", "Preposition / conjunction"), ("pron. / art.", "Pronoun / article"),
                          ("part. / interj.", "Particle / interjection"), ("name", "Proper name")):
        b.line(f"| {mark} | {meaning} |")
    b.line("")
    b.page("Part One", "part-one")
    paragraphs(b, T["complete"]["part_one"])
    letter_pages(b, entries, {k for k, _, _ in LETTERS})
    b.page("Part Two", "part-two")
    paragraphs(b, T["complete"]["part_two"])
    theme_pages(b, "theme-")
    b.page("One Last Word")
    paragraphs(b, T["complete"]["outro"])
    b.para(T["complete"]["signoff"])
    open(os.path.join(BOOKS, "complete_greek_lexicon.md"), "w", encoding="utf-8", newline="\n").write(b.text())
    print("complete", total, "Eintraege", b.n, "Abschnitte")


if __name__ == "__main__":
    main()
