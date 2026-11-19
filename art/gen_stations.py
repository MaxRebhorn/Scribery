"""Erzeugt die Modelle und Texturen der Werkbloecke (Pult, Presse, Lettern-Schrank, Uebersetzungstisch, Minting Table).

Aufruf: python art/gen_stations.py
Ausgabe: mod/.../textures/block/station_*.png   (Materialkacheln, 16x16, nur Farben aus art/palette.json)
         mod/.../models/block/{scribe_lectern,printing_press,letter_cabinet,translator_desk,minting_table}.json
         art/out/stations-preview.png            (Vorschau aus mehreren Winkeln, eigener Software-Renderer)
         art/out/station_boxes.json              (Kollisionsformen, werden in StationShapes.java uebernommen)
Das Manuscript Archive bleibt ein Wuerfel und wird nicht angefasst.

Modelle: nur Quader ohne Drehung (damit nichts vom Vorzeichen der Modelldrehung abhaengt). Vorderseite = Norden (-z),
das Blockstate dreht je nach Blickrichtung. Ohne uv nimmt Minecraft die Position (1 Pixel = 1 Einheit), so passen Pixeldichte
und Material ueberall zusammen.
"""
import json
import math
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "mod/src/main/resources/assets/scribery")
OUT = os.path.join(ROOT, "art/out")
PAL = {k: tuple(int(v[i:i + 2], 16) for i in (1, 3, 5)) for k, v in json.load(open(os.path.join(ROOT, "art/palette.json")))
       .items() if k != "_comment"}
C = PAL

# ---------------------------------------------------------------- Texturen (16x16 Kacheln)

def tile(fn, seed):
    rng = random.Random(seed)
    im = Image.new("RGB", (16, 16))
    px = im.load()
    fn(px, rng)
    return im


def planks(base, seam, hi, grain):
    def f(px, rng):
        for y in range(16):
            for x in range(16):
                px[x, y] = base
        for y in range(0, 16, 4):                       # Fugen zwischen den Brettern
            for x in range(16):
                px[x, y] = seam
        for row in range(4):                            # Stossfugen versetzt je Brett
            x = (row * 7 + 3) % 16
            for y in range(row * 4 + 1, row * 4 + 4):
                px[x, y] = seam
        for _ in range(7):                              # Maserung (sparsam)
            x, y = rng.randrange(16), rng.randrange(16)
            if y % 4 == 0:
                continue
            for k in range(rng.randrange(2, 5)):
                if x + k < 16:
                    px[x + k, y] = grain if rng.random() < 0.55 else hi
        for row in range(4):
            y = row * 4 + 1
            for x in range(16):
                if px[x, y] == base and rng.random() < 0.22:
                    px[x, y] = hi
    return f


def metal(base, dark, light, seed_density=0.12):
    def f(px, rng):
        for y in range(16):
            for x in range(16):
                px[x, y] = base
        for i in range(16):
            px[i, 0] = light
            px[i, 15] = dark
            px[0, i] = light if i < 15 else dark
            px[15, i] = dark
        for _ in range(int(256 * seed_density)):
            px[rng.randrange(1, 15), rng.randrange(1, 15)] = rng.choice([dark, light])
    return f


def bricks(brick, mortar, hi):
    def f(px, rng):
        for y in range(16):
            for x in range(16):
                px[x, y] = brick
        for y in range(0, 16, 4):
            for x in range(16):
                px[x, y] = mortar
        for row in range(4):
            off = 0 if row % 2 == 0 else 4
            for x in range(off, 16, 8):
                for y in range(row * 4, row * 4 + 4):
                    px[x % 16, y] = mortar
        for _ in range(26):
            x, y = rng.randrange(16), rng.randrange(16)
            if px[x, y] == brick:
                px[x, y] = hi if rng.random() < 0.6 else C["lead_1"]
    return f


def paper(px, rng):
    for y in range(16):
        for x in range(16):
            px[x, y] = C["parchment_3"]
    for _ in range(22):
        px[rng.randrange(16), rng.randrange(16)] = C["parchment_2"]
    for i in range(16):
        px[i, 15] = C["parchment_2"]
        px[15, i] = C["parchment_2"]


def inkwell(px, rng):
    for y in range(16):
        for x in range(16):
            px[x, y] = C["ink_1"]
    for _ in range(30):
        px[rng.randrange(16), rng.randrange(16)] = C["ink_0"]
    for _ in range(6):
        px[rng.randrange(16), rng.randrange(16)] = C["lead_1"]


