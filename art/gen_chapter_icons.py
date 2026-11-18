"""Erzeugt die 21 Kapitel-Icons fuer die Creative-Tabs (16x16, Low-Poly-Pixel-Art, transparent).

Aufruf: python art/gen_chapter_icons.py
Ausgabe: mod/.../textures/item/chapter_icon_<n>.png, models/item/chapter_icon_<n>.json und art/out/chapter-icons-preview.png

Stil wie die Fragment-Items: flache Flaechen in zwei bis drei Toenen, Licht von links oben, dunkle Kontur aussen.
Motive (Kapitel): 1 Lampe, 2 Weinkrug, 3 Mond, 4 Eimer am Brunnen, 5 Teich, 6 Brot, 7 Wassertropfen, 8 Sonne, 9 Auge,
10 Hirtenstab, 11 Leinenbinde, 12 Palmwedel, 13 Waschschuessel, 14 Tuer, 15 Trauben, 16 Taube, 17 gefaltete Haende,
18 Kette, 19 drei Kreuze auf einem Huegel, 20 leeres Grab, 21 Fisch.
"""
import json
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "mod/src/main/resources/assets/scribery/textures/item")
MODELS = os.path.join(ROOT, "mod/src/main/resources/assets/scribery/models/item")
OUT = os.path.join(ROOT, "art/out")

OUTLINE = (58, 40, 32, 255)
C = {k: v + (255,) for k, v in dict(
    light=(240, 226, 186), tan=(208, 174, 124), brown=(146, 100, 66), dark=(96, 64, 46),
    clay=(198, 130, 88), clay_d=(150, 92, 62), clay_l=(226, 164, 112),
    water=(92, 150, 206), water_l=(156, 202, 236), water_d=(56, 98, 160),
    gold=(228, 182, 72), gold_d=(172, 126, 44), gold_l=(248, 222, 130),
    gray=(150, 150, 156), gray_l=(198, 198, 204), gray_d=(100, 100, 108),
    green=(92, 144, 72), green_d=(58, 98, 52), green_l=(142, 188, 100),
    purple=(116, 62, 124), purple_d=(74, 38, 86), purple_l=(160, 100, 164),
    white=(244, 244, 238), red=(172, 66, 58), black=(40, 36, 42),
    flame=(244, 168, 60), flame_l=(252, 226, 124), fish=(128, 156, 196), fish_d=(88, 114, 158), fish_l=(176, 200, 226),
).items()}


class Canvas:
    def __init__(self):
        self.im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.im)

    def poly(self, pts, col):
        self.d.polygon(pts, fill=C[col])

    def rect(self, x0, y0, x1, y1, col):  # inklusive Grenzen
        self.d.rectangle([x0, y0, x1, y1], fill=C[col])

    def px(self, x, y, col):
        if 0 <= x < 16 and 0 <= y < 16:
            self.im.putpixel((x, y), C[col])

    def ell(self, x0, y0, x1, y1, col):
        self.d.ellipse([x0, y0, x1, y1], fill=C[col])

    def line(self, pts, col, w=1):
        self.d.line(pts, fill=C[col], width=w)

    def clear_ell(self, x0, y0, x1, y1):
        self.d.ellipse([x0, y0, x1, y1], fill=(0, 0, 0, 0))

    def finish(self):
        """Dunkle Kontur um alle Flaechen (4er-Nachbarschaft)."""
        src = self.im.copy()
        for y in range(16):
            for x in range(16):
                if src.getpixel((x, y))[3] == 0:
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        nx, ny = x + dx, y + dy
                        if 0 <= nx < 16 and 0 <= ny < 16 and src.getpixel((nx, ny))[3] > 0:
                            self.im.putpixel((x, y), OUTLINE)
                            break
        return self.im


def icon1():  # Oellampe
    c = Canvas()
    c.poly([(4, 9), (6, 7), (12, 7), (14, 9), (13, 12), (6, 12)], "clay")
    c.poly([(10, 7), (12, 7), (14, 9), (13, 12), (10, 12)], "clay_d")
    c.poly([(2, 8), (4, 8), (6, 9), (5, 10), (3, 10)], "clay")
    c.rect(6, 7, 9, 7, "clay_l")
    c.px(14, 10, "clay_d")
    c.poly([(2, 3), (3, 5), (3, 7), (1, 6)], "flame")
    c.px(2, 5, "flame_l")
    c.px(2, 6, "flame_l")
    return c


