"""Zeichnet die UI-Raster aus layout.json und tiles-layout.json als PNG (Kontrollbilder, keine Texturen).

Aufruf: python art/mockup.py
Prueft dabei, dass kein Rechteck die Leinwand verlaesst und sich keine zwei ueberlappen
(ausser ausdruecklich erlaubte Paare: Inhalt liegt auf seinem Traeger).
"""
import json
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent
SCALE = 4
ALLOWED_OVERLAP = {("lectern", "fragment"), ("barrel", "roller_rest")}
LAYOUTS = [("layout.json", "press-mockup.png"), ("tiles-layout.json", "tiles-mockup.png")]


def load(name):
    return json.loads((HERE / name).read_text(encoding="utf-8"))


def overlaps(a, b):
    ax, ay, aw, ah = a
    bx, by, bw, bh = b
    return ax < bx + bw and bx < ax + aw and ay < by + bh and by < ay + ah


def check(layout):
    errors = []
    cw, ch = layout["canvas"]["w"], layout["canvas"]["h"]
    rects = {k: v["rect"] for k, v in layout["rects"].items()}
    for name, (x, y, w, h) in rects.items():
        if x < 0 or y < 0 or x + w > cw or y + h > ch:
            errors.append(f"{name} verlaesst die Leinwand")
    names = list(rects)
    for i, a in enumerate(names):
        for b in names[i + 1:]:
            if (a, b) in ALLOWED_OVERLAP or (b, a) in ALLOWED_OVERLAP:
                continue
            if overlaps(rects[a], rects[b]):
                errors.append(f"{a} ueberlappt {b}")

    if "forme" in rects:
        # Setzform: alle Slots muessen in den Rahmen passen
        s, (ox, oy) = layout["sort"], layout["forme_slots_origin"]
        fx, fy, fw, fh = rects["forme"]
        right = ox + (layout["line_slots"] - 1) * s["pitch"] + s["w"]
        bottom = oy + (layout["forme_rows"] - 1) * layout["row_pitch"] + s["h"]
        if ox < fx or right > fx + fw or oy < fy or bottom > fy + fh:
            errors.append("Slots passen nicht in die Setzform")
        # Buchstabenbox: alle Zellen muessen in den Kasten passen
        c = layout["case"]
        lx, ly, lw, lh = rects["letter_case"]
        if (c["origin"][0] + c["cols"] * c["cell_w"] > lx + lw
                or c["origin"][1] + c["rows"] * c["cell_h"] > ly + lh):
            errors.append("Zellen passen nicht in die Buchstabenbox")
        if len(c["letters"]) > c["cols"] * c["rows"]:
            errors.append("zu viele Buchstaben fuer die Box")

    if "t_sheet" in rects:
        # Interlinear-Blatt: alle Zellen muessen auf das Blatt passen
        c = layout["sheet_cells"]
        sx, sy, sw, sh = rects["t_sheet"]
        if (c["origin"][0] < sx or c["origin"][0] + c["cols"] * c["col_w"] > sx + sw
                or c["origin"][1] < sy or c["origin"][1] + c["rows"] * c["row_h"] > sy + sh):
            errors.append("Zellen passen nicht auf das Interlinear-Blatt")
        # Kaesten: drei Reihen Plaettchen, zwei der breitesten nebeneinander
        t = layout["tile"]
        for name in ("t_box_0", "t_box_1", "t_box_2"):
            _, _, bw, bh = rects[name]
            if t["box_inset"] + t["box_rows"] * t["row_pitch"] > bh:
                errors.append(f"{name}: Reihen passen nicht")
            if 2 * t["max_w"] + t["gap"] > bw - 2 * t["box_inset"]:
                errors.append(f"{name}: zwei breite Plaettchen passen nicht nebeneinander")
    return errors


def render(layout, pal, out):
    cw, ch = layout["canvas"]["w"], layout["canvas"]["h"]
    img = Image.new("RGB", (cw * SCALE, ch * SCALE), pal["wood_0"])
    d = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("consola.ttf", 5 * SCALE)
        small = ImageFont.truetype("consola.ttf", 4 * SCALE)
    except OSError:
        font = small = ImageFont.load_default()

    def box(rect, fill, outline="#000000"):
        x, y, w, h = rect
        d.rectangle([x * SCALE, y * SCALE, (x + w) * SCALE - 1, (y + h) * SCALE - 1],
                    fill=fill, outline=outline)

    box([0, 22, cw, 168], pal["wood_1"], outline=pal["wood_1"])  # Tischflaeche
    for name, spec in layout["rects"].items():
        box(spec["rect"], pal[spec["fill"]])
        if spec["label"]:
            x, y, w, h = spec["rect"]
            light = spec["fill"].startswith(("parchment", "gui_face", "flame"))
            d.text(((x + 2) * SCALE, (y + 2) * SCALE), spec["label"],
                   fill="#000000" if light else "#ffffff", font=small)

    if "forme" in layout["rects"]:
        s, (ox, oy) = layout["sort"], layout["forme_slots_origin"]
        samples = ["EN ARCHĒ ĒN HO", "LOGOS", "", ""]
        for row in range(layout["forme_rows"]):
            sample = samples[row] if row < len(samples) else ""
            for i in range(layout["line_slots"]):
                r = [ox + i * s["pitch"], oy + row * layout["row_pitch"], s["w"], s["h"]]
                ch_ = sample[i] if i < len(sample) else ""
                box(r, pal["lead_3"] if ch_.strip() else pal["wood_0"])
                if ch_.strip():
                    d.text(((r[0] + 2) * SCALE, (r[1] + 3) * SCALE), ch_, fill=pal["ink_0"], font=font)
        c = layout["case"]
        for i, letter in enumerate(c["letters"]):
            cx = c["origin"][0] + (i % c["cols"]) * c["cell_w"]
            cy = c["origin"][1] + (i // c["cols"]) * c["cell_h"]
            r = [cx + c["sort_offset"][0], cy + c["sort_offset"][1], s["w"], s["h"]]
            box(r, pal["lead_3"])
            d.text(((r[0] + 2) * SCALE, (r[1] + 3) * SCALE), letter, fill=pal["ink_0"], font=font)
            d.text(((cx + 3) * SCALE, (cy + c["count_offset_y"]) * SCALE), "16", fill="#ffffff", font=small)

    if "t_sheet" in layout["rects"]:
        c, t = layout["sheet_cells"], layout["tile"]
        for i in range(c["cols"] * c["rows"]):
            x = c["origin"][0] + (i % c["cols"]) * c["col_w"]
            y = c["origin"][1] + (i // c["cols"]) * c["row_h"]
            d.text((x * SCALE, (y + c["head_dy"]) * SCALE), "WORT", fill=pal["rubric"], font=small)
            box([x, y + c["slot_dy"], c["slot_w"], t["h"]], pal["parchment_1"])

    img.save(out)
    print(f"geschrieben: {out}")


def main():
    pal = load("palette.json")
    failed = False
    for source, target in LAYOUTS:
        layout = load(source)
        render(layout, pal, HERE / "out" / target)
        for e in check(layout):
            print(f"FEHLER in {source}:", e)
            failed = True
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
