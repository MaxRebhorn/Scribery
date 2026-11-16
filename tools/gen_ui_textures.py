"""Erzeugt die ausgefransten Papier-Texturen (nur Farben aus art/palette.json). Alles andere nutzt Minecraft-Texturen.

Aufruf: python tools/gen_ui_textures.py
Ausgabe: mod/src/main/resources/assets/scribery/textures/gui/*.png (Groesse = Rect im Layout, 1 px = 1 GUI-Pixel)
Dynamisches (Buchstaben, Zahlen, Fuellstaende, Slots) wird im Code darueber gezeichnet und steht nicht hier.
"""
import json, os, random
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "mod/src/main/resources/assets/scribery/textures/gui")
PAL = {k: tuple(int(v[i:i + 2], 16) for i in (1, 3, 5)) + (255,)
       for k, v in json.load(open(os.path.join(ROOT, "art/palette.json")))["colors"].items()} \
    if "colors" in json.load(open(os.path.join(ROOT, "art/palette.json"))) else None
if PAL is None:
    raw = json.load(open(os.path.join(ROOT, "art/palette.json")))
    PAL = {k: tuple(int(v[i:i + 2], 16) for i in (1, 3, 5)) + (255,) for k, v in raw.items() if isinstance(v, str) and v.startswith("#")}


def new(w, h):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def rect(im, x, y, w, h, c):
    px = im.load()
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            if 0 <= xx < im.width and 0 <= yy < im.height:
                px[xx, yy] = PAL[c]


def dot(im, x, y, c):
    rect(im, x, y, 1, 1, c)


def grain(im, x, y, w, h, base, light, dark, seed, density=0.9, horizontal=True):
    """Maserung: Grundfarbe mit kurzen Streifen in hellerem und dunklerem Ton."""
    rng = random.Random(seed)
    rect(im, x, y, w, h, base)
    for _ in range(int(w * h * density / 14)):
        gx, gy = x + rng.randrange(w), y + rng.randrange(h)
        ln = rng.randrange(3, 9)
        c = light if rng.random() < 0.5 else dark
        for k in range(ln):
            if horizontal and gx + k < x + w:
                dot(im, gx + k, gy, c)
            elif not horizontal and gy + k < y + h:
                dot(im, gx, gy + k, c)


def bevel(im, x, y, w, h, hi, lo):
    rect(im, x, y, w, 1, hi); rect(im, x, y, 1, h, hi)
    rect(im, x, y + h - 1, w, 1, lo); rect(im, x + w - 1, y, 1, h, lo)


def nail(im, x, y):
    rect(im, x, y, 3, 3, "lead_1"); rect(im, x, y, 2, 2, "lead_3"); dot(im, x + 2, y + 2, "lead_0")


def wood_frame(w, h, t, inner, seed, nails=True):
    """Holzrahmen der Staerke t um eine vertiefte Flaeche der Farbe inner."""
    im = new(w, h)
    grain(im, 0, 0, w, h, "wood_2", "wood_3", "wood_1", seed)
    bevel(im, 1, 1, w - 2, h - 2, "wood_4", "wood_1")
    rect(im, 0, 0, w, 1, "wood_0"); rect(im, 0, h - 1, w, 1, "wood_0")
    rect(im, 0, 0, 1, h, "wood_0"); rect(im, w - 1, 0, 1, h, "wood_0")
    rect(im, t, t, w - 2 * t, h - 2 * t, inner)
    # vertiefter Rand der Flaeche: oben/links dunkel, unten/rechts hell
    rect(im, t, t, w - 2 * t, 1, "wood_0"); rect(im, t, t, 1, h - 2 * t, "wood_0")
    rect(im, t, h - t - 1, w - 2 * t, 1, "wood_3"); rect(im, w - t - 1, t, 1, h - 2 * t, "wood_3")
    rect(im, t + 1, t + 1, w - 2 * t - 2, 1, "ink_1")
    if nails and t >= 4:
        for nx, ny in ((1, 1), (w - 4, 1), (1, h - 4), (w - 4, h - 4)):
            nail(im, nx, ny)
    return im


