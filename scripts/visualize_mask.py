import os
import sys
import numpy as np
from PIL import Image, ImageDraw
from pathlib import Path

VAL_DIR = Path("d:/k/datasets/nikhilgurav21/nail-disease-detection-dataset/versions/1/data/validation")

def rgb_to_hsv(r, g, b):
    r_n, g_n, b_n = r / 255.0, g / 255.0, b / 255.0
    mx = max(r_n, g_n, b_n)
    mn = min(r_n, g_n, b_n)
    df = mx - mn
    h = 0.0
    if mx == mn:
        h = 0.0
    elif mx == r_n:
        h = (60 * ((g_n - b_n) / df) + 360) % 360
    elif mx == g_n:
        h = (60 * ((b_n - r_n) / df) + 120) % 360
    elif mx == b_n:
        h = (60 * ((r_n - g_n) / df) + 240) % 360
    s = 0.0 if mx == 0.0 else df / mx
    v = mx
    return h, s, v

def visualize(img_path, output_path):
    img = Image.open(img_path).convert("RGB")
    orig_w, orig_h = img.size
    
    # Replicate crop
    crop_w = int(orig_w * 0.6)
    crop_h = int(crop_w * 1.33)
    crop_x = (orig_w - crop_w) // 2
    crop_y = (orig_h - crop_h) // 2
    
    safe_x = max(0, min(crop_x, orig_w - crop_w))
    safe_y = max(0, min(crop_y, orig_h - crop_h))
    safe_w = min(crop_w, orig_w - safe_x)
    safe_h = min(crop_h, orig_h - safe_y)
    
    cropped_img = img.crop((safe_x, safe_y, safe_x + safe_w, safe_y + safe_h))
    cropped_img = cropped_img.resize((300, 400))
    
    width, height = cropped_img.size
    pixels = list(cropped_img.getdata())
    
    # Output visual image starting as a copy of cropped_img
    vis_img = cropped_img.copy()
    draw = ImageDraw.Draw(vis_img)
    vis_pixels = vis_img.load()
    
    sample_step = 4
    x_coords = list(range(0, width, sample_step))
    y_coords = list(range(0, height, sample_step))
    
    grid_valid = np.zeros((len(y_coords), len(x_coords)), dtype=bool)
    skin_pixels = []
    
    for j, y in enumerate(y_coords):
        for i, x in enumerate(x_coords):
            idx = y * width + x
            if idx >= len(pixels):
                continue
            r, g, b = pixels[idx]
            h, s, v = rgb_to_hsv(r, g, b)
            
            is_skin = (0.13 <= s <= 0.75) and (v >= 0.15) and (h <= 50 or h >= 320)
            if is_skin:
                grid_valid[j, i] = True
                skin_pixels.append((i, j))
                # Color skin pixels semi-transparent red
                # Since PIL load allows modifying:
                vis_pixels[x, y] = (min(255, r + 80), max(0, g - 40), max(0, b - 40))
            else:
                # Color background pixels blue
                vis_pixels[x, y] = (max(0, r - 40), max(0, g - 40), min(255, b + 80))
                
    if not skin_pixels:
        print("No skin pixels detected in", img_path.name)
        return
        
    xs = [p[0] for p in skin_pixels]
    ys = [p[1] for p in skin_pixels]
    min_i, max_i = min(xs), max(xs)
    min_j, max_j = min(ys), max(ys)
    
    # Draw bounding box
    min_x, max_x = x_coords[min_i], x_coords[max_i]
    min_y, max_y = y_coords[min_j], y_coords[max_j]
    draw.rectangle([min_x, min_y, max_x, max_y], outline="green", width=2)
    
    # Check interior and shiny
    interior = np.zeros_like(grid_valid, dtype=bool)
    for j in range(1, len(y_coords) - 1):
        for i in range(1, len(x_coords) - 1):
            if (grid_valid[j, i] and 
                grid_valid[j-1, i] and grid_valid[j+1, i] and 
                grid_valid[j, i-1] and grid_valid[j, i+1]):
                
                is_shiny_self = grid_v_val = pixels[y_coords[j] * width + x_coords[i]][2] / 255.0 > 0.80 and pixels[y_coords[j] * width + x_coords[i]][0] > 180
                if not is_shiny_self:
                    interior[j, i] = True
                    # Draw a yellow dot for valid interior pixel
                    draw.ellipse([x_coords[i]-1, y_coords[j]-1, x_coords[i]+1, y_coords[j]+1], fill="yellow")
                else:
                    # Draw a cyan dot for shiny pixel
                    draw.ellipse([x_coords[i]-1, y_coords[j]-1, x_coords[i]+1, y_coords[j]+1], fill="cyan")
                    
    vis_img.save(output_path)
    print("Saved visualization to", output_path)

if __name__ == "__main__":
    healthy_dir = VAL_DIR / "Healthy_Nail"
    images = list(healthy_dir.glob("*.jpg"))
    if images:
        visualize(images[0], "d:/project/nail_camera/scripts/healthy_vis.png")
