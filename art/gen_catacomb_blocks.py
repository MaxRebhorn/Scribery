"""Erzeugt die Blockgruppe der Katakomben: roemische Ziegel (Block, Treppe, Stufe, verwittert) und Mosaike.

Aufruf: python art/gen_catacomb_blocks.py
Ausgabe: Texturen, Blockstates, Modelle, Item-Modelle und Loot-Tabellen direkt unter mod/src/main/resources,
dazu art/out/catacomb-preview.png.

Vorbild: Duenne roemische Ziegel (Ostia, Katakomben): sehr flache Lagen, rotbraun, der Moertel kaum heller.
Mosaike nach dem Vorbild von Tabgha (Brotvermehrung): goldener Grund aus kleinen, unregelmaessigen Steinchen, dunkle
Fugen. Zwei Bilder, jeweils aus mehreren Bloecken zusammengesetzt:
  mosaic_loaves   5 x 3 Bloecke: zwei Fische und ein Brotkorb
  mosaic_chi_rho  3 x 3 Bloecke: Chi-Rho im Ring
Die Bildbloecke tragen die Eigenschaft "part" (Zeile * Spalten + Spalte, Zeile 0 = Norden, Spalte 0 = Westen).
"""
import json
import math
import os
import random
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "mod", "src", "main", "resources")
A = os.path.join(RES, "assets", "scribery")
D = os.path.join(RES, "data", "scribery", "loot_table", "blocks")
TEX = os.path.join(A, "textures", "block")
OUT = os.path.join(ROOT, "art", "out")
N = 16
rng = random.Random(20261120)

BRICK = [(132, 68, 52), (144, 78, 58), (120, 62, 48), (152, 86, 64), (138, 72, 54)]
MORTAR = (176, 128, 102)       # kalkiger Moertel im Schatten: nur wenig heller als der Ziegel

GOLD = [(224, 186, 108), (214, 174, 96), (232, 196, 120), (204, 164, 88)]
CREAM = [(240, 222, 168), (232, 208, 146), (246, 230, 184)]
BROWN = [(88, 60, 40), (76, 52, 34), (98, 68, 44)]
DARK = [(58, 48, 42), (48, 40, 36), (66, 56, 48)]
RED = [(176, 70, 46), (164, 62, 42), (186, 80, 52)]
ORANGE = [(222, 124, 52), (212, 112, 46), (230, 136, 62)]
OCHRE = [(214, 160, 72), (205, 150, 66), (222, 170, 84)]
YELLOW = [(232, 184, 64), (224, 174, 56), (238, 194, 76)]
BED = (112, 84, 54)            # Fuge und Moertelbett, wo Steinchen fehlen


def jit(c, a=5):
    d = rng.randint(-a, a)
    return tuple(max(0, min(255, v + d)) for v in c)


def pick(lst):
    return rng.choice(lst)


def bricks(weathered):
    """Duenne Ziegel: acht Lagen zu je einem Pixel Ziegel und einem Pixel Moertel, kurze Stossfugen."""
    im = Image.new("RGBA", (N, N))
    for row in range(8):
        y = row * 2
        x = rng.randrange(6)
        while x < N + 8:
            ln = rng.choice([5, 6, 7, 8])
            col = rng.choice(BRICK)
            if weathered:
                col = tuple(int(v * rng.uniform(0.7, 0.9)) for v in col)
            for dx in range(ln - 1):
                im.putpixel(((x + dx) % N, y), jit(col, 6) + (255,))
            im.putpixel(((x + ln - 1) % N, y), jit(MORTAR, 5) + (255,))
            x += ln
        for xx in range(N):
            c = jit(MORTAR, 5)
            if weathered and rng.random() < 0.25:
                c = tuple(int(v * 0.8) for v in c)
            im.putpixel((xx, y + 1), c + (255,))
    if weathered:
        for _ in range(22):  # Schmutz und Ausbluehungen
            x, y = rng.randrange(N), rng.randrange(N)
            p = im.getpixel((x, y))
            k = rng.choice([0.75, 0.85, 1.12])
            im.putpixel((x, y), tuple(max(0, min(255, int(v * k))) for v in p[:3]) + (255,))
    return im