def molten(px, rng):
    for y in range(16):
        for x in range(16):
            px[x, y] = C["lead_3"]
    for _ in range(60):
        px[rng.randrange(16), rng.randrange(16)] = rng.choice([C["lead_4"], C["lead_4"], C["lead_2"]])


def mouth(px, rng):
    """Ofenmaul: Steinrahmen mit Glut."""
    for y in range(16):
        for x in range(16):
            px[x, y] = C["lead_1"]
    for y in range(3, 15):
        for x in range(3, 13):
            px[x, y] = C["ink_0"]
    for y in range(9, 15):                              # Glut unten
        for x in range(3, 13):
            d = 14 - y
            px[x, y] = C["flame_2"] if d < 2 else (C["flame_1"] if d < 4 and rng.random() < 0.6 else C["flame_2"] if d < 5 else C["ink_0"])
    for x in range(5, 11):
        if rng.random() < 0.6:
            px[x, 14] = C["flame_0"]
    for _ in range(14):
        px[rng.randrange(16), rng.randrange(16)] = C["lead_2"] if px[0, 0] == C["lead_1"] else px[0, 0]
    for x in range(16):
        px[x, 0] = C["lead_2"]
        px[x, 15] = C["lead_0"]


def cab_front(px, rng):
    """Fachkasten des Setzkastens: 3x3 Faecher, kleine Schilder."""
    for y in range(16):
        for x in range(16):
            px[x, y] = C["wood_1"]
    for cy in range(3):
        for cx in range(3):
            x0, y0 = 1 + cx * 5, 1 + cy * 5
            for y in range(y0, y0 + 4):
                for x in range(x0, x0 + 4):
                    px[x, y] = C["ink_1"] if y > y0 else C["ink_0"]
            px[x0 + 1, y0 + 3] = C["parchment_1"]
            px[x0 + 2, y0 + 3] = C["parchment_2"]
    for i in range(16):
        px[i, 0] = C["wood_2"]
        px[0, i] = C["wood_2"]


def form(px, rng):
    """Setzform: 4 Zeilen Lettern (je 2x3 Pixel), manche Plaetze leer."""
    for y in range(16):
        for x in range(16):
            px[x, y] = C["ink_1"]
    for r in range(4):
        for c in range(8):
            if rng.random() < 0.2:
                continue
            for y in range(r * 4, r * 4 + 3):
                for x in range(c * 2, c * 2 + 2):
                    px[x, y] = C["lead_3"] if y == r * 4 else C["lead_2"]
            px[c * 2 + rng.randrange(2), r * 4 + 1] = C["lead_4"]


def letterbox(px, rng):
    """Buchstabenbox: 4x4 Faecher mit Lettern."""
    for y in range(16):
        for x in range(16):
            px[x, y] = C["wood_0"]
    for r in range(4):
        for c in range(4):
            for y in range(r * 4, r * 4 + 3):
                for x in range(c * 4, c * 4 + 3):
                    px[x, y] = C["lead_2"]
            px[c * 4 + 1, r * 4 + 1] = C["lead_4"] if rng.random() < 0.7 else C["lead_3"]
            for k in range(3):
                px[c * 4 + k, r * 4] = C["lead_3"]


def flame(px, rng):
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) / 8
            px[x, y] = C["flame_2"] if (y > 9 or d > 0.55) else (C["flame_1"] if d > 0.25 or y > 4 else C["flame_0"])
    for _ in range(30):
        px[rng.randrange(16), rng.randrange(16)] = rng.choice([C["flame_0"], C["flame_1"], C["flame_2"]])


MATERIALS = {
    "form": form,
    "letterbox": letterbox,
    "flame": flame,
    "wood": planks(C["wood_2"], C["wood_1"], C["wood_3"], C["wood_1"]),
    "dark": planks(C["wood_1"], C["wood_0"], C["wood_2"], C["wood_0"]),
    "light": planks(C["wood_3"], C["wood_2"], C["wood_4"], C["wood_2"]),
    "iron": metal(C["lead_2"], C["lead_1"], C["lead_3"]),
    "irond": metal(C["lead_1"], C["lead_0"], C["lead_2"]),
    "stone": bricks(C["lead_1"], C["lead_0"], C["lead_2"]),
    "paper": paper,
    "ink": inkwell,
    "molten": molten,
    "mouth": mouth,
    "front": cab_front,
}
SEEDS = {k: 100 + i for i, k in enumerate(MATERIALS)}


def write_textures():
    tex = {}
    for name, fn in MATERIALS.items():
        im = tile(fn, SEEDS[name])
        im.save(os.path.join(RES, "textures/block", f"station_{name}.png"))
        tex[name] = im
    return tex


