#!/usr/bin/env python3
"""Crop sheet.png (3 cols x 2 rows) into individual plus-tag icons with transparent background."""
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    print("Install Pillow: pip install Pillow")
    raise

# Order: row0 = bee, white, linked; row1 = booster, contrib, admin
NAMES = ["bee", "white", "linked", "booster", "contrib", "admin"]
ICON_SIZE = 64  # match PlusTagRenderer.TEX_SIZE for sharp in-game scaling
# Pixels within this distance of the sheet background color become transparent (0–255 per channel)
BG_TOLERANCE = 25

SCRIPT_DIR = Path(__file__).resolve().parent
OUT_DIR = SCRIPT_DIR / "src" / "main" / "resources" / "assets" / "eventutils" / "textures" / "gui"
# Prefer sheet in project root, then plus_sheet in gui folder
SHEET_CANDIDATES = [SCRIPT_DIR / "sheet.png", OUT_DIR / "plus_sheet.png"]


def make_bg_transparent(img: Image.Image, bg_rgba: tuple, tolerance: int) -> Image.Image:
    """Replace pixels matching the background color (within tolerance) with transparent."""
    data = img.getdata()
    r0, g0, b0, a0 = bg_rgba
    out = []
    for p in data:
        if len(p) == 3:
            r, g, b = p
            if abs(r - r0) <= tolerance and abs(g - g0) <= tolerance and abs(b - b0) <= tolerance:
                out.append((0, 0, 0, 0))
            else:
                out.append((r, g, b, 255))
        else:
            r, g, b, a = p
            if abs(r - r0) <= tolerance and abs(g - g0) <= tolerance and abs(b - b0) <= tolerance:
                out.append((0, 0, 0, 0))
            else:
                out.append((r, g, b, a))
    img.putdata(out)
    return img


def main():
    SHEET = next((p for p in SHEET_CANDIDATES if p.exists()), None)
    if SHEET is None:
        print(f"Not found: tried {SHEET_CANDIDATES}")
        return 1
    print(f"Using sheet: {SHEET}")
    img = Image.open(SHEET).convert("RGBA")
    w, h = img.size
    # Use top-left corner as background color
    bg_rgba = img.getpixel((0, 0))
    if len(bg_rgba) == 3:
        bg_rgba = (bg_rgba[0], bg_rgba[1], bg_rgba[2], 255)
    print(f"Background color (will be made transparent): {bg_rgba}")

    col_w = w // 3
    row_h = h // 2
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    idx = 0
    for row in range(2):
        for col in range(3):
            x = col * col_w
            y = row * row_h
            cw = (w - x) if col == 2 else col_w
            ch = (h - y) if row == 1 else row_h
            crop = img.crop((x, y, x + cw, y + ch))
            crop = crop.resize((ICON_SIZE, ICON_SIZE), Image.Resampling.LANCZOS)
            crop = make_bg_transparent(crop, bg_rgba, BG_TOLERANCE)
            out_path = OUT_DIR / f"{NAMES[idx]}.png"
            crop.save(out_path)
            print(f"Saved {out_path.name} ({ICON_SIZE}x{ICON_SIZE}, transparent bg)")
            idx += 1
    print("Done.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
