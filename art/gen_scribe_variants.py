"""Zwei alternative Scribe-Villager-Designs (nur Vorschau, nichts im Mod). Layout wie gen_scribe_villager.py."""
import json, os, random
from PIL import Image, ImageDraw
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
raw = json.load(open(os.path.join(ROOT, "art/palette.json")))
C = {k: tuple(int(v[i:i+2], 16) for i in (1, 3, 5)) + (255,) for k, v in raw.items() if isinstance(v, str) and v.startswith("#")}
def faces(u, v, w, h, d):
    return {"top": (u+d, v, w, d), "bottom": (u+d+w, v, w, d), "left": (u, v+d, d, h), "front": (u+d, v+d, w, h),
            "right": (u+d+w, v+d, d, h), "back": (u+2*d+w, v+d, w, h)}
H, J, A = faces(32, 0, 8, 10, 8), faces(0, 38, 8, 20, 6), faces(44, 22, 8, 4, 4)

class T:
    def __init__(s): s.im = Image.new("RGBA", (64, 64), (0, 0, 0, 0)); s.px = s.im.load()
    def p(s, x, y, c):
        if 0 <= x < 64 and 0 <= y < 64 and c: s.px[x, y] = c
    def fill(s, r, c, dither=None):
        x, y, w, h = r
        for j in range(h):
            for i in range(w): s.p(x+i, y+j, dither if dither and (i+j) % 4 == 0 else c)

def monk():
    t = T(); g1, g2, g3 = C["wood_1"], C["wood_2"], C["wood_0"]; rope = C["parchment_1"]
    # Kapuze: Kopf-Overlay, Gesichtsfenster vorne
    for k, r in H.items():
        t.fill(r, g2, g1)
    x, y, w, h = H["front"]
    for j in range(2, 9):
        for i in range(1, 7): t.p(x+i, y+j, None)
    for j in range(2, 9):
        for i in range(1, 7): t.px[x+i, y+j] = (0, 0, 0, 0)
    for i in range(8): t.p(x+i, y, g3); t.p(x+i, y+1, g1)
    t.fill(H["bottom"], g1)
    # Kutte
    for k, r in J.items(): t.fill(r, g2, g1)
    x, y, w, h = J["front"]
    for j in range(h): t.p(x+3, y+j, g3) if j < 2 else None
    for j in range(2, 14): t.p(x+3, y+j, g1); t.p(x+4, y+j, g1)       # Skapulier-Streifen
    for j in range(h): t.p(x, y+j, g1); t.p(x+7, y+j, g1)
    for i in range(w): t.p(x+i, y+11, rope); t.p(x+i, y+12, C["parchment_0"])  # Strick
    for j in range(13, 19): t.p(x+3, y+j, rope); t.p(x+4, y+j, rope) if j < 17 else None  # Strickende
    t.p(x+3, y+19, C["parchment_0"]); t.p(x+4, y+19, rope)
    x, y, w, h = J["back"]
    for i in range(w): t.p(x+i, y+11, rope)
    for j in range(2, 4):
        for i in range(w): t.p(x+i, y+j, g1)                            # Kapuzenzipfel
    for k, r in A.items(): t.fill(r, g2, g1)
    x, y, w, h = A["front"]
    for i in range(w): t.p(x+i, y+h-1, g1)
    return t.im

def greek():
    t = T(); white, w2, w3 = C["parchment_3"], C["parchment_2"], C["parchment_1"]
    gold, red = C["flame_1"], C["rubric"]
    # Lorbeerreif (gold) + Haarknoten
    t.fill(H["top"], C["wood_1"])
    for k in ("front", "back", "left", "right"):
        x, y, w, h = H[k]
        for i in range(w): t.p(x+i, y+1, gold if i % 2 == 0 else C["flame_2"])
        if k == "back":
            for j in range(2, 5):
                for i in range(w): t.p(x+i, y+j, C["wood_1"])
        if k in ("left", "right"):
            for j in range(2, 4):
                for i in range(0, 3): t.p(x+i, y+j, C["wood_1"])
    # Chiton mit roter Saeumung und Chlamys (Umhang) ueber einer Schulter
    for k, r in J.items(): t.fill(r, white, w2)
    x, y, w, h = J["front"]
    for j in range(h): t.p(x+1, y+j, red); t.p(x+6, y+j, red) if j > 2 else None   # Clavus-Streifen
    for i in range(w): t.p(x+i, y+h-1, red); t.p(x+i, y+h-2, red)                   # Saum
    for i in range(w): t.p(x+i, y+11, C["wood_2"])                                  # Guertel
    t.p(x+4, y+11, gold); t.p(x+3, y+2, C["lead_3"]); t.p(x+4, y+2, C["lead_4"])    # Fibel (Brosche)
    for (i, j) in ((5, 14), (2, 16)): t.p(x+i, y+j, C["ink_1"])
    # Umhang ueber linke Schulter: Rotbraun
    for j in range(0, 9):
        for i in range(max(0, 4-j//2 if j < 4 else 0), 3): t.p(x+i, y+j, C["rubric"] if (i+j) % 3 else C["wood_1"])
    x, y, w, h = J["back"]
    for j in range(2, 14):
        for i in range(w): t.p(x+i, y+j, C["rubric"] if (i+j) % 4 else C["wood_1"])
    for k in ("left", "right"):
        x, y, w, h = J[k]
        for j in range(2, 12): t.p(x+1, y+j, C["rubric"]); t.p(x+2, y+j, C["wood_1"])
    for k, r in A.items(): t.fill(r, white, w2)
    x, y, w, h = A["front"]
    for i in range(0, w, 2): t.p(x+i, y+h-1, C["ink_1"])
    return t.im

def current():
    return Image.open(os.path.join(ROOT, "mod/src/main/resources/assets/scribery/textures/entity/villager/profession/scribe.png")).convert("RGBA")

SK, VB = (186, 140, 108, 255), (96, 66, 44, 255)
def view(im, side):
    f = Image.new("RGBA", (16, 36), (198, 198, 198, 255)); d = ImageDraw.Draw(f)
    d.rectangle((4, 0, 11, 9), fill=SK if side == "front" else (70, 50, 36, 255)); d.rectangle((4, 10, 11, 29), fill=VB); d.rectangle((2, 15, 13, 18), fill=VB)
    b = lambda r, dx, dy: f.alpha_composite(im.crop((r[0], r[1], r[0]+r[2], r[1]+r[3])), (dx, dy))
    b(H[side], 4, 0); b(J[side], 4, 10)
    if side == "front": b(A["front"], 4, 14)
    return f.resize((16*10, 36*10), Image.NEAREST)

if __name__ == '__main__':
    designs = [("A: Schreiber (aktuell)", current()), ("B: Kopist-Moench", monk()), ("C: Griechischer Gelehrter", greek())]
    out = Image.new("RGBA", (3*(2*160+30), 36*10+50), (198, 198, 198, 255)); d = ImageDraw.Draw(out)
    for n, (name, im) in enumerate(designs):
        x0 = n*(2*160+30) + 10
        d.text((x0, 8), name, fill=(20, 20, 20))
        out.alpha_composite(view(im, "front"), (x0, 30)); out.alpha_composite(view(im, "back"), (x0+160, 30))
        im.save(os.path.join(ROOT, f"art/out/scribe-{'abc'[n]}.png"))
    out.convert("RGB").save(os.path.join(ROOT, "art/out/scribe-variants-preview.png"))
