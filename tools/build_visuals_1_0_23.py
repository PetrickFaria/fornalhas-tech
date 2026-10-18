"""One-shot visual refinement from pristine 1.0.22. Requires Pillow/numpy/scipy.
Never run against already reduced 1.0.23 models. Asset generation only, not runtime.
"""
from pathlib import Path
import json,copy,argparse
from collections import deque
import numpy as np
from PIL import Image
from scipy.ndimage import label, distance_transform_edt
p=argparse.ArgumentParser();p.add_argument('--baseline',required=True);p.add_argument('--metal',required=True);p.add_argument('--output',default='.');args=p.parse_args()
r=Path('src/main/resources/assets/fornalhastech');src=Path(args.baseline)/r;dst=Path(args.output)/r
(dst/'models/block').mkdir(parents=True,exist_ok=True);(dst/'textures/block').mkdir(parents=True,exist_ok=True);(dst/'blockstates').mkdir(exist_ok=True)
# Scale transverse axes around their original geometric edge, keeping length unchanged.
for name in ['tech_furnace_base','tech_furnace_base_neutral']:
 m=json.loads((src/f'models/block/{name}.json').read_text())
 assert m['elements'][0]['from']==[.25,-1.25,-1.25], 'Requires pristine 1.0.22 baseline'
 for i,e in enumerate(m['elements'][:20]):
  lengths=np.array(e['to'])-e['from'];longaxis=int(np.argmax(lengths)) if i<12 else -1
  for axis in range(3):
   if axis==longaxis:continue
   center=0 if (e['from'][axis]+e['to'][axis])/2<8 else 16
   for key in ['from','to']:e[key][axis]=center+(e[key][axis]-center)*.75
 (dst/f'models/block/{name}.json').write_text(json.dumps(m,separators=(',',':'))+'\n')
 if name=='tech_furnace_base':
  world=copy.deepcopy(m);world['elements']=[e for e in world['elements'] if not any(f.get('tintindex')==0 for f in e['faces'].values())]
  (dst/'models/block/tech_furnace_base_animated.json').write_text(json.dumps(world,separators=(',',':'))+'\n')
bs=json.loads((src/'blockstates/tech_furnace.json').read_text())
for value in bs['variants'].values():
 for variant in value if isinstance(value,list) else [value]:
  if variant['model']=='fornalhastech:block/tech_furnace_base':variant['model']='fornalhastech:block/tech_furnace_base_animated'
(dst/'blockstates/tech_furnace.json').write_text(json.dumps(bs,separators=(',',':'))+'\n')
material_image=Image.open(args.metal).convert('L').resize((64,64),Image.Resampling.NEAREST)
material_image.save(Path(args.output)/'docs/1.0.23-metal-source.png')
material=np.array(material_image,dtype=float)
# Limited, quantized contrast gives coherent brushed metal instead of noisy faces.
material=np.clip(np.round((material-material.mean())/4)*2,-10,10)
Image.fromarray(np.uint8(material+128)).save(Path(args.output)/'docs/1.0.23-metal-material.png')
faces=['north','south','west','east','up','down'];frames=[np.zeros((320,64,4),np.uint8) for _ in range(5)]
for index,face in enumerate(faces):
 a=np.array(Image.open(src/f'textures/block/tech_base_flat_{face}.png').convert('RGBA'));out=a.copy()
 for gray,contrast in [(187,1),(52,.6)]:
  mask=np.all(a[:,:,:3]==gray,axis=2)&(a[:,:,3]==255)
  # One-pixel edge highlight/shadow follows the EXISTING panel contour only.
  above=np.zeros_like(mask);above[1:]=mask[:-1]
  below=np.zeros_like(mask);below[:-1]=mask[1:]
  bevel=(mask&~above)*7-(mask&~below)*9
  value=np.clip(gray+material*contrast+bevel,0,255).astype(np.uint8)
  for channel in range(3):out[:,:,channel][mask]=value[mask]
 Image.fromarray(out).save(dst/f'textures/block/tech_base_flat_{face}.png')
 if face=='down':continue
 mask=np.array(Image.open(src/f'textures/block/tech_base_flat_{face}_emissive.png').convert('RGBA'))[:,:,3]>0
 labels,count=label(mask);distance=np.zeros((64,64));depth=distance_transform_edt(mask)
 # Shortest-path wavefronts follow connected channel pixels around turns/branches.
 for component in range(1,count+1):
  positions=np.argwhere(labels==component);start=tuple(positions[0]);seen={start};queue=deque([start])
  while queue:
   y,x=queue.popleft()
   for dy,dx in [(0,1),(1,0),(0,-1),(-1,0)]:
    yy,xx=y+dy,x+dx
    if 0<=yy<64 and 0<=xx<64 and mask[yy,xx] and (yy,xx) not in seen:
     seen.add((yy,xx));distance[yy,xx]=distance[y,x]+1;queue.append((yy,xx))
 for frame in range(5):
  wave=(1+np.cos(2*np.pi*(distance/24-frame/5)))/2
  intensity=np.clip(np.round((168+76*wave+11*np.minimum(depth/3,1))/3)*3,0,255).astype(np.uint8)
  tile=frames[frame][index*64:(index+1)*64]
  for c in range(3):tile[:,:,c][mask]=intensity[mask]
  tile[:,:,3][mask]=255
for frame,a in enumerate(frames):
 Image.fromarray(a).save(dst/f'textures/block/energy_flow_{frame}.png')
 (dst/f'textures/block/energy_flow_{frame}.png.mcmeta').write_text('{"texture":{"blur":false,"clamp":true}}\n')
print('Generated: 20 frame elements reduced once, 6 metal faces, 5 neutral animation sheets; static item and neutral models retained.')
