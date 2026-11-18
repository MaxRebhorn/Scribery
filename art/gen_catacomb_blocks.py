"""Erzeugt die Blockgruppe der Katakomben: roemische Ziegel (Block, Treppe, Stufe, verwittert) und Mosaike.

Aufruf: python art/gen_catacomb_blocks.py
Ausgabe: Texturen, Blockstates, Modelle, Item-Modelle und Loot-Tabellen direkt unter mod/src/main/resources,
dazu art/out/catacomb-preview.png.

Vorbild: Duenne roemische Ziegel (Ostia, Katakomben): sehr flache Lagen, rotbraun, der Moertel kaum heller. Mosaike
aus kleinen Steinchen: ein einzelnes Bild (Fisch, Chi-Rho) in einem Ring, aus 3x3 Bloecken zusammengesetzt, drumherum
schlichte cremefarbene Steinchen. Der uebrige Boden der Katakombe bleibt normaler Stein.
Die Bildbloecke tragen die Eigenschaft "part" (0-8, Zeile * 3 + Spalte, Zeile 0 = Norden, Spalte 0 = Westen).
"""
import json
import os
from PIL import Image
import random

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "mod", "src", "main", "resources")
A = os.path.join(RES, "assets", "scribery")
D = os.path.join(RES, "data", "scribery", "loot_table", "blocks")
TEX = os.path.join(A, "textures", "block")
OUT = os.path.join(ROOT, "art", "out")
N = 16
rng = random.Random(20261119)

BRICK = [(132, 68, 52), (144, 78, 58), (120, 62, 48), (152, 86, 64), (138, 72, 54)]
MORTAR = (176, 128, 102)       # kalkiger Moertel im Schatten: nur wenig heller als der Ziegel
CREAM = [(226, 210, 176), (218, 201, 166), (232, 217, 184)]
DARK = [(58, 52, 50), (48, 43, 42), (66, 59, 56)]
RED = [(160, 74, 54), (150, 66, 48), (170, 82, 60)]
OCHRE = [(205, 160, 80), (196, 150, 72), (214, 170, 90)]
BLUE = [(52, 78, 96), (44, 68, 86), (60, 88, 106)]
BED = (150, 128, 98)  # Moertelbett, wo Steinchen fehlen


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


def tesserae_img(cells, size, missing=0.0):
    """Mosaik aus 2x2-Steinchen. cells[y][x] ist die Farbe einer Steinchenzelle."""
    im = Image.new("RGBA", (size * 2, size * 2))
    for cy in range(size):
        for cx in range(size):
            base = cells[cy][cx]
            gone = rng.random() < missing
            for dy in range(2):
                for dx in range(2):
                    c = BED if gone else jit(base, 4)
                    if not gone and (dx == 1 or dy == 1):
                        c = tuple(int(v * 0.9) for v in c)
                    im.putpixel((cx * 2 + dx, cy * 2 + dy), c + (255,))
    return im


def m_plain():
    return tesserae_img([[pick(CREAM) for _ in range(8)] for _ in range(8)], 8)


def m_damaged():
    return tesserae_img([[pick(CREAM) if (x + y * 3) % 5 else pick(DARK) for x in range(8)] for y in range(8)], 8, missing=0.38)


S = 24  # Bild aus 3x3 Bloecken = 24x24 Steinchen


def ring_base():
    """Kreis mit rotem und dunklem Ring, aussen cremefarben mit Ocker-Punkten."""
    g = [[None] * S for _ in range(S)]
    for y in range(S):
        for x in range(S):
            r = ((x - 11.5) ** 2 + (y - 11.5) ** 2) ** .5
            if r < 8.6:
                c = pick(CREAM)
            elif r < 9.6:
                c = pick(RED)
            elif r < 10.8:
                c = pick(DARK)
            elif r < 11.9:
                c = pick(CREAM)
            else:
                c = pick(OCHRE) if (x + y) % 4 == 0 else pick(CREAM)
            g[y][x] = c
    return g


