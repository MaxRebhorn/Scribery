"""Scribe-Villager: Profession-Overlay 64x64 (transparent) + Vorschau.
Aufruf: python art/gen_scribe_villager.py
Ausgabe: mod/.../textures/entity/villager/profession/scribe.png, art/out/scribe-villager-preview.png

UV-Layout nach dem Vanilla-Villagermodell (aus der Modelldefinition abgeleitet, NICHT aus einer Vanilla-Textur kopiert):
  hat    texOffs(32, 0)  box 8x10x8   (Kopf-Overlay)
  jacket texOffs( 0,38)  box 8x20x6   (Robe, deckt Koerper und Beine)
  arms   texOffs(44,22)  box 8x4x4    (verschraenkte Arme)
Boxflaechen: top (u+d,v,w,d) bottom (u+d+w,v,w,d) left (u,v+d,d,h) front (u+d,v+d,w,h) right (u+d+w,v+d,d,h) back (u+2d+w,v+d,w,h).
Farben nur aus art/palette.json. Das Layout muss im Spiel geprueft werden (nicht getestet)."""
import json, os, random
from PIL import Image, ImageDraw
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "mod/src/main/resources/assets/scribery/textures/entity/villager/profession")
raw = json.load(open(os.path.join(ROOT, "art/palette.json")))
C = {k: tuple(int(v[i:i+2], 16) for i in (1, 3, 5)) + (255,) for k, v in raw.items() if isinstance(v, str) and v.startswith("#")}
LINEN, LINEN_S, LINEN_D = C["parchment_3"], C["parchment_2"], C["parchment_1"]
APRON, APRON_S, APRON_D = C["wood_2"], C["wood_1"], C["wood_0"]
INK, INK2, RUB, FLAME = C["ink_1"], C["ink_0"], C["rubric"], C["flame_1"]
rng = random.Random(20261119)

def faces(u, v, w, h, d):
    return {"top": (u+d, v, w, d), "bottom": (u+d+w, v, w, d), "left": (u, v+d, d, h), "front": (u+d, v+d, w, h),
            "right": (u+d+w, v+d, d, h), "back": (u+2*d+w, v+d, w, h)}

im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
px = im.load()
def fill(r, col):
    x, y, w, h = r
    for j in range(h):
        for i in range(w): px[x+i, y+j] = col
def p(x, y, col):
    if 0 <= x < 64 and 0 <= y < 64: px[x, y] = col

# ---- Kopf-Overlay: Pergament-Stirnband / Kappe, Gesicht bleibt frei
H = faces(32, 0, 8, 10, 8)
fill(H["top"], LINEN_S)
for k in ("front", "back", "left", "right"):
    x, y, w, h = H[k]
    for i in range(w):
        p(x+i, y, LINEN); p(x+i, y+1, LINEN_S)      # Kappenrand 2 Zeilen
        if k in ("front",): p(x+i, y+2, LINEN_D)    # Schattenkante ueber der Stirn
    if k == "back":
        for j in range(2, 7):
            for i in range(w): p(x+i, y+j, LINEN_S if j < 4 else LINEN_D)  # Nackenschutz
x, y, w, h = H["front"]; p(x+3, y+1, RUB); p(x+4, y+1, RUB)   # rotes Band/Siegel in der Mitte
for i in range(8): p(H["top"][0]+i, H["top"][1]+rng.randrange(8), LINEN)

# ---- Robe (jacket 8x20x6): Leinen, braune Schreiberschuerze vorne, Guertel mit Beutel
J = faces(0, 38, 8, 20, 6)
for k, (x, y, w, h) in J.items():
    if k in ("top", "bottom"): fill((x, y, w, h), LINEN_D); continue
    fill((x, y, w, h), LINEN if k != "left" and k != "right" else LINEN_S)
    for j in range(h):                              # leichte Falten
        if j % 5 == 2:
            for i in range(0, w, 2): p(x+i, y+j, LINEN_S if k in ("front", "back") else LINEN_D)
