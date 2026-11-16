"""Erzeugt die statischen UI-Texturen (nur Farben aus art/palette.json) in Minecraft-Optik.

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


def forme():
    return wood_frame(184, 61, 4, "wood_0", 11)


def paper_tray():
    return wood_frame(184, 59, 2, "wood_0", 12)


def platen():
    im = new(184, 16)
    rect(im, 0, 0, 184, 16, "lead_1")
    rect(im, 0, 0, 184, 1, "lead_0"); rect(im, 0, 15, 184, 1, "lead_0")
    rect(im, 0, 0, 1, 16, "lead_0"); rect(im, 183, 0, 1, 16, "lead_0")
    rect(im, 1, 1, 182, 2, "lead_3")
    rect(im, 1, 3, 182, 1, "lead_2")
    rect(im, 4, 11, 176, 3, "lead_2")
    rect(im, 4, 13, 176, 1, "lead_0")
    rect(im, 1, 14, 182, 1, "lead_0")
    for x in (6, 90, 174):
        nail(im, x, 5)
    return im


def letter_case():
    im = new(74, 109)
    rect(im, 0, 0, 74, 109, "lead_1")
    bevel(im, 0, 0, 74, 109, "lead_3", "lead_0")
    rect(im, 1, 1, 72, 107, "lead_0")
    rect(im, 2, 2, 70, 105, "ink_0")
    for c in range(5):
        for r in range(5):
            x, y = 2 + c * 14, 2 + r * 21
            rect(im, x, y, 1, 21, "lead_1")
            rect(im, x, y + 20, 14, 1, "lead_1")
            rect(im, x + 1, y + 20, 13, 1, "lead_0")
    rect(im, 72, 2, 1, 105, "lead_1")
    return im


def barrel():
    w, h = 44, 42
    im = new(w, h)
    for x in range(w):
        c = "wood_3" if (x // 4) % 2 == 0 else "wood_2"
        rect(im, x, 0, 1, h, c)
        if x % 4 == 0:
            rect(im, x, 0, 1, h, "wood_1")
    rect(im, 0, 0, w, 1, "wood_0"); rect(im, 0, h - 1, w, 1, "wood_0")
    rect(im, 0, 0, 1, h, "wood_0"); rect(im, w - 1, 0, 1, h, "wood_0")
    rect(im, 1, 1, w - 2, 1, "wood_4"); rect(im, 1, 1, 1, h - 2, "wood_4")
    rect(im, 1, h - 2, w - 2, 1, "wood_1"); rect(im, w - 2, 1, 1, h - 2, "wood_1")
    for y in (8, h - 10):
        rect(im, 0, y, w, 2, "lead_2"); rect(im, 0, y, w, 1, "lead_3"); rect(im, 0, y + 1, w, 1, "lead_1")
        nail(im, 3, y - 1 if False else y); nail(im, w - 6, y)
    # Oeffnung: Rahmen um die Flaeche, die der Code mit dem Fuellstand fuellt
    rect(im, 3, 11, 38, 20, "lead_0")
    rect(im, 3, 11, 38, 1, "lead_0"); rect(im, 3, 30, 38, 1, "lead_3")
    rect(im, 4, 12, 36, 18, "barrel_full")
    # runde Ecken
    for x, y in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1), (1, 0), (w - 2, 0), (1, h - 1), (w - 2, h - 1),
                 (0, 1), (w - 1, 1), (0, h - 2), (w - 1, h - 2)):
        im.load()[x, y] = (0, 0, 0, 0)
    return im


def lectern():
    return wood_frame(78, 74, 6, "wood_1", 13)


def box():
    im = wood_frame(120, 46, 3, "wood_0", 14, nails=False)
    for y in (13, 23, 33):
        rect(im, 4, y, 112, 1, "wood_1")
    return im


def sheet():
    w, h = 152, 146
    im = new(w, h)
    rng = random.Random(15)
    rect(im, 0, 0, w, h, "parchment_3")
    for _ in range(260):
        x, y = rng.randrange(2, w - 2), rng.randrange(2, h - 2)
        for k in range(rng.randrange(2, 6)):
            if x + k < w - 2:
                dot(im, x + k, y, "parchment_2")
    rect(im, 0, 0, w, 1, "parchment_0"); rect(im, 0, h - 1, w, 1, "parchment_0")
    rect(im, 0, 0, 1, h, "parchment_0"); rect(im, w - 1, 0, 1, h, "parchment_0")
    rect(im, 1, 1, w - 2, 1, "parchment_2"); rect(im, 1, 1, 1, h - 2, "parchment_2")
    rect(im, 1, h - 2, w - 2, 1, "parchment_1"); rect(im, w - 2, 1, 1, h - 2, "parchment_1")
    for x, y in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
        im.load()[x, y] = (0, 0, 0, 0)
    return im


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    allowed = {v for v in PAL.values()} | {(0, 0, 0, 0)}
    for name, fn in [("forme", forme), ("paper_tray", paper_tray), ("platen", platen), ("letter_case", letter_case),
                     ("barrel", barrel), ("lectern", lectern), ("box", box), ("sheet", sheet)]:
        im = fn()
        bad = {p for p in im.getdata() if p not in allowed}
        assert not bad, (name, bad)
        im.save(os.path.join(OUT, name + ".png"))
        print(name, im.size)
