"""Erzeugt Fund-Texturen: Amphore (Blocktextur), Ostrakon (Item), Fundstuecke im Schlamm (4 Bruststufen, transparent).

Aufruf: python art/gen_finds.py
Ausgabe: art/out/finds/*.png und art/out/finds-preview.png

Die Schlamm-Overlays liegen als zweite, transparente Ebene ueber der Vanilla-Textur minecraft:block/mud
(siehe models/block/suspicious_mud_*.json, suspicious_sand_*.json, suspicious_ashes_*.json). Sie zeigen Holzsplitter
und Papierstuecke, die mit jeder Stufe mehr herausschauen (die Stuecke der Stufe n bleiben in Stufe n+1 erhalten).
Schlamm: gebleichtes Holz, Sand: dunkleres Holz, Asche: verkohltes Holz und angesengtes Papier (uralt, nichts glueht).
Farben: art/palette.json plus die gebleichten Tonfarben aus gen_sherd_block.py.
"""
import json
import os
import random
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "art/out/finds")
SEED = 20261118

_raw = json.load(open(os.path.join(ROOT, "art/palette.json")))
P = {k: tuple(int(v[i:i + 2], 16) for i in (1, 3, 5)) + (255,)
     for k, v in _raw.items() if isinstance(v, str) and v.startswith("#")}
CLAY = [(96, 76, 62, 255), (168, 136, 108, 255), (198, 168, 136, 255), (226, 204, 172, 255)]
GLAZE = (58, 54, 56, 255)
FADED_RED = (150, 96, 80, 255)
BLEACH = [(120, 108, 90, 255), (168, 154, 128, 255), (205, 192, 164, 255)]  # gebleichtes Holz: dunkel, mittel, hell


# ------------------------------------------------------------------------------------------------ Amphore
def amphora():
    """16x16. Das Modell mappt die Texturflaechen ueber die Blockkoordinaten (u = x bzw. z, v = 16 - y).
    Zeile v: 0-1 Lippe, 1-4 Hals, 4-6 Schulter, 6-14 Bauch, 14-16 Fuss. Ecke (0..3, 0..3) = glatter Ton fuer Ober-/Unterseiten."""
    rng = random.Random(SEED)
    im = Image.new("RGBA", (16, 16), CLAY[1])
    for y in range(16):
        for x in range(16):
            c = CLAY[2] if x < 7 else (CLAY[1] if x < 11 else CLAY[0] if False else CLAY[1])
            if x >= 11:
                c = tuple(int(v * .86) for v in CLAY[1][:3]) + (255,)  # rechte Seite im Schatten
            im.putpixel((x, y), c)
    for _ in range(26):  # Tonkoernung
        x, y = rng.randrange(16), rng.randrange(16)
        im.putpixel((x, y), CLAY[2] if rng.random() < .5 else CLAY[1])
    for x in range(16):
        im.putpixel((x, 5), FADED_RED)              # Schulterstreifen
        im.putpixel((x, 7), CLAY[3] if x % 2 == 0 else CLAY[2])   # Ritzlinie
        for y in (8, 9):
            im.putpixel((x, y), GLAZE)               # Glasurband am Bauch
        im.putpixel((x, 10), CLAY[3] if x % 2 == 1 else CLAY[2])
        if x % 4 in (1, 2):
            im.putpixel((x, 9), CLAY[1])             # Maander-Andeutung im Band
        if x % 4 == 2:
            im.putpixel((x, 8), CLAY[1])
    for y in range(11, 14):  # unterer Bauch: Strahlenkranz (Striche)
        for x in range(16):
            if x % 3 == 0:
                im.putpixel((x, y), CLAY[0] if y == 13 else CLAY[1])
    for y in range(14, 16):  # Fuss dunkler
        for x in range(16):
            im.putpixel((x, y), CLAY[0] if x >= 6 else CLAY[1])
    for y in range(0, 3):    # Ober-/Unterseiten-Flaeche
        for x in range(0, 3):
            im.putpixel((x, y), CLAY[2] if (x + y) % 2 else CLAY[1])
    return im


