"""
Project avatar for RLCraft Death Overhaul.

Deliberately matched to the RLCraft Enchantment Recipes avatar so the two read as a
set: 512px squircle, near-black radial background, one centred flat-vector subject
with soft gradients and no outlines, ringed by lavender four-point sparkles.
Subject here is a cracked heart with the broken shard drifting away.

Run with `python tools/make-avatar.py` (needs Pillow). Writes project-avatar.png.
"""
import math
import os
from PIL import Image, ImageDraw, ImageFilter

S = 512          # final size
SS = 4           # supersample factor
N = S * SS

GOLD = (0xE8, 0xC2, 0x5C)
LAV = (0xC2, 0xB2, 0xF2)
EMBER = (0xF0, 0x6A, 0x82)


# ---------------------------------------------------------------- helpers

def radial_bg(size, c_in, c_out, cx, cy, radius, falloff=1.15):
    """Smooth radial gradient, built small and upscaled so it stays banding-free."""
    small = 96
    img = Image.new("RGB", (small, small))
    px = img.load()
    for y in range(small):
        for x in range(small):
            dx = (x + 0.5) / small - cx
            dy = (y + 0.5) / small - cy
            d = min(1.0, math.hypot(dx, dy) / radius) ** falloff
            px[x, y] = tuple(int(c_in[i] + (c_out[i] - c_in[i]) * d) for i in range(3))
    return img.resize((size, size), Image.BICUBIC)


def vertical_gradient(size, c_top, c_bot):
    w, h = size
    strip = Image.new("RGB", (1, h))
    for y in range(h):
        f = y / max(1, h - 1)
        strip.putpixel((0, y), tuple(int(c_top[i] + (c_bot[i] - c_top[i]) * f) for i in range(3)))
    return strip.resize((w, h), Image.BICUBIC)


def squircle_mask(size, radius):
    m = Image.new("L", (size, size), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=255)
    return m


def heart_points(cx, cy, w, h):
    """Classic parametric heart, normalised into the given box."""
    raw = []
    for i in range(1441):
        t = i * (2 * math.pi / 1440)
        x = 16 * math.sin(t) ** 3
        y = 13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)
        raw.append((x, -y))
    xs = [p[0] for p in raw]
    ys = [p[1] for p in raw]
    minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
    sx, sy = w / (maxx - minx), h / (maxy - miny)
    mx, my = (minx + maxx) / 2, (miny + maxy) / 2
    return [(cx + (x - mx) * sx, cy + (y - my) * sy) for x, y in raw]


def sparkle_points(cx, cy, r, n=4.2, stretch=1.10):
    """
    Four-point sparkle: a generalised astroid (x = cos^n t, y = sin^n t), which gives
    cusped tips on the axes joined by concave sides — the reference glyph exactly.
    A plain |cos 2t|^p curve does NOT work here: it pinches to zero at the diagonals
    and renders as four detached petals instead of one solid star.
    """
    pts = []
    for i in range(360):
        t = i * (2 * math.pi / 360)
        ct, st = math.cos(t), math.sin(t)
        x = r * math.copysign(abs(ct) ** n, ct)
        y = r * stretch * math.copysign(abs(st) ** n, st)
        pts.append((cx + x, cy - y))
    return pts


