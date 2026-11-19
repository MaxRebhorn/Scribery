"""Scribe-Villager Variante D: Renaissance-Gelehrter. Aufruf: python art/gen_scribe_renaissance.py
Ausgabe: art/out/scribe-d.png (Overlay 64x64), art/out/scribe-d-preview.png. Layout/Palette wie gen_scribe_variants.py."""
import os
from PIL import Image
import gen_scribe_variants as V
from gen_scribe_variants import C, H, J, A, T, view, ROOT

def renaissance():
    t = T(); gown, gown_s, gown_d = C["ink_1"], C["lead_1"], C["ink_0"]
    red, red_d = C["rubric"], (110, 30, 26, 255)
    ruff, ruff_s = C["parchment_3"], C["parchment_2"]; gold = C["flame_1"]
    # Barett: flache dunkle Kappe mit rotem Rand, Feder seitlich (rechts im Bild = Modell links)
    t.fill(H["top"], gown)
    for k in ("front", "back", "left", "right"):
        x, y, w, h = H[k]
        for i in range(w):
            t.p(x+i, y, gown); t.p(x+i, y+1, gown_s); t.p(x+i, y+2, red if k != "back" else gown_s)
        if k == "back":
            for j in range(3, 6):
                for i in range(w): t.p(x+i, y+j, C["wood_1"])           # Haar im Nacken
    x, y, w, h = H["left"]; t.p(x+1, y+3, ruff); t.p(x+2, y+4, ruff); t.p(x+3, y+4, ruff_s); t.p(x+4, y+3, ruff)  # Feder
    for i in range(2, 6): t.p(H["top"][0]+i, H["top"][1]+1, gown_s)
    # Talar/Gelehrtenrobe: dunkel, Knopfleiste, Pelzkragen unten, Krause am Hals
    for k, r in J.items(): t.fill(r, gown, gown_s if k in ("left", "right") else None)
    x, y, w, h = J["front"]
    for i in range(w): t.p(x+i, y, ruff); t.p(x+i, y+1, ruff_s if i % 2 else ruff)      # Halskrause
    for j in range(2, h):                                                                # Roter Untergrund im Ausschnitt + Knopfleiste
        t.p(x+3, y+j, gown_d); t.p(x+4, y+j, gown_d)
    for j in range(2, 18, 3): t.p(x+3, y+j, gold)                                       # Knoepfe
    for j in range(2, 8): t.p(x+2, y+j, red_d); t.p(x+5, y+j, red_d)                    # Wams-Rand
    for i in range(w): t.p(x+i, y+11, gown_d)                                            # Guertel
    t.p(x+4, y+11, gold)
    for j in range(h-3, h):
        for i in range(w): t.p(x+i, y+j, ruff_s if (i+j) % 3 else C["parchment_1"])      # Pelzsaum
    t.p(x+6, y+7, C["ink_0"]); t.p(x+1, y+14, C["ink_0"])                                # Tintenspritzer
    # Schreibzeug am Guertel: Feder und Tintenhorn
    for j in range(12, 16): t.p(x+6, y+j, ruff)
    t.p(x+6, y+16, C["lead_3"]); t.p(x+1, y+13, C["parchment_1"]); t.p(x+1, y+14, C["parchment_1"])
    x, y, w, h = J["back"]
    for i in range(w): t.p(x+i, y, ruff); t.p(x+i, y+1, ruff_s)
    for j in range(2, 18): t.p(x+3, y+j, gown_d); t.p(x+4, y+j, gown_d)                  # Rueckennaht
    for i in range(w): t.p(x+i, y+11, gown_d)
    for j in range(h-3, h):
        for i in range(w): t.p(x+i, y+j, ruff_s if (i+j) % 3 else C["parchment_1"])
    for k in ("left", "right"):
        x, y, w, h = J[k]
        for j in range(h-3, h):
            for i in range(w): t.p(x+i, y+j, ruff_s if (i+j) % 3 else C["parchment_1"])
        for i in range(w): t.p(x+i, y+11, gown_d)
    # Aermel: dunkel mit rot geschlitzten Puffaermeln und Spitzenmanschette
    for k, r in A.items(): t.fill(r, gown, gown_s)
    x, y, w, h = A["front"]
    for i in range(0, w, 2):
        t.p(x+i, y, red); t.p(x+i, y+1, red_d)
    for i in range(w): t.p(x+i, y+h-1, ruff)
    return t.im

if __name__ == "__main__":
    im = renaissance(); im.save(os.path.join(ROOT, "art/out/scribe-d.png"))
    out = Image.new("RGBA", (2*160+20, 36*10+20), (198, 198, 198, 255))
    out.alpha_composite(view(im, "front"), (10, 10)); out.alpha_composite(view(im, "back"), (170, 10))
    out.convert("RGB").save(os.path.join(ROOT, "art/out/scribe-d-preview.png"))
