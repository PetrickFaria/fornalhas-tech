#!/usr/bin/env python3
"""Dependency-free checks for the focused 1.0.34 visual changes."""
import hashlib
import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/fornalhastech"
BLOCK = (ROOT / "src/main/java/br/com/petrick/fornalhastech/block/TechFurnaceBlock.java").read_text()
RENDERER = (ROOT / "src/main/java/br/com/petrick/fornalhastech/client/TechFurnaceEnergyRenderer.java").read_text()
PHASE = (ROOT / "src/main/java/br/com/petrick/fornalhastech/client/EnergyFlowPhase.java").read_text()

def png_info(path):
    data = path.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", path
    length = struct.unpack(">I", data[8:12])[0]
    assert data[12:16] == b"IHDR" and length == 13, path
    width, height, depth, color = struct.unpack(">IIBB", data[16:26])
    return width, height, depth, color

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

assert "mod_version=1.0.34" in (ROOT / "gradle.properties").read_text()
assert 'version="${version}"' in (ROOT / "src/main/resources/META-INF/mods.toml").read_text()
assert "public static final int FRAMES = 7;" in PHASE
assert "public static final int[] FRAME_ORDER = {0, 1, 5, 2, 3, 4, 6};" in PHASE
assert "IDLE_CYCLE_TICKS = 60.0" in PHASE and "ACTIVE_CYCLE_TICKS = 20.0" in PHASE
assert "phase - phaseFrame" in RENDERER and "PASSES[(frame + 1) % 5]" not in RENDERER

# The original five pixel-art sheets are unmodified; the two tween sheets are
# interpolated at the highest-difference boundaries 1->2 and 4->0.
original_flow_hashes = {
    0: "a3dc7789660a702b0e7ca808a228585f20c76e7b2ed907d91ae2c7b6bdc08c20",
    1: "dbf2f574ec3149447be15f6537401108a6b962b68458534bb615e92ad82f80ce",
    2: "71ab9980e5297dd0a09ec6aa14955984cfc5981497edb87020a1318d1c706cbc",
    3: "617f90583b46ecca4bad5eaf42f13504b2ddeae2a573c3492261d54f7a74fc25",
    4: "ce9ef97be5292342f8a1e8491b6f16c787ecc968dae6cd8d03e2b218573853b5",
}
for i, expected in original_flow_hashes.items():
    assert sha(ASSETS / f"textures/block/energy_flow_{i}.png") == expected, i
for i in range(7):
    image = ASSETS / f"textures/block/energy_flow_{i}.png"
    assert png_info(image)[:2] == (64, 320), (i, png_info(image))
    meta = json.loads((ASSETS / f"textures/block/energy_flow_{i}.png.mcmeta").read_text())
    assert meta == {"texture": {"blur": False, "clamp": True}}
assert sha(ASSETS / "textures/block/energy_flow_5.png") == "3f9cd3c41f6e4ef00b69eb3a230b2e4452ef7f342b7f0627589511b9d0e59d1d"
assert sha(ASSETS / "textures/block/energy_flow_6.png") == "7749bcc1fb68174d5aea9840011478d36fd1106c3c5b2a5d41564bc389245f19"

# Exactly eight corner assemblies move 1/64 model unit (1/1024 block) inward
# on all axes. Their edge length remains unchanged; inner tips now meet 1.5/14.5.
for model_name, expected_parts in (("tech_furnace_base_animated.json", 25),
                                   ("tech_furnace_base_neutral.json", 28),
                                   ("tech_furnace_base.json", 28)):
    model = json.loads((ASSETS / f"models/block/{model_name}").read_text())
    groups = {i: [] for i in range(8)}
    for element in model["elements"]:
        name = element.get("name", "")
        if name.startswith("vertex126_"):
            group = int(name.split("_", 2)[1])
            groups[group].append(element)
    assert all(len(groups[i]) == expected_parts for i in range(8)), (model_name, [len(groups[i]) for i in range(8)])
    for i in range(8):
        body = next(e for e in groups[i] if e["name"] == f"vertex126_{i}_body")
        for axis in range(3):
            assert abs((body["to"][axis] - body["from"][axis]) - 1.96875) < 1e-9
        expected_from = [(-0.46875 if i < 4 else 14.5)]
        expected_to = [(1.5 if i < 4 else 16.46875)]
        assert abs(body["from"][0] - expected_from[0]) < 1e-9
        assert abs(body["to"][0] - expected_to[0]) < 1e-9
    assert len(groups) == 8
assert "final float step = 1F / 1024F" in RENDERER
assert "int corner = face / 3" in RENDERER

# Fire uses vanilla SMOKE + FLAME particle types and vanilla furnace offsets.
assert "ParticleTypes.SMOKE" in BLOCK and "ParticleTypes.FLAME" in BLOCK
assert "random.nextDouble() * 0.6D - 0.3D" in BLOCK
assert "facing.getStepX() * 0.52D" in BLOCK and "facing.getStepZ() * 0.52D" in BLOCK
assert "random.nextDouble() * 6.0D / 16.0D" in BLOCK
assert "state.getValue(MODE) == 0 && state.getValue(LIT)" in BLOCK
assert "SMALL_FIRE" not in BLOCK and "random.nextFloat() < 0.35F" not in BLOCK
assert not (ROOT / "src/main/java/br/com/petrick/fornalhastech/registry/ModParticles.java").exists()
assert not (ROOT / "src/main/java/br/com/petrick/fornalhastech/client/SmallFlameParticleProvider.java").exists()
assert not (ASSETS / "particles/small_fire.json").exists()

# Redstone emission is still cooking-gated and its average per-tick rate is 1.5x.
assert "!state.getValue(COOKING) || random.nextFloat() >= 0.7875F" in BLOCK
assert "DustParticleOptions" in BLOCK

# The independent fire-panel PNG animation remains exactly as it was.
fire_hashes = {
    0: "53b722d0ae2ce58321b14cabf715b6b8cb560817ee972836f68aee8253e072a3",
    1: "cd54c5e2962a19463b9073e851df3b756d59ffa2f7d3145d4ed1f1709404ee73",
    2: "1b8823375b333d72df73f51ac479da26e4144a4a4efc728e35efc3c7860ec400",
    3: "d29679d02aabbcef496eadd12891e65d5313980f66a4a2d56f6ce890fe1557e4",
    4: "38eba50308697c2e11a391c3d186b447b8afb45bbcae591affd4b151419180f7",
}
for i, expected in fire_hashes.items():
    assert sha(ASSETS / f"textures/block/fire_panel_{i}.png") == expected, i
assert "FIRE_PASSES = new RenderType[5]" in RENDERER
print("1.0.34 checks passed: 7-frame animation, preserved fire PNGs, synchronized corner shift, vanilla fire particles, and +50% redstone rate.")
