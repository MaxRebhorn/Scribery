"""Erzeugt die zerbrochenen Amphoren-Varianten (Block-Modelle, Blockstates, Item-Modelle) fuer Wracks und Ruinen.

Aufruf: python art/gen_amphora_variants.py
Ausgabe: mod/src/main/resources/assets/scribery/{models/block, models/item, blockstates}/amphora_*.json

Varianten (alle mit der Textur der heilen Amphore, jede dreht sich nach der Blickrichtung `facing`):
  amphora_lying         heile Amphore, auf der Seite liegend
  amphora_lying_broken  auf der Seite liegend, Hals und Lippe abgebrochen
  amphora_broken_neck   stehend, Hals abgebrochen (gezackter Rand)
  amphora_half          nur der untere Teil steht noch, Rand als Scherbenkranz
  amphora_shards        flach verstreute Scherben
  amphora_tilted        schief angelehnt (Kippwinkel 22,5 Grad)

Das heile Modell steht in amphora.json; seine Teile sind hier nachgebaut (Fuss, Bauch, Schulter, Hals, Lippe, Henkel).
Liegende Amphoren: Das stehende Modell wird um 3 Pixel nach hinten (z) verschoben und im Blockstate mit x=90 gekippt.
Dann liegt der Bauch genau auf dem Boden (Bauchradius 5, nach der Drehung um die Blockmitte 3 Pixel zu hoch).
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "mod/src/main/resources/assets/scribery")

FACES_UV = {"up": [0, 0, 3, 3], "down": [0, 0, 3, 3]}


def box(f, t, rotation=None):
    """Quader mit der Amphoren-Textur auf allen Seiten, Ober-/Unterseite aus der glatten Tonflaeche (wie amphora.json)."""
    faces = {}
    for side in ("north", "south", "east", "west"):
        faces[side] = {"texture": "#t"}
    for side, uv in FACES_UV.items():
        faces[side] = {"texture": "#t", "uv": uv}
    el = {"from": list(f), "to": list(t), "faces": faces}
    if rotation:
        el["rotation"] = rotation
    return el


# Teile der heilen Amphore (stehend), wie in amphora.json
FOOT = ((5, 0, 5), (11, 2, 11))
BELLY = ((3, 2, 3), (13, 10, 13))
SHOULDER = ((4, 10, 4), (12, 12, 12))
NECK = ((6, 12, 6), (10, 15, 10))
LIP = ((5, 15, 5), (11, 16, 11))
HANDLE_L = ((4, 12, 7), (6, 15, 9))
HANDLE_R = ((10, 12, 7), (12, 15, 9))

FULL = [FOOT, BELLY, SHOULDER, NECK, LIP, HANDLE_L, HANDLE_R]

# Hals abgebrochen: Fuss, Bauch, Schulter und gezackte Reste (Halsstummel, ein Henkelstumpf)
BROKEN_NECK = [FOOT, BELLY, SHOULDER,
               ((6, 12, 6), (9, 14, 9)), ((9, 12, 7), (11, 13, 10)), ((4, 12, 7), (6, 14, 9)),
               ((7, 12, 10), (9, 13, 11))]

# Nur der Unterbau: Fuss, unterer Bauch, ringsum ein unregelmaessiger Scherbenkranz
HALF = [FOOT, ((3, 2, 3), (13, 6, 13)),
        ((3, 6, 3), (13, 8, 5)),      # Nordwand, mittelhoch
        ((3, 6, 11), (13, 7, 13)),    # Suedwand, niedrig
        ((3, 6, 5), (5, 9, 11)),      # Westwand, hoch
        ((11, 6, 5), (13, 6.5, 11)),  # Ostwand fast weg
        ((5, 8, 3), (8, 9, 4))]       # Spitze auf der Nordwand

# Verstreute Scherben: flache Stuecke in verschiedener Hoehe, einzelne uebereinander
SHARDS = [((2, 0, 3), (7, 1, 6)), ((8, 0, 2), (13, 2, 5)), ((4, 0, 8), (9, 1, 12)), ((10, 0, 9), (14, 3, 13)),
          ((1, 0, 11), (4, 2, 14)), ((6, 1, 4), (9, 3, 6)), ((5, 1, 9), (8, 2, 11)), ((11, 2, 10), (13, 3, 12))]


def shift_z(parts, dz):
    return [((f[0], f[1], f[2] + dz), (t[0], t[1], t[2] + dz)) for f, t in parts]


def model(parts, rotation=None):
    return {"textures": {"t": "scribery:block/amphora", "particle": "scribery:block/amphora"},
            "elements": [box(f, t, rotation) for f, t in parts]}


def blockstate(name, x_rot=0):
    """Multipart: je Blickrichtung dasselbe Modell, um y gedreht (und bei liegenden um x gekippt)."""
    out = []
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        apply = {"model": f"scribery:block/{name}"}
        if x_rot:
            apply["x"] = x_rot
        if y:
            apply["y"] = y
        out.append({"when": {"facing": facing}, "apply": apply})
    return {"multipart": out}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def main():
    # name: (Modellteile, Drehung der Teile, x-Kippung im Blockstate, Eltern-Modell fuers Item)
    tilt = {"origin": [5, 0, 8], "axis": "z", "angle": -22.5}   # kippt um die untere Fusskante
    variants = {
        "amphora_lying": (shift_z(FULL, -3), None, 90, "amphora"),
        "amphora_lying_broken": (shift_z(BROKEN_NECK, -3), None, 90, "amphora_broken_neck"),
        "amphora_broken_neck": (BROKEN_NECK, None, 0, None),
        "amphora_half": (HALF, None, 0, None),
        "amphora_shards": (SHARDS, None, 0, None),
        "amphora_tilted": (FULL, tilt, 0, None),
    }
    for name, (parts, rot, x_rot, item_parent) in variants.items():
        write(os.path.join(ASSETS, "models/block", name + ".json"), model(parts, rot))
        write(os.path.join(ASSETS, "blockstates", name + ".json"), blockstate(name, x_rot))
        write(os.path.join(ASSETS, "models/item", name + ".json"),
              {"parent": f"scribery:block/{item_parent or name}"})
    print("ok", len(variants), "Varianten")


if __name__ == "__main__":
    main()