# ---------------------------------------------------------------- Modelle

def box(a, b, tex, **special):
    """a, b = (x1,y1,z1), (x2,y2,z2); tex = Material fuer alle Seiten; special: Seite -> (Material, uv oder None)."""
    return {"from": list(a), "to": list(b), "tex": tex, "special": special}


MODELS = {}
SHAPES = {}

MODELS["scribe_lectern"] = [
    box((3, 0, 3), (13, 2, 13), "dark"),
    box((4, 2, 4), (12, 3, 12), "wood"),
    box((6, 3, 6), (10, 11, 10), "wood"),
    box((5, 9, 5), (11, 10, 11), "dark"),
    box((1, 11, 1), (15, 13, 15), "wood", up=("light", None)),
    box((1, 13, 13), (15, 16, 15), "dark", north=("wood", None)),
    box((1, 13, 1), (2, 14, 2), "dark"),
    box((14, 13, 1), (15, 14, 2), "dark"),
]
SHAPES["scribe_lectern"] = [(3, 0, 3, 13, 3, 13), (6, 3, 6, 10, 11, 10), (1, 11, 1, 15, 13, 15), (1, 13, 13, 15, 16, 15)]

MODELS["translator_desk"] = [
    box((0, 11, 1), (16, 13, 15), "wood", up=("light", None)),
    box((1, 0, 2), (3, 11, 4), "dark"), box((13, 0, 2), (15, 11, 4), "dark"),
    box((1, 0, 12), (3, 11, 14), "dark"), box((13, 0, 12), (15, 11, 14), "dark"),
    box((1, 3, 4), (3, 4.5, 12), "dark"), box((13, 3, 4), (15, 4.5, 12), "dark"),
    box((3, 6, 3), (13, 11, 13), "wood"),
    box((7, 8, 2.5), (9, 9, 3), "iron"),
    box((0, 13, 13), (16, 15, 15), "dark"),
    box((12, 15, 13), (14, 16, 15), "ink"),
    box((2, 15, 13), (7, 16, 15), "paper"),
]
SHAPES["translator_desk"] = [(0, 11, 1, 16, 13, 15), (1, 0, 2, 3, 11, 4), (13, 0, 2, 15, 11, 4), (1, 0, 12, 3, 11, 14),
                             (13, 0, 12, 15, 11, 14), (3, 6, 3, 13, 11, 13), (0, 13, 13, 16, 16, 15)]

LEGS = [box((0, 0, 1), (2, 8, 3), "dark"), box((14, 0, 1), (16, 8, 3), "dark"),
        box((0, 0, 13), (2, 8, 15), "dark"), box((14, 0, 13), (16, 8, 15), "dark")]
TABLE = LEGS + [box((0, 8, 1), (16, 10, 15), "wood", up=("light", None)), box((2, 3, 3), (14, 4, 13), "dark")]

# Presse nach dem UI: Setzform in der Mitte, Papierablage davor, Tiegel (Platte) darueber, Kurbel links (aus Sicht des
# Spielers, also x hoch), Hebel rechts (x niedrig), Kerze hinten links.
MODELS["printing_press"] = TABLE + [
    box((3, 10, 4), (13, 11, 12), "irond", up=("form", [3, 4, 13, 12])),
    box((3, 10, 1.5), (13, 11, 3.5), "paper"),
    box((3, 11, 12), (4, 15, 13), "dark"), box((12, 11, 12), (13, 15, 13), "dark"),
    box((3, 15, 12), (13, 16, 13), "dark"),
    box((4.5, 13.5, 5), (11.5, 14.5, 11), "iron"),
    box((1.5, 10, 7.5), (2.5, 16, 8.5), "irond"), box((2.5, 13.5, 7.5), (4.5, 14.5, 8.5), "irond"),
    box((1, 15, 7), (3, 16, 9), "light"),
    box((13, 11.5, 7), (14, 12.5, 8), "irond"), box((14, 10.5, 5), (15, 15.5, 10), "iron"),
    box((15, 13, 8.5), (16, 14, 9.5), "light"),
    box((13, 10, 11), (14.5, 13, 12.5), "paper"), box((13.4, 13, 11.4), (14.1, 14, 12.1), "flame"),
]
SHAPES["printing_press"] = [(0, 0, 1, 16, 10, 15), (1, 10, 1, 16, 16, 13)]

