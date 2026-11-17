"""Erzeugt Zustands- und Eis-Varianten der Manuskript-Fragmente aus den vorhandenen Basis-Texturen.

Aufruf: python art/gen_fragment_variants.py
Ausgabe: art/out/variants/*.png (16x16) und art/out/fragment-variants-preview.png (Vorschau, 8x vergroessert)

Regeln
  - Die Basisform und die Textlinien bleiben, nur Farben und einzelne Pixel aendern sich.
  - Farben stammen aus art/palette.json. Ausnahme: die vier Eisfarben unten (die Palette hat kein Blau).
  - Zufall haengt nur vom SEED ab: gleiche Eingabe, gleiche Ausgabe.
"""
import json
import os
import random
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "mod/src/main/resources/assets/scribery/textures/item")
OUT = os.path.join(ROOT, "art/out/variants")
SEED = 20261117

_raw = json.load(open(os.path.join(ROOT, "art/palette.json")))
P = {k: tuple(int(v[i:i + 2], 16) for i in (1, 3, 5)) + (255,)
     for k, v in _raw.items() if isinstance(v, str) and v.startswith("#")}
ICE = {  # nicht in der Palette: nur fuer die gefrorenen Fragmente
    "ice_0": (47, 86, 140, 255), "ice_1": (103, 156, 210, 255),
    "ice_2": (166, 206, 238, 255), "ice_3": (233, 245, 252, 255), "ice_edge": (28, 52, 92, 255),
}

# Rollen der Basisfarben
ROLE = {(90, 58, 34): "edge", (227, 200, 143): "fill", (143, 116, 70): "line", (241, 226, 184): "hi"}


def load(n):
    """Liefert dict (x, y) -> Rolle fuer alle nicht transparenten Pixel."""
    im = Image.open(os.path.join(TEX, f"manuscript_fragment_{n}.png")).convert("RGBA")
    px = {}
    for y in range(16):
        for x in range(16):
            p = im.getpixel((x, y))
            if p[3]:
                px[(x, y)] = ROLE[p[:3]]
    return px


def render(px, colors):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y), v in px.items():
        im.putpixel((x, y), v if isinstance(v, tuple) else colors[v])
    return im


