#!/usr/bin/env python3
"""Checks 1.0.35 appearance-driven lighting and retained visual refinements."""
import hashlib
import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/fornalhastech"
JAVA = ROOT / "src/main/java/br/com/petrick/fornalhastech"
BLOCK = (JAVA / "block/TechFurnaceBlock.java").read_text()
BLOCKS = (JAVA / "registry/ModBlocks.java").read_text()
LIGHT = (JAVA / "block/FurnaceLightLevel.java").read_text()
ENTITY = (JAVA / "blockentity/TechFurnaceEntity.java").read_text()
PHASE = (JAVA / "client/EnergyFlowPhase.java").read_text()
RENDERER = (JAVA / "client/TechFurnaceEnergyRenderer.java").read_text()
PARTICLES = (JAVA / "registry/ModParticles.java").read_text()
PROVIDER = (JAVA / "client/TechFlameParticleProvider.java").read_text()
CLIENT = (JAVA / "client/ClientEvents.java").read_text()
CUSTOM_GUI = (JAVA / "client/TechFurnaceCustomizationScreen.java").read_text()

def png_info(path):
    data = path.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", path
    length = struct.unpack(">I", data[8:12])[0]
    assert data[12:16] == b"IHDR" and length == 13, path
    width, height, depth, color = struct.unpack(">IIBB", data[16:26])
    return width, height, depth, color

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

assert "mod_version=1.0.35" in (ROOT / "gradle.properties").read_text()
assert 'version="${version}"' in (ROOT / "src/main/resources/META-INF/mods.toml").read_text()

# Sixteen steps, retaining the seven prior images exactly and distributing
# nine interpolants in the highest-difference intervals without changing cycle time.
assert "public static final int FRAMES = 16;" in PHASE
expected_order = [0, 7, 8, 1, 9, 5, 10, 2, 11, 3, 12, 13, 4, 14, 6, 15]
assert "FRAME_ORDER = {" + ", ".join(map(str, expected_order)) + "}" in PHASE
assert "IDLE_CYCLE_TICKS = 60.0" in PHASE and "ACTIVE_CYCLE_TICKS = 20.0" in PHASE
assert "phase - phaseFrame" in RENDERER
old_hashes = {
    0: "a3dc7789660a702b0e7ca808a228585f20c76e7b2ed907d91ae2c7b6bdc08c20",
    1: "dbf2f574ec3149447be15f6537401108a6b962b68458534bb615e92ad82f80ce",
    2: "71ab9980e5297dd0a09ec6aa14955984cfc5981497edb87020a1318d1c706cbc",
    3: "617f90583b46ecca4bad5eaf42f13504b2ddeae2a573c3492261d54f7a74fc25",
    4: "ce9ef97be5292342f8a1e8491b6f16c787ecc968dae6cd8d03e2b218573853b5",
    5: "3f9cd3c41f6e4ef00b69eb3a230b2e4452ef7f342b7f0627589511b9d0e59d1d",
    6: "7749bcc1fb68174d5aea9840011478d36fd1106c3c5b2a5d41564bc389245f19",
}
new_hashes = {
    7: "19a871b8ca6da4ce216001f56f547099e394efdb8bc1606ac12556940077d259",
    8: "1c79feb0e9181b665135165133a194e84e45f37741468bb1e2bfd051593e5434",
    9: "b3d691245c6058d327c6ecaee6e6fb303f934f05a8f93c90a965cc284a4ce92e",
    10: "3115ba05e7dd00e308b39201f4d47369c4cae5d5c072b31de862572b27615cea",
    11: "23ee437c91ca2f282f2a51f7b4fdef9a78a68830a087f33be0e9bbf852716c6b",
    12: "eb5f68274ac49867dbcfb823c0c759249fbcfc4f8dfdd6bc7e57faa5e52d7ab0",
    13: "2b1feb15e2d260e2bbc4489e0f8ea2c2f50caf905a6ecfa392e0cc8e0beded2c",
    14: "f6dcd0a1ef04064848019c137066ffea1b78aba05c0edf76884d6dbe46b2a2fe",
    15: "109c77de63888026f7629052115e2d6bf26f134af6e4d37b1aa0625482e7f77c",
}
for i, expected in {**old_hashes, **new_hashes}.items():
    image = ASSETS / f"textures/block/energy_flow_{i}.png"
    assert sha(image) == expected, (i, sha(image))
    assert png_info(image) == (64, 320, 8, 6), (i, png_info(image))
    meta = json.loads((ASSETS / f"textures/block/energy_flow_{i}.png.mcmeta").read_text())
    assert meta == {"texture": {"blur": False, "clamp": True}}
assert "PASSES = new RenderType[EnergyFlowPhase.FRAMES]" in RENDERER