x, y, w, h = J["front"]
for j in range(3, h):                               # Schuerze: Brustlatz + Rock
    for i in range(1 if j < 7 else 0, w-1 if j < 7 else w):
        p(x+i, y+j, APRON if (i+j) % 7 else APRON_S)
for j in range(h): p(x, y+j, LINEN_D) if j < 3 else None
for i in range(w): p(x+i, y+11, APRON_D); p(x+i, y+12, APRON_D)   # Guertel
p(x+3, y+11, FLAME); p(x+4, y+11, FLAME)            # Schnalle
for (i, j) in ((2, 8), (3, 8), (5, 15), (6, 15), (2, 17)):        # Tintenflecken
    p(x+i, y+j, INK); 
p(x+5, y+16, INK2); p(x+1, y+14, INK)
for j in range(14, 18):                             # Beutel am Guertel (rechts), Schreibrohr schaut heraus
    for i in range(5, 8): p(x+i, y+j, APRON_S)
p(x+6, y+13, C["lead_3"]); p(x+6, y+12, C["lead_4"])              # Rohrfeder
x, y, w, h = J["back"]
for j in range(11, 13):
    for i in range(w): p(x+i, y+j, APRON_D)
for i in range(2, 6):                               # Papyrusrolle quer auf dem Ruecken
    p(x+i, y+4, C["parchment_2"]); p(x+i, y+5, C["parchment_1"])
p(x+2, y+4, RUB); p(x+5, y+4, RUB)
for k in ("left", "right"):
    x, y, w, h = J[k]
    for j in range(11, 13):
        for i in range(w): p(x+i, y+j, APRON_D)

# ---- verschraenkte Arme: weite Leinenaermel mit Tintenmanschette
A = faces(44, 22, 8, 4, 4)
for k, (x, y, w, h) in A.items():
    if x >= 64: continue
    for j in range(h):
        for i in range(w):
            if x+i < 64: p(x+i, y+j, LINEN if j < h-1 else LINEN_S)
x, y, w, h = A["front"]
for i in range(0, w, 3): p(x+i, y+h-1, INK) if x+i < 64 else None
p(x+1, y+1, INK)

os.makedirs(OUT, exist_ok=True)
im.save(os.path.join(OUT, "scribe.png"))

# ---- Vorschau: Overlay-Sheet + Vorderansicht ueber gedachter Villagerhaut
S = 8
sheet = Image.new("RGBA", (64*S, 64*S), (60, 60, 66, 255))
bg = Image.new("RGBA", (64, 64), (60, 60, 66, 255))
sheet = Image.alpha_composite(bg, im).resize((64*S, 64*S), Image.NEAREST)
SK, VB = (186, 140, 108, 255), (96, 66, 44, 255)
front = Image.new("RGBA", (16, 36), (198, 198, 198, 255))
d = ImageDraw.Draw(front)
d.rectangle((4, 0, 11, 9), fill=SK); d.rectangle((4, 10, 11, 29), fill=VB); d.rectangle((2, 15, 13, 18), fill=VB)
def blit(rect, dx, dy):
    x, y, w, h = rect; front.alpha_composite(im.crop((x, y, x+w, y+h)), (dx, dy))
blit(H["front"], 4, 0); blit(J["front"], 4, 10); blit(A["front"], 4, 14)
fv = front.resize((16*16, 36*16), Image.NEAREST)
out = Image.new("RGBA", (64*S + 16*16 + 24, 64*S), (198, 198, 198, 255))
out.alpha_composite(sheet, (0, 0)); out.alpha_composite(fv, (64*S + 12, 12))
os.makedirs(os.path.join(ROOT, "art/out"), exist_ok=True)
out.convert("RGB").save(os.path.join(ROOT, "art/out/scribe-villager-preview.png"))
