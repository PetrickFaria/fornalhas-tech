from pathlib import Path
from PIL import Image
import numpy as np,json,hashlib
import argparse
p=argparse.ArgumentParser();p.add_argument('--baseline',required=True);p.add_argument('--faces',required=True);p.add_argument('--root',default='.');args=p.parse_args()
res=Path('src/main/resources/assets/fornalhastech');root=Path(args.root)/res;old=Path(args.baseline)/res
for face in ['north','south','west','east','up','down']:
 a=np.array(Image.open(next(Path(args.faces).glob('*_'+face+'.png'))).convert('RGBA'));b=np.array(Image.open(root/f'textures/block/tech_base_flat_{face}.png'));m=np.array(Image.open(root/f'textures/block/tech_base_flat_{face}_emissive.png'));red=np.all(a[:,:,:3]==[237,28,36],axis=2);gray=np.all(a[:,:,:3]==155,axis=2)
 assert np.array_equal(a[~(red|gray)],b[~(red|gray)])
 assert (b[gray,:3]==187).all();assert np.array_equal(m[:,:,3]>0,red);assert (b[:,:,3]==255).all()
 if face=='down':assert np.array_equal(b,np.array(Image.open(old/f'textures/block/tech_base_flat_{face}.png').convert('RGBA')))
for name in ['tech_furnace_base','tech_furnace_base_neutral']:
 m=json.loads((root/f'models/block/{name}.json').read_text());o=json.loads((old/'models/block/tech_furnace_base.json').read_text());assert m['elements'][:20]==o['elements'][13:33]
 assert len(m['elements'][20]['faces'])==6
 for e in m['elements']:assert all(b>a for a,b in zip(e['from'],e['to']));assert not any('cullface' in f for f in e['faces'].values())
 for face in ['north','south','west','east','up','down']:
  coverage=np.zeros((256,256),int)
  for e in m['elements']:
   if e.get('name')!='closed122_'+face:continue
   f=e['faces'][face];x,y,X,Y=[round(v*16) for v in f['uv']];coverage[y:Y,x:X]+=1
   assert ('tintindex' not in f) if name.endswith('neutral') or face=='down' else True
  assert (coverage==1).all(),(name,face,np.unique(coverage))
print('PASS: six 64x64 faces; protected pixels and bottom unchanged; exact emissive masks; 20 frame pieces unchanged; finite thickness; six-face closed backing; no cullface; exactly one surface per UV cell on all six sides in both models.')
