#!/usr/bin/env python3
"""Deterministic checks for 1.0.28 inward corner shift and preserved invariants."""
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
INWARD_AXIS_SHIFT = 0.375
LIGHT_AXIS_SHIFT = INWARD_AXIS_SHIFT / 16
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
    body_by_id = {}
    for body in bodies:
        corner_id = int(body["name"].split("_")[1])
        center = tuple((a + b) / 2 for a, b in zip(body["from"], body["to"]))
        dims = tuple(b - a for a, b in zip(body["from"], body["to"]))
        assert all(close(d, SIDE) for d in dims), (filename, body["name"], dims)
        assert all(any(close(c, v) for v in (0.375, 15.625)) for c in center), (filename, body["name"], center)
        old_center = tuple(0 if c < 8 else 16 for c in center)
        expected_center = tuple(v + (INWARD_AXIS_SHIFT if v == 0 else -INWARD_AXIS_SHIFT) for v in old_center)
        assert all(close(a, b) for a, b in zip(center, expected_center)), (filename, body["name"], old_center, center)
        assert all(close(abs(a - b), INWARD_AXIS_SHIFT) for a, b in zip(center, old_center))
        assert math.isclose(math.dist(center, old_center), math.sqrt(3) * INWARD_AXIS_SHIFT, abs_tol=EPS)
        assert any(v < 0 for v in body["from"]) or any(v > 16 for v in body["to"]), (filename, body["name"], "cube lost positive protrusion")
        seen_centers.add(center)
        body_by_id[corner_id] = body
    assert seen_centers == {(x, y, z) for x in (0.375, 15.625) for y in (0.375, 15.625) for z in (0.375, 15.625)}, (filename, seen_centers)
    assert not any(e.get("name", "").startswith("edge126_") for e in elements), (filename, "edge frames must remain absent")

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
body_model = json.loads((MODEL_DIR / "tech_furnace_base.json").read_text())
caps_by_corner = {}
for element in body_model["elements"]:
    match = re.fullmatch(r"vertex126_(\d+)_([a-z]+)_cap", element.get("name", ""))
    if match:
        caps_by_corner.setdefault(int(match.group(1)), {})[match.group(2)] = element
assert len(caps_by_corner) == 8 and all(len(v) == 3 for v in caps_by_corner.values())
face_axis = {"west": 0, "east": 0, "down": 1, "up": 1, "north": 2, "south": 2}
face_index = {0: ("west", "east"), 1: ("down", "up"), 2: ("north", "south")}
for i in range(24):
    points = [values[i * 12 + j * 3:i * 12 + (j + 1) * 3] for j in range(4)]
    extents = [max(p[a] for p in points) - min(p[a] for p in points) for a in range(3)]
    dims = sorted(extents)
    assert close(dims[0], 0) and close(dims[1], HOLE / 16) and close(dims[2], HOLE / 16), (i, dims)
    corner_id, face_no = divmod(i, 3)
    normal_axis = extents.index(min(extents))
    center = [(a + b) / 2 for a, b in zip(body_by_id[corner_id]["from"], body_by_id[corner_id]["to"])]
    for tangent in (axis for axis in range(3) if axis != normal_axis):
        assert close(sum(p[tangent] for p in points) / 4, center[tangent] / 16), (i, tangent, points, center)
    low_face, high_face = face_index[normal_axis]
    face = low_face if center[normal_axis] < 8 else high_face
    cap = caps_by_corner[corner_id][face]
    cap_plane = cap["from"][normal_axis] / 16
    quad_plane = sum(p[normal_axis] for p in points) / 4
    assert close(quad_plane, cap_plane), (i, face, quad_plane, cap_plane)

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
assert version == "1.0.28", version
print("PASS: eight cubes moved 0.375 units on all three axes (radial 0.6495191), dimensions preserved; 24 recessed 25%-side details remain aligned; edge frames absent; fuel handoff, light levels and P controls preserved; version 1.0.28.")
