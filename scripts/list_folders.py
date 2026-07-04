import os
from pathlib import Path

train_path = Path("d:/k/datasets/nikhilgurav21/nail-disease-detection-dataset/versions/1/data/train")
val_path = Path("d:/k/datasets/nikhilgurav21/nail-disease-detection-dataset/versions/1/data/validation")

print("Train subfolders:")
if train_path.exists():
    for d in sorted(train_path.iterdir()):
        if d.is_dir():
            print(" -", d.name)

print("Validation subfolders:")
if val_path.exists():
    for d in sorted(val_path.iterdir()):
        if d.is_dir():
            print(" -", d.name)
