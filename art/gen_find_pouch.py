"""Item-Texturen der fuenf Fundbeutel des Scribe Villagers (16x16): Leinenbeutel mit Schnur und Fundart-Zeichen.
Aufruf: python art/gen_find_pouch.py -> mod/.../textures/item/find_pouch_{sand,mud,ashes,ice,shards}.png
Farben: Holz-/Pergamenttoene aus art/palette.json fuer Schnur und Rand, Beutelfarbe je Fundart."""
import json, os
from PIL import Image
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "mod/src/main/resources/assets/scribery/textures/item")
raw = json.load(open(os.path.join(ROOT, "art/palette.json")))
H = lambda k: tuple(int(raw[k][i:i+2], 16) for i in (1, 3, 5)) + (255,)
STRING = [H("wood_1"), H("wood_3")]
OUTLINE = H("ink_1")
# je Fundart: (Beutel dunkel, mittel, hell, Zeichen)
KINDS = {
    "sand":   ((176, 144, 92, 255), (212, 182, 124, 255), (234, 212, 160, 255), (120, 90, 50, 255)),
    "mud":    ((78, 62, 48, 255), (104, 84, 62, 255), (130, 108, 82, 255), (60, 96, 110, 255)),
    "ashes":  ((58, 58, 62, 255), (86, 86, 90, 255), (116, 116, 120, 255), (200, 90, 40, 255)),
    "ice":    ((120, 156, 196, 255), (166, 200, 232, 255), (214, 236, 250, 255), (255, 255, 255, 255)),
    "shards": ((150, 82, 56, 255), (186, 112, 78, 255), (214, 148, 108, 255), (240, 216, 180, 255)),
}
BODY = [
    "................",
    "......ss.ss.....",
    ".......ssss.....",
    "......oooooo....",
    ".....owwwwwwo...",
    "....obbbbbbbbo..",
    "...obmmmmmmmmbo.",
    "..obmmmmmmmmmmbo",
    "..obmmmccmmmmmbo",
    "..obmmmccmmmmmbo",
    "..obmmmmmmmmmmbo",
    "..obbmmmmmmmmbbo",
    "...obbbbbbbbbbo.",
    "....oooooooooo..",
    "................",
    "................",
]
for name, (dark, mid, light, mark) in KINDS.items():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pal = {"o": OUTLINE, "b": dark, "m": mid, "w": light, "c": mark, "s": STRING[0]}
    for y, row in enumerate(BODY):
        for x, ch in enumerate(row):
            if ch in pal: im.putpixel((x, y), pal[ch])
    im.putpixel((8, 1), STRING[1]); im.putpixel((9, 2), STRING[1])
    im.save(os.path.join(OUT, f"find_pouch_{name}.png"))
print("fertig:", ", ".join(KINDS))
