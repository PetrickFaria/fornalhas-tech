"""Surgical change from pristine 1.0.25 assets: remove 12 bars, recess 24 corner lights."""
from pathlib import Path
from copy import deepcopy
from PIL import Image
import json, numpy as np
import argparse
parser=argparse.ArgumentParser();parser.add_argument("--baseline",default="base126");parser.add_argument("--output",default="work126");args=parser.parse_args()
PROJECT=Path(args.output)
BASE=Path(args.baseline)/'src/main/resources/assets/fornalhastech'
OUT=PROJECT/'src/main/resources/assets/fornalhastech'
NORMALS={'north':(0,0,-1),'south':(0,0,1),'west':(-1,0,0),'east':(1,0,0),'up':(0,1,0),'down':(0,-1,0)}
def verts(e,f):
 x,y,z=e['from'];X,Y,Z=e['to']
 return np.array({'north':[(X,Y,z),(X,y,z),(x,y,z),(x,Y,z)],'south':[(x,Y,Z),(x,y,Z),(X,y,Z),(X,Y,Z)],'west':[(x,Y,z),(x,y,z),(x,y,Z),(x,Y,Z)],'east':[(X,Y,Z),(X,y,Z),(X,y,z),(X,Y,z)],'up':[(x,Y,z),(x,Y,Z),(X,Y,Z),(X,Y,z)],'down':[(x,y,Z),(x,y,z),(X,y,z),(X,y,Z)]}[f],float)
def clean(v):return [round(float(x),12) for x in v]
def plane(name,points,face,texture,uv=None,emissive=False,tint=False):
 pts=np.array(points);fd={'texture':texture,'uv':uv or [0,0,16,16]}
 if tint:fd['tintindex']=0
 if emissive:fd['emissivity']=15
 e={'name':name,'from':clean(pts.min(axis=0)),'to':clean(pts.max(axis=0)),'faces':{face:fd}}
 if emissive:e['shade']=False
 return e
manifest=[]
for variant in ['tech_furnace_base','tech_furnace_base_neutral','tech_furnace_base_animated']:
 m=json.loads((BASE/'models/block'/f'{variant}.json').read_text());old=m['elements']
 assert len(old)>20 and old[20].get('name')=='closed_internal_body'
 # Precisely identify the 12 longitudinal bars and the eight current 1.96875 corner cubes.
 for e in old[:12]:assert sum(b-a>15 for a,b in zip(e['from'],e['to']))==1
 cubes=old[12:20]
 new=[];holes=[]
 for i,cube in enumerate(cubes):
  lo=np.array(cube['from']);hi=np.array(cube['to']);center=(lo+hi)/2
  assert np.allclose(hi-lo,1.96875) and all(c in (0,16) for c in center)
  external=['west' if center[0]==0 else 'east','down' if center[1]==0 else 'up','north' if center[2]==0 else 'south']
  core=deepcopy(cube);core['name']=f'vertex126_{i}_body'
  for face in external:del core['faces'][face]
  new.append(core)
  for face in external:
   vv=verts(cube,face);p00,p01,p11,p10=vv;u=p10-p00;v=p01-p00;n=np.array(NORMALS[face]);depth=1.96875*.1/8
   def point(a,b,inset=0):return p00+u*a+v*b-n*inset
   for j,(u0,v0,u1,v1) in enumerate([(0,0,1,.45),(0,.55,1,1),(0,.45,.45,.55),(.55,.45,1,.55)]):
    points=[point(u0,v0),point(u0,v1),point(u1,v1),point(u1,v0)]
    new.append(plane(f'vertex126_{i}_{face}_rim_{j}',points,face,'#metal',[round(t*16,12) for t in [u0,v0,u1,v1]]))
   # Four walls face into the open shaft; its bottom is sealed by the colored cap.
   walls=[(point(.45,.45),point(.45,.55),u),
          (point(.55,.45),point(.55,.55),-u),
          (point(.45,.45),point(.55,.45),v),
          (point(.45,.55),point(.55,.55),-v)]
   for j,(a,b,normal) in enumerate(walls):
    wallface=next(f for f,nn in NORMALS.items() if np.dot(normal,nn)>0)
    new.append(plane(f'vertex126_{i}_{face}_wall_{j}',[a,b,b-n*depth,a-n*depth],wallface,'#wall'))
   cap=plane(f'vertex126_{i}_{face}_cap',[point(.45,.45,depth),point(.45,.55,depth),point(.55,.55,depth),point(.55,.45,depth)],face,'#corner_neutral' if variant.endswith('neutral') else '#corner_light',emissive=variant=='tech_furnace_base',tint=variant=='tech_furnace_base')
   holes.append({'corner':i,'face':face,'center':clean(center),'cube_from':cube['from'],'cube_to':cube['to'],'width':.196875,'depth':.024609375,'cap':cap})
   if not variant.endswith('animated'):new.append(cap)
 m['elements']=new+deepcopy(old[20:])
 m['textures']['corner_light']='fornalhastech:block/corner_light'
 m['textures']['corner_neutral']='fornalhastech:block/corner_neutral'
 p=OUT/'models/block'/f'{variant}.json';p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(m,separators=(',',':'))+'\n')
 if variant=='tech_furnace_base_animated':manifest=holes
