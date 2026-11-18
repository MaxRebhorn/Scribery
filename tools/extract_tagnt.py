"""Zieht aus der STEPBible-TAGNT-Datei (Mat-Jhn) die Johannes-Woerter im Rohformat von tools/data/john-1-1-18.tsv.

Aufruf: python extract_tagnt.py <TAGNT-Datei> <von-Vers> <bis-Vers> <ausgabe.tsv>
Textgrundlage: Woerter, die in NA28 stehen (Nestle-Aland). Die Pilotdaten (Joh 1,1-18) weichen an einer Stelle ab (1,18: \"ὁ\" vor μονογενής, steht nur in Tyn/TR/Byz). Reine TR/Byz-Zusaetze (z. B. Joh 5,3b-4) fallen weg.
"""
import re, sys, unicodedata
src, v0, v1, out = sys.argv[1:5]
def key(ref):  # "Jhn.7.53{8.1}#01=KO" -> (kapitel, vers, nr)
    m = re.match(r"Jhn\.(\d+)\.(\d+)(?:\{[^}]*\})?#(\d+)", ref); return tuple(int(x) for x in m.groups())
lo = tuple(int(x) for x in v0.split(":")) ; hi = tuple(int(x) for x in v1.split(":"))
rows = []
for l in open(src, encoding="utf-8-sig"):
    if not l.startswith("Jhn."): continue
    c = l.rstrip("\n").split("\t")
    ch, vs, nr = key(c[0])
    if not (lo <= (ch, vs) <= hi): continue
    gloss = c[2]; ds_morph = c[3]
    if "NA28" not in c[5].split("+"):
        # Haupttext ist eine TR/Byz/Tyn-Lesart, die NA28-Lesart steht nur in der Variantenspalte (nur in der Perikope Joh 7,53-8,11)
        m = re.match(r"^(\S+) \([^)]*\) (.*) - (G\d+\w?=\S+) in: (.*)$", c[6])
        if not m or "NA28" not in m.group(4).split("+"): continue
        c[1], gloss, ds_morph = m.group(1) + " (x)", m.group(2), m.group(3)
    greek = c[1].split(" (")[0]
    greek = unicodedata.normalize("NFC", greek).strip()  # NFC: Hochpunkt U+0387 -> U+00B7, griech. Fragezeichen U+037E -> ";", wie in den Pilotdaten
    greek = greek.replace("¶", "").replace("[[", "").replace("]]", "").replace("[", "").replace("]", "")
    ds, morph = ds_morph.split("=", 1)
    rows.append((ch, vs, nr, greek, ds, gloss, morph))
with open(out, "w", encoding="utf-8") as f:
    f.write("# Quelle: STEPBible TAGNT (Tyndale House Cambridge / STEPBible.org, CC BY 4.0), Auszug Johannes, Textgrundlage NA28\n")
    for ch, vs, nr, g, ds, gl, m in rows:
        f.write(f"{ch}:{vs}\t{g}\t{ds}\t{gl}\t{m}\n")
print(len(rows), "Woerter")