def ragged(w, h, seed, max_lr, max_tb, chips=6):
    """Pergament mit ausgefranstem Rand: pro Kante ein zufaelliger Verlauf der Tiefe, dazu ein paar Ausbrueche."""
    rng = random.Random(seed)
    def profile(n, mx):
        d, out = rng.randrange(mx + 1), []
        for _ in range(n):
            if rng.random() < 0.45:
                d = max(0, min(mx, d + rng.choice((-1, 1))))
            out.append(d)
        return out
    top, bot = profile(w, max_tb), profile(w, max_tb)
    left, right = profile(h, max_lr), profile(h, max_lr)
    solid = [[True] * w for _ in range(h)]
    for x in range(w):
        for y in range(h):
            if y < top[x] or y >= h - bot[x] or x < left[y] or x >= w - right[y]:
                solid[y][x] = False
    for _ in range(chips):  # kleine Ausbrueche
        cx, cy = rng.randrange(2, w - 2), rng.choice((rng.randrange(0, 3), rng.randrange(h - 3, h)))
        for dx in range(rng.randrange(1, 4)):
            for dy in range(rng.randrange(1, 3)):
                if 0 <= cx + dx < w and 0 <= cy + dy < h:
                    solid[cy + dy][cx + dx] = False
    im = new(w, h)
    px = im.load()
    def at(x, y):
        return 0 <= x < w and 0 <= y < h and solid[y][x]
    for y in range(h):
        for x in range(w):
            if not solid[y][x]:
                continue
            edge = not (at(x - 1, y) and at(x + 1, y) and at(x, y - 1) and at(x, y + 1))
            near = not (at(x - 2, y) and at(x + 2, y) and at(x, y - 2) and at(x, y + 2))
            px[x, y] = PAL["parchment_0" if edge else "parchment_1" if near else "parchment_3"]
    for _ in range(w * h // 40):  # Fasern
        x, y = rng.randrange(w), rng.randrange(h)
        for k in range(rng.randrange(2, 6)):
            if at(x + k, y) and px[x + k, y] == PAL["parchment_3"]:
                px[x + k, y] = PAL["parchment_2"]
    return im


def backdrop():
    """Tischplatte: lange glatte Bretter in zwei nahen Holztoenen, feine Fugen, sparsame Maserung. Keine Naegel, kein Rahmen."""
    w, h = 384, 216
    rng = random.Random(21)
    im = new(w, h)
    lip = 6
    bh = 30
    tones = ["wood_2", "wood_3", "wood_2", "wood_3", "wood_2", "wood_3", "wood_2"]
    y = 0
    for i in range(7):
        top = y
        bot = min(y + bh, h - lip)
        base = tones[i]
        light = "wood_3" if base == "wood_2" else "wood_4"
        dark = "wood_1" if base == "wood_2" else "wood_2"
        grain(im, 0, top, w, bot - top, base, light, dark, 100 + i, density=0.35)
        rect(im, 0, bot - 1, w, 1, "wood_1")
        rect(im, 0, top, w, 1, light)
        seam = rng.randrange(80, w - 80)
        rect(im, seam, top + 1, 1, bot - top - 2, "wood_1")
        y = bot
    # Tischkante unten
    rect(im, 0, h - lip, w, lip, "wood_1")
    rect(im, 0, h - lip, w, 1, "wood_3")
    rect(im, 0, h - lip + 1, w, 1, "wood_2")
    rect(im, 0, h - 1, w, 1, "wood_0")
    return im


def tablet():
    """Wooden Word Tablet: kleines Holztaefelchen mit Schnurloch und eingeritzten Zeilen (16x16 Item)."""
    im = new(16, 16)
    rect(im, 2, 3, 12, 10, "wood_0")
    grain(im, 3, 4, 10, 8, "wood_3", "wood_4", "wood_2", 5, density=1.2)
    rect(im, 3, 4, 10, 1, "wood_4")
    rect(im, 3, 11, 10, 1, "wood_2")
    rect(im, 12, 4, 1, 8, "wood_2")
    for x, y in ((2, 3), (13, 3), (2, 12), (13, 12)):
        im.load()[x, y] = (0, 0, 0, 0)
    rect(im, 4, 5, 2, 2, "wood_0")  # Schnurloch
    rect(im, 4, 5, 1, 1, "wood_1")
    rect(im, 7, 6, 4, 1, "wood_1")
    rect(im, 5, 8, 6, 1, "wood_1")
    rect(im, 5, 10, 4, 1, "wood_1")
    return im


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    allowed = {v for v in PAL.values()} | {(0, 0, 0, 0)}
    for name, fn in [("backdrop", backdrop)]:
        im = fn()
        bad = {p for p in im.getdata() if p not in allowed}
        assert not bad, (name, bad)
        im.save(os.path.join(OUT, name + ".png"))
        print(name, im.size)
    item_dir = os.path.join(ROOT, "mod/src/main/resources/assets/scribery/textures/item")
    t = tablet()
    t.save(os.path.join(item_dir, "wooden_word_tablet.png"))
    print("wooden_word_tablet", t.size)
