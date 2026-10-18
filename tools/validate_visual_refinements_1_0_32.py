#!/usr/bin/env python3
"""Dependency-free checks for the focused 1.0.32 visual refinement."""
import json, math, re, struct, zlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/"src/main/resources/assets/fornalhastech"
BLOCK=ASSETS/"textures/block"

def read_rgba(path):
    data=path.read_bytes(); assert data[:8]==b"\x89PNG\r\n\x1a\n"
    pos=8; compressed=bytearray(); width=height=depth=color=None
    while pos<len(data):
        length=struct.unpack(">I",data[pos:pos+4])[0]; kind=data[pos+4:pos+8]; chunk=data[pos+8:pos+8+length]; pos+=12+length
        if kind==b"IHDR": width,height,depth,color,compression,filter_method,interlace=struct.unpack(">IIBBBBB",chunk)
        elif kind==b"IDAT": compressed.extend(chunk)
        elif kind==b"IEND": break
    assert depth==8 and color==6, (path,depth,color)
    raw=zlib.decompress(compressed); stride=width*4; rows=[]; prior=bytearray(stride); offset=0
    for _ in range(height):
        filt=raw[offset]; offset+=1; scan=bytearray(raw[offset:offset+stride]); offset+=stride
        for j in range(stride):
            left=scan[j-4] if j>=4 else 0; up=prior[j]; ul=prior[j-4] if j>=4 else 0
            if filt==1: scan[j]=(scan[j]+left)&255
            elif filt==2: scan[j]=(scan[j]+up)&255
            elif filt==3: scan[j]=(scan[j]+((left+up)//2))&255
            elif filt==4:
                p=left+up-ul; pa=abs(p-left); pb=abs(p-up); pc=abs(p-ul)
                scan[j]=(scan[j]+(left if pa<=pb and pa<=pc else up if pb<=pc else ul))&255
            else: assert filt==0, (path,filt)
        rows.append(scan); prior=scan
    return width,height,rows

def pixel(rows,x,y):
    q=rows[y][x*4:x*4+4]; return tuple(q)

frames=[]
for i in range(5):
    w,h,rows=read_rgba(BLOCK/f"fire_panel_{i}.png")
    assert (w,h)==(64,32),(i,w,h)
    px=[pixel(rows,x,y) for y in range(h) for x in range(w)]
    alpha=sum(p[3]>0 for p in px)/len(px)
    assert .90<=alpha<1,(i,alpha)
    assert all(p[:3]!=(0,0,0) for p in px),f"frame {i} has black RGB texels"
    assert all(p[3]==0 or max(p[:3])>60 for p in px),f"frame {i} has opaque black pixels"
    frames.append(rows)
for frame in frames[1:]:
    for y in range(27,32):
        for x in range(64):
            a,b=pixel(frames[0],x,y),pixel(frame,x,y)
            if a[3] and b[3]: assert a==b

def hot_centroid(rows):
    xs=[x for y in range(12) for x in range(64) if (lambda p:p[3] and p[0]>=245 and p[1]>=140 and p[2]<140)(pixel(rows,x,y))]
    assert xs
    return sum(xs)/len(xs)
centres=[hot_centroid(r) for r in frames]
assert centres[0]<centres[1]<centres[2] and centres[2]>centres[3]>centres[4],centres
assert abs(centres[4]-centres[0])<=1.0,centres

renderer=(ROOT/"src/main/java/br/com/petrick/fornalhastech/client/TechFurnaceEnergyRenderer.java").read_text()
assert "MODE) == 0 && state.getValue(TechFurnaceBlock.LIT)" in renderer
assert "FIRE_QUAD" in renderer and "state.getValue(TechFurnaceBlock.FACING)" in renderer
assert all(f"case {d} -> {a}F" in renderer for d,a in (("EAST","-90"),("SOUTH","-180"),("WEST","-270")))
assert "FIRE_LEFT = 4.50F / 16F" in renderer and "FIRE_RIGHT = 11.50F / 16F" in renderer
assert "FIRE_TOP = 6.10F / 16F" in renderer and "FIRE_BOTTOM = 2.50F / 16F" in renderer
# Verify canonical face placement after the explicit quarter-turns.
left,right,z=4.5/16,11.5/16,.371/16
for angle,face in ((0,"north"),(-90,"east"),(-180,"south"),(-270,"west")):
    t=math.radians(angle); points=[]
    for x in (left,right):
        dx,dz=x-.5,z-.5
        points.append((.5+math.cos(t)*dx+math.sin(t)*dz,.5-math.sin(t)*dx+math.cos(t)*dz))
    plane={"north":all(abs(p[1]-z)<1e-6 for p in points),"east":all(abs(p[0]-(1-z))<1e-6 for p in points),"south":all(abs(p[1]-(1-z))<1e-6 for p in points),"west":all(abs(p[0]-z)<1e-6 for p in points)}[face]
    assert plane,(face,points)

block=(ROOT/"src/main/java/br/com/petrick/fornalhastech/block/TechFurnaceBlock.java").read_text()
assert "state.getValue(MODE) == 0 && state.getValue(LIT) && random.nextFloat() < 0.14F" in block and "ParticleTypes.FLAME" in block
assert all(f"case {d} ->" in block for d in ("SOUTH","EAST","WEST")) and "default ->" in block
assert "!state.getValue(COOKING)" in block and "DustParticleOptions" in block and "random.nextFloat() >= 0.525F" in block

for face in ("north","south","west","east","up","down"):
    _,_,rows=read_rgba(BLOCK/f"tech_base_flat_{face}.png")
    px=[pixel(rows,x,y) for y in range(64) for x in range(64)]
    assert any(p[:3]==(0,0,0) for p in px),face
    if face!="down": assert any(p[0]>=150 and abs(p[0]-p[1])<4 for p in px),face
    assert any(25<=p[0]<=48 and p[0]==p[1]==p[2] for p in px),face
_,_,steel=read_rgba(BLOCK/"tech_steel.png")
assert max(pixel(steel,x,y)[0] for x in range(64) for y in range(64))<90
assert pixel(read_rgba(BLOCK/"corner_neutral.png")[2],0,0)==(48,55,58,255)

for filename in ("tech_furnace_base.json","tech_furnace_base_animated.json","tech_furnace_base_neutral.json"):
    model=json.loads((ASSETS/"models/block"/filename).read_text()); elements=model["elements"]
    assert sum(e.get("name","").startswith("vertex126_") and e.get("name","").endswith("_body") for e in elements)==8
    down=[e for e in elements if abs(e["from"][1]+.01)<1e-8 and abs(e["to"][1]+.009)<1e-8 and any(f.get("texture")=="#down" for f in e.get("faces",{}).values())]
    walls=[e for e in elements if abs(e["from"][1]+.01)<1e-8 and abs(e["to"][1])<1e-8 and any(f.get("texture")=="#wall" for f in e.get("faces",{}).values())]
    assert (len(down),len(walls))==(160,608),(filename,len(down),len(walls))
    assert not any(abs(e["from"][1]+.625)<1e-8 and any(f.get("texture") in ("#down","#wall") for f in e.get("faces",{}).values()) for e in elements)

main=(ROOT/"src/main/java/br/com/petrick/fornalhastech/client/TechFurnaceScreen.java").read_text()
assert "Math.round((14 - font.lineHeight) / 2.0F);" in main
custom=(ROOT/"src/main/java/br/com/petrick/fornalhastech/client/TechFurnaceCustomizationScreen.java").read_text()
assert "RETURN_WIDTH=11, RETURN_HEIGHT=11" in custom and "pose.scale(0.75F,0.75F,1.0F)" in custom
assert "menu.getAccentColor()" in custom and "0xFFC6C6C6" in custom
assert "mod_version=1.0.32" in (ROOT/"gradle.properties").read_text()
print("1.0.32 visual checks passed; hot flame centroids:",centres)
