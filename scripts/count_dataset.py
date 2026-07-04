from pathlib import Path
base = Path('d:/k/datasets/nikhilgurav21/nail-disease-detection-dataset/versions/1/data')
for s in sorted((base / 'train').iterdir()):
    if s.is_dir():
        val_dir = base / 'validation' / s.name
        val_count = len(list(val_dir.glob('*'))) if val_dir.exists() else 0
        print(f'{s.name}: train={len(list(s.glob("*")))}, val={val_count}')
