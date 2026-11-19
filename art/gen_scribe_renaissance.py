"""Scribe-Villager Variante D: Renaissance-Gelehrter. Aufruf: python art/gen_scribe_renaissance.py
Ausgabe: art/out/scribe-d.png (Overlay 64x64), art/out/scribe-d-preview.png. Layout/Palette wie gen_scribe_variants.py."""
import os
from PIL import Image
import gen_scribe_variants as V
from gen_scribe_variants import C, H, J, A, T, view, ROOT

def renaissance(flat_beret=True):
    t = T(); gown, gown_s, gown_d = C["ink_1"], C["lead_1"], C["ink_0"]
    red, red_d = C["rubric"], (110, 30, 26, 255)
    ruff, ruff_s = C["parchment_3"], C["parchment_2"]; gold = C["flame_1"]
    # Barett: flache dunkle Kappe mit rotem Rand, Feder seitlich (rechts im Bild = Modell links)
    if flat_beret:
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
    else:  # 3D-Barett wird als eigenes Modell gerendert (ScribeBeretLayer); hier nur das Haar im Nacken
        x, y, w, h = H["back"]
        for j in range(0, 6):
            for i in range(w): t.p(x+i, y+j, C["wood_1"])
        for k in ("left", "right"):
            x, y, w, h = H[k]
            for j in range(0, 3):
                for i in range(w - 3, w) if k == "left" else range(0, 3): t.p(x+i, y+j, C["wood_1"])
        t.fill(H["top"], C["wood_1"])
    # Robe im Vanilla-Stil: eine ruhige Grundfarbe, hellere Schuerze vorn, dunkler Guertel, Saum, kleine Tintenflecken
    robe, robe_s, robe_d = C["wood_2"], C["wood_1"], C["wood_0"]
    apron, apron_s, apron_l = C["parchment_2"], C["parchment_1"], C["parchment_3"]
    for k, r in J.items(): t.fill(r, robe, robe_s if k in ("left", "right") else None)
    x, y, w, h = J["front"]
    for i in range(w): t.p(x+i, y, apron_l)                                              # Kragen
    for j in range(1, h - 2):                                                            # Schuerze
        for i in range(1, w - 1): t.p(x+i, y+j, apron if (i + j) % 7 else apron_s)
    for j in range(1, h - 2): t.p(x+1, y+j, apron_s); t.p(x+w-2, y+j, apron_s)          # Schuerzenrand
    for i in range(w): t.p(x+i, y+11, robe_d)                                            # Guertel
    t.p(x+3, y+11, gold); t.p(x+4, y+11, gold)
    for j in range(h-2, h):
        for i in range(w): t.p(x+i, y+j, robe_s)                                         # Saum
    t.p(x+5, y+5, C["ink_0"]); t.p(x+2, y+8, C["ink_0"]); t.p(x+5, y+14, C["ink_1"])     # Tintenflecken
    for j in range(12, 16): t.p(x+6, y+j, ruff)                                          # Feder am Guertel
    t.p(x+6, y+16, C["lead_3"])
    x, y, w, h = J["back"]
    for i in range(w): t.p(x+i, y, apron_l)
    for i in range(w): t.p(x+i, y+11, robe_d)
    for j in range(h-2, h):
        for i in range(w): t.p(x+i, y+j, robe_s)
    for k in ("left", "right"):
        x, y, w, h = J[k]
        for i in range(w): t.p(x+i, y+11, robe_d)
        for j in range(h-2, h):
            for i in range(w): t.p(x+i, y+j, robe_d)
    # Aermel: Grundfarbe der Robe, helle Manschette
    for k, r in A.items(): t.fill(r, robe, robe_s)
    x, y, w, h = A["front"]
    for i in range(w): t.p(x+i, y+h-1, apron_l)
    return t.im

def beret_texture():
    """64x32 fuer das 3D-Barett (ScribeBeretLayer): Scheibe 11x2x11 bei (0,0), Feder 1x4x4 bei (44,0)."""
    t = T(); t.im = Image.new("RGBA", (64, 32), (0, 0, 0, 0)); t.px = t.im.load()
    gown, gown_s = C["ink_1"], C["lead_1"]; red, red_d = C["rubric"], (110, 30, 26, 255)
    def faces(u, v, w, h, d):
        return {"top": (u+d, v, w, d), "bottom": (u+d+w, v, w, d), "left": (u, v+d, d, h), "front": (u+d, v+d, w, h),
                "right": (u+d+w, v+d, d, h), "back": (u+2*d+w, v+d, w, h)}
    F = faces(0, 0, 11, 2, 11)
    t.fill(F["top"], gown, gown_s); t.fill(F["bottom"], gown_s)
    for k in ("left", "front", "right", "back"):
        x, y, w, h = F[k]; t.fill((x, y, w, 1), red); t.fill((x, y+1, w, 1), red_d)
    x, y, w, h = F["top"]
    for i in range(2, 9): t.p(x+i, y+5, red_d)                      # Naht quer ueber der Scheibe
    G = faces(44, 0, 1, 4, 4)
    for r in G.values(): t.fill(r, C["parchment_3"], C["parchment_2"])
    x, y, w, h = G["front"]; [t.p(x, y+j, C["parchment_1"]) for j in range(4)]
    return t.im

if __name__ == "__main__":
    beret_texture().save(os.path.join(ROOT, "mod/src/main/resources/assets/scribery/textures/entity/scribe_beret.png"))
    im = renaissance(flat_beret=False)
    im.save(os.path.join(ROOT, "mod/src/main/resources/assets/scribery/textures/entity/villager/profession/scribe.png"))
    im.save(os.path.join(ROOT, "art/out/scribe-d.png"))
    out = Image.new("RGBA", (2*160+20, 36*10+20), (198, 198, 198, 255))
    out.alpha_composite(view(im, "front"), (10, 10)); out.alpha_composite(view(im, "back"), (170, 10))
    out.convert("RGB").save(os.path.join(ROOT, "art/out/scribe-d-preview.png"))