def icon2():  # Weinkrug
    c = Canvas()
    c.poly([(5, 5), (11, 5), (12, 8), (11, 13), (5, 13), (4, 8)], "clay")
    c.poly([(9, 5), (11, 5), (12, 8), (11, 13), (9, 13)], "clay_d")
    c.rect(6, 2, 10, 5, "clay")
    c.rect(9, 2, 10, 5, "clay_d")
    c.rect(5, 2, 11, 2, "clay_l")
    c.rect(4, 8, 12, 9, "red")
    c.line([(12, 5), (14, 6), (14, 9), (12, 11)], "clay_d")
    return c


def icon3():  # Mond mit Stern
    c = Canvas()
    c.ell(2, 2, 12, 13, "gold")
    c.clear_ell(6, 0, 15, 11)
    c.px(4, 6, "gold_l")
    c.px(4, 7, "gold_l")
    c.px(5, 4, "gold_l")
    c.px(3, 9, "gold_d")
    c.px(4, 11, "gold_d")
    c.px(12, 3, "white")
    c.px(11, 4, "white")
    c.px(13, 4, "white")
    c.px(12, 5, "white")
    return c


def icon4():  # Eimer
    c = Canvas()
    c.line([(3, 6), (4, 2), (12, 2), (13, 6)], "gray_d")
    c.poly([(3, 6), (13, 6), (12, 14), (4, 14)], "brown")
    c.poly([(9, 6), (13, 6), (12, 14), (9, 14)], "dark")
    c.rect(3, 6, 12, 7, "water")
    c.rect(4, 6, 7, 6, "water_l")
    c.rect(3, 9, 12, 9, "gray_d")
    c.rect(4, 12, 11, 12, "gray_d")
    return c


def icon5():  # Teich mit Funkeln
    c = Canvas()
    c.poly([(1, 8), (15, 8), (15, 14), (1, 14)], "gray_l")
    c.rect(2, 9, 13, 12, "water")
    c.rect(3, 10, 6, 10, "water_l")
    c.rect(9, 12, 12, 12, "water_d")
    c.rect(2, 13, 14, 13, "gray")
    c.line([(8, 1), (8, 6)], "gold")
    c.line([(6, 3), (10, 3)], "gold")
    c.px(8, 3, "gold_l")
    return c


def icon6():  # Brotlaib
    c = Canvas()
    c.poly([(1, 10), (3, 6), (13, 6), (15, 10), (14, 13), (2, 13)], "tan")
    c.poly([(2, 12), (14, 12), (14, 13), (2, 13)], "brown")
    c.poly([(10, 6), (13, 6), (15, 10), (14, 13), (11, 13)], "clay_d")
    c.rect(4, 6, 8, 7, "light")
    c.line([(5, 8), (6, 11)], "brown")
    c.line([(8, 8), (9, 11)], "brown")
    c.line([(11, 8), (12, 11)], "brown")
    return c


def icon7():  # Wassertropfen
    c = Canvas()
    c.poly([(8, 1), (12, 7), (13, 10), (11, 13), (5, 13), (3, 10), (4, 7)], "water")
    c.poly([(8, 1), (12, 7), (13, 10), (11, 13), (9, 13), (9, 7)], "water_d")
    c.poly([(5, 8), (6, 6), (7, 8), (7, 11), (5, 11)], "water_l")
    return c


def icon8():  # Sonne
    c = Canvas()
    c.poly([(5, 4), (11, 4), (13, 6), (13, 10), (11, 12), (5, 12), (3, 10), (3, 6)], "gold")
    c.poly([(9, 4), (11, 4), (13, 6), (13, 10), (11, 12), (9, 12)], "gold_d")
    c.rect(5, 5, 7, 6, "gold_l")
    for (x0, y0, x1, y1) in ((7, 0, 8, 1), (7, 14, 8, 15), (0, 7, 1, 8), (14, 7, 15, 8)):
        c.rect(x0, y0, x1, y1, "gold")
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        c.px(x, y, "gold_l")
    return c


