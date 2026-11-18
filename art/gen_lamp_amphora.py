"""Erzeugt Oellampe und Lager-Amphore: Texturen, Modelle, Blockstates, Item-Modelle, Loot, Rezepte, Namen, Tags.

Aufruf: python art/gen_lamp_amphora.py

Oellampe (oil_lamp): flache Tonschale mit Schnauze und Docht. Ausgehend aus (lit=false), mit Feuerzeug (Flint and Steel)
oder Feuerball anzuzuenden, mit der leeren Hand zu loeschen. Rezept wie eine Schale aus Scherben (S S / _ S _).
Lager-Amphore (amphora_storage): rote Amphore mit Korkstopfen und neun Platz im Inneren. Rezept wie eine Truhe aus Scherben.
"""
import copy
import json
import os
import random
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "mod", "src", "main", "resources")
A = os.path.join(RES, "assets", "scribery")
D = os.path.join(RES, "data", "scribery")
TEX = os.path.join(A, "textures", "block")
rng = random.Random(20261121)


def jit(c, a=6):
    d = rng.randint(-a, a)
    return tuple(max(0, min(255, v + d)) for v in c)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def lamp_texture():
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            base = (176, 122, 86) if (x + y) % 5 else (162, 110, 78)
            if y > 10:  # Russ und Brandspuren zur Schnauze hin
                base = tuple(int(v * 0.72) for v in base) if x > 8 else base
            im.putpixel((x, y), jit(base, 7) + (255,))
    return im


CLIENT_JAR = os.path.expanduser(r"~\.gradle\caches\neoformruntime\artifacts\minecraft_1.21.1_client.jar")


def oil_texture():
    """Olivenoel: die animierte Wassertextur von Minecraft, ins Olivgoldene umgefaerbt. Braucht das Minecraft-Archiv im
    Gradle-Cache; fehlt es, bleibt die vorhandene Textur im Projekt unveraendert."""
    import io
    import zipfile
    if not os.path.exists(CLIENT_JAR):
        return False
    z = zipfile.ZipFile(CLIENT_JAR)
    src = Image.open(io.BytesIO(z.read("assets/minecraft/textures/block/water_still.png"))).convert("RGBA")
    lum = [src.getpixel((x, y))[2] for y in range(src.height) for x in range(src.width)]
    lo, hi = min(lum), max(lum)
    dark, light = (92, 84, 18), (206, 188, 58)
    out = Image.new("RGBA", src.size)
    for y in range(src.height):
        for x in range(src.width):
            t = (src.getpixel((x, y))[2] - lo) / max(1, hi - lo)
            out.putpixel((x, y), tuple(int(dark[i] + (light[i] - dark[i]) * t) for i in range(3)) + (255,))
    out.save(os.path.join(TEX, "oil_surface.png"))
    with open(os.path.join(TEX, "oil_surface.png.mcmeta"), "w", encoding="utf-8", newline="\n") as f:
        # Sehr langsam: ein Bild alle drei Sekunden, mit weichem Uebergang
        f.write('{\n  "animation": {\n    "frametime": 60,\n    "interpolate": true\n  }\n}\n')
    return True


def wick_texture():
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), jit((196, 176, 140) if y < 8 else (70, 56, 44), 6) + (255,))
    return im


def flame_texture():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rows = {  # y: (halbe Breite, Farbe)
        2: (0, (255, 214, 90)), 3: (1, (255, 190, 64)), 4: (1, (255, 170, 48)), 5: (2, (255, 150, 40)),
        6: (2, (255, 140, 36)), 7: (2, (255, 150, 40)), 8: (2, (255, 170, 48)), 9: (1, (255, 200, 80)),
        10: (1, (255, 226, 130)),
    }
    for y, (hw, c) in rows.items():
        for x in range(8 - hw, 8 + hw + (0 if hw == 0 else 0) + 1):
            if x < 16:
                im.putpixel((x, y), c + (255,))
    for y in (6, 7, 8):  # heller Kern
        im.putpixel((8, y), (255, 240, 190, 255))
    return im


def storage_texture():
    src = Image.open(os.path.join(TEX, "amphora.png")).convert("RGBA")
    out = Image.new("RGBA", src.size)
    dark, light = (86, 40, 30), (204, 118, 80)
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = src.getpixel((x, y))
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255
            t = max(0.0, min(1.0, (lum - 0.2) / 0.7))
            c = tuple(int(dark[i] + (light[i] - dark[i]) * t) for i in range(3))
            out.putpixel((x, y), c + (a,))
    return out


def lid_texture():
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), jit((150, 110, 72) if (x * 3 + y) % 7 else (128, 92, 60), 6) + (255,))
    return im


def faces(tex, uv_up=None):
    f = {d: {"texture": tex} for d in ("north", "south", "east", "west", "up", "down")}
    return f


def box(frm, to, tex):
    return {"from": frm, "to": to, "faces": faces(tex)}


