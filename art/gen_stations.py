"""Erzeugt Modelle und Texturen der Werkbloecke (Pult, Presse, Lettern-Schrank, Uebersetzungstisch, Minting Table).

Aufruf: python art/gen_stations.py
Ausgabe: mod/.../textures/block/ws_*.png        (Kacheln 16x16)
         mod/.../models/block/{scribe_lectern,printing_press,letter_cabinet,translator_desk,minting_table}.json
         mod/.../java/de/scribery/StationShapes.java (Konstanten; Kollisionsformen passend zu den Modellen)
         art/out/stations-preview.png           (Vorschau aus mehreren Winkeln, eigener Software-Renderer)
Das Manuscript Archive bleibt ein Wuerfel und wird nicht angefasst.

Stil nach den Vanilla-Werkbloecken (Crafting, Smithing, Cartography, Loom, Lectern, Blast Furnace): weiche Toene mit wenig Kontrast,
dunkler 1-Pixel-Rand an Blockflaechen, flaches Metall, Symbole auf der Vorderseite statt vieler Kleinteile. Mischform: Koerper als
(fast) voller Block mit gemalten Flaechen, darauf ein paar kraeftige 3D-Teile. Kerze und Flamme nutzen Vanilla-Texturen
(minecraft:block/candle_lit, minecraft:block/campfire_fire), die nicht ins Repo kopiert werden.
Farben: Eiche, Fichte, Stein und Eisen sind vanilla-nahe Toene AUSSERHALB von art/palette.json (dokumentierte Ausnahme wie die Eisblau-Toene);
Pergament, Tinte, Blei und Flamme kommen aus der Palette.
Modelle: nur Quader, Drehung nur +-45 Grad um y bei den Flammenflaechen (symmetrisch, Vorzeichen egal). Vorderseite = Norden (-z).
"""
import json
import math
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "mod/src/main/resources/assets/scribery")
OUT = os.path.join(ROOT, "art/out")
VANILLA = os.environ.get("VANILLA_TEX", "/tmp/vanilla")   # nur fuer die Vorschau; fehlt der Ordner, werden Platzhalter gezeichnet
PAL = {k: tuple(int(v[i:i + 2], 16) for i in (1, 3, 5)) for k, v in json.load(open(os.path.join(ROOT, "art/palette.json")))
       .items() if k != "_comment"}
C = dict(PAL)
C.update({
    "o0": (120, 94, 52), "o1": (143, 114, 67), "o2": (162, 130, 78), "o3": (176, 143, 87), "orim": (82, 58, 30),     # Eiche
    "s0": (62, 44, 26), "s1": (84, 60, 34), "s2": (100, 74, 42), "s3": (114, 86, 50), "srim": (40, 28, 16),          # Fichte
    "k0": (70, 70, 72), "k1": (96, 96, 98), "k2": (112, 112, 114), "k3": (130, 130, 132), "krim": (44, 44, 46),      # Stein
    "m0": (96, 96, 102), "m1": (120, 120, 126), "m2": (142, 142, 148), "m3": (168, 168, 174),                         # Eisen
    "leather": (104, 44, 36), "leather_hi": (132, 62, 48),
})


# ---------------------------------------------------------------- Texturen (16x16)

def new(w=16, h=16, fill=None):
    return Image.new("RGB", (w, h), fill or (0, 0, 0))


def noise(px, w, h, tones, rng, weights):
    for y in range(h):
        for x in range(w):
            px[x, y] = rng.choices(tones, weights)[0]


