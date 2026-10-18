from pathlib import Path
from PIL import Image
root=Path(__file__).parents[1]
old=Image.open('base124/src/main/resources/assets/fornalhastech/textures/gui/tech_furnace_customization.png').convert('RGBA')
new=Image.open(root/'src/main/resources/assets/fornalhastech/textures/gui/tech_furnace_customization.png').convert('RGBA')
assert old.size==new.size==(176,89)
changed={(x,y) for y in range(89) for x in range(176) if old.getpixel((x,y))!=new.getpixel((x,y))}
assert changed and all(157<=x<167 and 9<=y<17 for x,y in changed)
rows=['1111110','1100011','1100011','1111110','1100000','1100000','1100000']
ink=(55,55,55,255);bg=(198,198,198,255)
for y in range(9,17):
 for x in range(157,167):
  dx=x-158;dy=y-9
  expected=ink if 0<=dx<7 and 0<=dy<7 and rows[dy][dx]=='1' else bg
  assert new.getpixel((x,y))==expected
print(f'PASS: customization button glyph is crisp 7x7 P; {len(changed)} pixels changed, all inside glyph area.')