# ------------------------------------------------------------------------------------------------ Ostrakon
def ostrakon():
    """Tonscherbe mit schwarzer Tinte: drei Zeilen kurzer Striche, wie hastig beschrieben."""
    rng = random.Random(SEED + 1)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    poly = [(1, 7), (5, 2), (9, 3), (14, 2), (14, 8), (11, 10), (12, 14), (6, 14), (3, 11), (1, 12)]
    d.polygon(poly, fill=CLAY[2])
    mask = [(x, y) for x in range(16) for y in range(16) if im.getpixel((x, y))[3]]
    for (x, y) in mask:  # Licht von links oben, Schatten rechts unten
        if x + y < 11:
            im.putpixel((x, y), CLAY[3])
        elif x + y > 19:
            im.putpixel((x, y), CLAY[1])
    ms = set(mask)
    for (x, y) in mask:  # Kontur im Ton, damit sie zu den Fragment-Icons passt (dunkler Rand)
        if any((x + dx, y + dy) not in ms for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            im.putpixel((x, y), P["wood_1"])
    for y, x0, x1 in ((6, 4, 12), (9, 3, 11), (12, 5, 10)):  # Schriftzeilen: Striche mit kurzen Luecken
        x = x0
        while x < x1:
            n = rng.choice([2, 3, 4])
            for k in range(n):
                px = (x + k, y)
                if px in ms and im.getpixel(px) != P["wood_1"] and (x + k, y - 1) in ms and (x + k, y + 1) in ms:
                    im.putpixel(px, GLAZE)
            x += n + 1
    return im


# ------------------------------------------------------------------------------------------------ Schlamm
# Farben der Fundstuecke je Untergrund: Holz (dunkel, mittel, hell), Papier (hell, mittel, Tinte)
PALETTES = {
    "mud": dict(wood=BLEACH, paper=(P["parchment_3"], P["parchment_2"], P["parchment_1"])),
    # auf hellem Sand muessen die Stuecke dunkler sein als der Untergrund
    "sand": dict(wood=[(84, 66, 48, 255), (116, 92, 66, 255), (150, 124, 92, 255)],
                 paper=(P["parchment_3"], P["parchment_2"], P["parchment_1"])),
    # Asche: uraltes, verkohltes Holz und brueckiges, angesengtes Papier (kalt, nichts glueht)
    "ashes": dict(wood=[(22, 20, 20, 255), (44, 38, 36, 255), (74, 64, 58, 255)],
                  paper=((96, 78, 60, 255), (62, 50, 42, 255), (34, 28, 26, 255))),
}


def mud_bits(kind="mud"):
    """Liste von Fundstuecken (Art, Pixel). Reihenfolge = Reihenfolge des Erscheinens."""
    pal = PALETTES[kind]
    BLEACH = pal["wood"]
    paper_hi, paper_mid, paper_lo = pal["paper"]
    rng = random.Random(SEED + 2)
    bits = []
    spots = [(3, 4), (11, 9), (6, 12), (12, 3), (2, 10), (8, 6), (13, 13), (5, 1)]
    kinds = ["wood", "paper", "wood", "paper", "wood", "paper", "wood", "paper"]
    for (x, y), k in zip(spots, kinds):
        px = []
        if k == "wood":
            horiz = rng.random() < .5
            n = rng.choice([3, 4, 4, 5])
            for i in range(n):
                px.append((x + i, y, BLEACH[1]) if horiz else (x, y + i, BLEACH[1]))
            px[0] = (px[0][0], px[0][1], BLEACH[2])      # helles Ende
            px[-1] = (px[-1][0], px[-1][1], BLEACH[0])   # dunkles Ende
            if n > 3:
                ox, oy = (px[1][0], px[1][1] + 1) if horiz else (px[1][0] + 1, px[1][1])
                px.append((ox, oy, BLEACH[0]))           # Astloch/Schatten
        else:
            for dx in range(3):
                for dy in range(2):
                    px.append((x + dx, y + dy, paper_hi))
            px.append((x, y + 1, paper_mid))
            px.append((x + 1, y + 1, paper_lo))  # Tintenspur/Schatten
            px = [p for p in px if not (p[0] == x + 2 and p[1] == y + 1)]  # abgerissene Ecke
        bits.append(([(a, b, c) for a, b, c in px if 0 <= a < 16 and 0 <= b < 16]))
    return bits


def mud_overlay(stage, kind="mud"):
    shown = {0: 2, 1: 4, 2: 6, 3: 8}[stage]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for bit in mud_bits(kind)[:shown]:
        for x, y, c in bit:
            im.putpixel((x, y), c)
    return im


def main():
    os.makedirs(OUT, exist_ok=True)
    amphora().save(os.path.join(OUT, "amphora.png"))
    ostrakon().save(os.path.join(OUT, "ostrakon.png"))
    for kind in PALETTES:
        for s in range(4):
            mud_overlay(s, kind).save(os.path.join(OUT, f"suspicious_{kind}_{s}.png"))
    # Vorschau: pro Untergrund 4 Stufen auf einem nachgebauten (!) Hintergrund, dazu Ostrakon und Amphore.
    # Die Hintergruende sind nur grobe Naeherungen der Vanilla-Texturen, das Spiel nimmt die echten.
    rng = random.Random(1)

    def noise(c, spread):
        im = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                v = rng.choice(spread)
                im.putpixel((x, y), tuple(max(0, min(255, a + v)) for a in c) + (255,))
        return im

    bases = {"mud": noise((60, 54, 52), (-6, -2, 2, 6)), "sand": noise((218, 207, 163), (-10, -4, 0, 5)),
             "ashes": noise((154, 154, 148), (-8, -3, 2, 6))}
    S = 10
    cols = 4
    sheet = Image.new("RGBA", (cols * (16 * S + 10) + 10, 4 * (16 * S + 10) + 10), (38, 38, 42, 255))
    rows = [[Image.alpha_composite(bases[k], mud_overlay(s, k)) for s in range(4)] for k in PALETTES]
    rows.append([ostrakon(), amphora()])
    for r, row in enumerate(rows):
        for i, t in enumerate(row):
            b = t.resize((16 * S, 16 * S), Image.NEAREST)
            sheet.alpha_composite(b, (10 + i * (16 * S + 10), 10 + r * (16 * S + 10)))
    sheet.save(os.path.join(ROOT, "art/out/finds-preview.png"))
    print("ok")


if __name__ == "__main__":
    main()