# Preserve the established 3-D models and 24 moving corner-light quads.
for model_name, expected_parts in (("tech_furnace_base_animated.json", 25),
                                   ("tech_furnace_base_neutral.json", 28),
                                   ("tech_furnace_base.json", 28)):
    model = json.loads((ASSETS / f"models/block/{model_name}").read_text())
    groups = {i: [] for i in range(8)}
    for element in model["elements"]:
        name = element.get("name", "")
        if name.startswith("vertex126_"):
            groups[int(name.split("_", 2)[1])].append(element)
    assert all(len(groups[i]) == expected_parts for i in range(8))
    for i in range(8):
        body = next(e for e in groups[i] if e["name"] == f"vertex126_{i}_body")
        assert all(abs((body["to"][a] - body["from"][a]) - 1.96875) < 1e-9 for a in range(3))
assert "final float step = 1F / 1024F" in RENDERER

# Light emission follows VISUAL_COLOR, not CORE; state changes are synchronized
# with setBlock flags 3 and the selection is persisted in the block entity NBT.
assert "FurnaceLightLevel.get(s.getValue(TechFurnaceBlock.VISUAL_COLOR)" in BLOCKS
assert "TechFurnaceBlock.CORE),s.getValue" not in BLOCKS
assert "public static int get(int visualColor, boolean burning)" in LIGHT
assert "boolean coloredAppearance = visualColor > 0" in LIGHT
assert "TechFurnaceBlock.VISUAL_COLOR, furnace.effectiveVisualColor()" in ENTITY
assert "TechFurnaceBlock.VISUAL_COLOR, effectiveVisualColor()" in ENTITY
assert "level.setBlock(worldPosition, next, 3)" in ENTITY
assert 'tag.putInt("Color", color)' in ENTITY and 'tag.getInt("Color")' in ENTITY
assert "color = value" in ENTITY and "effectiveVisualColor()" in ENTITY
assert "int speed = SPEED[furnace.coreTier()]" in ENTITY

# Empty appearance is pure black, bypasses emissive overlays and emits no redstone dust.
assert "default -> 0xFF000000" in BLOCK
assert "VISUAL_COLOR) == 0 || !state.getValue(COOKING)" in BLOCK
assert "VISUAL_COLOR) == 0) return" in RENDERER
assert "getTechnologyColor(state.getValue(TechFurnaceBlock.VISUAL_COLOR))" in CLIENT
assert sha(ASSETS / "textures/block/corner_neutral.png") == "abc58d5127d7cdf313beb9ec8ee839860a9c6bfbc48c8b8eb6a3f7d8bb63de6f"

# The furnace emits only a localized reduced-size vanilla flame; no smoke.
assert "ParticleTypes.SMOKE" not in BLOCK and "ParticleTypes.FLAME" not in BLOCK
assert "ModParticles.TECH_FLAME.get()" in BLOCK
assert "FlameParticle.Provider" in PROVIDER and "particle.scale(0.75F)" in PROVIDER
assert "event.register(ModParticles.TECH_FLAME.get(), TechFlameParticleProvider::new)" in CLIENT
assert "ModParticles.TYPES.register(bus)" in (JAVA / "FornalhasTech.java").read_text()
assert json.loads((ASSETS / "particles/tech_flame.json").read_text()) == {"textures": ["minecraft:flame"]}

# The P control uses only neutral grayscale edges; other colored option frames remain separate.
assert "neutralFrame(pose,left+RETURN_X" in CUSTOM_GUI
assert "menu.getAccentColor()" not in CUSTOM_GUI
for color in ("0xFF101010", "0xFFF0F0F0", "0xFFB8B8B8", "0xFF555555", "0xFF777777"):
    assert color in CUSTOM_GUI

# Keep the five-panel flame overlay and its orientation-independent resource sheets unchanged.
fire_hashes = {
    0: "53b722d0ae2ce58321b14cabf715b6b8cb560817ee972836f68aee8253e072a3",
    1: "cd54c5e2962a19463b9073e851df3b756d59ffa2f7d3145d4ed1f1709404ee73",
    2: "1b8823375b333d72df73f51ac479da26e4144a4a4efc728e35efc3c7860ec400",
    3: "d29679d02aabbcef496eadd12891e65d5313980f66a4a2d56f6ce890fe1557e4",
    4: "38eba50308697c2e11a391c3d186b447b8afb45bbcae591affd4b151419180f7",
}
for i, expected in fire_hashes.items():
    assert sha(ASSETS / f"textures/block/fire_panel_{i}.png") == expected
assert "FIRE_PASSES = new RenderType[5]" in RENDERER
print("1.0.35 checks passed: appearance-driven light, black empty accents, no empty redstone particles, 16 frames, scaled flame-only particles, and neutral P frame.")
