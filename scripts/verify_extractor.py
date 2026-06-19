import os
import sys
import math
import numpy as np
from PIL import Image
from pathlib import Path

# Target directory
VAL_DIR = Path("d:/k/datasets/nikhilgurav21/nail-disease-detection-dataset/versions/1/data/validation")

def rgb_to_hsv(r, g, b):
    # r, g, b are in 0..255
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

def std_dev(values, mean):
    if not values:
        return 0.0
    variance = sum((x - mean) ** 2 for x in values) / len(values)
    return math.sqrt(variance)

def extract_features(img_path):
    try:
        img = Image.open(img_path).convert("RGB")
        orig_w, orig_h = img.size
        
        # Replicate cropNailRegion from Kotlin:
        crop_w = int(orig_w * 0.6)
        crop_h = int(crop_w * 1.33)
        crop_x = (orig_w - crop_w) // 2
        crop_y = (orig_h - crop_h) // 2
        
        safe_x = max(0, min(crop_x, orig_w - crop_w))
        safe_y = max(0, min(crop_y, orig_h - crop_h))
        safe_w = min(crop_w, orig_w - safe_x)
        safe_h = min(crop_h, orig_h - safe_y)
        
        img = img.crop((safe_x, safe_y, safe_x + safe_w, safe_y + safe_h))
        # Downsample to a smaller size (e.g., 150x200) to naturally smooth out noise via bilinear scaling
        img = img.resize((150, 200), Image.Resampling.BILINEAR)
    except Exception as e:
        print(f"Error opening {img_path}: {e}")
        return None
    
    width, height = img.size
    pixels = list(img.getdata())
    
    sample_step = 2  # Smaller sample step because image is smaller (keeps grid size similar)
    
    r_sum, g_sum, b_sum = 0.0, 0.0, 0.0
    v_sum, s_sum, h_sum = 0.0, 0.0, 0.0
    
    brightness_values = []
    red_values = []
    hue_values = []
    
    sample_count = 0
    
    # We will sample into a grid
    x_coords = list(range(0, width, sample_step))
    y_coords = list(range(0, height, sample_step))
    
    grid_v = np.zeros((len(y_coords), len(x_coords)))
    grid_r = np.zeros((len(y_coords), len(x_coords)))
    grid_valid = np.zeros((len(y_coords), len(x_coords)), dtype=bool)
    
    skin_pixels = [] # list of (x, y) coords in grid coordinates
    
    # Define nail window (center 40% of cropped image)
    nail_x_min, nail_x_max = int(width * 0.28), int(width * 0.72)
    nail_y_min, nail_y_max = int(height * 0.28), int(height * 0.72)
    
    for j, y in enumerate(y_coords):
        for i, x in enumerate(x_coords):
            # Only process within center nail window
            if not (nail_x_min <= x <= nail_x_max and nail_y_min <= y <= nail_y_max):
                continue
                
            idx = y * width + x
            if idx >= len(pixels):
                continue
            r, g, b = pixels[idx]
            h, s, v = rgb_to_hsv(r, g, b)
            
            # Skin/finger detection: warm hue, moderate saturation, moderate brightness
            is_skin = (0.13 <= s <= 0.75) and (v >= 0.15) and (h <= 50 or h >= 320)
            
            if is_skin:
                grid_v[j, i] = v
                grid_r[j, i] = r
                grid_valid[j, i] = True
                skin_pixels.append((i, j))
                
                r_sum += r
                g_sum += g
                b_sum += b
                s_sum += s
                v_sum += v
                h_sum += h
                brightness_values.append(v)
                red_values.append(r)
                hue_values.append(h)
                sample_count += 1

    # Fallback if no skin detected: use all non-black pixels
    fallback_triggered = False
    if sample_count < 10:
        fallback_triggered = True
        for j, y in enumerate(y_coords):
            for i, x in enumerate(x_coords):
                idx = y * width + x
                if idx >= len(pixels):
                    continue
                r, g, b = pixels[idx]
                h, s, v = rgb_to_hsv(r, g, b)
                if v >= 0.15:
                    grid_v[j, i] = v
                    grid_r[j, i] = r
                    grid_valid[j, i] = True
                    skin_pixels.append((i, j))
                    
                    r_sum += r
                    g_sum += g
                    b_sum += b
                    s_sum += s
                    v_sum += v
                    h_sum += h
                    brightness_values.append(v)
                    red_values.append(r)
                    hue_values.append(h)
                    sample_count += 1

    if sample_count == 0:
        return None
        
    # print(f"Fallback triggered: {fallback_triggered}, Skin pixels: {sample_count}")
        
    avg_r = r_sum / sample_count
    avg_g = g_sum / sample_count
    avg_b = b_sum / sample_count
    avg_s = s_sum / sample_count
    avg_v = v_sum / sample_count
    avg_h = h_sum / sample_count
    
    # Bounding box of skin pixels
    xs = [p[0] for p in skin_pixels]
    ys = [p[1] for p in skin_pixels]
    min_i, max_i = min(xs), max(xs)
    min_j, max_j = min(ys), max(ys)
    
    # White spot detection: desaturated (s < 0.15) and very bright (v > 0.75) inside the skin bounding box
    white_spot_count = 0
    dark_edge_count = 0
    total_bbox_samples = 0
    
    for j in range(min_j, max_j + 1):
        for i in range(min_i, max_i + 1):
            y = y_coords[j]
            x = x_coords[i]
            idx = y * width + x
            if idx >= len(pixels):
                continue
            r, g, b = pixels[idx]
            h, s, v = rgb_to_hsv(r, g, b)
            
            # White spot: very bright, low saturation
            # And it must be brighter than the average brightness of the skin
            if s < 0.15 and v > 0.75 and v > avg_v * 1.15:
                white_spot_count += 1
                
            # Dark edge: very dark (v < 0.25) near the border of the skin bounding box
            is_near_bbox_edge = (i - min_i < 3 or max_i - i < 3 or j - min_j < 3 or max_j - j < 3)
            if is_near_bbox_edge and v < 0.30 and r < 80:
                dark_edge_count += 1
                
            total_bbox_samples += 1

    white_spot_ratio = white_spot_count / max(1, sample_count)
    dark_edge_ratio = dark_edge_count / max(1, sample_count)
    
    # Calculate local gradients (high-frequency texture) for interior pixels only
    local_v_diffs = []
    local_r_diffs = []
    
    # Simple erosion: a pixel is interior if it and its 4 neighbors are valid skin
    # We also mark shiny pixels (v > 0.80 and s < 0.18) as invalid for gradient to avoid reflection edges
    interior = np.zeros_like(grid_valid, dtype=bool)
    for j in range(1, len(y_coords) - 1):
        for i in range(1, len(x_coords) - 1):
            if (grid_valid[j, i] and 
                grid_valid[j-1, i] and grid_valid[j+1, i] and 
                grid_valid[j, i-1] and grid_valid[j, i+1]):
                
                # Check if it or neighbors are shiny
                is_shiny_self = grid_v[j, i] > 0.80 and grid_r[j, i] > 180 # using r as proxy for brightness
                if not is_shiny_self:
                    interior[j, i] = True
                
    for j in range(len(y_coords)):
        for i in range(len(x_coords)):
            if not interior[j, i]:
                continue
            # Right neighbor
            if i + 1 < len(x_coords) and interior[j, i + 1]:
                local_v_diffs.append(abs(grid_v[j, i] - grid_v[j, i + 1]))
                local_r_diffs.append(abs(grid_r[j, i] - grid_r[j, i + 1]))
            # Bottom neighbor
            if j + 1 < len(y_coords) and interior[j + 1, i]:
                local_v_diffs.append(abs(grid_v[j, i] - grid_v[j + 1, i]))
                local_r_diffs.append(abs(grid_r[j, i] - grid_r[j + 1, i]))
                
    avg_local_v_grad = np.mean(local_v_diffs) if local_v_diffs else 0.0
    avg_local_r_grad = np.mean(local_r_diffs) if local_r_diffs else 0.0
    
    brightness_std = std_dev(brightness_values, avg_v)
    redness_std = std_dev(red_values, avg_r)
    
    # Heuristics tuning
    is_dark_edges = dark_edge_ratio > 0.08
    
    # Low redness check:
    r_b_ratio = avg_r / (avg_b + 1e-5)
    is_low_redness = (avg_r < 138 or r_b_ratio < 1.15) and (not is_dark_edges)
    
    is_pale = avg_s < 0.22 and avg_v > 0.48 and avg_r < 185
    
    has_white_spots = white_spot_ratio > 0.015
    
    # Uneven texture: use local gradients!
    is_uneven_texture = (avg_local_v_grad > 0.022 or avg_local_r_grad > 5.5) and (not is_dark_edges)
    
    return {
        "avg_r": avg_r, "avg_s": avg_s, "avg_v": avg_v, "avg_h": avg_h,
        "white_spot_ratio": white_spot_ratio, "dark_edge_ratio": dark_edge_ratio,
        "brightness_std": brightness_std, "redness_std": redness_std,
        "avg_local_v_grad": avg_local_v_grad, "avg_local_r_grad": avg_local_r_grad,
        "is_pale": is_pale, "has_white_spots": has_white_spots,
        "is_dark_edges": is_dark_edges, "is_uneven_texture": is_uneven_texture,
        "is_low_redness": is_low_redness,
        "orig_w": orig_w, "orig_h": orig_h,
        "fallback": fallback_triggered
    }

