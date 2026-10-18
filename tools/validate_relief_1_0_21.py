"""Targeted validation against a pristine 1.0.20 checkout/resources directory."""
import argparse,json,hashlib
from pathlib import Path
import numpy as np
from PIL import Image
from build_relief_1_0_21 import point,uvpoint

def validate(root,baseline,vanilla):
 res=Path('src/main/resources/assets/fornalhastech');src=baseline/res;dest=root/res
 for name in ['tech_furnace_base','tech_furnace_base_neutral']:
  a=json.loads((src/f'models/block/{name}.json').read_text());b=json.loads((dest/f'models/block/{name}.json').read_text());count=len(a['elements'])
  assert b['elements'][1:count]==a['elements'][1:]
  assert {k:v for k,v in a['elements'][0]['faces'].items() if k!='north'}==b['elements'][0]['faces']
  assert all(b['textures'][k]==v for k,v in a['textures'].items())
  caps=[e for e in b['elements'][count:] if not e['name'].endswith('wall')]
  sums={}
  for e in b['elements'][count:]:
   assert all(-16<=v<=32 for v in e['from']+e['to'])
   assert all(x<=y for x,y in zip(e['from'],e['to']))
   assert sum(x==y for x,y in zip(e['from'],e['to']))==1
   for f in e['faces'].values():assert 'tintindex' not in f and 'emissivity' not in f
  for e in caps:
   face=next(iter(e['faces']));uv=e['faces'][face]['uv'];sums.setdefault(e['name'],0);sums[e['name']]+=(uv[2]-uv[0])*(uv[3]-uv[1])*16
   axis={'west':0,'east':0,'down':1,'up':1,'north':2,'south':2}[face]
   if e['name'].endswith('recess'):assert e['from'][axis]==.372
   if e['name'].endswith('raised'):
    h=.625 if face=='down' else .125;assert e['from'][axis]==point(face,0,0,h)[axis]
    # No intersection with protected frame solids; touching boundaries is allowed.
    for frame in a['elements'][-20:]:
     tang=[i for i in range(3) if i!=axis]
     assert not(frame['from'][axis]<e['from'][axis]<frame['to'][axis] and all(max(frame['from'][i],e['from'][i])<min(frame['to'][i],e['to'][i]) for i in tang))
    im=np.array(Image.open(src/f'textures/block/tech_base_flat_{face}.png').convert('RGBA'))
    for v in np.arange(uv[1]+.03125,uv[3],.0625):
     for u in np.arange(uv[0]+.03125,uv[2],.0625):
      assert tuple(im[int(v*4),int(u*4)])==((0,0,0,255) if face=='down' else (52,52,52,255))
   # Inverse UV mapping proves neither mirroring nor rotation has been introduced.
   for u,v in [(uv[0],uv[1]),(uv[2],uv[3])]:assert np.allclose(uvpoint(face,point(face,u,v,0)),(u,v))
  assert abs(sums['relief121_north_recess']-756)<1e-8
  assert abs(sums['relief121_north_surface']+sums['relief121_north_recess']-4096)<1e-8
  print(name+': protected original geometry exact; new materials, dimensions, UV, frame exclusion and front coverage PASS')
 for p in (src/'textures/block').glob('*.png'):
  assert p.read_bytes()==(dest/'textures/block'/p.name).read_bytes()
 print('All original block PNGs byte-identical; emissive geometry and textures unchanged')
 a=np.array(Image.open(vanilla).convert('RGBA'));b=np.array(Image.open(dest/'textures/gui/furnace_indicators.png').convert('RGBA'));rects=[(56,36,70,50),(79,34,103,50),(176,0,190,14),(176,14,200,30)]
 expected=np.zeros_like(a);removed=0
 for x,y,X,Y in rects:
  part=a[y:Y,x:X].copy();bg=np.all(part[:,:,:3]==198,axis=2)&(part[:,:,3]==255);removed+=int(bg.sum());part[bg,3]=0;expected[y:Y,x:X]=part
 assert np.array_equal(b,expected);assert removed==738
 print('GUI: exact original sprites, positions and 1:1 dimensions; only 738 gray background alpha pixels removed')

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--baseline',required=True);p.add_argument('--root',default='.');p.add_argument('--vanilla',required=True);a=p.parse_args();validate(Path(a.root),Path(a.baseline),Path(a.vanilla))
