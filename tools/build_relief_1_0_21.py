"""One-shot geometry refinement from the pinned 1.0.20 baseline, never cumulative.
Run from repository root with --baseline pointing to pristine 1.0.20 resources.
Requires Pillow, numpy, scipy. Does not modify block PNGs.
"""
from pathlib import Path
import json,copy,argparse
import numpy as np
from PIL import Image
from scipy.ndimage import label

def rectangles(mask):
    a=mask.copy();out=[]
    while a.any():
        y,x=np.argwhere(a)[0];x1=x
        while x1<a.shape[1] and a[y,x1]:x1+=1
        y1=y+1
        while y1<a.shape[0] and a[y1:y1+1,x:x1].all():y1+=1
        a[y:y1,x:x1]=False;out.append((int(x),int(y),int(x1),int(y1)))
    return out

def point(face,u,v,h):
    return {'north':(16-u,16-v,-h),'south':(u,16-v,16+h),'west':(-h,16-v,u),
            'east':(16+h,16-v,16-u),'up':(u,16+h,v),'down':(u,-h,16-v)}[face]

def uvpoint(face,p):
    x,y,z=p
    return {'north':(16-x,16-y),'south':(x,16-y),'west':(z,16-y),
            'east':(16-z,16-y),'up':(x,z),'down':(x,16-z)}[face]

def quad(points,face,texture,uv,name):
    return {'name':name,'from':[round(min(p[a] for p in points),6) for a in range(3)],
            'to':[round(max(p[a] for p in points),6) for a in range(3)],
            'faces':{face:{'texture':texture,'uv':[round(v,6) for v in uv]}}}

def caps(mask,face,h,texture,name):
    scale=16/mask.shape[0];out=[]
    for x,y,x1,y1 in rectangles(mask):
        u,v,u1,v1=[n*scale for n in (x,y,x1,y1)]
        out.append(quad([point(face,a,b,h) for a,b in [(u,v),(u1,v),(u1,v1),(u,v1)]],face,texture,[u,v,u1,v1],name))
    return out

def walls(mask,face,h,material,name,occluded=None):
    # Only the perimeter is emitted, never the internal boundaries of merged caps.
    step=16/mask.shape[0];out=[]
    dirs={'north':['east','west','up','down'],'south':['west','east','up','down'],
          'west':['north','south','up','down'],'east':['south','north','up','down'],
          'up':['west','east','north','south'],'down':['west','east','south','north']}[face]
    walluv=[1,1,1.25,1.25] if material=='dark' else [0,0,.25,.25]
    for side in range(4):
        adjacent=np.zeros_like(mask)
        if side==0:adjacent[:,1:]=mask[:,:-1]
        if side==1:adjacent[:,:-1]=mask[:,1:]
        if side==2:adjacent[1:,:]=mask[:-1,:]
        if side==3:adjacent[:-1,:]=mask[1:,:]
        edge=mask & ~adjacent
        if occluded is not None:
            blocked=np.zeros_like(mask)
            if side==0:blocked[:,1:]=occluded[:,:-1]
            if side==1:blocked[:,:-1]=occluded[:,1:]
            if side==2:blocked[1:,:]=occluded[:-1,:]
            if side==3:blocked[:-1,:]=occluded[1:,:]
            edge &= ~blocked
        if side<2:
            for x in range(edge.shape[1]):
                for y,_,y1,_ in rectangles(edge[:,x:x+1].T):
                    u=(x+(side==1))*step;v=y*step;v1=y1*step
                    pts=[point(face,u,v,0),point(face,u,v1,0),point(face,u,v1,h),point(face,u,v,h)]
                    normal=dirs[side] if h>0 else dirs[side^1]
                    out.append(quad(pts,normal,'#relief_material',walluv,name))
        else:
            for y in range(edge.shape[0]):
                for x,_,x1,_ in rectangles(edge[y:y+1,:]):
                    v=(y+(side==3))*step;u=x*step;u1=x1*step
                    pts=[point(face,u,v,0),point(face,u1,v,0),point(face,u1,v,h),point(face,u,v,h)]
                    normal=dirs[side] if h>0 else dirs[side^1]
                    out.append(quad(pts,normal,'#relief_material',walluv,name))
    return out