def evaluate():
    if not VAL_DIR.exists():
        print(f"Validation directory {VAL_DIR} does not exist.")
        return
        
    print("Evaluating baseline heuristics on Kaggle validation set...")
    categories = sorted([d.name for d in VAL_DIR.iterdir() if d.is_dir()])
    
    results = {}
    for cat in categories:
        cat_dir = VAL_DIR / cat
        images = list(cat_dir.glob("*.jpg")) + list(cat_dir.glob("*.jpeg")) + list(cat_dir.glob("*.png"))
        
        cat_results = []
        for img_path in images:
            features = extract_features(img_path)
            if features:
                cat_results.append(features)
        results[cat] = cat_results
        
        # Calculate statistics
        num_imgs = len(cat_results)
        if num_imgs == 0:
            continue
            
        avg_r_list = [f["avg_r"] for f in cat_results]
        avg_s_list = [f["avg_s"] for f in cat_results]
        avg_v_list = [f["avg_v"] for f in cat_results]
        avg_h_list = [f["avg_h"] for f in cat_results]
        v_grad_list = [f["avg_local_v_grad"] for f in cat_results]
        r_grad_list = [f["avg_local_r_grad"] for f in cat_results]
        ws_ratio_list = [f["white_spot_ratio"] for f in cat_results]
        orig_w_list = [f["orig_w"] for f in cat_results]
        orig_h_list = [f["orig_h"] for f in cat_results]
        fallbacks = sum(1 for f in cat_results if f["fallback"])
        
        pales = sum(1 for f in cat_results if f["is_pale"])
        low_reds = sum(1 for f in cat_results if f["is_low_redness"])
        white_spots = sum(1 for f in cat_results if f["has_white_spots"])
        dark_edges = sum(1 for f in cat_results if f["is_dark_edges"])
        unevens = sum(1 for f in cat_results if f["is_uneven_texture"])
        
        print(f"\n==========================================")
        print(f"Category: {cat} ({num_imgs} images)")
        print(f"==========================================")
        print(f"  Orig Size: {np.mean(orig_w_list):.1f}x{np.mean(orig_h_list):.1f}")
        print(f"  Fallback Rate: {fallbacks}/{num_imgs} ({fallbacks/num_imgs*100:.1f}%)")
        print(f"  Avg H: {np.mean(avg_h_list):.1f} ± {np.std(avg_h_list):.1f}")
        print(f"  Avg R: {np.mean(avg_r_list):.1f} ± {np.std(avg_r_list):.1f}")
        print(f"  Avg S: {np.mean(avg_s_list):.3f} ± {np.std(avg_s_list):.3f}")
        print(f"  Avg V: {np.mean(avg_v_list):.3f} ± {np.std(avg_v_list):.3f}")
        print(f"  Avg V_Grad: {np.mean(v_grad_list):.4f} ± {np.std(v_grad_list):.4f}")
        print(f"  Avg R_Grad: {np.mean(r_grad_list):.2f} ± {np.std(r_grad_list):.2f}")
        print(f"  Avg WS Ratio: {np.mean(ws_ratio_list):.4f} ± {np.std(ws_ratio_list):.4f}")
        print(f"  ---------------------------")
        print(f"  Pale: {pales}/{num_imgs} ({pales/num_imgs*100:.1f}%)")
        print(f"  Low Redness: {low_reds}/{num_imgs} ({low_reds/num_imgs*100:.1f}%)")
        print(f"  White Spots: {white_spots}/{num_imgs} ({white_spots/num_imgs*100:.1f}%)")
        print(f"  Dark Edges: {dark_edges}/{num_imgs} ({dark_edges/num_imgs*100:.1f}%)")
        print(f"  Uneven Texture: {unevens}/{num_imgs} ({unevens/num_imgs*100:.1f}%)")
        
if __name__ == "__main__":
    evaluate()
