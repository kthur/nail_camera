import os
import sys
from pathlib import Path
from PIL import Image
import numpy as np
import math

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
        "avgR": avg_r,
        "avgG": avg_g,
        "avgB": avg_b,
        "avgS": avg_s,
        "avgV": avg_v,
        "whiteSpotRatio": white_spot_ratio,
        "darkEdgeRatio": dark_edge_ratio,
        "brightnessStdDev": brightness_std_dev,
        "rednessStdDev": redness_std_dev,
        "normalizedTextureScore": normalized_texture_score,
        "p25": p25,
        "p75": p75,
        "p90": p90
    }

def evaluate_all_classes(all_samples, thresholds):
    pale_s_max = thresholds.get("pale_s_max", 0.22)
    pale_v_min = thresholds.get("pale_v_min", 0.45)
    pale_r_max = thresholds.get("pale_r_max", 200.0)
    
    low_r_max = thresholds.get("low_r_max", 130.0)
    low_r_ratio_b = thresholds.get("low_r_ratio_b", 0.95)
    low_r_min_abs = thresholds.get("low_r_min_abs", 100.0)
    
    white_ratio_min = thresholds.get("white_ratio_min", 0.015)
    
    texture_score_min = thresholds.get("texture_score_min", 0.15)
    red_std_min = thresholds.get("red_std_min", 35.0)
    
    dark_ratio_min = thresholds.get("dark_ratio_min", 0.30)
    
    evaluated_samples = []
    for actual_class, f in all_samples:
        avg_r, avg_s, avg_v = f["avgR"], f["avgS"], f["avgV"]
        avg_b = f["avgB"]
        white_spot_ratio = f["whiteSpotRatio"]
        dark_edge_ratio = f["darkEdgeRatio"]
        brightness_std_dev = f["brightnessStdDev"]
        redness_std_dev = f["rednessStdDev"]
        normalized_texture_score = f["normalizedTextureScore"]
        
        is_dark_edges = dark_edge_ratio > dark_ratio_min
        is_low_redness = (avg_r < low_r_max) and (not is_dark_edges) and (avg_r > avg_b * low_r_ratio_b or avg_r < low_r_min_abs)
        is_pale = (avg_s < pale_s_max) and (avg_v > pale_v_min) and (avg_r < pale_r_max)
        has_white_spots = white_spot_ratio > white_ratio_min
        is_uneven_texture = (normalized_texture_score > texture_score_min or redness_std_dev > red_std_min) and (not is_dark_edges)
        
        flags = {
            "isPale": is_pale,
            "hasWhiteSpots": has_white_spots,
            "isDarkEdges": is_dark_edges,
            "isUnevenTexture": is_uneven_texture,
            "isLowRedness": is_low_redness
        }
        
        evaluated_samples.append((actual_class, flags))

    # Predictors for all 6 classes
    def predict_healthy(fl):
        return not (fl["isPale"] or fl["hasWhiteSpots"] or fl["isDarkEdges"] or fl["isUnevenTexture"] or fl["isLowRedness"])
        
    def predict_blue_finger(fl):
        return fl["isLowRedness"] or fl["isPale"]
        
    def predict_clubbing(fl):
        return fl["isPale"]
        
    def predict_pitting(fl):
        return fl["isUnevenTexture"]
        
    def predict_onychogryphosis(fl):
        return fl["isUnevenTexture"] or fl["isDarkEdges"]
        
    def predict_alm(fl):
        return fl["isDarkEdges"]

    target_classes = {
        "Healthy_Nail": predict_healthy,
        "blue_finger": predict_blue_finger,
        "clubbing": predict_clubbing,
        "pitting": predict_pitting,
        "Onychogryphosis": predict_onychogryphosis,
        "Acral_Lentiginous_Melanoma": predict_alm
    }

    metrics = {}
    for target_class, predictor in target_classes.items():
        tp, fp, tn, fn = 0, 0, 0, 0
        for actual_class, fl in evaluated_samples:
            is_actual = (actual_class == target_class)
            is_predicted = predictor(fl)
            if is_actual and is_predicted:
                tp += 1
            elif not is_actual and is_predicted:
                fp += 1
            elif is_actual and not is_predicted:
                fn += 1
            else:
                tn += 1
        
        prec = tp / (tp + fp) if (tp + fp) > 0 else 0.0
        rec = tp / (tp + fn) if (tp + fn) > 0 else 0.0
        f1 = 2 * prec * rec / (prec + rec) if (prec + rec) > 0 else 0.0
        acc = (tp + tn) / len(evaluated_samples)
        
        metrics[target_class] = {"tp": tp, "fp": fp, "tn": tn, "fn": fn, "precision": prec, "recall": rec, "f1": f1, "accuracy": acc}
        
    return metrics

def calculate_score(metrics):
    # A balanced score: average F1 of all 6 classes
    # But let's penalize Healthy_Nail FP and Onychogryphosis FP, so we want high Precision as well as Recall.
    # Let's use the average F1 of all 6 classes as the base, and add a small weight on Healthy_Nail F1
    # score = mean(F1) + 0.5 * Healthy_Nail F1
    f1s = [m["f1"] for m in metrics.values()]
    avg_f1 = np.mean(f1s)
    healthy_f1 = metrics["Healthy_Nail"]["f1"]
    return avg_f1 + 0.5 * healthy_f1

