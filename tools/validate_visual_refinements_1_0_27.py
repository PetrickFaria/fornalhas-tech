#!/usr/bin/env python3
"""Deterministic checks for 1.0.27 corner recesses, vertex alignment and fuel handoff."""
import json
import math
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL_DIR = ROOT / "src/main/resources/assets/fornalhastech/models/block"
MODELS = ("tech_furnace_base.json", "tech_furnace_base_neutral.json", "tech_furnace_base_animated.json")
SIDE = 1.96875
HOLE = SIDE * 0.25
HALF_HOLE = HOLE / 2
EPS = 1e-10


def close(a, b):
    return math.isclose(a, b, rel_tol=0, abs_tol=EPS)


for filename in MODELS:
    model = json.loads((MODEL_DIR / filename).read_text())
    elements = model["elements"]
    corner = [e for e in elements if e.get("name", "").startswith("vertex126_")]
    bodies = [e for e in corner if e["name"].endswith("_body")]
    assert len(bodies) == 8, (filename, "expected eight corner cubes")
    seen_centers = set()
    for body in bodies:
        center = tuple((a + b) / 2 for a, b in zip(body["from"], body["to"]))
        dims = tuple(b - a for a, b in zip(body["from"], body["to"]))
        assert all(close(d, SIDE) for d in dims), (filename, body["name"], dims)
        assert all(any(close(c, v) for v in (0, 16)) for c in center), (filename, body["name"], center)
        seen_centers.add(center)
    assert seen_centers == {(x, y, z) for x in (0, 16) for y in (0, 16) for z in (0, 16)}, (filename, seen_centers)

    rims = [e for e in corner if "_rim_" in e["name"]]
    walls = [e for e in corner if "_wall_" in e["name"]]
    caps = [e for e in corner if e["name"].endswith("_cap")]
    assert (len(rims), len(walls), len(caps)) == (96, 96, 0 if filename.endswith("animated.json") else 24)
    groups = {}
    for element in corner:
        match = re.match(r"vertex126_(\d+)_([a-z]+)_(?:rim_\d+|wall_\d+|cap)$", element["name"])
        if match:
            groups.setdefault((int(match.group(1)), match.group(2)), []).append(element)
    assert len(groups) == 24, (filename, len(groups))
    assert all(sum(1 for i, _ in groups if i == corner_id) == 3 for corner_id in range(8))

    body_by_id = {int(e["name"].split("_")[1]): e for e in bodies}
    axes = {"west": 0, "east": 0, "down": 1, "up": 1, "north": 2, "south": 2}
    for (corner_id, face), group in groups.items():
        face_axis = axes[face]
        tangent_axes = [axis for axis in range(3) if axis != face_axis]
        center = tuple((a + b) / 2 for a, b in zip(body_by_id[corner_id]["from"], body_by_id[corner_id]["to"]))
        face_rims = [e for e in group if "_rim_" in e["name"]]
        assert len(face_rims) == 4
        for tangent in tangent_axes:
            endpoints = {v for e in face_rims for key in ("from", "to") for v in (e[key][tangent],)}
            assert any(close(v, center[tangent] - HALF_HOLE) for v in endpoints), (filename, corner_id, face, tangent, endpoints)
            assert any(close(v, center[tangent] + HALF_HOLE) for v in endpoints), (filename, corner_id, face, tangent, endpoints)
    if caps:
        for cap in caps:
            dims = sorted(b - a for a, b in zip(cap["from"], cap["to"]))
            assert close(dims[-1], HOLE), (filename, cap["name"], dims)
            assert close(dims[-2], HOLE)

renderer = (ROOT / "src/main/java/br/com/petrick/fornalhastech/client/TechFurnaceEnergyRenderer.java").read_text()
block = re.search(r"CORNER_VERTICES\s*=\s*\{(.*?)\};\s*private static final float\[\]\[\] CORNER_NORMALS", renderer, re.S)
assert block, "corner dynamic quad array not found"
values = [float(v) for v in re.findall(r"(-?\d+\.\d+)F", block.group(1))]
assert len(values) == 24 * 4 * 3, len(values)
for i in range(24):
    points = [values[i * 12 + j * 3:i * 12 + (j + 1) * 3] for j in range(4)]
    extents = [max(p[a] for p in points) - min(p[a] for p in points) for a in range(3)]
    dims = sorted(extents)
    assert close(dims[0], 0) and close(dims[1], HOLE / 16) and close(dims[2], HOLE / 16), (i, dims)

entity = (ROOT / "src/main/java/br/com/petrick/fornalhastech/blockentity/TechFurnaceEntity.java").read_text()
prime = entity.index("FuelWork.shouldPrimeNextTick(furnace.burn, furnace.hasRecipeWork(recipeType))")
lit = entity.index("boolean lit = furnace.burn > 0;")
assert prime < lit, "fuel handoff must resolve before final LIT state"
assert "if (furnace.burn <= 0" in entity[entity.rfind("// Decide LIT"):prime], "avoid repeated recipe lookup while fuel remains"
work_check = entity.index("return nextRecipe != null && canOutput(nextRecipe);")
assert work_check > prime, "priming must require a valid next recipe and output space"
assert "shouldPrimeNextTick(int remainingBurn, boolean recipeWorkRemains)" in (ROOT / "src/main/java/br/com/petrick/fornalhastech/blockentity/FuelWork.java").read_text()

light = (ROOT / "src/main/java/br/com/petrick/fornalhastech/block/FurnaceLightLevel.java").read_text()
assert "burning ? 10 : 0" in light and "burning ? 15 : 10" in light
main_screen = (ROOT / "src/main/java/br/com/petrick/fornalhastech/client/TechFurnaceScreen.java").read_text()
assert "Math.round((14 - font.width(pGlyph)) / 2.0F) + 1" in main_screen
assert "Math.round((14 - font.lineHeight) / 2.0F) + 1" in main_screen
custom = (ROOT / "src/main/java/br/com/petrick/fornalhastech/client/TechFurnaceCustomizationScreen.java").read_text()
assert "RETURN_WIDTH=16" in custom and "RETURN_HEIGHT=14" in custom
assert "left+RETURN_X+RETURN_WIDTH" in custom
texture = (ROOT / "src/main/resources/assets/fornalhastech/textures/gui/tech_furnace_customization.png").read_bytes()
assert texture[:8] == b"\x89PNG\r\n\x1a\n"
assert int.from_bytes(texture[16:20], "big") == 176 and int.from_bytes(texture[20:24], "big") == 89

version = re.search(r"^mod_version=(.+)$", (ROOT / "gradle.properties").read_text(), re.M).group(1)
assert version == "1.0.27", version
print("PASS: 8 vertex-centered cubes; 24 25%-side recessed channels in all model variants; animated quads match; final LIT is resolved after fuel handoff; 0/10 and 10/15 light levels; P controls updated; version 1.0.27.")
