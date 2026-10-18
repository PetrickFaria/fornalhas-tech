from pathlib import Path
from PIL import Image
import argparse
p=argparse.ArgumentParser();p.add_argument('--baseline',default='base124');p.add_argument('--root',default='work124');a=p.parse_args()
base=Path(a.baseline);root=Path(a.root)
count=0
for face in ['north','south','west','east','up','down']:
 rel=Path('src/main/resources/assets/fornalhastech/textures/block')/f'tech_base_flat_{face}.png'
 old=Image.open(base/rel).convert('RGBA');new=Image.open(root/rel).convert('RGBA')
 assert old.size==new.size==(64,64)
 changed=0
 for x,y in zip(old.getdata(),new.getdata()):
  r,g,b,alpha=x
  target=(r+32,g+32,b+32,alpha) if r==g==b and 10<r<128 else x
  assert y==target
  changed+=x!=y
 assert changed>0
 count+=changed
print(f'PASS: six 64x64 faces; {count} dark-metal pixels brightened by 32; all other pixels and alpha unchanged.')
