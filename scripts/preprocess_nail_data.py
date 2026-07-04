# scripts/preprocess_nail_data.py
"""Preprocess downloaded nail disease images.

Steps:
1. Resize every image to 150x200 (same as on‑device extractor).
2. Apply optional augmentation (horizontal flip, brightness jitter).
3. Generate a CSV file `labels.csv` for each source containing `filename,label`.
4. Merge all CSVs into `../datasets/all_nail_data.csv`.
"""
import os, argparse, csv
from pathlib import Path
from PIL import Image, ImageEnhance
import random

TARGET_SIZE = (150, 200)
AUGMENT = True

def crop_nail(img: Image.Image) -> Image.Image:
    w, h = img.size
    crop_w = int(w * 0.6)
    crop_h = int(crop_w * 1.33)
    crop_x = (w - crop_w) // 2
    crop_y = (h - crop_h) // 2
    left = max(0, min(crop_x, w - crop_w))
    top = max(0, min(crop_y, h - crop_h))
    right = min(left + crop_w, w)
    bottom = min(top + crop_h, h)
    return img.crop((left, top, right, bottom))

def resize_image(src_path: Path, dst_path: Path):
    img = Image.open(src_path).convert("RGB")
    img = crop_nail(img)
    img = img.resize(TARGET_SIZE, Image.BILINEAR)
    img.save(dst_path)

def augment_image(img: Image.Image) -> Image.Image:
    # Random horizontal flip
    if random.random() < 0.5:
        img = img.transpose(Image.FLIP_LEFT_RIGHT)
    # Random brightness
    enhancer = ImageEnhance.Brightness(img)
    factor = random.uniform(0.8, 1.2)
    img = enhancer.enhance(factor)
    # Random slight rotation (-15 to +15 degrees)
    angle = random.uniform(-15.0, 15.0)
    img = img.rotate(angle, resample=Image.BILINEAR, expand=False, fillcolor=(128, 128, 128))
    return img

def process_source(source_dir: Path, label_map: dict, output_root: Path):
    images_dir = source_dir / "images"
    out_dir = output_root / source_dir.name / "processed"
    out_dir.mkdir(parents=True, exist_ok=True)
    csv_path = out_dir / "labels.csv"
    rows = []
    for img_file in images_dir.glob("*.*"):
        try:
            img = Image.open(img_file).convert("RGB")
            img = crop_nail(img)
            img = img.resize(TARGET_SIZE, Image.BILINEAR)
            if AUGMENT:
                img = augment_image(img)
            out_path = out_dir / img_file.name
            img.save(out_path)
            # Infer label from original folder name or a mapping file if present
            raw_label = source_dir.name.lower()
            unified = label_map.get(raw_label, "UNKNOWN")
            rows.append([out_path.name, unified])
        except Exception as e:
            print(f"Failed {img_file}: {e}")
    # Write CSV for this source
    with open(csv_path, "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["filename", "label"])
        writer.writerows(rows)
    return csv_path

def merge_csvs(csv_paths, merged_path):
    with open(merged_path, "w", newline="") as out_f:
        writer = csv.writer(out_f)
        writer.writerow(["filename", "label"])
        for cp in csv_paths:
            with open(cp, "r") as in_f:
                reader = csv.reader(in_f)
                next(reader)  # skip header
                for row in reader:
                    writer.writerow(row)

def main():
    parser = argparse.ArgumentParser(description="Preprocess nail disease datasets")
    parser.add_argument("--datasets", type=str, default="../datasets", help="Root folder containing downloaded datasets")
    parser.add_argument("--label_map", type=str, default="../app/src/main/assets/label_map.json", help="Path to label mapping JSON")
    args = parser.parse_args()

    root = Path(__file__).parent.parent / args.datasets
    label_map_path = Path(__file__).parent.parent / args.label_map
    import json
    with open(label_map_path) as f:
        label_map = json.load(f)

    merged_csv = root / "all_nail_data.csv"
    csv_paths = []
    for source in root.iterdir():
        if source.is_dir():
            csv_path = process_source(source, label_map, root)
            csv_paths.append(csv_path)
    merge_csvs(csv_paths, merged_csv)
    print(f"Merged CSV written to {merged_csv}")

if __name__ == "__main__":
    main()
