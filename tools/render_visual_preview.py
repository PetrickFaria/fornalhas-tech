"""Diagnostic CPU orthographic rasterizer, not a Minecraft visual test.
Reads actual JSON faces/UVs and PNGs; nearest-neighbor sampling and simple lighting.
"""
from pathlib import Path
import numpy as np,json
from PIL import Image
NORMALS={'north':(0,0,-1),'south':(0,0,1),'west':(-1,0,0),'east':(1,0,0),'up':(0,1,0),'down':(0,-1,0)}
def vertices(e,f):
 x,y,z=e['from'];X,Y,Z=e['to']
 return np.array({'north':[(X,Y,z),(X,y,z),(x,y,z),(x,Y,z)],'south':[(x,Y,Z),(x,y,Z),(X,y,Z),(X,Y,Z)],'west':[(x,Y,z),(x,y,z),(x,y,Z),(x,Y,Z)],'east':[(X,Y,Z),(X,y,Z),(X,y,z),(X,Y,z)],'up':[(x,Y,z),(x,Y,Z),(X,Y,Z),(X,Y,z)],'down':[(x,y,Z),(x,y,z),(X,y,z),(X,y,Z)]}[f],dtype=float)
def render(root,direction,color=(232,62,62)):
 root=Path(root);m=json.loads((root/'models/block/tech_furnace_base.json').read_text());cache={};W=H=440
 eye=np.array(direction,dtype=float);eye/=np.linalg.norm(eye);right=np.cross([0,1,0],eye);right/=np.linalg.norm(right);up=np.cross(eye,right)
 output=np.full((H,W,3),[112,117,121],dtype=np.uint8);depth=np.full((H,W),-1e9)
 for e in m['elements']:
  for face,fd in e['faces'].items():
   normal=np.array(NORMALS[face]);
   if normal@eye<=0:continue
   key=fd['texture']
   while key.startswith('#'):key=m['textures'][key[1:]]
   if key not in cache:cache[key]=np.array(Image.open(root/('textures/'+key.split(':')[1]+'.png')).convert('RGBA'))
   pixels=cache[key];v=vertices(e,face)-8;projected=np.array([v@right*15+W/2,H/2-v@up*15,v@eye]).T
   u0,v0,u1,v1=fd.get('uv',[0,0,16,16]);uv=np.array([[u0,v0],[u0,v1],[u1,v1],[u1,v0]])
   light=1 if fd.get('emissivity') else .62+.38*max(0,normal@np.array([-.35,.8,-.48]))
   for ids in [(0,1,2),(0,2,3)]:
    a,b,c=projected[list(ids)];tx=uv[list(ids)];xmin=max(0,int(np.floor(min(a[0],b[0],c[0]))));xmax=min(W-1,int(np.ceil(max(a[0],b[0],c[0]))));ymin=max(0,int(np.floor(min(a[1],b[1],c[1]))));ymax=min(H-1,int(np.ceil(max(a[1],b[1],c[1]))))
    if xmax<xmin or ymax<ymin:continue
    xx,yy=np.meshgrid(np.arange(xmin,xmax+1)+.5,np.arange(ymin,ymax+1)+.5)
    den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
    if abs(den)<1e-10:continue
    wa=((b[1]-c[1])*(xx-c[0])+(c[0]-b[0])*(yy-c[1]))/den;wb=((c[1]-a[1])*(xx-c[0])+(a[0]-c[0])*(yy-c[1]))/den;wc=1-wa-wb
    coords=wa[:,:,None]*tx[0]+wb[:,:,None]*tx[1]+wc[:,:,None]*tx[2];ux=np.clip((coords[:,:,0]/16*pixels.shape[1]).astype(int),0,pixels.shape[1]-1);vy=np.clip((coords[:,:,1]/16*pixels.shape[0]).astype(int),0,pixels.shape[0]-1);rgba=pixels[vy,ux];zz=wa*a[2]+wb*b[2]+wc*c[2]
    active=(wa>=-1e-8)&(wb>=-1e-8)&(wc>=-1e-8)&(rgba[:,:,3]>127)&(zz>depth[ymin:ymax+1,xmin:xmax+1]+1e-7)
    rgb=rgba[:,:,:3].astype(float)
    if fd.get('tintindex')==0:rgb*=np.array(color)/255
    output[ymin:ymax+1,xmin:xmax+1][active]=np.clip(rgb*light,0,255).astype(np.uint8)[active];depth[ymin:ymax+1,xmin:xmax+1][active]=zz[active]
 return Image.fromarray(output)
