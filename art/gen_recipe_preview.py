"""Vorschau der geplanten Crafting-Rezepte (nur Darstellung, keine Spieldaten)."""
from PIL import Image, ImageDraw, ImageFont
import os
TEX = "mod/src/main/resources/assets/scribery/textures/item/"
F = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FB = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
font = ImageFont.truetype(F, 12); small = ImageFont.truetype(F, 10); big = ImageFont.truetype(FB, 15)

# key: (label, colour, texture or None)
AB={'paper': 'Pa', 'ingot': 'Ig', 'nugget': 'Nu', 'furnace': 'Fu', 'pot': 'Po', 'anvil': 'An', 'mold': 'Mo', 'candle': 'Ca', 'slab': 'Sl', 'plank': 'Pl', 'stick': 'St', 'piston': 'Pi', 'lever': 'Le', 'barrel': 'Ba', 'ink': 'Ik', 'feather': 'Fe', 'book': 'Bk', 'ostr': 'Os', 'shard': 'Sh', 'scrap': 'Sc'}
I = {
 "scrap": ("Papyrus scrap", (200,175,110), "papyrus_scrap.png"),
 "paper": ("Paper", (235,235,225), None),
 "shard": ("Amphora shard", (176,98,60), "amphora_shard.png"),
 "ingot": ("Iron ingot", (205,205,205), None),
 "nugget": ("Iron nugget", (170,170,175), None),
 "furnace": ("Furnace", (120,120,120), None),
 "pot": ("Decorated pot", (150,70,50), None),
 "anvil": ("Mini anvil", (70,70,75), None),
 "mold": ("Casting mold", (130,85,60), None),
 "candle": ("Candle", (240,230,200), None),
 "slab": ("Oak slab", (170,135,80), None),
 "plank": ("Oak planks", (185,150,90), None),
 "stick": ("Stick", (140,105,60), None),
 "piston": ("Piston", (150,130,90), None),
 "lever": ("Lever", (110,110,110), None),
 "barrel": ("Barrel", (140,100,60), None),
 "ink": ("Ink sac", (30,30,45), None),
 "feather": ("Feather", (225,225,225), None),
 "book": ("Book", (130,70,50), None),
 "ostr": ("Ostrakon", (190,140,100), "ostrakon.png"),
}
def icon(k, s=40):
    global AB
    lab, col, tex = I[k]
    if tex and os.path.exists(TEX+tex):
        return Image.open(TEX+tex).convert("RGBA").resize((s,s), Image.NEAREST)
    im = Image.new("RGBA",(s,s),(0,0,0,0)); d = ImageDraw.Draw(im)
    d.rounded_rectangle((4,4,s-5,s-5),4,fill=col,outline=tuple(max(0,c-60) for c in col),width=2)
    d.text((s//2,s//2),AB[k],fill=tuple(max(0,c-90) for c in col),font=big,anchor="mm")
    return im

def grid(d, im, x, y, pat, ok_shapeless=False, cell=48):
    d.rectangle((x-6,y-6,x+3*cell+6,y+3*cell+6),fill=(139,139,139),outline=(55,55,55),width=2)
    for r in range(3):
        for c in range(3):
            cx,cy = x+c*cell, y+r*cell
            d.rectangle((cx,cy,cx+cell-3,cy+cell-3),fill=(110,110,110),outline=(55,55,55))
            k = pat[r][c] if r < len(pat) and c < len(pat[r]) else None
            if k:
                im.alpha_composite(icon(k), (cx+2,cy+2))

RECIPES = [
 ("Paper (shapeless)", ["scrap","scrap","scrap"], "paper", 1, True),
 ("Mini anvil", ["III"," n ","nnn"], "anvil", 1, False),
 ("Casting mold", [" s ","s s"," s "], "mold", 1, False),
 ("Minting Table", [" p ","mfa"], "furnace_t", 1, False),
 ("Scribe's Lectern", [" C ","SpS"," P "], "lectern", 1, False),
 ("Printing Press", ["III","PXP","PPP"], "press", 1, False),
 ("Letter Cabinet", [" B ","PPP","T T"], "cabinet", 1, False),
 ("Translator's Desk", ["ifp","PPP","P P"], "desk", 1, False),
 ("Manuscript Archive", ["PPP","ppp","PPP"], "archive", 1, False),
]
KEY = {"I":"ingot","n":"nugget","s":"shard","p":"pot","m":"mold","f":"furnace","a":"anvil","C":"candle","S":"slab",
       "P":"plank","X":"piston","B":"barrel","T":"stick","i":"ink","L":"lever"}
# per-recipe overrides of letters (letters differ in meaning per recipe)
OVR = {
 "Paper (shapeless)": None,
 "Mini anvil": {"I":"ingot","n":"nugget"},
 "Casting mold": {"s":"shard"},
 "Minting Table": {"p":"pot","m":"mold","f":"furnace","a":"anvil"},
 "Scribe's Lectern": {"C":"candle","S":"slab","p":"paper","P":"plank"},
 "Printing Press": {"I":"ingot","P":"plank","X":"piston"},
 "Letter Cabinet": {"B":"barrel","P":"plank","T":"stick"},
 "Translator's Desk": {"i":"ink","f":"feather","p":"paper","P":"plank"},
 "Manuscript Archive": {"P":"plank","p":"paper"},
}
RES = {"paper":("Paper",None),"anvil":("Mini anvil",None),"mold":("Casting mold",None),"furnace_t":("Minting Table",None),
       "lectern":("Scribe's Lectern",None),"press":("Printing Press",None),"cabinet":("Letter Cabinet",None),
       "desk":("Translator's Desk",None),"archive":("Manuscript Archive",None)}
COLS, CW, CH = 3, 440, 290
rows = (len(RECIPES)+COLS-1)//COLS
img = Image.new("RGBA",(COLS*CW, rows*CH+50),(198,198,198,255)); d = ImageDraw.Draw(img)
d.text((12,12),"Scribery & Crafts - geplante Rezepte (Vorschau, Platzhalter-Icons)",fill=(30,30,30),font=big)
used = {}
for n,(title,pat,res,cnt,shapeless) in enumerate(RECIPES):
    ox, oy = (n%COLS)*CW+20, (n//COLS)*CH+55
    d.text((ox,oy-6),title,fill=(20,20,20),font=big)
    if shapeless:
        p = [[None]*3 for _ in range(3)]
        for j,k in enumerate(pat): p[j//3 if False else 0][j] = k
        pat2 = p
    else:
        m = OVR[title]
        pat2 = [[(m[ch] if ch!=" " else None) for ch in row] for row in pat]
    gx, gy = ox+4, oy+20
    grid(d,img,gx,gy,pat2)
    # arrow + result
    ax = gx+3*48+22
    d.polygon([(ax,gy+60),(ax+30,gy+60),(ax+30,gy+50),(ax+52,gy+72),(ax+30,gy+94),(ax+30,gy+84),(ax,gy+84)],fill=(90,90,90))
    rx, ry = ax+62, gy+50
    d.rectangle((rx,ry,rx+46,ry+46),fill=(110,110,110),outline=(55,55,55),width=2)
    resk = {"paper":"paper","anvil":"anvil","mold":"mold"}.get(res)
    if resk: img.alpha_composite(icon(resk,42),(rx+2,ry+2))
    else:
        d.text((rx+23,ry+23),"Block",fill=(240,240,240),font=small,anchor="mm")
    d.text((rx+23,ry+54),f"x{cnt}",fill=(20,20,20),font=font,anchor="mt")
    keys=[]
    for row in pat2:
        for k in row:
            if k and k not in keys: keys.append(k)
    leg = ", ".join(f"{AB[k]}={I[k][0]}" for k in keys)
    import textwrap
    for j,line in enumerate(textwrap.wrap(leg,62)):
        d.text((ox,gy+3*48+14+j*14),line,fill=(40,40,40),font=small)
    d.text((rx+23,ry-8),title.split(' (')[0],fill=(20,20,20),font=small,anchor="mb")
# legend
ly = rows*CH+52
img = img.crop((0,0,img.width,ly))
img.convert("RGB").save("art/out/recipes-preview.png")
print("ok", img.size)