# Lettern-Schrank: Tisch mit Buchstabenbox, Walze, Papiervorrat und dem Fass (Tinte) darauf.
MODELS["letter_cabinet"] = TABLE + [
    box((1, 10, 2), (9, 12, 14), "dark", up=("letterbox", [0, 0, 8, 12])),
    box((10, 10, 9), (15, 14, 14), "dark", up=("ink", None)),
    box((9.8, 10.5, 8.8), (15.2, 11.5, 14.2), "iron"), box((9.8, 12.5, 8.8), (15.2, 13.5, 14.2), "iron"),
    box((10, 10, 6), (15, 11.5, 8.5), "paper"),
    box((10.5, 10, 2.5), (14.5, 11.5, 5.5), "dark"),
    box((10, 10.2, 3), (10.5, 11.3, 5), "iron"), box((14.5, 10.2, 3), (15, 11.3, 5), "iron"),
]
SHAPES["letter_cabinet"] = [(0, 0, 1, 16, 10, 15), (1, 10, 2, 9, 12, 14), (10, 10, 9, 15, 14, 14), (10, 10, 2.5, 15, 11.5, 8.5)]

# Minting Table: Tiegel (Becher) auf einem Dreibein ueber offener Flamme, daneben ein kleiner Amboss.
MODELS["minting_table"] = [
    box((0, 0, 0), (16, 8, 16), "dark", north=("wood", None)),
    box((0, 8, 0), (16, 9, 16), "irond", up=("irond", None)),
    box((1.5, 9, 1.5), (8.5, 10, 8.5), "stone"),
    box((3, 10, 3), (7, 11.5, 7), "flame"), box((3.5, 11.5, 3.5), (6.5, 13, 6.5), "flame"), box((4.2, 13, 4.2), (5.8, 14, 5.8), "flame"),
    box((2, 10, 2), (3, 14, 3), "irond"), box((7, 10, 2), (8, 14, 3), "irond"),
    box((2, 10, 7), (3, 14, 8), "irond"), box((7, 10, 7), (8, 14, 8), "irond"),
    box((2, 14, 2), (8, 14.8, 8), "irond"),
    box((3, 14.8, 3), (7, 15.6, 7), "iron", up=("molten", None)),
    box((3, 15.6, 3), (7, 16, 3.5), "iron"), box((3, 15.6, 6.5), (7, 16, 7), "iron"),
    box((3, 15.6, 3.5), (3.5, 16, 6.5), "iron"), box((6.5, 15.6, 3.5), (7, 16, 6.5), "iron"),
    box((8, 15, 4.5), (10.5, 15.6, 5.5), "irond"),
    box((10, 9, 9), (15, 10, 14), "irond"),
    box((11.5, 10, 10), (13.5, 12, 13), "irond"),
    box((10, 12, 9), (16, 14, 14), "iron"),
    box((16, 12.5, 10.5), (16.01, 13.5, 12.5), "iron"),
]
SHAPES["minting_table"] = [(0, 0, 0, 16, 10, 16), (2, 10, 2, 8, 16, 8), (10, 9, 9, 16, 14, 14)]

FACES = ["north", "south", "east", "west", "up", "down"]


def auto_uv(face, a, b):
    (x1, y1, z1), (x2, y2, z2) = a, b
    return {"up": [x1, z1, x2, z2], "down": [x1, 16 - z2, x2, 16 - z1], "north": [16 - x2, 16 - y2, 16 - x1, 16 - y1],
            "south": [x1, 16 - y2, x2, 16 - y1], "west": [z1, 16 - y2, z2, 16 - y1], "east": [16 - z2, 16 - y2, 16 - z1, 16 - y1]}[face]


def model_json(elems):
    used = []
    out = []
    for e in elems:
        faces = {}
        for f in FACES:
            if f == "down" and e["from"][1] == 0:
                continue
            tex, uv = e["special"].get(f, (e["tex"], None))
            if tex not in used:
                used.append(tex)
            face = {"texture": "#" + tex}
            if uv is not None:
                face["uv"] = uv
            faces[f] = face
        out.append({"from": e["from"], "to": e["to"], "faces": faces})
    textures = {t: f"scribery:block/station_{t}" for t in used}
    textures["particle"] = textures[used[0]]
    return {"parent": "minecraft:block/block", "textures": textures, "elements": out}