def lamp_models():
    # Nach einer roemischen Tonlampe: runder Koerper mit offener Mitte, darin Olivenoel, ein Rand und eine Schnauze mit Docht.
    base = [
        box([5, 0, 5], [11, 1, 11], "#lamp"),                                   # Fuss
        box([3, 1, 3], [13, 4, 5], "#lamp"), box([3, 1, 11], [13, 4, 13], "#lamp"),   # Wand (Nord, Sued)
        box([3, 1, 5], [5, 4, 11], "#lamp"), box([11, 1, 5], [13, 4, 11], "#lamp"),   # Wand (West, Ost)
        box([5, 1, 5], [11, 2, 11], "#lamp"),                                   # Boden der Schale
        {"from": [5, 2, 5], "to": [11, 3.2, 11], "faces": {"up": {"texture": "#oil"}}},  # das Oel
        box([4, 4, 4], [12, 5, 5], "#lamp"), box([4, 4, 11], [12, 5, 12], "#lamp"),   # Rand
        box([4, 4, 5], [5, 5, 11], "#lamp"), box([11, 4, 5], [12, 5, 11], "#lamp"),
        box([12, 2, 6.5], [15, 4.5, 9.5], "#lamp"),                             # Schnauze
        box([14, 4.5, 7.6], [14.8, 5.6, 8.4], "#wick"),                         # Docht
    ]
    tex = {"lamp": "scribery:block/oil_lamp", "oil": "scribery:block/oil_surface", "wick": "scribery:block/oil_lamp_wick",
           "particle": "scribery:block/oil_lamp"}
    unlit = {"textures": tex, "elements": base}
    # Wie bei Kerzen gibt es kein Flammenmodell: Die Flamme sind die kleinen Flammenteilchen des Spiels (siehe OilLampBlock).
    lit = {"textures": tex, "elements": base}
    return unlit, lit


def main():
    os.makedirs(TEX, exist_ok=True)
    lamp_texture().save(os.path.join(TEX, "oil_lamp.png"))
    oil_texture()
    wick_texture().save(os.path.join(TEX, "oil_lamp_wick.png"))
    flame_file = os.path.join(TEX, "oil_lamp_flame.png")
    if os.path.exists(flame_file):
        os.remove(flame_file)  # frueher gab es ein eigenes Flammenbild; jetzt wie bei Kerzen nur Teilchen
    storage_texture().save(os.path.join(TEX, "amphora_storage.png"))
    lid_texture().save(os.path.join(TEX, "amphora_lid.png"))
    unlit, lit = lamp_models()
    w(os.path.join(A, "models", "block", "oil_lamp.json"), unlit)
    w(os.path.join(A, "models", "block", "oil_lamp_lit.json"), lit)
    w(os.path.join(A, "models", "item", "oil_lamp.json"), {"parent": "scribery:block/oil_lamp"})
    w(os.path.join(A, "blockstates", "oil_lamp.json"), {"variants": {
        "lit=false": {"model": "scribery:block/oil_lamp"}, "lit=true": {"model": "scribery:block/oil_lamp_lit"}}})
    # Lager-Amphore: Modell der Amphore, andere Textur, Korkstopfen statt Lippe
    m = json.load(open(os.path.join(A, "models", "block", "amphora.json"), encoding="utf-8"))
    m = copy.deepcopy(m)
    m["textures"] = {"t": "scribery:block/amphora_storage", "lid": "scribery:block/amphora_lid",
                     "particle": "scribery:block/amphora_storage"}
    for e in m["elements"]:
        if e["from"] == [5, 15, 5]:
            e["from"], e["to"] = [6, 15, 6], [10, 16, 10]
            for f in e["faces"].values():
                f["texture"] = "#lid"
                f.pop("uv", None)
    w(os.path.join(A, "models", "block", "amphora_storage.json"), m)
    w(os.path.join(A, "models", "item", "amphora_storage.json"), {"parent": "scribery:block/amphora_storage"})
    w(os.path.join(A, "blockstates", "amphora_storage.json"), {"variants": {"": {"model": "scribery:block/amphora_storage"}}})
    for name in ("oil_lamp", "amphora_storage"):
        w(os.path.join(D, "loot_table", "blocks", name + ".json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0, "conditions": [{"condition": "minecraft:survives_explosion"}],
                       "entries": [{"type": "minecraft:item", "name": "scribery:" + name}]}]})
    shard = {"item": "scribery:amphora_shard"}
    w(os.path.join(D, "recipe", "oil_lamp.json"), {
        "type": "minecraft:crafting_shaped", "category": "misc", "key": {"S": shard},
        "pattern": ["S S", " S "], "result": {"id": "scribery:oil_lamp", "count": 1}})
    w(os.path.join(D, "recipe", "amphora_storage.json"), {
        "type": "minecraft:crafting_shaped", "category": "misc", "key": {"S": shard},
        "pattern": ["SSS", "S S", "SSS"], "result": {"id": "scribery:amphora_storage", "count": 1}})
    p = os.path.join(A, "lang", "en_us.json")
    lang = json.load(open(p, encoding="utf-8"))
    lang["block.scribery.oil_lamp"] = "Oil Lamp"
    lang["block.scribery.amphora_storage"] = "Storage Amphora"
    lang["container.scribery.amphora_storage"] = "Storage Amphora"
    w(p, lang)
    p = os.path.join(RES, "data", "minecraft", "tags", "block", "mineable", "pickaxe.json")
    tag = json.load(open(p, encoding="utf-8"))
    for k in ("scribery:oil_lamp", "scribery:amphora_storage"):
        if k not in tag["values"]:
            tag["values"].append(k)
    w(p, tag)
    print("ok")


if __name__ == "__main__":
    main()
