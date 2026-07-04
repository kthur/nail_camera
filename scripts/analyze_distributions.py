import os
import math
import sys
from pathlib import Path
from PIL import Image
import numpy as np

def rgb_to_hsv_android(r, g, b):
    cmax = max(r, g, b)
    cmin = min(r, g, b)
    v = cmax / 255.0
    delta = cmax - cmin
    
    if cmax != 0:
        s = delta / cmax
    else:
        s = 0.0
        h = 0.0
        return h, s, v
        
    if delta == 0:
        h = 0.0
    else:
        if r == cmax:
            h = (g - b) / delta
        elif g == cmax:
            h = 2.0 + (b - r) / delta
        else:
            h = 4.0 + (r - g) / delta
        h *= 60.0
        if h < 0:
            h += 360.0
    return h, s, v

def std_dev(values, mean):
    if len(values) == 0:
        return 0.0
    diff_sum = sum((v - mean) ** 2 for v in values)
    return math.sqrt(diff_sum / len(values))

def extract_features(image_path):
    try:
        with Image.open(image_path) as im:
            im = im.convert("RGB")
            width, height = im.size
            pixels = im.load()
    except Exception as e:
        print(f"Error opening image {image_path}: {e}")
        return None

    if width == 0 or height == 0:
        return None

    sample_step = max(1, min(width, height) // 80)
    x_steps = (width - 1) // sample_step + 1
    y_steps = (height - 1) // sample_step + 1
    total_samples = x_steps * y_steps

    r_sum = 0.0
    g_sum = 0.0
    b_sum = 0.0
    s_sum = 0.0
    v_sum = 0.0
    dark_edge_count = 0
    brightness_values = []
    red_values = []

    edge_threshold = max(2, int(width * 0.12))
    dark_edge_threshold = 60

    # Match Kotlin sampling order: column-major (x outer, y inner)
    for x in range(0, width, sample_step):
        for y in range(0, height, sample_step):
            r, g, b = pixels[x, y]
            r_sum += r
            g_sum += g
            b_sum += b

            h, s, v = rgb_to_hsv_android(r, g, b)
            s_sum += s
            v_sum += v

            brightness_values.append(v)
            red_values.append(float(r))

            is_edge = (x < edge_threshold or x >= width - edge_threshold or
                       y < edge_threshold or y >= height - edge_threshold)
            if is_edge and r < dark_edge_threshold and g < dark_edge_threshold and b < dark_edge_threshold:
                dark_edge_count += 1

    sample_count = max(1, len(brightness_values))
    avg_r = r_sum / sample_count
    avg_g = g_sum / sample_count
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
    limit_val = min(white_threshold, 0.95)
    white_count = sum(1 for val in sorted_brightness if val > limit_val)
    white_spot_ratio = white_count / size

    dark_edge_ratio = dark_edge_count / sample_count

    # Use exact std_dev implementation matching Kotlin
    brightness_std_dev = std_dev(brightness_values, avg_v)
    redness_std_dev = std_dev(red_values, avg_r)

    if avg_v > 0.01:
        normalized_texture_score = brightness_std_dev / avg_v
    else:
        normalized_texture_score = brightness_std_dev * 255.0

    return {
        "averageRedness": avg_r,
        "averageSaturation": avg_s,
        "averageBrightness": avg_v,
        "whiteSpotRatio": white_spot_ratio,
        "darkEdgeRatio": dark_edge_ratio,
        "brightnessStdDev": brightness_std_dev,
        "rednessUniformity": redness_std_dev,
        "normalizedTextureScore": normalized_texture_score,
        "p25": p25,
        "p75": p75,
        "p90": p90
    }

def main():
    val_dir = Path("d:/k/datasets/nikhilgurav21/nail-disease-detection-dataset/versions/1/data/validation")
    if not val_dir.exists():
        print(f"Error: Validation directory {val_dir} does not exist.")
        sys.exit(1)

    classes = ["Healthy_Nail", "blue_finger", "pitting", "Onychogryphosis", "clubbing", "Acral_Lentiginous_Melanoma"]
    
    # Dictionary to store lists of values per class
    # Class -> FeatureName -> List of values
    features_by_class = {c: {} for c in classes}
    feature_keys = [
        "averageRedness", "averageSaturation", "averageBrightness", 
        "whiteSpotRatio", "darkEdgeRatio", "brightnessStdDev", 
        "rednessUniformity", "normalizedTextureScore", 
        "p25", "p75", "p90"
    ]
    for c in classes:
        for f in feature_keys:
            features_by_class[c][f] = []

    print("Running feature extraction on validation set...")
    for c in classes:
        class_dir = val_dir / c
        if not class_dir.exists():
            print(f"Warning: Class directory {class_dir} does not exist.")
            continue
        
        # Support different image extensions uniquely (preventing case-insensitive duplicates)
        image_paths = []
        seen_paths = set()
        for ext in ["*.jpg", "*.jpeg", "*.png", "*.JPG", "*.JPEG", "*.PNG"]:
            for p in class_dir.glob(ext):
                resolved = p.resolve()
                if resolved not in seen_paths:
                    seen_paths.add(resolved)
                    image_paths.append(p)
            
        print(f"Processing class {c}: {len(image_paths)} images found.")
        for p in image_paths:
            features = extract_features(p)
            if features is None:
                continue
            for f in feature_keys:
                features_by_class[c][f].append(features[f])

    # Compute statistical summaries for each feature per class
    # Format the report
    report = []
    report.append("======================================================================")
    report.append("FEATURE DISTRIBUTION ANALYSIS REPORT (validation set)")
    report.append("======================================================================\n")

    for f in feature_keys:
        report.append(f"Feature: {f}")
        report.append("-" * 80)
        report.append(f"{'Class':<30} | {'Mean':<8} | {'Std':<8} | {'Min':<8} | {'25%':<8} | {'50%':<8} | {'75%':<8} | {'90%':<8} | {'Max':<8}")
        report.append("-" * 80)
        
        for c in classes:
            vals = features_by_class[c][f]
            if len(vals) == 0:
                report.append(f"{c:<30} | {'N/A':<8} | {'N/A':<8} | {'N/A':<8} | {'N/A':<8} | {'N/A':<8} | {'N/A':<8} | {'N/A':<8} | {'N/A':<8}")
                continue
            
            arr = np.array(vals)
            mean = np.mean(arr)
            std = np.std(arr)
            vmin = np.min(arr)
            vmax = np.max(arr)
            p25 = np.percentile(arr, 25)
            p50 = np.percentile(arr, 50)
            p75 = np.percentile(arr, 75)
            p90 = np.percentile(arr, 90)
            
            report.append(
                f"{c:<30} | {mean:8.4f} | {std:8.4f} | {vmin:8.4f} | {p25:8.4f} | {p50:8.4f} | {p75:8.4f} | {p90:8.4f} | {vmax:8.4f}"
            )
        report.append("\n")

    report_text = "\n".join(report)
    print(report_text)
    
    # Save the report to workspace
    out_path = Path("d:/project/nail_camera/.agents/explorer_m3_data/distributions.txt")
    out_path.parent.mkdir(parents=True, exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        f.write(report_text)
    print(f"Report successfully saved to {out_path}")

if __name__ == "__main__":
    main()
