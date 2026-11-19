"""Item-Texturen fuer die Bauteile des Minting Table: Mini-Amboss und Giessform (16x16).
Aufruf: python art/gen_components.py   -> mod/.../textures/item/mini_anvil.png, casting_mold.png, art/out/components-preview.png
Farben: Blei-Toene aus art/palette.json (Eisen), gebleichte Tonfarben wie in gen_finds.py."""
import json, os
from PIL import Image
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "mod/src/main/resources/assets/scribery/textures/item")
raw = json.load(open(os.path.join(ROOT, "art/palette.json")))
H = lambda k: tuple(int(raw[k][i:i+2], 16) for i in (1, 3, 5)) + (255,)
L0, L1, L2, L3, L4 = (H(f"lead_{i}") for i in range(5))
CLAY = [(96, 76, 62, 255), (168, 136, 108, 255), (198, 168, 136, 255), (226, 204, 172, 255)]

def put(im, rows, pal):
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal: im.putpixel((x, y), pal[ch])

def anvil():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rows = [
        "................",
        "................",
        "..0000000000....",
        ".033444444332000",
        ".03344444433200.",
        "..0233333322000.",
        "...02333322.0...",
        "....023322......",
        "....023322......",
        "....023322......",
        "...0223322......",
        "..002333320.....",
        ".0023333332200..",
        ".0222222222220..",
        ".0111111111110..",
        "..00000000000...",
    ]
    rows = [r.ljust(16, ".")[:16] for r in rows]
    put(im, rows, {"0": L0, "1": L1, "2": L2, "3": L3, "4": L4})
    return im

def mold():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rows = [
        "................",
        "..000000000000..",
        ".02222222222210.",
        ".02333333333210.",
        ".02311111113210.",
        ".02310000013210.",
        ".02310000013210.",
        ".02310000013210.",
        ".02310000013210.",
        ".02311111113210.",
        ".02333333333210.",
        ".01222222222110.",
        ".00111111111100.",
        "..000000000000..",
        "................",
        "................",
    ]
    c = {"0": CLAY[0], "1": (130, 102, 82, 255), "2": CLAY[2], "3": CLAY[1]}
    put(im, rows, c)
    # Muenzmulde: dunkel, mit hellem Rand unten rechts
    for y in range(5, 9):
        for x in range(6, 10): im.putpixel((x, y), (58, 54, 56, 255))
    return im

if __name__ == "__main__":
    a, m = anvil(), mold()
    a.save(os.path.join(OUT, "mini_anvil.png")); m.save(os.path.join(OUT, "casting_mold.png"))
    pv = Image.new("RGBA", (16 * 8 * 2 + 24, 16 * 8 + 16), (198, 198, 198, 255))
    for i, im in enumerate((a, m)):
        pv.alpha_composite(im.resize((128, 128), Image.NEAREST), (8 + i * 144, 8))
    os.makedirs(os.path.join(ROOT, "art/out"), exist_ok=True)
    pv.convert("RGB").save(os.path.join(ROOT, "art/out/components-preview.png"))