def inner_edge(px):
    """Fuellpixel, die direkt an einen Randpixel grenzen."""
    out = []
    for (x, y), r in px.items():
        if r in ("fill", "hi", "line") and any(px.get((x + dx, y + dy)) == "edge"
                                               for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            out.append((x, y))
    return out


def charred(n):
    rng = random.Random(SEED + n)
    px = dict(load(n))
    c = {"edge": P["ink_1"], "fill": P["wood_2"], "line": P["wood_0"], "hi": P["wood_3"]}
    rim = inner_edge(px)
    for p in rng.sample(rim, len(rim) * 2 // 3):  # verkohlter Rand
        px[p] = P["wood_1"]
    for p in [q for q, r in px.items() if r == "edge"]:  # angefressene Ecken
        if rng.random() < 0.12:
            del px[p]
    # Glutflecken: wenige helle Pixel im Inneren
    inner = [q for q, r in px.items() if r in ("fill", "hi") and q not in rim]
    for p in rng.sample(inner, 2):
        px[p] = P["parchment_1"]
    return render(px, c)


def water(n):
    rng = random.Random(SEED + 100 + n)
    px = dict(load(n))
    c = {k: P[v] for k, v in {"edge": "wood_1", "fill": "parchment_2", "line": "parchment_0", "hi": "parchment_3"}.items()}
    ys = sorted({y for (_, y) in px})
    cut = ys[len(ys) * 3 // 5]
    for (x, y), r in list(px.items()):
        if r == "edge":
            continue
        if y > cut:  # Wasserrand: unterer Teil dunkler
            px[(x, y)] = P["wood_3"] if (r == "line" or y == cut + 1) else P["parchment_1"]
    for (x, y), r in list(px.items()):  # Tinte verlaeuft: manche Linienpixel verschwinden
        if r == "line" and rng.random() < 0.35:
            px[(x, y)] = P["parchment_1"] if y > cut else "fill"
    wet = [q for q, r in px.items() if r != "edge" and q[1] > cut]
    for p in rng.sample(wet, 3):  # Tropfen
        px[p] = P["lead_3"]
    return render(px, c)


def faded(n):
    rng = random.Random(SEED + 200 + n)
    px = dict(load(n))
    c = {"edge": P["wood_3"], "fill": P["parchment_3"], "line": P["parchment_1"], "hi": P["parchment_3"]}
    for (x, y), r in list(px.items()):  # Schrift loest sich auf
        if r == "line" and rng.random() < 0.3:
            px[(x, y)] = "fill"
    for (x, y), r in list(px.items()):  # Flecken
        if r == "fill" and rng.random() < 0.08:
            px[(x, y)] = P["parchment_2"]
    return render(px, c)


def codex(n):
    px = dict(load(n))
    c = {k: P[v] for k, v in {"edge": "wood_1", "fill": "parchment_2", "line": "parchment_0", "hi": "parchment_3"}.items()}
    ys = sorted({y for (_, y) in px})
    gmin = min(x for (x, y), r in px.items() if r != "edge")
    for (x, y), r in list(px.items()):
        if r == "edge":
            continue
        row = sorted(xx for (xx, yy), rr in px.items() if yy == y and rr != "edge")
        if row and x == row[0] and x <= gmin + 1:  # gerader Buchruecken, keine Zacken
            px[(x, y)] = P["rubric"]
    first = next((y for y in ys if any(r == "line" for (xx, yy), r in px.items() if yy == y)), None)
    if first is not None:
        xs = sorted(x for (x, y), r in px.items() if y == first and r == "line")
        if xs:
            px[(xs[0], first)] = P["rubric"]
    return render(px, c)


def frozen(n):
    """Gefrorenes Fragment: gleiche Form und Textlinien, Papier in Eisblau. Dient auch als Overlay-Textur im Eisblock."""
    rng = random.Random(SEED + 300 + n)
    px = dict(load(n))
    c = {"edge": ICE["ice_edge"], "fill": ICE["ice_2"], "line": ICE["ice_0"], "hi": ICE["ice_3"]}
    for p in [q for q, r in px.items() if r == "edge"]:  # Reif auf dem Rand
        if rng.random() < 0.3:
            px[p] = ICE["ice_0"]
    inner = [q for q, r in px.items() if r in ("fill", "hi")]
    for p in rng.sample(inner, 4):  # Frostkristalle
        px[p] = ICE["ice_3"]
    return render(px, c)


def main():
    os.makedirs(OUT, exist_ok=True)
    items = {}
    for n, name in ((1, "a"), (5, "b"), (3, "strip")):
        items[f"charred_{name}"] = charred(n)
        items[f"water_{name}"] = water(n)
        items[f"faded_{name}"] = faded(n)
    items["codex_b"] = codex(5)
    items["codex_a"] = codex(1)
    for n in (1, 2, 3, 4, 5):
        items[f"frozen_{n}"] = frozen(n)
    for k, im in items.items():
        im.save(os.path.join(OUT, f"{k}.png"))

    # Vorschau: Zeile 1 Basis, Zeile 2 Zustaende, Zeile 3 Eis. Hintergrund dunkel, 8x
    S, G = 8, 6
    rows = [
        ["base1", "base5", "base3", "codex_a", "codex_b"],
        ["charred_a", "charred_b", "charred_strip", "water_a", "water_b", "water_strip", "faded_a", "faded_b", "faded_strip"],
        ["frozen_1", "frozen_2", "frozen_3", "frozen_4", "frozen_5"],
    ]
    base = {"base1": Image.open(os.path.join(TEX, "manuscript_fragment_1.png")).convert("RGBA"),
            "base5": Image.open(os.path.join(TEX, "manuscript_fragment_5.png")).convert("RGBA"),
            "base3": Image.open(os.path.join(TEX, "manuscript_fragment_3.png")).convert("RGBA")}
    W = max(len(r) for r in rows) * (16 * S + G) + G
    sheet = Image.new("RGBA", (W, len(rows) * (16 * S + G) + G), (38, 38, 42, 255))
    for ri, r in enumerate(rows):
        for ci, k in enumerate(r):
            im = base[k] if k in base else items[k]
            b = im.resize((16 * S, 16 * S), Image.NEAREST)
            sheet.alpha_composite(b, (G + ci * (16 * S + G), G + ri * (16 * S + G)))
    sheet.save(os.path.join(ROOT, "art/out/fragment-variants-preview.png"))
    print(len(items), "Varianten ->", OUT)


if __name__ == "__main__":
    main()
