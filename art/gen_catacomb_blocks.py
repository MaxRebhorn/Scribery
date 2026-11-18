"""Erzeugt die Blockgruppe der Katakomben: roemische Ziegel (Block, Treppe, Stufe, verwittert) und Mosaikboeden.

Aufruf: python art/gen_catacomb_blocks.py
Ausgabe: Texturen, Blockstates, Modelle, Item-Modelle und Loot-Tabellen direkt unter mod/src/main/resources,
dazu art/out/catacomb-preview.png (alle Texturen 6x vergroessert).

Vorbild: duenne roemische Ziegel in lagenweisem Verband mit hellem Moertel (Ostia, Katakomben) und Tesserae-Mosaike
mit geometrischen Mustern (Schachbrett, Raute, Rosette). Die Mosaike bestehen aus 2x2-Steinchen mit leichtem Farbrauschen.
Vanilla-nahe, wenige nahe Toene.
"""
import json
import os
import random
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "mod", "src", "main", "resources")
TEX = os.path.join(RES, "assets", "scribery", "textures", "block")
OUT = os.path.join(ROOT, "art", "out")
N = 16
rng = random.Random(20261119)

BRICK = [(190, 112, 84), (178, 100, 76), (201, 126, 94), (168, 94, 72), (196, 119, 88)]
MORTAR = (214, 192, 164)
MORTAR_SHADE = (190, 168, 142)

CREAM = [(226, 210, 176), (218, 201, 166), (232, 217, 184)]
DARK = [(58, 52, 50), (48, 43, 42), (66, 59, 56)]
RED = [(160, 74, 54), (150, 66, 48), (170, 82, 60)]
OCHRE = [(205, 160, 80), (196, 150, 72), (214, 170, 90)]
BED = (150, 128, 98)  # Moertelbett, wo Steinchen fehlen


def jit(c, a=5):
    d = rng.randint(-a, a)
    return tuple(max(0, min(255, v + d)) for v in c)


def bricks(weathered):
    im = Image.new("RGBA", (N, N))
    for row in range(4):
        off = 0 if row % 2 == 0 else 4
        for seg in range(2):
            x0 = (seg * 8 + off) % N
            col = rng.choice(BRICK)
            if weathered:
                col = tuple(int(v * rng.uniform(0.78, 0.92)) for v in col)
            lost = weathered and rng.random() < 0.18
            for dy in range(3):
                for dx in range(8):
                    x = (x0 + dx) % N
                    y = row * 4 + dy
                    c = MORTAR_SHADE if lost else jit(col, 7)
                    if dy == 0 and not lost:  # Licht von oben
                        c = tuple(min(255, v + 8) for v in c)
                    im.putpixel((x, y), c + (255,))
            # Moertelfuge unter jeder Lage und neben jedem Stein
            for dx in range(8):
                im.putpixel(((x0 + dx) % N, row * 4 + 3), jit(MORTAR, 4) + (255,))
            for dy in range(3):
                im.putpixel(((x0 + 7) % N, row * 4 + dy), jit(MORTAR, 4) + (255,))
    if weathered:
        for _ in range(26):  # Schmutz und Ausbluehungen
            x, y = rng.randrange(N), rng.randrange(N)
            p = im.getpixel((x, y))
            k = rng.choice([0.82, 0.9, 1.1])
            im.putpixel((x, y), tuple(max(0, min(255, int(v * k))) for v in p[:3]) + (255,))
    return im


def tesserae(color_at, missing=0.0):
    """Ein Mosaik aus 2x2-Steinchen. color_at(cx, cy) liefert die Grundfarbe der Steinchenzelle (8x8 Zellen)."""
    im = Image.new("RGBA", (N, N))
    for cy in range(8):
        for cx in range(8):
            base = color_at(cx, cy)
            gone = rng.random() < missing
            for dy in range(2):
                for dx in range(2):
                    c = BED if gone else jit(base, 4)
                    if not gone and (dx == 1 or dy == 1):  # feine Fuge rechts und unten
                        c = tuple(int(v * 0.9) for v in c)
                    im.putpixel((cx * 2 + dx, cy * 2 + dy), c + (255,))
    return im


def pick(lst):
    return rng.choice(lst)


def m_plain():
    return tesserae(lambda x, y: pick(CREAM))