p=OUT/'textures/block';p.mkdir(parents=True,exist_ok=True)
Image.new('RGBA',(1,1),(255,255,255,255)).save(p/'corner_light.png')
Image.new('RGBA',(1,1),(154,166,170,255)).save(p/'corner_neutral.png')
(PROJECT/'docs/1.0.26-corner-geometry.json').write_text(json.dumps(manifest,indent=2)+'\n')
# Use one existing fully opaque channel texel for all small squares. It shares the same
# five frames, tint, interpolation and per-entity clock; no new animation textures/timers.
frames=np.array([np.asarray(Image.open(BASE/f'textures/block/energy_flow_{i}.png').convert('RGBA')) for i in range(5)])
eligible=np.all(frames[:,:64,:,3]==255,axis=0)
variation=np.ptp(frames[:,:64,:,0].astype(int),axis=0);variation[~eligible]=-1
y,x=np.unravel_index(variation.argmax(),variation.shape)
print('sample channel texel',x,y,'brightness',frames[:,y,x,0].tolist())
java=(Path(args.baseline)/'src/main/java/br/com/petrick/fornalhastech/client/TechFurnaceEnergyRenderer.java').read_text()
arrays=[];normals=[]
for hole in manifest:
 arrays.append('{'+','.join('{'+','.join(f'{a/16:.12f}F' for a in p)+'}' for p in verts(hole['cap'],hole['face']))+'}')
 normals.append('{'+','.join(str(a)+'F' for a in NORMALS[hole['face']])+'}')
fields='''    // Exactly 24 recessed corner caps, generated from the unchanged cube envelopes.
    private static final float[][][] CORNER_VERTICES = {\n        '''+',\n        '.join(arrays)+'''\n    };
    private static final float[][] CORNER_NORMALS = {'''+','.join(normals)+'''};
    private static final float CORNER_U = '''+f'{(x+.5)/64:.12f}F;\n    private static final float CORNER_V = {(y+.5)/320:.12f}F;\n'
java=java.replace('    private static final RenderType[] PASSES',fields+'    private static final RenderType[] PASSES')
needle='''        }
    }

    @Override public int getViewDistance()'''
replacement='''        }
        for (int face = 0; face < CORNER_VERTICES.length; face++) {
            float[] n = CORNER_NORMALS[face];
            for (float[] p : CORNER_VERTICES[face]) {
                out.vertex(pose.pose(), p[0], p[1], p[2]).color(r, g, b, alpha)
                    .uv(CORNER_U, CORNER_V)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(pose.normal(), n[0], n[1], n[2]).endVertex();
            }
        }
    }

    @Override public int getViewDistance()'''
assert needle in java;java=java.replace(needle,replacement)
p=(PROJECT/'src/main/java/br/com/petrick/fornalhastech/client/TechFurnaceEnergyRenderer.java');p.parent.mkdir(parents=True,exist_ok=True);p.write_text(java)
(PROJECT/'gradle.properties').write_text((Path(args.baseline)/'gradle.properties').read_text().replace('mod_version=1.0.25','mod_version=1.0.26'))