def tinted(mask, colour, alpha=255):
    layer = Image.new("RGBA", mask.size, colour + (0,))
    a = mask if alpha == 255 else mask.point(lambda v: v * alpha // 255)
    layer.putalpha(a)
    return layer


# ---------------------------------------------------------------- canvas

bg = radial_bg(N, (0x1B, 0x19, 0x28), (0x05, 0x05, 0x0A), 0.5, 0.42, 0.80)
canvas = bg.convert("RGBA")

# ---------------------------------------------------------------- heart

HW, HH = int(N * 0.47), int(N * 0.43)
HCX, HCY = N // 2, int(N * 0.535)

heart_mask = Image.new("L", (N, N), 0)
ImageDraw.Draw(heart_mask).polygon(heart_points(HCX, HCY, HW, HH), fill=255)


def uv(u, v):
    return (HCX + u * HW, HCY + v * HH)


# Jagged break across the top of the right lobe. Deliberately NOT a split down the
# middle — that reads as a valentine. Chipping a corner off reads as losing a piece.
# Runs diagonally from near the top notch out to the right flank, so the loss is a
# corner shard rather than a lid. Spacing and amplitude are deliberately uneven — an
# evenly-pitched zigzag reads as pinking shears, not a fracture.
CRACK = [(0.010, -0.300), (0.080, -0.290), (0.130, -0.252), (0.200, -0.248),
         (0.260, -0.214), (0.330, -0.206), (0.390, -0.176), (0.450, -0.166),
         (0.530, -0.128)]
cut_poly = [uv(*p) for p in CRACK] + [uv(0.72, -0.16), uv(0.72, -0.70), uv(0.00, -0.70)]

cut_mask = Image.new("L", (N, N), 0)
ImageDraw.Draw(cut_mask).polygon(cut_poly, fill=255)

frag_mask = Image.new("L", (N, N), 0)
frag_mask.paste(heart_mask, (0, 0), cut_mask)

body_mask = Image.new("L", (N, N), 0)
body_mask.paste(heart_mask, (0, 0), cut_mask.point(lambda v: 255 - v))

# Outer red glow, behind everything.
glow = heart_mask.filter(ImageFilter.GaussianBlur(N * 0.045)).point(lambda v: int(v * 0.5))
canvas.alpha_composite(tinted(glow, (0xC8, 0x28, 0x44)))

heart_grad = vertical_gradient((N, N), (0xD2, 0x3E, 0x57), (0x76, 0x14, 0x28)).convert("RGBA")

body = heart_grad.copy()
body.putalpha(body_mask)
canvas.alpha_composite(body)

# Shard: lifted up and out, rotated, and slightly faded as it drifts.
frag = heart_grad.copy()
frag.putalpha(frag_mask)
frag = frag.rotate(-9, resample=Image.BICUBIC, center=uv(0.28, -0.30))
frag = frag.transform(
    (N, N), Image.AFFINE,
    (1, 0, -int(N * 0.024), 0, 1, int(N * 0.024)),
    resample=Image.BICUBIC,
)
frag.putalpha(frag.getchannel("A").point(lambda v: int(v * 0.90)))
canvas.alpha_composite(frag)

# Soft top-left sheen on the body, so it reads as a rounded volume.
sheen_src = Image.new("L", (N, N), 0)
ImageDraw.Draw(sheen_src).ellipse(
    [uv(-0.34, -0.30)[0], uv(0, -0.32)[1], uv(-0.02, 0)[0], uv(0, 0.04)[1]], fill=255)
sheen = sheen_src.filter(ImageFilter.GaussianBlur(N * 0.035))
sheen = Image.composite(sheen, Image.new("L", (N, N), 0), body_mask)
canvas.alpha_composite(tinted(sheen, (0xFF, 0xB0, 0xBE), alpha=70))

# ---------------------------------------------------------------- sparkles

SPARKS = [  # (u, v, radius as fraction of N)
    (-0.355, -0.300, 0.062), (0.300, -0.360, 0.050), (0.395, -0.150, 0.038),
    (-0.400, 0.055, 0.036), (0.400, 0.135, 0.034), (-0.215, 0.330, 0.030),
    (0.170, 0.345, 0.036), (-0.090, -0.425, 0.028),
]
for u, v, r in SPARKS:
    cx, cy = N * 0.5 + u * N, N * 0.5 + v * N
    rad = r * N
    m = Image.new("L", (N, N), 0)
    ImageDraw.Draw(m).polygon(sparkle_points(cx, cy, rad), fill=255)
    canvas.alpha_composite(tinted(m.filter(ImageFilter.GaussianBlur(rad * 0.28)), LAV, alpha=135))
    canvas.alpha_composite(tinted(m, (0xCE, 0xBE, 0xF6)))

# Embers rising out of the break — the one cue that this heart is being lost.
for u, v, r in [(0.196, -0.150, 0.017), (0.268, -0.226, 0.012), (0.116, -0.212, 0.009)]:
    cx, cy = N * 0.5 + u * N, N * 0.5 + v * N
    rad = r * N
    m = Image.new("L", (N, N), 0)
    ImageDraw.Draw(m).polygon(sparkle_points(cx, cy, rad), fill=255)
    canvas.alpha_composite(tinted(m.filter(ImageFilter.GaussianBlur(rad * 0.45)), EMBER, alpha=180))
    canvas.alpha_composite(tinted(m, (0xFF, 0xD2, 0xDA)))

# ---------------------------------------------------------------- finish

canvas.putalpha(squircle_mask(N, int(N * 0.22)))
out = canvas.resize((S, S), Image.LANCZOS)

dest = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "project-avatar.png")
out.save(dest)
print("wrote", dest)