def m_checker():
    return tesserae(lambda x, y: pick(DARK) if ((x // 2) + (y // 2)) % 2 == 0 else pick(CREAM))


def m_lozenge():
    def f(x, y):
        d = abs(x - 3.5) + abs(y - 3.5)
        if d < 2:
            return pick(RED)
        if d < 3:
            return pick(CREAM)
        if d < 4.5:
            return pick(DARK)
        return pick(CREAM)
    return tesserae(f)


def m_rosette():
    def f(x, y):
        r = ((x - 3.5) ** 2 + (y - 3.5) ** 2) ** .5
        if r < 1.4:
            return pick(OCHRE)
        if r < 2.4:
            return pick(RED)
        if r < 3.1:
            return pick(CREAM)
        if r < 4.2:
            return pick(DARK)
        return pick(OCHRE) if (x + y) % 2 == 0 else pick(CREAM)
    return tesserae(f)


def m_damaged():
    return tesserae(lambda x, y: pick(CREAM) if (x + y * 3) % 5 else pick(DARK), missing=0.38)


TEXTURES = {
    "roman_bricks": bricks(False),
    "roman_bricks_weathered": bricks(True),
    "mosaic_plain": m_plain(),
    "mosaic_checker": m_checker(),
    "mosaic_lozenge": m_lozenge(),
    "mosaic_rosette": m_rosette(),
    "mosaic_damaged": m_damaged(),
}

NAMES = {
    "roman_bricks": "Roman Bricks",
    "roman_brick_stairs": "Roman Brick Stairs",
    "roman_brick_slab": "Roman Brick Slab",
    "roman_bricks_weathered": "Weathered Roman Bricks",
    "mosaic_plain": "Mosaic Floor",
    "mosaic_checker": "Checkered Mosaic Floor",
    "mosaic_lozenge": "Lozenge Mosaic Floor",
    "mosaic_rosette": "Rosette Mosaic Floor",
    "mosaic_damaged": "Damaged Mosaic Floor",
}


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
    rot = {  # (facing, half, shape) -> (x, y)
    }
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


def main():
    os.makedirs(TEX, exist_ok=True)
    os.makedirs(OUT, exist_ok=True)
    for name, im in TEXTURES.items():
        im.save(os.path.join(TEX, name + ".png"))
    A = os.path.join(RES, "assets", "scribery")
    D = os.path.join(RES, "data", "scribery", "loot_table", "blocks")
    cubes = ["roman_bricks", "roman_bricks_weathered", "mosaic_plain", "mosaic_checker", "mosaic_lozenge",
             "mosaic_rosette", "mosaic_damaged"]
    for c in cubes:
        w(os.path.join(A, "blockstates", c + ".json"), {"variants": {"": {"model": "scribery:block/" + c}}})
        w(os.path.join(A, "models", "block", c + ".json"),
          {"parent": "minecraft:block/cube_all", "textures": {"all": "scribery:block/" + c}})
        w(os.path.join(A, "models", "item", c + ".json"), {"parent": "scribery:block/" + c})
        w(os.path.join(D, c + ".json"), loot(c))
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
    st = {"bottom": "scribery:block/roman_bricks", "top": "scribery:block/roman_bricks", "side": "scribery:block/roman_bricks"}
    w(os.path.join(A, "blockstates", sl + ".json"), {"variants": {
        "type=bottom": {"model": "scribery:block/" + sl}, "type=top": {"model": "scribery:block/" + sl + "_top"},
        "type=double": {"model": "scribery:block/roman_bricks"}}})
    w(os.path.join(A, "models", "block", sl + ".json"), {"parent": "minecraft:block/slab", "textures": st})
    w(os.path.join(A, "models", "block", sl + "_top.json"), {"parent": "minecraft:block/slab_top", "textures": st})
    w(os.path.join(A, "models", "item", sl + ".json"), {"parent": "scribery:block/" + sl})
    w(os.path.join(D, sl + ".json"), loot(sl, True))

    # Sprachdatei und Tags
    p = os.path.join(A, "lang", "en_us.json")
    lang = json.load(open(p, encoding="utf-8"))
    for k, v in NAMES.items():
        lang["block.scribery." + k] = v
    w(p, lang)
    p = os.path.join(RES, "data", "minecraft", "tags", "block", "mineable", "pickaxe.json")
    tag = json.load(open(p, encoding="utf-8"))
    for k in NAMES:
        if "scribery:" + k not in tag["values"]:
            tag["values"].append("scribery:" + k)
    w(p, tag)

    # Vorschau
    names = list(TEXTURES)
    sheet = Image.new("RGBA", (N * 6 * len(names) + 8 * len(names), N * 6 * 2 + 8), (40, 36, 34, 255))
    for i, n in enumerate(names):
        big = TEXTURES[n].resize((N * 6, N * 6), Image.NEAREST)
        sheet.paste(big, (i * (N * 6 + 8), 4))
        small = TEXTURES[n]
        for ty in range(2):
            for tx in range(2):
                sheet.paste(small.resize((N * 3, N * 3), Image.NEAREST), (i * (N * 6 + 8) + tx * N * 3, N * 6 + 8 + ty * N * 3))
    sheet.save(os.path.join(OUT, "catacomb-preview.png"))
    print("ok", len(TEXTURES), "Texturen")


if __name__ == "__main__":
    main()