def write_java():
    def arr(v):
        return "new double[][] {\n            " + ",\n            ".join("{" + ", ".join(f"{x:g}" for x in t) + "}" for t in v) + " }"
    consts = "\n".join(f"    /** {k}: x1, y1, z1, x2, y2, z2 fuer Blickrichtung Norden (Vorderseite = -z). */\n"
                       f"    public static final double[][] {k.upper()} = {arr(v)};\n" for k, v in SHAPES.items())
    path = os.path.join(ROOT, "mod/src/main/java/de/scribery/StationShapes.java")
    src = open(path, encoding="utf-8").read()
    head = src[:src.index("    /** scribe_lectern") if "    /** scribe_lectern" in src else 0]
    tail = src[src.index("    /** Formen fuer alle vier"):]
    open(path, "w", encoding="utf-8", newline="\n").write(head + consts + "\n" + tail)


def write_models():
    for name, elems in MODELS.items():
        with open(os.path.join(RES, "models/block", name + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(model_json(elems), f, indent=2)
            f.write("\n")
    json.dump({k: [list(b) for b in v] for k, v in SHAPES.items()}, open(os.path.join(OUT, "station_boxes.json"), "w"))


# ---------------------------------------------------------------- Vorschau (Software-Renderer, ohne Minecraft)

SHADE = {"up": 1.0, "north": 0.9, "east": 0.78, "west": 0.7, "south": 0.62, "down": 0.5}


def corner(face, a, b, s, t):
    (x1, y1, z1), (x2, y2, z2) = a, b
    dx, dy, dz = x2 - x1, y2 - y1, z2 - z1
    return {"north": (x2 - s * dx, y2 - t * dy, z1), "south": (x1 + s * dx, y2 - t * dy, z2),
            "west": (x1, y2 - t * dy, z1 + s * dz), "east": (x2, y2 - t * dy, z2 - s * dz),
            "up": (x1 + s * dx, y2, z1 + t * dz), "down": (x1 + s * dx, y1, z2 - t * dz)}[face]


def render(elems, tex, yaw, pitch=32, scale=14, size=(300, 330)):
    W, H = size
    img = Image.new("RGB", (W, H), (58, 58, 66))
    px = img.load()
    zbuf = [[1e9] * W for _ in range(H)]
    ya, pa = math.radians(yaw), math.radians(pitch)
    for e in elems:
        a, b = e["from"], e["to"]
        for f in FACES:
            m, uv = e["special"].get(f, (e["tex"], None))
            if uv is None:
                uv = auto_uv(f, a, b)
            ex = [b[0] - a[0], b[1] - a[1], b[2] - a[2]]
            sx = {"north": ex[0], "south": ex[0], "up": ex[0], "down": ex[0], "west": ex[2], "east": ex[2]}[f]
            sy = ex[1] if f not in ("up", "down") else ex[2]
            if sx <= 0 or sy <= 0:
                continue
            n = int(max(sx, sy) * scale * 1.6) + 2
            sh = SHADE[f]
            T = tex[m].load()
            for i in range(n + 1):
                s = i / n
                for j in range(n + 1):
                    t = j / n
                    x, y, z = corner(f, a, b, s, t)
                    x, z = x - 8, z - 8
                    x2 = x * math.cos(ya) - z * math.sin(ya)
                    z2 = x * math.sin(ya) + z * math.cos(ya)
                    sxp = -x2
                    syp = (y - 8) * math.cos(pa) + z2 * math.sin(pa)
                    depth = z2 * math.cos(pa) - (y - 8) * math.sin(pa)
                    X = int(W / 2 + sxp * scale)
                    Y = int(H / 2 + 6 - syp * scale)
                    if 0 <= X < W and 0 <= Y < H and depth < zbuf[Y][X]:
                        u = uv[0] + (uv[2] - uv[0]) * s
                        v = uv[1] + (uv[3] - uv[1]) * t
                        c = T[min(15, max(0, int(u))) % 16, min(15, max(0, int(v))) % 16]
                        zbuf[Y][X] = depth
                        px[X, Y] = tuple(int(k * sh) for k in c)
    return img


def preview(tex):
    names = list(MODELS)
    views = [(-38, "vorn links"), (38, "vorn rechts"), (180 - 38, "hinten")]
    cw, ch = 300, 330
    sheet = Image.new("RGB", (cw * len(views), (ch + 22) * len(names)), (38, 38, 44))
    d = ImageDraw.Draw(sheet)
    for r, n in enumerate(names):
        for c, (yaw, label) in enumerate(views):
            sheet.paste(render(MODELS[n], tex, yaw), (c * cw, r * (ch + 22) + 22))
            d.text((c * cw + 6, r * (ch + 22) + 6), f"{n} - {label}", fill=(230, 230, 230))
    sheet.save(os.path.join(OUT, "stations-preview.png"))


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    tex = write_textures()
    write_models()
    write_java()
    preview(tex)
    print("fertig:", ", ".join(MODELS))
