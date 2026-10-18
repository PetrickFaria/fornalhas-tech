"""Apply supplied 64px faces to the existing relief height field; close all transitions.
Inputs: pristine 1.0.21 models, six supplied PNGs. No gameplay/GUI edits.
"""
from pathlib import Path
import json,copy,sys,argparse
import numpy as np
from PIL import Image

from build_relief_1_0_21 import rectangles,point,quad,uvpoint
parser=argparse.ArgumentParser();parser.add_argument('--baseline',required=True);parser.add_argument('--faces',required=True);parser.add_argument('--output',default='.');args=parser.parse_args()
res=Path('src/main/resources/assets/fornalhastech');src=Path(args.baseline)/res;dst=Path(args.output)/res;(dst/'models/block').mkdir(parents=True,exist_ok=True);(dst/'textures/block').mkdir(parents=True,exist_ok=True)
old=json.loads((src/'models/block/tech_furnace_base.json').read_text());frames=copy.deepcopy(old['elements'][13:33]);faces=['north','south','west','east','up','down'];directions={'north':['east','west','up','down'],'south':['west','east','up','down'],'west':['north','south','up','down'],'east':['south','north','up','down'],'up':['west','east','north','south'],'down':['west','east','south','north']}
textures={};masks={};heights={};report={}
for face in faces:
 p=next(Path(args.faces).glob('*_'+face+'.png'));a=np.array(Image.open(p).convert('RGBA'));assert a.shape==(64,64,4)
 red=np.all(a[:,:,:3]==[237,28,36],axis=2);light=np.all(a[:,:,:3]==155,axis=2);a[light,:3]=187
 structural=a.copy();structural[red]=[154,166,170,255]
 mask=np.zeros_like(a);mask[red]=[255,255,255,255]
 Image.fromarray(structural).save(dst/f'textures/block/tech_base_flat_{face}.png');Image.fromarray(mask).save(dst/f'textures/block/tech_base_flat_{face}_emissive.png')
 masks[face]=np.repeat(np.repeat(red,4,0),4,1);h=np.zeros((256,256))
 for e in old['elements']:
  name=e.get('name','')
  if name not in ['relief121_'+face+'_raised','relief121_'+face+'_recess']:continue
  fd=e['faces'][face];u,v,U,V=fd['uv'];axis={'north':2,'south':2,'east':0,'west':0,'up':1,'down':1}[face];coordinate=e['from'][axis];height=(-coordinate if face in ['north','west','down'] else coordinate-16)
  h[round(v*16):round(V*16),round(u*16):round(U*16)]=height
 h[masks[face]]=-.372
 heights[face]=h
 textures[face]='fornalhastech:block/tech_base_flat_'+face;textures[face+'_emissive']=textures[face]+'_emissive'
 report[face]={'lightened_pixels':int(light.sum()),'emissive_pixels':int(red.sum()),'heights':np.unique(h).tolist()}
textures['metal']=old['textures']['metal'];textures['particle']=textures['north'];textures['wall']='fornalhastech:block/tech_base_channel_wall'
Image.new('RGBA',(1,1),(52,52,52,255)).save(dst/'textures/block/tech_base_channel_wall.png')
def solid_quad(points,normal,texture,uv,name):
 e=quad(points,normal,texture,uv,name);axis={'north':2,'south':2,'west':0,'east':0,'up':1,'down':1}[normal]
 # Give the element a finite inward thickness, without adding overlapping faces.
 if normal in ['north','west','down']:e['to'][axis]+=.001
 else:e['from'][axis]-=.001
 return e
for neutral in [False,True]:
 elements=copy.deepcopy(frames)
 elements.append({'name':'closed_internal_body','from':[.5,.5,.5],'to':[15.5,15.5,15.5],'faces':{f:{'texture':'#wall','uv':[0,0,16,16]} for f in faces}})
 for face,h in heights.items():
  for depth in np.unique(h):
   for emissive in [False,True]:
    select=(h==depth)&(masks[face] if emissive else ~masks[face])
    for x,y,X,Y in rectangles(select):
     uv=[x/16,y/16,X/16,Y/16];u,v,U,V=uv;e=solid_quad([point(face,a,b,float(depth)) for a,b in [(u,v),(U,v),(U,V),(u,V)]],face,'#'+face+('_emissive' if emissive and not neutral else ''),uv,'closed122_'+face)
     if emissive and not neutral:e['shade']=False;e['faces'][face].update(tintindex=0,emissivity=15)
     elements.append(e)
  # Emit one wall for every height discontinuity, from higher to lower surface.
  for side in range(4):
   neighbor=np.zeros_like(h)
   if side==0:neighbor[:,1:]=h[:,:-1]
   if side==1:neighbor[:,:-1]=h[:,1:]
   if side==2:neighbor[1:,:]=h[:-1,:]
   if side==3:neighbor[:-1,:]=h[1:,:]
   for high in np.unique(h):
    for low in np.unique(neighbor):
     if high<=low:continue
     edges=(h==high)&(neighbor==low)
     if side<2:
      for x in np.where(edges.any(axis=0))[0]:
       for y,_,Y,_ in rectangles(edges[:,x:x+1].T):
        u=(x+(side==1))/16;v=y/16;V=Y/16
        elements.append(solid_quad([point(face,u,v,float(low)),point(face,u,V,float(low)),point(face,u,V,float(high)),point(face,u,v,float(high))],directions[face][side],'#wall',[0,0,16,16],'closed122_channel_wall'))
     else:
      for y in np.where(edges.any(axis=1))[0]:
       for x,_,X,_ in rectangles(edges[y:y+1,:]):
        v=(y+(side==3))/16;u=x/16;U=X/16
        elements.append(solid_quad([point(face,u,v,float(low)),point(face,U,v,float(low)),point(face,U,v,float(high)),point(face,u,v,float(high))],directions[face][side],'#wall',[0,0,16,16],'closed122_channel_wall'))
 model=copy.deepcopy(old);model['textures']=textures;model['elements']=elements
 name='tech_furnace_base'+('_neutral' if neutral else '')
 (dst/f'models/block/{name}.json').write_text(json.dumps(model,separators=(',',':'))+'\n');report[name]={'elements':len(elements)}
(Path(args.output)/'validation.json').write_text(json.dumps(report,indent=2));print(json.dumps(report))