def icon9():  # Auge
    c = Canvas()
    c.poly([(0, 8), (4, 4), (11, 4), (15, 8), (11, 12), (4, 12)], "white")
    c.poly([(0, 8), (4, 4), (11, 4), (15, 8), (14, 8), (11, 5), (4, 5), (1, 8)], "gray_l")
    c.ell(5, 5, 10, 10, "water")
    c.ell(6, 6, 9, 9, "water_d")
    c.rect(7, 7, 8, 8, "black")
    c.px(6, 6, "white")
    return c


def icon10():  # Hirtenstab
    c = Canvas()
    c.line([(10, 15), (10, 5)], "brown", 2)
    c.line([(10, 5), (9, 2), (6, 1), (4, 3), (4, 6)], "brown", 2)
    c.line([(11, 14), (11, 6)], "dark")
    c.px(5, 2, "tan")
    c.px(9, 8, "tan")
    c.px(9, 12, "tan")
    return c


def icon11():  # Leinenbinde
    c = Canvas()
    c.poly([(2, 4), (12, 4), (14, 6), (14, 11), (12, 13), (2, 13)], "white")
    c.poly([(10, 4), (12, 4), (14, 6), (14, 11), (12, 13), (10, 13)], "gray_l")
    c.ell(0, 4, 4, 13, "light")
    c.rect(5, 4, 5, 13, "tan")
    c.rect(8, 4, 8, 13, "tan")
    c.poly([(12, 12), (15, 14), (12, 15), (10, 13)], "white")
    return c


def icon12():  # Palmwedel
    c = Canvas()
    c.line([(3, 14), (8, 8)], "brown", 2)
    c.poly([(8, 8), (2, 6), (1, 3), (7, 5)], "green")
    c.poly([(8, 8), (8, 1), (10, 1), (10, 7)], "green_l")
    c.poly([(8, 8), (14, 3), (15, 6), (10, 9)], "green_d")
    c.poly([(8, 8), (13, 9), (14, 12), (9, 10)], "green")
    c.px(8, 3, "green_d")
    return c


def icon13():  # Waschschuessel
    c = Canvas()
    c.poly([(1, 7), (15, 7), (13, 13), (3, 13)], "gray_l")
    c.poly([(9, 7), (15, 7), (13, 13), (9, 13)], "gray")
    c.rect(2, 7, 14, 8, "water")
    c.rect(3, 7, 6, 7, "water_l")
    c.rect(1, 6, 15, 6, "gray_d")
    c.px(7, 3, "water")
    c.px(7, 4, "water_l")
    c.px(10, 2, "water")
    c.px(10, 3, "water_l")
    return c


def icon14():  # Tuer
    c = Canvas()
    c.rect(4, 2, 11, 14, "brown")
    c.rect(8, 2, 11, 14, "dark")
    c.rect(5, 4, 7, 7, "tan")
    c.rect(5, 9, 7, 12, "tan")
    c.rect(9, 4, 10, 7, "brown")
    c.rect(9, 9, 10, 12, "brown")
    c.px(7, 8, "gold")
    c.rect(4, 2, 11, 2, "tan")
    return c


def icon15():  # Trauben
    c = Canvas()
    c.line([(8, 1), (8, 4)], "brown")
    c.poly([(9, 2), (13, 2), (14, 5), (10, 5)], "green")
    for (x, y) in ((4, 5), (7, 5), (10, 5), (5, 8), (8, 8), (6, 11)):
        c.rect(x, y, x + 2, y + 2, "purple")
        c.px(x, y, "purple_l")
        c.px(x + 2, y + 2, "purple_d")
    c.rect(8, 12, 9, 13, "purple")
    c.px(8, 12, "purple_l")
    return c


def icon16():  # Taube
    c = Canvas()
    c.poly([(2, 8), (5, 6), (11, 6), (13, 8), (10, 12), (5, 12)], "white")
    c.poly([(8, 12), (13, 8), (10, 12)], "gray_l")
    c.poly([(5, 8), (8, 2), (11, 2), (9, 8)], "gray_l")
    c.poly([(8, 2), (11, 2), (10, 5), (9, 8)], "white")
    c.poly([(2, 8), (0, 11), (4, 11)], "gray_l")
    c.rect(11, 4, 13, 6, "white")
    c.px(14, 5, "gold")
    c.px(12, 5, "black")
    return c


