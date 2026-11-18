"""Erzeugt die Blocktextur fuer den Amphoren-Scherben-Block (16x16, nahtlos kachelbar).

Aufruf: python art/gen_sherd_block.py
Ausgabe: art/out/amphora_sherds.png (Textur) und art/out/amphora_sherds-preview.png (3x3 gekachelt, 6x vergroessert)

Aufbau: ein Haufen Tonscherben. Die Scherben sind Voronoi-Zellen (mit Umlauf, damit die Kante nahtlos ist),
zwischen ihnen liegen dunkle Fugen. Manche Scherben tragen schwarze Glasurstreifen oder einen roten Streifen,
einzelne zeigen die helle Innenseite. Farben: art/palette.json plus vier gebleichte Tonfarben, Glasur und Rot (nicht in der Palette).
"""
import json
import os
import random
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SEED = 20261118
N = 16
CELLS = 8

_raw = json.load(open(os.path.join(ROOT, "art/palette.json")))
P = {k: tuple(int(v[i:i + 2], 16) for i in (1, 3, 5)) + (255,)
     for k, v in _raw.items() if isinstance(v, str) and v.startswith("#")}
CLAY = [(96, 76, 62, 255), (168, 136, 108, 255), (198, 168, 136, 255), (226, 204, 172, 255)]  # gebleichter Ton: Fuge, mittel, hell, Glanz
GLAZE = (58, 54, 56, 255)      # verblasste Glasur
FADED_RED = (150, 96, 80, 255)  # verblasstes Rot


def torus_d2(ax, ay, bx, by):
    dx = min(abs(ax - bx), N - abs(ax - bx))
    dy = min(abs(ay - by), N - abs(ay - by))
    return dx * dx + dy * dy


def build():
    rng = random.Random(SEED)
    # Punkte gleichmaessiger verteilt als reiner Zufall (Mindestabstand), sonst gibt es Riesen- und Zwergscherben
    pts = []
    while len(pts) < CELLS:
        c = (rng.uniform(0, N), rng.uniform(0, N))
        if all(torus_d2(c[0], c[1], q[0], q[1]) > 13 for q in pts):
            pts.append(c)
    import math
    shape = [(rng.uniform(0, math.pi), rng.uniform(1.0, 1.9)) for _ in range(CELLS)]  # Drehung, Streckung
    style = {}
    kinds = ["clay"] * 4 + ["band"] * 2 + ["red", "inner"]
    rng.shuffle(kinds)
    for i in range(CELLS):
        style[i] = (kinds[i], rng.choice([1, 2, 2]))  # (Art, Tonstufe)
    im = Image.new("RGBA", (N, N))
    cell = {}
    for y in range(N):
        for x in range(N):
            def dist2(i, px, py):
                dx = ((x + .5 - px + N / 2) % N) - N / 2
                dy = ((y + .5 - py + N / 2) % N) - N / 2
                th, st = shape[i]
                u = dx * math.cos(th) + dy * math.sin(th)
                v = -dx * math.sin(th) + dy * math.cos(th)
                return (u / st) ** 2 + (v * st) ** 2   # gestreckt entlang einer Achse -> Scherben statt Kiesel
            d = sorted((dist2(i, px, py), i) for i, (px, py) in enumerate(pts))
            (d0, i0), (d1, _) = d[0], d[1]
            cell[(x, y)] = (i0, d0, d1)
    for (x, y), (i, d0, d1) in cell.items():
        kind, tone = style[i]
        gap = (d1 ** .5 - d0 ** .5) < 0.85  # nahe an der Grenze zur Nachbarzelle
        if gap:
            im.putpixel((x, y), CLAY[0])
            continue
        col = CLAY[tone]
        px, py = pts[i]
        # Licht von links oben: Pixel links/oberhalb des Zellmittelpunkts heller, rechts/unten dunkler
        dx = ((x + .5 - px + N / 2) % N) - N / 2
        dy = ((y + .5 - py + N / 2) % N) - N / 2
        if dx + dy < -2.2:
            col = CLAY[min(3, tone + 1)]
        elif dx + dy > 2.6:
            col = CLAY[max(1, tone - 1)]
        if kind == "inner":
            col = P["parchment_2"] if dx + dy > 0 else P["parchment_3"]
        elif kind == "band":
            r = (y + i * 3) % 8  # Streifen quer ueber die Scherbe, an Zelle gebunden
            if r in (3, 4):
                col = GLAZE
            elif r in (2, 5):
                col = CLAY[3]  # helle Ritzlinie ober- und unterhalb des Bandes
        elif kind == "red":
            if (x - y + i) % 6 in (0, 1):
                col = FADED_RED
        im.putpixel((x, y), col)
    # Ein paar Splitter-Lichter
    for _ in range(5):
        x, y = rng.randrange(N), rng.randrange(N)
        if im.getpixel((x, y)) in (CLAY[2], CLAY[3]):
            im.putpixel((x, y), CLAY[3])
    return im


def main():
    im = build()
    out = os.path.join(ROOT, "art/out")
    os.makedirs(out, exist_ok=True)
    im.save(os.path.join(out, "amphora_sherds.png"))
    S = 6
    t = Image.new("RGBA", (N * 3, N * 3))
    for i in range(3):
        for j in range(3):
            t.paste(im, (i * N, j * N))
    t.resize((N * 3 * S, N * 3 * S), Image.NEAREST).save(os.path.join(out, "amphora_sherds-preview.png"))
    print("ok")


if __name__ == "__main__":
    main()
