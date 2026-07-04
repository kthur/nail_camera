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
    interior_brightness_values = []
    interior_red_values = []

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
            if not is_edge:
                interior_brightness_values.append(v)
                interior_red_values.append(float(r))
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
    limit_val = min(white_threshold, 0.98)
    white_count = sum(1 for val in sorted_brightness if val > limit_val)
    white_spot_ratio = white_count / size

    dark_edge_ratio = dark_edge_count / sample_count

    interior_avg_v = sum(interior_brightness_values) / len(interior_brightness_values) if len(interior_brightness_values) > 0 else 0.0
    interior_avg_r = sum(interior_red_values) / len(interior_red_values) if len(interior_red_values) > 0 else 0.0

    # Use exact std_dev implementation matching Kotlin
    brightness_std_dev = std_dev(interior_brightness_values, interior_avg_v)
    redness_std_dev = std_dev(interior_red_values, interior_avg_r)

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

def evaluate_metrics(all_samples, thresholds):
    # Unpack thresholds
    # isPale params
    pale_s_max = thresholds.get("pale_s_max", 0.22)
    pale_v_min = thresholds.get("pale_v_min", 0.45)
    pale_r_max = thresholds.get("pale_r_max", 200.0)
    
    # isLowRedness params
    low_r_max = thresholds.get("low_r_max", 130.0)
    low_r_ratio_b = thresholds.get("low_r_ratio_b", 0.95)
    low_r_min_abs = thresholds.get("low_r_min_abs", 100.0)
    
    # hasWhiteSpots params
    white_ratio_min = thresholds.get("white_ratio_min", 0.015)
    
    # isUnevenTexture params
    texture_score_min = thresholds.get("texture_score_min", 0.15)
    red_std_min = thresholds.get("red_std_min", 35.0)
    
    # isDarkEdges params
    dark_ratio_min = thresholds.get("dark_ratio_min", 0.30)
    
    # Evaluate flags for all samples
    counts = {c: {"total": 0, "isPale": 0, "hasWhiteSpots": 0, "isDarkEdges": 0, "isUnevenTexture": 0, "isLowRedness": 0} for c in ["Healthy_Nail", "blue_finger", "pitting", "Onychogryphosis", "clubbing", "Acral_Lentiginous_Melanoma"]}
    
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
        
        # isLowRedness logic
        is_low_redness = (avg_r < low_r_max) and (not is_dark_edges) and (avg_r > avg_b * low_r_ratio_b or avg_r < low_r_min_abs)
        
        # isPale logic
        is_pale = (avg_s < pale_s_max) and (avg_v > pale_v_min) and (avg_r < pale_r_max)
        
        # hasWhiteSpots logic
        # We can also test if hasWhiteSpots has a brightness condition
        has_white_spots = white_spot_ratio > white_ratio_min
        
        # isUnevenTexture logic
        is_uneven_texture = (normalized_texture_score > texture_score_min or redness_std_dev > red_std_min) and (not is_dark_edges)
        
        flags = {
            "isPale": is_pale,
            "hasWhiteSpots": has_white_spots,
            "isDarkEdges": is_dark_edges,
            "isUnevenTexture": is_uneven_texture,
            "isLowRedness": is_low_redness
        }
        
        counts[actual_class]["total"] += 1
        if is_pale: counts[actual_class]["isPale"] += 1
        if has_white_spots: counts[actual_class]["hasWhiteSpots"] += 1
        if is_dark_edges: counts[actual_class]["isDarkEdges"] += 1
        if is_uneven_texture: counts[actual_class]["isUnevenTexture"] += 1
        if is_low_redness: counts[actual_class]["isLowRedness"] += 1
        
        evaluated_samples.append((actual_class, flags))

    # Prediction logic
    def predict_healthy(fl):
        return not (fl["isPale"] or fl["hasWhiteSpots"] or fl["isDarkEdges"] or fl["isUnevenTexture"] or fl["isLowRedness"])
        
    def predict_blue_finger(fl):
        return fl["isLowRedness"] or fl["isPale"]
        
    def predict_pitting(fl):
        return fl["isUnevenTexture"]
        
    def predict_onychogryphosis(fl):
        return fl["isUnevenTexture"] or fl["isDarkEdges"]

    key_classes = {
        "Healthy_Nail": predict_healthy,
        "blue_finger": predict_blue_finger,
        "pitting": predict_pitting,
        "Onychogryphosis": predict_onychogryphosis
    }

    metrics = {}
    for target_class, predictor in key_classes.items():
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
        
    return metrics, counts

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
                
    print(f"Extracted features for {len(all_samples)} validation samples.")

    # 1. Run baseline evaluation
    baseline_thresholds = {
        "pale_s_max": 0.22, "pale_v_min": 0.45, "pale_r_max": 200.0,
        "low_r_max": 130.0, "low_r_ratio_b": 0.95, "low_r_min_abs": 100.0,
        "white_ratio_min": 0.015,
        "texture_score_min": 0.15, "red_std_min": 35.0,
        "dark_ratio_min": 0.30
    }
    baseline_metrics, baseline_counts = evaluate_metrics(all_samples, baseline_thresholds)
    print("\n--- BASELINE METRICS ---")
    for tc, m in baseline_metrics.items():
        print(f"{tc}: Precision={m['precision']:.4f}, Recall={m['recall']:.4f}, F1={m['f1']:.4f}, Accuracy={m['accuracy']:.4f}")
    
    # 2. Search for better thresholds
    # Let's write a search loop to optimize F1 scores
    # We want to optimize:
    # - white_ratio_min
    # - texture_score_min, red_std_min
    # - dark_ratio_min
    # - pale_s_max, pale_v_min, pale_r_max
    # - low_r_max, low_r_ratio_b, low_r_min_abs
    
    best_f1_sum = -1.0
    best_thresholds = {}
    best_metrics = None
    
    # Grid search space
    # hasWhiteSpots:
    # Since Healthy_Nail whiteSpotRatio max is 0.5795, 75% is 0.2906, 90% is 0.3759.
    # blue_finger is 0. pitting has 75% = 0.0785, 90% = 0.2792.
    # clubbing has 75% = 0.0905, 90% = 0.2565.
    # If we want to avoid high false positive rates in Healthy_Nail, we need a higher whiteSpotRatio threshold.
    # Or what if we use the whiteSpotRatio but only when averageBrightness is very high? Wait, no, if averageBrightness is high,
    # healthy nails have high whiteSpotRatio. So we want hasWhiteSpots to be false if the whiteSpotRatio is below some threshold.
    # Let's test white_ratio_min from 0.05 to 0.55.
    
    # isUnevenTexture:
    # Currently: normalizedTextureScore > 0.15 or rednessStdDev > 35.0
    # Healthy_Nail normalizedTextureScore min = 0.3782, 50% = 0.6463, max = 0.8247.
    # Healthy_Nail rednessStdDev min = 63.25, 50% = 87.00.
    # We want pitting and Onychogryphosis to be detected.
    # pitting: normalizedTextureScore max = 0.9578, rednessStdDev max = 95.22
    # Onychogryphosis: normalizedTextureScore max = 0.9636, rednessStdDev max = 79.66
    # Let's search:
    # texture_score_min in [0.40, 0.50, 0.60, 0.70, 0.80, 0.90]
    # red_std_min in [60.0, 70.0, 80.0, 90.0, 100.0, 110.0, 120.0]
    
    # isPale:
    # Healthy_Nail avgS: 25% = 0.1395, 50% = 0.2044.
    # blue_finger avgS: Mean = 0.1906, 50% = 0.1762.
    # clubbing avgS: Mean = 0.1953, 50% = 0.1657.
    # If we make pale_s_max smaller, say 0.15, 0.18, 0.20?
    
    # isLowRedness:
    # Healthy_Nail avgR: Mean = 141.86, 25% = 129.00.
    # blue_finger avgR: Mean = 109.91, 50% = 110.94, 75% = 131.75.
    # Let's test low_r_max in [100.0, 110.0, 115.0, 120.0, 125.0, 130.0].
    
    # isDarkEdges:
    # Healthy_Nail darkEdgeRatio: Mean = 0.2340, 90% = 0.3007, max = 0.3043.
    # Onychogryphosis darkEdgeRatio: Mean = 0.2532, 75% = 0.3244, max = 0.3644.
    # ALM darkEdgeRatio: Mean = 0.2868, 75% = 0.3162, max = 0.3360.
    # Let's search dark_ratio_min in [0.20, 0.22, 0.24, 0.26, 0.28, 0.30, 0.32].

    def satisfies_unit_tests(th):
        if 0.0 > th["dark_ratio_min"]: return False
        if th["low_r_max"] > 220.0: return False
        if th["pale_s_max"] > 0.318: return False
        if 0.0 > th["white_ratio_min"]: return False
        if 0.0 > th["texture_score_min"] and 0.0 > th["red_std_min"]: return False

        is_pale = (0.10 < th["pale_s_max"]) and (0.588 > th["pale_v_min"]) and (150.0 < th["pale_r_max"])
        if not is_pale: return False
        
        is_low_redness = (150.0 < th["low_r_max"]) and (150.0 > 135.0 * th["low_r_ratio_b"] or 150.0 < th["low_r_min_abs"])
        if is_low_redness: return False

        if th["white_ratio_min"] >= 0.0225: return False
        if not (0.33336 > th["texture_score_min"] or 55.0 > th["red_std_min"]): return False
        if th["dark_ratio_min"] >= 0.4224: return False

        return True

    # To do a quick optimization, let's write a coordinate descent or random search to find the best thresholds.
    import random
    
    # Let's try 5000 random combinations of parameters to see what gives the highest F1 sum.
    random.seed(42)
    for i in range(20000):
        th = {
            "pale_s_max": random.uniform(0.10, 0.25),
            "pale_v_min": random.uniform(0.35, 0.60),
            "pale_r_max": random.uniform(150.0, 220.0),
            
            "low_r_max": random.uniform(90.0, 140.0),
            "low_r_ratio_b": random.uniform(0.85, 1.10),
            "low_r_min_abs": random.uniform(80.0, 120.0),
            
            "white_ratio_min": random.uniform(0.005, 0.0224),
            
            "texture_score_min": random.uniform(0.10, 0.95),
            "red_std_min": random.uniform(35.0, 120.0),
            
            "dark_ratio_min": random.uniform(0.20, 0.42)
        }
        
        if not satisfies_unit_tests(th):
            continue
            
        metrics, counts = evaluate_metrics(all_samples, th)
        
        # Calculate sum of F1-scores
        f1_sum = sum(m["f1"] for m in metrics.values())
        
        # Let's also check if we want Healthy_Nail F1 to be > 0
        if f1_sum > best_f1_sum:
            best_f1_sum = f1_sum
            best_thresholds = th
            best_metrics = metrics
            
    print("\n--- BEST METRICS FOUND ---")
    print(f"F1 Sum: {best_f1_sum:.4f}")
    for tc, m in best_metrics.items():
        print(f"{tc}: Precision={m['precision']:.4f}, Recall={m['recall']:.4f}, F1={m['f1']:.4f}, Accuracy={m['accuracy']:.4f}")
        print(f"    TP={m['tp']}, FP={m['fp']}, TN={m['tn']}, FN={m['fn']}")
        
    print("\nBest Thresholds:")
    for k, v in best_thresholds.items():
        print(f"  {k}: {v:.4f}")

if __name__ == "__main__":
    main()
