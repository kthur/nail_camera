import os
import sys
from pathlib import Path
from PIL import Image
import numpy as np

# Use the exact extraction from coordinate_descent to be consistent

def rgb_to_hsv_android(r, g, b):
    cmax = max(r, g, b)
    cmin = min(r, g, b)
    v = cmax / 255.0
    delta = cmax - cmin
    if cmax != 0:
        s = delta / cmax
    else:
        s = 0.0
    return 0.0, s, v

def std_dev(values, mean):
    if len(values) == 0:
        return 0.0
    return np.std(values)

def extract_features(image_path):
    try:
        with Image.open(image_path) as im:
            im = im.convert("RGB")
            width, height = im.size
            pixels = im.load()
    except Exception as e:
        return None

    sample_step = max(1, min(width, height) // 80)
    brightness_values = []
    red_values = []
    interior_brightness_values = []
    interior_red_values = []
    r_sum = 0.0
    b_sum = 0.0
    s_sum = 0.0
    v_sum = 0.0
    dark_edge_count = 0
    edge_threshold = max(2, int(width * 0.12))
    dark_edge_threshold = 60

    for x in range(0, width, sample_step):
        for y in range(0, height, sample_step):
            r, g, b = pixels[x, y]
            r_sum += r
            b_sum += b
            _, s, v = rgb_to_hsv_android(r, g, b)
            s_sum += s
            v_sum += v
            brightness_values.append(v)
            red_values.append(float(r))
            is_edge = (x < edge_threshold or x >= width - edge_threshold or
                       y < edge_threshold or y >= height - edge_threshold)
            if not is_edge:
                interior_brightness_values.append(v)
                interior_red_values.append(float(r))
            if is_edge and r < dark_edge_threshold and g < dark_edge_threshold and b < dark_edge_threshold:
                dark_edge_count += 1

    sample_count = max(1, len(brightness_values))
    avg_r = r_sum / sample_count
    avg_b = b_sum / sample_count
    avg_s = s_sum / sample_count
    avg_v = v_sum / sample_count

    sorted_brightness = sorted(brightness_values)
    size = len(sorted_brightness)
    idx_90 = min(size - 1, int(size * 0.90))
    idx_25 = min(size - 1, int(size * 0.25))
    idx_75 = min(size - 1, int(size * 0.75))
    p90 = sorted_brightness[idx_90]
    p25 = sorted_brightness[idx_25]
    p75 = sorted_brightness[idx_75]
    brightness_range = p90 - p25
    white_threshold = p75 + brightness_range * 2.0
    limit_val = min(white_threshold, 0.98)
    white_count = sum(1 for val in sorted_brightness if val > limit_val)
    white_spot_ratio = white_count / size
    dark_edge_ratio = dark_edge_count / sample_count

    interior_avg_v = sum(interior_brightness_values) / len(interior_brightness_values) if len(interior_brightness_values) > 0 else 0.0
    interior_avg_r = sum(interior_red_values) / len(interior_red_values) if len(interior_red_values) > 0 else 0.0
    brightness_std_dev = std_dev(interior_brightness_values, interior_avg_v)
    redness_std_dev = std_dev(interior_red_values, interior_avg_r)

    if avg_v > 0.01:
        normalized_texture_score = brightness_std_dev / avg_v
    else:
        normalized_texture_score = brightness_std_dev * 255.0

    return {
        "avgR": avg_r, "avgB": avg_b, "avgS": avg_s, "avgV": avg_v,
        "whiteSpotRatio": white_spot_ratio, "darkEdgeRatio": dark_edge_ratio,
        "brightnessStdDev": brightness_std_dev, "rednessStdDev": redness_std_dev,
        "normalizedTextureScore": normalized_texture_score
    }

def main():
    val_dir = Path("d:/k/datasets/nikhilgurav21/nail-disease-detection-dataset/versions/1/data/validation")
    classes = ["Healthy_Nail", "blue_finger", "pitting", "Onychogryphosis", "clubbing", "Acral_Lentiginous_Melanoma"]
    for c in classes:
        class_dir = val_dir / c
        if not class_dir.exists():
            continue
        image_paths = []
        seen = set()
        for ext in ["*.jpg", "*.jpeg", "*.png", "*.JPG", "*.JPEG", "*.PNG"]:
            for p in class_dir.glob(ext):
                res = p.resolve()
                if res not in seen:
                    seen.add(res)
                    image_paths.append(p)
        
        ws_ratios = []
        textures = []
        avg_rs = []
        for p in image_paths:
            f = extract_features(p)
            if f is not None:
                ws_ratios.append(f["whiteSpotRatio"])
                textures.append(f["normalizedTextureScore"])
                avg_rs.append(f["avgR"])
        
        print(f"Class {c}:")
        if ws_ratios:
            print(f"  whiteSpotRatio: min={np.min(ws_ratios):.4f}, 25%={np.percentile(ws_ratios, 25):.4f}, 50%={np.percentile(ws_ratios, 50):.4f}, 75%={np.percentile(ws_ratios, 75):.4f}, max={np.max(ws_ratios):.4f}")
            print(f"  normalizedTextureScore: min={np.min(textures):.4f}, 25%={np.percentile(textures, 25):.4f}, 50%={np.percentile(textures, 50):.4f}, 75%={np.percentile(textures, 75):.4f}, max={np.max(textures):.4f}")
            print(f"  avgR: min={np.min(avg_rs):.1f}, 25%={np.percentile(avg_rs, 25):.1f}, 50%={np.percentile(avg_rs, 50):.1f}, 75%={np.percentile(avg_rs, 75):.1f}, max={np.max(avg_rs):.1f}")
        print()

if __name__ == "__main__":
    main()
