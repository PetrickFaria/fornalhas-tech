from pathlib import Path
from fractions import Fraction as F
import json, argparse
parser=argparse.ArgumentParser();parser.add_argument("--baseline",default="base126");parser.add_argument("--root",default="work126");args=parser.parse_args()
from collections import Counter
base=Path(args.baseline)/'src/main/resources/assets/fornalhastech/models/block';root=Path(args.root)/'src/main/resources/assets/fornalhastech/models/block'
holes=json.loads((Path(args.root)/'docs/1.0.26-corner-geometry.json').read_text())
assert len(holes)==24 and Counter(h['corner'] for h in holes)=={i:3 for i in range(8)}
for variant in ['tech_furnace_base','tech_furnace_base_neutral','tech_furnace_base_animated']:
 old=json.loads((base/(variant+'.json')).read_text());new=json.loads((root/(variant+'.json')).read_text())
 # No geometry from the six main faces or internal closing body was touched.
 tail=[e for e in new['elements'] if not e.get('name','').startswith('vertex126_')]
 assert tail==old['elements'][20:]
 bodies=[e for e in new['elements'] if e.get('name','').endswith('_body') and e.get('name','').startswith('vertex126_')]
 assert len(bodies)==8
 for i,body in enumerate(bodies):
  assert body['from']==old['elements'][12+i]['from'] and body['to']==old['elements'][12+i]['to']
  assert len(body['faces'])==3
 caps=[e for e in new['elements'] if e.get('name','').endswith('_cap') and e.get('name','').startswith('vertex126_')]
 assert len(caps)==(0 if variant.endswith('animated') else 24)
 for h in holes:
  i=h['corner'];face=h['face'];original=old['elements'][12+i];prefix=f'vertex126_{i}_{face}_'
  rim=[e for e in new['elements'] if e.get('name','').startswith(prefix+'rim_')]
  walls=[e for e in new['elements'] if e.get('name','').startswith(prefix+'wall_')]
  assert len(rim)==4 and len(walls)==4
  axis={'west':0,'east':0,'down':1,'up':1,'north':2,'south':2}[face];sign=-1 if face in ('west','down','north') else 1
  surface=F(str(original['from' if sign<0 else 'to'][axis]));cap=h['cap'];size=F(str(original['to'][0]))-F(str(original['from'][0]))
  assert size==F('1.96875')
  for j in range(3):
   lo=F(str(cap['from'][j]));hi=F(str(cap['to'][j]))
   if j==axis: assert surface-lo==sign*F('.024609375') and lo==hi
   else:
    assert hi-lo==size/10
    assert (hi+lo)/2==(F(str(original['from'][j]))+F(str(original['to'][j])))/2
  # Rim area + aperture = exactly the full original face area.
  area=F(0)
  tangents=[a for a in range(3) if a!=axis]
  for e in rim:
   area+=(F(str(e['to'][tangents[0]]))-F(str(e['from'][tangents[0]])))*(F(str(e['to'][tangents[1]]))-F(str(e['from'][tangents[1]])))
  assert area+(size/10)**2==size**2
  for wall in walls:
   assert F(str(wall['to'][axis]))-F(str(wall['from'][axis]))==F('.024609375')
   assert len(wall['faces'])==1 and 'emissivity' not in str(wall)
 assert all(e not in new['elements'] for e in old['elements'][:12])
print('PASS: 12 bars removed; 8 identical cube envelopes; exactly 24 centered 10%-side recesses, depth .024609375; four rims/four walls/sealed caps; all other geometry preserved.')