def tesserae_img(cells, missing=0.0):
    """Mosaik aus 2x2-Steinchen. cells[y][x] ist die Farbe einer Steinchenzelle. Fuge rechts und unten, leichtes Rauschen."""
    h, w = len(cells), len(cells[0])
    im = Image.new("RGBA", (w * 2, h * 2))
    for cy in range(h):
        for cx in range(w):
            base = cells[cy][cx]
            gone = rng.random() < missing
            for dy in range(2):
                for dx in range(2):
                    if gone:
                        c = BED
                    else:
                        c = jit(base, 7)
                        if dx == 1 or dy == 1:
                            c = tuple(int(v * 0.8) for v in c)
                    im.putpixel((cx * 2 + dx, cy * 2 + dy), c + (255,))
    return im


STONE = [(172, 158, 132), (162, 148, 124), (182, 168, 142), (154, 140, 118)]


def floor_tiles():
    """Normaler Steinboden im Stil des Mosaiks: warme Steinplatten (8 x 4 Pixel, versetzt) mit der dunklen Mosaikfuge."""
    im = Image.new("RGBA", (N, N))
    for row in range(4):
        off = 0 if row % 2 == 0 else 4
        for seg in range(2):
            x0 = (seg * 8 + off) % N
            col = pick(STONE)
            for dy in range(4):
                for dx in range(8):
                    x, y = (x0 + dx) % N, row * 4 + dy
                    if dx == 7 or dy == 3:  # Fuge rechts und unten, dunkel wie im Mosaik
                        c = jit(BED, 4)
                    else:
                        c = jit(col, 6)
                        if dy == 0 or dx == 0:
                            c = tuple(min(255, v + 8) for v in c)  # Licht von links oben
                    im.putpixel((x, y), c + (255,))
    return im


def background(w, h):
    return [[pick(GOLD) if rng.random() < 0.72 else pick(CREAM) for _ in range(w)] for _ in range(h)]


def m_plain():
    return tesserae_img(background(8, 8))


def m_damaged():
    g = background(8, 8)
    for y in range(8):
        for x in range(8):
            if (x + y * 3) % 7 == 0:
                g[y][x] = pick(BROWN)
    return tesserae_img(g, missing=0.4)


def frame(g):
    """Rahmen aus zwei Steinchenreihen: aussen dunkelbraun, innen hell."""
    h, w = len(g), len(g[0])
    for y in range(h):
        for x in range(w):
            e = min(x, y, w - 1 - x, h - 1 - y)
            if e == 0:
                g[y][x] = pick(BROWN)
            elif e == 1:
                g[y][x] = pick(CREAM)


def setc(g, x, y, c):
    if 0 <= y < len(g) and 0 <= x < len(g[0]):
        g[y][x] = c