def fish_picture():
    g = ring_base()
    for y in range(S):
        for x in range(S):
            if ((x - 11.5) ** 2 + (y - 11.5) ** 2) ** .5 >= 8.6:
                continue
            if y in (6, 17) and 6 < x < 17 and (x // 2) % 2 == 0:  # Wellen
                g[y][x] = pick(BLUE)
    for k in range(4):  # Schwanzflosse links: ein Dreieck, das sich nach aussen oeffnet
        for dy in range(-(k + 1), k + 2):
            g[11 + dy][8 - k] = pick(DARK) if abs(dy) == k + 1 else pick(RED)
    for y in range(S):
        for x in range(S):
            ex, ey = (x - 13.5) / 5.6, (y - 11.5) / 3.4
            d = ex * ex + ey * ey
            if d <= 1.0:
                g[y][x] = pick(OCHRE) if d < 0.7 else pick(DARK)
                if y > 12 and d < 0.7:
                    g[y][x] = pick(CREAM) if (x + y) % 3 == 0 else pick(OCHRE)  # heller Bauch
    for x in range(12, 17):  # Rueckenflosse
        g[8][x] = pick(RED)
    for x in range(13, 16):
        g[7][x] = pick(RED)
    for x in range(12, 16):  # Bauchflosse
        g[15][x] = pick(RED)
    g[10][17] = pick(DARK)       # Auge
    for y in range(9, 14):       # Kieme
        g[y][15] = pick(RED)
    return g


def chi_rho_picture():
    g = ring_base()
    for k in range(-7, 8):  # Chi
        for t in (0, 1, 2):
            for x, y in ((10 + k + t, 11 + k), (12 - k + t, 11 + k)):
                if 0 <= x < S and 0 <= y < S and ((x - 11.5) ** 2 + (y - 11.5) ** 2) ** .5 < 8.4:
                    g[y][x] = pick(RED)
    for y in range(5, 19):  # Rho: Stab und Schlaufe
        for x in (11, 12):
            g[y][x] = pick(DARK)
    for x in range(11, 17):
        for y in (5, 6, 10, 11):
            g[y][x] = pick(DARK)
    for y in range(5, 12):
        for x in (15, 16):
            g[y][x] = pick(DARK)
    return g


def split_picture(cells):
    """24x24 Zellen -> neun Bloecke, Index = Zeile * 3 + Spalte."""
    big = tesserae_img(cells, S)
    return [big.crop((c * 16, r * 16, c * 16 + 16, r * 16 + 16)) for r in range(3) for c in range(3)]


TEXTURES = {
    "roman_bricks": bricks(False),
    "roman_bricks_weathered": bricks(True),
    "mosaic_plain": m_plain(),
    "mosaic_damaged": m_damaged(),
}
PICTURES = {"mosaic_fish": split_picture(fish_picture()), "mosaic_chi_rho": split_picture(chi_rho_picture())}
for _name, _parts in PICTURES.items():
    for _i, _im in enumerate(_parts):
        TEXTURES[f"{_name}_{_i}"] = _im

NAMES = {
    "roman_bricks": "Roman Bricks",
    "roman_brick_stairs": "Roman Brick Stairs",
    "roman_brick_slab": "Roman Brick Slab",
    "roman_bricks_weathered": "Weathered Roman Bricks",
    "mosaic_plain": "Mosaic Floor",
    "mosaic_damaged": "Damaged Mosaic Floor",
    "mosaic_fish": "Fish Mosaic",
    "mosaic_chi_rho": "Chi-Rho Mosaic",
}
STALE = ["mosaic_checker", "mosaic_lozenge", "mosaic_rosette"]


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


def remove(path):
    if os.path.exists(path):
        os.remove(path)


def main():
    os.makedirs(TEX, exist_ok=True)
    os.makedirs(OUT, exist_ok=True)
    for name in STALE:
        for p in (os.path.join(TEX, name + ".png"), os.path.join(A, "blockstates", name + ".json"),
                  os.path.join(A, "models", "block", name + ".json"), os.path.join(A, "models", "item", name + ".json"),
                  os.path.join(D, name + ".json")):
            remove(p)
    for name, im in TEXTURES.items():
        im.save(os.path.join(TEX, name + ".png"))
    cubes = ["roman_bricks", "roman_bricks_weathered", "mosaic_plain", "mosaic_damaged"]
    for c in cubes:
        w(os.path.join(A, "blockstates", c + ".json"), {"variants": {"": {"model": "scribery:block/" + c}}})
        w(os.path.join(A, "models", "block", c + ".json"),
          {"parent": "minecraft:block/cube_all", "textures": {"all": "scribery:block/" + c}})
        w(os.path.join(A, "models", "item", c + ".json"), {"parent": "scribery:block/" + c})
        w(os.path.join(D, c + ".json"), loot(c))
    # Bildbloecke: nur die Oberseite traegt das Bild
    for pic in PICTURES:
        variants = {}
        for i in range(9):
            variants[f"part={i}"] = {"model": f"scribery:block/{pic}_{i}"}
            w(os.path.join(A, "models", "block", f"{pic}_{i}.json"),
              {"parent": "minecraft:block/cube_bottom_top",
               "textures": {"top": f"scribery:block/{pic}_{i}", "bottom": "scribery:block/mosaic_plain",
                            "side": "scribery:block/mosaic_plain"}})
        w(os.path.join(A, "blockstates", pic + ".json"), {"variants": variants})
        w(os.path.join(A, "models", "item", pic + ".json"), {"parent": f"scribery:block/{pic}_4"})
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

    # Vorschau: Ziegel (gekachelt), Mosaikbilder (3x3 zusammengesetzt), schlicht, beschaedigt
    Z = 5
    sheet = Image.new("RGBA", (N * Z * 3 * 4 + 40, N * Z * 3 + 8), (40, 36, 34, 255))
    x0 = 4
    for n in ("roman_bricks", "roman_bricks_weathered"):
        for ty in range(3):
            for tx in range(3):
                sheet.paste(TEXTURES[n].resize((N * Z, N * Z), Image.NEAREST), (x0 + tx * N * Z, 4 + ty * N * Z))
        x0 += N * Z * 3 + 8
    for pic in PICTURES:
        for i in range(9):
            sheet.paste(TEXTURES[f"{pic}_{i}"].resize((N * Z, N * Z), Image.NEAREST), (x0 + (i % 3) * N * Z, 4 + (i // 3) * N * Z))
        x0 += N * Z * 3 + 8
    sheet = sheet.crop((0, 0, x0, N * Z * 3 + 8))
    sheet.save(os.path.join(OUT, "catacomb-preview.png"))
    print("ok", len(TEXTURES), "Texturen")


if __name__ == "__main__":
    main()
