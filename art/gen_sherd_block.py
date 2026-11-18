"""Erzeugt die Blocktextur fuer den Scherbenhaufen (16x16, nahtlos kachelbar).

Aufruf: python art/gen_sherd_block.py
Ausgabe: art/out/amphora_sherds.png (Textur) und art/out/amphora_sherds-preview.png (3x3 gekachelt, 6x vergroessert)

Stil: Vanilla-nah. Minecraft-Bloecke wie Kies oder Terrakotta haben wenige, eng beieinanderliegende Toene und ein
leichtes Pixelrauschen, keine harten Kontraste. Darum: viele kleine Scherben (Voronoi-Zellen mit Umlauf, damit die Kante
nahtlos ist), nur schwach dunklere Fugen, Licht von links oben ueber die Tonstufe, und auf zwei Scherben ein blasser
Tintenrest (ein bis zwei Pixel), mehr nicht. Farben: gebleichter Ton, nicht aus art/palette.json, weil Vanilla-Toene gebraucht werden.
"""
import math
import os
import random
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SEED = 20261118
N = 16
CELLS = 12

# Gebleichter Ton in fuenf nahen Stufen: Fuge, dunkel, mittel, hell, etwas heller (kein Glanzweiss)
CLAY = [(118, 93, 77), (142, 112, 92), (156, 125, 102), (169, 137, 112), (180, 148, 122)]
INK = (96, 80, 74)  # blasser Tintenrest, kaum dunkler als die Fuge


def torus_d2(ax, ay, bx, by):
    dx = min(abs(ax - bx), N - abs(ax - bx))
    dy = min(abs(ay - by), N - abs(ay - by))
    return dx * dx + dy * dy


def build():
    rng = random.Random(SEED)
    pts = []
    while len(pts) < CELLS:  # gleichmaessiger als reiner Zufall (Mindestabstand), sonst Riesen- und Zwergscherben
        c = (rng.uniform(0, N), rng.uniform(0, N))
        if all(torus_d2(c[0], c[1], q[0], q[1]) > 9 for q in pts):
            pts.append(c)
    shape = [(rng.uniform(0, math.pi), rng.uniform(1.0, 1.6)) for _ in range(CELLS)]  # Drehung, Streckung
    tone = [rng.choice([1, 2, 2, 3]) for _ in range(CELLS)]
    inked = set(rng.sample(range(CELLS), 2))  # nur zwei Scherben tragen einen Tintenrest

    def dist2(i, x, y):
        px, py = pts[i]
        dx = ((x + .5 - px + N / 2) % N) - N / 2
        dy = ((y + .5 - py + N / 2) % N) - N / 2
        th, st = shape[i]
        u = dx * math.cos(th) + dy * math.sin(th)
        v = -dx * math.sin(th) + dy * math.cos(th)
        return (u / st) ** 2 + (v * st) ** 2

    im = Image.new("RGBA", (N, N))
    for y in range(N):
        for x in range(N):
            d = sorted((dist2(i, x, y), i) for i in range(CELLS))
            (d0, i0), (d1, _) = d[0], d[1]
            if (d1 ** .5 - d0 ** .5) < 0.7:   # Fuge zwischen zwei Scherben
                col = CLAY[0]
            else:
                px, py = pts[i0]
                dx = ((x + .5 - px + N / 2) % N) - N / 2
                dy = ((y + .5 - py + N / 2) % N) - N / 2
                t = tone[i0]
                if dx + dy < -1.8:            # Licht von links oben
                    t = min(4, t + 1)
                elif dx + dy > 2.2:
                    t = max(1, t - 1)
                col = CLAY[t]
                if i0 in inked and abs(dx + dy * .5) < 1.1 and abs(dy) < 1.6 and (x * 3 + y) % 4 == 0:
                    col = INK                 # kurzer, blasser Strich
            # Pixelrauschen wie bei Kies: kleine Helligkeitsabweichung pro Pixel
            j = rng.choice([-5, -3, 0, 0, 0, 3, 5])
            im.putpixel((x, y), tuple(max(0, min(255, c + j)) for c in col) + (255,))
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