def draw_fish(g, cx, flip=False):
    """Ein Fisch senkrecht, Kopf oben (wie in Tabgha), mit Flossen und Schwanz. cx = Mittelspalte."""
    top, bottom = 3, 17
    for y in range(top, bottom + 1):
        t = (y - top) / (bottom - top)
        half = 4.4 * math.sqrt(max(0.0, 1 - ((t - 0.42) / 0.62) ** 2))
        for x in range(cx - 5, cx + 6):
            dx = abs(x - cx)
            if dx > half:
                continue
            edge = dx > half - 1.0 or y == top
            if edge:
                c = pick(BROWN)
            else:
                body = pick(OCHRE) if t < 0.3 else pick(ORANGE) if t < 0.72 else pick(RED)
                if (y + x) % 3 == 0 and 0.25 < t < 0.8:
                    body = pick(RED)  # Schuppenreihen
                c = body
            setc(g, x, y, c)
    setc(g, cx - 1 if not flip else cx + 1, top + 2, pick(DARK))                 # Auge
    for y in range(top + 3, top + 12):                                           # Kiemenbogen
        setc(g, cx - 2 if not flip else cx + 2, y, pick(BROWN)) if y in (top + 3, top + 4) else None
    for k in range(4):                                                           # Brustflossen
        setc(g, cx - 5 - (k // 2), 9 + k, pick(RED))
        setc(g, cx + 5 + (k // 2), 9 + k, pick(RED))
    for y in range(bottom, bottom + 6):                                          # Schwanzflosse
        half = 1 + (y - bottom) * 0.75
        for x in range(cx - 5, cx + 6):
            if abs(x - cx) <= half:
                edge = abs(x - cx) > half - 1 or y == bottom + 5
                setc(g, x, y, pick(BROWN) if edge else pick(RED) if (y + x) % 2 else pick(ORANGE))


def draw_basket(g, cx):
    """Brotkorb: oben ein Oval mit Broten, darunter das geflochtene Koerbchen."""
    cy_rim = 7
    for y in range(3, 12):
        for x in range(cx - 8, cx + 9):
            d = ((x - cx) / 7.5) ** 2 + ((y - cy_rim) / 3.6) ** 2
            if d <= 1.0:
                setc(g, x, y, pick(BROWN) if d > 0.72 else pick(CREAM))
    for bx, by in ((cx - 4, 6), (cx, 5), (cx + 4, 6), (cx - 2, 8), (cx + 2, 8)):  # Brote mit Einschnitt
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                if abs(dx) + abs(dy) <= 1 or (abs(dx) == 1 and abs(dy) == 1 and False):
                    setc(g, bx + dx, by + dy, pick(YELLOW) if (dx, dy) != (0, 0) else pick(DARK))
    for y in range(11, 22):  # Koerbchen, nach unten schmaler
        t = (y - 11) / 10
        half = 7 - 2.2 * t
        for x in range(cx - 8, cx + 9):
            dx = abs(x - cx)
            if dx > half:
                continue
            if dx > half - 1.0 or y == 21:
                c = pick(BROWN)
            elif (y - 11) % 3 == 1:
                c = pick(YELLOW)
            elif (y - 11) % 3 == 2:
                c = pick(RED) if (x + y) % 4 else pick(DARK)
            else:
                c = pick(BROWN)
            setc(g, x, y, c)


def loaves_picture():
    g = background(40, 24)
    draw_fish(g, 7)
    draw_fish(g, 32, flip=True)
    draw_basket(g, 20)
    frame(g)
    return g


def chi_rho_picture():
    S = 24
    g = background(S, S)
    for y in range(S):
        for x in range(S):
            r = ((x - 11.5) ** 2 + (y - 11.5) ** 2) ** .5
            if 8.6 <= r < 9.6:
                g[y][x] = pick(RED)
            elif 9.6 <= r < 10.8:
                g[y][x] = pick(BROWN)
            elif 10.8 <= r < 11.9:
                g[y][x] = pick(CREAM)
            elif r < 8.6:
                g[y][x] = pick(CREAM) if rng.random() < 0.75 else pick(GOLD)
    for k in range(-7, 8):  # Chi
        for t in (0, 1, 2):
            for x, y in ((10 + k + t, 11 + k), (12 - k + t, 11 + k)):
                if ((x - 11.5) ** 2 + (y - 11.5) ** 2) ** .5 < 8.4:
                    setc(g, x, y, pick(RED))
    for y in range(5, 19):  # Rho: Stab und Schlaufe
        for x in (11, 12):
            g[y][x] = pick(BROWN)
    for x in range(11, 17):
        for y in (5, 6, 10, 11):
            g[y][x] = pick(BROWN)
    for y in range(5, 12):
        for x in (15, 16):
            g[y][x] = pick(BROWN)
    return g


def split_picture(cells, cols, rows):
    """Zellen -> cols x rows Bloecke, Index = Zeile * cols + Spalte."""
    big = tesserae_img(cells)
    return [big.crop((c * 16, r * 16, c * 16 + 16, r * 16 + 16)) for r in range(rows) for c in range(cols)]


PICTURES = {"mosaic_loaves": (5, 3, loaves_picture()), "mosaic_chi_rho": (3, 3, chi_rho_picture())}
TEXTURES = {
    "roman_bricks": bricks(False),
    "roman_bricks_weathered": bricks(True),
    "mosaic_plain": m_plain(),
    "mosaic_damaged": m_damaged(),
    "roman_floor": floor_tiles(),
}
PARTS = {}
for _name, (_c, _r, _cells) in PICTURES.items():
    PARTS[_name] = split_picture(_cells, _c, _r)
    for _i, _im in enumerate(PARTS[_name]):
        TEXTURES[f"{_name}_{_i}"] = _im

NAMES = {
    "roman_bricks": "Roman Bricks",
    "roman_brick_stairs": "Roman Brick Stairs",
    "roman_brick_slab": "Roman Brick Slab",
    "roman_bricks_weathered": "Weathered Roman Bricks",
    "roman_floor": "Roman Stone Floor",
    "mosaic_plain": "Mosaic Floor",
    "mosaic_damaged": "Damaged Mosaic Floor",
    "mosaic_loaves": "Loaves and Fishes Mosaic",
    "mosaic_chi_rho": "Chi-Rho Mosaic",
}
STALE = ["mosaic_checker", "mosaic_lozenge", "mosaic_rosette", "mosaic_fish"]
MAX_PARTS = 15


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def loot(block, slab=False):
    entry = {"type": "minecraft:item", "name": "scribery:" + block}
    if slab:
        entry["functions"] = [{
            "function": "minecraft:set_count", "count": 2.0, "add": False,
            "conditions": [{"condition": "minecraft:block_state_property", "block": "scribery:" + block,
                            "properties": {"type": "double"}}]}, {"function": "minecraft:explosion_decay"}]
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
            "conditions": [{"condition": "minecraft:survives_explosion"}] if not slab else [],
            "entries": [entry]}]}


def stairs_state(name):
    base = "scribery:block/" + name
    bottom = {"east": [0, 0, 270, 0, 270], "south": [90, 90, 0, 90, 0],
              "west": [180, 180, 90, 180, 90], "north": [270, 270, 180, 270, 180]}
    top = {"east": [0, 90, 0, 90, 0], "south": [90, 180, 90, 180, 90],
           "west": [180, 270, 180, 270, 180], "north": [270, 0, 270, 0, 270]}
    shapes = ["straight", "outer_right", "outer_left", "inner_right", "inner_left"]
    model = {"straight": base, "outer_right": base + "_outer", "outer_left": base + "_outer",
             "inner_right": base + "_inner", "inner_left": base + "_inner"}
    v = {}
    for half, table in (("bottom", bottom), ("top", top)):
        for facing, ys in table.items():
            for s, y in zip(shapes, ys):
                e = {"model": model[s], "uvlock": True}
                if half == "top":
                    e["x"] = 180
                if y:
                    e["y"] = y
                v[f"facing={facing},half={half},shape={s}"] = e
    return {"variants": v}


def remove_stale():
    for name in STALE:
        for d, ext in ((TEX, ".png"), (os.path.join(A, "blockstates"), ".json"), (os.path.join(A, "models", "block"), ".json"),
                       (os.path.join(A, "models", "item"), ".json"), (D, ".json")):
            if not os.path.isdir(d):
                continue
            for f in os.listdir(d):
                if f == name + ext or (f.startswith(name + "_") and f.endswith(ext) and f[len(name) + 1:-len(ext)].isdigit()):
                    os.remove(os.path.join(d, f))


def main():
    os.makedirs(TEX, exist_ok=True)
    os.makedirs(OUT, exist_ok=True)
    remove_stale()
    # Bildbloecke eines frueheren Laufs mit mehr Teilen entfernen
    for name, im in TEXTURES.items():
        im.save(os.path.join(TEX, name + ".png"))
    cubes = ["roman_bricks", "roman_bricks_weathered", "roman_floor", "mosaic_plain", "mosaic_damaged"]
    for c in cubes:
        w(os.path.join(A, "blockstates", c + ".json"), {"variants": {"": {"model": "scribery:block/" + c}}})
        w(os.path.join(A, "models", "block", c + ".json"),
          {"parent": "minecraft:block/cube_all", "textures": {"all": "scribery:block/" + c}})
        w(os.path.join(A, "models", "item", c + ".json"), {"parent": "scribery:block/" + c})
        w(os.path.join(D, c + ".json"), loot(c))
    # Bildbloecke: nur die Oberseite traegt das Bild, die Seiten sind schlichtes Mosaik
    for pic, (cols, rows, _) in PICTURES.items():
        n = cols * rows
        variants = {}
        for i in range(MAX_PARTS):
            src = min(i, n - 1)  # nicht benutzte Werte zeigen das letzte Teil
            variants[f"part={i}"] = {"model": f"scribery:block/{pic}_{src}"}
        for i in range(n):
            w(os.path.join(A, "models", "block", f"{pic}_{i}.json"),
              {"parent": "minecraft:block/cube_bottom_top",
               "textures": {"top": f"scribery:block/{pic}_{i}", "bottom": "scribery:block/mosaic_plain",
                            "side": "scribery:block/mosaic_plain"}})
        w(os.path.join(A, "blockstates", pic + ".json"), {"variants": variants})
        w(os.path.join(A, "models", "item", pic + ".json"), {"parent": f"scribery:block/{pic}_{n // 2}"})
        w(os.path.join(D, pic + ".json"), loot(pic))
    # Treppe
    t = {"bottom": "scribery:block/roman_bricks", "top": "scribery:block/roman_bricks", "side": "scribery:block/roman_bricks"}
    s = "roman_brick_stairs"
    w(os.path.join(A, "blockstates", s + ".json"), stairs_state(s))
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        w(os.path.join(A, "models", "block", s + suffix + ".json"), {"parent": "minecraft:block/" + parent, "textures": t})
    w(os.path.join(A, "models", "item", s + ".json"), {"parent": "scribery:block/" + s})
    w(os.path.join(D, s + ".json"), loot(s))
    # Stufe
    sl = "roman_brick_slab"
    w(os.path.join(A, "blockstates", sl + ".json"), {"variants": {
        "type=bottom": {"model": "scribery:block/" + sl}, "type=top": {"model": "scribery:block/" + sl + "_top"},
        "type=double": {"model": "scribery:block/roman_bricks"}}})
    w(os.path.join(A, "models", "block", sl + ".json"), {"parent": "minecraft:block/slab", "textures": t})
    w(os.path.join(A, "models", "block", sl + "_top.json"), {"parent": "minecraft:block/slab_top", "textures": t})
    w(os.path.join(A, "models", "item", sl + ".json"), {"parent": "scribery:block/" + sl})
    w(os.path.join(D, sl + ".json"), loot(sl, True))

    # Sprachdatei und Tags
    p = os.path.join(A, "lang", "en_us.json")
    lang = json.load(open(p, encoding="utf-8"))
    for k in STALE:
        lang.pop("block.scribery." + k, None)
    for k, v in NAMES.items():
        lang["block.scribery." + k] = v
    w(p, lang)
    p = os.path.join(RES, "data", "minecraft", "tags", "block", "mineable", "pickaxe.json")
    tag = json.load(open(p, encoding="utf-8"))
    tag["values"] = [v for v in tag["values"] if v.split(":")[1] not in STALE]
    for k in NAMES:
        if "scribery:" + k not in tag["values"]:
            tag["values"].append("scribery:" + k)
    w(p, tag)

    # Vorschau: Ziegel gekachelt, dann die zusammengesetzten Bilder in einem Rahmen aus schlichtem Mosaik
    Z = 4
    pad = N * Z
    wide = N * Z * 3 * 2 + 8 * 4 + 9 * N * Z + 7 * N * Z + 8
    sheet = Image.new("RGBA", (wide + 4 * pad, N * Z * 7 + 8), (40, 36, 34, 255))
    x0 = 4
    for n in ("roman_bricks", "roman_bricks_weathered"):
        for ty in range(3):
            for tx in range(3):
                sheet.paste(TEXTURES[n].resize((N * Z, N * Z), Image.NEAREST), (x0 + tx * N * Z, 4 + ty * N * Z))
        x0 += N * Z * 3 + 8
    for pic, (cols, rows, _) in PICTURES.items():
        for yy in range(rows + 4):
            for xx in range(cols + 4):
                inside = 2 <= xx < cols + 2 and 2 <= yy < rows + 2
                ring = 1 <= xx <= cols + 2 and 1 <= yy <= rows + 2
                tex = (TEXTURES[f"{pic}_{(yy - 2) * cols + (xx - 2)}"] if inside
                       else TEXTURES["mosaic_plain"] if ring else TEXTURES["roman_floor"])
                sheet.paste(tex.resize((N * Z, N * Z), Image.NEAREST), (x0 + xx * N * Z, 4 + yy * N * Z))
        x0 += (cols + 4) * N * Z + 8
    sheet = sheet.crop((0, 0, x0, N * Z * 7 + 8))
    sheet.save(os.path.join(OUT, "catacomb-preview.png"))
    print("ok", len(TEXTURES), "Texturen")


if __name__ == "__main__":
    main()
