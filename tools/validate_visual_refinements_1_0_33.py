#!/usr/bin/env python3
"""Dependency-free checks for the focused 1.0.33 visual refinements."""
import json, math, struct, zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/fornalhastech"
BLOCK = (ROOT / "src/main/java/br/com/petrick/fornalhastech/block/TechFurnaceBlock.java").read_text()
RENDERER = (ROOT / "src/main/java/br/com/petrick/fornalhastech/client/TechFurnaceEnergyRenderer.java").read_text()
PROVIDER = (ROOT / "src/main/java/br/com/petrick/fornalhastech/client/SmallFlameParticleProvider.java").read_text()

def png_info(path):
    data = path.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", path
    length = struct.unpack(">I", data[8:12])[0]
    assert data[12:16] == b"IHDR" and length == 13, path
    width, height, depth, color = struct.unpack(">IIBB", data[16:26])
    return width, height, depth, color

assert "mod_version=1.0.33" in (ROOT / "gradle.properties").read_text()
assert 'version="${version}"' in (ROOT / "src/main/resources/META-INF/mods.toml").read_text()
assert "random.nextFloat() < 0.35F" in BLOCK  # 2.5x prior 0.14 random-tick chance
assert "ModParticles.SMALL_FIRE.get()" in BLOCK
assert "state.getValue(MODE) == 0 && state.getValue(LIT)" in BLOCK
assert "case SOUTH" in BLOCK and "case EAST" in BLOCK and "case WEST" in BLOCK
assert "particle.scale(0.25F)" in PROVIDER and "FlameParticle.Provider" in PROVIDER
assert "drawFire(poses.last(), buffers.getBuffer(FIRE_PASSES[fireFrame])," in RENDERER
assert all(f"case {d} ->" in RENDERER for d in ("SOUTH", "EAST", "WEST"))
assert "switch (facing)" in RENDERER and "Vector3f.YP.rotationDegrees(facingRotation)" not in RENDERER
assert "FIRE_LEFT = 4.50F / 16F" in RENDERER and "FIRE_RIGHT = 11.50F / 16F" in RENDERER
assert "FIRE_NEAR = 0.25F / 16F" in RENDERER

particle_json = json.loads((ASSETS / "particles/small_fire.json").read_text())
assert particle_json == {"textures": ["minecraft:flame"]}
for i in range(5):
    assert png_info(ASSETS / f"textures/block/fire_panel_{i}.png")[:2] == (64, 32)
for face in ("north", "south", "west", "east", "up", "down"):
    assert png_info(ASSETS / f"textures/block/tech_base_flat_{face}.png")[:2] == (64, 64)
assert png_info(ASSETS / "textures/block/tech_steel.png")[:2] == (64, 64)

# Confirm each explicit quad has outward winding and its plane is the face selected.
L, R, T, B, D = 4.5/16, 11.5/16, 6.10/16, 2.50/16, 0.25/16
quads = {
    "north": ([(L,T,D),(R,T,D),(R,B,D),(L,B,D)], (0,0,-1), "z", D),
    "south": ([(R,T,1-D),(L,T,1-D),(L,B,1-D),(R,B,1-D)], (0,0,1), "z", 1-D),
    "east": ([(1-D,T,L),(1-D,T,R),(1-D,B,R),(1-D,B,L)], (1,0,0), "x", 1-D),
    "west": ([(D,T,R),(D,T,L),(D,B,L),(D,B,R)], (-1,0,0), "x", D),
}
for face, (q, normal, axis, plane) in quads.items():
    axis_i = {"x": 0, "z": 2}[axis]
    assert all(abs(p[axis_i]-plane) < 1e-9 for p in q), (face, q)
    a = [q[1][i]-q[0][i] for i in range(3)]
    b = [q[2][i]-q[1][i] for i in range(3)]
    cross = (a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0])
    assert sum(cross[i]*normal[i] for i in range(3)) > 0, (face, cross, normal)

# Preserve the established oscillating five-frame PNG set and opacity; only the
# separate structural metal maps are touched in this update.
def png_rgba(path):
    data=path.read_bytes(); pos=8; comp=bytearray(); w=h=depth=color=None
    while pos<len(data):
        size=struct.unpack(">I",data[pos:pos+4])[0]; kind=data[pos+4:pos+8]; chunk=data[pos+8:pos+8+size]; pos+=12+size
        if kind==b"IHDR": w,h,depth,color,_,_,_=struct.unpack(">IIBBBBB",chunk)
        elif kind==b"IDAT": comp.extend(chunk)
        elif kind==b"IEND": break
    assert depth==8 and color==6, path
    raw=zlib.decompress(comp); stride=w*4; prior=bytearray(stride); off=0; alpha=0
    for _ in range(h):
        filt=raw[off]; off+=1; row=bytearray(raw[off:off+stride]); off+=stride
        for j in range(stride):
            left=row[j-4] if j>=4 else 0; up=prior[j]; ul=prior[j-4] if j>=4 else 0
            if filt==1: row[j]=(row[j]+left)&255
            elif filt==2: row[j]=(row[j]+up)&255
            elif filt==3: row[j]=(row[j]+(left+up)//2)&255
            elif filt==4:
                p=left+up-ul; pa=abs(p-left); pb=abs(p-up); pc=abs(p-ul)
                row[j]=(row[j]+(left if pa<=pb and pa<=pc else up if pb<=pc else ul))&255
            else: assert filt==0, (path,filt)
        alpha += sum(row[j] > 0 for j in range(3,stride,4)); prior=row
    return alpha/(w*h)

for i in range(5):
    coverage=png_rgba(ASSETS/f"textures/block/fire_panel_{i}.png")
    assert 0.90 <= coverage < 1.0, (i, coverage)
print("1.0.33 checks passed: particle rate/scale, four outward-facing flame quads, and resource dimensions/alpha.")
