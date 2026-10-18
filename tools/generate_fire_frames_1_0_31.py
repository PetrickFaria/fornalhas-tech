from PIL import Image, ImageDraw
from pathlib import Path
import math

ROOT = Path(__file__).resolve().parent
OUT = ROOT.parent / 'src/main/resources/assets/fornalhastech/textures/block'
W, H = 64, 32
# Five sampled phases: left, near-centre, right, near-centre, easing back left.
SWAY = (-5, -1, 5, 1, -4)
COLORS = {
    'deep': (194, 31, 20, 255),
    'red': (235, 48, 18, 255),
    'orange_dark': (247, 78, 17, 255),
    'orange': (255, 111, 20, 255),
    'gold': (255, 169, 32, 255),
    'yellow': (255, 220, 62, 255),
    'hot': (255, 241, 135, 255),
}

# Stable, irregular flame skyline. Uneven peak spacing makes the sway visible.
# The narrow valleys keep the opaque flame silhouette above 90% of the panel.
def top_y(x):
    peaks = ((1,0), (9,2), (20,0), (31,1), (43,0), (53,2), (62,0))
    return min(height + int(math.ceil(abs(x-center) * 0.70)) for center,height in peaks)

base = Image.new('RGBA', (W, H), (0,0,0,0))
p = base.load()
for y in range(H):
    for x in range(W):
        if y < top_y(x):
            continue
        # Narrow, scattered transparent pinholes: small dark gaps, not large voids.
        if 4 <= y <= 26 and ((x*17 + y*23 + x*y*3) % 181 == 0 or (x*7+y*13) % 317 == 0):
            continue
        ty = top_y(x)
        # The hottest color is concentrated at the base and in several rising tongues.
        core_idx = min(range(5), key=lambda i: abs(x-(6+i*13)))
        core_x = 6 + core_idx*13 + round(math.sin((y+core_idx*3)*0.18)*1.2)
        core_height = (6, 12, 7, 10, 5)[core_idx]
        core_width = max(0.7, 3.2 * (1 - max(0, 25-y)/max(1,25-core_height)))
        in_core = y >= core_height and abs(x-core_x) <= core_width
        # Pixel-art color bands with red tips and orange fire around yellow inner tongues.
        if in_core:
            if y >= 28 and ((x+y) % 7 in (0,1)):
                color = COLORS['hot']
            elif y >= 24 or (x+y*2) % 5 in (0,1):
                color = COLORS['yellow']
            else:
                color = COLORS['gold']
        elif y <= ty + 1:
            color = COLORS['deep'] if (x + y) % 3 == 0 else COLORS['red']
        elif y <= ty + 4 and (x*3+y) % 4 == 0:
            color = COLORS['red']
        elif y >= 28:
            color = COLORS['gold'] if (x+y) % 6 < 3 else COLORS['orange']
        elif y >= 25:
            color = COLORS['orange'] if (x+y*2) % 4 else COLORS['gold']
        elif (x*5 + y*7) % 13 == 0:
            color = COLORS['orange_dark']
        else:
            color = COLORS['orange']
        p[x,y] = color

# Warp each row progressively: tips sway more than the fixed lower 5-pixel base.
def frame_for(sway, phase):
    src = base.load()
    out = Image.new('RGBA', (W,H), (0,0,0,0))
    dst = out.load()
    for y in range(H):
        weight = 0.0 if y >= 27 else ((27-y)/27.0) ** 1.35
        dx = sway * weight
        for x in range(W):
            sx = max(0, min(W-1, int(round(x-dx))))
            dst[x,y] = src[sx,y]
    # Very small frame-specific pixel flickers around the upper tongues, preserving the broad shape.
    # Keep each phase distinct while leaving the base unchanged.
    draw = ImageDraw.Draw(out)
    for k in range(4):
        x = (8 + k*15 + (phase % 2)) % 62 + 1
        y = 5 + ((k*7 + phase*3) % 12)
        if out.getpixel((x,y))[3] and out.getpixel((x,y+1))[3]:
            if (phase + k) % 2 == 0:
                draw.point((x,y), fill=(255, 145, 24, 255))
            else:
                draw.point((x,y), fill=(235, 48, 18, 255))
    return out

frames=[]
for i, sway in enumerate(SWAY):
    im=frame_for(sway,i)
    cover=sum(1 for px in im.getdata() if px[3] > 0)/(W*H)
    if cover < .90:
        raise SystemExit(f'frame {i} coverage too low: {cover:.4%}')
    im.save(OUT/f'fire_panel_{i}.png', optimize=True)
    frames.append(im)
    print(f'frame {i}: sway={sway}px alpha coverage={cover:.2%}')

# Create a clear QA contact sheet: checker-black panel backing and nearest-neighbour zoom.
scale=6; label_h=22
sheet=Image.new('RGB',(W*scale*5+16*6,H*scale+label_h+32),(38,38,38))
d=ImageDraw.Draw(sheet)
for i,im in enumerate(frames):
    x0=16+(W*scale+16)*i; y0=label_h
    backing=Image.new('RGBA',(W,H),(17,17,20,255)); backing.alpha_composite(im)
    enlarged=backing.resize((W*scale,H*scale),Image.Resampling.NEAREST)
    sheet.paste(enlarged.convert('RGB'),(x0,y0))
    d.text((x0+4,4),f'Frame {i+1} | sway {SWAY[i]:+d}px',fill=(240,240,240))
sheet.save(ROOT/'fire-frames-preview.png')
