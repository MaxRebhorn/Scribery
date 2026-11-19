"""Erzeugt die Weltgenerierungs-Daten: Strukturen, Strukturgruppen, Biom-Listen, Funde (Suspicious-Bloecke, Eis) und Biom-Modifikatoren.

Aufruf: python tools/gen_worldgen.py
Ausgabe: mod/src/main/resources/data/scribery/... (siehe docs/DESIGN-WORLDGEN.md)

Strukturen bauen mit denselben Generatoren wie die Debug-Befehle (ScriberyStructure / ScriberyPiece). Die Funde sind
"ore"-Features, die Bloecke der Oberflaeche ersetzen. Asche gibt es nicht in der Weltgenerierung (kein Nether, siehe Doku).
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
D = os.path.join(ROOT, "mod", "src", "main", "resources", "data", "scribery")


def w(rel, obj):
    p = os.path.join(D, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def mc(*names):
    return ["minecraft:" + n for n in names]


# ---------------------------------------------------------------- Strukturen
# kind: (Biome, Schritt, Abstand, Mindestabstand, Salz)
STRUCTURES = {
    "wreck": (["#minecraft:is_overworld"],     # ueberall; ob Wasser da ist, prueft ScriberyStructure (Wassertiefe)
              "surface_structures", 14, 5, 1743001),
    "caravan": (mc("snowy_plains", "ice_spikes", "snowy_taiga"), "surface_structures", 16, 6, 1743002),
    "wanderer": (mc("snowy_plains", "ice_spikes", "snowy_taiga", "snowy_slopes"), "surface_structures", 10, 4, 1743003),
    "grave": (mc("swamp", "mangrove_swamp"), "surface_structures", 16, 6, 1743004),
    "catacomb": (mc("plains", "sunflower_plains", "meadow", "forest", "flower_forest", "birch_forest", "old_growth_birch_forest",
                    "taiga", "savanna", "savanna_plateau"), "underground_structures", 36, 14, 1743005),
    "catacomb_desert": (mc("desert"), "underground_structures", 30, 12, 1743006),
    "house_sand": (mc("desert"), "underground_structures", 8, 3, 1743007),
    "house_mud": (mc("swamp", "mangrove_swamp", "river"), "underground_structures", 8, 3, 1743008),
}

for kind, (biomes, step, spacing, sep, salt) in STRUCTURES.items():
    w(f"tags/worldgen/biome/has_structure/{kind}.json", {"replace": False, "values": biomes})
    w(f"worldgen/structure/{kind}.json", {
        "type": "scribery:scribery", "kind": kind, "biomes": f"#scribery:has_structure/{kind}", "step": step,
        "spawn_overrides": {}, "terrain_adaptation": "none"})
    w(f"worldgen/structure_set/{kind}.json", {
        "structures": [{"structure": f"scribery:{kind}", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": sep, "salt": salt}})

# ---------------------------------------------------------------- Funde (Bloecke der Oberflaeche)


def ore(name, targets, state, size):
    w(f"worldgen/configured_feature/{name}.json", {
        "type": "minecraft:ore",
        "config": {"discard_chance_on_air_exposure": 0.0, "size": size, "targets": [
            {"target": {"predicate_type": "minecraft:block_match", "block": t}, "state": state} for t in targets]}})


def placed(name, count, chance, heightmap):
    w(f"worldgen/placed_feature/{name}.json", {
        "feature": f"scribery:{name}",
        "placement": [{"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
                      {"type": "minecraft:heightmap", "heightmap": heightmap},
                      {"type": "minecraft:random_offset", "xz_spread": 0, "y_spread": 1},
                      {"type": "minecraft:rarity_filter", "chance": chance}, {"type": "minecraft:biome"}]})


def modifier(name, biomes, step="top_layer_modification"):
    w(f"tags/worldgen/biome/has_feature/{name}.json", {"replace": False, "values": biomes})
    w(f"neoforge/biome_modifier/{name}.json", {
        "type": "neoforge:add_features", "biomes": f"#scribery:has_feature/{name}", "features": f"scribery:{name}", "step": step})


SAND = {"Name": "scribery:suspicious_sand", "Properties": {"dusted": "0"}}
MUD = {"Name": "scribery:suspicious_mud", "Properties": {"dusted": "0"}}

# Suspicious Sand: einfach so in der Wueste verstreut
ore("suspicious_sand", mc("sand"), SAND, 3)
placed("suspicious_sand", 3, 3, "OCEAN_FLOOR_WG")
modifier("suspicious_sand", mc("desert"))

# Suspicious Mud: in Suempfen, an Land und unter Wasser, dazu seltener in Fluessen und an Straenden (Wasserschaeden)
ore("suspicious_mud", mc("mud", "dirt", "grass_block", "clay", "gravel", "sand"), MUD, 3)
placed("suspicious_mud_swamp", 3, 3, "OCEAN_FLOOR_WG")
w("worldgen/placed_feature/suspicious_mud_swamp.json", {
    "feature": "scribery:suspicious_mud",
    "placement": [{"type": "minecraft:count", "count": 3}, {"type": "minecraft:in_square"},
                  {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"},
                  {"type": "minecraft:random_offset", "xz_spread": 0, "y_spread": 1},
                  {"type": "minecraft:rarity_filter", "chance": 3}, {"type": "minecraft:biome"}]})
w("worldgen/placed_feature/suspicious_mud_water.json", {
    "feature": "scribery:suspicious_mud",
    "placement": [{"type": "minecraft:count", "count": 1}, {"type": "minecraft:in_square"},
                  {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"},
                  {"type": "minecraft:random_offset", "xz_spread": 0, "y_spread": 1},
                  {"type": "minecraft:rarity_filter", "chance": 8}, {"type": "minecraft:biome"}]})
for name, biomes in (("suspicious_mud_swamp", mc("swamp", "mangrove_swamp")),
                     ("suspicious_mud_water", mc("river", "beach"))):
    w(f"tags/worldgen/biome/has_feature/{name}.json", {"replace": False, "values": biomes})
    w(f"neoforge/biome_modifier/{name}.json", {
        "type": "neoforge:add_features", "biomes": f"#scribery:has_feature/{name}", "features": f"scribery:{name}",
        "step": "top_layer_modification"})

# Eingefrorene Fragmente: im Eis der Eisbiome
ore("frozen_fragment", mc("ice", "packed_ice", "blue_ice"), {"Name": "scribery:frozen_fragment_block"}, 3)
placed("frozen_fragment", 3, 4, "WORLD_SURFACE_WG")
modifier("frozen_fragment", mc("ice_spikes", "frozen_ocean", "deep_frozen_ocean", "frozen_river", "snowy_plains", "snowy_taiga",
                               "frozen_peaks", "jagged_peaks", "snowy_slopes"))
# Wracks in ueberfluteten Hoehlen: ein Feature sucht in jedem Chunk einige Male nach einem Hohlraum unter Wasser
w("worldgen/configured_feature/cave_wreck.json", {"type": "scribery:cave_wreck", "config": {}})
w("worldgen/placed_feature/cave_wreck.json", {
    "feature": "scribery:cave_wreck",
    "placement": [{"type": "minecraft:rarity_filter", "chance": 10}, {"type": "minecraft:in_square"}, {"type": "minecraft:biome"}]})
w("neoforge/biome_modifier/cave_wreck.json", {
    "type": "neoforge:add_features", "biomes": "#minecraft:is_overworld", "features": "scribery:cave_wreck",
    "step": "underground_decoration"})
print("ok")