def clip_frames(mask,face,h,frames):
    a=np.repeat(np.repeat(mask,4,axis=0),4,axis=1)
    axis={'west':0,'east':0,'down':1,'up':1,'north':2,'south':2}[face]
    lo,hi=sorted([point(face,0,0,0)[axis],point(face,0,0,h)[axis]])
    for e in frames:
        if e['to'][axis]<=lo or e['from'][axis]>=hi:continue
        ps=[uvpoint(face,(x,y,z)) for x in [e['from'][0],e['to'][0]] for y in [e['from'][1],e['to'][1]] for z in [e['from'][2],e['to'][2]]]
        x0,y0=[max(0,round(min(p[i] for p in ps)*16)) for i in (0,1)]
        x1,y1=[min(256,round(max(p[i] for p in ps)*16)) for i in (0,1)]
        if x1>x0 and y1>y0:a[y0:y1,x0:x1]=False
    return a

def build(root,baseline):
    res=Path('src/main/resources/assets/fornalhastech');src=baseline/res;dest=root/res
    source=json.loads((src/'models/block/tech_furnace_base.json').read_text())
    frames=source['elements'][-20:];masks={};negative=None
    for face in ['north','south','west','east','up','down']:
        im=np.array(Image.open(src/f'textures/block/tech_base_flat_{face}.png').convert('RGBA'))
        dark=(im[:,:,0]==52)&(im[:,:,3]==255)
        groups,n=label(dark,np.ones((3,3),dtype=int));outer=groups[1,1]
        inner=dark&(groups!=outer)
        if face=='north':
            components=sorted([(int((groups==i).sum()),i) for i in range(1,n+1) if i!=outer],reverse=True)
            assert [c[0] for c in components[:2]]==[378,378]
            negative=np.isin(groups,[c[1] for c in components[:2]])
            mask=inner&~negative
        elif face=='down':mask=(im[:,:,0]==0)&(im[:,:,3]==255)
        elif face=='up':mask=dark
        else:mask=inner
        masks[face]=clip_frames(mask,face,.625 if face=='down' else .125,frames)
    additions=caps(~negative,'north',0,'#north','relief121_north_surface')
    additions+=caps(negative,'north',-.372,'#north','relief121_north_recess')
    additions+=walls(negative,'north',-.372,'dark','relief121_north_recess_wall')
    stats={}
    for face,mask in masks.items():
        h=.625 if face=='down' else .125
        parts=caps(mask,face,h,'#'+face,'relief121_'+face+'_raised')
        parts+=walls(mask,face,h,'black' if face=='down' else 'dark','relief121_'+face+'_wall',~clip_frames(np.ones((64,64),dtype=bool),face,h,frames))
        additions+=parts;stats[face]={'height':h,'area_pixels':float(mask.sum()/16),'elements':len(parts)}
    for name in ['tech_furnace_base','tech_furnace_base_neutral']:
        model=json.loads((src/f'models/block/{name}.json').read_text())
        assert not any(e.get('name','').startswith('relief121') for e in model['elements'])
        model['elements'][0]['faces'].pop('north')
        model['textures']['relief_material']='fornalhastech:block/tech_base_flat_down'
        model['elements']+=copy.deepcopy(additions)
        out=dest/f'models/block/{name}.json';out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(model,separators=(',',':'))+'\n')
    return masks,negative,stats

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--baseline',required=True);parser.add_argument('--output',default='.');args=parser.parse_args()
    masks,negative,stats=build(Path(args.output),Path(args.baseline))
    print(json.dumps({'raised':stats,'front_recess_pixels':int(negative.sum()),'front_depth':.372}))