def icon17():  # Gefaltete Haende
    c = Canvas()
    c.poly([(8, 1), (12, 6), (12, 12), (4, 12), (4, 6)], "tan")
    c.poly([(8, 1), (12, 6), (12, 12), (8, 12)], "clay")
    c.line([(8, 3), (8, 11)], "brown")
    c.rect(3, 12, 12, 14, "light")
    c.rect(8, 12, 12, 14, "tan")
    return c


def icon18():  # Kette
    c = Canvas()
    for (x0, y0, x1, y1) in ((1, 2, 9, 8), (6, 7, 14, 13)):
        c.rect(x0, y0 + 1, x1, y1 - 1, "gray_l")
        c.rect(x0 + 1, y0, x1 - 1, y1, "gray_l")
        c.rect(x0 + 2, y0 + 2, x1 - 2, y1 - 2, "black")
        c.rect(x0 + 3, y0 + 3, x1 - 3, y1 - 3, "black")
        c.rect(x1 - 1, y0 + 1, x1, y1 - 1, "gray")
        c.rect(x0 + 1, y1 - 1, x1 - 1, y1, "gray")
    c.px(2, 3, "white")
    return c


def icon19():  # Drei Kreuze auf einem Huegel (das mittlere hoeher)
    c = Canvas()
    c.poly([(0, 15), (0, 12), (4, 10), (12, 10), (15, 12), (15, 15)], "green")
    c.poly([(9, 10), (12, 10), (15, 12), (15, 15), (9, 15)], "green_d")
    c.rect(0, 12, 4, 13, "green_l")
    # mittleres Kreuz
    c.rect(7, 1, 8, 10, "brown")
    c.rect(5, 3, 10, 4, "brown")
    c.rect(7, 1, 7, 9, "tan")
    c.rect(5, 3, 10, 3, "tan")
    c.rect(8, 4, 10, 4, "dark")
    # linkes und rechtes Kreuz, kleiner
    for x in (2, 13):
        c.rect(x, 5, x, 10, "dark")
        c.rect(x - 1, 6, x + 1, 6, "dark")
    c.px(2, 5, "brown")
    c.px(13, 5, "brown")
    return c


def icon20():  # Leeres Grab
    c = Canvas()
    c.poly([(1, 14), (1, 8), (5, 3), (11, 3), (15, 8), (15, 14)], "gray")
    c.poly([(9, 3), (11, 3), (15, 8), (15, 14), (9, 14)], "gray_d")
    c.poly([(6, 14), (6, 9), (8, 7), (10, 9), (10, 14)], "black")
    c.ell(0, 9, 5, 14, "gray_l")
    c.px(2, 10, "white")
    return c


def icon21():  # Fisch
    c = Canvas()
    c.poly([(1, 8), (5, 5), (10, 5), (13, 8), (10, 11), (5, 11)], "fish")
    c.poly([(5, 8), (10, 8), (13, 8), (10, 11), (5, 11)], "fish_d")
    c.poly([(5, 5), (10, 5), (11, 6), (4, 6)], "fish_l")
    c.poly([(12, 8), (15, 4), (15, 12)], "fish_d")
    c.px(3, 7, "black")
    c.px(7, 7, "fish_l")
    c.px(8, 9, "fish_l")
    return c


ICONS = [icon1, icon2, icon3, icon4, icon5, icon6, icon7, icon8, icon9, icon10, icon11,
         icon12, icon13, icon14, icon15, icon16, icon17, icon18, icon19, icon20, icon21]


def main():
    os.makedirs(TEX, exist_ok=True)
    os.makedirs(MODELS, exist_ok=True)
    os.makedirs(OUT, exist_ok=True)
    S = 8
    sheet = Image.new("RGBA", (7 * (16 * S + 8) + 8, 3 * (16 * S + 8) + 8), (58, 54, 64, 255))
    for n, fn in enumerate(ICONS, start=1):
        im = fn().finish()
        im.save(os.path.join(TEX, f"chapter_icon_{n}.png"))
        with open(os.path.join(MODELS, f"chapter_icon_{n}.json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"scribery:item/chapter_icon_{n}"}}, f, indent=2)
            f.write("\n")
        r, col = divmod(n - 1, 7)
        sheet.alpha_composite(im.resize((16 * S, 16 * S), Image.NEAREST), (8 + col * (16 * S + 8), 8 + r * (16 * S + 8)))
    sheet.save(os.path.join(OUT, "chapter-icons-preview.png"))
    print("ok", len(ICONS))


if __name__ == "__main__":
    main()