def main():
    val_dir = Path("d:/k/datasets/nikhilgurav21/nail-disease-detection-dataset/versions/1/data/validation")
    classes = ["Healthy_Nail", "blue_finger", "pitting", "Onychogryphosis", "clubbing", "Acral_Lentiginous_Melanoma"]
    
    all_samples = []
    for c in classes:
        class_dir = val_dir / c
        if not class_dir.exists():
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
        for p in image_paths:
            features = extract_features(p)
            if features is not None:
                all_samples.append((c, features))
                
    print(f"Loaded {len(all_samples)} samples.")

    # 1. Run baseline evaluation
    baseline_thresholds = {
        "pale_s_max": 0.22, "pale_v_min": 0.45, "pale_r_max": 200.0,
        "low_r_max": 130.0, "low_r_ratio_b": 0.95, "low_r_min_abs": 100.0,
        "white_ratio_min": 0.015,
        "texture_score_min": 0.15, "red_std_min": 35.0,
        "dark_ratio_min": 0.30
    }
    baseline_metrics = evaluate_all_classes(all_samples, baseline_thresholds)
    print("\n--- BASELINE METRICS (ALL 6 CLASSES) ---")
    for tc, m in baseline_metrics.items():
        print(f"{tc}: Precision={m['precision']:.4f}, Recall={m['recall']:.4f}, F1={m['f1']:.4f}, Accuracy={m['accuracy']:.4f}")
    print(f"Average F1: {np.mean([m['f1'] for m in baseline_metrics.values()]):.4f}")

    # 2. Search for better thresholds using random search first, then coordinate descent
    import random
    random.seed(42)
    
    best_score = -1.0
    best_thresholds = {}
    best_metrics = None
    
    for i in range(20000):
        th = {
            "pale_s_max": random.uniform(0.12, 0.22),
            "pale_v_min": random.uniform(0.40, 0.60),
            "pale_r_max": random.uniform(160.0, 210.0),
            
            "low_r_max": random.uniform(105.0, 135.0),
            "low_r_ratio_b": random.uniform(0.90, 1.05),
            "low_r_min_abs": random.uniform(90.0, 115.0),
            
            "white_ratio_min": random.uniform(0.20, 0.45),
            
            "texture_score_min": random.uniform(0.50, 0.85),
            "red_std_min": random.uniform(70.0, 110.0),
            
            "dark_ratio_min": random.uniform(0.25, 0.35)
        }
        
        metrics = evaluate_all_classes(all_samples, th)
        score = calculate_score(metrics)
        
        if score > best_score:
            best_score = score
            best_thresholds = th
            best_metrics = metrics
            
    print("\n--- AFTER RANDOM SEARCH ---")
    print(f"Score: {best_score:.4f}")
    for tc, m in best_metrics.items():
        print(f"{tc}: Precision={m['precision']:.4f}, Recall={m['recall']:.4f}, F1={m['f1']:.4f}, Accuracy={m['accuracy']:.4f}")
    
    # Coordinate descent local tuning
    th = best_thresholds.copy()
    steps = {
        "pale_s_max": 0.005,
        "pale_v_min": 0.005,
        "pale_r_max": 2.0,
        "low_r_max": 2.0,
        "low_r_ratio_b": 0.005,
        "low_r_min_abs": 2.0,
        "white_ratio_min": 0.01,
        "texture_score_min": 0.02,
        "red_std_min": 2.0,
        "dark_ratio_min": 0.005
    }

    improved = True
    iteration = 0
    while improved and iteration < 30:
        improved = False
        iteration += 1
        for param, step in steps.items():
            # Try +step
            th_plus = th.copy()
            th_plus[param] += step
            metrics_plus = evaluate_all_classes(all_samples, th_plus)
            score_plus = calculate_score(metrics_plus)
            
            # Try -step
            th_minus = th.copy()
            th_minus[param] -= step
            metrics_minus = evaluate_all_classes(all_samples, th_minus)
            score_minus = calculate_score(metrics_minus)
            
            if score_plus > best_score and score_plus >= score_minus:
                best_score = score_plus
                th = th_plus
                improved = True
            elif score_minus > best_score:
                best_score = score_minus
                th = th_minus
                improved = True

    final_metrics = evaluate_all_classes(all_samples, th)
    print("\n--- FINAL OPTIMIZED METRICS (ALL 6 CLASSES) ---")
    print(f"Final Score: {best_score:.4f}")
    print(f"Average F1: {np.mean([m['f1'] for m in final_metrics.values()]):.4f}")
    for tc, m in final_metrics.items():
        print(f"Class: {tc}")
        print(f"  TP: {m['tp']}, FP: {m['fp']}, TN: {m['tn']}, FN: {m['fn']}")
        print(f"  Accuracy:  {m['accuracy']:.4f}")
        print(f"  Precision: {m['precision']:.4f}")
        print(f"  Recall:    {m['recall']:.4f}")
        print(f"  F1:        {m['f1']:.4f}")
        print()
        
    print("Optimized Thresholds:")
    for k, v in th.items():
        print(f"  {k}: {v:.4f}")

if __name__ == "__main__":
    main()