def planks(tones, seam, rng, w=16, h=16):
    """Weiche Bretter: vier Bretter je 4 Pixel, kaum Kontrast, Fugen nur eine Stufe dunkler, sparsame Maserung."""
    im = new(w, h)
    px = im.load()
    board = [tones[1], tones[2], tones[1], tones[2]]
    for y in range(h):
        b = (y // 4) % 4
        for x in range(w):
            px[x, y] = rng.choices([board[b], tones[0], tones[3]], [0.8, 0.12, 0.08])[0]
        if y % 4 == 0:
            for x in range(w):
                px[x, y] = seam
    for _ in range(6):
        x, y = rng.randrange(w), rng.randrange(h)
        if y % 4:
            px[x, y] = tones[0]
            if x + 1 < w:
                px[x + 1, y] = tones[0]
    return im


def rim(im, color, box=None):
    """Dunkler 1-Pixel-Rand um die Flaeche (wie bei Vanilla-Werkbloecken); box = (x1, y1, x2, y2) einschliesslich."""
    px = im.load()
    x1, y1, x2, y2 = box or (0, 0, im.width - 1, im.height - 1)
    for x in range(x1, x2 + 1):
        px[x, y1] = color
        px[x, y2] = color
    for y in range(y1, y2 + 1):
        px[x1, y] = color
        px[x2, y] = color
    return im


def flat_metal(tones, rng, w=16, h=16):
    im = new(w, h)
    noise(im.load(), w, h, tones, rng, [0.5, 0.3, 0.12, 0.08][:len(tones)])
    return im


def rect(px, x1, y1, x2, y2, color):
    for y in range(y1, y2 + 1):
        for x in range(x1, x2 + 1):
            px[x, y] = color


def oak(seed, h=16):
    return planks([C["o0"], C["o1"], C["o2"], C["o3"]], C["o0"], random.Random(seed), 16, h)


def spruce(seed):
    return planks([C["s0"], C["s1"], C["s2"], C["s3"]], C["s0"], random.Random(seed))


def mat_oak():
    return oak(1)


def mat_dark():
    return spruce(2)


def mat_iron():
    return flat_metal([C["m1"], C["m2"], C["m0"], C["m3"]], random.Random(3))


def mat_irond():
    return flat_metal([C["m0"], C["k1"], C["k0"], C["m1"]], random.Random(4))


def mat_stone():
    return flat_metal([C["k1"], C["k2"], C["k0"], C["k3"]], random.Random(5))


def mat_paper():
    im = new(16, 16, C["parchment_3"])
    px = im.load()
    rng = random.Random(6)
    for _ in range(18):
        px[rng.randrange(16), rng.randrange(16)] = C["parchment_2"]
    return im


def mat_ink():
    im = new(16, 16, C["ink_1"])
    px = im.load()
    rng = random.Random(7)
    for _ in range(26):
        px[rng.randrange(16), rng.randrange(16)] = C["ink_0"]
    return im


def mat_molten():
    im = new(16, 16, C["lead_3"])
    px = im.load()
    rng = random.Random(8)
    for _ in range(60):
        px[rng.randrange(16), rng.randrange(16)] = rng.choice([C["lead_4"], C["lead_4"], C["lead_2"]])
    return im


# --- Flaechen mit Symbolen (Hoehe h: Block hat h Pixel Hoehe, der Rest der Kachel bleibt leer)

def face_oak(seed, h, rimc=None):
    im = oak(seed, h)
    return rim(im, rimc or C["orim"])


def desk_front():
    im = face_oak(11, 12)
    px = im.load()
    for x0 in (2, 9):                                  # zwei Schubladen
        rect(px, x0, 2, x0 + 5, 9, C["o0"])
        rect(px, x0 + 1, 3, x0 + 4, 8, C["o1"])
        rect(px, x0 + 2, 5, x0 + 3, 6, C["m2"])       # Griff
    return im


def desk_top():
    im = face_oak(12, 16)
    px = im.load()
    rect(px, 2, 2, 13, 13, C["leather"])               # Schreibunterlage
    for x in range(2, 14):
        px[x, 2] = C["leather_hi"]
    rim(im, C["orim"])
    return im


def cab_top():
    im = face_oak(14, 16)
    px = im.load()
    rect(px, 1, 1, 10, 14, C["ink_1"])                 # Buchstabenbox: Faecher mit Lettern
    for r in range(4):
        for c in range(3):
            x0, y0 = 2 + c * 3, 2 + r * 3
            rect(px, x0, y0, x0 + 1, y0 + 1, C["lead_2"])
            px[x0, y0] = C["lead_4"]
    rect(px, 11, 6, 14, 8, C["parchment_3"])           # Papierstapel
    rect(px, 11, 8, 14, 8, C["parchment_2"])
    return rim(im, C["orim"])


def press_front():
    im = face_oak(15, 10)
    px = im.load()
    rect(px, 2, 1, 13, 8, C["s0"])                    # Feld wie beim Smithing Table
    rect(px, 5, 2, 10, 3, C["m2"])                     # Tiegel (Platte)
    rect(px, 7, 4, 8, 5, C["m1"])                      # Spindel
    for x in range(4, 12, 2):                          # Lettern in der Form
        rect(px, x, 6, x + 1, 7, C["lead_3"])
    return im


def press_top():
    im = face_oak(16, 16)
    px = im.load()
    rect(px, 3, 4, 12, 11, C["ink_1"])                 # Setzform
    for r in range(2):
        for c in range(5):
            rect(px, 4 + c * 2, 5 + r * 3, 5 + c * 2, 6 + r * 3, C["lead_2"])
            px[4 + c * 2, 5 + r * 3] = C["lead_4"]
    rect(px, 3, 13, 12, 14, C["parchment_3"])          # Papierablage
    return rim(im, C["orim"])


def mint_inner():
    """Innenseite des Ofenmauls: dunkler, russiger Stein."""
    im = new(16, 16, C["ink_1"])
    noise(im.load(), 16, 16, [C["ink_1"], C["ink_0"], C["k0"]], random.Random(24), [0.55, 0.3, 0.15])
    return im


def coals():
    """Glut auf dem Boden des Ofens."""
    im = new(16, 16, C["ink_0"])
    px = im.load()
    rng = random.Random(25)
    for _ in range(90):
        px[rng.randrange(16), rng.randrange(16)] = rng.choice([C["flame_2"], C["flame_2"], C["flame_1"], C["ink_1"], C["flame_0"]])
    return im


def mint_side():
    im = new(16, 10, C["k1"])
    noise(im.load(), 16, 10, [C["k1"], C["k2"], C["k0"]], random.Random(22), [0.6, 0.25, 0.15])
    return rim(im, C["krim"])


def mint_top():
    im = flat_metal([C["m0"], C["k1"], C["k0"]], random.Random(23))
    px = im.load()
    for r in (4, 11):
        for x in range(2, 14):
            px[x, r] = C["k0"]
    return rim(im, C["krim"])


def mold_top():
    """Gussform: Eisenplatte mit zwei Lettern-Vertiefungen, eine mit fluessigem Blei gefuellt (Bereich 5x5 links oben)."""
    im = flat_metal([C["m0"], C["k1"], C["k0"]], random.Random(31))
    px = im.load()
    rect(px, 1, 1, 1, 3, C["ink_0"])
    rect(px, 3, 1, 3, 3, C["lead_3"])
    px[3, 1] = C["lead_4"]
    for k in (0, 4):
        for t in range(5):
            px[k, t] = C["k0"]
            px[t, k] = C["k0"]
    return im


def archive_front():
    """Archivschrank: 3 x 3 Faecher, in jedem eine Schriftrolle (Papyrus-Ende)."""
    im = face_oak(41, 16)
    px = im.load()
    for r in range(3):
        for c in range(3):
            x0, y0 = 1 + c * 5, 1 + r * 5
            rect(px, x0, y0, x0 + 3, y0 + 3, C["o0"])
            rect(px, x0 + 1, y0 + 1, x0 + 2, y0 + 2, C["ink_1"])
            px[x0 + 1, y0 + 1] = C["parchment_3"]
            px[x0 + 2, y0 + 1] = C["parchment_2"]
            px[x0 + 1, y0 + 2] = C["parchment_2"]
            px[x0 + 2, y0 + 2] = C["parchment_1"]
    return im


def archive_top():
    im = spruce(42)
    px = im.load()
    rect(px, 3, 3, 12, 12, C["leather"])
    for x in range(3, 13):
        px[x, 3] = C["leather_hi"]
    rect(px, 5, 5, 10, 10, C["parchment_3"])
    return rim(im, C["srim"])


def bookshelf_front():
    """Buecherregal: drei Boeden mit Buechern und Rollen (Farben aus der Palette plus drei gedaempfte Buchfarben)."""
    im = face_oak(43, 16)
    px = im.load()
    rng = random.Random(44)
    cols = [C["rubric"], (60, 88, 120), (72, 108, 72), C["parchment_1"], (98, 70, 116), C["lead_1"], C["wood_3"]]
    for shelf in range(3):
        y1 = 1 + shelf * 5
        rect(px, 1, y1, 14, y1 + 3, C["ink_0"])
        x = 1
        while x < 14:
            w = rng.choice([1, 2, 2, 3])
            w = min(w, 14 - x)
            h = rng.choice([3, 4, 4])
            col = rng.choice(cols)
            rect(px, x, y1 + 4 - h, x + w - 1, y1 + 3, col)
            if w > 1:
                px[x, y1 + 4 - h] = tuple(min(255, v + 30) for v in col)
            x += w
    return im


def bookshelf_top():
    im = oak(45)
    return rim(im, C["orim"])


def oak_side(h, seed):
    return lambda: face_oak(seed, h)


MATERIALS = {
    "oak": mat_oak, "dark": mat_dark, "iron": mat_iron, "irond": mat_irond, "stone": mat_stone,
    "paper": mat_paper, "ink": mat_ink, "molten": mat_molten,
    "desk_front": desk_front, "desk_top": desk_top, "desk_side": oak_side(12, 17),
    "cab_top": cab_top,
    "mold_top": mold_top,
    "archive_front": archive_front, "archive_side": oak_side(16, 46), "archive_top": archive_top,
    "bookshelf_front": bookshelf_front, "bookshelf_side": bookshelf_front, "bookshelf_top": bookshelf_top,
    "press_front": press_front, "press_top": press_top, "press_side": oak_side(10, 19),
    "mint_side": mint_side, "mint_top": mint_top, "mint_inner": mint_inner, "coals": coals,
}
# Vanilla-Texturen: Name im Modell -> (Resource-Pfad, Datei fuer die Vorschau)
VANILLA_TEXTURES = {"candle": ("minecraft:block/candle_lit", "candle_lit.png"), "fire": ("minecraft:block/campfire_fire", "campfire_fire.png")}


def write_textures():
    tex = {}
    d = os.path.join(RES, "textures/block")
    for f in os.listdir(d):                           # alte Kacheln dieses Skripts entfernen
        if f.startswith("station_") or f.startswith("ws_"):
            os.remove(os.path.join(d, f))
    for name, fn in MATERIALS.items():
        im = fn()
        canvas = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        canvas.paste(im.convert("RGBA"), (0, 0))
        canvas.save(os.path.join(d, f"ws_{name}.png"))
        tex[name] = canvas
    for name, (_, fname) in VANILLA_TEXTURES.items():
        path = os.path.join(VANILLA, fname)
        if os.path.exists(path):
            im = Image.open(path).convert("RGBA").crop((0, 0, 16, 16))
        else:
            im = Image.new("RGBA", (16, 16), (255, 160, 40, 255))
        tex[name] = im
    return tex


# ---------------------------------------------------------------- Modelle

def box(a, b, tex, only=None, rot=None, **special):
    """a, b = (x1,y1,z1), (x2,y2,z2); tex = Material; special: Seite -> (Material, uv oder None); only: nur diese Seiten;
    rot = (winkel, ursprung) Drehung um y."""
    return {"from": list(a), "to": list(b), "tex": tex, "special": special, "only": only, "rot": rot}


MODELS = {}
SHAPES = {}
CUTOUT = {"scribe_lectern", "scribe_lectern_mini", "minting_table"}

FRONT_SIDES = lambda pre, h: dict(east=(pre + "_side", [0, 0, 16, h]), west=(pre + "_side", [0, 0, 16, h]), south=(pre + "_side", [0, 0, 16, h]))


def body(pre, h):
    return box((0, 0, 0), (16, h, 16), pre + "_side", north=(pre + "_front", [0, 0, 16, h]), up=(pre + "_top", None),
               **FRONT_SIDES(pre, h))


def stone_wall(a, b, h=10, inner=(), **extra):
    """Quader des Ofenkoerpers: Aussenseiten aus mint_side (Rand an den Blockkanten, Hoehe h), oben mint_top,
    Seiten in 'inner' (z. B. 'east') zeigen die dunkle Innenseite des Maules."""
    (x1, y1, z1), (x2, y2, z2) = a, b
    uvs = {"north": [16 - x2, h - y2, 16 - x1, h - y1], "south": [x1, h - y2, x2, h - y1],
           "west": [z1, h - y2, z2, h - y1], "east": [16 - z2, h - y2, 16 - z1, h - y1]}
    sp = {f: ("mint_side", uv) for f, uv in uvs.items()}
    for f in inner:
        sp[f] = ("mint_inner", None)
    sp["up"] = ("mint_top", None)
    sp.update(extra)
    return box(a, b, "mint_side", **sp)


def candle(x, y, z, hgt=5):
    """Vanilla-Kerze (Aufbau wie block/template_candle): Koerper 2x2, zwei gekreuzte Dochtflaechen."""
    uv = [0, 8, 2, 8 + hgt]
    return [box((x, y, z), (x + 2, y + hgt, z + 2), "candle", north=("candle", uv), east=("candle", uv), south=("candle", uv),
                west=("candle", uv), up=("candle", [0, 6, 2, 8])),
            box((x + 0.5, y + hgt, z + 1), (x + 1.5, y + hgt + 1, z + 1), "candle", only=("north", "south"),
                rot=(45, (x + 1, y + hgt, z + 1)), north=("candle", [0, 5, 1, 6]), south=("candle", [0, 5, 1, 6])),
            box((x + 0.5, y + hgt, z + 1), (x + 1.5, y + hgt + 1, z + 1), "candle", only=("north", "south"),
                rot=(-45, (x + 1, y + hgt, z + 1)), north=("candle", [0, 5, 1, 6]), south=("candle", [0, 5, 1, 6]))]


def fire(cx, y, cz, w, hgt):
    """Flamme aus zwei gekreuzten Flaechen mit der animierten Vanilla-Lagerfeuer-Textur."""
    return [box((cx - w / 2, y, cz), (cx + w / 2, y + hgt, cz), "fire", only=("north", "south"), rot=(a, (cx, y, cz)),
                north=("fire", [0, 0, 16, 16]), south=("fire", [0, 0, 16, 16])) for a in (45, -45)]


# Pult: Standfuss mit Saeule und Platte, Rueckwand, Kerze (Flamme als Teilchen, siehe ScribeLecternBlock)
MODELS["scribe_lectern"] = [
    box((3, 0, 3), (13, 2, 13), "dark"),
    box((4, 2, 4), (12, 3, 12), "oak"),
    box((6, 3, 6), (10, 11, 10), "oak"),
    box((5, 9, 5), (11, 10, 11), "dark"),
    box((1, 11, 1), (15, 13, 15), "oak"),
    box((1, 13, 13), (15, 16, 15), "dark"),
] + candle(12, 16, 13)          # wie eine normale Kerze: nur Vanilla-Kerzenmodell, Flamme als Teilchen
SHAPES["scribe_lectern"] = [(3, 0, 3, 13, 3, 13), (6, 3, 6, 10, 11, 10), (1, 11, 1, 15, 13, 15), (1, 13, 13, 15, 16, 15)]

# Pult-Variante "mini": ein verkleinertes Pult auf einem kleinen Tisch, etwa so hoch wie die Presse (16). Noch nicht
# eingebunden; Modell: models/block/scribe_lectern_mini.json
def _mini_lectern(s=0.45, y0=9.5):
    def m(c, x, y, z):
        return (round(8 + (x - 8) * s, 2), round(y0 + y * s, 2), round(8 + (z - 8) * s, 2))
    out = []
    for (a, b, t) in [((3, 0, 3), (13, 2, 13), "dark"), ((4, 2, 4), (12, 3, 12), "oak"), ((6, 3, 6), (10, 11, 10), "oak"),
                      ((5, 9, 5), (11, 10, 11), "dark"), ((1, 11, 1), (15, 13, 15), "oak"), ((1, 13, 13), (15, 16, 15), "dark")]:
        out.append(box(m(0, *a), m(0, *b), t))
    return out


MODELS["scribe_lectern_mini"] = [
    box((2.5, 0, 2.5), (4.5, 8, 4.5), "dark"), box((11.5, 0, 2.5), (13.5, 8, 4.5), "dark"),
    box((2.5, 0, 11.5), (4.5, 8, 13.5), "dark"), box((11.5, 0, 11.5), (13.5, 8, 13.5), "dark"),
    box((1.5, 8, 1.5), (14.5, 9.5, 14.5), "oak"),
] + _mini_lectern() + candle(11.2, 9.5, 11.2, 3)

# Uebersetzungstisch: Schreibtisch-Block mit Schubladen, Schreibunterlage, Tintenfass und Papier hinten
MODELS["translator_desk"] = [
    body("desk", 12),
    box((11, 12, 13), (14.5, 15, 16), "ink"),
    box((1.5, 12, 13), (8.5, 14, 16), "paper"),
]
SHAPES["translator_desk"] = [(0, 0, 0, 16, 12, 16), (1.5, 12, 13, 14.5, 15, 16)]

# Presse: Block mit Symbol, Setzform und Papierablage oben (gemalt), darueber Rahmen mit Tiegel-Platte, Kurbel und Hebel
MODELS["printing_press"] = [
    body("press", 10),
    box((1, 10, 6), (3, 14, 10), "dark"), box((13, 10, 6), (15, 14, 10), "dark"),
    box((1, 14, 5), (15, 16, 11), "dark"),
    box((5, 11.5, 6.5), (11, 13, 9.5), "iron"), box((7, 13, 7), (9, 14, 9), "irond"),
    box((0, 14, 7), (1, 16, 9), "oak"),
    box((15, 12, 7), (16, 14.5, 9.5), "iron"),
]
SHAPES["printing_press"] = [(0, 0, 0, 16, 10, 16), (1, 10, 5, 15, 16, 11)]

# Lettern-Schrank: richtiger Tisch (Beine, Platte) mit Buchstabenbox und Papier oben gemalt, Fass (Tinte) und Walze als 3D
MODELS["letter_cabinet"] = [
    box((0, 9, 0), (16, 12, 16), "oak", up=("cab_top", None)),
    box((0.5, 0, 0.5), (3, 9, 3), "dark"), box((13, 0, 0.5), (15.5, 9, 3), "dark"),
    box((0.5, 0, 13), (3, 9, 15.5), "dark"), box((13, 0, 13), (15.5, 9, 15.5), "dark"),
    box((3, 3, 1.5), (13, 4.5, 14.5), "dark"),
    box((10, 12, 9), (15, 15, 14), "dark", up=("ink", None)),                          # offenes Fass: Tinte steht knapp unter dem Rand
    box((10, 15, 9), (15, 16, 9.8), "dark"), box((10, 15, 13.2), (15, 16, 14), "dark"),
    box((10, 15, 9.8), (10.8, 16, 13.2), "dark"), box((14.2, 15, 9.8), (15, 16, 13.2), "dark"),
    box((9.7, 13, 8.7), (15.3, 14, 14.3), "iron"),
    box((10.5, 12, 2.5), (14.5, 13.5, 5.5), "dark"),
    box((10, 12.2, 3), (10.5, 13.3, 5), "iron"), box((14.5, 12.2, 3), (15, 13.3, 5), "iron"),
]
SHAPES["letter_cabinet"] = [(0, 9, 0, 16, 12, 16), (0.5, 0, 0.5, 3, 9, 3), (13, 0, 0.5, 15.5, 9, 3), (0.5, 0, 13, 3, 9, 15.5),
                            (13, 0, 13, 15.5, 9, 15.5), (10, 12, 9, 15, 16, 14)]

# Minting Table: Steinblock wie ein Ofen mit echter, nach hinten gehender Oeffnung vorn links (oben abgerundet), darin Glut und
# Flamme; oben der Tiegel ueber dem Ofen, eine kleine Gussform und ein kleiner Amboss
MODELS["minting_table"] = [
    stone_wall((0, 0, 6), (16, 10, 16), inner=()),                                     # Rueckteil; Vorderseite bei z=6 ist die Rueckwand des Maules
    stone_wall((0, 0, 0), (16, 2, 6), up=("coals", None)),                             # Boden unter dem Maul
    stone_wall((0, 8, 0), (16, 10, 6), down=("mint_inner", None)),                     # Sturz
    stone_wall((0, 2, 0), (9, 8, 6), inner=("east",)),                                 # links vom Maul (aus Sicht des Spielers: x klein = rechts)
    stone_wall((15, 2, 0), (16, 8, 6), inner=("west",)),
    stone_wall((9, 7, 0), (10, 8, 6), inner=("east",), down=("mint_inner", None)),     # Rundung oben
    stone_wall((14, 7, 0), (15, 8, 6), inner=("west",), down=("mint_inner", None)),
    box((9.5, 10, 2.5), (14.5, 11.5, 7.5), "stone"),
    box((10, 11.5, 3), (14, 14, 7), "iron", up=("molten", None)),
    box((14, 12.5, 4.5), (16, 13.5, 5.5), "irond"),
    box((4, 10, 3), (9, 11.5, 8), "irond", up=("mold_top", [0, 0, 5, 5])),
    box((2, 10, 9), (7, 11, 14), "irond"),
    box((3.5, 11, 10), (5.5, 13, 13), "irond"),
    box((1, 13, 9), (7, 15, 14), "irond"),
    box((0, 13.5, 10), (1, 14.5, 13), "irond"),
] + fire(12, 2, 3.5, 5, 4.5)
SHAPES["minting_table"] = [(0, 0, 0, 16, 10, 16), (9.5, 10, 2.5, 16, 14, 7.5), (4, 10, 3, 9, 11.5, 8), (0, 10, 9, 7, 15, 14)]

# Nur fuer die Vorschau: Archiv und Buecherregal sind volle Wuerfel (Modell: orientable, siehe write_models)
CUBES = {
    "manuscript_archive": [box((0, 0, 0), (16, 16, 16), "archive_side", north=("archive_front", None), up=("archive_top", None))],
    "bookshelf": [box((0, 0, 0), (16, 16, 16), "bookshelf_side", north=("bookshelf_front", None), up=("bookshelf_top", None))],
}

FACES = ["north", "south", "east", "west", "up", "down"]


def auto_uv(face, a, b):
    (x1, y1, z1), (x2, y2, z2) = a, b
    return {"up": [x1, z1, x2, z2], "down": [x1, 16 - z2, x2, 16 - z1], "north": [16 - x2, 16 - y2, 16 - x1, 16 - y1],
            "south": [x1, 16 - y2, x2, 16 - y1], "west": [z1, 16 - y2, z2, 16 - y1], "east": [16 - z2, 16 - y2, 16 - z1, 16 - y1]}[face]


def model_json(name, elems):
    used = []
    out = []
    for e in elems:
        faces = {}
        for f in FACES:
            if e["only"] and f not in e["only"]:
                continue
            if f == "down" and e["from"][1] == 0:
                continue
            tex, uv = e["special"].get(f, (e["tex"], None))
            if tex not in used:
                used.append(tex)
            face = {"texture": "#" + tex}
            if uv is not None:
                face["uv"] = uv
            faces[f] = face
        el = {"from": e["from"], "to": e["to"]}
        if e["rot"]:
            el["rotation"] = {"angle": e["rot"][0], "axis": "y", "origin": list(e["rot"][1])}
        el["faces"] = faces
        out.append(el)
    textures = {}
    for t in used:
        textures[t] = VANILLA_TEXTURES[t][0] if t in VANILLA_TEXTURES else f"scribery:block/ws_{t}"
    textures["particle"] = textures.get("oak") or textures[used[0]]
    d = {"parent": "minecraft:block/block", "textures": textures, "elements": out}
    if name in CUTOUT:
        d["render_type"] = "minecraft:cutout"
    return d


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
    for name in ("archive", "bookshelf"):                     # volle Wuerfel, Vorderseite nach vorn (Blockstate dreht)
        target = {"archive": "manuscript_archive", "bookshelf": "bookshelf"}[name]
        with open(os.path.join(RES, "models/block", target + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"parent": "minecraft:block/orientable", "textures": {
                "top": f"scribery:block/ws_{name}_top", "front": f"scribery:block/ws_{name}_front",
                "side": f"scribery:block/ws_{name}_side"}}, f, indent=2)
            f.write("\n")
    for name, elems in MODELS.items():
        with open(os.path.join(RES, "models/block", name + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(model_json(name, elems), f, indent=2)
            f.write("\n")
    # Minting Table ohne Feuer (Zustand lit=false): dasselbe Modell ohne die beiden Flammenflaechen
    off = [e for e in MODELS["minting_table"] if "fire" not in str(e)]
    with open(os.path.join(RES, "models/block", "minting_table_off.json"), "w", encoding="utf-8", newline="\n") as f:
        m = model_json("minting_table", off)
        m["textures"].pop("fire", None)
        json.dump(m, f, indent=2)
        f.write("\n")


# ---------------------------------------------------------------- Vorschau (Software-Renderer, ohne Minecraft)

SHADE = {"up": 1.0, "north": 0.9, "east": 0.78, "west": 0.7, "south": 0.62, "down": 0.5}


def corner(face, a, b, s, t, rot=None):
    (x1, y1, z1), (x2, y2, z2) = a, b
    dx, dy, dz = x2 - x1, y2 - y1, z2 - z1
    p = {"north": (x2 - s * dx, y2 - t * dy, z1), "south": (x1 + s * dx, y2 - t * dy, z2),
         "west": (x1, y2 - t * dy, z1 + s * dz), "east": (x2, y2 - t * dy, z2 - s * dz),
         "up": (x1 + s * dx, y2, z1 + t * dz), "down": (x1 + s * dx, y1, z2 - t * dz)}[face]
    if rot:
        ang, (ox, oy, oz) = rot
        r = math.radians(ang)
        x, z = p[0] - ox, p[2] - oz
        p = (ox + x * math.cos(r) - z * math.sin(r), p[1], oz + x * math.sin(r) + z * math.cos(r))
    return p


def render(elems, tex, yaw, pitch=32, scale=14, size=(300, 330)):
    W, H = size
    img = Image.new("RGB", (W, H), (58, 58, 66))
    px = img.load()
    zbuf = [[1e9] * W for _ in range(H)]
    ya, pa = math.radians(yaw), math.radians(pitch)
    for e in elems:
        a, b = e["from"], e["to"]
        for f in FACES:
            if e["only"] and f not in e["only"]:
                continue
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
                    x, y, z = corner(f, a, b, s, t, e["rot"])
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
                        if c[3] < 128:
                            continue
                        zbuf[Y][X] = depth
                        px[X, Y] = tuple(int(k * sh) for k in c[:3])
    return img


def preview(tex):
    names = list(MODELS) + list(CUBES)
    views = [(-38, "vorn links"), (38, "vorn rechts"), (180 - 38, "hinten")]
    cw, ch = 300, 330
    sheet = Image.new("RGB", (cw * len(views), (ch + 22) * len(names)), (38, 38, 44))
    d = ImageDraw.Draw(sheet)
    for r, n in enumerate(names):
        for c, (yaw, label) in enumerate(views):
            sheet.paste(render((MODELS.get(n) or CUBES[n]), tex, yaw), (c * cw, r * (ch + 22) + 22))
            d.text((c * cw + 6, r * (ch + 22) + 6), f"{n} - {label}", fill=(230, 230, 230))
    sheet.save(os.path.join(OUT, "stations-preview.png"))


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    tex = write_textures()
    write_models()
    write_java()
    preview(tex)
    print("fertig:", ", ".join(MODELS))
