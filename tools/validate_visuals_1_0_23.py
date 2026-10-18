from pathlib import Path
import argparse,json,itertools
import numpy as np
from PIL import Image
p=argparse.ArgumentParser();p.add_argument('--baseline',required=True);p.add_argument('--root',default='.');args=p.parse_args();res=Path('src/main/resources/assets/fornalhastech');src=Path(args.baseline)/res;dst=Path(args.root)/res
for name in ['tech_furnace_base','tech_furnace_base_neutral']:
 old=json.loads((src/f'models/block/{name}.json').read_text());new=json.loads((dst/f'models/block/{name}.json').read_text());assert new['elements'][20:]==old['elements'][20:]
 corners=set()
 for i,(a,b) in enumerate(zip(old['elements'][:20],new['elements'][:20])):
  lengths=np.array(a['to'])-a['from'];axis=int(np.argmax(lengths)) if i<12 else -1
  for dim in range(3):
   if dim==axis:assert b['from'][dim]==a['from'][dim] and b['to'][dim]==a['to'][dim]
   else:
    center=0 if (a['from'][dim]+a['to'][dim])/2<8 else 16
    for key in ['from','to']:assert b[key][dim]==center+(a[key][dim]-center)*.75
  if i>=12:corners.add(tuple((x+y)/2 for x,y in zip(b['from'],b['to'])))
 assert corners==set(itertools.product([0,16],repeat=3))
 # Each unchanged-length bar intersects its two end corner cuboids.
 for bar in new['elements'][:12]:
  assert sum(all(min(bar['to'][d],c['to'][d])>max(bar['from'][d],c['from'][d]) for d in range(3)) for c in new['elements'][12:20])==2
 if name=='tech_furnace_base':
  world=json.loads((dst/'models/block/tech_furnace_base_animated.json').read_text())
  assert world['elements']==[e for e in new['elements'] if not any(f.get('tintindex')==0 for f in e['faces'].values())]
frames=[np.array(Image.open(dst/f'textures/block/energy_flow_{i}.png').convert('RGBA')) for i in range(5)]
for f in frames:assert f.shape==(320,64,4)
for n,face in enumerate(['north','south','west','east','up','down']):
 old=np.array(Image.open(src/f'textures/block/tech_base_flat_{face}.png').convert('RGBA'));new=np.array(Image.open(dst/f'textures/block/tech_base_flat_{face}.png').convert('RGBA'))
 metal=np.all(old[:,:,:3]==187,axis=2)|np.all(old[:,:,:3]==52,axis=2)
 assert np.array_equal(new[~metal],old[~metal]);assert np.array_equal(new[:,:,3],old[:,:,3]);assert not np.array_equal(new[metal],old[metal])
 if face=='down':continue
 mask=np.array(Image.open(src/f'textures/block/tech_base_flat_{face}_emissive.png').convert('RGBA'))[:,:,3]
 for frame in frames:
  tile=frame[n*64:(n+1)*64];assert np.array_equal(tile[:,:,3],mask)
  assert np.array_equal(tile[:,:,0],tile[:,:,1]) and np.array_equal(tile[:,:,0],tile[:,:,2])
 for i in range(5):assert not np.array_equal(frames[i][n*64:(n+1)*64],frames[(i+1)%5][n*64:(n+1)*64])
 # Cyclic interpolation approaches frame 0 continuously from frame 4.
 last=frames[4][n*64:(n+1)*64].astype(float);first=frames[0][n*64:(n+1)*64].astype(float)
 assert np.max(np.abs((last*.000001+first*.999999)-first))<.001
old=json.loads((src/'blockstates/tech_furnace.json').read_text());new=json.loads((dst/'blockstates/tech_furnace.json').read_text());assert old['variants'].keys()==new['variants'].keys()
for key,a in old['variants'].items():
 expected=dict(a)
 if expected['model']=='fornalhastech:block/tech_furnace_base':expected['model']='fornalhastech:block/tech_furnace_base_animated'
 assert new['variants'][key]==expected
print('PASS: exact one-time 25% reduction, 8 centered corners, 12 connected bars, other relief unchanged, masks and contours identical, 6 metal faces, 5 distinct cyclic frames, identical facing rotations and module/neutral variants.')
